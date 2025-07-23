package ai.prophetizo.wavelet;

/**
 * Transform result that tracks the original signal length before padding.
 * 
 * <p>This class is used internally when automatic padding is applied to handle
 * non-power-of-2 signal lengths. It allows the inverse transform to restore
 * the original signal dimensions.</p>
 * 
 * @since 1.2.0
 */
final class PaddedTransformResult implements TransformResult {
    
    private final TransformResult delegate;
    private final int originalLength;
    
    /**
     * Creates a padded transform result.
     * 
     * @param delegate the underlying transform result
     * @param originalLength the original signal length before padding
     */
    PaddedTransformResult(TransformResult delegate, int originalLength) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate result cannot be null");
        }
        if (originalLength <= 0) {
            throw new IllegalArgumentException("Original length must be positive");
        }
        this.delegate = delegate;
        this.originalLength = originalLength;
    }
    
    /**
     * Creates a padded transform result from coefficients.
     * 
     * @param approximationCoeffs the approximation coefficients
     * @param detailCoeffs the detail coefficients  
     * @param originalLength the original signal length before padding
     */
    PaddedTransformResult(double[] approximationCoeffs, double[] detailCoeffs, int originalLength) {
        this(new TransformResultImpl(approximationCoeffs, detailCoeffs), originalLength);
    }
    
    @Override
    public double[] approximationCoeffs() {
        return delegate.approximationCoeffs();
    }
    
    @Override
    public double[] detailCoeffs() {
        return delegate.detailCoeffs();
    }
    
    /**
     * Returns the original signal length before padding.
     * 
     * @return the original length
     */
    public int originalLength() {
        return originalLength;
    }
    
    /**
     * Returns the padded signal length.
     * 
     * @return the padded length (always a power of 2)
     */
    public int paddedLength() {
        // The padded length can be derived from coefficient lengths
        return (approximationCoeffs().length + detailCoeffs().length);
    }
    
    @Override
    public String toString() {
        return "PaddedTransformResult{" +
               "originalLength=" + originalLength +
               ", paddedLength=" + paddedLength() +
               ", approxCoeffs=" + approximationCoeffs().length +
               ", detailCoeffs=" + detailCoeffs().length +
               "}";
    }
}