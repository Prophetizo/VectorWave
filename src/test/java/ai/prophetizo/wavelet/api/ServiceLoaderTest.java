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
    
    // Minimum expected wavelets based on current providers:
    // OrthogonalWaveletProvider: 9 (Haar, DB2, DB4, SYM2-4, COIF1-3)
    // BiorthogonalWaveletProvider: 1 (BIOR1_3)
    // ContinuousWaveletProvider: 5 (Morlet, GAUS1-4)
    // FinancialWaveletProvider: 4 (Paul, DOG, Shannon-Gabor, Classical Shannon)
    private static final int MINIMUM_EXPECTED_WAVELETS = 19;
    
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
        
        // Verify we have the expected minimum number of wavelets
        assertTrue(totalWavelets >= MINIMUM_EXPECTED_WAVELETS, 
                  String.format("Should have at least %d wavelets total, but found %d", 
                               MINIMUM_EXPECTED_WAVELETS, totalWavelets));
    }
    
    @Test
    void testWaveletRegistryUsesServiceLoader() {
        // Verify that key wavelets from each provider are registered
        
        // Orthogonal wavelets
        assertTrue(WaveletRegistry.hasWavelet("haar"), "Haar wavelet should be registered");
        assertTrue(WaveletRegistry.hasWavelet("db4"), "Daubechies DB4 should be registered");
        
        // Continuous wavelets
        assertTrue(WaveletRegistry.hasWavelet("morl"), "Morlet wavelet should be registered");
        
        // Financial wavelets - check by searching for specific types rather than hardcoded names
        Set<String> availableWavelets = WaveletRegistry.getAvailableWavelets();
        boolean hasPaulWavelet = availableWavelets.stream()
            .anyMatch(name -> name.startsWith("paul"));
        assertTrue(hasPaulWavelet, "Should have at least one Paul wavelet registered");
        
        boolean hasGaussianDerivative = availableWavelets.stream()
            .anyMatch(name -> name.startsWith("gaus") || name.startsWith("dog"));
        assertTrue(hasGaussianDerivative, "Should have at least one Gaussian derivative wavelet registered");
        
        // Verify wavelets by type
        assertFalse(WaveletRegistry.getOrthogonalWavelets().isEmpty(), 
                   "Should have orthogonal wavelets registered");
        assertFalse(WaveletRegistry.getContinuousWavelets().isEmpty(), 
                   "Should have continuous wavelets registered");
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