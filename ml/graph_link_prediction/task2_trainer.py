"""
Training infrastructure for Task 2: NEXT_INTEGER Link Prediction.

This module provides training functionality for both approaches:
1. Regressor: Predict integer values, then order nodes
2. Pairwise: Binary classifier for consecutive edges

Includes training loops, evaluation, and hyperparameter tuning.
"""

from typing import Dict, Tuple, Optional

import numpy as np
import torch
import torch.nn.functional as F
from torch import nn
from torch.optim import Adam

from .config import (
    MAX_EPOCHS,
    MIN_EPOCHS,
    PATIENCE,
    LEARNING_RATE,
    PENALTIES,
    RANDOM_SEED,
    DEVICE,
)
from .task2_models import (
    IntegerValuePredictor,
    NextIntegerLinkPredictor,
    get_task2_model,
)
from .data_loader import generate_negative_samples, split_edges_train_test
from .evaluation import evaluate_task2_full


class Task2Trainer:
    """
    Trainer for Task 2 (NEXT_INTEGER) link prediction.
    
    Supports two training approaches:
    - Regressor: Predict integer values via regression
    - Pairwise: Binary classification on consecutive edges
    
    Example:
        >>> trainer = Task2Trainer(data_task2, device='cuda')
        >>> model, metrics = trainer.train_regressor()
        >>> print(f"Kendall's Tau: {metrics['kendalls_tau']:.4f}")
    """
    
    def __init__(
        self,
        data,
        device: str = DEVICE,
        random_seed: int = RANDOM_SEED,
    ):
        """
        Initialize the trainer.
        
        Args:
            data: PyG Data object with Task 2 features and edges
            device: Device to use ('cuda' or 'cpu')
            random_seed: Random seed for reproducibility
        """
        self.data = data.to(device)
        self.device = device
        self.random_seed = random_seed
        
        torch.manual_seed(random_seed)
        np.random.seed(random_seed)
        
        # Compute number of unique partitions
        self.num_partitions = len(torch.unique(self.data.partition_ids))
        
        print(f"Task 2 Trainer initialized:")
        print(f"  Nodes: {self.data.num_nodes}")
        print(f"  Partitions: {self.num_partitions}")
        print(f"  Ground truth NEXT_INTEGER edges: {self.data.next_int_edges.size(1)}")
        print(f"  Device: {device}\n")
    
    def train_regressor(
        self,
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        verbose: bool = True,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train integer value regressor approach.
        
        Args:
            weight_decay: L2 regularization strength
            learning_rate: Learning rate for Adam optimizer
            max_epochs: Maximum training epochs
            patience: Early stopping patience
            verbose: Whether to print progress
            
        Returns:
            Tuple of (trained model, metrics dictionary)
        """
        if verbose:
            print("=" * 70)
            print("Training Task 2: Integer Value Regressor")
            print("=" * 70)
        
        # Create model
        model = IntegerValuePredictor(
            num_features=self.data.num_features,
        ).to(self.device)
        
        optimizer = Adam(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
        
        # Target: true integer values
        target_ints = self.data.int_values.float()
        
        best_loss = float('inf')
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(max_epochs):
            # Training
            model.train()
            optimizer.zero_grad()
            
            predicted_ints = model(self.data.x, self.data.edge_index)
            loss = F.mse_loss(predicted_ints, target_ints)
            
            loss.backward()
            optimizer.step()
            
            # Early stopping
            if epoch >= MIN_EPOCHS:
                if loss < best_loss:
                    best_loss = loss
                    patience_counter = 0
                    best_model_state = {k: v.cpu().clone() for k, v in model.state_dict().items()}
                else:
                    patience_counter += 1
                    if patience_counter >= patience:
                        if verbose:
                            print(f"Early stopping at epoch {epoch}")
                        break
            
            if verbose and epoch % 10 == 0:
                mae = (predicted_ints - target_ints).abs().mean()
                print(f"Epoch {epoch:03d} | Loss: {loss:.4f} | MAE: {mae:.4f}")
        
        # Load best model
        if best_model_state is not None:
            model.load_state_dict({k: v.to(self.device) for k, v in best_model_state.items()})
        
        # Evaluation
        model.eval()
        with torch.no_grad():
            predicted_ints = model(self.data.x, self.data.edge_index)
            
            # Generate edges by ordering within partitions
            predicted_edges = model.predict_edges(
                self.data.x, self.data.edge_index, self.data.partition_ids
            )
            
            # Evaluate
            metrics = evaluate_task2_full(
                predicted_ints,
                self.data.int_values.float(),
                self.data.partition_ids,
                predicted_edges,
                self.data.next_int_edges,
            )
        
        if verbose:
            print(f"\nResults:")
            print(f"  Sequence Accuracy: {metrics['sequence_accuracy']:.4f}")
            print(f"  Kendall's Tau: {metrics['kendalls_tau']:.4f}")
            print(f"  Position MAE: {metrics['position_mae']:.4f}")
            print(f"  Link F1: {metrics['link_f1']:.4f}")
            print("=" * 70 + "\n")
        
        return model, metrics
    
    def train_pairwise(
        self,
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        negative_sample_ratio: float = 1.0,
        verbose: bool = True,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train pairwise consecutive predictor approach.
        
        Args:
            weight_decay: L2 regularization strength
            learning_rate: Learning rate
            max_epochs: Maximum training epochs
            patience: Early stopping patience
            negative_sample_ratio: Ratio of negative to positive samples
            verbose: Whether to print progress
            
        Returns:
            Tuple of (trained model, metrics dictionary)
        """
        if verbose:
            print("=" * 70)
            print("Training Task 2: Pairwise Consecutive Predictor")
            print("=" * 70)
        
        # Split edges into train/test
        train_edges, test_edges = split_edges_train_test(
            self.data.next_int_edges, test_fraction=0.2, random_seed=self.random_seed
        )
        
        # Generate negative samples for training
        # For Task 2, negatives should be within the same partition but not consecutive
        num_neg = int(train_edges.size(1) * negative_sample_ratio)
        neg_train_edges = self._generate_negative_consecutive_samples(num_neg, train_edges)
        
        if verbose:
            print(f"  Positive train edges: {train_edges.size(1)}")
            print(f"  Negative train edges: {neg_train_edges.size(1)}")
            print(f"  Test edges: {test_edges.size(1)}\n")
        
        # Create model
        model = NextIntegerLinkPredictor(
            num_features=self.data.num_features,
        ).to(self.device)
        
        optimizer = Adam(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
        
        best_loss = float('inf')
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(max_epochs):
            # Training
            model.train()
            optimizer.zero_grad()
            
            # Positive samples (consecutive edges)
            pos_pred = model(
                self.data.x, self.data.edge_index, train_edges,
                self.data.int_values, self.data.partition_ids
            )
            pos_loss = F.binary_cross_entropy(pos_pred, torch.ones_like(pos_pred))
            
            # Negative samples (non-consecutive edges)
            neg_pred = model(
                self.data.x, self.data.edge_index, neg_train_edges,
                self.data.int_values, self.data.partition_ids
            )
            neg_loss = F.binary_cross_entropy(neg_pred, torch.zeros_like(neg_pred))
            
            loss = pos_loss + neg_loss
            loss.backward()
            optimizer.step()
            
            # Early stopping
            if epoch >= MIN_EPOCHS:
                if loss < best_loss:
                    best_loss = loss
                    patience_counter = 0
                    best_model_state = {k: v.cpu().clone() for k, v in model.state_dict().items()}
                else:
                    patience_counter += 1
                    if patience_counter >= patience:
                        if verbose:
                            print(f"Early stopping at epoch {epoch}")
                        break
            
            if verbose and epoch % 10 == 0:
                print(f"Epoch {epoch:03d} | Loss: {loss:.4f}")
        
        # Load best model
        if best_model_state is not None:
            model.load_state_dict({k: v.to(self.device) for k, v in best_model_state.items()})
        
        # Evaluation
        model.eval()
        with torch.no_grad():
            # Predict edges
            predicted_edges, _ = model.predict_edges(
                self.data.x,
                self.data.edge_index,
                self.data.partition_ids,
                self.data.int_values,
                threshold=0.5,
            )
            
            # For evaluation, we need predicted integer values
            # We can derive them from the edge ordering
            predicted_ints = self._edges_to_integer_order(predicted_edges)
            
            # Evaluate
            metrics = evaluate_task2_full(
                predicted_ints,
                self.data.int_values.float(),
                self.data.partition_ids,
                predicted_edges,
                self.data.next_int_edges,
            )
        
        if verbose:
            print(f"\nResults:")
            print(f"  Sequence Accuracy: {metrics['sequence_accuracy']:.4f}")
            print(f"  Kendall's Tau: {metrics['kendalls_tau']:.4f}")
            print(f"  Position MAE: {metrics['position_mae']:.4f}")
            print(f"  Link F1: {metrics['link_f1']:.4f}")
            print("=" * 70 + "\n")
        
        return model, metrics
    
    def _generate_negative_consecutive_samples(
        self,
        num_negative: int,
        positive_edges: torch.Tensor,
    ) -> torch.Tensor:
        """
        Generate negative samples for consecutive edge prediction.
        
        Negative samples should be:
        - Within the same partition (to focus on ordering)
        - NOT consecutive integers
        
        Args:
            num_negative: Number of negative samples to generate
            positive_edges: Positive edge tensor
            
        Returns:
            Negative edge tensor [2, num_negative]
        """
        # Convert positive edges to set
        pos_edge_set = set()
        for i in range(positive_edges.size(1)):
            src = positive_edges[0, i].item()
            dst = positive_edges[1, i].item()
            pos_edge_set.add((src, dst))
        
        # Group nodes by partition
        from collections import defaultdict
        partitions = defaultdict(list)
        for idx, partition_id in enumerate(self.data.partition_ids.tolist()):
            partitions[partition_id].append(idx)
        
        # Sample negative edges within partitions
        negative_edges = []
        attempts = 0
        max_attempts = num_negative * 10
        
        while len(negative_edges) < num_negative and attempts < max_attempts:
            # Pick a random partition
            partition_id = np.random.choice(list(partitions.keys()))
            partition_nodes = partitions[partition_id]
            
            if len(partition_nodes) < 2:
                attempts += 1
                continue
            
            # Pick two random nodes in partition
            src, dst = np.random.choice(partition_nodes, size=2, replace=False)
            
            # Check if not consecutive
            src_int = self.data.int_values[src].item()
            dst_int = self.data.int_values[dst].item()
            
            if (src, dst) not in pos_edge_set and dst_int != src_int + 1:
                negative_edges.append([src, dst])
                pos_edge_set.add((src, dst))
            
            attempts += 1
        
        if len(negative_edges) == 0:
            return torch.zeros((2, 0), dtype=torch.long, device=self.device)
        
        return torch.tensor(negative_edges, dtype=torch.long, device=self.device).t().contiguous()
    
    def _edges_to_integer_order(self, edges: torch.Tensor) -> torch.Tensor:
        """
        Derive integer ordering from predicted NEXT_INTEGER edges.
        
        Uses topological sort within each partition to assign integer positions.
        
        Args:
            edges: NEXT_INTEGER edge tensor [2, num_edges]
            
        Returns:
            Predicted integer values [num_nodes]
        """
        num_nodes = self.data.num_nodes
        predicted_ints = torch.zeros(num_nodes, device=self.device)
        
        # Build adjacency list for each partition
        from collections import defaultdict
        adj = defaultdict(list)
        for i in range(edges.size(1)):
            src = edges[0, i].item()
            dst = edges[1, i].item()
            adj[src].append(dst)
        
        # For each partition, perform topological sort
        partitions = defaultdict(list)
        for idx, partition_id in enumerate(self.data.partition_ids.tolist()):
            partitions[partition_id].append(idx)
        
        for partition_id, partition_nodes in partitions.items():
            # Find nodes with no incoming edges (starts of sequences)
            in_degree = {node: 0 for node in partition_nodes}
            for node in partition_nodes:
                for neighbor in adj[node]:
                    if neighbor in in_degree:
                        in_degree[neighbor] += 1
            
            # Topological sort
            queue = [node for node in partition_nodes if in_degree[node] == 0]
            position = 0
            
            while queue:
                # Sort queue for deterministic ordering
                queue.sort()
                node = queue.pop(0)
                predicted_ints[node] = position
                position += 1
                
                for neighbor in adj[node]:
                    if neighbor in in_degree:
                        in_degree[neighbor] -= 1
                        if in_degree[neighbor] == 0:
                            queue.append(neighbor)
            
            # Handle nodes not reached by topological sort
            for node in partition_nodes:
                if predicted_ints[node] == 0 and node not in [n for n in partition_nodes if in_degree[n] == 0]:
                    predicted_ints[node] = position
                    position += 1
        
        return predicted_ints
    
    def grid_search(
        self,
        approach: str = 'regressor',
        penalties: list = PENALTIES,
        verbose: bool = True,
    ) -> Dict:
        """
        Perform grid search over penalty values.
        
        Args:
            approach: Which approach to use ('regressor' or 'pairwise')
            penalties: List of weight_decay values to try
            verbose: Whether to print progress
            
        Returns:
            Dictionary with best parameters and all results
        """
        if verbose:
            print("=" * 70)
            print(f"Grid Search for Task 2: {approach}")
            print("=" * 70)
        
        results = []
        best_penalty = None
        best_f1 = 0
        
        for penalty in penalties:
            if verbose:
                print(f"\nTesting penalty={penalty}...")
            
            if approach == 'regressor':
                _, metrics = self.train_regressor(
                    weight_decay=penalty, verbose=False
                )
            elif approach == 'pairwise':
                _, metrics = self.train_pairwise(
                    weight_decay=penalty, verbose=False
                )
            else:
                raise ValueError(f"Unknown approach: {approach}")
            
            results.append({'penalty': penalty, 'metrics': metrics})
            
            if verbose:
                print(f"  Link F1: {metrics['link_f1']:.4f}")
                print(f"  Kendall's Tau: {metrics['kendalls_tau']:.4f}")
            
            if metrics['link_f1'] > best_f1:
                best_f1 = metrics['link_f1']
                best_penalty = penalty
        
        if verbose:
            print(f"\nBest penalty: {best_penalty} with F1: {best_f1:.4f}")
            print("=" * 70 + "\n")
        
        return {
            'best_penalty': best_penalty,
            'best_f1': best_f1,
            'all_results': results,
        }

