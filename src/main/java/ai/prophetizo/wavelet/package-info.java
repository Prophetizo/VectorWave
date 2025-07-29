/**
 * Core wavelet transform functionality for the VectorWave library.
 * 
 * <p>This package provides the main components for performing Fast Wavelet Transforms (FWT)
 * on discrete signals. It includes the primary transform engine, factory for creating
 * configured transforms, and result containers.</p>
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.WaveletTransform} - Main transform engine for forward and inverse operations</li>
 *   <li>{@link ai.prophetizo.wavelet.WaveletTransformFactory} - Factory for creating configured transform instances</li>
 *   <li>{@link ai.prophetizo.wavelet.TransformResult} - Immutable container for transform coefficients</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a transform using factory
 * WaveletTransformFactory factory = new WaveletTransformFactory()
 *     .boundaryMode(BoundaryMode.PERIODIC);
 * WaveletTransform transform = factory.create(Daubechies.DB4);
 * 
 * // Perform forward transform
 * double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
 * TransformResult result = transform.forward(signal);
 * 
 * // Access coefficients
 * double[] approximation = result.approximationCoeffs();
 * double[] detail = result.detailCoeffs();
 * 
 * // Perform inverse transform
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 * 
 * <h2>Transform Requirements:</h2>
 * <ul>
 *   <li>Input signals must have power-of-2 length</li>
 *   <li>Currently supports single-level transforms only</li>
 *   <li>Supports both periodic and zero-padding boundary modes</li>
 * </ul>
 * 
 * <p>For wavelet selection and types, see {@link ai.prophetizo.wavelet.api}.
 * For configuration options, see {@link ai.prophetizo.wavelet.config}.
 * For exception handling, see {@link ai.prophetizo.wavelet.exception}.</p>
 * 
 * @see ai.prophetizo.wavelet.api
 * @see ai.prophetizo.wavelet.config
 * @see ai.prophetizo.wavelet.exception
 * @since 1.0.0
 */
package ai.prophetizo.wavelet;