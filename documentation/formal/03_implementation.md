# Implementation: Algorithm Construction and Graph Schema

This document explains the core mechanism by which the ZerosAndDifferences algorithm constructs polynomials satisfying root constraints, and documents the resulting graph structure in Neo4j.

---

## Part 1: Algorithm Overview

### 1.1 The Constraint Satisfaction Problem

Given a target root signature `pArray = [r₀, r₁, r₂, ..., rₙ]`, the algorithm constructs a polynomial P(x) such that:

| Level | Constraint |
|-------|------------|
| Ψ₀ | P(r₀) = 0 |
| Ψ₁ | Δ¹P(r₁) = 0 |
| Ψ₂ | Δ²P(r₂) = 0 |
| ... | ... |
| Ψₙ₋₁ | Δⁿ⁻¹P(rₙ₋₁) = 0 |

### 1.2 Top-Down Iteration Strategy

The algorithm processes constraints from **highest difference level down to level 0**:

```
for (wCount = dimension-1; wCount >= 0; wCount--) {
    1. Get factorial: rModulo = moduloList[wCount]
    2. Adjust coefficient: computeIndexZero(wCount, rModulo)
    3. Rebuild evaluation arrays: updateRlistB()
}
```

### 1.3 Why Top-Down Order Matters

Higher-level coefficient adjustments don't disturb lower-level constraints:

| Adjustment to | Affects | Does NOT affect |
|---------------|---------|-----------------|
| Cubic term (x³) | Δ³P | Δ⁴P, Δ⁵P, ... |
| Quadratic term (x²) | Δ²P | Δ³P, Δ⁴P, ... |
| Linear term (x) | Δ¹P | Δ²P, Δ³P, ... |
| Constant term | P(x) | Δ¹P, Δ²P, ... |

**Mathematical basis:** Δⁿ of a polynomial of degree < n equals zero. So adjusting figPArray[k] (degree-k term) only affects difference levels ≤ k.

---

## Part 2: Key Data Structures

### 2.1 moduloList — Factorial Lookup Table

**Location:** `LoopsLogic/ModuloList.java`

```java
public class ModuloList extends ArrayList {
    public ModuloList(int dimension) {
        this.add(one);                              // index 0: 1
        for (int n = 0; n < dimension; n++) {
            modulo = modulo.multiply(factorialCounter);
            factorialCounter = factorialCounter.add(one);
            this.add(modulo);                       // index n+1: n!
        }
    }
}
```

**Contents for dimension=4:**

| Index | Value | Meaning |
|-------|-------|---------|
| 0 | 1 | (base) |
| 1 | 1 | 1! |
| 2 | 2 | 2! |
| 3 | 6 | 3! |
| 4 | 24 | 4! |

**Purpose:** Provides n! for normalizing difference values. The nth difference of xⁿ equals n!, so dividing by n! recovers the original coefficient.

### 2.2 figPArray — The Mutable Coefficient Array

`figPArray` holds the polynomial coefficients being iteratively adjusted:

```
figPArray = [c₀, c₁, c₂, c₃, c₄]
             │    │    │    │    └── x⁴ coefficient
             │    │    │    └─────── x³ coefficient
             │    │    └──────────── x² coefficient
             │    └───────────────── x¹ coefficient
             └────────────────────── constant term
```

### 2.3 rListB — The Evaluation Array

`rListB` is a list of `LoopList` objects, one per difference level:

```
rListB[0] = LoopList for Ψ₀ (original polynomial values)
rListB[1] = LoopList for Ψ₁ (first differences)
rListB[2] = LoopList for Ψ₂ (second differences)
...
rListB[n] = LoopList for Ψₙ (nth differences)
```

---

## Part 3: The Core Mechanism

### 3.1 computeIndexZero — The Constraint Enforcer

**Location:** `LoopLists/LoopListener.java`

```java
private void computeIndexZero(int n, BigDecimal rModulo) {
    // n = current wCount (difference level)
    // rModulo = n! from moduloList
    
    // Get Gauss-reduced coefficient matrix for level n
    A = ((LoopList)rListB.get(n)).getgMatrix();
    
    // Evaluate polynomial at x = pArray[n] (the target root position)
    for (int k = 0; k < i; k++) {
        result = result + evalCoeff(pArray.getpValue(n), 
                                    (int)Math.rint(currentCoefficient), 
                                    denumerator);
    }
    
    // Normalize by factorial
    result = result / intModulo;
    
    // Adjust coefficient to force zero at target position
    newFigPArrayIndexValue = currentFigPArrayIndexValue - result;
    figPArray.set(n, newFigPArrayIndexValue);
}
```

