package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.exception.InvalidSignalException;

/**
 * Demonstrates error handling in the VectorWave library using MODWT.
 * Shows how to handle various error conditions that may occur during wavelet transforms.
 * Note: MODWT works with arbitrary length signals, so power-of-2 restriction no longer applies.
 */
public class ErrorHandlingDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Error Handling Demo (MODWT)");
        System.out.println("================================");
        
        demonstrateEmptySignal();
        demonstrateNullSignal();
        demonstrateInvalidWaveletConfig();
        demonstrateRecoveryStrategies();
    }
    
    /**
     * Demonstrates handling of empty signals.
     * Note: MODWT works with any length > 0, so we test empty signal instead.
     */
    private static void demonstrateEmptySignal() {
        System.out.println("\n1. Empty Signal Demo:");
        
        // Empty signal
        double[] emptySignal = new double[0];
        
        try {
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            
            transform.forward(emptySignal);
            System.out.println("   ERROR: Should have thrown an exception!");
        } catch (InvalidSignalException e) {
            System.out.println("   ✓ Caught expected exception: " + e.getMessage());
            System.out.println("   ✓ Proper error handling for empty signal");
        } catch (Exception e) {
            System.out.println("   ! Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates handling of null signal input.
     */
    private static void demonstrateNullSignal() {
        System.out.println("\n2. Null Signal Demo:");
        
        try {
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            
            transform.forward(null);
            System.out.println("   ERROR: Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            System.out.println("   ✓ Caught expected exception: " + e.getMessage());
            System.out.println("   ✓ Proper null pointer protection");
        } catch (Exception e) {
            System.out.println("   ! Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates handling of invalid wavelet configurations.
     */
    private static void demonstrateInvalidWaveletConfig() {
        System.out.println("\n3. Invalid Wavelet Config Demo:");
        
        // Valid signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        try {
            // Attempt to create transform with null wavelet
            MODWTTransform transform = new MODWTTransform(null, BoundaryMode.PERIODIC);
            
            transform.forward(signal);
            System.out.println("   ERROR: Should have thrown an exception!");
        } catch (NullPointerException e) {
            System.out.println("   ✓ Caught expected exception: " + e.getMessage());
            System.out.println("   ✓ Proper validation of wavelet configuration");
        } catch (Exception e) {
            System.out.println("   ! Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates recovery strategies when errors occur.
     */
    private static void demonstrateRecoveryStrategies() {
        System.out.println("\n4. Recovery Strategies Demo:");
        
        double[] noisySignal = {1.0, 2.0, Double.NaN, 4.0, 5.0}; // Signal with NaN
        
        System.out.println("   Signal with invalid value (NaN)");
        
        try {
            // Strategy 1: Direct transform - should fail
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            
            transform.forward(noisySignal);
            System.out.println("   ERROR: Should have thrown an exception!");
            
        } catch (InvalidSignalException e) {
            System.out.println("   ✓ Caught exception as expected: " + e.getMessage());
            
            // Strategy 2: Clean the signal first
            double[] cleanSignal = cleanSignal(noisySignal);
            System.out.println("   ✓ Strategy - Cleaned signal (replaced NaN with interpolation)");
            
            try {
                MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
                MODWTResult result = transform.forward(cleanSignal);
                System.out.println("   ✓ Transform successful with cleaned signal");
                System.out.println("   ✓ Coefficients length: " + result.approximationCoeffs().length);
            } catch (Exception ex) {
                System.out.println("   ! Recovery failed: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Cleans signal by replacing NaN and Infinity values with interpolated values.
     */
    private static double[] cleanSignal(double[] signal) {
        double[] cleaned = signal.clone();
        
        for (int i = 0; i < cleaned.length; i++) {
            if (!Double.isFinite(cleaned[i])) {
                // Simple interpolation: use average of neighbors
                double left = (i > 0 && Double.isFinite(cleaned[i-1])) ? cleaned[i-1] : 0;
                double right = (i < cleaned.length-1 && Double.isFinite(cleaned[i+1])) ? cleaned[i+1] : 0;
                cleaned[i] = (left + right) / 2;
            }
        }
        
        return cleaned;
    }
}