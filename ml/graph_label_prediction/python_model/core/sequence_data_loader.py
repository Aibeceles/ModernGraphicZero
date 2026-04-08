"""
Data Loading for the RootList Sequence Prediction Pipeline.

This module provides:
- SequenceDataLoader: Loads the full graph (all nodes, all edges) from Neo4j
  for GCN embeddings, plus the wNum=0 leaf-node subset with complete RootLists
  for sequence training.
- SequenceDataset: PyTorch Dataset that wraps sliding-window (context, target)
  pairs produced by SequenceDataLoader.
"""

from typing import Dict, List, Tuple

import numpy as np
import torch
from torch.utils.data import Dataset
from torch_geometric.data import Data

from .config import (
    get_node_query,
    get_edge_query,
    get_sequence_node_query,
    USE_GRAPH_FILTERING,
)
from .coefficient_features import extract_coefficient_features
from .sequence_config import SequencePredictorConfig


# ---------------------------------------------------------------------------
# SequenceDataLoader
# ---------------------------------------------------------------------------

class SequenceDataLoader:
    """
    Loads data from Neo4j for RootList sequence prediction.

    Uses the FULL GRAPH for GCN embeddings (all nodes, all edges) but only
    trains sequence prediction on wNum=0 nodes with complete RootLists.
    """

    def __init__(self, client, database: str, config: SequencePredictorConfig):
        self.client = client
        self.database = database
        self.config = config

        self._node_id_to_idx: Dict[str, int] = {}
        self._idx_to_node_id: Dict[int, str] = {}

        self.all_nodes_df = None
        self.edges_df = None
        self.node_features: torch.Tensor = None  # type: ignore[assignment]
        self.edge_index: torch.Tensor = None     # type: ignore[assignment]

        self.seq_nodes_df = None
        self.sequence_node_indices: List[int] = []
        self.root_lists: List[List[int]] = []

    # ------------------------------------------------------------------
    # public API
    # ------------------------------------------------------------------

    def load(self) -> 'SequenceDataLoader':
        """Load data from Neo4j (full graph + sequence subset). Returns self."""

        all_nodes_query = get_node_query(USE_GRAPH_FILTERING)
        edge_query = get_edge_query(USE_GRAPH_FILTERING)
        seq_node_query = get_sequence_node_query(USE_GRAPH_FILTERING)

        # Step 1 – all nodes
        print("Loading ALL nodes from Neo4j (full graph for GCN embeddings)...")
        self.all_nodes_df = self.client.run_query(all_nodes_query, self.database)
        print(f"  Loaded {len(self.all_nodes_df)} total nodes")

        if len(self.all_nodes_df) == 0:
            raise ValueError("No nodes found in database")

        self._node_id_to_idx = {
            nid: idx for idx, nid in enumerate(self.all_nodes_df['node_id'])
        }
        self._idx_to_node_id = {v: k for k, v in self._node_id_to_idx.items()}

        self._build_node_features()

        # Step 2 – all edges
        print("Loading ALL edges from Neo4j (full graph for GCN)...")
        self.edges_df = self.client.run_query(edge_query, self.database)
        self._build_edge_index()
        print(f"  Loaded {self.edge_index.shape[1]} edges (undirected)")

        # Step 3 – wNum=0 sequence nodes
        print("Loading wNum=0 nodes for sequence prediction...")
        self.seq_nodes_df = self.client.run_query(seq_node_query, self.database)
        print(f"  Loaded {len(self.seq_nodes_df)} leaf nodes with complete RootLists")

        if len(self.seq_nodes_df) == 0:
            raise ValueError("No wNum=0 nodes with complete RootLists found")

        self._build_sequence_mappings()
        self._extract_root_lists()

        return self

    def create_sequence_dataset(
        self,
        train_ratio: float = 0.7,
        val_ratio: float = 0.15,
        test_ratio: float = 0.15,
    ) -> Tuple['SequenceDataset', 'SequenceDataset', 'SequenceDataset']:
        """Create train/val/test SequenceDataset splits."""

        num_seq_nodes = len(self.sequence_node_indices)
        window_size = self.config.context_window_size

        samples: List[Tuple[List[int], int]] = []
        for i in range(num_seq_nodes - window_size):
            context_global_indices = [
                self.sequence_node_indices[j] for j in range(i, i + window_size)
            ]
            target_seq_idx = i + window_size
            samples.append((context_global_indices, target_seq_idx))

        print(f"Created {len(samples)} sequence samples (window size = {window_size})")
        print(f"  Using {num_seq_nodes} wNum=0 nodes from full graph of {len(self.node_features)} nodes")

        np.random.seed(self.config.random_seed)
        indices = np.arange(len(samples))
        np.random.shuffle(indices)

        n_train = int(len(samples) * train_ratio)
        n_val = int(len(samples) * val_ratio)

        train_samples = [samples[i] for i in indices[:n_train]]
        val_samples = [samples[i] for i in indices[n_train:n_train + n_val]]
        test_samples = [samples[i] for i in indices[n_train + n_val:]]

        print(f"Split: train={len(train_samples)}, val={len(val_samples)}, test={len(test_samples)}")

        common = dict(
            node_features=self.node_features,
            root_lists=self.root_lists,
            edge_index=self.edge_index,
            config=self.config,
        )
        return (
            SequenceDataset(samples=train_samples, **common),
            SequenceDataset(samples=val_samples, **common),
            SequenceDataset(samples=test_samples, **common),
        )

    def get_pyg_data(self) -> Data:
        """Return a PyG Data object for the full graph."""
        return Data(x=self.node_features, edge_index=self.edge_index)

    # ------------------------------------------------------------------
    # internals
    # ------------------------------------------------------------------

    def _build_node_features(self):
        """Build 16-D feature matrix for ALL nodes using extract_coefficient_features."""
        num_nodes = len(self.all_nodes_df)
        features = np.zeros((num_nodes, self.config.num_node_features), dtype=np.float32)

        for i, (_, row) in enumerate(self.all_nodes_df.iterrows()):
            features[i, 0] = float(row.get('wNum', 0) or 0)

            coeffs, degree, stats = extract_coefficient_features(
                row.get('vmResult'), max_degree=4,
            )
            features[i, 1] = degree
            features[i, 2] = float(row.get('determined', 0) or 0)
            features[i, 3:8] = coeffs
            features[i, 8:13] = stats

            n_val = float(row.get('n', 0) or 0)
            mu_d_val = float(row.get('mu_d', 0) or 0)
            mu_ratio = n_val / mu_d_val if mu_d_val > 0 else 0.0
            features[i, 13] = n_val
            features[i, 14] = mu_d_val
            features[i, 15] = mu_ratio

        self.node_features = torch.tensor(features, dtype=torch.float32)
        print(f"  Built feature matrix for full graph: {self.node_features.shape}")

    def _build_edge_index(self):
        """Build edge index tensor for the FULL graph (undirected)."""
        if len(self.edges_df) == 0:
            self.edge_index = torch.zeros((2, 0), dtype=torch.long)
            return

        edge_list = []
        edges_missing = 0

        for _, row in self.edges_df.iterrows():
            src_idx = self._node_id_to_idx.get(row['source'])
            tgt_idx = self._node_id_to_idx.get(row['target'])

            if src_idx is not None and tgt_idx is not None:
                edge_list.append([src_idx, tgt_idx])
                edge_list.append([tgt_idx, src_idx])
            else:
                edges_missing += 1

        if edges_missing > 0:
            print(f"  Warning: {edges_missing} edges reference nodes not in graph")

        if edge_list:
            self.edge_index = torch.tensor(edge_list, dtype=torch.long).t().contiguous()
        else:
            self.edge_index = torch.zeros((2, 0), dtype=torch.long)

    def _build_sequence_mappings(self):
        """Map sequence node IDs to global indices."""
        self.sequence_node_indices = []
        missing_count = 0

        for node_id in self.seq_nodes_df['node_id']:
            global_idx = self._node_id_to_idx.get(node_id)
            if global_idx is not None:
                self.sequence_node_indices.append(global_idx)
            else:
                missing_count += 1

        if missing_count > 0:
            print(f"  Warning: {missing_count} sequence nodes not found in full graph")
        print(f"  Mapped {len(self.sequence_node_indices)} sequence nodes to global indices")

    def _extract_root_lists(self):
        """Extract RootList for each wNum=0 sequence node."""
        self.root_lists = []
        for _, row in self.seq_nodes_df.iterrows():
            root_list = row.get('RootList', [])
            if root_list is None:
                root_list = []
            elif isinstance(root_list, str):
                root_list = root_list.strip('[]')
                if root_list:
                    root_list = [int(x.strip()) for x in root_list.split(',')]
                else:
                    root_list = []
            self.root_lists.append(list(root_list))

        sizes = [len(rl) for rl in self.root_lists]
        print(f"  RootList size distribution: min={min(sizes)}, max={max(sizes)}, mean={np.mean(sizes):.2f}")


