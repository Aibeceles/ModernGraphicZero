# Machine Learning Glossary

This glossary defines all machine learning terminology used in the graph label prediction pipeline. Terms are organized by task category: Models, Training, Prediction, and Production.

---

## Models

### Architecture Components

| Term | Definition |
|------|------------|
| **MLP (Multi-Layer Perceptron)** | A feedforward neural network consisting of fully connected layers. In this project, `MLPClassifier` serves as a baseline that ignores graph structure and predicts based solely on node features. |
| **GCN (Graph Convolutional Network)** | A neural network that aggregates information from neighboring nodes using spectral graph convolutions. `GCNConv` layers perform message passing where each node collects features from its neighbors. |
| **GraphSAGE** | Graph SAmple and aggreGatE - a GNN that learns node embeddings by sampling and aggregating features from local neighborhoods. Uses various aggregation methods (max, mean, add). |
| **GAT (Graph Attention Network)** | A GNN that uses attention mechanisms to weight the importance of neighboring nodes during aggregation. Different neighbors can have different influence on the node representation. |
| **GATConv** | PyTorch Geometric implementation of the Graph Attention Convolution layer that computes attention coefficients between connected nodes. |
| **Attention Heads** | Multiple independent attention computations run in parallel within a single GAT layer. Each head can learn different aspects of node relationships. Outputs are typically concatenated or averaged. |
| **Edge Features** | Attributes associated with edges (connections) between nodes. In this project, 18D polynomial-aware edge features capture differentiation relationships between connected polynomials. |

### Model Architectures

| Term | Definition |
|------|------------|
| **DepthAwareGAT** | A GAT variant that uses polynomial-specific edge features (18 dimensions) to modulate attention weights based on mathematical relationships between nodes. |
| **DepthAwareGATv2** | An improved GAT with anti-collapse mechanisms: skip connections, Jumping Knowledge (JKNet), and layer normalization. Prevents over-smoothing in deeper networks. |
| **FocalLossClassifier** | A GAT model trained with focal loss to address class imbalance by down-weighting easy examples and focusing on hard-to-classify minority class samples. |
| **CORALClassifier** | CORAL (Consistent Rank Logits) ordinal regression model. Instead of K-way softmax, outputs K-1 sigmoid thresholds to respect the ordinal structure of root counts (0 < 1 < 2 < 3 < 4). |
| **RegressionClassifier** | A model that predicts root count as a continuous value (0.0-4.0) using regression loss, then rounds to get class predictions. Tests whether ordinal structure improves predictions. |
| **MultiTaskRootClassifier** | A multi-task model with two heads: one predicts visible root count, another predicts the `determined` flag. Shared backbone enables joint learning. |

### Layer Types

| Term | Definition |
|------|------------|
| **Linear Layer** | A fully connected layer that computes `y = Wx + b` where W is a weight matrix and b is a bias vector. Also called a dense layer. |
| **ReLU (Rectified Linear Unit)** | Activation function `f(x) = max(0, x)`. Introduces non-linearity while being computationally efficient. |
| **ELU (Exponential Linear Unit)** | Activation function that outputs `x` for positive values and `α(e^x - 1)` for negative values. Smoother than ReLU with negative outputs. |
| **Dropout** | Regularization technique that randomly sets a fraction of activations to zero during training to prevent overfitting. Controlled by `dropout_rate` hyperparameter. |
| **LayerNorm (Layer Normalization)** | Normalizes activations across features for each sample. Stabilizes training by reducing internal covariate shift. |
| **Log-Softmax** | Computes `log(softmax(x))` in a numerically stable way. Used with NLL loss for classification. |
| **Softmax** | Converts logits to probabilities: `softmax(x_i) = exp(x_i) / sum(exp(x_j))`. Outputs sum to 1. |
| **Sigmoid** | Activation function `σ(x) = 1 / (1 + e^{-x})` that squashes values to (0, 1). Used for binary classification and CORAL thresholds. |

### Anti-Collapse Mechanisms

| Term | Definition |
|------|------------|
| **Over-Smoothing** | A problem in deep GNNs where node representations become indistinguishable after too many message-passing layers. All nodes converge to similar embeddings. |
| **Skip Connections** | Connections that bypass one or more layers, allowing gradients to flow directly through the network. Preserves original features and prevents information loss. |
| **Jumping Knowledge (JKNet)** | Technique that concatenates representations from all intermediate layers, allowing the final layer to select which depth's features are most useful. |
| **Residual Connection** | A skip connection that adds the input to the output: `y = F(x) + x`. Special case of skip connection. |

