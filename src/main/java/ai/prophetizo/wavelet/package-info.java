/**
 * Core wavelet transform functionality for the VectorWave library.
 * 
 * <p>This package provides the main components for performing wavelet transforms
 * on discrete signals, focusing on the MODWT (Maximal Overlap Discrete Wavelet Transform)
 * which offers shift-invariance and works with signals of any length.</p>
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.WaveletOperations} - Public facade for core operations with automatic optimization</li>
 *   <li>{@link ai.prophetizo.wavelet.modwt.MODWTTransform} - Main MODWT transform engine</li>
 *   <li>{@link ai.prophetizo.wavelet.modwt.MODWTResult} - Immutable container for transform coefficients</li>
 *   <li>{@link ai.prophetizo.wavelet.modwt.MODWTTransform#forwardBatch} - High-performance batch processing</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a MODWT transform
 * MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Perform forward transform on any length signal
 * double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};  // Not power-of-2!
 * MODWTResult result = transform.forward(signal);
 * 
 * // Access coefficients (same length as input)
 * double[] approximation = result.approximationCoeffs();
 * double[] detail = result.detailCoeffs();
 * 
 * // Perform inverse transform
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 * 
 * <h2>MODWT Advantages:</h2>
 * <ul>
 *   <li>Works with signals of any length (not restricted to power-of-2)</li>
 *   <li>Shift-invariant (translation-invariant)</li>
 *   <li>Same-length output coefficients</li>
 *   <li>Better for time series analysis</li>
 * </ul>
 * 
 * <p>For wavelet selection and types, see {@link ai.prophetizo.wavelet.api}.
 * For configuration options, see {@link ai.prophetizo.wavelet.config}.
 * For exception handling, see {@link ai.prophetizo.wavelet.exception}.</p>
 * 
 * @see ai.prophetizo.wavelet.modwt
 * @see ai.prophetizo.wavelet.api
 * @see ai.prophetizo.wavelet.config
 * @see ai.prophetizo.wavelet.exception
 */
package ai.prophetizo.wavelet;