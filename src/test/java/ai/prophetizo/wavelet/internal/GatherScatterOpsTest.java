package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GatherScatterOpsTest {
    
    private static final double EPSILON = 1e-10;
    private double[] haarFilter;
    private double[] db4Filter;
    
    @BeforeEach
    void setUp() {
        haarFilter = new Haar().lowPassDecomposition();
        db4Filter = Daubechies.DB4.lowPassDecomposition();
    }
    
    @Test
    void testGatherPeriodicDownsample() {
        double[] signal = createTestSignal(64);
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertEquals(32, result.length);
        
        // Compare with standard periodic downsampling
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testScatterUpsample() {
        int halfLength = 32;
        double[] approx = createTestSignal(halfLength);
        double[] detail = createTestSignal(halfLength);
        double[] output = new double[halfLength * 2];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, halfLength * 2);
        
        // Verify interleaving pattern
        for (int i = 0; i < halfLength; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    void testBatchGather() {
        int numSignals = 5;
        int signalLength = 100;
        int gatherCount = 20;
        
        // Create test signals
        double[][] signals = new double[numSignals][signalLength];
        Random random = new Random(42);
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = random.nextGaussian();
            }
        }
        
        // Create gather indices
        int[] indices = new int[gatherCount];
        for (int i = 0; i < gatherCount; i++) {
            indices[i] = random.nextInt(signalLength);
        }
        
        // Perform batch gather
        double[][] results = new double[numSignals][gatherCount];
        GatherScatterOps.batchGather(signals, indices, results, gatherCount);
        
        // Verify each signal's gathered values
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < gatherCount; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    void testGatherStrided() {
        double[] signal = createTestSignal(100);
        int offset = 5;
        int stride = 3;
        int count = 20;
        
        double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
        
        assertEquals(count, result.length);
        
        // Verify strided access
        for (int i = 0; i < count; i++) {
            assertEquals(signal[offset + i * stride], result[i], EPSILON);
        }
    }
    
    @Test
    void testGatherCompressed() {
        double[] signal = createTestSignal(50);
        boolean[] mask = new boolean[50];
        
        // Create sparse mask
        int expectedCount = 0;
        for (int i = 0; i < mask.length; i++) {
            mask[i] = (i % 3 == 0); // Every third element
            if (mask[i]) expectedCount++;
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertEquals(expectedCount, result.length);
        
        // Verify compressed values
        int resultIdx = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[resultIdx++], EPSILON);
            }
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256})
    void testVariousSignalSizes(int size) {
        double[] signal = createTestSignal(size);
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, db4Filter, size, db4Filter.length);
        
        assertEquals(size / 2, result.length);
        
        // Verify non-trivial result
        assertTrue(hasSignificantValues(result));
    }
    
    @Test
    void testLargeSignalGather() {
        double[] signal = createTestSignal(8192);
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertEquals(4096, result.length);
        
        // Spot check some values
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, haarFilter, signal.length, haarFilter.length);
        
        for (int i = 0; i < 10; i++) {
            assertEquals(expected[i], result[i], EPSILON);
        }
    }
    
    @Test
    void testBoundaryWrapping() {
        // Test that gather properly handles boundary wrapping
        double[] signal = new double[8];
        for (int i = 0; i < 8; i++) {
            signal[i] = i + 1.0; // 1, 2, 3, 4, 5, 6, 7, 8
        }
        
        double[] filter = new double[]{0.5, 0.5}; // Simple averaging
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, 8, 2);
        
        assertEquals(4, result.length);
        
        // Check boundary wrapping at the end
        // Last output should average positions 6,7 and wrap to 0
        assertEquals((signal[6] * 0.5 + signal[7] * 0.5), result[3], EPSILON);
    }
    
    @Test
    void testComplexGatherPattern() {
        // Test with multiple gather operations in sequence
        double[] signal = createTestSignal(256);
        
        // First gather - downsample by 2
        double[] gather1 = GatherScatterOps.gatherPeriodicDownsample(
            signal, haarFilter, 256, haarFilter.length);
        assertEquals(128, gather1.length);
        
        // Second gather - strided access
        double[] gather2 = GatherScatterOps.gatherStrided(gather1, 0, 2, 64);
        assertEquals(64, gather2.length);
        
        // Third gather - compressed
        boolean[] mask = new boolean[64];
        for (int i = 0; i < 64; i++) {
            mask[i] = (i % 4 < 2); // Half the elements
        }
        double[] gather3 = GatherScatterOps.gatherCompressed(gather2, mask);
        assertEquals(32, gather3.length);
        
        // Verify results are meaningful
        assertTrue(hasSignificantValues(gather3));
    }
    
    @Test
    void testBatchGatherWithDifferentIndices() {
        int numSignals = 3;
        double[][] signals = new double[numSignals][];
        
        // Create signals of different patterns
        signals[0] = new double[50];
        signals[1] = new double[50];
        signals[2] = new double[50];
        
        for (int i = 0; i < 50; i++) {
            signals[0][i] = Math.sin(2 * Math.PI * i / 10.0);
            signals[1][i] = Math.cos(2 * Math.PI * i / 10.0);
            signals[2][i] = i * 0.1;
        }
        
        // Gather specific indices
        int[] indices = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45};
        double[][] results = new double[numSignals][indices.length];
        
        GatherScatterOps.batchGather(signals, indices, results, indices.length);
        
        // Verify gathered values
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < indices.length; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    void testScatterUpsampleEdgeCases() {
        // Test with very small signal
        double[] approx = new double[]{1.0, 2.0};
        double[] detail = new double[]{3.0, 4.0};
        double[] output = new double[4];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, 4);
        
        assertArrayEquals(new double[]{1.0, 3.0, 2.0, 4.0}, output, EPSILON);
    }
    
    @Test
    void testGatherScatterInfo() {
        String info = GatherScatterOps.getGatherScatterInfo();
        
        assertNotNull(info);
        assertTrue(info.contains("Gather/Scatter Support:"));
        assertTrue(info.contains("Vector Length:"));
        assertTrue(info.contains("Platform:"));
    }
    
    @Test
    void testIsGatherScatterAvailable() {
        boolean available = GatherScatterOps.isGatherScatterAvailable();
        // Should return false as the implementation sets it to false by default
        assertFalse(available);
    }
    
    @Test
    void testLargeStrideGather() {
        double[] signal = createTestSignal(1000);
        int offset = 10;
        int stride = 15; // Large stride
        int count = 50;
        
        double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
        
        assertEquals(count, result.length);
        
        // Verify with large stride
        for (int i = 0; i < count; i++) {
            int idx = offset + i * stride;
            if (idx < signal.length) {
                assertEquals(signal[idx], result[i], EPSILON);
            }
        }
    }
    
    @Test
    void testEmptyMaskCompression() {
        double[] signal = createTestSignal(20);
        boolean[] mask = new boolean[20]; // All false
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertEquals(0, result.length);
    }
    
    @Test
    void testFullMaskCompression() {
        double[] signal = createTestSignal(20);
        boolean[] mask = new boolean[20];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = true; // All true
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertArrayEquals(signal, result, EPSILON);
    }
    
    @Test
    void testGatherPeriodicDownsampleWithLargeFilter() {
        // Test with filter length > 4 to exercise different code paths
        double[] signal = createTestSignal(128);
        double[] largeFilter = new double[]{0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.05};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, largeFilter, signal.length, largeFilter.length);
        
        assertEquals(64, result.length);
        
        // Verify correctness
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, largeFilter, signal.length, largeFilter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testScatterUpsampleWithLargeArrays() {
        // Test with larger arrays to exercise vector processing paths
        int halfLength = 128;
        double[] approx = createTestSignal(halfLength);
        double[] detail = createTestSignal(halfLength);
        double[] output = new double[halfLength * 2];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, halfLength * 2);
        
        // Verify all elements were scattered correctly
        for (int i = 0; i < halfLength; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    void testBatchGatherWithVectorProcessing() {
        // Test batch gather with sizes that exercise vector processing
        int numSignals = 8;
        int signalLength = 256;
        int gatherCount = 64;
        
        double[][] signals = new double[numSignals][signalLength];
        Random random = new Random(123);
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = random.nextDouble();
            }
        }
        
        // Create gather indices
        int[] indices = new int[gatherCount];
        for (int i = 0; i < gatherCount; i++) {
            indices[i] = (i * 3) % signalLength;
        }
        
        double[][] results = new double[numSignals][gatherCount];
        GatherScatterOps.batchGather(signals, indices, results, gatherCount);
        
        // Verify all gathered values
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < gatherCount; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    void testGatherStridedWithVariousParameters() {
        double[] signal = createTestSignal(512);
        
        // Test with small stride (optimized path)
        double[] result1 = GatherScatterOps.gatherStrided(signal, 10, 2, 50);
        assertEquals(50, result1.length);
        for (int i = 0; i < 50; i++) {
            assertEquals(signal[10 + i * 2], result1[i], EPSILON);
        }
        
        // Test with large stride (fallback path)
        double[] result2 = GatherScatterOps.gatherStrided(signal, 5, 10, 30);
        assertEquals(30, result2.length);
        for (int i = 0; i < 30; i++) {
            assertEquals(signal[5 + i * 10], result2[i], EPSILON);
        }
        
        // Test with stride = 1
        double[] result3 = GatherScatterOps.gatherStrided(signal, 0, 1, 100);
        assertEquals(100, result3.length);
        for (int i = 0; i < 100; i++) {
            assertEquals(signal[i], result3[i], EPSILON);
        }
    }
    
    @Test
    void testGatherCompressedWithDenseMask() {
        double[] signal = createTestSignal(100);
        boolean[] mask = new boolean[100];
        
        // Create dense mask (most elements true)
        for (int i = 0; i < mask.length; i++) {
            mask[i] = (i % 10 != 0); // 90% true
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertEquals(90, result.length);
        
        // Verify compressed values
        int resultIdx = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[resultIdx++], EPSILON);
            }
        }
    }
    
    @Test
    void testGatherCompressedWithAlternatingMask() {
        double[] signal = createTestSignal(64);
        boolean[] mask = new boolean[64];
        
        // Create alternating mask
        for (int i = 0; i < mask.length; i++) {
            mask[i] = (i % 2 == 0);
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertEquals(32, result.length);
        
        // Verify only even indices were gathered
        for (int i = 0; i < 32; i++) {
            assertEquals(signal[i * 2], result[i], EPSILON);
        }
    }
    
    @Test
    void testPeriodicDownsampleWithVectorLength() {
        // Test with signal size that's multiple of vector length
        double[] signal = new double[16]; // Assuming vector length could be 2, 4, or 8
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] filter = new double[]{0.25, 0.5, 0.25};
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertEquals(8, result.length);
        
        // Verify with scalar implementation
        double[] expected = ScalarOps.convolveAndDownsamplePeriodic(
            signal, filter, signal.length, filter.length);
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    void testScatterUpsampleWithOddLength() {
        // Test scatter with odd half-length to ensure remainder handling
        int halfLength = 63;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        double[] output = new double[halfLength * 2];
        
        for (int i = 0; i < halfLength; i++) {
            approx[i] = i;
            detail[i] = -i;
        }
        
        GatherScatterOps.scatterUpsample(approx, detail, output, halfLength * 2);
        
        // Verify scatter pattern
        for (int i = 0; i < halfLength; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
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
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}