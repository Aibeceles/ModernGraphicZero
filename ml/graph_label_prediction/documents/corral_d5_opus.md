# CORAL d5 Post-Experiment Debrief (Opus)

This document is a comprehensive post-experiment debriefing of the **CORAL ordinal regression pipeline** as executed in `workbooks/run_pipeline.ipynb`. It traces every stage of the system end-to-end: **Neo4j queries -> label construction -> feature engineering -> hyperparameter configuration -> model architecture -> training dynamics -> embedding extraction -> predictions and diagnostics -> write-back**.

It is written for someone who needs to understand not just *what* happened, but *why each piece exists* and *where things broke*.

---

## 0. What the experiment is trying to solve

The goal is to predict an **ordinal target**: the number of **distinct rational roots** for each `(:Dnode)` in a Neo4j graph database (`d5seed1`, 3,024,769 nodes total).

**Why ordinal matters**: predicting "2 roots" when the truth is "3 roots" should be penalized less than predicting "0 roots". Standard flat softmax treats all misclassifications equally. CORAL (Consistent Rank Logits) encodes the ordinal structure via **monotone threshold logits** rather than a `K`-way softmax, so the model is structurally encouraged to make errors that are *close* in rank rather than catastrophically far.

The experiment runs on a **filtered subgraph** of d5 (pArrayList in [0, 7)), which reduces the 3M-node graph down to **8,376 nodes** --- a 99.7% reduction. This makes spectral methods feasible and keeps iteration times manageable.

---

## 1. Queries: what gets pulled from Neo4j

All data loading is handled by `GraphDataLoader` in `python_model/core/data_loader.py`. Query selection is controlled by `USE_GRAPH_FILTERING` and related constants in `python_model/core/config.py`.

### 1.1 Node query (filtered, censored mode)

Because `USE_GRAPH_FILTERING = True`, the pipeline uses `FILTERED_NODE_QUERY_CENSORED_TEMPLATE`:

```cypher
MATCH (d:Dnode)
WITH d
CALL {
     WITH d
     MATCH (d)-[:CreatedBye]->(cb)
     WHERE all(x IN cb.pArrayList WHERE x >= 0 AND x < 7)
     WITH DISTINCT d AS unique_D
     RETURN unique_D
}
WITH unique_D AS d
RETURN elementId(d) as node_id,
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       d.n as n,
       d.d as mu_d,
       d.muList as muList
```

**Key observations**:

- The `CALL` subquery filters each `Dnode` by checking that **all** entries in its `CreatedBy.pArrayList` fall within `[PARRAY_MIN, PARRAY_MAX)` = `[0, 7)`. This is the d5 slice boundary.
- `wNum` (the CreatedBy depth) is **not returned** in the filtered query variant (it's commented out). In the filtered version the subquery structure means `cb` is scoped inside the CALL block. This means the loaded data has `wNum = 0` for all nodes in this run unless the loader fills it from another source. (It doesn't --- this is a known gap that affects depth-aware attention features.)
- The label is `coalesce(size(d.RootList), d.totalZero, 0)`: prefer the visible root list length, fall back to `totalZero`, default to 0.
- `determined` is a completeness flag (was the full root scan window exhausted?). It is used downstream as both a feature and an analysis stratum.

### 1.2 Edge query (filtered)

```cypher
MATCH (d:Dnode)
WITH d
CALL {
     WITH d
     MATCH (d)-[:CreatedBye]->(cb)
     WHERE all(x IN cb.pArrayList WHERE x >= 0 AND x < 7)
     WITH DISTINCT d AS unique_D
     RETURN unique_D
}
WITH unique_D
CALL {
      WITH unique_D
      MATCH (unique_D)<-[:zMap]-(d1)
      RETURN d1
}
WITH unique_D, d1
RETURN elementId(d1) as source, elementId(unique_D) as target
```

- Returns `(source, target)` elementId pairs from `zMap` relationships.
- The loader then maps elementIds to contiguous integer indices and builds `edge_index` with **both directions** so PyTorch Geometric treats the graph as undirected.
- Result: **8,375 undirected edges** for 8,376 nodes (graph density: 0.000239).
- The near-tree structure (edges ~= nodes - 1) is notable: this is an extremely sparse graph, almost a forest.

---

## 2. Labels: how root-count classes are defined and made ordinal

### 2.1 Raw label extraction

```python
labels = nodes_df['label']  # coalesce(size(RootList), totalZero, 0)
```

