"""
Configuration constants for the Graph Link Prediction pipeline.

This module centralizes all hyperparameters, Neo4j queries, and utility functions
for the two-stage link prediction pipeline:
- Task 1: SAME_DENOMINATOR (rational partitioning)
- Task 2: NEXT_INTEGER (sequential ordering)
"""

import ast
from typing import Dict, List, Tuple, Optional
import torch

# =============================================================================
# Core Node Properties
# =============================================================================

# Properties used from Dnode nodes
NODE_PROPERTIES = ['muList', 'n', 'd', 'totalZero', 'determined', 'nBinary']

# Target edge types for prediction
EDGE_TYPE_SAME_DENOMINATOR = 'SAME_DENOMINATOR'
EDGE_TYPE_NEXT_INTEGER = 'NEXT_INTEGER'

# =============================================================================
# Feature Engineering Functions
# =============================================================================

def muList_to_integer(muList_str: str) -> int:
    """
    Convert muList string to integer using binary encoding.
    
    This implements the binary encoding rule (χᵢ) from Definition 2.2:
    For muList = "[i₁, i₂, ...]" where iₖ are zero positions,
    create a binary string with bits set at positions iₖ,
    then compute the positional value.
    
    Example:
        muList = "[0, 2, 4]"
        Binary positions 0, 2, 4 are set
        Binary string: 10101 (reading right to left)
        Integer: 1×2⁰ + 0×2¹ + 1×2² + 0×2³ + 1×2⁴ = 1 + 4 + 16 = 21
    
    Args:
        muList_str: String representation of list like "[0, 2, 4]"
        
    Returns:
        Integer value of the binary encoding
        
    Raises:
        ValueError: If muList_str cannot be parsed
    """
    if not muList_str or muList_str == '[]':
        return 0
    
    try:
        positions = ast.literal_eval(muList_str)
        if not isinstance(positions, list):
            positions = [positions]
        
        # Compute binary encoding: sum of 2^position
        integer_value = sum(2**int(pos) for pos in positions)
        return integer_value
    except (ValueError, SyntaxError) as e:
        raise ValueError(f"Cannot parse muList '{muList_str}': {e}")


def validate_rational_encoding(muList_str: str, n_val: int, d_val: int) -> bool:
    """
    Verify that n/d are correctly computed from muList.
    
    For the Set Union Ratio μ = n/d:
    - n = cardinality of the union
    - d = sum of individual set cardinalities
    
    Note: This is a placeholder validation. The actual computation requires
    implementing the full set collection decoder, which is complex.
    For now, we trust the values computed by ZerosAndDifferences.jar.
    
    Args:
        muList_str: String representation of root positions
        n_val: Numerator from database
        d_val: Denominator from database
        
    Returns:
        True if validation passes (currently always True)
    """
    if not muList_str or muList_str == '[]':
        return n_val == 0 and d_val == 0
    
    # Accept graph values as authoritative
    # TODO: Implement full set collection decoder for rigorous validation
    return True


def encode_partition_id(n: int, d: int) -> int:
    """
    Encode (n, d) rational tuple as a unique partition ID.
    
    Uses a simple encoding: id = n * 10000 + d
    Assumes d < 10000, which is reasonable for polynomial roots.
    
    Args:
        n: Numerator
        d: Denominator
        
    Returns:
        Unique integer partition ID
    """
    return n * 10000 + d


def decode_partition_id(partition_id: int) -> Tuple[int, int]:
    """
    Decode partition ID back to (n, d) tuple.
    
    Args:
        partition_id: Encoded partition ID
        
    Returns:
        Tuple of (n, d)
    """
    n = partition_id // 10000
    d = partition_id % 10000
    return n, d


# =============================================================================
# Task 1: SAME_DENOMINATOR Configuration
# =============================================================================

# Node features for Task 1 (rational partitioning)
# Using n, d, n/d, totalZero, wNum, len(muList), max(muList)
NUM_FEATURES_TASK1 = 7

# Number of output classes (will be determined from unique (n,d) pairs in data)
# This is a placeholder - actual value computed during data loading
NUM_PARTITIONS = None  # Set dynamically

