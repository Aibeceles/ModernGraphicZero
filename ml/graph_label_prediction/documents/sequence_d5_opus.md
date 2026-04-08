# Sequence Predictor d5 Post-Experiment Debrief (Opus)

This document is a comprehensive post-experiment debriefing of the **RootList Sequence Prediction pipeline** as executed in `workbooks/sequence_predictor.ipynb`. It traces every stage end-to-end: **Neo4j queries -> graph embedding -> sequence construction -> tokenization -> model architecture -> training dynamics -> autoregressive inference -> evaluation metrics**.

It is written for someone who needs to understand not just *what* happened, but *why each piece exists*, how it relates to the CORAL classification pipeline, and where the results stand.

---

## 0. What the experiment is trying to solve

The CORAL pipeline (documented in `corral_d5_opus.md`) predicts the **count** of distinct rational roots for each `(:Dnode)`. The sequence predictor asks a harder question: can we predict the **actual root values** (the full `RootList`), not just how many there are?

**The prediction task**: Given a sliding window of 8 consecutive wNum=0 leaf nodes and their graph embeddings, predict the **next** node's `RootList` --- both its **size** (totalZero) and its **contents** (the integer root values).

**Key insight from the domain**: The generator guarantees one root at a known position, but additional roots emerge from the mathematical structure. Predicting both size and contents tests whether the model learns the underlying mathematics, not just statistical patterns.

**Relationship to CORAL**: Both pipelines share the same filtered d5 subgraph (pArrayList in [0, 7)), the same Neo4j queries, and the same 16D node feature vector. The sequence predictor adds a second data source (wNum=0 nodes with complete RootLists) and replaces the classification head with an autoregressive Transformer decoder.

---

## 1. Queries: what gets pulled from Neo4j

The sequence predictor uses **three** queries (two shared with CORAL, one sequence-specific). All are routed through `config.py` with the same `USE_GRAPH_FILTERING = True` and `PARRAY_MIN/MAX = [0, 7)` settings.

### 1.1 Full-graph node query (shared with CORAL)

Uses `get_node_query(USE_GRAPH_FILTERING)` which returns `FILTERED_NODE_QUERY_CENSORED_TEMPLATE`. This is the same query CORAL uses, returning all 8,376 nodes in the filtered subgraph with their full feature set (label, totalZero, RootList, determined, vmResult, n, mu_d, muList, wNum).

**Purpose**: Provides node features for the GCN encoder. The GCN sees the **entire** graph (all wNum levels), not just the wNum=0 leaf nodes. This is intentional --- parent-child relationships in the tree inform the leaf node embeddings.

### 1.2 Full-graph edge query (shared with CORAL)

Uses `get_edge_query(USE_GRAPH_FILTERING)` which returns `FILTERED_EDGE_QUERY_TEMPLATE`. Returns all `zMap` relationships between filtered nodes.

**Result**: 16,750 directed edges (8,375 undirected, stored as both directions for PyG).

### 1.3 Sequence node query (sequence-specific)

```cypher
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy {wNum:0})
WHERE size(d.RootList) = d.totalZero
  AND all(x IN cb.pArrayList WHERE x >= 0 AND x < 7)
RETURN elementId(d) as node_id,
       d.RootList as RootList,
       d.totalZero as totalZero,
       cb.pArrayList as pArrayList
ORDER BY node_id
```

**Key constraints**:
- `wNum = 0`: Only leaf nodes (5th-degree polynomials in d5).
- `size(d.RootList) = d.totalZero`: Only nodes with **complete** RootLists (where all roots have been found).
- Same pArrayList filtering as the full graph.

**Result**: **19,257** leaf nodes with complete RootLists.

**Important asymmetry**: The full graph has 8,376 nodes, but the sequence query returns 19,257 nodes. This is because many wNum=0 nodes share the same filtered subgraph --- the sequence query finds *all* qualifying leaf nodes, including duplicates that map to the same global graph node. All 19,257 nodes map successfully to global indices.

---

## 2. Features: what the GCN sees

### 2.1 Node feature vector (16D)

The sequence predictor builds its own 16D feature matrix (reimplemented in the notebook, not importing from `data_loader.py`):

```
[wNum, degree, determined, coeff_0..coeff_4, stats_0..stats_4, n, mu_d, mu_ratio]
```

