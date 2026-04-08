"""
Production Prediction System for Root Count Classification.

This package implements a complete multi-stage production system that:
- Uses a shared GAT backbone to preserve polynomial relationship geometry
- Employs CORAL ordinal regression as the primary safety layer
- Activates Focal classification for sharp predictions in stable regions
- Routes predictions based on uncertainty and regime detection
- Monitors drift via FAISS embedding indexing
- Provides k-NN explanations for uncertain predictions

Architecture Overview:

    Stage 1: Shared Backbone (DepthAwareGATv2)
        ↓ Produces embeddings h ∈ ℝ^d
    
    Stage 2: CORAL Head (Ordinal Regression)
        ↓ Class distribution + E[y] + uncertainty
    
    Stage 3: Focal Head (Sharp Classifier)
        ↓ Sharp predictions (gated by confidence)
    
    Stage 4: Uncertainty Router
        → Routes to: Defer | Use CORAL | Use Focal
    
    Vector Index Layer: FAISS
        → Regime detection, drift monitoring, explanation

Modules:
    - production_model: Multi-head model architecture and loss functions
    - production_trainer: Two-phase training pipeline
    - production_predictor: Complete prediction pipeline with routing
    - uncertainty_router: Confidence-based routing logic
    - regime_detector: FAISS indexing for regime detection and drift
    - calibration: ECE, selective prediction curves, threshold optimization

Quick Start:
    from python_model.production import train_production_model, ProductionPredictor
    
    # Train the model
    model, trainer, metrics = train_production_model(data, verbose=True)
    
    # Create predictor
    predictor = ProductionPredictor(
        model=model,
        regime_detector=trainer.regime_detector,
        router=trainer.router,
    )
    
    # Make predictions with routing
    output = predictor.predict(new_data)
    print(f"Deferred: {output.defer_mask.sum()} / {len(output.predictions)}")
"""

# Import key classes for easy access
from .production_model import (
    ProductionRootClassifier,
    DepthAwareGATv2Backbone,
    CORALHead,
    FocalHead,
    compute_coral_loss,
    compute_focal_loss,
    get_production_model,
)

from .production_trainer import (
    ProductionTrainer,
    TrainingConfig,
    train_production_model,
)

from .production_predictor import (
    ProductionPredictor,
    ProductionPredictionOutput,
    PredictionExplanation,
    create_predictor,
)

from .uncertainty_router import (
    UncertaintyRouter,
    RoutingConfig,
    RoutingDecision,
    RoutingOutput,
    calibrate_thresholds,
)

from .regime_detector import (
    RegimeDetector,
    RegimeInfo,
    RegimeDetectionOutput,
    compute_regime_boundaries,
)

from .calibration import (
    compute_ece,
    compute_mce,
    compute_brier_score,
    compute_calibration_metrics,
    selective_accuracy_curve,
    compute_selective_metrics,
    find_optimal_thresholds,
    temperature_scaling,
    CalibrationMetrics,
    SelectivePredictionMetrics,
)


__all__ = [
    # Main classes
    'ProductionRootClassifier',
    'ProductionTrainer',
    'ProductionPredictor',
    'UncertaintyRouter',
    'RegimeDetector',
    
    # Model components
    'DepthAwareGATv2Backbone',
    'CORALHead',
    'FocalHead',
    
    # Training
    'TrainingConfig',
    'train_production_model',
    
    # Prediction
    'ProductionPredictionOutput',
    'PredictionExplanation',
    'create_predictor',
    
    # Routing
    'RoutingConfig',
    'RoutingDecision',
    'RoutingOutput',
    'calibrate_thresholds',
    
    # Regime detection
    'RegimeInfo',
    'RegimeDetectionOutput',
    'compute_regime_boundaries',
    
    # Calibration
    'compute_ece',
    'compute_mce',
    'compute_brier_score',
    'compute_calibration_metrics',
    'selective_accuracy_curve',
    'compute_selective_metrics',
    'find_optimal_thresholds',
    'temperature_scaling',
    'CalibrationMetrics',
    'SelectivePredictionMetrics',
    
    # Loss functions
    'compute_coral_loss',
    'compute_focal_loss',
    
    # Factory functions
    'get_production_model',
]
