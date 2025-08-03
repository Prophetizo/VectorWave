package ai.prophetizo.demo;

import ai.prophetizo.wavelet.OptimizedTransformEngine;
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates optimization techniques with wavelets.
 * Shows performance comparisons, memory usage optimization,
 * and algorithm selection strategies.
 */
public class OptimizationDemo {
    
    /* TODO: This demo needs to be migrated to MODWT.
     * The demo uses DWT-specific features that need careful adaptation:
     * - Factory patterns (MODWT uses direct instantiation)
     * - FFM features (needs MODWT-specific FFM implementation)
     * - Streaming features (needs MODWT streaming implementation)
     * Temporarily disabled to allow compilation.
     */
    public static void main_disabled(String[] args) {
        System.out.println("This demo is temporarily disabled during DWT to MODWT migration.");
        System.out.println("Please check back later or contribute to the migration effort!");
    }
    
    public static void main_original(String[] args) {
        System.out.println("VectorWave - Optimization Demo");
        System.out.println("==============================");
        
        // Test signal
        double[] signal = generateTestSignal(16);
        
        demonstrateWaveletSelection(signal);
        demonstrateBoundaryModeOptimization(signal);
        demonstrateMemoryOptimization(signal);
        demonstratePerformanceOptimization(signal);
    }
    
    /**
     * Demonstrates how to select the optimal wavelet for different use cases.
     */
    private static void demonstrateWaveletSelection(double[] signal) {
        System.out.println("\n1. Wavelet Selection Optimization:");
        
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Coiflet.COIF1,
            BiorthogonalSpline.BIOR1_3
        };
        
        System.out.println("   Comparing wavelets for reconstruction accuracy...");
        
        double bestError = Double.MAX_VALUE;
        Wavelet bestWavelet = null;
        
        for (Wavelet wavelet : wavelets) {
            try {
                MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
                
                long startTime = System.nanoTime();
                MODWTResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                long endTime = System.nanoTime();
                
                double error = calculateReconstructionError(signal, reconstructed);
                double timeMs = (endTime - startTime) / 1_000_000.0;
                
                System.out.println("     " + wavelet.name() + ":");
                System.out.println("       - Reconstruction error: " + String.format("%.2e", error));
                System.out.println("       - Transform time: " + String.format("%.3f ms", timeMs));
                
                if (error < bestError) {
                    bestError = error;
                    bestWavelet = wavelet;
                }
                
            } catch (Exception e) {
                System.out.println("     " + wavelet.name() + ": ERROR - " + e.getMessage());
            }
        }
        
