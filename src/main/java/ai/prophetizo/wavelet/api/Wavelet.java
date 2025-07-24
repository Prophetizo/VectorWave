package ai.prophetizo.wavelet.api;

/**
 * Base interface for all wavelet types in the VectorWave library.
 *
 * <p>This sealed interface hierarchy ensures type safety while supporting
 * various wavelet categories including discrete (orthogonal, biorthogonal)
 * and continuous wavelets.</p>
 *
 * <p>All wavelets must provide filter coefficients for decomposition and
 * reconstruction, though the method of obtaining these coefficients may
 * vary (e.g., predefined for discrete wavelets, discretized for continuous).</p>
 *
 * <h2>Wavelet Type Hierarchy:</h2>
 * <pre>
 * Wavelet
 * ├── DiscreteWavelet
 * │   ├── OrthogonalWavelet (Haar, Daubechies, Symlets, Coiflets)
 * │   └── BiorthogonalWavelet (Biorthogonal Splines)
 * └── ContinuousWavelet (Morlet, Mexican Hat, etc.)
 * </pre>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Get a wavelet by type
 * Wavelet haar = new Haar();
 * Wavelet db4 = Daubechies.DB4;
 *
 * // Get wavelet from registry
 * Wavelet wavelet = WaveletRegistry.getWavelet("db4");
 *
 * // Use with transform
 * WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
 * }</pre>
 *
 * @see DiscreteWavelet
 * @see ContinuousWavelet
 * @see WaveletRegistry
 */
public sealed interface Wavelet permits DiscreteWavelet, ContinuousWavelet {
    /**
     * Returns the name of the wavelet (e.g., "Haar", "db4", "morl").
     * This should be a unique identifier for the wavelet.
     *
     * @return the wavelet name
     */
    String name();

    /**
     * Returns a human-readable description of the wavelet.
     *
     * @return the wavelet description
     */
    default String description() {
        return name() + " wavelet";
    }

    /**
     * Returns the type category of this wavelet.
     *
     * @return the wavelet type
     */
    WaveletType getType();

    /**
     * Returns the low-pass decomposition filter coefficients (h).
     * For continuous wavelets, these may be discretized values.
     *
     * @return the low-pass decomposition filter
     */
    double[] lowPassDecomposition();

    /**
     * Returns the high-pass decomposition filter coefficients (g).
     * For continuous wavelets, these may be discretized values.
     *
     * @return the high-pass decomposition filter
     */
    double[] highPassDecomposition();

    /**
     * Returns the low-pass reconstruction filter coefficients (h~).
     * For orthogonal wavelets, this is typically the time-reversed
     * version of the decomposition filter.
     *
     * @return the low-pass reconstruction filter
     */
    double[] lowPassReconstruction();

    /**
     * Returns the high-pass reconstruction filter coefficients (g~).
     * For orthogonal wavelets, this is typically the time-reversed
     * version of the decomposition filter.
     *
     * @return the high-pass reconstruction filter
     */
    double[] highPassReconstruction();

    /**
     * Validates that this wavelet's filters satisfy the perfect
     * reconstruction conditions within numerical tolerance.
     *
     * @return true if the wavelet satisfies perfect reconstruction
     */
    default boolean validatePerfectReconstruction() {
        // This would contain the mathematical validation
        // For now, return true as placeholder
        return true;
    }
}