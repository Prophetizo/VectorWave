package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for thread safety of StreamingRingBuffer with ThreadLocal buffers.
 */
class ThreadSafeRingBufferTest {
    
    private ExecutorService executor;
    
    @AfterEach
    void cleanup() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Test
    @DisplayName("Should provide separate buffers for each thread using getWindowDirect")
    void testThreadLocalWindowBuffers() throws InterruptedException {
        StreamingRingBuffer buffer = new StreamingRingBuffer(256, 64, 32);
        
        // Fill buffer with test data
        double[] testData = new double[128];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        buffer.write(testData, 0, testData.length);
        
        // Track unique buffer instances seen by each thread
        Set<Integer> bufferIdentities = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        
        // Create multiple threads that will call getWindowDirect
        executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    // Each thread should get its own buffer
                    double[] window = buffer.getWindowDirect();
                    assertNotNull(window);
                    
                    // Record the identity of this buffer
                    bufferIdentities.add(System.identityHashCode(window));
                    
                    // Verify we can read the data correctly
                    assertEquals(0.0, window[0]);
                    assertEquals(63.0, window[63]);
                    
                    // Clean up ThreadLocal resources
                    buffer.cleanupThread();
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(doneLatch.await(2, TimeUnit.SECONDS));
        
        // Each thread should have gotten a different buffer instance
        assertEquals(4, bufferIdentities.size(), 
            "Expected 4 unique buffer instances, but got: " + bufferIdentities.size());
    }
    
    @Test
    @DisplayName("Should handle concurrent processWindow calls safely")
    void testConcurrentProcessWindow() throws InterruptedException {
        StreamingRingBuffer buffer = new StreamingRingBuffer(1024, 128, 64);
        AtomicInteger processedWindows = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Pre-fill buffer with enough data for multiple windows
        double[] testData = new double[512];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        buffer.write(testData, 0, testData.length);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        
        executor = Executors.newFixedThreadPool(4);
        
        // Define a processor that verifies data integrity
        StreamingRingBuffer.WindowProcessor processor = (data, offset, length) -> {
            try {
                assertEquals(128, length);
                assertEquals(0, offset);
                
                // Verify the window contains expected sequential values
                double firstValue = data[0];
                for (int i = 1; i < length; i++) {
                    if (data[i] != firstValue + i) {
                        errors.incrementAndGet();
                        return;
                    }
                }
                
                processedWindows.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
            }
        };
        
        // Launch multiple threads processing windows concurrently
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Each thread tries to process a window
                    boolean processed = buffer.processWindow(processor);
                    if (!processed) {
                        // It's ok if some threads don't get a window
                        // due to concurrent advancement
                    }
                    
                    // Clean up ThreadLocal resources
                    buffer.cleanupThread();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        assertTrue(doneLatch.await(2, TimeUnit.SECONDS));
        
        // Verify no errors occurred
        assertEquals(0, errors.get(), "Concurrent processing caused errors");
        
        // At least some windows should have been processed
        assertTrue(processedWindows.get() > 0, "No windows were processed");
    }
    
    @Test
    @DisplayName("Should reuse ThreadLocal buffers within same thread")
    void testBufferReuse() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(256, 64, 32);
        
        // Fill buffer
        double[] testData = new double[128];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        buffer.write(testData, 0, testData.length);
        
        // Get window multiple times in same thread
        double[] window1 = buffer.getWindowDirect();
        double[] window2 = buffer.getWindowDirect();
        
        // Should be the same buffer instance (ThreadLocal reuse)
        assertSame(window1, window2, "ThreadLocal should reuse buffer within same thread");
        
        // Verify data is still correct
        assertEquals(0.0, window2[0]);
        assertEquals(63.0, window2[63]);
    }
    
    @Test
    @DisplayName("Should handle cleanup correctly")
    void testThreadLocalCleanup() throws InterruptedException {
        StreamingRingBuffer buffer = new StreamingRingBuffer(256, 64, 32);
        
        // Fill buffer
        double[] testData = new double[128];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        buffer.write(testData, 0, testData.length);
        
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger identityBefore = new AtomicInteger();
        AtomicInteger identityAfter = new AtomicInteger();
        
        Thread thread = new Thread(() -> {
            // Get buffer before cleanup
            double[] window1 = buffer.getWindowDirect();
            identityBefore.set(System.identityHashCode(window1));
            
            // Clean up ThreadLocal
            buffer.cleanupThread();
            
            // Get buffer after cleanup - should be a new instance
            double[] window2 = buffer.getWindowDirect();
            identityAfter.set(System.identityHashCode(window2));
            
            done.countDown();
        });
        
        thread.start();
        assertTrue(done.await(1, TimeUnit.SECONDS));
        
        // After cleanup, should get a new buffer instance
        assertNotEquals(identityBefore.get(), identityAfter.get(),
            "After cleanup, ThreadLocal should provide a new buffer instance");
    }
}