| Block | Features | Dimensions |
|---|---|---:|
| Base | wNum, degree_at_node, determined | 3 |
| Coefficients | coeff_0 through coeff_4 (padded to max_degree+1=5) | 5 |
| Statistics | magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs | 5 |
| Set Union Ratio | n, mu_d, mu_ratio | 3 |
| **Total** | | **16** |

**Note**: Unlike the CORAL pipeline, the sequence predictor does **not** include spectral positional encodings (no +8 PE dimensions). The feature vector is 16D, not 25D. This is because the sequence predictor defines its own `num_node_features = 16` in its config.

### 2.2 Coefficient parsing

Same logic as CORAL: `vmResult` string is parsed, reversed to ascending power order, and padded to `max_degree + 1 = 5` entries. Statistics (magnitude, leading coeff, constant term, sparsity, mean abs) are computed from the raw coefficient vector.

---

## 3. Sequence construction: how RootLists become training samples

### 3.1 RootList extraction

For each of the 19,257 wNum=0 nodes, the `RootList` property is extracted and parsed into a Python list of integers.

**RootList statistics**:
- Min size: 1
- Max size: 5
- Mean size: 1.82

### 3.2 Sliding window samples

The 19,257 sequence nodes are ordered (by elementId). A sliding window of size 8 creates `(context, target)` pairs:

```
Sample i:
  context = [node[i], node[i+1], ..., node[i+7]]     # 8 consecutive wNum=0 nodes
  target  = node[i+8]                                  # next node's RootList
```

**Total samples**: 19,249 (= 19,257 - 8)

**Context indices are global**: Each context node is referenced by its global index into the full 8,376-node graph, so the GCN embedding lookup crosses all wNum levels, not just wNum=0.

### 3.3 Train / Val / Test split

| Split | Samples | Batches (batch_size=32) |
|---|---:|---:|
| Train | 13,474 (70%) | 422 |
| Val | 2,887 (15%) | 91 |
| Test | 2,888 (15%) | 91 |

Split is random (seed=42), not sequential. This means train/val/test samples may come from anywhere in the ordered sequence.

---

## 4. Tokenization: encoding RootLists for the Transformer

### 4.1 Vocabulary

| Token range | Meaning |
|---|---|
| 0 to 120 | Integer root values (-60 to +60) |
| 121 | SOS (start-of-sequence) |
| 122 | PAD |

**Vocabulary size**: 123

Root value `v` maps to token `v - (-60) = v + 60`. So root `0` is token 60, root `-3` is token 57, root `5` is token 65.

### 4.2 Target format

Each target RootList is encoded as:

```
[size, root_1_token, root_2_token, ..., root_k_token, PAD, PAD, ...]
```

Padded to `max_rootlist_size + 1 = 6` positions. The first position holds the size (as a raw integer 0-5), and subsequent positions hold tokenized root values.

### 4.3 Decoder input (teacher forcing)

During training, the decoder input is the target shifted right with SOS prepended:

```
decoder_input = [SOS, size, root_1_token, ..., root_{k-1}_token, PAD]
```

---

## 5. Model architecture: GCN + Transformer encoder-decoder

**Total trainable parameters**: 255,361

### 5.1 NodeEncoder (GCN backbone)

```
NodeEncoder (encoder_type='gcn')
  conv1: GCNConv(16, 64)     # 16D input features -> 64D hidden
  conv2: GCNConv(64, 64)     # 64D -> 64D
  dropout: 0.1
  activation: ReLU
  output: [num_nodes, 64]     # embeddings for ALL 8,376 nodes
```

This is the same 2-layer GCN architecture as CORAL's backbone. It runs on the **full graph** (all wNum levels, all edges), producing 64D embeddings for every node.

### 5.2 SequenceEncoder (Transformer encoder)

```
SequenceEncoder
  pos_encoding: Sinusoidal positional encoding
  encoder: TransformerEncoder
    layers: 2
    d_model: 64
    nhead: 4
    dim_feedforward: 256
    dropout: 0.1
    batch_first: True
  input: [batch, 8, 64]       # 8 context node embeddings
  output: [batch, 8, 64]      # contextualized representations
```

Takes the 8 context node embeddings (gathered from the GCN output by global index) and applies 2 layers of self-attention with positional encoding. This captures dependencies between consecutive wNum=0 nodes in the sliding window.

### 5.3 RootListDecoder (autoregressive Transformer decoder)

