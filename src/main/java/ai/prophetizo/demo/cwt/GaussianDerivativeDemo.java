package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet;
import ai.prophetizo.wavelet.cwt.CWTTransform;
import ai.prophetizo.wavelet.cwt.CWTResult;
import ai.prophetizo.wavelet.cwt.ScaleSpace;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.WaveletName;
/**
 * Demonstrates Gaussian derivative wavelets for feature detection.
 * 
 * <p>This demo shows how to use Gaussian derivative wavelets of different orders
 * for detecting edges, ridges, inflection points, and other features in signals.</p>
 */
public class GaussianDerivativeDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Gaussian Derivative Wavelets Demo ===\n");
        
        // Demo 1: Overview of Gaussian derivative wavelets
        waveletOverview();
        
        // Demo 2: Edge detection with 1st derivative
        edgeDetection();
        
        // Demo 3: Ridge detection with 2nd derivative (Mexican Hat)
        ridgeDetection();
        
        // Demo 4: Higher-order feature detection
        higherOrderFeatures();
        
        // Demo 5: Multi-scale feature analysis
        multiScaleAnalysis();
        
        // Demo 6: Registry integration
        registryIntegration();
    }
    
    /**
     * Provides an overview of Gaussian derivative wavelets and their properties.
     */
    private static void waveletOverview() {
        System.out.println("1. Gaussian Derivative Wavelets Overview");
        System.out.println("=======================================");
        
        System.out.println("Gaussian derivative wavelets are excellent for feature detection:");
        System.out.println("- Order 1: Edge detection (first derivative)");
        System.out.println("- Order 2: Ridge/valley detection (Mexican Hat)");
        System.out.println("- Order 3: Inflection point detection");
        System.out.println("- Order 4+: Higher-order feature detection");
        
        System.out.println("\nWavelet properties for different orders:");
        
        for (int order = 1; order <= 4; order++) {
            GaussianDerivativeWavelet wavelet = new GaussianDerivativeWavelet(order);
            
            System.out.printf("  Order %d: %s%n", wavelet.getDerivativeOrder(), wavelet.description());
            System.out.printf("    Name: %s%n", wavelet.name());
            System.out.printf("    Bandwidth: %.3f%n", wavelet.bandwidth());
            System.out.printf("    Order: %d%n", wavelet.getDerivativeOrder());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates edge detection using 1st order Gaussian derivative.
     */
    private static void edgeDetection() {
        System.out.println("2. Edge Detection (1st Order Gaussian Derivative)");
        System.out.println("=================================================");
        
        // Create a signal with edges
        double[] signal = createSignalWithEdges();
        System.out.printf("Created signal with %d samples containing step edges%n", signal.length);
        
        // Use 1st order Gaussian derivative wavelet
        GaussianDerivativeWavelet edgeWavelet = new GaussianDerivativeWavelet(1);
        System.out.printf("Using %s%n", edgeWavelet.description());
        
        CWTTransform cwt = new CWTTransform(edgeWavelet);
        
        // Use small scales for edge detection
        double[] scales = ScaleSpace.linear(0.5, 5.0, 20).getScales();
        
        long startTime = System.nanoTime();
        CWTResult result = cwt.analyze(signal, scales);
        long endTime = System.nanoTime();
        
        System.out.printf("Edge detection completed in %.2f ms%n", (endTime - startTime) / 1e6);
        
        // Find edges
        double[][] coeffs = result.getCoefficients();
        findEdges(coeffs, scales, "step edges");
        
        System.out.println();
    }
    
    /**
     * Demonstrates ridge detection using 2nd order Gaussian derivative (Mexican Hat).
     */
    private static void ridgeDetection() {
        System.out.println("3. Ridge Detection (2nd Order - Mexican Hat)");
        System.out.println("============================================");
        
        // Create a signal with ridges and valleys
        double[] signal = createSignalWithRidges();
        System.out.printf("Created signal with %d samples containing ridges and valleys%n", signal.length);
        
        // Use 2nd order Gaussian derivative wavelet (Mexican Hat)
        GaussianDerivativeWavelet mexicanHat = new GaussianDerivativeWavelet(2);
        System.out.printf("Using %s%n", mexicanHat.description());
        
        CWTTransform cwt = new CWTTransform(mexicanHat);
        
        // Use medium scales for ridge detection
        double[] scales = ScaleSpace.linear(1.0, 10.0, 15).getScales();
        
        CWTResult result = cwt.analyze(signal, scales);
        double[][] coeffs = result.getCoefficients();
        
        // Find ridges (positive coefficients) and valleys (negative coefficients)
        findRidgesAndValleys(coeffs, scales);
        
        System.out.println();
    }
    
    /**
     * Demonstrates higher-order feature detection.
     */
    private static void higherOrderFeatures() {
        System.out.println("4. Higher-Order Feature Detection");
        System.out.println("=================================");
        
        // Create a signal with inflection points and complex features
        double[] signal = createComplexSignal();
        System.out.printf("Created complex signal with %d samples%n", signal.length);
        
        // Compare different orders
        int[] orders = {3, 4, 6, 8};
        
        for (int order : orders) {
            System.out.printf("\nAnalyzing with order %d Gaussian derivative:%n", order);
            
            GaussianDerivativeWavelet wavelet = new GaussianDerivativeWavelet(order);
            CWTTransform cwt = new CWTTransform(wavelet);
            
            double[] scales = ScaleSpace.logarithmic(1.0, 8.0, 12).getScales();
            CWTResult result = cwt.analyze(signal, scales);
            
            // Analyze feature strength
            double[][] coeffs = result.getCoefficients();
            analyzeFeatureStrength(coeffs, order);
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates multi-scale feature analysis.
     */
    private static void multiScaleAnalysis() {
        System.out.println("5. Multi-Scale Feature Analysis");
        System.out.println("===============================");
        
        // Create a signal with features at different scales
        double[] signal = createMultiScaleSignal();
        System.out.printf("Created multi-scale signal with %d samples%n", signal.length);
        
        GaussianDerivativeWavelet wavelet = new GaussianDerivativeWavelet(2); // Mexican Hat
        CWTTransform cwt = new CWTTransform(wavelet);
        
        // Use a wide range of scales
        double[] scales = ScaleSpace.logarithmic(0.5, 32.0, 30).getScales();
        
        CWTResult result = cwt.analyze(signal, scales);
        double[][] coeffs = result.getCoefficients();
        
        System.out.println("Feature analysis across different scales:");
        analyzeMultiScaleFeatures(coeffs, scales);
        
        System.out.println();
    }
    
    /**
     * Demonstrates registry integration for Gaussian derivative wavelets.
     */
    private static void registryIntegration() {
        System.out.println("6. Wavelet Registry Integration");
        System.out.println("==============================");
        
        System.out.println("Gaussian derivative wavelets registered in WaveletRegistry:");
        
        // Check registered wavelets
        String[] gaussianNames = {"gaus1", "gaus2", "gaus3", "gaus4"};
        
        for (String name : gaussianNames) {
            try {
                var wavelet = WaveletRegistry.getWavelet(name);
                if (wavelet != null) {
                    System.out.printf("  %-6s: %s%n", name, 
                        ((GaussianDerivativeWavelet)wavelet).description());
                } else {
                    System.out.printf("  %-6s: Not found%n", name);
                }
            } catch (Exception e) {
                System.out.printf("  %-6s: Error - %s%n", name, e.getMessage());
            }
        }
        
        // Demonstrate creating wavelets directly
        System.out.println("\nCreating Gaussian derivative wavelets directly:");
        
        // Create Mexican Hat wavelet (2nd order Gaussian derivative)
        GaussianDerivativeWavelet mexicanHat = new GaussianDerivativeWavelet(2);
        
        double[] testSignal = createSimpleTestSignal();
        CWTTransform cwt = new CWTTransform(mexicanHat);
        
        double[] scales = {1.0, 2.0, 4.0};
        CWTResult result = cwt.analyze(testSignal, scales);
        
        System.out.printf("  Successfully used '%s' wavelet%n", mexicanHat.name());
        System.out.printf("  Result dimensions: %d scales × %d time points%n", 
            result.getCoefficients().length, result.getCoefficients()[0].length);
        
        System.out.println();
    }
    
    // Signal generation methods
    
    private static double[] createSignalWithEdges() {
        int N = 256;
        double[] signal = new double[N];
        
        // Create step edges at different positions
        for (int i = 0; i < N; i++) {
            // Base level
            signal[i] = 1.0;
            
            // Step up at 1/4
            if (i >= N/4) signal[i] += 2.0;
            
            // Step down at 1/2
            if (i >= N/2) signal[i] -= 3.0;
            
            // Step up at 3/4
            if (i >= 3*N/4) signal[i] += 1.5;
            
            // Add small amount of noise
            signal[i] += 0.1 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createSignalWithRidges() {
        int N = 256;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double x = (double) i / N;
            
            // Gaussian ridge at x = 0.3
            signal[i] += 3.0 * Math.exp(-Math.pow((x - 0.3) / 0.05, 2));
            
            // Inverted Gaussian (valley) at x = 0.7
            signal[i] -= 2.0 * Math.exp(-Math.pow((x - 0.7) / 0.03, 2));
            
            // Broader ridge at x = 0.5
            signal[i] += 1.5 * Math.exp(-Math.pow((x - 0.5) / 0.1, 2));
            
            // Add noise
            signal[i] += 0.2 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createComplexSignal() {
        int N = 512;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double x = (double) i / N;
            
            // Polynomial with inflection points
            signal[i] = 10 * (x - 0.2) * (x - 0.5) * (x - 0.8);
            
            // Add sinusoidal components
            signal[i] += 2.0 * Math.sin(10 * Math.PI * x);
            signal[i] += 1.0 * Math.sin(20 * Math.PI * x);
            
            // Add chirp signal (frequency increasing)
            signal[i] += 0.5 * Math.sin(50 * Math.PI * x * x);
            
            // Add noise
            signal[i] += 0.3 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createMultiScaleSignal() {
        int N = 512;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double x = (double) i / N;
            
            // Fine scale features (small wavelets)
            signal[i] += Math.exp(-Math.pow((x - 0.2) / 0.02, 2));
            signal[i] += Math.exp(-Math.pow((x - 0.8) / 0.03, 2));
            
            // Medium scale features
            signal[i] += 2.0 * Math.exp(-Math.pow((x - 0.4) / 0.08, 2));
            signal[i] += 1.5 * Math.exp(-Math.pow((x - 0.6) / 0.06, 2));
            
            // Large scale features
            signal[i] += 3.0 * Math.exp(-Math.pow((x - 0.5) / 0.2, 2));
            
            // Add noise
            signal[i] += 0.1 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createSimpleTestSignal() {
        int N = 64;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double x = (double) i / N;
            signal[i] = Math.exp(-Math.pow((x - 0.5) / 0.1, 2));
        }
        
        return signal;
    }
    
    // Analysis methods
    
    private static void findEdges(double[][] coeffs, double[] scales, String featureType) {
        System.out.printf("Detecting %s:%n", featureType);
        
        // Use the finest scale for edge detection
        double[] finestScale = coeffs[0];
        
        // Compute threshold
        double threshold = computeAdaptiveThreshold(finestScale, 2.0);
        System.out.printf("  Threshold: %.3f%n", threshold);
        
        // Find peaks (edges)
        int edgeCount = 0;
        for (int t = 1; t < finestScale.length - 1; t++) {
            if (Math.abs(finestScale[t]) > threshold &&
                Math.abs(finestScale[t]) > Math.abs(finestScale[t-1]) &&
                Math.abs(finestScale[t]) > Math.abs(finestScale[t+1])) {
                
                System.out.printf("  Edge at position %d: strength = %.3f%n", 
                    t, finestScale[t]);
                edgeCount++;
            }
        }
        
        System.out.printf("  Total edges detected: %d%n", edgeCount);
    }
    
    private static void findRidgesAndValleys(double[][] coeffs, double[] scales) {
        System.out.println("Detecting ridges and valleys:");
        
        // Use a medium scale
        int scaleIndex = scales.length / 2;
        double[] scaleCoeffs = coeffs[scaleIndex];
        double scale = scales[scaleIndex];
        
        double threshold = computeAdaptiveThreshold(scaleCoeffs, 1.5);
        System.out.printf("  Scale: %.1f, Threshold: %.3f%n", scale, threshold);
        
        int ridgeCount = 0, valleyCount = 0;
        
        for (int t = 1; t < scaleCoeffs.length - 1; t++) {
            double coeff = scaleCoeffs[t];
            
            if (Math.abs(coeff) > threshold &&
                Math.abs(coeff) > Math.abs(scaleCoeffs[t-1]) &&
                Math.abs(coeff) > Math.abs(scaleCoeffs[t+1])) {
                
                if (coeff > 0) {
                    System.out.printf("  Ridge at position %d: strength = %.3f%n", t, coeff);
                    ridgeCount++;
                } else {
                    System.out.printf("  Valley at position %d: strength = %.3f%n", t, coeff);
                    valleyCount++;
                }
            }
        }
        
        System.out.printf("  Total ridges: %d, valleys: %d%n", ridgeCount, valleyCount);
    }
    
    private static void analyzeFeatureStrength(double[][] coeffs, int order) {
        // Compute total energy
        double totalEnergy = 0;
        double maxCoeff = 0;
        
        for (double[] scaleCoeffs : coeffs) {
            for (double coeff : scaleCoeffs) {
                totalEnergy += coeff * coeff;
                maxCoeff = Math.max(maxCoeff, Math.abs(coeff));
            }
        }
        
        System.out.printf("  Total energy: %.2e, Max coefficient: %.3f%n", 
            totalEnergy, maxCoeff);
    }
    
    private static void analyzeMultiScaleFeatures(double[][] coeffs, double[] scales) {
        // Compute energy at each scale
        for (int s = 0; s < scales.length; s += 3) { // Sample every 3rd scale
            double scaleEnergy = 0;
            int featureCount = 0;
            
            double threshold = computeAdaptiveThreshold(coeffs[s], 2.0);
            
            for (int t = 1; t < coeffs[s].length - 1; t++) {
                double coeff = coeffs[s][t];
                scaleEnergy += coeff * coeff;
                
                if (Math.abs(coeff) > threshold &&
                    Math.abs(coeff) > Math.abs(coeffs[s][t-1]) &&
                    Math.abs(coeff) > Math.abs(coeffs[s][t+1])) {
                    featureCount++;
                }
            }
            
            System.out.printf("  Scale %5.1f: Energy = %.2e, Features = %d%n", 
                scales[s], Math.sqrt(scaleEnergy), featureCount);
        }
    }
    
    private static double computeAdaptiveThreshold(double[] values, double factor) {
        // Compute median absolute deviation (MAD) based threshold
        double[] absValues = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            absValues[i] = Math.abs(values[i]);
        }
        
        java.util.Arrays.sort(absValues);
        double median = absValues[absValues.length / 2];
        
        return factor * median;
    }
}