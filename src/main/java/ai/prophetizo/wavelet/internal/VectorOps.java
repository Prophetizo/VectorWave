package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.util.PlatformDetector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD-optimized wavelet operations using Java Vector API.
 *
 * <p>This class provides vectorized implementations of core wavelet operations,
 * offering significant performance improvements on modern CPUs with SIMD support.</p>
 *
 * <p>Key optimizations:</p>
 * <ul>
 *   <li>Vectorized convolution for filter operations</li>
 *   <li>Parallel downsampling and upsampling</li>
 *   <li>SIMD-accelerated boundary handling</li>
 *   <li>Automatic fallback to scalar operations when needed</li>
 * </ul>
 *
 * <p>Performance characteristics:</p>
 * <ul>
 *   <li>2-8x speedup on AVX2/AVX512 capable processors</li>
 *   <li>Best performance with aligned data and power-of-2 lengths</li>
 *   <li>Automatic selection of optimal vector species</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class VectorOps {

    // Vector species for different data widths
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    // Platform detection
    private static final boolean IS_ARM = PlatformDetector.isARM();
    private static final boolean IS_APPLE_SILICON = PlatformDetector.isAppleSilicon();

    private static final int VECTOR_LENGTH = SPECIES.length();
    // Minimum signal length to use vectorization - adjusted for platform
    // ARM/Apple Silicon has smaller vectors (128-bit) so lower threshold
    private static final int MIN_VECTOR_LENGTH = IS_ARM ? VECTOR_LENGTH * 2 : VECTOR_LENGTH * 4;
    // Cache line size for blocking optimizations
    private static final int CACHE_LINE_SIZE = 64;

    private VectorOps() {
        // Utility class
    }

    /**
     * Checks if Vector API is available and beneficial for the given signal length.
     * Takes into account platform-specific characteristics.
     */
    public static boolean isVectorizedOperationBeneficial(int signalLength) {
        // For Apple Silicon with 128-bit vectors, be more aggressive about using SIMD
        if (IS_APPLE_SILICON && VECTOR_LENGTH == 2) {
            return signalLength >= 8; // Just 4x vector length
        }
        return signalLength >= MIN_VECTOR_LENGTH && VECTOR_LENGTH > 1;
    }

    /**
     * Vectorized convolution with downsampling for periodic boundary mode.
     *
     * @param signal       input signal
     * @param filter       wavelet filter coefficients
     * @param signalLength length of the signal (must be power of 2)
     * @param filterLength length of the filter
     * @return downsampled convolution result
     */
    public static double[] convolveAndDownsamplePeriodic(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // Use vectorization only if beneficial
        if (!isVectorizedOperationBeneficial(signalLength)) {
            return ScalarOps.convolveAndDownsamplePeriodic(signal, filter, signalLength, filterLength);
        }

        // Process main body with vectorization
        int i = 0;
        int vectorBound = outputLength - (outputLength % VECTOR_LENGTH);

        for (; i < vectorBound; i += VECTOR_LENGTH) {
            // Initialize result vector
            DoubleVector result = DoubleVector.zero(SPECIES);

            // Compute convolution for each output element in the vector
            for (int k = 0; k < filterLength; k++) {
                // Broadcast filter coefficient
                DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                // Load signal values directly using gather for better performance
                // Create index array for gather operation
                int[] indices = new int[VECTOR_LENGTH];
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) & (signalLength - 1); // Periodic boundary
                }

                // Use fromArray with computed indices - more efficient than temporary array
                DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signal, 0,
                        indices, 0);
                result = result.add(filterVec.mul(signalVec));
            }

            // Store results
            result.intoArray(output, i);
        }

        // Handle remaining elements with scalar operations
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) & (signalLength - 1);
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Vectorized convolution with downsampling for zero-padding boundary mode.
     */
    public static double[] convolveAndDownsampleZeroPadding(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        if (!isVectorizedOperationBeneficial(signalLength)) {
            return ScalarOps.convolveAndDownsampleZeroPadding(signal, filter, signalLength, filterLength);
        }

        int i = 0;
        int vectorBound = outputLength - (outputLength % VECTOR_LENGTH);

        for (; i < vectorBound; i += VECTOR_LENGTH) {
            DoubleVector result = DoubleVector.zero(SPECIES);

            for (int k = 0; k < filterLength; k++) {
                DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                // More efficient approach: load contiguous data when possible
                int baseIdx = 2 * i + k;
                DoubleVector signalVec;

                if (baseIdx + 2 * VECTOR_LENGTH <= signalLength) {
                    // Fast path: all indices are valid, load with stride
                    if (VECTOR_LENGTH == 2) {
                        // For ARM/Apple Silicon with 2-element vectors
                        signalVec = DoubleVector.fromArray(SPECIES,
                                new double[]{signal[baseIdx], signal[baseIdx + 2]}, 0);
                    } else {
                        // General case - still need temp array but try to minimize overhead
                        double[] temp = new double[VECTOR_LENGTH];
                        for (int v = 0; v < VECTOR_LENGTH; v++) {
                            temp[v] = signal[baseIdx + 2 * v];
                        }
                        signalVec = DoubleVector.fromArray(SPECIES, temp, 0);
                    }
                } else {
                    // Slow path: need bounds checking
                    double[] temp = new double[VECTOR_LENGTH];
                    for (int v = 0; v < VECTOR_LENGTH; v++) {
                        int idx = baseIdx + 2 * v;
                        temp[v] = (idx < signalLength) ? signal[idx] : 0.0;
                    }
                    signalVec = DoubleVector.fromArray(SPECIES, temp, 0);
                }
                result = result.add(filterVec.mul(signalVec));
            }

            result.intoArray(output, i);
        }

        // Scalar fallback for remainder
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = 2 * i + k;
                if (idx < signalLength) {
                    sum += filter[k] * signal[idx];
                }
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Vectorized upsampling with convolution for periodic boundary mode.
     */
    public static double[] upsampleAndConvolvePeriodic(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];

        if (!isVectorizedOperationBeneficial(outputLength)) {
            return ScalarOps.upsampleAndConvolvePeriodic(signal, filter, signalLength, filterLength);
        }

        // Process even indices (direct copy with filtering)
        int i = 0;
        int vectorBound = signalLength - (signalLength % VECTOR_LENGTH);

        for (; i < vectorBound; i += VECTOR_LENGTH) {
            DoubleVector result = DoubleVector.zero(SPECIES);

            // Process filter coefficients that align with signal samples
            for (int k = 0; k < filterLength; k += 2) {
                int signalIdx = (i - k / 2 + signalLength) % signalLength;

                // Load signal values
                double[] signalValues = new double[VECTOR_LENGTH];
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    int idx = (signalIdx + v) % signalLength;
                    signalValues[v] = signal[idx];
                }

                DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signalValues, 0);
                DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);
                result = result.add(signalVec.mul(filterVec));
            }

            // Store at even indices
            for (int v = 0; v < VECTOR_LENGTH; v++) {
                output[2 * (i + v)] = result.lane(v);
            }
        }

        // Handle remaining even indices
        for (; i < signalLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k += 2) {
                int idx = (i - k / 2 + signalLength) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[2 * i] = sum;
        }

        // Process odd indices
        i = 0;
        for (; i < vectorBound; i += VECTOR_LENGTH) {
            DoubleVector result = DoubleVector.zero(SPECIES);

            for (int k = 1; k < filterLength; k += 2) {
                int signalIdx = (i - k / 2 + signalLength) % signalLength;

                double[] signalValues = new double[VECTOR_LENGTH];
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    int idx = (signalIdx + v) % signalLength;
                    signalValues[v] = signal[idx];
                }

                DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signalValues, 0);
                DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);
                result = result.add(signalVec.mul(filterVec));
            }

            // Store at odd indices
            for (int v = 0; v < VECTOR_LENGTH; v++) {
                output[2 * (i + v) + 1] = result.lane(v);
            }
        }

        // Handle remaining odd indices
        for (; i < signalLength; i++) {
            double sum = 0.0;
            for (int k = 1; k < filterLength; k += 2) {
                int idx = (i - k / 2 + signalLength) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[2 * i + 1] = sum;
        }

        return output;
    }

    /**
     * Vectorized upsampling with convolution for zero-padding boundary mode.
     *
     * <p>This implementation uses SIMD operations to efficiently process multiple
     * filter coefficients in parallel. Zero-padding mode is simpler than periodic
     * mode as it doesn't require modulo operations for boundary handling.</p>
     */
    public static double[] upsampleAndConvolveZeroPadding(
            double[] coeffs, double[] filter, int coeffsLength, int filterLength) {

        // Null checks
        if (coeffs == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }

        int outputLength = coeffsLength * 2;
        double[] output = new double[outputLength];

        if (!isVectorizedOperationBeneficial(outputLength)) {
            return ScalarOps.upsampleAndConvolveZeroPadding(coeffs, filter, coeffsLength, filterLength);
        }

        // Clear output array using vector operations for efficiency
        int clearBound = outputLength - VECTOR_LENGTH;
        DoubleVector zeros = DoubleVector.zero(SPECIES);
        for (int i = 0; i <= clearBound; i += VECTOR_LENGTH) {
            zeros.intoArray(output, i);
        }
        // Clear remainder
        for (int i = clearBound + VECTOR_LENGTH; i < outputLength; i++) {
            output[i] = 0.0;
        }

        // Process each coefficient
        for (int i = 0; i < coeffsLength; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients for efficiency

            int baseIndex = 2 * i;
            DoubleVector coeffVec = DoubleVector.broadcast(SPECIES, coeff);

            // Process filter coefficients in vectorized chunks
            int j = 0;
            int filterBound = filterLength - VECTOR_LENGTH;

            for (; j <= filterBound; j += VECTOR_LENGTH) {
                int outputIndex = baseIndex + j;
                if (outputIndex + VECTOR_LENGTH <= outputLength) {
                    // Load filter coefficients
                    DoubleVector filterVec = DoubleVector.fromArray(SPECIES, filter, j);
                    // Load current output values
                    DoubleVector outputVec = DoubleVector.fromArray(SPECIES, output, outputIndex);
                    // Multiply and accumulate
                    outputVec = outputVec.add(coeffVec.mul(filterVec));
                    // Store back
                    outputVec.intoArray(output, outputIndex);
                } else {
                    // Handle boundary with scalar operations
                    for (int k = 0; k < VECTOR_LENGTH && j + k < filterLength; k++) {
                        int idx = outputIndex + k;
                        if (idx < outputLength) {
                            output[idx] += coeff * filter[j + k];
                        }
                    }
                }
            }

            // Handle remaining filter coefficients with scalar operations
            for (; j < filterLength; j++) {
                int outputIndex = baseIndex + j;
                if (outputIndex < outputLength) {
                    output[outputIndex] += coeff * filter[j];
                }
            }
        }

        return output;
    }

    /**
     * Get information about the Vector API implementation.
     */
    public static String getVectorInfo() {
        String platform = PlatformDetector.getPlatform().toString();
        return String.format("Vector API: Species=%s, Length=%d, Platform=%s, Enabled=%b",
                SPECIES, VECTOR_LENGTH, platform, VECTOR_LENGTH > 1);
    }

    /**
     * Vectorized element-wise operations for wavelet denoising.
     */
    public static class Denoising {

        /**
         * Soft thresholding for wavelet coefficients.
         *
         * @param coefficients wavelet coefficients to threshold
         * @param threshold    threshold value
         * @return thresholded coefficients
         */
        public static double[] softThreshold(double[] coefficients, double threshold) {
            int length = coefficients.length;
            double[] result = new double[length];

            if (!isVectorizedOperationBeneficial(length)) {
                // Scalar fallback
                for (int i = 0; i < length; i++) {
                    double coeff = coefficients[i];
                    double absCoeff = Math.abs(coeff);
                    result[i] = absCoeff <= threshold ? 0.0 :
                            Math.signum(coeff) * (absCoeff - threshold);
                }
                return result;
            }

            // Vectorized implementation
            DoubleVector thresholdVec = DoubleVector.broadcast(SPECIES, threshold);
            DoubleVector negThresholdVec = DoubleVector.broadcast(SPECIES, -threshold);
            DoubleVector zeroVec = DoubleVector.zero(SPECIES);

            int i = 0;
            int vectorBound = length - (length % VECTOR_LENGTH);

            for (; i < vectorBound; i += VECTOR_LENGTH) {
                DoubleVector coeff = DoubleVector.fromArray(SPECIES, coefficients, i);

                // Create masks for different cases
                VectorMask<Double> positiveMask = coeff.compare(VectorOperators.GT, thresholdVec);
                VectorMask<Double> negativeMask = coeff.compare(VectorOperators.LT, negThresholdVec);
                VectorMask<Double> zeroMask = positiveMask.or(negativeMask).not();

                // Apply soft thresholding
                DoubleVector result_vec = zeroVec;
                result_vec = coeff.sub(thresholdVec).blend(result_vec, positiveMask.not());
                result_vec = coeff.add(thresholdVec).blend(result_vec, negativeMask.not());
                result_vec = zeroVec.blend(result_vec, zeroMask.not());

                result_vec.intoArray(result, i);
            }

            // Handle remainder with scalar operations
            for (; i < length; i++) {
                double coeff = coefficients[i];
                double absCoeff = Math.abs(coeff);
                result[i] = absCoeff <= threshold ? 0.0 :
                        Math.signum(coeff) * (absCoeff - threshold);
            }

            return result;
        }

        /**
         * Hard thresholding for wavelet coefficients.
         */
        public static double[] hardThreshold(double[] coefficients, double threshold) {
            int length = coefficients.length;
            double[] result = new double[length];

            if (!isVectorizedOperationBeneficial(length)) {
                // Scalar fallback
                for (int i = 0; i < length; i++) {
                    result[i] = Math.abs(coefficients[i]) <= threshold ? 0.0 : coefficients[i];
                }
                return result;
            }

            // Vectorized implementation
            DoubleVector thresholdVec = DoubleVector.broadcast(SPECIES, threshold);
            DoubleVector negThresholdVec = DoubleVector.broadcast(SPECIES, -threshold);
            DoubleVector zeroVec = DoubleVector.zero(SPECIES);

            int i = 0;
            int vectorBound = length - (length % VECTOR_LENGTH);

            for (; i < vectorBound; i += VECTOR_LENGTH) {
                DoubleVector coeff = DoubleVector.fromArray(SPECIES, coefficients, i);

                // Create mask for values to keep
                VectorMask<Double> keepMask = coeff.abs().compare(VectorOperators.GT, thresholdVec);

                // Apply hard thresholding
                DoubleVector result_vec = coeff.blend(zeroVec, keepMask.not());
                result_vec.intoArray(result, i);
            }

            // Handle remainder
            for (; i < length; i++) {
                result[i] = Math.abs(coefficients[i]) <= threshold ? 0.0 : coefficients[i];
            }

            return result;
        }
    }
}