# Algorithm Construction: How Polynomials Are Built to Satisfy Root Constraints

This document explains the core mechanism by which the ZerosAndDifferences algorithm constructs polynomials that have zeros at positions specified by `pArray`. This is the "how it works" companion to the "what it produces" documentation in [graph_comprehension.md](graph_comprehension.md).

---

## Part 1: Conceptual Overview

### The Constraint Satisfaction Problem

Given a target root signature `pArray = [r₀, r₁, r₂, ..., rₙ]`, the algorithm must construct a polynomial P(x) such that:

| Level | Constraint |
|-------|------------|
| Ψ₀ | P(r₀) = 0 |
| Ψ₁ | Δ¹P(r₁) = 0 |
| Ψ₂ | Δ²P(r₂) = 0 |
| ... | ... |
| Ψₙ₋₁ | Δⁿ⁻¹P(rₙ₋₁) = 0 |

### The Top-Down Iteration Strategy

The algorithm processes constraints from **highest difference level down to level 0**:

```
for (wCount = dimension-1; wCount >= 0; wCount--) {
    1. Get factorial: rModulo = moduloList[wCount]
    2. Adjust coefficient: computeIndexZero(wCount, rModulo)
    3. Rebuild evaluation arrays: updateRlistB()
}
```

### Why Top-Down Order Matters

Higher-level coefficient adjustments don't disturb lower-level constraints:

| Adjustment to | Affects | Does NOT affect |
|---------------|---------|-----------------|
| Cubic term (x³) | Δ³P | Δ⁴P, Δ⁵P, ... |
| Quadratic term (x²) | Δ²P | Δ³P, Δ⁴P, ... |
| Linear term (x) | Δ¹P | Δ²P, Δ³P, ... |
| Constant term | P(x) | Δ¹P, Δ²P, ... |

**Mathematical basis:** Δⁿ of a polynomial of degree < n equals zero. So adjusting figPArray[k] (degree-k term) only affects difference levels ≤ k.

### The Iterative Refinement Cycle

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

## Part 2: Key Data Structures

### moduloList — Factorial Lookup Table

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

### figPArray — The Mutable Coefficient Array

`figPArray` (figure polynomial array) holds the polynomial coefficients being iteratively adjusted:

```
figPArray = [c₀, c₁, c₂, c₃, c₄]
             │    │    │    │    └── x⁴ coefficient
             │    │    │    └─────── x³ coefficient
             │    │    └──────────── x² coefficient
             │    └───────────────── x¹ coefficient
             └────────────────────── constant term
```

Each iteration of `computeIndexZero` modifies one element to force a zero at the target position.

### rListB — The Evaluation Array

`rListB` (result list bean) is a list of `LoopList` objects, one per difference level:

```
rListB[0] = LoopList for Ψ₀ (original polynomial values)
rListB[1] = LoopList for Ψ₁ (first differences)
rListB[2] = LoopList for Ψ₂ (second differences)
...
rListB[n] = LoopList for Ψₙ (nth differences)
```

Each LoopList contains the polynomial/difference values evaluated over the integer range, plus the Gauss-reduced coefficient matrix.

### Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA STRUCTURE RELATIONSHIPS                       │
│                                                                             │
│   pArray                    moduloList                                      │
│   [r₀,r₁,r₂,...]           [1,1!,2!,3!,...]                                │
│        │                         │                                          │
│        │ target positions        │ factorial scalars                        │
│        │                         │                                          │
│        ▼                         ▼                                          │
│   ┌─────────────────────────────────────────────────────┐                   │
│   │              computeIndexZero(wCount, rModulo)       │                   │
│   │                                                      │                   │
│   │  • Evaluate at x = pArray[wCount]                   │                   │
│   │  • Normalize by moduloList[wCount]                  │                   │
│   │  • Adjust figPArray[wCount]                         │                   │
│   └──────────────────────┬──────────────────────────────┘                   │
│                          │                                                   │
│                          ▼                                                   │
│                    ┌───────────┐                                            │
│                    │ figPArray │ ←── coefficients modified                  │
│                    └─────┬─────┘                                            │
│                          │                                                   │
│                          ▼                                                   │
│                   ┌────────────┐                                            │
│                   │ updateRlistB│                                            │
│                   └──────┬─────┘                                            │
│                          │                                                   │
│                          ▼                                                   │
│                    ┌──────────┐                                             │
│                    │  rListB  │ ←── evaluation arrays rebuilt               │
│                    └──────────┘                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Part 3: The Core Mechanism (Code-Annotated)

