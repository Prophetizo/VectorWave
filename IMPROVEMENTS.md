# VectorWave Architectural Improvements

This document summarizes the architectural improvements made to ensure consistency and maintainability across the VectorWave codebase.

## 1. Standardized Builder Method Naming

**Before:**
```java
builder.withBoundaryMode(BoundaryMode.PERIODIC)
       .withMaxLevels(5)
       .build();
```

**After:**
```java
builder.boundaryMode(BoundaryMode.PERIODIC)
       .maxLevels(5)
       .build();
```

All builder methods now use direct property names without the "with" prefix, following modern Java conventions.

## 2. Custom Exception Hierarchy with Error Codes

### Exception Hierarchy
```
WaveletTransformException (base)
├── InvalidArgumentException
├── InvalidSignalException  
├── InvalidStateException
└── InvalidConfigurationException
```

### Error Code System
Each exception can now include an error code for programmatic handling:

```java
try {
    // Some operation
} catch (InvalidArgumentException e) {
    if (e.getErrorCode() == ErrorCode.VAL_NULL_ARGUMENT) {
        // Handle null argument specifically
    }
}
```

Error codes are categorized:
- `VAL_xxx` - Validation errors
- `CFG_xxx` - Configuration errors  
- `SIG_xxx` - Signal processing errors
- `STATE_xxx` - State-related errors
- `POOL_xxx` - Resource pool errors

### Example Usage
```java
// Factory methods with error codes
InvalidArgumentException.nullArgument("signal")
// Returns exception with ErrorCode.VAL_NULL_ARGUMENT

InvalidSignalException.notPowerOfTwo(100)  
// Returns exception with ErrorCode.VAL_NOT_POWER_OF_TWO
```

## 3. Standardized Null Checking

### NullChecks Utility
A centralized utility class provides consistent null checking:

```java
public void process(Wavelet wavelet, double[] signal) {
    NullChecks.requireNonNull(wavelet, "wavelet");
    NullChecks.requireNonNull(signal, "signal");
    // ... rest of method
}
```

### Benefits
- Consistent error messages across the codebase
- Automatic error codes (VAL_NULL_ARGUMENT)
- Drop-in replacement for Objects.requireNonNull
- Additional methods for arrays and multiple parameters

### Available Methods
- `requireNonNull(T obj, String parameterName)`
- `requireNonEmpty(double[] array, String parameterName)`
- `requireBothNonNull(T obj1, String name1, U obj2, String name2)`
- `requireNoNullElements(T[] array, String arrayName)`

## 4. Consolidated Validation Patterns

### WaveletValidationUtils
Centralized wavelet-specific validation:

```java
// Validate discrete wavelet
WaveletValidationUtils.validateDiscreteWavelet(wavelet);

// Validate continuous wavelet  
WaveletValidationUtils.validateContinuousWavelet(wavelet);
```

### ValidationUtils Enhancements
- Signal validation with comprehensive checks
- Power-of-two validation with overflow protection
- Array validation utilities

## 5. Benefits of These Improvements

### Consistency
- Uniform exception handling across the codebase
- Standardized error messages
- Consistent builder patterns

### Maintainability  
- Centralized validation logic
- Reduced code duplication
- Clear separation of concerns

### Debugging
- Error codes for programmatic error handling
- Detailed error messages with context
- Consistent exception types

### Type Safety
- Custom exceptions extend base WaveletTransformException
- Sealed wavelet interface hierarchy
- Compile-time validation of wavelet types

## Migration Guide

### For Library Users
1. Update exception catch blocks from standard Java exceptions to custom ones:
   ```java
   // Before
   catch (IllegalArgumentException e)
   
   // After  
   catch (InvalidArgumentException e)
   ```

2. Remove "with" prefix from builder method calls:
   ```java
   // Before
   builder.withBoundaryMode(mode)
   
   // After
   builder.boundaryMode(mode)
   ```

3. Use error codes for specific error handling:
   ```java
   catch (InvalidArgumentException e) {
       if (e.getErrorCode() == ErrorCode.VAL_NULL_ARGUMENT) {
           // Handle null argument
       }
   }
   ```

### For Contributors
1. Use NullChecks utility for null validation:
   ```java
   NullChecks.requireNonNull(param, "paramName");
   ```

2. Use appropriate custom exceptions with error codes:
   ```java
   throw InvalidArgumentException.nullArgument("signal");
   ```

3. Follow builder pattern without "with" prefix
4. Add validation to WaveletValidationUtils for wavelet-specific checks