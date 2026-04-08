# The Determined/Undetermined Dichotomy

## Overview

The **determined/undetermined dichotomy** is a fundamental classification of polynomial nodes in the difference graph. It distinguishes between polynomials whose integer root structure is fully captured versus those with "hidden" roots outside the observable domain.

This document develops the dichotomy concept, exploring:
1. Formal definitions and classification criteria
2. Propagation between consecutive difference levels
3. Horizontal structure within the same level
4. The combinatorial completeness hypothesis
5. Connections to Diophantine equations

---

## Part I: The Dichotomy Defined

### 1.1 Classification Criterion

**Definition.** A polynomial P at difference level `wNum` (with degree d = dimension - wNum) is:

```
DETERMINED      if  totalZero == d
UNDETERMINED    if  totalZero < d
```

Where:
- `totalZero` = count of integer zeros found within evaluation range [-R/2, +R/2)
- `d` = polynomial degree at this difference level
- `dimension` = degree of the original (source) polynomial
- `wNum` = difference level (0 = original, 1 = first difference, etc.)

### 1.2 Mathematical Foundation

**Fundamental Theorem of Algebra:** A polynomial of degree d has exactly d roots in ℂ (counting multiplicity).

**Integer restriction:** When we restrict to integer roots within a finite range, we can find **at most** d roots. The dichotomy arises from whether we find exactly d or fewer than d.

### 1.3 The Four Categories of Missing Roots

When `totalZero < d`, the "missing" roots belong to one or more categories:

| Category | Description | Detectable? |
|----------|-------------|-------------|
| **Out of Range** | Integer roots at |x| > R/2 | No |
| **Irrational** | Real roots like √2, π | No |
| **Complex** | Roots of form a ± bi, b ≠ 0 | No |
| **Definite** | Positive/negative definite (no real roots) | No |

**Key insight:** Determined polynomials have **all roots visible** as integers within range. Undetermined polynomials have **hidden structure** — information about root locations that cannot be recovered from integer evaluation alone.

> **Connection to Latent Roots:**
> 
> The algorithm's coefficient adjustment mechanism (see [algorithm_construction.md Part 7](algorithm_construction.md)) explains why underdetermination occurs:
> 
> - When `computeIndexZero` adjusts a coefficient to force a root at position x, it changes the entire polynomial
> - This adjustment may **accidentally** create additional integer roots (latent roots) — or it may not
> - Whether enough latent roots emerge to make `totalZero == d` is **unpredictable** from the pArray alone
> 
> This is why the determined/undetermined classification is **probabilistic** for higher-degree polynomials: the algorithm doesn't control latent root emergence; it only discovers roots after construction.

---

## Part II: Vertical Propagation — Between Consecutive Levels

### 2.1 The Difference Chain Structure

The graph connects polynomials vertically through `:zMap` relationships:

```
Source Polynomial P(x)         degree = dimension
         │
         │ :zMap (Δ¹)
         ▼
First Difference Δ¹P(x)        degree = dimension - 1
         │
         │ :zMap (Δ²)
         ▼
Second Difference Δ²P(x)       degree = dimension - 2
         │
        ...
         │
         ▼
Constant Δ^dimension P(x)      degree = 0
```

### 2.2 How Zeros Propagate Downward

**Theorem (Zero Propagation).** If P has consecutive integer roots at k and k+1, then:

```
Δ¹P(k) = P(k+1) - P(k) = 0 - 0 = 0
```

**Generalization:** If P has r consecutive integer roots starting at k, then Δⁱ P(k) = 0 for all i < r.

** similarly if P has p(k)=p(k+1) then
triag p (k) =0.

## note this is a particular and general
so candidate for superposition anaysis.

**Example:** For P(x) = x(x-1)(x-2)(x-3):

```
k:      0    1    2    3    4
P(k):   0    0    0    0   24
Δ¹P:       0    0    0   24
Δ²P:          0    0   24
Δ³P:            0   24
Δ⁴P:              24
```

All zeros appear in column k=0 because of the consecutive roots {0,1,2,3}.

### 2.3 The Diophantine Connection at Consecutive Levels

**Key relationship:** Consecutive TriagTriag levels satisfy linear Diophantine equations:

