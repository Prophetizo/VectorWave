package ai.prophetizo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserStrategy;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the VectorWave library with various wavelet types.
 */
public class Main {
    public static void main(String[] args) {
        // Test signal
        double[] signal = {10, 12, 15, 18, 20, 17, 14, 11};
        System.out.println("VectorWave - Fast Wavelet Transform Demo");
        System.out.println("========================================");
        System.out.println("Original Signal: " + Arrays.toString(signal));
        System.out.println();

        // Create factory for building transforms
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC);

        // Demonstrate different wavelet types
        demonstrateOrthogonalWavelets(signal, factory);
        demonstrateBiorthogonalWavelets(signal, factory);
        demonstrateContinuousWavelets(signal, factory);
        demonstrateWaveletRegistry();
      
        // Demonstrate multi-level decomposition
        demonstrateMultiLevelDecomposition();

        // Demonstrate streaming denoiser
        demonstrateStreamingDenoiser();
        
        // Point users to comprehensive error handling examples
        System.out.println("\n\nFor comprehensive error handling examples, run:");
        System.out.println("java -cp target/classes ai.prophetizo.ErrorHandlingDemo");
    }

    private static void demonstrateOrthogonalWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("ORTHOGONAL WAVELETS");
        System.out.println("===================");

        // Haar wavelet
        System.out.println("\n1. Haar Wavelet:");
        performTransform(signal, factory.create(new Haar()));

        // Daubechies wavelets
        System.out.println("\n2. Daubechies DB2:");
        performTransform(signal, factory.create(Daubechies.DB2));

        System.out.println("\n3. Daubechies DB4:");
        performTransform(signal, factory.create(Daubechies.DB4));

        // Symlet wavelet
        System.out.println("\n4. Symlet SYM2:");
        performTransform(signal, factory.create(Symlet.SYM2));

        // Coiflet wavelet
        System.out.println("\n5. Coiflet COIF1:");
        performTransform(signal, factory.create(Coiflet.COIF1));
    }

    private static void demonstrateBiorthogonalWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("\n\nBIORTHOGONAL WAVELETS");
        System.out.println("=====================");

        System.out.println("\n1. Biorthogonal Spline BIOR1.3:");
        performTransform(signal, factory.create(BiorthogonalSpline.BIOR1_3));
    }

    private static void demonstrateContinuousWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("\n\nCONTINUOUS WAVELETS (Discretized)");
        System.out.println("==================================");

        System.out.println("\n1. Morlet Wavelet:");
        performTransform(signal, factory.create(new MorletWavelet()));
    }

    private static void performTransform(double[] signal, WaveletTransform transform) {
        try {
            // Forward transform
            TransformResult result = transform.forward(signal);
            System.out.println("   Approximation: " + Arrays.toString(result.approximationCoeffs()));
            System.out.println("   Detail:        " + Arrays.toString(result.detailCoeffs()));

            // Inverse transform
            double[] reconstructed = transform.inverse(result);

            // Calculate reconstruction error
            double maxError = 0;
            for (int i = 0; i < signal.length; i++) {
                maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
            }
            System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
    }

    private static void demonstrateWaveletRegistry() {
        System.out.println("\n\nWAVELET REGISTRY");
        System.out.println("================");

        System.out.println("\nAvailable wavelets: " + WaveletRegistry.getAvailableWavelets());
        System.out.println("\nOrthogonal wavelets: " + WaveletRegistry.getOrthogonalWavelets());
        System.out.println("Biorthogonal wavelets: " + WaveletRegistry.getBiorthogonalWavelets());
        System.out.println("Continuous wavelets: " + WaveletRegistry.getContinuousWavelets());

        System.out.println("\nGetting wavelet by name:");
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        System.out.println("   db4: " + db4.description());
    }

    private static void demonstrateMultiLevelDecomposition() {
        System.out.println("\n\nMULTI-LEVEL DECOMPOSITION");
        System.out.println("=========================");

        // Create a longer signal for multi-level analysis
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                    0.5 * Math.cos(2 * Math.PI * i / 8.0) +
                    0.1 * Math.random();
        }

        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        // Perform 3-level decomposition
        System.out.println("\n3-level decomposition:");
        MultiLevelTransformResult result = mwt.decompose(signal, 3);

        System.out.println("Original signal length: " + signal.length);
        System.out.println("Decomposition structure:");
        System.out.println("   Level 1 details: " + result.detailsAtLevel(1).length + " coefficients");
        System.out.println("   Level 2 details: " + result.detailsAtLevel(2).length + " coefficients");
        System.out.println("   Level 3 details: " + result.detailsAtLevel(3).length + " coefficients");
        System.out.println("   Final approximation: " + result.finalApproximation().length + " coefficients");

        // Energy analysis
        System.out.println("\nEnergy distribution:");
        for (int level = 1; level <= 3; level++) {
            double energy = result.detailEnergyAtLevel(level);
            System.out.printf("   Level %d: %.4f\n", level, energy);
        }

        // Perfect reconstruction
        double[] reconstructed = mwt.reconstruct(result);
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        System.out.println("\nMax reconstruction error: " + String.format("%.2e", maxError));

        // Denoising demonstration
        System.out.println("\nDenoising by removing finest scale:");
        double[] denoised = mwt.reconstructFromLevel(result, 2);
        System.out.println("   Denoised signal preserves main trends while removing noise");
    }

    private static void demonstrateStreamingDenoiser() {
        System.out.println("\n\nSTREAMING DENOISER");
        System.out.println("==================");
        System.out.println("Real-time denoising with dual implementations\n");

        try {
            // Generate a noisy signal
            int signalLength = 1024;
            double[] noisySignal = new double[signalLength];
            for (int i = 0; i < signalLength; i++) {
                // Clean signal: sine wave
                double clean = Math.sin(2 * Math.PI * i / 64.0);
                // Add noise
                double noise = 0.2 * (Math.random() - 0.5);
                noisySignal[i] = clean + noise;
            }

            // Configuration for streaming denoiser
            StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                    .wavelet(Daubechies.DB4)
                    .blockSize(256)
                    .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
                    .build();

            // Demonstrate FAST implementation
            System.out.println("1. FAST Implementation (Real-time optimized):");
            demonstrateStreamingImplementation(
                    StreamingDenoiserFactory.Implementation.FAST,
                    config,
                    noisySignal
            );

            // Demonstrate QUALITY implementation
            System.out.println("\n2. QUALITY Implementation (SNR optimized):");
            demonstrateStreamingImplementation(
                    StreamingDenoiserFactory.Implementation.QUALITY,
                    config,
                    noisySignal
            );

            // Demonstrate AUTO selection
            System.out.println("\n3. AUTO Implementation (Factory selects based on config):");
            StreamingDenoiserConfig autoConfig = new StreamingDenoiserConfig.Builder()
                    .wavelet(new Haar())
                    .blockSize(128)  // Small block size will trigger FAST selection
                    .build();

            StreamingDenoiserStrategy autoDenoiser = StreamingDenoiserFactory.create(autoConfig);
            StreamingDenoiserStrategy.PerformanceProfile profile = autoDenoiser.getPerformanceProfile();
            System.out.println("   Factory selected: " + (profile.expectedLatencyMicros() < 1.0 ? "FAST" : "QUALITY"));
            System.out.println("   Expected latency: " + String.format("%.2f", profile.expectedLatencyMicros()) + " µs/sample");
            System.out.println("   Real-time capable: " + profile.isRealTimeCapable());
            autoDenoiser.close();

        } catch (Exception e) {
            System.err.println("Error in streaming denoiser demo: " + e.getMessage());
        }
    }

    private static void demonstrateStreamingImplementation(
            StreamingDenoiserFactory.Implementation impl,
            StreamingDenoiserConfig config,
            double[] noisySignal) throws Exception {

        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(impl, config);

        // Get performance profile
        StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();
        System.out.println("   Expected latency: " + String.format("%.2f", profile.expectedLatencyMicros()) + " µs/sample");
        System.out.println("   Expected SNR improvement: " + String.format("%.1f", profile.expectedSNRImprovement()) + " dB");
        System.out.println("   Memory usage: " + (profile.memoryUsageBytes() / 1024) + " KB");
        System.out.println("   Real-time capable: " + profile.isRealTimeCapable());

        // Process the signal
        List<double[]> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(double[] item) {
                results.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        // Process in chunks to simulate streaming
        long startTime = System.nanoTime();
        int chunkSize = 64;
        for (int i = 0; i < noisySignal.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, noisySignal.length);
            double[] chunk = Arrays.copyOfRange(noisySignal, i, end);
            denoiser.process(chunk);
        }
        denoiser.close();

        latch.await(1, TimeUnit.SECONDS);
        long elapsedNanos = System.nanoTime() - startTime;

        // Report actual performance
        StreamingWaveletTransform.StreamingStatistics stats = denoiser.getStatistics();
        System.out.println("   Actual performance:");
        System.out.println("     - Samples processed: " + stats.getSamplesProcessed());
        System.out.println("     - Blocks emitted: " + stats.getBlocksEmitted());
        System.out.println("     - Average processing time: " + String.format("%.3f", stats.getAverageProcessingTime()) + " ms/block");
        System.out.println("     - Total time: " + String.format("%.2f", elapsedNanos / 1_000_000.0) + " ms");
        System.out.println("     - Throughput: " + String.format("%.2f", noisySignal.length / (elapsedNanos / 1_000_000_000.0)) + " samples/sec");
    }
}