### Edge Feature Components

| Term | Definition |
|------|------------|
| **Depth Features** | Edge attributes based on node depth (`wNum`): depth difference, absolute difference, direction, adjacency flag. |
| **Degree Features** | Edge attributes based on polynomial degree: degree difference, ratio, consistency check. |
| **Leading Coefficient Features** | Edge attributes from the highest-degree coefficient: ratio, difference, scaling error. |
| **Similarity Features** | Edge attributes measuring coefficient similarity: cosine similarity, L2 distance, correlation, sparsity difference. |
| **Spectral Positional Encodings** | Node features derived from eigenvectors of the graph Laplacian matrix. Encode global graph structure and node position. |

---

## Training

### Data Splits

| Term | Definition |
|------|------------|
| **Train Set** | Data used to update model weights via gradient descent. Typically 65% of data. |
| **Validation Set** | Data used to monitor training progress and select hyperparameters. Not used for weight updates. Typically 15% of data. |
| **Test Set** | Held-out data used only once for final evaluation. Provides unbiased performance estimate. Typically 20% of data. |
| **Holdout Fraction** | Percentage of data reserved for testing (not used during training or validation). |
| **Stratified Split** | A data split that maintains the same class distribution in each subset. Critical for imbalanced datasets. |
| **k-Fold Cross-Validation** | Technique that splits data into k parts, trains k models using different folds as validation, and averages results for robust hyperparameter selection. |

### Training Loop

| Term | Definition |
|------|------------|
| **Epoch** | One complete pass through the entire training dataset. |
| **Batch** | A subset of training samples processed together before updating weights. |
| **Mini-Batch Training** | Training where each gradient update uses a small batch of samples rather than the full dataset. Balances computational efficiency with gradient accuracy. |
| **Full-Batch Training** | Training where each gradient update uses all training samples. Common for smaller graph datasets. |
| **NeighborLoader** | PyTorch Geometric utility for mini-batch training on graphs. Samples local neighborhoods around target nodes. |
| **Forward Pass** | Computing model outputs by passing inputs through all layers in sequence. |
| **Backward Pass** | Computing gradients of the loss with respect to all model parameters via backpropagation. |
| **Gradient Descent** | Optimization algorithm that updates weights in the direction that reduces the loss: `w = w - lr * gradient`. |

### Early Stopping

| Term | Definition |
|------|------------|
| **Early Stopping** | Training termination technique that stops when validation performance stops improving. Prevents overfitting. |
| **Patience** | Number of epochs to wait for improvement before early stopping. Higher patience allows more exploration but risks overfitting. |
| **Best Model State** | The saved model weights from the epoch with best validation performance. Restored after early stopping. |
| **MIN_EPOCHS** | Minimum number of epochs before early stopping can activate. Ensures sufficient initial training. |

### Loss Functions

