package ai.prophetizo.wavelet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for TransformResult implementation.
 * 
 * <p>Tests focus on:</p>
 * <ul>
 *   <li>Immutability guarantees</li>
 *   <li>Defensive copying</li>
 *   <li>toString() output</li>
 *   <li>Internal validation (when enabled)</li>
 * </ul>
 */
@DisplayName("TransformResult Tests")
class TransformResultTest {
    
    @Test
    @DisplayName("TransformResult should provide defensive copies")
    void testDefensiveCopies() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        
        TransformResult result = new TransformResultImpl(approx, detail);
        
        // Modify original arrays
        approx[0] = 999.0;
        detail[0] = 999.0;
        
        // Result should be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0], 
            "Modification of source array should not affect result");
        assertEquals(0.1, result.detailCoeffs()[0],
            "Modification of source array should not affect result");
    }
    
    @Test
    @DisplayName("TransformResult getters should return new copies each time")
    void testMultipleGettersReturnDifferentArrays() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        
        TransformResult result = new TransformResultImpl(approx, detail);
        
        double[] approx1 = result.approximationCoeffs();
        double[] approx2 = result.approximationCoeffs();
        double[] detail1 = result.detailCoeffs();
        double[] detail2 = result.detailCoeffs();
        
        // Arrays should be equal but not the same instance
        assertArrayEquals(approx1, approx2, "Arrays should have same content");
        assertArrayEquals(detail1, detail2, "Arrays should have same content");
        assertNotSame(approx1, approx2, "Should return different array instances");
        assertNotSame(detail1, detail2, "Should return different array instances");
        
        // Modifying returned array should not affect subsequent calls
        approx1[0] = 999.0;
        assertEquals(1.0, approx2[0], "Arrays should be independent");
    }
    
    @Test
    @DisplayName("TransformResult toString should format correctly")
    void testToString() {
        double[] approx = {1.5, 2.5, 3.5};
        double[] detail = {0.1, 0.2, 0.3};
        
        TransformResult result = new TransformResultImpl(approx, detail);
        String str = result.toString();
        
        assertTrue(str.contains("TransformResult"), "Should contain class name");
        assertTrue(str.contains("Approximation"), "Should label approximation coefficients");
        assertTrue(str.contains("Detail"), "Should label detail coefficients");
        assertTrue(str.contains("1.5"), "Should contain coefficient values");
        assertTrue(str.contains("0.1"), "Should contain coefficient values");
    }
    
    @Test
    @DisplayName("TransformResult with empty arrays should throw assertion error")
    void testEmptyArrays() {
        // TransformResult enforces non-empty arrays via assertions
        double[] empty = new double[0];
        
        // With assertions enabled, this should throw
        assertThrows(AssertionError.class, () -> {
            new TransformResultImpl(empty, empty);
        }, "Empty arrays should trigger assertion error");
    }
    
    @Test
    @DisplayName("TransformResult with matching lengths should be valid")
    void testMatchingLengths() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        
        assertDoesNotThrow(() -> {
            TransformResult result = new TransformResultImpl(approx, detail);
            assertEquals(4, result.approximationCoeffs().length);
            assertEquals(4, result.detailCoeffs().length);
        });
    }
    
    @Test
    @DisplayName("TransformResult should handle large arrays efficiently")
    void testLargeArrays() {
        int size = 10000;
        double[] approx = new double[size];
        double[] detail = new double[size];
        
        // Fill with test data
        for (int i = 0; i < size; i++) {
            approx[i] = i * 0.1;
            detail[i] = i * 0.01;
        }
        
        TransformResult result = new TransformResultImpl(approx, detail);
        
        // Verify defensive copies work for large arrays
        approx[0] = 999.0;
        assertEquals(0.0, result.approximationCoeffs()[0], 1e-10);
        
        // Verify data integrity
        assertEquals(size, result.approximationCoeffs().length);
        assertEquals(size, result.detailCoeffs().length);
        assertEquals(0.1, result.approximationCoeffs()[1], 1e-10);
        assertEquals(0.01, result.detailCoeffs()[1], 1e-10);
    }
    
    @Test
    @DisplayName("TransformResult should preserve special values")
    void testSpecialValues() {
        double[] approx = {0.0, -0.0, 1e-300, -1e-300};
        double[] detail = {Double.MIN_VALUE, -Double.MIN_VALUE, 
                          Double.MIN_NORMAL, -Double.MIN_NORMAL};
        
        TransformResult result = new TransformResultImpl(approx, detail);
        
        // Verify special values are preserved exactly
        assertEquals(0.0, result.approximationCoeffs()[0]);
        assertEquals(-0.0, result.approximationCoeffs()[1]);
        assertTrue(Double.compare(-0.0, result.approximationCoeffs()[1]) == 0,
            "Should preserve negative zero");
        
        assertEquals(Double.MIN_VALUE, result.detailCoeffs()[0]);
        assertEquals(Double.MIN_NORMAL, result.detailCoeffs()[2]);
    }
    
    @Test
    @DisplayName("TransformResult interface should be sealed")
    void testSealedInterface() {
        // TransformResult should only be implemented by TransformResultImpl
        assertTrue(TransformResult.class.isSealed(),
            "TransformResult should be a sealed interface");
        
        Class<?>[] permitted = TransformResult.class.getPermittedSubclasses();
        assertEquals(2, permitted.length,
            "TransformResult should have exactly two permitted implementations");
        
        // Check that both expected implementations are present
        Set<String> permittedNames = Arrays.stream(permitted)
            .map(Class::getSimpleName)
            .collect(Collectors.toSet());
        assertTrue(permittedNames.contains("TransformResultImpl"),
            "TransformResultImpl should implement TransformResult");
        assertTrue(permittedNames.contains("PaddedTransformResult"),
            "PaddedTransformResult should implement TransformResult");
    }
}