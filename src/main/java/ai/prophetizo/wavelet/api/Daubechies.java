package ai.prophetizo.wavelet.api;

/**
 * The Daubechies family of orthogonal wavelets.
 *
 * <p>Daubechies wavelets are a family of orthogonal wavelets with compact
 * support and the maximum number of vanishing moments for a given support
 * width. They are widely used in signal processing and data compression.</p>
 *
 * <p>Properties:
 * <ul>
 *   <li>Orthogonal</li>
 *   <li>Compact support</li>
 *   <li>Asymmetric (except Haar/DB1)</li>
 *   <li>Smooth (smoothness increases with order)</li>
 * </ul>
 * </p>
 *
 * <p>Common variants: DB2, DB3, DB4, ..., DB10</p>
 *
 * <h3>Mathematical Foundation:</h3>
 * <p>Daubechies wavelets were developed by Ingrid Daubechies in 1988. They are
 * constructed to have the maximum number of vanishing moments for a given filter
 * length, making them optimal for representing polynomial signals.</p>
 *
 * <h3>Coefficient Sources:</h3>
 * <p>The coefficients implemented here are derived from:</p>
 * <ul>
 *   <li>Daubechies, I. (1988). "Orthonormal bases of compactly supported wavelets",
 *       Communications on Pure and Applied Mathematics, 41(7), pp. 909-996.</li>
 *   <li>Daubechies, I. (1992). "Ten Lectures on Wavelets", CBMS-NSF Regional
 *       Conference Series in Applied Mathematics, vol. 61, SIAM, Philadelphia.</li>
 *   <li>Numerical values verified against MATLAB Wavelet Toolbox and PyWavelets.</li>
 * </ul>
 *
 * <p>The coefficients satisfy the following constraints:</p>
 * <ul>
 *   <li>Σh[n] = √2 (DC gain normalization)</li>
 *   <li>Σh[n]² = 1 (energy normalization)</li>
 *   <li>Σh[n]h[n+2k] = 0 for k ≠ 0 (orthogonality)</li>
 *   <li>Σn^p h[n] = 0 for p = 0, 1, ..., N-1 (vanishing moments)</li>
 * </ul>
 */
public record Daubechies(String name, double[] lowPassCoeffs, int order) implements OrthogonalWavelet {

    /**
     * Daubechies 2 (DB2) coefficients.
     *
     * <p>Properties:</p>
     * <ul>
     *   <li>2 vanishing moments</li>
     *   <li>Filter length: 4</li>
     *   <li>Support width: 3</li>
     * </ul>
     *
     * <p>Source: Table 6.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     */
    public static final Daubechies DB2 = new Daubechies(
            "db2",
            new double[]{0.4829629131445341, 0.8365163037378079, 0.2241438680420134, -0.1294095225512603},
            2
    );

    /**
     * Daubechies 4 (DB4) coefficients.
     *
     * <p>Properties:</p>
     * <ul>
     *   <li>4 vanishing moments</li>
     *   <li>Filter length: 8</li>
     *   <li>Support width: 7</li>
     *   <li>Better frequency selectivity than DB2</li>
     * </ul>
     *
     * <p>Source: Table 6.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     */
    public static final Daubechies DB4 = new Daubechies(
            "db4",
            new double[]{
                    0.2303778133088964, 0.7148465705529154, 0.6308807679298587, -0.0279837693982488,
                    -0.1870348117190931, 0.0308413818355607, 0.0328830116668852, -0.0105974017850690
            },
            4
    );

    /**
     * Daubechies 6 (DB6) coefficients.
     *
     * <p>Properties:</p>
     * <ul>
     *   <li>6 vanishing moments</li>
     *   <li>Filter length: 12</li>
     *   <li>Support width: 11</li>
     *   <li>Smoother than DB4, better for continuous signals</li>
     * </ul>
     *
     * <p>Source: Table 6.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     * <p>Verified against MATLAB Wavelet Toolbox and PyWavelets</p>
     */
    public static final Daubechies DB6 = new Daubechies(
            "db6",
            new double[]{
                    0.1115407433501094, 0.4946238903984530, 0.7511339080210954, 0.3152503517091980,
                    -0.2262646939654399, -0.1297668675672624, 0.0975016055873224, 0.0275228655303053,
                    -0.0315820393174862, 0.0005538422011614, 0.0047772575109455, -0.0010773010853085
            },
            6
    );

