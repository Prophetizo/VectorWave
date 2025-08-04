# Enhanced Error Handling Implementation

## Overview

This document describes the enhanced error handling implementation for the VectorWave library, addressing issue #157's requirement for better debugging context in error messages.

## Implementation Details

### 1. **ErrorContext Builder**

Created `ErrorContext` class that provides a fluent builder pattern for constructing detailed error messages:

```java
throw new InvalidSignalException(
    ErrorCode.VAL_EMPTY,
    ErrorContext.builder("Signal cannot be empty")
        .withSignalInfo(signal.length)
        .withWavelet(wavelet)
        .withBoundaryMode(boundaryMode)
        .withSuggestion("Provide a signal with at least one sample")
        .build()
);
```

### 2. **Error Codes**

Leveraged existing `ErrorCode` enum with categorized codes:
- `VAL_xxx`: Validation errors
- `CFG_xxx`: Configuration errors  
- `SIG_xxx`: Signal processing errors
- `STATE_xxx`: State-related errors
- `POOL_xxx`: Resource pool errors

### 3. **Enhanced Error Messages Structure**

Each enhanced error message now includes:

1. **Base Message**: Clear description of the error
2. **Context Section**: Relevant operation details
   - Operation name
   - Parameter values
   - Wavelet information
   - Boundary mode
   - Signal characteristics
3. **Suggestions Section**: Remediation steps
   - How to fix the issue
   - Alternative approaches
   - Valid parameter ranges

Example output:
```
Signal length must be power of two for DWT

Context:
   Signal length: 777
   Wavelet: Haar
   Boundary mode: PERIODIC
   
Suggestions:
   - Use MODWT for arbitrary length signals
   - Pad signal to nearest power of two: 1024
```

### 4. **Files Enhanced**

Enhanced error handling in key files:

1. **MODWTTransform.java**
   - Boundary mode validation with suggestions
   - Empty signal validation with context
   - Invalid coefficient detection

2. **ScalarOps.java**
   - Null parameter validation with operation context
   - Array length mismatch with dimension details
   - Integer overflow protection with level information

3. **MultiLevelMODWTTransform.java**
   - Decomposition level validation with maximum levels
   - Integer overflow protection for bit shifts
   - Level range validation for reconstruction

4. **WaveletDenoiser.java**
   - Safe level validation for scale-dependent thresholds
   - Invalid level access with available range
   - Unsupported operation guidance

### 5. **Context Methods**

The `ErrorContext` builder provides specialized methods:

- `withSignalInfo(length)`: Add signal characteristics
- `withWavelet(wavelet)`: Include wavelet details
- `withBoundaryMode(mode)`: Add boundary mode info
- `withLevelInfo(requested, max)`: Show level constraints
- `withArrayDimensions(expected, actual)`: Compare dimensions
- `withPerformanceContext(op, size)`: Add performance info
- `withSuggestion(text)`: Add remediation suggestions

### 6. **Benefits**

1. **Better Debugging**: Developers see exactly what went wrong
2. **Faster Resolution**: Suggestions guide to solutions
3. **Programmatic Handling**: Error codes enable automated recovery
4. **Consistent Format**: All errors follow same structure
5. **Context Preservation**: Operation details help trace issues

## Usage Guidelines

When throwing exceptions:

1. Use appropriate custom exception class (not `IllegalArgumentException`)
2. Include relevant `ErrorCode`
3. Build context with all available information
4. Add at least one suggestion for resolution
5. Keep base message concise but clear

## Example Integration

Before:
```java
throw new IllegalArgumentException("Signal cannot be null");
```

After:
```java
throw new InvalidArgumentException(
    ErrorCode.VAL_NULL_ARGUMENT,
    ErrorContext.builder("Signal array cannot be null")
        .withContext("Operation", "convolveAndDownsamplePeriodic")
        .withContext("Parameter", "signal")
        .withSuggestion("Ensure signal array is initialized before calling this method")
        .build()
);
```

## Future Enhancements

1. Add more specialized context methods as needed
2. Consider logging integration with error codes
3. Add performance impact warnings for expensive operations
4. Include links to documentation in suggestions
5. Add telemetry hooks for error tracking