package ai.prophetizo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates the VectorWave library with various wavelet types.
 */
public class Main {
    public static void main(String[] args) {
        // Test signal
        double[] signal = {10, 12, 15, 18, 20, 17, 14, 11};
        System.out.println("VectorWave - Fast Wavelet Transform Demo");
        System.out.println("========================================");
        System.out.println("Original Signal: " + Arrays.toString(signal));
        System.out.println();

        // Create factory for building transforms
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);

        // Demonstrate different wavelet types
        demonstrateOrthogonalWavelets(signal, factory);
        demonstrateBiorthogonalWavelets(signal, factory);
        demonstrateContinuousWavelets(signal, factory);
        demonstrateWaveletRegistry();
        
        // Point users to comprehensive error handling examples
        System.out.println("\n\nFor comprehensive error handling examples, run:");
        System.out.println("java -cp target/classes ai.prophetizo.ErrorHandlingDemo");
    }

    private static void demonstrateOrthogonalWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("ORTHOGONAL WAVELETS");
        System.out.println("===================");

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
        System.out.println("\n\nBIORTHOGONAL WAVELETS");
        System.out.println("=====================");

        System.out.println("\n1. Biorthogonal Spline BIOR1.3:");
        performTransform(signal, factory.create(BiorthogonalSpline.BIOR1_3));
    }

    private static void demonstrateContinuousWavelets(double[] signal, WaveletTransformFactory factory) {
        System.out.println("\n\nCONTINUOUS WAVELETS (Discretized)");
        System.out.println("==================================");

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
            double maxError = 0;
            for (int i = 0; i < signal.length; i++) {
                maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
            }
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
}