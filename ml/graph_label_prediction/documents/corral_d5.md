# CORAL d5 Post-Experiment Debrief

This note debriefs what the **CORAL ordinal regression pipeline** is doing in `workbooks/run_pipeline.ipynb`, end-to-end: **Neo4j queries → labels → features → hyperparameters → embeddings → predictions (and optional Neo4j write-back)**.

---

## What this experiment is trying to solve

We are predicting an **ordinal** target: the **number of distinct rational roots** for each `(:Dnode)`.

- **Why ordinal**: predicting “2 roots” when the truth is “3 roots” should be penalized less than predicting “0 roots”. CORAL encodes this via monotone thresholds rather than a flat \(K\)-way softmax.

---

## 1) Queries: what gets pulled from Neo4j

All data loading goes through `GraphDataLoader` in `python_model/core/data_loader.py`, which selects Cypher via `get_node_query()` and `get_edge_query()` from `python_model/core/config.py`.

### Nodes (features + labels)

The “censored/truncated” node query used by default is `NODE_QUERY_CENSORED` (or its filtered template). It returns (key fields):

- **Identity**
  - `elementId(d) as node_id` (string; used later for write-back)
- **Labels**
  - `label = coalesce(size(d.RootList), d.totalZero, 0)`
  - `totalZero`, `RootList`, `visibleRootCount`
  - `determined` (computed on a larger/full scan window; treated as an auxiliary regime flag)
- **Raw polynomial**
  - `vmResult` (coefficient list; parsed into degree + coefficient/stat features)
- **Set union ratio inputs**
  - `n`, `d as mu_d`, `muList`
- **Depth / level**
  - `cb.wNum as wNum` (the CreatedBy depth)
- **Window metadata** (if present)
  - `windowMin`, `windowMax`, `windowSize`

### Filtering (this is important for d5)

If `USE_GRAPH_FILTERING=True`, `GraphDataLoader` uses `FILTERED_NODE_QUERY_CENSORED_TEMPLATE` and constrains nodes by:

- `all(x IN cb.pArrayList WHERE x >= PARRAY_MIN AND x < PARRAY_MAX)`

In `python_model/core/config.py`:

- `PARRAY_MIN = 0`
- `PARRAY_MAX = 7`

So the experiment is effectively “**d5 on the filtered subgraph defined by pArrayList ∈ [0, 7)**”.

### Edges (graph structure)

Edges come from zMap relationships, as an undirected edge list. In filtering mode, the query is `FILTERED_EDGE_QUERY_TEMPLATE` which returns pairs `(source, target)` as elementId strings, then the loader:

- maps elementIds → contiguous integer indices
- builds `edge_index` with **both directions** so PyG treats it as undirected

---

## 2) Labels: how root-count classes are defined and made ordinal

### The target used during training

In `GraphDataLoader.load()`:

- `labels = nodes_df['label'] ...` where `label = coalesce(size(RootList), totalZero, 0)`
- That means the default supervised target is **visible distinct root count** (in the expanded/truncated window), not necessarily the full “true” root count for every node.

The `Data` object stores:

- `data.y`: **contiguous** class indices for training
- `data.y_visible`: explicit copy of the same
- `data.totalZero` and `data.RootList`: stored for analysis/debugging (not used directly by models)

### Contiguous class remapping (critical for CORAL)

Raw root counts in Neo4j are not guaranteed to be a dense set (e.g., you might see only `{1,2,3,4,5}` on a filtered slice). CORAL and CE training require labels in `[0..K-1]`, so the loader builds:

- `classes_present = sorted(unique(y_raw))`
- `raw_to_contiguous = {raw_val -> idx}`
- `y_mapped = raw_to_contiguous[label]`

and stores metadata on `data`:

- `data.y_raw`: original raw labels (root counts)
- `data.class_values`: list of raw values in order
- `data.num_classes`: \(K\)
- `data.raw_to_contiguous`: mapping raw → contiguous

**Implication**: any “root count” you see inside training/metrics is in *contiguous label space* unless explicitly decoded back through `data.class_values`.

---

## 3) Features: what goes into the model (and why)

The feature matrix is constructed in `GraphDataLoader._build_feature_matrix()` as:

1. **`wNum`**: CreatedBy depth (float32)
2. **`degree_at_node`**: derived from `vmResult` via `extract_coefficient_features()`
3. **`determined`**: binary flag (float32; also stored as `data.determined` bool)
4. **coefficients**: padded coefficient vector up to `MAX_POLYNOMIAL_DEGREE + 1`
5. **coefficient statistics**: magnitude, leading abs, constant abs, sparsity, mean abs
6. **set union ratio features**: `n`, `mu_d`, `mu_ratio = n / mu_d` (safe-divided)

So base feature block is:

- `[wNum, degree, determined, coeff_0..coeff_D, stats_0..stats_4, n, mu_d, mu_ratio]`

### Spectral positional encodings (PE)

After building `Data(x, edge_index, y)`, the loader optionally appends Laplacian eigenvector features:

- configured by `SPECTRAL_PE_DIM` and `MAX_NODES_FOR_SPECTRAL_PE`
- if the graph is too large, it **pads zeros** to keep the expected dimension stable

In `python_model/core/config.py` for this pipeline:

- `SPECTRAL_PE_DIM = 8`
- `MAX_NODES_FOR_SPECTRAL_PE = 100000`

So in the filtered d5 setting, you either compute real spectral PE (if \(N \le 100k\)) or append 8 zeros per node.

---

## 4) Hyperparameters: how the run is configured

