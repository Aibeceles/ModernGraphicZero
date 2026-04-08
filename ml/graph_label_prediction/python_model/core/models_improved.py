"""
Improved Model Architectures for Root Count Prediction with Anti-Collapse Mechanisms.

Key improvements over original models.py:
1. DepthAwareGATv2: Skip connections + JKNet to prevent over-smoothing
2. FocalLossGAT: Focal loss integration for class imbalance
3. HierarchicalClassifier: Two-stage model exploiting determined structure
4. DropEdgeGAT: Random edge dropping to prevent over-smoothing

All models designed to avoid the "collapse to majority class" problem.
"""

from typing import Optional, Tuple, List

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import GCNConv, GATConv
from torch_geometric.utils import dropout_edge

from .config import (
    NUM_FEATURES,
    NUM_CLASSES,
    HIDDEN_DIM,
    DROPOUT_RATE,
    NUM_ATTENTION_HEADS,
    NUM_EDGE_FEATURES,
)
from .edge_features import compute_polynomial_edge_features


class DepthAwareGATv2(nn.Module):
    """
    Improved GAT with anti-collapse mechanisms:
    
    1. Skip connections: Preserve original features through each layer
    2. Jumping Knowledge (JKNet): Concatenate all layer representations
    3. Reduced layers: 2 layers instead of 3 to reduce over-smoothing
    4. Layer normalization: Stabilize training
    
    Architecture:
        Input -> [GAT + Skip + LayerNorm] x2 -> JK Concat -> Classifier
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
        self.dropout = dropout
        self.heads = heads
        self.use_skip = use_skip
        self.use_jk = use_jk
        
        # Edge feature encoder
        self.edge_encoder = nn.Sequential(
            nn.Linear(NUM_EDGE_FEATURES, hidden_dim),
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
    ) -> torch.Tensor:
        """Forward pass with skip connections and JKNet."""
        
        # Compute edge features
        edge_feat = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_feat)
        
        # Project input for skip connections
        h0 = self.input_proj(x)  # [N, hidden*heads]
        
        # Layer 1 with skip
        h1 = self.conv1(x, edge_index, edge_attr=edge_attr)
        h1 = F.elu(h1)
        if self.use_skip:
            h1 = h1 + h0  # Skip connection
        h1 = self.norm1(h1)
        h1 = F.dropout(h1, p=self.dropout, training=self.training)
        
        # Layer 2 with skip
        h2 = self.conv2(h1, edge_index, edge_attr=edge_attr)
        h2 = F.elu(h2)
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


class DropEdgeGAT(nn.Module):
    """
    GAT with DropEdge regularization to prevent over-smoothing.
    
    DropEdge randomly removes edges during training, which:
    1. Reduces the information flow between nodes
    2. Acts like dropout but on the graph structure
    3. Prevents all nodes from converging to similar representations
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        edge_drop_rate: float = 0.2,  # Drop 20% of edges
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.edge_drop_rate = edge_drop_rate
        
        # Edge encoder
        self.edge_encoder = nn.Sequential(
            nn.Linear(NUM_EDGE_FEATURES, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, heads),
        )
        
        # Two GAT layers
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
        
        self.classifier = nn.Linear(hidden_dim * heads, num_classes)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        wNum: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """Forward pass with DropEdge."""
        
        # Compute edge features on full graph
        edge_feat = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_feat)
        
        # DropEdge: randomly drop edges during training
        if self.training and self.edge_drop_rate > 0:
            edge_index_dropped, edge_mask = dropout_edge(
                edge_index, p=self.edge_drop_rate, training=True
            )
            edge_attr_dropped = edge_attr[edge_mask]
        else:
            edge_index_dropped = edge_index
            edge_attr_dropped = edge_attr
        
        # GAT layers with dropped edges
        h = self.conv1(x, edge_index_dropped, edge_attr=edge_attr_dropped)
        h = F.elu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index_dropped, edge_attr=edge_attr_dropped)
        h = F.elu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        out = self.classifier(h)
        return F.log_softmax(out, dim=1)
    
    def reset_parameters(self):
        for layer in self.edge_encoder:
            if hasattr(layer, 'reset_parameters'):
                layer.reset_parameters()
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.classifier.reset_parameters()


