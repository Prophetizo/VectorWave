package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool.PooledArray;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Memory-pooled vector operations for reduced allocation overhead.
 *
 * <p>This implementation uses {@link AlignedMemoryPool} to:</p>
 * <ul>
 *   <li>Eliminate temporary array allocations in hot paths</li>
 *   <li>Ensure 64-byte alignment for optimal SIMD performance</li>
 *   <li>Reduce GC pressure in high-throughput scenarios</li>
 * </ul>
 *
 * @since 1.4.0
 */
public final class VectorOpsPooled {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();

    private VectorOpsPooled() {
        // Utility class
    }

    /**
     * Pooled convolution with downsampling for periodic boundary.
     * Uses memory pool to avoid allocations.
     */
    public static double[] convolveAndDownsamplePeriodicPooled(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        int outputLength = signalLength / 2;

        // Use pooled array for output
        try (PooledArray pooledOutput = AlignedMemoryPool.allocate(outputLength)) {
            double[] output = pooledOutput.array();
            int outOffset = pooledOutput.offset();

            // Use pooled array for indices
            try (PooledArray pooledIndices = AlignedMemoryPool.allocate(VECTOR_LENGTH)) {
                double[] tempIndices = pooledIndices.array();
                int indOffset = pooledIndices.offset();

                // Process main body with vectorization
                int i = 0;
                int vectorBound = outputLength - (outputLength % VECTOR_LENGTH);

                for (; i < vectorBound; i += VECTOR_LENGTH) {
                    DoubleVector result = DoubleVector.zero(SPECIES);

                    for (int k = 0; k < filterLength; k++) {
                        DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                        // Gather signal values efficiently
                        for (int v = 0; v < VECTOR_LENGTH; v++) {
                            int idx = (2 * (i + v) + k) & (signalLength - 1);
                            tempIndices[indOffset + v] = signal[idx];
                        }

                        DoubleVector signalVec = DoubleVector.fromArray(SPECIES,
                                tempIndices, indOffset);
                        result = result.add(filterVec.mul(signalVec));
                    }

                    result.intoArray(output, outOffset + i);
                }

                // Scalar fallback for remainder
                for (; i < outputLength; i++) {
                    double sum = 0.0;
                    for (int k = 0; k < filterLength; k++) {
                        int idx = (2 * i + k) & (signalLength - 1);
                        sum += filter[k] * signal[idx];
                    }
                    output[outOffset + i] = sum;
                }

                // Copy result to non-pooled array
                double[] result = new double[outputLength];
                pooledOutput.copyTo(result, 0, outputLength);
                return result;
            }
        }
    }