This produces the **visible distinct root count** in the expanded/truncated window, not necessarily the "true" full root count. The `TARGET_MODE = 'visible'` setting in config confirms this is the intended training target.

### 2.2 Class distribution (the imbalance problem)

| Class (roots) | Count | Percentage |
|:---:|---:|---:|
| 0 | 1 | 0.0% |
| 1 | 6,021 | 71.9% |
| 2 | 1,793 | 21.4% |
| 3 | 383 | 4.6% |
| 4 | 89 | 1.1% |
| 5 | 89 | 1.1% |

**Imbalance ratio**: 6,021 : 1 (majority to minority). This is severe. Class 0 has exactly **one** sample. Classes 4 and 5 have only 89 each. Any model that always predicts "1 root" achieves 71.9% accuracy for free.

### 2.3 Contiguous class remapping (critical for CORAL)

Raw root counts are not guaranteed to be dense (e.g., you might see only `{0,1,2,3,4,5}` or `{1,2,3,5}` depending on the filter). CORAL and CE losses require labels in `[0..K-1]`, so the loader builds:

```python
classes_present = sorted(unique(y_raw))      # e.g. [0, 1, 2, 3, 4, 5]
raw_to_contiguous = {raw_val: idx for idx, raw_val in enumerate(classes_present)}
y_mapped = [raw_to_contiguous[label] for label in y_raw]
```

Stored on the PyG `Data` object:

| Attribute | Content |
|---|---|
| `data.y` | Contiguous class indices (used for training) |
| `data.y_raw` | Original raw labels (root counts) |
| `data.y_visible` | Same as `data.y` (explicit copy) |
| `data.class_values` | `[0, 1, 2, 3, 4, 5]` (ordered raw values) |
| `data.num_classes` | 6 |
| `data.raw_to_contiguous` | `{0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5}` |
| `data.determined` | Boolean mask for completeness |
| `data.totalZero` | Raw `totalZero` values |
| `data.RootList` | Raw `RootList` values |

**Implication**: In this particular run, raw and contiguous happen to coincide (classes are already `0..5`). But this is not guaranteed in general. Any write-back of predictions must decode through `data.class_values[pred_contiguous]`.

---

## 3. Features: what goes into the model (and why)

Feature construction lives in `GraphDataLoader._build_feature_matrix()`. The final feature matrix is **25-dimensional**.

### 3.1 Base features (3D)

| Feature | Source | Notes |
|---|---|---|
| `wNum` | `CreatedBy.wNum` | Depth in the generation tree. **In the filtered query, this is missing (0 for all nodes).** |
| `degree_at_node` | Derived from `vmResult` | Highest non-zero polynomial power |
| `determined` | `d.determined` | Binary flag: was the root scan window exhausted? |

### 3.2 Coefficient features (6D)

Extracted from `vmResult` (the coefficient list stored as a string like `"[0, c_n, ..., c_1, c_0]"`):

1. Parse the string to a list of floats.
2. Reverse to ascending order: `[c_0, c_1, ..., c_n]`.
3. Pad to `MAX_POLYNOMIAL_DEGREE + 1 = 6` entries.
4. Store as `coeff_0` through `coeff_5`.

### 3.3 Statistical features (5D)

Derived from the coefficient vector:

| Feature | Formula |
|---|---|
| `coeff_magnitude` | L2 norm of coefficient vector |
| `leading_coeff_abs` | \|c_n\| |
| `constant_term_abs` | \|c_0\| |
| `coeff_sparsity` | Fraction of zero coefficients |
| `coeff_mean_abs` | Mean absolute value |

### 3.4 Set Union Ratio features (3D)

| Feature | Source | Notes |
|---|---|---|
| `n` | `d.n` | `max(muList)` |
| `mu_d` | `d.d` | Sum of muList values |
| `mu_ratio` | `n / mu_d` | Safe-divided (handles zero denominator) |

### 3.5 Spectral Positional Encodings (8D)

Configured by `SPECTRAL_PE_DIM = 8` and `MAX_NODES_FOR_SPECTRAL_PE = 100000`.

- Computes the **normalized graph Laplacian** from `edge_index`.
- Extracts the `k+1` smallest eigenvectors (skips the constant eigenvector).
- Handles sign ambiguity (eigenvectors are sign-indeterminate).
- If the graph exceeds 100K nodes, pads with 8 zeros per node instead.

