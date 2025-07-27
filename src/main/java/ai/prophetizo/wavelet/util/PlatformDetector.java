package ai.prophetizo.wavelet.util;

/**
 * Utility class for detecting platform characteristics.
 * 
 * <p>Provides centralized platform detection logic to enable better testing
 * and maintainability across the wavelet transform library.</p>
 *
 * @since 1.0.0
 */
public final class PlatformDetector {
    
    /**
     * Platform types supported by the library.
     */
    public enum Platform {
        /** Apple Silicon (M1, M2, etc.) */
        APPLE_SILICON,
        /** x86-64 (Intel/AMD) */
        X86_64,
        /** ARM (non-Apple) */
        ARM,
        /** Unknown platform */
        UNKNOWN
    }
    
    /**
     * Cache configuration for the platform.
     */
    public record CacheInfo(
        int l1DataCacheSize,
        int l2CacheSize,
        int cacheLineSize
    ) {
        public static final CacheInfo APPLE_SILICON = new CacheInfo(128 * 1024, 4 * 1024 * 1024, 64);
        public static final CacheInfo X86_64 = new CacheInfo(32 * 1024, 256 * 1024, 64);
        public static final CacheInfo ARM_DEFAULT = new CacheInfo(64 * 1024, 1024 * 1024, 64);
        public static final CacheInfo DEFAULT = new CacheInfo(32 * 1024, 256 * 1024, 64);
    }
    
    private static final Platform CURRENT_PLATFORM = detectPlatform();
    private static final CacheInfo CACHE_INFO = detectCacheInfo();
    
    // Prevent instantiation
    private PlatformDetector() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Gets the detected platform type.
     * 
     * @return the current platform
     */
    public static Platform getPlatform() {
        return CURRENT_PLATFORM;
    }
    
    /**
     * Gets the cache configuration for the current platform.
     * 
     * @return cache information
     */
    public static CacheInfo getCacheInfo() {
        return CACHE_INFO;
    }
    
    /**
     * Checks if the current platform is Apple Silicon.
     * 
     * @return true if running on Apple Silicon
     */
    public static boolean isAppleSilicon() {
        return CURRENT_PLATFORM == Platform.APPLE_SILICON;
    }
    
    /**
     * Checks if the current platform is x86-64.
     * 
     * @return true if running on x86-64
     */
    public static boolean isX86_64() {
        return CURRENT_PLATFORM == Platform.X86_64;
    }
    
    /**
     * Checks if the current platform is ARM-based.
     * 
     * @return true if running on any ARM platform (including Apple Silicon)
     */
    public static boolean isARM() {
        return CURRENT_PLATFORM == Platform.APPLE_SILICON || CURRENT_PLATFORM == Platform.ARM;
    }
    
    /**
     * Detects the current platform.
     * 
     * <p>This method can be overridden for testing by setting system properties:
     * <ul>
     *   <li>{@code ai.prophetizo.test.platform} - Override platform detection</li>
     *   <li>{@code ai.prophetizo.test.arch} - Override architecture detection</li>
     *   <li>{@code ai.prophetizo.test.os} - Override OS detection</li>
     * </ul></p>
     * 
     * @return the detected platform
     */
    private static Platform detectPlatform() {
        // Check for test overrides first
        String testPlatform = System.getProperty("ai.prophetizo.test.platform");
        if (testPlatform != null) {
            try {
                return Platform.valueOf(testPlatform.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to normal detection
            }
        }
        
        // Get architecture and OS information
        String arch = System.getProperty("ai.prophetizo.test.arch", 
                                       System.getProperty("os.arch", "")).toLowerCase();
        String osName = System.getProperty("ai.prophetizo.test.os",
                                         System.getProperty("os.name", "")).toLowerCase();
        
        // Detect platform based on architecture and OS
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            if (osName.contains("mac") || osName.contains("darwin")) {
                return Platform.APPLE_SILICON;
            } else {
                return Platform.ARM;
            }
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            return Platform.X86_64;
        } else if (arch.contains("arm")) {
            return Platform.ARM;
        }
        
        return Platform.UNKNOWN;
    }
    
    /**
     * Detects cache configuration for the current platform.
     * 
     * <p>Cache sizes can be overridden using system properties:
     * <ul>
     *   <li>{@code ai.prophetizo.cache.l1.size} - L1 data cache size in bytes</li>
     *   <li>{@code ai.prophetizo.cache.l2.size} - L2 cache size in bytes</li>
     *   <li>{@code ai.prophetizo.cache.line.size} - Cache line size in bytes</li>
     * </ul></p>
     * 
     * @return cache configuration
     */
    private static CacheInfo detectCacheInfo() {
        // Check for property overrides
        Integer l1Size = getSystemPropertyInt("ai.prophetizo.cache.l1.size");
        Integer l2Size = getSystemPropertyInt("ai.prophetizo.cache.l2.size");
        Integer lineSize = getSystemPropertyInt("ai.prophetizo.cache.line.size");
        
        // If all properties are set, use them
        if (l1Size != null && l2Size != null && lineSize != null) {
            return new CacheInfo(l1Size, l2Size, lineSize);
        }
        
        // Otherwise, use platform defaults with property overrides
        CacheInfo platformDefault = switch (CURRENT_PLATFORM) {
            case APPLE_SILICON -> CacheInfo.APPLE_SILICON;
            case X86_64 -> CacheInfo.X86_64;
            case ARM -> CacheInfo.ARM_DEFAULT;
            case UNKNOWN -> CacheInfo.DEFAULT;
        };
        
        return new CacheInfo(
            l1Size != null ? l1Size : platformDefault.l1DataCacheSize,
            l2Size != null ? l2Size : platformDefault.l2CacheSize,
            lineSize != null ? lineSize : platformDefault.cacheLineSize
        );
    }
    
    /**
     * Gets an integer system property value.
     * 
     * @param key the property key
     * @return the integer value, or null if not set or invalid
     */
    private static Integer getSystemPropertyInt(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // Return null for invalid values
            }
        }
        return null;
    }
    
    /**
     * Gets a human-readable description of the current platform.
     * 
     * @return platform description
     */
    public static String getDescription() {
        return String.format("Platform: %s, L1 Cache: %d KB, L2 Cache: %d MB, Cache Line: %d bytes",
            CURRENT_PLATFORM,
            CACHE_INFO.l1DataCacheSize / 1024,
            CACHE_INFO.l2CacheSize / (1024 * 1024),
            CACHE_INFO.cacheLineSize
        );
    }
}