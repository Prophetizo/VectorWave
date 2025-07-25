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
 * <p>Compared to the buffer-based MADNoiseEstimator, this version:
 * <ul>
 *   <li>Has O(1) memory usage for the estimators (not counting input arrays)</li>
 *   <li>Uses constant memory regardless of total samples processed</li>
 *   <li>Provides continuous estimates without buffer delays</li>
 *   <li>Requires two passes over each input block (inherent to MAD calculation)</li>
 * </ul>
 * </p>
 *
 * <p><b>Design Note:</b> The two-pass requirement over each block is not a limitation
 * of the streaming approach but a fundamental requirement of MAD calculation. The key
 * insight is that we maintain O(1) memory across all blocks processed, making this
 * suitable for infinite streams.</p>
 *
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

    @Override
    public void updateEstimate(double[] newCoefficients) {
        if (newCoefficients == null || newCoefficients.length == 0) {
            return;
        }

        // Two-pass approach over input array is necessary for MAD calculation:
        // 1. MAD requires median of |x - median(x)|, so we need median(x) first
        // 2. This does NOT violate O(1) memory - we're iterating over the input array twice,
        //    not storing it. The P² estimators maintain only 5 values each, regardless of
        //    how many coefficients we've processed
        // 3. The input array is provided by the caller and would exist anyway
        // 4. For streaming: we maintain running estimates across blocks with O(1) memory,
        //    while each block requires two passes over its coefficients

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