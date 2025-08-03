package ai.prophetizo.wavelet.modwt.streaming;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.test.TestConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for streaming MODWT transform functionality.
 * 
 * <p>Key differences from DWT streaming tests:</p>
 * <ul>
 *   <li>Tests arbitrary buffer sizes (not just power-of-2)</li>
 *   <li>Verifies shift-invariant properties in streaming context</li>
 *   <li>Tests overlap handling for continuity</li>
 * </ul>
 * 
 * @since 3.0.0
 */
class MODWTStreamingTransformTest {
    
    // Use seeded Random for reproducible test results
    private static final Random random = new Random(TestConstants.TEST_SEED);
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void testBasicStreaming() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 100)) {  // Non-power-of-2 buffer
            
            CountDownLatch latch = new CountDownLatch(2); // Expect 2 buffers
            List<MODWTResult> results = new ArrayList<>();
            TestSubscriber<MODWTResult> subscriber = new TestSubscriber<>(results, latch);
            
            transform.subscribe(subscriber);
            
            // Process 200 samples in chunks
            double[] chunk = new double[50];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < chunk.length; j++) {
                    chunk[j] = Math.sin(2 * Math.PI * (i * 50 + j) / 25.0);
                }
                transform.process(chunk);
            }
            
            // Close is handled by try-with-resources
            assertTrue(latch.await(1, TimeUnit.SECONDS), "Timeout waiting for results");
            
            // Verify no errors occurred
            subscriber.assertNoError();
            
            assertEquals(2, results.size(), "Should emit 2 buffers");
            
            // Verify all results are same length as buffer
            for (MODWTResult result : results) {
                assertEquals(100, result.getSignalLength());
                assertEquals(100, result.approximationCoeffs().length);
                assertEquals(100, result.detailCoeffs().length);
            }
            
            // Verify statistics
            var stats = transform.getStatistics();
            assertEquals(200, stats.getSamplesProcessed());
            assertEquals(2, stats.getBlocksProcessed());
            assertTrue(stats.getAverageProcessingTimeNanos() > 0);
        }
    }
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void testSingleSampleProcessing() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                Daubechies.DB2, BoundaryMode.PERIODIC, 75)) {  // Non-power-of-2
            
            CountDownLatch latch = new CountDownLatch(1);
            List<MODWTResult> results = new ArrayList<>();
            TestSubscriber<MODWTResult> subscriber = new TestSubscriber<>(results, latch);
            
            transform.subscribe(subscriber);
            
            // Process samples one by one
            for (int i = 0; i < 75; i++) {
                transform.processSample(i * 0.1);
            }
            
            // Close is handled by try-with-resources
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            
            // Verify no errors occurred
            subscriber.assertNoError();
            
            assertEquals(1, results.size());
            assertEquals(75, transform.getStatistics().getSamplesProcessed());
        }
    }
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void testFlush() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 100)) {
            
            CountDownLatch latch = new CountDownLatch(1);
            List<MODWTResult> results = new ArrayList<>();
            TestSubscriber<MODWTResult> subscriber = new TestSubscriber<>(results, latch);
            
            transform.subscribe(subscriber);
            
            // Process partial buffer
            double[] chunk = new double[30];
            for (int i = 0; i < chunk.length; i++) {
                chunk[i] = i;
            }
            transform.process(chunk);
            
            // Should not emit yet
            assertEquals(0, results.size());
            
            // Flush should emit partial buffer
            transform.flush();
            
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(1, results.size());
            
            // Result should still be full buffer size (padded with zeros)
            assertEquals(100, results.get(0).getSignalLength());
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {50, 100, 150, 200, 256, 500, 1000})
    void testVariousBufferSizes(int bufferSize) throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, bufferSize)) {
            
            CountDownLatch latch = new CountDownLatch(1);
            List<MODWTResult> results = new ArrayList<>();
            TestSubscriber<MODWTResult> subscriber = new TestSubscriber<>(results, latch);
            
            transform.subscribe(subscriber);
            
            // Process exactly one buffer worth of data
            double[] data = new double[bufferSize];
            for (int i = 0; i < bufferSize; i++) {
                data[i] = random.nextGaussian();
            }
            transform.process(data);
            
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(1, results.size());
            assertEquals(bufferSize, results.get(0).getSignalLength());
        }
    }
    
    @Test
    void testInvalidBufferSize() {
        assertThrows(InvalidArgumentException.class,
            () -> MODWTStreamingTransform.create(new Haar(), BoundaryMode.PERIODIC, 0));
        
        assertThrows(InvalidArgumentException.class,
            () -> MODWTStreamingTransform.create(new Haar(), BoundaryMode.PERIODIC, -10));
    }
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void testMultipleSubscribers() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 64)) {
            
            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);
            
            List<MODWTResult> results1 = new ArrayList<>();
            List<MODWTResult> results2 = new ArrayList<>();
            
            TestSubscriber<MODWTResult> subscriber1 = new TestSubscriber<>(results1, latch1);
            TestSubscriber<MODWTResult> subscriber2 = new TestSubscriber<>(results2, latch2);
            
            transform.subscribe(subscriber1);
            transform.subscribe(subscriber2);
            
            // Process data
            double[] data = new double[64];
            transform.process(data);
            
            assertTrue(latch1.await(1, TimeUnit.SECONDS));
            assertTrue(latch2.await(1, TimeUnit.SECONDS));
            
            assertEquals(1, results1.size());
            assertEquals(1, results2.size());
        }
    }
    
    @Test
    @SuppressWarnings("try")  // close() may throw InterruptedException
    void testReset() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 50)) {
            
            // Process some data
            double[] data = new double[30];
            transform.process(data);
            
            assertEquals(30, transform.getBufferLevel());
            assertEquals(30, transform.getStatistics().getSamplesProcessed());
            
            // Reset
            transform.reset();
            
            assertEquals(0, transform.getBufferLevel());
            assertEquals(0, transform.getStatistics().getSamplesProcessed());
        }
    }
    
    @Test
    @Timeout(5)
    void testConcurrentProcessing() throws Exception {
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
                new Haar(), BoundaryMode.PERIODIC, 128)) {
            
            AtomicInteger resultCount = new AtomicInteger(0);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            transform.subscribe(new Flow.Subscriber<MODWTResult>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(MODWTResult item) {
                    resultCount.incrementAndGet();
                }
                
                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                }
                
                @Override
                public void onComplete() {}
            });
            
            // Process from multiple threads
            Thread[] threads = new Thread[4];
            for (int t = 0; t < threads.length; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    double[] chunk = new double[32];
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < chunk.length; j++) {
                            chunk[j] = threadId * 1000 + i * 32 + j;
                        }
                        transform.process(chunk);
                    }
                });
                threads[t].start();
            }
            
            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Verify results
            assertNull(error.get(), "No errors should occur");
            assertEquals(10, resultCount.get(), "Should emit 10 results (1280 samples / 128 buffer)");
        }
    }
    
    /**
     * Test subscriber for collecting results.
     */
    private static class TestSubscriber<T> implements Flow.Subscriber<T> {
        private final List<T> results;
        private final CountDownLatch latch;
        private volatile Throwable error;
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
            error = throwable;
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            // Transform completion
        }
        
        void assertNoError() {
            if (error != null) {
                fail("Subscriber received error: " + error.getMessage(), error);
            }
        }
    }
}