"""
Prediction and Neo4j write-back for Root Count Prediction.

This module provides functionality to:
- Generate root count predictions and probabilities for all nodes
- Write prediction results back to Neo4j database
- Analyze prediction accuracy and errors

IMPROVED: Supports depth-aware models that use wNum edge features.
"""

import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
import torch
from torch import nn
from torch_geometric.data import Data

# Add parent directory to path to import neo4jClient
sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'neo4j'))
from neo4jClient import Neo4jClient

from .config import (
    PREDICTION_PROPERTY,
    PROBABILITY_PROPERTY,
    BATCH_WRITE_QUERY,
)
from .models import DepthAwareGAT


class NodePredictor:
    """
    Generates root count predictions and writes results back to Neo4j.
    
    Predicts the number of rational roots (totalZero) for each polynomial node.
    
    Example:
        >>> predictor = NodePredictor(model, data)
        >>> predictions = predictor.predict()
        >>> predictor.write_to_neo4j(client, "d4seed1")
    """
    
    def __init__(
        self,
        model: nn.Module,
        data: Data,
        device: Optional[str] = None,
    ):
        """
        Initialize the predictor.
        
        Args:
            model: Trained classification model
            data: PyG Data object with node_ids attribute
            device: Device to use (None for auto-detect)
        """
        self.model = model
        self.data = data
        
        if device is None:
            self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        else:
            self.device = device
        
        self.model = self.model.to(self.device)
        self.data = self.data.to(self.device)
        
        # Check if model is depth-aware
        self.is_depth_aware = isinstance(self.model, DepthAwareGAT)
        
        # Get wNum for depth-aware models
        self.wNum = getattr(self.data, 'wNum', None)
        if self.wNum is not None:
            self.wNum = self.wNum.to(self.device)
        
        # Prediction results
        self._predictions: Optional[np.ndarray] = None
        self._probabilities: Optional[np.ndarray] = None
    
    def _forward(self) -> torch.Tensor:
        """Forward pass handling both standard and depth-aware models."""
        if self.is_depth_aware and self.wNum is not None:
            return self.model(self.data.x, self.data.edge_index, self.wNum)
        else:
            return self.model(self.data.x, self.data.edge_index)
    
    def predict(self) -> Dict:
        """
        Generate predictions for all nodes.
        
        Returns:
            Dictionary with predictions, probabilities, and node IDs
        """
        self.model.eval()
        with torch.no_grad():
            out = self._forward()
            
            # Get class predictions
            self._predictions = out.argmax(dim=1).cpu().numpy()
            
            # Convert log-softmax to probabilities
            self._probabilities = torch.exp(out).cpu().numpy()
        
        # Get node IDs (stored as list of strings)
        node_ids = self.data.node_ids if isinstance(self.data.node_ids, (list, np.ndarray)) else list(self.data.node_ids)
        
        return {
            'node_ids': node_ids,
            'predictions': self._predictions,
            'probabilities': self._probabilities,
        }
    
    def write_to_neo4j(
        self,
        client: Neo4jClient,
        database: str,
        batch_size: int = 100,
        verbose: bool = True,
    ) -> int:
        """
        Write predictions back to Neo4j database.
        
        Args:
            client: Neo4jClient instance
            database: Target database name
            batch_size: Number of nodes to update per batch
            verbose: Whether to print progress
            
        Returns:
            Number of nodes updated
        """
        if self._predictions is None:
            self.predict()
        
        # node_ids is stored as a list of strings (elementIds)
        node_ids = self.data.node_ids if isinstance(self.data.node_ids, list) else list(self.data.node_ids)
        total_nodes = len(node_ids)
        nodes_updated = 0
        
        # Process in batches
        for i in range(0, total_nodes, batch_size):
            batch_end = min(i + batch_size, total_nodes)
            
            # Prepare batch data
            batch_predictions = []
            for j in range(i, batch_end):
                pred_data = {
                    'node_id': str(node_ids[j]),  # elementId is string
                    'prediction': int(self._predictions[j]),
                    'probabilities': self._probabilities[j].tolist(),
                }
                batch_predictions.append(pred_data)
            
            # Execute batch write
            client.run_query(
                BATCH_WRITE_QUERY,
                database,
                {'predictions': batch_predictions},
            )
            
            nodes_updated += len(batch_predictions)
            
            if verbose and (i + batch_size) % 500 == 0:
                print(f"Updated {nodes_updated}/{total_nodes} nodes...")
        
        if verbose:
            print(f"✓ Successfully wrote predictions to {nodes_updated} nodes")
        
        return nodes_updated
    
    def get_prediction_stats(self) -> Dict:
        """
        Get statistics about the predictions.
        
        Returns:
            Dictionary with prediction statistics
        """
        if self._predictions is None:
            self.predict()
        
        unique, counts = np.unique(self._predictions, return_counts=True)
        class_dist = {int(u): int(c) for u, c in zip(unique, counts)}
        
        # Confidence statistics
        max_probs = self._probabilities.max(axis=1)
        
        return {
            'total_nodes': len(self._predictions),
            'class_distribution': class_dist,
            'mean_confidence': float(np.mean(max_probs)),
            'min_confidence': float(np.min(max_probs)),
            'max_confidence': float(np.max(max_probs)),
        }
    
    def get_uncertain_predictions(
        self,
        threshold: float = 0.6,
    ) -> Tuple[List, np.ndarray]:
        """
        Get nodes with uncertain predictions (low confidence).
        
        Args:
            threshold: Probability threshold for uncertainty
            
        Returns:
            Tuple of (node_ids, probabilities) for uncertain nodes
        """
        if self._predictions is None:
            self.predict()
        
        node_ids = self.data.node_ids if isinstance(self.data.node_ids, list) else list(self.data.node_ids)
        max_probs = self._probabilities.max(axis=1)
        
        uncertain_mask = max_probs < threshold
        
        uncertain_node_ids = [nid for nid, m in zip(node_ids, uncertain_mask) if m]
        return uncertain_node_ids, self._probabilities[uncertain_mask]
    
    def compare_with_ground_truth(self) -> Dict:
        """
        Compare predictions with ground truth root counts.
        
        Returns:
            Dictionary with comparison results including:
            - accuracy: Overall accuracy
            - class_accuracies: Per-class (per root count) accuracy
            - mae: Mean Absolute Error (avg root count difference)
            - confusion_matrix: Full confusion matrix for multi-class
        """
        if self._predictions is None:
            self.predict()
        
        ground_truth = self.data.y.cpu().numpy()
        
        # Accuracy
        correct = (self._predictions == ground_truth).sum()
        accuracy = correct / len(ground_truth)
        
        # Per-class accuracy (per root count)
        class_accuracies = {}
        for cls in np.unique(ground_truth):
            mask = ground_truth == cls
            cls_correct = (self._predictions[mask] == ground_truth[mask]).sum()
            class_accuracies[int(cls)] = float(cls_correct / mask.sum())
        
        # Mean Absolute Error (interpretable as "avg roots off")
        mae = float(np.mean(np.abs(self._predictions - ground_truth)))
        
        # Build confusion matrix
        num_classes = max(ground_truth.max(), self._predictions.max()) + 1
        confusion_matrix = np.zeros((num_classes, num_classes), dtype=int)
        for true, pred in zip(ground_truth, self._predictions):
            confusion_matrix[true, pred] += 1
        
        return {
            'accuracy': float(accuracy),
            'class_accuracies': class_accuracies,
            'mae': mae,
            'confusion_matrix': confusion_matrix.tolist(),
        }
    
    def get_misclassified_nodes(self) -> Dict:
        """
        Get information about misclassified nodes.
        
        Returns:
            Dictionary with misclassified node details
        """
        if self._predictions is None:
            self.predict()
        
        node_ids = self.data.node_ids if isinstance(self.data.node_ids, list) else list(self.data.node_ids)
        ground_truth = self.data.y.cpu().numpy()
        
        misclassified_mask = self._predictions != ground_truth
        
        misclassified_node_ids = [nid for nid, m in zip(node_ids, misclassified_mask) if m]
        
        return {
            'node_ids': misclassified_node_ids,
            'predictions': self._predictions[misclassified_mask].tolist(),
            'ground_truth': ground_truth[misclassified_mask].tolist(),
            'probabilities': self._probabilities[misclassified_mask].tolist(),
            'count': int(misclassified_mask.sum()),
        }


