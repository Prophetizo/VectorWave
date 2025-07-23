package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks comparing original VectorOps vs optimized VectorOps implementations.
 * 
 * <p>Run with: {@code ./jmh-runner.sh VectorOptimizationBenchmark}</p>
 * 
 * <p>Using 3 forks for more statistically reliable results by:</p>
 * <ul>
 *   <li>Accounting for JVM startup variations</li>
 *   <li>Reducing impact of system noise and background processes</li>
 *   <li>Providing better confidence in performance measurements</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 3, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UnlockDiagnosticVMOptions",
    "-Xms1G", "-Xmx1G",
    "-XX:+UseG1GC"
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
        // Use fixed seed for reproducible benchmarks
        Random random = new Random(42);
        
        // Create test signal
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Create test filter (normalized) with seeded random values
        filter = new double[filterLength];
        double sum = 0.0;
        for (int i = 0; i < filterLength; i++) {
            filter[i] = random.nextDouble();
            sum += filter[i];
        }
        // Normalize
        for (int i = 0; i < filterLength; i++) {
            filter[i] /= sum;
        }
        
        // Create low/high filters for combined transform
        lowFilter = filter.clone();
        highFilter = generateQMFHighPassFilter(filter);
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
    // Note: These benchmarks test optimized Haar wavelet implementations.
    // The Haar transform has specialized vectorized operations due to its simple coefficients.
    
    @Benchmark
    @Fork(value = 3)  // Removed unused -DtestHaar=true system property
    public void scalarHaarTransform(Blackhole bh) {
        double[] approx = new double[signalSize / 2];
        double[] detail = new double[signalSize / 2];
        
        // Haar coefficients from actual implementation
        Haar haar = new Haar();
        double[] haarLow = haar.lowPassDecomposition();
        double[] haarHigh = haar.highPassDecomposition();
        
        ScalarOps.combinedTransformPeriodic(signal, haarLow, haarHigh, approx, detail);
        bh.consume(approx);
        bh.consume(detail);
    }
    
    @Benchmark
    @Fork(value = 3)  // Removed unused -DtestHaar=true system property
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
    
    /**
     * Generates the high-pass filter from a low-pass filter using the Quadrature Mirror Filter (QMF) relationship.
     * 
     * <p>The QMF relationship is defined as: h[n] = (-1)^n * g[L-1-n], where:
     * <ul>
     *   <li>h[n] is the high-pass filter coefficient at index n</li>
     *   <li>g[n] is the low-pass filter coefficient</li>
     *   <li>L is the filter length</li>
     * </ul>
     * 
     * <p>This relationship ensures that the wavelet transform is orthogonal, which is crucial for:
     * <ul>
     *   <li>Preserving energy in the transform domain</li>
     *   <li>Enabling perfect reconstruction of the original signal</li>
     *   <li>Maintaining mathematical properties required for wavelet analysis</li>
     * </ul>
     * 
     * @param lowPassFilter the low-pass filter coefficients
     * @return the corresponding high-pass filter coefficients
     */
    private static double[] generateQMFHighPassFilter(double[] lowPassFilter) {
        int filterLength = lowPassFilter.length;
        double[] highPassFilter = new double[filterLength];
        
        for (int i = 0; i < filterLength; i++) {
            highPassFilter[i] = (i % 2 == 0 ? 1 : -1) * lowPassFilter[filterLength - 1 - i];
        }
        
        return highPassFilter;
    }
}