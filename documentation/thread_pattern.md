# Worker Thread Semaphore Pattern

Reference documentation for the semaphore-synchronized worker thread pattern in ZerosAndDifferences.

---

## 1. Pattern Overview

The application uses a chain of `Callable` worker threads — one per difference level (wNum) — coordinated by paired semaphores. Workers are linked in a forward/backward semaphore chain so that each iteration of the Cartesian product over polynomial coefficients proceeds in strict sequential order through the chain, from `Initial` to `Last`.

```
                      ┌─────────────────┐
                      │ GenerateBinary   │
                      │ Start            │
                      │ releases sf2[1]  │
                      └────────┬────────┘
                               │  sf2
                               ▼
┌────────────────────────────────────────────────────────────┐
│            LoopSemaphoreInitial  (wNum=0)                  │
│                                                            │
│   sf2.acquire() ─► sf2Aquire() × setSize ─► sf1.release() │
│   sb1.acquire() ◄────────────────────── sb1                │
│   sf2.release() (self, for next round)                     │
└──────────────────────────┬─────────────────────────────────┘
                    sf1/sb1 │ ↕ sf2/sb2
                           ▼
┌────────────────────────────────────────────────────────────┐
│            LoopSemaphore  (wNum=1..dimension-1)            │
│                                                            │
│   sf2.acquire() ─► sf2Aquire() × setSize                   │
│   ─► final sf2.acquire() ─► rentrentReset()                │
│   ─► sf1.release()                                         │
│   sb1.acquire() ◄────────────────────── sb1                │
│   sb2.release() ─────────────────────► sb2                 │
└──────────────────────────┬─────────────────────────────────┘
                    sf1/sb1 │ ↕ sf2/sb2
                           ▼
┌────────────────────────────────────────────────────────────┐
│            LoopSemaphoreLast  (wNum=dimension)             │
│                                                            │
│   sf2.acquire() ─► sf2Aquire() (once)                      │
│   ─► endProduct.setEndCproduct(true)                       │
│   ─► sb2.release()                                         │
│   ─► return                                                │
└────────────────────────────────────────────────────────────┘
```

---

## 2. Semaphore Chain Wiring

Workers are chained together through two semaphore lists: `semaphoreListF` (forward) and `semaphoreListB` (backward). Each worker receives four semaphores as constructor arguments — two connecting it to the next worker downstream (`sf1`, `sb1`) and two connecting it to the previous worker upstream (`sf2`, `sb2`).

### 2.1 Wiring Rules

Adjacent workers share semaphore pairs:

```
worker[i].sf1  ══  worker[i+1].sf2     (forward signal)
worker[i].sb1  ══  worker[i+1].sb2     (backward signal)
```

### 2.2 Concrete Wiring (from LoopsDriverTwoP.call)

Four initial semaphore pairs are created at indices 0..3. Each subsequent worker adds one more pair. The pattern indexes from the end of the list:

```java
// workerZero (Initial):
sf1 = semaphoreListF[size-2]    sb1 = semaphoreListB[size-2]
sf2 = semaphoreListF[size-3]    sb2 = semaphoreListB[size-3]

// For each subsequent worker, a new pair is appended first, then:
sf1 = semaphoreListF[size-2]    sb1 = semaphoreListB[size-2]
sf2 = semaphoreListF[size-3]    sb2 = semaphoreListB[size-3]
```

Because adjacent workers index into the same semaphore slots, the chain is established:

| Worker | sf1 / sb1 (downstream) | sf2 / sb2 (upstream) |
|--------|------------------------|----------------------|
| `GenerateBinaryStart` | — | releases `semaphoreListF[1]` |
| `Initial` (wNum=0) | `semaphoreListF[2]` / `semaphoreListB[2]` | `semaphoreListF[1]` / `semaphoreListB[1]` |
| `Middle` (wNum=1) | `semaphoreListF[3]` / `semaphoreListB[3]` | `semaphoreListF[2]` / `semaphoreListB[2]` |
| `Middle` (wNum=2) | `semaphoreListF[4]` / `semaphoreListB[4]` | `semaphoreListF[3]` / `semaphoreListB[3]` |
| ... | ... | ... |
| `Last` (wNum=dim) | `semaphoreListF[n]` / `semaphoreListB[n]` | `semaphoreListF[n-1]` / `semaphoreListB[n-1]` |

---

## 3. The Three Worker Types

### 3.1 LoopSemaphoreInitial

**Role:** First worker in the chain (wNum=0). Seeds the iteration.

