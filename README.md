# VectorWave

High-performance wavelet transform library for Java 23+ featuring the Maximal Overlap Discrete Wavelet Transform (MODWT) as the primary transform, with shift-invariance, arbitrary signal length support, and comprehensive wavelet family coverage. Also includes Continuous Wavelet Transform (CWT) with FFT acceleration.

## Features

### Core Capabilities
- **MODWT (Maximal Overlap Discrete Wavelet Transform)**: Primary transform offering:
  - Shift-invariance (translation-invariant) - crucial for pattern detection
  - Works with ANY signal length (no power-of-2 restriction)
  - Non-decimated output (same length as input)
  - Perfect reconstruction with machine precision
  - Ideal for streaming and real-time applications
- **Multiple Wavelet Families**: Haar, Daubechies (DB2-DB20), Symlets, Coiflets, Biorthogonal, Morlet
- **Continuous Wavelet Transform (CWT)**: FFT-accelerated CWT with O(n log n) complexity
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
- **Multi-level MODWT**: Configurable decomposition levels with MultiLevelMODWTTransform
- **Complex Wavelet Analysis**: Full magnitude and phase information
  - Complex coefficient computation
  - Instantaneous frequency extraction
  - Phase synchronization analysis
- **Adaptive Scale Selection**: Automatic scale optimization
  - Signal-adaptive placement based on energy distribution
  - Multiple spacing strategies (dyadic, logarithmic, mel-scale)
  - Real-time adaptation with sub-millisecond overhead
- **MODWT-Based CWT Reconstruction**: Fast and stable reconstruction method
  - Leverages orthogonal wavelet properties
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

- Java 23 or later (must include jdk.incubator.vector module for compilation)
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

### Basic Transform (MODWT)
```java
// Simple transform using MODWT
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);

// Using different wavelets
transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
transform = new MODWTTransform(BiorthogonalSpline.BIOR1_3, BoundaryMode.PERIODIC);

// Factory pattern for convenience
MODWTTransform transform = MODWTTransformFactory.create(new Haar());
```

### MODWT Features
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
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = generateSignals(32, 250); // 32 signals of any length!

// Batch forward transform - processes multiple signals simultaneously
MODWTResult[] results = transform.forwardBatch(signals);

// Batch inverse transform
double[][] reconstructed = transform.inverseBatch(results);

// For maximum performance with large batches
MODWTOptimizedTransformEngine engine = new MODWTOptimizedTransformEngine();
MODWTResult[] optimizedResults = engine.transformBatch(signals, wavelet, mode);

// Configure batch processing
MODWTOptimizedTransformEngine.EngineConfig config = new MODWTOptimizedTransformEngine.EngineConfig()
    .withParallelism(8)           // Use 8 threads
    .withSoALayout(true)          // Enable Structure-of-Arrays optimization
    .withSpecializedKernels(true) // Use optimized kernels
    .withCacheBlocking(true);     // Enable cache-aware blocking
MODWTOptimizedTransformEngine customEngine = new MODWTOptimizedTransformEngine(config);

// Thread-local cleanup for batch SIMD operations (important in thread pools)
try {
    BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
} finally {
    BatchSIMDTransform.cleanupThreadLocals();
}
```

### FFM-Based Operations (Java 23+)
```java
// Note: FFM integration with MODWT is planned for future releases
// Current FFM support includes memory pooling and aligned allocations

// Using FFM memory pools for efficient memory management
MemoryPool pool = new MemoryPool();
pool.setMaxArraysPerSize(10);

// Borrow arrays for MODWT operations
double[] signal = pool.borrowArray(1777); // Any size!
try {
    MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    MODWTResult result = transform.forward(signal);
    // Process results...
} finally {
    pool.returnArray(signal);
}

// Get memory pool statistics
pool.printStatistics();
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
double waveletSharpe = waveletAnalyzer.calculateWaveletSharpeRatio(returns); // Works with any length

// Live trading simulation with real-time analysis
LiveTradingSimulation simulation = new LiveTradingSimulation();
simulation.run(); // Interactive console-based trading bot
```

### Denoising with MODWT
```java
// MODWT-based denoising (works with any signal length!)
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(333) // Any size - no padding needed!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
    .build();

// Process streaming data
double[] denoisedBlock = denoiser.denoise(noisyBlock);
double noiseLevel = denoiser.getEstimatedNoiseLevel();

// Subscribe to denoised output stream
denoiser.subscribe(new Flow.Subscriber<double[]>() {
    @Override
    public void onNext(double[] denoised) {
        // Process denoised data
    }
    // ... other methods
});
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

// Fast MODWT-based CWT reconstruction (recommended)
MODWTBasedInverseCWT modwtInverse = new MODWTBasedInverseCWT(wavelet);
double[] reconstructed = modwtInverse.reconstruct(cwtResult);
// Works with any scales due to MODWT's flexibility
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

