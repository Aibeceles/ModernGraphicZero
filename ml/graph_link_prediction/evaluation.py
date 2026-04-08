"""
Evaluation metrics for Link Prediction tasks.

This module provides:
- Task 1 metrics: Partition quality, link prediction metrics
- Task 2 metrics: Sequence ordering quality, link prediction metrics
- Bijection validation: Uniqueness, completeness, ordering checks
"""

from typing import Dict, List, Tuple, Optional
from collections import defaultdict

import numpy as np
import torch
from sklearn.metrics import adjusted_rand_score
from scipy.stats import kendalltau


# =============================================================================
# Task 1: SAME_DENOMINATOR Evaluation
# =============================================================================

def evaluate_partition_quality(
    predicted_partition_ids: torch.Tensor,
    true_partition_ids: torch.Tensor,
) -> Dict[str, float]:
    """
    Evaluate partition quality using clustering metrics.
    
    Args:
        predicted_partition_ids: Predicted partition labels [num_nodes]
        true_partition_ids: Ground truth partition labels [num_nodes]
        
    Returns:
        Dictionary with metrics:
        - partition_purity: Fraction of predicted clusters that are pure
        - partition_completeness: Fraction of true clusters fully recovered
        - adjusted_rand_index: Agreement with true partitioning
    """
    pred_np = predicted_partition_ids.cpu().numpy()
    true_np = true_partition_ids.cpu().numpy()
    
    # Adjusted Rand Index
    ari = adjusted_rand_score(true_np, pred_np)
    
    # Partition purity: For each predicted cluster, what fraction belongs to majority true cluster?
    purity_scores = []
    for pred_id in np.unique(pred_np):
        pred_mask = (pred_np == pred_id)
        true_labels_in_pred = true_np[pred_mask]
        
        if len(true_labels_in_pred) == 0:
            continue
        
        # Most common true label in this predicted cluster
        unique, counts = np.unique(true_labels_in_pred, return_counts=True)
        max_count = counts.max()
        purity = max_count / len(true_labels_in_pred)
        purity_scores.append(purity)
    
    partition_purity = np.mean(purity_scores) if purity_scores else 0.0
    
    # Partition completeness: For each true cluster, what fraction is in the predicted cluster?
    completeness_scores = []
    for true_id in np.unique(true_np):
        true_mask = (true_np == true_id)
        pred_labels_in_true = pred_np[true_mask]
        
        if len(pred_labels_in_true) == 0:
            continue
        
        # Most common predicted label for this true cluster
        unique, counts = np.unique(pred_labels_in_true, return_counts=True)
        max_count = counts.max()
        completeness = max_count / len(pred_labels_in_true)
        completeness_scores.append(completeness)
    
    partition_completeness = np.mean(completeness_scores) if completeness_scores else 0.0
    
    return {
        'partition_purity': partition_purity,
        'partition_completeness': partition_completeness,
        'adjusted_rand_index': ari,
    }


def evaluate_link_prediction(
    predicted_edges: torch.Tensor,
    true_edges: torch.Tensor,
) -> Dict[str, float]:
    """
    Evaluate link prediction quality.
    
    Args:
        predicted_edges: Predicted edge tensor [2, num_pred_edges]
        true_edges: Ground truth edge tensor [2, num_true_edges]
        
    Returns:
        Dictionary with metrics:
        - link_precision: Correct edges / predicted edges
        - link_recall: Correct edges / true edges
        - link_f1: Harmonic mean of precision and recall
    """
    # Convert edges to sets for fast comparison
    pred_set = set()
    for i in range(predicted_edges.size(1)):
        src = predicted_edges[0, i].item()
        dst = predicted_edges[1, i].item()
        # Normalize to undirected: always store (min, max)
        pred_set.add((min(src, dst), max(src, dst)))
    
    true_set = set()
    for i in range(true_edges.size(1)):
        src = true_edges[0, i].item()
        dst = true_edges[1, i].item()
        true_set.add((min(src, dst), max(src, dst)))
    
    # Compute intersection
    correct = pred_set & true_set
    
    # Metrics
    precision = len(correct) / len(pred_set) if len(pred_set) > 0 else 0.0
    recall = len(correct) / len(true_set) if len(true_set) > 0 else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    
    return {
        'link_precision': precision,
        'link_recall': recall,
        'link_f1': f1,
        'num_predicted': len(pred_set),
        'num_true': len(true_set),
        'num_correct': len(correct),
    }


