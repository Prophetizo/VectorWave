package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.util.ValidationUtils;
import static java.util.Arrays.fill;

/**
 * Scalar implementation of the core wavelet transform operations.
 * <p>
 * This implementation includes optimizations for small signal sizes:
 * - Bitwise operations for power-of-2 modulo arithmetic
 * - Specialized implementations for small filter sizes (Haar, DB2)
 * - Cache-friendly memory access patterns
 * <p>
 * The optimizations are automatically applied based on signal and filter characteristics.
 * <p>
 * <strong>Note on zero coefficient handling:</strong>
 * Throughout this class, exact zero coefficients (coeff == 0.0) are skipped for performance.
 * This is intentional as wavelet transforms preserve exact zeros from sparse or
 * zero-padded signals. Very small coefficients are NOT treated as zero to maintain
 * numerical precision in reconstruction.
 */
public final class ScalarOps {

    private static final int SMALL_SIGNAL_THRESHOLD = 1024;
    
    // Vectorization threshold constants
    private static final int MIN_VECTORIZATION_LENGTH = 32;
    private static final int MEDIUM_ARRAY_THRESHOLD = 128;
    private static final int MAX_FILTER_LENGTH_FOR_MEDIUM_ARRAYS = 8;
    
    /**
     * Flag indicating whether Vector API is available and enabled.
     * Detected at class loading time for optimal performance.
     */
    private static final boolean VECTORIZATION_ENABLED;
    
