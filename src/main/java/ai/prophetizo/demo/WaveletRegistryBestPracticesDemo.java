package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;

/**
 * Best practices for using the WaveletRegistry.
 * 
 * This demo shows:
 * - How to discover available wavelets
 * - How to select wavelets for dropdown menus
 * - How to safely get wavelets with error handling
 * - How to use wavelets in transforms
 * 
 * Run with: mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.WaveletRegistryBestPracticesDemo"
 */
public class WaveletRegistryBestPracticesDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("            WaveletRegistry Best Practices Demo");
        System.out.println("=".repeat(70));
        
        WaveletRegistryBestPracticesDemo demo = new WaveletRegistryBestPracticesDemo();
        demo.demonstrateDiscovery();
        demo.demonstrateSelection();
        demo.demonstrateErrorHandling();
        demo.demonstrateTransformUsage();
        demo.demonstrateMotiveWaveIntegration();
    }
    
    /**
     * 1. DISCOVERING AVAILABLE WAVELETS
     */
    private void demonstrateDiscovery() {
        System.out.println("\n1. DISCOVERING AVAILABLE WAVELETS");
        System.out.println("-".repeat(40));
        
        // Get all available wavelets
        Set<String> allWavelets = WaveletRegistry.getAvailableWavelets();
        System.out.println("Total wavelets available: " + allWavelets.size());
        System.out.println("All wavelets: " + allWavelets);
        
        // Get wavelets by type
        List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("\nOrthogonal wavelets (" + orthogonal.size() + "): " + orthogonal);
        
        List<String> continuous = WaveletRegistry.getContinuousWavelets();
        System.out.println("Continuous wavelets (" + continuous.size() + "): " + continuous);
        
        // Check specific wavelet availability
        System.out.println("\nChecking specific wavelets:");
        String[] checkList = {"haar", "db4", "sym2", "nonexistent"};
        for (String name : checkList) {
            boolean available = WaveletRegistry.hasWavelet(name);
            System.out.println("  " + name + ": " + (available ? "✓ Available" : "✗ Not available"));
        }
    }
    
    /**
     * 2. SELECTING WAVELETS FOR UI
     */
    private void demonstrateSelection() {
        System.out.println("\n2. SELECTING WAVELETS FOR UI");
        System.out.println("-".repeat(40));
        
        // For a dropdown menu (e.g., in MotiveWave)
        List<String> dropdownOptions = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Dropdown options for discrete transforms:");
        for (String option : dropdownOptions) {
            System.out.println("  - " + option);
        }
        
        // Default selection
        String defaultWavelet = "db4";
        if (!WaveletRegistry.hasWavelet(defaultWavelet)) {
            defaultWavelet = dropdownOptions.get(0); // Fallback to first available
        }
        System.out.println("\nDefault selection: " + defaultWavelet);
    }
    
    /**
     * 3. ERROR HANDLING
     */
    private void demonstrateErrorHandling() {
        System.out.println("\n3. ERROR HANDLING");
        System.out.println("-".repeat(40));
        
        // Safe wavelet retrieval
        String[] testNames = {"db4", "HAAR", "invalid", null, ""};
        
        for (String name : testNames) {
            try {
                Wavelet w = WaveletRegistry.getWavelet(name);
                System.out.println("✓ Got wavelet '" + name + "': " + w.name());
            } catch (InvalidArgumentException e) {
                System.out.println("✗ Failed to get '" + name + "': " + e.getMessage());
            } catch (Exception e) {
                System.out.println("✗ Unexpected error for '" + name + "': " + e.getMessage());
            }
        }
        
        // Best practice: Always check before getting
        System.out.println("\nBest practice - check first:");
        String userInput = "sym2";
        if (WaveletRegistry.hasWavelet(userInput)) {
            Wavelet w = WaveletRegistry.getWavelet(userInput);
            System.out.println("Successfully got: " + w.name() + " - " + w.description());
        } else {
            System.out.println("Wavelet not available: " + userInput);
        }
    }
    
    /**
     * 4. USING WAVELETS IN TRANSFORMS
     */
    private void demonstrateTransformUsage() {
        System.out.println("\n4. USING WAVELETS IN TRANSFORMS");
        System.out.println("-".repeat(40));
        
        // Get a wavelet
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        
        // Create transform
        MODWTTransform transform = new MODWTTransform(db4, BoundaryMode.PERIODIC);
        
        // Sample data
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        
        // Perform transform
        MODWTResult result = transform.forward(signal);
        System.out.println("Transform complete with " + db4.name());
        System.out.println("  Signal length: " + signal.length);
        System.out.println("  Max level: " + result.getMaxLevel());
        
        // Get coefficients
        double[] approx = result.getApproximation(1);
        double[] detail = result.getDetail(1);
        System.out.println("  Level 1 coefficients: " + approx.length + " samples");
        
        // Reconstruct
        double[] reconstructed = transform.inverse(result);
        double error = 0;
        for (int i = 0; i < signal.length; i++) {
            error += Math.abs(signal[i] - reconstructed[i]);
        }
        System.out.println("  Reconstruction error: " + String.format("%.2e", error / signal.length));
    }
    
    /**
     * 5. MOTIVEWAVE INTEGRATION EXAMPLE
     */
    private void demonstrateMotiveWaveIntegration() {
        System.out.println("\n5. MOTIVEWAVE INTEGRATION EXAMPLE");
        System.out.println("-".repeat(40));
        
        // Simulate MotiveWave study settings
        class StudySettings {
            String waveletName = "db4";
            int decompositionLevel = 3;
            double threshold = 0.1;
        }
        
        StudySettings settings = new StudySettings();
        
        // Simulate price data
        double[] prices = new double[256];
        Random rand = new Random(42);
        prices[0] = 100;
        for (int i = 1; i < prices.length; i++) {
            prices[i] = prices[i-1] + rand.nextGaussian() * 0.5;
        }
        
        // Process with wavelet
        try {
            // Get wavelet from settings
            if (!WaveletRegistry.hasWavelet(settings.waveletName)) {
                System.out.println("Invalid wavelet, using default");
                settings.waveletName = "haar";
            }
            
            Wavelet wavelet = WaveletRegistry.getWavelet(settings.waveletName);
            System.out.println("Using wavelet: " + wavelet.name());
            
            // Create transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Process price data
            MODWTResult result = transform.forward(prices);
            
            // Extract trend (approximation at higher level)
            double[] trend = result.getApproximation(settings.decompositionLevel);
            System.out.println("Extracted trend at level " + settings.decompositionLevel);
            
            // Extract noise (detail at level 1)
            double[] noise = result.getDetail(1);
            
            // Calculate statistics
            double trendMean = Arrays.stream(trend).average().orElse(0);
            double noiseStd = Math.sqrt(Arrays.stream(noise)
                .map(x -> x * x)
                .average().orElse(0));
            
            System.out.println("  Trend mean: " + String.format("%.2f", trendMean));
            System.out.println("  Noise std: " + String.format("%.4f", noiseStd));
            
            // Denoise by thresholding
            int thresholdedCount = 0;
            for (int level = 1; level <= settings.decompositionLevel; level++) {
                double[] details = result.getDetail(level);
                for (int i = 0; i < details.length; i++) {
                    if (Math.abs(details[i]) < settings.threshold) {
                        details[i] = 0;
                        thresholdedCount++;
                    }
                }
            }
            System.out.println("  Thresholded " + thresholdedCount + " coefficients");
            
            // Reconstruct denoised signal
            double[] denoised = transform.inverse(result);
            System.out.println("  Denoised signal reconstructed");
            
        } catch (Exception e) {
            System.err.println("Error in processing: " + e.getMessage());
        }
    }
}