package ai.prophetizo.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for simple streaming analyzer.
 */
class SimpleStreamingAnalyzerTest {
    
    private SimpleStreamingAnalyzer analyzer;
    private List<SimpleStreamingAnalyzer.StreamingResult> results;
    
    @BeforeEach
    void setUp() {
        analyzer = new SimpleStreamingAnalyzer(100, 10);
        results = new ArrayList<>();
        analyzer.onResult(results::add);
    }
    
    @Test
    @DisplayName("Should process streaming data and emit results")
    void testStreamingAnalysis() {
        // Generate test data
        for (int i = 0; i < 100; i++) {
            double price = 100 + Math.sin(i * 0.1) + (Math.random() - 0.5) * 0.5;
            analyzer.processSample(price);
        }
        
        // Should emit results every 10 samples
        assertEquals(10, results.size());
        
        // Verify results
        for (var result : results) {
            assertTrue(result.price() > 0);
            assertTrue(result.instantVolatility() >= 0);
            assertTrue(result.riskLevel() >= 0 && result.riskLevel() <= 1);
            assertNotNull(result.regime());
        }
    }
    
    @Test
    @DisplayName("Should detect volatility changes")
    void testVolatilityDetection() {
        // Low volatility period
        for (int i = 0; i < 50; i++) {
            analyzer.processSample(100 + Math.random() * 0.1);
        }
        
        double lowVolatility = results.get(results.size() - 1).avgVolatility();
        
        // High volatility period
        for (int i = 0; i < 50; i++) {
            analyzer.processSample(100 + (Math.random() - 0.5) * 5);
        }
        
        double highVolatility = results.get(results.size() - 1).avgVolatility();
        
        assertTrue(highVolatility > lowVolatility * 2, 
            "High volatility should be significantly higher");
    }
    
    @Test
    @DisplayName("Incremental analyzer should process efficiently")
    void testIncrementalAnalysis() {
        IncrementalFinancialAnalyzer incremental = new IncrementalFinancialAnalyzer();
        
        // Process samples
        List<IncrementalFinancialAnalyzer.IncrementalAnalysisResult> incrementalResults = 
            new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            double price = 100 + i * 0.01 + Math.sin(i * 0.1);
            var result = incremental.processSample(price, 1_000_000);
            incrementalResults.add(result);
        }
        
        // Verify incremental calculations
        assertEquals(1000, incrementalResults.size());
        
        // Check EMAs are calculated
        var lastResult = incrementalResults.get(999);
        assertTrue(lastResult.ema12() > 0);
        assertTrue(lastResult.ema26() > 0);
        assertTrue(lastResult.ema50() > 0);
        
        // Should detect upward trend
        boolean upTrendFound = incrementalResults.stream()
            .skip(100) // After warm-up
            .anyMatch(r -> r.regime() == FinancialWaveletAnalyzer.MarketRegime.TRENDING_UP);
        assertTrue(upTrendFound, "Should detect upward trend");
    }
    
    @Test
    @DisplayName("Should generate trading signals")
    void testSignalGeneration() {
        List<SimpleStreamingAnalyzer.StreamingResult> signalResults = new ArrayList<>();
        
        // Track only results with signals
        analyzer.onResult(result -> {
            if (result.signal().isPresent()) {
                signalResults.add(result);
            }
        });
        
        // Generate trending data
        for (int i = 0; i < 200; i++) {
            double trend = i * 0.05; // Strong upward trend
            double noise = (Math.random() - 0.5) * 0.2;
            analyzer.processSample(100 + trend + noise);
        }
        
        // Should generate some signals
        assertFalse(signalResults.isEmpty(), "Should generate some signals");
        
        // Check signal types
        boolean hasBuySignal = signalResults.stream()
            .anyMatch(r -> r.signal().get().type() == FinancialWaveletAnalyzer.SignalType.BUY);
        boolean hasSellSignal = signalResults.stream()
            .anyMatch(r -> r.signal().get().type() == FinancialWaveletAnalyzer.SignalType.SELL);
        
        assertTrue(hasBuySignal || hasSellSignal, "Should generate some trading signals");
    }
    
    @Test
    @DisplayName("Memory usage should remain constant")
    void testMemoryEfficiency() {
        // Process large amount of data
        long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        for (int i = 0; i < 100_000; i++) {
            analyzer.processSample(100 + Math.random());
        }
        
        // Force GC to get accurate memory reading
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryIncrease = afterMemory - beforeMemory;
        
        // Memory increase should be minimal (less than 1MB)
        assertTrue(memoryIncrease < 1_000_000, 
            "Memory usage should remain constant, increased by: " + memoryIncrease);
    }
}