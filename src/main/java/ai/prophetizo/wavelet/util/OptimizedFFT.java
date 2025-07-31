package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.ComplexNumber;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

// IMPORTANT: This import requires JDK with Vector API support.
// The Vector API was introduced as an incubating feature in JDK 16 and became stable in JDK 21.
// At runtime, the code gracefully falls back to scalar implementation if Vector API
// is not available or not functional.
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Highly optimized FFT implementation with advanced algorithms.
 * 
 * <p><strong>Compilation Requirements:</strong></p>
 * <p>This class requires compilation with a JDK that includes the jdk.incubator.vector
 * module (JDK 16 or later). However, at runtime it will gracefully fall back to
 * scalar implementations if the Vector API is not available.</p>
 * 
 * <p>This class provides optimized FFT implementations:</p>
 * <ul>
 *   <li>Radix-2 FFT for power-of-2 sizes with automatic SIMD vectorization</li>
 *   <li>Bluestein's algorithm for arbitrary (non-power-of-2) sizes</li>
 *   <li>Extended twiddle factor caching for performance</li>
 *   <li>Real-to-complex FFT optimization using Hermitian symmetry</li>
 *   <li>Automatic scalar fallback for small signals (n < 256) to avoid vectorization overhead</li>
 * </ul>
 * 
 * <p><strong>References:</strong></p>
 * <ul>
 *   <li>Cooley, J. W., & Tukey, J. W. (1965). "An algorithm for the machine calculation 
 *       of complex Fourier series." Mathematics of computation, 19(90), 297-301.</li>
 *   <li>Bluestein, L. (1970). "A linear filtering approach to the computation 
 *       of discrete Fourier transform." IEEE Transactions on Audio and Electroacoustics.</li>
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
    
    // Performance threshold - use scalar for small signals
    // Benchmark results on various platforms:
    // - macOS ARM64 (vector length 2): scalar faster for all sizes
    // - Linux x86_64 AVX2 (vector length 4): crossover around 256-512
    // - Linux x86_64 AVX-512 (vector length 8): crossover around 128
    // Conservative default: 256 (can be tuned per platform)
    private static final int VECTOR_THRESHOLD = 256;
    
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
        fftOptimized(data, n, inverse, null);
    }
    
    /**
     * Performs optimized FFT with configuration control.
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples
     * @param inverse true for inverse FFT
     * @param config transform configuration (null for auto-detection)
     */
    public static void fftOptimized(double[] data, int n, boolean inverse, TransformConfig config) {
        if (n <= 1) return;
        
        if (PowerOf2Utils.isPowerOf2(n)) {
            // Use radix-2 for all power-of-2 sizes
            // The implementation will automatically use vectorization if available
            fftRadix2(data, n, inverse, config);
        } else {
            // Use Bluestein for arbitrary sizes
            fftBluestein(data, n, inverse);
        }
        
        // Note: Normalization for inverse FFT is handled within the specific implementations
        // (fftRadix2 and fftBluestein) to avoid double normalization
    }
    
    /**
     * Unified radix-2 FFT implementation with automatic vectorization.
     * 
     * <p>This method automatically uses SIMD operations when available,
     * falling back to scalar operations otherwise.</p>
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples (must be power of 2)
     * @param inverse true for inverse FFT
     */
    private static void fftRadix2(double[] data, int n, boolean inverse) {
        fftRadix2(data, n, inverse, null);
    }
    
    private static void fftRadix2(double[] data, int n, boolean inverse, TransformConfig config) {
        boolean forceScalar = config != null && config.isForceScalar();
        boolean forceSIMD = config != null && config.isForceSIMD();
        
        // Determine which implementation to use
        if (forceScalar) {
            // Explicitly forced to use scalar
            fftRadix2Scalar(data, n, inverse);
        } else if (forceSIMD && VECTOR_API_AVAILABLE) {
            // Explicitly forced to use SIMD (ignore threshold)
            fftRadix2Vector(data, n, inverse);
        } else if (!VECTOR_API_AVAILABLE || n < VECTOR_THRESHOLD) {
            // Use scalar for small signals or when vector API not available
            fftRadix2Scalar(data, n, inverse);
        } else {
            // Use vector for large signals when available
            fftRadix2Vector(data, n, inverse);
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
        
        // Normalize for inverse transform to match fftOptimized behavior
        if (inverse) {
            double norm = 1.0 / n;
            for (int i = 0; i < 2 * n; i++) {
                data[i] *= norm;
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
                    
                    // Pre-allocate temporary arrays outside the inner loop to reduce allocation overhead
                    double[] tempReal1 = new double[vecLen];
                    double[] tempImag1 = new double[vecLen];
                    double[] tempReal2 = new double[vecLen];
                    double[] tempImag2 = new double[vecLen];
                    
                    for (; j + vecLen <= halfLen; j += vecLen) {
                        // Load twiddle factors
                        DoubleVector wr = DoubleVector.fromArray(SPECIES, twiddle.cos, j);
                        DoubleVector wi = DoubleVector.fromArray(SPECIES, twiddle.sin, j)
                            .mul(sign);
                        
                        // Gather complex data - unroll for better performance
                        // This manual unrolling can help the JIT compiler optimize better
                        if (vecLen == 2) {
                            // Common case for many platforms
                            int idx1_0 = 2 * (i + j);
                            int idx2_0 = 2 * (i + j + halfLen);
                            int idx1_1 = 2 * (i + j + 1);
                            int idx2_1 = 2 * (i + j + 1 + halfLen);
                            
                            tempReal1[0] = data[idx1_0];
                            tempImag1[0] = data[idx1_0 + 1];
                            tempReal2[0] = data[idx2_0];
                            tempImag2[0] = data[idx2_0 + 1];
                            tempReal1[1] = data[idx1_1];
                            tempImag1[1] = data[idx1_1 + 1];
                            tempReal2[1] = data[idx2_1];
                            tempImag2[1] = data[idx2_1 + 1];
                        } else {
                            // General case
                            for (int k = 0; k < vecLen; k++) {
                                int idx1 = 2 * (i + j + k);
                                int idx2 = 2 * (i + j + k + halfLen);
                                
                                tempReal1[k] = data[idx1];
                                tempImag1[k] = data[idx1 + 1];
                                tempReal2[k] = data[idx2];
                                tempImag2[k] = data[idx2 + 1];
                            }
                        }
                        
                        // Vectorized butterfly computation
                        DoubleVector vReal1 = DoubleVector.fromArray(SPECIES, tempReal1, 0);
                        DoubleVector vImag1 = DoubleVector.fromArray(SPECIES, tempImag1, 0);
                        DoubleVector vReal2 = DoubleVector.fromArray(SPECIES, tempReal2, 0);
                        DoubleVector vImag2 = DoubleVector.fromArray(SPECIES, tempImag2, 0);
                        
                        // Complex multiplication: t = (real2 + i*imag2) * (wr + i*wi)
                        DoubleVector tReal = vReal2.mul(wr).sub(vImag2.mul(wi));
                        DoubleVector tImag = vReal2.mul(wi).add(vImag2.mul(wr));
                        
                        // Butterfly operation
                        DoubleVector resultReal1 = vReal1.add(tReal);
                        DoubleVector resultImag1 = vImag1.add(tImag);
                        DoubleVector resultReal2 = vReal1.sub(tReal);
                        DoubleVector resultImag2 = vImag1.sub(tImag);
                        
                        // Store results - unroll for common case
                        if (vecLen == 2) {
                            // Store directly without temporary arrays for common case
                            int idx1_0 = 2 * (i + j);
                            int idx2_0 = 2 * (i + j + halfLen);
                            int idx1_1 = 2 * (i + j + 1);
                            int idx2_1 = 2 * (i + j + 1 + halfLen);
                            
                            data[idx1_0] = resultReal1.lane(0);
                            data[idx1_0 + 1] = resultImag1.lane(0);
                            data[idx2_0] = resultReal2.lane(0);
                            data[idx2_0 + 1] = resultImag2.lane(0);
                            data[idx1_1] = resultReal1.lane(1);
                            data[idx1_1 + 1] = resultImag1.lane(1);
                            data[idx2_1] = resultReal2.lane(1);
                            data[idx2_1 + 1] = resultImag2.lane(1);
                        } else {
                            // General case - store to temp arrays first
                            resultReal1.intoArray(tempReal1, 0);
                            resultImag1.intoArray(tempImag1, 0);
                            resultReal2.intoArray(tempReal2, 0);
                            resultImag2.intoArray(tempImag2, 0);
                            
                            for (int k = 0; k < vecLen; k++) {
                                int idx1 = 2 * (i + j + k);
                                int idx2 = 2 * (i + j + k + halfLen);
                                
                                data[idx1] = tempReal1[k];
                                data[idx1 + 1] = tempImag1[k];
                                data[idx2] = tempReal2[k];
                                data[idx2 + 1] = tempImag2[k];
                            }
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
            
            // Normalize for inverse transform to match fftRadix2Scalar behavior
            if (inverse) {
                double norm = 1.0 / n;
                for (int i = 0; i < 2 * n; i++) {
                    data[i] *= norm;
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
        fftRadix2(a, m, false);
        fftRadix2(b, m, false);
        
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
        fftRadix2(a, m, false);
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
     * 
     * <p><strong>Implementation Note:</strong></p>
     * <p>The unpacking loop uses k &lt; halfN (not k &lt;= halfN) as the bound.
     * This is mathematically necessary because when k = halfN, the indices
     * k and (halfN - k) become equal, resulting in packed[2 * k] and 
     * packed[2 * (halfN - k)] referring to the same element. This would
     * violate the FFT butterfly operation requirements, which rely on
     * distinct pairs of elements for correct computation.</p>
     * 
     * @param real the real-valued input signal
     * @return the complex FFT result
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
        
        // Runtime validation of the mathematical assumption
        assert packed.length >= 2 * halfN : "Packed array must have at least 2*halfN elements";
        
        // For n=2 (halfN=1), the loop doesn't execute, which is correct
        // since we already handled DC and Nyquist components above
        for (int k = 1; k < halfN; k++) { // Use k < halfN to ensure indices k and halfN - k are distinct, as required by FFT butterfly operations
            // Runtime assertion to validate our boundary condition
            assert k < halfN && (halfN - k) > 0 : "Invalid indices: k=" + k + ", halfN-k=" + (halfN - k);
            assert 2 * k < packed.length && 2 * (halfN - k) < packed.length : 
                "Index out of bounds: 2*k=" + (2 * k) + ", 2*(halfN-k)=" + (2 * (halfN - k));
            
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