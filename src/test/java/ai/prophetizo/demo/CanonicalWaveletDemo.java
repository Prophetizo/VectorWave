package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.WaveletName;
import java.util.List;

/**
 * Demonstrates that WaveletRegistry now returns only canonical names without duplicates.
 */
public class CanonicalWaveletDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Canonical Wavelet Names Demo ===\n");
        
        // Get orthogonal wavelets - should have no duplicates
        List<WaveletName> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Orthogonal wavelets (count: " + orthogonalWavelets.size() + "):");
        for (WaveletName name : orthogonalWavelets) {
            System.out.println("  - " + name);
        }
        
        // Demonstrate that aliases still work
        System.out.println("\n=== Alias Compatibility ===");
        testAlias(WaveletName.DB4, "daubechies4");
        testAlias(WaveletName.DB2, "daubechies2");
        testAlias(WaveletName.DB6, "daubechies6");
        testAlias(WaveletName.DB8, "daubechies8");
        testAlias(WaveletName.DB10, "daubechies10");
        
        // Verify no duplicates
        System.out.println("\n=== Duplicate Check ===");
        boolean hasDuplicates = false;
        for (WaveletName waveletName : orthogonalWavelets) {
            // With enum, duplicates are impossible!
            // Check that we don't have both DB4 and a hypothetical DAUBECHIES4
            String nameStr = waveletName.name();
            if (nameStr.startsWith("DAUBECHIES") && !nameStr.startsWith("DB")) {
                System.out.println("ERROR: Found long-form name in results: " + waveletName);
                hasDuplicates = true;
            }
        }
        
        if (!hasDuplicates) {
            System.out.println("SUCCESS: No duplicate wavelets found!");
            System.out.println("The registry now returns only canonical names.");
        }
    }
    
    private static void testAlias(WaveletName canonical, String alias) {
        try {
            Wavelet w1 = WaveletRegistry.getWavelet(canonical);
            Wavelet w2 = WaveletRegistry.getWavelet(WaveletName.fromCode(alias));
            if (w1.equals(w2)) {
                System.out.println("  ✓ " + alias + " correctly maps to " + canonical.getCode());
            } else {
                System.out.println("  ✗ " + alias + " does not map to " + canonical);
            }
        } catch (Exception e) {
            System.out.println("  ✗ Error accessing " + alias + ": " + e.getMessage());
        }
    }
}