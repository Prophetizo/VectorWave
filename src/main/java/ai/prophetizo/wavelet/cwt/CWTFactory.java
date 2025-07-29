package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
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
    
    /**
     * Creates a CWT transform with default configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance
     */
    public static CWTTransform create(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.defaultConfig());
    }
    
    /**
     * Creates a CWT transform with custom configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @param config the configuration
     * @return new CWT transform instance
     */
    public static CWTTransform create(ContinuousWavelet wavelet, CWTConfig config) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        return new CWTTransform(wavelet, config);
    }
    
    /**
     * Creates a CWT transform optimized for real-time processing.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance optimized for real-time
     */
    public static CWTTransform createForRealTime(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.forRealTimeProcessing());
    }
    
    /**
     * Creates a CWT transform optimized for batch processing.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance optimized for batch processing
     */
    public static CWTTransform createForBatchProcessing(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.forBatchProcessing());
    }
    
    /**
     * Creates a CWT transform optimized for Java 23 features.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance with Java 23 optimizations
     */
    public static CWTTransform createOptimizedForJava23(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.optimizedForJava23());
    }
    
    /**
     * Creates a new builder for custom CWT transform configuration.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating customized CWT transforms.
     */
    public static class Builder {
        private ContinuousWavelet wavelet;
        private final CWTConfig.Builder configBuilder;
        
        private Builder() {
            this.configBuilder = CWTConfig.builder();
        }
        
        /**
         * Sets the continuous wavelet.
         * 
         * @param wavelet the wavelet to use
         * @return this builder
         */
        public Builder wavelet(ContinuousWavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        /**
         * Sets the boundary mode.
         * 
         * @param mode boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode mode) {
            configBuilder.boundaryMode(mode);
            return this;
        }
        
        /**
         * Enables or disables FFT acceleration.
         * 
         * @param enable true to enable FFT
         * @return this builder
         */
        public Builder enableFFT(boolean enable) {
            configBuilder.enableFFT(enable);
            return this;
        }
        
        /**
         * Enables or disables scale normalization.
         * 
         * @param normalize true to normalize across scales
         * @return this builder
         */
        public Builder normalizeScales(boolean normalize) {
            configBuilder.normalizeScales(normalize);
            return this;
        }
        
        /**
         * Sets the padding strategy.
         * 
         * @param strategy padding strategy
         * @return this builder
         */
        public Builder paddingStrategy(CWTConfig.PaddingStrategy strategy) {
            configBuilder.paddingStrategy(strategy);
            return this;
        }
        
        /**
         * Sets the FFT size.
         * 
         * @param size FFT size (must be power of 2 or 0 for auto)
         * @return this builder
         */
        public Builder fftSize(int size) {
            configBuilder.fftSize(size);
            return this;
        }
        
        /**
         * Enables or disables scoped values (Java 23).
         * 
         * @param use true to use scoped values
         * @return this builder
         */
        public Builder useScopedValues(boolean use) {
            configBuilder.useScopedValues(use);
            return this;
        }
        
        /**
         * Enables or disables structured concurrency (Java 23).
         * 
         * @param use true to use structured concurrency
         * @return this builder
         */
        public Builder useStructuredConcurrency(boolean use) {
            configBuilder.useStructuredConcurrency(use);
            return this;
        }
        
        /**
         * Enables or disables stream gatherers (Java 23).
         * 
         * @param use true to use stream gatherers
         * @return this builder
         */
        public Builder useStreamGatherers(boolean use) {
            configBuilder.useStreamGatherers(use);
            return this;
        }
        
        /**
         * Builds the CWT transform.
         * 
         * @return new CWT transform instance
         * @throws IllegalStateException if wavelet not set
         */
        public CWTTransform build() {
            if (wavelet == null) {
                throw new IllegalStateException("Wavelet must be set");
            }
            
            CWTConfig config = configBuilder.build();
            return new CWTTransform(wavelet, config);
        }
    }
}