# =============================================================================
# Task 2: NEXT_INTEGER Configuration
# =============================================================================

# Node features for Task 2 (sequential ordering)
# Using integer_value, len(muList), max(muList), sum(muList), totalZero
NUM_FEATURES_TASK2 = 5

# =============================================================================
# Data Split Configuration
# =============================================================================

# Fraction of edges held out for final test evaluation
HOLDOUT_FRACTION = 0.2

# Number of cross-validation folds for hyperparameter tuning
VALIDATION_FOLDS = 5

# Random seed for reproducibility
RANDOM_SEED = 49

# Negative sampling ratio for link prediction
# For each positive edge, sample this many negative edges
NEGATIVE_SAMPLE_RATIO = 1.0

# =============================================================================
# Model Architecture Configuration
# =============================================================================

# Hidden layer dimension for all models
HIDDEN_DIM = 64

# Embedding dimension for contrastive learning
EMBEDDING_DIM = 32

# Dropout rate for regularization
DROPOUT_RATE = 0.3

# Number of attention heads for GAT models
NUM_ATTENTION_HEADS = 4

# =============================================================================
# Training Configuration
# =============================================================================

# Maximum training epochs
MAX_EPOCHS = 100

# Minimum epochs before early stopping can trigger
MIN_EPOCHS = 10

# Early stopping patience (epochs without improvement)
PATIENCE = 10

# Learning rate for Adam optimizer
LEARNING_RATE = 0.01

# L2 regularization penalties for grid search
PENALTIES = [0.0625, 0.5, 1.0, 4.0]

# Contrastive learning temperature
CONTRASTIVE_TEMPERATURE = 0.5

# =============================================================================
# Neo4j Query Templates
# =============================================================================

# Query to fetch determined Dnode nodes with all required properties
NODE_QUERY = """
MATCH (d:Dnode)
WHERE d.determined = 1
RETURN elementId(d) as node_id,
       d.muList as muList,
       d.n as n,
       d.d as d,
       d.totalZero as totalZero,
       coalesce(d.nBinary, '') as nBinary
ORDER BY node_id
"""

# Query to fetch wNum from CreatedBy relationship
WNUM_QUERY = """
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
WHERE d.determined = 1
RETURN elementId(d) as node_id,
       cb.wNum as wNum
ORDER BY node_id
"""

# Query to fetch zMap edges between determined nodes
EDGE_QUERY = """
MATCH (d1:Dnode)-[:zMap]-(d2:Dnode)
WHERE d1.determined = 1 
  AND d2.determined = 1
  AND elementId(d1) < elementId(d2)
RETURN elementId(d1) as source, elementId(d2) as target
"""

# Query to generate SAME_DENOMINATOR ground truth edges
SAME_DENOMINATOR_GROUND_TRUTH_QUERY = """
MATCH (d1:Dnode), (d2:Dnode)
WHERE d1.determined = 1 
  AND d2.determined = 1
  AND d1.n = d2.n
  AND d1.d = d2.d
  AND elementId(d1) < elementId(d2)
RETURN elementId(d1) as source, 
       elementId(d2) as target,
       d1.n as n,
       d1.d as d
"""

# Query to generate NEXT_INTEGER ground truth edges
# Note: Requires intValue to be pre-computed and stored
NEXT_INTEGER_GROUND_TRUTH_QUERY = """
MATCH (d1:Dnode), (d2:Dnode)
WHERE d1.determined = 1 
  AND d2.determined = 1
  AND d1.n = d2.n
  AND d1.d = d2.d
  AND d1.intValue IS NOT NULL
  AND d2.intValue IS NOT NULL
  AND d1.intValue + 1 = d2.intValue
RETURN elementId(d1) as source,
       elementId(d2) as target,
       d1.intValue as src_int,
       d2.intValue as dst_int
"""

# Query to write intValue property to nodes
WRITE_INT_VALUE_QUERY = """
MATCH (d:Dnode)
WHERE d.determined = 1 AND elementId(d) = $node_id
SET d.intValue = $int_value
"""