In this run: 8,376 nodes < 100K, so **real spectral PE was computed** (confirmed by notebook output: "Feature dim: 25 (17 base+coeff+stats+set_union + 8 spectral PE)").

### 3.6 Summary: the 25D feature vector

```
[wNum, degree, determined,
 coeff_0, coeff_1, coeff_2, coeff_3, coeff_4, coeff_5,
 magnitude, leading_abs, constant_abs, sparsity, mean_abs,
 n, mu_d, mu_ratio,
 spectral_pe_0, ..., spectral_pe_7]
```

---

## 4. Hyperparameters: how the run is configured

### 4.1 Global defaults from config

| Parameter | Value | Notes |
|---|---|---|
| `HIDDEN_DIM` | 64 | Width of GNN hidden layers |
| `DROPOUT_RATE` | 0.3 | Applied after each conv layer |
| `LEARNING_RATE` | 0.01 | Adam optimizer |
| `MAX_EPOCHS` | 100 | (overridden to 200 in notebook) |
| `MIN_EPOCHS` | 10 | |
| `PATIENCE` | 10 | (overridden to 20 in notebook) |
| `PENALTIES` | [1e-4, 1e-3, 1e-2, 0.0625] | Interpreted as Adam `weight_decay` |
| `LOSS_TYPE` | 'emd' | Default loss; CORAL overrides internally |
| `TARGET_MODE` | 'visible' | Train on visible root count |
| `HOLDOUT_FRACTION` | 0.20 | 80/20 train-test split |
| `NUM_ATTENTION_HEADS` | 4 | For GAT-based models |
| `FOCAL_GAMMA` | 2.0 | For focal loss variant |

### 4.2 Class weight computation

Computed from the **training split only** using `compute_class_weights(method='sqrt')`:

```
sqrt inverse frequency, normalized to mean=1
```

| Class | Weight |
|:---:|---:|
| 0 roots | 4.361 |
| 1 root | 0.070 |
| 2 roots | 0.128 |
| 3 roots | 0.279 |
| 4 roots | 0.605 |
| 5 roots | 0.558 |

These weights are applied to CE/EMD/Focal losses but **not to CORAL** (CORAL has its own binary cross-entropy per threshold).

### 4.3 What the notebook actually passed for CORAL

```python
coral_model, coral_metrics = trainer.train(
    model_name='coral',
    weight_decay=1e-3,
    max_epochs=200,
    patience=20,
    verbose=True
)
```

- Optimizer: **Adam** with `lr=0.01`, `weight_decay=1e-3`.
- Early stopping: **validation macro F1**, patience 20, min epochs 10.
- Full-batch training (no mini-batch/NeighborLoader for this run).

### 4.4 Data splits (stratified)

| Split | Nodes | Classes present |
|---|---:|---|
| Train | 5,446 | [0, 1, 2, 3, 4, 5] |
| Validation | 1,256 | [1, 2, 3, 4, 5] |
| Test | 1,675 | [1, 2, 3, 4, 5] |

Class 0 has only 1 sample total, so it only appears in train.

---

## 5. CORAL model: what "ordinal regression" means here

### 5.1 Architecture (CORALClassifier)

Implemented in `python_model/core/models.py`:

```
CORALClassifier
  conv1: GCNConv(25, 64)       # input: 25D features
  conv2: GCNConv(64, 64)       # hidden layer
  dropout: 0.3
  fc: Linear(64, 1, bias=False) # shared projection (no bias!)
  threshold_biases: Parameter(K-1) = Parameter(5)  # learnable thresholds
```

### 5.2 Forward pass

1. **GCN backbone** (2 layers):
   - `h = dropout(relu(conv1(x, edge_index)))`
   - `h = dropout(relu(conv2(h, edge_index)))`

2. **CORAL head**:
   - Shared linear projection: `z = fc(h)` -> shape `[N, 1]`
   - Per-threshold learnable biases: `threshold_biases` -> shape `[K-1]` = `[5]`
   - Output logits: `logits[:, k] = z + threshold_biases[k]` -> shape `[N, 5]`

The key insight: all thresholds share the **same linear projection** of the embedding. Only the **bias** differs per threshold. This enforces a monotone structure: if the model raises the shared logit, it raises *all* threshold logits, which is consistent with the ordinal interpretation "higher embedding value = more roots".

### 5.3 Loss function

In `NodeClassificationTrainer._compute_coral_loss()`:

