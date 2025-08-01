package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available wavelets in the VectorWave library.
 * Provides a centralized location to discover and create wavelet instances.
 *
 * <p>This registry supports:
 * <ul>
 *   <li>Listing available wavelets by type</li>
 *   <li>Creating wavelets by name</li>
 *   <li>Querying wavelet properties</li>
 *   <li>Automatic discovery via ServiceLoader</li>
 *   <li>Plugin architecture for third-party wavelets</li>
 * </ul>
 * </p>
 * 
 * <p>The registry uses Java's ServiceLoader mechanism to automatically discover
 * wavelets at runtime, eliminating circular dependencies and supporting extensibility.</p>
 */
public final class WaveletRegistry {

    private static final Map<String, Wavelet> WAVELETS = new ConcurrentHashMap<>();
    private static final Map<WaveletType, List<String>> WAVELETS_BY_TYPE = new EnumMap<>(WaveletType.class);
    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    static {
        // Eagerly initialize the registry
        initialize();
    }

    /**
     * Initializes the registry by discovering wavelets via ServiceLoader.
     * This method is thread-safe and idempotent.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            
            // Discover wavelets using ServiceLoader
            ServiceLoader<WaveletProvider> loader = ServiceLoader.load(WaveletProvider.class);
            
            for (WaveletProvider provider : loader) {
                try {
                    List<Wavelet> wavelets = provider.getWavelets();
                    if (wavelets != null) {
                        for (Wavelet wavelet : wavelets) {
                            if (wavelet != null) {
                                register(wavelet);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log and continue with other providers
                    System.err.println("Error loading wavelets from provider " + 
                                     provider.getClass().getName() + ": " + e.getMessage());
                }
            }
            
            initialized = true;
        }
    }

    private WaveletRegistry() {
        // Private constructor to prevent instantiation
    }

    /**
     * Registers a wavelet in the registry.
     *
     * @param wavelet the wavelet to register
     */
    private static void register(Wavelet wavelet) {
        WAVELETS.put(wavelet.name().toLowerCase(), wavelet);

        WaveletType type = wavelet.getType();
        synchronized (WAVELETS_BY_TYPE) {
            WAVELETS_BY_TYPE.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(wavelet.name());
        }
    }

    /**
     * Gets a wavelet by name.
     *
     * @param name the wavelet name (case-insensitive)
     * @return the wavelet instance
     * @throws InvalidArgumentException if wavelet not found
     */
    public static Wavelet getWavelet(String name) {
        Wavelet wavelet = WAVELETS.get(name.toLowerCase());
        if (wavelet == null) {
            throw new InvalidArgumentException("Unknown wavelet: " + name);
        }
        return wavelet;
    }

    /**
     * Checks if a wavelet exists in the registry.
     *
     * @param name the wavelet name
     * @return true if the wavelet exists
     */
    public static boolean hasWavelet(String name) {
        return WAVELETS.containsKey(name.toLowerCase());
    }

    /**
     * Gets all available wavelet names.
     *
     * @return unmodifiable set of wavelet names
     */
    public static Set<String> getAvailableWavelets() {
        return Collections.unmodifiableSet(WAVELETS.keySet());
    }

    /**
     * Gets wavelets by type.
     *
     * @param type the wavelet type
     * @return list of wavelet names of the given type
     */
    public static List<String> getWaveletsByType(WaveletType type) {
        return Collections.unmodifiableList(
                WAVELETS_BY_TYPE.getOrDefault(type, Collections.emptyList())
        );
    }

    /**
     * Gets all orthogonal wavelets.
     *
     * @return list of orthogonal wavelet names
     */
    public static List<String> getOrthogonalWavelets() {
        return getWaveletsByType(WaveletType.ORTHOGONAL);
    }

    /**
     * Gets all biorthogonal wavelets.
     *
     * @return list of biorthogonal wavelet names
     */
    public static List<String> getBiorthogonalWavelets() {
        return getWaveletsByType(WaveletType.BIORTHOGONAL);
    }

    /**
     * Gets all continuous wavelets.
     *
     * @return list of continuous wavelet names
     */
    public static List<String> getContinuousWavelets() {
        return getWaveletsByType(WaveletType.CONTINUOUS);
    }

    /**
     * Prints a summary of all available wavelets to stdout.
     */
    public static void printAvailableWavelets() {
        System.out.println("Available Wavelets in VectorWave:");
        System.out.println("=================================");

        for (WaveletType type : WaveletType.values()) {
            List<String> wavelets = getWaveletsByType(type);
            if (!wavelets.isEmpty()) {
                System.out.println("\n" + type + " Wavelets:");
                for (String name : wavelets) {
                    Wavelet w = getWavelet(name);
                    System.out.println("  - " + name + ": " + w.description());
                }
            }
        }
    }
    
    /**
     * Manually registers a wavelet in the registry.
     * This method allows runtime registration of wavelets not discovered by ServiceLoader.
     *
     * @param wavelet the wavelet to register
     * @throws IllegalArgumentException if wavelet is null
     */
    public static void registerWavelet(Wavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        register(wavelet);
    }
    
    /**
     * Reloads the registry by re-discovering wavelets via ServiceLoader.
     * This can be useful for dynamic plugin scenarios.
     */
    public static void reload() {
        synchronized (INIT_LOCK) {
            WAVELETS.clear();
            synchronized (WAVELETS_BY_TYPE) {
                WAVELETS_BY_TYPE.clear();
            }
            initialized = false;
            initialize();
        }
    }
}