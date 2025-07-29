package ai.prophetizo.financial.integration;

import ai.prophetizo.financial.FinancialAnalysisConfig;
import ai.prophetizo.financial.FinancialAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating configurable thresholds in financial analysis.
 * These tests verify that the hardcoded threshold values mentioned in issue #65
 * have been successfully moved to configurable parameters.
 */
@DisplayName("Financial Analysis Integration Tests")
class FinancialAnalysisIntegrationTest {
    
    @Test
    @DisplayName("Issue #65: CRASH_ASYMMETRY_THRESHOLD = 10.0 should be configurable")
    void testCrashAsymmetryThresholdIsConfigurable() {
        // Verify the default matches the original hardcoded value
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.defaultConfig();
        assertEquals(10.0, defaultConfig.getCrashAsymmetryThreshold(), 1e-10,
                "Default crash asymmetry threshold should match original hardcoded value");
        
        // Verify we can customize it
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(5.0)
                .build();
        assertEquals(5.0, customConfig.getCrashAsymmetryThreshold(), 1e-10,
                "Crash asymmetry threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        double testAsymmetry = 7.5; // Between 5.0 and 10.0
        
        assertFalse(defaultAnalyzer.isCrashRisk(testAsymmetry), 
                "Default analyzer should not detect crash risk at 7.5 (below 10.0 threshold)");
        assertTrue(customAnalyzer.isCrashRisk(testAsymmetry), 
                "Custom analyzer should detect crash risk at 7.5 (above 5.0 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: VOLATILITY_LOW_THRESHOLD = 0.5 should be configurable")
    void testVolatilityLowThresholdIsConfigurable() {
        // Verify the default matches the original hardcoded value
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.defaultConfig();
        assertEquals(0.5, defaultConfig.getVolatilityLowThreshold(), 1e-10,
                "Default volatility low threshold should match original hardcoded value");
        
        // Verify we can customize it
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .volatilityLowThreshold(0.3)
                .build();
        assertEquals(0.3, customConfig.getVolatilityLowThreshold(), 1e-10,
                "Volatility low threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        double testVolatility = 0.4; // Between 0.3 and 0.5
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, 
                defaultAnalyzer.classifyVolatility(testVolatility),
                "Default analyzer should classify 0.4 as LOW (below 0.5 threshold)");
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, 
                customAnalyzer.classifyVolatility(testVolatility),
                "Custom analyzer should classify 0.4 as NORMAL (above 0.3 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: REGIME_TREND_THRESHOLD = 0.02 should be configurable")
    void testRegimeTrendThresholdIsConfigurable() {
        // Verify the default matches the original hardcoded value
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.defaultConfig();
        assertEquals(0.02, defaultConfig.getRegimeTrendThreshold(), 1e-10,
                "Default regime trend threshold should match original hardcoded value");
        
        // Verify we can customize it
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .regimeTrendThreshold(0.01)
                .build();
        assertEquals(0.01, customConfig.getRegimeTrendThreshold(), 1e-10,
                "Regime trend threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        double testTrendChange = 0.015; // Between 0.01 and 0.02
        
        assertFalse(defaultAnalyzer.isRegimeShift(testTrendChange), 
                "Default analyzer should not detect regime shift at 0.015 (below 0.02 threshold)");
        assertTrue(customAnalyzer.isRegimeShift(testTrendChange), 
                "Custom analyzer should detect regime shift at 0.015 (above 0.01 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: All hardcoded thresholds should be moved to configuration")
    void testAllHardcodedThresholdsAreConfigurable() {
        // Create a completely custom configuration
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(8.0)          // Was hardcoded at 10.0
                .volatilityLowThreshold(0.3)           // Was hardcoded at 0.5
                .volatilityHighThreshold(1.5)          // Additional configurable threshold
                .regimeTrendThreshold(0.015)           // Was hardcoded at 0.02
                .anomalyDetectionThreshold(2.5)        // Additional configurable threshold
                .windowSize(128)                       // Configurable window size
                .confidenceLevel(0.99)                 // Configurable confidence level
                .build();
        
        FinancialAnalyzer analyzer = new FinancialAnalyzer(customConfig);
        
        // Verify all configurations are applied
        assertEquals(8.0, analyzer.getConfig().getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.3, analyzer.getConfig().getVolatilityLowThreshold(), 1e-10);
        assertEquals(1.5, analyzer.getConfig().getVolatilityHighThreshold(), 1e-10);
        assertEquals(0.015, analyzer.getConfig().getRegimeTrendThreshold(), 1e-10);
        assertEquals(2.5, analyzer.getConfig().getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(128, analyzer.getConfig().getWindowSize());
        assertEquals(0.99, analyzer.getConfig().getConfidenceLevel(), 1e-10);
        
        // Verify analyzer works with custom configuration
        double[] testPrices = createTestData();
        assertDoesNotThrow(() -> {
            analyzer.analyzeCrashAsymmetry(testPrices);
            analyzer.analyzeVolatility(testPrices);
            analyzer.analyzeRegimeTrend(testPrices);
            analyzer.detectAnomalies(testPrices);
        }, "Analyzer should work with completely custom configuration");
    }
    
    @Test
    @DisplayName("Issue #65: Backward compatibility should be maintained")
    void testBackwardCompatibilityMaintained() {
        // Using default configuration should provide the same behavior as original hardcoded values
        FinancialAnalyzer defaultAnalyzer = FinancialAnalyzer.withDefaultConfig();
        
        // Verify default values match the original hardcoded constants from the issue
        FinancialAnalysisConfig config = defaultAnalyzer.getConfig();
        assertEquals(10.0, config.getCrashAsymmetryThreshold(), 1e-10, 
                "CRASH_ASYMMETRY_THRESHOLD default should be 10.0");
        assertEquals(0.5, config.getVolatilityLowThreshold(), 1e-10, 
                "VOLATILITY_LOW_THRESHOLD default should be 0.5");
        assertEquals(0.02, config.getRegimeTrendThreshold(), 1e-10, 
                "REGIME_TREND_THRESHOLD default should be 0.02");
        
        // Verify analyzer functions work with default configuration
        double[] testPrices = createTestData();
        assertDoesNotThrow(() -> {
            double asymmetry = defaultAnalyzer.analyzeCrashAsymmetry(testPrices);
            double volatility = defaultAnalyzer.analyzeVolatility(testPrices);
            double trendChange = defaultAnalyzer.analyzeRegimeTrend(testPrices);
            boolean hasAnomalies = defaultAnalyzer.detectAnomalies(testPrices);
            
            // All results should be finite and valid
            assertTrue(Double.isFinite(asymmetry) && asymmetry >= 0);
            assertTrue(Double.isFinite(volatility) && volatility >= 0);
            assertTrue(Double.isFinite(trendChange) && trendChange >= 0);
            // hasAnomalies is just a boolean, so any value is valid
        }, "Default analyzer should maintain backward compatibility");
    }
    
    @Test
    @DisplayName("Issue #65: Configuration should allow per-analysis customization")
    void testPerAnalysisCustomization() {
        // Demonstrate different configurations for different use cases
        
        // High-frequency trading configuration
        FinancialAnalysisConfig hftConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(5.0)       // More sensitive
                .volatilityLowThreshold(0.1)        // Lower volatility range
                .regimeTrendThreshold(0.005)        // More sensitive to changes
                .windowSize(64)                     // Smaller window for faster analysis
                .build();
        
        // Long-term investment analysis configuration
        FinancialAnalysisConfig longTermConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(15.0)      // Less sensitive to short-term fluctuations
                .volatilityLowThreshold(1.0)        // Higher volatility range
                .regimeTrendThreshold(0.05)         // Less sensitive to minor changes
                .windowSize(512)                    // Larger window for trend analysis
                .build();
        
        FinancialAnalyzer hftAnalyzer = new FinancialAnalyzer(hftConfig);
        FinancialAnalyzer longTermAnalyzer = new FinancialAnalyzer(longTermConfig);
        
        double[] testPrices = createTestData();
        
        // Both analyzers should work but may produce different results due to different configurations
        assertDoesNotThrow(() -> {
            double hftAsymmetry = hftAnalyzer.analyzeCrashAsymmetry(testPrices);
            double longTermAsymmetry = longTermAnalyzer.analyzeCrashAsymmetry(testPrices);
            
            assertTrue(Double.isFinite(hftAsymmetry));
            assertTrue(Double.isFinite(longTermAsymmetry));
        }, "Different configurations should allow per-analysis customization");
    }
    
    private double[] createTestData() {
        // Create a simple test dataset with power-of-2 length
        double[] prices = new double[16];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            // Simple random walk
            double change = (Math.random() - 0.5) * 0.02;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
}