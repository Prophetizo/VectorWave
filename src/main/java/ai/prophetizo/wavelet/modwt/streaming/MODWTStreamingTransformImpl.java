package ai.prophetizo.wavelet.modwt.streaming;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Implementation of streaming MODWT transform using circular buffering.
 *
 * <p>This implementation uses a circular buffer strategy that takes advantage
 * of MODWT's shift-invariant properties for seamless block processing.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Circular buffer for continuous processing</li>
 *   <li>Overlap handling for filter continuity</li>
 *   <li>Thread-safe operation</li>
 *   <li>Flexible buffer size (not limited to powers of 2)</li>
 * </ul>
 *
 * @since 3.0.0
 */
class MODWTStreamingTransformImpl extends SubmissionPublisher<MODWTResult>
        implements MODWTStreamingTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int bufferSize;
    private final MODWTTransform transform;
    
    // Circular buffer for streaming
    private final double[] circularBuffer;
    private int writePosition = 0;
    private int samplesInBuffer = 0;
    
    // Overlap handling for filter continuity
    private final int filterLength;
    private final int overlapSize;
    private double[] previousOverlap;
    
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();

    /**
     * Creates a new streaming MODWT transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param bufferSize   the buffer size (must be positive)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public MODWTStreamingTransformImpl(Wavelet wavelet, BoundaryMode boundaryMode, int bufferSize) {
        super();

        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        if (bufferSize <= 0) {
            throw new InvalidArgumentException("Buffer size must be positive, got: " + bufferSize);
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.bufferSize = bufferSize;
        
        // Get filter length for overlap calculation
        this.filterLength = wavelet.lowPassDecomposition().length;
        this.overlapSize = filterLength - 1;
        this.previousOverlap = new double[overlapSize];
        
        // Initialize circular buffer with extra space for overlap
        this.circularBuffer = new double[bufferSize + overlapSize];
        
        // Create MODWT transform
        this.transform = new MODWTTransform(wavelet, boundaryMode);
    }

    @Override
    public synchronized void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        if (data == null || data.length == 0) {
            throw new InvalidSignalException("Data cannot be null or empty");
        }

        // Process data sample by sample
        for (double sample : data) {
            processSampleInternal(sample);
        }
    }

    @Override
    public synchronized void processSample(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        processSampleInternal(sample);
    }

    private void processSampleInternal(double sample) {
        // Add sample to circular buffer
        circularBuffer[writePosition] = sample;
        writePosition = (writePosition + 1) % circularBuffer.length;
        samplesInBuffer++;
        
        // Check if we have enough samples to process
        if (samplesInBuffer >= bufferSize) {
            processBuffer();
        }
        
        statistics.incrementSamplesProcessed();
    }

    private void processBuffer() {
        long startTime = System.nanoTime();
        
        // Extract buffer contents in correct order
        double[] processingBuffer = new double[bufferSize];
        int readPos = (writePosition - bufferSize + circularBuffer.length) % circularBuffer.length;
        
        for (int i = 0; i < bufferSize; i++) {
            processingBuffer[i] = circularBuffer[(readPos + i) % circularBuffer.length];
        }
        
        // Apply MODWT transform
        MODWTResult result = transform.forward(processingBuffer);
        
        // Publish result to subscribers
        submit(result);
        
        // Update statistics
        long processingTime = System.nanoTime() - startTime;
        statistics.recordBlockProcessed(processingTime);
        
        // Reset buffer counter (keep overlap samples)
        samplesInBuffer = overlapSize;
        
        // Copy last samples for overlap
        for (int i = 0; i < overlapSize; i++) {
            int pos = (writePosition - overlapSize + i + circularBuffer.length) % circularBuffer.length;
            previousOverlap[i] = circularBuffer[pos];
        }
    }

    @Override
    public synchronized void flush() {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        if (samplesInBuffer > 0) {
            // Process remaining samples with zero padding if needed
            double[] finalBuffer = new double[bufferSize];
            int readPos = (writePosition - samplesInBuffer + circularBuffer.length) % circularBuffer.length;
            
            for (int i = 0; i < samplesInBuffer; i++) {
                finalBuffer[i] = circularBuffer[(readPos + i) % circularBuffer.length];
            }
            
            // Zero pad the rest
            Arrays.fill(finalBuffer, samplesInBuffer, bufferSize, 0.0);
            
            // Apply MODWT transform
            MODWTResult result = transform.forward(finalBuffer);
            submit(result);
            
            // Reset buffer
            samplesInBuffer = 0;
            writePosition = 0;
        }
    }

    @Override
    public StreamingStatistics getStatistics() {
        return statistics;
    }

    @Override
    public synchronized void reset() {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        // Clear buffers
        Arrays.fill(circularBuffer, 0.0);
        Arrays.fill(previousOverlap, 0.0);
        writePosition = 0;
        samplesInBuffer = 0;
        
        // Reset statistics
        statistics.reset();
    }

    @Override
    public int getBufferLevel() {
        return samplesInBuffer;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public void close() {
        if (!isClosed.get()) {
            // Flush any remaining data before marking as closed
            flush();
            
            // Now mark as closed
            isClosed.set(true);
            
            // Close the publisher
            super.close();
        }
    }

    /**
     * Implementation of streaming statistics.
     */
    private static class StreamingStatisticsImpl implements StreamingStatistics {
        private final AtomicLong samplesProcessed = new AtomicLong();
        private final AtomicLong blocksProcessed = new AtomicLong();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final AtomicLong maxProcessingTime = new AtomicLong();
        private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong startTime = new AtomicLong(System.nanoTime());

        void incrementSamplesProcessed() {
            samplesProcessed.incrementAndGet();
        }

        void recordBlockProcessed(long processingTimeNanos) {
            blocksProcessed.incrementAndGet();
            totalProcessingTime.add(processingTimeNanos);
            
            // Update max
            long currentMax = maxProcessingTime.get();
            while (processingTimeNanos > currentMax) {
                if (maxProcessingTime.compareAndSet(currentMax, processingTimeNanos)) {
                    break;
                }
                currentMax = maxProcessingTime.get();
            }
            
            // Update min
            long currentMin = minProcessingTime.get();
            while (processingTimeNanos < currentMin) {
                if (minProcessingTime.compareAndSet(currentMin, processingTimeNanos)) {
                    break;
                }
                currentMin = minProcessingTime.get();
            }
        }

        @Override
        public long getSamplesProcessed() {
            return samplesProcessed.get();
        }

        @Override
        public long getBlocksProcessed() {
            return blocksProcessed.get();
        }

        @Override
        public long getAverageProcessingTimeNanos() {
            long blocks = blocksProcessed.get();
            return blocks > 0 ? totalProcessingTime.sum() / blocks : 0;
        }

        @Override
        public long getMaxProcessingTimeNanos() {
            return maxProcessingTime.get();
        }

        @Override
        public long getMinProcessingTimeNanos() {
            long min = minProcessingTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        @Override
        public double getThroughputSamplesPerSecond() {
            long elapsedNanos = System.nanoTime() - startTime.get();
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            return elapsedSeconds > 0 ? samplesProcessed.get() / elapsedSeconds : 0;
        }

        @Override
        public void reset() {
            samplesProcessed.set(0);
            blocksProcessed.set(0);
            totalProcessingTime.reset();
            maxProcessingTime.set(0);
            minProcessingTime.set(Long.MAX_VALUE);
            startTime.set(System.nanoTime());
        }
    }
}