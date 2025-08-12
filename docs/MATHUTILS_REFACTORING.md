# MathUtils Refactoring Summary

## Overview
Refactored the `quickSelect` algorithm from `MODWTStreamingDenoiser` into a general-purpose `MathUtils` utility class, as suggested by Copilot.

## Changes Made

### 1. Created MathUtils Utility Class
Created `/src/main/java/ai/prophetizo/wavelet/util/MathUtils.java` containing:
- **quickSelect()**: Find kth smallest element in O(n) average time
- **median()**: Calculate median using quickSelect
- **medianAbsoluteDeviation()**: Calculate MAD (robust measure of variability)
- **standardDeviation()**: Calculate sample standard deviation

### 2. Benefits of the Refactoring
1. **Reusability**: These algorithms are now available throughout the codebase
2. **Better Organization**: Mathematical utilities are centralized
3. **Improved Testing**: Dedicated test suite for mathematical functions
4. **Cleaner Code**: MODWTStreamingDenoiser is now focused on its core responsibility
5. **Enhanced Documentation**: Better JavaDoc in the utility class

### 3. Updated MODWTStreamingDenoiser
- Removed ~150 lines of algorithm implementation
- Replaced with simple calls to `MathUtils`:
  ```java
  private double calculateMAD(double[] values) {
      return MathUtils.medianAbsoluteDeviation(values);
  }
  
  private double calculateSTD(double[] values) {
      if (values.length == 0) return 0.0;
      if (values.length == 1) return 0.0;
      return MathUtils.standardDeviation(values);
  }
  ```

### 4. Algorithm Improvements
The refactored implementation includes:
- **Median-of-three pivot selection** for better average performance
- **Comprehensive error checking** with clear error messages
- **Support for both odd and even length arrays** in median calculation
- **Proper handling of edge cases** (empty arrays, single elements, etc.)

### 5. Test Coverage
Created comprehensive test suite `MathUtilsTest` covering:
- Basic quickSelect functionality
- Edge cases (single element, two elements)
- Error conditions (null, empty, out of bounds)
- Median calculation for odd/even lengths
- MAD calculation with known examples
- Standard deviation with verified calculations

## Verification
- All existing MODWT tests pass (59 tests)
- New MathUtils tests pass (7 tests)
- No functionality changes, just better organization

## Future Opportunities
The MathUtils class can be extended with other general-purpose algorithms:
- Percentile calculations
- Trimmed mean
- Variance
- Covariance
- Other robust statistics

This refactoring improves code maintainability and follows the DRY (Don't Repeat Yourself) principle.