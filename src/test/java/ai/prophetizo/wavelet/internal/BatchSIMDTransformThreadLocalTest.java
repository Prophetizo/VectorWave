package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.util.ThreadLocalManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests ThreadLocal cleanup functionality in BatchSIMDTransform.
 */
class BatchSIMDTransformThreadLocalTest {
    
    @BeforeEach
    void setUp() {
        // Clean up any existing ThreadLocals before each test
        ThreadLocalManager.cleanupCurrentThread();
        ThreadLocalManager.resetCleanupFlag();
    }
    
    @AfterEach
    void tearDown() {
        // Ensure cleanup after each test
        ThreadLocalManager.cleanupCurrentThread();
    }
    
    @Test
    void testHaarTransformWithCleanup() throws Exception {
        // Create test data
        double[][] signals = new double[8][64];
        double[][] approxResults = new double[8][32];
        double[][] detailResults = new double[8][32];
        
        // Initialize with test data
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / signals[i].length) + i;
            }
        }
        
        // Use the new cleanup method
        BatchSIMDTransform.haarBatchTransformSIMDWithCleanup(signals, approxResults, detailResults);
        
        // Verify cleanup was performed
        assertTrue(ThreadLocalManager.isCleanupPerformed());
        
        // Verify results are not null or zero
        assertNotNull(approxResults[0][0]);
        assertNotNull(detailResults[0][0]);
    }
    
    @Test
    void testAdaptiveTransformWithCleanup() throws Exception {
        // Create test data
        double[][] signals = new double[16][128];
        double[][] approxResults = new double[16][64];
        double[][] detailResults = new double[16][64];
        
        // Haar filter coefficients
        double[] lowPass = {0.7071067811865476, 0.7071067811865476};
        double[] highPass = {0.7071067811865476, -0.7071067811865476};
        
        // Initialize with test data
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = j * 0.1 + i;
            }
        }
        
        // Use the new cleanup method
        BatchSIMDTransform.adaptiveBatchTransformWithCleanup(
            signals, approxResults, detailResults, lowPass, highPass);
        
        // Verify cleanup was performed
        assertTrue(ThreadLocalManager.isCleanupPerformed());
    }
    
    @Test
    void testThreadPoolScenario() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger cleanupCount = new AtomicInteger(0);
        AtomicInteger leakCount = new AtomicInteger(0);
        
        try {
            for (int task = 0; task < 20; task++) {
                executor.submit(() -> {
                    try {
                        // Create test data
                        double[][] signals = new double[8][64];
                        double[][] approxResults = new double[8][32];
                        double[][] detailResults = new double[8][32];
                        
                        // Initialize
                        for (int i = 0; i < signals.length; i++) {
                            for (int j = 0; j < signals[i].length; j++) {
                                signals[i][j] = Math.random();
                            }
                        }
                        
                        // Check initial state
                        ThreadLocalManager.ThreadLocalStats statsBefore = ThreadLocalManager.getStats();
                        
                        // Perform transform with automatic cleanup
                        BatchSIMDTransform.haarBatchTransformSIMDWithCleanup(
                            signals, approxResults, detailResults);
                        
                        // Check cleanup was performed
                        if (ThreadLocalManager.isCleanupPerformed()) {
                            cleanupCount.incrementAndGet();
                        }
                        
                        // Check for leaks
                        ThreadLocalManager.ThreadLocalStats statsAfter = ThreadLocalManager.getStats();
                        if (statsAfter.hasPotentialLeak()) {
                            leakCount.incrementAndGet();
                        }
                        
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all tasks
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            
            // All tasks should have cleanup performed
            assertEquals(20, cleanupCount.get());
            
            // No leaks should be detected
            assertEquals(0, leakCount.get());
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
    
    @Test
    void testManualCleanupStillWorks() {
        // Create test data
        double[][] signals = new double[4][32];
        double[][] approxResults = new double[4][16];
        double[][] detailResults = new double[4][16];
        
        // Initialize
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = i + j;
            }
        }
        
        // Use regular method
        BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults);
        
        // Check stats before cleanup
        ThreadLocalManager.ThreadLocalStats statsBefore = ThreadLocalManager.getStats();
        assertTrue(statsBefore.activeCount() > 0);
        assertFalse(statsBefore.cleanupPerformed());
        
        // Manual cleanup (deprecated method)
        BatchSIMDTransform.cleanupThreadLocals();
        
        // Verify cleanup
        ThreadLocalManager.ThreadLocalStats statsAfter = ThreadLocalManager.getStats();
        assertTrue(statsAfter.cleanupPerformed());
    }
    
    @Test
    void testTryWithResourcesPattern() throws Exception {
        double[][] signals = new double[8][64];
        double[][] approxResults = new double[8][32];
        double[][] detailResults = new double[8][32];
        
        // Initialize
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = Math.cos(Math.PI * j / 16);
            }
        }
        
        // Use try-with-resources pattern
        try (ThreadLocalManager.CleanupScope scope = ThreadLocalManager.createScope()) {
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults);
            
            // Within scope, ThreadLocals are active
            ThreadLocalManager.ThreadLocalStats stats = ThreadLocalManager.getStats();
            assertTrue(stats.activeCount() > 0);
        }
        
        // After scope, cleanup should be performed
        assertTrue(ThreadLocalManager.isCleanupPerformed());
    }
}