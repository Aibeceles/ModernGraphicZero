# Machine Learning: Tasks and Applications

This document describes the machine learning tasks applicable to the polynomial difference graph, connecting graph algorithms to the bijection theory.

---

## Overview: Three ML Tasks

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MACHINE LEARNING TASKS                                │
│                                                                              │
│  1. NODE CLASSIFICATION                                                      │
│     Input: Polynomial node P                                                 │
│     Output: determined (1) or undetermined (0)                               │
│     Question: "Does this polynomial have all its roots within range?"        │
│                                                                              │
│  2. LINK PREDICTION — INTEGER ORDERING                                       │
│     Input: Determined nodes with binary-encoded roots                        │
│     Output: :NEXT_INTEGER edges between consecutive integers                 │
│     Question: "Which nodes represent sequential integers N, N+1?"            │
│                                                                              │
│  3. LINK PREDICTION — RATIONAL PARTITIONING                                  │
│     Input: Determined nodes with μ = n/d rational encoding                   │
│     Output: :SAME_RATIONAL edges within equivalence classes                  │
│     Question: "Which nodes share the same μ value?"                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Task 1: Node Classification

### Problem Statement

Given a polynomial node, predict whether it is **determined** (all expected integer roots found) or **undetermined** (missing roots).

See [04_classification.md](04_classification.md) for the mathematical definition.

### Why This Matters

- **Determined polynomials** correspond to power set elements
- **Undetermined polynomials** represent boundary cases
- Prediction accuracy indicates whether structural patterns reveal root completeness

### Feature Space

| Feature | Source | Description | Importance |
|---------|--------|-------------|------------|
| `vmResult` coefficients | Node property | Polynomial coefficients [a₀, a₁, ...] | High |
| `totalZero` | Node property | Count of detected integer roots | High |
| **Degree** | Derived | len(vmResult) - 1 | **Critical** |
| **wNum** | CreatedBy | Difference level | **Critical** |
| Coefficient ratios | Derived | Relationships between coefficients | Medium |
| GraphSage embedding | Graph structure | Neighborhood-based representation | Medium |

**Key insight:** Degree is the strongest predictor — linear polynomials are almost always determined; higher degrees have decreasing probability.

### Training Approach

```python
# Feature extraction
features = []
for node in graph.nodes:
    coeffs = parse_vmResult(node.vmResult)
    embedding = graphsage.embed(node)
    features.append([
        coeffs,
        node.totalZero,
        len(coeffs) - 1,  # degree
        embedding
    ])

# Labels
labels = [node.determined for node in graph.nodes]

# Train classifier
model = LogisticRegression()
model.fit(features, labels)

# Predict
new_polynomial = parse_coefficients(input_p)
prediction = model.predict(new_polynomial)  # 0 or 1
```

### Use Case

```
INPUT:  P(x) = x⁴ - 6x³ + 11x² - 6x
        Coefficients: [0, -6, 11, -6, 1]

PROCESS:
  1. Compute GraphSage embedding
  2. Feed to trained classifier
  3. Output probability

OUTPUT: determined = 1 (probability 0.89)
        Interpretation: Likely has all 4 integer roots within range
        Actual roots: {0, 1, 2, 3} ✓
```

### Results from Notebooks

From `GraphicLablePrediction.json`:
- **Training F1:** ~82.9%
- **Test F1:** ~79.2%

The non-trivial accuracy suggests coefficient patterns **do encode** root information.

### Why ML Over Direct Computation?

- Direct root-finding is expensive for high-degree polynomials
- ML can **generalize patterns** from training data
- Enables **real-time classification** without symbolic computation
- Captures **structural relationships** in the difference graph

---

## Task 2: Link Prediction — Integer Ordering

### Problem Statement

Determined polynomials map to integers via binary encoding χ. Given the graph, predict `:NEXT_INTEGER` edges connecting nodes representing consecutive integers.

### The Mathematical Basis

From Definition 2.2 (Binary Encoding):

```
muList = [i₀, i₁, ...]  →  Binary string  →  Integer N

Example:
  muList = []      → binary "0"     → N = 0
  muList = [0]     → binary "1"     → N = 1  
  muList = [1]     → binary "10"    → N = 2
  muList = [0,1]   → binary "11"    → N = 3
  muList = [2]     → binary "100"   → N = 4
```

### Link Prediction Target

```
Node(N=5) ──:NEXT_INTEGER──► Node(N=6) ──:NEXT_INTEGER──► Node(N=7)
```

### Why Link Prediction?

