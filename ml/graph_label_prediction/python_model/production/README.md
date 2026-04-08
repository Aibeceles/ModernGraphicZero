# Production Multi-Stage Root Count Prediction System

A complete production-ready prediction system that goes beyond "deploying a model" to deploying a **decision system** with:

- **Uncertainty-aware routing** between ordinal and sharp predictions
- **Regime detection** via FAISS vector indexing
- **Drift monitoring** to flag out-of-distribution samples
- **Calibrated confidence** thresholds
- **Explainability** through k-nearest neighbors

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Stage 1: Shared Backbone (DepthAwareGATv2)                  │
│   • 2-layer GAT with skip connections + JK concatenation    │
│   • Preserves sub-arrow geometry                            │
│   • Output: embeddings h ∈ ℝ^d                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        ▼                           ▼
┌───────────────────┐     ┌─────────────────────┐
│ Stage 2: CORAL    │     │ Stage 3: Focal      │
│ (Ordinal Head)    │     │ (Sharp Classifier)  │
│                   │     │                     │
│ • K-1 thresholds  │     │ • Linear head       │
│ • E[y], probs     │     │ • Focal loss γ=2.0  │
│ • Safety layer    │     │ • Gated by conf.    │
└─────────┬─────────┘     └──────────┬──────────┘
          │                          │
          └──────────┬───────────────┘
                     ▼
        ┌────────────────────────────┐
        │ Stage 4: Uncertainty Router│
        │                            │
        │  Rules:                    │
        │  1. max_prob < 0.4 → Defer │
        │  2. |E[y]-round|<0.15→CORAL│
        │  3. High conf + stable→Focal│
        └────────────┬───────────────┘
                     ▼
              ┌──────────────┐
              │ Final Output │
              └──────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Vector Index Layer: FAISS                                    │
│   • Regime detection (nearest centroid)                     │
│   • Drift monitoring (k-NN distance)                        │
│   • Explanation (k-NN retrieval)                            │
└─────────────────────────────────────────────────────────────┘
```

## File Structure

| File | Description |
|------|-------------|
| `production_model.py` | Multi-head model with shared backbone, CORAL + Focal heads |
| `production_trainer.py` | Two-phase training (CORAL → frozen-backbone Focal) |
| `production_predictor.py` | Full prediction pipeline with routing |
| `uncertainty_router.py` | Confidence-based routing logic |
| `regime_detector.py` | FAISS indexing, drift detection, k-NN explanation |
| `calibration.py` | ECE, selective prediction curves, threshold optimization |

## Quick Start

### Training

```python
from python_model.production import train_production_model

# Train both phases + fit regime detector + calibrate thresholds
model, trainer, metrics = train_production_model(data, verbose=True)

print(f"Phase 1 (CORAL) - Test F1: {metrics['phase1']['test_macro_f1']:.4f}")
print(f"Phase 2 (Focal) - Test F1: {metrics['phase2']['test_macro_f1']:.4f}")
```

### Prediction

```python
from python_model.production import ProductionPredictor

# Create predictor with full routing
predictor = ProductionPredictor(
    model=model,
    regime_detector=trainer.regime_detector,
    router=trainer.router,
)

# Make predictions
output = predictor.predict(new_data)

# Routing statistics
print(f"Total: {len(output.predictions)}")
print(f"Deferred: {output.defer_mask.sum()} ({output.defer_mask.float().mean()*100:.1f}%)")
print(f"Used CORAL: {output.ordinal_mask.sum()} ({output.ordinal_mask.float().mean()*100:.1f}%)")
print(f"Used Focal: {output.focal_mask.sum()} ({output.focal_mask.float().mean()*100:.1f}%)")

# Get explanations for uncertain predictions
explanations = predictor.explain_predictions(new_data, output)
for exp in explanations[:5]:
    print(f"\nNode {exp.node_id}:")
    print(f"  Prediction: {exp.prediction} (confidence: {exp.confidence:.3f})")
    print(f"  Decision: {exp.decision}")
    print(f"  Top neighbor: class {exp.neighbors[0]['label']} @ distance {exp.neighbors[0]['distance']:.3f}")
```

## Key Design Decisions

### Why CORAL + Focal?

- **CORAL** respects ordinal structure and provides calibrated uncertainty
- **Focal** achieves sharp decisions in stable regions
- **Routing** gives you the best of both without their weaknesses

### Why Two-Phase Training?

**Phase 1: Backbone + CORAL**
- Goal: Learn embeddings that preserve polynomial relationship geometry
- No aggressive focal pressure that could collapse ordinal structure

**Phase 2: Focal only (frozen backbone)**
- Goal: Learn sharp decisions without destroying embeddings
- Focal head can focus on stable regions

### Why FAISS Vector Indexing?

Not for prediction (this is parametric, not retrieval).

**Use cases:**
1. **Regime detection** - Is this node near known stable regions?
2. **Drift monitoring** - Is this far from indexed manifold?
3. **Explanation** - Show similar nodes and their labels

Distance to regime centroid becomes a confidence signal for routing.

## Production Thresholds (Default)

Based on typical ordinal regression calibration:

| Threshold | Default | Meaning |
|-----------|---------|---------|
| `defer_threshold` | 0.4 | Below 40% confidence → flag for review |
| `ordinal_margin` | 0.15 | \|E[y] - round(E[y])\| < 0.15 → trust ordinal |
| `focal_confidence` | 0.7 | Only use focal when 70%+ confident |
| `regime_distance` | 2.0 | Max cosine distance to trust focal |
| `drift_threshold` | 1.5 | Mean k-NN distance for OOD detection |

**Calibrate on validation set:**

```python
from python_model.production import calibrate_thresholds

routing_config = calibrate_thresholds(
    coral_probs=val_coral_probs,
    coral_expected=val_expected,
    labels=val_labels,
    target_defer_rate=0.05,  # 5% flagged
    target_accuracy=0.95,    # 95% accuracy on non-deferred
)
```

## Monitoring in Production

```python
# Get prediction statistics
stats = predictor.get_prediction_stats(output)
print(f"Mean confidence: {stats['mean_confidence']:.3f}")
print(f"Drift detected: {stats['num_drift']} nodes")

# Compare CORAL vs Focal
comparison = predictor.compare_heads(output, labels=true_labels)
print(f"CORAL accuracy: {comparison['coral_accuracy']:.3f}")
print(f"Focal accuracy: {comparison['focal_accuracy']:.3f}")
print(f"Agreement rate: {comparison['agreement_rate']:.3f}")
```

## Dependencies

- PyTorch + PyTorch Geometric (backbone)
- FAISS (`pip install faiss-cpu` or `faiss-gpu`) (regime detection)
- scikit-learn (metrics, calibration)

## Why This Matters

In production, you don't deploy "a model" - you deploy a **decision system**.

This system:
- **Knows when it doesn't know** (defer low-confidence)
- **Handles regime shifts gracefully** (ordinal safety layer)
- **Accelerates when safe** (focal in stable regions)
- **Detects drift** (FAISS monitoring)
- **Explains itself** (k-NN neighbors)

Maximum in-distribution F1 ≠ production-optimal.

Production-optimal = **predictable errors + explicit uncertainty + robustness to regime shifts**.
