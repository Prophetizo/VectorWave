package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import java.util.Random;

/**
 * Demonstrates the batch processing performance issue where batch SIMD processing
 * is actually slower than individual processing.
 */
public class BatchProcessingIssueDemo {
    
    public static void main(String[] args) {
        System.out.println("Batch Processing Performance Issue Demo");
        System.out.println("======================================");
        
        // Test parameters
        int signalLength = 1024;
        int batchSize = 32;
        int iterations = 1000;
        
        // Generate test data
        double[][] signals = new double[batchSize][signalLength];
        Random random = new Random(42);
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = random.nextGaussian();
            }
        }
        
        // Create engines
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        OptimizedTransformEngine engine = new OptimizedTransformEngine();
        
        // Warmup
        System.out.println("Warming up...");
        for (int i = 0; i < 100; i++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        
        // Test individual processing
        System.out.println("\nTesting individual processing...");
        long startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            for (double[] signal : signals) {
                transform.forward(signal);
            }
        }
        long individualTime = System.nanoTime() - startTime;
        
        // Test batch processing
        System.out.println("Testing batch processing...");
        startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        }
        long batchTime = System.nanoTime() - startTime;
        
        // Calculate and display results
        double individualMs = individualTime / 1_000_000.0 / iterations;
        double batchMs = batchTime / 1_000_000.0 / iterations;
        double speedup = individualMs / batchMs;
        
        System.out.println("\nResults:");
        System.out.println("- Individual processing time: " + String.format("%.2f", individualMs) + " ms");
        System.out.println("- Batch SIMD time: " + String.format("%.2f", batchMs) + " ms");
        System.out.println("- Speedup: " + String.format("%.2fx", speedup));
        
        if (speedup < 1.0) {
            System.out.println("\nISSUE: Batch processing is " + String.format("%.1f%%", (1.0 - speedup) * 100) + " SLOWER than individual processing!");
            System.out.println("This indicates the batch processing is not properly utilizing SIMD.");
        }
        
        // Show what's happening
        System.out.println("\nCurrent batch processing approach:");
        System.out.println("- Uses SoA layout for medium batches (4-16 signals)");
        System.out.println("- Falls back to sequential processing for larger batches");
        System.out.println("- No true SIMD parallelization across multiple signals");
    }
}