### computeIndexZero — The Constraint Enforcer

**Location:** `LoopLists/LoopListener.java` lines 500-530

```java
private void computeIndexZero(int n, BigDecimal rModulo) {
    // n = current wCount (difference level)
    // rModulo = n! from moduloList
    
    double currentCoefficient = 0;
    double[][] A;
    int denumerator;
    int result = 0;
    int currentFigPArrayIndexValue;
    int newFigPArrayIndexValue;
    int intModulo;
    
    // Get Gauss-reduced coefficient matrix for level n
    A = ((LoopList)rListB.get(n)).getgMatrix();
    
    int i = A.length - 1;
    int j = A[0].length - 1;
    
    // Evaluate polynomial at x = pArray[n] (the target root position)
    for (int k = 0; k < i; k++) {
        denumerator = (i-1) - k;
        currentCoefficient = A[k+1][j];
        
        // evalCoeff computes: pArray[n]^denumerator × coefficient
        result = result + evalCoeff(pArray.getpValue(n), 
                                    (int)Math.rint(currentCoefficient), 
                                    denumerator);
    }
    
    // Normalize by factorial
    intModulo = rModulo.intValue();
    currentFigPArrayIndexValue = figPArray.getpValue(n);
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
4. **Subtract from current coefficient** — adjusts figPArray[n] so that re-evaluation at the target x will yield zero

### evalCoeff — Power Evaluation

**Location:** `LoopLists/LoopListener.java` lines 266-273

```java
private int evalCoeff(int index, int scalar, int denumerator) {
    // Computes: index^denumerator × scalar
    int result = 1;
    for (int k = 0; k < denumerator; k++) {
        result = result * index;    // index^denumerator
    }
    result = result * scalar;       // × coefficient
    return result;
}
```

**Example:** `evalCoeff(17, 5, 2)` = 17² × 5 = 289 × 5 = 1445

### updateRlistB — The Rebuild Cycle

**Location:** `LoopLists/LoopListener.java` lines 398-433

```java
private synchronized void updateRlistB() {
    this.rListB.clear();
    
    // Level 0: Evaluate P(k) using updated figPArray
    rListB.add(new LoopList(dimension+1, rListB, dimension, integerRange, 
                            0, xPowers, figPArray, moduloList));
    
    // Run Gauss elimination on level 0
    gMain.gauss(muList, muSem, dimension+1, dimension+2,
                (double[][])((LoopList)rListB.get(0)).getgMatrix(),
                new GaussBean1(figPArray, pArray.toString(), 
                              (LoopList)rListB.get(0), resultListCounter));
    
    // Levels 1..n: Compute differences
    for (int numerator = 1; numerator <= dimension; numerator++) {
        rListB.add(new LoopList(dimension-(numerator-1), rListB, dimension,
                                integerRange, numerator, xPowers, 
                                figPArray, moduloList));
        
        // Run Gauss elimination on each level
        gMain.gauss(muList, muSem, dimension-(numerator-1), 
                    dimension-(numerator-1)+1,
                    (double[][])((LoopList)rListB.get(numerator)).getgMatrix(),
                    new GaussBean1(figPArray, pArray.toString(),
                                  (LoopList)rListB.get(numerator), 
                                  resultListCounter));
    }
}
```

**What this does:**

1. **Clear rListB** — start fresh
2. **Create LoopList[0]** — evaluates P(k) for k in integer range using current figPArray
3. **Interpolate level 0** — `NewtonInterpolator` (equivalently unique interpolant from samples; legacy docs say Vandermonde/Gauss)
4. **Loop through levels 1..n** — compute each difference level
5. **Interpolate each level** — `NewtonInterpolator.interpolate` for `vmResult`

### propertyChange — The Main Loop

**Location:** `LoopLists/LoopListener.java` lines 537-562

```java
public void propertyChange(PropertyChangeEvent evt) {
    rModulo = (BigDecimal) moduloList.get(((muNumDen)evt.getNewValue()).getWorkNum());
    int wNum = ((muNumDen) evt.getNewValue()).getWorkNum();
    
    if (pArrayResetFlag.getPArrayReset()) {
        pArray.incrementP(rListIndex - 1);
    }
    
    // THE CORE LOOP: Process from highest level down to 0
    for (int wCount = (dimension-1); wCount > -1; wCount--) {
        rModulo = (BigDecimal) moduloList.get(wCount);   // Get n!
        computeIndexZero(wCount, rModulo);                // Adjust coefficient
        updateRlistB();                                   // Rebuild arrays
    }
    
    // Store results
    if (noBufferRun) {
        appendMuBuffer(noBufferRun);
    } else {
        appendMuBuffer();
    }
    
    // ... continue processing
}
```

---

## Part 4: Worked Example

### Input: pArray = [7, 15, 17, 10, 0]

**Goal:** Construct a degree-4 polynomial where:
- P(7) = 0
- Δ¹P(15) = 0
- Δ²P(17) = 0
- Δ³P(10) = 0

### Iteration Trace

```
INITIAL STATE:
  figPArray = [c₀, c₁, c₂, c₃, c₄]  (some starting coefficients)
  moduloList = [1, 1, 2, 6, 24]

