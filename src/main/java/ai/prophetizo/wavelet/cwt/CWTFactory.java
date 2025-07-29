package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;

/**
 * Factory for creating Continuous Wavelet Transform (CWT) computation engines.
 * 
 * <p>This factory provides a standardized way to create and configure CWT
 * computation instances, particularly the FFT-accelerated implementation for
 * efficient frequency-domain operations.</p>
 * 
 * <p>The factory supports configuration of computational parameters and
 * optimization settings for different use cases and performance requirements.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create default FFT-accelerated CWT engine
 * FFTAcceleratedCWT cwt = CWTFactory.createDefault();
 * 
 * // Create with custom configuration
 * FFTAcceleratedCWT cwt2 = new CWTFactory()
 *     .withOptimizations(true)
 *     .create();
 * }</pre>
 * 
 * @since 1.0.0
 */
public class CWTFactory implements Factory<FFTAcceleratedCWT> {
    
    // Configuration flags for CWT creation
    private boolean enableOptimizations = true;
    
    /**
     * Creates a default CWTFactory instance.
     */
    public CWTFactory() {
    }
    
    /**
     * Creates a default FFTAcceleratedCWT instance with standard optimizations.
     * This is a convenience method for simple use cases.
     * 
     * @return a FFTAcceleratedCWT instance with default settings
     */
    public static FFTAcceleratedCWT createDefault() {
        return new CWTFactory().create();
    }
    
    /**
     * Configures whether to enable performance optimizations.
     * 
     * @param enableOptimizations true to enable optimizations, false otherwise
     * @return this factory for method chaining
     */
    public CWTFactory withOptimizations(boolean enableOptimizations) {
        this.enableOptimizations = enableOptimizations;
        return this;
    }
    
    /**
     * Creates an FFTAcceleratedCWT instance with the current configuration.
     * 
     * @return a new FFTAcceleratedCWT instance
     */
    @Override
    public FFTAcceleratedCWT create() {
        // Currently FFTAcceleratedCWT doesn't take constructor parameters,
        // but this provides extensibility for future configuration options
        return new FFTAcceleratedCWT();
    }
    
    /**
     * Gets a description of this factory.
     * 
     * @return description of the factory's purpose
     */
    @Override
    public String getDescription() {
        return "Factory for creating FFT-accelerated Continuous Wavelet Transform engines";
    }
    
    /**
     * Gets the product type that this factory creates.
     * 
     * @return FFTAcceleratedCWT.class
     */
    @Override
    public Class<FFTAcceleratedCWT> getProductType() {
        return FFTAcceleratedCWT.class;
    }
    
    /**
     * Gets the current optimization setting.
     * 
     * @return true if optimizations are enabled
     */
    public boolean isOptimizationsEnabled() {
        return enableOptimizations;
    }
}