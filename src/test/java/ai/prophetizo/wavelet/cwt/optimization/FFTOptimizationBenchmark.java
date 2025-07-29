package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing standard and optimized FFT implementations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FFTOptimizationBenchmark {
    
    private FFTAcceleratedCWT standardFFT;
    private FFTAcceleratedCWT optimizedFFT;
    
    @Param({"64", "256", "1024", "4096"})
    private int size;
    
    private double[] testSignal;
    private Complex[] testSpectrum;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Generate test signal
        testSignal = new double[size];
        for (int i = 0; i < size; i++) {
            testSignal[i] = Math.sin(2 * Math.PI * i / size) + 
                           0.5 * Math.cos(4 * Math.PI * i / size) +
                           0.25 * Math.sin(8 * Math.PI * i / size);
        }
        
        // Generate test spectrum for IFFT benchmarks
        testSpectrum = new Complex[size];
        for (int i = 0; i < size; i++) {
            double phase = 2 * Math.PI * i / size;
            testSpectrum[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
    }
    
    @Benchmark
    public void standardFFT(Blackhole bh) {
        Complex[] result = standardFFT.fft(testSignal);
        bh.consume(result);
    }
    
    @Benchmark
    public void optimizedFFT(Blackhole bh) {
        Complex[] result = optimizedFFT.fft(testSignal);
        bh.consume(result);
    }
    
    @Benchmark
    public void standardIFFT(Blackhole bh) {
        double[] result = standardFFT.ifft(testSpectrum);
        bh.consume(result);
    }
    
    @Benchmark
    public void optimizedIFFT(Blackhole bh) {
        double[] result = optimizedFFT.ifft(testSpectrum);
        bh.consume(result);
    }
    
    @Benchmark
    public void standardRoundTrip(Blackhole bh) {
        Complex[] spectrum = standardFFT.fft(testSignal);
        double[] reconstructed = standardFFT.ifft(spectrum);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    public void optimizedRoundTrip(Blackhole bh) {
        Complex[] spectrum = optimizedFFT.fft(testSignal);
        double[] reconstructed = optimizedFFT.ifft(spectrum);
        bh.consume(reconstructed);
    }
}