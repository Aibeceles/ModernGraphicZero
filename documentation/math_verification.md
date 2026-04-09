# Mathematical Verification: Difference Table Analysis

## Correction Confirmed

**Yes, the correct modification is x⁴ - 6x³, not x⁴ - 6x.**

To zero out Δ³P(0), you need a cubic term because:
- Δ³(linear) = 0
- Δ³(quadratic) = 0  
- Δ³(cubic) = 3! = 6 (constant)

So: Δ³(x⁴ - 6x³)(0) = 36 - 6×6 = 0 ✓

---

## The Algorithm in Java Code

### Overview

The code implements **Newton's Forward Differences** in two phases:

```
Phase 1: Evaluate P(x) over integer range     → LoopList at wNum=0 (Ψ₀)
Phase 2: Compute successive differences Δⁿ    → LoopList at wNum=n (Ψₙ)
```

### Phase 1: `loadPArray()` — Polynomial Evaluation

**Location:** `LoopList.java` lines 168-213

```java
// For each x in [-halfIntegerRange, +halfIntegerRange):
for(int x = -halfIntegerRange; x < halfIntegerRange; x++) {
    listValueResult = zero;
    BigDecimal currentK = new BigDecimal(x);  // k as in Ψᵢ, triagTriag_j, k
    
    // Evaluate P(k) = Σ coefficient[y] × k^y
    for (int y = 0; y < (dimension+1); y++) {
        BigDecimal currentPcoeff = new BigDecimal((int)pArray.get(y));
        listValue = currentK.pow(y);
        listValueResult = listValueResult.add(listValue.multiply(currentPcoeff));
    }
    
    this.add(listValueResult);  // Store P(k)
    
    // Track zeros
    if (listValueResult.compareTo(zero) == 0 && (x > 0)) {
        muList.add(x - caiIndex);  // Record zero position
        caiIndex++;
    }
    if (listValueResult.compareTo(zero) == 0) {
        totalZero++;  // Count zeros
    }
}
```

**What it does:**
1. Takes polynomial coefficients from `pArray` = [a₀, a₁, a₂, ..., aₙ]
2. Evaluates P(k) = a₀ + a₁k + a₂k² + ... + aₙkⁿ for each k in range
3. Stores results as a list (the Ψ₀ row)
4. Records positions where P(k) = 0 in `muList`
5. Counts total zeros in `totalZero`

> **Discovery-Based Detection:** The algorithm **discovers** zeros by checking `listValueResult.compareTo(zero) == 0` — it evaluates the polynomial and tests for equality with zero. This is discovery, not construction from constraints.

> **caiIndex Adjustment:** Note that `muList.add(x - caiIndex)` stores an adjusted position, not the raw x value. The `caiIndex` increments after each zero, so consecutive roots have progressively larger adjustments. Use `rootList` for true mathematical positions.

### Phase 2: `subsequentWorkerListInit()` — Forward Differences

**Location:** `LoopList.java` lines 325-409

```java
// The triagTriag subtraction
for (int y = 0; y < integerRange; y++) {
    // Get consecutive values from previous level
    arg1 = (BigDecimal)((LoopList)rListB.get(x)).get(y);      // Ψₙ₋₁(k)
    
    if (y + 1 < integerRange) { 
        arg2 = (BigDecimal)((LoopList)rListB.get(x)).get(y+1); // Ψₙ₋₁(k+1)
    } else {
        arg2 = zero;
    }
    
    // THE KEY OPERATION: Forward Difference
    listResult = arg2.subtract(arg1);  // Δf(k) = f(k+1) - f(k)
    
    this.add(listResult);  // Store ΔⁿP(k)
    
    // Track zeros at this difference level
    if (listResult.compareTo(zero) == 0 && (y > halfIntegerRange)) {
        muList.add(y - caiIndex - halfIntegerRange);
        caiIndex++;
    }
    if (listResult.compareTo(zero) == 0) {
        totalZero++;
    }
}
```

**What it does:**
1. Takes the previous level's LoopList (Ψₙ₋₁)
2. Computes forward difference: Δf(k) = f(k+1) - f(k)
3. Stores results as new LoopList (Ψₙ)
4. Tracks zeros in the new level

> **caiIndex at Higher Levels:** The formula `muList.add(y - caiIndex - halfIntegerRange)` includes both the caiIndex adjustment and a range offset. This means `muList` values may differ from true x positions. The `rootList` property stores true mathematical root positions.

