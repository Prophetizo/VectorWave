package ai.prophetizo.wavelet.streaming;

/**
 * A lock-free ring buffer interface for high-performance, zero-copy streaming operations.
 * 
 * <p>This interface provides a contract for circular buffer implementations that support
 * concurrent access patterns typical in streaming wavelet transform scenarios. The buffer
 * is designed to minimize memory bandwidth usage and optimize for cache efficiency.</p>
 * 
 * <p>Key design principles:</p>
 * <ul>
 *   <li><strong>Lock-free</strong>: Uses atomic operations for thread safety</li>
 *   <li><strong>Zero-copy</strong>: Provides direct access to internal buffer segments</li>
 *   <li><strong>Cache-friendly</strong>: Sequential access patterns and power-of-2 sizing</li>
 *   <li><strong>Bounded</strong>: Fixed-size buffer to prevent unbounded memory growth</li>
 * </ul>
 * 
 * @param <T> the type of elements stored in the buffer
 */
public interface RingBuffer<T> {
    
    /**
     * Gets the capacity of the ring buffer.
     * 
     * @return the maximum number of elements this buffer can hold
     */
    int capacity();
    
    /**
     * Gets the current number of elements in the buffer.
     * 
     * @return the number of elements currently stored
     */
    int size();
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if the buffer contains no elements
     */
    boolean isEmpty();
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if the buffer is at capacity
     */
    boolean isFull();
    
    /**
     * Attempts to add an element to the buffer.
     * 
     * @param element the element to add
     * @return true if the element was successfully added, false if buffer is full
     */
    boolean offer(T element);
    
    /**
     * Retrieves and removes the head element from the buffer.
     * 
     * @return the head element, or null if buffer is empty
     */
    T poll();
    
    /**
     * Retrieves but does not remove the head element.
     * 
     * @return the head element, or null if buffer is empty
     */
    T peek();
    
    /**
     * Retrieves an element at the specified offset from the head without removing it.
     * This enables zero-copy access for sliding window operations.
     * 
     * @param offset the offset from the head (0 = head, 1 = next element, etc.)
     * @return the element at the specified offset, or null if offset is out of bounds
     * @throws IndexOutOfBoundsException if offset is negative or >= size()
     */
    T peek(int offset);
    
    /**
     * Provides direct access to a contiguous segment of the buffer for zero-copy operations.
     * The returned segment is valid until the next write operation.
     * 
     * @param startOffset the starting offset from the head
     * @param length the number of elements to include in the segment
     * @return a BufferSegment providing direct access to the requested data
     * @throws IndexOutOfBoundsException if the segment would extend beyond available data
     */
    BufferSegment<T> getSegment(int startOffset, int length);
    
    /**
     * Clears all elements from the buffer.
     */
    void clear();
    
    /**
     * Represents a contiguous segment of the ring buffer for zero-copy access.
     * 
     * @param <T> the type of elements in the segment
     */
    interface BufferSegment<T> {
        
        /**
         * Gets the element at the specified index within this segment.
         * 
         * @param index the index within the segment (0-based)
         * @return the element at the specified index
         * @throws IndexOutOfBoundsException if index is out of segment bounds
         */
        T get(int index);
        
        /**
         * Gets the length of this segment.
         * 
         * @return the number of elements in this segment
         */
        int length();
        
        /**
         * Checks if this segment is still valid. Segments become invalid after
         * write operations to the underlying buffer.
         * 
         * @return true if this segment is still valid for reading
         */
        boolean isValid();
        
        /**
         * Copies the segment data to the provided array for safe access beyond
         * the segment's validity period.
         * 
         * @param destination the array to copy data into
         * @param destOffset the starting offset in the destination array
         * @throws IllegalArgumentException if destination array is too small
         */
        void copyTo(T[] destination, int destOffset);
    }
}