"""
Data Loader for Link Prediction Pipeline.

This module provides functionality to load graph data from Neo4j for the two-stage
link prediction pipeline:
- Filter to determined nodes only (determined=1)
- Compute integer values from muList using binary encoding
- Generate ground truth edges for SAME_DENOMINATOR and NEXT_INTEGER
- Build PyTorch Geometric Data objects ready for GNN training
"""

import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from collections import defaultdict

import numpy as np
import torch
from torch_geometric.data import Data

# Add parent directory to path to import neo4jClient
sys.path.insert(0, str(Path(__file__).parent.parent / 'neo4j'))
from neo4jClient import Neo4jClient

from .config import (
    NODE_QUERY,
    WNUM_QUERY,
    EDGE_QUERY,
    SAME_DENOMINATOR_GROUND_TRUTH_QUERY,
    muList_to_integer,
    validate_rational_encoding,
    encode_partition_id,
    get_muList_features,
    NUM_FEATURES_TASK1,
    NUM_FEATURES_TASK2,
)


class LinkPredictionDataLoader:
    """
    Loads graph data from Neo4j for link prediction tasks.
    
    This class handles:
    - Querying Neo4j for determined Dnode nodes (determined=1)
    - Computing integer values from muList binary encoding
    - Building feature matrices for Task 1 and Task 2
    - Generating ground truth edges for both tasks
    - Creating PyG Data objects with appropriate train/test splits
    
    Example:
        >>> loader = LinkPredictionDataLoader(client, database="d4seed1")
        >>> data_task1, data_task2 = loader.load()
        >>> print(f"Task 1: {data_task1.num_nodes} nodes, {len(data_task1.same_denom_edges)} edges")
    """
    
    def __init__(self, client: Neo4jClient, database: str):
        """
        Initialize the data loader.
        
        Args:
            client: Neo4jClient instance for database connectivity
            database: Target database name in Neo4j
        """
        self.client = client
        self.database = database
        self._node_id_to_idx: Dict[str, int] = {}
        self._idx_to_node_id: Dict[int, str] = {}
        self._nodes_df = None
        self._int_values = None
        self._partition_ids = None
    
    def load(self) -> Tuple[Data, Data]:
        """
        Load graph data for both tasks.
        
        Returns:
            Tuple of (data_task1, data_task2) where:
            - data_task1: PyG Data for SAME_DENOMINATOR prediction
            - data_task2: PyG Data for NEXT_INTEGER prediction
        
        Raises:
            RuntimeError: If query execution fails
            ValueError: If no determined nodes are found
        """
        print("Loading data from Neo4j...")
        
        # Load nodes (determined=1 only)
        self._nodes_df = self.client.run_query(NODE_QUERY, self.database)
        
        if len(self._nodes_df) == 0:
            raise ValueError("No determined nodes (determined=1) found in the database")
        
        print(f"  Found {len(self._nodes_df)} determined nodes")
        
        # Build node ID mapping
        self._node_id_to_idx = {
            node_id: idx for idx, node_id in enumerate(self._nodes_df['node_id'])
        }
        self._idx_to_node_id = {
            idx: node_id for node_id, idx in self._node_id_to_idx.items()
        }
        
        # Compute integer values from muList
        print("  Computing integer values from muList...")
        self._compute_integer_values()
        
        # Compute partition IDs from (n, d)
        print("  Computing partition IDs...")
        self._compute_partition_ids()
        
        # Load wNum values
        print("  Loading wNum values...")
        wnum_df = self.client.run_query(WNUM_QUERY, self.database)
        wnum_dict = dict(zip(wnum_df['node_id'], wnum_df['wNum']))
        
        # Load structural edges (zMap)
        print("  Loading zMap edges...")
        edges_df = self.client.run_query(EDGE_QUERY, self.database)
        edge_index = self._build_edge_index(edges_df)
        
        # Generate ground truth edges
        print("  Generating ground truth edges...")
        same_denom_edges = self._generate_same_denominator_edges()
        next_int_edges = self._generate_next_integer_edges()
        
        print(f"  Generated {len(same_denom_edges)} SAME_DENOMINATOR edges")
        print(f"  Generated {len(next_int_edges)} NEXT_INTEGER edges")
        
        # Build features for Task 1 (SAME_DENOMINATOR)
        print("  Building Task 1 features...")
        x_task1 = self._build_task1_features(wnum_dict)
        
        # Build features for Task 2 (NEXT_INTEGER)
        print("  Building Task 2 features...")
        x_task2 = self._build_task2_features()
        
        # Create PyG Data objects
        data_task1 = Data(
            x=x_task1,
            edge_index=edge_index,
            num_nodes=len(self._nodes_df),
        )
        data_task1.node_ids = self._nodes_df['node_id'].values.tolist()
        data_task1.partition_ids = self._partition_ids
        data_task1.same_denom_edges = same_denom_edges
        data_task1.n_values = torch.tensor(self._nodes_df['n'].values, dtype=torch.long)
        data_task1.d_values = torch.tensor(self._nodes_df['d'].values, dtype=torch.long)
        
        data_task2 = Data(
            x=x_task2,
            edge_index=edge_index,
            num_nodes=len(self._nodes_df),
        )
        data_task2.node_ids = self._nodes_df['node_id'].values.tolist()
        data_task2.partition_ids = self._partition_ids
        data_task2.int_values = self._int_values
        data_task2.next_int_edges = next_int_edges
        data_task2.same_denom_edges = same_denom_edges  # For validation
        
        print("Data loading complete!\n")
        
        return data_task1, data_task2
    
    def _compute_integer_values(self):
        """Compute integer values from muList for all nodes."""
        int_values = []
        for muList_str in self._nodes_df['muList']:
            try:
                int_val = muList_to_integer(str(muList_str))
                int_values.append(int_val)
            except ValueError as e:
                print(f"  Warning: Could not parse muList '{muList_str}': {e}")
                int_values.append(0)
        
        self._int_values = torch.tensor(int_values, dtype=torch.long)
    
    def _compute_partition_ids(self):
        """Compute partition IDs from (n, d) tuples."""
        partition_ids = []
        for _, row in self._nodes_df.iterrows():
            partition_id = encode_partition_id(int(row['n']), int(row['d']))
            partition_ids.append(partition_id)
        
        self._partition_ids = torch.tensor(partition_ids, dtype=torch.long)
    
    def _build_edge_index(self, edges_df) -> torch.Tensor:
        """
        Build edge index tensor for undirected graph.
        
        Args:
            edges_df: DataFrame with source and target columns
            
        Returns:
            Edge index tensor of shape [2, num_edges * 2]
        """
        if len(edges_df) == 0:
            return torch.zeros((2, 0), dtype=torch.long)
        
        edge_list = []
        for _, row in edges_df.iterrows():
            src_idx = self._node_id_to_idx.get(row['source'])
            tgt_idx = self._node_id_to_idx.get(row['target'])
            
            if src_idx is not None and tgt_idx is not None:
                # Add both directions for undirected graph
                edge_list.append([src_idx, tgt_idx])
                edge_list.append([tgt_idx, src_idx])
        
        if not edge_list:
            return torch.zeros((2, 0), dtype=torch.long)
        
        edge_index = torch.tensor(edge_list, dtype=torch.long).t().contiguous()
        return edge_index
    
    def _generate_same_denominator_edges(self) -> torch.Tensor:
        """
        Generate ground truth SAME_DENOMINATOR edges.
        
        All pairs of nodes with matching (n, d) should be connected.
        
        Returns:
            Tensor of shape [2, num_edges] with edge indices
        """
        # Group nodes by partition ID
        partitions = defaultdict(list)
        for idx, partition_id in enumerate(self._partition_ids.tolist()):
            partitions[partition_id].append(idx)
        
        # Generate all pairs within each partition
        edge_list = []
        for partition_id, node_indices in partitions.items():
            if len(node_indices) < 2:
                continue
            
            # Generate all pairs
            for i in range(len(node_indices)):
                for j in range(i + 1, len(node_indices)):
                    edge_list.append([node_indices[i], node_indices[j]])
                    edge_list.append([node_indices[j], node_indices[i]])
        
        if not edge_list:
            return torch.zeros((2, 0), dtype=torch.long)
        
        edges = torch.tensor(edge_list, dtype=torch.long).t().contiguous()
        return edges
    
    def _generate_next_integer_edges(self) -> torch.Tensor:
        """
        Generate ground truth NEXT_INTEGER edges.
        
        Within each partition, connect nodes with consecutive integer values.
        
        Returns:
            Tensor of shape [2, num_edges] with edge indices
        """
        # Group nodes by partition ID
        partitions = defaultdict(list)
        for idx, partition_id in enumerate(self._partition_ids.tolist()):
            int_val = self._int_values[idx].item()
            partitions[partition_id].append((idx, int_val))
        
        # Within each partition, sort by integer value and connect consecutive
        edge_list = []
        for partition_id, nodes_with_ints in partitions.items():
            if len(nodes_with_ints) < 2:
                continue
            
            # Sort by integer value
            sorted_nodes = sorted(nodes_with_ints, key=lambda x: x[1])
            
            # Connect consecutive integers
            for i in range(len(sorted_nodes) - 1):
                src_idx, src_int = sorted_nodes[i]
                dst_idx, dst_int = sorted_nodes[i + 1]
                
                # Only connect if truly consecutive
                if dst_int == src_int + 1:
                    edge_list.append([src_idx, dst_idx])
        
        if not edge_list:
            return torch.zeros((2, 0), dtype=torch.long)
        
        edges = torch.tensor(edge_list, dtype=torch.long).t().contiguous()
        return edges
    
    def _build_task1_features(self, wnum_dict: Dict[str, int]) -> torch.Tensor:
        """
        Build feature matrix for Task 1 (SAME_DENOMINATOR).
        
        Features: [n, d, n/d, totalZero, wNum, len(muList), max(muList)]
        
        Args:
            wnum_dict: Mapping from node_id to wNum value
            
        Returns:
            Feature tensor of shape [num_nodes, NUM_FEATURES_TASK1]
        """
        num_nodes = len(self._nodes_df)
        features = np.zeros((num_nodes, NUM_FEATURES_TASK1), dtype=np.float32)
        
        for idx, row in self._nodes_df.iterrows():
            node_id = row['node_id']
            
            n = float(row['n'])
            d = float(row['d'])
            ratio = n / d if d != 0 else 0.0
            totalZero = float(row.get('totalZero', 0))
            wNum = float(wnum_dict.get(node_id, 0))
            
            # Extract muList features
            muList_feats = get_muList_features(str(row['muList']))
            
            features[idx] = [
                n,
                d,
                ratio,
                totalZero,
                wNum,
                muList_feats['length'],
                muList_feats['max_pos'],
            ]
        
        return torch.tensor(features, dtype=torch.float32)
    
    def _build_task2_features(self) -> torch.Tensor:
        """
        Build feature matrix for Task 2 (NEXT_INTEGER).
        
        Features: [int_value, len(muList), max(muList), sum(muList), totalZero]
        
        Returns:
            Feature tensor of shape [num_nodes, NUM_FEATURES_TASK2]
        """
        num_nodes = len(self._nodes_df)
        features = np.zeros((num_nodes, NUM_FEATURES_TASK2), dtype=np.float32)
        
        for idx, row in self._nodes_df.iterrows():
            int_value = float(self._int_values[idx].item())
            totalZero = float(row.get('totalZero', 0))
            
            # Extract muList features
            muList_feats = get_muList_features(str(row['muList']))
            
            features[idx] = [
                int_value,
                muList_feats['length'],
                muList_feats['max_pos'],
                muList_feats['sum_pos'],
                totalZero,
            ]
        
        return torch.tensor(features, dtype=torch.float32)
    
    def get_node_id(self, idx: int) -> str:
        """Get the original Neo4j node ID for a given index."""
        return self._idx_to_node_id[idx]
    
    def get_node_index(self, node_id: str) -> int:
        """Get the sequential index for a given Neo4j node ID."""
        return self._node_id_to_idx[node_id]
    
    def get_partition_stats(self) -> Dict:
        """
        Get statistics about the partitions.
        
        Returns:
            Dictionary with partition statistics
        """
        unique_partitions = torch.unique(self._partition_ids)
        partition_sizes = []
        
        for partition_id in unique_partitions:
            size = (self._partition_ids == partition_id).sum().item()
            partition_sizes.append(size)
        
        return {
            'num_partitions': len(unique_partitions),
            'mean_partition_size': np.mean(partition_sizes),
            'max_partition_size': np.max(partition_sizes),
            'min_partition_size': np.min(partition_sizes),
        }
    
    def get_integer_stats(self) -> Dict:
        """
        Get statistics about the integer values.
        
        Returns:
            Dictionary with integer value statistics
        """
        int_vals = self._int_values.numpy()
        
        return {
            'min_int': int(np.min(int_vals)),
            'max_int': int(np.max(int_vals)),
            'mean_int': float(np.mean(int_vals)),
            'unique_ints': len(np.unique(int_vals)),
        }


