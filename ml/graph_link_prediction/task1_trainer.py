"""
Training infrastructure for Task 1: SAME_DENOMINATOR Link Prediction.

This module provides training functionality for all three approaches:
1. Classifier: Train partition classifier
2. Pairwise: Train binary link predictor
3. Contrastive: Train contrastive embeddings

Includes training loops, evaluation, and hyperparameter tuning.
"""

from typing import Dict, Tuple, Optional
from collections import defaultdict

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
from .task1_models import (
    RationalPartitionClassifier,
    SameDenominatorLinkPredictor,
    ContrastiveEmbedder,
    ContrastiveLoss,
    get_task1_model,
)
from .data_loader import generate_negative_samples, split_edges_train_test
from .evaluation import evaluate_task1_full


class Task1Trainer:
    """
    Trainer for Task 1 (SAME_DENOMINATOR) link prediction.
    
    Supports three training approaches:
    - Classifier: Multi-class classification on partition labels
    - Pairwise: Binary classification on edge existence
    - Contrastive: Learn embeddings via contrastive loss
    
    Example:
        >>> trainer = Task1Trainer(data_task1, device='cuda')
        >>> model, metrics = trainer.train_classifier()
        >>> print(f"Partition Purity: {metrics['partition_purity']:.4f}")
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
            data: PyG Data object with Task 1 features and edges
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
        
        print(f"Task 1 Trainer initialized:")
        print(f"  Nodes: {self.data.num_nodes}")
        print(f"  Partitions: {self.num_partitions}")
        print(f"  Ground truth edges: {self.data.same_denom_edges.size(1)}")
        print(f"  Device: {device}\n")
    
    def train_classifier(
        self,
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        verbose: bool = True,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train partition classifier approach.
        
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
            print("Training Task 1: Partition Classifier")
            print("=" * 70)
        
        # Create model
        model = RationalPartitionClassifier(
            num_features=self.data.num_features,
            num_classes=self.num_partitions,
        ).to(self.device)
        
        optimizer = Adam(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
        
        # Create a mapping from partition_id to class label
        unique_partitions = torch.unique(self.data.partition_ids)
        partition_to_class = {p.item(): i for i, p in enumerate(unique_partitions)}
        class_labels = torch.tensor(
            [partition_to_class[p.item()] for p in self.data.partition_ids],
            dtype=torch.long,
            device=self.device
        )
        
        best_loss = float('inf')
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(max_epochs):
            # Training
            model.train()
            optimizer.zero_grad()
            
            out = model(self.data.x, self.data.edge_index)
            loss = F.nll_loss(out, class_labels)
            
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
                accuracy = (out.argmax(dim=1) == class_labels).float().mean()
                print(f"Epoch {epoch:03d} | Loss: {loss:.4f} | Acc: {accuracy:.4f}")
        
        # Load best model
        if best_model_state is not None:
            model.load_state_dict({k: v.to(self.device) for k, v in best_model_state.items()})
        
        # Evaluation
        model.eval()
        with torch.no_grad():
            out = model(self.data.x, self.data.edge_index)
            predicted_classes = out.argmax(dim=1)
            
            # Map class labels back to partition IDs
            # partition_to_class maps partition_id (int) -> class_idx (int)
            # We need the reverse: class_idx -> partition_id
            class_to_partition = {class_idx: partition_id for partition_id, class_idx in partition_to_class.items()}
            predicted_partition_ids = torch.tensor(
                [class_to_partition[c.item()] for c in predicted_classes],
                device=self.device
            )
            
            # Generate edges from predicted partitions
            predicted_edges = model.predict_edges(self.data.x, self.data.edge_index)
            
            # Evaluate
            metrics = evaluate_task1_full(
                predicted_partition_ids,
                self.data.partition_ids,
                predicted_edges,
                self.data.same_denom_edges,
            )
        
        if verbose:
            print(f"\nResults:")
            print(f"  Partition Purity: {metrics['partition_purity']:.4f}")
            print(f"  Adjusted Rand Index: {metrics['adjusted_rand_index']:.4f}")
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
        Train pairwise link predictor approach.
        
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
            print("Training Task 1: Pairwise Link Predictor")
            print("=" * 70)
        
        # Split edges into train/test
        train_edges, test_edges = split_edges_train_test(
            self.data.same_denom_edges, test_fraction=0.2, random_seed=self.random_seed
        )
        
        # Generate negative samples for training
        num_neg = int(train_edges.size(1) * negative_sample_ratio)
        neg_train_edges = generate_negative_samples(
            self.data.num_nodes, train_edges, num_neg, self.random_seed
        )
        
        if verbose:
            print(f"  Positive train edges: {train_edges.size(1)}")
            print(f"  Negative train edges: {neg_train_edges.size(1)}")
            print(f"  Test edges: {test_edges.size(1)}\n")
        
        # Create model
        model = SameDenominatorLinkPredictor(
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
            
            # Positive samples
            pos_pred = model(self.data.x, self.data.edge_index, train_edges)
            pos_loss = F.binary_cross_entropy(pos_pred, torch.ones_like(pos_pred))
            
            # Negative samples
            neg_pred = model(self.data.x, self.data.edge_index, neg_train_edges)
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
            # For evaluation, we need to derive partition IDs from predicted edges
            # This is computationally expensive for large graphs, so we use a threshold
            predicted_edges, _ = model.predict_edges(
                self.data.x, self.data.edge_index, threshold=0.5
            )
            
            # Derive partition IDs from connected components
            predicted_partition_ids = self._edges_to_partition_ids(predicted_edges)
            
            # Evaluate
            metrics = evaluate_task1_full(
                predicted_partition_ids,
                self.data.partition_ids,
                predicted_edges,
                self.data.same_denom_edges,
            )
        
        if verbose:
            print(f"\nResults:")
            print(f"  Partition Purity: {metrics['partition_purity']:.4f}")
            print(f"  Adjusted Rand Index: {metrics['adjusted_rand_index']:.4f}")
            print(f"  Link F1: {metrics['link_f1']:.4f}")
            print("=" * 70 + "\n")
        
        return model, metrics
    
    def train_contrastive(
        self,
        weight_decay: float = 0.0625,
        learning_rate: float = LEARNING_RATE,
        max_epochs: int = MAX_EPOCHS,
        patience: int = PATIENCE,
        verbose: bool = True,
    ) -> Tuple[nn.Module, Dict]:
        """
        Train contrastive learning approach.
        
        Args:
            weight_decay: L2 regularization strength
            learning_rate: Learning rate
            max_epochs: Maximum training epochs
            patience: Early stopping patience
            verbose: Whether to print progress
            
        Returns:
            Tuple of (trained model, metrics dictionary)
        """
        if verbose:
            print("=" * 70)
            print("Training Task 1: Contrastive Learning")
            print("=" * 70)
        
        # Create model and loss
        model = ContrastiveEmbedder(
            num_features=self.data.num_features,
        ).to(self.device)
        
        criterion = ContrastiveLoss().to(self.device)
        optimizer = Adam(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
        
        best_loss = float('inf')
        patience_counter = 0
        best_model_state = None
        
        for epoch in range(max_epochs):
            # Training
            model.train()
            optimizer.zero_grad()
            
            embeddings = model(self.data.x, self.data.edge_index)
            loss = criterion(embeddings, self.data.partition_ids)
            
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
            embeddings = model(self.data.x, self.data.edge_index)
            
            # Predict edges from embeddings using similarity threshold
            predicted_edges = model.predict_edges_from_embeddings(
                embeddings, similarity_threshold=0.8
            )
            
            # Derive partition IDs from connected components
            predicted_partition_ids = self._edges_to_partition_ids(predicted_edges)
            
            # Evaluate
            metrics = evaluate_task1_full(
                predicted_partition_ids,
                self.data.partition_ids,
                predicted_edges,
                self.data.same_denom_edges,
            )
        
        if verbose:
            print(f"\nResults:")
            print(f"  Partition Purity: {metrics['partition_purity']:.4f}")
            print(f"  Adjusted Rand Index: {metrics['adjusted_rand_index']:.4f}")
            print(f"  Link F1: {metrics['link_f1']:.4f}")
            print("=" * 70 + "\n")
        
        return model, metrics
    
    def _edges_to_partition_ids(self, edges: torch.Tensor) -> torch.Tensor:
        """
        Derive partition IDs from predicted edges using connected components.
        
        Args:
            edges: Edge tensor [2, num_edges]
            
        Returns:
            Partition IDs [num_nodes]
        """
        num_nodes = self.data.num_nodes
        parent = list(range(num_nodes))
        
        def find(x):
            if parent[x] != x:
                parent[x] = find(parent[x])
            return parent[x]
        
        def union(x, y):
            px, py = find(x), find(y)
            if px != py:
                parent[px] = py
        
        # Union nodes connected by edges
        for i in range(edges.size(1)):
            src = edges[0, i].item()
            dst = edges[1, i].item()
            union(src, dst)
        
        # Assign partition IDs based on component root
        partition_ids = torch.tensor([find(i) for i in range(num_nodes)], device=self.device)
        
        return partition_ids
    
    def grid_search(
        self,
        approach: str = 'classifier',
        penalties: list = PENALTIES,
        verbose: bool = True,
    ) -> Dict:
        """
        Perform grid search over penalty values.
        
        Args:
            approach: Which approach to use ('classifier', 'pairwise', 'contrastive')
            penalties: List of weight_decay values to try
            verbose: Whether to print progress
            
        Returns:
            Dictionary with best parameters and all results
        """
        if verbose:
            print("=" * 70)
            print(f"Grid Search for Task 1: {approach}")
            print("=" * 70)
        
        results = []
        best_penalty = None
        best_f1 = 0
        
        for penalty in penalties:
            if verbose:
                print(f"\nTesting penalty={penalty}...")
            
            if approach == 'classifier':
                _, metrics = self.train_classifier(
                    weight_decay=penalty, verbose=False
                )
            elif approach == 'pairwise':
                _, metrics = self.train_pairwise(
                    weight_decay=penalty, verbose=False
                )
            elif approach == 'contrastive':
                _, metrics = self.train_contrastive(
                    weight_decay=penalty, verbose=False
                )
            else:
                raise ValueError(f"Unknown approach: {approach}")
            
            results.append({'penalty': penalty, 'metrics': metrics})
            
            if verbose:
                print(f"  Link F1: {metrics['link_f1']:.4f}")
                print(f"  ARI: {metrics['adjusted_rand_index']:.4f}")
            
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

