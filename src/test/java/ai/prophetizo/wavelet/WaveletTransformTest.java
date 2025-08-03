package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import ai.prophetizo.wavelet.test.WaveletAssertions;
import ai.prophetizo.wavelet.test.WaveletTestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WaveletTransform class.
 * 
 * <p>Tests focus on:</p>
 * <ul>
 *   <li>Input validation</li>
 *   <li>Error handling</li>
 *   <li>Boundary conditions</li>
 *   <li>Transform correctness</li>
 *   <li>Null safety</li>
 * </ul>
 */
@DisplayName("WaveletTransform Tests")
class WaveletTransformTest extends BaseWaveletTest {
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logTestCompletion(testInfo);
    }
    
    // === Constructor Tests ===
    
    @Test
    @DisplayName("Constructor should reject null wavelet")
    void testConstructorNullWavelet() {
        assertThrows(NullPointerException.class, 
            () -> new WaveletTransform(null, BoundaryMode.PERIODIC),
            "Should throw NullPointerException for null wavelet");
    }
    
    @Test
    @DisplayName("Constructor should reject null boundary mode")
    void testConstructorNullBoundaryMode() {
        assertThrows(NullPointerException.class,
            () -> new WaveletTransform(new Haar(), null),
            "Should throw NullPointerException for null boundary mode");
    }
    
    // === Forward Transform Validation Tests ===
    
    @Test
    @DisplayName("Forward transform should reject null signal")
    void testForwardNullSignal() {
        WaveletTransform transform = createTransform(new Haar());
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(null),
            "Should throw InvalidSignalException for null signal");
    }
    
    @Test
    @DisplayName("Forward transform should reject empty signal")
    void testForwardEmptySignal() {
        WaveletTransform transform = createTransform(new Haar());
        double[] emptySignal = new double[0];
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(emptySignal),
            "Should throw InvalidSignalException for empty signal");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 15, 17, 31, 33, 63, 65})
    @DisplayName("Forward transform should work with any signal length (MODWT advantage)")
    void testForwardAnySignalLength(int length) {
        WaveletTransform transform = createTransform(new Haar());
        double[] signal = new double[length];
        
        // Fill with test data
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length);
        }
        
        // Should work without throwing an exception
        assertDoesNotThrow(() -> {
            TransformResult result = transform.forward(signal);
            // MODWT should produce same-length output
            assertEquals(length, result.approximationCoeffs().length, 
                        "Approximation coefficients should have same length as input");
            assertEquals(length, result.detailCoeffs().length,
                        "Detail coefficients should have same length as input");
        }, "MODWT should work with any signal length");
    }
    
    @Test
    @DisplayName("Forward transform should reject signal with NaN")
    void testForwardSignalWithNaN() {
        WaveletTransform transform = createTransform(new Haar());
        double[] signal = {1.0, 2.0, Double.NaN, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        InvalidSignalException exception = assertThrows(
            InvalidSignalException.class,
            () -> transform.forward(signal));
        
        assertTrue(exception.getMessage().contains("NaN"),
            "Error message should mention NaN");
        assertTrue(exception.getMessage().contains("index 2"),
            "Error message should include index of NaN");
    }
    
    @Test
    @DisplayName("Forward transform should reject signal with infinity")
    void testForwardSignalWithInfinity() {
        WaveletTransform transform = createTransform(new Haar());
        double[] signal = {1.0, 2.0, 3.0, Double.POSITIVE_INFINITY, 5.0, 6.0, 7.0, 8.0};
        
        InvalidSignalException exception = assertThrows(
            InvalidSignalException.class,
            () -> transform.forward(signal));
        
        assertTrue(exception.getMessage().contains("infinity"),
            "Error message should mention infinity");
    }
    
    // === Inverse Transform Validation Tests ===
    
    @Test
    @DisplayName("Inverse transform should reject null TransformResult")
    void testInverseNullResult() {
        WaveletTransform transform = createTransform(new Haar());
        
        assertThrows(NullPointerException.class,
            () -> transform.inverse(null),
            "Should throw NullPointerException for null TransformResult");
    }
    
    // === Boundary Mode Tests ===
    
    @Test
    @DisplayName("Transform with PERIODIC boundary mode")
    void testPeriodicBoundaryMode() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        WaveletTransform transform = createTransform(new Haar(), BoundaryMode.PERIODIC);
        
        TransformResult result = transform.forward(signal);
        WaveletAssertions.assertValidTransformResult(result);
        
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, WaveletTestUtils.DEFAULT_TOLERANCE);
    }
    
    @Test
    @DisplayName("Transform with ZERO_PADDING boundary mode")
    void testZeroPaddingBoundaryMode() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        WaveletTransform transform = createTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        TransformResult result = transform.forward(signal);
        WaveletAssertions.assertValidTransformResult(result);
        
        double[] reconstructed = transform.inverse(result);
        
        // Zero-padding does not provide perfect reconstruction due to boundary effects
        // The signal is effectively shifted and truncated
        // We verify the transform works but don't expect perfect reconstruction
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Verify no NaN or infinite values in reconstruction
        for (double value : reconstructed) {
            assertTrue(Double.isFinite(value),
                "Reconstructed signal should contain only finite values");
        }
        
        // With zero-padding, the reconstructed signal will differ from the original
        // due to boundary effects. We just verify it's not all zeros.
        boolean hasNonZeroValues = false;
        for (double value : reconstructed) {
            if (Math.abs(value) > 1e-10) {
                hasNonZeroValues = true;
                break;
            }
        }
        assertTrue(hasNonZeroValues, 
            "Reconstructed signal should have non-zero values");
    }
    
    @Test
    @DisplayName("Transform with unsupported boundary mode should fail at factory")
    void testUnsupportedBoundaryMode() {
        assertThrows(UnsupportedOperationException.class,
            () -> createTransform(new Haar(), BoundaryMode.SYMMETRIC),
            "Should throw UnsupportedOperationException for SYMMETRIC mode");
    }
    
    // === Multi-Wavelet Tests ===
    
    @ParameterizedTest
    @ValueSource(classes = {Haar.class})
    @DisplayName("Transform should work with different wavelet types")
    void testDifferentWavelets(Class<? extends Wavelet> waveletClass) throws Exception {
        Wavelet wavelet = waveletClass.getDeclaredConstructor().newInstance();
        double[] signal = generateTestSignal(SignalType.SINE, 64);
        
        WaveletTransform transform = createTransform(wavelet);
        TransformResult result = transform.forward(signal);
        
        WaveletAssertions.assertValidTransformResult(result);
        WaveletAssertions.assertEnergyPreserved(
            signal, result, WaveletTestUtils.ENERGY_TOLERANCE);
    }
    
    // === Edge Cases ===
    
    @Test
    @DisplayName("Transform of minimum size signal (2 elements)")
    void testMinimumSizeSignal() {
        double[] signal = {3.14, 2.71};
        WaveletTransform transform = createTransform(new Haar());
        
        TransformResult result = transform.forward(signal);
        
        // MODWT produces same-length output
        assertEquals(2, result.approximationCoeffs().length);
        assertEquals(2, result.detailCoeffs().length);
        
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, WaveletTestUtils.DEFAULT_TOLERANCE);
    }
    
    @Test
    @DisplayName("Transform of large signal")
    void testLargeSignal() {
        double[] signal = WaveletTestUtils.generateRandomSignal(
            4096, 99999L, -100.0, 100.0);
        
        WaveletTransform transform = createTransform(Daubechies.DB4);
        TransformResult result = transform.forward(signal);
        
        // MODWT produces same-length output
        assertEquals(4096, result.approximationCoeffs().length);
        assertEquals(4096, result.detailCoeffs().length);
        
        WaveletAssertions.assertEnergyPreserved(
            signal, result, 2e-5); // Relaxed for large signals with DB4
    }
    
    // === Transform Result Immutability ===
    
    @Test
    @DisplayName("TransformResult should return defensive copies")
    void testTransformResultImmutability() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        WaveletTransform transform = createTransform(new Haar());
        
        TransformResult result = transform.forward(signal);
        
        // Get arrays and modify them
        double[] approx1 = result.approximationCoeffs();
        double[] detail1 = result.detailCoeffs();
        approx1[0] = 999.0;
        detail1[0] = 999.0;
        
        // Get arrays again - should be unchanged
        double[] approx2 = result.approximationCoeffs();
        double[] detail2 = result.detailCoeffs();
        
        assertNotEquals(999.0, approx2[0], "Approximation should be immutable");
        assertNotEquals(999.0, detail2[0], "Detail should be immutable");
    }
    
    // === Performance Characteristics ===
    
    @Test
    @DisplayName("Transform should handle signals with extreme values")
    void testExtremeValues() {
        double[] signal = {
            1e-15, 1e15, -1e-15, -1e15,
            Double.MIN_VALUE, Double.MAX_VALUE/2, -Double.MIN_VALUE, -Double.MAX_VALUE/2
        };
        
        WaveletTransform transform = createTransform(new Haar());
        TransformResult result = transform.forward(signal);
        
        // Should not overflow or underflow
        WaveletAssertions.assertAllFinite(
            result.approximationCoeffs(), "Approximation");
        WaveletAssertions.assertAllFinite(
            result.detailCoeffs(), "Detail");
        
        // Should still preserve energy (approximately)
        // For extreme values, energy may overflow to infinity
        double signalEnergy = WaveletTestUtils.computeEnergy(signal);
        double transformEnergy = WaveletTestUtils.computeEnergy(result.approximationCoeffs()) +
                                WaveletTestUtils.computeEnergy(result.detailCoeffs());
        
        // Handle infinity case - both should be infinite or both finite
        if (Double.isInfinite(signalEnergy) || Double.isInfinite(transformEnergy)) {
            assertEquals(signalEnergy, transformEnergy,
                "Both signal and transform energy should be infinite for extreme values");
        } else {
            // Use relative tolerance for finite values
            double relativeError = Math.abs(signalEnergy - transformEnergy) / signalEnergy;
            
            // Allow up to 1% relative error for extreme values
            assertTrue(relativeError < 0.01,
                String.format("Energy should be approximately preserved even with extreme values. " +
                             "Relative error: %.6f%% (signal=%.6e, transform=%.6e)",
                             relativeError * 100, signalEnergy, transformEnergy));
        }
    }
    
    // === Helper Methods ===
    
}