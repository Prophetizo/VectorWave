package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Main class for performing the 1D Fast Wavelet Transform (FWT).
 *
 * <p>This implementation supports multiple wavelet families including
 * orthogonal (Haar, Daubechies, Symlets, Coiflets), biorthogonal, and
 * continuous wavelets. All transforms use a scalar implementation for
 * correctness and maintainability.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create transform with Haar wavelet
 * WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
 *
 * // Perform forward transform
 * double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
 * TransformResult result = transform.forward(signal);
 *
 * // Perform inverse transform
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 */
public class WaveletTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;

    /**
     * Constructs a transformer with the specified wavelet and boundary mode.
     *
     * @param wavelet      The wavelet to use for the transformations
     * @param boundaryMode The boundary handling mode
     * @throws NullPointerException if any parameter is null
     */
    public WaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null.");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null.");

        // Validate supported boundary modes
        if (boundaryMode != BoundaryMode.PERIODIC && boundaryMode != BoundaryMode.ZERO_PADDING) {
            throw new UnsupportedOperationException(
                    "Only PERIODIC and ZERO_PADDING boundary modes are currently supported.");
        }
    }

    /**
     * Performs a single-level forward 1D Fast Wavelet Transform.
     *
     * @param signal The input signal. Must have a power-of-two length.
     * @return A TransformResult containing the approximation and detail coefficients.
     * @throws InvalidSignalException if signal is null, empty, not power-of-two length, or contains invalid values
     */
    public TransformResult forward(double[] signal) {
        // Comprehensive validation
        ValidationUtils.validateSignal(signal, "signal");

        double[] lowPassFilter = wavelet.lowPassDecomposition();
        double[] highPassFilter = wavelet.highPassDecomposition();

        // The output coefficients will be half the length of the input signal.
        int outputLength = signal.length / 2;
        double[] approximationCoeffs = new double[outputLength];
        double[] detailCoeffs = new double[outputLength];

        // Perform convolution and downsampling based on boundary mode
        if (boundaryMode == BoundaryMode.PERIODIC) {
            // Use combined transform for better cache efficiency when possible
            ScalarOps.combinedTransformPeriodic(signal, lowPassFilter, highPassFilter,
                    approximationCoeffs, detailCoeffs);
        } else {
            ScalarOps.convolveAndDownsampleDirect(signal, lowPassFilter, approximationCoeffs);
            ScalarOps.convolveAndDownsampleDirect(signal, highPassFilter, detailCoeffs);
        }

        // Create the result using TransformResultImpl
        return new TransformResultImpl(approximationCoeffs, detailCoeffs);
    }

    /**
     * Performs a single-level inverse 1D Fast Wavelet Transform to reconstruct the signal.
     *
     * @param transformResult The transform result containing approximation and detail coefficients
     * @return The reconstructed signal
     * @throws NullPointerException   if transformResult is null
     * @throws InvalidSignalException if coefficients are invalid or mismatched
     */
    public double[] inverse(TransformResult transformResult) {
        Objects.requireNonNull(transformResult, "transformResult cannot be null.");

        // TransformResult guarantees valid coefficients and returns defensive copies
        double[] approx = transformResult.approximationCoeffs();
        double[] detail = transformResult.detailCoeffs();

        int outputLength = (approx.length + detail.length);
        double[] signal = new double[outputLength];

        double[] approxRecon = new double[outputLength];
        double[] detailRecon = new double[outputLength];

        // Perform upsampling and convolution based on boundary mode
        if (boundaryMode == BoundaryMode.PERIODIC) {
            ScalarOps.upsampleAndConvolvePeriodic(approx, wavelet.lowPassReconstruction(), approxRecon);
            ScalarOps.upsampleAndConvolvePeriodic(detail, wavelet.highPassReconstruction(), detailRecon);
        } else {
            ScalarOps.upsampleAndConvolveDirect(approx, wavelet.lowPassReconstruction(), approxRecon);
            ScalarOps.upsampleAndConvolveDirect(detail, wavelet.highPassReconstruction(), detailRecon);
        }

        // Add the two reconstructed components together to get the final signal
        for (int i = 0; i < outputLength; i++) {
            signal[i] = approxRecon[i] + detailRecon[i];
        }

        return signal;
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
     * Gets the boundary mode used by this transform.
     *
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
}