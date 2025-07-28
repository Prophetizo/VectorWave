package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.MorletWavelet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for Continuous Wavelet Transform operations.
 * 
 * Tests performance of:
 * - Different signal sizes
 * - Various scale counts
 * - FFT vs direct convolution
 * - Different wavelet types
 * - Complex vs real analysis
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class CWTBenchmark {
    
    @Param({"256", "1024", "4096", "16384"})
    private int signalSize;
    
    @Param({"8", "16", "32", "64"})
    private int scaleCount;
    
    @Param({"morlet", "paul", "dog2", "mexihat"})
    private String waveletType;
    
    private double[] signal;
    private double[] scales;
    private MorletWavelet morlet;
    private PaulWavelet paul;
    private DOGWavelet dog2;
    private MATLABMexicanHat mexihat;
    private CWTTransform cwtMorlet;
    private CWTTransform cwtPaul;
    private CWTTransform cwtDog2;
    private CWTTransform cwtMexihat;
    
    @Setup
    public void setup() {
        // Generate test signal with multiple frequency components
        Random rand = new Random(42);
        signal = new double[signalSize];
        
        // Create multi-frequency signal
        double samplingRate = 1000.0;
        for (int i = 0; i < signalSize; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 10 * t) +     // 10 Hz
                       0.5 * Math.sin(2 * Math.PI * 50 * t) + // 50 Hz
                       0.2 * Math.sin(2 * Math.PI * 200 * t) + // 200 Hz
                       0.1 * rand.nextGaussian();              // Noise
        }
        
        // Generate logarithmic scales
        scales = new double[scaleCount];
        double minScale = 2.0;
        double maxScale = Math.min(signalSize / 4.0, 100.0);
        for (int i = 0; i < scaleCount; i++) {
            double t = (double) i / (scaleCount - 1);
            scales[i] = minScale * Math.pow(maxScale / minScale, t);
        }
        
        // Initialize wavelets
        morlet = new MorletWavelet();
        paul = new PaulWavelet(4);
        dog2 = new DOGWavelet(2);
        mexihat = new MATLABMexicanHat();
        
        // Initialize transforms
        cwtMorlet = new CWTTransform(morlet);
        cwtPaul = new CWTTransform(paul);
        cwtDog2 = new CWTTransform(dog2);
        cwtMexihat = new CWTTransform(mexihat);
    }
    
    @Benchmark
    public void benchmarkCWTReal(Blackhole bh) {
        CWTResult result;
        switch (waveletType) {
            case "morlet":
                result = cwtMorlet.analyze(signal, scales);
                break;
            case "paul":
                result = cwtPaul.analyze(signal, scales);
                break;
            case "dog2":
                result = cwtDog2.analyze(signal, scales);
                break;
            case "mexihat":
                result = cwtMexihat.analyze(signal, scales);
                break;
            default:
                throw new IllegalStateException("Unknown wavelet: " + waveletType);
        }
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkCWTComplex(Blackhole bh) {
        ComplexCWTResult result;
        switch (waveletType) {
            case "morlet":
                result = cwtMorlet.analyzeComplex(signal, scales);
                break;
            case "paul":
                result = cwtPaul.analyzeComplex(signal, scales);
                break;
            case "dog2":
                result = cwtDog2.analyzeComplex(signal, scales);
                break;
            case "mexihat":
                result = cwtMexihat.analyzeComplex(signal, scales);
                break;
            default:
                throw new IllegalStateException("Unknown wavelet: " + waveletType);
        }
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkAdaptiveScaleSelection(Blackhole bh) {
        SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
        double[] adaptiveScales = selector.selectScales(signal, morlet, 1000.0);
        bh.consume(adaptiveScales);
    }
    
    @Benchmark
    public void benchmarkInverseCWT(Blackhole bh) {
        // Test inverse transform performance
        CWTResult forward = cwtMorlet.analyze(signal, scales);
        InverseCWT inverse = new InverseCWT(morlet);
        double[] reconstructed = inverse.reconstruct(forward);
        bh.consume(reconstructed);
    }
    
    // Ridge extraction not yet implemented
    // @Benchmark
    // public void benchmarkRidgeExtraction(Blackhole bh) {
    //     ComplexCWTResult result = cwtMorlet.analyzeComplex(signal, scales);
    //     RidgeExtractor extractor = new RidgeExtractor();
    //     RidgeExtractor.RidgeResult ridges = extractor.extractRidges(result, 1000.0);
    //     bh.consume(ridges);
    // }
    
    @Benchmark
    public void benchmarkFinancialAnalysis(Blackhole bh) {
        // Simulate price data
        double[] priceData = new double[signalSize];
        priceData[0] = 100.0;
        for (int i = 1; i < signalSize; i++) {
            priceData[i] = priceData[i-1] * (1 + 0.001 * signal[i]);
        }
        
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
        var crashes = analyzer.detectMarketCrashes(priceData, 0.05);
        bh.consume(crashes);
    }
}