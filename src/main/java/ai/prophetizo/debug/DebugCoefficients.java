package ai.prophetizo.debug;

import ai.prophetizo.wavelet.util.DaubechiesCoefficients;

/**
 * Debug program to check coefficient properties.
 */
public class DebugCoefficients {
    public static void main(String[] args) {
        System.out.println("Debugging Daubechies Coefficients");
        System.out.println("==================================");
        
        for (int order : new int[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20}) {
            System.out.println("\nDB" + order + ":");
            try {
                double[] coeffs = DaubechiesCoefficients.getCoefficients(order);
                System.out.println("  Length: " + coeffs.length + " (expected: " + (2*order) + ")");
                
                double sum = 0;
                for (double c : coeffs) sum += c;
                System.out.println("  Sum: " + sum + " (expected: " + Math.sqrt(2) + ")");
                
                double sumSquares = 0;
                for (double c : coeffs) sumSquares += c*c;
                System.out.println("  Sum of squares: " + sumSquares + " (expected: 1.0)");
                
                boolean verified = DaubechiesCoefficients.verifyCoefficients(coeffs, order);
                System.out.println("  Verification: " + verified);
                
                // Print first few coefficients
                System.out.print("  First 4 coeffs: ");
                for (int i = 0; i < Math.min(4, coeffs.length); i++) {
                    System.out.printf("%.6f ", coeffs[i]);
                }
                System.out.println();
                
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
    }
}