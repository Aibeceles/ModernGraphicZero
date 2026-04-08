"""
Predictor for Link Prediction with Neo4j Write-back.

This module handles:
- Loading trained models
- Generating predictions
- Writing predicted edges back to Neo4j
- Batch processing for efficiency
"""

import sys
from pathlib import Path
from typing import Dict, List, Tuple, Optional

import torch
from torch import nn

# Add parent directory to path to import neo4jClient
sys.path.insert(0, str(Path(__file__).parent.parent / 'neo4j'))
from neo4jClient import Neo4jClient

from .config import (
    WRITE_SAME_DENOMINATOR_EDGE_QUERY,
    WRITE_NEXT_INTEGER_EDGE_QUERY,
    BATCH_WRITE_INT_VALUE_QUERY,
    DEVICE,
)


class LinkPredictor:
    """
    Generate link predictions and write to Neo4j.
    
    This class handles inference from trained models and writes
    predicted edges back to the database.
    
    Example:
        >>> predictor = LinkPredictor(client, database="d4seed1")
        >>> predictor.predict_and_write(
        ...     task1_model, task2_model, data_t1, data_t2
        ... )
    """
    
    def __init__(
        self,
        client: Neo4jClient,
        database: str,
        device: str = DEVICE,
    ):
        """
        Initialize the predictor.
        
        Args:
            client: Neo4jClient instance
            database: Target database name
            device: Device for model inference
        """
        self.client = client
        self.database = database
        self.device = device
    
    def predict_and_write(
        self,
        task1_model: nn.Module,
        task2_model: nn.Module,
        data_task1,
        data_task2,
        write_int_values: bool = True,
        write_same_denom: bool = True,
        write_next_int: bool = True,
        batch_size: int = 1000,
        verbose: bool = True,
    ) -> Dict[str, int]:
        """
        Generate predictions and write to Neo4j.
        
        Args:
            task1_model: Trained Task 1 model
            task2_model: Trained Task 2 model
            data_task1: Task 1 data
            data_task2: Task 2 data
            write_int_values: Whether to write computed integer values
            write_same_denom: Whether to write SAME_DENOMINATOR edges
            write_next_int: Whether to write NEXT_INTEGER edges
            batch_size: Batch size for writing
            verbose: Whether to print progress
            
        Returns:
            Dictionary with counts of written items
        """
        results = {
            'int_values_written': 0,
            'same_denom_edges_written': 0,
            'next_int_edges_written': 0,
        }
        
        # Move models to device
        task1_model = task1_model.to(self.device)
        task2_model = task2_model.to(self.device)
        data_task1 = data_task1.to(self.device)
        data_task2 = data_task2.to(self.device)
        
        # Generate predictions
        if verbose:
            print("Generating predictions...")
        
        task1_model.eval()
        task2_model.eval()
        
        with torch.no_grad():
            # Task 1 predictions
            if hasattr(task1_model, 'predict_edges'):
                same_denom_edges = task1_model.predict_edges(
                    data_task1.x, data_task1.edge_index
                )
            else:
                # For contrastive model
                embeddings = task1_model(data_task1.x, data_task1.edge_index)
                same_denom_edges = task1_model.predict_edges_from_embeddings(
                    embeddings, similarity_threshold=0.8
                )
            
            # Task 2 predictions
            next_int_edges = task2_model.predict_edges(
                data_task2.x,
                data_task2.edge_index,
                data_task2.partition_ids,
            )
        
        if verbose:
            print(f"  Predicted {same_denom_edges.size(1)} SAME_DENOMINATOR edges")
            print(f"  Predicted {next_int_edges.size(1)} NEXT_INTEGER edges")
        
        # Write integer values
        if write_int_values:
            if verbose:
                print("\nWriting integer values to Neo4j...")
            
            count = self._write_int_values(
                data_task2.node_ids,
                data_task2.int_values,
                batch_size,
                verbose,
            )
            results['int_values_written'] = count
        
        # Write SAME_DENOMINATOR edges
        if write_same_denom:
            if verbose:
                print("\nWriting SAME_DENOMINATOR edges to Neo4j...")
            
            count = self._write_edges(
                same_denom_edges,
                data_task1.node_ids,
                'SAME_DENOMINATOR',
                batch_size,
                verbose,
            )
            results['same_denom_edges_written'] = count
        
        # Write NEXT_INTEGER edges
        if write_next_int:
            if verbose:
                print("\nWriting NEXT_INTEGER edges to Neo4j...")
            
            count = self._write_edges(
                next_int_edges,
                data_task2.node_ids,
                'NEXT_INTEGER',
                batch_size,
                verbose,
            )
            results['next_int_edges_written'] = count
        
        if verbose:
            print("\nWrite complete!")
            print(f"  Integer values: {results['int_values_written']}")
            print(f"  SAME_DENOMINATOR edges: {results['same_denom_edges_written']}")
            print(f"  NEXT_INTEGER edges: {results['next_int_edges_written']}")
        
        return results
    
    def _write_int_values(
        self,
        node_ids: List[str],
        int_values: torch.Tensor,
        batch_size: int,
        verbose: bool,
    ) -> int:
        """
        Write computed integer values to Neo4j nodes.
        
        Args:
            node_ids: List of Neo4j node element IDs
            int_values: Integer values tensor
            batch_size: Batch size for writing
            verbose: Whether to print progress
            
        Returns:
            Number of nodes updated
        """
        int_values_cpu = int_values.cpu().numpy()
        total_written = 0
        
        # Prepare batches
        nodes_data = []
        for node_id, int_val in zip(node_ids, int_values_cpu):
            nodes_data.append({
                'node_id': node_id,
                'int_value': int(int_val),
            })
        
        # Write in batches
        for i in range(0, len(nodes_data), batch_size):
            batch = nodes_data[i:i + batch_size]
            
            try:
                query = BATCH_WRITE_INT_VALUE_QUERY
                params = {'nodes': batch}
                self.client.run_query_no_return(query, self.database, params)
                total_written += len(batch)
                
                if verbose and (i // batch_size) % 10 == 0:
                    print(f"  Written {total_written}/{len(nodes_data)} integer values...")
            except Exception as e:
                print(f"  Warning: Failed to write batch at index {i}: {e}")
        
        return total_written
    
    def _write_edges(
        self,
        edges: torch.Tensor,
        node_ids: List[str],
        edge_type: str,
        batch_size: int,
        verbose: bool,
    ) -> int:
        """
        Write predicted edges to Neo4j.
        
        Args:
            edges: Edge tensor [2, num_edges]
            node_ids: List of Neo4j node element IDs
            edge_type: Type of edge ('SAME_DENOMINATOR' or 'NEXT_INTEGER')
            batch_size: Batch size for writing
            verbose: Whether to print progress
            
        Returns:
            Number of edges written
        """
        if edges.size(1) == 0:
            if verbose:
                print(f"  No {edge_type} edges to write")
            return 0
        
        # Convert to edge list with node IDs
        edge_list = []
        seen_edges = set()
        
        for i in range(edges.size(1)):
            src_idx = edges[0, i].item()
            dst_idx = edges[1, i].item()
            
            src_id = node_ids[src_idx]
            dst_id = node_ids[dst_idx]
            
            # For undirected edges, avoid duplicates
            if edge_type == 'SAME_DENOMINATOR':
                edge_key = tuple(sorted([src_id, dst_id]))
                if edge_key in seen_edges:
                    continue
                seen_edges.add(edge_key)
            
            edge_list.append({
                'source_id': src_id,
                'target_id': dst_id,
            })
        
        # Select query based on edge type
        if edge_type == 'SAME_DENOMINATOR':
            query = WRITE_SAME_DENOMINATOR_EDGE_QUERY
        elif edge_type == 'NEXT_INTEGER':
            query = WRITE_NEXT_INTEGER_EDGE_QUERY
        else:
            raise ValueError(f"Unknown edge type: {edge_type}")
        
        # Write in batches
        total_written = 0
        for i in range(0, len(edge_list), batch_size):
            batch = edge_list[i:i + batch_size]
            
            try:
                for edge in batch:
                    self.client.run_query_no_return(query, self.database, edge)
                    total_written += 1
                
                if verbose and (i // batch_size) % 10 == 0:
                    print(f"  Written {total_written}/{len(edge_list)} {edge_type} edges...")
            except Exception as e:
                print(f"  Warning: Failed to write edge batch at index {i}: {e}")
        
        return total_written
    
    def predict_only(
        self,
        task1_model: nn.Module,
        task2_model: nn.Module,
        data_task1,
        data_task2,
        verbose: bool = True,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Generate predictions without writing to Neo4j.
        
        Args:
            task1_model: Trained Task 1 model
            task2_model: Trained Task 2 model
            data_task1: Task 1 data
            data_task2: Task 2 data
            verbose: Whether to print progress
            
        Returns:
            Tuple of (same_denom_edges, next_int_edges)
        """
        # Move to device
        task1_model = task1_model.to(self.device)
        task2_model = task2_model.to(self.device)
        data_task1 = data_task1.to(self.device)
        data_task2 = data_task2.to(self.device)
        
        if verbose:
            print("Generating predictions...")
        
        task1_model.eval()
        task2_model.eval()
        
        with torch.no_grad():
            # Task 1 predictions
            if hasattr(task1_model, 'predict_edges'):
                same_denom_edges = task1_model.predict_edges(
                    data_task1.x, data_task1.edge_index
                )
            else:
                embeddings = task1_model(data_task1.x, data_task1.edge_index)
                same_denom_edges = task1_model.predict_edges_from_embeddings(
                    embeddings, similarity_threshold=0.8
                )
            
            # Task 2 predictions
            next_int_edges = task2_model.predict_edges(
                data_task2.x,
                data_task2.edge_index,
                data_task2.partition_ids,
            )
        
        if verbose:
            print(f"  Predicted {same_denom_edges.size(1)} SAME_DENOMINATOR edges")
            print(f"  Predicted {next_int_edges.size(1)} NEXT_INTEGER edges")
        
        return same_denom_edges, next_int_edges


def save_model(model: nn.Module, filepath: str):
    """
    Save a trained model to disk.
    
    Args:
        model: Model to save
        filepath: Path to save location
    """
    torch.save(model.state_dict(), filepath)
    print(f"Model saved to {filepath}")


def load_model(model_class, filepath: str, **model_kwargs) -> nn.Module:
    """
    Load a trained model from disk.
    
    Args:
        model_class: Model class to instantiate
        filepath: Path to saved model
        **model_kwargs: Arguments for model constructor
        
    Returns:
        Loaded model
    """
    model = model_class(**model_kwargs)
    model.load_state_dict(torch.load(filepath))
    model.eval()
    print(f"Model loaded from {filepath}")
    return model

