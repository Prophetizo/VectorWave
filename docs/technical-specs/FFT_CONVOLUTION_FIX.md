# Technical Specification: FFT Convolution Fix

## Problem Statement

The current FFT-based convolution in `CWTTransform` uses circular convolution, which introduces artifacts at signal boundaries. This is a critical mathematical correctness issue that must be fixed before the CWT feature can be merged.

## Current Implementation Issues

### 1. Circular Convolution Problem

```java
// Current problematic code in CWTTransform.java
private double[] fftConvolve(double[] signal, double[] wavelet) {
    int fftSize = nextPowerOfTwo(Math.max(signal.length, wavelet.length));
    // This causes circular wrapping!
    Complex[] signalFFT = fft(signal, fftSize);
    Complex[] waveletFFT = fft(wavelet, fftSize);
    // ... multiplication and IFFT
}
```

**Issue**: When the convolution result extends beyond the original signal length, it wraps around to the beginning (circular convolution), contaminating the results.

### 2. Insufficient Padding

The current implementation doesn't account for the fact that linear convolution of sequences of length N and M produces a result of length N + M - 1.

### 3. Edge Effects

Without proper padding, wavelet coefficients near signal boundaries are incorrect, particularly problematic for financial crash detection at recent time points.

## Proposed Solution

### 1. Proper Zero-Padding for Linear Convolution

```java
private double[] fftConvolve(double[] signal, double[] wavelet) {
    // Calculate required length for linear convolution
    int linearConvLength = signal.length + wavelet.length - 1;
    int fftSize = nextPowerOfTwo(linearConvLength);
    
    // Zero-pad both inputs to FFT size
    double[] paddedSignal = new double[fftSize];
    double[] paddedWavelet = new double[fftSize];
    
    System.arraycopy(signal, 0, paddedSignal, 0, signal.length);
    System.arraycopy(wavelet, 0, paddedWavelet, 0, wavelet.length);
    
    // Perform FFT on padded arrays
    Complex[] signalFFT = fft(paddedSignal);
    Complex[] waveletFFT = fft(paddedWavelet);
    
    // Multiply in frequency domain
    Complex[] productFFT = multiplyComplex(signalFFT, waveletFFT);
    
    // Inverse FFT
    double[] result = ifft(productFFT);
    
    // Extract valid portion (linear convolution result)
    double[] convResult = new double[signal.length];
    System.arraycopy(result, 0, convResult, 0, signal.length);
    
    return convResult;
}
```

### 2. Optimized Real-to-Complex FFT

Since financial signals are real-valued, we can use a specialized real-to-complex FFT:

```java
private Complex[] rfft(double[] real) {
    int n = real.length;
    int halfN = n / 2;
    
    // Pack real signal for efficient FFT
    double[] packed = new double[n];
    for (int i = 0; i < halfN; i++) {
        packed[i] = real[2 * i];      // Even indices
        packed[halfN + i] = real[2 * i + 1]; // Odd indices
    }
    
    // Perform single FFT on packed data
    Complex[] fft = fft(packed, halfN);
    
    // Unpack to get full spectrum
    Complex[] result = new Complex[halfN + 1];
    // ... unpacking logic
    
    return result;
}
```

### 3. Memory-Efficient Implementation

To reduce allocations, implement pooling for FFT workspace:

```java
public class FFTWorkspace {
    private final int maxSize;
    private final Complex[] workspace;
    private final double[] cosTable;
    private final double[] sinTable;
    
    public FFTWorkspace(int maxSize) {
        this.maxSize = nextPowerOfTwo(maxSize);
        this.workspace = new Complex[this.maxSize];
        
        // Pre-compute twiddle factors
        this.cosTable = new double[this.maxSize];
        this.sinTable = new double[this.maxSize];
        precomputeTwiddleFactors();
    }
    
    private void precomputeTwiddleFactors() {
        for (int i = 0; i < maxSize; i++) {
            double angle = -2.0 * Math.PI * i / maxSize;
            cosTable[i] = Math.cos(angle);
            sinTable[i] = Math.sin(angle);
        }
    }
}
```

### 4. Configuration Options

Add configuration to control convolution behavior:

```java
public enum ConvolutionMode {
    LINEAR,      // Proper zero-padding (default)
    CIRCULAR,    // Legacy mode (deprecated)
    SYMMETRIC,   // Mirror padding at boundaries
    VALID        // Only compute where wavelets fully overlap
}

public class CWTConfig {
    private ConvolutionMode convolutionMode = ConvolutionMode.LINEAR;
    // ... other config
}
```

## Implementation Plan

### Phase 1: Core Fix (Immediate)
1. Implement proper zero-padding in `fftConvolve`
2. Add unit tests to verify no circular artifacts
3. Validate against reference implementations

### Phase 2: Optimization (Next Sprint)
1. Implement real-to-complex FFT
2. Add FFT workspace pooling
3. Pre-compute twiddle factors

### Phase 3: Enhancement (Future)
1. Add configurable convolution modes
2. Implement symmetric padding option
3. Optimize for specific wavelet support sizes

## Testing Strategy

### 1. Unit Tests

```java
@Test
public void testNoCircularArtifacts() {
    // Create signal with known boundary
    double[] signal = createStepSignal(1024);
    
    // Apply CWT with edge-detecting wavelet
    CWTResult result = cwt.transform(signal, scales);
    
    // Verify no wraparound artifacts at end
    double[] lastScaleCoeffs = result.getCoefficients()[0];
    assertTrue("No circular artifacts", 
        lastScaleCoeffs[0] < threshold);
}
```

### 2. Mathematical Validation

Compare results against:
- MATLAB's `cwt()` function
- Python's PyWavelets
- Direct convolution implementation

### 3. Performance Benchmarks

Ensure fix doesn't significantly impact performance:
- Target: < 10% performance regression
- Optimize if necessary

## Backward Compatibility

- Default to LINEAR mode (correct behavior)
- Provide migration guide for users relying on old behavior
- Log warnings if CIRCULAR mode is used

## Success Criteria

1. ✅ No circular convolution artifacts in edge regions
2. ✅ Mathematical validation against reference implementations
3. ✅ Performance regression < 10%
4. ✅ All existing tests pass
5. ✅ New tests for edge cases pass

## References

1. Oppenheim & Schafer, "Discrete-Time Signal Processing", Chapter 8
2. FFTW documentation on convolution
3. PyWavelets implementation for comparison
4. MATLAB Signal Processing Toolbox documentation