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
    // These are the standard Cohen-Daubechies-Feauveau (CDF) 1,3 coefficients
    public static final BiorthogonalSpline BIOR1_3 = new BiorthogonalSpline(
            "bior1.3", 1, 3,
            // Decomposition low-pass filter (analysis filter h0_tilde)
            new double[]{-1.0/8, 1.0/8, 1.0, 1.0, 1.0/8, -1.0/8},
            // Reconstruction low-pass filter (synthesis filter h0)
            new double[]{1.0, 1.0},
            true,
            0.5,  // Reconstruction scaling factor: For CDF 1,3 wavelets, the perfect
                  // reconstruction condition requires scaling by 1/2. This compensates
                  // for the energy distribution between the analysis and synthesis filters
                  // to ensure that ||reconstructed|| = ||original||.
            2     // Group delay: For CDF 1,3 wavelets, the combined analysis-synthesis
                  // filter bank introduces a delay of 2 samples. This is calculated as:
                  // delay = (length(h0_tilde) - 1)/2 + (length(h0) - 1)/2 - 1
                  //       = (6 - 1)/2 + (2 - 1)/2 - 1 = 2.5 + 0.5 - 1 = 2
                  // This delay must be compensated during reconstruction to achieve
                  // perfect alignment with the original signal.
    );
    private final String name;
    private final int reconstructionOrder;
    private final int decompositionOrder;
    private final double[] lowPassDecomp;
    private final double[] lowPassRecon;
    private final boolean symmetric;
    private final double reconstructionScale;
    private final int groupDelay;

    private BiorthogonalSpline(String name, int reconOrder, int decompOrder,
                               double[] lowPassDecomp, double[] lowPassRecon,
                               boolean symmetric, double reconstructionScale, int groupDelay) {
        this.name = name;
        this.reconstructionOrder = reconOrder;
        this.decompositionOrder = decompOrder;
        // For biorthogonal wavelets, do NOT normalize the filters
        // The filters must satisfy the biorthogonality conditions exactly
        this.lowPassDecomp = lowPassDecomp.clone();
        this.lowPassRecon = lowPassRecon.clone();
        this.symmetric = symmetric;
        this.reconstructionScale = reconstructionScale;
        this.groupDelay = groupDelay;
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
        // For biorthogonal wavelets, generate high-pass decomposition from low-pass reconstruction
        // Using the correct biorthogonal relationship: g[n] = (-1)^n * h[-n]
        double[] h = lowPassRecon;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            // Reverse the filter and apply alternating signs
            int sign = ((h.length - 1 - i) % 2 == 0) ? 1 : -1;
            g[i] = sign * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public double[] lowPassReconstruction() {
        return lowPassRecon.clone();
    }

    @Override
    public double[] highPassReconstruction() {
        // For biorthogonal wavelets, generate high-pass reconstruction from low-pass decomposition
        // Using the correct biorthogonal relationship
        double[] h = lowPassDecomp;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            // Reverse the filter and apply alternating signs
            int sign = ((h.length - 1 - i) % 2 == 0) ? 1 : -1;
            g[i] = sign * h[h.length - 1 - i];
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
    
    /**
     * Returns the reconstruction scaling factor.
     * For CDF wavelets, this is typically 0.5 to satisfy perfect reconstruction.
     *
     * @return the reconstruction scaling factor
     */
    public double getReconstructionScale() {
        return reconstructionScale;
    }
    
    /**
     * Returns the group delay (phase shift) of the wavelet.
     * This is the number of samples the reconstruction is shifted.
     *
     * @return the group delay in samples
     */
    public int getGroupDelay() {
        return groupDelay;
    }
}