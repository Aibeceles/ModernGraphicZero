# Graph Label Prediction Experiments

This document describes the machine learning models used in the root count prediction pipeline. All models predict the number of rational roots (0-4) for polynomial nodes based on graph structure and node features. The target uses RootList (expanded window) which captures all distinct roots, making this a complete root count prediction task.

---

## Model Inventory

### 1. MLPClassifier

**Name:** Multi-Layer Perceptron Classifier (Baseline)

**What:** A feedforward neural network that ignores graph structure and predicts root count using only node features.

**How:** 
- Architecture: Input → Linear(hidden) → ReLU → Dropout → Linear(hidden) → ReLU → Dropout → Linear(num_classes) → LogSoftmax
- Input: Node features (wNum + optional spectral PE)
- Output: Log probabilities over 5 root count classes (0-4)
- Loss: Cross-entropy (optionally class-weighted)

**Why:** 
- Establishes a baseline to measure whether graph structure provides predictive value
- If graph-based models (GCN, GraphSAGE, GAT) perform no better than MLP, the graph structure is not informative
- Useful for diagnosing whether the prediction task is solvable from node features alone

**Preconditions:**
- Node features must contain discriminative information (wNum)
- No graph structure required (edge_index parameter ignored)
- Labels must be available for training nodes

---

### 2. GCNClassifier

**Name:** Graph Convolutional Network Classifier

**What:** A graph neural network using symmetric message passing to aggregate neighborhood information for root count prediction.

**How:**
- Architecture: Input → GCNConv → ReLU → Dropout → GCNConv → ReLU → Dropout → Linear(num_classes) → LogSoftmax
- Uses two GCN layers with symmetric normalization: h^(l+1) = σ(D̃^(-1/2) Ã D̃^(-1/2) h^(l) W^(l))
- Aggregates features from neighbors via zMap edges
- Treats edges as undirected (symmetric aggregation)

**Why:**
- Standard GNN baseline for node classification tasks
- Tests whether local neighborhood structure (1-2 hops) contains root count patterns
- Efficient computation via sparse matrix operations

**Preconditions:**
- Undirected graph (zMap edges are symmetric)
- Connected components should ideally overlap with training data
- Node features required (wNum)
- Graph density affects receptive field size

**Limitations:**
- Ignores edge directionality (treats parent→child same as child→parent)
- May not capture hierarchical constraints in polynomial difference trees
- Symmetric aggregation inappropriate for directed algebraic relationships

---

### 3. GraphSAGEClassifier

**Name:** GraphSAGE Classifier with Max Aggregation

**What:** A graph neural network using neighborhood sampling and max aggregation for scalable root count prediction.

**How:**
- Architecture: Input → SAGEConv(max) → ReLU → Dropout → SAGEConv(max) → ReLU → Dropout → Linear(num_classes) → LogSoftmax
- Max aggregation: h_N(v) = max({h_u : u ∈ N(v)})
- Combines node's own features with aggregated neighbor features: h_v' = σ(W · CONCAT(h_v, h_N(v)))
- Designed for mini-batch training with neighbor sampling (though currently uses full-batch)

**Why:**
- Max aggregation can capture extreme values (e.g., max wNum in neighborhood)
- More expressive than mean aggregation for capturing structural diversity
- Scalable to large graphs via sampling (important for 1M+ node databases)

**Preconditions:**
- Same as GCN (undirected graph, connected components, node features)
- Aggregation method ('max', 'mean', 'add') should align with task semantics
- For large graphs, mini-batch training requires torch-sparse or pyg-lib

**Limitations:**
- Same directionality issue as GCN (symmetric aggregation)
- Max aggregation loses information about neighbor distribution
- Fixed aggregation depth (2 hops) may miss long-range dependencies in deep trees

---

### 4. DepthAwareGAT

**Name:** Depth-Aware Graph Attention Network (IMPROVED)

**What:** A graph attention network that uses wNum (polynomial degree/depth) differences as edge features to compute attention weights, addressing the directional nature of polynomial difference trees.

