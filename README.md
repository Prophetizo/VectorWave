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
- **Boundary Modes**: Supports periodic and zero-padding boundary handling
- **Performance**: Optimized for small signals (<1024 samples) with integrated performance enhancements for financial time series analysis

## Requirements

- Java 21 or higher
- Maven 3.6+

## Quick Start

### Building the Project

```bash
mvn clean compile
```

### Running the Demo

```bash
java -cp target/classes ai.prophetizo.Main
```

### Basic Usage

```java
import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;

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
- **ScalarOps**: Core mathematical operations

### Package Structure

```
ai.prophetizo.wavelet/
├── api/                    # Public API interfaces
│   ├── Wavelet            # Base wavelet interface
│   ├── DiscreteWavelet    # Discrete wavelets base
│   ├── OrthogonalWavelet  # Orthogonal wavelets
│   ├── BiorthogonalWavelet# Biorthogonal wavelets
│   ├── ContinuousWavelet  # Continuous wavelets
│   ├── WaveletType        # Wavelet categorization
│   ├── WaveletRegistry    # Wavelet discovery
│   ├── BoundaryMode       # Boundary handling
│   └── [Wavelet implementations]
├── internal/               # Internal implementation
│   └── ScalarOps          # Core operations
├── util/                   # Utilities
│   ├── ValidationUtils    # Input validation
│   └── BatchValidation    # Batch validation
└── exception/             # Custom exceptions
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

- [BENCHMARKING.md](BENCHMARKING.md) - Detailed benchmarking guide
- [ADDING_WAVELETS.md](ADDING_WAVELETS.md) - Guide for adding new wavelet types
- [CLAUDE.md](CLAUDE.md) - Development guidelines for AI assistants
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

- Wavelet coefficients based on standard mathematical definitions
- Benchmarking powered by JMH (Java Microbenchmark Harness)