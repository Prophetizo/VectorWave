package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletOpsFactory;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.util.ValidationUtils;
import ai.prophetizo.wavelet.util.SignalUtils;

import java.lang.foreign.*;
import java.util.Objects;

/**
 * Wavelet transform implementation using Foreign Function & Memory API.
 * Provides drop-in replacement for WaveletTransform with improved memory efficiency.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>API-compatible with existing WaveletTransform</li>
 *   <li>Zero-copy operations on memory segments</li>
 *   <li>Reduced GC pressure through off-heap memory</li>
 *   <li>SIMD-aligned allocations for optimal performance</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Drop-in replacement
 * FFMWaveletTransform transform = new FFMWaveletTransform(new Haar());
 * TransformResult result = transform.forward(signal);
 * double[] reconstructed = transform.inverse(result);
 * 
 * // Or use with custom memory pool
 * try (FFMMemoryPool pool = new FFMMemoryPool()) {
 *     FFMWaveletTransform transform = new FFMWaveletTransform(new Haar(), pool);
 *     // ... perform transforms
 * }
 * }</pre>
 * 
 * @since 2.0.0
 */
public class FFMWaveletTransform implements AutoCloseable {
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final FFMMemoryPool memoryPool;
    private final boolean ownPool;
    private final FFMWaveletOps operations;
    
    // Pre-computed filters
    private final double[] lowPassFilter;
    private final double[] highPassFilter;
    private final double[] lowPassRecon;
    private final double[] highPassRecon;
    
    /**
     * Creates a wavelet transform with default settings.
     * 
     * @param wavelet the wavelet to use
     */
    public FFMWaveletTransform(Wavelet wavelet) {
        this(wavelet, BoundaryMode.PERIODIC, null, null);
    }
    
