# QuickSelect Algorithm Improvements

## Overview
Improved the quickSelect implementation in `MODWTStreamingDenoiser` to address Copilot's concerns about complexity, bounds checking, and documentation.

## Improvements Made

### 1. Enhanced Bounds Checking
- Added validation to ensure `k` is within the valid range `[left, right]`
- Added validation to ensure array bounds are valid
- Added empty array check in `calculateMAD` method
- Clear error messages indicate exactly what went wrong

### 2. Improved Documentation
- Added comprehensive JavaDoc for `quickSelect` method explaining:
  - Purpose and efficiency (O(n) vs O(n log n))
  - Parameter meanings and constraints
  - Return value description
  - Exception conditions
- Added documentation for `partition` method
- Added documentation for `swap` method
- Added documentation for `calculateMAD` method

### 3. Algorithm Enhancements
- Implemented median-of-three pivot selection in partition method
  - Reduces worst-case scenarios on sorted or reverse-sorted data
  - Improves average performance by choosing better pivots
- Added optimization to skip swap when indices are equal
- Better handling of edge cases

### 4. Code Quality
- More descriptive variable names and comments
- Clear separation of concerns between methods
- Defensive programming with proper validation

## Benefits
1. **Robustness**: The algorithm now fails fast with clear error messages instead of producing incorrect results
2. **Performance**: Median-of-three pivot selection improves average-case performance
3. **Maintainability**: Better documentation makes the code easier to understand and modify
4. **Safety**: Bounds checking prevents array index out of bounds exceptions

## Testing
All existing tests continue to pass, confirming that the improvements maintain correctness while adding safety and clarity.