package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;

import java.util.Arrays;

/**
 * Demonstrates Continuous Wavelet Transform (CWT) performance optimizations.
 * Shows comparison between different CWT implementations and optimization techniques.
 */
public class CWTPerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - CWT Performance Demo");
        System.out.println("=================================");
        
        // Test signals of different sizes
        int[] signalSizes = {16, 32, 64, 128};
        
        for (int size : signalSizes) {
            double[] signal = generateComplexSignal(size);
            System.out.println("\n--- Signal Size: " + size + " ---");
            
            demonstrateCWTBasicPerformance(signal);
            demonstrateFFTAcceleratedCWT(signal);
            demonstrateContinuousWaveletComparison(signal);
        }
        
        // Additional performance optimizations
        demonstrateScalabilityAnalysis();
        demonstrateMemoryEfficiency();
    }
    
    /**
     * Demonstrates basic CWT performance using standard Morlet wavelet.
     */
    private static void demonstrateCWTBasicPerformance(double[] signal) {
        System.out.println("\n1. Basic CWT Performance:");
        
        try {
            WaveletTransform transform = new WaveletTransformFactory()
                    .withBoundaryMode(BoundaryMode.PERIODIC)
                    .create(new MorletWavelet());
            
            // Measure forward transform
            long startTime = System.nanoTime();
            TransformResult result = transform.forward(signal);
            long forwardTime = System.nanoTime() - startTime;
            
            // Measure inverse transform
            startTime = System.nanoTime();
            double[] reconstructed = transform.inverse(result);
            long inverseTime = System.nanoTime() - startTime;
            
            double reconstructionError = calculateRMSE(signal, reconstructed);
            
            System.out.println("   Standard Morlet CWT:");
            System.out.println("     - Forward transform: " + String.format("%.3f ms", forwardTime / 1_000_000.0));
            System.out.println("     - Inverse transform: " + String.format("%.3f ms", inverseTime / 1_000_000.0));
            System.out.println("     - Total time: " + String.format("%.3f ms", (forwardTime + inverseTime) / 1_000_000.0));
            System.out.println("     - Reconstruction error: " + String.format("%.2e", reconstructionError));
            
        } catch (Exception e) {
            System.out.println("   ! Basic CWT error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates FFT-accelerated CWT performance.
     */
    private static void demonstrateFFTAcceleratedCWT(double[] signal) {
        System.out.println("\n2. FFT-Accelerated CWT:");
        
        try {
            // Create FFT-accelerated CWT instance
            FFTAcceleratedCWT fftCWT = new FFTAcceleratedCWT();
            
            // Measure FFT-accelerated transform using available methods
            long startTime = System.nanoTime();
            
            // Use FFT methods to demonstrate the acceleration
            FFTAcceleratedCWT.Complex[] fftResult = fftCWT.fft(signal);
            double[] ifftResult = fftCWT.ifft(fftResult);
            
            long fftTime = System.nanoTime() - startTime;
            
            // Calculate statistics
            int totalCoefficients = fftResult.length;
            double avgMagnitude = calculateAverageMagnitude(fftResult);
            
            System.out.println("   FFT-Accelerated Processing:");
            System.out.println("     - Transform time: " + String.format("%.3f ms", fftTime / 1_000_000.0));
            System.out.println("     - FFT coefficients: " + fftResult.length);
            System.out.println("     - Reconstructed samples: " + ifftResult.length);
            System.out.println("     - Total coefficients: " + totalCoefficients);
            System.out.println("     - Avg coefficient magnitude: " + String.format("%.4f", avgMagnitude));
            
            // Performance comparison with basic approach would require implementing basic CWT
            double efficiency = (double) totalCoefficients / (fftTime / 1_000_000.0);
            System.out.println("     - Efficiency: " + String.format("%.0f coeff/ms", efficiency));
            
        } catch (Exception e) {
            System.out.println("   ! FFT-accelerated processing error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates performance comparison between different continuous wavelets.
     */
    private static void demonstrateContinuousWaveletComparison(double[] signal) {
        System.out.println("\n3. Continuous Wavelet Comparison:");
        
        ContinuousWavelet[] wavelets = {
            new MorletWavelet()
            // Additional continuous wavelets would be added here when available
        };
        
        for (ContinuousWavelet wavelet : wavelets) {
            try {
                WaveletTransform transform = new WaveletTransformFactory()
                        .withBoundaryMode(BoundaryMode.PERIODIC)
                        .create(wavelet);
                
                // Measure performance
                long startTime = System.nanoTime();
                TransformResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                long totalTime = System.nanoTime() - startTime;
                
                double error = calculateRMSE(signal, reconstructed);
                double complexity = estimateComputationalComplexity(signal.length);
                
                System.out.println("   " + wavelet.name() + ":");
                System.out.println("     - Total time: " + String.format("%.3f ms", totalTime / 1_000_000.0));
                System.out.println("     - Reconstruction error: " + String.format("%.2e", error));
                System.out.println("     - Estimated complexity: O(" + String.format("%.0f", complexity) + ")");
                
            } catch (Exception e) {
                System.out.println("   " + wavelet.name() + ": ERROR - " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates scalability analysis of CWT algorithms.
     */
    private static void demonstrateScalabilityAnalysis() {
        System.out.println("\n4. Scalability Analysis:");
        
        int[] testSizes = {16, 32, 64, 128};
        
        System.out.println("   Performance scaling with signal size:");
        
        for (int size : testSizes) {
            double[] signal = generateComplexSignal(size);
            
            try {
                // Test with Morlet wavelet
                WaveletTransform transform = new WaveletTransformFactory()
                        .withBoundaryMode(BoundaryMode.PERIODIC)
                        .create(new MorletWavelet());
                
                // Warm up
                for (int i = 0; i < 10; i++) {
                    transform.forward(signal);
                }
                
                // Measure
                long startTime = System.nanoTime();
                int iterations = 100;
                for (int i = 0; i < iterations; i++) {
                    transform.forward(signal);
                }
                long totalTime = System.nanoTime() - startTime;
                
                double avgTime = totalTime / (iterations * 1_000_000.0);
                double timePerSample = avgTime / size;
                
                System.out.println("     Size " + size + ":");
                System.out.println("       - Avg transform time: " + String.format("%.4f ms", avgTime));
                System.out.println("       - Time per sample: " + String.format("%.6f ms", timePerSample));
                
            } catch (Exception e) {
                System.out.println("     Size " + size + ": ERROR - " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates memory efficiency considerations for CWT.
     */
    private static void demonstrateMemoryEfficiency() {
        System.out.println("\n5. Memory Efficiency Analysis:");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            // Create multiple CWT instances to analyze memory usage
            double[] signal = generateComplexSignal(64);
            WaveletTransform[] transforms = new WaveletTransform[10];
            
            for (int i = 0; i < transforms.length; i++) {
                transforms[i] = new WaveletTransformFactory()
                        .withBoundaryMode(BoundaryMode.PERIODIC)
                        .create(new MorletWavelet());
            }
            
            // Perform transforms
            TransformResult[] results = new TransformResult[transforms.length];
            for (int i = 0; i < transforms.length; i++) {
                results[i] = transforms[i].forward(signal);
            }
            
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            System.out.println("   Memory usage analysis:");
            System.out.println("     - Signal size: " + signal.length + " samples");
            System.out.println("     - Transform instances: " + transforms.length);
            System.out.println("     - Memory used: " + formatBytes(memoryUsed));
            System.out.println("     - Memory per transform: " + formatBytes(memoryUsed / transforms.length));
            System.out.println("     - Memory per coefficient: " + 
                              formatBytes(memoryUsed / (transforms.length * signal.length * 2))); // approx + detail
            
            // Test memory cleanup
            Arrays.fill(transforms, null);
            Arrays.fill(results, null);
            runtime.gc();
            
            long memoryAfterCleanup = runtime.totalMemory() - runtime.freeMemory();
            long memoryFreed = memoryAfter - memoryAfterCleanup;
            
            System.out.println("     - Memory freed on cleanup: " + formatBytes(memoryFreed));
            System.out.println("     - Cleanup efficiency: " + 
                              String.format("%.1f%%", (double) memoryFreed / memoryUsed * 100));
            
        } catch (Exception e) {
            System.out.println("   ! Memory analysis error: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private static double[] generateComplexSignal(int length) {
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            
            // Complex signal with multiple components
            signal[i] = 1.0 * Math.sin(2 * Math.PI * 2 * t) +        // Low frequency
                       0.5 * Math.sin(2 * Math.PI * 8 * t) +         // Medium frequency  
                       0.3 * Math.sin(2 * Math.PI * 16 * t) +        // High frequency
                       0.1 * (Math.random() - 0.5);                  // Noise
        }
        
        return signal;
    }
    
    private static double calculateRMSE(double[] original, double[] reconstructed) {
        if (original.length != reconstructed.length) {
            throw new IllegalArgumentException("Array lengths must match");
        }
        
        double sumSquaredError = 0;
        for (int i = 0; i < original.length; i++) {
            double error = original[i] - reconstructed[i];
            sumSquaredError += error * error;
        }
        
        return Math.sqrt(sumSquaredError / original.length);
    }
    
    private static double calculateAverageMagnitude(FFTAcceleratedCWT.Complex[] complexArray) {
        double sum = 0;
        
        for (FFTAcceleratedCWT.Complex complex : complexArray) {
            double magnitude = Math.sqrt(complex.real * complex.real + complex.imag * complex.imag);
            sum += magnitude;
        }
        
        return complexArray.length > 0 ? sum / complexArray.length : 0;
    }
    
    private static double calculateAverageMagnitude(double[][] matrix) {
        double sum = 0;
        int count = 0;
        
        for (double[] row : matrix) {
            for (double value : row) {
                sum += Math.abs(value);
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    private static double estimateComputationalComplexity(int n) {
        // Rough estimate of computational complexity for CWT
        // Actual complexity depends on implementation details
        return n * Math.log(n); // O(n log n) for FFT-based implementation
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}