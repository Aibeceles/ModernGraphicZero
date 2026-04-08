# JAR Commands Summary
> Backing code for `TwoPolynomialGenerator.jar` and `ZADScripts.jar`

---

## System Architecture Overview

Both JARs form a **data generation and graph-query pipeline** built around:
- **Neo4j** graph database (via JDBC bolt driver `jdbc:neo4j:bolt://localhost`)
- **Apache Kafka** message broker (`localhost:9092`) for inter-process streaming
- **Apache Spark** (consumed downstream in Zeppelin notebooks via `import zadscripts.DFScripts`)

The Neo4j database must be running with the graph schema below populated before either JAR can operate.

---

## Required Neo4j Graph Schema

```cypher
CREATE INDEX FOR (n:IndexedBy) ON (n.N, n.MaxN);
CREATE INDEX FOR (n:Evaluate) ON (n.Value);
CREATE INDEX FOR (n:TwoSeqFactor) ON (n.twoSeq);
CREATE INDEX FOR (n:CollectedProductArgNode) ON (n.MaxN, n.N);
CREATE INDEX FOR (n:IndexedByArgument) ON (n.N, n.MaxN);
CREATE INDEX FOR (n:CreatedBy) ON (n.resultId);
CREATE INDEX FOR (n:Dnode) ON (n.vmResult);
```

**Node labels used:**
| Label | Key Properties | Role |
|---|---|---|
| `VertexNode` | `Scalar`, `Degree` | Polynomial term (coefficient + degree) |
| `IndexedBy` | `N`, `MaxN`, `Dimension`, `RowCounter` | Groups vertex nodes at a given (N, MaxN) index |
| `IndexedByArgument` | `N`, `MaxN`, `Dimension` | Argument variant of IndexedBy |
| `Evaluate` | `Value` | Powers of 2 evaluated at N |
| `TwoSeqFactor` | `twoSeq` | Row scalar (power-of-2 sequence) |
| `CollectedProductArgNode` | `N`, `MaxN`, `Dimension`, `tft` | Pre-collected product argument node |
| `ArgumentsNode` | `ScalarProduct`, `DegreeSum`, `ResultDivisor` | Aggregated polynomial argument |

---

## JAR 1: `TwoPolynomialGenerator.jar`

**Source project:** `JavaProjects/TwoPolynomialGeneratorK`  
**Entry point:** `MainClass.LoopsMain`

### Command signature
```
java -jar TwoPolynomialGenerator.jar <csvFile> <MaxN> <lowerN> <upperN>
```

**Example:**
```
java -jar TwoPolynomialGenerator.jar test.csv 8 2 9
```

### What it does

1. **`plvManager` (Thread)** — receives `(MaxN, lowerN, upperN)` and launches a single `plvRunThread`. Semaphore-guarded producer thread.

2. **`plvRunThread` (Thread)** — iterates over `n` from `lowerN` to `upperN` for a fixed `MaxN`. For each `n`, it computes:
   - The **two-polynomial terms**: scalar × `twoSeq` row scalar combinations, organized as `(degree, scalar)` vertex pairs.
   - Results are pushed as `rowBean` objects into a shared `vertexVector` (thread-safe list).

3. **`zaddbTable1` (Thread)** — consumer thread that:
   - Reads `rowBean` entries from the shared `vertexVector` as they are produced.
   - Computes `targetEvaluate = 2^N` for each row.
   - Serializes each row as a **JSON map** via Jackson `ObjectMapper`.
   - Publishes the JSON to the Kafka topic **`twoPoly`**.
   - Also writes a CSV header to the output file (`csvFile` arg).

### Parameters in the example runs
| Arg | Example | Meaning |
|---|---|---|
| `csvFile` | `test.csv` | Output file (CSV header written; data to Kafka) |
| `MaxN` | `8`, `16`, `32`, `64`, `10000` | Upper bound N (defines the polynomial index space) |
| `lowerN` | `2` | Start of n range |
| `upperN` | `9`, `17`, `33`, `65`, `101` | End of n range (MaxN + 1) |

