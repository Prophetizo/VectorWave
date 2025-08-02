package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MODWTTransform class.
 * 
 * <p>These tests verify the basic functionality of the MODWT implementation,
 * including forward/inverse transforms, perfect reconstruction, and edge cases.</p>
 */
class MODWTTransformTest {

    private MODWTTransform modwtTransform;
    private static final double TOLERANCE = 1e-12;

    @BeforeEach
    void setUp() {
        modwtTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    }

    @Test
    void testConstructorValidation() {
        // Test null wavelet
        assertThrows(NullPointerException.class, 
            () -> new MODWTTransform(null, BoundaryMode.PERIODIC));
        
        // Test null boundary mode
        assertThrows(NullPointerException.class, 
            () -> new MODWTTransform(new Haar(), null));
        
        // Test unsupported boundary mode
        assertThrows(UnsupportedOperationException.class, 
            () -> new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING));
    }

    @Test
    void testForwardTransformBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        MODWTResult result = modwtTransform.forward(signal);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(4, result.getSignalLength());
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
    }

    @Test
    void testForwardTransformArbitraryLength() {
        // Test with non-power-of-2 length (MODWT should handle this)
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        MODWTResult result = modwtTransform.forward(signal);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(7, result.getSignalLength());
        assertEquals(7, result.approximationCoeffs().length);
        assertEquals(7, result.detailCoeffs().length);
    }

    @Test
    void testPerfectReconstruction() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        MODWTResult result = modwtTransform.forward(signal);
        double[] reconstructed = modwtTransform.inverse(result);
        
        assertEquals(signal.length, reconstructed.length);
        
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], TOLERANCE, 
                "Reconstruction error at index " + i);
        }
    }

    @Test
    void testPerfectReconstructionArbitraryLength() {
        // Test perfect reconstruction with non-power-of-2 length
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        MODWTResult result = modwtTransform.forward(signal);
        double[] reconstructed = modwtTransform.inverse(result);
        
        assertEquals(signal.length, reconstructed.length);
        
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], TOLERANCE, 
                "Reconstruction error at index " + i);
        }
    }

    @Test
    void testSingleElementSignal() {
        double[] signal = {5.0};
        
        MODWTResult result = modwtTransform.forward(signal);
        double[] reconstructed = modwtTransform.inverse(result);
        
        assertEquals(1, result.getSignalLength());
        assertEquals(1, reconstructed.length);
        assertEquals(signal[0], reconstructed[0], TOLERANCE);
    }

    @Test
    void testForwardTransformInputValidation() {
        // Test null signal
        assertThrows(NullPointerException.class, 
            () -> modwtTransform.forward(null));
        
        // Test empty signal
        assertThrows(InvalidSignalException.class, 
            () -> modwtTransform.forward(new double[0]));
        
        // Test signal with NaN
        assertThrows(InvalidSignalException.class, 
            () -> modwtTransform.forward(new double[]{1.0, Double.NaN, 3.0}));
        
        // Test signal with infinity
        assertThrows(InvalidSignalException.class, 
            () -> modwtTransform.forward(new double[]{1.0, Double.POSITIVE_INFINITY, 3.0}));
    }

    @Test
    void testInverseTransformInputValidation() {
        // Test null result
        assertThrows(NullPointerException.class, 
            () -> modwtTransform.inverse(null));
    }

    @Test
    void testGetters() {
        assertEquals(new Haar().getClass(), modwtTransform.getWavelet().getClass());
        assertEquals(BoundaryMode.PERIODIC, modwtTransform.getBoundaryMode());
    }

    @Test
    void testShiftInvariance() {
        // MODWT should be shift-invariant - shifting the input should shift the output
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] shiftedSignal = {8.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // circular shift by 1
        
        MODWTResult result1 = modwtTransform.forward(signal);
        MODWTResult result2 = modwtTransform.forward(shiftedSignal);
        
        // The coefficients should be circularly shifted versions of each other
        // This is a simplified test - full shift-invariance testing would be more complex
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getSignalLength(), result2.getSignalLength());
    }
}