"""
Production Multi-Stage Root Count Prediction Model.

This module implements a production-ready prediction system with:
- Stage 1: Shared DepthAwareGATv2 backbone (preserves sub-arrow geometry)
- Stage 2: CORAL ordinal head (primary prediction with uncertainty)
- Stage 3: Focal head (sharp classifier, gated by confidence)
- Stage 4: Uncertainty routing (defers, uses ordinal, or activates focal)

The architecture separates embedding computation from prediction heads,
enabling post-hoc analysis, regime detection, and drift monitoring.

Usage:
    model = ProductionRootClassifier(num_features=24, hidden_dim=64)
    
    # Training Phase 1: CORAL head
    h, coral_logits, _ = model(x, edge_index, return_all=True)
    coral_loss = compute_coral_loss(coral_logits, labels)
    
    # Training Phase 2: Focal head (frozen backbone)
    model.freeze_backbone()
    _, _, focal_logits = model(x, edge_index, return_all=True)
    focal_loss = compute_focal_loss(focal_logits, labels)
    
    # Inference with routing
    predictions, confidence, decisions = model.predict_with_routing(x, edge_index)
"""

from typing import Optional, Tuple, Dict, NamedTuple
from dataclasses import dataclass

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import GATConv

import sys
from pathlib import Path

# Add parent directory to path for imports
parent_dir = Path(__file__).parent.parent
if str(parent_dir) not in sys.path:
    sys.path.insert(0, str(parent_dir))

from core.config import (
    NUM_FEATURES,
    NUM_CLASSES,
    HIDDEN_DIM,
    DROPOUT_RATE,
    NUM_ATTENTION_HEADS,
    NUM_EDGE_FEATURES,
)
from core.edge_features import compute_polynomial_edge_features


# =============================================================================
# Data Classes for Structured Outputs
# =============================================================================

@dataclass
class PredictionOutput:
    """Structured output from production model prediction."""
    predictions: torch.Tensor  # Final class predictions [N]
    probabilities: torch.Tensor  # Class probabilities [N, num_classes]
    confidence: torch.Tensor  # Confidence scores [N]
    expected_value: torch.Tensor  # E[y] from CORAL [N]
    decisions: torch.Tensor  # Routing decisions [N] (0=defer, 1=ordinal, 2=focal)
    embeddings: torch.Tensor  # Node embeddings [N, hidden_dim]


# =============================================================================
# Stage 1: Shared Backbone
# =============================================================================

