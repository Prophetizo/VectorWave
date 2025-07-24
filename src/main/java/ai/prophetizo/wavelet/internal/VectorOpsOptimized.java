package ai.prophetizo.wavelet.internal;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

/**
 * Optimized SIMD wavelet operations using advanced Vector API features.
 *
 * <p>This class provides highly optimized vectorized implementations with:</p>
 * <ul>
 *   <li>Efficient memory gather operations</li>
 *   <li>Combined transform operations</li>
 *   <li>Specialized implementations for common wavelets</li>
 *   <li>Cache-aware processing</li>
 * </ul>
 *
 * @since 1.4.0
 */
public final class VectorOpsOptimized {

    // Vector species for different data widths
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> ISPECIES = IntVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    // Minimum signal length to use vectorization
    private static final int MIN_VECTOR_LENGTH = VECTOR_LENGTH * 4;

    // Cache line size for blocking
    private static final int CACHE_LINE_SIZE = 64; // bytes
    private static final int BYTES_PER_DOUBLE = 8;
    private static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / BYTES_PER_DOUBLE;

    private VectorOpsOptimized() {
        // Utility class
    }

    /**
     * Optimized convolution with downsampling using gather operations.
     *
     * <p>Note on memory allocation: This method allocates small int arrays (typically 2-8 elements)
     * on the stack for gather indices. While this creates temporary objects, it avoids the
     * complexity and potential memory leaks of ThreadLocal caching. The JVM is highly optimized
     * for such small, short-lived allocations through TLAB (Thread Local Allocation Buffers)
     * and escape analysis, making this approach both safe and efficient for production use.</p>
     */
    public static double[] convolveAndDownsamplePeriodicOptimized(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // Fall back to scalar for small signals
        if (!VectorOps.isVectorizedOperationBeneficial(signalLength)) {
            return ScalarOps.convolveAndDownsamplePeriodic(signal, filter, signalLength, filterLength);
        }

        int signalMask = signalLength - 1; // For periodic boundary

        // Process main body with vectorization
        int i = 0;
        int vectorBound = outputLength - VECTOR_LENGTH;

        // Allocate indices array on stack for small size
        // This avoids both ThreadLocal issues and allocation overhead
        int[] indices = new int[VECTOR_LENGTH];

        for (; i <= vectorBound; i += VECTOR_LENGTH) {
            DoubleVector accumulator = DoubleVector.zero(SPECIES);

            // Unroll first few filter taps for common sizes
            if (filterLength >= 4) {
                // Unroll first 4 taps
                accumulator = accumulator.add(gatherMultiplyAccumulate(signal, filter[0], indices, signalMask, i, 0));
                accumulator = accumulator.add(gatherMultiplyAccumulate(signal, filter[1], indices, signalMask, i, 1));
                accumulator = accumulator.add(gatherMultiplyAccumulate(signal, filter[2], indices, signalMask, i, 2));
                accumulator = accumulator.add(gatherMultiplyAccumulate(signal, filter[3], indices, signalMask, i, 3));

                // Process remaining filter taps
                for (int k = 4; k < filterLength; k++) {
                    accumulator = accumulator.add(gatherMultiplyAccumulate(signal, filter[k], indices, signalMask, i, k));
                }
            } else {
                // General case for small filters
                for (int k = 0; k < filterLength; k++) {
                    for (int v = 0; v < VECTOR_LENGTH; v++) {
                        indices[v] = (2 * (i + v) + k) & signalMask;
                    }
                    DoubleVector sigVec = DoubleVector.fromArray(SPECIES, signal, 0, indices, 0);
                    accumulator = accumulator.add(sigVec.mul(filter[k]));
                }
            }

            accumulator.intoArray(output, i);
        }

        // Handle remaining elements with scalar operations
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) & signalMask;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Helper method for gather and multiply-accumulate operation.
     * Gathers signal values based on indices and multiplies by filter coefficient.
     */
    private static DoubleVector gatherMultiplyAccumulate(double[] signal, double filterCoeff,
                                                         int[] indices, int signalMask,
                                                         int baseIndex, int filterIndex) {
        // Compute indices for this filter tap
        for (int v = 0; v < VECTOR_LENGTH; v++) {
            indices[v] = (2 * (baseIndex + v) + filterIndex) & signalMask;
        }

        // Gather signal values and multiply by filter coefficient
        DoubleVector sigVec = DoubleVector.fromArray(SPECIES, signal, 0, indices, 0);
        return sigVec.mul(filterCoeff);
    }

    /**
     * Vectorized combined transform for both approximation and detail coefficients.
     * Processes both filters in a single pass for better cache efficiency.
     */
    public static void combinedTransformPeriodicVectorized(
            double[] signal, double[] lowFilter, double[] highFilter,
            double[] approxCoeffs, double[] detailCoeffs) {

        int signalLen = signal.length;
        int outputLen = signalLen / 2;
        int filterLen = lowFilter.length;

        if (!VectorOps.isVectorizedOperationBeneficial(signalLen)) {
            ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter,
                    approxCoeffs, detailCoeffs);
            return;
        }

        int signalMask = signalLen - 1;
        // Allocate indices array on stack
        int[] indices = new int[VECTOR_LENGTH];

        int i = 0;
        int vectorBound = outputLen - VECTOR_LENGTH;

        for (; i <= vectorBound; i += VECTOR_LENGTH) {
            DoubleVector lowAccum = DoubleVector.zero(SPECIES);
            DoubleVector highAccum = DoubleVector.zero(SPECIES);

            // Process both filters in single pass
            for (int k = 0; k < filterLen; k++) {
                // Gather signal values once
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) & signalMask;
                }
                DoubleVector sigVec = DoubleVector.fromArray(SPECIES, signal, 0, indices, 0);

