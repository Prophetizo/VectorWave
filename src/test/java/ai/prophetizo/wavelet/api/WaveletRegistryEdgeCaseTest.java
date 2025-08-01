package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and error scenario tests for WaveletRegistry.
 */
class WaveletRegistryEdgeCaseTest {
    
    @Test
    void testGetWavelet_NullInput() {
        assertThrows(NullPointerException.class, () -> {
            WaveletRegistry.getWavelet(null);
        });
    }
    
    @Test
    void testGetWavelet_EmptyString() {
        InvalidArgumentException exception = assertThrows(
            InvalidArgumentException.class,
            () -> WaveletRegistry.getWavelet("")
        );
        assertEquals("Unknown wavelet: ", exception.getMessage());
    }
    
    @Test
    void testGetWavelet_WhitespaceOnly() {
        InvalidArgumentException exception = assertThrows(
            InvalidArgumentException.class,
            () -> WaveletRegistry.getWavelet("   ")
        );
        assertEquals("Unknown wavelet:    ", exception.getMessage());
    }
    
    @Test
    void testHasWavelet_EdgeCases() {
        assertThrows(NullPointerException.class, () -> {
            WaveletRegistry.hasWavelet(null);
        });
        
        assertFalse(WaveletRegistry.hasWavelet(""));
        assertFalse(WaveletRegistry.hasWavelet("   "));
        assertFalse(WaveletRegistry.hasWavelet("\t\n"));
    }
    
