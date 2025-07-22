package ai.prophetizo.wavelet.api;

/**
 * Symlet wavelets (symN) are a family of nearly symmetric orthogonal wavelets.
 * They are modifications of Daubechies wavelets with improved symmetry.
 *
 * <p>Symlets are used when near-symmetry is desired while maintaining
 * orthogonality. They have the same number of vanishing moments as
 * Daubechies wavelets of the same order.</p>
 *
 * <p>Common variants: sym2, sym3, sym4, ..., sym20</p>
 */
public final class Symlet implements OrthogonalWavelet {

    // Symlet 2 coefficients (verified from literature)
    // These are the standard sym2 coefficients used in signal processing
    public static final Symlet SYM2 = new Symlet(2, new double[]{
            0.48296291314453414, 0.83651630373780772,
            0.22414386804201339, -0.12940952255126034
    });
    
    // Symlet 3 coefficients
    public static final Symlet SYM3 = new Symlet(3, new double[]{
            0.33267055295095688, 0.80689150931333875,
            0.45987750211933132, -0.13501102001039084,
            -0.08544127388224149, 0.03522629188210562
    });
    
    // Symlet 4 coefficients
    public static final Symlet SYM4 = new Symlet(4, new double[]{
            0.03222310060407815, -0.01260396726226383,
            -0.09921954357695636, 0.29785779560553225,
            0.80373875180591614, 0.49761866763256292,
            -0.02963552764596039, -0.07576571478935668
    });
    private final int order;
    private final String name;
    private final double[] lowPassCoeffs;

    private Symlet(int order, double[] coefficients) {
        this.order = order;
        this.name = "sym" + order;
        this.lowPassCoeffs = coefficients;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Symlet wavelet of order " + order;
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
        return order;
    }
}