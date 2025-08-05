package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for ThreadLocalManager to ensure proper lifecycle management.
 */
class ThreadLocalManagerTest {
    
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
    void testBasicThreadLocalCreation() {
        // Create a managed ThreadLocal
        ThreadLocalManager.ManagedThreadLocal<String> local = 
            ThreadLocalManager.withInitial(() -> "initial");
        
        // Verify initial value
        assertEquals("initial", local.get());
        
        // Set a new value
        local.set("updated");
        assertEquals("updated", local.get());
        
        // Clean up
        ThreadLocalManager.cleanupCurrentThread();
        
        // After cleanup, should get initial value again
        assertEquals("initial", local.get());
    }
    
    @Test
    void testCleanupScope() throws Exception {
        AtomicBoolean cleanupCalled = new AtomicBoolean(false);
        
        ThreadLocalManager.ManagedThreadLocal<String> local = 
            ThreadLocalManager.withInitial(() -> "test");
        
        // Use try-with-resources
        try (ThreadLocalManager.CleanupScope scope = ThreadLocalManager.createScope()) {
            local.set("in scope");
            assertEquals("in scope", local.get());
            assertFalse(scope.isClosed());
        }
        
        // After scope, ThreadLocal should be cleaned
        assertEquals("test", local.get()); // Back to initial value
        assertTrue(ThreadLocalManager.isCleanupPerformed());
    }
    
    @Test
    void testMultipleThreadLocals() {
        // Create multiple ThreadLocals
        ThreadLocalManager.ManagedThreadLocal<Integer> intLocal = 
            ThreadLocalManager.withInitial(() -> 0);
        ThreadLocalManager.ManagedThreadLocal<String> stringLocal = 
            ThreadLocalManager.withInitial(() -> "");
        ThreadLocalManager.ManagedThreadLocal<Double> doubleLocal = 
            ThreadLocalManager.withInitial(() -> 0.0);
        
        // Set values
        intLocal.set(42);
        stringLocal.set("hello");
        doubleLocal.set(3.14);
        
        // Get stats before cleanup
        ThreadLocalManager.ThreadLocalStats stats = ThreadLocalManager.getStats();
        assertTrue(stats.activeCount() >= 3);
        assertFalse(stats.cleanupPerformed());
        
        // Clean up all
        ThreadLocalManager.cleanupCurrentThread();
        
        // Verify all are reset
        assertEquals(0, intLocal.get());
        assertEquals("", stringLocal.get());
        assertEquals(0.0, doubleLocal.get());
        
        // Check stats after cleanup
        stats = ThreadLocalManager.getStats();
        assertTrue(stats.cleanupPerformed());
    }
    
    @Test
    void testThreadPoolScenario() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger leakCount = new AtomicInteger(0);
        
        ThreadLocalManager.ManagedThreadLocal<byte[]> memoryLocal = 
            ThreadLocalManager.withInitial(() -> new byte[1024]); // 1KB per thread
        
        try {
            // Submit tasks that use ThreadLocal
            for (int i = 0; i < 10; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        // Simulate work with ThreadLocal
                        byte[] data = memoryLocal.get();
                        data[0] = (byte) taskId;
                        
                        // Proper cleanup
                        ThreadLocalManager.cleanupCurrentThread();
                        
                        // Check for leaks after cleanup
                        ThreadLocalManager.ThreadLocalStats stats = ThreadLocalManager.getStats();
                        if (stats.hasPotentialLeak()) {
                            leakCount.incrementAndGet();
                        }
                        
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all tasks
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            
            // No leaks should be detected with proper cleanup
            assertEquals(0, leakCount.get());
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
    
    @Test
    void testWithCleanupUtility() throws Exception {
        ThreadLocalManager.ManagedThreadLocal<String> local = 
            ThreadLocalManager.withInitial(() -> "default");
        
        String result = ThreadLocalManager.withCleanup(() -> {
            local.set("temporary");
            return local.get();
        });
        
        assertEquals("temporary", result);
        
        // After withCleanup, ThreadLocal should be cleaned
        assertEquals("default", local.get());
    }
    
    @Test
    void testMemoryLeakDetection() {
        ThreadLocalManager.ManagedThreadLocal<byte[]> memoryHog = 
            ThreadLocalManager.withInitial(() -> new byte[1024 * 1024]); // 1MB
        
        // Access to create value
        assertNotNull(memoryHog.get());
        
        // Check stats - should show potential leak
        ThreadLocalManager.ThreadLocalStats stats = ThreadLocalManager.getStats();
        assertTrue(stats.activeCount() > 0);
        assertFalse(stats.cleanupPerformed());
        assertTrue(stats.hasPotentialLeak());
        
        // Clean up
        ThreadLocalManager.cleanupCurrentThread();
        
        // No leak after cleanup
        stats = ThreadLocalManager.getStats();
        assertTrue(stats.cleanupPerformed());
        assertFalse(stats.hasPotentialLeak());
    }
    
    @Test
    void testNestedScopes() {
        ThreadLocalManager.ManagedThreadLocal<Integer> counter = 
            ThreadLocalManager.withInitial(() -> 0);
        
        try (ThreadLocalManager.CleanupScope outer = ThreadLocalManager.createScope()) {
            counter.set(1);
            
            try (ThreadLocalManager.CleanupScope inner = ThreadLocalManager.createScope()) {
                counter.set(2);
                assertEquals(2, counter.get());
            }
            
            // After inner scope, value should be reset
            assertEquals(0, counter.get());
            
            // Set again in outer scope
            counter.set(3);
        }
        
        // After outer scope, completely reset
        assertEquals(0, counter.get());
    }
    
    @Test
    void testHasValueForCurrentThread() {
        ThreadLocalManager.ManagedThreadLocal<String> local = 
            ThreadLocalManager.withInitial(() -> null);
        
        // Initially no value (null initial value)
        assertFalse(local.hasValueForCurrentThread());
        
        // Set a value
        local.set("value");
        assertTrue(local.hasValueForCurrentThread());
        
        // Remove value
        local.remove();
        assertFalse(local.hasValueForCurrentThread());
    }
    
    @Test
    void testStatsSummary() {
        // Create some ThreadLocals
        for (int i = 0; i < 5; i++) {
            final int index = i;
            ThreadLocalManager.withInitial(() -> "test" + index);
        }
        
        ThreadLocalManager.ThreadLocalStats stats = ThreadLocalManager.getStats();
        String summary = stats.summary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("registered"));
        assertTrue(summary.contains("active"));
        assertTrue(summary.contains("cleanup"));
    }
}