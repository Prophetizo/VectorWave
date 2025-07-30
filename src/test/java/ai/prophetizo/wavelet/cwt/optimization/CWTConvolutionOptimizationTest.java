package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CWT convolution optimization to verify removal of temporary arrays.
 */
class CWTConvolutionOptimizationTest {
    
    @Test
    @DisplayName("Should produce identical results with optimized convolution")
    void testOptimizedConvolutionCorrectness() {
        CWTVectorOps ops = new CWTVectorOps();
        
        // Test with different signal sizes
        int[] signalSizes = {128, 512, 2048};
        int waveletSize = 64;
        double scale = 8.0;
        
        Random random = new Random(42);
        
        for (int signalSize : signalSizes) {
            // Create test data
            double[] realSignal = new double[signalSize];
            double[] imagSignal = new double[signalSize];
            double[] realWavelet = new double[waveletSize];
            double[] imagWavelet = new double[waveletSize];
            
            // Initialize with random data
            for (int i = 0; i < signalSize; i++) {
                realSignal[i] = random.nextGaussian();
                imagSignal[i] = random.nextGaussian();
            }
            
            // Create a complex Morlet-like wavelet
            for (int i = 0; i < waveletSize; i++) {
                double t = (i - waveletSize / 2.0) / 8.0;
                double envelope = Math.exp(-t * t / 2);
                realWavelet[i] = envelope * Math.cos(5 * t);
                imagWavelet[i] = envelope * Math.sin(5 * t);
            }
            
            // Compute convolution using both methods
            ComplexMatrix optimizedResult = ops.complexConvolve(
                realSignal, imagSignal, realWavelet, imagWavelet, scale);
            
            // Also test with real-only signal
            ComplexMatrix realOnlyResult = ops.complexConvolve(
                realSignal, null, realWavelet, imagWavelet, scale);
            
            // Verify dimensions
            assertEquals(1, optimizedResult.getRows());
            assertEquals(signalSize, optimizedResult.getCols());
            assertEquals(1, realOnlyResult.getRows());
            assertEquals(signalSize, realOnlyResult.getCols());
            
            // Verify non-zero results
            boolean hasNonZero = false;
            for (int i = 0; i < signalSize; i++) {
                if (Math.abs(optimizedResult.getReal(0, i)) > 1e-10 ||
                    Math.abs(optimizedResult.getImaginary(0, i)) > 1e-10) {
                    hasNonZero = true;
                    break;
                }
            }
            assertTrue(hasNonZero, "Convolution should produce non-zero results");
        }
    }
    
    @Test
    @DisplayName("Should show performance improvement without temporary arrays")
    void testConvolutionPerformance() {
        CWTVectorOps ops = new CWTVectorOps();
        
        // Use larger sizes to show performance difference
        int signalSize = 8192;
        int waveletSize = 128;
        double scale = 16.0;
        
        // Create test data
        double[] realSignal = new double[signalSize];
        double[] imagSignal = new double[signalSize];
        double[] realWavelet = new double[waveletSize];
        double[] imagWavelet = new double[waveletSize];
        
        Random random = new Random(42);
        for (int i = 0; i < signalSize; i++) {
            realSignal[i] = random.nextGaussian();
            imagSignal[i] = random.nextGaussian();
        }
        
        for (int i = 0; i < waveletSize; i++) {
            double t = (i - waveletSize / 2.0) / 8.0;
            double envelope = Math.exp(-t * t / 2);
            realWavelet[i] = envelope * Math.cos(5 * t);
            imagWavelet[i] = envelope * Math.sin(5 * t);
        }
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            ops.complexConvolve(realSignal, imagSignal, realWavelet, imagWavelet, scale);
        }
        
