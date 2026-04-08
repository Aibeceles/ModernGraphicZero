"""
Node Feature Set Definitions for Ablation Studies.

Defines named subsets of node feature groups for systematic feature importance
evaluation. Each set specifies which groups (from node_features.py) to include.

Usage:
    from python_model.features.feature_sets import node_feature_indices, apply_node_mask

    indices = node_feature_indices('core_coeff')
    x_masked, idx = apply_node_mask(data.x, 'core_coeff')
"""

import torch
from typing import List, Tuple

from .node_features import NODE_FEATURE_REGISTRY

# =============================================================================
# Feature Set Definitions
# =============================================================================
#
# Each entry maps a set name to a list of feature groups to INCLUDE.
# Groups: 'base', 'coefficient', 'statistical', 'set_union', 'spectral_pe'
#
# The special value None triggers custom handling (see node_feature_indices).

FEATURE_SETS = {
    # Progressive addition
    'core_only':          ['base'],
    'core_coeff':         ['base', 'coefficient'],
    'core_coeff_stats':   ['base', 'coefficient', 'statistical'],
    'all_no_spectral':    ['base', 'coefficient', 'statistical', 'set_union'],
    'full':               ['base', 'coefficient', 'statistical', 'set_union', 'spectral_pe'],

    # Leave-one-group-out
    'drop_spectral':      ['base', 'coefficient', 'statistical', 'set_union'],
    'drop_set_union':     ['base', 'coefficient', 'statistical', 'spectral_pe'],
    'drop_statistical':   ['base', 'coefficient', 'set_union', 'spectral_pe'],
    'drop_coefficient':   ['base', 'statistical', 'set_union', 'spectral_pe'],

    # Special: determined ablation (handled as special case below)
    'full_no_determined':  None,
}

# Index of the 'determined' feature in the full node feature vector
_DETERMINED_INDEX = 2


# =============================================================================
# Public API
# =============================================================================

def node_feature_indices(set_name: str) -> List[int]:
    """Return sorted column indices to keep for a given feature set.

    Args:
        set_name: Key from FEATURE_SETS.

    Returns:
        Sorted list of integer column indices into the full node feature vector.

    Raises:
        KeyError: If set_name is not defined.
    """
    if set_name not in FEATURE_SETS:
        raise KeyError(
            f"Unknown feature set: '{set_name}'. "
            f"Available: {list(FEATURE_SETS.keys())}"
        )

    groups_or_none = FEATURE_SETS[set_name]

    if groups_or_none is None:
        # Special case: start from 'full' and drop 'determined' (index 2)
        full_indices = _indices_for_groups(FEATURE_SETS['full'])
        return sorted(idx for idx in full_indices if idx != _DETERMINED_INDEX)

    return _indices_for_groups(groups_or_none)


def apply_node_mask(
    x: torch.Tensor,
    set_name: str,
) -> Tuple[torch.Tensor, List[int]]:
    """Slice node feature matrix to the columns selected by *set_name*.

    Args:
        x: Full node feature matrix [N, NUM_FEATURES].
        set_name: Key from FEATURE_SETS.

    Returns:
        Tuple of (x_masked [N, len(indices)], indices).
    """
    indices = node_feature_indices(set_name)
    return x[:, indices], indices


# =============================================================================
# Internal helpers
# =============================================================================

def _indices_for_groups(groups: List[str]) -> List[int]:
    """Collect and sort feature indices for the requested groups."""
    group_set = set(groups)
    return sorted(
        f['index']
        for f in NODE_FEATURE_REGISTRY
        if f['group'] in group_set
    )
