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
        
        // Add common aliases
        if (name.startsWith("db")) {
            WAVELETS.put("daubechies" + name.substring(2), w);
        } else if (name.equals("morl")) {
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
        for (Map.Entry<String, Wavelet> entry : WAVELETS.entrySet()) {
            if (entry.getValue().getType() == WaveletType.ORTHOGONAL) {
                result.add(entry.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }
    
    public static List<String> getContinuousWavelets() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Wavelet> entry : WAVELETS.entrySet()) {
            if (entry.getValue().getType() == WaveletType.CONTINUOUS) {
                result.add(entry.getKey());
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
        for (Map.Entry<String, Wavelet> entry : WAVELETS.entrySet()) {
            if (entry.getValue().getType() == type) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    public static List<String> getBiorthogonalWavelets() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Wavelet> entry : WAVELETS.entrySet()) {
            if (entry.getValue().getType() == WaveletType.BIORTHOGONAL) {
                result.add(entry.getKey());
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