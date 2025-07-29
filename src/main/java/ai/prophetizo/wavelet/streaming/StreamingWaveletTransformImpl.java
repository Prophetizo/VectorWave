package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

/**
 * Implementation of StreamingWaveletTransform that processes continuous data streams
 * using overlapping windows and ring buffer optimization.
 * 
 * <p>This class eliminates the performance overhead of array copying by using:</p>
 * <ul>
 *   <li>Ring buffer for efficient sample management</li>
 *   <li>Zero-copy window extraction where possible</li>
 *   <li>Overlapping windows to maintain temporal continuity</li>
 *   <li>Configurable overlap ratios for different analysis needs</li>
 * </ul>
 * 
 * <p>The implementation is optimized for scenarios where:</p>
 * <ul>
 *   <li>Data arrives continuously in small batches</li>
 *   <li>Transform results are needed with minimal latency</li>
 *   <li>Memory bandwidth is a limiting factor</li>
 *   <li>Cache efficiency is important for performance</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External
 * synchronization is required for concurrent access.</p>
 */
public final class StreamingWaveletTransformImpl implements StreamingWaveletTransform {
    
    private final WaveletTransform transform;
    private final OverlapBuffer overlapBuffer;
    private final int windowSize;
    private final int overlapSize;
    private final int hopSize;
    
    // Reusable array for window extraction to minimize allocations
    private final double[] windowArray;
    
    /**
     * Creates a new streaming wavelet transform.
     * 
     * @param wavelet the wavelet to use for transforms
     * @param boundaryMode the boundary handling mode
     * @param windowSize the size of each transform window (must be power of 2)
     * @param overlapSize the number of samples to overlap between windows
     * @throws IllegalArgumentException if parameters are invalid
     */
    public StreamingWaveletTransformImpl(Wavelet wavelet, BoundaryMode boundaryMode, 
                                       int windowSize, int overlapSize) {
        // Validate window size is power of 2
        if (windowSize <= 0 || (windowSize & (windowSize - 1)) != 0) {
            throw new IllegalArgumentException(
                "Window size must be a positive power of 2: " + windowSize);
        }
        
        if (overlapSize < 0 || overlapSize >= windowSize) {
            throw new IllegalArgumentException(
                "Overlap size must be >= 0 and < window size: " + overlapSize);
        }
        
        this.transform = new WaveletTransform(wavelet, boundaryMode);
        this.windowSize = windowSize;
        this.overlapSize = overlapSize;
        this.hopSize = windowSize - overlapSize;
        this.overlapBuffer = new OverlapBuffer(windowSize, overlapSize);
        this.windowArray = new double[windowSize];
    }
    
    @Override
    public void addSamples(double[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("Samples array cannot be null");
        }
        
        overlapBuffer.addSamples(samples);
    }
    
    @Override
    public boolean addSample(double sample) {
        return overlapBuffer.addSample(sample);
    }
    
    @Override
    public boolean isResultReady() {
        return overlapBuffer.isWindowReady();
    }
    
    @Override
    public TransformResult getNextResult() {
        if (!isResultReady()) {
            throw new IllegalStateException("No result ready. Call isResultReady() first.");
        }
        
        // Get the current window with zero-copy optimization where possible
        double[] window = extractCurrentWindow();
        
        // Perform the wavelet transform
        TransformResult result = transform.forward(window);
        
        // Advance to the next overlapping window
        overlapBuffer.advance();
        
        return result;
    }
    
    /**
     * Extracts the current window from the overlap buffer.
     * Uses zero-copy access when possible to minimize memory bandwidth.
     * 
     * @return the current window data
     */
    private double[] extractCurrentWindow() {
        try {
            // Try zero-copy access first
            DoubleRingBuffer.DoubleBufferSegment segment = overlapBuffer.getCurrentWindowSegment();
            if (segment.isValid()) {
                segment.copyTo(windowArray, 0);
                return windowArray;
            }
        } catch (Exception e) {
            // Fall through to backup method
        }
        
        // Fallback: use standard window extraction
        return overlapBuffer.getCurrentWindow();
    }
    
    @Override
    public int getWindowSize() {
        return windowSize;
    }
    
    @Override
    public int getOverlapSize() {
        return overlapSize;
    }
    
    @Override
    public int getHopSize() {
        return hopSize;
    }
    
    @Override
    public int getBufferedSampleCount() {
        return overlapBuffer.size();
    }
    
    @Override
    public void reset() {
        overlapBuffer.clear();
    }
    
    @Override
    public boolean isEmpty() {
        return overlapBuffer.isEmpty();
    }
    
    /**
     * Gets the underlying wavelet transform instance.
     * Useful for accessing wavelet properties and configuration.
     * 
     * @return the wavelet transform used internally
     */
    public WaveletTransform getWaveletTransform() {
        return transform;
    }
    
    /**
     * Gets performance statistics for monitoring and optimization.
     * 
     * @return performance statistics object
     */
    public StreamingPerformanceStats getPerformanceStats() {
        return new StreamingPerformanceStats(
            overlapBuffer.size(),
            windowSize,
            overlapSize,
            hopSize
        );
    }
    
    /**
     * Performance statistics for streaming wavelet transforms.
     */
    public static final class StreamingPerformanceStats {
        private final int bufferedSamples;
        private final int windowSize;
        private final int overlapSize;
        private final int hopSize;
        
        StreamingPerformanceStats(int bufferedSamples, int windowSize, int overlapSize, int hopSize) {
            this.bufferedSamples = bufferedSamples;
            this.windowSize = windowSize;
            this.overlapSize = overlapSize;
            this.hopSize = hopSize;
        }
        
        public int getBufferedSamples() { return bufferedSamples; }
        public int getWindowSize() { return windowSize; }
        public int getOverlapSize() { return overlapSize; }
        public int getHopSize() { return hopSize; }
        public double getOverlapRatio() { return (double) overlapSize / windowSize; }
        public double getMemoryEfficiency() { return (double) hopSize / windowSize; }
        
        @Override
        public String toString() {
            return String.format(
                "StreamingStats{buffered=%d, window=%d, overlap=%d (%.1f%%), hop=%d, efficiency=%.1f%%}",
                bufferedSamples, windowSize, overlapSize, getOverlapRatio() * 100,
                hopSize, getMemoryEfficiency() * 100);
        }
    }
}