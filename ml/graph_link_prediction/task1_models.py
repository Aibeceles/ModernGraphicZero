"""
Model architectures for Task 1: SAME_DENOMINATOR Link Prediction.

This module provides three approaches for predicting rational partitioning:
1. RationalPartitionClassifier: Classification on (n,d) labels
2. SameDenominatorLinkPredictor: Pairwise binary edge classifier
3. ContrastiveEmbedder + ContrastiveLoss: Learn embeddings via contrastive learning

All models predict which nodes share the same rational value μ = n/d.
"""

from typing import Optional, Tuple

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import GCNConv, SAGEConv, GATConv

from .config import (
    NUM_FEATURES_TASK1,
    HIDDEN_DIM,
    EMBEDDING_DIM,
    DROPOUT_RATE,
    NUM_ATTENTION_HEADS,
    CONTRASTIVE_TEMPERATURE,
)


# =============================================================================
# Approach 1: Partition Classifier
# =============================================================================

class RationalPartitionClassifier(nn.Module):
    """
    Classify nodes into (n,d) equivalence classes.
    
    This is the simplest approach: treat each unique (n,d) pair as a class,
    and perform multi-class classification. After prediction, edges are derived
    by connecting all nodes within the same predicted class.
    
    Architecture:
        Input -> GCN -> GCN -> Linear -> LogSoftmax
    
    Args:
        num_features: Number of input features (default: 7)
        hidden_dim: Hidden layer dimension
        num_classes: Number of unique (n,d) partitions
        dropout: Dropout probability
        
    Example:
        >>> model = RationalPartitionClassifier(num_features=7, num_classes=50)
        >>> out = model(data.x, data.edge_index)
        >>> predicted_classes = out.argmax(dim=1)
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK1,
        hidden_dim: int = HIDDEN_DIM,
        num_classes: int = None,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        
        if num_classes is None:
            raise ValueError("num_classes must be specified for RationalPartitionClassifier")
        
        self.dropout = dropout
        self.num_classes = num_classes
        
        # Two GCN layers for feature extraction
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # Classification head
        self.classifier = nn.Linear(hidden_dim, num_classes)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
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
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Second GCN layer
        x = self.conv2(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # Classification
        x = self.classifier(x)
        return F.log_softmax(x, dim=1)
    
    def predict_edges(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Predict SAME_DENOMINATOR edges by deriving them from class predictions.
        
        Args:
            x: Node feature matrix
            edge_index: Edge connectivity
            
        Returns:
            Predicted edge tensor [2, num_predicted_edges]
        """
        self.eval()
        with torch.no_grad():
            out = self.forward(x, edge_index)
            predicted_classes = out.argmax(dim=1)
            
            # Generate edges within each predicted class
            edges = []
            for class_id in torch.unique(predicted_classes):
                class_nodes = (predicted_classes == class_id).nonzero(as_tuple=True)[0]
                
                if len(class_nodes) < 2:
                    continue
                
                # All pairs within class (both directions for undirected)
                for i in range(len(class_nodes)):
                    for j in range(i + 1, len(class_nodes)):
                        edges.append([class_nodes[i].item(), class_nodes[j].item()])
                        edges.append([class_nodes[j].item(), class_nodes[i].item()])
            
            if not edges:
                return torch.zeros((2, 0), dtype=torch.long, device=x.device)
            
            return torch.tensor(edges, dtype=torch.long, device=x.device).t().contiguous()
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.classifier.reset_parameters()


# =============================================================================
# Approach 2: Pairwise Link Predictor
# =============================================================================

