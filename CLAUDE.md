# CLAUDE.md

Development guide for Claude Code when working with the VectorWave repository.

## Recent Updates (August 2025)

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
1. **CRITICAL: Biorthogonal Wavelets (#138)**
   - BiorthogonalSpline implementation produces catastrophic reconstruction errors (RMSE > 1.4)
   - Incorrect filter coefficients and violated perfect reconstruction conditions
   - Test `FFMWaveletTransformTest.testBiorthogonalWavelets()` is disabled
   - **Workaround**: Use orthogonal wavelets (Haar, Daubechies, Symlets) instead

2. **Boundary Mode Limitations (#135-137)**
   - SYMMETRIC and CONSTANT modes not implemented for FFM upsampling operations
   - Will throw `UnsupportedOperationException` if used
   - Downsampling operations support all boundary modes

3. **FFM Memory Pool Arena Validation**
   - Memory segments from different arenas now throw `IllegalArgumentException` instead of silent failure
   - Helps catch programming errors during development

## Package Refactoring Tasks

### Complex.java Location Refactoring
- Problem: `Complex.java` is currently located in root wavelet package
- Suggested new locations:
  - `ai.prophetizo.wavelet.util`
  - `ai.prophetizo.wavelet.cwt.util`
  - `ai.prophetizo.wavelet.math`
- Considerations:
  - Used by multiple packages
  - Part of public API
  - Do not maintain backward compatibility

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
// Basic batch processing
WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = new double[32][1024];
TransformResult[] results = transform.forwardBatch(signals);

// Advanced configuration
OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
    .withParallelism(1)  // Disable threading for pure SIMD
    .withSoALayout(true);
OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
```

### Testing
- Comprehensive test suite in `BatchSIMDTransformTest`
- Performance benchmarks in `benchmark` package
- Validates correctness against sequential implementation

[Rest of the existing file content remains the same...]