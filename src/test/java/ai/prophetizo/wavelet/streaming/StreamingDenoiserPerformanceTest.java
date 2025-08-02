package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmarks for StreamingDenoiser.
 * 
 * Measures:
 * - Latency per sample (time to process each new signal point)
 * - Throughput (samples per second)
 * - Block processing time
 * - Memory usage
 */
class StreamingDenoiserPerformanceTest {
    
    // Adapter class for backward compatibility
    private static class StreamingDenoiser {
        static class Builder {
            private StreamingDenoiserConfig.Builder configBuilder = new StreamingDenoiserConfig.Builder();
            
            Builder wavelet(ai.prophetizo.wavelet.api.Wavelet wavelet) {
                configBuilder.wavelet(wavelet);
                return this;
            }
            
            Builder blockSize(int blockSize) {
                configBuilder.blockSize(blockSize);
                return this;
            }
            
            Builder overlapFactor(double overlapFactor) {
                configBuilder.overlapFactor(overlapFactor);
                return this;
            }
            
            Builder thresholdMethod(ThresholdMethod method) {
                configBuilder.thresholdMethod(method);
                return this;
            }
            
            Builder adaptiveThreshold(boolean adaptive) {
                configBuilder.adaptiveThreshold(adaptive);
                return this;
            }
            
            StreamingDenoiserStrategy build() {
                return StreamingDenoiserFactory.create(
                    StreamingDenoiserFactory.Implementation.FAST, configBuilder.build());
            }
        }
    }
    
    private static final int WARMUP_SAMPLES = 10_000;
    private static final int BENCHMARK_SAMPLES = 100_000;
    private static final int BLOCK_SIZE = 256;
    
    @Test
    // @Disabled("Performance test - run manually")
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void benchmarkCurrentImplementation() throws Exception {
        System.out.println("=== Current StreamingDenoiser Performance Benchmark ===");
        
        // Test different configurations
        benchmarkConfiguration("No overlap", 0.0);
        benchmarkConfiguration("50% overlap", 0.5);
        benchmarkConfiguration("75% overlap", 0.75);
    }
    
    @SuppressWarnings("try")  // close() may throw InterruptedException
    private void benchmarkConfiguration(String description, double overlapFactor) throws Exception {
        System.out.println("\n--- " + description + " ---");
        
        try (StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(BLOCK_SIZE)
                .overlapFactor(overlapFactor)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .adaptiveThreshold(false)
                .build()) {
            
            TestSubscriber subscriber = new TestSubscriber();
            denoiser.subscribe(subscriber);
            
            // Warmup
            long warmupStart = System.nanoTime();
            for (int i = 0; i < WARMUP_SAMPLES; i++) {
                denoiser.process(Math.sin(2 * Math.PI * i / 64) + 0.1 * Math.random());
            }
            long warmupEnd = System.nanoTime();
            
            // Reset statistics
            subscriber.reset();
            
            // Benchmark
            long benchmarkStart = System.nanoTime();
            long[] sampleTimes = new long[BENCHMARK_SAMPLES];
            
            for (int i = 0; i < BENCHMARK_SAMPLES; i++) {
                long sampleStart = System.nanoTime();
                denoiser.process(Math.sin(2 * Math.PI * i / 64) + 0.1 * Math.random());
                sampleTimes[i] = System.nanoTime() - sampleStart;
            }
            
            long benchmarkEnd = System.nanoTime();
            // Denoiser will be closed by try-with-resources
            
            // Wait for all blocks to be processed
            subscriber.latch.await(5, TimeUnit.SECONDS);
            
            // Calculate metrics
            analyzePerformance(sampleTimes, subscriber, benchmarkEnd - benchmarkStart, overlapFactor);
        }
    }
    
