# CLAUDE.md

Development guide for Claude Code when working with the VectorWave repository.

## Latest Updates (August 2025)

### Complete Migration from DWT to MODWT - COMPLETE
- **REMOVED**: All DWT (Discrete Wavelet Transform) classes and support
- **PRIMARY TRANSFORM**: MODWT is now the primary and only discrete wavelet transform
- **Key Changes**:
  - Removed `WaveletTransform` class (DWT implementation)
  - Removed `TransformResult` and related DWT classes
  - Updated all demos to use MODWT exclusively
  - Updated all documentation to focus on MODWT
  - Removed all DWT test files
- **Migration Benefits**:
  - No more power-of-2 restrictions
  - Shift-invariant processing throughout
  - Better suited for real-time applications
  - Simplified API with consistent behavior

## Recent Updates (August 2025)

### MODWT Implementation - COMPLETE
- **Added**: Complete MODWT (Maximal Overlap Discrete Wavelet Transform) implementation
- Located in `ai.prophetizo.wavelet.modwt` package with three main classes:
  - `MODWTTransform`: Main transform class with Java 23 optimizations
  - `MODWTResult`: Interface for transform results
  - `MODWTResultImpl`: Immutable implementation with defensive copying
- **Key Features**:
  - Shift-invariant transform (unlike standard DWT)
  - Works with arbitrary length signals (not just power-of-2)
  - Non-decimated output (same length as input)
  - Perfect reconstruction with machine precision
  - Automatic SIMD optimization via Vector API
- **Performance**:
  - Uses optimized circular convolution in `ScalarOps.circularConvolveMODWT()`
  - Automatic vectorization for signals > 64 samples
  - Performance monitoring via `getPerformanceInfo()` and `estimateProcessingTime()`
- **Use Cases**:
  - Time series analysis (financial, economic data)
  - Pattern detection where shift-invariance is critical
  - Feature extraction with preserved temporal alignment
  - Signal analysis where arbitrary lengths are needed
- **Documentation Updated**:
  - README.md: Added MODWT to features list and usage examples
  - API.md: Comprehensive MODWT section with best practices
  - MODWTDemo.java: Enhanced with 7 comprehensive examples including performance analysis

### Biorthogonal Wavelet Fix (#138) - COMPLETE
- **FIXED**: Updated BiorthogonalSpline implementation with correct Cohen-Daubechies-Feauveau (CDF) 1,3 coefficients
- Changed from incorrect normalized coefficients to standard CDF coefficients:
  - Decomposition low-pass: `[-1/8, 1/8, 1, 1, 1/8, -1/8]`
  - Reconstruction low-pass: `[1, 1]`
- Added reconstruction scaling factor (0.5) to satisfy perfect reconstruction condition
- Fixed high-pass filter generation formula for biorthogonal wavelets
- **Implemented phase compensation** to correct the 2-sample circular shift inherent in CDF wavelets:
  - Added `groupDelay` property to BiorthogonalSpline (set to 2 for BIOR1_3)
  - WaveletTransform automatically applies phase compensation during reconstruction
  - Phase compensation works with PERIODIC boundary mode
- **Results**:
  - Constant and sequential signals now have perfect reconstruction (RMSE = 0)
  - Random and complex signals show expected reconstruction behavior
  - Perfect reconstruction condition is satisfied (peak value = 2.25 for BIOR1_3)
- **Note**: With ZERO_PADDING boundary mode, phase compensation is not applied and edge effects may occur

## Recent Updates (August 2025)

### Removal of Default Values
- **BREAKING CHANGE**: Removed all default values from financial configurations
- `FinancialConfig` no longer has a default constructor or default risk-free rate
- `FinancialAnalysisConfig` no longer has `defaultConfig()` method or any default constants
- `FinancialAnalyzer.withDefaultConfig()` has been removed
- `FinancialWaveletAnalyzer` default constructor has been removed
- All financial parameters must now be explicitly configured based on:
  - Current market conditions
  - Geographic region (US, EU, Asia, etc.)
  - Time period of analysis
  - Asset class being analyzed
- Updated all tests and demos to use explicit configurations
- Updated documentation to explain why defaults were removed

## Recent Updates (August 2025)