# Batch write query for intValue
BATCH_WRITE_INT_VALUE_QUERY = """
UNWIND $nodes AS node
MATCH (d:Dnode)
WHERE elementId(d) = node.node_id
SET d.intValue = node.int_value
"""

# Query to write SAME_DENOMINATOR edges
WRITE_SAME_DENOMINATOR_EDGE_QUERY = """
MATCH (d1:Dnode), (d2:Dnode)
WHERE elementId(d1) = $source_id AND elementId(d2) = $target_id
MERGE (d1)-[:SAME_DENOMINATOR]->(d2)
"""

# Query to write NEXT_INTEGER edges
WRITE_NEXT_INTEGER_EDGE_QUERY = """
MATCH (d1:Dnode), (d2:Dnode)
WHERE elementId(d1) = $source_id AND elementId(d2) = $target_id
MERGE (d1)-[:NEXT_INTEGER]->(d2)
"""

# Batch write query for edges
BATCH_WRITE_EDGES_QUERY = """
UNWIND $edges AS edge
MATCH (d1:Dnode), (d2:Dnode)
WHERE elementId(d1) = edge.source_id AND elementId(d2) = edge.target_id
CALL apoc.create.relationship(d1, edge.edge_type, {}, d2) YIELD rel
RETURN count(rel) as edges_created
"""

# =============================================================================
# Evaluation Metrics Configuration
# =============================================================================

# Metrics for Task 1 (SAME_DENOMINATOR)
TASK1_METRICS = [
    'partition_purity',       # Fraction of predicted clusters that are pure
    'partition_completeness', # Fraction of true clusters fully recovered
    'adjusted_rand_index',    # Agreement with true partitioning
    'link_precision',         # Correct edges / predicted edges
    'link_recall',            # Correct edges / true edges
    'link_f1',                # Harmonic mean of precision and recall
]

# Target performance for Task 1
TASK1_TARGETS = {
    'partition_purity': 0.95,
    'partition_completeness': 0.90,
    'adjusted_rand_index': 0.90,
    'link_precision': 0.95,
    'link_recall': 0.90,
}

# Metrics for Task 2 (NEXT_INTEGER)
TASK2_METRICS = [
    'sequence_accuracy',  # Fraction of partitions with perfect ordering
    'kendalls_tau',       # Rank correlation within partitions
    'position_mae',       # Mean absolute error in predicted positions
    'link_precision',     # Correct edges / predicted edges
    'link_recall',        # Correct edges / true edges
    'link_f1',            # Harmonic mean of precision and recall
]

# Target performance for Task 2
TASK2_TARGETS = {
    'sequence_accuracy': 0.80,
    'kendalls_tau': 0.95,
    'position_mae': 2.0,
    'link_precision': 0.90,
    'link_recall': 0.85,
}

# =============================================================================
# Bijection Validation Properties
# =============================================================================

# Properties that must hold for a valid bijection
BIJECTION_PROPERTIES = [
    'uniqueness',              # Each node has at most one outgoing NEXT_INTEGER edge
    'partition_completeness',  # Nodes with same (n,d) are in same partition
    'sequential_ordering',     # NEXT_INTEGER edges connect consecutive integers
]

# =============================================================================
# Logging and Output Configuration
# =============================================================================

# Whether to print verbose training logs
VERBOSE = True

# How often to print training progress (in epochs)
LOG_INTERVAL = 10

# Directory for saving trained models
MODEL_SAVE_DIR = 'ml/graph_link_prediction/saved_models'

# Directory for saving evaluation results
RESULTS_SAVE_DIR = 'ml/graph_link_prediction/results'

# =============================================================================
# Device Configuration
# =============================================================================

# Auto-detect CUDA availability
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'

# =============================================================================
# Helper Functions
# =============================================================================

