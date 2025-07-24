package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;

import java.util.concurrent.Flow;

/**
 * Streaming wavelet transform for processing continuous data streams.
 *
 * <p>This interface enables real-time wavelet analysis of streaming data without
 * requiring the entire signal to be available in memory. It's particularly useful for:</p>
 * <ul>
 *   <li>Live audio processing</li>
 *   <li>Real-time financial data analysis</li>
 *   <li>Continuous sensor monitoring</li>
 *   <li>Network packet analysis</li>
 * </ul>
 *
 * <p>The implementation uses Java's Flow API for reactive stream processing,
 * providing backpressure support and efficient resource management.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * StreamingWaveletTransform transform = StreamingWaveletTransform.create(
 *     Daubechies.DB4,
 *     BoundaryMode.PERIODIC,
 *     512  // block size
 * );
 *
 * // Subscribe to transform results
 * transform.subscribe(new Flow.Subscriber<TransformResult>() {
 *     // Handle results...
 * });
 *
 * // Feed data into the transform
 * transform.process(dataChunk);
 * }</pre>
 *
 * @since 1.5.0
 */
public interface StreamingWaveletTransform extends Flow.Publisher<TransformResult>, AutoCloseable {

    /**
     * Create a streaming wavelet transform with default block size.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @return a new streaming transform
     */
    static StreamingWaveletTransform create(Wavelet wavelet, BoundaryMode boundaryMode) {
        return create(wavelet, boundaryMode, 512); // Default block size
    }

    /**
     * Create a streaming wavelet transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param blockSize    the processing block size (must be power of 2)
     * @return a new streaming transform
     * @throws InvalidArgumentException if blockSize is not a power of 2
     */
    static StreamingWaveletTransform create(Wavelet wavelet, BoundaryMode boundaryMode, int blockSize) {
        return new StreamingWaveletTransformImpl(wavelet, boundaryMode, blockSize);
    }

    /**
     * Create a multi-level streaming wavelet transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param blockSize    the processing block size
     * @param levels       number of decomposition levels
     * @return a new multi-level streaming transform
     */
    static StreamingWaveletTransform createMultiLevel(
            Wavelet wavelet, BoundaryMode boundaryMode, int blockSize, int levels) {
        return new MultiLevelStreamingTransform(wavelet, boundaryMode, blockSize, levels);
    }

    /**
     * Process a chunk of streaming data.
     *
     * @param data the data chunk to process
     * @throws InvalidSignalException if data is null or empty
     * @throws IllegalStateException  if the transform is closed
     */
    void process(double[] data);

    /**
     * Process a single sample.
     *
     * @param sample the sample value
     * @throws IllegalStateException if the transform is closed
     */
    void process(double sample);

    /**
     * Flush any buffered data and emit final results.
     * Call this when the stream ends to process any remaining samples.
     */
    void flush();

    /**
     * Get the block size used for processing.
     *
     * @return the block size in samples
     */
    int getBlockSize();

    /**
     * Get the current buffer fill level.
     *
     * @return number of samples currently buffered
     */
    int getBufferLevel();

    /**
     * Check if the transform is ready to process more data.
     *
     * @return true if ready for more data
     */
    boolean isReady();

    /**
     * Get statistics about the streaming performance.
     *
     * @return performance statistics
     */
    StreamingStatistics getStatistics();

    /**
     * Statistics for streaming transform performance.
     */
    interface StreamingStatistics {
        /**
         * Total number of samples processed.
         */
        long getSamplesProcessed();

        /**
         * Total number of blocks emitted.
         */
        long getBlocksEmitted();

        /**
         * Average processing time per block in nanoseconds.
         */
        double getAverageProcessingTime();

        /**
         * Maximum processing time for a single block.
         */
        long getMaxProcessingTime();

        /**
         * Number of buffer overruns (data loss).
         */
        long getOverruns();

        /**
         * Current throughput in samples per second.
         */
        double getThroughput();
    }
}
