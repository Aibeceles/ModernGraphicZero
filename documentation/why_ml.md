# Why Machine Learning? Applications for the Polynomial Difference Graph

This document explains the machine learning tasks applicable to the polynomial difference graph, connecting graph algorithms to the mathematical bijection theory.

---

## Overview: Three ML Tasks

The graph structure enables three complementary machine learning tasks:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MACHINE LEARNING TASKS                                │
│                                                                              │
│  1. NODE CLASSIFICATION                                                      │
│     Input: Polynomial node P                                                 │
│     Output: determined (1) or underdetermined (0)                            │
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

## Task 1: Node Classification — Determined vs Underdetermined

### The Problem

Given a polynomial node, predict whether it is **determined** (all expected integer roots found) or **underdetermined** (missing roots due to complex, irrational, or out-of-range values).

### Why This Matters

- **Determined polynomials** correspond to elements of the power set of possible root configurations
- **Underdetermined polynomials** represent boundary cases — the "edge" of the mathematical structure
- Prediction accuracy indicates whether **structural patterns** in polynomial coefficients reveal root completeness

### Feature Space

| Feature | Source | Description | Importance |
|---------|--------|-------------|------------|
| `vmResult` coefficients | Node property | Polynomial coefficients [a₀, a₁, ...] | High |
| `totalZero` | Node property | Count of detected integer roots | High |
| **Degree** | Derived (len(vmResult)-1) | Polynomial degree n | **Critical** |
| **wNum** | CreatedBy relationship | Difference level (0 = original, n = nth diff) | **Critical** |
| Coefficient ratios | Derived | Relationships between coefficients | Medium |
| GraphSage embedding | Graph structure | Neighborhood-based representation | Medium |

> **Key insight:** Degree (or equivalently, dimension - wNum) is the strongest predictor of determinedness. Linear polynomials (degree 1) are almost always determined; higher degrees have decreasing probability. See "Determinedness by Polynomial Degree" section below.

### Training Approach

```python
# Pseudocode for node classification pipeline

# 1. Feature extraction
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

# 2. Labels
labels = [node.determined for node in graph.nodes]

# 3. Train classifier
model = LogisticRegression()  # or GNN
model.fit(features, labels)

# 4. Predict
new_polynomial = parse_coefficients(input_p)
prediction = model.predict(new_polynomial)  # 0 or 1
```

### Use Case: Input P → Output Determined/Underdetermined

**Scenario:** User provides polynomial coefficients, system predicts classification.

```
INPUT:  P(x) = x⁴ - 6x³ + 11x² - 6x
        Coefficients: [0, -6, 11, -6, 1]

PROCESS:
  1. Compute GraphSage embedding (or use coefficient features only)
  2. Feed to trained classifier
  3. Output probability

OUTPUT: determined = 1 (probability 0.89)
        Interpretation: Likely has all 4 integer roots within range
        Actual roots: {0, 1, 2, 3} ✓
```

**Why ML over direct computation?**
- Direct root-finding is expensive for high-degree polynomials
- ML can **generalize patterns** from training data
- Enables **real-time classification** without symbolic computation
- Captures **structural relationships** in the difference graph

### Expected Results

From existing notebooks (`GraphicLablePrediction.json`):
- Training F1: ~82.9%
- Test F1: ~79.2%

The non-trivial accuracy suggests coefficient patterns **do encode** root information, but the relationship is not perfectly learnable — matching the mathematical expectation that undetermined cases have diverse causes (complex roots, irrationals, out-of-range).

---

## Determinedness by Polynomial Degree

A critical insight from algorithm analysis: **determinedness is probabilistic and degree-dependent**.

### Empirical Data from Graph Queries

Comparing two pArrays (`[7,15,17,10,0]` and `[7,16,17,10,0]`) reveals a consistent pattern:

| wNum | Degree | determined | totalZero | Observation |
|------|--------|------------|-----------|-------------|
| 0 | 4 | 0 | 1 | Quartic with 1 root — **undetermined** |
| 1 | 3 | 0 | 1 | Cubic with 1 root — **undetermined** |
| 2 | 2 | 1 | 2 | Quadratic with 2 roots — **determined** |
| 3 | 1 | 1 | 1 | Linear with 1 root — **determined** |
| 4 | 0 | 1 | 0 | Constant — **determined** (trivially) |