```
ax! + bx! = c(x+1)!
```

Where:
- The factorial scalars arise from the difference operator's action on power terms
- x = -y in the canonical solution form
- The relationship guarantees **algebraic consistency** between levels

**Implication for determinedness:** The Diophantine structure means that:
1. A zero at level Ψⱼ **constrains** the values at level Ψⱼ₊₁
2. Determined status at one level **influences** (but does not guarantee) determined status at adjacent levels
3. The factorial scalar modulus determines the propagation strength

### 2.4 Propagation of Determinedness

**Observation:** Determinedness does NOT propagate uniformly:

| Scenario | Parent Status | Child Status | Why |
|----------|---------------|--------------|-----|
| All consecutive roots | Determined | Determined | Zeros propagate via differences |
| Non-consecutive roots | Determined | May be undetermined | Difference destroys zero structure |
| Missing roots | Undetermined | May be determined | Lower degree, fewer roots needed |

**Example of asymmetric propagation:**

```
P(x) = (x-0)(x-5)  [roots at 0 and 5, non-consecutive]

Level 0: degree 2, roots {0,5} → determined (2 roots found)
Level 1: degree 1, may have no integer roots → could be undetermined
```

The difference Δ¹P(x) = P(x+1) - P(x) may have its root at a non-integer value, making Level 1 undetermined despite Level 0 being determined.

---

## Part III: Horizontal Structure — Within the Same Level

### 3.1 The Level as a Population

At each difference level d, there exists a **population of polynomials** — all polynomials that appear at that level across different source polynomial chains.

```
Level d = 2 (Quadratic Difference Level):

  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
  │ Quad₁   │   │ Quad₂   │   │ Quad₃   │   │ Quad₄   │
  │ roots:  │   │ roots:  │   │ roots:  │   │ roots:  │
  │ {0,1}   │   │ {0,2}   │   │ {1,2}   │   │ {5}     │
  │ DET     │   │ DET     │   │ DET     │   │ UNDET   │
  └─────────┘   └─────────┘   └─────────┘   └─────────┘
```

### 3.2 Preceding/Following Relationship at Same Level

**Question:** How do polynomials at the same level relate to each other?

**Horizontal ordering:** Polynomials at level d can be ordered by:
1. Their coefficient vectors (lexicographic on vmResult)
2. Their root patterns (lexicographic on muList)
3. Their binary encodings (numerical on χ value)

**Adjacent polynomials:** Two polynomials at level d are "horizontally adjacent" if their binary encodings differ by 1:

```
Polynomial A: muList = [0,1] → binary 011 → integer 3
Polynomial B: muList = [2]   → binary 100 → integer 4

A and B are horizontally adjacent (3 → 4)
```

### 3.3 The Quadratic Level Example

**Focus:** Difference level d = 2 (quadratic polynomials in the difference chain)

**Properties:**
- Each quadratic has at most 2 integer roots
- Determined quadratics have exactly 2 integer roots
- Undetermined quadratics have 0 or 1 integer root

**Root configurations for determined quadratics:**

For integerRange R, possible determined quadratics have root pairs from:
```
{(i, j) : 0 ≤ i < j < R, i,j ∈ ℤ}
```

This is exactly **C(R, 2) = R(R-1)/2** possible root configurations.

**Example (R = 5):**

| Root Pair (i,j) | muList | Binary | Integer |
|-----------------|--------|--------|---------|
| (0,1) | [0,1] | 011 | 3 |
| (0,2) | [0,2] | 101 | 5 |
| (0,3) | [0,3] | 1001 | 9 |
| (0,4) | [0,4] | 10001 | 17 |
| (1,2) | [1,2] | 110 | 6 |
| (1,3) | [1,3] | 1010 | 10 |
| (1,4) | [1,4] | 10010 | 18 |
| (2,3) | [2,3] | 1100 | 12 |
| (2,4) | [2,4] | 10100 | 20 |
| (3,4) | [3,4] | 11000 | 24 |

**Total:** C(5,2) = 10 determined quadratics possible.

### 3.4 Constructing Integer Polynomials with Prescribed Roots

**Question:** What integer-coefficient polynomials produce a determined quadratic at level d = 2?

**Construction:** For roots at positions r₁ and r₂:

