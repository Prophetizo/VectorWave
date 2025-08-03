package ai.prophetizo.wavelet.integration;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTTransformFactory;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.test.BaseMODWTTest;
import ai.prophetizo.wavelet.util.ToleranceConstants;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Integration tests for wavelet transforms.
 * Tests end-to-end transform/inverse transform cycles,
 * different signal lengths, edge cases, and numerical stability.
 */
@DisplayName("Wavelet Transform Integration Tests")
class WaveletIntegrationTest extends BaseMODWTTest {
    
    private MODWTTransformFactory factory;
    
    private static final double TOLERANCE = ToleranceConstants.DEFAULT_TOLERANCE;
    
    @BeforeEach
    protected void setUp(org.junit.jupiter.api.TestInfo testInfo) {
        super.setUp(testInfo);
        factory = new MODWTTransformFactory();
    }
    
    // === End-to-End Transform/Inverse Transform Cycle Tests ===
    
    @ParameterizedTest(name = "Perfect reconstruction for {0} wavelet")
    @MethodSource("provideWavelets")
    @DisplayName("Should achieve perfect reconstruction for all wavelet types")
    void testPerfectReconstruction(String waveletName, Wavelet wavelet) {
        MODWTTransform transform = factory.create(wavelet);
        
        // Test with different signal types
        double[][] testSignals = {
            createConstantSignal(64, 1.0),
            createLinearSignal(64),
            createSineSignal(64, 0.1),
            createRandomSignal(64, TestConstants.TEST_SEED)
        };
        
        for (double[] signal : testSignals) {
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // DB4 may have slightly larger numerical errors due to longer filters
            double tolerance = waveletName.equals("DB4") ? 1e-7 : TOLERANCE;
            assertArrayEquals(signal, reconstructed, tolerance,
                String.format("Perfect reconstruction failed for %s with signal type", waveletName));
        }
    }
    
    @ParameterizedTest(name = "Signal length {0}")
    @ValueSource(ints = {2, 3, 5, 7, 10, 13, 16, 32, 50, 64, 100, 128, 250, 256, 500, 512, 1000, 1024})
    @DisplayName("Should handle different signal lengths (including non-power-of-2)")
    void testDifferentSignalLengths(int length) {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = createRandomSignal(length, 12345L);
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        assertEquals(length, result.approximationCoeffs().length,
            "MODWT approximation coefficients should be same length as signal");
        assertEquals(length, result.detailCoeffs().length,
            "MODWT detail coefficients should be same length as signal");
        assertArrayEquals(signal, reconstructed, TOLERANCE,
            "Reconstruction failed for length " + length);
    }
    
    @Test
    @DisplayName("Should handle minimum signal length (2 elements)")
    void testMinimumSignalLength() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = {3.0, 7.0};
        MODWTResult result = transform.forward(signal);
        
        assertEquals(2, result.approximationCoeffs().length);
        assertEquals(2, result.detailCoeffs().length);
        
        // For MODWT Haar, coefficients have same length as input
        // Values will be different due to MODWT's shift-invariant property
        // Just verify the coefficients are computed
        assertNotNull(result.detailCoeffs());
        assertEquals(2, result.detailCoeffs().length);
        
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle single element array")
    void testSingleElementArray() {
        MODWTTransform transform = factory.create(new Haar());
        double[] signal = {5.0};
        
        // MODWT can handle single element arrays
        MODWTResult result = transform.forward(signal);
        assertNotNull(result);
        assertEquals(1, result.approximationCoeffs().length);
        assertEquals(1, result.detailCoeffs().length);
        
        // Verify reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, TOLERANCE);
    }
    
    @Test
    @DisplayName("Should reject empty array")
    void testEmptyArray() {
        MODWTTransform transform = factory.create(new Haar());
        double[] signal = {};
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should reject empty array");
    }
    
    // === Boundary Mode Integration Tests ===
    
    @ParameterizedTest
    @MethodSource("provideBoundaryModes")
    @DisplayName("Should work correctly with different boundary modes")
    void testBoundaryModeIntegration(BoundaryMode mode) {
        // MODWT supports PERIODIC and ZERO_PADDING modes
        if (mode != BoundaryMode.PERIODIC && mode != BoundaryMode.ZERO_PADDING) {
            return; // Skip unsupported modes
        }
        MODWTTransform transform = new MODWTTransform(new Haar(), mode);
        
        // Use a signal that would show boundary effects
        double[] signal = createLinearSignal(32);
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        if (mode == BoundaryMode.PERIODIC) {
            // Periodic boundary mode should provide perfect reconstruction
            assertArrayEquals(signal, reconstructed, TOLERANCE,
                "Reconstruction failed with boundary mode: " + mode);
        } else if (mode == BoundaryMode.ZERO_PADDING) {
            // Zero-padding does not provide perfect reconstruction
            // Just verify the transform completes and produces valid output
            assertNotNull(reconstructed);
            assertEquals(signal.length, reconstructed.length);
            // Verify no NaN or infinite values
            for (double value : reconstructed) {
                assertTrue(Double.isFinite(value),
                    "Reconstructed signal should contain only finite values");
            }
        }
    }
    
