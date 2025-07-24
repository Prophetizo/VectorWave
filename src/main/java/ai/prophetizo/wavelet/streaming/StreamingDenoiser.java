package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.MultiLevelTransformResult;
import ai.prophetizo.wavelet.MultiLevelWaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.memory.MemoryPool;

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
    private MemoryPool memoryPool;
    private boolean usingSharedPool;
    
    // Buffers
    private final double[] inputBuffer;
    private double[] processingBuffer;
    private int inputPosition;
    
    // State
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    
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
        
        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        public Builder blockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }
        
        public Builder overlapFactor(double overlapFactor) {
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
        
        public StreamingDenoiser build() {
            if (wavelet == null) {
                throw new InvalidArgumentException("Wavelet must be specified");
            }
            return new StreamingDenoiser(this);
        }
    }
    
    private StreamingDenoiser(Builder builder) {
        super();
        
        this.wavelet = builder.wavelet;
        this.boundaryMode = BoundaryMode.PERIODIC; // Default for streaming
        this.blockSize = builder.blockSize;
        this.levels = builder.levels;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdType = builder.thresholdType;
        this.adaptiveThreshold = builder.adaptiveThreshold;
        
        // Initialize processing buffer to null for exception safety during construction
        this.processingBuffer = null;
        
        MemoryPool tempPool = null;
        boolean tempUsingSharedPool = false;
        
        try {
            // Create components
            TransformConfig config = TransformConfig.builder()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .build();
            this.transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC, config);
            
            this.noiseEstimator = new MADNoiseEstimator(blockSize * 4, 0.95);
            this.thresholdAdapter = new StreamingThresholdAdapter(
                builder.attackTime, builder.releaseTime, 0.0, Double.MAX_VALUE);
            this.overlapBuffer = new OverlapBuffer(blockSize, builder.overlapFactor, builder.windowFunction);
            
            // Use shared or dedicated memory pool based on configuration
            if (builder.useSharedMemoryPool) {
                tempPool = SharedMemoryPoolManager.getInstance().getSharedPool();
                tempUsingSharedPool = true;
            } else {
                tempPool = new MemoryPool();
                tempUsingSharedPool = false;
            }
            
            // Allocate buffers
            this.inputBuffer = new double[blockSize];
            
            // Borrow processing buffer - this is the risky operation
            double[] tempBuffer = tempPool.borrowArray(blockSize);
            
            // Only assign fields after all allocations succeed
            this.memoryPool = tempPool;
            this.usingSharedPool = tempUsingSharedPool;
            this.processingBuffer = tempBuffer;
            this.inputPosition = 0;
            
        } catch (Exception e) {
            // Clean up any borrowed resources
            if (this.processingBuffer != null && tempPool != null) {
                tempPool.returnArray(this.processingBuffer);
            }
            
            // Release shared pool if we acquired it
            if (tempUsingSharedPool) {
                SharedMemoryPoolManager.getInstance().releaseUser();
            }
            
            throw e;
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
            
            // Work in-place on processing buffer to avoid extra allocation
            double[] denoised = processingBuffer;
            
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
                
                // Perform proper inverse transform
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
                for (int level = levels; level >= 1; level--) {
                    double[] details = multiResult.detailsAtLevel(level);
                    
                    // Apply level-dependent threshold scaling (finer levels get higher threshold)
                    double levelThreshold = threshold * Math.pow(1.2, level - 1);
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
     * Statistics implementation.
     */
    private static class StreamingStatisticsImpl implements StreamingWaveletTransform.StreamingStatistics {
        private final long startTime = System.nanoTime();
        private final AtomicLong samplesProcessed = new AtomicLong();
        private final AtomicLong blocksEmitted = new AtomicLong();
        private final AtomicLong totalProcessingTime = new AtomicLong();
        private final AtomicLong overruns = new AtomicLong();
        
        void addSamples(int count) {
            samplesProcessed.addAndGet(count);
        }
        
        void incrementBlocks() {
            blocksEmitted.incrementAndGet();
        }
        
        void recordProcessingTime(long nanos) {
            totalProcessingTime.addAndGet(nanos);
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
            // For simplicity, return average * 2 as estimate
            return (long)(getAverageProcessingTime() * 2);
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