**Source:** `LoopsSemaphorePattern/LoopSemaphoreInitial.java`

**Lifecycle per round:**

```
1. sf2.acquire()                   ← waits for upstream (GenerateBinaryStart on first round, self on subsequent)
2. if (!skip):
     loopsLogic.sf2Aquire()        ← fires PropertyChangeEvent on gbList
     setSizee++
     while (setSizee < setSize):
         loopsLogic.sf2Aquire()    ← iterate inner product steps
         setSizee++
     loopsLogic.rentrentReset()    ← trigger pArray carry/reset
3. sf1.release()                   ← signal next worker downstream
4. sb1.acquire()                   ← wait for downstream to finish
5. sf2.release()                   ← release own sf2 for next round
6. loop back to (1) unless endProduct is true
```

**Key difference from Middle:** Does not need an extra `sf2.acquire()` before `rentrentReset()`. Does not release `sb2` (has no upstream worker to signal back to). Instead releases its own `sf2` to start the next round.

### 3.2 LoopSemaphore

**Role:** Intermediate workers (wNum=1..dimension-1). Each represents one coefficient position in the Cartesian product.

**Source:** `LoopsSemaphorePattern/LoopSemaphore.java`

**Lifecycle per round:**

```
1. sf2.acquire()                   ← waits for upstream worker's sf1.release()
2. if (!skip):
     loopsLogic.sf2Aquire()        ← fires PropertyChangeEvent on gbList
     setSizee++
     sb2.release()                 ← signal upstream worker can continue
     while (setSizee < setSize):
         sf2.acquire()             ← wait for upstream's next permit
         loopsLogic.sf2Aquire()    ← iterate inner product step
         setSizee++
         sb2.release()             ← acknowledge each step
     sf2.acquire()                 ← final acquire
     loopsLogic.rentrentReset()    ← trigger pArray carry/reset
3. sf1.release()                   ← signal downstream worker
4. sb1.acquire()                   ← wait for downstream to finish
5. sb2.release()                   ← signal upstream that round is complete
6. loop back to (1) unless endProduct is true
```

**Key behavior:** The inner `while (setSizee < setSize)` loop implements the per-worker coefficient iteration. Each step acquires a permit from upstream (sf2), performs computation, and signals upstream it's ready for the next (sb2). This interleaving allows upstream and downstream workers to step through their iterations in a coordinated, lock-step fashion.

### 3.3 LoopSemaphoreLast

**Role:** Terminal worker (wNum=dimension). Fires once and terminates the chain.

**Source:** `LoopsSemaphorePattern/LoopSemaphoreLast.java`

**Current lifecycle (single-fire):**

```
1. sf2.acquire()                       ← waits for upstream worker
2. loopsLogic.sf2Aquire()              ← one computation step
3. endProduct.setEndCproduct(true)     ← signal termination to all workers
4. sb2.release()                       ← signal upstream
5. return                              ← thread exits
```

**Note:** The `while(!endProduct)` loop and inner iteration are **commented out** in the current code. See Section 8 for expected vs. current behavior.

### 3.4 GenerateBinaryStart

**Role:** Trigger that initiates the chain. Submitted last to the ExecutorService.

**Source:** `fractionintegerset/GenerateBinaryStart.java`

**Lifecycle:**

```
1. startSemap.release()    ← releases semaphoreListF[1], which is workerZero's sf2
2. return
```

---

## 4. Signal Protocol

### 4.1 Forward Signal (sf)

The forward signal propagates computation readiness from upstream (lower wNum) to downstream (higher wNum):

```
GenerateBinaryStart ──sf2──► Initial ──sf1/sf2──► Middle ──sf1/sf2──► ... ──► Last
```

- `sf2.acquire()` — worker blocks until upstream signals it
- `sf1.release()` — worker signals the next downstream worker

### 4.2 Backward Signal (sb)

The backward signal propagates completion acknowledgment from downstream back to upstream:

```
Last ──sb2──► Middle ──sb2──► ... ──sb2──► Initial
```

- `sb1.acquire()` — worker blocks until downstream acknowledges
- `sb2.release()` — worker signals the previous upstream worker

### 4.3 One Full Round

A complete round (one step of the outermost coefficient) proceeds as a forward wave followed by a backward wave:

```
FORWARD WAVE (sf):
  Start ─► Initial ─► Worker1 ─► Worker2 ─► ... ─► Last

BACKWARD WAVE (sb):
  Last ─► ... ─► Worker2 ─► Worker1 ─► Initial
                                         │
                                         └─► sf2.release() (self, next round)
```

