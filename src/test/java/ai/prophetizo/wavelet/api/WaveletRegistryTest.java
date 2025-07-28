package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WaveletRegistryTest {

    @Test
    void testGetWavelet_ExistingWavelet() {
        // Test with various known wavelets
        Wavelet haar = WaveletRegistry.getWavelet("haar");
        assertNotNull(haar);
        assertEquals("Haar", haar.name());
        assertTrue(haar instanceof Haar);
        
        Wavelet db2 = WaveletRegistry.getWavelet("db2");
        assertNotNull(db2);
        assertEquals("db2", db2.name());
        assertTrue(db2 instanceof Daubechies);
        
        Wavelet morlet = WaveletRegistry.getWavelet("morl");
        assertNotNull(morlet);
        assertEquals("morl", morlet.name());
        assertTrue(morlet instanceof MorletWavelet);
    }

    @Test
    void testGetWavelet_CaseInsensitive() {
        // Test that lookup is case-insensitive
        Wavelet lower = WaveletRegistry.getWavelet("haar");
        Wavelet upper = WaveletRegistry.getWavelet("HAAR");
        Wavelet mixed = WaveletRegistry.getWavelet("HaAr");
        
        assertNotNull(lower);
        assertNotNull(upper);
        assertNotNull(mixed);
        assertEquals(lower.name(), upper.name());
        assertEquals(lower.name(), mixed.name());
    }

    @Test
    void testGetWavelet_NonExistentWavelet() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletRegistry.getWavelet("nonexistent")
        );
        assertEquals("Unknown wavelet: nonexistent", exception.getMessage());
    }

    @Test
    void testHasWavelet() {
        // Existing wavelets
        assertTrue(WaveletRegistry.hasWavelet("haar"));
        assertTrue(WaveletRegistry.hasWavelet("db2"));
        assertTrue(WaveletRegistry.hasWavelet("db4"));
        assertTrue(WaveletRegistry.hasWavelet("sym2"));
        assertTrue(WaveletRegistry.hasWavelet("coif1"));
        assertTrue(WaveletRegistry.hasWavelet("bior1.3"));
        assertTrue(WaveletRegistry.hasWavelet("morl"));
        
        // Case insensitive
        assertTrue(WaveletRegistry.hasWavelet("HAAR"));
        assertTrue(WaveletRegistry.hasWavelet("DB2"));
        
        // Non-existent wavelets
        assertFalse(WaveletRegistry.hasWavelet("nonexistent"));
        assertFalse(WaveletRegistry.hasWavelet(""));
        assertFalse(WaveletRegistry.hasWavelet("db10")); // Not registered
    }

    @Test
    void testGetAvailableWavelets() {
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        
        // Check for some expected wavelets (registry stores lowercase keys)
        assertTrue(wavelets.contains("haar"));
        assertTrue(wavelets.contains("db2"));
        assertTrue(wavelets.contains("db4"));
        assertTrue(wavelets.contains("sym2"));
        assertTrue(wavelets.contains("coif1"));
        assertTrue(wavelets.contains("bior1.3"));
        assertTrue(wavelets.contains("morl")); // MorletWavelet returns "morl"
        
        // Verify it's unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> wavelets.add("new"));
        assertThrows(UnsupportedOperationException.class, () -> wavelets.remove("haar"));
    }

    @Test
    void testGetWaveletsByType() {
        // Test orthogonal wavelets
        List<String> orthogonal = WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL);
        assertNotNull(orthogonal);
        assertFalse(orthogonal.isEmpty());
        assertTrue(orthogonal.contains("Haar")); // Actual name is "Haar"
        assertTrue(orthogonal.contains("db2"));
        assertTrue(orthogonal.contains("sym2"));
        assertTrue(orthogonal.contains("coif1"));
        
        // Test biorthogonal wavelets
        List<String> biorthogonal = WaveletRegistry.getWaveletsByType(WaveletType.BIORTHOGONAL);
        assertNotNull(biorthogonal);
        assertFalse(biorthogonal.isEmpty());
        assertTrue(biorthogonal.contains("bior1.3"));
        
        // Test continuous wavelets
        List<String> continuous = WaveletRegistry.getWaveletsByType(WaveletType.CONTINUOUS);
        assertNotNull(continuous);
        assertFalse(continuous.isEmpty());
        assertTrue(continuous.contains("morl"));
        
        // Verify lists are unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> orthogonal.add("new"));
        assertThrows(UnsupportedOperationException.class, () -> biorthogonal.remove(0));
    }

    @Test
    void testGetOrthogonalWavelets() {
        List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        
        assertNotNull(orthogonal);
        assertFalse(orthogonal.isEmpty());
        
        // Verify all returned wavelets are actually orthogonal
        for (String name : orthogonal) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            assertEquals(WaveletType.ORTHOGONAL, w.getType());
            assertTrue(w instanceof OrthogonalWavelet);
        }
    }

    @Test
    void testGetBiorthogonalWavelets() {
        List<String> biorthogonal = WaveletRegistry.getBiorthogonalWavelets();
        
        assertNotNull(biorthogonal);
        assertFalse(biorthogonal.isEmpty());
        
        // Verify all returned wavelets are actually biorthogonal
        for (String name : biorthogonal) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            assertEquals(WaveletType.BIORTHOGONAL, w.getType());
            assertTrue(w instanceof BiorthogonalWavelet);
        }
    }

    @Test
    void testGetContinuousWavelets() {
        List<String> continuous = WaveletRegistry.getContinuousWavelets();
        
        assertNotNull(continuous);
        assertFalse(continuous.isEmpty());
        
        // Verify all returned wavelets are actually continuous
        for (String name : continuous) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            assertEquals(WaveletType.CONTINUOUS, w.getType());
            assertTrue(w instanceof ContinuousWavelet);
        }
    }

    @Test
    void testPrintAvailableWavelets() {
        // Redirect System.out to capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(new PrintStream(outContent));
            
            WaveletRegistry.printAvailableWavelets();
            
            String output = outContent.toString();
            
            // Verify output contains expected content
            assertTrue(output.contains("Available Wavelets in VectorWave:"));
            assertTrue(output.contains("================================="));
            assertTrue(output.contains("ORTHOGONAL Wavelets:"));
            assertTrue(output.contains("BIORTHOGONAL Wavelets:"));
            assertTrue(output.contains("CONTINUOUS Wavelets:"));
            
            // Check for specific wavelets (with actual names as returned by wavelets)
            assertTrue(output.contains("Haar:")); // Registry key is "haar" but name() returns "Haar"
            assertTrue(output.contains("db2:"));
            assertTrue(output.contains("morl:")); // MorletWavelet name is "morl"
            assertTrue(output.contains("bior1.3:"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testWaveletConsistency() {
        // Verify that all wavelets in the registry have consistent properties
        Set<String> allWavelets = WaveletRegistry.getAvailableWavelets();
        
        for (String name : allWavelets) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            
            // Basic properties should be non-null
            assertNotNull(w.name());
            assertNotNull(w.description());
            assertNotNull(w.getType());
            
            // Name should match (case-insensitive)
            assertEquals(name.toLowerCase(), w.name().toLowerCase());
            
            // Type should be valid
            assertTrue(w.getType() == WaveletType.ORTHOGONAL ||
                      w.getType() == WaveletType.BIORTHOGONAL ||
                      w.getType() == WaveletType.CONTINUOUS);
            
            // Verify type consistency
            if (w instanceof OrthogonalWavelet) {
                assertEquals(WaveletType.ORTHOGONAL, w.getType());
            } else if (w instanceof BiorthogonalWavelet) {
                assertEquals(WaveletType.BIORTHOGONAL, w.getType());
            } else if (w instanceof ContinuousWavelet) {
                assertEquals(WaveletType.CONTINUOUS, w.getType());
            }
        }
    }

    @Test
    void testNoMissingTypes() {
        // Verify that registered types have at least one wavelet
        // Note: Not all types in the enum may have registered wavelets
        List<String> orthogonal = WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL);
        assertNotNull(orthogonal);
        assertFalse(orthogonal.isEmpty());
        
        List<String> biorthogonal = WaveletRegistry.getWaveletsByType(WaveletType.BIORTHOGONAL);
        assertNotNull(biorthogonal);
        assertFalse(biorthogonal.isEmpty());
        
        List<String> continuous = WaveletRegistry.getWaveletsByType(WaveletType.CONTINUOUS);
        assertNotNull(continuous);
        assertFalse(continuous.isEmpty());
        
        // DISCRETE, COMPLEX, and SPECIALIZED may not have registered wavelets
        List<String> discrete = WaveletRegistry.getWaveletsByType(WaveletType.DISCRETE);
        assertNotNull(discrete); // Should return empty list, not null
        
        List<String> complex = WaveletRegistry.getWaveletsByType(WaveletType.COMPLEX);
        assertNotNull(complex); // Should return empty list, not null
        
        List<String> specialized = WaveletRegistry.getWaveletsByType(WaveletType.SPECIALIZED);
        assertNotNull(specialized); // Should return empty list, not null
    }

    @Test
    void testSpecificWavelets() {
        // Test specific wavelets are registered correctly
        
        // Daubechies family
        assertTrue(WaveletRegistry.hasWavelet("db2"));
        assertTrue(WaveletRegistry.hasWavelet("db4"));
        
        // Symlet family
        assertTrue(WaveletRegistry.hasWavelet("sym2"));
        assertTrue(WaveletRegistry.hasWavelet("sym3"));
        assertTrue(WaveletRegistry.hasWavelet("sym4"));
        
        // Coiflet family
        assertTrue(WaveletRegistry.hasWavelet("coif1"));
        assertTrue(WaveletRegistry.hasWavelet("coif2"));
        assertTrue(WaveletRegistry.hasWavelet("coif3"));
    }
}