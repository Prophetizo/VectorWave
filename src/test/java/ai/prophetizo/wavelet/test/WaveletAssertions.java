package ai.prophetizo.wavelet.test;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Custom assertions for wavelet transform testing.
 * 
 * <p>This class provides specialized assertion methods that understand
 * the mathematical properties and constraints of wavelet transforms.</p>
 */
public final class WaveletAssertions {
    
    private WaveletAssertions() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Asserts that two arrays are equal within the specified tolerance.
     * 
     * @param expected expected array
     * @param actual actual array
     * @param tolerance maximum allowed difference per element
     * @param message assertion message
     */
    public static void assertArraysEqualWithTolerance(double[] expected, 
                                                     double[] actual, 
                                                     double tolerance,
                                                     String message) {
        assertEquals(expected.length, actual.length, 
            message + " - Arrays have different lengths");
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], tolerance,
                String.format("%s - Element at index %d differs: expected=%.12f, actual=%.12f",
                    message, i, expected[i], actual[i]));
        }
    }
    
    /**
     * Asserts that two arrays are equal within the default tolerance.
     * 
     * @param expected expected array
     * @param actual actual array
     * @param message assertion message
     */
    public static void assertArraysEqualWithTolerance(double[] expected,
                                                     double[] actual,
                                                     String message) {
        assertArraysEqualWithTolerance(expected, actual, 
            ToleranceConstants.DEFAULT_TOLERANCE, message);
    }
    
    /**
     * Asserts that a transform preserves signal energy (Parseval's theorem).
     * 
     * @param originalSignal the input signal
     * @param transformResult the transform result
     * @param tolerance acceptable energy difference
     */
    public static void assertEnergyPreserved(double[] originalSignal,
                                           TransformResult transformResult,
                                           double tolerance) {
        double signalEnergy = WaveletTestUtils.computeEnergy(originalSignal);
        double transformEnergy = 
            WaveletTestUtils.computeEnergy(transformResult.approximationCoeffs()) +
            WaveletTestUtils.computeEnergy(transformResult.detailCoeffs());
        
        assertEquals(signalEnergy, transformEnergy, tolerance,
            String.format("Energy not preserved: signal=%.12f, transform=%.12f",
                signalEnergy, transformEnergy));
    }
    
    /**
     * Asserts that a wavelet filter is properly normalized.
     * 
     * @param filter the filter coefficients
     * @param tolerance acceptable deviation from unit norm
     * @param filterName name of the filter for error messages
     */
    public static void assertFilterNormalized(double[] filter, 
                                            double tolerance,
                                            String filterName) {
        double sumOfSquares = 0.0;
        for (double coeff : filter) {
            sumOfSquares += coeff * coeff;
        }
        
        assertEquals(1.0, sumOfSquares, tolerance,
            String.format("%s filter not normalized: sum of squares = %.12f",
                filterName, sumOfSquares));
    }
    
    /**
     * Asserts that two filters are orthogonal.
     * 
     * @param filter1 first filter
     * @param filter2 second filter
     * @param tolerance acceptable deviation from zero dot product
     */
    public static void assertFiltersOrthogonal(double[] filter1,
                                             double[] filter2,
                                             double tolerance) {
        assertEquals(filter1.length, filter2.length,
            "Filters must have same length for orthogonality check");
        
        double dotProduct = 0.0;
        for (int i = 0; i < filter1.length; i++) {
            dotProduct += filter1[i] * filter2[i];
        }
        
        assertEquals(0.0, dotProduct, tolerance,
            String.format("Filters not orthogonal: dot product = %.12f", dotProduct));
    }
    
    /**
     * Asserts that a signal has the expected length (power of 2).
     * 
     * @param signal the signal to check
     * @param expectedLength expected length
     */
    public static void assertSignalLength(double[] signal, int expectedLength) {
        assertNotNull(signal, "Signal is null");
        assertEquals(expectedLength, signal.length,
            String.format("Signal has incorrect length: expected=%d, actual=%d",
                expectedLength, signal.length));
        assertTrue(isPowerOfTwo(expectedLength),
            "Expected length must be a power of 2");
    }
    
    /**
     * Asserts that transform coefficients have the correct lengths.
     * 
     * @param result transform result
     * @param expectedLength expected length of each coefficient array
     */
    public static void assertCoefficientsLength(TransformResult result,
                                              int expectedLength) {
        assertNotNull(result, "TransformResult is null");
        
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        assertNotNull(approx, "Approximation coefficients are null");
        assertNotNull(detail, "Detail coefficients are null");
        
        assertEquals(expectedLength, approx.length,
            "Approximation coefficients have incorrect length");
        assertEquals(expectedLength, detail.length,
            "Detail coefficients have incorrect length");
    }
    
    /**
     * Asserts that all values in an array are finite (not NaN or Infinity).
     * 
     * @param array the array to check
     * @param arrayName name for error messages
     */
    public static void assertAllFinite(double[] array, String arrayName) {
        assertNotNull(array, arrayName + " is null");
        
        for (int i = 0; i < array.length; i++) {
            assertTrue(Double.isFinite(array[i]),
                String.format("%s contains non-finite value at index %d: %f",
                    arrayName, i, array[i]));
        }
    }
    
    /**
     * Asserts that a wavelet satisfies the vanishing moments property.
     * This checks that the wavelet can represent polynomials up to degree p-1.
     * 
     * @param wavelet the wavelet to test
     * @param expectedMoments expected number of vanishing moments
     * @param tolerance numerical tolerance
     */
    public static void assertVanishingMoments(Wavelet wavelet,
                                            int expectedMoments,
                                            double tolerance) {
        double[] highPass = wavelet.highPassDecomposition();
        
        // Check first p moments
        for (int p = 0; p < expectedMoments; p++) {
            double moment = 0.0;
            for (int k = 0; k < highPass.length; k++) {
                moment += highPass[k] * Math.pow(k, p);
            }
            
            assertEquals(0.0, moment, tolerance,
                String.format("Vanishing moment %d not satisfied for %s: moment=%.12f",
                    p, wavelet.name(), moment));
        }
    }
    
    /**
     * Asserts that reconstruction is perfect within tolerance.
     * 
     * @param original original signal
     * @param reconstructed reconstructed signal
     * @param tolerance acceptable reconstruction error
     */
    public static void assertPerfectReconstruction(double[] original,
                                                  double[] reconstructed,
                                                  double tolerance) {
        assertArraysEqualWithTolerance(original, reconstructed, tolerance,
            "Perfect reconstruction not achieved");
        
        // Also check RMSE for additional insight
        double rmse = WaveletTestUtils.computeRMSE(original, reconstructed);
        assertTrue(rmse < tolerance,
            String.format("RMSE too high for perfect reconstruction: %.12e", rmse));
    }
    
    /**
     * Asserts that a transform result contains valid data.
     * 
     * @param result the transform result to validate
     */
    public static void assertValidTransformResult(TransformResult result) {
        assertNotNull(result, "TransformResult is null");
        
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        assertNotNull(approx, "Approximation coefficients are null");
        assertNotNull(detail, "Detail coefficients are null");
        assertTrue(approx.length > 0, "Approximation coefficients are empty");
        assertTrue(detail.length > 0, "Detail coefficients are empty");
        assertEquals(approx.length, detail.length,
            "Coefficient arrays have different lengths");
        
        assertAllFinite(approx, "Approximation coefficients");
        assertAllFinite(detail, "Detail coefficients");
    }
    
    // === Helper Methods ===
    
    /**
     * Checks if a number is a power of two.
     * 
     * @param n the number to check
     * @return true if n is a power of two
     */
    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}