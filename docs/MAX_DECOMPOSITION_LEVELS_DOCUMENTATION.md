# MAX_DECOMPOSITION_LEVELS Documentation Enhancement

## Overview
Enhanced the documentation for the `MAX_DECOMPOSITION_LEVELS` constant in `MultiLevelMODWTTransform` to explain the rationale behind the limit of 10 levels, as suggested by Copilot.

## Documentation Added

### 1. Comprehensive Rationale
The enhanced documentation now explains four key reasons for the limit:

1. **Numerical Stability**: At level j, filters are upsampled by 2^(j-1), leading to very long filters (e.g., 512x original length at level 10)

2. **Signal Resolution**: Most real-world signals lose meaningful information beyond 8-10 levels due to finite precision

3. **Memory Requirements**: Each level stores full-length coefficient arrays, so 10 levels require 10x the original signal memory

4. **Practical Usage**: Financial and scientific applications rarely need more than 6-8 levels

### 2. Mathematical Examples
Added concrete examples showing the relationship between signal length, filter length, and maximum meaningful levels:
- Signal length 1024, Haar filter (L=2): max ≈ 9 levels
- Signal length 4096, DB4 filter (L=8): max ≈ 9 levels  
- Signal length 65536, DB8 filter (L=16): max ≈ 12 levels

### 3. Flexibility Note
Documented that the constant can be increased for specific applications, with a warning about performance and numerical stability considerations.

## API Enhancements

Added two new methods to make the limit more accessible:

1. **`getMaximumLevels(int signalLength)`**: Returns the maximum decomposition levels for a given signal length (capped at 10)

2. **`getMaxDecompositionLevels()`**: Static method that returns the constant value (10)

## Benefits

1. **Clarity**: Developers now understand why 10 was chosen as the limit
2. **Flexibility**: The reasoning is documented if someone needs to modify it
3. **Accessibility**: New API methods make it easier to work with the limit programmatically
4. **Education**: Examples help users understand the mathematical constraints

## Testing
Added comprehensive test coverage in `testMaxDecompositionLevelsConstant()` that:
- Verifies the constant value
- Tests the capping behavior
- Ensures decomposition works up to the limit
- Confirms exceptions are thrown when exceeding the limit

This enhancement addresses Copilot's concern by providing thorough documentation that explains both the practical and mathematical reasons for the chosen limit.