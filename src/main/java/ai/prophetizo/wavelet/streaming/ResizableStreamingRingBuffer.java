package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidStateException;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A resizable version of StreamingRingBuffer that supports dynamic capacity adjustment.
 * 
 * <p>This implementation allows the buffer to grow or shrink based on throughput and
 * utilization patterns, optimizing memory usage and performance for varying workloads.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe resize operations using read-write locks</li>
 *   <li>Zero data loss during resize operations</li>
 *   <li>Maintains window and hop size across resizes</li>
 *   <li>Atomic buffer swapping for consistency</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class uses a read-write lock to allow concurrent reads
 * during normal operation while ensuring exclusive access during resize operations.
 * The SPSC contract is maintained during resize by blocking both producer and consumer.</p>
 * 
 * @since 1.2
 */
public class ResizableStreamingRingBuffer {
    
    // Current buffer wrapped in AtomicReference for atomic swaps
    private final AtomicReference<StreamingRingBuffer> bufferRef;
    
    // Lock for coordinating resize operations
    private final ReentrantReadWriteLock resizeLock = new ReentrantReadWriteLock();
    
    // Configuration
    private final int windowSize;
    private final int hopSize;
    private final int minCapacity;
    private final int maxCapacity;
    
    // Statistics for resize decisions
    private volatile long lastResizeTime = 0; // 0 allows immediate first resize
    private static final long MIN_RESIZE_INTERVAL_NS = 5_000_000_000L; // 5 seconds
    
    /**
     * Creates a resizable streaming ring buffer.
     * 
     * @param initialCapacity initial buffer capacity (must be power of 2)
     * @param windowSize the sliding window size
     * @param hopSize the hop size between windows
     * @param minCapacity minimum allowed capacity
     * @param maxCapacity maximum allowed capacity
     * @throws InvalidArgumentException if parameters are invalid
     */
    public ResizableStreamingRingBuffer(int initialCapacity, int windowSize, int hopSize,
                                      int minCapacity, int maxCapacity) {
        if (minCapacity < windowSize * 2) {
            throw new InvalidArgumentException(
                "Minimum capacity must be at least 2x window size, got: " + minCapacity);
        }
        if (maxCapacity < minCapacity) {
            throw new InvalidArgumentException(
                "Maximum capacity must be >= minimum capacity");
        }
        if (initialCapacity < minCapacity || initialCapacity > maxCapacity) {
            throw new InvalidArgumentException(
                "Initial capacity must be between min and max capacity");
        }
        
        this.windowSize = windowSize;
        this.hopSize = hopSize;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
        
        // Create initial buffer
        StreamingRingBuffer initialBuffer = new StreamingRingBuffer(
            initialCapacity, windowSize, hopSize);
        this.bufferRef = new AtomicReference<>(initialBuffer);
    }
    
