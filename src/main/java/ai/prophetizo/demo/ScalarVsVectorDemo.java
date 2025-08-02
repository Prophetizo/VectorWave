package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Symlet;
import ai.prophetizo.wavelet.config.TransformConfig;

import java.util.Arrays;

/**
 * Demonstrates how to force scalar vs vector (SIMD) optimization paths.
 *
 * <p>This demo shows:
 * <ul>
 *   <li>How to explicitly force scalar operations</li>
 *   <li>How to explicitly force SIMD/vector operations</li>
 *   <li>How to let the system auto-detect the best path</li>
 *   <li>Performance comparison between different paths</li>
 * </ul>
 */
public class ScalarVsVectorDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Scalar vs Vector Optimization Demo ===\n");

        // Demo 1: Force scalar operations
        demonstrateScalarForced();

        // Demo 2: Force SIMD/vector operations
        demonstrateSIMDForced();

        // Demo 3: Auto-detection (default)
        demonstrateAutoDetection();

        // Demo 4: Performance comparison
        demonstratePerformanceComparison();

        // Demo 5: Configuration validation
        demonstrateConfigurationValidation();
    }

    private static void demonstrateScalarForced() {
        System.out.println("1. Forcing Scalar Operations");
        System.out.println("----------------------------");

        // Create configuration that forces scalar operations
        TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .boundaryMode(BoundaryMode.PERIODIC)
                .build();

        System.out.println("Config: " + scalarConfig);

        // Create transform with scalar-only config
        WaveletTransform transform = new WaveletTransform(
                Daubechies.DB4,
                BoundaryMode.PERIODIC,
                scalarConfig
        );

        // Test with a signal
        double[] signal = createTestSignal(128);
        TransformResult result = transform.forward(signal);

        System.out.println("Transform completed using scalar operations");
        System.out.println("Approximation coeffs: " + result.approximationCoeffs().length);
        System.out.println("Detail coeffs: " + result.detailCoeffs().length);
        System.out.println();
    }

    private static void demonstrateSIMDForced() {
        System.out.println("2. Forcing SIMD/Vector Operations");
        System.out.println("----------------------------------");

        // Create configuration that forces SIMD operations
        TransformConfig simdConfig = TransformConfig.builder()
                .forceVector(true)
                .boundaryMode(BoundaryMode.ZERO_PADDING)
                .build();

        System.out.println("Config: " + simdConfig);

        // Create transform with SIMD-forced config
        WaveletTransform transform = new WaveletTransform(
                new Haar(),
                BoundaryMode.ZERO_PADDING,
                simdConfig
        );

        // Test with a signal
        double[] signal = createTestSignal(256);
        TransformResult result = transform.forward(signal);

        System.out.println("Transform completed using SIMD operations (if available)");
        System.out.println("Note: If SIMD is not available on this platform, it will fall back to scalar");
        System.out.println("Approximation coeffs: " + result.approximationCoeffs().length);
        System.out.println("Detail coeffs: " + result.detailCoeffs().length);
        System.out.println();
    }

    private static void demonstrateAutoDetection() {
        System.out.println("3. Auto-Detection (Default Behavior)");
        System.out.println("------------------------------------");

        // Create default configuration (auto-detects best path)
        TransformConfig autoConfig = TransformConfig.defaultConfig();

        System.out.println("Config: " + autoConfig);
        System.out.println("This configuration will automatically choose the best optimization path");

        // Method 1: Using config with constructor
        WaveletTransform transform1 = new WaveletTransform(
                Symlet.SYM4,
                BoundaryMode.PERIODIC,
                autoConfig
        );

        // Method 2: Using default constructor (same as auto-detect)
        WaveletTransform transform2 = new WaveletTransform(
                Symlet.SYM4,
                BoundaryMode.PERIODIC
        );

        double[] signal = createTestSignal(512);
        TransformResult result1 = transform1.forward(signal);
        TransformResult result2 = transform2.forward(signal);

        System.out.println("Both transforms use auto-detection");
        System.out.println("Results are identical: " +
                Arrays.equals(result1.approximationCoeffs(), result2.approximationCoeffs()));
        System.out.println();
    }

    private static void demonstratePerformanceComparison() {
        System.out.println("4. Performance Comparison");
        System.out.println("-------------------------");

        int signalSize = 1024;
        double[] signal = createTestSignal(signalSize);
        int iterations = 1000;

        // Scalar configuration
        TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();

        // SIMD configuration
        TransformConfig simdConfig = TransformConfig.builder()
                .forceVector(true)
                .build();

        // Auto configuration
        TransformConfig autoConfig = TransformConfig.defaultConfig();

        // Create transforms
        WaveletTransform scalarTransform = new WaveletTransform(
                Daubechies.DB2, BoundaryMode.PERIODIC, scalarConfig);
        WaveletTransform simdTransform = new WaveletTransform(
                Daubechies.DB2, BoundaryMode.PERIODIC, simdConfig);
        WaveletTransform autoTransform = new WaveletTransform(
                Daubechies.DB2, BoundaryMode.PERIODIC, autoConfig);

        // Warm up
        for (int i = 0; i < 100; i++) {
            scalarTransform.forward(signal);
            simdTransform.forward(signal);
            autoTransform.forward(signal);
        }

        // Benchmark scalar
        long scalarStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            scalarTransform.forward(signal);
        }
        long scalarTime = System.nanoTime() - scalarStart;

        // Benchmark SIMD
        long simdStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            simdTransform.forward(signal);
        }
        long simdTime = System.nanoTime() - simdStart;

        // Benchmark auto
        long autoStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            autoTransform.forward(signal);
        }
        long autoTime = System.nanoTime() - autoStart;

        System.out.printf("Signal size: %d samples\n", signalSize);
        System.out.printf("Iterations: %d\n\n", iterations);
        System.out.printf("Scalar forced: %.2f ms (%.2f µs/transform)\n",
                scalarTime / 1e6, scalarTime / (iterations * 1e3));
        System.out.printf("SIMD forced:   %.2f ms (%.2f µs/transform)\n",
                simdTime / 1e6, simdTime / (iterations * 1e3));
        System.out.printf("Auto-detect:   %.2f ms (%.2f µs/transform)\n",
                autoTime / 1e6, autoTime / (iterations * 1e3));

        System.out.println("\nNote: Performance will vary based on:");
        System.out.println("- CPU architecture and SIMD support");
        System.out.println("- Signal size and wavelet type");
        System.out.println("- JVM warmup and optimization");
        System.out.println();
    }

    private static void demonstrateConfigurationValidation() {
        System.out.println("5. Configuration Validation");
        System.out.println("---------------------------");

        try {
            // This will throw an exception - can't force both scalar and SIMD
            TransformConfig invalidConfig = TransformConfig.builder()
                    .forceScalar(true)
                    .forceVector(true)
                    .build();
        } catch (Exception e) {
            System.out.println("Expected error when forcing both scalar and SIMD:");
            System.out.println("  " + e.getMessage());
        }

        // Valid configurations
        System.out.println("\nValid configuration examples:");

        // Example 1: Force scalar for debugging
        TransformConfig debugConfig = TransformConfig.builder()
                .forceScalar(true)
                .maxDecompositionLevels(5)
                .boundaryMode(BoundaryMode.PERIODIC)
                .build();
        System.out.println("Debug config: " + debugConfig);

        // Example 2: Force SIMD for performance testing
        TransformConfig perfConfig = TransformConfig.builder()
                .forceVector(true)
                .boundaryMode(BoundaryMode.ZERO_PADDING)
                .build();
        System.out.println("Performance config: " + perfConfig);

        // Example 3: Custom configuration
        TransformConfig customConfig = TransformConfig.builder()
                .boundaryMode(BoundaryMode.SYMMETRIC)
                .maxDecompositionLevels(10)
                .build();
        System.out.println("Custom config: " + customConfig);
        System.out.println();

        // Show how to check configuration
        System.out.println("Checking configuration properties:");
        System.out.println("Is scalar forced? " + debugConfig.isForceScalar());
        System.out.println("Is Vector API forced? " + perfConfig.isForceVector());
        System.out.println("Boundary mode: " + customConfig.getBoundaryMode());
        System.out.println("Max decomposition levels: " + customConfig.getMaxDecompositionLevels());
    }

    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                    0.5 * Math.cos(2 * Math.PI * i / 16.0);
        }
        return signal;
    }
}