---

## 5. Inner Iteration (setSize Loop)

Each worker (except Last in current code) has an inner loop controlled by `setSize` and `lbSetProductRange`:

```
setSizee = lbSetProductRange   (lower bound)
setSize  = setProductRange     (upper bound, sometimes +1 depending on constructor)

while (setSizee < setSize):
    sf2Aquire()     ← computation step
    setSizee++
```

This inner loop iterates one coefficient position through its range. The total Cartesian product is the combination of all workers' inner loops:

```
Total iterations = setSize^(number of non-skipped workers)
```

For `dimension=5` and `setProductRange=4`:
- 5 workers (wNum 0..4), up to `4^5 = 1024` coefficient combinations

---

## 6. The Skip Pattern (muMaskList)

The `skip` boolean — derived from `muMaskList` — allows individual workers to be bypassed during the semaphore iteration.

### 6.1 muMaskList Construction

In `LoopsDriverTwoPManager`, the method `muListBoolean(ArrayList theList)` converts a binary pattern (from the database `muQuerry`) into a list of booleans:

```java
// Example: dimension=5, theList=[2,3]
// muListBoolean = [false, true, false, true, false]
//                  wNum0  wNum1 wNum2  wNum3 wNum4
```

Each boolean is passed to the corresponding worker's constructor as the `skip` parameter.

### 6.2 Skip Behavior

When `skip=true`, a worker:
- Still participates in the semaphore chain (maintains ordering)
- Does NOT call `loopsLogic.sf2Aquire()` (no computation)
- Does NOT iterate its inner loop (no coefficient stepping)
- Passes the signal straight through: `sf2.acquire() → sf1.release() → sb1.acquire() → sb2.release()`

When `skip=false` (default), the worker runs its full inner iteration.

### 6.3 No-Skip Default

The no-argument `muListBoolean()` method returns all `false` — no workers are skipped. This is the default for the first run in `LoopsDriverTwoPManager.run()`:

```java
noBufferRun((ArrayList)muListBoolean());   // all workers active
```

Subsequent runs pull skip patterns from the database via `muQuerry()`.

---

## 7. EndProduct Termination

`EndProduct` is a shared mutable boolean flag that all workers check in their while-loop condition.

**Source:** `fractionintegerset/EndProduct.java`

```java
public class EndProduct {
    boolean endCproduct;
    public boolean getEndCproduct() { return endCproduct; }
    public void setEndCproduct(boolean endCproduct) { this.endCproduct = endCproduct; }
}
```

### 7.1 Termination Sequence

1. `LoopSemaphoreLast` sets `endProduct.setEndCproduct(true)` and releases `sb2`
2. The backward wave propagates through the chain via `sb2.release()` / `sb1.acquire()`
3. Each worker unblocks from `sb1.acquire()`, releases its own `sb2`, and loops back to the `while(!endProduct.getEndCproduct())` check
4. Since `endProduct` is now `true`, each worker exits its while loop and returns
5. The `CompletionService` in `LoopsDriverTwoP.call()` collects all returns via `service.take()`

```
Time ──────────────────────────────────────────────────────────────────►

Last:    sf2.acquire ─► setEndCproduct(true) ─► sb2.release ─► return
                                                     │
Middle:                              sb1.acquire ◄───┘ sb2.release ─► exit while
                                                           │
Initial:                                    sb1.acquire ◄──┘ sf2.release ─► exit while
```

---

## 8. Current vs. Expected Behavior

### 8.1 Current Behavior of LoopSemaphoreLast

The `while` loop and inner iteration in `LoopSemaphoreLast` are **commented out**:

```java
// while(!endProduct.getEndCproduct()){      ← commented out
    sf2.acquire();
    loopsLogic.sf2Aquire(); lbSetSize++;
//  sb2.release();                           ← commented out
//  while (lbSetSize < setSize) {            ← commented out
//      sf2.acquire();
//      loopsLogic.sf2Aquire();
//      if (lbSetSize+1==setSize) { endProduct.setEndCproduct(true); }
//      sb2.release();
//      lbSetSize++;
//  }
    endProduct.setEndCproduct(true);         ← fires immediately
    sb2.release();
// } //endcproduct while loop               ← commented out
```

**Consequence:** The Last worker:
- Receives exactly one forward signal
- Fires one `sf2Aquire()` computation
- Immediately terminates the entire chain
- Does NOT iterate through its coefficient range

### 8.2 Expected (Commented-Out) Behavior