                // Apply both filters
                lowAccum = lowAccum.add(sigVec.mul(lowFilter[k]));
                highAccum = highAccum.add(sigVec.mul(highFilter[k]));
            }

            lowAccum.intoArray(approxCoeffs, i);
            highAccum.intoArray(detailCoeffs, i);
        }

        // Scalar remainder
        for (; i < outputLen; i++) {
            double sumLow = 0.0;
            double sumHigh = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int signalIndex = (kStart + j) & signalMask;
                double signalValue = signal[signalIndex];
                sumLow += signalValue * lowFilter[j];
                sumHigh += signalValue * highFilter[j];
            }

            approxCoeffs[i] = sumLow;
            detailCoeffs[i] = sumHigh;
        }
    }

    /**
     * Specialized Haar wavelet transform using SIMD.
     * Exploits the simple structure of Haar wavelets for maximum performance.
     */
    public static void haarTransformVectorized(double[] signal,
                                               double[] approxCoeffs,
                                               double[] detailCoeffs) {
        int outputLen = signal.length / 2;

        // Haar scaling factor
        final double SQRT2_INV = 0.7071067811865476;
        DoubleVector scale = DoubleVector.broadcast(SPECIES, SQRT2_INV);

        int i = 0;
        int vectorBound = outputLen - VECTOR_LENGTH;

        // Process VECTOR_LENGTH outputs at a time
        for (; i <= vectorBound; i += VECTOR_LENGTH) {
            // Load pairs of values manually
            double[] evenValues = new double[VECTOR_LENGTH];
            double[] oddValues = new double[VECTOR_LENGTH];

            for (int v = 0; v < VECTOR_LENGTH; v++) {
                evenValues[v] = signal[2 * (i + v)];
                oddValues[v] = signal[2 * (i + v) + 1];
            }

            DoubleVector even = DoubleVector.fromArray(SPECIES, evenValues, 0);
            DoubleVector odd = DoubleVector.fromArray(SPECIES, oddValues, 0);

            // Compute Haar transform
            DoubleVector sum = even.add(odd).mul(scale);
            DoubleVector diff = even.sub(odd).mul(scale);

            sum.intoArray(approxCoeffs, i);
            diff.intoArray(detailCoeffs, i);
        }

        // Scalar remainder
        for (; i < outputLen; i++) {
            double even = signal[2 * i];
            double odd = signal[2 * i + 1];
            approxCoeffs[i] = (even + odd) * SQRT2_INV;
            detailCoeffs[i] = (even - odd) * SQRT2_INV;
        }
    }

    /**
     * Optimized upsampling with convolution using scatter operations.
     */
    public static double[] upsampleAndConvolvePeriodicOptimized(
            double[] coeffs, double[] filter, int coeffsLength, int filterLength) {

        int outputLength = coeffsLength * 2;
        double[] output = new double[outputLength];

        if (!VectorOps.isVectorizedOperationBeneficial(outputLength)) {
            return ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, coeffsLength, filterLength);
        }

        int outputMask = outputLength - 1;

        // Clear output array using vector operations
        int clearBound = outputLength - VECTOR_LENGTH;
        DoubleVector zeros = DoubleVector.zero(SPECIES);
        for (int i = 0; i <= clearBound; i += VECTOR_LENGTH) {
            zeros.intoArray(output, i);
        }
        // Clear remainder
        Arrays.fill(output, clearBound + VECTOR_LENGTH, outputLength, 0.0);

        // Process coefficients
        // Allocate scatter indices array on stack
        int[] scatterIndices = new int[VECTOR_LENGTH];

        // Process even indices (direct placement)
        for (int k = 0; k < filterLength; k += 2) {
            double filterCoeff = filter[k];
            DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filterCoeff);

            int i = 0;
            int vectorBound = coeffsLength - VECTOR_LENGTH;

            for (; i <= vectorBound; i += VECTOR_LENGTH) {
                DoubleVector coeffVec = DoubleVector.fromArray(SPECIES, coeffs, i);
                DoubleVector prod = coeffVec.mul(filterVec);

                // Compute scatter indices
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    scatterIndices[v] = (2 * (i + v) + k) & outputMask;
                }

                // Scatter to output
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    output[scatterIndices[v]] += prod.lane(v);
                }
            }

            // Scalar remainder
            for (; i < coeffsLength; i++) {
                int idx = (2 * i + k) & outputMask;
                output[idx] += coeffs[i] * filterCoeff;
            }
        }

        // Process odd indices
        for (int k = 1; k < filterLength; k += 2) {
            double filterCoeff = filter[k];
            DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filterCoeff);

            int i = 0;
            int vectorBound = coeffsLength - VECTOR_LENGTH;

            for (; i <= vectorBound; i += VECTOR_LENGTH) {
                DoubleVector coeffVec = DoubleVector.fromArray(SPECIES, coeffs, i);
                DoubleVector prod = coeffVec.mul(filterVec);

                // Compute scatter indices
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    scatterIndices[v] = (2 * (i + v) + k) & outputMask;
                }

                // Scatter to output
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    output[scatterIndices[v]] += prod.lane(v);
                }
            }

            // Scalar remainder
            for (; i < coeffsLength; i++) {
                int idx = (2 * i + k) & outputMask;
                output[idx] += coeffs[i] * filterCoeff;
            }
        }

        return output;
    }

    /**
     * Get information about the optimized Vector API implementation.
     */
    public static String getVectorInfo() {
        return String.format("Optimized Vector API: Species=%s, Length=%d, Enabled=%b",
                SPECIES, VECTOR_LENGTH, VECTOR_LENGTH > 1);
    }
}