─────────────────────────────────────────────────────────────────────────────

ITERATION wCount=3 (Δ³ level):
  rModulo = moduloList[3] = 6

  computeIndexZero(3, 6):
    • Get matrix A from rListB[3]
    • Evaluate at x = pArray[3] = 10
      result = Σ (coeff × 10^power)
    • Normalize: result = result / 6
    • figPArray[3] = figPArray[3] - result

  updateRlistB(): Rebuild all evaluation arrays

  RESULT: Δ³P(10) = 0 ✓

─────────────────────────────────────────────────────────────────────────────

ITERATION wCount=2 (Δ² level):
  rModulo = moduloList[2] = 2

  computeIndexZero(2, 2):
    • Evaluate at x = pArray[2] = 17
    • Normalize by 2
    • Adjust figPArray[2]

  updateRlistB(): Rebuild

  RESULT: Δ²P(17) = 0 ✓, Δ³P(10) = 0 ✓ (preserved)

─────────────────────────────────────────────────────────────────────────────

ITERATION wCount=1 (Δ¹ level):
  rModulo = moduloList[1] = 1

  computeIndexZero(1, 1):
    • Evaluate at x = pArray[1] = 15
    • Adjust figPArray[1]

  updateRlistB(): Rebuild

  RESULT: Δ¹P(15) = 0 ✓, Δ²P(17) = 0 ✓, Δ³P(10) = 0 ✓

─────────────────────────────────────────────────────────────────────────────

ITERATION wCount=0 (Ψ₀ level):
  rModulo = moduloList[0] = 1

  computeIndexZero(0, 1):
    • Evaluate at x = pArray[0] = 7
    • Adjust figPArray[0] (constant term)

  updateRlistB(): Final rebuild

  RESULT: P(7) = 0 ✓, Δ¹P(15) = 0 ✓, Δ²P(17) = 0 ✓, Δ³P(10) = 0 ✓

─────────────────────────────────────────────────────────────────────────────

FINAL STATE:
  figPArray = [c₀', c₁', c₂', c₃', c₄]  (adjusted coefficients)
  
  The polynomial P(x) = c₀' + c₁'x + c₂'x² + c₃'x³ + c₄x⁴
  satisfies ALL specified root constraints.
