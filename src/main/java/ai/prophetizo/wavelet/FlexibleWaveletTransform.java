package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.PaddingStrategy;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.ZeroPaddingStrategy;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Wavelet transform with flexible signal length handling through automatic padding.
 *
 * <p>This class extends the standard wavelet transform capabilities to handle
 * signals of arbitrary length by automatically padding to the next power of 2.
 * After inverse transform, results are trimmed back to the original length.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic padding for non-power-of-2 signals</li>
 *   <li>Multiple padding strategies (zero, symmetric, reflect, periodic)</li>
 *   <li>Preserves original signal dimensions</li>
 *   <li>Backward compatible with power-of-2 signals</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create transform with automatic padding
 * FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
 *     new Haar(),
 *     BoundaryMode.PERIODIC,
 *     new ZeroPaddingStrategy()
 * );
 *
 * // Signal of arbitrary length (e.g., 100 samples)
 * double[] signal = new double[100];
 * // ... fill signal ...
 *
 * // Automatically pads to 128, transforms, and returns result
 * TransformResult result = transform.forward(signal);
 *
 * // Inverse transform returns array of length 100
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 *
 * @since 1.0.0
 */
public class FlexibleWaveletTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final PaddingStrategy paddingStrategy;

    /**
     * Creates a flexible wavelet transform with the specified padding strategy.
     *
     * @param wavelet         the wavelet to use for transformations
     * @param boundaryMode    the boundary handling mode
     * @param paddingStrategy the padding strategy for non-power-of-2 signals
     * @throws NullPointerException if any parameter is null
     */
    public FlexibleWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode,
                                    PaddingStrategy paddingStrategy) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        this.paddingStrategy = Objects.requireNonNull(paddingStrategy, "paddingStrategy cannot be null");

        // Validate supported boundary modes
        if (boundaryMode != BoundaryMode.PERIODIC && boundaryMode != BoundaryMode.ZERO_PADDING) {
            throw new UnsupportedOperationException(
                    "Boundary mode " + boundaryMode + " is not yet implemented. " +
                            "Only PERIODIC and ZERO_PADDING are currently supported."
            );
        }
    }

    /**
     * Creates a flexible wavelet transform with zero padding strategy.
     *
     * @param wavelet      the wavelet to use for transformations
     * @param boundaryMode the boundary handling mode
     */
    public FlexibleWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, new ZeroPaddingStrategy());
    }

    /**
     * Performs forward wavelet transform with automatic padding if needed.
     *
     * <p>If the signal length is not a power of 2, it will be automatically
     * padded using the configured padding strategy. The result will track
     * the original length for proper reconstruction.</p>
     *
     * @param signal the input signal (any positive length)
     * @return transform result (may be PaddedTransformResult if padding was applied)
     * @throws IllegalArgumentException if signal is null, empty, or contains non-finite values
     */
    public TransformResult forward(double[] signal) {
        // Basic validation
        ValidationUtils.validateNotNullOrEmpty(signal, "signal");
        ValidationUtils.validateFiniteValues(signal, "signal");

        int originalLength = signal.length;
        double[] processedSignal = signal;
        boolean wasPadded = false;

        // Check if padding is needed
        // Note: Wavelet transforms require at least 2 samples, so single-element signals must be padded
        if (!ValidationUtils.isPowerOfTwo(signal.length) || signal.length < 2) {
            int targetLength = signal.length < 2 ? 2 : ValidationUtils.nextPowerOfTwo(signal.length);
            processedSignal = paddingStrategy.pad(signal, targetLength);
            wasPadded = true;
        }

        // Create standard transform for the processed signal
        WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
        TransformResult result = transform.forward(processedSignal);

        // Wrap result if padding was applied
        if (wasPadded) {
            return new PaddedTransformResult(result, originalLength);
        }

        return result;
    }

    /**
     * Performs inverse wavelet transform with automatic trimming if needed.
     *
     * <p>If the forward transform applied padding, the result will be
     * automatically trimmed to the original signal length.</p>
     *
     * @param transformResult the transform coefficients
     * @return reconstructed signal (original length if padding was used)
     * @throws IllegalArgumentException if transformResult is null
     */
    public double[] inverse(TransformResult transformResult) {
        if (transformResult == null) {
            throw new InvalidArgumentException("Transform result cannot be null");
        }

        // Create standard transform for inverse operation
        WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
        double[] reconstructed = transform.inverse(transformResult);

        // Trim if it was a padded transform
        if (transformResult instanceof PaddedTransformResult padded) {
            return paddingStrategy.trim(reconstructed, padded.originalLength());
        }

        return reconstructed;
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
     * Gets the padding strategy used by this transform.
     *
     * @return the padding strategy
     */
    public PaddingStrategy getPaddingStrategy() {
        return paddingStrategy;
    }
}
