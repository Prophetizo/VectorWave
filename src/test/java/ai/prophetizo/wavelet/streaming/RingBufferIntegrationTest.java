package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform.StreamingStatistics;
import ai.prophetizo.wavelet.TransformResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating ring buffer usage in streaming components.
 */
class RingBufferIntegrationTest {
    
    @Test
    @DisplayName("Should process streaming data without array copying")
    void testStreamingWithoutCopying() {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        
        // Create optimized streaming transform
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize
        );
        
        // Track blocks processed
        AtomicInteger blocksProcessed = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        
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
                completionLatch.countDown();
            }
        });
        
        // Generate test data
        Random random = new Random(42);
        double[] data = new double[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextGaussian();
        }
        
        // Process data
        transform.process(data);
        
        // Close the transform to ensure all processing is complete
        transform.close();
        
        // Wait for completion signal
        try {
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS), "Transform did not complete in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        // Verify blocks were processed (may be 3 or 4 due to async processing)
        assertTrue(blocksProcessed.get() >= 3);
        
        // Check statistics
        StreamingStatistics stats = transform.getStatistics();
        assertEquals(1024, stats.getSamplesProcessed());
        assertTrue(stats.getBlocksEmitted() >= 3);
        assertTrue(stats.getAverageProcessingTime() > 0);
    }
    
    @Test
    @DisplayName("Should process streaming data with overlap")
    void testStreamingWithOverlap() {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        double overlapFactor = 0.5; // 50% overlap
        
        // Create optimized streaming transform with overlap
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, overlapFactor
        );
        
        // Track blocks processed
        AtomicInteger blocksProcessed = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        
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
                completionLatch.countDown();
            }
        });
        
        // Generate test data
        Random random = new Random(42);
        double[] data = new double[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextGaussian();
        }
        
        // Process data
        transform.process(data);
        
        // Close the transform to ensure all processing is complete
        transform.close();
        
        // Wait for completion signal
        try {
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS), "Transform did not complete in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        // With 50% overlap and 1024 samples:
        // First block: 0-255 (256 samples)
        // Second block: 128-383 (256 samples, 128 overlap)
        // Third block: 256-511 (256 samples, 128 overlap)
        // etc...
        // Expected: ~7 blocks (more than without overlap)
        assertTrue(blocksProcessed.get() >= 7, "Expected at least 7 blocks with 50% overlap, but got: " + blocksProcessed.get());
        
        // Check statistics
        StreamingStatistics stats = transform.getStatistics();
        assertEquals(1024, stats.getSamplesProcessed());
        assertTrue(stats.getBlocksEmitted() >= 7);
        assertTrue(stats.getAverageProcessingTime() > 0);
    }
    
    @Test
    @DisplayName("Should handle custom buffer capacity multiplier")
    void testCustomBufferCapacity() {
        Wavelet wavelet = new Haar();
        int blockSize = 128;
        double overlapFactor = 0.5;
        int bufferCapacityMultiplier = 8; // Larger buffer for testing
        
        // Create transform with custom buffer capacity
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, overlapFactor, bufferCapacityMultiplier
        );
        
        // The buffer should be able to handle more data without blocking
        AtomicInteger blocksProcessed = new AtomicInteger(0);
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
            public void onComplete() {}
        });
        
        // Generate test data - more than default buffer could handle
        double[] data = new double[blockSize * 6]; // 6 blocks worth
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        
        // Process all data at once
        transform.process(data);
        
        // Should be able to buffer all data without issues
        assertTrue(transform.getBufferLevel() > 0, "Buffer should contain data");
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should handle sliding window processing efficiently")
    void testSlidingWindowProcessing() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(1024, 128, 64);
        
        // Fill buffer with test pattern
        double[] testData = new double[512];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = Math.sin(2 * Math.PI * i / 128.0);
        }
        
        buffer.write(testData, 0, testData.length);
        
        // Process windows
        int windowCount = 0;
        double[] prevWindow = null;
        
        while (buffer.hasWindow()) {
            double[] window = buffer.getWindowDirect();
            assertNotNull(window);
            assertEquals(128, window.length);
            
            // Verify overlap between consecutive windows
            if (prevWindow != null) {
                // Last 64 samples of previous window should match first 64 of current
                for (int i = 0; i < 64; i++) {
                    assertEquals(prevWindow[64 + i], window[i], 1e-10);
                }
            }
            
            prevWindow = window.clone();
            buffer.advanceWindow();
            windowCount++;
        }
        
        // With 512 samples, window 128, hop 64: expect 7 complete windows
        assertEquals(7, windowCount);
    }
    
}