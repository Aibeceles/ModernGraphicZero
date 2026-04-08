"""
Uncertainty Router for Production Prediction System.

This module implements the routing logic that decides which prediction
head to trust based on confidence signals:

- CORAL (ordinal) predictions: Always computed, provide uncertainty
- Focal predictions: Only trusted when confidence is high
- Defer: Flag predictions for human review

Routing Rules:
1. If max_prob < defer_threshold → DEFER (flag for review)
2. If |E[y] - round(E[y])| < ordinal_margin → USE ORDINAL (boundary case)
3. If max_prob > focal_threshold AND regime stable → USE FOCAL
4. Otherwise → USE ORDINAL

The router is NOT trainable - it uses calibrated thresholds.
"""

from dataclasses import dataclass
from enum import IntEnum
from typing import Optional, Tuple, Dict

import torch
import torch.nn.functional as F


class RoutingDecision(IntEnum):
    """Enumeration of routing decisions."""
    DEFER = 0      # Low confidence, flag for review
    ORDINAL = 1    # Use CORAL ordinal prediction
    FOCAL = 2      # Use focal sharp prediction


@dataclass
class RoutingConfig:
    """Configuration for uncertainty router thresholds."""
    
    # Threshold below which predictions are deferred
    defer_threshold: float = 0.4
    
    # Margin around integer E[y] to prefer ordinal
    # If |E[y] - round(E[y])| < margin, use ordinal
    ordinal_margin: float = 0.15
    
    # Minimum confidence to consider focal predictions
    focal_confidence: float = 0.7
    
    # Maximum regime distance to trust focal predictions
    regime_distance_threshold: float = 2.0
    
    # Enable/disable focal gating (if False, always use ordinal)
    enable_focal: bool = True


@dataclass 
class RoutingOutput:
    """Structured output from routing decision."""
    
    # Final predictions [N]
    predictions: torch.Tensor
    
    # Final probabilities [N, num_classes]
    probabilities: torch.Tensor
    
    # Confidence scores [N]
    confidence: torch.Tensor
    
    # Expected value from CORAL [N]
    expected_value: torch.Tensor
    
    # Routing decisions [N] (0=defer, 1=ordinal, 2=focal)
    decisions: torch.Tensor
    
    # Per-decision masks for analysis
    defer_mask: torch.Tensor
    ordinal_mask: torch.Tensor
    focal_mask: torch.Tensor


