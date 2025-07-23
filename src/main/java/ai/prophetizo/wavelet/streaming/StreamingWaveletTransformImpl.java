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

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Implementation of streaming wavelet transform using buffering.
 *
 * <p>This implementation uses a simple buffering strategy to collect samples
 * until a full block is available, then processes it synchronously.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Simple buffering for reliability</li>
 *   <li>Synchronous processing to avoid timing issues</li>
 *   <li>Thread-safe operation</li>
 *   <li>Configurable block size for latency/throughput trade-off</li>
 * </ul>
 */
class StreamingWaveletTransformImpl extends SubmissionPublisher<TransformResult>
        implements StreamingWaveletTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int blockSize;
    private final WaveletTransform transform;

    // Single buffer for simplicity
    private final double[] buffer;
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    private int bufferPosition = 0;

    /**
     * Creates a new streaming wavelet transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size (must be power of 2)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public StreamingWaveletTransformImpl(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize) {
        super();

        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        if (!ValidationUtils.isPowerOfTwo(blockSize)) {
            throw new InvalidArgumentException("Block size must be a power of 2, got: " + blockSize);
        }
        if (blockSize < 16) {
            throw new InvalidArgumentException("Block size must be at least 16, got: " + blockSize);
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.blockSize = blockSize;

        // Initialize buffer
        this.buffer = new double[blockSize];

        // Create transform with optimized config
        TransformConfig config = TransformConfig.builder()
                .boundaryMode(boundaryMode)
                .build();
        this.transform = new WaveletTransform(wavelet, boundaryMode, config);
    }

    @Override
    public synchronized void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        if (data == null || data.length == 0) {
            throw new InvalidSignalException("Data cannot be null or empty");
        }

        int dataIndex = 0;
        while (dataIndex < data.length && !isClosed.get()) {
            // Calculate how much data can fit in current buffer
            int spaceAvailable = blockSize - bufferPosition;
            int toCopy = Math.min(spaceAvailable, data.length - dataIndex);

            // Copy data to buffer
            System.arraycopy(data, dataIndex, buffer, bufferPosition, toCopy);
            bufferPosition += toCopy;
            dataIndex += toCopy;

            // If buffer is full, process it
            if (bufferPosition >= blockSize) {
                processBlock();
                bufferPosition = 0;
            }
        }

        statistics.addSamples(data.length);
    }

    @Override
    public synchronized void process(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }

        buffer[bufferPosition++] = sample;

        if (bufferPosition >= blockSize) {
            processBlock();
            bufferPosition = 0;
        }

        statistics.addSamples(1);
    }

    private void processBlock() {
        if (isClosed.get()) {
            return;
        }

        long startTime = System.nanoTime();

        try {
            // Create a copy of the buffer data
            double[] blockData = Arrays.copyOf(buffer, blockSize);

            // Perform wavelet transform
            TransformResult result = transform.forward(blockData);

            // Emit result to subscribers
            submit(result);

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

        // Process any remaining samples
        if (bufferPosition > 0) {
            // Pad the remaining buffer with zeros
            Arrays.fill(buffer, bufferPosition, blockSize, 0.0);
            processBlock();
            bufferPosition = 0;
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
        return bufferPosition;
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