package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.streaming.OverlapBuffer;
import ai.prophetizo.wavelet.streaming.StreamingDenoiser;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform.StreamingStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates streaming wavelet denoising for real-time applications.
 * 
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>Real-time audio signal denoising</li>
 *   <li>Financial time series filtering</li>
 *   <li>Adaptive threshold adjustment</li>
 *   <li>Performance metrics and latency analysis</li>
 * </ul>
 */
public class StreamingDenoiserDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("         VectorWave Streaming Denoiser Demo       ");
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
        System.out.println("1. Real-Time Audio Denoising");
        System.out.println("----------------------------");
        
        // Simulate 48kHz audio with 10ms blocks (480 samples, rounded to 512)
        int sampleRate = 48000;
        int blockSize = 512;
        double blockDuration = blockSize * 1000.0 / sampleRate;
        
        try (StreamingDenoiser denoiser = new StreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(blockSize)
                .overlapFactor(0.75)
                .levels(2)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .thresholdType(ThresholdType.SOFT)
                .windowFunction(OverlapBuffer.WindowFunction.HANN)
                .adaptiveThreshold(true)
                .attackTime(5.0)
                .releaseTime(20.0)
                .build()) {
            
            System.out.printf("Configuration:\n");
            System.out.printf("  Sample rate: %d Hz\n", sampleRate);
            System.out.printf("  Block size: %d samples (%.1f ms)\n", blockSize, blockDuration);
            System.out.printf("  Overlap: 75%% (%.1f ms hop)\n", blockDuration * 0.25);
            System.out.printf("  Wavelet: Daubechies DB4\n");
            System.out.printf("  Levels: 2\n\n");
            
            List<Double> latencies = new ArrayList<>();
            AtomicInteger processedBlocks = new AtomicInteger();
            
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
                public void onComplete() {}
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
                
                denoiser.process(audioBlock);
                
                // Simulate real-time constraint
                Thread.sleep((long)(blockDuration * 0.25)); // Process 4x real-time
            }
            
            long endTime = System.nanoTime();
            double totalTime = (endTime - startTime) / 1_000_000.0;
            
            // Get statistics
            StreamingStatistics stats = denoiser.getStatistics();
            
            System.out.printf("\nPerformance Metrics:\n");
            System.out.printf("  Total samples: %d\n", stats.getSamplesProcessed());
            System.out.printf("  Blocks processed: %d\n", processedBlocks.get());
            System.out.printf("  Average latency: %.2f ms\n", stats.getAverageProcessingTime() / 1_000_000.0);
            System.out.printf("  Throughput: %.0f samples/sec\n", stats.getThroughput());
            System.out.printf("  Processing speed: %.1fx real-time\n", 
                             (100 * blockSize * 1000.0) / (sampleRate * totalTime));
            
            // Analyze latency distribution
            if (!latencies.isEmpty()) {
                latencies.sort(Double::compareTo);
                System.out.printf("\nLatency Distribution:\n");
                System.out.printf("  Min: %.2f ms\n", latencies.get(0));
                System.out.printf("  Median: %.2f ms\n", latencies.get(latencies.size() / 2));
                System.out.printf("  Max: %.2f ms\n", latencies.get(latencies.size() - 1));
            }
        }
    }
    
    private static void demonstrateFinancialDataCleaning() throws Exception {
        System.out.println("2. Financial Time Series Denoising");
        System.out.println("----------------------------------");
        
        // Smaller blocks for lower latency in HFT scenarios
        int blockSize = 64;
        
        try (StreamingDenoiser denoiser = new StreamingDenoiser.Builder()
                .wavelet(new Haar()) // Simple wavelet for fast processing
                .blockSize(blockSize)
                .overlapFactor(0.5)
                .levels(1)
                .thresholdMethod(ThresholdMethod.MINIMAX)
                .thresholdType(ThresholdType.HARD)
                .adaptiveThreshold(true)
                .attackTime(2.0)  // Fast response to volatility changes
                .releaseTime(10.0)
                .build()) {
            
            System.out.println("Configuration:");
            System.out.println("  Block size: 64 ticks");
            System.out.println("  Overlap: 50%");
            System.out.println("  Wavelet: Haar (fast)");
            System.out.println("  Threshold: Minimax (conservative)");
            
            List<Double> cleanedPrices = new ArrayList<>();
            List<Double> noiseEstimates = new ArrayList<>();
            
            denoiser.subscribe(new Flow.Subscriber<double[]>() {
                private Flow.Subscription subscription;
                
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(double[] denoisedBlock) {
                    // Collect denoised prices
                    for (double price : denoisedBlock) {
                        cleanedPrices.add(price);
                    }
                    noiseEstimates.add(denoiser.getCurrentNoiseLevel());
                }
                
                @Override
                public void onError(Throwable throwable) {}
                
                @Override
                public void onComplete() {}
            });
            
            // Simulate price stream with microstructure noise
            System.out.println("\nProcessing price stream...");
            double basePrice = 100.0;
            
            for (int i = 0; i < 500; i++) {
                // Price with trend, volatility, and microstructure noise
                double trend = 0.0001 * i;
                double volatility = 0.002 * Math.sin(i * 0.05);
                double microNoise = 0.001 * (Math.random() - 0.5);
                double jumpComponent = (i % 100 == 50) ? 0.01 : 0.0; // Occasional jumps
                
                double price = basePrice + trend + volatility + microNoise + jumpComponent;
                denoiser.process(price);
            }
            
            denoiser.flush();
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
                    cleanedPrices.subList(0, 50)); // Use first part as proxy
                double outputVolatility = calculateVolatility(
                    cleanedPrices.subList(cleanedPrices.size() - 50, cleanedPrices.size()));
                
                System.out.printf("  Volatility reduction: %.1f%%\n",
                    (1 - outputVolatility / inputVolatility) * 100);
            }
        }
    }
    
    private static void demonstrateAdaptiveDenoising() throws Exception {
        System.out.println("3. Adaptive Threshold Demonstration");
        System.out.println("-----------------------------------");
        
        try (StreamingDenoiser denoiser = new StreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(128)
                .overlapFactor(0.5)
                .adaptiveThreshold(true)
                .attackTime(3.0)
                .releaseTime(15.0)
                .build()) {
            
            List<Double> thresholds = new ArrayList<>();
            List<Double> noiseLevels = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            denoiser.subscribe(new Flow.Subscriber<double[]>() {
                private Flow.Subscription subscription;
                
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(double[] item) {
                    thresholds.add(denoiser.getCurrentThreshold());
                    noiseLevels.add(denoiser.getCurrentNoiseLevel());
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
                    denoiser.process(signal);
                }
            }
            
            denoiser.close();
            latch.await(1, TimeUnit.SECONDS);
            
            // Analyze adaptation
            System.out.println("\nThreshold Adaptation:");
            for (int i = 0; i < Math.min(10, thresholds.size()); i++) {
                System.out.printf("  Block %2d: Noise=%.4f, Threshold=%.4f\n",
                    i + 1, noiseLevels.get(i), thresholds.get(i));
            }
            
            if (thresholds.size() > 2) {
                double initialThreshold = thresholds.get(0);
                double finalThreshold = thresholds.get(thresholds.size() - 1);
                System.out.printf("\nThreshold changed from %.4f to %.4f (%.1fx)\n",
                    initialThreshold, finalThreshold, finalThreshold / initialThreshold);
            }
        }
    }
    
    private static void compareConfigurationPerformance() throws Exception {
        System.out.println("4. Configuration Performance Comparison");
        System.out.println("---------------------------------------");
        
        int testSamples = 10000;
        
        // Test configurations
        Object[][] configs = {
            {"Haar/Hard/1-level", new Haar(), ThresholdType.HARD, 1},
            {"DB4/Soft/1-level", Daubechies.DB4, ThresholdType.SOFT, 1},
            {"DB4/Soft/3-level", Daubechies.DB4, ThresholdType.SOFT, 3}
        };
        
        System.out.printf("Processing %d samples with each configuration...\n\n", testSamples);
        System.out.printf("%-20s %10s %10s %10s\n", 
            "Configuration", "Latency(ms)", "Throughput", "Blocks");
        System.out.println("-".repeat(55));
        
        for (Object[] config : configs) {
            String name = (String) config[0];
            
            try (StreamingDenoiser denoiser = new StreamingDenoiser.Builder()
                    .wavelet((ai.prophetizo.wavelet.api.Wavelet) config[1])
                    .blockSize(256)
                    .overlapFactor(0.5)
                    .thresholdType((ThresholdType) config[2])
                    .levels((Integer) config[3])
                    .build()) {
                
                CountDownLatch latch = new CountDownLatch(1);
                AtomicInteger blockCount = new AtomicInteger();
                
                denoiser.subscribe(new Flow.Subscriber<double[]>() {
                    private Flow.Subscription subscription;
                    
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
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
                
                for (int i = 0; i < testSamples; i++) {
                    denoiser.process(Math.random() - 0.5);
                }
                
                denoiser.close();
                latch.await(1, TimeUnit.SECONDS);
                
                long endTime = System.nanoTime();
                double totalTime = (endTime - startTime) / 1_000_000.0;
                
                StreamingStatistics stats = denoiser.getStatistics();
                
                System.out.printf("%-20s %10.2f %10.0f %10d\n",
                    name,
                    stats.getAverageProcessingTime() / 1_000_000.0,
                    stats.getThroughput(),
                    blockCount.get());
            }
        }
    }
    
    private static void demonstrateMultiChannelProcessing() throws Exception {
        System.out.println("5. Multi-Channel Streaming (e.g., Stereo Audio)");
        System.out.println("-----------------------------------------------");
        
        int channels = 2;
        int blockSize = 256;
        
        // Create denoiser for each channel
        List<StreamingDenoiser> denoisers = new ArrayList<>();
        for (int ch = 0; ch < channels; ch++) {
            denoisers.add(new StreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(blockSize)
                .overlapFactor(0.75)
                .windowFunction(OverlapBuffer.WindowFunction.HANN)
                .build());
        }
        
        try {
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
                    private Flow.Subscription subscription;
                    
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
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
            
            System.out.printf("Processing %d-channel stream...\n", channels);
            
            // Generate and process multi-channel signal
            for (int sample = 0; sample < 1024; sample++) {
                for (int ch = 0; ch < channels; ch++) {
                    // Different content per channel
                    double t = sample / 48000.0;
                    double signal = (ch == 0) ?
                        0.5 * Math.sin(2 * Math.PI * 440 * t) : // Left: A4
                        0.5 * Math.sin(2 * Math.PI * 554.37 * t); // Right: C#5
                    
                    // Add channel-specific noise
                    signal += 0.05 * (Math.random() - 0.5);
                    
                    denoisers.get(ch).process(signal);
                }
            }
            
            // Close all channels
            for (StreamingDenoiser denoiser : denoisers) {
                denoiser.close();
            }
            
            // Wait for completion
            for (CountDownLatch latch : latches) {
                latch.await(1, TimeUnit.SECONDS);
            }
            
            // Report results
            System.out.println("\nResults:");
            for (int ch = 0; ch < channels; ch++) {
                StreamingStatistics stats = denoisers.get(ch).getStatistics();
                System.out.printf("  Channel %d: %d blocks, %.2f ms avg latency\n",
                    ch + 1, 
                    channelOutputs.get(ch).size(),
                    stats.getAverageProcessingTime() / 1_000_000.0);
            }
            
            System.out.println("\nMulti-channel streaming denoising completed successfully!");
            
        } finally {
            // Ensure cleanup
            for (StreamingDenoiser denoiser : denoisers) {
                if (denoiser.isReady()) {
                    denoiser.close();
                }
            }
        }
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