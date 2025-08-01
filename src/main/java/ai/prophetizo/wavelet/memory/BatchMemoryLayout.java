package ai.prophetizo.wavelet.memory;

import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.DoubleVector;
import java.util.Arrays;

/**
 * Optimized memory layout for batch wavelet processing.
 * 
 * <p>This class provides memory management specifically designed for batch SIMD operations:</p>
 * <ul>
 *   <li>Aligned memory allocation for optimal vector loads/stores</li>
 *   <li>Interleaved layout for better cache utilization</li>
 *   <li>Zero-copy views for efficient data access</li>
 *   <li>Automatic padding for SIMD alignment</li>
 * </ul>
 * 
 * @since 2.0.0
 */
public class BatchMemoryLayout implements AutoCloseable {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    private static final int ALIGNMENT = 64; // Cache line alignment
    
    // Validate vector length is reasonable
    static {
        if (VECTOR_LENGTH < 2 || VECTOR_LENGTH > 8) {
            throw new IllegalStateException(
                "Unexpected vector length: " + VECTOR_LENGTH + 
                ". Expected 2-8 doubles. Platform: " + System.getProperty("os.arch"));
        }
    }
    
    private final int batchSize;
    private final int signalLength;
    private final int paddedBatchSize;
    private final int paddedSignalLength;
    
    // Aligned memory pools
    private final AlignedMemoryPool.PooledArray inputPool;
    private final AlignedMemoryPool.PooledArray approxPool;
    private final AlignedMemoryPool.PooledArray detailPool;
    
    // Raw arrays from pools
    private final double[] inputData;
    private final double[] approxData;
    private final double[] detailData;
    
    /**
     * Creates a new batch memory layout.
     * 
     * @param batchSize Number of signals in the batch
     * @param signalLength Length of each signal
     */
    public BatchMemoryLayout(int batchSize, int signalLength) {
        this.batchSize = batchSize;
        this.signalLength = signalLength;
        
        // Pad to vector length for optimal SIMD
        this.paddedBatchSize = ((batchSize + VECTOR_LENGTH - 1) / VECTOR_LENGTH) * VECTOR_LENGTH;
        this.paddedSignalLength = signalLength; // Can be padded if needed
        
        // Allocate aligned memory
        int totalSize = paddedBatchSize * paddedSignalLength;
        int halfSize = paddedBatchSize * (paddedSignalLength / 2);
        
        this.inputPool = AlignedMemoryPool.allocate(totalSize);
        this.approxPool = AlignedMemoryPool.allocate(halfSize);
        this.detailPool = AlignedMemoryPool.allocate(halfSize);
        
        this.inputData = inputPool.array();
        this.approxData = approxPool.array();
        this.detailData = detailPool.array();
        
        // Clear padding areas
        Arrays.fill(inputData, 0.0);
        Arrays.fill(approxData, 0.0);
        Arrays.fill(detailData, 0.0);
    }
    
    /**
     * Copies signals into the aligned batch layout.
     * 
     * @param signals Input signals [signal_index][sample_index]
     */
    public void loadSignals(double[][] signals) {
        // Copy in a cache-friendly pattern
        for (int sig = 0; sig < batchSize; sig++) {
            System.arraycopy(signals[sig], 0, 
                           inputData, sig * paddedSignalLength, 
                           signalLength);
        }
    }
    
    /**
     * Copies signals into the layout with interleaving for better SIMD access.
     * 
     * @param signals Input signals
     * @param interleave If true, uses interleaved layout for vector processing
     */
    public void loadSignalsInterleaved(double[][] signals, boolean interleave) {
        if (!interleave) {
            loadSignals(signals);
            return;
        }
        
        // Interleave signals for SIMD processing
        // Layout: [s0[0], s1[0], ..., sN[0], s0[1], s1[1], ..., sN[1], ...]
        for (int sample = 0; sample < signalLength; sample++) {
            for (int sig = 0; sig < batchSize; sig++) {
                inputData[sample * paddedBatchSize + sig] = signals[sig][sample];
            }
        }
    }
    
    /**
     * Extracts results back to standard layout.
     * 
     * @param approxResults Output array for approximation coefficients
     * @param detailResults Output array for detail coefficients
     */
    public void extractResults(double[][] approxResults, double[][] detailResults) {
        int halfLength = signalLength / 2;
        
        for (int sig = 0; sig < batchSize; sig++) {
            System.arraycopy(approxData, sig * (paddedSignalLength / 2), 
                           approxResults[sig], 0, halfLength);
            System.arraycopy(detailData, sig * (paddedSignalLength / 2), 
                           detailResults[sig], 0, halfLength);
        }
    }
    
