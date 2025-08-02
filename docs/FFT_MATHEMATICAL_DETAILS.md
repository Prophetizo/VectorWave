# FFT Mathematical Details

## Canonical Algorithm References

This document describes the mathematical foundations of the FFT algorithms implemented
in VectorWave. All algorithms are cross-referenced with their canonical sources.

## Fast Fourier Transform (FFT)

### Mathematical Foundation

The Discrete Fourier Transform is defined as (Cooley & Tukey, 1965):

```
X[k] = Σ(n=0 to N-1) x[n] * e^(-j*2π*k*n/N)
```

Where:
- N is the transform length (must be power of 2 for radix-2 FFT)
- x[n] are the time-domain samples
- X[k] are the frequency-domain coefficients
- j is the imaginary unit (√-1)

### Cooley-Tukey Algorithm

The implementation follows the canonical Cooley-Tukey radix-2 DIT algorithm:
1. Bit-reversal permutation of input
2. Log₂(N) stages of butterfly operations
3. Twiddle factor multiplication: W_N^k = e^(-j*2π*k/N)

**Reference**: Cooley, J. W., & Tukey, J. W. (1965). "An algorithm for the machine 
calculation of complex Fourier series." Mathematics of computation, 19(90), 297-301.

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

The IFFT is computed using the conjugate method (Oppenheim & Schafer, 2009):
1. Take the complex conjugate of the input
2. Apply forward FFT
3. Take the complex conjugate of the result
4. Apply 1/N scaling

This approach leverages the existing FFT implementation for efficiency.

**Reference**: Oppenheim, A. V., & Schafer, R. W. (2009). "Discrete-Time Signal 
Processing" (3rd ed., pp. 652-653). Pearson.

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

## Linear Convolution via FFT

### Avoiding Circular Convolution Artifacts

To implement linear convolution using FFT (Proakis & Manolakis, 2006):
1. Zero-pad signal to length: L = signal_length + filter_length - 1
2. Zero-pad to next power of 2 for FFT efficiency
3. Compute: IFFT(FFT(signal) * FFT(filter))

**Reference**: Proakis, J. G., & Manolakis, D. G. (2006). "Digital Signal Processing" 
(4th ed., pp. 430-437). Pearson.

## Real-Valued FFT Optimization

### Hermitian Symmetry Property

For real-valued signals, the FFT exhibits Hermitian symmetry:
```
X[k] = X*[N-k] for k = 1, 2, ..., N-1
```

This property allows computation of only N/2+1 complex values (Sorensen et al., 1987).

**Reference**: Sorensen, H. V., et al. (1987). "Real-valued fast Fourier transform 
algorithms." IEEE Trans. ASSP, 35(6), 849-863

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