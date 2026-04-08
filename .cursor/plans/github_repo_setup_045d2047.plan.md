---
name: GitHub repo setup
overview: Push the Aibeceles project to the existing empty GitHub repository at https://github.com/Aibeceles/ModernGraphicZero.git, with a proper .gitignore, root README, and an initial commit.
todos:
  - id: gitignore
    content: Create root .gitignore with comprehensive exclusion rules for Java, Python, IDE, OS, secrets, and build artifacts
    status: completed
  - id: readme
    content: Create root README.md summarizing the project and linking to sub-module docs
    status: completed
  - id: git-init
    content: Run git init, stage files, verify nothing sensitive is staged, and make initial commit
    status: in_progress
  - id: gh-push
    content: Add existing ModernGraphicZero remote and push initial commit
    status: pending
isProject: false
---

# GitHub Repository Setup for Aibeceles

## Current State

- **No git repo** exists in `c:\Users\tomas\JavaProjects\Aibeceles`
- GitHub CLI (`gh`) is installed and authenticated as **Aibeceles**
- The project is a multi-language workspace: Java (NetBeans), Python ML (PyTorch Geometric, Neo4j), and documentation
- An `ml/.gitignore` already exists but there is **no root `.gitignore`**
- There is **no root `README.md`** (sub-READMEs exist in `documentation/` and `ml/`)

## What Gets Excluded (root `.gitignore`)

Build on the existing [ml/.gitignore](ml/.gitignore) patterns and add project-wide rules:

- **Virtual environments**: `.venv/`, `ml/.venv/`
- **Secrets**: `.env`, `.env.local`
- **IDE/editor**: `.vscode/`, `.idea/`, `.metals/`, `.classpath`, `.project`, `nbproject/private/`
- **Java build artifacts**: `build/`, `dist/`, `*.class`, `*.jar`
- **Python caches**: `__pycache__/`, `*.pyc`, `.pytest_cache/`
- **Jupyter**: `.ipynb_checkpoints/`
- **Derby databases**: `muTableDB/` (local DB data, large and machine-specific)
- **OS files**: `.DS_Store`, `Thumbs.db`
- **Model files**: `*.pt`, `*.pth`, `*.onnx`
- **Logs**: `*.log`, `logs/`

## What Gets Committed

- `**documentation/`** -- theory, formal notes, READMEs (Markdown, HTML, JSON)
- `**ml/`** -- Python ML source code, notebooks, requirements.txt, sub-READMEs (excludes .venv, .env, CSV data per ml/.gitignore)
- `**ZADScriptsK/`** -- Java source code, `build.xml`, `KafkaStreamsTopics.json` (excludes `nbproject/private/`, build output)
- `**ZerosAndDifferences033021/`** -- Java source code, `build.xml`, project documentation (excludes build output, `muTableDB/`, dist)
- **Root `README.md`** -- new file summarizing the entire project with pointers to sub-modules

## Steps

### 1. Create root `.gitignore`

A comprehensive file covering Java, Python, IDE, OS, and project-specific exclusions.

### 2. Create root `README.md`

A concise top-level README that:

- Describes Aibeceles as a graph-based polynomial analysis project
- Lists the major modules (`documentation/`, `ml/`, `ZADScriptsK/`, `ZerosAndDifferences033021/`)
- Links to sub-module READMEs for details
- References the YouTube explanation video already cited in the existing docs

### 3. Initialize git and make the initial commit

```
git init
git add .
git commit -m "Initial commit: Aibeceles project with Java and ML modules"
```

### 4. Add the existing remote and push

The repo already exists at `https://github.com/Aibeceles/ModernGraphicZero.git` (currently empty).

```
git remote add origin https://github.com/Aibeceles/ModernGraphicZero.git
git branch -M main
git push -u origin main
```

## Files Created/Modified


| File                | Action                                      |
| ------------------- | ------------------------------------------- |
| `.gitignore` (root) | **Create** -- comprehensive exclusion rules |
| `README.md` (root)  | **Create** -- project overview              |


No existing files are modified.