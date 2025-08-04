package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.util.PlatformDetector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * ARM/Apple Silicon optimized wavelet operations.
 *
 * <p>This class provides specialized implementations for ARM NEON (128-bit) vectors,
 * which have only 2 double-precision elements. These optimizations are crucial for
 * Apple M1/M2/M3 processors.</p>
 *
 * <p>Key optimizations:</p>
 * <ul>
 *   <li>Manually unrolled loops for 2-element vectors</li>
 *   <li>Reduced temporary allocations</li>
 *   <li>Direct array access patterns optimized for ARM</li>
 *   <li>Specialized kernels for common filter sizes</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class VectorOpsARM {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_128;
    private static final int VECTOR_LENGTH = 2; // ARM NEON has 128-bit vectors = 2 doubles

    private VectorOpsARM() {
        // Utility class
    }

    /**
     * Optimized Haar transform for ARM - the simplest and most common wavelet.
     */
    public static void haarTransformARM(double[] signal, double[] approx, double[] detail, int length) {
        // Haar coefficients
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);

        // Process pairs of elements
        for (int i = 0; i < length; i += 2) {
            double a = signal[i];
            double b = signal[i + 1];

            // Haar transform: sum and difference
            approx[i / 2] = (a + b) * SQRT2_INV;
            detail[i / 2] = (a - b) * SQRT2_INV;
        }
    }

    /**
     * Optimized DB2 transform for ARM.
     */
    public static double[] db2TransformARM(double[] signal, int signalLength, boolean lowPass) {
        // DB2 coefficients
        final double h0 = 0.48296291314453414;
        final double h1 = 0.83651630373780772;
        final double h2 = 0.22414386804201339;
        final double h3 = -0.12940952255126037;

        // Detail coefficients (QMF)
        final double g0 = h3;
        final double g1 = -h2;
        final double g2 = h1;
        final double g3 = -h0;

        double[] filter = lowPass ? new double[]{h0, h1, h2, h3} : new double[]{g0, g1, g2, g3};
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // Optimized for 2-element vectors
        DoubleVector filter01 = DoubleVector.fromArray(SPECIES, filter, 0);
        DoubleVector filter23 = DoubleVector.fromArray(SPECIES, filter, 2);

        // Process in pairs (utilizing 2-element vectors)
        for (int i = 0; i < outputLength - 1; i += 2) {
            // Load 6 signal values needed for 2 outputs
            int idx0 = (2 * i) % signalLength;
            int idx1 = (2 * i + 1) % signalLength;
            int idx2 = (2 * i + 2) % signalLength;
            int idx3 = (2 * i + 3) % signalLength;
            int idx4 = (2 * i + 4) % signalLength;
            int idx5 = (2 * i + 5) % signalLength;

            // First output: convolve signal[2i:2i+3] with filter
            DoubleVector sig01 = DoubleVector.fromArray(SPECIES,
                    new double[]{signal[idx0], signal[idx1]}, 0);
            DoubleVector sig23 = DoubleVector.fromArray(SPECIES,
                    new double[]{signal[idx2], signal[idx3]}, 0);

            double out0 = sig01.mul(filter01).reduceLanes(VectorOperators.ADD) +
                    sig23.mul(filter23).reduceLanes(VectorOperators.ADD);

            // Second output: convolve signal[2i+2:2i+5] with filter
            DoubleVector sig23_2 = DoubleVector.fromArray(SPECIES,
                    new double[]{signal[idx2], signal[idx3]}, 0);
            DoubleVector sig45 = DoubleVector.fromArray(SPECIES,
                    new double[]{signal[idx4], signal[idx5]}, 0);

            double out1 = sig23_2.mul(filter01).reduceLanes(VectorOperators.ADD) +
                    sig45.mul(filter23).reduceLanes(VectorOperators.ADD);

            output[i] = out0;
            output[i + 1] = out1;
        }

        // Handle last element if outputLength is odd
        if (outputLength % 2 == 1) {
            int i = outputLength - 1;
            double sum = 0.0;
            for (int k = 0; k < 4; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Specialized convolution for 2-element vectors (ARM NEON).
     * This version minimizes temporary allocations and uses direct indexing.
     */
    public static double[] convolveAndDownsampleARM(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        // Null checks
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // For very small filters (Haar, DB2), use specialized implementations
        if (filterLength == 2) {
            return convolveAndDownsampleHaarARM(signal, filter, signalLength);
        } else if (filterLength == 4) {
            return convolveAndDownsampleDB2ARM(signal, filter, signalLength);
        }

        // General case for arbitrary filter lengths
        // Process in pairs to utilize 2-element vectors
        for (int i = 0; i < outputLength - 1; i += 2) {
            DoubleVector sum0 = DoubleVector.zero(SPECIES);
            DoubleVector sum1 = DoubleVector.zero(SPECIES);

            // Process filter coefficients in pairs
            for (int k = 0; k < filterLength - 1; k += 2) {
                // Indices for first output
                int idx00 = (2 * i + k) & (signalLength - 1);
                int idx01 = (2 * i + k + 1) & (signalLength - 1);

                // Indices for second output
                int idx10 = (2 * (i + 1) + k) & (signalLength - 1);
                int idx11 = (2 * (i + 1) + k + 1) & (signalLength - 1);

                // Load filter coefficients
                DoubleVector filterVec = DoubleVector.fromArray(SPECIES, filter, k);

                // Load and accumulate for both outputs
                DoubleVector sig0 = DoubleVector.fromArray(SPECIES,
                        new double[]{signal[idx00], signal[idx01]}, 0);
                DoubleVector sig1 = DoubleVector.fromArray(SPECIES,
                        new double[]{signal[idx10], signal[idx11]}, 0);

                sum0 = sum0.add(filterVec.mul(sig0));
                sum1 = sum1.add(filterVec.mul(sig1));
            }

            // Handle odd filter length
            if (filterLength % 2 == 1) {
                int k = filterLength - 1;
                double fk = filter[k];
                int idx0 = (2 * i + k) & (signalLength - 1);
                int idx1 = (2 * (i + 1) + k) & (signalLength - 1);

                output[i] = sum0.reduceLanes(VectorOperators.ADD) + fk * signal[idx0];
                output[i + 1] = sum1.reduceLanes(VectorOperators.ADD) + fk * signal[idx1];
            } else {
                output[i] = sum0.reduceLanes(VectorOperators.ADD);
                output[i + 1] = sum1.reduceLanes(VectorOperators.ADD);
            }
        }

        // Handle last element if outputLength is odd
        if (outputLength % 2 == 1) {
            int i = outputLength - 1;
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
     * Specialized Haar convolution for ARM (2 coefficients).
     */
    private static double[] convolveAndDownsampleHaarARM(
            double[] signal, double[] filter, int signalLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        double f0 = filter[0];
        double f1 = filter[1];

        // Unrolled by 4 for better performance
        int i = 0;
        for (; i < outputLength - 3; i += 4) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            int idx2 = (2 * (i + 1)) & (signalLength - 1);
            int idx3 = (2 * (i + 1) + 1) & (signalLength - 1);
            int idx4 = (2 * (i + 2)) & (signalLength - 1);
            int idx5 = (2 * (i + 2) + 1) & (signalLength - 1);
            int idx6 = (2 * (i + 3)) & (signalLength - 1);
            int idx7 = (2 * (i + 3) + 1) & (signalLength - 1);

            output[i] = f0 * signal[idx0] + f1 * signal[idx1];
            output[i + 1] = f0 * signal[idx2] + f1 * signal[idx3];
            output[i + 2] = f0 * signal[idx4] + f1 * signal[idx5];
            output[i + 3] = f0 * signal[idx6] + f1 * signal[idx7];
        }

        // Handle remainder
        for (; i < outputLength; i++) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            output[i] = f0 * signal[idx0] + f1 * signal[idx1];
        }

        return output;
    }

    /**
     * Specialized DB2 convolution for ARM (4 coefficients).
     */
    private static double[] convolveAndDownsampleDB2ARM(
            double[] signal, double[] filter, int signalLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        double f0 = filter[0], f1 = filter[1], f2 = filter[2], f3 = filter[3];

        // Process in pairs for better vector utilization
        for (int i = 0; i < outputLength; i++) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            int idx2 = (2 * i + 2) & (signalLength - 1);
            int idx3 = (2 * i + 3) & (signalLength - 1);

            output[i] = f0 * signal[idx0] + f1 * signal[idx1] +
                    f2 * signal[idx2] + f3 * signal[idx3];
        }

        return output;
    }

    /**
     * ARM-optimized upsampling with convolution for periodic boundary mode.
     * 
     * <p>This implementation is optimized for ARM NEON 128-bit vectors (2 doubles)
     * and includes specific optimizations for Apple Silicon processors.</p>
     */
    public static double[] upsampleAndConvolvePeriodicARM(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        // Null checks
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }

        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];

        // For small signals, use scalar operations
        if (signalLength < 4) {
            return upsampleAndConvolvePeriodicScalar(signal, filter, signalLength, filterLength);
        }

        // Specialized path for Haar wavelet (most common)
        if (filterLength == 2) {
            return upsampleAndConvolveHaarPeriodicARM(signal, filter, signalLength);
        }

        // Process even indices (upsampled signal placement)
        // ARM optimization: process in pairs to utilize 2-element vectors
        for (int i = 0; i < signalLength - 1; i += 2) {
            DoubleVector sum0 = DoubleVector.zero(SPECIES);
            DoubleVector sum1 = DoubleVector.zero(SPECIES);

            // Process filter coefficients that apply to upsampled positions
            for (int k = 0; k < filterLength; k += 2) {
                if (k % 2 == 0) { // Even filter indices apply to even output positions
                    int idx0 = (i - k / 2 + signalLength) % signalLength;
                    int idx1 = ((i + 1) - k / 2 + signalLength) % signalLength;

                    // Load signal values
                    DoubleVector signalVec = DoubleVector.fromArray(SPECIES,
                            new double[]{signal[idx0], signal[idx1]}, 0);
                    DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[k]);

                    sum0 = sum0.add(signalVec.mul(filterVec));
                }
            }

            // Store results at even indices (2*i)
            output[2 * i] = sum0.lane(0);
            if (2 * (i + 1) < outputLength) {
                output[2 * (i + 1)] = sum0.lane(1);
            }
        }

        // Handle last element if signalLength is odd
        if (signalLength % 2 == 1) {
            int i = signalLength - 1;
            double sum = 0.0;
            for (int k = 0; k < filterLength; k += 2) {
                int idx = (i - k / 2 + signalLength) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[2 * i] = sum;
        }

        return output;
    }

    /**
     * ARM-optimized upsampling with convolution for zero-padding boundary mode.
     * 
     * <p>Zero-padding mode is simpler than periodic mode as it doesn't require
     * modulo operations, making it well-suited for ARM vectorization.</p>
     */
    public static double[] upsampleAndConvolveZeroPaddingARM(
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

        // For small signals, use scalar operations
        if (coeffsLength < 4) {
            return upsampleAndConvolveZeroPaddingScalar(coeffs, filter, coeffsLength, filterLength);
        }

        // Clear output array - ARM optimization: use vector zeros
        DoubleVector zeros = DoubleVector.zero(SPECIES);
        int clearBound = outputLength - VECTOR_LENGTH;
        for (int i = 0; i <= clearBound; i += VECTOR_LENGTH) {
            zeros.intoArray(output, i);
        }
        // Clear remainder
        for (int i = clearBound + VECTOR_LENGTH; i < outputLength; i++) {
            output[i] = 0.0;
        }

        // Process each coefficient in pairs for ARM vector efficiency
        for (int i = 0; i < coeffsLength - 1; i += 2) {
            double coeff0 = coeffs[i];
            double coeff1 = coeffs[i + 1];
            
            // Skip zero coefficients for efficiency
            if (coeff0 == 0.0 && coeff1 == 0.0) continue;

            int baseIndex0 = 2 * i;
            int baseIndex1 = 2 * (i + 1);

            DoubleVector coeffVec = DoubleVector.fromArray(SPECIES,
                    new double[]{coeff0, coeff1}, 0);

            // Process filter coefficients
            for (int j = 0; j < filterLength; j++) {
                int outputIndex0 = baseIndex0 + j;
                int outputIndex1 = baseIndex1 + j;

                if (outputIndex0 < outputLength && outputIndex1 < outputLength) {
                    DoubleVector filterVec = DoubleVector.broadcast(SPECIES, filter[j]);
                    DoubleVector contribution = coeffVec.mul(filterVec);
                    
                    output[outputIndex0] += contribution.lane(0);
                    output[outputIndex1] += contribution.lane(1);
                } else if (outputIndex0 < outputLength) {
                    output[outputIndex0] += coeff0 * filter[j];
                }
            }
        }

        // Handle last coefficient if coeffsLength is odd
        if (coeffsLength % 2 == 1) {
            int i = coeffsLength - 1;
            double coeff = coeffs[i];
            if (coeff != 0.0) {
                int baseIndex = 2 * i;
                for (int j = 0; j < filterLength; j++) {
                    int outputIndex = baseIndex + j;
                    if (outputIndex < outputLength) {
                        output[outputIndex] += coeff * filter[j];
                    }
                }
            }
        }

        return output;
    }

    /**
     * Specialized Haar upsampling for ARM (most common case).
     */
    private static double[] upsampleAndConvolveHaarPeriodicARM(
            double[] signal, double[] filter, int signalLength) {
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        double h0 = filter[0];
        double h1 = filter[1];
        
        // Process in pairs for ARM vector efficiency
        for (int i = 0; i < signalLength - 1; i += 2) {
            double s0 = signal[i];
            double s1 = signal[i + 1];
            
            // Even positions: direct filtering
            output[2 * i] = h0 * s0;
            output[2 * (i + 1)] = h0 * s1;
            
            // Odd positions: apply second filter coefficient
            output[2 * i + 1] = h1 * signal[(i + signalLength - 1) % signalLength];
            if (2 * (i + 1) + 1 < outputLength) {
                output[2 * (i + 1) + 1] = h1 * signal[i];
            }
        }
        
        // Handle last element if signalLength is odd
        if (signalLength % 2 == 1) {
            int i = signalLength - 1;
            output[2 * i] = h0 * signal[i];
            if (2 * i + 1 < outputLength) {
                output[2 * i + 1] = h1 * signal[(i + signalLength - 1) % signalLength];
            }
        }
        
        return output;
    }

    /**
     * Scalar fallback for small signals (periodic boundary).
     */
    private static double[] upsampleAndConvolvePeriodicScalar(
            double[] signal, double[] filter, int signalLength, int filterLength) {
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        // Standard upsampling with convolution
        for (int i = 0; i < signalLength; i++) {
            for (int k = 0; k < filterLength; k += 2) {
                int idx = (i - k / 2 + signalLength) % signalLength;
                output[2 * i] += filter[k] * signal[idx];
            }
        }
        
        return output;
    }

    /**
     * Scalar fallback for small signals (zero-padding boundary).
     */
    private static double[] upsampleAndConvolveZeroPaddingScalar(
            double[] coeffs, double[] filter, int coeffsLength, int filterLength) {
        
        int outputLength = coeffsLength * 2;
        double[] output = new double[outputLength];
        
        for (int i = 0; i < coeffsLength; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue;
            
            int baseIndex = 2 * i;
            for (int j = 0; j < filterLength; j++) {
                int outputIndex = baseIndex + j;
                if (outputIndex < outputLength) {
                    output[outputIndex] += coeff * filter[j];
                }
            }
        }
        
        return output;
    }

    /**
     * Check if this is an ARM platform that can benefit from these optimizations.
     */
    public static boolean isARMPlatform() {
        return PlatformDetector.isARM();
    }

    /**
     * Check if running on Apple Silicon (M1/M2/M3).
     */
    public static boolean isAppleSilicon() {
        return PlatformDetector.isAppleSilicon();
    }
}