    static {
        boolean vectorEnabled = false;
        try {
            // Try to access Vector API classes
            Class.forName("jdk.incubator.vector.DoubleVector");
            vectorEnabled = VectorOps.isVectorApiSupported();
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Vector API not available
            vectorEnabled = false;
        }
        VECTORIZATION_ENABLED = vectorEnabled;
    }

// (Removed the internal comment as it has been moved to the class-level Javadoc)

    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling.
     * This is the core operation of the Fast Wavelet Transform.
     *
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     */
    public static void convolveAndDownsamplePeriodic(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodic(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling on a slice.
     * This zero-copy version enables streaming processing without array copying.
     *
     * @param signal The input signal array.
     * @param offset The starting index in the signal array.
     * @param length The number of elements to process.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     * @throws IllegalArgumentException if signal, filter, or output is null
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void convolveAndDownsamplePeriodic(double[] signal, int offset, int length, 
                                                     double[] filter, double[] output) {
        // Validate parameters
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (output.length != length / 2) {
            throw new IllegalArgumentException("Output array must have length " + (length / 2) + 
                ", but has length " + output.length);
        }
        
        int filterLen = filter.length;

        // Use specialized implementations for small filters and signals
        if (length <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(length)) {
            if (filterLen == 2) {
                convolveAndDownsamplePeriodicHaar(signal, offset, length, filter, output);
                return;
            } else if (filterLen == 4) {
                convolveAndDownsamplePeriodicDB2(signal, offset, length, filter, output);
                return;
            }
            // For other filter sizes, use optimized version with bitwise modulo
            convolveAndDownsamplePeriodicOptimized(signal, offset, length, filter, output);
            return;
        }

        // Standard implementation for large signals or non-power-of-2
        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                // Calculate index relative to the slice, wrap within [0, length), then add offset
                int localIndex = (2 * i + j) % length;
                sum += signal[offset + localIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Performs convolution followed by downsampling by 2 with zero padding.
     *
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     */
    public static void convolveAndDownsampleDirect(double[] signal, double[] filter, double[] output) {
        convolveAndDownsampleDirect(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Performs convolution followed by downsampling by 2 with zero padding on a slice.
     * This zero-copy version enables streaming processing without array copying.
     *
     * @param signal The input signal array.
     * @param offset The starting index in the signal array.
     * @param length The number of elements to process.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     * @throws IllegalArgumentException if signal, filter, or output is null
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void convolveAndDownsampleDirect(double[] signal, int offset, int length,
                                                   double[] filter, double[] output) {
        // Validate parameters
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (output.length != length / 2) {
            throw new IllegalArgumentException("Output array must have length " + (length / 2) + 
                ", but has length " + output.length);
        }
        
        int filterLen = filter.length;

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int localIndex = kStart + j;
                if (localIndex < length) {
                    sum += signal[offset + localIndex] * filter[j];
                }
            }

            output[i] = sum;
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with periodic boundary handling.
     * This is the core operation of the inverse Fast Wavelet Transform.
     *
     * @param coeffs The coefficients to upsample and convolve.
     * @param filter The reconstruction filter.
     * @param output The output array.
     */
    public static void upsampleAndConvolvePeriodic(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;

        // Use optimized version for small power-of-2 signals
        if (outputLen <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(outputLen)) {
            if (filterLen == 2) {
                upsampleAndConvolvePeriodicHaar(coeffs, filter, output);
                return;
            }
            // For other filter sizes, use optimized version with bitwise modulo
            upsampleAndConvolvePeriodicOptimized(coeffs, filter, output);
            return;
        }

        // Standard implementation
        // Clear output array
        fill(output, 0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = (baseIndex + j) % outputLen;
                output[outputIndex] += coeff * filter[j];
            }
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with zero padding.
     *
     * @param coeffs The coefficients to upsample and convolve.
     * @param filter The reconstruction filter.
     * @param output The output array.
     */
    public static void upsampleAndConvolveDirect(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;

        // Clear output array
        for (int i = 0; i < outputLen; i++) {
            output[i] = 0.0;
        }

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = baseIndex + j;
                if (outputIndex < outputLen) {
                    output[outputIndex] += coeff * filter[j];
                }
            }
        }
    }

    // ========== Optimized implementations for small signals ==========


    /**
     * Optimized convolution for power-of-2 signal lengths using bitwise modulo.
     */
    private static void convolveAndDownsamplePeriodicOptimized(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicOptimized(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Optimized convolution for power-of-2 signal lengths using bitwise modulo on a slice.
     */
    private static void convolveAndDownsamplePeriodicOptimized(double[] signal, int offset, int length,
                                                               double[] filter, double[] output) {
        int filterLen = filter.length;
        int lengthMask = length - 1; // For bitwise modulo

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int localIndex = (kStart + j) & lengthMask;
                sum += signal[offset + localIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Specialized implementation for Haar wavelet (2 coefficients).
     */
    private static void convolveAndDownsamplePeriodicHaar(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicHaar(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Specialized implementation for Haar wavelet (2 coefficients) on a slice.
     */
    private static void convolveAndDownsamplePeriodicHaar(double[] signal, int offset, int length, 
                                                          double[] filter, double[] output) {
        int lengthMask = length - 1;
        double f0 = filter[0];
        double f1 = filter[1];

        for (int i = 0; i < output.length; i++) {
            int idx0 = (2 * i) & lengthMask;
            int idx1 = (2 * i + 1) & lengthMask;
            output[i] = signal[offset + idx0] * f0 + signal[offset + idx1] * f1;
        }
    }

    /**
     * Specialized implementation for DB2 wavelet (4 coefficients).
     */
    private static void convolveAndDownsamplePeriodicDB2(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicDB2(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Specialized implementation for DB2 wavelet (4 coefficients) on a slice.
     */
    private static void convolveAndDownsamplePeriodicDB2(double[] signal, int offset, int length,
                                                         double[] filter, double[] output) {
        int lengthMask = length - 1;
        double f0 = filter[0];
        double f1 = filter[1];
        double f2 = filter[2];
        double f3 = filter[3];

        for (int i = 0; i < output.length; i++) {
            int base = 2 * i;
            int idx0 = base & lengthMask;
            int idx1 = (base + 1) & lengthMask;
            int idx2 = (base + 2) & lengthMask;
            int idx3 = (base + 3) & lengthMask;

            output[i] = signal[offset + idx0] * f0 + signal[offset + idx1] * f1 +
                    signal[offset + idx2] * f2 + signal[offset + idx3] * f3;
        }
    }

    /**
     * Optimized upsampling for power-of-2 output lengths.
     */
    private static void upsampleAndConvolvePeriodicOptimized(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;
        int outputMask = outputLen - 1; // For bitwise modulo

        // Clear output array
        fill(output, 0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = (baseIndex + j) & outputMask;
                output[outputIndex] += coeff * filter[j];
            }
        }
    }

    /**
     * Specialized upsampling for Haar wavelet.
     */
    private static void upsampleAndConvolvePeriodicHaar(double[] coeffs, double[] filter, double[] output) {
        int outputMask = output.length - 1;
        double f0 = filter[0];
        double f1 = filter[1];

        // Clear output array
        fill(output, 0.0);

        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int base = 2 * i;
            int idx0 = base & outputMask;
            int idx1 = (base + 1) & outputMask;

            output[idx0] += coeff * f0;
            output[idx1] += coeff * f1;
        }
    }

    /**
     * Cache-friendly combined transform for small signals.
     * Processes both low-pass and high-pass filters in a single pass.
     *
     * @param signal       The input signal (must be power-of-2 length).
     * @param lowFilter    The low-pass filter.
     * @param highFilter   The high-pass filter.
     * @param approxCoeffs Output array for approximation coefficients.
     * @param detailCoeffs Output array for detail coefficients.
     */
    public static void combinedTransformPeriodic(double[] signal, double[] lowFilter,
                                                 double[] highFilter, double[] approxCoeffs,
                                                 double[] detailCoeffs) {
        combinedTransformPeriodic(signal, 0, signal.length, lowFilter, highFilter, approxCoeffs, detailCoeffs);
    }
    
    /**
     * Cache-friendly combined transform for small signals on a slice.
     * Processes both low-pass and high-pass filters in a single pass for zero-copy streaming.
     *
     * @param signal       The input signal array.
     * @param offset       The starting index in the signal array.
     * @param length       The number of elements to process (must be power-of-2).
     * @param lowFilter    The low-pass filter.
     * @param highFilter   The high-pass filter.
     * @param approxCoeffs Output array for approximation coefficients.
     * @param detailCoeffs Output array for detail coefficients.
     * @throws IllegalArgumentException if any array is null or output arrays have wrong length
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void combinedTransformPeriodic(double[] signal, int offset, int length,
                                                 double[] lowFilter, double[] highFilter,
                                                 double[] approxCoeffs, double[] detailCoeffs) {
        // Validate parameters
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (lowFilter == null) {
            throw new IllegalArgumentException("Low filter cannot be null");
        }
        if (highFilter == null) {
            throw new IllegalArgumentException("High filter cannot be null");
        }
        if (approxCoeffs == null) {
            throw new IllegalArgumentException("Approximation coefficients array cannot be null");
        }
        if (detailCoeffs == null) {
            throw new IllegalArgumentException("Detail coefficients array cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (approxCoeffs.length != length / 2) {
            throw new IllegalArgumentException("Approximation coefficients array must have length " + 
                (length / 2) + ", but has length " + approxCoeffs.length);
        }
        if (detailCoeffs.length != length / 2) {
            throw new IllegalArgumentException("Detail coefficients array must have length " + 
                (length / 2) + ", but has length " + detailCoeffs.length);
        }
        // Note: For biorthogonal wavelets, low and high filters may have different lengths
        int lowFilterLen = lowFilter.length;
        int highFilterLen = highFilter.length;

        if (length <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(length)) {
            int lengthMask = length - 1;

            for (int i = 0; i < approxCoeffs.length; i++) {
                double sumLow = 0.0;
                double sumHigh = 0.0;
                int kStart = 2 * i;

                // Process low-pass filter
                for (int j = 0; j < lowFilterLen; j++) {
                    int localIndex = (kStart + j) & lengthMask;
                    double signalValue = signal[offset + localIndex];
                    sumLow += signalValue * lowFilter[j];
                }

                // Process high-pass filter
                for (int j = 0; j < highFilterLen; j++) {
                    int localIndex = (kStart + j) & lengthMask;
                    double signalValue = signal[offset + localIndex];
                    sumHigh += signalValue * highFilter[j];
                }

                approxCoeffs[i] = sumLow;
                detailCoeffs[i] = sumHigh;
            }
        } else {
            // Fall back to separate transforms for large signals
            convolveAndDownsamplePeriodic(signal, offset, length, lowFilter, approxCoeffs);
            convolveAndDownsamplePeriodic(signal, offset, length, highFilter, detailCoeffs);
        }
    }

    // ========== Wrapper methods for WaveletOpsFactory compatibility ==========

    /**
     * Wrapper method for periodic convolution and downsampling that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] convolveAndDownsamplePeriodic(double[] signal, double[] filter,
                                                         int signalLength, int filterLength) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[signalLength / 2];
        convolveAndDownsamplePeriodic(signal, filter, output);
        return output;
    }

    /**
     * Wrapper method for zero-padding convolution and downsampling that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] convolveAndDownsampleZeroPadding(double[] signal, double[] filter,
                                                            int signalLength, int filterLength) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[signalLength / 2];
        convolveAndDownsampleDirect(signal, filter, output);
        return output;
    }

    /**
     * Wrapper method for periodic upsampling and convolution that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] upsampleAndConvolvePeriodic(double[] coeffs, double[] filter,
                                                       int coeffsLength, int filterLength) {
        if (coeffs == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[coeffsLength * 2];
        upsampleAndConvolvePeriodic(coeffs, filter, output);
        return output;
    }

    /**
     * Wrapper method for zero-padding upsampling and convolution that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] upsampleAndConvolveZeroPadding(double[] coeffs, double[] filter,
                                                          int coeffsLength, int filterLength) {
        if (coeffs == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[coeffsLength * 2];
        upsampleAndConvolveDirect(coeffs, filter, output);
        return output;
    }
    
    // ==========================================
    // MODWT-specific Operations
    // ==========================================
    
    /**
     * Performs circular convolution for MODWT (Maximal Overlap Discrete Wavelet Transform).
     * This is different from standard DWT convolution as it doesn't downsample.
     * 
     * @param signal The input signal of length N.
     * @param filter The filter coefficients of length L.
     * @param output The output array of length N (same as input).
     */
    public static void circularConvolveMODWT(double[] signal, double[] filter, double[] output) {
        if (VECTORIZATION_ENABLED && shouldUseVectorization(signal.length, filter.length)) {
            VectorOps.circularConvolveMODWTVectorized(signal, filter, output);
        } else {
            circularConvolveMODWTScalar(signal, filter, output);
        }
    }
    
    /**
     * Scalar implementation of circular convolution for MODWT.
     */
    private static void circularConvolveMODWTScalar(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // Circular indexing: (t + l) mod N for periodic boundary conditions
                int signalIndex = (t + l) % signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            
            output[t] = sum;
        }
    }

    /**
     * Performs circular convolution with level-based shift for multi-level MODWT.
     * This handles the modulo operation: (t - 2^(j-1) * l) mod N for level j.
     * 
     * @param signal    The input signal of length N.
     * @param filter    The filter coefficients.
     * @param output    The output array of length N.
     * @param level     The decomposition level (1-based, where level 1 = j=1).
     */
    public static void circularConvolveMODWTLevel(double[] signal, double[] filter, double[] output, int level) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        int shift = 1 << (level - 1); // 2^(j-1) where j = level
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // Level-adjusted circular indexing: (t - 2^(j-1) * l) mod N
                int signalIndex = (t - shift * l + signalLen * filterLen) % signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            
            output[t] = sum;
        }
    }

    /**
     * Scales wavelet filter coefficients for MODWT at a specific level.
     * MODWT uses scaled filters: h_j,l = h_l / 2^(j/2) for level j.
     * 
     * @param originalFilter The original filter coefficients.
     * @param level The decomposition level (1-based).
     * @return The scaled filter coefficients.
     */
    public static double[] scaleFilterForMODWT(double[] originalFilter, int level) {
        double scaleFactor = 1.0 / Math.sqrt(Math.pow(2.0, level));
        double[] scaledFilter = new double[originalFilter.length];
        
        for (int i = 0; i < originalFilter.length; i++) {
            scaledFilter[i] = originalFilter[i] * scaleFactor;
        }
        
        return scaledFilter;
    }
    
    // ==========================================
    // Java 23 Performance Optimizations
    // ==========================================
    
    /**
     * Determines if vectorization should be used based on array characteristics.
     * Uses efficient decision making optimized for Java 23.
     * 
     * @param signalLength The length of the signal array
     * @param filterLength The length of the filter array
     * @return true if vectorization is beneficial
     */
    private static boolean shouldUseVectorization(int signalLength, int filterLength) {
        if (signalLength < MIN_VECTORIZATION_LENGTH) {
            return false;  // Too small for vectorization overhead
        } else if (signalLength >= MIN_VECTORIZATION_LENGTH && signalLength < MEDIUM_ARRAY_THRESHOLD) {
            return filterLength <= MAX_FILTER_LENGTH_FOR_MEDIUM_ARRAYS;  // Medium arrays
        } else if (signalLength >= MEDIUM_ARRAY_THRESHOLD) {
            return true;  // Large arrays benefit from vectorization
        } else {
            return false;
        }
    }
    
    /**
     * Efficiently clears an array using the best available method.
     * Chooses between vectorized and scalar clearing based on array size.
     * 
     * @param array The array to clear
     */
    private static void clearArray(double[] array) {
        if (VECTORIZATION_ENABLED && array.length >= 64) {
            VectorOps.clearArrayVectorized(array);
        } else {
            // Traditional array clearing for small arrays
            for (int i = 0; i < array.length; i++) {
                array[i] = 0.0;
            }
        }
    }
    
    /**
     * Gets performance characteristics for the current system configuration.
     * Useful for monitoring and optimization decisions.
     * 
     * @return Performance information record
     */
    public static PerformanceInfo getPerformanceInfo() {
        return new PerformanceInfo(
            VECTORIZATION_ENABLED,
            VECTORIZATION_ENABLED ? VectorOps.getVectorCapabilities() : null,
            Runtime.getRuntime().availableProcessors()
        );
    }
    
    /**
     * Record containing performance configuration information.
     * Uses Java 17+ record pattern for clean data representation.
     */
    public record PerformanceInfo(
        boolean vectorizationEnabled,
        VectorOps.VectorCapabilityInfo vectorCapabilities,
        int availableProcessors
    ) {
        
        /**
         * Returns a human-readable description of the performance configuration.
         */
        public String description() {
            if (vectorizationEnabled) {
                return "High-performance mode: " + vectorCapabilities.description() + 
                       ", " + availableProcessors + " CPU cores";
            } else {
                return "Scalar mode: Vector API not available, " + 
                       availableProcessors + " CPU cores";
            }
        }
        
        /**
         * Estimates performance improvement for a given workload.
         * 
         * @param arraySize The size of arrays being processed
         * @return Estimated speedup factor
         */
        public double estimateSpeedup(int arraySize) {
            if (vectorizationEnabled) {
                return vectorCapabilities.estimatedSpeedup(arraySize);
            } else {
                return 1.0;  // No speedup in scalar mode
            }
        }
    }
}