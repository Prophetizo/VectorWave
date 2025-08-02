# VectorWave API Reference

## Table of Contents

1. [Core Transform API](#core-transform-api)
2. [Batch Processing API](#batch-processing-api)
3. [Wavelet Families](#wavelet-families)
4. [Denoising API](#denoising-api)
5. [Continuous Wavelet Transform (CWT)](#continuous-wavelet-transform-cwt)
6. [Financial Analysis API](#financial-analysis-api)
7. [Streaming API](#streaming-api)
8. [Factory Pattern API](#factory-pattern-api)
9. [Plugin Architecture](#plugin-architecture)
10. [FFM (Foreign Function & Memory) API](#ffm-api)
11. [Configuration API](#configuration-api)
12. [Exception Hierarchy](#exception-hierarchy)

## Core Transform API

### WaveletTransform

Main transform class for forward and inverse DWT operations.

```java
// Creation
WaveletTransform transform = WaveletTransformFactory.createDefault(wavelet);
WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode, config);

// Single signal operations
TransformResult forward(double[] signal)
double[] inverse(TransformResult result)

// Batch operations (NEW)
TransformResult[] forwardBatch(double[][] signals)
double[][] inverseBatch(TransformResult[] results)

// Properties
boolean isUsingVector()
Wavelet getWavelet()
BoundaryMode getBoundaryMode()
```

### TransformResult

Immutable result of wavelet transform.

```java
double[] approximationCoeffs()    // Low-frequency coefficients
double[] detailCoeffs()          // High-frequency coefficients  
int level()                      // Decomposition level
Wavelet wavelet()                // Wavelet used
BoundaryMode boundaryMode()      // Boundary handling mode

// Factory methods
static TransformResult create(double[] approx, double[] detail, Wavelet wavelet, BoundaryMode mode)
```

### MultiLevelTransformResult

Result of multi-level decomposition.

```java
TransformResult[] getLevels()      // All decomposition levels
TransformResult getLevel(int i)    // Specific level
double[] getApproximation()        // Final approximation
int levels()                       // Number of levels
```

## Batch Processing API

### Batch Transform Methods

Process multiple signals simultaneously using SIMD instructions.

```java
// In WaveletTransform class
TransformResult[] forwardBatch(double[][] signals)
double[][] inverseBatch(TransformResult[] results)

// Direct batch SIMD methods
BatchSIMDTransform.haarBatchTransformSIMD(double[][] signals, double[][] approx, double[][] detail)
BatchSIMDTransform.blockedBatchTransformSIMD(double[][] signals, double[][] approx, double[][] detail, double[] filter)
BatchSIMDTransform.adaptiveBatchTransform(double[][] signals, Wavelet wavelet, BoundaryMode mode)

// Thread-local cleanup (important for server applications)
BatchSIMDTransform.cleanupThreadLocals()
```

### OptimizedTransformEngine

High-performance engine with configurable optimizations.

```java
// Creation
OptimizedTransformEngine engine = new OptimizedTransformEngine();
OptimizedTransformEngine engine = new OptimizedTransformEngine(config);

// Batch operations
TransformResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode)

// Configuration
public static class EngineConfig {
    EngineConfig withParallelism(int threads)
    EngineConfig withMemoryPool(boolean enable)
    EngineConfig withSpecializedKernels(boolean enable)
    EngineConfig withSoALayout(boolean enable)  // Structure-of-Arrays
    EngineConfig withCacheBlocking(boolean enable)
}
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

⚠️ **Warning**: Critical bug (#138) - use orthogonal wavelets instead.

```java
BiorthogonalSpline.BIOR1_1, BIOR1_3, BIOR1_5
BiorthogonalSpline.BIOR2_2, BIOR2_4, BIOR2_6, BIOR2_8
BiorthogonalSpline.BIOR3_1, BIOR3_3, BIOR3_5, BIOR3_7, BIOR3_9
```

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

### WaveletDenoiser

```java
WaveletDenoiser denoiser = new WaveletDenoiser(wavelet);

// Single-level denoising
double[] denoise(double[] signal, ThresholdMethod method)

// Multi-level denoising
double[] denoise(double[] signal, ThresholdMethod method, int levels)
double[] denoise(double[] signal, ThresholdMethod method, int levels, ThresholdType type)

// With custom threshold
double[] denoiseWithThreshold(double[] signal, double threshold)
double[] denoiseWithThreshold(double[] signal, double threshold, ThresholdType type)

// Get threshold value
double computeThreshold(double[] signal, ThresholdMethod method)
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
    .realOptimized(true)       // Real-to-complex optimization
    .normalizeScales(true)     // Scale normalization
    .padding(PaddingType.ZERO) // Padding strategy
    .build();
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
    .windowSize(256)                   // Required, power of 2
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
double calculateWaveletSharpeRatio(double[] returns)  // Requires power-of-2 length
double calculateWaveletSharpeRatio(double[] returns, double riskFreeRate)

// Configuration - risk-free rate is required
FinancialConfig config = new FinancialConfig(0.045);  // 4.5% annual
```

## Streaming API

### StreamingWaveletTransform

```java
// Creation
StreamingWaveletTransform stream = StreamingWaveletTransform.create(
    wavelet, boundaryMode, blockSize
);

// Flow API subscription
stream.subscribe(new Flow.Subscriber<TransformResult>() {
    public void onNext(TransformResult result) { /* process */ }
    public void onError(Throwable t) { /* handle error */ }
    public void onComplete() { /* cleanup */ }
});

// Processing
stream.process(dataChunk);
stream.flush();
stream.close();
```

### OptimizedStreamingWaveletTransform

Zero-copy streaming with configurable overlap.

```java
OptimizedStreamingWaveletTransform stream = new OptimizedStreamingWaveletTransform(
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
// WaveletOpsFactory
Factory<WaveletOps, TransformConfig> opsFactory = WaveletOpsFactory.getInstance();

// WaveletTransformFactory  
WaveletTransformFactory factory = new WaveletTransformFactory();
factory.boundaryMode(BoundaryMode.PERIODIC);
factory.config(transformConfig);
WaveletTransform transform = factory.create(wavelet);

// CWTFactory
Factory<CWTTransform, CWTConfig> cwtFactory = CWTFactory.getInstance();

// StreamingDenoiserFactory
Factory<StreamingDenoiser, StreamingDenoiserConfig> denoiserFactory = 
    StreamingDenoiserFactory.getInstance();
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

## FFM API

### FFMWaveletTransform

Zero-copy transform using Foreign Function & Memory API (Java 23+).

```java
// Creation with memory pool
try (FFMMemoryPool pool = new FFMMemoryPool();
     FFMWaveletTransform transform = new FFMWaveletTransform(wavelet, pool)) {
    
    TransformResult result = transform.forward(signal);
    double[] reconstructed = transform.inverse(result);
    
    // Statistics
    FFMMemoryPool.Statistics stats = transform.getPoolStatistics();
    System.out.println("Hit rate: " + stats.hitRate());
}

// Drop-in replacement
FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet);
// Use exactly like WaveletTransform
```

### FFMMemoryPool

SIMD-aligned memory management.

```java
public class FFMMemoryPool implements AutoCloseable {
    // Creation
    FFMMemoryPool()
    FFMMemoryPool(long maxMemoryBytes)
    
    // Allocation
    MemorySegment allocateAligned(long size, int alignment)
    MemorySegment allocate(long size)
    
    // Statistics
    Statistics getStatistics()
    
    public record Statistics(
        long totalAllocations,
        long totalDeallocations,
        long currentlyAllocated,
        long peakAllocated,
        double hitRate
    ) {}
}
```

### FFMStreamingTransform

Streaming operations with FFM optimization.

```java
try (FFMStreamingTransform stream = new FFMStreamingTransform(wavelet, blockSize)) {
    stream.processChunk(data, offset, length);
    
    if (stream.hasCompleteBlock()) {
        TransformResult result = stream.getNextResult();
    }
}
```

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
    .realOptimized(true)       // Real-to-complex FFT optimization
    .build();
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
│   ├── notPowerOfTwo()
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
5. **Configuration**: Use `TransformConfig` for fine-tuning

```java
// Optimal batch processing example
int batchSize = 32;  // Multiple of vector width
int signalLength = 1024;  // Power of 2

OptimizedTransformEngine engine = new OptimizedTransformEngine(
    new EngineConfig()
        .withParallelism(Runtime.getRuntime().availableProcessors())
        .withMemoryPool(true)
        .withSoALayout(true)
);

TransformResult[] results = engine.transformBatch(signals, wavelet, mode);
```