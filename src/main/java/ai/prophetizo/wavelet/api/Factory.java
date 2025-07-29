package ai.prophetizo.wavelet.api;

/**
 * Common interface for all factory implementations in the VectorWave library.
 * 
 * <p>This interface provides a standardized factory pattern that ensures consistency
 * across different factory implementations. Factories are responsible for creating
 * and configuring complex objects with appropriate validation and default settings.</p>
 * 
 * <p>All factory implementations should:</p>
 * <ul>
 *   <li>Provide fluent configuration methods that return the factory instance</li>
 *   <li>Implement proper validation of configuration parameters</li>
 *   <li>Support both default and parameterized creation methods</li>
 *   <li>Ensure thread-safety for configuration and creation operations</li>
 * </ul>
 * 
 * <p>Example implementation pattern:</p>
 * <pre>{@code
 * public class MyFactory implements Factory<MyProduct> {
 *     private ConfigType config = DEFAULT_CONFIG;
 *     
 *     public MyFactory withConfiguration(ConfigType config) {
 *         this.config = Objects.requireNonNull(config);
 *         return this;
 *     }
 *     
 *     @Override
 *     public MyProduct create() {
 *         return new MyProduct(config);
 *     }
 *     
 *     public MyProduct create(ParameterType param) {
 *         Objects.requireNonNull(param);
 *         return new MyProduct(config, param);
 *     }
 *     
 *     public static MyProduct createDefault(ParameterType param) {
 *         return new MyFactory().create(param);
 *     }
 * }
 * }</pre>
 * 
 * @param <T> the type of object this factory creates
 * 
 * @since 1.0.0
 */
public interface Factory<T> {
    
    /**
     * Creates an instance of type T using the current factory configuration.
     * 
     * <p>This method should create a new instance each time it is called,
     * using the factory's current configuration state. Implementations must
     * ensure that the returned object is properly initialized and validated.</p>
     * 
     * <p>For factories that require mandatory parameters, this method may
     * throw an exception or create a default instance - the behavior should
     * be clearly documented in the implementing class.</p>
     * 
     * @return a new instance of type T
     * @throws IllegalStateException if the factory is not in a valid state for creation
     * @throws UnsupportedOperationException if parameterless creation is not supported
     */
    T create();
    
    /**
     * Gets a human-readable description of what this factory creates.
     * 
     * <p>This method is useful for debugging, logging, and factory registry
     * implementations that need to describe available factories.</p>
     * 
     * @return a description of the factory's purpose and product type
     */
    default String getDescription() {
        return "Factory for " + getClass().getSimpleName().replace("Factory", "");
    }
    
    /**
     * Gets the product type that this factory creates.
     * 
     * <p>This is a convenience method for reflection-based scenarios
     * and factory registries that need to categorize factories by their
     * product types.</p>
     * 
     * <p>Default implementation attempts to extract the generic type parameter,
     * but implementations should override this method to provide accurate
     * type information.</p>
     * 
     * @return the Class object representing the product type
     */
    default Class<T> getProductType() {
        // This is a basic implementation - specific factories should override
        // to provide accurate type information
        @SuppressWarnings("unchecked")
        Class<T> result = (Class<T>) Object.class;
        return result;
    }
}