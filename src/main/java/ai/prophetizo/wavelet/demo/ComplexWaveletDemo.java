package ai.prophetizo.wavelet.demo;

import ai.prophetizo.wavelet.Complex;
import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.util.WaveletEvaluationUtils;

/**
 * Demo class to showcase the evaluateAsComplex functionality.
 */
public class ComplexWaveletDemo {
    public static void main(String[] args) {
        System.out.println("=== Complex Wavelet Evaluation Demo ===\n");
        
        // Create a Morlet wavelet
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        
        System.out.println("Morlet Wavelet: " + morlet.description());
        System.out.println("Is Complex: " + morlet.isComplex());
        System.out.println();
        
        // Test evaluateAsComplex at different points
        double[] testPoints = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        System.out.println("Wavelet evaluation at different points:");
        System.out.println("t\t\tReal Part\tImaginary Part\tMagnitude\tPhase");
        System.out.println("---------------------------------------------------------------------");
        
        for (double t : testPoints) {
            Complex result = WaveletEvaluationUtils.evaluateAsComplex(morlet, t);
            System.out.printf("%.1f\t\t%.6f\t%.6f\t%.6f\t%.6f%n", 
                t, result.real(), result.imaginary(), result.magnitude(), result.phase());
        }
        
        System.out.println();
        
        // Test with scale and translation
        System.out.println("Wavelet evaluation with scale=2.0 and translation=0.5:");
        System.out.println("t\t\tReal Part\tImaginary Part\tMagnitude\tPhase");
        System.out.println("---------------------------------------------------------------------");
        
        double scale = 2.0;
        double translation = 0.5;
        
        for (double t : testPoints) {
            Complex result = WaveletEvaluationUtils.evaluateAsComplex(morlet, t, scale, translation);
            System.out.printf("%.1f\t\t%.6f\t%.6f\t%.6f\t%.6f%n", 
                t, result.real(), result.imaginary(), result.magnitude(), result.phase());
        }
        
        System.out.println();
        
        // Test null check
        System.out.println("Testing null check:");
        try {
            WaveletEvaluationUtils.evaluateAsComplex(null, 1.0);
        } catch (NullPointerException e) {
            System.out.println("✓ Null check works: " + e.getMessage());
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}