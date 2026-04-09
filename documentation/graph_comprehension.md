# Graph Comprehension: Understanding Neo4j Query Results

This document explains how to interpret Neo4j query results from the Zeros and Differences graph database, connecting the graph structure to the underlying polynomial difference algorithm.

---

## Sample Query

```cypher
MATCH (n:CreatedBy {pArray:"[7, 15, 17, 10, 0]"})
WITH n LIMIT 20
MATCH (n)<-[r:CreatedBye]-(m)
RETURN n, r, m
```

This query retrieves:
1. `CreatedBy` nodes with a specific `pArray` signature
2. `Dnode` nodes connected via `:CreatedBye` relationships
3. The complete difference table construction trace for a polynomial

---

## The Polynomial

From the `vmResult` on the Dnode at wNum=0:

```
vmResult = [0.0, 1.0, -46.0, 539.0, 1546.0, -23856.0]
```

> **Note:** Coefficients are stored in **descending power order** (highest power first), as determined by `MatrixA.java:transcribePowers()`.

| Index | Coefficient | Power | Column Label |
|-------|-------------|-------|--------------|
| 0 | 0 | x⁵ | (leading, but zero) |
| 1 | 1 | x⁴ | Dx⁴ |
| 2 | -46 | x³ | Ex³ |
| 3 | 539 | x² | Fx² |
| 4 | 1546 | x¹ | Gx |
| 5 | -23856 | x⁰ | (constant) |

**Polynomial:** P(x) = x⁴ - 46x³ + 539x² + 1546x - 23856

---

## Understanding pArray

**Critical Insight:** `pArray` is **not** the polynomial coefficients — it encodes the **x-positions where each difference level equals zero**.

### pArray = [7, 15, 17, 10, 0]

| Index | pArray Value | Difference Level | Meaning |
|-------|--------------|------------------|---------|
| 0 | **7** | Ψ₀ = f(x) | Original polynomial has root at x=7 |
| 1 | **15** | Ψ₁ = Δ¹P | First difference has root at x=15 |
| 2 | **17** | Ψ₂ = Δ²P | Second difference has root at x=17 |
| 3 | **10** | Ψ₃ = Δ³P | Third difference has root at x=10 |
| 4 | **0** | Ψ₄ = Δ⁴P | Terminal (constant level) |

---

## Difference Table Verification

For polynomial P(x) = x⁴ - 46x³ + 539x² + 1546x - 23856:

```
Increment:           1      2      6      24     120
x      f(x)         Δ      ΔΔ     Δ³     Δ⁴     Δ⁵
────────────────────────────────────────────────────
-7     9912      -12408   3168   -408    24      0
-6    -2496       -9240   2760   -384    24      0
...
 4   -11736        3960      0   -144    24      0    ← ΔΔ(4) = 0
...
 7        0        3552   -360    -72    24      0    ← f(7) = 0 ✓
...
10     9504        2280   -504      0    24      0    ← Δ³(10) = 0 ✓
...
15    15984           0   -264    120    24      0    ← Δ(15) = 0 ✓
...
17    15720        -408      0    168    24      0    ← ΔΔ(17) = 0 ✓
```

### Verification Summary

| pArray[i] | Level | Table Check |
|-----------|-------|-------------|
| 7 | f(x) | f(7) = 0 ✓ |
| 15 | Δ | Δ(15) = 0 ✓ |
| 17 | ΔΔ | ΔΔ(17) = 0 ✓ |
| 10 | Δ³ | Δ³(10) = 0 ✓ |

The fourth difference Δ⁴ = 24 = 4! confirms this is a degree-4 polynomial.

---

## Graph Structure

### Node Types

#### CreatedBy Nodes
Act as an index connecting the source polynomial specification to computed results.

| Property | Description |
|----------|-------------|
| `pArray` | Zero-position signature (string format) |
| `pArrayList` | Same as pArray (native array) |
| `wNum` | Worker number = difference level (0, 1, 2, ...) |
| `resultId` | Links nodes from same computation batch |
| `skipList` | Boolean flags for processing logic |

#### Dnode Nodes
Contain the actual mathematical analysis at each difference level.

