/**
 * Internal implementation details for the VectorWave wavelet transform library.
 * 
 * <p><strong>⚠️ Warning:</strong> This package contains internal implementation details
 * that are subject to change without notice. Classes in this package should not be
 * used directly by client code as they may be modified, moved, or removed in future
 * versions without maintaining backward compatibility.</p>
 * 
 * <h2>Purpose:</h2>
 * <p>This package contains the low-level mathematical operations and optimized
 * implementations that power the wavelet transform engine. It provides the core
 * computational primitives used by the public API classes.</p>
 * 
 * <h2>Key Classes:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.internal.ScalarOps} - Scalar implementation of core wavelet operations</li>
 * </ul>
 * 
 * <h2>Core Operations:</h2>
 * <ul>
 *   <li><strong>Convolution and Downsampling</strong> - Core FWT operations with different boundary modes</li>
 *   <li><strong>Upsampling and Convolution</strong> - Core inverse FWT operations</li>
 *   <li><strong>Boundary Handling</strong> - Periodic and zero-padding boundary implementations</li>
 *   <li><strong>Optimized Computations</strong> - Performance-critical mathematical operations</li>
 * </ul>
 * 
 * <h2>Implementation Details:</h2>
 * <p>The {@code ScalarOps} class provides clean, straightforward implementations
 * of the mathematical operations required for wavelet transforms:</p>
 * <ul>
 *   <li>Convolution with downsampling by factor of 2 (analysis)</li>
 *   <li>Upsampling by factor of 2 with convolution (synthesis)</li>
 *   <li>Support for both periodic and zero-padding boundary conditions</li>
 *   <li>Optimized for clarity and correctness rather than maximum performance</li>
 * </ul>
 * 
 * <h2>Usage Note:</h2>
 * <p>Client code should use the public API classes in {@link ai.prophetizo.wavelet}
 * and {@link ai.prophetizo.wavelet.api} packages. This internal package is exposed
 * in JavaDoc for educational purposes and to aid library developers who wish to
 * understand the underlying implementation.</p>
 * 
 * <h2>Future Extensions:</h2>
 * <p>Future versions may include additional optimized implementations such as:</p>
 * <ul>
 *   <li>SIMD-optimized operations for large signals</li>
 *   <li>GPU-accelerated computations</li>
 *   <li>Parallel processing for multi-level transforms</li>
 * </ul>
 * 
 * @see ai.prophetizo.wavelet.WaveletTransform
 */
package ai.prophetizo.wavelet.internal;