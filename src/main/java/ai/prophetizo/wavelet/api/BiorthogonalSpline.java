package ai.prophetizo.wavelet.api;

/**
 * Biorthogonal spline wavelets (biorNr.Nd) family.
 *
 * <p>These wavelets use different filters for decomposition and reconstruction,
 * allowing for symmetric filters which are important in image processing.</p>
 *
 * <p>The naming convention biorNr.Nd means:
 * <ul>
 *   <li>Nr: order of the spline for reconstruction</li>
 *   <li>Nd: order of the spline for decomposition</li>
 * </ul>
 * </p>
 *
 * <p>Common variants: bior1.3, bior2.2, bior3.3, bior4.4</p>
 */
public final class BiorthogonalSpline implements BiorthogonalWavelet {

    // Example: bior1.3 - commonly used for edge detection
    public static final BiorthogonalSpline BIOR1_3 = new BiorthogonalSpline(
            "bior1.3", 1, 3,
            // Decomposition low-pass filter
            new double[]{-0.08838834764831845, 0.08838834764831845,
                    0.7071067811865476, 0.7071067811865476,
                    0.08838834764831845, -0.08838834764831845},
            // Reconstruction low-pass filter
            new double[]{0.35355339059327373, 0.35355339059327373},
            true
    );
    private final String name;
    private final int reconstructionOrder;
    private final int decompositionOrder;
    private final double[] lowPassDecomp;
    private final double[] lowPassRecon;
    private final boolean symmetric;

    private BiorthogonalSpline(String name, int reconOrder, int decompOrder,
                               double[] lowPassDecomp, double[] lowPassRecon,
                               boolean symmetric) {
        this.name = name;
        this.reconstructionOrder = reconOrder;
        this.decompositionOrder = decompOrder;
        // Normalize filters to ensure L2 norm = 1
        this.lowPassDecomp = Wavelet.normalizeToUnitL2Norm(lowPassDecomp);
        this.lowPassRecon = Wavelet.normalizeToUnitL2Norm(lowPassRecon);
        this.symmetric = symmetric;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return String.format("Biorthogonal spline wavelet %s (reconstruction order %d, decomposition order %d)",
                name, reconstructionOrder, decompositionOrder);
    }

    @Override
    public double[] lowPassDecomposition() {
        return lowPassDecomp.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        // Generate high-pass from low-pass reconstruction filter
        double[] h = lowPassRecon;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public double[] lowPassReconstruction() {
        return lowPassRecon.clone();
    }

    @Override
    public double[] highPassReconstruction() {
        // Generate high-pass from low-pass decomposition filter
        double[] h = lowPassDecomp;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public int vanishingMoments() {
        return decompositionOrder;
    }

    @Override
    public int dualVanishingMoments() {
        return reconstructionOrder;
    }

    @Override
    public int splineOrder() {
        return Math.max(reconstructionOrder, decompositionOrder);
    }

    @Override
    public boolean isSymmetric() {
        return symmetric;
    }
}