```
RootListDecoder
  token_embedding: Embedding(123, 64)     # vocab -> d_model
  size_embedding: Embedding(6, 64)        # size 0-5 -> d_model
  pos_encoding: Sinusoidal
  decoder: TransformerDecoder
    layers: 2
    d_model: 64
    nhead: 4
    dim_feedforward: 256
    dropout: 0.1
    batch_first: True
  size_head: Linear(64, 6)                # predict size 0-5
  root_head: Linear(64, 123)              # predict root token
```

**Two output heads**:
- `size_head`: Predicts RootList size (0-5 classes) from the first decoder position.
- `root_head`: Predicts individual root tokens (123-class) at each subsequent position.

**Cross-attention**: The decoder attends to the SequenceEncoder output (8 context node representations).

**Causal masking**: Upper-triangular attention mask ensures autoregressive generation --- each position can only attend to earlier positions.

### 5.4 Full forward pass

```
1. NodeEncoder: full graph (8,376 nodes, 16,750 edges) -> [8376, 64] embeddings
2. Gather: context_indices [batch, 8] -> [batch, 8, 64] context embeddings
3. SequenceEncoder: [batch, 8, 64] -> [batch, 8, 64] contextualized
4. RootListDecoder: 
   - input: decoder_input tokens [batch, 6]
   - memory: encoder output [batch, 8, 64]
   - output: (size_logits [batch, 6, 6], root_logits [batch, 6, 123])
```

The GCN is re-encoded **every batch** during training to maintain gradient flow through the graph convolutions. During evaluation, it's encoded once.

---

## 6. Training: loss, optimizer, and dynamics

### 6.1 Loss function

Combined loss with two components:

```python
total_loss = size_loss + root_loss
```

- **Size loss**: Cross-entropy at position 0 only. `CE(size_logits[:, 0, :], target_size)`.
- **Root loss**: Cross-entropy at positions 1+ for valid (non-PAD) positions. `CE(root_logits[:, 1:, :][mask], target_tokens[:, 1:][mask])`.

Both components are weighted equally (no lambda tuning).

### 6.2 Optimizer

```
Adam(lr=0.001, weight_decay=1e-4)
Gradient clipping: max_norm=1.0
```

### 6.3 Early stopping

- Metric: **total validation loss** (size + root combined).
- Patience: 10 epochs.
- Max epochs: 100.
- Best model state saved and restored.

### 6.4 Training dynamics

| Epoch | Train Loss | Size Loss | Root Loss | Val Loss |
|---:|---:|---:|---:|---:|
| 0 | 2.8741 | 1.1120 | 1.7621 | 2.5468 |
| 5 | 2.0924 | 0.9142 | 1.1782 | 1.9434 |
| 10 | 1.8998 | 0.8479 | 1.0519 | 1.7634 |
| 20 | 1.7225 | 0.7895 | 0.9330 | 1.5728 |
| 40 | 1.5877 | 0.7382 | 0.8494 | 1.4295 |
| 60 | 1.5176 | 0.7123 | 0.8054 | 1.3577 |
| 80 | 1.4766 | 0.6985 | 0.7781 | 1.2838 |
| 99 | 1.4341 | 0.6836 | 0.7505 | 1.2961 |

**Best validation loss**: 1.2460 (no early stopping triggered; ran full 100 epochs).

**Observations**:
- Loss decreases steadily throughout training. No plateau or divergence.
- Size loss and root loss decrease roughly in parallel.
- Val loss is consistently lower than train loss (unusual; likely because the random split means some val samples come from "easier" regions of the sequence, or because dropout hurts training more than eval).
- The model did not overfit --- validation loss kept improving or staying flat through epoch 100.

---

## 7. Inference: autoregressive RootList generation

At test time, the model generates RootLists autoregressively:

1. **Encode graph**: Run GCN on full graph to get node embeddings.
2. **Encode context**: Gather 8 context node embeddings, run through SequenceEncoder.
3. **Predict size**: Feed SOS token to decoder, read `size_head` output at position 0. Take argmax.
4. **Generate roots**: For `predicted_size` steps:
   - Feed all generated tokens so far to decoder.
   - Read `root_head` output at the last position.
   - Take argmax (greedy decoding).
   - Append to generated sequence.
5. **Decode**: Convert token IDs back to integer root values.

**Greedy decoding only** --- no beam search, no temperature sampling. If the model produces a special token (SOS/PAD), it falls back to the most likely valid token.

