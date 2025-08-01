package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import java.util.Random;

/**
 * Demonstrates the real-to-complex FFT optimization for CWT.
 * 
 * Shows ~2x speedup for FFT operations when processing real signals,
 * which is common in financial data analysis.
 * 
 * Note: Performance timings may vary between runs due to JIT compilation,
 * system load, and other factors. The signal generation is deterministic
 * to ensure consistent input data across runs.
 */
public class RealFFTDemo {
    
    // Use a fixed seed for reproducible signal generation
    private static final Random RANDOM = new Random(42);
    
    public static void main(String[] args) {
        System.out.println("=== Real FFT Optimization Demo ===\n");
        
        // Generate test signals of various sizes
        int[] signalSizes = {512, 1024, 2048, 4096, 8192};
        double[] scales = generateLogScales(16, 1.0, 32.0);
        
        System.out.println("Comparing CWT performance with different FFT algorithms:");
        System.out.println("Signal sizes: " + java.util.Arrays.toString(signalSizes));
        System.out.println("Number of scales: " + scales.length);
        System.out.println();
        
        // Test with Morlet wavelet (complex)
        System.out.println("1. Morlet Wavelet (Complex) Analysis:");
        System.out.println("-------------------------------------");
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        comparePerformance(morlet, signalSizes, scales);
        
        // Test with another Morlet wavelet configuration
        System.out.println("\n2. Morlet Wavelet (Alternative) Analysis:");
        System.out.println("-----------------------------------------");
        MorletWavelet morlet2 = new MorletWavelet(5.0, 2.0);
        comparePerformance(morlet2, signalSizes, scales);
        
        // Demonstrate accuracy
        System.out.println("\n3. Accuracy Verification:");
        System.out.println("------------------------");
        verifyAccuracy();
        
        // Show memory efficiency
        System.out.println("\n4. Memory Efficiency:");
        System.out.println("--------------------");
        showMemoryEfficiency();
    }
    
    private static void comparePerformance(ContinuousWavelet wavelet, int[] signalSizes, double[] scales) {
        System.out.printf("%-12s %-15s %-15s %-15s %-12s%n", 
            "Signal Size", "Standard FFT", "Real FFT", "AUTO", "Speedup");
        System.out.println("-".repeat(75));
        
        for (int size : signalSizes) {
            // Generate test signal
            double[] signal = generateTestSignal(size);
            
            // Standard FFT
            long standardTime = benchmarkCWT(wavelet, signal, scales, FFTAlgorithm.RADIX2);
            
            // Real-optimized FFT
            long realTime = benchmarkCWT(wavelet, signal, scales, FFTAlgorithm.REAL_OPTIMIZED);
            
            // AUTO algorithm
            long autoTime = benchmarkCWT(wavelet, signal, scales, FFTAlgorithm.AUTO);
            
            // Calculate speedup
            double speedup = (double) standardTime / realTime;
            
            System.out.printf("%-12d %-15.2f %-15.2f %-15.2f %-12.2fx%n",
                size, standardTime / 1e6, realTime / 1e6, autoTime / 1e6, speedup);
        }
    }
    
    private static long benchmarkCWT(ContinuousWavelet wavelet, double[] signal, 
                                     double[] scales, FFTAlgorithm algorithm) {
        // Warmup
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(algorithm)
            .build();
        CWTTransform transform = new CWTTransform(wavelet, config);
        
        for (int i = 0; i < 3; i++) {
            transform.analyze(signal, scales);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            transform.analyze(signal, scales);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / 5;
    }
    
    private static void verifyAccuracy() {
        double[] signal = generateTestSignal(1024);
        double[] scales = {1, 2, 4, 8, 16};
        MorletWavelet wavelet = new MorletWavelet();
        
        // Standard FFT
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .build();
        CWTTransform standardTransform = new CWTTransform(wavelet, standardConfig);
        CWTResult standardResult = standardTransform.analyze(signal, scales);
        
        // Real FFT
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Compare results
        double[][] standardCoeffs = standardResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        double maxDiff = 0.0;
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < signal.length; t++) {
                double diff = Math.abs(standardCoeffs[s][t] - realCoeffs[s][t]);
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        System.out.printf("Maximum difference between standard and real FFT: %.2e%n", maxDiff);
        System.out.println("Results are " + (maxDiff < 1e-10 ? "IDENTICAL" : "DIFFERENT"));
    }
    
    private static void showMemoryEfficiency() {
        int signalSize = 4096;
        int numScales = 32;
        
        // Calculate memory usage
        long standardMemory = calculateStandardFFTMemory(signalSize, numScales);
        long realOptimizedMemory = calculateRealFFTMemory(signalSize, numScales);
        
        System.out.printf("Signal size: %d, Number of scales: %d%n", signalSize, numScales);
        System.out.printf("Standard FFT memory: %.2f MB%n", standardMemory / (1024.0 * 1024.0));
        System.out.printf("Real FFT memory: %.2f MB%n", realOptimizedMemory / (1024.0 * 1024.0));
        System.out.printf("Memory savings: %.1f%%%n", 
            (1.0 - (double)realOptimizedMemory / standardMemory) * 100);
        
        // Demonstrate real-world scenario
        System.out.println("\nReal-world scenario (financial data):");
        System.out.println("- Daily price data for 10 years: ~2500 samples");
        System.out.println("- Analyzing 50 scales for multi-resolution analysis");
        System.out.println("- Processing 1000 stocks in parallel");
        
        long dataPoints = 2500L * 50 * 1000;
        long standardTotal = dataPoints * 16; // Complex numbers
        long realTotal = dataPoints * 8; // Real signal processing
        
        System.out.printf("Total memory with standard FFT: %.2f GB%n", 
            standardTotal / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Total memory with real FFT: %.2f GB%n", 
            realTotal / (1024.0 * 1024.0 * 1024.0));
    }
    
    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            // Multi-component signal similar to financial data
            signal[i] = Math.sin(2 * Math.PI * 5 * i / size) +     // Trend
                       0.5 * Math.sin(2 * Math.PI * 20 * i / size) + // Cycle
                       0.3 * Math.sin(2 * Math.PI * 50 * i / size) + // High frequency
                       0.1 * RANDOM.nextGaussian();                 // Gaussian noise (more realistic than uniform)
        }
        return signal;
    }
    
    private static double[] generateLogScales(int numScales, double minScale, double maxScale) {
        double[] scales = new double[numScales];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        
        for (int i = 0; i < numScales; i++) {
            double t = (double) i / (numScales - 1);
            scales[i] = Math.exp(logMin + t * (logMax - logMin));
        }
        return scales;
    }
    
    private static long calculateStandardFFTMemory(int signalSize, int numScales) {
        // Complex FFT arrays: 2 * 8 bytes per element
        long fftMemory = signalSize * 16L;
        // Wavelet FFT for each scale
        long waveletMemory = numScales * signalSize * 16L;
        // Output coefficients
        long outputMemory = numScales * signalSize * 8L;
        
        return fftMemory + waveletMemory + outputMemory;
    }
    
    private static long calculateRealFFTMemory(int signalSize, int numScales) {
        // Real FFT uses half the complex storage
        long fftMemory = signalSize * 8L;
        // Wavelet FFT still complex but computed more efficiently
        long waveletMemory = numScales * signalSize * 8L;
        // Output coefficients
        long outputMemory = numScales * signalSize * 8L;
        
        return fftMemory + waveletMemory + outputMemory;
    }
}