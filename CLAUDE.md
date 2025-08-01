# CLAUDE.md

Development guide for Claude Code when working with the VectorWave repository.

## Build Commands

```bash
# Build
mvn clean compile

# Test
mvn test
mvn test -Dtest=TestClassName

# Coverage
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html

# Package
mvn clean package

# Run main demo
java -cp target/classes ai.prophetizo.Main

# Check Vector API availability
java -cp target/classes ai.prophetizo.wavelet.util.OptimizedFFT
```

## Architecture

### Package Structure
- `ai.prophetizo.wavelet.api` - Public API interfaces
- `ai.prophetizo.wavelet.internal` - SIMD and optimization implementations
- `ai.prophetizo.wavelet.wavelets` - Wavelet family implementations
- `ai.prophetizo.wavelet.streaming` - Real-time processing
- `ai.prophetizo.wavelet.denoising` - Signal denoising
- `ai.prophetizo.wavelet.concurrent` - Parallel processing
- `ai.prophetizo.wavelet.cwt` - Continuous Wavelet Transform
- `ai.prophetizo.wavelet.cwt.finance` - Financial analysis wavelets

### Key Design Patterns
- **Sealed Interfaces**: Type-safe wavelet hierarchy
- **Factory Pattern**: Transform and denoiser creation
  - Common `Factory<T, C>` interface for all factories
  - Static factory methods for convenience
  - Instance-based factory pattern via `getInstance()`
- **Strategy Pattern**: Boundary modes, threshold methods
- **Builder Pattern**: Configuration objects

### Performance Considerations
- Signal length must be power of 2
- SIMD benefits: ≥8 elements (ARM), ≥16 elements (x86)
- Use `TransformConfig` to control optimization paths
- Memory pools reduce GC pressure

## Testing

### Test Categories
- Unit tests: All wavelet types and operations
- Integration tests: Complex scenarios (`*IntegrationTest`)
- Performance tests: Disabled by default (`@Disabled`)
- Benchmarks: JMH in `src/test/java/.../benchmark/`

### Running Benchmarks
```bash
./jmh-runner.sh                    # All benchmarks
./jmh-runner.sh BenchmarkName      # Specific benchmark
```

## Adding New Features

### New Wavelet
1. Implement appropriate interface (`OrthogonalWavelet`, `BiorthogonalWavelet`, `ContinuousWavelet`)
2. Register in `WaveletRegistry`
3. Add tests in `src/test/java/.../wavelets/`
4. Update documentation

### New Optimization
1. Extend `VectorOps` or create specialized kernel
2. Add to `WaveletOpsFactory` selection logic
3. Benchmark against existing implementations
4. Document platform requirements

## CI/CD

GitHub Actions workflow (`ci.yml`):
- Multi-platform testing (Ubuntu, Windows, macOS)
- Code quality checks
- Coverage reporting
- Test artifacts upload

## Important Notes

- Java 21+ required (CI currently uses Java 21)
- Vector API is incubating - expect warnings but graceful fallback is implemented
- Use JMH for all performance measurements
- Coverage goal: >80% (excluding demos)
- Vector API automatically falls back to scalar operations when not available

## Factory Pattern

All major components use a common `Factory<T, C>` interface for consistent creation patterns:

### Available Factories
- **WaveletTransformFactory**: Creates `WaveletTransform` instances
  - Direct implementation of `Factory<WaveletTransform, Wavelet>`
  - Configurable boundary modes
- **CWTFactory**: Creates `CWTTransform` instances  
  - Access via `CWTFactory.getInstance()`
  - Static methods for convenience
- **StreamingDenoiserFactory**: Creates `StreamingDenoiser` instances
  - Access via `StreamingDenoiserFactory.getInstance()`
  - Automatic implementation selection based on overlap
- **WaveletOpsFactory**: Creates optimized `WaveletOps` implementations
  - Access via `WaveletOpsFactory.getInstance()`
  - Platform-adaptive SIMD selection

