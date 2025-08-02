package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.internal.ArrayPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for additional small signal optimizations.
 */
class OptimizationTest {
    
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        // Clear array pool before each test
        ArrayPool.clear();
    }
    
    @Test
    @DisplayName("Haar wavelet filter caching eliminates allocations")
    void testHaarFilterCaching() {
        Haar haar = new Haar();
        
        // Get filters multiple times
        double[] lowPass1 = haar.lowPassDecomposition();
        double[] lowPass2 = haar.lowPassDecomposition();
        double[] highPass1 = haar.highPassDecomposition();
        double[] highPass2 = haar.highPassDecomposition();
        
        // Filters should be equal but not same instance (defensive copies)
        assertNotSame(lowPass1, lowPass2, "Should return defensive copies");
        assertNotSame(highPass1, highPass2, "Should return defensive copies");
        assertArrayEquals(lowPass1, lowPass2, EPSILON);
        assertArrayEquals(highPass1, highPass2, EPSILON);
        
        // Verify correct values
        double expected = 1.0 / Math.sqrt(2);
        assertArrayEquals(new double[]{expected, expected}, lowPass1, EPSILON);
        assertArrayEquals(new double[]{expected, -expected}, highPass1, EPSILON);
    }
    
    @Test
    @DisplayName("Array pool reduces allocations for batch processing")
    void testArrayPoolBatchProcessing() {
        WaveletTransformPool transform = new WaveletTransformPool(new Haar());
        
        // Process multiple signals
        double[][] signals = new double[100][256];
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 32.0) + 0.1 * Math.random();
            }
        }
        
        // Process all signals
        TransformResult[] results = new TransformResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        
        // Verify results
        for (int i = 0; i < results.length; i++) {
            assertEquals(128, results[i].approximationCoeffs().length);
            assertEquals(128, results[i].detailCoeffs().length);
            
            // Verify reconstruction
            double[] reconstructed = transform.inverse(results[i]);
            assertArrayEquals(signals[i], reconstructed, 1e-10);
        }
        
        // Clear pool when done
        transform.clearPool();
    }
    
    @ParameterizedTest
    @ValueSource(ints = {32, 64, 128, 256, 512, 1024})
    @DisplayName("Array pool handles common signal sizes efficiently")
    void testArrayPoolCommonSizes(int size) {
        // Borrow and release arrays
        double[] array1 = ArrayPool.borrow(size);
        assertEquals(size, array1.length);
        
        // Fill with test data
        for (int i = 0; i < size; i++) {
            array1[i] = i;
        }
        
        // Release back to pool
        ArrayPool.release(array1);
        
        // Borrow again - should get cleared array
        double[] array2 = ArrayPool.borrow(size);
        assertEquals(size, array2.length);
        
        // Verify array was cleared
        for (int i = 0; i < size; i++) {
            assertEquals(0.0, array2[i], "Array should be cleared before reuse");
        }
        
        ArrayPool.release(array2);
    }
    
    @Test
    @DisplayName("Periodic wavelet transform eliminates boundary checks")
    void testPeriodicTransformOptimization() {
        Wavelet wavelet = Daubechies.DB2;
        double[] signal = new double[256];
        
        // Generate test signal
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / 64.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Standard transform
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult standardResult = standard.forward(signal);
        double[] standardRecon = standard.inverse(standardResult);
        
        // Optimized periodic transform
        PeriodicWaveletTransform optimized = new PeriodicWaveletTransform(wavelet);
        TransformResult optimizedResult = optimized.forward(signal);
        double[] optimizedRecon = optimized.inverse(optimizedResult);
        
        // Results should be identical
        assertArrayEquals(standardResult.approximationCoeffs(), 
                         optimizedResult.approximationCoeffs(), EPSILON);
        assertArrayEquals(standardResult.detailCoeffs(), 
                         optimizedResult.detailCoeffs(), EPSILON);
        assertArrayEquals(standardRecon, optimizedRecon, EPSILON);
        assertArrayEquals(signal, optimizedRecon, 1e-10);
    }
    
    @Test
    @DisplayName("Forward-inverse optimization reduces intermediate allocations")
    void testForwardInverseOptimization() {
        WaveletTransformPool transform = new WaveletTransformPool(new Haar());
        
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = 100 + 10 * Math.sin(2 * Math.PI * i / 128.0);
        }
        
        // Direct forward-inverse
        double[] result = transform.forwardInverse(signal);
        
        // Should perfectly reconstruct
        assertArrayEquals(signal, result, 1e-10);
        
        // Compare with separate operations
        TransformResult fwdResult = transform.forward(signal);
        double[] separateResult = transform.inverse(fwdResult);
        
        assertArrayEquals(result, separateResult, EPSILON);
    }
    
    @Test
    @DisplayName("In-place transform for maximum efficiency")
    void testInPlaceTransform() {
        PeriodicWaveletTransform transform = new PeriodicWaveletTransform(new Haar());
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] originalSignal = signal.clone();
        
        // Perform in-place transform
        int coeffLength = transform.forwardInPlace(signal);
        
        assertEquals(4, coeffLength);
        
        // First half contains approximation coefficients
        double[] approxCoeffs = java.util.Arrays.copyOfRange(signal, 0, coeffLength);
        
        // Second half contains detail coefficients  
        double[] detailCoeffs = java.util.Arrays.copyOfRange(signal, coeffLength, 2 * coeffLength);
        
        // Verify results match standard transform
        TransformResult standardResult = transform.forward(originalSignal);
        
        assertArrayEquals(standardResult.approximationCoeffs(), approxCoeffs, EPSILON);
        assertArrayEquals(standardResult.detailCoeffs(), detailCoeffs, EPSILON);
        
        // Verify the in-place array contains both coefficient sets
        double[] expected = new double[8];
        System.arraycopy(standardResult.approximationCoeffs(), 0, expected, 0, 4);
        System.arraycopy(standardResult.detailCoeffs(), 0, expected, 4, 4);
        assertArrayEquals(expected, signal, EPSILON);
    }
    
    @Test
    @DisplayName("TransformResult creation for internal operations")
    void testTransformResultCreation() {
        PeriodicWaveletTransform transform = new PeriodicWaveletTransform(Daubechies.DB4);
        
        double[] approx = {1, 2, 3, 4};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        
        // Create result using internal method
        TransformResult result = transform.createResult(approx, detail);
        
        // Get coefficients (should be defensive copies)
        double[] returnedApprox = result.approximationCoeffs();
        double[] returnedDetail = result.detailCoeffs();
        
        // Should NOT be same arrays (defensive copies)
        assertNotSame(approx, returnedApprox);
        assertNotSame(detail, returnedDetail);
        
        // But values should be equal
        assertArrayEquals(approx, returnedApprox, EPSILON);
        assertArrayEquals(detail, returnedDetail, EPSILON);
    }
    
    @Test
    @DisplayName("Fast transform result creation bypasses validation")
    void testFastTransformResult() {
        // Test that fast result creation works with valid data
        double[] approx = {1, 2, 3, 4};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        
        // Use public API to create fast result through PeriodicWaveletTransform
        PeriodicWaveletTransform transform = new PeriodicWaveletTransform(new Haar());
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Forward transform should use fast result creation internally
        TransformResult result = transform.forward(signal);
        
        // Verify result is valid
        assertNotNull(result.approximationCoeffs());
        assertNotNull(result.detailCoeffs());
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
        
        // Verify reconstruction works
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, 1e-10);
    }
}