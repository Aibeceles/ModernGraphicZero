"""
Production Trainer for Multi-Stage Root Count Prediction.

This module provides two-phase training for ProductionRootClassifier:
- Phase 1: Train backbone + CORAL head jointly (ordinal regression)
- Phase 2: Freeze backbone, train Focal head (sharp classification)

Post-training:
- Extract embeddings and fit FAISS index for regime detection
- Calibrate routing thresholds on validation set

Usage:
    trainer = ProductionTrainer(data)
    
    # Train both phases
    model, metrics = trainer.train_full_pipeline()
    
    # Or train phases separately
    model, phase1_metrics = trainer.train_phase1()
    model, phase2_metrics = trainer.train_phase2(model)
    
    # Fit regime detector post-training
    trainer.fit_regime_detector(model)
    
    # Calibrate routing thresholds
    trainer.calibrate_routing(model)
"""

import copy
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

import numpy as np
import torch
import torch.nn.functional as F
from sklearn.metrics import f1_score, balanced_accuracy_score
from sklearn.model_selection import train_test_split
from torch import nn
from torch.optim import Adam
from torch_geometric.data import Data

import sys
from pathlib import Path

# Add parent directory to path for imports
parent_dir = Path(__file__).parent.parent
if str(parent_dir) not in sys.path:
    sys.path.insert(0, str(parent_dir))

from core.config import (
    HOLDOUT_FRACTION,
    RANDOM_SEED,
    MAX_EPOCHS,
    MIN_EPOCHS,
    PATIENCE,
    LEARNING_RATE,
    NUM_CLASSES,
    NUM_FEATURES,
    HIDDEN_DIM,
    DROPOUT_RATE,
    NUM_ATTENTION_HEADS,
    FOCAL_GAMMA,
    compute_class_weights,
)
from .production_model import (
    ProductionRootClassifier,
    compute_coral_loss,
    compute_focal_loss,
    get_production_model,
)
from .uncertainty_router import (
    UncertaintyRouter,
    RoutingConfig,
    calibrate_thresholds,
)
from .regime_detector import RegimeDetector


@dataclass
class TrainingConfig:
    """Configuration for production model training."""
    
    # Phase 1: CORAL training
    phase1_epochs: int = 100
    phase1_patience: int = 15
    phase1_lr: float = 0.01
    phase1_weight_decay: float = 0.001
    
    # Phase 2: Focal training (frozen backbone)
    phase2_epochs: int = 50
    phase2_patience: int = 10
    phase2_lr: float = 0.001
    phase2_weight_decay: float = 0.0001
    
    # Focal loss gamma
    focal_gamma: float = 2.0
    
    # Data split
    holdout_fraction: float = 0.2
    val_fraction: float = 0.1  # From training set
    
    # Minimum epochs before early stopping
    min_epochs: int = 10
    
    # Class weighting
    use_class_weights: bool = True
    class_weight_method: str = 'sqrt'


