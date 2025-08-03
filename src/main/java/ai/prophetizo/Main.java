package ai.prophetizo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.MODWTTransform;
import ai.prophetizo.wavelet.MODWTTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates the VectorWave library with various wavelet types.
 * Now includes both DWT and MODWT implementations.
 */
public class Main {
    public static void main(String[] args) {
        // Test signal (power-of-2 for DWT compatibility)
        double[] signal = {10, 12, 15, 18, 20, 17, 14, 11};
        System.out.println("VectorWave - Fast Wavelet Transform Demo");
        System.out.println("========================================");
        System.out.println("Original Signal: " + Arrays.toString(signal));
        System.out.println();

        // Create factories for building transforms
        WaveletTransformFactory dwtFactory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        MODWTTransformFactory modwtFactory = new MODWTTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);

        // Demonstrate DWT vs MODWT comparison
        demonstrateDWTvsMODWT(signal, dwtFactory, modwtFactory);
        
        // Demonstrate different wavelet types with DWT
        demonstrateOrthogonalWavelets(signal, dwtFactory);
        demonstrateBiorthogonalWavelets(signal, dwtFactory);
        demonstrateContinuousWavelets(signal, dwtFactory);
        demonstrateWaveletRegistry();
        
        // Show MODWT advantages
        demonstrateMODWTAdvantages();
    }

    private static void demonstrateDWTvsMODWT(double[] signal, WaveletTransformFactory dwtFactory, MODWTTransformFactory modwtFactory) {
        System.out.println("DWT vs MODWT COMPARISON");
        System.out.println("========================");
        
        WaveletTransform dwt = dwtFactory.create(new Haar());
        MODWTTransform modwt = modwtFactory.create(new Haar());
        
        TransformResult dwtResult = dwt.forward(signal);
        TransformResult modwtResult = modwt.forward(signal);
        
        System.out.println("Input length: " + signal.length);
        System.out.println();
        
        System.out.println("DWT Results (with downsampling):");
        System.out.println("   Approximation length: " + dwtResult.approximationCoeffs().length);
        System.out.println("   Detail length: " + dwtResult.detailCoeffs().length);
        System.out.println("   Approximation: " + Arrays.toString(dwtResult.approximationCoeffs()));
        System.out.println("   Detail:        " + Arrays.toString(dwtResult.detailCoeffs()));
        
        double[] dwtReconstructed = dwt.inverse(dwtResult);
        double dwtMaxError = calculateMaxError(signal, dwtReconstructed);
        System.out.println("   Max reconstruction error: " + String.format("%.2e", dwtMaxError));
        System.out.println();
        
        System.out.println("MODWT Results (without downsampling):");
        System.out.println("   Approximation length: " + modwtResult.approximationCoeffs().length);
        System.out.println("   Detail length: " + modwtResult.detailCoeffs().length);
        System.out.println("   Approximation: " + Arrays.toString(modwtResult.approximationCoeffs()));
        System.out.println("   Detail:        " + Arrays.toString(modwtResult.detailCoeffs()));
        
        double[] modwtReconstructed = modwt.inverse(modwtResult);
        double modwtMaxError = calculateMaxError(signal, modwtReconstructed);
        System.out.println("   Max reconstruction error: " + String.format("%.2e", modwtMaxError));
        System.out.println();
        
        System.out.println("Key Differences:");
        System.out.println("• DWT: Output length = Input length / 2 (downsampling)");
        System.out.println("• MODWT: Output length = Input length (no downsampling)");
        System.out.println("• MODWT: Shift-invariant, better for pattern analysis");
        System.out.println("• MODWT: Works with any signal length (not just power-of-2)");
        System.out.println();
    }

    private static void demonstrateOrthogonalWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("ORTHOGONAL WAVELETS (DWT)");
        System.out.println("==========================");

        // Haar wavelet
        System.out.println("\n1. Haar Wavelet:");
        performTransform(signal, factory.create(new Haar()));

        // Daubechies wavelets
        System.out.println("\n2. Daubechies DB2:");
        performTransform(signal, factory.create(Daubechies.DB2));

        System.out.println("\n3. Daubechies DB4:");
        performTransform(signal, factory.create(Daubechies.DB4));

        // Symlet wavelet
        System.out.println("\n4. Symlet SYM2:");
        performTransform(signal, factory.create(Symlet.SYM2));

        // Coiflet wavelet
        System.out.println("\n5. Coiflet COIF1:");
        performTransform(signal, factory.create(Coiflet.COIF1));
    }

    private static void demonstrateBiorthogonalWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("\n\nBIORTHOGONAL WAVELETS (DWT)");
        System.out.println("===========================");

        System.out.println("\n1. Biorthogonal Spline BIOR1.3:");
        performTransform(signal, factory.create(BiorthogonalSpline.BIOR1_3));
    }

    private static void demonstrateContinuousWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("\n\nCONTINUOUS WAVELETS (Discretized DWT)");
        System.out.println("=====================================");

        System.out.println("\n1. Morlet Wavelet:");
        performTransform(signal, factory.create(new MorletWavelet()));
    }

    private static void performTransform(double[] signal, WaveletTransform transform) {
        try {
            // Forward transform
            TransformResult result = transform.forward(signal);
            System.out.println("   Approximation: " + Arrays.toString(result.approximationCoeffs()));
            System.out.println("   Detail:        " + Arrays.toString(result.detailCoeffs()));

            // Inverse transform
            double[] reconstructed = transform.inverse(result);
            
            // Calculate reconstruction error
            double maxError = calculateMaxError(signal, reconstructed);
            System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
    }

    private static void demonstrateWaveletRegistry() {
        System.out.println("\n\nWAVELET REGISTRY");
        System.out.println("================");
        
        System.out.println("\nAvailable wavelets: " + WaveletRegistry.getAvailableWavelets());
        System.out.println("\nOrthogonal wavelets: " + WaveletRegistry.getOrthogonalWavelets());
        System.out.println("Biorthogonal wavelets: " + WaveletRegistry.getBiorthogonalWavelets());
        System.out.println("Continuous wavelets: " + WaveletRegistry.getContinuousWavelets());
        
        System.out.println("\nGetting wavelet by name:");
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        System.out.println("   db4: " + db4.description());
    }

    private static void demonstrateMODWTAdvantages() {
        System.out.println("\n\nMODWT ADVANTAGES DEMONSTRATION");
        System.out.println("===============================");
        System.out.println("For more comprehensive MODWT examples, run:");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"ai.prophetizo.demo.MODWTDemo\"");
        System.out.println();
        
        // Quick demo of non-power-of-2 length
        double[] nonPowerOf2Signal = {1, 2, 3, 4, 5};  // Length 5
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        System.out.println("Example: MODWT with non-power-of-2 signal");
        System.out.println("Signal: " + Arrays.toString(nonPowerOf2Signal) + " (length=" + nonPowerOf2Signal.length + ")");
        
        TransformResult result = modwt.forward(nonPowerOf2Signal);
        System.out.println("Approximation: " + Arrays.toString(result.approximationCoeffs()));
        System.out.println("Detail:        " + Arrays.toString(result.detailCoeffs()));
        
        double[] reconstructed = modwt.inverse(result);
        double maxError = calculateMaxError(nonPowerOf2Signal, reconstructed);
        System.out.println("Max reconstruction error: " + String.format("%.2e", maxError));
        System.out.println();
        
        System.out.println("✓ MODWT works with any signal length!");
        System.out.println("✓ Same-length output preserves all information!");
        System.out.println("✓ Shift-invariant for better pattern detection!");
    }

    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0;
        for (int i = 0; i < original.length; i++) {
            maxError = Math.max(maxError, Math.abs(original[i] - reconstructed[i]));
        }
        return maxError;
    }
}