```

---

## Part 5: Connection to Graph Storage

### From Construction to Storage

After the iterative refinement completes:

1. **vmResult** — Monomial coefficients from **`NewtonInterpolator`** (primary) give the polynomial at each level; legacy Gauss/Vandermonde gives the same interpolant. Stored on `Dnode` nodes.

2. **muList** — During each LoopList construction, zeros are *discovered* by evaluating the polynomial. These include:
   - The explicitly specified pArray roots
   - **Latent roots** — additional zeros not in pArray (see [graph_comprehension.md](graph_comprehension.md#latent-variables-and-the-parray-signature))

3. **pArray on CreatedBy** — The zero-position signature is stored as a path identifier, linking to the construction trace.

### Discovery vs. Specification

The algorithm has a dual nature:

| Phase | Mode | Description |
|-------|------|-------------|
| Coefficient adjustment | **Specification** | Uses pArray positions to force zeros |
| LoopList evaluation | **Discovery** | Finds ALL zeros, including latent ones |

This is why `muList` may contain more roots than `pArray` specifies — the construction forces the specified roots, but may create additional zeros as a consequence.

---

## Part 6: The Diophantine Connection

The factorial normalization in `computeIndexZero` is not arbitrary — it arises from the **Diophantine structure** that governs consecutive TriagTriag levels.

### The Fundamental Relationship

From [theory.md](theory.md), consecutive TriagTriag levels satisfy linear Diophantine equations:

```
ax! + bx! = c(x+1)!
```

Simplifying:
```
(a + b)x! = c(x+1)·x!
(a + b) = c(x+1)
```

This guarantees that values at level n are always **multiples of n!**.

### Why Division by n! Always Produces Integers

When `computeIndexZero` performs:

```java
result = result / intModulo;  // intModulo = n!
```

This division is **guaranteed to produce an integer** because:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  THE ALGEBRAIC GUARANTEE                                                    │
│                                                                             │
│  1. Polynomial at level n has coefficients that are factorial-scaled        │
│     (the difference operation Δⁿ produces n! × leading coefficient)         │
│                                                                             │
│  2. The Diophantine structure:                                              │
│        ax! + bx! = c(x+1)!                                                  │
│     ensures all sums at level n remain multiples of n!                      │
│                                                                             │
│  3. When we evaluate at x = pArray[n] and sum:                              │
│        result = Σ (coefficient × pArray[n]^power)                           │
│                                                                             │
│  4. This result is a multiple of n! because each coefficient at level n     │
│     carries the n! factor inherited from the difference operation           │
│                                                                             │
│  5. Therefore: result / n! ∈ ℤ (always an integer)                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### The Factorial Divisibility Condition

From [dichotomy.md](dichotomy.md):

> **Condition at level k:** The leading coefficient of Δᵏ P must be divisible by k!.
>
> Since Δᵏ(xᵏ/k!) = 1 (the "divided difference basis"), integer polynomial reconstruction requires:
> ```
> Coefficient of xᵏ in Δᵏ P = k! × (some integer)
> ```

This is exactly what `moduloList` enforces — it ensures the algorithm stays in ℤ[x].

### Concrete Example

For a degree-4 polynomial at level 2 (quadratic difference):

```
Level 2 polynomial: Δ²P(x) = 2! × (ax² + bx + c)  where a,b,c ∈ ℤ

Evaluate at x = 17:
  result = 2! × (a(17)² + b(17) + c)
         = 2 × (289a + 17b + c)
         = 2 × (integer)

Divide by 2!:
  adjustment = result / 2 = (289a + 17b + c) ∈ ℤ  ✓
```

The Diophantine structure guarantees this works at every level.

### The Bidirectional Relationship

| Direction | Operation | Factorial Role | Guaranteed by |
|-----------|-----------|----------------|---------------|
| **Down** (Δ) | Difference | Multiply by n! | Definition of Δⁿ |
| **Up** (anti-Δ) | Reconstruction | Divide by n! | Diophantine solvability |

The algorithm's `computeIndexZero` uses the "up" direction — dividing by `n!` to adjust coefficients. The Diophantine equation guarantees this division always produces integers.

### Why This Matters

The Diophantine connection ensures:

1. **Integer adjustments** — `figPArray[n]` is always modified by an integer value
2. **Algebraic consistency** — the polynomial chain remains in ℤ[x] throughout construction
3. **Guaranteed solvability** — we can always find a coefficient adjustment to create a zero at any specified integer position

**Profound insight:** The same factorial structure that makes the Diophantine equations solvable also makes the root-constraint algorithm work entirely with integer arithmetic.

---

## Part 7: Latent Roots as Side Effects

A critical insight: when `computeIndexZero` adjusts a coefficient to force a root at one position, it changes the **entire polynomial**. This may accidentally create additional integer roots that weren't specified.

### The Mechanism

Consider a quadratic at difference level 2:

```
BEFORE adjustment:
  Q(x) = ax² + bx + c
  Q(17) = a(17)² + b(17) + c = 580  (not zero)
  Q(4)  = a(4)² + b(4) + c   = 36   (not zero)

