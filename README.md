# VectorWave

High-performance wavelet transform library for Java 21+ with comprehensive wavelet family support, SIMD optimizations, and both Discrete (DWT) and Continuous (CWT) wavelet transforms.

## Features

### Core Capabilities
- **Multiple Wavelet Families**: Haar, Daubechies (DB2-DB20), Symlets, Coiflets, Biorthogonal, Morlet
- **Continuous Wavelet Transform (CWT)**: FFT-accelerated CWT with O(n log n) complexity
- **Maximal Overlap DWT (MODWT)**: Shift-invariant transform for arbitrary length signals
- **Complex Wavelet Analysis**: Full complex coefficient support with magnitude and phase
- **Financial Wavelets**: Specialized wavelets for market analysis
  - Paul wavelet: Asymmetric pattern detection (crashes, recoveries)
  - Shannon wavelet: Band-limited analysis
  - Shannon-Gabor wavelet: Time-frequency localization with reduced artifacts
  - DOG wavelets: Gaussian derivatives for smooth features
  - MATLAB-compatible Mexican Hat: Legacy system integration
- **Foreign Function & Memory API (FFM)**: Zero-copy operations with Java 23's FFM API
  - SIMD-aligned memory allocation
  - Reduced GC pressure through off-heap memory
  - Thread-safe memory pooling with lifecycle management
- **Type-Safe API**: Sealed interfaces with compile-time validation
- **Zero Dependencies**: Pure Java implementation
- **Flexible Boundary Handling**: Periodic, Zero-padding (Symmetric and Constant modes partially implemented)
- **Plugin Architecture**: ServiceLoader-based wavelet discovery for extensibility

### Performance
- **SIMD Optimizations**: Platform-specific Vector API support with automatic fallback
  - x86: AVX2/AVX512 when available
  - ARM: NEON support, optimized for Apple Silicon
  - Graceful scalar fallback for unsupported platforms
- **FFT Acceleration**: O(n log n) convolution for CWT
  - Split-radix FFT for optimal performance
  - Bluestein algorithm for arbitrary sizes
  - Pre-computed twiddle factor caching
  - Real-to-complex FFT optimization (2x speedup for real signals)
- **Cache-Aware Operations**: Platform-adaptive cache configuration
- **Adaptive Thresholds**: 8+ elements for ARM/Apple Silicon, 16+ for x86
- **Memory Efficiency**: Object pooling, aligned allocation, streaming memory management
- **Parallel Processing**: Fork-join framework for batch operations
- **True SIMD Batch Processing**: Process multiple signals simultaneously
  - Processes N signals in parallel (N = SIMD vector width)
  - Optimized memory layouts (SoA) for coalesced vector operations
  - Adaptive algorithm selection based on batch size and wavelet type
  - Specialized kernels for Haar and small wavelets
  - 2-4x speedup for typical workloads

### Advanced Features
- **Denoising**: Universal, SURE, and Minimax threshold methods
- **Streaming Support**: Real-time processing with Java Flow API
  - Fast mode: < 1 µs/sample latency
  - Quality mode: Enhanced SNR with overlap
  - Factory-based automatic mode selection
- **Multi-level Transforms**: Configurable decomposition levels
- **Complex Wavelet Analysis**: Full magnitude and phase information
  - Complex coefficient computation
  - Instantaneous frequency extraction
  - Phase synchronization analysis
- **Adaptive Scale Selection**: Automatic scale optimization
  - Signal-adaptive placement based on energy distribution
  - Multiple spacing strategies (dyadic, logarithmic, mel-scale)
  - Real-time adaptation with sub-millisecond overhead
- **DWT-Based CWT Reconstruction**: Fast and stable reconstruction method
  - Leverages orthogonal DWT properties
  - 10-300x faster than standard methods
  - O(N log N) complexity
  - Ideal for financial applications and real-time processing
