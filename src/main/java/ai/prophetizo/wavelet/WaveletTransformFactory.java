package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.util.NullChecks;

/**
 * Factory for creating WaveletTransform instances.
 *
 * <p>This factory provides a fluent API for configuring and creating
 * WaveletTransform instances with different boundary modes and wavelet types.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Using factory with configuration
 * WaveletTransform transform = new WaveletTransformFactory()
 *     .boundaryMode(BoundaryMode.PERIODIC)
 *     .create(new Haar());
 *
 * // Using default factory method
 * WaveletTransform transform2 = WaveletTransformFactory.createDefault(Daubechies.DB4);
 *
 * // With biorthogonal wavelet
 * WaveletTransform transform3 = new WaveletTransformFactory()
 *     .create(BiorthogonalSpline.BIOR1_3);
 *
 * // With continuous wavelet (discretized)
 * WaveletTransform transform4 = new WaveletTransformFactory()
 *     .create(new MorletWavelet());
 * }</pre>
 */
public class WaveletTransformFactory implements Factory<WaveletTransform, Wavelet> {

    // Default boundary mode is periodic (most common for DWT)
    private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;

    /**
     * Creates a default factory instance.
     */
    public WaveletTransformFactory() {
    }

    /**
     * Creates a WaveletTransform with default settings (periodic boundary mode).
     * This is a convenience method for simple use cases.
     *
     * @param wavelet the wavelet to use
     * @return a WaveletTransform instance
     */
    public static WaveletTransform createDefault(Wavelet wavelet) {
        return new WaveletTransformFactory().create(wavelet);
    }

    /**
     * Sets the boundary mode for transforms created by this factory.
     *
     * @param boundaryMode the boundary mode to use
     * @return this factory for method chaining
     * @throws NullPointerException if boundaryMode is null
     */
    public WaveletTransformFactory boundaryMode(BoundaryMode boundaryMode) {
        this.boundaryMode = NullChecks.requireNonNull(boundaryMode, "boundaryMode");
        return this;
    }

    /**
     * Creates a WaveletTransform instance with the specified wavelet.
     *
     * @param wavelet the wavelet to use
     * @return a configured WaveletTransform instance
     * @throws NullPointerException if wavelet is null
     */
    @Override
    public WaveletTransform create(Wavelet wavelet) {
        NullChecks.requireNonNull(wavelet, "wavelet");
        return new WaveletTransform(wavelet, boundaryMode);
    }

    /**
     * Creates a WaveletTransform with default wavelet (Haar).
     * This satisfies the Factory interface requirement.
     *
     * @return a WaveletTransform with Haar wavelet
     */
    @Override
    public WaveletTransform create() {
        return create(new Haar());
    }

    /**
     * Validates if the given wavelet is valid for creating transforms.
     *
     * @param wavelet the wavelet to validate
     * @return true if the wavelet is valid
     */
    @Override
    public boolean isValidConfiguration(Wavelet wavelet) {
        return wavelet != null;
    }

    /**
     * Gets a description of this factory.
     *
     * @return factory description
     */
    @Override
    public String getDescription() {
        return "Factory for creating WaveletTransform instances with boundary mode: " + boundaryMode;
    }

    /**
     * Gets the current boundary mode setting.
     *
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
}