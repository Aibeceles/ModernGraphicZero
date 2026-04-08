# Machine Learning Module

This module contains machine learning pipelines for the Aibeceles project, focusing on graph-based node classification using PyTorch Geometric.

## Project Structure

```
ml/
├── .venv/                         # Virtual environment (created by setup)
├── neo4j/                         # Neo4j database connectivity
│   ├── neo4jClient.py            # Simple Neo4j client
│   └── README.md                 # Neo4j client documentation
├── graph_label_prediction/        # Node classification pipeline
│   ├── __init__.py               # Module exports
│   ├── config.py                 # Configuration and hyperparameters
│   ├── data_loader.py            # Neo4j to PyG data conversion
│   ├── models.py                 # ML models (MLP, GCN, GraphSAGE)
│   ├── trainer.py                # Training infrastructure
│   ├── predictor.py              # Inference and write-back
│   └── run_pipeline.ipynb        # Complete pipeline notebook
├── requirements.txt               # Python dependencies
├── setup_env.py                  # Environment setup script
└── README.md                     # This file
```

## Setup Instructions

### 1. Create Virtual Environment

**On Windows (PowerShell):**
```powershell
# Navigate to ml directory
cd ml

# Create virtual environment
python -m venv .venv

# Activate virtual environment
.\.venv\Scripts\Activate.ps1

# If you get an execution policy error, run:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**On Windows (Command Prompt):**
```cmd
cd ml
python -m venv .venv
.venv\Scripts\activate.bat
```

**On Linux/Mac:**
```bash
cd ml
python3 -m venv .venv
source .venv/bin/activate
```

### 2. Install Dependencies

Once the virtual environment is activated:

```bash
# Upgrade pip
pip install --upgrade pip

# Install core dependencies
pip install -r requirements.txt
```

### 3. Install PyTorch Geometric Dependencies
 
PyTorch Geometric requires additional extensions. Install them based on your system:

**First, check your PyTorch version:**
```bash
python -c "import torch; print('PyTorch:', torch.__version__); print('CUDA:', torch.version.cuda if torch.cuda.is_available() else 'CPU')"
```

**For PyTorch 2.5.x with CUDA 11.8:**
```bash
pip install torch-scatter torch-sparse torch-cluster torch-spline-conv -f https://data.pyg.org/whl/torch-2.5.0+cu118.html
```

**For other versions:**
- Visit [PyTorch Geometric Installation Guide](https://pytorch-geometric.readthedocs.io/en/latest/install/installation.html)
- Use the version matching your PyTorch (e.g., torch-2.4.0, torch-2.3.0, etc.)

### 4. Configure Neo4j Connection

Create a `.env` file in the parent directory (one level up from `ml/`):

```ini
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=your_password_here
NEO4J_DATABASE=d4seed1
```

**Important:** Never commit `.env` files to version control!

### 5. Verify Installation

```bash
# Activate venv if not already activated
# Windows: .\.venv\Scripts\Activate.ps1
# Linux/Mac: source .venv/bin/activate

# Test imports
python -c "import torch; import torch_geometric; import neo4j; print('✓ All imports successful')"
```

## Usage

### Running the Node Classification Pipeline

1. **Activate the virtual environment:**
   ```powershell
   .\.venv\Scripts\Activate.ps1  # Windows PowerShell
   ```

2. **Launch Jupyter:**
   ```bash
   jupyter notebook
   ```

3. **Open and run:**
   - Navigate to `graph_label_prediction/run_pipeline.ipynb`
   - Execute cells sequentially

### Using the Python API

```python
from graph_label_prediction.data_loader import load_graph_from_neo4j
from graph_label_prediction.trainer import train_and_evaluate
from graph_label_prediction.predictor import predict_and_write
from neo4j.neo4jClient import Neo4jClient

# Load data
data, loader = load_graph_from_neo4j(
    uri="bolt://localhost:7687",
    user="neo4j",
    password="password",
    database="d4seed1"
)

# Train model
model, metrics = train_and_evaluate(data, model_name='gcn', run_grid_search=True)

# Generate predictions and write to Neo4j
client = Neo4jClient(uri, user, password)
stats = predict_and_write(model, data, client, "d4seed1")
client.close()
```

## Modules

### neo4j
Simple Neo4j connectivity. See `neo4j/README.md` for details.

### graph_label_prediction
Binary node classification pipeline for predicting polynomial determination.

**Key Features:**
- Neo4j data loading with automatic PyG conversion
- Three model architectures: MLP (baseline), GCN, GraphSAGE
- Grid search with k-fold cross-validation
- F1-weighted evaluation (matches Neo4j GDS baseline)
- Automatic prediction write-back to Neo4j

**Expected Performance:**
- Train F1: 0.80-0.85 (baseline: 0.8295)
- Test F1: 0.75-0.82 (baseline: 0.7923)

## Development

### Activating Virtual Environment

**Always activate the virtual environment before working:**

```powershell
# Windows PowerShell (from ml directory)
.\.venv\Scripts\Activate.ps1

# You should see (.venv) in your prompt
```

### Running Tests

```bash
pytest tests/
```

### Code Formatting

```bash
# Format code
black .

# Check linting
flake8 .
```

### Adding New Dependencies

```bash
# Install new package
pip install package_name

# Update requirements.txt
pip freeze > requirements.txt
```

## Troubleshooting

### Virtual Environment Not Activating (Windows)

**Error:** "cannot be loaded because running scripts is disabled on this system"

**Solution:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### PyTorch Geometric Installation Issues

**Error:** "No matching distribution found"

**Solution:** Ensure you're using the correct PyTorch version and CUDA version in the installation URL.

Check versions:
```python
import torch
print(f"PyTorch: {torch.__version__}")
print(f"CUDA: {torch.version.cuda}")
```

### Neo4j Connection Errors

**Error:** "Failed to create Neo4j driver"

**Solutions:**
1. Verify Neo4j is running: Open http://localhost:7474
2. Check credentials in `.env` file
3. Verify database exists: Run `SHOW DATABASES` in Neo4j Browser
4. Check URI format: `bolt://localhost:7687` (not `neo4j://`)

### Import Errors in Jupyter

**Error:** "ModuleNotFoundError: No module named 'graph_label_prediction'"

**Solution:** Ensure Jupyter is using the correct kernel:
```bash
# Install ipykernel in venv
pip install ipykernel

# Add venv as Jupyter kernel
python -m ipykernel install --user --name=aibeceles-ml --display-name="Python (Aibeceles ML)"

# In Jupyter: Kernel > Change Kernel > Python (Aibeceles ML)
```

## Additional Resources

- [PyTorch Geometric Documentation](https://pytorch-geometric.readthedocs.io/)
- [Neo4j Python Driver](https://neo4j.com/docs/python-manual/current/)
- [Graph Label Prediction Specification](../GraphMLDifferences/GraphicLablePrediction_specification.md)

## License

Part of the Aibeceles project.

