"""
Training infrastructure for Root Count Prediction.

This module provides training functionality including:
- Training loop with early stopping on validation set
- Cross-validation for model selection
- Grid search over hyperparameters
- Evaluation metrics (F1 weighted, macro F1, MAE)
- Class-weighted loss for handling imbalanced data (IMPROVED)

FIX: Now uses proper train/val/test split and early stops on validation metric.
"""

import copy
from typing import Dict, List, Optional, Tuple, Type

import numpy as np
import torch
import torch.nn.functional as F
from sklearn.metrics import f1_score, classification_report, balanced_accuracy_score, confusion_matrix
from sklearn.model_selection import train_test_split, KFold
from torch import nn
from torch.optim import Adam
from torch.utils.data import WeightedRandomSampler
from torch_geometric.data import Data

# Try to import NeighborLoader (requires pyg-lib or torch-sparse)
try:
    from torch_geometric.loader import NeighborLoader
    NEIGHBOR_LOADER_AVAILABLE = True
except ImportError:
    NEIGHBOR_LOADER_AVAILABLE = False
    NeighborLoader = None

from .config import (
    HOLDOUT_FRACTION,
    VALIDATION_FOLDS,
    RANDOM_SEED,
    PENALTIES,
    MAX_EPOCHS,
    MIN_EPOCHS,
    PATIENCE,
    LEARNING_RATE,
    TARGET_MODE,
    LOSS_TYPE,
    FOCAL_GAMMA,
    USE_MINIBATCH,
    BATCH_SIZE,
    NUM_NEIGHBORS,
    USE_WEIGHTED_SAMPLER,
    compute_class_weights,
)
from .models import (
    get_model, 
    DepthAwareGAT, 
    DepthAwareGATv2,
    FocalLossClassifier,
    MultiTaskRootClassifier, 
    RegressionClassifier, 
    CORALClassifier,
)


