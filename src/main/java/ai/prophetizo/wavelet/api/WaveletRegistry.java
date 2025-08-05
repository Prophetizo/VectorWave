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
    private static final Map<WaveletType, Set<String>> CACHED_SETS_BY_TYPE = new EnumMap<>(WaveletType.class);
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
            // Fine-grained cache update: add to cached set if present
            Set<String> cachedSet = CACHED_SETS_BY_TYPE.get(type);
            if (cachedSet != null) {
                cachedSet.add(wavelet.name());
            }
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
     * Checks if a wavelet name is valid and available.
     *
     * @param name the wavelet name to check
     * @return true if the wavelet exists, false otherwise
     */
    public static boolean isWaveletAvailable(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
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
     * @return set of wavelet names of the given type
     */
    public static Set<String> getWaveletsByType(WaveletType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        
        // Use cached set if available
        Set<String> cachedSet = CACHED_SETS_BY_TYPE.get(type);
        if (cachedSet != null) {
            return cachedSet;
        }
        
        // Build and cache the set
        synchronized (WAVELETS_BY_TYPE) {
            // Double-check locking pattern
            cachedSet = CACHED_SETS_BY_TYPE.get(type);
            if (cachedSet != null) {
                return cachedSet;
            }
            
            List<String> wavelets = WAVELETS_BY_TYPE.getOrDefault(type, Collections.emptyList());
            Set<String> unmodifiableSet = Collections.unmodifiableSet(new LinkedHashSet<>(wavelets));
            CACHED_SETS_BY_TYPE.put(type, unmodifiableSet);
            return unmodifiableSet;
        }
    }

    /**
     * Gets all orthogonal wavelets.
     *
     * @return list of unique orthogonal wavelet names (in insertion order)
     */
    public static List<String> getOrthogonalWavelets() {
        return new ArrayList<>(getWaveletsByType(WaveletType.ORTHOGONAL));
    }

    /**
     * Gets all biorthogonal wavelets.
     *
     * @return a new list of unique biorthogonal wavelet names, preserving insertion order
     */
    public static List<String> getBiorthogonalWavelets() {
        return new ArrayList<>(getWaveletsByType(WaveletType.BIORTHOGONAL));
    }

    /**
     * Gets all continuous wavelets.
     *
     * @return a list of continuous wavelet names constructed from a set; order is not guaranteed
     */
    public static List<String> getContinuousWavelets() {
        return new ArrayList<>(getWaveletsByType(WaveletType.CONTINUOUS));
    }

    /**
     * Prints a summary of all available wavelets to stdout.
     */
    public static void printAvailableWavelets() {
        System.out.println("Available Wavelets in VectorWave:");
        System.out.println("=================================");

        for (WaveletType type : WaveletType.values()) {
            Set<String> wavelets = getWaveletsByType(type);
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
     * Clears any warnings from previous wavelet loading operations.
     * Alias for clearLoadWarnings() for API consistency.
     */
    public static void clearWarnings() {
        clearLoadWarnings();
    }
    
    /**
     * Gets warnings from the last wavelet loading operation.
     * Alias for getLoadWarnings() for API consistency.
     *
     * @return list of warning messages
     */
    public static List<String> getWarnings() {
        return getLoadWarnings();
    }
    
    /**
     * Returns metadata about a specific wavelet.
     *
     * @param name the wavelet name
     * @return WaveletInfo containing family, order, properties, etc.
     * @throws InvalidArgumentException if wavelet name is unknown
     */
    public static WaveletInfo getWaveletInfo(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidArgumentException("Wavelet name cannot be null or empty");
        }
        
        Wavelet wavelet = WAVELETS.get(name.toLowerCase());
        if (wavelet == null) {
            throw new InvalidArgumentException("Unknown wavelet: " + name);
        }
        
        return createWaveletInfo(wavelet);
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
                // Clear cache since data has changed
                CACHED_SETS_BY_TYPE.clear();
            }
        }
    }
    
    /**
     * Creates WaveletInfo from a Wavelet instance.
     */
    private static WaveletInfo createWaveletInfo(Wavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        
        String name = wavelet.name().toLowerCase();
        String displayName = createDisplayName(wavelet);
        WaveletType type = wavelet.getType();
        String family = extractFamily(wavelet);
        int order = extractOrder(wavelet);
        Set<String> aliases = createAliases(wavelet);
        String description = createDescription(wavelet);
        
        int vanishingMoments = 0;
        int filterLength = 0;
        
        // Extract properties for discrete wavelets
        if (wavelet instanceof DiscreteWavelet discreteWavelet) {
            vanishingMoments = discreteWavelet.vanishingMoments();
            filterLength = discreteWavelet.lowPassDecomposition().length;
        }
        
        return new WaveletInfo.Builder(name, type)
            .displayName(displayName)
            .family(family)
            .order(order)
            .aliases(aliases)
            .description(description)
            .vanishingMoments(vanishingMoments)
            .filterLength(filterLength)
            .build();
    }
    
    private static String createDisplayName(Wavelet wavelet) {
        String name = wavelet.name();
        
        // Handle specific naming patterns
        if (name.toLowerCase().startsWith("db")) {
            String orderStr = name.substring(2);
            try {
                int order = Integer.parseInt(orderStr);
                return "Daubechies " + order;
            } catch (NumberFormatException e) {
                return name;
            }
        } else if (name.toLowerCase().startsWith("sym")) {
            String orderStr = name.substring(3);
            try {
                int order = Integer.parseInt(orderStr);
                return "Symlet " + order;
            } catch (NumberFormatException e) {
                return name;
            }
        } else if (name.toLowerCase().startsWith("coif")) {
            String orderStr = name.substring(4);
            try {
                int order = Integer.parseInt(orderStr);
                return "Coiflet " + order;
            } catch (NumberFormatException e) {
                return name;
            }
        } else if (name.toLowerCase().equals("haar")) {
            return "Haar";
        } else if (name.toLowerCase().startsWith("bior")) {
            return "Biorthogonal " + name.substring(4);
        } else if (name.toLowerCase().equals("morlet")) {
            return "Morlet";
        } else if (name.toLowerCase().equals("morl")) {
            return "Morlet";
        } else {
            // Capitalize first letter
            return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
    }
    
    private static String extractFamily(Wavelet wavelet) {
        String name = wavelet.name().toLowerCase();
        
        if (name.startsWith("db") || name.startsWith("daub")) {
            return "Daubechies";
        } else if (name.startsWith("sym")) {
            return "Symlet";
        } else if (name.startsWith("coif")) {
            return "Coiflet";
        } else if (name.equals("haar")) {
            return "Haar";
        } else if (name.startsWith("bior")) {
            return "Biorthogonal";
        } else if (name.contains("morlet") || name.equals("morl")) {
            return "Morlet";
        } else if (name.contains("mexican") || name.equals("mexh")) {
            return "Mexican Hat";
        } else if (name.contains("gaussian") || name.startsWith("gaus")) {
            return "Gaussian";
        } else {
            return "Other";
        }
    }
    
    private static int extractOrder(Wavelet wavelet) {
        String name = wavelet.name().toLowerCase();
        
        // Try to extract numeric order from common patterns
        if (name.startsWith("db") || name.startsWith("sym") || name.startsWith("coif")) {
            String prefix = name.startsWith("db") ? "db" : 
                           name.startsWith("sym") ? "sym" : "coif";
            String orderStr = name.substring(prefix.length());
            try {
                return Integer.parseInt(orderStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (name.startsWith("bior")) {
            // Handle biorthogonal naming like bior1.3, bior2.2
            String orderStr = name.substring(4);
            if (orderStr.contains(".")) {
                String[] parts = orderStr.split("\\.");
                try {
                    return Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        
        return 0; // No specific order
    }
    
    private static Set<String> createAliases(Wavelet wavelet) {
        Set<String> aliases = new LinkedHashSet<>();
        String name = wavelet.name().toLowerCase();
        
        // Add common aliases
        if (name.startsWith("db")) {
            String orderStr = name.substring(2);
            aliases.add("daubechies" + orderStr);
            aliases.add("daub" + orderStr);
        } else if (name.startsWith("daubechies")) {
            String orderStr = name.substring(10);
            aliases.add("db" + orderStr);
            aliases.add("daub" + orderStr);
        } else if (name.equals("morlet")) {
            aliases.add("morl");
        } else if (name.equals("morl")) {
            aliases.add("morlet");
        } else if (name.startsWith("bior")) {
            aliases.add("biorthogonal" + name.substring(4));
        }
        
        return aliases;
    }
    
    private static String createDescription(Wavelet wavelet) {
        String family = extractFamily(wavelet);
        int order = extractOrder(wavelet);
        WaveletType type = wavelet.getType();
        
        StringBuilder desc = new StringBuilder();
        
        switch (family) {
            case "Haar":
                desc.append("The Haar wavelet, the simplest wavelet with compact support.");
                break;
            case "Daubechies":
                desc.append("Daubechies wavelets with ").append(order).append(" vanishing moments. ")
                    .append("Orthogonal wavelets with compact support and maximal smoothness.");
                break;
            case "Symlet":
                desc.append("Symlet ").append(order).append(" wavelet. ")
                    .append("Nearly symmetric wavelets with compact support.");
                break;
            case "Coiflet":
                desc.append("Coiflet ").append(order).append(" wavelet. ")
                    .append("Wavelets with both scaling and wavelet functions having vanishing moments.");
                break;
            case "Biorthogonal":
                desc.append("Biorthogonal wavelet with symmetric/antisymmetric properties.");
                break;
            case "Morlet":
                desc.append("Morlet wavelet, a complex continuous wavelet based on a Gaussian modulated by a complex exponential.");
                break;
            case "Mexican Hat":
                desc.append("Mexican Hat wavelet, the second derivative of a Gaussian function.");
                break;
            case "Gaussian":
                desc.append("Gaussian derivative wavelet.");
                break;
            default:
                desc.append("A ").append(type.name().toLowerCase()).append(" wavelet.");
        }
        
        return desc.toString();
    }
}