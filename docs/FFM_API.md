# Foreign Function & Memory API (FFM) Guide

## Overview

VectorWave includes comprehensive support for Java's Foreign Function & Memory API (FFM), providing zero-copy operations and improved memory efficiency for wavelet transforms. This guide covers the FFM implementation, usage patterns, and performance characteristics.

## Requirements

- Java 23 or later
- JVM flags: `--enable-native-access=ALL-UNNAMED`
- Additional modules: `jdk.incubator.vector` (for combined optimizations)

## Architecture

### Core Components

#### FFMWaveletTransform
Drop-in replacement for `WaveletTransform` with identical API but improved memory efficiency.

```java
// Traditional
WaveletTransform traditional = new WaveletTransform(wavelet);

// FFM equivalent
FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet);
```

#### FFMMemoryPool
Thread-safe memory pool providing SIMD-aligned memory segments with automatic recycling.

Features:
- Pre-warming for common sizes
- Automatic size bucketing
- Thread-safe operations
- Statistics tracking
- Scoped memory management

#### FFMStreamingTransform
Zero-copy streaming implementation with ring buffer architecture.

Features:
- Lock-free ring buffer
- Configurable overlap
- Real-time processing
- Backpressure handling

## Usage Patterns

### Basic Transform

```java
// Simple usage with automatic resource management
try (FFMWaveletTransform transform = new FFMWaveletTransform(new Haar())) {
    double[] signal = generateSignal();
    TransformResult result = transform.forward(signal);
    double[] reconstructed = transform.inverse(result);
}
```

### Shared Memory Pool

```java
// Reuse memory pool across multiple transforms
try (FFMMemoryPool pool = new FFMMemoryPool()) {
    // Pre-warm for better performance
    pool.prewarm(256, 512, 1024, 2048);
    
    FFMWaveletTransform transform1 = new FFMWaveletTransform(wavelet1, pool);
    FFMWaveletTransform transform2 = new FFMWaveletTransform(wavelet2, pool);
    
    // Process multiple signals
    for (double[] signal : signals) {
        TransformResult result = transform1.forward(signal);
        processResult(result);
    }
    
    // Check pool efficiency
    FFMMemoryPool.PoolStatistics stats = pool.getStatistics();
    System.out.println("Hit rate: " + stats.hitRate());
}
```

### Scoped Memory Operations

```java
// Automatic cleanup after scope
double[] result = FFMMemoryPool.withScope(pool -> {
    FFMWaveletTransform transform = new FFMWaveletTransform(wavelet, pool);
    TransformResult coeffs = transform.forward(inputSignal);
    // Process coefficients
    return transform.inverse(coeffs);
});
```

### Zero-Copy Slicing

```java
// Process array slice without copying
FFMWaveletTransform transform = new FFMWaveletTransform(wavelet);
TransformResult result = transform.forward(largeArray, offset, length);
```

### Streaming Processing

```java
try (FFMStreamingTransform streaming = new FFMStreamingTransform(wavelet, 1024, 0.5)) {
    // Configure backpressure
    streaming.setBackpressureStrategy(BackpressureStrategy.BLOCK);
    
    // Process incoming data
    while (hasMoreData()) {
        byte[] chunk = readChunk();
        streaming.processChunk(chunk, 0, chunk.length);
        
        // Process completed blocks
        while (streaming.hasCompleteBlock()) {
            TransformResult result = streaming.getNextResult();
            processResult(result);
        }
    }
}
```

## Performance Optimization

### Memory Pool Tuning

```java
// Custom pool configuration
FFMMemoryPool pool = new FFMMemoryPool();

// Pre-warm with specific sizes
pool.prewarm(
    64,    // Small signals
    256,   // Medium signals
    1024,  // Large signals
    4096   // Very large signals
);

// Monitor pool performance
pool.getStatistics().logStatistics();
```

### SIMD Alignment

All FFM allocations are automatically aligned for optimal SIMD performance:
- 64-byte alignment for AVX-512
- 32-byte alignment for AVX2
- 16-byte alignment for SSE/NEON

### Batch Processing

```java
// Process multiple signals efficiently
try (FFMMemoryPool pool = new FFMMemoryPool()) {
    FFMBatchProcessor processor = new FFMBatchProcessor(wavelet, pool);
    
    List<TransformResult> results = processor.processBatch(signals)
        .parallel()  // Use Fork-Join
        .withPoolSize(Runtime.getRuntime().availableProcessors())
        .execute();
}
```

## Memory Segment Operations

### Direct Memory Access

```java
// Work directly with memory segments
MemorySegment segment = pool.acquire(1024);
try {
    // Fill with data
    for (int i = 0; i < 1024; i++) {
        segment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, Math.sin(i * 0.1));
    }
    
    // Process with FFM
    TransformResult result = transform.forwardSegment(segment, 1024);
} finally {
    pool.release(segment);
}
```

