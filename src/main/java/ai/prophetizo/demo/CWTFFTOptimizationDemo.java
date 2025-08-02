package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;
import ai.prophetizo.wavelet.cwt.memory.CWTMemoryPool;
import ai.prophetizo.wavelet.util.*;

import java.util.Random;

/**
 * Demonstrates CWT performance improvements using OptimizedFFT.
 * 
 * <p>Shows how different FFT algorithms perform with CWT computation
 * and how configuration affects performance.</p>
 */
public class CWTFFTOptimizationDemo {
    
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    
    public static void main(String[] args) {
        System.out.println(BOLD + CYAN + "=== VectorWave CWT FFT Optimization Demo ===" + RESET);
        System.out.println();
        
        // Generate test signal
        int signalSize = 2048;
        double samplingRate = 1000.0; // Hz
        double[] signal = generateTestSignal(signalSize, samplingRate);
        
        // Use Morlet wavelet
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        // Generate scales
        SignalAdaptiveScaleSelector scaleSelector = new SignalAdaptiveScaleSelector();
        double[] scales = scaleSelector.selectScales(signal, wavelet, samplingRate);
        
        System.out.println("Signal: " + signalSize + " samples");
        System.out.println("Scales: " + scales.length + " scales");
        System.out.println();
        
        // Test different FFT algorithms
        System.out.println(BOLD + "1. FFT Algorithm Comparison" + RESET);
        System.out.println("Algorithm     | Time (ms) | Speedup vs Basic");
        System.out.println("--------------|-----------|------------------");
        
        // Basic FFT (existing implementation)
        long timeBasic = benchmarkCWT(signal, scales, wavelet, FFTAlgorithm.RADIX2);
        System.out.printf("%-13s | %-9d | %.2fx\n", "Basic", timeBasic, 1.0);
        
        // Split-radix FFT
        long timeSplitRadix = benchmarkCWT(signal, scales, wavelet, FFTAlgorithm.SPLIT_RADIX);
        double speedupSplit = (double) timeBasic / timeSplitRadix;
        System.out.printf("%-13s | %-9d | %.2fx\n", "Split-Radix", timeSplitRadix, speedupSplit);
        
        // Vectorized FFT
        long timeVector = benchmarkCWT(signal, scales, wavelet, FFTAlgorithm.RADIX2_VECTOR);
        double speedupVector = (double) timeBasic / timeVector;
        System.out.printf("%-13s | %-9d | %.2fx\n", "Vectorized", timeVector, speedupVector);
        
        // Real-optimized FFT
        long timeReal = benchmarkCWT(signal, scales, wavelet, FFTAlgorithm.REAL_OPTIMIZED);
        double speedupReal = (double) timeBasic / timeReal;
        System.out.printf("%-13s | %-9d | %.2fx\n", "Real-Opt", timeReal, speedupReal);
        
        // Auto selection
        long timeAuto = benchmarkCWT(signal, scales, wavelet, FFTAlgorithm.AUTO);
        double speedupAuto = (double) timeBasic / timeAuto;
        System.out.printf("%-13s | %-9d | %.2fx\n", "Auto", timeAuto, speedupAuto);
        
        System.out.println("\n" + BOLD + "2. Non-Power-of-2 Signal Sizes" + RESET);
        testNonPowerOf2();
        
        System.out.println("\n" + BOLD + "3. Financial Analysis with Optimized FFT" + RESET);
        demonstrateFinancialAnalysis();
        
        System.out.println("\n" + BOLD + "4. Memory Efficiency" + RESET);
        measureMemoryEfficiency();
    }
    
    private static long benchmarkCWT(double[] signal, double[] scales, ContinuousWavelet wavelet, 
                                    FFTAlgorithm algorithm) {
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(algorithm)
            .build();
        
        CWTTransform cwt = new CWTTransform(wavelet, config);
        
        // Warm up
        for (int i = 0; i < 3; i++) {
            cwt.analyze(signal, scales);
        }
        
        // Benchmark
        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            CWTResult result = cwt.analyze(signal, scales);
        }
        long elapsed = (System.nanoTime() - start) / 10_000_000; // Convert to ms
        
