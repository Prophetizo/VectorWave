# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

### Building the project
```bash
mvn clean compile
```

### Running the main demo
```bash
# Direct Java execution (recommended)
java -cp target/classes ai.prophetizo.Main

# Using Maven exec plugin (requires exec plugin configuration in pom.xml)
mvn exec:java -Dexec.mainClass="ai.prophetizo.Main"
```

### Packaging as JAR
```bash
mvn clean package
```

## Architecture Overview

VectorWave is a comprehensive Fast Wavelet Transform (FWT) library supporting multiple wavelet families through a flexible, type-safe architecture.

### Core Design Principles
- **Extensibility**: Supports orthogonal, biorthogonal, and continuous wavelets
- **Type-safe wavelet selection**: Sealed interface hierarchy ensures compile-time validation
- **Clean API separation**: Public API in `ai.prophetizo.wavelet.api`, internal implementations hidden
- **Zero dependencies**: Pure Java implementation
- **Performance-first design**: Multiple optimization paths (scalar, SIMD, cache-aware)
- **Memory efficiency**: Object pooling and aligned memory allocation
- **Streaming support**: Real-time processing with bounded memory usage
- **Flexible performance**: Dual implementations for different use cases

### Wavelet Type Hierarchy

```
Wavelet (sealed base interface)
├── DiscreteWavelet (for DWT)
│   ├── OrthogonalWavelet
│   │   ├── Haar
│   │   ├── Daubechies (DB2, DB4)
│   │   ├── Symlet (sym2, sym3, ...)
│   │   └── Coiflet (coif1, coif2, ...)
│   └── BiorthogonalWavelet
│       └── BiorthogonalSpline (bior1.3, ...)
└── ContinuousWavelet (for CWT)
    └── MorletWavelet
```

### Key Components

1. **WaveletTransform** (`wavelet/WaveletTransform.java`): Main transform engine
   - Handles forward and inverse transforms
   - Supports periodic and zero-padding boundary modes
   - Works with any wavelet type

2. **Wavelet Interfaces** (`wavelet/api/`):
   - `Wavelet`: Base sealed interface with core methods
   - `DiscreteWavelet`: Base for discrete wavelets with vanishing moments
   - `OrthogonalWavelet`: Wavelets where reconstruction = decomposition filters
   - `BiorthogonalWavelet`: Wavelets with dual filter pairs
   - `ContinuousWavelet`: Wavelets defined by mathematical functions

3. **WaveletRegistry** (`wavelet/api/WaveletRegistry.java`):
   - Central registry for all available wavelets
   - Lookup by name or type
   - Wavelet discovery functionality

4. **Transform Operations**:
   - `ScalarOps`: Core mathematical operations for wavelet transforms
   - `VectorOps` family: SIMD-optimized operations with platform-specific variants
   - `CacheAwareOps`: Cache-optimized operations for large signals
   - `SpecializedKernels`: Optimized implementations for common patterns
   - Handles convolution, downsampling, upsampling for multiple boundary modes

5. **Advanced Features**:
   - **WaveletDenoiser** (`wavelet/denoising/`): Signal denoising with multiple threshold methods
   - **MultiLevelWaveletTransform**: Multi-level decomposition and reconstruction
   - **StreamingWaveletTransform** (`wavelet/streaming/`): Real-time streaming support
   - **StreamingDenoiser** (`wavelet/streaming/`): Dual-implementation streaming denoiser
     - `FastStreamingDenoiser`: < 1 µs/sample latency for real-time
     - `QualityStreamingDenoiser`: Better SNR with overlapping transforms
     - Factory pattern with automatic implementation selection
   - **ParallelWaveletEngine** (`wavelet/concurrent/`): Parallel batch processing
   - **Memory Pools** (`wavelet/memory/`): Efficient memory management
   - **Padding Strategies** (`wavelet/padding/`): Flexible boundary handling

### Important Technical Notes
- Requires Java 21 or later
- Power-of-2 signal lengths required for transforms
- Supports both single-level and multi-level transforms
- No external dependencies (pure Java implementation)
- Continuous wavelets are discretized for DWT operations
- SIMD optimizations require `--add-modules jdk.incubator.vector`
- Platform-adaptive optimization thresholds:
  - Apple Silicon: Benefits from SIMD with signals ≥ 8 elements
  - x86 (AVX2/AVX512): Benefits from SIMD with signals ≥ 16-32 elements
  - ARM (general): Benefits from SIMD with signals ≥ 8 elements
- Streaming denoiser performance characteristics:
  - Fast implementation: 0.35-0.70 µs/sample, always real-time capable
  - Quality implementation: 0.2-11.4 µs/sample, real-time only without overlap
  - Automatic selection based on configuration parameters

### Adding New Wavelets

To add a new wavelet type:

1. **For Orthogonal wavelets**: Implement `OrthogonalWavelet` interface
2. **For Biorthogonal wavelets**: Implement `BiorthogonalWavelet` interface  
3. **For Continuous wavelets**: Implement `ContinuousWavelet` interface
4. Register in `WaveletRegistry` static initializer
5. Add comprehensive tests

Example:
```java
public record MyWavelet() implements OrthogonalWavelet {
    @Override
    public String name() { return "mywav"; }
    
    @Override
    public double[] lowPassDecomposition() { 
        return new double[]{...}; 
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate from low-pass using QMF
    }
    
    @Override
    public int vanishingMoments() { return 2; }
}
```

### Testing
JUnit 5 is configured for unit testing:
- Test files are located in `src/test/java/ai/prophetizo/`
- Run all tests: `mvn test`
- Run specific test: `mvn test -Dtest=TestClassName`
- Run tests with coverage: `mvn clean test jacoco:report`
- Coverage goal: >80% (currently achieved)
- Performance tests are disabled by default (annotated with `@Disabled`)
- Test categories:
  - Unit tests for all wavelet types and operations
  - Integration tests for complex scenarios
  - Property-based tests for mathematical verification
  - Performance regression tests

### Benchmarking
JMH (Java Microbenchmark Harness) is configured for accurate performance measurements:
- Benchmark classes are in `src/test/java/ai/prophetizo/wavelet/benchmark/`
- See `BENCHMARKING.md` for detailed instructions
- Run benchmarks using: `./jmh-runner.sh` or `./jmh-runner.sh BenchmarkName`
- **Important**: Always use JMH for performance measurements, not manual timing
- Available benchmarks:
  - `SignalSizeBenchmark`: Performance scaling with signal size
  - `WaveletTypeBenchmark`: Comparison across wavelet types
  - `ValidationBenchmark`: Validation overhead measurement
  - `ScalarVsVectorBenchmark`: SIMD vs scalar performance
  - `DenoisingBenchmark`: Denoising performance
  - `MultiLevelBenchmark`: Multi-level transform performance

### Continuous Integration
GitHub Actions are configured for:
- **CI Workflow** (`ci.yml`): Runs on all pushes and PRs
  - Multi-platform testing (Ubuntu, Windows, macOS)
  - Unit and integration tests
  - Code quality checks
  - Test result reporting and artifact upload
  - Coverage reporting with JaCoCo

### Lint and Type Checking Commands
When completing tasks, run these commands to ensure code quality:
```bash
# Check code style (if configured)
mvn checkstyle:check

# Run all tests
mvn test

# Generate and view coverage report
mvn clean test jacoco:report
# Coverage report will be in target/site/jacoco/index.html
```

### License
This project is licensed under the GNU General Public License v3.0.