def get_muList_features(muList_str: str) -> Dict[str, float]:
    """
    Extract features from muList string.
    
    Args:
        muList_str: String like "[0, 2, 4]"
        
    Returns:
        Dictionary with features:
        - length: Number of positions
        - max_pos: Maximum position
        - sum_pos: Sum of positions
        - mean_pos: Mean position
    """
    if not muList_str or muList_str == '[]':
        return {
            'length': 0,
            'max_pos': 0,
            'sum_pos': 0,
            'mean_pos': 0.0,
        }
    
    try:
        positions = ast.literal_eval(muList_str)
        if not isinstance(positions, list):
            positions = [positions]
        
        positions = [int(p) for p in positions]
        
        return {
            'length': len(positions),
            'max_pos': max(positions) if positions else 0,
            'sum_pos': sum(positions),
            'mean_pos': sum(positions) / len(positions) if positions else 0.0,
        }
    except (ValueError, SyntaxError):
        return {
            'length': 0,
            'max_pos': 0,
            'sum_pos': 0,
            'mean_pos': 0.0,
        }


def compute_pairwise_features(
    node1: Dict,
    node2: Dict,
    task: str = 'task1'
) -> Dict[str, float]:
    """
    Compute pairwise features for link prediction.
    
    Args:
        node1: Dictionary with node properties
        node2: Dictionary with node properties
        task: Either 'task1' (SAME_DENOMINATOR) or 'task2' (NEXT_INTEGER)
        
    Returns:
        Dictionary of pairwise features
    """
    features = {}
    
    if task == 'task1':
        # Task 1: SAME_DENOMINATOR features
        features['n_match'] = float(node1['n'] == node2['n'])
        features['n_diff'] = abs(node1['n'] - node2['n'])
        features['d_match'] = float(node1['d'] == node2['d'])
        features['d_diff'] = abs(node1['d'] - node2['d'])
        
        # Rational value comparison
        ratio1 = node1['n'] / node1['d'] if node1['d'] != 0 else 0
        ratio2 = node2['n'] / node2['d'] if node2['d'] != 0 else 0
        features['rational_match'] = float(abs(ratio1 - ratio2) < 1e-6)
        features['rational_diff'] = abs(ratio1 - ratio2)
        
        # Root count comparison
        features['totalZero_diff'] = abs(node1.get('totalZero', 0) - node2.get('totalZero', 0))
        
    elif task == 'task2':
        # Task 2: NEXT_INTEGER features
        int1 = node1.get('int_value', 0)
        int2 = node2.get('int_value', 0)
        
        features['int_diff'] = int2 - int1
        features['is_consecutive'] = float(int2 - int1 == 1)
        features['same_partition'] = float(
            node1['n'] == node2['n'] and node1['d'] == node2['d']
        )
        
        # muList similarity (Jaccard)
        try:
            list1 = set(ast.literal_eval(node1['muList']))
            list2 = set(ast.literal_eval(node2['muList']))
            intersection = len(list1 & list2)
            union = len(list1 | list2)
            features['muList_jaccard'] = intersection / union if union > 0 else 0
        except:
            features['muList_jaccard'] = 0.0
    
    return features


# =============================================================================
# Export Configuration Summary
# =============================================================================

def print_config_summary():
    """Print a summary of the configuration."""
    print("=" * 70)
    print("Graph Link Prediction Configuration")
    print("=" * 70)
    print(f"\nTask 1 (SAME_DENOMINATOR):")
    print(f"  Features: {NUM_FEATURES_TASK1}")
    print(f"  Target metrics: {TASK1_TARGETS}")
    print(f"\nTask 2 (NEXT_INTEGER):")
    print(f"  Features: {NUM_FEATURES_TASK2}")
    print(f"  Target metrics: {TASK2_TARGETS}")
    print(f"\nTraining:")
    print(f"  Max epochs: {MAX_EPOCHS}")
    print(f"  Learning rate: {LEARNING_RATE}")
    print(f"  Penalties: {PENALTIES}")
    print(f"  Device: {DEVICE}")
    print(f"\nData Split:")
    print(f"  Holdout: {HOLDOUT_FRACTION}")
    print(f"  CV folds: {VALIDATION_FOLDS}")
    print(f"  Random seed: {RANDOM_SEED}")
    print("=" * 70)

