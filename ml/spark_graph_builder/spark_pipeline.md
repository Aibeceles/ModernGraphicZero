# Spark Graph Builder Pipeline

## Overview

`pipeline.py` ingests a single Parquet batch file produced by the Java
`GaussTable1.writeBatchToParquet()` method, builds an in-memory GraphFrame
of `Dnode` vertices and `zMap` directed edges, and optionally persists the
graph to Neo4j via the Spark Connector.

---

## 1. Source Data Provenance

### Upstream Producer

| Item | Detail |
|------|--------|
| Java class | `GaussTable1` in `ZerosAndDifferences033021/src/MainClass/GaussTable1.java` |
| Method | `writeBatchToParquet()` |
| Trigger | Called after the compute thread fills `dbbVector` with `GaussBean1` results |
| Concurrency model | `dbbVector` is guarded by `synchronized`; a semaphore (`tlSem`) gates each drain pass |

### How Records Enter the File

1. Computation threads push `GaussBean1` results into `dbbVector`.
2. `writeBatchToParquet()` acquires `tlSem` and drains up to
   `batchNumber = batchSize × chainSize` (`3 × 6 = 18`) items per loop
   iteration.
3. Items are grouped into **chains** of length `chainSize = 6`.  Within each
   chain, items are assigned a sequential `chain_position` (0–5).  Each group
   of `chainSize` items shares the same `batch_id`.
4. A monotonically-increasing `file_batch` counter (local to one file write)
   increments after every flush of `batchNumber` items.
5. All records for a single invocation are written to **one** Parquet file,
   named `batches_<yyyyMMdd_HHmmssSSS>_<UUID>.parquet`, in the staging
   directory.

### Avro/Parquet Schema (`DNODE_BATCH_SCHEMA`)

```
record DnodeBatch (namespace MainClass) {
    required string  vmResult        // vertex identity key — symbolic polynomial result
    required int     n               // numerator of the rational form
    required int     d               // denominator of the rational form
    required int     z               // total zero count
    required string  muList          // serialized mu-parameter list
    required string  rootList        // serialized root list
    required int     wNum            // work-number / iteration counter
    required int     batch_id        // batch index within this file (0-based)
    required int     chain_position  // position within a chain (0 .. chainSize-1)
    required int     file_batch      // flush counter within this file (0-based)
}
```

Compression codec: **Snappy**.  
Writer library: Apache Parquet / Avro (`AvroParquetWriter`).

### Staging Directory

| Source | Default value |
|--------|---------------|
| Java constant `PARQUET_STAGING_DIR` | `<user.dir>/data/parquet_batches/` |
| Python `config.PARQUET_BATCH_DIR` | `ZerosAndDifferences033021/dist/data/parquet_batches/` |
| Environment override | `PARQUET_BATCH_DIR` env var (read via `python-dotenv`) |

---

## 2. Parquet Ingestion Policy

> **One invocation of `pipeline.py` processes exactly one Parquet file.**
> Multiple concurrent invocations are supported; each exclusively claims a
> distinct file.

### Rationale

- Each Parquet file written by `writeBatchToParquet()` is self-contained:
  it holds a complete set of chains for one computation run.
- Processing one file per invocation keeps memory bounded and makes
  provenance traceable (one file → one graph load).
- Multiple concurrent `pipeline.py` processes may target the same staging
  directory.  An atomic lock-file mechanism guarantees each file is processed
  by exactly one worker.  Neo4j writes use MERGE/upsert semantics, so results
  are always correct regardless of invocation order.

### File-Claim Protocol (`file_claim.py`)

The module `spark_graph_builder.file_claim` implements three functions:

| Function | Called when | Effect |
|----------|-------------|--------|
| `claim_next_parquet(batch_dir)` | pipeline start | atomically claims one file; returns its path or `None` |
| `archive_and_release(path, batch_dir)` | pipeline success | moves file to `done/`, deletes lock |
| `release_claim(path)` | pipeline failure | deletes lock so another worker can retry |

#### Claiming algorithm

1. Glob `batch_dir/*.parquet`, sorted by filename (oldest timestamp first).
2. For each candidate, attempt to create `<filename>.parquet.lock` using
   `os.open(O_CREAT | O_EXCL)` — atomic on both Windows and POSIX.
3. First process to succeed writes its PID to the lock file and owns the file.
4. All other processes receive `FileExistsError` and try the next candidate.
5. If no file can be claimed, `pipeline.py` exits with code 0 and a message.

#### Post-processing

