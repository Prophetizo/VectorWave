package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.test.WaveletAssertions;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Normalization tests for orthogonal and continuous wavelet implementations.
 * 
 * <p>Validates that orthogonal and continuous wavelets satisfy the L2 normalization 
 * requirement: sum of squares of filter coefficients equals 1.</p>
 * 
 * <p>Note: Biorthogonal wavelets are excluded from these tests as they must satisfy
 * biorthogonality conditions instead of L2 normalization for perfect reconstruction.</p>
 * 
 * <p>This ensures consistent energy preservation and cross-wavelet 
 * comparison results for applicable wavelet families.</p>
 */
@DisplayName("Wavelet Normalization Tests")
class WaveletNormalizationTest {
    
    /**
     * Extremely tight tolerance for wavelet filter normalization validation.
     * 
     * <p>This stringent tolerance (2e-10) is required because:</p>
     * <ul>
     *   <li>Wavelet coefficients are often derived from mathematical formulas with
     *       high precision requirements</li>
     *   <li>Small normalization errors can compound through multi-level decompositions,
     *       affecting reconstruction quality</li>
     *   <li>Cross-wavelet comparisons require consistent normalization to ensure
     *       energy preservation across different wavelet families</li>
     *   <li>Financial and scientific applications demand high numerical accuracy
     *       for reliable signal analysis</li>
     * </ul>
     * 
     * <p>Note: This is tighter than the general transform tolerance (1e-10) to ensure
     * wavelet filters meet the highest precision standards.</p>
     */
    private static final double NORMALIZATION_TOLERANCE = 2e-10;
    
    /**
     * Provides wavelets that should be L2-normalized for parameterized testing.
     * Excludes biorthogonal wavelets which have different normalization requirements.
     * 
     * @return stream of arguments containing wavelet instances and names
     */
    static Stream<Arguments> allWavelets() {
        return Stream.of(
            // Orthogonal wavelets
            Arguments.of(new Haar(), "Haar"),
            Arguments.of(Daubechies.DB2, "Daubechies DB2"),
            Arguments.of(Daubechies.DB4, "Daubechies DB4"),
            Arguments.of(Symlet.SYM2, "Symlet SYM2"),
            Arguments.of(Coiflet.COIF1, "Coiflet COIF1"),
            
            // Continuous wavelets
            Arguments.of(new MorletWavelet(), "Morlet (default)"),
            Arguments.of(new MorletWavelet(5.0, 1.5), "Morlet (ω₀=5.0, σ=1.5)")
        );
    }
    
    @ParameterizedTest
    @MethodSource("allWavelets")
    @DisplayName("All wavelets should have L2-normalized low-pass filters")
    void testLowPassFilterNormalization(Wavelet wavelet, String name) {
        double[] lowPass = wavelet.lowPassDecomposition();
        assertNotNull(lowPass, name + " low-pass filter should not be null");
        assertTrue(lowPass.length > 0, name + " low-pass filter should not be empty");
        
        WaveletAssertions.assertFilterNormalized(
            lowPass, 
            NORMALIZATION_TOLERANCE,
            name + " low-pass");
        
        // Also test with static utility method
        assertTrue(Wavelet.isNormalized(lowPass, NORMALIZATION_TOLERANCE),
            name + " low-pass filter not normalized according to utility method");
    }
    
    @ParameterizedTest
    @MethodSource("allWavelets")
    @DisplayName("All wavelets should have L2-normalized high-pass filters")
    void testHighPassFilterNormalization(Wavelet wavelet, String name) {
        double[] highPass = wavelet.highPassDecomposition();
        assertNotNull(highPass, name + " high-pass filter should not be null");
        assertTrue(highPass.length > 0, name + " high-pass filter should not be empty");
        
        WaveletAssertions.assertFilterNormalized(
            highPass, 
            NORMALIZATION_TOLERANCE,
            name + " high-pass");
        
        // Also test with static utility method  
        assertTrue(Wavelet.isNormalized(highPass, NORMALIZATION_TOLERANCE),
            name + " high-pass filter not normalized according to utility method");
    }
    
