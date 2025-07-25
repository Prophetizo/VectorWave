package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.MultiLevelTransformResult;
import ai.prophetizo.wavelet.MultiLevelWaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multi-level streaming wavelet transform implementation.
 *
 * <p>This implementation performs multi-level decomposition on streaming data,
 * emitting {@link MultiLevelTransformResult} objects containing coefficients
 * at all decomposition levels.</p>
 *
 * <p>The implementation uses a cascade of buffers, one for each decomposition
 * level, to handle the different sampling rates at each level.</p>
 */
class MultiLevelStreamingTransform extends SubmissionPublisher<TransformResult>
        implements StreamingWaveletTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int blockSize;
    private final int levels;
    private final MultiLevelWaveletTransform transform;

    // Cascaded buffers for each level
    private final double[][] levelBuffers;
    private final int[] bufferPositions;
    private final int[] levelBlockSizes;

    // Main input buffer
    private final double[] inputBuffer;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    private final Object processLock = new Object();
    private int inputPosition = 0;

    /**
     * Creates a multi-level streaming transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size at the finest level
     * @param levels       number of decomposition levels
     */
    public MultiLevelStreamingTransform(Wavelet wavelet, BoundaryMode boundaryMode,
                                        int blockSize, int levels) {
        super();

        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        ValidationUtils.validateBlockSizeForWavelet(blockSize, "MultiLevelStreamingTransform");
        if (levels < 1) {
            throw new InvalidArgumentException("Levels must be at least 1");
        }

        // Verify block size can support requested levels
        int minSize = 1 << levels;
        if (blockSize < minSize) {
            throw new InvalidArgumentException(
                    String.format("Block size %d too small for %d levels (minimum %d)",
                            blockSize, levels, minSize));
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.blockSize = blockSize;
        this.levels = levels;

        // Initialize buffers for each level
        this.levelBuffers = new double[levels][];
        this.bufferPositions = new int[levels];
        this.levelBlockSizes = new int[levels];

        int currentSize = blockSize;
        for (int level = 0; level < levels; level++) {
            levelBlockSizes[level] = currentSize;
            levelBuffers[level] = new double[currentSize];
            bufferPositions[level] = 0;
            currentSize /= 2;
        }

        this.inputBuffer = new double[blockSize];

        // Create transform
        TransformConfig config = TransformConfig.builder()
                .boundaryMode(boundaryMode)
                .maxDecompositionLevels(levels)
                .build();
        this.transform = new MultiLevelWaveletTransform(wavelet, boundaryMode, config);
    }

    @Override
    public void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }

        synchronized (processLock) {
            for (double sample : data) {
                processSingleSample(sample);
            }
        }
    }

    @Override
    public void process(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }

        synchronized (processLock) {
            processSingleSample(sample);
        }
    }

    private void processSingleSample(double sample) {
        inputBuffer[inputPosition] = sample;
        inputPosition++;
        statistics.addSamples(1);

        if (inputPosition >= blockSize) {
            processBlock();
            inputPosition = 0;
        }
    }

    private void processBlock() {
        long startTime = System.nanoTime();

        try {
            // Perform multi-level transform
            MultiLevelTransformResult result = transform.decompose(inputBuffer, levels);

            // Emit result as TransformResult
            // For now, emit only the final level coefficients
            submit(TransformResult.create(
                    result.finalApproximation(),
                    result.detailsAtLevel(levels)
            ));

            long processingTime = System.nanoTime() - startTime;
            statistics.recordBlockProcessed(processingTime);

        } catch (Exception e) {
            closeExceptionally(e);
        }
    }

    @Override
    public void flush() {
        synchronized (processLock) {
            if (isClosed.get() || inputPosition == 0) {
                return;
            }

            // Pad remaining data with zeros
            Arrays.fill(inputBuffer, inputPosition, blockSize, 0.0);
            processBlock();
            inputPosition = 0;
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
    public int getBufferLevel() {
        return inputPosition;
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
     * Statistics implementation (reused from StreamingWaveletTransformImpl).
     */
    private static class StreamingStatisticsImpl implements StreamingStatistics {
        private final long startTime = System.nanoTime();
        private final long overruns = 0;
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
            return overruns;
        }

        @Override
        public synchronized double getThroughput() {
            double elapsedSeconds = (System.nanoTime() - startTime) / 1e9;
            return elapsedSeconds > 0 ? samplesProcessed / elapsedSeconds : 0.0;
        }
    }
}
