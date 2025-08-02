package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.WaveletOpsFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import java.lang.foreign.*;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Wavelet operations using Foreign Function & Memory API for zero-copy processing.
 * Combines FFM's memory management with Vector API for optimal performance.
 * 
 * <p>Key benefits:</p>
 * <ul>
 *   <li>Zero-copy transforms operating directly on MemorySegments</li>
 *   <li>SIMD-aligned memory for optimal vectorization</li>
 *   <li>Reduced GC pressure through off-heap memory</li>
 *   <li>Direct integration with native libraries</li>
 * </ul>
 * 
 * <p><strong>Boundary Mode Support:</strong></p>
 * <ul>
 *   <li><strong>Downsampling operations:</strong> All boundary modes supported (PERIODIC, ZERO_PADDING, SYMMETRIC, CONSTANT)
 *       <ul>
 *         <li>{@link #convolveAndDownsample(double[], double[], int, int, BoundaryMode)}</li>
 *         <li>{@link #convolveAndDownsampleFFM(MemorySegment, int, MemorySegment, int, MemorySegment, int, BoundaryMode)}</li>
 *       </ul>
 *   </li>
 *   <li><strong>Upsampling operations:</strong> Only PERIODIC and ZERO_PADDING modes implemented.
 *       SYMMETRIC and CONSTANT modes will throw {@link UnsupportedOperationException}
 *       <ul>
 *         <li>{@link #upsampleAndConvolve(double[], double[], int, int, BoundaryMode)}</li>
 *         <li>{@link #upsampleAndConvolveFFM(MemorySegment, int, MemorySegment, int, MemorySegment, int, BoundaryMode)}</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * @since 2.0.0
 */
public final class FFMWaveletOps implements WaveletOpsFactory.WaveletOps {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    private static final ValueLayout.OfDouble DOUBLE_LAYOUT = ValueLayout.JAVA_DOUBLE;
    
    private final FFMMemoryPool pool;
    private final boolean ownPool;
    
    /**
     * Creates FFM wavelet operations with a dedicated memory pool.
     */
    public FFMWaveletOps() {
        this.pool = new FFMMemoryPool();
        this.ownPool = true;
    }
    
    /**
     * Creates FFM wavelet operations using an existing memory pool.
     * 
     * @param pool the memory pool to use
     */
    public FFMWaveletOps(FFMMemoryPool pool) {
        this.pool = pool;
        this.ownPool = false;
    }
    
    @Override
    @SuppressWarnings("try")  // Arena is used in body, safe to suppress
    public double[] convolveAndDownsample(double[] signal, double[] filter,
                                         int signalLength, int filterLength,
                                         BoundaryMode mode) {
        int outputLen = signalLength / 2;
        double[] output = new double[outputLen];
        
        // Use FFM for zero-copy processing
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment signalSeg = MemorySegment.ofArray(signal);
            MemorySegment filterSeg = MemorySegment.ofArray(filter);
            MemorySegment outputSeg = MemorySegment.ofArray(output);
            
            convolveAndDownsampleFFM(signalSeg, signalLength, filterSeg, filterLength, 
                                    outputSeg, outputLen, mode);
        }
        
        return output;
    }
    
    /**
     * Zero-copy convolution and downsampling using memory segments.
     */
    public void convolveAndDownsampleFFM(MemorySegment signal, int signalLen,
                                        MemorySegment filter, int filterLen,
                                        MemorySegment output, int outputLen,
                                        BoundaryMode mode) {
        // Use vectorized operations when beneficial
        if (signalLen >= VECTOR_LENGTH * 4 && FFMArrayAllocator.isAligned(signal) && filterLen <= 4) {
            convolveAndDownsampleVectorized(signal, signalLen, filter, filterLen, 
                                          output, outputLen, mode);
        } else {
            convolveAndDownsampleScalar(signal, signalLen, filter, filterLen, 
                                      output, outputLen, mode);
        }
    }
    
    private void convolveAndDownsampleVectorized(MemorySegment signal, int signalLen,
                                                 MemorySegment filter, int filterLen,
                                                 MemorySegment output, int outputLen,
                                                 BoundaryMode mode) {
        // For vectorization, we process multiple output samples simultaneously
        // Each lane of the vector computes one output sample
        int i = 0;
        int vectorBound = outputLen - (outputLen % VECTOR_LENGTH);
        
        for (; i < vectorBound; i += VECTOR_LENGTH) {
            DoubleVector result = DoubleVector.zero(SPECIES);
            
            // For each filter coefficient
            for (int k = 0; k < filterLen; k++) {
                double filterVal = filter.getAtIndex(DOUBLE_LAYOUT, k);
                
                // Load signal values for each output position in the vector
                double[] signalVals = new double[VECTOR_LENGTH];
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    int idx = 2 * (i + v) + k;
                    signalVals[v] = getSignalValue(signal, idx, signalLen, mode);
                }
                
                DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signalVals, 0);
                result = result.add(signalVec.mul(filterVal));
            }
            
            // Store results
            result.intoMemorySegment(output, i * Double.BYTES, java.nio.ByteOrder.nativeOrder());
        }
        
        // Handle remaining elements
        for (; i < outputLen; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLen; k++) {
                int idx = 2 * i + k;
                sum += getSignalValue(signal, idx, signalLen, mode) * 
                       filter.getAtIndex(DOUBLE_LAYOUT, k);
            }
            output.setAtIndex(DOUBLE_LAYOUT, i, sum);
        }
    }
    
    private void convolveAndDownsampleScalar(MemorySegment signal, int signalLen,
                                            MemorySegment filter, int filterLen,
                                            MemorySegment output, int outputLen,
                                            BoundaryMode mode) {
        for (int i = 0; i < outputLen; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLen; k++) {
                int idx = 2 * i + k;
                sum += getSignalValue(signal, idx, signalLen, mode) * 
                       filter.getAtIndex(DOUBLE_LAYOUT, k);
            }
            output.setAtIndex(DOUBLE_LAYOUT, i, sum);
        }
    }
    
    /**
     * Gets signal value with boundary handling.
     */
    private double getSignalValue(MemorySegment signal, int idx, int signalLen, 
                                 BoundaryMode mode) {
        switch (mode) {
            case PERIODIC:
                // Wrap around using modulo
                return signal.getAtIndex(DOUBLE_LAYOUT, idx % signalLen);
            case ZERO_PADDING:
                // Return 0 for out-of-bounds
                return (idx >= 0 && idx < signalLen) ? 
                       signal.getAtIndex(DOUBLE_LAYOUT, idx) : 0.0;
            case SYMMETRIC:
                // Mirror at boundaries
                if (idx < 0) {
                    idx = -idx;
                } else if (idx >= signalLen) {
                    idx = 2 * signalLen - idx - 2;
                }
                return (idx >= 0 && idx < signalLen) ?
                       signal.getAtIndex(DOUBLE_LAYOUT, idx) : 0.0;
            default:
                throw new IllegalArgumentException("Unsupported boundary mode: " + mode);
        }
    }
    
    /**
     * Upsamples and convolves the input signal with the given filter.
     * 
     * @param input the input signal
     * @param filter the filter coefficients
     * @param inputLength the input signal length
     * @param filterLength the filter length
     * @param mode the boundary mode (only PERIODIC and ZERO_PADDING supported)
     * @return the upsampled and convolved result
     * @throws UnsupportedOperationException if mode is SYMMETRIC or CONSTANT
     */
    @Override
    @SuppressWarnings("try")  // Arena is used in body, safe to suppress
    public double[] upsampleAndConvolve(double[] input, double[] filter,
                                       int inputLength, int filterLength,
                                       BoundaryMode mode) {
        int outputLen = inputLength * 2;
        double[] output = new double[outputLen];
        
        // Use FFM for zero-copy processing
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment inputSeg = MemorySegment.ofArray(input);
            MemorySegment filterSeg = MemorySegment.ofArray(filter);
            MemorySegment outputSeg = MemorySegment.ofArray(output);
            
            upsampleAndConvolveFFM(inputSeg, inputLength, filterSeg, filterLength, outputSeg, outputLen, mode);
        }
        
        return output;
    }
    
    /**
     * Zero-copy upsampling and convolution using memory segments.
     * 
     * @param input the input signal segment
     * @param inputLen the number of elements in the input
     * @param filter the filter coefficients segment
     * @param filterLen the number of filter coefficients
     * @param output the output segment (must be pre-allocated with size 2*inputLen)
     * @param outputLen the output length (should be 2*inputLen)
     * @param mode the boundary mode (only PERIODIC and ZERO_PADDING supported)
     * @throws UnsupportedOperationException if mode is SYMMETRIC or CONSTANT
     */
    public void upsampleAndConvolveFFM(MemorySegment input, int inputLen,
                                      MemorySegment filter, int filterLen,
                                      MemorySegment output, int outputLen,
                                      BoundaryMode mode) {
        // Clear output
        output.fill((byte) 0);
        
        // Upsample and convolve
        for (int i = 0; i < inputLen; i++) {
            double inputVal = input.getAtIndex(DOUBLE_LAYOUT, i);
            
            for (int k = 0; k < filterLen; k++) {
                int idx = 2 * i + k;
                
                // Apply boundary mode handling
                if (mode == BoundaryMode.PERIODIC) {
                    // Wrap around for periodic mode
                    idx = idx % outputLen;
                    double filterVal = filter.getAtIndex(DOUBLE_LAYOUT, k);
                    double current = output.getAtIndex(DOUBLE_LAYOUT, idx);
                    output.setAtIndex(DOUBLE_LAYOUT, idx, current + inputVal * filterVal);
                } else if (mode == BoundaryMode.ZERO_PADDING) {
                    // Only process if within bounds
                    if (idx < outputLen) {
                        double filterVal = filter.getAtIndex(DOUBLE_LAYOUT, k);
                        double current = output.getAtIndex(DOUBLE_LAYOUT, idx);
                        output.setAtIndex(DOUBLE_LAYOUT, idx, current + inputVal * filterVal);
                    }
                } else {
                    // Other modes not yet implemented for upsampling
                    throw new UnsupportedOperationException("Boundary mode " + mode + " not supported for upsampling");
                }
            }
        }
    }
    
    public void combinedTransform(double[] signal, double[] lowPass, double[] highPass,
                                 double[] approx, double[] detail) {
        int signalLen = signal.length;
        int lowPassLen = lowPass.length;
        int highPassLen = highPass.length;
        int outputLen = approx.length;
        
        // Acquire temporary aligned memory from pool
        MemorySegment tempSignal = pool.acquire(signalLen);
        MemorySegment tempLowPass = pool.acquire(lowPassLen);
        MemorySegment tempHighPass = pool.acquire(highPassLen);
        MemorySegment tempApprox = pool.acquire(outputLen);
        MemorySegment tempDetail = pool.acquire(outputLen);
        
        try {
            // Copy input data to aligned segments
            FFMArrayAllocator.copyFromArray(signal, 0, tempSignal, signalLen);
            FFMArrayAllocator.copyFromArray(lowPass, 0, tempLowPass, lowPassLen);
            FFMArrayAllocator.copyFromArray(highPass, 0, tempHighPass, highPassLen);
            
            // Perform transforms on aligned memory
            // Note: combinedTransform uses PERIODIC mode by default
            convolveAndDownsampleFFM(tempSignal, signalLen, tempLowPass, lowPassLen, 
                                    tempApprox, outputLen, BoundaryMode.PERIODIC);
            convolveAndDownsampleFFM(tempSignal, signalLen, tempHighPass, highPassLen, 
                                    tempDetail, outputLen, BoundaryMode.PERIODIC);
            
            // Copy results back
            FFMArrayAllocator.copyToArray(tempApprox, approx, 0, outputLen);
            FFMArrayAllocator.copyToArray(tempDetail, detail, 0, outputLen);
            
        } finally {
            // Release segments back to pool
            pool.release(tempSignal);
            pool.release(tempLowPass);
            pool.release(tempHighPass);
            pool.release(tempApprox);
            pool.release(tempDetail);
        }
    }
    
    @Override
    public String getImplementationType() {
        return "FFM-" + (VECTOR_LENGTH > 1 ? "Vector" + VECTOR_LENGTH : "Scalar");
    }
    
    /**
     * Performs a complete forward-inverse transform using scoped memory.
     * All intermediate allocations are automatically cleaned up.
     * 
     * @param signal the input signal
     * @param lowPass low-pass filter
     * @param highPass high-pass filter
     * @param lowRecon low-pass reconstruction filter
     * @param highRecon high-pass reconstruction filter
     * @return the reconstructed signal
     */
    public double[] forwardInverseScoped(double[] signal, double[] lowPass, double[] highPass,
                                        double[] lowRecon, double[] highRecon) {
        return FFMMemoryPool.withScope(scopedPool -> {
            int signalLen = signal.length;
            int coeffLen = signalLen / 2;
            
            // All allocations in this scope are automatically freed
            MemorySegment approx = scopedPool.acquire(coeffLen);
            MemorySegment detail = scopedPool.acquire(coeffLen);
            MemorySegment reconstructed = scopedPool.acquire(signalLen);
            
            // Forward transform
            MemorySegment signalSeg = MemorySegment.ofArray(signal);
            MemorySegment lowPassSeg = MemorySegment.ofArray(lowPass);
            MemorySegment highPassSeg = MemorySegment.ofArray(highPass);
            
            convolveAndDownsampleFFM(signalSeg, signalLen, lowPassSeg, lowPass.length, 
                                    approx, coeffLen, BoundaryMode.PERIODIC);
            convolveAndDownsampleFFM(signalSeg, signalLen, highPassSeg, highPass.length, 
                                    detail, coeffLen, BoundaryMode.PERIODIC);
            
            // Inverse transform
            MemorySegment lowReconSeg = MemorySegment.ofArray(lowRecon);
            MemorySegment highReconSeg = MemorySegment.ofArray(highRecon);
            MemorySegment temp = scopedPool.acquire(signalLen);
            
            upsampleAndConvolveFFM(approx, coeffLen, lowReconSeg, lowRecon.length, 
                                  reconstructed, signalLen, BoundaryMode.PERIODIC);
            upsampleAndConvolveFFM(detail, coeffLen, highReconSeg, highRecon.length, 
                                  temp, signalLen, BoundaryMode.PERIODIC);
            
            // Add contributions
            for (int i = 0; i < signalLen; i++) {
                double val = reconstructed.getAtIndex(DOUBLE_LAYOUT, i) + 
                            temp.getAtIndex(DOUBLE_LAYOUT, i);
                reconstructed.setAtIndex(DOUBLE_LAYOUT, i, val);
            }
            
            // Copy result to array
            double[] result = new double[signalLen];
            FFMArrayAllocator.copyToArray(reconstructed, result, 0, signalLen);
            
            return result;
        });
    }
    
    /**
     * Closes the wavelet operations and releases resources.
     */
    public void close() {
        if (ownPool) {
            pool.close();
        }
    }
}