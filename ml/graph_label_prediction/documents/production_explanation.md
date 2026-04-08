# Production System Explanation

## Overview

The `production.ipynb` notebook implements a **production-grade ML inference system** for predicting the number of rational roots (totalZero) of polynomial nodes in a mathematical graph database. This document explains the system architecture, prediction targets, and the rationale for separating production from experimentation pipelines.

---

## What Is Being Predicted

**Target Variable:** `totalZero` — the count of rational roots for each polynomial node (`Dnode`)

**Prediction Type:** Multi-class classification (ordinal)
- **Class 0:** 0 rational roots
- **Class 1:** 1 rational root
- **Class 2:** 2 rational roots
- **Class 3:** 3 rational roots
- **Class 4:** 4 rational roots

**Input Features (24-dimensional):**
- 3 base features: `wNum`, `degree`, `determined`
- 5 coefficient features: polynomial coefficients
- 5 statistical features: derived statistics
- 3 set union features: `n`, `d`, `mu_ratio`
- 8 spectral positional encodings: graph structure awareness

---

## Production System Architecture

### Multi-Stage Inference Pipeline

The production system implements a **multi-head architecture** with uncertainty-aware routing:

```
                    ┌─────────────────────────┐
                    │   Input Graph Data      │
                    │   (Node Features +      │
                    │    Edge Structure)      │
                    └───────────┬─────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │   Shared Backbone       │
                    │   (DepthAwareGATv2)     │
                    │   - Preserves geometry  │
                    │   - Skip connections    │
                    │   - Jumping Knowledge   │
                    └───────────┬─────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   Learned Embeddings  │
                    └───────────┬───────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
          ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   CORAL Head    │   │   Focal Head    │   │ Regime Detector │
│  (Ordinal       │   │  (Sharp         │   │ (OOD Detection) │
│   Regression)   │   │   Classification)│   │                 │
└────────┬────────┘   └────────┬────────┘   └────────┬────────┘
         │                     │                     │
         └──────────┬──────────┘                     │
                    │                                │
                    ▼                                ▼
          ┌─────────────────┐              ┌─────────────────┐
          │ Uncertainty     │              │ Drift Monitor   │
          │ Router          │◄─────────────│                 │
          └────────┬────────┘              └─────────────────┘
                   │
                   ▼
          ┌─────────────────┐
          │ Final Prediction│
          │ + Confidence    │
          │ + Explanation   │
          └─────────────────┘
```

### Key MLOps Components

#### 1. Model Serving Layer
- **Shared Backbone:** DepthAwareGATv2 with skip connections and Jumping Knowledge (JKNet) to prevent over-smoothing in deep GNN layers
- **Dual Heads:** CORAL for ordinal stability, Focal for high-confidence sharp predictions
- **Inference Routing:** Selects prediction head based on confidence thresholds

#### 2. Uncertainty Quantification
- **Prediction Confidence:** Softmax probabilities calibrated on validation set
- **Deferral Mechanism:** Low-confidence predictions are flagged for human review
- **Expected Calibration Error (ECE):** Measures reliability of confidence scores

#### 3. Data Drift Detection
- **FAISS Vector Indexing:** Efficient nearest-neighbor search on embeddings
- **Regime Detection:** Identifies distribution shifts via centroid distance
- **k-NN OOD Detection:** Flags out-of-distribution inputs based on embedding distance

#### 4. Monitoring & Observability
- **Calibration Curves:** Track prediction reliability over time
- **Selective Prediction:** Coverage vs. accuracy trade-off curves
- **Threshold Optimization:** Automated confidence threshold tuning

#### 5. Explainability
- **k-NN Retrieval:** Returns similar historical nodes for each prediction
- **Decision Explanations:** Human-readable reasoning with confidence scores

---

## Why Production Is Separate from `run_pipeline.ipynb`

### Separation of Concerns (MLOps Best Practice)

