# VectorWave API Reference

## Core Classes

### WaveletTransform

Main transform class for forward and inverse DWT operations.

```java
// Creation
WaveletTransform transform = WaveletTransformFactory.createDefault(wavelet);
WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode, config);

// Operations
TransformResult forward(double[] signal)
double[] inverse(TransformResult result)
```

### TransformResult

Immutable result of wavelet transform.

```java
double[] approximation()    // Low-frequency coefficients
double[] detail()          // High-frequency coefficients  
int level()               // Decomposition level
Wavelet wavelet()         // Wavelet used
```

### Wavelet Families

#### Orthogonal Wavelets
```java
// Haar
new Haar()

// Daubechies
Daubechies.DB2, DB4, DB6, DB8, DB10, DB12, DB14, DB16, DB18, DB20

// Symlets
Symlet.SYM2, SYM3, SYM4, SYM5, SYM6, SYM7, SYM8

// Coiflets
Coiflet.COIF1, COIF2, COIF3, COIF4, COIF5
```

#### Biorthogonal Wavelets
```java
BiorthogonalSpline.BIOR1_1, BIOR1_3, BIOR1_5
BiorthogonalSpline.BIOR2_2, BIOR2_4, BIOR2_6, BIOR2_8
BiorthogonalSpline.BIOR3_1, BIOR3_3, BIOR3_5, BIOR3_7, BIOR3_9
```

#### Continuous Wavelets
```java
new MorletWavelet(omega0, sigma)  // omega0: frequency, sigma: bandwidth
```

### Boundary Modes

```java
public enum BoundaryMode {
    PERIODIC,    // Signal wraps around
    ZERO,        // Zero padding
    SYMMETRIC,   // Mirror at boundaries
    REFLECT      // Reflect at boundaries
}
```

## Denoising API

### WaveletDenoiser

```java
WaveletDenoiser denoiser = new WaveletDenoiser(wavelet);

// Single-level denoising
double[] denoise(double[] signal, ThresholdMethod method)

// Multi-level denoising
double[] denoise(double[] signal, ThresholdMethod method, int levels)
double[] denoise(double[] signal, ThresholdMethod method, int levels, ThresholdType type)

// With custom threshold
double[] denoiseWithThreshold(double[] signal, double threshold)
double[] denoiseWithThreshold(double[] signal, double threshold, ThresholdType type)
```

### Threshold Methods

```java
public enum ThresholdMethod {
    UNIVERSAL,   // Universal threshold (Donoho & Johnstone)
    SURE,        // Stein's Unbiased Risk Estimate
    MINIMAX,     // Minimax threshold
    CUSTOM       // User-defined threshold
}

public enum ThresholdType {
    HARD,        // Hard thresholding
    SOFT         // Soft thresholding
}
```

## Streaming API

### StreamingWaveletTransform

```java
// Creation
StreamingWaveletTransform stream = StreamingWaveletTransform.create(
    wavelet, boundaryMode, blockSize
);

// Processing
stream.process(double[] chunk)
stream.process(double sample)
stream.flush()

// Subscription (Flow API)
stream.subscribe(new Flow.Subscriber<TransformResult>() {
    public void onNext(TransformResult result) { ... }
    public void onError(Throwable error) { ... }
    public void onComplete() { ... }
});
```

### StreamingDenoiser

```java
// Factory creation with auto-selection
StreamingDenoiser denoiser = StreamingDenoiserFactory.create(
    wavelet, method, blockSize, overlapRatio
);

// Direct creation
StreamingDenoiser fast = new FastStreamingDenoiser(wavelet, method, blockSize);
StreamingDenoiser quality = new QualityStreamingDenoiser(
    wavelet, method, blockSize, overlapRatio
);
```

## Configuration

### TransformConfig

```java
TransformConfig config = TransformConfig.builder()
    .boundaryMode(BoundaryMode.PERIODIC)
    .forceScalar(false)
    .forceSIMD(false)
    .enablePrefetch(true)
    .cacheOptimized(true)
    .poolSize(16)
    .alignedMemory(true)
    .build();
```

### WaveletRegistry

```java
// Lookup wavelets
Wavelet wavelet = WaveletRegistry.get("db4");
List<String> available = WaveletRegistry.getAvailableWavelets();
List<OrthogonalWavelet> orthogonal = WaveletRegistry.getOrthogonalWavelets();
```

## Utilities

### SignalUtils

```java
double[] padToPowerOfTwo(double[] signal)
boolean isPowerOfTwo(int n)
int nextPowerOfTwo(int n)
double[] normalize(double[] signal)
```

### WaveletUtils

```java
double[] computeHighPassFromLowPass(double[] lowPass)  // QMF
boolean verifyOrthogonality(Wavelet wavelet)
boolean verifyPerfectReconstruction(Wavelet wavelet)
```

## Exceptions

```java
WaveletException (base)
├── InvalidSignalException      // Invalid signal (null, empty, wrong size)
├── InvalidWaveletException     // Invalid wavelet parameters
├── InvalidArgumentException    // Invalid method arguments
├── TransformException          // Transform operation failed
└── InvalidStateException       // Invalid object state
```

## Thread Safety

- **Thread-safe**: WaveletTransform, WaveletDenoiser, all Wavelet implementations
- **Not thread-safe**: StreamingWaveletTransform (use one per thread)
- **Lock-free**: Memory pools, WaveletRegistry