### Native Interop

```java
// Interop with native libraries
MemorySegment nativeData = MemorySegment.ofAddress(nativePointer, size);
TransformResult result = transform.forwardSegment(nativeData, elementCount);
```

## Performance Characteristics

### Memory Efficiency
- **Zero-copy operations**: No array copying for slices
- **Pooled allocations**: 90%+ hit rate after warm-up
- **Off-heap memory**: Reduced GC pressure
- **SIMD alignment**: Optimal vectorization

### Speed Improvements
| Signal Size | Traditional | FFM | Speedup |
|------------|-------------|-----|---------|
| 256        | 45 µs       | 42 µs | 1.07x |
| 1024       | 180 µs      | 150 µs | 1.20x |
| 4096       | 750 µs      | 420 µs | 1.79x |
| 16384      | 3200 µs     | 1100 µs | 2.91x |

### When to Use FFM

**Recommended for:**
- Large signals (≥1024 elements)
- Streaming/real-time processing
- Memory-constrained environments
- Native library integration
- Batch processing

**Not recommended for:**
- Small signals (<256 elements)
- One-off transformations
- Environments without Java 23+

## Current Limitations

1. **Boundary Modes**: CONSTANT mode not implemented for upsampling operations
2. **Java Version**: Requires Java 23+
3. **JVM Flags**: Must run with `--enable-native-access=ALL-UNNAMED`
4. **Biorthogonal Wavelets**: Fixed in recent update - now includes automatic phase compensation for proper reconstruction

## Error Handling

### Arena Mismatch
```java
// This will throw IllegalArgumentException
FFMMemoryPool pool1 = new FFMMemoryPool();
FFMMemoryPool pool2 = new FFMMemoryPool();

MemorySegment segment = pool1.acquire(1024);
pool2.release(segment); // IllegalArgumentException: different arena
```

### Buffer Overflow
```java
// Ring buffer provides clear error messages
FFMStreamingTransform streaming = new FFMStreamingTransform(wavelet, 1024);
streaming.processChunk(hugeData, 0, hugeData.length);
// IllegalStateException: Ring buffer overflow: requested 10000 elements but only 512 available
```

## Best Practices

1. **Always use try-with-resources** for automatic cleanup
2. **Pre-warm pools** for predictable performance
3. **Share pools** across transforms when possible
4. **Monitor statistics** to tune pool sizes
5. **Use appropriate block sizes** for streaming (powers of 2)
6. **Handle backpressure** in streaming scenarios

## Example: Complete Application

```java
public class FFMWaveletProcessor {
    private final FFMMemoryPool pool;
    private final FFMWaveletTransform transform;
    private final WaveletDenoiser denoiser;
    
    public FFMWaveletProcessor(Wavelet wavelet) {
        this.pool = new FFMMemoryPool();
        this.pool.prewarm(256, 512, 1024, 2048, 4096);
        this.transform = new FFMWaveletTransform(wavelet, pool);
        this.denoiser = new WaveletDenoiser(wavelet);
    }
    
    public double[] processSignal(double[] noisySignal) {
        // Forward transform
        TransformResult coefficients = transform.forward(noisySignal);
        
        // Denoise in wavelet domain
        TransformResult denoised = denoiser.thresholdCoefficients(
            coefficients, 
            ThresholdMethod.SURE
        );
        
        // Inverse transform
        return transform.inverse(denoised);
    }
    
    public void close() {
        transform.close();
        pool.close();
    }
    
    public void logStatistics() {
        FFMMemoryPool.PoolStatistics stats = pool.getStatistics();
        System.out.printf("Pool Statistics:%n");
        System.out.printf("  Allocations: %d%n", stats.totalAllocations());
        System.out.printf("  Pool Hits: %d (%.1f%%)%n", 
            stats.poolHits(), stats.hitRate() * 100);
        System.out.printf("  Direct Allocations: %d%n", stats.directAllocations());
        System.out.printf("  Total Bytes: %d%n", stats.totalBytesAllocated());
    }
}
```

## Troubleshooting

### UnsupportedOperationException
- Check if using CONSTANT boundary mode with upsampling
- Verify Java version is 23+

### IllegalStateException
- Ring buffer overflow: reduce chunk size or increase buffer capacity
- Check for proper resource cleanup

### Performance Issues
- Ensure pool is pre-warmed
- Verify SIMD alignment (use `FFMArrayAllocator.isAligned()`)
- Check pool hit rate (should be >90% after warm-up)
- Consider signal size (FFM benefits appear at ≥1024 elements)