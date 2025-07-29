package ai.prophetizo.wavelet.memory;

/**
 * Factory for creating managed arrays with different memory management strategies.
 * 
 * <p>Provides methods to create arrays backed by either traditional heap arrays
 * or Foreign Function & Memory API MemorySegments with optional SIMD alignment.</p>
 */
public interface ArrayFactory extends AutoCloseable {
    
    /**
     * Creates a managed array with the specified length.
     * 
     * @param length the array length
     * @return a new managed array
     * @throws IllegalArgumentException if length is negative
     */
    ManagedArray create(int length);
    
    /**
     * Creates a managed array with the specified length and alignment.
     * 
     * @param length the array length
     * @param alignment the memory alignment in bytes (must be power of 2)
     * @return a new managed array with guaranteed alignment
     * @throws IllegalArgumentException if length is negative or alignment is invalid
     */
    ManagedArray createAligned(int length, int alignment);
    
    /**
     * Creates a managed array from an existing heap array.
     * The data is copied into the new managed array.
     * 
     * @param data the source data
     * @return a new managed array containing a copy of the data
     * @throws NullPointerException if data is null
     */
    ManagedArray from(double[] data);
    
    /**
     * Creates a managed array from an existing heap array with alignment.
     * The data is copied into the new aligned managed array.
     * 
     * @param data the source data
     * @param alignment the memory alignment in bytes (must be power of 2)
     * @return a new aligned managed array containing a copy of the data
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if alignment is invalid
     */
    ManagedArray fromAligned(double[] data, int alignment);
    
    /**
     * Checks if this factory creates off-heap arrays.
     * 
     * @return true if arrays are created off-heap using FFM API
     */
    boolean isOffHeap();
    
    /**
     * Gets the default alignment used by this factory.
     * 
     * @return the default alignment in bytes, or 0 if no alignment
     */
    int defaultAlignment();
    
    /**
     * Releases any resources associated with this factory.
     * After calling close(), arrays created by this factory may become invalid.
     */
    @Override
    void close();
}