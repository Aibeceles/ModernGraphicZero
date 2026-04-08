# GAT Collapse: Causes and Solutions

## The Problem

Your DepthAwareGAT collapses to predicting class 1 for all nodes, achieving only 49% accuracy (the majority class proportion). This is a well-known failure mode in GNNs.

## Root Causes

### 1. Over-Smoothing (The Main Culprit)

Each GAT layer averages neighbor representations:
```
h_i^(l+1) = Σ α_ij · W · h_j^(l)
```

After 3 layers, all nodes converge to similar embeddings because:
- Layer 1: Nodes mix with 1-hop neighbors
- Layer 2: Nodes mix with 2-hop neighbors  
- Layer 3: Most nodes share representation information

**Your model has 3 GAT layers** - this is likely too many for your graph structure.

### 2. Class Imbalance + Loss Landscape

| Class | Samples | Proportion |
|-------|---------|------------|
| 0 | 149 | 19.7% |
| 1 | 369 | **48.9%** |
| 2 | 152 | 20.1% |
| 3 | 70 | 9.3% |
| 4 | 15 | 2.0% |

The loss landscape has a "valley" at "predict all class 1":
- Immediate 49% accuracy with no learning
- Weak gradients make it hard to escape
- Early stopping triggers before escaping

### 3. Attention Collapse

When the model can't find discriminative patterns early, attention weights become uniform:
```
α_ij ≈ 1/|N(i)| for all j ∈ N(i)
```
This degenerates GAT to a simple mean aggregator.

### 4. Non-Ordinal Class Structure

Your data has a **hierarchical** structure, not ordinal:

```
determined=0: rootCount ∈ {0, 1}  ←→  vm4 separates perfectly
determined=1: rootCount ∈ {1, 2, 3, 4}  ←→  different vm4 patterns
```

CORAL and EMD loss assume 0 < 1 < 2 < 3 < 4 in feature space, but:
- Class 3 (determined=1, vm4 < 0) is **farther** from class 1 (determined=0) than from class 4

## Solutions

### Solution 1: DepthAwareGATv2 (Skip + JKNet)

**Best for**: General improvement without changing training

```python
model = DepthAwareGATv2(use_skip=True, use_jk=True)
```

Features:
- **Skip connections**: `h_out = GAT(h_in) + h_in` preserves original signal
- **Jumping Knowledge**: Concatenates all layer outputs, letting classifier choose
- **Only 2 layers**: Reduces over-smoothing
- **LayerNorm**: Stabilizes training

### Solution 2: DropEdgeGAT

**Best for**: Dense graphs with high connectivity

```python
model = DropEdgeGAT(edge_drop_rate=0.2)
```

Randomly drops 20% of edges during training:
- Reduces information flow
- Acts like structural dropout
- Prevents node representations from converging

### Solution 3: HierarchicalClassifier

**Best for**: Your specific data structure

```python
model = HierarchicalClassifier()
det_logprobs, root_logprobs = model(x, edge_index, determined_labels)
```

Two-stage classification:
1. Predict `determined` (0 or 1)
2. Route to specialized head:
   - determined=0 → predict {0, 1}
   - determined=1 → predict {1, 2, 3, 4}

This matches your data's actual structure!

### Solution 4: FocalLossClassifier

**Best for**: Class imbalance

```python
model = FocalLossClassifier(focal_gamma=2.0)
loss = model.focal_loss(logits, targets, alpha=class_weights)
```

Focal loss: `FL = -α(1-p)^γ · log(p)`

- Down-weights easy examples (majority class correctly classified)
- Forces model to focus on minority classes
- `γ=2` is typical; try `γ=3-5` for extreme imbalance

### Solution 5: RegressionWarmstartClassifier

**Best for**: Leveraging your working regression model

```python
# First train regression (which works)
reg_model = RegressionClassifier()
train(reg_model, ...)

# Then transfer to classifier
model = RegressionWarmstartClassifier()
model.load_from_regression(reg_model)
# Fine-tune with classification loss
```

This gives the classifier a good initialization in feature space.

## Recommended Approach

Based on your data analysis, I recommend:

### Primary: HierarchicalClassifier

Your data has clear hierarchical structure:
- `determined` is almost perfectly predictable from features
- Once you know `determined`, the classification problem becomes much easier

### Secondary: DepthAwareGATv2 + Focal Loss

If you want a single end-to-end model:
```python
model = DepthAwareGATv2(use_skip=True, use_jk=True)
# Train with focal loss and aggressive class weights
class_weights = torch.tensor([3.0, 1.0, 3.0, 5.0, 10.0])
```

### Diagnostic: Compare Embedding Distributions

To verify over-smoothing, add this after forward pass:
```python
# Check if embeddings are collapsing
h = model.get_embeddings(x, edge_index)  # Add this method
h_mean = h.mean(dim=0)
h_std = h.std(dim=0)
print(f"Embedding std: {h_std.mean():.4f}")  # Should be > 0.1
```

If std is very small (< 0.01), embeddings have collapsed.

## Summary Table

| Model | Fixes | Best For |
|-------|-------|----------|
| DepthAwareGATv2 | Over-smoothing | General use |
| DropEdgeGAT | Over-smoothing | Dense graphs |
| HierarchicalClassifier | Data structure mismatch | Your specific data |
| FocalLossClassifier | Class imbalance | Minority class focus |
| RegressionWarmstart | Initialization | Leveraging regression |

## Quick Experiment

Try this minimal change first:
```python
# In your current DepthAwareGAT, comment out conv3:
# h = self.conv3(h, edge_index, edge_attr=edge_attr)  # Skip this

# Add skip connection:
h = self.conv2(h, edge_index, edge_attr=edge_attr) + h_after_conv1
```

This alone may prevent collapse.
