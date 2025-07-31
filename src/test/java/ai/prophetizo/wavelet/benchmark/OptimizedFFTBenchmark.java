package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.util.SignalProcessor;
import ai.prophetizo.wavelet.util.OptimizedFFT;
import ai.prophetizo.wavelet.cwt.ComplexNumber;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks comparing different FFT implementations.
 * 
 * Compares:
 * - Basic SignalProcessor implementation (delegates to OptimizedFFT)
 * - OptimizedFFT with automatic algorithm selection
 * - Real-to-complex optimizations
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class OptimizedFFTBenchmark {
    
    @Param({"128", "256", "512", "1024", "2048", "4096", "8192"})
    private int fftSize;
    
    @Param({"true", "false"})
    private boolean isPowerOf2;
    
    private double[] realData;
    private double[] complexData; // Interleaved real/imag
    private ComplexNumber[] complexNumbers;
    
    @Setup
    public void setup() {
        Random rand = new Random(42);
        
        // Create test size: use exact size for power-of-2, or size-1 for non-power-of-2 tests
        // This allows testing both power-of-2 optimized paths and arbitrary-size algorithms
        int testSize = isPowerOf2 ? fftSize : fftSize - 1;
        
        // Initialize data arrays with the test size
        realData = new double[testSize];
        complexData = new double[2 * testSize];
        complexNumbers = new ComplexNumber[testSize];
        
        for (int i = 0; i < testSize; i++) {
            double real = rand.nextGaussian();
            double imag = rand.nextGaussian();
            
            realData[i] = real;
            complexData[2 * i] = real;
            complexData[2 * i + 1] = imag;
            complexNumbers[i] = new ComplexNumber(real, imag);
        }
    }
    
    @Benchmark
    public void benchmarkSignalProcessor(Blackhole bh) {
        if (isPowerOf2) {
            ComplexNumber[] data = complexNumbers.clone();
            SignalProcessor.fft(data);
            bh.consume(data);
        }
    }
    
    @Benchmark
    public void benchmarkOptimizedFFTAuto(Blackhole bh) {
        double[] data = complexData.clone();
        OptimizedFFT.fftOptimized(data, data.length / 2, false);
        bh.consume(data);
    }
    
    @Benchmark
    public void benchmarkRealOptimized(Blackhole bh) {
        ComplexNumber[] result = OptimizedFFT.fftRealOptimized(realData.clone());
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 0)
    @Measurement(iterations = 5)
    public void benchmarkMemoryAllocation(Blackhole bh) {
        // Measure memory allocation overhead
        double[] data = new double[2 * fftSize];
        ComplexNumber[] complex = new ComplexNumber[fftSize];
        
        for (int i = 0; i < fftSize; i++) {
            complex[i] = new ComplexNumber(data[2 * i], data[2 * i + 1]);
        }
        
        bh.consume(complex);
    }
    
    @State(Scope.Thread)
    public static class ConvolutionBenchmark {
        @Param({"256", "512", "1024"})
        private int signalSize;
        
        @Param({"32", "64", "128"})
        private int kernelSize;
        
        private double[] signal;
        private double[] kernel;
        
        @Setup
        public void setup() {
            Random rand = new Random(42);
            signal = new double[signalSize];
            kernel = new double[kernelSize];
            
            for (int i = 0; i < signalSize; i++) {
                signal[i] = rand.nextGaussian();
            }
            for (int i = 0; i < kernelSize; i++) {
                kernel[i] = rand.nextGaussian();
            }
        }
        
        @Benchmark
        public void benchmarkDirectConvolution(Blackhole bh) {
            double[] result = new double[signalSize + kernelSize - 1];
            
            for (int i = 0; i < result.length; i++) {
                double sum = 0;
                for (int j = 0; j < kernelSize; j++) {
                    if (i - j >= 0 && i - j < signalSize) {
                        sum += signal[i - j] * kernel[j];
                    }
                }
                result[i] = sum;
            }
            
            bh.consume(result);
        }
        
        @Benchmark
        public void benchmarkFFTConvolution(Blackhole bh) {
            double[] result = SignalProcessor.convolveFFT(signal, kernel);
            bh.consume(result);
        }
    }
}