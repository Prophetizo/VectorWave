package ai.prophetizo.wavelet.memory;

/**
 * Memory-aware implementation of wavelet transform operations using ManagedArrays.
 * 
 * <p>This class provides the same core wavelet operations as ScalarOps but works
 * with ManagedArrays, allowing for both heap and off-heap memory management.</p>
 */
public final class ManagedScalarOps {

    private ManagedScalarOps() {
        // Utility class
    }

    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling.
     * This is the core operation of the Fast Wavelet Transform.
     *
     * @param signal The input signal as a ManagedArray
     * @param filter The filter (wavelet coefficients) to apply
     * @param output The ManagedArray to store the results
     */
    public static void convolveAndDownsamplePeriodic(ManagedArray signal, double[] filter, ManagedArray output) {
        int signalLen = signal.length();
        int filterLen = filter.length;

        for (int i = 0; i < output.length(); i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = (kStart + j) % signalLen;
                sum += signal.get(signalIndex) * filter[j];
            }

            output.set(i, sum);
        }
    }

    /**
     * Performs convolution followed by downsampling by 2 with zero padding.
     *
     * @param signal The input signal as a ManagedArray
     * @param filter The filter (wavelet coefficients) to apply
     * @param output The ManagedArray to store the results
     */
    public static void convolveAndDownsampleDirect(ManagedArray signal, double[] filter, ManagedArray output) {
        int signalLen = signal.length();
        int filterLen = filter.length;

        for (int i = 0; i < output.length(); i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = kStart + j;
                if (signalIndex < signalLen) {
                    sum += signal.get(signalIndex) * filter[j];
                }
            }

            output.set(i, sum);
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with periodic boundary handling.
     * This is the core operation of the inverse Fast Wavelet Transform.
     *
     * @param coeffs The coefficients to upsample and convolve as a ManagedArray
     * @param filter The reconstruction filter
     * @param output The output ManagedArray
     */
    public static void upsampleAndConvolvePeriodic(ManagedArray coeffs, double[] filter, ManagedArray output) {
        int outputLen = output.length();
        int filterLen = filter.length;

        // Clear output array
        output.fill(0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length(); i++) {
            double coeff = coeffs.get(i);
            if (coeff == 0.0) continue;  // Skip zero coefficients

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = (baseIndex + j) % outputLen;
                output.set(outputIndex, output.get(outputIndex) + coeff * filter[j]);
            }
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with zero padding.
     *
     * @param coeffs The coefficients to upsample and convolve as a ManagedArray
     * @param filter The reconstruction filter
     * @param output The output ManagedArray
     */
    public static void upsampleAndConvolveDirect(ManagedArray coeffs, double[] filter, ManagedArray output) {
        int outputLen = output.length();
        int filterLen = filter.length;

        // Clear output array
        output.fill(0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length(); i++) {
            double coeff = coeffs.get(i);
            if (coeff == 0.0) continue;  // Skip zero coefficients

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = baseIndex + j;
                if (outputIndex < outputLen) {
                    output.set(outputIndex, output.get(outputIndex) + coeff * filter[j]);
                }
            }
        }
    }

    /**
     * Optimized version of convolveAndDownsamplePeriodic for SIMD-aligned off-heap arrays.
     * Falls back to the standard implementation if arrays are not suitable for optimization.
     *
     * @param signal The input signal as a ManagedArray
     * @param filter The filter (wavelet coefficients) to apply
     * @param output The ManagedArray to store the results
     */
    public static void convolveAndDownsamplePeriodicOptimized(ManagedArray signal, double[] filter, ManagedArray output) {
        // Check if both arrays are off-heap and aligned for potential SIMD optimization
        if (signal.isOffHeap() && output.isOffHeap() &&
            signal.alignment() >= 32 && output.alignment() >= 32 &&
            filter.length <= 16) { // Reasonable filter size for vectorization
            
            // Potential SIMD optimization could be implemented here
            // For now, use the standard implementation
            convolveAndDownsamplePeriodic(signal, filter, output);
        } else {
            // Use standard implementation
            convolveAndDownsamplePeriodic(signal, filter, output);
        }
    }

    /**
     * Optimized version of upsampleAndConvolvePeriodic for SIMD-aligned off-heap arrays.
     * Falls back to the standard implementation if arrays are not suitable for optimization.
     *
     * @param coeffs The coefficients to upsample and convolve as a ManagedArray
     * @param filter The reconstruction filter
     * @param output The output ManagedArray
     */
    public static void upsampleAndConvolvePeriodicOptimized(ManagedArray coeffs, double[] filter, ManagedArray output) {
        // Check if both arrays are off-heap and aligned for potential SIMD optimization
        if (coeffs.isOffHeap() && output.isOffHeap() &&
            coeffs.alignment() >= 32 && output.alignment() >= 32 &&
            filter.length <= 16) { // Reasonable filter size for vectorization
            
            // Potential SIMD optimization could be implemented here
            // For now, use the standard implementation
            upsampleAndConvolvePeriodic(coeffs, filter, output);
        } else {
            // Use standard implementation
            upsampleAndConvolvePeriodic(coeffs, filter, output);
        }
    }
}