### The Degree-Determinedness Relationship

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    DETERMINEDNESS PROBABILITY BY DEGREE                      │
│                                                                             │
│  Degree 0 (constant):  Always determined (no roots needed)                 │
│  Degree 1 (linear):    Almost always determined (1 root easy to find)      │
│  Degree 2 (quadratic): Often determined (2 roots, but may be complex)      │
│  Degree 3 (cubic):     Sometimes determined (complex roots more likely)    │
│  Degree 4+ (higher):   Rarely determined (many roots needed)               │
│                                                                             │
│  P(determined) decreases as degree increases                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Only Linear Is "Certainly Determined"

For a degree-1 polynomial P(x) = ax + b:
- Has exactly 1 root at x = -b/a
- If coefficients are integers and -b/a is an integer within range → determined
- The single-root requirement is easily satisfied

For higher degrees:
- Need ALL k roots (for degree k) to be integers within range
- Each additional root multiplies the probability of failure
- Complex roots, irrationals, or out-of-range values cause underdetermination

### Connection to Latent Variables

From [graph_comprehension.md](../documentation/graph_comprehension.md#latent-variables-and-the-parray-signature):

- The pArray specifies ONE root per level (path signature)
- But muList may contain MORE roots (latent variables)
- For the quadratic at wNum=2: pArray specifies 17, but muList = [4, 16] (two roots discovered)
- The algorithm **discovers** additional roots beyond the pArray specification

### Why pArray Cannot Predict Determinedness

This is the core challenge for ML classification — and the source of its non-triviality.

**The Adjustment Mechanism** (from [algorithm_construction.md Part 7](../documentation/algorithm_construction.md)):

When `computeIndexZero` forces a root at position x (specified by pArray), it adjusts the constant coefficient:

```
Q(x) = ax² + bx + c       →  Q(17) = some_value ≠ 0
adjustment = some_value / 2!
c_new = c - adjustment
Q'(x) = ax² + bx + c_new  →  Q'(17) = 0  ✓
```

**The Side Effect:** Changing `c` to force `Q'(17) = 0` changes `Q'(x)` for ALL x. This may **accidentally** create additional integer roots (latent roots) that weren't specified.

**Why ML Can't Simply Use pArray:**

| What pArray Tells Us | What pArray Doesn't Tell Us |
|---------------------|---------------------------|
| ONE specified root per level | Whether latent roots exist |
| The path through the graph | Whether totalZero will equal degree |
| The difference level (wNum) | The final polynomial coefficients |

**The Classification Challenge:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  INPUT (from pArray): "Force root at x=17"                                  │
│                                                                             │
│  POSSIBLE OUTCOMES:                                                         │
│    (A) Q'(x) has roots at x=4, x=17  → totalZero=2, degree=2 → DETERMINED   │
│    (B) Q'(x) has roots at x=17, x=5.3 → totalZero=1, degree=2 → UNDETERMINED│
│    (C) Q'(x) has roots at x=17, x=150 → totalZero=1 (out of range) → UNDET. │
│                                                                             │
│  ML must PREDICT which outcome, based on inherited coefficients (a, b).     │
└─────────────────────────────────────────────────────────────────────────────┘
```

**The ML Task:**

ML models must learn patterns in the inherited coefficients (from higher difference levels) that predict whether the adjustment will accidentally create enough latent roots to achieve determinedness. This is what makes the classification problem genuinely challenging — it's not solvable by simple rule application.

### ML Implication: Degree as Key Feature

**Insight:** Polynomial degree (= dimension - wNum) is a strong predictor of determinedness.

```python
# Enhanced feature extraction
features.append([
    coeffs,
    node.totalZero,
    len(coeffs) - 1,      # degree
    dimension - wNum,      # equivalent: difference level
    embedding
])

# Degree alone predicts:
#   degree <= 1 → likely determined
#   degree >= 3 → likely undetermined
```

---

## Graph Convergence and ML Implications

Different polynomial paths (pArrays) can **converge** to the same Dnode at lower difference levels.

### Observed Convergence Pattern

From the UNION query comparing two pArrays:

```
pArray [7,15,17,10,0]              pArray [7,16,17,10,0]
         │                                  │
    Dnode 1131211                      Dnode 1131342     ← DIFFERENT (wNum=0)
         │                                  │
    Dnode 1131165                      Dnode 1131296     ← DIFFERENT (wNum=1)
         │                                  │
         └──────────────┬───────────────────┘
                        │
                   Dnode 1129239                         ← SAME (wNum=2)
                        │
                   Dnode 1118395                         ← SAME (wNum=3)
                        │
                   Dnode 9                               ← SAME (wNum=4)
```

### Why Convergence Occurs

At wNum=2, both polynomials have the **same second difference** — the quadratic with roots at x=4 and x=17. The difference in first-difference roots (15 vs 16) "cancels out" when taking another difference.

This is explained by the **top-down constraint mechanism** in [algorithm_construction.md](../documentation/algorithm_construction.md):
- Higher-level adjustments affect lower levels
- But lower-level polynomials can be identical despite different higher-level paths

### ML Training Data Implications

**Challenge:** The same Dnode appears with multiple CreatedBy nodes (different pArray signatures).

| Dnode ID | CreatedBy pArrays | wNum |
|----------|-------------------|------|
| 1129239 | `[7,15,17,10,0]` AND `[7,16,17,10,0]` | 2 |
| 1118395 | `[7,15,17,10,0]` AND `[7,16,17,10,0]` | 3 |
| 9 | (many pArrays) | 4 |

**Implication for ML:**
1. **Node features are consistent** — same Dnode has same vmResult, muList, determined
2. **Graph structure varies** — different incoming paths via CreatedBy
3. **Training should use Dnode properties**, not CreatedBy path features
4. **Link prediction must handle multi-path convergence**

### Why This Matters for Bijection

The convergence structure means:
- Multiple polynomial "paths" map to the same lower-level representation
- The graph is a **DAG** (directed acyclic graph), not a tree
- The μ and χ encodings on shared Dnodes represent **equivalence classes**

---

## Task 2: Link Prediction — Integer Ordering

### The Problem

Determined polynomials map to integers via the binary encoding χ. Given the graph of determined nodes, predict `:NEXT_INTEGER` edges that connect nodes representing consecutive integers.

### The Mathematical Basis

From `html-with-appendix-and-toc.html`, Definition 2.2:

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

**Goal:** Given node embeddings, predict which pairs should be connected as consecutive integers.

### Why Link Prediction?

The `:NEXT_INTEGER` relationships **don't exist** in the original graph — they must be **discovered** through ML:

1. The graph only has `:zMap` (difference relationships)
2. Integer ordering is **implicit** in the muList encoding
3. Link prediction **extracts** this latent structure

### Training Approach

```python
# Pseudocode for integer ordering link prediction

# 1. Compute integer value for each determined node
def mulist_to_integer(mulist):
    """Convert root positions to binary-encoded integer"""
    binary = ['0'] * (max(mulist) + 1) if mulist else ['0']
    for pos in mulist:
        binary[pos] = '1'
    return int(''.join(reversed(binary)), 2)

# 2. Create ground truth edges
edges_true = []
nodes_by_int = {mulist_to_integer(n.muList): n for n in determined_nodes}
for N in sorted(nodes_by_int.keys()):
    if N + 1 in nodes_by_int:
        edges_true.append((nodes_by_int[N], nodes_by_int[N+1]))

# 3. Train link predictor
embeddings = graphsage.embed_all(determined_nodes)
link_model = LinkPredictor(feature_combiner='COSINE')
link_model.fit(embeddings, edges_true)

# 4. Predict missing consecutive pairs
predictions = link_model.predict_top_k(k=100)
```

### Use Case: Discover Integer Sequence

**Scenario:** Given a set of determined polynomial nodes, reconstruct the integer ordering.

```
INPUT:  Determined nodes with various muList values
        Node A: muList = "[2]"   → N = 4
        Node B: muList = "[0,2]" → N = 5  
        Node C: muList = "[1,2]" → N = 6
        Node D: muList = "[0,1,2]" → N = 7

LINK PREDICTION OUTPUT:
        A ──:NEXT_INTEGER──► B  (4 → 5) ✓
        B ──:NEXT_INTEGER──► C  (5 → 6) ✓
        C ──:NEXT_INTEGER──► D  (6 → 7) ✓

RESULT: Integer sequence discovered from graph structure
```

### Why This Demonstrates the Bijection

If link prediction successfully orders integers:
- The graph **encodes** the integer structure implicitly
- ML **recovers** the ℤ ordering from polynomial properties
- This validates the **χ encoding** as a meaningful mapping

---

## Task 3: Link Prediction — Rational Partitioning

### The Problem

Determined polynomials also map to rationals via μ = n/d. Nodes with the **same μ value** form equivalence classes. Predict `:SAME_RATIONAL` edges to partition nodes by their rational encoding.

### The Mathematical Basis

From `html-with-appendix-and-toc.html`, Definition 2.1:

```
μ(Å) = |∪_{A∈Å} A| / Σ_{A∈Å} |A| = n/d

Example set collections with μ = 1/2:
  Å₁ = {{1}, {1,2}, {1,2,3}}     → μ = 3/6 = 1/2
  Å₂ = {{1}, {1}}               → μ = 1/2 = 1/2
  Å₃ = {{1,2}, {1,2,3,4}}       → μ = 4/8 = 1/2
```

### Partitioning Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                  RATIONAL PARTITIONING                           │
│                                                                  │
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

### Link Prediction Target

```
Node(μ=1/2) ──:SAME_RATIONAL──► Node(μ=1/2)
```

**Goal:** Cluster nodes by their μ value through link prediction.

### Training Approach

```python
# Pseudocode for rational partitioning

# 1. Compute μ for each determined node
def compute_mu(node):
    """Compute Set Union Ratio from muList"""
    positions = parse_mulist(node.muList)
    # Decode to set collection, compute |union| / Σ|sizes|
    set_collection = binary_decode(positions)
    union_size = len(set.union(*set_collection))
    sum_sizes = sum(len(s) for s in set_collection)
    return Fraction(union_size, sum_sizes)  # returns n/d

# 2. Create ground truth edges (same μ)
edges_same_mu = []
mu_groups = defaultdict(list)
for node in determined_nodes:
    mu_groups[compute_mu(node)].append(node)

for mu_value, nodes in mu_groups.items():
    for i, n1 in enumerate(nodes):
        for n2 in nodes[i+1:]:
            edges_same_mu.append((n1, n2))

# 3. Train link predictor
embeddings = graphsage.embed_all(determined_nodes)
partition_model = LinkPredictor(feature_combiner='HADAMARD')
partition_model.fit(embeddings, edges_same_mu)

# 4. Predict partitions
clusters = partition_model.cluster(determined_nodes)
```

### Use Case: Partition by Rational

**Scenario:** Given determined nodes, group them by their μ rational value.

```
INPUT:  Mixed determined nodes

LINK PREDICTION CLUSTERS:
        Cluster 1 (μ = 1):   [Node₁, Node₂, Node₃]
        Cluster 2 (μ = 1/2): [Node₄, Node₅]
        Cluster 3 (μ = 2/3): [Node₆, Node₇, Node₈]

VERIFICATION:
        All nodes in Cluster 1 have n/d where n = d
        All nodes in Cluster 2 have n/d = 1/2 (or equivalent)
```

### Why This Matters for Bijection

- **μ maps to ℚ** — partitioning by μ groups nodes by their rational image
- Combined with integer ordering (χ), we have **both mappings**
- Successful partitioning validates the **non-injective but structured** nature of μ

---

## Combined Use Case: Full Pipeline

### Input → Classification → Ordering → Partitioning

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           FULL ML PIPELINE                                │
│                                                                           │
│  INPUT: New polynomial P(x) = x³ - 3x² + 2x                               │
│         Coefficients: [0, 2, -3, 1]                                       │
│                                                                           │
│  STEP 1: NODE CLASSIFICATION                                              │
│          Model predicts: determined = 1 (confidence 0.91)                 │
│          Interpretation: All roots likely within range                    │
│                                                                           │
│  STEP 2: COMPUTE REPRESENTATIONS                                          │
│          Actual roots: {0, 1, 2}                                          │
│          muList: [0, 1, 2]                                                │
│          Binary: "111" → Integer N = 7                                    │
│          Set collection: {{1}, {1,2}, {1,2,3}}                            │
│          μ = 3/6 = 1/2                                                    │
│                                                                           │
│  STEP 3: LINK PREDICTION — INTEGER POSITION                               │
│          Predicted neighbors: N=6 (before), N=8 (after)                   │
│          Position in ℤ ordering established                               │
│                                                                           │
│  STEP 4: LINK PREDICTION — RATIONAL PARTITION                             │
│          Predicted cluster: μ = 1/2 equivalence class                     │
│          Connected to other nodes with same rational                      │
│                                                                           │
│  OUTPUT: P is determined, maps to integer 7, rational 1/2                 │
│          Neighbors in integer sequence: nodes for N=6 and N=8             │
│          Rational equivalents: other polynomials with μ = 1/2             │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Speculative: Arithmetic on Trained Models

### The Vision

If the graph encodes the ℤ ↔ ℚ bijection, can we perform **arithmetic operations** on polynomial representations through the trained model?

### Potential Approaches

#### 1. Addition via Graph Navigation

```
Given: Node(N=3) and Node(N=5)
Want:  Node(N=8) = 3 + 5

Approach:
  1. From Node(N=3), traverse :NEXT_INTEGER 5 times
  2. Arrive at Node(N=8)
  3. ML predicts traversal path
```

#### 2. Multiplication via Embedding Arithmetic

```
Given: Embedding(N=3) and Embedding(N=2)
Want:  Embedding(N=6) = 3 × 2

Approach:
  1. Learn embedding transformation T such that:
     T(embed(a), embed(b)) ≈ embed(a × b)
  2. Apply T to get target embedding
  3. Find nearest node to result
```

#### 3. Rational Arithmetic via μ Operations

```
Given: Node(μ=1/2) and Node(μ=1/3)
Want:  Node(μ=5/6) = 1/2 + 1/3

Approach:
  1. Predict :SAME_RATIONAL edges for target μ = 5/6
  2. Navigate to partition containing result
  3. Select representative node
```

### Challenges

| Challenge | Description |
|-----------|-------------|
| **Closure** | Addition/multiplication may produce integers outside graph range |
| **Uniqueness** | Multiple nodes may map to same integer (need canonical representative) |
| **Precision** | Embedding arithmetic introduces approximation errors |
| **Training Data** | Need explicit arithmetic examples to learn transformations |

### Research Questions

1. **Can GNN embeddings preserve arithmetic structure?**
   - Do embed(a) + embed(b) ≈ embed(a+b) after training?

2. **Can link prediction discover arithmetic relationships?**
   - Predict :SUM edges where Node(a) + Node(b) → Node(a+b)

3. **Can rational arithmetic emerge from μ partitions?**
   - Navigate between partitions to perform ℚ operations

### Speculative Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ARITHMETIC-AWARE GNN                                  │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Input Layer                                                      │   │
│  │  • Polynomial coefficients (vmResult)                             │   │
│  │  • Root positions (muList)                                        │   │
│  │  • Graph structure (adjacency)                                    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  GraphSage Encoder                                                │   │
│  │  • Neighborhood aggregation                                       │   │
│  │  • Produces node embeddings                                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Arithmetic Head (speculative)                                    │   │
│  │  • Addition: T_add(e₁, e₂) → e_sum                                │   │
│  │  • Multiplication: T_mul(e₁, e₂) → e_product                      │   │
│  │  • Trained on known arithmetic pairs                              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Output Decoder                                                   │   │
│  │  • Nearest neighbor search in embedding space                     │   │
│  │  • Returns polynomial node for arithmetic result                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Summary: Why ML for This Graph?

| Task | Purpose | Validates |
|------|---------|-----------|
| **Node Classification** | Predict determined/undetermined | Coefficient patterns encode root information |
| **Link Prediction (Integer)** | Order by χ encoding | Graph contains implicit ℤ structure |
| **Link Prediction (Rational)** | Partition by μ value | Graph contains implicit ℚ structure |
| **Arithmetic (Speculative)** | Operate on representations | Bijection supports computation |

### The Core Insight

The polynomial difference graph is not just a data structure — it's a **mathematical artifact** encoding the ℤ ↔ ℚ relationship. Machine learning:

1. **Extracts** the implicit integer ordering (χ)
2. **Discovers** the rational equivalence classes (μ)
3. **Predicts** polynomial properties from structure
4. **May enable** arithmetic on the encoded representations

This transforms the bijection from a theoretical construct into a **computable, learnable system**.

---

## Related Documentation

### Algorithm Comprehension (NEW)
- [algorithm_construction.md](../documentation/algorithm_construction.md) — How polynomials are constructed with root constraints (explains determinedness patterns)
- [graph_comprehension.md](../documentation/graph_comprehension.md) — Understanding pArray, muList, latent variables, and graph convergence

### Graph Structure
- [the_graph.md](the_graph.md) — Graph structure explanation
- [abstract.md](abstract.md) — Mathematical foundations

### ML Notebooks
- [GraphicLablePrediction.json](GraphicLablePrediction.json) — Node classification notebook
- [GraphicLinkPrediction.json](GraphicLinkPrediction.json) — Link prediction notebook

### Theory
- [html-with-appendix-and-toc.html](../html-with-appendix-and-toc.html) — Bijection theory paper