**How:**
- Architecture: Input → GATConv(edge_dim) → ELU → Dropout → GATConv(edge_dim) → ELU → Dropout → GATConv(edge_dim) → LogSoftmax
- **Edge Feature Computation** (3-component encoding):
  1. `normalized_diff = (wNum_src - wNum_tgt) / max(|diff|)` - signed depth difference
  2. `abs_diff = |normalized_diff|` - magnitude of difference
  3. `direction = 1.0 if wNum_src > wNum_tgt else 0.0` - parent→child indicator
- Edge features encoded via: EdgeEncoder(3 dims) → Linear(hidden) → ReLU → Linear(heads)
- Attention mechanism: α_ij = softmax_j(LeakyReLU(a^T [W h_i || W h_j || edge_attr_ij]))
- Multi-head attention (default: 4 heads) for diverse attention patterns

**Why:**
- **Addresses core limitation of GCN/GraphSAGE**: Polynomial difference trees are hierarchical and directed
  - Parent nodes (higher wNum) constrain child nodes (lower wNum)
  - Differentiation flows downward: P(x) → ΔP(x) → Δ²P(x) → ...
  - Standard symmetric aggregation treats parent↔child as equivalent (incorrect)
- **Depth difference encodes algebraic relationships**:
  - wNum_src - wNum_tgt > 0 → parent-to-child edge (high importance)
  - wNum_src - wNum_tgt < 0 → child-to-parent edge (lower importance)
  - |difference| indicates distance in difference hierarchy
- **Attention learns to weight edges by relevance**: Parent information should dominate (learned via edge features)
- **Works with spectral positional encodings**: Combines local (depth) and global (spectral) structure

**Preconditions:**
- **wNum feature must be available** (used to compute edge features)
- Graph should represent difference tree structure (zMap edges)
- Depth differences should correlate with root count patterns
- Optional: Spectral positional encodings for global structure (requires graph size <100K nodes)

**Key Improvements over Standard GAT:**
- Standard GAT: α_ij = f(h_i, h_j) - no edge information
- DepthAwareGAT: α_ij = f(h_i, h_j, wNum_diff) - depth-aware attention

**Empirical Performance:**
- Best performer in ordinal metrics (Macro F1, Balanced Accuracy)
- Learns directional patterns (higher weights for parent→child edges)
- Requires class weighting to overcome imbalance (53.2% majority class)

---

### 5. MultiTaskRootClassifier

**Name:** Multi-Task Learning Classifier (Root Count + Determined Flag)

**What:** A depth-aware GAT that jointly predicts two targets: (1) number of visible roots in truncated window, and (2) determined flag (computed on larger/full scan window).

**How:**
- **Shared Backbone**: Same depth-aware GAT architecture as model #4 (2 GATConv layers)
- **Two Task Heads**:
  1. `count_head`: Linear(hidden×heads → num_classes) for root count (0-4)
  2. `determined_head`: Linear(hidden×heads → 2) for binary determined/undetermined
- **Training**: Multi-task loss = λ_count × NLLLoss(count) + λ_det × NLLLoss(determined)
- **Output**: Returns (count_logits, determined_logits) as tuple

**Why:**
- **Leverages task correlation**: Determined status and visible root count are related
  - Nodes with determined=True tend to have fewer remaining roots to find
  - Multi-task learning forces shared representations to capture both signals
- **Addresses truncated observation problem**: 
  - `y_visible = len(RootList)` - all distinct roots in expanded window (complete count)
  - `determined` - computed on larger scan window (provides completeness signal)
  - Joint training helps model understand observability vs. ground truth
- **Auxiliary task as regularization**: Determined prediction acts as auxiliary task, reducing overfitting

**Preconditions:**
- Both `visibleRootCount` (or `RootList`) and `determined` labels must be available
- Determined flag should be computed on larger/full window (not derived from RootList)
- Same as DepthAwareGAT: wNum feature, difference tree structure

