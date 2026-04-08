"""
Model architectures for Task 2: NEXT_INTEGER Link Prediction.

This module provides two approaches for predicting sequential ordering:
1. IntegerValuePredictor: Regression to predict integer values, then order nodes
2. NextIntegerLinkPredictor: Pairwise binary classifier for consecutive edges

Both models predict NEXT_INTEGER edges (i → i+1) within rational partitions.
"""

from typing import Optional, Tuple

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import GCNConv, SAGEConv

from .config import (
    NUM_FEATURES_TASK2,
    HIDDEN_DIM,
    EMBEDDING_DIM,
    DROPOUT_RATE,
)


# =============================================================================
# Approach 1: Integer Value Regressor
# =============================================================================

class IntegerValuePredictor(nn.Module):
    """
    Predict integer values from node features using regression.
    
    This model learns to predict the integer value encoded in muList directly.
    After prediction, nodes are sorted by their predicted values, and consecutive
    nodes within the same partition are connected.
    
    Architecture:
        Input -> GCN -> GCN -> Linear -> Scalar output
    
    Args:
        num_features: Number of input features (default: 5)
        hidden_dim: Hidden layer dimension
        dropout: Dropout probability
        
    Example:
        >>> model = IntegerValuePredictor(num_features=5)
        >>> predicted_ints = model(data.x, data.edge_index)
        >>> # Sort nodes by predicted values and connect consecutive
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK2,
        hidden_dim: int = HIDDEN_DIM,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        
        # Two GCN layers for feature extraction
        self.conv1 = GCNConv(num_features, hidden_dim)
        self.conv2 = GCNConv(hidden_dim, hidden_dim)
        
        # Regression head
        self.fc1 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.fc2 = nn.Linear(hidden_dim // 2, 1)
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """
        Forward pass to predict integer values.
        
        Args:
            x: Node feature matrix [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            
        Returns:
            Predicted integer values [num_nodes]
        """
        # First GCN layer
        h = self.conv1(x, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        # Second GCN layer
        h = self.conv2(h, edge_index)
        h = F.relu(h)
        h = F.dropout(h, p=self.dropout, training=self.training)
        
        # Regression head
        h = F.relu(self.fc1(h))
        h = F.dropout(h, p=self.dropout, training=self.training)
        h = self.fc2(h)
        
        return h.squeeze()
    
    def predict_edges(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        partition_ids: torch.Tensor,
    ) -> torch.Tensor:
        """
        Predict NEXT_INTEGER edges by ordering nodes within partitions.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Edge connectivity [2, num_edges]
            partition_ids: Partition ID for each node [num_nodes]
            
        Returns:
            Predicted NEXT_INTEGER edges [2, num_predicted_edges]
        """
        self.eval()
        with torch.no_grad():
            # Predict integer values
            predicted_ints = self.forward(x, edge_index)
            
            # Group nodes by partition
            unique_partitions = torch.unique(partition_ids)
            
            edges = []
            for partition_id in unique_partitions:
                partition_mask = (partition_ids == partition_id)
                partition_nodes = partition_mask.nonzero(as_tuple=True)[0]
                
                if len(partition_nodes) < 2:
                    continue
                
                # Sort nodes by predicted integer value
                partition_ints = predicted_ints[partition_nodes]
                sorted_indices = torch.argsort(partition_ints)
                sorted_nodes = partition_nodes[sorted_indices]
                
                # Connect consecutive nodes
                for i in range(len(sorted_nodes) - 1):
                    edges.append([sorted_nodes[i].item(), sorted_nodes[i + 1].item()])
            
            if not edges:
                return torch.zeros((2, 0), dtype=torch.long, device=x.device)
            
            return torch.tensor(edges, dtype=torch.long, device=x.device).t().contiguous()
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.conv1.reset_parameters()
        self.conv2.reset_parameters()
        self.fc1.reset_parameters()
        self.fc2.reset_parameters()


# =============================================================================
# Approach 2: Pairwise Consecutive Predictor
# =============================================================================

class NextIntegerEncoder(nn.Module):
    """
    GNN encoder for Task 2 node embeddings.
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK2,
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


class NextIntegerLinkPredictor(nn.Module):
    """
    Binary classifier for NEXT_INTEGER edge (i → i+1).
    
    This model learns to predict whether two nodes have consecutive integer
    values (dst = src + 1) within the same partition.
    
    Architecture:
        Encoder (GNN) -> Concatenate node pairs -> MLP -> Sigmoid
    
    Args:
        num_features: Number of input features
        hidden_dim: Hidden layer dimension
        embedding_dim: Dimension of node embeddings
        dropout: Dropout probability
        
    Example:
        >>> model = NextIntegerLinkPredictor(num_features=5)
        >>> embeddings = model.encode(data.x, data.edge_index)
        >>> prob = model.decode(embeddings[src], embeddings[dst])
    """
    
    def __init__(
        self,
        num_features: int = NUM_FEATURES_TASK2,
        hidden_dim: int = HIDDEN_DIM,
        embedding_dim: int = EMBEDDING_DIM,
        dropout: float = DROPOUT_RATE,
    ):
        super().__init__()
        self.dropout = dropout
        
        # Encoder: Node features -> embeddings
        self.encoder = NextIntegerEncoder(
            num_features, hidden_dim, embedding_dim, dropout
        )
        
        # Decoder: Concatenated embeddings -> consecutive probability
        # Also takes pairwise features (integer difference, partition match)
        decoder_input_dim = embedding_dim * 2 + 2  # embeddings + 2 pairwise features
        self.fc1 = nn.Linear(decoder_input_dim, hidden_dim)
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
        pairwise_features: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """
        Decode consecutive probability from node embeddings.
        
        Args:
            z_src: Source node embeddings [num_edges, embedding_dim]
            z_dst: Destination node embeddings [num_edges, embedding_dim]
            pairwise_features: Additional pairwise features [num_edges, 2]
                               (int_diff, same_partition)
            
        Returns:
            Consecutive probabilities [num_edges]
        """
        # Concatenate embeddings and pairwise features
        if pairwise_features is not None:
            z = torch.cat([z_src, z_dst, pairwise_features], dim=-1)
        else:
            # If no pairwise features provided, use zeros
            zeros = torch.zeros(z_src.size(0), 2, device=z_src.device)
            z = torch.cat([z_src, z_dst, zeros], dim=-1)
        
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
        int_values: Optional[torch.Tensor] = None,
        partition_ids: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """
        Forward pass for a batch of query edges.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Graph connectivity [2, num_edges]
            query_edges: Edges to predict [2, num_query_edges]
            int_values: Integer values for each node [num_nodes] (optional)
            partition_ids: Partition ID for each node [num_nodes] (optional)
            
        Returns:
            Consecutive probabilities [num_query_edges]
        """
        # Encode all nodes
        z = self.encode(x, edge_index)
        
        # Get embeddings for query edges
        src_embeddings = z[query_edges[0]]
        dst_embeddings = z[query_edges[1]]
        
        # Compute pairwise features if values are provided
        pairwise_features = None
        if int_values is not None or partition_ids is not None:
            pairwise_features = []
            
            if int_values is not None:
                int_diff = int_values[query_edges[1]] - int_values[query_edges[0]]
                pairwise_features.append(int_diff.unsqueeze(-1).float())
            else:
                pairwise_features.append(torch.zeros(query_edges.size(1), 1, device=x.device))
            
            if partition_ids is not None:
                same_partition = (partition_ids[query_edges[0]] == partition_ids[query_edges[1]]).float()
                pairwise_features.append(same_partition.unsqueeze(-1))
            else:
                pairwise_features.append(torch.zeros(query_edges.size(1), 1, device=x.device))
            
            pairwise_features = torch.cat(pairwise_features, dim=-1)
        
        return self.decode(src_embeddings, dst_embeddings, pairwise_features)
    
    def predict_edges(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        partition_ids: torch.Tensor,
        int_values: Optional[torch.Tensor] = None,
        threshold: float = 0.5,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Predict NEXT_INTEGER edges within partitions.
        
        Args:
            x: Node features
            edge_index: Graph connectivity
            partition_ids: Partition ID for each node
            int_values: True integer values (for computing pairwise features)
            threshold: Minimum probability threshold
            
        Returns:
            Tuple of (predicted_edges [2, num_edges], edge_scores [num_edges])
        """
        self.eval()
        with torch.no_grad():
            # Encode all nodes
            z = self.encode(x, edge_index)
            
            # Group nodes by partition
            unique_partitions = torch.unique(partition_ids)
            
            edges = []
            scores = []
            
            for partition_id in unique_partitions:
                partition_mask = (partition_ids == partition_id)
                partition_nodes = partition_mask.nonzero(as_tuple=True)[0]
                
                if len(partition_nodes) < 2:
                    continue
                
                # Consider all pairs within partition
                for i in range(len(partition_nodes)):
                    for j in range(len(partition_nodes)):
                        if i == j:
                            continue
                        
                        src_node = partition_nodes[i]
                        dst_node = partition_nodes[j]
                        
                        # Get embeddings
                        src_emb = z[src_node].unsqueeze(0)
                        dst_emb = z[dst_node].unsqueeze(0)
                        
                        # Compute pairwise features
                        pairwise_feats = None
                        if int_values is not None:
                            int_diff = (int_values[dst_node] - int_values[src_node]).float()
                            same_part = torch.ones(1, device=x.device)
                            # Reshape both to [1, 1] before concatenating
                            pairwise_feats = torch.cat([
                                int_diff.view(1, 1), 
                                same_part.view(1, 1)
                            ], dim=-1)  # Result: [1, 2]
                        
                        # Predict edge
                        score = self.decode(src_emb, dst_emb, pairwise_feats)
                        
                        if score >= threshold:
                            edges.append([src_node.item(), dst_node.item()])
                            scores.append(score.item())
            
            if not edges:
                return (
                    torch.zeros((2, 0), dtype=torch.long, device=x.device),
                    torch.zeros(0, device=x.device)
                )
            
            edges = torch.tensor(edges, dtype=torch.long, device=x.device).t()
            scores = torch.tensor(scores, device=x.device)
            
            return edges, scores
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.encoder.reset_parameters()
        self.fc1.reset_parameters()
        self.fc2.reset_parameters()
        self.fc3.reset_parameters()


# =============================================================================
# Model Factory
# =============================================================================

def get_task2_model(
    model_name: str,
    num_features: int = NUM_FEATURES_TASK2,
    **kwargs
) -> nn.Module:
    """
    Factory function to create Task 2 models by name.
    
    Args:
        model_name: One of 'regressor', 'pairwise'
        num_features: Number of input features
        **kwargs: Additional arguments for model constructor
        
    Returns:
        Initialized model instance
        
    Raises:
        ValueError: If model_name is not recognized
    """
    models = {
        'regressor': IntegerValuePredictor,
        'pairwise': NextIntegerLinkPredictor,
    }
    
    if model_name.lower() not in models:
        raise ValueError(
            f"Unknown Task 2 model: {model_name}. "
            f"Available models: {list(models.keys())}"
        )
    
    model_class = models[model_name.lower()]
    return model_class(num_features=num_features, **kwargs)

