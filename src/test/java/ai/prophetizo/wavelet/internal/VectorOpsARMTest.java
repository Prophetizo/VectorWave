package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VectorOpsARMTest {
    
    private static final double EPSILON = 1e-10;
    private double[] haarFilter;
    private double[] db2Filter;
    private double[] db4Filter;
    
    @BeforeEach
    void setUp() {
        haarFilter = new Haar().lowPassDecomposition();
        db2Filter = new double[]{0.48296291314453414, 0.83651630373780772, 
                                 0.22414386804201339, -0.12940952255126037};
        db4Filter = Daubechies.DB4.lowPassDecomposition();
    }
    
    @Test
    void testHaarTransformARM() {
        double[] signal = createTestSignal(64);
        double[] approx = new double[32];
        double[] detail = new double[32];
        
        VectorOpsARM.haarTransformARM(signal, approx, detail, 64);
        
        // Verify Haar transform manually
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        for (int i = 0; i < 32; i++) {
            double expected_approx = (signal[2 * i] + signal[2 * i + 1]) * sqrt2Inv;
            double expected_detail = (signal[2 * i] - signal[2 * i + 1]) * sqrt2Inv;
            
            assertEquals(expected_approx, approx[i], EPSILON);
            assertEquals(expected_detail, detail[i], EPSILON);
        }
    }
    
    @Test
    void testDB2TransformARMLowPass() {
        double[] signal = createTestSignal(128);
        
        double[] result = VectorOpsARM.db2TransformARM(signal, 128, true);
        
        assertEquals(64, result.length);
        
        // Compare with standard convolution
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, db2Filter, 128, 4);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testDB2TransformARMHighPass() {
        double[] signal = createTestSignal(64);
        
        // Get high-pass result
        double[] result = VectorOpsARM.db2TransformARM(signal, 64, false);
        
        assertEquals(32, result.length);
        
        // Create expected high-pass filter (QMF relation)
        double[] highPassFilter = new double[]{
            -0.12940952255126037,   // h3
            -0.22414386804201339,   // -h2
            0.83651630373780772,    // h1
            -0.48296291314453414    // -h0
        };
        
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, highPassFilter, 64, 4);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testConvolveAndDownsampleARM() {
        double[] signal = createTestSignal(256);
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, db4Filter, 256, db4Filter.length);
        
        assertEquals(128, result.length);
        
        // Compare with standard implementation
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, db4Filter, 256, db4Filter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testConvolveAndDownsampleARMHaarOptimized() {
        // Test the specialized Haar path
        double[] signal = createTestSignal(128);
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, haarFilter, 128, 2);
        
        assertEquals(64, result.length);
        
        // Verify against standard implementation
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, 128, 2);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testConvolveAndDownsampleARMDB2Optimized() {
        // Test the specialized DB2 path
        double[] signal = createTestSignal(256);
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, db2Filter, 256, 4);
        
        assertEquals(128, result.length);
        
        // Verify against standard implementation
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, db2Filter, 256, 4);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256, 512})
    void testVariousSignalSizes(int size) {
        double[] signal = createTestSignal(size);
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, db4Filter, size, db4Filter.length);
        
        assertEquals(size / 2, result.length);
        
        // Verify non-trivial result
        assertTrue(hasSignificantValues(result));
    }
    
    @Test
    void testOddOutputLength() {
        // Test with signal size that produces odd output length
        double[] signal = createTestSignal(126); // Results in 63 outputs
        
        double[] result = VectorOpsARM.db2TransformARM(signal, 126, true);
        
        assertEquals(63, result.length);
        
        // Verify last element is computed correctly
        assertTrue(Math.abs(result[62]) > EPSILON);
    }
    
    @Test
    void testBoundaryConditions() {
        // Test with small signal to verify boundary wrapping
        double[] signal = new double[8];
        for (int i = 0; i < 8; i++) {
            signal[i] = i + 1.0; // 1, 2, 3, 4, 5, 6, 7, 8
        }
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, haarFilter, 8, 2);
        
        assertEquals(4, result.length);
        
        // Check boundary wrapping
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, 8, 2);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testIsARMPlatform() {
        boolean isARM = VectorOpsARM.isARMPlatform();
        // Just verify the method works without throwing
        assertNotNull(isARM);
    }
    
    @Test
    void testIsAppleSilicon() {
        boolean isAppleSilicon = VectorOpsARM.isAppleSilicon();
        // Just verify the method works without throwing
        assertNotNull(isAppleSilicon);
    }
    
    @Test
    void testNullInputValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            VectorOpsARM.convolveAndDownsampleARM(null, haarFilter, 64, 2)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            VectorOpsARM.convolveAndDownsampleARM(new double[64], null, 64, 2)
        );
    }
    
    @Test
    void testLargeFilterLength() {
        // Test with filter longer than 4 to use general case
        double[] signal = createTestSignal(128);
        double[] longFilter = new double[]{0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.05};
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, longFilter, 128, 7);
        
        assertEquals(64, result.length);
        
        // Verify correctness
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, longFilter, 128, 7);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testUnrolledHaarPerformance() {
        // Test the unrolled Haar implementation with various sizes
        double[] signal = createTestSignal(256);
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, haarFilter, 256, 2);
        
        assertEquals(128, result.length);
        
        // Verify all elements are computed
        for (int i = 0; i < result.length; i++) {
            assertTrue(Double.isFinite(result[i]));
        }
    }
    
    @Test
    void testSmallSignalHaar() {
        // Test Haar with minimal signal size
        double[] signal = new double[]{1.0, 2.0, 3.0, 4.0};
        double[] approx = new double[2];
        double[] detail = new double[2];
        
        VectorOpsARM.haarTransformARM(signal, approx, detail, 4);
        
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        assertEquals(3.0 * sqrt2Inv, approx[0], EPSILON); // (1+2)/sqrt(2)
        assertEquals(7.0 * sqrt2Inv, approx[1], EPSILON); // (3+4)/sqrt(2)
        assertEquals(-1.0 * sqrt2Inv, detail[0], EPSILON); // (1-2)/sqrt(2)
        assertEquals(-1.0 * sqrt2Inv, detail[1], EPSILON); // (3-4)/sqrt(2)
    }
    
    @Test
    void testEnergyPreservation() {
        double[] signal = createTestSignal(128);
        double[] approx = new double[64];
        double[] detail = new double[64];
        
        VectorOpsARM.haarTransformARM(signal, approx, detail, 128);
        
        // Calculate energies
        double signalEnergy = 0.0;
        double transformEnergy = 0.0;
        
        for (double val : signal) {
            signalEnergy += val * val;
        }
        
        for (double val : approx) {
            transformEnergy += val * val;
        }
        for (double val : detail) {
            transformEnergy += val * val;
        }
        
        // Haar transform preserves energy
        assertEquals(signalEnergy, transformEnergy, signalEnergy * 0.001);
    }
    
    // Helper methods
    
    private double[] createTestSignal(int length) {
        Random random = new Random(12345);
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 16.0) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}