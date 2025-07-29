package ai.prophetizo.wavelet.cwt.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specific tests to verify that circular convolution artifacts are eliminated.
 * 
 * <p>This test class focuses on edge cases and boundary conditions that would
 * produce different results between circular and linear convolution, ensuring
 * our FFT implementation correctly avoids artifacts.</p>
 */
class CircularArtifactTest {
    
    private FFTAcceleratedCWT fft;
    
    @BeforeEach
    void setUp() {
        fft = new FFTAcceleratedCWT();
    }
    
    @Test
    @DisplayName("Should avoid circular artifacts with edge discontinuity")
    void convolveLinear_shouldAvoidCircularArtifactsWithEdgeDiscontinuity() {
        // Given - signal with strong discontinuity at boundaries
        // This pattern would cause significant circular artifacts if circular convolution was used
        double[] signal = {5.0, 0.0, 0.0, 0.0};  // Large value at start, zeros elsewhere
        double[] kernel = {1.0, 1.0};             // Simple averaging kernel
        
        // When - linear convolution
        double[] result = fft.convolveLinear(signal, kernel);
        
        // Then - result should have expected length and values
        assertEquals(5, result.length, "Linear convolution should have length N+M-1");
        
        // Expected result: [5.0, 5.0, 0.0, 0.0, 0.0]
        // - First element: 5.0 * 1.0 = 5.0
        // - Second element: 5.0 * 1.0 + 0.0 * 1.0 = 5.0
        // - Remaining elements are zero due to zero signal values
        
        assertEquals(5.0, result[0], 1e-12, "First element should be 5.0");
        assertEquals(5.0, result[1], 1e-12, "Second element should be 5.0");
        assertEquals(0.0, result[2], 1e-12, "Third element should be 0.0");
        assertEquals(0.0, result[3], 1e-12, "Fourth element should be 0.0");
        assertEquals(0.0, result[4], 1e-12, "Fifth element should be 0.0");
        
        // Key verification: In circular convolution, the large value at start would
        // "wrap around" and affect the end of the result. This doesn't happen here.
    }
    
    @Test
    @DisplayName("Should handle impulse response correctly without boundary artifacts")
    void convolveLinear_shouldHandleImpulseResponseCorrectly() {
        // Given - unit impulse (tests boundary handling)
        double[] signal = {1.0, 0.0, 0.0, 0.0};
        double[] kernel = {0.5, 0.5};  // Simple averaging kernel
        
        // When
        double[] result = fft.convolveLinear(signal, kernel);
        
        // Then - result should match expected impulse response
        assertEquals(5, result.length, "Should have correct length N+M-1");
        
        // Expected: convolution of [1,0,0,0] with [0.5,0.5] = [0.5,0.5,0,0,0]
        assertEquals(0.5, result[0], 1e-12, "First element should be 0.5");
        assertEquals(0.5, result[1], 1e-12, "Second element should be 0.5");
        assertEquals(0.0, result[2], 1e-12, "Third element should be 0.0");
        assertEquals(0.0, result[3], 1e-12, "Fourth element should be 0.0");
        assertEquals(0.0, result[4], 1e-12, "Fifth element should be 0.0");
        
        // Key: verify no spurious energy at boundaries (which would indicate circular artifacts)
        assertEquals(0.0, result[result.length - 1], 1e-12, 
            "Last element should be zero (no boundary artifacts)");
    }
    
