package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for measuring performance across different signal sizes.
 * 
 * <p>This benchmark measures how transform performance scales with signal size,
 * from small signals (256 samples) to large signals (16384 samples).
 * 
 * <p>To run this benchmark:
 * <pre>
 * mvn clean test
 * java -cp target/test-classes:target/classes \
 *     ai.prophetizo.wavelet.benchmark.SignalSizeBenchmark
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "-XX:+UseG1GC"})  // 2G for large signal sizes
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
public class SignalSizeBenchmark {
    
    @Param({"256", "512", "1024", "2048", "4096", "8192", "16384"})
    private int signalSize;
    
    private double[] signal;
    private WaveletTransform transform;
    
    @Setup
    public void setup() {
        // Generate random signal data
        signal = new double[signalSize];
        Random random = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < signalSize; i++) {
            signal[i] = random.nextGaussian() * 10.0; // Scale for more realistic values
        }
        
        // Create transform with default SIMD optimization
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        transform = factory.create(new Haar());
    }
    
    @Benchmark
    public void forwardTransform(Blackhole blackhole) {
        TransformResult result = transform.forward(signal);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void inverseTransform(Blackhole blackhole) {
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    @Benchmark
    public void roundTripTransform(Blackhole blackhole) {
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 100)
    public void coldStartForward(Blackhole blackhole) {
        TransformResult result = transform.forward(signal);
        blackhole.consume(result);
    }
    
    @Benchmark
    @OperationsPerInvocation(100)
    public void batchForwardTransform(Blackhole blackhole) {
        for (int i = 0; i < 100; i++) {
            TransformResult result = transform.forward(signal);
            blackhole.consume(result);
        }
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SignalSizeBenchmark.class.getSimpleName())
                .result("target/signal-size-results.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();
        
        new Runner(opt).run();
    }
}