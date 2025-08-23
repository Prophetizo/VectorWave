package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Extended Daubechies wavelets infrastructure.
 * 
 * This test verifies that the infrastructure is in place for extended Daubechies wavelets,
 * even though the actual wavelets are not yet implemented.
 */
class ExtendedDaubechiesInfrastructureTest {

    @Test
    @DisplayName("WaveletName enum should include extended Daubechies entries")
    void waveletNameEnumIncludesExtendedDaubechiesEntries() {
        // Verify that the enum entries exist (even if not yet registered)
        assertNotNull(WaveletName.DB12);
        assertNotNull(WaveletName.DB14);
        assertNotNull(WaveletName.DB16);
        assertNotNull(WaveletName.DB18);
        assertNotNull(WaveletName.DB20);
        
        assertEquals("db12", WaveletName.DB12.getCode());
        assertEquals("db14", WaveletName.DB14.getCode());
        assertEquals("db16", WaveletName.DB16.getCode());
        assertEquals("db18", WaveletName.DB18.getCode());
        assertEquals("db20", WaveletName.DB20.getCode());
        
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB12.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB14.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB16.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB18.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB20.getType());
    }

    @Test
    @DisplayName("Extended Daubechies wavelets should not be available in registry yet")
    void extendedDaubechiesWaveletsNotYetAvailableInRegistry() {
        // These should not be available yet since implementation is pending
        assertFalse(WaveletRegistry.isWaveletAvailable(WaveletName.DB12), 
                   "DB12 should not be available yet");
        assertFalse(WaveletRegistry.isWaveletAvailable(WaveletName.DB14), 
                   "DB14 should not be available yet");
        assertFalse(WaveletRegistry.isWaveletAvailable(WaveletName.DB16), 
                   "DB16 should not be available yet");
        assertFalse(WaveletRegistry.isWaveletAvailable(WaveletName.DB18), 
                   "DB18 should not be available yet");
        assertFalse(WaveletRegistry.isWaveletAvailable(WaveletName.DB20), 
                   "DB20 should not be available yet");
    }

    @Test
    @DisplayName("Attempting to get extended Daubechies wavelets should throw appropriate exception")
    void attemptingToGetExtendedDaubechiesWaveletsThrowsException() {
        for (WaveletName name : new WaveletName[]{WaveletName.DB12, WaveletName.DB14, WaveletName.DB16, 
                                                  WaveletName.DB18, WaveletName.DB20}) {
            assertThrows(ai.prophetizo.wavelet.exception.InvalidArgumentException.class, 
                        () -> WaveletRegistry.getWavelet(name),
                        "Should throw exception when trying to get " + name);
        }
    }

    @Test
    @DisplayName("DaubechiesCoefficients utility should indicate extended wavelets are not supported")
    void daubechiesCoefficientsUtilityIndicatesExtendedWaveletsNotSupported() {
        for (int order : new int[]{12, 14, 16, 18, 20}) {
            assertFalse(ai.prophetizo.wavelet.util.DaubechiesCoefficients.isSupported(order),
                      "DB" + order + " should not be supported yet");
        }
        
        // But existing ones should still be supported
        for (int order : new int[]{2, 4, 6, 8, 10}) {
            assertTrue(ai.prophetizo.wavelet.util.DaubechiesCoefficients.isSupported(order),
                      "DB" + order + " should be supported");
        }
    }

    @Test
    @DisplayName("Existing Daubechies wavelets should continue to work")
    void existingDaubechiesWaveletsStillWork() {
        for (WaveletName name : new WaveletName[]{WaveletName.DB2, WaveletName.DB4, WaveletName.DB6, 
                                                  WaveletName.DB8, WaveletName.DB10}) {
            assertTrue(WaveletRegistry.isWaveletAvailable(name), 
                       name + " should still be available");
            
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "Should be able to get " + name);
            assertTrue(wavelet instanceof Daubechies, "Should be a Daubechies wavelet");
            assertTrue(wavelet instanceof OrthogonalWavelet, "Should be an orthogonal wavelet");
        }
    }

    @Test
    @DisplayName("DaubechiesCoefficients should work with existing wavelets")
    void daubechiesCoefficientsWorksWithExistingWavelets() {
        for (int order : new int[]{2, 4, 6, 8, 10}) {
            assertDoesNotThrow(() -> {
                double[] coeffs = ai.prophetizo.wavelet.util.DaubechiesCoefficients.getCoefficients(order);
                assertNotNull(coeffs);
                assertEquals(2 * order, coeffs.length);
                assertTrue(ai.prophetizo.wavelet.util.DaubechiesCoefficients.verifyCoefficients(coeffs, order));
            }, "Should be able to get and verify coefficients for DB" + order);
        }
    }

    @Test
    @DisplayName("Extended Daubechies wavelets should be compatible with MODWT when implemented")
    void extendedDaubechiesWaveletsCompatibleWithMODWT() {
        // Even though not implemented yet, the enum entries should be compatible with MODWT
        for (WaveletName name : new WaveletName[]{WaveletName.DB12, WaveletName.DB14, WaveletName.DB16, 
                                                  WaveletName.DB18, WaveletName.DB20}) {
            assertTrue(WaveletRegistry.isCompatible(name, TransformType.MODWT),
                      name + " should be compatible with MODWT");
            
            assertEquals(TransformType.MODWT, WaveletRegistry.getRecommendedTransform(name),
                        name + " should have MODWT as recommended transform");
        }
    }

    @Test
    @DisplayName("getDaubechiesWavelets should include extended wavelets in enum but not implementation")
    void getDaubechiesWaveletsIncludesExtendedWavelets() {
        var daubechiesWavelets = WaveletRegistry.getDaubechiesWavelets();
        
        // Should include all Daubechies wavelets in the enum (even if not implemented)
        String[] expectedNames = {"DB10", "DB12", "DB14", "DB16", "DB18", "DB2", "DB20", "DB4", "DB6", "DB8"};
        assertEquals(expectedNames.length, daubechiesWavelets.size(), 
                    "Should have exactly " + expectedNames.length + " Daubechies wavelets in enum");
        
        for (String expectedName : expectedNames) {
            assertTrue(daubechiesWavelets.contains(WaveletName.valueOf(expectedName)),
                      "Should contain " + expectedName + " in enum");
        }
    }
}