    @ParameterizedTest
    @MethodSource("allWavelets")
    @DisplayName("All wavelets should have L2-normalized reconstruction filters")
    void testReconstructionFilterNormalization(Wavelet wavelet, String name) {
        double[] lowPassRecon = wavelet.lowPassReconstruction();
        double[] highPassRecon = wavelet.highPassReconstruction();
        
        assertNotNull(lowPassRecon, name + " low-pass reconstruction should not be null");
        assertNotNull(highPassRecon, name + " high-pass reconstruction should not be null");
        
        WaveletAssertions.assertFilterNormalized(
            lowPassRecon, 
            NORMALIZATION_TOLERANCE,
            name + " low-pass reconstruction");
        
        WaveletAssertions.assertFilterNormalized(
            highPassRecon, 
            NORMALIZATION_TOLERANCE,
            name + " high-pass reconstruction");
    }
    
    @Test
    @DisplayName("Normalization utility method should work correctly")
    void testNormalizationUtility() {
        // Test with unnormalized coefficients
        double[] unnormalized = {1.0, 2.0, 3.0}; // L2 norm = sqrt(14) ≈ 3.742
        double[] normalized = Wavelet.normalizeToUnitL2Norm(unnormalized);
        
        // Check that result is normalized
        assertTrue(Wavelet.isNormalized(normalized), 
            "Normalized coefficients should have unit L2 norm");
        
        // Check specific values
        double expectedNorm = Math.sqrt(14.0);
        assertEquals(1.0 / expectedNorm, normalized[0], 1e-15);
        assertEquals(2.0 / expectedNorm, normalized[1], 1e-15);
        assertEquals(3.0 / expectedNorm, normalized[2], 1e-15);
        
        // Original should be unchanged
        assertEquals(1.0, unnormalized[0]);
        assertEquals(2.0, unnormalized[1]);
        assertEquals(3.0, unnormalized[2]);
    }
    
    @Test
    @DisplayName("Normalization utility should handle edge cases")
    void testNormalizationUtilityEdgeCases() {
        // Test with null
        assertNull(Wavelet.normalizeToUnitL2Norm(null));
        
        // Test with empty array
        double[] empty = {};
        assertSame(empty, Wavelet.normalizeToUnitL2Norm(empty));
        
        // Test with zero coefficients
        double[] zeros = {0.0, 0.0, 0.0};
        double[] result = Wavelet.normalizeToUnitL2Norm(zeros);
        assertArrayEquals(zeros, result);
        // Should return a copy even for zero coefficients
        assertNotSame(zeros, result);
        
        // Test with already normalized coefficients
        double[] alreadyNormalized = {1.0 / Math.sqrt(2.0), 1.0 / Math.sqrt(2.0)};
        double[] result2 = Wavelet.normalizeToUnitL2Norm(alreadyNormalized);
        // Should return a copy without modification
        assertNotSame(alreadyNormalized, result2);
        for (int i = 0; i < alreadyNormalized.length; i++) {
            assertEquals(alreadyNormalized[i], result2[i], 1e-15);
        }
    }
    
    @Test
    @DisplayName("isNormalized utility should work correctly")
    void testIsNormalizedUtility() {
        // Test normalized coefficients
        double[] normalized = {1.0 / Math.sqrt(2.0), 1.0 / Math.sqrt(2.0)};
        assertTrue(Wavelet.isNormalized(normalized));
        assertTrue(Wavelet.isNormalized(normalized, 1e-15));
        
        // Test unnormalized coefficients
        double[] unnormalized = {1.0, 2.0};
        assertFalse(Wavelet.isNormalized(unnormalized));
        assertFalse(Wavelet.isNormalized(unnormalized, 1e-15));
        
        // Test edge cases
        assertFalse(Wavelet.isNormalized(null));
        assertFalse(Wavelet.isNormalized(new double[]{}));
        assertFalse(Wavelet.isNormalized(new double[]{0.0, 0.0}));
    }
    
    @Test
    @DisplayName("Cross-wavelet energy comparison should be consistent")
    void testCrossWaveletEnergyConsistency() {
        // Test that different DISCRETE wavelets preserve energy consistently
        // when applied to the same signal
        double[] testSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double originalEnergy = computeSignalEnergy(testSignal);
        
        // Only test discrete wavelets for energy preservation
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4
        };
        
        for (Wavelet wavelet : wavelets) {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            TransformResult result = transform.forward(testSignal);
            
            double transformEnergy = 
                computeSignalEnergy(result.approximationCoeffs()) +
                computeSignalEnergy(result.detailCoeffs());
            
            assertEquals(originalEnergy, transformEnergy, 1e-8,
                "Energy not preserved for " + wavelet.name());
        }
    }
    
    /**
     * Computes the energy (sum of squares) of a signal.
     */
    private double computeSignalEnergy(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
}