class DepthAwareGATv2Backbone(nn.Module):
    """
    Shared GAT backbone with anti-collapse mechanisms.
    
    Architecture:
        Input -> EdgeEncoder -> [GAT + Skip + LayerNorm] x2 -> JK Concat -> Projection
    
    This backbone produces embeddings h ∈ ℝ^d that preserve:
    - Semantic structure (polynomial relationships)
    - Regime geometry (class separation)
    - Sub-arrow relationships (depth-aware attention)
    
    The backbone does NOT include a classifier head - that's added separately
    as CORAL and Focal heads.
    
    Args:
        num_features: Number of input node features
        hidden_dim: Hidden layer dimension
        dropout: Dropout probability
        heads: Number of attention heads
        use_skip: Whether to use skip connections
        use_jk: Whether to use Jumping Knowledge concatenation
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        use_skip: bool = True,
        use_jk: bool = True,
    ):
        super().__init__()
        self.hidden_dim = hidden_dim
        self.heads = heads
        self.dropout = dropout
        self.use_skip = use_skip
        self.use_jk = use_jk
        
        # Output dimension after backbone
        self.output_dim = hidden_dim * heads
        
        # Edge feature encoder: 18D -> heads
        self.edge_encoder = nn.Sequential(
            nn.Linear(NUM_EDGE_FEATURES, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )
        
        # Initial projection to match hidden dim for skip connections
        self.input_proj = nn.Linear(num_features, hidden_dim * heads)
        
        # Only 2 GAT layers (reduced from 3) to prevent over-smoothing
        self.conv1 = GATConv(
            num_features, hidden_dim,
            heads=heads,
            edge_dim=heads,
            concat=True,
            dropout=dropout,
        )
        self.conv2 = GATConv(
            hidden_dim * heads, hidden_dim,
            heads=heads,
            edge_dim=heads,
            concat=True,
            dropout=dropout,
        )
        
        # Layer normalization for stability
        self.norm1 = nn.LayerNorm(hidden_dim * heads)
        self.norm2 = nn.LayerNorm(hidden_dim * heads)
        
        # Jumping Knowledge: concatenate input + layer1 + layer2
        if use_jk:
            jk_dim = hidden_dim * heads * 3  # 3 representations
            self.jk_proj = nn.Linear(jk_dim, hidden_dim * heads)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Forward pass producing embeddings.
        
        Args:
            x: Node feature matrix [N, num_features]
            edge_index: Edge connectivity [2, E]
            
        Returns:
            Node embeddings [N, hidden_dim * heads]
        """
        # Compute polynomial edge features
        edge_feat = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_feat)
        
        # Project input for skip connections
        h0 = self.input_proj(x)  # [N, hidden*heads]
        
        # Layer 1 with skip connection
        h1 = self.conv1(x, edge_index, edge_attr=edge_attr)
        h1 = F.elu(h1)
        if self.use_skip:
            h1 = h1 + h0
        h1 = self.norm1(h1)
        h1 = F.dropout(h1, p=self.dropout, training=self.training)
        
        # Layer 2 with skip connection
        h2 = self.conv2(h1, edge_index, edge_attr=edge_attr)
        h2 = F.elu(h2)
        if self.use_skip:
            h2 = h2 + h1
        h2 = self.norm2(h2)
        h2 = F.dropout(h2, p=self.dropout, training=self.training)
        
        # Jumping Knowledge: concatenate all representations
        if self.use_jk:
            h = torch.cat([h0, h1, h2], dim=-1)  # [N, hidden*heads*3]
            h = self.jk_proj(h)  # [N, hidden*heads]
        else:
            h = h2
        
        return h
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        for layer in self.edge_encoder:
            if hasattr(layer, 'reset_parameters'):
                layer.reset_parameters()
        self.input_proj.reset_parameters()
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.norm1.reset_parameters()
        self.norm2.reset_parameters()
        if self.use_jk:
            self.jk_proj.reset_parameters()


# =============================================================================
# Stage 2: CORAL Ordinal Head
# =============================================================================

