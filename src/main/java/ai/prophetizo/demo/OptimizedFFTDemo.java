package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import ai.prophetizo.wavelet.cwt.optimization.FFTAlgorithm;
import ai.prophetizo.wavelet.cwt.optimization.OptimizedFFT;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;

import java.util.Arrays;

/**
 * Demonstrates the optimized FFT implementation with algorithm selection.
 * 
 * <p>This demo showcases the performance improvements and configuration options
 * available with the new in-place FFT implementation.</p>
 */
public class OptimizedFFTDemo {
    
    public static void main(String[] args) {
        System.out.println("Optimized FFT Implementation Demo");
        System.out.println("=================================\n");
        
        // Demonstrate algorithm selection
        demonstrateAlgorithmSelection();
        
        // Demonstrate numerical equivalence
        demonstrateNumericalEquivalence();
        
        // Demonstrate twiddle factor caching
        demonstrateTwiddleFactorCaching();
        
        // Simple performance comparison
        demonstratePerformance();
    }
    
    private static void demonstrateAlgorithmSelection() {
        System.out.println("1. Algorithm Selection:");
        
        // Default constructor uses optimized algorithm
        FFTAcceleratedCWT defaultFFT = new FFTAcceleratedCWT();
        System.out.println("   Default algorithm: " + defaultFFT.getAlgorithm());
        
        // Explicit algorithm selection
        FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        System.out.println("   Standard algorithm: " + standardFFT.getAlgorithm());
        
        FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
        System.out.println("   Optimized algorithm: " + optimizedFFT.getAlgorithm());
        
        System.out.println();
    }
    
    private static void demonstrateNumericalEquivalence() {
        System.out.println("2. Numerical Equivalence:");
        
        // Create test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0};
        System.out.println("   Test signal: " + Arrays.toString(signal));
        
        // Compare results from both algorithms
        FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
        
        Complex[] standardResult = standardFFT.fft(signal);
        Complex[] optimizedResult = optimizedFFT.fft(signal);
        
        // Check if results are identical
        boolean identical = true;
        double maxDifference = 0.0;
        
        for (int i = 0; i < standardResult.length; i++) {
            double realDiff = Math.abs(standardResult[i].real - optimizedResult[i].real);
            double imagDiff = Math.abs(standardResult[i].imag - optimizedResult[i].imag);
            
            maxDifference = Math.max(maxDifference, Math.max(realDiff, imagDiff));
            
            if (realDiff > 1e-10 || imagDiff > 1e-10) {
                identical = false;
            }
        }
        
        System.out.println("   Results identical: " + identical);
        System.out.println("   Max difference: " + String.format("%.2e", maxDifference));
        
        // Test round-trip consistency
        double[] standardRoundtrip = standardFFT.ifft(standardResult);
        double[] optimizedRoundtrip = optimizedFFT.ifft(optimizedResult);
        
        double maxRoundtripError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            maxRoundtripError = Math.max(maxRoundtripError, 
                Math.abs(signal[i] - standardRoundtrip[i]));
            maxRoundtripError = Math.max(maxRoundtripError, 
                Math.abs(signal[i] - optimizedRoundtrip[i]));
        }
        
        System.out.println("   Round-trip error: " + String.format("%.2e", maxRoundtripError));
        System.out.println();
    }
    
    private static void demonstrateTwiddleFactorCaching() {
        System.out.println("3. Twiddle Factor Caching:");
        
        // Clear cache to start fresh
        OptimizedFFT.clearCache();
        System.out.println("   Initial cache size: " + OptimizedFFT.getCacheSize());
        
        FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
        
        // Perform FFTs of different sizes
        int[] sizes = {4, 8, 16, 8, 4}; // Note: repeated sizes
        
        for (int size : sizes) {
            double[] signal = new double[size];
            Arrays.fill(signal, 1.0); // Simple DC signal
            
            optimizedFFT.fft(signal);
            System.out.println("   After FFT of size " + size + ": cache size = " + 
                OptimizedFFT.getCacheSize());
        }
        
        System.out.println("   Notice: Cache size doesn't increase for repeated sizes");
        
        // Clear cache
        OptimizedFFT.clearCache();
        System.out.println("   After clear: cache size = " + OptimizedFFT.getCacheSize());
        System.out.println();
    }
    
    private static void demonstratePerformance() {
        System.out.println("4. Performance Comparison (Quick Test):");
        
        int size = 1024;
        int iterations = 1000;
        
        // Generate test signal
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / size) + 0.5 * Math.cos(4 * Math.PI * i / size);
        }
        
        FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
        
        // Warm up JVM
        for (int i = 0; i < 100; i++) {
            standardFFT.fft(signal);
            optimizedFFT.fft(signal);
        }
        
        // Measure standard algorithm
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            standardFFT.fft(signal);
        }
        long standardTime = System.nanoTime() - startTime;
        
        // Measure optimized algorithm
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            optimizedFFT.fft(signal);
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        double speedup = (double) standardTime / optimizedTime;
        
        System.out.println("   Signal size: " + size + " points");
        System.out.println("   Iterations: " + iterations);
        System.out.println("   Standard algorithm: " + String.format("%.2f ms", standardTime / 1_000_000.0));
        System.out.println("   Optimized algorithm: " + String.format("%.2f ms", optimizedTime / 1_000_000.0));
        System.out.println("   Speedup: " + String.format("%.2fx", speedup));
        System.out.println();
        
        System.out.println("Key Benefits of Optimized Implementation:");
        System.out.println("   ✓ In-place computation reduces memory allocation");
        System.out.println("   ✓ Pre-computed twiddle factors eliminate redundant calculations");
        System.out.println("   ✓ Iterative algorithm avoids recursion overhead");
        System.out.println("   ✓ Better cache efficiency for large transforms");
        System.out.println("   ✓ Thread-safe twiddle factor caching");
        System.out.println("   ✓ Maintains numerical accuracy and API compatibility");
    }
}