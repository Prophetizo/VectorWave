package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Extended Daubechies wavelets (DB12-DB20).
 * Verifies mathematical properties and integration with the VectorWave API.
 */
class ExtendedDaubechiesTest {

    @ParameterizedTest
    @EnumSource(value = WaveletName.class, names = {"DB12", "DB14", "DB16", "DB18", "DB20"})
    @DisplayName("Extended Daubechies wavelets should be available in registry")
    void extendedDaubechiesWaveletsAvailableInRegistry(WaveletName waveletName) {
        assertTrue(WaveletRegistry.isWaveletAvailable(waveletName), 
                   "Wavelet " + waveletName + " should be available in registry");
        
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(wavelet, "Retrieved wavelet should not be null");
        assertTrue(wavelet instanceof Daubechies, "Should be a Daubechies wavelet");
        assertTrue(wavelet instanceof OrthogonalWavelet, "Should be an orthogonal wavelet");
    }

    @ParameterizedTest  
    @EnumSource(value = WaveletName.class, names = {"DB12", "DB14", "DB16", "DB18", "DB20"})
    @DisplayName("Extended Daubechies wavelets should have correct properties")
    void extendedDaubechiesWaveletsHaveCorrectProperties(WaveletName waveletName) {
        Daubechies wavelet = (Daubechies) WaveletRegistry.getWavelet(waveletName);
        
        // Check order matches the wavelet name
        int expectedOrder = Integer.parseInt(waveletName.name().substring(2));
        assertEquals(expectedOrder, wavelet.order(), "Order should match wavelet name");
        assertEquals(expectedOrder, wavelet.vanishingMoments(), "Vanishing moments should equal order");
        
        // Check filter length = 2 * order
        double[] coeffs = wavelet.lowPassDecomposition();
        assertEquals(2 * expectedOrder, coeffs.length, "Filter length should be 2 * order");
        
        // Check name
        assertEquals(waveletName.getCode(), wavelet.name(), "Name should match enum code");
    }

    @ParameterizedTest
    @EnumSource(value = WaveletName.class, names = {"DB12", "DB14", "DB16", "DB18", "DB20"})
    @DisplayName("Extended Daubechies wavelets should satisfy orthogonality conditions")
    void extendedDaubechiesWaveletsSatisfyOrthogonality(WaveletName waveletName) {
        Daubechies wavelet = (Daubechies) WaveletRegistry.getWavelet(waveletName);
        double[] h = wavelet.lowPassDecomposition();
        double tolerance = 1e-12;
        
        // Check sum of coefficients = √2
        double sum = 0;
        for (double coeff : h) {
            sum += coeff;
        }
        assertEquals(Math.sqrt(2), sum, tolerance, "Sum of coefficients should equal √2");
        
        // Check sum of squares = 1
        double sumSquares = 0;
        for (double coeff : h) {
            sumSquares += coeff * coeff;
        }
        assertEquals(1.0, sumSquares, tolerance, "Sum of squared coefficients should equal 1");
        
        // Check orthogonality for even shifts
        for (int k = 2; k < h.length; k += 2) {
            double dot = 0;
            for (int n = 0; n < h.length - k; n++) {
                dot += h[n] * h[n + k];
            }
            assertEquals(0.0, dot, tolerance, 
                        "Dot product with shift " + k + " should be zero");
        }
    }

    @ParameterizedTest
    @EnumSource(value = WaveletName.class, names = {"DB12", "DB14", "DB16", "DB18", "DB20"})
    @DisplayName("Extended Daubechies wavelets should have correct vanishing moments")
    void extendedDaubechiesWaveletsHaveCorrectVanishingMoments(WaveletName waveletName) {
        Daubechies wavelet = (Daubechies) WaveletRegistry.getWavelet(waveletName);
        double[] g = wavelet.highPassDecomposition();
        int order = wavelet.order();
        double baseTolerance = 1e-10;
        
        // Check first N polynomial moments are zero for the wavelet (high-pass) function
        for (int p = 0; p < Math.min(order, 8); p++) { // Limit to avoid numerical instability
            double moment = 0;
            for (int n = 0; n < g.length; n++) {
                moment += Math.pow(n, p) * g[n];
            }
            
            // Tolerance increases with moment order due to numerical accumulation
            double tolerance = baseTolerance * Math.pow(10, p);
            assertEquals(0.0, moment, tolerance,
                        String.format("Vanishing moment %d should be zero for %s", p, waveletName));
        }
    }

    @Test
    @DisplayName("Daubechies coefficient utility should verify extended wavelets")
    void daubechiesCoefficientsUtilityVerifiesExtendedWavelets() {
        for (int order : new int[]{12, 14, 16, 18, 20}) {
            assertTrue(ai.prophetizo.wavelet.util.DaubechiesCoefficients.isSupported(order),
                      "DB" + order + " should be supported");
            
            double[] coeffs = ai.prophetizo.wavelet.util.DaubechiesCoefficients.getCoefficients(order);
            assertTrue(ai.prophetizo.wavelet.util.DaubechiesCoefficients.verifyCoefficients(coeffs, order),
                      "DB" + order + " coefficients should pass verification");
            
            assertEquals(2 * order, coeffs.length, "DB" + order + " should have " + (2 * order) + " coefficients");
        }
    }

    @Test
    @DisplayName("All Daubechies wavelets should be listed in getDaubechiesWavelets")
    void allDaubechiesWaveletsListedInGetMethod() {
        var daubechiesWavelets = WaveletRegistry.getDaubechiesWavelets();
        
        // Should include all Daubechies wavelets from DB2 to DB20
        String[] expectedNames = {"DB10", "DB12", "DB14", "DB16", "DB18", "DB2", "DB20", "DB4", "DB6", "DB8"};
        assertEquals(expectedNames.length, daubechiesWavelets.size(), 
                    "Should have exactly " + expectedNames.length + " Daubechies wavelets");
        
        for (String expectedName : expectedNames) {
            assertTrue(daubechiesWavelets.contains(WaveletName.valueOf(expectedName)),
                      "Should contain " + expectedName);
        }
    }

    @Test
    @DisplayName("Extended Daubechies wavelets should work with MODWT transform compatibility")
    void extendedDaubechiesWaveletsCompatibleWithMODWT() {
        for (WaveletName name : new WaveletName[]{WaveletName.DB12, WaveletName.DB14, WaveletName.DB16, 
                                                  WaveletName.DB18, WaveletName.DB20}) {
            assertTrue(WaveletRegistry.isCompatible(name, TransformType.MODWT),
                      name + " should be compatible with MODWT");
            
            assertEquals(TransformType.MODWT, WaveletRegistry.getRecommendedTransform(name),
                        name + " should have MODWT as recommended transform");
        }
    }
}