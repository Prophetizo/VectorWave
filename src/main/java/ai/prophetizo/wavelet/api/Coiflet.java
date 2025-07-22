package ai.prophetizo.wavelet.api;

/**
 * Coiflet wavelets (coifN) are a family of orthogonal wavelets designed
 * to have vanishing moments for both the wavelet and scaling functions.
 *
 * <p>Coiflets were designed by Ingrid Daubechies at the request of
 * Ronald Coifman. They have better symmetry properties than standard
 * Daubechies wavelets.</p>
 *
 * <p>Common variants: coif1, coif2, coif3, coif4, coif5</p>
 */
public final class Coiflet implements OrthogonalWavelet {

    // Coiflet 1 coefficients (6 coefficients)
    // These are the standard COIF1 coefficients used in signal processing
    public static final Coiflet COIF1 = new Coiflet(1, new double[]{
            -0.01565572813546454,
            -0.07273261951285611,
             0.38486484686420286,
             0.85257202021225542,
             0.33789766245780922,
            -0.07273261951285611
    });
    
    // Coiflet 2 coefficients (12 coefficients)
    public static final Coiflet COIF2 = new Coiflet(2, new double[]{
            -0.00072054944536451221,
            -0.00182320887091316802,
             0.00562114317110652447,
             0.02359620771620165462,
            -0.05942743678554536299,
            -0.07644214234475313916,
             0.41700518442169254227,
             0.81272363544554227114,
             0.38611006682505316398,
            -0.06737255472196302711,
            -0.04146493678175812849,
             0.01640642779780577506
    });
    
    // Coiflet 3 coefficients (18 coefficients)
    public static final Coiflet COIF3 = new Coiflet(3, new double[]{
            -0.0000345997728362,
            -0.0000709833031381,
             0.0004662169601129,
             0.0011175187708906,
            -0.0025745176887502,
            -0.0090079761366615,
             0.0158805448636158,
             0.0345550275730615,
            -0.0823019271068856,
            -0.0717998216193117,
             0.4284834763776168,
             0.7937772226256169,
             0.4051769024096150,
            -0.0611233900026726,
            -0.0657719112818552,
             0.0234526961418362,
             0.0077825964273254,
            -0.0037935128644910
    });
    private final int order;
    private final String name;
    private final double[] lowPassCoeffs;

    private Coiflet(int order, double[] coefficients) {
        this.order = order;
        this.name = "coif" + order;
        this.lowPassCoeffs = coefficients;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Coiflet wavelet of order " + order;
    }

    @Override
    public double[] lowPassDecomposition() {
        return lowPassCoeffs.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        // Generate high-pass from low-pass using quadrature mirror filter
        double[] h = lowPassCoeffs;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public int vanishingMoments() {
        return 2 * order;
    }
}