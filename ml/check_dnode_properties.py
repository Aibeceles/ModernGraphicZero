"""Check what properties actually exist on Dnode nodes."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'neo4j'))
from neo4jClient import Neo4jClient

import os
from dotenv import load_dotenv

# Load environment
load_dotenv(Path(__file__).parent / '.env')

NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD")
NEO4J_DATABASE = os.getenv("NEO4J_DATABASE", "neo4j")

print(f"Connecting to: {NEO4J_DATABASE}")

try:
    client = Neo4jClient(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
    
    # Get sample node properties
    query = """
    MATCH (d:Dnode)
    WITH d LIMIT 1
    RETURN properties(d) AS props
    """
    
    result = client.run_query(query, NEO4J_DATABASE)
    
    if len(result) > 0:
        props = result['props'].iloc[0]
        print("\nProperties found on Dnode:")
        print("=" * 60)
        for key in sorted(props.keys()):
            print(f"  {key}: {type(props[key]).__name__} = {props[key]}")
        print("=" * 60)
        
        # Check if spec properties exist
        expected = ['zero', 'one', 'two', 'three', 'four', 'wNum', 'totalZero', 'determined']
        print("\nExpected properties from specification:")
        for prop in expected:
            status = "✓" if prop in props else "✗ MISSING"
            print(f"  {status} {prop}")
    else:
        print("No Dnode nodes found in database!")
    
    client.close()
    
except Exception as e:
    print(f"Error: {e}")

