package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.util.SignalProcessor;
import ai.prophetizo.wavelet.cwt.ComplexNumber;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for FFT operations.
 * 
 * Tests:
 * - FFT performance at various sizes
 * - Forward vs inverse transforms
 * - Twiddle factor caching effectiveness
 * - Real vs complex FFT
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class FFTBenchmark {
    
    @Param({"128", "256", "512", "1024", "2048", "4096", "8192", "16384"})
    private int fftSize;
    
    private ComplexNumber[] complexData;
    private double[] realData;
    private double[] imagData;
    
    @Setup
    public void setup() {
        Random rand = new Random(42);
        
        // Initialize complex data
        complexData = new ComplexNumber[fftSize];
        realData = new double[fftSize];
        imagData = new double[fftSize];
        
        for (int i = 0; i < fftSize; i++) {
            double real = rand.nextGaussian();
            double imag = rand.nextGaussian();
            complexData[i] = new ComplexNumber(real, imag);
            realData[i] = real;
            imagData[i] = imag;
        }
    }
    
    @Benchmark
    public void benchmarkFFTForward(Blackhole bh) {
        ComplexNumber[] data = complexData.clone();
        SignalProcessor.fft(data);
        bh.consume(data);
    }
    
    @Benchmark
    public void benchmarkFFTInverse(Blackhole bh) {
        ComplexNumber[] data = complexData.clone();
        SignalProcessor.ifft(data);
        bh.consume(data);
    }
    
    @Benchmark
    public void benchmarkFFTRoundTrip(Blackhole bh) {
        ComplexNumber[] data = complexData.clone();
        SignalProcessor.fft(data);
        SignalProcessor.ifft(data);
        bh.consume(data);
    }
    
    @Benchmark
    public void benchmarkRealFFT(Blackhole bh) {
        double[] real = realData.clone();
        ComplexNumber[] result = SignalProcessor.fftReal(real);
        bh.consume(result);
    }
    
    @Benchmark
    public void benchmarkComplexMultiplication(Blackhole bh) {
        ComplexNumber[] data1 = complexData.clone();
        ComplexNumber[] data2 = new ComplexNumber[fftSize];
        
        // Initialize second array
        for (int i = 0; i < fftSize; i++) {
            data2[i] = new ComplexNumber(i * 0.1, -i * 0.1);
        }
        
        // Perform element-wise multiplication
        for (int i = 0; i < fftSize; i++) {
            data1[i] = data1[i].multiply(data2[i]);
        }
        
        bh.consume(data1);
    }
    
    @Benchmark
    public void benchmarkTwiddleFactorGeneration(Blackhole bh) {
        // Test twiddle factor calculation performance
        double[] cosTable = new double[fftSize / 2];
        double[] sinTable = new double[fftSize / 2];
        
        for (int i = 0; i < fftSize / 2; i++) {
            double angle = -2.0 * Math.PI * i / fftSize;
            cosTable[i] = Math.cos(angle);
            sinTable[i] = Math.sin(angle);
        }
        
        bh.consume(cosTable);
        bh.consume(sinTable);
    }
    
}