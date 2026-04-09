# The Graph: Understanding `graphsample.json`

This document explains the graph structure exported in `graphsample.json`, connecting the Neo4j data to the mathematical foundations described in `html-with-appendix-and-toc.html`.

---

## Overview

The graph represents **polynomial difference sequences** where:
- Each **node** (`:Dnode`) is a polynomial at some difference level
- Each **edge** (`:zMap`) connects a polynomial to its forward difference
- The **rational encoding** (`n/d`) maps root patterns to rationals via the μ function

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GRAPH STRUCTURE                                  │
│                                                                          │
│   Source Polynomial P(x)                                                 │
│          │                                                               │
│          │ :zMap                                                         │
│          ▼                                                               │
│   First Difference Δ¹P(x)                                               │
│          │                                                               │
│          │ :zMap                                                         │
│          ▼                                                               │
│   Second Difference Δ²P(x)                                              │
│          │                                                               │
│         ...                                                              │
│          │                                                               │
│          ▼                                                               │
│   Constant (Δⁿ⁻¹P = n! × leading coefficient)                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Data Format: Cypher Paths

The JSON contains **Cypher path objects** from Neo4j queries:

```json
{
  "p": {
    "start": { /* source Dnode */ },
    "end": { /* target Dnode */ },
    "segments": [
      {
        "start": { /* Dnode */ },
        "relationship": { "type": "zMap", ... },
        "end": { /* Dnode */ }
      }
    ],
    "length": 1.0
  }
}
```

---

## Node Properties (`:Dnode`)

Each node represents a polynomial derived from the difference analysis:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vmResult` | String | Monomial coefficients [aₙ, ..., a₁, a₀] (descending), from exact Newton interpolation (`NewtonInterpolator`) of the difference table | `"[0.0, 24.0, -24.0]"` |
| `muList` | String | Adjusted root positions (see note below) | `"[1]"` or `"[]"` |
| `n` | Integer | **Numerator** of μ rational (from set union ratio) | `1` |
| `d` | Integer | **Denominator** of μ rational (from set union ratio) | `1` |
| `totalZero` | Integer | Count of integer roots found within evaluation range | `1` |
| `determined` | Integer | Classification: `1` = all expected roots found, `0` = underdetermined | `1` |
| `rootList` | Array | **True mathematical x-positions** where polynomial equals zero | `[1]` |
| `wccComponent` | Integer | Weakly Connected Component ID (from graph algorithm) | `0` |

> **muList vs rootList:** These may contain different values due to the `caiIndex` adjustment in the algorithm. `muList` stores positions adjusted by a cumulative counter (e.g., `[4, 16]`), while `rootList` stores the true mathematical x-coordinates (e.g., `[4, 17]`). **Use `rootList` when you need actual root positions.** See [graph_comprehension.md](../documentation/graph_comprehension.md#mulist-vs-rootlist-the-caiindex-adjustment) for details.

### Property Relationships

```
muList = "[k₁, k₂, ...]"  ←──  Binary 1-positions (Definition 2.2)
         │
         ▼
    Binary Decoder (Definition 2.3)
         │
         ▼
    Set Collection Å = {A₀, A₁, ...}
         │
         ▼
    μ(Å) = |∪A| / Σ|A|  =  n/d  (Definition 2.1)
```

---

## The μ Rational Encoding

From `html-with-appendix-and-toc.html`, the **Set Union Ratio** defines how `muList` maps to rationals:

### Definition 2.1 (Set Union Ratio)

```
μ(Å) = |∪_{A∈Å} A| / Σ_{A∈Å} |A|

Where:
  n = |∪A|    (cardinality of union — numerator)
  d = Σ|A|    (sum of set sizes — denominator)
```

### Definition 2.2 (Binary Encoding Rule)

```
χᵢ(Å) = 1  if i = n + |Aₙ| - 1 for some Aₙ ∈ Å
χᵢ(Å) = 0  otherwise
```

The `muList` property stores the positions where χᵢ = 1.

### Definition 2.3 (Binary Decoder)

Given `muList = [i₀, i₁, ...]`, recover the set collection:

```
For each position iₖ where χᵢ = 1:
  Solve: iₖ = n + |Aₙ| - 1
  Get: |Aₙ| = iₖ - n + 1
  Build: Aₙ = {1, 2, ..., |Aₙ|}
