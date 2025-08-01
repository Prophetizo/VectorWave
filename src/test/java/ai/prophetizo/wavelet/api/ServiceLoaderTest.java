package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify ServiceLoader mechanism is working correctly.
 */
class ServiceLoaderTest {
    
    @Test
    void testServiceLoaderDiscovery() {
        ServiceLoader<WaveletProvider> loader = ServiceLoader.load(WaveletProvider.class);
        
        Set<String> discoveredProviders = new HashSet<>();
        int totalWavelets = 0;
        
        for (WaveletProvider provider : loader) {
            assertNotNull(provider);
            discoveredProviders.add(provider.getClass().getName());
            
            var wavelets = provider.getWavelets();
            assertNotNull(wavelets);
            assertFalse(wavelets.isEmpty(), "Provider should return at least one wavelet");
            
            totalWavelets += wavelets.size();
        }
        
        // Verify all expected providers are discovered
        assertTrue(discoveredProviders.contains("ai.prophetizo.wavelet.api.providers.OrthogonalWaveletProvider"));
        assertTrue(discoveredProviders.contains("ai.prophetizo.wavelet.api.providers.BiorthogonalWaveletProvider"));
        assertTrue(discoveredProviders.contains("ai.prophetizo.wavelet.cwt.providers.ContinuousWaveletProvider"));
        assertTrue(discoveredProviders.contains("ai.prophetizo.wavelet.cwt.finance.providers.FinancialWaveletProvider"));
        
        // Verify we have a reasonable number of wavelets
        assertTrue(totalWavelets >= 15, "Should have at least 15 wavelets total");
    }
    
    @Test
    void testWaveletRegistryUsesServiceLoader() {
        // Verify that wavelets are properly registered
        assertNotNull(WaveletRegistry.getWavelet("haar"));
        assertNotNull(WaveletRegistry.getWavelet("db4"));
        assertNotNull(WaveletRegistry.getWavelet("morl"));
        assertNotNull(WaveletRegistry.getWavelet("paul4")); // Paul wavelet with order 4
        assertNotNull(WaveletRegistry.getWavelet("gaus1")); // Gaussian derivative order 1
        
        // Verify wavelets by type
        assertFalse(WaveletRegistry.getOrthogonalWavelets().isEmpty());
        assertFalse(WaveletRegistry.getContinuousWavelets().isEmpty());
    }
    
    @Test
    void testManualRegistration() {
        // Test that we can manually register wavelets at runtime
        // Since all wavelet implementations are sealed, we'll test by registering
        // an existing wavelet instance and verifying the manual registration API works
        var haar = new Haar();
        
        // Get initial count
        int initialCount = WaveletRegistry.getAvailableWavelets().size();
        
        // Try to register it again (should be idempotent)
        WaveletRegistry.registerWavelet(haar);
        
        // Count should remain the same since it's already registered
        assertEquals(initialCount, WaveletRegistry.getAvailableWavelets().size());
        
        // Verify it's still accessible
        assertTrue(WaveletRegistry.hasWavelet("haar"));
    }
    
    @Test
    void testReload() {
        // Get initial count
        int initialCount = WaveletRegistry.getAvailableWavelets().size();
        
        // Reload should maintain the same wavelets
        WaveletRegistry.reload();
        
        assertEquals(initialCount, WaveletRegistry.getAvailableWavelets().size());
        
        // Verify wavelets are still accessible after reload
        assertNotNull(WaveletRegistry.getWavelet("haar"));
        assertNotNull(WaveletRegistry.getWavelet("db4"));
    }
}