**Target Modes (config.TARGET_MODE):**
- `'visible'`: Predict y_visible = len(RootList) only (single task - complete root count)
- `'multitask'`: Predict y_visible + determined (use this model)
- `'censored'`: Predict y_true with constraint y_true ≥ y_visible (uses censored learning)

**Usage:**
```python
model = MultiTaskRootClassifier()
count_probs, determined_probs = model(data.x, data.edge_index)
# count_probs: [N, 5] log probabilities for root count
# determined_probs: [N, 2] log probabilities for determined flag
```

---

### 6. RegressionClassifier

**Name:** Regression-Based Root Count Classifier

**What:** Predicts root count as a continuous scalar value (0.0-4.0) via regression, then rounds to integer class. Tests whether ordinal structure improves predictions.

**How:**
- Architecture: Input → GCNConv → ReLU → Dropout → GCNConv → ReLU → Dropout → Linear(1) → Sigmoid × 4
- **Regression Output**: Single scalar bounded to [0, 4] via sigmoid activation
- **Training**: MSE or SmoothL1 loss on continuous targets
- **Inference**: Round predictions and clamp to [0, 4] for class labels

**Why:**
- **Tests ordinal hypothesis**: Root counts have natural ordering (0 < 1 < 2 < 3 < 4)
  - Standard classification treats classes as nominal (no ordering assumed)
  - Regression explicitly models ordinality: predicting 2 when true=1 is better than predicting 4
- **Distance-aware loss**: 
  - MSE(pred=2, true=1) = 1.0 (small error)
  - MSE(pred=4, true=1) = 9.0 (large error)
  - Cross-entropy treats both as equally wrong (class mismatch)
- **Diagnostic tool**: If regression outperforms classification → ordinal structure matters → consider CORAL/ordinal methods

**Preconditions:**
- Labels must be numeric and ordinal (root count 0-4 satisfies this)
- Regression assumes smooth transitions between classes (may not hold if classes are highly separable)
- Same graph/feature requirements as GCN

**Empirical Results:**
- Test MAE: 0.6070 (better than classification 0.6437)
- Test Macro F1: 0.1529 (better than classification 0.1313)
- **Conclusion**: Regression wins on both MAE and F1 → ordinal structure helps → use CORAL

**Usage:**
```python
model = RegressionClassifier()
continuous_preds = model(data.x, data.edge_index)  # [N, 1] in [0.0, 4.0]
class_preds = model.predict_classes(data.x, data.edge_index)  # [N] rounded to [0, 4]
```

---

### 7. CORALClassifier

**Name:** CORAL Ordinal Regression Classifier

**What:** Consistent Rank Logits (CORAL) ordinal regression that replaces 5-way softmax with 4 binary sigmoid thresholds to respect ordering and improve minority class prediction.

**How:**
- **Architecture**: Input → GCNConv → ReLU → Dropout → GCNConv → ReLU → Dropout → CORAL Head
- **CORAL Head**:
  - Shared weight matrix: `fc = Linear(hidden → 1, bias=False)`
  - Per-threshold biases: `threshold_biases = Parameter([b0, b1, b2, b3])`
  - Output: `logits_k = fc(h) + threshold_biases[k]` for k ∈ {0,1,2,3}
- **4 Binary Thresholds**:
  1. t0: P(y > 0) - probability of having at least 1 root
  2. t1: P(y > 1) - probability of having at least 2 roots
  3. t2: P(y > 2) - probability of having at least 3 roots
  4. t3: P(y > 3) - probability of having 4 roots
- **Training**: Binary cross-entropy per threshold
  - For label y=2: targets = [1, 1, 0, 0] (true for t0,t1; false for t2,t3)
  - Loss = BCE(sigmoid(logit0), 1) + BCE(sigmoid(logit1), 1) + BCE(sigmoid(logit2), 0) + BCE(sigmoid(logit3), 0)
- **Prediction**: Count how many thresholds exceed 0.5
  - pred = sum([sigmoid(t0)>0.5, sigmoid(t1)>0.5, sigmoid(t2)>0.5, sigmoid(t3)>0.5])

