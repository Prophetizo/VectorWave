package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import java.util.Random;

/**
 * Performance test for the new BatchSIMDTransform implementation.
 * Compares true SIMD batch processing against sequential processing.
 */
public class BatchSIMDPerformanceTest {
    
    public static void main(String[] args) {
        System.out.println("Batch SIMD Transform Performance Test");
        System.out.println("=====================================");
        System.out.println();
        
        // Test configurations
        int[] batchSizes = {4, 8, 16, 32, 64};
        int[] signalLengths = {256, 512, 1024, 2048};
        
        // Show SIMD info
        System.out.println(BatchSIMDTransform.getBatchSIMDInfo());
        System.out.println();
        
        for (int signalLength : signalLengths) {
            System.out.println("Signal Length: " + signalLength);
            System.out.println("----------------------------------------");
            
            for (int batchSize : batchSizes) {
                testBatchPerformance(batchSize, signalLength);
            }
            System.out.println();
        }
        
        // Test different wavelet types
        testWaveletTypes();
    }
    
    private static void testBatchPerformance(int batchSize, int signalLength) {
        // Generate test data
        double[][] signals = generateBatch(batchSize, signalLength);
        
        // Create engines
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        OptimizedTransformEngine engine = new OptimizedTransformEngine();
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        
        // Measure iterations
        int iterations = 1000;
        
        // Test sequential processing
        long startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        long sequentialTime = System.nanoTime() - startTime;
        
        // Test batch SIMD processing
        startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        long batchTime = System.nanoTime() - startTime;
        
        // Test direct SIMD (for comparison)
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults);
        }
        long directSIMDTime = System.nanoTime() - startTime;
        
        // Calculate results
        double sequentialMs = sequentialTime / 1_000_000.0 / iterations;
        double batchMs = batchTime / 1_000_000.0 / iterations;
        double directSIMDMs = directSIMDTime / 1_000_000.0 / iterations;
        double speedup = sequentialMs / batchMs;
        double directSpeedup = sequentialMs / directSIMDMs;
        
        System.out.printf("Batch size %2d: Sequential: %.3f ms, Batch: %.3f ms, Direct SIMD: %.3f ms, " +
                         "Speedup: %.2fx (Direct: %.2fx)\n", 
                         batchSize, sequentialMs, batchMs, directSIMDMs, speedup, directSpeedup);
    }
    
    private static void testWaveletTypes() {
        System.out.println("Wavelet Type Performance Comparison");
        System.out.println("===================================");
        
        int batchSize = 16;
        int signalLength = 1024;
        double[][] signals = generateBatch(batchSize, signalLength);
        
        Wavelet[] wavelets = {new Haar(), Daubechies.DB2, Daubechies.DB4};
        String[] names = {"Haar", "DB2", "DB4"};
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine();
        
        for (int i = 0; i < wavelets.length; i++) {
            // Warmup
            for (int j = 0; j < 100; j++) {
                engine.transformBatch(signals, wavelets[i], BoundaryMode.PERIODIC);
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int j = 0; j < 1000; j++) {
                engine.transformBatch(signals, wavelets[i], BoundaryMode.PERIODIC);
            }
            long time = System.nanoTime() - startTime;
            
            double ms = time / 1_000_000.0 / 1000;
            System.out.printf("%s: %.3f ms per batch\n", names[i], ms);
        }
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