"""
Coefficient Feature Extraction for vmResult Polynomial Coefficients.

This module provides functionality to parse vmResult strings (polynomial coefficient
arrays) and extract both raw coefficient features and derived statistical features
for use in graph neural network models.

Key Functions:
- parse_vmresult_coefficients: Parse and pad coefficient arrays
- compute_coefficient_statistics: Compute statistical features from coefficients
"""

import numpy as np
from typing import Tuple

# Import MAX_POLYNOMIAL_DEGREE from config for default parameter
# Using late import pattern to avoid circular dependencies
def _get_max_polynomial_degree():
    """Lazy import to avoid circular dependency."""
    from .config import MAX_POLYNOMIAL_DEGREE
    return MAX_POLYNOMIAL_DEGREE


def parse_vmresult_coefficients(
    vm_result, 
    max_degree: int = None,
    tol: float = 1e-9
) -> Tuple[np.ndarray, float]:
    """
    Parse vmResult string to padded coefficient array.
    
    Database Format:
    - For degree > 0: "[0, c_n, c_{n-1}, ..., c_1, c_0]" with leading zero padding
    - For degree = 0: "[c_0]" (constant, no padding)
    
    Examples:
        "[0.0, 2.0, -5.0, 3.0]" represents 2x² - 5x + 3 (quadratic with padding)
        - Index 0: 0.0 (padding)
        - Index 1: 2.0 (coefficient of x²)
        - Index 2: -5.0 (coefficient of x)
        - Index 3: 3.0 (constant term)
        
        "[5.0]" represents 5 (constant, no padding)
    
    This function:
    1. Parses the string/list
    2. Reverses to ASCENDING order [c_0, c_1, ..., c_n]
    3. Pads to max_degree + 1 dimensions
    4. Computes actual polynomial degree
    
    Args:
        vm_result: vmResult string "[c_n, ..., c_0]" or list of coefficients
        max_degree: Maximum polynomial degree for padding (default: from config.MAX_POLYNOMIAL_DEGREE)
        tol: Tolerance for zero detection (default: 1e-9)
        
    Returns:
        Tuple of:
        - padded_coeffs: [coeff_0, coeff_1, ..., coeff_max_degree] in ascending order
        - degree: Actual polynomial degree (highest non-zero power)
    
    Examples:
        >>> parse_vmresult_coefficients("[0.0, 2.0, -5.0, 3.0]", max_degree=5)
        (array([3.0, -5.0, 2.0, 0.0, 0.0, 0.0], dtype=float32), 2.0)
        
        >>> parse_vmresult_coefficients("[5.0]", max_degree=5)  # constant
        (array([5.0, 0.0, 0.0, 0.0, 0.0, 0.0], dtype=float32), 0.0)
    """
    # Use config default if not specified
    if max_degree is None:
        max_degree = _get_max_polynomial_degree()
    
    # Handle missing/invalid vmResult
    if vm_result is None or (isinstance(vm_result, float) and np.isnan(vm_result)):
        return np.zeros(max_degree + 1, dtype=np.float32), 0.0
    
    # Convert string to list if needed
    if isinstance(vm_result, str):
        try:
            import ast
            vm_result = ast.literal_eval(vm_result)
        except (ValueError, SyntaxError):
            return np.zeros(max_degree + 1, dtype=np.float32), 0.0
    
    if not isinstance(vm_result, (list, tuple)) or len(vm_result) == 0:
        return np.zeros(max_degree + 1, dtype=np.float32), 0.0
    
    # Convert to numpy array
    coeffs = np.array([float(c) for c in vm_result], dtype=np.float32)
    
    # Database format: For degree > 0: [0, c_n, c_{n-1}, ..., c_1, c_0] (with leading zero padding)
    #                  For degree = 0: [c_0] (no padding)
    # Strip leading zero if present (padding for non-constant polynomials)
    if len(coeffs) > 1 and abs(coeffs[0]) < tol:
        coeffs_desc = coeffs[1:]  # Strip padding, now [c_n, ..., c_1, c_0] descending
    else:
        coeffs_desc = coeffs  # Constant polynomial [c_0]
    
    # Reverse to ascending order: [c_0, c_1, ..., c_n]
    coeffs_asc = coeffs_desc[::-1]
    
    # Find actual degree (last non-zero coefficient)
    degree = 0.0
    for i in range(len(coeffs_asc) - 1, -1, -1):
        if abs(coeffs_asc[i]) > tol:
            degree = float(i)
            break
    
    # Pad or truncate to max_degree + 1
    padded = np.zeros(max_degree + 1, dtype=np.float32)
    copy_len = min(len(coeffs_asc), max_degree + 1)
    padded[:copy_len] = coeffs_asc[:copy_len]
    
    # If polynomial degree exceeds max_degree, clamp it
    if degree > max_degree:
        degree = float(max_degree)
    
    return padded, degree


