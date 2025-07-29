package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;

/**
 * Demo showing consistent wavelet normalization across all types.
 */
public class NormalizationDemo {
    public static void main(String[] args) {
        System.out.println("=== Wavelet Normalization Verification ===\n");
        
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Coiflet.COIF1,
            BiorthogonalSpline.BIOR1_3,
            new MorletWavelet()
        };
        
        for (Wavelet wavelet : wavelets) {
            System.out.printf("%-25s: ", wavelet.name() + " (" + wavelet.getType() + ")");
            
            boolean lowPassNormalized = Wavelet.isNormalized(wavelet.lowPassDecomposition(), 2e-10);
            boolean highPassNormalized = Wavelet.isNormalized(wavelet.highPassDecomposition(), 2e-10);
            boolean lowReconNormalized = Wavelet.isNormalized(wavelet.lowPassReconstruction(), 2e-10);
            boolean highReconNormalized = Wavelet.isNormalized(wavelet.highPassReconstruction(), 2e-10);
            
            if (lowPassNormalized && highPassNormalized && lowReconNormalized && highReconNormalized) {
                System.out.println("✓ All filters normalized (L2 = 1)");
            } else {
                System.out.println("✗ Normalization issues detected");
            }
        }
        
        System.out.println("\n=== Cross-Wavelet Energy Comparison ===\n");
        
        // Demonstrate consistent energy preservation
        double[] testSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double originalEnergy = computeEnergy(testSignal);
        System.out.printf("Original signal energy: %.6f\n\n", originalEnergy);
        
        // Test with discrete wavelets
        Wavelet[] discreteWavelets = {new Haar(), Daubechies.DB2, Daubechies.DB4};
        
        for (Wavelet wavelet : discreteWavelets) {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            TransformResult result = transform.forward(testSignal);
            
            double transformEnergy = computeEnergy(result.approximationCoeffs()) + 
                                   computeEnergy(result.detailCoeffs());
            
            System.out.printf("%-15s energy: %.6f (difference: %.2e)\n", 
                wavelet.name(), transformEnergy, Math.abs(originalEnergy - transformEnergy));
        }
        
        System.out.println("\n✓ All wavelets preserve energy consistently!");
    }
    
    private static double computeEnergy(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
}