- **Success**: the Parquet file is moved to `<batch_dir>/done/` and its
  `.lock` sidecar is deleted.
- **Failure (exception)**: the `.lock` sidecar is deleted so another worker
  can retry the file.  The Parquet file remains in place.
- **Hard kill** (SIGKILL / Task Manager): the `.lock` sidecar is orphaned.
  Recovery: manually delete `<filename>.parquet.lock` from the staging
  directory.

### Invocation

Pass the staging **directory** via `--batch-dir` (the pipeline selects a
file automatically):

```bash
python -m spark_graph_builder.pipeline \
    --batch-dir path/to/parquet_batches/
```

Or programmatically:

```python
from spark_graph_builder.pipeline import run_pipeline
graph = run_pipeline(batch_dir="path/to/parquet_batches/")
# Returns None if no unclaimed file is available.
```

### File Selection / Naming Convention

| Field | Pattern |
|-------|---------|
| Prefix | `batches_` |
| Timestamp | `yyyyMMdd_HHmmssSSS` |
| Uniquifier | UUID4 |
| Extension | `.parquet` |
| Lock sidecar | `<same name>.parquet.lock` |
| Archive destination | `<batch_dir>/done/<same name>.parquet` |
| Example | `batches_20260312_143022123_550e8400-e29b-41d4-a716-446655440000.parquet` |

The `done/` subfolder is excluded from the glob scan, so archived files are
never reclaimed.

---

## 3. Spark Structure Post-Ingress

### 3a. SparkSession

Created by `config.create_spark_session()` in **local mode** (`local[*]`),
meaning all executors run in the same JVM process on the local machine.

| Spark package | Version |
|---------------|---------|
| Neo4j Connector | `org.neo4j:neo4j-connector-apache-spark_2.12:5.3.8_for_spark_3` |
| GraphFrames | `graphframes:graphframes:0.8.4-spark3.5-s_2.12` |
| Scala compat | 2.12 |

Runtime requirements set by `create_spark_session()`:

| Env var | Default |
|---------|---------|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot` |
| `HADOOP_HOME` | `C:\hadoop` |

### 3b. Raw DataFrame (`raw_df`)

```
nodes.read_raw_batches(spark, parquet_path)  →  spark.read.parquet(<single_file>)
```

Columns inherited directly from the Parquet schema:

| Column | Spark Type | Role |
|--------|-----------|------|
| `vmResult` | StringType | vertex identity |
| `n` | IntegerType | numerator |
| `d` | IntegerType | denominator |
| `z` | IntegerType | zero count |
| `muList` | StringType | mu parameters |
| `rootList` | StringType | root list |
| `wNum` | IntegerType | work number |
| `batch_id` | IntegerType | chain grouping key |
| `chain_position` | IntegerType | ordering within chain |
| `file_batch` | IntegerType | flush counter |

`raw_df.cache()` is called immediately after read so that both the vertex and
edge derivation passes share the same in-memory materialization.

### 3c. Vertices DataFrame (`vertices_df`)

**Produced by** `nodes.build_vertices(raw_df)`

**Logical transform:**

```sql
SELECT
    vmResult  AS  id,          -- GraphFrames requires column named "id"
    n, d, z,
    muList, rootList,
    wNum
FROM raw_df
GROUP BY id  -- effectively: dropDuplicates(["id"])
```

**Implementation:**

```python
raw_df
  .select(col("vmResult").alias("id"), "n", "d", "z", "muList", "rootList", "wNum")
  .dropDuplicates(["id"])
```

**Deduplication rule:** First occurrence wins (Spark's `dropDuplicates` is
order-sensitive on the physical plan; no explicit ordering is applied, so the
winner is arbitrary among duplicates—which is acceptable because `vmResult`
uniquely identifies a mathematical result whose properties are deterministic).

**Output schema:**

| Column | Type | Description |
|--------|------|-------------|
| `id` | StringType | unique vertex key (`vmResult`) |
| `n` | IntegerType | numerator |
| `d` | IntegerType | denominator |
| `z` | IntegerType | zero count |
| `muList` | StringType | mu parameters |
| `rootList` | StringType | root list |
| `wNum` | IntegerType | work number |

### 3d. Edges DataFrame (`edges_df`)

**Produced by** `edges.build_edges(raw_df)`

**Logical transform:**

```sql
-- Window: partition rows by (file_batch, batch_id), order by chain_position
-- lead() fetches the vmResult of the next row in the chain

