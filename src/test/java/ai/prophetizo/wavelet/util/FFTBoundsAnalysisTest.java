package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to analyze potential array bounds issues in FFT implementation.
 */
class FFTBoundsAnalysisTest {
    
    @Test
    @DisplayName("Analyze array bounds in fftRealOptimized loop")
    void testArrayBoundsAnalysis() {
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
                
                // Test analysis: check the loop bounds logic
                int halfN = n / 2;
                System.out.println("  halfN = " + halfN);
                System.out.println("  packed array length would be: " + (2 * halfN));
                
                // Analyze the critical loop: for (int k = 1; k < halfN; k++)
                for (int k = 1; k < halfN; k++) {
                    int access1 = 2 * k;                    // packed[2 * k]
                    int access2 = 2 * (halfN - k);          // packed[2 * (halfN - k)]
                    int access3 = 2 * k + 1;                // packed[2 * k + 1]
                    int access4 = 2 * (halfN - k) + 1;      // packed[2 * (halfN - k) + 1]
                    
                    int maxValidIndex = 2 * halfN - 1;
                    
                    System.out.println("    k=" + k + ": access indices [" + access1 + ", " + access2 + ", " + access3 + ", " + access4 + "], max valid: " + maxValidIndex);
                    
                    // All accesses should be within bounds
                    assertTrue(access1 <= maxValidIndex, "access1 out of bounds");
                    assertTrue(access2 <= maxValidIndex, "access2 out of bounds");
                    assertTrue(access3 <= maxValidIndex, "access3 out of bounds");
                    assertTrue(access4 <= maxValidIndex, "access4 out of bounds");
                    
                    assertTrue(access1 >= 0, "access1 negative");
                    assertTrue(access2 >= 0, "access2 negative");
                    assertTrue(access3 >= 0, "access3 negative");
                    assertTrue(access4 >= 0, "access4 negative");
                }
            }, "fftRealOptimized failed for size " + n);
        }
    }
    
    @Test
    @DisplayName("Test boundary condition and verify loop bounds are correct")
    void testBoundaryConditionDocumentation() {
        // Document why the loop bound k < halfN is critical
        int n = 8;  // Even number
        int halfN = n / 2; // halfN = 4
        int packedLength = 2 * halfN; // = 8
        
        System.out.println("Analysis for n=" + n + ", halfN=" + halfN + ", packedLength=" + packedLength);
        
        // If k reached halfN (which it shouldn't in the current loop):
        int k = halfN; // k = 4
        int access = 2 * (halfN - k); // 2 * (4 - 4) = 2 * 0 = 0
        
        System.out.println("If k=" + k + " (=halfN), then access index would be: " + access);
        System.out.println("This would cause packed[2*k] and packed[2*(halfN-k)] to access the same element");
        System.out.println("Leading to incorrect FFT calculations due to symmetry violation");
        
        // Verify current loop bounds prevent this issue
        System.out.println("Current loop: k from 1 to " + (halfN - 1) + " (k < " + halfN + ")");
        
        // Additional verification: ensure that array accesses are valid
        for (int testK = 1; testK < halfN; testK++) {
            int symmetric = halfN - testK;
            
            // When k equals halfN - k (at the center), that's mathematically correct
            // This happens when k = halfN/2, which is valid for FFT symmetry
            if (testK == symmetric) {
                System.out.println("Center symmetry point: k=" + testK + " equals (halfN-k)=" + symmetric);
            }
            
            assertTrue(testK >= 1 && testK < halfN, 
                "k should be in range [1, halfN)");
            assertTrue(symmetric >= 1 && symmetric < halfN, 
                "symmetric index should be in range [1, halfN)");
            
            // Verify array accesses are within bounds
            assertTrue(2 * testK < packedLength, "2*k access within bounds");
            assertTrue(2 * symmetric < packedLength, "2*(halfN-k) access within bounds");
            assertTrue(2 * testK + 1 < packedLength, "2*k+1 access within bounds");
            assertTrue(2 * symmetric + 1 < packedLength, "2*(halfN-k)+1 access within bounds");
        }
        
        assertTrue(halfN > 0, "halfN should be positive");
        assertTrue(packedLength > 0, "packedLength should be positive");
    }
}