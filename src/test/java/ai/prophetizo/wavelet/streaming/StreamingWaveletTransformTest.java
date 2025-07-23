package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for streaming wavelet transform functionality.
 */
class StreamingWaveletTransformTest {
    
    @Test
    void testBasicStreaming() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 128)) {
            
            CountDownLatch latch = new CountDownLatch(2); // Expect 2 blocks
            List<TransformResult> results = new ArrayList<>();
            
            transform.subscribe(new TestSubscriber<>(results, latch));
            
            // Process 256 samples in chunks
            double[] chunk = new double[64];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < chunk.length; j++) {
                    chunk[j] = Math.sin(2 * Math.PI * (i * 64 + j) / 32.0);
                }
                transform.process(chunk);
            }
            
            // Close is handled by try-with-resources
            assertTrue(latch.await(1, TimeUnit.SECONDS), "Timeout waiting for results");
            
            assertEquals(2, results.size(), "Should emit 2 blocks");
            
            // Verify statistics
            var stats = transform.getStatistics();
            assertEquals(256, stats.getSamplesProcessed());
            assertEquals(2, stats.getBlocksEmitted());
            assertTrue(stats.getAverageProcessingTime() > 0);
        }
    }
    
    @Test
    void testSingleSampleProcessing() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                Daubechies.DB2, BoundaryMode.ZERO_PADDING, 64)) {
            
            CountDownLatch latch = new CountDownLatch(1);
            List<TransformResult> results = new ArrayList<>();
            
            transform.subscribe(new TestSubscriber<>(results, latch));
            
            // Process samples one by one
            for (int i = 0; i < 64; i++) {
                transform.process(i * 0.1);
            }
            
            // Close is handled by try-with-resources
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            
            assertEquals(1, results.size());
            assertEquals(64, transform.getStatistics().getSamplesProcessed());
        }
    }
    
    @Test
    void testFlush() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 128)) {
            
            CountDownLatch latch = new CountDownLatch(1);
            List<TransformResult> results = new ArrayList<>();
            
            transform.subscribe(new TestSubscriber<>(results, latch));
            
            // Process partial block
            double[] data = new double[100];
            for (int i = 0; i < data.length; i++) {
                data[i] = i;
            }
            transform.process(data);
            
            // Should not emit yet
            assertEquals(0, results.size());
            
            // Flush should emit the partial block
            transform.flush();
            
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(1, results.size());
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256, 512, 1024})
    void testVariousBlockSizes(int blockSize) throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, blockSize)) {
            
            AtomicInteger blockCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(1);
            
            transform.subscribe(new Flow.Subscriber<TransformResult>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(TransformResult item) {
                    blockCount.incrementAndGet();
                    assertEquals(blockSize / 2, item.approximationCoeffs().length);
                    assertEquals(blockSize / 2, item.detailCoeffs().length);
                    latch.countDown(); // Count down when we receive the block
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Unexpected error: " + throwable);
                    latch.countDown(); // Also count down on error to prevent hanging
                }
                
                @Override
                public void onComplete() {
                    // onComplete is only called when the transform is closed
                }
            });
            
            // Process exactly one block
            double[] data = new double[blockSize];
            for (int i = 0; i < data.length; i++) {
                data[i] = Math.random();
            }
            transform.process(data);
            
            // Wait for processing to complete
            assertTrue(latch.await(1, TimeUnit.SECONDS), "Processing did not complete in time");
            
            // Verify we got the block
            assertEquals(1, blockCount.get());
        }
    }
    
    @Test
    void testInvalidBlockSize() {
        assertThrows(InvalidArgumentException.class, () ->
            StreamingWaveletTransform.create(new Haar(), BoundaryMode.PERIODIC, 13));
        
        assertThrows(InvalidArgumentException.class, () ->
            StreamingWaveletTransform.create(new Haar(), BoundaryMode.PERIODIC, 8));
    }
    
    @Test
    void testBackpressure() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 64)) {
            
            AtomicInteger receivedCount = new AtomicInteger();
            CountDownLatch subscriptionLatch = new CountDownLatch(1);
            CountDownLatch receiveLatch = new CountDownLatch(1);
            
            transform.subscribe(new Flow.Subscriber<TransformResult>() {
                private Flow.Subscription subscription;
                
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscriptionLatch.countDown();
                    // Request only 1 item
                    subscription.request(1);
                }
                
                @Override
                public void onNext(TransformResult item) {
                    receivedCount.incrementAndGet();
                    receiveLatch.countDown();
                    // Don't request more
                }
                
                @Override
                public void onError(Throwable throwable) {}
                
                @Override
                public void onComplete() {}
            });
            
            assertTrue(subscriptionLatch.await(1, TimeUnit.SECONDS));
            
            // Send multiple blocks
            double[] block = new double[64];
            for (int i = 0; i < 3; i++) {
                transform.process(block);
            }
            
            // Wait for the first item to be received
            assertTrue(receiveLatch.await(1, TimeUnit.SECONDS), "Did not receive first item");
            
            // Should only receive 1 due to backpressure
            assertEquals(1, receivedCount.get());
        }
    }
    
    @Test
    @Timeout(2)
    void testConcurrentAccess() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 128)) {
            
            AtomicInteger totalBlocks = new AtomicInteger();
            CountDownLatch completeLatch = new CountDownLatch(1);
            CountDownLatch blocksLatch = new CountDownLatch(4); // Expect 4 blocks
            
            transform.subscribe(new Flow.Subscriber<TransformResult>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(TransformResult item) {
                    totalBlocks.incrementAndGet();
                    blocksLatch.countDown();
                }
                
                @Override
                public void onError(Throwable throwable) {
                    fail("Error: " + throwable);
                }
                
                @Override
                public void onComplete() {
                    completeLatch.countDown();
                }
            });
            
            // Multiple threads sending data
            Thread[] threads = new Thread[4];
            CountDownLatch startLatch = new CountDownLatch(1);
            
            for (int t = 0; t < threads.length; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    try {
                        startLatch.await();
                        double[] data = new double[32];
                        for (int i = 0; i < 4; i++) {
                            for (int j = 0; j < data.length; j++) {
                                data[j] = threadId * 1000 + i * 32 + j;
                            }
                            transform.process(data);
                        }
                    } catch (Exception e) {
                        fail("Thread error: " + e);
                    }
                });
                threads[t].start();
            }
            
            startLatch.countDown();
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Wait for all blocks to be processed
            assertTrue(blocksLatch.await(1, TimeUnit.SECONDS), "Did not receive all 4 blocks");
            
            transform.close();
            assertTrue(completeLatch.await(1, TimeUnit.SECONDS));
            
            // 4 threads * 4 chunks * 32 samples = 512 samples = 4 blocks of 128
            assertEquals(4, totalBlocks.get());
        }
    }
    
    @Test
    void testErrorHandling() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 64)) {
            
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch errorLatch = new CountDownLatch(1);
            
            transform.subscribe(new Flow.Subscriber<TransformResult>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(TransformResult item) {
                    // Force an error in processing
                    throw new RuntimeException("Test error");
                }
                
                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                    errorLatch.countDown();
                }
                
                @Override
                public void onComplete() {}
            });
            
            transform.process(new double[64]);
            
            assertTrue(errorLatch.await(1, TimeUnit.SECONDS));
            assertNotNull(error.get());
        }
    }
    
    @Test
    void testStatisticsAccuracy() throws Exception {
        try (StreamingWaveletTransform transform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 256)) {
            
            CountDownLatch latch = new CountDownLatch(4);
            
            transform.subscribe(new Flow.Subscriber<TransformResult>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(TransformResult item) {
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable throwable) {}
                
                @Override
                public void onComplete() {}
            });
            
            // Process 1024 samples = 4 blocks
            for (int i = 0; i < 1024; i++) {
                transform.process(i * 0.01);
            }
            
            // Wait for processing to complete
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            
            var stats = transform.getStatistics();
            assertEquals(1024, stats.getSamplesProcessed());
            assertEquals(4, stats.getBlocksEmitted());
            assertTrue(stats.getThroughput() > 0);
            assertTrue(stats.getMaxProcessingTime() >= stats.getAverageProcessingTime());
        }
    }
    
    /**
     * Helper test subscriber.
     */
    private static class TestSubscriber<T> implements Flow.Subscriber<T> {
        private final List<T> results;
        private final CountDownLatch latch;
        private Flow.Subscription subscription;
        
        TestSubscriber(List<T> results, CountDownLatch latch) {
            this.results = results;
            this.latch = latch;
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T item) {
            results.add(item);
            latch.countDown();
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }
        
        @Override
        public void onComplete() {}
    }
}