package ai.prophetizo.wavelet.memory;

/**
 * Interface for memory-managed double arrays that can be backed by either 
 * traditional heap arrays or Foreign Function & Memory API MemorySegments.
 * 
 * <p>Provides a unified interface for array operations while hiding the 
 * underlying memory management strategy.</p>
 */
public interface ManagedArray extends AutoCloseable {
    
    /**
     * Gets the length of the array.
     * 
     * @return the array length
     */
    int length();
    
    /**
     * Gets the value at the specified index.
     * 
     * @param index the index
     * @return the value at the index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    double get(int index);
    
    /**
     * Sets the value at the specified index.
     * 
     * @param index the index
     * @param value the value to set
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    void set(int index, double value);
    
    /**
     * Copies data from this array to a heap array.
     * 
     * @param dest the destination array
     * @param destOffset the offset in the destination array
     * @param srcOffset the offset in this array
     * @param length the number of elements to copy
     * @throws IndexOutOfBoundsException if any index is out of bounds
     */
    void copyTo(double[] dest, int destOffset, int srcOffset, int length);
    
    /**
     * Copies data from a heap array to this array.
     * 
     * @param src the source array
     * @param srcOffset the offset in the source array
     * @param destOffset the offset in this array
     * @param length the number of elements to copy
     * @throws IndexOutOfBoundsException if any index is out of bounds
     */
    void copyFrom(double[] src, int srcOffset, int destOffset, int length);
    
    /**
     * Copies data from this array to another managed array.
     * 
     * @param dest the destination array
     * @param destOffset the offset in the destination array
     * @param srcOffset the offset in this array
     * @param length the number of elements to copy
     * @throws IndexOutOfBoundsException if any index is out of bounds
     */
    void copyTo(ManagedArray dest, int destOffset, int srcOffset, int length);
    
    /**
     * Returns a traditional heap array copy of this managed array.
     * For backward compatibility.
     * 
     * @return a heap array copy
     */
    double[] toArray();
    
    /**
     * Fills the array with the specified value.
     * 
     * @param value the value to fill with
     */
    void fill(double value);
    
    /**
     * Checks if this array is backed by a MemorySegment.
     * 
     * @return true if using FFM API, false if using heap arrays
     */
    boolean isOffHeap();
    
    /**
     * Returns the memory alignment of this array in bytes.
     * For SIMD optimization, returns the alignment boundary.
     * 
     * @return alignment in bytes, or 0 if not aligned
     */
    int alignment();
    
    /**
     * Releases any resources associated with this array.
     * After calling close(), the array should not be used.
     */
    @Override
    void close();
}