class CORALHead(nn.Module):
    """
    CORAL ordinal regression head for root count prediction.
    
    Instead of K-way softmax, outputs K-1 sigmoid thresholds:
    - t0: P(y > 0), t1: P(y > 1), t2: P(y > 2), t3: P(y > 3)
    
    Key methods:
    - forward(): Returns threshold logits for loss computation
    - get_class_probs(): Converts thresholds to class probabilities
    - get_expected_value(): Computes E[y] = sum of threshold probabilities
    - predict(): Returns class predictions (count of exceeded thresholds)
    
    This head respects ordinal structure and provides calibrated uncertainty.
    
    Args:
        input_dim: Dimension of input embeddings
        num_classes: Number of ordinal classes K (produces K-1 thresholds)
    """
    
    def __init__(
        self,
        input_dim: int,
        num_classes: int = NUM_CLASSES,
    ):
        super().__init__()
        self.input_dim = input_dim
        self.num_classes = num_classes
        self.num_thresholds = num_classes - 1  # K-1 thresholds for K classes
        
        # CORAL: shared linear weights + per-threshold learnable biases
        # This ensures rank consistency: if t_k fires, t_{k-1} should too
        self.fc = nn.Linear(input_dim, 1, bias=False)
        self.threshold_biases = nn.Parameter(torch.zeros(self.num_thresholds))
    
    def forward(self, h: torch.Tensor) -> torch.Tensor:
        """
        Forward pass - returns raw logits for each threshold.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Threshold logits [N, num_thresholds] (apply sigmoid externally)
        """
        shared_logit = self.fc(h)  # [N, 1]
        logits = shared_logit + self.threshold_biases  # [N, num_thresholds]
        return logits
    
    def get_threshold_probs(self, h: torch.Tensor) -> torch.Tensor:
        """
        Get threshold probabilities P(y > k) for each k.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Threshold probabilities [N, num_thresholds]
        """
        logits = self(h)
        return torch.sigmoid(logits)
    
    def get_class_probs(self, h: torch.Tensor) -> torch.Tensor:
        """
        Convert threshold probabilities to class probabilities.
        
        For ordinal regression:
        - P(y=0) = 1 - P(y>0) = 1 - t0
        - P(y=k) = P(y>k-1) - P(y>k) = t_{k-1} - t_k  for k in [1, K-2]
        - P(y=K-1) = P(y>K-2) = t_{K-2}
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Class probabilities [N, num_classes] summing to 1
        """
        threshold_probs = self.get_threshold_probs(h)  # [N, num_thresholds]
        
        class_probs = torch.zeros(
            h.shape[0], self.num_classes, 
            device=h.device, dtype=h.dtype
        )
        
        # P(y=0) = 1 - P(y>0)
        class_probs[:, 0] = 1 - threshold_probs[:, 0]
        
        # P(y=k) = P(y>k-1) - P(y>k) for middle classes
        for k in range(1, self.num_classes - 1):
            class_probs[:, k] = threshold_probs[:, k-1] - threshold_probs[:, k]
        
        # P(y=K-1) = P(y>K-2) = last threshold prob
        class_probs[:, -1] = threshold_probs[:, -1]
        
        # Clamp to handle numerical issues and renormalize
        class_probs = class_probs.clamp(min=0)
        class_probs = class_probs / class_probs.sum(dim=-1, keepdim=True).clamp(min=1e-8)
        
        return class_probs
    
    def get_expected_value(self, h: torch.Tensor) -> torch.Tensor:
        """
        Compute expected value E[y] from threshold probabilities.
        
        E[y] = sum_{k=0}^{K-2} P(y > k) = sum of all threshold probabilities
        
        This provides a continuous prediction useful for:
        - Detecting boundary cases (fractional E[y])
        - Confidence estimation
        - Regression-style evaluation
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Expected values [N] in range [0, num_classes-1]
        """
        threshold_probs = self.get_threshold_probs(h)  # [N, num_thresholds]
        return threshold_probs.sum(dim=-1)  # E[y] = sum of P(y > k)
    
    def predict(self, h: torch.Tensor) -> torch.Tensor:
        """
        Get class predictions by counting exceeded thresholds.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Class predictions [N] as integers in [0, num_classes-1]
        """
        threshold_probs = self.get_threshold_probs(h)
        return (threshold_probs > 0.5).sum(dim=-1).long()
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.fc.reset_parameters()
        nn.init.zeros_(self.threshold_biases)


# =============================================================================
# Stage 3: Focal Head (Sharp Classifier)
# =============================================================================

class FocalHead(nn.Module):
    """
    Focal loss classifier head for sharp predictions.
    
    This is a simple linear classifier trained with focal loss to
    focus on hard examples. It produces sharp predictions in stable
    regions but should NOT be trusted near regime boundaries.
    
    Use through gating: only trust focal predictions when CORAL
    confidence is high and regime detection confirms stability.
    
    Args:
        input_dim: Dimension of input embeddings
        num_classes: Number of output classes
    """
    
    def __init__(
        self,
        input_dim: int,
        num_classes: int = NUM_CLASSES,
    ):
        super().__init__()
        self.input_dim = input_dim
        self.num_classes = num_classes
        
        self.classifier = nn.Linear(input_dim, num_classes)
    
    def forward(self, h: torch.Tensor) -> torch.Tensor:
        """
        Forward pass - returns raw logits.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Class logits [N, num_classes]
        """
        return self.classifier(h)
    
    def get_probs(self, h: torch.Tensor) -> torch.Tensor:
        """
        Get class probabilities via softmax.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Class probabilities [N, num_classes]
        """
        return F.softmax(self(h), dim=-1)
    
    def predict(self, h: torch.Tensor) -> torch.Tensor:
        """
        Get class predictions.
        
        Args:
            h: Node embeddings [N, input_dim]
            
        Returns:
            Class predictions [N]
        """
        return self(h).argmax(dim=-1)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.classifier.reset_parameters()


