package ai.prophetizo.wavelet.util;

/**
 * Utility class for detecting platform-specific characteristics and optimizations.
 * 
 * <p>Centralizes platform detection logic to improve maintainability and enable
 * better testing of different platform scenarios. This utility provides methods
 * for detecting CPU architecture, operating system, and platform-specific
 * optimization capabilities.</p>
 * 
 * <p>This class replaces scattered platform detection code throughout the codebase
 * with a centralized, testable approach. It supports dependency injection of
 * system properties for testing purposes while providing sensible defaults
 * for production use.</p>
 *
 * @since 1.0.0
 */
public final class PlatformDetection {
    
    /**
     * Platform types supported by the library.
     */
    public enum Platform {
        /** Apple Silicon Macs (ARM64 architecture) */
        APPLE_SILICON,
        /** Intel/AMD x86-64 architecture */
        X86_64,
        /** ARM64 architecture on non-Apple systems */
        ARM64,
        /** Other/unknown platform */
        OTHER
    }
    
    /**
     * Operating system types.
     */
    public enum OperatingSystem {
        /** macOS */
        MACOS,
        /** Microsoft Windows */
        WINDOWS,
        /** Linux distributions */
        LINUX,
        /** Other/unknown OS */
        OTHER
    }
    
    // System property keys
    private static final String OS_ARCH_PROPERTY = "os.arch";
    private static final String OS_NAME_PROPERTY = "os.name";
    
    // Architecture constants
    private static final String AARCH64_ARCH = "aarch64";
    private static final String ARM64_ARCH = "arm64";
    private static final String X86_64_ARCH = "x86_64";
    private static final String AMD64_ARCH = "amd64";
    
    // OS name constants
    private static final String MAC_OS_NAME = "mac";
    private static final String WINDOWS_OS_NAME = "windows";
    private static final String LINUX_OS_NAME = "linux";
    
    // Cached detection results
    private static volatile Platform detectedPlatform;
    private static volatile OperatingSystem detectedOS;
    private static volatile Boolean isAppleSilicon;
    private static volatile Boolean hasAVX512;
    private static volatile Boolean hasAVX2;
    
    // System property provider (for testing)
    private static SystemPropertyProvider propertyProvider = System::getProperty;
    
    /**
     * Interface for providing system properties (enables testing).
     */
    public interface SystemPropertyProvider {
        String getProperty(String key);
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private PlatformDetection() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Sets a custom system property provider for testing purposes.
     * 
     * @param provider the custom property provider
     * @throws IllegalArgumentException if provider is null
     */
    public static void setSystemPropertyProvider(SystemPropertyProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("System property provider cannot be null");
        }
        propertyProvider = provider;
        // Clear cached values when provider changes
        clearCache();
    }
    
    /**
     * Resets to the default system property provider.
     */
    public static void resetSystemPropertyProvider() {
        propertyProvider = System::getProperty;
        clearCache();
    }
    
    /**
     * Clears all cached detection results.
     * This forces re-detection on the next method call.
     */
    public static void clearCache() {
        detectedPlatform = null;
        detectedOS = null;
        isAppleSilicon = null;
        hasAVX512 = null;
        hasAVX2 = null;
    }
    
    /**
     * Detects the current platform type.
     * 
     * @return the detected platform type
     */
    public static Platform detectPlatform() {
        if (detectedPlatform == null) {
            synchronized (PlatformDetection.class) {
                if (detectedPlatform == null) {
                    detectedPlatform = detectPlatformInternal();
                }
            }
        }
        return detectedPlatform;
    }
    
    /**
     * Detects the current operating system.
     * 
     * @return the detected operating system
     */
    public static OperatingSystem detectOperatingSystem() {
        if (detectedOS == null) {
            synchronized (PlatformDetection.class) {
                if (detectedOS == null) {
                    detectedOS = detectOperatingSystemInternal();
                }
            }
        }
        return detectedOS;
    }
    
    /**
     * Checks if the current platform is Apple Silicon (ARM64 macOS).
     * 
     * <p>This method replaces scattered checks like:</p>
     * <pre>{@code
     * System.getProperty("os.arch").equals("aarch64") && 
     * System.getProperty("os.name").toLowerCase().contains("mac")
     * }</pre>
     * 
     * @return true if running on Apple Silicon, false otherwise
     */
    public static boolean isAppleSilicon() {
        if (isAppleSilicon == null) {
            synchronized (PlatformDetection.class) {
                if (isAppleSilicon == null) {
                    isAppleSilicon = detectPlatform() == Platform.APPLE_SILICON;
                }
            }
        }
        return isAppleSilicon;
    }
    
    /**
     * Checks if the current platform is x86-64 architecture.
     * 
     * @return true if running on x86-64, false otherwise
     */
    public static boolean isX86_64() {
        return detectPlatform() == Platform.X86_64;
    }
    
