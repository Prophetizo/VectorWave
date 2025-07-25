package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

/**
 * Streaming Median Absolute Deviation (MAD) noise estimator using P² algorithm.
 *
 * <p>This implementation provides efficient online MAD estimation without
 * sorting, making it suitable for high-throughput streaming applications.
 * It uses two P² quantile estimators to track the median of values and
 * the median of absolute deviations.</p>
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>O(1) memory complexity - uses only 10 values total (5 per P² estimator)</li>
 *   <li>Suitable for infinite streams - no buffer limits</li>
 *   <li>Continuous estimates - no buffer delays</li>
 *   <li>Two-pass processing per block - inherent to MAD algorithm</li>
 * </ul>
 * </p>
 *
 * @see P2QuantileEstimator
 * @see MADNoiseEstimator
 * @since 1.6.0
 */
public class StreamingMADNoiseEstimator implements NoiseEstimator {

    // Constants for noise estimation
    private static final double MAD_SCALE_FACTOR = 1.4826; // For Gaussian assumption

    // P² estimators for efficient median tracking
    private final P2QuantileEstimator valueMedianEstimator;
    private final P2QuantileEstimator deviationMedianEstimator;

    // Smoothing parameters
    private final double smoothingFactor;
    private double smoothedNoiseLevel;

    // Statistics
    private long totalSamples;
    private boolean initialized;

    /**
     * Creates a streaming MAD noise estimator.
     *
     * @param smoothingFactor exponential smoothing factor (0.0 to 1.0)
     */
    public StreamingMADNoiseEstimator(double smoothingFactor) {
        if (smoothingFactor < 0.0 || smoothingFactor > 1.0) {
            throw new IllegalArgumentException("Smoothing factor must be between 0 and 1");
        }

        this.smoothingFactor = smoothingFactor;
        this.valueMedianEstimator = P2QuantileEstimator.forMedian();
        this.deviationMedianEstimator = P2QuantileEstimator.forMedian();
        this.smoothedNoiseLevel = 0.0;
        this.totalSamples = 0;
        this.initialized = false;
    }

    /**
     * Creates a streaming MAD noise estimator with default smoothing (0.95).
     */
    public StreamingMADNoiseEstimator() {
        this(0.95);
    }

    @Override
    public double estimateNoise(double[] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return 0.0;
        }

        // Update with new coefficients
        updateEstimate(coefficients);
        return getCurrentNoiseLevel();
    }

    /**
     * Updates the noise estimate with new wavelet coefficients.
     *
     * <p><b>Implementation note:</b> MAD calculation requires median of |x - median(x)|,
     * necessitating two passes over the input:
     * <ol>
     *   <li>First pass: Update value median estimator to get median(x)</li>
     *   <li>Second pass: Calculate |x - median(x)| and update deviation median estimator</li>
     * </ol>
     * This maintains O(1) memory complexity as we only iterate over the input array
     * (provided by caller), not store it. The P² estimators use fixed memory (5 values each)
     * regardless of total samples processed.</p>
     *
     * @param newCoefficients wavelet detail coefficients for noise estimation
     */
    @Override
    public void updateEstimate(double[] newCoefficients) {
        if (newCoefficients == null || newCoefficients.length == 0) {
            return;
        }

        // Two-pass approach: first compute median, then compute deviations

        // First pass: update value median estimator
        for (double coeff : newCoefficients) {
            valueMedianEstimator.update(coeff);
        }

        // Get current median estimate
        double median = valueMedianEstimator.getQuantile();

        // Second pass: update deviation median estimator
        for (double coeff : newCoefficients) {
            double deviation = Math.abs(coeff - median);
            deviationMedianEstimator.update(deviation);
        }

        // Calculate MAD-based noise estimate
        double mad = deviationMedianEstimator.getQuantile();
        double noiseEstimate = mad * MAD_SCALE_FACTOR;

        // Apply exponential smoothing
        if (!initialized) {
            smoothedNoiseLevel = noiseEstimate;
            initialized = true;
        } else {
            smoothedNoiseLevel = smoothingFactor * smoothedNoiseLevel +
                    (1.0 - smoothingFactor) * noiseEstimate;
        }

        totalSamples += newCoefficients.length;
    }

    @Override
    public double getCurrentNoiseLevel() {
        return smoothedNoiseLevel;
    }

    @Override
    public double getThreshold(ThresholdMethod method) {
        double sigma = getCurrentNoiseLevel();

        switch (method) {
            case UNIVERSAL:
                // Universal threshold: σ * sqrt(2 * log(n))
                long n = Math.max(totalSamples, 1);
                return sigma * Math.sqrt(2.0 * Math.log(n));

            case SURE:
                // SURE is complex for streaming; use conservative approximation
                return sigma * 2.5;

            case MINIMAX:
                // Minimax threshold approximation
                return sigma * 1.5;

            default:
                return sigma * Math.sqrt(2.0 * Math.log(Math.max(totalSamples, 1)));
        }
    }

    @Override
    public void reset() {
        valueMedianEstimator.reset();
        deviationMedianEstimator.reset();
        smoothedNoiseLevel = 0.0;
        totalSamples = 0;
        initialized = false;
    }

    @Override
    public long getSampleCount() {
        return totalSamples;
    }

    /**
     * Gets the current median estimate of the signal values.
     *
     * @return the median of signal values
     */
    public double getSignalMedian() {
        return valueMedianEstimator.getQuantile();
    }

    /**
     * Gets the current median absolute deviation.
     *
     * @return the raw MAD before scaling
     */
    public double getRawMAD() {
        return deviationMedianEstimator.getQuantile();
    }
}