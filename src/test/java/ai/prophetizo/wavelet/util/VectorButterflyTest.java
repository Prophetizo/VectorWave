package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.test.ComplexArrayTestUtils;
import ai.prophetizo.wavelet.test.TestConstants;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that vector butterfly operations are implemented correctly.
 * This test ensures that the vectorized FFT produces the same results as scalar FFT
 * while potentially providing performance benefits.
 * 
 * <p>This test uses the public API with TransformConfig to control scalar/vector paths,
 * ensuring proper encapsulation and avoiding tight coupling between test and implementation.</p>
 */
class VectorButterflyTest {
    
    private static final double TOLERANCE = TestConstants.DEFAULT_TOLERANCE;
    
    @Test
    @DisplayName("Vector butterfly operations should produce identical results to scalar")
    void testVectorButterflyCorrectness() {
        // Test with various power-of-2 sizes to exercise vector paths
        int[] testSizes = TestConstants.POWER_OF_TWO_SIZES;
        
        for (int size : testSizes) {
            // Create test signal
            double[] original = new double[2 * size]; // Interleaved complex
            for (int i = 0; i < size; i++) {
                original[2 * i] = Math.sin(2 * Math.PI * 3 * i / size) + 
                                 0.5 * Math.cos(2 * Math.PI * 7 * i / size);
                original[2 * i + 1] = 0.3 * Math.sin(2 * Math.PI * 2 * i / size);
            }
            
            // Create two copies
            double[] vectorData = original.clone();
            double[] scalarData = original.clone();
            
            // Use the public API with TransformConfig to control scalar/vector paths
            // Default behavior: use vectorization if available
            OptimizedFFT.fftOptimized(vectorData, size, false);
            
            // Force scalar path for comparison using TransformConfig
            TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
            OptimizedFFT.fftOptimized(scalarData, size, false, scalarConfig);
            
            // Results should be identical (or very close due to floating point precision)
            for (int i = 0; i < vectorData.length; i++) {
                assertEquals(vectorData[i], scalarData[i], TOLERANCE,
                    "Vector and scalar FFT results differ at index " + i + " for size " + size);
            }
            
            // Test inverse as well - apply inverse to the same data that had forward FFT applied
            OptimizedFFT.fftOptimized(vectorData, size, true);
            OptimizedFFT.fftOptimized(scalarData, size, true, scalarConfig);
            
            for (int i = 0; i < vectorData.length; i++) {
                assertEquals(vectorData[i], scalarData[i], TOLERANCE,
                    "Vector and scalar IFFT results differ at index " + i + " for size " + size);
            }
        }
    }
    
    @Test
    @DisplayName("Vector butterfly should handle edge cases correctly")
    void testVectorButterflyEdgeCases() {
        // Test minimum size where vector operations might be used
        double[] data = {1.0, 0.0, 0.0, 1.0}; // Size 2 complex numbers
        double[] original = data.clone();
        
        assertDoesNotThrow(() -> {
            OptimizedFFT.fftOptimized(data, 2, false);
            OptimizedFFT.fftOptimized(data, 2, true);
        });
        
        // Verify round-trip accuracy
        for (int i = 0; i < data.length; i++) {
            assertEquals(original[i], data[i], TOLERANCE,
                "Round-trip FFT failed at index " + i);
        }
    }
    
    @Test
    @DisplayName("Vector operations should maintain FFT properties")
    void testFFTProperties() {
        int size = 8; // Small size to debug easily
        double[] signal = new double[2 * size];
        
        // Create a simple known signal
        signal[0] = 1.0; // Real part
        signal[1] = 0.0; // Imaginary part
        signal[2] = 1.0; // Real part
        signal[3] = 0.0; // Imaginary part
        // Rest are zeros
        
        double[] original = signal.clone();
        
        // Test round-trip: FFT followed by IFFT should restore original
        OptimizedFFT.fftOptimized(signal, size, false);
        OptimizedFFT.fftOptimized(signal, size, true);
        
        for (int i = 0; i < signal.length; i++) {
            assertEquals(original[i], signal[i], TOLERANCE,
                "Round-trip FFT should restore original signal at index " + i);
        }
    }
    
    @Test
    @DisplayName("Performance comparison between vector and potential scalar fallback")
    void testPerformanceCharacteristics() {
        int size = 1024; // Large enough for meaningful timing
        int iterations = 1000;
        
        // Create test data with seeded Random for consistent performance tests
        Random random = new Random(TestConstants.TEST_SEED);
        double[] data = new double[2 * size];
        for (int i = 0; i < size; i++) {
            data[2 * i] = random.nextDouble();
            data[2 * i + 1] = random.nextDouble();
        }
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            double[] temp = data.clone();
            OptimizedFFT.fftOptimized(temp, size, false);
        }
        
        // Time the operations
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            double[] temp = data.clone();
            OptimizedFFT.fftOptimized(temp, size, false);
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
        
        // Verify it completes in reasonable time
        assertTrue(averageTimeMs < 10.0, 
            "FFT should complete in reasonable time, got: " + averageTimeMs + " ms");
        
        // Performance assertion to track regressions
        // This serves as documentation of expected performance without console output
        assertTrue(averageTimeMs < 1.0, 
            "FFT performance check: average time " + averageTimeMs + " ms (< 1.0 ms expected for size " + size + ")");
    }
}