- **Factory Pattern Architecture**: Modern dependency injection
  - Common factory interface for all components
  - Registry-based factory management
  - Polymorphic usage patterns
- **Configurable Financial Analysis**: Builder-pattern configuration
  - Custom risk-free rates for Sharpe ratio calculation
  - Adjustable crash detection thresholds
  - Volatility and regime change parameters
  - Anomaly detection settings

## Requirements

- Java 21 or later (must include jdk.incubator.vector module for compilation)
- Maven 3.6+
- Runtime: Vector API is optional (graceful fallback to scalar implementation)

> **Important**: The project requires compilation with a JDK that includes the Vector API
> (jdk.incubator.vector module), available in JDK 16+. At runtime, the code will automatically
> fall back to scalar implementations if Vector API is not available. See
> [Vector API Compilation Requirements](docs/technical-specs/VECTOR_API_COMPILATION.md) for details.

## Quick Start

```bash
# Build
mvn clean compile

# Run tests
mvn test

# Run benchmarks (automatically detects Vector API availability)
./jmh-runner.sh

# Run specific benchmark
./jmh-runner.sh OptimizedFFTBenchmark

# Run demos
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"
```

## Usage

### Basic Transform
```java
// Simple transform
WaveletTransform transform = WaveletTransformFactory.createDefault(new Haar());
double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
TransformResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);

// Using different wavelets
transform = WaveletTransformFactory.createDefault(Daubechies.DB4);
transform = WaveletTransformFactory.createDefault(BiorthogonalSpline.BIOR1_3);
transform = WaveletTransformFactory.createDefault(new MorletWavelet(6.0, 1.0));
```

### MODWT (Maximal Overlap DWT)
```java
// MODWT for shift-invariant analysis with arbitrary length signals
MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Works with any signal length (not just power-of-2)
double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // length 7

// Forward transform - produces same-length coefficients
MODWTResult result = modwt.forward(signal);
double[] approx = result.approximationCoeffs(); // length 7
double[] detail = result.detailCoeffs();       // length 7

// Perfect reconstruction
double[] reconstructed = modwt.inverse(result);

// MODWT is shift-invariant - ideal for pattern detection
double[] shifted = shiftSignal(signal, 2);
MODWTResult shiftedResult = modwt.forward(shifted);
// Coefficients are shifted versions of original (not true for DWT)
```

### Batch Processing
```java
// Process multiple signals in parallel using SIMD
WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = generateSignals(32, 1024); // 32 signals of length 1024

// Batch forward transform - processes multiple signals simultaneously
TransformResult[] results = transform.forwardBatch(signals);

// Batch inverse transform
double[][] reconstructed = transform.inverseBatch(results);

// For maximum performance with large batches
OptimizedTransformEngine engine = new OptimizedTransformEngine();
TransformResult[] optimizedResults = engine.transformBatch(signals, wavelet, mode);

// Configure batch processing
OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
    .withParallelism(8)           // Use 8 threads
    .withSoALayout(true)          // Enable Structure-of-Arrays optimization
    .withSpecializedKernels(true) // Use optimized kernels
    .withCacheBlocking(true);     // Enable cache-aware blocking
OptimizedTransformEngine customEngine = new OptimizedTransformEngine(config);

// Thread-local cleanup for batch SIMD operations (important in thread pools)
try {
    BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
} finally {
    BatchSIMDTransform.cleanupThreadLocals();
}
```

### FFM-Based Transform (Java 23+)
```java
// Using Foreign Function & Memory API for zero-copy operations
try (FFMMemoryPool pool = new FFMMemoryPool();
     FFMWaveletTransform ffmTransform = new FFMWaveletTransform(wavelet, pool)) {
    
    TransformResult result = ffmTransform.forward(signal);
    double[] reconstructed = ffmTransform.inverse(result);
    
    // Get memory pool statistics
    var stats = ffmTransform.getPoolStatistics();
    System.out.println("Pool hit rate: " + stats.hitRate());
}

// Streaming with FFM
try (FFMStreamingTransform streaming = new FFMStreamingTransform(wavelet, blockSize)) {
    streaming.processChunk(data, offset, length);
    if (streaming.hasCompleteBlock()) {
        TransformResult result = streaming.getNextResult();
    }
}
```

