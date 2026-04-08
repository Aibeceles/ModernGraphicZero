"""
Node Feature Inventory for the Graph Label Prediction Pipeline.

Complete registry of all node features used as input to the GNN models.
Each feature is documented with its name, group, position in the feature
vector, data type, source, description, and expected value range.

Feature Vector Layout (25 total = 17 base + 8 spectral PE):
  [0]      wNum
  [1]      degree_at_node
  [2]      determined
  [3..8]   coeff_0 .. coeff_5          (MAX_POLYNOMIAL_DEGREE + 1)
  [9]      coeff_magnitude
  [10]     leading_coeff_abs
  [11]     constant_term_abs
  [12]     coeff_sparsity
  [13]     coeff_mean_abs
  [14]     n
  [15]     mu_d
  [16]     mu_ratio
  [17..24] spectral_pe_0 .. spectral_pe_7  (SPECTRAL_PE_DIM)
"""

# =============================================================================
# Master Configuration (mirrored from config.py)
# =============================================================================

MAX_GRAPH_DEPTH = 5
MAX_POLYNOMIAL_DEGREE = MAX_GRAPH_DEPTH
SPECTRAL_PE_DIM = 8

# =============================================================================
# Node Feature Registry
# =============================================================================
#
# Each entry is a dict with:
#   name        - identifier string used in code
#   group       - feature category
#   index       - position in the node feature vector
#   dtype       - "continuous", "binary", or "categorical"
#   source      - where the raw data originates
#   description - short human-readable explanation
#   range       - expected value range as a string

