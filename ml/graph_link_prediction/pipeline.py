"""
Two-Stage Link Prediction Pipeline.

This module implements the complete pipeline that integrates:
- Stage 1: Task 1 (SAME_DENOMINATOR) - Partition by rational value
- Stage 2: Task 2 (NEXT_INTEGER) - Order by integer within partitions

The pipeline can use any combination of approaches for each task.
"""

from typing import Dict, Tuple, Optional

import torch
from torch import nn

from .config import DEVICE
from .task1_trainer import Task1Trainer
from .task2_trainer import Task2Trainer
from .evaluation import (
    evaluate_task1_full,
    evaluate_task2_full,
    validate_bijection_properties,
    generate_evaluation_report,
    print_evaluation_summary,
)


class TwoStageLinkPredictionPipeline:
    """
    Two-stage pipeline for link prediction.
    
    Stage 1: Predict SAME_DENOMINATOR edges (rational partitioning)
    Stage 2: Predict NEXT_INTEGER edges (sequential ordering within partitions)
    
    The pipeline validates that Stage 2 predictions respect Stage 1 partitions.
    
    Example:
        >>> pipeline = TwoStageLinkPredictionPipeline(data_t1, data_t2)
        >>> results = pipeline.run(
        ...     task1_approach='classifier',
        ...     task2_approach='regressor'
        ... )
        >>> print(f"Combined F1: {results['combined_f1']:.4f}")
    """
    
    def __init__(
        self,
        data_task1,
        data_task2,
        device: str = DEVICE,
    ):
        """
        Initialize the pipeline.
        
        Args:
            data_task1: PyG Data for Task 1 (SAME_DENOMINATOR)
            data_task2: PyG Data for Task 2 (NEXT_INTEGER)
            device: Device to use ('cuda' or 'cpu')
        """
        self.data_task1 = data_task1.to(device)
        self.data_task2 = data_task2.to(device)
        self.device = device
        
        # Trainers for each task
        self.task1_trainer = Task1Trainer(data_task1, device=device)
        self.task2_trainer = Task2Trainer(data_task2, device=device)
        
        # Models (will be set after training)
        self.task1_model = None
        self.task2_model = None
        
        # Results
        self.task1_metrics = None
        self.task2_metrics = None
        self.bijection_validation = None
    
    def run(
        self,
        task1_approach: str = 'classifier',
        task2_approach: str = 'regressor',
        task1_penalty: float = 0.0625,
        task2_penalty: float = 0.0625,
        verbose: bool = True,
    ) -> Dict:
        """
        Run the complete two-stage pipeline.
        
        Args:
            task1_approach: Task 1 model ('classifier', 'pairwise', 'contrastive')
            task2_approach: Task 2 model ('regressor', 'pairwise')
            task1_penalty: Weight decay for Task 1
            task2_penalty: Weight decay for Task 2
            verbose: Whether to print progress
            
        Returns:
            Dictionary with comprehensive results
        """
        if verbose:
            print("\n" + "=" * 70)
            print("TWO-STAGE LINK PREDICTION PIPELINE")
            print("=" * 70)
            print(f"Stage 1 Approach: {task1_approach}")
            print(f"Stage 2 Approach: {task2_approach}")
            print("=" * 70 + "\n")
        
        # Stage 1: Train Task 1 model
        if verbose:
            print("STAGE 1: Training SAME_DENOMINATOR predictor...")
        
        if task1_approach == 'classifier':
            self.task1_model, self.task1_metrics = self.task1_trainer.train_classifier(
                weight_decay=task1_penalty, verbose=verbose
            )
        elif task1_approach == 'pairwise':
            self.task1_model, self.task1_metrics = self.task1_trainer.train_pairwise(
                weight_decay=task1_penalty, verbose=verbose
            )
        elif task1_approach == 'contrastive':
            self.task1_model, self.task1_metrics = self.task1_trainer.train_contrastive(
                weight_decay=task1_penalty, verbose=verbose
            )
        else:
            raise ValueError(f"Unknown Task 1 approach: {task1_approach}")
        
        # Stage 2: Train Task 2 model
        if verbose:
            print("STAGE 2: Training NEXT_INTEGER predictor...")
        
        if task2_approach == 'regressor':
            self.task2_model, self.task2_metrics = self.task2_trainer.train_regressor(
                weight_decay=task2_penalty, verbose=verbose
            )
        elif task2_approach == 'pairwise':
            self.task2_model, self.task2_metrics = self.task2_trainer.train_pairwise(
                weight_decay=task2_penalty, verbose=verbose
            )
        else:
            raise ValueError(f"Unknown Task 2 approach: {task2_approach}")
        
        # Get predictions for validation
        same_denom_edges, next_int_edges = self.predict()
        
        # Validate bijection properties
        if verbose:
            print("VALIDATION: Checking bijection properties...")
        
        self.bijection_validation = validate_bijection_properties(
            same_denom_edges,
            next_int_edges,
            self.data_task1.partition_ids,
            self.data_task2.int_values,
        )
        
        # Compute combined metrics
        combined_results = {
            'task1_approach': task1_approach,
            'task2_approach': task2_approach,
            'task1_metrics': self.task1_metrics,
            'task2_metrics': self.task2_metrics,
            'bijection_validation': self.bijection_validation,
            'combined_f1': (
                self.task1_metrics['link_f1'] + self.task2_metrics['link_f1']
            ) / 2,
        }
        
        # Print summary
        if verbose:
            print_evaluation_summary(
                self.task1_metrics,
                self.task2_metrics,
                self.bijection_validation,
            )
        
        return combined_results
    
    def predict(self) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Generate predictions from both trained models.
        
        Returns:
            Tuple of (same_denom_edges, next_int_edges)
        """
        if self.task1_model is None or self.task2_model is None:
            raise RuntimeError("Models must be trained before prediction. Call run() first.")
        
        # Task 1 predictions
        self.task1_model.eval()
        with torch.no_grad():
            if hasattr(self.task1_model, 'predict_edges'):
                result = self.task1_model.predict_edges(
                    self.data_task1.x, self.data_task1.edge_index
                )
                # Handle different return types (some models return tuple, some just edges)
                if isinstance(result, tuple):
                    same_denom_edges = result[0]  # Extract edges from (edges, scores) tuple
                else:
                    same_denom_edges = result
            else:
                # For contrastive model
                embeddings = self.task1_model(
                    self.data_task1.x, self.data_task1.edge_index
                )
                same_denom_edges = self.task1_model.predict_edges_from_embeddings(
                    embeddings, similarity_threshold=0.8
                )
        
        # Task 2 predictions
        self.task2_model.eval()
        with torch.no_grad():
            result = self.task2_model.predict_edges(
                self.data_task2.x,
                self.data_task2.edge_index,
                self.data_task2.partition_ids,
            )
            # Handle different return types (some models return tuple, some just edges)
            if isinstance(result, tuple):
                next_int_edges = result[0]  # Extract edges from (edges, scores) tuple
            else:
                next_int_edges = result
        
        return same_denom_edges, next_int_edges
    
    def generate_report(
        self,
        task1_targets: Optional[Dict] = None,
        task2_targets: Optional[Dict] = None,
    ) -> str:
        """
        Generate comprehensive evaluation report.
        
        Args:
            task1_targets: Target metrics for Task 1
            task2_targets: Target metrics for Task 2
            
        Returns:
            Formatted report string
        """
        if self.task1_metrics is None or self.task2_metrics is None:
            raise RuntimeError("Pipeline must be run before generating report")
        
        return generate_evaluation_report(
            self.task1_metrics,
            self.task2_metrics,
            self.bijection_validation,
            task1_targets,
            task2_targets,
        )
    
    def compare_approaches(
        self,
        task1_approaches: list = ['classifier', 'pairwise', 'contrastive'],
        task2_approaches: list = ['regressor', 'pairwise'],
        verbose: bool = True,
    ) -> Dict:
        """
        Compare all combinations of approaches.
        
        Args:
            task1_approaches: List of Task 1 approaches to try
            task2_approaches: List of Task 2 approaches to try
            verbose: Whether to print progress
            
        Returns:
            Dictionary with results for all combinations
        """
        results = {}
        
        if verbose:
            print("\n" + "=" * 70)
            print("COMPARING ALL APPROACH COMBINATIONS")
            print("=" * 70 + "\n")
        
        for t1_approach in task1_approaches:
            for t2_approach in task2_approaches:
                key = f"{t1_approach}+{t2_approach}"
                
                if verbose:
                    print(f"\nTesting: {key}...")
                
                result = self.run(
                    task1_approach=t1_approach,
                    task2_approach=t2_approach,
                    verbose=False,
                )
                
                results[key] = result
                
                if verbose:
                    print(f"  Task 1 F1: {result['task1_metrics']['link_f1']:.4f}")
                    print(f"  Task 2 F1: {result['task2_metrics']['link_f1']:.4f}")
                    print(f"  Combined F1: {result['combined_f1']:.4f}")
                    print(f"  Bijection Valid: {result['bijection_validation']['all_valid']}")
        
        # Find best combination
        best_key = max(results.keys(), key=lambda k: results[k]['combined_f1'])
        best_result = results[best_key]
        
        if verbose:
            print("\n" + "=" * 70)
            print(f"BEST COMBINATION: {best_key}")
            print(f"  Combined F1: {best_result['combined_f1']:.4f}")
            print(f"  Bijection Valid: {best_result['bijection_validation']['all_valid']}")
            print("=" * 70 + "\n")
        
        return {
            'all_results': results,
            'best_combination': best_key,
            'best_result': best_result,
        }


def run_complete_pipeline(
    data_task1,
    data_task2,
    task1_approach: str = 'classifier',
    task2_approach: str = 'regressor',
    device: str = DEVICE,
    verbose: bool = True,
) -> Dict:
    """
    Convenience function to run the complete pipeline.
    
    Args:
        data_task1: PyG Data for Task 1
        data_task2: PyG Data for Task 2
        task1_approach: Task 1 model type
        task2_approach: Task 2 model type
        device: Device to use
        verbose: Whether to print progress
        
    Returns:
        Dictionary with complete results
        
    Example:
        >>> from ml.graph_link_prediction import load_graph_from_neo4j
        >>> from ml.graph_link_prediction.pipeline import run_complete_pipeline
        >>> data_t1, data_t2, _ = load_graph_from_neo4j(uri, user, pw, db)
        >>> results = run_complete_pipeline(data_t1, data_t2)
    """
    pipeline = TwoStageLinkPredictionPipeline(data_task1, data_task2, device=device)
    results = pipeline.run(
        task1_approach=task1_approach,
        task2_approach=task2_approach,
        verbose=verbose,
    )
    
    return results