### Code Quality Improvements
- Extracted duplicate `isPowerOfTwo` validation logic to `ValidationUtils` utility class
- Fixed inefficient `OptimizedTransformEngine` creation in batch operations (now reused)
- Replaced all `System.err.println` calls with proper exception handling
- Improved floating-point comparisons with epsilon tolerance
- Enhanced error messages for better debugging
- Added comprehensive class-level documentation for power-of-two requirements

### Factory Pattern Implementation
- Added standardized `Factory<T,C>` interface in `ai.prophetizo.wavelet.api`
- Implemented factory pattern for all major components:
  - `WaveletOpsFactory` with static instance accessor
  - `WaveletTransformFactory` implementing Factory directly
  - `CWTFactory` with builder pattern support
  - `StreamingDenoiserFactory` with automatic implementation selection
- Centralized factory registry with `FactoryRegistry` singleton
- Improved error messages with context about what's being registered

### Financial Analysis Module
- Added `FinancialAnalyzer` with configurable thresholds via `FinancialAnalysisConfig`
- Added `FinancialWaveletAnalyzer` for wavelet-based financial metrics
- Implemented Sharpe ratio calculations (standard and wavelet-based)
- Builder pattern for configuration with validation

### ServiceLoader-based Plugin Architecture
- Automatic wavelet discovery via `WaveletRegistry`
- Three built-in providers: Orthogonal, Biorthogonal, Continuous
- Case-insensitive wavelet lookup
- Warning collection mechanism instead of stderr output
- Support for custom wavelet providers via SPI

### Documentation Updates
- Comprehensive update of README.md with all new features
- Complete rewrite of API.md with 12 major sections
- Created comprehensive demo files:
  - `BatchProcessingDemo` - showcases SIMD batch processing
  - `PluginArchitectureDemo` - demonstrates ServiceLoader features
  - `FFMDemo` - comprehensive FFM API demonstration
- Added detailed batch processing guide

### Batch SIMD Processing (#124)
- Added true SIMD batch processing in `ai.prophetizo.wavelet.internal.BatchSIMDTransform`
- Processes multiple signals in parallel using vector instructions
- New batch API methods in `WaveletTransform`: `forwardBatch()` and `inverseBatch()`
- Optimized memory layout in `ai.prophetizo.wavelet.memory.BatchMemoryLayout`
- Adaptive algorithm selection based on batch size and platform capabilities
- Performance improvements vary by platform (typical 2-4x speedup for aligned batches)

## Recent Updates (November 2024)

### Foreign Function & Memory API (FFM) Support
- Added comprehensive FFM implementation in `ai.prophetizo.wavelet.memory.ffm` package
- Provides zero-copy operations with SIMD-aligned memory
- Thread-safe memory pooling with automatic cleanup
- Requires Java 23+ with `--enable-native-access=ALL-UNNAMED` flag

