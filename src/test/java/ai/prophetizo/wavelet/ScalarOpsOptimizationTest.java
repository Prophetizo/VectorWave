package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for scalar operations optimizations.
 * Ensures mathematical correctness of optimizations integrated into ScalarOps.
 */
class ScalarOpsOptimizationTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("ScalarOps automatically uses optimizations for small power-of-2 signals")
    void testAutomaticOptimizationPath() {
        // Test that ScalarOps produces correct results for signals that trigger optimization
        double[] signal256 = createSignal(256);
        double[] signal1024 = createSignal(1024);
        double[] filter = {0.5, 0.5}; // Simple averaging filter
        
        double[] output256 = new double[128];
        double[] output1024 = new double[512];
        
        // These should use optimized paths internally
        ScalarOps.convolveAndDownsamplePeriodic(signal256, filter, output256);
        ScalarOps.convolveAndDownsamplePeriodic(signal1024, filter, output1024);
        
        // Verify results are mathematically correct
        for (int i = 0; i < output256.length; i++) {
            double expected = (signal256[2*i] * filter[0] + signal256[(2*i + 1) % 256] * filter[1]);
            assertEquals(expected, output256[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("ScalarOps uses specialized Haar implementation automatically")
    void testHaarSpecializationPath() {
        Wavelet haar = WaveletRegistry.getWavelet("haar");
        double[] signal = createSignal(512);
        double[] lowFilter = haar.lowPassDecomposition();
        double[] highFilter = haar.highPassDecomposition();
        
        assertEquals(2, lowFilter.length, "Haar should have 2 coefficients");
        
        double[] approx = new double[256];
        double[] detail = new double[256];
        
        // ScalarOps should automatically use Haar optimization
        ScalarOps.convolveAndDownsamplePeriodic(signal, lowFilter, approx);
        ScalarOps.convolveAndDownsamplePeriodic(signal, highFilter, detail);
        
        // Verify correctness
        for (int i = 0; i < approx.length; i++) {
            double expectedLow = signal[2*i] * lowFilter[0] + signal[(2*i + 1) % 512] * lowFilter[1];
            double expectedHigh = signal[2*i] * highFilter[0] + signal[(2*i + 1) % 512] * highFilter[1];
            assertEquals(expectedLow, approx[i], EPSILON);
            assertEquals(expectedHigh, detail[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("ScalarOps uses specialized DB2 implementation automatically")
    void testDB2SpecializationPath() {
        Wavelet db2 = WaveletRegistry.getWavelet("db2");
        double[] signal = createSignal(256);
        double[] filter = db2.lowPassDecomposition();
        
        assertEquals(4, filter.length, "DB2 should have 4 coefficients");
        
        double[] output = new double[128];
        
        // ScalarOps should automatically use DB2 optimization
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        // Verify correctness manually
        for (int i = 0; i < output.length; i++) {
            double expected = 0;
            for (int j = 0; j < 4; j++) {
                expected += signal[(2*i + j) % 256] * filter[j];
            }
            assertEquals(expected, output[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Combined transform produces same results as separate transforms")
    void testCombinedTransformCorrectness() {
        double[] signal = createSignal(512);
        double[] lowFilter = {0.25, 0.5, 0.25}; // Simple low-pass
        double[] highFilter = {-0.25, 0.5, -0.25}; // Simple high-pass
        
        // Separate transforms
        double[] approxSeparate = new double[256];
        double[] detailSeparate = new double[256];
        ScalarOps.convolveAndDownsamplePeriodic(signal, lowFilter, approxSeparate);
        ScalarOps.convolveAndDownsamplePeriodic(signal, highFilter, detailSeparate);
        
        // Combined transform
        double[] approxCombined = new double[256];
        double[] detailCombined = new double[256];
        ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, 
                                           approxCombined, detailCombined);
        
        assertArrayEquals(approxSeparate, approxCombined, EPSILON,
            "Combined transform approximation should match separate");
        assertArrayEquals(detailSeparate, detailCombined, EPSILON,
            "Combined transform detail should match separate");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {64, 128, 256, 512, 1024})
    @DisplayName("WaveletTransform uses optimizations automatically for small signals")
    void testTransformOptimizationPath(int signalLength) {
        Wavelet wavelet = WaveletRegistry.getWavelet("db2");
        double[] signal = createSignal(signalLength);
        
        WaveletTransform transform = new WaveletTransform(wavelet, ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
        
        // Transform should automatically use optimizations
        TransformResult result = transform.forward(signal);
        
        // Verify correctness
        assertEquals(signalLength / 2, result.approximationCoeffs().length);
        assertEquals(signalLength / 2, result.detailCoeffs().length);
        
        // Verify perfect reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, EPSILON,
                         "Perfect reconstruction should be maintained");
    }
    
    @Test
    @DisplayName("Perfect reconstruction maintained with all optimizations")
    void testPerfectReconstructionWithOptimizations() {
        Wavelet wavelet = WaveletRegistry.getWavelet("haar");
        double[] original = createSignal(256);
        
        WaveletTransform transform = new WaveletTransform(wavelet, ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
        
        TransformResult result = transform.forward(original);
        double[] reconstructed = transform.inverse(result);
        
        assertArrayEquals(original, reconstructed, EPSILON,
            "Perfect reconstruction should be maintained with integrated optimizations");
    }
    
    @Test
    @DisplayName("ImmutableTransformResult maintains data integrity")
    void testImmutableTransformResult() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {-1.0, -2.0, -3.0, -4.0};
        
        ImmutableTransformResult result = new ImmutableTransformResult(approx, detail);
        
        // Modify original arrays
        approx[0] = 999.0;
        detail[0] = -999.0;
        
        // Result should be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0], EPSILON);
        assertEquals(-1.0, result.detailCoeffs()[0], EPSILON);
        
        // Views should be read-only
        assertThrows(Exception.class, () -> {
            result.approximationCoeffsView().put(0, 999.0);
        });
    }
    
    @Test
    @DisplayName("Validation optimizations work correctly")
    void testValidationOptimizations() {
        // Test that validation fast path works for small signals
        double[] validSignal = createSignal(512);
        double[] invalidSignal = createSignal(512);
        invalidSignal[100] = Double.NaN;
        
        // Should pass validation
        assertDoesNotThrow(() -> ValidationUtils.validateSignal(validSignal, "signal"));
        
        // Should fail validation at correct index
        InvalidSignalException ex = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(invalidSignal, "signal"));
        assertTrue(ex.getMessage().contains("index 100"));
    }
    
    /**
     * Creates a test signal with known properties.
     */
    private double[] createSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combination of low and high frequency components
            signal[i] = Math.sin(2 * Math.PI * i / length) + 
                       0.5 * Math.sin(16 * Math.PI * i / length);
        }
        return signal;
    }
}