    // === Numerical Stability Tests ===
    
    @Test
    @DisplayName("Should handle very large values")
    void testLargeValues() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 1e10 * (i + 1);
        }
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // Use relative tolerance for large values
        for (int i = 0; i < signal.length; i++) {
            double relativeDiff = Math.abs(signal[i] - reconstructed[i]) / Math.abs(signal[i]);
            assertTrue(relativeDiff < 1e-10,
                String.format("Large value reconstruction failed at index %d: expected %e, got %e",
                    i, signal[i], reconstructed[i]));
        }
    }
    
    @Test
    @DisplayName("Should handle very small values")
    void testSmallValues() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 1e-10 * (i + 1);
        }
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        assertArrayEquals(signal, reconstructed, 1e-20,
            "Small value reconstruction failed");
    }
    
    @Test
    @DisplayName("Should handle mixed extreme values")
    void testMixedExtremeValues() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = {
            1e-15, 1e15, -1e-15, -1e15,
            0.0, 1.0, -1.0, 1e10
        };
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // Check each value with appropriate tolerance
        for (int i = 0; i < signal.length; i++) {
            if (Math.abs(signal[i]) < 1) {
                assertEquals(signal[i], reconstructed[i], 1e-15,
                    "Failed at index " + i);
            } else {
                double relativeDiff = Math.abs(signal[i] - reconstructed[i]) / Math.abs(signal[i]);
                assertTrue(relativeDiff < 1e-10,
                    "Failed at index " + i);
            }
        }
    }
    
    @Test
    @DisplayName("Should reject signals with NaN values")
    void testNaNValues() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = {1.0, 2.0, Double.NaN, 4.0};
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should reject signal with NaN");
    }
    
    @Test
    @DisplayName("Should reject signals with infinity values")
    void testInfinityValues() {
        MODWTTransform transform = factory.create(new Haar());
        
        double[] signal = {1.0, 2.0, Double.POSITIVE_INFINITY, 4.0};
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should reject signal with infinity");
    }
    
    // === Cross-Wavelet Compatibility Tests ===
    
    @Test
    @DisplayName("Should not allow inverse transform with mismatched wavelet")
    void testCrossWaveletIncompatibility() {
        MODWTTransform haarTransform = factory.create(new Haar());
        MODWTTransform db2Transform = factory.create(Daubechies.DB2);
        
        double[] signal = createRandomSignal(64, 999L);
        MODWTResult haarResult = haarTransform.forward(signal);
        
        // While technically this might work, it should produce incorrect results
        double[] incorrectReconstruction = db2Transform.inverse(haarResult);
        
        // The reconstruction should NOT match the original signal
        boolean allClose = true;
        for (int i = 0; i < signal.length; i++) {
            if (Math.abs(signal[i] - incorrectReconstruction[i]) > 1e-10) {
                allClose = false;
                break;
            }
        }
        assertFalse(allClose, 
            "Cross-wavelet reconstruction should not match original signal");
    }
    
    // === Consistency Tests ===
    
    @Test
    @DisplayName("Multiple transforms should produce consistent results")
    void testTransformConsistency() {
        double[] signal = createRandomSignal(128, 777L);
        
        // Create multiple transforms with same settings
        MODWTTransform transform1 = factory.create(new Haar());
        MODWTTransform transform2 = factory.create(new Haar());
        
        MODWTResult result1 = transform1.forward(signal);
        MODWTResult result2 = transform2.forward(signal);
        
        assertArrayEquals(result1.approximationCoeffs(), 
                         result2.approximationCoeffs(), 
                         TOLERANCE,
                         "Approximation coefficients should match between transforms");
        
        assertArrayEquals(result1.detailCoeffs(), 
                         result2.detailCoeffs(), 
                         TOLERANCE,
                         "Detail coefficients should match between transforms");
    }
    
    // === Helper Methods ===
    
    private static Stream<Arguments> provideWavelets() {
        return Stream.of(
            Arguments.of("Haar", new Haar()),
            Arguments.of("DB2", Daubechies.DB2),
            Arguments.of("DB4", Daubechies.DB4)
        );
    }
    
    private static Stream<BoundaryMode> provideBoundaryModes() {
        // Only test implemented boundary modes
        return Stream.of(BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING);
    }
    
    private double[] createConstantSignal(int length, double value) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = value;
        }
        return signal;
    }
    
    private double[] createLinearSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i;
        }
        return signal;
    }
    
    private double[] createSineSignal(int length, double frequency) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i);
        }
        return signal;
    }
    
    private double[] createRandomSignal(int length, long seed) {
        Random random = new Random(seed);
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
}