    /**
     * Pooled batch convolution - processes multiple signals efficiently.
     * This is ideal for real-time processing of multiple channels.
     */
    public static double[][] batchConvolveAndDownsample(
            double[][] signals, double[] filter, int signalLength, int filterLength) {

        int batchSize = signals.length;
        int outputLength = signalLength / 2;
        double[][] results = new double[batchSize][outputLength];

        // Process in batches that fit in L2 cache
        int batchChunkSize = Math.min(8, batchSize); // Process 8 signals at a time

        // Allocate pooled arrays for batch processing
        try (PooledArray pooledTemp = AlignedMemoryPool.allocate(VECTOR_LENGTH * batchChunkSize)) {
            double[] temp = pooledTemp.array();
            int tempOffset = pooledTemp.offset();

            for (int b = 0; b < batchSize; b += batchChunkSize) {
                int currentBatchSize = Math.min(batchChunkSize, batchSize - b);

                // Process this batch chunk
                for (int i = 0; i < outputLength; i += VECTOR_LENGTH) {
                    int remainingOut = Math.min(VECTOR_LENGTH, outputLength - i);

                    // Process each signal in the batch
                    for (int s = 0; s < currentBatchSize; s++) {
                        DoubleVector result = DoubleVector.zero(SPECIES);

                        for (int k = 0; k < filterLength; k++) {
                            DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                            // Gather signal values
                            for (int v = 0; v < remainingOut; v++) {
                                int idx = (2 * (i + v) + k) & (signalLength - 1);
                                temp[tempOffset + s * VECTOR_LENGTH + v] = signals[b + s][idx];
                            }

                            DoubleVector signalVec = DoubleVector.fromArray(SPECIES,
                                    temp, tempOffset + s * VECTOR_LENGTH);
                            result = result.add(filterVec.mul(signalVec));
                        }

                        // Store results
                        if (remainingOut == VECTOR_LENGTH) {
                            result.intoArray(results[b + s], i);
                        } else {
                            // Handle partial vector at end
                            double[] partial = new double[VECTOR_LENGTH];
                            result.intoArray(partial, 0);
                            System.arraycopy(partial, 0, results[b + s], i, remainingOut);
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Cache-aware convolution for very large signals.
     * Processes signal in cache-friendly blocks.
     */
    public static double[] convolveAndDownsampleCacheAware(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        // L2 cache is typically 256KB-1MB, use conservative block size
        // Each double is 8 bytes, so 8192 doubles = 64KB
        final int BLOCK_SIZE = 8192;
        final int OUTPUT_BLOCK_SIZE = BLOCK_SIZE / 2;

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // Process in blocks
        for (int blockStart = 0; blockStart < outputLength; blockStart += OUTPUT_BLOCK_SIZE) {
            int blockEnd = Math.min(blockStart + OUTPUT_BLOCK_SIZE, outputLength);

            // Use pooled memory for this block
            try (PooledArray pooledBlock = AlignedMemoryPool.allocate(blockEnd - blockStart)) {
                double[] blockOutput = pooledBlock.array();
                int blockOffset = pooledBlock.offset();

                // Process this block with vectors
                for (int i = blockStart; i < blockEnd; i += VECTOR_LENGTH) {
                    int remainingOut = Math.min(VECTOR_LENGTH, blockEnd - i);
                    DoubleVector result = DoubleVector.zero(SPECIES);

                    for (int k = 0; k < filterLength; k++) {
                        DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                        // Load signal values for this vector
                        double[] temp = new double[VECTOR_LENGTH];
                        for (int v = 0; v < remainingOut; v++) {
                            int idx = (2 * (i + v) + k) & (signalLength - 1);
                            temp[v] = signal[idx];
                        }

                        DoubleVector signalVec = DoubleVector.fromArray(SPECIES, temp, 0);
                        result = result.add(filterVec.mul(signalVec));
                    }

                    // Store to block output
                    if (remainingOut == VECTOR_LENGTH) {
                        result.intoArray(blockOutput, blockOffset + (i - blockStart));
                    } else {
                        double[] partial = new double[VECTOR_LENGTH];
                        result.intoArray(partial, 0);
                        System.arraycopy(partial, 0, blockOutput,
                                blockOffset + (i - blockStart), remainingOut);
                    }
                }

                // Copy block to final output
                pooledBlock.copyTo(output, blockStart, blockEnd - blockStart);
            }
        }

        return output;
    }

    /**
     * Structure-of-Arrays (SoA) convolution for better SIMD utilization.
     * Processes multiple interleaved signals as separate arrays.
     */
    public static void convolveAndDownsampleSoA(
            double[] signalReal, double[] signalImag,
            double[] filterReal, double[] filterImag,
            double[] outputReal, double[] outputImag,
            int signalLength, int filterLength) {

        int outputLength = signalLength / 2;

        // Process real and imaginary parts separately for better vectorization
        try (PooledArray pooledTempR = AlignedMemoryPool.allocate(VECTOR_LENGTH);
             PooledArray pooledTempI = AlignedMemoryPool.allocate(VECTOR_LENGTH)) {

            double[] tempR = pooledTempR.array();
            double[] tempI = pooledTempI.array();
            int offsetR = pooledTempR.offset();
            int offsetI = pooledTempI.offset();

            for (int i = 0; i < outputLength; i += VECTOR_LENGTH) {
                int remaining = Math.min(VECTOR_LENGTH, outputLength - i);

                DoubleVector resultR = DoubleVector.zero(SPECIES);
                DoubleVector resultI = DoubleVector.zero(SPECIES);

                for (int k = 0; k < filterLength; k++) {
                    // Complex multiplication: (a + bi) * (c + di) = (ac - bd) + (ad + bc)i
                    DoubleVector filterR_vec = DoubleVector.broadcast(SPECIES, filterReal[k]);
                    DoubleVector filterI_vec = DoubleVector.broadcast(SPECIES, filterImag[k]);

                    // Gather signal values
                    for (int v = 0; v < remaining; v++) {
                        int idx = (2 * (i + v) + k) % signalLength;
                        tempR[offsetR + v] = signalReal[idx];
                        tempI[offsetI + v] = signalImag[idx];
                    }

                    DoubleVector signalR_vec = DoubleVector.fromArray(SPECIES, tempR, offsetR);
                    DoubleVector signalI_vec = DoubleVector.fromArray(SPECIES, tempI, offsetI);

                    // Complex multiplication
                    resultR = resultR.add(filterR_vec.mul(signalR_vec))
                            .sub(filterI_vec.mul(signalI_vec));
                    resultI = resultI.add(filterR_vec.mul(signalI_vec))
                            .add(filterI_vec.mul(signalR_vec));
                }

                // Store results
                if (remaining == VECTOR_LENGTH) {
                    resultR.intoArray(outputReal, i);
                    resultI.intoArray(outputImag, i);
                } else {
                    double[] partialR = new double[VECTOR_LENGTH];
                    double[] partialI = new double[VECTOR_LENGTH];
                    resultR.intoArray(partialR, 0);
                    resultI.intoArray(partialI, 0);
                    System.arraycopy(partialR, 0, outputReal, i, remaining);
                    System.arraycopy(partialI, 0, outputImag, i, remaining);
                }
            }
        }
    }
}