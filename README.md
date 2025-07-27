# VectorWave

High-performance Fast Wavelet Transform (FWT) library for Java 21+ with comprehensive wavelet family support and SIMD optimizations.

## Features

### Core Capabilities
- **Multiple Wavelet Families**: Haar, Daubechies (DB2-DB20), Symlets, Coiflets, Biorthogonal, Morlet
- **Type-Safe API**: Sealed interfaces with compile-time validation
- **Zero Dependencies**: Pure Java implementation
- **Flexible Boundary Handling**: Periodic, Zero, Symmetric, and Reflect padding modes

### Performance
- **SIMD Optimizations**: Platform-specific Vector API support (x86 AVX2/AVX512, ARM NEON, Apple Silicon)
- **Adaptive Thresholds**: 8+ elements for ARM/Apple Silicon, 16+ for x86
- **Memory Efficiency**: Object pooling, aligned allocation, cache-aware operations
- **Parallel Processing**: Fork-join framework for batch operations

### Advanced Features
- **Denoising**: Universal, SURE, and Minimax threshold methods
- **Streaming Support**: Real-time processing with Java Flow API
  - Fast mode: < 1 µs/sample latency
  - Quality mode: Enhanced SNR with overlap
- **Multi-level Transforms**: Configurable decomposition levels

## Quick Start

```bash
# Build
mvn clean compile

# Run tests
mvn test

# Run benchmarks
./jmh-runner.sh
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

### Streaming
```java
// Real-time streaming transform
StreamingWaveletTransform stream = StreamingWaveletTransform.create(
    Daubechies.DB4,
    BoundaryMode.PERIODIC,
    512 // block size
);

stream.subscribe(result -> processResult(result));
stream.process(dataChunk);

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
```

## Documentation

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Performance Guide](docs/PERFORMANCE.md)
- [API Reference](docs/API.md)
- [Adding New Wavelets](docs/ADDING_WAVELETS.md)
- [Benchmarking Guide](docs/BENCHMARKING.md)

## Requirements

- Java 21 or higher
- Maven 3.6+
- For SIMD: `--add-modules jdk.incubator.vector`

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.