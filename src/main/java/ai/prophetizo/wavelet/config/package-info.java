/**
 * Configuration classes for customizing wavelet transform behavior.
 * 
 * <p>This package provides configuration options for controlling various aspects
 * of wavelet transform operations, including boundary handling, performance
 * optimization settings, and decomposition parameters.</p>
 * 
 * <h2>Key Classes:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.config.TransformConfig} - Immutable configuration for transform parameters</li>
 * </ul>
 * 
 * <h2>Configuration Options:</h2>
 * <ul>
 *   <li><strong>Boundary Mode</strong> - Controls how edges are handled during convolution operations</li>
 *   <li><strong>Force Scalar</strong> - Forces scalar engine usage instead of auto-detection</li>
 *   <li><strong>Max Decomposition Levels</strong> - Limits the maximum allowed decomposition levels</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create configuration with builder pattern
 * TransformConfig config = TransformConfig.builder()
 *     .boundaryMode(BoundaryMode.PERIODIC)
 *     .forceScalar(true)
 *     .maxDecompositionLevels(5)
 *     .build();
 * 
 * // Use with transform factory
 * WaveletTransformFactory factory = new WaveletTransformFactory()
 *     .withConfig(config);
 * WaveletTransform transform = factory.create(Daubechies.DB4);
 * }</pre>
 * 
 * <p>The configuration system uses the builder pattern to provide flexible,
 * type-safe construction of immutable configuration objects. Default values
 * are provided for all parameters to ensure sensible behavior out of the box.</p>
 * 
 * @see ai.prophetizo.wavelet.WaveletTransformFactory
 * @see ai.prophetizo.wavelet.api.BoundaryMode
 */
package ai.prophetizo.wavelet.config;