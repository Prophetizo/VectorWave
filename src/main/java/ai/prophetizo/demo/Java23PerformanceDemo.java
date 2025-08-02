package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demonstration of Java 23 performance features in VectorWave.
 * 
 * <p>This demo showcases the performance benefits of Java 23's Vector API,
 * pattern matching, and modern language features for high-performance
 * wavelet transforms.</p>
 * 
 * <p><strong>Features Demonstrated:</strong></p>
 * <ul>
 *   <li>Vector API acceleration for large signal processing</li>
 *   <li>Automatic algorithm selection based on signal characteristics</li>
 *   <li>Performance monitoring and estimation</li>
 *   <li>Modern Java 23 syntax and patterns</li>
 * </ul>
 */
public class Java23PerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave Java 23 Performance Demo ===\n");
        
        // Display system capabilities
        displaySystemCapabilities();
        
        // Demonstrate performance scaling
        demonstratePerformanceScaling();
        
        // Show automatic algorithm selection
        demonstrateAlgorithmSelection();
        
        // Compare different wavelets
        compareWaveletPerformance();
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    /**
     * Display system vector capabilities and Java 23 features.
     */
    private static void displaySystemCapabilities() {
        System.out.println("📊 System Capabilities:");
        
        var perfInfo = ScalarOps.getPerformanceInfo();
        System.out.println("  • " + perfInfo.description());
        
        if (perfInfo.vectorizationEnabled()) {
            var vectorInfo = perfInfo.vectorCapabilities();
            System.out.println("  • Vector Shape: " + vectorInfo.shape());
            System.out.println("  • Elements per Vector: " + vectorInfo.length());
            System.out.println("  • Vectorization Threshold: " + vectorInfo.threshold());
        }
        
        System.out.println("  • Available Processors: " + perfInfo.availableProcessors());
        System.out.println();
    }
    
    /**
     * Demonstrate performance scaling with different signal sizes.
     */
    private static void demonstratePerformanceScaling() {
        System.out.println("🚀 Performance Scaling Demonstration:");
        System.out.println("Signal Size | Est. Time | Speedup | Method");
        System.out.println("------------|-----------|---------|--------");
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        int[] testSizes = {64, 256, 1024, 4096, 16384};
        
        for (int size : testSizes) {
            var estimate = modwt.estimateProcessingTime(size);
            
            String method;
            if (estimate.vectorizationUsed()) {
                method = "Vector";
            } else {
                method = "Scalar";
            }
            
            System.out.printf("%-12d| %-10s| %-8s| %s%n",
                size,
                formatTime(estimate.estimatedTimeMs()),
                formatSpeedup(estimate.speedupFactor()),
                method
            );
        }
        System.out.println();
    }
    
    /**
     * Demonstrate automatic algorithm selection based on signal characteristics.
     */
    private static void demonstrateAlgorithmSelection() {
        System.out.println("🎯 Automatic Algorithm Selection:");
        System.out.println("Signal Length | Filter Length | Strategy");
        System.out.println("------------- | ------------- | --------");
        
        int[] signalLengths = {16, 64, 256, 1024, 8192};
        int[] filterLengths = {2, 4, 8};
        
        for (int signalLen : signalLengths) {
            for (int filterLen : filterLengths) {
                var strategy = VectorOps.selectOptimalStrategy(signalLen, filterLen);
                System.out.printf("%-13d | %-13d | %s%n",
                    signalLen, filterLen, strategy.getDescription());
            }
        }
        System.out.println();
    }
    
    /**
     * Compare performance across different wavelet types.
     */
    private static void compareWaveletPerformance() {
        System.out.println("📈 Wavelet Performance Comparison:");
        System.out.println("Wavelet | Signal Size | Processing Time | Performance Rating");
        System.out.println("--------|-------------|-----------------|------------------");
        
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4
        };
        
        int testSize = 4096;
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform modwt = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            var estimate = modwt.estimateProcessingTime(testSize);
            
            String rating;
            if (estimate.estimatedTimeMs() < 0.5) {
                rating = "Excellent ⭐⭐⭐⭐⭐";
            } else if (estimate.estimatedTimeMs() < 1.0) {
                rating = "Very Good ⭐⭐⭐⭐";
            } else if (estimate.estimatedTimeMs() < 2.0) {
                rating = "Good ⭐⭐⭐";
            } else if (estimate.estimatedTimeMs() < 5.0) {
                rating = "Fair ⭐⭐";
            } else {
                rating = "Basic ⭐";
            }
            
            System.out.printf("%-7s | %-11d | %-15s | %s%n",
                wavelet.name().toUpperCase(),
                testSize,
                formatTime(estimate.estimatedTimeMs()),
                rating
            );
        }
        System.out.println();
    }
    
    /**
     * Demonstrate real-world performance with actual timing.
     */
    private static void demonstrateRealPerformance() {
        System.out.println("⏱️ Real Performance Measurement:");
        
        // Generate test signal
        int signalSize = 8192;
        double[] signal = generateTestSignal(signalSize);
        
        MODWTTransform modwt = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Warm up JVM
        for (int i = 0; i < 10; i++) {
            modwt.forward(signal);
        }
        
        // Measure performance
        long startTime = System.nanoTime();
        var result = modwt.forward(signal);
        long forwardTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        modwt.inverse(result);
        long inverseTime = System.nanoTime() - startTime;
        
        System.out.printf("Signal Size: %d elements%n", signalSize);
        System.out.printf("Forward Transform: %.3f ms%n", forwardTime / 1_000_000.0);
        System.out.printf("Inverse Transform: %.3f ms%n", inverseTime / 1_000_000.0);
        System.out.printf("Total Processing: %.3f ms%n", (forwardTime + inverseTime) / 1_000_000.0);
        
        var estimate = modwt.estimateProcessingTime(signalSize);
        System.out.printf("Estimated Time: %.3f ms%n", estimate.estimatedTimeMs());
        System.out.println();
    }
    
    /**
     * Generate a test signal with interesting frequency content.
     */
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            // Multi-frequency signal with noise
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +     // 5 Hz component
                       0.5 * Math.sin(2 * Math.PI * 20 * t) + // 20 Hz component
                       0.1 * Math.random();                    // Noise
        }
        
        return signal;
    }
    
    /**
     * Format time with appropriate units.
     */
    private static String formatTime(double timeMs) {
        if (timeMs < 1.0) {
            return String.format("%.2f μs", timeMs * 1000);
        } else if (timeMs < 1000.0) {
            return String.format("%.2f ms", timeMs);
        } else {
            return String.format("%.2f s", timeMs / 1000.0);
        }
    }
    
    /**
     * Format speedup factor with appropriate precision.
     */
    private static String formatSpeedup(double speedup) {
        if (speedup < 1.5) {
            return "1.0x";
        } else if (speedup < 10.0) {
            return String.format("%.1fx", speedup);
        } else {
            return String.format("%.0fx", speedup);
        }
    }
}