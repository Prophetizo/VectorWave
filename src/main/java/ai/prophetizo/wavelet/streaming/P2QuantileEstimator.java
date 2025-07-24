package ai.prophetizo.wavelet.streaming;

/**
 * P² (Piecewise-Parabolic) Quantile Estimator for streaming data.
 *
 * <p>This implementation provides efficient online quantile estimation
 * with O(1) update time and O(1) query time, making it ideal for
 * streaming applications where sorting would be too expensive.</p>
 *
 * <p>Based on the paper "The P² Algorithm for Dynamic Calculation of
 * Quantiles and Histograms Without Storing Observations" by Jain & Chlamtac.</p>
 *
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. If multiple threads need to
 * update the estimator concurrently, external synchronization is required. For best
 * performance in multi-threaded scenarios, consider using separate instances per thread
 * and combining results, or wrapping access with appropriate synchronization.</p>
 *
 * @since 1.6.0
 */
public class P2QuantileEstimator {

    /**
     * Number of markers used in the P² algorithm.
     *
     * <p>The P² algorithm uses exactly 5 markers to maintain its quantile estimate:
     * <ul>
     *   <li>Marker 0: Minimum value seen so far</li>
     *   <li>Marker 1: p/2 quantile estimate</li>
     *   <li>Marker 2: p quantile estimate (the target quantile)</li>
     *   <li>Marker 3: (1+p)/2 quantile estimate</li>
     *   <li>Marker 4: Maximum value seen so far</li>
     * </ul>
     *
     * <p>This specific arrangement of 5 markers enables the algorithm to:
     * <ul>
     *   <li>Track the full range of data (min/max)</li>
     *   <li>Maintain the target quantile estimate</li>
     *   <li>Use parabolic interpolation between adjacent markers</li>
     *   <li>Achieve O(1) space and time complexity</li>
     * </ul>
     *
     * <p>The number 5 is fundamental to the algorithm and cannot be changed
     * without redesigning the entire approach.</p>
     */
    private static final int MARKERS = 5;

    // Marker positions (indices)
    private final int[] n;

    // Marker heights (quantile estimates)
    private final double[] q;

    // Desired marker positions
    private final double[] np;

    // Desired quantile (e.g., 0.5 for median)
    private final double p;

    // Increments for desired positions
    private final double[] dn;

    // Count of observations
    private long count;

    /**
     * Creates a P² quantile estimator for the specified quantile.
     *
     * @param quantile the quantile to estimate (0.0 to 1.0)
     */
    public P2QuantileEstimator(double quantile) {
        if (quantile < 0.0 || quantile > 1.0) {
            throw new IllegalArgumentException("Quantile must be between 0 and 1");
        }

        this.p = quantile;
        this.n = new int[MARKERS];
        this.q = new double[MARKERS];
        this.np = new double[MARKERS];
        this.dn = new double[MARKERS];
        this.count = 0;

        // Initialize desired positions and increments
        np[0] = 1.0;
        np[1] = 1.0 + 2.0 * p;
        np[2] = 1.0 + 4.0 * p;
        np[3] = 3.0 + 2.0 * p;
        np[4] = 5.0;

        dn[0] = 0.0;
        dn[1] = p / 2.0;
        dn[2] = p;
        dn[3] = (1.0 + p) / 2.0;
        dn[4] = 1.0;

        // Initialize marker positions
        for (int i = 0; i < MARKERS; i++) {
            n[i] = i + 1;
        }
    }

    /**
     * Creates a median estimator (quantile = 0.5).
     *
     * @return a new P² estimator for the median
     */
    public static P2QuantileEstimator forMedian() {
        return new P2QuantileEstimator(0.5);
    }

    /**
     * Updates the estimator with a new observation.
     *
     * @param x the new observation
     */
    public void update(double x) {
        if (count < MARKERS) {
            // Initialization phase
            q[(int) count] = x;
            count++;

            if (count == MARKERS) {
                // Sort initial observations
                sortInitialMarkers();
            }
            return;
        }

        // Find cell k such that q[k] <= x < q[k+1]
        int k;
        if (x < q[0]) {
            q[0] = x;
            k = 0;
        } else if (x >= q[MARKERS - 1]) {
            q[MARKERS - 1] = x;
            k = MARKERS - 2;
        } else {
            k = 0;
            for (int i = 1; i < MARKERS; i++) {
                if (x < q[i]) {
                    k = i - 1;
                    break;
                }
            }
        }

        // Increment positions of markers k+1 to 4
        for (int i = k + 1; i < MARKERS; i++) {
            n[i]++;
        }

        // Update desired positions
        for (int i = 0; i < MARKERS; i++) {
            np[i] += dn[i];
        }

        // Adjust marker heights
        for (int i = 1; i < MARKERS - 1; i++) {
            double d = np[i] - n[i];

            if ((d >= 1.0 && n[i + 1] - n[i] > 1) ||
                    (d <= -1.0 && n[i - 1] - n[i] < -1)) {

                int dInt = d >= 0 ? 1 : -1;
                double qp = parabolicInterpolation(i, dInt);

                if (qp > q[i - 1] && qp < q[i + 1]) {
                    q[i] = qp;
                } else {
                    // Linear interpolation
                    q[i] = linearInterpolation(i, dInt);
                }

                n[i] += dInt;
            }
        }

        count++;
    }

    /**
     * Gets the current quantile estimate.
     *
     * @return the estimated quantile value
     */
    public double getQuantile() {
        if (count == 0) {
            return 0.0;
        }

        if (count < MARKERS) {
            // Not enough data yet, return exact quantile
            double[] sorted = new double[(int) count];
            System.arraycopy(q, 0, sorted, 0, (int) count);
            java.util.Arrays.sort(sorted);

            int index = (int) Math.ceil(p * count) - 1;
            index = Math.max(0, Math.min(index, (int) count - 1));
            return sorted[index];
        }

        // Return the middle marker for the estimated quantile
        return q[2];
    }

    /**
     * Gets the number of observations processed.
     *
     * @return the observation count
     */
    public long getCount() {
        return count;
    }

    /**
     * Resets the estimator to its initial state.
     */
    public void reset() {
        count = 0;
        for (int i = 0; i < MARKERS; i++) {
            n[i] = i + 1;
            q[i] = 0.0;
        }
    }

    /**
     * Sorts the initial markers after collecting first 5 observations.
     */
    private void sortInitialMarkers() {
        // Simple insertion sort for 5 elements
        for (int i = 1; i < MARKERS; i++) {
            double key = q[i];
            int j = i - 1;

            while (j >= 0 && q[j] > key) {
                q[j + 1] = q[j];
                j--;
            }
            q[j + 1] = key;
        }
    }

    /**
     * Parabolic interpolation using P² formula.
     */
    private double parabolicInterpolation(int i, int d) {
        double qi = q[i];
        double qim1 = q[i - 1];
        double qip1 = q[i + 1];

        int ni = n[i];
        int nim1 = n[i - 1];
        int nip1 = n[i + 1];

        double a = d / (double) (nip1 - nim1);

        double b = (ni - nim1 + d) * (qip1 - qi) / (nip1 - ni) +
                (nip1 - ni - d) * (qi - qim1) / (ni - nim1);

        return qi + a * b;
    }

    /**
     * Linear interpolation fallback.
     */
    private double linearInterpolation(int i, int d) {
        int j = i + d;
        return q[i] + d * (q[j] - q[i]) / (n[j] - n[i]);
    }
}