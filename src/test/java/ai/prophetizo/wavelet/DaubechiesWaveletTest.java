package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import ai.prophetizo.wavelet.test.WaveletAssertions;
import ai.prophetizo.wavelet.test.WaveletTestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Daubechies wavelet implementations.
 * 
 * <p>Tests DB2 and DB4 wavelets for:</p>
 * <ul>
 *   <li>Filter coefficient properties</li>
 *   <li>Mathematical correctness</li>
 *   <li>Perfect reconstruction</li>
 *   <li>Vanishing moments</li>
 *   <li>Implementation consistency</li>
 * </ul>
 */
@DisplayName("Daubechies Wavelets Tests")
class DaubechiesWaveletTest extends BaseWaveletTest {
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logTestCompletion(testInfo);
    }
    
    // Provide all Daubechies wavelets for parameterized tests
    static Stream<Arguments> daubechiesWavelets() {
        return Stream.of(
            Arguments.of(Daubechies.DB2, "DB2", 4, 2),
            Arguments.of(Daubechies.DB4, "DB4", 8, 4)
        );
    }
    
    // === Filter Properties Tests ===
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Filter coefficients should be normalized")
    void testFilterNormalization(Daubechies wavelet, String name, int length, int vanishingMoments) {
        WaveletAssertions.assertFilterNormalized(
            wavelet.lowPassDecomposition(),
            2e-12, // Slightly relaxed tolerance for Daubechies
            name + " low-pass");
        
        WaveletAssertions.assertFilterNormalized(
            wavelet.highPassDecomposition(),
            2e-12,
            name + " high-pass");
    }
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Decomposition and reconstruction filters should be orthogonal")
    void testFilterOrthogonality(Daubechies wavelet, String name, int length, int vanishingMoments) {
        WaveletAssertions.assertFiltersOrthogonal(
            wavelet.lowPassDecomposition(),
            wavelet.highPassDecomposition(),
            1e-12);
    }
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Filter length should match expected values")
    void testFilterLength(Daubechies wavelet, String name, int expectedLength, int vanishingMoments) {
        assertEquals(expectedLength, wavelet.lowPassDecomposition().length,
            "Low-pass decomposition filter length for " + name);
        assertEquals(expectedLength, wavelet.highPassDecomposition().length,
            "High-pass decomposition filter length for " + name);
        assertEquals(expectedLength, wavelet.lowPassReconstruction().length,
            "Low-pass reconstruction filter length for " + name);
        assertEquals(expectedLength, wavelet.highPassReconstruction().length,
            "High-pass reconstruction filter length for " + name);
    }
    
    // === Vanishing Moments Tests ===
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Daubechies wavelets should have correct vanishing moments")
    void testVanishingMoments(Daubechies wavelet, String name, int length, int expectedMoments) {
        // Daubechies DB_n has n vanishing moments
        // Note: numerical precision limits accuracy for higher moments
        double tolerance = expectedMoments > 3 ? 2e-9 : 5e-10;
        WaveletAssertions.assertVanishingMoments(
            wavelet, expectedMoments, tolerance);
    }
    
    // === Perfect Reconstruction Tests ===
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Perfect reconstruction for all Daubechies wavelets")
    void testPerfectReconstruction(Daubechies wavelet, String name, int length, int vanishingMoments) {
        double[] signal = WaveletTestUtils.generateCompositeSignal(
            128, 
            new double[]{0.05, 0.15, 0.25}, 
            new double[]{1.0, 0.7, 0.4});
        
        testBothImplementations(wavelet, (transform, implType) -> {
            assertTrue(
                WaveletTestUtils.verifyPerfectReconstruction(
                    transform, signal, WaveletTestUtils.DEFAULT_TOLERANCE),
                name + " perfect reconstruction failed with " + 
                implType + " implementation");
        });
    }
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Perfect reconstruction with minimum signal size")
    void testPerfectReconstructionMinimumSize(Daubechies wavelet, String name, int filterLength, int vanishingMoments) {
        // Minimum signal size is next power of 2 after filter length
        int minSize = Integer.highestOneBit(filterLength) << 1;
        double[] signal = WaveletTestUtils.generateRandomSignal(
            minSize, 42L, -1.0, 1.0);
        
        WaveletTransform transform = createTransform(wavelet);
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, WaveletTestUtils.DEFAULT_TOLERANCE);
    }
    
    // === Energy Preservation Tests ===
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Energy preservation for all Daubechies wavelets")
    void testEnergyPreservation(Daubechies wavelet, String name, int length, int vanishingMoments) {
        double[] signal = generateTestSignal(SignalType.RANDOM, 256);
        
        WaveletTransform transform = createTransform(wavelet);
        TransformResult result = transform.forward(signal);
        
        WaveletAssertions.assertEnergyPreserved(
            signal, result, 2e-10); // Slightly relaxed for Daubechies
    }
    
    // === Polynomial Signal Tests ===
    
    @Test
    @DisplayName("DB2 should handle linear signals exactly")
    void testDB2LinearSignal() {
        // DB2 has 2 vanishing moments, so can represent linear signals exactly
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i; // Linear signal
        }
        
        WaveletTransform transform = createTransform(Daubechies.DB2);
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // Should reconstruct perfectly
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, 1e-10);
    }
    
    @Test
    @DisplayName("DB4 should handle cubic signals well")
    void testDB4CubicSignal() {
        // DB4 has 4 vanishing moments, so can represent cubic signals well
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            double x = i / 64.0;
            signal[i] = x * x * x; // Cubic signal
        }
        
        WaveletTransform transform = createTransform(Daubechies.DB4);
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // Should reconstruct with high accuracy
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, 1e-9);
    }
    
    // === Specific Coefficient Tests ===
    
    @Test
    @DisplayName("DB2 coefficients should match known values")
    void testDB2Coefficients() {
        double[] lowPass = Daubechies.DB2.lowPassDecomposition();
        
        // Known DB2 coefficients (approximate values)
        double[] expected = {
            (1 + Math.sqrt(3)) / (4 * Math.sqrt(2)),
            (3 + Math.sqrt(3)) / (4 * Math.sqrt(2)),
            (3 - Math.sqrt(3)) / (4 * Math.sqrt(2)),
            (1 - Math.sqrt(3)) / (4 * Math.sqrt(2))
        };
        
        assertEquals(4, lowPass.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i], lowPass[i], 1e-14,
                "DB2 coefficient " + i + " mismatch");
        }
    }
    
    @Test
    @DisplayName("DB4 filter should have correct length and properties")
    void testDB4Properties() {
        Daubechies db4 = Daubechies.DB4;
        
        assertEquals("db4", db4.name());
        assertEquals(8, db4.lowPassDecomposition().length);
        
        // Verify filter sums
        double[] lowPass = db4.lowPassDecomposition();
        double sum = 0;
        for (double coeff : lowPass) {
            sum += coeff;
        }
        assertEquals(Math.sqrt(2.0), sum, 2e-10,
            "Sum of low-pass coefficients should be sqrt(2)");
    }
    
    // === Implementation Consistency Tests ===
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Scalar and vector implementations should match")
    void testImplementationConsistency(Daubechies wavelet, String name, int length, int vanishingMoments) {
        double[] signal = WaveletTestUtils.generateRandomSignal(
            256, 54321L, -5.0, 5.0);
        
        WaveletTransform scalarTransform = createScalarTransform(wavelet);
        WaveletTransform vectorTransform = createTransform(wavelet);
        
        TransformResult scalarResult = scalarTransform.forward(signal);
        TransformResult vectorResult = vectorTransform.forward(signal);
        
        // Allow slightly more tolerance for longer filters
        double tolerance = length > 6 ? 1e-11 : 1e-12;
        
        WaveletAssertions.assertArraysEqualWithTolerance(
            scalarResult.approximationCoeffs(),
            vectorResult.approximationCoeffs(),
            tolerance,
            name + " approximation coefficients differ");
        
        WaveletAssertions.assertArraysEqualWithTolerance(
            scalarResult.detailCoeffs(),
            vectorResult.detailCoeffs(),
            tolerance,
            name + " detail coefficients differ");
    }
    
    // === Edge Case Tests ===
    
    @Test
    @DisplayName("Transform of constant signal for DB2")
    void testDB2ConstantSignal() {
        testConstantSignalForWavelet(Daubechies.DB2, "DB2");
    }
    
    @Test
    @DisplayName("Transform of constant signal for DB4")
    void testDB4ConstantSignal() {
        testConstantSignalForWavelet(Daubechies.DB4, "DB4");
    }
    
    private void testConstantSignalForWavelet(Daubechies wavelet, String name) {
        double constantValue = 7.5;
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = constantValue;
        }
        
        WaveletTransform transform = createTransform(wavelet);
        TransformResult result = transform.forward(signal);
        
        // For constant signal, detail coefficients should be near zero
        double[] details = result.detailCoeffs();
        for (int i = 0; i < details.length; i++) {
            assertEquals(0.0, details[i], 2e-10,
                name + " detail[" + i + "] should be ~0 for constant signal");
        }
        
        // Perfect reconstruction should still work
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, 3e-10); // Slightly relaxed for constant signals
    }
    
    @ParameterizedTest
    @MethodSource("daubechiesWavelets")
    @DisplayName("Transform of step function")
    void testStepFunction(Daubechies wavelet, String name, int length, int vanishingMoments) {
        double[] signal = WaveletTestUtils.generateStepFunction(
            64, 32, 1.0, 5.0);
        
        testBothImplementations(wavelet, (transform, implType) -> {
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Step functions are challenging for wavelets
            // Allow slightly more tolerance
            double tolerance = length > 6 ? 1e-9 : 1e-10;
            
            WaveletAssertions.assertArraysEqualWithTolerance(
                signal, reconstructed, tolerance,
                name + " step function reconstruction with " + implType);
        });
    }
    
    @Test
    @DisplayName("DB2 coefficients satisfy mathematical properties")
    void testDB2CoefficientVerification() {
        assertTrue(Daubechies.DB2.verifyCoefficients(), 
            "DB2 coefficients should satisfy all mathematical properties");
    }
    
    @Test
    @DisplayName("DB4 coefficients satisfy mathematical properties")
    void testDB4CoefficientVerification() {
        assertTrue(Daubechies.DB4.verifyCoefficients(), 
            "DB4 coefficients should satisfy all mathematical properties");
    }
}