```python
# Build binary targets: target[k] = 1 if label > k, else 0
targets = zeros(N, K-1)
for k in range(K-1):
    targets[:, k] = (labels > k).float()

# Binary cross-entropy with logits (numerically stable)
loss = F.binary_cross_entropy_with_logits(logits[mask], targets[mask])
```

- Each threshold `k` gets a binary classification problem: "is the true label > k?"
- The loss is the **mean** of all `K-1` binary cross-entropies across all masked nodes.
- **No class weighting** is applied inside the CORAL loss. The binary targets are inherently imbalanced (threshold 0: "label > 0" is almost always true; threshold 4: "label > 4" is rarely true).

### 5.4 Prediction

Class prediction is "how many thresholds exceed 0.5":

```python
def predict_classes(logits):
    probs = torch.sigmoid(logits)      # [N, K-1]
    preds = (probs > 0.5).sum(dim=1)   # [N] -> values in [0, K-1]
    return preds
```

Probability conversion (threshold probabilities -> class probabilities):

```python
P(y = 0) = 1 - P(y > 0)
P(y = k) = P(y > k-1) - P(y > k)   for k in [1, K-2]
P(y = K-1) = P(y > K-2)
```

---

## 6. Training dynamics: what happened epoch by epoch

### 6.1 CORAL training log

| Epoch | Loss | Train Macro F1 | Val Macro F1 |
|---:|---:|---:|---:|
| 0 | 158.9875 | 0.0037 | 0.0025 |
| 10 | 41.2209 | 0.0055 | 0.0011 |
| 20 | 11.7099 | 0.0009 | 0.0000 |
| 30 | 1.5726 | 0.1360 | 0.1361 |
| 40 | 0.6353 | 0.1334 | 0.1337 |
| 50 | 0.5563 | 0.1395 | 0.1676 |

Early stopping triggered at **epoch 87**.

**Interpretation**: The model starts with high loss (random thresholds). By epoch 20, loss drops dramatically but Macro F1 is near zero --- this is the period where the model is learning to predict the majority class. By epoch 30, F1 jumps to ~0.14 (slight differentiation begins), but it never improves meaningfully beyond ~0.17. The model converged to a degenerate solution.

---

## 7. Predictions: what the model actually produced

### 7.1 Test set metrics

| Metric | Value |
|---|---:|
| Test Macro F1 | 0.1781 |
| Test Balanced Accuracy | 0.1972 |
| Test MAE | 0.4322 roots |
| Weighted F1 | 0.59 (misleading) |

### 7.2 Confusion matrix (normalized by true class, test set)

|  | Pred 0 | Pred 1 | Pred 2 | Pred 3 | Pred 4 | Pred 5 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **True 1** | - | **100.0%** | 0 | 0 | 0 | 0 |
| **True 2** | - | **100.0%** | 0 | 0 | 0 | 0 |
| **True 3** | - | **100.0%** | 0 | 0 | 0 | 0 |
| **True 4** | - | **100.0%** | 0 | 0 | 0 | 0 |
| **True 5** | - | **100.0%** | 0 | 0 | 0 | 0 |

**Total class collapse**: the model predicts "1 root" for every single node.

### 7.3 Classification report

```
              precision    recall  f1-score   support
      1 root       0.71      1.00      0.83      1195
     2 roots       0.00      0.00      0.00       360
     3 roots       0.00      0.00      0.00        74
     4 roots       0.00      0.00      0.00        26
     5 roots       0.00      0.00      0.00        20

    accuracy                           0.71      1675
   macro avg       0.14      0.20      0.17      1675
weighted avg       0.51      0.71      0.59      1675
```

### 7.4 Prediction distribution (collapse diagnostics)

| Class | Train | Val | Test | Status |
|---|---:|---:|---:|---|
| 0 roots | 0 | 0 | 0 | COLLAPSED |
| 1 root | 5,445 | 1,256 | 1,675 | *only predicted class* |
| 2 roots | 0 | 0 | 0 | COLLAPSED |
| 3 roots | 0 | 0 | 0 | COLLAPSED |
| 4 roots | 0 | 0 | 0 | COLLAPSED |
| 5 roots | 0 | 0 | 0 | COLLAPSED |

**Error analysis**: 360 neighbor-class errors (75.0%), 120 distant errors (25.0%).

### 7.5 Why collapse happened

CORAL's binary threshold formulation means:

