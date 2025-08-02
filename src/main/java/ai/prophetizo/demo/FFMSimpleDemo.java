package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.memory.ffm.*;

import java.util.Random;

/**
 * Simple demonstration of FFM wavelet transform implementation.
 * 
 * @since 2.0.0
 */
public class FFMSimpleDemo {
    
    public static void main(String[] args) {
        System.out.println("=== FFM Wavelet Transform Simple Demo ===\n");
        
        // Generate test signal
        int signalSize = 1024;
        double[] signal = generateSignal(signalSize);
        
        // Use Haar wavelet (simplest)
        Wavelet wavelet = new Haar();
        
        System.out.println("1. Basic Comparison: Traditional vs FFM");
        System.out.println("----------------------------------------");
        
        // Traditional approach
        WaveletTransform traditional = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        long tradStart = System.nanoTime();
        TransformResult tradResult = traditional.forward(signal);
        double[] tradRecon = traditional.inverse(tradResult);
        long tradTime = System.nanoTime() - tradStart;
        
        // FFM approach
        try (FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet)) {
            long ffmStart = System.nanoTime();
            TransformResult ffmResult = ffm.forward(signal);
            double[] ffmRecon = ffm.inverse(ffmResult);
            long ffmTime = System.nanoTime() - ffmStart;
            
            // Verify results match
            double maxDiff = 0;
            for (int i = 0; i < signal.length; i++) {
                maxDiff = Math.max(maxDiff, Math.abs(tradRecon[i] - ffmRecon[i]));
            }
            
            System.out.printf("Signal size: %d%n", signalSize);
            System.out.printf("Traditional time: %.2f ms%n", tradTime / 1e6);
            System.out.printf("FFM time: %.2f ms%n", ffmTime / 1e6);
            System.out.printf("Speedup: %.2fx%n", (double) tradTime / ffmTime);
            System.out.printf("Max difference: %.2e%n", maxDiff);
            System.out.printf("Results match: %s%n", maxDiff < 1e-10 ? "YES" : "NO");
        }
        
        System.out.println("\n2. Memory Pool Statistics");
        System.out.println("-------------------------");
        
        FFMMemoryPool pool = new FFMMemoryPool();
        try (FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet, pool)) {
            // Warm up the pool
            for (int i = 0; i < 10; i++) {
                TransformResult result = ffm.forward(signal);
                ffm.inverse(result);
            }
            
            // Show statistics
            var stats = pool.getStatistics();
            System.out.println(stats.toDetailedString());
        } finally {
            pool.close();
        }
        
        System.out.println("\n3. Zero-Copy Streaming");
        System.out.println("----------------------");
        
        try (FFMStreamingTransform streaming = new FFMStreamingTransform(wavelet, 256)) {
            // Process in chunks
            int chunkSize = 64;
            int processed = 0;
            
            while (processed < signalSize) {
                int size = Math.min(chunkSize, signalSize - processed);
                streaming.processChunk(signal, processed, size);
                processed += size;
                
                if (streaming.hasCompleteBlock()) {
                    TransformResult result = streaming.getNextResult();
                    System.out.printf("Processed block: %d coefficients%n", 
                                    result.approximationCoeffs().length);
                }
            }
        }
        
        System.out.println("\nDemo completed successfully!");
    }
    
    private static double[] generateSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian() * 0.1 +
                       Math.sin(2 * Math.PI * i / 50);
        }
        
        return signal;
    }
}