> **For constraint construction details:** See [algorithm_construction.md](../documentation/algorithm_construction.md) for how the algorithm iteratively adjusts coefficients using `computeIndexZero` and factorial normalization to force zeros at pArray-specified positions.

### The Complete Algorithm Flow

> **Important Terminology Note:** In the Java code, `pArray` holds polynomial coefficients for evaluation. However, when stored as a `CreatedBy` node property in Neo4j, `pArray` represents a **zero-position signature** `[r₀, r₁, r₂, ...]` where `rᵢ` is the x-position where difference level `wNum=i` equals zero. See [graph_comprehension.md](../documentation/graph_comprehension.md) for details.

```
Input: pArray = [a₀, a₁, a₂, ..., aₙ]  (polynomial coefficients in Java code)
       dimension = n                    (polynomial degree)
       integerRange = 200               (evaluation range)

┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Create LoopList for wNum=0 (Ψ₀)                        │
│                                                                 │
│   loadPArray(pArray)                                           │
│   For k = -100 to +99:                                         │
│       P(k) = a₀ + a₁k + a₂k² + ... + aₙkⁿ                     │
│       Store P(k)                                                │
│       If P(k) == 0: record in muList, increment totalZero      │
│                                                                 │
│   Result: LoopList₀ = [P(-100), P(-99), ..., P(99)]           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Create LoopList for wNum=1 (Ψ₁ = Δ¹P)                 │
│                                                                 │
│   subsequentWorkerListInit()                                   │
│   For k = 0 to integerRange-1:                                 │
│       Δ¹P(k) = P(k+1) - P(k)                                   │
│       Store Δ¹P(k)                                             │
│       Track zeros                                               │
│                                                                 │
│   Result: LoopList₁ = [Δ¹P(k) for k in range]                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Create LoopList for wNum=2 (Ψ₂ = Δ²P)                 │
│                                                                 │
│   subsequentWorkerListInit()                                   │
│   For k = 0 to integerRange-1:                                 │
│       Δ²P(k) = Δ¹P(k+1) - Δ¹P(k)                              │
│       Store Δ²P(k)                                             │
│       Track zeros                                               │
│                                                                 │
│   Result: LoopList₂ = [Δ²P(k) for k in range]                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                             ...
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step n+1: Create LoopList for wNum=n (Ψₙ = ΔⁿP = constant)    │
│                                                                 │
│   Result: LoopList_n = [n! × aₙ, n! × aₙ, ...]  (constant)    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step Final: Interpolation & Database Write                      │
│                                                                 │
│   NewtonInterpolator — monomial vmResult (primary)              │
│   GaussTable1 — write to Neo4j:                                │
│       :Dnode with vmResult, n, d, totalZero, muList, determined│
│       :zMap connecting difference levels                        │
└─────────────────────────────────────────────────────────────────┘
```

### Key Variables Mapping

| Math Concept | Java Variable | Description |
|--------------|---------------|-------------|
| P(x) coefficients (Java) | `pArray` | [a₀, a₁, ..., aₙ] — used for polynomial evaluation |
| Zero-position signature (Neo4j) | `pArray` on CreatedBy | [r₀, r₁, ...] — x-positions where each wNum level = 0 |
| Polynomial coefficients (Neo4j) | `vmResult` on Dnode | [aₙ, ..., a₀] descending — Newton interpolation (primary) |
| Degree n | `dimension` | Polynomial degree |
| Evaluation range | `integerRange` | Usually 200 → k ∈ [-100, +99] |
| Ψᵢ (level i values) | `LoopList` at `wNum=i` | Stored as ArrayList |
| Δⁿf(k) | `listResult = arg2.subtract(arg1)` | Forward difference |
| Zero positions | `muList` | Indices where value = 0 |
| Zero count | `totalZero` | Count of zeros |
| Difference level | `wNum` | 0=original, 1=Δ¹, 2=Δ², ... |

---

## Setup: P(x) = x⁴

Computing the difference table:

```
k:      0    1     2     3      4      5
P(k):   0    1    16    81    256    625

Δ¹P:       1    15    65    175    369
Δ²P:          14    50    110    194
Δ³P:             36    60     84
Δ⁴P:                24    24         ← constant = 4! = 24 ✓
```

