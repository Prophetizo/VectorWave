# MultiLevelMODWTTransform Overflow Protection

## Overview
Enhanced `MultiLevelMODWTTransform` with overflow protection for bit shift and multiplication operations, as suggested by Copilot.

## Issue Addressed
The bit shift operation `(1 << (maxLevels - 1))` in `calculateMaxLevels()` and similar operations in filter scaling methods could theoretically overflow for large values, even though `MAX_DECOMPOSITION_LEVELS = 10` provides practical protection.

## Implementation

### 1. Enhanced calculateMaxLevels() Method
```java
try {
    // Use Math.multiplyExact to detect overflow
    int scaleFactor = 1 << (maxLevels - 1);
    int scaledFilterLength = Math.addExact(
        Math.multiplyExact(filterLength - 1, scaleFactor), 
        1
    );
    
    if (scaledFilterLength > signalLength) {
        break;
    }
    maxLevels++;
} catch (ArithmeticException e) {
    // Overflow occurred - we've reached the practical limit
    break;
}
```

### 2. Protected Filter Scaling Methods
Both `scaleFilterForLevel()` and `upsampleFilterForLevel()` now use:
- `1 << (level - 1)` instead of `Math.pow(2, level - 1)` for efficiency
- `Math.multiplyExact()` for multiplication with overflow detection
- `Math.addExact()` for addition with overflow detection
- Clear exception handling with informative error messages

### 3. Benefits
1. **Robustness**: Prevents integer overflow even with extreme inputs
2. **Performance**: Using bit shifts is more efficient than `Math.pow()` for powers of 2
3. **Clear Errors**: Throws `InvalidArgumentException` with specific details when overflow would occur
4. **Future-proof**: Code remains safe even if `MAX_DECOMPOSITION_LEVELS` is increased

## Test Coverage
Created comprehensive test suite `MultiLevelMODWTOverflowTest` that:
- Tests normal operation with reflection to access private methods
- Verifies overflow protection triggers appropriately
- Tests decomposition up to MAX_DECOMPOSITION_LEVELS
- Confirms reconstruction accuracy is maintained

## Practical Impact
- For typical use (levels 1-10), no performance impact
- For extreme cases, provides graceful failure instead of undefined behavior
- Maintains backward compatibility while improving robustness

## Example Overflow Scenario
Without protection, at level 31:
- `scaleFactor = 1 << 30 = 1,073,741,824`
- For a filter of length 100: `scaledLength = 99 * 1,073,741,824 + 1`
- This would overflow, causing negative array sizes or other failures

With protection, this scenario throws a clear error:
```
InvalidArgumentException: Level 31 would create filter length exceeding integer limits
```

## Filter Truncation Validation
Also added validation to the `getTruncatedFilter()` method to prevent `ArrayIndexOutOfBoundsException`:
- Validates `targetLength > 0`
- Validates `targetLength <= filter.length`
- Throws clear `IllegalArgumentException` with details if validation fails

This ensures safe filter truncation when long filters at high decomposition levels need to be shortened to match signal length.