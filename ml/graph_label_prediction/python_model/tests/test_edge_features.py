"""
Test suite for polynomial edge feature computation.

Tests:
1. Correct dimensionality (18 features per edge)
2. No NaN/Inf values
3. Reasonable value ranges for each feature category
4. Variance across edges (features should differentiate edges)
5. Mathematical consistency tests
"""

import torch
import numpy as np
import sys
from pathlib import Path

# Add core directory to path for imports
core_dir = Path(__file__).parent.parent / 'core'
if str(core_dir) not in sys.path:
    sys.path.insert(0, str(core_dir))

from edge_features import (
    compute_polynomial_edge_features,
    get_edge_feature_names,
    NUM_EDGE_FEATURES,
)


def create_test_data():
    """
    Create synthetic node features and edges for testing.
    
    Simulates a small polynomial difference tree:
    - Node 0: Quartic (degree 4)
    - Node 1: Cubic (degree 3) - child of 0
    - Node 2: Quadratic (degree 2) - child of 1
    - Node 3: Linear (degree 1) - child of 2
    - Node 4: Constant (degree 0) - child of 3
    """
    num_nodes = 5
    
    # Node features: [wNum, degree, coeff_0..4, magnitude, leading, constant, sparsity, mean_abs]
    # Format: each row is 12D
    x = torch.tensor([
        # Node 0: wNum=4, degree=4, P(x) = x^4 + 2x^2 + 1
        [4.0, 4.0, 1.0, 0.0, 2.0, 0.0, 1.0, 2.449, 1.0, 1.0, 0.4, 0.8],
        # Node 1: wNum=3, degree=3, P(x) = x^3 - x + 3
        [3.0, 3.0, 3.0, -1.0, 0.0, 1.0, 0.0, 3.317, 1.0, 3.0, 0.25, 1.25],
        # Node 2: wNum=2, degree=2, P(x) = 2x^2 - 5x + 3
        [2.0, 2.0, 3.0, -5.0, 2.0, 0.0, 0.0, 6.164, 2.0, 3.0, 0.0, 3.333],
        # Node 3: wNum=1, degree=1, P(x) = 4x - 2
        [1.0, 1.0, -2.0, 4.0, 0.0, 0.0, 0.0, 4.472, 4.0, 2.0, 0.0, 3.0],
        # Node 4: wNum=0, degree=0, P(x) = 7 (constant)
        [0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 7.0, 7.0, 7.0, 0.0, 7.0],
    ], dtype=torch.float32)
    
    # Edges: tree structure (parent→child and child→parent for undirected)
    edge_index = torch.tensor([
        [0, 1, 1, 2, 2, 3, 3, 4],  # source
        [1, 0, 2, 1, 3, 2, 4, 3],  # target
    ], dtype=torch.long)
    
    return x, edge_index


