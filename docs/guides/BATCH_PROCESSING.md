# Batch Processing Guide

This guide covers the batch processing capabilities in VectorWave, which enable efficient processing of multiple signals simultaneously using SIMD instructions.

## Overview

Batch processing in VectorWave provides true parallel processing of multiple signals, leveraging SIMD (Single Instruction, Multiple Data) capabilities of modern processors. This is particularly useful for:

- Multi-channel audio processing
- Financial time series analysis (multiple stocks/currencies)
- Sensor array data processing
- Large-scale signal analysis pipelines

## Basic Usage

### Simple Batch Transform

```java
import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;

// Create a wavelet transform
WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);

// Prepare multiple signals
double[][] signals = new double[32][1024]; // 32 signals of length 1024
// ... populate signals ...

// Process all signals in parallel
TransformResult[] results = transform.forwardBatch(signals);

// Inverse transform
double[][] reconstructed = transform.inverseBatch(results);
```

### Using Different Wavelets

```java
// Daubechies wavelets
WaveletTransform db4Transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
TransformResult[] db4Results = db4Transform.forwardBatch(signals);

// Symlet wavelets
WaveletTransform sym4Transform = new WaveletTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
TransformResult[] sym4Results = sym4Transform.forwardBatch(signals);
```

## Advanced Configuration

### Optimized Transform Engine

For maximum performance, use the `OptimizedTransformEngine`:

```java
// Create engine with custom configuration
OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
    .withParallelism(8)           // Number of threads (use 1 for pure SIMD)
    .withSoALayout(true)          // Enable Structure-of-Arrays optimization
    .withSpecializedKernels(true) // Use optimized kernels for common wavelets
    .withCacheBlocking(true)      // Enable cache-aware blocking
    .withMemoryPool(true);        // Use memory pooling

OptimizedTransformEngine engine = new OptimizedTransformEngine(config);

// Process batch
TransformResult[] results = engine.transformBatch(signals, wavelet, boundaryMode);
```

### Memory-Aligned Batch Processing

For optimal SIMD performance with aligned memory:

```java
import ai.prophetizo.wavelet.memory.BatchMemoryLayout;

// Create aligned memory layout
try (BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength)) {
    // Load signals with interleaving for better SIMD access
    layout.loadSignalsInterleaved(signals, true);
    
    // Perform transform
    layout.haarTransformInterleaved();
    
    // Extract results
    double[][] approxResults = new double[batchSize][signalLength / 2];
    double[][] detailResults = new double[batchSize][signalLength / 2];
    layout.extractResultsInterleaved(approxResults, detailResults);
}
```

## Performance Optimization Tips

### 1. Batch Size Selection

- **Optimal sizes**: Multiples of the SIMD vector width (typically 2, 4, or 8)
- **Sweet spot**: 8-64 signals for most applications
- **Large batches**: Use parallel processing for 64+ signals

### 2. Signal Length Considerations

- Powers of 2 are most efficient
- Longer signals benefit more from batch processing
- Consider padding non-power-of-2 signals

### 3. Memory Layout

The batch processor supports two memory layouts:

**Array of Structures (AoS)** - Default:
```
Signal 0: [s0_0, s0_1, s0_2, ...]
Signal 1: [s1_0, s1_1, s1_2, ...]
```

**Structure of Arrays (SoA)** - Optimized:
```
Sample 0: [s0_0, s1_0, s2_0, ...]
Sample 1: [s0_1, s1_1, s2_1, ...]
```

### 4. Platform-Specific Optimization

```java
// Get information about the current platform's SIMD capabilities
System.out.println(BatchSIMDTransform.getBatchSIMDInfo());
```

## Real-World Examples

### Multi-Channel Audio Processing

```java
// Process stereo audio (2 channels)
double[][] stereoSignal = new double[2][44100]; // 1 second at 44.1kHz
// ... load audio data ...

WaveletTransform transform = new WaveletTransform(Daubechies.DB8, BoundaryMode.PERIODIC);
TransformResult[] channelResults = transform.forwardBatch(stereoSignal);

// Apply processing to each channel
for (int ch = 0; ch < 2; ch++) {
    // ... process channelResults[ch] ...
}

// Reconstruct
double[][] processedAudio = transform.inverseBatch(channelResults);
```

### Financial Time Series Analysis

```java
// Analyze multiple stock prices
String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN"};
double[][] priceData = new double[symbols.length][252]; // 1 year of daily data
// ... load price data ...

// Use optimized engine for financial analysis
OptimizedTransformEngine engine = new OptimizedTransformEngine();
TransformResult[] results = engine.transformBatch(
    priceData, 
    Daubechies.DB4, 
    BoundaryMode.PERIODIC
);

// Analyze each stock's wavelet coefficients
for (int i = 0; i < symbols.length; i++) {
    System.out.println("Analysis for " + symbols[i]);
    analyzeCoefficients(results[i]);
}
```

### Sensor Array Processing

```java
// Process data from sensor array
int numSensors = 16;
int samplesPerSecond = 1000;
double[][] sensorData = new double[numSensors][samplesPerSecond];
// ... collect sensor data ...

// Configure for real-time processing
OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
    .withParallelism(1)      // Single thread for low latency
    .withSoALayout(true)     // Optimize for SIMD
    .withMemoryPool(true);   // Reduce allocation overhead

OptimizedTransformEngine engine = new OptimizedTransformEngine(config);

// Process in real-time
TransformResult[] sensorResults = engine.transformBatch(
    sensorData,
    new Haar(),  // Fast wavelet for real-time
    BoundaryMode.PERIODIC
);
```

## Performance Benchmarking

### Measuring Batch Performance

```java
import ai.prophetizo.wavelet.benchmark.*;

// Compare sequential vs batch processing
int batchSize = 32;
int signalLength = 1024;
int iterations = 1000;

// Generate test data
double[][] testSignals = generateTestSignals(batchSize, signalLength);

// Sequential processing
long seqStart = System.nanoTime();
for (int i = 0; i < iterations; i++) {
    for (double[] signal : testSignals) {
        transform.forward(signal);
    }
}
long seqTime = System.nanoTime() - seqStart;

// Batch processing
long batchStart = System.nanoTime();
for (int i = 0; i < iterations; i++) {
    transform.forwardBatch(testSignals);
}
long batchTime = System.nanoTime() - batchStart;

// Calculate speedup
double speedup = (double) seqTime / batchTime;
System.out.printf("Batch processing speedup: %.2fx%n", speedup);
```

## Troubleshooting

### Common Issues

1. **Performance not improving**: 
   - Check batch size alignment with SIMD vector width
   - Ensure signals are properly aligned in memory
   - Verify platform supports Vector API

2. **Out of memory errors**:
   - Use memory pooling
   - Process in smaller batches
   - Enable streaming mode for very large datasets

3. **Incorrect results**:
   - Verify all signals have the same length
   - Check boundary mode compatibility
   - Ensure proper signal padding for non-power-of-2 lengths

### Debug Information

```java
// Get detailed optimization information
OptimizedTransformEngine engine = new OptimizedTransformEngine();
System.out.println(engine.getOptimizationInfo());

// Check SIMD capabilities
System.out.println("Vector species: " + DoubleVector.SPECIES_PREFERRED);
System.out.println("Vector length: " + DoubleVector.SPECIES_PREFERRED.length());
```

## See Also

- [Performance Guide](../PERFORMANCE.md)
- [SIMD Optimization Analysis](../performance/SIMD_OPTIMIZATION_ANALYSIS.md)
- [Streaming Guide](STREAMING.md)