### Financial Analysis
```java
// Basic financial analysis with configurable parameters
FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
    .crashAsymmetryThreshold(0.7)
    .volatilityLowThreshold(0.5)
    .volatilityHighThreshold(2.0)
    .anomalyDetectionThreshold(3.0)
    .confidenceLevel(0.95)
    .build();

FinancialAnalyzer analyzer = new FinancialAnalyzer(config);
double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
double volatility = analyzer.analyzeVolatility(prices);
boolean hasAnomalies = analyzer.detectAnomalies(prices);

// Wavelet-based financial analysis - risk-free rate required
FinancialConfig waveletConfig = new FinancialConfig(0.045); // 4.5% annual risk-free rate

FinancialWaveletAnalyzer waveletAnalyzer = new FinancialWaveletAnalyzer(waveletConfig);
double sharpeRatio = waveletAnalyzer.calculateSharpeRatio(returns);
double waveletSharpe = waveletAnalyzer.calculateWaveletSharpeRatio(returns); // Must be power-of-2 length

// Live trading simulation with real-time analysis
LiveTradingSimulation simulation = new LiveTradingSimulation();
simulation.run(); // Interactive console-based trading bot
```

### Denoising
```java
WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4);
double[] clean = denoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);

// Multi-level denoising
double[] clean = denoiser.denoise(noisySignal, 
    ThresholdMethod.SURE, 
    4, // levels
    ThresholdType.SOFT
);

// Streaming denoiser with automatic implementation selection
StreamingDenoiser streamDenoiser = StreamingDenoiserFactory.create(
    Daubechies.DB4,
    ThresholdMethod.UNIVERSAL,
    512, // block size
    0.5  // overlap ratio - auto-selects fast or quality mode
);
```

### Continuous Wavelet Transform (CWT)
```java
// Basic CWT analysis
MorletWavelet wavelet = new MorletWavelet();
CWTTransform cwt = new CWTTransform(wavelet);
double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
CWTResult result = cwt.analyze(signal, scales);

// Automatic scale selection
DyadicScaleSelector scaleSelector = DyadicScaleSelector.create();
double[] optimalScales = scaleSelector.selectScales(signal, wavelet, samplingRate);
CWTResult autoResult = cwt.analyze(signal, optimalScales);

// Signal-adaptive scale selection
SignalAdaptiveScaleSelector adaptiveSelector = new SignalAdaptiveScaleSelector();
double[] adaptiveScales = adaptiveSelector.selectScales(signal, wavelet, samplingRate);
// Automatically places more scales where signal has energy

// Complex wavelet analysis for phase information
ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);
double[][] magnitude = complexResult.getMagnitude();
double[][] phase = complexResult.getPhase();
double[][] instFreq = complexResult.getInstantaneousFrequency();

// Phase synchronization between signals
ComplexCWTResult result1 = cwt.analyzeComplex(signal1, scales);
ComplexCWTResult result2 = cwt.analyzeComplex(signal2, scales);
double syncIndex = calculatePhaseSynchronization(result1.getPhase(), result2.getPhase());

// Financial analysis with specialized wavelets
PaulWavelet paulWavelet = new PaulWavelet(4); // Order 4 for market analysis
CWTTransform financialCWT = new CWTTransform(paulWavelet);
CWTResult crashAnalysis = financialCWT.analyze(priceReturns, scales);

// FFT-accelerated CWT with real-to-complex optimization
CWTConfig config = CWTConfig.builder()
    .enableFFT(true)           // Enable FFT acceleration
    .realOptimized(true)       // 2x speedup for real signals
    .normalizeScales(true)
    .build();
CWTTransform fftCwt = new CWTTransform(wavelet, config);

// Fast DWT-based CWT reconstruction (recommended)
DWTBasedInverseCWT dwtInverse = new DWTBasedInverseCWT(wavelet);
double[] reconstructed = dwtInverse.reconstruct(cwtResult);
// Works best with dyadic scales for optimal accuracy
```

