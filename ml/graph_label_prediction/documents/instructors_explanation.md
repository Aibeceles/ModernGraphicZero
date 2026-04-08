# Instructor's Explanation: Graph Label Prediction Pipeline

## Executive Summary

**The model appears to achieve 97.89% F1 score but has actually learned nothing useful.** It predicts every single node as "under-determined" (class 0), exploiting severe class imbalance rather than learning meaningful patterns. This document explains what happened, why, and what it reveals about the believability of determined/undetermined prediction.

---

## Part 1: Cell-by-Cell Explanation

### Cells 0-6: Setup and Database Connection

**What happened:**
- Loaded Python libraries (PyTorch, PyTorch Geometric, scikit-learn)
- Connected to Neo4j database `d4seed1`
- Confirmed 152,196 Dnode nodes in the database

**Key observation:** The database has significantly more nodes (152K) than the original 2021 experiment (384 nodes). This is a completely different scale of data.

---

### Cells 7-9: Data Loading

**What happened:**
- Queried Neo4j for Dnode properties via CreatedBy relationship
- Extracted 2 features per node: `wNum` (polynomial degree) and `totalZero` (rational root count)
- Loaded edge connectivity (zMap relationships)

**Critical finding - Class Distribution:**
```
Under-determined (0): 150,053 nodes (98.59%)
Determined (1):        2,143 nodes (1.41%)
```

