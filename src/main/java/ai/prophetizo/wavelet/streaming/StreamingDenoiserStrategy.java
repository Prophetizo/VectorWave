package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import java.util.concurrent.Flow.Publisher;

/**
 * Strategy interface for streaming wavelet denoising implementations.
 * 
 * <p>This interface defines the contract for streaming denoiser implementations,
 * enabling different algorithms to be used interchangeably through dependency injection.
 * This allows applications to select the most appropriate implementation based on
 * specific requirements such as latency constraints, quality needs, or resource limitations.</p>
 * 
 * <h3>Implementation Types</h3>
 * <ul>
 *   <li><strong>Fast Implementation</strong> ({@link FastStreamingDenoiser})
 *       <ul>
 *         <li>Latency: 0.35-0.70 µs/sample</li>
 *         <li>Throughput: 1.37-2.69 million samples/second</li>
 *         <li>Memory: ~22 KB per instance</li>
 *         <li>Real-time capable: Always</li>
 *         <li>Use case: Audio processing, sensor data, high-frequency trading</li>
 *       </ul>
 *   </li>
 *   <li><strong>Quality Implementation</strong> ({@link QualityStreamingDenoiser})
 *       <ul>
 *         <li>Latency: 0.2-11.4 µs/sample (depends on overlap)</li>
 *         <li>SNR improvement: 4.5 dB better than Fast</li>
 *         <li>Memory: ~26 KB per instance</li>
 *         <li>Real-time capable: Only without overlap</li>
 *         <li>Use case: Offline processing, scientific analysis, medical signals</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Configuration for your use case
 * StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
 *     .wavelet(Daubechies.DB4)
 *     .blockSize(256)
 *     .thresholdMethod(ThresholdMethod.UNIVERSAL)
 *     .adaptiveThreshold(true)
 *     .build();
 * 
 * // Let the factory choose the best implementation
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(config);
 * 
 * // Or explicitly choose an implementation
 * StreamingDenoiserStrategy fastDenoiser = StreamingDenoiserFactory.create(
 *     StreamingDenoiserFactory.Implementation.FAST, config);
 * 
 * // Subscribe to denoised output
 * denoiser.subscribe(new Flow.Subscriber<double[]>() {
 *     public void onNext(double[] denoisedBlock) {
 *         // Process the denoised signal block
 *     }
 *     // ... other subscriber methods
 * });
 * 
 * // Process streaming data
 * while (dataAvailable) {
 *     denoiser.process(getNextSample());
 * }
 * 
 * // Flush remaining data and cleanup
 * denoiser.flush();
 * denoiser.close();
 * }</pre>
 * 
 * <h3>Thread Safety</h3>
 * <p>Implementations are NOT thread-safe. Each instance should be used by a single
 * thread or protected by external synchronization.</p>
 * 
 * <h3>Resource Management</h3>
 * <p>Implementations may use memory pools or other resources that need cleanup.
 * Always call {@link #close()} when done or use try-with-resources:</p>
 * <pre>{@code
 * try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(config)) {
 *     // Use the denoiser
 * }
 * }</pre>
 * 
 * @see StreamingDenoiserFactory
 * @see StreamingDenoiserConfig
 * @see FastStreamingDenoiser
 * @see QualityStreamingDenoiser
 * 
 * @since 1.8.0
 */
public interface StreamingDenoiserStrategy extends Publisher<double[]>, AutoCloseable {
    
    /**
     * Processes a single sample through the denoiser.
     * 
     * @param sample the input sample
     */
    void process(double sample);
    
    /**
     * Processes multiple samples through the denoiser.
     * 
     * @param samples the input samples
     */
    void process(double[] samples);
    
    /**
     * Flushes any remaining samples in the buffer.
     * This should be called when the stream ends to process any buffered data.
     */
    void flush();
    
    /**
     * Checks if the denoiser is ready to process samples.
     * 
     * @return true if ready, false if closed
     */
    boolean isReady();
    
    /**
     * Gets the current statistics for this denoiser.
     * 
     * @return streaming statistics
     */
    StreamingWaveletTransform.StreamingStatistics getStatistics();
    
    /**
     * Gets the block size used by this denoiser.
     * 
     * @return block size in samples
     */
    int getBlockSize();
    
    /**
     * Gets the hop size (non-overlapping portion).
     * 
     * @return hop size in samples
     */
    int getHopSize();
    
    /**
     * Gets the current buffer level.
     * 
     * @return number of samples currently buffered
     */
    int getBufferLevel();
    
    /**
     * Gets the current estimated noise level.
     * 
     * @return noise level estimate
     */
    double getCurrentNoiseLevel();
    
    /**
     * Gets the current threshold being applied.
     * 
     * @return current threshold value
     */
    double getCurrentThreshold();
    
    /**
     * Performance characteristics of the implementation.
     */
    interface PerformanceProfile {
        /**
         * Expected latency per sample in microseconds.
         * 
         * @return latency in microseconds
         */
        double expectedLatencyMicros();
        
        /**
         * Expected SNR improvement in dB.
         * 
         * @return SNR improvement
         */
        double expectedSNRImprovement();
        
        /**
         * Memory usage in bytes.
         * 
         * @return memory usage
         */
        long memoryUsageBytes();
        
        /**
         * Whether this implementation is suitable for real-time processing.
         * 
         * @return true if suitable for real-time
         */
        boolean isRealTimeCapable();
    }
    
    /**
     * Gets the performance profile for this implementation.
     * 
     * @return performance characteristics
     */
    PerformanceProfile getPerformanceProfile();
}