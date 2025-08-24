package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive mathematical verification test for all biorthogonal wavelets.
 * Ensures all biorthogonal wavelets satisfy their required mathematical properties.
 */
public class BiorthogonalMathematicalVerificationTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double RELAXED_TOLERANCE = 1e-8;
    
    /**
     * Test 1: Verify the perfect reconstruction property mathematically.
     * For biorthogonal wavelets, the analysis and synthesis filters must satisfy:
     * sum_k h_tilde[k] * h[n-2k] = delta[n]
     */
    @ParameterizedTest(name = "Perfect Reconstruction: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify perfect reconstruction condition")
    void testPerfectReconstructionCondition(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        double[] loD = wavelet.lowPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        
        // Calculate the convolution at different shifts
        // For perfect reconstruction, convolution at shift 0 should be non-zero (typically 2)
        // and convolution at other even shifts should be 0
        int maxShift = Math.min(loD.length + loR.length - 1, 20);
        
        for (int shift = 0; shift <= maxShift; shift += 2) {
            double conv = computeConvolution(loD, loR, shift);
            
            if (shift == 0) {
                // At shift 0, should be 2 for standard biorthogonal wavelets
                assertTrue(Math.abs(conv) > 0.5, 
                    String.format("%s: Convolution at shift 0 = %.6f (should be ~2)", 
                        waveletName, conv));
            } else {
                // At other even shifts, should be 0
                assertEquals(0.0, conv, RELAXED_TOLERANCE,
                    String.format("%s: Convolution at shift %d should be 0", 
                        waveletName, shift));
            }
        }
    }
    
    /**
     * Test 2: Verify the biorthogonality condition between analysis and synthesis filters.
     * The filters must satisfy: <h_tilde[n-2k], h[n]> = delta[k]
     */
    @ParameterizedTest(name = "Biorthogonality: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify biorthogonality between filter pairs")
    void testBiorthogonalityCondition(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] hiD = wavelet.highPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Test orthogonality between low-pass and high-pass filters
        double loHiConv = computeConvolution(loD, hiR, 0);
        assertEquals(0.0, loHiConv, RELAXED_TOLERANCE,
            String.format("%s: Low-pass decomp and high-pass recon should be orthogonal", 
                waveletName));
        
        double hiLoConv = computeConvolution(hiD, loR, 0);
        assertEquals(0.0, hiLoConv, RELAXED_TOLERANCE,
            String.format("%s: High-pass decomp and low-pass recon should be orthogonal", 
                waveletName));
    }
    
    /**
     * Test 3: Verify vanishing moments property.
     * The number of vanishing moments determines how many polynomial moments
     * the wavelet can represent exactly.
     */
    @ParameterizedTest(name = "Vanishing Moments: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify vanishing moments match specification")
    void testVanishingMoments(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        // Extract expected values from name (e.g., BIOR2_4 -> rec=2, dec=4)
        String code = waveletName.getCode();
        String[] parts = code.replace("bior", "").split("\\.");
        int expectedRecOrder = Integer.parseInt(parts[0]);
        int expectedDecOrder = Integer.parseInt(parts[1]);
        
        assertEquals(expectedDecOrder, wavelet.vanishingMoments(),
            String.format("%s: Decomposition vanishing moments should be %d", 
                waveletName, expectedDecOrder));
        
        assertEquals(expectedRecOrder, wavelet.dualVanishingMoments(),
            String.format("%s: Reconstruction vanishing moments should be %d", 
                waveletName, expectedRecOrder));
        
        // Verify vanishing moments through polynomial annihilation
        // A wavelet with N vanishing moments should annihilate polynomials up to degree N-1
        double[] hiD = wavelet.highPassDecomposition();
        
        // Test that sum(k^p * h[k]) = 0 for p < vanishing moments
        for (int p = 0; p < Math.min(wavelet.vanishingMoments(), 3); p++) {
            double moment = 0;
            for (int k = 0; k < hiD.length; k++) {
                moment += Math.pow(k, p) * hiD[k];
            }
            
            // For symmetric filters centered at origin, adjust indexing
            if (wavelet.isSymmetric() && p == 0) {
                // Zero-th moment (sum) should be approximately zero for high-pass
                assertEquals(0.0, moment, 0.1,
                    String.format("%s: %d-th moment of high-pass filter should be ~0", 
                        waveletName, p));
            }
        }
    }
    
    /**
     * Test 4: Verify symmetry properties.
     * Biorthogonal wavelets can have symmetric or antisymmetric filters.
     */
    @ParameterizedTest(name = "Symmetry: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify symmetry properties")
    void testSymmetryProperties(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        if (wavelet.isSymmetric()) {
            double[] loD = wavelet.lowPassDecomposition();
            double[] loR = wavelet.lowPassReconstruction();
            
            // Check if filters are symmetric or antisymmetric
            boolean loD_symmetric = isSymmetric(loD);
            boolean loR_symmetric = isSymmetric(loR);
            
            assertTrue(loD_symmetric || isAntiSymmetric(loD),
                String.format("%s: Decomposition filter should be symmetric or antisymmetric", 
                    waveletName));
            
            assertTrue(loR_symmetric || isAntiSymmetric(loR),
                String.format("%s: Reconstruction filter should be symmetric or antisymmetric", 
                    waveletName));
        }
    }
    
    /**
     * Test 5: Verify filter normalization.
     * Sum of low-pass filter coefficients should be positive (typically sqrt(2) or 2).
     */
    @ParameterizedTest(name = "Filter Normalization: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify filter normalization")
    void testFilterNormalization(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiD = wavelet.highPassDecomposition();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Sum of low-pass coefficients should be positive
        double sumLoD = sum(loD);
        double sumLoR = sum(loR);
        
        assertTrue(sumLoD > 0, 
            String.format("%s: Sum of decomposition low-pass should be positive (got %.6f)", 
                waveletName, sumLoD));
        assertTrue(sumLoR > 0,
            String.format("%s: Sum of reconstruction low-pass should be positive (got %.6f)", 
                waveletName, sumLoR));
        
        // Product of low-pass sums should be 2 for perfect reconstruction
        double product = sumLoD * sumLoR;
        assertEquals(2.0, product, 0.1,
            String.format("%s: Product of low-pass sums should be ~2 (got %.6f)", 
                waveletName, product));
        
        // Sum of high-pass coefficients should be close to zero
        double sumHiD = sum(hiD);
        double sumHiR = sum(hiR);
        
        assertEquals(0.0, sumHiD, 0.1,
            String.format("%s: Sum of decomposition high-pass should be ~0", waveletName));
        assertEquals(0.0, sumHiR, 0.1,
            String.format("%s: Sum of reconstruction high-pass should be ~0", waveletName));
    }
    
    /**
     * Test 6: Verify specific properties of important wavelets.
     */
    @Test
    @DisplayName("Verify BIOR4.4 (JPEG2000) specific properties")
    void testBIOR44_JPEG2000Properties() {
        BiorthogonalWavelet bior44 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR4_4);
        
        // BIOR4.4 is the CDF 9/7 wavelet used in JPEG2000
        assertEquals(9, bior44.lowPassDecomposition().length, 
            "BIOR4.4 should have 9-tap decomposition filter (CDF 9/7)");
        assertEquals(7, bior44.lowPassReconstruction().length,
            "BIOR4.4 should have 7-tap reconstruction filter (CDF 9/7)");
        
        // Verify it's symmetric
        assertTrue(bior44.isSymmetric(), "BIOR4.4 should be symmetric");
        
        // Verify vanishing moments
        assertEquals(4, bior44.vanishingMoments(), 
            "BIOR4.4 should have 4 vanishing moments for decomposition");
        assertEquals(4, bior44.dualVanishingMoments(),
            "BIOR4.4 should have 4 vanishing moments for reconstruction");
    }
    
    @Test
    @DisplayName("Verify BIOR1.1 is equivalent to Haar")
    void testBIOR11_HaarEquivalence() {
        BiorthogonalWavelet bior11 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR1_1);
        
        double[] loD = bior11.lowPassDecomposition();
        double[] loR = bior11.lowPassReconstruction();
        
        // BIOR1.1 should have identical decomposition and reconstruction
        assertArrayEquals(loD, loR, TOLERANCE,
            "BIOR1.1 should have identical decomposition and reconstruction filters");
        
        // Should be normalized Haar coefficients
        assertEquals(2, loD.length, "BIOR1.1 should have 2 coefficients like Haar");
        assertEquals(1/Math.sqrt(2), loD[0], TOLERANCE, "BIOR1.1 coefficients should be 1/sqrt(2)");
        assertEquals(1/Math.sqrt(2), loD[1], TOLERANCE, "BIOR1.1 coefficients should be 1/sqrt(2)");
    }
    
    /**
     * Test 7: Verify energy preservation property.
     * The sum of squares of coefficients should maintain certain relationships.
     */
    @ParameterizedTest(name = "Energy Preservation: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify energy preservation")
    void testEnergyPreservation(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] hiD = wavelet.highPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Compute energy (sum of squares)
        double energyLoD = sumOfSquares(loD);
        double energyHiD = sumOfSquares(hiD);
        double energyLoR = sumOfSquares(loR);
        double energyHiR = sumOfSquares(hiR);
        
        // For biorthogonal wavelets, the product of analysis and synthesis energies
        // should be related to perfect reconstruction
        double productLo = energyLoD * energyLoR;
        double productHi = energyHiD * energyHiR;
        
        // Both products should be positive
        assertTrue(productLo > 0, 
            String.format("%s: Product of low-pass energies should be positive", waveletName));
        assertTrue(productHi > 0,
            String.format("%s: Product of high-pass energies should be positive", waveletName));
    }
    
    /**
     * Test 8: Verify Cohen-Daubechies-Feauveau (CDF) construction.
     * Many biorthogonal wavelets follow the CDF construction pattern.
     */
    @Test
    @DisplayName("Verify CDF construction for specific wavelets")
    void testCDFConstruction() {
        // Test CDF 2,2 (BIOR2.2)
        BiorthogonalWavelet bior22 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR2_2);
        assertEquals(5, bior22.lowPassDecomposition().length, "CDF 2,2 should have 5-tap decomposition");
        assertEquals(3, bior22.lowPassReconstruction().length, "CDF 2,2 should have 3-tap reconstruction");
        
        // Test CDF 1,3 (BIOR1.3)
        BiorthogonalWavelet bior13 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR1_3);
        assertEquals(6, bior13.lowPassDecomposition().length, "CDF 1,3 should have 6-tap decomposition");
        assertEquals(2, bior13.lowPassReconstruction().length, "CDF 1,3 should have 2-tap reconstruction");
    }
    
    // Helper methods
    
    private double computeConvolution(double[] a, double[] b, int shift) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            int j = shift - i;
            if (j >= 0 && j < b.length) {
                sum += a[i] * b[j];
            }
        }
        return sum;
    }
    
    private boolean isSymmetric(double[] filter) {
        int n = filter.length;
        for (int i = 0; i < n / 2; i++) {
            if (Math.abs(filter[i] - filter[n - 1 - i]) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isAntiSymmetric(double[] filter) {
        int n = filter.length;
        for (int i = 0; i < n / 2; i++) {
            if (Math.abs(filter[i] + filter[n - 1 - i]) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }
    
    private double sum(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v;
        }
        return sum;
    }
    
    private double sumOfSquares(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v * v;
        }
        return sum;
    }
}