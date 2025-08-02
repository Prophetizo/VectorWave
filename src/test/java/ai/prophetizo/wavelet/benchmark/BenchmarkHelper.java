package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.OptimizedTransformEngine;
import ai.prophetizo.wavelet.api.*;
import java.util.Random;

/**
 * Helper class for common benchmark configurations and utilities.
 */
public final class BenchmarkHelper {
    
    private static final Random RANDOM = new Random(42);
    
    private BenchmarkHelper() {
        // Utility class
    }
    
    /**
     * Creates a standard engine configuration for benchmarking.
     * Disables parallel processing to isolate SIMD performance.
     */
    public static OptimizedTransformEngine.EngineConfig createBenchmarkConfig() {
        return new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1)           // Disable parallel processing for pure SIMD
            .withSoALayout(true)          // Enable Structure-of-Arrays optimization
            .withSpecializedKernels(true) // Use optimized kernels
            .withCacheBlocking(true)      // Enable cache-aware blocking
            .withMemoryPool(true);        // Use memory pooling
    }
    
    /**
     * Creates a configuration for parallel benchmarking.
     */
    public static OptimizedTransformEngine.EngineConfig createParallelConfig() {
        return createParallelConfig(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates a configuration for parallel benchmarking with specified threads.
     */
    public static OptimizedTransformEngine.EngineConfig createParallelConfig(int threads) {
        return new OptimizedTransformEngine.EngineConfig()
            .withParallelism(threads)
            .withSoALayout(true)
            .withSpecializedKernels(true)
            .withCacheBlocking(true)
            .withMemoryPool(true);
    }
    
    /**
     * Creates a minimal configuration for baseline comparisons.
     */
    public static OptimizedTransformEngine.EngineConfig createMinimalConfig() {
        return new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1)
            .withSoALayout(false)
            .withSpecializedKernels(false)
            .withCacheBlocking(false)
            .withMemoryPool(false);
    }
    
    /**
     * Generates a batch of test signals with consistent random data.
     */
    public static double[][] generateBatch(int batchSize, int signalLength) {
        double[][] batch = new double[batchSize][signalLength];
        
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                batch[i][j] = RANDOM.nextGaussian();
            }
        }
        
        return batch;
    }
    
    /**
     * Generates a batch of signals with specific patterns for testing.
     */
    public static double[][] generatePatternedBatch(int batchSize, int signalLength, SignalPattern pattern) {
        double[][] batch = new double[batchSize][signalLength];
        
        for (int i = 0; i < batchSize; i++) {
            batch[i] = generateSignal(signalLength, pattern, i);
        }
        
        return batch;
    }
    
    private static double[] generateSignal(int length, SignalPattern pattern, int index) {
        double[] signal = new double[length];
        
        switch (pattern) {
            case RANDOM:
                for (int i = 0; i < length; i++) {
                    signal[i] = RANDOM.nextGaussian();
                }
                break;
                
            case SINUSOIDAL:
                double frequency = 2.0 * Math.PI * (index + 1) / length;
                for (int i = 0; i < length; i++) {
                    signal[i] = Math.sin(frequency * i);
                }
                break;
                
            case STEP:
                double stepValue = index % 2 == 0 ? 1.0 : -1.0;
                for (int i = 0; i < length; i++) {
                    signal[i] = i < length / 2 ? stepValue : -stepValue;
                }
                break;
                
            case MIXED:
                // Combination of patterns
                double freq = 2.0 * Math.PI / length;
                for (int i = 0; i < length; i++) {
                    signal[i] = Math.sin(freq * i) + 0.5 * Math.sin(3 * freq * i) + 
                               0.1 * RANDOM.nextGaussian();
                }
                break;
        }
        
        return signal;
    }
    
    /**
     * Enum for different signal patterns used in benchmarking.
     */
    public enum SignalPattern {
        RANDOM,
        SINUSOIDAL,
        STEP,
        MIXED
    }
    
    /**
     * Formats benchmark results for display.
     */
    public static String formatResults(String name, double timeMs, double baselineMs) {
        double speedup = baselineMs / timeMs;
        return String.format("%-20s: %.3f ms (%.2fx speedup)", name, timeMs, speedup);
    }
    
    /**
     * Calculates and formats throughput.
     */
    public static String formatThroughput(int numSignals, int signalLength, double timeMs) {
        double totalSamples = numSignals * signalLength;
        double throughputMSamples = totalSamples / (timeMs * 1000); // Mega-samples per second
        return String.format("Throughput: %.2f MSamples/sec", throughputMSamples);
    }
}