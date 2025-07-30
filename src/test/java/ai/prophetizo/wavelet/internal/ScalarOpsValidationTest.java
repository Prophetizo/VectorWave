package ai.prophetizo.wavelet.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parameter validation in ScalarOps slice-based methods.
 */
class ScalarOpsValidationTest {
    
    @Test
    @DisplayName("convolveAndDownsamplePeriodic should validate null parameters")
    void testConvolveAndDownsamplePeriodicNullValidation() {
        double[] signal = new double[8];
        double[] filter = new double[4];
        double[] output = new double[4];
        
        // Test null signal
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(null, 0, 8, filter, output),
            "Should reject null signal");
        
        // Test null filter
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 8, null, output),
            "Should reject null filter");
        
        // Test null output
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 8, filter, null),
            "Should reject null output");
    }
    
    @Test
    @DisplayName("convolveAndDownsamplePeriodic should validate bounds")
    void testConvolveAndDownsamplePeriodicBoundsValidation() {
        double[] signal = new double[8];
        double[] filter = new double[4];
        double[] output = new double[4];
        
        // Test negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, -1, 8, filter, output),
            "Should reject negative offset");
        
        // Test negative length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, -1, filter, output),
            "Should reject negative length");
        
        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 4, 8, filter, output),
            "Should reject bounds exceeding array");
        
        // Test wrong output array size
        double[] wrongOutput = new double[3];
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 8, filter, wrongOutput),
            "Should reject wrong output array size");
    }
    
    @Test
    @DisplayName("convolveAndDownsampleDirect should validate null parameters")
    void testConvolveAndDownsampleDirectNullValidation() {
        double[] signal = new double[8];
        double[] filter = new double[4];
        double[] output = new double[4];
        
        // Test null signal
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(null, 0, 8, filter, output),
            "Should reject null signal");
        
        // Test null filter
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, 0, 8, null, output),
            "Should reject null filter");
        
        // Test null output
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, 0, 8, filter, null),
            "Should reject null output");
    }
    
    @Test
    @DisplayName("convolveAndDownsampleDirect should validate bounds")
    void testConvolveAndDownsampleDirectBoundsValidation() {
        double[] signal = new double[8];
        double[] filter = new double[4];
        double[] output = new double[4];
        
        // Test negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, -1, 8, filter, output),
            "Should reject negative offset");
        
        // Test negative length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, 0, -1, filter, output),
            "Should reject negative length");
        
        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, 4, 8, filter, output),
            "Should reject bounds exceeding array");
        
        // Test wrong output array size
        double[] wrongOutput = new double[3];
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.convolveAndDownsampleDirect(signal, 0, 8, filter, wrongOutput),
            "Should reject wrong output array size");
    }
    
    @Test
    @DisplayName("combinedTransformPeriodic should validate null parameters")
    void testCombinedTransformPeriodicNullValidation() {
        double[] signal = new double[8];
        double[] lowFilter = new double[4];
        double[] highFilter = new double[4];
        double[] approxCoeffs = new double[4];
        double[] detailCoeffs = new double[4];
        
        // Test null signal
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(null, 0, 8, lowFilter, highFilter, approxCoeffs, detailCoeffs),
            "Should reject null signal");
        
        // Test null low filter
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, null, highFilter, approxCoeffs, detailCoeffs),
            "Should reject null low filter");
        
        // Test null high filter
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, null, approxCoeffs, detailCoeffs),
            "Should reject null high filter");
        
        // Test null approx coeffs
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, highFilter, null, detailCoeffs),
            "Should reject null approximation coefficients");
        
        // Test null detail coeffs
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, highFilter, approxCoeffs, null),
            "Should reject null detail coefficients");
    }
    
    @Test
    @DisplayName("combinedTransformPeriodic should validate bounds and array sizes")
    void testCombinedTransformPeriodicBoundsValidation() {
        double[] signal = new double[8];
        double[] lowFilter = new double[4];
        double[] highFilter = new double[4];
        double[] approxCoeffs = new double[4];
        double[] detailCoeffs = new double[4];
        
        // Test negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, -1, 8, lowFilter, highFilter, approxCoeffs, detailCoeffs),
            "Should reject negative offset");
        
        // Test negative length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, -1, lowFilter, highFilter, approxCoeffs, detailCoeffs),
            "Should reject negative length");
        
        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 4, 8, lowFilter, highFilter, approxCoeffs, detailCoeffs),
            "Should reject bounds exceeding array");
        
        // Test wrong approx array size
        double[] wrongApprox = new double[3];
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, highFilter, wrongApprox, detailCoeffs),
            "Should reject wrong approximation array size");
        
        // Test wrong detail array size
        double[] wrongDetail = new double[3];
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, highFilter, approxCoeffs, wrongDetail),
            "Should reject wrong detail array size");
        
        // Test mismatched filter lengths
        double[] shortHighFilter = new double[2];
        assertThrows(IllegalArgumentException.class, () -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, lowFilter, shortHighFilter, approxCoeffs, detailCoeffs),
            "Should reject mismatched filter lengths");
    }
    
    @Test
    @DisplayName("Valid slice operations should work correctly")
    void testValidSliceOperations() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.5};  // Simple averaging filter
        double[] output = new double[4];
        
        // This should work without throwing
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 8, filter, output));
        
        // Test with offset
        double[] output2 = new double[2];
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 2, 4, filter, output2));
        
        // Combined transform
        double[] approx = new double[4];
        double[] detail = new double[4];
        assertDoesNotThrow(() -> 
            ScalarOps.combinedTransformPeriodic(signal, 0, 8, filter, filter, approx, detail));
    }
}