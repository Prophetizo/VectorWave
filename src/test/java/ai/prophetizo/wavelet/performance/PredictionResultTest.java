package ai.prophetizo.wavelet.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for PredictionResult record.
 * Tests constructor validation, methods, and edge cases.
 */
@DisplayName("PredictionResult Tests")
class PredictionResultTest {

    // === Constructor/Validation Tests ===
    
    @Test
    @DisplayName("Should create valid prediction result")
    void testConstructorValid() {
        PredictionResult result = new PredictionResult(100.0, 80.0, 120.0, 0.95);
        
        assertEquals(100.0, result.estimatedTime());
        assertEquals(80.0, result.lowerBound());
        assertEquals(120.0, result.upperBound());
        assertEquals(0.95, result.confidence());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -0.1, -100.0, -Double.MAX_VALUE})
    @DisplayName("Should reject negative estimated time")
    void testConstructorRejectsNegativeEstimatedTime(double negativeTime) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new PredictionResult(negativeTime, 50.0, 150.0, 0.9));
        
        assertTrue(exception.getMessage().contains("Estimated time cannot be negative"));
    }
    
    @ParameterizedTest
    @CsvSource({
        "-1.0, 100.0",   // Negative lower bound
        "101.0, 100.0"   // Lower bound > estimated time
    })
    @DisplayName("Should reject invalid lower bounds")
    void testConstructorRejectsInvalidLowerBound(double lowerBound, double estimatedTime) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new PredictionResult(estimatedTime, lowerBound, 150.0, 0.9));
        
        assertTrue(exception.getMessage().contains("Invalid lower bound"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {50.0, 99.9, 0.0})
    @DisplayName("Should reject upper bound < estimated time")
    void testConstructorRejectsInvalidUpperBound(double upperBound) {
        double estimatedTime = 100.0;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new PredictionResult(estimatedTime, 80.0, upperBound, 0.9));
        
        assertTrue(exception.getMessage().contains("Upper bound must be >= estimated time"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, 2.0, -1.0})
    @DisplayName("Should reject confidence outside [0,1]")
    void testConstructorRejectsInvalidConfidence(double confidence) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new PredictionResult(100.0, 80.0, 120.0, confidence));
        
        assertTrue(exception.getMessage().contains("Confidence must be between 0 and 1"));
    }
    
    @Test
    @DisplayName("Should accept edge case values")
    void testConstructorEdgeCases() {
        // Zero estimated time with matching bounds
        PredictionResult zeroTime = new PredictionResult(0.0, 0.0, 0.0, 0.5);
        assertEquals(0.0, zeroTime.estimatedTime());
        
        // Lower bound equals estimated time
        PredictionResult equalLower = new PredictionResult(100.0, 100.0, 120.0, 0.8);
        assertEquals(100.0, equalLower.lowerBound());
        
        // Upper bound equals estimated time
        PredictionResult equalUpper = new PredictionResult(100.0, 80.0, 100.0, 0.9);
        assertEquals(100.0, equalUpper.upperBound());
        
        // Confidence at boundaries
        PredictionResult zeroConfidence = new PredictionResult(100.0, 80.0, 120.0, 0.0);
        assertEquals(0.0, zeroConfidence.confidence());
        
        PredictionResult fullConfidence = new PredictionResult(100.0, 80.0, 120.0, 1.0);
        assertEquals(1.0, fullConfidence.confidence());
    }

    // === getUncertainty Tests ===
    
    @Test
    @DisplayName("Should calculate uncertainty correctly")
    void testGetUncertainty() {
        // Symmetric bounds: 100 ± 20 -> uncertainty = 20%
        PredictionResult result = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        assertEquals(0.2, result.getUncertainty(), 1e-10);
    }
    
    @ParameterizedTest
    @CsvSource({
        "100.0, 90.0, 110.0, 0.1",     // ±10% uncertainty
        "100.0, 50.0, 150.0, 0.5",     // ±50% uncertainty
        "100.0, 99.0, 101.0, 0.01",    // ±1% uncertainty
        "50.0, 40.0, 60.0, 0.2",       // ±20% uncertainty on different base
        "200.0, 180.0, 220.0, 0.1"     // ±10% uncertainty on larger value
    })
    @DisplayName("Should calculate uncertainty for various inputs")
    void testGetUncertaintyVariousInputs(double estimated, double lower, double upper, double expectedUncertainty) {
        PredictionResult result = new PredictionResult(estimated, lower, upper, 0.9);
        assertEquals(expectedUncertainty, result.getUncertainty(), 1e-10);
    }
    
    @Test
    @DisplayName("Should handle zero estimated time in uncertainty calculation")
    void testGetUncertaintyZeroEstimate() {
        PredictionResult result = new PredictionResult(0.0, 0.0, 0.0, 0.5);
        assertEquals(0.0, result.getUncertainty());
    }
    
    @Test
    @DisplayName("Should handle asymmetric bounds in uncertainty")
    void testGetUncertaintyAsymmetricBounds() {
        // Lower bound closer than upper bound: 100, [95, 130]
        // Range = 35, uncertainty = 35/(2*100) = 17.5%
        PredictionResult result = new PredictionResult(100.0, 95.0, 130.0, 0.8);
        assertEquals(0.175, result.getUncertainty(), 1e-10);
    }

    // === isHighConfidence Tests ===
    
    @Test
    @DisplayName("Should identify high confidence correctly")
    void testIsHighConfidence() {
        PredictionResult highConf = new PredictionResult(100.0, 90.0, 110.0, 0.95);
        assertTrue(highConf.isHighConfidence());
        
        PredictionResult exactlyHigh = new PredictionResult(100.0, 90.0, 110.0, 0.9);
        assertTrue(exactlyHigh.isHighConfidence());
        
        PredictionResult lowConf = new PredictionResult(100.0, 90.0, 110.0, 0.89);
        assertFalse(lowConf.isHighConfidence());
        
        PredictionResult veryLowConf = new PredictionResult(100.0, 90.0, 110.0, 0.5);
        assertFalse(veryLowConf.isHighConfidence());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.9, 0.95, 0.99, 1.0})
    @DisplayName("Should identify high confidence for boundary values")
    void testIsHighConfidenceBoundaries(double confidence) {
        PredictionResult result = new PredictionResult(100.0, 90.0, 110.0, confidence);
        assertTrue(result.isHighConfidence());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 0.89, 0.899})
    @DisplayName("Should identify low confidence correctly")
    void testIsLowConfidence(double confidence) {
        PredictionResult result = new PredictionResult(100.0, 90.0, 110.0, confidence);
        assertFalse(result.isHighConfidence());
    }

    // === format Tests ===
    
    @Test
    @DisplayName("Should format prediction correctly")
    void testFormat() {
        PredictionResult result = new PredictionResult(123.45, 100.12, 150.78, 0.92);
        String formatted = result.format();
        
        assertEquals("123.45 ms [100.12 - 150.78 ms] (92% confidence)", formatted);
    }
    
    @ParameterizedTest
    @CsvSource({
        "100.0, 80.0, 120.0, 0.95, '100.00 ms [80.00 - 120.00 ms] (95% confidence)'",
        "50.5, 45.2, 55.8, 0.88, '50.50 ms [45.20 - 55.80 ms] (88% confidence)'",
        "0.0, 0.0, 0.0, 1.0, '0.00 ms [0.00 - 0.00 ms] (100% confidence)'",
        "1234.567, 1000.123, 1500.999, 0.75, '1234.57 ms [1000.12 - 1500.00 ms] (75% confidence)'"
    })
    @DisplayName("Should format various predictions correctly")
    void testFormatVariousInputs(double estimated, double lower, double upper, double confidence, String expected) {
        PredictionResult result = new PredictionResult(estimated, lower, upper, confidence);
        assertEquals(expected, result.format());
    }

    // === summary Tests ===
    
    @Test
    @DisplayName("Should create correct summary")
    void testSummary() {
        // 100.0 with ±20% uncertainty -> "100.00±20% ms"
        PredictionResult result = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        String summary = result.summary();
        
        assertEquals("100.00±20% ms", summary);
    }
    
    @ParameterizedTest
    @CsvSource({
        "50.0, 45.0, 55.0, '50.00±10% ms'",     // ±10% uncertainty
        "200.0, 180.0, 220.0, '200.00±10% ms'", // ±10% uncertainty on larger value
        "100.0, 50.0, 150.0, '100.00±50% ms'",  // ±50% uncertainty
        "100.0, 99.0, 101.0, '100.00±1% ms'"    // ±1% uncertainty
    })
    @DisplayName("Should create summaries for various uncertainties")
    void testSummaryVariousInputs(double estimated, double lower, double upper, String expected) {
        PredictionResult result = new PredictionResult(estimated, lower, upper, 0.9);
        assertEquals(expected, result.summary());
    }
    
    @Test
    @DisplayName("Should handle zero uncertainty in summary")
    void testSummaryZeroUncertainty() {
        PredictionResult result = new PredictionResult(100.0, 100.0, 100.0, 1.0);
        String summary = result.summary();
        
        assertEquals("100.00±0% ms", summary);
    }
    
    @Test
    @DisplayName("Should handle zero estimated time in summary")
    void testSummaryZeroEstimate() {
        PredictionResult result = new PredictionResult(0.0, 0.0, 0.0, 0.5);
        String summary = result.summary();
        
        assertEquals("0.00±0% ms", summary);
    }

    // === Record Contract Tests ===
    
    @Test
    @DisplayName("Should implement record equality correctly")
    void testRecordEquality() {
        PredictionResult result1 = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        PredictionResult result2 = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        PredictionResult result3 = new PredictionResult(100.0, 80.0, 120.0, 0.8);
        
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        
        // Test with null
        assertNotEquals(result1, null);
        
        // Test with different type
        assertNotEquals(result1, "not a result");
    }
    
    @Test
    @DisplayName("Should implement record hashCode correctly")
    void testRecordHashCode() {
        PredictionResult result1 = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        PredictionResult result2 = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        PredictionResult result3 = new PredictionResult(100.0, 80.0, 120.0, 0.8);
        
        assertEquals(result1.hashCode(), result2.hashCode());
        assertNotEquals(result1.hashCode(), result3.hashCode());
    }
    
    @Test
    @DisplayName("Should implement record toString correctly")
    void testRecordToString() {
        PredictionResult result = new PredictionResult(100.0, 80.0, 120.0, 0.9);
        String toString = result.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("100.0"));
        assertTrue(toString.contains("80.0"));
        assertTrue(toString.contains("120.0"));
        assertTrue(toString.contains("0.9"));
    }

    // === Integration Tests ===
    
    @Test
    @DisplayName("Should work in realistic performance scenarios")
    void testRealisticScenarios() {
        // Fast operation with high confidence
        PredictionResult fast = new PredictionResult(10.5, 9.0, 12.0, 0.95);
        assertTrue(fast.isHighConfidence());
        assertTrue(fast.getUncertainty() < 0.2); // Less than 20% uncertainty
        
        // Slow operation with lower confidence
        PredictionResult slow = new PredictionResult(5000.0, 3000.0, 8000.0, 0.75);
        assertFalse(slow.isHighConfidence());
        assertEquals(0.5, slow.getUncertainty(), 1e-10); // 50% uncertainty
        
        // Very precise prediction
        PredictionResult precise = new PredictionResult(100.0, 99.5, 100.5, 0.99);
        assertTrue(precise.isHighConfidence());
        assertEquals(0.005, precise.getUncertainty(), 1e-10); // 0.5% uncertainty
    }
    
    @Test
    @DisplayName("Should maintain consistency across methods")
    void testMethodConsistency() {
        PredictionResult result = new PredictionResult(200.0, 150.0, 250.0, 0.85);
        
        // Uncertainty calculation should be consistent with bounds
        double expectedUncertainty = (250.0 - 150.0) / (2 * 200.0);
        assertEquals(expectedUncertainty, result.getUncertainty(), 1e-10);
        
        // High confidence threshold should be consistent
        assertFalse(result.isHighConfidence()); // 0.85 < 0.9
        
        // Format and summary should contain consistent information
        String format = result.format();
        String summary = result.summary();
        
        assertTrue(format.contains("200.00"));
        assertTrue(summary.contains("200.00"));
        assertTrue(format.contains("85%"));
        
        // Summary should show correct uncertainty percentage
        assertTrue(summary.contains("25%")); // 25% uncertainty
    }
    
    @Test
    @DisplayName("Should handle extreme but valid values")
    void testExtremeValues() {
        // Very large values
        PredictionResult large = new PredictionResult(1e6, 5e5, 2e6, 0.8);
        assertDoesNotThrow(() -> large.format());
        assertDoesNotThrow(() -> large.summary());
        assertEquals(0.75, large.getUncertainty(), 1e-10);
        
        // Very small values
        PredictionResult small = new PredictionResult(0.001, 0.0005, 0.002, 0.6);
        assertDoesNotThrow(() -> small.format());
        assertDoesNotThrow(() -> small.summary());
        assertEquals(0.75, small.getUncertainty(), 1e-10);
        
        // Perfect confidence
        PredictionResult perfect = new PredictionResult(100.0, 100.0, 100.0, 1.0);
        assertTrue(perfect.isHighConfidence());
        assertEquals(0.0, perfect.getUncertainty());
        
        // No confidence
        PredictionResult noConfidence = new PredictionResult(100.0, 50.0, 200.0, 0.0);
        assertFalse(noConfidence.isHighConfidence());
        assertEquals(0.75, noConfidence.getUncertainty(), 1e-10);
    }
}