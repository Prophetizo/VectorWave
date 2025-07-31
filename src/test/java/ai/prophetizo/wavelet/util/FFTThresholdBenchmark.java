package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.test.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import java.util.Random;

/**
 * Benchmark to determine the optimal threshold for switching between
 * scalar and vector FFT implementations.
 */
@Disabled("Benchmark - run manually to determine optimal threshold")
public class FFTThresholdBenchmark {
    
    @Test
    @DisplayName("Determine optimal FFT vector/scalar threshold")
    void findOptimalThreshold() {
        // Test a wide range of sizes to find crossover point
        int[] sizes = {
            4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
        };
        
        int warmupIterations = 100;
        int testIterations = 1000;
        
        System.out.println("FFT Threshold Analysis");
        System.out.println("======================");
        System.out.println("Finding optimal threshold for vector vs scalar FFT");
        System.out.println();
        System.out.printf("%-10s %-15s %-15s %-10s %-15s%n", 
            "Size", "Vector (μs)", "Scalar (μs)", "Speedup", "Recommendation");
        System.out.println("----------------------------------------------------------------------");
        
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        
        TransformConfig vectorConfig = TransformConfig.builder()
            .forceVector(true)
            .build();
        
        int recommendedThreshold = Integer.MAX_VALUE;
        boolean foundCrossover = false;
        
        // Use seeded Random for consistent benchmark results
        Random random = new Random(TestConstants.TEST_SEED);
        
        for (int size : sizes) {
            // Create test data with deterministic random values
            double[] data = new double[2 * size];
            for (int i = 0; i < size; i++) {
                data[2 * i] = random.nextDouble() - 0.5;
                data[2 * i + 1] = random.nextDouble() - 0.5;
            }
            
            // Warmup both paths
            for (int i = 0; i < warmupIterations; i++) {
                double[] temp1 = data.clone();
                double[] temp2 = data.clone();
                try {
                    OptimizedFFT.fftOptimized(temp1, size, false, vectorConfig);
                } catch (Exception e) {
                    // Vector path might fail, use default
                    OptimizedFFT.fftOptimized(temp1, size, false);
                }
                OptimizedFFT.fftOptimized(temp2, size, false, scalarConfig);
            }
            
            // Test vector performance
            long vectorStart = System.nanoTime();
            boolean vectorFailed = false;
            for (int i = 0; i < testIterations; i++) {
                double[] temp = data.clone();
                try {
                    OptimizedFFT.fftOptimized(temp, size, false, vectorConfig);
                } catch (Exception e) {
                    vectorFailed = true;
                    OptimizedFFT.fftOptimized(temp, size, false);
                }
            }
            long vectorEnd = System.nanoTime();
            double vectorTime = (vectorEnd - vectorStart) / (testIterations * 1000.0); // microseconds
            
            // Test scalar performance
            long scalarStart = System.nanoTime();
            for (int i = 0; i < testIterations; i++) {
                double[] temp = data.clone();
                OptimizedFFT.fftOptimized(temp, size, false, scalarConfig);
            }
            long scalarEnd = System.nanoTime();
            double scalarTime = (scalarEnd - scalarStart) / (testIterations * 1000.0); // microseconds
            
            double speedup = scalarTime / vectorTime;
            String recommendation = speedup > 1.0 ? "Use Vector" : "Use Scalar";
            
            // Track when vector becomes beneficial
            if (!foundCrossover && speedup > 1.0) {
                recommendedThreshold = size;
                foundCrossover = true;
            }
            
            System.out.printf("%-10d %-15.3f %-15.3f %-10.2fx %-15s%s%n", 
                size, vectorTime, scalarTime, speedup, recommendation,
                vectorFailed ? " (forced scalar)" : "");
        }
        
        System.out.println();
        if (foundCrossover) {
            System.out.println("RECOMMENDED THRESHOLD: " + recommendedThreshold);
            System.out.println("Use scalar for sizes < " + recommendedThreshold + ", vector for sizes >= " + recommendedThreshold);
        } else {
            System.out.println("WARNING: Vector implementation is slower for all tested sizes!");
            System.out.println("Consider disabling vectorization or investigating the implementation.");
        }
        
        System.out.println();
        System.out.println("System info: " + OptimizedFFT.getVectorApiInfo());
        System.out.println("Note: Results vary by CPU architecture, JVM version, and system load.");
    }
}