def evaluate_task1_full(
    predicted_partition_ids: torch.Tensor,
    true_partition_ids: torch.Tensor,
    predicted_edges: torch.Tensor,
    true_edges: torch.Tensor,
) -> Dict[str, float]:
    """
    Full evaluation for Task 1 (SAME_DENOMINATOR).
    
    Args:
        predicted_partition_ids: Predicted partition labels
        true_partition_ids: Ground truth partition labels
        predicted_edges: Predicted edges
        true_edges: Ground truth edges
        
    Returns:
        Dictionary with all Task 1 metrics
    """
    partition_metrics = evaluate_partition_quality(
        predicted_partition_ids, true_partition_ids
    )
    
    link_metrics = evaluate_link_prediction(predicted_edges, true_edges)
    
    # Combine metrics
    metrics = {**partition_metrics, **link_metrics}
    
    return metrics


# =============================================================================
# Task 2: NEXT_INTEGER Evaluation
# =============================================================================

def evaluate_sequence_quality(
    predicted_int_values: torch.Tensor,
    true_int_values: torch.Tensor,
    partition_ids: torch.Tensor,
) -> Dict[str, float]:
    """
    Evaluate sequence ordering quality within partitions.
    
    Args:
        predicted_int_values: Predicted integer values [num_nodes]
        true_int_values: True integer values [num_nodes]
        partition_ids: Partition ID for each node [num_nodes]
        
    Returns:
        Dictionary with metrics:
        - sequence_accuracy: Fraction of partitions with perfect ordering
        - kendalls_tau: Average rank correlation within partitions
        - position_mae: Mean absolute error in predicted positions
    """
    pred_np = predicted_int_values.cpu().numpy()
    true_np = true_int_values.cpu().numpy()
    part_np = partition_ids.cpu().numpy()
    
    # Group nodes by partition
    partitions = defaultdict(list)
    for idx, partition_id in enumerate(part_np):
        partitions[partition_id].append(idx)
    
    perfect_sequences = 0
    kendall_taus = []
    position_maes = []
    
    for partition_id, node_indices in partitions.items():
        if len(node_indices) < 2:
            continue
        
        # Get true and predicted values for this partition
        true_vals = true_np[node_indices]
        pred_vals = pred_np[node_indices]
        
        # Check if ordering is perfect
        true_order = np.argsort(true_vals)
        pred_order = np.argsort(pred_vals)
        
        if np.array_equal(true_order, pred_order):
            perfect_sequences += 1
        
        # Compute Kendall's Tau (rank correlation)
        try:
            tau, _ = kendalltau(true_vals, pred_vals)
            if not np.isnan(tau):
                kendall_taus.append(tau)
        except:
            pass
        
        # Compute position MAE
        true_positions = np.argsort(true_order)
        pred_positions = np.argsort(pred_order)
        mae = np.mean(np.abs(true_positions - pred_positions))
        position_maes.append(mae)
    
    num_partitions = len([p for p in partitions.values() if len(p) >= 2])
    sequence_accuracy = perfect_sequences / num_partitions if num_partitions > 0 else 0.0
    kendalls_tau = np.mean(kendall_taus) if kendall_taus else 0.0
    position_mae = np.mean(position_maes) if position_maes else 0.0
    
    return {
        'sequence_accuracy': sequence_accuracy,
        'kendalls_tau': kendalls_tau,
        'position_mae': position_mae,
    }


def evaluate_task2_full(
    predicted_int_values: torch.Tensor,
    true_int_values: torch.Tensor,
    partition_ids: torch.Tensor,
    predicted_edges: torch.Tensor,
    true_edges: torch.Tensor,
) -> Dict[str, float]:
    """
    Full evaluation for Task 2 (NEXT_INTEGER).
    
    Args:
        predicted_int_values: Predicted integer values
        true_int_values: True integer values
        partition_ids: Partition IDs
        predicted_edges: Predicted edges
        true_edges: Ground truth edges
        
    Returns:
        Dictionary with all Task 2 metrics
    """
    sequence_metrics = evaluate_sequence_quality(
        predicted_int_values, true_int_values, partition_ids
    )
    
    link_metrics = evaluate_link_prediction(predicted_edges, true_edges)
    
    # Combine metrics
    metrics = {**sequence_metrics, **link_metrics}
    
    return metrics


# =============================================================================
# Bijection Validation
# =============================================================================

