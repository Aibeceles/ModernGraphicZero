# ZerosAndDifferences

A Java application for analyzing polynomial differences and generating graph data for machine learning applications in Neo4j.

## Overview

ZerosAndDifferences is a multi-threaded Java application that computes **iterated differences of polynomials** to analyze their rational roots and determine their classification. The output graph data is used for machine learning tasks in the [GraphMLDifferences](https://github.com/Aibeceles/GraphMLDifferences) project.

📺 **Algorithm Explanation**: [YouTube Video - Zeros and Differences](https://www.youtube.com/watch?v=H4dBkofVA4A)

## Mathematical Background

### The Difference of Scalars Polynomial Tree (DOSPT)

The core algorithm implements Newton's forward difference method applied to polynomials. Key concepts include:

| Term | Definition |
|------|------------|
| **Psi (Ψ)** | A polynomial of degree n being analyzed |
| **TriagTriag** | Triangular difference tables - iterated differences organized by level |
| **DOSPT** | Difference of Scalars Polynomial Tree - the hierarchical structure of factored differences |
| **FDOSPT** | Factored DOSPT - differences scaled by factorial divisors |

### Algorithm Flow

1. **Polynomial Assertion**: Assert an arbitrary polynomial Ψ of dimension n
2. **Sequence Generation**: Generate the evaluation sequence Ψ(0), Ψ(1), Ψ(2), ...
3. **Difference Iteration**: Compute successive differences until a constant is reached
4. **Factored Differences**: Factor each difference level by its corresponding factorial (1!, 2!, 3!, ...)
5. **Gaussian Elimination**: Solve Vandermonde matrices to name the representative polynomials
6. **Graph Construction**: Build the DOSPT as a graph structure for analysis

### Key Mathematical Properties

- **Determined Polynomials**: A polynomial is "determined" when the number of rational roots equals its degree
- **Underdetermined Polynomials**: Fewer rational roots than expected by degree
- **Diophantine Equations**: The relationship between consecutive triag levels forms linear Diophantine equations: `ax! + bx! = c(x+1)!`

## Project Structure

```
ZerosAndDifferences033021/
├── src/
│   ├── MainClass/              # Entry points and drivers
│   │   ├── LoopsMain.java      # Main application entry
│   │   ├── LoopsDriver.java    # Primary execution driver
│   │   ├── LoopsDriverTwoP.java
│   │   └── GaussTable1.java    # Database output handler
│   │
│   ├── fractionintegerset/     # Core FIS algorithm
│   │   ├── FractionIntegerSet.java
│   │   ├── FractionIntegerDriver.java
│   │   ├── ResultListBean.java
│   │   ├── ProductBean.java
│   │   └── ...
│   │
│   ├── LoopLists/              # Data structures for differences
│   │   ├── LoopList.java       # Difference sequence container
│   │   ├── LoopListener.java   # Property change listener
│   │   ├── MatrixA.java        # Vandermonde matrix wrapper
│   │   └── GaussBean1.java     # Database record bean
│   │
│   ├── LoopsLogic/             # Semaphore pattern logic
│   │   ├── LoopsLogicLoopSemaphore.java
│   │   ├── LoopsLogicLoopCondition.java
│   │   └── ModuloList.java
│   │
│   ├── LoopsSemaphorePattern/  # Concurrent execution patterns
│   │   ├── LoopSemaphore.java
│   │   ├── LoopSemaphoreInitial.java
│   │   ├── LoopSemaphoreLast.java
│   │   └── LoopsSemaphoreInterface.java
│   │
│   ├── PArrayReset/            # Polynomial array management
│   │   ├── PolynomialArray.java
│   │   ├── PArrayResetListener.java
│   │   └── ResultListID.java
│   │
│   ├── mugauss/                # Gaussian elimination
│   │   └── GaussMain.java
│   │
│   ├── mucorrolationthreaded/  # Database correlation threads
│   │   ├── GaussCorrolation.java
│   │   ├── GaussTable.java
│   │   └── twoTableDBThread.java
│   │
│   ├── twopolynomial/          # Two-polynomial analysis
│   │   ├── plvManager.java
│   │   ├── vertexVector.java
│   │   └── zaddbTable1.java
│   │
│   ├── SqlErata/               # Database utilities
│   │   └── newDB.java
│   │
│   └── IntegerPoly/            # Integer polynomial utilities
│       └── GenerateBinaryLoopLogic.java
│
├── dist/                       # Compiled JAR and dependencies
│   ├── ZerosAndDifferences.jar
│   └── lib/
│       ├── derby.jar
│       ├── derbytools.jar
│       ├── mysql-connector-java-5.1.42.jar
│       └── neo4j-jdbc-driver-4.0.0.jar
│
├── muTableDB/                  # Embedded Derby database
├── build.xml                   # Ant build configuration
└── differencessNotes.txt       # Development notes and theory
```

## Key Components

### LoopsDriver
The main execution driver that:
- Initializes polynomial arrays and result lists
- Creates worker threads using `ExecutorService`
- Manages semaphore-based synchronization patterns
- Coordinates Gaussian elimination for polynomial identification

### LoopList
Represents a difference sequence with:
- `rListB` - Result list containing computed differences
- `MatrixA` - Vandermonde matrix for Gaussian elimination
- Property change support for event-driven updates

### Semaphore Pattern
Implements a producer-consumer pattern for iterating through polynomial coefficients:
- `LoopSemaphoreInitial` - Starting worker
- `LoopSemaphore` - Intermediate workers
- `LoopSemaphoreLast` - Terminal worker with completion logic

### GaussMain
Solves Vandermonde systems using Gaussian elimination to identify representative polynomials from sampled values.

## Configuration

### Parameters in LoopsDriver

```java
int dimension = 3;        // Polynomial degree
int integerRange = 20;    // Evaluation range (0 to integerRange-1)
int setProductRange = 20; // Iteration range for coefficient exploration
```

## Database Support

The application supports multiple database backends:

| Database | Usage |
|----------|-------|
| **Apache Derby** | Embedded database for local development (`muTableDB/`) |
| **MySQL** | Production database for larger datasets |
| **Neo4j** | Graph database export via JDBC driver |

### MySQL Configuration
- Database: `fisdb`
- Table: `gaussiterationtable`
- Columns: `parray`, `looplist`, `vmresult`, `resultlistid`

## Building and Running

### Prerequisites
- Java JDK 8+
- Apache Ant (or NetBeans IDE)

### Build
```bash
cd ZerosAndDifferences033021
ant clean build
```

### Run
```bash
cd dist
java -jar "ZerosAndDifferences.jar"
```

### Dependencies
The following JARs are required (included in `dist/lib/`):
- `derby.jar` - Apache Derby embedded database
- `derbytools.jar` - Derby utilities
- `mysql-connector-java-5.1.42.jar` - MySQL JDBC driver
- `neo4j-jdbc-driver-4.0.0.jar` - Neo4j JDBC driver

## Output

### Console Output
The application outputs:
- Polynomial array states (`pArray`)
- Computed differences (`rListB`)
- Gaussian elimination results
- Thread execution status

### Database Output
Each Gaussian result is stored as a `GaussBean1` containing:
- `pArray` - Zero-position signature: `[r₀, r₁, r₂, ...]` where `rᵢ` is the x-position where difference level `wNum=i` equals zero (stored on `CreatedBy` nodes)
- `loopList` - The difference sequence
- `vmResult` - Vandermonde matrix solution: polynomial coefficients `[a₀, a₁, ...]` (stored on `Dnode` nodes)
- `resultListId` - Correlation identifier for related sequences

> **Note:** In the Java code, `pArray` initially holds polynomial coefficients for evaluation. However, when written to the database as a `CreatedBy` node property, `pArray` represents the zero-position signature that uniquely identifies which x-values produce zeros at each difference level. The actual polynomial coefficients are stored in `vmResult` on `Dnode` nodes.

## Integration with GraphMLDifferences

The output from ZerosAndDifferences populates the Neo4j graph database (`neo4j1.dump`) used in the GraphMLDifferences project:

1. **Node Generation**: Each polynomial difference becomes a `Dnode` with properties
2. **Relationship Mapping**: `zMap` relationships connect related polynomials
3. **Property Assignment**: `determined`, `zero`, `one`, `two`, `three`, `wNum`, `totalZero` properties are derived from the difference analysis

## Development Notes

Extensive development notes are available in `differencessNotes.txt`, covering:
- Mathematical theory of iterated differences
- Diophantine equation derivations
- DOSPT construction algorithms
- Semaphore pattern design decisions
- Database schema evolution

## Related Resources

- **FIDFormulas.odt** - Formal documentation of TriagTriag equations
- **SubDiffOfScalarsExample.ods** - Spreadsheet examples of difference calculations
- **FidFig.odg** - Diagrams of DOSPT structures
- **differences.ods** - Working spreadsheet for algorithm development

## License

This project is provided as-is for educational and research purposes.

## Author

Aibes (Aibeceles)

