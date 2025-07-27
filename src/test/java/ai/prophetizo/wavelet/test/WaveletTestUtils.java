package ai.prophetizo.wavelet.test;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.util.ToleranceConstants;

import java.util.Random;

/**
 * Utility class providing common test helpers for wavelet transform testing.
 * 
 * <p>This class provides methods for:</p>
 * <ul>
 *   <li>Generating test signals with known properties</li>
 *   <li>Verifying mathematical properties of wavelets</li>
 *   <li>Comparing transform results with tolerances</li>
 *   <li>Creating reference data for validation</li>
 * </ul>
 */
public final class WaveletTestUtils {
    
    /**
     * Default tolerance for floating-point comparisons.
     * @deprecated Use {@link ToleranceConstants#DEFAULT_TOLERANCE} instead.
     * This constant is maintained for backward compatibility but will be removed in future versions.
     */
    @Deprecated
    public static final double DEFAULT_TOLERANCE = ToleranceConstants.DEFAULT_TOLERANCE;
    
    /**
     * Tolerance for energy preservation checks.
     * @deprecated Use {@link ToleranceConstants#ENERGY_TOLERANCE} instead.
     * This constant is maintained for backward compatibility but will be removed in future versions.
     */
    @Deprecated
    public static final double ENERGY_TOLERANCE = ToleranceConstants.ENERGY_TOLERANCE;
    
    /**
     * Tolerance for orthogonality checks in wavelet filters.
     * @deprecated Use {@link ToleranceConstants#ORTHOGONALITY_TOLERANCE} instead.
     * This constant is maintained for backward compatibility but will be removed in future versions.
     */
    @Deprecated
    public static final double ORTHOGONALITY_TOLERANCE = ToleranceConstants.ORTHOGONALITY_TOLERANCE;
    
    private WaveletTestUtils() {
        // Utility class, prevent instantiation
    }
    
    // === Signal Generation Methods ===
    
