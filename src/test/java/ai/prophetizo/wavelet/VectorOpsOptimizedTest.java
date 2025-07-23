package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for optimized vector operations to ensure correctness.
 */
class VectorOpsOptimizedTest {
    
    private static final double EPSILON = 1e-10;
    
    @ParameterizedTest
    @ValueSource(ints = {64, 128, 256, 512, 1024})
    void testConvolveAndDownsampleOptimized(int signalSize) {
        // Create test signal
        double[] signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Create test filter (DB4-like)
        double[] filter = {0.23, 0.71, 0.63, -0.03, -0.19, 0.03, 0.03, -0.01};
        
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
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.random() - 0.5;
        }
        
        // DB2 filters
        double[] lowFilter = {0.48, 0.84, 0.22, -0.13};
        double[] highFilter = {-0.13, -0.22, 0.84, -0.48};
        
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
        
        // Haar coefficients
        double[] haarLow = {0.7071067811865476, 0.7071067811865476};
        double[] haarHigh = {0.7071067811865476, -0.7071067811865476};
        
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
        
        // Test filter
        double[] filter = {0.25, 0.5, 0.25};
        
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
        double[] filter = {0.5, 0.5};
        
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
}