def predict_and_write(
    model: nn.Module,
    data: Data,
    client: Neo4jClient,
    database: str,
    verbose: bool = True,
) -> Dict:
    """
    Convenience function to generate root count predictions and write to Neo4j.
    
    Args:
        model: Trained model
        data: PyG Data object
        client: Neo4jClient instance
        database: Target database name
        verbose: Whether to print progress
        
    Returns:
        Dictionary with prediction statistics
        
    Example:
        >>> stats = predict_and_write(model, data, client, "d4seed1")
        >>> print(f"Updated {stats['nodes_updated']} nodes")
        >>> print(f"MAE: {stats['mae']:.4f} roots")
    """
    predictor = NodePredictor(model, data)
    
    # Generate predictions
    predictions = predictor.predict()
    
    if verbose:
        stats = predictor.get_prediction_stats()
        print(f"Generated root count predictions for {stats['total_nodes']} nodes")
        print(f"Root count distribution: {stats['class_distribution']}")
        print(f"Mean confidence: {stats['mean_confidence']:.4f}")
    
    # Write to Neo4j
    nodes_updated = predictor.write_to_neo4j(client, database, verbose=verbose)
    
    # Get comparison with ground truth
    comparison = predictor.compare_with_ground_truth()
    
    if verbose:
        print(f"\nAccuracy: {comparison['accuracy']:.4f}")
        print(f"MAE: {comparison['mae']:.4f} roots")
        print(f"Per-class accuracies: {comparison['class_accuracies']}")
    
    return {
        'nodes_updated': nodes_updated,
        **predictor.get_prediction_stats(),
        **comparison,
    }

