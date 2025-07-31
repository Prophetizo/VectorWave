package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.internal.PrefetchOptimizer;
import ai.prophetizo.wavelet.internal.ScalarOps;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * JMH benchmark to measure the impact of cache prefetching optimizations.
 * 
 * <p>This benchmark compares standard wavelet transforms with prefetch-optimized
 * versions across different signal sizes to measure cache efficiency improvements.</p>
 * 
 * Run with: ./jmh-runner.sh PrefetchBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {
    "-Xms2G", "-Xmx2G",
    "-XX:+UseG1GC",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+PrintCompilation",
    "-XX:+PrintInlining"
})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class PrefetchBenchmark {
    
    @Param({"256", "1024", "4096", "16384", "65536"})
    private int signalLength;
    
    @Param({"haar", "db4", "sym8"})
    private String waveletName;
    
    private double[] signal;
    private double[] lowFilter;
    private double[] highFilter;
    private Wavelet wavelet;
    private WaveletTransform transform;
    
    // Output arrays
    private double[] approxCoeffs;
    private double[] detailCoeffs;
    
    @Setup
    public void setup() {
        // Create test signal with realistic characteristics
        Random random = new Random(TestConstants.TEST_SEED);
        signal = new double[signalLength];
        
        // Generate signal with multiple frequency components
        for (int i = 0; i < signalLength; i++) {
            double t = i / (double) signalLength;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +        // Low frequency
                       0.5 * Math.sin(2 * Math.PI * 20 * t) +  // Medium frequency
                       0.25 * Math.sin(2 * Math.PI * 50 * t) + // High frequency
                       0.1 * (random.nextDouble() - 0.5);      // Noise
        }
        
        // Initialize wavelet and filters
        wavelet = WaveletRegistry.getWavelet(waveletName);
        lowFilter = wavelet.lowPassDecomposition();
        highFilter = wavelet.highPassDecomposition();
        
        // Initialize transform
        transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Allocate output arrays
        approxCoeffs = new double[signalLength / 2];
        detailCoeffs = new double[signalLength / 2];
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void standardTransform(Blackhole bh) {
        ScalarOps.convolveAndDownsamplePeriodic(signal, lowFilter, approxCoeffs);
        ScalarOps.convolveAndDownsamplePeriodic(signal, highFilter, detailCoeffs);
        bh.consume(approxCoeffs);
        bh.consume(detailCoeffs);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void prefetchOptimizedTransform(Blackhole bh) {
        if (PrefetchOptimizer.isPrefetchBeneficial(signalLength)) {
            PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(
                signal, lowFilter, approxCoeffs);
            PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(
                signal, highFilter, detailCoeffs);
        } else {
            // Fall back to standard for small signals
            ScalarOps.convolveAndDownsamplePeriodic(signal, lowFilter, approxCoeffs);
            ScalarOps.convolveAndDownsamplePeriodic(signal, highFilter, detailCoeffs);
        }
        bh.consume(approxCoeffs);
        bh.consume(detailCoeffs);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void combinedTransform(Blackhole bh) {
        ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, 
                                           approxCoeffs, detailCoeffs);
        bh.consume(approxCoeffs);
        bh.consume(detailCoeffs);
    }
    
    /**
     * Measures cache misses by accessing data in a pattern that defeats prefetching.
     * This serves as a baseline to show the impact of poor cache usage.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void randomAccessTransform(Blackhole bh) {
        // Simulate poor cache access pattern
        Random random = new Random(TestConstants.TEST_SEED);
        int filterLen = lowFilter.length;
        
        for (int i = 0; i < approxCoeffs.length; i++) {
            double sumLow = 0.0;
            double sumHigh = 0.0;
            
            // Access signal in random order (defeats cache)
            int[] indices = new int[filterLen];
            for (int j = 0; j < filterLen; j++) {
                indices[j] = (2 * i + j) % signalLength;
            }
            
            // Shuffle indices using the Fisher-Yates algorithm
            // 
            // This shuffle is intentionally used to simulate poor cache access patterns
            // by randomizing the order in which signal elements are accessed. This 
            // introduces an O(n) overhead for each convolution operation, which is 
            // not typical for standard convolution performance. The purpose of this 
            // is to defeat caching and measure the impact of cache prefetching optimizations.
            for (int j = filterLen - 1; j > 0; j--) {
                int k = random.nextInt(j + 1);
                int temp = indices[j];
                indices[j] = indices[k];
                indices[k] = temp;
            }
            
            // Access in shuffled order
            for (int j = 0; j < filterLen; j++) {
                double val = signal[indices[j]];
                sumLow += val * lowFilter[j];
                sumHigh += val * highFilter[j];
            }
            
            approxCoeffs[i] = sumLow;
            detailCoeffs[i] = sumHigh;
        }
        
        bh.consume(approxCoeffs);
        bh.consume(detailCoeffs);
    }
    
    /**
     * Benchmark multi-level transform to show prefetch benefits in recursive operations.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void multiLevelTransform(Blackhole bh) {
        TransformResult result = transform.forward(signal);
        
        // Perform 3 levels of decomposition
        double[] currentSignal = result.approximationCoeffs();
        for (int level = 1; level < 3 && currentSignal.length >= 4; level++) {
            double[] tempApprox = new double[currentSignal.length / 2];
            double[] tempDetail = new double[currentSignal.length / 2];
            
            if (PrefetchOptimizer.isPrefetchBeneficial(currentSignal.length)) {
                PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(
                    currentSignal, lowFilter, tempApprox);
                PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(
                    currentSignal, highFilter, tempDetail);
            } else {
                ScalarOps.convolveAndDownsamplePeriodic(currentSignal, lowFilter, tempApprox);
                ScalarOps.convolveAndDownsamplePeriodic(currentSignal, highFilter, tempDetail);
            }
            
            currentSignal = tempApprox;
            bh.consume(tempDetail);
        }
        
        bh.consume(currentSignal);
    }
    
    /**
     * Information method to display prefetch capabilities.
     */
    @TearDown(Level.Trial)
    public void displayPrefetchInfo() {
        System.out.println("\n" + PrefetchOptimizer.getPrefetchInfo());
    }
}