The number in the comment (e.g. `67`, `240`) is the expected **row count** produced.

---

## JAR 2: `ZADScripts.jar`

**Source project:** `JavaProjects/ZADScriptsK`  
**Entry point:** `zadscripts.ZADScripts`

### Command signature
```
java -jar ZADScripts.jar <lowRange> <highRange> <index> <dimension>
```

**Example:**
```
java -jar ZADScripts.jar 2 7 8 2
```

### What it does

Drives a **multi-step Cypher query pipeline** against the Neo4j graph, streaming results to Kafka topics, to compute polynomial product terms at a given dimension level.

#### Top-level dispatch (`ZADScripts.main`)
- If `dimension == "2"`: runs initial dimension-2 scripts then publishes to write-dimension `4`.
- If `dimension != "2"` and `dimension != "4"`: runs higher-dimension scripts (dimension doubles: `writeDimension = dimension × 2`).

#### `ScriptScheduler.scheduler()`
Outer loop over `rangeLow..rangeHigh`. For each `n`:

1. **`constraintLoop(1, ...)` → `s12Initial`** — runs the **initial product Cypher query** (`IndexedBy` + `TwoSeqFactor` pattern). Matches vertex nodes at dimension 2 with their twoSeq row scalars. Computes `scalarProduct`, `degreeSum`, `resultDivisor` for each polynomial term pair. Streams result to Kafka topic **`s12`**.

2. **`pollS12()`** — polls Neo4j via `CollectedProductArgNode` to confirm the last written record is present (write-read consistency gate).

3. **`constraintLoop(3, ...)` → `s3TEc`** — runs the **collected product argument query** (`CollectedProductArgNode` → `ArgumentsNode`). Aggregates scalars with `apoc.number.exact` arithmetic, normalizes by `maxDivisor`. Streams to Kafka topic **`s3t`**.

4. **`pollS3t()`** — consistency poll against `IndexedBy` for the last-written term.

5. **`s3ADc` loop** — iterates degree `0..indx`, running the **indexed-by argument degree-constrained query** (`IndexedByArgument` + `VertexNode` filtered by degree). Streams to Kafka topic **`s3a`**.

6. **`pollS3a()`** — consistency poll against `IndexedByArgument`.

### Parameters in the example runs
| Arg | Example | Meaning |
|---|---|---|
| `lowRange` | `2` | Start of n range (e.g. n=2) |
| `highRange` | `7` | End of n range |
| `index` | `8` | MaxN (the index space, e.g. MaxN=8) |
| `dimension` | `2`, `8`, `16`, `32` | Starting read dimension; `writeDimension = dimension × 2` |

The comment numbers (e.g. `19001`, `19852`) are **Neo4j node counts** at the time of the run, used to track graph growth.

---

## Connection to `import zadscripts.DFScripts`

`DFScripts` (in `ZADScriptsK`) is the **Spark-notebook-facing utility class** used in Zeppelin. It opens its own direct JDBC Neo4j connection and exposes query methods consumed by the Scala cells in notebooks like `GraphicZero-HigherDegreedP's`:

| Method | Cypher Pattern | Used by notebook |
|---|---|---|
| `s12QuadQ(low, high, maxN)` | `IndexedBy → VertexNode + TwoSeqFactor` at Dimension='2' | `QuerryS12QuadQuerry` object (cell 1) |
| `s3A(low, high, maxN, dim)` | `IndexedBy → VertexNode + Evaluate` — full scalar normalization | upstream notebooks |
| `s3T(...)` | `CollectedProductArgNode → ArgumentsNode` — aggregated terms | upstream notebooks |
| `s12Initial(...)` | Same as `ScriptsAutomation.s12Initial` but returns `ResultSet` | `DFScripts` standalone use |

