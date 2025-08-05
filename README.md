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

- Java 23+
- Maven 3.6+
- Compilation: `--add-modules jdk.incubator.vector`
- Runtime: Vector API optional (automatic scalar fallback)

## Quick Start

```bash
# Build and test
mvn clean test

# Run interactive demos
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"

# Basic MODWT usage
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[] signal = {1, 2, 3, 4, 5, 6, 7}; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

## Key Examples

### 1. Basic MODWT Transform
```java
// Works with ANY signal length - no power-of-2 restriction!
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result); // Perfect reconstruction
```

### 2. High-Performance Batch Processing
```java
// Process multiple signals with automatic SIMD optimization
double[][] signals = new double[32][1000]; // 32 signals of any length
MODWTResult[] results = transform.forwardBatch(signals); // 2-4x speedup
double[][] reconstructed = transform.inverseBatch(results);
```

### 3. Financial Analysis
```java
// Configure financial analysis parameters
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

// Wavelet-based Sharpe ratio calculation
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);

// Crash asymmetry detection using Paul wavelet
PaulWavelet paulWavelet = new PaulWavelet(4);
CWTTransform cwt = new CWTTransform(paulWavelet);
CWTResult crashAnalysis = cwt.analyze(priceReturns, scales);
```

### 4. Real-time Streaming
```java
// Streaming denoiser with arbitrary block sizes
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .bufferSize(480) // 10ms at 48kHz - no padding needed!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .build();

// Process continuous stream
for (double[] chunk : audioStream) {
    double[] denoised = denoiser.denoise(chunk);
}
```

### 5. Performance Monitoring
```java
// Check platform capabilities
WaveletOperations.PerformanceInfo info = WaveletOperations.getPerformanceInfo();
System.out.println(info.description());
// Output: "Vectorized operations enabled on aarch64 with S_128_BIT"

// Estimate processing time
MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
System.out.println(estimate.description());
```

## Wavelet Selection Guide

| Wavelet | Best For | Key Properties |
|---------|----------|----------------|
| **Haar** | Fast processing, edge detection | Simplest, compact support |
| **Daubechies** | General purpose, compression | Orthogonal, good frequency localization |
| **Paul** | Financial crash detection | Asymmetric, captures sharp rises/falls |
| **Morlet** | Time-frequency analysis | Complex, good time-frequency balance |
| **Mexican Hat** | Edge detection, volatility | Second derivative of Gaussian |
| **Shannon-Gabor** | Spectral analysis | Reduced artifacts vs classical Shannon |

## Documentation

- [API Reference](docs/API.md) - Complete API documentation  
- [Architecture Overview](docs/ARCHITECTURE.md) - System design
- [Performance Guide](docs/PERFORMANCE.md) - Performance characteristics
- [Wavelet Selection Guide](docs/WAVELET_SELECTION.md) - Choosing wavelets
- [Financial Analysis Guide](docs/guides/FINANCIAL_ANALYSIS.md) - Market analysis
- [Batch Processing Guide](docs/guides/BATCH_PROCESSING.md) - SIMD optimization

## Demos

Run interactive demos:
```bash
# All demos via menu
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"

# Specific demo
mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.LiveTradingSimulation"
```

**Core demos**: `MODWTDemo`, `FinancialDemo`, `StreamingDenoiserDemo`, `BatchProcessingDemo`

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.