SELECT
    vmResult             AS  src,
    LEAD(vmResult) OVER (
        PARTITION BY file_batch, batch_id
        ORDER BY chain_position
    )                    AS  dst,
    'zMap'               AS  relationship
FROM raw_df
WHERE dst IS NOT NULL          -- drop last element of each chain (no successor)
GROUP BY src, dst              -- dropDuplicates(["src", "dst"])
```

**Implementation:**

```python
chain_window = (
    Window
    .partitionBy("file_batch", "batch_id")
    .orderBy("chain_position")
)

raw_df
  .select(
      col("vmResult").alias("src"),
      lead("vmResult").over(chain_window).alias("dst"),
      lit("zMap").alias("relationship"),
  )
  .filter(col("dst").isNotNull())
  .dropDuplicates(["src", "dst"])
```

**Semantics:** Each row represents a directed edge from one `Dnode` to the
next within the same computation chain.  The chain depth is `chainSize = 6`,
so each chain of length 6 contributes at most 5 edges.

**Output schema:**

| Column | Type | Description |
|--------|------|-------------|
| `src` | StringType | source vertex `vmResult` |
| `dst` | StringType | target vertex `vmResult` |
| `relationship` | StringType | always `"zMap"` |

### 3e. GraphFrame

```python
graph = GraphFrame(vertices_df, edges_df)
```

An in-memory GraphFrames graph.  Standard GraphFrames algorithms
(`graph.inDegrees`, `graph.pageRank`, `graph.connectedComponents`, etc.)
are available on the returned object.

After the graph is constructed:

```python
raw_df.unpersist()   # release the cached raw DataFrame
```

---

## 4. Neo4j Persistence

Controlled by `persist_to_neo4j` flag (default `True`).

### Connection

| Setting | Default | Env Override |
|---------|---------|-------------|
| URL | `bolt://localhost:7687` | `NEO4J_URL` |
| User | `neo4j` | `NEO4J_USER` |
| Password | `ChiefQuippy` | `NEO4J_PASSWORD` |
| Database | `tagtest` | `NEO4J_DATABASE` |

### Node Write (`write_nodes`)

```
DataSource format   : org.neo4j.spark.DataSource
mode                : Append
label               : :Dnode
node key (MERGE)    : id → vmResult
```

Uses **MERGE** semantics: if a `:Dnode` with the given `vmResult` already
exists, it is updated; otherwise it is created.  Idempotent on re-run.

### Edge Write (`write_edges`)

```
DataSource format   : org.neo4j.spark.DataSource
mode                : Append
query (explicit)    : MATCH (src:Dnode {vmResult: event.src})
                      MATCH (dst:Dnode {vmResult: event.dst})
                      MERGE (src)-[:zMap]->(dst)
```

Uses an **explicit MERGE query** so that a given `(src, dst)` pair
produces at most one `zMap` relationship regardless of how many pipeline
runs write the same edge.  Source and target nodes must already exist
(nodes are written first in `run_pipeline`).  The `event` map is
populated by the Spark Connector from each row of `edges_df`.

---

## 5. Pipeline Execution Sequence

```
Java GaussTable1.writeBatchToParquet()
        │
        │  writes  batches_<ts>_<uuid>.parquet  (Snappy/Avro schema)
        ▼
file_claim.claim_next_parquet(batch_dir)
        │  atomically creates batches_<ts>_<uuid>.parquet.lock
        │  returns single parquet_path  (or None → exit 0)
        ▼
config.create_spark_session()
        │
        ▼
nodes.read_raw_batches(spark, parquet_path)    ← single file
  → raw_df  [vmResult, n, d, z, muList, rootList, wNum, batch_id, chain_position, file_batch]
        │
        ├─── raw_df.cache()
        │
        ├──► nodes.build_vertices(raw_df)
        │      SELECT vmResult AS id, n, d, z, muList, rootList, wNum
        │      dropDuplicates(["id"])
        │      → vertices_df
        │
        └──► edges.build_edges(raw_df)
               Window PARTITION BY (file_batch, batch_id) ORDER BY chain_position
               lead(vmResult) → dst
               filter dst IS NOT NULL
               dropDuplicates(["src","dst"])
               → edges_df  [src, dst, relationship="zMap"]
                    │
                    ▼
             GraphFrame(vertices_df, edges_df)
                    │
          ┌─────────┴──────────┐
          │  persist_to_neo4j  │
          │  (if True)         │
          │                    │
          ▼                    ▼
    write_nodes()        write_edges()
    :Dnode MERGE         :zMap MERGE (explicit query)
          │
          ▼
    raw_df.unpersist()
          │
          ▼                         ▼ (on exception)
  archive_and_release()        release_claim()
  move → done/, delete .lock   delete .lock, re-raise
          │
          ▼
    return GraphFrame
```