| Property | Description |
|----------|-------------|
| `vmResult` | Monomial coefficients [aₙ, ..., a₀] from `NewtonInterpolator` (legacy path: Vandermonde/`GaussMain`) |
| `muList` | All x-positions where this level equals zero |
| `n` | Numerator of μ rational encoding |
| `d` | Denominator of μ rational encoding |
| `totalZero` | Count of integer roots found at this level |
| `determined` | 1 if all expected roots found, 0 otherwise |
| `rootList` | Native array version of muList |
| `wccComponent` | Weakly Connected Component ID |

### Relationship: CreatedBye

```
(Dnode) -[:CreatedBye]-> (CreatedBy)
```

Links each difference level's analysis (Dnode) back to the source polynomial specification (CreatedBy with matching wNum).

---

## Algorithm Flow Visualization

```
                    ┌──────────────────────────────┐
                    │      pArray Polynomial       │
                    │     [7, 15, 17, 10, 0]       │
                    │  "Zero-position signature"   │
                    └──────────────────────────────┘
                                   │
        ┌──────────┬───────────┬───┴───┬───────────┬──────────┐
        ▼          ▼           ▼       ▼           ▼          
   CreatedBy   CreatedBy   CreatedBy  CreatedBy  CreatedBy
    wNum=0      wNum=1      wNum=2     wNum=3     wNum=4
        ▲          ▲           ▲       ▲           ▲
        │          │           │       │           │
  :CreatedBye :CreatedBye :CreatedBye :CreatedBye :CreatedBye
        │          │           │       │           │
        ▼          ▼           ▼       ▼           ▼
   ┌────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ ┌────────┐
   │ Dnode  │ │ Dnode  │ │  Dnode  │ │ Dnode  │ │ Dnode  │
   │  Ψ₀    │ │  Ψ₁    │ │   Ψ₂    │ │  Ψ₃    │ │  Ψ₄    │
   │        │ │        │ │         │ │        │ │        │
   │muList: │ │muList: │ │ muList: │ │muList: │ │(const) │
   │  [7]   │ │  [15]  │ │ [4,16]  │ │  [10]  │ │   24   │
   │ n=7    │ │ n=15   │ │  n=16   │ │ n=10   │ │        │
   │ d=7    │ │ d=15   │ │  d=20   │ │ d=10   │ │        │
   └────────┘ └────────┘ └─────────┘ └────────┘ └────────┘
       │          │           │          │
       ▼          ▼           ▼          ▼
   Root at    Root at    Roots at    Root at
    x=7        x=15      x=4, x=16    x=10
```

---

## Mathematical Interpretation

### The Difference Chain

```
P(x) has root at x=7
    │
    ▼ Δ (forward difference)
Δ¹P has root at x=15
    │
    ▼ Δ
Δ²P has roots at x=4, x=17
    │
    ▼ Δ
Δ³P has root at x=10
    │
    ▼ Δ
Δ⁴P = 24 = 4! (constant, confirming degree 4)
```

### Key Observations

1. **pArray encodes a path** through the difference table — the primary zero at each level
2. **muList may contain multiple zeros** at a given level (e.g., [4, 16] at Ψ₂)
3. **vmResult** is the polynomial recovered via Newton interpolation of the difference table (primary); Vandermonde/Gauss on some legacy paths
4. **n/d** encodes the μ rational (set union ratio) for the bijection theory
5. **Discovery-first principle** — the algorithm discovers zeros by evaluating polynomials, not by constructing them from pArray constraints

> **For the construction mechanism:** See [algorithm_construction.md](algorithm_construction.md) for a detailed explanation of how the algorithm iteratively adjusts coefficients to satisfy root constraints specified by pArray.

---

## Latent Variables and the pArray Signature

A critical insight: **pArray encodes only ONE root per difference level**, serving as a path identifier through the difference table. However, each level may have **multiple roots** — all of which appear in `muList`.

### The Distinction

| Property | Contains | Purpose |
|----------|----------|---------|
| `pArray[i]` | One root at level i | Path identifier / signature |
| `muList` | All roots at that level | Complete root enumeration |

### Example: Dnode 1129239 at Ψ₂

```
CreatedBy nodes with pArray [7, 15, 17, 10, 0] and [7, 16, 17, 10, 0]
both point to the same Dnode 1129239:

  pArray[2] = 17    (both pArrays specify this as the "signature" root)
  muList = [4, 16]  (actual discovered roots — but see caiIndex note below)
  rootList = [4, 17]  (true mathematical positions)
```

### Why x=4 is a "Latent Variable"