```
The difference level 2 polynomial must factor as:
Δ²P(x) = c(x - r₁)(x - r₂) for some constant c

Working backward:
Δ¹P(x) = ∫ Δ²P dx  (discrete anti-difference)
P(x)   = ∫ Δ¹P dx  (discrete anti-difference)
```

**Constraint:** For P to have integer coefficients, the anti-difference operations must produce integers.

**Factorial constraint:** Since Δⁿ(xⁿ) = n!, the reconstruction involves:

```
Level 2 → Level 1: divide by 2! = 2
Level 1 → Level 0: divide by 1! = 1
```

**Divisibility requirement:** The leading coefficient of Δ²P must be divisible by the appropriate factorials for the anti-difference to produce integer coefficients.

---

## Part IV: The Completeness Hypothesis

### 4.1 Statement of the Hypothesis

**Central Claim:** For a graph generated with parameters (dimension, integerRange, setProductRange), the graph contains representatives of **all possible root configurations** — specifically:

> For dimension = d and integerRange = R, the determined polynomials at each level L form a subset that, with sufficient generation parameters, covers C(R, d-L) distinct root configurations.

### 4.2 The Power Set Correspondence

At each difference level L, the degree is d - L. The possible root configurations form a combinatorial structure:

| Level | Degree | Max Roots | Configurations | Count |
|-------|--------|-----------|----------------|-------|
| L = 0 | d | d | C(R, d) | R!/(d!(R-d)!) |
| L = 1 | d-1 | d-1 | C(R, d-1) | R!/((d-1)!(R-d+1)!) |
| L = 2 | d-2 | d-2 | C(R, d-2) | R!/((d-2)!(R-d+2)!) |
| ... | ... | ... | ... | ... |
| L = d | 0 | 0 | 1 (constant) | 1 |

### 4.3 Why C(R, k) at Each Level?

**Argument:**

1. **Determined polynomial at level L** has exactly k = d - L integer roots
2. These k roots are **distinct** positions chosen from R available positions
3. The number of ways to choose k positions from R is C(R, k)
4. Each distinct choice of k positions corresponds to a **unique root pattern**
5. Therefore, there are C(R, k) possible determined polynomials (up to scaling)

### 4.4 The Quadratic Level in Detail

For the **quadratic difference level** (degree 2):

```
Number of determined configurations = C(R, 2) = R(R-1)/2
```

**Why this is the complete set:**

1. **Quadratic polynomials** have exactly 2 roots (in ℂ)
2. **Determined quadratics** have both roots as integers in range [0, R-1]
3. **Order doesn't matter** for roots: {r₁, r₂} = {r₂, r₁}
4. **All pairs** are achievable via polynomial construction
5. **No gaps** exist in the combinatorial enumeration

**Example verification for R = 10:**

C(10, 2) = 45 distinct root pairs

If the graph contains 45 distinct determined quadratic nodes at level d = 2, the completeness hypothesis is validated at that level.

### 4.5 Generation Parameters for Completeness

**Conjecture:** The graph achieves completeness when:

```
setProductRange^dimension ≥ C(integerRange, dimension)
```

This ensures enough source polynomials are explored to produce all root configurations through the difference process.

**For quadratic level (k = 2):**

```
Need sufficient polynomials to cover C(R, 2) = R(R-1)/2 configurations
```

---

## Part V: The Algebraic Foundation

### 5.1 Why Integer Polynomials Admit the Dichotomy

**Integer polynomial:** P(x) ∈ ℤ[x] — polynomial with integer coefficients.

**Fact 1:** Integer polynomials evaluated at integers produce integers:
```
P ∈ ℤ[x], n ∈ ℤ  ⟹  P(n) ∈ ℤ
```

**Fact 2:** Differences of integer polynomials are integer polynomials:
```
P ∈ ℤ[x]  ⟹  ΔP ∈ ℤ[x]
```

**Fact 3:** Zero detection is exact for integers:
```
P(n) = 0 ⟺ n is a root of P
```

**Consequence:** The determined/undetermined classification is **well-defined** for integer polynomials — no floating-point ambiguity about whether a value is "close to zero."

### 5.2 The Rational Root Theorem

