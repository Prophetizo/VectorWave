package ai.prophetizo.wavelet.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform;
import ai.prophetizo.wavelet.streaming.SlidingWindowTransform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

/**
 * Demonstration of streaming wavelet transform capabilities.
 * 
 * <p>This demo shows:</p>
 * <ul>
 *   <li>Real-time signal processing simulation</li>
 *   <li>Block-based streaming transform</li>
 *   <li>Sliding window analysis</li>
 *   <li>Performance monitoring</li>
 * </ul>
 */
public class StreamingDemo {
    
    // Use seeded Random for reproducible demonstrations
    private static final Random random = new Random(42);
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== VectorWave Streaming Transform Demo ===\n");
        
        // Demo 1: Basic streaming transform
        System.out.println("1. Basic Streaming Transform Demo");
        System.out.println("---------------------------------");
        demoBasicStreaming();
        
        System.out.println("\n2. Sliding Window Transform Demo");
        System.out.println("---------------------------------");
        demoSlidingWindow();
        
        System.out.println("\n3. Real-time Audio Simulation");
        System.out.println("------------------------------");
        demoRealtimeAudio();
        
        System.out.println("\n4. Financial Tick Data Streaming");
        System.out.println("--------------------------------");
        demoFinancialStreaming();
    }
    
    private static void demoBasicStreaming() throws InterruptedException {
        // Create streaming transform
        StreamingWaveletTransform transform = StreamingWaveletTransform.create(
            Daubechies.DB4,
            BoundaryMode.PERIODIC,
            256  // block size
        );
        
        // Subscribe to results
        CountDownLatch latch = new CountDownLatch(4);  // Expect 4 blocks
        AtomicLong coeffCount = new AtomicLong();
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
                System.out.println("Subscribed to transform results");
            }
            
            @Override
            public void onNext(TransformResult result) {
                coeffCount.addAndGet(result.approximationCoeffs().length + 
                                   result.detailCoeffs().length);
                System.out.printf("Received block: %d approximation + %d detail coefficients\n",
                    result.approximationCoeffs().length,
                    result.detailCoeffs().length);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Transform complete");
            }
        });
        
        // Simulate streaming data
        double[] chunk = new double[128];
        for (int i = 0; i < 8; i++) {  // 8 chunks = 1024 samples = 4 blocks
            // Generate test signal chunk
            for (int j = 0; j < chunk.length; j++) {
                double t = (i * chunk.length + j) / 44100.0;  // 44.1kHz sample rate
                chunk[j] = Math.sin(2 * Math.PI * 440 * t) +     // 440Hz
                          0.5 * Math.sin(2 * Math.PI * 880 * t);  // 880Hz
            }
            
            transform.process(chunk);
            Thread.sleep(10);  // Simulate real-time delay
        }
        
        try {
            transform.close();
        } catch (Exception e) {
            System.err.println("Error closing transform: " + e.getMessage());
        }
        latch.await(1, TimeUnit.SECONDS);
        
        // Print statistics
        var stats = transform.getStatistics();
        System.out.printf("\nStatistics:\n");
        System.out.printf("  Samples processed: %d\n", stats.getSamplesProcessed());
        System.out.printf("  Blocks emitted: %d\n", stats.getBlocksEmitted());
        System.out.printf("  Avg processing time: %.2f µs\n", 
            stats.getAverageProcessingTime() / 1000.0);
        System.out.printf("  Throughput: %.2f kHz\n", stats.getThroughput() / 1000.0);
    }
    
    private static void demoSlidingWindow() throws InterruptedException {
        // Create sliding window transform with 75% overlap
        SlidingWindowTransform transform = new SlidingWindowTransform(
            new Haar(),
            BoundaryMode.PERIODIC,
            128,   // window size
            0.75   // 75% overlap
        );
        
        AtomicLong windowCount = new AtomicLong();
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult result) {
                long count = windowCount.incrementAndGet();
                System.out.printf("Window %d: %d approximation + %d detail coefficients\n",
                    count, result.approximationCoeffs().length, result.detailCoeffs().length);
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Sliding window analysis complete");
            }
        });
        
        // Process samples one by one to show sliding behavior
        System.out.println("Processing 512 samples with 75% overlap...");
        for (int i = 0; i < 512; i++) {
            double sample = Math.sin(2 * Math.PI * i / 32.0);
            transform.process(sample);
        }
        
        try {
            transform.close();
        } catch (Exception e) {
            System.err.println("Error closing transform: " + e.getMessage());
        }
        Thread.sleep(100);
        
        System.out.printf("Total windows generated: %d\n", windowCount.get());
    }
    
    private static void demoRealtimeAudio() throws InterruptedException {
        System.out.println("Simulating 48kHz audio stream processing...");
        
        // Create transform optimized for audio
        StreamingWaveletTransform transform = StreamingWaveletTransform.create(
            new Haar(),  // Fast for real-time
            BoundaryMode.PERIODIC,
            512  // ~10.7ms blocks at 48kHz
        );
        
        // Track latency
        AtomicLong maxLatency = new AtomicLong();
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            private Flow.Subscription subscription;
            private long lastBlockTime = System.nanoTime();
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult result) {
                long now = System.nanoTime();
                long latency = now - lastBlockTime;
                lastBlockTime = now;
                
                maxLatency.updateAndGet(val -> Math.max(val, latency));
                
                // Simulate audio processing (e.g., noise reduction)
                double[] details = result.detailCoeffs();
                int silenced = 0;
                for (int i = 0; i < details.length; i++) {
                    if (Math.abs(details[i]) < 0.01) {
                        details[i] = 0;  // Simple thresholding
                        silenced++;
                    }
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }
            
            @Override
            public void onComplete() {}
        });
        
        // Simulate 100ms of audio
        int totalSamples = 4800;  // 100ms at 48kHz
        int chunkSize = 48;       // 1ms chunks
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < totalSamples / chunkSize; i++) {
            double[] chunk = new double[chunkSize];
            for (int j = 0; j < chunkSize; j++) {
                double t = (i * chunkSize + j) / 48000.0;
                // Complex audio signal
                chunk[j] = 0.5 * Math.sin(2 * Math.PI * 440 * t) +
                          0.3 * Math.sin(2 * Math.PI * 880 * t) +
                          0.1 * Math.sin(2 * Math.PI * 1760 * t) +
                          0.05 * (random.nextDouble() - 0.5);  // noise
            }
            
            transform.process(chunk);
            
            // Simulate real-time constraint
            Thread.sleep(1);
        }
        
        try {
            transform.close();
        } catch (Exception e) {
            System.err.println("Error closing transform: " + e.getMessage());
        }
        
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.printf("Processed 100ms of audio in %dms\n", elapsedMs);
        System.out.printf("Max inter-block latency: %.2fms\n", maxLatency.get() / 1_000_000.0);
        
        var stats = transform.getStatistics();
        System.out.printf("Average block processing: %.2fµs\n", 
            stats.getAverageProcessingTime() / 1000.0);
    }
    
    private static void demoFinancialStreaming() throws InterruptedException {
        System.out.println("Simulating financial tick data stream...");
        
        // Multi-level transform for trend extraction
        StreamingWaveletTransform transform = StreamingWaveletTransform.createMultiLevel(
            Daubechies.DB4,
            BoundaryMode.ZERO_PADDING,  // No future lookahead
            64,   // Small blocks for low latency
            3     // 3 decomposition levels
        );
        
        AtomicLong tickCount = new AtomicLong();
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult result) {
                // In real scenario, extract trend and volatility here
                double[] approx = result.approximationCoeffs();
                double trend = approx.length > 0 ? approx[0] : 0;
                
                // Simple volatility estimate from detail coefficients
                double[] details = result.detailCoeffs();
                double volatility = 0;
                for (double d : details) {
                    volatility += d * d;
                }
                volatility = Math.sqrt(volatility / details.length);
                
                System.out.printf("Block %d: trend=%.4f, volatility=%.4f\n",
                    tickCount.incrementAndGet(), trend, volatility);
            }
            
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }
            
            @Override
            public void onComplete() {
                System.out.println("Financial stream processing complete");
            }
        });
        
        // Simulate tick data
        double price = 100.0;
        for (int i = 0; i < 256; i++) {
            // Random walk with drift
            double return_ = 0.0001 + 0.01 * (random.nextDouble() - 0.5);
            price *= (1 + return_);
            
            // Process log return
            transform.process(Math.log(price / 100.0));
            
            // Simulate variable tick rate
            if (random.nextDouble() < 0.3) {
                Thread.sleep(1);
            }
        }
        
        try {
            transform.close();
        } catch (Exception e) {
            System.err.println("Error closing transform: " + e.getMessage());
        }
        Thread.sleep(100);
        
        System.out.println("\nDemo complete!");
    }
}