# ---------------------------------------------------------------------------
# SequenceDataset
# ---------------------------------------------------------------------------

class SequenceDataset(Dataset):
    """
    PyTorch Dataset for sequence prediction.

    Each sample contains global node indices for the context window and the
    tokenised target RootList.
    """

    def __init__(
        self,
        samples: List[Tuple[List[int], int]],
        node_features: torch.Tensor,
        root_lists: List[List[int]],
        edge_index: torch.Tensor,
        config: SequencePredictorConfig,
    ):
        self.samples = samples
        self.node_features = node_features
        self.root_lists = root_lists
        self.edge_index = edge_index
        self.config = config

    def __len__(self) -> int:
        return len(self.samples)

    def root_to_token(self, root_value: int) -> int:
        return root_value - self.config.min_root_value

    def token_to_root(self, token_id: int) -> int:
        return token_id + self.config.min_root_value

    def tokenize_root_list(self, root_list: List[int]) -> torch.Tensor:
        """Tokenize a RootList into ``[size, r1, r2, ..., PAD, ...]``."""
        max_len = self.config.max_rootlist_size + 1
        tokens = torch.full((max_len,), self.config.pad_token_id, dtype=torch.long)
        tokens[0] = len(root_list)
        for i, root in enumerate(root_list[:self.config.max_rootlist_size]):
            tokens[i + 1] = self.root_to_token(root)
        return tokens

    def __getitem__(self, idx: int) -> Dict[str, torch.Tensor]:
        context_global_indices, target_seq_idx = self.samples[idx]
        target_root_list = self.root_lists[target_seq_idx]
        target_tokens = self.tokenize_root_list(target_root_list)
        target_size = len(target_root_list)

        target_mask = torch.zeros(self.config.max_rootlist_size + 1, dtype=torch.bool)
        target_mask[:target_size + 1] = True

        return {
            'context_indices': torch.tensor(context_global_indices, dtype=torch.long),
            'target_tokens': target_tokens,
            'target_size': torch.tensor(target_size, dtype=torch.long),
            'target_mask': target_mask,
        }
