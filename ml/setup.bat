@echo off
REM Setup script for Windows
REM Usage: setup.bat

echo ============================================================
echo   Aibeceles ML Environment Setup (Windows)
echo ============================================================
echo.

REM Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python is not installed or not in PATH
    echo Please install Python 3.8 or higher
    pause
    exit /b 1
)

echo Running setup script...
echo.

python setup_env.py

if errorlevel 1 (
    echo.
    echo Setup failed. Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo.
echo To activate the virtual environment, run:
echo   .venv\Scripts\activate.bat
echo.
echo Or in PowerShell:
echo   .venv\Scripts\Activate.ps1
echo.
echo ============================================================
pause