    @Test
    @DisplayName("Should handle asymmetric boundary conditions correctly")
    void convolveLinear_shouldHandleAsymmetricBoundaries() {
        // Given - asymmetric signal with different boundary values
        double[] signal = {1.0, 2.0, 3.0, 10.0};  // Large end value would cause circular artifacts
        double[] kernel = {-1.0, 1.0};            // Difference kernel (sensitive to boundaries)
        
        // When
        double[] result = fft.convolveLinear(signal, kernel);
        
        // Then - verify correct boundary handling
        assertEquals(5, result.length, "Should have correct output length");
        
        // Expected values for linear convolution:
        // result[0] = 1.0 * (-1.0) = -1.0
        // result[1] = 1.0 * 1.0 + 2.0 * (-1.0) = -1.0
        // result[2] = 2.0 * 1.0 + 3.0 * (-1.0) = -1.0
        // result[3] = 3.0 * 1.0 + 10.0 * (-1.0) = -7.0
        // result[4] = 10.0 * 1.0 = 10.0
        
        assertEquals(-1.0, result[0], 1e-12, "First boundary element");
        assertEquals(-1.0, result[1], 1e-12, "Second element");
        assertEquals(-1.0, result[2], 1e-12, "Third element");
        assertEquals(-7.0, result[3], 1e-12, "Fourth element");
        assertEquals(10.0, result[4], 1e-12, "Last boundary element");
        
        // In circular convolution, the large end value (10.0) would affect
        // the beginning of the result, creating artifacts. This doesn't happen here.
    }
    
    @Test
    @DisplayName("Should match theoretical linear convolution formula")
    void convolveLinear_shouldMatchTheoreticalFormula() {
        // Given - simple test case for mathematical verification
        double[] signal = {1.0, 1.0, 1.0, 1.0};
        double[] kernel = {1.0, -1.0};
        
        // When
        double[] result = fft.convolveLinear(signal, kernel);
        
        // Then - verify against mathematical definition of linear convolution
        // Linear convolution: y[n] = Σ x[k] * h[n-k] for valid k
        
        assertEquals(5, result.length, "Should have length N+M-1");
        
        // Manual calculation:
        // y[0] = x[0]*h[0] = 1.0*1.0 = 1.0
        // y[1] = x[0]*h[1] + x[1]*h[0] = 1.0*(-1.0) + 1.0*1.0 = 0.0
        // y[2] = x[1]*h[1] + x[2]*h[0] = 1.0*(-1.0) + 1.0*1.0 = 0.0
        // y[3] = x[2]*h[1] + x[3]*h[0] = 1.0*(-1.0) + 1.0*1.0 = 0.0
        // y[4] = x[3]*h[1] = 1.0*(-1.0) = -1.0
        
        assertEquals(1.0, result[0], 1e-12, "y[0] should be 1.0");
        assertEquals(0.0, result[1], 1e-12, "y[1] should be 0.0");
        assertEquals(0.0, result[2], 1e-12, "y[2] should be 0.0");
        assertEquals(0.0, result[3], 1e-12, "y[3] should be 0.0");
        assertEquals(-1.0, result[4], 1e-12, "y[4] should be -1.0");
    }
    
    @Test
    @DisplayName("Should demonstrate clear difference from circular convolution")
    void convolveLinear_shouldDifferFromCircularConvolution() {
        // Given - case where circular and linear convolution give very different results
        double[] signal = {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0}; // Values at opposite ends
        double[] kernel = {1.0, 1.0}; // Simple summing kernel
        
        // When
        double[] linearResult = fft.convolveLinear(signal, kernel);
        
        // Then - verify linear convolution properties
        assertEquals(9, linearResult.length, "Linear result has length N+M-1");
        
        // Key verification points:
        // - First elements affected only by start of signal
        assertEquals(1.0, linearResult[0], 1e-12, "Start: only affected by signal start");
        assertEquals(1.0, linearResult[1], 1e-12, "Start+1: transition region");
        
        // - Middle elements are zero (no wraparound from end)
        for (int i = 2; i < 7; i++) {
            assertEquals(0.0, linearResult[i], 1e-12, 
                "Middle elements should be zero (no circular wraparound) at index " + i);
        }
        
        // - End elements affected only by end of signal
        assertEquals(9.0, linearResult[7], 1e-12, "End-1: transition region");
        assertEquals(9.0, linearResult[8], 1e-12, "End: only affected by signal end");
        
        // This demonstrates the key fix: in circular convolution, the large value (9.0)
        // at the end would wraparound and contaminate the beginning of the result.
        // Our linear convolution correctly keeps beginning and end separate.
    }
}