**Theorem:** If P(x) = aₙxⁿ + ... + a₁x + a₀ ∈ ℤ[x] has a rational root p/q (in lowest terms), then:
- p divides a₀ (constant term)
- q divides aₙ (leading coefficient)

**Implication for monic polynomials (aₙ = 1):**

All rational roots are integers that divide the constant term.

**Implication for determinedness:**

For monic integer polynomials, rational roots ARE integer roots. Thus:
- If all roots are rational → can be determined
- If some roots are irrational → necessarily undetermined

### 5.3 Constructing Polynomials with Prescribed Integer Roots

**Recipe:** To create a polynomial determined at level d with roots {r₁, r₂, ..., rₖ}:

```python
# Construct difference polynomial at level d
delta_d_P = product((x - rᵢ) for rᵢ in roots)  # = (x-r₁)(x-r₂)...(x-rₖ)

# Anti-difference to get higher levels
delta_d_minus_1_P = anti_difference(delta_d_P)
delta_d_minus_2_P = anti_difference(delta_d_minus_1_P)
...
P = anti_difference(delta_1_P)
```

**Challenge:** The anti-difference operation may introduce non-integer coefficients unless the polynomial satisfies factorial divisibility conditions.

### 5.4 Factorial Divisibility Conditions

For the anti-difference to produce integer polynomials:

**Condition at level k:** The leading coefficient of Δᵏ P must be divisible by k!.

**Why:** Since Δᵏ(xᵏ/k!) = 1 (the "divided difference basis"), integer polynomial reconstruction requires:

```
Coefficient of xᵏ in Δᵏ P = k! × (some integer)
```

**Connection to Diophantine equations:**

The factorial divisibility conditions manifest as the Diophantine equations:
```
ax! + bx! = c(x+1)!
```

These are automatically satisfied when constructing via the difference operator, ensuring the chain of polynomials remains in ℤ[x].

---

## Part VI: Graph Representation of the Dichotomy

### 6.1 Node Properties Encoding the Dichotomy

| Property | Meaning |
|----------|---------|
| `determined` | 1 if determined, 0 if undetermined |
| `totalZero` | Count of integer roots found |
| `d` | Polynomial degree at this level |
| `muList` | Positions of roots (for determined nodes) |
| `vmResult` | Polynomial coefficients |

### 6.2 Graph Queries for Dichotomy Analysis

**Count by classification at each level:**

```cypher
MATCH (n:Dnode)
RETURN n.d AS degree,
       n.determined AS status,
       COUNT(n) AS count
ORDER BY degree, status
```

**Verify completeness at quadratic level:**

```cypher
MATCH (n:Dnode {d: 2, determined: 1})
RETURN COUNT(DISTINCT n.muList) AS distinct_root_patterns

// Compare to C(integerRange, 2)
```

**Find undetermined nodes with most roots:**

```cypher
MATCH (n:Dnode {determined: 0})
RETURN n.d AS degree,
       n.totalZero AS roots_found,
       n.d - n.totalZero AS roots_missing,
       n.vmResult AS polynomial
ORDER BY roots_found DESC
LIMIT 10
```

### 6.3 Visual Representation

```
THE DICHOTOMY AT LEVEL d = 3 (Cubic Difference Level)
══════════════════════════════════════════════════════

DETERMINED (totalZero = 3)         UNDETERMINED (totalZero < 3)
┌─────────────────────────┐        ┌─────────────────────────┐
│                         │        │                         │
│  muList: [0,1,2]       │        │  muList: [0,1]         │
│  muList: [0,1,3]       │        │  muList: [5]           │
│  muList: [0,2,3]       │        │  muList: []            │
│  muList: [1,2,3]       │        │  (missing roots:       │
│  muList: [0,1,4]       │        │   complex, irrational, │
│  ...                    │        │   or out of range)     │
│                         │        │                         │
│  Total: C(R, 3) nodes   │        │  Total: varies          │
│                         │        │                         │
└─────────────────────────┘        └─────────────────────────┘
         │                                   │
         │                                   │
         ▼                                   ▼
   Complete enumeration              Boundary cases
   of root patterns                  (partial information)
```

---

## Part VII: Open Questions and Further Research

### 7.1 Completeness Verification