    /**
     * Generates a simple sine wave signal.
     * 
     * @param length number of samples (must be power of 2)
     * @param frequency normalized frequency (0 to 0.5)
     * @param amplitude signal amplitude
     * @param phase phase offset in radians
     * @return sine wave signal
     */
    public static double[] generateSineWave(int length, double frequency, 
                                           double amplitude, double phase) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = amplitude * Math.sin(2 * Math.PI * frequency * i + phase);
        }
        return signal;
    }
    
    /**
     * Generates a composite signal with multiple sine components.
     * 
     * @param length number of samples (must be power of 2)
     * @param frequencies array of normalized frequencies
     * @param amplitudes array of amplitudes (must match frequencies length)
     * @return composite signal
     */
    public static double[] generateCompositeSignal(int length, 
                                                  double[] frequencies, 
                                                  double[] amplitudes) {
        if (frequencies.length != amplitudes.length) {
            throw new IllegalArgumentException(
                "Frequencies and amplitudes arrays must have same length");
        }
        
        double[] signal = new double[length];
        for (int j = 0; j < frequencies.length; j++) {
            for (int i = 0; i < length; i++) {
                signal[i] += amplitudes[j] * Math.sin(2 * Math.PI * frequencies[j] * i);
            }
        }
        return signal;
    }
    
    /**
     * Generates a step function signal.
     * 
     * @param length number of samples (must be power of 2)
     * @param stepPosition position of the step (0 to length-1)
     * @param lowValue value before step
     * @param highValue value after step
     * @return step function signal
     */
    public static double[] generateStepFunction(int length, int stepPosition,
                                               double lowValue, double highValue) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (i < stepPosition) ? lowValue : highValue;
        }
        return signal;
    }
    
    /**
     * Generates a random signal with specified seed for reproducibility.
     * 
     * @param length number of samples (must be power of 2)
     * @param seed random seed for reproducible test results. Using the same seed 
     *             will always generate the same signal sequence
     * @param minValue minimum signal value
     * @param maxValue maximum signal value
     * @return random signal with values uniformly distributed between minValue and maxValue
     */
    public static double[] generateRandomSignal(int length, long seed,
                                               double minValue, double maxValue) {
        Random random = new Random(seed);
        double[] signal = new double[length];
        double range = maxValue - minValue;
        
        for (int i = 0; i < length; i++) {
            signal[i] = minValue + range * random.nextDouble();
        }
        return signal;
    }
    
    // === Verification Methods ===
    
    /**
     * Verifies perfect reconstruction property of a wavelet transform.
     * 
     * @param transform the wavelet transform to test
     * @param signal the test signal
     * @param tolerance acceptable reconstruction error
     * @return true if reconstruction is within tolerance
     */
    public static boolean verifyPerfectReconstruction(WaveletTransform transform,
                                                     double[] signal,
                                                     double tolerance) {
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        return arraysEqualWithTolerance(signal, reconstructed, tolerance);
    }
    
    /**
     * Verifies energy preservation (Parseval's theorem).
     * 
     * @param signal original signal
     * @param result transform result
     * @param tolerance acceptable energy difference
     * @return true if energy is preserved within tolerance
     */
    public static boolean verifyEnergyPreservation(double[] signal,
                                                  TransformResult result,
                                                  double tolerance) {
        double signalEnergy = computeEnergy(signal);
        double coeffEnergy = computeEnergy(result.approximationCoeffs()) +
                            computeEnergy(result.detailCoeffs());
        
        return Math.abs(signalEnergy - coeffEnergy) < tolerance;
    }
    
    /**
     * Verifies orthogonality of wavelet filters.
     * 
     * @param lowPass low-pass filter coefficients
     * @param highPass high-pass filter coefficients
     * @param tolerance acceptable deviation from orthogonality
     * @return true if filters are orthogonal within tolerance
     */
    public static boolean verifyFilterOrthogonality(double[] lowPass,
                                                   double[] highPass,
                                                   double tolerance) {
        double dotProduct = 0.0;
        for (int i = 0; i < lowPass.length; i++) {
            dotProduct += lowPass[i] * highPass[i];
        }
        return Math.abs(dotProduct) < tolerance;
    }
    
    /**
     * Verifies that wavelet filters are normalized (sum of squares = 1).
     * 
     * @param filter the filter coefficients
     * @param tolerance acceptable deviation from unit norm
     * @return true if filter is normalized within tolerance
     */
    public static boolean verifyFilterNormalization(double[] filter, double tolerance) {
        double sumOfSquares = 0.0;
        for (double coeff : filter) {
            sumOfSquares += coeff * coeff;
        }
        return Math.abs(sumOfSquares - 1.0) < tolerance;
    }
    
    // === Comparison Methods ===
    
    /**
     * Compares two arrays element-wise with specified tolerance.
     * 
     * @param expected expected values
     * @param actual actual values
     * @param tolerance maximum allowed difference per element
     * @return true if arrays match within tolerance
     */
    public static boolean arraysEqualWithTolerance(double[] expected,
                                                  double[] actual,
                                                  double tolerance) {
        if (expected.length != actual.length) {
            return false;
        }
        
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > tolerance) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Computes the maximum absolute difference between two arrays.
     * 
     * @param expected expected values
     * @param actual actual values
     * @return maximum absolute difference
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public static double maxAbsoluteDifference(double[] expected, double[] actual) {
        if (expected.length != actual.length) {
            throw new IllegalArgumentException("Arrays must have same length.");
        }
        
        double maxDiff = 0.0;
        for (int i = 0; i < expected.length; i++) {
            double diff = Math.abs(expected[i] - actual[i]);
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }
        return maxDiff;
    }
    
    /**
     * Computes the root mean square error between two arrays.
     * 
     * @param expected expected values
     * @param actual actual values
     * @return RMSE value
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public static double computeRMSE(double[] expected, double[] actual) {
        if (expected.length != actual.length) {
            throw new IllegalArgumentException("Arrays must have same length.");
        }
        
        double sumSquaredDiff = 0.0;
        for (int i = 0; i < expected.length; i++) {
            double diff = expected[i] - actual[i];
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / expected.length);
    }
    
    // === Utility Methods ===
    
    /**
     * Computes the energy (sum of squares) of a signal.
     * 
     * @param signal the signal
     * @return signal energy
     */
    public static double computeEnergy(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
    
    /**
     * Creates a simple test signal of specified length.
     * Default pattern: [1, 2, 3, 4, 5, 4, 3, 2, ...] cycling
     * 
     * @param length desired signal length (must be power of 2)
     * @return test signal
     */
    public static double[] createSimpleTestSignal(int length) {
        double[] signal = new double[length];
        int[] pattern = {1, 2, 3, 4, 5, 4, 3, 2};
        
        for (int i = 0; i < length; i++) {
            signal[i] = pattern[i % pattern.length];
        }
        return signal;
    }
    
    /**
     * Formats a signal as a string for debugging.
     * 
     * @param label label for the signal
     * @param signal the signal to format
     * @param maxElements maximum elements to include (0 for all)
     * @return formatted string representation of the signal
     */
    public static String formatSignal(String label, double[] signal, int maxElements) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s [length=%d]: ", label, signal.length));
        
        int elementsToShow = (maxElements <= 0 || maxElements > signal.length) 
                           ? signal.length : maxElements;
        
        sb.append("[");
        for (int i = 0; i < elementsToShow; i++) {
            sb.append(String.format("%.6f", signal[i]));
            if (i < elementsToShow - 1) {
                sb.append(", ");
            }
        }
        
        if (elementsToShow < signal.length) {
            sb.append(String.format(", ... (%d more elements)", signal.length - elementsToShow));
        }
        sb.append("]");
        
        return sb.toString();
    }
}