```

---

## Examples from `graphsample.json`

### Example 1: Empty Root Set (Undetermined)

```json
{
  "vmResult": "[0.0, 3.9999999999999996, 6.0, 4.0, 1.0]",
  "muList": "[]",
  "d": 0,
  "n": 0,
  "totalZero": 0,
  "determined": 0
}
```

**Interpretation:**
- Polynomial: P(x) ≈ 0 + 4x + 6x² + 4x³ + x⁴ = (x+1)⁴
- No integer roots detected in range (`muList = "[]"`)
- μ = 0/0 (empty set collection)
- **Underdetermined**: `totalZero=0` but polynomial has degree 4

### Example 2: Single Root (Determined)

```json
{
  "vmResult": "[0.0, 24.0, -24.0]",
  "muList": "[1]",
  "d": 1,
  "n": 1,
  "totalZero": 1,
  "determined": 1,
  "rootList": [1]
}
```

**Interpretation:**
- vmResult coefficients in descending order: [a₂, a₁, a₀] = [0, 24, -24]
- Polynomial: P(x) = 0x² + 24x - 24 = 24x - 24 = 24(x - 1)
- Root at x = 1 (`muList = "[1]"`, `rootList = [1]`)
- Binary encoding: χ₁ = 1 → binary "01" → integer 2
- Set collection via decoder: A₀ with |A₀| = 1 - 0 + 1 = 2
- μ = 2/2 = 1, stored as n=1, d=1
- **Determined**: Linear polynomial with 1 root found

### Example 3: Root Sequence Pattern

Nodes with `muList = "[k]"` for k = 1, 2, 3, ... show a progression:

| muList | vmResult | n | d | μ |
|--------|----------|---|---|---|
| `"[1]"` | `[0.0, 24.0, -24.0]` | 1 | 1 | 1 |
| `"[2]"` | `[0.0, 24.0, -48.0]` | 2 | 2 | 1 |
| `"[3]"` | `[0.0, 24.0, -72.0]` | 3 | 3 | 1 |
| `"[4]"` | `[0.0, 24.0, -96.0]` | 4 | 4 | 1 |

**Pattern:** For single roots, μ = k/k = 1 always. The raw (n, d) values preserve the root index information before simplification.

### Example 4: Constant Polynomial (End of Chain)

```json
{
  "vmResult": "[0.0, 24.0]",
  "muList": "[]",
  "d": 0,
  "n": 0,
  "totalZero": 0,
  "determined": 1
}
```

**Interpretation:**
- vmResult coefficients in descending order: [a₁, a₀] = [0, 24]
- Polynomial: P(x) = 0x + 24 = 24 (constant)
- This is the end of a difference chain
- **Determined**: Constant polynomial with no roots (degree 0, totalZero=0)

---

## Relationship Structure (`:zMap`)

The `:zMap` relationship connects polynomials in the difference sequence:

```json
"relationship": {
  "type": "zMap",
  "start": 13,
  "end": 11,
  "properties": {}
}
```

**Semantics:**
- `start` → `end`: The polynomial at `start` has `end` as its forward difference (or vice versa in traversal)
- Creates a **tree structure** from source polynomials down to constants
- Multiple source polynomials may **converge** to the same difference polynomial

### Graph Topology

```
         [P₁(x)]        [P₂(x)]        [P₃(x)]
              \           |           /
               \          |          /
                ▼         ▼         ▼
              [Δ¹P₁]   [Δ¹P₂]   [Δ¹P₃]
                   \      |      /
                    \     |     /
                     ▼    ▼    ▼
                    [Shared Δ²P]    ← Convergence point
                          |
                          ▼
                      [Constant]
```

---

## Polynomial Coefficient Interpretation

The `vmResult` array represents monomial coefficients recovered from the sampled difference sequence (unique interpolant of degree ≤ d on the evaluation nodes). The **primary** implementation is `NewtonInterpolator` in Java (Newton divided differences, `BigDecimal`, then descending-power monomials). **Legacy** paths used `MatrixA.transcribePowers()` with Vandermonde + `GaussMain`; that ordering convention matches the same descending-power layout.

```
vmResult = [aₙ, aₙ₋₁, ..., a₁, a₀]   (DESCENDING powers)

