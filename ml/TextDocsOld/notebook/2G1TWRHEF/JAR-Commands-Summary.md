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
CREATE INDEX ON :IndexedBy(N,MaxN)
CREATE INDEX ON :Evaluate(Value)
CREATE INDEX ON :TwoSeqFactor(twoSeq)
CREATE INDEX ON :CollectedProductArgNode(MaxN,N)
CREATE INDEX ON :IndexedByArgument(N,MaxN)
CREATE INDEX ON :CreatedBy(resultId)
CREATE INDEX ON :Dnode(vmResult)
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
| **Kafka install path** | `C:\Users\tomas\kafka\kafka_2.12-2.5.0\kafka_2.12-2.5.0` |
| **Kafka version** | `kafka_2.12-2.5.0` (Kafka 2.5.0, Scala 2.12) |
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
cd C:\Users\tomas\kafka\kafka_2.12-2.5.0\kafka_2.12-2.5.0\bin\windows
zookeeper-server-start.bat ..\..\config\zookeeper.properties
```

**Step 2 — Start Kafka broker** (CMD window 2):
```bat
cd kafka_2.13-2.5.0\bin\windows
kafka-server-start.bat ..\..\config\server.properties
```

**Step 3 — Create topics (first-time setup only):**
```bat
cd C:\Users\tomas\kafka\kafka_2.12-2.5.0\kafka_2.12-2.5.0\bin\windows
kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic twoPoly
kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic s12
kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic s3t
kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic s3a
```

**Verify topics:**
```bat
kafka-topics.bat --list --bootstrap-server localhost:9092
```

> **Note:** Topics must be created from a regular CMD window. Running `kafka-topics.bat` from inside Zeppelin's `%sh` interpreter fails with a Scala version conflict (`NoSuchMethodError`).

> **Note:** Windows Defender may block Zookeeper and Neo4j network access on first run — allow access when prompted.

> **Note:** If Kafka/Zookeeper logs become stale or corrupt, delete `kafka_2.12-2.5.0\logs\` and `kafka_2.12-2.5.0\zookeeper-data\` before restarting.

### Neo4j Streams Sink — `neo4j.conf` Settings

Source: `ZADScriptsK/KafkaStreamsTopics.json` (Zeppelin notebook "Streams.Sink.Topic.Cypher", id `2FT43SUAG`)

These settings must be present in `neo4j.conf` for the neo4j-streams plugin to connect to Kafka and consume the topics. **Neo4j will not start** if these are configured but Kafka is not running.

```properties
kafka.zookeeper.connect=localhost:2181
kafka.bootstrap.servers=localhost:9092
streams.procedures.enabled=true
streams.sink.enabled=true

streams.sink.topic.cypher.twoPoly=MERGE (i:IndexedBy {N:event.NN,RowCounter:event.flatFileRowCounterr,MaxN:event.nMaxx,Dimension:"2"} ) \
MERGE (t:TwoSeqFactor {twoSeq:event.tSeqDB} ) \
MERGE (v:VertexNode {Vertex:event.vertexDBVertex,Scalar:event.vertexScalarDB,Degree:event.vertexDegreeDB} ) \
MERGE (e:Evaluate {Value:event.targetEvaluate}) \
MERGE (i)-[ee:TwoFactor]->(t) \
MERGE (i)-[:IndexedByEvaluate]->(e) \
MERGE (i)-[:VertexIndexedBy]->(v)

streams.sink.topic.cypher.s12=CREATE (aN:ArgumentsNode { TDegree:event.TTDegree, TScalar:event.TTScalar, TDivisor:event.vDivisor, ADivisor:event.vvDivisor,ATwoSeq:event.vvTwoSeq,ADegree:event.AADegree,AScalar:event.AAScalar,DegreeSum:event.degreeSum,ScalarProduct:event.scalarProduct,ResultDivisor:event.resultDivisor }) \
MERGE (cl:CollectedProductArgNode {Dimension:event.writeDimension,MaxN:event.iM,N:event.iN,Divisor:event.pD,afe:event.p2Eval,tft:toInteger(event.vTwoSeq)}) \
MERGE (cl)-[:Arguments]->(aN)

streams.sink.topic.cypher.s3t=CREATE (v:VertexNode {Scalar:event.tScalar,Degree:toString(event.tDegree)} ) \
MERGE (i:IndexedBy {N:event.iN,RowCounter:event.eTrm,MaxN:event.iM,Dimension:event.dimension} ) \
MERGE (e:Evaluate {Value:toString(toInteger(2^toInteger(event.iN)))}) \
MERGE (i)-[:IndexedByEvaluate]->(e) \
MERGE (i)-[eee:VertexIndexedBy {twoSeq:event.eTrm, divisor:event.pTD}]->(v)

streams.sink.topic.cypher.s3a=CREATE (v:VertexNode {Scalar:event.tScalar,Degree:toString(event.tDegree)}) \
MERGE (e:Evaluate {Value:toString(toInteger(2^toInteger(event.iN)))}) \
MERGE (i:IndexedByArgument {N:event.iN,MaxN:event.iM,Dimension:event.dimension} ) \
MERGE (i)-[:IndexedByEvaluate]->(e) \
MERGE (i)-[eee:VertexIndexedBy {twoSeq:"99", divisor:event.pTD}]->(v)
```

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
3. (Create topics — first time only)
4. Neo4j  (neo4j-streams connects to Kafka at startup)
5. TwoPolynomialGenerator.jar  (→ Kafka twoPoly → neo4j-streams → graph)
6. ZADScripts.jar              (queries Neo4j → Kafka s12/s3t/s3a → neo4j-streams → graph)
7. Zeppelin notebook           (queries Neo4j via DFScripts JDBC)
```

### APOC Plugin Required

The Cypher queries in `DFScripts` and `ScriptsAutomation` use Neo4j APOC procedures for exact-precision arithmetic:

```
apoc.number.exact.mul(...)
apoc.number.exact.div(...)
apoc.number.exact.add(...)
```

APOC must be installed in the Neo4j instance before any query will succeed.
