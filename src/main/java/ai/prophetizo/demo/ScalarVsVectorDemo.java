package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Symlet;

import java.util.Arrays;

/**
 * Demonstrates MODWT's automatic scalar vs vector (SIMD) optimization.
 *
 * <p>This demo shows:
 * <ul>
 *   <li>How MODWT automatically selects optimal implementation</li>
 *   <li>Performance characteristics with different signal sizes</li>
 *   <li>When SIMD optimization kicks in</li>
 *   <li>Performance monitoring capabilities</li>
 * </ul>
 */
public class ScalarVsVectorDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave MODWT Automatic Optimization Demo ===\n");

        // Demo 1: Small signal (typically uses scalar)
        demonstrateSmallSignal();

        // Demo 2: Large signal (typically uses SIMD)
        demonstrateLargeSignal();

        // Demo 3: Performance monitoring
        demonstratePerformanceMonitoring();

        // Demo 4: Performance comparison across sizes
        demonstratePerformanceScaling();
    }

    private static void demonstrateSmallSignal() {
        System.out.println("1. Small Signal Processing (Typically Scalar)");
        System.out.println("---------------------------------------------");

        // Small signals typically use scalar operations
        int smallSize = 32;
        double[] signal = createTestSignal(smallSize);
        
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        long startTime = System.nanoTime();
        MODWTResult result = transform.forward(signal);
        long endTime = System.nanoTime();
        
        double milliseconds = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Signal size: %d%n", smallSize);
        System.out.printf("Transform time: %.3f ms%n", milliseconds);
        System.out.println("Coefficients length: " + result.approximationCoeffs().length);
        System.out.println("Note: Small signals typically use scalar operations for efficiency");
        System.out.println();
    }

    private static void demonstrateLargeSignal() {
        System.out.println("2. Large Signal Processing (Typically SIMD)");
        System.out.println("-------------------------------------------");

        // Large signals typically benefit from SIMD operations
        int largeSize = 4096;
        double[] signal = createTestSignal(largeSize);
        
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        long startTime = System.nanoTime();
        MODWTResult result = transform.forward(signal);
        long endTime = System.nanoTime();
        
        double milliseconds = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Signal size: %d%n", largeSize);
        System.out.printf("Transform time: %.3f ms%n", milliseconds);
        System.out.println("Coefficients length: " + result.approximationCoeffs().length);
        System.out.println("Note: Large signals typically use SIMD operations for performance");
        System.out.println();
    }

    private static void demonstratePerformanceMonitoring() {
        System.out.println("3. Performance Monitoring");
        System.out.println("-------------------------");

        MODWTTransform transform = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
        
        // Test with various sizes to see optimization behavior
        int[] sizes = {16, 64, 256, 1024};
        
        for (int size : sizes) {
            double[] signal = createTestSignal(size);
            
            // Warm up
            for (int i = 0; i < 10; i++) {
                transform.forward(signal);
            }
            
            // Measure
            long totalTime = 0;
            int runs = 100;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                MODWTResult result = transform.forward(signal);
                totalTime += System.nanoTime() - start;
            }
            
            double avgMicroseconds = (totalTime / runs) / 1000.0;
            System.out.printf("Size %4d: avg %.2f μs/transform%n", size, avgMicroseconds);
        }
        
        System.out.println("\nMODWT automatically selects the optimal implementation");
        System.out.println("based on signal size and system capabilities");
        System.out.println();
    }

    private static void demonstratePerformanceScaling() {
        System.out.println("4. Performance Scaling Analysis");
        System.out.println("-------------------------------");

        MODWTTransform transform = new MODWTTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        
        System.out.println("Signal Size | Time (ms) | Throughput (samples/ms)");
        System.out.println("------------|-----------|------------------------");
        
        for (int power = 6; power <= 14; power++) {
            int size = 1 << power; // 2^power
            double[] signal = createTestSignal(size);
            
            // Warm up
            for (int i = 0; i < 5; i++) {
                transform.forward(signal);
            }
            
            // Measure
            long startTime = System.nanoTime();
            int iterations = Math.max(1, 10000 / size); // Adjust iterations based on size
            
            for (int i = 0; i < iterations; i++) {
                MODWTResult result = transform.forward(signal);
            }
            
            long totalTime = System.nanoTime() - startTime;
            double msPerTransform = (totalTime / iterations) / 1_000_000.0;
            double throughput = size / msPerTransform;
            
            System.out.printf("%11d | %9.3f | %23.0f%n", size, msPerTransform, throughput);
        }
        
        System.out.println("\nNote: SIMD optimization typically shows better throughput");
        System.out.println("for larger signals where vectorization benefits outweigh overhead");
    }

    // Helper methods

    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                        0.5 * Math.sin(2 * Math.PI * i / 8.0);
        }
        return signal;
    }
}