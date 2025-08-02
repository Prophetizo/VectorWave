# VectorWave Documentation

Welcome to the VectorWave documentation. This directory contains comprehensive guides, API documentation, and technical specifications for the VectorWave wavelet transform library.

## Quick Links

- [Architecture Overview](ARCHITECTURE.md) - System design and components
- [API Reference](API.md) - Complete API documentation
- [Performance Guide](PERFORMANCE.md) - Optimization strategies and benchmarks
- [Adding Wavelets](ADDING_WAVELETS.md) - How to extend wavelet families
- [Benchmarking](BENCHMARKING.md) - Running and interpreting benchmarks (includes Vector API configuration)

## Guides

- [Denoising Guide](guides/DENOISING.md) - Signal denoising techniques
- [Streaming Guide](guides/STREAMING.md) - Real-time processing
- [Streaming Financial Analysis](guides/STREAMING_FINANCIAL_ANALYSIS.md) - Real-time financial applications
- [Financial Analysis](guides/FINANCIAL_ANALYSIS.md) - Financial metrics and wavelet analysis
- [Streaming Demo](guides/STREAMING_DEMO.md) - Interactive streaming demonstration
- [Wavelet Properties](guides/WAVELET_PROPERTIES.md) - Mathematical properties

## Performance Analysis

- [Optimization Summary](performance/OPTIMIZATIONS.md) - Implemented optimizations
- [Performance Results](performance/PERFORMANCE_SUMMARY.md) - Benchmark results
- [SIMD Analysis](performance/SIMD_OPTIMIZATION_ANALYSIS.md) - Vector API analysis
- [Performance Optimizations](performance/PERFORMANCE_OPTIMIZATIONS.md) - Optimization strategies
- [Complex Optimization Results](COMPLEX_OPTIMIZATION_RESULTS.md) - Complex number optimizations

## Technical Specifications

- [FFT Canonical References](FFT_CANONICAL_REFERENCES.md) - Academic references
- [FFT Mathematical Details](FFT_MATHEMATICAL_DETAILS.md) - Mathematical foundations
- [Modern Java Features](technical-specs/MODERN_JAVA_FEATURES.md) - Java 14-21+ features used
- [Vector API Fallback](technical-specs/VECTOR_API_FALLBACK.md) - Vector API graceful degradation
- [Ring Buffer Implementation](ring-buffer-implementation.md) - Zero-copy streaming

## Financial Analysis

- [Financial Wavelets](FINANCIAL_WAVELETS.md) - MATLAB compatibility guide
- [Financial Analysis Parameters](FINANCIAL_ANALYSIS_PARAMETERS.md) - Configuration guide
- [Wavelet Selection](WAVELET_SELECTION.md) - Choosing the right wavelet

## Continuous Wavelet Transform (CWT)

- [CWT Roadmap](CWT_ROADMAP.md) - Current status and future plans
- [CWT Reconstruction](CWT_RECONSTRUCTION.md) - DWT-based reconstruction
- [Wavelet Normalization](WAVELET_NORMALIZATION.md) - L¹ and L² normalization
- [Shannon Wavelets Comparison](SHANNON_WAVELETS_COMPARISON.md) - Shannon wavelet analysis

## Getting Started

1. Read the [Architecture Overview](ARCHITECTURE.md) to understand the system design
2. Check the [API Reference](API.md) for usage examples
3. Review the [Performance Guide](PERFORMANCE.md) for optimization tips
4. Run benchmarks using the [Benchmarking Guide](BENCHMARKING.md)

## For Contributors

- Follow patterns in [Architecture](ARCHITECTURE.md)
- Add tests for new features
- Run benchmarks before/after changes
- Update relevant documentation
- See [CLAUDE.md](../CLAUDE.md) for development guidelines

## Vector API Support

VectorWave includes optional Vector API support for enhanced performance:
- Automatically detects Vector API availability
- Falls back to scalar implementations when not available
- See [Benchmarking Guide](BENCHMARKING.md) for configuration details

## Additional Resources

- [Main README](../README.md) - Project overview and quick start
- [CHANGELOG](../CHANGELOG.md) - Version history and recent updates
- [Implementation Notes](implementation-notes/) - Technical implementation details
- [Release Notes](release-notes/) - Detailed release information