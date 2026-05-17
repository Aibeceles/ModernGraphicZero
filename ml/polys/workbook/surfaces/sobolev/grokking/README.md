# Grokking / mechinterp experiments (character Sobolev student)

Follow-up notebooks to [`sobolev_student_character_periodic.ipynb`](../sobolev_student_character_periodic.ipynb): Fourier probes, dynamics, manifold ablation, modulus sweep, and grokking grids. All use JAX + `sobolev_distill_character` under `ml/polys`.

## Prerequisites

1. **JAX GPU venv (WSL2 recommended)** — see [JAX GPU training (WSL2)](../../../../README.md#jax-gpu-training-wsl2) in `ml/polys/README.md`: create `.venv-jax-gpu-wsl`, install `requirements-jax-gpu-wsl.txt`, register the ipykernel.
2. **Headless runners** (optional): `pip install nbclient nbformat` (not listed in `requirements-jax-gpu-wsl.txt`).
3. **Working directory**: run scripts from this folder (`grokking/`) so notebook import cells resolve `ml/polys`. Open notebooks with the notebook directory as the Jupyter cwd (same as the `_run_*.py` scripts set via `resources.metadata.path`).

```bash
cd /mnt/c/Users/<you>/JavaProjects/Aibeceles/ml/polys/workbook/surfaces/sobolev/grokking
source ../../../../.venv-jax-gpu-wsl/bin/activate   # adjust path
```

## Recommended experiment order

Runs are independent, but the narrative order matches the notebook intros:

| Step | Notebook | Default `p` |
|------|----------|-------------|
| 0 (parent dir) | [`sobolev_student_character_periodic.ipynb`](../sobolev_student_character_periodic.ipynb) | 8 |
| 1 | [`fourier_decomp.ipynb`](fourier_decomp.ipynb) | 8 |
| 2 | [`dynamics_excluded_loss.ipynb`](dynamics_excluded_loss.ipynb) | 8 |
| 3 | [`manifold_and_ablation.ipynb`](manifold_and_ablation.ipynb) | 8 |
| 4 | [`modulus_sweep.ipynb`](modulus_sweep.ipynb) | 17, 23, 113 |
| 5 | [`grokking_baseline_with_decay.ipynb`](grokking_baseline_with_decay.ipynb) | 8 (grid) + 17 (confirm) |
| 6 | [`grokking_capacity_sweep.ipynb`](grokking_capacity_sweep.ipynb) | 17 |

## How to run

### Jupyter (interactive)

Open the `.ipynb`, select the JAX GPU kernel, run cells top-to-bottom. Save the notebook to persist outputs.

### Headless (`_run_*.py`)

Executes the notebook in-place and writes outputs back into the `.ipynb`. Exits non-zero on the first cell error.

```bash
python _run_fourier_decomp_nb.py
python _run_dynamics_excluded_loss_nb.py
python _run_manifold_and_ablation_nb.py
python _run_modulus_sweep_nb.py              # ~30–50 min full sweep; saves after each cell
python _run_grokking_baseline_with_decay_nb.py   # multi-hour grid
python _run_grokking_capacity_sweep_nb.py
```

| Script | Timeout | Notes |
|--------|---------|--------|
| `_run_fourier_decomp_nb.py`, `_run_dynamics_excluded_loss_nb.py`, `_run_manifold_and_ablation_nb.py` | 2400 s | Standard execute |
| `_run_modulus_sweep_nb.py` | 24 h | Cell-by-cell; re-open `.ipynb` mid-run for progress |
| `_run_grokking_baseline_with_decay_nb.py`, `_run_grokking_capacity_sweep_nb.py` | per script | Large sweeps (~4.5 h for full baseline grid) |

Entry notebooks in the parent [`sobolev/`](../) directory: `python _run_periodic_nb.py`, `python _run_ramped_nb.py`.

### Dry-run (`_dryrun_*.py`)

Smoke tests with reduced budgets. **`_dryrun_modulus_sweep_nb.py` patches the notebook in memory only** (tiny `p=5` row, skips `p=23` and `p=113`); it does not modify the committed `.ipynb`.

```bash
python _dryrun_modulus_sweep_nb.py
python _dryrun_fourier_decomp_nb.py
python _dryrun_manifold_and_ablation_nb.py
python _dryrun_dynamics_excluded_loss_nb.py
python _dryrun_grokking_baseline_with_decay_nb.py
python _dryrun_grokking_capacity_sweep_nb.py
```

### Rebuild notebooks from builders

After editing `_build_<name>_nb.py`:

```bash
python _build_modulus_sweep_nb.py
python _build_fourier_decomp_nb.py
# ... etc.
```

## Setting the modulus `p`

`p` is the cyclic modulus for the character teacher \(\zeta^{(x+y) \bmod p}\) on the \(p \times p\) lattice.

| Notebook | Where to set `p` | Notes |
|----------|------------------|-------|
| `fourier_decomp`, `dynamics_excluded_loss`, `manifold_and_ablation` | Config cell: `MODULUS = 8` | Set `MAX_N` to the same value as `p` (lattice side length). Re-run **training** after changing `p`; DFT / summary cells only **report** `rep.p`. |
| `modulus_sweep` | Cells `sweep_p17`, `sweep_p23`, `sweep_p113`: `_p, _mesh, _ep, _ramp, _bs = ...` | The **`summary` cell does not set `p`** — it prints rows from `results` populated by the sweep cells above. |
| `modulus_sweep` | `SWEEP = [...]` in config | **Documentation only.** Changing `SWEEP` alone does not run anything; edit the `sweep_p*` cells or `_build_modulus_sweep_nb.py` and rebuild. |
| `grokking_baseline_with_decay` | `P8_MODULUS`, `P17_MODULUS` (and `P*_MAX_N`, `P*_MESH_N`) | 27-row grid at `p=8`; one confirmation row at `p=17`. |
| `grokking_capacity_sweep` | `P_MODULUS` (default `17`) | Entire capacity grid uses that modulus. |

### `modulus_sweep` workflow

1. Set `p` (and mesh / epochs / ramp / batch) in the relevant **`sweep_p*`** cell tuple, e.g. `_p, _mesh, _ep, _ramp, _bs = 17, 64, 2000, 200, 256`.
2. Run that sweep cell (calls `_train_and_score` and stores `results['p=17'] = out`).
3. Run **`summary`** and the spectrum cells — they aggregate whatever keys exist in `results`.
4. **Skip a row**: replace the sweep cell body with `# skipped`.
5. **Add a row**: duplicate a sweep cell pattern, set a new `_p` and `results['p=<p>'] = out`.
6. **Quick check**: `python _dryrun_modulus_sweep_nb.py` (~minutes, in-memory patch only).

Default hyperparameters are **paired per row** (not derived automatically from `p`):

| `p` | `mesh_n` | `epochs` | `ramp_epochs` | `batch_size` |
|-----|----------|----------|---------------|--------------|
| 17 | 64 | 2000 | 200 | 256 |
| 23 | 96 | 3000 | 300 | 256 |
| 113 | 128 | 6000 | 600 | 512 |

Roughly 4–8 mesh points per lattice cell along each axis; scale manually if you add a new modulus.

### Common pitfalls

- **`summary` prints `no sweep rows ran`** — no `sweep_p*` cell completed, or all were skipped. Re-run the sweep cells first.
- **`four_of_four` is False in `modulus_sweep`** — expected with `energy_pd=0.0` in that notebook (`pd_certificate` untrained). See the takeaway markdown at the end of `modulus_sweep.ipynb`.
- **Imports fail** — cwd must be `grokking/` (or use `_run_*.py`, which sets the kernel path).

## File map

| Kind | Files |
|------|--------|
| Notebooks | `*.ipynb` |
| Headless run | `_run_*_nb.py` |
| Dry-run | `_dryrun_*_nb.py` |
| Regenerate notebook | `_build_*_nb.py` |

Parent overview: [`../README.md`](../README.md).