- **Threshold 0** ("is label > 0?"): The answer is "yes" for 8,375/8,376 nodes. The model trivially learns this threshold correctly.
- **Threshold 1** ("is label > 1?"): Only 2,354/8,376 nodes (28.1%) have label > 1. With no per-threshold class weighting, the model can satisfy this threshold by predicting "no" for everyone --- the BCE loss for this threshold alone is minimized by always outputting a negative logit.
- **Thresholds 2-4**: Even more imbalanced (4.6%, 1.1%, 1.1% positive). The model learns to output strongly negative logits for all of these.

Result: `sigmoid(threshold_0) > 0.5` for all nodes (predicts "at least 1 root"), but `sigmoid(threshold_k) < 0.5` for k >= 1 (predicts "not more than 1 root"). Net prediction: class 1 for everyone.

The CORAL loss treats all `K-1` thresholds with **equal weight in the mean**, so the easy-to-satisfy majority-class thresholds dominate the gradient signal. The rare "high root count" thresholds contribute very little to the total loss and are effectively ignored during optimization.

---

## 8. Embeddings: what is being visualized and how

### 8.1 Extraction

The notebook extracts 64-dimensional hidden embeddings using a forward hook:

```python
def extract_embeddings(model, data, layer_name='conv2'):
    # Registers a hook on model.conv2 to capture hidden activations
    # Returns: H of shape [N, 64]
```

### 8.2 Dimensionality reduction

Three methods applied:

| Method | Type | What it shows |
|---|---|---|
| PCA | Linear | Global variance structure |
| t-SNE | Nonlinear | Local neighborhood preservation |
| UMAP | Nonlinear | Global + local structure |

### 8.3 Visualization layout

For each model, a 3x3 grid is generated:

- **Rows**: colored by true labels, predicted labels, errors
- **Columns**: PCA, t-SNE, UMAP

### 8.4 CORAL embedding observations

- CORAL embedding accuracy (all nodes, not just test): **68.39%** (5,728/8,376 correct). This is misleading --- it simply reflects the majority class dominance.
- Because CORAL collapsed to a single predicted class, the "predicted labels" coloring in embedding plots shows a single color.
- The "true labels" coloring reveals whether the GCN backbone learned any internal structure despite the collapsed predictions. If the embeddings show class separation in the PCA/t-SNE/UMAP views even though predictions collapsed, it suggests the backbone has useful signal but the CORAL head (shared linear + biases) failed to separate the thresholds.

---

## 9. Comparison with all models trained in this experiment

| Model | Macro F1 | Balanced Acc | MAE (roots) | Collapse? |
|---|---:|---:|---:|---|
| DepthAwareGAT (CE, wd=0.0625) | 0.1713 | 0.1999 | 0.4346 | Yes (predicted class 1) |
| DepthAwareGATv2 (CE, wd=1e-3) | 0.1666 | 0.2000 | 0.3976 | Yes |
| **Focal Loss (wd=1e-3)** | **0.2142** | **0.2329** | 0.4388 | Partial |
| MLP (wd=1e-3) | - | - | - | - |
| GraphSAGE (wd=1e-3) | - | - | - | - |
| Regression (wd=0.0625) | 0.0000 | 0.0000 | 1.3976 | Complete (predicted 0) |
| **CORAL (wd=1e-3)** | 0.1781 | 0.1972 | 0.4322 | **Complete (predicted 1)** |
| Full-batch GAT (EMD, wd=1e-3) | 0.1666 | 0.2000 | 0.3976 | Yes |
| Full-batch GAT (CE, wd=1e-3) | 0.0708 | 0.2000 | 0.8245 | Yes |

**Best Macro F1**: Focal Loss at 0.2142 (still very poor).

**Universal pattern**: Nearly every model collapsed to predicting the majority class. The 6,021:1 class imbalance, combined with the extremely sparse graph structure (near-tree), makes this a fundamentally difficult problem for GNN-based classification.

---

## 10. Neo4j write-back: current state and CORAL caveats

### 10.1 Write-back path

Write-back is implemented in `python_model/core/predictor.py` (`NodePredictor` / `predict_and_write`):

```cypher
UNWIND $predictions AS pred
MATCH (d:Dnode)
WHERE elementId(d) = pred.node_id
SET d.predictedRootCount = pred.prediction,
    d.predictedRootProbabilities = pred.probabilities
```

Batch processing: 100 nodes per batch.

### 10.2 CORAL compatibility issues (not yet fixed)

The `NodePredictor.predict()` method assumes:

- `out` is **log-softmax** over classes.
- `pred = out.argmax(dim=1)`.
- `prob = exp(out)`.

