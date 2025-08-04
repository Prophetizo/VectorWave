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
     * Checks if Vector API is supported on this platform.
     * @return true if Vector API is available and functional
     */
    public static boolean isVectorApiSupported() {
        try {
            // Test basic Vector API functionality
            var species = DoubleVector.SPECIES_PREFERRED;
            var vector = DoubleVector.zero(species);
            return vector != null && species.length() > 1;
        } catch (Throwable e) {
            return false;
        }
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
        int vectorLoopBound = outputLength - (outputLength % VECTOR_LENGTH);

        for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
        int vectorLoopBound = outputLength - (outputLength % VECTOR_LENGTH);

        for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
        int vectorLoopBound = signalLength - (signalLength % VECTOR_LENGTH);

        for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
        for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
            int vectorLoopBound = length - (length % VECTOR_LENGTH);

            for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
            int vectorLoopBound = length - (length % VECTOR_LENGTH);

            for (; i < vectorLoopBound; i += VECTOR_LENGTH) {
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
    
    /**
     * Vectorized circular convolution for MODWT.
     * 
     * Implements the MODWT convolution formula: W_j,t = Σ_{l=0}^{L-1} h_j,l * X_{t-l mod N}
     * This uses (t - l) indexing to implement time-reversed filter convolution, which is
     * standard in wavelet analysis for maintaining causality and proper time-ordering.
     * 
     * @param signal The input signal
     * @param filter The filter coefficients
     * @param output The output array
     * @see ScalarOps#circularConvolveMODWTScalar for mathematical justification
     */
    public static void circularConvolveMODWTVectorized(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        // For smaller arrays, fall back to scalar implementation
        if (!isVectorizedOperationBeneficial(signalLen)) {
            circularConvolveMODWTScalar(signal, filter, output);
            return;
        }
        
        // Clear output array
        clearArrayVectorized(output);
        
        // Pre-allocate indices array for reuse
        int[] indices = new int[VECTOR_LENGTH];
        
        // Calculate vector loop bound once, outside the filter loop
        int vectorLoopBound = SPECIES.loopBound(signalLen);
        
        for (int l = 0; l < filterLen; l++) {
            if (filter[l] == 0.0) continue; // Skip zero coefficients
            
            DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[l]);
            
            // Check if we can use direct vectorization without circular indexing
            // For MODWT with time-reversed indexing, we need t - l >= 0 for all t in [l, vectorLoopBound + l)
            if (l == 0) {
                // Special case: no shift needed
                for (int t = 0; t < vectorLoopBound; t += VECTOR_LENGTH) {
                    DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signal, t);
                    DoubleVector outputVec = DoubleVector.fromArray(SPECIES, output, t);
                    DoubleVector result = outputVec.add(signalVec.mul(filterVec));
                    result.intoArray(output, t);
                }
            } else {
                // Complex case: need to handle circular indexing
                // Most indices won't wrap, so we can optimize by checking boundaries
                for (int t = 0; t < vectorLoopBound; t += VECTOR_LENGTH) {
                    int startIdx = t - l;
                    int endIdx = startIdx + VECTOR_LENGTH;
                    
                    if (startIdx >= 0 && endIdx <= signalLen) {
                        // No wrapping needed for this vector
                        DoubleVector signalVec = DoubleVector.fromArray(SPECIES, signal, startIdx);
                        DoubleVector outputVec = DoubleVector.fromArray(SPECIES, output, t);
                        DoubleVector result = outputVec.add(signalVec.mul(filterVec));
                        result.intoArray(output, t);
                    } else {
                        // Complex wrapping case - fall back to scalar processing for this vector
                        // This is more efficient than lane-by-lane vector manipulation
                        for (int i = 0; i < VECTOR_LENGTH && t + i < signalLen; i++) {
                            int idx = t + i - l;
                            int signalIndex = idx >= 0 ? idx : idx + signalLen;
                            // Handle positive wrapping too
                            if (signalIndex >= signalLen) {
                                signalIndex -= signalLen;
                            }
                            
                            // Direct scalar accumulation - faster than vector lane manipulation
                            output[t + i] += signal[signalIndex] * filter[l];
                        }
                    }
                }
            }
            
            // Handle remainder
            for (int t = vectorLoopBound; t < signalLen; t++) {
                // Circular indexing optimization: when idx >= 0, no wrapping needed
                int idx = t - l;
                int signalIndex = idx >= 0 ? idx : idx + signalLen;
                output[t] += signal[signalIndex] * filter[l];
            }
        }
    }
    
    /**
     * Scalar fallback for circular convolution.
     * Uses the same (t - l) indexing as the main ScalarOps implementation
     * to ensure consistent results between vectorized and scalar paths.
     */
    private static void circularConvolveMODWTScalar(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // Use (t - l) indexing to match MODWT time-reversed filter convention
                // Optimize modulo operation: when t >= l, no wrapping needed
                int idx = t - l;
                int signalIndex = idx >= 0 ? idx : idx + signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            
            output[t] = sum;
        }
    }
    
    /**
     * Selects the optimal processing strategy based on signal characteristics.
     * 
     * @param signalLength The length of the signal to process
     * @param filterLength The length of the filter
     * @return The recommended processing strategy
     */
    public static ProcessingStrategy selectOptimalStrategy(int signalLength, int filterLength) {
        if (signalLength < MIN_VECTOR_LENGTH) {
            return ProcessingStrategy.SCALAR_OPTIMIZED;
        } else if (signalLength >= MIN_VECTOR_LENGTH && isPowerOfTwo(signalLength)) {
            return ProcessingStrategy.VECTORIZED_POWER_OF_TWO;
        } else if (signalLength >= MIN_VECTOR_LENGTH) {
            return ProcessingStrategy.VECTORIZED_GENERAL;
        } else {
            return ProcessingStrategy.SCALAR_FALLBACK;
        }
    }
    
    /**
     * Checks if a number is a power of two.
     */
    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Processing strategy enumeration using modern Java patterns.
     */
    public enum ProcessingStrategy {
        SCALAR_OPTIMIZED("Scalar with loop unrolling"),
        SCALAR_FALLBACK("Basic scalar implementation"),
        VECTORIZED_POWER_OF_TWO("Vectorized with bit-shift optimization"),
        VECTORIZED_GENERAL("Vectorized with standard modulo");
        
        private final String description;
        
        ProcessingStrategy(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Efficiently clear an array using vectorized operations.
     * 
     * @param array The array to clear
     */
    public static void clearArrayVectorized(double[] array) {
        int vectorLoopBound = SPECIES.loopBound(array.length);
        var zeroVector = DoubleVector.zero(SPECIES);
        
        // Vectorized clearing
        for (int i = 0; i < vectorLoopBound; i += VECTOR_LENGTH) {
            zeroVector.intoArray(array, i);
        }
        
        // Clear remainder elements
        for (int i = vectorLoopBound; i < array.length; i++) {
            array[i] = 0.0;
        }
    }
    
    /**
     * Gets information about the vector capabilities of the current system.
     * Useful for performance monitoring and optimization decisions.
     * 
     * @return Vector capability information
     */
    public static VectorCapabilityInfo getVectorCapabilities() {
        return new VectorCapabilityInfo(
            SPECIES.vectorShape().toString(),
            VECTOR_LENGTH,
            SPECIES.elementType().getSimpleName(),
            MIN_VECTOR_LENGTH
        );
    }
    
    /**
     * Record containing vector capability information.
     * 
     * @param shape The vector shape (e.g., "S_128_BIT", "S_256_BIT", "S_512_BIT")
     * @param length The number of elements per vector
     * @param elementType The element type (e.g., "Double")
     * @param threshold The minimum array size for vectorization
     */
    public record VectorCapabilityInfo(
        String shape,
        int length,
        String elementType,
        int threshold
    ) {
        
        /**
         * Returns a human-readable description of the vector capabilities.
         */
        public String description() {
            return shape + " with " + length + " " + elementType + " elements";
        }
        
        /**
         * Estimates performance improvement for a given array size.
         * 
         * @param arraySize The size of arrays being processed
         * @return Estimated speedup factor (1.0 = no speedup)
         */
        public double estimatedSpeedup(int arraySize) {
            if (arraySize < threshold) {
                return 1.0;  // No speedup for small arrays
            } else if (arraySize < threshold * 4) {
                return 2.0 + (arraySize - threshold) * 0.001;  // Gradual improvement
            } else if (arraySize < threshold * 16) {
                return 4.0 + (arraySize - threshold * 4) * 0.0005;  // Better improvement
            } else {
                return Math.min(8.0, 6.0 + (arraySize - threshold * 16) * 0.0001);  // Cap at 8x
            }
        }
    }
}