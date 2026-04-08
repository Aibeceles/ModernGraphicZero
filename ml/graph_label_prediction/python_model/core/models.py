"""
Model architectures for Root Count Prediction.

This module provides classifier architectures for predicting the number
of rational roots (totalZero) for polynomial nodes:
- MLPClassifier: Multi-layer perceptron baseline (no graph structure)
- GCNClassifier: Graph Convolutional Network
- GraphSAGEClassifier: GraphSAGE with max aggregation
- DepthAwareGAT: Graph Attention Network with depth-aware edge features (IMPROVED)
- DepthAwareGATv2: GAT with skip connections + JKNet to prevent over-smoothing
- FocalLossClassifier: GAT with focal loss for class imbalance

All models follow the same interface for easy swapping during experiments.
Input: NUM_FEATURES_TOTAL dimensions (wNum + degree + determined + coeffs + stats + set_union)
Output: NUM_CLASSES root count classes [0..MAX_GRAPH_DEPTH]
"""

from typing import Optional

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import GCNConv, SAGEConv, GATConv

from .config import (
    NUM_FEATURES,
    NUM_CLASSES,
    HIDDEN_DIM,
    DROPOUT_RATE,
    NUM_ATTENTION_HEADS,
    NUM_EDGE_FEATURES,
    DEFAULT_ACTIVATION,
)
from .edge_features import compute_polynomial_edge_features


# ---------------------------------------------------------------------------
# Activation helper
# ---------------------------------------------------------------------------

_ACTIVATIONS = {
    'relu': F.relu,
    'elu': F.elu,
    'leaky_relu': F.leaky_relu,
    'gelu': F.gelu,
}


def get_activation(name: str):
    """Map an activation name string to the corresponding torch function.

    Args:
        name: One of 'relu', 'elu', 'leaky_relu', 'gelu'.

    Returns:
        A callable ``(Tensor) -> Tensor``.

    Raises:
        ValueError: If *name* is not recognised.
    """
    try:
        return _ACTIVATIONS[name]
    except KeyError:
        raise ValueError(
            f"Unknown activation: {name!r}. "
            f"Supported: {list(_ACTIVATIONS.keys())}"
        )