The `:NEXT_INTEGER` relationships **don't exist** in the original graph — they must be **discovered** through ML:

1. The graph only has `:zMap` (difference relationships)
2. Integer ordering is **implicit** in the muList encoding
3. Link prediction **extracts** this latent structure

### Training Approach

```python
# Compute integer value for each determined node
def mulist_to_integer(mulist):
    """Convert root positions to binary-encoded integer"""
    binary = ['0'] * (max(mulist) + 1) if mulist else ['0']
    for pos in mulist:
        binary[pos] = '1'
    return int(''.join(reversed(binary)), 2)

# Create ground truth edges
edges_true = []
nodes_by_int = {mulist_to_integer(n.muList): n for n in determined_nodes}
for N in sorted(nodes_by_int.keys()):
    if N + 1 in nodes_by_int:
        edges_true.append((nodes_by_int[N], nodes_by_int[N+1]))

# Train link predictor
embeddings = graphsage.embed_all(determined_nodes)
link_model = LinkPredictor(feature_combiner='COSINE')
link_model.fit(embeddings, edges_true)

# Predict missing consecutive pairs
predictions = link_model.predict_top_k(k=100)
```

### Use Case

```
INPUT:  Determined nodes with various muList values
        Node A: muList = "[2]"   → N = 4
        Node B: muList = "[0,2]" → N = 5  
        Node C: muList = "[1,2]" → N = 6
        Node D: muList = "[0,1,2]" → N = 7

OUTPUT:
        A ──:NEXT_INTEGER──► B  (4 → 5) ✓
        B ──:NEXT_INTEGER──► C  (5 → 6) ✓
        C ──:NEXT_INTEGER──► D  (6 → 7) ✓

RESULT: Integer sequence discovered from graph structure
```

### Significance

If link prediction successfully orders integers:
- The graph **encodes** integer structure implicitly
- ML **recovers** the ℤ ordering from polynomial properties
- This validates **χ encoding** as a meaningful mapping

---

## Task 3: Link Prediction — Rational Partitioning

### Problem Statement

Determined polynomials map to rationals via μ = n/d. Nodes with the **same μ value** form equivalence classes. Predict `:SAME_RATIONAL` edges to partition nodes.

### The Mathematical Basis

From Definition 2.1 (Set Union Ratio):

```
μ(Å) = |∪_{A∈Å} A| / Σ_{A∈Å} |A| = n/d

Example set collections with μ = 1/2:
  Å₁ = {{1}, {1,2}, {1,2,3}}     → μ = 3/6 = 1/2
  Å₂ = {{1}, {1}}               → μ = 1/2 = 1/2
```

### Partitioning Structure

```
┌─────────────────────────────────────────────────────────────────┐
│  μ = 1                      μ = 1/2                   μ = 1/3   │
│  ┌─────────┐               ┌─────────┐              ┌─────────┐ │
│  │ Node A  │               │ Node D  │              │ Node G  │ │
│  │ Node B  │◄─:SAME_RAT─►  │ Node E  │◄─:SAME_RAT─► │ Node H  │ │
│  │ Node C  │               │ Node F  │              │         │ │
│  └─────────┘               └─────────┘              └─────────┘ │
│                                                                  │
│  Each partition = equivalence class under μ                      │
└─────────────────────────────────────────────────────────────────┘
```

### Training Approach

```python
# Compute μ for each determined node
def compute_mu(node):
    """Compute Set Union Ratio from muList"""
    positions = parse_mulist(node.muList)
    set_collection = binary_decode(positions)
    union_size = len(set.union(*set_collection))
    sum_sizes = sum(len(s) for s in set_collection)
    return Fraction(union_size, sum_sizes)

# Create ground truth edges (same μ)
edges_same_mu = []
mu_groups = defaultdict(list)
for node in determined_nodes:
    mu_groups[compute_mu(node)].append(node)

for mu_value, nodes in mu_groups.items():
    for i, n1 in enumerate(nodes):
        for n2 in nodes[i+1:]:
            edges_same_mu.append((n1, n2))

# Train link predictor
embeddings = graphsage.embed_all(determined_nodes)
partition_model = LinkPredictor(feature_combiner='HADAMARD')
partition_model.fit(embeddings, edges_same_mu)
```

### Significance

- **μ maps to ℚ** — partitioning groups nodes by rational image
- Combined with integer ordering (χ), we have **both bijection mappings**
- Validates the **non-injective but structured** nature of μ

---

