package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates optimization techniques using various wavelets.
 * Shows compression, denoising, and signal processing optimizations.
 */
public class OptimizationDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Optimization Demo");
        System.out.println("===============================");
        
        // Test signal with noise
        double[] signal = generateTestSignal();
        System.out.println("Original Signal: " + Arrays.toString(signal));
        
        demonstrateCompressionOptimization(signal);
        demonstrateWaveletSelection(signal);
        demonstrateBoundaryModeOptimization(signal);
    }
    
    private static double[] generateTestSignal() {
        // Generate a signal with multiple frequency components and noise
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 
                5.0 * Math.sin(2 * Math.PI * i / 8.0) +        // Low frequency
                2.0 * Math.sin(2 * Math.PI * i / 2.0) +        // High frequency
                0.5 * (Math.random() - 0.5);                   // Noise
        }
        return signal;
    }
    
    private static void demonstrateCompressionOptimization(double[] signal) {
        System.out.println("\n1. Compression Optimization:");
        System.out.println("----------------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Compare different wavelets for compression
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2
        };
        
        for (Wavelet wavelet : wavelets) {
            WaveletTransform transform = factory.create(wavelet);
            
            try {
                TransformResult result = transform.forward(signal);
                
                // Calculate compression ratio by thresholding
                double threshold = 0.1;
                double[] details = result.detailCoeffs();
                int significantCoeffs = 0;
                
                for (double coeff : details) {
                    if (Math.abs(coeff) > threshold) {
                        significantCoeffs++;
                    }
                }
                
                double compressionRatio = (double) significantCoeffs / details.length;
                
                System.out.printf("%-12s: Compression ratio: %.2f (%.0f%% coefficients retained)\n",
                    wavelet.name(), compressionRatio, compressionRatio * 100);
                
            } catch (Exception e) {
                System.out.println(wavelet.name() + ": Error - " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateWaveletSelection(double[] signal) {
        System.out.println("\n2. Optimal Wavelet Selection:");
        System.out.println("-----------------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Coiflet.COIF1,
            BiorthogonalSpline.BIOR1_3
        };
        
        System.out.println("Evaluating wavelets for signal characteristics:");
        
        for (Wavelet wavelet : wavelets) {
            WaveletTransform transform = factory.create(wavelet);
            
            try {
                TransformResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                
                // Calculate reconstruction error
                double mse = 0.0;
                for (int i = 0; i < signal.length; i++) {
                    double error = signal[i] - reconstructed[i];
                    mse += error * error;
                }
                mse /= signal.length;
                
                // Calculate energy concentration in approximation
                double[] approx = result.approximationCoeffs();
                double[] detail = result.detailCoeffs();
                
                double approxEnergy = calculateEnergy(approx);
                double detailEnergy = calculateEnergy(detail);
                double energyRatio = approxEnergy / (approxEnergy + detailEnergy);
                
                System.out.printf("%-15s: MSE=%.6f, Energy ratio=%.3f", 
                    wavelet.name(), mse, energyRatio);
                
                // Additional metrics for orthogonal wavelets
                if (wavelet instanceof OrthogonalWavelet orthWavelet) {
                    System.out.printf(", Vanishing moments=%d", orthWavelet.vanishingMoments());
                }
                System.out.println();
                
            } catch (Exception e) {
                System.out.println(wavelet.name() + ": Error - " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateBoundaryModeOptimization(double[] signal) {
        System.out.println("\n3. Boundary Mode Optimization:");
        System.out.println("------------------------------");
        
        BoundaryMode[] modes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING};
        Wavelet wavelet = Daubechies.DB4;
        
        for (BoundaryMode mode : modes) {
            WaveletTransformFactory factory = new WaveletTransformFactory()
                    .withBoundaryMode(mode);
            WaveletTransform transform = factory.create(wavelet);
            
            try {
                long startTime = System.nanoTime();
                TransformResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                long endTime = System.nanoTime();
                
                double executionTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                
                // Calculate boundary artifacts
                double boundaryError = calculateBoundaryError(signal, reconstructed);
                
                System.out.printf("%-13s: Time=%.3f ms, Boundary error=%.6f\n",
                    mode.name(), executionTime, boundaryError);
                
            } catch (Exception e) {
                System.out.println(mode.name() + ": Error - " + e.getMessage());
            }
        }
    }
    
    private static double calculateEnergy(double[] coeffs) {
        double energy = 0.0;
        for (double coeff : coeffs) {
            energy += coeff * coeff;
        }
        return energy;
    }
    
    private static double calculateBoundaryError(double[] original, double[] reconstructed) {
        if (original.length != reconstructed.length) {
            return Double.MAX_VALUE;
        }
        
        // Focus on boundary elements (first and last few elements)
        int boundarySize = Math.min(2, original.length / 4);
        double error = 0.0;
        int count = 0;
        
        // Start boundary
        for (int i = 0; i < boundarySize; i++) {
            double diff = original[i] - reconstructed[i];
            error += diff * diff;
            count++;
        }
        
        // End boundary
        for (int i = original.length - boundarySize; i < original.length; i++) {
            double diff = original[i] - reconstructed[i];
            error += diff * diff;
            count++;
        }
        
        return count > 0 ? error / count : 0.0;
    }
}