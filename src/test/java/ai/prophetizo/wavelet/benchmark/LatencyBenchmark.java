package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Latency-focused benchmarks for real-time applications.
 * 
 * <p>Measures:</p>
 * <ul>
 *   <li>Worst-case latencies (99th, 99.9th percentiles)</li>
 *   <li>Jitter and latency stability</li>
 *   <li>GC impact on latency</li>
 *   <li>Thread contention effects</li>
 * </ul>
 * 
 * <p>Run with: {@code ./jmh-runner.sh LatencyBenchmark}</p>
 */
@State(Scope.Benchmark)
@Fork(value = 3, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UnlockDiagnosticVMOptions",
    "-Xms256M", "-Xmx256M",  // Fixed heap to reduce GC variability
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=5",  // Aggressive pause target
    "-XX:+UseNUMA",  // NUMA awareness for better latency
    "-XX:+AlwaysPreTouch"  // Pre-touch heap for predictable latency
})
@Warmup(iterations = 20, time = 1)
@Measurement(iterations = 50, time = 1)
public class LatencyBenchmark {

    // Different signal sizes for latency profiling
    @Param({"16", "32", "64", "128", "256"})
    private int signalSize;
    
    private double[] signal;
    private WaveletTransform transform;
    private WaveletTransform transformNoSIMD;
    
    // Pre-allocated arrays to reduce allocation jitter
    private double[] workspace;
    private TransformResult cachedResult;
    
    // Thread-local pre-allocated arrays for multi-threaded benchmarks
    private static final int MAX_THREADS = 8;
    private double[][] threadLocalSignals;
    private static final ThreadLocal<Integer> threadIndex = ThreadLocal.withInitial(() -> {
        return (int) (Thread.currentThread().getId() % MAX_THREADS);
    });
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize signal with realistic data
        signal = new double[signalSize];
        workspace = new double[signalSize];
        
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 8.0);
        }
        
        // Setup transforms
        TransformConfig simdConfig = TransformConfig.builder()
            .forceSIMD(true)
            .build();
            
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        
        transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, simdConfig);
        transformNoSIMD = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, scalarConfig);
        
        // Pre-allocate result to measure transform-only latency
        cachedResult = transform.forward(signal);
        
        // Pre-allocate thread-local arrays
        threadLocalSignals = new double[MAX_THREADS][signalSize];
        for (int t = 0; t < MAX_THREADS; t++) {
            System.arraycopy(signal, 0, threadLocalSignals[t], 0, signalSize);
        }
    }
    
    // ===== Percentile Latency Measurements =====
    
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 1000, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void forwardTransformLatencyDistribution(Blackhole bh) {
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 1000, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void inverseTransformLatencyDistribution(Blackhole bh) {
        double[] reconstructed = transform.inverse(cachedResult);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 1000, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void roundTripLatencyDistribution(Blackhole bh) {
        TransformResult forward = transform.forward(signal);
        double[] reconstructed = transform.inverse(forward);
        bh.consume(reconstructed);
    }
    
    // ===== SIMD vs Scalar Latency Comparison =====
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void simdLatency(Blackhole bh) {
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void scalarLatency(Blackhole bh) {
        TransformResult result = transformNoSIMD.forward(signal);
        bh.consume(result);
    }
    
    // ===== Jitter Measurements =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 100, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(1)  // Single thread to measure pure jitter
    public void singleThreadedJitter(Blackhole bh) {
        // Copy to workspace to avoid cache effects
        System.arraycopy(signal, 0, workspace, 0, signalSize);
        TransformResult result = transform.forward(workspace);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 100, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(4)  // Multiple threads to measure contention
    public void multiThreadedJitter(Blackhole bh) {
        // Use pre-allocated thread-local arrays to avoid allocation overhead
        double[] localSignal = threadLocalSignals[threadIndex.get()];
        TransformResult result = transform.forward(localSignal);
        bh.consume(result);
    }
    
    // ===== Allocation Impact =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void withAllocation(Blackhole bh) {
        // Measure with fresh allocation each time
        double[] freshSignal = new double[signalSize];
        System.arraycopy(signal, 0, freshSignal, 0, signalSize);
        WaveletTransform freshTransform = new WaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = freshTransform.forward(freshSignal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void withoutAllocation(Blackhole bh) {
        // Measure with pre-allocated resources
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    // ===== Wavelet Type Impact =====
    
    private WaveletTransform haarTransform;
    private WaveletTransform db2Transform;
    private WaveletTransform db4Transform;
    
    @Setup(Level.Trial)
    public void setupWavelets() {
        TransformConfig config = TransformConfig.builder()
            .build();
            
        haarTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, config);
        db2Transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC, config);
        db4Transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC, config);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void haarLatency(Blackhole bh) {
        TransformResult result = haarTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void db2Latency(Blackhole bh) {
        TransformResult result = db2Transform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void db4Latency(Blackhole bh) {
        TransformResult result = db4Transform.forward(signal);
        bh.consume(result);
    }
    
    // ===== Custom runner for detailed percentile reporting =====
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(LatencyBenchmark.class.getSimpleName())
            .param("signalSize", "64")  // Focus on one size for detailed analysis
            .mode(Mode.SampleTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .warmupIterations(20)
            .measurementIterations(1000)
            .forks(1)
            .jvmArgs(
                "--add-modules", "jdk.incubator.vector",
                "-XX:+UnlockDiagnosticVMOptions",
                "-Xms256M", "-Xmx256M",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=5"
            )
            .build();
            
        Collection<RunResult> results = new Runner(opt).run();
        
        // Print detailed percentile analysis
        System.out.println("\n=== Latency Percentile Analysis ===");
        for (RunResult result : results) {
            System.out.println("\nBenchmark: " + result.getPrimaryResult().getLabel());
            System.out.println("Samples: " + result.getPrimaryResult().getStatistics().getN());
            System.out.println("Mean: " + String.format("%.2f ns", 
                result.getPrimaryResult().getStatistics().getMean()));
            System.out.println("StdDev: " + String.format("%.2f ns",
                result.getPrimaryResult().getStatistics().getStandardDeviation()));
            
            // Calculate percentiles manually if needed
            double[] percentiles = {50.0, 90.0, 95.0, 99.0, 99.9, 99.99};
            System.out.println("\nPercentiles:");
            for (double p : percentiles) {
                System.out.println(String.format("  p%.2f: %.2f ns", p,
                    result.getPrimaryResult().getStatistics().getPercentile(p)));
            }
        }
    }
}