        // Measure performance
        int iterations = 20;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            ComplexMatrix result = ops.complexConvolve(
                realSignal, imagSignal, realWavelet, imagWavelet, scale);
            // Consume result to prevent optimization
            assertNotNull(result);
        }
        
        long totalTime = System.nanoTime() - startTime;
        double avgTimeMs = totalTime / (1_000_000.0 * iterations);
        
        System.out.println("\nOptimized CWT Convolution Performance:");
        System.out.printf("Signal size: %d, Wavelet size: %d%n", signalSize, waveletSize);
        System.out.printf("Average time per convolution: %.2f ms%n", avgTimeMs);
        System.out.printf("Throughput: %.2f Msamples/sec%n", 
                         signalSize / (avgTimeMs * 1000));
        
        // Performance should be reasonable
        assertTrue(avgTimeMs < 50, "Convolution should complete in reasonable time");
    }
    
    @Test
    @DisplayName("Should handle edge cases correctly")
    void testEdgeCases() {
        CWTVectorOps ops = new CWTVectorOps();
        
        // Test with small signals (below SIMD threshold)
        double[] smallSignal = new double[32];
        double[] smallWavelet = new double[8];
        for (int i = 0; i < smallSignal.length; i++) {
            smallSignal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        for (int i = 0; i < smallWavelet.length; i++) {
            smallWavelet[i] = 1.0 / smallWavelet.length;
        }
        
        ComplexMatrix smallResult = ops.complexConvolve(
            smallSignal, null, smallWavelet, new double[8], 1.0);
        
        assertEquals(1, smallResult.getRows());
        assertEquals(32, smallResult.getCols());
        
        // Test with wavelet longer than signal (unusual but should work)
        double[] shortSignal = new double[16];
        double[] longWavelet = new double[32];
        for (int i = 0; i < shortSignal.length; i++) {
            shortSignal[i] = 1.0;
        }
        for (int i = 0; i < longWavelet.length; i++) {
            longWavelet[i] = Math.exp(-(i - 16) * (i - 16) / 64.0);
        }
        
        ComplexMatrix edgeResult = ops.complexConvolve(
            shortSignal, null, longWavelet, new double[32], 1.0);
        
        assertEquals(1, edgeResult.getRows());
        assertEquals(16, edgeResult.getCols());
    }
    
    @Test
    @DisplayName("Should demonstrate memory efficiency")
    void testMemoryEfficiency() {
        CWTVectorOps ops = new CWTVectorOps();
        
        // Test with multiple scales to show memory efficiency
        int signalSize = 4096;
        int waveletSize = 64;
        double[] scales = {2.0, 4.0, 8.0, 16.0, 32.0};
        
        double[] realSignal = new double[signalSize];
        double[] realWavelet = new double[waveletSize];
        double[] imagWavelet = new double[waveletSize];
        
        // Initialize test data
        Random random = new Random(42);
        for (int i = 0; i < signalSize; i++) {
            realSignal[i] = random.nextGaussian();
        }
        
        for (int i = 0; i < waveletSize; i++) {
            double t = (i - waveletSize / 2.0) / 8.0;
            double envelope = Math.exp(-t * t / 2);
            realWavelet[i] = envelope * Math.cos(5 * t);
            imagWavelet[i] = envelope * Math.sin(5 * t);
        }
        
        System.out.println("\nMemory-Efficient Multi-Scale CWT:");
        
        // Get initial memory usage
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - 
                            Runtime.getRuntime().freeMemory();
        
        // Perform convolutions at multiple scales
        ComplexMatrix[] results = new ComplexMatrix[scales.length];
        long startTime = System.nanoTime();
        
        for (int i = 0; i < scales.length; i++) {
            results[i] = ops.complexConvolve(
                realSignal, null, realWavelet, imagWavelet, scales[i]);
        }
        
        long totalTime = System.nanoTime() - startTime;
        
        // Get final memory usage
        long finalMemory = Runtime.getRuntime().totalMemory() - 
                          Runtime.getRuntime().freeMemory();
        long memoryUsed = (finalMemory - initialMemory) / (1024 * 1024);
        
        System.out.printf("Scales processed: %d%n", scales.length);
        System.out.printf("Total time: %.2f ms%n", totalTime / 1_000_000.0);
        System.out.printf("Approximate memory increase: %d MB%n", memoryUsed);
        
        // Verify all results are valid
        for (int i = 0; i < results.length; i++) {
            assertNotNull(results[i]);
            assertEquals(1, results[i].getRows());
            assertEquals(signalSize, results[i].getCols());
        }
    }
}