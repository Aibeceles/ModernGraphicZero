# Document Organization: Zeros, Differences, and Bijections

## Overview

This project explores the intersection of **polynomial difference theory**, **set-theoretic bijections (ℤ ↔ ℚ)**, and **graph machine learning**. The documentation spans multiple file formats, each serving a distinct purpose in the research-to-production pipeline.

---

## Document Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DOCUMENT ECOSYSTEM                                 │
│                                                                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                   │
│  │   THEORY    │────►│   DATA &    │────►│ PRODUCTION  │                   │
│  │   (PDF/HTML)│     │ EXPLORATION │     │   (JSON)    │                   │
│  └─────────────┘     │   (ODS/MD)  │     └─────────────┘                   │
│                      └─────────────┘                                        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     DOCUMENTATION (MD)                               │   │
│  │   Connects theory ↔ implementation ↔ ML tasks ↔ release plans       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## File Categories

### 1. Core Theory Documents

| File | Format | Purpose | Status |
|------|--------|---------|--------|
| `A Set-Theoretic Approach to Establishing Bijections...pdf` | PDF | Formal mathematical paper on ℤ ↔ ℚ bijection | Published/Reference |
| `html-with-appendix-and-toc.html` | HTML | Interactive version with appendix, table of contents | Reference |

**Usage:** These are the foundational mathematical references. All other documents build upon the definitions (μ, χ, set collections) established here.

---

### 2. Interactive Data & Exploration

| File | Format | Purpose | Status |
|------|--------|---------|--------|
| `GraphMLDifferences/DifferencePolynomials.ods` | ODS | Polynomial difference calculations, TriagTriag tables | Working |
| `SubDiffOfScalarsExample2.ods` | ODS | Scalar Difference Polynomial examples | Working |
| `graphsample.json` | JSON | Exported Neo4j graph data for analysis | Data Export |

**Usage:** 
- **ODS spreadsheets**: Hands-on verification of mathematical relationships (factorial scalars, difference propagation, zero patterns)
- **JSON export**: Bridge between Neo4j database and external analysis tools

---

### 3. Machine Learning Notebooks

| File | Format | Purpose | Status |
|------|--------|---------|--------|
| `GraphMLDifferences/GraphicLablePrediction.json` | JSON (Zeppelin) | Node classification: predict `determined` property | Implemented |
| `GraphMLDifferences/GraphicLinkPrediction.json` | JSON (Zeppelin) | Link prediction: predict `:zMap` relationships | Implemented |

**Usage:** Apache Zeppelin notebooks executable against Neo4j with GDS library. These implement the ML pipeline described in the documentation.

---

### 4. Documentation Files (Markdown)

#### Core Documentation Suite

| File | Purpose | Audience |
|------|---------|----------|
| `GraphMLDifferences/abstract.md` | Executive summary of entire project | New readers, overview |
| `GraphMLDifferences/README.md` | ML notebooks documentation & setup | Developers, ML practitioners |
| `GraphMLDifferences/README_ZerosAndDifferences.md` | Java application documentation | Developers, maintainers |

#### Technical Deep-Dives

| File | Purpose | Audience |
|------|---------|----------|
| `GraphMLDifferences/graph_structure.md` | Neo4j schema, node/relationship types, width analysis | Database administrators, developers |
| `GraphMLDifferences/graph_theory.md` | Mathematical theory, bijection connection, ML task formulation | Researchers, mathematicians |
| `GraphMLDifferences/the_graph.md` | Understanding `graphsample.json` export format | Data analysts |
| `GraphMLDifferences/math_verification.md` | Worked examples validating algorithms | Quality assurance, verification |

#### Future/Planning Documents

| File | Purpose | Audience |
|------|---------|----------|
| `GraphMLDifferences/why_ml.md` | ML task rationale, use cases, speculative arithmetic | Stakeholders, researchers |
| `GraphMLDifferences/hf.md` | Hugging Face release checklist & model card planning | Release managers |
| `GraphMLDifferences/ip.md` | Intellectual property considerations | Legal, business |

---

## Reading Order (Recommended)

### For New Readers
```
1. abstract.md                    ← Start here: project overview
2. html-with-appendix-and-toc.html ← Mathematical foundation (Definitions 2.1-2.3)
3. README_ZerosAndDifferences.md  ← Understand the Java algorithm
4. README.md                      ← ML pipeline overview
```

### For Implementers
```
1. graph_structure.md             ← Neo4j schema details
2. the_graph.md                   ← JSON export format
3. math_verification.md           ← Algorithm validation
4. GraphicLablePrediction.json    ← Node classification notebook
5. GraphicLinkPrediction.json     ← Link prediction notebook
```

### For Researchers
```
1. abstract.md                    ← Central claims and overview
2. graph_theory.md                ← Theoretical framework
3. why_ml.md                      ← ML task justification
4. math_verification.md           ← Proofs and examples
```

### For Production/Release
```
1. hf.md                          ← Hugging Face release checklist
2. ip.md                          ← IP considerations
3. README.md                      ← Public-facing documentation
```

---

## Document Dependencies

