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

### Running demo applications
```bash
# Main comprehensive demo
java -cp target/classes ai.prophetizo.Main

# Error handling demo
java -cp target/classes ai.prophetizo.demo.ErrorHandlingDemo

# Financial optimization demo
java -cp target/classes ai.prophetizo.demo.FinancialOptimizationDemo

# General optimization demo
java -cp target/classes ai.prophetizo.demo.OptimizationDemo

# CWT performance demo
java -cp target/classes ai.prophetizo.demo.cwt.CWTPerformanceDemo

# IFFT demo
java -cp target/classes ai.prophetizo.demo.IFFTDemo
```

## Architecture Overview

VectorWave is a comprehensive Fast Wavelet Transform (FWT) library supporting multiple wavelet families through a flexible, type-safe architecture.

### Core Design Principles
- **Extensibility**: Supports orthogonal, biorthogonal, and continuous wavelets
- **Type-safe wavelet selection**: Sealed interface hierarchy ensures compile-time validation
- **Clean API separation**: Public API in `ai.prophetizo.wavelet.api`, internal implementations hidden
- **Zero dependencies**: Pure Java implementation

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
   - Handles convolution, downsampling, upsampling for both boundary modes

5. **Demo Applications** (`demo/`):
   - `ErrorHandlingDemo`: Demonstrates error handling and validation
   - `FinancialOptimizationDemo`: Financial signal processing and optimization
   - `OptimizationDemo`: General wavelet optimization techniques
   - `IFFTDemo`: Inverse FFT demonstrations
   - `demo/cwt/CWTPerformanceDemo`: CWT performance analysis and benchmarking

### Important Technical Notes
- Requires Java 17 or later (updated for compatibility)
- Power-of-2 signal lengths required for transforms
- Currently implements single-level transforms only
- No external dependencies (pure Java implementation)
- Continuous wavelets are discretized for DWT operations

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
- Performance tests are disabled by default (annotated with `@Disabled`)
- Example: `ValidationUtilsTest` covers all validation scenarios

### Benchmarking
JMH (Java Microbenchmark Harness) is configured for accurate performance measurements:
- Benchmark classes are in `src/test/java/ai/prophetizo/wavelet/benchmark/`
- See `BENCHMARKING.md` for detailed instructions
- Run benchmarks using: `./jmh-runner.sh` or `./jmh-runner.sh BenchmarkName`
- **Important**: Always use JMH for performance measurements, not manual timing

### Continuous Integration
GitHub Actions are configured for:
- **CI Workflow** (`ci.yml`): Runs on all pushes and PRs
  - Multi-platform testing (Ubuntu, Windows, macOS)
  - Unit and integration tests
  - Code quality checks
  - Test result reporting and artifact upload

### License
This project is licensed under the GNU General Public License v3.0.