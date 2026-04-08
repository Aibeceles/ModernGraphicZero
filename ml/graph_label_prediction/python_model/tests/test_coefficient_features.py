"""
Integration test for coefficient feature extraction.

Tests the vmResult parsing and feature extraction pipeline to ensure:
1. Correct parsing of descending power order
2. Proper coefficient padding
3. Accurate statistical feature computation
4. Integration with data loader
"""

import numpy as np
import sys
from pathlib import Path

# Add core directory to path for imports
core_dir = Path(__file__).parent.parent / 'core'
if str(core_dir) not in sys.path:
    sys.path.insert(0, str(core_dir))

from coefficient_features import (
    parse_vmresult_coefficients,
    compute_coefficient_statistics,
    extract_coefficient_features
)


def test_parse_quadratic():
    """Test parsing of quadratic polynomial: 2x² - 5x + 3"""
    print("\n=== Test 1: Quadratic Polynomial ===")
    # Database format: [0, c_2, c_1, c_0] with leading zero padding
    vm_result = "[0.0, 2.0, -5.0, 3.0]"  # Padded descending: 2x² - 5x + 3
    
    padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree=4)
    
    print(f"Input: {vm_result}")
    print(f"Expected (ascending): [3.0, -5.0, 2.0, 0.0, 0.0]")
    print(f"Got:                  {padded_coeffs}")
    print(f"Degree: {degree}")
    
    # Verify
    expected = np.array([3.0, -5.0, 2.0, 0.0, 0.0], dtype=np.float32)
    assert np.allclose(padded_coeffs, expected), f"Mismatch: {padded_coeffs} vs {expected}"
    assert degree == 2.0, f"Degree mismatch: {degree} vs 2.0"
    print("✓ PASSED")
    
    return padded_coeffs, degree


def test_parse_constant():
    """Test parsing of constant polynomial: 5"""
    print("\n=== Test 2: Constant Polynomial ===")
    # Database format: [c_0] - NO padding for constants
    vm_result = "[5.0]"  # Just constant
    
    padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree=4)
    
    print(f"Input: {vm_result}")
    print(f"Expected (ascending): [5.0, 0.0, 0.0, 0.0, 0.0]")
    print(f"Got:                  {padded_coeffs}")
    print(f"Degree: {degree}")
    
    expected = np.array([5.0, 0.0, 0.0, 0.0, 0.0], dtype=np.float32)
    assert np.allclose(padded_coeffs, expected), f"Mismatch: {padded_coeffs} vs {expected}"
    assert degree == 0.0, f"Degree mismatch: {degree} vs 0.0"
    print("✓ PASSED")
    
    return padded_coeffs, degree


def test_parse_sparse_quartic():
    """Test parsing of sparse quartic: x⁴ + 2 (no x³, x², x terms)"""
    print("\n=== Test 3: Sparse Quartic ===")
    # Database format: [0, c_4, c_3, c_2, c_1, c_0] with leading zero padding
    vm_result = "[0.0, 1.0, 0.0, 0.0, 0.0, 2.0]"  # x⁴ + 2
    
    padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree=4)
    
    print(f"Input: {vm_result}")
    print(f"Expected (ascending): [2.0, 0.0, 0.0, 0.0, 1.0]")
    print(f"Got:                  {padded_coeffs}")
    print(f"Degree: {degree}")
    
    expected = np.array([2.0, 0.0, 0.0, 0.0, 1.0], dtype=np.float32)
    assert np.allclose(padded_coeffs, expected), f"Mismatch: {padded_coeffs} vs {expected}"
    assert degree == 4.0, f"Degree mismatch: {degree} vs 4.0"
    print("✓ PASSED")
    
    return padded_coeffs, degree


def test_compute_statistics():
    """Test statistical feature computation"""
    print("\n=== Test 4: Statistical Features ===")
    
    # Use quadratic: 2x² - 5x + 3 → [3, -5, 2, 0, 0]
    coeffs = np.array([3.0, -5.0, 2.0, 0.0, 0.0], dtype=np.float32)
    degree = 2.0
    
    stats = compute_coefficient_statistics(coeffs, degree)
    
    print(f"Coefficients: {coeffs}")
    print(f"Degree: {degree}")
    print(f"\nStatistics:")
    print(f"  1. Magnitude (L2 norm):      {stats[0]:.4f}")
    print(f"     Expected: sqrt(3² + 5² + 2²) = {np.sqrt(3**2 + 5**2 + 2**2):.4f}")
    print(f"  2. Leading coeff abs:        {stats[1]:.4f}")
    print(f"     Expected: |2.0| = 2.0")
    print(f"  3. Constant term abs:        {stats[2]:.4f}")
    print(f"     Expected: |3.0| = 3.0")
    print(f"  4. Sparsity:                 {stats[3]:.4f}")
    print(f"     Expected: 0/3 = 0.0")
    print(f"  5. Mean abs:                 {stats[4]:.4f}")
    print(f"     Expected: (|3| + |-5| + |2|) / 3 = {(3 + 5 + 2) / 3:.4f}")
    
    # Verify
    expected_magnitude = np.sqrt(3**2 + 5**2 + 2**2)
    assert np.isclose(stats[0], expected_magnitude), f"Magnitude mismatch: {stats[0]} vs {expected_magnitude}"
    assert np.isclose(stats[1], 2.0), f"Leading coeff mismatch: {stats[1]} vs 2.0"
    assert np.isclose(stats[2], 3.0), f"Constant term mismatch: {stats[2]} vs 3.0"
    assert np.isclose(stats[3], 0.0), f"Sparsity mismatch: {stats[3]} vs 0.0"
    assert np.isclose(stats[4], (3 + 5 + 2) / 3), f"Mean abs mismatch: {stats[4]} vs {(3 + 5 + 2) / 3}"
    print("✓ PASSED")
    
    return stats


