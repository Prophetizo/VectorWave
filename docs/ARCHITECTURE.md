# VectorWave Architecture

## Overview

VectorWave is designed with a layered architecture that separates public API from internal optimizations while maintaining type safety and extensibility.

## Core Components

### 1. Wavelet Type System

```
ai.prophetizo.wavelet.api/
├── Wavelet (sealed interface)
├── DiscreteWavelet
│   ├── OrthogonalWavelet
│   └── BiorthogonalWavelet
└── ContinuousWavelet
```

**Key Features:**
- Sealed interfaces ensure compile-time type safety
- Each wavelet type defines its filter coefficients
- Automatic QMF (Quadrature Mirror Filter) generation for orthogonal wavelets

### 2. Transform Engine

**WaveletTransform**: Core transform implementation
- Forward/inverse DWT operations
- Boundary mode handling (periodic, zero, symmetric, reflect)
- Automatic optimization path selection

**TransformConfig**: Performance configuration
- Force scalar/SIMD execution
- Enable prefetching and cache optimization
- Memory pool configuration

### 3. Optimization Layers

```
ai.prophetizo.wavelet.internal/
├── ScalarOps         - Baseline implementation
├── VectorOps         - SIMD with platform detection
├── VectorOpsARM      - ARM-specific optimizations
├── CacheAwareOps     - Cache-optimized for large signals
└── SpecializedKernels - Common pattern optimizations
```

**Platform Adaptation:**
- Apple Silicon: Optimized for 128-bit NEON
- x86: AVX2/AVX512 support
- Automatic fallback to scalar operations

### 4. Streaming Architecture

**Flow API Integration:**
- Publisher/Subscriber pattern for real-time data
- Backpressure support
- Configurable block sizes
- Zero-copy ring buffer implementation

**Streaming Implementations:**
- **OptimizedStreamingWaveletTransform**: Zero-copy processing with ring buffer
  - True zero-copy using WaveletTransform.forward(double[], int, int)
  - Lock-free SPSC (Single Producer, Single Consumer) design
  - Configurable overlap support (0-100%)
  - Exponential backoff for buffer full conditions
- **FastStreamingDenoiser**: < 1 µs/sample latency
- **QualityStreamingDenoiser**: Better SNR with overlap

**Ring Buffer Design:**
- Lock-free atomic operations for thread safety
- Thread-local buffers for window extraction
- Configurable capacity multiplier for smooth operation
- Race condition prevention through atomic snapshots

### 5. Memory Management

**Object Pooling:**
- WaveletTransformPool: Reusable transform instances
- AlignedMemoryPool: SIMD-aligned allocations
- Generational pools for different object lifetimes

## Design Principles

1. **Zero Allocation Hot Path**: Critical operations avoid allocations
2. **Platform Agnostic API**: Same code runs optimally on all platforms
3. **Fail-Fast Validation**: Early error detection with custom exceptions
4. **Extensibility**: Easy to add new wavelets and optimizations

## Data Flow

```
Signal Input
    ↓
Validation (power-of-2 check)
    ↓
Optimization Path Selection
    ├── Scalar (small signals)
    ├── SIMD (medium signals)
    └── Cache-aware (large signals)
    ↓
Boundary Padding
    ↓
Convolution & Downsampling
    ↓
Multi-level Decomposition (optional)
    ↓
TransformResult
```

## Thread Safety

- Transform instances are thread-safe for concurrent use
- Streaming components use atomic operations
- Memory pools use lock-free data structures

## Extension Points

1. **New Wavelets**: 
   - Implement appropriate wavelet interface (OrthogonalWavelet, BiorthogonalWavelet, or ContinuousWavelet)
   - Create a WaveletProvider implementation
   - Register via ServiceLoader in META-INF/services
2. **Custom Padding**: Implement PaddingStrategy interface
3. **Optimization Paths**: Extend VectorOps with platform-specific code
4. **Threshold Methods**: Add to ThresholdCalculator

## ServiceLoader Architecture

### Wavelet Discovery

VectorWave uses Java's ServiceLoader mechanism for automatic wavelet discovery, eliminating static initialization blocks and circular dependencies.

```
ai.prophetizo.wavelet.api.providers/
├── OrthogonalWaveletProvider    - Haar, Daubechies, Symlets, Coiflets
├── BiorthogonalWaveletProvider  - Biorthogonal spline wavelets
└── (custom providers)           - Third-party wavelets

ai.prophetizo.wavelet.cwt.providers/
├── ContinuousWaveletProvider    - Morlet, Gaussian derivatives
└── (custom providers)           - Third-party CWT wavelets

ai.prophetizo.wavelet.cwt.finance.providers/
└── FinancialWaveletProvider     - Paul, DOG, Shannon variants
```

### Benefits

1. **No Circular Dependencies**: Providers are loaded lazily on first access
2. **Plugin Architecture**: Add wavelets without modifying core library
3. **Clean Separation**: Each wavelet family has its own provider
4. **Thread-Safe Loading**: Double-checked locking ensures safe initialization

### Registry Design

```java
WaveletRegistry (static facade)
    ↓
ServiceLoader.load(WaveletProvider.class)
    ↓
WaveletProvider implementations
    ↓
Individual Wavelet instances
```

The registry maintains:
- Thread-safe wavelet lookup by name (case-insensitive)
- Categorization by wavelet type
- Manual registration support for runtime additions
- Reload capability for dynamic scenarios