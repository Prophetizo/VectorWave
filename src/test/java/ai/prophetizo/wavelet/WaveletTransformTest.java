package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
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
        assertThrows(InvalidArgumentException.class, 
            () -> new WaveletTransform(null, BoundaryMode.PERIODIC),
            "Should throw InvalidArgumentException for null wavelet");
    }
    
    @Test
    @DisplayName("Constructor should reject null boundary mode")
    void testConstructorNullBoundaryMode() {
        assertThrows(InvalidArgumentException.class,
            () -> new WaveletTransform(new Haar(), null),
            "Should throw InvalidArgumentException for null boundary mode");
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
    @DisplayName("Forward transform should reject non-power-of-2 lengths")
    void testForwardNonPowerOfTwoLength(int length) {
        WaveletTransform transform = createTransform(new Haar());
        double[] signal = new double[length];
        
        InvalidSignalException exception = assertThrows(
            InvalidSignalException.class,
            () -> transform.forward(signal));
        
        assertTrue(exception.getMessage().contains("power of two"),
            "Error message should mention power of two requirement");
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
            signal, reconstructed, ToleranceConstants.DEFAULT_TOLERANCE);
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
        assertThrows(InvalidConfigurationException.class,
            () -> createTransform(new Haar(), BoundaryMode.SYMMETRIC),
            "Should throw InvalidConfigurationException for SYMMETRIC mode");
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
            signal, result, ToleranceConstants.ENERGY_TOLERANCE);
    }
    
    // === Edge Cases ===
    
    @Test
    @DisplayName("Transform of minimum size signal (2 elements)")
    void testMinimumSizeSignal() {
        double[] signal = {3.14, 2.71};
        WaveletTransform transform = createTransform(new Haar());
        
        TransformResult result = transform.forward(signal);
        
        assertEquals(1, result.approximationCoeffs().length);
        assertEquals(1, result.detailCoeffs().length);
        
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, ToleranceConstants.DEFAULT_TOLERANCE);
    }
    
    @Test
    @DisplayName("Transform of large signal")
    void testLargeSignal() {
        double[] signal = WaveletTestUtils.generateRandomSignal(
            4096, 99999L, -100.0, 100.0);
        
        WaveletTransform transform = createTransform(Daubechies.DB4);
        TransformResult result = transform.forward(signal);
        
        assertEquals(2048, result.approximationCoeffs().length);
        assertEquals(2048, result.detailCoeffs().length);
        
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
            
            // Use centralized tolerance constant with detailed explanation
            assertTrue(relativeError < ToleranceConstants.EXTREME_VALUE_RELATIVE_TOLERANCE,
                String.format("Energy should be approximately preserved even with extreme values. " +
                             "Relative error: %.6f%% (signal=%.6e, transform=%.6e). %s",
                             relativeError * 100, signalEnergy, transformEnergy,
                             ToleranceConstants.explainTolerance(ToleranceConstants.EXTREME_VALUE_RELATIVE_TOLERANCE)));
        }
    }
    
    @Test
    @DisplayName("Zero-padding boundary mode with vectorized operations")
    void testZeroPaddingVectorizedOperations() {
        // Test with zero-padding mode to ensure vectorized implementation works
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.ZERO_PADDING);
        
        // Forward transform
        TransformResult result = transform.forward(signal);
        
        // Inverse transform
        double[] reconstructed = transform.inverse(result);
        
        // Due to zero-padding, there will be border effects.
        // The reconstruction won't be perfect, but should be reasonable
        double totalError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            totalError += Math.abs(signal[i] - reconstructed[i]);
        }
        double avgError = totalError / signal.length;
        
        // Zero-padding introduces errors, but they should be bounded
        assertTrue(avgError < 0.5, 
            "Average reconstruction error should be reasonable: " + avgError);
        
        // Test with larger signal to ensure vectorization is used
        double[] largeSignal = new double[256];
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        TransformResult largeResult = transform.forward(largeSignal);
        double[] largeReconstructed = transform.inverse(largeResult);
        
        // Check reconstruction error is bounded
        double largeError = 0.0;
        for (int i = 0; i < largeSignal.length; i++) {
            largeError += Math.abs(largeSignal[i] - largeReconstructed[i]);
        }
        double largeAvgError = largeError / largeSignal.length;
        
        assertTrue(largeAvgError < 0.1, 
            "Average reconstruction error for smooth signal should be small: " + largeAvgError);
    }
    
    @Test
    @DisplayName("Zero-padding vs Periodic boundary mode comparison")
    void testBoundaryModeComparison() {
        // Create a signal with strong boundary discontinuity
        // Use a ramp function that would wrap around in periodic mode
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i; // Linear ramp from 0 to 63
        }
        
        // Use DB2 which is more sensitive to boundary conditions than Haar
        WaveletTransform periodicTransform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        WaveletTransform zeroPadTransform = new WaveletTransform(Daubechies.DB2, BoundaryMode.ZERO_PADDING);
        
        TransformResult periodicResult = periodicTransform.forward(signal);
        TransformResult zeroPadResult = zeroPadTransform.forward(signal);
        
        // The results should be different due to boundary handling
        double[] periodicDetails = periodicResult.detailCoeffs();
        double[] zeroPadDetails = zeroPadResult.detailCoeffs();
        
        // Check the last few coefficients where boundary effects are strongest
        double maxDiff = 0.0;
        int diffCount = 0;
        for (int i = 0; i < periodicDetails.length; i++) {
            double diff = Math.abs(periodicDetails[i] - zeroPadDetails[i]);
            maxDiff = Math.max(maxDiff, diff);
            if (diff > 1e-10) {
                diffCount++;
            }
        }
        
        assertTrue(diffCount > 0, 
            "Periodic and zero-padding modes should produce different results. Max diff: " + maxDiff);
        
        // Verify that the difference is significant
        assertTrue(maxDiff > 0.1, 
            "Boundary mode differences should be significant for ramp signal. Max diff: " + maxDiff);
    }
    
    // === Helper Methods ===
    
}