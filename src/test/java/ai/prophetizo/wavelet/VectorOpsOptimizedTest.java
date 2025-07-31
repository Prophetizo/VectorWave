package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Tests for optimized vector operations to ensure correctness.
 * 
 * <p>These tests compare the output of scalar, standard vector, and optimized vector
 * implementations to ensure they produce equivalent results within acceptable
 * floating-point precision bounds.</p>
 * 
 * <h3>Floating-Point Precision</h3>
 * <p>The tests use an epsilon value of 1e-10 for comparisons, which provides a good balance between:</p>
 * <ul>
 *   <li>Detecting actual implementation errors</li>
 *   <li>Allowing for minor numerical differences due to:
 *     <ul>
 *       <li>Different order of operations in SIMD (non-associativity of FP arithmetic)</li>
 *       <li>Potential use of fused multiply-add (FMA) instructions</li>
 *       <li>Different rounding behaviors between scalar and vector operations</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>If tests fail due to numerical precision on specific hardware, consider
 * relaxing the epsilon to 1e-9 or 1e-8, but document the hardware configuration.</p>
 */
class VectorOpsOptimizedTest {
    
    private static final double EPSILON = 1e-10;
    
    // Test filter coefficients from actual wavelets
    private static final double[] HAAR_LOW_PASS = new Haar().lowPassDecomposition();
    private static final double[] HAAR_HIGH_PASS = new Haar().highPassDecomposition();
    private static final double[] DB2_LOW_PASS = Daubechies.DB2.lowPassDecomposition();
    private static final double[] DB2_HIGH_PASS = Daubechies.DB2.highPassDecomposition();
    private static final double[] DB4_LOW_PASS = Daubechies.DB4.lowPassDecomposition();
    private static final double[] DB4_HIGH_PASS = Daubechies.DB4.highPassDecomposition();
    
    @ParameterizedTest
    @ValueSource(ints = {64, 128, 256, 512, 1024})
    void testConvolveAndDownsampleOptimized(int signalSize) {
        // Create test signal
        double[] signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Use actual DB4 filter coefficients
        double[] filter = DB4_LOW_PASS;
        
        // Compute with different implementations
        double[] scalarResult = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signalSize, filter.length);
        double[] vectorResult = VectorOps.convolveAndDownsamplePeriodic(
            signal, filter, signalSize, filter.length);
        double[] optimizedResult = VectorOpsOptimized.convolveAndDownsamplePeriodicOptimized(
            signal, filter, signalSize, filter.length);
        
