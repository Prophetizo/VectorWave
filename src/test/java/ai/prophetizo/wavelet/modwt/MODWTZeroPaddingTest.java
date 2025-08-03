package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.modwt.test.BaseMODWTTest;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MODWT with ZERO_PADDING boundary mode.
 */
@DisplayName("MODWT Zero Padding Tests")
class MODWTZeroPaddingTest extends BaseMODWTTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("Should create MODWT with ZERO_PADDING boundary mode")
    void testCreateWithZeroPadding() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        assertEquals(BoundaryMode.ZERO_PADDING, transform.getBoundaryMode());
    }
    
    @Test
    @DisplayName("Should handle simple signal with ZERO_PADDING")
    void testSimpleSignalZeroPadding() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        // Simple signal where boundary effects are visible
        double[] signal = {1, 2, 3, 4};
        MODWTResult result = transform.forward(signal);
        
        assertNotNull(result);
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
        
        // With zero padding, boundary coefficients will be different from periodic
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // First coefficient: only signal[0] contributes (signal[-1] is treated as 0)
        // For Haar with zero padding: h0 = 1/sqrt(2), h1 = 1/sqrt(2) 
        // approx[0] = h0 * signal[0] + h1 * 0 = signal[0] / sqrt(2)
        // detail[0] = g0 * signal[0] + g1 * 0 = signal[0] / sqrt(2)
        // But filters are already scaled by 1/sqrt(2) in MODWT
        double expectedApprox0 = signal[0] / 2.0;  // (1/sqrt(2)) * (1/sqrt(2)) = 1/2
        double expectedDetail0 = signal[0] / 2.0;
        
        assertEquals(expectedApprox0, approx[0], TOLERANCE);
        assertEquals(expectedDetail0, detail[0], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should differ from PERIODIC mode at boundaries")
    void testDifferenceFromPeriodicMode() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MODWTTransform periodicTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTTransform zeroPadTransform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        MODWTResult periodicResult = periodicTransform.forward(signal);
        MODWTResult zeroPadResult = zeroPadTransform.forward(signal);
        
        double[] periodicApprox = periodicResult.approximationCoeffs();
        double[] zeroPadApprox = zeroPadResult.approximationCoeffs();
        double[] periodicDetail = periodicResult.detailCoeffs();
        double[] zeroPadDetail = zeroPadResult.detailCoeffs();
        
        // Boundary coefficients should differ
        assertNotEquals(periodicApprox[0], zeroPadApprox[0], TOLERANCE,
            "First approximation coefficient should differ between modes");
        assertNotEquals(periodicDetail[0], zeroPadDetail[0], TOLERANCE,
            "First detail coefficient should differ between modes");
        
        // Interior coefficients should be similar (but not necessarily identical due to filter length)
        // For Haar wavelet with length 2, coefficient at index 2 should be unaffected by boundaries
        assertEquals(periodicApprox[2], zeroPadApprox[2], TOLERANCE,
            "Interior approximation coefficients should match");
        assertEquals(periodicDetail[2], zeroPadDetail[2], TOLERANCE,
            "Interior detail coefficients should match");
    }
    
    @Test
    @DisplayName("ZERO_PADDING may have boundary effects")
    void testBoundaryEffects() {
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // ZERO_PADDING may not provide perfect reconstruction at boundaries
        // Check that interior points are well reconstructed
        for (int i = 1; i < signal.length - 1; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-8,
                "Interior points should be well reconstructed at index " + i);
        }
        
        // Check if there are boundary effects
        double totalError = 0;
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            double error = Math.abs(signal[i] - reconstructed[i]);
            totalError += error;
            maxError = Math.max(maxError, error);
        }
        
        // With ZERO_PADDING, there will be reconstruction error at boundaries
        // For this simple ramp signal, the error is significant
        System.out.println("Total error: " + totalError + ", Max error: " + maxError);
        assertTrue(totalError > 0.1, 
            "ZERO_PADDING should have noticeable reconstruction error");
    }
    
    @ParameterizedTest
    @MethodSource("provideWavelets")
    @DisplayName("Should work with different wavelets in ZERO_PADDING mode")
    void testDifferentWavelets(String waveletName, Wavelet wavelet) {
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.ZERO_PADDING);
        
        double[] signal = createTestSignal(64);
        MODWTResult result = transform.forward(signal);
        
        assertNotNull(result);
        assertEquals(64, result.approximationCoeffs().length);
        assertEquals(64, result.detailCoeffs().length);
        
        // Verify coefficients are computed (not all zeros)
        double approxEnergy = computeEnergy(result.approximationCoeffs());
        double detailEnergy = computeEnergy(result.detailCoeffs());
        
        assertTrue(approxEnergy > 0, "Approximation coefficients should have energy");
        assertTrue(detailEnergy > 0, "Detail coefficients should have energy");
    }
    
    @Test
    @DisplayName("Multi-level MODWT should work with ZERO_PADDING")
    void testMultiLevelZeroPadding() {
        MultiLevelMODWTTransform mlTransform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.ZERO_PADDING);
        
        double[] signal = createTestSignal(32);
        MultiLevelMODWTResult result = mlTransform.decompose(signal, 3);
        
        assertNotNull(result);
        assertEquals(3, result.getLevels());
        
        // Check all levels have correct length
        for (int level = 1; level <= 3; level++) {
            double[] detail = result.getDetailCoeffsAtLevel(level);
            assertEquals(32, detail.length, "Level " + level + " should have same length as signal");
        }
    }
    
    // Helper methods
    
    private static Stream<Arguments> provideWavelets() {
        return Stream.of(
            Arguments.of("Haar", new Haar()),
            Arguments.of("DB2", Daubechies.DB2),
            Arguments.of("DB4", Daubechies.DB4)
        );
    }
    
    protected double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length) + 0.5 * Math.cos(4 * Math.PI * i / length);
        }
        return signal;
    }
    
    private double computeEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
}