# Sequence Prediction Theory: RootList Forecasting

## Overview

This document describes the machine learning theory behind the `sequence_predictor.ipynb` pipeline, which predicts RootList values for polynomial nodes in a mathematical graph. Unlike the classification task in `run_pipeline.ipynb` (which predicts root *count*), this pipeline treats RootList prediction as a **sequence-to-sequence** problem.

---

## Problem Formulation

### The Prediction Task

Given a sequence of polynomial nodes ordered by their generation (reflected in `pArray` ordering), predict the RootList of the next node.

**Input:** Context window of 8 prior leaf nodes (wNum=0)
**Output:** Complete RootList prediction via **dual-head architecture**:
1. **Size prediction (totalZero):** Root count as 5-class classification (0-4)
2. **Root value prediction:** Individual root values as token sequence

### Mathematical Structure

Each `Dnode` in the graph represents a polynomial with:
- **RootList**: An ordered list of rational roots (integers in range [-20, 20])
- **totalZero**: The count of roots (size of RootList), ranging from 0 to 4
- **determined**: Boolean indicating if the root count is certain

The generator algorithm guarantees:
1. One root at a known position (mathematically determined)
2. Additional roots emerge from the underlying algebraic structure

This creates a **partially deterministic, partially emergent** pattern that sequence models can potentially learn.

---

## Architecture Design

### Why Sequence Prediction?

The traditional classification approach predicts only the *count* of roots (totalZero). Sequence prediction extends this with a **dual-task formulation**:

| Approach | Predicts | Captures |
|----------|----------|----------|
| Classification | `totalZero` (0-4) | Root count distribution |
| Sequence | `size` + `[r₁, r₂, ..., rₖ]` | Count + exact root values |

**Key insight:** The sequence model predicts root count (size) **and** root values as separate but joint tasks:
- **Size head:** Directly comparable to the CORAL/classification model
- **Root head:** Tests whether the model learns algebraic relationships

**Hypothesis:** If the model can predict specific root values (not just count), it has learned something about the underlying mathematical relationships. Comparing size accuracy between the sequence model and the standalone classifier validates that the encoder-decoder architecture doesn't sacrifice count prediction quality.

### Three-Stage Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FULL GRAPH                                │
│   (All nodes: wNum = 0, 1, 2, 3, ...)                           │
│   (All zMap edges)                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   1. NODE ENCODER (GCN/GAT)                      │
│                                                                  │
│   Input: Node features [N, 16]  + Edge index [2, E]             │
│   Output: Node embeddings [N, hidden_dim]                        │
│                                                                  │
│   Key: Uses FULL graph structure for rich embeddings            │
│   - Parent-child relationships via zMap edges                    │
│   - Depth information (wNum) encoded in features                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │  Filter wNum=0     │
                    │  (Leaf nodes only) │
                    └────────┬───────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                2. SEQUENCE ENCODER (Transformer)                 │
│                                                                  │
│   Input: Context window [batch, 8, hidden_dim]                  │
│          (8 consecutive leaf node embeddings)                    │
│   Output: Encoded context [batch, 8, hidden_dim]                │
│                                                                  │
│   Components:                                                    │
│   - Sinusoidal positional encoding                              │
│   - Multi-head self-attention (4 heads)                         │
│   - Feed-forward layers                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│               3. ROOTLIST DECODER (Transformer)                  │
│                                                                  │
│   Input: Encoded context + decoder tokens                        │
│                                                                  │
│   DUAL-HEAD OUTPUT:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │  size_head: Linear(hidden_dim → 5)                      │   │
│   │  → size_logits [batch, seq, 5] (predicts 0-4)          │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │  root_head: Linear(hidden_dim → 43)                     │   │
│   │  → root_logits [batch, seq, 43] (predicts root tokens) │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│   Generation: Autoregressive with teacher forcing               │
│   - Position 0: Use size_head to predict totalZero (0-4)        │
│   - Position 1+: Use root_head to predict root values           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Full Graph Embeddings

### The Key Innovation

Unlike approaches that only use leaf nodes, this architecture computes GCN embeddings on the **full graph** while training only on leaf nodes.