The commented code shows the intended behavior where the Last worker would:

1. Participate in the `while(!endProduct)` loop like other workers
2. Iterate its inner `while (lbSetSize < setSize)` loop, stepping through its coefficient range
3. Set `endProduct=true` only at the **final step** (`if (lbSetSize+1==setSize)`)
4. Properly interleave `sf2.acquire()` / `sb2.release()` with the upstream worker
5. After the inner loop completes, release `sf1` and wait on `sb1` (also commented out) to coordinate with workers that would be downstream of it

### 8.3 Impact on Product Behavior

| Aspect | Current | Expected (commented-out) |
|--------|---------|--------------------------|
| Last worker iterations | 1 | `setSize - lbSetProductRange` |
| Cartesian product coverage | Partial — last dimension is fixed | Complete — all dimensions iterate |
| EndProduct trigger | Immediate after first signal | Only at final step of last worker's range |
| Chain termination | After one full forward/backward wave | After full exhaustion of product space |

---

## 9. How sf2Aquire Triggers Computation

Each call to `loopsLogic.sf2Aquire()` is the computation heartbeat. The concrete implementation is `LoopsLogicLoopSemaphore`:

**Source:** `LoopsLogic/LoopsLogicLoopSemaphore.java`

```java
public void sf2Aquire() {
    synchronized (this) {
        synchronized (pArray) {
            copyPArray = pArray;
        }
        gbList.setGBList(new muNumDen(modulo, workerNum, 
                         gbList.getancestorlist(), gbList.getAMatrix(), copyPArray));
    }
}
```

This calls `gbList.setGBList(...)` which fires a `PropertyChangeEvent` on the `LoopList`. The attached `LoopListener.propertyChange()` then:

1. Increments `pArray` via `pArrayIncrement()`
2. Loops top-down (`wCount = dimension-1` down to `0`):
   - `computeIndexZero(wCount, rModulo)` — adjusts `figPArray[wCount]` to force a zero
   - `updateRlistB()` — clears and rebuilds all difference sequences from the modified `figPArray`
3. Appends results to `muBuffer` via `appendMuBuffer()` — queues `GaussBean1` objects for the database writer
4. Copies cashed state for the next iteration
5. Calls `pArrayIncrement()` to advance the iteration counter

### 9.1 rentrentReset

Called at the end of each worker's inner loop. Triggers `pArray.setpArray(...)` which fires the `PArrayResetListener`:

```java
public void rentrentReset() {
    pArray.setpArray(new muNumDen(modulo, workerNum, gbList.getancestorlist()));
}
```

The `PArrayResetListener` handles carry/reset logic — when a worker's coefficient position overflows `setProductRange`, the array resets and cascades the overflow.

---

## 10. Interfaces and Abstractions

### 10.1 LoopsSemaphoreInterface

Defines the contract for semaphore-step logic:

```java
public interface LoopsSemaphoreInterface {
    void sf2Aquire();       // computation step triggered by sf2 permit
    void sb2Release();      // (unused in current implementation)
    void sf1Release();      // (unused in current implementation)
    void sb1Aquire();       // (unused in current implementation)
    void rentrentReset();   // pArray carry/reset at end of inner loop
}
```

Only `sf2Aquire()` and `rentrentReset()` are actively used. The others are stubs on `AbstractLoopSemaphore`.

### 10.2 LoopsConditionInterface

Defines a one-shot boolean condition:

```java
public interface LoopsConditionInterface {
    void setLoopCondition();
    boolean getLoopCondition();
}
```

`LoopsLogicLoopCondition` returns `true` once, then `false` on subsequent calls. Currently unused in the worker logic but available for conditional iteration control.

### 10.3 AbstractLoopSemaphore

Base class providing no-op defaults for `LoopsSemaphoreInterface` methods. `LoopsLogicLoopSemaphore` extends this and overrides `sf2Aquire()` and `rentrentReset()`.

---

## 11. Class Hierarchy and File Map

