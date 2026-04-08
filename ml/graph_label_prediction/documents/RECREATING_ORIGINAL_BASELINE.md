# Recreating the Original GraphicLablePrediction.json Baseline

## What Was Used in the Original 2021 Notebook

### 1. The Graph/Subgraph

**From GraphicLablePrediction.json line 426:**
```
nodeCount: 384
relationshipCount: 766
createMillis: 96ms
```

**Database:** `"neo4j"` (different from your current `d4seed1`)

**The graph was NOT filtered** - it was the **entire database** at that time. The original run of `ZerosAndDifferences.jar` simply generated a smaller graph (384 nodes) based on algorithm parameters:
- Likely dimension=3 or 4 (polynomial degree)
- Likely integerRange=20-50 (evaluation range)
- Likely setProductRange=20 (coefficient exploration)

Your current `d4seed1` database (1M+ nodes) was generated with larger parameters.

---

### 2. The Features

**From line 353:**
```cypher
CALL gds.graph.create(
    'myGraph',
    {
        Dnode: {
            label: 'Dnode',
            properties: ['determined', 'zero', 'one', 'two', 'three', 'wNum', 'totalZero']
        }
    },
    ...
)
```

**7 properties loaded**, but the successful model used:
```python
featureProperties: ['zero', 'one', 'two', 'three', 'four', 'wNum', 'totalZero']
```

**These are one-hot encoded wNum** (as we determined earlier):
- `zero` = 1 if wNum==0, else 0
- `one` = 1 if wNum==1, else 0
- `two` = 1 if wNum==2, else 0
- `three` = 1 if wNum==3, else 0
- `four` = 1 if wNum==4, else 0
- `wNum` = raw depth value
- `totalZero` = root count

**7 total features** (5 one-hot + wNum + totalZero, with redundancy)

---

### 3. The Training Pipeline

**From lines 653-670:**
```cypher
CALL gds.alpha.ml.nodeClassification.train('myGraph', {
  nodeLabels: ['Dnode'],
  modelName: 'determined-model',
  featureProperties: ['zero','one','two','three','four','wNum','totalZero'],
  targetProperty: 'determined',
  randomSeed: 49,
  holdoutFraction: 0.2,
  validationFolds: 185,
  metrics: ['F1_WEIGHTED'],
  params: [
    {penalty: 0.0625},
    {penalty: 0.5},
    {penalty: 1.0},
    {penalty: 4.0}
  ]
})
```

**Algorithm:** Neo4j GDS Node Classification = **Logistic Regression**
- Binary classification (determined=0 or 1)
- L2 regularization (penalty parameter)
- 185-fold cross-validation (nearly leave-one-out with 384 nodes)
- Grid search over 4 penalty values
- Best model: penalty = 0.0625

**Results:**
```
Train F1: 0.8295
Test F1: 0.7923
```

---

### 4. Why It Worked Better Than Current Attempts

| Factor | Original 2021 | Current d4seed1 | Impact |
|--------|---------------|-----------------|--------|
| **Graph size** | 384 nodes | 1,018,710 nodes | Smaller = easier |
| **Features** | 7 (with one-hot wNum) | 2 (wNum, totalZero) | More informative |
| **Task** | Binary (determined) | Multi-class (root count) | Simpler |
| **Class balance** | Unknown (likely better) | 98.6% / 1.4% | Probably balanced |
| **Properties exist** | Yes (pre-computed) | No (need engineering) | Critical difference |

---

## Can This Be Recreated? YES

### Option 1: Recreate on Similar-Sized Filtered Graph

Use your filtering to get ~400-500 nodes and replicate the exact pipeline:

**Steps:**
1. Filter to get ~400 nodes (adjust PARRAY_MAX to 3 or 4)
2. Engineer one-hot wNum features
3. Train binary classifier on 'determined' target
4. Use sklearn LogisticRegressionCV with 185 folds
5. Compare to original F1 = 0.8295/0.7923

### Option 2: Generate a New Small Graph

Run `ZerosAndDifferences.jar` with small parameters:
```bash
dimension = 3
integerRange = 30
setProductRange = 20
```

This should generate a graph similar in size to the original 384 nodes.

---

## Implementation: Recreate Original Pipeline

I'll create a new notebook `recreate_original_baseline.ipynb` that:

### 1. Graph Selection
```python
# Filter to ~400 nodes using stricter pArrayList constraint
PARRAY_MIN = 0
PARRAY_MAX = 3  # Smaller range → fewer nodes
```

### 2. Feature Engineering
```python
def engineer_original_features(data):
    """
    Recreate the original 7-feature setup:
    zero, one, two, three, four (one-hot wNum), wNum, totalZero
    """
    wNum = data.x[:, 0]  # Extract wNum
    totalZero = data.y.float()  # For this task, we need both as features
    
    # One-hot encode wNum (5 categories: 0, 1, 2, 3, 4)
    wNum_onehot = torch.zeros(len(wNum), 5)
    for i, w in enumerate(wNum):
        if 0 <= w < 5:
            wNum_onehot[i, int(w)] = 1.0
    
    # Combine: [zero, one, two, three, four, wNum, totalZero]
    # But wait - for 'determined' task, totalZero is a feature, not target!
    # Target is: determined = (totalZero == wNum)
    
    # Need to query determined from database
    ...
```