The root at x=4 exists mathematically — it is **discovered** by the algorithm when evaluating the second difference polynomial. However, it is not part of the pArray signature because:

1. **pArray serves as an identifier** — each position stores one representative root
2. **The algorithm iterates through coefficients** to generate polynomials, discovers their zeros, then records ONE zero per level in the pArray signature
3. **Additional roots are "latent"** — they exist, are found, and stored in muList, but don't appear in the path signature

This is why `muList = [4, 16]` contains two roots while `pArray[2] = 17` specifies only one.

### The Mechanism: Why Latent Roots Emerge

Latent roots are not random — they are a **mathematical consequence** of the coefficient adjustment mechanism used to force specified roots.

**How the algorithm creates a root at x=17:**

```
Step 1: Evaluate Q(17) = a(17)² + b(17) + c = some_value

Step 2: Normalize by factorial: adjustment = some_value / 2!

Step 3: Adjust constant: c_new = c - adjustment

Step 4: New polynomial: Q'(x) = ax² + bx + c_new
        Now Q'(17) = 0  ✓
```

**The side effect:** Changing `c` affects Q'(x) for ALL values of x. The new polynomial Q'(x) may have additional integer roots that weren't specified — these are the latent roots.

**For the quadratic at Dnode 1129239:**

- pArray specifies: force root at x=17
- Adjustment: changes constant term c → c_new
- Result: Q'(x) has roots at x=4 AND x=17
- The root at x=4 is **latent** — it emerged as a side effect of forcing x=17

**Key insight:** The algorithm doesn't predict or control latent roots. They emerge from the specific coefficient values inherited from higher difference levels. This is why determinedness is **probabilistic** for higher-degree polynomials.

See [algorithm_construction.md Part 7](algorithm_construction.md) for the detailed code mechanism.

---

## muList vs rootList: The caiIndex Adjustment

You may notice that `muList` and `rootList` sometimes differ:

```
Dnode 1129239:
  muList = [4, 16]
  rootList = [4, 17]
```

**Why the discrepancy?** The algorithm uses a **Cumulative Adjustment Index (caiIndex)** that shifts muList values.

### The caiIndex Mechanism

From `LoopList.java` lines 381-387:

```java
if (listResult.compareTo(zero) == 0 && (y > halfIntegerRange)) {
    muList.add(y - caiIndex - halfIntegerRange);
    caiIndex++;   // ← increment AFTER adding
}
```

Each time a zero is detected:
1. Store `y - caiIndex - halfIntegerRange` in muList
2. **Then** increment caiIndex

### Worked Example

For Dnode 1129239 with true roots at x=4 and x=17:

| Detection | y (index) | caiIndex | Stored Value | True x |
|-----------|-----------|----------|--------------|--------|
| 1st root | 104 | 0 | 104 - 0 - 100 = **4** | 4 |
| 2nd root | 117 | 1 | 117 - 1 - 100 = **16** | 17 |

So:
- **muList = [4, 16]** — adjusted positions due to caiIndex
- **rootList = [4, 17]** — true mathematical x positions

### Which to Use?

| Property | Use When |
|----------|----------|
| `rootList` | You need the actual x-coordinates where the polynomial equals zero |
| `muList` | Internal algorithm logic, or when matching the stored format |

The pArray values correspond to `rootList` (true positions), not `muList` (adjusted positions).

---

## Structural Sharing: Convergence of Difference Sequences

A key property of the graph is **structural sharing** — when two different polynomials produce the same difference polynomial at some level, they share the same Dnode from that point downward.

### Demonstration: Comparing Two pArrays

Using a UNION query to compare:
- `pArray = [7, 15, 17, 10, 0]`
- `pArray = [7, 16, 17, 10, 0]`

```cypher
CALL {
  MATCH (n:CreatedBy {pArray:"[7, 16, 17, 10, 0]"})
  WITH n LIMIT 20
  MATCH (n)<-[r:CreatedBye]-(m)
  RETURN n, r, m
  UNION
  MATCH (n:CreatedBy {pArray:"[7, 15, 17, 10, 0]"})
  WITH n LIMIT 20
  MATCH (n)<-[r:CreatedBye]-(m)
  RETURN n, r, m
}
RETURN n, r, m
```

### Results: Dnode Identity Comparison

