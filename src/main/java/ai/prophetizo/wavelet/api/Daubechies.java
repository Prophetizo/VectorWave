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
 */
public record Daubechies(String name, double[] lowPassCoeffs, int order) implements OrthogonalWavelet {

    public static final Daubechies DB2 = new Daubechies(
            "db2",
            new double[]{0.4829629131445341, 0.8365163037378079, 0.2241438680420134, -0.1294095225512603},
            2
    );

    public static final Daubechies DB4 = new Daubechies(
            "db4",
            new double[]{
                    0.2303778133088964, 0.7148465705529154, 0.6308807679298587, -0.0279837693982488,
                    -0.1870348117190931, 0.0308413818355607, 0.0328830116668852, -0.0105974017850690
            },
            4
    );
    
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
}