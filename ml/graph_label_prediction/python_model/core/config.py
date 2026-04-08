"""
Configuration constants for the Graph Label Prediction pipeline.

This module centralizes all hyperparameters and configuration values
used throughout the node classification pipeline.

IMPROVED VERSION: Adds spectral positional encodings, depth-aware attention,
and class weights for handling imbalanced root count prediction.

GRAPH FILTERING: For large graphs (>1M nodes), filter by pArrayList to enable
spectral PE computation on a manageable subset.
"""

import torch

# =============================================================================
# Graph Filtering Configuration
# =============================================================================

# Enable graph filtering for spectral PE computation on large graphs
USE_GRAPH_FILTERING = True

# pArrayList constraints for filtering CreatedBy nodes
# Only include nodes where all pArrayList values are in [PARRAY_MIN, PARRAY_MAX)
PARRAY_MIN = 0
PARRAY_MAX = 7  # Exclusive upper bound

# Threshold for deciding if spectral PE is feasible
MAX_NODES_FOR_SPECTRAL_PE = 100000  # Skip spectral PE if graph > 100K nodes

# =============================================================================
# Feature Configuration
# Canonical definitions live in python_model.features (node_features.py,
# edge_features.py).  The constants below are re-exported from there so
# that every existing ``from .config import ...`` keeps working unchanged.
# =============================================================================

from ..features.node_features import (
    MAX_GRAPH_DEPTH,
    MAX_POLYNOMIAL_DEGREE,
    BASE_FEATURES,
    COEFFICIENT_FEATURES,
    STATISTICAL_FEATURES,
    SET_UNION_FEATURES,
)

# Target property for classification (root count prediction)
TARGET_PROPERTY = 'totalZero'

# Target modes
# - 'visible': predict y_visible = len(RootList) (all distinct roots in expanded window)
# - 'multitask': y_visible + predict `determined` (computed on larger/full scan window)
# - 'censored': predict y_true with constraint y_true >= y_visible (see trainer)
TARGET_MODE = 'visible'  # 'visible' | 'multitask' | 'censored'

# Loss function type for classification
# - 'ce': Cross-entropy (standard softmax + NLL)
# - 'emd': Earth Mover's Distance (ordinal-aware, penalizes by distance from true class)
# - 'focal': Focal loss (down-weights easy examples, focuses on hard minority classes)
LOSS_TYPE = 'emd'  # 'ce' | 'emd' | 'focal'

# Focal loss gamma parameter (only used when LOSS_TYPE = 'focal')
# Higher gamma = more focus on hard examples; typical values: 2.0-5.0
FOCAL_GAMMA = 2.0

# =============================================================================
# Mini-Batch Training Configuration
# =============================================================================

# Whether to use mini-batch training (True) or full-batch (False)
USE_MINIBATCH = True

# Mini-batch training parameters
BATCH_SIZE = 256  # Number of target nodes per batch
NUM_NEIGHBORS = [10, 10]  # Neighbors to sample at each hop (2-hop)

# WeightedRandomSampler settings
USE_WEIGHTED_SAMPLER = True  # Oversample minority classes

# Feature dimensions (re-exported from features package)
from ..features.node_features import (
    NUM_BASE_FEATURES,
    NUM_COEFFICIENT_FEATURES,
    NUM_STATISTICAL_FEATURES,
    NUM_SET_UNION_FEATURES,
    NUM_FEATURES_TOTAL,
    SPECTRAL_PE_DIM,
    NUM_FEATURES,
)

# =============================================================================
# Edge Feature Configuration (re-exported from features package)
# =============================================================================

from ..features.node_features import NODE_FEATURE_INDICES
from ..features.edge_features import NUM_EDGE_FEATURES, NUM_EDGE_FEATURES_OLD

# Number of output classes (root count: 0, 1, ..., MAX_GRAPH_DEPTH)
# Derived from MAX_GRAPH_DEPTH for arbitrary depth support
NUM_CLASSES = MAX_GRAPH_DEPTH + 1

# Class names for reporting (dynamically generated)
CLASS_NAMES = [f'{i} root{"s" if i != 1 else ""}' for i in range(NUM_CLASSES)]

# =============================================================================
# Class Weights for Imbalanced Data
# =============================================================================

# Default class weights placeholder (uniform)
# Will be recomputed from actual data at runtime via compute_class_weights()
# Using uniform weights as placeholder since actual distribution depends on dataset
DEFAULT_CLASS_WEIGHTS = torch.ones(NUM_CLASSES)

