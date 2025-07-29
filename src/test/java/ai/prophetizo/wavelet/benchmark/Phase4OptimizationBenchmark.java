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
 * JMH benchmark specifically measuring Phase 4 optimization impact:
 * - DB2/DB4 specialized implementations
 * - Signal size specialization (small/medium/large)
 * 
 * <p>To run this benchmark:
 * <pre>
 * mvn clean test-compile
 * java -cp target/test-classes:target/classes \
 *     ai.prophetizo.wavelet.benchmark.Phase4OptimizationBenchmark
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
public class Phase4OptimizationBenchmark {
    
    @Param({"256", "1024", "16384"})  // Small, Medium, Large
    private int signalSize;
    
    public enum WaveletType {
        HAAR {
            @Override
            public Wavelet createWavelet() {
                return new Haar();
            }
        },
        DB2 {
            @Override
            public Wavelet createWavelet() {
                return Daubechies.DB2;
            }
        },
        DB4 {
            @Override
            public Wavelet createWavelet() {
                return Daubechies.DB4;
            }
        };
        
        public abstract Wavelet createWavelet();
    }
    
    @Param
    private WaveletType waveletType;
    
    private double[] signal;
    private WaveletTransform transform;
    
    @Setup
    public void setup() {
        // Generate test signal
        signal = new double[signalSize];
        Random random = new Random(42);
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.cos(4 * Math.PI * i / 64) +
                       0.1 * random.nextGaussian();
        }
        
        // Create wavelet
        Wavelet wavelet = waveletType.createWavelet();
        
        // Create optimized transform
        transform = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC)
                .create(wavelet);
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
    public void roundTrip(Blackhole blackhole) {
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        blackhole.consume(reconstructed);
    }
    
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Phase4OptimizationBenchmark.class.getSimpleName())
                .result("target/phase4-optimization-results.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();
        
        new Runner(opt).run();
    }
}