| wNum | pArray `[7,15,17,10,0]` | pArray `[7,16,17,10,0]` | Same? |
|------|-------------------------|-------------------------|-------|
| 0 | Dnode **1131211** | Dnode **1131342** | ❌ Different |
| 1 | Dnode **1131165** | Dnode **1131296** | ❌ Different |
| 2 | Dnode **1129239** | Dnode **1129239** | ✅ **SAME** |
| 3 | Dnode **1118395** | Dnode **1118395** | ✅ **SAME** |
| 4 | Dnode **9** | Dnode **9** | ✅ **SAME** |

### Convergence Visualization

```
pArray [7,15,17,10,0]              pArray [7,16,17,10,0]
         │                                  │
    ┌────┴────┐                        ┌────┴────┐
    │ Dnode   │                        │ Dnode   │
    │ 1131211 │  ←── DIFFERENT ──→     │ 1131342 │
    │ Ψ₀      │                        │ Ψ₀      │
    └────┬────┘                        └────┬────┘
         │                                  │
    ┌────┴────┐                        ┌────┴────┐
    │ Dnode   │                        │ Dnode   │
    │ 1131165 │  ←── DIFFERENT ──→     │ 1131296 │
    │muList:  │                        │muList:  │
    │  [15]   │                        │  [16]   │
    └────┬────┘                        └────┬────┘
         │                                  │
         └─────────────┬────────────────────┘
                       │
                       ▼ CONVERGENCE POINT
                  ┌─────────┐
                  │ Dnode   │
                  │ 1129239 │  ← SHARED
                  │muList:  │
                  │ [4,16]  │
                  └────┬────┘
                       │
                  ┌────┴────┐
                  │ Dnode   │
                  │ 1118395 │  ← SHARED
                  │muList:  │
                  │  [10]   │
                  └────┬────┘
                       │
                  ┌────┴────┐
                  │ Dnode 9 │  ← SHARED
                  │ Ψ₄=24   │
                  └─────────┘
```

### Mathematical Significance

1. **Different source polynomials** produce different vmResults at Ψ₀
2. **Different first differences** — root at x=15 vs x=16 at Ψ₁
3. **Convergence at Ψ₂** — the second difference is identical for both
4. **Shared cascade** — Ψ₃ and Ψ₄ are inherited from the shared Ψ₂

### Why This Matters

- **Efficiency:** Avoids redundant storage of identical difference polynomials
- **Structure:** Reveals mathematical relationships between polynomials
- **Graph Topology:** Creates a DAG (directed acyclic graph) where multiple paths converge
- **Bijection Theory:** Shared Dnodes represent equivalence classes in the μ encoding

### Implications for Queries

When querying by pArray, you get **distinct CreatedBy nodes** but may get **shared Dnode nodes** at lower difference levels. This is by design — the algorithm correctly identifies when difference sequences converge.

---

## Connection to Bijection Theory

Each Dnode encodes dual mappings:

| Encoding | Formula | Result |
|----------|---------|--------|
| **μ (rational)** | \|∪A\| / Σ\|A\| | n/d ∈ ℚ |
| **χ (integer)** | Binary encoding of muList positions | z ∈ ℤ |

This supports the project's goal of establishing a computational ℤ ↔ ℚ bijection through polynomial difference analysis.

---

## Useful Queries

### Find all difference levels for a pArray
```cypher
MATCH (n:CreatedBy {pArray:"[7, 15, 17, 10, 0]"})
MATCH (n)<-[r:CreatedBye]-(m:Dnode)
RETURN n.wNum, m.muList, m.vmResult, m.n, m.d
ORDER BY n.wNum
```

### Find polynomials with specific root pattern
```cypher
MATCH (m:Dnode {muList:"[7]"})
MATCH (m)-[:CreatedBye]->(n:CreatedBy {wNum:0})
RETURN n.pArray, m.vmResult
```

### Traverse the difference chain via zMap
```cypher
MATCH path = (start:Dnode)-[:zMap*]->(end:Dnode)
WHERE start.muList = "[7]"
RETURN path
```

---

## Related Documentation

- [algorithm_construction.md](algorithm_construction.md) — How polynomials are constructed to satisfy root constraints
- [theory.md](theory.md) — Mathematical foundations (Newton's differences, bijection framework)
- [the_graph.md](../GraphMLDifferences/the_graph.md) — Full graph schema documentation
- [math_verification.md](../GraphMLDifferences/math_verification.md) — Difference table computations and verification