def compute_class_weights(
    labels: torch.Tensor, 
    num_classes: int = NUM_CLASSES,
    method: str = 'sqrt',
    beta: float = 0.999,
) -> torch.Tensor:
    """
    Compute class weights from label distribution.
    
    Supports multiple weighting strategies to handle class imbalance:
    - 'sqrt': Square root of inverse frequency (default, tempered)
    - 'inverse': Raw inverse frequency (aggressive, can cause class collapse)
    - 'effective': Effective number of samples (Cui et al., 2019)
    
    Args:
        labels: Tensor of class labels
        num_classes: Number of classes
        method: Weighting method ('sqrt', 'inverse', 'effective')
        beta: Beta parameter for 'effective' method (0.9-0.9999)
        
    Returns:
        Tensor of class weights [num_classes], normalized to mean=1
    """
    # Count samples per class
    class_counts = torch.zeros(num_classes, device=labels.device)
    for c in range(num_classes):
        class_counts[c] = (labels == c).sum().float()
    
    # Avoid division by zero
    class_counts = class_counts.clamp(min=1.0)
    total_samples = class_counts.sum()
    
    if method == 'sqrt':
        # Square root inverse frequency - tempered, prevents ordinal collapse
        weights = torch.sqrt(total_samples / class_counts)
    elif method == 'inverse':
        # Raw inverse frequency - aggressive, can cause class collapse
        weights = total_samples / (num_classes * class_counts)
    elif method == 'effective':
        # Effective number of samples (Cui et al., 2019)
        # Smoothly interpolates between uniform and inverse frequency
        effective_num = 1.0 - torch.pow(beta, class_counts)
        weights = (1.0 - beta) / effective_num
    else:
        raise ValueError(f"Unknown weighting method: {method}. Use 'sqrt', 'inverse', or 'effective'")
    
    # Normalize so mean weight = 1 (for stable loss scale)
    weights = weights / weights.mean()
    
    return weights

# =============================================================================
# Data Split Configuration
# =============================================================================

# Fraction of data held out for final test evaluation
HOLDOUT_FRACTION = 0.2

# Number of cross-validation folds for hyperparameter tuning
# Using 5 folds for practicality (spec used 185, which is leave-one-out-like)
VALIDATION_FOLDS = 5

# Random seed for reproducibility (matches Neo4j GDS baseline)
RANDOM_SEED = 49

# =============================================================================
# Model Architecture Configuration
# =============================================================================

# Hidden layer dimension for all models
HIDDEN_DIM = 64

# Dropout rate for regularization
DROPOUT_RATE = 0.3  # Reduced from 0.5 for better gradient flow

# Number of attention heads for DepthAwareGAT
NUM_ATTENTION_HEADS = 4

# Default activation function name (used when activation is not explicitly specified)
# Supported: 'relu', 'elu', 'leaky_relu', 'gelu'
DEFAULT_ACTIVATION = 'relu'

# =============================================================================
# Training Configuration
# =============================================================================

# Maximum training epochs
MAX_EPOCHS = 100

# Minimum epochs before early stopping can trigger
MIN_EPOCHS = 50

# Early stopping patience (epochs without improvement)
PATIENCE = 75

# Learning rate for Adam optimizer
LEARNING_RATE = 0.01

# L2 regularization penalties for grid search
# Reduced for better gradient flow with censored learning
PENALTIES = [1e-4, 1e-3, 1e-2, 0.0625]

# =============================================================================
# Neo4j Output Configuration
# =============================================================================

# Property name for predicted root count
PREDICTION_PROPERTY = 'predictedRootCount'

# Property name for predicted probabilities (per class)
PROBABILITY_PROPERTY = 'predictedRootProbabilities'

# =============================================================================
# Neo4j Query Templates
# =============================================================================

# Query to fetch all Dnode nodes with features for root count prediction
# - wNum (polynomial degree) is the input feature
# - totalZero (root count) is the prediction target (label)
NODE_QUERY = """
MATCH (d:Dnode)-[:CreatedBye]->(c:CreatedBy)
RETURN elementId(d) as node_id,
       d.totalZero as label,
       c.wNum as wNum
ORDER BY node_id
"""

# Query to fetch all zMap edges (undirected, deduplicated)
EDGE_QUERY = """
MATCH (d1:Dnode)-[:zMap]-(d2:Dnode)
WHERE elementId(d1) < elementId(d2)
RETURN elementId(d1) as source, elementId(d2) as target
"""

