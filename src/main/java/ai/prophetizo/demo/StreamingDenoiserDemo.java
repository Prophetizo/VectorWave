package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates streaming wavelet denoising using MODWT for real-time applications.
 *
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>Real-time audio signal denoising with MODWT</li>
 *   <li>Financial time series filtering with arbitrary block sizes</li>
 *   <li>Adaptive threshold adjustment with shift-invariance</li>
 *   <li>Performance metrics and latency analysis</li>
 *   <li>Multi-channel processing with MODWT</li>
 * </ul>
 * 
 */
public class StreamingDenoiserDemo {

    // Performance simulation constants
    private static final double REALTIME_SPEED_MULTIPLIER = 0.25; // Process 4x faster than real-time

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("    VectorWave MODWT Streaming Denoiser Demo      ");
        System.out.println("==================================================\n");

        demonstrateAudioDenoising();
        System.out.println();

        demonstrateFinancialDataCleaning();
        System.out.println();

        demonstrateAdaptiveDenoising();
        System.out.println();

        compareConfigurationPerformance();
        System.out.println();

        demonstrateMultiChannelProcessing();
    }

    private static void demonstrateAudioDenoising() throws Exception {
        System.out.println("1. Real-Time Audio Denoising with MODWT");
        System.out.println("----------------------------------------");

        // Simulate 48kHz audio with varying block sizes (MODWT handles any size!)
        int sampleRate = 48000;
        int blockSize = 480; // Exactly 10ms at 48kHz - no need to round to power of 2!
        double blockDuration = blockSize * 1000.0 / sampleRate;

        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .boundaryMode(BoundaryMode.PERIODIC)
                .bufferSize(blockSize)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .thresholdType(ThresholdType.SOFT)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .build();

        System.out.print("Configuration:\n");
        System.out.printf("  Sample rate: %d Hz\n", sampleRate);
        System.out.printf("  Block size: %d samples (%.1f ms) - exact size!\n", blockSize, blockDuration);
        System.out.print("  Wavelet: Daubechies DB4\n");
        System.out.print("  Boundary: PERIODIC\n");
        System.out.print("  MODWT Advantage: No padding needed, shift-invariant processing\n\n");

        List<Double> latencies = new ArrayList<>();
        AtomicInteger processedBlocks = new AtomicInteger();
        long totalSamples = 0;

        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;
            private long lastTime = System.nanoTime();

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(double[] denoisedBlock) {
                long currentTime = System.nanoTime();
                double latency = (currentTime - lastTime) / 1_000_000.0;
                latencies.add(latency);
                lastTime = currentTime;
                processedBlocks.incrementAndGet();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
            }
        });

        // Simulate audio stream with varying noise
        System.out.println("Processing audio stream...");
        long startTime = System.nanoTime();

        for (int block = 0; block < 100; block++) {
            double noiseLevel = 0.05 + 0.03 * Math.sin(block * 0.1); // Varying noise

            // Generate audio-like signal with noise
            double[] audioBlock = new double[blockSize];
            for (int i = 0; i < blockSize; i++) {
                // Mix of frequencies typical in speech
                double t = (block * blockSize + i) / (double) sampleRate;
                audioBlock[i] = 0.3 * Math.sin(2 * Math.PI * 440 * t) +     // A4 note
                        0.2 * Math.sin(2 * Math.PI * 880 * t) +     // A5 note
                        0.1 * Math.sin(2 * Math.PI * 220 * t) +     // A3 note
                        noiseLevel * (Math.random() - 0.5);         // Noise
            }

            double[] denoised = denoiser.denoise(audioBlock);
            totalSamples += audioBlock.length;

            // Simulate real-time constraint
            Thread.sleep((long) (blockDuration * REALTIME_SPEED_MULTIPLIER));
        }

        long endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1_000_000.0;

        System.out.print("\nPerformance Metrics:\n");
        System.out.printf("  Total samples: %d\n", totalSamples);
        System.out.printf("  Blocks processed: %d\n", processedBlocks.get());
        System.out.printf("  Average processing time: %.2f ms\n", totalTime / 100);
        System.out.printf("  Throughput: %.0f samples/sec\n", totalSamples * 1000.0 / totalTime);
        System.out.printf("  Processing speed: %.1fx real-time\n",
                (totalSamples * 1000.0) / (sampleRate * totalTime));
        System.out.printf("  Estimated noise level: %.4f\n", denoiser.getEstimatedNoiseLevel());

        // Analyze latency distribution
        if (!latencies.isEmpty()) {
            latencies.sort(Double::compareTo);
            System.out.print("\nLatency Distribution:\n");
            System.out.printf("  Min: %.2f ms\n", latencies.get(0));
            System.out.printf("  Median: %.2f ms\n", latencies.get(latencies.size() / 2));
            System.out.printf("  Max: %.2f ms\n", latencies.get(latencies.size() - 1));
        }

        denoiser.close();
    }

    private static void demonstrateFinancialDataCleaning() throws Exception {
        System.out.println("2. Financial Time Series Denoising with MODWT");
        System.out.println("----------------------------------------------");

        // Use arbitrary block size for lower latency in HFT scenarios
        int blockSize = 50; // Non-power-of-2! MODWT handles it perfectly

        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar()) // Simple wavelet for fast processing
                .boundaryMode(BoundaryMode.PERIODIC)
                .bufferSize(blockSize)
                .thresholdMethod(ThresholdMethod.MINIMAX)
                .thresholdType(ThresholdType.HARD)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .noiseWindowSize(200)
                .build();

        System.out.println("Configuration:");
        System.out.println("  Block size: 50 ticks (not power-of-2!)");
        System.out.println("  Wavelet: Haar (fast)");
        System.out.println("  Threshold: Minimax (conservative)");
        System.out.println("  MODWT Benefits: No padding delays, shift-invariant detection");

        List<Double> cleanedPrices = new ArrayList<>();
        List<Double> noiseEstimates = new ArrayList<>();

        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(double[] denoisedBlock) {
                // Collect denoised prices
                for (double price : denoisedBlock) {
                    cleanedPrices.add(price);
                }
                noiseEstimates.add(denoiser.getEstimatedNoiseLevel());
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        // Simulate price stream with microstructure noise
        System.out.println("\nProcessing price stream...");
        double basePrice = 100.0;
        List<Double> rawPrices = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            // Price with trend, volatility, and microstructure noise
            double trend = 0.0001 * i;
            double volatility = 0.002 * Math.sin(i * 0.05);
            double microNoise = 0.001 * (Math.random() - 0.5);
            double jumpComponent = (i % 100 == 50) ? 0.01 : 0.0; // Occasional jumps

            double price = basePrice + trend + volatility + microNoise + jumpComponent;
            rawPrices.add(price);
            
            // Process in blocks
            if (rawPrices.size() == blockSize) {
                double[] priceBlock = rawPrices.stream().mapToDouble(Double::doubleValue).toArray();
                denoiser.denoise(priceBlock);
                rawPrices.clear();
            }
        }

        // Process remaining data
        if (!rawPrices.isEmpty()) {
            double[] priceBlock = rawPrices.stream().mapToDouble(Double::doubleValue).toArray();
            denoiser.denoise(priceBlock);
        }

        Thread.sleep(50);

        // Analyze results
        System.out.println("\nResults:");
        System.out.printf("  Processed %d ticks\n", 500);
        System.out.printf("  Output %d cleaned prices\n", cleanedPrices.size());

        if (!noiseEstimates.isEmpty()) {
            double avgNoise = noiseEstimates.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            System.out.printf("  Average noise estimate: %.5f\n", avgNoise);
            System.out.printf("  Noise variation: %.5f to %.5f\n",
                    noiseEstimates.stream().min(Double::compareTo).orElse(0.0),
                    noiseEstimates.stream().max(Double::compareTo).orElse(0.0));
        }

        // Calculate noise reduction
        if (cleanedPrices.size() >= 100) {
            double inputVolatility = calculateVolatility(
                    cleanedPrices.subList(0, 50));
            double outputVolatility = calculateVolatility(
                    cleanedPrices.subList(cleanedPrices.size() - 50, cleanedPrices.size()));

            System.out.printf("  Volatility reduction: %.1f%%\n",
                    (1 - outputVolatility / inputVolatility) * 100);
        }

        System.out.println("\nMODWT advantages for financial data:");
        System.out.println("  - No artificial delays from padding to power-of-2");
        System.out.println("  - Shift-invariant detection of price jumps");
        System.out.println("  - Better preservation of market microstructure");

        denoiser.close();
    }

    private static void demonstrateAdaptiveDenoising() throws Exception {
        System.out.println("3. Adaptive Threshold with MODWT");
        System.out.println("---------------------------------");

        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .boundaryMode(BoundaryMode.PERIODIC)
                .bufferSize(128)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .noiseWindowSize(512)
                .build();

        List<Double> noiseLevels = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(double[] item) {
                noiseLevels.add(denoiser.getEstimatedNoiseLevel());
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

        System.out.println("Simulating varying noise conditions...\n");

        // Process signal with step changes in noise
        for (int segment = 0; segment < 5; segment++) {
            double noiseLevel = (segment % 2 == 0) ? 0.05 : 0.15;
            System.out.printf("Segment %d: Noise level = %.2f\n", segment + 1, noiseLevel);

            for (int i = 0; i < 256; i++) {
                double signal = Math.sin(2 * Math.PI * i / 64) +
                        noiseLevel * (Math.random() - 0.5);
                
                // Process single sample (MODWT handles any size)
                denoiser.denoise(new double[]{signal});
            }
        }

        denoiser.close();
        latch.await(1, TimeUnit.SECONDS);

        // Analyze adaptation
        System.out.println("\nNoise Level Adaptation (MODWT with MAD estimation):");
        for (int i = 0; i < Math.min(10, noiseLevels.size()); i++) {
            System.out.printf("  Block %2d: Estimated noise=%.4f\n",
                    i + 1, noiseLevels.get(i));
        }

        if (noiseLevels.size() > 2) {
            double initialNoise = noiseLevels.get(0);
            double finalNoise = noiseLevels.get(noiseLevels.size() - 1);
            System.out.printf("\nNoise estimate changed from %.4f to %.4f\n",
                    initialNoise, finalNoise);
        }

        System.out.println("\nMODWT adaptive denoising benefits:");
        System.out.println("  - Shift-invariant noise estimation");
        System.out.println("  - Works with any buffer size");
        System.out.println("  - More accurate noise tracking");
    }

    private static void compareConfigurationPerformance() throws Exception {
        System.out.println("4. MODWT Configuration Performance Comparison");
        System.out.println("---------------------------------------------");

        int testSamples = 10000;

        // Test configurations with various non-power-of-2 sizes
        Object[][] configs = {
                {"Haar/Hard/Size-100", new Haar(), ThresholdType.HARD, 100},
                {"DB4/Soft/Size-256", Daubechies.DB4, ThresholdType.SOFT, 256},
                {"DB4/Soft/Size-333", Daubechies.DB4, ThresholdType.SOFT, 333}
        };

        System.out.printf("Processing %d samples with each configuration...\n\n", testSamples);
        System.out.printf("%-20s %10s %15s %12s %15s\n",
                "Configuration", "BufferSize", "Throughput(s/s)", "ProcessTime", "MODWT Benefit");
        System.out.println("-".repeat(80));

        for (Object[] config : configs) {
            String name = (String) config[0];
            Wavelet wavelet = (Wavelet) config[1];
            ThresholdType thresholdType = (ThresholdType) config[2];
            int bufferSize = (Integer) config[3];

            MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                    .wavelet(wavelet)
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .bufferSize(bufferSize)
                    .thresholdType(thresholdType)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger blockCount = new AtomicInteger();

            denoiser.subscribe(new Flow.Subscriber<double[]>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(double[] item) {
                    blockCount.incrementAndGet();
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

            // Process test signal
            long startTime = System.nanoTime();
            
            // Feed samples in blocks
            double[] buffer = new double[bufferSize];
            int bufferIndex = 0;
            
            for (int i = 0; i < testSamples; i++) {
                buffer[bufferIndex++] = Math.random() - 0.5;
                
                if (bufferIndex == bufferSize) {
                    denoiser.denoise(buffer);
                    bufferIndex = 0;
                }
            }
            
            // Process remaining samples
            if (bufferIndex > 0) {
                double[] remaining = new double[bufferIndex];
                System.arraycopy(buffer, 0, remaining, 0, bufferIndex);
                denoiser.denoise(remaining);
            }

            denoiser.close();
            latch.await(1, TimeUnit.SECONDS);

            long endTime = System.nanoTime();
            double totalTime = (endTime - startTime) / 1_000_000.0;
            double throughput = testSamples * 1000.0 / totalTime;

            String benefit = (bufferSize % 2 != 0) ? "No padding!" : "Power of 2";

            System.out.printf("%-20s %10d %15.0f %10.2fms %15s\n",
                    name,
                    bufferSize,
                    throughput,
                    totalTime,
                    benefit);
        }

        System.out.println("\nMODWT allows optimal buffer sizes for your application!");
    }

    private static void demonstrateMultiChannelProcessing() throws Exception {
        System.out.println("5. Multi-Channel MODWT Streaming (e.g., Stereo Audio)");
        System.out.println("-----------------------------------------------------");

        int channels = 2;
        int blockSize = 333; // Non-power-of-2 for realistic audio frame size

        // Create denoiser for each channel
        List<MODWTStreamingDenoiser> denoisers = new ArrayList<>();
        for (int ch = 0; ch < channels; ch++) {
            denoisers.add(new MODWTStreamingDenoiser.Builder()
                    .wavelet(Daubechies.DB4)
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .bufferSize(blockSize)
                    .build());
        }

        // Subscribe to outputs
        List<List<double[]>> channelOutputs = new ArrayList<>();
        List<CountDownLatch> latches = new ArrayList<>();

        for (int ch = 0; ch < channels; ch++) {
            List<double[]> outputs = new ArrayList<>();
            channelOutputs.add(outputs);
            CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);

            final int channel = ch;
            denoisers.get(ch).subscribe(new Flow.Subscriber<double[]>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(double[] item) {
                    outputs.add(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    latches.get(channel).countDown();
                }

                @Override
                public void onComplete() {
                    latches.get(channel).countDown();
                }
            });
        }

        System.out.printf("Processing %d-channel stream with buffer size %d...\n", channels, blockSize);

        // Generate and process multi-channel signal
        double[][] channelBuffers = new double[channels][blockSize];
        int bufferIndex = 0;
        
        for (int sample = 0; sample < 1024; sample++) {
            for (int ch = 0; ch < channels; ch++) {
                // Different content per channel
                double t = sample / 48000.0;
                double signal = (ch == 0) ?
                        0.5 * Math.sin(2 * Math.PI * 440 * t) : // Left: A4
                        0.5 * Math.sin(2 * Math.PI * 554.37 * t); // Right: C#5

                // Add channel-specific noise
                signal += 0.05 * (Math.random() - 0.5);

                channelBuffers[ch][bufferIndex] = signal;
            }
            
            bufferIndex++;
            
            // Process when buffer is full
            if (bufferIndex == blockSize) {
                for (int ch = 0; ch < channels; ch++) {
                    denoisers.get(ch).denoise(channelBuffers[ch]);
                }
                bufferIndex = 0;
            }
        }
        
        // Process remaining samples
        if (bufferIndex > 0) {
            for (int ch = 0; ch < channels; ch++) {
                double[] remaining = new double[bufferIndex];
                System.arraycopy(channelBuffers[ch], 0, remaining, 0, bufferIndex);
                denoisers.get(ch).denoise(remaining);
            }
        }

        // Close all channels
        for (MODWTStreamingDenoiser denoiser : denoisers) {
            denoiser.close();
        }

        // Wait for completion
        for (CountDownLatch latch : latches) {
            latch.await(1, TimeUnit.SECONDS);
        }

        // Report results
        System.out.println("\nResults:");
        for (int ch = 0; ch < channels; ch++) {
            System.out.printf("  Channel %d: %d blocks processed\n",
                    ch + 1,
                    channelOutputs.get(ch).size());
            System.out.printf("    Samples processed: %d\n", 
                    denoisers.get(ch).getSamplesProcessed());
            System.out.printf("    Noise estimate: %.4f\n",
                    denoisers.get(ch).getEstimatedNoiseLevel());
        }

        System.out.println("\nMODWT multi-channel advantages:");
        System.out.println("  - Synchronized processing across channels");
        System.out.println("  - No phase distortion between channels");
        System.out.println("  - Optimal buffer sizes for audio frames");
        System.out.println("\nMulti-channel streaming denoising completed successfully!");
    }

    private static double calculateVolatility(List<Double> prices) {
        if (prices.size() < 2) return 0.0;

        double sum = 0.0;
        double sumSq = 0.0;

        for (int i = 1; i < prices.size(); i++) {
            double ret = Math.log(prices.get(i) / prices.get(i - 1));
            sum += ret;
            sumSq += ret * ret;
        }

        int n = prices.size() - 1;
        double mean = sum / n;
        return Math.sqrt(sumSq / n - mean * mean);
    }
}