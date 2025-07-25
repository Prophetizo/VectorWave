# VectorWave

A comprehensive Fast Wavelet Transform (FWT) library for Java with support for multiple wavelet families and a clean, extensible architecture.

## Features

- **Multiple Wavelet Families**: 
  - Orthogonal: Haar, Daubechies (DB2, DB4), Symlets, Coiflets
  - Biorthogonal: Biorthogonal Spline wavelets
  - Continuous: Morlet wavelet (with discretization support)
- **Type-Safe API**: Sealed interface hierarchy ensures compile-time wavelet validation
- **Extensible Architecture**: Easy to add new wavelet types through well-defined interfaces
- **Zero Dependencies**: Pure Java implementation with no external dependencies
- **Flexible Padding Strategies**: 
  - Periodic, Zero, Symmetric, and Reflect padding modes
  - Strategy pattern for custom padding implementations
- **Performance Optimizations**: 
  - SIMD/Vector API support with platform-specific optimizations (x86, ARM, Apple Silicon)
  - Cache-aware operations for improved memory access patterns
  - Memory pooling (standard and SIMD-aligned) for reduced GC pressure
  - Specialized kernels for common operations
  - Minimum signal threshold: 8 elements for Apple Silicon, varies by platform
- **Mathematical Verification**: Built-in coefficient verification for all wavelets with documented sources
- **Advanced Signal Processing**:
  - Wavelet denoising with multiple threshold methods (Universal, SURE, Minimax)
  - Multi-level wavelet decomposition and reconstruction
  - Level-dependent analysis and thresholding
- **Streaming and Real-time Support**:
  - Reactive streams using Java Flow API
  - Sliding window transforms for continuous data
  - Multi-level streaming transforms
  - Real-time wavelet denoising with dual implementations:
    - Fast implementation: < 1 µs/sample latency for real-time applications
    - Quality implementation: Better SNR at the cost of higher latency
  - Factory pattern with automatic implementation selection
- **Parallel Processing**:
  - Fork-join based parallel engine for batch processing
  - Configurable parallelism with work-stealing
- **Production-Ready Features**:
  - Custom exception hierarchy for precise error handling
  - Thread-safe operations with atomic indexing
  - Comprehensive validation framework

## Requirements

- Java 21 or higher
- Maven 3.6+

### SIMD/Vector API Support

VectorWave includes advanced SIMD optimizations using Java's Vector API (incubator module) with multiple implementation variants:

**Implementation Variants**:
- **VectorOps**: Base SIMD implementation
- **VectorOpsOptimized**: Enhanced version with additional optimizations
- **VectorOpsARM**: ARM-specific optimizations for Apple Silicon
- **VectorOpsPooled**: Memory-pooled variant for reduced allocation overhead

**Configuration Options**:
- **Auto-detection** (default): System automatically chooses optimal path based on hardware
- **Force Scalar**: Use `TransformConfig.forceScalar(true)` for debugging or compatibility
- **Force SIMD**: Use `TransformConfig.forceSIMD(true)` for maximum performance

**Performance Characteristics**:
- Platform-adaptive thresholds:
  - Apple Silicon (M-series): Benefits from vectors with signals ≥ 8 elements
  - x86 (AVX2/AVX512): Benefits from vectors with signals ≥ 16-32 elements
  - ARM (general): Benefits from vectors with signals ≥ 8 elements
- 2-8x speedup on compatible hardware for larger signals
- Thread-safe with atomic indexing to prevent collisions
- Automatic fallback to scalar operations when beneficial

**Note**: When building the project, you may see warnings about "using incubating module(s)". This is expected and does not affect functionality. The Vector API is an incubating feature that will be finalized in a future JDK release.

## Quick Start

### Building the Project

```bash
mvn clean compile
```

### Running the Demos

VectorWave includes a comprehensive demo suite showcasing all features:

```bash
# Run the main demo (now includes streaming denoiser)
java -cp target/classes ai.prophetizo.Main

# Run specific demos (see src/main/java/ai/prophetizo/demo/)
java -cp target/classes ai.prophetizo.demo.BasicUsageDemo
java -cp target/classes ai.prophetizo.demo.WaveletSelectionGuideDemo
java -cp target/classes ai.prophetizo.demo.PerformanceOptimizationDemo
java -cp target/classes ai.prophetizo.demo.BoundaryModesDemo
java -cp target/classes ai.prophetizo.demo.SignalAnalysisDemo
java -cp target/classes ai.prophetizo.demo.MemoryEfficiencyDemo
java -cp target/classes ai.prophetizo.demo.FinancialOptimizationDemo

# Streaming demos
java -cp target/classes ai.prophetizo.demo.StreamingDenoiserDemo
java -cp target/classes ai.prophetizo.demo.StreamingDenoiserFactoryDemo

# Demos requiring Vector API support
java -cp target/classes --add-modules jdk.incubator.vector ai.prophetizo.demo.ScalarVsVectorDemo
java -cp target/classes --add-modules jdk.incubator.vector ai.prophetizo.demo.DenoisingDemo
java -cp target/classes --add-modules jdk.incubator.vector ai.prophetizo.demo.MultiLevelDemo
```

See [Demo Suite Documentation](src/main/java/ai/prophetizo/demo/README.md) for the complete list of available demos.

### Basic Usage

```java
import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;

// Using Haar wavelet
WaveletTransform transform = WaveletTransformFactory.createDefault(new Haar());
double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
TransformResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);

// Using Daubechies DB4
transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(Daubechies.DB4);
    
// Using Biorthogonal wavelet
transform = new WaveletTransformFactory()
    .create(BiorthogonalSpline.BIOR1_3);
    
// Using Morlet wavelet (continuous)
transform = new WaveletTransformFactory()
    .create(new MorletWavelet(6.0, 1.0));

// Configuring optimization paths
TransformConfig config = TransformConfig.builder()
    .forceScalar(true)  // or forceSIMD(true)
    .boundaryMode(BoundaryMode.PERIODIC)
    .build();
transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, config);
```

### Advanced Usage

#### Wavelet Denoising

```java
import ai.prophetizo.wavelet.denoising.*;

WaveletDenoiser denoiser = new WaveletDenoiser(new Daubechies.DB4());
double[] denoised = denoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);

// With custom threshold
double[] denoised = denoiser.denoiseFixed(noisySignal, 0.5, ThresholdType.SOFT);
```

#### Multi-Level Decomposition

```java
import ai.prophetizo.wavelet.MultiLevelWaveletTransform;

MultiLevelWaveletTransform mlTransform = new MultiLevelWaveletTransform(
    new Daubechies.DB4(), 
    BoundaryMode.PERIODIC
);
MultiLevelTransformResult mlResult = mlTransform.decompose(signal, 3); // 3 levels
double[] reconstructed = mlTransform.reconstruct(mlResult);
```

#### Streaming Transforms

```java
import ai.prophetizo.wavelet.streaming.*;

StreamingWaveletTransform streamTransform = new StreamingWaveletTransformImpl(
    new Haar(), 
    BoundaryMode.PERIODIC
);
// Process data as it arrives
streamTransform.getInputPublisher().submit(dataChunk);
```

#### Real-time Streaming Denoising

VectorWave provides a flexible streaming denoiser with two implementations optimized for different use cases:

```java
import ai.prophetizo.wavelet.streaming.*;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.*;

// Configuration for streaming denoiser
StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
    .wavelet(Daubechies.DB4)
    .blockSize(256)
    .overlapFactor(0.5)
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .adaptiveThreshold(true)
    .build();

// Option 1: Fast implementation for real-time processing
StreamingDenoiserStrategy fastDenoiser = StreamingDenoiserFactory.create(
    StreamingDenoiserFactory.Implementation.FAST, config);
// Performance: < 1 µs/sample, suitable for real-time audio/sensor data

// Option 2: Quality implementation for better denoising
StreamingDenoiserStrategy qualityDenoiser = StreamingDenoiserFactory.create(
    StreamingDenoiserFactory.Implementation.QUALITY, config);
// Performance: Better SNR (4.5 dB improvement), higher latency with overlap

// Option 3: Let the factory choose based on configuration
StreamingDenoiserStrategy autoDenoiser = StreamingDenoiserFactory.create(config);
// Automatically selects FAST or QUALITY based on parameters

// Use as a reactive stream
denoiser.subscribe(new Flow.Subscriber<double[]>() {
    public void onNext(double[] denoisedBlock) {
        // Process denoised output
    }
    // ... other subscriber methods
});

// Process streaming data
denoiser.process(samples);
```

**Performance Characteristics:**
- **Fast Implementation**: 
  - Latency: 0.35-0.70 µs/sample
  - Throughput: 1.37-2.69 million samples/second
  - Memory: ~22 KB per instance
  - Real-time capable: Always
