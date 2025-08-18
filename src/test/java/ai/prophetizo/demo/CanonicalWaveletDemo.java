package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;

import java.util.List;

/**
 * Demonstrates that WaveletRegistry now returns only canonical names without duplicates.
 */
public class CanonicalWaveletDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Canonical Wavelet Names Demo ===\n");
        
        // Get orthogonal wavelets - should have no duplicates
        List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Orthogonal wavelets (count: " + orthogonalWavelets.size() + "):");
        for (String name : orthogonalWavelets) {
            System.out.println("  - " + name);
        }
        
        // Demonstrate that aliases still work
        System.out.println("\n=== Alias Compatibility ===");
        testAlias("db4", "daubechies4");
        testAlias("db2", "daubechies2");
        testAlias("db6", "daubechies6");
        testAlias("db8", "daubechies8");
        testAlias("db10", "daubechies10");
        
        // Verify no duplicates
        System.out.println("\n=== Duplicate Check ===");
        boolean hasDuplicates = false;
        for (String waveletName : orthogonalWavelets) {
            if (waveletName.startsWith("daubechies") && waveletName.length() > 10) {
                System.out.println("ERROR: Found long-form name in results: " + waveletName);
                hasDuplicates = true;
            }
        }
        
        if (!hasDuplicates) {
            System.out.println("SUCCESS: No duplicate wavelets found!");
            System.out.println("The registry now returns only canonical names.");
        }
    }
    
    private static void testAlias(String canonical, String alias) {
        try {
            Wavelet w1 = WaveletRegistry.getWavelet(canonical);
            Wavelet w2 = WaveletRegistry.getWavelet(alias);
            if (w1.equals(w2)) {
                System.out.println("  ✓ " + alias + " correctly maps to " + canonical);
            } else {
                System.out.println("  ✗ " + alias + " does not map to " + canonical);
            }
        } catch (Exception e) {
            System.out.println("  ✗ Error accessing " + alias + ": " + e.getMessage());
        }
    }
}