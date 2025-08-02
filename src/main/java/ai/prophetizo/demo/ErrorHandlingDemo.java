package ai.prophetizo.demo;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.exception.InvalidSignalException;

/**
 * Demonstrates error handling in the VectorWave library.
 * Shows how to handle various error conditions that may occur during wavelet transforms.
 */
public class ErrorHandlingDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Error Handling Demo");
        System.out.println("================================");
        
        demonstrateInvalidSignalLength();
        demonstrateNullSignal();
        demonstrateInvalidWaveletConfig();
        demonstrateRecoveryStrategies();
    }
    
    /**
     * Demonstrates handling of invalid signal lengths (non-power-of-2).
     */
    private static void demonstrateInvalidSignalLength() {
        System.out.println("\n1. Invalid Signal Length Demo:");
        
        // Signal with length 7 (not a power of 2)
        double[] invalidSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        
        try {
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(new Haar());
            
            transform.forward(invalidSignal);
            System.out.println("   ERROR: Should have thrown an exception!");
        } catch (InvalidSignalException e) {
            System.out.println("   ✓ Caught expected exception: " + e.getMessage());
            System.out.println("   ✓ Proper error handling for invalid signal length");
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
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(new Haar());
            
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
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(null);
            
            transform.forward(signal);
            System.out.println("   ERROR: Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
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
        
        double[] problematicSignal = {1.0, 2.0, 3.0, 4.0, 5.0}; // Length 5, not power of 2
        
        System.out.println("   Original signal length: " + problematicSignal.length);
        
        try {
            // Strategy 1: Pad signal to next power of 2
            double[] paddedSignal = padToPowerOfTwo(problematicSignal);
            System.out.println("   ✓ Strategy 1 - Padded signal length: " + paddedSignal.length);
            
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.ZERO_PADDING)
                    .create(new Haar());
            
            var result = transform.forward(paddedSignal);
            System.out.println("   ✓ Transform successful with padded signal");
            System.out.println("   ✓ Approximation coefficients: " + result.approximationCoeffs().length);
            System.out.println("   ✓ Detail coefficients: " + result.detailCoeffs().length);
            
        } catch (Exception e) {
            System.out.println("   ! Recovery failed: " + e.getMessage());
        }
        
        try {
            // Strategy 2: Truncate signal to nearest smaller power of 2
            double[] truncatedSignal = truncateToPowerOfTwo(problematicSignal);
            System.out.println("   ✓ Strategy 2 - Truncated signal length: " + truncatedSignal.length);
            
            WaveletTransform transform = new WaveletTransformFactory()
                    .boundaryMode(BoundaryMode.PERIODIC)
                    .create(new Haar());
            
            var result = transform.forward(truncatedSignal);
            System.out.println("   ✓ Transform successful with truncated signal");
            
        } catch (Exception e) {
            System.out.println("   ! Recovery failed: " + e.getMessage());
        }
    }
    
    /**
     * Pads signal to the next power of 2 length using zero padding.
     */
    private static double[] padToPowerOfTwo(double[] signal) {
        int originalLength = signal.length;
        int paddedLength = nextPowerOfTwo(originalLength);
        
        double[] padded = new double[paddedLength];
        System.arraycopy(signal, 0, padded, 0, originalLength);
        // Rest of array is already zero-initialized
        
        return padded;
    }
    
    /**
     * Truncates signal to the nearest smaller power of 2 length.
     */
    private static double[] truncateToPowerOfTwo(double[] signal) {
        int originalLength = signal.length;
        int truncatedLength = previousPowerOfTwo(originalLength);
        
        double[] truncated = new double[truncatedLength];
        System.arraycopy(signal, 0, truncated, 0, truncatedLength);
        
        return truncated;
    }
    
    /**
     * Finds the next power of 2 greater than or equal to n.
     */
    private static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        return Integer.highestOneBit(n - 1) << 1;
    }
    
    /**
     * Finds the previous power of 2 less than or equal to n.
     */
    private static int previousPowerOfTwo(int n) {
        if (n <= 1) return 1;
        return Integer.highestOneBit(n);
    }
}