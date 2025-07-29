package ai.prophetizo.wavelet.cwt.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Optimized in-place FFT implementation with pre-computed twiddle factors.
 * 
 * <p>This class provides significant performance improvements over the basic recursive
 * FFT implementation through:
 * <ul>
 *   <li>In-place computation to reduce memory allocation</li>
 *   <li>Pre-computed and cached twiddle factors for common sizes</li>
 *   <li>Iterative Cooley-Tukey algorithm to avoid recursion overhead</li>
 *   <li>Efficient bit-reversal permutation</li>
 * </ul>
 * </p>
 * 
 * <p>Expected performance improvements: 30-50% speedup with reduced memory allocation
 * and better cache efficiency.</p>
 *
 * @since 1.0.0
 */
public final class OptimizedFFT {
    
    /**
     * Cache for pre-computed twiddle factors indexed by transform size.
     * Uses ConcurrentHashMap for thread-safe access.
     */
    private static final Map<Integer, FFTAcceleratedCWT.Complex[]> TWIDDLE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Maximum size to cache twiddle factors for (to prevent excessive memory usage).
     */
    private static final int MAX_CACHE_SIZE = 8192;
    
    /**
     * Computes in-place FFT with pre-computed twiddle factors.
     * 
     * <p>This method performs the FFT computation directly on the input array,
     * avoiding temporary array allocations. Twiddle factors are pre-computed
     * and cached for efficient reuse.</p>
     * 
     * @param data the complex input data (modified in-place)
     * @throws IllegalArgumentException if data length is not a power of 2
     */
    public static void fftInPlace(FFTAcceleratedCWT.Complex[] data) {
        int n = data.length;
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Data length must be a power of 2.");
        }
        
        if (n == 1) {
            return; // Single element, already transformed
        }
        
        // Bit-reversal permutation
        bitReversePermutation(data);
        
        // Get or compute twiddle factors
        FFTAcceleratedCWT.Complex[] twiddles = getTwiddleFactors(n);
        
        // Iterative Cooley-Tukey FFT
        for (int len = 2; len <= n; len *= 2) {
            int halfLen = len / 2;
            int twiddleStep = n / len;
            
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    int twiddleIndex = j * twiddleStep;
                    FFTAcceleratedCWT.Complex twiddle = twiddles[twiddleIndex];
                    
                    FFTAcceleratedCWT.Complex u = data[i + j];
                    FFTAcceleratedCWT.Complex v = data[i + j + halfLen].multiply(twiddle);
                    
                    data[i + j] = u.add(v);
                    data[i + j + halfLen] = u.subtract(v);
                }
            }
        }
    }
    
    /**
     * Computes in-place IFFT with pre-computed twiddle factors.
     * 
     * <p>This method performs the inverse FFT computation directly on the input array.</p>
     * 
     * @param data the complex frequency-domain data (modified in-place)
     * @throws IllegalArgumentException if data length is not a power of 2
     */
    public static void ifftInPlace(FFTAcceleratedCWT.Complex[] data) {
        int n = data.length;
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Data length must be a power of 2.");
        }
        
        if (n == 1) {
            return; // Single element, already transformed
        }
        
        // Conjugate input
        for (int i = 0; i < n; i++) {
            data[i] = data[i].conjugate();
        }
        
        // Perform FFT
        fftInPlace(data);
        
        // Conjugate and scale result
        double scale = 1.0 / n;
        for (int i = 0; i < n; i++) {
            FFTAcceleratedCWT.Complex conj = data[i].conjugate();
            data[i] = new FFTAcceleratedCWT.Complex(conj.real * scale, conj.imag * scale);
        }
    }
    
    /**
     * Performs bit-reversal permutation in-place.
     * 
     * @param data the array to permute
     */
    private static void bitReversePermutation(FFTAcceleratedCWT.Complex[] data) {
        int n = data.length;
        int bits = Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < n; i++) {
            int j = bitReverse(i, bits);
            if (i < j) {
                // Swap data[i] and data[j]
                FFTAcceleratedCWT.Complex temp = data[i];
                data[i] = data[j];
                data[j] = temp;
            }
        }
    }
    
    /**
     * Computes bit-reversed index.
     * 
     * @param index the original index
     * @param bits the number of bits to reverse
     * @return the bit-reversed index
     */
    private static int bitReverse(int index, int bits) {
        int result = 0;
        for (int i = 0; i < bits; i++) {
            result = (result << 1) | (index & 1);
            index >>= 1;
        }
        return result;
    }
    
    /**
     * Gets or computes twiddle factors for the specified size.
     * 
     * <p>Twiddle factors are cached for sizes up to MAX_CACHE_SIZE to balance
     * memory usage and performance.</p>
     * 
     * @param n the transform size
     * @return the twiddle factors for size n
     */
    private static FFTAcceleratedCWT.Complex[] getTwiddleFactors(int n) {
        // Only cache twiddle factors for reasonable sizes
        if (n <= MAX_CACHE_SIZE) {
            return TWIDDLE_CACHE.computeIfAbsent(n, OptimizedFFT::computeTwiddleFactors);
        } else {
            // For very large sizes, compute on-demand without caching
            return computeTwiddleFactors(n);
        }
    }
    
    /**
     * Computes twiddle factors for the specified size.
     * 
     * <p>Twiddle factors are the complex exponentials e^(-2πik/n) used in the FFT.</p>
     * 
     * @param n the transform size
     * @return the computed twiddle factors
     */
    private static FFTAcceleratedCWT.Complex[] computeTwiddleFactors(int n) {
        FFTAcceleratedCWT.Complex[] twiddles = new FFTAcceleratedCWT.Complex[n];
        double angleStep = -2.0 * Math.PI / n;
        
        for (int k = 0; k < n; k++) {
            double angle = k * angleStep;
            twiddles[k] = new FFTAcceleratedCWT.Complex(Math.cos(angle), Math.sin(angle));
        }
        
        return twiddles;
    }
    
    /**
     * Clears the twiddle factor cache.
     * 
     * <p>This method can be called to free memory if needed, though the cache
     * is designed to have bounded size.</p>
     */
    public static void clearCache() {
        TWIDDLE_CACHE.clear();
    }
    
    /**
     * Gets the current size of the twiddle factor cache.
     * 
     * @return the number of cached twiddle factor arrays
     */
    public static int getCacheSize() {
        return TWIDDLE_CACHE.size();
    }
}