        if (bestWavelet != null) {
            System.out.println("   ✓ Optimal wavelet: " + bestWavelet.name() + 
                              " (error: " + String.format("%.2e", bestError) + ")");
        }
    }
    
    /**
     * Demonstrates boundary mode optimization for different signal characteristics.
     */
    private static void demonstrateBoundaryModeOptimization(double[] signal) {
        System.out.println("\n2. Boundary Mode Optimization:");
        
        BoundaryMode[] modes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING};
        Wavelet wavelet = Daubechies.DB4;
        
        System.out.println("   Testing boundary modes with " + wavelet.name() + "...");
        
        for (BoundaryMode mode : modes) {
            try {
                WaveletTransform transform = new WaveletTransformFactory()
                        .boundaryMode(mode)
                        .create(wavelet);
                
                long startTime = System.nanoTime();
                TransformResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                long endTime = System.nanoTime();
                
                double error = calculateReconstructionError(signal, reconstructed);
                double timeMs = (endTime - startTime) / 1_000_000.0;
                double compressionRatio = calculateCompressionRatio(result);
                
                System.out.println("     " + mode + ":");
                System.out.println("       - Reconstruction error: " + String.format("%.2e", error));
                System.out.println("       - Transform time: " + String.format("%.3f ms", timeMs));
                System.out.println("       - Compression ratio: " + String.format("%.2f", compressionRatio));
                
            } catch (Exception e) {
                System.out.println("     " + mode + ": ERROR - " + e.getMessage());
            }
        }
    }
    
    /**
     * Demonstrates memory usage optimization strategies.
     */
    private static void demonstrateMemoryOptimization(double[] signal) {
        System.out.println("\n3. Memory Optimization:");
        
        // Measure memory usage before transform
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        try {
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(Daubechies.DB2); // Smaller filter = less memory
            
            // Perform multiple transforms to see memory pattern
            TransformResult[] results = new TransformResult[5];
            
            for (int i = 0; i < results.length; i++) {
                results[i] = transform.forward(signal);
            }
            
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            System.out.println("   Memory analysis:");
            System.out.println("     - Memory before: " + formatBytes(memoryBefore));
            System.out.println("     - Memory after: " + formatBytes(memoryAfter));
            System.out.println("     - Memory used: " + formatBytes(memoryUsed));
            System.out.println("     - Memory per transform: " + formatBytes(memoryUsed / results.length));
            
            // Demonstrate memory cleanup
            Arrays.fill(results, null);
            runtime.gc();
            long memoryAfterCleanup = runtime.totalMemory() - runtime.freeMemory();
            
            System.out.println("     - Memory after cleanup: " + formatBytes(memoryAfterCleanup));
            System.out.println("   ✓ Memory efficiency demonstrated");
            
        } catch (Exception e) {
            System.out.println("   ! Memory optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates performance optimization techniques.
     */
    private static void demonstratePerformanceOptimization(double[] signal) {
        System.out.println("\n4. Performance Optimization:");
        
        // Test with different signal sizes (all powers of 2)
        int[] signalSizes = {8, 16, 32, 64};
        Wavelet wavelet = new Haar(); // Fastest wavelet
        
        System.out.println("   Performance scaling with signal size:");
        
        for (int size : signalSizes) {
            if (size <= signal.length) {
                double[] testSignal = Arrays.copyOf(signal, size);
                
                try {
                    WaveletTransform transform = new WaveletTransformFactory()
                            .boundaryMode(BoundaryMode.PERIODIC)
                            .create(wavelet);
                    
                    // Warm up JVM
                    for (int i = 0; i < 100; i++) {
                        transform.forward(testSignal);
                    }
                    
                    // Measure performance
                    int iterations = 1000;
                    long startTime = System.nanoTime();
                    
                    for (int i = 0; i < iterations; i++) {
                        TransformResult result = transform.forward(testSignal);
                        transform.inverse(result);
                    }
                    
                    long endTime = System.nanoTime();
                    double avgTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
                    double throughput = iterations / ((endTime - startTime) / 1_000_000_000.0);
                    
                    System.out.println("     Size " + size + ":");
                    System.out.println("       - Avg time per transform: " + String.format("%.4f ms", avgTimeMs));
                    System.out.println("       - Throughput: " + String.format("%.0f transforms/sec", throughput));
                    
                } catch (Exception e) {
                    System.out.println("     Size " + size + ": ERROR - " + e.getMessage());
                }
            }
        }
        
        // Demonstrate batch processing optimization
        demonstrateBatchOptimization(signal, wavelet);
    }
    
    /**
     * Demonstrates batch processing optimization.
     */
    private static void demonstrateBatchOptimization(double[] signal, Wavelet wavelet) {
        System.out.println("\n   Batch Processing Optimization:");
        
        try {
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(wavelet);
            
            int batchSize = 100;
            double[][] batch = new double[batchSize][];
            for (int i = 0; i < batchSize; i++) {
                batch[i] = Arrays.copyOf(signal, signal.length);
            }
            
            // Sequential processing
            long startTime = System.nanoTime();
            for (double[] s : batch) {
                TransformResult result = transform.forward(s);
                transform.inverse(result);
            }
            long sequentialTime = System.nanoTime() - startTime;
            
            // Optimized batch processing (reuse transform object)
            startTime = System.nanoTime();
            TransformResult[] results = new TransformResult[batchSize];
            for (int i = 0; i < batchSize; i++) {
                results[i] = transform.forward(batch[i]);
            }
            for (int i = 0; i < batchSize; i++) {
                transform.inverse(results[i]);
            }
            long batchTime = System.nanoTime() - startTime;
            
            double improvement = ((double) sequentialTime / batchTime - 1) * 100;
            
            System.out.println("     - Sequential time: " + String.format("%.2f ms", sequentialTime / 1_000_000.0));
            System.out.println("     - Batch time: " + String.format("%.2f ms", batchTime / 1_000_000.0));
            System.out.println("     - Performance improvement: " + String.format("%.1f%%", improvement));
            
        } catch (Exception e) {
            System.out.println("     ! Batch optimization error: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Mix of different frequencies with some noise
            signal[i] = Math.sin(2 * Math.PI * i / length) + 
                       0.5 * Math.sin(4 * Math.PI * i / length) +
                       0.1 * (Math.random() - 0.5);
        }
        return signal;
    }
    
    private static double calculateReconstructionError(double[] original, double[] reconstructed) {
        if (original.length != reconstructed.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }
        
        double sumSquaredError = 0;
        for (int i = 0; i < original.length; i++) {
            double error = original[i] - reconstructed[i];
            sumSquaredError += error * error;
        }
        
        return Math.sqrt(sumSquaredError / original.length); // RMSE
    }
    
    private static double calculateCompressionRatio(MODWTResult result) {
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // Count significant coefficients (above threshold)
        double threshold = 1e-6;
        long significantCoeffs = Arrays.stream(approx).filter(x -> Math.abs(x) > threshold).count() +
                                Arrays.stream(detail).filter(x -> Math.abs(x) > threshold).count();
        
        double totalCoeffs = approx.length + detail.length;
        return totalCoeffs / Math.max(1, significantCoeffs);
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}