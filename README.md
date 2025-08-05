# VectorWave

High-performance wavelet transform library for Java 23+ featuring MODWT (Maximal Overlap Discrete Wavelet Transform) as the primary transform. Offers shift-invariance, arbitrary signal length support, SIMD acceleration, and comprehensive wavelet families for financial analysis, signal processing, and real-time applications.

## Features

### Core Capabilities
- **MODWT**: Shift-invariant transform for ANY signal length
- **Wavelet Families**: Haar, Daubechies, Symlets, Coiflets, Biorthogonal, Financial wavelets
- **CWT**: FFT-accelerated continuous transforms with complex analysis
- **SIMD Acceleration**: Automatic vectorization with scalar fallback
- **Financial Analysis**: Specialized wavelets and configurable parameters
- **Streaming**: Real-time processing with arbitrary block sizes
- **Zero Dependencies**: Pure Java 23+ implementation

### Performance
- **SIMD Acceleration**: Platform-adaptive Vector API (x86: AVX2/AVX512, ARM: NEON)
- **FFT Optimization**: Real-to-complex FFT with 2x speedup for real signals
- **Batch Processing**: SIMD parallel processing of multiple signals (2-4x speedup)
- **Memory Efficiency**: Object pooling and aligned allocation
- **Automatic Optimization**: No manual configuration required

### Key Applications
- **Financial Analysis**: Crash detection, volatility analysis, regime identification
- **Signal Processing**: Denoising, time-frequency analysis, feature extraction
- **Real-time Systems**: Streaming transforms with microsecond latency
- **Scientific Computing**: Multi-level decomposition and reconstruction

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

### Using the WaveletOperations Facade
```java
// Query platform capabilities
WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
System.out.println(perfInfo.description());

// Direct MODWT operations with automatic optimization
double[] signal = generateSignal(1000);
double[] filter = {0.7071, 0.7071}; // Haar low-pass
double[] output = new double[signal.length];

// Circular convolution for MODWT (periodic boundary)
WaveletOperations.circularConvolveMODWT(signal, filter, output);

// Zero-padding convolution for MODWT
WaveletOperations.zeroPaddingConvolveMODWT(signal, filter, output);

// Denoising operations with automatic SIMD optimization
double[] coefficients = getWaveletCoefficients();
double threshold = 0.5;
double[] softThresholded = WaveletOperations.softThreshold(coefficients, threshold);
double[] hardThresholded = WaveletOperations.hardThreshold(coefficients, threshold);
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

// Creating custom MODWT results with factory method
double[] modifiedDetail = processDetailCoefficients(detail);
MODWTResult customResult = MODWTResult.create(approx, modifiedDetail);
double[] customReconstructed = modwt.inverse(customResult);
```

