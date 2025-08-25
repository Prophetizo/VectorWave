/**
 * Streaming wavelet processing framework built on MODWT.
 * 
 * <p>This package provides high-level streaming interfaces and implementations
 * for real-time wavelet denoising. All implementations use MODWT (Maximal Overlap
 * Discrete Wavelet Transform) for superior streaming performance:</p>
 * 
 * <ul>
 *   <li>Shift-invariance prevents block boundary artifacts</li>
 *   <li>Works with any buffer size (not restricted to powers of 2)</li>
 *   <li>Better continuity across streaming blocks</li>
 *   <li>Maintains precise time alignment with input</li>
 * </ul>
 * 
 * <h2>Key Components</h2>
 * 
 * <h3>{@link ai.prophetizo.wavelet.streaming.StreamingDenoiserStrategy}</h3>
 * <p>Strategy interface for streaming denoisers with two implementations:</p>
 * <ul>
 *   <li>Fast: Low-latency real-time processing</li>
 *   <li>Quality: Higher SNR improvement with moderate latency</li>
 * </ul>
 * 
 * <h3>{@link ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory}</h3>
 * <p>Factory for creating streaming denoisers with automatic implementation
 * selection based on configuration.</p>
 * 
 * <h3>{@link ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig}</h3>
 * <p>Configuration builder for streaming denoisers with sensible defaults
 * for audio and financial data processing.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Configure streaming denoiser
 * StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
 *     .wavelet(Daubechies.DB4)
 *     .blockSize(256)
 *     .overlapFactor(0.5)
 *     .thresholdMethod(ThresholdMethod.UNIVERSAL)
 *     .build();
 * 
 * // Create denoiser (auto-selects best implementation)
 * try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(config)) {
 *     // Subscribe to denoised output
 *     denoiser.subscribe(new Flow.Subscriber<double[]>() {
 *         // Handle denoised blocks...
 *     });
 *     
 *     // Process streaming data
 *     while (hasMoreData()) {
 *         double[] chunk = getNextChunk();
 *         denoiser.process(chunk);
 *     }
 *     
 *     // Flush remaining data
 *     denoiser.flush();
 * }
 * }</pre>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Fast implementation: ~0.1 µs/sample, suitable for real-time</li>
 *   <li>Quality implementation: ~0.3 µs/sample with overlap processing</li>
 *   <li>All implementations use SIMD optimizations when available</li>
 *   <li>Memory usage scales with block size and overlap factor</li>
 * </ul>
 * 
 */
package ai.prophetizo.wavelet.streaming;