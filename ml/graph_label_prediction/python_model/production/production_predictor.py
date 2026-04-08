"""
Production Predictor for Root Count Prediction.

This module provides a complete prediction pipeline that:
1. Computes embeddings from the shared backbone
2. Gets predictions from CORAL and Focal heads
3. Routes predictions based on uncertainty
4. Detects drift and flags out-of-distribution samples
5. Provides explanations via nearest neighbors

Usage:
    predictor = ProductionPredictor(model, regime_detector, router)
    
    # Single prediction with full output
    output = predictor.predict(data)
    print(f"Predictions: {output.predictions}")
    print(f"Confidence: {output.confidence}")
    print(f"Deferred: {output.defer_mask.sum()}")
    
    # Get explanations for uncertain predictions
    explanations = predictor.explain_predictions(data, output)
"""

from dataclasses import dataclass
from typing import Optional, Dict, List, Tuple, Any

import numpy as np
import torch
import torch.nn.functional as F
from torch_geometric.data import Data

from .production_model import ProductionRootClassifier, PredictionOutput
from .uncertainty_router import (
    UncertaintyRouter, 
    RoutingConfig, 
    RoutingOutput,
    RoutingDecision,
)
from .regime_detector import RegimeDetector, RegimeDetectionOutput


@dataclass
class ProductionPredictionOutput:
    """Complete output from production prediction pipeline."""
    
    # Core predictions
    predictions: torch.Tensor  # Final class predictions [N]
    probabilities: torch.Tensor  # Class probabilities [N, num_classes]
    confidence: torch.Tensor  # Confidence scores [N]
    expected_value: torch.Tensor  # E[y] from CORAL [N]
    
    # Routing information
    decisions: torch.Tensor  # Routing decisions [N]
    defer_mask: torch.Tensor  # Deferred predictions [N]
    ordinal_mask: torch.Tensor  # Used CORAL [N]
    focal_mask: torch.Tensor  # Used Focal [N]
    
    # Regime/drift detection
    regime_distance: Optional[torch.Tensor]  # Distance to regime centroid [N]
    is_drift: Optional[torch.Tensor]  # Drift flag [N]
    nearest_regime: Optional[torch.Tensor]  # Predicted regime [N]
    
    # Embeddings (for analysis/explanation)
    embeddings: torch.Tensor  # Node embeddings [N, dim]
    
    # Individual head outputs
    coral_probs: torch.Tensor  # CORAL class probabilities [N, K]
    focal_probs: torch.Tensor  # Focal class probabilities [N, K]


@dataclass
class PredictionExplanation:
    """Explanation for a single prediction."""
    node_idx: int
    node_id: Any  # Original node ID if available
    prediction: int
    confidence: float
    expected_value: float
    decision: str  # 'defer', 'ordinal', 'focal'
    regime_distance: Optional[float]
    is_drift: bool
    neighbors: List[Dict]  # List of {node_id, label, distance}


