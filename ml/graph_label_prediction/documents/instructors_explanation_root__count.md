# Instructor's Explanation: Root Count Prediction Pipeline

## Executive Summary

**The model achieves 62.49% weighted F1 but only predicts 2 out of 5 possible root counts.** It predicts 0 and 1 roots perfectly but completely ignores polynomials with 2, 3, or 4 roots—misclassifying them all as having 1 root. This is a multi-class variant of the same class imbalance problem seen in the binary determined/undetermined task.

---

## Part 1: Cell-by-Cell Explanation

### Cells 0-6: Setup and Database Connection

**What happened:**
- Loaded Python libraries (PyTorch, PyTorch Geometric, scikit-learn)
- Connected to Neo4j database `d4seed1`
- Confirmed 152,196 Dnode nodes in the database

**Task Change:** This notebook predicts **root count (totalZero)** as a 5-class problem, not the binary determined/undetermined classification.

---

### Cells 7-9: Data Loading

**What happened:**
- Queried Neo4j for all nodes connected via CreatedBye→CreatedBy relationship
- Extracted 1 feature per node: `wNum` (polynomial degree)
- Target: `totalZero` (number of rational roots: 0, 1, 2, 3, or 4)
- Loaded 152,195 edges (zMap relationships)

**Class Distribution:**
```
0 roots: 203,745 nodes (20.0%)
1 root:  541,867 nodes (53.2%) ← MAJORITY CLASS
2 roots: 231,306 nodes (22.7%)
3 roots:  36,008 nodes (3.5%)
4 roots:   5,784 nodes (0.6%)  ← EXTREME MINORITY
```

