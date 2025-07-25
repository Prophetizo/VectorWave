/**
 * Streaming and real-time wavelet transform implementations.
 *
 * <p>This package provides streaming implementations of wavelet transforms and denoising
 * algorithms optimized for real-time signal processing applications.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>Streaming Transforms</h3>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.streaming.StreamingWaveletTransform} - Base interface for streaming transforms</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.StreamingWaveletTransformImpl} - Core streaming transform implementation</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.SlidingWindowTransform} - Sliding window approach for continuous streams</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.MultiLevelStreamingTransform} - Multi-level decomposition for streams</li>
 * </ul>
 *
 * <h3>Streaming Denoising</h3>
 * <p>The package provides a flexible streaming denoiser with two optimized implementations:</p>
 *
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.streaming.StreamingDenoiserStrategy} - Strategy interface for denoiser implementations</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory} - Factory for creating denoiser instances</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.FastStreamingDenoiser} - Low-latency implementation (&lt; 1 µs/sample)</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.QualityStreamingDenoiser} - Higher quality with overlapping transforms</li>
 * </ul>
 *
 * <h3>Supporting Components</h3>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.streaming.OverlapBuffer} - Overlap-add processing with windowing</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.NoiseEstimator} - Real-time noise level estimation</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.StreamingThresholdAdapter} - Adaptive threshold adjustment</li>
 *   <li>{@link ai.prophetizo.wavelet.streaming.SharedMemoryPoolManager} - Memory pool for reduced allocation overhead</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Streaming Transform</h3>
 * <pre>{@code
 * StreamingWaveletTransform transform = new StreamingWaveletTransformImpl(
 *     new Haar(),
 *     BoundaryMode.PERIODIC
 * );
 *
 * // Subscribe to transform results
 * transform.subscribe(new Flow.Subscriber<TransformResult>() {
 *     public void onNext(TransformResult result) {
 *         // Process transform coefficients
 *     }
 *     // ... other subscriber methods
 * });
 *
 * // Process streaming data
 * transform.getInputPublisher().submit(dataChunk);
 * }</pre>
 *
 * <h3>Real-time Denoising</h3>
 * <pre>{@code
 * // Configuration
 * StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
 *     .wavelet(Daubechies.DB4)
 *     .blockSize(256)
 *     .overlapFactor(0.5)
 *     .thresholdMethod(ThresholdMethod.UNIVERSAL)
 *     .adaptiveThreshold(true)
 *     .build();
 *
 * // Create denoiser with automatic implementation selection
 * StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(config);
 *
 * // Or explicitly choose implementation
 * StreamingDenoiserStrategy fastDenoiser = StreamingDenoiserFactory.create(
 *     StreamingDenoiserFactory.Implementation.FAST, config);
 *
 * // Subscribe to denoised output
 * denoiser.subscribe(new Flow.Subscriber<double[]>() {
 *     public void onNext(double[] denoisedBlock) {
 *         // Process denoised signal
 *     }
 *     // ... other subscriber methods
 * });
 *
 * // Process samples
 * denoiser.process(samples);
 * }</pre>
 *
 * <h2>Performance Characteristics</h2>
 *
 * <h3>Fast Implementation</h3>
 * <ul>
 *   <li>Latency: 0.35-0.70 µs per sample</li>
 *   <li>Throughput: 1.37-2.69 million samples/second</li>
 *   <li>Memory: ~22 KB per instance</li>
 *   <li>Real-time capable: Always</li>
 *   <li>Use case: Real-time audio, sensor data, high-frequency trading</li>
 * </ul>
 *
 * <h3>Quality Implementation</h3>
 * <ul>
 *   <li>Latency: 0.2-11.4 µs per sample (depends on overlap)</li>
 *   <li>SNR improvement: 4.5 dB better than Fast implementation</li>
 *   <li>Memory: ~26 KB per instance</li>
 *   <li>Real-time capable: Only without overlap</li>
 *   <li>Use case: Offline processing, quality-critical applications</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * <p>The streaming implementations follow these key principles:</p>
 * <ul>
 *   <li><strong>Bounded memory</strong>: O(1) memory complexity regardless of stream length</li>
 *   <li><strong>Low latency</strong>: Minimal processing delay for real-time applications</li>
 *   <li><strong>Reactive streams</strong>: Using Java Flow API for backpressure handling</li>
 *   <li><strong>Configurable trade-offs</strong>: Quality vs. latency based on application needs</li>
 *   <li><strong>Adaptive processing</strong>: Dynamic threshold and noise estimation</li>
 * </ul>
 *
 * <h2>Implementation Notes</h2>
 *
 * <h3>Block Processing</h3>
 * <p>Streaming transforms process data in fixed-size blocks (must be powers of 2).
 * The block size affects:</p>
 * <ul>
 *   <li>Latency: Larger blocks = higher latency</li>
 *   <li>Quality: Larger blocks = better frequency resolution</li>
 *   <li>Memory: Larger blocks = more memory usage</li>
 * </ul>
 *
 * <h3>Overlap Processing</h3>
 * <p>The Quality implementation supports overlapping transforms with configurable
 * overlap factors (0.0 to 0.875). Overlap processing:</p>
 * <ul>
 *   <li>Reduces block boundary artifacts</li>
 *   <li>Improves reconstruction quality</li>
 *   <li>Increases computational cost</li>
 *   <li>May prevent real-time operation at high overlap factors</li>
 * </ul>
 *
 * <h3>Memory Management</h3>
 * <p>The implementations use memory pooling to reduce GC pressure:</p>
 * <ul>
 *   <li>Shared pool: Multiple instances share a common memory pool</li>
 *   <li>Dedicated pool: Each instance has its own pool</li>
 *   <li>Array recycling: Reuse arrays across processing blocks</li>
 * </ul>
 *
 * @since 1.0.0
 */
package ai.prophetizo.wavelet.streaming;