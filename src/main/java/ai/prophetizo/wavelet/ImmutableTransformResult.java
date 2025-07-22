package ai.prophetizo.wavelet;

import java.nio.DoubleBuffer;

/**
 * Memory-efficient immutable transform result that avoids unnecessary copying.
 * 
 * This implementation stores coefficients in a single array and provides
 * read-only views instead of defensive copies. This significantly reduces
 * memory allocation overhead for small signal processing.
 * 
 * For trusted internal operations, use the direct array access methods.
 * For external API, use the buffer views which are read-only.
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
     * @param splitPoint Index where detail coefficients start
     */
    private ImmutableTransformResult(double[] coefficients, int splitPoint) {
        this.coefficients = coefficients;
        this.splitPoint = splitPoint;
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
     * Factory method for creating results with pre-allocated arrays.
     * Useful for batch processing and memory pooling.
     * 
     * @param workspace Pre-allocated array of size 2 * coefficientLength
     * @param coefficientLength Length of each coefficient array
     * @return New transform result using the workspace
     */
    public static ImmutableTransformResult fromWorkspace(double[] workspace, int coefficientLength) {
        if (workspace.length != 2 * coefficientLength) {
            throw new IllegalArgumentException("Workspace size mismatch");
        }
        return new ImmutableTransformResult(workspace, coefficientLength);
    }
    
    /**
     * Converts this immutable result to a standard TransformResult.
     * Creates defensive copies for compatibility.
     */
    public TransformResult toTransformResult() {
        // Since TransformResultImpl is package-private, we return our own implementation
        return new TransformResultImpl(approximationCoeffs(), detailCoeffs());
    }
}