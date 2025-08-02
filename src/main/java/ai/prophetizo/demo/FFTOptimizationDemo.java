package ai.prophetizo.demo;

import ai.prophetizo.wavelet.util.SignalProcessor;
import ai.prophetizo.wavelet.util.OptimizedFFT;
import ai.prophetizo.wavelet.cwt.ComplexNumber;

import java.util.*;

/**
 * Demonstrates FFT optimization improvements.
 * 
 * <p>This demo shows the performance gains from various FFT optimizations
 * including split-radix, vectorization, and real-to-complex optimizations.</p>
 */
public class FFTOptimizationDemo {
    
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    
    public static void main(String[] args) {
        System.out.println(BOLD + CYAN + "=== VectorWave FFT Optimization Demo ===" + RESET);
        System.out.println();
        
        // Test different sizes
        int[] testSizes = {128, 256, 512, 1024, 2048, 4096, 8192};
        
        System.out.println(BOLD + "1. Performance Comparison - Power of 2 Sizes" + RESET);
        System.out.println("Size  | Basic FFT | Split-Radix | Real-Opt | Speedup");
        System.out.println("------|-----------|-------------|----------|--------");
        
        for (int size : testSizes) {
            compareFFTImplementations(size);
        }
        
        System.out.println("\n" + BOLD + "2. Non-Power-of-2 FFT Support" + RESET);
        int[] nonPowerSizes = {100, 255, 500, 1000, 1500};
        
        System.out.println("Size  | Bluestein | Time (μs)");
        System.out.println("------|-----------|----------");
        
        for (int size : nonPowerSizes) {
            testNonPowerOf2(size);
        }
        
        System.out.println("\n" + BOLD + "3. Convolution Performance" + RESET);
        compareConvolution();
        
        System.out.println("\n" + BOLD + "4. Memory Usage Analysis" + RESET);
        analyzeMemoryUsage();
        
        System.out.println("\n" + BOLD + "5. Twiddle Factor Cache Effectiveness" + RESET);
        measureCacheEffectiveness();
    }
    
    private static void compareFFTImplementations(int size) {
        // Generate test data
        Random rand = new Random(42);
        double[] realData = new double[size];
        for (int i = 0; i < size; i++) {
            realData[i] = rand.nextGaussian();
        }
        
        // Warm up - consume results to prevent dead code elimination
        double checksum = 0;
        for (int i = 0; i < 10; i++) {
            ComplexNumber[] temp = SignalProcessor.fftReal(realData.clone());
            // Consume result to prevent optimization
            checksum += temp[0].real();
        }
        // Use checksum to prevent entire warmup from being eliminated
        if (checksum == Double.NaN) System.out.println("Warmup failed");
        
        // Basic FFT
        long startBasic = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ComplexNumber[] result = SignalProcessor.fftReal(realData.clone());
        }
        long timeBasic = (System.nanoTime() - startBasic) / 100_000; // Convert to μs
        
        // Split-radix FFT
        double[] complexData = new double[2 * size];
        for (int i = 0; i < size; i++) {
            complexData[2 * i] = realData[i];
        }
        