def test_extract_all_features():
    """Test combined extraction of all features"""
    print("\n=== Test 5: Combined Feature Extraction ===")
    # Database format with padding
    vm_result = "[0.0, 2.0, -5.0, 3.0]"
    
    coeffs, degree, stats = extract_coefficient_features(vm_result, max_degree=4)
    
    print(f"Input: {vm_result}")
    print(f"\nExtracted features:")
    print(f"  Degree: {degree}")
    print(f"  Coefficients: {coeffs}")
    print(f"  Statistics: {stats}")
    
    # Verify dimensions
    assert coeffs.shape == (5,), f"Coeffs shape mismatch: {coeffs.shape}"
    assert isinstance(degree, float), f"Degree type mismatch: {type(degree)}"
    assert stats.shape == (5,), f"Stats shape mismatch: {stats.shape}"
    
    # Total feature count: 1 degree + 5 coeffs + 5 stats = 11 (plus wNum from elsewhere = 12 total)
    print(f"\nFeature vector composition (excluding wNum):")
    print(f"  degree:       1 feature")
    print(f"  coeffs:       5 features")
    print(f"  stats:        5 features")
    print(f"  TOTAL:        11 features (+ 1 wNum = 12)")
    print("✓ PASSED")


def test_edge_cases():
    """Test edge cases and error handling"""
    print("\n=== Test 6: Edge Cases ===")
    
    # Test None
    coeffs, degree = parse_vmresult_coefficients(None, max_degree=4)
    assert np.all(coeffs == 0), "None should return zeros"
    assert degree == 0.0, "None should return degree 0"
    print("✓ None handled correctly")
    
    # Test empty list
    coeffs, degree = parse_vmresult_coefficients([], max_degree=4)
    assert np.all(coeffs == 0), "Empty list should return zeros"
    assert degree == 0.0, "Empty list should return degree 0"
    print("✓ Empty list handled correctly")
    
    # Test invalid string
    coeffs, degree = parse_vmresult_coefficients("invalid", max_degree=4)
    assert np.all(coeffs == 0), "Invalid string should return zeros"
    assert degree == 0.0, "Invalid string should return degree 0"
    print("✓ Invalid string handled correctly")
    
    # Test list input (not string) with padding
    coeffs, degree = parse_vmresult_coefficients([0.0, 2.0, -5.0, 3.0], max_degree=4)
    expected = np.array([3.0, -5.0, 2.0, 0.0, 0.0], dtype=np.float32)
    assert np.allclose(coeffs, expected), "List input should work"
    print("✓ List input handled correctly")
    
    # Test quartic at wNum=4 (6 elements: padding + 5 coefficients)
    # Quartic: x^4 + 2x^3 + 3x^2 + 4x + 5
    quartic = "[0.0, 1.0, 2.0, 3.0, 4.0, 5.0]"  # [padding, c_4, c_3, c_2, c_1, c_0]
    coeffs, degree = parse_vmresult_coefficients(quartic, max_degree=4)
    # After stripping padding and reversing: [5, 4, 3, 2, 1]
    expected = np.array([5.0, 4.0, 3.0, 2.0, 1.0], dtype=np.float32)
    assert np.allclose(coeffs, expected), f"Quartic parsing failed: {coeffs}"
    assert degree == 4.0, f"Degree should be 4.0, got {degree}"
    print("✓ Quartic polynomial (6 elements) handled correctly")
    
    print("✓ ALL EDGE CASES PASSED")


def run_all_tests():
    """Run all tests"""
    print("="*70)
    print("VMRESULT COEFFICIENT FEATURE EXTRACTION TEST SUITE")
    print("="*70)
    
    try:
        test_parse_quadratic()
        test_parse_constant()
        test_parse_sparse_quartic()
        test_compute_statistics()
        test_extract_all_features()
        test_edge_cases()
        
        print("\n" + "="*70)
        print("✓ ALL TESTS PASSED")
        print("="*70)
        print("\nImplementation verified:")
        print("  ✓ Correct parsing of descending power order")
        print("  ✓ Proper coefficient padding to max_degree + 1")
        print("  ✓ Accurate statistical feature computation")
        print("  ✓ Robust error handling for edge cases")
        print("\nReady for integration with GraphDataLoader!")
        
        return True
        
    except Exception as e:
        print("\n" + "="*70)
        print("✗ TEST FAILED")
        print("="*70)
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)