    private void analyzePerformance(long[] sampleTimes, TestSubscriber subscriber, 
                                   long totalTime, double overlapFactor) {
        // Per-sample latency statistics
        long minLatency = Long.MAX_VALUE;
        long maxLatency = 0;
        long totalLatency = 0;
        
        for (long time : sampleTimes) {
            minLatency = Math.min(minLatency, time);
            maxLatency = Math.max(maxLatency, time);
            totalLatency += time;
        }
        
        double avgLatencyNs = (double) totalLatency / sampleTimes.length;
        double avgLatencyUs = avgLatencyNs / 1000.0;
        
        // Throughput
        double throughputSamplesPerSec = BENCHMARK_SAMPLES * 1_000_000_000.0 / totalTime;
        
        // Block processing statistics
        double avgBlockTimeMs = subscriber.getAverageBlockProcessingTime() / 1_000_000.0;
        double maxBlockTimeMs = subscriber.getMaxBlockProcessingTime() / 1_000_000.0;
        
        // Calculate effective latency (time from input to output)
        int hopSize = (int)(BLOCK_SIZE * (1 - overlapFactor));
        double effectiveLatencyMs = (BLOCK_SIZE / throughputSamplesPerSec) * 1000;
        
        // Print results
        System.out.printf("Sample Processing:\n");
        System.out.printf("  Average latency: %.2f µs/sample\n", avgLatencyUs);
        System.out.printf("  Min latency: %.2f µs\n", minLatency / 1000.0);
        System.out.printf("  Max latency: %.2f µs\n", maxLatency / 1000.0);
        System.out.printf("  Throughput: %.0f samples/second\n", throughputSamplesPerSec);
        
        System.out.printf("\nBlock Processing:\n");
        System.out.printf("  Blocks processed: %d\n", subscriber.blocksProcessed);
        System.out.printf("  Average block time: %.2f ms\n", avgBlockTimeMs);
        System.out.printf("  Max block time: %.2f ms\n", maxBlockTimeMs);
        System.out.printf("  Hop size: %d samples\n", hopSize);
        
        System.out.printf("\nLatency Characteristics:\n");
        System.out.printf("  Algorithmic latency: %d samples (%.2f ms @ 48kHz)\n", 
            BLOCK_SIZE, BLOCK_SIZE / 48.0);
        System.out.printf("  Effective processing latency: %.2f ms\n", effectiveLatencyMs);
        
        // Memory estimate (rough)
        long memoryEstimate = estimateMemoryUsage(overlapFactor);
        System.out.printf("\nMemory Usage (estimate):\n");
        System.out.printf("  Per-instance: ~%.1f KB\n", memoryEstimate / 1024.0);
    }
    
    private long estimateMemoryUsage(double overlapFactor) {
        // Rough estimate based on buffer sizes
        long memory = 0;
        memory += BLOCK_SIZE * 8; // Input buffer
        memory += BLOCK_SIZE * 8; // Processing buffer  
        memory += (int)(BLOCK_SIZE * overlapFactor) * 8; // Overlap buffer
        memory += BLOCK_SIZE * 4 * 8; // Transform working buffers (estimate)
        memory += 1024 * 8; // Noise estimator buffer
        return memory;
    }
    
    private static class TestSubscriber implements Flow.Subscriber<double[]> {
        private final List<Long> blockProcessingTimes = new ArrayList<>();
        private final CountDownLatch latch = new CountDownLatch(1);
        private long lastBlockTime = System.nanoTime();
        private int blocksProcessed = 0;
        
        void reset() {
            blockProcessingTimes.clear();
            blocksProcessed = 0;
            lastBlockTime = System.nanoTime();
        }
        
        double getAverageBlockProcessingTime() {
            return blockProcessingTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        }
        
        long getMaxBlockProcessingTime() {
            return blockProcessingTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(double[] item) {
            long now = System.nanoTime();
            blockProcessingTimes.add(now - lastBlockTime);
            lastBlockTime = now;
            blocksProcessed++;
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void quickPerformanceCheck() throws Exception {
        // Quick test to verify performance tracking works
        try (StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(256)
                .overlapFactor(0.0)
                .build()) {
            
            TestSubscriber subscriber = new TestSubscriber();
            denoiser.subscribe(subscriber);
            
            // Process one block worth of samples
            for (int i = 0; i < 256; i++) {
                denoiser.process(Math.sin(2 * Math.PI * i / 64));
            }
            
            // Denoiser will be closed by try-with-resources
            subscriber.latch.await(1, TimeUnit.SECONDS);
            
            System.out.println("Quick check - blocks processed: " + subscriber.blocksProcessed);
            assert subscriber.blocksProcessed == 1;
        }
    }
}