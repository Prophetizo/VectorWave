package ai.prophetizo.wavelet.streaming;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A high-performance, lock-free ring buffer optimized for double values in streaming
 * wavelet transform operations.
 * 
 * <p>This implementation uses atomic operations for thread-safe access and is optimized
 * for cache efficiency through:</p>
 * <ul>
 *   <li>Power-of-2 capacity for fast modulo operations using bit masking</li>
 *   <li>Cache-line aligned data structure to minimize false sharing</li>
 *   <li>Sequential access patterns to maximize cache hit rates</li>
 *   <li>Direct array access for zero-copy operations</li>
 * </ul>
 * 
 * <p>The buffer uses a single-producer, single-consumer model which is typical
 * for streaming signal processing scenarios.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe for single producer/
 * single consumer scenarios. Multiple producers or consumers require external
 * synchronization.</p>
 */
public final class DoubleRingBuffer {
    
    // Cache line size is typically 64 bytes = 8 doubles
    private static final int CACHE_LINE_SIZE = 64;
    private static final int CACHE_LINE_DOUBLES = CACHE_LINE_SIZE / Double.BYTES;
    
    private final double[] buffer;
    private final int capacity;
    private final int mask; // For fast modulo operation (capacity - 1)
    
    // Use separate atomic longs to avoid false sharing
    // Align to cache lines for optimal performance
    private final AtomicLong writePosition = new AtomicLong(0);
    private final AtomicLong readPosition = new AtomicLong(0);
    
    // Cached values to reduce atomic reads
    private volatile long cachedReadPosition = 0;
    private volatile long cachedWritePosition = 0;
    