- **Quality Implementation**:
  - Latency: 0.2 µs/sample (no overlap) to 11.4 µs/sample (75% overlap)
  - SNR: 4.5 dB better than Fast implementation
  - Memory: ~26 KB per instance
  - Real-time capable: Only without overlap

#### Parallel Batch Processing

```java
import ai.prophetizo.wavelet.concurrent.*;

ParallelWaveletEngine engine = new ParallelWaveletEngine(
    new Daubechies.DB4(), 
    4 // number of threads
);
List<TransformResult> results = engine.transformBatch(signals);
```

### Wavelet Registry

```java
// Discover available wavelets
Set<String> allWavelets = WaveletRegistry.getAvailableWavelets();
List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();

// Get wavelet by name
Wavelet db4 = WaveletRegistry.getWavelet("db4");
Wavelet haar = WaveletRegistry.getWavelet("haar");

// Print all available wavelets
WaveletRegistry.printAvailableWavelets();

// Verify mathematical properties
boolean isValidHaar = new Haar().verifyCoefficients();
boolean isValidDB4 = Daubechies.DB4.verifyCoefficients();
```

## Performance Benchmarking

VectorWave includes comprehensive JMH benchmarks to measure performance across different configurations.

### Available Benchmarks

1. **Signal Size Scaling**: Tests performance with different signal sizes (256-16384)
2. **Wavelet Type Comparison**: Compares performance of Haar, DB2, and DB4
3. **Validation Performance**: Measures overhead of input validation

### Running Benchmarks

```bash
# Run all benchmarks
./jmh-runner.sh

# Run specific benchmark
./jmh-runner.sh ValidationBenchmark

# Run with custom parameters
./jmh-runner.sh SignalSizeBenchmark -wi 5 -i 10 -p signalSize=1024
```

### Benchmark Parameters

- `-wi N`: Warmup iterations (default: 5)
- `-i N`: Measurement iterations (default: 10)
- `-f N`: Fork count (default: 2)
- `-t N`: Thread count (default: 1)
- `-p param=value`: Override @Param values

## Architecture

### Wavelet Type Hierarchy

```
Wavelet (sealed interface)
├── DiscreteWavelet
│   ├── OrthogonalWavelet
│   │   ├── Haar
│   │   ├── Daubechies (DB2, DB4, ...)
│   │   ├── Symlet (sym2, sym3, ...)
│   │   └── Coiflet (coif1, coif2, ...)
│   └── BiorthogonalWavelet
│       └── BiorthogonalSpline (bior1.3, bior2.2, ...)
└── ContinuousWavelet
    └── MorletWavelet
```

### Core Components

- **WaveletTransform**: Main entry point for transforms
- **WaveletTransformFactory**: Factory for creating configured transforms
- **Wavelet Interfaces**: Type-safe hierarchy for different wavelet families
- **WaveletRegistry**: Central registry for wavelet discovery and creation
- **TransformResult**: Immutable container for transform coefficients
- **WaveletDenoiser**: Signal denoising with various threshold methods
- **MultiLevelWaveletTransform**: Multi-level decomposition and reconstruction
- **StreamingWaveletTransform**: Real-time streaming transforms
- **StreamingDenoiserStrategy**: Interface for streaming denoiser implementations
- **StreamingDenoiserFactory**: Factory for creating optimized denoiser instances
- **FastStreamingDenoiser**: Low-latency implementation for real-time processing
- **QualityStreamingDenoiser**: Higher quality denoising with overlapping transforms
- **ParallelWaveletEngine**: Parallel batch processing
- **Memory Management**: MemoryPool and AlignedMemoryPool for efficient memory usage
- **Operation Implementations**:
  - **ScalarOps**: Core mathematical operations
  - **VectorOps**: SIMD-optimized operations with platform variants
  - **CacheAwareOps**: Cache-optimized operations
  - **SpecializedKernels**: Optimized kernels for specific operations

### Package Structure

