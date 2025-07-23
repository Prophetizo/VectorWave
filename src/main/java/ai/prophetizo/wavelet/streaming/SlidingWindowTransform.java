package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sliding window wavelet transform for continuous spectral analysis.
 * 
 * <p>This implementation provides overlapping window analysis, which is useful for:</p>
 * <ul>
 *   <li>Time-frequency analysis with better time resolution</li>
 *   <li>Smooth spectral evolution tracking</li>
 *   <li>Feature detection in continuous signals</li>
 * </ul>
 * 
 * <p>The overlap factor determines the time resolution vs computational cost trade-off.
 * An overlap of 0.5 (50%) is common, while 0.75 (75%) provides smoother results.</p>
 * 
 * @since 1.5.0
 */
public class SlidingWindowTransform extends SubmissionPublisher<TransformResult> 
        implements StreamingWaveletTransform {
    
    private final Wavelet wavelet;
    private final int windowSize;
    private final int hopSize;
    private final WaveletTransform transform;
    
    // Circular buffer for the sliding window
    private final double[] circularBuffer;
    private int bufferHead = 0;
    private int samplesInBuffer = 0;
    private int samplesSinceLastWindow = 0;
    
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    
    /**
     * Creates a sliding window transform with specified overlap.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary mode
     * @param windowSize size of the analysis window (must be power of 2)
     * @param overlapFactor overlap factor between 0 and 1 (e.g., 0.5 for 50% overlap)
     */
    public SlidingWindowTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                int windowSize, double overlapFactor) {
        super();
        
        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        if (windowSize < 16 || (windowSize & (windowSize - 1)) != 0) {
            throw new InvalidArgumentException(
                "Window size must be a power of 2 and at least 16");
        }
        if (overlapFactor < 0 || overlapFactor >= 1) {
            throw new InvalidArgumentException(
                "Overlap factor must be between 0 (inclusive) and 1 (exclusive)");
        }
        
        this.wavelet = wavelet;
        this.windowSize = windowSize;
        this.hopSize = (int) Math.round(windowSize * (1 - overlapFactor));
        
        if (hopSize < 1) {
            throw new InvalidArgumentException("Hop size too small, reduce overlap factor");
        }
        
        // Circular buffer needs to be at least window size
        this.circularBuffer = new double[windowSize * 2];
        
        // Create transform
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(boundaryMode)
            .build();
        this.transform = new WaveletTransform(wavelet, boundaryMode, config);
    }
    
    /**
     * Creates a sliding window transform with 50% overlap.
     */
    public SlidingWindowTransform(Wavelet wavelet, BoundaryMode boundaryMode, int windowSize) {
        this(wavelet, boundaryMode, windowSize, 0.5);
    }
    
    @Override
    public void process(double[] data) {
        if (isClosed.get()) {
            throw new IllegalStateException("Transform is closed");
        }
        
        for (double sample : data) {
            process(sample);
        }
    }
    
    @Override
    public void process(double sample) {
        if (isClosed.get()) {
            throw new IllegalStateException("Transform is closed");
        }
        
        // Add sample to circular buffer
        circularBuffer[bufferHead] = sample;
        bufferHead = (bufferHead + 1) % circularBuffer.length;
        
        if (samplesInBuffer < circularBuffer.length) {
            samplesInBuffer++;
        }
        
        samplesSinceLastWindow++;
        statistics.addSamples(1);
        
        // Check if we have enough samples and it's time for a new window
        if (samplesInBuffer >= windowSize && samplesSinceLastWindow >= hopSize) {
            processWindow();
            samplesSinceLastWindow = 0;
        }
    }
    
    private void processWindow() {
        long startTime = System.nanoTime();
        
        try {
            // Extract window from circular buffer
            double[] window = new double[windowSize];
            int readPos = (bufferHead - samplesInBuffer + circularBuffer.length) % circularBuffer.length;
            
            for (int i = 0; i < windowSize; i++) {
                window[i] = circularBuffer[(readPos + i) % circularBuffer.length];
            }
            
            // Apply window function (optional - could add Hamming, Hanning, etc.)
            // For now, using rectangular window
            
            // Perform transform
            TransformResult result = transform.forward(window);
            
            // Emit the underlying TransformResult
            // Subscribers can check if it's a WindowedTransformResult for timing info
            submit(result);
            
            long processingTime = System.nanoTime() - startTime;
            statistics.recordBlockProcessed(processingTime);
            
        } catch (Exception e) {
            closeExceptionally(e);
        }
    }
    
    @Override
    public void flush() {
        if (isClosed.get()) {
            return;
        }
        
        // Process final window if we have enough samples
        if (samplesInBuffer >= windowSize) {
            processWindow();
        }
    }
    
    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            flush();
            super.close();
        }
    }
    
    @Override
    public int getBlockSize() {
        return windowSize;
    }
    
    @Override
    public int getBufferLevel() {
        return samplesInBuffer;
    }
    
    @Override
    public boolean isReady() {
        return !isClosed.get();
    }
    
    @Override
    public StreamingStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Extended transform result with window timing information.
     * This wraps a TransformResult and adds timing metadata.
     */
    public static class WindowedTransformResult {
        private final TransformResult result;
        private final long startSample;
        private final int windowSize;
        private final int hopSize;
        
        public WindowedTransformResult(TransformResult result, long startSample, 
                                     int windowSize, int hopSize) {
            this.result = result;
            this.startSample = startSample;
            this.windowSize = windowSize;
            this.hopSize = hopSize;
        }
        
        /**
         * Get the underlying transform result.
         */
        public TransformResult getTransformResult() {
            return result;
        }
        
        /**
         * Get approximation coefficients from the underlying result.
         */
        public double[] approximationCoeffs() {
            return result.approximationCoeffs();
        }
        
        /**
         * Get detail coefficients from the underlying result.
         */
        public double[] detailCoeffs() {
            return result.detailCoeffs();
        }
        
        /**
         * Get the starting sample index of this window.
         */
        public long getStartSample() {
            return startSample;
        }
        
        /**
         * Get the window size.
         */
        public int getWindowSize() {
            return windowSize;
        }
        
        /**
         * Get the hop size between windows.
         */
        public int getHopSize() {
            return hopSize;
        }
        
        /**
         * Convert sample index to time given a sample rate.
         */
        public double getStartTime(double sampleRate) {
            return startSample / sampleRate;
        }
    }
    
    /**
     * Statistics implementation.
     */
    private static class StreamingStatisticsImpl implements StreamingStatistics {
        private final long startTime = System.nanoTime();
        private long samplesProcessed = 0;
        private long blocksEmitted = 0;
        private long totalProcessingTime = 0;
        private long maxProcessingTime = 0;
        
        synchronized void addSamples(long count) {
            samplesProcessed += count;
        }
        
        synchronized void recordBlockProcessed(long processingTime) {
            blocksEmitted++;
            totalProcessingTime += processingTime;
            maxProcessingTime = Math.max(maxProcessingTime, processingTime);
        }
        
        @Override
        public synchronized long getSamplesProcessed() {
            return samplesProcessed;
        }
        
        @Override
        public synchronized long getBlocksEmitted() {
            return blocksEmitted;
        }
        
        @Override
        public synchronized double getAverageProcessingTime() {
            return blocksEmitted > 0 ? (double) totalProcessingTime / blocksEmitted : 0.0;
        }
        
        @Override
        public synchronized long getMaxProcessingTime() {
            return maxProcessingTime;
        }
        
        @Override
        public synchronized long getOverruns() {
            return 0; // Sliding window doesn't have overruns
        }
        
        @Override
        public synchronized double getThroughput() {
            double elapsedSeconds = (System.nanoTime() - startTime) / 1e9;
            return elapsedSeconds > 0 ? samplesProcessed / elapsedSeconds : 0.0;
        }
    }
}