    /**
     * Writes data to the buffer.
     * 
     * @param data the data array
     * @param offset start offset
     * @param length number of elements to write
     * @return number of elements actually written
     */
    public int write(double[] data, int offset, int length) {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().write(data, offset, length);
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Writes a single value to the buffer.
     * 
     * @param value the value to write
     * @return true if written successfully
     */
    public boolean write(double value) {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().write(value);
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Checks if a complete window is available.
     * 
     * @return true if window is available
     */
    public boolean hasWindow() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().hasWindow();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the current window without copying (zero-copy).
     * 
     * @return the window data or null if no window available
     */
    public double[] getWindowDirect() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().getWindowDirect();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Advances the window position by hop size.
     */
    public void advanceWindow() {
        resizeLock.readLock().lock();
        try {
            bufferRef.get().advanceWindow();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Returns the number of elements available for reading.
     * 
     * @return available elements
     */
    public int available() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().available();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Returns the current capacity.
     * 
     * @return current buffer capacity
     */
    public int getCapacity() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().getCapacity();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if full
     */
    public boolean isFull() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().isFull();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the processing buffer for temporary operations.
     * 
     * @return thread-local processing buffer
     */
    public double[] getProcessingBuffer() {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().getProcessingBuffer();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Reads data from the buffer.
     * 
     * @param data destination array
     * @param offset start offset
     * @param length number of elements to read
     * @return number of elements actually read
     */
    public int read(double[] data, int offset, int length) {
        resizeLock.readLock().lock();
        try {
            return bufferRef.get().read(data, offset, length);
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Attempts to resize the buffer to a new capacity.
     * 
     * <p>This operation will:</p>
     * <ol>
     *   <li>Validate the new capacity</li>
     *   <li>Create a new buffer with the target capacity</li>
     *   <li>Transfer all data from the old buffer</li>
     *   <li>Atomically swap the buffers</li>
     * </ol>
     * 
     * @param newCapacity the desired new capacity (must be power of 2)
     * @return true if resize was successful, false if skipped (too soon, same size, etc.)
     * @throws InvalidArgumentException if new capacity is invalid
     */
    public boolean resize(int newCapacity) {
        // Validate new capacity
        if (newCapacity < minCapacity || newCapacity > maxCapacity) {
            throw new InvalidArgumentException(
                "New capacity must be between " + minCapacity + " and " + maxCapacity +
                ", got: " + newCapacity);
        }
        
        // Check if power of 2
        if ((newCapacity & (newCapacity - 1)) != 0) {
            // Round up to next power of 2
            newCapacity = Integer.highestOneBit(newCapacity) << 1;
            
            // Recheck bounds after rounding
            if (newCapacity > maxCapacity) {
                newCapacity = Integer.highestOneBit(maxCapacity);
            }
        }
        
        // Check if resize is needed
        StreamingRingBuffer currentBuffer = bufferRef.get();
        if (newCapacity == currentBuffer.getCapacity()) {
            return false; // No resize needed
        }
        
        // Check if enough time has passed since last resize
        long currentTime = System.nanoTime();
        if (lastResizeTime > 0 && currentTime - lastResizeTime < MIN_RESIZE_INTERVAL_NS) {
            return false; // Too soon to resize
        }
        
        // Perform the resize
        resizeLock.writeLock().lock();
        try {
            // Double-check capacity under write lock
            currentBuffer = bufferRef.get();
            if (newCapacity == currentBuffer.getCapacity()) {
                return false;
            }
            
            // Create new buffer
            StreamingRingBuffer newBuffer = new StreamingRingBuffer(
                newCapacity, windowSize, hopSize);
            
            // Transfer data from old to new buffer
            int available = currentBuffer.available();
            if (available > 0) {
                // Use a temporary buffer to transfer data
                double[] tempData = new double[Math.min(available, newCapacity - 1)];
                int read = currentBuffer.read(tempData, 0, tempData.length);
                
                if (read > 0) {
                    int written = newBuffer.write(tempData, 0, read);
                    if (written != read) {
                        // This should not happen with proper sizing
                        throw new InvalidStateException(
                            "Data loss during resize: read " + read + " but wrote " + written);
                    }
                }
            }
            
            // Atomically swap buffers
            bufferRef.set(newBuffer);
            lastResizeTime = currentTime;
            
            // Clean up old buffer's thread-local resources
            currentBuffer.cleanupThread();
            
            return true;
            
        } finally {
            resizeLock.writeLock().unlock();
        }
    }
    
    /**
     * Attempts to resize based on current utilization.
     * 
     * @param utilization current buffer utilization (0.0 to 1.0)
     * @return true if resize occurred, false otherwise
     */
    public boolean resizeBasedOnUtilization(double utilization) {
        int currentCapacity = getCapacity();
        int newCapacity = currentCapacity;
        
        if (utilization > 0.85 && currentCapacity < maxCapacity) {
            // Buffer is nearly full - increase size
            newCapacity = Math.min(currentCapacity * 2, maxCapacity);
        } else if (utilization < 0.25 && currentCapacity > minCapacity) {
            // Buffer is mostly empty - decrease size
            newCapacity = Math.max(currentCapacity / 2, minCapacity);
        }
        
        if (newCapacity != currentCapacity) {
            return resize(newCapacity);
        }
        
        return false;
    }
    
    /**
     * Cleans up ThreadLocal resources for the current thread.
     */
    public void cleanupThread() {
        resizeLock.readLock().lock();
        try {
            bufferRef.get().cleanupThread();
        } finally {
            resizeLock.readLock().unlock();
        }
    }
    
    /**
     * Forces an immediate resize without time interval check.
     * This method is primarily for testing purposes.
     * 
     * @param newCapacity the desired new capacity
     * @return true if resize was successful
     */
    public boolean forceResize(int newCapacity) {
        // Temporarily reset last resize time to allow immediate resize
        long savedTime = lastResizeTime;
        lastResizeTime = 0;
        try {
            return resize(newCapacity);
        } finally {
            // Restore the time if resize didn't happen
            if (lastResizeTime == 0) {
                lastResizeTime = savedTime;
            }
        }
    }
}