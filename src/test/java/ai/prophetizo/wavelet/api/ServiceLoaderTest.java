package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify ServiceLoader mechanism is working correctly.
 */
class ServiceLoaderTest {
    
    @Test
    void testServiceLoaderDiscovery() {
        ServiceLoader<WaveletProvider> loader = ServiceLoader.load(WaveletProvider.class);
        
        Map<String, Integer> providerWaveletCounts = new HashMap<>();
        Set<String> expectedProviders = Set.of(
            "ai.prophetizo.wavelet.api.providers.OrthogonalWaveletProvider",
            "ai.prophetizo.wavelet.api.providers.BiorthogonalWaveletProvider",
            "ai.prophetizo.wavelet.cwt.providers.ContinuousWaveletProvider",
            "ai.prophetizo.wavelet.cwt.finance.providers.FinancialWaveletProvider"
        );
        
        for (WaveletProvider provider : loader) {
            assertNotNull(provider);
            String providerName = provider.getClass().getName();
            
            var wavelets = provider.getWavelets();
            assertNotNull(wavelets, "Provider " + providerName + " should not return null");
            assertFalse(wavelets.isEmpty(), 
                       "Provider " + providerName + " should return at least one wavelet");
            
            providerWaveletCounts.put(providerName, wavelets.size());
        }
        
        // Verify all expected providers are discovered
        for (String expectedProvider : expectedProviders) {
            assertTrue(providerWaveletCounts.containsKey(expectedProvider),
                      "Expected provider not found: " + expectedProvider);
            assertTrue(providerWaveletCounts.get(expectedProvider) > 0,
                      "Provider " + expectedProvider + " should contribute at least one wavelet");
        }
        
        // Verify we have a reasonable total (at least one wavelet per provider)
        int totalWavelets = providerWaveletCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        assertTrue(totalWavelets >= expectedProviders.size(), 
                  "Should have at least one wavelet per provider");
        
        // Log discovered wavelets for debugging
        System.out.println("Discovered wavelets by provider:");
        providerWaveletCounts.forEach((provider, count) -> 
            System.out.println("  " + provider.substring(provider.lastIndexOf('.') + 1) + ": " + count));
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