package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;

/**
 * Demonstration of the newly added wavelets in VectorWave.
 * 
 * This demo showcases:
 * - New Daubechies wavelets (DB6, DB8, DB10)
 * - Extended Symlet family (SYM5-SYM20)
 * - Additional Coiflet wavelets (COIF4, COIF5)
 */
public class NewWaveletsDemo {
    
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   VectorWave: New Wavelets Demonstration");
        System.out.println("==============================================\n");
        
        // List all available wavelets
        listAvailableWavelets();
        
        // Demonstrate Daubechies wavelets
        demonstrateDaubechiesWavelets();
        
        // Demonstrate Symlet wavelets
        demonstrateSymletWavelets();
        
        // Demonstrate Coiflet wavelets
        demonstrateCoifletWavelets();
        
        // Performance comparison
        performanceComparison();
        
        // Denoising demonstration
        denoisingDemo();
    }
    
    private static void listAvailableWavelets() {
        System.out.println("Available Orthogonal Wavelets:");
        System.out.println("------------------------------");
        
        var orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        int count = 0;
        for (String name : orthogonalWavelets) {
            System.out.printf("%-12s", name);
            count++;
            if (count % 6 == 0) {
                System.out.println();
            }
        }
        System.out.println("\n");
    }
    
    private static void demonstrateDaubechiesWavelets() {
        System.out.println("Daubechies Wavelets Demonstration");
        System.out.println("----------------------------------");
        
        // Create a test signal
        double[] signal = createTestSignal(64);
        
        // Test different Daubechies wavelets
        Daubechies[] wavelets = {
            Daubechies.DB2, Daubechies.DB4, 
            Daubechies.DB6, Daubechies.DB8, Daubechies.DB10
        };
        
        for (Daubechies wavelet : wavelets) {
            System.out.printf("%s: ", wavelet.name().toUpperCase());
            System.out.printf("Order=%d, Filter Length=%d, ", 
                wavelet.vanishingMoments(), 
                wavelet.lowPassDecomposition().length);
            
            // Perform transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Calculate reconstruction error
            double error = calculateRMSE(signal, reconstructed);
            System.out.printf("Reconstruction Error: %.2e\n", error);
        }
        System.out.println();
    }
    
    private static void demonstrateSymletWavelets() {
        System.out.println("Symlet Wavelets Demonstration");
        System.out.println("------------------------------");
        
        double[] signal = createTestSignal(64);
        
        // Test selected Symlet wavelets
        Symlet[] wavelets = {
            Symlet.SYM2, Symlet.SYM4, Symlet.SYM5, 
            Symlet.SYM6, Symlet.SYM8
        };
        
        for (Symlet wavelet : wavelets) {
            System.out.printf("%s: ", wavelet.name().toUpperCase());
            System.out.printf("Order=%d, Filter Length=%d, ", 
                wavelet.vanishingMoments(), 
                wavelet.lowPassDecomposition().length);
            
            // Perform transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(signal);
            
            // Calculate energy distribution
            double approxEnergy = calculateEnergy(result.approximationCoeffs());
            double detailEnergy = calculateEnergy(result.detailCoeffs());
            double totalEnergy = approxEnergy + detailEnergy;
            
            System.out.printf("Energy Distribution: Approx=%.1f%%, Detail=%.1f%%\n",
                (approxEnergy/totalEnergy)*100, (detailEnergy/totalEnergy)*100);
        }
        System.out.println();
    }
    
    private static void demonstrateCoifletWavelets() {
        System.out.println("Coiflet Wavelets Demonstration");
        System.out.println("-------------------------------");
        
        double[] signal = createTestSignal(64);
        
        // Test all Coiflet wavelets
        Coiflet[] wavelets = {
            Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3, 
            Coiflet.COIF4, Coiflet.COIF5
        };
        
        for (Coiflet wavelet : wavelets) {
            System.out.printf("%s: ", wavelet.name().toUpperCase());
            System.out.printf("Order=%d, Filter Length=%d, ", 
                wavelet.vanishingMoments(), 
                wavelet.lowPassDecomposition().length);
            
            // Test orthogonality
            double[] h = wavelet.lowPassDecomposition();
            double sum = 0;
            double sumSquares = 0;
            for (double coeff : h) {
                sum += coeff;
                sumSquares += coeff * coeff;
            }
            
            System.out.printf("Sum=%.6f (√2=%.6f), Sum²=%.6f\n", 
                sum, Math.sqrt(2), sumSquares);
        }
        System.out.println();
    }
    
    private static void performanceComparison() {
        System.out.println("Performance Comparison (1000 iterations)");
        System.out.println("-----------------------------------------");
        
        double[] signal = createTestSignal(1024);
        int iterations = 1000;
        
        // Test wavelets with different complexity
        Wavelet[] wavelets = {
            Haar.INSTANCE,
            Daubechies.DB2,
            Daubechies.DB6,
            Daubechies.DB10,
            Symlet.SYM4,
            Symlet.SYM8,
            Coiflet.COIF2,
            Coiflet.COIF4
        };
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Warm-up
            for (int i = 0; i < 100; i++) {
                MODWTResult result = transform.forward(signal);
                transform.inverse(result);
            }
            
            // Timing
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                MODWTResult result = transform.forward(signal);
                transform.inverse(result);
            }
            long endTime = System.nanoTime();
            
            double avgTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
            System.out.printf("%-8s: %.3f ms/transform\n", 
                wavelet.name().toUpperCase(), avgTimeMs);
        }
        System.out.println();
    }
    
    private static void denoisingDemo() {
        System.out.println("Denoising with Different Wavelets");
        System.out.println("----------------------------------");
        
        // Create noisy signal
        double[] cleanSignal = createTestSignal(128);
        double[] noisySignal = addNoise(cleanSignal, 0.1);
        
        // Test different wavelets for denoising
        Wavelet[] wavelets = {
            Daubechies.DB4,
            Daubechies.DB8,
            Symlet.SYM4,
            Symlet.SYM8,
            Coiflet.COIF2,
            Coiflet.COIF4
        };
        
        System.out.printf("Original SNR: %.2f dB\n", 
            calculateSNR(cleanSignal, noisySignal));
        System.out.println();
        
        for (Wavelet wavelet : wavelets) {
            WaveletDenoiser denoiser = new WaveletDenoiser(wavelet, BoundaryMode.PERIODIC);
            double[] denoised = denoiser.denoise(noisySignal, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL);
            
            double snr = calculateSNR(cleanSignal, denoised);
            double improvement = snr - calculateSNR(cleanSignal, noisySignal);
            
            System.out.printf("%-8s: SNR = %.2f dB (improvement: %.2f dB)\n", 
                wavelet.name().toUpperCase(), snr, improvement);
        }
    }
    
    // Helper methods
    
    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(8 * Math.PI * i / 32.0);
        }
        return signal;
    }
    
    private static double[] addNoise(double[] signal, double noiseLevel) {
        double[] noisy = new double[signal.length];
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + noiseLevel * rng.nextGaussian();
        }
        return noisy;
    }
    
    private static double calculateRMSE(double[] original, double[] reconstructed) {
        double sumSquaredError = 0;
        for (int i = 0; i < original.length; i++) {
            double error = original[i] - reconstructed[i];
            sumSquaredError += error * error;
        }
        return Math.sqrt(sumSquaredError / original.length);
    }
    
    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
    private static double calculateSNR(double[] signal, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < signal.length; i++) {
            signalPower += signal[i] * signal[i];
            double noise = noisy[i] - signal[i];
            noisePower += noise * noise;
        }
        
        if (noisePower == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        return 10 * Math.log10(signalPower / noisePower);
    }
}