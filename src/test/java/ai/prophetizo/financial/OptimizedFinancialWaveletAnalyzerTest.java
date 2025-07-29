package ai.prophetizo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OptimizedFinancialWaveletAnalyzer functionality.
 * Ensures the optimized version produces the same results as the original.
 */
class OptimizedFinancialWaveletAnalyzerTest {
    
    private OptimizedFinancialWaveletAnalyzer optimizedAnalyzer;
    private FinancialWaveletAnalyzer originalAnalyzer;
    private double[] samplePrices;
    private double[] sampleVolumes;
    
    @BeforeEach
    void setUp() {
        optimizedAnalyzer = new OptimizedFinancialWaveletAnalyzer();
        originalAnalyzer = new FinancialWaveletAnalyzer();
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
    @DisplayName("Optimized analyzer should handle basic price analysis")
    void testBasicPriceAnalysis() {
        Map<String, Object> results = optimizedAnalyzer.analyzePriceData(samplePrices);
        
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
    @DisplayName("Optimized volatility analysis should produce similar results to original")
    void testVolatilityAnalysisConsistency() {
        VolatilityResult optimizedResult = optimizedAnalyzer.analyzeVolatility(samplePrices, 20);
        VolatilityResult originalResult = originalAnalyzer.analyzeVolatility(samplePrices, 20);
        
        assertNotNull(optimizedResult);
        assertNotNull(originalResult);
        
        // Results should be very close (within 1% tolerance due to potential numerical differences)
        double tolerance = 0.01;
        assertEquals(originalResult.realizedVolatility(), optimizedResult.realizedVolatility(), 
                    originalResult.realizedVolatility() * tolerance);
        assertEquals(originalResult.garchVolatility(), optimizedResult.garchVolatility(), 
                    originalResult.garchVolatility() * tolerance);
        assertEquals(originalResult.waveletVolatility(), optimizedResult.waveletVolatility(), 
                    originalResult.waveletVolatility() * tolerance);
    }
    
    @Test
    @DisplayName("Optimized trading signal generation should work")
    void testTradingSignalGeneration() {
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(samplePrices, sampleVolumes);
        
        assertNotNull(signals);
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
    @DisplayName("Optimized market regime analysis should return valid regimes")
    void testMarketRegimeAnalysis() {
        String regime = optimizedAnalyzer.analyzeMarketRegime(samplePrices);
        
        assertNotNull(regime);
        assertTrue(regime.equals("TRENDING_LOW_VOL") || 
                  regime.equals("TRENDING_HIGH_VOL") ||
                  regime.equals("RANGING_HIGH_VOL") ||
                  regime.equals("RANGING_LOW_VOL") ||
                  regime.equals("INSUFFICIENT_DATA") ||
                  regime.equals("ANALYSIS_ERROR"));
    }
    
    @Test
    @DisplayName("Pool statistics should be available")
    void testPoolStatistics() {
        Map<String, Integer> stats = optimizedAnalyzer.getPoolStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("signal_pool_size"));
        assertTrue(stats.containsKey("reusable_arrays_count"));
        
        assertTrue(stats.get("signal_pool_size") >= 0);
        assertEquals(12, stats.get("reusable_arrays_count").intValue());
    }
    
    @Test
    @DisplayName("Should handle multiple calls without memory leaks")
    void testMemoryReuse() {
        // This test verifies that reusable arrays work across multiple calls
        
        // First call
        VolatilityResult result1 = optimizedAnalyzer.analyzeVolatility(samplePrices, 20);
        assertNotNull(result1);
        
        // Second call should reuse arrays
        VolatilityResult result2 = optimizedAnalyzer.analyzeVolatility(samplePrices, 30);
        assertNotNull(result2);
        
        // Results should be valid and different (different lookback windows)
        assertNotEquals(result1.realizedVolatility(), result2.realizedVolatility());
        
        // Multiple signal generation calls
        List<TradingSignal> signals1 = optimizedAnalyzer.generateTradingSignals(samplePrices, sampleVolumes);
        List<TradingSignal> signals2 = optimizedAnalyzer.generateTradingSignals(samplePrices, sampleVolumes);
        
        assertNotNull(signals1);
        assertNotNull(signals2);
        // Should produce same results with same input
        assertEquals(signals1.size(), signals2.size());
    }
    
    @Test
    @DisplayName("Should handle large datasets efficiently")
    void testLargeDatasetHandling() {
        double[] largePrices = generateSamplePrices(2048);
        double[] largeVolumes = generateSampleVolumes(2048);
        
        // Should not throw exceptions and complete in reasonable time
        assertDoesNotThrow(() -> {
            VolatilityResult result = optimizedAnalyzer.analyzeVolatility(largePrices, 50);
            assertNotNull(result);
        });
        
        assertDoesNotThrow(() -> {
            List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(largePrices, largeVolumes);
            assertNotNull(signals);
        });
        
        assertDoesNotThrow(() -> {
            String regime = optimizedAnalyzer.analyzeMarketRegime(largePrices);
            assertNotNull(regime);
        });
    }
    
    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
        double[] shortPrices = {100.0, 101.0, 99.0};
        
        // Should throw exception for insufficient data (same as original)
        assertThrows(IllegalArgumentException.class, () -> 
            optimizedAnalyzer.analyzePriceData(shortPrices));
        
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(shortPrices, new double[3]);
        assertTrue(signals.isEmpty());
        
        String regime = optimizedAnalyzer.analyzeMarketRegime(shortPrices);
        assertEquals("INSUFFICIENT_DATA", regime);
    }
    
    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputHandling() {
        assertThrows(IllegalArgumentException.class, () -> 
            optimizedAnalyzer.analyzePriceData(null));
        assertThrows(IllegalArgumentException.class, () -> 
            optimizedAnalyzer.analyzeVolatility(null, 20));
        
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(null, sampleVolumes);
        assertTrue(signals.isEmpty());
    }
}