"""Test the recreated Neo4j GDS baseline."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'neo4j'))
from neo4jClient import Neo4jClient
from graph_label_prediction.python_model.binary_determined_loader import BinaryDeterminedLoader

import os
from dotenv import load_dotenv
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegressionCV
from sklearn.metrics import f1_score

load_dotenv(Path(__file__).parent / '.env')

print("=" * 70)
print("Testing Recreated Neo4j GDS Baseline")
print("=" * 70)

client = Neo4jClient(
    os.getenv('NEO4J_URI'),
    os.getenv('NEO4J_USER'),
    os.getenv('NEO4J_PASSWORD')
)

try:
    loader = BinaryDeterminedLoader(client, os.getenv('NEO4J_DATABASE'))
    
    # Load with parray_max=3 to get ~400 nodes
    print("\nLoading data with filtering (pArrayList ∈ [0, 3))...")
    X, y, df = loader.load(use_filtering=True, parray_min=0, parray_max=3)
    
    stats = loader.get_stats(y)
    print(f"\n✓ Data loaded!")
    print(f"  Samples: {len(X):,}")
    print(f"  Features: {X.shape[1]}")
    print(f"  Determined: {stats['determined']:,} ({stats['determined_pct']:.1f}%)")
    print(f"  Under-determined: {stats['undetermined']:,} ({stats['undetermined_pct']:.1f}%)")
    
    # Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=49, stratify=y
    )
    
    # Train
    penalties = [0.0625, 0.5, 1.0, 4.0]
    Cs = [1/p for p in penalties]
    n_folds = min(185, (y_train == 1).sum())
    
    print(f"\nTraining LogisticRegressionCV:")
    print(f"  CV folds: {n_folds}")
    print(f"  Penalties: {penalties}")
    
    clf = LogisticRegressionCV(
        Cs=Cs,
        cv=n_folds,
        scoring='f1_weighted',
        random_state=49,
        max_iter=100,
        penalty='l2',
        solver='lbfgs'
    )
    
    clf.fit(X_train, y_train)
    
    # Evaluate
    y_train_pred = clf.predict(X_train)
    y_test_pred = clf.predict(X_test)
    
    train_f1 = f1_score(y_train, y_train_pred, average='weighted')
    test_f1 = f1_score(y_test, y_test_pred, average='weighted')
    
    print(f"\n" + "=" * 70)
    print("Results:")
    print("=" * 70)
    print(f"Original (2021):  Train F1 = 0.8295, Test F1 = 0.7923")
    print(f"Recreated (2026): Train F1 = {train_f1:.4f}, Test F1 = {test_f1:.4f}")
    print(f"\nDifference: ΔTest F1 = {test_f1 - 0.7923:+.4f}")
    
    # Check if close
    if abs(test_f1 - 0.7923) < 0.05:
        print(f"\n✓ SUCCESS: Results match original (within 5%)")
    else:
        print(f"\n⚠️ Results differ from original")
        print(f"   Possible reasons:")
        print(f"   - Different data (different database/filtering)")
        print(f"   - Different class balance")
        print(f"   - Graph structure differences")

finally:
    client.close()
    print("\n" + "=" * 70)