# =============================================================================
# Filtered Queries for Spectral PE on Large Graphs
# =============================================================================

# Filtered node query: Only nodes where pArrayList values are in [PARRAY_MIN, PARRAY_MAX)
FILTERED_NODE_QUERY_TEMPLATE = """
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
WHERE all(x IN cb.pArrayList WHERE x >= {parray_min} AND x < {parray_max})
RETURN elementId(d) as node_id,
       d.totalZero as label,
       cb.wNum as wNum,
       cb.pArrayList as pArrayList
ORDER BY node_id
"""

# =============================================================================
# Censored / Truncated Root Observation Queries
# =============================================================================

# NOTE:
# - `RootList` is the list of ALL DISTINCT roots in the EXPANDED window (complete root count).
# - `determined` is computed on a larger/full scan window (separate completeness flag).
# - `vmResult` (coeff list) lives on the Dnode and is used to derive `degree_at_node`.
#
# We keep a `label` column for backward compatibility with existing training code.
# Here, `label` corresponds to the visible distinct root count (y_visible).
NODE_QUERY_CENSORED = """
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
RETURN elementId(d) as node_id,
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       d.n as n,
       d.d as mu_d,
       d.muList as muList,
       cb.wNum as wNum,
       d.windowMin as windowMin,
       d.windowMax as windowMax,
       d.windowSize as windowSize
ORDER BY node_id
"""

FILTERED_NODE_QUERY_CENSORED_TEMPLATE = """
MATCH (dd:Dnode)   
WITH dd
CALL {{
     WITH dd
     MATCH (dd)-[:CreatedBye]->(cb)   // and constrain d against all pArrays.
     WHERE all(x IN cb.pArrayList WHERE x >= 0 AND x < 6 )
     //WHERE ( all(x IN cb.pArrayList WHERE x >= -10 AND x < 10 ) AND (cb.wNum > 2))
     WITH dd AS d, cb.wNum AS wNum LIMIT 1
     // WITH DISTINCT d AS unique_D, cb.wNum as wNum
     RETURN d, wNum
     }}
WITH d, wNum
RETURN elementId(d) as node_id, 
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       d.n as n,
       d.d as mu_d,
       d.muList as muList,
       wNum
       //cb.pArrayList as pArrayList
//ORDER BY node_id
"""

# Filtered edge query: Only edges between filtered nodes
FILTERED_EDGE_QUERY_TEMPLATE = """
MATCH (d:Dnode)
WITH d
CALL {{
     WITH d
     MATCH (d)-[:CreatedBye]->(cb)   // and constrain d against all pArrays.
     WHERE all(x IN cb.pArrayList WHERE x >= 0 AND x < 6)
     //WHERE ( all(x IN cb.pArrayList WHERE x >= -10 AND x < 10 ) AND (cb.wNum > 2))
     WITH DISTINCT d AS unique_D
     RETURN unique_D
     }}
WITH unique_D
CALL {{
      WITH unique_D
      MATCH (unique_D)<-[:zMap]-(d1)
      RETURN d1
     }}
WITH unique_D, d1
RETURN elementId(d1) as source, elementId(unique_D) as target
"""

def get_node_query(use_filtering: bool = USE_GRAPH_FILTERING) -> str:
    """Get the appropriate node query based on filtering configuration."""
    if use_filtering:
        return FILTERED_NODE_QUERY_CENSORED_TEMPLATE.format(
            parray_min=PARRAY_MIN,
            parray_max=PARRAY_MAX
        )
    return NODE_QUERY_CENSORED

def get_edge_query(use_filtering: bool = USE_GRAPH_FILTERING) -> str:
    """Get the appropriate edge query based on filtering configuration."""
    if use_filtering:
        return FILTERED_EDGE_QUERY_TEMPLATE.format(
            parray_min=PARRAY_MIN,
            parray_max=PARRAY_MAX
        )
    return EDGE_QUERY

# =============================================================================
# Incremental wNum Learning Queries
# =============================================================================
#
# Parameterised variants of the censored filtered queries that accept a
# ``min_wnum`` threshold instead of the hardcoded ``cb.wNum > 2``.
# Used by the incremental-learning pipeline which trains from high to low
# wNum in a curriculum fashion.

