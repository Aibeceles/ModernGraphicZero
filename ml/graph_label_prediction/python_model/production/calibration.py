"""
Probability Calibration Utilities for Production Prediction.

This module provides tools for:
1. Expected Calibration Error (ECE) computation
2. Reliability diagrams
3. Selective prediction analysis (accuracy vs coverage curves)
4. Threshold optimization for routing

Calibrated probabilities are essential for:
- Meaningful uncertainty thresholds
- Reliable routing decisions
- Trustworthy confidence scores

Usage:
    from calibration import compute_ece, selective_accuracy_curve
    
    # Check calibration
    ece = compute_ece(probs, labels, n_bins=15)
    print(f"ECE: {ece:.4f}")
    
    # Analyze selective prediction
    coverages, accuracies = selective_accuracy_curve(probs, preds, labels)
"""

from dataclasses import dataclass
from typing import Tuple, List, Optional, Dict

import numpy as np
import torch


@dataclass
class CalibrationMetrics:
    """Complete calibration metrics."""
    ece: float  # Expected Calibration Error
    mce: float  # Maximum Calibration Error
    ace: float  # Average Calibration Error (class-conditional)
    brier: float  # Brier score
    reliability_diagram: Dict  # Data for reliability diagram


@dataclass
class SelectivePredictionMetrics:
    """Metrics for selective prediction analysis."""
    coverages: np.ndarray  # Coverage levels [0, 1]
    accuracies: np.ndarray  # Accuracy at each coverage
    f1_scores: np.ndarray  # Macro F1 at each coverage
    auc: float  # Area under accuracy-coverage curve
    optimal_threshold: float  # Threshold for target accuracy


def compute_ece(
    probs: torch.Tensor,
    labels: torch.Tensor,
    n_bins: int = 15,
) -> float:
    """
    Compute Expected Calibration Error (ECE).
    
    ECE measures how well predicted probabilities match empirical accuracy.
    Perfect calibration: ECE = 0.
    
    ECE = sum_{b} (n_b / N) * |accuracy(b) - confidence(b)|
    
    Args:
        probs: Predicted probabilities [N, K] or [N] (max prob)
        labels: Ground truth labels [N]
        n_bins: Number of confidence bins
        
    Returns:
        ECE value in [0, 1]
    """
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    # Get max probability and predictions
    if probs.dim() == 2:
        confidences, predictions = probs.max(dim=1)
    else:
        confidences = probs
        predictions = labels  # Assume we're checking confidence calibration
    
    # Convert to numpy
    confidences = confidences.cpu().numpy()
    predictions = predictions.cpu().numpy()
    labels = labels.cpu().numpy()
    
    # Create bins
    bin_boundaries = np.linspace(0, 1, n_bins + 1)
    bin_lowers = bin_boundaries[:-1]
    bin_uppers = bin_boundaries[1:]
    
    ece = 0.0
    for bin_lower, bin_upper in zip(bin_lowers, bin_uppers):
        # Find samples in this bin
        in_bin = (confidences > bin_lower) & (confidences <= bin_upper)
        prop_in_bin = in_bin.mean()
        
        if prop_in_bin > 0:
            # Accuracy in bin
            accuracy_in_bin = (predictions[in_bin] == labels[in_bin]).mean()
            # Average confidence in bin
            avg_confidence_in_bin = confidences[in_bin].mean()
            # Contribution to ECE
            ece += prop_in_bin * np.abs(accuracy_in_bin - avg_confidence_in_bin)
    
    return float(ece)


def compute_mce(
    probs: torch.Tensor,
    labels: torch.Tensor,
    n_bins: int = 15,
) -> float:
    """
    Compute Maximum Calibration Error (MCE).
    
    MCE is the maximum gap between confidence and accuracy across bins.
    
    Args:
        probs: Predicted probabilities [N, K]
        labels: Ground truth labels [N]
        n_bins: Number of confidence bins
        
    Returns:
        MCE value in [0, 1]
    """
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    confidences, predictions = probs.max(dim=1)
    confidences = confidences.cpu().numpy()
    predictions = predictions.cpu().numpy()
    labels = labels.cpu().numpy()
    
    bin_boundaries = np.linspace(0, 1, n_bins + 1)
    mce = 0.0
    
    for i in range(n_bins):
        in_bin = (confidences > bin_boundaries[i]) & (confidences <= bin_boundaries[i + 1])
        if in_bin.sum() > 0:
            accuracy_in_bin = (predictions[in_bin] == labels[in_bin]).mean()
            avg_confidence = confidences[in_bin].mean()
            mce = max(mce, np.abs(accuracy_in_bin - avg_confidence))
    
    return float(mce)


