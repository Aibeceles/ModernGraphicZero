"""
Edge Feature Inventory for the Graph Label Prediction Pipeline.

Complete registry of all 18 edge features computed by
compute_polynomial_edge_features() in core/edge_features.py.

These features capture the forward difference (Delta) relationship between
connected polynomials in the difference tree, providing rich information
for graph attention mechanisms.

Edge Feature Vector Layout (18 total):
  [0..3]   Depth features       (4D)
  [4..6]   Degree features      (3D)
  [7..9]   Leading coeff features (3D)
  [10..13] Similarity features  (4D)
  [14..15] Magnitude features   (2D)
  [16..17] Constant term features (2D)
"""

# =============================================================================
# Edge Feature Registry
# =============================================================================
#
# Each entry is a dict with:
#   name              - identifier string used in code
#   category          - feature category
#   index             - position in the 18D edge feature vector
#   dtype             - "continuous" or "binary"
#   description       - short human-readable explanation
#   formula           - computation formula
#   range             - expected value range as a string
#   node_dependencies - which node features are consumed

EDGE_FEATURE_REGISTRY = [
    # -------------------------------------------------------------------------
    # Depth Features (4D) - Tree position relationships
    # -------------------------------------------------------------------------
    {
        'name': 'depth_diff_normalized',
        'category': 'depth',
        'index': 0,
        'dtype': 'continuous',
        'description': (
            'Signed depth difference between source and target, '
            'normalized by max depth in batch.'
        ),
        'formula': '(wnum_src - wnum_tgt) / (max_depth + eps)',
        'range': '[-1, 1]',
        'node_dependencies': ['wNum'],
    },
    {
        'name': 'depth_diff_abs',
        'category': 'depth',
        'index': 1,
        'dtype': 'continuous',
        'description': (
            'Absolute depth difference, normalized by max depth. '
            'Captures hop distance regardless of direction.'
        ),
        'formula': '|wnum_src - wnum_tgt| / (max_depth + eps)',
        'range': '[0, 1]',
        'node_dependencies': ['wNum'],
    },
    {
        'name': 'direction',
        'category': 'depth',
        'index': 2,
        'dtype': 'binary',
        'description': (
            'Edge direction indicator. 1.0 if source has higher wNum '
            '(parent-to-child in the difference tree), else 0.0.'
        ),
        'formula': '1.0 if wnum_src > wnum_tgt else 0.0',
        'range': '{0, 1}',
        'node_dependencies': ['wNum'],
    },
    {
        'name': 'is_adjacent',
        'category': 'depth',
        'index': 3,
        'dtype': 'binary',
        'description': (
            'Adjacency indicator. 1.0 if the depth difference is '
            'approximately 1 (direct parent-child), else 0.0.'
        ),
        'formula': '1.0 if ||depth_diff| - 1.0| < 0.1 else 0.0',
        'range': '{0, 1}',
        'node_dependencies': ['wNum'],
    },

    # -------------------------------------------------------------------------
    # Degree Features (3D) - Polynomial degree relationships
    # For differentiation: deg(child) = deg(parent) - 1
    # -------------------------------------------------------------------------
    {
        'name': 'degree_diff_normalized',
        'category': 'degree',
        'index': 4,
        'dtype': 'continuous',
        'description': (
            'Signed degree difference, normalized by scale factor of 4. '
            'Should be approx 0.25 for true differentiation edges.'
        ),
        'formula': '(degree_src - degree_tgt) / 4.0',
        'range': 'approx [-1, 1]',
        'node_dependencies': ['degree_at_node'],
    },
    {
        'name': 'degree_ratio',
        'category': 'degree',
        'index': 5,
        'dtype': 'continuous',
        'description': (
            'Ratio of target to source degree, clamped. '
            'Should be approx (n-1)/n for differentiation.'
        ),
        'formula': 'clamp(degree_tgt / (degree_src + eps), 0, 2)',
        'range': '[0, 2]',
        'node_dependencies': ['degree_at_node'],
    },
    {
        'name': 'degree_consistency',
        'category': 'degree',
        'index': 6,
        'dtype': 'continuous',
        'description': (
            'Gaussian consistency score around degree_diff = 1. '
            'High when the edge looks like a true differentiation step.'
        ),
        'formula': 'exp(-(degree_diff - 1)^2)',
        'range': '[0, 1]',
        'node_dependencies': ['degree_at_node'],
    },

    # -------------------------------------------------------------------------
    # Leading Coefficient Features (3D)
    # For Delta(c_n x^n): leading_child ~ n * leading_parent
    # -------------------------------------------------------------------------
    {
        'name': 'leading_ratio',
        'category': 'leading_coeff',
        'index': 7,
        'dtype': 'continuous',
        'description': (
            'Ratio of target to source leading coefficient, '
            'clamped and normalized to approx [0, 1].'
        ),
        'formula': 'clamp(leading_tgt / (leading_src + eps), 0, 10) / 10',
        'range': '[0, 1]',
        'node_dependencies': ['leading_coeff_abs', 'degree_at_node'],
    },
    {
        'name': 'leading_diff',
        'category': 'leading_coeff',
        'index': 8,
        'dtype': 'continuous',
        'description': (
            'Normalized difference of leading coefficients. '
            'Scaled by max leading coefficient in batch.'
        ),
        'formula': '(leading_tgt - leading_src) / (max(leading_src.max, leading_tgt.max) + eps)',
        'range': 'approx [-1, 1]',
        'node_dependencies': ['leading_coeff_abs'],
    },
    {
        'name': 'scaling_error',
        'category': 'leading_coeff',
        'index': 9,
        'dtype': 'continuous',
        'description': (
            'How far the leading coefficient relationship deviates from '
            'the expected differentiation scaling: leading_tgt ~ degree_src * leading_src.'
        ),
        'formula': 'clamp(|leading_tgt - degree_src * leading_src| / (max_leading + eps), 0, 2)',
        'range': '[0, 2]',
        'node_dependencies': ['leading_coeff_abs', 'degree_at_node'],
    },

    # -------------------------------------------------------------------------
    # Similarity Features (4D) - Coefficient vector similarity
    # -------------------------------------------------------------------------
    {
        'name': 'cosine_similarity',
        'category': 'similarity',
        'index': 10,
        'dtype': 'continuous',
        'description': (
            'Cosine similarity between L2-normalized coefficient vectors. '
            'Captures directional alignment of polynomials.'
        ),
        'formula': 'dot(coeff_src / ||coeff_src||, coeff_tgt / ||coeff_tgt||)',
        'range': '[-1, 1]',
        'node_dependencies': ['coeff_0', 'coeff_1', 'coeff_2', 'coeff_3', 'coeff_4', 'coeff_5'],
    },
    {
        'name': 'l2_distance',
        'category': 'similarity',
        'index': 11,
        'dtype': 'continuous',
        'description': (
            'L2 distance between coefficient vectors, normalized by '
            'max distance in the batch.'
        ),
        'formula': '||coeff_src - coeff_tgt||_2 / (max_dist + eps)',
        'range': '[0, 1]',
        'node_dependencies': ['coeff_0', 'coeff_1', 'coeff_2', 'coeff_3', 'coeff_4', 'coeff_5'],
    },
    {
        'name': 'correlation',
        'category': 'similarity',
        'index': 12,
        'dtype': 'continuous',
        'description': (
            'Pearson correlation between coefficient vectors '
            '(centered before computing). Captures linear covariance.'
        ),
        'formula': 'sum(centered_src * centered_tgt) / (||centered_src|| * ||centered_tgt||)',
        'range': '[-1, 1]',
        'node_dependencies': ['coeff_0', 'coeff_1', 'coeff_2', 'coeff_3', 'coeff_4', 'coeff_5'],
    },
    {
        'name': 'sparsity_diff',
        'category': 'similarity',
        'index': 13,
        'dtype': 'continuous',
        'description': (
            'Difference in coefficient sparsity between source and target. '
            'Positive means source is sparser.'
        ),
        'formula': 'sparsity_src - sparsity_tgt',
        'range': '[-1, 1]',
        'node_dependencies': ['coeff_sparsity'],
    },

    # -------------------------------------------------------------------------
    # Magnitude Features (2D) - Overall scale comparisons
    # -------------------------------------------------------------------------
    {
        'name': 'magnitude_ratio',
        'category': 'magnitude',
        'index': 14,
        'dtype': 'continuous',
        'description': (
            'Ratio of target to source coefficient magnitude (L2 norm), '
            'clamped and normalized to [0, 1].'
        ),
        'formula': 'clamp(magnitude_tgt / (magnitude_src + eps), 0, 10) / 10',
        'range': '[0, 1]',
        'node_dependencies': ['coeff_magnitude'],
    },
    {
        'name': 'magnitude_diff_log',
        'category': 'magnitude',
        'index': 15,
        'dtype': 'continuous',
        'description': (
            'Log-space magnitude difference, normalized by max absolute '
            'log difference in the batch. More stable for varying scales.'
        ),
        'formula': '(log(magnitude_tgt + 1) - log(magnitude_src + 1)) / (max_log_diff + eps)',
        'range': 'approx [-1, 1]',
        'node_dependencies': ['coeff_magnitude'],
    },

    # -------------------------------------------------------------------------
    # Constant Term Features (2D) - c_0 = P(0) relationships
    # For differentiation: Delta P(0) = P(1) - P(0)
    # -------------------------------------------------------------------------
    {
        'name': 'constant_diff',
        'category': 'constant_term',
        'index': 16,
        'dtype': 'continuous',
        'description': (
            'Absolute difference of constant terms, normalized by max '
            'constant in the batch.'
        ),
        'formula': '|constant_tgt - constant_src| / (max(constant_src.max, constant_tgt.max) + eps)',
        'range': '[0, 1]',
        'node_dependencies': ['constant_term_abs'],
    },
    {
        'name': 'constant_ratio',
        'category': 'constant_term',
        'index': 17,
        'dtype': 'continuous',
        'description': (
            'Ratio of target to source constant term, '
            'clamped and normalized to [0, 1].'
        ),
        'formula': 'clamp(constant_tgt / (constant_src + eps), 0, 10) / 10',
        'range': '[0, 1]',
        'node_dependencies': ['constant_term_abs'],
    },
]

