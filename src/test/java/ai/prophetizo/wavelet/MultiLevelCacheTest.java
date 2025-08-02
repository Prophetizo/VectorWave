package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiLevelTransformResult cache management functionality.
 * Verifies that cache clearing works correctly and doesn't affect correctness.
 */
class MultiLevelCacheTest {
    
    @Test
    @DisplayName("Cache clearing should free memory without affecting correctness")
    void testCacheClearingFunctionality() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 0.5 * Math.cos(4 * Math.PI * i / 32.0);
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 4);
        
        // Access some approximations to populate cache
        double[] approx1Before = result.approximationAtLevel(1);
        double[] approx2Before = result.approximationAtLevel(2);
        double[] approx3Before = result.approximationAtLevel(3);
        
        // Check cache memory usage
        long memoryBefore = result.getCacheMemoryUsage();
        assertTrue(memoryBefore > 0, "Cache should contain data after accessing approximations");
        
        // Clear cache
        result.clearCache();
        
        // Verify cache was cleared
        long memoryAfter = result.getCacheMemoryUsage();
        assertEquals(0, memoryAfter, "Cache memory should be 0 after clearing");
        
        // Verify we can still access approximations correctly
        double[] approx1After = result.approximationAtLevel(1);
        double[] approx2After = result.approximationAtLevel(2);
        double[] approx3After = result.approximationAtLevel(3);
        
        // Results should be identical
        assertArrayEquals(approx1Before, approx1After, 1e-14, 
            "Approximation at level 1 should be identical after cache clear");
        assertArrayEquals(approx2Before, approx2After, 1e-14,
            "Approximation at level 2 should be identical after cache clear");
        assertArrayEquals(approx3Before, approx3After, 1e-14,
            "Approximation at level 3 should be identical after cache clear");
    }
    
    @Test
    @DisplayName("Cache memory usage should be calculated correctly")
    void testCacheMemoryCalculation() {
        double[] signal = new double[128]; // Small signal for predictable sizes
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i;
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB2);
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Initially, cache should be empty (except final approximation)
        assertEquals(0, result.getCacheMemoryUsage(), "Initial cache should be empty");
        
        // Access level 1 approximation (64 coefficients)
        result.approximationAtLevel(1);
        long memory1 = result.getCacheMemoryUsage();
        assertEquals(64 * 8, memory1, "Level 1 cache should be 64 doubles * 8 bytes");
        
        // Access level 2 approximation (32 coefficients)
        result.approximationAtLevel(2);
        long memory2 = result.getCacheMemoryUsage();
        assertEquals((64 + 32) * 8, memory2, "Cache should contain levels 1 and 2");
        
        // Clear and verify
        result.clearCache();
        assertEquals(0, result.getCacheMemoryUsage(), "Cache should be empty after clearing");
    }
    
    @Test
    @DisplayName("Thread safety of cache operations")
    void testCacheThreadSafety() throws InterruptedException {
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 5);
        
        // Create multiple threads that access and clear cache concurrently
        Thread[] threads = new Thread[10];
        for (int t = 0; t < threads.length; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    if (threadId % 2 == 0) {
                        // Even threads access approximations
                        for (int level = 1; level <= 4; level++) {
                            result.approximationAtLevel(level);
                        }
                    } else {
                        // Odd threads clear cache
                        result.clearCache();
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify result is still correct
        double[] finalApprox = result.finalApproximation();
        assertNotNull(finalApprox);
        assertTrue(finalApprox.length > 0);
        
        // Verify we can still reconstruct
        double[] reconstructed = mwt.reconstruct(result);
        assertArrayEquals(signal, reconstructed, 1e-10,
            "Reconstruction should work correctly after concurrent access");
    }
    
    @Test
    @DisplayName("Final approximation should never be cleared from cache")
    void testFinalApproximationRetention() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Access final approximation
        double[] finalBefore = result.finalApproximation();
        double[] approxAtDeepestBefore = result.approximationAtLevel(3);
        
        // Clear cache multiple times
        result.clearCache();
        result.clearCache();
        
        // Final approximation should still be accessible and identical
        double[] finalAfter = result.finalApproximation();
        double[] approxAtDeepestAfter = result.approximationAtLevel(3);
        
        assertArrayEquals(finalBefore, finalAfter, 0.0,
            "Final approximation should be unchanged after cache clear");
        assertArrayEquals(approxAtDeepestBefore, approxAtDeepestAfter, 0.0,
            "Approximation at deepest level should be unchanged");
    }
    
    @Test
    @DisplayName("isCacheCleared flag should work correctly")
    void testCacheClearedFlag() {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.1;
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 2);
        
        // Cast to implementation to access the flag
        if (result instanceof MultiLevelTransformResultImpl impl) {
            assertFalse(impl.isCacheCleared(), "Cache should not be marked as cleared initially");
            
            result.clearCache();
            
            assertTrue(impl.isCacheCleared(), "Cache should be marked as cleared after clearCache()");
        }
    }
}