**Wait - Critical Issue:** The original task was predicting **`determined`** (binary), not **`totalZero`** (multi-class). These are different tasks!

### 3. Binary Classification Setup
```python
# Target: determined (0 or 1)
# Features: zero, one, two, three, four, wNum, totalZero

from sklearn.linear_model import LogisticRegressionCV
from sklearn.metrics import f1_score

# Create feature matrix (7 features)
X = np.column_stack([
    zero_feature,    # wNum==0
    one_feature,     # wNum==1
    two_feature,     # wNum==2
    three_feature,   # wNum==3
    four_feature,    # wNum==4
    wNum,            # Raw depth
    totalZero        # Root count
])

# Target: determined (binary)
y = (totalZero == wNum).astype(int)

# Split 80/20
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=49, stratify=y
)

# Logistic regression with 185-fold CV
clf = LogisticRegressionCV(
    Cs=[1/4.0, 1/1.0, 1/0.5, 1/0.0625],  # Inverse of penalties
    cv=185,  # Nearly leave-one-out
    scoring='f1_weighted',
    random_state=49,
    max_iter=100
)

clf.fit(X_train, y_train)

# Evaluate
train_f1 = f1_score(y_train, clf.predict(X_train), average='weighted')
test_f1 = f1_score(y_test, clf.predict(X_test), average='weighted')

print(f"Train F1: {train_f1:.4f} (original: 0.8295)")
print(f"Test F1: {test_f1:.4f} (original: 0.7923)")
```

---

## Key Differences: Original vs Current Pipelines

| Aspect | Original (2021) | Current Implementations |
|--------|-----------------|-------------------------|
| **Task** | Binary: determined (0/1) | Multi-class: root count (0-4) |
| **Target** | determined = (totalZero == wNum) | totalZero itself |
| **Features** | 7 (one-hot wNum + raw wNum + totalZero) | 1-2 (wNum ± totalZero) |
| **Graph** | 384 nodes (small database) | 3,775 or 1M nodes (large database) |
| **Algorithm** | Neo4j GDS Logistic Regression | PyTorch GNNs |
| **CV Folds** | 185 (leave-one-out-like) | 5 (practical) |
| **Class balance** | Likely good | Very poor (98.6%/1.4% or 53%/6%) |

---

## Can We Recreate the Original Results?

**Yes, but need to:**

1. **Use the same task** (binary determined, not multi-class root count)
2. **Engineer the one-hot features** (zero, one, two, three, four from wNum)
3. **Use logistic regression** (not GNNs)
4. **Find or create a similar-sized graph** (~400 nodes with good balance)
5. **Query the 'determined' property** from your database

### Quick Check: Does Your Database Have the Determined Property?

```cypher
MATCH (d:Dnode)
WHERE d.determined IS NOT NULL
RETURN count(d) AS determined_count, 
       count(CASE WHEN d.determined = 1 THEN 1 END) AS determined_yes,
       count(CASE WHEN d.determined = 0 THEN 1 END) AS determined_no
```

If this returns data, we can recreate the original pipeline!

---

## Proposed New Notebook: `recreate_neo4j_gds_baseline.ipynb`

I can create a notebook that:

### Section 1: Query Determined Binary Target
```cypher
MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
WHERE d.determined IS NOT NULL
  AND all(x IN cb.pArrayList WHERE x >= 0 AND x < 5)
RETURN elementId(d), d.determined, cb.wNum, d.totalZero
```

### Section 2: Engineer One-Hot Features
```python
# Create zero, one, two, three, four from wNum
features_df['zero'] = (features_df['wNum'] == 0).astype(int)
features_df['one'] = (features_df['wNum'] == 1).astype(int)
features_df['two'] = (features_df['wNum'] == 2).astype(int)
features_df['three'] = (features_df['wNum'] == 3).astype(int)
features_df['four'] = (features_df['wNum'] == 4).astype(int)
```

### Section 3: Train Logistic Regression
```python
from sklearn.linear_model import LogisticRegressionCV

# Match Neo4j GDS configuration
model = LogisticRegressionCV(
    Cs=[1/p for p in [0.0625, 0.5, 1.0, 4.0]],  # Inverse penalties
    cv=min(185, len(y_train)),  # Can't exceed sample size
    scoring='f1_weighted',
    random_state=49,
    max_iter=100,
    penalty='l2',
    solver='lbfgs'
)
```

### Section 4: Compare Results
```python
print("Original Neo4j GDS (2021):")
print("  Train F1: 0.8295")
print("  Test F1: 0.7923")
print("  Graph: 384 nodes")
print("  Features: 7 (one-hot wNum)")

print("\nRecreated in Python:")
print(f"  Train F1: {train_f1:.4f}")
print(f"  Test F1: {test_f1:.4f}")
print(f"  Graph: {len(X)} nodes")
print(f"  Features: 7 (one-hot wNum)")
```

---

Would you like me to create this notebook to recreate the original baseline?

