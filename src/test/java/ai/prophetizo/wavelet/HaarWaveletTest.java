package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import ai.prophetizo.wavelet.test.WaveletAssertions;
import ai.prophetizo.wavelet.test.WaveletTestUtils;
import ai.prophetizo.wavelet.util.ToleranceConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Haar wavelet implementation.
 * 
 * <p>Tests include:</p>
 * <ul>
 *   <li>Filter coefficient validation</li>
 *   <li>Mathematical properties (orthogonality, normalization)</li>
 *   <li>Perfect reconstruction</li>
 *   <li>Known transform outputs</li>
 *   <li>Edge cases and boundary conditions</li>
 * </ul>
 */
@DisplayName("Haar Wavelet Tests")
class HaarWaveletTest extends BaseWaveletTest {
    
    private final Haar haar = new Haar();
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logTestCompletion(testInfo);
    }
    
    // === Filter Coefficient Tests ===
    
    @Test
    @DisplayName("Haar filter coefficients should have correct values")
    void testFilterCoefficients() {
        // Expected Haar filter values
        double expectedValue = 1.0 / Math.sqrt(2.0);
        
        // Low-pass decomposition filter
        double[] lowPass = haar.lowPassDecomposition();
        assertEquals(2, lowPass.length, "Low-pass filter should have 2 coefficients");
        assertEquals(expectedValue, lowPass[0], 1e-15);
        assertEquals(expectedValue, lowPass[1], 1e-15);
        
        // High-pass decomposition filter
        double[] highPass = haar.highPassDecomposition();
        assertEquals(2, highPass.length, "High-pass filter should have 2 coefficients");
        assertEquals(expectedValue, highPass[0], 1e-15);
        assertEquals(-expectedValue, highPass[1], 1e-15);
        
        // Reconstruction filters (same as decomposition for Haar)
        assertArrayEquals(lowPass, haar.lowPassReconstruction());
        assertArrayEquals(highPass, haar.highPassReconstruction());
    }
    
    @Test
    @DisplayName("Haar filters should be normalized")
    void testFilterNormalization() {
        WaveletAssertions.assertFilterNormalized(
            haar.lowPassDecomposition(), 
            ToleranceConstants.ORTHOGONALITY_TOLERANCE,
            "Haar low-pass");
        
        WaveletAssertions.assertFilterNormalized(
            haar.highPassDecomposition(),
            ToleranceConstants.ORTHOGONALITY_TOLERANCE,
            "Haar high-pass");
    }
    
    @Test
    @DisplayName("Haar filters should be orthogonal")
    void testFilterOrthogonality() {
        WaveletAssertions.assertFiltersOrthogonal(
            haar.lowPassDecomposition(),
            haar.highPassDecomposition(),
            ToleranceConstants.ORTHOGONALITY_TOLERANCE);
    }
    
    @Test
    @DisplayName("Haar wavelet should have 1 vanishing moment")
    void testVanishingMoments() {
        // Haar wavelet has 1 vanishing moment
        // This means it can exactly represent constant signals
        WaveletAssertions.assertVanishingMoments(haar, 1, 1e-14);
    }
    
    // === Transform Tests with Known Outputs ===
    
    @Test
    @DisplayName("Haar transform of constant signal")
    void testConstantSignal() {
        double[] signal = {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
        WaveletTransform transform = createTransform(haar);
        
        TransformResult result = transform.forward(signal);
        
        // For constant signal, all detail coefficients should be zero
        double[] details = result.detailCoeffs();
        for (int i = 0; i < details.length; i++) {
            assertEquals(0.0, details[i], 1e-10,
                "Detail coefficient " + i + " should be zero for constant signal");
        }
        
        // Approximation coefficients should all equal constant * sqrt(2)
        double expectedApprox = 5.0 * Math.sqrt(2.0);
        double[] approx = result.approximationCoeffs();
        for (int i = 0; i < approx.length; i++) {
            assertEquals(expectedApprox, approx[i], 1e-10,
                "Approximation coefficient " + i + " incorrect");
        }
    }
    
    @Test
    @DisplayName("Haar transform of simple test signal")
    void testSimpleSignal() {
        // Simple signal with known transform
        double[] signal = {1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0};
        WaveletTransform transform = createTransform(haar);
        
        TransformResult result = transform.forward(signal);
        
        // Expected values calculated by hand
        double sqrt2 = Math.sqrt(2.0);
        double[] expectedApprox = {sqrt2, 2*sqrt2, 3*sqrt2, 4*sqrt2};
        double[] expectedDetail = {0.0, 0.0, 0.0, 0.0};
        
        WaveletAssertions.assertArraysEqualWithTolerance(
            expectedApprox, result.approximationCoeffs(), 1e-10,
            "Approximation coefficients");
        
        WaveletAssertions.assertArraysEqualWithTolerance(
            expectedDetail, result.detailCoeffs(), 1e-10,
            "Detail coefficients");
    }
    
    @Test
    @DisplayName("Haar transform of alternating signal")
    void testAlternatingSignal() {
        double[] signal = {1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0};
        WaveletTransform transform = createTransform(haar);
        
        TransformResult result = transform.forward(signal);
        
        // For alternating signal, approximation should be zero
        double[] approx = result.approximationCoeffs();
        for (double value : approx) {
            assertEquals(0.0, value, 1e-10,
                "Approximation should be zero for alternating signal");
        }
        
        // Detail coefficients capture the alternation
        double expectedDetail = Math.sqrt(2.0);
        double[] details = result.detailCoeffs();
        for (double value : details) {
            assertEquals(expectedDetail, Math.abs(value), 1e-10,
                "Detail coefficient magnitude incorrect");
        }
    }
    
    // === Perfect Reconstruction Tests ===
    
    @ParameterizedTest
    @DisplayName("Perfect reconstruction for various signal types")
    @ValueSource(strings = {"CONSTANT", "LINEAR", "SINE", "STEP", "RANDOM"})
    void testPerfectReconstruction(String signalTypeName) {
        SignalType signalType = SignalType.valueOf(signalTypeName);
        double[] signal = generateTestSignal(signalType, 64);
        
        testBothImplementations(haar, (transform, implType) -> {
            assertTrue(
                WaveletTestUtils.verifyPerfectReconstruction(
                    transform, signal, ToleranceConstants.DEFAULT_TOLERANCE),
                "Perfect reconstruction failed for " + signalType + 
                " with " + implType + " implementation");
        });
    }
    
    @Test
    @DisplayName("Perfect reconstruction with small signals")
    void testPerfectReconstructionSmallSignals() {
        // Test minimum signal size (2 elements)
        double[] twoElementSignal = {3.14, 2.71};
        WaveletTransform transform = createTransform(haar);
        
        TransformResult result = transform.forward(twoElementSignal);
        double[] reconstructed = transform.inverse(result);
        
        WaveletAssertions.assertPerfectReconstruction(
            twoElementSignal, reconstructed, ToleranceConstants.DEFAULT_TOLERANCE);
    }
    
    // === Energy Preservation Tests ===
    
    @Test
    @DisplayName("Energy preservation (Parseval's theorem)")
    void testEnergyPreservation() {
        double[] signal = WaveletTestUtils.generateCompositeSignal(
            128, 
            new double[]{0.1, 0.2, 0.3}, 
            new double[]{1.0, 0.5, 0.25});
        
        testBothImplementations(haar, (transform, implType) -> {
            TransformResult result = transform.forward(signal);
            WaveletAssertions.assertEnergyPreserved(
                signal, result, ToleranceConstants.ENERGY_TOLERANCE);
        });
    }
    
    // === Edge Cases and Boundary Tests ===
    
    @Test
    @DisplayName("Transform of zero signal")
    void testZeroSignal() {
        double[] signal = new double[16]; // All zeros
        WaveletTransform transform = createTransform(haar);
        
        TransformResult result = transform.forward(signal);
        
        // All coefficients should be zero
        for (double coeff : result.approximationCoeffs()) {
            assertEquals(0.0, coeff, 1e-15);
        }
        for (double coeff : result.detailCoeffs()) {
            assertEquals(0.0, coeff, 1e-15);
        }
    }
    
    @Test
    @DisplayName("Transform with single spike")
    void testSingleSpike() {
        double[] signal = new double[8];
        signal[3] = 1.0; // Single spike at position 3
        
        WaveletTransform transform = createTransform(haar);
        TransformResult result = transform.forward(signal);
        
        // Verify energy is preserved
        WaveletAssertions.assertEnergyPreserved(
            signal, result, ToleranceConstants.ENERGY_TOLERANCE);
        
        // Verify perfect reconstruction
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, ToleranceConstants.DEFAULT_TOLERANCE);
    }
    
    // === Implementation Comparison Tests ===
    
    @Test
    @DisplayName("Scalar and vector implementations should produce identical results")
    void testImplementationConsistency() {
        double[] signal = WaveletTestUtils.generateRandomSignal(256, 12345L, -10.0, 10.0);
        
        // Get results from both implementations
        WaveletTransform scalarTransform = createScalarTransform(haar);
        WaveletTransform vectorTransform = createTransform(haar); // Auto-selects vector if available
        
        TransformResult scalarResult = scalarTransform.forward(signal);
        TransformResult vectorResult = vectorTransform.forward(signal);
        
        // Results should be identical
        WaveletAssertions.assertArraysEqualWithTolerance(
            scalarResult.approximationCoeffs(),
            vectorResult.approximationCoeffs(),
            1e-12,
            "Approximation coefficients differ between implementations");
        
        WaveletAssertions.assertArraysEqualWithTolerance(
            scalarResult.detailCoeffs(),
            vectorResult.detailCoeffs(),
            1e-12,
            "Detail coefficients differ between implementations");
    }
    
    // === Wavelet Properties Tests ===
    
    @Test
    @DisplayName("Haar wavelet name and properties")
    void testWaveletProperties() {
        assertEquals("Haar", haar.name());
        assertEquals(2, haar.lowPassDecomposition().length, "Haar wavelet should have length 2");
    }
    
    @Test
    @DisplayName("Haar coefficients satisfy mathematical properties")
    void testCoefficientVerification() {
        assertTrue(haar.verifyCoefficients(), 
            "Haar coefficients should satisfy all mathematical properties");
    }
}