**Rationale:**
- `zMap` edges connect nodes across depth levels (wNum values)
- Parent nodes (wNum > 0) contain information about the polynomial derivation
- GCN message passing propagates this structural information to leaf nodes
- Leaf node embeddings become "contextually enriched" by their ancestry

### Data Flow

```
Full Graph (152K nodes)     →  GCN  →  All Embeddings (152K × 64)
                                              ↓
                                    Filter to wNum=0 indices
                                              ↓
                               Leaf Embeddings (~30K × 64)
                                              ↓
                                    Sliding Window
                                              ↓
                            Training Samples (context → target)
```

---

## Tokenization Scheme

### Vocabulary Design

The RootList prediction uses a discrete vocabulary:

| Token ID | Meaning |
|----------|---------|
| 0-40 | Integer values -20 to +20 |
| 41 | SOS (Start of Sequence) |
| 42 | PAD (Padding) |

**Total vocabulary size:** 43 tokens

### Target Sequence Format

Each RootList is encoded as:

```
[size, root₁, root₂, ..., rootₖ, PAD, PAD, ...]
```

**Example:**
- RootList `[-3, 0, 5]` with size 3
- Encoded: `[3, 17, 20, 25, PAD]` (token IDs)

The size is stored as the first token (handled by a separate prediction head), followed by the root value tokens.

---

## Training Methodology

### Teacher Forcing

During training, the decoder receives the shifted target sequence:

```
Decoder Input:  [SOS, size, root₁, root₂, ...]
Target Output:  [size, root₁, root₂, root₃, ...]
```

This allows parallel training of all positions while maintaining autoregressive structure for inference.

### Loss Function

**Multi-task loss** combining size prediction and root prediction:

```
L_total = L_size + L_root
```

**Size Loss (Root Count Classification):**
- Uses the `size_head` output at position 0
- 5-class cross-entropy (classes 0, 1, 2, 3, 4)
- Equivalent to the totalZero classification task

```python
size_pred = size_logits[:, 0, :]  # [batch, 5]
size_loss = CrossEntropy(size_pred, target_size)
```

**Root Loss (Root Value Prediction):**
- Uses the `root_head` output at positions 1+
- 43-class cross-entropy (tokens for [-20, 20] + special tokens)
- Masked to only compute loss for valid positions

```python
root_pred = root_logits[:, 1:, :]  # [batch, seq_len-1, 43]
root_loss = CrossEntropy(root_pred[mask], target_roots[mask])
```

**Training Metrics:**
Both losses are tracked separately during training:
- `train_size_loss` / `val_size_loss` - Monitors root count prediction
- `train_root_loss` / `val_root_loss` - Monitors root value prediction

### Sliding Window Construction

Training samples are created via sliding window over the leaf node sequence:

```
Nodes:    [n₀, n₁, n₂, n₃, n₄, n₅, n₆, n₇, n₈, n₉, ...]
           └─────── context ───────┘  │
                                      └─ target

Sample 1: context=[n₀..n₇], target=n₈
Sample 2: context=[n₁..n₈], target=n₉
...
```

**Window size:** 8 nodes (configurable)

---

## Theoretical Justification

### Why This Should Work

1. **Graph Structure Encodes Derivation History**
   - Each polynomial node is derived from parent nodes via mathematical operations
   - The zMap edges capture these derivation relationships
   - GCN message passing aggregates this historical context

2. **Sequence Patterns in Generation**
   - Nodes are ordered by their generation path (pArray)
   - Consecutive leaf nodes share common ancestry
   - Temporal patterns in RootList values should be learnable

3. **Transformer's Attention Mechanism**
   - Self-attention can learn long-range dependencies in the context window
   - Cross-attention allows the decoder to selectively focus on relevant context nodes
   - Positional encoding maintains order information

### Expected Outcomes

| Size Accuracy | Root Accuracy | Interpretation |
|---------------|---------------|----------------|
| High (≥ classifier) | Low | Model learns count patterns but not value patterns |
| High (≥ classifier) | High | Model has learned algebraic relationships |
| Low (< classifier) | Low | Encoder-decoder adds overhead without benefit |
| High | Better than random | Some algebraic structure is being captured |

