package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.util.BatchValidation;
import ai.prophetizo.wavelet.util.ValidationUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for batch validation performance.
 * 
 * <p>This benchmark compares batch validation versus individual validation
 * to demonstrate the benefits of improved cache locality.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})  // Small heap for validation
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class BatchValidationBenchmark {
    
    @Param({"3", "5", "7"})
    private int levels;
    
    private double[][] signals;
    private String[] names;
    
    @Setup
    public void setup() {
        signals = new double[levels][];
        names = new String[levels];
        int length = 1024;
        
        for (int i = 0; i < levels; i++) {
            length /= 2;
            signals[i] = new double[length];
            names[i] = "Level" + (i + 1);
            // Fill with simple deterministic data
            for (int j = 0; j < length; j++) {
                signals[i][j] = j * 0.1 + i; // Simple linear function, no expensive calculations
            }
        }
    }
    
    @Benchmark
    public void batchValidation() {
        BatchValidation.validateMultiLevelSignals(signals, names, null);
    }
    
    @Benchmark
    public void individualValidation() {
        for (int i = 0; i < signals.length; i++) {
            ValidationUtils.validateNotNullOrEmpty(signals[i], names[i]);
            ValidationUtils.validateFiniteValues(signals[i], names[i]);
        }
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void validateFiniteValuesSingle() {
        // Benchmark a single finite values check
        ValidationUtils.validateFiniteValues(signals[0], names[0]);
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BatchValidationBenchmark.class.getSimpleName())
                .build();
        
        new Runner(opt).run();
    }
}