---

## 6. Module Reference

| Module | Responsibility |
|--------|---------------|
| `config.py` | SparkSession factory; all environment-sourced constants |
| `nodes.py` | `read_raw_batches()` — parquet read; `build_vertices()` — dedup |
| `edges.py` | `build_edges()` — window-based chain-to-edge derivation |
| `writer.py` | `write_nodes()`, `write_edges()` — Neo4j Spark Connector writes |
| `pipeline.py` | Orchestrates all of the above; CLI entry point |

---

## 7. Preconditions for `run_pipelines.ps1`

The following must be satisfied before launching `run_pipelines.ps1`.

### 7a. Upstream Java Pipeline Must Have Run First

`run_pipelines.ps1` consumes **Parquet files** written by the Java
`GaussTable1.writeBatchToParquet()` method.  Those files are produced by
the Java ZAD pipeline (`run_zad_batches.ps1`), which itself consumes
`:Configure` nodes from Neo4j.

> **`:Configure` nodes and the `:ConfigLock` node are preconditions for
> `run_zad_batches.ps1`, not for `run_pipelines.ps1`.**  By the time
> `run_pipelines.ps1` is launched, the Java pipeline should have already
> processed all `:Configure` nodes and written the resulting Parquet files
> to the staging directory.

Required sequence:

```
1. Populate :Configure nodes in Neo4j  (and :ConfigLock node)
        │
        ▼
2. Run run_zad_batches.ps1
   Java workers consume :Configure nodes → write batches_*.parquet files
        │
        ▼
3. Run run_pipelines.ps1
   Spark workers consume Parquet files → write :Dnode graph to Neo4j
```

### 7b. Staging Directory

| Check | Requirement |
|-------|-------------|
| Directory exists | `$ParquetBatchDir` (default `ml\data\parquet_batches\`) must exist |
| Files present | At least one `batches_*.parquet` file must be present |
| No orphaned locks | No `*.parquet.lock` sidecars from a previous crashed run; delete them manually if present |
| `done/` subfolder | Created automatically on first successful run; no action needed |

### 7c. Neo4j

| Check | Requirement |
|-------|-------------|
| Server running | Neo4j must be reachable at `NEO4J_URL` (default `bolt://localhost:7687`) |
| Database exists | Target database (`NEO4J_DATABASE`, default `tagtest`) must exist |
| Credentials | `NEO4J_USER` / `NEO4J_PASSWORD` must be correct |
| Unique constraint (recommended) | `CREATE CONSTRAINT dnode_vmresult_unique IF NOT EXISTS FOR (d:Dnode) REQUIRE d.vmResult IS UNIQUE` — required for `ConcurrentCount > 1` to prevent race-condition duplicate nodes; safe to add before any data exists |

### 7d. Python Environment

| Check | Requirement |
|-------|-------------|
| Virtual environment | `.venv` activated or `$PythonExe` points to the correct interpreter |
| Dependencies installed | `pyspark`, `graphframes`, `neo4j-connector-apache-spark`, `python-dotenv` |
| `.env` file (optional) | `ml/.env` may override `NEO4J_URL`, `NEO4J_USER`, `NEO4J_PASSWORD`, `NEO4J_DATABASE`, `PARQUET_BATCH_DIR` |

### 7e. Java / Hadoop (Windows)

| Check | Requirement |
|-------|-------------|
| Java 17 | Must be installed; `SPARK_JAVA_HOME` defaults to `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot` |
| Hadoop winutils | `HADOOP_HOME` (default `C:\hadoop`) must contain `bin\winutils.exe`; required for Spark on Windows |

---

## 8. Out of Scope

- **Concurrent pipeline execution**: running multiple `pipeline.py` processes
  against separate Parquet files simultaneously.  The Neo4j MERGE/upsert
  semantics are safe for concurrent writes, but scheduling, file locking, and
  orchestration are not addressed here.
- **Schema evolution**: adding or removing fields in `DNODE_BATCH_SCHEMA`
  requires coordinated changes in both the Java writer and Python reader.
- **Parquet file archival / cleanup**: moving or deleting processed files
  after ingestion.
- **Error recovery / partial writes**: handling interrupted pipeline runs
  or partially-written Parquet files.
