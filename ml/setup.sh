#!/bin/bash
# Setup script for Linux/Mac
# Usage: ./setup.sh

echo "============================================================"
echo "  Aibeceles ML Environment Setup (Linux/Mac)"
echo "============================================================"
echo

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is not installed"
    echo "Please install Python 3.8 or higher"
    exit 1
fi

echo "Running setup script..."
echo

python3 setup_env.py

if [ $? -ne 0 ]; then
    echo
    echo "Setup failed. Please check the error messages above."
    exit 1
fi

echo
echo "============================================================"
echo
echo "To activate the virtual environment, run:"
echo "  source .venv/bin/activate"
echo
echo "============================================================"