### Usage Patterns
```java
// Direct factory usage
WaveletTransformFactory factory = new WaveletTransformFactory();
WaveletTransform transform = factory.create(new Haar());

// Static factory pattern
CWTTransform cwt = CWTFactory.create(new MorletWavelet());

// Factory interface pattern
Factory<CWTTransform, ContinuousWavelet> cwtFactory = CWTFactory.getInstance();
CWTTransform cwt2 = cwtFactory.create(new MorletWavelet());

// Factory registry
FactoryRegistry registry = FactoryRegistry.getInstance();
registry.register("myFactory", factory);
Optional<Factory<?, ?>> retrieved = registry.getFactory("myFactory");
```

## Common Tasks

### Debugging SIMD
```java
// Force scalar mode
TransformConfig.builder().forceScalar(true).build()

// Check Vector API info
VectorOps.getVectorInfo()
```

### Memory Profiling
```java
// Use pooled variants
WaveletTransformPool pool = new WaveletTransformPool(wavelet, mode);
```

### Streaming Performance
```java
// Check latency requirements
StreamingDenoiserFactory.create(wavelet, method, blockSize, overlap)
// overlap < 0.3 → FastStreamingDenoiser
// overlap ≥ 0.3 → QualityStreamingDenoiser
```

## CWT Implementation

### Key CWT Components
- **CWTTransform**: Main engine with FFT acceleration
- **ComplexCWTResult**: Complex coefficients with phase/magnitude
- **Adaptive Scale Selection**: Automatic scale optimization
  - `DyadicScaleSelector`: Powers-of-2 scales
  - `SignalAdaptiveScaleSelector`: Energy-based placement
  - `OptimalScaleSelector`: Mathematical spacing strategies

### CWT Usage
```java
// Basic CWT
MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
CWTTransform cwt = new CWTTransform(wavelet);

// Automatic scale selection
SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
double[] scales = selector.selectScales(signal, wavelet, samplingRate);

// Complex analysis
ComplexCWTResult result = cwt.analyzeComplex(signal, scales);
double[][] phase = result.getPhase();
double[][] instFreq = result.getInstantaneousFrequency();

// Financial analysis with configurable parameters
FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(15.0)  // More sensitive for volatile markets
    .volatilityThresholds(0.3, 1.2, 2.5)  // Custom volatility bands
    .build();

FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(params);
var crashes = analyzer.detectMarketCrashes(priceData, samplingRate);
```

### CWT Performance Tips
- Use FFT acceleration for signals > 256 samples
- Dyadic scales optimize FFT performance
- Signal-adaptive selection reduces computation by 30-50%
- Complex analysis adds ~20% overhead vs real-only

## Foreign Function & Memory API (FFM)

The `ai.prophetizo.wavelet.memory.ffm` package provides high-performance implementations using Java 23's FFM API:

### Key Components
- **FFMWaveletTransform**: Drop-in replacement for WaveletTransform with better memory efficiency
- **FFMMemoryPool**: Thread-safe, SIMD-aligned memory pool
- **FFMStreamingTransform**: Zero-copy streaming implementation
- **FFMArrayAllocator**: Low-level memory management utilities

### Usage
```java
// Basic usage
try (FFMWaveletTransform transform = new FFMWaveletTransform(new Haar())) {
    TransformResult result = transform.forward(signal);
}

// Shared memory pool
try (FFMMemoryPool pool = new FFMMemoryPool()) {
    FFMWaveletTransform t1 = new FFMWaveletTransform(wavelet1, pool);
    FFMWaveletTransform t2 = new FFMWaveletTransform(wavelet2, pool);
}

// Scoped memory
double[] result = FFMMemoryPool.withScope(pool -> {
    FFMWaveletTransform transform = new FFMWaveletTransform(wavelet, pool);
    return transform.forwardInverse(signal);
});
```

### Running with FFM
```bash
# Compile with FFM support
mvn clean compile

# Run with required JVM flags
java --enable-native-access=ALL-UNNAMED \
     --add-modules=jdk.incubator.vector \
     -cp target/classes ai.prophetizo.demo.FFMWaveletDemo
```

### Performance Benefits
- 2-4x speedup for large signals (≥4096 samples)
- 90%+ memory pool hit rate after warm-up
- Zero GC pressure for off-heap allocations
- SIMD-aligned memory for optimal vectorization