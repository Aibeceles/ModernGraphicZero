# Configure Node — Property Reference

The `:Configure` node is a singleton Neo4j node read at startup by
`LoopsDriverTwoPManager` (via `GaussTable1.configureQuery()` /
`configureQuery1()`).  Its properties are loaded directly into
`LoopsDriverTwoPManager` fields before any computation begins, replacing
the hard-coded defaults declared in the class body.

```cypher
// Query issued by configureQuery()
MATCH (e:Configure)
RETURN e.setProductRange,
       e.setProductRAngeIncrement,
       e.maxSetProductRange,
       e.dimension,
       e.maxFigPScalar,
       e.integerRange

// Additional property returned by configureQuery1()
// ... e.pArray
```

---

## Properties

### `dimension`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.dimension` |
| **Default in code** | `5` |
| **Type** | Integer |
| **Category** | Initial condition — structural |

Degree of the polynomial under analysis.  This is the single most
structural parameter in the run.

- Sets the length of both `pArray` and `figPArray` to `dimension + 1`
  coefficients.
- Controls the number of `LoopList` worker threads created inside
  `LoopsDriverTwoP.call()`.  The loop `for (int numerator=1;
  numerator<dimension; numerator++)` spawns `dimension - 1` intermediate
  workers plus `workerZero` and `workerLast`, giving `dimension + 1`
  total semaphore workers.
- Propagated to `GaussTable1.setDimension()`, which uses it when writing
  the `determined` property to each `:Dnode`:
  `if (dimension - wNum == totalZero) { determined = 1; }`.
- Passed down through every `LoopsDriverTwoP` constructor and into every
  `LoopList` and `LoopListener`.

**Run-duration effect:** Increasing `dimension` adds one new worker
thread per unit and increases the depth of the finite-difference pyramid,
significantly extending runtime.

---

### `integerRange`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.integerRange` |
| **Default in code** | `20` |
| **Type** | Integer (must be even) |
| **Category** | Run duration — domain size |

The number of integer points at which the polynomial is evaluated.  The
domain is the symmetric interval `[-integerRange/2, integerRange/2 - 1]`.

- `LoopList.loadPArray()` iterates `for (x = -halfIntegerRange; x < halfIntegerRange; x++)`,
  producing a list of `integerRange` values.
- `LoopList.subsequentWorkerListInit()` iterates
  `for (int y = 0; y < integerRange; y++)` when computing successive
  differences.
- Passed to `LoopsLogicLoopSemaphore`, where it scales the modulo/
  increment computation that governs how far each semaphore worker
  advances the pArray.
- Inline comment in code: *"Range: -10 to 9"* (for the default value 20).

**Run-duration effect:** Every LoopList stores `integerRange` elements;
all differencing and zero-counting is O(`integerRange`) per worker per
step.  Doubling `integerRange` roughly doubles per-worker memory and time.
Must be even; odd values cause an off-by-one in `halfIntegerRange`.

---

### `setProductRange`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.setProductRange` |
| **Default in code** | `10` |
| **Type** | Integer |
| **Category** | Run duration — pArray sweep width |

Upper bound for each semaphore worker's pArray increment sweep.  Passed
directly as the `setProductRange` argument to `LoopSemaphoreInitial`,
`LoopSemaphore`, and `LoopSemaphoreLast`.

- Each semaphore worker advances the corresponding coefficient of `pArray`
  from `lbSetProductRange` up to `setProductRange`, generating one
  `:Dnode` write per step.
- In the `runney()` outer loop, `setProductRange` starts at its
  configured value and increments by `setProductRangeIncrement` each
  iteration until it reaches `maxSetProductRange`.

**Run-duration effect:** Directly controls the number of pArray values
explored by each semaphore worker thread.  Larger values produce more
graph nodes and a proportionally longer run.

---

### `setProductRAngeIncrement`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.setProductRangeIncrement` |
| **Default in code** | `2` |
| **Note on spelling** | The Cypher return clause spells this `setProductRAngeIncrement` (capital A); the Java field uses lowercase `a`. |
| **Type** | Integer |
| **Category** | Run duration — outer loop step size |

Step size by which `setProductRange` is incremented after each outer
loop iteration in `runney()`:

```java
setProductRange = setProductRange + setProductRangeIncrement;
```

Smaller values mean more outer iterations between `setProductRange` and
`maxSetProductRange`, increasing total nodes written.

**Run-duration effect:** Controls granularity of the parameter sweep.
Currently the increment lines are commented out in `run()` (the primary
entry point), but are active in `runney()`.

