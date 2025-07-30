package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Optimized streaming wavelet transform using ring buffer for reduced memory allocations.
 *
 * <p>This implementation uses a ring buffer to minimize memory allocations and improve
 * cache locality. While the ring buffer provides zero-copy window extraction, a single
 * copy is still required for the wavelet transform operation due to API constraints.</p>
 *
 * <p>Key improvements over StreamingWaveletTransformImpl:</p>
 * <ul>
 *   <li>Ring buffer eliminates repeated allocations</li>
 *   <li>Single copy operation per block (vs multiple in naive implementation)</li>
 *   <li>Better cache locality</li>
 *   <li>Lower GC pressure</li>
 * </ul>
 *
 * <p>Note: True zero-copy operation would require modifying WaveletTransform to accept
 * array slices, which would impact the entire codebase.</p>
 */
public class OptimizedStreamingWaveletTransform extends SubmissionPublisher<TransformResult>
        implements StreamingWaveletTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int blockSize;
    private final WaveletTransform transform;
    
    // Ring buffer for zero-copy operations
    private final StreamingRingBuffer ringBuffer;
    
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    
    // Processing buffer (reused to avoid allocation)
    private final double[] processingBuffer;

    /**
     * Creates an optimized streaming wavelet transform with no overlap.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size (must be power of 2)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public OptimizedStreamingWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize) {
        this(wavelet, boundaryMode, blockSize, 0.0);
    }
    
    /**
     * Creates an optimized streaming wavelet transform with configurable overlap.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size (must be power of 2)
     * @param overlapFactor the overlap factor (0.0 = no overlap, 0.5 = 50% overlap, 0.75 = 75% overlap)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public OptimizedStreamingWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize, double overlapFactor) {
        super();

        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        ValidationUtils.validateBlockSizeForWavelet(blockSize, "OptimizedStreamingWaveletTransform");
        if (blockSize < 16) {
            throw new InvalidArgumentException("Block size must be at least 16, got: " + blockSize);
        }
        if (overlapFactor < 0.0 || overlapFactor >= 1.0) {
            throw new InvalidArgumentException("Overlap factor must be in range [0.0, 1.0), got: " + overlapFactor);
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.blockSize = blockSize;
        
        // Calculate hop size based on overlap factor
        // overlap = 0.0 -> hopSize = blockSize (no overlap)
        // overlap = 0.5 -> hopSize = blockSize/2 (50% overlap)
        // overlap = 0.75 -> hopSize = blockSize/4 (75% overlap)
        int hopSize = (int) Math.round(blockSize * (1.0 - overlapFactor));
        if (hopSize < 1) {
            hopSize = 1;
        }
        
        // Create ring buffer with 4x block size for smooth operation
        this.ringBuffer = new StreamingRingBuffer(blockSize * 4, blockSize, hopSize);
        
        // Pre-allocate processing buffer
        this.processingBuffer = new double[blockSize];

        // Create transform with optimized config
        TransformConfig config = TransformConfig.builder()
                .boundaryMode(boundaryMode)
                .build();
        this.transform = new WaveletTransform(wavelet, boundaryMode, config);
    }

    @Override
    public void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        if (data == null || data.length == 0) {
            throw new InvalidSignalException("Data cannot be null or empty");
        }

        // Write data to ring buffer
        int offset = 0;
        while (offset < data.length && !isClosed.get()) {
            int written = ringBuffer.write(data, offset, data.length - offset);
            offset += written;
            
            // Process any available windows
            processAvailableWindows();
            
            // If no data was written and buffer is full, we need to process
            if (written == 0 && ringBuffer.isFull()) {
                // Force processing of one window to make space
                if (ringBuffer.hasWindow()) {
                    processOneWindow();
                }
            }
        }

        statistics.addSamples(data.length);
    }

    @Override
    public void process(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }

        // Try to write to ring buffer
        while (!ringBuffer.write(sample) && !isClosed.get()) {
            // Buffer is full, process one window to make space
            if (ringBuffer.hasWindow()) {
                processOneWindow();
            } else {
                // This shouldn't happen with proper buffer sizing
                throw new InvalidStateException("Ring buffer full but no window available");
            }
        }
        
        // Process any available windows
        processAvailableWindows();
        
        statistics.addSamples(1);
    }
    
    private void processAvailableWindows() {
        while (ringBuffer.hasWindow() && !isClosed.get()) {
            processOneWindow();
        }
    }
    
    private void processOneWindow() {
        if (isClosed.get()) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Get window data directly from ring buffer (zero-copy from buffer)
            double[] windowData = ringBuffer.getWindowDirect();
            if (windowData == null) {
                return;
            }
            
            // Copy to processing buffer (required because WaveletTransform doesn't support array slices)
            // TODO: Future optimization - modify WaveletTransform to accept (array, offset, length)
            System.arraycopy(windowData, 0, processingBuffer, 0, blockSize);
            
            // Perform wavelet transform
            TransformResult result = transform.forward(processingBuffer);
            
            // Emit result to subscribers
            submit(result);
            
            // Advance window
            ringBuffer.advanceWindow();
            
            long processingTime = System.nanoTime() - startTime;
            statistics.recordBlockProcessed(processingTime);
            
        } catch (Exception e) {
            // Notify subscribers of error
            closeExceptionally(e);
        }
    }

    @Override
    public synchronized void flush() {
        if (isClosed.get()) {
            return;
        }

        // Process any remaining complete windows
        processAvailableWindows();
        
        // Handle partial window if needed
        int remaining = ringBuffer.available();
        if (remaining > 0 && remaining < blockSize) {
            // Read remaining data
            double[] partialData = new double[blockSize];
            int read = ringBuffer.read(partialData, 0, remaining);
            
            // Zero-pad the rest
            for (int i = read; i < blockSize; i++) {
                partialData[i] = 0.0;
            }
            
            long startTime = System.nanoTime();
            
            try {
                // Process partial block
                TransformResult result = transform.forward(partialData);
                submit(result);
                
                long processingTime = System.nanoTime() - startTime;
                statistics.recordBlockProcessed(processingTime);
            } catch (Exception e) {
                closeExceptionally(e);
            }
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
        return blockSize;
    }

    @Override
    public synchronized int getBufferLevel() {
        return ringBuffer.available();
    }

    @Override
    public boolean isReady() {
        return !isClosed.get() && !ringBuffer.isFull();
    }

    @Override
    public StreamingStatistics getStatistics() {
        return statistics;
    }

    /**
     * Internal statistics implementation.
     */
    private static class StreamingStatisticsImpl implements StreamingStatistics {
        private final AtomicLong samplesProcessed = new AtomicLong();
        private final AtomicLong blocksEmitted = new AtomicLong();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final AtomicLong maxProcessingTime = new AtomicLong();
        private final AtomicLong overruns = new AtomicLong();
        private final long startTime = System.nanoTime();

        void addSamples(long count) {
            samplesProcessed.addAndGet(count);
        }

        void recordBlockProcessed(long processingTime) {
            blocksEmitted.incrementAndGet();
            totalProcessingTime.add(processingTime);

            // Update max processing time
            long currentMax;
            do {
                currentMax = maxProcessingTime.get();
            } while (processingTime > currentMax &&
                    !maxProcessingTime.compareAndSet(currentMax, processingTime));
        }

        @Override
        public long getSamplesProcessed() {
            return samplesProcessed.get();
        }

        @Override
        public long getBlocksEmitted() {
            return blocksEmitted.get();
        }

        @Override
        public double getAverageProcessingTime() {
            long blocks = blocksEmitted.get();
            return blocks > 0 ? totalProcessingTime.sum() / (double) blocks : 0.0;
        }

        @Override
        public long getMaxProcessingTime() {
            return maxProcessingTime.get();
        }

        @Override
        public long getOverruns() {
            return overruns.get();
        }

        @Override
        public double getThroughput() {
            double elapsedSeconds = (System.nanoTime() - startTime) / 1e9;
            return elapsedSeconds > 0 ? samplesProcessed.get() / elapsedSeconds : 0.0;
        }
    }
}