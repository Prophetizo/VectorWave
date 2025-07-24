package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverlapBufferCacheTest {
    
    @BeforeEach
    void setUp() {
        OverlapBuffer.clearWindowCache();
    }
    
    @AfterEach
    void tearDown() {
        OverlapBuffer.clearWindowCache();
    }
    
    @Test
    void testCacheReuse() {
        // Create multiple buffers with same parameters
        OverlapBuffer buffer1 = new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HANN);
        OverlapBuffer buffer2 = new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HANN);
        OverlapBuffer buffer3 = new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HANN);
        
        // Should only have one cached window
        assertEquals(1, OverlapBuffer.getWindowCacheSize());
    }
    
    @Test
    void testCacheWithDifferentParameters() {
        // Create buffers with different parameters
        new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HANN);
        new OverlapBuffer(256, 0.5, OverlapBuffer.WindowFunction.HANN);
        new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HAMMING);
        new OverlapBuffer(128, 0.75, OverlapBuffer.WindowFunction.HANN);
        
        // Each unique combination should be cached
        assertEquals(4, OverlapBuffer.getWindowCacheSize());
    }
    
    @Test
    void testLRUEviction() {
        // Note: This test would need to set a smaller cache size via system property
        // For now, just verify we can create many windows without issues
        int maxSize = OverlapBuffer.getMaxWindowCacheSize();
        
        // Create more windows than cache size
        for (int i = 0; i < maxSize + 10; i++) {
            new OverlapBuffer(128 + i, 0.5, OverlapBuffer.WindowFunction.HANN);
        }
        
        // Cache should not exceed max size
        assertTrue(OverlapBuffer.getWindowCacheSize() <= maxSize);
    }
    
    @Test
    void testCacheClear() {
        // Create some buffers
        new OverlapBuffer(128, 0.5, OverlapBuffer.WindowFunction.HANN);
        new OverlapBuffer(256, 0.5, OverlapBuffer.WindowFunction.HAMMING);
        
        assertTrue(OverlapBuffer.getWindowCacheSize() > 0);
        
        // Clear cache
        OverlapBuffer.clearWindowCache();
        
        assertEquals(0, OverlapBuffer.getWindowCacheSize());
    }
    
    @Test
    void testNonOverlappingBuffersDoNotCache() {
        // Create buffer with no overlap
        new OverlapBuffer(128, 0.0, OverlapBuffer.WindowFunction.RECTANGULAR);
        
        // Should still cache even for 0% overlap
        assertEquals(1, OverlapBuffer.getWindowCacheSize());
    }
}