package ai.prophetizo.wavelet.internal;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
class VectorOpsTest {

    private static final double EPSILON = 1e-10;
    private static final Random random = new Random(TestConstants.TEST_SEED);

    @Test
    void testIsVectorizedOperationBeneficial() {
        // The exact threshold depends on VECTOR_LENGTH which varies by platform
        VectorSpecies<Double> species = DoubleVector.SPECIES_PREFERRED;
        int minLength = species.length() * 4;
        
        // Should return false for signals smaller than minimum
        if (minLength > 4) {
            assertFalse(VectorOps.isVectorizedOperationBeneficial(4));
        }
        
        // Should return true for signals >= minimum threshold
        assertTrue(VectorOps.isVectorizedOperationBeneficial(minLength));
        assertTrue(VectorOps.isVectorizedOperationBeneficial(256));
        assertTrue(VectorOps.isVectorizedOperationBeneficial(1024));
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256})
    void testConvolveAndDownsamplePeriodic(int signalLength) {
        // Create test signal and filter
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Simple averaging filter
        double[] filter = {0.5, 0.5};
        
        // Perform convolution
        double[] result = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, signalLength, filter.length);
        
        // Verify result properties
        assertEquals(signalLength / 2, result.length);
        
        // Verify against scalar implementation
        double[] scalarResult = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signalLength, filter.length);
        
        assertArrayEquals(scalarResult, result, EPSILON);
    }

    @Test
    void testConvolveAndDownsamplePeriodicWithLargeFilter() {
        int signalLength = 64;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = i;
        }
        
        // Larger filter (e.g., Daubechies-4)
        double[] filter = {
            0.48296291314453416,
            0.8365163037378077,
            0.22414386804201338,
            -0.12940952255126037
        };
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, signalLength, filter.length);
        
        assertEquals(signalLength / 2, result.length);
        
        // Verify specific boundary handling (periodic)
        // The result should wrap around at boundaries
        assertNotEquals(0.0, result[0]); // Should include wrapped values
        assertNotEquals(0.0, result[result.length - 1]);
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256})
    void testConvolveAndDownsampleZeroPadding(int signalLength) {
        // Create test signal
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = 1.0; // Constant signal
        }
        
        // Simple filter
        double[] filter = {0.25, 0.5, 0.25};
        
        // Perform convolution
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(
            signal, filter, signalLength, filter.length);
        
        // Verify result properties
        assertEquals(signalLength / 2, result.length);
        
        // Verify against scalar implementation
        double[] scalarResult = ScalarOps.convolveAndDownsampleZeroPadding(
            signal, filter, signalLength, filter.length);
        
        assertArrayEquals(scalarResult, result, EPSILON);
        
        // Just verify the result is valid, don't make assumptions about specific values
    }

    @Test
    void testUpsampleAndConvolvePeriodic() {
        int signalLength = 32;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = i % 4; // Repeating pattern
        }
        
        double[] filter = {0.7071, 0.7071}; // Normalized filter
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(
            signal, filter, signalLength, filter.length);
        
        // Result should be twice the input length
        assertEquals(signalLength * 2, result.length);
        
        // Verify against scalar implementation
        double[] scalarResult = ScalarOps.upsampleAndConvolvePeriodic(
            signal, filter, signalLength, filter.length);
        
        assertArrayEquals(scalarResult, result, EPSILON);
    }

    @Test
    void testUpsampleAndConvolveZeroPadding() {
        int signalLength = 32;
        double[] signal = new double[signalLength];
        Arrays.fill(signal, 1.0);
        
        double[] filter = {0.5, 0.5};
        
        double[] result = VectorOps.upsampleAndConvolveZeroPadding(
            signal, filter, signalLength, filter.length);
        
        assertEquals(signalLength * 2, result.length);
        
        // Verify against scalar implementation
        double[] scalarResult = ScalarOps.upsampleAndConvolveZeroPadding(
            signal, filter, signalLength, filter.length);
        
        assertArrayEquals(scalarResult, result, EPSILON);
    }

    @Test
    void testGetVectorInfo() {
        // Test that vector info method returns useful information
        String info = VectorOps.getVectorInfo();
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        // Should contain information about the vector species
        assertTrue(info.contains("Vector") || info.contains("SPECIES"));
    }

    @Test
    void testVectorizedDenoisingOperations() {
        int length = 64;
        double[] signal = new double[length];
        double[] coefficients = new double[length];
        
        // Create test data
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8.0) + 0.1 * random.nextGaussian();
            coefficients[i] = signal[i]; // Copy for modification
        }
        
        double threshold = 0.1;
        
        // Test soft thresholding
        double[] softResult = VectorOps.Denoising.softThreshold(coefficients, threshold);
        
        // Verify soft thresholding behavior
        for (int i = 0; i < length; i++) {
            double original = signal[i];
            double thresholded = softResult[i];
            
            if (Math.abs(original) <= threshold) {
                assertEquals(0.0, thresholded, EPSILON);
            } else if (original > threshold) {
                assertEquals(original - threshold, thresholded, EPSILON);
            } else {
                assertEquals(original + threshold, thresholded, EPSILON);
            }
        }
        
        // Test hard thresholding
        double[] hardResult = VectorOps.Denoising.hardThreshold(coefficients, threshold);
        
        // Verify hard thresholding behavior
        for (int i = 0; i < length; i++) {
            double original = signal[i];
            double thresholded = hardResult[i];
            
            if (Math.abs(original) <= threshold) {
                assertEquals(0.0, thresholded, EPSILON);
            } else {
                assertEquals(original, thresholded, EPSILON);
            }
        }
    }


    @Test
    void testSmallSignalFallback() {
        // Test that small signals fall back to scalar operations
        int smallLength = 8; // Likely smaller than MIN_VECTOR_LENGTH
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.5};
        
        // This should use scalar implementation internally
        double[] result = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, smallLength, filter.length);
        
        assertEquals(4, result.length);
        
        // Verify correctness
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, smallLength, filter.length);
        assertArrayEquals(expected, result, EPSILON);
    }

    @Test
    void testEdgeCasesAndBoundaries() {
        // Test with minimum valid signal size
        double[] signal = new double[4];
        signal[0] = 1.0;
        signal[1] = 2.0;
        signal[2] = 3.0;
        signal[3] = 4.0;
        double[] filter = {1.0};
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, 4, 1);
        
        assertEquals(2, result.length);
        // With periodic boundary and filter [1.0], result should be [1.0, 3.0]
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        
    }

    @Test
    @EnabledIfSystemProperty(named = "jdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK", matches = "0|false|null")
    void testVectorSpeciesSelection() {
        // This test verifies that the vector species is properly selected
        VectorSpecies<Double> species = DoubleVector.SPECIES_PREFERRED;
        
        // Should be at least 2 (128-bit SIMD)
        assertTrue(species.length() >= 2);
        
        // On modern x86_64 with AVX2, should be 4 (256-bit)
        // On AVX-512 systems, could be 8 (512-bit)
        System.out.println("Vector species length: " + species.length());
        System.out.println("Vector species: " + species);
    }
}