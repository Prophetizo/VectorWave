/**
 * VectorWave - A comprehensive Fast Wavelet Transform (FWT) library for Java.
 * 
 * <p>This package contains the main demonstration application for the VectorWave library,
 * showcasing the capabilities of various wavelet types and transform operations.</p>
 * 
 * <p>VectorWave is a pure Java implementation providing:</p>
 * <ul>
 *   <li>Multiple wavelet families (orthogonal, biorthogonal, continuous)</li>
 *   <li>Type-safe API with sealed interface hierarchy</li>
 *   <li>Support for different boundary modes</li>
 *   <li>Zero external dependencies</li>
 *   <li>Comprehensive performance benchmarking</li>
 * </ul>
 * 
 * <p>The main entry point is {@link ai.prophetizo.Main}, which demonstrates
 * transforms using various wavelet types including Haar, Daubechies, Symlets,
 * Coiflets, Biorthogonal Splines, and Morlet wavelets.</p>
 * 
 * <h2>Quick Start Example:</h2>
 * <pre>{@code
 * // Run the demonstration
 * java -cp target/classes ai.prophetizo.Main
 * 
 * // Or use the library programmatically
 * WaveletTransform transform = WaveletTransformFactory.createDefault(new Haar());
 * double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
 * TransformResult result = transform.forward(signal);
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 * 
 * <p>For detailed API usage, see {@link ai.prophetizo.wavelet} and its sub-packages.</p>
 * 
 * @see ai.prophetizo.wavelet
 * @see ai.prophetizo.wavelet.api
 * @since 1.0
 * @version 1.0-SNAPSHOT
 */
package ai.prophetizo;