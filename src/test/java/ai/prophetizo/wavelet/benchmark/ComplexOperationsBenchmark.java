package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.cwt.optimization.ComplexVectorOps;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for complex number operations.
 * 
 * <p>Compares vectorized vs scalar implementations of complex arithmetic.
 * This benchmark evaluates the performance of various complex number operations
 * including multiplication, magnitude computation, conjugation, and addition.</p>
 * 
 * <p><b>Memory Requirements:</b> The benchmark allocates 6 arrays (real1, imag1, real2, imag2, 
 * resultReal, resultImag) based on the size parameter:
 * <ul>
 *   <li>Size 256: 6 × 256 × 8 bytes = ~12KB per benchmark instance</li>
 *   <li>Size 1024: 6 × 1024 × 8 bytes = ~48KB per benchmark instance</li>
 *   <li>Size 4096: 6 × 4096 × 8 bytes = ~192KB per benchmark instance</li>
 *   <li>Size 16384: 6 × 16384 × 8 bytes = ~768KB per benchmark instance</li>
 *   <li>JMH overhead: Additional memory for framework, measurement, and thread management</li>
 *   <li>Fork configuration: With fork=2, benchmarks run in separate JVMs</li>
 *   <li>Recommended heap: -Xmx512M for default sizes, -Xmx2G for larger sizes</li>
 * </ul>
 * </p>
 * 
 * <p><b>Running the benchmark:</b></p>
 * <pre>
 * # Default (CI-friendly, excludes 16384):
 * ./jmh-runner.sh ComplexOperationsBenchmark
 * 
 * # Include 16384 size to see vectorization benefits:
 * JAVA_OPTS="-Xmx1G" ./jmh-runner.sh ComplexOperationsBenchmark -p size=256,1024,4096,16384
 * 
 * # Local testing with even larger sizes:
 * JAVA_OPTS="-Xmx2G" ./jmh-runner.sh ComplexOperationsBenchmark -p size=16384,32768,65536
 * 
 * # Custom size combinations:
 * ./jmh-runner.sh ComplexOperationsBenchmark -p size=1024,8192
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ComplexOperationsBenchmark {
    
    // Default sizes exclude 16384 to keep the benchmark CI-friendly and avoid memory-related issues in constrained environments.
    @Param({"256", "1024", "4096"})
    private int size;
    
    private double[] real1, imag1, real2, imag2;
    private double[] resultReal, resultImag;
    private ComplexVectorOps vectorOps;
    
    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(42);
        
        // Initialize arrays
        real1 = new double[size];
        imag1 = new double[size];
        real2 = new double[size];
        imag2 = new double[size];
        resultReal = new double[size];
        resultImag = new double[size];
        
        // Fill with random data
        for (int i = 0; i < size; i++) {
            real1[i] = random.nextGaussian();
            imag1[i] = random.nextGaussian();
            real2[i] = random.nextGaussian();
            imag2[i] = random.nextGaussian();
        }
        
        vectorOps = new ComplexVectorOps();
    }
    
    @Benchmark
    public void complexMultiplyScalar(Blackhole blackhole) {
        scalarComplexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    public void complexMultiplyVector(Blackhole blackhole) {
        vectorOps.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    public void complexMagnitudeScalar(Blackhole blackhole) {
        scalarComplexMagnitude(real1, imag1, resultReal);
        blackhole.consume(resultReal);
    }
    
    @Benchmark
    public void complexMagnitudeVector(Blackhole blackhole) {
        vectorOps.complexMagnitude(real1, imag1, resultReal);
        blackhole.consume(resultReal);
    }
    
    @Benchmark
    public void complexConjugateScalar(Blackhole blackhole) {
        scalarComplexConjugate(real1, imag1, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    public void complexConjugateVector(Blackhole blackhole) {
        vectorOps.complexConjugate(real1, imag1, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    public void complexAddScalar(Blackhole blackhole) {
        scalarComplexAdd(real1, imag1, real2, imag2, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    public void complexAddVector(Blackhole blackhole) {
        vectorOps.complexAdd(real1, imag1, real2, imag2, resultReal, resultImag);
        blackhole.consume(resultReal);
        blackhole.consume(resultImag);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void complexConvolutionScalar(Blackhole blackhole) {
        // Simulate a small convolution kernel (size 64)
        int kernelSize = Math.min(64, size / 4);
        double[] kernelReal = new double[kernelSize];
        double[] kernelImag = new double[kernelSize];
        System.arraycopy(real2, 0, kernelReal, 0, kernelSize);
        System.arraycopy(imag2, 0, kernelImag, 0, kernelSize);
        
        double sumReal = 0.0;
        double sumImag = 0.0;
        
        for (int i = 0; i < kernelSize; i++) {
            double ar = real1[i];
            double ai = imag1[i];
            double br = kernelReal[i];
            double bi = kernelImag[i];
            sumReal += ar * br - ai * bi;
            sumImag += ar * bi + ai * br;
        }
        
        blackhole.consume(sumReal);
        blackhole.consume(sumImag);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void complexConvolutionVector(Blackhole blackhole) {
        // Simulate a small convolution kernel (size 64)
        int kernelSize = Math.min(64, size / 4);
        double[] kernelReal = new double[kernelSize];
        double[] kernelImag = new double[kernelSize];
        double[] tempResultReal = new double[kernelSize];
        double[] tempResultImag = new double[kernelSize];
        
        System.arraycopy(real2, 0, kernelReal, 0, kernelSize);
        System.arraycopy(imag2, 0, kernelImag, 0, kernelSize);
        
        // Use vectorized multiplication
        vectorOps.complexMultiply(real1, imag1, kernelReal, kernelImag, 
                                 tempResultReal, tempResultImag);
        
        // Sum the results (this part could also be vectorized in production)
        double sumReal = 0.0;
        double sumImag = 0.0;
        for (int i = 0; i < kernelSize; i++) {
            sumReal += tempResultReal[i];
            sumImag += tempResultImag[i];
        }
        
        blackhole.consume(sumReal);
        blackhole.consume(sumImag);
    }
    
    // Scalar implementations for comparison
    
    private void scalarComplexMultiply(double[] real1, double[] imag1,
                                      double[] real2, double[] imag2,
                                      double[] resultReal, double[] resultImag) {
        for (int i = 0; i < real1.length; i++) {
            double ar = real1[i], ai = imag1[i];
            double br = real2[i], bi = imag2[i];
            resultReal[i] = ar * br - ai * bi;
            resultImag[i] = ar * bi + ai * br;
        }
    }
    
    private void scalarComplexMagnitude(double[] real, double[] imag, double[] magnitude) {
        for (int i = 0; i < real.length; i++) {
            double r = real[i];
            double im = imag[i];
            magnitude[i] = Math.sqrt(r * r + im * im);
        }
    }
    
    private void scalarComplexConjugate(double[] real, double[] imag,
                                       double[] resultReal, double[] resultImag) {
        System.arraycopy(real, 0, resultReal, 0, real.length);
        for (int i = 0; i < imag.length; i++) {
            resultImag[i] = -imag[i];
        }
    }
    
    private void scalarComplexAdd(double[] real1, double[] imag1,
                                 double[] real2, double[] imag2,
                                 double[] resultReal, double[] resultImag) {
        for (int i = 0; i < real1.length; i++) {
            resultReal[i] = real1[i] + real2[i];
            resultImag[i] = imag1[i] + imag2[i];
        }
    }
}