| Aspect | `run_pipeline.ipynb` | `production.ipynb` |
|--------|---------------------|-------------------|
| **Purpose** | Experimentation & Model Selection | Deployment & Inference |
| **Focus** | Compare architectures, hyperparameter tuning | Reliable predictions at scale |
| **Output** | Metrics, visualizations, insights | Predictions written to Neo4j |
| **Reproducibility** | May vary between runs | Deterministic inference |
| **Error Handling** | Minimal (fail fast) | Robust (graceful degradation) |

### Technical Rationale

1. **Model Selection vs. Model Serving**
   - `run_pipeline.ipynb` trains and evaluates multiple models (DepthAwareGAT, DepthAwareGATv2, FocalLoss, CORAL) to find the best architecture
   - `production.ipynb` deploys the selected architecture with production hardening

2. **Experimentation Overhead**
   - Training notebooks include visualization, debugging, and comparison code
   - Production removes this overhead for faster, leaner inference

3. **Uncertainty Handling**
   - Training focuses on maximizing accuracy metrics (Macro F1, Balanced Accuracy)
   - Production adds uncertainty routing, deferral, and OOD detection

4. **Feature Engineering vs. Feature Serving**
   - `run_pipeline.ipynb` may prototype new features
   - `production.ipynb` uses a frozen feature set for consistency

5. **Database Writes**
   - Training notebooks read-only (no side effects)
   - Production writes predictions back to Neo4j (`predict_and_write`)

6. **Monitoring Requirements**
   - Experimentation doesn't need drift detection
   - Production requires ongoing calibration and regime monitoring

---

## Production Workflow Summary

```
1. Load Environment & Connect to Neo4j
   └─> Environment variables, database connection

2. Load Graph Data
   └─> 24D features (base + coefficients + statistics + spectral PE)

3. Train Production Model (Two-Phase)
   └─> Phase 1: CORAL ordinal regression (stable baseline)
   └─> Phase 2: Focal classification (sharp predictions)

4. Fit Regime Detector
   └─> FAISS indexing of training embeddings
   └─> Centroid computation for regime detection

5. Calibrate Thresholds
   └─> Optimize confidence thresholds on validation set
   └─> Compute ECE and selective prediction curves

6. Generate Predictions
   └─> Route through uncertainty-aware system
   └─> Attach explanations and confidence scores

7. Write to Neo4j
   └─> Persist predictions for downstream consumers

8. Monitor
   └─> Track calibration, drift, and coverage metrics
```

---

## Metrics Reference

| Metric | Definition | Production Use |
|--------|------------|----------------|
| **Macro F1** | Unweighted average F1 across classes | Primary evaluation (treats minority classes equally) |
| **Balanced Accuracy** | Average recall per class | Confirms class-balanced performance |
| **MAE** | Mean Absolute Error in root count | Measures ordinal prediction quality |
| **ECE** | Expected Calibration Error | Confidence reliability |
| **Coverage** | % of predictions not deferred | Throughput metric |

---

## Configuration Reference

Key hyperparameters (from `config.py`):

```python
# Feature dimensions
NUM_BASE_FEATURES = 3
NUM_COEFFICIENT_FEATURES = 5
NUM_STATISTICAL_FEATURES = 5
NUM_SET_UNION_FEATURES = 3
SPECTRAL_PE_DIM = 8

# Model architecture
NUM_ATTENTION_HEADS = 4
HIDDEN_DIM = 64

# Training
HOLDOUT_FRACTION = 0.2
MAX_NODES_FOR_SPECTRAL_PE = 100_000

# Filtering
USE_GRAPH_FILTERING = True
PARRAY_MIN = 0
PARRAY_MAX = 5
```

---

## Summary

The production system transforms experimental model training into a **reliable, observable, and explainable** inference pipeline. By separating concerns, we ensure that:

- **Experiments** can iterate rapidly without production constraints
- **Production** delivers consistent, monitored predictions with uncertainty quantification
- **MLOps practices** (drift detection, calibration, routing) are applied only where needed

This architecture enables confident deployment of GNN-based root count predictions while maintaining the flexibility to improve models through continued experimentation.