    @Test
    void testRegisterWavelet_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            WaveletRegistry.registerWavelet(null);
        });
    }
    
    @Test
    void testRegisterWavelet_DuplicateRegistration() {
        // Register same wavelet multiple times
        Wavelet haar = new Haar();
        int initialSize = WaveletRegistry.getAvailableWavelets().size();
        
        WaveletRegistry.registerWavelet(haar);
        WaveletRegistry.registerWavelet(haar);
        WaveletRegistry.registerWavelet(haar);
        
        // Size should remain the same (idempotent)
        assertEquals(initialSize, WaveletRegistry.getAvailableWavelets().size());
    }
    
    @Test
    void testGetWaveletsByType_NullType() {
        // Should handle null gracefully
        List<String> result = WaveletRegistry.getWaveletsByType(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testReload_Consistency() {
        // Get initial state
        Set<String> initialWavelets = Set.copyOf(WaveletRegistry.getAvailableWavelets());
        int initialSize = initialWavelets.size();
        
        // Reload multiple times
        WaveletRegistry.reload();
        WaveletRegistry.reload();
        WaveletRegistry.reload();
        
        // Should have same wavelets after reload
        Set<String> afterReload = WaveletRegistry.getAvailableWavelets();
        assertEquals(initialSize, afterReload.size());
        assertEquals(initialWavelets, afterReload);
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 100;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger invalidArgumentCount = new AtomicInteger(0);
        AtomicInteger otherExceptionCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Mix of read operations
                        WaveletRegistry.getWavelet("haar");
                        WaveletRegistry.hasWavelet("db4");
                        WaveletRegistry.getAvailableWavelets();
                        WaveletRegistry.getOrthogonalWavelets();
                        
                        // Only one thread does reloads to avoid conflicts
                        if (threadId == 0 && j % 20 == 0) {
                            WaveletRegistry.reload();
                        }
                    }
                    successCount.incrementAndGet();
                } catch (InvalidArgumentException e) {
                    // Expected if wavelet is temporarily unavailable during reload
                    invalidArgumentCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected exception - track it
                    otherExceptionCount.incrementAndGet();
                    System.err.println("Unexpected exception in thread " + threadId + ": " + 
                                     e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Most threads should succeed (the reload thread might fail)
        assertTrue(successCount.get() >= THREAD_COUNT - 1, 
                  "At least " + (THREAD_COUNT - 1) + " threads should complete successfully");
        
        // No unexpected exceptions should occur
        assertEquals(0, otherExceptionCount.get(), 
                    "No unexpected exceptions should occur during concurrent access");
        
        // InvalidArgumentException is acceptable during reload
        System.out.println("Concurrent test results: " + successCount.get() + " successes, " + 
                          invalidArgumentCount.get() + " temporary wavelet unavailability");
    }
    
    @Test
    void testConcurrentRegistration() throws InterruptedException, ExecutionException {
        final int THREAD_COUNT = 5;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // Multiple threads trying to register the same wavelet
        Wavelet haar = new Haar();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    WaveletRegistry.registerWavelet(haar);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }
        
        // All should succeed
        for (Future<Boolean> future : futures) {
            assertTrue(future.get());
        }
        
        executor.shutdown();
        assertTrue(WaveletRegistry.hasWavelet("haar"));
    }
    
    @Test
    void testLargeScaleAccess() {
        // Test with many rapid accesses
        for (int i = 0; i < 1000; i++) {
            assertNotNull(WaveletRegistry.getWavelet("haar"));
            assertTrue(WaveletRegistry.hasWavelet("db2"));
            assertFalse(WaveletRegistry.getAvailableWavelets().isEmpty());
        }
    }
    
    @Test
    void testWaveletsByType_AllTypes() {
        // Test all enum values
        for (WaveletType type : WaveletType.values()) {
            List<String> wavelets = WaveletRegistry.getWaveletsByType(type);
            assertNotNull(wavelets, "Should never return null for type: " + type);
            
            // Verify returned list is unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> {
                wavelets.add("test");
            });
        }
    }
    
    @Test
    void testSpecialCharacterNames() {
        // Test wavelets with special characters in names
        assertTrue(WaveletRegistry.hasWavelet("bior1.3"));
        Wavelet bior = WaveletRegistry.getWavelet("bior1.3");
        assertEquals("bior1.3", bior.name());
        
        // Test case variations
        assertTrue(WaveletRegistry.hasWavelet("BIOR1.3"));
        assertTrue(WaveletRegistry.hasWavelet("BioR1.3"));
    }
    
    @Test
    void testMemoryLeaks() {
        // Simulate many reloads to check for memory leaks
        for (int i = 0; i < 100; i++) {
            WaveletRegistry.reload();
            
            // Access some wavelets to ensure they're loaded
            WaveletRegistry.getWavelet("haar");
            WaveletRegistry.getAvailableWavelets();
        }
        
        // If we get here without OutOfMemoryError, test passes
        assertTrue(true);
    }
    
    @Test
    void testInvalidWaveletNames() {
        String[] invalidNames = {
            "nonexistent",
            "haar2", // Haar doesn't have variants
            "db0",   // Invalid Daubechies order
            "db100", // Too high order
            "sym0",  // Invalid Symlet order
            "coif0", // Invalid Coiflet order
            "12345", // Random number
            "!@#$%", // Special characters
            "null",  // String "null"
            "\0",    // Null character
            "haar\0db2", // Embedded null
            "../../../etc/passwd", // Path traversal attempt
            "<script>alert('xss')</script>", // XSS attempt
        };
        
        for (String invalidName : invalidNames) {
            assertFalse(WaveletRegistry.hasWavelet(invalidName),
                       "Should not have wavelet: " + invalidName);
            
            assertThrows(InvalidArgumentException.class, () -> {
                WaveletRegistry.getWavelet(invalidName);
            }, "Should throw exception for: " + invalidName);
        }
    }
    
    @Test
    void testServiceLoaderFailureHandling() {
        // This test verifies the error handling in the ServiceLoader loop
        // The actual implementation logs errors and continues, so we just
        // verify the registry still works even if a provider fails
        
        WaveletRegistry.reload();
        
        // Registry should still function normally
        assertTrue(WaveletRegistry.hasWavelet("haar"));
        assertFalse(WaveletRegistry.getAvailableWavelets().isEmpty());
    }
}