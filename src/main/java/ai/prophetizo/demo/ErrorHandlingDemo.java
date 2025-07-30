package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;

/**
 * Demonstrates error handling scenarios in the VectorWave library.
 * Shows how the library handles various invalid inputs and edge cases.
 */
public class ErrorHandlingDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Error Handling Demo");
        System.out.println("=================================");
        
        demonstrateNullInputHandling();
        demonstrateInvalidSignalSizes();
        demonstrateInvalidArguments();
    }
    
    private static void demonstrateNullInputHandling() {
        System.out.println("\n1. Null Input Handling:");
        System.out.println("-----------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(new Haar());
        
        // Test null signal
        try {
            transform.forward(null);
            System.out.println("ERROR: Should have thrown exception for null signal");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected null signal: " + e.getMessage());
        }
        
        // Test null transform result
        try {
            transform.inverse(null);
            System.out.println("ERROR: Should have thrown exception for null result");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected null transform result: " + e.getMessage());
        }
    }
    
    private static void demonstrateInvalidSignalSizes() {
        System.out.println("\n2. Invalid Signal Sizes:");
        System.out.println("------------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(new Haar());
        
        // Test empty signal
        try {
            transform.forward(new double[0]);
            System.out.println("ERROR: Should have thrown exception for empty signal");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected empty signal: " + e.getMessage());
        }
        
        // Test single element signal
        try {
            transform.forward(new double[]{1.0});
            System.out.println("ERROR: Should have thrown exception for single element");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected single element signal: " + e.getMessage());
        }
        
        // Test non-power-of-2 signal (if required)
        try {
            double[] signal = {1.0, 2.0, 3.0}; // 3 elements, not power of 2
            TransformResult result = transform.forward(signal);
            System.out.println("✓ Handled non-power-of-2 signal successfully");
        } catch (Exception e) {
            System.out.println("✓ Correctly handled non-power-of-2 signal: " + e.getMessage());
        }
    }
    
    private static void demonstrateInvalidArguments() {
        System.out.println("\n3. Invalid Constructor Arguments:");
        System.out.println("--------------------------------");
        
        // Test null wavelet
        try {
            new WaveletTransformFactory().create(null);
            System.out.println("ERROR: Should have thrown exception for null wavelet");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected null wavelet: " + e.getMessage());
        }
        
        // Test null boundary mode
        try {
            new WaveletTransformFactory().withBoundaryMode(null);
            System.out.println("ERROR: Should have thrown exception for null boundary mode");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected null boundary mode: " + e.getMessage());
        }
    }
}