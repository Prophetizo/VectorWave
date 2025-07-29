package ai.prophetizo.wavelet;

/**
 * Represents the result of a single-level forward wavelet transform.
 * It cleanly separates the approximation (low-frequency) and detail (high-frequency) coefficients.
 * <p>
 * This is a sealed interface that can only be instantiated through internal factory methods
 * to ensure all instances are properly validated.
 *
 * <h3>Validation Strategy</h3>
 * <p>Since TransformResult instances are only created by internal transform operations
 * that have already validated inputs, the implementation uses assertions rather than
 * full validation for better performance. This is safe because:</p>
 * <ul>
 *   <li>Input signals are fully validated before transformation</li>
 *   <li>Transform operations preserve finite values for finite inputs</li>
 *   <li>Arrays are created internally with known dimensions</li>
 * </ul>
 *
 * <p>For debugging, enable full validation with:
 * {@code -Dai.prophetizo.wavelet.fullValidation=true}</p>
 */
public sealed interface TransformResult permits TransformResultImpl, PaddedTransformResult {

    /**
     * Creates a new TransformResult with modified coefficients.
     * This factory method is the recommended way to create modified transform results
     * for operations like thresholding or denoising.
     *
     * @param approximationCoeffs the approximation coefficients
     * @param detailCoeffs        the detail coefficients
     * @return a new TransformResult
     * @throws IllegalArgumentException if coefficients are invalid
     */
    static TransformResult create(double[] approximationCoeffs, double[] detailCoeffs) {
        return new TransformResultImpl(approximationCoeffs, detailCoeffs);
    }

    /**
     * Returns a defensive copy of the approximation coefficients.
     *
     * @return the approximation coefficients (cA)
     */
    double[] approximationCoeffs();

    /**
     * Returns a defensive copy of the detail coefficients.
     *
     * @return the detail coefficients (cD)
     */
    double[] detailCoeffs();
}
