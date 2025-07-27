package ai.prophetizo.wavelet.util;

/**
 * Example demonstrating how to use PlatformDetection utility to replace
 * scattered platform detection code throughout the codebase.
 * 
 * <p>This class shows before/after examples of platform detection patterns
 * and how they can be refactored using the centralized utility.</p>
 *
 * @since 1.0.0
 */
public final class PlatformDetectionExample {
    
    /**
     * Example of OLD platform detection pattern (scattered throughout codebase).
     * This is the type of code that should be replaced.
     * 
     * WARNING: These examples show PROBLEMATIC CODE with potential issues:
     * - NullPointerException risks from unchecked System.getProperty() calls
     * - Scattered, duplicated platform detection logic
     * - Hard to test and maintain
     * 
     * DO NOT USE THESE PATTERNS - they are shown here only as examples
     * of what to avoid. Use the NewPatterns examples instead.
     */
    public static class OldPatterns {
        
        /**
         * Example of hardcoded Apple Silicon detection.
         * This pattern was found in the CWT implementation code.
         * WARNING: This code has a potential NullPointerException!
         */
        public boolean isAppleSiliconOldWay() {
            // OLD: Hardcoded system property checks without null safety
            // FIXED VERSION (but still not recommended - use PlatformDetection instead):
            String arch = System.getProperty("os.arch");
            String osName = System.getProperty("os.name");
            return arch != null && arch.equals("aarch64") && 
                   osName != null && osName.toLowerCase().contains("mac");
        }
        
        /**
         * Example of platform-specific SIMD threshold selection.
         * WARNING: This code also has a potential NullPointerException!
         */
        public int getSIMDThresholdOldWay() {
            // OLD: Scattered platform checks without null safety
            // FIXED VERSION (but still not recommended - use PlatformDetection instead):
            String arch = System.getProperty("os.arch");
            String osName = System.getProperty("os.name");
            boolean isAppleSilicon = arch != null && arch.equals("aarch64") && 
                                   osName != null && osName.toLowerCase().contains("mac");
            return isAppleSilicon ? 8 : 16;
        }
        
        /**
         * Example of AVX capability detection.
         * NOTE: This version actually has proper null checking, but is still
         * not recommended due to incomplete platform detection logic.
         */
        public boolean hasAVXSupportOldWay() {
            // OLD: Incomplete platform detection (though null-safe in this case)
            String arch = System.getProperty("os.arch");
            // This is already null-safe due to short-circuit evaluation
            return arch != null && (arch.equals("x86_64") || arch.equals("amd64"));
        }
    }
    
    /**
     * Example of NEW platform detection pattern using the utility.
     * This is the recommended approach after refactoring.
     */
    public static class NewPatterns {
        
        /**
         * Example of centralized Apple Silicon detection.
         */
        public boolean isAppleSiliconNewWay() {
            // NEW: Clean, testable, centralized detection
            return PlatformDetector.isAppleSilicon();
        }
        
        /**
         * Example of platform-specific SIMD threshold selection.
         */
        public int getSIMDThresholdNewWay() {
            // NEW: Single method call with platform-optimized defaults
            return PlatformDetector.getRecommendedSIMDThreshold();
        }
        
        /**
         * Example of AVX capability detection.
         */
        public boolean hasAVX2SupportNewWay() {
            // NEW: Accurate, heuristic-based detection
            return PlatformDetector.hasAVX2Support();
        }
        
        /**
         * Example of comprehensive platform information.
         */
        public String getPlatformInfoNewWay() {
            // NEW: Rich platform information for debugging/optimization
            return PlatformDetector.getPlatformOptimizationHints();
        }
    }
    
    /**
     * Example of how the utility enables better testing.
     */
    public static class TestableCode {
        
        /**
         * Method that uses platform detection and can be easily tested.
         */
        public String selectOptimizationStrategy() {
            if (PlatformDetector.isAppleSilicon()) {
                return "Apple Silicon NEON optimizations";
            } else if (PlatformDetector.hasAVX512Support()) {
                return "AVX-512 optimizations"; 
            } else if (PlatformDetector.hasAVX2Support()) {
                return "AVX2 optimizations";
            } else {
                return "Scalar optimizations";
            }
        }
        
        /**
         * Configuration method that adapts to platform characteristics.
         */
        public OptimizationConfig createOptimizationConfig() {
            return new OptimizationConfig(
                PlatformDetector.getRecommendedSIMDThreshold(),
                PlatformDetector.hasAVX2Support(),
                PlatformDetector.isAppleSilicon()
            );
        }
    }
    
    /**
     * Example configuration class that uses platform detection.
     */
    public static class OptimizationConfig {
        private final int simdThreshold;
        private final boolean vectorizationEnabled;
        private final boolean appleOptimizations;
        
        public OptimizationConfig(int simdThreshold, boolean vectorizationEnabled, 
                                boolean appleOptimizations) {
            this.simdThreshold = simdThreshold;
            this.vectorizationEnabled = vectorizationEnabled;
            this.appleOptimizations = appleOptimizations;
        }
        
        public int getSIMDThreshold() { return simdThreshold; }
        public boolean isVectorizationEnabled() { return vectorizationEnabled; }
        public boolean hasAppleOptimizations() { return appleOptimizations; }
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private PlatformDetectionExample() {
        throw new AssertionError("Example class should not be instantiated");
    }
}