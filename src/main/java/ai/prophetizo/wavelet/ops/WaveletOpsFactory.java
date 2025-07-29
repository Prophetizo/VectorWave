package ai.prophetizo.wavelet.ops;

import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.api.BoundaryMode;

/**
 * Factory for creating wavelet operation strategies and configurations.
 * 
 * <p>This factory provides a centralized way to create and configure
 * wavelet operation implementations. While the current implementation
 * uses static methods in ScalarOps, this factory provides extensibility
 * for future optimization strategies and operation implementations.</p>
 * 
 * <p>The factory can be extended to support different computational
 * backends (scalar, SIMD, GPU) and optimization strategies based on
 * signal characteristics and system capabilities.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create default operations configuration
 * WaveletOpsConfig config = WaveletOpsFactory.createDefault();
 * 
 * // Create with specific boundary handling
 * WaveletOpsConfig config2 = new WaveletOpsFactory()
 *     .withBoundaryMode(BoundaryMode.ZERO_PADDING)
 *     .withOptimizationLevel(OptimizationLevel.AGGRESSIVE)
 *     .create();
 * }</pre>
 * 
 * @since 1.0.0
 */
public class WaveletOpsFactory implements Factory<WaveletOpsConfig> {
    
    /**
     * Enumeration of available optimization levels for wavelet operations.
     */
    public enum OptimizationLevel {
        /** Basic implementation with minimal optimizations */
        BASIC,
        /** Standard optimizations suitable for most use cases */
        STANDARD, 
        /** Aggressive optimizations that may use more memory */
        AGGRESSIVE
    }
    
    // Configuration parameters
    private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
    private OptimizationLevel optimizationLevel = OptimizationLevel.STANDARD;
    private boolean enableVectorization = true;
    
    /**
     * Creates a default WaveletOpsFactory instance.
     */
    public WaveletOpsFactory() {
    }
    
    /**
     * Creates a default WaveletOpsConfig with standard settings.
     * This is a convenience method for simple use cases.
     * 
     * @return a WaveletOpsConfig instance with default settings
     */
    public static WaveletOpsConfig createDefault() {
        return new WaveletOpsFactory().create();
    }
    
    /**
     * Sets the boundary mode for operations.
     * 
     * @param boundaryMode the boundary mode to use
     * @return this factory for method chaining
     * @throws NullPointerException if boundaryMode is null
     */
    public WaveletOpsFactory withBoundaryMode(BoundaryMode boundaryMode) {
        this.boundaryMode = java.util.Objects.requireNonNull(boundaryMode,
                "boundaryMode cannot be null.");
        return this;
    }
    
    /**
     * Sets the optimization level for operations.
     * 
     * @param level the optimization level to use
     * @return this factory for method chaining
     * @throws NullPointerException if level is null
     */
    public WaveletOpsFactory withOptimizationLevel(OptimizationLevel level) {
        this.optimizationLevel = java.util.Objects.requireNonNull(level,
                "optimization level cannot be null.");
        return this;
    }
    
    /**
     * Configures whether to enable vectorization optimizations.
     * 
     * @param enableVectorization true to enable vectorization
     * @return this factory for method chaining
     */
    public WaveletOpsFactory withVectorization(boolean enableVectorization) {
        this.enableVectorization = enableVectorization;
        return this;
    }
    
    /**
     * Creates a WaveletOpsConfig instance with the current factory configuration.
     * 
     * @return a new WaveletOpsConfig instance
     */
    @Override
    public WaveletOpsConfig create() {
        return new WaveletOpsConfig(boundaryMode, optimizationLevel, enableVectorization);
    }
    
    /**
     * Gets a description of this factory.
     * 
     * @return description of the factory's purpose
     */
    @Override
    public String getDescription() {
        return "Factory for creating wavelet operation configurations and strategies";
    }
    
    /**
     * Gets the product type that this factory creates.
     * 
     * @return WaveletOpsConfig.class
     */
    @Override
    public Class<WaveletOpsConfig> getProductType() {
        return WaveletOpsConfig.class;
    }
    
    /**
     * Gets the current boundary mode setting.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Gets the current optimization level setting.
     * 
     * @return the optimization level
     */
    public OptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }
    
    /**
     * Gets the current vectorization setting.
     * 
     * @return true if vectorization is enabled
     */
    public boolean isVectorizationEnabled() {
        return enableVectorization;
    }
}