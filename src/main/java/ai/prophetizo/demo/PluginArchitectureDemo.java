package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;

import java.util.List;
import java.util.Set;

/**
 * Demonstrates the ServiceLoader-based plugin architecture for wavelet discovery.
 * Shows how wavelets are automatically discovered and registered.
 */
public class PluginArchitectureDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave Plugin Architecture Demo");
        System.out.println("===================================\n");
        
        // Demo 1: Show all available wavelets
        showAvailableWavelets();
        
        // Demo 2: Show wavelets by type
        showWaveletsByType();
        
        // Demo 3: Lookup wavelets by name
        waveletLookupDemo();
        
        // Demo 4: Show provider information
        showProviderInfo();
        
        // Demo 5: Demonstrate case-insensitive lookup
        caseInsensitiveLookupDemo();
        
        // Demo 6: Show how to add custom wavelets
        customWaveletExample();
    }
    
    private static void showAvailableWavelets() {
        System.out.println("1. All Available Wavelets");
        System.out.println("-------------------------");
        
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        System.out.println("Total wavelets discovered: " + wavelets.size());
        System.out.println();
        
        System.out.println("Wavelets grouped by family:");
        
        // Group by prefix
        System.out.println("\nHaar family:");
        wavelets.stream()
            .filter(w -> w.toLowerCase().contains("haar"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println("\nDaubechies family:");
        wavelets.stream()
            .filter(w -> w.startsWith("DB") || w.toLowerCase().contains("daubechies"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println("\nSymlet family:");
        wavelets.stream()
            .filter(w -> w.startsWith("SYM") || w.toLowerCase().contains("symlet"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println("\nCoiflet family:");
        wavelets.stream()
            .filter(w -> w.startsWith("COIF") || w.toLowerCase().contains("coiflet"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println("\nBiorthogonal family:");
        wavelets.stream()
            .filter(w -> w.startsWith("BIOR") || w.toLowerCase().contains("biorthogonal"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println("\nContinuous wavelets:");
        wavelets.stream()
            .filter(w -> w.toLowerCase().contains("morlet") || 
                        w.toLowerCase().contains("paul") || 
                        w.toLowerCase().contains("dog") ||
                        w.toLowerCase().contains("shannon") ||
                        w.toLowerCase().contains("gaussian"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w));
        
        System.out.println();
    }
    
    private static void showWaveletsByType() {
        System.out.println("2. Wavelets by Type");
        System.out.println("-------------------");
        
        List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Orthogonal wavelets (" + orthogonal.size() + "):");
        orthogonal.stream().limit(5).forEach(w -> System.out.println("  - " + w));
        System.out.println("  ... and " + (orthogonal.size() - 5) + " more");
        
        List<String> biorthogonal = WaveletRegistry.getBiorthogonalWavelets();
        System.out.println("\nBiorthogonal wavelets (" + biorthogonal.size() + "):");
        biorthogonal.stream().limit(5).forEach(w -> System.out.println("  - " + w));
        if (biorthogonal.size() > 5) {
            System.out.println("  ... and " + (biorthogonal.size() - 5) + " more");
        }
        
        List<String> continuous = WaveletRegistry.getContinuousWavelets();
        System.out.println("\nContinuous wavelets (" + continuous.size() + "):");
        continuous.forEach(w -> System.out.println("  - " + w));
        
        System.out.println();
    }
    
    private static void waveletLookupDemo() {
        System.out.println("3. Wavelet Lookup");
        System.out.println("-----------------");
        
        // Lookup specific wavelets
        String[] testNames = {"Haar", "DB4", "SYM3", "COIF2", "Morlet"};
        
        for (String name : testNames) {
            if (WaveletRegistry.hasWavelet(name)) {
                Wavelet wavelet = WaveletRegistry.getWavelet(name);
                System.out.printf("Found '%s': %s (type: %s)\n", 
                    name, 
                    wavelet.name(),
                    wavelet.getType());
            } else {
                System.out.printf("Wavelet '%s' not found\n", name);
            }
        }
        
        // Try non-existent wavelet
        System.out.println("\nTrying non-existent wavelet:");
        try {
            WaveletRegistry.getWavelet("NonExistent");
        } catch (IllegalArgumentException e) {
            System.out.println("  Exception: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void showProviderInfo() {
        System.out.println("4. Provider Information");
        System.out.println("-----------------------");
        
        // Check for any load warnings
        List<String> warnings = WaveletRegistry.getLoadWarnings();
        if (warnings.isEmpty()) {
            System.out.println("All wavelet providers loaded successfully!");
        } else {
            System.out.println("Load warnings:");
            warnings.forEach(w -> System.out.println("  - " + w));
        }
        
        // Show how wavelets are discovered
        System.out.println("\nWavelets are discovered via ServiceLoader from:");
        System.out.println("  META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider");
        
        System.out.println("\nCurrent providers:");
        System.out.println("  - OrthogonalWaveletProvider (Haar, Daubechies, Symlets, Coiflets)");
        System.out.println("  - BiorthogonalWaveletProvider (Biorthogonal Splines)");
        System.out.println("  - ContinuousWaveletProvider (Morlet, Paul, DOG, Shannon, etc.)");
        
        System.out.println();
    }
    
    private static void caseInsensitiveLookupDemo() {
        System.out.println("5. Case-Insensitive Lookup");
        System.out.println("--------------------------");
        
        String[] variations = {"haar", "HAAR", "Haar", "HaAr"};
        
        System.out.println("All these variations return the same wavelet:");
        for (String variation : variations) {
            Wavelet wavelet = WaveletRegistry.getWavelet(variation);
            System.out.printf("  '%s' -> %s\n", variation, wavelet.name());
        }
        
        System.out.println("\nAlso works for complex names:");
        String[] complexNames = {"daubechies-4", "DAUBECHIES-4", "Daubechies-4", "db4", "DB4"};
        for (String name : complexNames) {
            if (WaveletRegistry.hasWavelet(name)) {
                Wavelet wavelet = WaveletRegistry.getWavelet(name);
                System.out.printf("  '%s' -> %s\n", name, wavelet.name());
            }
        }
        
        System.out.println();
    }
    
    private static void customWaveletExample() {
        System.out.println("6. Adding Custom Wavelets");
        System.out.println("-------------------------");
        
        System.out.println("To add custom wavelets to VectorWave:\n");
        
        System.out.println("1. Implement the Wavelet interface:");
        System.out.println("   ```java");
        System.out.println("   public class MyCustomWavelet implements DiscreteWavelet {");
        System.out.println("       @Override");
        System.out.println("       public String getName() { return \"MyCustom\"; }");
        System.out.println("       // ... implement other methods");
        System.out.println("   }");
        System.out.println("   ```\n");
        
        System.out.println("2. Create a WaveletProvider:");
        System.out.println("   ```java");
        System.out.println("   public class MyWaveletProvider implements WaveletProvider {");
        System.out.println("       @Override");
        System.out.println("       public List<Wavelet> getWavelets() {");
        System.out.println("           return List.of(new MyCustomWavelet());");
        System.out.println("       }");
        System.out.println("       @Override");
        System.out.println("       public String getName() { return \"My Custom Provider\"; }");
        System.out.println("       @Override");
        System.out.println("       public String getVersion() { return \"1.0\"; }");
        System.out.println("   }");
        System.out.println("   ```\n");
        
        System.out.println("3. Register in META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider:");
        System.out.println("   ```");
        System.out.println("   com.mycompany.MyWaveletProvider");
        System.out.println("   ```\n");
        
        System.out.println("4. Your wavelet will be automatically discovered!");
        System.out.println("   ```java");
        System.out.println("   Wavelet custom = WaveletRegistry.getWavelet(\"MyCustom\");");
        System.out.println("   ```\n");
        
        System.out.println("Benefits of plugin architecture:");
        System.out.println("  - Zero configuration required");
        System.out.println("  - Automatic discovery at runtime");
        System.out.println("  - Easy to extend without modifying core");
        System.out.println("  - Works with Java modules and classpath");
        
        // Force reload demonstration
        System.out.println("\nReloading wavelet registry...");
        WaveletRegistry.reload();
        System.out.println("Registry reloaded. Total wavelets: " + 
            WaveletRegistry.getAvailableWavelets().size());
    }
}