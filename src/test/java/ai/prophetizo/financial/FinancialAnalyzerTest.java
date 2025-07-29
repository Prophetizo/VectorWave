package ai.prophetizo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinancialAnalyzer class.
 */
@DisplayName("Financial Analyzer Tests")
class FinancialAnalyzerTest {
    
    private FinancialAnalyzer analyzer;
    private FinancialAnalysisConfig config;
    
    @BeforeEach
    void setUp() {
        config = FinancialAnalysisConfig.defaultConfig();
        analyzer = new FinancialAnalyzer(config);
    }
    
    @Test
    @DisplayName("Constructor should reject null configuration")
    void testConstructorRejectsNullConfig() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FinancialAnalyzer(null)
        );
        assertTrue(exception.getMessage().contains("Configuration cannot be null"));
    }
    
    @Test
    @DisplayName("withDefaultConfig should create analyzer with default configuration")
    void testWithDefaultConfig() {
        FinancialAnalyzer defaultAnalyzer = FinancialAnalyzer.withDefaultConfig();
        
        assertNotNull(defaultAnalyzer);
        assertEquals(10.0, defaultAnalyzer.getConfig().getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.5, defaultAnalyzer.getConfig().getVolatilityLowThreshold(), 1e-10);
        assertEquals(0.02, defaultAnalyzer.getConfig().getRegimeTrendThreshold(), 1e-10);
    }
    
    @Test
    @DisplayName("getConfig should return the configured instance")
    void testGetConfig() {
        assertEquals(config, analyzer.getConfig());
    }
    
    @Test
    @DisplayName("analyzeCrashAsymmetry should reject null prices")
    void testAnalyzeCrashAsymmetryRejectsNullPrices() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyzeCrashAsymmetry(null)
        );
        assertTrue(exception.getMessage().contains("Prices cannot be null"));
    }
    
    @Test
    @DisplayName("analyzeCrashAsymmetry should reject prices with less than 2 elements")
    void testAnalyzeCrashAsymmetryRejectsShortPrices() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyzeCrashAsymmetry(new double[]{100.0})
        );
        assertTrue(exception.getMessage().contains("Prices must contain at least 2 elements"));
    }
    
    @Test
    @DisplayName("analyzeCrashAsymmetry should reject prices with NaN or infinite values")
    void testAnalyzeCrashAsymmetryRejectsInvalidValues() {
        double[] pricesWithNaN = {100.0, 101.0, Double.NaN, 102.0};
        double[] pricesWithInf = {100.0, 101.0, Double.POSITIVE_INFINITY, 102.0};
        
        IllegalArgumentException nanException = assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyzeCrashAsymmetry(pricesWithNaN)
        );
        assertTrue(nanException.getMessage().contains("Prices must contain only finite values"));
        
        IllegalArgumentException infException = assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyzeCrashAsymmetry(pricesWithInf)
        );
        assertTrue(infException.getMessage().contains("Prices must contain only finite values"));
    }
    
    @Test
    @DisplayName("analyzeCrashAsymmetry should return non-negative value for valid prices")
    void testAnalyzeCrashAsymmetryReturnsValidValue() {
        double[] prices = createTestPrices(16);
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        assertTrue(asymmetry >= 0.0, "Asymmetry should be non-negative");
        assertTrue(Double.isFinite(asymmetry), "Asymmetry should be finite");
    }
    
    @Test
    @DisplayName("analyzeVolatility should return non-negative value for valid prices")
    void testAnalyzeVolatilityReturnsValidValue() {
        double[] prices = createTestPrices(16);
        
        double volatility = analyzer.analyzeVolatility(prices);
        
        assertTrue(volatility >= 0.0, "Volatility should be non-negative");
        assertTrue(Double.isFinite(volatility), "Volatility should be finite");
    }
    
    @Test
    @DisplayName("analyzeRegimeTrend should return non-negative value for valid prices")
    void testAnalyzeRegimeTrendReturnsValidValue() {
        double[] prices = createTestPrices(16);
        
        double trendChange = analyzer.analyzeRegimeTrend(prices);
        
        assertTrue(trendChange >= 0.0, "Trend change should be non-negative");
        assertTrue(Double.isFinite(trendChange), "Trend change should be finite");
    }
    
    @Test
    @DisplayName("detectAnomalies should return boolean for valid prices")
    void testDetectAnomaliesReturnsBoolean() {
        double[] prices = createTestPrices(16);
        
        boolean hasAnomalies = analyzer.detectAnomalies(prices);
        
        // Should not throw and should return a boolean value
        assertNotNull(hasAnomalies);
    }
    
    @Test
    @DisplayName("classifyVolatility should return correct classification")
    void testClassifyVolatility() {
        double lowVolatility = config.getVolatilityLowThreshold() - 0.1;
        double normalVolatility = (config.getVolatilityLowThreshold() + config.getVolatilityHighThreshold()) / 2;
        double highVolatility = config.getVolatilityHighThreshold() + 0.1;
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, 
                analyzer.classifyVolatility(lowVolatility));
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, 
                analyzer.classifyVolatility(normalVolatility));
        assertEquals(FinancialAnalyzer.VolatilityClassification.HIGH, 
                analyzer.classifyVolatility(highVolatility));
    }
    
    @Test
    @DisplayName("isCrashRisk should use configured threshold")
    void testIsCrashRiskUsesThreshold() {
        double belowThreshold = config.getCrashAsymmetryThreshold() - 0.1;
        double aboveThreshold = config.getCrashAsymmetryThreshold() + 0.1;
        
        assertFalse(analyzer.isCrashRisk(belowThreshold));
        assertTrue(analyzer.isCrashRisk(aboveThreshold));
    }
    
    @Test
    @DisplayName("isRegimeShift should use configured threshold")
    void testIsRegimeShiftUsesThreshold() {
        double belowThreshold = config.getRegimeTrendThreshold() - 0.001;
        double aboveThreshold = config.getRegimeTrendThreshold() + 0.001;
        
        assertFalse(analyzer.isRegimeShift(belowThreshold));
        assertTrue(analyzer.isRegimeShift(aboveThreshold));
    }
    
    @Test
    @DisplayName("Analysis methods should work with different price patterns")
    void testAnalysisWithDifferentPatterns() {
        // Test with trending prices
        double[] trendingPrices = createTrendingPrices(16, 100.0, 0.01);
        assertDoesNotThrow(() -> {
            analyzer.analyzeCrashAsymmetry(trendingPrices);
            analyzer.analyzeVolatility(trendingPrices);
            analyzer.analyzeRegimeTrend(trendingPrices);
            analyzer.detectAnomalies(trendingPrices);
        });
        
        // Test with volatile prices
        double[] volatilePrices = createVolatilePrices(16, 100.0, 0.05);
        assertDoesNotThrow(() -> {
            analyzer.analyzeCrashAsymmetry(volatilePrices);
            analyzer.analyzeVolatility(volatilePrices);
            analyzer.analyzeRegimeTrend(volatilePrices);
            analyzer.detectAnomalies(volatilePrices);
        });
        
        // Test with constant prices
        double[] constantPrices = createConstantPrices(16, 100.0);
        assertDoesNotThrow(() -> {
            analyzer.analyzeCrashAsymmetry(constantPrices);
            analyzer.analyzeVolatility(constantPrices);
            analyzer.analyzeRegimeTrend(constantPrices);
            analyzer.detectAnomalies(constantPrices);
        });
    }
    
    @Test
    @DisplayName("Custom configuration should affect analysis results")
    void testCustomConfigurationAffectsResults() {
        // Create analyzer with very low thresholds
        FinancialAnalysisConfig lowThresholdConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.01)
                .volatilityLowThreshold(0.01)
                .volatilityHighThreshold(0.02)
                .regimeTrendThreshold(0.001)
                .anomalyDetectionThreshold(0.5)
                .build();
        
        FinancialAnalyzer lowThresholdAnalyzer = new FinancialAnalyzer(lowThresholdConfig);
        
        // Use moderately volatile prices - create deterministic data for consistent testing
        double[] prices = new double[16];
        prices[0] = 100.0;
        for (int i = 1; i < prices.length; i++) {
            // Create alternating large changes to ensure high volatility
            double change = (i % 2 == 0) ? 0.05 : -0.05;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        double asymmetry = lowThresholdAnalyzer.analyzeCrashAsymmetry(prices);
        double volatility = lowThresholdAnalyzer.analyzeVolatility(prices);
        double trendChange = lowThresholdAnalyzer.analyzeRegimeTrend(prices);
        
        // Verify that analysis methods return finite values
        assertTrue(Double.isFinite(asymmetry), "Asymmetry should be finite");
        assertTrue(Double.isFinite(volatility), "Volatility should be finite");
        assertTrue(Double.isFinite(trendChange), "Trend change should be finite");
        
        // Test that thresholds are actually used by testing a specific case
        // Test with a value that should be classified as high with our low threshold
        double testVolatility = 0.03; // Above 0.02 threshold
        assertEquals(FinancialAnalyzer.VolatilityClassification.HIGH, 
                lowThresholdAnalyzer.classifyVolatility(testVolatility));
        
        // Test with default analyzer for comparison
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, 
                analyzer.classifyVolatility(testVolatility)); // 0.03 is below default 0.5 threshold
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Analysis should work with different signal lengths")
    void testAnalysisWithDifferentLengths(int length) {
        double[] prices = createTestPrices(length);
        
        assertDoesNotThrow(() -> {
            double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
            double volatility = analyzer.analyzeVolatility(prices);
            double trendChange = analyzer.analyzeRegimeTrend(prices);
            boolean hasAnomalies = analyzer.detectAnomalies(prices);
            
            assertTrue(Double.isFinite(asymmetry));
            assertTrue(Double.isFinite(volatility));
            assertTrue(Double.isFinite(trendChange));
            assertNotNull(hasAnomalies);
        });
    }
    
    // Helper methods for creating test data
    
    private double[] createTestPrices(int length) {
        double[] prices = new double[length];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < length; i++) {
            // Simple random walk with small changes
            double change = (Math.random() - 0.5) * 0.02;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
    
    private double[] createTrendingPrices(int length, double startPrice, double trendRate) {
        double[] prices = new double[length];
        prices[0] = startPrice;
        
        for (int i = 1; i < length; i++) {
            prices[i] = prices[i-1] * (1 + trendRate);
        }
        
        return prices;
    }
    
    private double[] createVolatilePrices(int length, double basePrice, double volatility) {
        double[] prices = new double[length];
        prices[0] = basePrice;
        
        for (int i = 1; i < length; i++) {
            double change = (Math.random() - 0.5) * volatility;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
    
    private double[] createConstantPrices(int length, double price) {
        double[] prices = new double[length];
        for (int i = 0; i < length; i++) {
            prices[i] = price;
        }
        return prices;
    }
}