def compute_brier_score(
    probs: torch.Tensor,
    labels: torch.Tensor,
) -> float:
    """
    Compute Brier score (mean squared error of probabilities).
    
    Lower is better. Perfect predictions: Brier = 0.
    
    Args:
        probs: Predicted probabilities [N, K]
        labels: Ground truth labels [N]
        
    Returns:
        Brier score
    """
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    num_classes = probs.shape[1]
    one_hot = torch.zeros_like(probs)
    one_hot.scatter_(1, labels.unsqueeze(1), 1)
    
    brier = ((probs - one_hot) ** 2).sum(dim=1).mean()
    return float(brier)


def compute_calibration_metrics(
    probs: torch.Tensor,
    labels: torch.Tensor,
    n_bins: int = 15,
) -> CalibrationMetrics:
    """
    Compute comprehensive calibration metrics.
    
    Args:
        probs: Predicted probabilities [N, K]
        labels: Ground truth labels [N]
        n_bins: Number of confidence bins
        
    Returns:
        CalibrationMetrics with ECE, MCE, ACE, Brier, and reliability diagram
    """
    ece = compute_ece(probs, labels, n_bins)
    mce = compute_mce(probs, labels, n_bins)
    brier = compute_brier_score(probs, labels)
    
    # Class-conditional ECE (Average Calibration Error)
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    num_classes = probs.shape[1]
    class_eces = []
    for c in range(num_classes):
        mask = labels == c
        if mask.sum() > 0:
            class_probs = probs[mask, c]
            class_labels = torch.ones(mask.sum(), dtype=torch.long)
            # Treat as binary: probability of class c
            binary_probs = torch.stack([1 - class_probs, class_probs], dim=1)
            class_ece = compute_ece(binary_probs, class_labels, n_bins)
            class_eces.append(class_ece)
    ace = np.mean(class_eces) if class_eces else 0.0
    
    # Reliability diagram data
    confidences, predictions = probs.max(dim=1)
    confidences = confidences.cpu().numpy()
    predictions = predictions.cpu().numpy()
    labels_np = labels.cpu().numpy()
    
    bin_boundaries = np.linspace(0, 1, n_bins + 1)
    bin_centers = (bin_boundaries[:-1] + bin_boundaries[1:]) / 2
    bin_accuracies = []
    bin_confidences = []
    bin_counts = []
    
    for i in range(n_bins):
        in_bin = (confidences > bin_boundaries[i]) & (confidences <= bin_boundaries[i + 1])
        if in_bin.sum() > 0:
            bin_accuracies.append((predictions[in_bin] == labels_np[in_bin]).mean())
            bin_confidences.append(confidences[in_bin].mean())
            bin_counts.append(in_bin.sum())
        else:
            bin_accuracies.append(0)
            bin_confidences.append(bin_centers[i])
            bin_counts.append(0)
    
    reliability_diagram = {
        'bin_centers': bin_centers,
        'bin_accuracies': np.array(bin_accuracies),
        'bin_confidences': np.array(bin_confidences),
        'bin_counts': np.array(bin_counts),
    }
    
    return CalibrationMetrics(
        ece=ece,
        mce=mce,
        ace=ace,
        brier=brier,
        reliability_diagram=reliability_diagram,
    )