class UncertaintyRouter:
    """
    Routes predictions between CORAL, Focal, and Defer based on confidence.
    
    The router implements a decision system that:
    1. CORAL is the safety layer (always provides calibrated uncertainty)
    2. Focal is an acceleration layer (sharp when confident)
    3. Defer flags low-confidence cases for human review
    
    Usage:
        router = UncertaintyRouter()
        output = router.route(coral_probs, coral_expected, focal_probs, regime_dist)
        
        # Access final predictions
        predictions = output.predictions
        confidence = output.confidence
        
        # Analyze routing decisions
        deferred = output.defer_mask.sum()
        used_focal = output.focal_mask.sum()
    
    Args:
        config: RoutingConfig with threshold values
    """
    
    def __init__(self, config: Optional[RoutingConfig] = None):
        self.config = config or RoutingConfig()
    
    def route(
        self,
        coral_probs: torch.Tensor,
        coral_expected: torch.Tensor,
        focal_probs: torch.Tensor,
        regime_distance: Optional[torch.Tensor] = None,
    ) -> RoutingOutput:
        """
        Route predictions based on confidence signals.
        
        Args:
            coral_probs: Class probabilities from CORAL [N, num_classes]
            coral_expected: Expected value E[y] from CORAL [N]
            focal_probs: Class probabilities from Focal [N, num_classes]
            regime_distance: Distance to regime centroid [N] (optional)
            
        Returns:
            RoutingOutput with predictions, confidence, and decision masks
        """
        device = coral_probs.device
        N = coral_probs.shape[0]
        num_classes = coral_probs.shape[1]
        
        # Compute confidence signals
        coral_confidence = coral_probs.max(dim=-1).values  # [N]
        focal_confidence = focal_probs.max(dim=-1).values  # [N]
        
        # Fractional part of E[y] (distance to nearest integer)
        fractional_part = torch.abs(coral_expected - coral_expected.round())
        
        # Initialize decisions to ORDINAL (default)
        decisions = torch.full((N,), RoutingDecision.ORDINAL, device=device, dtype=torch.long)
        
        # Rule 1: Low confidence → DEFER
        defer_mask = coral_confidence < self.config.defer_threshold
        decisions[defer_mask] = RoutingDecision.DEFER
        
        # Rule 2: Near boundary (high fractional E[y]) → stay ORDINAL
        # Already default, but explicit for clarity
        boundary_mask = fractional_part < self.config.ordinal_margin
        # These stay ordinal - no change needed
        
        # Rule 3: High confidence + stable regime → FOCAL
        if self.config.enable_focal:
            focal_eligible = focal_confidence > self.config.focal_confidence
            
            # Check regime stability if distance provided
            if regime_distance is not None:
                regime_stable = regime_distance < self.config.regime_distance_threshold
                focal_eligible = focal_eligible & regime_stable
            
            # Don't use focal for deferred or boundary cases
            focal_eligible = focal_eligible & ~defer_mask & ~boundary_mask
            
            decisions[focal_eligible] = RoutingDecision.FOCAL
        
        # Build decision masks
        defer_mask = (decisions == RoutingDecision.DEFER)
        ordinal_mask = (decisions == RoutingDecision.ORDINAL)
        focal_mask = (decisions == RoutingDecision.FOCAL)
        
        # Compute final predictions based on routing
        predictions = torch.zeros(N, device=device, dtype=torch.long)
        probabilities = torch.zeros(N, num_classes, device=device)
        confidence = torch.zeros(N, device=device)
        
        # Ordinal predictions (CORAL)
        if ordinal_mask.any():
            predictions[ordinal_mask] = coral_probs[ordinal_mask].argmax(dim=-1)
            probabilities[ordinal_mask] = coral_probs[ordinal_mask]
            confidence[ordinal_mask] = coral_confidence[ordinal_mask]
        
        # Focal predictions
        if focal_mask.any():
            predictions[focal_mask] = focal_probs[focal_mask].argmax(dim=-1)
            probabilities[focal_mask] = focal_probs[focal_mask]
            confidence[focal_mask] = focal_confidence[focal_mask]
        
        # Deferred: still provide CORAL prediction but flag
        if defer_mask.any():
            predictions[defer_mask] = coral_probs[defer_mask].argmax(dim=-1)
            probabilities[defer_mask] = coral_probs[defer_mask]
            confidence[defer_mask] = coral_confidence[defer_mask]
        
        return RoutingOutput(
            predictions=predictions,
            probabilities=probabilities,
            confidence=confidence,
            expected_value=coral_expected,
            decisions=decisions,
            defer_mask=defer_mask,
            ordinal_mask=ordinal_mask,
            focal_mask=focal_mask,
        )
    
    def get_routing_stats(self, output: RoutingOutput) -> Dict[str, float]:
        """
        Compute statistics about routing decisions.
        
        Args:
            output: RoutingOutput from route()
            
        Returns:
            Dictionary with routing statistics
        """
        N = output.predictions.shape[0]
        
        return {
            'total_predictions': N,
            'num_deferred': int(output.defer_mask.sum().item()),
            'num_ordinal': int(output.ordinal_mask.sum().item()),
            'num_focal': int(output.focal_mask.sum().item()),
            'pct_deferred': 100 * output.defer_mask.float().mean().item(),
            'pct_ordinal': 100 * output.ordinal_mask.float().mean().item(),
            'pct_focal': 100 * output.focal_mask.float().mean().item(),
            'mean_confidence': output.confidence.mean().item(),
            'mean_confidence_deferred': (
                output.confidence[output.defer_mask].mean().item() 
                if output.defer_mask.any() else 0.0
            ),
            'mean_confidence_ordinal': (
                output.confidence[output.ordinal_mask].mean().item()
                if output.ordinal_mask.any() else 0.0
            ),
            'mean_confidence_focal': (
                output.confidence[output.focal_mask].mean().item()
                if output.focal_mask.any() else 0.0
            ),
        }
    
    def update_thresholds(
        self,
        defer_threshold: Optional[float] = None,
        ordinal_margin: Optional[float] = None,
        focal_confidence: Optional[float] = None,
        regime_distance_threshold: Optional[float] = None,
    ):
        """
        Update routing thresholds dynamically.
        
        Args:
            defer_threshold: New defer threshold
            ordinal_margin: New ordinal margin
            focal_confidence: New focal confidence threshold
            regime_distance_threshold: New regime distance threshold
        """
        if defer_threshold is not None:
            self.config.defer_threshold = defer_threshold
        if ordinal_margin is not None:
            self.config.ordinal_margin = ordinal_margin
        if focal_confidence is not None:
            self.config.focal_confidence = focal_confidence
        if regime_distance_threshold is not None:
            self.config.regime_distance_threshold = regime_distance_threshold


def calibrate_thresholds(
    coral_probs: torch.Tensor,
    coral_expected: torch.Tensor,
    labels: torch.Tensor,
    target_defer_rate: float = 0.05,
    target_accuracy: float = 0.95,
) -> RoutingConfig:
    """
    Calibrate routing thresholds on validation data.
    
    Finds thresholds that achieve:
    - Target defer rate (percentage flagged for review)
    - Target accuracy on non-deferred predictions
    
    Args:
        coral_probs: CORAL class probabilities [N, num_classes]
        coral_expected: CORAL expected values [N]
        labels: Ground truth labels [N]
        target_defer_rate: Desired percentage of deferred predictions
        target_accuracy: Target accuracy on non-deferred
        
    Returns:
        RoutingConfig with calibrated thresholds
    """
    confidence = coral_probs.max(dim=-1).values
    predictions = coral_probs.argmax(dim=-1)
    correct = (predictions == labels)
    
    # Sort by confidence
    sorted_conf, sorted_idx = confidence.sort()
    sorted_correct = correct[sorted_idx]
    
    # Find defer threshold for target defer rate
    defer_idx = int(len(sorted_conf) * target_defer_rate)
    defer_threshold = sorted_conf[defer_idx].item() if defer_idx > 0 else 0.0
    
    # Find focal threshold where accuracy exceeds target
    # Work backwards from highest confidence
    cumsum_correct = sorted_correct.flip(0).cumsum(0).flip(0)
    cumsum_total = torch.arange(len(sorted_correct), 0, -1, device=sorted_correct.device).float()
    cumulative_accuracy = cumsum_correct / cumsum_total
    
    focal_idx = (cumulative_accuracy >= target_accuracy).nonzero()
    if len(focal_idx) > 0:
        focal_threshold = sorted_conf[focal_idx[0]].item()
    else:
        focal_threshold = 0.95  # Very conservative default
    
    # Compute ordinal margin from E[y] fractional distribution
    fractional = torch.abs(coral_expected - coral_expected.round())
    ordinal_margin = fractional.quantile(0.25).item()  # 25th percentile
    
    return RoutingConfig(
        defer_threshold=defer_threshold,
        ordinal_margin=ordinal_margin,
        focal_confidence=focal_threshold,
    )
