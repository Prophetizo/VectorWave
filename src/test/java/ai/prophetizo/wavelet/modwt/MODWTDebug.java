package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;

/**
 * Simple debug test to understand MODWT implementation issues.
 */
public class MODWTDebug {
    public static void main(String[] args) {
        System.out.println("MODWT Debug Test");
        System.out.println("================");
        
        // Create MODWT transform
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Test with simple signal
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        System.out.println("Original signal: " + java.util.Arrays.toString(signal));
        
        // Get Haar filters
        Haar haar = new Haar();
        System.out.println("Haar low-pass decomposition: " + java.util.Arrays.toString(haar.lowPassDecomposition()));
        System.out.println("Haar high-pass decomposition: " + java.util.Arrays.toString(haar.highPassDecomposition()));
        System.out.println("Haar low-pass reconstruction: " + java.util.Arrays.toString(haar.lowPassReconstruction()));
        System.out.println("Haar high-pass reconstruction: " + java.util.Arrays.toString(haar.highPassReconstruction()));
        
        // Forward transform
        MODWTResult result = modwt.forward(signal);
        System.out.println("Approximation coeffs: " + java.util.Arrays.toString(result.approximationCoeffs()));
        System.out.println("Detail coeffs: " + java.util.Arrays.toString(result.detailCoeffs()));
        
        // Inverse transform
        double[] reconstructed = modwt.inverse(result);
        System.out.println("Reconstructed signal: " + java.util.Arrays.toString(reconstructed));
        
        // Check reconstruction error
        double maxError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            double error = Math.abs(signal[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
            System.out.println("Index " + i + ": " + signal[i] + " -> " + reconstructed[i] + " (error: " + error + ")");
        }
        System.out.println("Max reconstruction error: " + maxError);
    }
}