**What this does:**
1. **Get matrix A** — the Gauss-reduced coefficients from the current difference level
2. **Evaluate at target x** — computes P(pArray[n]) using the matrix coefficients
3. **Divide by n!** — normalizes the factorial scaling from difference operations
4. **Subtract from current coefficient** — adjusts figPArray[n] so re-evaluation yields zero

### 3.2 The Iterative Refinement Cycle

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ITERATIVE REFINEMENT CYCLE                          │
│                                                                             │
│  ┌─────────────┐      ┌──────────────────┐      ┌─────────────────┐        │
│  │ figPArray   │─────▶│ computeIndexZero │─────▶│ figPArray'      │        │
│  │ (current)   │      │ (adjust coeff)   │      │ (modified)      │        │
│  └─────────────┘      └──────────────────┘      └────────┬────────┘        │
│                                                          │                  │
│                                                          ▼                  │
│  ┌─────────────┐      ┌──────────────────┐      ┌─────────────────┐        │
│  │ rListB'     │◀─────│   updateRlistB   │◀─────│ figPArray'      │        │
│  │ (rebuilt)   │      │ (rebuild arrays) │      │                 │        │
│  └──────┬──────┘      └──────────────────┘      └─────────────────┘        │
│         │                                                                   │
│         └───────────────────────────────────────────────────────────────▶   │
│                           NEXT ITERATION (wCount--)                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Part 4: The Diophantine Guarantee

### 4.1 Why Division by n! Always Produces Integers

When `computeIndexZero` performs `result = result / intModulo` (where intModulo = n!), this division is **guaranteed to produce an integer** because:

1. Polynomial at level n has coefficients that are factorial-scaled (Δⁿ produces n! × leading coefficient)
2. The Diophantine structure `ax! + bx! = c(x+1)!` ensures all sums at level n remain multiples of n!
3. When evaluating at x = pArray[n] and summing, the result is a multiple of n!
4. Therefore: result / n! ∈ ℤ (always an integer)

### 4.2 The Bidirectional Relationship

| Direction | Operation | Factorial Role | Guaranteed by |
|-----------|-----------|----------------|---------------|
| **Down** (Δ) | Difference | Multiply by n! | Definition of Δⁿ |
| **Up** (anti-Δ) | Reconstruction | Divide by n! | Diophantine solvability |

---

## Part 5: Latent Roots

### 5.1 The Side Effect Phenomenon

When `computeIndexZero` adjusts a coefficient to force a root at one position, it changes the **entire polynomial**. This may accidentally create additional integer roots.

```
BEFORE adjustment:
  Q(x) = ax² + bx + c
  Q(17) = 580  (not zero)

computeIndexZero adjusts c:
  c_new = c - (580 / 2!)

AFTER adjustment:
  Q'(x) = ax² + bx + c_new
  Q'(17) = 0  ✓  (INTENDED - specified in pArray)
  Q'(4)  = ?     (UNKNOWN - may accidentally be zero)
```

### 5.2 Latent Root Categories

The algorithm **discovers** additional roots beyond pArray specification:

| Scenario | Result |
|----------|--------|
| Adjustment creates enough latent roots | totalZero == degree → **determined** |
| Adjustment creates some latent roots | totalZero < degree → **undetermined** |
| No latent roots (complex/irrational) | totalZero < degree → **undetermined** |

**Key insight:** The algorithm cannot predict whether forcing one root will create enough additional roots to make the polynomial determined.

---

## Part 6: Graph Schema

### 6.1 Node Types

#### DifferencePolynomial Node (`:Dnode`)

The primary node type representing a polynomial at a difference level.

| Property | Type | Description |
|----------|------|-------------|
| `vmResult` | String | Polynomial coefficients **[aₙ, aₙ₋₁, ..., a₁, a₀]** in descending power order |
| `n` | Integer | Numerator of μ rational (max of muList values) |
| `d` | Integer | Denominator of μ rational (= Σ muList values). **Not polynomial degree** |
| `totalZero` | Integer | Count of integer roots found in evaluation range |
| `muList` | String | Adjusted root positions (indices where polynomial = 0) |
| `rootList` | Array | True mathematical root positions |
| `determined` | Integer | 1 if (dimension - wNum) == totalZero, 0 otherwise |

> **Important:** 
> - Polynomial degree at a node = `dimension - wNum`, NOT the `d` property
> - The `d` property stores the μ denominator for the bijection mapping
> - **vmResult coefficients are in DESCENDING power order** (highest power first), as determined by `MatrixA.java:transcribePowers()`