**Question:** For given parameters (dimension, integerRange, setProductRange), does the graph actually contain all C(R, k) configurations at level k?

**Verification approach:**
1. Extract all determined nodes at level k
2. Parse muList to get root positions
3. Count distinct root patterns
4. Compare to C(R, k)

### 7.2 Transition Probabilities

**Question:** Given a determined polynomial at level L, what is the probability its child at level L+1 is also determined?

**Factors affecting transition:**
- Consecutiveness of roots
- Coefficient magnitudes
- Factorial divisibility

### 7.3 Prediction Task

**ML Application:** Given an undetermined node, can we predict:
1. How many roots are missing?
2. What type (complex, irrational, out-of-range)?
3. Where might the missing roots be located?

### 7.4 Inverse Problem

**Question:** Given a target muList (root pattern), can we efficiently find or construct the source polynomial P that produces this pattern at level L?

---

## Part VIII: The Dichotomy as a Graph Generation Artifact

### 8.1 What "Artifact" Means

The dichotomy is not **explicitly programmed** into the graph generator. Rather, it **emerges naturally** from the algorithmic process of:

1. Evaluating polynomials at integer points
2. Computing iterated differences
3. Detecting zeros in the difference sequences
4. Recording which polynomials have all expected roots

The graph generator does not "decide" which polynomials are determined — it discovers this classification by counting zeros and comparing to expected degree.

### 8.2 The Generation Algorithm

The ZerosAndDifferences Java application generates the graph through this pipeline:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         GRAPH GENERATION PIPELINE                            │
│                                                                              │
│  STEP 1: Coefficient Iteration                                              │
│  ─────────────────────────────                                              │
│  For each coefficient combination in setProductRange^dimension:             │
│      pArray = [a₀, a₁, a₂, ..., aₙ]                                        │
│                                                                              │
│  STEP 2: Polynomial Evaluation                                              │
│  ──────────────────────────────                                              │
│  For k in [-integerRange/2, +integerRange/2):                               │
│      P(k) = a₀ + a₁k + a₂k² + ... + aₙkⁿ                                   │
│      if P(k) == 0: record in muList, increment totalZero                   │
│                                                                              │
│  STEP 3: Difference Computation                                             │
│  ───────────────────────────────                                             │
│  For each level wNum = 1, 2, ..., dimension:                                │
│      ΔʷP(k) = Δʷ⁻¹P(k+1) - Δʷ⁻¹P(k)                                       │
│      if ΔʷP(k) == 0: record zero, increment totalZero                      │
│                                                                              │
│  STEP 4: Classification (THE DICHOTOMY EMERGES HERE)                        │
│  ────────────────────────────────────────────────────                        │
│  For each node at level wNum with degree = dimension - wNum:                │
│      if totalZero == degree:                                                │
│          determined = 1   ← DETERMINED                                      │
│      else:                                                                  │
│          determined = 0   ← UNDETERMINED                                    │
│                                                                              │
│  STEP 5: Graph Construction                                                 │
│  ───────────────────────────                                                 │
│  Create :Dnode with properties (vmResult, muList, totalZero, determined)   │
│  Create :zMap edges connecting difference levels                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Why the Dichotomy Emerges Naturally

**Reason 1: Finite Integer Evaluation Range**

The generator evaluates polynomials only at integers in [-R/2, +R/2). This finite window creates a natural boundary:
- Roots inside the window → detected and counted
- Roots outside the window → invisible, contribute to underdetermination

```
      ← Out of Range →│← Observable Range →│← Out of Range →
    ────────────────────┼───────────────────┼────────────────────
                     -R/2                 +R/2
                        │    roots here    │
                        │    are counted   │
```

**Reason 2: Integer-Only Zero Detection**

The algorithm only detects zeros at integer evaluation points:
- Integer roots → P(k) = 0 exactly
- Irrational roots → P(k) ≠ 0 for all integer k
- Complex roots → P(k) ≠ 0 for all real k

This creates the separation: polynomials with all integer roots (determined) vs. those with non-integer roots (undetermined).

**Reason 3: Difference Degree Reduction**

Each difference operation reduces degree by 1:
```
wNum = 0: degree = dimension (source polynomial)
wNum = 1: degree = dimension - 1
wNum = 2: degree = dimension - 2
...
wNum = dimension: degree = 0 (constant)
```

