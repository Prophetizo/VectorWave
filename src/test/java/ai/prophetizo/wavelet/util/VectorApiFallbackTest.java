package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.ComplexNumber;
import ai.prophetizo.wavelet.test.ComplexArrayTestUtils;
import ai.prophetizo.wavelet.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Vector API fallback functionality in OptimizedFFT.
 * Ensures that the FFT implementation works correctly with and without Vector API.
 */
@DisplayName("Vector API Fallback Tests")
class VectorApiFallbackTest {
    
    private static final double EPSILON = TestConstants.DEFAULT_TOLERANCE;
    
    @Test
    @DisplayName("Vector API availability check should return consistent result")
    void testVectorApiAvailability() {
        // This should not throw and should return a consistent result
        boolean isAvailable = OptimizedFFT.isVectorApiAvailable();
        String info = OptimizedFFT.getVectorApiInfo();
        
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        if (isAvailable) {
            assertTrue(info.contains("Vector API available"));
            assertTrue(info.contains("vector length"));
        } else {
            assertTrue(info.contains("not available"));
            assertTrue(info.contains("scalar fallback"));
        }
        
        System.out.println("Test environment: " + info);
    }
    
    @Test
    @DisplayName("FFT should work regardless of Vector API availability")
    void testFFTWorksAlways() {
        double[] data = createComplexData(new double[]{1, 2, 3, 4, 5, 6, 7, 8});
        
        // This should work whether Vector API is available or not
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, 8, false));
        
        // Verify it actually did something (not just returned)
        // After FFT, the DC component should be the sum of all values
        double dcReal = data[0];
        double dcImag = data[1];
        assertEquals(36.0, dcReal, EPSILON); // 1+2+3+4+5+6+7+8 = 36
        assertEquals(0.0, dcImag, EPSILON);
    }
    
    @Test
    @DisplayName("Scalar implementation should be accessible and functional")
    void testScalarImplementationWorks() {
        // Test that we can force scalar implementation using TransformConfig
        double[] data = createComplexData(new double[]{1, 2, 3, 4, 5, 6, 7, 8});
        
        // Force scalar implementation even if Vector API is available
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, 8, false, scalarConfig));
        
        // Should have transformed the data (not all zeros)
        boolean hasNonZero = false;
        for (double v : data) {
            if (Math.abs(v) > EPSILON) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero, "Scalar FFT should produce non-zero results");
    }
    
    @Test
    @DisplayName("Real-optimized FFT should work with fallback")
    void testRealOptimizedFFTFallback() {
        double[] realData = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // This should work regardless of Vector API availability
        ComplexNumber[] result = assertDoesNotThrow(() -> 
            OptimizedFFT.fftRealOptimized(realData));
        
        assertNotNull(result);
        assertEquals(realData.length, result.length);
        
        // Verify DC component
        assertEquals(36.0, result[0].real(), EPSILON);
        assertEquals(0.0, result[0].imag(), EPSILON);
    }
    
    @ParameterizedTest
    @DisplayName("FFT should handle various sizes correctly")
    @ValueSource(ints = {1, 7, 15, 31, 100, 127})
    void testNonPowerOfTwoSizes(int size) {
        Random rand = new Random(TestConstants.TEST_SEED);
        double[] data = ComplexArrayTestUtils.createRandomRealComplex(size, rand);
        
        // Should use Bluestein algorithm for non-power-of-2 sizes
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, size, false));
        
        // Verify it's not all zeros or NaN
        assertTrue(ComplexArrayTestUtils.allFinite(data), "FFT should not produce NaN or Inf values");
        assertTrue(ComplexArrayTestUtils.hasNonZero(data, EPSILON), "FFT should produce non-zero results");
    }
    
    @Test
    @DisplayName("FFT operations should complete without exceptions")
    void testFFTOperationsComplete() {
        // Test various sizes to ensure no exceptions
        int[] sizes = {8, 16, 32, 64, 128};
        Random rand = new Random(TestConstants.TEST_SEED);
        
        for (int size : sizes) {
            double[] data = ComplexArrayTestUtils.createRandomRealComplex(size, rand);
            
            // Forward FFT should complete
            assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, size, false),
                "Forward FFT should not throw for size " + size);
            
            // Inverse FFT should complete
            assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, size, true),
                "Inverse FFT should not throw for size " + size);
        }
    }
    
    @Test
    @DisplayName("Split-radix FFT should work with fallback")
    void testSplitRadixFallback() {
        // Test size that triggers split-radix (>= 32)
        int size = 64;
        Random rand = new Random(TestConstants.TEST_SEED);
        double[] data = ComplexArrayTestUtils.createRandomRealComplex(size, rand);
        
        // This uses split-radix algorithm
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, size, false));
        
        // Verify it produced valid output (no NaN or Inf)
        assertTrue(ComplexArrayTestUtils.allFinite(data), "FFT should not produce NaN or Inf");
    }
    
    @Test
    @DisplayName("Vector API info should be accessible without exceptions")
    void testVectorApiInfoAccess() {
        // Multiple calls should return consistent results
        String info1 = OptimizedFFT.getVectorApiInfo();
        String info2 = OptimizedFFT.getVectorApiInfo();
        
        assertEquals(info1, info2, "Vector API info should be consistent");
        
        // Should be human-readable
        assertTrue(info1.length() > 10, "Info string should be meaningful");
        assertTrue(info1.contains("Vector API") || info1.contains("scalar"),
            "Info should mention Vector API or scalar");
    }
    
    @Test
    @DisplayName("Fallback should handle edge cases gracefully")
    void testEdgeCases() {
        // Test n=0 case (returns early without processing)
        double[] empty = new double[0];
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(empty, 0, false));
        
        // Test with non-empty array but n=0 (should return early)
        double[] nonEmpty = {1.0, 2.0, 3.0, 4.0};
        double[] original = nonEmpty.clone();
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(nonEmpty, 0, false));
        assertArrayEquals(original, nonEmpty, EPSILON, "Array should not be modified when n=0");
        
        // Single element (n=1, returns early without processing)
        double[] single = {1.0, 0.0};
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(single, 1, false));
        assertEquals(1.0, single[0], EPSILON);
        assertEquals(0.0, single[1], EPSILON);
        
        // Very small size (2)
        double[] small = {1.0, 0.0, 2.0, 0.0};
        assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(small, 2, false));
    }
    
    /**
     * Helper method to create complex array from real values.
     * Sets real parts to the provided values and imaginary parts to zero.
     */
    private double[] createComplexData(double[] real) {
        return ComplexArrayTestUtils.createComplexFromReal(real);
    }
}