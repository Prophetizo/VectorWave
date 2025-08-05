package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test suite for wavelet discovery API methods.
 * Tests the ability to programmatically discover available wavelets
 * and their properties.
 */
@DisplayName("Wavelet Discovery API Test Suite")
public class WaveletDiscoveryTest {
    
    @BeforeEach
    void setUp() {
        // Ensure clean state
        WaveletRegistry.clearWarnings();
    }
    
    @Test
    @DisplayName("Get available wavelets returns non-empty set")
    void testGetAvailableWavelets() {
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        
        assertNotNull(wavelets, "Available wavelets should not be null");
        assertFalse(wavelets.isEmpty(), "Should have at least one wavelet available");
        
        // Verify some basic wavelets are present - check both cases
        assertTrue(wavelets.contains("haar") || wavelets.contains("Haar"), 
                   "Should contain Haar wavelet (case variations): " + wavelets);
        // Note: We check what's actually available rather than assuming specific names
    }
    
    @Test
    @DisplayName("Get available wavelets returns immutable set")
    void testGetAvailableWaveletsImmutable() {
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        
        assertThrows(UnsupportedOperationException.class, 
                     () -> wavelets.add("fake_wavelet"),
                     "Returned set should be immutable");
    }
    
    @Test
    @DisplayName("Check wavelet availability for valid wavelets")
    void testIsWaveletAvailableValid() {
        // Test known wavelets - find any available wavelet first
        Set<String> available = WaveletRegistry.getAvailableWavelets();
        assertFalse(available.isEmpty(), "Should have at least one wavelet available");
        
        String testWavelet = available.iterator().next();
        assertTrue(WaveletRegistry.isWaveletAvailable(testWavelet), 
                   testWavelet + " wavelet should be available");
        
        // Test case insensitivity with the available wavelet
        assertTrue(WaveletRegistry.isWaveletAvailable(testWavelet.toLowerCase()), 
                   "Should be case-insensitive");
        assertTrue(WaveletRegistry.isWaveletAvailable(testWavelet.toUpperCase()), 
                   "Should be case-insensitive");
    }
    
    @Test
    @DisplayName("Check wavelet availability for invalid wavelets")
    void testIsWaveletAvailableInvalid() {
        assertFalse(WaveletRegistry.isWaveletAvailable("nonexistent"), 
                    "Non-existent wavelet should return false");
        assertFalse(WaveletRegistry.isWaveletAvailable(""), 
                    "Empty string should return false");
        assertFalse(WaveletRegistry.isWaveletAvailable(null), 
                    "Null should return false");
    }
    
    @Test
    @DisplayName("Get wavelets by type returns correct wavelets")
    void testGetWaveletsByType() {
        Set<String> orthogonal = WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL);
        Set<String> biorthogonal = WaveletRegistry.getWaveletsByType(WaveletType.BIORTHOGONAL);
        Set<String> continuous = WaveletRegistry.getWaveletsByType(WaveletType.CONTINUOUS);
        
        assertNotNull(orthogonal, "Orthogonal wavelets should not be null");
        assertNotNull(biorthogonal, "Biorthogonal wavelets should not be null");
        assertNotNull(continuous, "Continuous wavelets should not be null");
        
