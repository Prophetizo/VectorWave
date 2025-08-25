package ai.prophetizo.demo;

import ai.prophetizo.wavelet.WaveletOperations;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;

import java.util.Arrays;
import java.util.Random;

/**
 * Demonstrates the proper use of the WaveletOperations public API facade.
 * 
 * <p>This demo shows best practices for using the simplified public API
 * without accessing internal implementation details.</p>
 */
public class WaveletOperationsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== WaveletOperations API Demo ===\n");
        
        demonstratePerformanceInfo();
        demonstrateMODWTOperations();
        demonstrateThresholdingOperations();
        demonstrateDenoisingIntegration();
    }
    
    /**
     * Demonstrates how to query platform performance capabilities.
     */
    private static void demonstratePerformanceInfo() {
        System.out.println("1. Performance Information");
        System.out.println("-".repeat(50));
        
        WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
        
        System.out.println("Platform capabilities:");
        System.out.println("  " + perfInfo.description());
        System.out.println("  Vectorization enabled: " + perfInfo.vectorizationEnabled());
        System.out.println("  Platform: " + perfInfo.platformName());
        System.out.println("  Vector species: " + perfInfo.vectorSpecies());
        
        // Estimate speedup for different signal sizes
        System.out.println("\nEstimated speedups:");
        int[] sizes = {64, 256, 1024, 4096, 16384};
        for (int size : sizes) {
            double speedup = perfInfo.estimateSpeedup(size);
            System.out.printf("  Signal length %6d: %.2fx speedup\n", size, speedup);
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates MODWT convolution operations.
     */
    private static void demonstrateMODWTOperations() {
        System.out.println("2. MODWT Convolution Operations");
        System.out.println("-".repeat(50));
        
        // Create a test signal
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 8.0);
        }
        
        // Haar wavelet filters
        double[] lowPass = {0.7071067811865475, 0.7071067811865475};
        double[] highPass = {-0.7071067811865475, 0.7071067811865475};
        
        // Allocate output arrays
        double[] approx = new double[signal.length];
        double[] detail = new double[signal.length];
        
        // Perform MODWT convolution using public API
        System.out.println("Performing circular convolution (PERIODIC mode):");
        WaveletOperations.circularConvolveMODWT(signal, lowPass, approx);
        WaveletOperations.circularConvolveMODWT(signal, highPass, detail);
        
        System.out.printf("  Signal energy: %.4f\n", calculateEnergy(signal));
        System.out.printf("  Approx energy: %.4f\n", calculateEnergy(approx));
        System.out.printf("  Detail energy: %.4f\n", calculateEnergy(detail));
        
        // Zero-padding convolution
        System.out.println("\nPerforming zero-padding convolution:");
        WaveletOperations.zeroPaddingConvolveMODWT(signal, lowPass, approx);
        WaveletOperations.zeroPaddingConvolveMODWT(signal, highPass, detail);
        
        System.out.printf("  Approx energy: %.4f\n", calculateEnergy(approx));
        System.out.printf("  Detail energy: %.4f\n", calculateEnergy(detail));
        
        System.out.println();
    }
    
    /**
     * Demonstrates threshold operations for denoising.
     */
    private static void demonstrateThresholdingOperations() {
        System.out.println("3. Thresholding Operations");
        System.out.println("-".repeat(50));
        
        // Create noisy coefficients
        Random random = new Random(42);
        double[] coefficients = new double[64];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = random.nextGaussian() * 0.5;
        }
        
        double threshold = 0.3;
        
        // Soft thresholding
        double[] softThresholded = WaveletOperations.softThreshold(coefficients, threshold);
        long softNonZero = Arrays.stream(softThresholded)
            .filter(x -> x != 0.0)
            .count();
        
        System.out.println("Soft thresholding (threshold = " + threshold + "):");
        System.out.printf("  Original range: [%.3f, %.3f]\n", 
            Arrays.stream(coefficients).min().orElse(0),
            Arrays.stream(coefficients).max().orElse(0));
        System.out.printf("  Thresholded range: [%.3f, %.3f]\n",
            Arrays.stream(softThresholded).min().orElse(0),
            Arrays.stream(softThresholded).max().orElse(0));
        System.out.printf("  Non-zero coefficients: %d/%d (%.1f%%)\n",
            softNonZero, coefficients.length, 
            100.0 * softNonZero / coefficients.length);
        
        // Hard thresholding
        double[] hardThresholded = WaveletOperations.hardThreshold(coefficients, threshold);
        long hardNonZero = Arrays.stream(hardThresholded)
            .filter(x -> x != 0.0)
            .count();
        
        System.out.println("\nHard thresholding (threshold = " + threshold + "):");
        System.out.printf("  Non-zero coefficients: %d/%d (%.1f%%)\n",
            hardNonZero, coefficients.length,
            100.0 * hardNonZero / coefficients.length);
        
        // Compare energies
        double origEnergy = calculateEnergy(coefficients);
        double softEnergy = calculateEnergy(softThresholded);
        double hardEnergy = calculateEnergy(hardThresholded);
        
        System.out.println("\nEnergy comparison:");
        System.out.printf("  Original:        %.4f\n", origEnergy);
        System.out.printf("  Soft threshold:  %.4f (%.1f%% retained)\n", 
            softEnergy, 100.0 * softEnergy / origEnergy);
        System.out.printf("  Hard threshold:  %.4f (%.1f%% retained)\n", 
            hardEnergy, 100.0 * hardEnergy / origEnergy);
        
        System.out.println();
    }
    
    /**
     * Demonstrates integration with WaveletDenoiser.
     */
    private static void demonstrateDenoisingIntegration() {
        System.out.println("4. Denoising Integration");
        System.out.println("-".repeat(50));
        
        // Create a noisy signal
        Random random = new Random(42);
        double[] cleanSignal = new double[256];
        double[] noise = new double[256];
        
        for (int i = 0; i < cleanSignal.length; i++) {
            cleanSignal[i] = Math.sin(2 * Math.PI * i / 64.0) + 
                            0.3 * Math.cos(2 * Math.PI * i / 16.0);
            noise[i] = random.nextGaussian() * 0.2;
        }
        
        double[] noisySignal = new double[256];
        for (int i = 0; i < noisySignal.length; i++) {
            noisySignal[i] = cleanSignal[i] + noise[i];
        }
        
        // Use WaveletDenoiser which internally uses WaveletOperations
        WaveletDenoiser denoiser = new WaveletDenoiser.Builder()
            .withWavelet(Daubechies.DB4)
            .withThresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .withSoftThresholding(true)
            .build();
        
        double[] denoised = denoiser.denoise(noisySignal);
        
        // Calculate SNR improvement
        double noisyError = calculateRMSE(cleanSignal, noisySignal);
        double denoisedError = calculateRMSE(cleanSignal, denoised);
        
        System.out.println("Denoising results:");
        System.out.printf("  Noisy signal RMSE:    %.4f\n", noisyError);
        System.out.printf("  Denoised signal RMSE: %.4f\n", denoisedError);
        System.out.printf("  Improvement factor:   %.2fx\n", noisyError / denoisedError);
        
        // Show that denoiser uses optimized operations automatically
        boolean vectorized = WaveletOperations.getPerformanceInfo().vectorizationEnabled();
        System.out.println("\nDenoiser automatically uses " + 
            (vectorized ? "SIMD-optimized" : "scalar") + " operations");
        
        System.out.println();
    }
    
    // Helper methods
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
    
    private static double calculateRMSE(double[] reference, double[] signal) {
        if (reference.length != signal.length) {
            throw new IllegalArgumentException("Signals must have same length");
        }
        
        double sumSquaredError = 0;
        for (int i = 0; i < reference.length; i++) {
            double error = reference[i] - signal[i];
            sumSquaredError += error * error;
        }
        
        return Math.sqrt(sumSquaredError / reference.length);
    }
}