        // Verify all implementations produce same result
        assertArrayEquals(scalarResult, vectorResult, EPSILON, 
            "Vector result differs from scalar");
        assertArrayEquals(scalarResult, optimizedResult, EPSILON, 
            "Optimized result differs from scalar");
    }
    
    @Test
    void testCombinedTransformOptimized() {
        int signalSize = 256;
        double[] signal = new double[signalSize];
        Random random = new Random(TestConstants.TEST_SEED); // Seed for reproducibility
        for (int i = 0; i < signalSize; i++) {
            signal[i] = random.nextDouble() - 0.5;
        }
        
        // Use actual DB2 filters
        double[] lowFilter = DB2_LOW_PASS;
        double[] highFilter = DB2_HIGH_PASS;
        
        // Scalar reference
        double[] scalarApprox = new double[signalSize / 2];
        double[] scalarDetail = new double[signalSize / 2];
        ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, 
                                           scalarApprox, scalarDetail);
        
        // Optimized vector
        double[] vectorApprox = new double[signalSize / 2];
        double[] vectorDetail = new double[signalSize / 2];
        VectorOpsOptimized.combinedTransformPeriodicVectorized(
            signal, lowFilter, highFilter, vectorApprox, vectorDetail);
        
        // Verify
        assertArrayEquals(scalarApprox, vectorApprox, EPSILON, 
            "Approximation coefficients differ");
        assertArrayEquals(scalarDetail, vectorDetail, EPSILON, 
            "Detail coefficients differ");
    }
    
    @Test
    void testHaarTransformOptimized() {
        int signalSize = 512;
        double[] signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = i % 17; // Repeating pattern
        }
        
        // Use actual Haar coefficients
        double[] haarLow = HAAR_LOW_PASS;
        double[] haarHigh = HAAR_HIGH_PASS;
        
        // Scalar reference
        double[] scalarApprox = new double[signalSize / 2];
        double[] scalarDetail = new double[signalSize / 2];
        ScalarOps.combinedTransformPeriodic(signal, haarLow, haarHigh, 
                                           scalarApprox, scalarDetail);
        
        // Optimized Haar
        double[] vectorApprox = new double[signalSize / 2];
        double[] vectorDetail = new double[signalSize / 2];
        VectorOpsOptimized.haarTransformVectorized(signal, vectorApprox, vectorDetail);
        
        // Verify
        assertArrayEquals(scalarApprox, vectorApprox, EPSILON, 
            "Haar approximation coefficients differ");
        assertArrayEquals(scalarDetail, vectorDetail, EPSILON, 
            "Haar detail coefficients differ");
    }
    
    @Test
    void testUpsampleAndConvolveOptimized() {
        int coeffsSize = 128;
        double[] coeffs = new double[coeffsSize];
        for (int i = 0; i < coeffsSize; i++) {
            coeffs[i] = Math.cos(2 * Math.PI * i / 16.0);
        }
        
        // Test with a simple symmetric filter
        double[] filter = createSymmetricFilter(3);
        
        // Compute with different implementations
        double[] scalarResult = ScalarOps.upsampleAndConvolvePeriodic(
            coeffs, filter, coeffsSize, filter.length);
        double[] optimizedResult = VectorOpsOptimized.upsampleAndConvolvePeriodicOptimized(
            coeffs, filter, coeffsSize, filter.length);
        
        // Verify
        assertArrayEquals(scalarResult, optimizedResult, EPSILON, 
            "Optimized upsampling differs from scalar");
    }
    
    @Test
    void testBoundaryConditions() {
        // Test with very small signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = createNormalizedFilter(0.5, 0.5);
        
        double[] scalarResult = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signal.length, filter.length);
        double[] optimizedResult = VectorOpsOptimized.convolveAndDownsamplePeriodicOptimized(
            signal, filter, signal.length, filter.length);
        
        assertArrayEquals(scalarResult, optimizedResult, EPSILON, 
            "Small signal results differ");
    }
    
    @Test
    void testLargeFilterSupport() {
        // Test with large filter (Symlet 8)
        int signalSize = 256;
        double[] signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = (i % 2 == 0) ? 1.0 : -1.0;
        }
        
        // Symlet 8 has 16 coefficients
        double[] filter = new double[16];
        for (int i = 0; i < 16; i++) {
            filter[i] = Math.random() * 0.1;
        }
        
        double[] scalarResult = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signalSize, filter.length);
        double[] optimizedResult = VectorOpsOptimized.convolveAndDownsamplePeriodicOptimized(
            signal, filter, signalSize, filter.length);
        
        assertArrayEquals(scalarResult, optimizedResult, EPSILON, 
            "Large filter results differ");
    }
    
    /**
     * Creates a normalized filter with the given coefficients.
     * The filter is normalized so that the sum of coefficients equals 1.
     */
    private static double[] createNormalizedFilter(double... coefficients) {
        double sum = 0.0;
        for (double coeff : coefficients) {
            sum += coeff;
        }
        double[] normalized = new double[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            normalized[i] = coefficients[i] / sum;
        }
        return normalized;
    }
    
    /**
     * Creates a symmetric filter of the given length with triangular shape.
     * For length 3: [0.25, 0.5, 0.25]
     * For length 5: [0.0625, 0.25, 0.375, 0.25, 0.0625]
     */
    private static double[] createSymmetricFilter(int length) {
        double[] filter = new double[length];
        int mid = length / 2;
        
        // Create triangular filter
        for (int i = 0; i <= mid; i++) {
            filter[i] = i + 1;
            filter[length - 1 - i] = i + 1;
        }
        
        // Normalize
        double sum = 0.0;
        for (double val : filter) {
            sum += val;
        }
        for (int i = 0; i < length; i++) {
            filter[i] /= sum;
        }
        
        return filter;
    }
}