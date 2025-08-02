package ai.prophetizo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinancialAnalysisConfig class.
 */
@DisplayName("Financial Analysis Configuration Tests")
class FinancialAnalysisConfigTest {
    
    private FinancialAnalysisConfig testConfig;
    
    @BeforeEach
    void setUp() {
        // Create a test configuration with explicit values
        testConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
    }
    
    @Test
    @DisplayName("Configuration should store all values correctly")
    void testConfiguration() {
        assertEquals(0.7, testConfig.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.5, testConfig.getVolatilityLowThreshold(), 1e-10);
        assertEquals(0.02, testConfig.getRegimeTrendThreshold(), 1e-10);
        assertEquals(2.0, testConfig.getVolatilityHighThreshold(), 1e-10);
        assertEquals(3.0, testConfig.getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(256, testConfig.getWindowSize());
        assertEquals(0.95, testConfig.getConfidenceLevel(), 1e-10);
    }
    
    @Test
    @DisplayName("Builder should require all parameters")
    void testBuilderRequiresAllParameters() {
        // Test missing crash asymmetry threshold
        assertThrows(IllegalStateException.class, () -> 
            FinancialAnalysisConfig.builder()
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build()
        );
        
        // Test missing volatility low threshold
        assertThrows(IllegalStateException.class, () -> 
            FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build()
        );
        
        // Test missing window size
        assertThrows(IllegalStateException.class, () -> 
            FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .confidenceLevel(0.95)
                .build()
        );
    }
    
    @Test
    @DisplayName("Builder should validate volatility threshold relationship")
    void testBuilderValidatesVolatilityThresholds() {
        assertThrows(IllegalArgumentException.class, () -> 
            FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(2.0)  // Low > High
                .volatilityHighThreshold(1.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build()
        );
    }
    
    @Test
    @DisplayName("Builder should create custom configuration")
    void testBuilder() {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(5.0)
                .volatilityLowThreshold(0.3)
                .volatilityHighThreshold(1.5)
                .regimeTrendThreshold(0.01)
                .anomalyDetectionThreshold(2.5)
                .windowSize(128)
                .confidenceLevel(0.99)
                .build();
        
        assertEquals(5.0, config.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.3, config.getVolatilityLowThreshold(), 1e-10);
        assertEquals(1.5, config.getVolatilityHighThreshold(), 1e-10);
        assertEquals(0.01, config.getRegimeTrendThreshold(), 1e-10);
        assertEquals(2.5, config.getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(128, config.getWindowSize());
        assertEquals(0.99, config.getConfidenceLevel(), 1e-10);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.1})
    @DisplayName("Builder should reject non-positive crash asymmetry thresholds")
    void testBuilderRejectsNonPositiveCrashAsymmetryThreshold(double threshold) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.crashAsymmetryThreshold(threshold)
        );
        assertTrue(exception.getMessage().contains("Crash asymmetry threshold must be positive"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.1})
    @DisplayName("Builder should reject non-positive volatility low thresholds")
    void testBuilderRejectsNonPositiveVolatilityLowThreshold(double threshold) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.volatilityLowThreshold(threshold)
        );
        assertTrue(exception.getMessage().contains("Volatility low threshold must be positive"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.1})
    @DisplayName("Builder should reject non-positive volatility high thresholds")
    void testBuilderRejectsNonPositiveVolatilityHighThreshold(double threshold) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.volatilityHighThreshold(threshold)
        );
        assertTrue(exception.getMessage().contains("Volatility high threshold must be positive"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.1})
    @DisplayName("Builder should reject non-positive regime trend thresholds")
    void testBuilderRejectsNonPositiveRegimeTrendThreshold(double threshold) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.regimeTrendThreshold(threshold)
        );
        assertTrue(exception.getMessage().contains("Regime trend threshold must be positive"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.1})
    @DisplayName("Builder should reject non-positive anomaly detection thresholds")
    void testBuilderRejectsNonPositiveAnomalyDetectionThreshold(double threshold) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.anomalyDetectionThreshold(threshold)
        );
        assertTrue(exception.getMessage().contains("Anomaly detection threshold must be positive"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 5, 7, 9, 15, 17, 255, 257})
    @DisplayName("Builder should reject invalid window sizes")
    void testBuilderRejectsInvalidWindowSizes(int windowSize) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.windowSize(windowSize)
        );
        assertTrue(exception.getMessage().contains("Window size must be a power of 2 and at least 2"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("Builder should accept valid window sizes")
    void testBuilderAcceptsValidWindowSizes(int windowSize) {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(windowSize)
                .confidenceLevel(0.95)
                .build();
        
        assertEquals(windowSize, config.getWindowSize());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.0, -0.1, 1.1, 2.0})
    @DisplayName("Builder should reject invalid confidence levels")
    void testBuilderRejectsInvalidConfidenceLevels(double confidenceLevel) {
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.confidenceLevel(confidenceLevel)
        );
        assertTrue(exception.getMessage().contains("Confidence level must be between 0.0 and 1.0"));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.5, 0.9, 0.95, 0.99, 0.999})
    @DisplayName("Builder should accept valid confidence levels")
    void testBuilderAcceptsValidConfidenceLevels(double confidenceLevel) {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(confidenceLevel)
                .build();
        
        assertEquals(confidenceLevel, config.getConfidenceLevel(), 1e-10);
    }
    
    @Test
    @DisplayName("Builder should support method chaining")
    void testBuilderMethodChaining() {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(8.0)
                .volatilityLowThreshold(0.4)
                .volatilityHighThreshold(1.8)
                .regimeTrendThreshold(0.015)
                .anomalyDetectionThreshold(2.8)
                .windowSize(64)
                .confidenceLevel(0.98)
                .build();
        
        assertNotNull(config);
        assertEquals(8.0, config.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.4, config.getVolatilityLowThreshold(), 1e-10);
        assertEquals(1.8, config.getVolatilityHighThreshold(), 1e-10);
        assertEquals(0.015, config.getRegimeTrendThreshold(), 1e-10);
        assertEquals(2.8, config.getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(64, config.getWindowSize());
        assertEquals(0.98, config.getConfidenceLevel(), 1e-10);
    }
}