package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for wavelets using type-safe enum lookups.
 * Provides efficient wavelet access through the WaveletName enum.
 */
public final class WaveletRegistry {
    
    private static final Map<WaveletName, Wavelet> WAVELETS = new ConcurrentHashMap<>();
    
    static {
        // Register all wavelets using enum keys
        // Orthogonal wavelets
        register(WaveletName.HAAR, Haar.INSTANCE);
        
        // Daubechies wavelets
        register(WaveletName.DB2, Daubechies.DB2);
        register(WaveletName.DB4, Daubechies.DB4);
        register(WaveletName.DB6, Daubechies.DB6);
        register(WaveletName.DB8, Daubechies.DB8);
        register(WaveletName.DB10, Daubechies.DB10);
        
        // Symlet wavelets
        register(WaveletName.SYM2, Symlet.SYM2);
        register(WaveletName.SYM3, Symlet.SYM3);
        register(WaveletName.SYM4, Symlet.SYM4);
        register(WaveletName.SYM5, Symlet.SYM5);
        register(WaveletName.SYM6, Symlet.SYM6);
        register(WaveletName.SYM7, Symlet.SYM7);
        register(WaveletName.SYM8, Symlet.SYM8);
        register(WaveletName.SYM10, Symlet.SYM10);
        register(WaveletName.SYM12, Symlet.SYM12);
        register(WaveletName.SYM15, Symlet.SYM15);
        register(WaveletName.SYM20, Symlet.SYM20);
        
        // Coiflet wavelets
        register(WaveletName.COIF1, Coiflet.COIF1);
        register(WaveletName.COIF2, Coiflet.COIF2);
        register(WaveletName.COIF3, Coiflet.COIF3);
        register(WaveletName.COIF4, Coiflet.COIF4);
        register(WaveletName.COIF5, Coiflet.COIF5);
        
        // Continuous wavelets
        register(WaveletName.MORLET, new MorletWavelet());  // Uses default params (omega0=6, sigma=1)
    }
    
    private static void register(WaveletName name, Wavelet wavelet) {
        WAVELETS.put(name, wavelet);
    }
    
    /**
     * Get a wavelet by its enum name.
     * @param name the wavelet name enum
     * @return the corresponding Wavelet instance
     * @throws InvalidArgumentException if the wavelet is not registered
     */
    public static Wavelet getWavelet(WaveletName name) {
        if (name == null) {
            throw new InvalidArgumentException("Wavelet name cannot be null");
        }
        
        Wavelet w = WAVELETS.get(name);
        if (w == null) {
            throw new InvalidArgumentException("Wavelet not registered: " + name);
        }
        return w;
    }
    
    /**
     * Check if a wavelet is available.
     * @param name the wavelet name enum
     * @return true if the wavelet is registered, false otherwise
     */
    public static boolean hasWavelet(WaveletName name) {
        return name != null && WAVELETS.containsKey(name);
    }
    
    /**
     * Get all available wavelet names.
     * @return set of available wavelet names
     */
    public static Set<WaveletName> getAvailableWavelets() {
        return EnumSet.copyOf(WAVELETS.keySet());
    }
    
