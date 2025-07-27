package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformDetector utility class.
 */
class PlatformDetectorTest {
    
    @Test
    @DisplayName("Should detect valid platform")
    void testPlatformDetection() {
        PlatformDetector.Platform platform = PlatformDetector.getPlatform();
        assertNotNull(platform);
        assertNotEquals(PlatformDetector.Platform.UNKNOWN, platform, 
            "Platform should be detected on this system");
    }
    
    @Test
    @DisplayName("Should return cache information")
    void testCacheInfo() {
        PlatformDetector.CacheInfo cacheInfo = PlatformDetector.getCacheInfo();
        assertNotNull(cacheInfo);
        assertTrue(cacheInfo.l1DataCacheSize() > 0, "L1 cache size should be positive");
        assertTrue(cacheInfo.l2CacheSize() > 0, "L2 cache size should be positive");
        assertTrue(cacheInfo.cacheLineSize() > 0, "Cache line size should be positive");
    }
    
    @Test
    @DisplayName("Should provide platform-specific checks")
    void testPlatformChecks() {
        // At least one of these should be true unless we're on an unknown platform
        if (PlatformDetector.getPlatform() != PlatformDetector.Platform.UNKNOWN) {
            boolean hasValidPlatform = PlatformDetector.isAppleSilicon() || 
                                     PlatformDetector.isX86_64() || 
                                     PlatformDetector.isARM();
            assertTrue(hasValidPlatform, "Should detect at least one valid platform type");
        }
    }
    
    @Test
    @DisplayName("Should provide human-readable description")
    void testDescription() {
        String description = PlatformDetector.getDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty());
        assertTrue(description.contains("Platform:"));
        assertTrue(description.contains("L1 Cache:"));
        assertTrue(description.contains("L2 Cache:"));
    }
    
    @Test
    @DisplayName("Cache info constants should have reasonable values")
    void testCacheInfoConstants() {
        // Test Apple Silicon cache info
        assertEquals(128 * 1024, PlatformDetector.CacheInfo.APPLE_SILICON.l1DataCacheSize());
        assertEquals(4 * 1024 * 1024, PlatformDetector.CacheInfo.APPLE_SILICON.l2CacheSize());
        assertEquals(64, PlatformDetector.CacheInfo.APPLE_SILICON.cacheLineSize());
        
        // Test x86-64 cache info
        assertEquals(32 * 1024, PlatformDetector.CacheInfo.X86_64.l1DataCacheSize());
        assertEquals(256 * 1024, PlatformDetector.CacheInfo.X86_64.l2CacheSize());
        assertEquals(64, PlatformDetector.CacheInfo.X86_64.cacheLineSize());
        
        // Test ARM default cache info
        assertEquals(64 * 1024, PlatformDetector.CacheInfo.ARM_DEFAULT.l1DataCacheSize());
        assertEquals(1024 * 1024, PlatformDetector.CacheInfo.ARM_DEFAULT.l2CacheSize());
        assertEquals(64, PlatformDetector.CacheInfo.ARM_DEFAULT.cacheLineSize());
    }
}