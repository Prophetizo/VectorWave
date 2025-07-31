# Changelog

All notable changes to VectorWave will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Vector API graceful fallback mechanism for environments without incubator module support
- Comprehensive unit tests for Vector API fallback functionality (`VectorApiFallbackTest`)
- Runtime detection of Vector API availability with automatic scalar fallback
- Public API methods to check Vector API status (`OptimizedFFT.isVectorApiAvailable()`, `getVectorApiInfo()`)
- Enhanced benchmark configuration to work with and without Vector API

### Changed
- Updated JMH benchmarks to use `jvmArgsAppend` instead of `jvmArgs` for flexible configuration
- Enhanced `jmh-runner.sh` to automatically detect Vector API availability
- Improved FFT implementation to seamlessly switch between vectorized and scalar implementations
- Updated documentation to reflect Vector API optional nature
- Consolidated benchmark documentation in `docs/BENCHMARKING.md`

### Fixed
- Fixed compilation and runtime errors when Vector API module is not available
- Fixed benchmark failures on JVMs without incubator module support

### Documentation
- Reorganized documentation structure for clarity
- Moved implementation notes to `docs/implementation-notes/`
- Updated README.md to reflect current capabilities
- Enhanced CLAUDE.md with Vector API fallback information

## [1.1.0] - 2025-01-30

### Added
- Zero-copy streaming wavelet transform implementation (`OptimizedStreamingWaveletTransform`)
- `WaveletTransform.forward(double[], int, int)` method for array slice processing
- Lock-free ring buffer for streaming operations
- Configurable buffer capacity multiplier for streaming transforms
- Zero-copy verification tests (`ZeroCopyVerificationTest`)
- JMH benchmark for streaming transform performance (`StreamingTransformBenchmark`)
- Thread-local buffers in `StreamingRingBuffer` for thread safety

### Changed
- Refactored `OptimizedStreamingWaveletTransform` to use true zero-copy processing
- Simplified exponential backoff logic with extracted `handleBufferFull()` method
- Updated `ScalarOps` to support array slices for zero-copy operations
- Improved race condition handling in `RingBuffer` with atomic snapshots
- Enhanced documentation to reflect actual implementation behavior

### Fixed
- Race condition in `RingBufferIntegrationTest` using `CountDownLatch` synchronization
- False advertising in documentation about zero-copy capabilities
- Race conditions in `RingBuffer` methods through consistent atomic snapshots
- Thread safety issues with pre-allocated buffers

### Performance
- 50% reduction in memory bandwidth usage for streaming transforms
- Eliminated array copying during transform operations
- Better cache locality with ring buffer design
- Lower GC pressure through buffer reuse

## [1.0.0] - 2025-01-15

## Recent Updates (2025)

### Continuous Wavelet Transform (CWT) Implementation
1. **FFT-Accelerated CWT**: Complete O(n log n) CWT implementation using Cooley-Tukey FFT
   - Replaced inefficient O(n²) DFT with proper FFT algorithm
   - FFTAcceleratedCWT class with comprehensive conjugate-based IFFT
   - Platform-adaptive optimization thresholds and caching
   
2. **Financial Wavelets**: Specialized wavelets for market analysis
   - **PaulWavelet**: Optimal for detecting market volatility and trends
   - **ShannonWavelet**: Frequency analysis with excellent localization
   - **DOGWavelet**: Difference of Gaussians for edge detection in price data
   - **FinancialWaveletAnalyzer**: High-level API for market crash detection

3. **Gaussian Derivative Wavelets**: Feature detection capabilities
   - Orders 1-8 for edge, ridge, and inflection point detection
   - **GaussianDerivativeWavelet(2)**: Mexican Hat wavelet for blob detection
   - Automatic registration in WaveletRegistry as "gaus1", "gaus2", etc.

4. **Platform-Adaptive Cache Configuration**: 
   - Auto-detection of Apple Silicon (128KB L1, 4MB L2) vs x86 (32KB L1, 256KB L2)
   - Configurable via system properties: `ai.prophetizo.cache.l1.size`, etc.
   - **CacheAwareOps.CacheConfig**: Flexible cache parameter tuning

5. **Enhanced Documentation**: Comprehensive Javadoc improvements
   - Detailed algorithm explanations with complexity analysis
   - Usage examples and mathematical background
   - Performance characteristics and optimization guidance

### Performance and Bug Fixes
1. **Thread Safety**: Fixed thread indexing collision in LatencyBenchmark using AtomicInteger
2. **Streaming Transforms**: Resolved infinite loop issue in StreamingWaveletTransformTest
3. **Code Cleanup**: Removed obsolete comments in VectorOptimizationBenchmark
4. **SIMD Control**: Added TransformConfig for explicit scalar/SIMD path control
5. **Test Determinism**: Replaced Math.random() with seeded Random for reproducible tests

### New Features
1. **ScalarVsVectorDemo**: Demonstrates optimization path control and validation
2. **Enhanced Exception Hierarchy**: Custom exceptions with error codes for precise handling
3. **Memory Pooling**: Thread-safe memory pools for reduced GC pressure
4. **Multi-level Decomposition**: Efficient multi-resolution analysis

### Performance Characteristics

#### Discrete Wavelet Transform (DWT)
- Haar wavelet: ~107 ns/op for 64-sample signals
- DB2 wavelet: ~193 ns/op for 64-sample signals  
- DB4 wavelet: ~294 ns/op for 64-sample signals
- Minimal SIMD overhead for small signals (<256 samples)

#### Continuous Wavelet Transform (CWT)
- **FFT-Accelerated**: O(n log n) complexity vs O(n²) direct convolution
- **Performance scaling**: FFT shows significant advantage for signals >1024 samples
- **Apple Silicon optimization**: Leverages 128KB L1 and 4MB L2 cache sizes
- **Memory efficiency**: Platform-adaptive block sizes for optimal cache utilization
- **Real-time capable**: Sub-millisecond processing for moderate signal sizes

#### Cache Performance
- **Auto-detection**: Platform-specific cache sizes (Apple Silicon vs x86)
- **Configurable**: System properties for custom cache tuning
- **Block optimization**: Optimal blocking based on L1/L2 cache hierarchy

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