    /**
     * Daubechies 8 (DB8) coefficients.
     *
     * <p>Properties:</p>
     * <ul>
     *   <li>8 vanishing moments</li>
     *   <li>Filter length: 16</li>
     *   <li>Support width: 15</li>
     *   <li>Good balance between smoothness and computational efficiency</li>
     * </ul>
     *
     * <p>Source: Table 6.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     * <p>Verified against MATLAB Wavelet Toolbox and PyWavelets</p>
     */
    public static final Daubechies DB8 = new Daubechies(
            "db8",
            new double[]{
                    0.0544158422431049, 0.3128715909143031, 0.6756307362972904, 0.5853546836541907,
                    -0.0158291052563816, -0.2840155429615702, 0.0004724845739124, 0.1287474266204837,
                    -0.0173693010018083, -0.0440882539307952, 0.0139810279173995, 0.0087460940474061,
                    -0.0048703529934518, -0.0003917403733770, 0.0006754494064506, -0.0001174767841248
            },
            8
    );

    /**
     * Daubechies 10 (DB10) coefficients.
     *
     * <p>Properties:</p>
     * <ul>
     *   <li>10 vanishing moments</li>
     *   <li>Filter length: 20</li>
     *   <li>Support width: 19</li>
     *   <li>Very smooth, suitable for highly regular signals</li>
     *   <li>Higher computational cost due to longer filter</li>
     * </ul>
     *
     * <p>Source: Table 6.1 in "Ten Lectures on Wavelets" by I. Daubechies (1992)</p>
     * <p>Verified against MATLAB Wavelet Toolbox and PyWavelets</p>
     */
    public static final Daubechies DB10 = new Daubechies(
            "db10",
            new double[]{
                    0.0266700579005546, 0.1881768000776347, 0.5272011889317202, 0.6884590394536250,
                    0.2811723436605715, -0.2498464243271598, -0.1959462743772862, 0.1273693403357932,
                    0.0930573646035547, -0.0713941471663501, -0.0294575368218399, 0.0332126740593612,
                    0.0036065535669870, -0.0107331754833007, 0.0013953517470688, 0.0019924052951925,
                    -0.0006858566949564, -0.0001164668551285, 0.0000935886703202, -0.0000132642028945
            },
            10
    );

    // Note: Extended Daubechies wavelets (DB12-DB20) will be added in future versions
    // after proper coefficient verification against multiple reference sources.

    @Override
    public String description() {
        return "Daubechies wavelet of order " + order;
    }

    @Override
    public double[] lowPassDecomposition() {
        return this.lowPassCoeffs.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        int len = lowPassCoeffs.length;
        double[] highPass = new double[len];
        for (int i = 0; i < len; i++) {
            highPass[i] = (i % 2 == 0 ? 1 : -1) * lowPassCoeffs[len - 1 - i];
        }
        return highPass;
    }

    @Override
    public int vanishingMoments() {
        return order;
    }

    /**
     * Verifies that the Daubechies coefficients satisfy the orthogonality conditions.
     * This method validates the mathematical correctness of the coefficients.
     *
     * <p>Conditions checked:</p>
     * <ul>
     *   <li>Sum of coefficients equals √2 (within numerical tolerance)</li>
     *   <li>Sum of squared coefficients equals 1 (within numerical tolerance)</li>
     *   <li>Orthogonality condition for shifts by 2k</li>
     *   <li>Vanishing moments up to order-1</li>
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

        // Check vanishing moments (first N polynomial moments should be zero)
        // For the wavelet function, not the scaling function
        double[] g = highPassDecomposition();
        for (int p = 0; p < order; p++) {
            double moment = 0;
            for (int n = 0; n < g.length; n++) {
                moment += Math.pow(n, p) * g[n];
            }
            // Higher tolerance for higher moments due to numerical accumulation
            double momentTolerance = tolerance * Math.pow(10, p);
            if (Math.abs(moment) > momentTolerance) {
                return false;
            }
        }

        return true;
    }
}