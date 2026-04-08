# Abstract: Graph-Based Exploration of Polynomial Differences and Integer-Rational Bijections

## Overview

This project explores the intersection of three mathematical domains through computational graph generation and machine learning:

1. **Newton's Forward Differences** — Iterated difference operations on polynomials
2. **Set-Theoretic Bijections** — A novel approach to establishing ℤ ↔ ℚ mappings
3. **Graph Machine Learning** — Node classification and link prediction on the resulting structure

### Central Claim

**Well-bounded graph generation constraints produce a graph containing the complete set of determined polynomials — corresponding to a power set structure — along with undetermined polynomials.**

Specifically:
- **For every element in the power set** (every possible combination of integer roots within the range), **there exists a graph node** with corresponding root indexing (`muList`)
- Each determined polynomial's root pattern **encodes as a binary number**, hence maps to an integer
- The ML link prediction task can **discover and order the integers** represented in the graph by predicting `:NEXT_INTEGER` relationships between nodes whose binary encodings differ by 1

The undetermined polynomials represent the boundary cases — those whose roots lie outside the observable range, are irrational, complex, or result from positive/negative definite polynomials.

---

## Mathematical Foundation

### Newton's Difference Algorithm

For a polynomial P(x) of degree n, the **forward difference operator** Δ is defined as:

```
Δf(k) = f(k+1) - f(k)
```

Iterated application produces the **difference sequence**:

```
Ψ₀ = P(x)           degree n
Ψ₁ = ΔP(x)          degree n-1
Ψ₂ = Δ²P(x)         degree n-2
...
Ψₙ = ΔⁿP(x)         degree 0 (constant = n! × leading coefficient)
```

### TriagTriag Structure

The **Triangle of Triangles** (TriagTriag) is the tabular representation of this difference sequence, where:
- Each row represents a difference level (Ψᵢ)
- Each column represents an evaluation point (k)
- Diagonal patterns reveal algebraic relationships through **factorial scalars**

### Determined vs. Undetermined Polynomials

A polynomial at difference level i is **determined** if:

```
totalZero == dimension - wNum
```

Where:
- `totalZero` = count of integer zeros found within evaluation range
- `dimension` = original polynomial degree
- `wNum` = difference level (0 = original, 1 = first difference, etc.)

**Determined:** All possible integer roots are captured within the evaluation range.
**Undetermined:** The polynomial has fewer detected zeros than its degree allows.

### Why Polynomials Are Undetermined — Complete Analysis

A degree-d polynomial can have at most d roots (Fundamental Theorem of Algebra). When `totalZero < d`, the "missing" roots fall into distinct categories:

#### 1. Roots Outside the Observable Range

**Cause:** Integer roots exist but lie beyond `[-integerRange/2, +integerRange/2)`

**Example:** P(x) = (x - 200)(x - 300) with integerRange = 200
- True roots: {200, 300}
- Detectable: none (both outside [-100, +99])
- totalZero = 0, degree = 2 → **undetermined**

#### 2. Positive Definite / Negative Definite (No Real Roots)

**Cause:** Polynomial never crosses zero — all values have the same sign.

**Calculus criterion:** A polynomial P(x) is positive definite if:
- P(x) > 0 for all x ∈ ℝ
- Equivalently: all roots are complex (non-real)

**Examples:**
```
P(x) = x² + 1           Always > 0, roots at ±i
P(x) = x⁴ + x² + 1      Always > 0, no real roots
P(x) = (x² + 1)²        Always > 0, double roots at ±i
```

**Negative definite:** P(x) < 0 for all x ∈ ℝ (e.g., -x² - 1)

**Detection:** At every evaluation point k, P(k) ≠ 0
- totalZero = 0 for all difference levels affected
- **Undetermined** at every level

#### 3. Complex Conjugate Roots

**Cause:** Roots exist but are in ℂ \ ℝ (complex, non-real)

**From algebra:** Complex roots of real-coefficient polynomials come in conjugate pairs: a ± bi

**Example:** P(x) = x² - 2x + 5
- Discriminant: 4 - 20 = -16 < 0
- Roots: 1 ± 2i (complex)
- P(k) ≠ 0 for all integer k
- totalZero = 0, degree = 2 → **undetermined**

**Impact on difference table:**
- The polynomial evaluates to non-zero at all integer points
- No zeros propagate through the difference structure
- All levels with degree > 0 are potentially undetermined

#### 4. Irrational Roots

**Cause:** Real roots exist but are not integers

**Example:** P(x) = x² - 2
- Roots: ±√2 ≈ ±1.414...
- P(1) = -1 ≠ 0, P(2) = 2 ≠ 0
- No integer k gives P(k) = 0
- totalZero = 0, degree = 2 → **undetermined**

**Note:** Irrational roots are "invisible" to integer-only evaluation, just like complex roots.

#### 5. Roots at Non-Sampled Points

