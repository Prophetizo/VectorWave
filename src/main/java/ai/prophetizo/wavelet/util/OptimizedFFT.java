package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.cwt.ComplexNumber;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

// IMPORTANT: This import requires JDK with Vector API support (JDK 16+)
// The code will compile only on JDKs that include the jdk.incubator.vector module.
// At runtime, the code gracefully falls back to scalar implementation if Vector API
// is not available or not functional.
import jdk.incubator.vector.*;

/**
 * Highly optimized FFT implementation with advanced algorithms.
 * 
 * <p><strong>Compilation Requirements:</strong></p>
 * <p>This class requires compilation with a JDK that includes the jdk.incubator.vector
 * module (JDK 16 or later). However, at runtime it will gracefully fall back to
 * scalar implementations if the Vector API is not available.</p>
 * 
 * <p>This class extends the basic FFT with:</p>
 * <ul>
 *   <li>Mixed-radix FFT for non-power-of-2 sizes</li>
 *   <li>Split-radix FFT for better performance</li>
 *   <li>SIMD vectorization using Vector API</li>
 *   <li>Extended twiddle factor caching</li>
 *   <li>Real-to-complex FFT optimization</li>
 * </ul>
 * 
 * <p><strong>Canonical References:</strong></p>
 * <ul>
 *   <li>Duhamel, P., & Hollmann, H. (1984). "Split radix FFT algorithm." 
 *       Electronics letters, 20(1), 14-16.</li>
 *   <li>Bluestein, L. (1970). "A linear filtering approach to the computation 
 *       of discrete Fourier transform." IEEE Transactions on Audio and Electroacoustics.</li>
 *   <li>Johnson, S. G., & Frigo, M. (2007). "A modified split-radix FFT with 
 *       fewer arithmetic operations." IEEE Trans. Signal Processing, 55(1), 111-119.</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public final class OptimizedFFT {
    
    // Extended twiddle factor cache
    private static final int MAX_CACHED_SIZE = 65536; // Up to 64K
    private static final ConcurrentHashMap<Integer, TwiddleFactors> TWIDDLE_CACHE = 
        new ConcurrentHashMap<>();
    
    // Vector API availability check
    private static final boolean VECTOR_API_AVAILABLE;
    private static final VectorSpecies<Double> SPECIES;
    
    static {
        boolean vectorAvailable = false;
        VectorSpecies<Double> speciesTemp = null;
        
        try {
            // Try to access Vector API
            speciesTemp = DoubleVector.SPECIES_PREFERRED;
            // Verify it works by creating a zero vector
            DoubleVector.zero(speciesTemp);
            vectorAvailable = true;
        } catch (Throwable e) {
            // Vector API not available or not functional
            // Use scalar fallback
        }
        
        VECTOR_API_AVAILABLE = vectorAvailable;
        SPECIES = speciesTemp;
    }
    
    /**
     * Cached twiddle factors for a specific FFT size.
     */
    private static class TwiddleFactors {
        final double[] cos;
        final double[] sin;
        
        TwiddleFactors(int n) {
            int halfN = n / 2;
            cos = new double[halfN];
            sin = new double[halfN];
            
            for (int k = 0; k < halfN; k++) {
                double angle = -2.0 * Math.PI * k / n;
                cos[k] = Math.cos(angle);
                sin[k] = Math.sin(angle);
            }
        }
    }
    
    /**
     * Performs optimized FFT with automatic algorithm selection.
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples
     * @param inverse true for inverse FFT
     */
    public static void fftOptimized(double[] data, int n, boolean inverse) {
        if (n <= 1) return;
        
        if (PowerOf2Utils.isPowerOf2(n)) {
            // Use split-radix for power-of-2
            if (n >= 32) {
                fftSplitRadix(data, n, inverse);
            } else {
                if (VECTOR_API_AVAILABLE) {
                    fftRadix2Vector(data, n, inverse);
                } else {
                    fftRadix2Scalar(data, n, inverse);
                }
            }
        } else {
            // Use Bluestein for arbitrary sizes
            fftBluestein(data, n, inverse);
        }
        
        // Normalize for inverse transform
        if (inverse) {
            double norm = 1.0 / n;
            for (int i = 0; i < 2 * n; i++) {
                data[i] *= norm;
            }
        }
    }
    
    /**
     * Split-radix FFT for improved performance.
     * 
     * <p>Split-radix combines radix-2 and radix-4 to minimize operations.
     * It requires ~25% fewer operations than pure radix-2.</p>
     */
    private static void fftSplitRadix(double[] data, int n, boolean inverse) {
        // Bit reversal
        bitReverseVector(data, n);
        
        // Split-radix butterflies
        int sign = inverse ? 1 : -1;
        
        // Stage 1: Handle n=2 base case
        for (int i = 0; i < n; i += 2) {
            int i1 = 2 * i;
            int i2 = i1 + 2;
            double xr = data[i2];
            double xi = data[i2 + 1];
            data[i2] = data[i1] - xr;
            data[i2 + 1] = data[i1 + 1] - xi;
            data[i1] += xr;
            data[i1 + 1] += xi;
        }
        
        // Remaining stages
        for (int len = 4; len <= n; len <<= 1) {
            int halfLen = len >> 1;
            int quarterLen = len >> 2;
            
            TwiddleFactors twiddle = getTwiddleFactors(len);
            
            // Radix-2 butterflies
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < quarterLen; j++) {
                    int idx1 = 2 * (i + j);
                    int idx2 = 2 * (i + j + halfLen);
                    
                    double wr = twiddle.cos[j];
                    double wi = sign * twiddle.sin[j];
                    
                    double xr = data[idx2] * wr - data[idx2 + 1] * wi;
                    double xi = data[idx2] * wi + data[idx2 + 1] * wr;
                    
                    data[idx2] = data[idx1] - xr;
                    data[idx2 + 1] = data[idx1 + 1] - xi;
                    data[idx1] += xr;
                    data[idx1 + 1] += xi;
                }
            }
            
            // Radix-4 butterflies for odd indices
            if (quarterLen > 1) {
                for (int i = 0; i < n; i += len) {
                    for (int j = 1; j < quarterLen; j += 2) {
                        int idx0 = 2 * (i + j);
                        int idx1 = 2 * (i + j + quarterLen);
                        int idx2 = 2 * (i + j + halfLen);
                        int idx3 = 2 * (i + j + halfLen + quarterLen);
                        
                        // Complex butterflies with twiddle factors
                        // For split-radix, we need to compute twiddle factors directly
                        double angle1 = -2.0 * Math.PI * j / len;
                        double angle2 = -2.0 * Math.PI * (2 * j) / len;
                        double angle3 = -2.0 * Math.PI * (3 * j) / len;
                        
                        if (inverse) {
                            angle1 = -angle1;
                            angle2 = -angle2;
                            angle3 = -angle3;
                        }
                        
                        double w1r = Math.cos(angle1);
                        double w1i = Math.sin(angle1);
                        double w2r = Math.cos(angle2);
                        double w2i = Math.sin(angle2);
                        double w3r = Math.cos(angle3);
                        double w3i = Math.sin(angle3);
                        
                        // Apply twiddle factors
                        double x1r = data[idx1] * w1r - data[idx1 + 1] * w1i;
                        double x1i = data[idx1] * w1i + data[idx1 + 1] * w1r;
                        double x2r = data[idx2] * w2r - data[idx2 + 1] * w2i;
                        double x2i = data[idx2] * w2i + data[idx2 + 1] * w2r;
                        double x3r = data[idx3] * w3r - data[idx3 + 1] * w3i;
                        double x3i = data[idx3] * w3i + data[idx3 + 1] * w3r;
                        
                        // Radix-4 butterfly
                        double t0r = data[idx0] + x2r;
                        double t0i = data[idx0 + 1] + x2i;
                        double t1r = data[idx0] - x2r;
                        double t1i = data[idx0 + 1] - x2i;
                        double t2r = x1r + x3r;
                        double t2i = x1i + x3i;
                        double t3r = sign * (x1i - x3i);
                        double t3i = -sign * (x1r - x3r);
                        
                        data[idx0] = t0r + t2r;
                        data[idx0 + 1] = t0i + t2i;
                        data[idx1] = t1r + t3r;
                        data[idx1 + 1] = t1i + t3i;
                        data[idx2] = t0r - t2r;
                        data[idx2 + 1] = t0i - t2i;
                        data[idx3] = t1r - t3r;
                        data[idx3 + 1] = t1i - t3i;
                    }
                }
            }
        }
    }
    
    /**
     * Scalar radix-2 FFT implementation (fallback when Vector API is not available).
     * Package-private for testing.
     */
    static void fftRadix2Scalar(double[] data, int n, boolean inverse) {
        bitReverseVector(data, n);
        
        int sign = inverse ? 1 : -1;
        
        for (int len = 2; len <= n; len <<= 1) {
            int halfLen = len >> 1;
            TwiddleFactors twiddle = getTwiddleFactors(len);
            
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    int idx1 = 2 * (i + j);
                    int idx2 = 2 * (i + j + halfLen);
                    
                    double wr = twiddle.cos[j];
                    double wi = sign * twiddle.sin[j];
                    
                    double xr = data[idx2] * wr - data[idx2 + 1] * wi;
                    double xi = data[idx2] * wi + data[idx2 + 1] * wr;
                    
                    data[idx2] = data[idx1] - xr;
                    data[idx2 + 1] = data[idx1 + 1] - xi;
                    data[idx1] += xr;
                    data[idx1 + 1] += xi;
                }
            }
        }
    }
    
    /**
     * Vectorized radix-2 FFT using SIMD operations.
     */
    private static void fftRadix2Vector(double[] data, int n, boolean inverse) {
        if (!VECTOR_API_AVAILABLE) {
            fftRadix2Scalar(data, n, inverse);
            return;
        }
        
        try {
            bitReverseVector(data, n);
            
            int sign = inverse ? 1 : -1;
            int vecLen = SPECIES.length();
            
            for (int len = 2; len <= n; len <<= 1) {
                int halfLen = len >> 1;
                TwiddleFactors twiddle = getTwiddleFactors(len);
                
                for (int i = 0; i < n; i += len) {
                    // Vectorized loop for j - process elements in chunks
                    int j = 0;
                    for (; j + vecLen <= halfLen; j += vecLen) {
                        // Load twiddle factors
                        DoubleVector wr = DoubleVector.fromArray(SPECIES, twiddle.cos, j);
                        DoubleVector wi = DoubleVector.fromArray(SPECIES, twiddle.sin, j)
                            .mul(sign);
                        
                        // Process vecLen butterfly operations using vectorized computation
                        // For efficiency, we'll use temporary arrays to batch the operations
                        double[] tempReal1 = new double[vecLen];
                        double[] tempImag1 = new double[vecLen];
                        double[] tempReal2 = new double[vecLen];
                        double[] tempImag2 = new double[vecLen];
                        
                        // Gather data for vectorization (minimize scatter-gather overhead)
                        for (int k = 0; k < vecLen; k++) {
                            int idx1 = 2 * (i + j + k);
                            int idx2 = 2 * (i + j + k + halfLen);
                            
                            tempReal1[k] = data[idx1];
                            tempImag1[k] = data[idx1 + 1];
                            tempReal2[k] = data[idx2];
                            tempImag2[k] = data[idx2 + 1];
                        }
                        
                        // Vectorized butterfly computation
                        DoubleVector vReal1 = DoubleVector.fromArray(SPECIES, tempReal1, 0);
                        DoubleVector vImag1 = DoubleVector.fromArray(SPECIES, tempImag1, 0);
                        DoubleVector vReal2 = DoubleVector.fromArray(SPECIES, tempReal2, 0);
                        DoubleVector vImag2 = DoubleVector.fromArray(SPECIES, tempImag2, 0);
                        
                        // Complex multiplication: t = (real2 + i*imag2) * (wr + i*wi)
                        DoubleVector tReal = vReal2.mul(wr).sub(vImag2.mul(wi));
                        DoubleVector tImag = vReal2.mul(wi).add(vImag2.mul(wr));
                        
                        // Butterfly operation: 
                        // First pair:  (real1, imag1) = (real1, imag1) + t
                        // Second pair: (real2, imag2) = (real1, imag1) - t
                        DoubleVector resultReal1 = vReal1.add(tReal);
                        DoubleVector resultImag1 = vImag1.add(tImag);
                        DoubleVector resultReal2 = vReal1.sub(tReal);
                        DoubleVector resultImag2 = vImag1.sub(tImag);
                        
                        // Store results back to temporary arrays
                        resultReal1.intoArray(tempReal1, 0);
                        resultImag1.intoArray(tempImag1, 0);
                        resultReal2.intoArray(tempReal2, 0);
                        resultImag2.intoArray(tempImag2, 0);
                        
                        // Scatter results back to original data array
                        for (int k = 0; k < vecLen; k++) {
                            int idx1 = 2 * (i + j + k);
                            int idx2 = 2 * (i + j + k + halfLen);
                            
                            data[idx1] = tempReal1[k];
                            data[idx1 + 1] = tempImag1[k];
                            data[idx2] = tempReal2[k];
                            data[idx2 + 1] = tempImag2[k];
                        }
                    }
                    
                    // Handle remaining elements
                    for (; j < halfLen; j++) {
                        int idx1 = 2 * (i + j);
                        int idx2 = 2 * (i + j + halfLen);
                        
                        double wr = twiddle.cos[j];
                        double wi = sign * twiddle.sin[j];
                        
                        double xr = data[idx2] * wr - data[idx2 + 1] * wi;
                        double xi = data[idx2] * wi + data[idx2 + 1] * wr;
                        
                        data[idx2] = data[idx1] - xr;
                        data[idx2 + 1] = data[idx1 + 1] - xi;
                        data[idx1] += xr;
                        data[idx1 + 1] += xi;
                    }
                }
            }
        } catch (Throwable e) {
            // Fall back to scalar implementation
            fftRadix2Scalar(data, n, inverse);
        }
    }
    
    /**
     * Bluestein's algorithm for arbitrary-size FFT.
     * 
     * <p>Converts arbitrary-size DFT to convolution, which can be
     * computed using power-of-2 FFT.</p>
     */
    private static void fftBluestein(double[] data, int n, boolean inverse) {
        // Find next power of 2 >= 2n - 1 using bit operations for optimal performance
        int target = 2 * n - 1;
        int m = PowerOf2Utils.nextPowerOf2(target);
        
        // Chirp sequence
        double[] chirp = new double[2 * n];
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * i * i / n;
            if (inverse) angle = -angle;
            chirp[2 * i] = Math.cos(angle);
            chirp[2 * i + 1] = Math.sin(angle);
        }
        
        // Prepare sequences for convolution
        double[] a = new double[2 * m];
        double[] b = new double[2 * m];
        
        // a[k] = x[k] * chirp[k]
        for (int i = 0; i < n; i++) {
            a[2 * i] = data[2 * i] * chirp[2 * i] - data[2 * i + 1] * chirp[2 * i + 1];
            a[2 * i + 1] = data[2 * i] * chirp[2 * i + 1] + data[2 * i + 1] * chirp[2 * i];
        }
        
        // b[k] = conjugate(chirp[k]) for k = 0..n-1
        // b[k] = conjugate(chirp[m-k]) for k = m-n+1..m-1
        for (int i = 0; i < n; i++) {
            b[2 * i] = chirp[2 * i];
            b[2 * i + 1] = -chirp[2 * i + 1];
        }
        for (int i = 1; i < n; i++) {
            b[2 * (m - i)] = chirp[2 * i];
            b[2 * (m - i) + 1] = -chirp[2 * i + 1];
        }
        
        // Convolve using FFT
        if (VECTOR_API_AVAILABLE) {
            fftRadix2Vector(a, m, false);
            fftRadix2Vector(b, m, false);
        } else {
            fftRadix2Scalar(a, m, false);
            fftRadix2Scalar(b, m, false);
        }
        
        // Multiply in frequency domain
        for (int i = 0; i < m; i++) {
            int idx = 2 * i;
            double ar = a[idx];
            double ai = a[idx + 1];
            double br = b[idx];
            double bi = b[idx + 1];
            a[idx] = ar * br - ai * bi;
            a[idx + 1] = ar * bi + ai * br;
        }
        
        // Inverse FFT (use false to avoid double normalization)
        if (VECTOR_API_AVAILABLE) {
            fftRadix2Vector(a, m, false);
        } else {
            fftRadix2Scalar(a, m, false);
        }
        // Apply conjugation and scaling manually for inverse
        double scale = 1.0 / m;
        for (int i = 0; i < m; i++) {
            double temp = a[2 * i + 1];
            a[2 * i] *= scale;
            a[2 * i + 1] = -temp * scale;
        }
        
        // Extract result: y[k] = a[k] * chirp[k]
        for (int i = 0; i < n; i++) {
            data[2 * i] = a[2 * i] * chirp[2 * i] - a[2 * i + 1] * chirp[2 * i + 1];
            data[2 * i + 1] = a[2 * i] * chirp[2 * i + 1] + a[2 * i + 1] * chirp[2 * i];
        }
    }
    
    /**
     * Optimized bit-reversal using vectorization.
     */
    private static void bitReverseVector(double[] data, int n) {
        int shift = 32 - Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> shift;
            if (i < j) {
                // Swap complex numbers
                int idx1 = 2 * i;
                int idx2 = 2 * j;
                double tr = data[idx1];
                double ti = data[idx1 + 1];
                data[idx1] = data[idx2];
                data[idx1 + 1] = data[idx2 + 1];
                data[idx2] = tr;
                data[idx2 + 1] = ti;
            }
        }
    }
    
    /**
     * Real-to-complex FFT using half the operations.
     * 
     * <p>Exploits Hermitian symmetry to compute FFT of real signal
     * using N/2 complex FFT.</p>
     */
    public static ComplexNumber[] fftRealOptimized(double[] real) {
        int n = real.length;
        if (n == 0) {
            return new ComplexNumber[0];
        }
        if (n == 1) {
            return new ComplexNumber[]{new ComplexNumber(real[0], 0)};
        }
        
        // For odd-length arrays, use standard FFT
        if (n % 2 != 0) {
            double[] complexData = new double[2 * n];
            for (int i = 0; i < n; i++) {
                complexData[2 * i] = real[i];
                complexData[2 * i + 1] = 0;
            }
            fftOptimized(complexData, n, false);
            ComplexNumber[] result = new ComplexNumber[n];
            for (int i = 0; i < n; i++) {
                result[i] = new ComplexNumber(complexData[2 * i], complexData[2 * i + 1]);
            }
            return result;
        }
        
        // Pack real data into complex array (even in real, odd in imag)
        int halfN = n / 2;
        double[] packed = new double[2 * halfN];
        for (int i = 0; i < halfN; i++) {
            packed[2 * i] = real[2 * i];
            packed[2 * i + 1] = real[2 * i + 1];
        }
        
        // Compute FFT of half-size
        fftOptimized(packed, halfN, false);
        
        // Unpack using symmetry properties
        ComplexNumber[] result = new ComplexNumber[n];
        result[0] = new ComplexNumber(
            packed[0] + packed[1], 
            0
        );
        result[halfN] = new ComplexNumber(
            packed[0] - packed[1], 
            0
        );
        
        for (int k = 1; k < halfN; k++) {
            // IMPORTANT: Loop bound must be k < halfN (not k <= halfN)
            // Mathematical reasoning: When k = halfN, the indices k and (halfN - k)
            // become equal, resulting in packed[2 * k] and packed[2 * (halfN - k)]
            // referring to the same element. This violates the FFT butterfly operation
            // requirements, which rely on distinct pairs of elements for correct computation.
            double wr = Math.cos(Math.PI * k / halfN);
            double wi = -Math.sin(Math.PI * k / halfN);
            
            // G[k] = FFT(even indexed)
            // H[k] = FFT(odd indexed)
            double gr = 0.5 * (packed[2 * k] + packed[2 * (halfN - k)]);
            double gi = 0.5 * (packed[2 * k + 1] - packed[2 * (halfN - k) + 1]);
            double hr = 0.5 * (packed[2 * k + 1] + packed[2 * (halfN - k) + 1]);
            double hi = -0.5 * (packed[2 * k] - packed[2 * (halfN - k)]);
            
            // Apply twiddle factor to H[k]
            double tr = hr * wr - hi * wi;
            double ti = hr * wi + hi * wr;
            
            // X[k] = G[k] + W * H[k]
            result[k] = new ComplexNumber(gr + tr, gi + ti);
            // X[n-k] = conj(G[k] - W * H[k])
            result[n - k] = new ComplexNumber(gr - tr, -(gi - ti));
        }
        
        return result;
    }
    
    /**
     * Get cached twiddle factors or compute if not cached.
     */
    private static TwiddleFactors getTwiddleFactors(int n) {
        if (n <= MAX_CACHED_SIZE) {
            return TWIDDLE_CACHE.computeIfAbsent(n, TwiddleFactors::new);
        }
        return new TwiddleFactors(n);
    }
    
    
    /**
     * Returns true if Vector API is available and functional on this platform.
     * 
     * @return true if Vector API is available, false otherwise
     */
    public static boolean isVectorApiAvailable() {
        return VECTOR_API_AVAILABLE;
    }
    
    /**
     * Returns information about Vector API availability.
     * 
     * @return a string describing the Vector API status
     */
    public static String getVectorApiInfo() {
        if (VECTOR_API_AVAILABLE && SPECIES != null) {
            return "Vector API available with vector length: " + SPECIES.length();
        } else {
            return "Vector API not available - using scalar fallback";
        }
    }
    
    private OptimizedFFT() {}
}