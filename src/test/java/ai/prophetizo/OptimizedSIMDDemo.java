package ai.prophetizo;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;

/**
 * Demonstration of optimized SIMD implementations.
 */
public class OptimizedSIMDDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave Optimized SIMD Demo");
        System.out.println("==============================\n");
        
        // Test different signal sizes to show scaling
        int[] signalSizes = {256, 512, 1024, 2048, 4096};
        
        for (int size : signalSizes) {
            System.out.println("Signal size: " + size + " samples");
            System.out.println("-".repeat(40));
            
            // Create test signal
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                           0.5 * Math.sin(2 * Math.PI * i / 16.0) +
                           0.25 * Math.sin(2 * Math.PI * i / 8.0);
            }
            
            // Test different wavelets
            testWavelet("Haar", new Haar(), signal);
            testWavelet("DB4", Daubechies.DB4, signal);
            
            System.out.println();
        }
        
        // Test specialized Haar optimization
        System.out.println("Specialized Haar Transform Test");
        System.out.println("===============================");
        testHaarSpecialized();
        
        // Test combined transform
        System.out.println("\nCombined Transform Test (DB4)");
        System.out.println("============================");
        testCombinedTransform();
    }
    
    private static void testWavelet(String name, Wavelet wavelet, double[] signal) {
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
            
        WaveletTransform scalarTransform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC, scalarConfig);
        WaveletTransform autoTransform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            scalarTransform.forward(signal);
            autoTransform.forward(signal);
        }
        
        // Measure scalar
        long scalarStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            TransformResult result = scalarTransform.forward(signal);
        }
        long scalarTime = System.nanoTime() - scalarStart;
        
        // Measure optimized
        long optimizedStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            TransformResult result = autoTransform.forward(signal);
        }
        long optimizedTime = System.nanoTime() - optimizedStart;
        
        double scalarMs = scalarTime / 1_000_000.0;
        double optimizedMs = optimizedTime / 1_000_000.0;
        double speedup = scalarMs / optimizedMs;
        
        System.out.printf("%-6s: Scalar=%6.2f ms, Optimized=%6.2f ms, Speedup=%.2fx\n",
                         name, scalarMs, optimizedMs, speedup);
    }
    
    private static void testHaarSpecialized() {
        int size = 4096;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = i % 100;
        }
        
        double[] approx = new double[size / 2];
        double[] detail = new double[size / 2];
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            VectorOpsOptimized.haarTransformVectorized(signal, approx, detail);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            VectorOpsOptimized.haarTransformVectorized(signal, approx, detail);
        }
        long elapsed = System.nanoTime() - start;
        
        double perTransform = (elapsed / 10000.0) / 1000.0; // microseconds
        System.out.printf("Specialized Haar (4096 samples): %.2f μs per transform\n", perTransform);
        System.out.printf("Throughput: %.2f million samples/second\n", 
                         (4096.0 * 10000) / (elapsed / 1000.0));
    }
    
    private static void testCombinedTransform() {
        int size = 2048;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.random() - 0.5;
        }
        
        // DB4 filters
        double[] lowFilter = Daubechies.DB4.lowPassDecomposition();
        double[] highFilter = Daubechies.DB4.highPassDecomposition();
        
        double[] approx = new double[size / 2];
        double[] detail = new double[size / 2];
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            VectorOpsOptimized.combinedTransformPeriodicVectorized(
                signal, lowFilter, highFilter, approx, detail);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 5000; i++) {
            VectorOpsOptimized.combinedTransformPeriodicVectorized(
                signal, lowFilter, highFilter, approx, detail);
        }
        long elapsed = System.nanoTime() - start;
        
        double perTransform = (elapsed / 5000.0) / 1000.0; // microseconds
        System.out.printf("Combined DB4 transform (2048 samples): %.2f μs per transform\n", perTransform);
        System.out.printf("Cache efficiency: Processing both filters in single pass\n");
    }
}