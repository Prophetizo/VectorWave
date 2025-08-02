package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark demonstrating Java 23 performance optimizations in VectorWave.
 * 
 * <p>This benchmark measures the performance impact of Java 23's Vector API
 * and other optimization features across different signal sizes and wavelets.</p>
 * 
 * <p><strong>Run with:</strong></p>
 * <pre>
 * mvn clean package
 * java -jar target/benchmarks.jar Java23PerformanceBenchmark
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgsAppend = {
    "--add-modules", "jdk.incubator.vector",
    "--enable-preview",
    "-XX:+UnlockExperimentalVMOptions"
})
public class Java23PerformanceBenchmark {
    
    @Param({"256", "1024", "4096", "16384"})
    private int signalSize;
    
    @Param({"haar", "db2", "db4"})
    private String waveletType;
    
    private double[] signal;
    private MODWTTransform modwtTransform;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // Create test signal
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            double t = (double) i / signalSize;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) + 
                       0.5 * Math.sin(2 * Math.PI * 20 * t) +
                       0.1 * (Math.random() - 0.5);
        }
        
        // Create wavelet based on type
        Wavelet wavelet;
        switch (waveletType) {
            case "haar":
                wavelet = new Haar();
                break;
            case "db2":
                wavelet = Daubechies.DB2;
                break;
            case "db4":
                wavelet = Daubechies.DB4;
                break;
            default:
                throw new IllegalArgumentException("Unknown wavelet: " + waveletType);
        }
        
        // Create transform
        modwtTransform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Benchmark MODWT forward transform with automatic optimization.
     */
    @Benchmark
    public void modwtForwardOptimized(Blackhole bh) {
        var result = modwtTransform.forward(signal);
        bh.consume(result);
    }
    
    /**
     * Benchmark full MODWT round trip (forward + inverse).
     */
    @Benchmark
    public void modwtRoundTripOptimized(Blackhole bh) {
        var result = modwtTransform.forward(signal);
        var reconstructed = modwtTransform.inverse(result);
        bh.consume(reconstructed);
    }
    
    /**
     * Benchmark vectorized circular convolution directly.
     */
    @Benchmark
    public void circularConvolutionVectorized(Blackhole bh) {
        double[] filter = modwtTransform.getWavelet().lowPassDecomposition();
        double[] output = new double[signalSize];
        
        VectorOps.circularConvolveMODWTVectorized(signal, filter, output);
        bh.consume(output);
    }
    
    /**
     * Benchmark scalar circular convolution for comparison.
     */
    @Benchmark
    public void circularConvolutionScalar(Blackhole bh) {
        double[] filter = modwtTransform.getWavelet().lowPassDecomposition();
        double[] output = new double[signalSize];
        
        // Force scalar implementation
        circularConvolveMODWTScalar(signal, filter, output);
        bh.consume(output);
    }
    
    /**
     * Benchmark algorithm selection overhead.
     */
    @Benchmark
    public void algorithmSelection(Blackhole bh) {
        var strategy = VectorOps.selectOptimalStrategy(signalSize, 
            modwtTransform.getWavelet().lowPassDecomposition().length);
        bh.consume(strategy);
    }
    
    /**
     * Benchmark performance estimation.
     */
    @Benchmark
    public void performanceEstimation(Blackhole bh) {
        var estimate = modwtTransform.estimateProcessingTime(signalSize);
        bh.consume(estimate);
    }
    
    /**
     * Scalar-only circular convolution for comparison purposes.
     */
    private static void circularConvolveMODWTScalar(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            for (int l = 0; l < filterLen; l++) {
                int signalIndex = (t + l) % signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            output[t] = sum;
        }
    }
    
    /**
     * Performance information benchmark setup.
     */
    @State(Scope.Thread)
    public static class PerformanceInfoState {
        
        private ScalarOps.PerformanceInfo perfInfo;
        
        @Setup(Level.Trial)
        public void setup() {
            perfInfo = ScalarOps.getPerformanceInfo();
        }
        
        @Benchmark
        public void getPerformanceInfo(Blackhole bh) {
            bh.consume(perfInfo.description());
        }
        
        @Benchmark
        public void estimateSpeedup(Blackhole bh) {
            double speedup = perfInfo.estimateSpeedup(4096);
            bh.consume(speedup);
        }
    }
    
    /**
     * Memory allocation benchmark for different implementations.
     */
    @State(Scope.Thread)
    public static class MemoryAllocationState {
        
        @Param({"1024", "4096"})
        private int arraySize;
        
        @Benchmark
        public void clearArrayVectorized(Blackhole bh) {
            double[] array = new double[arraySize];
            VectorOps.clearArrayVectorized(array);
            bh.consume(array);
        }
        
        @Benchmark
        public void clearArrayScalar(Blackhole bh) {
            double[] array = new double[arraySize];
            for (int i = 0; i < array.length; i++) {
                array[i] = 0.0;
            }
            bh.consume(array);
        }
    }
}