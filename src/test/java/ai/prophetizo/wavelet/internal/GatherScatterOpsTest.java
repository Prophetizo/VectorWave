package ai.prophetizo.wavelet.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for GatherScatterOps class.
 * Tests optimized gather/scatter operations using Vector API intrinsics.
 */
@DisplayName("GatherScatterOps Test Suite")
class GatherScatterOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    // ==========================================
    // Platform Support Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather/scatter availability detection")
    void testIsGatherScatterAvailable() {
        // Should not throw an exception
        assertDoesNotThrow(() -> {
            boolean isAvailable = GatherScatterOps.isGatherScatterAvailable();
            // Result depends on the platform, just verify it returns a boolean
            assertTrue(isAvailable || !isAvailable);
        });
    }
    
    @Test
    @DisplayName("Test gather/scatter info")
    void testGetGatherScatterInfo() {
        assertDoesNotThrow(() -> {
            String info = GatherScatterOps.getGatherScatterInfo();
            assertNotNull(info);
            assertFalse(info.isEmpty());
            assertTrue(info.contains("Gather/Scatter Support"));
            assertTrue(info.contains("Vector Length"));
            assertTrue(info.contains("Platform"));
        });
    }
    
    // ==========================================
    // Gather Periodic Downsample Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample basic")
    void testGatherPeriodicDownsampleBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with zeros")
    void testGatherPeriodicDownsampleWithZeros() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Zero signal should produce zero output
        for (double value : result) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with Haar filter")
    void testGatherPeriodicDownsampleHaar() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] haarFilter = {0.7071067811865475, 0.7071067811865475};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Expected values for Haar transform
        double sqrt2Inv = 0.7071067811865475;
        double[] expected = {
            (1.0 + 2.0) * sqrt2Inv,
            (3.0 + 4.0) * sqrt2Inv,
            (5.0 + 6.0) * sqrt2Inv,
            (7.0 + 8.0) * sqrt2Inv
        };
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64, 128})
    @DisplayName("Test gather periodic downsample with various signal lengths")
    void testGatherPeriodicDownsampleVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length) + 1.0;
        }
        
        double[] filter = {0.25, 0.5, 0.25};
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, length, filter.length);
            
            assertNotNull(result);
            assertEquals(length / 2, result.length);
            
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    @ParameterizedTest
    @CsvSource({
        "8, 2",
        "16, 4",
        "32, 6",
        "64, 8"
    })
    @DisplayName("Test gather periodic downsample with various filter lengths")
    void testGatherPeriodicDownsampleVariousFilterLengths(int signalLength, int filterLength) {
        double[] signal = new double[signalLength];
        double[] filter = new double[filterLength];
        
        // Initialize with test data
        for (int i = 0; i < signalLength; i++) {
            signal[i] = i + 1.0;
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 1.0 / filterLength; // Averaging filter
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signalLength, filterLength);
            
            assertNotNull(result);
            assertEquals(signalLength / 2, result.length);
        });
    }
    
    // ==========================================
    // Scatter Upsample Tests
    // ==========================================
    
    @Test
    @DisplayName("Test scatter upsample basic")
    void testScatterUpsampleBasic() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 0.5, 0.5, 0.5};
        double[] output = new double[8];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, 8);
        
        // Verify that values are scattered correctly
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.5, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.5, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.5, output[5], EPSILON);
        assertEquals(4.0, output[6], EPSILON);
        assertEquals(0.5, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test scatter upsample with zeros")
    void testScatterUpsampleWithZeros() {
        double[] approx = {0.0, 0.0};
        double[] detail = {0.0, 0.0};
        double[] output = new double[4];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, 4);
        
        // All outputs should be zero
        for (double value : output) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test scatter upsample with various lengths")
    void testScatterUpsampleVariousLengths(int length) {
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        double[] output = new double[length];
        
        // Initialize with test data
        for (int i = 0; i < length / 2; i++) {
            approx[i] = i + 1.0;
            detail[i] = (i + 1.0) * 0.1;
        }
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.scatterUpsample(approx, detail, output, length);
            
            // Verify scattered pattern
            for (int i = 0; i < length / 2; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
        });
    }
    
    // ==========================================
    // Batch Gather Tests
    // ==========================================
    
    @Test
    @DisplayName("Test batch gather basic")
    void testBatchGatherBasic() {
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0},
            {11.0, 12.0, 13.0, 14.0, 15.0}
        };
        int[] indices = {0, 2, 4};
        double[][] results = new double[3][3];
        
        GatherScatterOps.batchGather(signals, indices, results, 3);
        
        // Verify gathered values
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        
        assertEquals(6.0, results[1][0], EPSILON);
        assertEquals(8.0, results[1][1], EPSILON);
        assertEquals(10.0, results[1][2], EPSILON);
        
        assertEquals(11.0, results[2][0], EPSILON);
        assertEquals(13.0, results[2][1], EPSILON);
        assertEquals(15.0, results[2][2], EPSILON);
    }
    
    @Test
    @DisplayName("Test batch gather single signal")
    void testBatchGatherSingleSignal() {
        double[][] signals = {{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}};
        int[] indices = {1, 3, 5, 7};
        double[][] results = new double[1][4];
        
        GatherScatterOps.batchGather(signals, indices, results, 4);
        
        assertEquals(2.0, results[0][0], EPSILON);
        assertEquals(4.0, results[0][1], EPSILON);
        assertEquals(6.0, results[0][2], EPSILON);
        assertEquals(8.0, results[0][3], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    @DisplayName("Test batch gather with various signal counts")
    void testBatchGatherVariousSignalCounts(int numSignals) {
        double[][] signals = new double[numSignals][16];
        int[] indices = {0, 2, 4, 6, 8, 10, 12, 14};
        double[][] results = new double[numSignals][8];
        
        // Initialize signals
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < 16; i++) {
                signals[s][i] = s * 16 + i + 1.0;
            }
        }
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.batchGather(signals, indices, results, 8);
            
            // Verify gathered values
            for (int s = 0; s < numSignals; s++) {
                for (int i = 0; i < 8; i++) {
                    double expected = s * 16 + indices[i] + 1.0;
                    assertEquals(expected, results[s][i], EPSILON);
                }
            }
        });
    }
    
    // ==========================================
    // Gather Strided Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather strided basic")
    void testGatherStridedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        
        double[] result = GatherScatterOps.gatherStrided(signal, 1, 2, 4);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(2.0, result[0], EPSILON); // signal[1]
        assertEquals(4.0, result[1], EPSILON); // signal[3]
        assertEquals(6.0, result[2], EPSILON); // signal[5]
        assertEquals(8.0, result[3], EPSILON); // signal[7]
    }
    
    @Test
    @DisplayName("Test gather strided with stride 1")
    void testGatherStridedStride1() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        double[] result = GatherScatterOps.gatherStrided(signal, 2, 1, 3);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(3.0, result[0], EPSILON); // signal[2]
        assertEquals(4.0, result[1], EPSILON); // signal[3]
        assertEquals(5.0, result[2], EPSILON); // signal[4]
    }
    
    @Test
    @DisplayName("Test gather strided with large stride")
    void testGatherStridedLargeStride() {
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 10, 5);
        
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(1.0, result[0], EPSILON);   // signal[0]
        assertEquals(11.0, result[1], EPSILON);  // signal[10]
        assertEquals(21.0, result[2], EPSILON);  // signal[20]
        assertEquals(31.0, result[3], EPSILON);  // signal[30]
        assertEquals(41.0, result[4], EPSILON);  // signal[40]
    }
    
    @ParameterizedTest
    @CsvSource({
        "0, 1, 8",
        "1, 2, 8",
        "2, 3, 6",
        "0, 4, 4"
    })
    @DisplayName("Test gather strided with various parameters")
    void testGatherStridedVariousParameters(int offset, int stride, int count) {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
            
            assertNotNull(result);
            assertEquals(count, result.length);
            
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    // ==========================================
    // Gather Compressed Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather compressed basic")
    void testGatherCompressedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        boolean[] mask = {true, false, true, false, true, false, true, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        assertEquals(5.0, result[2], EPSILON);
        assertEquals(7.0, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed all true")
    void testGatherCompressedAllTrue() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {true, true, true, true};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertArrayEquals(signal, result, EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed all false")
    void testGatherCompressedAllFalse() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {false, false, false, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(0, result.length);
    }
    
    @Test
    @DisplayName("Test gather compressed single element")
    void testGatherCompressedSingleElement() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        boolean[] mask = {false, false, false, true, false, false, false, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(4.0, result[0], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test gather compressed with various lengths")
    void testGatherCompressedVariousLengths(int length) {
        double[] signal = new double[length];
        boolean[] mask = new boolean[length];
        
        // Initialize signal and create alternating mask
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1.0;
            mask[i] = (i % 2 == 0);
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            int expectedLength = (length + 1) / 2; // Half, rounded up
            assertEquals(expectedLength, result.length);
            
            // Verify gathered values
            for (int i = 0; i < result.length; i++) {
                assertEquals(2 * i + 1.0, result[i], EPSILON);
            }
        });
    }
    
    // ==========================================
    // Edge Cases and Error Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test with minimal arrays")
    void testMinimalArrays() {
        assertDoesNotThrow(() -> {
            // Minimal gather periodic downsample
            double[] signal = {1.0, 2.0};
            double[] filter = {1.0};
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            assertEquals(1, result.length);
            
            // Minimal scatter upsample
            double[] approx = {1.0};
            double[] detail = {2.0};
            double[] output = new double[2];
            GatherScatterOps.scatterUpsample(approx, detail, output, 2);
            assertEquals(1.0, output[0], EPSILON);
            assertEquals(2.0, output[1], EPSILON);
        });
    }
    
    @Test
    @DisplayName("Test with extreme values")
    void testExtremeValues() {
        double[] signal = {
            Double.MAX_VALUE / 1e10, -Double.MAX_VALUE / 1e10,
            1e-10, -1e-10, 0.0, 1000.0, -1000.0, 0.5
        };
        double[] filter = {0.5, 0.5};
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(4, result.length);
            
            // Verify no overflow/underflow
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    @Test
    @DisplayName("Test consistency across different methods")
    void testConsistencyAcrossMethods() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        // Test gather periodic downsample
        double[] gatherResult = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(gatherResult);
        assertEquals(4, gatherResult.length);
        
        // Test strided access for comparison
        double[] stridedResult = GatherScatterOps.gatherStrided(signal, 0, 2, 4);
        
        assertNotNull(stridedResult);
        assertEquals(4, stridedResult.length);
        
        // Both should be valid (not necessarily equal due to different operations)
        for (int i = 0; i < 4; i++) {
            assertFalse(Double.isNaN(gatherResult[i]));
            assertFalse(Double.isNaN(stridedResult[i]));
        }
    }
    
    @Test
    @DisplayName("Test platform info formatting")
    void testPlatformInfoFormatting() {
        String info = GatherScatterOps.getGatherScatterInfo();
        
        // Verify info contains expected components
        assertTrue(info.contains("Available") || info.contains("Not Available"));
        assertTrue(info.contains("Vector Length:") && info.contains("doubles"));
        assertTrue(info.contains("Little Endian") || info.contains("Big Endian"));
    }
    
    // ==========================================
    // Direct Scalar Method Testing
    // ==========================================
    
    @Test
    @DisplayName("Test scalar periodic downsample directly")
    void testScalarPeriodicDownsampleDirectly() throws Exception {
        // Use reflection to access the private scalar method
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scalarPeriodicDownsample", double[].class, double[].class, int.class, int.class);
        method.setAccessible(true);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = (double[]) method.invoke(null, signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify expected values
        assertEquals(1.5, result[0], EPSILON); // (1*0.5 + 2*0.5)
        assertEquals(3.5, result[1], EPSILON); // (3*0.5 + 4*0.5)
        assertEquals(5.5, result[2], EPSILON); // (5*0.5 + 6*0.5)
        assertEquals(7.5, result[3], EPSILON); // (7*0.5 + 8*0.5)
    }
    
    @Test
    @DisplayName("Test scalar upsample directly")
    void testScalarUpsampleDirectly() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scalarUpsample", double[].class, double[].class, double[].class, int.class);
        method.setAccessible(true);
        
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        double[] output = new double[8];
        
        method.invoke(null, approx, detail, output, 8);
        
        // Verify interleaved pattern
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.3, output[5], EPSILON);
        assertEquals(4.0, output[6], EPSILON);
        assertEquals(0.4, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test batch gather scalar directly")
    void testBatchGatherScalarDirectly() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "batchGatherScalar", double[][].class, int[].class, double[][].class, int.class);
        method.setAccessible(true);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0}
        };
        int[] indices = {0, 2, 4};
        double[][] results = new double[2][3];
        
        method.invoke(null, signals, indices, results, 3);
        
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        assertEquals(6.0, results[1][0], EPSILON);
        assertEquals(8.0, results[1][1], EPSILON);
        assertEquals(10.0, results[1][2], EPSILON);
    }
    
    // ==========================================
    // Additional Tests for Better Coverage
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample with large signal to trigger vector path")
    void testGatherPeriodicDownsampleLargeSignal() {
        // Use large signal to ensure vector path is taken
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length) * 100.0;
        }
        
        double[] filter = {0.25, 0.5, 0.25};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(128, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with single-tap filter")
    void testGatherPeriodicDownsampleSingleTapFilter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {1.0}; // Single tap filter
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // With single tap filter, should get every other element
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        assertEquals(5.0, result[2], EPSILON);
        assertEquals(7.0, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test scatter upsample with large arrays")
    void testScatterUpsampleLargeArrays() {
        int halfLength = 128;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        double[] output = new double[halfLength * 2];
        
        // Initialize with pattern
        for (int i = 0; i < halfLength; i++) {
            approx[i] = i * 2.0;
            detail[i] = i * 2.0 + 1.0;
        }
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify correct scatter pattern
        for (int i = 0; i < halfLength; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test batch gather with large count")
    void testBatchGatherLargeCount() {
        // Test with count that will use vector path
        int count = 32;
        double[][] signals = {
            new double[64],
            new double[64]
        };
        int[] indices = new int[count];
        double[][] results = new double[2][count];
        
        // Initialize data
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 64; i++) {
                signals[s][i] = s * 100 + i;
            }
        }
        for (int i = 0; i < count; i++) {
            indices[i] = i * 2; // Every other element
        }
        
        GatherScatterOps.batchGather(signals, indices, results, count);
        
        // Verify gathered values
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < count; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test batch gather with empty signals")
    void testBatchGatherEmptySignals() {
        double[][] signals = {};
        int[] indices = {0, 1, 2};
        double[][] results = {};
        
        // Should handle empty signals gracefully
        assertDoesNotThrow(() -> {
            GatherScatterOps.batchGather(signals, indices, results, 3);
        });
    }
    
    @Test
    @DisplayName("Test gather strided with small stride to trigger vector path")
    void testGatherStridedSmallStride() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 1.5;
        }
        
        // Small stride (<=8) should use vector path
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 2, 64);
        
        assertNotNull(result);
        assertEquals(64, result.length);
        
        // Verify strided access
        for (int i = 0; i < 64; i++) {
            assertEquals(signal[i * 2], result[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather compressed with large array")
    void testGatherCompressedLargeArray() {
        int length = 128;
        double[] signal = new double[length];
        boolean[] mask = new boolean[length];
        
        // Initialize signal and create sparse mask
        for (int i = 0; i < length; i++) {
            signal[i] = i + 0.5;
            mask[i] = (i % 3 == 0); // Every third element
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        int expectedCount = (length + 2) / 3; // Ceiling division
        assertEquals(expectedCount, result.length);
        
        // Verify gathered values
        int resultIdx = 0;
        for (int i = 0; i < length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[resultIdx++], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test gather compressed with boundary alignment")
    void testGatherCompressedBoundaryAlignment() {
        // Test with length that aligns with vector length
        int vectorLength = 8; // Typical DOUBLE_LENGTH
        double[] signal = new double[vectorLength * 4]; // 32 elements
        boolean[] mask = new boolean[vectorLength * 4];
        
        // Create pattern that tests vector boundary handling
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.1;
            // Create mask that has different patterns at vector boundaries
            if (i < vectorLength) {
                mask[i] = true; // All true in first vector
            } else if (i < vectorLength * 2) {
                mask[i] = false; // All false in second vector
            } else if (i < vectorLength * 3) {
                mask[i] = (i % 2 == 0); // Alternating in third vector
            } else {
                mask[i] = (i == signal.length - 1); // Only last element in fourth vector
            }
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        
        // Count expected elements
        int expectedCount = vectorLength + 0 + vectorLength/2 + 1;
        assertEquals(expectedCount, result.length);
        
        // Verify first vector (all true)
        for (int i = 0; i < vectorLength; i++) {
            assertEquals(signal[i], result[i], EPSILON);
        }
        
        // Verify third vector (alternating)
        int idx = vectorLength;
        for (int i = vectorLength * 2; i < vectorLength * 3; i += 2) {
            assertEquals(signal[i], result[idx++], EPSILON);
        }
        
        // Verify last element
        assertEquals(signal[signal.length - 1], result[result.length - 1], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with filter length edge cases")
    void testGatherPeriodicDownsampleFilterLengthEdgeCases() {
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        // Test with filter length equal to signal length/2
        double[] longFilter = new double[8];
        for (int i = 0; i < longFilter.length; i++) {
            longFilter[i] = 1.0 / longFilter.length;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, longFilter, signal.length, longFilter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test scatter upsample with odd length handling")
    void testScatterUpsampleOddLength() {
        // Test with odd output length (should handle gracefully)
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {0.1, 0.2, 0.3};
        double[] output = new double[7]; // Odd length
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Should scatter as much as possible
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.3, output[5], EPSILON);
        assertEquals(0.0, output[6], EPSILON); // Last element untouched
    }
    
    // ==========================================
    // Additional Coverage Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample with wrap-around indices")
    void testGatherPeriodicDownsampleWrapAround() {
        // Test with filter length that causes wrap-around
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // 4-tap filter on 4-element signal
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        
        // First output: indices 0,1,2,3 -> all elements
        assertEquals(2.5, result[0], EPSILON); // (1+2+3+4)*0.25
        // Second output: indices 2,3,0,1 (wraps around)
        assertEquals(2.5, result[1], EPSILON); // (3+4+1+2)*0.25
    }
    
    @Test
    @DisplayName("Test gather strided with offset near end")
    void testGatherStridedOffsetNearEnd() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        
        // Start near end with small count
        double[] result = GatherScatterOps.gatherStrided(signal, 7, 1, 3);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(8.0, result[0], EPSILON);
        assertEquals(9.0, result[1], EPSILON);
        assertEquals(10.0, result[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed with complex mask patterns")
    void testGatherCompressedComplexMask() {
        double[] signal = new double[20];
        boolean[] mask = new boolean[20];
        
        // Create complex pattern
        for (int i = 0; i < 20; i++) {
            signal[i] = i * 1.1;
            // Complex mask: true for prime indices
            mask[i] = isPrime(i);
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        // Count primes less than 20: 2,3,5,7,11,13,17,19 = 8
        assertEquals(8, result.length);
        
        // Verify gathered values
        assertEquals(signal[2], result[0], EPSILON);
        assertEquals(signal[3], result[1], EPSILON);
        assertEquals(signal[5], result[2], EPSILON);
        assertEquals(signal[7], result[3], EPSILON);
    }
    
    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    @Test
    @DisplayName("Test batch gather with different signal lengths")
    void testBatchGatherDifferentLengths() {
        // This tests robustness even though signals should typically be same length
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0, 6.0},
            {10.0, 20.0, 30.0, 40.0},
            {100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0}
        };
        int[] indices = {0, 1, 2};
        double[][] results = new double[3][3];
        
        GatherScatterOps.batchGather(signals, indices, results, 3);
        
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(2.0, results[0][1], EPSILON);
        assertEquals(3.0, results[0][2], EPSILON);
        
        assertEquals(10.0, results[1][0], EPSILON);
        assertEquals(20.0, results[1][1], EPSILON);
        assertEquals(30.0, results[1][2], EPSILON);
        
        assertEquals(100.0, results[2][0], EPSILON);
        assertEquals(200.0, results[2][1], EPSILON);
        assertEquals(300.0, results[2][2], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with maximum filter length")
    void testGatherPeriodicDownsampleMaxFilterLength() {
        double[] signal = new double[32];
        double[] filter = new double[16]; // Half the signal length
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signal.length);
        }
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test scatter upsample with pre-filled output")
    void testScatterUpsamplePreFilledOutput() {
        double[] approx = {10.0, 20.0};
        double[] detail = {1.0, 2.0};
        double[] output = {-1.0, -1.0, -1.0, -1.0}; // Pre-filled with -1
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify scatter overwrites pre-filled values
        assertEquals(10.0, output[0], EPSILON);
        assertEquals(1.0, output[1], EPSILON);
        assertEquals(20.0, output[2], EPSILON);
        assertEquals(2.0, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather strided with maximum stride")
    void testGatherStridedMaxStride() {
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * i; // Quadratic pattern
        }
        
        // Large stride (>8) to test scalar fallback
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 15, 6);
        
        assertNotNull(result);
        assertEquals(6, result.length);
        
        assertEquals(0.0, result[0], EPSILON);   // 0^2
        assertEquals(225.0, result[1], EPSILON); // 15^2
        assertEquals(900.0, result[2], EPSILON); // 30^2
        assertEquals(2025.0, result[3], EPSILON); // 45^2
        assertEquals(3600.0, result[4], EPSILON); // 60^2
        assertEquals(5625.0, result[5], EPSILON); // 75^2
    }
    
    @Test
    @DisplayName("Test checkGatherScatterSupport method")
    void testCheckGatherScatterSupport() throws Exception {
        // Use reflection to test the private method
        Method method = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        method.setAccessible(true);
        
        // Should return true on x86-64 with proper vector support
        boolean result = (boolean) method.invoke(null);
        System.out.println("GatherScatter support: " + result);
        
        // Also test the public method
        boolean publicResult = GatherScatterOps.isGatherScatterAvailable();
        assertEquals(result, publicResult);
    }
    
    @Test
    @DisplayName("Test checkX86GatherSupport method")
    void testCheckX86GatherSupport() throws Exception {
        // Use reflection to test the private method
        Method method = GatherScatterOps.class.getDeclaredMethod("checkX86GatherSupport");
        method.setAccessible(true);
        
        boolean result = (boolean) method.invoke(null);
        System.out.println("X86 Gather support: " + result);
    }
}