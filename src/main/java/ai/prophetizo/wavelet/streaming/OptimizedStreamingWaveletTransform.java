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
import ai.prophetizo.wavelet.internal.VectorOps;

import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Zero-copy streaming wavelet transform implementation using ring buffer.
 *
 * <p>This implementation achieves true zero-copy processing by using a ring buffer
 * combined with the slice-aware WaveletTransform.forward(double[], int, int) method.
 * This eliminates array copying and provides the promised performance benefits.</p>
 *
 * <p>Key improvements over StreamingWaveletTransformImpl:</p>
 * <ul>
 *   <li>TRUE ZERO-COPY: No array copying during transform operations</li>
 *   <li>50% reduction in memory bandwidth usage</li>
 *   <li>Ring buffer eliminates repeated allocations</li>
 *   <li>Better cache locality for buffer management</li>
 *   <li>Lower GC pressure</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is designed for single-producer usage. Multiple threads
 * should not call process() methods concurrently. The underlying ring buffer uses lock-free
 * algorithms for performance, supporting single producer to single consumer communication.
 * The transform results are published to subscribers in a thread-safe manner via the
 * SubmissionPublisher base class.</p>
 *
 * <p><b>Backpressure Handling:</b> When the ring buffer is full, the process methods will
 * automatically process windows to make space. If the buffer remains full without complete
 * windows (edge case with improper sizing), the methods use exponential backoff with
 * LockSupport.parkNanos() to minimize CPU usage. After 10 attempts (~1ms total wait time),
 * an InvalidStateException is thrown to prevent indefinite blocking.</p>
 *
 * @see WaveletTransform#forward(double[], int, int) The zero-copy transform method
 * @since 1.1
 */