class MLPClassifier(nn.Module):
    """
    Multi-layer perceptron classifier (baseline) for root count prediction.
    
    This model ignores graph structure and predicts root count based solely
    on node features (wNum, degree, coefficients, statistics). Useful as a 
    baseline to assess if graph structure provides additional predictive power.
    
    Architecture:
        Input (NUM_FEATURES) -> Linear -> Act -> Dropout -> 
        Linear -> Act -> Dropout -> Linear (NUM_CLASSES) -> LogSoftmax
    
    Args:
        num_features: Number of input features per node (default: from config.NUM_FEATURES)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: from config.NUM_CLASSES)
        dropout: Dropout probability
        activation: Activation function name ('relu', 'elu', 'leaky_relu', 'gelu')
        
    Example:
        >>> model = MLPClassifier()
        >>> out = model(data.x, data.edge_index)
        >>> pred = out.argmax(dim=1)  # Predicted root count
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        activation: str = DEFAULT_ACTIVATION,
    ):
        super().__init__()
        self.dropout = dropout
        self.act = get_activation(activation)
        
        self.fc1 = nn.Linear(num_features, hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.fc3 = nn.Linear(hidden_dim, num_classes)
    
    def forward(
        self, 
        x: torch.Tensor, 
        edge_index: Optional[torch.Tensor] = None
    ) -> torch.Tensor:
        """
        Forward pass.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity (ignored for MLP)
            
        Returns:
            Log-softmax class probabilities [num_nodes, num_classes]
        """
        x = self.act(self.fc1(x))
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.act(self.fc2(x))
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.fc3(x)
        return F.log_softmax(x, dim=1)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.fc1.reset_parameters()
        self.fc2.reset_parameters()
        self.fc3.reset_parameters()


class GCNClassifier(nn.Module):
    """
    Graph Convolutional Network classifier for root count prediction.
    
    Uses two GCN layers to aggregate neighborhood information via zMap edges,
    followed by a linear classification head to predict root count.
    
    Architecture:
        Input (NUM_FEATURES) -> GCNConv -> ReLU -> Dropout ->
        GCNConv -> ReLU -> Dropout -> Linear (NUM_CLASSES) -> LogSoftmax
    
    Args:
        num_features: Number of input features per node (default: from config.NUM_FEATURES)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: from config.NUM_CLASSES)
        dropout: Dropout probability
        
    Example:
        >>> model = GCNClassifier()
        >>> out = model(data.x, data.edge_index)
        >>> pred = out.argmax(dim=1)  # Predicted root count
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        activation: str = DEFAULT_ACTIVATION,
    ):
        super().__init__()
        self.dropout = dropout
        self.act = get_activation(activation)
        
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        self.classifier = nn.Linear(hidden_dim, num_classes)
    
    def forward(
        self, 
        x: torch.Tensor, 
        edge_index: torch.Tensor
    ) -> torch.Tensor:
        """
        Forward pass.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Log-softmax class probabilities [num_nodes, num_classes]
        """
        # First GCN layer
        x = self.conv1(x, edge_index)
        x = self.act(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Second GCN layer
        x = self.conv2(x, edge_index)
        x = self.act(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Classification head
        x = self.classifier(x)
        return F.log_softmax(x, dim=1)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.classifier.reset_parameters()


class GraphSAGEClassifier(nn.Module):
    """
    GraphSAGE classifier with max aggregation for root count prediction.
    
    Uses max aggregation for neighborhood aggregation via zMap edges
    to predict the number of rational roots.
    
    Architecture:
        Input (NUM_FEATURES) -> SAGEConv(max) -> ReLU -> Dropout ->
        SAGEConv(max) -> ReLU -> Dropout -> Linear (NUM_CLASSES) -> LogSoftmax
    
    Args:
        num_features: Number of input features per node (default: from config.NUM_FEATURES)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: from config.NUM_CLASSES)
        dropout: Dropout probability
        aggr: Aggregation method ('max', 'mean', 'add')
        
    Example:
        >>> model = GraphSAGEClassifier()
        >>> out = model(data.x, data.edge_index)
        >>> pred = out.argmax(dim=1)  # Predicted root count
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        aggr: str = 'max',
        activation: str = DEFAULT_ACTIVATION,
    ):
        super().__init__()
        self.dropout = dropout
        self.act = get_activation(activation)
        
        # SAGEConv with max aggregation (matches Neo4j 'pool' aggregator)
        self.conv1 = SAGEConv(num_features, hidden_dim, aggr=aggr)
        self.conv2 = SAGEConv(hidden_dim, hidden_dim, aggr=aggr)
        self.classifier = nn.Linear(hidden_dim, num_classes)
    
    def forward(
        self, 
        x: torch.Tensor, 
        edge_index: torch.Tensor
    ) -> torch.Tensor:
        """
        Forward pass.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Log-softmax class probabilities [num_nodes, num_classes]
        """
        # First SAGE layer
        x = self.conv1(x, edge_index)
        x = self.act(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Second SAGE layer
        x = self.conv2(x, edge_index)
        x = self.act(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Classification head
        x = self.classifier(x)
        return F.log_softmax(x, dim=1)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.classifier.reset_parameters()


class DepthAwareGAT(nn.Module):
    """
    Depth-Aware Graph Attention Network for root count prediction.
    
    This model uses mathematically-motivated edge features that capture the
    differentiation relationship between connected polynomials in the tree.
    
    Edge Features (18 dimensions):
    1. Depth features (4D): depth_diff, abs_diff, direction, is_adjacent
    2. Degree features (3D): degree_diff, degree_ratio, degree_consistency
    3. Leading coeff features (3D): ratio, diff, scaling_error
    4. Similarity features (4D): cosine_sim, l2_dist, correlation, sparsity_diff
    5. Magnitude features (2D): magnitude_ratio, magnitude_diff_log
    6. Constant term features (2D): constant_diff, constant_ratio
    
    These features capture polynomial relationships beyond just tree depth,
    enabling the attention mechanism to learn differentiation-aware patterns.
    
    Architecture:
        Input (12 + spectral_pe_dim) -> 
        GATConv(edge_dim=18) -> ELU -> Dropout ->
        GATConv(edge_dim=18) -> ELU -> Dropout ->
        GATConv(edge_dim=18) -> LogSoftmax
    
    Args:
        num_features: Number of input features (12 base + coeff + stats + spectral PE)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: 5)
        dropout: Dropout probability
        heads: Number of attention heads
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        activation: str = 'elu',
        num_edge_features: int = NUM_EDGE_FEATURES,
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.act = get_activation(activation)
        
        # Edge feature encoder: polynomial edge features -> edge embedding
        # Input: num_edge_features (default 18; may be fewer under ablation)
        # Output: heads (one per attention head)
        self.edge_encoder = nn.Sequential(
            nn.Linear(num_edge_features, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )
        
        # Three GATConv layers with edge features
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
        self.conv3 = GATConv(
            hidden_dim * heads, num_classes,
            heads=1,
            edge_dim=heads,
            concat=False,
            dropout=dropout,
        )
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        wNum: Optional[torch.Tensor] = None,
        edge_attr: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """
        Forward pass with polynomial-aware attention.
        
        Computes 18D edge features from node polynomial coefficients and
        uses them to modulate attention weights in the GAT layers.
        
        Args:
            x: Node feature matrix [num_nodes, num_features] with structure:
               [wNum, degree, determined, coeff_0..N, stats, set_union] where N = MAX_POLYNOMIAL_DEGREE
            edge_index: Edge connectivity [2, num_edges]
            wNum: Deprecated, kept for API compatibility (now extracted from x)
            edge_attr: Pre-computed (optionally masked) edge features [num_edges, D].
                If None, edge features are computed internally from x.
            
        Returns:
            Log-softmax class probabilities [num_nodes, num_classes]
        """
        if edge_attr is None:
            # Compute polynomial-based edge features (18 components)
            # Uses full node features including coefficients and statistics
            edge_attr = compute_polynomial_edge_features(x, edge_index)  # [num_edges, 18]
        
        # Encode into attention-compatible edge features
        edge_attr = self.edge_encoder(edge_attr)  # [num_edges, heads]
        
        # Three-layer GAT with polynomial-aware attention
        h = self.conv1(x, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv3(h, edge_index, edge_attr=edge_attr)
        
        return F.log_softmax(h, dim=1)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        for layer in self.edge_encoder:
            if hasattr(layer, 'reset_parameters'):
                layer.reset_parameters()
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.conv3.reset_parameters()


class DepthAwareGATv2(nn.Module):
    """
    Improved GAT with anti-collapse mechanisms:
    
    1. Skip connections: Preserve original features through each layer
    2. Jumping Knowledge (JKNet): Concatenate all layer representations
    3. Reduced layers: 2 layers instead of 3 to reduce over-smoothing
    4. Layer normalization: Stabilize training
    
    Architecture:
        Input -> [GAT + Skip + LayerNorm] x2 -> JK Concat -> Classifier
    
    Args:
        num_features: Number of input features (16 = wNum + degree + determined + 5 coeffs + 5 stats + 3 set_union)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: 5)
        dropout: Dropout probability
        heads: Number of attention heads
        use_skip: Whether to use skip connections
        use_jk: Whether to use Jumping Knowledge
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
        activation: str = 'elu',
        num_edge_features: int = NUM_EDGE_FEATURES,
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.use_skip = use_skip
        self.use_jk = use_jk
        self.act = get_activation(activation)
        
        # Edge feature encoder (accepts ablated dimension via num_edge_features)
        self.edge_encoder = nn.Sequential(
            nn.Linear(num_edge_features, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )
        
        # Initial projection to match hidden dim for skip connections
        self.input_proj = nn.Linear(num_features, hidden_dim * heads)
        
        # Only 2 GAT layers (not 3) to reduce over-smoothing
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
        
        # Layer norms for stability
        self.norm1 = nn.LayerNorm(hidden_dim * heads)
        self.norm2 = nn.LayerNorm(hidden_dim * heads)
        
        # JK: concatenate input + layer1 + layer2
        if use_jk:
            jk_dim = hidden_dim * heads * 3  # 3 representations
            self.jk_proj = nn.Linear(jk_dim, hidden_dim * heads)
        
        # Classifier head
        self.classifier = nn.Linear(hidden_dim * heads, num_classes)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        wNum: Optional[torch.Tensor] = None,
        edge_attr: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """Forward pass with skip connections and JKNet."""
        
        if edge_attr is None:
            edge_attr = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_attr)
        
        # Project input for skip connections
        h0 = self.input_proj(x)  # [N, hidden*heads]
        
        # Layer 1 with skip
        h1 = self.conv1(x, edge_index, edge_attr=edge_attr)
        h1 = self.act(h1)
        if self.use_skip:
            h1 = h1 + h0  # Skip connection
        h1 = self.norm1(h1)
        h1 = F.dropout(h1, p=self.dropout, training=self.training)
        
        # Layer 2 with skip
        h2 = self.conv2(h1, edge_index, edge_attr=edge_attr)
        h2 = self.act(h2)
        if self.use_skip:
            h2 = h2 + h1  # Skip connection
        h2 = self.norm2(h2)
        h2 = F.dropout(h2, p=self.dropout, training=self.training)
        
        # Jumping Knowledge: concatenate all representations
        if self.use_jk:
            h = torch.cat([h0, h1, h2], dim=-1)  # [N, hidden*heads*3]
            h = self.jk_proj(h)  # [N, hidden*heads]
        else:
            h = h2
        
        # Classify
        out = self.classifier(h)
        return F.log_softmax(out, dim=1)
    
    def reset_parameters(self):
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
        self.classifier.reset_parameters()


class FocalLossClassifier(nn.Module):
    """
    GAT with built-in focal loss computation for class imbalance.
    
    Focal loss: FL(p) = -α(1-p)^γ * log(p)
    
    This down-weights easy examples (majority class) and focuses on hard ones.
    γ=2 is typical; higher values focus more on hard examples.
    
    Architecture:
        Input -> Skip projection + [GAT x2] -> Skip connection -> Classifier
    
    Args:
        num_features: Number of input features (16)
        hidden_dim: Hidden layer dimension
        num_classes: Number of output classes (default: 5)
        dropout: Dropout probability
        heads: Number of attention heads
        focal_gamma: Focal loss gamma parameter (default: 2.0)
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        focal_gamma: float = 2.0,
        activation: str = 'elu',
        num_edge_features: int = NUM_EDGE_FEATURES,
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.focal_gamma = focal_gamma
        self.num_classes = num_classes
        self.act = get_activation(activation)
        
        # Edge encoder (accepts ablated dimension via num_edge_features)
        self.edge_encoder = nn.Sequential(
            nn.Linear(num_edge_features, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )
        
        # Two GAT layers (not 3)
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
        
        # Skip connection projection
        self.skip_proj = nn.Linear(num_features, hidden_dim * heads)
        
        self.classifier = nn.Linear(hidden_dim * heads, num_classes)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        wNum: Optional[torch.Tensor] = None,
        edge_attr: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """Forward pass returning logits (not log_softmax for focal loss)."""
        
        if edge_attr is None:
            edge_attr = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_attr)
        
        # Skip connection from input
        skip = self.skip_proj(x)
        
        h = self.conv1(x, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = h + skip  # Add skip connection
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        logits = self.classifier(h)
        return logits  # Return raw logits for focal loss
    
    def focal_loss(
        self,
        logits: torch.Tensor,
        targets: torch.Tensor,
        alpha: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """
        Compute focal loss.
        
        Args:
            logits: Raw logits [N, num_classes]
            targets: Class labels [N]
            alpha: Optional class weights [num_classes]
            
        Returns:
            Scalar loss value
        """
        probs = F.softmax(logits, dim=1)
        ce_loss = F.cross_entropy(logits, targets, reduction='none')
        
        # Get probability for true class
        p_t = probs.gather(1, targets.unsqueeze(1)).squeeze(1)
        
        # Focal weight: (1 - p_t)^gamma
        focal_weight = (1 - p_t) ** self.focal_gamma
        
        # Apply class weights if provided
        if alpha is not None:
            alpha_t = alpha.gather(0, targets)
            focal_weight = focal_weight * alpha_t
        
        loss = (focal_weight * ce_loss).mean()
        return loss
    
    def get_log_probs(self, x: torch.Tensor, edge_index: torch.Tensor) -> torch.Tensor:
        """Get log probabilities for prediction."""
        logits = self(x, edge_index)
        return F.log_softmax(logits, dim=1)
    
    def reset_parameters(self):
        for layer in self.edge_encoder:
            if hasattr(layer, 'reset_parameters'):
                layer.reset_parameters()
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.skip_proj.reset_parameters()
        self.classifier.reset_parameters()


class MultiTaskRootClassifier(nn.Module):
    """
    Multi-task model for truncated root observation learning.

    Predicts:
      1) `y_visible` = len(RootList): number of DISTINCT roots in the expanded window (0..NUM_CLASSES-1)
      2) `determined` (binary): a flag computed on a larger/full scan window (separate completeness indicator)

    Implementation: Polynomial-aware GAT backbone (shared) + two linear heads.
    Uses 18D edge features capturing differentiation relationships.
    """

    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        activation: str = 'elu',
        num_edge_features: int = NUM_EDGE_FEATURES,
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.num_classes = num_classes
        self.act = get_activation(activation)

        # Polynomial-aware edge encoder (accepts ablated dimension)
        self.edge_encoder = nn.Sequential(
            nn.Linear(num_edge_features, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )

        # Shared backbone (2 layers)
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

        # Task heads
        self.count_head = nn.Linear(hidden_dim * heads, num_classes)
        self.determined_head = nn.Linear(hidden_dim * heads, 2)

    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        wNum: Optional[torch.Tensor] = None,
        edge_attr: Optional[torch.Tensor] = None,
    ) -> tuple[torch.Tensor, torch.Tensor]:
        """
        Forward pass with polynomial-aware attention.
        
        Args:
            x: Node features [N, num_features] with polynomial coefficients
            edge_index: Edge connectivity [2, E]
            wNum: Deprecated, kept for API compatibility
            edge_attr: Pre-computed (optionally masked) edge features [E, D].
                If None, edge features are computed internally from x.
            
        Returns:
            Tuple of (count_logprobs, determined_logprobs)
        """
        if edge_attr is None:
            edge_attr = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_attr)  # [num_edges, heads]

        # Shared backbone
        h = self.conv1(x, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)

        h = self.conv2(h, edge_index, edge_attr=edge_attr)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)

        # Heads (return log-probs for NLLLoss compatibility)
        count_logits = self.count_head(h)
        determined_logits = self.determined_head(h)

        return F.log_softmax(count_logits, dim=1), F.log_softmax(determined_logits, dim=1)

    def reset_parameters(self):
        for layer in self.edge_encoder:
            if hasattr(layer, 'reset_parameters'):
                layer.reset_parameters()
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.count_head.reset_parameters()
        self.determined_head.reset_parameters()


class RegressionClassifier(nn.Module):
    """
    Regression-based classifier for root count prediction.
    
    Instead of predicting class probabilities, this model predicts a scalar
    root count value (0.0 to 4.0) and rounds to get class predictions.
    
    This serves as a diagnostic to test if treating root counts as ordinal/
    continuous improves predictions for middle classes (1, 2).
    
    Architecture:
        Input (12 + spectral_pe_dim) -> 
        GCNConv -> ReLU -> Dropout ->
        GCNConv -> ReLU -> Dropout ->
        Linear -> Sigmoid * 4  (outputs scalar in [0, 4])
    
    Training: Use MSE or SmoothL1 loss
    Inference: Round output and clamp to [0, 4]
    
    Args:
        num_features: Number of input features (12 base + coeff + stats + spectral PE)
        hidden_dim: Hidden layer dimension
        num_classes: Max root count + 1 (default: 5, so max=4)
        dropout: Dropout probability
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        activation: str = DEFAULT_ACTIVATION,
    ):
        super().__init__()
        self.dropout = dropout
        self.num_classes = num_classes
        self.max_value = num_classes - 1  # max ordinal value
        self.act = get_activation(activation)
        
        # Two GCN layers for feature extraction
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # Regression head: outputs single scalar
        self.regressor = nn.Linear(hidden_dim, 1)
    
    def forward(
        self, 
        x: torch.Tensor, 
        edge_index: torch.Tensor
    ) -> torch.Tensor:
        """
        Forward pass - returns scalar predictions.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Scalar root count predictions [num_nodes, 1] in range [0, max_value]
        """
        # GCN feature extraction
        h = self.conv1(x, edge_index)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        # Regression output: sigmoid * max_value to bound in [0, 4]
        out = torch.sigmoid(self.regressor(h)) * self.max_value
        
        return out
    
    def predict_classes(
        self, 
        x: torch.Tensor, 
        edge_index: torch.Tensor
    ) -> torch.Tensor:
        """
        Get class predictions by rounding regression output.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Class predictions [num_nodes] as integers in [0, num_classes-1]
        """
        with torch.no_grad():
            out = self(x, edge_index)
            # Round and clamp to valid class range
            classes = out.round().clamp(0, self.max_value).long().squeeze(-1)
        return classes
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.regressor.reset_parameters()


class CORALClassifier(nn.Module):
    """
    CORAL ordinal regression for root count prediction.
    
    Instead of K-way softmax, outputs K-1 sigmoid thresholds:
    - t_k: P(y > k) for k in [0, 1, ..., K-2]
    
    Prediction: sum(sigmoid(t_k) > 0.5) for k in [0..K-2]
    
    The number of thresholds adapts automatically to the number of
    observed classes in the dataset (set via the num_classes argument).
    
    This approach:
    1. Respects ordinal structure (2 is closer to 1 than to 4)
    2. Solves "never predicts max class" by giving each threshold its own loss
    3. Single model, no ensembling
    
    Architecture:
        Input -> GCNConv -> ReLU -> Dropout ->
        GCNConv -> ReLU -> Dropout ->
        Linear(shared) + threshold_biases -> (K-1) logits
    
    Training: Binary cross-entropy per threshold
    Inference: Count how many thresholds exceed 0.5
    
    Args:
        num_features: Number of input features (base + coeff + stats + spectral PE)
        hidden_dim: Hidden layer dimension
        num_classes: Number of ordinal classes K (produces K-1 thresholds)
        dropout: Dropout probability
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        activation: str = DEFAULT_ACTIVATION,
    ):
        super().__init__()
        self.dropout = dropout
        self.num_classes = num_classes
        self.num_thresholds = num_classes - 1  # K-1 thresholds for K classes
        self.act = get_activation(activation)
        
        # GCN backbone for feature extraction
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # CORAL head: shared linear weights + per-threshold learnable biases
        # This ensures rank consistency: if t_k fires, t_{k-1} should too
        self.fc = nn.Linear(hidden_dim, 1, bias=False)  # shared weights
        self.threshold_biases = nn.Parameter(torch.zeros(self.num_thresholds))
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Forward pass - returns raw logits for each threshold.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Threshold logits [num_nodes, num_thresholds] (apply sigmoid externally)
        """
        # GCN feature extraction
        h = self.conv1(x, edge_index)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index)
        h = self.act(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        # Shared linear projection: [N, hidden_dim] -> [N, 1]
        shared_logit = self.fc(h)  # [N, 1]
        
        # Add per-threshold biases: [N, 1] + [num_thresholds] -> [N, num_thresholds]
        logits = shared_logit + self.threshold_biases  # broadcasts to [N, num_thresholds]
        
        return logits
    
    def predict_classes(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Get class predictions by counting thresholds exceeded.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Class predictions [num_nodes] as integers in [0, num_classes-1]
        """
        with torch.no_grad():
            logits = self(x, edge_index)
            probs = torch.sigmoid(logits)  # [N, num_thresholds]
            # Count how many thresholds are exceeded (> 0.5)
            classes = (probs > 0.5).sum(dim=-1).long()  # [N]
        return classes
    
    def get_probabilities(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Convert threshold probabilities to class probabilities.
        
        For ordinal regression, P(y=k) = P(y>k-1) - P(y>k).
        This gives a proper probability distribution over classes.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Class probabilities [num_nodes, num_classes] summing to 1
        """
        with torch.no_grad():
            logits = self(x, edge_index)
            threshold_probs = torch.sigmoid(logits)  # [N, num_thresholds]
            
            # P(y=0) = 1 - P(y>0) = 1 - t0
            # P(y=k) = P(y>k-1) - P(y>k) = t_{k-1} - t_k  for k in [1, K-2]
            # P(y=K-1) = P(y>K-2) = t_{K-2}
            
            class_probs = torch.zeros(x.shape[0], self.num_classes, device=x.device)
            
            # P(y=0) = 1 - P(y>0)
            class_probs[:, 0] = 1 - threshold_probs[:, 0]
            
            # P(y=k) = P(y>k-1) - P(y>k) for middle classes
            for k in range(1, self.num_classes - 1):
                class_probs[:, k] = threshold_probs[:, k-1] - threshold_probs[:, k]
            
            # P(y=K-1) = P(y>K-2) = last threshold prob
            class_probs[:, -1] = threshold_probs[:, -1]
            
            # Clamp to handle numerical issues
            class_probs = class_probs.clamp(min=0)
            # Renormalize to sum to 1
            class_probs = class_probs / class_probs.sum(dim=-1, keepdim=True).clamp(min=1e-8)
        
        return class_probs
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.fc.reset_parameters()
        nn.init.zeros_(self.threshold_biases)


def get_model(model_name: str, **kwargs) -> nn.Module:
    """
    Factory function to create models by name.
    
    Args:
        model_name: One of 'mlp', 'gcn', 'graphsage', 'depth_aware_gat',
                    'depth_aware_gat_v2', 'focal', 'multitask', 'regression', 'coral'
        **kwargs: Additional arguments passed to model constructor
        
    Returns:
        Initialized model instance
        
    Raises:
        ValueError: If model_name is not recognized
        
    Example:
        >>> model = get_model('depth_aware_gat_v2', hidden_dim=128)
        >>> model = get_model('focal')  # Focal loss classifier
        >>> model = get_model('coral')  # CORAL ordinal regression
    """
    models = {
        'mlp': MLPClassifier,
        'gcn': GCNClassifier,
        'graphsage': GraphSAGEClassifier,
        'depth_aware_gat': DepthAwareGAT,
        'depth_aware_gat_v2': DepthAwareGATv2,
        'focal': FocalLossClassifier,
        'multitask': MultiTaskRootClassifier,
        'regression': RegressionClassifier,
        'coral': CORALClassifier,
    }
    
    if model_name.lower() not in models:
        raise ValueError(
            f"Unknown model: {model_name}. "
            f"Available models: {list(models.keys())}"
        )
    
    return models[model_name.lower()](**kwargs)

