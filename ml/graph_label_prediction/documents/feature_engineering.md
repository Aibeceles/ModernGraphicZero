# Feature Engineering Pipeline

This document outlines the complete feature engineering infrastructure for the graph label prediction pipeline, including data loading, feature extraction, preprocessing, and model-specific feature configurations.

---

## Table of Contents

1. [Pipeline Overview](#pipeline-overview)
2. [Common Feature Engineering Steps](#common-feature-engineering-steps)
3. [Base Features](#base-features)
4. [Derived Features](#derived-features)
5. [Per-Model Feature Requirements](#per-model-feature-requirements)
6. [Implementation Details](#implementation-details)
7. [Graph Filtering Strategy](#graph-filtering-strategy)
8. [Feature Preprocessing](#feature-preprocessing)

---

## Pipeline Overview

### High-Level Architecture

```
Neo4j Database
      ↓
  [Cypher Queries]
      ↓
  DataFrame (nodes + edges)
      ↓
  [Feature Extraction]
      ↓
  Base Features (wNum, degree_at_node)
      ↓
  [Optional: Spectral PE]
      ↓
  Feature Matrix [N, d]
      ↓
  PyG Data Object → Model Training
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| **Data Loader** | `data_loader.py` | Queries Neo4j, builds feature matrices, creates PyG Data objects |
| **Config** | `config.py` | Feature definitions, dimensions, filtering parameters |
| **Models** | `models.py` | Model-specific feature usage (e.g., edge features for DepthAwareGAT) |
| **Binary Loader** | `binary_determined_loader.py` | Legacy one-hot encoding for baseline recreation |

### Data Flow Classes

**GraphDataLoader** (primary)
- Input: Neo4j connection, database name, filtering config
- Output: PyG Data object with node features, edge index, labels
- Use case: Root count prediction with GNN models

**BinaryDeterminedLoader** (baseline)
- Input: Neo4j connection, database name
- Output: NumPy arrays (X, y) with one-hot wNum encoding
- Use case: Binary determined/undetermined classification (2021 baseline recreation)

---

## Common Feature Engineering Steps

All models follow this pipeline:

### Step 1: Query Neo4j

**Node Query** (retrieves node properties):
```cypher
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
RETURN elementId(d) as node_id,
       coalesce(size(d.RootList), d.totalZero, 0) as label,
       d.totalZero as totalZero,
       d.RootList as RootList,
       size(d.RootList) as visibleRootCount,
       d.determined as determined,
       d.vmResult as vmResult,
       cb.wNum as wNum,
       cb.pArrayList as pArrayList
ORDER BY node_id
```

**Edge Query** (retrieves zMap relationships):
```cypher
MATCH (d1:Dnode)-[:zMap]-(d2:Dnode)
WHERE elementId(d1) < elementId(d2)
RETURN elementId(d1) as source, elementId(d2) as target
```

### Step 2: Build Node ID Mapping

Convert Neo4j elementIds (strings) to sequential indices (integers):

```python
# In GraphDataLoader.__init__()
self._node_id_to_idx = {
    node_id: idx for idx, node_id in enumerate(nodes_df['node_id'])
}
self._idx_to_node_id = {
    idx: node_id for node_id, idx in self._node_id_to_idx.items()
}
```

### Step 3: Extract Base Features

See [Base Features](#base-features) section below.

### Step 4: Construct Edge Index

Build undirected edge connectivity:

```python
edge_list = []
for _, row in edges_df.iterrows():
    src_idx = self._node_id_to_idx[row['source']]
    tgt_idx = self._node_id_to_idx[row['target']]
    
    # Add both directions for undirected graph
    edge_list.append([src_idx, tgt_idx])
    edge_list.append([tgt_idx, src_idx])

edge_index = torch.tensor(edge_list, dtype=torch.long).t()
# Shape: [2, num_edges * 2]
```

### Step 5: Optional Enrichment

- Spectral positional encodings (if graph size permits)
- Edge features (computed on-the-fly by models like DepthAwareGAT)
- Normalization/scaling (typically handled by model layers)

### Step 6: Create PyG Data Object

```python
data = Data(
    x=feature_matrix,          # [N, d] node features
    edge_index=edge_index,     # [2, E] edge connectivity
    y=labels,                  # [N] ground truth labels
    node_ids=node_ids,         # [N] original Neo4j IDs
    wNum=wNum_tensor,          # [N] convenience copy
    degree_at_node=degree_tensor  # [N] derived polynomial degree
)
```

---

## Base Features

### Configuration

Defined in `config.py`:

```python
MAX_POLYNOMIAL_DEGREE = 4

BASE_FEATURES = ['wNum', 'degree_at_node']
COEFFICIENT_FEATURES = ['coeff_0', 'coeff_1', 'coeff_2', 'coeff_3', 'coeff_4']
STATISTICAL_FEATURES = ['coeff_magnitude', 'leading_coeff_abs', 
                         'constant_term_abs', 'coeff_sparsity', 'coeff_mean_abs']

NUM_BASE_FEATURES = len(BASE_FEATURES)  # = 2
NUM_COEFFICIENT_FEATURES = MAX_POLYNOMIAL_DEGREE + 1  # = 5
NUM_STATISTICAL_FEATURES = len(STATISTICAL_FEATURES)  # = 5
NUM_FEATURES_TOTAL = NUM_BASE_FEATURES + NUM_COEFFICIENT_FEATURES + NUM_STATISTICAL_FEATURES  # = 12
```

### Feature 1: wNum (Polynomial Depth)

**Source:** `CreatedBy.wNum` property in Neo4j

**Definition:** Depth/level in the polynomial difference tree. Represents how many differentiation steps have been applied.

**Mathematical Interpretation:**
- wNum = 0: Constant polynomial (final difference)
- wNum = 1: Linear difference
- wNum = 2: Quadratic difference
- wNum = 3: Cubic difference
- wNum = 4: Quartic polynomial (root/original)

**Extraction:**
```python
def _build_feature_matrix(self, nodes_df):
    wnum_values = nodes_df['wNum'].fillna(0).values.astype(np.float32)
    wnum_values = wnum_values.reshape(-1, 1)  # [N, 1]
```

**Data Type:** Float32 (for compatibility with PyTorch)

**Range:** Typically [0, 4] in d4seed1 database

**Missing Values:** Filled with 0.0

**Usage:**
- All models use wNum as primary input feature
- DepthAwareGAT uses wNum to compute edge features (wNum_src - wNum_tgt)
- Directly encodes position in algebraic hierarchy

---

### Feature 2: degree_at_node (Derived Polynomial Degree)

**Source:** Derived from `Dnode.vmResult` (coefficient list)

**Definition:** Effective polynomial degree computed from non-zero coefficients in vmResult.

**vmResult Format (CRITICAL):** 
- Database stores with **leading zero padding** for degree > 0:
  - Degree > 0: `[0, c_n, c_{n-1}, ..., c_1, c_0]` (length = degree + 2)
  - Degree = 0: `[c_0]` (length = 1, no padding)
- Example: `"[0.0, 2.0, -5.0, 3.0]"` represents `2x² - 5x + 3` (quadratic with padding)
- At wNum=4: 6 elements (1 padding + 5 coefficients for quartic)
- At wNum=0: 1 element (constant, no padding)

**Extraction Algorithm:**

```python
from coefficient_features import parse_vmresult_coefficients

def extract_degree(vm_result):
    """Extract polynomial degree from vmResult."""
    padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree=4)
    return degree
```

**Example:**
```python
vmResult = "[0.0, 2.0, -5.0, 3.0]"  # 2x² - 5x + 3 (padded descending order)
# Parse: strip padding [0.0], get [2.0, -5.0, 3.0]
#        reverse to [3.0, -5.0, 2.0] (ascending: x^0, x^1, x^2)
# Find last non-zero: index 2 → degree = 2
```

**Data Type:** Float32

**Range:** [0.0, max_degree] - typically [0.0, 4.0]

**Missing Values:** 
- If vmResult is None or NaN → 0.0
- If vmResult is empty list → 0.0
- If all coefficients negligible → 0.0

**Usage:**
- Provides richer information than wNum alone
- Captures actual polynomial structure (not just tree position)
- Used by all models as second base feature
- In censored learning: y_true = round(degree_at_node) when determined=True

---

### Features 3-7: Coefficient Features (Padded)

**Source:** Extracted from `Dnode.vmResult` after parsing and padding

**Definition:** Individual polynomial coefficients in ascending power order, padded to max_degree + 1.

**Features:**
- `coeff_0`: Constant term (x⁰)
- `coeff_1`: Linear coefficient (x¹)
- `coeff_2`: Quadratic coefficient (x²)
- `coeff_3`: Cubic coefficient (x³)
- `coeff_4`: Quartic coefficient (x⁴)

**Extraction:**
```python
from coefficient_features import parse_vmresult_coefficients

padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree=4)
# padded_coeffs = [coeff_0, coeff_1, coeff_2, coeff_3, coeff_4]
```

**Example:**
```python
vmResult = "[0.0, 2.0, -5.0, 3.0]"  # 2x² - 5x + 3 (with leading zero padding)
padded_coeffs = [3.0, -5.0, 2.0, 0.0, 0.0]
#                 x^0   x^1   x^2  x^3  x^4
```

**Data Type:** Float32 array of length 5

**Padding:** Coefficients beyond actual degree are zero-padded

**Usage:**
- Provides full polynomial information to models
- GNN aggregation can learn coefficient patterns across zMap edges
- Attention models can weight neighbors by coefficient similarity
- Richer representation than degree alone

---

### Features 8-12: Statistical Features (Derived)

**Source:** Computed from padded coefficient vector

**Definition:** Statistical summary features capturing polynomial properties.

**Features:**

1. **coeff_magnitude** (Feature 8): L2 norm of coefficient vector
   - Formula: `sqrt(c_0² + c_1² + ... + c_n²)`
   - Captures overall scale of polynomial
   - Invariant to sign flips

2. **leading_coeff_abs** (Feature 9): Absolute value of leading coefficient
   - The coefficient of the highest-degree term
   - Indicates growth rate for large |x|

3. **constant_term_abs** (Feature 10): Absolute value of constant term
   - `|coeff_0|`
   - Value of polynomial at x=0

4. **coeff_sparsity** (Feature 11): Fraction of zero coefficients
   - Range: [0.0, 1.0]
   - 0.0 = dense (no zeros), 1.0 = sparse (all zeros except one)
   - Computed over relevant coefficients (up to degree, not padding)

5. **coeff_mean_abs** (Feature 12): Mean absolute coefficient value
   - Average magnitude of relevant coefficients
   - Scale-normalized measure of coefficient distribution

**Extraction:**
```python
from coefficient_features import compute_coefficient_statistics

stats = compute_coefficient_statistics(padded_coeffs, degree)
# stats = [magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs]
```

**Example:**
```python
vmResult = "[0.0, 2.0, -5.0, 3.0]"  # 2x² - 5x + 3 (with padding)
padded_coeffs = [3.0, -5.0, 2.0, 0.0, 0.0]
degree = 2.0

stats = compute_coefficient_statistics(padded_coeffs, degree)
# magnitude = sqrt(3² + 5² + 2²) ≈ 6.164
# leading_coeff_abs = |2.0| = 2.0
# constant_term_abs = |3.0| = 3.0
# sparsity = 0/3 = 0.0 (no zeros in [3, -5, 2])
# mean_abs = (|3| + |-5| + |2|) / 3 ≈ 3.333
```

**Data Type:** Float32 array of length 5

**Usage:**
- Provides scale and structural information complementing raw coefficients
- Helps models learn scale-invariant patterns
- Sparsity indicates special polynomial forms (e.g., binomials)
- Statistical aggregation more robust to coefficient noise

---

## Derived Features

### Spectral Positional Encodings (PE)

**What:** Eigenvectors of the normalized graph Laplacian that capture global graph structure.

**Mathematical Background:**

Given adjacency matrix A and degree matrix D:
```
L_norm = I - D^{-1/2} A D^{-1/2}
```

Compute smallest k eigenvectors (excluding trivial constant eigenvector):
```
L_norm v_i = λ_i v_i  for i = 1, 2, ..., k
```

Each node gets k-dimensional positional encoding: PE(node) = [v_1(node), v_2(node), ..., v_k(node)]

**Implementation:**

```python
def _add_spectral_positional_encodings(self, data, k=SPECTRAL_PE_DIM):
    """
    Add spectral PE via Laplacian eigenvectors.
    
    Steps:
    1. Build sparse adjacency matrix from edge_index
    2. Compute degree matrix D
    3. Construct normalized Laplacian: L = I - D^{-1/2} A D^{-1/2}
    4. Compute smallest k+1 eigenvectors using scipy.sparse.linalg.eigsh
    5. Skip first (constant) eigenvector, take next k
    6. Resolve sign ambiguity (make first non-zero element positive)
    7. Concatenate with base features
    """
    from scipy.sparse.linalg import eigsh
    
    # Build Laplacian (see data_loader.py lines 305-322)
    num_nodes = data.num_nodes
    adj = build_sparse_adjacency(data.edge_index, num_nodes)
    degrees = adj.sum(axis=1).flatten()
    d_inv_sqrt = 1.0 / np.sqrt(np.maximum(degrees, 1))
    L_norm = I - D_inv_sqrt @ A @ D_inv_sqrt
    
    # Compute eigenvectors
    eigenvalues, eigenvectors = eigsh(L_norm, k=k+1, which='SM')
    
    # Skip first, take k dimensions
    pe = eigenvectors[:, 1:k+1]
    
    # Concatenate
    data.x = torch.cat([data.x, torch.tensor(pe, dtype=torch.float32)], dim=-1)
    return data
```

**Configuration:**

```python
# In config.py
SPECTRAL_PE_DIM = 0  # Disabled by default (set to 8 for enabled)
MAX_NODES_FOR_SPECTRAL_PE = 100000  # Skip if graph too large
```

**When Applied:**
- Only if `SPECTRAL_PE_DIM > 0` in config
- Only if `num_nodes <= MAX_NODES_FOR_SPECTRAL_PE`
- Otherwise, zero-padded to maintain expected feature dimension

**Memory Requirements:**
- Laplacian construction: O(E) sparse matrix (manageable)
- Eigensolver: O(N * k * iterations) (bottleneck for N > 100K)
- Example: N=1M, k=8 → ~7.55 TiB RAM required (infeasible)

**Fallback Behavior:**
```python
if data.num_nodes > MAX_NODES_FOR_SPECTRAL_PE:
    print("Skipping spectral PE (graph too large)")
    pe_placeholder = torch.zeros(data.num_nodes, SPECTRAL_PE_DIM)
    data.x = torch.cat([data.x, pe_placeholder], dim=-1)
```

**Why It Helps:**
- Standard GNN message passing is local (k-hop neighborhood)
- Spectral PE provides global positional context
- Nodes in same spectral cluster (similar eigenvector values) share structural roles
- Useful for distinguishing nodes with similar local neighborhoods but different global positions

**Used By:**
- All models when SPECTRAL_PE_DIM > 0
- Particularly beneficial for DepthAwareGAT (combines local depth + global position)

---

### Edge Features (Polynomial-Aware, 18D)

**What:** Mathematically-motivated features computed from polynomial coefficient relationships between connected nodes.

**Source:** Computed on-the-fly from full node feature vectors during forward pass (see `edge_features.py`)

**Key Insight:** In the polynomial difference tree, parent→child edges represent the forward difference operation Δ. These edge features capture how well the coefficient relationship aligns with differentiation properties.

**Implementation (in DepthAwareGAT):**

```python
from edge_features import compute_polynomial_edge_features

def forward(self, x, edge_index, wNum=None):
    """
    Compute polynomial-aware edge features during forward pass.
    Uses full 12D node features including coefficients.
    """
    # Compute 18D edge features from node polynomial information
    edge_feat = compute_polynomial_edge_features(x, edge_index)  # [E, 18]
    
    # Encode via MLP: 18D → num_heads
    edge_attr = self.edge_encoder(edge_feat)  # [E, num_heads]
    
    # Use in attention
    h = self.conv1(x, edge_index, edge_attr=edge_attr)
```

**Categories (18 features total):**

#### Category 1: Depth Features (4D)

| Feature | Description | Range |
|---------|-------------|-------|
| `depth_diff_normalized` | (wNum_src - wNum_tgt) / max_depth | [-1, 1] |
| `depth_diff_abs` | |wNum_src - wNum_tgt| / max_depth | [0, 1] |
| `direction` | 1.0 if parent→child, else 0.0 | {0, 1} |
| `is_adjacent` | 1.0 if |depth_diff| ≈ 1, else 0.0 | {0, 1} |

#### Category 2: Degree Features (3D)

| Feature | Description | Expected for Δ |
|---------|-------------|----------------|
| `degree_diff_normalized` | (degree_src - degree_tgt) / 4 | ~0.25 (diff≈1) |
| `degree_ratio` | degree_tgt / degree_src | ~(n-1)/n |
| `degree_consistency` | exp(-(degree_diff - 1)²) | ~1.0 for valid Δ |

#### Category 3: Leading Coefficient Features (3D)

| Feature | Description | Mathematical Motivation |
|---------|-------------|------------------------|
| `leading_ratio` | |leading_tgt| / |leading_src| | For Δ: ~n (degree scaling) |
| `leading_diff` | (leading_tgt - leading_src) / max | Coefficient change |
| `scaling_error` | |leading_tgt - degree_src × leading_src| | Δ property error |

#### Category 4: Similarity Features (4D)

| Feature | Description | Range |
|---------|-------------|-------|
| `cosine_similarity` | cos(coeff_src, coeff_tgt) | [-1, 1] |
| `l2_distance` | ||coeff_src - coeff_tgt||₂ normalized | [0, 1] |
| `correlation` | Pearson correlation of coefficient vectors | [-1, 1] |
| `sparsity_diff` | sparsity_src - sparsity_tgt | [-1, 1] |

#### Category 5: Magnitude Features (2D)

| Feature | Description | Range |
|---------|-------------|-------|
| `magnitude_ratio` | ||coeff_tgt|| / ||coeff_src|| | [0, 1] normalized |
| `magnitude_diff_log` | log(||coeff_tgt||+1) - log(||coeff_src||+1) | normalized |

#### Category 6: Constant Term Features (2D)

| Feature | Description | Range |
|---------|-------------|-------|
| `constant_diff` | |c0_tgt - c0_src| normalized | [0, 1] |
| `constant_ratio` | |c0_tgt| / |c0_src| | [0, 1] normalized |

**Why 18D Instead of 3D:**

The previous 3D edge features (depth_diff, abs_diff, direction) were **identical for all parent→child edges at the same level**. In a tree where all such edges have depth_diff=1, the attention mechanism received no information to differentiate edges.

With 18D features:
- Coefficient-based features vary uniquely per edge
- Similarity metrics capture polynomial family relationships
- Mathematical consistency features (degree_consistency, scaling_error) highlight edges that follow differentiation patterns vs. anomalies

**Configuration:**

```python
# In config.py
NUM_EDGE_FEATURES = 18

# Feature indices for edge computation
NODE_FEATURE_INDICES = {
    'wNum': 0, 'degree': 1,
    'coeff_start': 2, 'coeff_end': 7,
    'magnitude': 7, 'leading_coeff': 8,
    'constant_term': 9, 'sparsity': 10, 'mean_abs': 11,
}
```

**Used By:**
- DepthAwareGAT
- MultiTaskRootClassifier

**Why These Features:**
- Addresses directionality ignored by GCN/GraphSAGE
- Parent-child edges carry different information than child-parent
- Normalization prevents saturation with large depth ranges
- Explicit direction helps attention learn asymmetric weights

**Edge Encoder:**

```python
self.edge_encoder = nn.Sequential(
    nn.Linear(3, hidden_dim),
    nn.ReLU(),
    nn.Linear(hidden_dim, num_heads),
)
```

Maps 3-dim edge features to per-head attention features.

**Used By:**
- DepthAwareGAT
- MultiTaskRootClassifier (shared backbone with DepthAwareGAT)

---

### One-Hot wNum Encoding (Legacy)

**What:** Binary indicators for each wNum value (0-4), used in 2021 baseline recreation.

**Implementation (BinaryDeterminedLoader):**

```python
def _engineer_features(self, df):
    wNum = df['wNum'].fillna(0).values
    
    # One-hot encode wNum
    zero = (wNum == 0).astype(np.float32)   # [1, 0, 0, 0, 0] for wNum=0
    one = (wNum == 1).astype(np.float32)    # [0, 1, 0, 0, 0] for wNum=1
    two = (wNum == 2).astype(np.float32)    # [0, 0, 1, 0, 0] for wNum=2
    three = (wNum == 3).astype(np.float32)  # [0, 0, 0, 1, 0] for wNum=3
    four = (wNum == 4).astype(np.float32)   # [0, 0, 0, 0, 1] for wNum=4
    
    return {
        'zero': zero, 'one': one, 'two': two, 'three': three, 'four': four,
        'wNum': wNum.astype(np.float32),
        'totalZero': df['totalZero'].fillna(0).astype(np.float32),
    }
```

**Feature Vector:** [zero, one, two, three, four, wNum, totalZero] (7 dimensions)

**Why It Exists:**
- Recreates Neo4j GDS original pipeline for apples-to-apples comparison
- One-hot encoding makes wNum a categorical variable (not ordinal)
- Includes totalZero as input (creates data leakage in determined prediction)

**Used By:**
- Binary determined/undetermined classification baseline
- NOT used by root count prediction models (use continuous wNum instead)

---

## Per-Model Feature Requirements

### MLPClassifier

**Input Features:**
- Base: wNum, degree_at_node (2 dims)
- Coefficient: coeff_0 to coeff_4 (5 dims)
- Statistical: magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs (5 dims)
- Optional: Spectral PE (8 dims) → Total: 12 or 20 dims

**Edge Features:** None (ignores graph structure)

**Preprocessing:** None required (model handles raw features)

**Feature Usage:**
```python
x = data.x  # [N, 12] (or 20 with spectral PE)
# Forward: x → fc1 → ReLU → dropout → fc2 → ReLU → dropout → fc3 → log_softmax
```

---

### GCNClassifier & GraphSAGEClassifier

**Input Features:**
- Base: wNum, degree_at_node (2 dims)
- Coefficient: coeff_0 to coeff_4 (5 dims)
- Statistical: magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs (5 dims)
- Optional: Spectral PE (8 dims) → Total: 12 or 20 dims

**Edge Features:** None (symmetric aggregation)

**Graph Structure:** Undirected edge_index

**Preprocessing:** None required

**Feature Usage:**
```python
x = data.x  # [N, 12] (or 20 with spectral PE)
edge_index = data.edge_index
# GCN aggregation: h^(l+1) = σ(D̃^{-1/2} Ã D̃^{-1/2} h^(l) W^(l))
# SAGE aggregation: h_v' = σ(W · CONCAT(h_v, AGG({h_u : u ∈ N(v)})))
```

---

### DepthAwareGAT & MultiTaskRootClassifier

**Input Features:**
- Base: wNum, degree_at_node (2 dims)
- Optional: Spectral PE (8 dims) → Total: 2 or 10 dims

**Edge Features:** 
- Computed from wNum during forward pass
- 3-component encoding: [normalized_diff, abs_diff, direction]
- Encoded to per-head attention features

**Graph Structure:** Undirected edge_index (but edge features encode directionality)

**Critical Requirement:** 
- `wNum` must be available (either as data.wNum or data.x[:, 0])
- If not provided to forward(), extracted from first feature column

**Feature Usage:**
```python
x = data.x
edge_index = data.edge_index
wNum = data.wNum  # or x[:, 0]

# Compute edge features on-the-fly
src, tgt = edge_index
depth_diff = wNum[src] - wNum[tgt]
edge_feat = build_3component_encoding(depth_diff)
edge_attr = edge_encoder(edge_feat)

# Attention with edge features
h = GATConv(x, edge_index, edge_attr=edge_attr)
```

**Preprocessing:**
- wNum normalization handled internally (max_diff scaling)
- No external preprocessing required

---

### RegressionClassifier

**Input Features:**
- Base: wNum, degree_at_node (2 dims)
- Optional: Spectral PE (8 dims) → Total: 2 or 10 dims

**Edge Features:** None (uses GCN backbone)

**Output:** Continuous scalar [0.0, 4.0] (not class probabilities)

**Feature Usage:**
```python
x = data.x
edge_index = data.edge_index
# GCN feature extraction → regression head → sigmoid * 4.0
continuous_pred = model(x, edge_index)  # [N, 1]
class_pred = continuous_pred.round().clamp(0, 4)  # [N] discretized
```

---

### CORALClassifier

**Input Features:**
- Base: wNum, degree_at_node (2 dims)
- Optional: Spectral PE (8 dims) → Total: 2 or 10 dims

**Edge Features:** None (uses GCN backbone)

**Output:** 4 threshold logits (not 5 class logits)

**Feature Usage:**
```python
x = data.x
edge_index = data.edge_index
# GCN feature extraction
h = GCN(x, edge_index)
# CORAL head: shared weights + per-threshold biases
shared_logit = fc(h)  # [N, 1]
logits = shared_logit + threshold_biases  # [N, 4] broadcasting
# Each logit_k = P(y > k) for k ∈ {0,1,2,3}
```

**Training Targets:**
- For label y=2: binary_targets = [1, 1, 0, 0]
  - P(y > 0) = True (at least 1 root)
  - P(y > 1) = True (at least 2 roots)
  - P(y > 2) = False (not 3 or more)
  - P(y > 3) = False (not 4 roots)

---

## Implementation Details

### Source Code: Feature Matrix Construction

**Location:** `data_loader.py`, lines 190-250

```python
def _build_feature_matrix(self, nodes_df) -> torch.Tensor:
    """
    Build feature matrix: [wNum, degree, coeffs[5], stats[5]] = 12D.
    
    Returns:
        Tensor of shape [num_nodes, NUM_FEATURES_TOTAL]
    """
    num_nodes = len(nodes_df)
    
    # Feature 1: wNum
    if 'wNum' in nodes_df.columns:
        wnum_values = nodes_df['wNum'].fillna(0).values.astype(np.float32).reshape(-1, 1)
    else:
        wnum_values = np.zeros((num_nodes, 1), dtype=np.float32)
    
    # Features 2-12: degree + coefficients + statistics
    degree_values = np.zeros((num_nodes, 1), dtype=np.float32)
    coeff_values = np.zeros((num_nodes, NUM_COEFFICIENT_FEATURES), dtype=np.float32)
    stats_values = np.zeros((num_nodes, NUM_STATISTICAL_FEATURES), dtype=np.float32)
    
    if 'vmResult' in nodes_df.columns:
        for i, vm_result in enumerate(nodes_df['vmResult']):
            coeffs, degree, stats = extract_coefficient_features(
                vm_result, 
                max_degree=MAX_POLYNOMIAL_DEGREE
            )
            degree_values[i, 0] = degree
            coeff_values[i, :] = coeffs
            stats_values[i, :] = stats
    
    # Concatenate all features: [wNum, degree, coeffs, stats]
    feature_matrix = np.concatenate([
        wnum_values,      # [N, 1]
        degree_values,    # [N, 1]
        coeff_values,     # [N, 5]
        stats_values,     # [N, 5]
    ], axis=1)  # [N, 12]
    
    # Validate dimension
    assert feature_matrix.shape[1] == NUM_FEATURES_TOTAL
        raise ValueError(
            f"Base feature dim mismatch: got {base.shape[1]}, "
            f"expected {NUM_BASE_FEATURES}"
        )
    
    return torch.tensor(base, dtype=torch.float32)
```

### Source Code: Spectral PE

**Location:** `data_loader.py`, lines 266-371

**Key Steps:**

1. **Build Sparse Adjacency:**
```python
row, col = edge_index[0], edge_index[1]
data_vals = np.ones(len(row))
adj = csr_matrix((data_vals, (row, col)), shape=(num_nodes, num_nodes))
```

2. **Normalize:**
```python
degrees = np.array(adj.sum(axis=1)).flatten()
d_inv_sqrt = 1.0 / np.sqrt(np.maximum(degrees, 1))
d_inv_sqrt_matrix = csr_matrix((d_inv_sqrt, (range(num_nodes), range(num_nodes))))
normalized_adj = d_inv_sqrt_matrix @ adj @ d_inv_sqrt_matrix
laplacian = eye(num_nodes) - normalized_adj
```

3. **Eigensolver:**
```python
num_eigenvectors = min(k + 1, num_nodes - 2)
eigenvalues, eigenvectors = eigsh(
    laplacian.astype(np.float64),
    k=num_eigenvectors,
    which='SM',  # Smallest magnitude
    maxiter=num_nodes * 10
)
```

4. **Sign Resolution:**
```python
for i in range(pe.shape[1]):
    first_nonzero_idx = np.argmax(np.abs(pe[:, i]) > 1e-8)
    if pe[first_nonzero_idx, i] < 0:
        pe[:, i] = -pe[:, i]  # Flip to positive
```

### Source Code: Edge Index Construction

**Location:** `data_loader.py`, lines 373-405

```python
def _build_edge_index(self, edges_df) -> torch.Tensor:
    """
    Build undirected edge index from Neo4j zMap edges.
    
    Neo4j query returns deduplicated edges (source < target).
    This adds both directions for PyG undirected representation.
    """
    if len(edges_df) == 0:
        return torch.zeros((2, 0), dtype=torch.long)
    
    edge_list = []
    for _, row in edges_df.iterrows():
        src_idx = self._node_id_to_idx.get(row['source'])
        tgt_idx = self._node_id_to_idx.get(row['target'])
        
        if src_idx is not None and tgt_idx is not None:
            # Add both directions
            edge_list.append([src_idx, tgt_idx])
            edge_list.append([tgt_idx, src_idx])
    
    if not edge_list:
        return torch.zeros((2, 0), dtype=torch.long)
    
    edge_index = torch.tensor(edge_list, dtype=torch.long).t().contiguous()
    return edge_index
```

### Source Code: PyG Data Assembly

**Location:** `data_loader.py`, lines 129-171

```python
# Create PyG Data object
data = Data(x=x, edge_index=edge_index, y=y_visible)

# Store metadata
data.node_ids = node_ids.tolist()  # For Neo4j write-back
data.y_visible = y_visible.clone()
data.determined = torch.tensor(determined, dtype=torch.bool)
data.wNum = x[:, 0].clone()  # Convenience copy
data.degree_at_node = x[:, 1].clone()  # Convenience copy

# Optional: derived y_true for censored learning
y_true = torch.full((data.num_nodes,), -1, dtype=torch.long)
if hasattr(data, 'degree_at_node'):
    deg_int = data.degree_at_node.round().clamp(min=0).long()
    y_true[data.determined] = deg_int[data.determined]
data.y_true = y_true

# Add spectral PE (if configured and feasible)
if SPECTRAL_PE_DIM > 0 and data.num_nodes <= MAX_NODES_FOR_SPECTRAL_PE:
    data = self._add_spectral_positional_encodings(data)
```

---

## Graph Filtering Strategy

### Problem: Large Graphs

**Challenge:** Full d4seed1 database has 152,196 Dnode nodes
- Spectral PE requires eigendecomposition of N×N Laplacian
- Memory: O(N²) for dense methods, O(N·k·iterations) for sparse
- N=152K → infeasible on typical hardware

### Solution: pArrayList Filtering

**Concept:** Filter nodes by `pArrayList` property on CreatedBy nodes

**Configuration:**
```python
# config.py
USE_GRAPH_FILTERING = True
PARRAY_MIN = 0
PARRAY_MAX = 5  # Exclusive
MAX_NODES_FOR_SPECTRAL_PE = 100000
```

**Filtered Query:**
```cypher
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
WHERE all(x IN cb.pArrayList WHERE x >= 0 AND x < 5)
WITH collect(DISTINCT d) AS filtered_nodes
MATCH (a:Dnode)-[r:zMap]-(b:Dnode)
WHERE a IN filtered_nodes AND b IN filtered_nodes
  AND elementId(a) < elementId(b)
RETURN elementId(a) as source, elementId(b) as target
```

**Effect:**
- Full database: 152,196 nodes
- Filtered (pArrayList ∈ [0, 5)): 3,775 nodes (97.5% reduction)
- Enables spectral PE computation on manageable subgraph

**Trade-offs:**
- ✅ Enables advanced feature engineering (spectral PE)
- ✅ Faster training (smaller graph)
- ⚠️ Predictions only valid for filtered subgraph
- ⚠️ May introduce selection bias if pArrayList correlates with root count

**Dynamic Behavior:**
```python
if data.num_nodes > MAX_NODES_FOR_SPECTRAL_PE:
    print("Skipping spectral PE (graph too large)")
    pe_placeholder = torch.zeros(data.num_nodes, SPECTRAL_PE_DIM)
    data.x = torch.cat([data.x, pe_placeholder], dim=-1)
else:
    print("Computing spectral PE...")
    data = self._add_spectral_positional_encodings(data)
```

### Tuning Filtering Parameters

**Stricter Filtering (Smaller Graph):**
```python
PARRAY_MAX = 3  # Reduce to [0, 3) for even smaller graph
```

**Looser Filtering (Larger Coverage):**
```python
PARRAY_MAX = 7  # Expand to [0, 7) for more nodes
```

**Disable Filtering (Full Graph):**
```python
USE_GRAPH_FILTERING = False  # Load all nodes (may skip spectral PE)
```

---

## Feature Preprocessing

### Normalization

**Current Approach:** No explicit normalization in data loader

**Rationale:**
- wNum has natural scale [0, 4] - interpretable and already bounded
- degree_at_node has similar scale [0.0, 4.0] - derived from polynomial degree
- GNN layers (GCN, GAT) apply their own normalization via symmetric/attention weights

**Alternative (if needed):**

```python
from sklearn.preprocessing import StandardScaler

# After building feature matrix
scaler = StandardScaler()
x_scaled = scaler.fit_transform(x.numpy())
x = torch.tensor(x_scaled, dtype=torch.float32)
```

**When to Use:**
- If adding features with vastly different scales (e.g., node degree counts in [1, 10000])
- If using distance-based methods (KNN, clustering) as auxiliary tasks
- NOT recommended for current 2-feature setup (wNum, degree_at_node)

### Missing Value Handling

**Strategy:** Fill with zeros

```python
wnum_values = nodes_df['wNum'].fillna(0).values
degree_values = nodes_df['vmResult'].apply(derive_degree).fillna(0).values
```

**Justification:**
- wNum missing → treat as constant polynomial (depth 0)
- vmResult missing → treat as zero-degree polynomial (constant)
- Zero is semantically meaningful (not arbitrary imputation)

**Validation:**
```python
if (labels < 0).any():
    raise ValueError("Found negative labels in database")
```

### Feature Validation

**Dimension Check:**
```python
if base.shape[1] != NUM_BASE_FEATURES:
    raise ValueError(
        f"Base feature dim mismatch: got {base.shape[1]}, "
        f"expected {NUM_BASE_FEATURES}"
    )
```

**Data Type Enforcement:**
```python
x = torch.tensor(base, dtype=torch.float32)  # Always float32
y = torch.tensor(labels, dtype=torch.long)   # Always int64
```

### Feature Clipping

**Degree Clipping (for y_true):**
```python
deg_int = data.degree_at_node.round().clamp(min=0).long()
```

Ensures y_true ≥ 0 (non-negative root counts)

**Edge Feature Normalization:**
```python
max_diff = depth_diff.abs().max().clamp(min=1.0)
normalized_diff = depth_diff / max_diff
```

Prevents division by zero when all nodes have same wNum (rare)

---

## Feature Matrix Format

### Shape Convention

```python
x: [num_nodes, num_features]
   - num_nodes: Total nodes in graph (N)
   - num_features: NUM_BASE_FEATURES + SPECTRAL_PE_DIM
   
   Default: [N, 2] (wNum + degree_at_node)
   With PE: [N, 10] (wNum + degree_at_node + 8 spectral dims)
```

### Storage Layout

**Column Order (BASE_FEATURES):**
```
x[:, 0] = wNum             # Depth in difference tree
x[:, 1] = degree_at_node   # Derived polynomial degree
x[:, 2:10] = spectral_pe   # (if enabled) Laplacian eigenvectors
```

**Convenience Copies:**
```python
data.wNum = x[:, 0].clone()            # Used by DepthAwareGAT
data.degree_at_node = x[:, 1].clone()  # Used for y_true derivation
```

### Label Conventions

**Primary Target:**
```python
data.y = y_visible  # len(RootList) - all distinct roots in expanded window
```

**Auxiliary Targets:**
```python
data.y_visible = y_visible.clone()  # Explicit visible count
data.y_true = y_true                # Derived from degree_at_node (valid when determined=1)
data.determined = determined_bool   # Completeness flag (from larger scan window)
```

**Target Modes (config.TARGET_MODE):**
- `'visible'`: Train on y_visible (default)
- `'multitask'`: Train on y_visible + determined simultaneously
- `'censored'`: Train on y_true with constraint y_true ≥ y_visible

---

## Configuration Summary

### config.py Settings

```python
# Base features (always used)
BASE_FEATURES = ['wNum', 'degree_at_node']
NUM_BASE_FEATURES = 2

# Spectral PE (optional)
SPECTRAL_PE_DIM = 0  # Set to 8 to enable
MAX_NODES_FOR_SPECTRAL_PE = 100000

# Total input dimension
NUM_FEATURES = NUM_BASE_FEATURES + SPECTRAL_PE_DIM  # = 2 (default)

# Graph filtering (for large graphs)
USE_GRAPH_FILTERING = True
PARRAY_MIN = 0
PARRAY_MAX = 5  # Exclusive

# Target configuration
TARGET_MODE = 'visible'  # 'visible' | 'multitask' | 'censored'
LOSS_TYPE = 'emd'        # 'ce' | 'emd'
```

### Loading Examples

**Standard Loading (Filtered, No PE):**
```python
from graph_label_prediction.data_loader import GraphDataLoader

loader = GraphDataLoader(client, "d4seed1", use_filtering=True)
data = loader.load()
# data.x: [3775, 2] - wNum + degree_at_node
# data.edge_index: [2, num_edges]
# data.y: [3775] - visible root count labels
```

**With Spectral PE (Requires Small Graph):**
```python
# In config.py, set:
SPECTRAL_PE_DIM = 8
USE_GRAPH_FILTERING = True  # Ensure graph < 100K nodes
PARRAY_MAX = 3              # Stricter filtering if needed

loader = GraphDataLoader(client, "d4seed1", use_filtering=True)
data = loader.load()
# data.x: [N, 10] - wNum + degree_at_node + 8 spectral dims
```

**Full Graph (No Filtering):**
```python
loader = GraphDataLoader(client, "d4seed1", use_filtering=False)
data = loader.load()
# data.x: [152196, 2] - all nodes, no spectral PE (too large)
```

---

## Troubleshooting

### Issue: Spectral PE Computation Fails

**Symptoms:**
```
RuntimeError: eigsh() did not converge
```

**Solutions:**
1. Increase maxiter: `eigsh(L, k=k+1, maxiter=num_nodes * 20)`
2. Use smaller k: `SPECTRAL_PE_DIM = 4` (instead of 8)
3. Filter graph more aggressively: `PARRAY_MAX = 3`
4. Disable PE: `SPECTRAL_PE_DIM = 0`

### Issue: Memory Error During Spectral PE

**Symptoms:**
```
MemoryError: Unable to allocate array
```

**Solutions:**
1. Enable filtering: `USE_GRAPH_FILTERING = True`
2. Reduce graph size: `PARRAY_MAX = 3`
3. Skip PE for large graphs (automatic): `MAX_NODES_FOR_SPECTRAL_PE = 50000`

### Issue: Feature Dimension Mismatch

**Symptoms:**
```
RuntimeError: mat1 and mat2 shapes cannot be multiplied
```

**Diagnosis:**
```python
print(f"Expected: {config.NUM_FEATURES}")
print(f"Got: {data.x.shape[1]}")
```

**Solutions:**
1. Check BASE_FEATURES config matches data_loader implementation
2. Ensure spectral PE correctly padded when skipped
3. Restart Jupyter kernel to reload config changes

### Issue: Missing wNum for DepthAwareGAT

**Symptoms:**
```
AttributeError: 'Data' object has no attribute 'wNum'
```

**Solutions:**
```python
# Option 1: Use convenience copy
model(data.x, data.edge_index, wNum=data.wNum)

# Option 2: Extract from features
model(data.x, data.edge_index, wNum=data.x[:, 0])

# Option 3: Let model extract automatically
model(data.x, data.edge_index)  # Uses x[:, 0] internally
```

---

## Summary

### Feature Pipeline at a Glance

| Stage | Input | Output | Location |
|-------|-------|--------|----------|
| **Query** | Neo4j database | DataFrames (nodes, edges) | `data_loader.py:90-124` |
| **Base Features** | wNum, vmResult | [N, 2] tensor | `data_loader.py:190-233` |
| **Spectral PE** | edge_index | [N, 8] eigenvectors | `data_loader.py:266-371` |
| **Edge Index** | DataFrame | [2, E] tensor | `data_loader.py:373-405` |
| **Edge Features** | wNum, edge_index | [E, 3] tensor | `models.py:345-362` |
| **PyG Data** | All above | Data object | `data_loader.py:129-187` |

### Feature Dimensions

| Configuration | wNum | degree | Spectral PE | Total |
|---------------|------|--------|-------------|-------|
| Default | 1 | 1 | 0 | 2 |
| With PE | 1 | 1 | 8 | 10 |

### Model Requirements

| Model | Base | Spectral PE | Edge Feat | Graph |
|-------|------|-------------|-----------|-------|
| MLPClassifier | ✅ | Optional | ❌ | ❌ |
| GCNClassifier | ✅ | Optional | ❌ | ✅ |
| GraphSAGEClassifier | ✅ | Optional | ❌ | ✅ |
| DepthAwareGAT | ✅ | Optional | ✅ (computed) | ✅ |
| MultiTask | ✅ | Optional | ✅ (computed) | ✅ |
| RegressionClassifier | ✅ | Optional | ❌ | ✅ |
| CORALClassifier | ✅ | Optional | ❌ | ✅ |

---

*Document version: 1.0*  
*Last updated: 2026-01-22*  
*Pipeline: Graph Label Prediction - Feature Engineering*