class ProductionTrainer:
    """
    Two-phase trainer for ProductionRootClassifier.
    
    Training Strategy:
    - Phase 1: Train backbone + CORAL head with ordinal BCE loss
      Goal: Learn embeddings that preserve sub-arrow geometry
    - Phase 2: Freeze backbone, train Focal head with focal loss
      Goal: Learn sharp decisions in stable regions
    
    Post-training:
    - Fit FAISS index on training embeddings for regime detection
    - Calibrate routing thresholds on validation set
    
    Args:
        data: PyG Data object with node features, labels, and edges
        config: TrainingConfig with hyperparameters
        device: Device to use ('cuda', 'cpu', or None for auto)
    """
    
    def __init__(
        self,
        data: Data,
        config: Optional[TrainingConfig] = None,
        device: Optional[str] = None,
    ):
        self.data = data
        self.config = config or TrainingConfig()
        
        # Set device
        if device is None:
            self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        else:
            self.device = device
        
        # Move data to device
        self.data = self.data.to(self.device)
        
        # Create train/val/test split
        self._create_masks()
        
        # Compute class weights
        if self.config.use_class_weights:
            train_labels = self.data.y[self.train_mask]
            self.class_weights = compute_class_weights(
                train_labels, NUM_CLASSES, 
                method=self.config.class_weight_method
            ).to(self.device)
        else:
            self.class_weights = None
        
        # Regime detector (fit post-training)
        self.regime_detector: Optional[RegimeDetector] = None
        
        # Routing config (calibrated post-training)
        self.routing_config: Optional[RoutingConfig] = None
    
    def _create_masks(self):
        """Create stratified train/val/test masks."""
        n_nodes = self.data.num_nodes
        labels = self.data.y.cpu().numpy()
        indices = np.arange(n_nodes)
        
        # First split: train+val vs test
        train_val_idx, test_idx = train_test_split(
            indices,
            test_size=self.config.holdout_fraction,
            stratify=labels,
            random_state=RANDOM_SEED,
        )
        
        # Second split: train vs val
        val_size = self.config.val_fraction / (1 - self.config.holdout_fraction)
        train_idx, val_idx = train_test_split(
            train_val_idx,
            test_size=val_size,
            stratify=labels[train_val_idx],
            random_state=RANDOM_SEED,
        )
        
        # Create boolean masks
        self.train_mask = torch.zeros(n_nodes, dtype=torch.bool, device=self.device)
        self.val_mask = torch.zeros(n_nodes, dtype=torch.bool, device=self.device)
        self.test_mask = torch.zeros(n_nodes, dtype=torch.bool, device=self.device)
        
        self.train_mask[train_idx] = True
        self.val_mask[val_idx] = True
        self.test_mask[test_idx] = True
        
        # Store indices for embedding extraction
        self.train_indices = train_idx
        self.val_indices = val_idx
        self.test_indices = test_idx
    
    def train_full_pipeline(
        self,
        model: Optional[ProductionRootClassifier] = None,
        verbose: bool = True,
    ) -> Tuple[ProductionRootClassifier, Dict]:
        """
        Train complete production pipeline (both phases).
        
        Args:
            model: Optional pre-initialized model (created if None)
            verbose: Whether to print progress
            
        Returns:
            Tuple of (trained model, combined metrics)
        """
        # Phase 1: CORAL training
        model, phase1_metrics = self.train_phase1(model=model, verbose=verbose)
        
        if verbose:
            print("\n" + "="*60)
            print("Phase 1 Complete - Starting Phase 2 (Focal Head)")
            print("="*60 + "\n")
        
        # Phase 2: Focal training
        model, phase2_metrics = self.train_phase2(model=model, verbose=verbose)
        
        # Post-training: Fit regime detector
        if verbose:
            print("\nFitting regime detector...")
        self.fit_regime_detector(model)
        
        # Calibrate routing thresholds
        if verbose:
            print("Calibrating routing thresholds...")
        self.calibrate_routing(model)
        
        # Combine metrics
        metrics = {
            'phase1': phase1_metrics,
            'phase2': phase2_metrics,
            'routing_config': {
                'defer_threshold': self.routing_config.defer_threshold,
                'ordinal_margin': self.routing_config.ordinal_margin,
                'focal_confidence': self.routing_config.focal_confidence,
            } if self.routing_config else None,
        }
        
        return model, metrics
    
    def train_phase1(
        self,
        model: Optional[ProductionRootClassifier] = None,
        verbose: bool = True,
    ) -> Tuple[ProductionRootClassifier, Dict]:
        """
        Phase 1: Train backbone + CORAL head with ordinal BCE loss.
        
        Args:
            model: Optional pre-initialized model
            verbose: Whether to print progress
            
        Returns:
            Tuple of (model, phase1_metrics)
        """
        if verbose:
            print("Phase 1: Training backbone + CORAL head")
        
        # Create model if not provided
        if model is None:
            model = get_production_model(
                num_features=self.data.x.shape[1],
            ).to(self.device)
        
        # Ensure backbone is unfrozen
        model.unfreeze_backbone()
        
        # Get Phase 1 parameters (backbone + CORAL)
        params = model.get_trainable_params(phase=1)
        
        optimizer = Adam(
            params,
            lr=self.config.phase1_lr,
            weight_decay=self.config.phase1_weight_decay,
        )
        
        best_val_macro_f1 = 0
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(self.config.phase1_epochs):
            # Training step
            model.train()
            optimizer.zero_grad()
            
            # Forward pass - get CORAL logits
            coral_logits = model(self.data.x, self.data.edge_index)
            
            # CORAL loss
            loss = compute_coral_loss(
                coral_logits[self.train_mask],
                self.data.y[self.train_mask],
            )
            
            loss.backward()
            optimizer.step()
            
            # Evaluation
            metrics = self._evaluate_coral(model)
            val_macro_f1 = metrics['val_macro_f1']
            
            if verbose and epoch % 10 == 0:
                print(f"  Epoch {epoch:03d} | Loss: {loss:.4f} | "
                      f"Train F1: {metrics['train_macro_f1']:.4f} | "
                      f"Val F1: {val_macro_f1:.4f}")
            
            # Early stopping
            if epoch >= self.config.min_epochs:
                if val_macro_f1 > best_val_macro_f1:
                    best_val_macro_f1 = val_macro_f1
                    patience_counter = 0
                    best_model_state = copy.deepcopy(model.state_dict())
                else:
                    patience_counter += 1
                    if patience_counter >= self.config.phase1_patience:
                        if verbose:
                            print(f"  Early stopping at epoch {epoch}")
                        break
        
        # Load best model state
        if best_model_state is not None:
            model.load_state_dict(best_model_state)
        
        # Final evaluation
        final_metrics = self._evaluate_coral(model)
        final_metrics['epochs_trained'] = epoch + 1
        final_metrics['phase'] = 1
        
        return model, final_metrics
    
    def train_phase2(
        self,
        model: ProductionRootClassifier,
        verbose: bool = True,
    ) -> Tuple[ProductionRootClassifier, Dict]:
        """
        Phase 2: Freeze backbone, train Focal head with focal loss.
        
        Args:
            model: Model with trained backbone from Phase 1
            verbose: Whether to print progress
            
        Returns:
            Tuple of (model, phase2_metrics)
        """
        if verbose:
            print("Phase 2: Training Focal head (frozen backbone)")
        
        # Freeze backbone
        model.freeze_backbone()
        
        # Get Phase 2 parameters (Focal head only)
        params = model.get_trainable_params(phase=2)
        
        optimizer = Adam(
            params,
            lr=self.config.phase2_lr,
            weight_decay=self.config.phase2_weight_decay,
        )
        
        best_val_macro_f1 = 0
        patience_counter = 0
        best_focal_state = None
        
        for epoch in range(self.config.phase2_epochs):
            # Training step
            model.train()
            optimizer.zero_grad()
            
            # Forward pass - get focal logits
            _, _, focal_logits = model(
                self.data.x, self.data.edge_index, return_all=True
            )
            
            # Focal loss
            loss = compute_focal_loss(
                focal_logits[self.train_mask],
                self.data.y[self.train_mask],
                gamma=self.config.focal_gamma,
                alpha=self.class_weights,
            )
            
            loss.backward()
            optimizer.step()
            
            # Evaluation
            metrics = self._evaluate_focal(model)
            val_macro_f1 = metrics['val_macro_f1']
            
            if verbose and epoch % 10 == 0:
                print(f"  Epoch {epoch:03d} | Loss: {loss:.4f} | "
                      f"Train F1: {metrics['train_macro_f1']:.4f} | "
                      f"Val F1: {val_macro_f1:.4f}")
            
            # Early stopping (save only focal head state)
            if epoch >= self.config.min_epochs:
                if val_macro_f1 > best_val_macro_f1:
                    best_val_macro_f1 = val_macro_f1
                    patience_counter = 0
                    best_focal_state = copy.deepcopy(model.focal_head.state_dict())
                else:
                    patience_counter += 1
                    if patience_counter >= self.config.phase2_patience:
                        if verbose:
                            print(f"  Early stopping at epoch {epoch}")
                        break
        
        # Load best focal head state
        if best_focal_state is not None:
            model.focal_head.load_state_dict(best_focal_state)
        
        # Final evaluation
        final_metrics = self._evaluate_focal(model)
        final_metrics['epochs_trained'] = epoch + 1
        final_metrics['phase'] = 2
        
        return model, final_metrics
    
    def fit_regime_detector(
        self,
        model: ProductionRootClassifier,
        use_gpu: bool = False,
    ):
        """
        Fit FAISS regime detector on training embeddings.
        
        Args:
            model: Trained production model
            use_gpu: Whether to use GPU for FAISS
        """
        model.eval()
        
        with torch.no_grad():
            embeddings = model.get_embeddings(
                self.data.x, self.data.edge_index
            )
        
        # Get training embeddings and labels
        train_embeddings = embeddings[self.train_mask].cpu().numpy()
        train_labels = self.data.y[self.train_mask].cpu().numpy()
        
        # Node IDs for explanation
        if hasattr(self.data, 'node_ids'):
            train_ids = np.array(self.data.node_ids)[self.train_indices]
        else:
            train_ids = self.train_indices
        
        # Create and fit detector
        self.regime_detector = RegimeDetector(
            embedding_dim=train_embeddings.shape[1],
            num_regimes=NUM_CLASSES,
            use_gpu=use_gpu,
        )
        self.regime_detector.fit(train_embeddings, train_labels, train_ids)
    
    def calibrate_routing(
        self,
        model: ProductionRootClassifier,
        target_defer_rate: float = 0.05,
        target_accuracy: float = 0.95,
    ):
        """
        Calibrate routing thresholds on validation set.
        
        Args:
            model: Trained production model
            target_defer_rate: Desired defer rate
            target_accuracy: Target accuracy on non-deferred
        """
        model.eval()
        
        with torch.no_grad():
            h, coral_logits, _ = model(
                self.data.x, self.data.edge_index, return_all=True
            )
        
        # Get validation data
        val_h = h[self.val_mask]
        coral_probs = model.coral_head.get_class_probs(val_h)
        coral_expected = model.coral_head.get_expected_value(val_h)
        val_labels = self.data.y[self.val_mask]
        
        # Calibrate thresholds
        self.routing_config = calibrate_thresholds(
            coral_probs=coral_probs,
            coral_expected=coral_expected,
            labels=val_labels,
            target_defer_rate=target_defer_rate,
            target_accuracy=target_accuracy,
        )
    
    def _evaluate_coral(self, model: ProductionRootClassifier) -> Dict:
        """Evaluate CORAL head performance."""
        model.eval()
        
        with torch.no_grad():
            h = model.get_embeddings(self.data.x, self.data.edge_index)
            coral_probs = model.coral_head.get_class_probs(h)
            pred = coral_probs.argmax(dim=-1)
        
        return self._compute_metrics(pred)
    
    def _evaluate_focal(self, model: ProductionRootClassifier) -> Dict:
        """Evaluate Focal head performance."""
        model.eval()
        
        with torch.no_grad():
            h = model.get_embeddings(self.data.x, self.data.edge_index)
            focal_probs = model.focal_head.get_probs(h)
            pred = focal_probs.argmax(dim=-1)
        
        return self._compute_metrics(pred)
    
    def _compute_metrics(self, pred: torch.Tensor) -> Dict:
        """Compute evaluation metrics for predictions."""
        y_train = self.data.y[self.train_mask].cpu().numpy()
        y_val = self.data.y[self.val_mask].cpu().numpy()
        y_test = self.data.y[self.test_mask].cpu().numpy()
        
        pred_train = pred[self.train_mask].cpu().numpy()
        pred_val = pred[self.val_mask].cpu().numpy()
        pred_test = pred[self.test_mask].cpu().numpy()
        
        return {
            'train_macro_f1': f1_score(y_train, pred_train, average='macro', zero_division=0),
            'val_macro_f1': f1_score(y_val, pred_val, average='macro', zero_division=0),
            'test_macro_f1': f1_score(y_test, pred_test, average='macro', zero_division=0),
            'train_weighted_f1': f1_score(y_train, pred_train, average='weighted', zero_division=0),
            'val_weighted_f1': f1_score(y_val, pred_val, average='weighted', zero_division=0),
            'test_weighted_f1': f1_score(y_test, pred_test, average='weighted', zero_division=0),
            'train_balanced_acc': balanced_accuracy_score(y_train, pred_train),
            'val_balanced_acc': balanced_accuracy_score(y_val, pred_val),
            'test_balanced_acc': balanced_accuracy_score(y_test, pred_test),
            'train_mae': np.abs(y_train - pred_train).mean(),
            'val_mae': np.abs(y_val - pred_val).mean(),
            'test_mae': np.abs(y_test - pred_test).mean(),
        }
    
    def evaluate_routed_predictions(
        self,
        model: ProductionRootClassifier,
        router: Optional[UncertaintyRouter] = None,
    ) -> Dict:
        """
        Evaluate performance with uncertainty routing.
        
        Args:
            model: Trained production model
            router: UncertaintyRouter (uses calibrated config if None)
            
        Returns:
            Dict with routed evaluation metrics
        """
        if router is None:
            router = UncertaintyRouter(self.routing_config)
        
        model.eval()
        
        with torch.no_grad():
            h, coral_logits, focal_logits = model(
                self.data.x, self.data.edge_index, return_all=True
            )
            
            coral_probs = model.coral_head.get_class_probs(h)
            coral_expected = model.coral_head.get_expected_value(h)
            focal_probs = F.softmax(focal_logits, dim=-1)
            
            # Get regime distance if detector is fit
            if self.regime_detector is not None:
                h_np = h.cpu().numpy()
                _, regime_dist = self.regime_detector.detect_regime(h_np)
                regime_dist = torch.tensor(regime_dist, device=self.device)
            else:
                regime_dist = None
            
            # Route predictions
            output = router.route(
                coral_probs=coral_probs,
                coral_expected=coral_expected,
                focal_probs=focal_probs,
                regime_distance=regime_dist,
            )
        
        # Compute metrics for test set
        pred_test = output.predictions[self.test_mask].cpu().numpy()
        y_test = self.data.y[self.test_mask].cpu().numpy()
        
        # Get routing stats for test set
        test_decisions = output.decisions[self.test_mask]
        
        return {
            'test_macro_f1': f1_score(y_test, pred_test, average='macro', zero_division=0),
            'test_weighted_f1': f1_score(y_test, pred_test, average='weighted', zero_division=0),
            'test_balanced_acc': balanced_accuracy_score(y_test, pred_test),
            'test_mae': np.abs(y_test - pred_test).mean(),
            'pct_deferred': (test_decisions == 0).float().mean().item() * 100,
            'pct_ordinal': (test_decisions == 1).float().mean().item() * 100,
            'pct_focal': (test_decisions == 2).float().mean().item() * 100,
            'mean_confidence': output.confidence[self.test_mask].mean().item(),
        }


def train_production_model(
    data: Data,
    config: Optional[TrainingConfig] = None,
    verbose: bool = True,
) -> Tuple[ProductionRootClassifier, ProductionTrainer, Dict]:
    """
    Convenience function to train a production model.
    
    Args:
        data: PyG Data object
        config: Optional training configuration
        verbose: Whether to print progress
        
    Returns:
        Tuple of (model, trainer, metrics)
    """
    trainer = ProductionTrainer(data, config=config)
    model, metrics = trainer.train_full_pipeline(verbose=verbose)
    return model, trainer, metrics