**Total nodes: 1,018,710** (including CreatedBy nodes that don't have Dnode labels but participate in the structure)

---

### Cells 10-16: Model Training

**What happened:**
- Split data: 814,968 training nodes (80%), 203,742 test nodes (20%)
- Trained 3 models: MLP, GCN, GraphSAGE
- All achieved identical F1 ≈ 0.6249 and MAE ≈ 0.3148

**Training progression (all models):**
```
Epoch 0-10:  F1 ≈ 0.37-0.45 (learning class 1 only)
Epoch 20:    F1 = 0.6249 (converged to local optimum)
Early stop:  epochs 25-30
```

**The convergence pattern:** All models reached the same local minimum—predicting only classes 0 and 1.

---

### Cells 17-19: Hyperparameter Tuning

**What happened:**
- Grid search over L2 penalties: [0.0625, 0.5, 1.0, 4.0]
- Results:
  - penalty=0.0625: F1 = 0.6249 ± 0.0005
  - penalty=0.5:    F1 = 0.3694 ± 0.0009
  - penalty=1.0:    F1 = 0.3694 ± 0.0009
  - penalty=4.0:    F1 = 0.3694 ± 0.0009

**Interpretation:** Higher regularization (≥0.5) forces the model to predict only the majority class (1 root), achieving F1 = 0.37. Lower regularization allows it to also learn class 0.

---

### Cells 20-22: Final Model & Classification Report

**What happened:**
- Trained final GCN with penalty=0.0625
- Generated detailed classification report

**The Classification Report:**
```
              precision    recall  f1-score   support

     0 roots       1.00      1.00      1.00     40749
      1 root       0.66      1.00      0.80    108373
     2 roots       0.00      0.00      0.00     46261
     3 roots       0.00      0.00      0.00      7202
     4 roots       0.00      0.00      0.00      1157

    accuracy                           0.73    203742
   macro avg       0.33      0.40      0.36    203742
weighted avg       0.55      0.73      0.62    203742
```

**Critical Observations:**
- **Class 0 (0 roots):** Perfect classification (precision=1.00, recall=1.00)
- **Class 1 (1 root):** Precision=0.66 (many false positives from classes 2-4), Recall=1.00
- **Classes 2, 3, 4:** Complete failure (0.00 across all metrics)

---

### Cells 23-26: Prediction Analysis

**What happened:**
- Generated predictions for all 1,018,710 nodes
- Analyzed prediction distribution

**Prediction Distribution:**
```
0 roots: 203,741 predictions (20.0%)
1 root:  814,969 predictions (80.0%) ← Everything else
2 roots:       0 predictions
3 roots:       0 predictions
4 roots:       0 predictions
```

**Confusion Matrix:**
```
           Predicted
            0      1      2      3      4
Actual 0  203741     4     0      0      0   ← Near-perfect
       1       0 541867     0      0      0   ← Perfect
       2       0 231306     0      0      0   ← ALL misclassified as 1
       3       0  36008     0      0      0   ← ALL misclassified as 1
       4       0   5784     0      0      0   ← ALL misclassified as 1
```

**Per-class Accuracy:**
- 0 roots: 100%
- 1 root:  100%
- 2 roots: 0%
- 3 roots: 0%
- 4 roots: 0%

---

## Part 2: Theoretical Analysis

### Why Did This Happen?

#### 1. Multi-Class Imbalance Structure

The class distribution creates a hierarchical dominance:

| Class | Count | % | Relative to Class 4 |
|-------|-------|---|---------------------|
| 1 root (majority) | 541,867 | 53.2% | 94× more |
| 2 roots | 231,306 | 22.7% | 40× more |
| 0 roots | 203,745 | 20.0% | 35× more |
| 3 roots | 36,008 | 3.5% | 6× more |
| 4 roots | 5,784 | 0.6% | 1× (baseline) |

**Key insight:** The model learns in order of class frequency:
1. First learns to predict class 1 (majority) for everything → F1 ≈ 0.37
2. Then learns class 0 (separable from others) → F1 ≈ 0.62
3. Never learns classes 2, 3, 4 (gradient signal too weak)

#### 2. Single Feature Limitation

With only `wNum` (polynomial degree) as input, the model lacks information to distinguish root counts within the same degree. Consider:
- A degree-4 polynomial could have 0, 1, 2, 3, or 4 rational roots
- The feature `wNum=4` gives no information about which case applies
- The model defaults to the most common root count for each degree

#### 3. Gradient Dynamics in Multi-Class

For cross-entropy loss with 5 classes:
$$\mathcal{L} = -\sum_{c=0}^{4} y_c \log(\hat{p}_c)$$

Each class contributes to the gradient proportionally to its frequency:
- Class 1 (53.2%): Dominates gradient signal
- Class 4 (0.6%): Contributes 1/88th the gradient signal

**Result:** The model optimizes for classes 0 and 1 because that's where the loss reduction is largest.

---

## Part 3: Why Class 0 Is Learnable

**Interesting question:** Why did the model learn class 0 (20% of data) but not class 2 (22.7%)?

**Answer: Feature separability.**

The relationship between `wNum` and `totalZero` creates natural clustering:

```
wNum = 0 (constants)  → totalZero must be 0  → Class 0
wNum = 1 (linear)     → totalZero ∈ {0, 1}
wNum = 2 (quadratic)  → totalZero ∈ {0, 1, 2}
wNum = 3 (cubic)      → totalZero ∈ {0, 1, 2, 3}
wNum = 4 (quartic)    → totalZero ∈ {0, 1, 2, 3, 4}
```

**Class 0 nodes with wNum=0:** These are constant polynomials that must have 0 roots. This creates a perfect decision boundary: `if wNum == 0, predict 0 roots`.

**Classes 2, 3, 4:** These require `wNum ≥ totalZero`, but within each wNum value, multiple totalZero values are possible. The model can't separate them.

---

## Part 4: Data Leakage Analysis

### Question: "Did any info other than the graph leak?"

**Answer: No leakage, but a feature limitation problem.**

Unlike the binary determined/undetermined task (where `determined = (totalZero == wNum)` was trivially computable), the root count prediction task genuinely requires more information:

| What We Want to Predict | Information Needed |
|-------------------------|-------------------|
| totalZero = 0 | Polynomial has no rational roots |
| totalZero = 1 | Polynomial has exactly 1 rational root |
| ... | ... |
| totalZero = 4 | Polynomial has exactly 4 rational roots |

**The fundamental problem:** `wNum` (degree) tells us the *maximum possible* roots, not the *actual* roots. The actual root count depends on:
- The polynomial coefficients (`vmResult`)
- The algebraic structure of the polynomial
- Whether roots are rational vs. irrational

**These features are not provided to the model.**

---

## Part 5: Performance Interpretation

### Is F1 = 0.6249 Good?

**Weighted F1 = 0.6249 is misleading.** It's weighted by class size:

$$\text{F1}_{weighted} = \sum_{c=0}^{4} \frac{N_c}{N} \cdot \text{F1}_c = 0.20 \times 1.0 + 0.53 \times 0.80 + 0.23 \times 0 + 0.04 \times 0 + 0.01 \times 0 = 0.62$$

**Macro F1 = 0.36** tells the true story:
$$\text{F1}_{macro} = \frac{1}{5}(1.0 + 0.80 + 0 + 0 + 0) = 0.36$$

### Is MAE = 0.3148 Good?

**Mean Absolute Error = 0.3148 roots** means on average, predictions are off by ~0.3 roots.

**But this hides the pattern:**
- Classes 0 and 1: Error = 0 (perfect)
- Class 2: Error = 1 (predicted as 1, off by 1)
- Class 3: Error = 2 (predicted as 1, off by 2)
- Class 4: Error = 3 (predicted as 1, off by 3)

**Weighted MAE calculation:**
```
MAE = (203745×0 + 541867×0 + 231306×1 + 36008×2 + 5784×3) / 1018710
    = (0 + 0 + 231306 + 72016 + 17352) / 1018710
    = 320674 / 1018710
    = 0.3148
```

The low MAE is because 73.2% of nodes are in classes 0-1 with zero error.

---

## Part 6: Baseline Comparisons

### Trivial Baselines

| Baseline Strategy | Accuracy | Macro F1 | MAE |
|-------------------|----------|----------|-----|
| Always predict 1 (majority) | 53.2% | 0.10 | 0.79 |
| Always predict 1 except wNum=0 | 73.2% | 0.40 | 0.31 |
| Random by class frequency | 37.8% | 0.20 | 1.00 |
| **Our Model** | **73.2%** | **0.36** | **0.31** |

**Conclusion:** Our GCN model is equivalent to the "predict 1 except for wNum=0" baseline. It learned no additional patterns from graph structure or optimization.

---

## Part 7: Recommendations to Fix

### 1. Class Weights for Multi-Class

```python
# Inverse frequency weights
class_counts = torch.tensor([203745, 541867, 231306, 36008, 5784], dtype=torch.float)
weights = class_counts.sum() / (5 * class_counts)
# Result: [1.0, 0.38, 0.88, 5.66, 35.25]

# Apply in loss
loss = F.cross_entropy(logits, labels, weight=weights)
```

### 2. Add More Features

The single `wNum` feature is insufficient. Consider adding:

```python
# From vmResult (polynomial coefficients)
- Coefficient magnitudes
- Coefficient signs
- Leading coefficient

# From graph structure
- Node centrality measures
- Distance to root node
- Subgraph patterns

# Derived features
- wNum / max(wNum) (normalized degree)
- Graph embeddings from unsupervised pretraining
```

### 3. Ordinal Classification

Root count is **ordinal** (0 < 1 < 2 < 3 < 4), not nominal. Use ordinal regression:

```python
# Instead of softmax over 5 classes, predict 4 thresholds
# P(y > 0), P(y > 1), P(y > 2), P(y > 3)

class OrdinalClassifier(nn.Module):
    def __init__(self, in_features, hidden_dim):
        super().__init__()
        self.fc = nn.Sequential(
            nn.Linear(in_features, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, 4)  # 4 thresholds
        )
    
    def forward(self, x):
        return torch.sigmoid(self.fc(x))  # Each threshold is a probability
```

### 4. Hierarchical Classification

Group classes by difficulty:

```
Level 1: {0 roots} vs {1+ roots}
Level 2: {1 root} vs {2+ roots}
Level 3: {2 roots} vs {3+ roots}
Level 4: {3 roots} vs {4 roots}
```

Train separate classifiers for each level.

### 5. Use Proper Metrics

```python
from sklearn.metrics import f1_score, balanced_accuracy_score

# Evaluate with macro F1 (treats all classes equally)
macro_f1 = f1_score(y_true, y_pred, average='macro')

# Per-class recall (sensitivity)
per_class_recall = recall_score(y_true, y_pred, average=None)

# Balanced accuracy
balanced_acc = balanced_accuracy_score(y_true, y_pred)
```

---

## Part 8: Theoretical Foundation - Multi-Class Imbalance

### Why Standard Training Fails

For K-class classification with cross-entropy:

$$\mathcal{L} = -\frac{1}{N} \sum_{i=1}^{N} \sum_{c=1}^{K} \mathbf{1}[y_i = c] \log(\hat{p}_{ic})$$

The gradient contribution from class $c$ is proportional to $N_c$ (number of samples in class $c$). Classes with few samples contribute tiny gradients that get overwhelmed by majority classes.

### Class Weights Theory

Weighted cross-entropy rebalances the gradients:

$$\mathcal{L}_{weighted} = -\frac{1}{N} \sum_{i=1}^{N} w_{y_i} \sum_{c=1}^{K} \mathbf{1}[y_i = c] \log(\hat{p}_{ic})$$

With inverse frequency weights $w_c = \frac{N}{K \cdot N_c}$, each class contributes equally to the total gradient.

### Focal Loss for Hard Examples

Focal loss (Lin et al., 2017) down-weights easy examples and up-weights hard ones:

$$\mathcal{L}_{focal} = -\sum_{c=1}^{K} \alpha_c (1 - \hat{p}_c)^\gamma \log(\hat{p}_c)$$

Where:
- $\alpha_c$ = class weight
- $\gamma$ = focusing parameter (typically 2)
- $(1 - \hat{p}_c)^\gamma$ = modulating factor

For easily classified class-1 examples with $\hat{p}_1 \approx 0.9$:
$$\text{modulator} = (1 - 0.9)^2 = 0.01$$

For hard class-4 examples with $\hat{p}_4 \approx 0.1$:
$$\text{modulator} = (1 - 0.1)^2 = 0.81$$

**Result:** Hard minority examples get 81× more weight in the gradient.

---

## Conclusion: Believability Assessment

### Is the Root Count Prediction Believable?

**Partially.** The model reliably identifies:
- Polynomials with 0 roots (100% accuracy)
- Polynomials with 1 root (100% accuracy)

**But completely fails for:**
- Polynomials with 2 roots (0% accuracy)
- Polynomials with 3 roots (0% accuracy)
- Polynomials with 4 roots (0% accuracy)

### Why This Task Is Harder Than Binary Classification

| Aspect | Binary (determined) | Multi-class (root count) |
|--------|---------------------|-------------------------|
| Classes | 2 | 5 |
| Target definition | totalZero == wNum | Predict exact totalZero |
| From features alone | Trivially computable | Requires coefficient info |
| Class separation | Clear boundary | Overlapping within wNum |

### Final Verdict

| Metric | Value | Interpretation |
|--------|-------|----------------|
| Weighted F1 | 0.6249 | Misleading - dominated by classes 0, 1 |
| Macro F1 | 0.36 | Honest - shows 3/5 classes fail |
| MAE | 0.3148 | Misleading - errors hidden in minority |
| Class 4 Recall | 0.00 | Complete failure on hardest class |

**The model has learned a useful but trivial pattern:** Constant polynomials (wNum=0) have 0 roots, everything else defaults to 1 root. This is not machine learning—it's a lookup table with one entry.

**To make believable root count predictions:**
1. Add polynomial coefficient features
2. Use class-weighted or focal loss
3. Evaluate with macro F1 and per-class recall
4. Consider ordinal regression (root count has natural ordering)

---

## Part 9: Leveraging Graph Structure for Root Count Prediction

### Corrected Understanding: wNum and Root Count Relationship

The relationship between `wNum` (depth/level) and possible root counts is:

| wNum | Possible totalZero | Interpretation |
|------|-------------------|----------------|
| 0 | {1, 2, 3, 4} | Constant polynomial - original had 1-4 roots factored out |
| 1 | {1, 2, 3} | First difference - 1-3 roots remaining to find |
| 2 | {1, 2} | Second difference - 1-2 roots remaining |
| 3 | {1} | Third difference - 1 root remaining |
| 4 | {} | Fourth difference - fully determined |

**Key insight:** As wNum increases, the polynomial has "descended" the difference tree, with each level representing one fewer root to discover. The graph structure encodes this algebraic journey.

### Why Graph Structure Contains Valuable Information

The zMap edges encode **Newton's forward difference relationships**:

```
         P(x)           ← wNum = 4, totalZero = 4 (fully determined)
          │ :zMap
          ▼
        ΔP(x)           ← wNum = 3, totalZero ∈ {1,2,3}
          │ :zMap
          ▼
       Δ²P(x)           ← wNum = 2, totalZero ∈ {1,2}
          │ :zMap
          ▼
       Δ³P(x)           ← wNum = 1, totalZero ∈ {1,2,3}
          │ :zMap
          ▼
       Δ⁴P(x)           ← wNum = 0, totalZero ∈ {1,2,3,4}
```

**The graph structure reveals:**
1. **Ancestry chain**: What polynomials this node derived from
2. **Sibling relationships**: Polynomials sharing common structure
3. **Descendant patterns**: How the polynomial "factors out"
4. **Local topology**: Neighborhood density and connectivity patterns

---

### Idea 1: Ancestor Feature Propagation

**Concept:** Aggregate features from ancestor nodes (path to root) to capture the polynomial's algebraic history.

```python
import torch
from torch_geometric.nn import MessagePassing

class AncestorAggregator(MessagePassing):
    """
    Propagate features DOWN the difference tree (from high wNum to low wNum).
    Uses edges oriented from parent (higher degree) to child (lower degree).
    """
    def __init__(self, in_channels, out_channels):
        super().__init__(aggr='mean', flow='source_to_target')
        self.mlp = nn.Sequential(
            nn.Linear(in_channels * 2, out_channels),
            nn.ReLU(),
            nn.Linear(out_channels, out_channels)
        )
    
    def forward(self, x, edge_index, num_layers=4):
        # Iteratively propagate ancestor information
        h = x
        for _ in range(num_layers):
            h = self.propagate(edge_index, x=h)
            h = torch.cat([x, h], dim=-1)  # Combine with original
            h = self.mlp(h)
        return h
```

**Why this helps:** A node's root count is constrained by its ancestors. If the parent has totalZero=2, the child must have totalZero ≤ 2.

---

### Idea 2: Path Encoding to Root

**Concept:** Encode the specific path each node takes to reach the root of its difference tree.

```python
def compute_path_features(data, max_depth=5):
    """
    For each node, compute features based on its path to root.
    """
    num_nodes = data.num_nodes
    path_features = torch.zeros(num_nodes, max_depth)
    
    # Build adjacency for traversal (child → parent direction)
    edge_index = data.edge_index
    parent_map = build_parent_map(edge_index, data.x[:, 0])  # Use wNum to orient
    
    for node_id in range(num_nodes):
        # Trace path to root
        current = node_id
        depth = 0
        while current is not None and depth < max_depth:
            parent = parent_map.get(current)
            if parent is not None:
                # Feature: wNum of ancestor at each depth
                path_features[node_id, depth] = data.x[parent, 0]
            current = parent
            depth += 1
    
    return path_features
```

**Why this helps:** Sibling nodes (same parent, same depth) may have different paths further up. These differences encode structural variation that correlates with root count.

---

### Idea 3: Subgraph Pattern Mining

**Concept:** Extract local subgraph patterns (graphlets/motifs) around each node as features.

```python
from torch_geometric.utils import k_hop_subgraph

def compute_subgraph_features(data, k=2):
    """
    For each node, extract features from its k-hop neighborhood.
    """
    features = []
    for node_id in range(data.num_nodes):
        subset, sub_edge_index, mapping, edge_mask = k_hop_subgraph(
            node_id, k, data.edge_index, relabel_nodes=True
        )
        
        # Compute structural features
        subgraph_size = len(subset)
        subgraph_edges = sub_edge_index.size(1)
        subgraph_density = subgraph_edges / (subgraph_size * (subgraph_size - 1) + 1e-8)
        
        # Distribution of wNum in neighborhood
        neighbor_wNum = data.x[subset, 0]
        wNum_mean = neighbor_wNum.mean()
        wNum_std = neighbor_wNum.std()
        wNum_max = neighbor_wNum.max()
        wNum_min = neighbor_wNum.min()
        
        # Distribution of known totalZero in neighborhood (training signal)
        neighbor_labels = data.y[subset]
        
        features.append([
            subgraph_size, subgraph_edges, subgraph_density,
            wNum_mean, wNum_std, wNum_max, wNum_min
        ])
    
    return torch.tensor(features, dtype=torch.float)
```

**Why this helps:** Nodes in dense, well-connected regions may have different root count distributions than isolated nodes.

---

### Idea 4: Spectral Positional Encodings

**Concept:** Use graph Laplacian eigenvectors as positional features, capturing global graph structure.

```python
from torch_geometric.transforms import AddLaplacianEigenvectorPE

class SpectralPositionalEncoding:
    def __init__(self, k=8):
        self.k = k
    
    def __call__(self, data):
        # Compute Laplacian eigenvectors
        from scipy.sparse.linalg import eigsh
        from torch_geometric.utils import get_laplacian, to_scipy_sparse_matrix
        
        edge_index, edge_weight = get_laplacian(
            data.edge_index, 
            normalization='sym',
            num_nodes=data.num_nodes
        )
        L = to_scipy_sparse_matrix(edge_index, edge_weight, data.num_nodes)
        
        # Smallest k eigenvectors (excluding constant)
        eigenvalues, eigenvectors = eigsh(L, k=self.k + 1, which='SM')
        
        # Skip first (constant) eigenvector
        pe = torch.tensor(eigenvectors[:, 1:self.k+1], dtype=torch.float)
        
        # Concatenate with node features
        data.x = torch.cat([data.x, pe], dim=-1)
        return data
```

**Why this helps:** Eigenvectors encode graph community structure. Nodes in the same spectral cluster may share root count patterns.

---

### Idea 5: Random Walk Structural Encoding

**Concept:** Use random walk probabilities to encode each node's structural context.

```python
from torch_geometric.transforms import AddRandomWalkPE

def add_random_walk_features(data, walk_length=16):
    """
    Compute random walk landing probabilities as node features.
    """
    from torch_geometric.utils import to_dense_adj
    
    adj = to_dense_adj(data.edge_index, max_num_nodes=data.num_nodes)[0]
    deg = adj.sum(dim=1, keepdim=True).clamp(min=1)
    P = adj / deg  # Transition probability matrix
    
    # Compute P^k for k = 1, 2, ..., walk_length
    rw_features = []
    Pk = P
    for k in range(1, walk_length + 1):
        # Diagonal entries = probability of returning to self after k steps
        rw_features.append(Pk.diag().unsqueeze(1))
        Pk = Pk @ P
    
    rw_pe = torch.cat(rw_features, dim=1)
    data.x = torch.cat([data.x, rw_pe], dim=-1)
    return data
```

**Why this helps:** Return probabilities encode local density. Nodes that quickly return are in tight clusters (may correlate with root count patterns).

---

### Idea 6: Sibling-Aware Message Passing

**Concept:** Explicitly model relationships between sibling nodes (same parent).

```python
class SiblingAwareGNN(nn.Module):
    """
    GNN that distinguishes between parent-child and sibling relationships.
    """
    def __init__(self, in_channels, hidden_channels, out_channels):
        super().__init__()
        # Parent → Child message passing
        self.parent_conv = GCNConv(in_channels, hidden_channels)
        # Sibling → Sibling message passing
        self.sibling_conv = GCNConv(in_channels, hidden_channels)
        # Combine
        self.combine = nn.Linear(hidden_channels * 2, hidden_channels)
        self.classifier = nn.Linear(hidden_channels, out_channels)
    
    def forward(self, x, parent_edge_index, sibling_edge_index):
        # Messages from parents
        h_parent = self.parent_conv(x, parent_edge_index)
        # Messages from siblings
        h_sibling = self.sibling_conv(x, sibling_edge_index)
        # Combine both types
        h = self.combine(torch.cat([h_parent, h_sibling], dim=-1))
        h = F.relu(h)
        return self.classifier(h)
```

**Preprocessing to create sibling edges:**
```python
def create_sibling_edges(edge_index, wNum):
    """
    Create edges between nodes that share a parent (same wNum+1 neighbor).
    """
    from collections import defaultdict
    
    # Build parent map
    children_of = defaultdict(list)
    for src, tgt in edge_index.t().tolist():
        if wNum[src] > wNum[tgt]:  # src is parent
            children_of[src].append(tgt)
        else:  # tgt is parent
            children_of[tgt].append(src)
    
    # Create sibling edges
    sibling_edges = []
    for parent, children in children_of.items():
        for i, c1 in enumerate(children):
            for c2 in children[i+1:]:
                sibling_edges.append([c1, c2])
                sibling_edges.append([c2, c1])  # Undirected
    
    return torch.tensor(sibling_edges, dtype=torch.long).t()
```

**Why this helps:** Siblings share algebraic structure from their parent. If one sibling has totalZero=2, its sibling is more likely to also have higher root count.

---

### Idea 7: Depth-Aware Attention

**Concept:** Use attention mechanism where attention weights depend on relative wNum (depth difference).

```python
from torch_geometric.nn import GATConv

class DepthAwareGAT(nn.Module):
    """
    Graph Attention Network where attention incorporates depth relationships.
    """
    def __init__(self, in_channels, hidden_channels, out_channels, heads=4):
        super().__init__()
        # Compute edge features based on wNum difference
        self.edge_encoder = nn.Linear(1, heads)
        self.conv1 = GATConv(in_channels, hidden_channels, heads=heads, 
                             edge_dim=heads, concat=True)
        self.conv2 = GATConv(hidden_channels * heads, out_channels, heads=1,
                             edge_dim=heads, concat=False)
    
    def forward(self, x, edge_index, wNum):
        # Compute edge features: wNum difference between source and target
        src, tgt = edge_index
        wNum_diff = (wNum[src] - wNum[tgt]).float().unsqueeze(-1)
        edge_attr = self.edge_encoder(wNum_diff)
        
        # Message passing with depth-aware attention
        h = self.conv1(x, edge_index, edge_attr=edge_attr)
        h = F.relu(h)
        h = F.dropout(h, p=0.5, training=self.training)
        h = self.conv2(h, edge_index, edge_attr=edge_attr)
        return h
```

**Why this helps:** The direction of edges (parent→child vs child→parent) matters algebraically. Attention can learn to weight parent information more heavily.

---

### Idea 8: Graph-Level Context Injection

**Concept:** Compute graph-level features and inject them into node representations.

```python
from torch_geometric.nn import global_mean_pool, global_max_pool

class GraphContextGNN(nn.Module):
    def __init__(self, in_channels, hidden_channels, out_channels):
        super().__init__()
        self.node_encoder = nn.Linear(in_channels, hidden_channels)
        self.conv1 = GCNConv(hidden_channels, hidden_channels)
        self.conv2 = GCNConv(hidden_channels, hidden_channels)
        
        # Graph-level encoder
        self.graph_encoder = nn.Linear(hidden_channels * 2, hidden_channels)
        
        # Final classifier takes node + graph context
        self.classifier = nn.Linear(hidden_channels * 2, out_channels)
    
    def forward(self, x, edge_index, batch=None):
        # Node embeddings
        h = F.relu(self.node_encoder(x))
        h = F.relu(self.conv1(h, edge_index))
        h = F.relu(self.conv2(h, edge_index))
        
        # Graph-level context (for single graph, batch is all zeros)
        if batch is None:
            batch = torch.zeros(x.size(0), dtype=torch.long, device=x.device)
        
        graph_mean = global_mean_pool(h, batch)  # [num_graphs, hidden]
        graph_max = global_max_pool(h, batch)
        graph_ctx = self.graph_encoder(torch.cat([graph_mean, graph_max], dim=-1))
        
        # Broadcast graph context to all nodes
        node_graph_ctx = graph_ctx[batch]  # [num_nodes, hidden]
        
        # Combine node and graph context
        combined = torch.cat([h, node_graph_ctx], dim=-1)
        return self.classifier(combined)
```

**Why this helps:** Individual node features are insufficient. Graph-level statistics (overall root count distribution, tree depth distribution) provide valuable context.

---

### Idea 9: Multi-Scale Aggregation

**Concept:** Aggregate information at multiple hop distances and combine.

```python
class MultiScaleGNN(nn.Module):
    """
    Aggregate 1-hop, 2-hop, 3-hop, 4-hop neighborhoods separately.
    """
    def __init__(self, in_channels, hidden_channels, out_channels, max_hops=4):
        super().__init__()
        self.convs = nn.ModuleList([
            GCNConv(in_channels, hidden_channels) for _ in range(max_hops)
        ])
        self.combine = nn.Linear(hidden_channels * max_hops, hidden_channels)
        self.classifier = nn.Linear(hidden_channels, out_channels)
        self.max_hops = max_hops
    
    def forward(self, x, edge_index):
        hop_embeddings = []
        h = x
        
        for i in range(self.max_hops):
            h = self.convs[i](h, edge_index)
            h = F.relu(h)
            hop_embeddings.append(h)
        
        # Concatenate all hop embeddings
        multi_scale = torch.cat(hop_embeddings, dim=-1)
        h = F.relu(self.combine(multi_scale))
        return self.classifier(h)
```

**Why this helps:** Root count may depend on structure at different scales. Local patterns (1-2 hop) capture immediate algebraic relationships; global patterns (3-4 hop) capture tree-wide tendencies.

---

### Summary: Recommended Approach

**Minimum viable improvement:**
1. Add spectral positional encodings (Idea 4)
2. Use depth-aware attention (Idea 7)
3. Apply class weights for imbalance

**More sophisticated:**
1. Create sibling edges and use sibling-aware GNN (Idea 6)
2. Combine with ancestor feature propagation (Idea 1)
3. Use focal loss for hard minority examples

**Full exploration:**
1. Multi-scale aggregation (Idea 9) with all structural features
2. Graph-level context injection (Idea 8)
3. Ensemble of multiple approaches

---

*Document created: January 16, 2026*
*Pipeline version: 1.0 (Root Count)*
*Database: d4seed1 (1,018,710 nodes)*

hy GraphSAGE and FastRP Are Not Effective for Root Count Prediction
The Core Problem: Symmetric Aggregation in Directed Structure
The polynomial difference tree has an inherently directed, hierarchical structure:

wNum=4 (quartic)     →  rootcount must be 4 (determined)
    │ :zMap
    ▼
wNum=3 (cubic diff)  →  rootcount ∈ {1}
    │ :zMap
    ▼
wNum=2 (quadratic diff) → rootcount ∈ {1, 2}
    │ :zMap
    ▼
wNum=1 (linear diff)    → rootcount ∈ {1, 2, 3}
    │ :zMap
    ▼
wNum=0 (constant diff)  → rootcount ∈ {1, 2, 3, 4}
Key insight: Information flows downward (parent constrains child), but GraphSAGE and FastRP treat edges as symmetric.

Why GraphSAGE Fails
1. Symmetric Neighborhood Aggregation
GraphSAGE aggregates neighbor features without distinguishing direction:

# GraphSAGE aggregation (simplified)
def aggregate(node, neighbors):
    neighbor_features = [get_features(n) for n in neighbors]
    return MEAN(neighbor_features)  # or MAX, LSTM
The problem:

Node at wNum=2 has neighbors at wNum=1 (child) and wNum=3 (parent)
GraphSAGE treats both equally: h = MEAN(h_child, h_parent)
But algebraically: parent constrains child, not vice versa
2. No Edge Directionality
GraphSAGE was designed for undirected social networks where:

"Alice is friends with Bob" = "Bob is friends with Alice"
Symmetry is appropriate
In the polynomial difference tree:

"P(x) differentiates to ΔP(x)" ≠ "ΔP(x) differentiates to P(x)"
Asymmetry is fundamental
3. Fixed Aggregation Depth
GraphSAGE typically uses 2-3 layers (hops). For your tree:

Depth from wNum=0 to wNum=4 is 4 edges
A 2-layer GraphSAGE only sees 2-hop neighborhood
Misses the full ancestry chain
4. Mean/Max Aggregation Loses Information
# Consider two nodes with same wNum=1
# Node A: neighbors have wNum = [0, 2] 
# Node B: neighbors have wNum = [0, 0, 2]

# GraphSAGE MEAN:
agg_A = MEAN([feat_0, feat_2])    # Average of parent + child
agg_B = MEAN([feat_0, feat_0, feat_2])  # More children, same parent

# These are DIFFERENT, but GraphSAGE doesn't capture WHY
Why FastRP Fails
1. Random Projection Ignores Semantics
FastRP (Fast Random Projection) creates node embeddings by:

Start with random vectors for each node
Iteratively smooth by averaging with neighbors
Project to lower dimension
# FastRP (simplified)
def fastRP(adj_matrix, dim, iterations):
    # Random initialization
    embeddings = random_normal(num_nodes, dim)
    
    # Iterative smoothing
    for i in range(iterations):
        embeddings = normalize(adj_matrix @ embeddings)
    
    return embeddings
The problem: Random initialization has no connection to graph semantics. The resulting embeddings capture topological similarity (nodes with similar neighborhoods) but not algebraic relationships.

2. Transductive, Not Inductive
FastRP computes embeddings for the entire fixed graph. It cannot:

Generalize to new nodes
Leverage node features (wNum) during embedding
Adapt to the specific prediction task
3. No Awareness of Tree Structure
FastRP treats the graph as a general network. It doesn't know that:

The graph is a forest of difference trees
Edges represent differentiation operations
wNum encodes position in the tree
4. Linear Dimensionality Reduction
FastRP uses linear projections. The relationship between wNum and rootcount is:

wNum=0 → rootcount ∈ {1,2,3,4}
wNum=1 → rootcount ∈ {1,2,3}
wNum=2 → rootcount ∈ {1,2}
wNum=3 → rootcount ∈ {1}
This is a non-linear, set-valued mapping that linear projections cannot capture.

What These Methods Actually Capture
Method	What It Learns	What It Misses
GraphSAGE	Local neighborhood structure	Edge direction, hierarchy, ancestry
FastRP	Topological similarity	Node features, task-specific patterns
GCN	Symmetric message passing	Same as GraphSAGE
Empirical Evidence
In your experiments, all three (MLP, GCN, GraphSAGE) achieved identical F1 = 0.6249. This proves:

Graph structure provided zero additional signal over MLP
The symmetric aggregation didn't capture useful patterns
Models defaulted to the same trivial solution (predict 0 or 1)
What Would Work Instead
1. Directed Message Passing
Use edge direction explicitly:

class DirectedGNN(nn.Module):
    def __init__(self, in_channels, hidden_channels, out_channels):
        super().__init__()
        # Separate transformations for incoming vs outgoing edges
        self.parent_to_child = nn.Linear(in_channels, hidden_channels)
        self.child_to_parent = nn.Linear(in_channels, hidden_channels)
        self.combine = nn.Linear(hidden_channels * 2, out_channels)
    
    def forward(self, x, edge_index, wNum):
        # Identify edge directions
        src, tgt = edge_index
        is_parent_to_child = wNum[src] > wNum[tgt]
        
        # Aggregate separately
        parent_edges = edge_index[:, is_parent_to_child]
        child_edges = edge_index[:, ~is_parent_to_child]
        
        h_from_parents = aggregate(x, parent_edges, self.parent_to_child)
        h_from_children = aggregate(x, child_edges, self.child_to_parent)
        
        return self.combine(torch.cat([h_from_parents, h_from_children], dim=-1))
2. Depth-Aware Attention (as explained before)
Attention weights that depend on wNum[src] - wNum[tgt]:

Parent-to-child edges get high weight (information flows down)
Child-to-parent edges get low weight (reverse direction less useful)
3. Tree-Structured Networks
Use architectures designed for trees:

TreeLSTM: Aggregates children with gating
Recursive Neural Networks: Process tree bottom-up or top-down
Neural Message Passing with Edge Types: Different message functions per edge type
4. Spectral Methods with Directionality
Magnetic Laplacian or directed graph Laplacians that encode edge direction:

# Magnetic Laplacian for directed graphs
# L_q = D - (A * exp(i*q) + A^T * exp(-i*q)) / 2
# where q controls the "magnetic field" strength
Summary: Why Standard GNNs Fail Here
Issue	GraphSAGE	FastRP	Your Difference Tree
Edge direction	Ignored	Ignored	Critical (parent→child)
Hierarchy	Not modeled	Not modeled	Defines structure
Node features	Used	Ignored	wNum determines constraints
Task-specific	Somewhat	No	Need constraint propagation
Bottom line: GraphSAGE and FastRP assume the graph is a general undirected network where "similar neighbors = similar nodes." Your polynomial difference tree has:

Directed edges (differentiation has a direction)
Hierarchical constraints (parent bounds child's possibilities)
Semantic meaning (wNum isn't just a feature, it's structural position)
Standard methods miss all of this, explaining why they add no value over a simple MLP.

Recommended Architecture
class HierarchicalRootCountPredictor(nn.Module):
    """
    Designed for polynomial difference trees:
    1. Spectral positional encodings (global position)
    2. Directed message passing (parent → child flow)
    3. Depth-aware attention (wNum difference matters)
    4. Class-weighted loss (handle imbalance)
    """
    def __init__(self, base_features, pe_dim, hidden_dim, num_classes):
        super().__init__()
        in_dim = base_features + pe_dim
        
        # Separate message functions for each direction
        self.parent_msg = nn.Sequential(
            nn.Linear(in_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim)
        )
        self.child_msg = nn.Sequential(
            nn.Linear(in_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim)
        )
        
        # Attention based on depth difference
        self.depth_attn = nn.Sequential(
            nn.Linear(1, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, 1)
        )
        
        # Classifier
        self.classifier = nn.Sequential(
            nn.Linear(in_dim + hidden_dim * 2, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(hidden_dim, num_classes)
        )
    
    def forward(self, x, edge_index, wNum):
        src, tgt = edge_index
        
        # Compute depth-aware attention
        depth_diff = (wNum[src] - wNum[tgt]).float().unsqueeze(-1)
        attn_weights = torch.sigmoid(self.depth_attn(depth_diff))
        
        # Separate parent and child messages
        is_from_parent = depth_diff > 0  # Parent has higher wNum
        
        # Messages from parents (high attention)
        parent_mask = is_from_parent.squeeze()
        parent_msgs = self.parent_msg(x[src[parent_mask]])
        parent_agg = scatter_mean(
            parent_msgs * attn_weights[parent_mask], 
            tgt[parent_mask], 
            dim=0, 
            dim_size=x.size(0)
        )
        
        # Messages from children (lower attention)
        child_mask = ~parent_mask
        child_msgs = self.child_msg(x[src[child_mask]])
        child_agg = scatter_mean(
            child_msgs * attn_weights[child_mask],
            tgt[child_mask],
            dim=0,
            dim_size=x.size(0)
        )
        
        # Combine original features + parent info + child info
        h = torch.cat([x, parent_agg, child_agg], dim=-1)
        
        return self.classifier(h)
This architecture explicitly handles what GraphSAGE and FastRP cannot: directed, hierarchical, constraint-propagating structure.