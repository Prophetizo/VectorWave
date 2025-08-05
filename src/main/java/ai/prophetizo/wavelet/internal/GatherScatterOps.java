package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.util.PlatformDetector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;

/**
 * Optimized gather/scatter operations using Vector API intrinsics.
 *
 * <p>These operations are crucial for wavelet transforms which often
 * access non-contiguous memory locations due to downsampling and
 * boundary wrapping.</p>
 *
 */
final class GatherScatterOps {

    private static final VectorSpecies<Double> DOUBLE_SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final int DOUBLE_LENGTH = DOUBLE_SPECIES.length();
    private static final int INT_LENGTH = INT_SPECIES.length();

    // Platform detection
    private static final boolean GATHER_SCATTER_AVAILABLE = checkGatherScatterSupport();
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private GatherScatterOps() {
        // Utility class
    }

    /**
     * Check if gather/scatter operations are available on this platform.
     * 
     * <p>Gather/scatter operations require:</p>
     * <ul>
     *   <li>Vector API support with sufficient vector length</li>
     *   <li>Platform-specific intrinsics (AVX2+ on x86, SVE on ARM)</li>
     *   <li>Not universally supported on all platforms</li>
     * </ul>
     */
    private static boolean checkGatherScatterSupport() {
        try {
            // Check basic requirements
            if (DOUBLE_SPECIES.length() < 4) {
                // Need at least 256-bit vectors for efficient gather/scatter
                return false;
            }
            
            // Platform-specific checks
            PlatformDetector.Platform platform = PlatformDetector.getPlatform();
            
            switch (platform) {
                case X86_64:
                    // x86-64 needs AVX2 or later for gather operations
                    // Check if we can perform a gather operation
                    return checkX86GatherSupport();
                    
                case APPLE_SILICON:
                    // Apple Silicon M1/M2/M3 have good gather/scatter support
                    // but through different intrinsics than x86
                    return true;
                    
                case ARM:
                    // Generic ARM may or may not have SVE/SVE2
                    // Conservative approach: disable for now
                    return false;
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            // If any check fails, disable gather/scatter
            return false;
        }
    }
    
    /**
     * Check x86-64 gather support by attempting a gather operation.
     * 
     * Note: The Java Vector API's gather support is limited and may not
     * be available even on platforms with hardware gather instructions.
     */
    private static boolean checkX86GatherSupport() {
        // The Vector API's gather operation (fromArray with indices) has very
        // limited support and often throws exceptions even on capable hardware.
        // For now, we'll disable gather/scatter to ensure stability.
        // This can be revisited when the Vector API matures.
        return false;
    }

    /**
     * Optimized gather for periodic boundary convolution.
     * Gathers values from signal at computed indices for downsampling.
     */
    public static double[] gatherPeriodicDownsample(double[] signal, double[] filter,
                                                    int signalLength, int filterLength) {
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        if (!GATHER_SCATTER_AVAILABLE || DOUBLE_LENGTH < 4) {
            // Fallback to scalar implementation
            return scalarPeriodicDownsample(signal, filter, signalLength, filterLength);
        }

        // Process with gather operations
        int i = 0;
        for (; i <= outputLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            DoubleVector sum = DoubleVector.zero(DOUBLE_SPECIES);

            for (int k = 0; k < filterLength; k++) {
                // Create index vector for gather
                int[] indices = new int[DOUBLE_LENGTH];
                for (int v = 0; v < DOUBLE_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) % signalLength;
                }

                // Gather signal values manually (gather not available on all platforms)
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Multiply by filter coefficient and accumulate
                DoubleVector filterVec = DoubleVector.broadcast(DOUBLE_SPECIES, filter[k]);
                sum = sum.add(values.mul(filterVec));
            }

            // Store result
            sum.intoArray(output, i);
        }

        // Handle remainder with scalar code
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Optimized scatter for upsampling during inverse transform.
     * Scatters values to non-contiguous locations efficiently.
     */
    public static void scatterUpsample(double[] approx, double[] detail,
                                       double[] output, int length) {
        if (!GATHER_SCATTER_AVAILABLE || DOUBLE_LENGTH < 4) {
            // Fallback to scalar implementation
            scalarUpsample(approx, detail, output, length);
            return;
        }

        int halfLength = length / 2;

        // Process with scatter operations
        int i = 0;
        for (; i <= halfLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Load approximation and detail coefficients
            DoubleVector approxVec = DoubleVector.fromArray(DOUBLE_SPECIES, approx, i);
            DoubleVector detailVec = DoubleVector.fromArray(DOUBLE_SPECIES, detail, i);

            // Create index vectors for scatter
            int[] evenIndices = new int[DOUBLE_LENGTH];
            int[] oddIndices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                evenIndices[v] = 2 * (i + v);
                oddIndices[v] = 2 * (i + v) + 1;
            }

            // Scatter manually (scatter not available on all platforms)
            double[] approxArray = new double[DOUBLE_LENGTH];
            double[] detailArray = new double[DOUBLE_LENGTH];
            approxVec.intoArray(approxArray, 0);
            detailVec.intoArray(detailArray, 0);

            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                output[evenIndices[j]] = approxArray[j];
                output[oddIndices[j]] = detailArray[j];
            }
        }

