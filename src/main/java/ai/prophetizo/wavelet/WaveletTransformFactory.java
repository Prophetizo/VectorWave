package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.api.Wavelet;

import java.util.Objects;

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
 *     .withBoundaryMode(BoundaryMode.PERIODIC)
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
public class WaveletTransformFactory implements Factory<WaveletTransform> {

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
    public WaveletTransformFactory withBoundaryMode(BoundaryMode boundaryMode) {
        this.boundaryMode = Objects.requireNonNull(boundaryMode,
                "boundaryMode cannot be null.");
        return this;
    }

    /**
     * Creates a WaveletTransform instance with the specified wavelet.
     *
     * @param wavelet the wavelet to use
     * @return a configured WaveletTransform instance
     * @throws NullPointerException if wavelet is null
     */
    public WaveletTransform create(Wavelet wavelet) {
        Objects.requireNonNull(wavelet, "wavelet cannot be null.");
        return new WaveletTransform(wavelet, boundaryMode);
    }

    /**
     * Creates a WaveletTransform instance using default configuration.
     * 
     * <p>This method implements the Factory interface contract but requires
     * a wavelet parameter for meaningful creation. Since no default wavelet
     * is appropriate for all use cases, this method throws an exception.</p>
     * 
     * <p>Use {@link #create(Wavelet)} or {@link #createDefault(Wavelet)} instead.</p>
     * 
     * @throws UnsupportedOperationException always, as wavelet parameter is required
     */
    @Override
    public WaveletTransform create() {
        throw new UnsupportedOperationException(
            "WaveletTransformFactory requires a wavelet parameter. " +
            "Use create(Wavelet) or createDefault(Wavelet) instead.");
    }

    /**
     * Gets a description of this factory.
     * 
     * @return description of the factory's purpose
     */
    @Override
    public String getDescription() {
        return "Factory for creating WaveletTransform instances with configurable boundary modes";
    }

    /**
     * Gets the product type that this factory creates.
     * 
     * @return WaveletTransform.class
     */
    @Override
    public Class<WaveletTransform> getProductType() {
        return WaveletTransform.class;
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