package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import ai.prophetizo.wavelet.memory.BatchMemoryLayout;
import java.util.Random;

/**
 * Demonstrates the improved batch processing performance with true SIMD parallelization.
 */
public class ImprovedBatchDemo {
    
    public static void main(String[] args) {
        System.out.println("Improved Batch Processing Performance Demo");
        System.out.println("=========================================");
        System.out.println();
        
        // Show system info
        System.out.println(BatchSIMDTransform.getBatchSIMDInfo());
        System.out.println();
        
        // Test parameters
        int signalLength = 1024;
        int[] batchSizes = {4, 8, 16, 32, 64};
        int iterations = 1000;
        
        for (int batchSize : batchSizes) {
            System.out.println("Batch Size: " + batchSize);
            System.out.println("--------------------");
            
            // Generate test data
            double[][] signals = generateBatch(batchSize, signalLength);
            
            // Test different approaches
            testSequential(signals, iterations);
            testBasicBatch(signals, iterations);
            testOptimizedBatch(signals, iterations);
            testDirectSIMD(signals, iterations);
            testAlignedMemory(signals, iterations);
            
            System.out.println();
        }
        
        // Show API usage example
        showAPIExample();
    }
    
    private static void testSequential(double[][] signals, int iterations) {
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        long time = System.nanoTime() - startTime;
        
        double ms = time / 1_000_000.0 / iterations;
        System.out.printf("Sequential:      %.3f ms (baseline)\n", ms);
    }
    
    private static void testBasicBatch(double[][] signals, int iterations) {
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            transform.forwardBatch(signals);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            transform.forwardBatch(signals);
        }
        long time = System.nanoTime() - startTime;
        
        double ms = time / 1_000_000.0 / iterations;
        System.out.printf("Batch API:       %.3f ms\n", ms);
    }
    
    private static void testOptimizedBatch(double[][] signals, int iterations) {
        OptimizedTransformEngine engine = new OptimizedTransformEngine();
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        long time = System.nanoTime() - startTime;
        
        double ms = time / 1_000_000.0 / iterations;
        System.out.printf("Optimized:       %.3f ms\n", ms);
    }
    
    private static void testDirectSIMD(double[][] signals, int iterations) {
        int halfLength = signals[0].length / 2;
        double[][] approx = new double[signals.length][halfLength];
        double[][] detail = new double[signals.length][halfLength];
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
        }
        long time = System.nanoTime() - startTime;
        
        double ms = time / 1_000_000.0 / iterations;
        System.out.printf("Direct SIMD:     %.3f ms\n", ms);
    }
    
    private static void testAlignedMemory(double[][] signals, int iterations) {
        // Use aligned memory layout
        try (BatchMemoryLayout layout = new BatchMemoryLayout(signals.length, signals[0].length)) {
            // Warmup
            for (int i = 0; i < 100; i++) {
                layout.loadSignalsInterleaved(signals, true);
                layout.haarTransformInterleaved();
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                layout.loadSignalsInterleaved(signals, true);
                layout.haarTransformInterleaved();
            }
            long time = System.nanoTime() - startTime;
            
            double ms = time / 1_000_000.0 / iterations;
            System.out.printf("Aligned Memory:  %.3f ms\n", ms);
        }
    }
    
    private static void showAPIExample() {
        System.out.println("\nAPI Usage Example:");
        System.out.println("==================");
        System.out.println();
        System.out.println("// Simple batch processing");
        System.out.println("WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);");
        System.out.println("double[][] signals = generateSignals(32, 1024);");
        System.out.println("TransformResult[] results = transform.forwardBatch(signals);");
        System.out.println();
        System.out.println("// Reconstruction");
        System.out.println("double[][] reconstructed = transform.inverseBatch(results);");
        System.out.println();
        System.out.println("// Advanced optimization");
        System.out.println("OptimizedTransformEngine engine = new OptimizedTransformEngine();");
        System.out.println("TransformResult[] optimizedResults = engine.transformBatch(signals, wavelet, mode);");
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