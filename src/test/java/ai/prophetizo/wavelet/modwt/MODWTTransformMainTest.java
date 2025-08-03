package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.modwt.test.BaseMODWTTest;
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
 * Unit tests for the MODWTTransform class.
 * 
 * <p>Tests focus on:</p>
 * <ul>
 *   <li>Input validation</li>
 *   <li>Error handling</li>
 *   <li>Boundary conditions</li>
 *   <li>Transform correctness</li>
 *   <li>Shift-invariant properties</li>
 *   <li>Arbitrary length signal support</li>
 * </ul>
 * 
 * @since 3.0.0
 */
@DisplayName("MODWTTransform Tests")
class MODWTTransformMainTest extends BaseMODWTTest {
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logTestCompletion(testInfo);
    }
    
    // === Constructor Tests ===
    
    @Test
    @DisplayName("Constructor should reject null wavelet")
    void testConstructorNullWavelet() {
        assertThrows(NullPointerException.class, 
            () -> new MODWTTransform(null, BoundaryMode.PERIODIC),
            "Should throw NullPointerException for null wavelet");
    }
    
    @Test
    @DisplayName("Constructor should reject null boundary mode")
    void testConstructorNullBoundaryMode() {
        assertThrows(NullPointerException.class,
            () -> new MODWTTransform(new Haar(), null),
            "Should throw NullPointerException for null boundary mode");
    }
    
    @Test
    @DisplayName("Constructor should reject non-periodic boundary mode")
    void testConstructorNonPeriodicBoundaryMode() {
        assertThrows(IllegalArgumentException.class,
            () -> new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING),
            "Should throw IllegalArgumentException for non-periodic boundary mode");
    }
    
    // === Forward Transform Validation Tests ===
    
    @Test
    @DisplayName("Forward transform should reject null signal")
    void testForwardNullSignal() {
        MODWTTransform transform = createTransform(new Haar());
        
        assertThrows(NullPointerException.class,
            () -> transform.forward(null),
            "Should throw NullPointerException for null signal");
    }
    
    @Test
    @DisplayName("Forward transform should reject empty signal")
    void testForwardEmptySignal() {
        MODWTTransform transform = createTransform(new Haar());
        double[] emptySignal = new double[0];
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(emptySignal),
            "Should throw InvalidSignalException for empty signal");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 15, 17, 31, 33, 63, 65, 100, 1000})
    @DisplayName("Forward transform should accept non-power-of-2 lengths")
    void testForwardNonPowerOfTwoLength(int length) {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = WaveletTestUtils.generateSineWave(length, 0.1, 1.0, 0.0);
        
        // Should NOT throw exception for non-power-of-2
        MODWTResult result = assertDoesNotThrow(() -> transform.forward(signal));
        
        assertNotNull(result);
        assertEquals(length, result.getSignalLength());
        assertEquals(length, result.approximationCoeffs().length);
        assertEquals(length, result.detailCoeffs().length);
    }
    
    @Test
    @DisplayName("Forward transform should reject signal with NaN")
    void testForwardSignalWithNaN() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = new double[16];
        signal[5] = Double.NaN;
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should throw InvalidSignalException for signal containing NaN");
    }
    
    @Test
    @DisplayName("Forward transform should reject signal with infinity")
    void testForwardSignalWithInfinity() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = new double[16];
        signal[7] = Double.POSITIVE_INFINITY;
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should throw InvalidSignalException for signal containing infinity");
    }
    
    // === Shift-Invariance Tests (MODWT specific) ===
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("MODWT should be shift-invariant")
    void testShiftInvariance(int shift) {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = WaveletTestUtils.generateSineWave(64, 0.1, 1.0, 0.0);
        
        boolean isShiftInvariant = testShiftInvariance(transform, signal, shift, 1e-10);
        assertTrue(isShiftInvariant, 
            "MODWT should be shift-invariant for shift=" + shift);
    }
    
    // === Perfect Reconstruction Tests ===
    
    @Test
    @DisplayName("MODWT should have perfect reconstruction for Haar")
    void testPerfectReconstructionHaar() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = mediumTestSignal;
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        WaveletAssertions.assertArraysEqualWithTolerance(signal, reconstructed, 
            ToleranceConstants.DEFAULT_TOLERANCE, "Perfect reconstruction failed");
    }
    
    @Test
    @DisplayName("MODWT should have perfect reconstruction for Daubechies")
    void testPerfectReconstructionDaubechies() {
        MODWTTransform transform = createTransform(Daubechies.DB4);
        double[] signal = mediumTestSignal;
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        WaveletAssertions.assertArraysEqualWithTolerance(signal, reconstructed, 
            ToleranceConstants.DEFAULT_TOLERANCE, "Perfect reconstruction failed");
    }
    
    // === Energy Conservation Tests ===
    
    @Test
    @DisplayName("MODWT should approximately conserve energy")
    void testEnergyConservation() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = smallTestSignal;
        
        // Calculate signal energy
        double signalEnergy = 0.0;
        for (double s : signal) {
            signalEnergy += s * s;
        }
        
        // Transform and calculate coefficient energy
        MODWTResult result = transform.forward(signal);
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        double coeffEnergy = 0.0;
        for (int i = 0; i < signal.length; i++) {
            coeffEnergy += approx[i] * approx[i] + detail[i] * detail[i];
        }
        
        assertEquals(signalEnergy, coeffEnergy, signalEnergy * 0.01,
            "Energy should be approximately conserved (within 1%)");
    }
    
    // === Edge Case Tests ===
    
    @Test
    @DisplayName("MODWT should handle single-sample signal")
    void testSingleSampleSignal() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = {5.0};
        
        MODWTResult result = transform.forward(signal);
        
        assertNotNull(result);
        assertEquals(1, result.getSignalLength());
        assertEquals(1, result.approximationCoeffs().length);
        assertEquals(1, result.detailCoeffs().length);
    }
    
    @Test
    @DisplayName("MODWT should handle constant signal")
    void testConstantSignal() {
        MODWTTransform transform = createTransform(new Haar());
        double[] signal = new double[50];
        java.util.Arrays.fill(signal, 3.14);
        
        MODWTResult result = transform.forward(signal);
        double[] detail = result.detailCoeffs();
        
        // Detail coefficients should be near zero for constant signal
        for (double d : detail) {
            assertEquals(0.0, d, 1e-10, 
                "Detail coefficients should be zero for constant signal");
        }
    }
    
    // === Performance Info Tests ===
    
    @Test
    @DisplayName("Performance info should be available")
    void testPerformanceInfo() {
        MODWTTransform transform = createTransform(new Haar());
        
        var perfInfo = transform.getPerformanceInfo();
        
        assertNotNull(perfInfo);
        assertNotNull(perfInfo.description());
        assertTrue(perfInfo.description().length() > 0);
    }
    
    @Test
    @DisplayName("Processing estimate should be reasonable")
    void testProcessingEstimate() {
        MODWTTransform transform = createTransform(new Haar());
        
        var estimate = transform.estimateProcessingTime(1000);
        
        assertNotNull(estimate);
        assertEquals(1000, estimate.signalLength());
        assertTrue(estimate.estimatedTimeMs() >= 0);
        assertNotNull(estimate.description());
    }
}