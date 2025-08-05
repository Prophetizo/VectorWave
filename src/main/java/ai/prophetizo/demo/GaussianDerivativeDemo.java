package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.cwt.*;

/**
 * Demonstrates Gaussian derivative wavelets for edge and feature detection.
 */
public class GaussianDerivativeDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Gaussian Derivative Wavelets Demo ===\n");
        
        // Create a test signal with edges and ridges
        int N = 256;
        double[] signal = createTestSignal(N);
        
        // Demo each Gaussian derivative order
        for (int order = 1; order <= 4; order++) {
            demonstrateGaussianDerivative(signal, order);
        }
        
        // Show wavelet registry
        System.out.println("\n=== Available Gaussian Derivative Wavelets ===");
        WaveletRegistry.getContinuousWavelets().stream()
            .filter(name -> name.startsWith("gaus"))
            .forEach(name -> {
                var wavelet = WaveletRegistry.getWavelet(name);
                System.out.println(name + ": " + wavelet.description());
            });
    }
    
    private static double[] createTestSignal(int N) {
        double[] signal = new double[N];
        
        // Step edge at 1/4
        for (int i = N/4; i < N/2; i++) {
            signal[i] = 1.0;
        }
        
        // Gaussian ridge at 3/4
        int center = 3*N/4;
        double sigma = 10.0;
        for (int i = 0; i < N; i++) {
            double x = (i - center) / sigma;
            signal[i] += 0.8 * Math.exp(-0.5 * x * x);
        }
        
        // Add some noise
        for (int i = 0; i < N; i++) {
            signal[i] += 0.05 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static void demonstrateGaussianDerivative(double[] signal, int order) {
        System.out.println("\n--- Gaussian Derivative Order " + order + " ---");
        
        // Get wavelet from registry
        GaussianDerivativeWavelet wavelet = (GaussianDerivativeWavelet) 
            WaveletRegistry.getWavelet("gaus" + order);
        
        System.out.println("Description: " + wavelet.description());
        System.out.println("Center frequency: " + wavelet.centerFrequency());
        System.out.println("Bandwidth: " + wavelet.bandwidth());
        
        // Create CWT transform
        CWTConfig config = CWTConfig.defaultConfig();
        CWTTransform transform = new CWTTransform(wavelet, config);
        
        // Analyze at multiple scales
        ScaleSpace scales = ScaleSpace.logarithmic(2.0, 20.0, 10);
        CWTResult result = transform.analyze(signal, scales);
        
        // Find maximum responses
        double[][] magnitude = result.getMagnitude();
        double maxResponse = 0;
        int maxTimeIdx = 0;
        int maxScaleIdx = 0;
        
        for (int s = 0; s < scales.getNumScales(); s++) {
            for (int t = 0; t < signal.length; t++) {
                if (magnitude[s][t] > maxResponse) {
                    maxResponse = magnitude[s][t];
                    maxTimeIdx = t;
                    maxScaleIdx = s;
                }
            }
        }
        
        System.out.printf("Maximum response: %.4f at time=%d, scale=%.2f%n",
            maxResponse, maxTimeIdx, scales.getScale(maxScaleIdx));
        
        // Detect features based on order
        switch (order) {
            case 1 -> detectEdges(result, signal.length);
            case 2 -> detectRidges(result, signal.length);
            case 3 -> detectInflectionPoints(result, signal.length);
            case 4 -> detectHigherOrderFeatures(result, signal.length);
        }
    }
    
    private static void detectEdges(CWTResult result, int signalLength) {
        System.out.println("Edge detection results:");
        double[][] coeffs = result.getCoefficients();
        
        // Look for zero crossings in the first derivative
        for (int t = 1; t < signalLength - 1; t++) {
            double prev = coeffs[2][t-1];
            double curr = coeffs[2][t];
            double next = coeffs[2][t+1];
            
            // Zero crossing with significant magnitude
            if (prev * next < 0 && Math.abs(curr) > 0.1) {
                System.out.printf("  Edge detected at position %d%n", t);
            }
        }
    }
    
    private static void detectRidges(CWTResult result, int signalLength) {
        System.out.println("Ridge/valley detection results:");
        double[][] coeffs = result.getCoefficients();
        
        // Look for strong negative responses (ridges)
        for (int t = 1; t < signalLength - 1; t++) {
            double val = coeffs[2][t];
            
            if (val < -0.2 && val < coeffs[2][t-1] && val < coeffs[2][t+1]) {
                System.out.printf("  Ridge detected at position %d (strength=%.3f)%n", t, -val);
            } else if (val > 0.2 && val > coeffs[2][t-1] && val > coeffs[2][t+1]) {
                System.out.printf("  Valley detected at position %d (strength=%.3f)%n", t, val);
            }
        }
    }
    
    private static void detectInflectionPoints(CWTResult result, int signalLength) {
        System.out.println("Inflection point detection results:");
        double[][] coeffs = result.getCoefficients();
        
        // Look for zero crossings in the third derivative
        for (int t = 1; t < signalLength - 1; t++) {
            double prev = coeffs[2][t-1];
            double next = coeffs[2][t+1];
            
            if (prev * next < 0) {
                System.out.printf("  Inflection point at position %d%n", t);
            }
        }
    }
    
    private static void detectHigherOrderFeatures(CWTResult result, int signalLength) {
        System.out.println("Higher-order feature detection results:");
        double[][] magnitude = result.getMagnitude();
        
        // Look for significant responses
        double threshold = 0.15;
        for (int t = 0; t < signalLength; t++) {
            if (magnitude[2][t] > threshold) {
                System.out.printf("  Feature at position %d (strength=%.3f)%n", t, magnitude[2][t]);
            }
        }
    }
}