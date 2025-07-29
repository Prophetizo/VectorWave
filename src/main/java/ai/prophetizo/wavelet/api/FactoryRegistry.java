package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.CWTFactory;
import ai.prophetizo.wavelet.ops.WaveletOpsFactory;
import ai.prophetizo.wavelet.denoising.StreamingDenoiserFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all factory implementations in the VectorWave library.
 * 
 * <p>This registry provides a unified way to discover, access, and manage
 * different factory implementations. It supports both built-in factories
 * and custom factory registration for extensibility.</p>
 * 
 * <p>The registry is thread-safe and supports concurrent access for both
 * registration and retrieval operations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get all available factories
 * Collection<Factory<?>> factories = FactoryRegistry.getAllFactories();
 * 
 * // Get factory by name
 * Optional<Factory<?>> factory = FactoryRegistry.getFactory("WaveletTransform");
 * 
 * // Get factories by product type
 * List<Factory<WaveletTransform>> transformFactories = 
 *     FactoryRegistry.getFactoriesForType(WaveletTransform.class);
 * 
 * // Register custom factory
 * FactoryRegistry.registerFactory("MyCustom", new MyCustomFactory());
 * }</pre>
 * 
 * @since 1.0.0
 */
public final class FactoryRegistry {
    
    // Thread-safe storage for registered factories
    private static final Map<String, Factory<?>> factories = new ConcurrentHashMap<>();
    
    // Static initialization of built-in factories
    static {
        registerBuiltInFactories();
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private FactoryRegistry() {
        throw new UnsupportedOperationException("FactoryRegistry is a utility class and cannot be instantiated");
    }
    
    /**
     * Registers all built-in factory implementations.
     */
    private static void registerBuiltInFactories() {
        registerFactory("CWT", new CWTFactory());
        registerFactory("WaveletOps", new WaveletOpsFactory());
        registerFactory("StreamingDenoiser", new StreamingDenoiserFactory());
    }
    
    /**
     * Registers a factory with the specified name.
     * 
     * <p>If a factory with the same name already exists, it will be replaced
     * with the new factory. This allows for factory override scenarios.</p>
     * 
     * @param name the unique name for the factory
     * @param factory the factory instance to register
     * @throws NullPointerException if name or factory is null
     * @throws IllegalArgumentException if name is empty or blank
     */
    public static void registerFactory(String name, Factory<?> factory) {
        Objects.requireNonNull(name, "Factory name cannot be null");
        Objects.requireNonNull(factory, "Factory cannot be null");
        
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Factory name cannot be empty or blank");
        }
        
        factories.put(name.trim(), factory);
    }
    
    /**
     * Retrieves a factory by its registered name.
     * 
     * @param name the name of the factory to retrieve
     * @return an Optional containing the factory if found, empty otherwise
     * @throws NullPointerException if name is null
     */
    public static Optional<Factory<?>> getFactory(String name) {
        Objects.requireNonNull(name, "Factory name cannot be null");
        return Optional.ofNullable(factories.get(name.trim()));
    }
    
    /**
     * Retrieves a factory by name with a specific type.
     * 
     * <p>This method performs type checking and returns the factory only
     * if it produces objects of the specified type.</p>
     * 
     * @param <T> the expected product type
     * @param name the name of the factory to retrieve
     * @param expectedType the expected product type class
     * @return an Optional containing the typed factory if found and compatible
     * @throws NullPointerException if name or expectedType is null
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<Factory<T>> getFactory(String name, Class<T> expectedType) {
        Objects.requireNonNull(name, "Factory name cannot be null");
        Objects.requireNonNull(expectedType, "Expected type cannot be null");
        
        Factory<?> factory = factories.get(name.trim());
        if (factory != null && expectedType.isAssignableFrom(factory.getProductType())) {
            return Optional.of((Factory<T>) factory);
        }
        return Optional.empty();
    }
    
    /**
     * Gets all factories that produce objects of the specified type.
     * 
     * @param <T> the product type
     * @param productType the class of the product type
     * @return a list of factories that produce the specified type
     * @throws NullPointerException if productType is null
     */
    @SuppressWarnings("unchecked")
    public static <T> List<Factory<T>> getFactoriesForType(Class<T> productType) {
        Objects.requireNonNull(productType, "Product type cannot be null");
        
        return factories.values().stream()
                .filter(factory -> productType.isAssignableFrom(factory.getProductType()))
                .map(factory -> (Factory<T>) factory)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Gets all registered factories.
     * 
     * @return an unmodifiable collection of all registered factories
     */
    public static Collection<Factory<?>> getAllFactories() {
        return Collections.unmodifiableCollection(factories.values());
    }
    
    /**
     * Gets all registered factory names.
     * 
     * @return an unmodifiable set of all registered factory names
     */
    public static Set<String> getFactoryNames() {
        return Collections.unmodifiableSet(factories.keySet());
    }
    
    /**
     * Checks if a factory with the specified name is registered.
     * 
     * @param name the factory name to check
     * @return true if a factory with the name is registered
     * @throws NullPointerException if name is null
     */
    public static boolean isRegistered(String name) {
        Objects.requireNonNull(name, "Factory name cannot be null");
        return factories.containsKey(name.trim());
    }
    
    /**
     * Unregisters a factory by name.
     * 
     * @param name the name of the factory to unregister
     * @return true if a factory was removed, false if no factory was found
     * @throws NullPointerException if name is null
     */
    public static boolean unregisterFactory(String name) {
        Objects.requireNonNull(name, "Factory name cannot be null");
        return factories.remove(name.trim()) != null;
    }
    
    /**
     * Gets the number of registered factories.
     * 
     * @return the number of registered factories
     */
    public static int getFactoryCount() {
        return factories.size();
    }
    
    /**
     * Clears all registered factories except built-in ones.
     * 
     * <p>This method removes all custom-registered factories but keeps
     * the built-in factories. To remove all factories including built-ins,
     * use {@link #clearAllFactories()}.</p>
     */
    public static void clearCustomFactories() {
        // Store built-in factory names
        Set<String> builtInNames = Set.of("WaveletTransform", "CWT", "WaveletOps", "StreamingDenoiser");
        
        // Remove all factories that are not built-in
        factories.entrySet().removeIf(entry -> !builtInNames.contains(entry.getKey()));
    }
    
    /**
     * Clears all registered factories, including built-in ones.
     * 
     * <p><strong>Warning:</strong> This method removes all factories from the registry.
     * Built-in factories can be restored by calling {@link #registerBuiltInFactories()}.</p>
     */
    public static void clearAllFactories() {
        factories.clear();
    }
    
    /**
     * Resets the registry to its initial state with only built-in factories.
     */
    public static void reset() {
        clearAllFactories();
        registerBuiltInFactories();
    }
}