```
ai.prophetizo.wavelet/
├── api/                     # Public API interfaces
│   ├── Wavelet             # Base wavelet interface
│   ├── DiscreteWavelet     # Discrete wavelets base
│   ├── OrthogonalWavelet   # Orthogonal wavelets
│   ├── BiorthogonalWavelet # Biorthogonal wavelets
│   ├── ContinuousWavelet   # Continuous wavelets
│   ├── WaveletType         # Wavelet categorization
│   ├── WaveletRegistry     # Wavelet discovery
│   ├── BoundaryMode        # Boundary handling
│   └── [Wavelet implementations]
├── concurrent/              # Parallel processing
│   └── ParallelWaveletEngine
├── config/                  # Configuration
│   └── TransformConfig     # Transform configuration
├── denoising/               # Signal denoising
│   ├── WaveletDenoiser     # Main denoising class
│   ├── ThresholdMethod     # Threshold selection methods
│   └── ThresholdType       # Soft/hard thresholding
├── internal/                # Internal implementation
│   ├── ScalarOps           # Core scalar operations
│   ├── VectorOps           # SIMD operations (base)
│   ├── VectorOpsOptimized  # Enhanced SIMD
│   ├── VectorOpsARM        # ARM-specific SIMD
│   ├── VectorOpsPooled     # Pooled SIMD
│   ├── CacheAwareOps       # Cache-optimized ops
│   ├── SpecializedKernels  # Optimized kernels
│   ├── GatherScatterOps    # Gather/scatter ops
│   ├── PrefetchOptimizer   # Prefetch optimization
│   ├── SoATransform        # Structure of Arrays
│   └── ArrayPool           # Array pooling
├── memory/                  # Memory management
│   ├── MemoryPool          # Basic memory pool
│   └── AlignedMemoryPool   # SIMD-aligned pool
├── padding/                 # Padding strategies
│   ├── PaddingStrategy     # Strategy interface
│   ├── PeriodicPaddingStrategy
│   ├── ZeroPaddingStrategy
│   ├── SymmetricPaddingStrategy
│   └── ReflectPaddingStrategy
├── streaming/               # Streaming support
│   ├── StreamingWaveletTransform
│   ├── StreamingWaveletTransformImpl
│   ├── SlidingWindowTransform
│   ├── MultiLevelStreamingTransform
│   ├── StreamingDenoiserStrategy      # Denoiser interface
│   ├── StreamingDenoiserFactory       # Factory pattern
│   ├── StreamingDenoiserConfig        # Shared configuration
│   ├── FastStreamingDenoiser          # Real-time implementation
│   ├── QualityStreamingDenoiser       # Quality-focused implementation
│   ├── OverlapBuffer                  # Overlap-add processing
│   ├── NoiseEstimator                 # Adaptive noise estimation
│   └── StreamingThresholdAdapter      # Adaptive thresholding
├── util/                    # Utilities
│   ├── ValidationUtils     # Input validation
│   └── BatchValidation     # Batch validation
└── exception/              # Custom exceptions
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=WaveletTransformTest

# Run with coverage
mvn clean test jacoco:report
```

### Test Coverage

The project maintains >80% code coverage with comprehensive tests for:
- Transform correctness
- Boundary conditions
- Error handling
- Edge cases

## Continuous Integration

The project includes GitHub Actions workflows for:
- Automated testing on push/PR
- Code coverage reporting
- Build verification

## Documentation

- [BENCHMARKING.md](BENCHMARKING.md) - Detailed benchmarking guide and performance analysis
- [ADDING_WAVELETS.md](ADDING_WAVELETS.md) - Guide for adding new wavelet types
- [CLAUDE.md](CLAUDE.md) - Development guidelines for AI assistants
- [WAVELET_PROPERTIES.md](WAVELET_PROPERTIES.md) - Mathematical properties and sources for all wavelets
- [IMPROVEMENTS.md](IMPROVEMENTS.md) - Planned improvements and feature roadmap
- [OPTIMIZATIONS.md](OPTIMIZATIONS.md) - Detailed optimization strategies
- [PERFORMANCE_SUMMARY.md](PERFORMANCE_SUMMARY.md) - Performance benchmark results
- [SIMD_OPTIMIZATION_ANALYSIS.md](SIMD_OPTIMIZATION_ANALYSIS.md) - SIMD optimization analysis
- [STREAMING_FINANCIAL_ANALYSIS.md](STREAMING_FINANCIAL_ANALYSIS.md) - Streaming for financial data
- JavaDoc - Run `mvn javadoc:javadoc` to generate API documentation

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass and coverage remains >80%
5. Submit a pull request

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.


## Acknowledgments

- Wavelet coefficients based on peer-reviewed mathematical sources (see WAVELET_PROPERTIES.md)
- Benchmarking powered by JMH (Java Microbenchmark Harness)