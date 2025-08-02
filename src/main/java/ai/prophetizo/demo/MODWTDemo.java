package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;

import java.util.Arrays;

/**
 * Demonstration of MODWT (Maximal Overlap Discrete Wavelet Transform) functionality.
 * 
 * <p>This demo shows the key differences between standard DWT and MODWT:</p>
 * <ul>
 *   <li>MODWT can handle arbitrary length signals (not just power-of-2)</li>
 *   <li>MODWT produces same-length output as input (non-decimated)</li>
 *   <li>MODWT is shift-invariant</li>
 * </ul>
 */
public class MODWTDemo {
    
    public static void main(String[] args) {
        System.out.println("MODWT (Maximal Overlap Discrete Wavelet Transform) Demo");
        System.out.println("======================================================");
        System.out.println();
        
        demonstrateArbitraryLength();
        System.out.println();
        
        demonstrateNonDecimated();
        System.out.println();
        
        demonstrateShiftInvariance();
        System.out.println();
        
        demonstrateMultipleWavelets();
    }
    
    private static void demonstrateArbitraryLength() {
        System.out.println("1. Arbitrary Length Signals");
        System.out.println("==========================");
        
        // MODWT can handle signals that are not power-of-2 length
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // length 7
        
        System.out.println("Original signal (length " + signal.length + "): " + Arrays.toString(signal));
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(signal);
        double[] reconstructed = modwt.inverse(result);
        
        System.out.println("Approximation coeffs (length " + result.approximationCoeffs().length + "): " + 
                         formatArray(result.approximationCoeffs()));
        System.out.println("Detail coeffs (length " + result.detailCoeffs().length + "): " + 
                         formatArray(result.detailCoeffs()));
        System.out.println("Reconstructed signal: " + formatArray(reconstructed));
        
        double maxError = calculateMaxError(signal, reconstructed);
        System.out.println("Max reconstruction error: " + String.format("%.2e", maxError));
    }
    
    private static void demonstrateNonDecimated() {
        System.out.println("2. Non-decimated Transform");
        System.out.println("=========================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}; // length 8
        
        System.out.println("Original signal: " + Arrays.toString(signal));
        System.out.println("Signal length: " + signal.length);
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(signal);
        
        System.out.println("MODWT approximation length: " + result.approximationCoeffs().length + 
                         " (same as input)");
        System.out.println("MODWT detail length: " + result.detailCoeffs().length + 
                         " (same as input)");
        
        // Compare with what DWT would produce
        System.out.println("Standard DWT would produce: " + (signal.length / 2) + 
                         " approximation + " + (signal.length / 2) + " detail coefficients");
    }
    
    private static void demonstrateShiftInvariance() {
        System.out.println("3. Shift Invariance");
        System.out.println("==================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] shiftedSignal = {8.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // circular shift left by 1
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        MODWTResult result1 = modwt.forward(signal);
        MODWTResult result2 = modwt.forward(shiftedSignal);
        
        System.out.println("Original signal:     " + formatArray(signal));
        System.out.println("Shifted signal:      " + formatArray(shiftedSignal));
        System.out.println();
        System.out.println("Original approx:     " + formatArray(result1.approximationCoeffs()));
        System.out.println("Shifted approx:      " + formatArray(result2.approximationCoeffs()));
        System.out.println();
        System.out.println("Original detail:     " + formatArray(result1.detailCoeffs()));
        System.out.println("Shifted detail:      " + formatArray(result2.detailCoeffs()));
        
        System.out.println("\nNote: MODWT coefficients shift with the input signal, preserving temporal relationships.");
    }
    
    private static void demonstrateMultipleWavelets() {
        System.out.println("4. Different Wavelets");
        System.out.println("====================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        System.out.println("Original signal: " + Arrays.toString(signal));
        System.out.println();
        
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4
        };
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform modwt = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = modwt.forward(signal);
            double[] reconstructed = modwt.inverse(result);
            
            double maxError = calculateMaxError(signal, reconstructed);
            
            System.out.println(wavelet.name() + " wavelet:");
            System.out.println("  Approximation: " + formatArray(result.approximationCoeffs()));
            System.out.println("  Detail:        " + formatArray(result.detailCoeffs()));
            System.out.println("  Max error:     " + String.format("%.2e", maxError));
            System.out.println();
        }
    }
    
    private static String formatArray(double[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < Math.min(array.length, 8); i++) { // Show first 8 elements max
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.3f", array[i]));
        }
        if (array.length > 8) {
            sb.append(", ...");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0.0;
        for (int i = 0; i < original.length; i++) {
            double error = Math.abs(original[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        return maxError;
    }
}