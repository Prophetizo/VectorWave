package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates optimization techniques with MODWT wavelets.
 * Shows performance comparisons, memory usage optimization,
 * and algorithm selection strategies.
 * 
 */
public class OptimizationDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - MODWT Optimization Demo");
        System.out.println("====================================");
        
        // Test signals of various sizes (MODWT handles any size!)
        double[] signal16 = generateTestSignal(16);
        double[] signal333 = generateTestSignal(333);
        double[] signal1024 = generateTestSignal(1024);
        
        demonstrateWaveletSelection(signal333);
        demonstrateBoundaryModeOptimization(signal333);
        demonstrateMemoryOptimization(signal333);
        demonstratePerformanceOptimization();
        demonstrateMODWTAdvantages();
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
        System.out.println("   Signal size: " + signal.length + " (non-power-of-2!)");
        
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
                MODWTTransform transform = new MODWTTransform(wavelet, mode);
                
                long startTime = System.nanoTime();
                MODWTResult result = transform.forward(signal);
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
     * Demonstrates memory usage optimization strategies with MODWT.
     */
    private static void demonstrateMemoryOptimization(double[] signal) {
        System.out.println("\n3. Memory Optimization with MODWT:");
        
        // Measure memory usage before transform
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        try {
            MODWTTransform transform = new MODWTTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
            
            // Perform multiple transforms to see memory pattern
            MODWTResult[] results = new MODWTResult[5];
            
            System.out.println("   Processing " + results.length + " signals of size " + signal.length);
            
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
            
            // Calculate memory savings vs DWT
            int dwtSize = nextPowerOfTwo(signal.length);
            long dwtMemory = (long)dwtSize * 2 * 8 * results.length; // approx + detail coeffs
            long modwtMemory = (long)signal.length * 2 * 8 * results.length;
            long memorySaved = dwtMemory - modwtMemory;
            
            System.out.println("   MODWT vs DWT memory comparison:");
            System.out.println("     - DWT would need: " + formatBytes(dwtMemory) + " (size " + dwtSize + ")");
            System.out.println("     - MODWT uses: " + formatBytes(modwtMemory) + " (size " + signal.length + ")");
            System.out.println("     - Memory saved: " + formatBytes(memorySaved) + " (" + 
                              String.format("%.1f%%", 100.0 * memorySaved / dwtMemory) + ")");
            
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
     * Demonstrates performance optimization techniques with MODWT.
     */
    private static void demonstratePerformanceOptimization() {
        System.out.println("\n4. Performance Optimization:");
        
        // Test with different signal sizes (including non-power-of-2!)
        int[] signalSizes = {100, 256, 333, 512, 777, 1024};
        Wavelet wavelet = new Haar(); // Fastest wavelet
        
        System.out.println("   Performance scaling with signal size:");
        
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        for (int size : signalSizes) {
            double[] testSignal = generateTestSignal(size);
            
            try {
                // Warm up JVM
                for (int i = 0; i < 100; i++) {
                    transform.forward(testSignal);
                }
                
                // Measure performance
                int iterations = 1000;
                long startTime = System.nanoTime();
                
                for (int i = 0; i < iterations; i++) {
                    MODWTResult result = transform.forward(testSignal);
                    transform.inverse(result);
                }
                
                long endTime = System.nanoTime();
                double avgTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
                double throughput = iterations / ((endTime - startTime) / 1_000_000_000.0);
                
                System.out.println("     Size " + size + ":");
                System.out.println("       - Avg time per transform: " + String.format("%.4f ms", avgTimeMs));
                System.out.println("       - Throughput: " + String.format("%.0f transforms/sec", throughput));
                
                // Show if SIMD is being used
                var perfInfo = transform.getPerformanceInfo();
                if (size == 1024) {
                    System.out.println("       - " + perfInfo.description());
                }
                
            } catch (Exception e) {
                System.out.println("     Size " + size + ": ERROR - " + e.getMessage());
            }
        }
        
        // Demonstrate batch processing optimization
        demonstrateBatchOptimization();
    }
    
    /**
     * Demonstrates batch processing optimization with MODWT.
     */
    private static void demonstrateBatchOptimization() {
        System.out.println("\n   Batch Processing Optimization:");
        
        try {
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            
            int batchSize = 100;
            int signalSize = 333; // Non-power-of-2!
            double[][] batch = new double[batchSize][];
            for (int i = 0; i < batchSize; i++) {
                batch[i] = generateTestSignal(signalSize);
            }
            
            // Sequential processing
            long startTime = System.nanoTime();
            for (double[] s : batch) {
                MODWTResult result = transform.forward(s);
                transform.inverse(result);
            }
            long sequentialTime = System.nanoTime() - startTime;
            
            // Optimized batch processing (reuse transform object)
            startTime = System.nanoTime();
            MODWTResult[] results = new MODWTResult[batchSize];
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
            
            // Show estimated processing time for larger batches
            System.out.println("     - Estimated time for 10,000 signals: " + 
                              String.format("%.2f s", transform.estimateProcessingTime(signalSize * 10000) / 1_000_000_000.0));
            
        } catch (Exception e) {
            System.out.println("     ! Batch optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates specific MODWT advantages for optimization.
     */
    private static void demonstrateMODWTAdvantages() {
        System.out.println("\n5. MODWT-Specific Optimization Advantages:");
        
        // Demonstrate shift-invariance
        System.out.println("   a) Shift-Invariance for Pattern Detection:");
        double[] signal = generateTestSignal(100);
        double[] shiftedSignal = new double[100];
        System.arraycopy(signal, 1, shiftedSignal, 0, 99);
        shiftedSignal[99] = signal[0]; // Circular shift by 1
        
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        MODWTResult result1 = transform.forward(signal);
        MODWTResult result2 = transform.forward(shiftedSignal);
        
        // Compare coefficient energies
        double energy1 = calculateEnergy(result1.detailCoeffs());
        double energy2 = calculateEnergy(result2.detailCoeffs());
        double energyDiff = Math.abs(energy1 - energy2) / energy1 * 100;
        
        System.out.println("     - Original signal detail energy: " + String.format("%.4f", energy1));
        System.out.println("     - Shifted signal detail energy: " + String.format("%.4f", energy2));
        System.out.println("     - Energy difference: " + String.format("%.2f%%", energyDiff));
        System.out.println("     - Shift-invariance preserved: " + (energyDiff < 1.0 ? "YES" : "NO"));
        
        // Demonstrate arbitrary length efficiency
        System.out.println("\n   b) Arbitrary Length Efficiency:");
        int[] testSizes = {97, 101, 199, 251, 397}; // Prime numbers!
        
        System.out.println("     Processing prime-sized signals:");
        for (int size : testSizes) {
            double[] testSignal = generateTestSignal(size);
            MODWTResult result = transform.forward(testSignal);
            
            int dwtPadding = nextPowerOfTwo(size) - size;
            double paddingPercent = 100.0 * dwtPadding / nextPowerOfTwo(size);
            
            System.out.printf("     - Size %3d: No padding needed (DWT would waste %d samples = %.1f%%)\n",
                            size, dwtPadding, paddingPercent);
        }
        
        // Multi-level decomposition efficiency
        System.out.println("\n   c) Multi-Level Decomposition:");
        MultiLevelMODWTTransform mlTransform = new MultiLevelMODWTTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        double[] largeSignal = generateTestSignal(777);
        int maxLevels = mlTransform.getMaxDecompositionLevel(largeSignal.length);
        
        System.out.println("     Signal size: " + largeSignal.length);
        System.out.println("     Max decomposition levels: " + maxLevels);
        
        MultiLevelMODWTResult mlResult = mlTransform.forward(largeSignal, 3);
        System.out.println("     Performed 3-level decomposition:");
        for (int level = 1; level <= 3; level++) {
            System.out.printf("       - Level %d: %d coefficients\n", 
                            level, mlResult.getDetailCoeffsAtLevel(level).length);
        }
        
        System.out.println("\n   ✓ MODWT optimization advantages demonstrated");
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
    
    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }
}