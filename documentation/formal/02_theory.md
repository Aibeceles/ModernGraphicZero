# Mathematical Theory: Polynomial Differences and Set-Theoretic Bijections

## Abstract

This document presents the mathematical foundations underlying the project:

1. **Finite Calculus** — Newton's forward differences and polynomial interpolation
2. **Set Theory** — Bijections between integers and rationals via set-measure mappings
3. **Number Theory** — Linear Diophantine equations and factorial relationships
4. **Combinatorics** — Binary encodings, power sets, and binomial coefficients

The central thesis is that polynomial difference tables generate graph structures whose properties encode both integer sequences (via binary root patterns) and rational numbers (via set union ratios), illuminating the classical ℤ ↔ ℚ bijection through a computational lens.

---

## Part I: Newton's Forward Differences

### 1.1 The Forward Difference Operator

**Definition.** The forward difference operator Δ on a function f: ℤ → ℝ is defined as:

```
Δf(k) = f(k+1) - f(k)
```

**Iterated differences:**

```
Δ⁰f(k) = f(k)
Δ¹f(k) = f(k+1) - f(k)
Δ²f(k) = Δ(Δf)(k) = f(k+2) - 2f(k+1) + f(k)
Δⁿf(k) = Σⱼ₌₀ⁿ (-1)ⁿ⁻ʲ C(n,j) f(k+j)
```

where C(n,j) = n!/(j!(n-j)!) is the binomial coefficient.

### 1.2 Fundamental Properties

**Theorem 1.1 (Degree Reduction).** If P(x) is a polynomial of degree n, then ΔP(x) is a polynomial of degree n-1.

**Corollary.** For P(x) of degree n:
- Δⁿ P(x) = n! × (leading coefficient) — a constant
- Δⁿ⁺¹ P(x) = 0

