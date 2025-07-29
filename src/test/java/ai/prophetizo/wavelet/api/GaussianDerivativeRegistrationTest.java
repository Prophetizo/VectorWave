package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class GaussianDerivativeRegistrationTest {
    
    @Test
    @DisplayName("Should register Gaussian derivative wavelets")
    void testGaussianDerivativeRegistration() {
        // Test that all Gaussian derivative wavelets are registered
        assertTrue(WaveletRegistry.hasWavelet("gaus1"));
        assertTrue(WaveletRegistry.hasWavelet("gaus2"));
        assertTrue(WaveletRegistry.hasWavelet("gaus3"));
        assertTrue(WaveletRegistry.hasWavelet("gaus4"));
        
        // Test that they are classified as continuous wavelets
        assertTrue(WaveletRegistry.getContinuousWavelets().contains("gaus1"));
        assertTrue(WaveletRegistry.getContinuousWavelets().contains("gaus2"));
        assertTrue(WaveletRegistry.getContinuousWavelets().contains("gaus3"));
        assertTrue(WaveletRegistry.getContinuousWavelets().contains("gaus4"));
    }
    
    @Test
    @DisplayName("Should retrieve correct Gaussian derivative wavelet instances")
    void testRetrieveGaussianDerivatives() {
        // Test retrieving each order
        Wavelet gaus1 = WaveletRegistry.getWavelet("gaus1");
        assertNotNull(gaus1);
        assertInstanceOf(GaussianDerivativeWavelet.class, gaus1);
        assertEquals(1, ((GaussianDerivativeWavelet) gaus1).getDerivativeOrder());
        
        Wavelet gaus2 = WaveletRegistry.getWavelet("gaus2");
        assertNotNull(gaus2);
        assertInstanceOf(GaussianDerivativeWavelet.class, gaus2);
        assertEquals(2, ((GaussianDerivativeWavelet) gaus2).getDerivativeOrder());
        
        Wavelet gaus3 = WaveletRegistry.getWavelet("gaus3");
        assertNotNull(gaus3);
        assertInstanceOf(GaussianDerivativeWavelet.class, gaus3);
        assertEquals(3, ((GaussianDerivativeWavelet) gaus3).getDerivativeOrder());
        
        Wavelet gaus4 = WaveletRegistry.getWavelet("gaus4");
        assertNotNull(gaus4);
        assertInstanceOf(GaussianDerivativeWavelet.class, gaus4);
        assertEquals(4, ((GaussianDerivativeWavelet) gaus4).getDerivativeOrder());
    }
    
    @Test
    @DisplayName("Should have proper descriptions for Gaussian derivatives")
    void testGaussianDerivativeDescriptions() {
        Wavelet gaus1 = WaveletRegistry.getWavelet("gaus1");
        assertNotNull(gaus1.description());
        assertTrue(gaus1.description().contains("Gaussian"));
        
        Wavelet gaus2 = WaveletRegistry.getWavelet("gaus2");
        assertNotNull(gaus2.description());
        assertTrue(gaus2.description().contains("Gaussian"));
    }
}