### Wavelet Registry and Discovery
```java
// Discover available wavelets using ServiceLoader
Set<String> available = WaveletRegistry.getAvailableWavelets();
List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();

// Look up wavelets by name (case-insensitive)
Wavelet db4 = WaveletRegistry.getWavelet("db4");
Wavelet morl = WaveletRegistry.getWavelet("morl");

// Check if a wavelet exists
if (WaveletRegistry.hasWavelet("meyer")) {
    Wavelet meyer = WaveletRegistry.getWavelet("meyer");
}

// Force reload of wavelet providers
WaveletRegistry.reload();

// Get any provider loading warnings
List<String> warnings = WaveletRegistry.getLoadWarnings();

// Add custom wavelets via ServiceLoader
// 1. Implement WaveletProvider
// 2. Register in META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider
// See docs/WAVELET_PROVIDER_SPI.md for details
```

### Factory Pattern Usage
```java
// Access the factory registry
FactoryRegistry registry = FactoryRegistry.getInstance();

// Register custom factories
registry.register("myTransform", new MyCustomTransformFactory());

// Retrieve and use factories
Factory<WaveletOps, TransformConfig> opsFactory = 
    registry.getFactory("waveletOps", WaveletOps.class, TransformConfig.class)
        .orElseThrow(() -> new IllegalStateException("Factory not found"));

WaveletOps ops = opsFactory.create();

// Default factories are automatically registered
FactoryRegistry.registerDefaults();
```

### Wavelet Selection Guide

| Wavelet | Best For | Characteristics |
|---------|----------|-----------------|
| **Morlet** | General time-frequency analysis | Gaussian-modulated complex sinusoid, good frequency localization |
| **Paul** | Financial crash detection | Asymmetric, captures sharp rises/falls |
| **Mexican Hat (DOG2)** | Edge detection, volatility | Second derivative of Gaussian, zero crossings at edges |
| **DOG(n)** | Smooth feature extraction | Higher derivatives for smoother patterns |
| **Shannon** | Band-limited signals | Perfect frequency localization, poor time localization |
| **Shannon-Gabor** | Time-frequency analysis | Reduced artifacts vs classical Shannon |
| **Daubechies** | Signal compression, denoising | Compact support, orthogonal |
| **Symlets** | Symmetric signal analysis | Near-symmetric, orthogonal |
| **Coiflets** | Numerical analysis | Vanishing moments for polynomial signals |

### Streaming
```java
// Real-time streaming transform with zero-copy ring buffer
StreamingWaveletTransform stream = StreamingWaveletTransform.create(
    Daubechies.DB4,
    BoundaryMode.PERIODIC,
    512 // block size
);

stream.subscribe(result -> processResult(result));
stream.process(dataChunk);

// Zero-copy optimized streaming with configurable overlap
OptimizedStreamingWaveletTransform optimizedStream = new OptimizedStreamingWaveletTransform(
    Daubechies.DB4,
    BoundaryMode.PERIODIC,
    512,  // block size
    0.5,  // overlap factor (0.0-1.0)
    8     // buffer capacity multiplier
);

// Streaming denoiser with automatic mode selection
StreamingDenoiser denoiser = StreamingDenoiserFactory.create(
    Daubechies.DB4,
    ThresholdMethod.UNIVERSAL,
    512, // block size
    0.5  // overlap ratio - auto-selects implementation
);
```

