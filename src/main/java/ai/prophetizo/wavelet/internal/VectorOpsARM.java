package ai.prophetizo.wavelet.internal;

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
     * Check if this is an ARM platform that can benefit from these optimizations.
     */
    public static boolean isARMPlatform() {
        String arch = System.getProperty("os.arch").toLowerCase();
        return arch.contains("aarch64") || arch.contains("arm");
    }

    /**
     * Check if running on Apple Silicon (M1/M2/M3).
     */
    public static boolean isAppleSilicon() {
        return isARMPlatform() &&
                System.getProperty("os.name").toLowerCase().contains("mac");
    }
}