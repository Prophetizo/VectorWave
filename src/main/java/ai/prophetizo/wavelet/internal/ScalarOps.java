package ai.prophetizo.wavelet.internal;

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
        if (length <= SMALL_SIGNAL_THRESHOLD && isPowerOfTwo(length)) {
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
        if (outputLen <= SMALL_SIGNAL_THRESHOLD && isPowerOfTwo(outputLen)) {
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
     * Checks if a number is a power of two.
     */
    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

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
        if (lowFilter.length != highFilter.length) {
            throw new IllegalArgumentException("Low and high filters must have the same length");
        }
        
        int filterLen = lowFilter.length;

        if (length <= SMALL_SIGNAL_THRESHOLD && isPowerOfTwo(length)) {
            int lengthMask = length - 1;

            for (int i = 0; i < approxCoeffs.length; i++) {
                double sumLow = 0.0;
                double sumHigh = 0.0;
                int kStart = 2 * i;

                // Process both filters in same loop for cache reuse
                for (int j = 0; j < filterLen; j++) {
                    int localIndex = (kStart + j) & lengthMask;
                    double signalValue = signal[offset + localIndex];
                    sumLow += signalValue * lowFilter[j];
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
}