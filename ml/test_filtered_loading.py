"""Test the filtered graph loading with spectral PE."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'neo4j'))
from neo4jClient import Neo4jClient
from graph_label_prediction.python_model.data_loader import GraphDataLoader
from graph_label_prediction import config

import os
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / '.env')

print("=" * 70)
print("Testing Filtered Graph Loading")
print("=" * 70)

client = Neo4jClient(
    os.getenv('NEO4J_URI'),
    os.getenv('NEO4J_USER'),
    os.getenv('NEO4J_PASSWORD')
)

try:
    # Test with filtering enabled
    print(f"\nConfiguration:")
    print(f"  Filtering enabled: {config.USE_GRAPH_FILTERING}")
    print(f"  pArrayList constraint: [{config.PARRAY_MIN}, {config.PARRAY_MAX})")
    print(f"  Max nodes for spectral PE: {config.MAX_NODES_FOR_SPECTRAL_PE:,}")
    
    loader = GraphDataLoader(client, os.getenv('NEO4J_DATABASE'), use_filtering=True)
    data = loader.load()
    stats = loader.get_graph_stats(data)
    
    print(f"\n✓ Filtered graph loaded successfully!")
    print(f"\nGraph Statistics:")
    print(f"  Nodes: {stats['num_nodes']:,}")
    print(f"  Edges: {stats['num_edges']:,}")
    print(f"  Features: {stats['num_features']}")
    print(f"  Class Distribution: {stats['class_distribution']}")
    
    # Check spectral PE
    has_spectral = data.x.shape[1] > 1 and data.x[:, 1:].abs().sum() > 0
    print(f"\nSpectral PE Status:")
    print(f"  Feature shape: {data.x.shape}")
    print(f"  Spectral PE computed: {has_spectral}")
    
    if has_spectral:
        print(f"  ✓ SUCCESS: Spectral PE working on filtered graph")
        print(f"  Sample spectral features (first 3 nodes):")
        for i in range(min(3, data.x.shape[0])):
            print(f"    Node {i}: wNum={data.x[i, 0]:.1f}, PE={data.x[i, 1:4].tolist()}")
    else:
        print(f"  ✗ Spectral PE not computed (graph still too large or computation failed)")
    
    # Memory estimate
    if stats['num_nodes'] <= config.MAX_NODES_FOR_SPECTRAL_PE:
        memory_gb = (stats['num_nodes'] ** 2 * 8) / (1024 ** 3)
        print(f"\nMemory estimate for spectral PE: {memory_gb:.2f} GB")
        print(f"  (Feasible: {'Yes' if memory_gb < 16 else 'No, needs >16GB RAM'})")

finally:
    client.close()
    print(f"\n✓ Test complete")

