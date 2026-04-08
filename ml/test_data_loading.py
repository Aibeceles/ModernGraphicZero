"""Test the updated data loader."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'neo4j'))
from neo4jClient import Neo4jClient
from graph_label_prediction.python_model.data_loader import GraphDataLoader

import os
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / '.env')

client = Neo4jClient(
    os.getenv('NEO4J_URI'),
    os.getenv('NEO4J_USER'),
    os.getenv('NEO4J_PASSWORD')
)

try:
    loader = GraphDataLoader(client, os.getenv('NEO4J_DATABASE'))
    data = loader.load()
    stats = loader.get_graph_stats(data)
    
    print("✓ Data loading successful!")
    print(f"\nGraph Statistics:")
    print(f"  Nodes: {stats['num_nodes']}")
    print(f"  Edges: {stats['num_edges']}")
    print(f"  Features: {stats['num_features']}")
    print(f"  Class Distribution: {stats['class_distribution']}")
    
    print(f"\nFeature matrix shape: {data.x.shape}")
    print(f"Labels shape: {data.y.shape}")
    print(f"\nSample features (first 5 nodes):")
    print(f"  [wNum, totalZero]")
    for i in range(min(5, data.x.shape[0])):
        print(f"  Node {i}: {data.x[i].tolist()} -> label={data.y[i].item()}")

finally:
    client.close()