The dichotomy criterion adapts to each level:
```java
determined = (totalZero == dimension - wNum) ? 1 : 0;
```

This is not a design choice — it's a mathematical necessity from the Fundamental Theorem of Algebra.

### 8.4 The Artifact Nature: Not Designed, Discovered

**Key insight:** The graph generator does not implement a theory of determined/undetermined polynomials. Instead:

1. It **implements Newton's differences** — a 17th-century algorithm
2. It **counts zeros** — a mechanical process
3. It **compares counts** — simple arithmetic
4. The dichotomy **falls out** of these operations

This is why we call it an **artifact** — it's a byproduct of computation, not an intended feature.

### 8.5 Why This Matters

**For Machine Learning:**

The dichotomy is a **ground truth label** that emerges from structure, not annotation. When training models to predict `determined`, we're not predicting an arbitrary label — we're predicting a mathematically inevitable classification.

**For the Bijection Hypothesis:**

The determined polynomials form the "valid" elements of the power set structure. The bijection ℤ ↔ ℚ operates on these nodes. Undetermined nodes represent **boundary cases** where the bijection mapping is incomplete or undefined.

**For Verification:**

Because the dichotomy is an artifact of computation, it can be independently verified:
- Re-run the algorithm with same parameters → same classification
- Compute roots symbolically → compare to detected zeros
- Check factorial divisibility → predict determinedness

### 8.6 Generation Parameters Affecting the Dichotomy

| Parameter | Effect on Dichotomy |
|-----------|---------------------|
| `dimension` | Higher → more roots expected → harder to be determined |
| `integerRange` | Larger → more detection chances → more determined nodes |
| `setProductRange` | Larger → more polynomials explored → more coverage of configurations |

**Critical relationship:**

```
P(determined) ∝ integerRange / (dimension × coefficient_magnitude)
```

- Large evaluation range + moderate coefficients → many determined nodes
- Small range + large coefficients → few determined nodes

### 8.7 The Dichotomy Distribution

For typical generation parameters:

```
┌───────────────────────────────────────────────────────────────┐
│          DICHOTOMY DISTRIBUTION BY LEVEL                       │
│                                                                │
│  Level 0 (degree = dim):     Few determined (most roots        │
│                               missing due to high degree)      │
│                                                                │
│  Level dim/2 (middle):       Mixed — some determined,          │
│                               some undetermined                │
│                                                                │
│  Level dim (constant):       All determined (no roots          │
│                               expected, none needed)           │
│                                                                │
│  General pattern:                                              │
│                                                                │
│  Level:     0    1    2    3    4    5                        │
│  Degree:    5    4    3    2    1    0                        │
│  P(det):   Low  Low  Med  High High 100%                      │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

### 8.8 Code Insight: Where the Dichotomy is Computed

From `GaussTable1.java`:

```java
int determined = 0;
if (dimension - wNum == totalZero) { 
    determined = 1; 
}
```

These three lines embody the entire dichotomy. The classification is:
- **Local** — depends only on the node's own properties
- **Deterministic** — no randomness, no heuristics
- **Mathematical** — derived from degree-root relationship

The graph generator **computes** this value; it does not **choose** it.

---

## Summary

The determined/undetermined dichotomy is not merely a classification — it is a **structural principle** that:

1. **Distinguishes** polynomials with complete vs. partial root information
2. **Propagates** through the difference chain via Diophantine relationships
3. **Organizes** each level into combinatorial families (C(R, k) configurations)
4. **Enables** the power set hypothesis connecting polynomials to integers via binary encoding
5. **Admits** verification through graph analysis and counting

The dichotomy is the key to understanding why the polynomial difference graph serves as a **mathematical artifact** encoding both integers (via χ) and rationals (via μ) — the determined nodes form the "interior" of this structure, while undetermined nodes form the "boundary."

---

## References

- `abstract.md` — Section on Determined/Undetermined Dichotomy
- `graph_theory.md` — Complete analysis of undetermined causes
- `math_verification.md` — Examples of zero propagation
- `graph_structure.md` — Node property definitions
- Newton's Forward Difference Formula
- Fundamental Theorem of Algebra
- Rational Root Theorem