**Values at k=0:**
- Δ⁰P(0) = 0
- Δ¹P(0) = 1
- Δ²P(0) = 14
- Δ³P(0) = 36
- Δ⁴P(0) = 24

---

## User's Claim #1: Linear Polynomial (Δ³P)

> "The linear polynomial in sequence at zero would evaluate to 36"

**Verified:** Δ³P(0) = 36 ✓

For degree-4 polynomial:
- Δ³P is degree 4-3 = 1 (linear) ✓

> "The factorial sequence is [1!, 2!, 3!, 4!] = [1, 2, 6, 24]"
> "For the linear term, the relevant factorial is 6"
> "6 × 6 = 36"

**The relationship:** 36 = 6 × 6... but why?

Let me check the pattern:
- Δ⁴P(0) = 24 = 4! × 1 (leading coeff)
- Δ³P(0) = 36 = 3! × ? = 6 × 6

The coefficient 6 in "6 × 6" is actually related to C(4,3) = 4 or something else...

Actually: 36 = 3! × 6, where 6 = C(4,1) + something? Let me investigate...

---

## User's Claim #2: Modifying P

> "If change P to x⁴ - 6x then the linear polynomial (Δ³P) at zero would evaluate to 0"

### ⚠️ HOLE #1: Linear terms don't affect Δ³

**Key principle:** The nth difference of a polynomial of degree < n is 0.

- Δ¹(ax) = a (constant)
- Δ²(ax) = 0
- Δ³(ax) = 0

Therefore:
```
Δ³(x⁴ - 6x) = Δ³(x⁴) - Δ³(6x) = Δ³(x⁴) - 0 = Δ³(x⁴)
```

**Verification for P(x) = x⁴ - 6x:**

```
k:      0    1     2     3      4
P(k):   0   -5     4    63    232

Δ¹P:      -5     9    59    169
Δ²P:          14    50    110
Δ³P:             36    60        ← UNCHANGED!
Δ⁴P:                24           ← UNCHANGED!
```

**Δ³(x⁴ - 6x)(0) = 36 ≠ 0**

---

## User's Claim #3: Quadratic Polynomial (Δ²P)

> "The quadratic polynomial in difference sequence evaluates to 14"

**Verified:** Δ²P(0) = 14 ✓

> "Change P to x⁴ - 7x... the quadratic polynomial would have a root at x=0"

### ⚠️ HOLE #2: Same issue — linear terms don't affect Δ²

```
Δ²(x⁴ - 7x) = Δ²(x⁴) = 14
```

**The modification doesn't achieve the stated goal.**

---

## What WOULD Work?

To make Δ³P(0) = 0, you need to subtract a **cubic** term:

Since Δ³(x³) = 3! = 6 (constant), we need:
```
Δ³(x⁴ - cx³)(0) = 36 - 6c = 0
c = 6
```

**Correct modification: P(x) = x⁴ - 6x³**

```
P(x) = x⁴ - 6x³

k:      0    1     2     3      4
P(k):   0   -5   -32   -81    -128

Δ¹P:      -5   -27   -49    -47
Δ²P:         -22   -22      2
Δ³P:             0    24        ← NOW ZERO AT k=0! ✓
Δ⁴P:               24
```

---

## To Make Δ²P(0) = 0

Since Δ²(x²) = 2! = 2 (constant), and Δ²(x⁴)(0) = 14:

```
Δ²(x⁴ - cx²)(0) = 14 - 2c = 0
c = 7
```

**Correct modification: P(x) = x⁴ - 7x²**

```
P(x) = x⁴ - 7x²

k:      0    1     2     3      4
P(k):   0   -6     2    54    200

Δ¹P:      -6     8    52    146
Δ²P:          14    44     94
```

Wait, that gives Δ²P(0) = 14, not 0...

Let me recalculate. For Δ²(x²):
- x²: 0, 1, 4, 9, 16
- Δ¹: 1, 3, 5, 7
- Δ²: 2, 2, 2 (constant = 2!)

So Δ²(7x²) = 7 × 2 = 14.

Δ²(x⁴ - 7x²)(0) = 14 - 14 = 0 ✓

Let me verify:
```
P(x) = x⁴ - 7x²

k:      0    1     2     3      4
P(k):   0   -6     2    54    200

Δ¹P:      -6     8    52    146
Δ²P:          14    44     94
```