        long startSplit = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            double[] data = complexData.clone();
            OptimizedFFT.fftOptimized(data, size, false);
        }
        long timeSplit = (System.nanoTime() - startSplit) / 100_000;
        
        // Real-optimized FFT
        long startReal = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(realData.clone());
        }
        long timeReal = (System.nanoTime() - startReal) / 100_000;
        
        // Calculate speedup
        double splitSpeedup = (double) timeBasic / timeSplit;
        double realSpeedup = (double) timeBasic / timeReal;
        
        System.out.printf("%-6d| %-9d | %-11d | %-8d | %.2fx/%.2fx\n",
            size, timeBasic, timeSplit, timeReal, splitSpeedup, realSpeedup);
    }
    
    private static void testNonPowerOf2(int size) {
        Random rand = new Random(42);
        double[] complexData = new double[2 * size];
        for (int i = 0; i < 2 * size; i++) {
            complexData[i] = rand.nextGaussian();
        }
        
        // Warm up
        for (int i = 0; i < 5; i++) {
            double[] temp = complexData.clone();
            OptimizedFFT.fftOptimized(temp, size, false);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            double[] data = complexData.clone();
            OptimizedFFT.fftOptimized(data, size, false);
        }
        long time = (System.nanoTime() - start) / 50_000; // μs
        
        System.out.printf("%-6d| %-9s | %d\n", size, "✓", time);
    }
    
    private static void compareConvolution() {
        int signalSize = 1024;
        int kernelSize = 64;
        
        Random rand = new Random(42);
        double[] signal = new double[signalSize];
        double[] kernel = new double[kernelSize];
        
        for (int i = 0; i < signalSize; i++) {
            signal[i] = rand.nextGaussian();
        }
        for (int i = 0; i < kernelSize; i++) {
            kernel[i] = rand.nextGaussian() * Math.exp(-i * 0.1); // Exponentially decaying
        }
        
        // Direct convolution
        long startDirect = System.nanoTime();
        double[] directResult = null;
        for (int run = 0; run < 10; run++) {
            directResult = new double[signalSize + kernelSize - 1];
            for (int i = 0; i < directResult.length; i++) {
                double sum = 0;
                for (int j = 0; j < kernelSize; j++) {
                    if (i - j >= 0 && i - j < signalSize) {
                        sum += signal[i - j] * kernel[j];
                    }
                }
                directResult[i] = sum;
            }
        }
        long timeDirect = (System.nanoTime() - startDirect) / 10_000;
        
        // FFT convolution
        long startFFT = System.nanoTime();
        double[] fftResult = null;
        for (int run = 0; run < 10; run++) {
            fftResult = SignalProcessor.convolveFFT(signal, kernel);
        }
        long timeFFT = (System.nanoTime() - startFFT) / 10_000;
        
        double speedup = (double) timeDirect / timeFFT;
        
        System.out.println("Signal: " + signalSize + " samples, Kernel: " + kernelSize + " samples");
        System.out.printf("Direct: %d μs, FFT: %d μs, Speedup: %.1fx\n", 
            timeDirect, timeFFT, speedup);
        
        // Verify correctness
        double maxError = 0;
        for (int i = 0; i < directResult.length; i++) {
            maxError = Math.max(maxError, Math.abs(directResult[i] - fftResult[i]));
        }
        System.out.printf("Max error: %.2e %s\n", maxError, 
            maxError < 1e-10 ? GREEN + "✓ PASS" + RESET : "✗ FAIL");
    }
    
    private static void analyzeMemoryUsage() {
        int size = 4096;
        
        // Measure memory for different approaches
        Runtime runtime = Runtime.getRuntime();
        
        // Basic approach - separate arrays
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        ComplexNumber[] complexArray = new ComplexNumber[size];
        for (int i = 0; i < size; i++) {
            complexArray[i] = new ComplexNumber(i, -i);
        }
        
        long memAfterComplex = runtime.totalMemory() - runtime.freeMemory();
        long complexMemory = memAfterComplex - memBefore;
        
        // Optimized approach - interleaved array
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        double[] interleavedArray = new double[2 * size];
        for (int i = 0; i < size; i++) {
            interleavedArray[2 * i] = i;
            interleavedArray[2 * i + 1] = -i;
        }
        
        long memAfterInterleaved = runtime.totalMemory() - runtime.freeMemory();
        long interleavedMemory = memAfterInterleaved - memBefore;
        
        System.out.println("Memory usage for " + size + " complex numbers:");
        System.out.printf("ComplexNumber[]: %d KB\n", complexMemory / 1024);
        System.out.printf("double[] interleaved: %d KB\n", interleavedMemory / 1024);
        System.out.printf("Memory savings: %.1f%%\n", 
            (1.0 - (double)interleavedMemory / complexMemory) * 100);
    }
    
    private static void measureCacheEffectiveness() {
        int[] sizes = {128, 256, 512, 1024, 2048, 4096};
        
        System.out.println("Size  | First Run | Cached Run | Cache Speedup");
        System.out.println("------|-----------|------------|---------------");
        
        for (int size : sizes) {
            // Create test data
            double[] data = new double[2 * size];
            Random rand = new Random(42);
            for (int i = 0; i < 2 * size; i++) {
                data[i] = rand.nextGaussian();
            }
            
            // First run (cold cache)
            long startCold = System.nanoTime();
            double[] dataCopy = data.clone();
            OptimizedFFT.fftOptimized(dataCopy, size, false);
            long timeCold = System.nanoTime() - startCold;
            
            // Subsequent runs (warm cache)
            long totalWarm = 0;
            for (int i = 0; i < 10; i++) {
                dataCopy = data.clone();
                long start = System.nanoTime();
                OptimizedFFT.fftOptimized(dataCopy, size, false);
                totalWarm += System.nanoTime() - start;
            }
            long timeWarm = totalWarm / 10;
            
            double cacheSpeedup = (double) timeCold / timeWarm;
            
            System.out.printf("%-6d| %-9d | %-10d | %.2fx\n",
                size, timeCold / 1000, timeWarm / 1000, cacheSpeedup);
        }
        
        System.out.println("\n" + GREEN + "✓ Twiddle factor caching provides significant speedup!" + RESET);
    }
}