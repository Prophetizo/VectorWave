package ai.prophetizo.wavelet.cwt.optimization;

/**
 * Configuration options for FFT algorithm selection.
 * 
 * <p>This enum allows choosing between different FFT implementations
 * to optimize for different use cases and performance requirements.</p>
 *
 * @since 1.0.0
 */
public enum FFTAlgorithm {
    
    /**
     * Standard recursive Cooley-Tukey FFT implementation.
     * 
     * <p>This is the original implementation that creates temporary arrays
     * and recalculates twiddle factors. It provides good numerical stability
     * and is suitable for small to medium sized transforms.</p>
     * 
     * <p><strong>Characteristics:</strong></p>
     * <ul>
     *   <li>Higher memory usage due to temporary arrays</li>
     *   <li>Recalculates twiddle factors each time</li>
     *   <li>Excellent numerical stability</li>
     *   <li>Simple implementation</li>
     * </ul>
     */
    STANDARD,
    
    /**
     * Optimized in-place FFT with pre-computed twiddle factors.
     * 
     * <p>This implementation provides significant performance improvements
     * through in-place computation, cached twiddle factors, and iterative
     * algorithm. Expected 30-50% speedup with reduced memory allocation.</p>
     * 
     * <p><strong>Characteristics:</strong></p>
     * <ul>
     *   <li>Lower memory usage (in-place computation)</li>
     *   <li>Pre-computed and cached twiddle factors</li>
     *   <li>Better cache efficiency</li>
     *   <li>30-50% performance improvement</li>
     * </ul>
     */
    OPTIMIZED
}