def validate_uniqueness(
    next_int_edges: torch.Tensor,
    num_nodes: int,
) -> Tuple[bool, List[int]]:
    """
    Validate uniqueness: Each node has at most one outgoing NEXT_INTEGER edge.
    
    Args:
        next_int_edges: NEXT_INTEGER edge tensor [2, num_edges]
        num_nodes: Total number of nodes
        
    Returns:
        Tuple of (is_valid, violating_nodes)
    """
    out_degree = torch.zeros(num_nodes, dtype=torch.long)
    
    for i in range(next_int_edges.size(1)):
        src = next_int_edges[0, i].item()
        out_degree[src] += 1
    
    violations = (out_degree > 1).nonzero(as_tuple=True)[0].tolist()
    
    return len(violations) == 0, violations


def validate_partition_completeness(
    same_denom_edges: torch.Tensor,
    true_partition_ids: torch.Tensor,
) -> Tuple[bool, List[int]]:
    """
    Validate partition completeness: Nodes with same (n,d) are in same partition.
    
    This checks if the predicted SAME_DENOMINATOR edges form connected components
    that align with the true partition IDs.
    
    Args:
        same_denom_edges: SAME_DENOMINATOR edge tensor [2, num_edges]
        true_partition_ids: Ground truth partition IDs [num_nodes]
        
    Returns:
        Tuple of (is_valid, violating_components)
    """
    if same_denom_edges.size(1) == 0:
        return True, []
    
    # Build connected components from predicted edges
    num_nodes = true_partition_ids.size(0)
    parent = list(range(num_nodes))
    
    def find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])
        return parent[x]
    
    def union(x, y):
        px, py = find(x), find(y)
        if px != py:
            parent[px] = py
    
    # Union nodes connected by SAME_DENOMINATOR edges
    for i in range(same_denom_edges.size(1)):
        src = same_denom_edges[0, i].item()
        dst = same_denom_edges[1, i].item()
        union(src, dst)
    
    # Group nodes by their component
    components = defaultdict(list)
    for i in range(num_nodes):
        components[find(i)].append(i)
    
    # Check if each component has uniform true partition ID
    violations = []
    for component_id, node_list in components.items():
        true_ids = true_partition_ids[node_list]
        unique_ids = torch.unique(true_ids)
        
        if len(unique_ids) > 1:
            violations.append(component_id)
    
    return len(violations) == 0, violations


def validate_sequential_ordering(
    next_int_edges: torch.Tensor,
    true_int_values: torch.Tensor,
) -> Tuple[bool, List[Tuple[int, int]]]:
    """
    Validate sequential ordering: NEXT_INTEGER edges connect consecutive integers.
    
    Args:
        next_int_edges: NEXT_INTEGER edge tensor [2, num_edges]
        true_int_values: True integer values [num_nodes]
        
    Returns:
        Tuple of (is_valid, violating_edges)
    """
    violations = []
    
    for i in range(next_int_edges.size(1)):
        src = next_int_edges[0, i].item()
        dst = next_int_edges[1, i].item()
        
        src_int = true_int_values[src].item()
        dst_int = true_int_values[dst].item()
        
        # Check if consecutive
        if dst_int != src_int + 1:
            violations.append((src, dst))
    
    return len(violations) == 0, violations


def validate_bijection_properties(
    same_denom_edges: torch.Tensor,
    next_int_edges: torch.Tensor,
    true_partition_ids: torch.Tensor,
    true_int_values: torch.Tensor,
) -> Dict[str, bool]:
    """
    Validate all bijection properties.
    
    Args:
        same_denom_edges: SAME_DENOMINATOR edges
        next_int_edges: NEXT_INTEGER edges
        true_partition_ids: Ground truth partition IDs
        true_int_values: True integer values
        
    Returns:
        Dictionary with validation results for each property
    """
    num_nodes = true_partition_ids.size(0)
    
    uniqueness_valid, uniqueness_violations = validate_uniqueness(
        next_int_edges, num_nodes
    )
    
    completeness_valid, completeness_violations = validate_partition_completeness(
        same_denom_edges, true_partition_ids
    )
    
    ordering_valid, ordering_violations = validate_sequential_ordering(
        next_int_edges, true_int_values
    )
    
    return {
        'uniqueness': uniqueness_valid,
        'partition_completeness': completeness_valid,
        'sequential_ordering': ordering_valid,
        'all_valid': uniqueness_valid and completeness_valid and ordering_valid,
        'uniqueness_violations': len(uniqueness_violations),
        'completeness_violations': len(completeness_violations),
        'ordering_violations': len(ordering_violations),
    }


