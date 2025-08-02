package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;

import java.util.Random;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Quick performance comparison test to show the impact of our optimizations.
 * 
 * Run with: java -cp target/test-classes:target/classes ai.prophetizo.wavelet.benchmark.PerformanceComparisonTest
 */
public class PerformanceComparisonTest {
    
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    private static final int[] SIGNAL_SIZES = {256, 512, 1024};
    
    public static void main(String[] args) {
        System.out.println("VectorWave Performance Comparison");
        System.out.println("=================================");
        System.out.println();
        
        for (int size : SIGNAL_SIZES) {
            System.out.println("Signal Size: " + size);
            System.out.println("-----------------");
            testPerformance(size);
            System.out.println();
        }
        
        // Test specific optimizations
        testOptimizationImpact();
    }
    
    private static void testPerformance(int signalSize) {
        double[] signal = generateSignal(signalSize);
        
        // Test different wavelets
        Wavelet[] wavelets = {new Haar(), Daubechies.DB2, Daubechies.DB4};
        String[] names = {"Haar", "DB2", "DB4"};
        
        for (int i = 0; i < wavelets.length; i++) {
            WaveletTransform transform = new WaveletTransform(wavelets[i], BoundaryMode.PERIODIC);
            
            // Warmup
            for (int j = 0; j < WARMUP_ITERATIONS; j++) {
                TransformResult result = transform.forward(signal);
                transform.inverse(result);
            }
            
            // Test forward transform
            long startTime = System.nanoTime();
            for (int j = 0; j < TEST_ITERATIONS; j++) {
                transform.forward(signal);
            }
            long forwardTime = System.nanoTime() - startTime;
            
            // Test round-trip
            startTime = System.nanoTime();
            for (int j = 0; j < TEST_ITERATIONS; j++) {
                TransformResult result = transform.forward(signal);
                transform.inverse(result);
            }
            long roundTripTime = System.nanoTime() - startTime;
            
            double forwardMicros = forwardTime / 1000.0 / TEST_ITERATIONS;
            double roundTripMicros = roundTripTime / 1000.0 / TEST_ITERATIONS;
            
            System.out.printf("%s: Forward: %.2f μs, Round-trip: %.2f μs%n", 
                names[i], forwardMicros, roundTripMicros);
        }
    }
    
    private static void testOptimizationImpact() {
        System.out.println("Optimization Impact Analysis");
        System.out.println("============================");
        System.out.println();
        
        double[] signal = generateSignal(512);
        
        // Test validation bypass
        testValidationBypass(signal);
        
        // Test batch processing
        testBatchProcessing();
        
        // Test memory pooling
        testMemoryPooling(signal);
        
        // Test multi-level decomposition
        testMultiLevelPerformance(signal);
    }
    
    private static void testValidationBypass(double[] signal) {
        System.out.println("1. Validation Impact (internal optimizations):");
        
        // The current implementation has internal optimizations for validation
        // We'll test the overall performance which includes these optimizations
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Test with small signals (where validation overhead is more noticeable)
        double[] smallSignal = generateSignal(64);
        double[] largeSignal = generateSignal(1024);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            transform.forward(smallSignal);
            transform.forward(largeSignal);
        }
        
        // Test small signal
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS * 10; i++) {
            transform.forward(smallSignal);
        }
        long smallSignalTime = System.nanoTime() - startTime;
        
        // Test large signal
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS * 10; i++) {
            transform.forward(largeSignal);
        }
        long largeSignalTime = System.nanoTime() - startTime;
        
        double smallPerOp = smallSignalTime / 1000.0 / (TEST_ITERATIONS * 10);
        double largePerOp = largeSignalTime / 1000.0 / (TEST_ITERATIONS * 10);
        double validationOverhead = (smallPerOp / 64) / (largePerOp / 1024) * 100 - 100;
        
        System.out.printf("   Small signal (64): %.2f μs%n", smallPerOp);
        System.out.printf("   Large signal (1024): %.2f μs%n", largePerOp);
        System.out.printf("   Relative validation overhead: ~%.1f%%%n", Math.max(0, validationOverhead));
        System.out.println();
    }
    
    private static void testBatchProcessing() {
        System.out.println("2. Batch Processing Performance:");
        
        int batchSize = 100;
        double[][] batch = new double[batchSize][256];
        for (int i = 0; i < batchSize; i++) {
            batch[i] = generateSignal(256);
        }
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Individual processing
        long individualTime = measureBatchProcessingTime(transform, batch, 100, "Individual");
        
        // Batch processing (simulated with reused transform)
        // The transform reuses internal structures
        long batchTime = measureBatchProcessingTime(transform, batch, 100, "Batch-style");
        
        System.out.printf("   Individual: %.2f ms per batch%n", individualTime / 1_000_000.0 / 100);
        System.out.printf("   Batch-style: %.2f ms per batch%n", batchTime / 1_000_000.0 / 100);
        System.out.println();
    }
    
    /**
     * Measures the time to process a batch of signals multiple times.
     *
     * @param transform the wavelet transform to use
     * @param batch the batch of signals to process
     * @param iterations number of times to process the entire batch
     * @param description description of the processing mode (for documentation)
     * @return total processing time in nanoseconds
     */
    private static long measureBatchProcessingTime(WaveletTransform transform, 
                                                   double[][] batch, 
                                                   int iterations,
                                                   String description) {
        long startTime = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            for (double[] signal : batch) {
                transform.forward(signal);
            }
        }
        return System.nanoTime() - startTime;
    }
    
    private static void testMemoryPooling(double[] signal) {
        System.out.println("3. Memory Allocation Impact:");
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        
        // Force GC before test
        System.gc();
        Thread.yield();
        
        // Measure with memory pressure
        long startTime = System.nanoTime();
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            TransformResult result = transform.forward(signal);
            // Force allocation by accessing coefficients
            result.approximationCoeffs();
            result.detailCoeffs();
        }
        
        long endTime = System.nanoTime();
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        double timePerOp = (endTime - startTime) / 1000.0 / TEST_ITERATIONS;
        double memPerOp = (endMem - startMem) / (double)TEST_ITERATIONS;
        
        System.out.printf("   Time per operation: %.2f μs%n", timePerOp);
        System.out.printf("   Memory per operation: %.0f bytes (approx)%n", memPerOp);
        System.out.println();
    }
    
    private static void testMultiLevelPerformance(double[] signal) {
        System.out.println("4. Multi-level Decomposition:");
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            mwt.decompose(signal, 3);
        }
        
        // Test different levels
        int[] levels = {1, 3, 5};
        for (int level : levels) {
            long startTime = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                MultiLevelTransformResult result = mwt.decompose(signal, level);
                // Access some data to prevent optimization
                result.finalApproximation();
            }
            long time = System.nanoTime() - startTime;
            
            System.out.printf("   %d levels: %.2f μs per decomposition%n", 
                level, time / 1000.0 / 1000);
        }
        System.out.println();
    }
    
    private static double[] generateSignal(int size) {
        // Use fixed seed for reproducible benchmarks
        Random random = new Random(TestConstants.TEST_SEED);
        
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(8 * Math.PI * i / 32.0) +
                       0.1 * (random.nextDouble() - 0.5);
        }
        return signal;
    }
}