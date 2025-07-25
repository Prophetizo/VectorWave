package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Factory for creating streaming denoiser implementations.
 * 
 * <p>This factory allows easy selection of the appropriate denoiser implementation
 * based on specific use case requirements:</p>
 * 
 * <ul>
 *   <li><b>FAST</b>: Optimized for real-time processing with minimal latency</li>
 *   <li><b>QUALITY</b>: Optimized for better denoising quality at the cost of performance</li>
 *   <li><b>AUTO</b>: Automatically selects based on configuration parameters</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Real-time audio processing
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
 *     Implementation.FAST,
 *     new StreamingDenoiserConfig.Builder()
 *         .wavelet(Daubechies.DB4)
 *         .blockSize(256)
 *         .overlapFactor(0.5)
 *         .build()
 * );
 * 
 * // High-quality offline processing
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
 *     Implementation.QUALITY,
 *     new StreamingDenoiserConfig.Builder()
 *         .wavelet(Symlet.SYM8)
 *         .blockSize(512)
 *         .overlapFactor(0.75)
 *         .build()
 * );
 * 
 * // Let the factory decide based on configuration
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
 *     Implementation.AUTO,
 *     config
 * );
 * }</pre>
 * 
 * @since 1.8.0
 */
public final class StreamingDenoiserFactory {
    
    /**
     * Available implementations.
     */
    public enum Implementation {
        /**
         * Fast implementation optimized for real-time processing.
         * <ul>
         *   <li>Latency: < 1 µs per sample</li>
         *   <li>Quality: -4.5 to -10.5 dB SNR vs batch</li>
         *   <li>Memory: ~20-22 KB</li>
         *   <li>Real-time capable: Yes</li>
         * </ul>
         */
        FAST,
        
        /**
         * Quality implementation optimized for better denoising.
         * <ul>
         *   <li>Latency: 0.2-11.4 µs per sample (depends on overlap)</li>
         *   <li>Quality: 1.5-7.3 dB better SNR than FAST</li>
         *   <li>Memory: ~26 KB</li>
         *   <li>Real-time capable: Only without overlap</li>
         * </ul>
         */
        QUALITY,
        
        /**
         * Automatic selection based on configuration.
         * <p>Selects FAST if:</p>
         * <ul>
         *   <li>Overlap factor > 0 and adaptive threshold is enabled</li>
         *   <li>Block size < 256</li>
         * </ul>
         * <p>Otherwise selects QUALITY.</p>
         */
        AUTO
    }
    
    // Private constructor to prevent instantiation
    private StreamingDenoiserFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }
    
    /**
     * Creates a streaming denoiser with the specified implementation.
     * 
     * @param implementation the implementation to use
     * @param config the denoiser configuration
     * @return a configured streaming denoiser
     * @throws InvalidArgumentException if configuration is invalid
     */
    public static StreamingDenoiserStrategy create(Implementation implementation, 
                                                  StreamingDenoiserConfig config) {
        if (implementation == null) {
            throw new InvalidArgumentException("Implementation must be specified");
        }
        if (config == null) {
            throw new InvalidArgumentException("Configuration must be specified");
        }
        
        Implementation selected = implementation;
        
        // Auto-select implementation based on configuration
        if (implementation == Implementation.AUTO) {
            selected = selectImplementation(config);
        }
        
        return switch (selected) {
            case FAST -> new FastStreamingDenoiser(config);
            case QUALITY -> new QualityStreamingDenoiser(config);
            case AUTO -> throw new IllegalStateException("AUTO should have been resolved");
        };
    }
    
    /**
     * Creates a streaming denoiser with automatic implementation selection.
     * 
     * @param config the denoiser configuration
     * @return a configured streaming denoiser
     */
    public static StreamingDenoiserStrategy create(StreamingDenoiserConfig config) {
        return create(Implementation.AUTO, config);
    }
    
    /**
     * Automatically selects the best implementation based on configuration.
     */
    private static Implementation selectImplementation(StreamingDenoiserConfig config) {
        // Prioritize real-time capability
        if (config.getOverlapFactor() > 0 && config.isAdaptiveThreshold()) {
            // With overlap and adaptive threshold, FAST is better for real-time
            return Implementation.FAST;
        }
        
        // Small block sizes work better with FAST implementation
        if (config.getBlockSize() < 256) {
            return Implementation.FAST;
        }
        
        // For larger blocks without overlap, QUALITY can be real-time capable
        if (config.getOverlapFactor() == 0) {
            return Implementation.QUALITY;
        }
        
        // Default to FAST for real-time applications
        return Implementation.FAST;
    }
    
    /**
     * Gets performance characteristics for a given implementation and configuration.
     * 
     * @param implementation the implementation type
     * @param config the configuration
     * @return expected performance profile
     */
    public static StreamingDenoiserStrategy.PerformanceProfile getExpectedPerformance(
            Implementation implementation, StreamingDenoiserConfig config) {
        
        Implementation selected = implementation == Implementation.AUTO ? 
            selectImplementation(config) : implementation;
            
        // Create a temporary instance to get its performance profile
        StreamingDenoiserStrategy denoiser = create(selected, config);
        StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();
        
        // Clean up
        try {
            denoiser.close();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        return profile;
    }
}