def test_edge_feature_dimension():
    """Test that edge features have correct dimensionality."""
    print("\n=== Test 1: Edge Feature Dimension ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    num_edges = edge_index.shape[1]
    print(f"Number of edges: {num_edges}")
    print(f"Edge feature shape: {edge_feat.shape}")
    print(f"Expected shape: [{num_edges}, {NUM_EDGE_FEATURES}]")
    
    assert edge_feat.shape == (num_edges, NUM_EDGE_FEATURES), \
        f"Shape mismatch: {edge_feat.shape} vs expected ({num_edges}, {NUM_EDGE_FEATURES})"
    
    print("✓ PASSED")
    return edge_feat


def test_no_nan_inf():
    """Test that edge features contain no NaN or Inf values."""
    print("\n=== Test 2: No NaN/Inf Values ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    nan_count = torch.isnan(edge_feat).sum().item()
    inf_count = torch.isinf(edge_feat).sum().item()
    
    print(f"NaN count: {nan_count}")
    print(f"Inf count: {inf_count}")
    
    assert nan_count == 0, f"Found {nan_count} NaN values in edge features"
    assert inf_count == 0, f"Found {inf_count} Inf values in edge features"
    
    print("✓ PASSED")


def test_value_ranges():
    """Test that edge features have reasonable value ranges."""
    print("\n=== Test 3: Value Ranges ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    feature_names = get_edge_feature_names()
    
    print(f"\nFeature ranges:")
    for i, name in enumerate(feature_names):
        feat_col = edge_feat[:, i]
        min_val = feat_col.min().item()
        max_val = feat_col.max().item()
        mean_val = feat_col.mean().item()
        std_val = feat_col.std().item()
        print(f"  {i:2d}. {name:25s}: min={min_val:7.3f}, max={max_val:7.3f}, mean={mean_val:7.3f}, std={std_val:7.3f}")
    
    # Check that most features are in reasonable ranges
    # (normalized features should be roughly in [-2, 2])
    max_abs = edge_feat.abs().max().item()
    assert max_abs < 10.0, f"Feature values too extreme: max_abs = {max_abs}"
    
    print("✓ PASSED")


def test_edge_variance():
    """Test that edge features have variance across edges (not uniform)."""
    print("\n=== Test 4: Edge Variance (Non-Uniform Features) ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    feature_names = get_edge_feature_names()
    
    # Check variance for each feature
    low_variance_features = []
    for i, name in enumerate(feature_names):
        variance = edge_feat[:, i].var().item()
        if variance < 1e-6:
            low_variance_features.append((name, variance))
    
    print(f"Features with low variance:")
    if low_variance_features:
        for name, var in low_variance_features:
            print(f"  - {name}: variance = {var:.6f}")
    else:
        print("  (none - all features have variance)")
    
    # Total variance across all features
    total_variance = edge_feat.var().item()
    print(f"\nTotal feature variance: {total_variance:.4f}")
    
    # At least some features should have variance
    high_variance_count = sum(1 for i in range(NUM_EDGE_FEATURES) 
                              if edge_feat[:, i].var().item() > 1e-4)
    print(f"Features with meaningful variance (>1e-4): {high_variance_count}/{NUM_EDGE_FEATURES}")
    
    assert high_variance_count >= 10, \
        f"Too few features have variance: {high_variance_count}"
    
    print("✓ PASSED")


def test_direction_consistency():
    """Test that direction feature correctly identifies parent→child edges."""
    print("\n=== Test 5: Direction Consistency ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    # Direction is feature index 2
    direction = edge_feat[:, 2]
    
    print("Edge directions:")
    for i in range(edge_index.shape[1]):
        src, tgt = edge_index[0, i].item(), edge_index[1, i].item()
        wnum_src = x[src, 0].item()
        wnum_tgt = x[tgt, 0].item()
        dir_val = direction[i].item()
        expected = 1.0 if wnum_src > wnum_tgt else 0.0
        print(f"  Edge {src}→{tgt}: wNum {wnum_src}→{wnum_tgt}, direction={dir_val:.1f} (expected {expected:.1f})")
        
        assert abs(dir_val - expected) < 0.01, \
            f"Direction mismatch for edge {src}→{tgt}: got {dir_val}, expected {expected}"
    
    print("✓ PASSED")


def test_degree_consistency():
    """Test that degree_consistency feature is high for proper differentiation edges."""
    print("\n=== Test 6: Degree Consistency ===")
    
    x, edge_index = create_test_data()
    edge_feat = compute_polynomial_edge_features(x, edge_index)
    
    # degree_consistency is feature index 6
    degree_consistency = edge_feat[:, 6]
    
    print("Degree consistency for each edge:")
    for i in range(edge_index.shape[1]):
        src, tgt = edge_index[0, i].item(), edge_index[1, i].item()
        degree_src = x[src, 1].item()
        degree_tgt = x[tgt, 1].item()
        degree_diff = degree_src - degree_tgt
        consistency = degree_consistency[i].item()
        
        # For parent→child (degree_diff ≈ 1), consistency should be high
        expected_high = abs(degree_diff - 1.0) < 0.1
        
        print(f"  Edge {src}→{tgt}: degrees {degree_src}→{degree_tgt}, diff={degree_diff:.1f}, consistency={consistency:.3f}")
        
        if expected_high:
            assert consistency > 0.5, \
                f"Expected high consistency for degree_diff={degree_diff}, got {consistency}"
    
    print("✓ PASSED")


def test_empty_edges():
    """Test handling of empty edge index."""
    print("\n=== Test 7: Empty Edges ===")
    
    x, _ = create_test_data()
    empty_edge_index = torch.zeros((2, 0), dtype=torch.long)
    
    edge_feat = compute_polynomial_edge_features(x, empty_edge_index)
    
    print(f"Empty edge feature shape: {edge_feat.shape}")
    assert edge_feat.shape == (0, NUM_EDGE_FEATURES), \
        f"Expected shape (0, {NUM_EDGE_FEATURES}), got {edge_feat.shape}"
    
    print("✓ PASSED")


def test_feature_names():
    """Test that feature names list matches expected count."""
    print("\n=== Test 8: Feature Names ===")
    
    names = get_edge_feature_names()
    
    print(f"Number of feature names: {len(names)}")
    print(f"Expected: {NUM_EDGE_FEATURES}")
    
    assert len(names) == NUM_EDGE_FEATURES, \
        f"Name count mismatch: {len(names)} vs {NUM_EDGE_FEATURES}"
    
    print("\nFeature names:")
    for i, name in enumerate(names):
        print(f"  {i:2d}. {name}")
    
    print("✓ PASSED")


def run_all_tests():
    """Run all edge feature tests."""
    print("=" * 70)
    print("POLYNOMIAL EDGE FEATURE TEST SUITE")
    print("=" * 70)
    
    try:
        edge_feat = test_edge_feature_dimension()
        test_no_nan_inf()
        test_value_ranges()
        test_edge_variance()
        test_direction_consistency()
        test_degree_consistency()
        test_empty_edges()
        test_feature_names()
        
        print("\n" + "=" * 70)
        print("✓ ALL TESTS PASSED")
        print("=" * 70)
        print("\nEdge feature implementation verified:")
        print("  ✓ Correct 18D dimensionality")
        print("  ✓ No NaN/Inf values")
        print("  ✓ Reasonable value ranges")
        print("  ✓ Non-uniform features (variance across edges)")
        print("  ✓ Direction feature correctly identifies parent→child")
        print("  ✓ Degree consistency captures differentiation edges")
        print("  ✓ Empty edge handling")
        print("  ✓ Feature names match count")
        print("\nReady for integration with DepthAwareGAT and MultiTaskRootClassifier!")
        
        return True
        
    except Exception as e:
        print("\n" + "=" * 70)
        print("✗ TEST FAILED")
        print("=" * 70)
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)

