"""
Quick test script to verify the environment is set up correctly.

Run this after setting up the virtual environment:
    python test_setup.py
"""

import sys


def test_import(module_name, description):
    """Test importing a module."""
    try:
        __import__(module_name)
        print(f"✓ {description}")
        return True
    except ImportError as e:
        print(f"✗ {description}: {e}")
        return False


def main():
    """Run all tests."""
    print("=" * 60)
    print("  Environment Setup Verification")
    print("=" * 60)
    print()
    
    # Test Python version
    version = sys.version_info
    print(f"Python version: {version.major}.{version.minor}.{version.micro}")
    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print("✗ Python 3.8+ required")
        return False
    print("✓ Python version OK")
    print()
    
    # Test core packages
    print("Testing core packages:")
    all_passed = True
    
    tests = [
        ("neo4j", "Neo4j driver"),
        ("pandas", "Pandas"),
        ("numpy", "NumPy"),
        ("torch", "PyTorch"),
        ("torch_geometric", "PyTorch Geometric"),
        ("torch_scatter", "PyTorch Scatter"),
        ("torch_sparse", "PyTorch Sparse"),
        ("sklearn", "Scikit-learn"),
        ("matplotlib", "Matplotlib"),
        ("dotenv", "Python-dotenv"),
    ]
    
    for module, desc in tests:
        if not test_import(module, desc):
            all_passed = False
    
    print()
    
    # Test PyTorch setup
    print("Testing PyTorch setup:")
    try:
        import torch
        print(f"  PyTorch version: {torch.__version__}")
        print(f"  CUDA available: {torch.cuda.is_available()}")
        if torch.cuda.is_available():
            print(f"  CUDA version: {torch.version.cuda}")
            print(f"  GPU count: {torch.cuda.device_count()}")
        print("✓ PyTorch setup OK")
    except Exception as e:
        print(f"✗ PyTorch setup failed: {e}")
        all_passed = False
    
    print()
    
    # Test pipeline modules
    print("Testing pipeline modules:")
    sys.path.insert(0, '.')
    sys.path.insert(0, './neo4j')
    
    pipeline_tests = [
        ("graph_label_prediction.config", "Config module"),
        ("graph_label_prediction.data_loader", "Data loader"),
        ("graph_label_prediction.models", "Models"),
        ("graph_label_prediction.trainer", "Trainer"),
        ("graph_label_prediction.predictor", "Predictor"),
        ("neo4jClient", "Neo4j Client"),
    ]
    
    for module, desc in pipeline_tests:
        if not test_import(module, desc):
            all_passed = False
    
    print()
    print("=" * 60)
    
    if all_passed:
        print("✓ All tests passed! Environment is ready.")
        print()
        print("Next steps:")
        print("  1. Ensure Neo4j is running")
        print("  2. Configure .env file with credentials")
        print("  3. Run: jupyter notebook")
        print("  4. Open: graph_label_prediction/run_pipeline.ipynb")
    else:
        print("✗ Some tests failed. Please review the errors above.")
        print()
        print("Common fixes:")
        print("  1. Activate virtual environment")
        print("  2. Run: pip install -r requirements.txt")
        print("  3. Install PyG extensions (see README.md)")
    
    print("=" * 60)
    
    return all_passed


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)

