package ai.prophetizo.demo;

import ai.prophetizo.wavelet.util.SignalProcessor;
import ai.prophetizo.wavelet.cwt.ComplexNumber;

import java.util.Arrays;

/**
 * Demonstrates FFT and IFFT operations using the consolidated FFT utilities.
 */
public class IFFTDemo {
    
    public static void main(String[] args) {
        System.out.println("FFT/IFFT Demo using Consolidated Implementation");
        System.out.println("==============================================\n");
        
        // Valid usage examples
        demonstrateValidUsage();
        
        // Validation examples
        demonstrateParameterValidation();
    }
    
    private static void demonstrateValidUsage() {
        System.out.println("1. Valid FFT/IFFT usage:");
        
        // Test with simple impulse signal
        double[] originalSignal = {1.0, 0.0, 0.0, 0.0};
        System.out.println("   Original signal: " + Arrays.toString(originalSignal));
        
        // Forward FFT
        ComplexNumber[] spectrum = SignalProcessor.fftReal(originalSignal);
        System.out.println("   FFT spectrum length: " + spectrum.length);
        
        // Inverse FFT
        SignalProcessor.ifft(spectrum);
        
        // Extract real part
        double[] reconstructed = new double[originalSignal.length];
        for (int i = 0; i < originalSignal.length; i++) {
            reconstructed[i] = spectrum[i].real();
        }
        System.out.println("   Reconstructed: " + Arrays.toString(reconstructed));
        
        // Check accuracy
        double maxError = 0.0;
        for (int i = 0; i < originalSignal.length; i++) {
            maxError = Math.max(maxError, Math.abs(originalSignal[i] - reconstructed[i]));
        }
        System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
        System.out.println();
    }
    
    private static void demonstrateParameterValidation() {
        System.out.println("2. Parameter validation demonstrations:");
        
        // Test null input
        try {
            SignalProcessor.ifft(null);
        } catch (NullPointerException e) {
            System.out.println("   ✓ Null input validation: " + e.getMessage());
        }
        
        // Test non-power-of-2 length
        try {
            ComplexNumber[] invalidLength = new ComplexNumber[3]; // Not power of 2
            for (int i = 0; i < 3; i++) {
                invalidLength[i] = new ComplexNumber(1.0, 0.0);
            }
            SignalProcessor.ifft(invalidLength);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Non-power-of-2 validation: " + e.getMessage());
        }
        
        // Test null complex coefficient
        try {
            ComplexNumber[] withNull = new ComplexNumber[4];
            withNull[0] = new ComplexNumber(1.0, 0.0);
            withNull[1] = null; // Null coefficient
            withNull[2] = new ComplexNumber(2.0, 0.0);
            withNull[3] = new ComplexNumber(3.0, 0.0);
            SignalProcessor.ifft(withNull);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Null coefficient validation: " + e.getMessage());
        }
        
        // Test NaN values
        try {
            ComplexNumber[] withNaN = {
                new ComplexNumber(1.0, 0.0),
                new ComplexNumber(Double.NaN, 1.0), // NaN value
                new ComplexNumber(2.0, 0.0),
                new ComplexNumber(3.0, 0.0)
            };
            SignalProcessor.ifft(withNaN);
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ NaN value validation: " + e.getMessage());
        }
        
        // Test infinity values
        try {
            ComplexNumber[] withInf = {
                new ComplexNumber(1.0, 0.0),
                new ComplexNumber(2.0, Double.POSITIVE_INFINITY), // Infinity value
                new ComplexNumber(2.0, 0.0),
                new ComplexNumber(3.0, 0.0)
            };
            SignalProcessor.ifft(withInf);
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