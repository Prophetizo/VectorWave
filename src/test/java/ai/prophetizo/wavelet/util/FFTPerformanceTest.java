package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

/**
 * Performance comparison between vector and scalar FFT implementations.
 * This test is disabled by default to avoid running during regular test cycles.
 */
@Disabled("Performance test - run manually")
public class FFTPerformanceTest {
    
    @Test
    @DisplayName("Compare performance of vector vs scalar FFT")
    void compareFFTPerformance() {
        int[] sizes = {64, 128, 256, 512, 1024, 2048, 4096};
        int warmupIterations = 1000;
        int testIterations = 10000;
        
        System.out.println("FFT Performance Comparison (Vector vs Scalar)");
        System.out.println("=============================================");
        System.out.printf("%-10s %-15s %-15s %-10s%n", "Size", "Vector (ms)", "Scalar (ms)", "Speedup");
        System.out.println("---------------------------------------------");
        
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        
        for (int size : sizes) {
            // Create test data
            double[] data = new double[2 * size];
            for (int i = 0; i < size; i++) {
                data[2 * i] = Math.random();
                data[2 * i + 1] = Math.random();
            }
            
            // Warmup
            for (int i = 0; i < warmupIterations; i++) {
                double[] temp = data.clone();
                OptimizedFFT.fftOptimized(temp, size, false);
                OptimizedFFT.fftOptimized(temp, size, false, scalarConfig);
            }
            
            // Test vector performance
            long vectorStart = System.nanoTime();
            for (int i = 0; i < testIterations; i++) {
                double[] temp = data.clone();
                OptimizedFFT.fftOptimized(temp, size, false);
            }
            long vectorEnd = System.nanoTime();
            double vectorTime = (vectorEnd - vectorStart) / (testIterations * 1_000_000.0);
            
            // Test scalar performance
            long scalarStart = System.nanoTime();
            for (int i = 0; i < testIterations; i++) {
                double[] temp = data.clone();
                OptimizedFFT.fftOptimized(temp, size, false, scalarConfig);
            }
            long scalarEnd = System.nanoTime();
            double scalarTime = (scalarEnd - scalarStart) / (testIterations * 1_000_000.0);
            
            double speedup = scalarTime / vectorTime;
            
            System.out.printf("%-10d %-15.6f %-15.6f %-10.2fx%n", 
                size, vectorTime, scalarTime, speedup);
        }
        
        System.out.println("\nNote: Results may vary based on CPU, JVM version, and system load.");
        System.out.println("Vector API availability: " + OptimizedFFT.getVectorApiInfo());
    }
}