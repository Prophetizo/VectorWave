package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the optimized power-of-2 calculation.
 */
class PowerOf2OptimizationTest {
    
    /**
     * Original loop-based implementation for comparison.
     */
    private static int nextPowerOf2Loop(int target) {
        int m = 1;
        while (m < target) {
            m <<= 1;
        }
        return m;
    }
    
    /**
     * Optimized bit operation implementation.
     */
    private static int nextPowerOf2Optimized(int target) {
        return target <= 1 ? 1 : Integer.highestOneBit(target - 1) << 1;
    }
    
    @Test
    @DisplayName("Optimized power-of-2 calculation should match loop-based approach")
    void testOptimizedVsLoop() {
        // Test with various values that would be used in Bluestein FFT
        int[] testValues = {
            1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65,
            127, 128, 129, 255, 256, 257, 511, 512, 513, 1023, 1024, 1025,
            2047, 2048, 2049, 4095, 4096, 4097
        };
        
        for (int n : testValues) {
            int target = 2 * n - 1; // This is what Bluestein algorithm needs
            
            int loopResult = nextPowerOf2Loop(target);
            int optimizedResult = nextPowerOf2Optimized(target);
            
            assertEquals(loopResult, optimizedResult, 
                "Mismatch for n=" + n + " (target=" + target + "): loop=" + loopResult + ", optimized=" + optimizedResult);
            
            // Verify the result is actually a power of 2
            assertTrue(isPowerOf2(optimizedResult), 
                "Result " + optimizedResult + " is not a power of 2");
            
            // Verify it's the smallest power of 2 >= target
            assertTrue(optimizedResult >= target, 
                "Result " + optimizedResult + " is less than target " + target);
            
            if (optimizedResult > 1) {
                assertTrue(optimizedResult / 2 < target, 
                    "Result " + optimizedResult + " is not the smallest power of 2 >= " + target);
            }
        }
    }
    
    @Test
    @DisplayName("Edge cases should be handled correctly")
    void testEdgeCases() {
        // Test edge cases
        assertEquals(1, nextPowerOf2Optimized(1));
        assertEquals(2, nextPowerOf2Optimized(2));
        assertEquals(4, nextPowerOf2Optimized(3));
        assertEquals(4, nextPowerOf2Optimized(4));
        assertEquals(8, nextPowerOf2Optimized(5));
        
        // Test some Bluestein-specific cases
        // For n=1: target = 2*1-1 = 1, should return 1
        assertEquals(1, nextPowerOf2Optimized(1));
        
        // For n=2: target = 2*2-1 = 3, should return 4
        assertEquals(4, nextPowerOf2Optimized(3));
        
        // For n=3: target = 2*3-1 = 5, should return 8
        assertEquals(8, nextPowerOf2Optimized(5));
    }
    
    @Test
    @DisplayName("Performance comparison between loop and bit operations")
    void testPerformanceCharacteristics() {
        // This is not a strict performance test, but demonstrates the optimization
        int iterations = 100000;
        int[] testValues = {15, 31, 63, 127, 255, 511, 1023, 2047, 4095};
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            for (int n : testValues) {
                int target = 2 * n - 1;
                nextPowerOf2Loop(target);
                nextPowerOf2Optimized(target);
            }
        }
        
        // Time loop approach
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int n : testValues) {
                int target = 2 * n - 1;
                nextPowerOf2Loop(target);
            }
        }
        long loopTime = System.nanoTime() - startTime;
        
        // Time optimized approach
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int n : testValues) {
                int target = 2 * n - 1;
                nextPowerOf2Optimized(target);
            }
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        System.out.println("Loop approach: " + (loopTime / 1_000_000.0) + " ms");
        System.out.println("Optimized approach: " + (optimizedTime / 1_000_000.0) + " ms");
        System.out.println("Speedup: " + (double) loopTime / optimizedTime + "x");
        
        // The optimized approach should be faster, especially for larger values
        // But we won't assert this since it depends on JVM optimizations
        assertTrue(optimizedTime > 0, "Optimized approach should complete");
        assertTrue(loopTime > 0, "Loop approach should complete");
    }
    
    @Test
    @DisplayName("Integration with Bluestein FFT should work correctly")
    void testBluesteinIntegration() {
        // Test that the optimization doesn't break Bluestein FFT
        // We'll test with odd-sized arrays that would use Bluestein
        
        int[] oddSizes = {3, 5, 7, 9, 11, 13, 15, 17, 19, 21};
        
        for (int n : oddSizes) {
            double[] data = new double[2 * n]; // Complex data
            
            // Fill with test data
            for (int i = 0; i < n; i++) {
                data[2 * i] = Math.cos(2 * Math.PI * i / n);     // Real
                data[2 * i + 1] = Math.sin(2 * Math.PI * i / n); // Imaginary
            }
            
            // This should not throw exceptions and should complete
            assertDoesNotThrow(() -> {
                OptimizedFFT.fftOptimized(data, n, false);
            }, "Bluestein FFT failed for size " + n);
            
            // Verify data is not all zeros (basic sanity check)
            boolean hasNonZero = false;
            for (double value : data) {
                if (Math.abs(value) > 1e-10) {
                    hasNonZero = true;
                    break;
                }
            }
            assertTrue(hasNonZero, "FFT result should not be all zeros for size " + n);
        }
    }
    
    private static boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}