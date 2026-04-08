"""
Graph Label Prediction - Root Count Classification Pipeline

This module implements a multi-class node classification pipeline for predicting 
the number of rational roots (totalZero) for polynomial nodes (Dnode).

IMPROVED VERSION features:
    - Spectral positional encodings for global graph structure awareness
    - Depth-aware attention (DepthAwareGAT) for hierarchical tree structure
    - Class-weighted loss for handling imbalanced root count distribution
    - Macro F1 evaluation for proper assessment on imbalanced data

Components:
    - config: Hyperparameters, class weights, and configuration constants
    - data_loader: Neo4j to PyTorch Geometric conversion with spectral PE
    - models: MLP, GCN, GraphSAGE, and DepthAwareGAT classifiers
    - trainer: Training with class weights, macro F1 early stopping
    - predictor: Inference and Neo4j write-back functionality
"""

# Import the config module itself to make it accessible as graph_label_prediction.config
from .python_model.core import config

# Also import commonly used constants for convenience
from .python_model.core.config import (
    BASE_FEATURES,
    TARGET_PROPERTY,
    TARGET_MODE,
    LOSS_TYPE,
    HOLDOUT_FRACTION,
    VALIDATION_FOLDS,
    RANDOM_SEED,
    PENALTIES,
    HIDDEN_DIM,
    NUM_CLASSES,
    NUM_FEATURES,
    NUM_BASE_FEATURES,
    DROPOUT_RATE,
    MAX_EPOCHS,
    LEARNING_RATE,
    SPECTRAL_PE_DIM,
    NUM_ATTENTION_HEADS,
    USE_MINIBATCH,
    BATCH_SIZE,
    NUM_NEIGHBORS,
    USE_WEIGHTED_SAMPLER,
    compute_class_weights,
)

__all__ = [
    "BASE_FEATURES",
    "TARGET_PROPERTY",
    "TARGET_MODE",
    "LOSS_TYPE",
    "HOLDOUT_FRACTION",
    "VALIDATION_FOLDS",
    "RANDOM_SEED",
    "PENALTIES",
    "HIDDEN_DIM",
    "NUM_CLASSES",
    "NUM_FEATURES",
    "NUM_BASE_FEATURES",
    "DROPOUT_RATE",
    "MAX_EPOCHS",
    "LEARNING_RATE",
    "SPECTRAL_PE_DIM",
    "NUM_ATTENTION_HEADS",
    "USE_MINIBATCH",
    "BATCH_SIZE",
    "NUM_NEIGHBORS",
    "USE_WEIGHTED_SAMPLER",
    "compute_class_weights",
]