### Performance Configuration
```java
// Force specific optimization path
TransformConfig config = TransformConfig.builder()
    .forceVector(true)  // or forceScalar(true)
    .enablePrefetch(true)
    .cacheOptimized(true)
    .build();

WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode, config);

// Platform-adaptive cache configuration
CacheAwareOps.CacheConfig cacheConfig = CacheAwareOps.getDefaultCacheConfig();
// Auto-detects: Apple Silicon (128KB L1, 4MB L2) vs x86 (32KB L1, 256KB L2)

// Custom cache configuration
CacheAwareOps.CacheConfig customConfig = CacheAwareOps.CacheConfig.create(
    64 * 1024,  // L1 size
    512 * 1024, // L2 size  
    64          // cache line size
);
```

## Documentation

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Performance Guide](docs/PERFORMANCE.md)
- [API Reference](docs/API.md)
- [Foreign Function & Memory API Guide](docs/FFM_API.md)
- [Adding New Wavelets](docs/ADDING_WAVELETS.md)
- [WaveletProvider SPI Guide](docs/WAVELET_PROVIDER_SPI.md)
- [Benchmarking Guide](docs/BENCHMARKING.md)
- [Financial Wavelets Guide](docs/FINANCIAL_WAVELETS.md)
- [Wavelet Normalization](docs/WAVELET_NORMALIZATION.md)
- [Wavelet Selection Guide](docs/WAVELET_SELECTION.md)
- [Batch Processing Guide](docs/BATCH_PROCESSING.md)
- [Factory Pattern Guide](docs/FACTORY_PATTERN.md)

## Available Demos

The project includes comprehensive demos showcasing various features:

### Core Functionality
- `Main` - Interactive menu system for all demos
- `WaveletShowcase` - Comprehensive wavelet transform demonstrations
- `PerformanceDemo` - Performance comparisons and benchmarks
- `ScalarVsVectorDemo` - SIMD vs scalar implementation comparison

### Financial Analysis
- `FinancialDemo` - Basic financial time series analysis
- `FinancialAnalysisDemo` - Advanced configurable analysis
- `FinancialOptimizationDemo` - Performance-optimized financial processing
- `LiveTradingSimulation` - Interactive trading bot simulation

### Advanced Features
- `BatchProcessingDemo` - True SIMD batch processing examples
- `StreamingDemo` - Real-time streaming transform examples
- `FFMDemo` - Foreign Function & Memory API demonstrations
- `MemoryPoolLifecycleDemo` - Memory management patterns
- `ComplexCWTDemo` - Complex wavelet analysis with phase
- `AdaptiveScaleDemo` - Automatic scale selection strategies

### CWT Demonstrations
- `CWTBasicDemo` - Continuous wavelet transform basics
- `CWTFFTOptimizationDemo` - FFT acceleration examples
- `DWTBasedReconstructionDemo` - Fast reconstruction methods
- `ShannonWaveletComparisonDemo` - Shannon vs Shannon-Gabor comparison

### Performance and Optimization
- `FFTOptimizationDemo` - FFT performance improvements
- `ConvolutionDemo` - Convolution optimization strategies
- `CacheOptimizationDemo` - Cache-aware algorithm demonstrations

Run demos with:
```bash
# Run all demos through interactive menu
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"

# Run specific demo
mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.LiveTradingSimulation"
```

## Known Issues

- **Boundary Modes** (#135-137): SYMMETRIC and CONSTANT modes not fully implemented for FFM upsampling operations.
- **FFM Requirements**: Requires Java 23+ with `--enable-native-access=ALL-UNNAMED` flag.
- **Power-of-Two Requirement**: Wavelet-based methods in FinancialWaveletAnalyzer require input arrays with power-of-two length.

## Requirements

- Java 21 or higher
- Maven 3.6+
- For SIMD: `--add-modules jdk.incubator.vector`
- For FFM: Java 23+ with `--enable-native-access=ALL-UNNAMED`

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.