"""
Core Model Components

This package contains the core implementation modules for root count prediction:
    - config: Configuration and hyperparameters
    - data_loader: Neo4j to PyTorch Geometric data conversion
    - models: Neural network architectures (MLP, GCN, GraphSAGE, GAT)
    - models_improved: Improved model architectures with advanced features
    - trainer: Training infrastructure and evaluation
    - predictor: Inference and Neo4j write-back
    - edge_features: Edge feature computation for graph models
    - coefficient_features: Polynomial coefficient feature extraction
    - binary_determined_loader: Binary classification data loader
    - experiment: Experiment orchestration (ExperimentConfig, ExperimentRunner, ExperimentReport)

Sequence prediction pipeline:
    - sequence_config: SequencePredictorConfig and RootListTokenizer
    - sequence_data_loader: SequenceDataLoader and SequenceDataset
    - sequence_models: NodeEncoder, SequenceEncoder, RootListDecoder, SequencePredictor
    - sequence_trainer: Training, inference, and evaluation functions
"""

# Export all modules for easy import
__all__ = [
    'config',
    'data_loader',
    'models',
    'models_improved',
    'trainer',
    'predictor',
    'edge_features',
    'coefficient_features',
    'binary_determined_loader',
    'experiment',
    'sequence_config',
    'sequence_data_loader',
    'sequence_models',
    'sequence_trainer',
]