    /**
     * Creates a wavelet transform with specified boundary mode.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     */
    public FFMWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, null, null);
    }
    
    /**
     * Creates a wavelet transform using an existing memory pool.
     * 
     * @param wavelet the wavelet to use
     * @param memoryPool the memory pool to use
     */
    public FFMWaveletTransform(Wavelet wavelet, FFMMemoryPool memoryPool) {
        this(wavelet, BoundaryMode.PERIODIC, memoryPool, null);
    }
    
    /**
     * Creates a wavelet transform with full configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param memoryPool optional memory pool (null to create new)
     * @param config optional transform configuration
     */
    public FFMWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode,
                              FFMMemoryPool memoryPool, TransformConfig config) {
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "Boundary mode cannot be null");
        
        // Validate discrete wavelet
        if (!(wavelet instanceof DiscreteWavelet discreteWavelet)) {
            throw new InvalidArgumentException(
                "Only discrete wavelets are supported. Got: " + wavelet.getClass().getSimpleName()
            );
        }
        
        // Setup memory pool
        if (memoryPool != null) {
            this.memoryPool = memoryPool;
            this.ownPool = false;
        } else {
            this.memoryPool = new FFMMemoryPool();
            this.ownPool = true;
        }
        
        // Create operations
        this.operations = new FFMWaveletOps(this.memoryPool);
        
        // Pre-compute filters
        this.lowPassFilter = discreteWavelet.lowPassDecomposition();
        this.highPassFilter = discreteWavelet.highPassDecomposition();
        
        if (wavelet instanceof OrthogonalWavelet) {
            this.lowPassRecon = this.lowPassFilter;
            this.highPassRecon = this.highPassFilter;
        } else if (wavelet instanceof BiorthogonalWavelet biortho) {
            this.lowPassRecon = biortho.lowPassReconstruction();
            this.highPassRecon = biortho.highPassReconstruction();
        } else {
            throw new InvalidArgumentException(
                "Unsupported wavelet type: " + wavelet.getClass().getSimpleName()
            );
        }
        
        // Pre-warm pool with common sizes
        this.memoryPool.prewarm(64, 128, 256, 512, 1024);
    }
    
    /**
     * Performs forward wavelet transform.
     * 
     * @param signal the input signal
     * @return transform result with coefficients
     */
    public TransformResult forward(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");
        
        int outputLength = (signal.length + 1) / 2;
        double[] approxCoeffs = new double[outputLength];
        double[] detailCoeffs = new double[outputLength];
        
        if (boundaryMode == BoundaryMode.PERIODIC && 
            lowPassFilter.length == highPassFilter.length) {
            // Use combined transform for efficiency when filters have same length
            operations.combinedTransform(signal, lowPassFilter, highPassFilter,
                                       approxCoeffs, detailCoeffs);
        } else {
            // Separate transforms
            double[] tempApprox = operations.convolveAndDownsample(signal, lowPassFilter, 
                                                                   signal.length, lowPassFilter.length, boundaryMode);
            double[] tempDetail = operations.convolveAndDownsample(signal, highPassFilter, 
                                                                   signal.length, highPassFilter.length, boundaryMode);
            System.arraycopy(tempApprox, 0, approxCoeffs, 0, outputLength);
            System.arraycopy(tempDetail, 0, detailCoeffs, 0, outputLength);
        }
        
        return TransformResult.create(approxCoeffs, detailCoeffs);
    }
    
    /**
     * Performs forward transform with zero-copy on a signal slice.
     * 
     * @param signal the signal array
     * @param offset start offset
     * @param length length to process
     * @return transform result
     */
    public TransformResult forward(double[] signal, int offset, int length) {
        Objects.requireNonNull(signal, "Signal cannot be null");
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        if (!ValidationUtils.isPowerOfTwo(length)) {
            throw new InvalidArgumentException("Length must be power of two: " + length);
        }
        
        // Use memory segments for zero-copy slice
        MemorySegment signalSeg = MemorySegment.ofArray(signal)
            .asSlice(offset * Double.BYTES, length * Double.BYTES);
        
        return forwardSegment(signalSeg, length);
    }
    
    /**
     * Performs forward transform on a memory segment.
     * Enables true zero-copy processing.
     * 
     * @param signal the signal segment
     * @param length number of elements
     * @return transform result
     */
    public TransformResult forwardSegment(MemorySegment signal, int length) {
        if (!ValidationUtils.isPowerOfTwo(length)) {
            throw new InvalidArgumentException("Length must be power of two: " + length);
        }
        
        int outputLength = length / 2;
        
        // Acquire aligned memory from pool
        MemorySegment approxSeg = memoryPool.acquire(outputLength);
        MemorySegment detailSeg = memoryPool.acquire(outputLength);
        
        try {
            // Perform transform on segments
            MemorySegment filterLowSeg = MemorySegment.ofArray(lowPassFilter);
            MemorySegment filterHighSeg = MemorySegment.ofArray(highPassFilter);
            
            operations.convolveAndDownsampleFFM(signal, length, filterLowSeg, 
                                              lowPassFilter.length, approxSeg, outputLength, boundaryMode);
            operations.convolveAndDownsampleFFM(signal, length, filterHighSeg, 
                                              highPassFilter.length, detailSeg, outputLength, boundaryMode);
            
            // Copy to result arrays
            double[] approxCoeffs = new double[outputLength];
            double[] detailCoeffs = new double[outputLength];
            
            FFMArrayAllocator.copyToArray(approxSeg, approxCoeffs, 0, outputLength);
            FFMArrayAllocator.copyToArray(detailSeg, detailCoeffs, 0, outputLength);
            
            return TransformResult.create(approxCoeffs, detailCoeffs);
            
        } finally {
            memoryPool.release(approxSeg);
            memoryPool.release(detailSeg);
        }
    }
    
    /**
     * Performs inverse wavelet transform.
     * 
     * @param coefficients the transform coefficients
     * @return reconstructed signal
     */
    public double[] inverse(TransformResult coefficients) {
        Objects.requireNonNull(coefficients, "Coefficients cannot be null");
        
        double[] approxCoeffs = coefficients.approximationCoeffs();
        double[] detailCoeffs = coefficients.detailCoeffs();
        
        if (approxCoeffs.length != detailCoeffs.length) {
            throw new InvalidArgumentException("Coefficient arrays must have same length");
        }
        
        int outputLength = approxCoeffs.length * 2;
        double[] reconstructed = new double[outputLength];
        double[] temp = new double[outputLength];
        
        // Perform reconstruction
        double[] tempRecon1 = operations.upsampleAndConvolve(approxCoeffs, lowPassRecon,
                                                             approxCoeffs.length, lowPassRecon.length, boundaryMode);
        double[] tempRecon2 = operations.upsampleAndConvolve(detailCoeffs, highPassRecon,
                                                             detailCoeffs.length, highPassRecon.length, boundaryMode);
        System.arraycopy(tempRecon1, 0, reconstructed, 0, outputLength);
        System.arraycopy(tempRecon2, 0, temp, 0, outputLength);
        
        // Apply reconstruction scaling for biorthogonal wavelets
        double scale = 1.0;
        int phaseShift = 0;
        if (wavelet instanceof BiorthogonalSpline) {
            BiorthogonalSpline bior = (BiorthogonalSpline) wavelet;
            scale = bior.getReconstructionScale();
            phaseShift = bior.getGroupDelay();
        }
        
        // Add contributions with scaling
        for (int i = 0; i < outputLength; i++) {
            reconstructed[i] = scale * (reconstructed[i] + temp[i]);
        }
        
        // Apply phase compensation for biorthogonal wavelets
        if (phaseShift != 0 && boundaryMode == BoundaryMode.PERIODIC) {
            reconstructed = SignalUtils.circularShift(reconstructed, phaseShift);
        }
        
        return reconstructed;
    }
    
    /**
     * Performs combined forward-inverse transform using scoped memory.
     * All intermediate allocations are automatically freed.
     * 
     * @param signal the input signal
     * @return reconstructed signal
     */
    public double[] forwardInverse(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");
        
        return operations.forwardInverseScoped(signal, lowPassFilter, highPassFilter,
                                              lowPassRecon, highPassRecon);
    }
    
    /**
     * Gets memory pool statistics.
     * 
     * @return current statistics
     */
    public FFMMemoryPool.PoolStatistics getPoolStatistics() {
        return memoryPool.getStatistics();
    }
    
    /**
     * Gets the wavelet used by this transform.
     * 
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    @Override
    public void close() {
        operations.close();
        if (ownPool) {
            memoryPool.close();
        }
    }
}