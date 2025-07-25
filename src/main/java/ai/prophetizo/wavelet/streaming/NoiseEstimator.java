package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

/**
 * Interface for estimating noise levels in streaming wavelet coefficients.
 *
 * <p>Implementations provide adaptive noise estimation suitable for
 * non-stationary signals in real-time applications.</p>
 *
 * @since 1.0.0
 */
public interface NoiseEstimator {

    /**
     * Estimates the noise level from wavelet coefficients.
     *
     * @param coefficients detail coefficients from wavelet transform
     * @return estimated noise standard deviation
     */
    double estimateNoise(double[] coefficients);

    /**
     * Updates the noise estimate with new coefficients.
     *
     * <p>This method allows for adaptive estimation that tracks
     * changes in noise characteristics over time.</p>
     *
     * @param coefficients new detail coefficients
     */
    void updateEstimate(double[] coefficients);

    /**
     * Calculates the threshold based on the current noise estimate.
     *
     * @param method threshold selection method
     * @return calculated threshold value
     */
    double getThreshold(ThresholdMethod method);

    /**
     * Gets the current noise standard deviation estimate.
     *
     * @return current noise estimate
     */
    double getCurrentNoiseLevel();

    /**
     * Resets the estimator to initial state.
     */
    void reset();

    /**
     * Gets the number of samples used in the current estimate.
     *
     * @return sample count
     */
    long getSampleCount();
}