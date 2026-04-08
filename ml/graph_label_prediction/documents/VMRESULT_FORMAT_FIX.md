# vmResult Format Correction

## Issue

Initial implementation crashed with:
```
ValueError: could not broadcast input array from shape (6,) into shape (5,)
```

## Root Cause

Misunderstood the database vmResult format. The documentation in `03_implementation.md` describes the **logical polynomial format**, but the **database storage format** includes a leading zero padding.

## Correct Database Format

### Non-Constant Polynomials (degree > 0)
```
Format: [0, c_n, c_{n-1}, ..., c_1, c_0]
Length: degree + 2 (1 padding + degree + 1 coefficients)
```

Examples:
- Quartic (wNum=4, degree=4): `[0, c_4, c_3, c_2, c_1, c_0]` → 6 elements
- Cubic (wNum=3, degree=3): `[0, c_3, c_2, c_1, c_0]` → 5 elements
- Quadratic (wNum=2, degree=2): `[0, c_2, c_1, c_0]` → 4 elements
- Linear (wNum=1, degree=1): `[0, c_1, c_0]` → 3 elements

### Constant Polynomials (degree = 0)
```
Format: [c_0]
Length: 1 (no padding)
```

Example:
- Constant (wNum=0, degree=0): `[c_0]` → 1 element

## Fix Applied

Updated `parse_vmresult_coefficients()` in `coefficient_features.py`:

```python
# Strip leading zero if present (padding for non-constant polynomials)
if len(coeffs) > 1 and abs(coeffs[0]) < tol:
    coeffs_desc = coeffs[1:]  # Strip padding, now [c_n, ..., c_1, c_0] descending
else:
    coeffs_desc = coeffs  # Constant polynomial [c_0]

# Reverse to ascending order: [c_0, c_1, ..., c_n]
coeffs_asc = coeffs_desc[::-1]

# Pad or truncate to max_degree + 1
padded = np.zeros(max_degree + 1, dtype=np.float32)
copy_len = min(len(coeffs_asc), max_degree + 1)
padded[:copy_len] = coeffs_asc[:copy_len]
```

## Test Verification

Updated all test cases to use correct database format:

```python
# Quadratic with padding
"[0.0, 2.0, -5.0, 3.0]"  # ✓ 2x² - 5x + 3

# Constant without padding
"[5.0]"  # ✓ 5

# Quartic with padding
"[0.0, 1.0, 0.0, 0.0, 0.0, 2.0]"  # ✓ x⁴ + 2
```

All tests pass:
```
✓ Correct parsing of descending power order with padding
✓ Proper coefficient padding to max_degree + 1
✓ Quartic polynomial (6 elements) handled correctly
✓ Constant polynomial (1 element, no padding) handled correctly
```

## Documentation Updates

Updated in `feature_engineering.md`:
- Corrected vmResult format description
- Updated all examples to include leading zero padding
- Clarified database vs. logical format distinction

## Status

✅ Bug fixed
✅ Tests updated and passing
✅ Documentation corrected
✅ Ready for data loading from Neo4j database

