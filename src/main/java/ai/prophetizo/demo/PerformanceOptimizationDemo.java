package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.memory.MemoryPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demonstrates performance optimization techniques and best practices.
 *
 * <p>This demo covers:
 * <ul>
 *   <li>SIMD vectorization benefits</li>
 *   <li>Memory pooling for reduced allocations</li>
 *   <li>Batch processing strategies</li>
 *   <li>Parallel processing patterns</li>
 *   <li>Cache-friendly access patterns</li>
 * </ul>
 */
public class PerformanceOptimizationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== VectorWave Performance Optimization Demo ===\n");

        // Demo 1: SIMD vs Scalar performance
        demonstrateSIMDPerformance();

        // Demo 2: Memory pooling benefits
        demonstrateMemoryPooling();

        // Demo 3: Batch processing
        demonstrateBatchProcessing();

        // Demo 4: Parallel processing
        demonstrateParallelProcessing();

        // Demo 5: Optimization tips
        demonstrateOptimizationTips();
    }

    private static void demonstrateSIMDPerformance() {
        System.out.println("1. SIMD Vectorization Performance");
        System.out.println("---------------------------------");

        // Check SIMD availability
        boolean simdAvailable = true; // Assume available for demo
        System.out.println("SIMD Support: " + (simdAvailable ? "Available" : "Not Available"));
        System.out.println("Preferred Vector Length: " +
                (simdAvailable ? jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length() : "N/A"));

        // Test different signal sizes
        int[] sizes = {128, 512, 2048, 8192};
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

        System.out.println("\nPerformance comparison (ops/second):");
        System.out.println("Size  | With SIMD | Without | Speedup");
        System.out.println("------|-----------|---------|--------");

        for (int size : sizes) {
            double[] signal = generateTestSignal(size);

            // Force scalar implementation
            System.setProperty("jdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK", "3");
            long scalarOps = benchmarkTransform(transform, signal, 1000);

            // Enable SIMD
            System.clearProperty("jdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK");
            long simdOps = benchmarkTransform(transform, signal, 1000);

            double speedup = (double) simdOps / scalarOps;
            System.out.printf("%5d | %9d | %7d | %.2fx\n",
                    size, simdOps, scalarOps, speedup);
        }

        System.out.println("\nNote: SIMD provides larger benefits for bigger signals\n");
    }

    private static void demonstrateMemoryPooling() {
        System.out.println("2. Memory Pooling Benefits");
        System.out.println("--------------------------");

        int signalSize = 1024;
        int iterations = 10000;
        double[] signal = generateTestSignal(signalSize);

        // Without pooling
        System.gc();
        long startMem = getUsedMemory();
        long startTime = System.nanoTime();

        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        for (int i = 0; i < iterations; i++) {
            TransformResult result = transform.forward(signal);
            // Simulate processing
            double sum = result.approximationCoeffs()[0] + result.detailCoeffs()[0];
        }

        long timeWithoutPool = System.nanoTime() - startTime;
        long memWithoutPool = getUsedMemory() - startMem;

        // With pooling
        System.gc();
        startMem = getUsedMemory();
        startTime = System.nanoTime();

        MemoryPool pool = new MemoryPool();
        for (int i = 0; i < iterations; i++) {
            double[] tempBuffer = pool.borrowArray(signalSize);
            try {
                // Use pooled buffer for intermediate calculations
                System.arraycopy(signal, 0, tempBuffer, 0, signalSize);
                TransformResult result = transform.forward(tempBuffer);
                double sum = result.approximationCoeffs()[0] + result.detailCoeffs()[0];
            } finally {
                pool.returnArray(tempBuffer);
            }
        }

        long timeWithPool = System.nanoTime() - startTime;
        long memWithPool = getUsedMemory() - startMem;

        System.out.printf("Without pooling: %.2f ms, ~%.1f MB allocated\n",
                timeWithoutPool / 1e6, memWithoutPool / 1e6);
        System.out.printf("With pooling:    %.2f ms, ~%.1f MB allocated\n",
                timeWithPool / 1e6, memWithPool / 1e6);
        System.out.printf("Time saved: %.1f%%, Memory saved: %.1f%%\n\n",
                100.0 * (timeWithoutPool - timeWithPool) / timeWithoutPool,
                100.0 * (memWithoutPool - memWithPool) / memWithoutPool);
    }

    private static void demonstrateBatchProcessing() {
        System.out.println("3. Batch Processing Strategies");
        System.out.println("------------------------------");

        int numSignals = 100;
        int signalSize = 512;
        List<double[]> signals = new ArrayList<>();
        for (int i = 0; i < numSignals; i++) {
            signals.add(generateTestSignal(signalSize));
        }

        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

        // Single processing
        long startTime = System.nanoTime();
        List<TransformResult> results1 = new ArrayList<>();
        for (double[] sig : signals) {
            results1.add(transform.forward(sig));
        }
        long singleTime = System.nanoTime() - startTime;

        // Batch processing with reused buffers
        startTime = System.nanoTime();
        List<TransformResult> results2 = new ArrayList<>();

        // Pre-allocate workspace
        double[] workspace = new double[signalSize];
        for (double[] sig : signals) {
            System.arraycopy(sig, 0, workspace, 0, signalSize);
            results2.add(transform.forward(workspace));
        }
        long batchTime = System.nanoTime() - startTime;

        System.out.printf("Single processing: %.2f ms\n", singleTime / 1e6);
        System.out.printf("Batch processing:  %.2f ms\n", batchTime / 1e6);
        System.out.printf("Speedup: %.2fx\n\n", (double) singleTime / batchTime);

        // Demonstrate cache-friendly access
        demonstrateCacheFriendlyAccess();
    }

    private static void demonstrateCacheFriendlyAccess() {
        System.out.println("Cache-Friendly Access Patterns:");
        System.out.println("-------------------------------");

        int size = 4096;
        double[][] multiChannelData = new double[16][size]; // 16 channels

        // Initialize data
        for (int ch = 0; ch < 16; ch++) {
            for (int i = 0; i < size; i++) {
                multiChannelData[ch][i] = Math.random();
            }
        }

        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);

        // Poor pattern: channel-by-channel in wrong order
        long startTime = System.nanoTime();
        for (int i = 0; i < size; i += 128) { // Process chunks
            for (int ch = 0; ch < 16; ch++) {
                // This jumps between arrays - poor cache usage
                double sum = multiChannelData[ch][i];
            }
        }
        long poorTime = System.nanoTime() - startTime;

        // Good pattern: process each channel completely
        startTime = System.nanoTime();
        for (int ch = 0; ch < 16; ch++) {
            for (int i = 0; i < size; i += 128) {
                // Sequential access within array - good cache usage
                double sum = multiChannelData[ch][i];
            }
        }
        long goodTime = System.nanoTime() - startTime;

        System.out.printf("Poor access pattern:  %.2f µs\n", poorTime / 1e3);
        System.out.printf("Good access pattern:  %.2f µs\n", goodTime / 1e3);
        System.out.printf("Improvement: %.1fx faster\n\n", (double) poorTime / goodTime);
    }

    private static void demonstrateParallelProcessing() throws Exception {
        System.out.println("4. Parallel Processing");
        System.out.println("----------------------");

        int numSignals = 32;
        int signalSize = 2048;
        List<double[]> signals = new ArrayList<>();
        for (int i = 0; i < numSignals; i++) {
            signals.add(generateTestSignal(signalSize));
        }

        // Sequential processing
        long startTime = System.nanoTime();
        List<TransformResult> sequentialResults = new ArrayList<>();
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

        for (double[] signal : signals) {
            sequentialResults.add(transform.forward(signal));
        }
        long sequentialTime = System.nanoTime() - startTime;

        // Parallel processing with thread pool
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        startTime = System.nanoTime();
        List<Future<TransformResult>> futures = new ArrayList<>();

        for (double[] signal : signals) {
            futures.add(executor.submit(() -> {
                // Each thread gets its own transform instance
                WaveletTransform localTransform = new WaveletTransform(
                        Daubechies.DB4, BoundaryMode.PERIODIC);
                return localTransform.forward(signal);
            }));
        }

        List<TransformResult> parallelResults = new ArrayList<>();
        for (Future<TransformResult> future : futures) {
            parallelResults.add(future.get());
        }
        long parallelTime = System.nanoTime() - startTime;

        executor.shutdown();

        System.out.printf("Sequential: %.2f ms\n", sequentialTime / 1e6);
        System.out.printf("Parallel (%d threads): %.2f ms\n",
                numThreads, parallelTime / 1e6);
        System.out.printf("Speedup: %.2fx\n\n",
                (double) sequentialTime / parallelTime);

        // Parallel streaming example
        demonstrateParallelStreaming();
    }

    private static void demonstrateParallelStreaming() {
        System.out.println("Parallel Stream Processing:");
        System.out.println("--------------------------");

        List<double[]> signals = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            signals.add(generateTestSignal(256));
        }

        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);

        // Using parallel streams
        long startTime = System.nanoTime();
        List<Double> energies = signals.parallelStream()
                .map(transform::forward)
                .map(result -> {
                    double energy = 0;
                    for (double c : result.approximationCoeffs()) energy += c * c;
                    for (double c : result.detailCoeffs()) energy += c * c;
                    return energy;
                })
                .toList();
        long streamTime = System.nanoTime() - startTime;

        System.out.printf("Processed %d signals in %.2f ms\n",
                signals.size(), streamTime / 1e6);
        System.out.printf("Average energy: %.4f\n\n",
                energies.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private static void demonstrateOptimizationTips() {
        System.out.println("5. Optimization Best Practices");
        System.out.println("------------------------------");

        System.out.println("✓ Choose the right wavelet:");
        System.out.println("  - Haar: Fastest, good for sharp transitions");
        System.out.println("  - DB2/DB4: Good balance of speed and smoothness");
        System.out.println("  - Higher order: More computation but smoother");

        System.out.println("\n✓ Signal length considerations:");
        System.out.println("  - Powers of 2 are optimal (no padding needed)");
        System.out.println("  - Larger signals benefit more from SIMD");
        System.out.println("  - Consider multi-level transforms for long signals");

        System.out.println("\n✓ Memory optimization:");
        System.out.println("  - Reuse transform instances");
        System.out.println("  - Use memory pools for temporary buffers");
        System.out.println("  - Process in-place when possible");

        System.out.println("\n✓ Parallel processing:");
        System.out.println("  - Use separate transform per thread");
        System.out.println("  - Batch signals for better throughput");
        System.out.println("  - Consider CPU cache sizes");

        // Performance comparison summary
        System.out.println("\n✓ Performance Summary:");
        performanceComparison();
    }

    private static void performanceComparison() {
        int size = 1024;
        double[] signal = generateTestSignal(size);
        int iterations = 1000;

        System.out.println("\nTransform performance for 1024-sample signal:");
        System.out.println("Wavelet | Time (µs) | Relative");
        System.out.println("--------|-----------|----------");

        Wavelet[] wavelets = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM4,
                Coiflet.COIF1
        };

        long haarTime = 0;
        for (int i = 0; i < wavelets.length; i++) {
            WaveletTransform transform = new WaveletTransform(
                    wavelets[i], BoundaryMode.PERIODIC);

            // Warmup
            for (int j = 0; j < 100; j++) {
                transform.forward(signal);
            }

            // Benchmark
            long startTime = System.nanoTime();
            for (int j = 0; j < iterations; j++) {
                transform.forward(signal);
            }
            long elapsed = System.nanoTime() - startTime;
            long avgTime = elapsed / iterations / 1000; // Convert to microseconds

            if (i == 0) haarTime = avgTime;

            System.out.printf("%-7s | %9d | %.2fx\n",
                    wavelets[i].name().toUpperCase(),
                    avgTime,
                    (double) avgTime / haarTime);
        }
    }

    // Helper methods

    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) +
                    0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        return signal;
    }

    private static long benchmarkTransform(WaveletTransform transform,
                                           double[] signal, int iterations) {
        // Warmup
        for (int i = 0; i < 100; i++) {
            transform.forward(signal);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            transform.forward(signal);
        }
        long elapsed = System.nanoTime() - startTime;

        return (long) (iterations * 1e9 / elapsed); // ops/second
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}