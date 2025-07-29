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
 * JMH benchmark comparing performance across different wavelet types.
 * 
 * <p>This benchmark measures how different wavelet families perform,
 * accounting for their varying filter lengths and computational complexity.
 * Currently tests: Haar, Daubechies-2 (DB2), and Daubechies-4 (DB4).
 * 
 * <p>To run this benchmark:
 * <pre>
 * mvn clean test
 * java -cp target/test-classes:target/classes \
 *     ai.prophetizo.wavelet.benchmark.WaveletTypeBenchmark
 * 
 * # To override heap size for systems with limited memory:
 * java -Xmx512m -cp target/test-classes:target/classes \
 *     ai.prophetizo.wavelet.benchmark.WaveletTypeBenchmark
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
public class WaveletTypeBenchmark {
    
    @Param({"1024", "4096"})
    private int signalSize;
    
    private double[] signal;
    
    // Different wavelet transforms
    private WaveletTransform haarTransform;
    private WaveletTransform db2Transform;
    private WaveletTransform db4Transform;
    
    @Setup
    public void setup() {
        // Generate random signal data
        signal = new double[signalSize];
        Random random = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < signalSize; i++) {
            signal[i] = random.nextGaussian() * 10.0;
        }
        
        // Create factory with consistent settings
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC);
        
        // Initialize all wavelet transforms
        haarTransform = factory.create(new Haar());
        db2Transform = factory.create(Daubechies.DB2);
        db4Transform = factory.create(Daubechies.DB4);
    }
    
    @Benchmark
    @Group("haar")
    public void haarForward(Blackhole blackhole) {
        TransformResult result = haarTransform.forward(signal);
        blackhole.consume(result);
    }
    
    @Benchmark
    @Group("haar")
    public void haarInverse(Blackhole blackhole) {
        TransformResult result = haarTransform.forward(signal);
        double[] reconstructed = haarTransform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    @Benchmark
    @Group("db2")
    public void db2Forward(Blackhole blackhole) {
        TransformResult result = db2Transform.forward(signal);
        blackhole.consume(result);
    }
    
    @Benchmark
    @Group("db2")
    public void db2Inverse(Blackhole blackhole) {
        TransformResult result = db2Transform.forward(signal);
        double[] reconstructed = db2Transform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    @Benchmark
    @Group("db4")
    public void db4Forward(Blackhole blackhole) {
        TransformResult result = db4Transform.forward(signal);
        blackhole.consume(result);
    }
    
    @Benchmark
    @Group("db4")
    public void db4Inverse(Blackhole blackhole) {
        TransformResult result = db4Transform.forward(signal);
        double[] reconstructed = db4Transform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WaveletTypeBenchmark.class.getSimpleName())
                .result("target/wavelet-type-results.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();
        
        new Runner(opt).run();
    }
}