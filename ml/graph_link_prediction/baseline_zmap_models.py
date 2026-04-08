"""
Baseline models for recreating original Neo4j GDS link prediction experiment.

This module recreates the GraphicLinkPrediction.json pipeline which predicted
:zMap edges using GraphSage embeddings and link feature combiners.

Original configuration:
- Feature: dd (d value as float)
- GraphSage: 4 layers, pool aggregator, 64-dim embeddings
- Link predictor: Logistic regression with HADAMARD/L2/COSINE combiners
- Best: penalty=10000.0, combiner=COSINE, AUCPR=0.5545
"""

from typing import Tuple, Optional

import torch
import torch.nn.functional as F
from torch import nn
from torch_geometric.nn import SAGEConv


class GraphSageEncoder(nn.Module):
    """
    GraphSage encoder matching Neo4j GDS configuration.
    
    Original parameters:
    - 4 layers with sample sizes [25, 10, 10, 10]
    - Pool aggregator (max in PyG)
    - 64-dim final embeddings
    - Input: d value only (1 feature)
    
    Args:
        num_features: Number of input features (default: 1 for d value)
        hidden_dim: Hidden layer dimensions (default: 64)
        embedding_dim: Final embedding dimension (default: 64)
        num_layers: Number of SAGE layers (default: 4)
        
    Example:
        >>> encoder = GraphSageEncoder(num_features=1, embedding_dim=64)
        >>> embeddings = encoder(data.x, data.edge_index)
    """
    
    def __init__(
        self,
        num_features: int = 1,
        hidden_dim: int = 64,
        embedding_dim: int = 64,
        num_layers: int = 4,
    ):
        super().__init__()
        
        self.num_layers = num_layers
        
        # First layer: features -> hidden_dim
        self.conv_layers = nn.ModuleList([
            SAGEConv(num_features, hidden_dim, aggr='max')  # pool ≈ max
        ])
        
        # Middle layers: hidden_dim -> hidden_dim
        for _ in range(num_layers - 2):
            self.conv_layers.append(
                SAGEConv(hidden_dim, hidden_dim, aggr='max')
            )
        
        # Final layer: hidden_dim -> embedding_dim
        self.conv_layers.append(
            SAGEConv(hidden_dim, embedding_dim, aggr='max')
        )
    
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
        h = x
        
        for i, conv in enumerate(self.conv_layers):
            h = conv(h, edge_index)
            
            # ReLU activation (except maybe on last layer - but original uses it)
            h = F.relu(h)
        
        return h
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        for conv in self.conv_layers:
            conv.reset_parameters()


class LinkFeatureCombiner:
    """
    Compute edge features from node embeddings using different combiners.
    
    Matches Neo4j GDS link feature combiners:
    - HADAMARD: Element-wise product
    - L2: Squared L2 distance
    - COSINE: Cosine similarity
    """
    
    @staticmethod
    def hadamard(z_src: torch.Tensor, z_dst: torch.Tensor) -> torch.Tensor:
        """
        Hadamard product (element-wise multiplication).
        
        Args:
            z_src: Source embeddings [num_edges, embedding_dim]
            z_dst: Destination embeddings [num_edges, embedding_dim]
            
        Returns:
            Edge features [num_edges, embedding_dim]
        """
        return z_src * z_dst
    
    @staticmethod
    def l2(z_src: torch.Tensor, z_dst: torch.Tensor) -> torch.Tensor:
        """
        L2 distance (squared).
        
        Args:
            z_src: Source embeddings [num_edges, embedding_dim]
            z_dst: Destination embeddings [num_edges, embedding_dim]
            
        Returns:
            Edge features [num_edges, embedding_dim]
        """
        return (z_src - z_dst).pow(2)
    
    @staticmethod
    def cosine(z_src: torch.Tensor, z_dst: torch.Tensor) -> torch.Tensor:
        """
        Cosine similarity.
        
        Args:
            z_src: Source embeddings [num_edges, embedding_dim]
            z_dst: Destination embeddings [num_edges, embedding_dim]
            
        Returns:
            Edge features [num_edges, 1] (scalar similarity)
        """
        # Compute cosine similarity for each pair
        # F.cosine_similarity computes along dim=1 by default
        cos_sim = F.cosine_similarity(z_src, z_dst, dim=1)
        return cos_sim.unsqueeze(-1)  # [num_edges, 1]
    
    @staticmethod
    def combine(
        z_src: torch.Tensor,
        z_dst: torch.Tensor,
        combiner: str
    ) -> torch.Tensor:
        """
        Combine embeddings using specified method.
        
        Args:
            z_src: Source embeddings
            z_dst: Destination embeddings
            combiner: One of 'hadamard', 'l2', 'cosine'
            
        Returns:
            Combined edge features
        """
        combiner = combiner.lower()
        
        if combiner == 'hadamard':
            return LinkFeatureCombiner.hadamard(z_src, z_dst)
        elif combiner == 'l2':
            return LinkFeatureCombiner.l2(z_src, z_dst)
        elif combiner == 'cosine':
            return LinkFeatureCombiner.cosine(z_src, z_dst)
        else:
            raise ValueError(f"Unknown combiner: {combiner}")


