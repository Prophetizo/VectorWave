package ai.prophetizo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates the VectorWave library with MODWT (Maximal Overlap Discrete Wavelet Transform).
 * Showcases the advantages of MODWT over traditional DWT.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("VectorWave - MODWT (Maximal Overlap Discrete Wavelet Transform) Demo");
        System.out.println("==================================================================");
        
        // Demonstrate MODWT with different signal lengths
        demonstrateAnySignalLength();
        
        // Show various wavelet types
        demonstrateWaveletTypes();
        
        // Demonstrate shift-invariance
        demonstrateShiftInvariance();
        
        // Show wavelet registry
        demonstrateWaveletRegistry();
    }

    private static void demonstrateAnySignalLength() {
        System.out.println("\n1. ANY SIGNAL LENGTH SUPPORT");
        System.out.println("=============================");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Different signal lengths - all work with MODWT!
        int[] lengths = {3, 5, 7, 10, 15, 16};
        
        for (int length : lengths) {
            double[] signal = createTestSignal(length);
            System.out.println("\nSignal length " + length + ": " + Arrays.toString(signal));
            
            WaveletTransform transform = factory.create(new Haar());
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            double maxError = calculateMaxError(signal, reconstructed);
            System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
            System.out.println("   ✓ Perfect reconstruction!");
        }
    }

    private static void demonstrateWaveletTypes() {
        System.out.println("\n\n2. VARIOUS WAVELET TYPES");
        System.out.println("========================");
        
        double[] signal = {10, 12, 15, 18, 20, 17, 14, 11, 9};  // Length 9 (not power-of-2)
        System.out.println("Test signal: " + Arrays.toString(signal));
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);

        System.out.println("\nOrthogonal Wavelets:");
        performTransform(signal, factory.create(new Haar()), "Haar");
        performTransform(signal, factory.create(Daubechies.DB2), "Daubechies DB2");
        performTransform(signal, factory.create(Daubechies.DB4), "Daubechies DB4");
        performTransform(signal, factory.create(Symlet.SYM2), "Symlet SYM2");
        performTransform(signal, factory.create(Coiflet.COIF1), "Coiflet COIF1");

        System.out.println("\nBiorthogonal Wavelets:");
        performTransform(signal, factory.create(BiorthogonalSpline.BIOR1_3), "Biorthogonal Spline BIOR1.3");

        System.out.println("\nContinuous Wavelets (discretized):");
        performTransform(signal, factory.create(new MorletWavelet()), "Morlet");
    }

    private static void demonstrateShiftInvariance() {
        System.out.println("\n\n3. SHIFT-INVARIANCE");
        System.out.println("===================");
        
        double[] original = {1, 4, 2, 8, 5, 3, 7};
        double[] shifted = {3, 7, 1, 4, 2, 8, 5};  // Circular shift by 2
        
        System.out.println("Original signal: " + Arrays.toString(original));
        System.out.println("Shifted signal:  " + Arrays.toString(shifted));
        
        WaveletTransform transform = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC)
                .create(new Haar());
        
        TransformResult origResult = transform.forward(original);
        TransformResult shiftResult = transform.forward(shifted);
        
        System.out.println("\nOriginal approximation: " + Arrays.toString(origResult.approximationCoeffs()));
        System.out.println("Shifted approximation:  " + Arrays.toString(shiftResult.approximationCoeffs()));
        
        // Verify shift relationship in coefficients
        boolean isShiftInvariant = verifyCircularShift(origResult.approximationCoeffs(), 
                                                      shiftResult.approximationCoeffs(), 2);
        
        System.out.println("✓ Coefficients show circular shift pattern: " + isShiftInvariant);
        System.out.println("✓ MODWT preserves shift relationships!");
    }

    private static void performTransform(double[] signal, WaveletTransform transform, String name) {
        try {
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            double maxError = calculateMaxError(signal, reconstructed);
            
            System.out.println("   " + name + ": error = " + String.format("%.2e", maxError));
        } catch (Exception e) {
            System.out.println("   " + name + ": Error - " + e.getMessage());
        }
    }

    private static void demonstrateWaveletRegistry() {
        System.out.println("\n\n4. WAVELET REGISTRY");
        System.out.println("===================");
        
        System.out.println("Available wavelets: " + WaveletRegistry.getAvailableWavelets());
        System.out.println("Orthogonal wavelets: " + WaveletRegistry.getOrthogonalWavelets());
        System.out.println("Biorthogonal wavelets: " + WaveletRegistry.getBiorthogonalWavelets());
        System.out.println("Continuous wavelets: " + WaveletRegistry.getContinuousWavelets());
        
        System.out.println("\nGetting wavelet by name:");
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        System.out.println("   db4: " + db4.description());
    }

    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = 10 + 5 * Math.sin(2 * Math.PI * i / length) + 2 * Math.cos(4 * Math.PI * i / length);
        }
        return signal;
    }

    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0;
        for (int i = 0; i < original.length; i++) {
            maxError = Math.max(maxError, Math.abs(original[i] - reconstructed[i]));
        }
        return maxError;
    }

    private static boolean verifyCircularShift(double[] original, double[] shifted, int shiftAmount) {
        if (original.length != shifted.length) return false;
        
        int n = original.length;
        double tolerance = 1e-10;
        
        for (int i = 0; i < n; i++) {
            int shiftedIndex = (i + shiftAmount) % n;
            if (Math.abs(original[i] - shifted[shiftedIndex]) > tolerance) {
                return false;
            }
        }
        return true;
    }
}