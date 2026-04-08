"""
Experiment orchestration for the Root Count Prediction pipeline.

Provides ``ExperimentConfig`` (what to run), ``ExperimentRunner`` (how to run),
and ``ExperimentReport`` (structured results) so that ``run_pipeline.ipynb``
can sweep over model / loss / activation combinations in a single call.

Example
-------
>>> from graph_label_prediction.python_model.core.experiment import (
...     ExperimentConfig, ExperimentRunner,
... )
>>> runner = ExperimentRunner(data)
>>> configs = [
...     ExperimentConfig(name="SAGE-CE-relu", model_name="graphsage"),
...     ExperimentConfig(name="GAT-Focal-elu", model_name="depth_aware_gat",
...                      loss_type="focal", activation="elu"),
... ]
>>> report = runner.run_all(configs)
>>> print(report.summary())
>>> report.to_json("evaluation_metrics/latest.json")
"""

from __future__ import annotations

import itertools
import json
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import numpy as np
import pandas as pd
import torch

from .config import (
    LEARNING_RATE,
    MAX_EPOCHS,
    PATIENCE,
    HIDDEN_DIM,
    DROPOUT_RATE,
    DEFAULT_ACTIVATION,
    LOSS_TYPE,
    TARGET_MODE,
)
from .models import get_model
from .trainer import NodeClassificationTrainer


# ---------------------------------------------------------------------------
# ExperimentConfig
# ---------------------------------------------------------------------------

@dataclass
class ExperimentConfig:
    """Fully-serialisable specification for a single training experiment.

    Every field maps directly to a ``trainer.train()`` kwarg so the runner
    can unpack it without translation.

    Attributes:
        name: Human-readable label shown in comparison tables.
        model_name: Key accepted by ``get_model()``
            (e.g. ``'graphsage'``, ``'depth_aware_gat'``, ``'coral'``).
        loss_type: ``'ce'`` | ``'focal'`` | ``'emd'``.
        activation: ``'relu'`` | ``'elu'`` | ``'leaky_relu'`` | ``'gelu'``.
        weight_decay: L2 regularisation strength.
        learning_rate: Adam learning rate.
        hidden_dim: Hidden layer width.
        dropout: Dropout probability.
        max_epochs: Maximum training epochs.
        patience: Early-stopping patience.
        feature_set: Node feature set name (key from ``FEATURE_SETS``).
            ``'full'`` uses all node features; other values slice columns
            from ``data.x`` for ablation studies.
        edge_feature_set: Edge feature set name (key from ``EDGE_FEATURE_SETS``).
            ``'full'`` uses all 18 edge features; other values slice columns
            from the computed edge feature vector.
        model_kwargs: Extra kwargs forwarded to the model constructor.
    """

    name: str
    model_name: str = 'depth_aware_gat'
    loss_type: str = 'ce'
    activation: str = DEFAULT_ACTIVATION
    weight_decay: float = 1e-3
    learning_rate: float = LEARNING_RATE
    hidden_dim: int = HIDDEN_DIM
    dropout: float = DROPOUT_RATE
    max_epochs: int = MAX_EPOCHS
    patience: int = PATIENCE
    feature_set: str = 'full'
    edge_feature_set: str = 'full'
    model_kwargs: dict = field(default_factory=dict)

    # ------------------------------------------------------------------
    def to_dict(self) -> Dict[str, Any]:
        """Return a JSON-friendly dictionary."""
        return asdict(self)


# ---------------------------------------------------------------------------
# ExperimentReport
# ---------------------------------------------------------------------------

