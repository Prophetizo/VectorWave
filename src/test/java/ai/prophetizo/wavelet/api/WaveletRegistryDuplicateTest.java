package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that WaveletRegistry returns canonical names without duplicates.
 */
class WaveletRegistryDuplicateTest {
    
    @Test
    @DisplayName("getOrthogonalWavelets should not return duplicate wavelets")
    void testNoDuplicatesInOrthogonalWavelets() {
        List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Group wavelets by their actual wavelet object to detect duplicates
        Map<Wavelet, List<String>> waveletToNames = new HashMap<>();
        for (String name : orthogonalWavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            waveletToNames.computeIfAbsent(wavelet, k -> new ArrayList<>()).add(name);
        }
        
        // Check for duplicates
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<Wavelet, List<String>> entry : waveletToNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(String.format("Wavelet %s has duplicate names: %s", 
                    entry.getKey().name(), entry.getValue()));
            }
        }
        
        assertTrue(duplicates.isEmpty(), 
            "Found duplicate wavelets:\n" + String.join("\n", duplicates));
    }
    
    @Test
    @DisplayName("getOrthogonalWavelets should use canonical names")
    void testCanonicalNamesInOrthogonalWavelets() {
        List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Check that we use canonical short names, not long aliases
        for (String name : orthogonalWavelets) {
            if (name.startsWith("daubechies") && name.length() > 10) {
                String number = name.substring(10);
                String canonical = "db" + number;
                assertFalse(orthogonalWavelets.contains(canonical),
                    String.format("Both '%s' and canonical '%s' are present", name, canonical));
            }
        }
    }
    
    @Test
    @DisplayName("Alias lookup should still work for backward compatibility")
    void testAliasLookupStillWorks() {
        // Ensure aliases still work for getWavelet()
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        Wavelet daubechies4 = WaveletRegistry.getWavelet("daubechies4");
        assertEquals(db4, daubechies4, "Alias 'daubechies4' should map to 'db4'");
        
        Wavelet db2 = WaveletRegistry.getWavelet("db2");
        Wavelet daubechies2 = WaveletRegistry.getWavelet("daubechies2");
        assertEquals(db2, daubechies2, "Alias 'daubechies2' should map to 'db2'");
    }
    
    @Test
    @DisplayName("Print duplicate analysis for debugging")
    void analyzeDuplicates() {
        List<String> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        System.out.println("\n=== Duplicate Analysis ===");
        System.out.println("Total wavelets returned: " + orthogonalWavelets.size());
        
        // Group by wavelet object
        Map<Wavelet, List<String>> waveletToNames = new HashMap<>();
        for (String name : orthogonalWavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            waveletToNames.computeIfAbsent(wavelet, k -> new ArrayList<>()).add(name);
        }
        
        System.out.println("Unique wavelet objects: " + waveletToNames.size());
        
        // Show duplicates
        System.out.println("\nDuplicates found:");
        for (Map.Entry<Wavelet, List<String>> entry : waveletToNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("  " + entry.getKey().name() + " -> " + entry.getValue());
            }
        }
        
        // Show all names
        System.out.println("\nAll names returned:");
        Collections.sort(orthogonalWavelets);
        for (String name : orthogonalWavelets) {
            System.out.println("  " + name);
        }
    }
}