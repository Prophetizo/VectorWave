package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SynchronizedP2QuantileEstimator.
 */
@DisplayName("SynchronizedP2QuantileEstimator")
class SynchronizedP2QuantileEstimatorTest {
    
    private SynchronizedP2QuantileEstimator estimator;
    
    @BeforeEach
    void setUp() {
        estimator = new SynchronizedP2QuantileEstimator(0.5);
    }
    
    @Test
    @DisplayName("Constructor validation")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> new SynchronizedP2QuantileEstimator(-0.1));
        assertThrows(IllegalArgumentException.class, 
            () -> new SynchronizedP2QuantileEstimator(1.1));
        
        // Valid cases
        assertDoesNotThrow(() -> new SynchronizedP2QuantileEstimator(0.0));
        assertDoesNotThrow(() -> new SynchronizedP2QuantileEstimator(1.0));
        assertDoesNotThrow(() -> new SynchronizedP2QuantileEstimator(0.25));
        assertDoesNotThrow(() -> new SynchronizedP2QuantileEstimator(0.75));
    }
    
    @Test
    @DisplayName("Factory method for median")
    void testForMedian() {
        SynchronizedP2QuantileEstimator median = SynchronizedP2QuantileEstimator.forMedian();
        assertNotNull(median);
        assertEquals(0, median.getCount());
        
        // Add some data and verify it estimates median
        median.update(1.0);
        median.update(2.0);
        median.update(3.0);
        median.update(4.0);
        median.update(5.0);
        
        double estimate = median.getQuantile();
        assertTrue(estimate > 2.0 && estimate < 4.0); // Should be around 3.0
    }
    
    @Test
    @DisplayName("Basic functionality")
    void testBasicFunctionality() {
        // Initially empty
        assertEquals(0, estimator.getCount());
        assertEquals(0.0, estimator.getQuantile());
        
        // Add single value
        estimator.update(5.0);
        assertEquals(1, estimator.getCount());
        assertEquals(5.0, estimator.getQuantile());
        
        // Add more values
        for (int i = 1; i <= 10; i++) {
            estimator.update(i);
        }
        assertEquals(11, estimator.getCount()); // 5.0 + 10 more values
        
        // Median of [1,2,3,4,5,5,6,7,8,9,10] should be around 5
        double median = estimator.getQuantile();
        assertTrue(median > 4.0 && median < 6.0);
    }
    
    @Test
    @DisplayName("Reset functionality")
    void testReset() {
        // Add data
        for (int i = 0; i < 100; i++) {
            estimator.update(i);
        }
        
        assertTrue(estimator.getCount() > 0);
        assertTrue(estimator.getQuantile() > 0);
        
        // Reset
        estimator.reset();
        
        assertEquals(0, estimator.getCount());
        assertEquals(0.0, estimator.getQuantile());
        
        // Should work normally after reset
        estimator.update(10.0);
        assertEquals(1, estimator.getCount());
        assertEquals(10.0, estimator.getQuantile());
    }
    
    @Test
    @DisplayName("Thread safety - concurrent updates")
    void testThreadSafetyConcurrentUpdates() throws InterruptedException {
        final int numThreads = 10;
        final int updatesPerThread = 1000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try {
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();
                        
                        // Each thread adds different range of values
                        int base = threadId * updatesPerThread;
                        for (int j = 0; j < updatesPerThread; j++) {
                            estimator.update(base + j);
                        }
                        
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(endLatch.await(10, TimeUnit.SECONDS));
            
            // Verify all threads completed successfully
            assertEquals(numThreads, successCount.get());
            
            // Verify count is correct
            assertEquals(numThreads * updatesPerThread, estimator.getCount());
            
            // Verify quantile is reasonable (should be around middle of range)
            double quantile = estimator.getQuantile();
            assertTrue(quantile > 0);
            assertTrue(quantile < numThreads * updatesPerThread);
        } finally {
            executor.shutdown();
        }
    }
    
    @Test
    @DisplayName("Thread safety - mixed operations")
    void testThreadSafetyMixedOperations() throws InterruptedException {
        final int numThreads = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numThreads);
        final List<Exception> exceptions = new ArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try {
            // Mix of update, getQuantile, getCount, and reset operations
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        Random random = new Random(threadId);
                        for (int j = 0; j < 100; j++) {
                            int operation = random.nextInt(4);
                            switch (operation) {
                                case 0:
                                    estimator.update(random.nextDouble() * 100);
                                    break;
                                case 1:
                                    double q = estimator.getQuantile();
                                    assertTrue(q >= 0);
                                    break;
                                case 2:
                                    long count = estimator.getCount();
                                    assertTrue(count >= 0);
                                    break;
                                case 3:
                                    if (threadId == 0 && j == 50) { // Only one reset
                                        estimator.reset();
                                    }
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            // Start all threads
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(endLatch.await(10, TimeUnit.SECONDS));
            
            // No exceptions should have occurred
            assertTrue(exceptions.isEmpty(), 
                "Exceptions occurred: " + exceptions);
            
            // Estimator should still be in valid state
            assertTrue(estimator.getCount() >= 0);
            assertTrue(estimator.getQuantile() >= 0);
        } finally {
            executor.shutdown();
        }
    }
    
    @Test
    @DisplayName("Different quantiles")
    void testDifferentQuantiles() {
        // Test 25th percentile
        SynchronizedP2QuantileEstimator q25 = new SynchronizedP2QuantileEstimator(0.25);
        
        // Test 75th percentile
        SynchronizedP2QuantileEstimator q75 = new SynchronizedP2QuantileEstimator(0.75);
        
        // Add same data to both
        for (int i = 1; i <= 100; i++) {
            q25.update(i);
            q75.update(i);
        }
        
        double estimate25 = q25.getQuantile();
        double estimate75 = q75.getQuantile();
        
        // 25th percentile should be less than 75th percentile
        assertTrue(estimate25 < estimate75);
        
        // Rough checks
        assertTrue(estimate25 > 15 && estimate25 < 35); // Around 25
        assertTrue(estimate75 > 65 && estimate75 < 85); // Around 75
    }
    
    @Test
    @DisplayName("Edge cases")
    void testEdgeCases() {
        // Min quantile (0.0)
        SynchronizedP2QuantileEstimator minEstimator = new SynchronizedP2QuantileEstimator(0.0);
        for (int i = 1; i <= 10; i++) {
            minEstimator.update(i);
        }
        // With only 10 values and quantile 0.0, the algorithm may return values up to the 2nd element
        assertTrue(minEstimator.getQuantile() <= 3.0); // Should be close to minimum
        
        // Max quantile (1.0)
        SynchronizedP2QuantileEstimator maxEstimator = new SynchronizedP2QuantileEstimator(1.0);
        for (int i = 1; i <= 10; i++) {
            maxEstimator.update(i);
        }
        // With only 10 values and quantile 1.0, the algorithm may return values from the 8th element
        assertTrue(maxEstimator.getQuantile() >= 7.0); // Should be close to maximum
    }
}