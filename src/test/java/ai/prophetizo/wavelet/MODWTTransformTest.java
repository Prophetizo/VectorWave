package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MODWT (Maximal Overlap Discrete Wavelet Transform) implementation.
 */
class MODWTTransformTest {

    @Test
    @DisplayName("MODWT should work with non-power-of-2 signal lengths")
    void testNonPowerOfTwoSignal() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Test with various non-power-of-2 lengths
        double[] signal1 = {1, 2, 3};  // length 3
        double[] signal2 = {1, 2, 3, 4, 5};  // length 5
        double[] signal3 = {1, 2, 3, 4, 5, 6, 7};  // length 7
        
        // Should not throw exceptions
        assertDoesNotThrow(() -> {
            TransformResult result1 = transform.forward(signal1);
            assertEquals(3, result1.approximationCoeffs().length);
            assertEquals(3, result1.detailCoeffs().length);
            
            TransformResult result2 = transform.forward(signal2);
            assertEquals(5, result2.approximationCoeffs().length);
            assertEquals(5, result2.detailCoeffs().length);
            
            TransformResult result3 = transform.forward(signal3);
            assertEquals(7, result3.approximationCoeffs().length);
            assertEquals(7, result3.detailCoeffs().length);
        });
    }
    
    @Test
    @DisplayName("MODWT output length should equal input length")
    void testSameLengthOutput() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        TransformResult result = transform.forward(signal);
        
        assertEquals(signal.length, result.approximationCoeffs().length);
        assertEquals(signal.length, result.detailCoeffs().length);
    }
    
    @Test
    @DisplayName("MODWT should be shift-invariant")
    void testShiftInvariance() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] shiftedSignal = {8, 1, 2, 3, 4, 5, 6, 7};  // Circular shift
        
        TransformResult result1 = transform.forward(signal);
        TransformResult result2 = transform.forward(shiftedSignal);
        
        // The coefficients should be shifted versions of each other
        double[] approx1 = result1.approximationCoeffs();
        double[] approx2 = result2.approximationCoeffs();
        double[] detail1 = result1.detailCoeffs();
        double[] detail2 = result2.detailCoeffs();
        
        // Check that the circular correlation is high (shift-invariance property)
        assertTrue(isCircularlyShifted(approx1, approx2, 0.001));
        assertTrue(isCircularlyShifted(detail1, detail2, 0.001));
    }
    
    @Test
    @DisplayName("MODWT reconstruction should be perfect for Haar wavelet")
    void testPerfectReconstruction() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 4, 2, 8, 5, 3, 7, 6};
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        assertEquals(signal.length, reconstructed.length);
        
        // Check reconstruction accuracy
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-10, 
                "Reconstruction error at index " + i);
        }
    }
    
    @Test
    @DisplayName("MODWT should work with different wavelets")
    void testDifferentWavelets() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Test with Haar
        MODWTTransform haarTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult haarResult = haarTransform.forward(signal);
        
        // Test with DB2 (which is the same as Haar for this purpose)
        MODWTTransform db2Transform = new MODWTTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        TransformResult db2Result = db2Transform.forward(signal);
        
        // Both should produce results of the same length as input
        assertEquals(signal.length, haarResult.approximationCoeffs().length);
        assertEquals(signal.length, db2Result.approximationCoeffs().length);
        
        // Results should be different (different wavelets)
        assertFalse(Arrays.equals(haarResult.approximationCoeffs(), db2Result.approximationCoeffs()));
    }
    
    @Test
    @DisplayName("MODWT should work with both boundary modes")
    void testBoundaryModes() {
        double[] signal = {1, 2, 3, 4, 5};
        
        MODWTTransform periodicTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTTransform zeroPadTransform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        TransformResult periodicResult = periodicTransform.forward(signal);
        TransformResult zeroPadResult = zeroPadTransform.forward(signal);
        
        // Both should have same length output
        assertEquals(signal.length, periodicResult.approximationCoeffs().length);
        assertEquals(signal.length, zeroPadResult.approximationCoeffs().length);
        
        // But different values due to different boundary handling
        assertFalse(Arrays.equals(periodicResult.approximationCoeffs(), 
                                  zeroPadResult.approximationCoeffs()));
    }
    
    @Test
    @DisplayName("MODWT should reject null and empty signals")
    void testInvalidSignals() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        assertThrows(InvalidSignalException.class, () -> transform.forward(null));
        assertThrows(InvalidSignalException.class, () -> transform.forward(new double[0]));
    }
    
    @Test
    @DisplayName("MODWT should reject signals with NaN or infinity")
    void testInvalidValues() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] nanSignal = {1, 2, Double.NaN, 4};
        double[] infSignal = {1, 2, Double.POSITIVE_INFINITY, 4};
        
        assertThrows(InvalidSignalException.class, () -> transform.forward(nanSignal));
        assertThrows(InvalidSignalException.class, () -> transform.forward(infSignal));
    }
    
    @Test
    @DisplayName("MODWT Factory should create transforms correctly")
    void testMODWTFactory() {
        MODWTTransformFactory factory = new MODWTTransformFactory()
            .withBoundaryMode(BoundaryMode.ZERO_PADDING);
        
        MODWTTransform transform = factory.create(new Haar());
        
        assertEquals(BoundaryMode.ZERO_PADDING, transform.getBoundaryMode());
        assertInstanceOf(Haar.class, transform.getWavelet());
    }
    
    @Test
    @DisplayName("MODWT should work with minimum signal length")
    void testMinimumSignalLength() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Test with length 1 (minimum meaningful signal)
        double[] signal = {5.0};
        TransformResult result = transform.forward(signal);
        
        assertEquals(1, result.approximationCoeffs().length);
        assertEquals(1, result.detailCoeffs().length);
        
        double[] reconstructed = transform.inverse(result);
        assertEquals(1, reconstructed.length);
        assertEquals(signal[0], reconstructed[0], 1e-10);
    }
    
    /**
     * Helper method to check if two arrays are circular shifts of each other.
     */
    private boolean isCircularlyShifted(double[] arr1, double[] arr2, double tolerance) {
        if (arr1.length != arr2.length) return false;
        
        int n = arr1.length;
        for (int shift = 0; shift < n; shift++) {
            boolean matches = true;
            for (int i = 0; i < n; i++) {
                if (Math.abs(arr1[i] - arr2[(i + shift) % n]) > tolerance) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }
}