    /**
     * Checks if the current platform is ARM64 (any ARM64, including Apple Silicon).
     * 
     * @return true if running on any ARM64 platform, false otherwise
     */
    public static boolean isARM64() {
        Platform platform = detectPlatform();
        return platform == Platform.ARM64 || platform == Platform.APPLE_SILICON;
    }
    
    /**
     * Estimates if the current platform supports AVX-512 instructions.
     * 
     * <p>This is a heuristic based on platform detection. For precise
     * detection, native code or specialized libraries would be needed.</p>
     * 
     * @return true if AVX-512 is likely supported, false otherwise
     */
    public static boolean hasAVX512Support() {
        if (hasAVX512 == null) {
            synchronized (PlatformDetection.class) {
                if (hasAVX512 == null) {
                    hasAVX512 = detectAVX512Support();
                }
            }
        }
        return hasAVX512;
    }
    
    /**
     * Estimates if the current platform supports AVX2 instructions.
     * 
     * <p>This is a heuristic based on platform detection. For precise
     * detection, native code or specialized libraries would be needed.</p>
     * 
     * @return true if AVX2 is likely supported, false otherwise
     */
    public static boolean hasAVX2Support() {
        if (hasAVX2 == null) {
            synchronized (PlatformDetection.class) {
                if (hasAVX2 == null) {
                    hasAVX2 = detectAVX2Support();
                }
            }
        }
        return hasAVX2;
    }
    
    /**
     * Gets the recommended SIMD threshold for the current platform.
     * 
     * <p>Apple Silicon typically benefits from lower thresholds due to
     * its efficient vector processing units.</p>
     * 
     * @return recommended threshold for SIMD operations
     */
    public static int getRecommendedSIMDThreshold() {
        return isAppleSilicon() ? 8 : 16;
    }
    
    /**
     * Gets platform-specific optimization hints.
     * 
     * @return a string describing platform-specific optimization characteristics
     */
    public static String getPlatformOptimizationHints() {
        Platform platform = detectPlatform();
        OperatingSystem os = detectOperatingSystem();
        
        StringBuilder hints = new StringBuilder();
        hints.append("Platform: ").append(platform);
        hints.append(", OS: ").append(os);
        
        if (isAppleSilicon()) {
            hints.append(", Optimizations: Apple Silicon NEON, unified memory");
        } else if (isX86_64()) {
            if (hasAVX512Support()) {
                hints.append(", Optimizations: AVX-512");
            } else if (hasAVX2Support()) {
                hints.append(", Optimizations: AVX2");
            } else {
                hints.append(", Optimizations: SSE");
            }
        }
        
        return hints.toString();
    }
    
    // Internal detection methods
    
    private static Platform detectPlatformInternal() {
        String arch = getProperty(OS_ARCH_PROPERTY);
        String osName = getProperty(OS_NAME_PROPERTY);
        
        if (arch == null) {
            return Platform.OTHER;
        }
        
        arch = arch.toLowerCase();
        
        // Check for Apple Silicon
        if ((arch.equals(AARCH64_ARCH) || arch.equals(ARM64_ARCH)) && 
            osName != null && osName.toLowerCase().contains(MAC_OS_NAME)) {
            return Platform.APPLE_SILICON;
        }
        
        // Check for other ARM64
        if (arch.equals(AARCH64_ARCH) || arch.equals(ARM64_ARCH)) {
            return Platform.ARM64;
        }
        
        // Check for x86-64
        if (arch.equals(X86_64_ARCH) || arch.equals(AMD64_ARCH)) {
            return Platform.X86_64;
        }
        
        return Platform.OTHER;
    }
    
    private static OperatingSystem detectOperatingSystemInternal() {
        String osName = getProperty(OS_NAME_PROPERTY);
        
        if (osName == null) {
            return OperatingSystem.OTHER;
        }
        
        osName = osName.toLowerCase();
        
        if (osName.contains(MAC_OS_NAME)) {
            return OperatingSystem.MACOS;
        } else if (osName.contains(WINDOWS_OS_NAME)) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains(LINUX_OS_NAME) || osName.contains("ubuntu") || 
                   osName.contains("debian") || osName.contains("centos") || 
                   osName.contains("fedora") || osName.contains("red hat")) {
            return OperatingSystem.LINUX;
        }
        
        return OperatingSystem.OTHER;
    }
    
    private static boolean detectAVX512Support() {
        // Heuristic: Most modern x86-64 server processors support AVX-512
        // This is a conservative estimate and may not be accurate for all systems
        return isX86_64() && !isAppleSilicon();
    }
    
    private static boolean detectAVX2Support() {
        // Heuristic: Most x86-64 processors from 2013+ support AVX2
        // Apple Silicon has equivalent NEON capabilities
        return isX86_64() || isAppleSilicon();
    }
    
    private static String getProperty(String key) {
        try {
            return propertyProvider.getProperty(key);
        } catch (Exception e) {
            // Return null if property cannot be accessed
            return null;
        }
    }
}