#### Origin Node (`:CreatedBy`)

Metadata tracking computational origin.

| Property | Type | Description |
|----------|------|-------------|
| `resultId` | Integer | Batch correlation ID |
| `wNum` | Integer | Difference level (0 = source, 1+ = differences) |
| `pArray` | String | Zero-position signature [r₀, r₁, ...] |

### 6.2 Relationship Types

#### ZeroMapping (`:zMap`)

```
(DifferencePolynomial)-[:zMap]->(DifferencePolynomial)
```

Connects a polynomial to its next-level difference. Creates a tree structure from source polynomial down to constant.

#### CreationLink (`:CreatedBye`)

```
(DifferencePolynomial)-[:CreatedBye]->(Origin)
```

Tracks provenance of each polynomial.

### 6.3 Important Distinction: pArray vs vmResult

| Property | Location | Contains | Purpose |
|----------|----------|----------|---------|
| `vmResult` | Dnode | Polynomial coefficients [aₙ, ..., a₁, a₀] (descending) | Identifies the polynomial |
| `pArray` | CreatedBy | Zero-position signature [r₀, r₁, ...] | Path identifier through graph |

### 6.4 muList vs rootList

| Property | Contains | Use Case |
|----------|----------|----------|
| `muList` | Adjusted positions (affected by caiIndex) | Internal algorithm tracking |
| `rootList` | True mathematical x-coordinates | Actual root positions |

**Example:** A quadratic might have `muList = [4, 16]` but `rootList = [4, 17]` due to cumulative index adjustment.

---

## Part 7: Graph Dimensions

### 7.1 Configuration Parameters

| Parameter | Effect |
|-----------|--------|
| `dimension` | Polynomial degree → tree depth (depth = dimension + 1) |
| `integerRange` | Evaluation range: k ∈ [-range/2, +range/2) |
| `setProductRange` | Coefficient iteration → graph width |

### 7.2 Width Formula

```
Global Width = setProductRange^dimension × branching_factor
Node Data Width (wNum=n) = integerRange - n
```

### 7.3 Convergence Behavior

As differences are computed, width **converges** toward lower levels:

| Level | Degree | Typical Width Behavior |
|-------|--------|----------------------|
| 0 | dimension | setProductRange^dimension unique nodes |
| 1 | dimension-1 | Slightly fewer (some merging) |
| ... | ... | More merging occurs |
| dimension | 0 | Heavy merging — many paths lead to same constant |

---

## Part 8: Multi-Threaded Architecture

### 8.1 Producer-Consumer Pattern

```
┌─────────────────────────────────────────────────────────────────────┐
│                     PRODUCER THREADS                                 │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ LoopsDriver  │  │ LoopListener │  │ LoopListener │  ...          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘               │
│         │                 │                 │                        │
│         └─────────────────┼─────────────────┘                        │
│                           ▼                                          │
│              ┌────────────────────────┐                              │
│              │   Shared Data Buffer   │  ← Semaphore-protected      │
│              │     (GaussBean1[])     │                              │
│              └───────────┬────────────┘                              │
└──────────────────────────┼───────────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    CONSUMER THREAD                                   │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                      GaussTable1                                │ │
│  │  • Acquires data from buffer via semaphore                     │ │
│  │  • Executes Cypher queries to Neo4j                            │ │
│  │  • Batches writes for performance                              │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                          │                                           │
│                          ▼                                           │
│              ┌────────────────────────┐                              │
│              │      Neo4j Database    │                              │
│              └────────────────────────┘                              │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Part 9: Complete Algorithm Flow

```
Input: pArray = [a₀, a₁, ..., aₙ]  (polynomial coefficients)
       dimension = n               (polynomial degree)
       integerRange = 200          (evaluation range)

┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Create LoopList for wNum=0 (Ψ₀)                        │
│   loadPArray(pArray)                                           │
│   For k = -100 to +99:                                         │
│       P(k) = a₀ + a₁k + a₂k² + ... + aₙkⁿ                     │
│       If P(k) == 0: record in muList, increment totalZero      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Steps 2..n: Create LoopList for wNum=1..n (Ψ₁..Ψₙ)            │
│   subsequentWorkerListInit()                                   │
│   For each k: Δⁱ P(k) = Δⁱ⁻¹ P(k+1) - Δⁱ⁻¹ P(k)              │
│   Track zeros at each level                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step Final: Interpolation & Database Write                      │
│   NewtonInterpolator.interpolate() — monomial vmResult          │
│     (primary; legacy: GaussMain.gauss() on Vandermonde)        │
│   GaussTable1 — write to Neo4j:                                │
│       :Dnode with vmResult, n, d, totalZero, muList, determined│
│       :zMap connecting difference levels                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Cypher Query Examples

