package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.cwt.finance.*;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * JMH benchmarks comparing performance of different wavelet families.
 * 
 * Tests:
 * - Wavelet evaluation speed
 * - Complex vs real wavelets
 * - Different wavelet parameters
 * - Discretization performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class WaveletFamilyBenchmark {
    
    @Param({"100", "1000", "10000"})
    private int evaluationCount;
    
    private double[] tValues;
    private MorletWavelet morlet;
    private MorletWavelet morletCustom;
    private PaulWavelet paul4;
    private PaulWavelet paul6;
    private DOGWavelet dog2;
    private DOGWavelet dog4;
    private MATLABMexicanHat mexihat;
    private ClassicalShannonWavelet shannon;
    
    @Setup
    public void setup() {
        // Generate random t values for evaluation
        Random rand = new Random(TestConstants.TEST_SEED);
        tValues = new double[evaluationCount];
        for (int i = 0; i < evaluationCount; i++) {
            tValues[i] = -10.0 + 20.0 * rand.nextDouble();
        }
        
        // Initialize wavelets
        morlet = new MorletWavelet();
        morletCustom = new MorletWavelet(6.0, 1.5);
        paul4 = new PaulWavelet(4);
        paul6 = new PaulWavelet(6);
        dog2 = new DOGWavelet(2);
        dog4 = new DOGWavelet(4);
        mexihat = new MATLABMexicanHat();
        shannon = new ClassicalShannonWavelet();
    }
    
    @Benchmark
    public void benchmarkMorletDefault(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(morlet.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkMorletCustom(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(morletCustom.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkMorletComplex(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(morlet.psi(t));
            bh.consume(morlet.psiImaginary(t));
        }
    }
    
    @Benchmark
    public void benchmarkPaul4(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(paul4.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkPaul6(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(paul6.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkPaulComplex(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(paul4.psi(t));
            bh.consume(paul4.psiImaginary(t));
        }
    }
    
    @Benchmark
    public void benchmarkDOG2(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(dog2.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkDOG4(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(dog4.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkMexicanHat(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(mexihat.psi(t));
        }
    }
    
    @Benchmark
    public void benchmarkShannon(Blackhole bh) {
        for (double t : tValues) {
            bh.consume(shannon.psi(t));
        }
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void benchmarkDiscretizeMorlet(Blackhole bh) {
        double[] samples = morlet.discretize(1024);
        bh.consume(samples);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void benchmarkDiscretizePaul(Blackhole bh) {
        double[] samples = paul4.discretize(1024);
        bh.consume(samples);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void benchmarkDiscretizeDOG(Blackhole bh) {
        double[] samples = dog2.discretize(1024);
        bh.consume(samples);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void benchmarkDiscretizeMexicanHat(Blackhole bh) {
        double[] samples = mexihat.discretize(1024);
        bh.consume(samples);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void benchmarkDiscretizeShannon(Blackhole bh) {
        double[] samples = shannon.discretize(1024);
        bh.consume(samples);
    }
}