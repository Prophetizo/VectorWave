package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify array bounds correctness in the fftRealOptimized implementation.
 * 
 * <p>This test analyzes the bounds of array accesses in OptimizedFFT.fftRealOptimized
 * to ensure that the packed array indexing is correct and doesn't cause out-of-bounds
 * exceptions. The fftRealOptimized method uses a packed array where even-indexed
 * real values are stored in the real parts and odd-indexed real values are stored
 * in the imaginary parts of a half-size complex array.</p>
 */
class FFTBoundsAnalysisTest {
    
    @Test
    @DisplayName("Verify array bounds in fftRealOptimized implementation")
    void testArrayBoundsInActualImplementation() {
        // Test with various even-sized arrays to check bounds
        int[] testSizes = {2, 4, 8, 16, 32, 64, 128};
        
        for (int n : testSizes) {
            System.out.println("Testing size n = " + n);
            double[] real = new double[n];
            
            // Fill with test data
            for (int i = 0; i < n; i++) {
                real[i] = Math.sin(2 * Math.PI * i / n);
            }
            
            // This should not throw any bounds exceptions
            assertDoesNotThrow(() -> {
                ComplexNumber[] result = OptimizedFFT.fftRealOptimized(real);
                assertEquals(n, result.length);
                
                // Verify the actual implementation handles bounds correctly
                // No additional assertions needed - the test passes if no exception is thrown
            }, "fftRealOptimized failed for size " + n);
        }
    }
    
    @Test
    @DisplayName("Analyze theoretical bounds of packed array indexing in fftRealOptimized")
    void analyzePackedArrayIndexing() {
        // This test analyzes the theoretical bounds of the packed array indexing
        // as implemented in OptimizedFFT.fftRealOptimized lines 528-547
        
        int[] testSizes = {4, 8, 16, 32};
        
        for (int n : testSizes) {
            System.out.println("\nAnalyzing packed array bounds for n=" + n);
            int halfN = n / 2;
            int packedArrayLength = 2 * halfN; // Complex array has 2 * halfN doubles
            
            System.out.println("  halfN = " + halfN);
            System.out.println("  packed array length = " + packedArrayLength);
            System.out.println("  Loop range: k from 1 to " + (halfN - 1) + " (exclusive)");
            
            // The critical insight: the loop MUST use k < halfN (not k <= halfN)
            // because when k = halfN, we get halfN - k = 0, which would cause
            // packed[2 * k] and packed[2 * (halfN - k)] to access different but
            // mathematically incorrect elements for the FFT symmetry
            
            // Verify array bounds for the actual loop indices
            for (int k = 1; k < halfN; k++) {
                // These are the actual array accesses from the implementation
                int idx1 = 2 * k;                    // packed[2 * k]
                int idx2 = 2 * (halfN - k);          // packed[2 * (halfN - k)]
                int idx3 = 2 * k + 1;                // packed[2 * k + 1]
                int idx4 = 2 * (halfN - k) + 1;      // packed[2 * (halfN - k) + 1]
                
                // All indices must be within [0, packedArrayLength)
                assertTrue(idx1 >= 0 && idx1 < packedArrayLength, 
                    "packed[2*k] out of bounds: " + idx1);
                assertTrue(idx2 >= 0 && idx2 < packedArrayLength, 
                    "packed[2*(halfN-k)] out of bounds: " + idx2);
                assertTrue(idx3 >= 0 && idx3 < packedArrayLength, 
                    "packed[2*k+1] out of bounds: " + idx3);
                assertTrue(idx4 >= 0 && idx4 < packedArrayLength, 
                    "packed[2*(halfN-k)+1] out of bounds: " + idx4);
                
                // Verify we never access the same indices (except at center for odd halfN)
                if (k != halfN - k) {
                    assertNotEquals(idx1, idx2, "Should not access same real part");
                    assertNotEquals(idx3, idx4, "Should not access same imaginary part");
                }
            }
            
            // Demonstrate why k = halfN would be problematic
            if (halfN > 0) {
                int problematicK = halfN;
                int problematicIdx = 2 * (halfN - problematicK); // = 2 * 0 = 0
                System.out.println("  If k = halfN (" + problematicK + "), then 2*(halfN-k) = " + problematicIdx);
                System.out.println("  This would incorrectly access packed[0] when we want packed[" + (2 * halfN) + "]");
            }
        }
    }
    
    @Test
    @DisplayName("Test edge cases for fftRealOptimized")
    void testEdgeCases() {
        // Test empty array
        assertDoesNotThrow(() -> {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(new double[0]);
            assertEquals(0, result.length);
        });
        
        // Test single element
        assertDoesNotThrow(() -> {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(new double[]{1.0});
            assertEquals(1, result.length);
            assertEquals(1.0, result[0].real());
            assertEquals(0.0, result[0].imag());
        });
        
        // Test odd-length arrays (should fall back to standard FFT)
        int[] oddSizes = {3, 5, 7, 9, 11};
        for (int n : oddSizes) {
            double[] real = new double[n];
            for (int i = 0; i < n; i++) {
                real[i] = i + 1.0; // Simple test data
            }
            
            assertDoesNotThrow(() -> {
                ComplexNumber[] result = OptimizedFFT.fftRealOptimized(real);
                assertEquals(n, result.length);
            }, "fftRealOptimized failed for odd size " + n);
        }
    }
}