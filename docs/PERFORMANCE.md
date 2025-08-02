# Performance Guide

## Overview

VectorWave achieves high performance through multiple optimization strategies tailored to different signal sizes and platforms.

## Optimization Strategies

### Zero-Copy Streaming

**OptimizedStreamingWaveletTransform:**
- Eliminates array copying during transform operations
- 50% reduction in memory bandwidth usage
- Ring buffer with lock-free operations
- Configurable overlap support (0-100%)
- Automatic backpressure handling

```java
// Zero-copy streaming with overlap
OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
    wavelet, BoundaryMode.PERIODIC, blockSize, 
    0.5,  // 50% overlap
    8     // buffer capacity = blockSize * 8
);
```

### 1. SIMD/Vector API

**Platform Thresholds:**
- Apple Silicon (M1/M2/M3): Benefits from SIMD with signals ≥ 8 elements
- x86 (AVX2): Benefits from SIMD with signals ≥ 16 elements  
- x86 (AVX512): Benefits from SIMD with signals ≥ 32 elements
- ARM (general): Benefits from SIMD with signals ≥ 8 elements

**Performance Gains:**
- 2-8x speedup for convolution operations
- 3-5x speedup for threshold operations
- Platform-specific optimizations for gather/scatter

### 1.1 Batch SIMD Processing

**True Parallel Signal Processing:**
- Process N signals simultaneously (N = SIMD vector width)
- Optimized memory layouts for coalesced vector operations
- Adaptive algorithm selection based on batch size

**Usage:**
```java
// Basic batch processing
WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
double[][] signals = new double[32][1024];
TransformResult[] results = transform.forwardBatch(signals);

// Advanced configuration
OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
    .withSoALayout(true)          // Structure-of-Arrays layout
    .withSpecializedKernels(true) // Use optimized kernels
    .withCacheBlocking(true);     // Cache-aware blocking
```

**Performance Characteristics:**
- 2-4x speedup for aligned batch sizes (multiples of vector width)
- Best performance with batch sizes 8-64 signals
- Automatic fallback for non-aligned batches
- Memory bandwidth limited for very large batches

### 2. Memory Optimization

**Object Pooling:**
```java
// Reuse transform instances
WaveletTransformPool pool = new WaveletTransformPool(wavelet, mode);
WaveletTransform transform = pool.acquire();
try {
    result = transform.forward(signal);
} finally {
    pool.release(transform);
}
```

**Aligned Memory:**
- 64-byte alignment for cache lines
- Reduces cache misses by 30-40%
- Automatic in VectorOpsPooled

### 3. Cache-Aware Operations

For signals > 64KB:
- Block processing to fit L2 cache
- Prefetching for sequential access
- 20-30% improvement for large signals

### 4. Parallel Processing

```java
ParallelWaveletEngine engine = new ParallelWaveletEngine(wavelet);
List<TransformResult> results = engine.transformBatch(signals);
```

**Scaling:**
- Near-linear scaling up to 8 cores
- Work-stealing for load balancing
- Configurable parallelism threshold

## Configuration

### Force Optimization Path

```java
// Force Vector API for testing
TransformConfig config = TransformConfig.builder()
    .forceVector(true)
    .build();

// Force scalar for compatibility
TransformConfig config = TransformConfig.builder()
    .forceScalar(true)
    .build();
```

### Memory Configuration

```java
// Configure memory pools
TransformConfig config = TransformConfig.builder()
    .poolSize(16)
    .alignedMemory(true)
    .build();
```

## Benchmarking

### Running Benchmarks

```bash
# All benchmarks
./jmh-runner.sh

# Specific benchmark
./jmh-runner.sh SignalSizeBenchmark

# With specific parameters
./jmh-runner.sh SignalSizeBenchmark -p signalSize=1024,2048,4096
```

### Key Benchmarks

1. **SignalSizeBenchmark**: Performance vs signal size
2. **ScalarVsVectorBenchmark**: SIMD speedup measurement
3. **WaveletTypeBenchmark**: Performance across wavelet families
4. **StreamingBenchmark**: Real-time processing latency
5. **StreamingTransformBenchmark**: Zero-copy streaming performance

### Typical Results

| Signal Size | Scalar | SIMD | Speedup |
|-------------|---------|---------|---------|
| 64 | 250 ns | 280 ns | 0.9x |
| 256 | 950 ns | 420 ns | 2.3x |
| 1024 | 3.8 µs | 1.2 µs | 3.2x |
| 4096 | 15.2 µs | 3.8 µs | 4.0x |

## Optimization Tips

### 1. Signal Length
- Always use power-of-2 lengths
- Pad signals if necessary
- Minimum 64 elements for best performance

### 2. Wavelet Selection
- Haar: Fastest (2 coefficients)
- DB4: Good balance (8 coefficients)
- Higher-order: More computation

### 3. Streaming Configuration
- Block size: 512-1024 for latency/throughput balance
- Overlap < 30% for real-time
- Use factory for automatic selection
- Zero-copy ring buffer reduces memory bandwidth by 50%
- Configure buffer capacity multiplier for smooth operation

### 4. Memory Patterns
- Process signals in batches
- Reuse arrays when possible
- Use streaming for large datasets
- Zero-copy streaming with OptimizedStreamingWaveletTransform

## Platform-Specific Notes

### Apple Silicon
- Excellent SIMD performance even for small signals
- Unified memory architecture benefits
- Use VectorOpsARM for best results

### x86-64
- AVX2: Good for signals ≥ 256 elements
- AVX512: Best for signals ≥ 1024 elements
- Enable turbo boost for benchmarks

### ARM (Non-Apple)
- NEON instructions well-supported
- Similar characteristics to Apple Silicon
- May need platform-specific tuning