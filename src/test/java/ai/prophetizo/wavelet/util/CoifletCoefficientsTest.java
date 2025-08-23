package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CoifletCoefficients utility.
 * Ensures proper functionality and mathematical properties of Coiflet coefficients.
 */
class CoifletCoefficientsTest {

    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Get coefficients for supported orders")
    void testGetCoefficientsForSupportedOrders(int order) {
        double[] coeffs = CoifletCoefficients.get(order);
        assertNotNull(coeffs, "Coefficients should not be null for COIF" + order);
        assertEquals(6 * order, coeffs.length, 
            "COIF" + order + " should have " + (6 * order) + " coefficients");
        
        // Verify mathematical properties
        boolean valid = CoifletCoefficients.verifyCoefficients(coeffs, order);
        assertTrue(valid, "COIF" + order + " coefficients must satisfy all properties");
    }

    @Test
    @DisplayName("Get coefficients for unsupported low order throws exception")
    void testGetCoefficientsForLowOrder() {
        // Orders 1-5 should be accessed from Coiflet class
        for (int order = 1; order <= 5; order++) {
            int finalOrder = order;
            Exception exception = assertThrows(IllegalArgumentException.class,
                () -> CoifletCoefficients.get(finalOrder),
                "Should throw exception for COIF" + order);
            
            assertTrue(exception.getMessage().contains("should be accessed directly from Coiflet class"),
                "Exception message should indicate to use Coiflet class");
        }
    }

    @Test
    @DisplayName("Get coefficients for unimplemented high order throws exception")
    void testGetCoefficientsForHighOrder() {
        // Orders 11-17 are not yet implemented
        for (int order = 11; order <= 17; order++) {
            int finalOrder = order;
            assertThrows(UnsupportedOperationException.class,
                () -> CoifletCoefficients.get(finalOrder),
                "Should throw exception for unimplemented COIF" + order);
        }
    }

    @Test
    @DisplayName("Get coefficients for invalid order throws exception")
    void testGetCoefficientsForInvalidOrder() {
        assertThrows(IllegalArgumentException.class,
            () -> CoifletCoefficients.get(0),
            "Should throw exception for order 0");
        
        assertThrows(IllegalArgumentException.class,
            () -> CoifletCoefficients.get(-1),
            "Should throw exception for negative order");
        
        assertThrows(IllegalArgumentException.class,
            () -> CoifletCoefficients.get(18),
            "Should throw exception for order > 17");
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Check if coefficients are available")
    void testHasCoefficientsForAvailableOrders(int order) {
        assertTrue(CoifletCoefficients.hasCoefficients(order),
            "COIF" + order + " should have coefficients available");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 11, 12, 13, 14, 15, 16, 17})
    @DisplayName("Check if coefficients are not available")
    void testHasCoefficientsForUnavailableOrders(int order) {
        assertFalse(CoifletCoefficients.hasCoefficients(order),
            "COIF" + order + " should not have coefficients available in this class");
    }

    @Test
    @DisplayName("Get filter length for various orders")
    void testGetFilterLength() {
        for (int order = 1; order <= 17; order++) {
            int length = CoifletCoefficients.getFilterLength(order);
            assertEquals(6 * order, length,
                "Filter length for COIF" + order + " should be " + (6 * order));
        }
    }

    @Test
    @DisplayName("Get vanishing moments for various orders")
    void testGetVanishingMoments() {
        for (int order = 1; order <= 17; order++) {
            int moments = CoifletCoefficients.getVanishingMoments(order);
            assertEquals(2 * order, moments,
                "Vanishing moments for COIF" + order + " should be " + (2 * order));
        }
    }

    @Test
    @DisplayName("Verify COIF6 coefficients match expected values")
    void testCOIF6Coefficients() {
        double[] coeffs = CoifletCoefficients.get(6);
        assertEquals(36, coeffs.length);
        
        // Verify first coefficient
        assertEquals(-5.3090884171968937E-09, coeffs[0], 1e-20,
            "First COIF6 coefficient should match expected value");
        
        // Verify last coefficient
        assertEquals(5.0775487836340565E-05, coeffs[35], 1e-15,
            "Last COIF6 coefficient should match expected value");
        
        // Verify mathematical properties
        assertTrue(CoifletCoefficients.verifyCoefficients(coeffs, 6));
    }

    @Test
    @DisplayName("Verify COIF10 coefficients match expected values")
    void testCOIF10Coefficients() {
        double[] coeffs = CoifletCoefficients.get(10);
        assertEquals(60, coeffs.length);
        
        // Verify first coefficient
        assertEquals(-5.7379612668974354E-14, coeffs[0], 1e-25,
            "First COIF10 coefficient should match expected value");
        
        // Verify last coefficient
        assertEquals(1.7423674803127223E-07, coeffs[59], 1e-18,
            "Last COIF10 coefficient should match expected value");
        
        // Verify mathematical properties
        assertTrue(CoifletCoefficients.verifyCoefficients(coeffs, 10));
    }