Hmm, Δ²P(0) = 14 ≠ 0. Let me recompute...

Actually the issue is:
- Δ¹P(0) = P(1) - P(0) = -6 - 0 = -6
- Δ¹P(1) = P(2) - P(1) = 2 - (-6) = 8
- Δ²P(0) = Δ¹P(1) - Δ¹P(0) = 8 - (-6) = 14

So Δ²(x⁴ - 7x²)(0) = 14, not 0.

Let me reconsider. The principle is:
- Δⁿ(xⁿ)(k) = n! for all k
- Δⁿ(xᵐ)(k) depends on both n and m

For Δ²(x²): 
The constant 2nd difference of x² is 2.

For Δ²(x⁴):
This is NOT constant. It varies with k.

Δ²(x⁴)(0) = 14
Δ²(x⁴)(1) = 50
...

So Δ²(x⁴ - cx²)(0) = Δ²(x⁴)(0) - c·Δ²(x²)(0) = 14 - 2c

Setting to 0: c = 7.

But wait, Δ²(x²) IS constant = 2, so Δ²(x²)(0) = 2.

So x⁴ - 7x² should give:
Δ²(x⁴ - 7x²)(0) = 14 - 7×2 = 14 - 14 = 0

Let me recompute more carefully:

x²: 0, 1, 4, 9, 16, 25
Δ¹(x²): 1, 3, 5, 7, 9
Δ²(x²): 2, 2, 2, 2

7x²: 0, 7, 28, 63, 112, 175
Δ¹(7x²): 7, 21, 35, 49, 63
Δ²(7x²): 14, 14, 14, 14

x⁴: 0, 1, 16, 81, 256, 625
Δ¹(x⁴): 1, 15, 65, 175, 369
Δ²(x⁴): 14, 50, 110, 194

x⁴ - 7x²: 0, -6, -12, 18, 144, 450
Δ¹(x⁴ - 7x²): -6, -6, 30, 126, 306
Δ²(x⁴ - 7x²): 0, 36, 96, 180

**Δ²(x⁴ - 7x²)(0) = 0** ✓

I made an arithmetic error before. The correct calculation shows it works!

---

## Summary of Holes

### Hole #1: Degree Mismatch

The user's modification uses **linear** terms (-6x, -7x) but:
- To affect Δ³P, you need a **cubic** term (degree 3)
- To affect Δ²P, you need a **quadratic** term (degree 2)

**Principle:** Δⁿ(polynomial of degree < n) = 0

### Hole #2: The Factorial Relationship

The user's intuition about the factorial relationship is **correct**:
- Δ³(x³) = 3! = 6
- Δ²(x²) = 2! = 2
- Δ⁴(x⁴) = 4! = 24

But the application was to the wrong degree term.

### Corrected Examples

| To make this zero | Add this term | Coefficient calculation |
|-------------------|---------------|------------------------|
| Δ³(x⁴)(0) = 36 | -6x³ | 36 / 3! = 36/6 = 6 |
| Δ²(x⁴)(0) = 14 | -7x² | 14 / 2! = 14/2 = 7 |
| Δ¹(x⁴)(0) = 1 | -1x¹ | 1 / 1! = 1/1 = 1 |

---

## The "Determined" Connection

This relates to when a polynomial is **determined**:

- If P(x) = x⁴, it has one root at x=0
- Δ¹P(0) = 1 ≠ 0, so x=0 is NOT a root of Δ¹P
- Δ²P(0) = 14 ≠ 0, so x=0 is NOT a root of Δ²P
- etc.

**For consecutive integer roots:**

If P(x) = x(x-1)(x-2)(x-3) = x⁴ - 6x³ + 11x² - 6x:

```
k:      0    1    2    3     4
P(k):   0    0    0    0    24

Δ¹P:       0    0    0    24
Δ²P:          0    0    24
Δ³P:            0    24
Δ⁴P:              24
```

**All the zeros in column k=0!** This is because:
- P(0) = 0 (root at 0)
- P(1) = 0 (root at 1) → Δ¹P(0) = P(1) - P(0) = 0
- P(2) = 0 (root at 2) → Δ²P(0) = 0
- P(3) = 0 (root at 3) → Δ³P(0) = 0

**This is the deep connection between roots and the difference table!**