computeIndexZero adjusts c:
  adjustment = 580 / 2! = 290
  c_new = c - 290

AFTER adjustment:
  Q'(x) = ax² + bx + c_new
  Q'(17) = a(17)² + b(17) + c_new = 0  ✓  (INTENDED - specified in pArray)
  Q'(4)  = a(4)² + b(4) + c_new   = ?   (UNKNOWN - may or may not be zero)
```

### The Key Insight

When you change `c` to force `Q'(17) = 0`, you're changing `Q'(x)` for ALL values of x. Whether this adjustment **accidentally** creates `Q'(4) = 0` depends on the specific values of `a`, `b`, and the original `c`.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    THE LATENT ROOT EMERGENCE                                 │
│                                                                             │
│  pArray specifies: "Force a root at x=17"                                   │
│                                                                             │
│  computeIndexZero: Adjusts constant term c → c_new                          │
│                                                                             │
│  Intended effect: Q'(17) = 0  ✓                                             │
│                                                                             │
│  Side effect:     Q'(4) may also = 0  (LATENT ROOT)                         │
│                   Q'(other) may also = 0  (MORE LATENT ROOTS)               │
│                                                                             │
│  The algorithm does NOT control this.                                       │
│  The algorithm DISCOVERS these roots during updateRlistB().                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Latent Roots Are Unpredictable

The adjustment `c_new = c - adjustment` is computed to satisfy ONE constraint: `Q'(17) = 0`. But a degree-2 polynomial can have up to 2 roots. Whether the second root happens to be:

1. **An integer within range** → latent root discovered, added to muList
2. **An integer outside range** → root exists but not detected
3. **A non-integer** → no additional root in muList
4. **Complex** → no real root at all

...depends entirely on the coefficients `a` and `b`, which are inherited from the higher difference levels.

### Concrete Example from the Graph

For `pArray = [7, 15, 17, 10, 0]` at wNum=2 (quadratic level):

```
pArray[2] = 17  → algorithm forces Q'(17) = 0

After construction:
  vmResult = [0.0, 12.0, -252.0, 816.0]  (coefficients for Δ²P)
  muList = [4, 16]  (adjusted)
  rootList = [4, 17]  (true positions)

The quadratic Δ²P(x) = 12x - 252x + 816 has roots at x=4 AND x=17.
  - x=17 was SPECIFIED in pArray
  - x=4 is a LATENT ROOT — emerged as a side effect of the adjustment
```

### Connection to Determinedness

This explains why determinedness is **probabilistic**:

| Scenario | totalZero | determined |
|----------|-----------|------------|
| Adjustment creates enough latent roots | = degree | 1 (determined) |
| Adjustment creates some latent roots | < degree | 0 (undetermined) |
| No latent roots (complex/irrational) | < degree | 0 (undetermined) |

**The algorithm cannot predict** whether forcing one root will accidentally create enough additional roots to make the polynomial determined.

### Implications for ML Classification

This is the source of the ML classification challenge:

1. **pArray doesn't determine determinedness** — it only specifies ONE root per level
2. **Latent roots are emergent** — they result from the coefficient adjustment
3. **Prediction requires modeling** — ML must learn patterns in coefficients that predict latent root emergence
4. **Degree matters** — higher degrees need more "lucky" latent roots to be determined

See [why_ml.md](../GraphMLDifferences/why_ml.md) for how this affects ML training.

---

## Related Documentation

- [graph_comprehension.md](graph_comprehension.md) — Understanding graph query results and the pArray/muList relationship
- [theory.md](theory.md) — Mathematical foundations (Newton's differences, Diophantine equations)
- [dichotomy.md](dichotomy.md) — Factorial divisibility conditions and determinedness
- [math_verification.md](../GraphMLDifferences/math_verification.md) — Verification of difference table computations
- [ModuloList.java](../ZerosAndDifferences033021/src/LoopsLogic/ModuloList.java) — Factorial array implementation
- [LoopListener.java](../ZerosAndDifferences033021/src/LoopLists/LoopListener.java) — Core constraint mechanism