```
LoopsSemaphorePattern/
├── LoopsSemaphoreInterface.java      Interface: sf2Aquire, rentrentReset
├── LoopsConditionInterface.java      Interface: setLoopCondition, getLoopCondition
├── AbstractLoopSemaphore.java        Abstract base: no-op defaults
├── AbstractLoopCondition.java        Abstract base: no-op defaults
├── LoopSemaphoreInitial.java         Callable worker: first in chain (wNum=0)
├── LoopSemaphore.java                Callable worker: middle of chain (wNum=1..dim-1)
└── LoopSemaphoreLast.java            Callable worker: last in chain (wNum=dimension)

LoopsLogic/
├── LoopsLogicLoopSemaphore.java      Concrete: fires PropertyChangeEvent via gbList.setGBList
└── LoopsLogicLoopCondition.java      Concrete: one-shot boolean

fractionintegerset/
├── EndProduct.java                   Shared termination flag
└── GenerateBinaryStart.java          Chain trigger: releases initial sf2

MainClass/
├── LoopsDriverTwoP.java              Wires semaphore chain, submits workers
└── LoopsDriverTwoPManager.java       Manages lifecycle, configuration, skip patterns

LoopLists/
├── LoopListener.java                 PropertyChangeListener: core computation
└── LoopListenerRunnable.java         Runnable/Callable for factored sub-iterations

PArrayReset/
├── PolynomialArray.java              Observable array: fires PropertyChangeEvent on set
└── PArrayResetLIstener.java          Handles carry/reset on pArray overflow
```

---

## 12. Execution Flow Diagram

Complete lifecycle from `LoopsDriverTwoPManager.run()` through chain termination:

```
LoopsDriverTwoPManager.run()
│
├── configureQuery1()                                read runtime params from DB
├── startbuffer()                                    launch GaussTable1 consumer thread
│
├── noBufferRun(muListBoolean())                     first run, no skips
│   │
│   └── new LoopsDriverTwoP(...)
│       │
│       └── .call()
│           │
│           ├── Create LoopList[0..dimension]         difference sequences
│           ├── Interpolate each LoopList (NewtonInterpolator)  vmResult
│           ├── Attach LoopListener to each gbList    PropertyChangeListener
│           │
│           ├── Wire semaphore chain:
│           │   ├── workerZero = LoopSemaphoreInitial(sf1,sb1,sf2,sb2,...)
│           │   ├── worker[1..dim-1] = LoopSemaphore(sf1,sb1,sf2,sb2,...)
│           │   └── workerLast = LoopSemaphoreLast(sf1,sb1,sf2,sb2,...)
│           │
│           ├── Attach PArrayResetListener to pArray
│           │
│           ├── service.submit(workerZero)
│           ├── service.submit(worker[1..dim-1])
│           ├── service.submit(workerLast)
│           ├── service.submit(workerStart)            ◄── kicks off the chain last
│           │
│           └── while(workerCounter > 0):
│               service.take()                         collect completions
│
├── muQuerry()                                        get next skip pattern from DB
│
└── while(!valueList.isEmpty()):
    ├── noBufferRun(muListBoolean(nBList))             run with skip pattern
    ├── muDelete(nBList)                               remove processed pattern
    └── muQuerry()                                     get next
```

---

## 13. Concurrency Considerations

### 13.1 Thread Safety

- `sf2Aquire()` is `synchronized(this)` on the `LoopsLogicLoopSemaphore` instance, with a nested `synchronized(pArray)` for safe copy
- `LoopListener.propertyChange()` uses `synchronized(this)` blocks when accessing shared `muList`
- `pArrayIncrement()` is `synchronized`
- `updateRlistB()` is `synchronized`
- The `muList` buffer uses `muList.wait()` / `muList.notify()` for flow control when the buffer exceeds 25,000 entries

### 13.2 Semaphore Permits

All semaphores are initialized with **0 permits** (`new Semaphore(0)`). No worker can proceed until explicitly signaled. This ensures the chain starts only when `GenerateBinaryStart` releases the first permit.

### 13.3 ExecutorService

`LoopsDriverTwoP` uses a fixed thread pool of 50 threads with a `CompletionService` wrapper. Workers are submitted and collected via `service.take()` / `future.get()`. The pool is shut down after all workers return.

---

## 14. Quick Reference: Semaphore Operations by Worker Type

| Operation | Initial | Middle | Last (current) |
|-----------|---------|--------|----------------|
| `sf2.acquire()` (enter) | 1 per round | 1 + setSize per round | 1 total |
| `loopsLogic.sf2Aquire()` | setSize times | setSize times | 1 time |
| `loopsLogic.rentrentReset()` | 1 per round | 1 per round | never |
| `sf1.release()` | 1 per round | 1 per round | never |
| `sb1.acquire()` | 1 per round | 1 per round | never |
| `sb2.release()` | never | setSize + 1 per round | 1 total |
| `sf2.release()` (self) | 1 per round | never | never |
| Loops until endProduct | yes | yes | no (single fire) |
| Sets endProduct | never | never | yes |
