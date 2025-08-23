package ai.prophetizo.debug;

import ai.prophetizo.wavelet.api.Daubechies;

/**
 * Debug program to check existing coefficient properties.
 */
public class DebugExistingCoefficients {
    public static void main(String[] args) {
        System.out.println("Debugging Existing Daubechies Coefficients");
        System.out.println("==========================================");
        
        // Check existing ones
        Daubechies[] existing = {Daubechies.DB2, Daubechies.DB4, Daubechies.DB6, Daubechies.DB8, Daubechies.DB10};
        
        for (Daubechies db : existing) {
            System.out.println("\n" + db.name().toUpperCase() + ":");
            double[] coeffs = db.lowPassDecomposition();
            System.out.println("  Order: " + db.order());
            System.out.println("  Length: " + coeffs.length + " (expected: " + (2*db.order()) + ")");
            
            double sum = 0;
            for (double c : coeffs) sum += c;
            System.out.println("  Sum: " + sum + " (expected: " + Math.sqrt(2) + ")");
            
            double sumSquares = 0;
            for (double c : coeffs) sumSquares += c*c;
            System.out.println("  Sum of squares: " + sumSquares + " (expected: 1.0)");
            
            boolean verified = db.verifyCoefficients();
            System.out.println("  Verification: " + verified);
            
            // Print first few coefficients
            System.out.print("  First 4 coeffs: ");
            for (int i = 0; i < Math.min(4, coeffs.length); i++) {
                System.out.printf("%.6f ", coeffs[i]);
            }
            System.out.println();
        }
    }
}