package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.internal.ArrayPool;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;

/**
 * Memory-efficient wavelet transform that uses array pooling.
 * Optimized for batch processing of small signals.
 * 
 * <p>This implementation reuses arrays from a thread-local pool to
 * reduce garbage collection pressure during high-frequency transforms.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * WaveletTransformPool transform = new WaveletTransformPool(new Haar());
 * 
 * // Process many signals efficiently
 * for (double[] signal : signals) {
 *     TransformResult result = transform.forward(signal);
 *     // Process result...
 * }
 * 
 * // Optional: clear pool when done with batch
 * transform.clearPool();
 * }</pre>
 * </p>
 */
public class WaveletTransformPool {
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final double[] lowPassFilter;
    private final double[] highPassFilter;
    
    /**
     * Creates a wavelet transform with pooled memory and periodic boundary handling.
     *
     * @param wavelet the wavelet to use for transforms
     */
    public WaveletTransformPool(Wavelet wavelet) {
        this(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Creates a wavelet transform with pooled memory.
     *
     * @param wavelet the wavelet to use for transforms
     * @param boundaryMode the boundary handling mode
     */
    public WaveletTransformPool(Wavelet wavelet, BoundaryMode boundaryMode) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new IllegalArgumentException("Boundary mode cannot be null");
        }
        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        
        if (wavelet instanceof DiscreteWavelet discreteWavelet) {
            this.lowPassFilter = discreteWavelet.lowPassDecomposition();
            this.highPassFilter = discreteWavelet.highPassDecomposition();
        } else {
            throw new IllegalArgumentException(
                "Only discrete wavelets are supported. Got: " + wavelet.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * Performs forward wavelet transform using pooled arrays.
     *
     * @param signal the input signal
     * @return transform result containing approximation and detail coefficients
     */
    public TransformResult forward(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");
        
        int outputLength = (signal.length + 1) / 2;
        
        // Borrow arrays from pool
        double[] approxCoeffs = ArrayPool.borrow(outputLength);
        double[] detailCoeffs = ArrayPool.borrow(outputLength);
        
        try {
            if (boundaryMode == BoundaryMode.PERIODIC) {
                // Use combined transform for better cache efficiency
                ScalarOps.combinedTransformPeriodic(signal, lowPassFilter, highPassFilter,
                        approxCoeffs, detailCoeffs);
            } else {
                ScalarOps.convolveAndDownsampleDirect(signal, lowPassFilter, approxCoeffs);
                ScalarOps.convolveAndDownsampleDirect(signal, highPassFilter, detailCoeffs);
            }
            
            // Create result with copies (caller owns the result)
            return new TransformResultImpl(
                java.util.Arrays.copyOf(approxCoeffs, outputLength),
                java.util.Arrays.copyOf(detailCoeffs, outputLength)
            );
            
        } finally {
            // Always return arrays to pool
            ArrayPool.release(approxCoeffs);
            ArrayPool.release(detailCoeffs);
        }
    }
    
    /**
     * Performs inverse wavelet transform using pooled arrays.
     *
     * @param coefficients the transform coefficients
     * @return the reconstructed signal
     */
    public double[] inverse(TransformResult coefficients) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        
        double[] approxCoeffs = coefficients.approximationCoeffs();
        double[] detailCoeffs = coefficients.detailCoeffs();
        
        if (approxCoeffs == null) {
            throw new IllegalArgumentException("Approximation coefficients cannot be null");
        }
        if (detailCoeffs == null) {
            throw new IllegalArgumentException("Detail coefficients cannot be null");
        }
        if (approxCoeffs.length != detailCoeffs.length) {
            throw new IllegalArgumentException("Coefficient arrays must have same length");
        }
        
        int outputLength = approxCoeffs.length * 2;
        
        // Borrow arrays from pool
        double[] reconstructed = ArrayPool.borrow(outputLength);
        double[] temp = ArrayPool.borrow(outputLength);
        
        try {
            // Get reconstruction filters
            double[] lowPassRecon = wavelet instanceof OrthogonalWavelet ?
                    lowPassFilter : ((BiorthogonalWavelet) wavelet).lowPassReconstruction();
            double[] highPassRecon = wavelet instanceof OrthogonalWavelet ?
                    highPassFilter : ((BiorthogonalWavelet) wavelet).highPassReconstruction();
            
            if (boundaryMode == BoundaryMode.PERIODIC) {
                ScalarOps.upsampleAndConvolvePeriodic(approxCoeffs, lowPassRecon, reconstructed);
                ScalarOps.upsampleAndConvolvePeriodic(detailCoeffs, highPassRecon, temp);
            } else {
                ScalarOps.upsampleAndConvolveDirect(approxCoeffs, lowPassRecon, reconstructed);
                ScalarOps.upsampleAndConvolveDirect(detailCoeffs, highPassRecon, temp);
            }
            
            // Add contributions
            for (int i = 0; i < outputLength; i++) {
                reconstructed[i] += temp[i];
            }
            
            // Return a copy (caller owns the result)
            return java.util.Arrays.copyOf(reconstructed, outputLength);
            
        } finally {
            // Always return arrays to pool
            ArrayPool.release(reconstructed);
            ArrayPool.release(temp);
        }
    }
    
    /**
     * Performs forward transform followed immediately by inverse transform.
     * Optimized to reuse intermediate arrays.
     *
     * @param signal the input signal
     * @return the processed signal (should equal input for orthogonal wavelets)
     */
    public double[] forwardInverse(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");
        
        int coeffLength = (signal.length + 1) / 2;
        
        // Borrow all arrays at once
        double[] approxCoeffs = ArrayPool.borrow(coeffLength);
        double[] detailCoeffs = ArrayPool.borrow(coeffLength);
        double[] reconstructed = ArrayPool.borrow(signal.length);
        double[] temp = ArrayPool.borrow(signal.length);
        
        try {
            // Forward transform
            if (boundaryMode == BoundaryMode.PERIODIC) {
                ScalarOps.combinedTransformPeriodic(signal, lowPassFilter, highPassFilter,
                        approxCoeffs, detailCoeffs);
            } else {
                ScalarOps.convolveAndDownsampleDirect(signal, lowPassFilter, approxCoeffs);
                ScalarOps.convolveAndDownsampleDirect(signal, highPassFilter, detailCoeffs);
            }
            
            // Inverse transform
            double[] lowPassRecon = wavelet instanceof OrthogonalWavelet ?
                    lowPassFilter : ((BiorthogonalWavelet) wavelet).lowPassReconstruction();
            double[] highPassRecon = wavelet instanceof OrthogonalWavelet ?
                    highPassFilter : ((BiorthogonalWavelet) wavelet).highPassReconstruction();
            
            if (boundaryMode == BoundaryMode.PERIODIC) {
                ScalarOps.upsampleAndConvolvePeriodic(approxCoeffs, lowPassRecon, reconstructed);
                ScalarOps.upsampleAndConvolvePeriodic(detailCoeffs, highPassRecon, temp);
            } else {
                ScalarOps.upsampleAndConvolveDirect(approxCoeffs, lowPassRecon, reconstructed);
                ScalarOps.upsampleAndConvolveDirect(detailCoeffs, highPassRecon, temp);
            }
            
            // Add contributions
            for (int i = 0; i < signal.length; i++) {
                reconstructed[i] += temp[i];
            }
            
            // Return a copy
            return java.util.Arrays.copyOf(reconstructed, signal.length);
            
        } finally {
            // Return all arrays to pool
            ArrayPool.release(approxCoeffs);
            ArrayPool.release(detailCoeffs);
            ArrayPool.release(reconstructed);
            ArrayPool.release(temp);
        }
    }
    
    /**
     * Clears the thread-local array pool.
     * Call this when done with batch processing to free memory.
     */
    public void clearPool() {
        ArrayPool.clear();
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
     * Gets the boundary mode used by this transform.
     *
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
}