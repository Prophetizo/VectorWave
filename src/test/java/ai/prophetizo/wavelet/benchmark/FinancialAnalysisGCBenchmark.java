package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.cwt.finance.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Benchmark to measure GC pressure reduction in FinancialWaveletAnalyzer.
 * 
 * <p>Run with: ./jmh-runner.sh FinancialAnalysisGCBenchmark</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class FinancialAnalysisGCBenchmark {
    
    @Param({"256", "1024", "4096"})
    private int dataSize;
    
    private double[] priceData;
    private double[] volumeData;
    private FinancialWaveletAnalyzer analyzer;
    private FinancialAnalysisParameters parameters;
    
    @Setup
    public void setup() {
        // Generate realistic price data
        Random random = new Random(TestConstants.TEST_SEED);
        priceData = new double[dataSize];
        volumeData = new double[dataSize];
        
        // Start at $100
        priceData[0] = 100.0;
        volumeData[0] = 1_000_000;
        
        for (int i = 1; i < dataSize; i++) {
            // Random walk with slight upward bias
            double returnPct = (random.nextGaussian() * 0.02) + 0.0001;
            priceData[i] = priceData[i-1] * (1 + returnPct);
            
            // Volume with some correlation to price movement
            double volumeChange = Math.abs(returnPct) * 10 + random.nextGaussian() * 0.1;
            volumeData[i] = volumeData[i-1] * (1 + volumeChange);
        }
        
        // Add some crashes for more realistic testing
        for (int i = 0; i < 3 && i * 100 < dataSize; i++) {
            int crashPoint = Math.min(100 + i * 200, dataSize - 1);
            priceData[crashPoint] = priceData[crashPoint-1] * 0.95; // 5% drop
        }
        
        parameters = FinancialAnalysisParameters.defaultParameters();
        analyzer = new FinancialWaveletAnalyzer(parameters);
    }
    
    @Benchmark
    public void benchmarkVolatilityAnalysis(Blackhole bh) {
        FinancialWaveletAnalyzer.VolatilityAnalysisResult result = 
            analyzer.analyzeVolatility(priceData, 1.0);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkCrashDetection(Blackhole bh) {
        FinancialWaveletAnalyzer.CrashDetectionResult result = 
            analyzer.detectMarketCrashes(priceData, 1.0);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkTradingSignals(Blackhole bh) {
        FinancialWaveletAnalyzer.TradingSignalResult result = 
            analyzer.generateTradingSignals(priceData, 1.0);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkFullAnalysis(Blackhole bh) {
        MarketAnalysisRequest request = MarketAnalysisRequest.builder()
            .priceData(priceData)
            .volumeData(volumeData)
            .samplingRate(1.0)
            .build();
            
        FinancialWaveletAnalyzer.MarketAnalysisResult result = 
            analyzer.analyzeMarket(request);
        bh.consume(result);
    }
    
    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        // Get pool statistics after each iteration
        FinancialAnalysisObjectPool.PoolStatistics stats = analyzer.getPoolStatistics();
        System.out.printf("Pool stats - Hit rate: %.2f%%, Array hits: %d, misses: %d%n",
            stats.hitRate() * 100, stats.arrayHits(), stats.arrayMisses());
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(FinancialAnalysisGCBenchmark.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .build();
            
        new Runner(opt).run();
    }
}