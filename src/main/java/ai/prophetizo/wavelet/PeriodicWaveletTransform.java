package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BiorthogonalWavelet;
import ai.prophetizo.wavelet.api.DiscreteWavelet;
import ai.prophetizo.wavelet.api.OrthogonalWavelet;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;

/**
 * Optimized wavelet transform specifically for periodic boundary conditions.
 * Eliminates boundary mode checks for better performance on small signals.
 *
 * <p>This specialized implementation is ideal for:
 * <ul>
 *   <li>Signals that naturally have periodic properties</li>
 *   <li>Financial time series with cyclic patterns</li>
 *   <li>Real-time processing where performance is critical</li>
 * </ul>
 * </p>
 *
 * <p>By eliminating boundary mode checks, this implementation enables
 * better JIT optimization and reduces branch mispredictions.</p>
 */
public final class PeriodicWaveletTransform {

    private final Wavelet wavelet;
    private final double[] lowPassFilter;
    private final double[] highPassFilter;
    private final double[] lowPassRecon;
    private final double[] highPassRecon;

    /**
     * Creates a periodic wavelet transform.
     *
     * @param wavelet the wavelet to use (must be discrete)
     * @throws IllegalArgumentException if wavelet is not discrete
     */
    public PeriodicWaveletTransform(Wavelet wavelet) {
        this.wavelet = wavelet;
        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }

        if (wavelet instanceof DiscreteWavelet discreteWavelet) {
            this.lowPassFilter = discreteWavelet.lowPassDecomposition();
            this.highPassFilter = discreteWavelet.highPassDecomposition();

            // Pre-compute reconstruction filters
            if (wavelet instanceof OrthogonalWavelet) {
                this.lowPassRecon = lowPassFilter;
                this.highPassRecon = highPassFilter;
            } else if (wavelet instanceof BiorthogonalWavelet biortho) {
                this.lowPassRecon = biortho.lowPassReconstruction();
                this.highPassRecon = biortho.highPassReconstruction();
            } else {
                throw new InvalidArgumentException(
                        "Unsupported wavelet type: " + wavelet.getClass().getSimpleName()
                );
            }
        } else {
            throw new InvalidArgumentException(
                    "Only discrete wavelets are supported. Got: " + wavelet.getClass().getSimpleName()
            );
        }
    }

    /**
     * Performs forward wavelet transform with periodic boundaries.
     * Optimized for small signals by using combined transform.
     *
     * @param signal the input signal (must be power of 2 length)
     * @return transform result
     */
    public TransformResult forward(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");

        int outputLength = (signal.length + 1) / 2;
        double[] approximationCoeffs = new double[outputLength];
        double[] detailCoeffs = new double[outputLength];

        // Always use combined transform for periodic mode
        ScalarOps.combinedTransformPeriodic(signal, lowPassFilter, highPassFilter,
                approximationCoeffs, detailCoeffs);

        // Use fast result creation - arrays are freshly allocated and valid
        return TransformResultImpl.createFast(approximationCoeffs, detailCoeffs);
    }

    /**
     * Performs inverse wavelet transform with periodic boundaries.
     *
     * @param coefficients the transform coefficients
     * @return the reconstructed signal
     */
    public double[] inverse(TransformResult coefficients) {
        if (coefficients == null) {
            throw new InvalidArgumentException("Coefficients cannot be null");
        }

        double[] approxCoeffs = coefficients.approximationCoeffs();
        double[] detailCoeffs = coefficients.detailCoeffs();

        if (approxCoeffs == null) {
            throw new InvalidArgumentException("Approximation coefficients cannot be null");
        }
        if (detailCoeffs == null) {
            throw new InvalidArgumentException("Detail coefficients cannot be null");
        }
        if (approxCoeffs.length != detailCoeffs.length) {
            throw new InvalidArgumentException("Coefficient arrays must have same length");
        }

        int outputLength = approxCoeffs.length * 2;
        double[] reconstructed = new double[outputLength];
        double[] temp = new double[outputLength];

        // Direct calls without boundary checks
        ScalarOps.upsampleAndConvolvePeriodic(approxCoeffs, lowPassRecon, reconstructed);
        ScalarOps.upsampleAndConvolvePeriodic(detailCoeffs, highPassRecon, temp);

        // Add contributions
        for (int i = 0; i < outputLength; i++) {
            reconstructed[i] += temp[i];
        }

        return reconstructed;
    }

    /**
     * Performs in-place forward transform for maximum efficiency.
     * The input array is modified to contain approximation coefficients
     * in the first half and detail coefficients in the second half.
     *
     * @param signal the signal to transform (modified in place)
     * @return the length of coefficient arrays (half of signal length)
     */
    public int forwardInPlace(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");

        int coeffLength = signal.length / 2;
        double[] approxTemp = new double[coeffLength];
        double[] detailTemp = new double[coeffLength];

        // Compute both sets of coefficients
        ScalarOps.convolveAndDownsamplePeriodic(signal, lowPassFilter, approxTemp);
        ScalarOps.convolveAndDownsamplePeriodic(signal, highPassFilter, detailTemp);

        // Copy coefficients back to signal array
        System.arraycopy(approxTemp, 0, signal, 0, coeffLength);
        System.arraycopy(detailTemp, 0, signal, coeffLength, coeffLength);

        return coeffLength;
    }

    /**
     * Gets the wavelet used by this transform.
     *
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }

    /**
     * Creates a transform result without unnecessary validation.
     * For internal use when arrays are known to be valid.
     *
     * @param approxCoeffs approximation coefficients
     * @param detailCoeffs detail coefficients
     * @return transform result
     */
    TransformResult createResult(double[] approxCoeffs, double[] detailCoeffs) {
        return TransformResultImpl.createFast(approxCoeffs, detailCoeffs);
    }
}