| Term | Definition |
|------|------------|
| **NLL Loss (Negative Log-Likelihood)** | Classification loss that measures cross-entropy between predicted log-probabilities and true labels: `-log(p(correct_class))`. |
| **Cross-Entropy Loss** | Equivalent to NLL loss when applied after softmax. Measures divergence between predicted and true distributions. |
| **Focal Loss** | Loss function `FL(p) = -α(1-p)^γ * log(p)` that down-weights easy examples. `γ` (gamma) controls focusing: higher γ = more focus on hard examples. |
| **CORAL Loss** | Binary cross-entropy loss computed per threshold for ordinal regression. Treats K-class ordinal problem as K-1 binary threshold predictions. |
| **EMD Loss (Earth Mover's Distance)** | Loss that penalizes predictions based on distance from true class, not just correctness. Also called Wasserstein distance. Better for ordinal targets. |
| **SmoothL1 Loss** | Regression loss that's L1 (absolute error) for large errors and L2 (squared error) for small errors. More robust to outliers than MSE. |
| **MSE (Mean Squared Error)** | Regression loss `(y - ŷ)²` averaged over samples. Sensitive to outliers. |

### Regularization

| Term | Definition |
|------|------------|
| **Weight Decay (L2 Regularization)** | Penalty added to loss proportional to sum of squared weights: `L_total = L_data + λ||w||²`. Prevents large weights and overfitting. |
| **Penalty** | The regularization strength coefficient (λ). Called "penalty" in Neo4j GDS. Higher values = stronger regularization. |
| **Class Weights** | Weights applied to loss per class to address imbalance. Typically inverse frequency: rare classes get higher weight. |
| **Inverse Frequency Weighting** | Class weight = 1 / (class count). Gives minority classes more influence during training. |
| **Sqrt Inverse Frequency** | Class weight = 1 / sqrt(class count). A softer version of inverse frequency weighting. |
| **WeightedRandomSampler** | PyTorch sampler that oversamples minority classes by drawing samples with probability proportional to inverse class frequency. |

### Optimization

| Term | Definition |
|------|------------|
| **Adam Optimizer** | Adaptive Moment Estimation optimizer that maintains per-parameter learning rates using first and second moment estimates of gradients. Default choice for deep learning. |
| **Learning Rate** | Step size for gradient updates. Too high = unstable training; too low = slow convergence. |
| **Grid Search** | Hyperparameter optimization that evaluates all combinations of specified parameter values. |
| **Hyperparameter** | A parameter set before training (not learned): learning rate, hidden dimensions, dropout rate, etc. |

### Evaluation Metrics

| Term | Definition |
|------|------------|
| **Accuracy** | Fraction of correct predictions: `correct / total`. Can be misleading for imbalanced data. |
| **Macro F1** | F1 score averaged across classes, treating each class equally regardless of size. Primary metric for imbalanced classification. |
| **Weighted F1** | F1 score weighted by class support (sample count). Dominated by majority class performance. |
| **Balanced Accuracy** | Average of per-class recall (sensitivity). Equivalent to accuracy on a balanced dataset. |
| **Recall (Sensitivity)** | Fraction of true positives correctly identified: `TP / (TP + FN)`. "Of all actual positives, how many did we find?" |
| **Precision** | Fraction of predicted positives that are correct: `TP / (TP + FP)`. "Of all predictions, how many were right?" |
| **F1 Score** | Harmonic mean of precision and recall: `2 * (precision * recall) / (precision + recall)`. |
| **MAE (Mean Absolute Error)** | Average absolute difference between prediction and ground truth: `mean(|y - ŷ|)`. For root counts, interpretable as "average roots off." |
| **Confusion Matrix** | Table showing counts of true vs. predicted classes. Rows = true class, columns = predicted class. Diagonal = correct predictions. |
| **Class Collapse** | Failure mode where model predicts only majority class(es), ignoring minority classes entirely. Detected via confusion matrix or prediction distribution. |

---

## Prediction

### Inference

| Term | Definition |
|------|------------|
| **Inference** | Using a trained model to make predictions on new data. Model is set to evaluation mode (no dropout, no gradient computation). |
| **eval() Mode** | PyTorch mode where dropout is disabled and batch normalization uses running statistics. Required for consistent predictions. |
| **torch.no_grad()** | Context manager that disables gradient tracking for memory efficiency during inference. |
| **Logits** | Raw (unnormalized) model outputs before softmax. Higher logit = higher predicted probability for that class. |
| **Probabilities** | Softmax-normalized outputs that sum to 1. Interpretable as predicted class likelihoods. |
| **Argmax** | Operation that returns the index of the maximum value. Used to convert probabilities to class predictions. |

### Confidence

| Term | Definition |
|------|------------|
| **Confidence** | The maximum predicted probability across classes. Higher confidence = model is more certain. |
| **Uncertainty** | 1 - confidence, or entropy of predicted distribution. Higher uncertainty = model is less certain. |
| **Threshold** | A confidence cutoff below which predictions are flagged as uncertain or deferred. |
| **Deferral** | Declining to make a prediction when confidence is too low. Routes to human review or fallback system. |

### Comparison

| Term | Definition |
|------|------------|
| **Ground Truth** | The correct labels (typically from human annotation or known data). Used to evaluate predictions. |
| **Misclassification** | A prediction that doesn't match ground truth. |
| **Ordinal Error** | A misclassification to a neighboring class (|true - pred| = 1). Less severe than distant errors for ordinal targets. |
| **Distant Error** | A misclassification to a non-neighboring class (|true - pred| > 1). More severe for ordinal targets. |

### Neo4j Integration

| Term | Definition |
|------|------------|
| **Batch Write** | Writing predictions to database in groups (e.g., 100 nodes at a time) for efficiency. |
| **elementId** | Neo4j's unique identifier for nodes. Used to match predictions back to database records. |
| **Property Write-Back** | Storing model predictions as new properties on existing database nodes. |

---

## Production

### Architecture

| Term | Definition |
|------|------------|
| **Backbone** | The shared encoder network that produces embeddings. In production model, the DepthAwareGATv2 backbone is shared between CORAL and Focal heads. |
| **Head** | A task-specific layer attached to the backbone. Converts embeddings to predictions for a specific task. |
| **Multi-Head Architecture** | Design with multiple prediction heads sharing a common backbone. Enables different prediction strategies from same embeddings. |
| **Frozen Backbone** | Backbone with fixed (non-trainable) weights. Used in Phase 2 training to train heads without changing embeddings. |
| **Two-Phase Training** | Training strategy: Phase 1 trains backbone + primary head; Phase 2 freezes backbone and trains secondary head. |

### CORAL Ordinal Regression

| Term | Definition |
|------|------------|
| **Ordinal Regression** | Regression approach for ordered categories (0 < 1 < 2 < 3 < 4). Respects that 2 is closer to 1 than to 4. |
| **Threshold Probability** | CORAL output P(y > k) - the probability that the true class exceeds threshold k. |
| **Threshold Biases** | Per-threshold learnable parameters in CORAL. Shared linear weights + separate biases ensure rank consistency. |
| **Rank Consistency** | Property that if P(y > k) is high, then P(y > k-1) should also be high. Enforced by CORAL's architecture. |
| **Expected Value E[y]** | CORAL's continuous prediction: sum of all threshold probabilities. E[y] = Σ P(y > k). Useful for boundary detection. |
| **Class Probabilities from CORAL** | Converted from thresholds: P(y=0) = 1-t₀, P(y=k) = t_{k-1} - t_k, P(y=K-1) = t_{K-2}. |

### Calibration

| Term | Definition |
|------|------------|
| **Probability Calibration** | Adjusting predicted probabilities so they match empirical frequencies. A 80% confidence prediction should be correct 80% of the time. |
| **ECE (Expected Calibration Error)** | Weighted average of |accuracy - confidence| across confidence bins. ECE = 0 means perfect calibration. |
| **MCE (Maximum Calibration Error)** | Maximum gap between accuracy and confidence across any bin. Measures worst-case calibration. |
| **ACE (Average Calibration Error)** | Class-conditional ECE averaged across classes. Checks calibration per-class. |
| **Brier Score** | Mean squared error of probability estimates. Lower = better. Combines calibration and refinement. |
| **Reliability Diagram** | Plot of accuracy vs. confidence across bins. Perfect calibration = diagonal line. |
| **Temperature Scaling** | Post-hoc calibration: `p = softmax(logits / T)`. Temperature T > 1 softens probabilities; T < 1 sharpens them. |

### Selective Prediction

| Term | Definition |
|------|------------|
| **Selective Prediction** | Making predictions only when confident, abstaining otherwise. Trade-off between coverage and accuracy. |
| **Coverage** | Fraction of samples where model makes a prediction (doesn't defer). 100% coverage = predict on everything. |
| **Accuracy-Coverage Curve** | Plot showing accuracy achieved at each coverage level when predicting only on most confident samples. |
| **AUC (Area Under Curve)** | For accuracy-coverage curve, AUC measures overall selective prediction quality. Higher = better. |
| **Optimal Threshold** | Confidence threshold that achieves target accuracy at maximum coverage. |

### Uncertainty Routing

| Term | Definition |
|------|------------|
| **Uncertainty-Aware Routing** | Decision system that routes predictions based on confidence: defer (low), use ordinal (medium), use focal (high). |
| **Routing Decision** | The chosen prediction path: 0 = defer to human, 1 = use CORAL ordinal, 2 = use Focal sharp prediction. |
| **Defer Threshold** | Confidence below which predictions are flagged for human review. |
| **Focal Threshold** | Confidence above which the focal (sharp) classifier is trusted over CORAL. |
| **Ordinal Margin** | Distance of E[y] from nearest integer. Large margin = uncertain boundary case. |

### Regime Detection

| Term | Definition |
|------|------------|
| **Regime** | A region in embedding space associated with a particular class. Samples in the same regime have similar characteristics. |
| **Regime Detection** | Identifying which class regime a new sample belongs to based on embedding proximity. |
| **Centroid** | The mean embedding of all samples in a regime. Used as regime representative. |
| **Regime Radius** | Mean distance from centroid to regime members. Measures regime spread. |
| **Regime Confidence** | Softmax over negative distances to regime centroids. High confidence = clearly in one regime. |

### Drift Detection

| Term | Definition |
|------|------------|
| **Distribution Drift** | Change in data distribution over time. Model may become less accurate if drift is significant. |
| **OOD (Out-of-Distribution)** | Samples that differ significantly from training data. May produce unreliable predictions. |
| **OOD Detection** | Identifying samples that are far from the training distribution. Uses embedding distance thresholds. |
| **Drift Monitoring** | Ongoing tracking of prediction distribution and embedding distances to detect distribution shift. |
| **k-NN Distance** | Average distance to k nearest neighbors in training set. High k-NN distance = potential OOD sample. |

### Vector Indexing (FAISS)

| Term | Definition |
|------|------------|
| **FAISS** | Facebook AI Similarity Search - library for efficient similarity search on dense vectors. |
| **Vector Index** | Data structure for fast nearest neighbor search in embedding space. |
| **IndexFlatIP** | FAISS index for exact inner product (cosine similarity on normalized vectors) search. |
| **Normalized Embeddings** | Embeddings scaled to unit length (`||e|| = 1`). Inner product equals cosine similarity. |
| **Cosine Similarity** | Similarity measure: `cos(θ) = (a · b) / (||a|| ||b||)`. Range [-1, 1], higher = more similar. |
| **k-NN (k-Nearest Neighbors)** | Finding k training samples closest to a query. Used for explanation and regime detection. |
| **Neighbor Retrieval** | Finding similar historical samples for a new prediction. Enables explainability. |

### Explainability

| Term | Definition |
|------|------------|
| **Explainability** | Ability to provide human-understandable reasoning for model predictions. |
| **k-NN Explanation** | Explaining a prediction by showing the k most similar training samples and their labels. |
| **Decision Explanation** | Human-readable description of why a particular prediction or routing decision was made. |

---

## Hyperparameter Reference

### Common Values

| Hyperparameter | Typical Value | Description |
|----------------|---------------|-------------|
| `HIDDEN_DIM` | 64 | Hidden layer width |
| `NUM_ATTENTION_HEADS` | 4 | Attention heads per GAT layer |
| `DROPOUT_RATE` | 0.5 | Dropout probability |
| `LEARNING_RATE` | 0.01 | Adam learning rate |
| `WEIGHT_DECAY` | 0.0625 | L2 regularization strength |
| `MAX_EPOCHS` | 200 | Maximum training epochs |
| `PATIENCE` | 20 | Early stopping patience |
| `HOLDOUT_FRACTION` | 0.2 | Test set fraction |
| `FOCAL_GAMMA` | 2.0 | Focal loss focusing parameter |
| `SPECTRAL_PE_DIM` | 8 | Spectral positional encoding dimensions |
| `NUM_EDGE_FEATURES` | 18 | Polynomial edge feature dimensions |

---

## Acronyms Quick Reference

| Acronym | Full Term |
|---------|-----------|
| **GNN** | Graph Neural Network |
| **GCN** | Graph Convolutional Network |
| **GAT** | Graph Attention Network |
| **MLP** | Multi-Layer Perceptron |
| **CORAL** | Consistent Rank Logits (ordinal regression method) |
| **JKNet** | Jumping Knowledge Network |
| **NLL** | Negative Log-Likelihood |
| **BCE** | Binary Cross-Entropy |
| **CE** | Cross-Entropy |
| **MAE** | Mean Absolute Error |
| **MSE** | Mean Squared Error |
| **EMD** | Earth Mover's Distance |
| **ECE** | Expected Calibration Error |
| **MCE** | Maximum Calibration Error |
| **ACE** | Average Calibration Error |
| **AUC** | Area Under Curve |
| **OOD** | Out-of-Distribution |
| **k-NN** | k-Nearest Neighbors |
| **PyG** | PyTorch Geometric |
| **FAISS** | Facebook AI Similarity Search |
