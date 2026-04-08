"""
Polynomial Edge Feature Computation for Graph Attention Networks.

This module provides mathematically-motivated edge features that capture the
differentiation relationship between connected polynomials in the difference tree.

Key insight: In a polynomial difference tree, parent→child edges represent the
forward difference operation Δ. These edge features capture how well the 
coefficient relationship aligns with differentiation properties.

Edge Feature Categories (18 total):
1. Depth Features (4D): Enhanced tree position features
2. Degree Features (3D): Polynomial degree relationships
3. Leading Coefficient Features (3D): Leading term relationships
4. Similarity Features (4D): Coefficient vector similarity
5. Magnitude Features (2D): Overall scale comparisons
6. Constant Term Features (2D): Constant term relationships
"""

import torch
import torch.nn.functional as F
from typing import Tuple

# Import feature indices and MAX_GRAPH_DEPTH from config
from .config import (
    NODE_FEATURE_INDICES,
    MAX_GRAPH_DEPTH,
)

# Feature indices in the node feature vector (from config)
# Layout: [wNum, degree, determined, coeff×(MAX_POLYNOMIAL_DEGREE+1), stats×5, n, mu_d, mu_ratio]
WNUM_IDX = NODE_FEATURE_INDICES['wNum']
DEGREE_IDX = NODE_FEATURE_INDICES['degree']
DETERMINED_IDX = NODE_FEATURE_INDICES['determined']
COEFF_START_IDX = NODE_FEATURE_INDICES['coeff_start']
COEFF_END_IDX = NODE_FEATURE_INDICES['coeff_end']  # exclusive
MAGNITUDE_IDX = NODE_FEATURE_INDICES['magnitude']
LEADING_COEFF_IDX = NODE_FEATURE_INDICES['leading_coeff']
CONSTANT_TERM_IDX = NODE_FEATURE_INDICES['constant_term']
SPARSITY_IDX = NODE_FEATURE_INDICES['sparsity']
MEAN_ABS_IDX = NODE_FEATURE_INDICES['mean_abs']
N_IDX = NODE_FEATURE_INDICES['n']
MU_D_IDX = NODE_FEATURE_INDICES['mu_d']
MU_RATIO_IDX = NODE_FEATURE_INDICES['mu_ratio']

# Total edge feature dimension
NUM_EDGE_FEATURES = 18


def compute_polynomial_edge_features(
    x: torch.Tensor,
    edge_index: torch.Tensor,
    eps: float = 1e-6,
) -> torch.Tensor:
    """
    Compute mathematically-motivated edge features from node features.
    
    For each edge (src, tgt), computes 18 features that capture:
    - Depth/position relationship in the tree
    - Polynomial degree relationships
    - Coefficient similarity and differences
    - Magnitude scaling patterns
    
    These features are designed to capture the differentiation relationship
    where child ≈ Δ(parent), providing rich information for attention.
    
    Args:
        x: Node feature matrix [N, NUM_FEATURES_TOTAL] with structure:
           [wNum, degree, determined, coeff_0..N, magnitude, leading_coeff, constant, sparsity, mean_abs, n, mu_d, mu_ratio]
           where N = MAX_POLYNOMIAL_DEGREE
        edge_index: Edge connectivity [2, E] where edge_index[0] = source, edge_index[1] = target
        eps: Small constant to avoid division by zero
        
    Returns:
        Edge features [E, 18] with the following structure:
        - [0-3]: Depth features
        - [4-6]: Degree features
        - [7-9]: Leading coefficient features
        - [10-13]: Similarity features
        - [14-15]: Magnitude features
        - [16-17]: Constant term features
        
    Example:
        >>> x = data.x  # [N, 16]
        >>> edge_index = data.edge_index  # [2, E]
        >>> edge_feat = compute_polynomial_edge_features(x, edge_index)
        >>> edge_feat.shape
        torch.Size([E, 18])
    """
    src, tgt = edge_index  # [E], [E]
    num_edges = src.shape[0]
    
    if num_edges == 0:
        return torch.zeros((0, NUM_EDGE_FEATURES), device=x.device, dtype=x.dtype)
    
    # Extract source and target features
    x_src = x[src]  # [E, 16]
    x_tgt = x[tgt]  # [E, 16]
    
    # Compute each feature category
    depth_feat = _compute_depth_features(x_src, x_tgt, eps)  # [E, 4]
    degree_feat = _compute_degree_features(x_src, x_tgt, eps)  # [E, 3]
    leading_feat = _compute_leading_coeff_features(x_src, x_tgt, eps)  # [E, 3]
    similarity_feat = _compute_similarity_features(x_src, x_tgt, eps)  # [E, 4]
    magnitude_feat = _compute_magnitude_features(x_src, x_tgt, eps)  # [E, 2]
    constant_feat = _compute_constant_features(x_src, x_tgt, eps)  # [E, 2]
    
    # Concatenate all features
    edge_features = torch.cat([
        depth_feat,       # 4D
        degree_feat,      # 3D
        leading_feat,     # 3D
        similarity_feat,  # 4D
        magnitude_feat,   # 2D
        constant_feat,    # 2D
    ], dim=-1)  # [E, 18]
    
    return edge_features


