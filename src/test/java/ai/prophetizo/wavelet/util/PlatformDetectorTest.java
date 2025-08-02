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
    
    @Test
    @DisplayName("Should detect operating system")
    void testOperatingSystemDetection() {
        PlatformDetector.OperatingSystem os = PlatformDetector.getOperatingSystem();
        assertNotNull(os);
        // At least verify it's one of the known OS types
        assertTrue(os == PlatformDetector.OperatingSystem.MACOS ||
                  os == PlatformDetector.OperatingSystem.WINDOWS ||
                  os == PlatformDetector.OperatingSystem.LINUX ||
                  os == PlatformDetector.OperatingSystem.OTHER);
    }
    
    @Test
    @DisplayName("Should provide AVX support information")
    void testAVXSupport() {
        // These are heuristics, so we just verify they return reasonable values
        boolean hasAVX2 = PlatformDetector.hasAVX2Support();
        boolean hasAVX512 = PlatformDetector.hasAVX512Support();
        
        // If platform has AVX-512, it should also have AVX2
        if (hasAVX512) {
            assertTrue(hasAVX2, "Platform with AVX-512 should also support AVX2");
        }
        
        // ARM platforms shouldn't report AVX-512 support
        if (PlatformDetector.isARM() || PlatformDetector.isAppleSilicon()) {
            assertFalse(hasAVX512, "ARM platforms should not report AVX-512 support");
        }
    }
    
    @Test
    @DisplayName("Should provide reasonable SIMD threshold")
    void testSIMDThreshold() {
        int threshold = PlatformDetector.getRecommendedSIMDThreshold();
        assertTrue(threshold > 0, "SIMD threshold should be positive");
        assertTrue(threshold <= 64, "SIMD threshold should be reasonable");
        
        // Platform-specific checks
        if (PlatformDetector.isAppleSilicon() || PlatformDetector.isARM()) {
            assertEquals(8, threshold, "ARM platforms should have lower SIMD threshold");
        } else if (PlatformDetector.isX86_64()) {
            assertEquals(16, threshold, "x86-64 should have higher SIMD threshold");
        }
    }
    
    @Test
    @DisplayName("Should provide optimization hints")
    void testOptimizationHints() {
        String hints = PlatformDetector.getPlatformOptimizationHints();
        assertNotNull(hints);
        assertFalse(hints.isEmpty());
        
        // Should contain basic information
        assertTrue(hints.contains("Platform:"));
        assertTrue(hints.contains("OS:"));
        assertTrue(hints.contains("SIMD Threshold:"));
        assertTrue(hints.contains("Cache:"));
        
        // Platform-specific content
        if (PlatformDetector.isAppleSilicon()) {
            assertTrue(hints.contains("NEON"));
            assertTrue(hints.contains("unified memory"));
        } else if (PlatformDetector.isX86_64()) {
            assertTrue(hints.contains("SSE"));
        }
    }
}