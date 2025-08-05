# VectorWave API Reference

## Table of Contents

1. [Simplified Public API](#simplified-public-api) - **Start Here**
2. [MODWT (Maximal Overlap DWT) API](#modwt-maximal-overlap-dwt-api) - **Primary Transform**
3. [Wavelet Families](#wavelet-families)
4. [Denoising API](#denoising-api)
5. [Streaming API](#streaming-api)
6. [Batch Processing API](#batch-processing-api)
7. [Continuous Wavelet Transform (CWT)](#continuous-wavelet-transform-cwt)
8. [Financial Analysis API](#financial-analysis-api)
9. [Factory Pattern API](#factory-pattern-api)
10. [Plugin Architecture](#plugin-architecture)
11. [Memory Management API](#memory-management-api)
12. [Configuration API](#configuration-api)
13. [Exception Hierarchy](#exception-hierarchy)

## Simplified Public API

VectorWave 4.0+ provides a simplified public API that hides internal implementation details while providing automatic optimizations.

### WaveletOperations Facade

The main entry point for wavelet operations with automatic SIMD optimization.

```java
// Query platform capabilities
WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
System.out.println(perfInfo.description()); // Shows SIMD availability

// MODWT operations with automatic optimization
double[] signal = new double[1000];
double[] filter = {0.7071, 0.7071}; // Haar low-pass
double[] output = new double[signal.length];

// Circular convolution for MODWT (periodic boundary)
WaveletOperations.circularConvolveMODWT(signal, filter, output);

// Zero-padding convolution for MODWT
WaveletOperations.zeroPaddingConvolveMODWT(signal, filter, output);

// Denoising operations with automatic SIMD
double[] coefficients = getWaveletCoefficients();
double threshold = 0.5;
double[] softThresholded = WaveletOperations.softThreshold(coefficients, threshold);
double[] hardThresholded = WaveletOperations.hardThreshold(coefficients, threshold);
```

### Key Benefits of the Simplified API

1. **No Internal Classes**: All internal implementation classes are package-private
2. **Automatic Optimization**: The library automatically selects the best implementation (scalar/SIMD)
3. **Clean Factory Methods**: Use `MODWTResult.create()` instead of implementation classes
4. **No Power-of-2 Restrictions**: MODWT works with signals of any length
5. **Unified Interface**: Single entry point for operations through WaveletOperations

### Migration from Previous Versions

```java
// Old: Direct implementation class usage
MODWTResult result = new MODWTResultImpl(approx, detail);

// New: Factory method
MODWTResult result = MODWTResult.create(approx, detail);

// Old: Manual optimization selection
VectorOps.Denoising.softThreshold(coefficients, threshold);

// New: Automatic optimization through facade
WaveletOperations.softThreshold(coefficients, threshold);

// Old: Power-of-2 validation
if (!ValidationUtils.isPowerOfTwo(signal.length)) {
    throw new IllegalArgumentException("Signal must be power of 2");
}

// New: No validation needed - MODWT works with any length
// Just use the signal directly
```

## MODWT (Maximal Overlap DWT) API

MODWT is the primary transform in VectorWave, offering shift-invariance and support for arbitrary signal lengths.

### MODWTTransform

Non-decimated wavelet transform for shift-invariant analysis.

```java
// Creation
MODWTTransform modwt = new MODWTTransform(wavelet, boundaryMode);

// Forward transform - works with ANY signal length
MODWTResult forward(double[] signal)

// Inverse transform - perfect reconstruction
double[] inverse(MODWTResult result)

// Batch operations for multiple signals
MODWTResult[] forwardBatch(double[][] signals)
double[][] inverseBatch(MODWTResult[] results)

// Performance monitoring
PerformanceInfo getPerformanceInfo()
long estimateProcessingTime(int signalLength)

// Properties
Wavelet getWavelet()
BoundaryMode getBoundaryMode()
```

### MODWTResult

Result of MODWT with same-length coefficients.

```java
double[] approximationCoeffs()  // Same length as input signal
double[] detailCoeffs()        // Same length as input signal  
int getSignalLength()          // Original signal length
boolean isValid()              // Validation check

// Factory method
static MODWTResult create(double[] approx, double[] detail)
```

### MultiLevelMODWTTransform

Multi-level MODWT decomposition.

```java
// Creation
MultiLevelMODWTTransform mlModwt = new MultiLevelMODWTTransform(wavelet, boundaryMode);

// Multi-level decomposition
MultiLevelMODWTResult forward(double[] signal, int levels)

// Reconstruction
double[] inverse(MultiLevelMODWTResult result)

// Maximum decomposition levels for a signal
int getMaxDecompositionLevel(int signalLength)
```

### MultiLevelMODWTResult

Result of multi-level MODWT decomposition.

```java
double[] getApproximationCoeffs()         // Final approximation
double[] getDetailCoeffsAtLevel(int level) // Details at specific level
int getLevels()                           // Number of decomposition levels
MODWTResult getLevelResult(int level)     // Complete result at level
```

### Key MODWT Features

- **Shift-invariant**: Translation of input results in corresponding translation of coefficients
- **Arbitrary length**: Works with signals of any length (not just power-of-2)
- **Non-decimated**: Output coefficients have same length as input
- **Perfect reconstruction**: Machine-precision reconstruction error
- **Java 23 optimizations**: Automatic SIMD acceleration for large signals

### MODWT vs DWT Comparison

| Feature | DWT | MODWT |
|---------|-----|-------|
| Signal length | Power of 2 required | Any length |
| Output length | Half of input | Same as input |
| Shift-invariant | No | Yes |
| Redundancy | None | 2x redundancy |
| Use cases | Compression | Pattern detection, time series |

### MODWT Usage Examples

```java
// Basic usage with arbitrary signal length
MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length!
MODWTResult result = modwt.forward(signal);
double[] reconstructed = modwt.inverse(result);

// Multi-level decomposition
MultiLevelMODWTTransform mlModwt = new MultiLevelMODWTTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC);
MultiLevelMODWTResult mlResult = mlModwt.forward(signal, 3);

// Shift-invariant pattern detection
double[] signal1 = loadSignal();
double[] signal2 = shiftRight(signal1, 5);
MODWTResult result1 = modwt.forward(signal1);
MODWTResult result2 = modwt.forward(signal2);
// Coefficients in result2 are shifted versions of result1
```

## Batch Processing API

### Batch Transform Methods

Process multiple signals simultaneously using automatic SIMD optimization.

```java
// MODWT batch processing with automatic optimization
MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);

// Forward batch transform - automatically uses SIMD when beneficial
MODWTResult[] forwardBatch(double[][] signals)

// Inverse batch transform
double[][] inverseBatch(MODWTResult[] results)

// Example usage
double[][] signals = new double[32][1000]; // 32 signals of any length
MODWTResult[] results = transform.forwardBatch(signals);
double[][] reconstructed = transform.inverseBatch(results);
```

### Automatic Transform Optimization

VectorWave automatically applies optimizations based on signal characteristics and platform capabilities.

```java
// Simple usage - optimizations are automatic
MODWTTransform transform = new MODWTTransform(wavelet, mode);
MODWTResult result = transform.forward(signal);

// Batch processing - automatic SIMD optimization
MODWTResult[] results = transform.forwardBatch(signals);

// The transform automatically selects:
// - SIMD operations when beneficial (signal length > threshold)
// - Platform-specific optimizations (ARM vs x86)
// - Specialized kernels for common wavelets (Haar, etc.)
// - Cache-aware algorithms for large signals
// - Memory pooling for reduced allocation overhead

// No manual optimization configuration needed!
```

### BatchMemoryLayout

Memory layout optimization for batch processing.

```java
BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength);
layout.getAlignedLength()
layout.getBatchStride()
layout.isAligned()
```

## Wavelet Families

### Orthogonal Wavelets

```java
// Haar
new Haar()

// Daubechies
Daubechies.DB2, DB4, DB6, DB8, DB10, DB12, DB14, DB16, DB18, DB20

// Symlets
Symlet.SYM2, SYM3, SYM4, SYM5, SYM6, SYM7, SYM8

// Coiflets
Coiflet.COIF1, COIF2, COIF3, COIF4, COIF5
```

### Biorthogonal Wavelets

Biorthogonal wavelets use different filters for decomposition and reconstruction, offering linear phase response and symmetric filters.

```java
BiorthogonalSpline.BIOR1_1, BIOR1_3, BIOR1_5
BiorthogonalSpline.BIOR2_2, BIOR2_4, BIOR2_6, BIOR2_8
BiorthogonalSpline.BIOR3_1, BIOR3_3, BIOR3_5, BIOR3_7, BIOR3_9
```

**Note**: Biorthogonal wavelets include automatic phase compensation to correct for inherent circular shifts. Perfect reconstruction is achieved for simple signals (constant, sequential) with PERIODIC boundary mode. Complex signals may have small reconstruction errors, which is normal behavior for biorthogonal wavelets.

### Continuous Wavelets

```java
// Basic wavelets
new MorletWavelet()                      // Default parameters
new MorletWavelet(omega0, sigma)         // omega0: frequency, sigma: bandwidth

// Financial wavelets
new PaulWavelet(m)                       // m: order (2,4,6 recommended)
new DOGWavelet(m)                        // m: derivative order (1-4)
new ShannonGaborWavelet()                // Default parameters
new ShannonGaborWavelet(fb, fc)          // fb: bandwidth, fc: center frequency
new ClassicalShannonWavelet()            // Perfect frequency localization
new GaussianDerivativeWavelet(n)         // n: derivative order (1-4)
```

### Boundary Modes

```java
public enum BoundaryMode {
    PERIODIC,    // Signal wraps around (fully supported)
    ZERO,        // Zero padding (fully supported)
    SYMMETRIC,   // Mirror at boundaries (partial FFM support)
    REFLECT      // Reflect at boundaries (partial FFM support)
}
```

## Denoising API

### MODWTStreamingDenoiser

Real-time MODWT-based denoising with shift-invariant processing.

```java
// Builder pattern for configuration
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(333)  // Any size - no power-of-2 restriction!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
    .noiseWindowSize(1000)
    .build();

// Process data
double[] denoised = denoiser.denoise(noisySignal);

// Get noise statistics
double getEstimatedNoiseLevel()
long getSamplesProcessed()

// Flow API subscription
denoiser.subscribe(Flow.Subscriber<double[]> subscriber)
```

### Noise Estimation Methods

```java
public enum NoiseEstimation {
    MAD,        // Median Absolute Deviation (robust)
    STD,        // Standard Deviation
    ADAPTIVE    // Adaptive estimation
}
```

### Threshold Methods

```java
public enum ThresholdMethod {
    UNIVERSAL,  // sqrt(2 * log(n)) * sigma
    SURE,       // Stein's Unbiased Risk Estimate
    MINIMAX     // Minimax threshold
}

public enum ThresholdType {
    SOFT,       // Smooth threshold
    HARD        // Discontinuous threshold
}
```

### StreamingDenoiser

Real-time denoising with automatic implementation selection.

```java
// Factory creation with automatic mode selection
StreamingDenoiser denoiser = StreamingDenoiserFactory.create(
    wavelet, method, blockSize, overlapRatio
);

// Manual creation
FastStreamingDenoiser fast = new FastStreamingDenoiser(wavelet, method, blockSize);
QualityStreamingDenoiser quality = new QualityStreamingDenoiser(
    wavelet, method, blockSize, overlapRatio
);

// Usage
denoiser.process(samples);
denoiser.getLatencyMicros();
denoiser.getThroughputSamplesPerSec();
```

## Continuous Wavelet Transform (CWT)

### CWTTransform

```java
// Creation
CWTTransform cwt = new CWTTransform(wavelet);
CWTTransform cwt = new CWTTransform(wavelet, config);

// Analysis
CWTResult analyze(double[] signal, double[] scales)
ComplexCWTResult analyzeComplex(double[] signal, double[] scales)

// Configuration
CWTConfig config = CWTConfig.builder()
    .enableFFT(true)           // FFT acceleration
    .normalizeScales(true)     // Scale normalization
    .padding(PaddingType.ZERO) // Padding strategy
    .build();
// Real-to-complex optimization is automatically applied when appropriate
```

### CWTResult

```java
double[][] getCoefficients()      // Real coefficients matrix
double[] getScales()             // Scale values used
int getSignalLength()            // Original signal length
double getCoefficient(int scale, int time)

// Complex results
ComplexCWTResult extends CWTResult {
    double[][] getMagnitude()
    double[][] getPhase()
    double[][] getReal()
    double[][] getImaginary()
    double[][] getInstantaneousFrequency()
}
```

### Scale Selection

```java
// Automatic scale selection
ScaleSpace.dyadic(minScale, maxScale, voices)
ScaleSpace.logarithmic(minScale, maxScale, numScales)
ScaleSpace.linear(minScale, maxScale, numScales)
ScaleSpace.melScale(minFreq, maxFreq, numScales, samplingRate)

// Adaptive selection
SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
double[] scales = selector.selectScales(signal, wavelet, samplingRate);
```

### Inverse CWT

```java
// Standard reconstruction
InverseCWT inverse = new InverseCWT(wavelet);
double[] reconstructed = inverse.reconstruct(cwtResult);

// Fast DWT-based reconstruction (recommended)
DWTBasedInverseCWT dwtInverse = new DWTBasedInverseCWT(wavelet);
double[] reconstructed = dwtInverse.reconstruct(cwtResult);
```

## Financial Analysis API

### FinancialAnalyzer

Configurable financial time series analysis.

```java
// Creation - configuration required
FinancialAnalyzer analyzer = new FinancialAnalyzer(config);

// Analysis methods
double analyzeCrashAsymmetry(double[] prices)
double analyzeVolatility(double[] prices)
double analyzeRegimeTrend(double[] prices)
boolean detectAnomalies(double[] prices)

// Classification
VolatilityClassification classifyVolatility(double volatility)
boolean isCrashRisk(double asymmetry)
boolean isRegimeShift(double trendChange)

// Configuration access
FinancialAnalysisConfig getConfig()
```

### FinancialAnalysisConfig

Builder-pattern configuration for financial analysis.

```java
// All parameters are required - no defaults
FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
    .crashAsymmetryThreshold(0.7)      // Required, > 0
    .volatilityLowThreshold(0.5)       // Required, > 0
    .volatilityHighThreshold(2.0)      // Required, > low threshold
    .regimeTrendThreshold(0.02)        // Required, > 0
    .anomalyDetectionThreshold(3.0)    // Required, > 0 (std devs)
    .windowSize(256)                   // Required, >= 2 (any size with MODWT)
    .confidenceLevel(0.95)             // Required, 0 < level < 1
    .build();
```

### FinancialWaveletAnalyzer

Wavelet-based financial analysis with risk-free rate configuration.

```java
// Creation - FinancialConfig required
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config, transform);

// Sharpe ratio calculations
double calculateSharpeRatio(double[] returns)
double calculateSharpeRatio(double[] returns, double riskFreeRate)
double calculateWaveletSharpeRatio(double[] returns)  // Works with any length
double calculateWaveletSharpeRatio(double[] returns, double riskFreeRate)

// Configuration - risk-free rate is required
FinancialConfig config = new FinancialConfig(0.045);  // 4.5% annual
```

## Streaming API

### MODWTStreamingTransform

```java
// Creation
MODWTStreamingTransform stream = MODWTStreamingTransform.create(
    wavelet, boundaryMode, bufferSize
);

// Flow API subscription
stream.subscribe(new Flow.Subscriber<MODWTResult>() {
    public void onNext(MODWTResult result) { /* process */ }
    public void onError(Throwable t) { /* handle error */ }
    public void onComplete() { /* cleanup */ }
});

// Processing
stream.process(dataChunk);
stream.flush();
stream.close();
```

### MODWTStreamingTransform

Streaming MODWT with automatic optimization and configurable overlap.

```java
MODWTStreamingTransform stream = new MODWTStreamingTransform(
    wavelet,
    boundaryMode,
    blockSize,
    overlapFactor,    // 0.0 to 1.0
    bufferMultiplier  // Ring buffer capacity
);

// Metrics
stream.getProcessedSamples()
stream.getDroppedSamples()
stream.getAverageLatencyNanos()
// Optimizations are automatically applied based on block size and platform
```

### StreamingDenoiserFactory

Automatic implementation selection based on parameters.

```java
public enum ImplementationMode {
    FAST,     // Minimal latency, lower quality
    QUALITY,  // Better SNR, higher latency
    AUTO      // Automatic selection based on overlap
}

// Factory methods
StreamingDenoiser create(Wavelet w, ThresholdMethod m, int blockSize, double overlap)
StreamingDenoiser create(Wavelet w, ThresholdMethod m, int blockSize, double overlap, ImplementationMode mode)
```

## MODWT (Maximal Overlap DWT) API

### MODWTTransform

Non-decimated wavelet transform for shift-invariant analysis.

```java
// Creation
MODWTTransform modwt = new MODWTTransform(wavelet, boundaryMode);

// Forward transform - works with any signal length
MODWTResult forward(double[] signal)

// Inverse transform - perfect reconstruction
double[] inverse(MODWTResult result)

// Performance monitoring
PerformanceInfo getPerformanceInfo()
ProcessingEstimate estimateProcessingTime(int signalLength)
```

### MODWTResult

Result of MODWT with same-length coefficients.

```java
double[] approximationCoeffs()  // Same length as input
double[] detailCoeffs()        // Same length as input
int getSignalLength()          // Original signal length
boolean isValid()              // Validation check

// Factory method
static MODWTResult create(double[] approx, double[] detail)
```

### Key MODWT Features

- **Shift-invariant**: Translation of input results in corresponding translation of coefficients
- **Arbitrary length**: Works with signals of any length (not just power-of-2)
- **Non-decimated**: Output coefficients have same length as input
- **Perfect reconstruction**: Machine-precision reconstruction error
- **Java 23 optimizations**: Automatic SIMD acceleration for large signals

### MODWT vs DWT Comparison

| Feature | DWT | MODWT |
|---------|-----|-------|
| Signal length | Power of 2 required | Any length |
| Output length | Half of input | Same as input |
| Shift-invariant | No | Yes |
| Redundancy | None | 2x redundancy |
| Use cases | Compression, general analysis | Pattern detection, time series |

### MODWT Best Practices

1. **Signal Analysis**: MODWT is ideal for detecting patterns that may occur at any position
2. **Time Series**: Better for financial and economic time series analysis
3. **Feature Extraction**: Preserves temporal alignment for feature-based applications
4. **Computational Cost**: MODWT is more computationally intensive than DWT
5. **Memory Usage**: Requires 2x memory due to non-decimated coefficients

```java
// Example: Shift-invariant pattern detection
MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Original signal analysis
double[] signal = loadSignal();
MODWTResult result1 = modwt.forward(signal);

// Shifted signal analysis - coefficients will also be shifted
double[] shiftedSignal = shiftRight(signal, 5);
MODWTResult result2 = modwt.forward(shiftedSignal);

// Pattern detection works regardless of shift
double[] pattern1 = extractPattern(result1.detailCoeffs());
double[] pattern2 = extractPattern(result2.detailCoeffs());
// pattern1 and pattern2 will be similar, just shifted
```

## Factory Pattern API

### Factory Interface

Common interface for all VectorWave factories.

```java
public interface Factory<T, C> {
    T create();                    // Default configuration
    T create(C config);            // Custom configuration
    boolean isValidConfiguration(C config);
    String getDescription();
}
```

### FactoryRegistry

Centralized factory management.

```java
// Singleton access
FactoryRegistry registry = FactoryRegistry.getInstance();

// Registration
registry.register(String key, Factory<?, ?> factory)
registry.unregister(String key)
registry.clear()

// Retrieval
Optional<Factory<T, C>> getFactory(String key, Class<T> productType, Class<C> configType)
Optional<Factory<?, ?>> getFactory(String key)
boolean isRegistered(String key)
Set<String> getRegisteredKeys()

// Default factories
FactoryRegistry.registerDefaults()  // Registers all built-in factories
```

### Built-in Factories

```java
// MODWTTransformFactory - Primary transform factory
MODWTTransformFactory modwtFactory = new MODWTTransformFactory();
MODWTTransform modwt = modwtFactory.create(wavelet);
MODWTTransform modwtWithBoundary = modwtFactory.create(wavelet, BoundaryMode.PERIODIC);

// CWTFactory
Factory<CWTTransform, CWTConfig> cwtFactory = CWTFactory.getInstance();

// StreamingDenoiserFactory
Factory<StreamingDenoiser, StreamingDenoiserConfig> denoiserFactory = 
    StreamingDenoiserFactory.getInstance();

// Note: Wavelet operations are now accessed through the WaveletOperations facade
// which provides automatic optimization without manual factory configuration
```

## Plugin Architecture

### WaveletProvider

Service Provider Interface for custom wavelets.

```java
public interface WaveletProvider {
    List<Wavelet> getWavelets();
    String getName();
    String getVersion();
}

// Implementation example
public class CustomWaveletProvider implements WaveletProvider {
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(new MyCustomWavelet());
    }
}
```

### WaveletRegistry

Automatic wavelet discovery using ServiceLoader.

```java
// Discovery
Set<String> getAvailableWavelets()
List<String> getWaveletsByType(WaveletType type)
List<String> getOrthogonalWavelets()
List<String> getBiorthogonalWavelets()
List<String> getContinuousWavelets()

// Lookup
Wavelet getWavelet(String name)  // Case-insensitive
boolean hasWavelet(String name)

// Provider management
void reload()                    // Force provider reload
List<String> getLoadWarnings()   // Get any loading issues
void clearLoadWarnings()
```

### Registration

Add to `META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider`:
```
com.example.CustomWaveletProvider
```

## Memory Management API

### MemoryPool

Efficient array pooling for wavelet operations.

```java
// Creation
MemoryPool pool = new MemoryPool();
pool.setMaxArraysPerSize(10);  // Limit pool size

// Borrow and return arrays
double[] array = pool.borrowArray(1777); // Any size!
try {
    // Use array for MODWT operations
    MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    MODWTResult result = transform.forward(array);
} finally {
    pool.returnArray(array);  // Always return!
}

// Management
pool.clear()              // Clear all pooled arrays
pool.printStatistics()    // Print usage statistics

// Metrics
double getHitRate()       // Pool hit rate (0.0 to 1.0)
int getTotalPooledCount() // Total arrays in pool
long getBorrowCount()     // Total borrows
long getReturnCount()     // Total returns
```

### AlignedMemoryPool

SIMD-aligned memory allocation (internal use).

```java
// Note: This class is for internal use only
// Use MemoryPool for application code
```

### Memory Best Practices

1. **Always use try-finally** when borrowing arrays
2. **Set reasonable pool limits** to prevent unbounded growth
3. **Clear pools periodically** between processing phases
4. **Monitor hit rates** to tune pool sizes
5. **With MODWT**, borrow exact sizes needed (no padding required!)

### FFM Integration (Future)

Full Foreign Function & Memory API integration with MODWT is planned for future releases. Current FFM support includes memory pooling and aligned allocations for internal optimizations.

## Configuration API

### TransformConfig

Fine-grained transform configuration.

```java
TransformConfig config = TransformConfig.builder()
    .forceScalar(true)         // Force scalar implementation
    .forceVector(true)         // Force vector implementation
    .boundaryMode(mode)        // Override boundary mode
    .maxDecompositionLevels(5) // Limit decomposition depth
    .build();

// Validation
config.isForceScalar()
config.isForceVector()
config.getBoundaryMode()
config.getMaxDecompositionLevels()
```

### CWTConfig

CWT-specific configuration.

```java
CWTConfig config = CWTConfig.builder()
    .enableFFT(true)
    .normalizeScales(true)
    .padding(PaddingType.ZERO)
    .samplingFrequency(1000.0)
    .build();
// FFT optimizations including real-to-complex are automatically applied
```

### Performance Hints

```java
// Platform detection
PlatformDetector.isVectorAPIAvailable()
PlatformDetector.isARM()
PlatformDetector.isAppleSilicon()
PlatformDetector.getVectorSpecies()

// Cache configuration
CacheAwareOps.CacheConfig.getDefaultCacheConfig()
CacheAwareOps.CacheConfig.create(l1Size, l2Size, lineSize)
```

## Exception Hierarchy

```java
WaveletException (base)
├── InvalidSignalException
│   ├── nullSignal()
│   ├── emptySignal()
│   ├── nanValue()
│   └── infinityValue()
├── InvalidConfigurationException
│   ├── conflictingOptions()
│   ├── unsupportedBoundaryMode()
│   └── invalidWaveletForOperation()
├── InvalidArgumentException
│   ├── notPositive()
│   ├── outOfRange()
│   └── tooLarge()
└── TransformException
    ├── coefficientMismatch()
    └── decompositionError()
```

## Thread Safety

- **Immutable**: `TransformResult`, `Wavelet` implementations, `Config` classes
- **Thread-safe**: `WaveletRegistry`, `FactoryRegistry`, `FFMMemoryPool`
- **Not thread-safe**: `WaveletTransform`, `CWTTransform`, `StreamingTransform`

For concurrent usage:
```java
// Use separate instances per thread
ThreadLocal<WaveletTransform> threadLocalTransform = 
    ThreadLocal.withInitial(() -> new WaveletTransform(wavelet, mode));

// Or use synchronized access
synchronized(transform) {
    result = transform.forward(signal);
}

// Batch SIMD cleanup in thread pools
try {
    // Batch processing
} finally {
    BatchSIMDTransform.cleanupThreadLocals();
}
```

## Performance Best Practices

1. **Signal Length**: Use power-of-2 lengths for optimal performance
2. **Batch Processing**: Process multiple signals together for SIMD benefits
3. **Memory Pooling**: Reuse memory allocations with pooling
4. **Platform Detection**: Let the library auto-detect optimal implementations
5. **Batch Processing**: Automatic optimization for multiple signals

```java
// Simple batch processing - optimizations are automatic
MODWTTransform transform = new MODWTTransform(wavelet, mode);
MODWTResult[] results = transform.forwardBatch(signals);

// The transform automatically:
// - Uses SIMD instructions when beneficial
// - Optimizes memory layout (Structure-of-Arrays)
// - Applies platform-specific optimizations
// - Selects specialized kernels for common wavelets
// No manual configuration needed!
```