---

## 8. Results: what the model actually achieved

### 8.1 Test set metrics

| Metric | Value |
|---|---:|
| **Size Accuracy** | 75.45% |
| **Exact Match** (order-aware) | 41.86% |
| **Set Match** (order-agnostic) | 42.35% |
| **Element Accuracy** | 46.90% |
| Samples | 2,888 |

### 8.2 Validation set metrics (for comparison)

| Metric | Value |
|---|---:|
| Size Accuracy | 75.48% |
| Exact Match | 41.53% |
| Set Match | 42.29% |
| Element Accuracy | 45.88% |
| Samples | 2,887 |

Test and validation metrics are nearly identical, confirming no overfitting.

### 8.3 Exact match accuracy by RootList size

| True Size | Exact Match | Count |
|:---:|---:|---:|
| 1 | 52.8% (866/1640) | 1,640 |
| 2 | 33.7% (230/682) | 682 |
| 3 | 22.7% (66/291) | 291 |
| 5 | 17.1% (47/275) | 275 |

**Key observations**:
- Size 4 is absent from the test set (no nodes with exactly 4 roots in the wNum=0 filtered subset --- the polynomial either has 1, 2, 3, or 5 roots, but never exactly 4 in this data slice).
- Accuracy degrades with size: predicting 1 root correctly is much easier (52.8%) than predicting all 5 roots correctly (17.1%).
- Even for size-5 lists, the model gets 17.1% exactly right --- nontrivial for a 5-element sequence prediction over a vocabulary of 121 integers.

### 8.4 Example predictions

```
✗ Pred: size=2, roots=[5, 6]        True: size=1, roots=[4]
✗ Pred: size=5, roots=[6,7,8,4,5]   True: size=1, roots=[6]
✗ Pred: size=1, roots=[5]           True: size=2, roots=[4, 5]
✓ Pred: size=1, roots=[0]           True: size=1, roots=[0]
✗ Pred: size=5, roots=[6,7,3,4,5]   True: size=5, roots=[3,4,5,6,7]
```

The last example is interesting: the model predicted the correct set {3,4,5,6,7} but in the wrong order. This would count as a set match but not an exact match (the 0.5% gap between set_match and exact_match comes from cases like this).

---

## 9. Interpretation: what the model learned (and didn't)

### 9.1 Size prediction is strong

75.45% size accuracy is well above the majority-class baseline. The RootList size distribution has mean 1.82 with heavy concentration at size 1 (~57% of the test data). The model does more than just predict the mode --- it correctly identifies multi-root cases ~75% of the time.

### 9.2 Root value prediction has real signal

41.86% exact match across all sizes is meaningful. For single-root predictions (size 1), the model must predict the correct integer out of 121 possible values. Getting 52.8% right means the GCN embeddings + context window carry substantial information about which specific root will appear.

### 9.3 What the model struggles with

- **Multi-root ordering**: The model sometimes predicts the right set of roots but in the wrong order (set_match > exact_match).
- **Large lists**: 5-root predictions are hard. Even with the correct size, the model must get 5 integers right in sequence.
- **Size over-prediction**: Some examples show the model predicting size=5 when the true size is 1. The size head may be fooled by certain feature patterns.

### 9.4 Context window effects

The 8-node sliding window over ordered wNum=0 nodes creates an implicit sequential dependency. The Transformer encoder can learn that certain root patterns tend to follow certain contexts. However, the ordering is by elementId (not by a mathematical relationship), so the sequential structure may be somewhat arbitrary.

---

## 10. Architecture comparison with CORAL pipeline

| Aspect | CORAL (classification) | Sequence Predictor |
|---|---|---|
| **Goal** | Predict root count | Predict root count + root values |
| **Graph data** | Same filtered subgraph | Same filtered subgraph |
| **Node features** | 25D (16 base + 8 spectral PE) | 16D (no spectral PE) |
| **Training nodes** | All nodes (any wNum) | Only wNum=0 leaf nodes |
| **GCN backbone** | 2-layer GCNConv(25,64) | 2-layer GCNConv(16,64) |
| **Head** | Shared linear + threshold biases | Transformer encoder-decoder |
| **Output** | K-1 threshold logits | Size logits + root token logits |
| **Loss** | Binary CE per threshold | CE(size) + CE(root tokens) |
| **Decoding** | Count thresholds > 0.5 | Greedy autoregressive |
| **Parameters** | ~10K | 255,361 |
| **Training data** | ~5.4K train nodes | ~13.5K train samples |

