# `ml/polys` Scala Spark MVP

`ml/polys` is the first production pipeline for the strongest workbook migration path.  
It runs the S12-style polynomial flow end-to-end and writes versioned batch artifacts.

## Prerequisites

The project jar is built for **Scala 2.12 / Spark 3.5.1 / Java 17**. Mismatched runtimes cause `NoClassDefFoundError: scala/Serializable` (Scala mismatch) or `UnsupportedOperationException: getSubject is not supported` (Java 21+ mismatch).

| Component | Required | Notes |
|---|---|---|
| Java | **17** (Temurin 17.0.x recommended) | JDK 21/24/25 remove APIs Hadoop 3.3.4 uses |
| Scala | **2.12.18** | set by `build.sbt` |
| Spark | **3.5.1** | PySpark 3.5.1 in venv is a convenient source |
| PySpark (optional) | `pip install pyspark==3.5.1` | provides a matching `spark-submit.cmd` |
| Hadoop `winutils` (Windows) | `C:\hadoop\bin\winutils.exe` | get from [cdarlint/winutils](https://github.com/cdarlint/winutils) Hadoop 3.3.x |
| Neo4j JDBC driver | `org.neo4j:neo4j-jdbc-driver:4.0.0` | passed via `--packages` at submit time |
| `pyarrow` (for notebook) | `pip install pyarrow` | needed by `ml/polys/workbook/quadratics.ipynb` |

### Verify your shell before submitting

```powershell
# Java should report 17.x
java -version

# Pick the venv spark-submit to avoid a mismatched global install
.venv\Lib\site-packages\pyspark\bin\spark-submit --version
# expected: version 3.5.1, Scala 2.12.x
```

### Environment variables required by spark-submit

```powershell
# JDK 17
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Hadoop for winutils.exe
$env:HADOOP_HOME = "C:\hadoop"
$env:PATH = "$env:HADOOP_HOME\bin;$env:PATH"
```

`[System.Environment]::SetEnvironmentVariable(...,"User")` persists these across future PowerShell sessions, but the **current** session still needs the `$env:` assignments above.

## What it does

Pipeline stages:
1. `extractS12` - load canonical term rows from file input or Neo4j query
2. `groupedDegreeSum` - aggregate row-scaled scalar contributions by `(maxN, index, degree)`
3. `rowScalarDegreePivot` - pivot view by `(N, rowScalar, degree)` for analysis parity
4. `aggregatedScalarDegreePivot` - pivot grouped totals by degree
5. `reducedResult` - evaluate index-level result terms via index-power scaling

Canonical extract schema used across all modes:
- `index`
- `maxN`
- `rowScalar`
- `divisor`
- `scalar`
- `degree`

## Build and test

```powershell
cd ml/polys
sbt test
```

If `sbt` is not installed or not in `PATH`, install it first or run from an environment where sbt is available.

## Build the job JAR

From `ml/polys`:

```powershell
cd ml/polys
sbt package
```

Output JAR (default name from `build.sbt`):

- `ml/polys/target/scala-2.12/polys_2.12-0.1.0.jar`

## Run (direct spark-submit)

Each `spark-submit` block below starts with **`cd` to the repository root** (the directory that contains `ml/polys/`), so paths like `ml/polys/target/...` and `ml/polys/artifacts` resolve correctly. Replace the `cd` path with your clone location.

If you prefer to run from `ml/polys` instead, `cd` there and use `target/scala-2.12/polys_2.12-0.1.0.jar` plus `--artifactsRoot=artifacts` (see wrapper section for the same layout).

### Windows + PySpark `spark-submit` (venv)

Spark’s `find-spark-home.cmd` defaults to `python3`, which may not see your venv. Point PySpark at the venv interpreter before `spark-submit`:

```powershell
$py = Join-Path $env:VIRTUAL_ENV "Scripts\python.exe"
$env:PYSPARK_PYTHON = $py
$env:PYSPARK_DRIVER_PYTHON = $py
```

Parquet / file mode:

```powershell
cd C:\path\to\Aibeceles
.venv\Lib\site-packages\pyspark\bin\spark-submit `
  --class polys.app.MainJob `
  ml/polys/target/scala-2.12/polys_2.12-0.1.0.jar `
  --pipelineName=strongestPath `
  --inputFormat=parquet `
  --inputPath=C:/path/to/s12_input `
  --artifactsRoot=ml/polys/artifacts
```

### Neo4j mode (direct extraction)

Neo4j mode bypasses file input and queries graph data directly.

**Current verified command (Windows, PySpark 3.5.1 from venv, run from repo root):**

```powershell
cd C:\path\to\Aibeceles

# Prereqs in this shell:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:PATH      = "$env:JAVA_HOME\bin;$env:PATH"
$env:HADOOP_HOME = "C:\hadoop"
$env:PATH        = "$env:HADOOP_HOME\bin;$env:PATH"

.venv\Lib\site-packages\pyspark\bin\spark-submit `
  --packages org.neo4j:neo4j-jdbc-driver:4.0.0 `
  --class polys.app.MainJob `
  ml/polys/target/scala-2.12/polys_2.12-0.1.0.jar `
  --pipelineName=strongestPath `
  --inputFormat=neo4j `
  --rangeLow=2 `
  --rangeHigh=7 `
  --maxN=8 `
  --dimension=2 `
  --artifactsRoot=ml/polys/artifacts
```

Notes:

- `--packages org.neo4j:neo4j-jdbc-driver:4.0.0` is required because `sbt package` does not bundle dependencies. On first run Spark downloads the jar to `~\.ivy2\cache` and reuses it afterwards.
- Using `.venv\Lib\site-packages\pyspark\bin\spark-submit` explicitly avoids picking up an incompatible `spark-submit` earlier in `PATH` (e.g. Spark 4.x/Scala 2.13 builds). Check with `Get-Command spark-submit -All`.
- If the driver fails on Hadoop filesystem APIs, set up `winutils` and `HADOOP_HOME` per [Hadoop on Windows](https://wiki.apache.org/hadoop/WindowsProblems). Passing `--conf "spark.driver.extraJavaOptions=-Dhadoop.home.dir=C:/hadoop"` also works; use forward slashes so PowerShell does not eat the backslash.

`inputFormat=neo4j` reads **`ml/polys/db.properties`** (repo root: `Aibeceles/ml/polys/db.properties`).  
If you run with CWD inside `ml/polys`, the same file is used as **`db.properties`** in that folder.

Resolution order:

1. JVM: `-Dpolys.db.properties=<absolute or relative path>`
2. `./ml/polys/db.properties` (typical when CWD is repo root)
3. `./db.properties` (typical when CWD is `ml/polys`)
4. Walk parent directories of `user.dir` until `ml/polys/db.properties` is found

Copy `db.properties.example` to `ml/polys/db.properties` and set your password.  
The repo `.gitignore` ignores `db.properties` so credentials are not committed.

Required keys:

- `neo4j.url`
- `neo4j.user`
- `neo4j.password`
- `neo4j.database` (use `tagtest` when that database is required)

Neo4j extraction args:
- `--rangeLow`
- `--rangeHigh`

- `--maxN`
- `--dimension`

## Run (Windows wrapper)

Use the included PowerShell wrapper to standardize arguments:

```powershell
cd ml/polys
.\run_polys.ps1 `
  -JarPath .\target\scala-2.12\polys_2.12-0.1.0.jar `
  -InputPath C:\path\to\s12_input `
  -InputFormat parquet `
  -PipelineName strongestPath `
  -ArtifactsRoot artifacts
```

Use `-ArtifactsRoot artifacts` when your current directory is `ml/polys`, so output lands in `ml/polys/artifacts` instead of a nested `ml/polys/ml/polys/artifacts` path.

From **repository root** (without `cd ml/polys`), use an absolute or repo-relative JAR path, for example:

```powershell
.\ml\polys\run_polys.ps1 `
  -JarPath .\ml\polys\target\scala-2.12\polys_2.12-0.1.0.jar `
  -InputPath C:\path\to\s12_input `
  -InputFormat parquet `
  -ArtifactsRoot ml\polys\artifacts
```

Neo4j mode with wrapper:

```powershell
cd ml/polys
.\run_polys.ps1 `
  -JarPath .\target\scala-2.12\polys_2.12-0.1.0.jar `
  -InputFormat neo4j `
  -RangeLow 2 `
  -RangeHigh 7 `
  -MaxN 8 `
  -Dimension 2 `
  -ArtifactsRoot artifacts
```

### Wrapper parameters

- `-JarPath` (required): path to compiled job jar (`target/scala-2.12/polys_2.12-0.1.0.jar` under `ml/polys` after `sbt package`, or `ml/polys/target/...` from repo root)
- `-InputPath` (required for file modes): path to input dataset
- `-InputFormat`: `parquet` (default), `csv`, `json`, or `neo4j`
- `-RangeLow`: Neo4j extraction low range (default `2`)
- `-RangeHigh`: Neo4j extraction high range (default `7`)
- `-MaxN`: Neo4j extraction MaxN (default `8`)
- `-Dimension`: Neo4j extraction dimension (default `2`)
- `-PipelineName`: defaults to `strongestPath`
- `-ArtifactsRoot`: defaults to `ml/polys/artifacts` (correct when you invoke from **repository root**; if you `cd ml/polys` first, pass `-ArtifactsRoot artifacts` so output is not nested under `ml/polys/ml/polys/artifacts`)
- `-RunId`: optional override; if omitted, app generates UTC run id
- `-SparkSubmit`: override command if `spark-submit` is not in PATH

### Example with explicit run id

```powershell
.\ml\polys\run_polys.ps1 `
  -JarPath .\ml\polys\target\scala-2.12\polys_2.12-0.1.0.jar `
  -InputPath C:\path\to\s12_input `
  -ArtifactsRoot ml\polys\artifacts `
  -RunId 20260420_101500
```

## Expected behavior

On successful run:
- all five stage outputs are written to Parquet
- run metrics are written to JSON
- `latest.txt` pointer is updated to this run id

On failed run:
- job exits non-zero
- `latest.txt` is not advanced
- partial stage outputs may exist for debugging

Typical console flow:
- prints run settings
- executes extract and transform stages
- writes artifacts and metrics
- prints successful completion or failure exit code

## Input contract (`extractS12`)

Required columns:
- `index`
- `maxN`
- `rowScalar`
- `divisor`
- `scalar`
- `degree`

All are read as strings and normalized during transforms.

## Artifact layout

- `ml/polys/artifacts/parquet/<pipelineName>/<runId>/<stageName>/`
- `ml/polys/artifacts/metrics/<pipelineName>/<runId>/run_metrics.json`
- `ml/polys/artifacts/reports/<pipelineName>/<runId>/` (optional)
- `ml/polys/artifacts/parquet/<pipelineName>/latest.txt` (active run pointer)

`runId` format: `yyyyMMdd_HHmmss` (UTC).

`latest.txt` is only updated after all stages and validations succeed.

Stage folders created per run:
- `extractS12`
- `groupedDegreeSum`
- `rowScalarDegreePivot`
- `aggregatedScalarDegreePivot`
- `reducedResult`

## Notebook artifact consumption (`.ipynb`)

A ready-to-use workbook is provided at [`ml/polys/workbook/quadratics.ipynb`](workbook/quadratics.ipynb). It reads `latest.txt`, loads all five stage outputs via `pyarrow.parquet` (avoiding the pandas Arrow extension registration bug), and prints row counts / heads for each stage. Requires `pip install pyarrow` in the venv.

Legacy-parity workbook migrations are also available:

- [`ml/polys/workbook/GraphicZero-TheQuadratics.ipynb`](workbook/GraphicZero-TheQuadratics.ipynb): Python migration of the Zeppelin quadratics narrative with Zeppelin-style checkpoint names.
- [`ml/polys/workbook/GraphicZero-HigherDegreedPs.ipynb`](workbook/GraphicZero-HigherDegreedPs.ipynb): Python migration of the Zeppelin higher-degree notebook (`2G1TWRHEF`) using live Neo4j source logic equivalent to `DFScripts.s12QuadQ`.

Formal analyses of the graph-to-polynomial pipeline that frame the migrated workbooks:

- [`ml/polys/GraphStructureToResultPoly.md`](GraphStructureToResultPoly.md): end-to-end semantic walk from `TwoPolynomialGenerator.jar` / `ZADScripts.jar` through the Neo4j graph to the reduced result polynomials (`p_N(MaxN) = 2^N` for dimension 2).
- [`documentation/MultiVariableNMGradientDescent.md`](../../documentation/MultiVariableNMGradientDescent.md): cell-by-cell formal analysis of the higher-p (dimension 8 "quartet") Zeppelin notebook (`2GXTHE9EN`), showing how two reduced polynomials are cross-joined into a bivariate surface and optimized via Newton's method and gradient descent. Explicit tie-back to the three migrated workbooks above.

For `GraphicZero-HigherDegreedPs.ipynb`:

- Install dependencies in the notebook environment: `pip install neo4j pandas`.
- Configure `ml/polys/db.properties` (copy from `ml/polys/db.properties.example`).
- Execute cells in order; the notebook asserts required intermediate checkpoints and final schema contracts.

Manual equivalent:

```python
from pathlib import Path
import pyarrow.parquet as pq

base = Path("ml/polys/artifacts/parquet/strongestPath")
run_id = (base / "latest.txt").read_text().strip()

grouped = pq.read_table(base / run_id / "groupedDegreeSum").to_pandas()
reduced = pq.read_table(base / run_id / "reducedResult").to_pandas()

display(grouped.head())
display(reduced.head())
```

If reproducibility is required, set `run_id` explicitly instead of reading `latest.txt`.

## Operational notes

- Neo4j mode reads credentials from properties; do not commit real passwords.
- File mode is useful for deterministic replay and offline validation.
- Use explicit `RunId` when you need reproducible backtesting.
