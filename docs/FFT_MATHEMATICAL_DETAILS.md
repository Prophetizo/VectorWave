# FFT Mathematical Details

## Inverse Fast Fourier Transform (IFFT)

### Mathematical Foundation

The Inverse Discrete Fourier Transform is defined as:

```
x[n] = (1/N) * Σ(k=0 to N-1) X[k] * e^(j*2π*k*n/N)
```

Where:
- N is the transform length (must be power of 2 for FFT efficiency)
- X[k] are the frequency-domain coefficients
- x[n] are the reconstructed time-domain samples
- j is the imaginary unit (√-1)

### Implementation Details

The VectorWave IFFT implementation:
- Uses Cooley-Tukey algorithm with bit-reversal permutation
- Applies 1/N normalization for proper scaling
- Optimized for real-valued output extraction
- Maintains numerical precision through careful floating-point handling

### Algorithm Approach

The IFFT is computed using the conjugate method:
1. Take the complex conjugate of the input
2. Apply forward FFT
3. Take the complex conjugate of the result
4. Apply 1/N scaling

This approach leverages the existing FFT implementation for efficiency.

### Performance Characteristics

- **Time complexity**: O(N log N)
- **Space complexity**: O(N)
- **Optimized for**: Power-of-2 lengths
- **Memory access**: Cache-friendly patterns

### Validation Requirements

Input validation ensures:
- Input array must not be null
- Input array length must be a power of 2 (2, 4, 8, 16, 32, ...)
- All complex coefficients must contain finite values

### Usage Example

```java
// Forward transform
Complex[] spectrum = fft(timeSignal);

// Process in frequency domain
// ... apply filtering, convolution, etc.

// Inverse transform back to time domain
double[] reconstructed = ifft(spectrum);
```

## Forward Fast Fourier Transform (FFT)

### Mathematical Foundation

The Discrete Fourier Transform is defined as:

```
X[k] = Σ(n=0 to N-1) x[n] * e^(-j*2π*k*n/N)
```

Where:
- N is the transform length
- x[n] are the time-domain samples
- X[k] are the frequency-domain coefficients
- j is the imaginary unit

### Cooley-Tukey Algorithm

The implementation uses the radix-2 decimation-in-time approach:
1. Bit-reversal permutation of input
2. Iterative butterfly operations
3. Twiddle factor computations using Euler's formula

### Numerical Considerations

- Twiddle factors are computed on-the-fly to reduce memory usage
- Careful handling of floating-point precision
- Optimized for real-valued inputs common in signal processing

## CWT-Specific Optimizations

For Continuous Wavelet Transform applications:
- Real-valued signal inputs are efficiently converted to complex
- IFFT extracts only the real part for CWT reconstruction
- Memory allocation is minimized through reuse
- Cache-aware data access patterns for large transforms