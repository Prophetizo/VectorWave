package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool.PooledArray;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Structure-of-Arrays (SoA) wavelet transform implementation.
 *
 * <p>This implementation reorganizes data layout for optimal SIMD processing:
 * instead of Array-of-Structures (AoS) where each signal is stored separately,
 * SoA stores all first elements together, all second elements together, etc.</p>
 *
 * <p>Benefits:</p>
 * <ul>
 *   <li>Better SIMD utilization - process multiple signals in parallel</li>
 *   <li>Improved cache locality for batch operations</li>
 *   <li>Reduced memory bandwidth requirements</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class SoATransform {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();

    private SoATransform() {
        // Utility class
    }

    /**
     * Convert Array-of-Structures to Structure-of-Arrays layout.
     *
     * @param signals AoS layout: signals[signal_index][sample_index]
     * @return SoA layout: [all_sample_0, all_sample_1, ..., all_sample_n]
     */
    public static double[] convertAoSToSoA(double[][] signals) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        double[] soa = new double[numSignals * signalLength];

        // Transpose the data
        for (int sample = 0; sample < signalLength; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                soa[sample * numSignals + signal] = signals[signal][sample];
            }
        }

        return soa;
    }

    /**
     * Convert Structure-of-Arrays back to Array-of-Structures layout.
     */
    public static void convertSoAToAoS(double[] soa, double[][] signals) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;

        // Transpose back
        for (int sample = 0; sample < signalLength; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                signals[signal][sample] = soa[sample * numSignals + signal];
            }
        }
    }

    /**
     * Perform Haar wavelet transform on multiple signals in SoA layout.
     * Processes VECTOR_LENGTH signals simultaneously.
     */
    public static void haarTransformSoA(double[] soaInput, double[] soaApprox,
                                        double[] soaDetail, int numSignals, int signalLength) {
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);
        DoubleVector sqrt2Vec = DoubleVector.broadcast(SPECIES, SQRT2_INV);

        int halfLength = signalLength / 2;

        // Process all signals VECTOR_LENGTH at a time
        for (int sample = 0; sample < halfLength; sample++) {
            int evenIdx = 2 * sample * numSignals;
            int oddIdx = (2 * sample + 1) * numSignals;
            int outIdx = sample * numSignals;

            // Process VECTOR_LENGTH signals at once
            for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                int remaining = Math.min(VECTOR_LENGTH, numSignals - s);

                if (remaining == VECTOR_LENGTH) {
                    // Load even and odd samples for VECTOR_LENGTH signals
                    DoubleVector even = DoubleVector.fromArray(SPECIES, soaInput, evenIdx + s);
                    DoubleVector odd = DoubleVector.fromArray(SPECIES, soaInput, oddIdx + s);

                    // Compute approximation and detail
                    DoubleVector approx = even.add(odd).mul(sqrt2Vec);
                    DoubleVector detail = even.sub(odd).mul(sqrt2Vec);

                    // Store results
                    approx.intoArray(soaApprox, outIdx + s);
                    detail.intoArray(soaDetail, outIdx + s);
                } else {
                    // Handle partial vector
                    for (int v = 0; v < remaining; v++) {
                        double e = soaInput[evenIdx + s + v];
                        double o = soaInput[oddIdx + s + v];
                        soaApprox[outIdx + s + v] = (e + o) * SQRT2_INV;
                        soaDetail[outIdx + s + v] = (e - o) * SQRT2_INV;
                    }
                }
            }
        }
    }

    /**
     * Perform general wavelet transform on SoA data.
     * Optimized for processing multiple signals with the same filter.
     */
    public static void transformSoA(double[] soaInput, double[] soaApprox, double[] soaDetail,
                                    double[] lowPass, double[] highPass,
                                    int numSignals, int signalLength, int filterLength) {
        int halfLength = signalLength / 2;

        // Use pooled memory for temporary storage
        try (PooledArray pooledTemp = AlignedMemoryPool.allocate(numSignals * VECTOR_LENGTH)) {
            double[] temp = pooledTemp.array();

            // Process each output sample
            for (int outSample = 0; outSample < halfLength; outSample++) {
                int outIdx = outSample * numSignals;

                // Initialize output to zero
                for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                    int remaining = Math.min(VECTOR_LENGTH, numSignals - s);
                    DoubleVector zero = DoubleVector.zero(SPECIES);

                    if (remaining == VECTOR_LENGTH) {
                        zero.intoArray(soaApprox, outIdx + s);
                        zero.intoArray(soaDetail, outIdx + s);
                    } else {
                        for (int v = 0; v < remaining; v++) {
                            soaApprox[outIdx + s + v] = 0.0;
                            soaDetail[outIdx + s + v] = 0.0;
                        }
                    }
                }

                // Perform convolution
                for (int k = 0; k < filterLength; k++) {
                    int inSample = (2 * outSample + k) % signalLength;
                    int inIdx = inSample * numSignals;

                    DoubleVector lowCoeff = DoubleVector.broadcast(SPECIES, lowPass[k]);
                    DoubleVector highCoeff = DoubleVector.broadcast(SPECIES, highPass[k]);

                    // Process VECTOR_LENGTH signals at once
                    for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                        int remaining = Math.min(VECTOR_LENGTH, numSignals - s);

                        if (remaining == VECTOR_LENGTH) {
                            // Load input values
                            DoubleVector input = DoubleVector.fromArray(SPECIES, soaInput, inIdx + s);

                            // Load current sums
                            DoubleVector approxSum = DoubleVector.fromArray(SPECIES, soaApprox, outIdx + s);
                            DoubleVector detailSum = DoubleVector.fromArray(SPECIES, soaDetail, outIdx + s);

                            // Accumulate
                            approxSum = approxSum.add(input.mul(lowCoeff));
                            detailSum = detailSum.add(input.mul(highCoeff));

                            // Store back
                            approxSum.intoArray(soaApprox, outIdx + s);
                            detailSum.intoArray(soaDetail, outIdx + s);
                        } else {
                            // Handle partial vector
                            for (int v = 0; v < remaining; v++) {
                                double val = soaInput[inIdx + s + v];
                                soaApprox[outIdx + s + v] += val * lowPass[k];
                                soaDetail[outIdx + s + v] += val * highPass[k];
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Batch inverse transform in SoA layout.
     */
    public static void inverseTransformSoA(double[] soaApprox, double[] soaDetail,
                                           double[] soaOutput, double[] lowPass, double[] highPass,
                                           int numSignals, int outputLength, int filterLength) {
        int inputLength = outputLength / 2;

        // Clear output
        java.util.Arrays.fill(soaOutput, 0.0);

        // Process each output sample
        for (int outSample = 0; outSample < outputLength; outSample++) {
            int outIdx = outSample * numSignals;

            // Calculate contributing input samples
            for (int k = 0; k < filterLength; k++) {
                if ((outSample - k) >= 0 && (outSample - k) % 2 == 0) {
                    int inSample = (outSample - k) / 2;
                    if (inSample < inputLength) {
                        int inIdx = inSample * numSignals;

                        DoubleVector lowCoeff = DoubleVector.broadcast(SPECIES, lowPass[k]);
                        DoubleVector highCoeff = DoubleVector.broadcast(SPECIES, highPass[k]);

                        // Process VECTOR_LENGTH signals at once
                        for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                            int remaining = Math.min(VECTOR_LENGTH, numSignals - s);

                            if (remaining == VECTOR_LENGTH) {
                                // Load coefficients
                                DoubleVector approx = DoubleVector.fromArray(SPECIES, soaApprox, inIdx + s);
                                DoubleVector detail = DoubleVector.fromArray(SPECIES, soaDetail, inIdx + s);

                                // Load current output
                                DoubleVector output = DoubleVector.fromArray(SPECIES, soaOutput, outIdx + s);

                                // Accumulate
                                output = output.add(approx.mul(lowCoeff));
                                output = output.add(detail.mul(highCoeff));

                                // Store back
                                output.intoArray(soaOutput, outIdx + s);
                            } else {
                                // Handle partial vector
                                for (int v = 0; v < remaining; v++) {
                                    double a = soaApprox[inIdx + s + v];
                                    double d = soaDetail[inIdx + s + v];
                                    soaOutput[outIdx + s + v] += a * lowPass[k] + d * highPass[k];
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Optimized SoA transform for complex-valued signals.
     * Stores real and imaginary parts in separate arrays for better SIMD.
     */
    public static void transformComplexSoA(double[] realInput, double[] imagInput,
                                           double[] realApprox, double[] imagApprox,
                                           double[] realDetail, double[] imagDetail,
                                           double[] realFilter, double[] imagFilter,
                                           int numSignals, int signalLength, int filterLength) {
        int halfLength = signalLength / 2;

        // Process each output sample
        for (int outSample = 0; outSample < halfLength; outSample++) {
            int outIdx = outSample * numSignals;

            // Clear outputs
            for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                int remaining = Math.min(VECTOR_LENGTH, numSignals - s);
                if (remaining == VECTOR_LENGTH) {
                    DoubleVector zero = DoubleVector.zero(SPECIES);
                    zero.intoArray(realApprox, outIdx + s);
                    zero.intoArray(imagApprox, outIdx + s);
                    zero.intoArray(realDetail, outIdx + s);
                    zero.intoArray(imagDetail, outIdx + s);
                } else {
                    for (int v = 0; v < remaining; v++) {
                        realApprox[outIdx + s + v] = 0.0;
                        imagApprox[outIdx + s + v] = 0.0;
                        realDetail[outIdx + s + v] = 0.0;
                        imagDetail[outIdx + s + v] = 0.0;
                    }
                }
            }

            // Convolution with complex multiplication
            for (int k = 0; k < filterLength; k++) {
                int inSample = (2 * outSample + k) % signalLength;
                int inIdx = inSample * numSignals;

                // Broadcast filter coefficients
                DoubleVector realFilterVec = DoubleVector.broadcast(SPECIES, realFilter[k]);
                DoubleVector imagFilterVec = DoubleVector.broadcast(SPECIES, imagFilter[k]);

                // Process signals
                for (int s = 0; s < numSignals; s += VECTOR_LENGTH) {
                    int remaining = Math.min(VECTOR_LENGTH, numSignals - s);

                    if (remaining == VECTOR_LENGTH) {
                        // Load input
                        DoubleVector realIn = DoubleVector.fromArray(SPECIES, realInput, inIdx + s);
                        DoubleVector imagIn = DoubleVector.fromArray(SPECIES, imagInput, inIdx + s);

                        // Complex multiplication: (a + bi) * (c + di) = (ac - bd) + (ad + bc)i
                        DoubleVector realProd = realIn.mul(realFilterVec).sub(imagIn.mul(imagFilterVec));
                        DoubleVector imagProd = realIn.mul(imagFilterVec).add(imagIn.mul(realFilterVec));

                        // Accumulate to approximation
                        DoubleVector realApproxSum = DoubleVector.fromArray(SPECIES, realApprox, outIdx + s);
                        DoubleVector imagApproxSum = DoubleVector.fromArray(SPECIES, imagApprox, outIdx + s);
                        realApproxSum = realApproxSum.add(realProd);
                        imagApproxSum = imagApproxSum.add(imagProd);
                        realApproxSum.intoArray(realApprox, outIdx + s);
                        imagApproxSum.intoArray(imagApprox, outIdx + s);

                        // For detail, use QMF relation (simplified here)
                        DoubleVector realDetailSum = DoubleVector.fromArray(SPECIES, realDetail, outIdx + s);
                        DoubleVector imagDetailSum = DoubleVector.fromArray(SPECIES, imagDetail, outIdx + s);
                        realDetailSum = realDetailSum.add(realProd.neg());
                        imagDetailSum = imagDetailSum.add(imagProd.neg());
                        realDetailSum.intoArray(realDetail, outIdx + s);
                        imagDetailSum.intoArray(imagDetail, outIdx + s);
                    } else {
                        // Handle partial vector
                        for (int v = 0; v < remaining; v++) {
                            double realVal = realInput[inIdx + s + v];
                            double imagVal = imagInput[inIdx + s + v];

                            // Complex multiplication
                            double realProd = realVal * realFilter[k] - imagVal * imagFilter[k];
                            double imagProd = realVal * imagFilter[k] + imagVal * realFilter[k];

                            realApprox[outIdx + s + v] += realProd;
                            imagApprox[outIdx + s + v] += imagProd;
                            realDetail[outIdx + s + v] -= realProd;
                            imagDetail[outIdx + s + v] -= imagProd;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get SoA layout information.
     */
    public static String getSoAInfo() {
        return String.format(
                "Structure-of-Arrays Configuration:%n" +
                        "Vector Length: %d doubles%n" +
                        "Optimal batch size: %d signals (for L1 cache)%n" +
                        "Memory layout: Transposed for SIMD efficiency",
                VECTOR_LENGTH,
                VECTOR_LENGTH * 4  // Process 4x vector length for good cache usage
        );
    }
}