package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Arrays;

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
public sealed interface TransformResult permits TransformResultImpl {

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

/**
 * Package-private implementation of TransformResult.
 * This class can only be instantiated within the wavelet package.
 */
record TransformResultImpl(double[] approximationCoeffs, double[] detailCoeffs) implements TransformResult {
    // System property to enable full validation in production
    // System property name for enabling full validation
    private static final String FULL_VALIDATION_PROPERTY = "ai.prophetizo.wavelet.fullValidation";

    private static final boolean FULL_VALIDATION =
            Boolean.getBoolean(FULL_VALIDATION_PROPERTY);

    /**
     * Package-private constructor that validates and defensively copies the coefficient arrays.
     *
     * <p>Since this constructor is only called by internal transform operations that have
     * already validated the input signal, we use assertions for internal consistency checks
     * rather than full validation. This improves performance while maintaining safety.</p>
     *
     * <p>Enable assertions with -ea JVM flag during development/testing to verify invariants.
     * Alternatively, set system property -Dai.prophetizo.wavelet.fullValidation=true to
     * enable full validation in production for debugging purposes.</p>
     *
     * @param approximationCoeffs the approximation coefficients (must not be null/empty)
     * @param detailCoeffs        the detail coefficients (must not be null/empty)
     */
    TransformResultImpl(double[] approximationCoeffs, double[] detailCoeffs) {
        if (FULL_VALIDATION) {
            // Full validation mode for debugging
            ValidationUtils.validateNotNullOrEmpty(approximationCoeffs, "approximationCoeffs");
            ValidationUtils.validateNotNullOrEmpty(detailCoeffs, "detailCoeffs");
            ValidationUtils.validateMatchingLengths(approximationCoeffs, detailCoeffs);
            ValidationUtils.validateFiniteValues(approximationCoeffs, "approximationCoeffs");
            ValidationUtils.validateFiniteValues(detailCoeffs, "detailCoeffs");
        } else {
            // Use assertions for internal consistency checks
            assert approximationCoeffs != null : "approximationCoeffs cannot be null.";
            assert detailCoeffs != null : "detailCoeffs cannot be null.";
            assert approximationCoeffs.length > 0 : "approximationCoeffs cannot be empty.";
            assert detailCoeffs.length > 0 : "detailCoeffs cannot be empty.";
            assert approximationCoeffs.length == detailCoeffs.length :
                    "Coefficient arrays must have matching lengths.";

            // In debug mode, verify finite values
            assert areAllFinite(approximationCoeffs) : "approximationCoeffs contains non-finite values.";
            assert areAllFinite(detailCoeffs) : "detailCoeffs contains non-finite values.";
        }

        // Make defensive copies to ensure immutability
        this.approximationCoeffs = approximationCoeffs.clone();
        this.detailCoeffs = detailCoeffs.clone();
    }

    /**
     * Helper method for assertion to check if all values are finite.
     * Only executed when assertions are enabled.
     */
    private static boolean areAllFinite(double[] values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double[] approximationCoeffs() {
        return approximationCoeffs.clone();
    }

    @Override
    public double[] detailCoeffs() {
        return detailCoeffs.clone();
    }

    @Override
    public String toString() {
        return "TransformResult{" +
                "\n  Approximation (cA) = " + Arrays.toString(approximationCoeffs) +
                "\n  Detail (cD)        = " + Arrays.toString(detailCoeffs) +
                "\n}";
    }

    /**
     * Creates a TransformResult without validation for internal use.
     * Package-private for use by optimized transform classes.
     *
     * @param approxCoeffs approximation coefficients (will be cloned)
     * @param detailCoeffs detail coefficients (will be cloned)
     * @return new TransformResult
     */
    static TransformResult createFast(double[] approxCoeffs, double[] detailCoeffs) {
        // Bypass validation by temporarily setting the property
        boolean originalValidation = FULL_VALIDATION;

        try {
            if (originalValidation) {
                System.setProperty(FULL_VALIDATION_PROPERTY, "false");
            }
            return new TransformResultImpl(approxCoeffs, detailCoeffs);
        } finally {
            if (originalValidation) {
                System.setProperty(FULL_VALIDATION_PROPERTY, "true");
            }
        }
    }
}