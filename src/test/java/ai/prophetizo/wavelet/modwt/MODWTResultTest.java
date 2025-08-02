package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MODWTResult interface and its implementation.
 */
class MODWTResultTest {

    @Test
    void testMODWTResultImplConstruction() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult result = new MODWTResultImpl(approx, detail);
        
        assertEquals(4, result.getSignalLength());
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
        assertTrue(result.isValid());
    }

    @Test
    void testMODWTResultImplDefensiveCopies() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult result = new MODWTResultImpl(approx, detail);
        
        // Modify original arrays
        approx[0] = 999.0;
        detail[0] = 888.0;
        
        // Result should be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0]);
        assertEquals(0.5, result.detailCoeffs()[0]);
        
        // Modify returned arrays
        double[] returnedApprox = result.approximationCoeffs();
        double[] returnedDetail = result.detailCoeffs();
        returnedApprox[0] = 777.0;
        returnedDetail[0] = 666.0;
        
        // Result should still be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0]);
        assertEquals(0.5, result.detailCoeffs()[0]);
    }

    @Test
    void testMODWTResultImplValidation() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        // Valid construction
        assertDoesNotThrow(() -> new MODWTResultImpl(approx, detail));
        
        // Null approximation
        assertThrows(NullPointerException.class, 
            () -> new MODWTResultImpl(null, detail));
        
        // Null detail
        assertThrows(NullPointerException.class, 
            () -> new MODWTResultImpl(approx, null));
        
        // Mismatched lengths
        double[] shortDetail = {0.5, 1.5};
        assertThrows(IllegalArgumentException.class, 
            () -> new MODWTResultImpl(approx, shortDetail));
        
        // Empty arrays
        assertThrows(IllegalArgumentException.class, 
            () -> new MODWTResultImpl(new double[0], new double[0]));
        
        // NaN values
        double[] nanApprox = {1.0, Double.NaN, 3.0, 4.0};
        assertThrows(InvalidSignalException.class, 
            () -> new MODWTResultImpl(nanApprox, detail));
        
        // Infinite values
        double[] infDetail = {0.5, Double.POSITIVE_INFINITY, 2.5, 3.5};
        assertThrows(InvalidSignalException.class, 
            () -> new MODWTResultImpl(approx, infDetail));
    }

    @Test
    void testMODWTResultEqualsAndHashCode() {
        double[] approx1 = {1.0, 2.0, 3.0, 4.0};
        double[] detail1 = {0.5, 1.5, 2.5, 3.5};
        double[] approx2 = {1.0, 2.0, 3.0, 4.0};
        double[] detail2 = {0.5, 1.5, 2.5, 3.5};
        double[] approx3 = {1.0, 2.0, 3.0, 5.0}; // Different value
        
        MODWTResult result1 = new MODWTResultImpl(approx1, detail1);
        MODWTResult result2 = new MODWTResultImpl(approx2, detail2);
        MODWTResult result3 = new MODWTResultImpl(approx3, detail1);
        
        // Test equality
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1, result1); // reflexive
        
        // Test hash code consistency
        assertEquals(result1.hashCode(), result2.hashCode());
        
        // Test equals with null and different class
        assertNotEquals(result1, null);
        assertNotEquals(result1, "not a MODWTResult");
    }

    @Test
    void testMODWTResultToString() {
        double[] approx = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5, 4.5, 5.5};
        
        MODWTResult result = new MODWTResultImpl(approx, detail);
        String str = result.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("signalLength=6"));
        assertTrue(str.contains("MODWTResult"));
        // Should only show first 5 elements due to truncation
        assertTrue(str.contains("1.0") && str.contains("2.0"));
    }

    @Test
    void testIsValidMethod() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult validResult = new MODWTResultImpl(approx, detail);
        assertTrue(validResult.isValid());
        
        // Create a mock invalid result (this would require creating an invalid implementation)
        // For now, just verify that the valid result passes the validation
        MODWTResult result = new MODWTResultImpl(approx, detail);
        assertTrue(result.isValid());
    }
}