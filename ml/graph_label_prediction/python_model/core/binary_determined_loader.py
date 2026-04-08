"""
Data Loader for Binary Determined Classification (Neo4j GDS Baseline Recreation).

This module recreates the original GraphicLablePrediction.json (2021) data loading
with one-hot encoded wNum features for binary determined/undetermined classification.

Updated to support arbitrary graph depth via MAX_GRAPH_DEPTH config parameter.
"""

import sys
from pathlib import Path
from typing import Dict, Tuple

import numpy as np
import pandas as pd

# Add parent directory to path to import neo4jClient
sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'neo4j'))
from neo4jClient import Neo4jClient

# Import MAX_GRAPH_DEPTH from config for dynamic one-hot encoding
from .config import MAX_GRAPH_DEPTH


class BinaryDeterminedLoader:
    """
    Loads data for binary determined classification with Neo4j GDS-style features.
    
    Recreates the original 2021 pipeline with dynamic wNum depth support:
    - Binary target: determined (0 or 1)
    - Features: [wnum_0, wnum_1, ..., wnum_{MAX_GRAPH_DEPTH}, wNum, totalZero]
    - One-hot encoding of wNum (0 through MAX_GRAPH_DEPTH)
    - Total features: MAX_GRAPH_DEPTH + 3 (one-hot + wNum + totalZero)
    
    Example:
        >>> loader = BinaryDeterminedLoader(client, "d4seed1")
        >>> X, y = loader.load(use_filtering=True, parray_max=3)
        >>> print(f"Features: {X.shape}, Labels: {y.shape}")
    """
    
    def __init__(self, client: Neo4jClient, database: str):
        """
        Initialize the loader.
        
        Args:
            client: Neo4jClient instance
            database: Target database name
        """
        self.client = client
        self.database = database
    
    def load(
        self, 
        use_filtering: bool = True,
        parray_min: int = 0,
        parray_max: int = 5
    ) -> Tuple[np.ndarray, np.ndarray, pd.DataFrame]:
        """
        Load data with one-hot wNum features for binary classification.
        
        Args:
            use_filtering: Whether to filter by pArrayList constraint
            parray_min: Minimum pArrayList value (inclusive)
            parray_max: Maximum pArrayList value (exclusive)
            
        Returns:
            Tuple of (features, labels, dataframe)
            - features: [num_samples, MAX_GRAPH_DEPTH + 3] numpy array
            - labels: [num_samples] numpy array of 0/1
            - dataframe: Full data including node_ids
        """
        # Build query based on filtering
        if use_filtering:
            query = f"""
            MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
            WHERE d.determined IS NOT NULL
              AND all(x IN cb.pArrayList WHERE x >= {parray_min} AND x < {parray_max})
            RETURN elementId(d) as node_id,
                   d.determined as determined,
                   cb.wNum as wNum,
                   d.totalZero as totalZero
            ORDER BY node_id
            """
        else:
            query = """
            MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
            WHERE d.determined IS NOT NULL
            RETURN elementId(d) as node_id,
                   d.determined as determined,
                   cb.wNum as wNum,
                   d.totalZero as totalZero
            ORDER BY node_id
            """
        
        # Load data
        df = self.client.run_query(query, self.database)
        
        if len(df) == 0:
            raise ValueError("No nodes found with determined property")
        
        # Engineer one-hot wNum features (dynamic based on MAX_GRAPH_DEPTH)
        features_dict = self._engineer_features(df)
        
        # Combine into feature matrix: [wnum_0, wnum_1, ..., wnum_{MAX_GRAPH_DEPTH}, wNum, totalZero]
        one_hot_columns = [features_dict[f'wnum_{i}'] for i in range(MAX_GRAPH_DEPTH + 1)]
        X = np.column_stack([
            *one_hot_columns,
            features_dict['wNum'],
            features_dict['totalZero'],
        ]).astype(np.float32)
        
        # Labels (binary)
        y = df['determined'].values.astype(np.int64)
        
        return X, y, df
    
    def _engineer_features(self, df: pd.DataFrame) -> Dict[str, np.ndarray]:
        """
        Engineer one-hot wNum features with dynamic depth support.
        
        Args:
            df: DataFrame with wNum and totalZero columns
            
        Returns:
            Dictionary of feature arrays with keys:
            - wnum_0, wnum_1, ..., wnum_{MAX_GRAPH_DEPTH}: One-hot encoded wNum
            - wNum: Raw wNum value
            - totalZero: Total zero count
        """
        wNum = df['wNum'].fillna(0).values
        totalZero = df['totalZero'].fillna(0).values
        
        # Dynamic one-hot encode wNum [0..MAX_GRAPH_DEPTH]
        features = {
            f'wnum_{i}': (wNum == i).astype(np.float32)
            for i in range(MAX_GRAPH_DEPTH + 1)
        }
        
        # Add raw values
        features['wNum'] = wNum.astype(np.float32)
        features['totalZero'] = totalZero.astype(np.float32)
        
        return features
    
    def get_stats(self, y: np.ndarray) -> Dict:
        """
        Get statistics about the loaded data.
        
        Args:
            y: Label array
            
        Returns:
            Dictionary with data statistics
        """
        total = len(y)
        determined = (y == 1).sum()
        undetermined = (y == 0).sum()
        
        return {
            'total_samples': total,
            'determined': int(determined),
            'undetermined': int(undetermined),
            'determined_pct': float(determined / total * 100),
            'undetermined_pct': float(undetermined / total * 100),
            'imbalance_ratio': float(max(determined, undetermined) / min(determined, undetermined)),
        }


def load_binary_determined_data(
    uri: str,
    user: str,
    password: str,
    database: str,
    use_filtering: bool = True,
    parray_max: int = 5,
) -> Tuple[np.ndarray, np.ndarray, pd.DataFrame]:
    """
    Convenience function to load binary determined classification data.
    
    Args:
        uri: Neo4j connection URI
        user: Database username
        password: Database password
        database: Target database name
        use_filtering: Whether to filter by pArrayList
        parray_max: Maximum pArrayList value (exclusive)
        
    Returns:
        Tuple of (features, labels, dataframe)
        
    Example:
        >>> X, y, df = load_binary_determined_data(
        ...     "bolt://localhost:7687", "neo4j", "password", "d4seed1",
        ...     use_filtering=True, parray_max=3
        ... )
        >>> print(f"Loaded {len(X)} samples with {X.shape[1]} features")
    """
    client = Neo4jClient(uri, user, password)
    loader = BinaryDeterminedLoader(client, database)
    X, y, df = loader.load(use_filtering=use_filtering, parray_max=parray_max)
    client.close()
    
    return X, y, df

