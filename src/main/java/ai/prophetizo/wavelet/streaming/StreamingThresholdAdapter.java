package ai.prophetizo.wavelet.streaming;

/**
 * Adaptive threshold controller for streaming denoising applications.
 *
 * <p>This class provides smooth threshold transitions with configurable
 * attack and release times, preventing abrupt changes that could cause
 * artifacts in the denoised signal.</p>
 *
 * @since 1.6.0
 */
public class StreamingThresholdAdapter {

    private final double attackTime;  // Time to increase threshold (in blocks)
    private final double releaseTime; // Time to decrease threshold (in blocks)
    private final double minThreshold;
    private final double maxThreshold;
    private final double attackRate;
    private final double releaseRate;
    private double currentThreshold;
    private double targetThreshold;

    /**
     * Creates a threshold adapter with default settings.
     */
    public StreamingThresholdAdapter() {
        this(10.0, 50.0, 0.0, Double.MAX_VALUE);
    }

    /**
     * Creates a threshold adapter with specified parameters.
     *
     * @param attackTime   time constant for threshold increases (in blocks)
     * @param releaseTime  time constant for threshold decreases (in blocks)
     * @param minThreshold minimum allowed threshold
     * @param maxThreshold maximum allowed threshold
     */
    public StreamingThresholdAdapter(double attackTime, double releaseTime,
                                     double minThreshold, double maxThreshold) {
        if (attackTime <= 0 || releaseTime <= 0) {
            throw new IllegalArgumentException("Attack and release times must be positive");
        }
        if (minThreshold < 0) {
            throw new IllegalArgumentException("Minimum threshold must be non-negative");
        }
        if (maxThreshold <= minThreshold) {
            throw new IllegalArgumentException("Maximum threshold must be greater than minimum");
        }

        this.attackTime = attackTime;
        this.releaseTime = releaseTime;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;

        // Calculate rates (0.63 of target reached after time constant)
        this.attackRate = 1.0 - Math.exp(-1.0 / attackTime);
        this.releaseRate = 1.0 - Math.exp(-1.0 / releaseTime);

        this.currentThreshold = minThreshold;
        this.targetThreshold = minThreshold;
    }

    /**
     * Adapts the current threshold toward the target.
     *
     * @return current adapted threshold
     */
    public double adaptThreshold() {
        if (currentThreshold < targetThreshold) {
            // Attack phase - threshold increasing
            currentThreshold += (targetThreshold - currentThreshold) * attackRate;
        } else if (currentThreshold > targetThreshold) {
            // Release phase - threshold decreasing
            currentThreshold += (targetThreshold - currentThreshold) * releaseRate;
        }

        // Ensure bounds
        currentThreshold = Math.max(minThreshold, Math.min(maxThreshold, currentThreshold));

        return currentThreshold;
    }

    /**
     * Gets the current threshold without adapting.
     *
     * @return current threshold value
     */
    public double getCurrentThreshold() {
        return currentThreshold;
    }

    /**
     * Immediately sets the current threshold.
     *
     * @param threshold new threshold value
     */
    public void setCurrentThreshold(double threshold) {
        this.currentThreshold = Math.max(minThreshold, Math.min(maxThreshold, threshold));
        this.targetThreshold = this.currentThreshold;
    }

    /**
     * Gets the target threshold.
     *
     * @return target threshold value
     */
    public double getTargetThreshold() {
        return targetThreshold;
    }

    /**
     * Updates the target threshold.
     *
     * @param newTarget new target threshold value
     */
    public void setTargetThreshold(double newTarget) {
        this.targetThreshold = Math.max(minThreshold, Math.min(maxThreshold, newTarget));
    }

    /**
     * Resets the adapter to minimum threshold.
     */
    public void reset() {
        this.currentThreshold = minThreshold;
        this.targetThreshold = minThreshold;
    }

    /**
     * Checks if the threshold has reached its target.
     *
     * @param tolerance tolerance for comparison
     * @return true if within tolerance of target
     */
    public boolean hasReachedTarget(double tolerance) {
        return Math.abs(currentThreshold - targetThreshold) < tolerance;
    }

    /**
     * Gets the attack time constant.
     */
    public double getAttackTime() {
        return attackTime;
    }

    /**
     * Gets the release time constant.
     */
    public double getReleaseTime() {
        return releaseTime;
    }
}