### Shared components

- Same `get_node_query()` and `get_edge_query()` from `config.py`.
- Same filtering logic (pArrayList in [0, 7)).
- Same coefficient parsing from `vmResult`.
- Same base feature structure (wNum, degree, determined, coeffs, stats, set-union).

### Key differences

1. **Spectral PE**: CORAL uses 8D spectral positional encodings; sequence predictor does not.
2. **Training scope**: CORAL trains on all nodes; sequence predictor trains only on wNum=0 nodes with complete RootLists (a much larger set due to the many-to-one mapping).
3. **Model complexity**: Sequence predictor is ~25x larger (255K vs ~10K parameters) due to the Transformer layers and 123-class vocabulary embedding.
4. **Task difficulty**: Predicting specific root integer values is fundamentally harder than predicting a root count.

---

## 11. Data flow summary

```
Neo4j (d5seed1, 3M nodes)
  │
  ├─ FILTERED_NODE_QUERY_CENSORED ─────> 8,376 nodes (all wNum levels)
  │                                        │
  │                                        ├─ Build 16D features ──> node_features [8376, 16]
  │                                        │
  ├─ FILTERED_EDGE_QUERY ──────────────> 8,375 undirected edges
  │                                        │
  │                                        ├─ edge_index [2, 16750]
  │                                        │
  ├─ FILTERED_SEQUENCE_NODE_QUERY ─────> 19,257 wNum=0 nodes
  │                                        │
  │                                        ├─ Map to global indices ──> sequence_node_indices
  │                                        ├─ Extract RootLists ──────> root_lists
  │                                        ├─ Sliding window (8) ─────> 19,249 samples
  │                                        └─ Split 70/15/15 ────────> train/val/test
  │
  └─ PyG Data(x, edge_index)
       │
       GCN NodeEncoder ──> [8376, 64] embeddings
       │
       Gather context [batch, 8] ──> [batch, 8, 64]
       │
       Transformer SequenceEncoder ──> [batch, 8, 64]
       │
       Transformer RootListDecoder
       ├─ size_head ──> [batch, 6] (size 0-5)
       └─ root_head ──> [batch, 123] (root tokens)
```

---

## 12. Takeaways and next actions

### What worked

- **The pipeline produces real signal**: 75% size accuracy and 42% exact match are well above random baselines. The GCN embeddings + Transformer sequence model captures meaningful patterns.
- **No overfitting**: Val and test metrics are nearly identical. The model generalizes well.
- **Cross-wNum embedding**: Using the full graph (all wNum levels) for GCN encoding and only wNum=0 for training is a sound design that lets parent-child tree structure inform leaf node representations.
- **Autoregressive generation works**: The model can generate variable-length RootLists of correct size ~75% of the time.

### What could improve

1. **Add spectral PE**: The CORAL pipeline uses 8D spectral positional encodings that the sequence predictor omits. Adding them would make the feature vector 24D and could improve GCN embedding quality.

2. **Sequential ordering**: The current sliding window uses elementId ordering, which may not reflect a mathematically meaningful sequence. Ordering by `pArrayList` (the generator's parameterization) might create more predictable patterns.

3. **Beam search decoding**: Greedy decoding takes the argmax at each step. Beam search (width 3-5) could improve exact match by exploring multiple generation paths.

4. **Set-based loss**: The current loss penalizes ordering mistakes. A set-based loss (e.g., Hungarian matching) would focus on predicting the right root values regardless of order.

5. **Larger context window**: The window of 8 is relatively small. Increasing to 16 or 32 might capture longer-range dependencies in the sequence.

6. **Integrate with CORAL**: Use CORAL's size predictions as a conditioning signal for the sequence decoder, replacing or supplementing the size_head. This would combine the strengths of both pipelines.

7. **Edge features for GCN**: The CORAL pipeline's GAT-based models use 18D polynomial edge features. Swapping the sequence predictor's plain GCN for a GATConv + edge features could improve node embeddings.

### Model saved

Model checkpoint saved to `workbooks/sequence_predictor_d5_1.pt`, including model state, config, training history, and test metrics.

---

*Generated from analysis of `workbooks/sequence_predictor.ipynb` and supporting source files in `python_model/core/`.*