    /**
     * Get all orthogonal wavelet names.
     * @return list of orthogonal wavelet names
     */
    public static List<WaveletName> getOrthogonalWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.ORTHOGONAL)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all continuous wavelet names.
     * @return list of continuous wavelet names
     */
    public static List<WaveletName> getContinuousWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.CONTINUOUS)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all biorthogonal wavelet names.
     * @return list of biorthogonal wavelet names
     */
    public static List<WaveletName> getBiorthogonalWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.BIORTHOGONAL)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get wavelets by their type category.
     * @param type the wavelet type
     * @return set of wavelet names matching the type
     */
    public static Set<WaveletName> getWaveletsByType(WaveletType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == type)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(WaveletName.class)));
    }
    
    /**
     * Check if a wavelet is available.
     * @param name the wavelet name enum
     * @return true if the wavelet is registered, false otherwise
     */
    public static boolean isWaveletAvailable(WaveletName name) {
        return hasWavelet(name);
    }
    
    /**
     * Get Daubechies wavelets by order.
     * @return list of Daubechies wavelet names
     */
    public static List<WaveletName> getDaubechiesWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("DB"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get Symlet wavelets by order.
     * @return list of Symlet wavelet names
     */
    public static List<WaveletName> getSymletWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("SYM"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get Coiflet wavelets by order.
     * @return list of Coiflet wavelet names
     */
    public static List<WaveletName> getCoifletWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("COIF"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Print all available wavelets with their descriptions.
     */
    public static void printAvailableWavelets() {
        System.out.println("Available Wavelets:");
        for (WaveletName name : getAvailableWavelets()) {
            Wavelet w = getWavelet(name);
            System.out.println("  " + name + " (" + name.getCode() + ") - " + w.description());
        }
    }
    
    // ============================================================
    // Transform Compatibility API
    // ============================================================
    
    /**
     * Get all transforms supported by a specific wavelet.
     * This provides explicit information about which transforms can be used
     * with the given wavelet, improving API discoverability.
     * 
     * @param waveletName the wavelet to check
     * @return set of compatible transform types
     */
    public static Set<TransformType> getSupportedTransforms(WaveletName waveletName) {
        if (waveletName == null) {
            return EnumSet.noneOf(TransformType.class);
        }
        
        WaveletType type = waveletName.getType();
        Set<TransformType> supported = EnumSet.noneOf(TransformType.class);
        
        for (TransformType transform : TransformType.values()) {
            if (transform.isCompatibleWith(type)) {
                supported.add(transform);
            }
        }
        
        return supported;
    }
    
    /**
     * Check if a specific wavelet can be used with a specific transform.
     * 
     * @param waveletName the wavelet to check
     * @param transformType the transform type
     * @return true if the wavelet can be used with the transform
     */
    public static boolean isCompatible(WaveletName waveletName, TransformType transformType) {
        if (waveletName == null || transformType == null) {
            return false;
        }
        return transformType.isCompatibleWith(waveletName.getType());
    }
    
    /**
     * Get all wavelets that can be used with a specific transform.
     * Useful for populating UI dropdowns or validating user input.
     * 
     * @param transformType the transform type
     * @return list of compatible wavelet names
     */
    public static List<WaveletName> getWaveletsForTransform(TransformType transformType) {
        if (transformType == null) {
            return List.of();
        }
        
        return Stream.of(WaveletName.values())
            .filter(name -> transformType.isCompatibleWith(name.getType()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get recommended transform for a specific wavelet based on its type.
     * This helps users choose the most appropriate transform.
     * 
     * @param waveletName the wavelet
     * @return recommended transform type, or null if wavelet not found
     */
    public static TransformType getRecommendedTransform(WaveletName waveletName) {
        if (waveletName == null) {
            return null;
        }
        
        return switch (waveletName.getType()) {
            case ORTHOGONAL, BIORTHOGONAL -> TransformType.MODWT;
            case CONTINUOUS -> TransformType.CWT;
            case COMPLEX -> TransformType.CWT;
            default -> TransformType.MODWT; // Default fallback
        };
    }
    
    /**
     * Print transform compatibility matrix for all wavelets.
     * Useful for documentation and debugging.
     */
    public static void printTransformCompatibilityMatrix() {
        System.out.println("\nWavelet-Transform Compatibility Matrix:");
        System.out.println("=========================================");
        
        // Header
        System.out.printf("%-20s", "Wavelet");
        for (TransformType transform : TransformType.values()) {
            System.out.printf("%-15s", transform.getCode());
        }
        System.out.println();
        
        // Separator
        System.out.println("-".repeat(20 + TransformType.values().length * 15));
        
        // Matrix
        for (WaveletName wavelet : WaveletName.values()) {
            System.out.printf("%-20s", wavelet.getCode());
            for (TransformType transform : TransformType.values()) {
                String compatible = isCompatible(wavelet, transform) ? "✓" : "-";
                System.out.printf("%-15s", compatible);
            }
            System.out.println();
        }
    }
    
    private WaveletRegistry() {}
}