package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the simple WaveletRegistry.
 */
class WaveletRegistryTest {
    
    @Test
    @DisplayName("Get wavelet by name")
    void testGetWavelet() {
        Wavelet haar = WaveletRegistry.getWavelet("haar");
        assertNotNull(haar);
        assertEquals("Haar", haar.name());
        
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        assertNotNull(db4);
        assertEquals("db4", db4.name());
    }
    
    @Test
    @DisplayName("Get wavelet case insensitive")
    void testGetWaveletCaseInsensitive() {
        Wavelet lower = WaveletRegistry.getWavelet("haar");
        Wavelet upper = WaveletRegistry.getWavelet("HAAR");
        Wavelet mixed = WaveletRegistry.getWavelet("HaAr");
        
        assertEquals(lower, upper);
        assertEquals(lower, mixed);
    }
    
    @Test
    @DisplayName("Get wavelet with alias")
    void testGetWaveletAlias() {
        Wavelet db4 = WaveletRegistry.getWavelet("db4");
        Wavelet daubechies4 = WaveletRegistry.getWavelet("daubechies4");
        assertEquals(db4, daubechies4);
        
        Wavelet morl = WaveletRegistry.getWavelet("morl");
        Wavelet morlet = WaveletRegistry.getWavelet("morlet");
        assertEquals(morl, morlet);
    }
    
    @Test
    @DisplayName("Get unknown wavelet throws exception")
    void testGetUnknownWavelet() {
        assertThrows(InvalidArgumentException.class, () -> 
            WaveletRegistry.getWavelet("nonexistent"));
    }
    
    @Test
    @DisplayName("Get wavelet with null name throws exception")
    void testGetWaveletNull() {
        assertThrows(InvalidArgumentException.class, () -> 
            WaveletRegistry.getWavelet(null));
    }
    
    @Test
    @DisplayName("Get wavelet with empty name throws exception")
    void testGetWaveletEmpty() {
        assertThrows(InvalidArgumentException.class, () -> 
            WaveletRegistry.getWavelet(""));
        assertThrows(InvalidArgumentException.class, () -> 
            WaveletRegistry.getWavelet("   "));
    }
    
    @Test
    @DisplayName("Check wavelet existence")
    void testHasWavelet() {
        assertTrue(WaveletRegistry.hasWavelet("haar"));
        assertTrue(WaveletRegistry.hasWavelet("db4"));
        assertTrue(WaveletRegistry.hasWavelet("morl"));
        
        assertFalse(WaveletRegistry.hasWavelet("nonexistent"));
        assertFalse(WaveletRegistry.hasWavelet(null));
        assertFalse(WaveletRegistry.hasWavelet(""));
    }
    
    @Test
    @DisplayName("Get available wavelets")
    void testGetAvailableWavelets() {
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        
        // Check core wavelets are present
        assertTrue(wavelets.contains("haar"));
        assertTrue(wavelets.contains("db2"));
        assertTrue(wavelets.contains("db4"));
        assertTrue(wavelets.contains("sym2"));
        assertTrue(wavelets.contains("coif1"));
        assertTrue(wavelets.contains("morl"));
        
        // Check aliases are present
        assertTrue(wavelets.contains("daubechies4"));
        assertTrue(wavelets.contains("morlet"));
    }
    
    @Test
    @DisplayName("Get orthogonal wavelets")
    void testGetOrthogonalWavelets() {
        List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        
        assertNotNull(orthogonal);
        assertFalse(orthogonal.isEmpty());
        
        // Should contain orthogonal wavelets
        assertTrue(orthogonal.contains("haar"));
        assertTrue(orthogonal.contains("db2"));
        assertTrue(orthogonal.contains("db4"));
        assertTrue(orthogonal.contains("sym2"));
        assertTrue(orthogonal.contains("coif1"));
        
        // Should NOT contain continuous wavelets
        assertFalse(orthogonal.contains("morl"));
        
        // Should be sorted
        List<String> sorted = new ArrayList<>(orthogonal);
        Collections.sort(sorted);
        assertEquals(sorted, orthogonal);
    }
    
    @Test
    @DisplayName("Get continuous wavelets")
    void testGetContinuousWavelets() {
        List<String> continuous = WaveletRegistry.getContinuousWavelets();
        
        assertNotNull(continuous);
        assertFalse(continuous.isEmpty());
        
        // Should contain continuous wavelets
        assertTrue(continuous.contains("morl"));
        
        // Should NOT contain orthogonal wavelets
        assertFalse(continuous.contains("haar"));
        assertFalse(continuous.contains("db4"));
        assertFalse(continuous.contains("sym2"));
        
        // Should be sorted
        List<String> sorted = new ArrayList<>(continuous);
        Collections.sort(sorted);
        assertEquals(sorted, continuous);
    }
    
    @Test
    @DisplayName("All registered wavelets are valid")
    void testAllWaveletsValid() {
        Set<String> wavelets = WaveletRegistry.getAvailableWavelets();
        
        for (String name : wavelets) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            assertNotNull(w, "Wavelet " + name + " should not be null");
            assertNotNull(w.name(), "Wavelet " + name + " should have a name");
            assertNotNull(w.getType(), "Wavelet " + name + " should have a type");
            assertNotNull(w.description(), "Wavelet " + name + " should have a description");
        }
    }
    
    
    @Test
    @DisplayName("Registry is thread-safe")
    void testThreadSafety() throws InterruptedException {
        int threads = 10;
        int iterations = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Concurrent reads
                        WaveletRegistry.getWavelet("haar");
                        WaveletRegistry.hasWavelet("db4");
                        WaveletRegistry.getAvailableWavelets();
                        WaveletRegistry.getOrthogonalWavelets();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        assertEquals(0, errors.get(), "No errors should occur during concurrent access");
    }
}