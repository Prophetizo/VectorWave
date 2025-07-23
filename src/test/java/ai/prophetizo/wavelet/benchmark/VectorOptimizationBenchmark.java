package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks comparing original VectorOps vs optimized VectorOps implementations.
 * 
 * <p>Run with: {@code ./jmh-runner.sh VectorOptimizationBenchmark}</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UnlockDiagnosticVMOptions"
})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class VectorOptimizationBenchmark {

    @Param({"128", "256", "512", "1024", "2048", "4096"})
    private int signalSize;
    
    @Param({"4", "8"})  // DB2 and DB4 filter lengths
    private int filterLength;
    
    private double[] signal;
    private double[] filter;
    private double[] lowFilter;
    private double[] highFilter;
    
    @Setup(Level.Trial)
    public void setup() {
        // Create test signal
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Create test filter (normalized)
        filter = new double[filterLength];
        double sum = 0.0;
        for (int i = 0; i < filterLength; i++) {
            filter[i] = Math.random();
            sum += filter[i];
        }
        // Normalize
        for (int i = 0; i < filterLength; i++) {
            filter[i] /= sum;
        }
        
        // Create low/high filters for combined transform
        lowFilter = filter.clone();
        highFilter = new double[filterLength];
        for (int i = 0; i < filterLength; i++) {
            highFilter[i] = (i % 2 == 0) ? filter[filterLength - 1 - i] : -filter[filterLength - 1 - i];
        }
    }
    
    // ===== Convolution and Downsampling Benchmarks =====
    
    @Benchmark
    public void scalarConvolveDownsample(Blackhole bh) {
        double[] result = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signalSize, filterLength);
        bh.consume(result);
    }
    
    @Benchmark
    public void vectorConvolveDownsample(Blackhole bh) {
        double[] result = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, signalSize, filterLength);
        bh.consume(result);
    }
    
    @Benchmark
    public void vectorOptimizedConvolveDownsample(Blackhole bh) {
        double[] result = VectorOpsOptimized.convolveAndDownsamplePeriodicOptimized(
            signal, filter, signalSize, filterLength);
        bh.consume(result);
    }
    
    // ===== Combined Transform Benchmarks =====
    
    @Benchmark
    public void scalarCombinedTransform(Blackhole bh) {
        double[] approx = new double[signalSize / 2];
        double[] detail = new double[signalSize / 2];
        ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, approx, detail);
        bh.consume(approx);
        bh.consume(detail);
    }
    
    @Benchmark
    public void vectorOptimizedCombinedTransform(Blackhole bh) {
        double[] approx = new double[signalSize / 2];
        double[] detail = new double[signalSize / 2];
        VectorOpsOptimized.combinedTransformPeriodicVectorized(
            signal, lowFilter, highFilter, approx, detail);
        bh.consume(approx);
        bh.consume(detail);
    }
    
    // ===== Haar Transform Benchmarks =====
    
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-DtestHaar=true")
    public void scalarHaarTransform(Blackhole bh) {
        double[] approx = new double[signalSize / 2];
        double[] detail = new double[signalSize / 2];
        
        // Haar coefficients
        double[] haarLow = {0.7071067811865476, 0.7071067811865476};
        double[] haarHigh = {0.7071067811865476, -0.7071067811865476};
        
        ScalarOps.combinedTransformPeriodic(signal, haarLow, haarHigh, approx, detail);
        bh.consume(approx);
        bh.consume(detail);
    }
    
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-DtestHaar=true")
    public void vectorOptimizedHaarTransform(Blackhole bh) {
        double[] approx = new double[signalSize / 2];
        double[] detail = new double[signalSize / 2];
        VectorOpsOptimized.haarTransformVectorized(signal, approx, detail);
        bh.consume(approx);
        bh.consume(detail);
    }
    
    // ===== Upsampling Benchmarks =====
    
    @Benchmark
    public void scalarUpsampleConvolve(Blackhole bh) {
        double[] coeffs = new double[signalSize / 2];
        System.arraycopy(signal, 0, coeffs, 0, coeffs.length);
        
        double[] result = ScalarOps.upsampleAndConvolvePeriodic(
            coeffs, filter, coeffs.length, filterLength);
        bh.consume(result);
    }
    
    @Benchmark
    public void vectorUpsampleConvolve(Blackhole bh) {
        double[] coeffs = new double[signalSize / 2];
        System.arraycopy(signal, 0, coeffs, 0, coeffs.length);
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(
            coeffs, filter, coeffs.length, filterLength);
        bh.consume(result);
    }
    
    @Benchmark
    public void vectorOptimizedUpsampleConvolve(Blackhole bh) {
        double[] coeffs = new double[signalSize / 2];
        System.arraycopy(signal, 0, coeffs, 0, coeffs.length);
        
        double[] result = VectorOpsOptimized.upsampleAndConvolvePeriodicOptimized(
            coeffs, filter, coeffs.length, filterLength);
        bh.consume(result);
    }
}