    @Test
    @DisplayName("Coefficients are immutable")
    void testCoefficientsAreImmutable() {
        double[] coeffs1 = CoifletCoefficients.get(6);
        double originalValue = coeffs1[0];
        
        // Modify the returned array
        coeffs1[0] = 999.0;
        
        // Get coefficients again
        double[] coeffs2 = CoifletCoefficients.get(6);
        
        // Second array should not be affected
        assertEquals(originalValue, coeffs2[0], 1e-20,
            "Modifying returned array should not affect internal coefficients");
    }

    @Test
    @DisplayName("Verify coefficients with invalid length returns false")
    void testVerifyCoefficientsInvalidLength() {
        double[] wrongLength = new double[35]; // Wrong length for COIF6
        
        assertFalse(CoifletCoefficients.verifyCoefficients(wrongLength, 6),
            "Should return false for incorrect coefficient length");
    }

    @Test
    @DisplayName("Verify coefficients with incorrect sum returns false")
    void testVerifyCoefficientsIncorrectSum() {
        double[] badCoeffs = new double[36];
        // Fill with values that don't sum to sqrt(2)
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] = 0.01;
        }
        
        assertFalse(CoifletCoefficients.verifyCoefficients(badCoeffs, 6),
            "Should return false for incorrect sum");
    }

    @Test
    @DisplayName("Verify coefficients with incorrect sum of squares returns false")
    void testVerifyCoefficientsIncorrectSumOfSquares() {
        double[] badCoeffs = new double[36];
        // Create coefficients that sum to sqrt(2) but have wrong sum of squares
        double target = Math.sqrt(2) / 36;
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] = target;
        }
        
        assertFalse(CoifletCoefficients.verifyCoefficients(badCoeffs, 6),
            "Should return false for incorrect sum of squares");
    }

    @Test
    @DisplayName("Verify orthogonality property for all extended Coiflets")
    void testOrthogonalityProperty() {
        for (int order = 6; order <= 10; order++) {
            double[] coeffs = CoifletCoefficients.get(order);
            
            // Test orthogonality: sum(h[n] * h[n+2k]) = 0 for k != 0
            for (int k = 1; k < coeffs.length / 2; k++) {
                double dot = 0;
                for (int n = 0; n < coeffs.length - 2*k; n++) {
                    dot += coeffs[n] * coeffs[n + 2*k];
                }
                assertEquals(0.0, dot, 1e-10,
                    String.format("COIF%d orthogonality must be satisfied for k=%d", order, k));
            }
        }
    }

    @Test
    @DisplayName("Verify vanishing moments approximation property")
    void testVanishingMomentsProperty() {
        // Coiflets have both vanishing moments for the wavelet
        // and approximation order for the scaling function
        for (int order = 6; order <= 10; order++) {
            double[] coeffs = CoifletCoefficients.get(order);
            
            // Check that coefficients decay at the boundaries
            // (characteristic of Coiflet design)
            double firstMag = Math.abs(coeffs[0]);
            double lastMag = Math.abs(coeffs[coeffs.length - 1]);
            double middleMag = Math.abs(coeffs[coeffs.length / 2]);
            
            // Boundary coefficients should be much smaller than middle ones
            assertTrue(firstMag < middleMag / 1000,
                String.format("COIF%d should have very small boundary coefficients", order));
            assertTrue(lastMag < middleMag / 100,
                String.format("COIF%d should have small boundary coefficients", order));
        }
    }

    @Test
    @DisplayName("Different Coiflet orders have different properties")
    void testOrderProgression() {
        // As order increases, filter length increases
        for (int order = 6; order < 10; order++) {
            double[] coeffs1 = CoifletCoefficients.get(order);
            double[] coeffs2 = CoifletCoefficients.get(order + 1);
            
            assertTrue(coeffs2.length > coeffs1.length,
                String.format("COIF%d should be longer than COIF%d", order + 1, order));
            
            // Higher order should have smaller boundary coefficients (better approximation)
            double boundary1 = Math.abs(coeffs1[0]);
            double boundary2 = Math.abs(coeffs2[0]);
            assertTrue(boundary2 < boundary1,
                String.format("COIF%d should have smaller boundary than COIF%d", order + 1, order));
        }
    }

    @Test
    @DisplayName("Verify all stored coefficients satisfy mathematical properties")
    void testAllStoredCoefficientsSatisfyProperties() {
        int[] orders = {6, 7, 8, 9, 10};
        
        for (int order : orders) {
            double[] coeffs = CoifletCoefficients.get(order);
            
            // Verify sum equals sqrt(2)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 1e-10,
                String.format("COIF%d coefficients should sum to sqrt(2)", order));
            
            // Verify sum of squares equals 1
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, 1e-10,
                String.format("COIF%d sum of squares should equal 1", order));
            
            // Verify using the built-in verification method
            assertTrue(CoifletCoefficients.verifyCoefficients(coeffs, order),
                String.format("COIF%d should pass all verification checks", order));
        }
    }
}