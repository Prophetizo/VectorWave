package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import java.util.concurrent.atomic.AtomicReference;

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
 * <p><b>Thread Safety:</b> This class inherits the SPSC (Single Producer, Single Consumer) 
 * thread-safety model from {@link RingBuffer}. The window extraction methods 
 * ({@link #getWindowDirect()}, {@link #processWindow}) use ThreadLocal buffers to avoid 
 * allocations, but they are NOT safe for concurrent use by multiple reader threads. 
 * Only one thread should perform read operations (including window extraction) at a time. 
 * The ThreadLocal buffers only provide memory isolation, not thread-safe access to the 
 * underlying ring buffer data.</p>
 */
public class StreamingRingBuffer extends RingBuffer {
    
    private final int windowSize;
    private final int hopSize;
    private final int overlapSize;
    
    // ThreadLocal buffers for thread-safe window extraction without allocation
    private final ThreadLocal<double[]> windowBuffer;
    private final ThreadLocal<double[]> processingBuffer;
    
    // Concurrent reader detection (only in assertions, zero overhead in production)
    private final AtomicReference<Thread> currentReader = new AtomicReference<>(null);
    
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
     * <p><b>Thread Safety:</b> This method is NOT safe for concurrent use by multiple threads.
     * While each thread gets its own buffer (via ThreadLocal), the underlying peek operation
     * is not synchronized. Use this method only in SPSC scenarios where a single thread
     * performs all read operations.</p>
     * 
     * @return the thread-local window buffer if data is available, null otherwise
     */
    public double[] getWindowDirect() {
        if (!hasWindow()) {
            return null;
        }
        
        // Detect concurrent readers (only active with -ea flag)
        assert enterReader() : "Concurrent reader detected! Only one thread should read from StreamingRingBuffer at a time.";
        
        try {
            double[] buffer = windowBuffer.get();
            peek(buffer, 0, windowSize);
            return buffer;
        } finally {
            assert exitReader();
        }
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
     * <p><b>Thread Safety:</b> This method is NOT safe for concurrent use by multiple threads.
     * While each thread gets its own buffer (via ThreadLocal), the underlying peek and skip
     * operations are not synchronized. Use this method only in SPSC scenarios where a single
     * thread performs all read operations.</p>
     * 
     * @param processor the processor to apply to the window
     * @return true if processing occurred, false if insufficient data
     */
    public boolean processWindow(WindowProcessor processor) {
        if (!hasWindow()) {
            return false;
        }
        
        // Detect concurrent readers (only active with -ea flag)
        assert enterReader() : "Concurrent reader detected! Only one thread should read from StreamingRingBuffer at a time.";
        
        try {
            // Get current window using thread-local buffer
            double[] buffer = processingBuffer.get();
            peek(buffer, 0, windowSize);
            
            // Process the window
            processor.process(buffer, 0, windowSize);
            
            // Advance by hop size
            advanceWindow();
            
            return true;
        } finally {
            assert exitReader();
        }
    }
    
    /**
     * Gets the overlap region from the previous window.
     * Useful for overlap-add or overlap-save processing.
     * 
     * @param output the array to fill with overlap data
     * @return true if overlap was extracted, false if insufficient data or no overlap configured
     * @throws InvalidArgumentException if output is null or too small (when overlapSize > 0)
     */
    public boolean getOverlap(double[] output) {
        // No overlap configured - return false without validation
        if (overlapSize == 0) {
            return false;
        }
        
        if (output == null || output.length < overlapSize) {
            throw new InvalidArgumentException("Output array must have at least overlapSize elements");
        }
        
        if (available() < overlapSize) {
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
     * 
     * <p><b>When to call:</b></p>
     * <ul>
     *   <li>Before a thread pool worker thread is returned to the pool</li>
     *   <li>In finally blocks when using temporary threads</li>
     *   <li>When switching between different StreamingRingBuffer instances on the same thread</li>
     * </ul>
     * 
     * <p><b>Consequences of not calling:</b></p>
     * <ul>
     *   <li>Each thread retains references to windowSize-sized double arrays (16-64KB typically)</li>
     *   <li>In thread pools, these references persist across task boundaries</li>
     *   <li>Can cause ClassLoader leaks in application servers if the buffer references classes from a webapp</li>
     * </ul>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * StreamingRingBuffer buffer = new StreamingRingBuffer(1024, 256, 128);
     * try {
     *     // Use buffer for processing
     *     double[] window = buffer.getWindowDirect();
     *     // ... process window ...
     * } finally {
     *     buffer.cleanupThread(); // Clean up ThreadLocal storage
     * }
     * }</pre>
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
    
    /**
     * Helper method for concurrent reader detection.
     * Only active when assertions are enabled (-ea flag).
     * 
     * @return true if this thread can proceed as reader
     */
    private boolean enterReader() {
        Thread current = Thread.currentThread();
        
        // Try to atomically set ourselves as the reader
        if (currentReader.compareAndSet(null, current)) {
            return true; // We acquired reader status
        }
        
        // Someone else might be reading, check if it's us or if they're still alive
        Thread existing = currentReader.get();
        if (existing == current) {
            return true; // Re-entrant call from same thread
        }
        
        if (existing != null && existing.isAlive()) {
            return false; // Another thread is actively reading
        }
        
        // The previous reader thread is dead, try to take over
        return currentReader.compareAndSet(existing, current);
    }
    
    /**
     * Helper method to exit reader state.
     * 
     * @return always true (for assert statement)
     */
    private boolean exitReader() {
        // Only clear if we are the current reader
        currentReader.compareAndSet(Thread.currentThread(), null);
        return true;
    }
}