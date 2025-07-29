package ai.prophetizo.wavelet.streaming;

/**
 * A specialized buffer for managing overlapping windows in streaming wavelet transforms.
 * 
 * <p>This class handles the common pattern in streaming signal processing where consecutive
 * processing windows need to overlap to maintain continuity. Instead of shifting array
 * contents (which is expensive), it uses a ring buffer underneath to efficiently manage
 * the overlapping segments.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li><strong>Zero-copy overlap management</strong>: No array copying for overlaps</li>
 *   <li><strong>Configurable overlap</strong>: Support for arbitrary overlap sizes</li>
 *   <li><strong>Window-based access</strong>: Direct access to current processing window</li>
 *   <li><strong>Streaming friendly</strong>: Optimized for continuous data ingestion</li>
 * </ul>
 * 
 * <p>Example usage for 50% overlapping windows:</p>
 * <pre>{@code
 * OverlapBuffer buffer = new OverlapBuffer(1024, 512); // 1024 window, 512 overlap
 * 
 * // Add new data
 * buffer.addSamples(newData);
 * 
 * if (buffer.isWindowReady()) {
 *     double[] window = buffer.getCurrentWindow();
 *     // Process the window...
 *     buffer.advance(); // Move to next overlapping window
 * }
 * }</pre>
 */
public final class OverlapBuffer {
    
    private final DoubleRingBuffer ringBuffer;
    private final int windowSize;
    private final int overlapSize;
    private final int hopSize; // windowSize - overlapSize
    private final double[] windowBuffer; // Reused buffer for window extraction
    
    private int samplesInCurrentWindow = 0;
    private boolean windowReady = false;
    
    /**
     * Creates a new OverlapBuffer with the specified window and overlap sizes.
     * 
     * @param windowSize the size of each processing window
     * @param overlapSize the number of samples that overlap between consecutive windows
     * @throws IllegalArgumentException if windowSize <= 0, overlapSize < 0, or overlapSize >= windowSize
     */
    public OverlapBuffer(int windowSize, int overlapSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive: " + windowSize);
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("Overlap size cannot be negative: " + overlapSize);
        }
        if (overlapSize >= windowSize) {
            throw new IllegalArgumentException(
                "Overlap size must be less than window size: " + overlapSize + " >= " + windowSize);
        }
        
        this.windowSize = windowSize;
        this.overlapSize = overlapSize;
        this.hopSize = windowSize - overlapSize;
        this.windowBuffer = new double[windowSize];
        
        // Buffer capacity needs to hold at least two windows to handle overlap efficiently
        int bufferCapacity = findNextPowerOfTwo(windowSize * 2);
        this.ringBuffer = new DoubleRingBuffer(bufferCapacity);
    }
    
    /**
     * Adds new samples to the buffer.
     * 
     * @param samples the samples to add
     * @throws IllegalArgumentException if samples is null
     */
    public void addSamples(double[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("Samples array cannot be null");
        }
        
        for (double sample : samples) {
            addSample(sample);
        }
    }
    
    /**
     * Adds a single sample to the buffer.
     * 
     * @param sample the sample to add
     * @return true if adding this sample makes a window ready for processing
     */
    public boolean addSample(double sample) {
        if (!ringBuffer.offer(sample)) {
            // Ring buffer is full, need to make space
            // This shouldn't happen with proper buffer sizing, but handle gracefully
            ringBuffer.poll(); // Remove oldest sample
            if (!ringBuffer.offer(sample)) {
                throw new IllegalStateException("Failed to add sample even after making space");
            }
        }
        
        samplesInCurrentWindow++;
        
        if (samplesInCurrentWindow >= windowSize) {
            windowReady = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a complete window is ready for processing.
     * 
     * @return true if a window of the specified size is available
     */
    public boolean isWindowReady() {
        return windowReady && ringBuffer.size() >= windowSize;
    }
    
    /**
     * Gets the current processing window.
     * This method provides zero-copy access when possible, but may copy data
     * if the window wraps around the ring buffer boundary.
     * 
     * @return a copy of the current window data
     * @throws IllegalStateException if no window is ready
     */
    public double[] getCurrentWindow() {
        if (!isWindowReady()) {
            throw new IllegalStateException("No window ready for processing");
        }
        
        // Try to get the window as a single segment for zero-copy access
        try {
            DoubleRingBuffer.DoubleBufferSegment segment = ringBuffer.getSegment(0, windowSize);
            segment.copyTo(windowBuffer, 0);
            return windowBuffer.clone(); // Return defensive copy
        } catch (Exception e) {
            // Fallback: extract samples one by one
            for (int i = 0; i < windowSize; i++) {
                windowBuffer[i] = ringBuffer.peek(i);
            }
            return windowBuffer.clone(); // Return defensive copy
        }
    }
    
    /**
     * Gets direct access to the current window segment for zero-copy processing.
     * The returned segment is only valid until the next modification to the buffer.
     * 
     * @return a BufferSegment providing direct access to the window data
     * @throws IllegalStateException if no window is ready
     */
    public DoubleRingBuffer.DoubleBufferSegment getCurrentWindowSegment() {
        if (!isWindowReady()) {
            throw new IllegalStateException("No window ready for processing");
        }
        
        return ringBuffer.getSegment(0, windowSize);
    }
    
    /**
     * Advances to the next overlapping window by discarding samples according to the hop size.
     * This prepares the buffer for the next processing window.
     * 
     * @throws IllegalStateException if no window is ready
     */
    public void advance() {
        if (!isWindowReady()) {
            throw new IllegalStateException("Cannot advance: no window ready");
        }
        
        // Remove hopSize samples from the front of the buffer
        for (int i = 0; i < hopSize; i++) {
            double discarded = ringBuffer.poll();
            if (Double.isNaN(discarded)) {
                throw new IllegalStateException("Unexpected empty buffer during advance");
            }
        }
        
        // Update state
        samplesInCurrentWindow -= hopSize;
        windowReady = (samplesInCurrentWindow >= windowSize);
    }
    
    /**
     * Gets the window size.
     * 
     * @return the size of each processing window
     */
    public int getWindowSize() {
        return windowSize;
    }
    
    /**
     * Gets the overlap size.
     * 
     * @return the number of samples that overlap between consecutive windows
     */
    public int getOverlapSize() {
        return overlapSize;
    }
    
    /**
     * Gets the hop size (advance distance between windows).
     * 
     * @return the number of samples to advance between windows
     */
    public int getHopSize() {
        return hopSize;
    }
    
    /**
     * Gets the number of samples currently in the buffer.
     * 
     * @return the current buffer size
     */
    public int size() {
        return ringBuffer.size();
    }
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if the buffer contains no samples
     */
    public boolean isEmpty() {
        return ringBuffer.isEmpty();
    }
    
    /**
     * Clears all samples from the buffer.
     */
    public void clear() {
        ringBuffer.clear();
        samplesInCurrentWindow = 0;
        windowReady = false;
    }
    
    /**
     * Finds the next power of two greater than or equal to the given value.
     * Used for optimal ring buffer sizing.
     * 
     * @param value the input value
     * @return the next power of two >= value
     */
    private static int findNextPowerOfTwo(int value) {
        if (value <= 0) {
            return 1;
        }
        
        // Handle the case where value is already a power of 2
        if ((value & (value - 1)) == 0) {
            return value;
        }
        
        // Find the next power of 2
        int power = 1;
        while (power < value) {
            power <<= 1;
            if (power <= 0) {
                // Overflow protection
                throw new IllegalArgumentException("Value too large: " + value);
            }
        }
        
        return power;
    }
}