class ProductionPredictor:
    """
    Production prediction pipeline with uncertainty routing.
    
    Combines:
    - ProductionRootClassifier for predictions
    - UncertaintyRouter for routing decisions
    - RegimeDetector for drift monitoring and explanation
    
    Args:
        model: Trained ProductionRootClassifier
        regime_detector: Fitted RegimeDetector (optional)
        router: Configured UncertaintyRouter (optional)
        device: Device to use
    """
    
    def __init__(
        self,
        model: ProductionRootClassifier,
        regime_detector: Optional[RegimeDetector] = None,
        router: Optional[UncertaintyRouter] = None,
        device: Optional[str] = None,
    ):
        self.model = model
        self.regime_detector = regime_detector
        self.router = router or UncertaintyRouter()
        
        if device is None:
            self.device = next(model.parameters()).device
        else:
            self.device = device
        
        self.model = self.model.to(self.device)
        self.model.eval()
    
    def predict(
        self,
        data: Data,
        detect_drift: bool = True,
        drift_threshold: Optional[float] = None,
    ) -> ProductionPredictionOutput:
        """
        Make predictions with full routing pipeline.
        
        Args:
            data: PyG Data object with node features and edges
            detect_drift: Whether to run drift detection
            drift_threshold: Override drift threshold
            
        Returns:
            ProductionPredictionOutput with all prediction information
        """
        data = data.to(self.device)
        
        with torch.no_grad():
            # Get embeddings and head outputs
            h, coral_logits, focal_logits = self.model(
                data.x, data.edge_index, return_all=True
            )
            
            # Convert to probabilities
            coral_probs = self.model.coral_head.get_class_probs(h)
            coral_expected = self.model.coral_head.get_expected_value(h)
            focal_probs = F.softmax(focal_logits, dim=-1)
            
            # Regime detection if available
            regime_distance = None
            nearest_regime = None
            is_drift = None
            
            if self.regime_detector is not None:
                h_np = h.cpu().numpy()
                
                # Detect regime
                regime_output = self.regime_detector.detect_regime(
                    h_np, return_all=True
                )
                nearest_regime = torch.tensor(
                    regime_output.nearest_regime, device=self.device
                )
                regime_distance = torch.tensor(
                    regime_output.distance_to_regime, device=self.device
                )
                
                # Detect drift
                if detect_drift:
                    drift_mask, _ = self.regime_detector.detect_drift(
                        h_np, threshold=drift_threshold
                    )
                    is_drift = torch.tensor(drift_mask, device=self.device)
            
            # Route predictions
            routing_output = self.router.route(
                coral_probs=coral_probs,
                coral_expected=coral_expected,
                focal_probs=focal_probs,
                regime_distance=regime_distance,
            )
        
        return ProductionPredictionOutput(
            predictions=routing_output.predictions,
            probabilities=routing_output.probabilities,
            confidence=routing_output.confidence,
            expected_value=routing_output.expected_value,
            decisions=routing_output.decisions,
            defer_mask=routing_output.defer_mask,
            ordinal_mask=routing_output.ordinal_mask,
            focal_mask=routing_output.focal_mask,
            regime_distance=regime_distance,
            is_drift=is_drift,
            nearest_regime=nearest_regime,
            embeddings=h,
            coral_probs=coral_probs,
            focal_probs=focal_probs,
        )
    
    def predict_batch(
        self,
        data: Data,
        batch_size: int = 1000,
    ) -> ProductionPredictionOutput:
        """
        Make predictions in batches for large graphs.
        
        Note: For full-batch GNN inference, this still processes the
        entire graph but returns results in chunks to save memory.
        
        Args:
            data: PyG Data object
            batch_size: Number of nodes per batch for output
            
        Returns:
            ProductionPredictionOutput
        """
        # For now, just call predict directly
        # Future: implement true mini-batch inference with NeighborLoader
        return self.predict(data)
    
    def explain_predictions(
        self,
        data: Data,
        output: ProductionPredictionOutput,
        k_neighbors: int = 5,
        indices: Optional[List[int]] = None,
    ) -> List[PredictionExplanation]:
        """
        Generate explanations for predictions.
        
        Args:
            data: PyG Data object
            output: ProductionPredictionOutput from predict()
            k_neighbors: Number of neighbors for explanation
            indices: Specific node indices to explain (default: deferred)
            
        Returns:
            List of PredictionExplanation objects
        """
        if self.regime_detector is None:
            raise RuntimeError(
                "RegimeDetector required for explanations. "
                "Fit regime_detector on training data first."
            )
        
        # Default: explain deferred predictions
        if indices is None:
            indices = output.defer_mask.nonzero().squeeze(-1).cpu().tolist()
            if isinstance(indices, int):
                indices = [indices]
        
        if len(indices) == 0:
            return []
        
        # Get node IDs if available
        node_ids = getattr(data, 'node_ids', None)
        
        explanations = []
        
        for idx in indices:
            # Get embedding
            embedding = output.embeddings[idx:idx+1].cpu().numpy()
            
            # Find neighbors
            neighbor_indices, distances, neighbor_labels, neighbor_ids = \
                self.regime_detector.explain(embedding, k=k_neighbors)
            
            # Build neighbor list
            neighbors = []
            for i in range(k_neighbors):
                neighbors.append({
                    'node_id': neighbor_ids[0, i],
                    'label': int(neighbor_labels[0, i]),
                    'distance': float(distances[0, i]),
                })
            
            # Get decision string
            decision_int = output.decisions[idx].item()
            decision = {0: 'defer', 1: 'ordinal', 2: 'focal'}[decision_int]
            
            explanations.append(PredictionExplanation(
                node_idx=idx,
                node_id=node_ids[idx] if node_ids is not None else idx,
                prediction=output.predictions[idx].item(),
                confidence=output.confidence[idx].item(),
                expected_value=output.expected_value[idx].item(),
                decision=decision,
                regime_distance=(
                    output.regime_distance[idx].item() 
                    if output.regime_distance is not None else None
                ),
                is_drift=(
                    output.is_drift[idx].item() 
                    if output.is_drift is not None else False
                ),
                neighbors=neighbors,
            ))
        
        return explanations
    
    def get_prediction_stats(
        self,
        output: ProductionPredictionOutput,
    ) -> Dict:
        """
        Compute statistics about predictions.
        
        Args:
            output: ProductionPredictionOutput
            
        Returns:
            Dictionary with prediction statistics
        """
        N = output.predictions.shape[0]
        
        # Class distribution
        unique, counts = torch.unique(output.predictions, return_counts=True)
        class_dist = {int(u): int(c) for u, c in zip(unique.cpu(), counts.cpu())}
        
        stats = {
            'total_predictions': N,
            'class_distribution': class_dist,
            
            # Confidence stats
            'mean_confidence': output.confidence.mean().item(),
            'min_confidence': output.confidence.min().item(),
            'max_confidence': output.confidence.max().item(),
            'std_confidence': output.confidence.std().item(),
            
            # Routing stats
            'num_deferred': output.defer_mask.sum().item(),
            'num_ordinal': output.ordinal_mask.sum().item(),
            'num_focal': output.focal_mask.sum().item(),
            'pct_deferred': 100 * output.defer_mask.float().mean().item(),
            'pct_ordinal': 100 * output.ordinal_mask.float().mean().item(),
            'pct_focal': 100 * output.focal_mask.float().mean().item(),
            
            # Expected value stats
            'mean_expected_value': output.expected_value.mean().item(),
            'std_expected_value': output.expected_value.std().item(),
        }
        
        # Drift stats if available
        if output.is_drift is not None:
            stats['num_drift'] = output.is_drift.sum().item()
            stats['pct_drift'] = 100 * output.is_drift.float().mean().item()
        
        if output.regime_distance is not None:
            stats['mean_regime_distance'] = output.regime_distance.mean().item()
            stats['max_regime_distance'] = output.regime_distance.max().item()
        
        return stats
    
    def get_uncertain_predictions(
        self,
        output: ProductionPredictionOutput,
        threshold: Optional[float] = None,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Get indices and probabilities of uncertain predictions.
        
        Args:
            output: ProductionPredictionOutput
            threshold: Confidence threshold (default: use router's defer_threshold)
            
        Returns:
            Tuple of (indices, probabilities) for uncertain predictions
        """
        if threshold is None:
            threshold = self.router.config.defer_threshold
        
        uncertain_mask = output.confidence < threshold
        indices = uncertain_mask.nonzero().squeeze(-1)
        probs = output.probabilities[uncertain_mask]
        
        return indices, probs
    
    def compare_heads(
        self,
        output: ProductionPredictionOutput,
        labels: Optional[torch.Tensor] = None,
    ) -> Dict:
        """
        Compare CORAL vs Focal predictions.
        
        Args:
            output: ProductionPredictionOutput
            labels: Optional ground truth labels
            
        Returns:
            Dictionary with comparison statistics
        """
        coral_pred = output.coral_probs.argmax(dim=-1)
        focal_pred = output.focal_probs.argmax(dim=-1)
        
        # Agreement rate
        agreement = (coral_pred == focal_pred).float().mean().item()
        
        stats = {
            'agreement_rate': agreement,
            'coral_mean_confidence': output.coral_probs.max(dim=-1).values.mean().item(),
            'focal_mean_confidence': output.focal_probs.max(dim=-1).values.mean().item(),
        }
        
        # If labels provided, compute accuracies
        if labels is not None:
            labels = labels.to(self.device)
            stats['coral_accuracy'] = (coral_pred == labels).float().mean().item()
            stats['focal_accuracy'] = (focal_pred == labels).float().mean().item()
            stats['routed_accuracy'] = (output.predictions == labels).float().mean().item()
            
            # Agreement when correct
            both_correct = (coral_pred == labels) & (focal_pred == labels)
            stats['both_correct_rate'] = both_correct.float().mean().item()
            
            # Disagreement analysis
            disagree = coral_pred != focal_pred
            if disagree.any():
                coral_correct_when_disagree = (
                    (coral_pred[disagree] == labels[disagree]).float().mean().item()
                )
                focal_correct_when_disagree = (
                    (focal_pred[disagree] == labels[disagree]).float().mean().item()
                )
                stats['coral_correct_when_disagree'] = coral_correct_when_disagree
                stats['focal_correct_when_disagree'] = focal_correct_when_disagree
        
        return stats


def create_predictor(
    model: ProductionRootClassifier,
    regime_detector: Optional[RegimeDetector] = None,
    routing_config: Optional[RoutingConfig] = None,
) -> ProductionPredictor:
    """
    Factory function to create a ProductionPredictor.
    
    Args:
        model: Trained ProductionRootClassifier
        regime_detector: Optional fitted RegimeDetector
        routing_config: Optional RoutingConfig
        
    Returns:
        ProductionPredictor instance
    """
    router = UncertaintyRouter(routing_config) if routing_config else None
    return ProductionPredictor(
        model=model,
        regime_detector=regime_detector,
        router=router,
    )