---

## Pipeline Summary

```
TwoPolynomialGenerator.jar
        │
        │  generates polynomial term rows (n, MaxN, twoSeq, degree, scalar)
        ▼
   Kafka topic: "twoPoly"
        │
        │  (consumed by ZADScripts to populate Neo4j graph nodes)
        ▼
ZADScripts.jar
        │
        │  runs multi-step Cypher queries against Neo4j
        │  → s12Initial   → Kafka "s12"
        │  → s3TEc        → Kafka "s3t"
        │  → s3ADc        → Kafka "s3a"
        ▼
   Neo4j Graph
(CollectedProductArgNode, IndexedBy, VertexNode, etc.)
        │
        │  queried directly via JDBC in Zeppelin Spark notebooks
        ▼
   DFScripts.s12QuadQ(...)
        │
        ▼
   Spark DataFrame pipeline
   (GraphicZero-HigherDegreedP's notebook)
```

---

## Neo4j Database Preconditions

Both JARs and `DFScripts` connect to Neo4j using hardcoded values. Neo4j must be running and reachable before executing either JAR or launching the Zeppelin notebook.

| Parameter | Value | Source |
|---|---|---|
| **URL / Protocol** | `jdbc:neo4j:bolt://localhost` | `DFScripts.java` line 53, `ScriptsAutomation.java` line 56 |
| **Username** | `neo4j` | `DFScripts.java` line 82, `ScriptsAutomation.java` line 650 |
| **Password** | `ChiefQuippy` | `DFScripts.java` line 82, `ScriptsAutomation.java` line 650 |
| **Database name** | `graph.db` | `DFScripts.java` line 81 (set in props, not used in bolt URL) |
| **JDBC Driver** | `org.neo4j.jdbc.Driver` (v4.0.0) | `DFScripts.java` line 95, `ScriptsAutomation.java` line 663 |
| **Bolt port** | `7687` (Neo4j bolt default) | implied by `bolt://localhost` |

### Connection code (both classes identical)

```java
private final String protocol = "jdbc:neo4j:bolt://localhost";

conn = DriverManager.getConnection(protocol, "neo4j", "ChiefQuippy");
conn.setAutoCommit(false);
```

### Kafka Preconditions

| Parameter | Value |
|---|---|
| **Bootstrap server** | `localhost:9092` |
| **Zookeeper port** | `localhost:2181` |
| **Kafka install path** | `C:\kafka` (shortened from `C:\Users\tomas\kafka\kafka_2.12-2.5.0\kafka_2.12-2.5.0` to avoid Windows CMD line-length limit) |
| **Kafka version** | `kafka_2.12-2.5.0` (Kafka 2.5.0, Scala 2.12). Upgrade to 3.4.1+ required for Neo4j Connector for Kafka. |
| **Topics** | `twoPoly`, `s12`, `s3t`, `s3a` |
| **Serializer** | `StringSerializer` (both key and value) |

### Kafka Startup — External Commands Required

**The Java code does NOT manage or embed a Kafka instance.** It only creates a `KafkaProducer` client. Kafka and Zookeeper must be started externally in separate terminal sessions before running either JAR. If Kafka is not running, the JARs will loop with repeated warnings:

```
WARN [Producer clientId=producer-1] Connection to node -1 (localhost/127.0.0.1:9092)
     could not be established. Broker may not be available.
```

**Startup sequence (Windows — two separate CMD windows):**

**Step 1 — Start Zookeeper** (CMD window 1):
```bat
cd C:\kafka\bin\windows
.\zookeeper-server-start.bat ..\..\config\zookeeper.properties
```

**Step 2 — Start Kafka broker** (CMD window 2):
```bat
cd C:\kafka\bin\windows
.\kafka-server-start.bat ..\..\config\server.properties
```

