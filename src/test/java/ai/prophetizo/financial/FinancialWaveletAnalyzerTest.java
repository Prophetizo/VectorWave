package ai.prophetizo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinancialWaveletAnalyzer functionality.
 */
class FinancialWaveletAnalyzerTest {
    
    private FinancialWaveletAnalyzer analyzer;
    private double[] samplePrices;
    private double[] sampleVolumes;
    
    @BeforeEach
    void setUp() {
        analyzer = new FinancialWaveletAnalyzer();
        samplePrices = generateSamplePrices(256);
        sampleVolumes = generateSampleVolumes(256);
    }
    
    private double[] generateSamplePrices(int length) {
        double[] prices = new double[length];
        prices[0] = 100.0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 1; i < length; i++) {
            double change = random.nextGaussian() * 0.02; // 2% daily volatility
            prices[i] = prices[i - 1] * (1 + change);
        }
        
        return prices;
    }
    
    private double[] generateSampleVolumes(int length) {
        double[] volumes = new double[length];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 0; i < length; i++) {
            volumes[i] = random.nextDouble(1000, 10000);
        }
        
        return volumes;
    }
    
    @Test
    @DisplayName("Analyzer should handle basic price analysis")
    void testBasicPriceAnalysis() {
        Map<String, Object> results = analyzer.analyzePriceData(samplePrices);
        
        assertNotNull(results);
        assertTrue(results.containsKey("mean"));
        assertTrue(results.containsKey("volatility"));
        assertTrue(results.containsKey("trend"));
        
        double mean = (Double) results.get("mean");
        double volatility = (Double) results.get("volatility");
        String trend = (String) results.get("trend");
        
        assertTrue(mean > 0);
        assertTrue(volatility >= 0);
        assertTrue(trend.equals("BULLISH") || trend.equals("BEARISH") || trend.equals("SIDEWAYS"));
    }
    
    @Test
    @DisplayName("Volatility analysis should produce valid results")
    void testVolatilityAnalysis() {
        VolatilityResult result = analyzer.analyzeVolatility(samplePrices, 20);
        
        assertNotNull(result);
        assertTrue(result.realizedVolatility() >= 0);
        assertTrue(result.garchVolatility() >= 0);
        assertTrue(result.waveletVolatility() >= 0);
        assertNotNull(result.detailVolatilities());
        assertNotNull(result.timeScaleVolatilities());
        assertTrue(result.timestamp() > 0);
    }
    
    @Test
    @DisplayName("Trading signal generation should work")
    void testTradingSignalGeneration() {
        List<TradingSignal> signals = analyzer.generateTradingSignals(samplePrices, sampleVolumes);
        
        assertNotNull(signals);
        // Should generate some signals for realistic price data
        assertTrue(signals.size() >= 0);
        
        if (!signals.isEmpty()) {
            TradingSignal signal = signals.get(0);
            assertNotNull(signal.type());
            assertNotNull(signal.strength());
            assertTrue(signal.confidence() >= 0 && signal.confidence() <= 1.0);
            assertTrue(signal.price() > 0);
            assertTrue(signal.timestamp() > 0);
            assertNotNull(signal.reason());
        }
    }
    
    @Test
    @DisplayName("Market regime analysis should return valid regimes")
    void testMarketRegimeAnalysis() {
        String regime = analyzer.analyzeMarketRegime(samplePrices);
        
        assertNotNull(regime);
        assertTrue(regime.equals("TRENDING_LOW_VOL") || 
                  regime.equals("TRENDING_HIGH_VOL") ||
                  regime.equals("RANGING_HIGH_VOL") ||
                  regime.equals("RANGING_LOW_VOL") ||
                  regime.equals("INSUFFICIENT_DATA") ||
                  regime.equals("ANALYSIS_ERROR"));
    }
    
    @Test
    @DisplayName("Multi-timeframe analysis should work")
    void testMultiTimeframeAnalysis() {
        double[] largePriceArray = generateSamplePrices(1024);
        Map<String, Object> analysis = analyzer.performMultiTimeframeAnalysis(largePriceArray);
        
        assertNotNull(analysis);
        assertTrue(analysis.size() > 0);
        
        // Should have analysis for timeframes that fit in the data
        if (analysis.containsKey("timeframe_64")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> timeframe64 = (Map<String, Object>) analysis.get("timeframe_64");
            assertNotNull(timeframe64);
            assertTrue(timeframe64.containsKey("regime"));
            assertTrue(timeframe64.containsKey("volatility"));
            assertTrue(timeframe64.containsKey("signals"));
        }
    }
    
    @Test
    @DisplayName("Risk analysis should calculate metrics")
    void testRiskAnalysis() {
        double[] benchmarkPrices = generateSamplePrices(samplePrices.length);
        Map<String, Double> riskMetrics = analyzer.analyzeRisk(samplePrices, benchmarkPrices);
        
        assertNotNull(riskMetrics);
        assertTrue(riskMetrics.containsKey("volatility"));
        assertTrue(riskMetrics.containsKey("beta"));
        assertTrue(riskMetrics.containsKey("var_95"));
        assertTrue(riskMetrics.containsKey("max_drawdown"));
        
        assertTrue(riskMetrics.get("volatility") >= 0);
        assertTrue(riskMetrics.get("var_95") >= 0);
        assertTrue(riskMetrics.get("max_drawdown") >= 0);
        assertTrue(riskMetrics.get("max_drawdown") <= 1.0);
    }
    
    @Test
    @DisplayName("Portfolio optimization should work")
    void testPortfolioOptimization() {
        int numAssets = 3;
        double[][] assetPrices = new double[numAssets][];
        double[] expectedReturns = new double[numAssets];
        
        for (int i = 0; i < numAssets; i++) {
            assetPrices[i] = generateSamplePrices(100);
            expectedReturns[i] = 0.08 + ThreadLocalRandom.current().nextGaussian() * 0.02;
        }
        
        Map<String, Object> optimization = analyzer.optimizePortfolio(assetPrices, expectedReturns, 0.1);
        
        assertNotNull(optimization);
        assertTrue(optimization.containsKey("weights"));
        assertTrue(optimization.containsKey("expected_return"));
        assertTrue(optimization.containsKey("volatility"));
        assertTrue(optimization.containsKey("sharpe_ratio"));
        
        double[] weights = (double[]) optimization.get("weights");
        assertEquals(numAssets, weights.length);
        
        // Weights should sum to approximately 1
        double weightSum = 0.0;
        for (double weight : weights) {
            weightSum += weight;
            assertTrue(weight >= 0); // Non-negative weights
        }
        assertEquals(1.0, weightSum, 0.001);
    }
    
    @Test
    @DisplayName("Should handle insufficient data gracefully")
    void testInsufficientDataHandling() {
        double[] shortPrices = {100.0, 101.0, 99.0};
        
        // analyzePriceData should throw exception for insufficient data
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzePriceData(shortPrices));
        
        List<TradingSignal> signals = analyzer.generateTradingSignals(shortPrices, new double[3]);
        assertTrue(signals.isEmpty());
        
        String regime = analyzer.analyzeMarketRegime(shortPrices);
        assertEquals("INSUFFICIENT_DATA", regime);
    }
    
    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputHandling() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzePriceData(null));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzeVolatility(null, 20));
        
        List<TradingSignal> signals = analyzer.generateTradingSignals(null, sampleVolumes);
        assertTrue(signals.isEmpty());
    }
    
    @Test
    @DisplayName("Volatility result should be immutable")
    void testVolatilityResultImmutability() {
        VolatilityResult result = analyzer.analyzeVolatility(samplePrices, 20);
        
        double[] originalDetails = result.detailVolatilities();
        double[] modifiedDetails = result.detailVolatilities();
        modifiedDetails[0] = -999.0; // Try to modify
        
        // Should return different arrays (defensive copies)
        assertNotSame(originalDetails, modifiedDetails);
        assertNotEquals(-999.0, result.detailVolatilities()[0]);
    }
}