INCREMENTAL_NODE_QUERY_TEMPLATE = """
MATCH (dd:Dnode)   
WITH dd
CALL {{
     WITH dd
     MATCH (dd)-[:CreatedBye]->(cb)
     WHERE ( all(x IN cb.pArrayList WHERE x >= -10 AND x < 10 ) AND (cb.wNum >= {min_wnum}))
     WITH dd AS d, cb.wNum AS wNum LIMIT 1
     RETURN d, wNum
     }}
WITH d, wNum
RETURN elementId(d) as node_id, 
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       d.n as n,
       d.d as mu_d,
       d.muList as muList,
       wNum
"""

INCREMENTAL_EDGE_QUERY_TEMPLATE = """
MATCH (d:Dnode)
WITH d
CALL {{
     WITH d
     MATCH (d)-[:CreatedBye]->(cb)
     WHERE ( all(x IN cb.pArrayList WHERE x >= -10 AND x < 10 ) AND (cb.wNum >= {min_wnum}))
     WITH DISTINCT d AS unique_D
     RETURN unique_D
     }}
WITH unique_D
CALL {{
      WITH unique_D
      MATCH (unique_D)<-[:zMap]-(d1)
      RETURN d1
     }}
WITH unique_D, d1
RETURN elementId(d1) as source, elementId(unique_D) as target
"""

# Discovery query: returns distinct wNum values and their node counts
# (subject to the same pArrayList constraint used by the filtered queries).
WNUM_DISCOVERY_QUERY = """
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
WHERE all(x IN cb.pArrayList WHERE x >= -10 AND x < 10)
WITH cb.wNum AS wNum, d
WITH wNum, count(DISTINCT d) AS nodeCount
RETURN wNum, nodeCount
ORDER BY wNum DESC
"""


def get_incremental_queries(min_wnum: int):
    """Return ``(node_query, edge_query)`` for an incremental wNum threshold.

    Args:
        min_wnum: Minimum wNum value to include (``cb.wNum >= min_wnum``).

    Returns:
        Tuple of ``(node_query_str, edge_query_str)`` ready for Neo4j.
    """
    node_q = INCREMENTAL_NODE_QUERY_TEMPLATE.format(min_wnum=min_wnum)
    edge_q = INCREMENTAL_EDGE_QUERY_TEMPLATE.format(min_wnum=min_wnum)
    return node_q, edge_q


# =============================================================================
# Sequence Prediction Queries (wNum=0 nodes with complete RootLists)
# =============================================================================
#
# Sequence prediction: wNum=0 nodes with complete RootLists
SEQUENCE_NODE_QUERY = """
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy {wNum:0})
WHERE size(d.RootList) = d.totalZero
RETURN elementId(d) as node_id,
       d.RootList as RootList,
       d.totalZero as totalZero,
       cb.pArrayList as pArrayList
ORDER BY node_id
"""

# Filtered version for large graphs
FILTERED_SEQUENCE_NODE_QUERY_TEMPLATE = """
MATCH (dd:Dnode) -[:CreatedBye]->(cb:CreatedBy {{wNum:0}})  
WITH dd
CALL {{
     WITH dd
     MATCH (dd)-[:CreatedBye]->(cb)
     WHERE  all(x IN cb.pArrayList WHERE x >= 0 AND x < 6) 
     WITH dd AS d LIMIT 1
     RETURN d
     }}
WITH d
RETURN elementId(d) as node_id, 
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       d.n as n,
       d.d as mu_d,
       d.muList as muList
"""

def get_sequence_node_query(use_filtering: bool = USE_GRAPH_FILTERING) -> str:
    """Get the appropriate sequence node query based on filtering configuration."""
    if use_filtering:
        return FILTERED_SEQUENCE_NODE_QUERY_TEMPLATE.format(
            parray_min=PARRAY_MIN,
            parray_max=PARRAY_MAX
        )
    return SEQUENCE_NODE_QUERY

# Query to write predictions back to Neo4j
WRITE_PREDICTION_QUERY = """
MATCH (d:Dnode)
WHERE elementId(d) = $node_id
SET d.predictedRootCount = $prediction,
    d.predictedRootProbabilities = $probabilities
"""

# Batch write query for efficiency
BATCH_WRITE_QUERY = """
UNWIND $predictions AS pred
MATCH (d:Dnode)
WHERE elementId(d) = pred.node_id
SET d.predictedRootCount = pred.prediction,
    d.predictedRootProbabilities = pred.probabilities
"""