**Success criteria:**
- Size accuracy should match or exceed the standalone CORAL classifier
- Root accuracy significantly above random (2.3%) indicates learned structure

### Baseline Comparisons

- **Random baseline:** 1/43 per token ≈ 2.3% accuracy
- **Frequency baseline:** Predict most common root values
- **Classification baseline:** Predict only size (from CORAL model)

---

## Model Configuration

### Hyperparameters

```python
@dataclass
class SequencePredictorConfig:
    # Context
    context_window_size: int = 8
    
    # Vocabulary
    min_root_value: int = -20
    max_root_value: int = 20
    max_rootlist_size: int = 4
    
    # Model
    encoder_type: str = 'gcn'  # or 'gat'
    hidden_dim: int = 64
    num_encoder_layers: int = 2
    num_decoder_layers: int = 2
    num_attention_heads: int = 4
    dropout: float = 0.1
    
    # Features
    num_node_features: int = 16
```

### Node Feature Vector (16D)

| Index | Feature | Description |
|-------|---------|-------------|
| 0 | wNum | Polynomial depth level |
| 1 | degree | Polynomial degree from vmResult |
| 2 | determined | Root count certainty flag |
| 3-7 | coeffs[5] | Polynomial coefficients |
| 8-12 | stats[5] | Coefficient statistics |
| 13 | n | μ numerator |
| 14 | mu_d | μ denominator |
| 15 | mu_ratio | Set union ratio |

---

## Relationship to Classification Pipeline

### Comparison

| Aspect | Classification (`run_pipeline.ipynb`) | Sequence (`sequence_predictor.ipynb`) |
|--------|---------------------------------------|---------------------------------------|
| **Target** | totalZero (count) | totalZero + RootList values |
| **Architecture** | GNN → Classification Head | GNN → Transformer Encoder-Decoder |
| **Size Prediction** | Single 5-class head | Dedicated `size_head` (5-class) |
| **Root Prediction** | N/A | Dedicated `root_head` (43-class) |
| **Output** | 5-class probability | Dual: size + token sequence |
| **Training Data** | All nodes (any wNum) | Leaf nodes only (wNum=0) |
| **Graph Usage** | Filtered subgraph | Full graph embeddings |

### Complementary Use

1. **Classification** provides fast, reliable root count predictions
2. **Sequence** predicts root count **plus** exact root values
3. The `size_head` output is directly comparable to the classification model
4. High root value accuracy would validate that the model learns algebraic structure

### Validation Strategy

Compare size prediction accuracy between models:
- **Sequence model's size_head** vs **Standalone CORAL classifier**
- If similar accuracy: encoder-decoder doesn't hurt count prediction
- If better accuracy: context window adds predictive power for count
- Root accuracy provides additional signal about learned structure

---

## Future Directions

### Potential Improvements

1. **Beam Search Decoding**
   - Current: Greedy autoregressive generation
   - Improvement: Beam search for better global sequences

2. **Constrained Decoding**
   - Enforce valid RootList properties (e.g., sorted order, no duplicates)
   - Use grammar-constrained generation

3. **Multi-Task Learning**
   - Joint training with root count classification
   - Shared backbone with task-specific heads

4. **Longer Context Windows**
   - Experiment with window sizes > 8
   - Use sparse attention for efficiency

5. **Alternative Architectures**
   - Mamba (state-space models) for efficient long sequences
   - Graph Transformers that directly incorporate edge structure

---

## References

### Foundational Papers

1. **Attention Is All You Need** (Vaswani et al., 2017)
   - Transformer architecture for sequence-to-sequence learning

2. **Semi-Supervised Classification with Graph Convolutional Networks** (Kipf & Welling, 2017)
   - GCN for learning node representations

3. **Graph Attention Networks** (Veličković et al., 2018)
   - Attention-based message passing for graphs

### Related Techniques

- **Teacher Forcing:** Williams & Zipser, 1989
- **Positional Encoding:** Vaswani et al., 2017
- **Autoregressive Generation:** Various language modeling works

---

*Document created: January 2026*
*Pipeline version: 1.0 (Initial Implementation)*
