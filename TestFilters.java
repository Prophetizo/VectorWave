import ai.prophetizo.wavelet.api.Haar;

public class TestFilters {
    public static void main(String[] args) {
        Haar haar = new Haar();
        
        double[] h = haar.lowPassDecomposition();
        double[] g = haar.highPassDecomposition();
        double[] h_tilde = haar.lowPassReconstruction();
        double[] g_tilde = haar.highPassReconstruction();
        
        System.out.println("Haar filter coefficients:");
        System.out.print("h (low-pass decomp):  [");
        for (double v : h) System.out.printf("%.15f ", v);
        System.out.println("]");
        
        System.out.print("g (high-pass decomp): [");
        for (double v : g) System.out.printf("%.15f ", v);
        System.out.println("]");
        
        System.out.print("h_tilde (low-pass recon):  [");
        for (double v : h_tilde) System.out.printf("%.15f ", v);
        System.out.println("]");
        
        System.out.print("g_tilde (high-pass recon): [");
        for (double v : g_tilde) System.out.printf("%.15f ", v);
        System.out.println("]");
        
        // MODWT scaling
        double scale = 1.0 / Math.sqrt(2.0);
        System.out.println("\nMODWT scaled filters (1/sqrt(2) scaling):");
        System.out.print("scaled h: [");
        for (double v : h) System.out.printf("%.15f ", v * scale);
        System.out.println("]");
        
        System.out.print("scaled g: [");
        for (double v : g) System.out.printf("%.15f ", v * scale);
        System.out.println("]");
    }
}