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
 * 
 * <p><b>Thread Safety:</b> This registry is designed for thread-safe operation:
 * <ul>
 *   <li>ConcurrentHashMap is used for the main wavelet storage to allow concurrent reads
 *       without locking, which is critical for performance in multi-threaded applications</li>
 *   <li>EnumMap with synchronized access is used for type-based lookups since writes are
 *       infrequent (only during initialization)</li>
 *   <li>Double-checked locking pattern ensures thread-safe lazy initialization</li>
 *   <li>Volatile boolean prevents visibility issues across threads</li>
 * </ul>
 * The choice of ConcurrentHashMap over synchronized collections provides better scalability
 * for read-heavy workloads typical in wavelet transform applications.</p>
 */
public final class WaveletRegistry {
    
    /**
     * Stores warnings from the last provider loading attempt.
     * Thread-safe list for collecting provider loading issues.
     */
    private static final List<String> loadWarnings = Collections.synchronizedList(new ArrayList<>());

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
                } catch (NoClassDefFoundError e) {
                    throw new RuntimeException(
                        "Missing dependency for provider " + provider.getClass().getName() + 
                        ". Check that all required classes are on the classpath.", 
                        e);
                } catch (NullPointerException e) {
                    throw new RuntimeException(
                        "Provider " + provider.getClass().getName() + 
                        " returned null or contains null wavelets.", 
                        e);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Error loading wavelets from provider " + provider.getClass().getName() + 
                        ": " + e.getClass().getSimpleName() + " - " + e.getMessage(), 
                        e);
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
     * Gets warnings from the last provider loading attempt.
     * 
     * @return unmodifiable list of warning messages from provider loading
     */
    public static List<String> getLoadWarnings() {
        return Collections.unmodifiableList(new ArrayList<>(loadWarnings));
    }
    
    /**
     * Clears provider loading warnings.
     */
    public static void clearLoadWarnings() {
        loadWarnings.clear();
    }
    
    /**
     * Reloads the registry by re-discovering wavelets via ServiceLoader.
     * This can be useful for dynamic plugin scenarios.
     * 
     * <p>Any warnings from provider loading can be retrieved via {@link #getLoadWarnings()}</p>
     */
    public static void reload() {
        synchronized (INIT_LOCK) {
            loadWarnings.clear(); // Clear previous warnings
            // Create temporary collections for atomic swap
            Map<String, Wavelet> newWavelets = new ConcurrentHashMap<>();
            Map<WaveletType, List<String>> newWaveletsByType = new EnumMap<>(WaveletType.class);
            
            // Discover wavelets into temporary collections
            ServiceLoader<WaveletProvider> loader = ServiceLoader.load(WaveletProvider.class);
            
            for (WaveletProvider provider : loader) {
                try {
                    List<Wavelet> wavelets = provider.getWavelets();
                    if (wavelets != null) {
                        for (Wavelet wavelet : wavelets) {
                            if (wavelet != null) {
                                // Register in temporary collections
                                newWavelets.put(wavelet.name().toLowerCase(), wavelet);
                                WaveletType type = wavelet.getType();
                                newWaveletsByType.computeIfAbsent(type, k -> new ArrayList<>())
                                        .add(wavelet.name());
                            }
                        }
                    }
                } catch (NoClassDefFoundError e) {
                    // During reload, it's possible for a provider to have issues
                    // Collect warning and skip this provider rather than failing entirely
                    String warning = "Missing dependency for provider " + 
                                   provider.getClass().getName() + ": " + e.getMessage();
                    loadWarnings.add(warning);
                } catch (Exception e) {
                    // Collect warning and skip problematic providers during reload
                    String warning = "Error loading wavelets from provider " + 
                                   provider.getClass().getName() + ": " + e.getMessage();
                    loadWarnings.add(warning);
                }
            }
            
            // Atomic swap - clear and replace
            WAVELETS.clear();
            WAVELETS.putAll(newWavelets);
            synchronized (WAVELETS_BY_TYPE) {
                WAVELETS_BY_TYPE.clear();
                WAVELETS_BY_TYPE.putAll(newWaveletsByType);
            }
        }
    }
}