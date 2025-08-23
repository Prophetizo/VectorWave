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
        // First Extended Set (DB12-DB20)
        assertNotNull(WaveletName.DB12);
        assertNotNull(WaveletName.DB14);
        assertNotNull(WaveletName.DB16);
        assertNotNull(WaveletName.DB18);
        assertNotNull(WaveletName.DB20);
        
        // Advanced Extended Set (DB22-DB45)
        assertNotNull(WaveletName.DB22);
        assertNotNull(WaveletName.DB24);
        assertNotNull(WaveletName.DB26);
        assertNotNull(WaveletName.DB28);
        assertNotNull(WaveletName.DB30);
        assertNotNull(WaveletName.DB32);
        assertNotNull(WaveletName.DB34);
        assertNotNull(WaveletName.DB36);
        assertNotNull(WaveletName.DB38);
        assertNotNull(WaveletName.DB40);
        assertNotNull(WaveletName.DB42);
        assertNotNull(WaveletName.DB44);
        assertNotNull(WaveletName.DB45);
        
        // Check codes for first extended set
        assertEquals("db12", WaveletName.DB12.getCode());
        assertEquals("db14", WaveletName.DB14.getCode());
        assertEquals("db16", WaveletName.DB16.getCode());
        assertEquals("db18", WaveletName.DB18.getCode());
        assertEquals("db20", WaveletName.DB20.getCode());
        
        // Check codes for advanced extended set (sample)
        assertEquals("db22", WaveletName.DB22.getCode());
        assertEquals("db30", WaveletName.DB30.getCode());
        assertEquals("db38", WaveletName.DB38.getCode());
        assertEquals("db45", WaveletName.DB45.getCode());
        
        // Check types for first extended set
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB12.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB14.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB16.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB18.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB20.getType());
        
        // Check types for advanced extended set (sample)
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB22.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB30.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB38.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB45.getType());
    }

    @Test
    @DisplayName("Extended Daubechies wavelets should not be available in registry yet")
    void extendedDaubechiesWaveletsNotYetAvailableInRegistry() {
        // First Extended Set (DB12-DB20) - should not be available yet since implementation is pending
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
        
        // Advanced Extended Set (DB22-DB45) - should not be available yet
        WaveletName[] advancedExtended = {
            WaveletName.DB22, WaveletName.DB24, WaveletName.DB26, WaveletName.DB28, WaveletName.DB30,
            WaveletName.DB32, WaveletName.DB34, WaveletName.DB36, WaveletName.DB38,
            WaveletName.DB40, WaveletName.DB42, WaveletName.DB44, WaveletName.DB45
        };
        
        for (WaveletName name : advancedExtended) {
            assertFalse(WaveletRegistry.isWaveletAvailable(name), 
                       name + " should not be available yet");
        }
    }

    @Test
    @DisplayName("Attempting to get extended Daubechies wavelets should throw appropriate exception")
    void attemptingToGetExtendedDaubechiesWaveletsThrowsException() {
        // First Extended Set (DB12-DB20)
        WaveletName[] firstExtended = {WaveletName.DB12, WaveletName.DB14, WaveletName.DB16, 
                                       WaveletName.DB18, WaveletName.DB20};
        for (WaveletName name : firstExtended) {
            assertThrows(ai.prophetizo.wavelet.exception.InvalidArgumentException.class, 
                        () -> WaveletRegistry.getWavelet(name),
                        "Should throw exception when trying to get " + name);
        }
        
        // Advanced Extended Set (DB22-DB45) - test a representative sample
        WaveletName[] advancedExtended = {WaveletName.DB22, WaveletName.DB30, WaveletName.DB38, WaveletName.DB45};
        for (WaveletName name : advancedExtended) {
            assertThrows(ai.prophetizo.wavelet.exception.InvalidArgumentException.class, 
                        () -> WaveletRegistry.getWavelet(name),
                        "Should throw exception when trying to get " + name);
        }
    }

    @Test
    @DisplayName("DaubechiesCoefficients utility should indicate extended wavelets are not supported")
    void daubechiesCoefficientsUtilityIndicatesExtendedWaveletsNotSupported() {
        // First Extended Set (DB12-DB20)
        for (int order : new int[]{12, 14, 16, 18, 20}) {
            assertFalse(ai.prophetizo.wavelet.util.DaubechiesCoefficients.isSupported(order),
                      "DB" + order + " should not be supported yet");
        }
        
        // Advanced Extended Set (DB22-DB45) - test representative sample
        for (int order : new int[]{22, 24, 30, 32, 38, 40, 42, 44, 45}) {
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
        // First Extended Set - even though not implemented yet, the enum entries should be compatible with MODWT
        for (WaveletName name : new WaveletName[]{WaveletName.DB12, WaveletName.DB14, WaveletName.DB16, 
                                                  WaveletName.DB18, WaveletName.DB20}) {
            assertTrue(WaveletRegistry.isCompatible(name, TransformType.MODWT),
                      name + " should be compatible with MODWT");
            
            assertEquals(TransformType.MODWT, WaveletRegistry.getRecommendedTransform(name),
                        name + " should have MODWT as recommended transform");
        }
        
        // Advanced Extended Set - test representative sample
        for (WaveletName name : new WaveletName[]{WaveletName.DB22, WaveletName.DB30, 
                                                  WaveletName.DB38, WaveletName.DB45}) {
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
        // Original: DB2, DB4, DB6, DB8, DB10 (5)
        // First Extended: DB12, DB14, DB16, DB18, DB20 (5)
        // Advanced Extended: DB22, DB24, DB26, DB28, DB30, DB32, DB34, DB36, DB38, DB40, DB42, DB44, DB45 (13)
        // Total: 23 wavelets
        String[] expectedNames = {
            "DB10", "DB12", "DB14", "DB16", "DB18", "DB2", "DB20", "DB22", "DB24", "DB26", "DB28", "DB30",
            "DB32", "DB34", "DB36", "DB38", "DB4", "DB40", "DB42", "DB44", "DB45", "DB6", "DB8"
        };
        assertEquals(expectedNames.length, daubechiesWavelets.size(), 
                    "Should have exactly " + expectedNames.length + " Daubechies wavelets in enum");
        
        for (String expectedName : expectedNames) {
            assertTrue(daubechiesWavelets.contains(WaveletName.valueOf(expectedName)),
                      "Should contain " + expectedName + " in enum");
        }
    }
}