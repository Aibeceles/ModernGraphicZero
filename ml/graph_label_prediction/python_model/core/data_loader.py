"""
Data Loader for Root Count Prediction Pipeline.

This module provides functionality to load graph data from Neo4j and convert it
to PyTorch Geometric format for predicting the number of rational roots (totalZero).

IMPROVED VERSION: Adds spectral positional encodings for graph structure awareness.
"""

import sys
from pathlib import Path
from typing import Dict, Optional, Tuple

import numpy as np
import torch
from torch_geometric.data import Data

# Add parent directory to path to import neo4jClient
sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'neo4j'))
from neo4jClient import Neo4jClient

from .config import (
    BASE_FEATURES,
    SPECTRAL_PE_DIM,
    USE_GRAPH_FILTERING,
    MAX_NODES_FOR_SPECTRAL_PE,
    PARRAY_MIN,
    PARRAY_MAX,
    NUM_BASE_FEATURES,
    NUM_COEFFICIENT_FEATURES,
    NUM_STATISTICAL_FEATURES,
    NUM_SET_UNION_FEATURES,
    NUM_FEATURES_TOTAL,
    MAX_POLYNOMIAL_DEGREE,
    get_node_query,
    get_edge_query,
    get_incremental_queries,
)
from .coefficient_features import extract_coefficient_features


