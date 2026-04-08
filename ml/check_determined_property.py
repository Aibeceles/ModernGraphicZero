"""Check if determined property exists in the database."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'neo4j'))
from neo4jClient import Neo4jClient

import os
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / '.env')

print("Checking for 'determined' property in database...")
print("=" * 60)

client = Neo4jClient(
    os.getenv('NEO4J_URI'),
    os.getenv('NEO4J_USER'),
    os.getenv('NEO4J_PASSWORD')
)

try:
    # Check if determined property exists
    query = """
    MATCH (d:Dnode)
    WHERE d.determined IS NOT NULL
    RETURN count(d) AS has_determined,
           count(CASE WHEN d.determined = 1 THEN 1 END) AS determined_yes,
           count(CASE WHEN d.determined = 0 THEN 1 END) AS determined_no
    """
    
    result = client.run_query(query, os.getenv('NEO4J_DATABASE'))
    
    if len(result) > 0 and result['has_determined'].iloc[0] > 0:
        total = result['has_determined'].iloc[0]
        yes_count = result['determined_yes'].iloc[0]
        no_count = result['determined_no'].iloc[0]
        
        print(f"✓ 'determined' property exists!")
        print(f"\nDistribution:")
        print(f"  Total nodes with determined: {total:,}")
        print(f"  Determined (1): {yes_count:,} ({yes_count/total*100:.1f}%)")
        print(f"  Under-determined (0): {no_count:,} ({no_count/total*100:.1f}%)")
        print(f"\nClass balance: {max(yes_count, no_count) / min(yes_count, no_count):.1f}:1")
    else:
        print("✗ 'determined' property does NOT exist in database")
        print("\nWill need to compute it as: determined = (totalZero == wNum)")
        
        # Check if we can compute it
        check_query = """
        MATCH (d:Dnode)-[:CreatedBye]->(cb:CreatedBy)
        WHERE d.totalZero IS NOT NULL AND cb.wNum IS NOT NULL
        RETURN count(d) AS can_compute,
               count(CASE WHEN d.totalZero = cb.wNum THEN 1 END) AS would_be_determined,
               count(CASE WHEN d.totalZero < cb.wNum THEN 1 END) AS would_be_undetermined
        LIMIT 1
        """
        
        comp_result = client.run_query(check_query, os.getenv('NEO4J_DATABASE'))
        
        if len(comp_result) > 0:
            total = comp_result['can_compute'].iloc[0]
            det = comp_result['would_be_determined'].iloc[0]
            undet = comp_result['would_be_undetermined'].iloc[0]
            
            print(f"\n✓ Can compute determined from totalZero and wNum:")
            print(f"  Total computable: {total:,}")
            print(f"  Would be determined (totalZero == wNum): {det:,} ({det/total*100:.1f}%)")
            print(f"  Would be under-determined (totalZero < wNum): {undet:,} ({undet/total*100:.1f}%)")

finally:
    client.close()
    print("\n" + "=" * 60)

