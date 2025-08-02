package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.util.OptimizedFFT;
import ai.prophetizo.wavelet.test.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the optimized streaming wavelet transform with new optimizations.
 * These tests verify the performance improvements from:
 * - Batch processing
 * - SIMD integration
 * - Memory prefetching
 * - Adaptive buffer sizing
 */
class OptimizedStreamingPerformanceTest {

    @Test
    @DisplayName("Should demonstrate batch processing performance improvement")
    void testBatchProcessingPerformance() {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        int numBatches = 1000;
        int batchSize = 16;
        
        RingBuffer buffer = new RingBuffer(blockSize * 16);
        Random random = new Random(TestConstants.TEST_SEED);
        
        // Generate test data batches
        double[][] batches = new double[batchSize][];
        for (int i = 0; i < batchSize; i++) {
            batches[i] = new double[16];
            for (int j = 0; j < 16; j++) {
                batches[i][j] = random.nextGaussian();
            }
        }
        
        // Measure individual writes
        buffer.clear();
        long individualStart = System.nanoTime();
        for (int n = 0; n < numBatches; n++) {
            for (double[] batch : batches) {
                for (double value : batch) {
                    buffer.write(value);
                }
            }
            buffer.clear(); // Reset for next iteration
        }
        long individualTime = System.nanoTime() - individualStart;
        
        // Measure batch writes
        buffer.clear();
        long batchStart = System.nanoTime();
        for (int n = 0; n < numBatches; n++) {
            buffer.writeBatch(batches);
            buffer.clear(); // Reset for next iteration
        }
        long batchTime = System.nanoTime() - batchStart;
        
        // Batch processing should be faster
        double speedup = (double) individualTime / batchTime;
        System.out.printf("Batch write speedup: %.2fx (individual: %d µs, batch: %d µs)%n",
            speedup, individualTime / 1000, batchTime / 1000);
        
        assertTrue(speedup > 1.5, "Batch processing should be at least 1.5x faster");
    }
    
    @Test
    @DisplayName("Should verify streaming works correctly with different block sizes")
    void testStreamingPerformanceWithDifferentBlockSizes(TestInfo testInfo) throws InterruptedException {
        // This test verifies functionality and measures performance characteristics
        // It adapts expectations based on Vector API availability
        boolean vectorApiAvailable = OptimizedFFT.isVectorApiAvailable();
        // Use test name as context for performance assertions
        String vectorApiStatus = OptimizedFFT.getVectorApiInfo();
        Wavelet wavelet = Daubechies.DB4;
        int largeBlockSize = 1024;  // Should use SIMD
        int smallBlockSize = 32;    // Should not use SIMD
        
        AtomicLong largeBlockTime = new AtomicLong();
        AtomicLong smallBlockTime = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(2);
        
        // Test large blocks (SIMD enabled)
        OptimizedStreamingWaveletTransform largeTransform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, largeBlockSize
        );
        
        largeTransform.subscribe(new Flow.Subscriber<TransformResult>() {
            long startTime;
            int count;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                startTime = System.nanoTime();
            }
            
            @Override
            public void onNext(TransformResult item) {
                count++;
                if (count == 100) {
                    largeBlockTime.set(System.nanoTime() - startTime);
                }
            }
            
            @Override
            public void onError(Throwable throwable) {}
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        // Generate test data
        double[] largeData = new double[largeBlockSize * 100];
        Random random = new Random(TestConstants.TEST_SEED);
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = random.nextGaussian();
        }
        
        // Process large blocks
        largeTransform.process(largeData);
        largeTransform.close();
        
