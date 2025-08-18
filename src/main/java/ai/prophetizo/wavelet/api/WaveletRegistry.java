package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple WaveletRegistry that just works everywhere.
 * No ServiceLoader, no complexity, just wavelets.
 */
public final class WaveletRegistry {
    
    private static final Map<String, Wavelet> WAVELETS = new ConcurrentHashMap<>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static final Set<String> CANONICAL_NAMES = ConcurrentHashMap.newKeySet();
    
    static {
        // Register all wavelets using consistent pattern
        // Orthogonal wavelets
        register(Haar.INSTANCE);
        
        // Daubechies wavelets
        register(Daubechies.DB2);
        register(Daubechies.DB4);
        register(Daubechies.DB6);
        register(Daubechies.DB8);
        register(Daubechies.DB10);
        
        // Symlet wavelets
        register(Symlet.SYM2);
        register(Symlet.SYM3);
        register(Symlet.SYM4);
        register(Symlet.SYM5);
        register(Symlet.SYM6);
        register(Symlet.SYM7);
        register(Symlet.SYM8);
        register(Symlet.SYM10);
        register(Symlet.SYM12);
        register(Symlet.SYM15);
        register(Symlet.SYM20);
        
        // Coiflet wavelets
        register(Coiflet.COIF1);
        register(Coiflet.COIF2);
        register(Coiflet.COIF3);
        register(Coiflet.COIF4);
        register(Coiflet.COIF5);
        
        // Continuous wavelets
        register(new MorletWavelet());  // Uses default params (omega0=6, sigma=1)
    }
    
    private static void register(Wavelet w) {
        String name = w.name().toLowerCase();
        WAVELETS.put(name, w);
        CANONICAL_NAMES.add(name);
        
        // Add common aliases (map alias to canonical name)
        if (name.startsWith("db")) {
            String alias = "daubechies" + name.substring(2);
            ALIASES.put(alias, name);
            WAVELETS.put(alias, w);
        } else if (name.equals("morl")) {
            ALIASES.put("morlet", name);
            WAVELETS.put("morlet", w);
        }
    }
    
    public static Wavelet getWavelet(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidArgumentException("Wavelet name cannot be null or empty");
        }
        
        Wavelet w = WAVELETS.get(name.toLowerCase());
        if (w == null) {
            throw new InvalidArgumentException("Unknown wavelet: " + name);
        }
        return w;
    }
    
    public static boolean hasWavelet(String name) {
        return name != null && WAVELETS.containsKey(name.toLowerCase());
    }
    
    public static Set<String> getAvailableWavelets() {
        return new TreeSet<>(WAVELETS.keySet());
    }
    
    public static List<String> getOrthogonalWavelets() {
        List<String> result = new ArrayList<>();
        for (String name : CANONICAL_NAMES) {
            Wavelet wavelet = WAVELETS.get(name);
            if (wavelet != null && wavelet.getType() == WaveletType.ORTHOGONAL) {
                result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }
    
    public static List<String> getContinuousWavelets() {
        List<String> result = new ArrayList<>();
        for (String name : CANONICAL_NAMES) {
            Wavelet wavelet = WAVELETS.get(name);
            if (wavelet != null && wavelet.getType() == WaveletType.CONTINUOUS) {
                result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }
    
    // Compatibility methods for existing code
    public static boolean isWaveletAvailable(String name) {
        return hasWavelet(name);
    }
    
    public static Set<String> getWaveletsByType(WaveletType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        
        Set<String> result = new TreeSet<>();
        for (String name : CANONICAL_NAMES) {
            Wavelet wavelet = WAVELETS.get(name);
            if (wavelet != null && wavelet.getType() == type) {
                result.add(name);
            }
        }
        return result;
    }
    
    public static List<String> getBiorthogonalWavelets() {
        List<String> result = new ArrayList<>();
        for (String name : CANONICAL_NAMES) {
            Wavelet wavelet = WAVELETS.get(name);
            if (wavelet != null && wavelet.getType() == WaveletType.BIORTHOGONAL) {
                result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }
    
    public static void printAvailableWavelets() {
        System.out.println("Available Wavelets:");
        for (String name : getAvailableWavelets()) {
            Wavelet w = getWavelet(name);
            System.out.println("  " + name + " - " + w.description());
        }
    }
    
    private WaveletRegistry() {}
}