        return elapsed;
    }
    
    private static void testNonPowerOf2() {
        System.out.println("Size  | Algorithm  | Time (ms)");
        System.out.println("------|------------|----------");
        
        int[] sizes = {1000, 1500, 2000, 3000};
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        for (int size : sizes) {
            double[] signal = generateTestSignal(size, 1000.0);
            double[] scales = {1, 2, 4, 8, 16, 32};
            
            // Bluestein algorithm for non-power-of-2
            CWTConfig config = CWTConfig.builder()
                .enableFFT(true)
                .fftAlgorithm(FFTAlgorithm.BLUESTEIN)
                .build();
            
            CWTTransform cwt = new CWTTransform(wavelet, config);
            
            long start = System.nanoTime();
            cwt.analyze(signal, scales);
            long time = (System.nanoTime() - start) / 1_000_000;
            
            System.out.printf("%-6d| %-10s | %d\n", size, "Bluestein", time);
        }
    }
    
    private static void demonstrateFinancialAnalysis() {
        // Generate synthetic financial data
        int days = 1000;
        double[] prices = generateFinancialData(days);
        
        // Setup CWT with optimized FFT
        PaulWavelet wavelet = new PaulWavelet(4);
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        
        CWTTransform cwt = new CWTTransform(wavelet, config);
        
        // Analyze with financial-specific scales
        OptimalScaleSelector scaleSelector = new OptimalScaleSelector();
        double[] scales = scaleSelector.selectScales(prices, wavelet, 1.0);
        
        long start = System.nanoTime();
        ComplexCWTResult result = cwt.analyzeComplex(prices, scales);
        long analysisTime = (System.nanoTime() - start) / 1_000_000;
        
        System.out.println("Financial data: " + days + " days");
        System.out.println("Analysis time: " + analysisTime + " ms");
        
        // Extract features
        start = System.nanoTime();
        double[][] magnitude = result.getMagnitude();
        double[][] phase = result.getPhase();
        double maxMagnitude = 0;
        for (double[] row : magnitude) {
            for (double val : row) {
                maxMagnitude = Math.max(maxMagnitude, val);
            }
        }
        long featureTime = (System.nanoTime() - start) / 1_000_000;
        
        System.out.println("Feature extraction: " + featureTime + " ms");
        System.out.println("Max magnitude: " + String.format("%.2f", maxMagnitude));
        System.out.println(GREEN + "✓ Real-optimized FFT reduces computation by ~50%" + RESET);
    }
    
    private static void measureMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();
        
        // Test with different configurations
        int signalSize = 4096;
        double[] signal = generateTestSignal(signalSize, 1000.0);
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        double[] scales = {1, 2, 4, 8, 16, 32, 64, 128};
        
        // Basic implementation (no optimization)
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        CWTConfig basicConfig = CWTConfig.builder()
            .enableFFT(false)
            .build();
        CWTTransform basicCWT = new CWTTransform(wavelet, basicConfig);
        CWTResult basicResult = basicCWT.analyze(signal, scales);
        
        long memBasic = runtime.totalMemory() - runtime.freeMemory() - memBefore;
        
        // Optimized with memory pool
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        CWTMemoryPool pool = new CWTMemoryPool();
        CWTConfig optConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .memoryPool(pool)
            .build();
        CWTTransform optCWT = new CWTTransform(wavelet, optConfig);
        CWTResult optResult = optCWT.analyze(signal, scales);
        
        long memOpt = runtime.totalMemory() - runtime.freeMemory() - memBefore;
        
        System.out.println("Memory usage for " + signalSize + " samples, " + scales.length + " scales:");
        System.out.printf("Basic: %d KB\n", memBasic / 1024);
        System.out.printf("Optimized: %d KB\n", memOpt / 1024);
        System.out.printf("Memory savings: %.1f%%\n", 
            (1.0 - (double)memOpt / memBasic) * 100);
        
        // Clean up
        pool.clear();
    }
    
    private static double[] generateTestSignal(int size, double samplingRate) {
        double[] signal = new double[size];
        Random rand = new Random(42);
        
        // Combination of sinusoids + noise
        for (int i = 0; i < size; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 10 * t) +    // 10 Hz
                       0.5 * Math.sin(2 * Math.PI * 50 * t) + // 50 Hz
                       0.3 * Math.sin(2 * Math.PI * 120 * t) + // 120 Hz
                       0.2 * rand.nextGaussian();              // Noise
        }
        
        return signal;
    }
    
    private static double[] generateFinancialData(int days) {
        double[] prices = new double[days];
        Random rand = new Random(42);
        
        // Geometric Brownian Motion
        double price = 100.0;
        double drift = 0.0002;
        double volatility = 0.02;
        
        prices[0] = price;
        for (int i = 1; i < days; i++) {
            double change = drift + volatility * rand.nextGaussian();
            price *= (1 + change);
            prices[i] = price;
        }
        
        return prices;
    }
}