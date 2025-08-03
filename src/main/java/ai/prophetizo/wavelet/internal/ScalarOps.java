package ai.prophetizo.wavelet.internal;

/**
 * Scalar implementation of the core wavelet transform operations.
 * This provides a clean, simple implementation without SIMD optimizations.
 * Supports both DWT (with downsampling) and MODWT (without downsampling) operations.
 */
public final class ScalarOps {

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

    // MODWT Operations (Maximal Overlap Discrete Wavelet Transform)
    
    /**
     * Performs MODWT convolution with periodic boundary handling.
     * Unlike DWT, MODWT does not perform downsampling, so output length equals input length.
     * 
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results (same length as signal).
     */
    public static void modwtConvolvePeriodic(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;

        for (int i = 0; i < signalLen; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = (i + j) % signalLen;
                sum += signal[signalIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Performs MODWT convolution with zero padding boundary handling.
     * Unlike DWT, MODWT does not perform downsampling, so output length equals input length.
     * 
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results (same length as signal).
     */
    public static void modwtConvolveZeroPadding(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;

        for (int i = 0; i < signalLen; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = i + j;
                if (signalIndex < signalLen) {
                    sum += signal[signalIndex] * filter[j];
                }
                // Values outside range are implicitly zero
            }

            output[i] = sum;
        }
    }

    /**
     * Scales filter coefficients for MODWT at a given level.
     * For single-level MODWT, the scaling factor is 1/√2.
     * 
     * @param filter The original filter coefficients.
     * @param level The decomposition level (1-based).
     * @return The scaled filter coefficients.
     */
    public static double[] scaleFilterForMODWT(double[] filter, int level) {
        double[] scaledFilter = new double[filter.length];
        double scale = Math.pow(0.5, level / 2.0); // 1/2^(level/2) = 1/√2 for level 1
        
        for (int i = 0; i < filter.length; i++) {
            scaledFilter[i] = filter[i] * scale;
        }
        
        return scaledFilter;
    }
}