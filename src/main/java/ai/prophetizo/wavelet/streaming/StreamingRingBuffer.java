package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Specialized ring buffer for streaming wavelet transforms with sliding window support.
 * 
 * <p>This buffer provides zero-copy sliding window operations optimized for
 * overlap-based processing in streaming scenarios.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Zero-copy sliding windows through view operations</li>
 *   <li>Efficient overlap management for streaming denoisers</li>
 *   <li>Direct buffer access for SIMD operations</li>
 *   <li>Automatic window advancement</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is fully thread-safe. It maintains the same 
 * thread-safety guarantees as {@link RingBuffer} for read/write operations. The window 
 * extraction methods ({@link #getWindowDirect()}, {@link #processWindow}) use ThreadLocal 
 * buffers, making them thread-safe while avoiding allocations. Each thread gets its own 
 * set of buffers, eliminating data races without synchronization overhead.</p>
 */
public class StreamingRingBuffer extends RingBuffer {
    
    private final int windowSize;
    private final int hopSize;
    private final int overlapSize;
    
    // ThreadLocal buffers for thread-safe window extraction without allocation
    private final ThreadLocal<double[]> windowBuffer;
    private final ThreadLocal<double[]> processingBuffer;
    
    /**
     * Creates a streaming ring buffer with sliding window support.
     * 
     * @param capacity the total buffer capacity (must be power of 2)
     * @param windowSize the sliding window size
     * @param hopSize the hop size between windows
     * @throws InvalidArgumentException if parameters are invalid
     */
    public StreamingRingBuffer(int capacity, int windowSize, int hopSize) {
        super(capacity);
        
        if (windowSize < 1) {
            throw new InvalidArgumentException("Window size must be positive, got: " + windowSize);
        }
        if (hopSize < 1 || hopSize > windowSize) {
            throw new InvalidArgumentException("Hop size must be between 1 and window size, got: " + hopSize);
        }
        if (capacity < windowSize * 2) {
            throw new InvalidArgumentException("Capacity must be at least 2x window size for proper operation");
        }
        
        this.windowSize = windowSize;
        this.hopSize = hopSize;
        this.overlapSize = windowSize - hopSize;
        
        // Initialize ThreadLocal buffers - each thread gets its own buffer
        this.windowBuffer = ThreadLocal.withInitial(() -> new double[windowSize]);
        this.processingBuffer = ThreadLocal.withInitial(() -> new double[windowSize]);
    }
    
    /**
     * Checks if a complete window is available for processing.
     * 
     * @return true if at least windowSize samples are available
     */
    public boolean hasWindow() {
        return available() >= windowSize;
    }
    
    /**
     * Extracts the current window without advancing positions.
     * 
     * @param output the array to fill with window data
     * @return true if window was extracted, false if insufficient data
     */
    public boolean getWindow(double[] output) {
        if (output == null || output.length < windowSize) {
            throw new InvalidArgumentException("Output array must have at least windowSize elements");
        }
        
        if (!hasWindow()) {
            return false;
        }
        
        peek(output, 0, windowSize);
        return true;
    }
    
    /**
     * Extracts the current window into a thread-local buffer without advancing positions.
     * This method is useful for zero-allocation processing.
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe. Each thread gets its own
     * internal buffer, eliminating data races while avoiding allocations.</p>
     * 
     * @return the thread-local window buffer if data is available, null otherwise
     */
    public double[] getWindowDirect() {
        if (!hasWindow()) {
            return null;
        }
        
        double[] buffer = windowBuffer.get();
        peek(buffer, 0, windowSize);
        return buffer;
    }
    
    /**
     * Advances the window by hopSize positions.
     * 
     * @return true if advance was successful, false if insufficient data
     */
    public boolean advanceWindow() {
        if (available() < hopSize) {
            return false;
        }
        
        skip(hopSize);
        return true;
    }
    
    /**
     * Processes the current window and advances by hopSize.
     * This is the main method for streaming processing.
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe. Each thread gets its own
     * processing buffer, allowing concurrent window processing.</p>
     * 
     * @param processor the processor to apply to the window
     * @return true if processing occurred, false if insufficient data
     */
    public boolean processWindow(WindowProcessor processor) {
        if (!hasWindow()) {
            return false;
        }
        
        // Get current window using thread-local buffer
        double[] buffer = processingBuffer.get();
        peek(buffer, 0, windowSize);
        
        // Process the window
        processor.process(buffer, 0, windowSize);
        
        // Advance by hop size
        advanceWindow();
        
        return true;
    }
    
    /**
     * Gets the overlap region from the previous window.
     * Useful for overlap-add or overlap-save processing.
     * 
     * @param output the array to fill with overlap data
     * @return true if overlap was extracted, false if insufficient data
     */
    public boolean getOverlap(double[] output) {
        if (output == null || output.length < overlapSize) {
            throw new InvalidArgumentException("Output array must have at least overlapSize elements");
        }
        
        if (overlapSize == 0 || available() < overlapSize) {
            return false;
        }
        
        // Overlap is at the beginning of current window
        peek(output, 0, overlapSize);
        return true;
    }
    
    /**
     * Fills the buffer to prepare for streaming.
     * This method ensures enough data is available for the first window.
     * 
     * @param data the data to fill with
     * @param offset the offset in the data array
     * @param length the length of data to use
     * @return true if buffer has enough data for a window after filling
     */
    public boolean fillForStreaming(double[] data, int offset, int length) {
        write(data, offset, length);
        return hasWindow();
    }
    
    /**
     * Returns the configured window size.
     * 
     * @return the window size
     */
    public int getWindowSize() {
        return windowSize;
    }
    
    /**
     * Returns the configured hop size.
     * 
     * @return the hop size
     */
    public int getHopSize() {
        return hopSize;
    }
    
    /**
     * Returns the overlap size (windowSize - hopSize).
     * 
     * @return the overlap size
     */
    public int getOverlapSize() {
        return overlapSize;
    }
    
    /**
     * Cleans up ThreadLocal resources for the current thread.
     * 
     * <p>Call this method when a thread is done using the StreamingRingBuffer
     * to prevent potential memory leaks in thread pool scenarios.</p>
     */
    public void cleanupThread() {
        windowBuffer.remove();
        processingBuffer.remove();
    }
    
    /**
     * Gets the thread-local processing buffer for zero-copy operations.
     * 
     * <p>This method provides access to the internal thread-local buffer,
     * allowing callers to reuse it for operations like partial window
     * processing in flush() methods, avoiding allocations.</p>
     * 
     * @return the thread-local processing buffer of size windowSize
     */
    public double[] getProcessingBuffer() {
        return processingBuffer.get();
    }
    
    /**
     * Functional interface for window processing.
     */
    @FunctionalInterface
    public interface WindowProcessor {
        /**
         * Processes a window of data.
         * 
         * @param data the window data
         * @param offset the offset in the data array
         * @param length the length of the window
         */
        void process(double[] data, int offset, int length);
    }
}