def compute_coefficient_statistics(
    padded_coeffs: np.ndarray,
    degree: float,
    tol: float = 1e-9
) -> np.ndarray:
    """
    Compute statistical features from coefficient vector.
    
    Extracts 5 statistical features that capture polynomial properties:
    1. Magnitude (L2 norm): Overall scale of polynomial
    2. Leading coefficient (absolute): Magnitude of highest-degree term
    3. Constant term (absolute): Magnitude of x^0 term
    4. Sparsity: Fraction of zero coefficients (up to actual degree)
    5. Mean absolute value: Average magnitude of non-padded coefficients
    
    These statistics provide scale-invariant and structural information about
    the polynomial that complements the raw coefficient values.
    
    Args:
        padded_coeffs: Padded coefficients [coeff_0, ..., coeff_max_degree] (ascending)
        degree: Actual polynomial degree
        tol: Tolerance for zero detection (default: 1e-9)
        
    Returns:
        stats: [magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs]
        
    Example:
        >>> coeffs = np.array([3.0, -5.0, 2.0, 0.0, 0.0])  # 2x² - 5x + 3
        >>> degree = 2.0
        >>> compute_coefficient_statistics(coeffs, degree)
        array([6.164..., 2.0, 3.0, 0.0, 3.333...], dtype=float32)
        # magnitude = sqrt(3² + 5² + 2²) ≈ 6.164
        # leading_coeff_abs = |2.0| = 2.0
        # constant_term_abs = |3.0| = 3.0
        # sparsity = 0/3 = 0.0 (no zeros in [3, -5, 2])
        # mean_abs = (|3| + |-5| + |2|) / 3 ≈ 3.333
    """
    # 1. L2 norm (magnitude)
    magnitude = np.linalg.norm(padded_coeffs)
    
    # 2. Leading coefficient (highest non-zero power)
    degree_int = int(degree)
    leading_coeff_abs = abs(padded_coeffs[degree_int]) if degree_int < len(padded_coeffs) else 0.0
    
    # 3. Constant term
    constant_term_abs = abs(padded_coeffs[0])
    
    # 4. Sparsity (fraction of zero coefficients up to degree)
    relevant_coeffs = padded_coeffs[:degree_int + 1] if degree_int > 0 else padded_coeffs[:1]
    num_zeros = np.sum(np.abs(relevant_coeffs) <= tol)
    sparsity = num_zeros / len(relevant_coeffs) if len(relevant_coeffs) > 0 else 1.0
    
    # 5. Mean absolute value
    mean_abs = np.mean(np.abs(relevant_coeffs)) if len(relevant_coeffs) > 0 else 0.0
    
    return np.array([magnitude, leading_coeff_abs, constant_term_abs, sparsity, mean_abs], 
                    dtype=np.float32)


def extract_coefficient_features(
    vm_result,
    max_degree: int = None,
    tol: float = 1e-9
) -> Tuple[np.ndarray, float, np.ndarray]:
    """
    Convenience function to extract all coefficient features at once.
    
    Combines parse_vmresult_coefficients and compute_coefficient_statistics
    for easy integration into data loading pipelines.
    
    Args:
        vm_result: vmResult string "[c_n, ..., c_0]" or list
        max_degree: Maximum polynomial degree for padding (default: from config.MAX_POLYNOMIAL_DEGREE)
        tol: Tolerance for zero detection
        
    Returns:
        Tuple of:
        - padded_coeffs: [max_degree + 1] array of coefficients (ascending order)
        - degree: Actual polynomial degree (scalar)
        - stats: [5] array of statistical features
        
    Example:
        >>> coeffs, degree, stats = extract_coefficient_features("[0.0, 2.0, -5.0, 3.0]")
        >>> coeffs
        array([3.0, -5.0, 2.0, 0.0, 0.0, 0.0], dtype=float32)  # Length = MAX_POLYNOMIAL_DEGREE + 1
        >>> degree
        2.0
        >>> stats
        array([6.164..., 2.0, 3.0, 0.0, 3.333...], dtype=float32)
    """
    # Use config default if not specified
    if max_degree is None:
        max_degree = _get_max_polynomial_degree()
    
    padded_coeffs, degree = parse_vmresult_coefficients(vm_result, max_degree, tol)
    stats = compute_coefficient_statistics(padded_coeffs, degree, tol)
    return padded_coeffs, degree, stats

