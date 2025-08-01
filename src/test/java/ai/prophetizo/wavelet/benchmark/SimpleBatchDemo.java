package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import java.util.Random;

/**
 * Simple demonstration of improved batch processing performance.
 */
public class SimpleBatchDemo {
    
    public static void main(String[] args) {
        System.out.println("Batch Processing Performance Improvements");
        System.out.println("========================================");
        System.out.println();
        
        // Test parameters
        int signalLength = 1024;
        int batchSize = 32;
        int iterations = 1000;
        
        // Generate test data
        double[][] signals = generateBatch(batchSize, signalLength);
        
        // Create transform with no parallel processing to avoid memory issues
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1)  // Disable parallel processing
            .withSoALayout(true)
            .withSpecializedKernels(true);
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        // Warmup
        System.out.println("Warming up...");
        for (int i = 0; i < 100; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        
        // Test 1: Sequential processing (baseline)
        System.out.println("\nTest 1: Sequential Processing (Baseline)");
        long startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        long sequentialTime = System.nanoTime() - startTime;
        double sequentialMs = sequentialTime / 1_000_000.0 / iterations;
        System.out.printf("Time per batch: %.3f ms\n", sequentialMs);
        System.out.printf("Time per signal: %.3f μs\n", sequentialMs * 1000 / batchSize);
        
        // Test 2: Optimized batch processing with SIMD
        System.out.println("\nTest 2: Optimized Batch Processing (SIMD)");
        startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        long batchTime = System.nanoTime() - startTime;
        double batchMs = batchTime / 1_000_000.0 / iterations;
        System.out.printf("Time per batch: %.3f ms\n", batchMs);
        System.out.printf("Time per signal: %.3f μs\n", batchMs * 1000 / batchSize);
        
        // Test 3: Direct SIMD (for comparison)
        System.out.println("\nTest 3: Direct SIMD Implementation");
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults);
        }
        long directTime = System.nanoTime() - startTime;
        double directMs = directTime / 1_000_000.0 / iterations;
        System.out.printf("Time per batch: %.3f ms\n", directMs);
        System.out.printf("Time per signal: %.3f μs\n", directMs * 1000 / batchSize);
        
        // Calculate speedups
        System.out.println("\nPerformance Summary:");
        System.out.println("-------------------");
        double optimizedSpeedup = sequentialMs / batchMs;
        double directSpeedup = sequentialMs / directMs;
        
        System.out.printf("Optimized batch speedup: %.2fx\n", optimizedSpeedup);
        System.out.printf("Direct SIMD speedup: %.2fx\n", directSpeedup);
        
        if (optimizedSpeedup > 1.0) {
            System.out.printf("\nSUCCESS: Batch processing is %.1f%% faster than sequential!\n", 
                (optimizedSpeedup - 1.0) * 100);
        }
        
        // Show what changed
        System.out.println("\nKey Improvements:");
        System.out.println("- True SIMD parallelization across multiple signals");
        System.out.println("- Processes N signals simultaneously (N = SIMD vector width)");
        System.out.println("- Optimized memory access patterns for vector operations");
        System.out.println("- Adaptive algorithm selection based on batch size");
        
        // Test different batch sizes
        System.out.println("\nSpeedup vs Batch Size:");
        System.out.println("----------------------");
        int[] testSizes = {2, 4, 8, 16, 32, 64};
        for (int size : testSizes) {
            double speedup = measureSpeedup(size, signalLength, 500);
            System.out.printf("Batch size %2d: %.2fx speedup\n", size, speedup);
        }
    }
    
    private static double measureSpeedup(int batchSize, int signalLength, int iterations) {
        double[][] signals = generateBatch(batchSize, signalLength);
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1);
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        // Warmup
        for (int i = 0; i < 50; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        
        // Measure sequential
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        long sequentialTime = System.nanoTime() - startTime;
        
        // Measure batch
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        long batchTime = System.nanoTime() - startTime;
        
        return (double) sequentialTime / batchTime;
    }
    
    private static double[][] generateBatch(int batchSize, int signalLength) {
        double[][] batch = new double[batchSize][signalLength];
        Random random = new Random(42);
        
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                batch[i][j] = random.nextGaussian();
            }
        }
        
        return batch;
    }
}