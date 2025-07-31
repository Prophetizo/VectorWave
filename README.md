# VectorWave

High-performance wavelet transform library for Java 21+ with comprehensive wavelet family support, SIMD optimizations, and both Discrete (DWT) and Continuous (CWT) wavelet transforms.

## Features

### Core Capabilities
- **Multiple Wavelet Families**: Haar, Daubechies (DB2-DB20), Symlets, Coiflets, Biorthogonal, Morlet
- **Continuous Wavelet Transform (CWT)**: FFT-accelerated CWT with O(n log n) complexity
- **Complex Wavelet Analysis**: Full complex coefficient support with magnitude and phase
- **Financial Wavelets**: Specialized wavelets for market analysis
  - Paul wavelet: Asymmetric pattern detection (crashes, recoveries)
  - Shannon wavelet: Band-limited analysis
  - DOG wavelets: Gaussian derivatives for smooth features
  - MATLAB-compatible Mexican Hat: Legacy system integration
- **Type-Safe API**: Sealed interfaces with compile-time validation
- **Zero Dependencies**: Pure Java implementation
- **Flexible Boundary Handling**: Periodic, Zero, Symmetric, and Reflect padding modes

### Performance
- **SIMD Optimizations**: Platform-specific Vector API support with automatic fallback
  - x86: AVX2/AVX512 when available
  - ARM: NEON support, optimized for Apple Silicon
  - Graceful scalar fallback for unsupported platforms
- **FFT Acceleration**: O(n log n) convolution for CWT
  - Split-radix FFT for optimal performance
  - Bluestein algorithm for arbitrary sizes
  - Pre-computed twiddle factor caching
- **Cache-Aware Operations**: Platform-adaptive cache configuration
- **Adaptive Thresholds**: 8+ elements for ARM/Apple Silicon, 16+ for x86
- **Memory Efficiency**: Object pooling, aligned allocation, streaming memory management
- **Parallel Processing**: Fork-join framework for batch operations

### Advanced Features
- **Denoising**: Universal, SURE, and Minimax threshold methods
- **Streaming Support**: Real-time processing with Java Flow API
  - Fast mode: < 1 µs/sample latency
  - Quality mode: Enhanced SNR with overlap
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

## Requirements

- Java 21 or later
- Maven 3.6+
- Optional: Java Vector API support (included in Java 16+ as incubator module)

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
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
var crashResult = analyzer.detectMarketCrashes(priceData, threshold);

// FFT-accelerated CWT for large signals
CWTConfig config = CWTConfig.builder()
    .enableFFT(true)
    .normalizeScales(true)
    .build();
CWTTransform fftCwt = new CWTTransform(wavelet, config);

// Fast DWT-based CWT reconstruction (recommended)
DWTBasedInverseCWT dwtInverse = new DWTBasedInverseCWT(wavelet);
double[] reconstructed = dwtInverse.reconstruct(cwtResult);
// Works best with dyadic scales for optimal accuracy
```

### Wavelet Selection Guide

| Wavelet | Best For | Characteristics |
|---------|----------|-----------------|
| **Morlet** | General time-frequency analysis | Gaussian-modulated complex sinusoid, good frequency localization |
| **Paul** | Financial crash detection | Asymmetric, captures sharp rises/falls |
| **Mexican Hat (DOG2)** | Edge detection, volatility | Second derivative of Gaussian, zero crossings at edges |
| **DOG(n)** | Smooth feature extraction | Higher derivatives for smoother patterns |
| **Shannon** | Band-limited signals | Perfect frequency localization, poor time localization |
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
    .forceSIMD(true)  // or forceScalar(true)
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
- [Adding New Wavelets](docs/ADDING_WAVELETS.md)
- [Benchmarking Guide](docs/BENCHMARKING.md)
- [Financial Wavelets Guide](docs/FINANCIAL_WAVELETS.md)
- [Wavelet Normalization](docs/WAVELET_NORMALIZATION.md)
- [Wavelet Selection Guide](docs/WAVELET_SELECTION.md)

## Requirements

- Java 21 or higher
- Maven 3.6+
- For SIMD: `--add-modules jdk.incubator.vector`

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.