package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;

import java.util.Arrays;

/**
 * Demonstrates the IFFT method with parameter validation.
 */
public class IFFTDemo {
    
    public static void main(String[] args) {
        System.out.println("IFFT Parameter Validation Demo");
        System.out.println("==============================\n");
        
        FFTAcceleratedCWT fft = new FFTAcceleratedCWT();
        
        // Valid usage examples
        demonstrateValidUsage(fft);
        
        // Validation examples
        demonstrateParameterValidation(fft);
    }
    
    private static void demonstrateValidUsage(FFTAcceleratedCWT fft) {
        System.out.println("1. Valid IFFT usage:");
        
        // Test with simple impulse signal
        double[] originalSignal = {1.0, 0.0, 0.0, 0.0};
        System.out.println("   Original signal: " + Arrays.toString(originalSignal));
        
        // Forward FFT
        Complex[] spectrum = fft.fft(originalSignal);
        System.out.println("   FFT spectrum length: " + spectrum.length);
        
        // Inverse FFT (this is where the validation happens)
        double[] reconstructed = fft.ifft(spectrum);
        System.out.println("   Reconstructed: " + Arrays.toString(reconstructed));
        
        // Check accuracy
        double maxError = 0.0;
        for (int i = 0; i < originalSignal.length; i++) {
            maxError = Math.max(maxError, Math.abs(originalSignal[i] - reconstructed[i]));
        }
        System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
        System.out.println();
    }
    
    private static void demonstrateParameterValidation(FFTAcceleratedCWT fft) {
        System.out.println("2. Parameter validation demonstrations:");
        
        // Test null input
        try {
            fft.ifft(null);
        } catch (NullPointerException e) {
            System.out.println("   ✓ Null input validation: " + e.getMessage());
        }
        
        // Test non-power-of-2 length
        try {
            Complex[] invalidLength = new Complex[3]; // Not power of 2
            for (int i = 0; i < 3; i++) {
                invalidLength[i] = new Complex(1.0, 0.0);
            }
            fft.ifft(invalidLength);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Non-power-of-2 validation: " + e.getMessage());
        }
        
        // Test null complex coefficient
        try {
            Complex[] withNull = new Complex[4];
            withNull[0] = new Complex(1.0, 0.0);
            withNull[1] = null; // Null coefficient
            withNull[2] = new Complex(2.0, 0.0);
            withNull[3] = new Complex(3.0, 0.0);
            fft.ifft(withNull);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Null coefficient validation: " + e.getMessage());
        }
        
        // Test NaN values
        try {
            Complex[] withNaN = {
                new Complex(1.0, 0.0),
                new Complex(Double.NaN, 1.0), // NaN value
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            fft.ifft(withNaN);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ NaN value validation: " + e.getMessage());
        }
        
        // Test infinity values
        try {
            Complex[] withInf = {
                new Complex(1.0, 0.0),
                new Complex(2.0, Double.POSITIVE_INFINITY), // Infinity value
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            fft.ifft(withInf);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Infinity value validation: " + e.getMessage());
        }
        
        System.out.println("\n3. Validation follows VectorWave patterns:");
        System.out.println("   - Consistent with ValidationUtils.java power-of-2 checking");
        System.out.println("   - Similar error messages to other validation methods");
        System.out.println("   - Comprehensive finite value checking");
        System.out.println("   - Null safety for all parameters");
    }
}