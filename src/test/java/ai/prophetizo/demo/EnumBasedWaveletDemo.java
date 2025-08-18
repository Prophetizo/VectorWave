package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletName;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.WaveletType;

import java.util.List;
import java.util.Set;

/**
 * Demonstrates the new enum-based WaveletRegistry API.
 * This approach provides type safety, IDE support, and eliminates string typos.
 */
public class EnumBasedWaveletDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Enum-Based Wavelet Registry Demo ===\n");
        
        // Type-safe wavelet access - no string typos possible!
        demonstrateTypeSafeAccess();
        
        // Easy filtering by wavelet family
        demonstrateFamilyFiltering();
        
        // No duplicates with enum approach
        demonstrateNoDuplicates();
        
        // Conversion from legacy string codes
        demonstrateStringConversion();
        
        // IDE benefits
        demonstrateIDEBenefits();
    }
    
    private static void demonstrateTypeSafeAccess() {
        System.out.println("1. Type-Safe Wavelet Access:");
        System.out.println("-----------------------------");
        
        // Direct enum access - compile-time checked
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        System.out.println("  DB4: " + db4.description());
        
        Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);
        System.out.println("  HAAR: " + haar.description());
        
        Wavelet morlet = WaveletRegistry.getWavelet(WaveletName.MORLET);
        System.out.println("  MORLET: " + morlet.description());
        
        System.out.println();
    }
    
    private static void demonstrateFamilyFiltering() {
        System.out.println("2. Wavelet Family Filtering:");
        System.out.println("-----------------------------");
        
        List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
        System.out.println("  Daubechies wavelets: " + daubechies);
        
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        System.out.println("  Symlet wavelets: " + symlets.size() + " available");
        
        List<WaveletName> coiflets = WaveletRegistry.getCoifletWavelets();
        System.out.println("  Coiflet wavelets: " + coiflets);
        
        System.out.println();
    }
    
    private static void demonstrateNoDuplicates() {
        System.out.println("3. No Duplicates with Enum:");
        System.out.println("-----------------------------");
        
        List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("  Orthogonal wavelets count: " + orthogonal.size());
        System.out.println("  Each wavelet appears exactly once");
        System.out.println("  No 'db4' vs 'daubechies4' confusion!");
        
        System.out.println();
    }
    
    private static void demonstrateStringConversion() {
        System.out.println("4. String Code Conversion:");
        System.out.println("-----------------------------");
        
        // Convert legacy string codes to enum
        String[] legacyCodes = {"db4", "haar", "sym2", "coif1", "morl"};
        
        for (String code : legacyCodes) {
            try {
                WaveletName name = WaveletName.fromCode(code);
                System.out.println("  '" + code + "' -> " + name);
            } catch (IllegalArgumentException e) {
                System.out.println("  '" + code + "' -> Not found");
            }
        }
        
        // Also handles aliases
        System.out.println("\n  Aliases also work:");
        System.out.println("  'daubechies4' -> " + WaveletName.fromCode("daubechies4"));
        System.out.println("  'morlet' -> " + WaveletName.fromCode("morlet"));
        
        System.out.println();
    }
    
    private static void demonstrateIDEBenefits() {
        System.out.println("5. IDE Benefits:");
        System.out.println("-----------------------------");
        System.out.println("  With enum-based approach:");
        System.out.println("  ✓ Autocomplete shows all available wavelets");
        System.out.println("  ✓ Javadoc appears for each wavelet");
        System.out.println("  ✓ Refactoring is safe and automatic");
        System.out.println("  ✓ Find usages works perfectly");
        System.out.println("  ✓ No runtime errors from typos");
        
        System.out.println("\n  Example - switch on wavelet type:");
        WaveletName selectedWavelet = WaveletName.DB4;
        
        switch (selectedWavelet) {
            case HAAR:
                System.out.println("    Using Haar wavelet");
                break;
            case DB2:
            case DB4:
            case DB6:
            case DB8:
            case DB10:
                System.out.println("    Using Daubechies family");
                break;
            case SYM2:
            case SYM3:
            case SYM4:
                System.out.println("    Using Symlet family");
                break;
            default:
                System.out.println("    Using other wavelet");
        }
        
        System.out.println();
    }
}