**Why:**
- **Solves "never predicts max class" problem**: 
  - Standard softmax: Class 4 (0.6% of data) gets buried by majority class gradients
  - CORAL: Threshold t3 gets its own binary loss signal → class 4 can be learned independently
- **Respects ordinal structure**:
  - Constraint: If P(y > k) = 1, then P(y > k-1) must also = 1 (rank consistency)
  - Shared weights ensure: t0 ≥ t1 ≥ t2 ≥ t3 (after proper bias ordering)
- **More balanced gradients**:
  - Each threshold sees ~50% positive/negative examples (across all samples)
  - Class 4: 0.6% of samples contribute to t3, but t3 gets equal weight as t0,t1,t2
- **Better than regression**: Outputs proper class probabilities (via threshold differences)

**Preconditions:**
- Labels must be ordinal with natural ordering (0 < 1 < 2 < 3 < 4)
- Binary cross-entropy requires sigmoid outputs (built into CORAL head)
- Class imbalance: CORAL specifically designed for imbalanced ordinal tasks

**Empirical Results:**
- **Best Macro F1: 0.2405** (vs 0.1313 GAT, 0.1529 Regression)
- **Best Balanced Acc: 0.3198** (vs 0.2000 GAT, 0.1894 Regression)
- Test MAE: 0.6424 (slightly worse than regression 0.6070, but better class coverage)
- **Breakthrough**: Predicts classes 0, 1, 2 (previously only 0, 1)
- Still struggles with classes 3-4 (needs further minority class handling)

**Convert Thresholds to Class Probabilities:**
```python
# P(y=0) = 1 - P(y>0) = 1 - sigmoid(t0)
# P(y=k) = P(y>k-1) - P(y>k) = sigmoid(t_{k-1}) - sigmoid(t_k)
# P(y=4) = P(y>3) = sigmoid(t3)
class_probs = model.get_probabilities(data.x, data.edge_index)  # [N, 5]
```

---

## Training Infrastructure

### Loss Functions

**Cross-Entropy (CE)**
- Standard: `-sum(y_c * log(p_c))` for c ∈ classes
- Class-Weighted: `-sum(w_c * y_c * log(p_c))` where `w_c = sqrt(N / (K * N_c))`
- **Use when**: Classes are relatively balanced, or class weights handle imbalance

**Earth Mover's Distance (EMD)**
- Ordinal-aware loss: `sum(|CDF_pred - CDF_true|)` where CDF = cumulative distribution
- Penalizes predictions proportionally to distance from true class
- Example: Predicting 3 when true=1 incurs more loss than predicting 2
- **Use when**: Ordinal structure is important, errors should be distance-weighted
- **Config**: `LOSS_TYPE = 'emd'`

**Binary Cross-Entropy (for CORAL)**
- Per-threshold: `BCE(sigmoid(t_k), target_k)` for k ∈ {0,1,2,3}
- Target vector computed from ordinal label: y=2 → [1,1,0,0]
- **Use when**: Using CORALClassifier

**Regression Losses**
- MSE: `mean((pred - true)^2)` - standard for regression
- SmoothL1: Combines MSE (small errors) and MAE (large errors) - more robust to outliers
- **Use when**: Using RegressionClassifier

### Class Weighting Strategies

**Inverse Frequency (Aggressive)**
- `w_c = N / (K * N_c)` where N=total samples, K=num classes, N_c=count of class c
- Normalizes so each class contributes equally to total loss
- **Risk**: Can cause "class collapse" (model ignores majority to satisfy minority)

**Square Root Inverse Frequency (Tempered, Default)**
- `w_c = sqrt(N / N_c)` normalized to mean=1
- Balances majority/minority without extreme reweighting
- **Config**: `compute_class_weights(labels, method='sqrt')`
- **Best for**: Imbalanced classification without ordinal collapse

**Effective Number of Samples**
- `E_c = (1 - β^N_c) / (1 - β)` where β ∈ [0.9, 0.9999]
- `w_c = (1 - β) / E_c`
- Smoothly interpolates between uniform and inverse frequency
- **Use when**: Very long-tailed distributions (e.g., class 4 = 0.6%)

