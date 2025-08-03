package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

import java.util.Objects;

/**
 * Factory for creating MODWT (Maximal Overlap Discrete Wavelet Transform) instances.
 *
 * <p>This factory provides a fluent interface for configuring and creating
 * {@link MODWTTransform} instances with various settings. It follows the same
 * pattern as {@link WaveletTransformFactory} for consistency.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * MODWTTransformFactory factory = new MODWTTransformFactory()
 *     .withBoundaryMode(BoundaryMode.PERIODIC);
 *
 * MODWTTransform transform = factory.create(new Haar());
 * }</pre>
 */
public class MODWTTransformFactory {

    private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;

    /**
     * Sets the boundary mode for transforms created by this factory.
     *
     * @param boundaryMode the boundary mode to use
     * @return this factory instance for method chaining
     * @throws NullPointerException if boundaryMode is null
     */
    public MODWTTransformFactory withBoundaryMode(BoundaryMode boundaryMode) {
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        return this;
    }

    /**
     * Creates a new MODWT transform with the specified wavelet and the
     * factory's configured settings.
     *
     * @param wavelet the wavelet to use for the transform
     * @return a new MODWTTransform instance
     * @throws NullPointerException if wavelet is null
     */
    public MODWTTransform create(Wavelet wavelet) {
        return new MODWTTransform(wavelet, boundaryMode);
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