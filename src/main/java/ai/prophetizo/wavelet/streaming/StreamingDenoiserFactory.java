package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.api.FactoryRegistry;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.ErrorContext;

/**
 * Factory for creating streaming wavelet denoisers.
 * 
 * <p>This factory provides different implementations optimized for various
 * use cases:</p>
 * <ul>
 *   <li>FAST: Low-latency real-time processing</li>
 *   <li>QUALITY: Higher SNR improvement with moderate latency</li>
 * </ul>
 * 
 * <p>The factory automatically selects the best implementation based on
 * configuration when using the {@link #create(StreamingDenoiserConfig)} method.</p>
 * 
 * @since 3.0.0
 */
public final class StreamingDenoiserFactory implements Factory<StreamingDenoiserStrategy, StreamingDenoiserConfig> {
    
    private static final StreamingDenoiserFactory INSTANCE = new StreamingDenoiserFactory();
    
    static {
        // Register with the central factory registry
        FactoryRegistry.getInstance().register(
            "streamingDenoiser", 
            INSTANCE
        );
    }
    
    /**
     * Available implementations.
     */
    public enum Implementation {
        /** Fast implementation for real-time processing */
        FAST,
        /** Quality implementation for better denoising */
        QUALITY
    }
    
    private StreamingDenoiserFactory() {
        // Singleton
    }
    
    /**
     * Gets the singleton factory instance.
     */
    public static StreamingDenoiserFactory getInstance() {
        return INSTANCE;
    }
    
    @Override
    public StreamingDenoiserStrategy create() {
        // Create with default audio configuration
        return create(StreamingDenoiserConfig.defaultAudioConfig());
    }
    
    /**
     * Creates a streaming denoiser with explicit implementation selection.
     * 
     * @param implementation desired implementation
     * @param config configuration
     * @return configured denoiser
     */
    public static StreamingDenoiserStrategy create(Implementation implementation, StreamingDenoiserConfig config) {
        if (implementation == null) {
            throw new InvalidArgumentException(
                ErrorContext.builder("Implementation cannot be null")
                    .withContext("field", "implementation")
                    .withContext("value", "null")
                    .withContext("constraint", "non-null")
                    .withSuggestion("Use Implementation.FAST or QUALITY")
                    .build()
            );
        }
        
        return switch (implementation) {
            case FAST -> new FastStreamingDenoiser(config);
            case QUALITY -> new QualityStreamingDenoiser(config);
        };
    }
    
    @Override
    public StreamingDenoiserStrategy create(StreamingDenoiserConfig config) {
        if (config == null) {
            throw new InvalidArgumentException(
                ErrorContext.builder("Configuration cannot be null")
                    .withContext("field", "config")
                    .withContext("value", "null")
                    .withContext("constraint", "non-null")
                    .withSuggestion("Create config with StreamingDenoiserConfig.Builder")
                    .build()
            );
        }
        
        // Auto-select implementation based on configuration
        if (shouldUseFastImplementation(config)) {
            return new FastStreamingDenoiser(config);
        } else {
            return new QualityStreamingDenoiser(config);
        }
    }
    
    @Override
    public boolean isValidConfiguration(StreamingDenoiserConfig config) {
        if (config == null) return false;
        
        try {
            // Validate by attempting to determine implementation
            shouldUseFastImplementation(config);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Factory for creating streaming wavelet denoisers with FAST and QUALITY implementations";
    }
    
    /**
     * Determines whether to use the FAST implementation for streaming denoising based on the provided configuration.
     * <p>
     * The selection logic prioritizes low-latency, real-time processing when any of the following conditions are met:
     * <ul>
     *   <li><b>Small block size (≤ 256 samples):</b> Indicates a requirement for low latency, as smaller blocks reduce processing delay.</li>
     *   <li><b>High overlap factor (≥ 0.5) <i>and</i> adaptive threshold enabled:</b> Suggests the need for real-time adaptation, where frequent updates and overlapping windows benefit from the FAST implementation's efficiency.</li>
     * </ul>
     * <p>
     * The method returns {@code true} if either the block size is small, or both the overlap factor is high and adaptive thresholding is enabled.
     * This ensures that the FAST implementation is chosen for scenarios where latency and adaptation are critical, while the QUALITY implementation
     * is used otherwise for improved denoising performance at the cost of higher latency.
     * <p>
     * <b>Rationale:</b> These thresholds are empirically chosen to balance latency and denoising quality. Adjust them as needed for different application requirements.
     *
     * @param config the streaming denoiser configuration
     * @return {@code true} if the FAST implementation should be used; {@code false} for QUALITY implementation
     */
    private boolean shouldUseFastImplementation(StreamingDenoiserConfig config) {
        // Use fast implementation for:
        // - Small block sizes (low latency requirement)
        // - High overlap with adaptive threshold (real-time adaptation)
        // - When explicitly optimizing for latency
        
        boolean smallBlock = config.getBlockSize() <= 256;
        boolean highOverlap = config.getOverlapFactor() >= 0.5;
        boolean adaptive = config.isAdaptiveThreshold();
        
        return smallBlock || (highOverlap && adaptive);
    }
}