class ExperimentReport:
    """Structured container for a batch of experiment results.

    Attributes:
        configs: The configs that were executed.
        results: One metrics dict per config (order matches *configs*).
        models: Trained model objects (same order).
        dataset_info: Optional metadata about the graph dataset.
        timestamp: ISO-8601 timestamp of the run.
        run_id: Short identifier (``YYYYMMDD_HHMMSS``).
    """

    def __init__(
        self,
        configs: List[ExperimentConfig],
        results: List[Dict[str, Any]],
        models: List[torch.nn.Module],
        dataset_info: Optional[Dict[str, Any]] = None,
    ):
        self.configs = configs
        self.results = results
        self.models = models
        self.dataset_info = dataset_info or {}
        self.timestamp = datetime.now().isoformat()
        self.run_id = datetime.now().strftime('%Y%m%d_%H%M%S')

    # -- Tabular helpers ---------------------------------------------------

    def to_dataframe(self) -> pd.DataFrame:
        """Build a comparison DataFrame with one row per experiment."""
        rows = []
        for cfg, metrics in zip(self.configs, self.results):
            row = {
                'name': cfg.name,
                'model': cfg.model_name,
                'loss': cfg.loss_type,
                'activation': cfg.activation,
                'weight_decay': cfg.weight_decay,
                'hidden_dim': cfg.hidden_dim,
                'dropout': cfg.dropout,
                'test_macro_f1': metrics.get('test_macro_f1', float('nan')),
                'test_balanced_acc': metrics.get('test_balanced_acc', float('nan')),
                'test_mae': metrics.get('test_mae', float('nan')),
                'test_f1_weighted': metrics.get('test_f1', float('nan')),
                'val_macro_f1': metrics.get('val_macro_f1', float('nan')),
                'epochs_trained': metrics.get('epochs_trained', 0),
            }
            rows.append(row)
        return pd.DataFrame(rows)

    def summary(self, sort_by: str = 'test_macro_f1', ascending: bool = False) -> str:
        """Return a printable comparison table sorted by *sort_by*."""
        df = self.to_dataframe()
        if sort_by in df.columns:
            df = df.sort_values(sort_by, ascending=ascending)

        cols = ['name', 'model', 'loss', 'activation',
                'test_macro_f1', 'test_balanced_acc', 'test_mae', 'epochs_trained']
        display_cols = [c for c in cols if c in df.columns]

        lines = []
        lines.append('=' * 90)
        lines.append('EXPERIMENT COMPARISON')
        lines.append('=' * 90)

        # Header
        header = f"{'Name':<30} {'Model':<18} {'Loss':<7} {'Act':<10} "
        header += f"{'MacroF1':>8} {'BalAcc':>8} {'MAE':>8} {'Epochs':>6}"
        lines.append(header)
        lines.append('-' * 90)

        for _, row in df.iterrows():
            line = (
                f"{str(row.get('name', '')):<30} "
                f"{str(row.get('model', '')):<18} "
                f"{str(row.get('loss', '')):<7} "
                f"{str(row.get('activation', '')):<10} "
                f"{row.get('test_macro_f1', float('nan')):>8.4f} "
                f"{row.get('test_balanced_acc', float('nan')):>8.4f} "
                f"{row.get('test_mae', float('nan')):>8.4f} "
                f"{int(row.get('epochs_trained', 0)):>6}"
            )
            lines.append(line)

        lines.append('-' * 90)

        best = self.best()
        if best:
            lines.append(
                f"\nBest (Macro F1): {best['name']}  "
                f"= {best['test_macro_f1']:.4f}"
            )

        return '\n'.join(lines)

    # -- Best model --------------------------------------------------------

    def best(
        self,
        metric: str = 'test_macro_f1',
        higher_is_better: bool = True,
    ) -> Optional[Dict[str, Any]]:
        """Return config + metrics for the best experiment.

        Returns a dict with keys from both the config and the metrics.
        Returns ``None`` if there are no results.
        """
        if not self.results:
            return None

        values = [r.get(metric, float('-inf') if higher_is_better else float('inf'))
                  for r in self.results]
        idx = int(np.argmax(values) if higher_is_better else np.argmin(values))

        return {
            **self.configs[idx].to_dict(),
            **self.results[idx],
            '_index': idx,
        }

    def best_model(self, metric: str = 'test_macro_f1') -> Optional[torch.nn.Module]:
        """Return the trained model object for the best experiment."""
        info = self.best(metric)
        if info is None:
            return None
        return self.models[info['_index']]

    # -- JSON export -------------------------------------------------------

    def to_json(self, path: str | Path) -> None:
        """Write a JS-frontend-friendly JSON file.

        Schema::

            {
              "run_id": "...",
              "timestamp": "...",
              "dataset": { ... },
              "experiments": [ { "name", "config", "metrics" }, ... ],
              "best": { "name", "metric", "value" }
            }
        """
        experiments = []
        for cfg, metrics in zip(self.configs, self.results):
            experiments.append({
                'name': cfg.name,
                'config': cfg.to_dict(),
                'metrics': {k: _safe_json(v) for k, v in metrics.items()},
                'epochs_trained': metrics.get('epochs_trained', 0),
            })

        best_info = self.best()
        best_block = None
        if best_info is not None:
            best_block = {
                'name': best_info.get('name', ''),
                'metric': 'test_macro_f1',
                'value': _safe_json(best_info.get('test_macro_f1', 0)),
            }

        payload = {
            'run_id': self.run_id,
            'timestamp': self.timestamp,
            'dataset': {k: _safe_json(v) for k, v in self.dataset_info.items()},
            'experiments': experiments,
            'best': best_block,
        }

        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, 'w') as f:
            json.dump(payload, f, indent=2)


