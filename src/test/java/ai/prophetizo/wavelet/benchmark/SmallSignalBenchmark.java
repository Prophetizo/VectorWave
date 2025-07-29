package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for small signal performance.
 * Tests the integrated optimizations for signals < 1024 samples.
 * 
 * Run with: ./jmh-runner.sh SmallSignalBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512M", "-Xmx1G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class SmallSignalBenchmark {
    
    // Financial signal simulation constants
    private static final double PERIODIC_AMPLITUDE = 0.1;
    private static final double PERIODIC_WAVELENGTH = 20.0;
    
    @Param({"64", "128", "256", "512", "1024"})
    private int signalLength;
    
    @Param({"haar", "db2", "db4"})
    private String waveletName;
    
    private double[] signal;
    private Wavelet wavelet;
    private WaveletTransform transform;
    private double[][] batchSignals;
    
    @Setup
    public void setup() {
        // Use fixed seed for reproducible benchmarks
        Random random = new Random(42);
        
        // Create test signal with financial-like characteristics
        signal = new double[signalLength];
        double trend = 100.0;
        double volatility = 0.02;
        
        for (int i = 0; i < signalLength; i++) {
            // Brownian motion simulation
            trend *= (1 + volatility * (random.nextDouble() - 0.5));
            signal[i] = trend + PERIODIC_AMPLITUDE * Math.sin(2 * Math.PI * i / PERIODIC_WAVELENGTH); // Add some periodicity
        }
        
        // Initialize wavelets and transforms
        wavelet = WaveletRegistry.getWavelet(waveletName);
        transform = new WaveletTransform(wavelet, ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
        
        // Create batch signals
        batchSignals = new double[10][signalLength];
        for (int i = 0; i < 10; i++) {
            System.arraycopy(signal, 0, batchSignals[i], 0, signalLength);
        }
    }
    
    @Benchmark
    public void forward(Blackhole bh) {
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void forwardInverse(Blackhole bh) {
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    public void batchTransform(Blackhole bh) {
        TransformResult[] results = new TransformResult[batchSignals.length];
        for (int i = 0; i < batchSignals.length; i++) {
            results[i] = transform.forward(batchSignals[i]);
        }
        bh.consume(results);
    }
    
    /**
     * Benchmarks memory allocation overhead and GC pressure caused by defensive copying.
     * Tests the allocation cost in TransformResult during forward() and getter calls,
     * which create new arrays to ensure immutability. Measures the impact of these
     * allocations on performance over 100 iterations.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void memoryPressure(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            // Step 1: forward() allocates new arrays for transform results
            TransformResult result = transform.forward(signal);
            
            // Step 2: Each getter method creates defensive copies to ensure immutability
            // approximationCoeffs() allocates a new array of size signalSize/2
            double[] approx = result.approximationCoeffs();
            
            // detailCoeffs() allocates another new array of size signalSize/2
            double[] detail = result.detailCoeffs();
            
            // Total allocations per iteration:
            // - 2 arrays from forward() (internal to TransformResult)
            // - 2 arrays from defensive copying (approx + detail getters)
            // With 100 iterations, this creates 400 array allocations total
            
            bh.consume(approx);
            bh.consume(detail);
        }
    }
}