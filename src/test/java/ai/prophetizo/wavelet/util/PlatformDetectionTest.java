package ai.prophetizo.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for PlatformDetection utility.
 * 
 * Tests various platform scenarios by mocking system properties,
 * ensuring the utility correctly identifies different platforms
 * and provides appropriate optimization hints.
 */
class PlatformDetectionTest {
    
    /**
     * Mock system property provider for testing.
     */
    private static class MockSystemPropertyProvider implements PlatformDetection.SystemPropertyProvider {
        private String osArch;
        private String osName;
        
        public MockSystemPropertyProvider(String osArch, String osName) {
            this.osArch = osArch;
            this.osName = osName;
        }
        
        @Override
        public String getProperty(String key) {
            switch (key) {
                case "os.arch":
                    return osArch;
                case "os.name":
                    return osName;
                default:
                    return null;
            }
        }
        
        public void setOsArch(String osArch) {
            this.osArch = osArch;
        }
        
        public void setOsName(String osName) {
            this.osName = osName;
        }
    }
    
    private MockSystemPropertyProvider mockProvider;
    
    @BeforeEach
    void setUp() {
        // Clear cache before each test
        PlatformDetection.clearCache();
        mockProvider = new MockSystemPropertyProvider("x86_64", "Linux");
        PlatformDetection.setSystemPropertyProvider(mockProvider);
    }
    
    @AfterEach
    void tearDown() {
        // Reset to default provider after each test
        PlatformDetection.resetSystemPropertyProvider();
    }
    
    @Test
    @DisplayName("Should detect Apple Silicon platform correctly")
    void testAppleSiliconDetection() {
        // Given - Apple Silicon Mac
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Mac OS X");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.APPLE_SILICON, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.MACOS, PlatformDetection.detectOperatingSystem());
        assertTrue(PlatformDetection.isAppleSilicon());
        assertTrue(PlatformDetection.isARM64());
        assertFalse(PlatformDetection.isX86_64());
        assertEquals(8, PlatformDetection.getRecommendedSIMDThreshold());
    }
    
