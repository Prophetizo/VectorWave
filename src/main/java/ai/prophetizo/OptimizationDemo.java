package ai.prophetizo;

import ai.prophetizo.wavelet.ImmutableTransformResult;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;

/**
 * Demonstrates the performance improvements from small signal optimizations.
 */
public class OptimizationDemo {

    public static void main(String[] args) {
        System.out.println("VectorWave Performance Demo");
        System.out.println("===========================\n");
        System.out.println("Note: All optimizations are now integrated into the standard implementation\n");

        // Test different signal sizes
        int[] signalSizes = {256, 512, 1024};
        String[] waveletNames = {"haar", "db2", "db4"};

        for (int size : signalSizes) {
            System.out.println("Signal size: " + size + " samples");
            System.out.println("-".repeat(40));

            // Create test signal (financial time series simulation)
            double[] signal = createFinancialSignal(size);

            for (String waveletName : waveletNames) {
                Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);

                // Time implementation (now includes optimizations)
                WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
                long transformTime = timeTransform(transform, signal, 1000);

                System.out.printf("%-10s Transform time: %6d ns\n",
                        waveletName.toUpperCase(), transformTime);
            }
            System.out.println();
        }

        // Demonstrate batch processing
        demonstrateBatchProcessing();

        // Demonstrate memory efficiency
        demonstrateMemoryEfficiency();
    }

    private static double[] createFinancialSignal(int length) {
        double[] signal = new double[length];
        double price = 100.0;
        double volatility = 0.02;

        for (int i = 0; i < length; i++) {
            // Random walk with drift
            double return_ = 0.0001 + volatility * (Math.random() - 0.5);
            price *= (1 + return_);

            // Add some market microstructure noise
            double noise = 0.01 * (Math.random() - 0.5);
            signal[i] = Math.log(price) + noise; // Log returns
        }

        return signal;
    }

    private static long timeTransform(WaveletTransform transform, double[] signal, int iterations) {
        // Warmup
        for (int i = 0; i < 100; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }

        // Timing
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
        }
        long end = System.nanoTime();

        return (end - start) / iterations;
    }

    private static void demonstrateBatchProcessing() {
        System.out.println("Batch Processing Demo");
        System.out.println("-".repeat(40));

        Wavelet wavelet = WaveletRegistry.getWavelet("db2");
        int batchSize = 100;
        int signalLength = 512;

        // Create batch of signals
        double[][] signals = new double[batchSize][signalLength];
        for (int i = 0; i < batchSize; i++) {
            signals[i] = createFinancialSignal(signalLength);
        }

        // Time batch processing
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        long start = System.nanoTime();
        for (double[] signal : signals) {
            transform.forward(signal);
        }
        long totalTime = System.nanoTime() - start;

        System.out.printf("Batch processing time: %d µs\n", totalTime / 1000);
        System.out.printf("Average per signal: %d ns\n\n", totalTime / batchSize);
    }

    private static void demonstrateMemoryEfficiency() {
        System.out.println("Memory Efficiency Demo");
        System.out.println("-".repeat(40));

        double[] approx = new double[128];
        double[] detail = new double[128];
        for (int i = 0; i < 128; i++) {
            approx[i] = i;
            detail[i] = -i;
        }

        // Standard TransformResult (creates defensive copies)
        long standardMemory = estimateMemoryUsage(() -> {
            // Create a transform result using the standard transform
            WaveletTransform tempTransform = new WaveletTransform(
                    WaveletRegistry.getWavelet("haar"), BoundaryMode.PERIODIC);
            double[] signal = new double[256];
            System.arraycopy(approx, 0, signal, 0, 128);
            System.arraycopy(detail, 0, signal, 128, 128);
            TransformResult result = tempTransform.forward(signal);
            for (int i = 0; i < 1000; i++) {
                double[] a = result.approximationCoeffs();
                double[] d = result.detailCoeffs();
            }
        });

        // Immutable TransformResult (returns views)
        long immutableMemory = estimateMemoryUsage(() -> {
            ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
            for (int i = 0; i < 1000; i++) {
                var a = result.approximationCoeffsView();
                var d = result.detailCoeffsView();
            }
        });

        System.out.printf("Standard result memory allocations: ~%d KB\n", standardMemory / 1024);
        System.out.printf("Immutable result memory allocations: ~%d KB\n", immutableMemory / 1024);
        System.out.printf("Memory reduction: %.1f%%\n",
                100.0 * (1 - (double) immutableMemory / standardMemory));
    }

    private static long estimateMemoryUsage(Runnable task) {
        System.gc();
        long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        task.run();
        System.gc();
        long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return Math.max(0, after - before);
    }
}