package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MutableMultiLevelMODWTResult;
import ai.prophetizo.wavelet.swt.VectorWaveSwtAdapter;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates basic usage patterns for the VectorWave library using MODWT and SWT.
 *
 * <p>This demo covers:
 * <ul>
 *   <li>Creating MODWT transforms</li>
 *   <li>Forward and inverse transforms</li>
 *   <li>Using different wavelet families</li>
 *   <li>Working with arbitrary length signals (not just power-of-2)</li>
 *   <li>SWT for shift-invariant analysis</li>
 *   <li>Error handling best practices</li>
 * </ul>
 */
public class BasicUsageDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Basic Usage Demo ===\n");

        // Demo 1: Simple transform with Haar wavelet
        demonstrateBasicTransform();

        // Demo 2: Working with different wavelets
        demonstrateDifferentWavelets();

        // Demo 3: Working with different signal lengths
        demonstrateSignalLengths();

        // Demo 4: Error handling
        demonstrateErrorHandling();

        // Demo 5: Coefficient analysis
        demonstrateCoefficientAnalysis();
        
        // Demo 6: SWT for shift-invariant analysis
        demonstrateSWT();
    }

    private static void demonstrateBasicTransform() {
        System.out.println("1. Basic Transform Example");
        System.out.println("--------------------------");

        // Create a simple signal
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        System.out.println("Original signal: " + Arrays.toString(signal));

        // Create MODWT transform with Haar wavelet
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

        // Forward transform
        MODWTResult result = transform.forward(signal);
        System.out.println("\nForward transform:");
        System.out.println("  Approximation: " + Arrays.toString(result.approximationCoeffs()));
        System.out.println("  Detail:        " + Arrays.toString(result.detailCoeffs()));

        // Inverse transform
        double[] reconstructed = transform.inverse(result);
        System.out.println("\nReconstructed:   " + Arrays.toString(reconstructed));

        // Verify perfect reconstruction
        double error = calculateMaxError(signal, reconstructed);
        System.out.printf("Max error: %.2e (should be ~0)\n\n", error);
    }

    private static void demonstrateDifferentWavelets() {
        System.out.println("2. Different Wavelet Families");
        System.out.println("-----------------------------");

        double[] signal = generateSineWave(64);
        System.out.println("Signal length: " + signal.length);

        // Try different wavelets
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB4,
            Symlet.SYM3,
            Coiflet.COIF1
        };

        for (Wavelet wavelet : wavelets) {
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(signal);
            
            System.out.printf("%s: %d coefficients (same as input)\n",
                    wavelet.getClass().getSimpleName(),
                    result.approximationCoeffs().length);
        }
        System.out.println();
    }

    private static void demonstrateSignalLengths() {
        System.out.println("3. Different Signal Lengths (MODWT Advantage)");
        System.out.println("---------------------------------------------");

        MODWTTransform transform = new MODWTTransform(
                Daubechies.DB2, BoundaryMode.PERIODIC);

        // Test various lengths - MODWT works with ANY length!
        int[] lengths = {7, 13, 25, 64, 100, 256};

        System.out.println("Signal Length -> Approx + Detail Coefficients");
        for (int length : lengths) {
            double[] signal = new double[length];
            Arrays.fill(signal, 1.0); // Constant signal

            MODWTResult result = transform.forward(signal);
            System.out.printf("  %4d -> %d + %d = %d total\n",
                    length,
                    result.approximationCoeffs().length,
                    result.detailCoeffs().length,
                    result.approximationCoeffs().length + result.detailCoeffs().length);
        }

        System.out.println("\nNote: MODWT works with ANY signal length!\n");
    }

    private static void demonstrateErrorHandling() {
        System.out.println("4. Error Handling Best Practices");
        System.out.println("--------------------------------");

        MODWTTransform transform = new MODWTTransform(
                new Haar(), BoundaryMode.PERIODIC);

        // Example 1: Empty signal (MODWT still requires non-empty signal)
        try {
            double[] emptySignal = new double[0];
            transform.forward(emptySignal);
        } catch (Exception e) {
            System.out.println("Expected error for empty signal: " + e.getMessage());
        }

        // Example 2: Null signal
        try {
            transform.forward(null);
        } catch (Exception e) {
            System.out.println("Expected error for null signal: " + e.getMessage());
        }

        // Example 3: Signal with NaN
        try {
            double[] nanSignal = {1.0, 2.0, Double.NaN, 4.0};
            transform.forward(nanSignal);
        } catch (Exception e) {
            System.out.println("Expected error for NaN in signal: " + e.getMessage());
        }

        // Example 4: Signal with Infinity
        try {
            double[] infSignal = {1.0, 2.0, Double.POSITIVE_INFINITY, 4.0};
            transform.forward(infSignal);
        } catch (Exception e) {
            System.out.println("Expected error for infinity in signal: " + e.getMessage());
        }

        System.out.println("\nAlways validate input data before transformation!\n");
    }

    private static void demonstrateCoefficientAnalysis() {
        System.out.println("5. Coefficient Analysis");
        System.out.println("-----------------------");

        // Create signal with multiple frequency components
        int length = 128;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +    // Low frequency
                    0.5 * Math.sin(2 * Math.PI * i / 8.0) +  // Medium frequency
                    0.1 * Math.sin(2 * Math.PI * i / 2.0);   // High frequency
        }

        MODWTTransform transform = new MODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        MODWTResult result = transform.forward(signal);

        // Analyze approximation coefficients (low frequency)
        double approxEnergy = calculateEnergy(result.approximationCoeffs());
        double approxMax = findMaxAbsolute(result.approximationCoeffs());

        // Analyze detail coefficients (high frequency)
        double detailEnergy = calculateEnergy(result.detailCoeffs());
        double detailMax = findMaxAbsolute(result.detailCoeffs());

        double totalEnergy = approxEnergy + detailEnergy;

        System.out.println("Coefficient Analysis:");
        System.out.printf("  Approximation energy: %.4f (%.1f%%)\n",
                approxEnergy, 100 * approxEnergy / totalEnergy);
        System.out.printf("  Detail energy:        %.4f (%.1f%%)\n",
                detailEnergy, 100 * detailEnergy / totalEnergy);
        System.out.printf("  Max approx coeff:     %.4f\n", approxMax);
        System.out.printf("  Max detail coeff:     %.4f\n", detailMax);

        // Demonstrate coefficient thresholding
        System.out.println("\nThresholding example:");
        double threshold = 0.1;
        int zeroedCount = 0;
        double[] originalDetail = result.detailCoeffs();
        double[] modifiedDetail = new double[originalDetail.length];
        
        // Create a modified copy of detail coefficients
        for (int i = 0; i < originalDetail.length; i++) {
            if (Math.abs(originalDetail[i]) < threshold) {
                modifiedDetail[i] = 0;
                zeroedCount++;
            } else {
                modifiedDetail[i] = originalDetail[i];
            }
        }
        System.out.printf("  Zeroed %d of %d detail coefficients (%.1f%%)\n",
                zeroedCount, originalDetail.length,
                100.0 * zeroedCount / originalDetail.length);

        // Reconstruct with modified coefficients using factory method
        MODWTResult modifiedResult = MODWTResult.create(
                result.approximationCoeffs(), modifiedDetail);
        double[] reconstructed = transform.inverse(modifiedResult);

        double compressionError = calculateMaxError(signal, reconstructed);
        System.out.printf("  Compression error: %.4f\n", compressionError);
    }
    
    private static void demonstrateSWT() {
        System.out.println("\n6. SWT (Stationary Wavelet Transform)");
        System.out.println("-------------------------------------");
        System.out.println("SWT provides shift-invariant analysis with automatic optimizations.\n");
        
        // Create a signal with a transient event
        int length = 256;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0); // Base frequency
            if (i >= 100 && i <= 110) {
                signal[i] += 2.0; // Transient spike
            }
        }
        
        // Create SWT adapter - optimizations are automatic!
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Multi-level decomposition
        int levels = 3;
        MutableMultiLevelMODWTResult swtResult = swt.forward(signal, levels);
        
        System.out.println("SWT Decomposition:");
        System.out.printf("  Signal length: %d\n", signal.length);
        System.out.printf("  Decomposition levels: %d\n", levels);
        System.out.printf("  Shift-invariant: YES\n");
        
        // Analyze energy distribution
        System.out.println("\nEnergy distribution across levels:");
        double totalEnergy = 0;
        double[] levelEnergies = new double[levels + 1];
        
        // Approximation energy
        double[] approx = swtResult.getMutableApproximationCoeffs();
        levelEnergies[0] = calculateEnergy(approx);
        totalEnergy += levelEnergies[0];
        
        // Detail energies
        for (int level = 1; level <= levels; level++) {
            double[] detail = swtResult.getMutableDetailCoeffs(level);
            levelEnergies[level] = calculateEnergy(detail);
            totalEnergy += levelEnergies[level];
        }
        
        System.out.printf("  Approximation: %.2f%%\n", 
                         100 * levelEnergies[0] / totalEnergy);
        for (int level = 1; level <= levels; level++) {
            System.out.printf("  Level %d detail: %.2f%%\n", 
                             level, 100 * levelEnergies[level] / totalEnergy);
        }
        
        // Demonstrate denoising
        System.out.println("\nDenoising example:");
        
        // Add noise to signal
        double[] noisySignal = new double[length];
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < length; i++) {
            noisySignal[i] = signal[i] + 0.2 * rand.nextGaussian();
        }
        
        // Denoise using universal threshold
        double[] denoised = swt.denoise(noisySignal, levels, -1, true);
        
        double noiseError = calculateMaxError(signal, noisySignal);
        double denoisedError = calculateMaxError(signal, denoised);
        
        System.out.printf("  Noisy signal error: %.4f\n", noiseError);
        System.out.printf("  Denoised signal error: %.4f\n", denoisedError);
        System.out.printf("  Improvement: %.1f%%\n", 
                         100 * (noiseError - denoisedError) / noiseError);
        
        // Feature extraction at specific level
        System.out.println("\nFeature extraction (level 2 only):");
        double[] level2Features = swt.extractLevel(signal, levels, 2);
        double level2Energy = calculateEnergy(level2Features);
        System.out.printf("  Level 2 features energy: %.4f\n", level2Energy);
        
        // Clean up resources
        swt.cleanup();
        
        System.out.println("\nKey advantages of SWT:");
        System.out.println("  • Shift-invariant (redundant representation)");
        System.out.println("  • Better for pattern detection and alignment");
        System.out.println("  • Automatic internal optimizations");
        System.out.println("  • Perfect for denoising applications");
    }

    // Helper methods

    private static double[] generateSineWave(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / (length / 4.0));
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

    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }

    private static double findMaxAbsolute(double[] coeffs) {
        double max = 0;
        for (double c : coeffs) {
            max = Math.max(max, Math.abs(c));
        }
        return max;
    }
}