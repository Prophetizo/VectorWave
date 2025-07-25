package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Factory for creating streaming denoiser implementations.
 *
 * <p>This factory provides a centralized way to create streaming denoiser instances,
 * supporting both manual and automatic implementation selection based on use case
 * requirements. The factory pattern enables easy switching between implementations
 * without changing client code.</p>
 *
 * <h3>Implementation Selection</h3>
 * <p>The factory supports three selection modes:</p>
 * <ul>
 *   <li><b>FAST</b>: Forces the fast implementation for strict real-time requirements</li>
 *   <li><b>QUALITY</b>: Forces the quality implementation for best denoising results</li>
 *   <li><b>AUTO</b>: Intelligently selects based on configuration parameters</li>
 * </ul>
 *
 * <h3>Automatic Selection Logic</h3>
 * <p>When using AUTO mode, the factory considers:</p>
 * <ul>
 *   <li>Overlap factor and adaptive threshold combination</li>
 *   <li>Block size (smaller blocks favor FAST)</li>
 *   <li>Real-time capability requirements</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Example 1: Real-time audio processing (48 kHz)
 * StreamingDenoiserConfig audioConfig = new StreamingDenoiserConfig.Builder()
 *     .wavelet(Daubechies.DB4)
 *     .blockSize(256)        // ~5.3ms latency at 48 kHz
 *     .overlapFactor(0.5)    // 50% overlap for smoothness
 *     .adaptiveThreshold(true)
 *     .build();
 *
 * StreamingDenoiserStrategy audioDenoiser = StreamingDenoiserFactory.create(
 *     Implementation.FAST, audioConfig);
 *
 * // Example 2: High-quality scientific signal processing
 * StreamingDenoiserConfig sciConfig = new StreamingDenoiserConfig.Builder()
 *     .wavelet(Symlet.SYM8)
 *     .blockSize(1024)       // Larger block for better frequency resolution
 *     .overlapFactor(0.75)   // High overlap for quality
 *     .levels(4)             // Multi-level decomposition
 *     .thresholdMethod(ThresholdMethod.SURE)
 *     .build();
 *
 * StreamingDenoiserStrategy sciDenoiser = StreamingDenoiserFactory.create(
 *     Implementation.QUALITY, sciConfig);
 *
 * // Example 3: Let factory choose based on requirements
 * StreamingDenoiserConfig autoConfig = new StreamingDenoiserConfig.Builder()
 *     .wavelet(new Haar())
 *     .blockSize(128)        // Small block size
 *     .overlapFactor(0.0)    // No overlap
 *     .build();
 *
 * // Factory will choose FAST due to small block size
 * StreamingDenoiserStrategy autoDenoiser = StreamingDenoiserFactory.create(autoConfig);
 * }</pre>
 *
 * <h3>Performance Profiling</h3>
 * <p>The factory can provide expected performance characteristics without creating instances:</p>
 * <pre>{@code
 * PerformanceProfile profile = StreamingDenoiserFactory.getExpectedPerformance(
 *     Implementation.QUALITY, config);
 *
 * System.out.println("Expected latency: " + profile.expectedLatencyMicros() + " µs/sample");
 * System.out.println("Real-time capable: " + profile.isRealTimeCapable());
 * }</pre>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li>Use AUTO mode unless you have specific requirements</li>
 *   <li>Always close denoisers when done or use try-with-resources</li>
 *   <li>Consider memory pool settings for multiple instances</li>
 *   <li>Profile your specific use case for optimal configuration</li>
 * </ul>
 *
 * @see StreamingDenoiserStrategy
 * @see StreamingDenoiserConfig
 * @see FastStreamingDenoiser
 * @see QualityStreamingDenoiser
 * @since 1.8.0
 */
public final class StreamingDenoiserFactory {

    // Private constructor to prevent instantiation
    private StreamingDenoiserFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }

    /**
     * Creates a streaming denoiser with the specified implementation.
     *
     * @param implementation the implementation to use
     * @param config         the denoiser configuration
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

        // Resolve AUTO to a concrete implementation
        Implementation selected = (implementation == Implementation.AUTO) 
            ? selectImplementation(config) 
            : implementation;

        // Defensive check to ensure AUTO was resolved
        if (selected == Implementation.AUTO) {
            // This should never happen, but provide a sensible default
            selected = Implementation.FAST;
        }

        return switch (selected) {
            case FAST -> new FastStreamingDenoiser(config);
            case QUALITY -> new QualityStreamingDenoiser(config);
            case AUTO -> new FastStreamingDenoiser(config); // Defensive default
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
     * @param config         the configuration
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
}