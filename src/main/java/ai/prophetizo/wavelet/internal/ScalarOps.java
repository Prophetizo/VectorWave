package ai.prophetizo.wavelet.internal;

/**
 * Scalar implementation of the core wavelet transform operations.
 * This provides a clean, simple implementation without SIMD optimizations.
 * 
 * <p>Includes both standard DWT operations (with downsampling/upsampling) and
 * MODWT operations (non-decimated, shift-invariant transforms).</p>
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

    // ==========================================
    // MODWT Operations (Non-decimated)
    // ==========================================

    /**
     * Performs circular convolution without downsampling for MODWT.
     * This is the core operation of the Maximal Overlap Discrete Wavelet Transform.
     * 
     * <p>The MODWT uses circular convolution with the formula:
     * output[t] = Σ(l=0 to L-1) signal[(t + l) mod N] * filter[l]</p>
     * 
     * @param signal The input signal of length N.
     * @param filter The filter coefficients of length L.
     * @param output The output array of length N (same as input).
     */
    public static void circularConvolveMODWT(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // Try different indexing: (t + l) mod N
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
     * @param level         The decomposition level (1-based).
     * @return The scaled filter coefficients.
     */
    public static double[] scaleFilterForMODWT(double[] originalFilter, int level) {
        double scaleFactor = 1.0 / Math.pow(2.0, level / 2.0);
        double[] scaledFilter = new double[originalFilter.length];
        
        for (int i = 0; i < originalFilter.length; i++) {
            scaledFilter[i] = originalFilter[i] * scaleFactor;
        }
        
        return scaledFilter;
    }
}