### Known Issues
1. **Boundary Mode Limitations (#135-137)**
   - SYMMETRIC and CONSTANT modes not implemented for FFM upsampling operations
   - Will throw `UnsupportedOperationException` if used
   - Downsampling operations support all boundary modes

2. **FFM Memory Pool Arena Validation**
   - Memory segments from different arenas now throw `IllegalArgumentException` instead of silent failure
   - Helps catch programming errors during development

## Package Refactoring Tasks

### Complex.java Location Refactoring - COMPLETE
- **COMPLETED**: `Complex.java` has been moved to `ai.prophetizo.wavelet.math` package
- **New Location**: `ai.prophetizo.wavelet.math.Complex`
- **Changes Made**:
  - Created new `ai.prophetizo.wavelet.math` package with proper documentation
  - Moved Complex.java from `ai.prophetizo.wavelet.util` to `ai.prophetizo.wavelet.math`
  - Updated all import statements across the codebase
  - Removed old Complex.java from util package
  - Added package-info.java for the math package
- **Rationale**: Math package is more semantically appropriate for fundamental mathematical types
- **Impact**: All tests pass, compilation successful, no breaking changes to public API functionality

## Batch Processing Implementation

### Overview
The batch processing implementation provides true SIMD parallelization across multiple signals, addressing the performance issue where batch processing was slower than sequential processing.

### Key Components

1. **BatchSIMDTransform** (`ai.prophetizo.wavelet.internal.BatchSIMDTransform`)
   - Core SIMD batch processing algorithms
   - Multiple implementations:
     - `haarBatchTransformSIMD`: Specialized Haar implementation
     - `blockedBatchTransformSIMD`: Cache-optimized for general wavelets
     - `alignedBatchTransformSIMD`: Unrolled loops for aligned data
     - `adaptiveBatchTransform`: Automatic algorithm selection

2. **BatchMemoryLayout** (`ai.prophetizo.wavelet.memory.BatchMemoryLayout`)
   - Manages aligned memory for batch operations
   - Supports both standard and interleaved layouts
   - Automatic padding for SIMD alignment
   - Integration with `AlignedMemoryPool`

3. **API Integration**
   - `WaveletTransform.forwardBatch(double[][] signals)`
   - `WaveletTransform.inverseBatch(TransformResult[] results)`
   - Uses `OptimizedTransformEngine` internally

### Performance Considerations

- **Vector Length**: Uses `SPECIES_PREFERRED` for platform-specific optimization
- **Memory Alignment**: Signals are padded to vector length boundaries
- **Branch-Free Loops**: Full vectors processed first, remainder handled separately
- **Cache Optimization**: Block processing for large signal sets

### Usage Example

```java
// Basic MODWT batch processing
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = new double[32][777]; // Any length!
MODWTResult[] results = transform.forwardBatch(signals);

// Advanced configuration
MODWTOptimizedTransformEngine.EngineConfig config = new MODWTOptimizedTransformEngine.EngineConfig()
    .withParallelism(1)  // Disable threading for pure SIMD
    .withSoALayout(true);
MODWTOptimizedTransformEngine engine = new MODWTOptimizedTransformEngine(config);
```

### Testing
- Comprehensive test suite in `BatchSIMDTransformTest`
- Performance benchmarks in `benchmark` package
- Validates correctness against sequential implementation

### Thread-Local Memory Management
The `BatchSIMDTransform` class uses ThreadLocal storage to avoid allocations in hot paths. In thread pool or application server environments, call `BatchSIMDTransform.cleanupThreadLocals()` when done to prevent memory leaks:

1. **MODWTTransform** (`wavelet/modwt/MODWTTransform.java`): Main MODWT transform engine
   - Handles forward and inverse transforms with shift-invariance
   - Supports periodic and zero-padding boundary modes
   - Works with any wavelet type and signal length

2. **Wavelet Interfaces** (`wavelet/api/`):
   - `Wavelet`: Base sealed interface with core methods
   - `DiscreteWavelet`: Base for discrete wavelets with vanishing moments
   - `OrthogonalWavelet`: Wavelets where reconstruction = decomposition filters
   - `BiorthogonalWavelet`: Wavelets with dual filter pairs
   - `ContinuousWavelet`: Wavelets defined by mathematical functions

3. **WaveletRegistry** (`wavelet/api/WaveletRegistry.java`):
   - Central registry for all available wavelets
   - Lookup by name or type
   - Wavelet discovery functionality

4. **Transform Operations**:
   - `ScalarOps`: Core mathematical operations for wavelet transforms
   - Handles convolution, downsampling, upsampling for both boundary modes

### Important Technical Notes
- **Requires Java 23 or later** for full feature support
- Java 17+ compatible fallback mode available
- MODWT works with ANY signal length (no power-of-2 restriction)
- Multi-level MODWT transforms fully supported
- No external dependencies (pure Java implementation)
- Continuous wavelets available via CWT implementation
- **Vector API support** for high-performance SIMD operations

### Adding New Wavelets

To add a new wavelet type:

1. **For Orthogonal wavelets**: Implement `OrthogonalWavelet` interface
2. **For Biorthogonal wavelets**: Implement `BiorthogonalWavelet` interface  
3. **For Continuous wavelets**: Implement `ContinuousWavelet` interface
4. Register in `WaveletRegistry` static initializer
5. Add comprehensive tests

Example:
=======
```java
try {
    // Use batch SIMD transforms
    BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
} finally {
    // Clean up thread-local resources
    BatchSIMDTransform.cleanupThreadLocals();
}
```

[Rest of the existing file content remains the same...]