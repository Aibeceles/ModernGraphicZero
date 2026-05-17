# Sobolev surface experiments (character teacher / student)

JAX notebooks that distill a **character** (modular) teacher into a SIREN student on a \(p \times p\) lattice mesh. The [`grokking/`](grokking/) folder continues the same stack with mechinterp probes, modulus sweeps, and grokking grids.

## What lives here

| Path | Purpose |
|------|---------|
| `sobolev_student_character_periodic.ipynb` | Periodic-cardinal teacher; main baseline at **p = 8** |
| `sobolev_student_character_ramped.ipynb` | Ramped / scheduled variant (same `MODULUS` pattern) |
| `sobolev_student_character.ipynb`, `sobolev_student.ipynb`, … | Earlier / related Sobolev student work |
| [`grokking/`](grokking/) | Fourier decomposition, dynamics, manifold ablation, modulus sweep, grokking sweeps — see [`grokking/README.md`](grokking/README.md) |

Library code: `ml/polys/sobolev_distill_character/` (imported from notebooks via `_polys_root_for_import()`).

## Prerequisites

JAX GPU setup (WSL2): [ml/polys/README.md — JAX GPU training (WSL2)](../../../README.md#jax-gpu-training-wsl2).

For headless execution: `pip install nbclient nbformat`.

## Setting `p` (entry notebooks)

In `sobolev_student_character_periodic.ipynb` (and the ramped notebook), the config cell defines:

```python
MODULUS = 8
MAX_N = 8   # lattice side; keep equal to MODULUS
```

Change `MODULUS` (and `MAX_N`), then re-run teacher build and training cells. Plots and verdict cells use `p = MODULUS` downstream.

If you edit the notebook via builders in this directory:

```bash
python _build_sobolev_student_character_periodic_nb.py
python _build_sobolev_student_character_ramped_nb.py
```

## How to run (this directory)

```bash
cd ml/polys/workbook/surfaces/sobolev
source ../../../.venv-jax-gpu-wsl/bin/activate   # adjust path

python _run_periodic_nb.py    # sobolev_student_character_periodic.ipynb
python _run_ramped_nb.py      # sobolev_student_character_ramped.ipynb
```

Or open the `.ipynb` in Jupyter with the JAX GPU kernel and run all cells.

## Grokking / mechinterp follow-up

For **modulus sweep**, **Fourier probes**, **how to set `p` per notebook**, headless vs dry-run, and wall-clock budgets, use the dedicated runbook:

**[grokking/README.md](grokking/README.md)**
