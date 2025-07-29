package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VectorOpsPooledTest {
    
    private static final double EPSILON = 1e-10;
    private double[] haarFilter;
    private double[] db4Filter;
    
    @BeforeEach
    void setUp() {
        haarFilter = new Haar().lowPassDecomposition();
        db4Filter = Daubechies.DB4.lowPassDecomposition();
    }
    
    @Test
    void testConvolveAndDownsamplePeriodicPooled() {
        double[] signal = createTestSignal(64);
        
        double[] result = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertEquals(32, result.length);
        
        // Compare with standard implementation
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testBatchConvolveAndDownsample() {
        int batchSize = 10;
        int signalLength = 128;
        double[][] signals = new double[batchSize][signalLength];
        
        // Fill with test data
        Random random = new Random(42);
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = random.nextGaussian();
            }
        }
        
        double[][] results = VectorOpsPooled.batchConvolveAndDownsample(
            signals, db4Filter, signalLength, db4Filter.length);
        
        assertEquals(batchSize, results.length);
        assertEquals(signalLength / 2, results[0].length);
        
        // Verify each signal
        for (int i = 0; i < batchSize; i++) {
            double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
                signals[i], db4Filter, signalLength, db4Filter.length);
            assertArrayEquals(expected, results[i], EPSILON);
        }
    }
    
    @Test
    void testCacheAwareConvolution() {
        // Test with large signal
        double[] signal = createTestSignal(16384); // 16K samples
        
        double[] result = VectorOpsPooled.convolveAndDownsampleCacheAware(
            signal, db4Filter, signal.length, db4Filter.length);
        
        assertEquals(8192, result.length);
        
        // Verify correctness
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, db4Filter, signal.length, db4Filter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testComplexSoAConvolution() {
        int signalLength = 256;
        double[] signalReal = new double[signalLength];
        double[] signalImag = new double[signalLength];
        
        // Create complex signal
        for (int i = 0; i < signalLength; i++) {
            signalReal[i] = Math.cos(2 * Math.PI * i / 32.0);
            signalImag[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Real filter with zero imaginary part
        double[] filterReal = haarFilter;
        double[] filterImag = new double[haarFilter.length]; // All zeros
        
        double[] outputReal = new double[signalLength / 2];
        double[] outputImag = new double[signalLength / 2];
        
        VectorOpsPooled.convolveAndDownsampleSoA(
            signalReal, signalImag, filterReal, filterImag,
            outputReal, outputImag, signalLength, filterReal.length);
        
        // Verify real part matches standard convolution
        double[] expectedReal = ScalarOps.convolveAndDownsamplePeriodic(
            signalReal, filterReal, signalLength, filterReal.length);
        
        assertArrayEquals(expectedReal, outputReal, EPSILON);
        
        // Imaginary part should also be properly convolved
        double[] expectedImag = ScalarOps.convolveAndDownsamplePeriodic(
            signalImag, filterReal, signalLength, filterReal.length);
        
        assertArrayEquals(expectedImag, outputImag, EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256, 512, 1024})
    void testVariousSignalSizes(int size) {
        double[] signal = createTestSignal(size);
        
        double[] result = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            signal, db4Filter, size, db4Filter.length);
        
        assertEquals(size / 2, result.length);
        
        // Verify non-trivial result
        assertTrue(hasSignificantValues(result));
    }
    
    @Test
    void testSmallBatchProcessing() {
        // Test with batch smaller than chunk size
        int batchSize = 3;
        int signalLength = 64;
        double[][] signals = createTestSignals(batchSize, signalLength);
        
        double[][] results = VectorOpsPooled.batchConvolveAndDownsample(
            signals, haarFilter, signalLength, haarFilter.length);
        
        assertEquals(batchSize, results.length);
        
        // Verify each result
        for (int i = 0; i < batchSize; i++) {
            assertEquals(signalLength / 2, results[i].length);
            assertTrue(hasSignificantValues(results[i]));
        }
    }
    
    @Test
    void testPowerOfTwoOptimization() {
        // Test with power-of-2 signal length (optimized with & instead of %)
        double[] signal = createTestSignal(256);
        
        double[] result = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            signal, db4Filter, signal.length, db4Filter.length);
        
        // Verify correctness
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, db4Filter, signal.length, db4Filter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testComplexFilterConvolution() {
        int signalLength = 128;
        double[] signalReal = createTestSignal(signalLength);
        double[] signalImag = createTestSignal(signalLength);
        
        // Complex filter
        double[] filterReal = new double[]{0.5, -0.5};
        double[] filterImag = new double[]{0.25, -0.25};
        
        double[] outputReal = new double[signalLength / 2];
        double[] outputImag = new double[signalLength / 2];
        
        VectorOpsPooled.convolveAndDownsampleSoA(
            signalReal, signalImag, filterReal, filterImag,
            outputReal, outputImag, signalLength, 2);
        
        // Verify complex multiplication was performed correctly
        // Manual check of first output value
        double expectedReal0 = (signalReal[0] * filterReal[0] - signalImag[0] * filterImag[0]) +
                              (signalReal[1] * filterReal[1] - signalImag[1] * filterImag[1]);
        double expectedImag0 = (signalReal[0] * filterImag[0] + signalImag[0] * filterReal[0]) +
                              (signalReal[1] * filterImag[1] + signalImag[1] * filterReal[1]);
        
        assertEquals(expectedReal0, outputReal[0], EPSILON);
        assertEquals(expectedImag0, outputImag[0], EPSILON);
    }
    
    @Test
    void testLargeBatchProcessing() {
        // Test with batch larger than chunk size
        int batchSize = 20;
        int signalLength = 64;
        double[][] signals = createTestSignals(batchSize, signalLength);
        
        double[][] results = VectorOpsPooled.batchConvolveAndDownsample(
            signals, db4Filter, signalLength, db4Filter.length);
        
        assertEquals(batchSize, results.length);
        
        // Spot check a few signals
        for (int i : new int[]{0, batchSize/2, batchSize-1}) {
            double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
                signals[i], db4Filter, signalLength, db4Filter.length);
            assertArrayEquals(expected, results[i], EPSILON);
        }
    }
    
    @Test
    void testVeryLargeSignalCacheAware() {
        // Test with very large signal to verify cache blocking
        double[] signal = createTestSignal(65536); // 64K samples
        
        double[] result = VectorOpsPooled.convolveAndDownsampleCacheAware(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertEquals(32768, result.length);
        
        // Verify result is non-trivial and has correct size
        assertTrue(hasSignificantValues(result));
        
        // Spot check first few values against expected
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, signal.length, haarFilter.length);
        
        // Check first 10 values
        for (int i = 0; i < 10; i++) {
            assertEquals(expected[i], result[i], EPSILON);
        }
    }
    
    @Test
    void testPartialVectorHandling() {
        // Test with size that doesn't divide evenly by vector length
        double[] signal = createTestSignal(65); // Odd size
        
        // Pad to power of 2 for periodic boundary optimization
        double[] paddedSignal = new double[128];
        System.arraycopy(signal, 0, paddedSignal, 0, 65);
        
        double[] result = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            paddedSignal, haarFilter, paddedSignal.length, haarFilter.length);
        
        assertEquals(64, result.length);
        assertTrue(hasSignificantValues(result));
    }
    
    // Helper methods
    
    private double[] createTestSignal(int length) {
        Random random = new Random(12345);
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 16.0) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private double[][] createTestSignals(int count, int length) {
        double[][] signals = new double[count][length];
        Random random = new Random(54321);
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < length; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 16.0 + i * Math.PI / 8) +
                               0.1 * random.nextGaussian();
            }
        }
        
        return signals;
    }
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}