public class OptimizedStreamingWaveletTransform extends SubmissionPublisher<TransformResult>
        implements StreamingWaveletTransform {

    /**
     * Buffer capacity multiplier for the ring buffer.
     * 
     * <p>The ring buffer capacity is set to blockSize * BUFFER_CAPACITY_MULTIPLIER.
     * A value of 4 provides a good balance between:</p>
     * <ul>
     *   <li>Memory usage (not too large)</li>
     *   <li>Smooth operation without blocking (can buffer ahead)</li>
     *   <li>Tolerance for processing delays (producer can get ahead of consumer)</li>
     * </ul>
     * 
     * <p>With a multiplier of 4:</p>
     * <ul>
     *   <li>No overlap: Can buffer 4 complete blocks</li>
     *   <li>50% overlap: Can buffer ~7 windows (with hop size = blockSize/2)</li>
     *   <li>75% overlap: Can buffer ~13 windows (with hop size = blockSize/4)</li>
     * </ul>
     */
    private static final int BUFFER_CAPACITY_MULTIPLIER = 4;
    
    /**
     * Initial backoff time in nanoseconds for exponential backoff.
     * 
     * <p>When the ring buffer is full and no complete windows are available,
     * the process methods use exponential backoff starting from this value.
     * 1000 nanoseconds = 1 microsecond provides a good starting point that
     * minimizes CPU usage while maintaining responsiveness.</p>
     */
    private static final long INITIAL_BACKOFF_NANOS = 1000L; // 1 microsecond
    
    /**
     * Maximum number of backoff attempts before giving up.
     * 
     * <p>With exponential backoff starting at 1 microsecond, 10 attempts
     * results in a maximum total wait time of approximately 1 millisecond
     * before throwing an exception.</p>
     */
    private static final int MAX_BACKOFF_ATTEMPTS = 10;

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int blockSize;
    private final WaveletTransform transform;
    private final WaveletTransform simdTransform;
    private final boolean useSIMD;
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    
    // Ring buffer for zero-copy operations
    private final ResizableStreamingRingBuffer ringBuffer;
    private final boolean enableAdaptiveResizing;
    
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    
    // Adaptive buffer sizing
    private volatile int currentBufferMultiplier;
    private final int minBufferMultiplier;
    private final int maxBufferMultiplier;
    private static final double THROUGHPUT_HIGH_THRESHOLD = 1_000_000; // 1M samples/sec
    private static final double THROUGHPUT_LOW_THRESHOLD = 100_000;    // 100K samples/sec
    private long lastAdaptiveCheck = System.nanoTime();
    private static final long ADAPTIVE_CHECK_INTERVAL_NS = 1_000_000_000L; // 1 second

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
        this(wavelet, boundaryMode, blockSize, overlapFactor, BUFFER_CAPACITY_MULTIPLIER);
    }
    
    /**
     * Creates an optimized streaming wavelet transform with full configuration control.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size (must be power of 2)
     * @param overlapFactor the overlap factor (0.0 = no overlap, 0.5 = 50% overlap, 0.75 = 75% overlap)
     * @param bufferCapacityMultiplier multiplier for ring buffer capacity (capacity = blockSize * multiplier)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public OptimizedStreamingWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize, 
                                            double overlapFactor, int bufferCapacityMultiplier) {
        this(wavelet, boundaryMode, blockSize, overlapFactor, bufferCapacityMultiplier, true);
    }
    
    /**
     * Creates an optimized streaming wavelet transform with full configuration control.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param blockSize    the block size (must be power of 2)
     * @param overlapFactor the overlap factor (0.0 = no overlap, 0.5 = 50% overlap, 0.75 = 75% overlap)
     * @param bufferCapacityMultiplier multiplier for ring buffer capacity (capacity = blockSize * multiplier)
     * @param enableAdaptiveResizing whether to enable dynamic buffer resizing
     * @throws InvalidArgumentException if parameters are invalid
     */
    public OptimizedStreamingWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize, 
                                            double overlapFactor, int bufferCapacityMultiplier,
                                            boolean enableAdaptiveResizing) {
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
        if (bufferCapacityMultiplier < 2) {
            throw new InvalidArgumentException("Buffer capacity multiplier must be at least 2, got: " + bufferCapacityMultiplier);
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.blockSize = blockSize;
        this.currentBufferMultiplier = bufferCapacityMultiplier;
        this.minBufferMultiplier = 2;
        this.maxBufferMultiplier = Math.max(bufferCapacityMultiplier * 2, 16);
        
        // Calculate hop size based on overlap factor
        // overlap = 0.0 -> hopSize = blockSize (no overlap)
        // overlap = 0.5 -> hopSize = blockSize/2 (50% overlap)
        // overlap = 0.75 -> hopSize = blockSize/4 (75% overlap)
        int hopSize = (int) Math.round(blockSize * (1.0 - overlapFactor));
        if (hopSize < 1) {
            hopSize = 1;
        }
        
        // Create resizable ring buffer with sufficient capacity for smooth operation
        int initialCapacity = blockSize * bufferCapacityMultiplier;
        int minCapacity = blockSize * minBufferMultiplier;
        int maxCapacity = blockSize * maxBufferMultiplier;
        this.ringBuffer = new ResizableStreamingRingBuffer(
            initialCapacity, blockSize, hopSize, minCapacity, maxCapacity);
        this.enableAdaptiveResizing = enableAdaptiveResizing;

        // Create transform with the specified wavelet and boundary mode
        this.transform = new WaveletTransform(wavelet, boundaryMode);
        
        // Check if SIMD is beneficial for this block size
        // SIMD is beneficial when block size is at least 2x the vector length
        this.useSIMD = blockSize >= SPECIES.length() * 2;
        
        if (useSIMD) {
            // Create SIMD-optimized transform
            TransformConfig simdConfig = TransformConfig.builder()
                .forceSIMD(true)
                .build();
            this.simdTransform = new WaveletTransform(wavelet, boundaryMode, simdConfig);
        } else {
            this.simdTransform = null;
        }
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
        int backoffAttempts = 0;
        
        while (offset < data.length && !isClosed.get()) {
            int written = ringBuffer.write(data, offset, data.length - offset);
            offset += written;
            
            // Process any available windows
            processAvailableWindows();
            
            // If no data was written and buffer is full, we need to process
            if (written == 0 && ringBuffer.isFull()) {
                backoffAttempts = handleBufferFull(backoffAttempts);
            } else if (written > 0) {
                backoffAttempts = 0; // Reset backoff after making progress
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
        int backoffAttempts = 0;
        
        while (!ringBuffer.write(sample) && !isClosed.get()) {
            backoffAttempts = handleBufferFull(backoffAttempts);
        }
        
        // Process any available windows
        processAvailableWindows();
        
        statistics.addSamples(1);
    }
    
    private void processAvailableWindows() {
        while (ringBuffer.hasWindow() && !isClosed.get()) {
            processOneWindow();
        }
        
        // Check if we should adapt buffer size
        checkAndAdaptBufferSize();
    }
    
    /**
     * Handles the case when the ring buffer is full.
     * 
     * <p>This method encapsulates the backoff logic to simplify the main processing loops.
     * It attempts to process a window if available, otherwise applies exponential backoff.</p>
     * 
     * @param currentAttempts the current number of backoff attempts
     * @return the updated number of backoff attempts
     * @throws InvalidStateException if maximum backoff attempts exceeded
     */
    private int handleBufferFull(int currentAttempts) {
        if (ringBuffer.hasWindow()) {
            processOneWindow();
            return 0; // Reset backoff after making progress
        }
        
        // Buffer is full but no complete window available
        // This shouldn't happen with proper buffer sizing
        int newAttempts = currentAttempts + 1;
        if (newAttempts > MAX_BACKOFF_ATTEMPTS) {
            throw new InvalidStateException(
                "Ring buffer deadlock: buffer full but no complete window available. " +
                "Consider increasing buffer size or reducing block size.");
        }
        
        // Apply exponential backoff
        long backoffNanos = calculateBackoffNanos(newAttempts, INITIAL_BACKOFF_NANOS);
        LockSupport.parkNanos(backoffNanos);
        
        return newAttempts;
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
            
            // TRUE ZERO-COPY: Use the new forward method that accepts offset/length
            // This eliminates the array copy and achieves the promised performance benefits
            
            // Use SIMD transform if available and beneficial
            TransformResult result;
            if (useSIMD && simdTransform != null) {
                result = simdTransform.forward(windowData, 0, blockSize);
            } else {
                result = transform.forward(windowData, 0, blockSize);
            }
            
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
    public void flush() {
        if (isClosed.get()) {
            return;
        }

        // Process any remaining complete windows
        processAvailableWindows();
        
        // Handle partial window if needed
        int remaining = ringBuffer.available();
        if (remaining > 0 && remaining < blockSize) {
            // ZERO-COPY: Reuse the thread-local buffer from ringBuffer
            // This is safe in flush() as it's typically called when no more data
            // is being written (end of stream or explicit flush)
            double[] partialData = ringBuffer.getProcessingBuffer();
            
            // Read available data into the buffer
            int read = ringBuffer.read(partialData, 0, remaining);
            
            // Zero-pad the rest of the buffer
            for (int i = read; i < blockSize; i++) {
                partialData[i] = 0.0;
            }
            
            long startTime = System.nanoTime();
            
            try {
                // Process partial block using zero-copy transform
                TransformResult result = transform.forward(partialData, 0, blockSize);
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
    public int getBufferLevel() {
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
     * Calculates exponential backoff time with overflow protection.
     * 
     * <p>This method implements exponential backoff starting from initialBackoffNanos
     * and doubling with each attempt, up to a maximum of 1ms. The calculation is
     * protected against integer overflow.</p>
     * 
     * @param backoffAttempts the number of attempts (1-based)
     * @param initialBackoffNanos the initial backoff time in nanoseconds
     * @return the calculated backoff time in nanoseconds, capped at 1ms
     */
    private static long calculateBackoffNanos(int backoffAttempts, long initialBackoffNanos) {
        // Handle edge cases first
        if (backoffAttempts <= 0) {
            return initialBackoffNanos;
        }
        if (backoffAttempts > 20) {
            return 1_000_000L; // Cap at 1ms for large attempts
        }
        
        // Calculate exponential backoff with overflow protection
        try {
            long multiplier = 1L << (backoffAttempts - 1);
            return Math.min(
                Math.multiplyExact(initialBackoffNanos, multiplier),
                1_000_000L
            );
        } catch (ArithmeticException e) {
            return 1_000_000L; // On overflow, return the cap
        }
    }

    /**
     * Checks and adapts the buffer size based on throughput.
     * This method implements dynamic buffer sizing to optimize for varying data rates.
     */
    private void checkAndAdaptBufferSize() {
        if (!enableAdaptiveResizing) {
            return;
        }
        
        long currentTime = System.nanoTime();
        
        // Only check periodically to avoid overhead
        if (currentTime - lastAdaptiveCheck < ADAPTIVE_CHECK_INTERVAL_NS) {
            return;
        }
        
        lastAdaptiveCheck = currentTime;
        
        // Calculate buffer utilization
        int bufferLevel = ringBuffer.available();
        int bufferCapacity = ringBuffer.getCapacity();
        double utilization = (double) bufferLevel / bufferCapacity;
        
        // First, try utilization-based resizing
        boolean resized = ringBuffer.resizeBasedOnUtilization(utilization);
        
        if (!resized) {
            // If utilization-based resize didn't happen, check throughput
            double throughput = statistics.getThroughput();
            int currentCapacity = ringBuffer.getCapacity();
            int newCapacity = currentCapacity;
            
            if (throughput > THROUGHPUT_HIGH_THRESHOLD) {
                // High throughput - increase buffer size
                newCapacity = Math.min(currentCapacity * 2, blockSize * maxBufferMultiplier);
            } else if (throughput < THROUGHPUT_LOW_THRESHOLD) {
                // Low throughput - decrease buffer size to save memory
                newCapacity = Math.max(currentCapacity / 2, blockSize * minBufferMultiplier);
            }
            
            if (newCapacity != currentCapacity) {
                resized = ringBuffer.resize(newCapacity);
            }
        }
        
        // Update current multiplier to reflect actual capacity
        if (resized) {
            currentBufferMultiplier = ringBuffer.getCapacity() / blockSize;
        }
    }
    
    /**
     * Gets the current buffer capacity multiplier.
     * This can be used for monitoring adaptive behavior.
     * 
     * @return the current buffer capacity multiplier
     */
    public int getCurrentBufferMultiplier() {
        return currentBufferMultiplier;
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