### Streaming with MODWT
```java
// Real-time MODWT streaming (works with any block size!)
MODWTStreamingTransform stream = new MODWTStreamingTransformImpl(
    new Haar(),
    BoundaryMode.PERIODIC,
    480 // Exactly 10ms at 48kHz - no padding needed!
);

stream.subscribe(result -> processResult(result));
stream.onNext(dataChunk);

// Streaming denoiser with shift-invariant processing
MODWTStreamingDenoiser streamDenoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(100) // Any size!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .build();

// Process continuous stream
for (double[] chunk : audioStream) {
    double[] denoised = streamDenoiser.denoise(chunk);
    // Shift-invariant denoising preserves signal timing
}
```

### Performance Configuration
```java
// MODWT performance optimization
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Get performance information
MODWTTransform.PerformanceInfo perfInfo = transform.getPerformanceInfo();
System.out.println(perfInfo.description()); // Shows if SIMD is being used

// Estimate processing time for planning
long estimatedNanos = transform.estimateProcessingTime(signalLength);

// Multi-level MODWT with optimal decomposition
MultiLevelMODWTTransform mlTransform = new MultiLevelMODWTTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC);
int maxLevels = mlTransform.getMaxDecompositionLevel(signalLength);

// Batch processing configuration for MODWT
MODWTOptimizedTransformEngine.EngineConfig config = 
    new MODWTOptimizedTransformEngine.EngineConfig()
        .withParallelism(8)
        .withSoALayout(true)
        .withSpecializedKernels(true);
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

The project includes comprehensive demos showcasing MODWT and other features:

### Core MODWT Functionality
- `Main` - Interactive menu system for all demos
- `MODWTDemo` - Comprehensive MODWT demonstrations with 7 examples
- `BasicUsageDemo` - Getting started with MODWT transforms
- `BoundaryModesDemo` - MODWT boundary handling (PERIODIC, ZERO_PADDING)
- `ScalarVsVectorDemo` - MODWT SIMD optimization comparison

### Financial Analysis
- `FinancialDemo` - Basic financial time series analysis
- `FinancialAnalysisDemo` - Advanced configurable analysis
- `FinancialOptimizationDemo` - Performance-optimized financial processing
- `LiveTradingSimulation` - Interactive trading bot simulation

### Advanced MODWT Features
- `SignalAnalysisDemo` - Time-frequency analysis with MODWT shift-invariance
- `StreamingDenoiserDemo` - Real-time MODWT denoising with arbitrary block sizes
- `OptimizationDemo` - MODWT performance optimization techniques
- `MemoryEfficiencyDemo` - MODWT memory advantages over DWT
- `MemoryPoolLifecycleDemo` - Memory management with MODWT
- `FactoryPatternDemo` - Factory pattern with MODWTTransformFactory
- `FFMDemo` - Memory efficiency focus with MODWT
- `FFMSimpleDemo` - Simple FFM concepts with MODWT

### CWT Demonstrations
- `CWTBasicDemo` - Continuous wavelet transform basics
- `CWTFFTOptimizationDemo` - FFT acceleration examples
- `MODWTBasedReconstructionDemo` - Fast reconstruction methods
- `ShannonWaveletComparisonDemo` - Shannon vs Shannon-Gabor comparison

### Streaming and Real-time Processing
- `StreamingDemo` - Real-time MODWT streaming examples
- `BatchProcessingDemo` - MODWT batch processing with SIMD
- `ComplexCWTDemo` - Complex wavelet analysis with phase
- `AdaptiveScaleDemo` - Automatic scale selection for CWT

Run demos with:
```bash
# Run all demos through interactive menu
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"

# Run specific demo
mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.LiveTradingSimulation"
```

## Known Issues

- **MODWT Boundary Modes**: SYMMETRIC mode is not supported by MODWT (only PERIODIC and ZERO_PADDING)
- **FFM Integration**: Full FFM integration with MODWT is planned for future releases
- **Demo Compilation**: Demo files are excluded from Maven compilation (in pom.xml) but can be run individually

## Requirements

- Java 23 or higher
- Maven 3.6+
- For SIMD: `--add-modules jdk.incubator.vector`
- For FFM: `--enable-native-access=ALL-UNNAMED`

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.

## Documentation

- [API Reference](docs/API.md) - Complete API documentation
- [Developer Guide](docs/guides/DEVELOPER_GUIDE.md) - Development guidelines and best practices
- [Architecture Overview](docs/ARCHITECTURE.md) - System design and architecture
- [Performance Guide](docs/performance/PERFORMANCE_SUMMARY.md) - Performance characteristics and benchmarks
- [FFM API Guide](docs/FFM_API.md) - Foreign Function & Memory API usage
- [Wavelet Selection Guide](docs/WAVELET_SELECTION.md) - Choosing the right wavelet

### Guides
- [Batch Processing](docs/guides/BATCH_PROCESSING.md) - SIMD batch processing guide
- [Denoising](docs/guides/DENOISING.md) - Signal denoising techniques
- [Financial Analysis](docs/guides/FINANCIAL_ANALYSIS.md) - Financial market analysis
- [Streaming](docs/guides/STREAMING.md) - Real-time streaming transforms

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.