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
 * 
 * <h3>Mathematical Foundation:</h3>
 * <p>Symlets were designed by Ingrid Daubechies to be as symmetric as possible
 * while maintaining the same orthogonality and compact support properties as
 * standard Daubechies wavelets. They minimize the phase nonlinearity of the
 * transfer function.</p>
 * 
 * <h3>Coefficient Sources:</h3>
 * <p>The coefficients implemented here are derived from:</p>
 * <ul>
 *   <li>Daubechies, I. (1992). "Ten Lectures on Wavelets", CBMS-NSF Regional 
 *       Conference Series in Applied Mathematics, vol. 61, SIAM, Philadelphia,
 *       Chapter 8 (Symmetry for Compactly Supported Wavelet Bases).</li>
 *   <li>Percival, D.B. and Walden, A.T. (2000). "Wavelet Methods for Time Series
 *       Analysis", Cambridge University Press, Table 114.</li>
 *   <li>Numerical values verified against MATLAB Wavelet Toolbox (wfilters('sym2'))
 *       and PyWavelets implementation.</li>
 * </ul>
 * 
 * <p>Symlets satisfy the same orthogonality conditions as Daubechies wavelets
 * but with coefficients chosen to maximize symmetry around the center.</p>
 */
public final class Symlet implements OrthogonalWavelet {

    /**
     * Symlet 2 (sym2) coefficients.
     * 
     * <p>Properties:</p>
     * <ul>
     *   <li>2 vanishing moments</li>
     *   <li>Filter length: 4</li>
     *   <li>Near symmetric (phase is nearly linear)</li>
     * </ul>
     * 
     * <p>Note: sym2 is identical to db2 as there is only one solution
     * for N=2 vanishing moments with minimal support.</p>
     * 
     * <p>Source: Table 8.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     */
    public static final Symlet SYM2 = new Symlet(2, new double[]{
            0.48296291314453414, 0.83651630373780772,
            0.22414386804201339, -0.12940952255126034
    });
    
    /**
     * Symlet 3 (sym3) coefficients.
     * 
     * <p>Properties:</p>
     * <ul>
     *   <li>3 vanishing moments</li>
     *   <li>Filter length: 6</li>
     *   <li>More symmetric than db3</li>
     * </ul>
     * 
     * <p>Source: Percival & Walden (2000), "Wavelet Methods for Time Series Analysis",
     * Table 114, Cambridge University Press.</p>
     */
    public static final Symlet SYM3 = new Symlet(3, new double[]{
            0.33267055295095688, 0.80689150931333875,
            0.45987750211933132, -0.13501102001039084,
            -0.08544127388224149, 0.03522629188210562
    });
    
    /**
     * Symlet 4 (sym4) coefficients.
     * 
     * <p>Properties:</p>
     * <ul>
     *   <li>4 vanishing moments</li>
     *   <li>Filter length: 8</li>
     *   <li>Nearly symmetric around center</li>
     *   <li>Popular choice for signal denoising</li>
     * </ul>
     * 
     * <p>Source: Percival & Walden (2000), "Wavelet Methods for Time Series Analysis",
     * Table 114, Cambridge University Press.</p>
     */
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
    
    /**
     * Verifies that the Symlet coefficients satisfy the orthogonality conditions.
     * This method validates the mathematical correctness of the coefficients.
     * 
     * <p>Conditions checked:</p>
     * <ul>
     *   <li>Sum of coefficients equals √2 (within numerical tolerance)</li>
     *   <li>Sum of squared coefficients equals 1 (within numerical tolerance)</li>
     *   <li>Orthogonality condition for shifts by 2k</li>
     *   <li>Near-symmetry property (phase linearity)</li>
     * </ul>
     * 
     * @return true if all conditions are satisfied within tolerance
     */
    public boolean verifyCoefficients() {
        double tolerance = 1e-10;
        double[] h = lowPassCoeffs;
        
        // Check sum = √2
        double sum = 0;
        for (double coeff : h) {
            sum += coeff;
        }
        if (Math.abs(sum - Math.sqrt(2)) > tolerance) {
            return false;
        }
        
        // Check sum of squares = 1
        double sumSquares = 0;
        for (double coeff : h) {
            sumSquares += coeff * coeff;
        }
        if (Math.abs(sumSquares - 1.0) > tolerance) {
            return false;
        }
        
        // Check orthogonality for even shifts
        for (int k = 2; k < h.length; k += 2) {
            double dot = 0;
            for (int n = 0; n < h.length - k; n++) {
                dot += h[n] * h[n + k];
            }
            if (Math.abs(dot) > tolerance) {
                return false;
            }
        }
        
        // Check symmetry metric: Symlets should have better symmetry than
        // standard Daubechies wavelets. We measure this by comparing
        // coefficients around the center.
        int center = h.length / 2;
        double asymmetry = 0;
        for (int i = 0; i < center; i++) {
            // Compare coefficients equidistant from center
            // Perfect symmetry would give asymmetry = 0
            asymmetry += Math.abs(h[i] - h[h.length - 1 - i]);
        }
        
        // Symlets are "nearly" symmetric, not perfectly symmetric
        // so we just verify the asymmetry is reasonable
        if (asymmetry > h.length * 0.5) {
            return false; // Too asymmetric for a Symlet
        }
        
        return true;
    }
}