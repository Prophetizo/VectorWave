package ai.prophetizo.financial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for FinancialConfig.
 * Tests all public methods including constructor validation, getters, and Object methods.
 */
@DisplayName("FinancialConfig Tests")
class FinancialConfigTest {

    // === Constructor Tests ===
    
    @Test
    @DisplayName("Should create config with valid risk-free rate")
    void testConstructorValid() {
        double riskFreeRate = 0.045; // 4.5%
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        
        assertEquals(riskFreeRate, config.getRiskFreeRate(), 1e-15);
    }
    
    @Test
    @DisplayName("Should create config with zero risk-free rate")
    void testConstructorZeroRate() {
        FinancialConfig config = new FinancialConfig(0.0);
        assertEquals(0.0, config.getRiskFreeRate(), 1e-15);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-0.001, -0.05, -1.0, -Double.MAX_VALUE})
    @DisplayName("Should reject negative risk-free rate")
    void testConstructorRejectsNegativeRate(double negativeRate) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new FinancialConfig(negativeRate));
        
        assertTrue(exception.getMessage().contains("Risk-free rate cannot be negative"));
        assertTrue(exception.getMessage().contains(String.valueOf(negativeRate)));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.001, 0.025, 0.045, 0.10, 0.15, 1.0})
    @DisplayName("Should accept various valid risk-free rates")
    void testConstructorValidRates(double rate) {
        FinancialConfig config = new FinancialConfig(rate);
        assertEquals(rate, config.getRiskFreeRate(), 1e-15);
    }
    
    @Test
    @DisplayName("Should handle extreme but valid rates")
    void testConstructorExtremeRates() {
        // Very small positive rate
        FinancialConfig lowConfig = new FinancialConfig(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, lowConfig.getRiskFreeRate());
        
        // Very high rate
        FinancialConfig highConfig = new FinancialConfig(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, highConfig.getRiskFreeRate());
        
        // Infinite rate (theoretically valid but unrealistic)
        FinancialConfig infConfig = new FinancialConfig(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, infConfig.getRiskFreeRate());
    }
    
    @Test
    @DisplayName("Should handle NaN risk-free rate")
    void testConstructorNaN() {
        // NaN should be allowed by the constructor (it's not negative)
        // but the behavior with NaN might be undefined in calculations
        FinancialConfig config = new FinancialConfig(Double.NaN);
        assertTrue(Double.isNaN(config.getRiskFreeRate()));
    }

    // === getRiskFreeRate Tests ===
    
    @Test
    @DisplayName("getRiskFreeRate should return configured value")
    void testGetRiskFreeRate() {
        double expectedRate = 0.0375; // 3.75%
        FinancialConfig config = new FinancialConfig(expectedRate);
        
        assertEquals(expectedRate, config.getRiskFreeRate(), 1e-15);
    }
    
    @Test
    @DisplayName("getRiskFreeRate should maintain precision")
    void testGetRiskFreeRatePrecision() {
        double preciseRate = 0.04567891234567890; // High precision rate
        FinancialConfig config = new FinancialConfig(preciseRate);
        
        assertEquals(preciseRate, config.getRiskFreeRate(), 1e-15);
    }

    // === toString Tests ===
    
    @Test
    @DisplayName("toString should format rate with 4 decimal places")
    void testToString() {
        FinancialConfig config = new FinancialConfig(0.045);
        String result = config.toString();
        
        assertEquals("FinancialConfig{riskFreeRate=0.0450}", result);
    }
    
    @Test
    @DisplayName("toString should handle zero rate")
    void testToStringZero() {
        FinancialConfig config = new FinancialConfig(0.0);
        String result = config.toString();
        
        assertEquals("FinancialConfig{riskFreeRate=0.0000}", result);
    }
    
    @Test
    @DisplayName("toString should handle high precision rates")
    void testToStringHighPrecision() {
        FinancialConfig config = new FinancialConfig(0.123456789);
        String result = config.toString();
        
        // Should truncate to 4 decimal places
        assertEquals("FinancialConfig{riskFreeRate=0.1235}", result);
    }
    
    @Test
    @DisplayName("toString should handle very large rates")
    void testToStringLargeRate() {
        FinancialConfig config = new FinancialConfig(123.456789);
        String result = config.toString();
        
        assertEquals("FinancialConfig{riskFreeRate=123.4568}", result);
    }

    // === equals Tests ===
    
    @Test
    @DisplayName("equals should return true for same object")
    void testEqualsSameObject() {
        FinancialConfig config = new FinancialConfig(0.045);
        
        assertTrue(config.equals(config));
    }
    
    @Test
    @DisplayName("equals should return true for equal configs")
    void testEqualsEqualConfigs() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1)); // Symmetry
    }
    
    @Test
    @DisplayName("equals should return false for different rates")
    void testEqualsDifferentRates() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.046);
        
        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }
    
    @Test
    @DisplayName("equals should return false for null")
    void testEqualsNull() {
        FinancialConfig config = new FinancialConfig(0.045);
        
        assertFalse(config.equals(null));
    }
    
    @Test
    @DisplayName("equals should return false for different class")
    void testEqualsDifferentClass() {
        FinancialConfig config = new FinancialConfig(0.045);
        String notAConfig = "not a config";
        
        assertFalse(config.equals(notAConfig));
    }
    
    @Test
    @DisplayName("equals should handle edge cases")
    void testEqualsEdgeCases() {
        // Zero rates
        FinancialConfig zero1 = new FinancialConfig(0.0);
        FinancialConfig zero2 = new FinancialConfig(0.0);
        assertTrue(zero1.equals(zero2));
        
        // Very close but different rates
        FinancialConfig close1 = new FinancialConfig(0.045000000000001);
        FinancialConfig close2 = new FinancialConfig(0.045000000000002);
        assertFalse(close1.equals(close2));
        
        // NaN rates
        FinancialConfig nan1 = new FinancialConfig(Double.NaN);
        FinancialConfig nan2 = new FinancialConfig(Double.NaN);
        // NaN != NaN in double comparison, so these should be false
        assertFalse(nan1.equals(nan2));
        
        // Infinite rates  
        FinancialConfig inf1 = new FinancialConfig(Double.POSITIVE_INFINITY);
        FinancialConfig inf2 = new FinancialConfig(Double.POSITIVE_INFINITY);
        assertTrue(inf1.equals(inf2));
    }
    
    @Test
    @DisplayName("equals should satisfy transitivity")
    void testEqualsTransitivity() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        FinancialConfig config3 = new FinancialConfig(0.045);
        
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config3));
        assertTrue(config1.equals(config3)); // Transitivity
    }

    // === hashCode Tests ===
    
    @Test
    @DisplayName("hashCode should be consistent")
    void testHashCodeConsistent() {
        FinancialConfig config = new FinancialConfig(0.045);
        
        int hash1 = config.hashCode();
        int hash2 = config.hashCode();
        
        assertEquals(hash1, hash2);
    }
    
    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void testHashCodeEqualObjects() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        
        assertEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    @DisplayName("hashCode should likely differ for different rates")
    void testHashCodeDifferentRates() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.046);
        
        // While not guaranteed by the contract, hash codes should typically differ
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    @DisplayName("hashCode should handle edge cases")
    void testHashCodeEdgeCases() {
        // Zero
        FinancialConfig zero = new FinancialConfig(0.0);
        assertNotNull(zero.hashCode()); // Should not throw
        
        // Very large value
        FinancialConfig large = new FinancialConfig(Double.MAX_VALUE);
        assertNotNull(large.hashCode());
        
        // Very small value
        FinancialConfig small = new FinancialConfig(Double.MIN_VALUE);
        assertNotNull(small.hashCode());
        
        // NaN
        FinancialConfig nan = new FinancialConfig(Double.NaN);
        assertNotNull(nan.hashCode());
        
        // Infinity
        FinancialConfig inf = new FinancialConfig(Double.POSITIVE_INFINITY);
        assertNotNull(inf.hashCode());
    }

    // === Integration Tests ===
    
    @Test
    @DisplayName("Should work in realistic financial scenarios")
    void testRealisticScenarios() {
        // US 10-year Treasury (historical average)
        FinancialConfig historicalUS = new FinancialConfig(0.044);
        assertEquals(0.044, historicalUS.getRiskFreeRate());
        
        // European Central Bank rates (low)
        FinancialConfig lowECB = new FinancialConfig(0.0025);
        assertEquals(0.0025, lowECB.getRiskFreeRate());
        
        // High inflation period rates  
        FinancialConfig highInflation = new FinancialConfig(0.12);
        assertEquals(0.12, highInflation.getRiskFreeRate());
        
        // All should be unique
        assertNotEquals(historicalUS, lowECB);
        assertNotEquals(lowECB, highInflation);
        assertNotEquals(historicalUS, highInflation);
    }
    
    @Test
    @DisplayName("Should maintain object contract consistency")
    void testObjectContractConsistency() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        FinancialConfig config3 = new FinancialConfig(0.046);
        
        // Reflexivity
        assertTrue(config1.equals(config1));
        
        // Symmetry
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
        
        // Consistency with hashCode
        assertEquals(config1.hashCode(), config2.hashCode());
        
        // Inequality
        assertFalse(config1.equals(config3));
        assertFalse(config3.equals(config1));
    }
}