package ai.prophetizo.financial.benchmark;

import ai.prophetizo.financial.FinancialWaveletAnalyzer;
import ai.prophetizo.financial.TradingSignal;
import ai.prophetizo.financial.VolatilityResult;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for measuring GC pressure and performance of FinancialWaveletAnalyzer.
 * This measures the baseline performance before optimization.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class FinancialAnalyzerBenchmark {
    
    private FinancialWaveletAnalyzer analyzer;
    private double[] smallPrices;
    private double[] mediumPrices;
    private double[] largePrices;
    private double[] smallVolumes;
    private double[] mediumVolumes;
    private double[] largeVolumes;
    
    @Setup
    public void setup() {
        analyzer = new FinancialWaveletAnalyzer();
        
        // Generate test data of different sizes
        smallPrices = generatePrices(128);
        mediumPrices = generatePrices(512);
        largePrices = generatePrices(2048);
        
        smallVolumes = generateVolumes(128);
        mediumVolumes = generateVolumes(512);
        largeVolumes = generateVolumes(2048);
    }
    
    private double[] generatePrices(int length) {
        double[] prices = new double[length];
        prices[0] = 100.0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 1; i < length; i++) {
            double change = random.nextGaussian() * 0.02;
            prices[i] = prices[i - 1] * (1 + change);
        }
        
        return prices;
    }
    
    private double[] generateVolumes(int length) {
        double[] volumes = new double[length];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 0; i < length; i++) {
            volumes[i] = random.nextDouble(1000, 10000);
        }
        
        return volumes;
    }
    
    /**
     * Benchmark the analyzeVolatility method - HOT SPOT #1
     */
    @Benchmark
    public void benchmarkVolatilityAnalysisSmall(Blackhole bh) {
        VolatilityResult result = analyzer.analyzeVolatility(smallPrices, 20);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkVolatilityAnalysisMedium(Blackhole bh) {
        VolatilityResult result = analyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkVolatilityAnalysisLarge(Blackhole bh) {
        VolatilityResult result = analyzer.analyzeVolatility(largePrices, 50);
        bh.consume(result);
    }
    
    /**
     * Benchmark the generateTradingSignals method - HOT SPOT #2
     */
    @Benchmark
    public void benchmarkTradingSignalsSmall(Blackhole bh) {
        List<TradingSignal> signals = analyzer.generateTradingSignals(smallPrices, smallVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void benchmarkTradingSignalsMedium(Blackhole bh) {
        List<TradingSignal> signals = analyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void benchmarkTradingSignalsLarge(Blackhole bh) {
        List<TradingSignal> signals = analyzer.generateTradingSignals(largePrices, largeVolumes);
        bh.consume(signals);
    }
    
    /**
     * Benchmark multi-timeframe analysis which uses helper methods - HOT SPOT #3
     */
    @Benchmark
    public void benchmarkMultiTimeframeAnalysis(Blackhole bh) {
        var analysis = analyzer.performMultiTimeframeAnalysis(largePrices);
        bh.consume(analysis);
    }
    
    /**
     * Benchmark risk analysis which involves many helper method calls
     */
    @Benchmark
    public void benchmarkRiskAnalysis(Blackhole bh) {
        double[] benchmarkPrices = generatePrices(mediumPrices.length);
        var riskMetrics = analyzer.analyzeRisk(mediumPrices, benchmarkPrices);
        bh.consume(riskMetrics);
    }
    
    /**
     * Combined benchmark that exercises all hot spots
     */
    @Benchmark
    public void benchmarkFullAnalysisPipeline(Blackhole bh) {
        // Hot spot #1: Volatility analysis
        VolatilityResult volatility = analyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(volatility);
        
        // Hot spot #2: Trading signals
        List<TradingSignal> signals = analyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
        
        // Hot spot #3: Multi-timeframe (helper methods)
        var multiTimeframe = analyzer.performMultiTimeframeAnalysis(mediumPrices);
        bh.consume(multiTimeframe);
        
        // Additional analysis
        String regime = analyzer.analyzeMarketRegime(mediumPrices);
        bh.consume(regime);
    }
    
    /**
     * Stress test - repeatedly call volatile methods to measure GC pressure
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void benchmarkRepeatedVolatilityAnalysis(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            VolatilityResult result = analyzer.analyzeVolatility(smallPrices, 20);
            bh.consume(result);
        }
    }
    
    /**
     * Stress test - repeatedly generate trading signals
     */
    @Benchmark
    @OperationsPerInvocation(50) 
    public void benchmarkRepeatedTradingSignals(Blackhole bh) {
        for (int i = 0; i < 50; i++) {
            List<TradingSignal> signals = analyzer.generateTradingSignals(smallPrices, smallVolumes);
            bh.consume(signals);
        }
    }
}