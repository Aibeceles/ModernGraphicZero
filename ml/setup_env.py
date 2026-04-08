"""
Setup script for creating and configuring the virtual environment.

Usage:
    python setup_env.py
"""

import os
import platform
import subprocess
import sys
from pathlib import Path


def print_header(text):
    """Print a formatted header."""
    print("\n" + "=" * 60)
    print(f"  {text}")
    print("=" * 60)


def run_command(cmd, shell=False):
    """Run a command and return success status."""
    try:
        result = subprocess.run(
            cmd,
            shell=shell,
            check=True,
            capture_output=True,
            text=True
        )
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr


def main():
    """Main setup function."""
    print_header("Aibeceles ML Environment Setup")
    
    # Check Python version
    py_version = sys.version_info
    print(f"\nPython version: {py_version.major}.{py_version.minor}.{py_version.micro}")
    
    if py_version.major < 3 or (py_version.major == 3 and py_version.minor < 8):
        print("❌ Error: Python 3.8 or higher is required")
        sys.exit(1)
    
    print("✓ Python version OK")
    
    # Get paths
    ml_dir = Path(__file__).parent
    venv_dir = ml_dir / ".venv"
    
    # Determine OS
    os_type = platform.system()
    print(f"\nOperating System: {os_type}")
    
    # Check if venv exists
    if venv_dir.exists():
        response = input("\n⚠️  Virtual environment already exists. Recreate? (y/N): ")
        if response.lower() != 'y':
            print("Aborting setup.")
            sys.exit(0)
        
        print("Removing existing virtual environment...")
        import shutil
        shutil.rmtree(venv_dir)
    
    # Create virtual environment
    print_header("Creating Virtual Environment")
    print(f"Location: {venv_dir}")
    
    success, output = run_command([sys.executable, "-m", "venv", str(venv_dir)])
    
    if not success:
        print(f"❌ Failed to create virtual environment:\n{output}")
        sys.exit(1)
    
    print("✓ Virtual environment created")
    
    # Determine activation script path
    if os_type == "Windows":
        activate_script = venv_dir / "Scripts" / "activate.bat"
        pip_executable = venv_dir / "Scripts" / "pip.exe"
    else:
        activate_script = venv_dir / "bin" / "activate"
        pip_executable = venv_dir / "bin" / "pip"
    
    # Upgrade pip
    print_header("Upgrading pip")
    success, output = run_command([str(pip_executable), "install", "--upgrade", "pip"])
    
    if success:
        print("✓ pip upgraded")
    else:
        print(f"⚠️  Warning: Could not upgrade pip:\n{output}")
    
    # Install requirements
    print_header("Installing Requirements")
    requirements_file = ml_dir / "requirements.txt"
    
    if not requirements_file.exists():
        print(f"❌ requirements.txt not found at {requirements_file}")
        sys.exit(1)
    
    print("Installing packages (this may take several minutes)...")
    success, output = run_command([
        str(pip_executable), 
        "install", 
        "-r", 
        str(requirements_file)
    ])
    
    if success:
        print("✓ Requirements installed")
    else:
        print(f"❌ Failed to install requirements:\n{output}")
        sys.exit(1)
    
    # Check for CUDA
    print_header("Checking CUDA Availability")
    
    check_cuda = f"""
import torch
if torch.cuda.is_available():
    print(f"CUDA available: {{torch.version.cuda}}")
else:
    print("CUDA not available (CPU only)")
"""
    
    python_executable = venv_dir / ("Scripts/python.exe" if os_type == "Windows" else "bin/python")
    success, output = run_command([str(python_executable), "-c", check_cuda])
    
    if success:
        print(output.strip())
        
        if "CUDA available" in output:
            response = input("\nInstall PyTorch Geometric with CUDA support? (Y/n): ")
            if response.lower() != 'n':
                cuda_version = output.split(":")[1].strip() if ":" in output else "11.8"
                torch_version = "2.5.0"  # PyG uses 2.5.0 for PyTorch 2.5.x
                
                print(f"\nInstalling PyG extensions for CUDA {cuda_version}...")
                pyg_url = f"https://data.pyg.org/whl/torch-{torch_version}+cu{cuda_version.replace('.', '')}.html"
                
                success, output = run_command([
                    str(pip_executable),
                    "install",
                    "torch-scatter",
                    "torch-sparse", 
                    "torch-cluster",
                    "torch-spline-conv",
                    "-f",
                    pyg_url
                ])
                
                if success:
                    print("✓ PyTorch Geometric extensions installed")
                else:
                    print(f"⚠️  Could not install PyG extensions automatically")
                    print("You may need to install them manually. See README.md")
        else:
            print("\nInstalling PyTorch Geometric for CPU...")
            pyg_url = "https://data.pyg.org/whl/torch-2.5.0+cpu.html"
            
            success, output = run_command([
                str(pip_executable),
                "install",
                "torch-scatter",
                "torch-sparse",
                "torch-cluster", 
                "torch-spline-conv",
                "-f",
                pyg_url
            ])
            
            if success:
                print("✓ PyTorch Geometric extensions installed")
            else:
                print(f"⚠️  Could not install PyG extensions automatically")
                print("You may need to install them manually. See README.md")
    
    # Check .env file
    print_header("Checking Configuration")
    
    env_file = ml_dir.parent / ".env"
    if env_file.exists():
        print(f"✓ Found .env file at {env_file}")
    else:
        print(f"⚠️  No .env file found at {env_file}")
        print("\nCreate a .env file with:")
        print("  NEO4J_URI=bolt://localhost:7687")
        print("  NEO4J_USER=neo4j")
        print("  NEO4J_PASSWORD=your_password")
        print("  NEO4J_DATABASE=d4seed1")
    
    # Setup complete
    print_header("Setup Complete!")
    
    print("\nTo activate the virtual environment:")
    if os_type == "Windows":
        print(f"  PowerShell: .\\{venv_dir.relative_to(ml_dir)}\\Scripts\\Activate.ps1")
        print(f"  CMD:        .\\{venv_dir.relative_to(ml_dir)}\\Scripts\\activate.bat")
    else:
        print(f"  source {venv_dir.relative_to(ml_dir)}/bin/activate")
    
    print("\nTo start Jupyter:")
    print("  jupyter notebook")
    
    print("\nTo run the pipeline:")
    print("  Open graph_label_prediction/run_pipeline.ipynb in Jupyter")
    
    print("\n✓ Environment is ready to use!\n")


if __name__ == "__main__":
    main()

