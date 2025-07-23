package ai.prophetizo.wavelet;

import java.nio.DoubleBuffer;

/**
 * Performance-optimized immutable transform result for internal operations.
 * <p>
 * This implementation stores coefficients in a single contiguous array for
 * better cache locality and provides both defensive copies (for API compatibility)
 * and direct access methods (for performance-critical internal operations).
 * <p>
 * For maximum performance in trusted internal code, use the direct array access
 * methods or buffer views to avoid copying overhead. For API compatibility,
 * use the standard accessor methods which return defensive copies.
 */
public final class ImmutableTransformResult {

    private final double[] coefficients;
    private final int splitPoint;

    /**
     * Creates a transform result from separate coefficient arrays.
     * The arrays are copied once during construction.
     */
    public ImmutableTransformResult(double[] approximationCoeffs, double[] detailCoeffs) {
        if (approximationCoeffs == null || detailCoeffs == null) {
            throw new IllegalArgumentException("Coefficient arrays cannot be null");
        }

        if (approximationCoeffs.length != detailCoeffs.length) {
            throw new IllegalArgumentException("Coefficient arrays must have equal length");
        }

        this.splitPoint = approximationCoeffs.length;
        this.coefficients = new double[splitPoint * 2];

        // Copy coefficients into single array
        System.arraycopy(approximationCoeffs, 0, coefficients, 0, splitPoint);
        System.arraycopy(detailCoeffs, 0, coefficients, splitPoint, splitPoint);
    }

    /**
     * Creates a transform result from a pre-allocated coefficient array.
     * No copying is performed - the array is used directly.
     *
     * @param coefficients Combined coefficient array
     * @param splitPoint   Index where detail coefficients start
     */
    private ImmutableTransformResult(double[] coefficients, int splitPoint) {
        this.coefficients = coefficients;
        this.splitPoint = splitPoint;
    }

    /**
     * Factory method for creating results with pre-allocated arrays.
     * Useful for batch processing and memory pooling.
     *
     * @param workspace         Pre-allocated array of size 2 * coefficientLength
     * @param coefficientLength Length of each coefficient array
     * @return New transform result using the workspace
     */
    public static ImmutableTransformResult fromWorkspace(double[] workspace, int coefficientLength) {
        if (workspace.length != 2 * coefficientLength) {
            throw new IllegalArgumentException("Workspace size mismatch");
        }
        return new ImmutableTransformResult(workspace, coefficientLength);
    }

    public double[] approximationCoeffs() {
        // Return defensive copy for compatibility
        double[] copy = new double[splitPoint];
        System.arraycopy(coefficients, 0, copy, 0, splitPoint);
        return copy;
    }

    public double[] detailCoeffs() {
        // Return defensive copy for compatibility
        double[] copy = new double[splitPoint];
        System.arraycopy(coefficients, splitPoint, copy, 0, splitPoint);
        return copy;
    }

    /**
     * Returns a read-only view of the approximation coefficients.
     * This avoids copying for read-only operations.
     */
    public DoubleBuffer approximationCoeffsView() {
        return DoubleBuffer.wrap(coefficients, 0, splitPoint).asReadOnlyBuffer();
    }

    /**
     * Returns a read-only view of the detail coefficients.
     * This avoids copying for read-only operations.
     */
    public DoubleBuffer detailCoeffsView() {
        return DoubleBuffer.wrap(coefficients, splitPoint, splitPoint).asReadOnlyBuffer();
    }

    /**
     * Direct access to approximation coefficients array segment.
     * For trusted internal operations only.
     *
     * @return Array containing all coefficients
     */
    double[] getCoefficientsArray() {
        return coefficients;
    }

    /**
     * Gets the split point between approximation and detail coefficients.
     *
     * @return The index where detail coefficients start
     */
    int getSplitPoint() {
        return splitPoint;
    }

    /**
     * Converts this immutable result to a standard TransformResult.
     * <p>
     * Note: This method creates defensive copies of the coefficient arrays.
     * For performance-critical internal operations, use the direct access
     * methods or buffer views instead.
     *
     * @return a new TransformResult with defensive copies of the coefficients
     */
    public TransformResult toTransformResult() {
        return new TransformResultImpl(approximationCoeffs(), detailCoeffs());
    }
}