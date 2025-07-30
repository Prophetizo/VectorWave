package ai.prophetizo.wavelet.demo;

import ai.prophetizo.wavelet.OptimizedTransformEngine;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine;
import ai.prophetizo.wavelet.internal.*;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;

import java.util.Random;

/**
 * Demonstrates the performance improvements from various optimizations.
 */
public class OptimizationDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Optimization Demo ===\n");

        // Check platform
        System.out.println("Platform Information:");
        System.out.println("- OS: " + System.getProperty("os.name"));
        System.out.println("- Architecture: " + System.getProperty("os.arch"));
        System.out.println("- Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        // Check available optimizations
        System.out.println("Available Optimizations:");
        System.out.println("- Vector API: " + VectorOps.isVectorizedOperationBeneficial(128));
        System.out.println("- Vector Info: " + VectorOps.getVectorInfo());
        System.out.println("- Apple Silicon: " + VectorOpsARM.isAppleSilicon());
        System.out.println("- Gather/Scatter: " + GatherScatterOps.isGatherScatterAvailable());
        System.out.println();

        // Run performance comparison
        int[] sizes = {128, 1024, 8192};
        Wavelet[] wavelets = {new Haar(), Daubechies.DB4, Symlet.SYM4};

        for (int size : sizes) {
            System.out.println("\n=== Signal Size: " + size + " ===");
            double[] signal = generateSignal(size);

            for (Wavelet wavelet : wavelets) {
                System.out.println("\nWavelet: " + wavelet.name());

                // Baseline scalar
                long scalarTime = timeTransform(signal, wavelet, false, false, false);
                System.out.printf("  Scalar:           %,d ns%n", scalarTime);

                // Vector operations
                long vectorTime = timeTransform(signal, wavelet, true, false, false);
                System.out.printf("  Vector:           %,d ns (%.2fx speedup)%n",
                        vectorTime, (double) scalarTime / vectorTime);

                // Memory pooled
                long pooledTime = timeTransformPooled(signal, wavelet);
                System.out.printf("  Memory Pooled:    %,d ns (%.2fx speedup)%n",
                        pooledTime, (double) scalarTime / pooledTime);

                // Specialized kernel (if available)
                if (hasSpecializedKernel(wavelet)) {
                    long specializedTime = timeTransformSpecialized(signal, wavelet);
                    System.out.printf("  Specialized:      %,d ns (%.2fx speedup)%n",
                            specializedTime, (double) scalarTime / specializedTime);
                }

                // Fully optimized
                long optimizedTime = timeTransformOptimized(signal, wavelet);
                System.out.printf("  Fully Optimized:  %,d ns (%.2fx speedup)%n",
                        optimizedTime, (double) scalarTime / optimizedTime);
            }
        }

        // Batch processing demo
        System.out.println("\n\n=== Batch Processing (16 signals of 1024 samples) ===");
        double[][] batch = new double[16][1024];
        for (int i = 0; i < 16; i++) {
            batch[i] = generateSignal(1024);
        }

        Wavelet wavelet = Daubechies.DB4;

        // Sequential
        long seqTime = timeBatchSequential(batch, wavelet);
        System.out.printf("Sequential:       %,d μs%n", seqTime / 1000);

        // Parallel
        long parTime = timeBatchParallel(batch, wavelet);
        System.out.printf("Parallel:         %,d μs (%.2fx speedup)%n",
                parTime / 1000, (double) seqTime / parTime);

        // SoA layout
        long soaTime = timeBatchSoA(batch, wavelet);
        System.out.printf("SoA Layout:       %,d μs (%.2fx speedup)%n",
                soaTime / 1000, (double) seqTime / soaTime);

        // Memory pool statistics
        System.out.println("\n" + AlignedMemoryPool.getStatistics());
    }

    private static long timeTransform(double[] signal, Wavelet wavelet,
                                      boolean useVector, boolean usePooled, boolean useSpecialized) {
        // Warm up
        for (int i = 0; i < 100; i++) {
            performTransform(signal, wavelet, useVector);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            performTransform(signal, wavelet, useVector);
        }
        return (System.nanoTime() - start) / 1000;
    }

    private static void performTransform(double[] signal, Wavelet wavelet, boolean useVector) {
        if (!(wavelet instanceof DiscreteWavelet dw)) return;

        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();

        if (useVector) {
            VectorOps.convolveAndDownsamplePeriodic(signal, lowPass, signal.length, lowPass.length);
            VectorOps.convolveAndDownsamplePeriodic(signal, highPass, signal.length, highPass.length);
        } else {
            ScalarOps.convolveAndDownsamplePeriodic(signal, lowPass, signal.length, lowPass.length);
            ScalarOps.convolveAndDownsamplePeriodic(signal, highPass, signal.length, highPass.length);
        }
    }

    private static long timeTransformPooled(double[] signal, Wavelet wavelet) {
        if (!(wavelet instanceof DiscreteWavelet dw)) return 0;

        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();

        // Warm up
        for (int i = 0; i < 100; i++) {
            VectorOpsPooled.convolveAndDownsamplePeriodicPooled(signal, lowPass, signal.length, lowPass.length);
            VectorOpsPooled.convolveAndDownsamplePeriodicPooled(signal, highPass, signal.length, highPass.length);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            VectorOpsPooled.convolveAndDownsamplePeriodicPooled(signal, lowPass, signal.length, lowPass.length);
            VectorOpsPooled.convolveAndDownsamplePeriodicPooled(signal, highPass, signal.length, highPass.length);
        }
        return (System.nanoTime() - start) / 1000;
    }

    private static long timeTransformSpecialized(double[] signal, Wavelet wavelet) {
        int halfLength = signal.length / 2;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];

        // Warm up
        for (int i = 0; i < 100; i++) {
            performSpecializedTransform(signal, wavelet, approx, detail);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            performSpecializedTransform(signal, wavelet, approx, detail);
        }
        return (System.nanoTime() - start) / 1000;
    }

    private static void performSpecializedTransform(double[] signal, Wavelet wavelet,
                                                    double[] approx, double[] detail) {
        String name = wavelet.name().toLowerCase();
        switch (name) {
            case "haar" -> {
                double[][] batch = {signal};
                double[][] approxBatch = {approx};
                double[][] detailBatch = {detail};
                SpecializedKernels.haarBatchOptimized(batch, approxBatch, detailBatch);
            }
            case "db4" -> SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
            case "sym4" -> SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
        }
    }

    private static long timeTransformOptimized(double[] signal, Wavelet wavelet) {
        OptimizedTransformEngine engine = new OptimizedTransformEngine();

        // Warm up
        for (int i = 0; i < 100; i++) {
            engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        }
        return (System.nanoTime() - start) / 1000;
    }

    private static long timeBatchSequential(double[][] batch, Wavelet wavelet) {
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);

        // Warm up
        for (int i = 0; i < 10; i++) {
            for (double[] signal : batch) {
                transform.forward(signal);
            }
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (double[] signal : batch) {
                transform.forward(signal);
            }
        }
        return (System.nanoTime() - start) / 100;
    }

    private static long timeBatchParallel(double[][] batch, Wavelet wavelet) {
        try (ParallelWaveletEngine engine = new ParallelWaveletEngine()) {
            // Warm up
            for (int i = 0; i < 10; i++) {
                engine.transformBatch(batch, wavelet, BoundaryMode.PERIODIC);
            }

            // Measure
            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                engine.transformBatch(batch, wavelet, BoundaryMode.PERIODIC);
            }
            return (System.nanoTime() - start) / 100;
        }
    }

    private static long timeBatchSoA(double[][] batch, Wavelet wavelet) {
        if (!(wavelet instanceof DiscreteWavelet dw)) return 0;

        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();

        int numSignals = batch.length;
        int signalLength = batch[0].length;
        int halfLength = signalLength / 2;

        double[] soa = SoATransform.convertAoSToSoA(batch);
        double[] soaApprox = new double[numSignals * halfLength];
        double[] soaDetail = new double[numSignals * halfLength];

        // Warm up
        for (int i = 0; i < 10; i++) {
            SoATransform.transformSoA(soa, soaApprox, soaDetail, lowPass, highPass,
                    numSignals, signalLength, lowPass.length);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            SoATransform.transformSoA(soa, soaApprox, soaDetail, lowPass, highPass,
                    numSignals, signalLength, lowPass.length);
        }
        return (System.nanoTime() - start) / 100;
    }

    private static boolean hasSpecializedKernel(Wavelet wavelet) {
        String name = wavelet.name().toLowerCase();
        return name.equals("haar") || name.equals("db4") || name.equals("sym4");
    }

    private static double[] generateSignal(int size) {
        Random rand = new Random(42);
        double[] signal = new double[size];

        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                    0.5 * Math.cos(8 * Math.PI * i / 32.0) +
                    0.25 * Math.sin(16 * Math.PI * i / 32.0) +
                    0.1 * rand.nextGaussian();
        }

        return signal;
    }
}