def selective_accuracy_curve(
    probs: torch.Tensor,
    predictions: torch.Tensor,
    labels: torch.Tensor,
    n_points: int = 100,
) -> Tuple[np.ndarray, np.ndarray]:
    """
    Compute selective prediction accuracy-coverage curve.
    
    For each coverage level, compute accuracy when only predicting
    on the most confident samples.
    
    Args:
        probs: Predicted probabilities [N, K]
        predictions: Predicted classes [N]
        labels: Ground truth labels [N]
        n_points: Number of coverage levels to evaluate
        
    Returns:
        Tuple of (coverages, accuracies) arrays
    """
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    if isinstance(predictions, np.ndarray):
        predictions = torch.from_numpy(predictions)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    # Get max probabilities (confidence)
    if probs.dim() == 2:
        confidences = probs.max(dim=1).values
    else:
        confidences = probs
    
    # Sort by confidence (descending)
    sorted_indices = confidences.argsort(descending=True)
    sorted_predictions = predictions[sorted_indices].cpu().numpy()
    sorted_labels = labels[sorted_indices].cpu().numpy()
    
    # Compute accuracy at each coverage level
    n_samples = len(labels)
    coverages = np.linspace(0.01, 1.0, n_points)
    accuracies = []
    
    for coverage in coverages:
        n_selected = max(1, int(coverage * n_samples))
        selected_preds = sorted_predictions[:n_selected]
        selected_labels = sorted_labels[:n_selected]
        acc = (selected_preds == selected_labels).mean()
        accuracies.append(acc)
    
    return coverages, np.array(accuracies)


def compute_selective_metrics(
    probs: torch.Tensor,
    predictions: torch.Tensor,
    labels: torch.Tensor,
    target_accuracy: float = 0.95,
    n_points: int = 100,
) -> SelectivePredictionMetrics:
    """
    Compute comprehensive selective prediction metrics.
    
    Args:
        probs: Predicted probabilities [N, K]
        predictions: Predicted classes [N]
        labels: Ground truth labels [N]
        target_accuracy: Target accuracy for threshold optimization
        n_points: Number of coverage levels
        
    Returns:
        SelectivePredictionMetrics
    """
    coverages, accuracies = selective_accuracy_curve(
        probs, predictions, labels, n_points
    )
    
    # Compute F1 at each coverage (more expensive)
    if isinstance(probs, np.ndarray):
        probs = torch.from_numpy(probs)
    
    confidences = probs.max(dim=1).values if probs.dim() == 2 else probs
    sorted_indices = confidences.argsort(descending=True)
    
    from sklearn.metrics import f1_score
    
    sorted_predictions = predictions[sorted_indices].cpu().numpy()
    sorted_labels = labels[sorted_indices].cpu().numpy()
    n_samples = len(labels)
    
    f1_scores = []
    for coverage in coverages:
        n_selected = max(1, int(coverage * n_samples))
        selected_preds = sorted_predictions[:n_selected]
        selected_labels = sorted_labels[:n_selected]
        f1 = f1_score(selected_labels, selected_preds, average='macro', zero_division=0)
        f1_scores.append(f1)
    
    # AUC (area under accuracy-coverage curve)
    auc = np.trapz(accuracies, coverages)
    
    # Find optimal threshold for target accuracy
    # Threshold = confidence below which we defer
    for i, (cov, acc) in enumerate(zip(coverages, accuracies)):
        if acc >= target_accuracy:
            optimal_coverage = cov
            break
    else:
        optimal_coverage = coverages[0]  # Can't reach target
    
    # Convert coverage to confidence threshold
    confidences_np = confidences.cpu().numpy()
    n_keep = int(optimal_coverage * n_samples)
    sorted_conf = np.sort(confidences_np)[::-1]
    optimal_threshold = sorted_conf[min(n_keep, len(sorted_conf) - 1)]
    
    return SelectivePredictionMetrics(
        coverages=coverages,
        accuracies=accuracies,
        f1_scores=np.array(f1_scores),
        auc=auc,
        optimal_threshold=float(optimal_threshold),
    )


