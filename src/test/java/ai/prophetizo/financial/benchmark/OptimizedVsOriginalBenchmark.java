package ai.prophetizo.financial.benchmark;

import ai.prophetizo.financial.FinancialWaveletAnalyzer;
import ai.prophetizo.financial.OptimizedFinancialWaveletAnalyzer;
import ai.prophetizo.financial.TradingSignal;
import ai.prophetizo.financial.VolatilityResult;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive benchmark comparing original vs optimized FinancialWaveletAnalyzer.
 * Measures both performance and GC pressure.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class OptimizedVsOriginalBenchmark {
    
    private FinancialWaveletAnalyzer originalAnalyzer;
    private OptimizedFinancialWaveletAnalyzer optimizedAnalyzer;
    
    private double[] smallPrices;
    private double[] mediumPrices;
    private double[] largePrices;
    private double[] smallVolumes;
    private double[] mediumVolumes;
    private double[] largeVolumes;
    
    @Setup
    public void setup() {
        originalAnalyzer = new FinancialWaveletAnalyzer();
        optimizedAnalyzer = new OptimizedFinancialWaveletAnalyzer();
        
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
    
    // ========== VOLATILITY ANALYSIS BENCHMARKS ==========
    
    @Benchmark
    public void originalVolatilityAnalysisSmall(Blackhole bh) {
        VolatilityResult result = originalAnalyzer.analyzeVolatility(smallPrices, 20);
        bh.consume(result);
    }
    
    @Benchmark
    public void optimizedVolatilityAnalysisSmall(Blackhole bh) {
        VolatilityResult result = optimizedAnalyzer.analyzeVolatility(smallPrices, 20);
        bh.consume(result);
    }
    
    @Benchmark
    public void originalVolatilityAnalysisMedium(Blackhole bh) {
        VolatilityResult result = originalAnalyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(result);
    }
    
    @Benchmark
    public void optimizedVolatilityAnalysisMedium(Blackhole bh) {
        VolatilityResult result = optimizedAnalyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(result);
    }
    
    @Benchmark
    public void originalVolatilityAnalysisLarge(Blackhole bh) {
        VolatilityResult result = originalAnalyzer.analyzeVolatility(largePrices, 50);
        bh.consume(result);
    }
    
    @Benchmark
    public void optimizedVolatilityAnalysisLarge(Blackhole bh) {
        VolatilityResult result = optimizedAnalyzer.analyzeVolatility(largePrices, 50);
        bh.consume(result);
    }
    
    // ========== TRADING SIGNALS BENCHMARKS ==========
    
    @Benchmark
    public void originalTradingSignalsSmall(Blackhole bh) {
        List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void optimizedTradingSignalsSmall(Blackhole bh) {
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void originalTradingSignalsMedium(Blackhole bh) {
        List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void optimizedTradingSignalsMedium(Blackhole bh) {
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void originalTradingSignalsLarge(Blackhole bh) {
        List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(largePrices, largeVolumes);
        bh.consume(signals);
    }
    
    @Benchmark
    public void optimizedTradingSignalsLarge(Blackhole bh) {
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(largePrices, largeVolumes);
        bh.consume(signals);
    }
    
    // ========== MARKET REGIME BENCHMARKS ==========
    
    @Benchmark
    public void originalMarketRegime(Blackhole bh) {
        String regime = originalAnalyzer.analyzeMarketRegime(mediumPrices);
        bh.consume(regime);
    }
    
    @Benchmark
    public void optimizedMarketRegime(Blackhole bh) {
        String regime = optimizedAnalyzer.analyzeMarketRegime(mediumPrices);
        bh.consume(regime);
    }
    
    // ========== COMPREHENSIVE PIPELINE BENCHMARKS ==========
    
    @Benchmark
    public void originalFullAnalysisPipeline(Blackhole bh) {
        // Hot spot #1: Volatility analysis
        VolatilityResult volatility = originalAnalyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(volatility);
        
        // Hot spot #2: Trading signals
        List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
        
        // Additional analysis
        String regime = originalAnalyzer.analyzeMarketRegime(mediumPrices);
        bh.consume(regime);
    }
    
    @Benchmark
    public void optimizedFullAnalysisPipeline(Blackhole bh) {
        // Hot spot #1: Volatility analysis
        VolatilityResult volatility = optimizedAnalyzer.analyzeVolatility(mediumPrices, 30);
        bh.consume(volatility);
        
        // Hot spot #2: Trading signals
        List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(mediumPrices, mediumVolumes);
        bh.consume(signals);
        
        // Additional analysis
        String regime = optimizedAnalyzer.analyzeMarketRegime(mediumPrices);
        bh.consume(regime);
    }
    
    // ========== GC PRESSURE STRESS TESTS ==========
    
    @Benchmark
    @OperationsPerInvocation(100)
    public void originalRepeatedVolatilityAnalysis(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            VolatilityResult result = originalAnalyzer.analyzeVolatility(smallPrices, 20);
            bh.consume(result);
        }
    }
    
    @Benchmark
    @OperationsPerInvocation(100)
    public void optimizedRepeatedVolatilityAnalysis(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            VolatilityResult result = optimizedAnalyzer.analyzeVolatility(smallPrices, 20);
            bh.consume(result);
        }
    }
    
    @Benchmark
    @OperationsPerInvocation(50)
    public void originalRepeatedTradingSignals(Blackhole bh) {
        for (int i = 0; i < 50; i++) {
            List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
            bh.consume(signals);
        }
    }
    
    @Benchmark
    @OperationsPerInvocation(50)
    public void optimizedRepeatedTradingSignals(Blackhole bh) {
        for (int i = 0; i < 50; i++) {
            List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
            bh.consume(signals);
        }
    }
    
    // ========== MIXED WORKLOAD BENCHMARK ==========
    
    @Benchmark
    @OperationsPerInvocation(30)
    public void originalMixedWorkload(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            VolatilityResult volatility = originalAnalyzer.analyzeVolatility(smallPrices, 20);
            bh.consume(volatility);
            
            List<TradingSignal> signals = originalAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
            bh.consume(signals);
            
            String regime = originalAnalyzer.analyzeMarketRegime(smallPrices);
            bh.consume(regime);
        }
    }
    
    @Benchmark
    @OperationsPerInvocation(30)
    public void optimizedMixedWorkload(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            VolatilityResult volatility = optimizedAnalyzer.analyzeVolatility(smallPrices, 20);
            bh.consume(volatility);
            
            List<TradingSignal> signals = optimizedAnalyzer.generateTradingSignals(smallPrices, smallVolumes);
            bh.consume(signals);
            
            String regime = optimizedAnalyzer.analyzeMarketRegime(smallPrices);
            bh.consume(regime);
        }
    }
}