```
                    ┌──────────────────────────┐
                    │ PDF / HTML (Theory)      │
                    │ - Definitions (μ, χ)     │
                    │ - Bijection proofs       │
                    └────────────┬─────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            ▼                    ▼                    ▼
   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │ abstract.md     │  │ graph_theory.md │  │ math_verif.md   │
   │ (Overview)      │  │ (Theory→Graph)  │  │ (Examples)      │
   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
            │                    │                    │
            └────────────────────┼────────────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            ▼                    ▼                    ▼
   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │ graph_struct.md │  │ the_graph.md    │  │ README_ZAD.md   │
   │ (Schema)        │  │ (JSON Format)   │  │ (Java App)      │
   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
            │                    │                    │
            └────────────────────┼────────────────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │ README.md (ML Notebooks) │
                    │ - Setup instructions     │
                    │ - Results summary        │
                    └────────────┬─────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            ▼                    ▼                    ▼
   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │ why_ml.md       │  │ hf.md           │  │ ip.md           │
   │ (Justification) │  │ (Release Plan)  │  │ (IP Notes)      │
   └─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## Interactive Document Flow

### Spreadsheet ↔ Documentation Connections

| ODS File | Related MD Documentation | Connection |
|----------|-------------------------|------------|
| `DifferencePolynomials.ods` | `math_verification.md` | Examples match spreadsheet calculations |
| `SubDiffOfScalarsExample2.ods` | `graph_theory.md` | SDP patterns align with theory section |

### JSON ↔ Database Connections

| JSON File | Source | Used By |
|-----------|--------|---------|
| `graphsample.json` | Neo4j export | `the_graph.md` (explains format), external analysis |
| `GraphicLablePrediction.json` | Zeppelin export | `README.md` (documents workflow) |
| `GraphicLinkPrediction.json` | Zeppelin export | `README.md` (documents workflow) |

---

## Production Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PRODUCTION PIPELINE                                  │
│                                                                              │
│  RESEARCH                        BUILD                       RELEASE        │
│  ───────                         ─────                       ───────        │
│                                                                              │
│  ┌─────────────┐                                                            │
│  │ PDF/HTML    │  Theory & Definitions                                      │
│  └──────┬──────┘                                                            │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │ ODS Files   │  Interactive verification                                  │
│  └──────┬──────┘                                                            │
│         ▼                                                                    │
│  ┌─────────────┐  ┌─────────────┐                                           │
│  │ Java App    │─►│ Neo4j DB    │  Graph generation                         │
│  │ (ZAD.jar)   │  │ (neo4j1.dump│                                           │
│  └─────────────┘  └──────┬──────┘                                           │
│                          ▼                                                   │
│                   ┌─────────────┐  ┌─────────────┐                          │
│                   │ Zeppelin    │─►│ Trained     │  ML training             │
│                   │ Notebooks   │  │ Models      │                          │
│                   └─────────────┘  └──────┬──────┘                          │
│                                           ▼                                  │
│                                    ┌─────────────┐                          │
│                                    │ HuggingFace │  Model release           │
│                                    │ Release     │  (see hf.md)             │
│                                    └─────────────┘                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## File Format Rationale

| Format | Purpose | Interactivity | Version Control |
|--------|---------|---------------|-----------------|
| **PDF** | Formal publication | Read-only | ❌ Poor (binary) |
| **HTML** | Rich presentation + navigation | High (TOC, links) | ⚠️ Medium |
| **ODS** | Calculations, formulas, exploration | Very High | ❌ Poor (binary) |
| **JSON** | Data exchange, ML notebooks | Machine-readable | ⚠️ Medium |
| **MD** | Documentation, explanations | Rendered + source | ✅ Excellent |

---

## Outstanding Tasks

### Documentation Gaps
- [ ] Add examples to `graph_theory.md` connecting μ/χ to specific graph nodes
- [ ] Create visualization diagrams for `the_graph.md`
- [ ] Add troubleshooting section to `README.md`

### Production Readiness
- [ ] Complete `hf.md` checklist items
- [ ] Generate model cards from training results
- [ ] Export final trained models

### Interactive Enhancements
- [ ] Consider Jupyter notebook versions of ODS calculations
- [ ] Add interactive graph visualization (D3.js or similar)

---

## Cross-Reference Index

### Key Concepts → Documents

| Concept | Primary Document | Supporting Documents |
|---------|------------------|---------------------|
| Newton's Differences | `abstract.md` | `math_verification.md`, `README_ZAD.md` |
| Set Union Ratio (μ) | `graph_theory.md` | `html-with-appendix-and-toc.html` |
| Binary Encoding (χ) | `graph_theory.md` | `the_graph.md` |
| Determined/Undetermined | `abstract.md` | `why_ml.md`, `graph_structure.md` |
| TriagTriag | `README_ZAD.md` | `DifferencePolynomials.ods` |
| Power Set Correspondence | `abstract.md` | `graph_theory.md` |
| Diophantine Connection | `graph_theory.md` | `differencessNotes.txt` |

### Node Properties → Documents

| Property | Defined In | Explained In |
|----------|------------|--------------|
| `vmResult` | `graph_structure.md` | `the_graph.md` |
| `muList` | `graph_structure.md` | `graph_theory.md`, `the_graph.md` |
| `n`, `d` | `graph_structure.md` | `graph_theory.md` |
| `determined` | `graph_structure.md` | `abstract.md`, `why_ml.md` |
| `totalZero` | `graph_structure.md` | `math_verification.md` |

---

## Maintenance Notes

### Update Triggers

| When... | Update... |
|---------|-----------|
| Theory paper revised | `abstract.md`, `graph_theory.md` |
| Java algorithm changes | `README_ZAD.md`, `math_verification.md` |
| Neo4j schema changes | `graph_structure.md`, `the_graph.md` |
| ML results improve | `README.md`, `hf.md` |
| New ML task added | `why_ml.md`, `graph_theory.md` |

### Document Ownership

| Document Group | Primary Maintainer Role |
|----------------|------------------------|
| Theory (PDF/HTML) | Researcher/Author |
| ODS Spreadsheets | Researcher |
| Java Documentation | Developer |
| ML Documentation | ML Engineer |
| Release Planning | Release Manager |

---

*Last updated: January 2026*

