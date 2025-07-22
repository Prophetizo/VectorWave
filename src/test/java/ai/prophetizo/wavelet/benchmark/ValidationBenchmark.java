package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.util.ValidationUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for validation performance.
 * 
 * <p>This benchmark measures the performance of validation operations
 * with proper warm-up, multiple iterations, and statistical analysis.
 * 
 * <p>To run this benchmark:
 * <pre>
 * mvn clean test
 * java -cp target/test-classes:target/classes ai.prophetizo.wavelet.benchmark.ValidationBenchmark
 * </pre>
 * 
 * <p>Or using Maven:
 * <pre>
 * mvn clean test
 * mvn exec:java -Dexec.mainClass="ai.prophetizo.wavelet.benchmark.ValidationBenchmark" -Dexec.classpathScope="test"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})  // Small heap for validation
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class ValidationBenchmark {
    
    @Param({"256", "512", "1024", "2048"})
    private int arraySize;
    
    private double[] approxCoeffs;
    private double[] detailCoeffs;
    
    @Setup
    public void setup() {
        approxCoeffs = new double[arraySize];
        detailCoeffs = new double[arraySize];
        
        // Fill with valid data
        for (int i = 0; i < arraySize; i++) {
            approxCoeffs[i] = i * 0.5;
            detailCoeffs[i] = i * 0.5;
        }
    }
    
    @Benchmark
    public void validateSeparately() {
        // Validate each array separately (old pattern)
        ValidationUtils.validateNotNullOrEmpty(approxCoeffs, "approxCoeffs");
        ValidationUtils.validateNotNullOrEmpty(detailCoeffs, "detailCoeffs");
        ValidationUtils.validateFiniteValues(approxCoeffs, "approxCoeffs");
        ValidationUtils.validateFiniteValues(detailCoeffs, "detailCoeffs");
        ValidationUtils.validateMatchingLengths(approxCoeffs, detailCoeffs);
    }
    
    @Benchmark
    public void validateOptimized() {
        // Optimized validation pattern (avoiding redundant checks)
        ValidationUtils.validateNotNullOrEmpty(approxCoeffs, "approxCoeffs");
        ValidationUtils.validateNotNullOrEmpty(detailCoeffs, "detailCoeffs");
        ValidationUtils.validateMatchingLengths(approxCoeffs, detailCoeffs);
        ValidationUtils.validateFiniteValues(approxCoeffs, "approxCoeffs");
        ValidationUtils.validateFiniteValues(detailCoeffs, "detailCoeffs");
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 1000)
    public void validateSingleArray() {
        ValidationUtils.validateSignal(approxCoeffs, "signal");
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ValidationBenchmark.class.getSimpleName())
                .build();
        
        new Runner(opt).run();
    }
}