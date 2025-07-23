# VectorWave Demo Suite

This directory contains comprehensive demonstrations of the VectorWave wavelet transform library's capabilities. Each
demo is a standalone Java application that showcases specific features and best practices.

## Running the Demos

To run any demo, first compile the project:

```bash
mvn clean compile
```

Then run a specific demo:

```bash
java -cp target/classes ai.prophetizo.demo.DemoName
```

## Available Demos

### 1. BasicUsageDemo

**Purpose**: Introduction to fundamental VectorWave operations

**What it covers**:

- Creating wavelet transforms
- Forward and inverse transforms
- Using the factory pattern
- Working with different signal lengths
- Error handling best practices
- Coefficient analysis and manipulation

**Run**: `java -cp target/classes ai.prophetizo.demo.BasicUsageDemo`

### 2. BoundaryModesDemo

**Purpose**: Understanding boundary handling in wavelet transforms

**What it covers**:

- Effects of PERIODIC vs ZERO_PADDING modes
- Edge artifact analysis
- Reconstruction accuracy comparison
- Mode selection guidelines
- Real-world application scenarios

**Run**: `java -cp target/classes ai.prophetizo.demo.BoundaryModesDemo`

### 3. PerformanceOptimizationDemo

**Purpose**: Maximizing transform performance

**What it covers**:

- SIMD vectorization benefits
- Memory pooling strategies
- Batch processing techniques
- Parallel processing patterns
- Cache-friendly access patterns
- Performance comparison across wavelets

**Run**: `java -cp target/classes ai.prophetizo.demo.PerformanceOptimizationDemo`

### 4. WaveletSelectionGuideDemo

**Purpose**: Choosing the right wavelet for your application

**What it covers**:

- Characteristics of each wavelet family
- Matching wavelets to signal types
- Application-specific recommendations
- Performance vs accuracy trade-offs
- Interactive decision flowchart

**Run**: `java -cp target/classes ai.prophetizo.demo.WaveletSelectionGuideDemo`

### 5. SignalAnalysisDemo

**Purpose**: Advanced signal analysis using wavelets

**What it covers**:

- Time-frequency analysis
- Feature extraction for machine learning
- Anomaly detection techniques
- Signal classification
- Pattern recognition

**Run**: `java -cp target/classes ai.prophetizo.demo.SignalAnalysisDemo`

### 6. MemoryEfficiencyDemo

**Purpose**: Memory-efficient usage patterns

**What it covers**:

- Memory pooling benefits
- In-place transformations
- Streaming with minimal memory footprint
- GC-friendly coding patterns
- Large dataset handling strategies

**Run**: `java -cp target/classes ai.prophetizo.demo.MemoryEfficiencyDemo`

### 7. MultiLevelDemo

**Purpose**: Multi-level wavelet decomposition

**What it covers**:

- Full and partial decomposition
- Energy distribution analysis
- Adaptive decomposition
- Multi-scale volatility analysis
- Financial time series analysis

**Run**: `java -cp target/classes ai.prophetizo.demo.MultiLevelDemo`

### 8. DenoisingDemo

**Purpose**: Signal denoising using wavelets

**What it covers**:

- Different threshold methods (Universal, SURE, Minimax)
- Soft vs hard thresholding
- Multi-level denoising
- Financial data denoising
- Performance benchmarks

**Run**: `java -cp target/classes ai.prophetizo.demo.DenoisingDemo`

### 9. StreamingDemo

**Purpose**: Real-time streaming transforms

**What it covers**:

- Block-based streaming
- Sliding window analysis
- Real-time audio simulation
- Financial tick data streaming
- Backpressure handling

**Run**: `java -cp target/classes ai.prophetizo.demo.StreamingDemo`

## Demo Categories by Use Case

### Getting Started

- BasicUsageDemo - Start here for fundamentals
- WaveletSelectionGuideDemo - Choose the right wavelet

### Performance Optimization

- PerformanceOptimizationDemo - Speed optimization
- MemoryEfficiencyDemo - Memory optimization

### Signal Processing

- SignalAnalysisDemo - Analysis techniques
- DenoisingDemo - Noise reduction
- MultiLevelDemo - Multi-resolution analysis

### Real-time Applications

- StreamingDemo - Streaming transforms
- MemoryEfficiencyDemo - Low-latency patterns

### Algorithm Selection

- BoundaryModesDemo - Boundary handling
- WaveletSelectionGuideDemo - Wavelet selection

## Best Practices Demonstrated

1. **Error Handling**: All demos show proper exception handling
2. **Resource Management**: Proper cleanup and resource disposal
3. **Performance**: Efficient patterns for high-performance applications
4. **Memory Usage**: Techniques to minimize memory footprint
5. **Scalability**: Patterns that scale from small to large datasets

## Common Patterns

### Transform Creation

```java
// Using factory
WaveletTransform transform = new WaveletTransformFactory()
    .boundaryMode(BoundaryMode.PERIODIC)
    .create(Daubechies.DB4);

// Direct instantiation
WaveletTransform transform = new WaveletTransform(
    new Haar(), BoundaryMode.ZERO_PADDING);
```

### Basic Transform

```java
double[] signal = new double[128]; // Must be power of 2
TransformResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

### Memory-Efficient Processing

```java
MemoryPool pool = new MemoryPool();
double[] buffer = pool.borrowArray(size);
try {
    // Use buffer
} finally {
    pool.returnArray(buffer);
}
```

## Notes

- All demos use seeded random numbers for reproducibility
- Performance measurements may vary based on hardware
- SIMD optimizations require Java 21+ with Vector API support
- Some demos create temporary data for visualization

## Further Reading

- See the main README.md for library overview
- Check BENCHMARKING.md for performance testing
- Review the API documentation for detailed usage