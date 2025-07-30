package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the zero-copy forward transform method.
 */
class ZeroCopyTransformTest {
    
    @Test
    @DisplayName("Should perform zero-copy transform on array slice")
    void testZeroCopyTransform() {
        Wavelet wavelet = new Haar();
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Create a larger array with multiple windows
        double[] largeSignal = new double[32];
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = i + 1.0;
        }
        
        // Transform the full array for comparison
        TransformResult fullResult = transform.forward(largeSignal);
        
        // Transform slices using zero-copy method
        TransformResult slice1 = transform.forward(largeSignal, 0, 16);
        TransformResult slice2 = transform.forward(largeSignal, 16, 16);
        
        // Create expected arrays for first slice
        double[] expectedSignal1 = new double[16];
        System.arraycopy(largeSignal, 0, expectedSignal1, 0, 16);
        TransformResult expectedResult1 = transform.forward(expectedSignal1);
        
        // Create expected arrays for second slice  
        double[] expectedSignal2 = new double[16];
        System.arraycopy(largeSignal, 16, expectedSignal2, 0, 16);
        TransformResult expectedResult2 = transform.forward(expectedSignal2);
        
        // Verify slices match expected results
        assertArrayEquals(expectedResult1.approximationCoeffs(), slice1.approximationCoeffs(), 1e-10);
        assertArrayEquals(expectedResult1.detailCoeffs(), slice1.detailCoeffs(), 1e-10);
        
        assertArrayEquals(expectedResult2.approximationCoeffs(), slice2.approximationCoeffs(), 1e-10);
        assertArrayEquals(expectedResult2.detailCoeffs(), slice2.detailCoeffs(), 1e-10);
    }
    
    @Test
    @DisplayName("Should handle overlapping windows correctly")
    void testOverlappingWindows() {
        Wavelet wavelet = new Haar();
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Transform overlapping windows
        TransformResult window1 = transform.forward(signal, 0, 16);
        TransformResult window2 = transform.forward(signal, 8, 16); // 50% overlap
        TransformResult window3 = transform.forward(signal, 16, 16);
        
        // Verify each window was transformed independently
        assertNotNull(window1);
        assertNotNull(window2);
        assertNotNull(window3);
        
        assertEquals(8, window1.approximationCoeffs().length);
        assertEquals(8, window1.detailCoeffs().length);
        
        // Windows should have different results due to different data
        assertFalse(arraysEqual(window1.approximationCoeffs(), window2.approximationCoeffs()));
        assertFalse(arraysEqual(window2.approximationCoeffs(), window3.approximationCoeffs()));
    }
    
    @Test
    @DisplayName("Should validate slice parameters")
    void testSliceValidation() {
        Wavelet wavelet = new Haar();
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        
        double[] signal = new double[32];
        
        // Test null signal
        assertThrows(Exception.class, () -> 
            transform.forward(null, 0, 16));
        
        // Test negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> 
            transform.forward(signal, -1, 16));
        
        // Test negative length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            transform.forward(signal, 0, -1));
        
        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            transform.forward(signal, 20, 16));
        
        // Test non-power-of-two length
        assertThrows(InvalidSignalException.class, () -> 
            transform.forward(signal, 0, 15));
        
        // Test zero length
        assertThrows(InvalidSignalException.class, () -> 
            transform.forward(signal, 0, 0));
    }
    
    @Test
    @DisplayName("Should work with different boundary modes")
    void testDifferentBoundaryModes() {
        Wavelet wavelet = new Haar();
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        // Test with PERIODIC boundary mode
        WaveletTransform periodicTransform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult periodicResult = periodicTransform.forward(signal, 0, 16);
        assertNotNull(periodicResult);
        
        // Test with ZERO_PADDING boundary mode
        WaveletTransform zeroPadTransform = new WaveletTransform(wavelet, BoundaryMode.ZERO_PADDING);
        TransformResult zeroPadResult = zeroPadTransform.forward(signal, 0, 16);
        assertNotNull(zeroPadResult);
        
        // For this specific signal and Haar wavelet, the results might be the same
        // depending on the signal values. Let's just verify both transforms worked.
        assertEquals(8, periodicResult.approximationCoeffs().length);
        assertEquals(8, zeroPadResult.approximationCoeffs().length);
    }
    
    private boolean arraysEqual(double[] a, double[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-10) return false;
        }
        return true;
    }
}