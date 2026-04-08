## Hugging Face Release Notes (`GraphicZero` / GraphMLDifferences)

This document is a checklist for packaging a **credible Hugging Face (HF)** model release around the polynomial-difference graph.

---

### What to Release (v1 scope)

- **Node classification model**: predict `determined ∈ {0,1}` for a polynomial/difference node.
- **(Optional) Link prediction model(s)**:
  - **Structural**: predict missing `:zMap` edges (reconstruction / completion).
  - **Derived** (research): predict `:NEXT_INTEGER` edges (ordering determined nodes by χ encoding).
  - **Derived** (research): predict `:SAME_RATIONAL` edges (partition determined nodes by μ = n/d).

---

### Model Size Targets (practical + reproducible)

- **Baseline (non-graph)**:
  - Logistic regression / small MLP on engineered features (e.g., parsed `vmResult`, degree, `totalZero`).
  - Good for a “sanity check” artifact.

- **Graph embedding + head (recommended HF v1)**:
  - **GraphSage embeddings**: **64-d** (or **128-d**) node embeddings.
  - **Classifier head**: 2-layer MLP, hidden **128–256**, dropout 0.1–0.3.
  - Typical parameter count: **~50k–500k** (depends on feature dims).

- **Full GNN (optional v2)**:
  - GraphSAGE (PyTorch Geometric), **2–3 layers**, hidden **128**, output emb **64/128**.
  - Typical parameter count: **<1–5M** (feature-dependent).

---

### Training Details HF Users Expect

#### Data splits
- **Node classification**:
  - Train/val/test split with **stratification** on `determined` (handles class imbalance).
  - Alternatively, **k-fold** (report k and aggregation).

- **Link prediction**:
  - Hold out a fraction of edges for test (and optionally validation).
  - Report **negative sampling ratio** (e.g., 1.0).

#### Metrics to report
- **Node classification**:
  - **F1 (weighted)**, **F1 (macro)**, accuracy.
  - Confusion matrix + class prevalence.

- **Link prediction**:
  - **AUCPR** (primary), ROC-AUC.
  - Precision@K / Recall@K for top-K predicted edges.

#### Hyperparameters worth logging
- Embedding dim (64/128), GraphSage aggregator (mean/pool), neighborhood sample sizes, depth.
- Epochs, batch size, learning rate, regularization/penalty.
- Random seeds and how many runs averaged.

#### Compute + reproducibility
- State CPU/GPU hardware used; training time.
- Pin versions (Neo4j, GDS, Python, PyTorch, etc.).
- Publish seed(s), dataset checksum/hash, and preprocessing steps.

---

### What to Put in the HF Repo

#### 1) Model card (`README.md`)
- What the model predicts (task + labels).
- Training data provenance:
  - generation bounds (`dimension`, `integerRange`, `setProductRange`),
  - how nodes/edges are produced (`:Dnode`, `:zMap`),
  - what “determined” means.
- Metrics on test set + split protocol.
- Intended use + limitations (bounded range, floating precision artifacts, non-injective μ, etc.).

#### 2) Artifacts
- **Weights**:
  - `model.safetensors` (PyTorch) or equivalent.
  - If embeddings are precomputed: ship `node_embeddings.npy` (+ metadata mapping node IDs).
- **Config**:
  - `config.json` (dims, aggregator, feature list).
- **Tokenizer-equivalent** (for graphs):
  - feature normalization stats (means/stds) if used.

#### 3) Data loader / conversion script
- A script to convert:
  - Neo4j export (`graphsample.json` / CSV) → training tensors.
- Document how to reproduce from a Neo4j dump if available.

#### 4) Minimal inference demo
- **Input**: polynomial coefficients (or `vmResult`) and basic metadata.
- **Output**: `P(determined=1)` and predicted label.
- Optional: top-K link predictions for `NEXT_INTEGER` / `SAME_RATIONAL`.

#### 5) Licensing
- Separate licenses for:
  - code,
  - data,
  - model weights.

---

### “Interesting Angle” Unique to This Project (recommended)

Add an **optional verification step** that makes inference more than “generic GNN”:

- **Predict → Decode → Certify**
  - Model proposes a classification or link.
  - Decode χ (integer) and/or μ (rational) from `muList` / `n,d`.
  - Run a deterministic check (e.g., consecutive integer for `NEXT_INTEGER`, equality of rationals for `SAME_RATIONAL`).
  - If check fails, fallback (beam search over candidates or abstain).

This “hybrid ML + certificate” story is compelling and aligns with the project’s math foundations.

---

### Recommended HF “v1” Configuration (conservative)

- **Embeddings**: GraphSage, **64-d**, aggregator = pool/mean, depth = 2–3.
- **Node classification head**: MLP(64 + numeric features → 128 → 2).
- **Training**: 50–100 epochs, early stopping on val F1-macro.
- **Reporting**:
  - F1 weighted + macro, confusion matrix, class balance.
  - Seeded runs (≥3) with mean ± std.


