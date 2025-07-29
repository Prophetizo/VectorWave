package ai.prophetizo.financial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.BoundaryMode;

/**
 * Unit tests for FinancialConfig class.
 */
class FinancialConfigTest {
    
    @Test
    @DisplayName("Default constructor should use default risk-free rate")
    void testDefaultConstructor() {
        FinancialConfig config = new FinancialConfig();
        assertEquals(FinancialConfig.DEFAULT_RISK_FREE_RATE, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    @DisplayName("Constructor with risk-free rate should set value correctly")
    void testConstructorWithRiskFreeRate() {
        double riskFreeRate = 0.03;
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        assertEquals(riskFreeRate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    @DisplayName("Constructor should reject negative risk-free rate")
    void testConstructorRejectsNegativeRate() {
        assertThrows(IllegalArgumentException.class, () -> new FinancialConfig(-0.01));
    }
    
    @Test
    @DisplayName("Constructor should accept zero risk-free rate")
    void testConstructorAcceptsZeroRate() {
        FinancialConfig config = new FinancialConfig(0.0);
        assertEquals(0.0, config.getRiskFreeRate(), 1e-10);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.025, 0.045, 0.1, 0.15})
    @DisplayName("Constructor should accept various valid risk-free rates")
    void testConstructorAcceptsValidRates(double rate) {
        FinancialConfig config = new FinancialConfig(rate);
        assertEquals(rate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    @DisplayName("toString should format rate correctly")
    void testToString() {
        FinancialConfig config = new FinancialConfig(0.045);
        String result = config.toString();
        assertTrue(result.contains("0.0450"));
    }
    
    @Test
    @DisplayName("equals should work correctly")
    void testEquals() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        FinancialConfig config3 = new FinancialConfig(0.03);
        
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertNotEquals(config1, null);
        assertNotEquals(config1, "not a config");
    }
    
    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        
        assertEquals(config1.hashCode(), config2.hashCode());
    }
}