**Theory:** A polynomial is "determined" when `totalZero == wNum` (the number of rational roots equals the polynomial's degree). In this dataset, only 1.41% of polynomials are determined.

---

### Cells 10-16: Model Training

**What happened:**
- Split data: 121,756 training nodes (80%), 30,440 test nodes (20%)
- Trained 3 models: MLP (no graph), GCN, GraphSAGE
- All achieved identical F1 = 0.9789

**The alarming pattern:**
```
All models → F1 = 0.9789
All models → Early stopping at epoch 20
All models → Identical behavior
```

**What this reveals:** When all architectures (including MLP which ignores graph structure) achieve identical scores, the models are not learning from the features or structure—they're exploiting a statistical shortcut.

---

### Cells 17-19: Hyperparameter Tuning

**What happened:**
- Grid search over L2 penalties: [0.0625, 0.5, 1.0, 4.0]
- All penalties achieved identical F1 = 0.9789 ± 0.0004

**Interpretation:** The regularization strength has no effect because the model isn't learning anything that could be regularized—it's just predicting the majority class.

---

### Cells 20-22: Final Model & Classification Report

**What happened:**
- Trained final GCN model
- Generated detailed classification report

**The Smoking Gun - Classification Report:**
```
                  precision    recall  f1-score   support

Under-determined       0.99      1.00      0.99     30011
      Determined       0.00      0.00      0.00       429

        accuracy                           0.99     30440
```

**Translation:**
- For under-determined (98.6% of test): Perfect recall (1.00), catches all
- For determined (1.4% of test): **Zero precision, zero recall** - catches none
- The model predicts EVERY node as "under-determined"

---

### Cells 23-26: Prediction Analysis

**What happened:**
- Generated predictions for all 152,196 nodes
- Analyzed prediction distribution and confidence

**Devastating results:**
```
Predicted class distribution: {0: 152196}   ← ALL predicted as under-determined
True Positives (Determined correct): 0       ← Caught zero determined nodes
True Negatives (Under-determined correct): 150,053
False Negatives (Missed determined): 2,143   ← Missed ALL determined nodes
```

**Confidence analysis:**
- Mean confidence: 94.81%
- The model is very confident in its wrong predictions

---

## Part 2: Theoretical Analysis

### Why Did This Happen?

#### 1. Extreme Class Imbalance

The fundamental problem is that 98.59% of nodes are under-determined. A trivial model that always predicts "under-determined" achieves:
- 98.59% accuracy
- 97.89% weighted F1 (since weighted F1 is dominated by the majority class)

The neural network discovered this shortcut in the first few epochs and stopped learning anything else.

#### 2. Features May Be Insufficient

With only 2 features (`wNum`, `totalZero`), the model has limited information. The relationship is:
- `determined = 1` if `totalZero == wNum`
- `determined = 0` if `totalZero < wNum`

**In theory**, this should be perfectly predictable from the features! But with extreme imbalance, the gradient signal from the rare "determined" class is overwhelmed.

#### 3. Weighted F1 is Misleading

The weighted F1 score weights each class by its support (sample count):
```
F1_weighted ≈ 0.9859 × F1(class_0) + 0.0141 × F1(class_1)
            ≈ 0.9859 × 0.99 + 0.0141 × 0.00
            ≈ 0.9760
```

This metric **hides the complete failure** on the minority class.

---

## Part 3: Data Leakage Analysis

### Question: "Did any info other than the graph leak?"

**Answer: The relationship IS trivially computable, which is actually worse than leakage.**

The target variable `determined` is defined as:
```
determined = 1  if totalZero == wNum
determined = 0  if totalZero < wNum
```

Since `totalZero` and `wNum` are our features, **a simple comparison should achieve 100% accuracy**. The fact that we achieve only ~98.6% suggests either:

1. **Data quality issues**: Some nodes have incorrect `determined` labels
2. **Missing data**: Some nodes don't have the CreatedBy relationship (and thus no `wNum`)
3. **Feature extraction bugs**: Our query may not be getting the correct values

### Why This Isn't Traditional Leakage

Traditional data leakage means test data information flows into training. Here, the problem is different:

1. **The task is trivially solvable** from the features we have
2. **Class imbalance prevents the model from learning** the simple rule
3. **The model takes the lazy path** (always predict majority class)

### Is the Graph Structure Leaking Information?

No. The GCN and GraphSAGE models performed identically to the MLP (which ignores graph structure). This means:
- Graph structure provided no additional signal
- Or the imbalance was so severe that graph features were irrelevant

---

## Part 4: "Simply Too Likely to Predict Under-determined?"

**Yes, exactly.**

### The Math of Class Imbalance

With 98.59% under-determined:
- Random baseline accuracy: 98.59% (by always guessing majority)
- Our model's accuracy: 98.59%
- Our model learned: **nothing beyond guessing majority**

### Cross-Entropy Loss Dynamics

During training, the loss function sees:
- ~99 under-determined examples for every 1 determined example
- Gradient updates are dominated by the majority class
- The model minimizes loss by getting the majority class right

### Early Stopping Made It Worse

The model achieved peak F1 at epoch 10-20 (the majority class baseline), then early stopping kicked in because the training F1 wasn't improving. The model never had a chance to learn the minority class.

---

## Part 5: Comparison to Original 2021 Experiment

| Aspect | Original (2021) | Current (2026) |
|--------|-----------------|----------------|
| **Database** | `neo4j` | `d4seed1` |
| **Nodes** | 384 | 152,196 |
| **Features** | 7 (incl. one-hot wNum) | 2 (wNum, totalZero) |
| **Train F1** | 0.8295 | 0.9789 (fake) |
| **Test F1** | 0.7923 | 0.9789 (fake) |
| **Class balance** | Unknown (likely better) | 98.6% / 1.4% |

### Why Original Worked Better

1. **Smaller, curated dataset**: 384 nodes likely had better class balance
2. **More features**: One-hot encoding of wNum provided categorical structure
3. **Different data distribution**: The original graph may have been designed for this task

---

## Part 6: Recommendations

### To Fix This Pipeline

1. **Address class imbalance:**
   ```python
   # Option A: Class weights
   class_weights = torch.tensor([1.0, 70.0])  # 98.6/1.4 ratio
   loss = F.nll_loss(out, y, weight=class_weights)
   
   # Option B: Oversample minority class
   # Option C: Undersample majority class
   # Option D: SMOTE or similar
   ```

2. **Use appropriate metrics:**
   ```python
   # Use macro F1 (treats classes equally)
   f1_score(y_true, y_pred, average='macro')
   
   # Or balanced accuracy
   balanced_accuracy_score(y_true, y_pred)
   ```

3. **Add threshold tuning:**
   ```python
   # Instead of argmax, tune decision threshold
   threshold = 0.1  # Lower threshold for rare class
   pred = (probs[:, 1] > threshold).long()
   ```

4. **Verify the task makes sense:**
   - Why are 98.6% of polynomials under-determined?
   - Is this the expected distribution, or a data issue?
   - Should we be predicting something else?

### Fundamental Question

**Is this prediction task meaningful?**

If `determined = (totalZero == wNum)` and we have both features, then:
- A simple rule achieves 100% accuracy
- No ML is needed
- The task is trivially solvable

If the model can't learn this trivial rule due to imbalance, either:
1. Fix the imbalance (as above)
2. Or acknowledge the task is too easy for ML and use a simple comparison

---

## Conclusion: Believability Assessment

### Is the Model's Prediction of Determined/Undetermined Believable?

**No.** The model has zero ability to identify determined polynomials:
- 0% recall on determined class
- 0% precision on determined class
- Predicts everything as under-determined

### What Would Make It Believable?

1. **Balanced or weighted training** achieving >50% recall on minority class
2. **Macro F1 > 0.7** (not weighted F1 which hides problems)
3. **Non-trivial predictions** (some nodes predicted as determined)

### Final Verdict

| Claim | Verdict |
|-------|---------|
| "Model achieves 97.89% F1" | **Technically true, deeply misleading** |
| "Model can predict determined status" | **False** - predicts only one class |
| "Graph structure helps prediction" | **Unproven** - identical to MLP baseline |
| "Features are sufficient" | **Yes** - but imbalance prevents learning |

The pipeline executed correctly, but the results reveal a textbook example of **class imbalance defeating machine learning**. The high weighted F1 score is a statistical artifact, not evidence of learning.

---

## Part 7: Theoretical Foundations for Class Imbalance Correction

### 7.1 Why Class Imbalance Breaks Standard Training

#### The Loss Function Perspective

Neural networks are trained to minimize a loss function. For classification, we typically use **cross-entropy loss**:

$$\mathcal{L} = -\frac{1}{N}\sum_{i=1}^{N} \left[ y_i \log(\hat{p}_i) + (1-y_i) \log(1-\hat{p}_i) \right]$$

Where:
- $N$ = total samples
- $y_i$ = true label (0 or 1)
- $\hat{p}_i$ = predicted probability of class 1

**The problem**: Each sample contributes equally to the gradient. With 98.6% majority class:
- ~99 gradient signals push toward predicting class 0
- ~1 gradient signal pushes toward predicting class 1
- Net effect: model learns to always predict class 0

#### The Optimization Landscape

The loss surface has a **local minimum** at "always predict majority class" that is:
1. Easy to find (first few epochs)
2. Hard to escape (gradient from minority class is overwhelmed)
3. Has low loss value (correctly classifies 98.6% of samples)

---

### 7.2 Class Weights: Cost-Sensitive Learning Theory

#### Mathematical Foundation

Class weighting modifies the loss function to penalize mistakes on rare classes more heavily:

$$\mathcal{L}_{weighted} = -\frac{1}{N}\sum_{i=1}^{N} w_{y_i} \left[ y_i \log(\hat{p}_i) + (1-y_i) \log(1-\hat{p}_i) \right]$$

Where $w_c$ is the weight for class $c$.

#### Weight Calculation Strategies

**Strategy 1: Inverse Frequency Weighting**
$$w_c = \frac{N}{N_c \cdot C}$$

Where:
- $N$ = total samples
- $N_c$ = samples in class $c$
- $C$ = number of classes

For our dataset:
```
w_0 (under-determined) = 152,196 / (150,053 × 2) ≈ 0.507
w_1 (determined)       = 152,196 / (2,143 × 2)   ≈ 35.5
```

**Strategy 2: Effective Number of Samples (Cui et al., 2019)**

For highly imbalanced data, inverse frequency can be too aggressive. The "effective number" formula provides smoother weights:

$$E_c = \frac{1 - \beta^{N_c}}{1 - \beta}$$

Where $\beta \in [0, 1)$ controls smoothness (typically $\beta = 0.9$ or $0.99$).

#### Why Class Weights Work

1. **Gradient rebalancing**: The minority class now contributes equal gradient magnitude
2. **Decision boundary shift**: The boundary moves toward the majority class, making minority predictions more likely
3. **Implicit threshold adjustment**: Higher loss for minority misclassification effectively lowers the prediction threshold

#### Implementation

```python
import torch.nn.functional as F

# Calculate weights from class counts
n_samples = torch.tensor([150053.0, 2143.0])
class_weights = n_samples.sum() / (2 * n_samples)
# Result: tensor([0.507, 35.5])

# Apply in loss function
loss = F.cross_entropy(logits, labels, weight=class_weights)
```

---

### 7.3 Oversampling Theory

#### The Data Distribution Perspective

Oversampling aims to **rebalance the empirical distribution** seen during training without modifying the loss function.

#### Random Oversampling

**Method**: Duplicate minority class samples until classes are balanced.

**Mathematical effect**: If minority class has $N_{min}$ samples and majority has $N_{maj}$:
- Repeat minority samples $k = \lfloor N_{maj} / N_{min} \rfloor$ times
- New minority count: $k \cdot N_{min} \approx N_{maj}$

**Advantages**:
- Simple to implement
- No synthetic data generation

**Disadvantages**:
- **Overfitting risk**: Model may memorize duplicated samples
- **No new information**: Just repeats existing patterns

#### SMOTE: Synthetic Minority Oversampling Technique

**Theory (Chawla et al., 2002)**:

Instead of duplicating, SMOTE **creates synthetic samples** by interpolating between existing minority class examples:

1. For each minority sample $x_i$, find its $k$ nearest neighbors (typically $k=5$)
2. Randomly select one neighbor $x_{nn}$
3. Create synthetic sample: $x_{new} = x_i + \lambda \cdot (x_{nn} - x_i)$
   where $\lambda \sim \text{Uniform}(0, 1)$

**Geometric interpretation**: New samples lie on line segments connecting minority class points, effectively "filling in" the minority class region in feature space.

```
Original:      x₁ -------- x₂
                    
After SMOTE:   x₁ --x_new-- x₂
```

**Advantages**:
- Creates diverse training examples
- Reduces overfitting compared to duplication
- Expands the minority class decision region

**Disadvantages**:
- Can create noisy samples if minority class is not convex
- May interpolate across class boundaries in complex data
- For our 2D feature space (wNum, totalZero), SMOTE may create invalid combinations

#### Undersampling (Alternative)

**Method**: Remove majority class samples to match minority count.

**For our data**: Reduce 150,053 → 2,143 samples (loses 97.8% of data!)

**When useful**: When majority class is highly redundant and data is plentiful.

---

### 7.4 Threshold Tuning: Decision Theory

#### The Default Threshold Problem

Classification converts probabilities to predictions using a **decision threshold**:

$$\hat{y} = \begin{cases} 1 & \text{if } P(y=1|x) \geq \tau \\ 0 & \text{otherwise} \end{cases}$$

The default threshold $\tau = 0.5$ assumes:
1. Equal class priors: $P(y=0) = P(y=1)$
2. Equal misclassification costs: $C_{01} = C_{10}$

**Neither assumption holds in imbalanced data.**

#### Bayes Optimal Decision Theory

The optimal threshold minimizes expected cost:

$$\tau^* = \frac{C_{01} \cdot P(y=0)}{C_{01} \cdot P(y=0) + C_{10} \cdot P(y=1)}$$

Where:
- $C_{01}$ = cost of false positive (predicting 1 when true is 0)
- $C_{10}$ = cost of false negative (predicting 0 when true is 1)
- $P(y=c)$ = prior probability of class $c$

**For equal costs**: $\tau^* = P(y=0) = 0.986$ (very high!)

This explains why threshold = 0.5 fails: the model needs to be >98.6% confident before predicting "determined".

#### Threshold Tuning in Practice

**Method 1: Precision-Recall Curve**
- Plot precision vs. recall for thresholds $\tau \in [0, 1]$
- Choose $\tau$ that maximizes F1 or meets business requirements

**Method 2: Cost-Based Optimization**
- If catching "determined" is 10× more valuable than avoiding false positives:
  - $C_{10} = 10$, $C_{01} = 1$
  - Lower threshold to catch more positives at cost of more false positives

**Method 3: Equal Error Rate (EER)**
- Find $\tau$ where false positive rate = false negative rate
- Provides balanced errors across classes

#### Implementation

```python
from sklearn.metrics import precision_recall_curve

# Get probabilities instead of hard predictions
probs = model(data.x, data.edge_index).softmax(dim=1)[:, 1]

# Find optimal threshold
precisions, recalls, thresholds = precision_recall_curve(y_true, probs)
f1_scores = 2 * (precisions * recalls) / (precisions + recalls + 1e-8)
optimal_threshold = thresholds[f1_scores.argmax()]

# Apply tuned threshold
predictions = (probs >= optimal_threshold).long()
```

---

### 7.5 Proper Metrics for Imbalanced Data

#### Why Weighted F1 Fails

**Weighted F1** computes F1 for each class and averages by support (sample count):

$$\text{F1}_{weighted} = \sum_{c=0}^{C-1} \frac{N_c}{N} \cdot \text{F1}_c$$

**Problem**: The majority class dominates:
```
F1_weighted = 0.986 × F1₀ + 0.014 × F1₁
            = 0.986 × 0.99 + 0.014 × 0.00
            ≈ 0.976
```

A model with **zero** minority class performance gets 97.6% score!

#### Macro F1: Equal Class Importance

**Macro F1** treats all classes equally regardless of size:

$$\text{F1}_{macro} = \frac{1}{C} \sum_{c=0}^{C-1} \text{F1}_c$$

For binary classification:
$$\text{F1}_{macro} = \frac{\text{F1}_0 + \text{F1}_1}{2}$$

**For our failing model**:
```
F1_macro = (0.99 + 0.00) / 2 = 0.495
```

This **correctly reveals** the model performs at chance level (0.5).

#### Balanced Accuracy

**Standard accuracy** can be misleading:
$$\text{Accuracy} = \frac{\text{TP} + \text{TN}}{N} = \frac{0 + 150053}{152196} = 0.986$$

**Balanced accuracy** averages per-class recall:
$$\text{Balanced Accuracy} = \frac{1}{C} \sum_{c=0}^{C-1} \text{Recall}_c = \frac{\text{TPR} + \text{TNR}}{2}$$

Where:
- TPR (True Positive Rate) = Recall for class 1 = TP / (TP + FN)
- TNR (True Negative Rate) = Recall for class 0 = TN / (TN + FP)

**For our failing model**:
```
TPR = 0 / 2143 = 0.00
TNR = 150053 / 150053 = 1.00
Balanced Accuracy = (0.00 + 1.00) / 2 = 0.50
```

Again, **correctly reveals** random-chance performance.

#### Matthews Correlation Coefficient (MCC)

The most informative single metric for imbalanced binary classification:

$$\text{MCC} = \frac{\text{TP} \times \text{TN} - \text{FP} \times \text{FN}}{\sqrt{(\text{TP}+\text{FP})(\text{TP}+\text{FN})(\text{TN}+\text{FP})(\text{TN}+\text{FN})}}$$

**Properties**:
- Range: [-1, 1] (1 = perfect, 0 = random, -1 = inverse)
- Only achieves high score if good at **both** classes
- Symmetric: treats false positives and false negatives equally

**For our failing model**:
```
MCC = (0 × 150053 - 0 × 2143) / √((0)(2143)(150053)(150053))
    = 0 / (anything) = 0
```

**Correctly shows** the model has zero correlation with truth.

#### Metric Comparison Summary

| Metric | Our Model's Score | Interpretation |
|--------|-------------------|----------------|
| Accuracy | 0.986 | Misleading - hides failure |
| Weighted F1 | 0.976 | Misleading - dominated by majority |
| **Macro F1** | **0.495** | Correct - shows chance-level |
| **Balanced Accuracy** | **0.50** | Correct - shows chance-level |
| **MCC** | **0.00** | Correct - shows no learning |

---

### 7.6 Recommended Implementation for This Dataset

```python
import torch
import torch.nn.functional as F
from sklearn.metrics import f1_score, balanced_accuracy_score, matthews_corrcoef

# 1. Class Weights
n_under = 150053
n_deter = 2143
weights = torch.tensor([n_deter / (n_under + n_deter), 
                        n_under / (n_under + n_deter)])
# Normalize so sum = 2 (number of classes)
weights = weights / weights.sum() * 2
# Result: [0.028, 1.972] - heavily upweight minority

# 2. Training with weighted loss
def train_step(model, data, optimizer, class_weights):
    model.train()
    optimizer.zero_grad()
    out = model(data.x, data.edge_index)
    loss = F.cross_entropy(out[data.train_mask], 
                           data.y[data.train_mask],
                           weight=class_weights)
    loss.backward()
    optimizer.step()
    return loss.item()

# 3. Evaluation with proper metrics
def evaluate(model, data, mask):
    model.eval()
    with torch.no_grad():
        out = model(data.x, data.edge_index)
        pred = out[mask].argmax(dim=1)
        y_true = data.y[mask].cpu().numpy()
        y_pred = pred.cpu().numpy()
        
    return {
        'macro_f1': f1_score(y_true, y_pred, average='macro'),
        'balanced_acc': balanced_accuracy_score(y_true, y_pred),
        'mcc': matthews_corrcoef(y_true, y_pred),
        'recall_minority': (y_pred[y_true == 1] == 1).mean()
    }
```

---

*Document created: January 15, 2026*
*Pipeline version: 1.0*
*Database: d4seed1 (152,196 nodes)*

