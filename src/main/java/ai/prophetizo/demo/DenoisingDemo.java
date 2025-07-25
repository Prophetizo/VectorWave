package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Symlet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserStrategy;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates wavelet-based signal denoising capabilities.
 *
 * <p>This demo shows how to use the WaveletDenoiser class to remove
 * noise from signals while preserving important features.</p>
 */
public class DenoisingDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Denoising Demo ===\n");

        // Demo 1: Basic denoising with different threshold methods
        demonstrateThresholdMethods();

        // Demo 2: Soft vs Hard thresholding
        demonstrateThresholdTypes();

        // Demo 3: Multi-level denoising
        demonstrateMultiLevelDenoising();

        // Demo 4: Financial data denoising
        demonstrateFinancialDenoising();

        // Demo 5: Performance comparison
        demonstratePerformance();

        // Demo 6: Streaming vs Batch denoising
        demonstrateStreamingVsBatch();
    }

    private static void demonstrateThresholdMethods() {
        System.out.println("1. Threshold Method Comparison");
        System.out.println("------------------------------");

        // Generate test signal
        int length = 256;
        double[] cleanSignal = generateTestSignal(length);
        double[] noisySignal = addNoise(cleanSignal, 0.5, 42);

        System.out.printf("Original signal SNR: %.2f dB%n", calculateSNR(cleanSignal, noisySignal));

        // Create denoiser
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);

        // Test different threshold methods
        ThresholdMethod[] methods = {
                ThresholdMethod.UNIVERSAL,
                ThresholdMethod.SURE,
                ThresholdMethod.MINIMAX
        };

        for (ThresholdMethod method : methods) {
            double[] denoised = denoiser.denoise(noisySignal, method);
            double snr = calculateSNR(cleanSignal, denoised);
            double mse = calculateMSE(cleanSignal, denoised);

            System.out.printf("  %-10s: SNR = %.2f dB, MSE = %.6f%n",
                    method, snr, mse);
        }

        System.out.println();
    }

    private static void demonstrateThresholdTypes() {
        System.out.println("2. Soft vs Hard Thresholding");
        System.out.println("----------------------------");

        // Generate signal with discontinuities
        double[] signal = generatePiecewiseSignal(256);
        double[] noisy = addNoise(signal, 0.3, 123);

        WaveletDenoiser denoiser = new WaveletDenoiser(Symlet.SYM3, BoundaryMode.PERIODIC);

        // Compare soft and hard thresholding
        double[] softDenoised = denoiser.denoise(noisy,
                ThresholdMethod.UNIVERSAL, ThresholdType.SOFT);
        double[] hardDenoised = denoiser.denoise(noisy,
                ThresholdMethod.UNIVERSAL, ThresholdType.HARD);

        System.out.printf("Original noise level: %.4f%n", estimateNoise(noisy));
        System.out.printf("Soft thresholding residual: %.4f%n",
                calculateRMSE(signal, softDenoised));
        System.out.printf("Hard thresholding residual: %.4f%n",
                calculateRMSE(signal, hardDenoised));

        System.out.println("  -> Soft thresholding: smoother, may blur edges");
        System.out.println("  -> Hard thresholding: preserves edges better, may have artifacts");
        System.out.println();
    }

    private static void demonstrateMultiLevelDenoising() {
        System.out.println("3. Multi-level Denoising");
        System.out.println("------------------------");

        // Generate complex signal
        double[] signal = generateComplexSignal(512);
        double[] noisy = addNoise(signal, 0.4, 456);

        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);

        System.out.println("Denoising with different decomposition levels:");

        for (int levels = 1; levels <= 4; levels++) {
            double[] denoised = denoiser.denoiseMultiLevel(noisy, levels,
                    ThresholdMethod.SURE, ThresholdType.SOFT);

            double snr = calculateSNR(signal, denoised);
            System.out.printf("  Level %d: SNR = %.2f dB%n", levels, snr);
        }

        System.out.println("  -> More levels can capture different noise scales");
        System.out.println();
    }

    private static void demonstrateFinancialDenoising() {
        System.out.println("4. Financial Data Denoising");
        System.out.println("---------------------------");

        // Simulate financial returns
        double[] returns = generateFinancialReturns(256);
        double[] noisyReturns = addMicrostructureNoise(returns, 789);

        // Use optimized denoiser for financial data
        WaveletDenoiser denoiser = WaveletDenoiser.forFinancialData();

        double[] denoised = denoiser.denoise(noisyReturns, ThresholdMethod.SURE);

        // Calculate statistics
        double originalVol = calculateVolatility(returns);
        double noisyVol = calculateVolatility(noisyReturns);
        double denoisedVol = calculateVolatility(denoised);

        System.out.printf("Original volatility:  %.4f%n", originalVol);
        System.out.printf("Noisy volatility:     %.4f (+%.1f%%)%n",
                noisyVol, (noisyVol / originalVol - 1) * 100);
        System.out.printf("Denoised volatility:  %.4f (+%.1f%%)%n",
                denoisedVol, (denoisedVol / originalVol - 1) * 100);

        System.out.println("  -> Denoising removes microstructure noise");
        System.out.println("  -> Preserves true volatility better than noisy signal");
        System.out.println();
    }

    private static void demonstratePerformance() {
        System.out.println("5. Performance Comparison");
        System.out.println("-------------------------");

        int[] sizes = {256, 1024, 4096};
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);

        System.out.println("Denoising time for different signal sizes:");

        for (int size : sizes) {
            double[] signal = generateTestSignal(size);
            double[] noisy = addNoise(signal, 0.3, size);

            // Warm up
            for (int i = 0; i < 10; i++) {
                denoiser.denoise(noisy, ThresholdMethod.UNIVERSAL);
            }

            // Time the operation
            long start = System.nanoTime();
            int iterations = 100;

            for (int i = 0; i < iterations; i++) {
                denoiser.denoise(noisy, ThresholdMethod.UNIVERSAL);
            }

            long elapsed = System.nanoTime() - start;
            double avgTime = (elapsed / 1e6) / iterations; // ms

            System.out.printf("  Size %5d: %.3f ms per denoise%n", size, avgTime);
        }

        System.out.println("  -> SIMD optimizations provide significant speedup");
    }

    // Helper methods for signal generation

    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +
                    0.5 * Math.sin(2 * Math.PI * 10 * t);
        }
        return signal;
    }

    private static double[] generatePiecewiseSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            if (i < length / 3) {
                signal[i] = 1.0;
            } else if (i < 2 * length / 3) {
                signal[i] = -0.5;
            } else {
                signal[i] = 0.0;
            }
        }
        return signal;
    }

    private static double[] generateComplexSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.sin(2 * Math.PI * 3 * t) +
                    0.5 * Math.sin(2 * Math.PI * 10 * t) +
                    0.25 * Math.sin(2 * Math.PI * 25 * t) +
                    0.1 * Math.sin(2 * Math.PI * 50 * t);
        }
        return signal;
    }

    private static double[] generateFinancialReturns(int length) {
        Random rng = new Random(42);
        double[] returns = new double[length];

        // GARCH-like volatility
        double vol = 0.01;
        double longRunVol = 0.01;

        for (int i = 0; i < length; i++) {
            returns[i] = rng.nextGaussian() * vol;

            // Update volatility
            vol = 0.9 * vol + 0.1 * longRunVol +
                    0.05 * Math.abs(returns[i]);

            // Occasional jumps
            if (rng.nextDouble() < 0.02) {
                returns[i] += (rng.nextDouble() - 0.5) * 0.05;
            }
        }

        return returns;
    }

    private static double[] addNoise(double[] signal, double noiseLevel, long seed) {
        Random rng = new Random(seed);
        double[] noisy = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + rng.nextGaussian() * noiseLevel;
        }

        return noisy;
    }

    private static double[] addMicrostructureNoise(double[] returns, long seed) {
        Random rng = new Random(seed);
        double[] noisy = new double[returns.length];

        for (int i = 0; i < returns.length; i++) {
            // Bid-ask bounce
            double bounce = 0.0001 * (rng.nextDouble() - 0.5);

            // Price discreteness
            double rounding = 0.00005 * Math.round(rng.nextGaussian());

            noisy[i] = returns[i] + bounce + rounding;
        }

        return noisy;
    }

    // Analysis helper methods

    private static double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;

        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }

        if (noisePower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }

    private static double calculateMSE(double[] reference, double[] estimate) {
        double sum = 0;
        for (int i = 0; i < reference.length; i++) {
            double diff = reference[i] - estimate[i];
            sum += diff * diff;
        }
        return sum / reference.length;
    }

    private static double calculateRMSE(double[] reference, double[] estimate) {
        return Math.sqrt(calculateMSE(reference, estimate));
    }

    private static double calculateVolatility(double[] returns) {
        double mean = 0;
        for (double r : returns) {
            mean += r;
        }
        mean /= returns.length;

        double variance = 0;
        for (double r : returns) {
            variance += (r - mean) * (r - mean);
        }
        variance /= returns.length;

        return Math.sqrt(variance);
    }

    private static double estimateNoise(double[] signal) {
        // Simple noise estimation using high-frequency content
        double sum = 0;
        for (int i = 1; i < signal.length; i++) {
            double diff = signal[i] - signal[i - 1];
            sum += diff * diff;
        }
        return Math.sqrt(sum / (2 * (signal.length - 1)));
    }

    private static void demonstrateStreamingVsBatch() {
        System.out.println("6. Streaming vs Batch Denoising Comparison");
        System.out.println("------------------------------------------");
        System.out.println("Comparing batch processing with real-time streaming approaches\n");

        // Generate a longer signal for meaningful comparison
        int length = 2048;
        double[] cleanSignal = generateComplexSignal(length);
        double[] noisySignal = addNoise(cleanSignal, 0.3, 789);

        System.out.printf("Signal length: %d samples\n", length);
        System.out.printf("Input SNR: %.2f dB\n\n", calculateSNR(cleanSignal, noisySignal));

        try {
            // 1. Batch denoising (reference)
            System.out.println("Batch Denoising (Traditional):");
            long startTime = System.nanoTime();
            WaveletDenoiser batchDenoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
            double[] batchDenoised = batchDenoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);
            long batchTime = System.nanoTime() - startTime;

            double batchSNR = calculateSNR(cleanSignal, batchDenoised);
            System.out.printf("  - SNR improvement: %.2f dB\n", batchSNR - calculateSNR(cleanSignal, noisySignal));
            System.out.printf("  - Processing time: %.3f ms\n", batchTime / 1_000_000.0);
            System.out.print("  - Latency: Full signal buffering required\n\n");

            // 2. Fast streaming denoising
            System.out.println("Fast Streaming Denoising (Real-time):");
            StreamingDenoiserConfig fastConfig = new StreamingDenoiserConfig.Builder()
                    .wavelet(Daubechies.DB4)
                    .blockSize(256)
                    .thresholdMethod(ThresholdMethod.UNIVERSAL)
                    .build();

            double[] fastDenoised = processStreamingDenoiser(
                    StreamingDenoiserFactory.Implementation.FAST,
                    fastConfig,
                    noisySignal
            );

            double fastSNR = calculateSNR(cleanSignal, fastDenoised);
            System.out.printf("  - SNR improvement: %.2f dB (%.2f dB vs batch)\n",
                    fastSNR - calculateSNR(cleanSignal, noisySignal),
                    fastSNR - batchSNR);
            System.out.printf("  - Latency: ~256 samples (%.1f ms at 48kHz)\n", 256 / 48.0);
            System.out.print("  - Real-time capable: YES\n\n");

            // 3. Quality streaming denoising
            System.out.println("Quality Streaming Denoising (High SNR):");
            StreamingDenoiserConfig qualityConfig = new StreamingDenoiserConfig.Builder()
                    .wavelet(Daubechies.DB4)
                    .blockSize(512)
                    .overlapFactor(0.5)
                    .thresholdMethod(ThresholdMethod.UNIVERSAL)
                    .build();

            double[] qualityDenoised = processStreamingDenoiser(
                    StreamingDenoiserFactory.Implementation.QUALITY,
                    qualityConfig,
                    noisySignal
            );

            double qualitySNR = calculateSNR(cleanSignal, qualityDenoised);
            System.out.printf("  - SNR improvement: %.2f dB (%.2f dB vs batch)\n",
                    qualitySNR - calculateSNR(cleanSignal, noisySignal),
                    qualitySNR - batchSNR);
            System.out.printf("  - Latency: ~768 samples with 50%% overlap (%.1f ms at 48kHz)\n", 768 / 48.0);
            System.out.print("  - Real-time capable: Depends on processing power\n\n");

            // Summary
            System.out.println("Summary:");
            System.out.println("  - Batch: Best SNR but requires full signal buffering");
            System.out.println("  - Fast Streaming: Lowest latency, suitable for real-time");
            System.out.println("  - Quality Streaming: Better SNR than Fast, moderate latency");
            System.out.println("  - Choose based on your latency vs quality requirements");
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in streaming comparison: " + e.getMessage());
        }
    }

    private static double[] processStreamingDenoiser(
            StreamingDenoiserFactory.Implementation impl,
            StreamingDenoiserConfig config,
            double[] signal) throws Exception {

        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(impl, config);
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

        // Process the signal
        long startTime = System.nanoTime();
        denoiser.process(signal);
        denoiser.close();

        latch.await(1, TimeUnit.SECONDS);
        long processingTime = System.nanoTime() - startTime;

        // Get performance stats
        StreamingWaveletTransform.StreamingStatistics stats = denoiser.getStatistics();
        System.out.printf("  - Processing time: %.3f ms\n", processingTime / 1_000_000.0);
        System.out.printf("  - Throughput: %.1f ksamples/sec\n", stats.getThroughput() / 1000);
        System.out.printf("  - Blocks processed: %d\n", stats.getBlocksEmitted());

        // Reconstruct the full signal
        double[] output = new double[signal.length];
        int pos = 0;
        for (double[] block : results) {
            System.arraycopy(block, 0, output, pos, Math.min(block.length, output.length - pos));
            pos += block.length;
        }

        return output;
    }
}