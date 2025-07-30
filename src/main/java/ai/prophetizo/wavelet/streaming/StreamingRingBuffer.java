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
 * <p><b>Thread Safety:</b> This class maintains the same thread-safety guarantees as
 * {@link RingBuffer} for read/write operations. However, the window extraction methods
 * ({@link #getWindowDirect()}, {@link #processWindow}) use shared internal buffers and
 * are NOT thread-safe. Only one thread should call these methods at a time. If concurrent
 * window extraction is needed, use {@link #getWindow(double[])} with separate output arrays
 * or synchronize access to the window extraction methods.</p>
 */
public class StreamingRingBuffer extends RingBuffer {
    
    private final int windowSize;
    private final int hopSize;
    private final int overlapSize;
    
    // Working arrays for window extraction (reused to avoid allocation)
    // WARNING: These shared buffers make getWindowDirect() and processWindow() NOT thread-safe
    // Only one thread should call these methods at a time, or use getWindow() with external arrays
    private final double[] windowBuffer;
    private final double[] processingBuffer;
    
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
        
        // Pre-allocate working buffers
        this.windowBuffer = new double[windowSize];
        this.processingBuffer = new double[windowSize];
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
     * Extracts the current window into the internal buffer without advancing positions.
     * This method is useful for zero-allocation processing.
     * 
     * <p><b>Thread Safety:</b> This method is NOT thread-safe as it uses a shared internal buffer.
     * Only one thread should call this method at a time. For concurrent access, use
     * {@link #getWindow(double[])} with separate output arrays.</p>
     * 
     * @return the internal window buffer if data is available, null otherwise
     */
    public double[] getWindowDirect() {
        if (!hasWindow()) {
            return null;
        }
        
        peek(windowBuffer, 0, windowSize);
        return windowBuffer;
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
     * <p><b>Thread Safety:</b> This method is NOT thread-safe as it uses a shared internal buffer.
     * Only one thread should call this method at a time.</p>
     * 
     * @param processor the processor to apply to the window
     * @return true if processing occurred, false if insufficient data
     */
    public boolean processWindow(WindowProcessor processor) {
        if (!hasWindow()) {
            return false;
        }
        
        // Get current window
        peek(processingBuffer, 0, windowSize);
        
        // Process the window
        processor.process(processingBuffer, 0, windowSize);
        
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