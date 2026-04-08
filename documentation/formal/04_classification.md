# Classification: Determined vs Undetermined Polynomials

## Overview

The **determined/undetermined classification** is a fundamental property of polynomial nodes in the difference graph. It distinguishes between polynomials whose integer root structure is fully captured versus those with "hidden" roots outside the observable domain.

---

## Part 1: The Classification Criterion

### 1.1 Definition

A polynomial P at difference level `wNum` with **polynomial degree = dimension - wNum** is:

```
DETERMINED      if  totalZero == (dimension - wNum)
UNDETERMINED    if  totalZero < (dimension - wNum)
```

Where:
- `totalZero` = count of integer zeros found within evaluation range [-R/2, +R/2)
- `dimension - wNum` = polynomial degree at this difference level
- `dimension` = degree of the original (source) polynomial
- `wNum` = difference level (0 = original, 1 = first difference, etc.)

> **Note:** The `d` property on Dnode stores the **μ denominator** (Σ muList values), NOT polynomial degree. See [03_implementation.md](03_implementation.md) for property definitions.

### 1.2 Mathematical Foundation

**Fundamental Theorem of Algebra:** A polynomial of degree d has exactly d roots in ℂ (counting multiplicity).

**Integer restriction:** When we restrict to integer roots within a finite range, we can find **at most** d roots. The classification arises from whether we find exactly d or fewer than d.

### 1.3 Code Implementation

From `GaussTable1.java`:

```java
int determined = 0;
if (dimension - wNum == totalZero) { 
    determined = 1; 
}
```

These three lines embody the entire classification — it's **local** (depends only on node properties), **deterministic** (no randomness), and **mathematical** (derived from degree-root relationship).

---

## Part 2: Why Polynomials Are Undetermined

When `totalZero < d`, the "missing" roots belong to one or more categories:

### 2.1 The Five Categories of Missing Roots

| Category | Description | Example | Real? | Integer? | Detectable? |
|----------|-------------|---------|-------|----------|-------------|
| **Out of Range** | Integer roots at \|x\| > R/2 | x = 500 with R = 200 | Yes | Yes | **No** |
| **Positive Definite** | P(x) > 0 for all real x | x² + 1 | No | No | **No** |
| **Complex Conjugate** | Roots of form a ± bi | 1 ± 2i | No | No | **No** |
| **Irrational** | Real roots r ∈ ℝ \ ℤ | √2 | Yes | No | **No** |
| **Boundary Effects** | Roots at exact range boundary | Edge case | Yes | Yes | **Maybe** |

### 2.2 Category Details

#### Out of Range

**Example:** P(x) = (x - 200)(x - 300) with integerRange = 200
- True roots: {200, 300}
- Observable range: [-100, +99]
- Detectable: none → **undetermined**

#### Positive/Negative Definite

**Criterion:** P(x) > 0 (or < 0) for all x ∈ ℝ — polynomial never crosses zero.

**Examples:**
```
P(x) = x² + 1           Always > 0, roots at ±i
P(x) = x⁴ + x² + 1      Always > 0, no real roots
P(x) = -x² - 1          Always < 0, roots at ±i
```

#### Complex Conjugate Roots

**From algebra:** Complex roots of real-coefficient polynomials come in conjugate pairs: a ± bi

**Example:** P(x) = x² - 2x + 5
- Discriminant: 4 - 20 = -16 < 0
- Roots: 1 ± 2i (complex)
- P(k) ≠ 0 for all integer k → **undetermined**

#### Irrational Roots

**Example:** P(x) = x² - 2
- Roots: ±√2 ≈ ±1.414...
- P(1) = -1 ≠ 0, P(2) = 2 ≠ 0
- No integer k gives P(k) = 0 → **undetermined**

**Key insight:** Irrational roots are "invisible" to integer-only evaluation, just like complex roots.

---

## Part 3: Vertical Propagation — Between Levels

### 3.1 The Difference Chain

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

### 3.2 Zero Propagation

**Theorem (Zero Propagation).** If P has consecutive integer roots at k and k+1, then:

```
Δ¹P(k) = P(k+1) - P(k) = 0 - 0 = 0
```

**Generalization:** If P has r consecutive integer roots starting at k, then Δⁱ P(k) = 0 for all i < r.

**Example:** For P(x) = x(x-1)(x-2)(x-3):

```
k:      0    1    2    3    4
P(k):   0    0    0    0   24
Δ¹P:       0    0    0   24
Δ²P:          0    0   24
Δ³P:            0   24
Δ⁴P:              24
```

### 3.3 Propagation of Determinedness

**Observation:** Determinedness does NOT propagate uniformly:

| Scenario | Parent Status | Child Status | Why |
|----------|---------------|--------------|-----|
| All consecutive roots | Determined | Determined | Zeros propagate via differences |
| Non-consecutive roots | Determined | May be undetermined | Difference destroys zero structure |
| Missing roots | Undetermined | May be determined | Lower degree, fewer roots needed |

---

## Part 4: Horizontal Structure — Same Level

### 4.1 Root Configurations at Fixed Degree

At difference level L with degree k = d - L, determined polynomials have exactly k integer roots chosen from R available positions.

**Count:** C(R, k) = R!/(k!(R-k)!) possible determined configurations.

### 4.2 Example: Quadratic Level (d = 2)

For integerRange R = 5, possible determined quadratics have root pairs:

| Root Pair (i,j) | muList | Binary | Integer |
|-----------------|--------|--------|---------|
| (0,1) | [0,1] | 011 | 3 |
| (0,2) | [0,2] | 101 | 5 |
| (0,3) | [0,3] | 1001 | 9 |
| (1,2) | [1,2] | 110 | 6 |
| (1,3) | [1,3] | 1010 | 10 |
| (2,3) | [2,3] | 1100 | 12 |
| ... | ... | ... | ... |

**Total:** C(5,2) = 10 determined quadratics possible.

---

## Part 5: The Completeness Hypothesis

### 5.1 Statement

**Central Claim:** For a graph generated with parameters (dimension, integerRange, setProductRange), the graph contains representatives of **all possible root configurations**:

> For dimension = d and integerRange = R, the determined polynomials at each level L form a subset that, with sufficient generation parameters, covers C(R, d-L) distinct root configurations.

### 5.2 Power Set Correspondence

| Level | Degree | Configurations | Count |
|-------|--------|----------------|-------|
| L = 0 | d | C(R, d) | R!/(d!(R-d)!) |
| L = 1 | d-1 | C(R, d-1) | R!/((d-1)!(R-d+1)!) |
| L = 2 | d-2 | C(R, d-2) | ... |
| L = d | 0 | 1 (constant) | 1 |

### 5.3 Generation Parameters for Completeness

**Conjecture:** Completeness is achieved when:

```
setProductRange^dimension ≥ C(integerRange, dimension)
```

---

## Part 6: Degree-Determinedness Relationship

### 6.1 Empirical Pattern

| Degree | wNum (dim=4) | P(determined) | Reasoning |
|--------|--------------|---------------|-----------|
| 0 | 4 | Always (trivial) | No roots needed |
| 1 | 3 | Almost always | Single root easy to find |
| 2 | 2 | Often | May have complex roots |
| 3 | 1 | Sometimes | Three roots needed |
| 4 | 0 | Rarely | Four integer roots unlikely |

**Key insight:** P(determined) decreases as degree increases.

### 6.2 Why Only Linear Is "Certainly Determined"

For a degree-1 polynomial P(x) = ax + b:
- Has exactly 1 root at x = -b/a
- If coefficients are integers and -b/a is an integer within range → determined
- The single-root requirement is easily satisfied

For higher degrees:
- Need ALL k roots (for degree k) to be integers within range
- Each additional root multiplies the probability of failure

### 6.3 The Latent Root Connection

The algorithm **specifies** one root per level (via pArray), but may **discover** additional roots (latent roots) created as side effects of coefficient adjustment.

**Classification depends on latent root emergence:**

| Scenario | Result |
|----------|--------|
| Adjustment creates enough latent roots | totalZero == degree → **determined** |
| Insufficient latent roots | totalZero < degree → **undetermined** |

This is unpredictable from pArray alone — the ML classification task must learn patterns in coefficients that predict latent root emergence.

---

## Part 7: Classification as Graph Artifact

### 7.1 Emergent, Not Designed

The classification is not **explicitly programmed** into the generator. It **emerges naturally** from:

1. Evaluating polynomials at integer points
2. Computing iterated differences
3. Detecting zeros in the difference sequences
4. Comparing found zeros to expected degree

### 7.2 Generation Parameters Affecting Classification

| Parameter | Effect |
|-----------|--------|
| `dimension` | Higher → more roots expected → harder to be determined |
| `integerRange` | Larger → more detection chances → more determined nodes |
| `setProductRange` | Larger → more polynomials → better coverage |

### 7.3 Distribution Pattern

```
Level 0 (degree = dim):     Few determined (most roots missing)
Level dim/2 (middle):       Mixed — some determined, some undetermined  
Level dim (constant):       All determined (no roots needed)

General: P(determined) increases as wNum increases (degree decreases)
```

---

## Part 8: Implications

### For Machine Learning

The classification is a **ground truth label** that emerges from structure, not annotation. When training models to predict `determined`, we're predicting a mathematically inevitable classification.

### For the Bijection Hypothesis

Determined polynomials form the "valid" elements of the power set structure. The bijection ℤ ↔ ℚ operates on these nodes. Undetermined nodes represent **boundary cases** where the bijection mapping is incomplete.

### For Verification

Because the classification is an artifact of computation, it can be independently verified:
- Re-run the algorithm with same parameters → same classification
- Compute roots symbolically → compare to detected zeros
- Check factorial divisibility → predict determinedness

---

## Graph Queries for Analysis

### Count by Classification at Each Level

```cypher
MATCH (n:Dnode)
RETURN n.d AS degree,
       n.determined AS status,
       COUNT(n) AS count
ORDER BY degree, status
```

### Verify Completeness at Quadratic Level

```cypher
MATCH (n:Dnode {d: 2, determined: 1})
RETURN COUNT(DISTINCT n.muList) AS distinct_root_patterns
// Compare to C(integerRange, 2)
```

### Find Undetermined Nodes with Most Roots

```cypher
MATCH (n:Dnode {determined: 0})
RETURN n.d AS degree,
       n.totalZero AS roots_found,
       n.d - n.totalZero AS roots_missing
ORDER BY roots_found DESC
LIMIT 10
```

---

## Summary

The determined/undetermined classification is a **structural principle** that:

1. **Distinguishes** polynomials with complete vs. partial root information
2. **Propagates** through the difference chain via factorial relationships
3. **Organizes** each level into combinatorial families
4. **Enables** the power set hypothesis connecting polynomials to integers
5. **Provides** ground truth labels for ML classification tasks

---

*This document condenses and modernizes content from `dichotomy.md`, using "classification" terminology.*