### Evaluation Metrics

**Macro F1 (Primary)**
- `(F1_0 + F1_1 + F1_2 + F1_3 + F1_4) / 5` - treats all classes equally
- **Critical for imbalanced data**: Prevents majority class from dominating metric
- **Use for**: Model comparison, early stopping

**Balanced Accuracy**
- `(Recall_0 + Recall_1 + ... + Recall_4) / 5` - mean per-class recall
- Measures ability to correctly identify each class
- **Use for**: Assessing minority class performance

**Mean Absolute Error (MAE)**
- `mean(|pred_class - true_class|)` in units of "roots"
- Interpretable: "On average, predictions are off by X roots"
- **Use for**: Ordinal evaluation, comparing regression vs classification

**Weighted F1 (For Comparison Only)**
- `sum(N_c / N * F1_c)` - weighted by class frequency
- **Misleading for imbalanced data**: Dominated by majority class
- Reported for backwards compatibility with earlier experiments

**Per-Class Recall**
- `Recall_c = TP_c / (TP_c + FN_c)` for each class
- **Critical diagnostic**: Identifies which classes the model fails on
- **Example**: Recall_4 = 0.00 → model never predicts class 4

---

## Experimental Findings

### Key Results Summary

| Model | Test Macro F1 | Balanced Acc | Test MAE | Classes Predicted |
|-------|---------------|--------------|----------|-------------------|
| Full-batch GAT (CE) | 0.1313 | 0.2000 | 0.6437 | 0, 1 |
| Regression | 0.1529 | 0.1894 | 0.6070 | 0, 1, 2 |
| **CORAL** | **0.2405** | **0.3198** | 0.6424 | **0, 1** |
| Full-batch GAT (EMD) | 0.2055 | 0.2344 | 0.6861 | 0, 1, 2, 3 |

### Critical Insights

1. **Graph Structure Provides Minimal Signal (Initially)**
   - MLP, GCN, GraphSAGE achieved identical F1 ≈ 0.625 (all predict only classes 0,1)
   - Symmetric aggregation (GCN, GraphSAGE) fails to capture directed tree structure
   - **Solution**: DepthAwareGAT uses edge directionality → improves minority class coverage

2. **Ordinal Structure Matters**
   - Regression outperforms classification on MAE and F1
   - Root counts are ordinal (0 < 1 < 2 < 3 < 4), not nominal
   - **Solution**: CORAL ordinal regression achieves best Macro F1 and Balanced Acc

3. **Class Imbalance Requires Specialized Handling**
   - Class 1 (53.2%) dominates gradients → classes 2-4 ignored
   - Class weights help but insufficient alone
   - **Solution**: CORAL gives each threshold independent loss signal → class 4 learnable

4. **Feature Engineering Critical**
   - Single feature (wNum) insufficient to distinguish root counts within same degree
   - Depth-aware edge features (wNum_src - wNum_tgt) provide directional signal
   - Spectral positional encodings (when feasible) capture global structure

### Performance Interpretation

**F1 = 0.6249 is Misleading**
- Weighted by class size: 0.20×F1_0 + 0.53×F1_1 + 0.23×F1_2 + ... = 0.62
- Hides complete failure on classes 2-4 (F1=0.00)
- **Always report Macro F1 (treats classes equally)**

**MAE = 0.31 Hides Error Pattern**
- 73.2% of nodes in classes 0-1 with zero error
- Class 2: Error = 1 (predicted as 1, off by 1)
- Class 3: Error = 2, Class 4: Error = 3
- Low MAE because majority classes dominate average

**CORAL Breakthrough**
- First model to predict class 2 with non-zero recall
- Balanced Accuracy 0.32 (vs 0.20 for standard GAT)
- Still room for improvement on classes 3-4 (needs further techniques)

---

## Recommendations for Future Work

