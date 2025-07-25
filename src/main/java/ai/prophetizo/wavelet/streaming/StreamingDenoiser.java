package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.MultiLevelTransformResult;
import ai.prophetizo.wavelet.MultiLevelWaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.memory.MemoryPool;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streaming wavelet denoiser for real-time signal processing.
 *
 * <p>This implementation provides low-latency denoising suitable for
 * audio processing, financial data cleaning, and sensor data filtering.
 * It uses overlapping blocks with adaptive thresholding for smooth,
 * artifact-free denoising.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Overlap-add processing for smooth transitions</li>
 *   <li>Adaptive noise estimation using MAD</li>
 *   <li>Time-varying threshold adaptation</li>
 *   <li>Multi-level denoising support</li>
 *   <li>Memory-efficient circular buffering</li>
 * </ul>
 *
 * @since 1.6.0
 */
public final class StreamingDenoiser extends SubmissionPublisher<double[]>
        implements AutoCloseable {

    /**
     * Default factor for noise estimator buffer size relative to block size.
     * The noise estimator needs a larger buffer to get accurate statistics.
     */
    private static final int DEFAULT_NOISE_BUFFER_FACTOR = 4;
    
    /**
     * Scale factor for level-dependent threshold adjustment.
     * Higher decomposition levels (coarser details) get scaled thresholds.
     * For level L: threshold * LEVEL_THRESHOLD_SCALE_FACTOR^(L-1)
     */
    private static final double LEVEL_THRESHOLD_SCALE_FACTOR = 1.2;

    // Configuration
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int blockSize;
    private final int levels;
    private final ThresholdMethod thresholdMethod;
    private final ThresholdType thresholdType;
    private final boolean adaptiveThreshold;

    // Processing components
    private final WaveletTransform transform;
    private final NoiseEstimator noiseEstimator;
    private final StreamingThresholdAdapter thresholdAdapter;
    private final OverlapBuffer overlapBuffer;
    // Buffers
    private final double[] inputBuffer;
    // State
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    private final MemoryPool memoryPool;
    private final boolean usingSharedPool;
    private double[] processingBuffer;
    private int inputPosition;

    private StreamingDenoiser(Builder builder) {
        super();

        // Validate block size for wavelet transforms
        ValidationUtils.validateBlockSizeForWavelet(builder.blockSize, "StreamingDenoiser");

        // Initialize configuration fields
        this.wavelet = builder.wavelet;
        this.boundaryMode = BoundaryMode.PERIODIC; // Default for streaming
        this.blockSize = builder.blockSize;
        this.levels = builder.levels;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdType = builder.thresholdType;
        this.adaptiveThreshold = builder.adaptiveThreshold;
        this.inputPosition = 0;

        // Initialize resources using helper method
        ResourceBundle resources = initializeResources(builder);
        
        // Assign successfully initialized resources
        this.transform = resources.transform;
        this.noiseEstimator = resources.noiseEstimator;
        this.thresholdAdapter = resources.thresholdAdapter;
        this.overlapBuffer = resources.overlapBuffer;
        this.inputBuffer = resources.inputBuffer;
        this.processingBuffer = resources.processingBuffer;
        this.memoryPool = resources.memoryPool;
        this.usingSharedPool = resources.usingSharedPool;
    }

    /**
     * Helper class to bundle resources during initialization.
     */
    private static class ResourceBundle {
        WaveletTransform transform;
        NoiseEstimator noiseEstimator;
        StreamingThresholdAdapter thresholdAdapter;
        OverlapBuffer overlapBuffer;
        double[] inputBuffer;
        double[] processingBuffer;
        MemoryPool memoryPool;
        boolean usingSharedPool;
    }

    /**
     * Initializes all resources required by the StreamingDenoiser.
     * This method handles resource allocation with proper cleanup on failure.
     * 
     * @param builder the configuration builder
     * @return bundle of initialized resources
     * @throws RuntimeException if initialization fails
     */
    private static ResourceBundle initializeResources(Builder builder) {
        ResourceBundle resources = new ResourceBundle();
        boolean sharedPoolAcquired = false;

        try {
            // Create transform components
            TransformConfig config = TransformConfig.builder()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .build();
            resources.transform = new WaveletTransform(builder.wavelet, BoundaryMode.PERIODIC, config);

            // Create processing components
            resources.noiseEstimator = new MADNoiseEstimator(
                    builder.blockSize * builder.noiseBufferFactor, 0.95);
            resources.thresholdAdapter = new StreamingThresholdAdapter(
                    builder.attackTime, builder.releaseTime, 0.0, Double.MAX_VALUE);
            resources.overlapBuffer = new OverlapBuffer(
                    builder.blockSize, builder.overlapFactor, builder.windowFunction);

            // Setup memory pool
            if (builder.useSharedMemoryPool) {
                resources.memoryPool = SharedMemoryPoolManager.getInstance().getSharedPool();
                resources.usingSharedPool = true;
                sharedPoolAcquired = true;
            } else {
                resources.memoryPool = new MemoryPool();
                resources.usingSharedPool = false;
            }

            // Allocate buffers
            resources.inputBuffer = new double[builder.blockSize];
            resources.processingBuffer = resources.memoryPool.borrowArray(builder.blockSize);

            return resources;

        } catch (InvalidArgumentException e) {
            // Re-throw validation exceptions as-is
            throw e;
        } catch (OutOfMemoryError e) {
            // Clean up and provide specific memory error context
            cleanupResources(resources, sharedPoolAcquired);
            throw new RuntimeException("Insufficient memory to initialize StreamingDenoiser with block size " + 
                    builder.blockSize + ". Consider reducing block size or using shared memory pool.", e);
        } catch (Exception e) {
            // Clean up and provide initialization context
            cleanupResources(resources, sharedPoolAcquired);
            
            String errorContext = String.format(
                "Failed to initialize StreamingDenoiser (wavelet=%s, blockSize=%d, levels=%d, sharedPool=%s)",
                builder.wavelet != null ? builder.wavelet.name() : "null",
                builder.blockSize,
                builder.levels,
                builder.useSharedMemoryPool
            );
            
            throw new RuntimeException(errorContext, e);
        }
    }
    
    /**
     * Cleans up allocated resources during failed initialization.
     */
    private static void cleanupResources(ResourceBundle resources, boolean sharedPoolAcquired) {
        if (resources.processingBuffer != null && resources.memoryPool != null) {
            try {
                resources.memoryPool.returnArray(resources.processingBuffer);
            } catch (Exception ignored) {
                // Best effort cleanup - don't mask original exception
            }
        }

        if (sharedPoolAcquired) {
            try {
                SharedMemoryPoolManager.getInstance().releaseUser();
            } catch (Exception ignored) {
                // Best effort cleanup - don't mask original exception
            }
        }
    }

    public void process(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("StreamingDenoiser");
        }

        inputBuffer[inputPosition++] = sample;
        statistics.addSamples(1);

        if (inputPosition >= blockSize) {
            processBlock();
            inputPosition = 0;
        }
    }

    public void process(double[] samples) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("StreamingDenoiser");
        }

        for (double sample : samples) {
            process(sample);
        }
    }

    private void processBlock() {
        long startTime = System.nanoTime();

        try {
            // Copy input to processing buffer
            System.arraycopy(inputBuffer, 0, processingBuffer, 0, blockSize);

            // Process the signal - transforms will allocate new arrays as needed
            double[] denoised;

            if (levels == 1) {
                // Transform
                TransformResult result = transform.forward(processingBuffer);

                // Update noise estimate from details
                noiseEstimator.updateEstimate(result.detailCoeffs());

                // Calculate threshold
                double threshold = noiseEstimator.getThreshold(thresholdMethod);

                // Adapt threshold if enabled
                if (adaptiveThreshold) {
                    thresholdAdapter.setTargetThreshold(threshold);
                    threshold = thresholdAdapter.adaptThreshold();
                }

                // Apply thresholding
                double[] denoisedDetail = applyThreshold(result.detailCoeffs(), threshold);

                // Create a new TransformResult with denoised coefficients
                TransformResult denoisedResult = TransformResult.create(
                        result.approximationCoeffs(),
                        denoisedDetail
                );

                denoised = transform.inverse(denoisedResult);
            } else {
                // For multi-level denoising, perform multi-level decomposition
                MultiLevelWaveletTransform multiTransform = new MultiLevelWaveletTransform(
                        wavelet, boundaryMode, TransformConfig.builder().build());

                // Perform multi-level decomposition
                MultiLevelTransformResult multiResult = multiTransform.decompose(processingBuffer, levels);

                // Update noise estimate from finest detail level
                noiseEstimator.updateEstimate(multiResult.detailsAtLevel(1));

                // Calculate threshold
                double threshold = noiseEstimator.getThreshold(thresholdMethod);

                // Adapt threshold if enabled
                if (adaptiveThreshold) {
                    thresholdAdapter.setTargetThreshold(threshold);
                    threshold = thresholdAdapter.adaptThreshold();
                }

                // Reconstruct level by level with thresholding
                double[] current = multiResult.finalApproximation();

                // Reconstruct from coarsest to finest level
                // Note: In wavelet notation, higher level numbers represent coarser details
                // Level 1 = finest details (high frequency), Level N = coarsest details (low frequency)
                for (int level = levels; level >= 1; level--) {
                    double[] details = multiResult.detailsAtLevel(level);

                    // Apply level-dependent threshold scaling
                    // Higher levels (coarser details) get higher thresholds
                    // level=1: threshold * LEVEL_THRESHOLD_SCALE_FACTOR^0 = threshold (finest details, lowest threshold)
                    // level=N: threshold * LEVEL_THRESHOLD_SCALE_FACTOR^(N-1) (coarsest details, highest threshold)
                    double levelThreshold = threshold * Math.pow(LEVEL_THRESHOLD_SCALE_FACTOR, level - 1);
                    double[] denoisedDetails = applyThreshold(details, levelThreshold);

                    // Create single-level result
                    TransformResult levelResult = TransformResult.create(current, denoisedDetails);

                    // Apply inverse transform
                    current = transform.inverse(levelResult);
                }

                denoised = current;
            }

            // Apply overlap processing
            double[] output = overlapBuffer.process(denoised);

            // Emit result
            submit(output);

            // Update statistics
            long processingTime = System.nanoTime() - startTime;
            statistics.recordProcessingTime(processingTime);
            statistics.incrementBlocks();

        } catch (Exception e) {
            closeExceptionally(e);
        }
    }

    private double[] applyThreshold(double[] coefficients, double threshold) {
        if (VectorOps.isVectorizedOperationBeneficial(coefficients.length)) {
            return thresholdType == ThresholdType.SOFT ?
                    VectorOps.Denoising.softThreshold(coefficients, threshold) :
                    VectorOps.Denoising.hardThreshold(coefficients, threshold);
        } else {
            // Scalar fallback for small arrays
            double[] result = new double[coefficients.length];
            for (int i = 0; i < coefficients.length; i++) {
                double coeff = coefficients[i];
                if (thresholdType == ThresholdType.SOFT) {
                    double absCoeff = Math.abs(coeff);
                    result[i] = absCoeff <= threshold ? 0.0 :
                            Math.signum(coeff) * (absCoeff - threshold);
                } else {
                    result[i] = Math.abs(coeff) <= threshold ? 0.0 : coeff;
                }
            }
            return result;
        }
    }

    public void flush() {
        if (isClosed.get() || inputPosition == 0) {
            return;
        }

        // Pad with zeros and process
        Arrays.fill(inputBuffer, inputPosition, blockSize, 0.0);
        processBlock();
        inputPosition = 0;
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            flush();

            // Return processing buffer if it was allocated
            if (processingBuffer != null && memoryPool != null) {
                memoryPool.returnArray(processingBuffer);
            }

            // Handle cleanup based on pool type
            if (usingSharedPool) {
                // Notify the manager that we're done using the shared pool
                SharedMemoryPoolManager.getInstance().releaseUser();
            } else if (memoryPool != null) {
                // Clear the dedicated pool
                memoryPool.clear();
            }

            super.close();
        }
    }

    public boolean isReady() {
        return !isClosed.get();
    }

    public StreamingWaveletTransform.StreamingStatistics getStatistics() {
        return statistics;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getBufferLevel() {
        return inputPosition;
    }

    /**
     * Gets the current noise level estimate.
     */
    public double getCurrentNoiseLevel() {
        return noiseEstimator.getCurrentNoiseLevel();
    }

    /**
     * Gets the current threshold value.
     */
    public double getCurrentThreshold() {
        return thresholdAdapter.getCurrentThreshold();
    }

    /**
     * Builder for creating configured StreamingDenoiser instances.
     */
    public static class Builder {
        private Wavelet wavelet;
        private int blockSize = 512;
        private double overlapFactor = 0.75;
        private int levels = 1;
        private ThresholdMethod thresholdMethod = ThresholdMethod.UNIVERSAL;
        private ThresholdType thresholdType = ThresholdType.SOFT;
        private boolean adaptiveThreshold = true;
        private OverlapBuffer.WindowFunction windowFunction = OverlapBuffer.WindowFunction.HANN;
        private double attackTime = 10.0;
        private double releaseTime = 50.0;
        private boolean useSharedMemoryPool = true;
        private int noiseBufferFactor = DEFAULT_NOISE_BUFFER_FACTOR;

        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }

        public Builder blockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }

        public Builder overlapFactor(double overlapFactor) {
            if (overlapFactor < 0.0 || overlapFactor >= 1.0) {
                throw new IllegalArgumentException("Overlap factor must be in [0, 1)");
            }
            this.overlapFactor = overlapFactor;
            return this;
        }

        public Builder levels(int levels) {
            this.levels = levels;
            return this;
        }

        public Builder thresholdMethod(ThresholdMethod method) {
            this.thresholdMethod = method;
            return this;
        }

        public Builder thresholdType(ThresholdType type) {
            this.thresholdType = type;
            return this;
        }

        public Builder adaptiveThreshold(boolean adaptive) {
            this.adaptiveThreshold = adaptive;
            return this;
        }

        public Builder windowFunction(OverlapBuffer.WindowFunction function) {
            this.windowFunction = function;
            return this;
        }

        public Builder attackTime(double attackTime) {
            this.attackTime = attackTime;
            return this;
        }

        public Builder releaseTime(double releaseTime) {
            this.releaseTime = releaseTime;
            return this;
        }

        public Builder useSharedMemoryPool(boolean useShared) {
            this.useSharedMemoryPool = useShared;
            return this;
        }

        /**
         * Sets the noise estimator buffer size factor.
         * The noise estimator will use a buffer of size blockSize * factor.
         *
         * @param factor multiplication factor for noise buffer size (must be positive)
         * @return this builder
         */
        public Builder noiseBufferFactor(int factor) {
            if (factor <= 0) {
                throw new IllegalArgumentException("Noise buffer factor must be positive");
            }
            this.noiseBufferFactor = factor;
            return this;
        }

        public StreamingDenoiser build() {
            if (wavelet == null) {
                throw new InvalidArgumentException("Wavelet must be specified");
            }
            return new StreamingDenoiser(this);
        }
    }

    /**
     * Statistics implementation.
     */
    private static class StreamingStatisticsImpl implements StreamingWaveletTransform.StreamingStatistics {
        private final long startTime = System.nanoTime();
        private final AtomicLong samplesProcessed = new AtomicLong();
        private final AtomicLong blocksEmitted = new AtomicLong();
        private final AtomicLong totalProcessingTime = new AtomicLong();
        private final AtomicLong maxProcessingTime = new AtomicLong();
        private final AtomicLong overruns = new AtomicLong();

        void addSamples(int count) {
            samplesProcessed.addAndGet(count);
        }

        void incrementBlocks() {
            blocksEmitted.incrementAndGet();
        }

        void recordProcessingTime(long nanos) {
            totalProcessingTime.addAndGet(nanos);
            // Update max processing time using atomic compare-and-set
            long currentMax;
            do {
                currentMax = maxProcessingTime.get();
                if (nanos <= currentMax) {
                    break;
                }
            } while (!maxProcessingTime.compareAndSet(currentMax, nanos));
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
            return blocks > 0 ? totalProcessingTime.get() / (double) blocks : 0.0;
        }

        @Override
        public long getMaxProcessingTime() {
            return maxProcessingTime.get();
        }

        @Override
        public double getThroughput() {
            long elapsed = System.nanoTime() - startTime;
            return elapsed > 0 ? samplesProcessed.get() * 1_000_000_000.0 / elapsed : 0.0;
        }

        @Override
        public long getOverruns() {
            return overruns.get();
        }
    }
}