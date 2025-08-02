package ai.prophetizo.wavelet.test;

import java.util.Random;

/**
 * Utility class for creating complex arrays in tests.
 * Provides common patterns for initializing complex data in interleaved format.
 */
public final class ComplexArrayTestUtils {
    
    private ComplexArrayTestUtils() {
        // Prevent instantiation
    }
    
    /**
     * Creates a complex array from real values with zero imaginary parts.
     * 
     * @param realValues the real values
     * @return interleaved complex array with real parts from input and zero imaginary parts
     */
    public static double[] createComplexFromReal(double[] realValues) {
        double[] complex = new double[2 * realValues.length];
        for (int i = 0; i < realValues.length; i++) {
            complex[2 * i] = realValues[i];      // Real part
            complex[2 * i + 1] = 0;              // Imaginary part: zero
        }
        return complex;
    }
    
    /**
     * Creates a complex array with random real values and zero imaginary parts.
     * 
     * @param size the number of complex values
     * @param random the random number generator
     * @return interleaved complex array with random real parts and zero imaginary parts
     */
    public static double[] createRandomRealComplex(int size, Random random) {
        double[] complex = new double[2 * size];
        for (int i = 0; i < size; i++) {
            complex[2 * i] = random.nextGaussian();  // Real part: random
            complex[2 * i + 1] = 0;                  // Imaginary part: zero
        }
        return complex;
    }
    
    /**
     * Creates a complex array with random real and imaginary values.
     * 
     * @param size the number of complex values
     * @param random the random number generator
     * @return interleaved complex array with random real and imaginary parts
     */
    public static double[] createRandomComplex(int size, Random random) {
        double[] complex = new double[2 * size];
        for (int i = 0; i < size; i++) {
            complex[2 * i] = random.nextGaussian();      // Real part: random
            complex[2 * i + 1] = random.nextGaussian();  // Imaginary part: random
        }
        return complex;
    }
    
    /**
     * Creates a complex array representing a sinusoidal signal.
     * 
     * @param size the number of complex values
     * @param frequency the frequency of the sinusoid (cycles per size)
     * @param phase the phase offset in radians
     * @return interleaved complex array with sinusoidal real parts and zero imaginary parts
     */
    public static double[] createSinusoidalComplex(int size, double frequency, double phase) {
        double[] complex = new double[2 * size];
        for (int i = 0; i < size; i++) {
            complex[2 * i] = Math.sin(2 * Math.PI * frequency * i / size + phase);  // Real part
            complex[2 * i + 1] = 0;                                                  // Imaginary part: zero
        }
        return complex;
    }
    
    /**
     * Creates a complex array with specific real and imaginary values.
     * 
     * @param size the number of complex values
     * @param realValue the value for all real parts
     * @param imagValue the value for all imaginary parts
     * @return interleaved complex array with constant values
     */
    public static double[] createConstantComplex(int size, double realValue, double imagValue) {
        double[] complex = new double[2 * size];
        for (int i = 0; i < size; i++) {
            complex[2 * i] = realValue;      // Real part
            complex[2 * i + 1] = imagValue;  // Imaginary part
        }
        return complex;
    }
    
    /**
     * Fills an existing complex array with random real values and zero imaginary parts.
     * 
     * @param data the complex array to fill (must have even length)
     * @param random the random number generator
     */
    public static void fillRandomReal(double[] data, Random random) {
        int size = data.length / 2;
        for (int i = 0; i < size; i++) {
            data[2 * i] = random.nextGaussian();  // Real part: random
            data[2 * i + 1] = 0;                  // Imaginary part: zero
        }
    }
    
    /**
     * Checks if all values in the array are finite (not NaN or Infinite).
     * 
     * @param data the array to check
     * @return true if all values are finite
     */
    public static boolean allFinite(double[] data) {
        for (double v : data) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the array contains at least one non-zero value.
     * 
     * @param data the array to check
     * @param epsilon the tolerance for zero comparison
     * @return true if at least one value has absolute value greater than epsilon
     */
    public static boolean hasNonZero(double[] data, double epsilon) {
        for (double v : data) {
            if (Math.abs(v) > epsilon) {
                return true; // Return early if a non-zero value is found
            }
        }
        return false; // Return false if no non-zero value is found
    }
}