# ---------------------------------------------------------------------------
# ExperimentRunner
# ---------------------------------------------------------------------------

class ExperimentRunner:
    """Orchestrates training experiments over a shared dataset.

    Parameters
    ----------
    data : torch_geometric.data.Data
        Loaded graph data (output of ``GraphDataLoader.load()``).
    trainer_kwargs : dict, optional
        Extra keyword arguments forwarded to ``NodeClassificationTrainer``
        (e.g. ``use_class_weights``, ``holdout_fraction``).
    """

    def __init__(
        self,
        data,
        trainer_kwargs: Optional[Dict[str, Any]] = None,
    ):
        self.data = data
        self.trainer_kwargs = trainer_kwargs or {}
        # Build the trainer once; all experiments share the same split
        self.trainer = NodeClassificationTrainer(data, **self.trainer_kwargs)

    # -- Public API --------------------------------------------------------

    def run_all(
        self,
        configs: List[ExperimentConfig],
        verbose: bool = True,
        checkpoints: Optional[Dict[str, dict]] = None,
        frontier_mask: Optional[torch.Tensor] = None,
    ) -> ExperimentReport:
        """Execute every config and return a structured report.

        Parameters
        ----------
        configs : list[ExperimentConfig]
            Experiments to run (explicit list and/or output of ``grid()``).
        verbose : bool
            If *True*, print a header and progress for each experiment.
        checkpoints : dict[str, dict], optional
            Mapping from config *name* to a ``state_dict`` from a previous
            training run.  When a matching checkpoint is found for a config,
            a fresh model is created, the checkpoint weights are loaded with
            ``strict=False`` (allowing output-head size mismatches), and the
            pre-initialised model is passed to the trainer for fine-tuning.
        frontier_mask : torch.Tensor, optional
            Boolean mask [num_nodes] selecting which nodes contribute to the
            loss.  All nodes still participate in message passing but only
            frontier nodes are supervised.  Used for curriculum learning with
            frontier-only supervision.

        Returns
        -------
        ExperimentReport
        """
        checkpoints = checkpoints or {}
        all_results: List[Dict[str, Any]] = []
        all_models: List[torch.nn.Module] = []

        total = len(configs)
        for i, cfg in enumerate(configs, 1):
            if verbose:
                print(f"\n{'=' * 70}")
                print(f"[{i}/{total}] {cfg.name}")
                print(f"  model={cfg.model_name}  loss={cfg.loss_type}  "
                      f"act={cfg.activation}  wd={cfg.weight_decay}")
                if cfg.feature_set != 'full' or cfg.edge_feature_set != 'full':
                    print(f"  features={cfg.feature_set}  edge_features={cfg.edge_feature_set}")
                if cfg.name in checkpoints:
                    print(f"  [curriculum] restoring checkpoint from previous level")
                if frontier_mask is not None:
                    print(f"  [frontier] supervising {frontier_mask.sum().item()} / {len(frontier_mask)} nodes")
                print('=' * 70)

            checkpoint_state = checkpoints.get(cfg.name)
            model, metrics = self._run_one(cfg, verbose=verbose,
                                           checkpoint_state=checkpoint_state,
                                           frontier_mask=frontier_mask)
            all_results.append(metrics)
            all_models.append(model)

            if verbose:
                mf1 = metrics.get('test_macro_f1', float('nan'))
                ba = metrics.get('test_balanced_acc', float('nan'))
                mae = metrics.get('test_mae', float('nan'))
                print(f"  -> Macro F1={mf1:.4f}  BalAcc={ba:.4f}  MAE={mae:.4f}")

        # Build dataset info from the data object
        dataset_info = self._build_dataset_info()

        report = ExperimentReport(
            configs=configs,
            results=all_results,
            models=all_models,
            dataset_info=dataset_info,
        )

        if verbose:
            print(f"\n{report.summary()}")

        return report

    # -- Grid generation ---------------------------------------------------

    @staticmethod
    def grid(
        models: List[str],
        losses: Optional[List[str]] = None,
        activations: Optional[List[str]] = None,
        weight_decays: Optional[List[float]] = None,
        feature_sets: Optional[List[str]] = None,
        edge_feature_sets: Optional[List[str]] = None,
        **shared,
    ) -> List[ExperimentConfig]:
        """Generate configs from the cartesian product of supplied lists.

        Any parameter not supplied defaults to a single-element list
        containing the ``ExperimentConfig`` default for that field.

        Extra *shared* kwargs are forwarded to every generated config.

        Parameters
        ----------
        feature_sets : list[str], optional
            Node feature set names (keys from ``FEATURE_SETS``).
        edge_feature_sets : list[str], optional
            Edge feature set names (keys from ``EDGE_FEATURE_SETS``).

        Returns
        -------
        list[ExperimentConfig]
        """
        losses = losses or ['ce']
        activations = activations or [DEFAULT_ACTIVATION]
        weight_decays = weight_decays or [1e-3]
        feature_sets = feature_sets or ['full']
        edge_feature_sets = edge_feature_sets or ['full']

        configs: List[ExperimentConfig] = []
        for model, loss, act, wd, fs, efs in itertools.product(
            models, losses, activations, weight_decays,
            feature_sets, edge_feature_sets,
        ):
            name = f"{model}-{loss}-{act}"
            if len(weight_decays) > 1:
                name += f"-wd{wd}"
            if len(feature_sets) > 1:
                name += f"-{fs}"
            if len(edge_feature_sets) > 1:
                name += f"-e_{efs}"
            configs.append(ExperimentConfig(
                name=name,
                model_name=model,
                loss_type=loss,
                activation=act,
                weight_decay=wd,
                feature_set=fs,
                edge_feature_set=efs,
                **shared,
            ))
        return configs

    # -- Internals ---------------------------------------------------------

    def _run_one(
        self,
        config: ExperimentConfig,
        verbose: bool = True,
        checkpoint_state: Optional[dict] = None,
        frontier_mask: Optional[torch.Tensor] = None,
    ) -> Tuple[torch.nn.Module, Dict[str, Any]]:
        """Run a single experiment, delegating to the trainer.

        Parameters
        ----------
        checkpoint_state : dict, optional
            A ``state_dict`` from a previous training run.  When provided a
            fresh model is created for the current dataset, the checkpoint is
            loaded with ``strict=False`` (so output-head size mismatches are
            tolerated), and the pre-initialised model is handed to the trainer
            for fine-tuning instead of training from scratch.
        frontier_mask : torch.Tensor, optional
            Boolean mask for frontier-only supervision.  Passed through to
            ``trainer.train(frontier_mask=...)``.
        """
        from ..features.feature_sets import apply_node_mask, node_feature_indices
        from ..features.edge_feature_sets import edge_feature_indices

        # ----- Feature ablation setup -----
        # Node mask: store column indices on the trainer for inline slicing
        # (More robust than setting x_masked attribute on Data object)
        if config.feature_set != 'full':
            node_idx = node_feature_indices(config.feature_set)
            self.trainer._node_feat_idx = node_idx
            if verbose:
                print(f"  [Feature Mask] Node features: {self.trainer.data.x.shape[1]}D -> {len(node_idx)}D")
        else:
            self.trainer._node_feat_idx = None

        # Edge mask: store the set name on the trainer for _forward() to use
        self.trainer.edge_feature_set = config.edge_feature_set

        # Build model_kwargs: merge config-level params with extra model_kwargs
        model_kwargs = {
            'hidden_dim': config.hidden_dim,
            'dropout': config.dropout,
            'activation': config.activation,
            **config.model_kwargs,
        }

        # Override dimensions to match the ablated feature counts
        if config.feature_set != 'full':
            model_kwargs.setdefault('num_features', len(node_idx))
        if config.edge_feature_set != 'full':
            model_kwargs.setdefault(
                'num_edge_features',
                len(edge_feature_indices(config.edge_feature_set)),
            )

        # ----- Checkpoint restoration (curriculum fine-tuning) -----
        pretrained_model = None
        if checkpoint_state is not None and config.model_name != 'regression':
            # Create a fresh model whose architecture matches the *current*
            # dataset (num_classes may differ from the checkpoint's).
            # Use num_classes from model_kwargs if provided, otherwise use trainer's default
            model_kwargs_with_classes = model_kwargs.copy()
            model_kwargs_with_classes.setdefault('num_classes', self.trainer.num_classes)
            pretrained_model = get_model(
                config.model_name,
                **model_kwargs_with_classes,
            )
            # Load backbone weights; strict=False tolerates output-head
            # shape mismatches when num_classes changed between levels.
            missing, unexpected = pretrained_model.load_state_dict(
                checkpoint_state, strict=False,
            )
            if verbose and (missing or unexpected):
                print(f"  [curriculum] load_state_dict: "
                      f"{len(missing)} missing, {len(unexpected)} unexpected keys")

        # Regression uses a separate trainer method
        if config.model_name == 'regression':
            model, metrics = self.trainer.train_regression(
                weight_decay=config.weight_decay,
                learning_rate=config.learning_rate,
                max_epochs=config.max_epochs,
                patience=config.patience,
                verbose=verbose,
                **model_kwargs,
            )
        else:
            model, metrics = self.trainer.train(
                model_name=config.model_name,
                weight_decay=config.weight_decay,
                learning_rate=config.learning_rate,
                max_epochs=config.max_epochs,
                patience=config.patience,
                verbose=verbose,
                loss_type=config.loss_type,
                model=pretrained_model,
                frontier_mask=frontier_mask,
                **model_kwargs,
            )

        # Record which feature sets were used in the metrics
        metrics['feature_set'] = config.feature_set
        metrics['edge_feature_set'] = config.edge_feature_set

        return model, metrics

    def _build_dataset_info(self) -> Dict[str, Any]:
        """Extract dataset metadata from the loaded data object."""
        data = self.data
        info: Dict[str, Any] = {}

        info['num_nodes'] = int(data.x.shape[0])
        info['num_features'] = int(data.x.shape[1])
        if hasattr(data, 'edge_index') and data.edge_index is not None:
            info['num_edges'] = int(data.edge_index.shape[1] // 2)

        if hasattr(data, 'num_classes'):
            info['num_classes'] = int(data.num_classes)
        if hasattr(data, 'class_values'):
            info['classes'] = [int(v) for v in data.class_values]

        # Class counts
        if hasattr(data, 'y'):
            y = data.y.cpu()
            counts = torch.bincount(y).tolist()
            info['class_counts'] = counts

        return info


# ---------------------------------------------------------------------------
# Utilities
# ---------------------------------------------------------------------------

def _safe_json(val):
    """Coerce numpy / torch scalars to plain Python for ``json.dump``."""
    if isinstance(val, (np.integer,)):
        return int(val)
    if isinstance(val, (np.floating, float)):
        if np.isnan(val) or np.isinf(val):
            return None
        return float(val)
    if isinstance(val, (np.ndarray,)):
        return val.tolist()
    if isinstance(val, torch.Tensor):
        return val.detach().cpu().tolist()
    if isinstance(val, dict):
        return {k: _safe_json(v) for k, v in val.items()}
    if isinstance(val, (list, tuple)):
        return [_safe_json(v) for v in val]
    return val
