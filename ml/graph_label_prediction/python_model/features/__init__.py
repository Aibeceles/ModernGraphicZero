"""
Feature Inventory Package for the Graph Label Prediction Pipeline.

Provides a centralized, config-style registry of all node and edge features
used by the GNN models. Import from here for feature metadata, dimension
constants, and index mappings.

Usage:
    from python_model.features import NODE_FEATURE_REGISTRY, EDGE_FEATURE_REGISTRY
    from python_model.features import NODE_FEATURE_INDICES, NUM_EDGE_FEATURES
"""

# ---------------------------------------------------------------------------
# Node feature exports
# ---------------------------------------------------------------------------
from .node_features import (
    # Registry
    NODE_FEATURE_REGISTRY,
    NODE_FEATURE_BY_NAME,
    NODE_FEATURE_BY_INDEX,
    NODE_FEATURE_GROUPS,
    ALL_NODE_FEATURE_NAMES,
    # Group name lists
    BASE_FEATURES,
    COEFFICIENT_FEATURES,
    STATISTICAL_FEATURES,
    SET_UNION_FEATURES,
    SPECTRAL_PE_FEATURES,
    # Dimension constants
    NUM_BASE_FEATURES,
    NUM_COEFFICIENT_FEATURES,
    NUM_STATISTICAL_FEATURES,
    NUM_SET_UNION_FEATURES,
    NUM_SPECTRAL_PE_FEATURES,
    NUM_FEATURES_TOTAL,
    NUM_FEATURES,
    # Index mapping
    NODE_FEATURE_INDICES,
    # Master config
    MAX_GRAPH_DEPTH,
    MAX_POLYNOMIAL_DEGREE,
    SPECTRAL_PE_DIM,
)

# ---------------------------------------------------------------------------
# Edge feature exports
# ---------------------------------------------------------------------------
from .edge_features import (
    # Registry
    EDGE_FEATURE_REGISTRY,
    EDGE_FEATURE_BY_NAME,
    EDGE_FEATURE_BY_INDEX,
    EDGE_FEATURE_CATEGORIES,
    ALL_EDGE_FEATURE_NAMES,
    # Category name lists
    DEPTH_FEATURES,
    DEGREE_FEATURES,
    LEADING_COEFF_FEATURES,
    SIMILARITY_FEATURES,
    MAGNITUDE_FEATURES,
    CONSTANT_TERM_FEATURES,
    # Dimension constants
    NUM_DEPTH_FEATURES,
    NUM_DEGREE_FEATURES,
    NUM_LEADING_COEFF_FEATURES,
    NUM_SIMILARITY_FEATURES,
    NUM_MAGNITUDE_FEATURES,
    NUM_CONSTANT_TERM_FEATURES,
    NUM_EDGE_FEATURES,
    NUM_EDGE_FEATURES_OLD,
)

# ---------------------------------------------------------------------------
# Feature set / ablation exports
# ---------------------------------------------------------------------------
from .feature_sets import (
    FEATURE_SETS,
    node_feature_indices,
    apply_node_mask,
)
from .edge_feature_sets import (
    EDGE_FEATURE_SETS,
    edge_feature_indices,
    apply_edge_mask,
)

__all__ = [
    # Node feature registry
    'NODE_FEATURE_REGISTRY',
    'NODE_FEATURE_BY_NAME',
    'NODE_FEATURE_BY_INDEX',
    'NODE_FEATURE_GROUPS',
    'ALL_NODE_FEATURE_NAMES',
    'BASE_FEATURES',
    'COEFFICIENT_FEATURES',
    'STATISTICAL_FEATURES',
    'SET_UNION_FEATURES',
    'SPECTRAL_PE_FEATURES',
    'NUM_BASE_FEATURES',
    'NUM_COEFFICIENT_FEATURES',
    'NUM_STATISTICAL_FEATURES',
    'NUM_SET_UNION_FEATURES',
    'NUM_SPECTRAL_PE_FEATURES',
    'NUM_FEATURES_TOTAL',
    'NUM_FEATURES',
    'NODE_FEATURE_INDICES',
    'MAX_GRAPH_DEPTH',
    'MAX_POLYNOMIAL_DEGREE',
    'SPECTRAL_PE_DIM',
    # Edge feature registry
    'EDGE_FEATURE_REGISTRY',
    'EDGE_FEATURE_BY_NAME',
    'EDGE_FEATURE_BY_INDEX',
    'EDGE_FEATURE_CATEGORIES',
    'ALL_EDGE_FEATURE_NAMES',
    'DEPTH_FEATURES',
    'DEGREE_FEATURES',
    'LEADING_COEFF_FEATURES',
    'SIMILARITY_FEATURES',
    'MAGNITUDE_FEATURES',
    'CONSTANT_TERM_FEATURES',
    'NUM_DEPTH_FEATURES',
    'NUM_DEGREE_FEATURES',
    'NUM_LEADING_COEFF_FEATURES',
    'NUM_SIMILARITY_FEATURES',
    'NUM_MAGNITUDE_FEATURES',
    'NUM_CONSTANT_TERM_FEATURES',
    'NUM_EDGE_FEATURES',
    'NUM_EDGE_FEATURES_OLD',
    # Feature set / ablation utilities
    'FEATURE_SETS',
    'node_feature_indices',
    'apply_node_mask',
    'EDGE_FEATURE_SETS',
    'edge_feature_indices',
    'apply_edge_mask',
]