def _compute_depth_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute enhanced depth-based edge features.
    
    Features:
    1. depth_diff_normalized: Signed depth difference / max_depth
    2. depth_diff_abs: Absolute depth difference / max_depth
    3. direction: 1.0 if src has higher wNum (parent→child), else 0.0
    4. is_adjacent: 1.0 if |depth_diff| == 1, else 0.0
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Depth features [E, 4]
    """
    wnum_src = x_src[:, WNUM_IDX]  # [E]
    wnum_tgt = x_tgt[:, WNUM_IDX]  # [E]
    
    depth_diff = wnum_src - wnum_tgt  # [E]
    
    # Normalize by max depth in batch (or use MAX_GRAPH_DEPTH for stability)
    max_depth = torch.max(
        torch.max(wnum_src.abs().max(), wnum_tgt.abs().max()),
        torch.tensor(float(MAX_GRAPH_DEPTH), device=wnum_src.device)
    )
    
    depth_diff_normalized = depth_diff / (max_depth + eps)  # [-1, 1]
    depth_diff_abs = depth_diff.abs() / (max_depth + eps)  # [0, 1]
    direction = (depth_diff > 0).float()  # 1.0 = parent→child
    is_adjacent = (depth_diff.abs() - 1.0).abs() < 0.1  # approximately 1
    is_adjacent = is_adjacent.float()
    
    return torch.stack([
        depth_diff_normalized,
        depth_diff_abs,
        direction,
        is_adjacent,
    ], dim=-1)  # [E, 4]


def _compute_degree_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute degree relationship features.
    
    For differentiation, we expect: deg(child) = deg(parent) - 1
    
    Features:
    1. degree_diff: degree_src - degree_tgt (should be ~1 for Δ)
    2. degree_ratio: degree_tgt / degree_src (should be ~(n-1)/n for Δ)
    3. degree_consistency: 1.0 if degree_diff ∈ [0.5, 1.5], else sigmoid decay
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Degree features [E, 3]
    """
    degree_src = x_src[:, DEGREE_IDX]  # [E]
    degree_tgt = x_tgt[:, DEGREE_IDX]  # [E]
    
    degree_diff = degree_src - degree_tgt  # [E], should be ~1 for differentiation
    
    # Normalize degree difference (typical range 0-4)
    degree_diff_normalized = degree_diff / 4.0
    
    # Degree ratio (clamped to reasonable range)
    degree_ratio = degree_tgt / (degree_src + eps)
    degree_ratio = torch.clamp(degree_ratio, 0.0, 2.0)
    
    # Degree consistency: how close is degree_diff to 1.0?
    # Use smooth indicator that's high when degree_diff ≈ 1
    degree_consistency = torch.exp(-((degree_diff - 1.0) ** 2))  # Gaussian around 1.0
    
    return torch.stack([
        degree_diff_normalized,
        degree_ratio,
        degree_consistency,
    ], dim=-1)  # [E, 3]