        // Handle remainder with scalar code
        for (; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
    }

    /**
     * Batch gather operation for multiple signals.
     * Efficiently processes multiple gather operations in parallel.
     */
    public static void batchGather(double[][] signals, int[] indices,
                                   double[][] results, int count) {
        if (!GATHER_SCATTER_AVAILABLE) {
            batchGatherScalar(signals, indices, results, count);
            return;
        }

        int numSignals = signals.length;

        // Process multiple signals simultaneously
        for (int s = 0; s < numSignals; s++) {
            double[] signal = signals[s];
            double[] result = results[s];

            int i = 0;
            for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
                // Gather values manually
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[i + j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Store gathered values
                values.intoArray(result, i);
            }

            // Handle remainder
            for (; i < count; i++) {
                result[i] = signal[indices[i]];
            }
        }
    }

    /**
     * Optimized gather for strided access patterns.
     * Common in multi-level wavelet decomposition.
     */
    public static double[] gatherStrided(double[] signal, int offset,
                                         int stride, int count) {
        double[] result = new double[count];

        if (!GATHER_SCATTER_AVAILABLE || stride > 8) {
            // Fallback for large strides or no gather support
            for (int i = 0; i < count; i++) {
                result[i] = signal[offset + i * stride];
            }
            return result;
        }

        // Use gather for strided access
        int i = 0;
        for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create strided indices
            int[] indices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                indices[v] = offset + (i + v) * stride;
            }

            // Gather values manually
            double[] gathered = new double[DOUBLE_LENGTH];
            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                gathered[j] = signal[indices[j]];
            }
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

            // Store result
            values.intoArray(result, i);
        }

        // Handle remainder
        for (; i < count; i++) {
            result[i] = signal[offset + i * stride];
        }

        return result;
    }

    /**
     * Compressed gather operation for sparse access patterns.
     * Uses a mask to gather only selected elements.
     */
    public static double[] gatherCompressed(double[] signal, boolean[] mask) {
        // Count true elements
        int count = 0;
        for (boolean b : mask) {
            if (b) count++;
        }

        double[] result = new double[count];
        int resultIdx = 0;

        if (!GATHER_SCATTER_AVAILABLE) {
            // Scalar fallback
            for (int i = 0; i < mask.length; i++) {
                if (mask[i]) {
                    result[resultIdx++] = signal[i];
                }
            }
            return result;
        }

        // Process with vector mask
        int i = 0;
        for (; i <= mask.length - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create mask vector
            boolean[] maskChunk = new boolean[DOUBLE_LENGTH];
            System.arraycopy(mask, i, maskChunk, 0, DOUBLE_LENGTH);
            VectorMask<Double> vectorMask = VectorMask.fromArray(DOUBLE_SPECIES, maskChunk, 0);

            // Load values under mask
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, signal, i, vectorMask);

            // Compress values under mask
            DoubleVector compressed = values.compress(vectorMask);
            int compressedLength = vectorMask.trueCount();
            
            // Only copy the valid compressed elements
            if (compressedLength > 0) {
                double[] temp = new double[DOUBLE_LENGTH];
                compressed.intoArray(temp, 0);
                System.arraycopy(temp, 0, result, resultIdx, compressedLength);
            }
            
            resultIdx += compressedLength;
        }

        // Handle remainder
        for (; i < mask.length; i++) {
            if (mask[i]) {
                result[resultIdx++] = signal[i];
            }
        }

        return result;
    }

    // Scalar fallback implementations

    private static double[] scalarPeriodicDownsample(double[] signal, double[] filter,
                                                     int signalLength, int filterLength) {
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    private static void scalarUpsample(double[] approx, double[] detail,
                                       double[] output, int length) {
        int halfLength = length / 2;
        for (int i = 0; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
    }

    private static void batchGatherScalar(double[][] signals, int[] indices,
                                          double[][] results, int count) {
        for (int s = 0; s < signals.length; s++) {
            for (int i = 0; i < count; i++) {
                results[s][i] = signals[s][indices[i]];
            }
        }
    }

    /**
     * Check if gather/scatter operations are available.
     */
    public static boolean isGatherScatterAvailable() {
        return GATHER_SCATTER_AVAILABLE;
    }

    /**
     * Get information about gather/scatter support.
     */
    public static String getGatherScatterInfo() {
        return String.format(
                "Gather/Scatter Support: %s%nVector Length: %d doubles%nPlatform: %s",
                GATHER_SCATTER_AVAILABLE ? "Available" : "Not Available",
                DOUBLE_LENGTH,
                IS_LITTLE_ENDIAN ? "Little Endian" : "Big Endian"
        );
    }
}