def load_graph_from_neo4j(
    uri: str,
    user: str,
    password: str,
    database: str,
) -> Tuple[Data, Data, LinkPredictionDataLoader]:
    """
    Convenience function to load graph data from Neo4j.
    
    Args:
        uri: Neo4j connection URI (e.g., 'bolt://localhost:7687')
        user: Database username
        password: Database password
        database: Target database name
        
    Returns:
        Tuple of (data_task1, data_task2, loader)
        
    Example:
        >>> data_t1, data_t2, loader = load_graph_from_neo4j(
        ...     "bolt://localhost:7687", "neo4j", "password", "d4seed1"
        ... )
        >>> print(f"Loaded {data_t1.num_nodes} nodes")
    """
    client = Neo4jClient(uri, user, password)
    loader = LinkPredictionDataLoader(client, database)
    data_task1, data_task2 = loader.load()
    
    # Print statistics
    print("Dataset Statistics:")
    print(f"  Nodes: {data_task1.num_nodes}")
    print(f"  Structural edges (zMap): {data_task1.edge_index.size(1) // 2}")
    print(f"  SAME_DENOMINATOR edges: {data_task1.same_denom_edges.size(1)}")
    print(f"  NEXT_INTEGER edges: {data_task2.next_int_edges.size(1)}")
    
    partition_stats = loader.get_partition_stats()
    print(f"\nPartition Statistics:")
    print(f"  Number of partitions: {partition_stats['num_partitions']}")
    print(f"  Mean partition size: {partition_stats['mean_partition_size']:.2f}")
    print(f"  Max partition size: {partition_stats['max_partition_size']}")
    
    int_stats = loader.get_integer_stats()
    print(f"\nInteger Value Statistics:")
    print(f"  Range: [{int_stats['min_int']}, {int_stats['max_int']}]")
    print(f"  Unique integers: {int_stats['unique_ints']}")
    print()
    
    return data_task1, data_task2, loader