**Theorem 1.2 (Newton's Forward Difference Formula).** Any polynomial P(x) of degree n can be expressed as:

```
P(x) = Σₖ₌₀ⁿ C(x,k) · Δᵏf(0)
```

where C(x,k) = x(x-1)(x-2)...(x-k+1)/k! is the generalized binomial coefficient.

### 1.3 The TriagTriag Structure

The **Triangle of Triangles** (TriagTriag) is the tabular representation of iterated differences:

```
For P(x) = x³:

k:     0    1    2    3    4    5    6
────────────────────────────────────────────
Ψ₀:    0    1    8   27   64  125  216    ← P(k)
Ψ₁:       1    7   19   37   61   91     ← Δ¹P(k)
Ψ₂:          6   12   18   24   30       ← Δ²P(k)
Ψ₃:             6    6    6    6         ← Δ³P(k) = 3! = 6
```

**Key observations:**
- Row Ψᵢ represents the i-th difference sequence
- Column k represents evaluation at integer k
- The final row is constant = n! × leading coefficient
- Zeros propagate through the structure in predictable patterns

### 1.4 Connection to Calculus

Newton's differences are the **discrete analog of derivatives**:

| Continuous Calculus | Discrete Calculus (Finite Differences) |
|---------------------|----------------------------------------|
| d/dx | Δ |
| dⁿ/dxⁿ (xⁿ) = n! | Δⁿ(xⁿ) = n! |
| Taylor series | Newton's forward formula |
| ∫ (integral) | Σ (summation) |

---

## Part II: The Diophantine Connection

### 2.1 Linear Diophantine Equations in TriagTriag

The relationship between consecutive TriagTriag levels satisfies:

```
ax! + bx! = c(x+1)!
```

where:
- x and y relate to factorial scalars at level nMax - i
- The coefficients arise from the binomial expansion applied to difference rows
- Crucially, x = -y in the canonical form

### 2.2 Solvability Conditions

**Classical result:** The linear Diophantine equation ax + by = c has integer solutions if and only if gcd(a, b) | c.

**In the TriagTriag context:** Since both coefficients are multiples of x!, and c is a multiple of (x+1)! = (x+1) · x!, the equation is always solvable. This guarantees that consecutive TriagTriag levels are algebraically related through well-defined integer arithmetic.

### 2.3 Factorial Scalar Relationships

Each TriagTriag level Ψⱼ has an associated **factorial scalar**:

```
TriagTriag_1: modulo 1! = 1
TriagTriag_2: modulo 2! = 2
TriagTriag_3: modulo 3! = 6
TriagTriag_4: modulo 4! = 24
TriagTriag_5: modulo 5! = 120  (constant difference for degree-5)
```

**Key insight:** The factorial scalars form their own difference sequences — the **Scalar Difference Polynomials (SDP)**. Each factorial scalar expansion has a representative polynomial P* that encodes the scalar structure.

---

## Part III: Set-Theoretic Bijection Framework

### 3.1 Motivation

**Classical result (Cantor):** The set of rational numbers ℚ is countable — there exists a bijection f: ℕ → ℚ.

Standard proofs use diagonal enumeration, pairing functions, or Stern-Brocot tree traversal.

**This project's approach:** Establish a bijection through dual mappings on **collections of finite sets**, where:
- μ (Set Union Ratio) → Rational numbers ℚ
- χ (Binary Encoding) → Integers ℤ

### 3.2 Definition: Set Collections

Let Å = {A₀, A₁, ..., Aₘ₋₁} be a finite collection of finite sets, where each Aᵢ ⊂ ℕ.

### 3.3 Definition 2.1: Set Union Ratio (μ)

The **Set Union Ratio** μ: 𝒫(𝒫(ℕ)) → [0,1] is defined as:

```
μ(Å) = |∪_{A∈Å} A| / Σ_{A∈Å} |A|
```

**Interpretation:**
- Numerator: Cardinality of the union of all sets
- Denominator: Sum of individual set cardinalities
- Result: A rational number in [0,1]

**Example:**
```
Å = {{1}, {1,2}, {1,2,3}}

|∪ A| = |{1,2,3}| = 3
Σ|A| = 1 + 2 + 3 = 6

μ(Å) = 3/6 = 1/2
```

### 3.4 Definition 2.2: Binary Encoding Rule (χ)

The **Binary Encoding Rule** χᵢ: 𝒫(𝒫(ℕ)) → {0,1} is defined for each position i ∈ ℕ as:

```
χᵢ(Å) = 1  if i = n + |Aₙ| - 1 for some Aₙ ∈ Å
χᵢ(Å) = 0  otherwise
```

**Alternative formulation:** Positions where χᵢ = 1 are the cumulative sums of set cardinalities minus 1:

```
{|A₀|-1, |A₀|+|A₁|-1, |A₀|+|A₁|+|A₂|-1, ...}
```

**Example:**
```
Å = {{1}, {1,2}, {1,2,3}}

A₀: n=0, |A₀|=1 → i = 0 + 1 - 1 = 0 → χ₀ = 1
A₁: n=1, |A₁|=2 → i = 1 + 2 - 1 = 2 → χ₂ = 1
A₂: n=2, |A₂|=3 → i = 2 + 3 - 1 = 4 → χ₄ = 1

Binary string: 10101 → Integer 21
```

### 3.5 Definition 2.3: Binary Decoder (D)

The **Binary Decoder** recovers a set collection from a binary sequence:

Given positions I = {i₀, i₁, i₂, ...} where χᵢ = 1:

```
|A₀| = i₀ + 1
|A₁| = i₁ - i₀
|A₂| = i₂ - i₁
...
```

**Proposition 2.4:** The binary encoding uniquely determines the cardinalities |A₀|, |A₁|, ... of the generating collection.

### 3.6 The Bijection Chain

```
Set Collection Å
      │
      ├──── μ ────→  Rational q ∈ ℚ ∩ [0,1]
      │
      └──── χ ────→  Binary string → Integer z ∈ ℤ
```

**The bijection hypothesis:** Through careful constraint definition (index-consistent sets, canonical representatives), the dual mapping establishes ℤ ↔ ℚ.

### 3.7 Index-Consistent Sets

**Definition 3.1:** A collection {A₀, A₁, ..., Aₘ₋₁} is **index-consistent** if for any two sets Aᵢ and Aⱼ, if both have an element at index k, then they have the same element at that index.

**Result:** Index-consistent collections form **nested structures**: A₀ ⊆ A₁ ⊆ ... ⊆ Aₘ₋₁

### 3.8 Theorem 5.1: Non-uniqueness

> There exist distinct collections Å ≠ ℬ such that μ(Å) = μ(ℬ).

**Implication:** The μ mapping is not injective. Multiple set collections can map to the same rational. The bijection requires canonical representative selection and equivalence class structure.

---

## Part IV: Connecting Polynomials to Bijections

### 4.1 The Bridge: muList and Root Positions

In the polynomial difference graph:
- `muList` stores indices where the polynomial evaluates to zero
- These indices function as "set collection cardinalities"
- The dual encoding (μ for rational, χ for integer) applies

**Correspondence:**

| Graph Property | Bijection Concept |
|----------------|-------------------|
| `muList = [i₁, i₂, ...]` | Set collection cardinalities |
| `n/d` on node | μ(Å) rational value |
| Binary encoding of muList | χ integer value |
| `:NEXT_INTEGER` edge | Consecutive integers in ordering |
| `:SAME_RATIONAL` edge | Equivalence class (same μ) |

### 4.2 The Power Set Correspondence

**Key Claim:** For well-bounded generation parameters, the set of determined polynomials corresponds to the **power set** of possible root indices.

| Power Set Element | Graph Node | Root Indexing |
|-------------------|------------|---------------|
| ∅ (empty set) | Constant polynomial | `muList = []`, `totalZero = 0` |
| {0} | Linear with root at 0 | `muList = [0]`, `totalZero = 1` |
| {0, 1} | Quadratic with roots 0,1 | `muList = [0,1]`, `totalZero = 2` |
| {i₁, ..., iₖ} | Degree-k polynomial | `muList = [i₁, ..., iₖ]` |

**Size:** For integerRange = R, the power set has 2^R elements.

---

## Part V: The DOSPT Structure

### 5.1 Definition

The **Difference of Scalars Polynomial Tree (DOSPT)** is the hierarchical structure encoding factorial scalar relationships:

```
DOSPT Structure:
├── Vertical branches: TriagTriag levels (j = 1, 2, ..., degree)
├── Horizontal branches: Polynomial transformations Ψᵢ → Ψᵢ₊₁
└── Nodes: Coordinates (horizontal_n, vertical_m)
```

### 5.2 Scalar Difference Polynomials (SDP)

Each TriagTriag level's factorial scalar sequence forms its own difference pattern:

```
j=5: SDP is constant (zero difference)
j=4: SDP is constant at 5
j=3: SDP is a polynomial of degree 1
j=2: SDP is a polynomial of degree 2
j=1: SDP = P itself
```

**Key insight:** The first SDP for the 1! modulus is P itself. Subsequent SDPs decrease in degree, forming a nested structure.

---

## Part VI: Further Mathematical Context

### 6.1 Connections to Combinatorics

**Binomial Coefficients:** The binomial expansion C(n,k) appears in:
- Newton's difference formula
- Scalar Difference Polynomials (SDP) composition
- The relationship between TriagTriag levels

**Stirling Numbers:** Connect power sums and falling factorials, relating ordinary powers to Newton's basis polynomials.

**Pascal's Triangle:** The structure of iterated differences mirrors Pascal's triangle, with each difference level involving binomial weights.

### 6.2 Connections to Algebra

**Vandermonde Matrices:** Polynomial recovery from sampled values requires solving:

```
V · c = y

where V[i,j] = xᵢʲ, c = coefficients, y = sample values
```

The Vandermonde matrix is invertible when sample points are distinct, enabling Gaussian elimination to recover polynomial coefficients (conceptually the same unique interpolant as **Newton divided differences** on those samples). The Java **primary** export path implements recovery via **`NewtonInterpolator`**, not Vandermonde elimination.

**Polynomial Rings:** The polynomials form a ring ℤ[x]. The difference operator Δ is linear, and ker(Δⁿ) consists of polynomials of degree < n.

### 6.3 Connections to Number Theory

**Rational Root Theorem:** If P(x) ∈ ℤ[x] has a rational root p/q (lowest terms), then p | a₀ and q | aₙ.

**Implication for monic polynomials:** All rational roots are integers dividing the constant term.

### 6.4 Connections to Graph Theory

**Graph Structure:**
- `:Dnode` nodes form a **directed forest** via `:zMap`
- Convergence occurs as differences reduce polynomial degree
- The structure is a **DAG** (directed acyclic graph)

---

## Part VII: Open Questions

### 7.1 Bijection Completeness

Does the proposed μ/χ dual mapping establish a complete bijection ℤ ↔ ℚ?

Requirements:
- **Injectivity:** Different rationals → different binary encodings
- **Surjectivity:** Every integer corresponds to some rational
- **Canonicalization:** Unique representatives for equivalence classes

### 7.2 Graph Coverage

For what parameter bounds (dimension, integerRange, setProductRange) does the graph contain all power set elements?

### 7.3 Computational Implications

Can arithmetic on integers/rationals be performed through graph operations?

### 7.4 Generalization

Can the framework extend to algebraic numbers, p-adic numbers, or other number fields?

---

## References

### Primary Sources
- Newton, I. — *Methodus Differentialis* (forward difference calculus)
- Cantor, G. — Countability proofs for ℚ

### Project Documents
- `html-with-appendix-and-toc.html` — Full bijection theory with definitions
- `A Set-Theoretic Approach...pdf` — Formal paper

### Related Mathematical Areas
- Graham, Knuth, Patashnik — *Concrete Mathematics* (Finite Calculus)
- Mordell — *Diophantine Equations*
- Halmos — *Naive Set Theory*

---

*This document synthesizes the mathematical theory underlying the Zeros and Differences project.*