**Cause:** Root exists at integer within range, but evaluation misses it due to boundary effects

**Edge case:** Root at exactly `-integerRange/2` or `+integerRange/2 - 1` may have boundary handling issues in the code.

### Summary Table

| Category | Root Location | Example | Real? | Integer? | Detectable? |
|----------|---------------|---------|-------|----------|-------------|
| Outside range | x > range or x < -range | x = 500 | Yes | Yes | **No** |
| Positive definite | ∅ (no real roots) | x² + 1 | No | No | **No** |
| Complex | a ± bi | 1 ± 2i | No | No | **No** |
| Irrational | r ∈ ℝ \ ℤ | √2 | Yes | No | **No** |
| In range | -range/2 ≤ x < range/2, x ∈ ℤ | x = 5 | Yes | Yes | **Yes** |

### Implications for the Graph

**Distribution of determined vs undetermined:**

For random polynomial coefficients, most polynomials are **undetermined** because:
1. Complex roots are common (especially for higher degrees)
2. Irrational roots are measure-theoretically "almost all" real roots
3. Integer roots are relatively rare

**For structured exploration** (the Java code's approach):
- Coefficients are systematically varied via `setProductRange`
- This increases the likelihood of finding polynomials with integer roots
- The graph becomes enriched with determined polynomials

**The power set claim requires:** Generation parameters that ensure sufficient coverage of integer-root configurations.

### The Power Set Correspondence

For well-bounded generation parameters, the set of determined polynomials at each difference level corresponds to elements of a **power set**:

**Key Claim:** For every element in the power set, there exists a graph node with corresponding root indexing.

| Power Set Element | Graph Node | Root Indexing |
|-------------------|------------|---------------|
| ∅ (empty set) | Constant polynomial node | `muList = []`, `totalZero = 0` |
| {0} | Linear with root at 0 | `muList = [0]`, `totalZero = 1` |
| {0, 1} | Quadratic with roots at 0,1 | `muList = [0,1]`, `totalZero = 2` |
| {i₁, i₂, ..., iₖ} | Degree-k polynomial | `muList = [i₁, i₂, ..., iₖ]` |

**Binary Representation:**

Each determined polynomial's root pattern encodes as a **binary number**:

```
muList = [i₁, i₂, ..., iₖ]  →  Binary string via χᵢ  →  Integer N
```

The `nBinary` property on `:MuNumber` nodes stores this encoding.

**Example (dimension = 3, integerRange covers [-2, +2]):**

| muList (roots) | Binary Encoding | Integer |
|----------------|-----------------|---------|
| [] | 0 | 0 |
| [0] | 1 | 1 |
| [1] | 10 | 2 |
| [0, 1] | 11 | 3 |
| [2] | 100 | 4 |
| [0, 2] | 101 | 5 |
| ... | ... | ... |

**The ML Link Prediction Task:**

Given that each determined polynomial maps to an integer via binary encoding:

1. **Discover integer ordering** — Link prediction identifies `:NEXT_INTEGER` relationships
2. **Connect sequential integers** — Nodes representing N and N+1 should be linked
3. **Partition by rational** — Nodes with same μ(muList) value form equivalence classes

```
Graph contains:    Node(N=5) ──?──> Node(N=6) ──?──> Node(N=7)
ML task:           Predict these :NEXT_INTEGER edges
```

This transforms the polynomial difference graph into a structure where **integers can be sequentially ordered** through link prediction on the binary-encoded root patterns.

---

## The Bijection Theory

### Source

*"A Set-Theoretic Approach to Establishing Bijections between Integers and Rational Numbers"*
(`html-with-appendix-and-toc.html`)

### Key Definitions

**Set Union Ratio (Definition 2.1):**
```
μ(Å) = |∪_{A∈Å} A| / Σ_{A∈Å} |A|
```
Maps set collections to rationals in [0,1].

**Binary Encoding Rule (Definition 2.2):**
```
χᵢ(Å) = 1 if i = n + |Aₙ| - 1 for some Aₙ ∈ Å
χᵢ(Å) = 0 otherwise
```
Maps set collections to binary sequences (integers).

### The Open Question

> Does the polynomial difference graph lend itself to discovering or validating the integer-rational bijection?

The `muList` property (zero positions) may encode set cardinalities that map to rationals via μ, but this connection requires further investigation.

---

## Implementation

### Java Codebase: ZerosAndDifferences

**Core Algorithm:**

1. **Polynomial Evaluation** (`LoopList.loadPArray`)
   - Evaluates P(k) for k ∈ [-integerRange/2, +integerRange/2)
   - Records zero positions in `muList`
   - Counts zeros in `totalZero`

2. **Difference Computation** (`LoopList.subsequentWorkerListInit`)
   - Computes Δf(k) = f(k+1) - f(k)
   - Creates successive LoopLists for each difference level
   - Tracks zeros at each level

3. **Gaussian Elimination** (`GaussMain`)
   - Recovers polynomial coefficients from sampled values
   - Produces `vmResult` (Vandermonde matrix solution)

4. **Database Persistence** (`GaussTable1`)
   - Writes to Neo4j via JDBC
   - Creates `:Dnode` nodes with polynomial properties
   - Creates `:zMap` edges connecting difference levels

**Key Parameters:**
- `dimension` — polynomial degree (e.g., 5)
- `integerRange` — evaluation range (e.g., 200 → k ∈ [-100, +99])
- `setProductRange` — coefficient iteration range

### Graph Structure

**Nodes:**
- `:Dnode` — Polynomial at a difference level
  - `vmResult`: Vandermonde solution (polynomial identifier)
  - `n`, `d`: Numerator/denominator from muList
  - `totalZero`: Count of zeros
  - `muList`: Zero positions
  - `determined`: 1 if all roots found, 0 otherwise

**Relationships:**
- `:zMap` — Connects polynomial to its difference (structural, not predicted)
- `:CreatedBye` — Provenance tracking

---

## Machine Learning Tasks

### Task 1: Node Classification

**Objective:** Predict `determined` property (0 or 1)

**Why Non-Trivial:**
- The classification is deterministic given `totalZero` and `d`
- But can it be predicted from `muList` patterns alone?
- Can GraphSage embeddings capture the structural signal?

**Scientific Question:** Do patterns in zero positions predict whether all roots were found?

### Task 2: Link Prediction (Bijection Discovery)

**Objective:** If the bijection connection holds:
- Partition graph into rational equivalence classes
- Connect integers in sequential order within partitions

**Note:** `:zMap` edges are **structural** (polynomial differences) — not the prediction target.

---

## Generated Documentation

| File | Content |
|------|---------|
| `README.md` | Project overview, ML notebook documentation |
| `README_ZerosAndDifferences.md` | Java application documentation |
| `graph_structure.md` | Neo4j schema, node/relationship types, width analysis |
| `graph_theory.md` | Mathematical theory, bijection connection, ML tasks |
| `math_verification.md` | Worked examples, algorithm verification |
| `abstract.md` | This summary |

---

## Key Insights

### 1. The Determined/Undetermined Dichotomy

Creating a root at a specific position (e.g., Δ³P(0) = 0) guarantees **one root exists**, but does not guarantee **determinedness** unless the polynomial degree equals 1 at that level.

**Example:**
- P(x) = x⁴ - 6x³ creates a root at k=0 for Δ³P (linear)
- Since Δ³P has degree 1 and 1 root found → **determined**
- But Δ²P has degree 2 and may have only 1 root found → **undetermined**

### 2. The Factorial Scalar Relationship

To zero out Δⁿ at k=0, modify P by subtracting a degree-n term:

```
Δⁿ(xⁿ) = n! (constant)
Δⁿ(P - cx^n)(0) = Δⁿ(P)(0) - c × n!
```

Setting to zero: c = Δⁿ(P)(0) / n!

### 3. Graph as Mathematical Artifact

The graph is **generated by implementing Newton's differences**, not by implementing the bijection directly. The scientific question is whether the resulting structure **lends itself** to bijection-related ML tasks.

### 4. Power Set Structure

With proper bounds (`dimension`, `integerRange`, `setProductRange`):

**Completeness:** The graph contains a node for every element of the power set of possible root indices:
- Power set size = 2^(integerRange) possible root configurations
- Each configuration → a determined polynomial → a graph node
- `muList` property indexes the root positions

**Binary Encoding → Integer Mapping:**
```
muList = [i₁, i₂, ..., iₖ]  
    ↓ χᵢ encoding
Binary string B  
    ↓ positional value
Integer N
```

**Link Prediction Discovers Integer Order:**
- Nodes with binary encodings differing by 1 represent consecutive integers
- ML predicts `:NEXT_INTEGER` edges between these nodes
- Result: Sequential integer ordering emerges from the polynomial structure

**Undetermined Polynomials:** Represent the boundary — polynomials with partial root information (roots outside range, complex, irrational, or positive/negative definite)

---

## Conclusion

This project bridges:
- **Classical mathematics** (Newton's differences, Diophantine equations)
- **Set theory** (bijections, power sets, rational encodings)
- **Graph databases** (Neo4j, structural queries)
- **Machine learning** (GDS, GraphSage, node classification)

The generated graph artifact serves as both:
1. A **computational exploration** of polynomial difference structures
2. A **testbed** for ML algorithms on mathematically grounded data

The central hypothesis — that well-bounded generation produces a power set of determined polynomials — provides a foundation for understanding the graph's structure and the validity of ML predictions on it.

---

## References

- Newton's Forward Difference Formula
- *A Set-Theoretic Approach to Establishing Bijections between Integers and Rational Numbers* (companion PDF/HTML)
- Neo4j Graph Data Science Library
- Apache Zeppelin ML Notebooks

