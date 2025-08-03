package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Main class for performing the 1D Maximal Overlap Discrete Wavelet Transform (MODWT).
 *
 * <p>MODWT offers several advantages over standard DWT:
 * <ul>
 *   <li><strong>No power-of-2 restriction:</strong> Works with signals of any length</li>
 *   <li><strong>Shift-invariant:</strong> Translation of input results in corresponding translation of coefficients</li>
 *   <li><strong>Same-length output:</strong> Both approximation and detail coefficients have the same length as input</li>
 *   <li><strong>Better for real-world applications:</strong> Especially financial time series analysis</li>
 * </ul>
 * </p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create transform with Haar wavelet
 * WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
 * 
 * // Perform forward transform (works with any signal length)
 * double[] signal = {1, 2, 3, 4, 5, 6, 7};  // Not power-of-2!
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
     * Constructs a MODWT transformer with the specified wavelet and boundary mode.
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
     * Performs a single-level forward MODWT transform.
     *
     * @param signal The input signal. Can be any length (no power-of-2 restriction).
     * @return A TransformResult containing the approximation and detail coefficients (same length as input).
     * @throws InvalidSignalException if signal is null, empty, or contains invalid values
     */
    public TransformResult forward(double[] signal) {
        // Validate signal (without power-of-2 requirement)
        validateMODWTSignal(signal, "signal");

        // Get filters and scale them for MODWT
        double[] lowPassFilter = ScalarOps.scaleFilterForMODWT(wavelet.lowPassDecomposition(), 1);
        double[] highPassFilter = ScalarOps.scaleFilterForMODWT(wavelet.highPassDecomposition(), 1);

        // For MODWT, output length equals input length
        int outputLength = signal.length;
        double[] approximationCoeffs = new double[outputLength];
        double[] detailCoeffs = new double[outputLength];

        // Perform convolution based on boundary mode (no downsampling)
        if (boundaryMode == BoundaryMode.PERIODIC) {
            ScalarOps.modwtConvolvePeriodic(signal, lowPassFilter, approximationCoeffs);
            ScalarOps.modwtConvolvePeriodic(signal, highPassFilter, detailCoeffs);
        } else {
            ScalarOps.modwtConvolveZeroPadding(signal, lowPassFilter, approximationCoeffs);
            ScalarOps.modwtConvolveZeroPadding(signal, highPassFilter, detailCoeffs);
        }

        return new TransformResultImpl(approximationCoeffs, detailCoeffs);
    }

    /**
     * Performs a single-level inverse MODWT transform to reconstruct the signal.
     *
     * @param transformResult The transform result containing approximation and detail coefficients
     * @return The reconstructed signal (same length as input coefficients)
     * @throws NullPointerException   if transformResult is null
     * @throws InvalidSignalException if coefficients are invalid or mismatched
     */
    public double[] inverse(TransformResult transformResult) {
        Objects.requireNonNull(transformResult, "transformResult cannot be null.");

        double[] approx = transformResult.approximationCoeffs();
        double[] detail = transformResult.detailCoeffs();

        // For MODWT, coefficients should have the same length
        if (approx.length != detail.length) {
            throw new InvalidSignalException("MODWT coefficients must have the same length. " +
                    "Approximation: " + approx.length + ", Detail: " + detail.length);
        }

        int outputLength = approx.length;
        double[] signal = new double[outputLength];

        // Get reconstruction filters and scale them for MODWT
        double[] lowPassReconFilter = ScalarOps.scaleFilterForMODWT(wavelet.lowPassReconstruction(), 1);
        double[] highPassReconFilter = ScalarOps.scaleFilterForMODWT(wavelet.highPassReconstruction(), 1);

        // For MODWT inverse, we need to use a different convolution approach
        // The reconstruction involves convolution with filters in reverse order
        double[] approxRecon = new double[outputLength];
        double[] detailRecon = new double[outputLength];

        // Perform MODWT inverse convolution (reverse direction)
        if (boundaryMode == BoundaryMode.PERIODIC) {
            modwtInverseConvolvePeriodic(approx, lowPassReconFilter, approxRecon);
            modwtInverseConvolvePeriodic(detail, highPassReconFilter, detailRecon);
        } else {
            modwtInverseConvolveZeroPadding(approx, lowPassReconFilter, approxRecon);
            modwtInverseConvolveZeroPadding(detail, highPassReconFilter, detailRecon);
        }

        // Add the two reconstructed components together to get the final signal
        for (int i = 0; i < outputLength; i++) {
            signal[i] = approxRecon[i] + detailRecon[i];
        }

        return signal;
    }

    /**
     * MODWT inverse convolution with periodic boundary handling.
     * This is different from forward convolution - it uses the filter in reverse.
     */
    private void modwtInverseConvolvePeriodic(double[] coeffs, double[] filter, double[] output) {
        int signalLen = coeffs.length;
        int filterLen = filter.length;

        for (int i = 0; i < signalLen; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                // For inverse, we convolve with the filter in reverse direction
                int coeffIndex = (i - j + signalLen) % signalLen;
                sum += coeffs[coeffIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * MODWT inverse convolution with zero padding boundary handling.
     * This is different from forward convolution - it uses the filter in reverse.
     */
    private void modwtInverseConvolveZeroPadding(double[] coeffs, double[] filter, double[] output) {
        int signalLen = coeffs.length;
        int filterLen = filter.length;

        for (int i = 0; i < signalLen; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                // For inverse, we convolve with the filter in reverse direction
                int coeffIndex = i - j;
                if (coeffIndex >= 0) {
                    sum += coeffs[coeffIndex] * filter[j];
                }
                // Values outside range are implicitly zero
            }

            output[i] = sum;
        }
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

    /**
     * Validates signal for MODWT (no power-of-2 requirement).
     * 
     * @param signal        the signal to validate
     * @param parameterName the name of the parameter for error messages
     * @throws InvalidSignalException if the signal is invalid
     */
    private static void validateMODWTSignal(double[] signal, String parameterName) {
        // Check null and empty
        ValidationUtils.validateNotNullOrEmpty(signal, parameterName);
        
        // Check for NaN and infinity
        ValidationUtils.validateFiniteValues(signal, parameterName);
        
        // Note: No power-of-2 requirement for MODWT
    }
}