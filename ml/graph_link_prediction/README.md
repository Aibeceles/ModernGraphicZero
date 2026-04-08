# Graph Link Prediction Module

A complete PyTorch Geometric implementation of two-stage link prediction for discovering the integer-rational bijection in polynomial difference graphs.

## Overview

This module implements the pipeline described in [`GraphMLDifferences/GraphicLinkPrediction_specification.md`](../../GraphMLDifferences/GraphicLinkPrediction_specification.md):

### Two-Stage Pipeline

**Stage 1 (Task 1): SAME_DENOMINATOR**
- **Goal**: Partition nodes by their rational value μ = n/d
- **Approaches**:
  - **Classifier**: Multi-class classification on partition labels
  - **Pairwise**: Binary edge classifier
  - **Contrastive**: Learn embeddings via contrastive loss

**Stage 2 (Task 2): NEXT_INTEGER**
- **Goal**: Order nodes by consecutive integer values within partitions
- **Approaches**:
  - **Regressor**: Predict integer values, then order
  - **Pairwise**: Binary classifier for consecutive edges

## Installation

The module requires:
- PyTorch
- PyTorch Geometric
- NumPy
- scikit-learn
- scipy
- Neo4j Python driver

Install dependencies:
```bash
cd ml
pip install -r requirements.txt
```

## Quick Start

```python
from ml.graph_link_prediction import (
    load_graph_from_neo4j,
    TwoStageLinkPredictionPipeline,
)

# Load data
data_t1, data_t2, loader = load_graph_from_neo4j(
    uri="bolt://localhost:7687",
    user="neo4j",
    password="your_password",
    database="d4seed1"
)

# Run pipeline
pipeline = TwoStageLinkPredictionPipeline(data_t1, data_t2)
results = pipeline.run(
    task1_approach='classifier',
    task2_approach='regressor',
    verbose=True
)

print(f"Combined F1: {results['combined_f1']:.4f}")
```

## Module Structure

```
graph_link_prediction/
├── __init__.py              # Main exports
├── config.py                # Configuration and constants
├── data_loader.py           # Neo4j → PyTorch Geometric
├── task1_models.py          # SAME_DENOMINATOR models
├── task2_models.py          # NEXT_INTEGER models
├── task1_trainer.py         # Task 1 training
├── task2_trainer.py         # Task 2 training
├── pipeline.py              # Two-stage pipeline
├── evaluation.py            # Metrics and validation
├── predictor.py             # Inference and Neo4j write-back
├── run_pipeline.ipynb       # Demo notebook
└── README.md                # This file
```

## Usage Examples

### Compare All Approaches

```python
comparison = pipeline.compare_approaches(
    task1_approaches=['classifier', 'pairwise', 'contrastive'],
    task2_approaches=['regressor', 'pairwise'],
    verbose=True
)

best = comparison['best_combination']
print(f"Best: {best}")
```

### Train Individual Tasks

```python
from ml.graph_link_prediction import Task1Trainer, Task2Trainer

# Task 1
task1_trainer = Task1Trainer(data_task1)
model_t1, metrics_t1 = task1_trainer.train_classifier()

# Task 2
task2_trainer = Task2Trainer(data_task2)
model_t2, metrics_t2 = task2_trainer.train_regressor()
```

### Grid Search

```python
# Find best hyperparameters
grid_results = task1_trainer.grid_search(
    approach='classifier',
    penalties=[0.0625, 0.5, 1.0, 4.0],
    verbose=True
)

print(f"Best penalty: {grid_results['best_penalty']}")
```

### Write to Neo4j

```python
from ml.neo4j.neo4jClient import Neo4jClient
from ml.graph_link_prediction import LinkPredictor

client = Neo4jClient(uri, user, password)
predictor = LinkPredictor(client, database)

results = predictor.predict_and_write(
    task1_model=model_t1,
    task2_model=model_t2,
    data_task1=data_t1,
    data_task2=data_t2,
    write_same_denom=True,
    write_next_int=True,
    verbose=True
)
```

## Key Features

### Integer Computation from muList
```python
from ml.graph_link_prediction import muList_to_integer

# Binary encoding: [0, 2, 4] → 10101₂ → 21
int_value = muList_to_integer("[0, 2, 4]")
print(int_value)  # 21
```

### Bijection Validation
```python
from ml.graph_link_prediction import validate_bijection_properties

validation = validate_bijection_properties(
    same_denom_edges,
    next_int_edges,
    true_partition_ids,
    true_int_values
)

print(f"Valid bijection: {validation['all_valid']}")
```

### Comprehensive Evaluation
```python
from ml.graph_link_prediction import generate_evaluation_report

report = generate_evaluation_report(
    task1_metrics,
    task2_metrics,
    bijection_validation,
    task1_targets=TASK1_TARGETS,
    task2_targets=TASK2_TARGETS
)

print(report)
```

## Performance Targets

### Task 1 (SAME_DENOMINATOR)
- Partition Purity: > 0.95
- Adjusted Rand Index: > 0.90
- Link Precision/Recall: > 0.90

### Task 2 (NEXT_INTEGER)
- Sequence Accuracy: > 0.80
- Kendall's Tau: > 0.95
- Link Precision/Recall: > 0.85

### Combined Pipeline
- End-to-end F1: > 0.85 for both edge types
- Bijection validity: 100% (no violations)

## Data Requirements

The module expects Neo4j data with:

### Node Properties (`:Dnode`)
- `muList`: String representation of root positions
- `n`: Numerator of rational μ
- `d`: Denominator of rational μ
- `determined`: 1 if all expected roots found, 0 otherwise
- `totalZero`: Count of integer roots
- `wNum`: Polynomial degree (via `:CreatedBye` relationship)

### Filtering
Only nodes with `determined=1` are used, as they have complete root information required for the bijection.

## Model Architectures

### Task 1 Models
- **RationalPartitionClassifier**: GCN → GCN → Linear → Softmax
- **SameDenominatorLinkPredictor**: GCN encoder + MLP decoder
- **ContrastiveEmbedder**: GCN → L2 normalize + InfoNCE loss

### Task 2 Models
- **IntegerValuePredictor**: GCN → GCN → Regression head
- **NextIntegerLinkPredictor**: GCN encoder + MLP with pairwise features

## Training Configuration

Default hyperparameters (can be customized):
- Learning rate: 0.01
- Max epochs: 100
- Early stopping patience: 10
- Dropout: 0.3
- Hidden dimensions: 64
- Embedding dimensions: 32

## Evaluation Metrics

### Partition Quality (Task 1)
- Purity: Fraction of pure predicted clusters
- Completeness: Fraction of true clusters recovered
- Adjusted Rand Index: Agreement with true partitioning

### Link Prediction (Both Tasks)
- Precision: Correct / Predicted
- Recall: Correct / True
- F1: Harmonic mean

### Sequence Quality (Task 2)
- Sequence Accuracy: Perfect orderings
- Kendall's Tau: Rank correlation
- Position MAE: Mean position error

## References

- **Specification**: [`GraphMLDifferences/GraphicLinkPrediction_specification.md`](../../GraphMLDifferences/GraphicLinkPrediction_specification.md)
- **Theory**: [`documentation/html-with-appendix-and-toc.html`](../../documentation/html-with-appendix-and-toc.html)
- **Graph Label Prediction**: [`ml/graph_label_prediction/`](../graph_label_prediction/)

## License

See project root for license information.

## Citation

If you use this module in your research, please cite the original work describing the integer-rational bijection theory.