# =============================================================================
# Comprehensive Evaluation Report
# =============================================================================

def generate_evaluation_report(
    task1_metrics: Dict[str, float],
    task2_metrics: Dict[str, float],
    bijection_validation: Dict[str, bool],
    task1_targets: Optional[Dict[str, float]] = None,
    task2_targets: Optional[Dict[str, float]] = None,
) -> str:
    """
    Generate a comprehensive evaluation report.
    
    Args:
        task1_metrics: Task 1 evaluation metrics
        task2_metrics: Task 2 evaluation metrics
        bijection_validation: Bijection validation results
        task1_targets: Target metrics for Task 1 (optional)
        task2_targets: Target metrics for Task 2 (optional)
        
    Returns:
        Formatted report string
    """
    lines = []
    lines.append("=" * 70)
    lines.append("Link Prediction Evaluation Report")
    lines.append("=" * 70)
    
    # Task 1 results
    lines.append("\nTask 1: SAME_DENOMINATOR (Rational Partitioning)")
    lines.append("-" * 70)
    
    for metric, value in task1_metrics.items():
        if metric.startswith('num_'):
            lines.append(f"  {metric}: {int(value)}")
        else:
            target_str = ""
            if task1_targets and metric in task1_targets:
                target = task1_targets[metric]
                status = "✓" if value >= target else "✗"
                target_str = f" (target: {target:.3f}) {status}"
            lines.append(f"  {metric}: {value:.4f}{target_str}")
    
    # Task 2 results
    lines.append("\nTask 2: NEXT_INTEGER (Sequential Ordering)")
    lines.append("-" * 70)
    
    for metric, value in task2_metrics.items():
        if metric.startswith('num_'):
            lines.append(f"  {metric}: {int(value)}")
        else:
            target_str = ""
            if task2_targets and metric in task2_targets:
                target = task2_targets[metric]
                # position_mae should be below target (smaller is better)
                if metric == 'position_mae':
                    status = "✓" if value <= target else "✗"
                else:
                    status = "✓" if value >= target else "✗"
                target_str = f" (target: {target:.3f}) {status}"
            lines.append(f"  {metric}: {value:.4f}{target_str}")
    
    # Bijection validation
    lines.append("\nBijection Property Validation")
    lines.append("-" * 70)
    
    for prop, valid in bijection_validation.items():
        if prop.endswith('_violations'):
            lines.append(f"  {prop}: {valid}")
        elif prop != 'all_valid':
            status = "✓ PASS" if valid else "✗ FAIL"
            lines.append(f"  {prop}: {status}")
    
    lines.append("\n" + "-" * 70)
    all_valid = bijection_validation.get('all_valid', False)
    overall_status = "✓ ALL VALID" if all_valid else "✗ VALIDATION FAILED"
    lines.append(f"Overall Bijection Validation: {overall_status}")
    
    lines.append("=" * 70)
    
    return "\n".join(lines)


def print_evaluation_summary(
    task1_metrics: Dict[str, float],
    task2_metrics: Dict[str, float],
    bijection_validation: Dict[str, bool],
):
    """
    Print a concise evaluation summary.
    
    Args:
        task1_metrics: Task 1 metrics
        task2_metrics: Task 2 metrics
        bijection_validation: Bijection validation results
    """
    print("\n" + "=" * 70)
    print("EVALUATION SUMMARY")
    print("=" * 70)
    
    print(f"\nTask 1 (SAME_DENOMINATOR):")
    print(f"  Partition Purity: {task1_metrics.get('partition_purity', 0):.4f}")
    print(f"  Adjusted Rand Index: {task1_metrics.get('adjusted_rand_index', 0):.4f}")
    print(f"  Link F1: {task1_metrics.get('link_f1', 0):.4f}")
    
    print(f"\nTask 2 (NEXT_INTEGER):")
    print(f"  Sequence Accuracy: {task2_metrics.get('sequence_accuracy', 0):.4f}")
    print(f"  Kendall's Tau: {task2_metrics.get('kendalls_tau', 0):.4f}")
    print(f"  Link F1: {task2_metrics.get('link_f1', 0):.4f}")
    
    print(f"\nBijection Validation:")
    all_valid = bijection_validation.get('all_valid', False)
    status = "✓ PASS" if all_valid else "✗ FAIL"
    print(f"  {status}")
    
    print("=" * 70 + "\n")