class NodeClassificationTrainer:
    """
    Trainer for root count prediction (multi-class node classification).
    
    Handles training loop, evaluation, early stopping, and hyperparameter tuning.
    Predicts the number of rational roots (totalZero) for polynomial nodes.
    
    IMPROVED: Now supports class-weighted loss for handling imbalanced data.
    
    Example:
        >>> trainer = NodeClassificationTrainer(data, use_class_weights=True)
        >>> model, metrics = trainer.train('depth_aware_gat', weight_decay=0.0625)
        >>> print(f"Test Macro F1: {metrics['test_macro_f1']:.4f}")
    """
    
    def __init__(
        self,
        data: Data,
        holdout_fraction: float = HOLDOUT_FRACTION,
        random_seed: int = RANDOM_SEED,
        device: Optional[str] = None,
        use_class_weights: bool = True,
    ):
        """
        Initialize the trainer.
        
        Args:
            data: PyG Data object with node features, labels, and edges
            holdout_fraction: Fraction of data for test set
            random_seed: Random seed for reproducibility
            device: Device to use ('cuda', 'cpu', or None for auto-detect)
            use_class_weights: Whether to use inverse frequency class weights
        """
        self.data = data
        self.holdout_fraction = holdout_fraction
        self.random_seed = random_seed
        self.use_class_weights = use_class_weights
        
        # Derive label-space parameters from the data object (set by data_loader).
        # Falls back to max(y)+1 if the data wasn't built with the mapping.
        self.num_classes = getattr(data, 'num_classes', int(data.y.max().item()) + 1)
        self.class_values = getattr(data, 'class_values', list(range(self.num_classes)))
        self.class_names = [
            f'{v} root{"s" if v != 1 else ""}' for v in self.class_values
        ]
        
        # Set device
        if device is None:
            self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        else:
            self.device = device
        
        # Move data to device
        self.data = self.data.to(self.device)
        
        # Create train/test split
        self._create_masks()
        
        # Compute class weights from training data
        if use_class_weights:
            train_labels = self.data.y[self.train_mask]
            self.class_weights = compute_class_weights(train_labels, self.num_classes)
            self.class_weights = self.class_weights.to(self.device)
            print(f"Class weights (sqrt inverse frequency, normalized):")
            for i, (name, weight) in enumerate(zip(self.class_names, self.class_weights)):
                print(f"  {name}: {weight:.3f}")
        else:
            self.class_weights = None
    
    def _get_model_num_classes(self, model: nn.Module) -> int:
        """Detect the model's output dimension (num_classes)."""
        # Try common attribute names
        if hasattr(model, 'num_classes'):
            return model.num_classes
        
        # Check output layer shapes for different architectures
        if hasattr(model, 'fc3'):  # MLPClassifier
            return model.fc3.out_features
        if hasattr(model, 'classifier'):  # GCN, SAGE, GATv2, Focal
            return model.classifier.out_features
        if hasattr(model, 'conv3'):  # DepthAwareGAT
            return model.conv3.out_channels
        if hasattr(model, 'count_head'):  # MultiTaskRootClassifier
            return model.count_head.out_features
        if hasattr(model, 'regressor'):  # RegressionClassifier
            return model.num_classes  # stored as attribute
        if hasattr(model, 'num_thresholds'):  # CORALClassifier
            return model.num_thresholds + 1
        
        # Fallback: assume it matches data
        return self.num_classes
    
    def _create_masks(self):
        """Create train, validation, and test masks for the data.
        
        Split strategy (with guaranteed class coverage):
        - Test: holdout_fraction (default 20%) - only evaluated once at the end
        - Validation: ~15% of remaining data - used for early stopping
        - Train: remaining data - used for training
        
        Guarantees: All present classes appear in all splits by first reserving
        at least 1 sample per class per split, then distributing the rest.
        """
        num_nodes = self.data.num_nodes
        labels = self.data.y.cpu().numpy()
        
        # Compute target sizes
        test_size = int(num_nodes * self.holdout_fraction)
        val_size = int(num_nodes * 0.15)
        train_size = num_nodes - test_size - val_size
        
        # Group indices by class
        rng = np.random.RandomState(self.random_seed)
        class_indices = {}
        for c in range(self.num_classes):
            class_indices[c] = np.where(labels == c)[0].tolist()
            rng.shuffle(class_indices[c])
        
        # Reserve at least 1 sample per class per split (if available)
        train_idx, val_idx, test_idx = [], [], []
        
        for c in range(self.num_classes):
            n_c = len(class_indices[c])
            if n_c == 0:
                continue  # Skip empty classes
            elif n_c == 1:
                # Only 1 sample: put in train
                train_idx.append(class_indices[c][0])
            elif n_c == 2:
                # 2 samples: train + val
                train_idx.append(class_indices[c][0])
                val_idx.append(class_indices[c][1])
            else:
                # 3+ samples: 1 each for train/val/test
                train_idx.append(class_indices[c][0])
                val_idx.append(class_indices[c][1])
                test_idx.append(class_indices[c][2])
                # Remove reserved samples
                class_indices[c] = class_indices[c][3:]
        
        # Distribute remaining samples proportionally
        remaining = []
        for c in range(self.num_classes):
            remaining.extend(class_indices[c])
        rng.shuffle(remaining)
        
        # Calculate how many more we need in each split
        train_needed = max(0, train_size - len(train_idx))
        val_needed = max(0, val_size - len(val_idx))
        test_needed = max(0, test_size - len(test_idx))
        
        # Distribute remaining samples
        ptr = 0
        if train_needed > 0 and ptr < len(remaining):
            train_idx.extend(remaining[ptr:ptr + train_needed])
            ptr += train_needed
        if val_needed > 0 and ptr < len(remaining):
            val_idx.extend(remaining[ptr:ptr + val_needed])
            ptr += val_needed
        if test_needed > 0 and ptr < len(remaining):
            test_idx.extend(remaining[ptr:ptr + test_needed])
            ptr += test_needed
        # Any leftovers go to train
        if ptr < len(remaining):
            train_idx.extend(remaining[ptr:])
        
        train_idx = np.array(train_idx)
        val_idx = np.array(val_idx)
        test_idx = np.array(test_idx)
        
        # Report class coverage
        print(f"Stratified split (guaranteed class coverage):")
        print(f"  Train: {len(train_idx)} | Val: {len(val_idx)} | Test: {len(test_idx)}")
        for split_name, split_idx in [('Train', train_idx), ('Val', val_idx), ('Test', test_idx)]:
            if len(split_idx) > 0:
                unique_classes = np.unique(labels[split_idx])
                print(f"  {split_name} classes: {sorted(unique_classes.tolist())}")
        
        # Create boolean masks
        self.train_mask = torch.zeros(num_nodes, dtype=torch.bool, device=self.device)
        self.val_mask = torch.zeros(num_nodes, dtype=torch.bool, device=self.device)
        self.test_mask = torch.zeros(num_nodes, dtype=torch.bool, device=self.device)
        self.train_mask[train_idx] = True
        self.val_mask[val_idx] = True
        self.test_mask[test_idx] = True
        
        # Store indices for CV (use train+val for CV folds)
        self.train_indices = train_idx
        self.val_indices = val_idx
        self.test_indices = test_idx
        self.train_val_indices = np.concatenate([train_idx, val_idx])  # For CV: use combined train+val
    
    def train(
        self,
        model_name: str = 'depth_aware_gat',
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        verbose: bool = True,
        target_mode: str = TARGET_MODE,
        loss_type: str = LOSS_TYPE,
        lambda_determined: float = 1.0,
        lambda_censor: float = 1.0,
        model: Optional[nn.Module] = None,
        frontier_mask: Optional[torch.Tensor] = None,
        **model_kwargs,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train a model with given hyperparameters.
        
        Args:
            model_name: Model architecture ('mlp', 'gcn', 'graphsage', 'depth_aware_gat')
            weight_decay: L2 regularization strength (penalty in Neo4j terms)
            learning_rate: Learning rate for Adam optimizer
            max_epochs: Maximum number of training epochs
            patience: Early stopping patience
            verbose: Whether to print training progress
            model: Optional pre-built model to use instead of creating a new one.
                Useful for curriculum / fine-tuning workflows where weights are
                transferred from a previous training run.
            frontier_mask: Optional boolean mask [num_nodes] selecting which nodes
                contribute to the loss. When provided, train/val/test masks are
                intersected with this mask so that only frontier nodes are
                supervised while all nodes still participate in message passing.
                Useful for curriculum learning (frontier-only supervision).
            **model_kwargs: Additional arguments for model constructor
            
        Returns:
            Tuple of (trained model, metrics dictionary)
        """
        # Use provided model or create a new one
        if model is not None:
            model = model.to(self.device)
        else:
            # Use num_classes from model_kwargs if provided, otherwise use trainer's default
            model_kwargs.setdefault('num_classes', self.num_classes)
            model = get_model(model_name, **model_kwargs)
            model = model.to(self.device)
        
        # Check if model is depth-aware (needs wNum)
        self.is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
        self.is_multitask = isinstance(model, MultiTaskRootClassifier)
        self.is_focal = isinstance(model, FocalLossClassifier)
        
        # Detect model's actual output dimension and pad class weights if needed
        # (for curriculum learning where models are pre-allocated with max_num_classes)
        model_num_classes = self._get_model_num_classes(model)
        if self.use_class_weights and model_num_classes > self.num_classes:
            # Pad class_weights to match model output dimension
            # Absent classes get weight=1.0 (neutral)
            padded_weights = torch.ones(model_num_classes, device=self.device)
            padded_weights[:self.num_classes] = self.class_weights
            class_weights_for_loss = padded_weights
        elif self.use_class_weights:
            class_weights_for_loss = self.class_weights
        else:
            class_weights_for_loss = None
        
        # Frontier-only supervision: temporarily narrow masks so that loss
        # and evaluation only apply to frontier nodes, while all nodes still
        # participate in GNN message passing.
        _frontier_active = frontier_mask is not None
        if _frontier_active:
            frontier_mask = frontier_mask.to(self.device)
            _orig_train_mask = self.train_mask
            _orig_val_mask = self.val_mask
            _orig_test_mask = self.test_mask
            self.train_mask = self.train_mask & frontier_mask
            self.val_mask = self.val_mask & frontier_mask
            self.test_mask = self.test_mask & frontier_mask
        
        # Get wNum for depth-aware models
        wNum = getattr(self.data, 'wNum', None)
        if wNum is not None:
            wNum = wNum.to(self.device)
        
        # Optimizer with L2 regularization
        optimizer = Adam(
            model.parameters(),
            lr=learning_rate,
            weight_decay=weight_decay,
        )
        
        best_val_macro_f1 = 0  # Use VALIDATION macro F1 for early stopping
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(max_epochs):
            # Training step
            model.train()
            optimizer.zero_grad()
            
            out = self._forward(model, wNum)

            # Compute loss based on model type and loss_type setting
            is_coral = isinstance(model, CORALClassifier)
            is_focal = isinstance(model, FocalLossClassifier)
            
            if is_coral:
                # CORAL uses BCE-per-threshold loss; respect target_mode
                if target_mode == 'censored':
                    y_coral = self.data.y
                else:
                    y_coral = getattr(self.data, 'y_visible', self.data.y)
                loss = self._compute_coral_loss(out, y_coral, self.train_mask)
            elif is_focal or loss_type == 'focal':
                # Focal loss for class imbalance (down-weights easy examples)
                # FocalLossClassifier outputs raw logits
                if isinstance(out, tuple):
                    logits = out[0]
                else:
                    logits = out
                if is_focal:
                    # Use model's built-in focal_loss method
                    loss = model.focal_loss(
                        logits[self.train_mask],
                        self.data.y[self.train_mask],
                        alpha=class_weights_for_loss,
                    )
                else:
                    # Compute focal loss manually for non-FocalLossClassifier models
                    loss = self._compute_focal_loss(
                        logits=logits,
                        labels=self.data.y,
                        mask=self.train_mask,
                        class_weights=class_weights_for_loss,
                        gamma=FOCAL_GAMMA,
                    )
            elif loss_type == 'emd':
                # EMD loss for ordinal-aware training (penalizes by distance from true class)
                # Note: EMD works with raw logits (before log_softmax)
                # For models that output log_softmax, we need the logits
                if isinstance(out, tuple):
                    logits = out[0]  # multitask: use count head
                else:
                    logits = out
                # If model outputs log_softmax, convert back to logits (exp then re-parameterize)
                # This is approximate but works for EMD
                loss = self._compute_emd_loss(
                    logits=logits,
                    labels=self.data.y,
                    mask=self.train_mask,
                    class_weights=class_weights_for_loss,
                )
            else:
                # Standard CE loss (default)
                loss = self._compute_loss(
                    out=out,
                    mask=self.train_mask,
                    class_weights=class_weights_for_loss,
                    target_mode=target_mode,
                    lambda_determined=lambda_determined,
                    lambda_censor=lambda_censor,
                )
            
            loss.backward()
            optimizer.step()
            
            # Evaluation - use VALIDATION macro F1 for early stopping (not train, not test)
            metrics = self._evaluate_full(model, wNum, target_mode=target_mode)
            train_macro_f1 = metrics['train_macro_f1']
            val_macro_f1 = metrics['val_macro_f1']
            
            if verbose and epoch % 10 == 0:
                print(f"Epoch {epoch:03d} | Loss: {loss:.4f} | "
                      f"Train Macro F1: {train_macro_f1:.4f} | Val Macro F1: {val_macro_f1:.4f}")
            
            # Early stopping based on VALIDATION macro F1 (proper generalization check)
            if epoch >= MIN_EPOCHS:
                if val_macro_f1 > best_val_macro_f1:
                    best_val_macro_f1 = val_macro_f1
                    patience_counter = 0
                    # Use deepcopy to avoid reference issues with tensor buffers
                    best_model_state = copy.deepcopy(model.state_dict())
                else:
                    patience_counter += 1
                    if patience_counter >= patience:
                        if verbose:
                            print(f"Early stopping at epoch {epoch}")
                        break
        
        # Load best model state
        if best_model_state is not None:
            model.load_state_dict(best_model_state)
        
        # Final evaluation (test metrics computed only once here)
        final_metrics = self._evaluate_full(model, wNum, target_mode=target_mode)
        train_mae, val_mae, test_mae = self._compute_mae(model, wNum)
        stratified_metrics = self._evaluate_stratified(model, wNum)
        
        metrics = {
            'train_f1': final_metrics['train_weighted_f1'],
            'val_f1': final_metrics['val_weighted_f1'],
            'test_f1': final_metrics['test_weighted_f1'],
            'train_macro_f1': final_metrics['train_macro_f1'],
            'val_macro_f1': final_metrics['val_macro_f1'],
            'test_macro_f1': final_metrics['test_macro_f1'],
            'train_balanced_acc': final_metrics['train_balanced_acc'],
            'val_balanced_acc': final_metrics['val_balanced_acc'],
            'test_balanced_acc': final_metrics['test_balanced_acc'],
            'train_mae': train_mae,
            'val_mae': val_mae,
            'test_mae': test_mae,
            'epochs_trained': epoch + 1,
            'model_name': model_name,
            'weight_decay': weight_decay,
            'target_mode': target_mode,
            'loss_type': loss_type,
            **stratified_metrics,
        }
        
        # Restore original masks after frontier-only training
        if _frontier_active:
            self.train_mask = _orig_train_mask
            self.val_mask = _orig_val_mask
            self.test_mask = _orig_test_mask
        
        return model, metrics

    def _evaluate_stratified(self, model: nn.Module, wNum: Optional[torch.Tensor] = None) -> Dict:
        """
        Evaluate visible-count performance stratified by `determined`.

        Note: `determined` is computed on a larger/full scan window and is NOT a
        completeness flag for `rootList`. This stratification is still useful
        because `determined` correlates with boundary/censoring regimes.

        Returns:
            Dict with test-set macro F1 and MAE on determined/undetermined subsets.
            If a subset is empty, values are NaN.
        """
        determined = getattr(self.data, 'determined', None)
        if determined is None:
            return {}

        y_visible = getattr(self.data, 'y_visible', self.data.y)

        model.eval()
        is_coral = isinstance(model, CORALClassifier)
        with torch.no_grad():
            out = self._forward(model, wNum)
            determined_out = None
            if isinstance(out, tuple):
                out, determined_out = out
            
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)

        def _subset_metrics(mask: torch.Tensor) -> Dict[str, float]:
            if not mask.any():
                return {
                    'macro_f1': float('nan'),
                    'balanced_acc': float('nan'),
                    'mae': float('nan'),
                    'n': 0.0,
                }
            y_true = y_visible[mask].cpu().numpy()
            y_pred = pred[mask].cpu().numpy()
            return {
                'macro_f1': float(f1_score(y_true, y_pred, average='macro', zero_division=0)),
                'balanced_acc': float(balanced_accuracy_score(y_true, y_pred)),
                'mae': float(np.mean(np.abs(y_true - y_pred))),
                'n': float(mask.sum().item()),
            }

        test_det_mask = self.test_mask & determined
        test_undet_mask = self.test_mask & ~determined

        det = _subset_metrics(test_det_mask)
        undet = _subset_metrics(test_undet_mask)

        metrics = {
            'test_macro_f1_determined': det['macro_f1'],
            'test_balanced_acc_determined': det['balanced_acc'],
            'test_mae_determined': det['mae'],
            'test_n_determined': det['n'],
            'test_macro_f1_undetermined': undet['macro_f1'],
            'test_balanced_acc_undetermined': undet['balanced_acc'],
            'test_mae_undetermined': undet['mae'],
            'test_n_undetermined': undet['n'],
        }

        # If multitask head is available, report determined accuracy on the test set.
        if determined_out is not None:
            det_pred = determined_out.argmax(dim=1)
            det_true = determined.long()
            det_mask = self.test_mask
            if det_mask.any():
                metrics['test_determined_acc'] = float((det_pred[det_mask] == det_true[det_mask]).float().mean().item())
                metrics['test_determined_balanced_acc'] = float(
                    balanced_accuracy_score(
                        det_true[det_mask].cpu().numpy(),
                        det_pred[det_mask].cpu().numpy(),
                    )
                )

        return metrics
    
    def _create_train_loader(self, batch_size: int = BATCH_SIZE, num_neighbors: List[int] = None) -> NeighborLoader:
        """
        Create a NeighborLoader with WeightedRandomSampler for balanced mini-batch training.
        
        Oversamples minority classes to ensure each batch has roughly equal class representation.
        
        Args:
            batch_size: Number of target nodes per batch
            num_neighbors: Neighbors to sample at each hop (default: [10, 10] for 2-hop)
            
        Returns:
            NeighborLoader configured for weighted sampling
        """
        if num_neighbors is None:
            num_neighbors = NUM_NEIGHBORS
        
        # Get training node indices and their labels
        train_indices = self.train_mask.nonzero(as_tuple=True)[0]
        train_labels = self.data.y[train_indices]
        
        # Compute per-sample weights based on inverse class frequency
        class_counts = torch.bincount(train_labels, minlength=self.num_classes).float()
        class_counts = class_counts.clamp(min=1)  # Avoid division by zero
        class_weights = 1.0 / class_counts
        sample_weights = class_weights[train_labels].cpu().numpy()
        
        # Create WeightedRandomSampler
        if USE_WEIGHTED_SAMPLER:
            sampler = WeightedRandomSampler(
                weights=sample_weights,
                num_samples=len(sample_weights),
                replacement=True,  # Required for oversampling minority classes
            )
        else:
            sampler = None
        
        # Create NeighborLoader
        loader = NeighborLoader(
            self.data,
            num_neighbors=num_neighbors,
            batch_size=batch_size,
            input_nodes=train_indices,
            sampler=sampler,
            shuffle=(sampler is None),  # Shuffle only if no sampler
        )
        
        return loader
    
    def train_minibatch(
        self,
        model_name: str = 'depth_aware_gat',
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        batch_size: int = BATCH_SIZE,
        num_neighbors: List[int] = None,
        verbose: bool = True,
        loss_type: str = LOSS_TYPE,
        **model_kwargs,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train a model using mini-batch training with NeighborLoader.
        
        Uses WeightedRandomSampler to oversample minority classes, which helps
        address extreme class imbalance better than class-weighted loss alone.
        
        **Requires**: Either `pyg-lib` or `torch-sparse` to be installed.
        
        Args:
            model_name: Name of model architecture
            weight_decay: L2 regularization strength
            learning_rate: Optimizer learning rate
            max_epochs: Maximum training epochs
            patience: Early stopping patience
            batch_size: Number of target nodes per batch
            num_neighbors: Neighbors to sample per hop
            verbose: Whether to print progress
            loss_type: Loss function type ('ce' or 'emd')
            **model_kwargs: Additional model arguments
            
        Returns:
            Tuple of (trained model, metrics dict)
            
        Raises:
            ImportError: If NeighborLoader dependencies are not available
        """
        if not NEIGHBOR_LOADER_AVAILABLE:
            raise ImportError(
                "Mini-batch training requires either 'pyg-lib' or 'torch-sparse'.\n"
                "Install with:\n"
                "  pip install pyg-lib torch-sparse -f https://data.pyg.org/whl/torch-2.9.1+cpu.html\n"
                "Or use regular full-batch training with: trainer.train(loss_type='emd')"
            )
        
        if num_neighbors is None:
            num_neighbors = NUM_NEIGHBORS
        
        # Initialize model with the correct number of output classes
        model = get_model(model_name, num_classes=self.num_classes, **model_kwargs).to(self.device)
        optimizer = Adam(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
        
        # Create data loader with weighted sampling
        train_loader = self._create_train_loader(batch_size=batch_size, num_neighbors=num_neighbors)
        
        # Get wNum for depth-aware models (use full data's wNum)
        wNum = self.data.wNum if hasattr(self.data, 'wNum') else None
        
        # Training loop
        best_val_macro_f1 = -1.0
        best_model_state = None
        epochs_without_improvement = 0
        
        for epoch in range(max_epochs):
            model.train()
            total_loss = 0.0
            num_batches = 0
            
            for batch in train_loader:
                batch = batch.to(self.device)
                optimizer.zero_grad()
                
                # Forward pass on batch
                # Note: batch.n_id contains the global node IDs, batch.batch_size is num target nodes
                is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
                is_coral = isinstance(model, CORALClassifier)

                # Feature ablation: mask node features if configured
                batch_x_input = batch.x
                node_feat_idx = getattr(self, '_node_feat_idx', None)
                if node_feat_idx is not None:
                    batch_x_input = batch.x[:, node_feat_idx]

                # Edge ablation: compute from full-width batch.x, then mask
                batch_edge_attr = None
                if is_depth_aware:
                    from .edge_features import compute_polynomial_edge_features
                    from ..features.edge_feature_sets import apply_edge_mask
                    batch_edge_attr = compute_polynomial_edge_features(batch.x, batch.edge_index)
                    edge_feature_set = getattr(self, 'edge_feature_set', 'full')
                    if edge_feature_set != 'full':
                        batch_edge_attr, _ = apply_edge_mask(batch_edge_attr, edge_feature_set)

                if is_depth_aware and wNum is not None:
                    # Get wNum for batch nodes
                    batch_wNum = wNum[batch.n_id]
                    out = model(batch_x_input, batch.edge_index, batch_wNum, edge_attr=batch_edge_attr)
                elif is_depth_aware:
                    out = model(batch_x_input, batch.edge_index, edge_attr=batch_edge_attr)
                else:
                    out = model(batch_x_input, batch.edge_index)
                
                # Only compute loss on target nodes (first batch_size nodes in batch)
                target_mask = torch.zeros(batch.num_nodes, dtype=torch.bool, device=self.device)
                target_mask[:batch.batch_size] = True
                target_labels = batch.y[:batch.batch_size]
                
                # Compute loss
                if is_coral:
                    loss = self._compute_coral_loss_batch(out, target_labels, target_mask)
                elif loss_type == 'emd':
                    if isinstance(out, tuple):
                        logits = out[0]
                    else:
                        logits = out
                    loss = self._compute_emd_loss(
                        logits=logits,
                        labels=batch.y,
                        mask=target_mask,
                        class_weights=None,  # Don't use class weights with weighted sampler
                    )
                else:
                    # Standard CE loss
                    if isinstance(out, tuple):
                        out = out[0]
                    loss = F.cross_entropy(out[target_mask], target_labels)
                
                loss.backward()
                optimizer.step()
                
                total_loss += loss.item()
                num_batches += 1
            
            avg_loss = total_loss / max(num_batches, 1)
            
            # Evaluation on full graph (not mini-batch) for accurate metrics
            metrics = self._evaluate_full(model, wNum, target_mode='visible')
            val_macro_f1 = metrics['val_macro_f1']
            
            if verbose and epoch % 10 == 0:
                print(f"Epoch {epoch:03d} | Loss: {avg_loss:.4f} | "
                      f"Train Macro F1: {metrics['train_macro_f1']:.4f} | Val Macro F1: {val_macro_f1:.4f}")
            
            # Early stopping
            if epoch >= MIN_EPOCHS:
                if val_macro_f1 > best_val_macro_f1:
                    best_val_macro_f1 = val_macro_f1
                    best_model_state = copy.deepcopy(model.state_dict())
                    epochs_without_improvement = 0
                else:
                    epochs_without_improvement += 1
                    if epochs_without_improvement >= patience:
                        if verbose:
                            print(f"Early stopping at epoch {epoch}")
                        break
        
        # Restore best model
        if best_model_state is not None:
            model.load_state_dict(best_model_state)
        
        # Final evaluation
        final_metrics = self._evaluate_full(model, wNum, target_mode='visible')
        train_mae, val_mae, test_mae = self._compute_mae(model, wNum)
        stratified_metrics = self._evaluate_stratified(model, wNum)
        
        metrics = {
            'train_f1': final_metrics['train_weighted_f1'],
            'val_f1': final_metrics['val_weighted_f1'],
            'test_f1': final_metrics['test_weighted_f1'],
            'train_macro_f1': final_metrics['train_macro_f1'],
            'val_macro_f1': final_metrics['val_macro_f1'],
            'test_macro_f1': final_metrics['test_macro_f1'],
            'train_balanced_acc': final_metrics['train_balanced_acc'],
            'val_balanced_acc': final_metrics['val_balanced_acc'],
            'test_balanced_acc': final_metrics['test_balanced_acc'],
            'train_mae': train_mae,
            'val_mae': val_mae,
            'test_mae': test_mae,
            'epochs_trained': epoch + 1,
            'model_name': model_name,
            'weight_decay': weight_decay,
            'loss_type': loss_type,
            'batch_size': batch_size,
            'use_weighted_sampler': USE_WEIGHTED_SAMPLER,
            **stratified_metrics,
        }
        
        return model, metrics
    
    def _compute_coral_loss_batch(
        self,
        logits: torch.Tensor,
        labels: torch.Tensor,
        mask: torch.Tensor,
    ) -> torch.Tensor:
        """Compute CORAL loss for a mini-batch."""
        num_thresholds = logits.shape[1]
        
        # Build binary targets
        targets = torch.zeros_like(logits)
        for k in range(num_thresholds):
            targets[:, k] = (labels > k).float()
        
        # BCE with logits on masked nodes
        loss = F.binary_cross_entropy_with_logits(
            logits[mask], targets[mask], reduction='mean'
        )
        return loss

    def train_regression(
        self,
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        verbose: bool = True,
        **model_kwargs,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train a regression model for root count prediction.
        
        This treats root count as a continuous value (0.0-4.0) and uses 
        SmoothL1 loss. Predictions are rounded to get class labels.
        
        This serves as a diagnostic to test if ordinal structure matters.
        If regression beats classification, consider CORAL ordinal loss.
        
        Args:
            weight_decay: L2 regularization strength
            learning_rate: Learning rate for Adam optimizer
            max_epochs: Maximum number of training epochs
            patience: Early stopping patience
            verbose: Whether to print training progress
            **model_kwargs: Additional arguments for model constructor
            
        Returns:
            Tuple of (trained model, metrics dictionary)
        """
        # Create regression model
        model = RegressionClassifier(**model_kwargs)
        model = model.to(self.device)
        
        # Optimizer
        optimizer = Adam(
            model.parameters(),
            lr=learning_rate,
            weight_decay=weight_decay,
        )
        
        # For regression, early stop on validation MAE (lower is better)
        best_val_mae = float('inf')
        patience_counter = 0
        best_model_state = None
        
        # Target as float for regression
        y_float = self.data.y.float()
        
        for epoch in range(max_epochs):
            # Training step
            model.train()
            optimizer.zero_grad()
            
            # Forward pass
            out = model(self.data.x, self.data.edge_index)  # [N, 1]
            
            # SmoothL1 loss (less sensitive to outliers than MSE)
            loss = F.smooth_l1_loss(
                out[self.train_mask].squeeze(-1),
                y_float[self.train_mask]
            )
            
            loss.backward()
            optimizer.step()
            
            # Evaluation
            metrics = self._evaluate_regression(model)
            train_mae = metrics['train_mae']
            val_mae = metrics['val_mae']
            val_macro_f1 = metrics['val_macro_f1']
            
            if verbose and epoch % 10 == 0:
                print(f"Epoch {epoch:03d} | Loss: {loss:.4f} | "
                      f"Train MAE: {train_mae:.4f} | Val MAE: {val_mae:.4f} | Val Macro F1: {val_macro_f1:.4f}")
            
            # Early stopping based on validation MAE
            if epoch >= MIN_EPOCHS:
                if val_mae < best_val_mae:
                    best_val_mae = val_mae
                    patience_counter = 0
                    best_model_state = copy.deepcopy(model.state_dict())
                else:
                    patience_counter += 1
                    if patience_counter >= patience:
                        if verbose:
                            print(f"Early stopping at epoch {epoch}")
                        break
        
        # Load best model state
        if best_model_state is not None:
            model.load_state_dict(best_model_state)
        
        # Final evaluation
        final_metrics = self._evaluate_regression(model)
        
        metrics = {
            'train_f1': final_metrics['train_weighted_f1'],
            'val_f1': final_metrics['val_weighted_f1'],
            'test_f1': final_metrics['test_weighted_f1'],
            'train_macro_f1': final_metrics['train_macro_f1'],
            'val_macro_f1': final_metrics['val_macro_f1'],
            'test_macro_f1': final_metrics['test_macro_f1'],
            'train_balanced_acc': final_metrics['train_balanced_acc'],
            'val_balanced_acc': final_metrics['val_balanced_acc'],
            'test_balanced_acc': final_metrics['test_balanced_acc'],
            'train_mae': final_metrics['train_mae'],
            'val_mae': final_metrics['val_mae'],
            'test_mae': final_metrics['test_mae'],
            'epochs_trained': epoch + 1,
            'model_name': 'regression',
            'weight_decay': weight_decay,
        }
        
        return model, metrics
    
    def _evaluate_regression(self, model: nn.Module) -> Dict:
        """
        Evaluate regression model with both regression and classification metrics.
        
        Args:
            model: RegressionClassifier model
            
        Returns:
            Dictionary with MAE, F1, and balanced accuracy metrics
        """
        model.eval()
        with torch.no_grad():
            # Get regression predictions
            out = model(self.data.x, self.data.edge_index).squeeze(-1)  # [N]
            
            # Round to get class predictions
            pred = out.round().clamp(0, self.num_classes - 1).long()
            
            # Compute MAE (regression metric)
            y_float = self.data.y.float()
            train_mae = F.l1_loss(out[self.train_mask], y_float[self.train_mask]).item()
            val_mae = F.l1_loss(out[self.val_mask], y_float[self.val_mask]).item()
            test_mae = F.l1_loss(out[self.test_mask], y_float[self.test_mask]).item()
            
            # Compute classification metrics after rounding
            y_true_train = self.data.y[self.train_mask].cpu().numpy()
            y_pred_train = pred[self.train_mask].cpu().numpy()
            
            y_true_val = self.data.y[self.val_mask].cpu().numpy()
            y_pred_val = pred[self.val_mask].cpu().numpy()
            
            y_true_test = self.data.y[self.test_mask].cpu().numpy()
            y_pred_test = pred[self.test_mask].cpu().numpy()
            
            # Weighted F1
            train_weighted_f1 = f1_score(y_true_train, y_pred_train, average='weighted', zero_division=0)
            val_weighted_f1 = f1_score(y_true_val, y_pred_val, average='weighted', zero_division=0)
            test_weighted_f1 = f1_score(y_true_test, y_pred_test, average='weighted', zero_division=0)
            
            # Macro F1
            train_macro_f1 = f1_score(y_true_train, y_pred_train, average='macro', zero_division=0)
            val_macro_f1 = f1_score(y_true_val, y_pred_val, average='macro', zero_division=0)
            test_macro_f1 = f1_score(y_true_test, y_pred_test, average='macro', zero_division=0)
            
            # Balanced accuracy
            train_balanced_acc = balanced_accuracy_score(y_true_train, y_pred_train)
            val_balanced_acc = balanced_accuracy_score(y_true_val, y_pred_val)
            test_balanced_acc = balanced_accuracy_score(y_true_test, y_pred_test)
        
        return {
            'train_mae': train_mae,
            'val_mae': val_mae,
            'test_mae': test_mae,
            'train_weighted_f1': train_weighted_f1,
            'val_weighted_f1': val_weighted_f1,
            'test_weighted_f1': test_weighted_f1,
            'train_macro_f1': train_macro_f1,
            'val_macro_f1': val_macro_f1,
            'test_macro_f1': test_macro_f1,
            'train_balanced_acc': train_balanced_acc,
            'val_balanced_acc': val_balanced_acc,
            'test_balanced_acc': test_balanced_acc,
        }
    
    def _forward(self, model: nn.Module, wNum: Optional[torch.Tensor] = None) -> torch.Tensor:
        """Forward pass handling standard, depth-aware, focal, and multitask models.

        Uses ``data.x_masked`` (if set by the experiment runner) as the node
        representation passed to the model, while edge features are always
        derived from ``data.x`` (full-width) so that hardcoded column indices
        in ``compute_polynomial_edge_features`` remain correct.

        When ``self.edge_feature_set`` is not ``'full'``, the 18-D raw edge
        feature vector is sliced down before being passed to the model.
        """
        from .edge_features import compute_polynomial_edge_features
        from ..features.edge_feature_sets import apply_edge_mask

        # Node representation: apply feature mask if configured
        if hasattr(self, '_node_feat_idx') and self._node_feat_idx is not None:
            x_input = self.data.x[:, self._node_feat_idx]
        else:
            x_input = self.data.x

        is_edge_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))

        if is_edge_aware:
            # Compute edge features from full-width x (correct indices)
            edge_feat = compute_polynomial_edge_features(self.data.x, self.data.edge_index)
            edge_feature_set = getattr(self, 'edge_feature_set', 'full')
            if edge_feature_set != 'full':
                edge_feat, _ = apply_edge_mask(edge_feat, edge_feature_set)

            if wNum is not None:
                return model(x_input, self.data.edge_index, wNum=wNum, edge_attr=edge_feat)
            return model(x_input, self.data.edge_index, edge_attr=edge_feat)

        return model(x_input, self.data.edge_index)

    def _compute_loss(
        self,
        out,
        mask: torch.Tensor,
        class_weights: Optional[torch.Tensor],
        target_mode: str,
        lambda_determined: float,
        lambda_censor: float,
    ) -> torch.Tensor:
        """
        Compute training loss for the selected target mode.

        target_mode:
          - 'visible': supervised on y_visible (stored as data.y / data.y_visible)
          - 'multitask': supervised on y_visible + determined (binary)
          - 'censored': supervised on y_true where available + hinge constraint y_pred >= y_visible
        """
        if target_mode not in ('visible', 'multitask', 'censored'):
            raise ValueError(f"Unknown target_mode='{target_mode}'")

        # Visible labels (always defined)
        y_visible = getattr(self.data, 'y_visible', self.data.y)

        # Multitask output is a tuple: (count_log_probs, determined_log_probs)
        if isinstance(out, tuple):
            count_out, determined_out = out
        else:
            count_out, determined_out = out, None

        if target_mode == 'visible':
            if class_weights is not None:
                return F.nll_loss(count_out[mask], y_visible[mask], weight=class_weights)
            return F.nll_loss(count_out[mask], y_visible[mask])

        if target_mode == 'multitask':
            if determined_out is None:
                raise ValueError("target_mode='multitask' requires a multitask model output (tuple).")
            determined = getattr(self.data, 'determined', None)
            if determined is None:
                raise ValueError("Data object is missing `determined` for multitask training.")

            if class_weights is not None:
                loss_count = F.nll_loss(count_out[mask], y_visible[mask], weight=class_weights)
            else:
                loss_count = F.nll_loss(count_out[mask], y_visible[mask])

            loss_determined = F.nll_loss(determined_out[mask], determined[mask].long())
            return loss_count + lambda_determined * loss_determined

        # target_mode == 'censored'
        y_true = getattr(self.data, 'y_true', None)
        if y_true is None:
            raise ValueError("Data object is missing `y_true` for censored training.")

        # Supervise only where y_true is defined (>=0)
        det_mask = mask & (y_true >= 0)
        if det_mask.any():
            loss_det = F.nll_loss(count_out[det_mask], y_true[det_mask])
        else:
            loss_det = torch.tensor(0.0, device=self.device)

        # Inequality: predicted >= visible (on the remaining nodes)
        undet_mask = mask & ~det_mask
        if undet_mask.any():
            pred = count_out[undet_mask].argmax(dim=1)
            censor_penalty = F.relu(y_visible[undet_mask].float() - pred.float()).mean()
        else:
            censor_penalty = torch.tensor(0.0, device=self.device)

        return loss_det + lambda_censor * censor_penalty
    
    def _compute_coral_loss(
        self,
        logits: torch.Tensor,
        labels: torch.Tensor,
        mask: torch.Tensor,
    ) -> torch.Tensor:
        """
        Compute CORAL ordinal regression loss (binary cross-entropy per threshold).
        
        For each sample with label y, create binary targets:
        - target[k] = 1 if y > k, else 0
        
        Then compute BCE loss for each threshold independently.
        
        Args:
            logits: Threshold logits [num_nodes, num_thresholds] (before sigmoid)
            labels: Ordinal class labels [num_nodes] in [0, num_classes-1]
            mask: Boolean mask for which nodes to include in loss
            
        Returns:
            Scalar loss tensor
        """
        num_thresholds = logits.shape[1]  # K-1 for K-class ordinal
        
        # Build binary targets: [N, num_thresholds] where target[i,k] = 1 if label[i] > k
        targets = torch.zeros_like(logits)
        for k in range(num_thresholds):
            targets[:, k] = (labels > k).float()
        
        # BCE with logits (numerically stable)
        loss = F.binary_cross_entropy_with_logits(
            logits[mask], targets[mask], reduction='mean'
        )
        return loss
    
    def _get_coral_predictions(self, logits: torch.Tensor) -> torch.Tensor:
        """
        Convert CORAL logits to class predictions.
        
        Prediction is the count of thresholds exceeded (sigmoid > 0.5).
        
        Args:
            logits: Threshold logits [num_nodes, num_thresholds]
            
        Returns:
            Class predictions [num_nodes] in [0, num_classes-1]
        """
        probs = torch.sigmoid(logits)
        return (probs > 0.5).sum(dim=-1).long()
    
    def _compute_emd_loss(
        self,
        logits: torch.Tensor,
        labels: torch.Tensor,
        mask: torch.Tensor,
        class_weights: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """
        Compute Earth Mover's Distance loss for ordinal classification.
        
        EMD penalizes predictions based on distance from true class, not just correctness.
        This is better for ordinal targets where predicting class 2 when true is 3
        should be penalized less than predicting class 0.
        
        EMD = sum of |CDF_pred - CDF_true| (L1 distance between cumulative distributions)
        
        Args:
            logits: Raw model output [num_nodes, num_classes] (before softmax)
            labels: True class labels [num_nodes]
            mask: Boolean mask for which nodes to include
            class_weights: Optional per-class weights (not directly used in EMD,
                          but can be used to weight samples by class)
            
        Returns:
            Scalar EMD loss
        """
        # Get probabilities from logits
        probs = F.softmax(logits[mask], dim=-1)  # [N_masked, K]
        
        # One-hot encode labels
        targets = F.one_hot(labels[mask], num_classes=self.num_classes).float()  # [N_masked, K]
        
        # Compute cumulative distributions
        cdf_pred = probs.cumsum(dim=-1)   # [N_masked, K]
        cdf_true = targets.cumsum(dim=-1)  # [N_masked, K]
        
        # EMD = sum of |CDF_pred - CDF_true| per sample, then mean across samples
        # This is the L1 Wasserstein distance for 1D ordinal distributions
        emd_per_sample = (cdf_pred - cdf_true).abs().sum(dim=-1)  # [N_masked]
        
        # Optionally weight by class (inverse frequency reweighting)
        if class_weights is not None:
            sample_weights = class_weights[labels[mask]]
            emd = (emd_per_sample * sample_weights).mean()
        else:
            emd = emd_per_sample.mean()
        
        return emd
    
    def _compute_focal_loss(
        self,
        logits: torch.Tensor,
        labels: torch.Tensor,
        mask: torch.Tensor,
        class_weights: Optional[torch.Tensor] = None,
        gamma: float = 2.0,
    ) -> torch.Tensor:
        """
        Compute focal loss for class imbalance.
        
        Focal loss: FL(p) = -α(1-p)^γ * log(p)
        
        This down-weights easy examples (majority class) and focuses on hard ones.
        γ=2 is typical; higher values focus more on hard examples.
        
        Args:
            logits: Raw model output [num_nodes, num_classes] (before softmax)
            labels: True class labels [num_nodes]
            mask: Boolean mask for which nodes to include
            class_weights: Optional per-class weights (alpha in focal loss)
            gamma: Focusing parameter (default: 2.0)
            
        Returns:
            Scalar focal loss
        """
        masked_logits = logits[mask]
        masked_labels = labels[mask]
        
        probs = F.softmax(masked_logits, dim=1)
        ce_loss = F.cross_entropy(masked_logits, masked_labels, reduction='none')
        
        # Get probability for true class
        p_t = probs.gather(1, masked_labels.unsqueeze(1)).squeeze(1)
        
        # Focal weight: (1 - p_t)^gamma
        focal_weight = (1 - p_t) ** gamma
        
        # Apply class weights if provided
        if class_weights is not None:
            alpha_t = class_weights.gather(0, masked_labels)
            focal_weight = focal_weight * alpha_t
        
        loss = (focal_weight * ce_loss).mean()
        return loss
    
    def _evaluate(self, model: nn.Module, wNum: Optional[torch.Tensor] = None) -> Tuple[float, float]:
        """
        Evaluate model on train and test sets (weighted F1 for backward compatibility).
        
        Args:
            model: Model to evaluate
            wNum: Node depth values (for depth-aware models)
            
        Returns:
            Tuple of (train_f1, test_f1)
        """
        model.eval()
        is_coral = isinstance(model, CORALClassifier)
        with torch.no_grad():
            out = self._forward(model, wNum)
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)
            
            y_true_train = self.data.y[self.train_mask].cpu().numpy()
            y_pred_train = pred[self.train_mask].cpu().numpy()
            
            y_true_test = self.data.y[self.test_mask].cpu().numpy()
            y_pred_test = pred[self.test_mask].cpu().numpy()
            
            train_f1 = f1_score(y_true_train, y_pred_train, average='weighted', zero_division=0)
            test_f1 = f1_score(y_true_test, y_pred_test, average='weighted', zero_division=0)
        
        return train_f1, test_f1
    
    def _evaluate_full(
        self,
        model: nn.Module,
        wNum: Optional[torch.Tensor] = None,
        target_mode: str = TARGET_MODE,
    ) -> Dict:
        """
        Full evaluation with multiple metrics for imbalanced data.
        
        Args:
            model: Model to evaluate
            wNum: Node depth values (for depth-aware models)
            
        Returns:
            Dictionary with weighted F1, macro F1, balanced accuracy for train/val/test
        """
        model.eval()
        is_coral = isinstance(model, CORALClassifier)
        with torch.no_grad():
            out = self._forward(model, wNum)
            if isinstance(out, tuple):
                out = out[0]  # evaluate visible-count head by default
            
            # CORAL outputs logits -> predictions via threshold counting
            # Standard models output log_softmax -> predictions via argmax
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)
            
            # Train metrics
            y_true_train = self.data.y[self.train_mask].cpu().numpy()
            y_pred_train = pred[self.train_mask].cpu().numpy()
            
            # Validation metrics
            y_true_val = self.data.y[self.val_mask].cpu().numpy()
            y_pred_val = pred[self.val_mask].cpu().numpy()
            
            # Test metrics
            y_true_test = self.data.y[self.test_mask].cpu().numpy()
            y_pred_test = pred[self.test_mask].cpu().numpy()
            
            # Weighted F1 (traditional metric)
            train_weighted_f1 = f1_score(y_true_train, y_pred_train, average='weighted', zero_division=0)
            val_weighted_f1 = f1_score(y_true_val, y_pred_val, average='weighted', zero_division=0)
            test_weighted_f1 = f1_score(y_true_test, y_pred_test, average='weighted', zero_division=0)
            
            # Macro F1 (treats all classes equally - better for imbalanced data)
            train_macro_f1 = f1_score(y_true_train, y_pred_train, average='macro', zero_division=0)
            val_macro_f1 = f1_score(y_true_val, y_pred_val, average='macro', zero_division=0)
            test_macro_f1 = f1_score(y_true_test, y_pred_test, average='macro', zero_division=0)
            
            # Balanced accuracy (average of per-class recall)
            train_balanced_acc = balanced_accuracy_score(y_true_train, y_pred_train)
            val_balanced_acc = balanced_accuracy_score(y_true_val, y_pred_val)
            test_balanced_acc = balanced_accuracy_score(y_true_test, y_pred_test)
        
        return {
            'train_weighted_f1': train_weighted_f1,
            'val_weighted_f1': val_weighted_f1,
            'test_weighted_f1': test_weighted_f1,
            'train_macro_f1': train_macro_f1,
            'val_macro_f1': val_macro_f1,
            'test_macro_f1': test_macro_f1,
            'train_balanced_acc': train_balanced_acc,
            'val_balanced_acc': val_balanced_acc,
            'test_balanced_acc': test_balanced_acc,
        }
    
    def _compute_mae(self, model: nn.Module, wNum: Optional[torch.Tensor] = None) -> Tuple[float, float, float]:
        """
        Compute Mean Absolute Error for root count prediction.
        
        Since we're predicting counts, MAE gives interpretable error in terms
        of "how many roots off" the prediction is on average.
        
        Args:
            model: Model to evaluate
            wNum: Node depth values (for depth-aware models)
            
        Returns:
            Tuple of (train_mae, val_mae, test_mae)
        """
        model.eval()
        is_coral = isinstance(model, CORALClassifier)
        with torch.no_grad():
            out = self._forward(model, wNum)
            if isinstance(out, tuple):
                out = out[0]
            
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)
            
            y_true_train = self.data.y[self.train_mask].cpu().numpy()
            y_pred_train = pred[self.train_mask].cpu().numpy()
            
            y_true_val = self.data.y[self.val_mask].cpu().numpy()
            y_pred_val = pred[self.val_mask].cpu().numpy()
            
            y_true_test = self.data.y[self.test_mask].cpu().numpy()
            y_pred_test = pred[self.test_mask].cpu().numpy()
            
            train_mae = np.mean(np.abs(y_true_train - y_pred_train))
            val_mae = np.mean(np.abs(y_true_val - y_pred_val))
            test_mae = np.mean(np.abs(y_true_test - y_pred_test))
        
        return train_mae, val_mae, test_mae
    
    def cross_validate(
        self,
        model_name: str = 'depth_aware_gat',
        weight_decay: float = 0.0625,
        n_folds: int = VALIDATION_FOLDS,
        target_mode: str = TARGET_MODE,
        lambda_determined: float = 1.0,
        lambda_censor: float = 1.0,
        **kwargs,
    ) -> Dict:
        """
        Perform k-fold cross-validation on the training+validation set.
        
        Uses macro F1 for evaluation (better for imbalanced data).
        FIX: Now recomputes class weights per fold and passes kwargs to model.
        
        Args:
            model_name: Model architecture to use
            weight_decay: L2 regularization strength
            n_folds: Number of CV folds
            **kwargs: Additional arguments for training/model
            
        Returns:
            Dictionary with CV results
        """
        kfold = KFold(n_splits=n_folds, shuffle=True, random_state=self.random_seed)
        fold_macro_f1_scores = []
        fold_weighted_f1_scores = []
        
        # Use fewer epochs for CV to speed up grid search
        cv_epochs = kwargs.get('max_epochs', 50)
        verbose = kwargs.get('verbose', False)
        
        # Separate model kwargs from training kwargs
        training_kwargs = {'max_epochs', 'verbose', 'learning_rate', 'target_mode', 'lambda_determined', 'lambda_censor'}
        model_kwargs = {k: v for k, v in kwargs.items() if k not in training_kwargs}
        
        # Get wNum for depth-aware models
        wNum = getattr(self.data, 'wNum', None)
        if wNum is not None:
            wNum = wNum.to(self.device)
        
        # Use train+val indices for CV (test set is held out)
        cv_indices = self.train_val_indices
        
        for fold, (train_cv_idx, val_cv_idx) in enumerate(kfold.split(cv_indices)):
            if verbose:
                print(f"    Fold {fold + 1}/{n_folds}...", end='', flush=True)
            
            # Create CV masks
            train_cv_mask = torch.zeros(
                self.data.num_nodes, dtype=torch.bool, device=self.device
            )
            val_cv_mask = torch.zeros(
                self.data.num_nodes, dtype=torch.bool, device=self.device
            )
            
            train_cv_mask[cv_indices[train_cv_idx]] = True
            val_cv_mask[cv_indices[val_cv_idx]] = True
            
            # FIX: Recompute class weights from this fold's training data
            if self.use_class_weights:
                fold_weights = compute_class_weights(
                    self.data.y[train_cv_mask], self.num_classes
                ).to(self.device)
            else:
                fold_weights = None
            
            # FIX: Pass model_kwargs and num_classes to get_model
            model = get_model(model_name, num_classes=self.num_classes, **model_kwargs)
            model = model.to(self.device)
            is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
            is_coral = isinstance(model, CORALClassifier)
            
            optimizer = Adam(
                model.parameters(),
                lr=kwargs.get('learning_rate', LEARNING_RATE),
                weight_decay=weight_decay,
            )
            
            for epoch in range(cv_epochs):
                model.train()
                optimizer.zero_grad()
                
                # Forward pass
                if is_depth_aware and wNum is not None:
                    out = model(self.data.x, self.data.edge_index, wNum)
                else:
                    out = model(self.data.x, self.data.edge_index)
                
                # CORAL uses separate loss; respect target_mode
                if is_coral:
                    if target_mode == 'censored':
                        y_coral_cv = self.data.y
                    else:
                        y_coral_cv = getattr(self.data, 'y_visible', self.data.y)
                    loss = self._compute_coral_loss(out, y_coral_cv, train_cv_mask)
                else:
                    loss = self._compute_loss(
                        out=out,
                        mask=train_cv_mask,
                        class_weights=fold_weights,
                        target_mode=target_mode,
                        lambda_determined=lambda_determined,
                        lambda_censor=lambda_censor,
                    )
                
                loss.backward()
                optimizer.step()
            
            # Evaluate on validation fold
            model.eval()
            with torch.no_grad():
                if is_depth_aware and wNum is not None:
                    out = model(self.data.x, self.data.edge_index, wNum)
                else:
                    out = model(self.data.x, self.data.edge_index)
                if isinstance(out, tuple):
                    out = out[0]
                
                if is_coral:
                    pred = self._get_coral_predictions(out)
                else:
                    pred = out.argmax(dim=1)
                
                # CV scores are computed on visible labels (consistent target across all nodes)
                y_visible = getattr(self.data, 'y_visible', self.data.y)
                y_true = y_visible[val_cv_mask].cpu().numpy()
                y_pred = pred[val_cv_mask].cpu().numpy()
                
                # Use macro F1 as primary metric for imbalanced data
                val_macro_f1 = f1_score(y_true, y_pred, average='macro', zero_division=0)
                val_weighted_f1 = f1_score(y_true, y_pred, average='weighted', zero_division=0)
                
                fold_macro_f1_scores.append(val_macro_f1)
                fold_weighted_f1_scores.append(val_weighted_f1)
                
            if verbose:
                print(f" Macro F1={val_macro_f1:.4f}")
        
        return {
            'mean_f1': np.mean(fold_macro_f1_scores),  # Use macro F1 as primary
            'std_f1': np.std(fold_macro_f1_scores),
            'mean_weighted_f1': np.mean(fold_weighted_f1_scores),
            'fold_scores': fold_macro_f1_scores,
            'weight_decay': weight_decay,
        }
    
    def grid_search(
        self,
        model_name: str = 'depth_aware_gat',
        penalties: List[float] = PENALTIES,
        n_folds: int = VALIDATION_FOLDS,
        verbose: bool = True,
        target_mode: str = TARGET_MODE,
        lambda_determined: float = 1.0,
        lambda_censor: float = 1.0,
        **kwargs,
    ) -> Dict:
        """
        Perform grid search over penalty values using cross-validation.
        
        Args:
            model_name: Model architecture to use
            penalties: List of weight_decay values to try
            n_folds: Number of CV folds
            verbose: Whether to print progress
            **kwargs: Additional arguments for training
            
        Returns:
            Dictionary with best parameters and all results
        """
        results = []
        best_penalty = None
        best_f1 = 0
        
        for penalty in penalties:
            if verbose:
                print(f"Testing penalty={penalty}...")
            
            # Pass verbose to cross_validate for fold-level feedback
            cv_result = self.cross_validate(
                model_name=model_name,
                weight_decay=penalty,
                n_folds=n_folds,
                verbose=verbose,
                target_mode=target_mode,
                lambda_determined=lambda_determined,
                lambda_censor=lambda_censor,
                **kwargs,
            )
            results.append(cv_result)
            
            if verbose:
                print(f"  Mean F1: {cv_result['mean_f1']:.4f} ± {cv_result['std_f1']:.4f}")
            
            if cv_result['mean_f1'] > best_f1:
                best_f1 = cv_result['mean_f1']
                best_penalty = penalty
        
        if verbose:
            print(f"\nBest penalty: {best_penalty} with F1: {best_f1:.4f}")
        
        return {
            'best_penalty': best_penalty,
            'best_f1': best_f1,
            'all_results': results,
        }
    
    def get_prediction_distribution(
        self,
        model: nn.Module,
    ) -> Dict[str, torch.Tensor]:
        """
        Get prediction counts per class for debugging class collapse.
        
        If certain classes have 0 predictions, the model is collapsing.
        
        Args:
            model: Trained model
            
        Returns:
            Dictionary with prediction counts for train/val/test sets
        """
        wNum = getattr(self.data, 'wNum', None)
        if wNum is not None:
            wNum = wNum.to(self.device)
        
        is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
        is_coral = isinstance(model, CORALClassifier)
        
        model.eval()
        with torch.no_grad():
            if is_depth_aware and wNum is not None:
                out = model(self.data.x, self.data.edge_index, wNum)
            else:
                out = model(self.data.x, self.data.edge_index)
            if isinstance(out, tuple):
                out = out[0]
            
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)
            
            train_pred = pred[self.train_mask]
            val_pred = pred[self.val_mask]
            test_pred = pred[self.test_mask]
            
            train_counts = torch.bincount(train_pred, minlength=self.num_classes)
            val_counts = torch.bincount(val_pred, minlength=self.num_classes)
            test_counts = torch.bincount(test_pred, minlength=self.num_classes)
        
        return {
            'train': train_counts,
            'val': val_counts,
            'test': test_counts,
        }
    
    def print_prediction_diagnostics(self, model: nn.Module) -> None:
        """
        Print prediction distribution diagnostics.
        
        This helps identify class collapse (when certain classes are never predicted).
        """
        dist = self.get_prediction_distribution(model)
        
        print("\nPrediction Distribution (check for class collapse):")
        print("-" * 60)
        print(f"{'Class':<12} {'Train':>10} {'Val':>10} {'Test':>10}")
        print("-" * 60)
        
        for i, name in enumerate(self.class_names):
            train_count = dist['train'][i].item()
            val_count = dist['val'][i].item()
            test_count = dist['test'][i].item()
            
            # Mark if class is never predicted (collapse warning)
            warning = " ⚠️ COLLAPSED" if (train_count == 0 and test_count == 0) else ""
            print(f"{name:<12} {train_count:>10} {val_count:>10} {test_count:>10}{warning}")
        
        print("-" * 60)
        print(f"{'Total':<12} {dist['train'].sum().item():>10} {dist['val'].sum().item():>10} {dist['test'].sum().item():>10}")
    
    def get_confusion_matrix(
        self,
        model: nn.Module,
        split: str = 'test',
    ) -> np.ndarray:
        """
        Compute confusion matrix for model predictions.
        
        Handles both classification models (argmax) and regression models (round).
        
        Args:
            model: Trained model (classifier or regression)
            split: Which split to evaluate ('train', 'val', 'test')
            
        Returns:
            Confusion matrix as numpy array [true_class, pred_class]
        """
        wNum = getattr(self.data, 'wNum', None)
        if wNum is not None:
            wNum = wNum.to(self.device)
        
        is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
        is_regression = isinstance(model, RegressionClassifier)
        is_coral = isinstance(model, CORALClassifier)
        
        # Select mask for split
        if split == 'train':
            mask = self.train_mask
        elif split == 'val':
            mask = self.val_mask
        else:
            mask = self.test_mask
        
        model.eval()
        with torch.no_grad():
            if is_regression:
                # Regression model: round output to get class
                out = model(self.data.x, self.data.edge_index).squeeze(-1)
                pred = out.round().clamp(0, self.num_classes - 1).long()
            elif is_coral:
                # CORAL model: count thresholds exceeded
                out = model(self.data.x, self.data.edge_index)
                pred = self._get_coral_predictions(out)
            elif is_depth_aware and wNum is not None:
                out = model(self.data.x, self.data.edge_index, wNum)
                if isinstance(out, tuple):
                    out = out[0]
                pred = out.argmax(dim=1)
            else:
                out = model(self.data.x, self.data.edge_index)
                if isinstance(out, tuple):
                    out = out[0]
                pred = out.argmax(dim=1)
            
            y_true = self.data.y[mask].cpu().numpy()
            y_pred = pred[mask].cpu().numpy()
        
        return confusion_matrix(y_true, y_pred, labels=list(range(self.num_classes)))
    
    def print_confusion_matrix(
        self,
        model: nn.Module,
        split: str = 'test',
        normalize: bool = True,
    ) -> None:
        """
        Pretty-print confusion matrix showing where each class goes.
        
        Rows = true class, Columns = predicted class.
        If normalized, shows percentage of each true class going to each prediction.
        
        Args:
            model: Trained model
            split: Which split to evaluate ('train', 'val', 'test')
            normalize: If True, normalize by row (shows % of true class)
        """
        cm = self.get_confusion_matrix(model, split)
        
        print(f"\nConfusion Matrix ({split} set):")
        print("Rows = True class, Columns = Predicted class")
        if normalize:
            print("(Values show % of true class going to each prediction)")
        print("-" * 70)
        
        # Header
        header = f"{'True \\ Pred':<12}"
        for name in self.class_names:
            header += f"{name:>12}"
        print(header)
        print("-" * 70)
        
        # Rows
        for i, true_name in enumerate(self.class_names):
            row = f"{true_name:<12}"
            row_sum = cm[i].sum()
            
            for j in range(self.num_classes):
                if normalize and row_sum > 0:
                    pct = 100 * cm[i, j] / row_sum
                    # Highlight diagonal (correct) vs off-diagonal (errors)
                    if i == j:
                        row += f"{pct:>11.1f}%"
                    elif cm[i, j] > 0:
                        row += f"{pct:>11.1f}%"
                    else:
                        row += f"{'0':>12}"
                else:
                    row += f"{cm[i, j]:>12}"
            
            # Add row total
            row += f"  (n={row_sum})"
            print(row)
        
        print("-" * 70)
        
        # Show which errors are ordinal (neighbor class) vs non-ordinal
        ordinal_errors = 0
        non_ordinal_errors = 0
        total_errors = 0
        
        for i in range(self.num_classes):
            for j in range(self.num_classes):
                if i != j:
                    errors = cm[i, j]
                    total_errors += errors
                    if abs(i - j) == 1:
                        ordinal_errors += errors
                    else:
                        non_ordinal_errors += errors
        
        if total_errors > 0:
            print(f"\nError Analysis:")
            print(f"  Neighbor class errors (|true-pred|=1): {ordinal_errors} ({100*ordinal_errors/total_errors:.1f}%)")
            print(f"  Distant class errors (|true-pred|>1):  {non_ordinal_errors} ({100*non_ordinal_errors/total_errors:.1f}%)")
    
    def get_detailed_report(
        self,
        model: nn.Module,
        class_names: Optional[List[str]] = None,
    ) -> str:
        """
        Generate a detailed classification report for root count prediction.
        
        Args:
            model: Trained model
            class_names: Names for the classes (defaults to self.class_names)
            
        Returns:
            Classification report string
        """
        if class_names is None:
            class_names = self.class_names
        
        # Get wNum for depth-aware models
        wNum = getattr(self.data, 'wNum', None)
        if wNum is not None:
            wNum = wNum.to(self.device)
        
        is_depth_aware = isinstance(model, (DepthAwareGAT, DepthAwareGATv2, FocalLossClassifier, MultiTaskRootClassifier))
        is_coral = isinstance(model, CORALClassifier)
            
        model.eval()
        with torch.no_grad():
            if is_depth_aware and wNum is not None:
                out = model(self.data.x, self.data.edge_index, wNum)
            else:
                out = model(self.data.x, self.data.edge_index)
            if isinstance(out, tuple):
                out = out[0]
            
            if is_coral:
                pred = self._get_coral_predictions(out)
            else:
                pred = out.argmax(dim=1)
            
            y_true = self.data.y[self.test_mask].cpu().numpy()
            y_pred = pred[self.test_mask].cpu().numpy()
        
        # Get unique labels present in test set
        labels_present = sorted(set(y_true) | set(y_pred))
        names_present = [class_names[i] if i < len(class_names) else f'{i} roots' 
                         for i in labels_present]
        
        return classification_report(
            y_true, y_pred, 
            labels=labels_present,
            target_names=names_present,
            zero_division=0
        )


def train_and_evaluate(
    data: Data,
    model_name: str = 'depth_aware_gat',
    run_grid_search: bool = True,
    use_class_weights: bool = True,
    verbose: bool = True,
    target_mode: str = TARGET_MODE,
    lambda_determined: float = 1.0,
    lambda_censor: float = 1.0,
) -> Tuple[nn.Module, Dict]:
    """
    Convenience function for full root count prediction training pipeline.
    
    IMPROVED: Uses class weights and reports macro F1 for imbalanced data.
    
    Args:
        data: PyG Data object
        model_name: Model architecture to use
        run_grid_search: Whether to run grid search for best penalty
        use_class_weights: Whether to use inverse frequency class weights
        verbose: Whether to print progress
        
    Returns:
        Tuple of (trained model, metrics dictionary)
        
    Example:
        >>> model, metrics = train_and_evaluate(data, model_name='depth_aware_gat')
        >>> print(f"Test Macro F1: {metrics['test_macro_f1']:.4f}")
    """
    trainer = NodeClassificationTrainer(data, use_class_weights=use_class_weights)
    
    if run_grid_search:
        # Find best hyperparameters
        grid_result = trainer.grid_search(
            model_name=model_name,
            verbose=verbose,
            target_mode=target_mode,
            lambda_determined=lambda_determined,
            lambda_censor=lambda_censor,
        )
        best_penalty = grid_result['best_penalty']
    else:
        best_penalty = 0.0625  # Default
    
    # Train final model
    if verbose:
        print(f"\nTraining final model with penalty={best_penalty}")
    
    model, metrics = trainer.train(
        model_name=model_name,
        weight_decay=best_penalty,
        verbose=verbose,
        target_mode=target_mode,
        lambda_determined=lambda_determined,
        lambda_censor=lambda_censor,
    )
    
    if verbose:
        print(f"\nFinal Results (Root Count Prediction - FIXED):")
        print(f"  Train Macro F1: {metrics['train_macro_f1']:.4f}")
        print(f"  Val Macro F1:   {metrics['val_macro_f1']:.4f} (used for early stopping)")
        print(f"  Test Macro F1:  {metrics['test_macro_f1']:.4f} (final held-out evaluation)")
        print(f"")
        print(f"  Train Balanced Acc: {metrics['train_balanced_acc']:.4f}")
        print(f"  Val Balanced Acc:   {metrics['val_balanced_acc']:.4f}")
        print(f"  Test Balanced Acc:  {metrics['test_balanced_acc']:.4f}")
        print(f"")
        print(f"  Train MAE: {metrics['train_mae']:.4f} roots")
        print(f"  Val MAE:   {metrics['val_mae']:.4f} roots")
        print(f"  Test MAE:  {metrics['test_mae']:.4f} roots")
        print(f"\n  (Weighted metrics for comparison:)")
        print(f"  Train/Val/Test Weighted F1: {metrics['train_f1']:.4f} / {metrics['val_f1']:.4f} / {metrics['test_f1']:.4f}")
        
        # Print prediction distribution diagnostics
        trainer.print_prediction_diagnostics(model)
        
        # Print confusion matrix (shows WHERE misclassifications go)
        trainer.print_confusion_matrix(model, split='test', normalize=True)
        
        print(f"\nDetailed Report (Test Set):")
        print(trainer.get_detailed_report(model))
    
    return model, metrics

