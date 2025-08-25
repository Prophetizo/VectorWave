package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.*;
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
        
        // Demo 5: Demonstrate enum benefits
        enumBenefitsDemo();
        
        // Demo 6: Show how to add custom wavelets
        customWaveletExample();
    }
    
    private static void showAvailableWavelets() {
        System.out.println("1. All Available Wavelets");
        System.out.println("-------------------------");
        
        Set<WaveletName> wavelets = WaveletRegistry.getAvailableWavelets();
        System.out.println("Total wavelets discovered: " + wavelets.size());
        System.out.println();
        
        System.out.println("Wavelets grouped by family:");
        
        // Group by family
        System.out.println("\nHaar family:");
        wavelets.stream()
            .filter(w -> w.name().contains("HAAR"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println("\nDaubechies family:");
        wavelets.stream()
            .filter(w -> w.name().startsWith("DB"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println("\nSymlet family:");
        wavelets.stream()
            .filter(w -> w.name().startsWith("SYM"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println("\nCoiflet family:");
        wavelets.stream()
            .filter(w -> w.name().startsWith("COIF"))
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println("\nContinuous wavelets:");
        wavelets.stream()
            .filter(w -> w.getType() == WaveletType.CONTINUOUS)
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println("\nComplex wavelets:");
        wavelets.stream()
            .filter(w -> w.getType() == WaveletType.COMPLEX)
            .sorted()
            .forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println();
    }
    
    private static void showWaveletsByType() {
        System.out.println("2. Wavelets by Type");
        System.out.println("-------------------");
        
        List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Orthogonal wavelets (" + orthogonal.size() + "):");
        orthogonal.stream().limit(5).forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        if (orthogonal.size() > 5) {
            System.out.println("  ... and " + (orthogonal.size() - 5) + " more");
        }
        
        List<WaveletName> biorthogonal = WaveletRegistry.getBiorthogonalWavelets();
        System.out.println("\nBiorthogonal wavelets (" + biorthogonal.size() + "):");
        biorthogonal.stream().limit(5).forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        if (biorthogonal.size() > 5) {
            System.out.println("  ... and " + (biorthogonal.size() - 5) + " more");
        }
        
        List<WaveletName> continuous = WaveletRegistry.getContinuousWavelets();
        System.out.println("\nContinuous wavelets (" + continuous.size() + "):");
        continuous.forEach(w -> System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        System.out.println();
    }
    
    private static void waveletLookupDemo() {
        System.out.println("3. Wavelet Lookup");
        System.out.println("-----------------");
        
        // Lookup specific wavelets using enum
        WaveletName[] testWavelets = {WaveletName.HAAR, WaveletName.DB4, WaveletName.SYM3, WaveletName.COIF2, WaveletName.MORLET};
        
        for (WaveletName name : testWavelets) {
            if (WaveletRegistry.hasWavelet(name)) {
                Wavelet wavelet = WaveletRegistry.getWavelet(name);
                System.out.printf("Found %s: %s (type: %s)\n", 
                    name, 
                    wavelet.description(),
                    wavelet.getType());
            } else {
                System.out.printf("Wavelet %s not found\n", name);
            }
        }
        
        // Demonstrate type-safe access - no runtime errors for typos!
        System.out.println("\nType-safe access benefits:");
        System.out.println("  - Compile-time checking prevents typos");
        System.out.println("  - IDE autocomplete shows all options");
        System.out.println("  - Refactoring is safe and automatic");
        
        System.out.println();
    }
    
    private static void showProviderInfo() {
        System.out.println("4. Provider Information");
        System.out.println("-----------------------");
        
        // Wavelets are loaded automatically via ServiceLoader
        System.out.println("All wavelet providers loaded successfully!");
        
        // Show how wavelets are discovered
        System.out.println("\nWavelets are discovered via ServiceLoader from:");
        System.out.println("  META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider");
        
        System.out.println("\nCurrent providers:");
        System.out.println("  - OrthogonalWaveletProvider (Haar, Daubechies, Symlets, Coiflets)");
        System.out.println("  - BiorthogonalWaveletProvider (Biorthogonal Splines)");
        System.out.println("  - ContinuousWaveletProvider (Morlet, Paul, DOG, Shannon, etc.)");
        
        System.out.println();
    }
    
    private static void enumBenefitsDemo() {
        System.out.println("5. Enum-Based Access Benefits");
        System.out.println("------------------------------");
        
        System.out.println("No more case sensitivity issues:");
        System.out.println("  - WaveletName.HAAR is always HAAR");
        System.out.println("  - No confusion between 'haar', 'HAAR', 'Haar'");
        
        System.out.println("\nNo more string typos:");
        System.out.println("  - Compile-time checking prevents errors");
        System.out.println("  - IDE autocomplete shows all available wavelets");
        
        System.out.println("\nConsistent naming:");
        System.out.println("  - Each wavelet has ONE canonical representation");
        System.out.println("  - No duplicates like 'db4' vs 'daubechies4'");
        
        System.out.println("\nDirect access examples:");
        Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        System.out.printf("  HAAR -> %s\n", haar.description());
        System.out.printf("  DB4 -> %s\n", db4.description());
        
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
        
        // Wavelets are loaded once at startup
        System.out.println("\nTotal wavelets available: " + 
            WaveletRegistry.getAvailableWavelets().size());
    }
}