class LinkPredictorEncoder(nn.Module):
    """
    GNN encoder for generating node embeddings.
    
    Used by both pairwise link predictor and contrastive learning.
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK1,
        hidden_dim: int = HIDDEN_DIM,
        embedding_dim: int = EMBEDDING_DIM,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        self.fc = nn.Linear(hidden_dim, embedding_dim)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Generate node embeddings.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Node embeddings [num_nodes, embedding_dim]
        """
        x = self.conv1(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.conv2(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.fc(x)
        return x
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.fc.reset_parameters()


class SameDenominatorLinkPredictor(nn.Module):
    """
    Binary classifier for SAME_DENOMINATOR edge existence.
    
    This model learns to predict whether two nodes share the same (n,d) value
    by examining their concatenated embeddings.
    
    Architecture:
        Encoder (GNN) -> Concatenate node pairs -> MLP -> Sigmoid
    
    Args:
        num_features: Number of input features
        hidden_dim: Hidden layer dimension
        embedding_dim: Dimension of node embeddings
        dropout: Dropout probability
        
    Example:
        >>> model = SameDenominatorLinkPredictor(num_features=7)
        >>> embeddings = model.encode(data.x, data.edge_index)
        >>> prob = model.decode(embeddings[edge[0]], embeddings[edge[1]])
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK1,
        hidden_dim: int = HIDDEN_DIM,
        embedding_dim: int = EMBEDDING_DIM,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        
        # Encoder: Node features -> embeddings
        self.encoder = LinkPredictorEncoder(
            num_features, hidden_dim, embedding_dim, dropout
        )
        
        # Decoder: Concatenated embeddings -> edge probability
        self.fc1 = nn.Linear(embedding_dim * 2, hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.fc3 = nn.Linear(hidden_dim // 2, 1)
    
    def encode(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Encode nodes into embeddings.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Node embeddings [num_nodes, embedding_dim]
        """
        return self.encoder(x, edge_index)
    
    def decode(
        self,
        z_src: torch.Tensor,
        z_dst: torch.Tensor,
    ) -> torch.Tensor:
        """
        Decode edge existence probability from node embeddings.
        
        Args:
            z_src: Source node embeddings [num_edges, embedding_dim]
            z_dst: Destination node embeddings [num_edges, embedding_dim]
            
        Returns:
            Edge probabilities [num_edges]
        """
        # Concatenate source and destination embeddings
        z = torch.cat([z_src, z_dst], dim=-1)
        
        z = F.relu(self.fc1(z))
        z = F.dropout(z, p=self.dropout, training=self.training)
        
        z = F.relu(self.fc2(z))
        z = F.dropout(z, p=self.dropout, training=self.training)
        
        z = self.fc3(z)
        return torch.sigmoid(z).squeeze()
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        query_edges: torch.Tensor,
    ) -> torch.Tensor:
        """
        Forward pass for a batch of query edges.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Graph connectivity [2, num_edges]
            query_edges: Edges to predict [2, num_query_edges]
            
        Returns:
            Edge probabilities [num_query_edges]
        """
        # Encode all nodes
        z = self.encode(x, edge_index)
        
        # Decode query edges
        src_embeddings = z[query_edges[0]]
        dst_embeddings = z[query_edges[1]]
        
        return self.decode(src_embeddings, dst_embeddings)
    
    def predict_edges(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        threshold: float = 0.5,
        top_k: Optional[int] = None,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Predict SAME_DENOMINATOR edges with scores above threshold.
        
        Args:
            x: Node features
            edge_index: Graph connectivity
            threshold: Minimum probability threshold
            top_k: If specified, return only top-k edges by probability
            
        Returns:
            Tuple of (predicted_edges [2, num_edges], edge_scores [num_edges])
        """
        self.eval()
        with torch.no_grad():
            # Encode all nodes
            z = self.encode(x, edge_index)
            
            num_nodes = x.size(0)
            
            # Generate all possible edges (expensive for large graphs!)
            # For efficiency, we could sample or use batching
            edges = []
            scores = []
            
            batch_size = 1000
            for i in range(0, num_nodes, batch_size):
                end_i = min(i + batch_size, num_nodes)
                
                for j in range(0, num_nodes):
                    if i <= j < end_i:
                        continue  # Skip self-loops and lower triangle
                    
                    # Compute score for edge (i, j)
                    src_emb = z[i:end_i]
                    dst_emb = z[j].unsqueeze(0).expand(end_i - i, -1)
                    
                    batch_scores = self.decode(src_emb, dst_emb)
                    
                    for k, score in enumerate(batch_scores):
                        if score >= threshold:
                            node_i = i + k
                            edges.append([node_i, j])
                            edges.append([j, node_i])  # Undirected
                            scores.append(score.item())
                            scores.append(score.item())
            
            if not edges:
                return (
                    torch.zeros((2, 0), dtype=torch.long, device=x.device),
                    torch.zeros(0, device=x.device)
                )
            
            edges = torch.tensor(edges, dtype=torch.long, device=x.device).t()
            scores = torch.tensor(scores, device=x.device)
            
            # Apply top-k if specified
            if top_k is not None and len(scores) > top_k:
                _, top_indices = torch.topk(scores, top_k)
                edges = edges[:, top_indices]
                scores = scores[top_indices]
            
            return edges, scores
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.encoder.reset_parameters()
        self.fc1.reset_parameters()
        self.fc2.reset_parameters()
        self.fc3.reset_parameters()


# =============================================================================
# Approach 3: Contrastive Learning
# =============================================================================

class ContrastiveEmbedder(nn.Module):
    """
    Learn embeddings via contrastive loss.
    
    This model learns to generate embeddings where nodes with the same (n,d)
    are close together and nodes with different (n,d) are far apart in
    embedding space.
    
    Architecture:
        Input -> GCN -> GCN -> Linear -> L2 Normalize
    
    Args:
        num_features: Number of input features
        hidden_dim: Hidden layer dimension
        embedding_dim: Dimension of output embeddings
        dropout: Dropout probability
        
    Example:
        >>> model = ContrastiveEmbedder(num_features=7)
        >>> embeddings = model(data.x, data.edge_index)
        >>> # Train with ContrastiveLoss
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK1,
        hidden_dim: int = HIDDEN_DIM,
        embedding_dim: int = EMBEDDING_DIM,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        self.fc = nn.Linear(hidden_dim, embedding_dim)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Generate L2-normalized embeddings.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            L2-normalized embeddings [num_nodes, embedding_dim]
        """
        x = self.conv1(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.conv2(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        x = self.fc(x)
        
        # L2 normalize for contrastive learning
        x = F.normalize(x, p=2, dim=1)
        
        return x
    
    def predict_edges_from_embeddings(
        self,
        embeddings: torch.Tensor,
        similarity_threshold: float = 0.8,
    ) -> torch.Tensor:
        """
        Predict edges by clustering embeddings based on cosine similarity.
        
        Args:
            embeddings: Node embeddings [num_nodes, embedding_dim]
            similarity_threshold: Minimum cosine similarity for edge
            
        Returns:
            Predicted edges [2, num_edges]
        """
        # Compute pairwise cosine similarities (embeddings already normalized)
        similarity = torch.mm(embeddings, embeddings.t())
        
        # Find pairs with similarity above threshold
        mask = (similarity >= similarity_threshold) & (torch.eye(len(embeddings), device=embeddings.device) == 0)
        
        edges = mask.nonzero(as_tuple=False).t()
        return edges
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.fc.reset_parameters()


class ContrastiveLoss(nn.Module):
    """
    Contrastive loss for partition learning.
    
    Pulls together nodes with the same partition ID and pushes apart
    nodes with different partition IDs.
    
    Uses InfoNCE-style loss:
        L = -log(exp(sim(i,j)/τ) / Σ_k exp(sim(i,k)/τ))
    
    where j is a positive sample (same partition) and k ranges over all samples.
    
    Args:
        temperature: Scaling temperature for similarities
        
    Example:
        >>> criterion = ContrastiveLoss(temperature=0.5)
        >>> loss = criterion(embeddings, partition_labels)
    """
    
    def __init__(self, temperature: float = CONTRASTIVE_TEMPERATURE):
        super().__init__()
        self.temperature = temperature
    
    def forward(
        self,
        embeddings: torch.Tensor,
        partition_labels: torch.Tensor,
    ) -> torch.Tensor:
        """
        Compute contrastive loss.
        
        Args:
            embeddings: Node embeddings [num_nodes, embedding_dim] (L2 normalized)
            partition_labels: Partition IDs [num_nodes]
            
        Returns:
            Scalar loss value
        """
        num_nodes = embeddings.size(0)
        
        # Compute pairwise similarities (already normalized)
        similarity_matrix = torch.mm(embeddings, embeddings.t()) / self.temperature
        
        # Create mask for same partition
        labels_equal = partition_labels.unsqueeze(0) == partition_labels.unsqueeze(1)
        labels_equal.fill_diagonal_(False)  # Exclude self-similarity
        
        # For each node, compute InfoNCE loss
        losses = []
        
        for i in range(num_nodes):
            # Positive samples (same partition, excluding self)
            positive_mask = labels_equal[i]
            
            if not positive_mask.any():
                continue  # No positive samples for this node
            
            # Similarities to all other nodes
            similarities = similarity_matrix[i]
            
            # Numerator: sum over positive samples
            pos_similarities = similarities[positive_mask]
            pos_exp = torch.exp(pos_similarities)
            
            # Denominator: sum over all samples (excluding self)
            all_mask = torch.ones(num_nodes, dtype=torch.bool, device=embeddings.device)
            all_mask[i] = False
            all_exp = torch.exp(similarities[all_mask])
            
            # InfoNCE loss for node i
            loss_i = -torch.log(pos_exp.sum() / all_exp.sum())
            losses.append(loss_i)
        
        if not losses:
            return torch.tensor(0.0, device=embeddings.device)
        
        return torch.stack(losses).mean()


# =============================================================================
# Model Factory
# =============================================================================

def get_task1_model(
    model_name: str,
    num_features: int = NUM_FEATURES_TASK1,
    num_classes: Optional[int] = None,
    **kwargs
) -> nn.Module:
    """
    Factory function to create Task 1 models by name.
    
    Args:
        model_name: One of 'classifier', 'pairwise', 'contrastive'
        num_features: Number of input features
        num_classes: Number of partition classes (required for 'classifier')
        **kwargs: Additional arguments for model constructor
        
    Returns:
        Initialized model instance
        
    Raises:
        ValueError: If model_name is not recognized
    """
    models = {
        'classifier': RationalPartitionClassifier,
        'pairwise': SameDenominatorLinkPredictor,
        'contrastive': ContrastiveEmbedder,
    }
    
    if model_name.lower() not in models:
        raise ValueError(
            f"Unknown Task 1 model: {model_name}. "
            f"Available models: {list(models.keys())}"
        )
    
    model_class = models[model_name.lower()]
    
    # Special handling for classifier which requires num_classes
    if model_name.lower() == 'classifier':
        if num_classes is None:
            raise ValueError("num_classes must be specified for classifier model")
        return model_class(num_features=num_features, num_classes=num_classes, **kwargs)
    
    return model_class(num_features=num_features, **kwargs)