def _compute_leading_coeff_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute leading coefficient relationship features.
    
    For differentiation Δ(c_n x^n), the leading coefficient transforms as:
    leading_coeff_child ≈ n * leading_coeff_parent
    
    Features:
    1. leading_coeff_ratio: |leading_tgt| / |leading_src|
    2. leading_coeff_diff: (leading_tgt - leading_src) normalized
    3. expected_scaling_error: |leading_tgt - degree_src * leading_src| normalized
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Leading coefficient features [E, 3]
    """
    leading_src = x_src[:, LEADING_COEFF_IDX]  # [E] (already absolute value)
    leading_tgt = x_tgt[:, LEADING_COEFF_IDX]  # [E]
    degree_src = x_src[:, DEGREE_IDX]  # [E]
    
    # Ratio of leading coefficients
    leading_ratio = leading_tgt / (leading_src + eps)
    leading_ratio = torch.clamp(leading_ratio, 0.0, 10.0)  # Limit extreme ratios
    
    # Normalized difference
    max_leading = torch.max(leading_src.max(), leading_tgt.max()) + eps
    leading_diff = (leading_tgt - leading_src) / max_leading
    
    # Expected scaling: for Δ, we expect leading_tgt ≈ degree_src * leading_src
    # But this relationship is approximate, so we compute error
    expected_leading = degree_src * leading_src
    scaling_error = (leading_tgt - expected_leading).abs() / (max_leading + eps)
    scaling_error = torch.clamp(scaling_error, 0.0, 2.0)  # Limit extreme errors
    
    return torch.stack([
        leading_ratio / 10.0,  # Normalize to ~[0, 1]
        leading_diff,
        scaling_error,
    ], dim=-1)  # [E, 3]


def _compute_similarity_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute coefficient vector similarity features.
    
    Features:
    1. cosine_similarity: cos(coeff_src, coeff_tgt)
    2. l2_distance: ||coeff_src - coeff_tgt||_2 normalized
    3. correlation: Pearson correlation of coefficient vectors
    4. sparsity_diff: sparsity_src - sparsity_tgt
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Similarity features [E, 4]
    """
    # Extract coefficient vectors [E, 5]
    coeff_src = x_src[:, COEFF_START_IDX:COEFF_END_IDX]
    coeff_tgt = x_tgt[:, COEFF_START_IDX:COEFF_END_IDX]
    
    # 1. Cosine similarity
    norm_src = torch.norm(coeff_src, p=2, dim=-1, keepdim=True) + eps
    norm_tgt = torch.norm(coeff_tgt, p=2, dim=-1, keepdim=True) + eps
    coeff_src_norm = coeff_src / norm_src
    coeff_tgt_norm = coeff_tgt / norm_tgt
    cosine_sim = (coeff_src_norm * coeff_tgt_norm).sum(dim=-1)  # [E]
    
    # 2. L2 distance (normalized)
    l2_dist = torch.norm(coeff_src - coeff_tgt, p=2, dim=-1)  # [E]
    # Normalize by typical magnitude
    max_dist = l2_dist.max() + eps
    l2_dist_normalized = l2_dist / max_dist
    
    # 3. Pearson correlation
    # Center the coefficient vectors
    coeff_src_centered = coeff_src - coeff_src.mean(dim=-1, keepdim=True)
    coeff_tgt_centered = coeff_tgt - coeff_tgt.mean(dim=-1, keepdim=True)
    
    numerator = (coeff_src_centered * coeff_tgt_centered).sum(dim=-1)
    denom_src = torch.norm(coeff_src_centered, p=2, dim=-1) + eps
    denom_tgt = torch.norm(coeff_tgt_centered, p=2, dim=-1) + eps
    correlation = numerator / (denom_src * denom_tgt)  # [E], range [-1, 1]
    
    # 4. Sparsity difference
    sparsity_src = x_src[:, SPARSITY_IDX]  # [E]
    sparsity_tgt = x_tgt[:, SPARSITY_IDX]  # [E]
    sparsity_diff = sparsity_src - sparsity_tgt  # [E], range [-1, 1]
    
    return torch.stack([
        cosine_sim,
        l2_dist_normalized,
        correlation,
        sparsity_diff,
    ], dim=-1)  # [E, 4]