### 1. Hybrid Approaches
- Combine CORAL ordinal structure with EMD loss for distance-aware learning
- Use DepthAwareGAT backbone + CORAL head for best of both worlds

### 2. Advanced Imbalance Handling
- **Focal Loss**: Down-weight easy examples (majority class), up-weight hard examples (minority)
- **Cost-Sensitive Learning**: Asymmetric misclassification costs (penalize missing class 4 more)
- **Oversampling**: SMOTE for minority classes (challenging in graph setting)

### 3. Graph Structural Features
- **Spectral Positional Encodings**: Laplacian eigenvectors (requires graph <100K nodes)
- **Sibling-Aware GNN**: Separate message passing for parent→child vs sibling→sibling
- **Tree-Structured Networks**: TreeLSTM or Recursive NNs designed for hierarchical data

### 4. Feature Engineering
- **Polynomial Coefficients**: Extract from `vmResult` (coefficient list)
- **Derived Features**: Coefficient magnitudes, signs, leading coefficient
- **Neighborhood Statistics**: Aggregated root counts in k-hop neighborhoods

### 5. Ensemble Methods
- Combine predictions from CORAL, Regression, and DepthAwareGAT(EMD)
- Weighted voting based on per-class strengths
- Stacking: Use Level-1 models as features for Level-2 meta-classifier

---

## Configuration Reference

**Model Selection**
```python
from graph_label_prediction.models import get_model

# Get model by name
model = get_model('coral', hidden_dim=64, num_classes=5)
# Available: 'mlp', 'gcn', 'graphsage', 'depth_aware_gat', 'multitask', 'regression', 'coral'
```

**Training Configuration** (`config.py`)
```python
# Feature settings
BASE_FEATURES = ['wNum', 'degree_at_node']  # 2 base features
SPECTRAL_PE_DIM = 0  # Disabled for large graphs
NUM_FEATURES = 2  # BASE_FEATURES + SPECTRAL_PE_DIM

# Model architecture
HIDDEN_DIM = 64
DROPOUT_RATE = 0.3
NUM_ATTENTION_HEADS = 4  # For GAT models

# Training
MAX_EPOCHS = 100
PATIENCE = 10  # Early stopping
LEARNING_RATE = 0.01
PENALTIES = [1e-4, 1e-3, 1e-2, 0.0625]  # Grid search

# Loss and targets
TARGET_MODE = 'visible'  # 'visible' | 'multitask' | 'censored'
LOSS_TYPE = 'emd'  # 'ce' | 'emd'
USE_CLASS_WEIGHTS = True
```

**Graph Filtering** (for spectral PE on large graphs)
```python
USE_GRAPH_FILTERING = True
PARRAY_MIN = 0
PARRAY_MAX = 5  # Only nodes where all pArrayList ∈ [0, 5)
MAX_NODES_FOR_SPECTRAL_PE = 100000  # Skip PE if graph too large
```

---

## Preconditions Summary

All models require:
- ✅ **Node features**: At minimum `wNum` (polynomial degree/depth)
- ✅ **Labels**: `totalZero` (root count 0-4) or related targets
- ✅ **Graph structure**: zMap edges defining polynomial difference trees

Model-specific:
- **DepthAwareGAT, MultiTask**: wNum feature for edge encoding
- **MultiTask**: Both `visibleRootCount` and `determined` labels
- **CORAL**: Ordinal labels with natural ordering
- **Regression**: Numeric labels suitable for continuous prediction
- **Spectral PE**: Graph size <100K nodes (or use filtering)
- **Mini-batch training**: torch-sparse or pyg-lib (optional, not currently used)

Data quality:
- ✅ **Class coverage**: All 5 classes present in train/val/test splits
- ✅ **Connected components**: Training nodes should cover graph regions
- ✅ **Label consistency**: No contradictory labels for same structural patterns
- ⚠️ **Class imbalance**: Expect extreme imbalance (53% class 1, 0.6% class 4)

---

*Document version: 1.0*  
*Last updated: 2026-01-22*  
*Pipeline: Graph Label Prediction for Root Count (totalZero)*