class GraphDataLoader:
    """
    Loads graph data from Neo4j and converts to PyTorch Geometric format.
    
    This class handles:
    - Querying Neo4j for Dnode nodes and zMap relationships
    - Building feature matrices from node properties
    - Constructing edge indices for undirected graph representation
    - Creating PyG Data objects ready for GNN training
    
    Example:
        >>> loader = GraphDataLoader(client, database="d4seed1")
        >>> data = loader.load()
        >>> print(f"Nodes: {data.num_nodes}, Edges: {data.num_edges}")
    """
    
    def __init__(
        self,
        client: Neo4jClient,
        database: str,
        use_filtering: bool = USE_GRAPH_FILTERING,
        min_wnum: Optional[int] = None,
    ):
        """
        Initialize the data loader.
        
        Args:
            client: Neo4jClient instance for database connectivity
            database: Target database name in Neo4j
            use_filtering: Whether to use graph filtering (for large graphs)
            min_wnum: Optional minimum wNum threshold for incremental learning.
                When set, uses parameterised incremental queries (``cb.wNum >= min_wnum``)
                instead of the default filtered queries.  Overrides *use_filtering*.
        """
        self.client = client
        self.database = database
        self.use_filtering = use_filtering
        self.min_wnum = min_wnum
        self._node_id_to_idx: Dict[int, int] = {}
        self._idx_to_node_id: Dict[int, int] = {}
    
    def load(self) -> Data:
        """
        Load graph data from Neo4j and convert to PyG Data format.
        
        Uses filtered queries if use_filtering=True to reduce graph size
        for spectral PE computation.
        
        Returns:
            PyG Data object containing:
                - x: Node feature matrix [num_nodes, NUM_FEATURES_TOTAL + SPECTRAL_PE_DIM]
                     where NUM_FEATURES_TOTAL = 3 + (MAX_GRAPH_DEPTH+1) + 5 + 3 
                     (wNum + degree + determined + coeffs + stats + set_union)
                - edge_index: Edge connectivity [2, num_edges]
                - y: Default training labels [num_nodes] (y_visible = distinct roots visible in truncated window)
                - y_visible: Same as y (explicit)
                - y_true: Optional "true" label (degree_at_node) for censored experiments; meaningful only when determined=1
                - determined: Boolean flag (computed on larger/full scan window; separate completeness indicator)
                - node_ids: Original Neo4j node IDs (elementId strings) for write-back
                - wNum: Convenience copy of the wNum feature column
                - degree_at_node: Derived degree (float) from vmResult (coeff list)
        
        Raises:
            RuntimeError: If query execution fails
            ValueError: If no nodes are found in the database
        """
        # Get appropriate queries based on filtering / incremental configuration
        if self.min_wnum is not None:
            node_query, edge_query = get_incremental_queries(self.min_wnum)
            print(f"Using incremental queries (wNum >= {self.min_wnum})...")
        else:
            node_query = get_node_query(self.use_filtering)
            edge_query = get_edge_query(self.use_filtering)
            if self.use_filtering:
                print(f"Using filtered queries (pArrayList ∈ [{PARRAY_MIN}, {PARRAY_MAX}))...")
        
        # Load nodes
        nodes_df = self.client.run_query(node_query, self.database)
        
        if len(nodes_df) == 0:
            raise ValueError("No Dnode nodes found in the database")
        
        # Build node ID mapping (Neo4j ID -> sequential index)
        self._node_id_to_idx = {
            node_id: idx for idx, node_id in enumerate(nodes_df['node_id'])
        }
        self._idx_to_node_id = {
            idx: node_id for node_id, idx in self._node_id_to_idx.items()
        }
        
        # Build feature matrix (wNum only)
        x = self._build_feature_matrix(nodes_df)
        
        # Build labels (default = visible distinct root count in truncated window)
        # Validate labels are non-negative integers
        labels = nodes_df['label'].fillna(0).values.astype(np.int64)
        if (labels < 0).any():
            raise ValueError("Found negative labels in database")
        y_raw = torch.tensor(labels, dtype=torch.long)
        
        # Build contiguous label mapping for training.
        # Raw root counts may be sparse (e.g. {0,1,3,5}) -- CORAL and CE
        # require labels in [0..K-1].  We map to contiguous indices and
        # store the reverse mapping so predictions can be decoded later.
        classes_present = sorted(torch.unique(y_raw).tolist())
        num_classes_present = len(classes_present)
        raw_to_contiguous = {int(raw): idx for idx, raw in enumerate(classes_present)}
        y_mapped = torch.tensor(
            [raw_to_contiguous[int(v)] for v in labels], dtype=torch.long,
        )
        
        print(f"Label mapping: {num_classes_present} classes present "
              f"{classes_present} -> [0..{num_classes_present - 1}]")
        
        # Store original node IDs (elementIds are strings) for write-back
        node_ids = nodes_df['node_id'].values  # Keep as numpy array of strings
        
        # Load edges (using filtered query if enabled)
        edges_df = self.client.run_query(edge_query, self.database)
        
        # Build edge index (undirected: add both directions)
        edge_index = self._build_edge_index(edges_df)
        
        # Create PyG Data object (y = contiguous mapped labels for training)
        data = Data(x=x, edge_index=edge_index, y=y_mapped)
        # Store node_ids as list of strings for prediction write-back
        data.node_ids = node_ids.tolist() if hasattr(node_ids, 'tolist') else list(node_ids)
        
        # Label mapping metadata -- used by trainer / predictor to decode
        data.y_raw = y_raw                          # original raw root counts
        data.class_values = classes_present          # [raw_val_0, ..., raw_val_{K-1}]
        data.num_classes = num_classes_present       # K
        data.raw_to_contiguous = raw_to_contiguous   # {raw -> contiguous idx}
        
        # Explicitly store targets/flags for downstream trainers
        # y_visible uses the same contiguous mapping as y
        data.y_visible = y_mapped.clone()

        # determined is computed on a larger/full scan window (separate completeness indicator)
        if 'determined' in nodes_df.columns:
            determined = nodes_df['determined'].fillna(0).values.astype(np.int64)
            data.determined = torch.tensor(determined, dtype=torch.bool)
        else:
            data.determined = torch.zeros(data.num_nodes, dtype=torch.bool)

        # Store RootList if present (debug/analysis; not used directly by models)
        if 'RootList' in nodes_df.columns:
            data.RootList = nodes_df['RootList'].tolist()

        # Store totalZero if present (sanity check vs y_visible)
        if 'totalZero' in nodes_df.columns:
            total_zero = nodes_df['totalZero'].fillna(0).values.astype(np.int64)
            data.totalZero = torch.tensor(total_zero, dtype=torch.long)

        # Store window metadata (may be all-null depending on schema)
        for col in ('windowMin', 'windowMax', 'windowSize'):
            if col in nodes_df.columns:
                vals = nodes_df[col].fillna(0).values.astype(np.float32)
                setattr(data, col, torch.tensor(vals, dtype=torch.float32))

        # Convenience copies of base features
        data.wNum = x[:, 0].clone()  # required for depth-aware attention
        if NUM_BASE_FEATURES >= 2:
            data.degree_at_node = x[:, 1].clone()

        # Optional y_true target for censored experiments:
        # define y_true = round(degree_at_node), but only treat it as valid when determined=1.
        # For undetermined nodes, set to -1 sentinel.
        # Values are remapped to contiguous indices consistent with data.y.
        y_true = torch.full((data.num_nodes,), -1, dtype=torch.long)
        if hasattr(data, 'degree_at_node'):
            deg_int = data.degree_at_node.round().clamp(min=0).long()
            # Remap determined y_true values to contiguous label space
            det_mask = data.determined
            for raw_val, cont_idx in raw_to_contiguous.items():
                y_true[det_mask & (deg_int == raw_val)] = cont_idx
            # Values not in the mapping stay at -1 (unknown class)
        data.y_true = y_true
        
        # Add spectral positional encodings (if enabled and graph size is manageable)
        if SPECTRAL_PE_DIM == 0:
            print(f"Spectral PE disabled. Using {NUM_FEATURES_TOTAL} features (base + coefficient + statistical + set_union).")
        elif data.num_nodes > MAX_NODES_FOR_SPECTRAL_PE:
            print(f"Graph has {data.num_nodes:,} nodes (>{MAX_NODES_FOR_SPECTRAL_PE:,})")
            print("Skipping spectral PE computation (requires too much memory)")
            print(f"Using only base features as input (dim={NUM_BASE_FEATURES})")
            # Pad with zeros to maintain expected feature dimension
            pe_placeholder = torch.zeros(data.num_nodes, SPECTRAL_PE_DIM)
            data.x = torch.cat([data.x, pe_placeholder], dim=-1)
        else:
            print(f"Computing spectral positional encodings for {data.num_nodes:,} nodes...")
            data = self._add_spectral_positional_encodings(data)
            print(f"  Feature dim: {data.x.shape[1]} ({NUM_FEATURES_TOTAL} base+coeff+stats+set_union + {SPECTRAL_PE_DIM} spectral PE)")
        
        return data
    
    def _build_feature_matrix(self, nodes_df) -> torch.Tensor:
        """
        Build feature matrix: [wNum, degree, determined, coeffs, stats, set_union] = NUM_FEATURES_TOTAL dimensions.

        Features extracted:
        - wNum: depth/index in the difference chain (from CreatedBy)
        - degree_at_node: polynomial degree from vmResult
        - determined: boolean flag from larger/full scan window
        - coeff_0 to coeff_{MAX_POLYNOMIAL_DEGREE}: padded polynomial coefficients (ascending power order)
        - Statistical features: magnitude, leading_coeff_abs, constant_term_abs, 
                                sparsity, mean_abs
        - Set Union Ratio features: n (μ numerator), mu_d (μ denominator), mu_ratio (n/d)
        
        Args:
            nodes_df: DataFrame with node properties
            
        Returns:
            Feature tensor of shape [num_nodes, NUM_FEATURES_TOTAL]
        """
        num_nodes = len(nodes_df)
        
        # Feature 1: wNum
        if 'wNum' in nodes_df.columns:
            wnum_values = nodes_df['wNum'].fillna(0).values.astype(np.float32).reshape(-1, 1)
        else:
            wnum_values = np.zeros((num_nodes, 1), dtype=np.float32)
        
        # Feature 2: degree (derived from vmResult)
        degree_values = np.zeros((num_nodes, 1), dtype=np.float32)
        coeff_values = np.zeros((num_nodes, NUM_COEFFICIENT_FEATURES), dtype=np.float32)
        stats_values = np.zeros((num_nodes, NUM_STATISTICAL_FEATURES), dtype=np.float32)
        
        if 'vmResult' in nodes_df.columns:
            for i, vm_result in enumerate(nodes_df['vmResult']):
                coeffs, degree, stats = extract_coefficient_features(
                    vm_result, 
                    max_degree=MAX_POLYNOMIAL_DEGREE
                )
                degree_values[i, 0] = degree
                coeff_values[i, :] = coeffs
                stats_values[i, :] = stats
        
        # Feature 3: determined (binary flag)
        if 'determined' in nodes_df.columns:
            determined_values = nodes_df['determined'].fillna(0).values.astype(np.float32).reshape(-1, 1)
        else:
            determined_values = np.zeros((num_nodes, 1), dtype=np.float32)
        
        # Set Union Ratio features: n, mu_d, mu_ratio
        n_values = np.zeros((num_nodes, 1), dtype=np.float32)
        mu_d_values = np.zeros((num_nodes, 1), dtype=np.float32)
        mu_ratio_values = np.zeros((num_nodes, 1), dtype=np.float32)
        
        if 'n' in nodes_df.columns:
            n_values = nodes_df['n'].fillna(0).values.astype(np.float32).reshape(-1, 1)
        if 'mu_d' in nodes_df.columns:
            mu_d_values = nodes_df['mu_d'].fillna(0).values.astype(np.float32).reshape(-1, 1)
            # Compute mu_ratio = n / d (with safe division to avoid div by zero)
            with np.errstate(divide='ignore', invalid='ignore'):
                mu_ratio_values = np.where(mu_d_values > 0, n_values / mu_d_values, 0.0)
        
        # Concatenate all features: [wNum, degree, determined, coeffs, stats, n, mu_d, mu_ratio]
        feature_matrix = np.concatenate([
            wnum_values,        # [N, 1]
            degree_values,      # [N, 1]
            determined_values,  # [N, 1]
            coeff_values,       # [N, 5]
            stats_values,       # [N, 5]
            n_values,           # [N, 1]
            mu_d_values,        # [N, 1]
            mu_ratio_values,    # [N, 1]
        ], axis=1)  # [N, 16]
        
        assert feature_matrix.shape[1] == NUM_FEATURES_TOTAL, \
            f"Expected {NUM_FEATURES_TOTAL} features, got {feature_matrix.shape[1]}"
        
        return torch.tensor(feature_matrix, dtype=torch.float32)

    # Note: _degree_from_vmresult replaced with extract_coefficient_features
    # The old method incorrectly assumed vmResult had ascending order with placeholder.
    # New method correctly parses descending power order as documented in 03_implementation.md
    
    def _add_spectral_positional_encodings(
        self, 
        data: Data, 
        k: int = SPECTRAL_PE_DIM
    ) -> Data:
        """
        Add spectral positional encodings based on graph Laplacian eigenvectors.
        
        Spectral PE captures global graph structure by using the smallest
        eigenvectors of the normalized Laplacian. This gives each node a
        unique "position" in the graph that standard message passing misses.
        
        Args:
            data: PyG Data object with edge_index
            k: Number of eigenvector dimensions to use
            
        Returns:
            Data object with spectral PE concatenated to features
        """
        try:
            from scipy.sparse import csr_matrix
            from scipy.sparse.linalg import eigsh
        except ImportError:
            print("Warning: scipy not available, skipping spectral PE")
            # Pad with zeros if scipy unavailable
            pe = torch.zeros(data.num_nodes, k)
            data.x = torch.cat([data.x, pe], dim=-1)
            return data
        
        num_nodes = data.num_nodes
        edge_index = data.edge_index.cpu().numpy()
        
        if edge_index.shape[1] == 0:
            # No edges - use zero PE
            pe = torch.zeros(num_nodes, k)
            data.x = torch.cat([data.x, pe], dim=-1)
            return data
        
        # Build adjacency matrix
        row, col = edge_index[0], edge_index[1]
        data_vals = np.ones(len(row))
        adj = csr_matrix((data_vals, (row, col)), shape=(num_nodes, num_nodes))
        
        # Compute degree matrix
        degrees = np.array(adj.sum(axis=1)).flatten()
        degrees = np.maximum(degrees, 1)  # Avoid division by zero
        
        # Compute normalized Laplacian: L = I - D^{-1/2} A D^{-1/2}
        d_inv_sqrt = 1.0 / np.sqrt(degrees)
        d_inv_sqrt_matrix = csr_matrix(
            (d_inv_sqrt, (np.arange(num_nodes), np.arange(num_nodes))),
            shape=(num_nodes, num_nodes)
        )
        
        # L_norm = I - D^{-1/2} A D^{-1/2}
        normalized_adj = d_inv_sqrt_matrix @ adj @ d_inv_sqrt_matrix
        laplacian = csr_matrix(np.eye(num_nodes)) - normalized_adj
        
        # Compute smallest eigenvectors (excluding the trivial constant eigenvector)
        # Request k+1 eigenvectors since we'll skip the first one
        try:
            num_eigenvectors = min(k + 1, num_nodes - 2)
            if num_eigenvectors < 2:
                # Graph too small
                pe = torch.zeros(num_nodes, k)
                data.x = torch.cat([data.x, pe], dim=-1)
                return data
                
            eigenvalues, eigenvectors = eigsh(
                laplacian.astype(np.float64), 
                k=num_eigenvectors, 
                which='SM',  # Smallest magnitude
                maxiter=num_nodes * 10
            )
            
            # Sort by eigenvalue
            idx = np.argsort(eigenvalues)
            eigenvectors = eigenvectors[:, idx]
            
            # Skip the first (constant) eigenvector, take next k
            if eigenvectors.shape[1] > k:
                pe = eigenvectors[:, 1:k+1]
            else:
                # Pad if we don't have enough eigenvectors
                pe = np.zeros((num_nodes, k))
                available = eigenvectors.shape[1] - 1
                if available > 0:
                    pe[:, :available] = eigenvectors[:, 1:1+available]
                    
        except Exception as e:
            print(f"Warning: Spectral PE computation failed ({e}), using zeros")
            pe = np.zeros((num_nodes, k))
        
        # Handle sign ambiguity - make eigenvectors consistent
        # Use the sign that makes the first non-zero element positive
        for i in range(pe.shape[1]):
            if np.abs(pe[:, i]).sum() > 0:
                first_nonzero_idx = np.argmax(np.abs(pe[:, i]) > 1e-8)
                if pe[first_nonzero_idx, i] < 0:
                    pe[:, i] = -pe[:, i]
        
        # Concatenate with original features
        pe_tensor = torch.tensor(pe, dtype=torch.float32)
        data.x = torch.cat([data.x, pe_tensor], dim=-1)
        
        return data
    
    def _build_edge_index(self, edges_df) -> torch.Tensor:
        """
        Build edge index tensor for undirected graph.
        
        The query returns deduplicated edges (source < target).
        This method adds both directions for undirected representation.
        
        Args:
            edges_df: DataFrame with source and target columns
            
        Returns:
            Edge index tensor of shape [2, num_edges * 2]
        """
        if len(edges_df) == 0 or 'source' not in edges_df.columns or 'target' not in edges_df.columns:
            # Return empty edge index if no edges or missing columns
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
    
    def get_node_id(self, idx: int) -> int:
        """
        Get the original Neo4j node ID for a given index.
        
        Args:
            idx: Sequential node index in the Data object
            
        Returns:
            Original Neo4j node ID
        """
        return self._idx_to_node_id[idx]
    
    def get_node_index(self, node_id: int) -> int:
        """
        Get the sequential index for a given Neo4j node ID.
        
        Args:
            node_id: Original Neo4j node ID
            
        Returns:
            Sequential node index in the Data object
        """
        return self._node_id_to_idx[node_id]
    
    def get_graph_stats(self, data: Data) -> Dict:
        """
        Compute statistics about the loaded graph.
        
        Args:
            data: PyG Data object
            
        Returns:
            Dictionary with graph statistics
        """
        num_nodes = data.num_nodes
        num_edges = data.num_edges // 2  # Undirected, counted twice
        
        # Class distribution
        unique, counts = torch.unique(data.y, return_counts=True)
        class_dist = {int(u): int(c) for u, c in zip(unique, counts)}
        
        # Graph density
        max_edges = num_nodes * (num_nodes - 1) / 2
        density = num_edges / max_edges if max_edges > 0 else 0
        
        return {
            'num_nodes': num_nodes,
            'num_edges': num_edges,
            'num_features': data.num_features,
            'class_distribution': class_dist,
            'density': density,
        }


def load_graph_from_neo4j(
    uri: str,
    user: str,
    password: str,
    database: str,
    use_filtering: bool = USE_GRAPH_FILTERING
) -> Tuple[Data, GraphDataLoader]:
    """
    Convenience function to load graph data from Neo4j.
    
    Args:
        uri: Neo4j connection URI (e.g., 'bolt://localhost:7687')
        user: Database username
        password: Database password
        database: Target database name
        use_filtering: Whether to use graph filtering (for large graphs)
        
    Returns:
        Tuple of (PyG Data object, GraphDataLoader instance)
        
    Example:
        >>> data, loader = load_graph_from_neo4j(
        ...     "bolt://localhost:7687", "neo4j", "password", "d4seed1",
        ...     use_filtering=True
        ... )
        >>> print(f"Loaded {data.num_nodes} nodes with {data.num_features} features")
    """
    client = Neo4jClient(uri, user, password)
    loader = GraphDataLoader(client, database, use_filtering=use_filtering)
    data = loader.load()
    
    return data, loader

