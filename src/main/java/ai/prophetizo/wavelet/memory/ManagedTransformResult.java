package ai.prophetizo.wavelet.memory;

import ai.prophetizo.wavelet.TransformResult;

/**
 * Memory-managed transform result using ManagedArrays for improved memory management.
 * 
 * <p>This class provides similar functionality to TransformResult but uses
 * ManagedArrays internally, allowing for both heap and off-heap memory management.
 * It does not implement TransformResult due to the sealed interface restriction.</p>
 */
public final class ManagedTransformResult implements AutoCloseable {
    
    private final ManagedArray approximationCoeffs;
    private final ManagedArray detailCoeffs;
    
    /**
     * Creates a new managed transform result with the given coefficients.
     * 
     * @param approximationCoeffs the approximation coefficients
     * @param detailCoeffs the detail coefficients
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public ManagedTransformResult(ManagedArray approximationCoeffs, ManagedArray detailCoeffs) {
        if (approximationCoeffs == null) {
            throw new NullPointerException("approximationCoeffs cannot be null");
        }
        if (detailCoeffs == null) {
            throw new NullPointerException("detailCoeffs cannot be null");
        }
        if (approximationCoeffs.length() != detailCoeffs.length()) {
            throw new IllegalArgumentException("Coefficient arrays must have the same length: " +
                    approximationCoeffs.length() + " != " + detailCoeffs.length());
        }
        
        this.approximationCoeffs = approximationCoeffs;
        this.detailCoeffs = detailCoeffs;
    }
    
    /**
     * Gets the approximation coefficients as a heap array for backward compatibility.
     * 
     * @return a defensive copy of the approximation coefficients
     */
    public double[] approximationCoeffs() {
        // Return defensive copy for backward compatibility
        return approximationCoeffs.toArray();
    }
    
    /**
     * Gets the detail coefficients as a heap array for backward compatibility.
     * 
     * @return a defensive copy of the detail coefficients
     */
    public double[] detailCoeffs() {
        // Return defensive copy for backward compatibility
        return detailCoeffs.toArray();
    }
    
    /**
     * Gets the approximation coefficients as a ManagedArray.
     * 
     * <p><strong>Warning:</strong> The returned array is not a copy and should
     * be treated as read-only to maintain immutability semantics.</p>
     * 
     * @return the approximation coefficients ManagedArray
     */
    public ManagedArray getManagedApproximationCoeffs() {
        return approximationCoeffs;
    }
    
    /**
     * Gets the detail coefficients as a ManagedArray.
     * 
     * <p><strong>Warning:</strong> The returned array is not a copy and should
     * be treated as read-only to maintain immutability semantics.</p>
     * 
     * @return the detail coefficients ManagedArray
     */
    public ManagedArray getManagedDetailCoeffs() {
        return detailCoeffs;
    }
    
    /**
     * Checks if this result uses off-heap memory.
     * 
     * @return true if the coefficients are stored off-heap
     */
    public boolean isOffHeap() {
        return approximationCoeffs.isOffHeap() && detailCoeffs.isOffHeap();
    }
    
    /**
     * Gets the memory alignment of the coefficient arrays.
     * 
     * @return the minimum alignment of both coefficient arrays, or 0 if not aligned
     */
    public int alignment() {
        return Math.min(approximationCoeffs.alignment(), detailCoeffs.alignment());
    }
    
    /**
     * Releases any resources associated with this transform result.
     * After calling close(), the result should not be used.
     */
    @Override
    public void close() {
        approximationCoeffs.close();
        detailCoeffs.close();
    }
    
    @Override
    public String toString() {
        return String.format("ManagedTransformResult{approxLength=%d, detailLength=%d, offHeap=%s, alignment=%d}",
                approximationCoeffs.length(), detailCoeffs.length(), isOffHeap(), alignment());
    }
}