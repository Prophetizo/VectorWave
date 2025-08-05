package ai.prophetizo.examples;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comprehensive demo showing best practices for using the WaveletRegistry.
 * 
 * <p>This demo covers:
 * <ul>
 *   <li>Wavelet discovery and exploration</li>
 *   <li>Safe wavelet selection with validation</li>
 *   <li>Error handling patterns</li>
 *   <li>Performance considerations</li>
 *   <li>Integration with MODWT transforms</li>
 *   <li>Advanced filtering and selection strategies</li>
 * </ul>
 * </p>
 * 
 * <p><b>Best Practices Demonstrated:</b></p>
 * <ul>
 *   <li>Always validate wavelet availability before use</li>
 *   <li>Use type-specific queries for better performance</li>
 *   <li>Handle exceptions gracefully with fallback strategies</li>
 *   <li>Cache frequently used wavelets for performance</li>
 *   <li>Leverage wavelet metadata for intelligent selection</li>
 * </ul>
 * 
 * @author VectorWave Team
 * @since 1.0
 */
public class WaveletRegistryBestPracticesDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("         VectorWave Wavelet Registry - Best Practices Demo");
        System.out.println("=".repeat(80));

        WaveletRegistryBestPracticesDemo demo = new WaveletRegistryBestPracticesDemo();
        
        try {
            // 1. Wavelet Discovery
            demo.demonstrateWaveletDiscovery();
            
            // 2. Safe Wavelet Selection
            demo.demonstrateSafeWaveletSelection();
            
            // 3. Error Handling Patterns
            demo.demonstrateErrorHandling();
            
            // 4. Advanced Filtering and Selection
            demo.demonstrateAdvancedSelection();
            
            // 5. Performance Optimization
            demo.demonstratePerformanceOptimization();
            
            // 6. Integration with MODWT
            demo.demonstrateMODWTIntegration();
            
            // 7. Wavelet Metadata Usage
            demo.demonstrateMetadataUsage();
            
        } catch (Exception e) {
            System.err.println("Demo failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                            Demo Complete");
        System.out.println("=".repeat(80));
    }

    /**
     * Demonstrates basic wavelet discovery patterns.
     * Best Practice: Always explore available wavelets before making assumptions.
     */
    public void demonstrateWaveletDiscovery() {
        System.out.println("\n📋 1. WAVELET DISCOVERY");
        System.out.println("-".repeat(50));
        
        // Get all available wavelets
        Set<String> allWavelets = WaveletRegistry.getAvailableWavelets();
        System.out.println("Total wavelets available: " + allWavelets.size());
        
        // Discover by type - this is more efficient than filtering all wavelets
        System.out.println("\nWavelets by type:");
        for (WaveletType type : WaveletType.values()) {
            Set<String> waveletsOfType = WaveletRegistry.getWaveletsByType(type);
            if (!waveletsOfType.isEmpty()) {
                System.out.printf("  %-12s: %d wavelets (%s)\n", 
                    type, waveletsOfType.size(), 
                    waveletsOfType.stream().limit(3).collect(Collectors.joining(", ")) +
                    (waveletsOfType.size() > 3 ? ", ..." : ""));
            }
        }
        
        // Use convenience methods for common types
        List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        List<String> continuousWavelets = WaveletRegistry.getContinuousWavelets();
        
        System.out.println("\nUsing convenience methods:");
        System.out.println("  Orthogonal wavelets: " + orthogonalWavelets.size());
        System.out.println("  Continuous wavelets: " + continuousWavelets.size());
    }

    /**
     * Demonstrates safe wavelet selection with proper validation.
     * Best Practice: Always validate before use, provide fallbacks for robustness.
     */
    public void demonstrateSafeWaveletSelection() {
        System.out.println("\n🔒 2. SAFE WAVELET SELECTION");
        System.out.println("-".repeat(50));
        
        // Best Practice: Check availability before use
        String[] candidateWavelets = {"db4", "haar", "sym8", "nonexistent"};
        
        System.out.println("Checking wavelet availability:");
        for (String name : candidateWavelets) {
            if (WaveletRegistry.isWaveletAvailable(name)) {
                System.out.println("  ✓ " + name + " - Available");
            } else {
                System.out.println("  ✗ " + name + " - Not available");
            }
        }
        
        // Safe selection with fallback strategy
        Wavelet selectedWavelet = selectWaveletWithFallback("sym8", "db4", "haar");
        System.out.println("\nSelected wavelet: " + selectedWavelet.name());
        
        // Validate wavelet properties for your use case
        if (selectedWavelet instanceof DiscreteWavelet discreteWavelet) {
            int filterLength = discreteWavelet.lowPassDecomposition().length;
            System.out.println("Filter length: " + filterLength);
            
            // Best Practice: Validate suitability for your signal length
            int signalLength = 1024;
            if (isWaveletSuitableForSignal(discreteWavelet, signalLength)) {
                System.out.println("✓ Wavelet is suitable for signal length " + signalLength);
            } else {
                System.out.println("⚠ Wavelet may not be optimal for signal length " + signalLength);
            }
        }
    }

    /**
     * Demonstrates proper error handling patterns.
     * Best Practice: Handle exceptions gracefully with meaningful error messages.
     */
    public void demonstrateErrorHandling() {
        System.out.println("\n⚠️ 3. ERROR HANDLING PATTERNS");
        System.out.println("-".repeat(50));
        
        // Pattern 1: Graceful handling of invalid wavelet names
        String[] testNames = {"", null, "invalid_wavelet", "db999"};
        
        for (String name : testNames) {
            try {
                System.out.println("Attempting to get wavelet: " + (name == null ? "null" : "'" + name + "'"));
                Wavelet wavelet = getWaveletSafely(name);
                System.out.println("  ✓ Success: " + wavelet.name());
            } catch (InvalidArgumentException e) {
                System.out.println("  ✗ Expected error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  ✗ Unexpected error: " + e.getMessage());
            }
        }
        
        // Pattern 2: Validation with detailed error messages
        System.out.println("\nValidation examples:");
        validateWaveletForApplication("db4", "financial_analysis");
        validateWaveletForApplication("morl", "image_processing");
        validateWaveletForApplication("nonexistent", "any_application");
    }

    /**
     * Demonstrates advanced wavelet selection strategies.
     * Best Practice: Use metadata and properties for intelligent selection.
     */
    public void demonstrateAdvancedSelection() {
        System.out.println("\n🎯 4. ADVANCED SELECTION STRATEGIES");
        System.out.println("-".repeat(50));
        
        // Strategy 1: Select by vanishing moments for smoothness
        System.out.println("Selecting wavelets with high vanishing moments:");
        List<String> smoothWavelets = findWaveletsWithMinVanishingMoments(4);
        smoothWavelets.stream().limit(5).forEach(name -> {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                System.out.printf("  %s: %d vanishing moments\n", 
                    info.getDisplayName(), info.getVanishingMoments());
            } catch (Exception e) {
                System.out.println("  " + name + ": metadata unavailable");
            }
        });
        
        // Strategy 2: Select by filter length for computational efficiency
        System.out.println("\nSelecting compact support wavelets (short filters):");
        List<String> compactWavelets = findCompactSupportWavelets(8);
        compactWavelets.stream().limit(5).forEach(name -> {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                System.out.printf("  %s: filter length %d\n", 
                    info.getDisplayName(), info.getFilterLength());
            } catch (Exception e) {
                System.out.println("  " + name + ": metadata unavailable");
            }
        });
        
        // Strategy 3: Family-based selection
        System.out.println("\nSelecting from Daubechies family:");
        List<String> daubechiesWavelets = findWaveletsByFamily("Daubechies");
        daubechiesWavelets.forEach(name -> {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                System.out.printf("  %s (order %d)\n", info.getDisplayName(), info.getOrder());
            } catch (Exception e) {
                System.out.println("  " + name + ": metadata unavailable");
            }
        });
    }

    /**
     * Demonstrates performance optimization techniques.
     * Best Practice: Cache wavelets, use efficient queries, minimize object creation.
     */
    public void demonstratePerformanceOptimization() {
        System.out.println("\n⚡ 5. PERFORMANCE OPTIMIZATION");
        System.out.println("-".repeat(50));
        
        // Performance tip 1: Cache frequently used wavelets
        WaveletCache cache = new WaveletCache();
        
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Wavelet w = cache.getWavelet("db4"); // Cached lookup
        }
        long cachedTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Wavelet w = WaveletRegistry.getWavelet("db4"); // Direct lookup
        }
        long directTime = System.nanoTime() - startTime;
        
        System.out.printf("Performance comparison (1000 lookups):\n");
        System.out.printf("  Cached:  %d ns\n", cachedTime);
        System.out.printf("  Direct:  %d ns\n", directTime);
        System.out.printf("  Speedup: %.2fx\n", (double) directTime / cachedTime);
        
        // Performance tip 2: Use type-specific queries
        System.out.println("\nType-specific queries are more efficient:");
        startTime = System.nanoTime();
        Set<String> orthogonal1 = WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL);
        long typeSpecificTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        Set<String> orthogonal2 = WaveletRegistry.getAvailableWavelets().stream()
            .filter(name -> {
                try {
                    return WaveletRegistry.getWavelet(name).getType() == WaveletType.ORTHOGONAL;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toSet());
        long filterTime = System.nanoTime() - startTime;
        
        System.out.printf("  Type-specific query: %d ns\n", typeSpecificTime);
        System.out.printf("  Filter-based query: %d ns\n", filterTime);
        System.out.printf("  Speedup: %.2fx\n", (double) filterTime / typeSpecificTime);
    }

    /**
     * Demonstrates integration with MODWT transforms.
     * Best Practice: Validate wavelet compatibility with your transform requirements.
     */
    public void demonstrateMODWTIntegration() {
        System.out.println("\n🔗 6. MODWT INTEGRATION");
        System.out.println("-".repeat(50));
        
        // Generate test signal
        double[] signal = generateTestSignal(256);
        
        // Test different wavelets with MODWT
        String[] testWavelets = {"haar", "db4", "sym8"};
        
        for (String waveletName : testWavelets) {
            try {
                Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
                
                if (wavelet instanceof DiscreteWavelet discreteWavelet) {
                    // Create MODWT transform
                    MODWTTransform transform = new MODWTTransform(discreteWavelet, BoundaryMode.PERIODIC);
                    
                    // Perform transform
                    long startTime = System.nanoTime();
                    MODWTResult result = transform.forward(signal);
                    long transformTime = System.nanoTime() - startTime;
                    
                    // Test reconstruction
                    startTime = System.nanoTime();
                    double[] reconstructed = transform.inverse(result);
                    long reconstructTime = System.nanoTime() - startTime;
                    
                    // Calculate reconstruction error
                    double maxError = calculateMaxError(signal, reconstructed);
                    
                    System.out.printf("  %s:\n", wavelet.name());
                    System.out.printf("    Transform time:  %d ns\n", transformTime);
                    System.out.printf("    Reconstruct time: %d ns\n", reconstructTime);
                    System.out.printf("    Max error:       %.2e\n", maxError);
                    System.out.printf("    Perfect reconstruction: %s\n", 
                        maxError < 1e-10 ? "✓" : "✗");
                } else {
                    System.out.printf("  %s: Not a discrete wavelet (skipping MODWT)\n", waveletName);
                }
                
            } catch (Exception e) {
                System.out.printf("  %s: Error - %s\n", waveletName, e.getMessage());
            }
        }
    }

    /**
     * Demonstrates effective use of wavelet metadata.
     * Best Practice: Use metadata to make informed decisions about wavelet selection.
     */
    public void demonstrateMetadataUsage() {
        System.out.println("\n📊 7. WAVELET METADATA USAGE");
        System.out.println("-".repeat(50));
        
        String[] exampleWavelets = {"haar", "db4", "sym8", "coif2", "bior1.3"};
        
        System.out.println("Detailed wavelet information:");
        System.out.println("-".repeat(80));
        
        for (String name : exampleWavelets) {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                
                System.out.printf("Wavelet: %s\n", info.getDisplayName());
                System.out.printf("  Name:             %s\n", info.getName());
                System.out.printf("  Type:             %s\n", info.getType());
                System.out.printf("  Family:           %s\n", info.getFamily());
                System.out.printf("  Order:            %d\n", info.getOrder());
                System.out.printf("  Vanishing moments: %d\n", info.getVanishingMoments());
                System.out.printf("  Filter length:    %d\n", info.getFilterLength());
                System.out.printf("  Description:      %s\n", info.getDescription());
                
                if (!info.getAliases().isEmpty()) {
                    System.out.printf("  Aliases:          %s\n", 
                        String.join(", ", info.getAliases()));
                }
                
                // Use metadata for recommendations
                System.out.print("  Best for:         ");
                recommendUseCases(info);
                System.out.println();
                System.out.println();
                
            } catch (Exception e) {
                System.out.printf("  %s: Could not retrieve metadata - %s\n", name, e.getMessage());
            }
        }
    }

    // Helper methods for the demo

    /**
     * Selects a wavelet with fallback strategy.
     */
    private Wavelet selectWaveletWithFallback(String... candidates) {
        for (String candidate : candidates) {
            if (WaveletRegistry.isWaveletAvailable(candidate)) {
                return WaveletRegistry.getWavelet(candidate);
            }
        }
        throw new RuntimeException("No suitable wavelet found from candidates");
    }

    /**
     * Safely gets a wavelet with proper null/empty checking.
     */
    private Wavelet getWaveletSafely(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidArgumentException("Wavelet name cannot be null or empty");
        }
        return WaveletRegistry.getWavelet(name);
    }

    /**
     * Validates if a wavelet is suitable for a given signal length.
     */
    private boolean isWaveletSuitableForSignal(DiscreteWavelet wavelet, int signalLength) {
        int filterLength = wavelet.lowPassDecomposition().length;
        // Rule of thumb: signal should be at least 3x the filter length
        return signalLength >= filterLength * 3;
    }

    /**
     * Validates wavelet for specific application with detailed feedback.
     */
    private void validateWaveletForApplication(String waveletName, String application) {
        try {
            if (!WaveletRegistry.isWaveletAvailable(waveletName)) {
                System.out.printf("  ✗ %s not available for %s\n", waveletName, application);
                return;
            }

            Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
            WaveletInfo info = WaveletRegistry.getWaveletInfo(waveletName);
            
            boolean suitable = switch (application) {
                case "financial_analysis" -> 
                    wavelet.getType() == WaveletType.ORTHOGONAL && info.getVanishingMoments() >= 2;
                case "image_processing" -> 
                    info.getFilterLength() <= 20; // Compact support preferred
                default -> true;
            };
            
            System.out.printf("  %s %s for %s (%s, %d vanishing moments)\n",
                suitable ? "✓" : "⚠",
                waveletName,
                application,
                info.getFamily(),
                info.getVanishingMoments());
                
        } catch (Exception e) {
            System.out.printf("  ✗ Error validating %s: %s\n", waveletName, e.getMessage());
        }
    }

    /**
     * Finds wavelets with minimum vanishing moments.
     */
    private List<String> findWaveletsWithMinVanishingMoments(int minMoments) {
        return WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL)
            .stream()
            .filter(name -> {
                try {
                    WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                    return info.getVanishingMoments() >= minMoments;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Finds wavelets with compact support (short filters).
     */
    private List<String> findCompactSupportWavelets(int maxFilterLength) {
        return WaveletRegistry.getAvailableWavelets()
            .stream()
            .filter(name -> {
                try {
                    WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                    return info.getFilterLength() > 0 && info.getFilterLength() <= maxFilterLength;
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted((a, b) -> {
                try {
                    int lenA = WaveletRegistry.getWaveletInfo(a).getFilterLength();
                    int lenB = WaveletRegistry.getWaveletInfo(b).getFilterLength();
                    return Integer.compare(lenA, lenB);
                } catch (Exception e) {
                    return 0;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Finds wavelets by family name.
     */
    private List<String> findWaveletsByFamily(String family) {
        return WaveletRegistry.getAvailableWavelets()
            .stream()
            .filter(name -> {
                try {
                    WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                    return family.equalsIgnoreCase(info.getFamily());
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted((a, b) -> {
                try {
                    int orderA = WaveletRegistry.getWaveletInfo(a).getOrder();
                    int orderB = WaveletRegistry.getWaveletInfo(b).getOrder();
                    return Integer.compare(orderA, orderB);
                } catch (Exception e) {
                    return 0;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Generates a test signal for demonstrations.
     */
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combine sine waves and noise
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8) + 
                       0.1 * Math.random();
        }
        return signal;
    }

    /**
     * Calculates maximum reconstruction error.
     */
    private double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0.0;
        for (int i = 0; i < original.length; i++) {
            double error = Math.abs(original[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        return maxError;
    }

    /**
     * Provides use case recommendations based on wavelet properties.
     */
    private void recommendUseCases(WaveletInfo info) {
        if ("Haar".equals(info.getFamily())) {
            System.out.print("Edge detection, simple analysis");
        } else if ("Daubechies".equals(info.getFamily())) {
            if (info.getOrder() <= 4) {
                System.out.print("General purpose, signal compression");
            } else {
                System.out.print("Smooth signal analysis, feature extraction");
            }
        } else if ("Symlet".equals(info.getFamily())) {
            System.out.print("Nearly symmetric applications, image processing");
        } else if ("Coiflet".equals(info.getFamily())) {
            System.out.print("Signal processing requiring symmetry");
        } else if ("Biorthogonal".equals(info.getFamily())) {
            System.out.print("Image compression, perfect reconstruction");
        } else if ("Morlet".equals(info.getFamily())) {
            System.out.print("Time-frequency analysis, frequency localization");
        } else {
            System.out.print("Specialized applications");
        }
    }

    /**
     * Simple wavelet cache for performance optimization.
     */
    private static class WaveletCache {
        private final java.util.Map<String, Wavelet> cache = new java.util.concurrent.ConcurrentHashMap<>();
        
        public Wavelet getWavelet(String name) {
            return cache.computeIfAbsent(name.toLowerCase(), 
                n -> WaveletRegistry.getWavelet(n));
        }
        
        public void clear() {
            cache.clear();
        }
        
        public int size() {
            return cache.size();
        }
    }
}