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
 */
public final class ScalarOps {

    private static final int SMALL_SIGNAL_THRESHOLD = 1024;

    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling.
     * This is the core operation of the Fast Wavelet Transform.
     *
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     */
    public static void convolveAndDownsamplePeriodic(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;

        // Use specialized implementations for small filters and signals
        if (signalLen <= SMALL_SIGNAL_THRESHOLD && isPowerOfTwo(signalLen)) {
            if (filterLen == 2) {
                convolveAndDownsamplePeriodicHaar(signal, filter, output);
                return;
            } else if (filterLen == 4) {
                convolveAndDownsamplePeriodicDB2(signal, filter, output);
                return;
            }
            // For other filter sizes, use optimized version with bitwise modulo
            convolveAndDownsamplePeriodicOptimized(signal, filter, output);
            return;
        }

        // Standard implementation for large signals or non-power-of-2
        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = (kStart + j) % signalLen;
                sum += signal[signalIndex] * filter[j];
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
        int signalLen = signal.length;
        int filterLen = filter.length;

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = kStart + j;
                if (signalIndex < signalLen) {
                    sum += signal[signalIndex] * filter[j];
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
            if (coeff == 0.0) continue;  // Skip zero coefficients

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
            if (coeff == 0.0) continue;  // Skip zero coefficients

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
        int signalLen = signal.length;
        int filterLen = filter.length;
        int signalMask = signalLen - 1; // For bitwise modulo

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = (kStart + j) & signalMask;
                sum += signal[signalIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Specialized implementation for Haar wavelet (2 coefficients).
     */
    private static void convolveAndDownsamplePeriodicHaar(double[] signal, double[] filter, double[] output) {
        int signalMask = signal.length - 1;
        double f0 = filter[0];
        double f1 = filter[1];

        for (int i = 0; i < output.length; i++) {
            int idx0 = (2 * i) & signalMask;
            int idx1 = (2 * i + 1) & signalMask;
            output[i] = signal[idx0] * f0 + signal[idx1] * f1;
        }
    }

    /**
     * Specialized implementation for DB2 wavelet (4 coefficients).
     */
    private static void convolveAndDownsamplePeriodicDB2(double[] signal, double[] filter, double[] output) {
        int signalMask = signal.length - 1;
        double f0 = filter[0];
        double f1 = filter[1];
        double f2 = filter[2];
        double f3 = filter[3];

        for (int i = 0; i < output.length; i++) {
            int base = 2 * i;
            int idx0 = base & signalMask;
            int idx1 = (base + 1) & signalMask;
            int idx2 = (base + 2) & signalMask;
            int idx3 = (base + 3) & signalMask;

            output[i] = signal[idx0] * f0 + signal[idx1] * f1 +
                    signal[idx2] * f2 + signal[idx3] * f3;
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
            if (Double.compare(coeff, 0.0) == 0) continue;  // Skip zero coefficients

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
            if (coeff == 0.0) continue;

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
        int signalLen = signal.length;
        int filterLen = lowFilter.length;

        if (signalLen <= SMALL_SIGNAL_THRESHOLD && isPowerOfTwo(signalLen)) {
            int signalMask = signalLen - 1;

            for (int i = 0; i < approxCoeffs.length; i++) {
                double sumLow = 0.0;
                double sumHigh = 0.0;
                int kStart = 2 * i;

                // Process both filters in same loop for cache reuse
                for (int j = 0; j < filterLen; j++) {
                    int signalIndex = (kStart + j) & signalMask;
                    double signalValue = signal[signalIndex];
                    sumLow += signalValue * lowFilter[j];
                    sumHigh += signalValue * highFilter[j];
                }

                approxCoeffs[i] = sumLow;
                detailCoeffs[i] = sumHigh;
            }
        } else {
            // Fall back to separate transforms for large signals
            convolveAndDownsamplePeriodic(signal, lowFilter, approxCoeffs);
            convolveAndDownsamplePeriodic(signal, highFilter, detailCoeffs);
        }
    }
}