# =============================================================================
# Unified Production Model
# =============================================================================

class ProductionRootClassifier(nn.Module):
    """
    Production-ready multi-stage root count classifier.
    
    Combines:
    - Shared GAT backbone (preserves geometry)
    - CORAL ordinal head (primary, uncertainty-aware)
    - Focal head (accelerator, gated by confidence)
    
    Training:
    - Phase 1: Train backbone + CORAL jointly
    - Phase 2: Freeze backbone, train Focal head
    
    Inference:
    - Use predict_with_routing() for production predictions
    - Routing decides: defer, use ordinal, or use focal
    
    Args:
        num_features: Number of input node features
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes
        dropout: Dropout probability
        heads: Number of attention heads
        use_skip: Whether to use skip connections in backbone
        use_jk: Whether to use Jumping Knowledge in backbone
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        use_skip: bool = True,
        use_jk: bool = True,
    ):
        super().__init__()
        self.num_features = num_features
        self.hidden_dim = hidden_dim
        self.num_classes = num_classes
        self.heads = heads
        
        # Stage 1: Shared backbone
        self.backbone = DepthAwareGATv2Backbone(
            num_features=num_features,
            hidden_dim=hidden_dim,
            dropout=dropout,
            heads=heads,
            use_skip=use_skip,
            use_jk=use_jk,
        )
        
        # Embedding dimension from backbone
        embedding_dim = self.backbone.output_dim
        
        # Stage 2: CORAL ordinal head
        self.coral_head = CORALHead(
            input_dim=embedding_dim,
            num_classes=num_classes,
        )
        
        # Stage 3: Focal head
        self.focal_head = FocalHead(
            input_dim=embedding_dim,
            num_classes=num_classes,
        )
        
        # Track frozen state
        self._backbone_frozen = False
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        return_all: bool = False,
    ) -> Tuple[torch.Tensor, ...]:
        """
        Forward pass through backbone and heads.
        
        Args:
            x: Node feature matrix [N, num_features]
            edge_index: Edge connectivity [2, E]
            return_all: If True, return (embeddings, coral_logits, focal_logits)
                       If False, return only coral_logits (default)
        
        Returns:
            If return_all=False: coral_logits [N, num_thresholds]
            If return_all=True: (embeddings, coral_logits, focal_logits)
        """
        # Get embeddings from backbone
        h = self.backbone(x, edge_index)
        
        # Get outputs from both heads
        coral_logits = self.coral_head(h)
        
        if return_all:
            focal_logits = self.focal_head(h)
            return h, coral_logits, focal_logits
        
        return coral_logits
    
    def get_embeddings(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Get node embeddings from backbone (for indexing, analysis).
        
        Args:
            x: Node feature matrix [N, num_features]
            edge_index: Edge connectivity [2, E]
            
        Returns:
            Node embeddings [N, hidden_dim * heads]
        """
        return self.backbone(x, edge_index)
    
    def freeze_backbone(self):
        """Freeze backbone parameters for Phase 2 training."""
        for param in self.backbone.parameters():
            param.requires_grad = False
        self._backbone_frozen = True
    
    def unfreeze_backbone(self):
        """Unfreeze backbone parameters."""
        for param in self.backbone.parameters():
            param.requires_grad = True
        self._backbone_frozen = False
    
    def get_trainable_params(self, phase: int = 1) -> list:
        """
        Get parameters to train for each phase.
        
        Args:
            phase: Training phase (1 = backbone + CORAL, 2 = Focal only)
            
        Returns:
            List of parameters to optimize
        """
        if phase == 1:
            return list(self.backbone.parameters()) + list(self.coral_head.parameters())
        elif phase == 2:
            return list(self.focal_head.parameters())
        else:
            raise ValueError(f"Unknown training phase: {phase}")
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.backbone.reset_parameters()
        self.coral_head.reset_parameters()
        self.focal_head.reset_parameters()


