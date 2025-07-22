package ai.prophetizo.wavelet.api;

/**
 * The Haar wavelet, the simplest possible wavelet.
 *
 * <p>The Haar wavelet is a step function that represents the simplest
 * orthogonal wavelet. It has compact support and is particularly useful
 * for educational purposes and applications requiring fast computation.</p>
 *
 * <p>Properties:
 * <ul>
 *   <li>Orthogonal</li>
 *   <li>Compact support of width 2</li>
 *   <li>1 vanishing moment</li>
 *   <li>Discontinuous</li>
 * </ul>
 * </p>
 */
public record Haar() implements OrthogonalWavelet {
    private static final double[] DECOMP_COEFFS = {1 / Math.sqrt(2), 1 / Math.sqrt(2)};

    @Override
    public String name() {
        return "Haar";
    }

    @Override
    public String description() {
        return "Haar wavelet - the simplest orthogonal wavelet";
    }

    @Override
    public double[] lowPassDecomposition() {
        return DECOMP_COEFFS.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        return new double[]{DECOMP_COEFFS[1], -DECOMP_COEFFS[0]};
    }

    @Override
    public int vanishingMoments() {
        return 1;
    }
}