def _compute_magnitude_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute overall magnitude comparison features.
    
    Features:
    1. magnitude_ratio: ||coeff_tgt|| / ||coeff_src||
    2. magnitude_diff_log: log(||coeff_tgt|| + 1) - log(||coeff_src|| + 1)
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Magnitude features [E, 2]
    """
    magnitude_src = x_src[:, MAGNITUDE_IDX]  # [E]
    magnitude_tgt = x_tgt[:, MAGNITUDE_IDX]  # [E]
    
    # Ratio (clamped to reasonable range)
    magnitude_ratio = magnitude_tgt / (magnitude_src + eps)
    magnitude_ratio = torch.clamp(magnitude_ratio, 0.0, 10.0) / 10.0  # Normalize to [0, 1]
    
    # Log difference (more stable for varying scales)
    log_src = torch.log(magnitude_src + 1.0)
    log_tgt = torch.log(magnitude_tgt + 1.0)
    magnitude_diff_log = log_tgt - log_src
    
    # Normalize log difference
    max_log_diff = magnitude_diff_log.abs().max() + eps
    magnitude_diff_log = magnitude_diff_log / max_log_diff
    
    return torch.stack([
        magnitude_ratio,
        magnitude_diff_log,
    ], dim=-1)  # [E, 2]


def _compute_constant_features(
    x_src: torch.Tensor, 
    x_tgt: torch.Tensor, 
    eps: float
) -> torch.Tensor:
    """
    Compute constant term relationship features.
    
    For polynomials, the constant term c_0 = P(0).
    For differentiation: ΔP(0) = P(1) - P(0).
    
    Features:
    1. constant_diff: |c0_tgt - c0_src| normalized
    2. constant_ratio: |c0_tgt| / |c0_src|
    
    Args:
        x_src: Source node features [E, 16]
        x_tgt: Target node features [E, 16]
        eps: Numerical stability constant
        
    Returns:
        Constant term features [E, 2]
    """
    constant_src = x_src[:, CONSTANT_TERM_IDX]  # [E] (already absolute value)
    constant_tgt = x_tgt[:, CONSTANT_TERM_IDX]  # [E]
    
    # Absolute difference (normalized)
    constant_diff = (constant_tgt - constant_src).abs()
    max_constant = torch.max(constant_src.max(), constant_tgt.max()) + eps
    constant_diff_normalized = constant_diff / max_constant
    
    # Ratio (clamped)
    constant_ratio = constant_tgt / (constant_src + eps)
    constant_ratio = torch.clamp(constant_ratio, 0.0, 10.0) / 10.0  # Normalize to [0, 1]
    
    return torch.stack([
        constant_diff_normalized,
        constant_ratio,
    ], dim=-1)  # [E, 2]


def get_edge_feature_names() -> list:
    """
    Get human-readable names for all 18 edge features.
    
    Returns:
        List of 18 feature names in order.
    """
    return [
        # Depth features (4)
        'depth_diff_normalized',
        'depth_diff_abs',
        'direction',
        'is_adjacent',
        # Degree features (3)
        'degree_diff_normalized',
        'degree_ratio',
        'degree_consistency',
        # Leading coefficient features (3)
        'leading_ratio',
        'leading_diff',
        'scaling_error',
        # Similarity features (4)
        'cosine_similarity',
        'l2_distance',
        'correlation',
        'sparsity_diff',
        # Magnitude features (2)
        'magnitude_ratio',
        'magnitude_diff_log',
        # Constant term features (2)
        'constant_diff',
        'constant_ratio',
    ]