## Combined Pipeline

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           FULL ML PIPELINE                                │
│                                                                           │
│  INPUT: New polynomial P(x) = x³ - 3x² + 2x                               │
│         Coefficients: [0, 2, -3, 1]                                       │
│                                                                           │
│  STEP 1: NODE CLASSIFICATION                                              │
│          Model predicts: determined = 1 (confidence 0.91)                 │
│                                                                           │
│  STEP 2: COMPUTE REPRESENTATIONS                                          │
│          Actual roots: {0, 1, 2}                                          │
│          muList: [0, 1, 2]                                                │
│          Binary: "111" → Integer N = 7                                    │
│          μ = 3/6 = 1/2                                                    │
│                                                                           │
│  STEP 3: LINK PREDICTION — INTEGER POSITION                               │
│          Predicted neighbors: N=6 (before), N=8 (after)                   │
│                                                                           │
│  STEP 4: LINK PREDICTION — RATIONAL PARTITION                             │
│          Predicted cluster: μ = 1/2 equivalence class                     │
│                                                                           │
│  OUTPUT: P is determined, maps to integer 7, rational 1/2                 │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Model Configurations

### Node Classification (from notebooks)

**GraphSage Training:**
- Aggregator: Pool
- Activation: ReLU
- Epochs: 75
- Features: `zero`, `one`, `two`, `three`, `wNum`, `totalZero`

**Classification:**
- Model: Logistic Regression
- Validation: 185-fold cross-validation
- Metric: F1 Weighted Score

### Link Prediction (from notebooks)

**GraphSage Embeddings:**
- Dimension: 64
- Sample sizes: [25, 10, 10, 10]
- Search depth: 6

**Best Configuration:**
- Feature combiner: COSINE
- Penalty: 10000.0
- Test AUCPR: ~55.5%

---

## Evaluation Metrics

### Node Classification

| Metric | Description |
|--------|-------------|
| F1 Weighted | Balance of precision and recall |
| Accuracy | Overall correct predictions |
| AUC-ROC | Discriminative ability |

### Link Prediction

| Metric | Description |
|--------|-------------|
| AUCPR | Area under precision-recall curve |
| Link Precision | Correct edges / Predicted edges |
| Link Recall | Correct edges / True edges |
| Kendall's Tau | Rank correlation (for sequencing) |

---

## Speculative: Arithmetic on Embeddings

### The Vision

If the graph encodes ℤ ↔ ℚ, can we perform **arithmetic** through trained embeddings?

### Potential Approaches

#### Addition via Graph Navigation

```
Given: Node(N=3) and Node(N=5)
Want:  Node(N=8) = 3 + 5

Approach:
  1. From Node(N=3), traverse :NEXT_INTEGER 5 times
  2. Arrive at Node(N=8)
```

#### Multiplication via Embedding Arithmetic

```
Given: Embedding(N=3) and Embedding(N=2)
Want:  Embedding(N=6) = 3 × 2

Approach:
  1. Learn transformation T: T(embed(a), embed(b)) ≈ embed(a × b)
  2. Find nearest node to result
```

### Research Questions

1. Can GNN embeddings preserve arithmetic structure?
2. Can link prediction discover arithmetic relationships?
3. Can rational arithmetic emerge from μ partitions?

---

## Graph Relationship Summary

| Relationship | Meaning | Status |
|--------------|---------|--------|
| `:zMap` | Polynomial difference (structural) | **Given** — do not predict |
| `:SAME_RATIONAL` | Same μ value | **Predicted** via clustering |
| `:NEXT_INTEGER` | Sequential integers | **Predicted** via link prediction |

---

## Required Software

- **Neo4j** 4.x+ with Graph Data Science (GDS) Library 1.6+
- **Apache Zeppelin** with Neo4j interpreter
- **Key GDS Procedures:**
  - `gds.beta.graphSage.train()` / `.mutate()` / `.stream()`
  - `gds.alpha.ml.nodeClassification.train()` / `.predict.write()`
  - `gds.alpha.ml.linkPrediction.train()` / `.predict.stream()`

---

## Summary

| Task | Purpose | Validates |
|------|---------|-----------|
| **Node Classification** | Predict determined/undetermined | Coefficient patterns encode root info |
| **Link Prediction (Integer)** | Order by χ encoding | Graph contains implicit ℤ structure |
| **Link Prediction (Rational)** | Partition by μ value | Graph contains implicit ℚ structure |

The polynomial difference graph is a **mathematical artifact** encoding the ℤ ↔ ℚ relationship. Machine learning extracts implicit integer ordering, discovers rational equivalence classes, and predicts polynomial properties from structure.

---

*This document extracts ML content from `why_ml.md` and `README.md`.*