**Step 3 — Create topics (first-time setup only):**
```powershell
cd C:\Users\tomas\JavaProjects\Aibeceles\kafka-connect
.\create-topics.ps1 -KafkaHome "C:\kafka"
```

**Verify topics:**
```powershell
cd C:\kafka\bin\windows
.\kafka-topics.bat --list --bootstrap-server localhost:9092
```

> **Note:** Windows Defender may block Zookeeper and Neo4j network access on first run — allow access when prompted.

> **Note:** If Kafka/Zookeeper logs become stale or corrupt, delete `C:\kafka\logs\` and `C:\kafka\zookeeper-data\` before restarting.

### Kafka Connect Neo4j Sink (replaces deprecated neo4j-streams plugin)

The legacy neo4j-streams plugin (configured in `neo4j.conf`) was EOL'd at Neo4j 4.4 and does not run on Neo4j 5.x. It has been replaced by the **Neo4j Connector for Kafka**, which runs as an external Kafka Connect worker process that writes to Neo4j over Bolt.

No Kafka-related settings belong in `neo4j.conf` on Neo4j 5.24.

Full setup instructions: [`kafka-connect/README.md`](../kafka-connect/README.md)

**Sink connector configs** (JSON files submitted to the Kafka Connect REST API):

| File | Topic | Producer |
|---|---|---|
| `kafka-connect/sink-twoPoly.json` | `twoPoly` | `TwoPolynomialGenerator.jar` |
| `kafka-connect/sink-s12.json` | `s12` | `ZADScripts.jar` |
| `kafka-connect/sink-s3t.json` | `s3t` | `ZADScripts.jar` |
| `kafka-connect/sink-s3a.json` | `s3a` | `ZADScripts.jar` |

These were migrated from the legacy `streams.sink.topic.cypher.*` entries originally in `ZADScriptsK/KafkaStreamsTopics.json` (Zeppelin notebook `2FT43SUAG`, dated 2020-12-24).

### What each topic sink writes to Neo4j

| Topic | Producer | Neo4j nodes/rels created |
|---|---|---|
| `twoPoly` | `TwoPolynomialGenerator.jar` | `IndexedBy`, `TwoSeqFactor`, `VertexNode`, `Evaluate`; rels `TwoFactor`, `IndexedByEvaluate`, `VertexIndexedBy` |
| `s12` | `ZADScripts.jar` (`s12Initial`, `s12Dc`) | `ArgumentsNode`, `CollectedProductArgNode`; rel `Arguments` |
| `s3t` | `ZADScripts.jar` (`s3TEc`) | `VertexNode`, `IndexedBy`, `Evaluate`; rels `IndexedByEvaluate`, `VertexIndexedBy` (with `twoSeq`/`divisor`) |
| `s3a` | `ZADScripts.jar` (`s3ADc`) | `VertexNode`, `IndexedByArgument`, `Evaluate`; rels `IndexedByEvaluate`, `VertexIndexedBy` (twoSeq fixed `"99"`) |

**Full startup order:**
```
1. Zookeeper
2. Kafka broker
3. Create topics                 (first time: .\create-topics.ps1 -KafkaHome "C:\kafka")
4. Neo4j 5.24                   (no Kafka settings in neo4j.conf)
5. Kafka Connect standalone      (hosts Neo4j sink connectors — see kafka-connect/README.md)
6. TwoPolynomialGenerator.jar   (produces to Kafka topic "twoPoly")
7. ZADScripts.jar               (produces to Kafka topics s12, s3t, s3a)
8. Zeppelin notebook            (queries Neo4j via DFScripts JDBC)
```

### APOC Plugin Required

The Cypher queries in `DFScripts` and `ScriptsAutomation` use Neo4j APOC procedures for exact-precision arithmetic:

```
apoc.number.exact.mul(...)
apoc.number.exact.div(...)
apoc.number.exact.add(...)
```

APOC must be installed in the Neo4j instance before any query will succeed.