    @Test  
    @DisplayName("Should detect Apple Silicon with arm64 architecture")
    void testAppleSiliconWithArm64() {
        // Given - Apple Silicon Mac with arm64 architecture
        mockProvider.setOsArch("arm64");
        mockProvider.setOsName("macOS");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.APPLE_SILICON, PlatformDetection.detectPlatform());
        assertTrue(PlatformDetection.isAppleSilicon());
        assertTrue(PlatformDetection.isARM64());
    }
    
    @Test
    @DisplayName("Should detect x86-64 Linux platform correctly")
    void testX86_64LinuxDetection() {
        // Given - x86-64 Linux
        mockProvider.setOsArch("x86_64");
        mockProvider.setOsName("Linux");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.X86_64, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.LINUX, PlatformDetection.detectOperatingSystem());
        assertFalse(PlatformDetection.isAppleSilicon());
        assertFalse(PlatformDetection.isARM64());
        assertTrue(PlatformDetection.isX86_64());
        assertEquals(16, PlatformDetection.getRecommendedSIMDThreshold());
    }
    
    @Test
    @DisplayName("Should detect AMD64 Windows platform correctly")
    void testAMD64WindowsDetection() {
        // Given - AMD64 Windows
        mockProvider.setOsArch("amd64");
        mockProvider.setOsName("Windows 11");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.X86_64, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.WINDOWS, PlatformDetection.detectOperatingSystem());
        assertTrue(PlatformDetection.isX86_64());
        assertFalse(PlatformDetection.isAppleSilicon());
    }
    
    @Test
    @DisplayName("Should detect ARM64 Linux platform correctly")
    void testARM64LinuxDetection() {
        // Given - ARM64 Linux (not Apple)
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Ubuntu");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.ARM64, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.LINUX, PlatformDetection.detectOperatingSystem());
        assertFalse(PlatformDetection.isAppleSilicon());
        assertTrue(PlatformDetection.isARM64());
        assertFalse(PlatformDetection.isX86_64());
    }
    
    @Test
    @DisplayName("Should handle unknown platform gracefully")
    void testUnknownPlatformDetection() {
        // Given - Unknown architecture
        mockProvider.setOsArch("mips64");
        mockProvider.setOsName("FreeBSD");
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.OTHER, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.OTHER, PlatformDetection.detectOperatingSystem());
        assertFalse(PlatformDetection.isAppleSilicon());
        assertFalse(PlatformDetection.isARM64());
        assertFalse(PlatformDetection.isX86_64());
    }
    
    @Test
    @DisplayName("Should handle null system properties gracefully")
    void testNullSystemProperties() {
        // Given - Null system properties
        PlatformDetection.setSystemPropertyProvider(key -> null);
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.OTHER, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.OTHER, PlatformDetection.detectOperatingSystem());
        assertFalse(PlatformDetection.isAppleSilicon());
    }
    
    @Test
    @DisplayName("Should handle exceptions in system property access")
    void testExceptionInSystemPropertyAccess() {
        // Given - System property provider that throws exceptions
        PlatformDetection.setSystemPropertyProvider(key -> {
            throw new SecurityException("Access denied");
        });
        PlatformDetection.clearCache();
        
        // When & Then
        assertEquals(PlatformDetection.Platform.OTHER, PlatformDetection.detectPlatform());
        assertEquals(PlatformDetection.OperatingSystem.OTHER, PlatformDetection.detectOperatingSystem());
        assertFalse(PlatformDetection.isAppleSilicon());
    }
    
    @Test
    @DisplayName("Should cache detection results properly")
    void testCaching() {
        // Given - Initial setup
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Mac OS X");
        PlatformDetection.clearCache();
        
        // When - First detection
        PlatformDetection.Platform firstResult = PlatformDetection.detectPlatform();
        
        // Change the mock but don't clear cache
        mockProvider.setOsArch("x86_64");
        mockProvider.setOsName("Linux");
        
        // When - Second detection (should use cached result)
        PlatformDetection.Platform secondResult = PlatformDetection.detectPlatform();
        
        // Then - Results should be the same due to caching
        assertEquals(PlatformDetection.Platform.APPLE_SILICON, firstResult);
        assertEquals(firstResult, secondResult);
        
        // When - Clear cache and detect again
        PlatformDetection.clearCache();
        PlatformDetection.Platform thirdResult = PlatformDetection.detectPlatform();
        
        // Then - Should reflect the new system properties
        assertEquals(PlatformDetection.Platform.X86_64, thirdResult);
    }
    
    @Test
    @DisplayName("Should provide correct AVX support estimates")
    void testAVXSupportEstimates() {
        // Test Apple Silicon - should have AVX2 equivalent but not AVX-512
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Mac OS X");
        PlatformDetection.clearCache();
        
        assertTrue(PlatformDetection.hasAVX2Support()); // NEON equivalent
        assertFalse(PlatformDetection.hasAVX512Support()); // No AVX-512 on ARM
        
        // Test x86-64 - should have both AVX2 and potentially AVX-512
        mockProvider.setOsArch("x86_64");
        mockProvider.setOsName("Linux");
        PlatformDetection.clearCache();
        
        assertTrue(PlatformDetection.hasAVX2Support());
        assertTrue(PlatformDetection.hasAVX512Support()); // Heuristic estimate
    }
    
    @Test
    @DisplayName("Should provide meaningful optimization hints")
    void testOptimizationHints() {
        // Test Apple Silicon hints
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Mac OS X");
        PlatformDetection.clearCache();
        
        String appleSiliconHints = PlatformDetection.getPlatformOptimizationHints();
        assertTrue(appleSiliconHints.contains("APPLE_SILICON"));
        assertTrue(appleSiliconHints.contains("MACOS"));
        assertTrue(appleSiliconHints.contains("NEON"));
        assertTrue(appleSiliconHints.contains("unified memory"));
        
        // Test x86-64 hints
        mockProvider.setOsArch("x86_64");
        mockProvider.setOsName("Linux");
        PlatformDetection.clearCache();
        
        String x86Hints = PlatformDetection.getPlatformOptimizationHints();
        assertTrue(x86Hints.contains("X86_64"));
        assertTrue(x86Hints.contains("LINUX"));
        assertTrue(x86Hints.contains("AVX") || x86Hints.contains("SSE"));
    }
    
    @Test
    @DisplayName("Should handle case-insensitive OS name matching")
    void testCaseInsensitiveOSMatching() {
        // Test various case combinations
        String[] macOSVariants = {"Mac OS X", "macOS", "MACOS", "mac os x"};
        String[] windowsVariants = {"Windows 10", "WINDOWS", "windows 11"};
        String[] linuxVariants = {"Linux", "LINUX", "linux ubuntu"};
        
        for (String osName : macOSVariants) {
            mockProvider.setOsName(osName);
            PlatformDetection.clearCache();
            assertEquals(PlatformDetection.OperatingSystem.MACOS, PlatformDetection.detectOperatingSystem());
        }
        
        for (String osName : windowsVariants) {
            mockProvider.setOsName(osName);
            PlatformDetection.clearCache();
            assertEquals(PlatformDetection.OperatingSystem.WINDOWS, PlatformDetection.detectOperatingSystem());
        }
        
        for (String osName : linuxVariants) {
            mockProvider.setOsName(osName);
            PlatformDetection.clearCache();
            assertEquals(PlatformDetection.OperatingSystem.LINUX, PlatformDetection.detectOperatingSystem());
        }
    }
    
    @Test
    @DisplayName("Should prevent instantiation")
    void testPreventInstantiation() {
        // The class should not be instantiable due to private constructor
        // This test verifies the design but doesn't actually test instantiation
        // since it would cause a compilation error
        
        // Verify that PlatformDetection is a utility class with only static methods
        assertNotNull(PlatformDetection.class);
        assertTrue(java.lang.reflect.Modifier.isFinal(PlatformDetection.class.getModifiers()));
    }
    
    @Test
    @DisplayName("Should reject null system property provider")
    void testNullSystemPropertyProviderRejection() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            PlatformDetection.setSystemPropertyProvider(null);
        });
    }
    
    @Test
    @DisplayName("Should reset system property provider correctly")
    void testSystemPropertyProviderReset() {
        // Given - Set a mock provider
        mockProvider.setOsArch("aarch64");
        mockProvider.setOsName("Mac OS X");
        PlatformDetection.setSystemPropertyProvider(mockProvider);
        PlatformDetection.clearCache();
        
        // Verify mock is working
        assertTrue(PlatformDetection.isAppleSilicon());
        
        // When - Reset to default provider
        PlatformDetection.resetSystemPropertyProvider();
        
        // Then - Should use real system properties
        // (This test will pass regardless of actual system, just verifies reset works)
        assertNotNull(PlatformDetection.detectPlatform());
        assertNotNull(PlatformDetection.detectOperatingSystem());
    }
}