"""
Graph Label Prediction - Python Model Package

This package contains the core implementation modules for root count prediction:
    - core: Core model components (config, data_loader, models, trainer, predictor, etc.)
    - features: Centralized feature inventory (node and edge feature registries)
    - production: Complete multi-stage prediction system with:
        * Shared GAT backbone + dual heads (CORAL ordinal + Focal sharp)
        * Uncertainty-based routing (defer/ordinal/focal decisions)
        * FAISS regime detection and drift monitoring
        * Calibrated confidence thresholds and explanation via k-NN
        * Two-phase training pipeline
    - tests: Test modules for core components
"""

# Import from core subfolder for backward compatibility
from .core import (
    config,
    data_loader,
    models,
    models_improved,
    trainer,
    predictor,
    edge_features,
    coefficient_features,
    binary_determined_loader,
)

__all__ = [
    # Core modules (re-exported from core/)
    'config',
    'data_loader',
    'models',
    'trainer',
    'predictor',
    'edge_features',
    'coefficient_features',
    'binary_determined_loader',
    'models_improved',
    # Subpackages
    'core',
    'features',
    'production',
    'tests',
]