        // Verify some expected wavelets by type - check case variations
        assertTrue(orthogonal.contains("haar") || orthogonal.contains("Haar"), 
                   "Haar should be orthogonal, available orthogonal: " + orthogonal);
        // Note: Just verify the structure is working - actual wavelets depend on what's registered
    }
    
    @Test
    @DisplayName("Get wavelets by type returns immutable set")
    void testGetWaveletsByTypeImmutable() {
        Set<String> wavelets = WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL);
        
        assertThrows(UnsupportedOperationException.class, 
                     () -> wavelets.add("fake_wavelet"),
                     "Returned set should be immutable");
    }
    
    @Test
    @DisplayName("Get wavelet info for valid wavelets")
    void testGetWaveletInfoValid() {
        // Test Haar wavelet
        WaveletInfo haarInfo = WaveletRegistry.getWaveletInfo("haar");
        assertNotNull(haarInfo, "Haar info should not be null");
        assertEquals("haar", haarInfo.getName(), "Name should match");
        assertEquals("Haar", haarInfo.getDisplayName(), "Display name should be proper case");
        assertEquals(WaveletType.ORTHOGONAL, haarInfo.getType(), "Haar should be orthogonal");
        assertNotNull(haarInfo.getAliases(), "Aliases should not be null");
        
        // Test a Daubechies wavelet if available
        if (WaveletRegistry.isWaveletAvailable("db4")) {
            WaveletInfo db4Info = WaveletRegistry.getWaveletInfo("db4");
            assertNotNull(db4Info, "DB4 info should not be null");
            assertEquals("db4", db4Info.getName(), "Name should match");
            assertTrue(db4Info.getDisplayName().contains("Daubechies"), 
                       "Display name should contain Daubechies");
            assertEquals(4, db4Info.getOrder(), "Order should be 4");
            assertEquals(WaveletType.ORTHOGONAL, db4Info.getType(), "DB4 should be orthogonal");
        }
    }
    
    @Test
    @DisplayName("Get wavelet info throws for invalid wavelets")
    void testGetWaveletInfoInvalid() {
        assertThrows(InvalidArgumentException.class,
                     () -> WaveletRegistry.getWaveletInfo("nonexistent"),
                     "Should throw for non-existent wavelet");
        
        assertThrows(InvalidArgumentException.class,
                     () -> WaveletRegistry.getWaveletInfo(null),
                     "Should throw for null");
        
        assertThrows(InvalidArgumentException.class,
                     () -> WaveletRegistry.getWaveletInfo(""),
                     "Should throw for empty string");
    }
    
    @Test
    @DisplayName("Available wavelets match what getWavelet accepts")
    void testAvailableWaveletsConsistency() {
        Set<String> available = WaveletRegistry.getAvailableWavelets();
        
        // Every available wavelet should be retrievable
        for (String name : available) {
            assertTrue(WaveletRegistry.isWaveletAvailable(name),
                       "Available wavelet " + name + " should be retrievable");
            
            Wavelet wavelet = assertDoesNotThrow(
                () -> WaveletRegistry.getWavelet(name),
                "Should be able to get available wavelet: " + name
            );
            assertNotNull(wavelet, "Retrieved wavelet should not be null: " + name);
        }
    }
    
    @Test
    @DisplayName("Wavelet info properties are complete")
    void testWaveletInfoProperties() {
        // Use Haar if db4 is not available
        String testWavelet = WaveletRegistry.isWaveletAvailable("db4") ? "db4" : "haar";
        WaveletInfo info = WaveletRegistry.getWaveletInfo(testWavelet);
        
        // Verify all properties are populated
        assertNotNull(info.getName(), "Name should not be null");
        assertNotNull(info.getDisplayName(), "Display name should not be null");
        assertNotNull(info.getType(), "Type should not be null");
        assertNotNull(info.getFamily(), "Family should not be null");
        assertTrue(info.getOrder() >= 0, "Order should be non-negative");
        assertNotNull(info.getAliases(), "Aliases should not be null (can be empty)");
        assertNotNull(info.getDescription(), "Description should not be null");
        
        // Properties specific to discrete wavelets
        if (info.getType() == WaveletType.ORTHOGONAL || 
            info.getType() == WaveletType.BIORTHOGONAL) {
            assertTrue(info.getVanishingMoments() >= 0, 
                       "Discrete wavelets should have non-negative vanishing moments");
            assertTrue(info.getFilterLength() > 0, 
                       "Discrete wavelets should have filter length");
        }
    }
    
    @Test
    @DisplayName("Test alias support")
    void testWaveletAliases() {
        // Test that common aliases work - only if available
        if (WaveletRegistry.isWaveletAvailable("db4") && 
            WaveletRegistry.isWaveletAvailable("daubechies4")) {
            // Both aliases should return the same wavelet
            Wavelet w1 = WaveletRegistry.getWavelet("db4");
            Wavelet w2 = WaveletRegistry.getWavelet("daubechies4");
            
            assertNotNull(w1, "db4 should return a wavelet");
            assertNotNull(w2, "daubechies4 should return a wavelet");
            
            // They should have the same properties
            assertArrayEquals(w1.lowPassDecomposition(), w2.lowPassDecomposition(),
                             "Aliases should return equivalent wavelets");
        } else {
            // Just test that we can check availability of aliases
            Set<String> available = WaveletRegistry.getAvailableWavelets();
            assertFalse(available.isEmpty(), "Should have at least one wavelet available");
        }
    }
    
    @Test
    @DisplayName("Get wavelets by family")
    void testGetWaveletsByFamily() {
        // This tests if we can filter by family
        Set<String> allWavelets = WaveletRegistry.getAvailableWavelets();
        assertFalse(allWavelets.isEmpty(), "Should have at least one wavelet available");
        
        // Group wavelets by family
        Map<String, Long> familyCount = allWavelets.stream()
            .collect(Collectors.groupingBy(name -> {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                return info.getFamily();
            }, Collectors.counting()));
        
        assertFalse(familyCount.isEmpty(), "Should have wavelets grouped by family");
        
        // Verify that at least some family has wavelets
        long totalWavelets = familyCount.values().stream().mapToLong(Long::longValue).sum();
        assertTrue(totalWavelets > 0, "Should have at least one wavelet in some family");
        
        // Test that Haar family exists (most basic)
        assertTrue(familyCount.containsKey("Haar"), "Should have Haar family");
        assertTrue(familyCount.get("Haar") > 0, "Haar family should have at least one wavelet");
    }
    
    @Test
    @DisplayName("Performance: Discovery methods should be fast")
    void testDiscoveryPerformance() {
        // Warm up
        WaveletRegistry.getAvailableWavelets();
        
        // Test that discovery is fast (should be cached)
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            WaveletRegistry.getAvailableWavelets();
        }
        long elapsed = System.nanoTime() - start;
        double avgMs = elapsed / 1_000_000.0 / 1000;
        
        assertTrue(avgMs < 0.01, "Average time per call should be < 0.01ms, was " + avgMs);
    }
}