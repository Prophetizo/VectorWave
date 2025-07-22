package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class SmallSignalBenchmark {
    
    @Param({"256", "512", "1024"})
    private int signalLength;
    
    @Param({"haar", "db2", "db4"})
    private String waveletName;
    
    private double[] signal;
    private Wavelet wavelet;
    private WaveletTransform transform;
    private double[][] batchSignals;
    
    @Setup
    public void setup() {
        // Create test signal with financial-like characteristics
        signal = new double[signalLength];
        double trend = 100.0;
        double volatility = 0.02;
        
        for (int i = 0; i < signalLength; i++) {
            // Brownian motion simulation
            trend *= (1 + volatility * (Math.random() - 0.5));
            signal[i] = trend + 0.1 * Math.sin(2 * Math.PI * i / 20); // Add some periodicity
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
    @BenchmarkMode(Mode.Throughput)
    public void batchTransform(Blackhole bh) {
        TransformResult[] results = new TransformResult[batchSignals.length];
        for (int i = 0; i < batchSignals.length; i++) {
            results[i] = transform.forward(batchSignals[i]);
        }
        bh.consume(results);
    }
    
    /**
     * Measure memory allocation rate
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void memoryPressure(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            TransformResult result = transform.forward(signal);
            // Force defensive copy
            double[] approx = result.approximationCoeffs();
            double[] detail = result.detailCoeffs();
            bh.consume(approx);
            bh.consume(detail);
        }
    }
}