CORAL instead returns **threshold logits**, so:

- `argmax` is meaningless on threshold logits.
- `exp(out)` is not a class probability distribution.
- Labels are in **contiguous space**, so writing raw `pred` values would write *indices*, not true root counts.

**Correct write-back for CORAL**:

```python
pred_contig = coral_model.predict_classes(logits)
prob_matrix = coral_model.get_probabilities(logits)
pred_raw = data.class_values[pred_contig]   # decode to actual root counts
```

### 10.3 Write-back status in this experiment

The write-back cells in the notebook were **never executed** (outputs are empty). No predictions were written to Neo4j.

---

## 11. Root cause analysis: why CORAL failed on d5

### 11.1 The imbalance-threshold interaction

CORAL decomposes a K-class ordinal problem into K-1 binary problems. Each binary problem inherits its own class ratio:

| Threshold k | Question | Positive rate |
|---:|---|---:|
| 0 | "label > 0?" | 99.99% (8,375/8,376) |
| 1 | "label > 1?" | 28.1% (2,354/8,376) |
| 2 | "label > 2?" | 6.7% (561/8,376) |
| 3 | "label > 3?" | 2.1% (178/8,376) |
| 4 | "label > 4?" | 1.1% (89/8,376) |

The higher thresholds are extremely imbalanced binary problems. Standard BCE treats positive and negative examples equally, so the optimal strategy for thresholds 2-4 is to **always predict negative** (minimizing BCE at the cost of zero recall on the minority side). Since the mean loss across all 5 thresholds weights them equally, the model converges to:

- Threshold 0: positive (nearly all nodes have label > 0).
- Thresholds 1-4: negative (predicting "not > k" is the low-loss solution).

This gives predicted class = 1 for every node.

### 11.2 Graph structure limitations

The filtered d5 graph is nearly a tree (8,375 edges for 8,376 nodes). In a tree-like graph, GCN message passing has limited reach --- each node's 2-hop neighborhood is small. Combined with the sparse high-root-count nodes, the GCN backbone has very few informative neighbors to aggregate from for minority classes.

### 11.3 Missing wNum feature

The filtered query variant does not return `wNum` (it's commented out in the query). This means the depth information --- which may be a strong signal for root count prediction --- is zero for all nodes. This robs the model of a potentially discriminative feature.

---

## 12. Takeaways and next actions

### What worked

- **The pipeline plumbing is correct**: queries, feature construction (including vmResult parsing, set-union features, spectral PE), ordinal target mapping, CORAL loss/prediction math, and embedding extraction all function as designed.
- **Spectral PE was computed** successfully on the filtered subgraph.
- **Stratified splitting** ensures class representation in train/val/test.

### What failed

- **CORAL collapsed to predicting one class**. This is not a bug but a fundamental limitation of unweighted CORAL under extreme class imbalance.
- **Every model in the experiment** suffered from some degree of class collapse. The problem is not CORAL-specific.
- The Focal Loss model was the best performer but still achieved only Macro F1 = 0.21.

### Recommended next actions

1. **Fix the wNum gap**: Update the filtered node query to join back to `CreatedBy` and return `cb.wNum`. This is potentially a strong feature.

2. **Add threshold-wise weighting to CORAL loss**: Weight each binary threshold's BCE inversely proportional to its class imbalance:
   ```python
   weight_k = total_samples / (2 * positive_count_k)
   ```
   This prevents the model from ignoring rare thresholds.

3. **Use mini-batch + weighted sampling**: `NeighborLoader` with `WeightedRandomSampler` (already supported by config flags `USE_MINIBATCH` and `USE_WEIGHTED_SAMPLER`) to oversample minority-class nodes and give the model more gradient signal from rare classes.

4. **Consider a hybrid architecture**: DepthAwareGAT backbone (with attention-based edge features) + CORAL ordinal head. The GAT backbone may extract richer representations from the sparse graph than GCN.

5. **Fix the write-back path for CORAL**: Add a model-type check in `NodePredictor.predict()` to use `predict_classes()` and `get_probabilities()` for CORAL models, and decode contiguous labels to raw root counts.

6. **Investigate the near-tree structure**: With density 0.000239, the graph may be too sparse for standard GNNs to propagate useful information. Consider adding synthetic edges (k-nearest-neighbors in feature space) or using a Transformer-style architecture that doesn't rely on graph edges.

---

*Generated from analysis of `workbooks/run_pipeline.ipynb` and supporting source files in `python_model/core/`.*