def split_edges_train_test(
    edges: torch.Tensor,
    test_fraction: float = 0.2,
    random_seed: int = 49
) -> Tuple[torch.Tensor, torch.Tensor]:
    """
    Split edges into train and test sets.
    
    Args:
        edges: Edge tensor of shape [2, num_edges]
        test_fraction: Fraction of edges to use for testing
        random_seed: Random seed for reproducibility
        
    Returns:
        Tuple of (train_edges, test_edges)
    """
    if edges.size(1) == 0:
        return edges, torch.zeros((2, 0), dtype=torch.long)
    
    # Set random seed
    torch.manual_seed(random_seed)
    
    num_edges = edges.size(1)
    perm = torch.randperm(num_edges)
    
    num_test = int(num_edges * test_fraction)
    test_idx = perm[:num_test]
    train_idx = perm[num_test:]
    
    train_edges = edges[:, train_idx]
    test_edges = edges[:, test_idx]
    
    return train_edges, test_edges


def generate_negative_samples(
    num_nodes: int,
    positive_edges: torch.Tensor,
    num_negative: int,
    random_seed: int = 49
) -> torch.Tensor:
    """
    Generate negative edge samples for link prediction training.
    
    Args:
        num_nodes: Total number of nodes
        positive_edges: Positive edge tensor of shape [2, num_pos_edges]
        num_negative: Number of negative samples to generate
        random_seed: Random seed for reproducibility
        
    Returns:
        Negative edge tensor of shape [2, num_negative]
    """
    torch.manual_seed(random_seed)
    
    # Convert positive edges to set for fast lookup
    pos_edge_set = set()
    for i in range(positive_edges.size(1)):
        src = positive_edges[0, i].item()
        dst = positive_edges[1, i].item()
        pos_edge_set.add((src, dst))
        pos_edge_set.add((dst, src))  # Undirected
    
    # Sample negative edges
    negative_edges = []
    attempts = 0
    max_attempts = num_negative * 10
    
    while len(negative_edges) < num_negative and attempts < max_attempts:
        src = torch.randint(0, num_nodes, (1,)).item()
        dst = torch.randint(0, num_nodes, (1,)).item()
        
        if src != dst and (src, dst) not in pos_edge_set:
            negative_edges.append([src, dst])
            pos_edge_set.add((src, dst))
            pos_edge_set.add((dst, src))
        
        attempts += 1
    
    if len(negative_edges) == 0:
        return torch.zeros((2, 0), dtype=torch.long)
    
    return torch.tensor(negative_edges, dtype=torch.long).t().contiguous()