### Batch Processing
```java
// Process multiple signals in parallel with automatic SIMD optimization
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = generateSignals(32, 250); // 32 signals of any length!

// Batch forward transform - automatically uses SIMD when beneficial
MODWTResult[] results = transform.forwardBatch(signals);

// Batch inverse transform
double[][] reconstructed = transform.inverseBatch(results);

// The transform automatically selects the best optimization strategy based on:
// - Signal length and count
// - Platform capabilities (ARM/x86, vector width)
// - Wavelet type (specialized kernels for Haar, etc.)
// - Memory alignment and cache considerations
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
// Basic denoising with WaveletDenoiser
WaveletDenoiser denoiser = new WaveletDenoiser.Builder()
    .withWavelet(Daubechies.DB4)
    .withThresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
    .withSoftThresholding(true)
    .build();

double[] denoised = denoiser.denoise(noisySignal);

// The denoiser automatically uses optimized operations via WaveletOperations
// No need to manually select scalar vs SIMD implementations

// MODWT-based streaming denoiser (works with any signal length!)
MODWTStreamingDenoiser streamingDenoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(333) // Any size - no padding needed!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
    .build();

// Process streaming data
double[] denoisedBlock = streamingDenoiser.denoise(noisyBlock);
double noiseLevel = streamingDenoiser.getEstimatedNoiseLevel();

// Subscribe to denoised output stream
streamingDenoiser.subscribe(new Flow.Subscriber<double[]>() {
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

// FFT-accelerated CWT with automatic optimizations
CWTConfig config = CWTConfig.builder()
    .enableFFT(true)           // Enable FFT acceleration
    .normalizeScales(true)
    .build();
CWTTransform fftCwt = new CWTTransform(wavelet, config);
// Real-to-complex optimization is automatically applied for real signals

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
Factory<MODWTTransform, MODWTTransformFactory.Config> transformFactory = 
    registry.getFactory("modwtTransform", MODWTTransform.class, MODWTTransformFactory.Config.class)
        .orElseThrow(() -> new IllegalStateException("Factory not found"));

MODWTTransform transform = transformFactory.create(
    new MODWTTransformFactory.Config(new Haar(), BoundaryMode.PERIODIC));

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

## Simplified API

VectorWave 4.0+ provides a simplified public API that hides internal implementation details:

- **WaveletOperations**: Public facade for core wavelet operations with automatic optimization
- **MODWTResult.create()**: Factory method for creating custom results
- **No power-of-2 restrictions**: MODWT works with signals of any length
- **Automatic optimization**: The library automatically selects the best implementation (scalar/SIMD)
- **Clean package structure**: Internal classes are package-private

### Migration from Previous Versions

#### Removed Classes
- `WaveletOpsFactory` → Use `WaveletOperations` facade instead
- `OptimizedTransformEngine` → Optimizations are now automatic
- `BatchSIMDTransform` → Use `MODWTTransform.forwardBatch()` instead
- All DWT-specific classes → Use MODWT equivalents

#### Updated Patterns
```java
// Old: Direct instantiation
MODWTResult result = new MODWTResultImpl(approx, detail);

// New: Factory method
MODWTResult result = MODWTResult.create(approx, detail);

// Old: Manual optimization selection
if (useOptimized) {
    engine = new OptimizedTransformEngine();
}

// New: Automatic optimization
// Just use MODWTTransform - it optimizes automatically

// Old: Power-of-2 padding
if (!isPowerOfTwo(signal.length)) {
    signal = padToPowerOfTwo(signal);
}

// New: No padding needed
// MODWT works with any signal length
```

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

// MODWT transforms automatically optimize based on:
// - Signal characteristics (length, batch size)
// - Platform capabilities (ARM vs x86, vector width)
// - Available system resources (cores, memory)
// No manual configuration needed for optimal performance!
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
- `FinancialAnalysisDemo` - Advanced configurable analysis with custom parameters
- `LiveTradingSimulation` - Interactive trading bot simulation

### Advanced MODWT Features
- `SignalAnalysisDemo` - Time-frequency analysis with MODWT shift-invariance
- `StreamingDenoiserDemo` - Real-time MODWT denoising with arbitrary block sizes
- `PerformanceDemo` - MODWT performance characteristics and benchmarking
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

## Quick Examples

### 1. Basic Signal Processing
```java
// Transform with any signal length - no padding needed!
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length works
MODWTResult result = transform.forward(signal);

// Perfect reconstruction
double[] reconstructed = transform.inverse(result);
```

### 2. Real-Time Denoising
```java
// Denoise a noisy signal
WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
double[] clean = denoiser.denoise(noisySignal, 
    WaveletDenoiser.ThresholdMethod.UNIVERSAL,
    WaveletDenoiser.ThresholdType.SOFT);
```

### 3. Financial Analysis
```java
// Analyze market data - no defaults, explicit configuration
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);
```

### 4. High-Performance Batch Processing
```java
// Process 32 signals in parallel with automatic SIMD
double[][] signals = new double[32][1000];
MODWTResult[] results = transform.forwardBatch(signals);
```

### 5. Platform Capabilities
```java
// Query what optimizations are available
WaveletOperations.PerformanceInfo info = WaveletOperations.getPerformanceInfo();
System.out.println(info.description());
// Output: "Vectorized operations enabled on aarch64 with S_128_BIT"
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.