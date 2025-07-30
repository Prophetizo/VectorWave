package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.MorletWavelet;

import java.util.Arrays;

/**
 * Demonstrates performance characteristics of Continuous Wavelet Transform (CWT) operations.
 * Shows timing comparisons and optimization strategies for CWT computations.
 */
public class CWTPerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - CWT Performance Demo");
        System.out.println("==================================");
        
        demonstrateBasicCWTPerformance();
        demonstrateSignalSizeImpact();
        demonstrateBoundaryModePerformance();
        demonstrateOptimizationStrategies();
    }
    
    private static void demonstrateBasicCWTPerformance() {
        System.out.println("\n1. Basic CWT Performance:");
        System.out.println("-------------------------");
        
        double[] signal = generateTestSignal(64);
        MorletWavelet morlet = new MorletWavelet();
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(morlet);
        
        // Warm up JVM
        for (int i = 0; i < 5; i++) {
            try {
                transform.forward(signal);
            } catch (Exception e) {
                // Ignore warm-up errors
            }
        }
        
        // Measure performance
        int iterations = 1000;
        long totalTime = 0;
        int successfulRuns = 0;
        
        for (int i = 0; i < iterations; i++) {
            try {
                long startTime = System.nanoTime();
                TransformResult result = transform.forward(signal);
                transform.inverse(result);
                long endTime = System.nanoTime();
                
                totalTime += (endTime - startTime);
                successfulRuns++;
            } catch (Exception e) {
                // Count failed attempts
            }
        }
        
        if (successfulRuns > 0) {
            double averageTime = totalTime / (double) successfulRuns / 1_000_000.0; // Convert to ms
            System.out.printf("Average CWT time (signal size %d): %.3f ms\n", signal.length, averageTime);
            System.out.printf("Successful runs: %d/%d\n", successfulRuns, iterations);
        } else {
            System.out.println("All CWT attempts failed - this may indicate an implementation issue");
        }
    }
    
    private static void demonstrateSignalSizeImpact() {
        System.out.println("\n2. Signal Size Impact on Performance:");
        System.out.println("------------------------------------");
        
        int[] signalSizes = {8, 16, 32, 64, 128, 256};
        MorletWavelet morlet = new MorletWavelet();
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(morlet);
        
        System.out.println("Signal Size | Avg Time (ms) | Throughput (transforms/sec)");
        System.out.println("-----------|---------------|---------------------------");
        
        for (int size : signalSizes) {
            double[] signal = generateTestSignal(size);
            
            // Warm up
            try {
                for (int i = 0; i < 3; i++) {
                    transform.forward(signal);
                }
            } catch (Exception e) {
                // Ignore warm-up errors
            }
            
            // Measure
            int iterations = Math.max(10, 1000 / size); // Fewer iterations for larger signals
            long totalTime = 0;
            int successfulRuns = 0;
            
            for (int i = 0; i < iterations; i++) {
                try {
                    long startTime = System.nanoTime();
                    TransformResult result = transform.forward(signal);
                    long endTime = System.nanoTime();
                    
                    totalTime += (endTime - startTime);
                    successfulRuns++;
                } catch (Exception e) {
                    // Count failed attempts
                }
            }
            
            if (successfulRuns > 0) {
                double averageTime = totalTime / (double) successfulRuns / 1_000_000.0;
                double throughput = 1000.0 / averageTime;
                
                System.out.printf("%10d | %13.3f | %25.1f\n", size, averageTime, throughput);
            } else {
                System.out.printf("%10d | %13s | %25s\n", size, "FAILED", "N/A");
            }
        }
    }
    
    private static void demonstrateBoundaryModePerformance() {
        System.out.println("\n3. Boundary Mode Performance Comparison:");
        System.out.println("----------------------------------------");
        
        double[] signal = generateTestSignal(128);
        MorletWavelet morlet = new MorletWavelet();
        
        BoundaryMode[] modes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING};
        
        for (BoundaryMode mode : modes) {
            WaveletTransformFactory factory = new WaveletTransformFactory()
                    .withBoundaryMode(mode);
            WaveletTransform transform = factory.create(morlet);
            
            // Warm up
            try {
                for (int i = 0; i < 5; i++) {
                    transform.forward(signal);
                }
            } catch (Exception e) {
                // Ignore warm-up errors
            }
            
            // Measure
            int iterations = 100;
            long totalTime = 0;
            int successfulRuns = 0;
            
            for (int i = 0; i < iterations; i++) {
                try {
                    long startTime = System.nanoTime();
                    TransformResult result = transform.forward(signal);
                    transform.inverse(result);
                    long endTime = System.nanoTime();
                    
                    totalTime += (endTime - startTime);
                    successfulRuns++;
                } catch (Exception e) {
                    // Count failed attempts
                }
            }
            
            if (successfulRuns > 0) {
                double averageTime = totalTime / (double) successfulRuns / 1_000_000.0;
                System.out.printf("%-13s: %.3f ms (success rate: %d%%)\n", 
                    mode.name(), averageTime, (successfulRuns * 100) / iterations);
            } else {
                System.out.printf("%-13s: FAILED (all attempts failed)\n", mode.name());
            }
        }
    }
    
    private static void demonstrateOptimizationStrategies() {
        System.out.println("\n4. CWT Optimization Strategies:");
        System.out.println("------------------------------");
        
        double[] signal = generateTestSignal(64);
        
        System.out.println("Strategy 1: Reuse Transform Objects");
        demonstrateTransformReuse(signal);
        
        System.out.println("\nStrategy 2: Signal Preprocessing");
        demonstrateSignalPreprocessing(signal);
        
        System.out.println("\nStrategy 3: Memory Usage Optimization");
        demonstrateMemoryOptimization(signal);
    }
    
    private static void demonstrateTransformReuse(double[] signal) {
        MorletWavelet morlet = new MorletWavelet();
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Strategy 1: Create new transform each time
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            try {
                WaveletTransform transform = factory.create(morlet);
                transform.forward(signal);
            } catch (Exception e) {
                // Ignore errors for timing comparison
            }
        }
        long newTransformTime = System.nanoTime() - startTime;
        
        // Strategy 2: Reuse transform object
        WaveletTransform reuseTransform = factory.create(morlet);
        startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            try {
                reuseTransform.forward(signal);
            } catch (Exception e) {
                // Ignore errors for timing comparison
            }
        }
        long reuseTime = System.nanoTime() - startTime;
        
        System.out.printf("   New transform each time: %.3f ms\n", newTransformTime / 1_000_000.0);
        System.out.printf("   Reuse transform object:  %.3f ms\n", reuseTime / 1_000_000.0);
        if (newTransformTime > 0 && reuseTime > 0) {
            double speedup = (double) newTransformTime / reuseTime;
            System.out.printf("   Speedup factor: %.2fx\n", speedup);
        }
    }
    
    private static void demonstrateSignalPreprocessing(double[] signal) {
        // Demonstrate the impact of signal normalization
        MorletWavelet morlet = new MorletWavelet();
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(morlet);
        
        // Original signal
        long startTime = System.nanoTime();
        try {
            transform.forward(signal);
        } catch (Exception e) {
            // Ignore for timing
        }
        long originalTime = System.nanoTime() - startTime;
        
        // Normalized signal
        double[] normalizedSignal = normalizeSignal(signal);
        startTime = System.nanoTime();
        try {
            transform.forward(normalizedSignal);
        } catch (Exception e) {
            // Ignore for timing
        }
        long normalizedTime = System.nanoTime() - startTime;
        
        System.out.printf("   Original signal:    %.3f ms\n", originalTime / 1_000_000.0);
        System.out.printf("   Normalized signal:  %.3f ms\n", normalizedTime / 1_000_000.0);
    }
    
    private static void demonstrateMemoryOptimization(double[] signal) {
        System.out.println("   Memory usage optimization tips:");
        System.out.println("   - Reuse array objects when possible");
        System.out.println("   - Process signals in batches");
        System.out.println("   - Use appropriate signal sizes (powers of 2)");
        
        // Show memory-friendly signal size
        int optimalSize = findOptimalSignalSize(signal.length);
        System.out.printf("   Current signal size: %d, Optimal size: %d\n", 
            signal.length, optimalSize);
    }
    
    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 4.0) +
                       0.1 * (Math.random() - 0.5);
        }
        return signal;
    }
    
    private static double[] normalizeSignal(double[] signal) {
        double mean = Arrays.stream(signal).average().orElse(0.0);
        double max = Arrays.stream(signal).map(Math::abs).max().orElse(1.0);
        
        double[] normalized = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            normalized[i] = (signal[i] - mean) / max;
        }
        return normalized;
    }
    
    private static int findOptimalSignalSize(int currentSize) {
        // Find the nearest power of 2
        int power = 1;
        while (power < currentSize) {
            power *= 2;
        }
        return power;
    }
}