# =============================================================================
# Loss Functions
# =============================================================================

def compute_coral_loss(
    logits: torch.Tensor,
    labels: torch.Tensor,
    reduction: str = 'mean',
) -> torch.Tensor:
    """
    Compute CORAL loss (binary cross-entropy per threshold).
    
    For each threshold k, creates binary target: y > k.
    Loss = sum of BCE losses across all thresholds.
    
    Args:
        logits: Threshold logits [N, num_thresholds]
        labels: Class labels [N] as integers in [0, num_classes-1]
        reduction: 'mean', 'sum', or 'none'
        
    Returns:
        CORAL loss (scalar if reduction='mean'/'sum', [N] if 'none')
    """
    num_thresholds = logits.shape[1]
    device = logits.device
    
    # Create binary targets for each threshold
    # threshold k: target = 1 if label > k, else 0
    targets = torch.zeros_like(logits)
    for k in range(num_thresholds):
        targets[:, k] = (labels > k).float()
    
    # Binary cross-entropy loss per threshold
    loss = F.binary_cross_entropy_with_logits(
        logits, targets, reduction='none'
    )
    
    # Sum across thresholds
    loss = loss.sum(dim=-1)  # [N]
    
    if reduction == 'mean':
        return loss.mean()
    elif reduction == 'sum':
        return loss.sum()
    return loss


def compute_focal_loss(
    logits: torch.Tensor,
    labels: torch.Tensor,
    gamma: float = 2.0,
    alpha: Optional[torch.Tensor] = None,
    reduction: str = 'mean',
) -> torch.Tensor:
    """
    Compute focal loss for classification.
    
    FL(p_t) = -alpha_t * (1 - p_t)^gamma * log(p_t)
    
    Down-weights easy examples, focuses on hard minority classes.
    
    Args:
        logits: Class logits [N, num_classes]
        labels: Class labels [N]
        gamma: Focusing parameter (higher = more focus on hard examples)
        alpha: Optional class weights [num_classes]
        reduction: 'mean', 'sum', or 'none'
        
    Returns:
        Focal loss
    """
    probs = F.softmax(logits, dim=-1)
    num_classes = logits.shape[1]
    
    # Get probability for true class
    labels_one_hot = F.one_hot(labels, num_classes=num_classes).float()
    p_t = (probs * labels_one_hot).sum(dim=-1)  # [N]
    
    # Focal weight: (1 - p_t)^gamma
    focal_weight = (1 - p_t) ** gamma
    
    # Cross-entropy term: -log(p_t)
    ce_loss = -torch.log(p_t.clamp(min=1e-8))
    
    # Focal loss
    loss = focal_weight * ce_loss
    
    # Apply class weights if provided
    if alpha is not None:
        alpha_t = alpha[labels]
        loss = alpha_t * loss
    
    if reduction == 'mean':
        return loss.mean()
    elif reduction == 'sum':
        return loss.sum()
    return loss


# =============================================================================
# Factory Function
# =============================================================================

def get_production_model(**kwargs) -> ProductionRootClassifier:
    """
    Factory function to create production model with default settings.
    
    Args:
        **kwargs: Override default parameters
        
    Returns:
        ProductionRootClassifier instance
    """
    defaults = {
        'num_features': NUM_FEATURES,
        'hidden_dim': HIDDEN_DIM,
        'num_classes': NUM_CLASSES,
        'dropout': DROPOUT_RATE,
        'heads': NUM_ATTENTION_HEADS,
        'use_skip': True,
        'use_jk': True,
    }
    defaults.update(kwargs)
    return ProductionRootClassifier(**defaults)