# =============================================================================
# Category Name Lists
# =============================================================================

DEPTH_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'depth']
DEGREE_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'degree']
LEADING_COEFF_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'leading_coeff']
SIMILARITY_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'similarity']
MAGNITUDE_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'magnitude']
CONSTANT_TERM_FEATURES = [f['name'] for f in EDGE_FEATURE_REGISTRY if f['category'] == 'constant_term']

ALL_EDGE_FEATURE_NAMES = [f['name'] for f in EDGE_FEATURE_REGISTRY]

# =============================================================================
# Dimension Constants (derived from registry)
# =============================================================================

NUM_DEPTH_FEATURES = len(DEPTH_FEATURES)                    # 4
NUM_DEGREE_FEATURES = len(DEGREE_FEATURES)                  # 3
NUM_LEADING_COEFF_FEATURES = len(LEADING_COEFF_FEATURES)    # 3
NUM_SIMILARITY_FEATURES = len(SIMILARITY_FEATURES)          # 4
NUM_MAGNITUDE_FEATURES = len(MAGNITUDE_FEATURES)            # 2
NUM_CONSTANT_TERM_FEATURES = len(CONSTANT_TERM_FEATURES)    # 2

NUM_EDGE_FEATURES = len(EDGE_FEATURE_REGISTRY)              # 18

# Previous edge feature dimension (for reference / backward compatibility)
NUM_EDGE_FEATURES_OLD = 3  # Was just [depth_diff, abs_diff, direction]

# =============================================================================
# Convenience Lookups
# =============================================================================

EDGE_FEATURE_BY_NAME = {f['name']: f for f in EDGE_FEATURE_REGISTRY}
EDGE_FEATURE_BY_INDEX = {f['index']: f for f in EDGE_FEATURE_REGISTRY}
EDGE_FEATURE_CATEGORIES = {
    'depth': DEPTH_FEATURES,
    'degree': DEGREE_FEATURES,
    'leading_coeff': LEADING_COEFF_FEATURES,
    'similarity': SIMILARITY_FEATURES,
    'magnitude': MAGNITUDE_FEATURES,
    'constant_term': CONSTANT_TERM_FEATURES,
}
