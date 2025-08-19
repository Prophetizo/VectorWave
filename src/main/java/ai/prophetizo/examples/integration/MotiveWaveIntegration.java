package ai.prophetizo.examples.integration;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.*;
import java.util.*;

/**
 * MotiveWave integration example - shows how to use VectorWave in trading platforms.
 * 
 * Run: mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.integration.MotiveWaveIntegration"
 */
public class MotiveWaveIntegration {
    
    public static void main(String[] args) {
        System.out.println("=== MotiveWave Integration Example ===\n");
        
        // Method 1: Using WaveletUIHelper for clean UI integration
        demonstrateUIHelper();
        
        // Method 2: Direct usage with proper separation
        demonstrateDirectUsage();
    }
    
    private static void demonstrateUIHelper() {
        System.out.println("1. Using WaveletUIHelper (Recommended):");
        System.out.println("----------------------------------------");
        
        // Get choices for MODWT-compatible wavelets
        List<WaveletUIHelper.WaveletChoice> choices = 
            WaveletUIHelper.getWaveletChoices(TransformType.MODWT);
        
        // Display first few choices
        System.out.println("Available wavelets for dropdown:");
        choices.stream().limit(5).forEach(choice -> 
            System.out.println("  " + choice.getDisplayText()));
        System.out.println("  ... and " + (choices.size() - 5) + " more\n");
        
        // User selects from dropdown (simulated)
        WaveletUIHelper.WaveletChoice selected = choices.get(2); // DB4
        System.out.println("User selected: " + selected.getDisplayText());
        
        // Use the selection
        Wavelet wavelet = WaveletRegistry.getWavelet(selected.getValue());
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        double[] prices = {100, 102, 101, 103, 105, 104, 106, 108};
        MODWTResult result = transform.forward(prices);
        System.out.println("Transform complete!\n");
    }
    
    private static void demonstrateDirectUsage() {
        System.out.println("2. Direct Usage with List (Type-Safe):");
        System.out.println("--------------------------------------");
        
        // Get available wavelets as List (not array)
        List<WaveletName> wavelets = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Available wavelets: " + wavelets.size() + " total");
        
        // User selects DB4 from dropdown
        WaveletName selected = WaveletName.DB4;
        
        // Validate and use
        if (WaveletRegistry.hasWavelet(selected)) {
            Wavelet wavelet = WaveletRegistry.getWavelet(selected);
            
            // Create transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Use it on price data
            double[] prices = {100, 102, 101, 103, 105, 104, 106, 108};
            MODWTResult result = transform.forward(prices);
            
            System.out.println("Transform complete!");
        }
    }
    
    /**
     * Example of what the MotiveWave study would look like.
     * Demonstrates best practices for UI integration.
     */
    public static class MotiveWaveStudy {
        
        /**
         * Get available wavelets as a List for better type safety.
         * @return list of available wavelet names
         */
        public List<WaveletName> getAvailableWavelets() {
            // Return as List - more flexible than array
            return WaveletRegistry.getOrthogonalWavelets();
        }
        
        /**
         * Get display strings for UI dropdown.
         * Separates the enum from its display representation.
         * @return array of display strings for UI
         */
        public String[] getWaveletDisplayNames() {
            return WaveletRegistry.getOrthogonalWavelets().stream()
                .map(name -> String.format("%s - %s", 
                    name.getCode().toUpperCase(), 
                    name.getDescription()))
                .toArray(String[]::new);
        }
        
        /**
         * Get a mapping of display names to enum values.
         * Useful for UI dropdowns that need both display and value.
         * @return map of display string to wavelet enum
         */
        public Map<String, WaveletName> getWaveletChoicesMap() {
            Map<String, WaveletName> choices = new LinkedHashMap<>();
            for (WaveletName name : WaveletRegistry.getOrthogonalWavelets()) {
                String displayName = String.format("%s - %s", 
                    name.getCode().toUpperCase(), 
                    name.getDescription());
                choices.put(displayName, name);
            }
            return choices;
        }
        
        /**
         * Calculate using the selected wavelet.
         * @param waveletName the selected wavelet enum
         * @param data the input data
         */
        public void calculate(WaveletName waveletName, double[] data) {
            // Validate input
            if (waveletName == null) {
                throw new IllegalArgumentException("Wavelet selection is required");
            }
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Input data is required");
            }
            
            // Get selected wavelet
            Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
            
            // Do transform
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(data);
            
            // Use results...
        }
        
        /**
         * Alternative: Calculate using display name from UI.
         * @param displayName the selected display string from UI
         * @param data the input data
         */
        public void calculateFromDisplayName(String displayName, double[] data) {
            // Parse wavelet from display name
            Map<String, WaveletName> choices = getWaveletChoicesMap();
            WaveletName waveletName = choices.get(displayName);
            
            if (waveletName == null) {
                throw new IllegalArgumentException("Invalid wavelet selection: " + displayName);
            }
            
            calculate(waveletName, data);
        }
        
        /**
         * Best practice: Use WaveletUIHelper for cleaner code.
         * @return list of choices for UI dropdown
         */
        public List<WaveletUIHelper.WaveletChoice> getWaveletChoicesV2() {
            // Get only MODWT-compatible wavelets for this study
            return WaveletUIHelper.getWaveletChoices(TransformType.MODWT);
        }
        
        /**
         * Calculate using WaveletChoice from UI.
         * @param choice the selected choice from dropdown
         * @param data the input data
         */
        public void calculateFromChoice(WaveletUIHelper.WaveletChoice choice, double[] data) {
            if (choice == null) {
                throw new IllegalArgumentException("No wavelet selected");
            }
            calculate(choice.getValue(), data);
        }
    }
}