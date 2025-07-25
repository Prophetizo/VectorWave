package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import java.util.concurrent.Flow.Publisher;

/**
 * Strategy interface for streaming wavelet denoising implementations.
 * 
 * <p>This interface allows different denoising algorithms to be used interchangeably
 * through dependency injection, enabling the selection of the most appropriate
 * implementation based on specific use case requirements.</p>
 * 
 * <h3>Available Implementations:</h3>
 * <ul>
 *   <li>{@link FastStreamingDenoiser} - Optimized for speed (< 1 µs/sample)</li>
 *   <li>{@link QualityStreamingDenoiser} - Optimized for quality (better SNR)</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // For real-time applications with strict latency requirements
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
 *     StreamingDenoiserFactory.Implementation.FAST,
 *     new StreamingDenoiserConfig.Builder()
 *         .wavelet(Daubechies.DB4)
 *         .blockSize(256)
 *         .build()
 * );
 * 
 * // For offline processing where quality is paramount
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
 *     StreamingDenoiserFactory.Implementation.QUALITY,
 *     new StreamingDenoiserConfig.Builder()
 *         .wavelet(Daubechies.DB4)
 *         .blockSize(256)
 *         .overlapFactor(0.5)
 *         .build()
 * );
 * }</pre>
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