NODE_FEATURE_REGISTRY = [
    # -------------------------------------------------------------------------
    # Base Features (3)
    # -------------------------------------------------------------------------
    {
        'name': 'wNum',
        'group': 'base',
        'index': 0,
        'dtype': 'continuous',
        'source': 'CreatedBy.wNum',
        'description': (
            'Depth in the polynomial difference tree. Number of '
            'differentiation steps (0=constant, 1=linear, ..., 5=quintic).'
        ),
        'range': '[0, MAX_GRAPH_DEPTH]',
    },
    {
        'name': 'degree_at_node',
        'group': 'base',
        'index': 1,
        'dtype': 'continuous',
        'source': 'derived from Dnode.vmResult',
        'description': (
            'Effective polynomial degree from non-zero coefficients in '
            'vmResult. Computed by parse_vmresult_coefficients().'
        ),
        'range': '[0, MAX_GRAPH_DEPTH]',
    },
    {
        'name': 'determined',
        'group': 'base',
        'index': 2,
        'dtype': 'binary',
        'source': 'Dnode.determined',
        'description': (
            'Completeness flag computed on a larger/full scan window. '
            'Indicates whether the root count is fully determined. '
            'Almost fully separates classes.'
        ),
        'range': '{0, 1}',
    },

    # -------------------------------------------------------------------------
    # Coefficient Features (6 = MAX_POLYNOMIAL_DEGREE + 1)
    # Polynomial coefficients in ascending power order from vmResult.
    # Padded to length MAX_POLYNOMIAL_DEGREE + 1.
    # -------------------------------------------------------------------------
    {
        'name': 'coeff_0',
        'group': 'coefficient',
        'index': 3,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Constant term c_0 of the polynomial (x^0 coefficient).',
        'range': '(-inf, +inf)',
    },
    {
        'name': 'coeff_1',
        'group': 'coefficient',
        'index': 4,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Linear coefficient c_1 (x^1 coefficient).',
        'range': '(-inf, +inf)',
    },
    {
        'name': 'coeff_2',
        'group': 'coefficient',
        'index': 5,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Quadratic coefficient c_2 (x^2 coefficient).',
        'range': '(-inf, +inf)',
    },
    {
        'name': 'coeff_3',
        'group': 'coefficient',
        'index': 6,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Cubic coefficient c_3 (x^3 coefficient).',
        'range': '(-inf, +inf)',
    },
    {
        'name': 'coeff_4',
        'group': 'coefficient',
        'index': 7,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Quartic coefficient c_4 (x^4 coefficient).',
        'range': '(-inf, +inf)',
    },
    {
        'name': 'coeff_5',
        'group': 'coefficient',
        'index': 8,
        'dtype': 'continuous',
        'source': 'parsed from Dnode.vmResult',
        'description': 'Quintic coefficient c_5 (x^5 coefficient).',
        'range': '(-inf, +inf)',
    },

    # -------------------------------------------------------------------------
    # Statistical Features (5)
    # Derived from the coefficient vector by compute_coefficient_statistics().
    # -------------------------------------------------------------------------
    {
        'name': 'coeff_magnitude',
        'group': 'statistical',
        'index': 9,
        'dtype': 'continuous',
        'source': 'derived from coefficient vector',
        'description': (
            'L2 norm of the coefficient vector: sqrt(sum(c_i^2)). '
            'Captures overall polynomial scale.'
        ),
        'range': '[0, +inf)',
    },
    {
        'name': 'leading_coeff_abs',
        'group': 'statistical',
        'index': 10,
        'dtype': 'continuous',
        'source': 'derived from coefficient vector',
        'description': (
            'Absolute value of the highest non-zero power coefficient. '
            'Determines asymptotic polynomial behavior.'
        ),
        'range': '[0, +inf)',
    },
    {
        'name': 'constant_term_abs',
        'group': 'statistical',
        'index': 11,
        'dtype': 'continuous',
        'source': 'derived from coefficient vector',
        'description': 'Absolute value of c_0 (constant term). Equal to |P(0)|.',
        'range': '[0, +inf)',
    },
    {
        'name': 'coeff_sparsity',
        'group': 'statistical',
        'index': 12,
        'dtype': 'continuous',
        'source': 'derived from coefficient vector',
        'description': (
            'Fraction of zero coefficients among terms up to the actual '
            'degree. Captures polynomial structure/simplicity.'
        ),
        'range': '[0, 1]',
    },
    {
        'name': 'coeff_mean_abs',
        'group': 'statistical',
        'index': 13,
        'dtype': 'continuous',
        'source': 'derived from coefficient vector',
        'description': (
            'Mean of |c_i| over coefficients up to the actual degree. '
            'Average coefficient magnitude.'
        ),
        'range': '[0, +inf)',
    },

    # -------------------------------------------------------------------------
    # Set Union Ratio Features (3)
    # Derived from muList on the Dnode.
    # See suggestion_and_feature_review.md Appendix A.
    # -------------------------------------------------------------------------
    {
        'name': 'n',
        'group': 'set_union',
        'index': 14,
        'dtype': 'continuous',
        'source': 'Dnode.n',
        'description': (
            'Mu numerator = max(muList). Cardinality of the union of '
            'sets in the encoding.'
        ),
        'range': '[0, +inf)',
    },
    {
        'name': 'mu_d',
        'group': 'set_union',
        'index': 15,
        'dtype': 'continuous',
        'source': 'Dnode.d (aliased mu_d in query)',
        'description': (
            'Mu denominator = sum(muList). Sum of individual set '
            'cardinalities. Renamed from d to avoid Python keyword.'
        ),
        'range': '[0, +inf)',
    },
    {
        'name': 'mu_ratio',
        'group': 'set_union',
        'index': 16,
        'dtype': 'continuous',
        'source': 'computed as n / mu_d',
        'description': (
            'Set Union Ratio mu = n / d. Measures overlap among sets. '
            'Safe division: 0 when denominator is 0.'
        ),
        'range': '[0, 1]',
    },

    # -------------------------------------------------------------------------
    # Spectral Positional Encodings (8)
    # Smallest non-trivial eigenvectors of the normalized graph Laplacian.
    # Disabled (zero-filled) when node count > MAX_NODES_FOR_SPECTRAL_PE.
    # -------------------------------------------------------------------------
    {
        'name': 'spectral_pe_0',
        'group': 'spectral_pe',
        'index': 17,
        'dtype': 'continuous',
        'source': 'eigenvector 1 of normalized Laplacian',
        'description': '1st spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_1',
        'group': 'spectral_pe',
        'index': 18,
        'dtype': 'continuous',
        'source': 'eigenvector 2 of normalized Laplacian',
        'description': '2nd spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_2',
        'group': 'spectral_pe',
        'index': 19,
        'dtype': 'continuous',
        'source': 'eigenvector 3 of normalized Laplacian',
        'description': '3rd spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_3',
        'group': 'spectral_pe',
        'index': 20,
        'dtype': 'continuous',
        'source': 'eigenvector 4 of normalized Laplacian',
        'description': '4th spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_4',
        'group': 'spectral_pe',
        'index': 21,
        'dtype': 'continuous',
        'source': 'eigenvector 5 of normalized Laplacian',
        'description': '5th spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_5',
        'group': 'spectral_pe',
        'index': 22,
        'dtype': 'continuous',
        'source': 'eigenvector 6 of normalized Laplacian',
        'description': '6th spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_6',
        'group': 'spectral_pe',
        'index': 23,
        'dtype': 'continuous',
        'source': 'eigenvector 7 of normalized Laplacian',
        'description': '7th spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
    {
        'name': 'spectral_pe_7',
        'group': 'spectral_pe',
        'index': 24,
        'dtype': 'continuous',
        'source': 'eigenvector 8 of normalized Laplacian',
        'description': '8th spectral positional encoding coordinate.',
        'range': '(-1, 1)',
    },
]

# =============================================================================
# Feature Group Name Lists
# =============================================================================

BASE_FEATURES = [f['name'] for f in NODE_FEATURE_REGISTRY if f['group'] == 'base']
COEFFICIENT_FEATURES = [f['name'] for f in NODE_FEATURE_REGISTRY if f['group'] == 'coefficient']
STATISTICAL_FEATURES = [f['name'] for f in NODE_FEATURE_REGISTRY if f['group'] == 'statistical']
SET_UNION_FEATURES = [f['name'] for f in NODE_FEATURE_REGISTRY if f['group'] == 'set_union']
SPECTRAL_PE_FEATURES = [f['name'] for f in NODE_FEATURE_REGISTRY if f['group'] == 'spectral_pe']

ALL_NODE_FEATURE_NAMES = [f['name'] for f in NODE_FEATURE_REGISTRY]

# =============================================================================
# Dimension Constants (derived from registry)
# =============================================================================

NUM_BASE_FEATURES = len(BASE_FEATURES)                  # 3
NUM_COEFFICIENT_FEATURES = len(COEFFICIENT_FEATURES)    # 6  (MAX_POLYNOMIAL_DEGREE + 1)
NUM_STATISTICAL_FEATURES = len(STATISTICAL_FEATURES)    # 5
NUM_SET_UNION_FEATURES = len(SET_UNION_FEATURES)        # 3
NUM_SPECTRAL_PE_FEATURES = len(SPECTRAL_PE_FEATURES)    # 8

NUM_FEATURES_TOTAL = (NUM_BASE_FEATURES + NUM_COEFFICIENT_FEATURES +
                      NUM_STATISTICAL_FEATURES + NUM_SET_UNION_FEATURES)  # 17
NUM_FEATURES = NUM_FEATURES_TOTAL + NUM_SPECTRAL_PE_FEATURES              # 25

# =============================================================================
# Index Mapping (for edge feature computation and direct access)
# =============================================================================

_COEFF_START = NUM_BASE_FEATURES                                 # 3
_COEFF_END = _COEFF_START + NUM_COEFFICIENT_FEATURES             # 9
_STATS_START = _COEFF_END                                        # 9

NODE_FEATURE_INDICES = {
    'wNum': 0,
    'degree': 1,
    'determined': 2,
    'coeff_start': _COEFF_START,
    'coeff_end': _COEFF_END,           # exclusive
    'magnitude': _STATS_START,         # 9
    'leading_coeff': _STATS_START + 1, # 10
    'constant_term': _STATS_START + 2, # 11
    'sparsity': _STATS_START + 3,      # 12
    'mean_abs': _STATS_START + 4,      # 13
    'n': _STATS_START + 5,             # 14
    'mu_d': _STATS_START + 6,          # 15
    'mu_ratio': _STATS_START + 7,      # 16
}

# =============================================================================
# Convenience Lookups
# =============================================================================

NODE_FEATURE_BY_NAME = {f['name']: f for f in NODE_FEATURE_REGISTRY}
NODE_FEATURE_BY_INDEX = {f['index']: f for f in NODE_FEATURE_REGISTRY}
NODE_FEATURE_GROUPS = {
    'base': BASE_FEATURES,
    'coefficient': COEFFICIENT_FEATURES,
    'statistical': STATISTICAL_FEATURES,
    'set_union': SET_UNION_FEATURES,
    'spectral_pe': SPECTRAL_PE_FEATURES,
}
