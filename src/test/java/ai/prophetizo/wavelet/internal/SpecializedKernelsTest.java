package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Symlet;
import ai.prophetizo.wavelet.api.Haar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SpecializedKernelsTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    void testDB4ForwardOptimized() {
        // Test against standard implementation
        double[] signal = createTestSignal(128);
        double[] approx = new double[64];
        double[] detail = new double[64];
        
        // Use specialized kernel
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Compare with standard transform
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult expected = transform.forward(signal);
        
        // Verify results match
        assertArrayEquals(expected.approximationCoeffs(), approx, EPSILON);
        assertArrayEquals(expected.detailCoeffs(), detail, EPSILON);
    }
    
    @Test
    void testDB4WithSmallSignals() {
        // Test with minimum size
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Verify outputs are non-zero
        boolean hasNonZeroApprox = false;
        boolean hasNonZeroDetail = false;
        
        for (double val : approx) {
            if (Math.abs(val) > EPSILON) hasNonZeroApprox = true;
        }
        for (double val : detail) {
            if (Math.abs(val) > EPSILON) hasNonZeroDetail = true;
        }
        
        assertTrue(hasNonZeroApprox);
        assertTrue(hasNonZeroDetail);
    }
    
    @Test
    void testSym4ForwardOptimized() {
        double[] signal = createTestSignal(64);
        double[] approx = new double[32];
        double[] detail = new double[32];
        
        // Use specialized kernel
        SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Compare with standard transform
        WaveletTransform transform = new WaveletTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
        TransformResult expected = transform.forward(signal);
        
        // Verify results match
        assertArrayEquals(expected.approximationCoeffs(), approx, EPSILON);
        assertArrayEquals(expected.detailCoeffs(), detail, EPSILON);
    }
    
    @Test
    void testHaarBatchOptimized() {
        // Create batch of signals
        int numSignals = 8;
        int signalLength = 64;
        double[][] signals = new double[numSignals][signalLength];
        double[][] approx = new double[numSignals][signalLength / 2];
        double[][] detail = new double[numSignals][signalLength / 2];
        
        // Fill with test data
        Random random = new Random(42);
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = random.nextGaussian();
            }
        }
        
        // Process batch
        SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        
        // Verify each signal
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        for (int i = 0; i < numSignals; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approx[i], EPSILON);
            assertArrayEquals(expected.detailCoeffs(), detail[i], EPSILON);
        }
    }
    
    @Test
    void testHaarBatchWithNonMultipleOfFour() {
        // Test with 5 signals (not multiple of 4)
        int numSignals = 5;
        int signalLength = 32;
        double[][] signals = new double[numSignals][signalLength];
        double[][] approx = new double[numSignals][signalLength / 2];
        double[][] detail = new double[numSignals][signalLength / 2];
        
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 8.0);
            }
        }
        
        SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        
        // Verify remainder signals are processed correctly
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult expected = transform.forward(signals[4]);
        assertArrayEquals(expected.approximationCoeffs(), approx[4], EPSILON);
        assertArrayEquals(expected.detailCoeffs(), detail[4], EPSILON);
    }
    
    @Test
    void testConvolveWithPrefetch() {
        double[] signal = createTestSignal(256);
        double[] filter = {0.1, 0.2, 0.3, 0.4};
        
        double[] result = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        
        assertEquals(128, result.length);
        
        // Verify convolution manually for first few elements
        for (int i = 0; i < 5; i++) {
            double expected = 0.0;
            int base = 2 * i;
            for (int k = 0; k < filter.length; k++) {
                expected += filter[k] * signal[(base + k) % signal.length];
            }
            assertEquals(expected, result[i], EPSILON);
        }
    }
    
    @Test
    void testConvolvePrecomputedIndices() {
        double[] signal = createTestSignal(128);
        double[] filter = {0.5, -0.5, 0.25, -0.25, 0.125, -0.125};
        
        double[] result = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        assertEquals(64, result.length);
        
        // Compare with prefetch version
        double[] prefetchResult = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        
        assertArrayEquals(prefetchResult, result, EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256, 512})
    void testDB4VariousSizes(int size) {
        double[] signal = createTestSignal(size);
        double[] approx = new double[size / 2];
        double[] detail = new double[size / 2];
        
        assertDoesNotThrow(() -> 
            SpecializedKernels.db4ForwardOptimized(signal, approx, detail, size)
        );
        
        // Verify energy preservation (Parseval's theorem)
        double signalEnergy = 0.0;
        double transformEnergy = 0.0;
        
        for (double val : signal) {
            signalEnergy += val * val;
        }
        
        for (double val : approx) {
            transformEnergy += val * val;
        }
        for (double val : detail) {
            transformEnergy += val * val;
        }
        
        // Energy should be approximately preserved
        assertEquals(signalEnergy, transformEnergy, signalEnergy * 0.001);
    }
    
    @Test
    void testConvolveWithLargeFilter() {
        double[] signal = createTestSignal(64);
        double[] filter = new double[16]; // Large filter
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length; // Simple averaging
        }
        
        double[] result1 = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        double[] result2 = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        // Both methods should give same result
        assertArrayEquals(result1, result2, EPSILON);
    }
    
    @Test
    void testBoundaryHandling() {
        // Test that periodic boundary is handled correctly
        double[] signal = new double[8];
        for (int i = 0; i < 8; i++) {
            signal[i] = i + 1; // 1, 2, 3, 4, 5, 6, 7, 8
        }
        
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, 8);
        
        // With periodic boundary, last coefficient should use wrapped values
        // Verify no out-of-bounds access occurred
        assertDoesNotThrow(() -> {
            for (double val : approx) {
                assertTrue(Double.isFinite(val));
            }
            for (double val : detail) {
                assertTrue(Double.isFinite(val));
            }
        });
    }
    
    @Test
    void testKernelConsistency() {
        // Test that all kernels produce valid transforms
        double[] signal = createTestSignal(128);
        
        // DB4
        double[] db4Approx = new double[64];
        double[] db4Detail = new double[64];
        SpecializedKernels.db4ForwardOptimized(signal, db4Approx, db4Detail, 128);
        
        // Sym4
        double[] sym4Approx = new double[64];
        double[] sym4Detail = new double[64];
        SpecializedKernels.sym4ForwardOptimized(signal, sym4Approx, sym4Detail, 128);
        
        // Haar batch (single signal)
        double[][] haarSignals = {signal};
        double[][] haarApprox = new double[1][64];
        double[][] haarDetail = new double[1][64];
        SpecializedKernels.haarBatchOptimized(haarSignals, haarApprox, haarDetail);
        
        // All should produce valid non-trivial results
        assertTrue(hasSignificantValues(db4Approx));
        assertTrue(hasSignificantValues(db4Detail));
        assertTrue(hasSignificantValues(sym4Approx));
        assertTrue(hasSignificantValues(sym4Detail));
        assertTrue(hasSignificantValues(haarApprox[0]));
        assertTrue(hasSignificantValues(haarDetail[0]));
    }
    
    // Helper methods
    
    private double[] createTestSignal(int length) {
        Random random = new Random(12345); // Fixed seed for reproducibility
        double[] signal = new double[length];
        
        // Mix of sine waves and noise
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 8.0) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}