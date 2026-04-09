 # ModernGraphicZero

Graph-based exploration of polynomial differences, integer-rational bijections, and graph machine learning.

[Algorithm Explanation (YouTube)](https://www.youtube.com/watch?v=H4dBkofVA4A)

## Overview

This project explores the intersection of three mathematical domains through computational graph generation and machine learning:

1. **Newton's Forward Differences** -- Forward difference operations on polynomials to analyze rational roots; factorial-scaled levels obey the linear Diophantine structure described in [documentation/README_ZerosAndDifferences.md](documentation/README_ZerosAndDifferences.md) (see also [documentation/formal/03_implementation.md](documentation/formal/03_implementation.md) Part 4), which underpins integer closure in the construction
2. **Set-Theoretic Bijections** -- A novel approach to establishing integer-rational (Z <-> Q) mappings via binary-encoded root patterns
3. **Graph Machine Learning** -- Node classification, link prediction, and rational partitioning on the resulting polynomial difference graph

The core algorithm evaluates polynomials over integer ranges, computes successive forward differences, and persists the results as a graph in Neo4j. Each node represents a polynomial at some difference level; edges encode the difference operation. Machine learning then extracts latent structure from this graph -- predicting polynomial determinedness, discovering integer orderings, and partitioning nodes by rational equivalence classes.

### Central Claim

Well-bounded graph generation constraints produce a graph containing the complete set of **determined polynomials** (those whose integer roots are fully captured within the evaluation range), corresponding to a power set structure. Root positions recorded in graph properties such as `muList` encode as binary patterns that map to integers.

The **machine learning** layer in `ml/` then surfaces what that structure implies without explicit symbolic algebra. **Node classification** (see [ml/graph_label_prediction/](ml/graph_label_prediction/)) learns determined versus underdetermined polynomials from graph and coefficient features (e.g. ~79–83% F1 in the documented GraphSAGE setup). **Two-stage link prediction** (see [ml/graph_link_prediction/](ml/graph_link_prediction/)) first partitions nodes by rational class via `:SAME_DENOMINATOR` (Stage 1, μ / set-union ratio), then recovers consecutive integer order within each class via `:NEXT_INTEGER` (Stage 2). Full pipeline rationale and task design: [documentation/why_ml.md](documentation/why_ml.md); setup and APIs: [ml/README.md](ml/README.md) and [ml/graph_link_prediction/README.md](ml/graph_link_prediction/README.md).

## Project Structure

```
ModernGraphicZero/
├── documentation/             Theory, formal notes, graph structure docs
│   ├── formal/                Formal write-up (introduction through reference)
│   ├── abstract.md            Mathematical foundations and bijection theory
│   ├── the_graph.md           Graph structure explanation with examples
│   ├── why_ml.md              ML task rationale and full pipeline design
│   ├── README.md              GraphMLDifferences notebook documentation
│   └── ...                    Algorithm construction, graph theory, etc.
│
├── ZerosAndDifferences033021/ Java: polynomial difference computation engine
│   ├── src/                   Multi-threaded polynomial analysis
│   └── build.xml              Ant build configuration
│
├── ZADScriptsK/               Java: Neo4j Cypher query utilities + Kafka producers
│   └── ZADScripts/            Spark notebook scripts, automation, CSV export
│
├── TwoPolynomialGeneratorK/   Java: two-polynomial generator (Kafka pipeline)
│
├── kafka-connect/             Kafka Connect sink connector configs for Neo4j 5.x
│
├── ml/                        Python ML pipelines (PyTorch Geometric)
│   ├── graph_label_prediction/   Node classification (determined vs undetermined)
│   ├── graph_link_prediction/    Link prediction (integer ordering + rational partitioning)
│   ├── spark_graph_builder/      Spark-based graph construction pipeline
│   ├── neo4j/                    Neo4j Python client
│   └── requirements.txt
│
└── .gitignore
```

## Machine Learning Tasks

Three complementary ML tasks operate on the polynomial difference graph:

**Node Classification** -- Predict whether a polynomial is *determined* (all expected integer roots found) or *underdetermined*. Achieves ~82.9% training F1 and ~79.2% test F1 using GraphSAGE embeddings with logistic regression.

**Link Prediction -- Integer Ordering** -- Determined polynomials map to integers via binary encoding of root positions. Link prediction discovers `:NEXT_INTEGER` edges connecting nodes whose binary encodings represent consecutive integers.

**Link Prediction -- Rational Partitioning** -- Determined polynomials also map to rationals via the Set Union Ratio (mu). Link prediction partitions nodes into equivalence classes sharing the same rational value, predicting `:SAME_DENOMINATOR` edges.

### Python ML Module

The `ml/` directory contains PyTorch Geometric implementations:

- **graph_label_prediction/** -- MLP, GCN, and GraphSAGE models with grid search, k-fold cross-validation, and automatic prediction write-back to Neo4j
- **graph_link_prediction/** -- Two-stage pipeline: Stage 1 partitions by rational value (classifier, pairwise, or contrastive approaches); Stage 2 orders by integer value (regressor or pairwise approaches)

See [ml/README.md](ml/README.md) for setup instructions and [ml/graph_link_prediction/README.md](ml/graph_link_prediction/README.md) for the link prediction module.

## Java Components

### ZerosAndDifferences

The core computational engine. For a polynomial P(x) of degree n, the forward difference operator is applied iteratively:

```
Psi_0 = P(x)             degree n
Psi_1 = Delta P(x)       degree n-1
...
Psi_n = Delta^n P(x)     degree 0 (constant = n! * leading coefficient)
```

**Newton interpolation** (divided differences on equally spaced integer samples, exact `BigDecimal` arithmetic in `NewtonInterpolator`) recovers monomial coefficients from the difference table and writes them as `vmResult` on each node. This is the primary path in `LoopsDriver`, `LoopListener`, and related drivers. Some auxiliary or legacy database paths still use Vandermonde matrices with `GaussMain`. Results are persisted to Neo4j as `:Dnode` nodes connected by `:zMap` relationships.

See [documentation/README_ZerosAndDifferences.md](documentation/README_ZerosAndDifferences.md) for full documentation.

### ZADScripts

Cypher query utilities and Kafka producers for the polynomial pipeline. Operates against Neo4j via JDBC, supports batch automation and CSV export.

### Kafka Connect

Neo4j sink connector configurations replacing the deprecated neo4j-streams plugin (EOL at Neo4j 4.4). Targets Neo4j 5.24.0 Enterprise with Kafka Connect architecture. See [kafka-connect/README.md](kafka-connect/README.md).

## Graph Structure

```
(:Dnode)-[:zMap]->(:Dnode)
(:Dnode)-[:CreatedBye]->(:CreatedBy)
```

| Node Property | Description |
|---------------|-------------|
| `vmResult` | Monomial coefficients (exact Newton interpolation of sampled values) |
| `muList` | Root positions (binary encoding source) |
| `n`, `d` | Numerator/denominator of rational mu |
| `totalZero` | Count of integer roots found |
| `determined` | 1 if all expected roots captured, 0 otherwise |

See [documentation/the_graph.md](documentation/the_graph.md) for detailed structure with examples and [documentation/abstract.md](documentation/abstract.md) for the full mathematical foundation.

## Documentation

| Document | Content |
|----------|---------|
| [documentation/abstract.md](documentation/abstract.md) | Mathematical foundations, bijection theory, power set correspondence |
| [documentation/the_graph.md](documentation/the_graph.md) | Graph structure, node properties, mu encoding, examples |
| [documentation/why_ml.md](documentation/why_ml.md) | ML task rationale, training approaches, speculative arithmetic |
| [documentation/README.md](documentation/README.md) | GDS notebook documentation (node classification + link prediction) |
| [documentation/README_ZerosAndDifferences.md](documentation/README_ZerosAndDifferences.md) | Java application documentation |
| [documentation/formal/](documentation/formal/) | Formal write-up: introduction, theory, implementation, classification, ML, references |
| [documentation/algorithm_construction.md](documentation/algorithm_construction.md) | How polynomials are constructed with root constraints |
| [documentation/graph_theory.md](documentation/graph_theory.md) | Theoretical background for the graph structure |

## Requirements

### Java
- Java JDK 8+
- Apache Ant (or NetBeans IDE)
- Neo4j 4.x+ with JDBC driver
- Apache Derby (embedded, for local development)

### Python
- Python 3.9+
- PyTorch + PyTorch Geometric
- Neo4j Python driver
- See [ml/requirements.txt](ml/requirements.txt) for full dependency list

### Infrastructure
- Neo4j 5.24.0 Enterprise (graph database)
- Apache Kafka 3.4.1+ (streaming pipeline)
- Kafka Connect with Neo4j Connector 5.3.0

## Configuration

Database credentials are loaded from `db.properties` files (Java) and `.env` files (Python). These are git-ignored. Copy the `.example` templates and fill in your values:

```bash
# Java projects
cp ZADScriptsK/ZADScripts/db.properties.example ZADScriptsK/ZADScripts/db.properties
cp ZerosAndDifferences033021/db.properties.example ZerosAndDifferences033021/db.properties

# Python ML
cp ml/.env.example ml/.env   # if template exists, otherwise create manually
```

See [ml/README.md](ml/README.md) for Python environment setup.

## License

This project is provided as-is for educational and research purposes.

## Author

Aibes (Aibeceles)