### Global defaults (config)

From `python_model/core/config.py`:

- **model width**: `HIDDEN_DIM = 64`
- **dropout**: `DROPOUT_RATE = 0.3`
- **optimizer**: Adam with `LEARNING_RATE = 0.01`
- **penalties (grid)**: `PENALTIES = [1e-4, 1e-3, 1e-2, 0.0625]` (interpreted as Adam `weight_decay`)
- **loss type**: `LOSS_TYPE = 'emd'` (note: CORAL overrides loss handling in trainer)
- **target mode**: `TARGET_MODE = 'visible'`

The trainer also computes **class weights** from the *training split only* using `compute_class_weights(method='sqrt')` (sqrt inverse frequency, normalized).

### What the notebook actually passed for CORAL

In `run_pipeline.ipynb`, CORAL training is invoked as:

- `model_name='coral'`
- `weight_decay=1e-3`
- `max_epochs=200`
- `patience=20`

Early stopping is driven by **validation macro F1** (not train, not test).

---

## 5) CORAL model: what “ordinal regression” means here

Implemented in `python_model/core/models.py` as `CORALClassifier`.

### Forward outputs

Instead of producing \(K\) class logits, CORAL produces \(K-1\) **threshold logits**:

- \(t_k \approx \Pr(y > k)\) for \(k = 0..K-2\)

Implementation details:

- **backbone**: 2-layer `GCNConv` feature extractor
- **CORAL head**:
  - shared linear projection `fc(h)` (no bias)
  - per-threshold learnable biases `threshold_biases[k]`
  - `logits[:, k] = fc(h) + threshold_biases[k]`

### Loss

In `NodeClassificationTrainer._compute_coral_loss()` (`python_model/core/trainer.py`):

- build binary targets: `targets[:, k] = 1[label > k]`
- apply `binary_cross_entropy_with_logits(logits[mask], targets[mask])`

### Prediction

Class prediction is “how many thresholds exceed 0.5”:

- `pred = sum(sigmoid(t_k) > 0.5)`

This is implemented both in `CORALClassifier.predict_classes()` and in trainer helper `_get_coral_predictions()`.

---

## 6) Embeddings: what is being visualized and how

The notebook defines embedding helpers (in the “Embedding Visualization” section):

- **`extract_embeddings(model, data, layer_name='conv2')`**
  - uses a forward hook on `model.conv2` to capture hidden activations
- **`reduce_embeddings(H, method=...)`**
  - `PCA`, `t-SNE`, and optionally `UMAP`

Predictions used alongside embedding plots are produced by a helper that special-cases CORAL:

- if the model has `predict_classes`, it uses that (CORAL / regression)
- otherwise, it uses `argmax` on classifier outputs

This matters because CORAL’s forward pass returns **threshold logits**, not class logits.

---

## 7) Predictions: metrics, diagnostics, and Neo4j write-back

### In-notebook evaluation path (used for the experiment readout)

Metrics and reports are produced via `NodeClassificationTrainer`:

- confusion matrix (optionally normalized by true-class row)
- detailed per-class report (precision/recall/F1)
- prediction distribution diagnostics (detect “class collapse”)
- MAE in “roots” units: `mean(|pred - true|)` in contiguous class space

### What happened in this CORAL run (d5)

From the experiment output you pasted (test split):

- **Predictions collapsed** almost entirely to **“1 root”**.
- Confusion matrix shows:
  - true `2..5 roots` → predicted `1 root` (100% in those rows)
- Prediction diagnostics report:
  - only one class predicted (others show “⚠️ COLLAPSED”)

Interpretation:

- The CORAL thresholds did not separate the higher-count regimes; the model learned a degenerate solution that satisfies loss on the dominant regime.

### Neo4j write-back path (caution for CORAL)

Write-back is implemented in `python_model/core/predictor.py` (`NodePredictor` / `predict_and_write`).

Important caveats for CORAL:

- `NodePredictor.predict()` currently assumes:
  - `out` is **log-softmax** over classes
  - `pred = out.argmax(...)`
  - `prob = exp(out)`
- CORAL instead returns **threshold logits**, so:
  - `argmax` is meaningless
  - `exp(out)` is not a class probability distribution
- Additionally, labels are trained in **contiguous space**, so writing raw `pred` back to Neo4j may write *indices*, not true root counts, unless you decode via `data.class_values`.

If you plan to write CORAL predictions back, the correct approach is:

- `pred_contig = coral_model.predict_classes(...)`
- `prob_contig = coral_model.get_probabilities(...)`
- `pred_raw = data.class_values[pred_contig]` (vectorized decode)

---

## Takeaways / next actions (high-signal)

- **The pipeline plumbing is correct**: queries, feature construction (incl. `vmResult` parsing + set-union features + spectral PE), ordinal target mapping, and CORAL loss/prediction all line up.
- **The failure mode is “class collapse”**: CORAL is not currently producing meaningful thresholds on d5 (it predicts essentially one class).
- **If you want CORAL in production**, update the write-back path (`predictor.py`) to:
  - support CORAL predictions + probabilities
  - decode contiguous → raw root counts before `SET d.predictedRootCount = ...`

Potential modeling fixes (beyond this debrief, but directly implied by the collapse):

- add **threshold-wise weighting** (later thresholds correspond to rarer “high root count” regimes)
- use **mini-batch + weighted sampling** (`NeighborLoader` + `WeightedRandomSampler`) for better minority gradient signal
- consider **DepthAwareGAT backbone + CORAL head** (keep ordinal head, improve representation)