    /**
     * Extracts results from interleaved layout.
     */
    public void extractResultsInterleaved(double[][] approxResults, double[][] detailResults) {
        int halfLength = signalLength / 2;
        
        for (int sample = 0; sample < halfLength; sample++) {
            for (int sig = 0; sig < batchSize; sig++) {
                approxResults[sig][sample] = approxData[sample * paddedBatchSize + sig];
                detailResults[sig][sample] = detailData[sample * paddedBatchSize + sig];
            }
        }
    }
    
    /**
     * Performs optimized Haar transform on the batch using aligned memory.
     * This method works with standard (non-interleaved) layout.
     */
    public void haarTransformAligned() {
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);
        DoubleVector sqrt2Vec = DoubleVector.broadcast(SPECIES, SQRT2_INV);
        
        int halfLength = signalLength / 2;
        
        // Process each signal
        for (int sig = 0; sig < batchSize; sig++) {
            int signalOffset = sig * paddedSignalLength;
            int resultOffset = sig * (paddedSignalLength / 2);
            
            // Process samples for this signal
            for (int sample = 0; sample < halfLength; sample++) {
                double even = inputData[signalOffset + 2 * sample];
                double odd = inputData[signalOffset + 2 * sample + 1];
                
                approxData[resultOffset + sample] = (even + odd) * SQRT2_INV;
                detailData[resultOffset + sample] = (even - odd) * SQRT2_INV;
            }
        }
    }
    
    /**
     * Performs optimized Haar transform on interleaved data.
     */
    public void haarTransformInterleaved() {
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);
        DoubleVector sqrt2Vec = DoubleVector.broadcast(SPECIES, SQRT2_INV);
        
        int halfLength = signalLength / 2;
        
        // Process all signals in parallel with interleaved layout
        for (int sample = 0; sample < halfLength; sample++) {
            int evenBase = 2 * sample * paddedBatchSize;
            int oddBase = (2 * sample + 1) * paddedBatchSize;
            int outBase = sample * paddedBatchSize;
            
            // Process VECTOR_LENGTH signals at once
            for (int sig = 0; sig < paddedBatchSize; sig += VECTOR_LENGTH) {
                DoubleVector even = DoubleVector.fromArray(SPECIES, inputData, evenBase + sig);
                DoubleVector odd = DoubleVector.fromArray(SPECIES, inputData, oddBase + sig);
                
                DoubleVector approx = even.add(odd).mul(sqrt2Vec);
                DoubleVector detail = even.sub(odd).mul(sqrt2Vec);
                
                approx.intoArray(approxData, outBase + sig);
                detail.intoArray(detailData, outBase + sig);
            }
        }
    }
    
    /**
     * Gets the padded batch size for SIMD alignment.
     */
    public int getPaddedBatchSize() {
        return paddedBatchSize;
    }
    
    /**
     * Gets the input data array (for direct SIMD operations).
     */
    public double[] getInputData() {
        return inputData;
    }
    
    /**
     * Gets the approximation data array.
     */
    public double[] getApproxData() {
        return approxData;
    }
    
    /**
     * Gets the detail data array.
     */
    public double[] getDetailData() {
        return detailData;
    }
    
    @Override
    public void close() {
        // Return memory to pool
        if (inputPool != null) inputPool.close();
        if (approxPool != null) approxPool.close();
        if (detailPool != null) detailPool.close();
    }
    
    /**
     * Creates a batch layout optimized for the current platform.
     */
    public static BatchMemoryLayout createOptimized(int batchSize, int signalLength) {
        // Adjust batch size for optimal SIMD utilization
        int optimalBatch = batchSize;
        
        // For small batches, pad to at least 2x vector length
        if (batchSize < VECTOR_LENGTH * 2) {
            optimalBatch = VECTOR_LENGTH * 2;
        }
        
        return new BatchMemoryLayout(optimalBatch, signalLength);
    }
    
    /**
     * Gets information about the memory layout.
     */
    public String getLayoutInfo() {
        return String.format(
            "Batch Memory Layout:%n" +
            "Original batch size: %d%n" +
            "Padded batch size: %d%n" +
            "Signal length: %d%n" +
            "Memory alignment: %d bytes%n" +
            "Total memory: %.2f MB",
            batchSize,
            paddedBatchSize,
            signalLength,
            ALIGNMENT,
            (inputData.length + approxData.length + detailData.length) * 8.0 / (1024 * 1024)
        );
    }
}