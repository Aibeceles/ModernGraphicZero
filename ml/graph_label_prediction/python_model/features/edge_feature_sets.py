"""
Edge Feature Set Definitions for Ablation Studies.

Defines named subsets of edge feature categories for systematic feature
importance evaluation. Each set specifies which categories (from
edge_features.py) to include.

Usage:
    from python_model.features.edge_feature_sets import edge_feature_indices, apply_edge_mask

    indices = edge_feature_indices('depth_degree')
    edge_attr_masked, idx = apply_edge_mask(edge_attr, 'depth_degree')
"""

import torch
from typing import List, Tuple

from .edge_features import EDGE_FEATURE_REGISTRY

# =============================================================================
# Edge Feature Set Definitions
# =============================================================================
#
# Each entry maps a set name to a list of edge feature categories to INCLUDE.
# Categories: 'depth', 'degree', 'leading_coeff', 'similarity',
#             'magnitude', 'constant_term'

EDGE_FEATURE_SETS = {
    # Progressive addition
    'depth_only':    ['depth'],
    'depth_degree':  ['depth', 'degree'],
    'no_similarity': ['depth', 'degree', 'leading_coeff', 'magnitude', 'constant_term'],
    'full':          ['depth', 'degree', 'leading_coeff', 'similarity', 'magnitude', 'constant_term'],

    # Leave-one-category-out
    'drop_depth':         ['degree', 'leading_coeff', 'similarity', 'magnitude', 'constant_term'],
    'drop_degree':        ['depth', 'leading_coeff', 'similarity', 'magnitude', 'constant_term'],
    'drop_leading_coeff': ['depth', 'degree', 'similarity', 'magnitude', 'constant_term'],
    'drop_similarity':    ['depth', 'degree', 'leading_coeff', 'magnitude', 'constant_term'],
    'drop_magnitude':     ['depth', 'degree', 'leading_coeff', 'similarity', 'constant_term'],
    'drop_constant_term': ['depth', 'degree', 'leading_coeff', 'similarity', 'magnitude'],
}


# =============================================================================
# Public API
# =============================================================================

def edge_feature_indices(set_name: str) -> List[int]:
    """Return sorted column indices to keep for a given edge feature set.

    Args:
        set_name: Key from EDGE_FEATURE_SETS.

    Returns:
        Sorted list of integer column indices into the 18D edge feature vector.

    Raises:
        KeyError: If set_name is not defined.
    """
    if set_name not in EDGE_FEATURE_SETS:
        raise KeyError(
            f"Unknown edge feature set: '{set_name}'. "
            f"Available: {list(EDGE_FEATURE_SETS.keys())}"
        )

    categories = set(EDGE_FEATURE_SETS[set_name])
    return sorted(
        f['index']
        for f in EDGE_FEATURE_REGISTRY
        if f['category'] in categories
    )


def apply_edge_mask(
    edge_attr: torch.Tensor,
    set_name: str,
) -> Tuple[torch.Tensor, List[int]]:
    """Slice edge feature matrix to the columns selected by *set_name*.

    Args:
        edge_attr: Full edge feature matrix [E, 18].
        set_name: Key from EDGE_FEATURE_SETS.

    Returns:
        Tuple of (edge_attr_masked [E, len(indices)], indices).
    """
    indices = edge_feature_indices(set_name)
    return edge_attr[:, indices], indices