class HierarchicalClassifier(nn.Module):
    """
    Two-stage hierarchical classifier exploiting the data structure:
    
    Stage 1: Predict determined (0 or 1) using shared features
    Stage 2a: If determined=0, predict rootCount ∈ {0, 1}
    Stage 2b: If determined=1, predict rootCount ∈ {1, 2, 3, 4}
    
    This matches the actual data structure where:
    - determined=0 nodes only have rootCount 0 or 1
    - determined=1 nodes have rootCount 1, 2, 3, or 4
    
    During training, uses ground truth determined for routing.
    During inference, uses predicted determined.
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        self.num_classes = num_classes
        
        # Shared GCN backbone
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # Stage 1: determined classifier (binary)
        self.determined_head = nn.Linear(hidden_dim, 2)
        
        # Stage 2a: rootCount for determined=0 (classes 0, 1)
        self.undetermined_head = nn.Linear(hidden_dim, 2)
        
        # Stage 2b: rootCount for determined=1 (classes 1, 2, 3, 4)
        self.determined_root_head = nn.Linear(hidden_dim, 4)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        determined_labels: Optional[torch.Tensor] = None,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Forward pass.
        
        Args:
            x: Node features [N, num_features]
            edge_index: Edge connectivity [2, E]
            determined_labels: Ground truth determined labels for training routing
            
        Returns:
            Tuple of (determined_logprobs, rootcount_logprobs)
            rootcount_logprobs has shape [N, num_classes] with proper class mapping
        """
        # Shared backbone
        h = self.conv1(x, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        # Stage 1: predict determined
        det_logits = self.determined_head(h)
        det_logprobs = F.log_softmax(det_logits, dim=1)
        
        # Stage 2: predict rootCount based on determined
        # During training, use ground truth; during inference, use prediction
        if self.training and determined_labels is not None:
            det_mask = determined_labels.bool()
        else:
            det_mask = det_logits.argmax(dim=1).bool()
        
        # Initialize output logits
        root_logits = torch.full(
            (x.shape[0], self.num_classes), 
            float('-inf'), 
            device=x.device
        )
        
        # Undetermined nodes (det=0): can be class 0 or 1
        undet_mask = ~det_mask
        if undet_mask.any():
            undet_logits = self.undetermined_head(h[undet_mask])  # [n_undet, 2]
            root_logits[undet_mask, 0] = undet_logits[:, 0]  # class 0
            root_logits[undet_mask, 1] = undet_logits[:, 1]  # class 1
        
        # Determined nodes (det=1): can be class 1, 2, 3, 4
        if det_mask.any():
            det_root_logits = self.determined_root_head(h[det_mask])  # [n_det, 4]
            root_logits[det_mask, 1] = det_root_logits[:, 0]  # class 1
            root_logits[det_mask, 2] = det_root_logits[:, 1]  # class 2
            root_logits[det_mask, 3] = det_root_logits[:, 2]  # class 3
            root_logits[det_mask, 4] = det_root_logits[:, 3]  # class 4
        
        root_logprobs = F.log_softmax(root_logits, dim=1)
        
        return det_logprobs, root_logprobs
    
    def predict(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """Get class predictions."""
        with torch.no_grad():
            _, root_logprobs = self(x, edge_index)
            return root_logprobs.argmax(dim=1)
    
    def reset_parameters(self):
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.determined_head.reset_parameters()
        self.undetermined_head.reset_parameters()
        self.determined_root_head.reset_parameters()


class FocalLossClassifier(nn.Module):
    """
    GAT with built-in focal loss computation.
    
    Focal loss: FL(p) = -α(1-p)^γ * log(p)
    
    This down-weights easy examples (majority class) and focuses on hard ones.
    γ=2 is typical; higher values focus more on hard examples.
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
        heads: int = NUM_ATTENTION_HEADS,
        focal_gamma: float = 2.0,
    ):
        super().__init__()
        self.dropout = dropout
        self.heads = heads
        self.focal_gamma = focal_gamma
        self.num_classes = num_classes
        
        # Edge encoder
        self.edge_encoder = nn.Sequential(
            nn.Linear(NUM_EDGE_FEATURES, hidden_dim),
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
    ) -> torch.Tensor:
        """Forward pass returning logits (not log_softmax for focal loss)."""
        
        edge_feat = compute_polynomial_edge_features(x, edge_index)
        edge_attr = self.edge_encoder(edge_feat)
        
        # Skip connection from input
        skip = self.skip_proj(x)
        
        h = self.conv1(x, edge_index, edge_attr=edge_attr)
        h = F.elu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index, edge_attr=edge_attr)
        h = F.elu(h)
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


class RegressionWarmstartClassifier(nn.Module):
    """
    Classifier that can be initialized from a trained regression model.
    
    Strategy:
    1. Train regression model first (which doesn't collapse)
    2. Copy backbone weights to this classifier
    3. Fine-tune with classification loss
    
    This gives the model a good starting point in feature space.
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = NUM_CLASSES,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        self.num_classes = num_classes
        
        # Same architecture as RegressionClassifier for weight transfer
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # Classification head (replaces regression head)
        self.classifier = nn.Linear(hidden_dim, num_classes)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """Forward pass."""
        h = self.conv1(x, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        h = self.conv2(h, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        out = self.classifier(h)
        return F.log_softmax(out, dim=1)
    
    def load_from_regression(self, regression_model: nn.Module):
        """
        Copy backbone weights from a trained regression model.
        
        Args:
            regression_model: Trained RegressionClassifier instance
        """
        # Copy GCN weights
        self.conv1.load_state_dict(regression_model.conv1.state_dict())
        self.conv2.load_state_dict(regression_model.conv2.state_dict())
        
        # Initialize classifier from regression head (expand 1 -> num_classes)
        with torch.no_grad():
            reg_weight = regression_model.regressor.weight  # [1, hidden_dim]
            reg_bias = regression_model.regressor.bias  # [1]
            
            # Initialize each class weight as scaled version of regression weight
            # This creates a reasonable starting point
            for i in range(self.num_classes):
                scale = i / (self.num_classes - 1)  # 0, 0.25, 0.5, 0.75, 1.0
                self.classifier.weight[i] = reg_weight.squeeze() * scale
                self.classifier.bias[i] = reg_bias.item() * scale
        
        print("Loaded backbone weights from regression model")
    
    def reset_parameters(self):
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.classifier.reset_parameters()


def get_improved_model(model_name: str, **kwargs) -> nn.Module:
    """
    Factory function for improved models.
    
    Args:
        model_name: One of 'gat_v2', 'drop_edge', 'hierarchical', 
                    'focal', 'warmstart'
        **kwargs: Additional arguments passed to model constructor
        
    Returns:
        Initialized model instance
    """
    models = {
        'gat_v2': DepthAwareGATv2,
        'drop_edge': DropEdgeGAT,
        'hierarchical': HierarchicalClassifier,
        'focal': FocalLossClassifier,
        'warmstart': RegressionWarmstartClassifier,
    }
    
    if model_name.lower() not in models:
        raise ValueError(
            f"Unknown model: {model_name}. "
            f"Available: {list(models.keys())}"
        )
    
    return models[model_name.lower()](**kwargs)
