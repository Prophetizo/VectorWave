package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.cwt.FrequencyBSplineWavelet;
import ai.prophetizo.wavelet.cwt.ShannonWavelet;
import ai.prophetizo.wavelet.cwt.finance.ShannonGaborWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the specific issue mentioned in GitHub issue #196 is fixed.
 * Previously, both SHANNON and FBSP wavelets incorrectly used ShannonGaborWavelet.
 */
class Issue196FixTest {
    
    @Test
    @DisplayName("Shannon and FBSP wavelets should no longer both use ShannonGaborWavelet")
    void testShannonAndFBSPNoDifferentImplementations() {
        Wavelet shannon = WaveletRegistry.getWavelet(WaveletName.SHANNON);
        Wavelet fbsp = WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Verify they are NOT both ShannonGaborWavelet instances
        assertFalse(shannon instanceof ShannonGaborWavelet && fbsp instanceof ShannonGaborWavelet,
            "SHANNON and FBSP should not both use ShannonGaborWavelet");
        
        // Verify correct types
        assertTrue(shannon instanceof ShannonWavelet, 
            "SHANNON should use ShannonWavelet implementation");
        assertTrue(fbsp instanceof FrequencyBSplineWavelet,
            "FBSP should use FrequencyBSplineWavelet implementation");
        
        // Verify they are different classes
        assertNotEquals(shannon.getClass(), fbsp.getClass(),
            "SHANNON and FBSP should use different wavelet classes");
    }
    
    @Test
    @DisplayName("Verify mathematical correctness of separated wavelets")
    void testMathematicalCorrectness() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Test at t=0 where both should have defined values
        double shannonAt0 = shannon.psi(0);
        double fbspAt0 = fbsp.psi(0);
        
        // Values should be finite and non-zero
        assertTrue(Double.isFinite(shannonAt0), "Shannon value at t=0 should be finite");
        assertTrue(Double.isFinite(fbspAt0), "FBSP value at t=0 should be finite");
        
        // Values should be different
        assertNotEquals(shannonAt0, fbspAt0, 1e-10, 
            "Shannon and FBSP should produce different values at t=0");
    }
    
    @Test
    @DisplayName("Verify wavelets have expected characteristics")
    void testWaveletCharacteristics() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Shannon should be real
        assertFalse(shannon.isComplex(), "Shannon wavelet should be real-valued");
        
        // FBSP should be complex
        assertTrue(fbsp.isComplex(), "FBSP wavelet should be complex-valued");
        
        // Both should have positive bandwidth and center frequency
        assertTrue(shannon.bandwidth() > 0, "Shannon bandwidth should be positive");
        assertTrue(shannon.centerFrequency() > 0, "Shannon center frequency should be positive");
        assertTrue(fbsp.bandwidth() > 0, "FBSP bandwidth should be positive");
        assertTrue(fbsp.centerFrequency() > 0, "FBSP center frequency should be positive");
    }
}