P(x) = aₙxⁿ + aₙ₋₁xⁿ⁻¹ + ... + a₁x + a₀
```

> **Important Distinction: pArray vs vmResult**
>
> - **`vmResult`** (on Dnode nodes): Polynomial coefficients `[aₙ, ..., a₁, a₀]` in descending power order
> - **`pArray`** (on CreatedBy nodes): Zero-position signature `[r₀, r₁, r₂, ...]` where `rᵢ` is the x-position where difference level `wNum=i` equals zero
>
> These are different data structures serving different purposes:
> - `vmResult` tells you **what polynomial** is at a given Dnode
> - `pArray` tells you **which x-values produce zeros** at each difference level, linking CreatedBy nodes to their corresponding Dnodes

### Coefficient Patterns

| vmResult | Polynomial | Degree | Roots |
|----------|------------|--------|-------|
| `[0.0, 24.0]` | 24 (constant) | 0 | none |
| `[0.0, 24.0, -240.0]` | 24x - 240 = 24(x-10) | 1 | x = 10 |
| `[0.0, 12.0, -252.0, 816.0]` | 12x² - 252x + 816 | 2 | x = 4, 17 |

**Note:** The leading coefficient aₙ is often 0, meaning the effective degree is less than the array length minus 1. Values like `3.9999999999999996` instead of `4.0` can appear from **double** serialization in `vmResult`, from **legacy** Vandermonde/Gauss output, or from older graph exports — not from exact `BigDecimal` arithmetic before formatting.

---

## Connection to the Bijection Theory

The graph structure supports the **dual mapping** approach to ℤ ↔ ℚ bijection:

### Mapping 1: Set Collection → Rational (μ)

```
muList → Binary Decoder → Set Collection Å → μ(Å) = n/d ∈ ℚ
```

### Mapping 2: Set Collection → Integer (χ)

```
muList → Binary string → Positional value → N ∈ ℤ
```

### Graph as Mathematical Artifact

Each `:Dnode` with non-empty `muList` represents:
1. A **polynomial** with specific integer roots
2. A **set collection** encoding those roots
3. A **rational number** via μ
4. An **integer** via binary encoding χ

The `:zMap` relationships encode the **difference operation**, creating a mathematical structure where:
- **Determined nodes** (all roots found) correspond to power set elements
- **Underdetermined nodes** represent boundary cases (complex/irrational roots, out-of-range)

---

## Sample Subgraph Visualization

From the JSON, a typical path looks like:

```
Node 629420                          Node 9
┌─────────────────────┐              ┌─────────────────────┐
│ vmResult:           │              │ vmResult:           │
│  [0.0, 24.0, -24.0] │    :zMap     │  [0.0, 24.0]        │
│ muList: "[1]"       │ ──────────►  │ muList: "[]"        │
│ n: 1, d: 1          │              │ n: 0, d: 0          │
│ totalZero: 1        │              │ totalZero: 0        │
│ determined: 1       │              │ determined: 1       │
└─────────────────────┘              └─────────────────────┘
   Quadratic with                       Linear (or constant
   root at x=1                          after differencing)
```

---

## Statistics from Sample

Based on `graphsample.json` analysis:

| Metric | Value |
|--------|-------|
| Path objects | ~100+ |
| Unique nodes | ~50+ distinct identities |
| Relationship type | All `:zMap` |
| Nodes with roots (`muList ≠ "[]"`) | ~15+ |
| Determined nodes | Mix of 0 and 1 |
| Max `totalZero` observed | 4 |
| Root positions observed | 1 through 10+ |

---

## Related Documentation

- [abstract.md](abstract.md) — Mathematical foundations and bijection theory
- [graph_structure.md](graph_structure.md) — Full Neo4j schema documentation
- [graph_theory.md](graph_theory.md) — Theoretical background
- [html-with-appendix-and-toc.html](../html-with-appendix-and-toc.html) — Original bijection paper with μ and χ definitions

---

## Key Takeaways

1. **`muList` is the bridge** — It stores binary 1-positions that decode into set collections
2. **`n/d` is the μ rational** — Computed from the set collection via Definition 2.1
3. **`:zMap` encodes differences** — The forward difference operator creates the graph edges
4. **Determined vs Underdetermined** — Whether all expected roots were found within the integer evaluation range
5. **Dual representation** — Each node can be mapped to both a rational (μ) and an integer (χ binary encoding)


