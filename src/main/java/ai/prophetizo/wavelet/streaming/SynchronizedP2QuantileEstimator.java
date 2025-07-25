package ai.prophetizo.wavelet.streaming;

/**
 * Thread-safe wrapper for P2QuantileEstimator.
 *
 * <p>This class provides a synchronized wrapper around P2QuantileEstimator
 * for use in multi-threaded environments. For single-threaded applications
 * or when external synchronization is used, prefer the unsynchronized
 * P2QuantileEstimator for better performance.</p>
 *
 * @since 1.0.0
 */
public class SynchronizedP2QuantileEstimator {

    private final P2QuantileEstimator estimator;

    /**
     * Creates a synchronized P² quantile estimator for the specified quantile.
     *
     * @param quantile the quantile to estimate (0.0 to 1.0)
     */
    public SynchronizedP2QuantileEstimator(double quantile) {
        this.estimator = new P2QuantileEstimator(quantile);
    }

    /**
     * Creates a synchronized median estimator (quantile = 0.5).
     *
     * @return a new synchronized P² estimator for the median
     */
    public static SynchronizedP2QuantileEstimator forMedian() {
        return new SynchronizedP2QuantileEstimator(0.5);
    }

    /**
     * Updates the estimator with a new observation.
     *
     * @param x the new observation
     */
    public synchronized void update(double x) {
        estimator.update(x);
    }

    /**
     * Gets the current quantile estimate.
     *
     * @return the estimated quantile value
     */
    public synchronized double getQuantile() {
        return estimator.getQuantile();
    }

    /**
     * Gets the number of observations processed.
     *
     * @return the observation count
     */
    public synchronized long getCount() {
        return estimator.getCount();
    }

    /**
     * Resets the estimator to its initial state.
     */
    public synchronized void reset() {
        estimator.reset();
    }
}