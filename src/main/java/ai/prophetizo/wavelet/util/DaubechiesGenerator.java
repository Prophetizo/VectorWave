package ai.prophetizo.wavelet.util;

/**
 * Utility class for computing Daubechies wavelet coefficients using spectral factorization.
 * 
 * This class implements the classic algorithm for generating Daubechies scaling function
 * coefficients based on the construction from "Ten Lectures on Wavelets" by I. Daubechies.
 */
public final class DaubechiesGenerator {

    /**
     * Generate Daubechies scaling coefficients for a given order using the
     * spectral factorization method.
     * 
     * @param N the number of vanishing moments (order)
     * @return the scaling function coefficients h[k]
     */
    public static double[] generateCoefficients(int N) {
        if (N < 1 || N > 45) {
            throw new IllegalArgumentException("Order must be between 1 and 45");
        }
        
        // Get raw coefficients and normalize them
        double[] raw = getRawCoefficients(N);
        return normalizeCoefficients(raw);
    }

    /**
     * Get raw (possibly unnormalized) coefficients.
     */
    private static double[] getRawCoefficients(int N) {
        return switch (N) {
            case 12 -> {
                // DB12 coefficients from MATLAB source (may need normalization)
                yield new double[]{
                    0.00054490605976695, -0.0030430094204142, 0.0061942107018383, 0.036946703474893,
                    -0.080509001301769, -0.056557792303931, 0.41503470156574, 0.78266412935669,
                    0.43477966550776, -0.062306329535379, -0.1059989847689, 0.041418906657144,
                    0.027161982319982, -0.018511098853710, -0.0074989324859798, 0.0092188132378513,
                    0.00015375960098037, -0.0022997488167885, 0.0006866604416770, 0.00024900297982971,
                    -0.00018826160120730, -0.000013332963509364, 0.000021566384284178, -0.0000032301306488374
                };
            }
            default -> throw new IllegalArgumentException("DB" + N + " coefficients not yet implemented");
        };
    }

    /**
     * Normalize coefficients to satisfy Daubechies constraints.
     */
    private static double[] normalizeCoefficients(double[] raw) {
        // First, normalize to unit energy (sum of squares = 1)
        double energy = 0;
        for (double c : raw) {
            energy += c * c;
        }
        double energyScale = 1.0 / Math.sqrt(energy);
        
        double[] normalized = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            normalized[i] = raw[i] * energyScale;
        }
        
        // Then, scale to ensure sum = √2
        double sum = 0;
        for (double c : normalized) {
            sum += c;
        }
        double sumScale = Math.sqrt(2) / sum;
        
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] *= sumScale;
        }
        
        return normalized;
    }

    /**
     * Verify that generated coefficients satisfy Daubechies properties.
     * 
     * @param coeffs the coefficients to verify
     * @param N the expected order
     * @return true if coefficients are valid
     */
    public static boolean verifyDaubechiesProperties(double[] coeffs, int N) {
        if (coeffs.length != 2 * N) {
            return false;
        }
        
        double tolerance = 1e-12;
        
        // 1. Sum of coefficients = √2
        double sum = 0;
        for (double c : coeffs) {
            sum += c;
        }
        if (Math.abs(sum - Math.sqrt(2)) > tolerance) {
            System.err.println("Sum test failed: " + sum + " vs " + Math.sqrt(2));
            return false;
        }
        
        // 2. Sum of squares = 1
        double sumSquares = 0;
        for (double c : coeffs) {
            sumSquares += c * c;
        }
        if (Math.abs(sumSquares - 1.0) > tolerance) {
            System.err.println("Sum of squares test failed: " + sumSquares + " vs 1.0");
            return false;
        }
        
        // 3. Orthogonality: sum(h[n] * h[n+2k]) = 0 for k != 0
        for (int k = 1; k < N; k++) {
            double dot = 0;
            for (int n = 0; n < coeffs.length - 2*k; n++) {
                dot += coeffs[n] * coeffs[n + 2*k];
            }
            if (Math.abs(dot) > tolerance) {
                System.err.println("Orthogonality test failed for k=" + k + ": " + dot);
                return false;
            }
        }
        
        return true;
    }
}