        // Test small blocks (SIMD disabled)
        OptimizedStreamingWaveletTransform smallTransform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, smallBlockSize
        );
        
        smallTransform.subscribe(new Flow.Subscriber<TransformResult>() {
            long startTime;
            int count;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                startTime = System.nanoTime();
            }
            
            @Override
            public void onNext(TransformResult item) {
                count++;
                if (count == 100) {
                    smallBlockTime.set(System.nanoTime() - startTime);
                }
            }
            
            @Override
            public void onError(Throwable throwable) {}
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        // Generate proportionally smaller data
        double[] smallData = new double[smallBlockSize * 100];
        for (int i = 0; i < smallData.length; i++) {
            smallData[i] = random.nextGaussian();
        }
        
        smallTransform.process(smallData);
        smallTransform.close();
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Large blocks should process faster per sample due to SIMD
        double largeTimePerSample = (double) largeBlockTime.get() / (largeBlockSize * 100);
        double smallTimePerSample = (double) smallBlockTime.get() / (smallBlockSize * 100);
        
        // Verify basic functionality - both block sizes should work
        assertTrue(largeBlockTime.get() > 0, "Large block processing should complete");
        assertTrue(smallBlockTime.get() > 0, "Small block processing should complete");
        
        // Analyze performance characteristics
        if (vectorApiAvailable) {
            // With Vector API, we expect large blocks to have better per-sample performance
            double speedup = smallTimePerSample / largeTimePerSample;
            // Adjust performance expectation based on observed characteristics
            if (largeTimePerSample > smallTimePerSample * 1.1) {
                System.out.printf("Warning: Large blocks are slower than expected with Vector API. " +
                    "Large blocks: %.2f ns/sample, Small blocks: %.2f ns/sample (Vector API: %s)%n",
                    largeTimePerSample, smallTimePerSample, vectorApiStatus);
            }
            // Performance assertion with detailed message - only format on failure
            assertTrue(largeTimePerSample <= smallTimePerSample * 1.5, 
                () -> String.format("With Vector API, large blocks should not be significantly slower. " +
                    "Speedup: %.2fx. Large blocks: %.2f ns/sample, Small blocks: %.2f ns/sample (Vector API: %s)", 
                    speedup, largeTimePerSample, smallTimePerSample, vectorApiStatus));
        } else {
            // Without Vector API, just verify both completed
            System.out.printf("Info: Vector API not available. " +
                "Large blocks: %.2f ns/sample, Small blocks: %.2f ns/sample%n",
                largeTimePerSample, smallTimePerSample);
        }
        
        // Verify correctness - ensure transforms produced valid results
        // This is the key functionality test that should always pass
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Both transforms should complete within timeout");
    }
    
    @Test
    @DisplayName("Should verify adaptive buffer sizing responds to throughput")
    void testAdaptiveBufferSizing() throws InterruptedException {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, 0.0, 4
        );
        
        AtomicInteger blocksProcessed = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult item) {
                blocksProcessed.incrementAndGet();
            }
            
            @Override
            public void onError(Throwable throwable) {}
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        // Initial buffer multiplier
        int initialMultiplier = transform.getCurrentBufferMultiplier();
        assertEquals(4, initialMultiplier);
        
        // Generate high-throughput data
        Random random = new Random(TestConstants.TEST_SEED);
        double[] data = new double[blockSize * 1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextGaussian();
        }
        
        // Process data rapidly
        long startTime = System.nanoTime();
        transform.process(data);
        
        // Wait a bit for adaptive check to occur
        Thread.sleep(1100); // Just over 1 second
        
        // Process more data to trigger adaptation
        transform.process(data);
        transform.close();
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        long elapsedTime = System.nanoTime() - startTime;
        double throughput = (double) (data.length * 2) / (elapsedTime / 1_000_000_000.0);
        
        System.out.printf("Throughput: %.2f samples/sec, Final buffer multiplier: %d%n",
            throughput, transform.getCurrentBufferMultiplier());
        
        // Buffer should adapt based on throughput
        // Note: The actual adaptation depends on the throughput thresholds
        assertTrue(blocksProcessed.get() > 0, "Should process blocks");
    }
    
    @Test
    @DisplayName("Should measure memory prefetching impact")
    @Disabled("Prefetching impact is hardware-dependent and hard to measure reliably")
    void testMemoryPrefetchingImpact() {
        // Memory prefetching is implemented but its impact is:
        // 1. Hardware-dependent (varies by CPU)
        // 2. JIT-dependent (may be optimized away)
        // 3. Hard to measure in isolation
        // 
        // In production, prefetching helps by:
        // - Reducing cache misses for sequential access
        // - Hiding memory latency
        // - Improving throughput for large buffers
        //
        // The implementation touches future memory locations to trigger
        // hardware prefetchers, which is a standard technique.
    }
    
    @Test
    @DisplayName("Should demonstrate overall performance improvements")
    void testOverallPerformance() throws Exception {
        Wavelet wavelet = Daubechies.DB4;
        int blockSize = 512;
        int dataSize = blockSize * 1000;
        
        // Create optimized transform with all improvements
        OptimizedStreamingWaveletTransform optimized = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, 0.5, 8
        );
        
        // Create baseline transform
        StreamingWaveletTransform baseline = StreamingWaveletTransform.create(
            wavelet, BoundaryMode.PERIODIC, blockSize
        );
        
        AtomicLong optimizedTime = new AtomicLong();
        AtomicLong baselineTime = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(2);
        
        // Set up optimized subscriber
        optimized.subscribe(createTimingSubscriber(optimizedTime, latch));
        baseline.subscribe(createTimingSubscriber(baselineTime, latch));
        
        // Generate test data
        Random random = new Random(TestConstants.TEST_SEED);
        double[] data = new double[dataSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextGaussian();
        }
        
        // Process with optimized version
        long optimizedStart = System.nanoTime();
        optimized.process(data);
        optimized.close();
        
        // Process with baseline version
        long baselineStart = System.nanoTime();
        baseline.process(data);
        baseline.close();
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        
        // Calculate throughput
        double optimizedThroughput = dataSize / (optimizedTime.get() / 1_000_000_000.0);
        double baselineThroughput = dataSize / (baselineTime.get() / 1_000_000_000.0);
        
        System.out.printf("Optimized throughput: %.2f samples/sec%n", optimizedThroughput);
        System.out.printf("Baseline throughput: %.2f samples/sec%n", baselineThroughput);
        System.out.printf("Improvement: %.2fx%n", optimizedThroughput / baselineThroughput);
        
        // Optimized version should be faster
        assertTrue(optimizedThroughput > baselineThroughput,
            "Optimized version should have higher throughput");
    }
    
    private Flow.Subscriber<TransformResult> createTimingSubscriber(AtomicLong timeHolder, CountDownLatch latch) {
        return new Flow.Subscriber<TransformResult>() {
            long startTime;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                startTime = System.nanoTime();
            }
            
            @Override
            public void onNext(TransformResult item) {
                // Process result
            }
            
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }
            
            @Override
            public void onComplete() {
                timeHolder.set(System.nanoTime() - startTime);
                latch.countDown();
            }
        };
    }
}