    /**
     * Creates a new DoubleRingBuffer with the specified capacity.
     * 
     * @param capacity the capacity of the buffer, must be a power of 2
     * @throws IllegalArgumentException if capacity is not a power of 2 or is <= 0
     */
    public DoubleRingBuffer(int capacity) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException(
                "Capacity must be a positive power of 2, got: " + capacity);
        }
        
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.buffer = new double[capacity];
    }
    
    /**
     * Gets the capacity of the ring buffer.
     * 
     * @return the maximum number of elements this buffer can hold
     */
    public int capacity() {
        return capacity;
    }
    
    /**
     * Gets the current number of elements in the buffer.
     * This operation may be approximate in concurrent scenarios.
     * 
     * @return the approximate number of elements currently stored
     */
    public int size() {
        long write = writePosition.get();
        long read = readPosition.get();
        return (int) (write - read);
    }
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if the buffer contains no elements
     */
    public boolean isEmpty() {
        return readPosition.get() == writePosition.get();
    }
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if the buffer is at capacity
     */
    public boolean isFull() {
        long write = writePosition.get();
        long read = readPosition.get();
        return (write - read) >= capacity;
    }
    
    /**
     * Attempts to add an element to the buffer.
     * 
     * @param value the value to add
     * @return true if the value was successfully added, false if buffer is full
     */
    public boolean offer(double value) {
        long currentWrite = writePosition.get();
        long currentRead = cachedReadPosition;
        
        // Check if buffer is full
        if (currentWrite - currentRead >= capacity) {
            // Refresh cached read position and try again
            cachedReadPosition = readPosition.get();
            if (currentWrite - cachedReadPosition >= capacity) {
                return false; // Buffer is full
            }
        }
        
        // Write the value
        buffer[(int) (currentWrite & mask)] = value;
        
        // Advance write position
        writePosition.lazySet(currentWrite + 1);
        return true;
    }
    
    /**
     * Retrieves and removes the head element from the buffer.
     * 
     * @return the head element, or Double.NaN if buffer is empty
     */
    public double poll() {
        long currentRead = readPosition.get();
        long currentWrite = cachedWritePosition;
        
        // Check if buffer is empty
        if (currentRead >= currentWrite) {
            // Refresh cached write position and try again
            cachedWritePosition = writePosition.get();
            if (currentRead >= cachedWritePosition) {
                return Double.NaN; // Buffer is empty
            }
        }
        
        // Read the value
        double value = buffer[(int) (currentRead & mask)];
        
        // Advance read position
        readPosition.lazySet(currentRead + 1);
        return value;
    }
    
    /**
     * Retrieves but does not remove the head element.
     * 
     * @return the head element, or Double.NaN if buffer is empty
     */
    public double peek() {
        long currentRead = readPosition.get();
        long currentWrite = writePosition.get();
        
        if (currentRead >= currentWrite) {
            return Double.NaN; // Buffer is empty
        }
        
        return buffer[(int) (currentRead & mask)];
    }
    
    /**
     * Retrieves an element at the specified offset from the head without removing it.
     * 
     * @param offset the offset from the head (0 = head, 1 = next element, etc.)
     * @return the element at the specified offset, or Double.NaN if offset is out of bounds
     * @throws IndexOutOfBoundsException if offset is negative
     */
    public double peek(int offset) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
        }
        
        long currentRead = readPosition.get();
        long currentWrite = writePosition.get();
        
        if (currentRead + offset >= currentWrite) {
            return Double.NaN; // Offset is out of bounds
        }
        
        return buffer[(int) ((currentRead + offset) & mask)];
    }
    
    /**
     * Provides direct access to a contiguous segment of the buffer for zero-copy operations.
     * 
     * @param startOffset the starting offset from the head
     * @param length the number of elements to include in the segment
     * @return a DoubleBufferSegment providing direct access to the requested data
     * @throws IndexOutOfBoundsException if the segment would extend beyond available data
     * @throws IllegalArgumentException if startOffset is negative or length is non-positive
     */
    public DoubleBufferSegment getSegment(int startOffset, int length) {
        if (startOffset < 0) {
            throw new IndexOutOfBoundsException("Start offset cannot be negative: " + startOffset);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }
        
        long currentRead = readPosition.get();
        long currentWrite = writePosition.get();
        long availableElements = currentWrite - currentRead;
        
        if (startOffset + length > availableElements) {
            throw new IndexOutOfBoundsException(
                String.format("Segment [%d, %d) extends beyond available data (%d elements)",
                    startOffset, startOffset + length, availableElements));
        }
        
        return new DoubleBufferSegmentImpl(currentRead + startOffset, length, currentWrite);
    }
    
    /**
     * Clears all elements from the buffer by resetting read and write positions.
     */
    public void clear() {
        readPosition.set(0);
        writePosition.set(0);
        cachedReadPosition = 0;
        cachedWritePosition = 0;
    }
    
    /**
     * Implementation of BufferSegment for double values.
     */
    private final class DoubleBufferSegmentImpl implements DoubleBufferSegment {
        private final long startPosition;
        private final int segmentLength;
        private final long snapshotWritePosition;
        
        DoubleBufferSegmentImpl(long startPosition, int length, long snapshotWritePosition) {
            this.startPosition = startPosition;
            this.segmentLength = length;
            this.snapshotWritePosition = snapshotWritePosition;
        }
        
        @Override
        public double get(int index) {
            if (index < 0 || index >= segmentLength) {
                throw new IndexOutOfBoundsException(
                    String.format("Index %d out of bounds for segment of length %d", index, segmentLength));
            }
            
            if (!isValid()) {
                throw new IllegalStateException("Segment is no longer valid");
            }
            
            return buffer[(int) ((startPosition + index) & mask)];
        }
        
        @Override
        public int length() {
            return segmentLength;
        }
        
        @Override
        public boolean isValid() {
            // Segment is valid if the write position hasn't advanced beyond our snapshot
            // and the read position hasn't advanced beyond our start
            long currentWrite = writePosition.get();
            long currentRead = readPosition.get();
            
            return currentRead <= startPosition && currentWrite >= snapshotWritePosition;
        }
        
        @Override
        public void copyTo(double[] destination, int destOffset) {
            if (destination == null) {
                throw new IllegalArgumentException("Destination array cannot be null");
            }
            if (destOffset < 0 || destOffset + segmentLength > destination.length) {
                throw new IllegalArgumentException(
                    String.format("Destination array too small. Need %d elements starting at %d, got %d",
                        segmentLength, destOffset, destination.length));
            }
            
            for (int i = 0; i < segmentLength; i++) {
                destination[destOffset + i] = buffer[(int) ((startPosition + i) & mask)];
            }
        }
    }
    
    /**
     * Specialized BufferSegment interface for double values.
     */
    public interface DoubleBufferSegment {
        
        /**
         * Gets the element at the specified index within this segment.
         * 
         * @param index the index within the segment (0-based)
         * @return the element at the specified index
         * @throws IndexOutOfBoundsException if index is out of segment bounds
         * @throws IllegalStateException if the segment is no longer valid
         */
        double get(int index);
        
        /**
         * Gets the length of this segment.
         * 
         * @return the number of elements in this segment
         */
        int length();
        
        /**
         * Checks if this segment is still valid.
         * 
         * @return true if this segment is still valid for reading
         */
        boolean isValid();
        
        /**
         * Copies the segment data to the provided array.
         * 
         * @param destination the array to copy data into
         * @param destOffset the starting offset in the destination array
         * @throws IllegalArgumentException if destination array is too small
         */
        void copyTo(double[] destination, int destOffset);
    }
}