---

### `maxSetProductRange`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.maxSetProductRange` |
| **Default in code** | `10` |
| **Type** | Integer |
| **Category** | Run duration — outer loop termination |

The hard upper bound for the `setProductRange` sweep in `runney()`:

```java
while (setProductRange <= maxSetProductRange) { ... }
```

When `setProductRange` exceeds this value the outer loop exits, ending
the run.  Setting `maxSetProductRange == setProductRange` means only one
outer pass is performed.

**Run-duration effect:** Together with `setProductRange` and
`setProductRangeIncrement`, this determines the total number of outer
iterations and therefore the total number of `:Dnode` records generated.

---

### `maxFigPScalar`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.maxFigPScalar` |
| **Default in code** | `1` |
| **Type** | Integer |
| **Category** | Initial condition — figurate polynomial leading coefficient |

The scalar appended as the **last coefficient** of the figurate polynomial
array `figPArray` (and its cached copy `cashedFigPArray`):

```java
figPArray = new PolynomialArray(dimension, maxFigPScalar, true);
// → initPArrayFig(maxFigPScalar, noBufferRun)
// → [0, 0, ..., 0, maxFigPScalar]   (dimension zeros then the scalar)
```

Class-level comment: *"maxFigPScalar and dimension define the initial P
for differencing … appended as last element to figparray, not sure of
effect if varied."*

`figPArray` is the polynomial whose iterated finite differences form the
basis of the ZAD computation.  `maxFigPScalar` scales its leading term.

**Run-duration effect:** Negligible direct effect on duration.  Changes
the polynomial being analyzed, shifting which integers register as zeros
and therefore the content of the graph, not its size.

---

### `pArray`
| | |
|---|---|
| **Java field** | `LoopsDriverTwoPManager.pArrayConfigure` (String) |
| **Read by** | `configureQuery1()` only (column 7) |
| **Default when absent** | All-zero array: `[0, 0, ..., 0]` |
| **Type** | String encoding an integer list, e.g. `"[0, 1, 2, 3, 4, 5]"` |
| **Category** | Initial condition — pArray starting position |

Provides the initial coefficient vector for `pArray`, the "translation
polynomial" that is advanced during the semaphore worker sweep.  When
present it is parsed and used to seed the `PolynomialArray`:

```java
pArray = new PolynomialArray(
    dimension, TRUE,
    arrayStringToIntegerArrayList(pArrayConfigure)
);
```

When this property is absent from the Configure node (i.e.
`configureQuery()` is called instead of `configureQuery1()`), `pArray`
defaults to `[0, 0, ..., 0]` of length `dimension + 1`.

`pArray` is evaluated over `integerRange` inside `LoopList.loadPArray()`
to produce the initial `LoopList` values:

```
LoopList[x] = sum( pArray[y] * x^y )  for x in [-integerRange/2, integerRange/2)
```

Each semaphore worker then increments one coefficient of `pArray` from
`lbSetProductRange` to `setProductRange`, scanning the result space.

**Run-duration effect:** The starting value changes which region of the
parameter space is explored first but does not change the total number of
steps, assuming `setProductRange` and `maxSetProductRange` are unchanged.

---

## Summary Table

| Property | Category | Primary effect on run |
|---|---|---|
| `dimension` | Initial condition (structural) | Polynomial degree; number of worker threads; size of all arrays |
| `integerRange` | Run duration | Number of evaluation points per LoopList; must be even |
| `setProductRange` | Run duration | Upper bound of per-worker pArray sweep; nodes written per outer pass |
| `setProductRAngeIncrement` | Run duration | Step size for outer `setProductRange` loop (`runney()`) |
| `maxSetProductRange` | Run duration | Termination condition for outer loop (`runney()`) |
| `maxFigPScalar` | Initial condition | Leading coefficient of the figurate polynomial `figPArray` |
| `pArray` | Initial condition | Starting coefficient vector for `pArray`; region of parameter space explored |

---

## Example Configure Node (Cypher)

```cypher
MERGE (c:Configure)
SET c.dimension              = 5,
    c.integerRange           = 20,
    c.setProductRange        = 10,
    c.setProductRAngeIncrement = 2,
    c.maxSetProductRange     = 10,
    c.maxFigPScalar          = 1,
    c.pArray                 = "[0, 0, 0, 0, 0, 0]"
```

> Note: only one `:Configure` node should exist.  `MERGE` without
> additional match properties will create it if absent or update the
> existing singleton.
