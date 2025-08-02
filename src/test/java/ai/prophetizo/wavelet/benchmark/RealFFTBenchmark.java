package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.cwt.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing real FFT optimization vs standard FFT in CWT.
 * 
 * Expected results:
 * - Real FFT provides significant performance improvements for FFT computation
 * - Overall CWT speedup depends on FFT vs convolution ratio
 * - Larger signals typically show greater benefits
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--add-modules=jdk.incubator.vector"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class RealFFTBenchmark {
    
    @Param({"512", "1024", "2048", "4096", "8192"})
    private int signalSize;
    
    @Param({"4", "8", "16", "32"})
    private int numScales;
    
    private double[] signal;
    private double[] scales;
    private CWTTransform standardTransform;
    private CWTTransform realOptimizedTransform;
    private CWTTransform autoTransform;
    
    @Setup
    public void setup() {
        // Generate realistic test signal
        signal = generateChirpSignal(signalSize, 0.01, 0.4, 1.0);
        
        // Generate logarithmic scales
        scales = new double[numScales];
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.pow(2, i * 0.5); // 1, 1.41, 2, 2.83, 4, ...
        }
        
        // Create wavelet
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        // Standard FFT transform
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .normalizeScales(true)
            .build();
        standardTransform = new CWTTransform(wavelet, standardConfig);
        
        // Real-optimized FFT transform
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .normalizeScales(true)
            .build();
        realOptimizedTransform = new CWTTransform(wavelet, realConfig);
        
        // AUTO algorithm transform
        CWTConfig autoConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.AUTO)
            .normalizeScales(true)
            .build();
        autoTransform = new CWTTransform(wavelet, autoConfig);
    }
    
    @Benchmark
    public CWTResult standardFFT() {
        return standardTransform.analyze(signal, scales);
    }
    
    @Benchmark
    public CWTResult realOptimizedFFT() {
        return realOptimizedTransform.analyze(signal, scales);
    }
    
    @Benchmark
    public CWTResult autoFFT() {
        return autoTransform.analyze(signal, scales);
    }
    
    /**
     * Benchmark complex analysis which also benefits from real signal FFT.
     */
    @Benchmark
    public ComplexCWTResult standardComplexAnalysis() {
        return standardTransform.analyzeComplex(signal, scales);
    }
    
    @Benchmark
    public ComplexCWTResult realOptimizedComplexAnalysis() {
        return realOptimizedTransform.analyzeComplex(signal, scales);
    }
    
    /**
     * Direct convolution baseline (no FFT).
     */
    @Benchmark
    public CWTResult directConvolution() {
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        CWTConfig directConfig = CWTConfig.builder()
            .enableFFT(false)  // Force direct convolution
            .normalizeScales(true)
            .build();
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        return directTransform.analyze(signal, scales);
    }
    
    // Helper method to generate chirp signal (deterministic for consistent benchmarks)
    private static double[] generateChirpSignal(int length, double f0, double f1, double amplitude) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = f0 + (f1 - f0) * t;
            signal[i] = amplitude * Math.sin(2 * Math.PI * freq * i);
        }
        return signal;
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(RealFFTBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();
        
        new Runner(opt).run();
    }
}