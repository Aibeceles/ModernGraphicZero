"""
Graph Link Prediction Pipeline for Polynomial Difference Graphs.

This module implements a two-stage link prediction pipeline for discovering
the integer-rational bijection implicit in polynomial root patterns:

Task 1 (SAME_DENOMINATOR): Partition nodes by their rational value μ = n/d
Task 2 (NEXT_INTEGER): Order nodes by consecutive integer values within partitions

The module provides three approaches for Task 1:
- Classification: Predict partition labels directly
- Pairwise: Binary classifier for edge existence
- Contrastive: Learn embeddings via contrastive loss

And two approaches for Task 2:
- Regression: Predict integer values, then order
- Pairwise: Binary classifier for consecutive edges

Example:
    >>> from ml.graph_link_prediction import load_graph_from_neo4j, TwoStageLinkPredictionPipeline
    >>> data_t1, data_t2, loader = load_graph_from_neo4j(
    ...     "bolt://localhost:7687", "neo4j", "password", "d4seed1"
    ... )
    >>> pipeline = TwoStageLinkPredictionPipeline(data_t1, data_t2)
    >>> results = pipeline.run(task1_approach='classifier', task2_approach='regressor')
"""

# Configuration
from .config import (
    muList_to_integer,
    validate_rational_encoding,
    encode_partition_id,
    decode_partition_id,
    NUM_FEATURES_TASK1,
    NUM_FEATURES_TASK2,
    TASK1_TARGETS,
    TASK2_TARGETS,
)

# Data loading
from .data_loader import (
    LinkPredictionDataLoader,
    load_graph_from_neo4j,
    split_edges_train_test,
    generate_negative_samples,
)

# Task 1 models
from .task1_models import (
    RationalPartitionClassifier,
    SameDenominatorLinkPredictor,
    ContrastiveEmbedder,
    ContrastiveLoss,
    get_task1_model,
)

# Task 2 models
from .task2_models import (
    IntegerValuePredictor,
    NextIntegerLinkPredictor,
    get_task2_model,
)

# Trainers
from .task1_trainer import Task1Trainer
from .task2_trainer import Task2Trainer

# Pipeline
from .pipeline import (
    TwoStageLinkPredictionPipeline,
    run_complete_pipeline,
)

# Evaluation
from .evaluation import (
    evaluate_partition_quality,
    evaluate_link_prediction,
    evaluate_sequence_quality,
    evaluate_task1_full,
    evaluate_task2_full,
    validate_bijection_properties,
    generate_evaluation_report,
    print_evaluation_summary,
)

# Predictor
from .predictor import (
    LinkPredictor,
    save_model,
    load_model,
)

__version__ = "1.0.0"

__all__ = [
    # Configuration
    "muList_to_integer",
    "validate_rational_encoding",
    "encode_partition_id",
    "decode_partition_id",
    "NUM_FEATURES_TASK1",
    "NUM_FEATURES_TASK2",
    "TASK1_TARGETS",
    "TASK2_TARGETS",
    # Data loading
    "LinkPredictionDataLoader",
    "load_graph_from_neo4j",
    "split_edges_train_test",
    "generate_negative_samples",
    # Task 1 models
    "RationalPartitionClassifier",
    "SameDenominatorLinkPredictor",
    "ContrastiveEmbedder",
    "ContrastiveLoss",
    "get_task1_model",
    # Task 2 models
    "IntegerValuePredictor",
    "NextIntegerLinkPredictor",
    "get_task2_model",
    # Trainers
    "Task1Trainer",
    "Task2Trainer",
    # Pipeline
    "TwoStageLinkPredictionPipeline",
    "run_complete_pipeline",
    # Evaluation
    "evaluate_partition_quality",
    "evaluate_link_prediction",
    "evaluate_sequence_quality",
    "evaluate_task1_full",
    "evaluate_task2_full",
    "validate_bijection_properties",
    "generate_evaluation_report",
    "print_evaluation_summary",
    # Predictor
    "LinkPredictor",
    "save_model",
    "load_model",
]

