package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.internal.ScalarOps;

/**
 * Public facade for wavelet transform operations.
 * 
 * <p>This class provides access to the core wavelet operations needed by
 * transform implementations while hiding internal implementation details.</p>
 * 
 */
public final class WaveletOperations {
    
    private WaveletOperations() {
        // Static utility class
    }
    
    /**
     * Performs circular convolution for MODWT without downsampling.
     * 
     * @param signal input signal
     * @param filter wavelet filter coefficients
     * @param output pre-allocated output array (same length as signal)
     */
    public static void circularConvolveMODWT(double[] signal, double[] filter, double[] output) {
        ScalarOps.circularConvolveMODWT(signal, filter, output);
    }
    
    /**
     * Performs zero-padding convolution for MODWT without downsampling.
     * 
     * @param signal input signal
     * @param filter wavelet filter coefficients  
     * @param output pre-allocated output array (same length as signal)
     */
    public static void zeroPaddingConvolveMODWT(double[] signal, double[] filter, double[] output) {
        ScalarOps.zeroPaddingConvolveMODWT(signal, filter, output);
    }
    
    /**
     * Gets performance information about the current platform's capabilities.
     * 
     * @return performance information including vectorization status
     */
    public static PerformanceInfo getPerformanceInfo() {
        ScalarOps.PerformanceInfo internal = ScalarOps.getPerformanceInfo();
        String platformName = System.getProperty("os.arch", "unknown");
        String vectorSpecies = internal.vectorizationEnabled() 
            ? internal.vectorCapabilities().shape() 
            : "N/A";
        String processingHint = internal.vectorizationEnabled()
            ? "Use signals >= " + internal.vectorCapabilities().threshold() + " for best performance"
            : "Vector API not available";
        
        return new PerformanceInfo(
            internal.vectorizationEnabled(),
            platformName,
            vectorSpecies,
            processingHint
        );
    }
    
    /**
     * Applies soft thresholding to wavelet coefficients.
     * 
     * <p>Soft thresholding shrinks coefficients toward zero by the threshold amount:
     * <ul>
     *   <li>If |x| ≤ threshold: result = 0</li>
     *   <li>If x > threshold: result = x - threshold</li>
     *   <li>If x < -threshold: result = x + threshold</li>
     * </ul>
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value (must be non-negative)
     * @return new array with thresholded coefficients
     * @throws IllegalArgumentException if threshold is negative
     */
    public static double[] softThreshold(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients array cannot be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        return ScalarOps.softThreshold(coefficients, threshold);
    }
    
    /**
     * Applies hard thresholding to wavelet coefficients.
     * 
     * <p>Hard thresholding sets coefficients to zero if their absolute value
     * is less than or equal to the threshold:
     * <ul>
     *   <li>If |x| ≤ threshold: result = 0</li>
     *   <li>If |x| > threshold: result = x</li>
     * </ul>
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value (must be non-negative)
     * @return new array with thresholded coefficients
     * @throws IllegalArgumentException if threshold is negative
     */
    public static double[] hardThreshold(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients array cannot be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        return ScalarOps.hardThreshold(coefficients, threshold);
    }
    
    /**
     * Performance information about wavelet operations on this platform.
     */
    public record PerformanceInfo(
        boolean vectorizationEnabled,
        String platformName,
        String vectorSpecies,
        String processingHint
    ) {
        /**
         * Returns a human-readable description of the performance capabilities.
         */
        public String description() {
            if (vectorizationEnabled) {
                return String.format("Vectorized operations enabled on %s with %s. %s",
                    platformName, vectorSpecies, processingHint);
            } else {
                return String.format("Scalar operations on %s. %s", 
                    platformName, processingHint);
            }
        }
        
        /**
         * Estimates the potential speedup for a given signal length.
         * 
         * @param signalLength the length of the signal
         * @return estimated speedup factor
         */
        public double estimateSpeedup(int signalLength) {
            if (signalLength < 0) {
                throw new IllegalArgumentException("Signal length cannot be negative");
            }
            if (!vectorizationEnabled || signalLength < 64) {
                return 1.0;
            }
            // Simplified estimate - real speedup depends on many factors
            return Math.min(4.0, 1.0 + (signalLength / 1024.0));
        }
    }
}