class ZMapLinkPredictor(nn.Module):
    """
    Link prediction model for :zMap edges.
    
    Matches the Neo4j GDS logistic regression link predictor:
    - Takes combined edge features
    - Predicts edge probability via logistic regression
    
    Architecture:
        Edge features -> Linear -> Sigmoid
    
    Args:
        edge_feature_dim: Dimension of combined edge features
        combiner: Feature combiner type ('hadamard', 'l2', 'cosine')
        
    Example:
        >>> encoder = GraphSageEncoder()
        >>> predictor = ZMapLinkPredictor(edge_feature_dim=64, combiner='cosine')
        >>> embeddings = encoder(data.x, train_edges)
        >>> probs = predictor(embeddings, query_edges)
    """
    
    def __init__(
        self,
        edge_feature_dim: int = 64,
        combiner: str = 'hadamard',
    ):
        super().__init__()
        
        self.combiner = combiner
        
        # For COSINE, edge features are 1-dim (scalar similarity)
        # For HADAMARD and L2, edge features are embedding_dim
        if combiner.lower() == 'cosine':
            input_dim = 1
        else:
            input_dim = edge_feature_dim
        
        # Logistic regression: Linear layer + sigmoid
        self.classifier = nn.Linear(input_dim, 1)
    
    def forward(
        self,
        embeddings: torch.Tensor,
        edges: torch.Tensor,
    ) -> torch.Tensor:
        """
        Predict edge probabilities.
        
        Args:
            embeddings: Node embeddings [num_nodes, embedding_dim]
            edges: Query edges [2, num_query_edges]
            
        Returns:
            Edge probabilities [num_query_edges]
        """
        # Get source and destination embeddings
        z_src = embeddings[edges[0]]
        z_dst = embeddings[edges[1]]
        
        # Combine features
        edge_features = LinkFeatureCombiner.combine(z_src, z_dst, self.combiner)
        
        # Predict probability
        logits = self.classifier(edge_features)
        probs = torch.sigmoid(logits).squeeze()
        
        return probs
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.classifier.reset_parameters()


class ZMapPipeline(nn.Module):
    """
    Complete pipeline: GraphSage encoder + Link predictor.
    
    This combines the encoder and predictor into a single model
    for end-to-end training and inference.
    
    Args:
        num_features: Number of input features
        embedding_dim: Embedding dimension
        combiner: Link feature combiner
        
    Example:
        >>> model = ZMapPipeline(num_features=1, combiner='cosine')
        >>> probs = model(data.x, train_edges, query_edges)
    """
    
    def __init__(
        self,
        num_features: int = 1,
        embedding_dim: int = 64,
        combiner: str = 'hadamard',
    ):
        super().__init__()
        
        self.encoder = GraphSageEncoder(
            num_features=num_features,
            embedding_dim=embedding_dim,
        )
        
        self.predictor = ZMapLinkPredictor(
            edge_feature_dim=embedding_dim,
            combiner=combiner,
        )
    
    def forward(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
        query_edges: torch.Tensor,
    ) -> torch.Tensor:
        """
        End-to-end forward pass.
        
        Args:
            x: Node features [num_nodes, num_features]
            edge_index: Training graph edges [2, num_train_edges]
            query_edges: Edges to predict [2, num_query_edges]
            
        Returns:
            Edge probabilities [num_query_edges]
        """
        # Encode nodes using training graph
        embeddings = self.encoder(x, edge_index)
        
        # Predict query edges
        probs = self.predictor(embeddings, query_edges)
        
        return probs
    
    def encode(
        self,
        x: torch.Tensor,
        edge_index: torch.Tensor,
    ) -> torch.Tensor:
        """Get node embeddings."""
        return self.encoder(x, edge_index)
    
    def predict_edges(
        self,
        embeddings: torch.Tensor,
        edges: torch.Tensor,
    ) -> torch.Tensor:
        """Predict edge probabilities from embeddings."""
        return self.predictor(embeddings, edges)
    
    def reset_parameters(self):
        """Reset all learnable parameters."""
        self.encoder.reset_parameters()
        self.predictor.reset_parameters()

