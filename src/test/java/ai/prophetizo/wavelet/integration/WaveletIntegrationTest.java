package ai.prophetizo.wavelet.integration;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
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

/**
 * Integration tests for wavelet transforms.
 * Tests end-to-end transform/inverse transform cycles,
 * different signal lengths, edge cases, and numerical stability.
 */
@DisplayName("Wavelet Transform Integration Tests")
class WaveletIntegrationTest extends BaseWaveletTest {
    
    private WaveletTransformFactory factory;
    
    private static final double TOLERANCE = ToleranceConstants.DEFAULT_TOLERANCE;
    
    @BeforeEach
    protected void setUp(org.junit.jupiter.api.TestInfo testInfo) {
        super.setUp(testInfo);
        factory = new WaveletTransformFactory();
    }
    
    // === End-to-End Transform/Inverse Transform Cycle Tests ===
    
    @ParameterizedTest(name = "Perfect reconstruction for {0} wavelet")
    @MethodSource("provideWavelets")
    @DisplayName("Should achieve perfect reconstruction for all wavelet types")
    void testPerfectReconstruction(String waveletName, Wavelet wavelet) {
        WaveletTransform transform = factory.create(wavelet);
        
        // Test with different signal types
        double[][] testSignals = {
            createConstantSignal(64, 1.0),
            createLinearSignal(64),
            createSineSignal(64, 0.1),
            createRandomSignal(64, 42L)
        };
        
        for (double[] signal : testSignals) {
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // DB4 may have slightly larger numerical errors due to longer filters
            double tolerance = waveletName.equals("DB4") ? 1e-7 : TOLERANCE;
            assertArrayEquals(signal, reconstructed, tolerance,
                String.format("Perfect reconstruction failed for %s with signal type", waveletName));
        }
    }
    
    @ParameterizedTest(name = "Signal length {0}")
    @ValueSource(ints = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096})
    @DisplayName("Should handle different powers of 2 signal lengths")
    void testDifferentSignalLengths(int length) {
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = createRandomSignal(length, 12345L);
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        assertEquals(length / 2, result.approximationCoeffs().length,
            "Approximation coefficients should be half the signal length");
        assertEquals(length / 2, result.detailCoeffs().length,
            "Detail coefficients should be half the signal length");
        assertArrayEquals(signal, reconstructed, TOLERANCE,
            "Reconstruction failed for length " + length);
    }
    
    @Test
    @DisplayName("Should handle minimum signal length (2 elements)")
    void testMinimumSignalLength() {
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = {3.0, 7.0};
        TransformResult result = transform.forward(signal);
        
        assertEquals(1, result.approximationCoeffs().length);
        assertEquals(1, result.detailCoeffs().length);
        
        // For Haar: approx = (3 + 7) / sqrt(2) ≈ 7.07
        //           detail = (3 - 7) / sqrt(2) ≈ -2.83
        assertEquals(7.071, result.approximationCoeffs()[0], 0.001);
        assertEquals(-2.828, result.detailCoeffs()[0], 0.001);
        
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, TOLERANCE);
    }
    
    @Test
    @DisplayName("Should reject single element array")
    void testSingleElementArray() {
        WaveletTransform transform = factory.create(new Haar());
        double[] signal = {5.0};
        
        // Single element arrays trigger assertion error in debug mode,
        // or InvalidSignalException when assertions are disabled
        assertThrows(Throwable.class,
            () -> transform.forward(signal),
            "Should reject single element array");
    }
    
    @Test
    @DisplayName("Should reject empty array")
    void testEmptyArray() {
        WaveletTransform transform = factory.create(new Haar());
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
        WaveletTransform transform = factory
            .withBoundaryMode(mode)
            .create(new Haar());
        
        // Use a signal that would show boundary effects
        double[] signal = createLinearSignal(32);
        
        TransformResult result = transform.forward(signal);
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
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 1e10 * (i + 1);
        }
        
        TransformResult result = transform.forward(signal);
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
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 1e-10 * (i + 1);
        }
        
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        assertArrayEquals(signal, reconstructed, 1e-20,
            "Small value reconstruction failed");
    }
    
    @Test
    @DisplayName("Should handle mixed extreme values")
    void testMixedExtremeValues() {
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = {
            1e-15, 1e15, -1e-15, -1e15,
            0.0, 1.0, -1.0, 1e10
        };
        
        TransformResult result = transform.forward(signal);
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
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = {1.0, 2.0, Double.NaN, 4.0};
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should reject signal with NaN");
    }
    
    @Test
    @DisplayName("Should reject signals with infinity values")
    void testInfinityValues() {
        WaveletTransform transform = factory.create(new Haar());
        
        double[] signal = {1.0, 2.0, Double.POSITIVE_INFINITY, 4.0};
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(signal),
            "Should reject signal with infinity");
    }
    
    // === Cross-Wavelet Compatibility Tests ===
    
    @Test
    @DisplayName("Should not allow inverse transform with mismatched wavelet")
    void testCrossWaveletIncompatibility() {
        WaveletTransform haarTransform = factory.create(new Haar());
        WaveletTransform db2Transform = factory.create(Daubechies.DB2);
        
        double[] signal = createRandomSignal(64, 999L);
        TransformResult haarResult = haarTransform.forward(signal);
        
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
        WaveletTransform transform1 = factory.create(new Haar());
        WaveletTransform transform2 = factory.create(new Haar());
        
        TransformResult result1 = transform1.forward(signal);
        TransformResult result2 = transform2.forward(signal);
        
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