def find_optimal_thresholds(
    coral_probs: torch.Tensor,
    coral_expected: torch.Tensor,
    focal_probs: torch.Tensor,
    labels: torch.Tensor,
    target_defer_rate: float = 0.05,
    target_focal_accuracy: float = 0.95,
) -> Dict[str, float]:
    """
    Find optimal routing thresholds.
    
    Searches for thresholds that achieve:
    - Target defer rate
    - Target accuracy when using focal head
    
    Args:
        coral_probs: CORAL class probabilities [N, K]
        coral_expected: CORAL expected values [N]
        focal_probs: Focal class probabilities [N, K]
        labels: Ground truth labels [N]
        target_defer_rate: Desired defer rate
        target_focal_accuracy: Target accuracy for focal predictions
        
    Returns:
        Dictionary with optimal thresholds
    """
    if isinstance(coral_probs, np.ndarray):
        coral_probs = torch.from_numpy(coral_probs)
    if isinstance(focal_probs, np.ndarray):
        focal_probs = torch.from_numpy(focal_probs)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    coral_confidence = coral_probs.max(dim=1).values
    focal_confidence = focal_probs.max(dim=1).values
    coral_predictions = coral_probs.argmax(dim=1)
    focal_predictions = focal_probs.argmax(dim=1)
    
    # 1. Defer threshold (percentile of CORAL confidence)
    defer_threshold = np.percentile(
        coral_confidence.cpu().numpy(), 
        target_defer_rate * 100
    )
    
    # 2. Focal threshold (for target accuracy)
    sorted_focal_conf = focal_confidence.sort(descending=True)
    sorted_focal_pred = focal_predictions[sorted_focal_conf.indices]
    sorted_labels = labels[sorted_focal_conf.indices]
    
    cumsum_correct = (sorted_focal_pred == sorted_labels).cumsum(0).float()
    cumsum_total = torch.arange(1, len(labels) + 1, device=labels.device).float()
    cumulative_accuracy = cumsum_correct / cumsum_total
    
    # Find first index where accuracy exceeds target
    focal_threshold = 0.9  # Default high threshold
    for i, (acc, conf) in enumerate(zip(
        cumulative_accuracy.cpu().numpy(),
        sorted_focal_conf.values.cpu().numpy()
    )):
        if acc >= target_focal_accuracy:
            focal_threshold = conf
            break
    
    # 3. Ordinal margin (based on E[y] distribution)
    fractional = torch.abs(coral_expected - coral_expected.round())
    ordinal_margin = np.percentile(fractional.cpu().numpy(), 25)
    
    return {
        'defer_threshold': float(defer_threshold),
        'focal_confidence': float(focal_threshold),
        'ordinal_margin': float(ordinal_margin),
    }


def temperature_scaling(
    logits: torch.Tensor,
    labels: torch.Tensor,
    lr: float = 0.01,
    max_iter: int = 50,
) -> float:
    """
    Find optimal temperature for calibration via temperature scaling.
    
    Temperature scaling: p = softmax(logits / T)
    Optimizes T to minimize NLL on validation set.
    
    Args:
        logits: Model logits [N, K]
        labels: Ground truth labels [N]
        lr: Learning rate for optimization
        max_iter: Maximum iterations
        
    Returns:
        Optimal temperature value
    """
    if isinstance(logits, np.ndarray):
        logits = torch.from_numpy(logits)
    if isinstance(labels, np.ndarray):
        labels = torch.from_numpy(labels)
    
    temperature = torch.nn.Parameter(torch.ones(1) * 1.5)
    optimizer = torch.optim.LBFGS([temperature], lr=lr, max_iter=max_iter)
    
    def eval_fn():
        optimizer.zero_grad()
        scaled_logits = logits / temperature
        loss = torch.nn.functional.cross_entropy(scaled_logits, labels)
        loss.backward()
        return loss
    
    optimizer.step(eval_fn)
    
    return float(temperature.item())


def apply_temperature_scaling(
    probs: torch.Tensor,
    temperature: float,
) -> torch.Tensor:
    """
    Apply temperature scaling to probabilities.
    
    Args:
        probs: Probabilities [N, K]
        temperature: Temperature value
        
    Returns:
        Scaled probabilities [N, K]
    """
    # Convert probs back to logits, scale, and re-softmax
    logits = torch.log(probs.clamp(min=1e-8))
    scaled_logits = logits / temperature
    return torch.softmax(scaled_logits, dim=1)
