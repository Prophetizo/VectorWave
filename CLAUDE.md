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
- Vector API is incubating - expect warnings
- Use JMH for all performance measurements
- Coverage goal: >80% (excluding demos)

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