### Get Polynomials by μ Denominator

```cypher
MATCH (p:Dnode)
RETURN p.vmResult AS polynomial, 
       p.d AS muDenominator, 
       p.totalZero AS zeros,
       p.determined AS isDetermined
ORDER BY p.d
```

### Find Convergence Points

```cypher
MATCH (child:Dnode)<-[r:zMap]-()
WITH child, COUNT(r) AS incomingCount
WHERE incomingCount > 1
RETURN child.vmResult, child.d, incomingCount
ORDER BY incomingCount DESC
```

### Traverse Difference Tree

```cypher
MATCH path = (root:Dnode {d: 0})-[:zMap*]->(leaf:Dnode)
WHERE NOT (leaf)-[:zMap]->()
RETURN path
```

---

## Part 10: vmResult — Polynomial Coefficient Parsing Reference

### 10.1 Overview

`vmResult` is a **String property on `:Dnode` nodes** that stores the polynomial coefficients (descending power order) for the unique interpolant at that difference level. The **primary** code path computes them with **`NewtonInterpolator`** (Newton divided differences on equally spaced integer samples, exact `BigDecimal`, conversion to monomial form, then `.doubleValue()` for the string). **Legacy / auxiliary** paths may still use **Vandermonde + `GaussMain.gauss()`**.

### 10.2 Data Structure

**Type:** String (serialized list of doubles)

**Source (primary):** `NewtonInterpolator.interpolate()` → monomial buffer → `GaussBean1.setVmResult()`

**Source (legacy):** `GaussMain.transcribeResult()` → `MatrixA.transcribePowers()`

**Storage Format:**
```
vmResult = "[c_n, c_{n-1}, c_{n-2}, ..., c_1, c_0]"
```

Where:
- `c_n` = coefficient of x^n (highest power term)
- `c_{n-1}` = coefficient of x^(n-1)
- ...
- `c_1` = coefficient of x (linear term)
- `c_0` = constant term

### 10.3 Coefficient Ordering

**CRITICAL:** Coefficients are stored in **DESCENDING power order** (highest degree first).

| Array Index | Power | Term | Example (cubic) |
|-------------|-------|------|-----------------|
| 0 | n | c_n · x^n | a₃ · x³ |
| 1 | n-1 | c_{n-1} · x^{n-1} | a₂ · x² |
| 2 | n-2 | c_{n-2} · x^{n-2} | a₁ · x |
| 3 | ... | ... | a₀ (constant) |
| ... | ... | ... | ... |
| n | 0 | c_0 | constant |

**Implementation Detail:** This ordering is established in `MatrixA.transcribePowers()` (lines 127-163):

```java
denumerator = this.i - 1;  // Start at highest power
for (int y=0; y<this.i; y++){
    this.A[x][y] = this.xPowers[x][denumerator];
    denumerator = denumerator - 1;  // Descend through powers
}
```

### 10.4 Parsing Examples

#### Example 1: Quadratic Polynomial
```
vmResult = "[2.0, -5.0, 3.0]"

Represents: P(x) = 2x² - 5x + 3

Parsing:
  vmResult[0] = 2.0   → coefficient of x²
  vmResult[1] = -5.0  → coefficient of x
  vmResult[2] = 3.0   → constant term
```

#### Example 2: Cubic Polynomial
```
vmResult = "[1.0, 0.0, -4.0, 7.0]"

Represents: P(x) = x³ - 4x + 7

Parsing:
  vmResult[0] = 1.0   → coefficient of x³
  vmResult[1] = 0.0   → coefficient of x² (zero coefficient)
  vmResult[2] = -4.0  → coefficient of x
  vmResult[3] = 7.0   → constant term
```

#### Example 3: Linear Polynomial
```
vmResult = "[3.0, -2.0]"

Represents: P(x) = 3x - 2

Parsing:
  vmResult[0] = 3.0   → coefficient of x
  vmResult[1] = -2.0  → constant term
```

#### Example 4: Constant Polynomial (Edge Case)
```
vmResult = "[5.0]"

Represents: P(x) = 5

Parsing:
  vmResult[0] = 5.0   → constant value

Note: At difference level wNum = dimension, all polynomials reduce to constants.
This is the deepest level of the difference tree.
```

### 10.5 Zero Coefficients

**Are there zero paddings?** No explicit zero padding, but zero coefficients appear naturally:

- If a polynomial lacks a term (e.g., x³ - 4x + 7 has no x² term), the corresponding coefficient is `0.0`
- Zero coefficients are **included** in vmResult at their proper position
- Array length always equals `(polynomial degree + 1)`

**Example:**
```
vmResult = "[1.0, 0.0, 0.0, -8.0]"  // P(x) = x³ - 8
                  ^^^  ^^^
            Missing x² and x terms → explicit zeros
```

### 10.6 Relationship to Node Properties

| Property | Contains | Relationship to vmResult |
|----------|----------|--------------------------|
| `vmResult` | **[c_n, ..., c_0]** | The polynomial coefficients in descending power order |
| `dimension` | Polynomial degree at wNum=0 | vmResult length at level wNum = (dimension - wNum + 1) |
| `wNum` | Difference level | Determines polynomial degree: degree = dimension - wNum |
| `pArray` | Zero-position signature | Input specification; vmResult is the computed solution |

### 10.7 Degree Determination from vmResult

```
Polynomial degree = vmResult.length - 1
```

| vmResult Length | Degree | Polynomial Type |
|-----------------|--------|-----------------|
| 1 | 0 | Constant |
| 2 | 1 | Linear |
| 3 | 2 | Quadratic |
| 4 | 3 | Cubic |
| n+1 | n | Degree-n polynomial |

### 10.8 Implementation Source Code References

**Coefficient generation (primary):**
- `NewtonInterpolator.java`: `computeNewtonCoeffs`, `newtonToMonomial`, `interpolate`

**Coefficient generation (legacy / auxiliary):**
- `MatrixA.java` (lines 127-163): `transcribePowers()` sets up descending power matrix
- `GaussMain.java` (lines 229-242): `transcribeResult()` extracts solution column

**Data flow (primary):**
```
LoopList samples (forward-difference table)
        ↓
NewtonInterpolator.computeNewtonCoeffs() / newtonToMonomial()
        ↓
vmResult String (stored on Dnode)
```

**Data flow (legacy Vandermonde path):**
```
MatrixA.xPowers (ascending)
        ↓
MatrixA.transcribePowers() (reverses to descending)
        ↓
MatrixA.gMatrix (Vandermonde matrix with descending powers)
        ↓
GaussMain.gauss() (Gaussian elimination)
        ↓
GaussMain.transcribeResult() (extracts A[x][m] column)
        ↓
vmResult String (stored on Dnode)
```

### 10.9 Reconstruction Formula

To evaluate P(x) from vmResult coefficients:

```
P(x) = Σ(i=0 to n) vmResult[i] · x^(n-i)
```

Or iteratively (Horner's method):
```
P(x) = vmResult[0]
for i = 1 to n:
    P(x) = P(x) · x + vmResult[i]
```

**Example in Java:**
```java
// Parse vmResult string to array
String vmResult = "[2.0, -5.0, 3.0]";  // 2x² - 5x + 3
double[] coeffs = parseVmResult(vmResult);
int degree = coeffs.length - 1;

// Evaluate at x = 4
double x = 4.0;
double result = coeffs[0];
for (int i = 1; i <= degree; i++) {
    result = result * x + coeffs[i];
}
// result = 2(16) - 5(4) + 3 = 32 - 20 + 3 = 15
```

### 10.10 Common Parsing Pitfalls

| Mistake | Consequence | Correction |
|---------|-------------|------------|
| Reading left-to-right as ascending powers | P(x) = 2 + 5x - 3x² (wrong!) | Always read as descending: 2x² - 5x + 3 |
| Assuming zero padding at end | Incorrect degree calculation | No padding; zeros only appear for missing terms |
| Confusing with `pArray` | Wrong interpretation | pArray = input spec; vmResult = computed coefficients |
| Using `d` property as degree | Wrong at all wNum levels | Use: degree = dimension - wNum |

### 10.11 Query Examples

**Cypher: Extract polynomial as readable form**
```cypher
MATCH (p:Dnode)
WITH p, 
     toInteger(p.d) AS muDenom,
     split(replace(replace(p.vmResult, "[", ""), "]", ""), ", ") AS coeffs
RETURN coeffs[0] + "x^" + toString(size(coeffs)-1) + " + ... + " + coeffs[-1] AS polynomial,
       p.totalZero AS roots,
       p.vmResult AS rawCoeffs
LIMIT 10
```

**Cypher: Find constant polynomials**
```cypher
MATCH (p:Dnode)
WHERE size(split(p.vmResult, ",")) = 1
RETURN p.vmResult AS constantValue,
       p.muList AS sourcePositions
```

---

*This document consolidates content from `algorithm_construction.md` and `graph_structure.md`.*

