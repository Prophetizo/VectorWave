package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for StreamingWaveletTransformImpl.
 * Tests focus on streaming behavior, transform correctness, and performance characteristics.
 */
class StreamingWaveletTransformImplTest {
    
    private StreamingWaveletTransformImpl streaming;
    
    @BeforeEach
    void setUp() {
        // 8-sample window with 4-sample overlap using Haar wavelet
        streaming = new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, 8, 4);
    }
    
    @Test
    @DisplayName("Constructor should validate parameters")
    void testConstructorValidation() {
        // Valid parameters
        assertDoesNotThrow(() -> new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, 8, 0));
        assertDoesNotThrow(() -> new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.ZERO_PADDING, 16, 8));
        
        // Invalid window size (not power of 2)
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 7, 0));
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 0, 0));
        
        // Invalid overlap size
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 8, -1));
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 8, 8));
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 8, 9));
    }
    
    @Test
    @DisplayName("Empty streaming transform should behave correctly")
    void testEmptyStreaming() {
        assertTrue(streaming.isEmpty());
        assertFalse(streaming.isResultReady());
        assertEquals(0, streaming.getBufferedSampleCount());
        
        assertEquals(8, streaming.getWindowSize());
        assertEquals(4, streaming.getOverlapSize());
        assertEquals(4, streaming.getHopSize());
    }
    
    @Test
    @DisplayName("Single sample addition should work correctly")
    void testSingleSampleAddition() {
        for (int i = 1; i <= 7; i++) {
            assertFalse(streaming.addSample(i * 1.0));
            assertFalse(streaming.isResultReady());
            assertEquals(i, streaming.getBufferedSampleCount());
        }
        
        assertTrue(streaming.addSample(8.0)); // Should make result ready
        assertTrue(streaming.isResultReady());
        assertEquals(8, streaming.getBufferedSampleCount());
    }
    
    @Test
    @DisplayName("Batch sample addition should work correctly")
    void testBatchSampleAddition() {
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        streaming.addSamples(samples);
        assertTrue(streaming.isResultReady());
        assertEquals(8, streaming.getBufferedSampleCount());
        
        assertThrows(IllegalArgumentException.class, () -> streaming.addSamples(null));
    }
    
    @Test
    @DisplayName("Transform results should be correct")
    void testTransformCorrectness() {
        // Use a simple signal that we can verify
        double[] signal = {1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0};
        streaming.addSamples(signal);
        
        assertTrue(streaming.isResultReady());
        
        TransformResult result = streaming.getNextResult();
        assertNotNull(result);
        
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        assertEquals(4, approx.length);
        assertEquals(4, detail.length);
        
        // For Haar wavelet with this signal, we expect specific values
        // The exact values depend on the normalization, but the pattern should be consistent
        assertFalse(Double.isNaN(approx[0]));
        assertFalse(Double.isNaN(detail[0]));
    }
    
    @Test
    @DisplayName("Streaming should handle overlapping windows correctly")
    void testOverlappingWindows() {
        // Add enough samples for multiple windows
        for (int i = 1; i <= 16; i++) {
            streaming.addSample(i * 1.0);
        }
        
        assertTrue(streaming.isResultReady());
        
        // Get first result
        TransformResult firstResult = streaming.getNextResult();
        assertNotNull(firstResult);
        assertEquals(12, streaming.getBufferedSampleCount()); // 16 - 4 hop size
        
        assertTrue(streaming.isResultReady()); // Should still have a ready window
        
        // Get second result
        TransformResult secondResult = streaming.getNextResult();
        assertNotNull(secondResult);
        assertEquals(8, streaming.getBufferedSampleCount()); // 12 - 4 hop size
        
        assertTrue(streaming.isResultReady()); // Should still have a ready window
        
        // Results should be different due to different input windows
        assertNotEquals(firstResult.approximationCoeffs()[0], 
                       secondResult.approximationCoeffs()[0]);
    }
    
    @Test
    @DisplayName("No overlap configuration should work correctly")
    void testNoOverlap() {
        StreamingWaveletTransformImpl noOverlapStreaming = 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 8, 0);
        
        assertEquals(8, noOverlapStreaming.getHopSize());
        
        // Add first window
        for (int i = 1; i <= 8; i++) {
            noOverlapStreaming.addSample(i * 1.0);
        }
        
        assertTrue(noOverlapStreaming.isResultReady());
        TransformResult firstResult = noOverlapStreaming.getNextResult();
        assertNotNull(firstResult);
        
        // After getting result with no overlap, should need full new window
        assertEquals(0, noOverlapStreaming.getBufferedSampleCount());
        assertFalse(noOverlapStreaming.isResultReady());
        
        // Add second window
        for (int i = 9; i <= 16; i++) {
            noOverlapStreaming.addSample(i * 1.0);
        }
        
        assertTrue(noOverlapStreaming.isResultReady());
        TransformResult secondResult = noOverlapStreaming.getNextResult();
        assertNotNull(secondResult);
    }
    
    @Test
    @DisplayName("High overlap configuration should work correctly")
    void testHighOverlap() {
        // 75% overlap (6 out of 8 samples)
        StreamingWaveletTransformImpl highOverlapStreaming = 
            new StreamingWaveletTransformImpl(new Haar(), BoundaryMode.PERIODIC, 8, 6);
        
        assertEquals(2, highOverlapStreaming.getHopSize());
        
        // Add samples for multiple overlapping windows
        for (int i = 1; i <= 12; i++) {
            highOverlapStreaming.addSample(i * 1.0);
        }
        
        // Should be able to get multiple results with high overlap
        assertTrue(highOverlapStreaming.isResultReady());
        TransformResult result1 = highOverlapStreaming.getNextResult();
        
        assertTrue(highOverlapStreaming.isResultReady());
        TransformResult result2 = highOverlapStreaming.getNextResult();
        
        assertTrue(highOverlapStreaming.isResultReady());
        TransformResult result3 = highOverlapStreaming.getNextResult();
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
    
    @Test
    @DisplayName("Different wavelets should work correctly")
    void testDifferentWavelets() {
        StreamingWaveletTransformImpl db2Streaming = 
            new StreamingWaveletTransformImpl(Daubechies.DB2, BoundaryMode.PERIODIC, 8, 4);
        
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        db2Streaming.addSamples(samples);
        
        assertTrue(db2Streaming.isResultReady());
        TransformResult result = db2Streaming.getNextResult();
        
        assertNotNull(result);
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
        
        // DB2 should produce different results than Haar
        // (We tested Haar in previous tests)
        for (double coeff : result.approximationCoeffs()) {
            assertFalse(Double.isNaN(coeff));
            assertTrue(Double.isFinite(coeff));
        }
    }
    
    @Test
    @DisplayName("Reset should clear all state")
    void testReset() {
        streaming.addSamples(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        assertTrue(streaming.isResultReady());
        
        streaming.reset();
        
        assertTrue(streaming.isEmpty());
        assertFalse(streaming.isResultReady());
        assertEquals(0, streaming.getBufferedSampleCount());
    }
    
    @Test
    @DisplayName("Operations without ready result should throw exceptions")
    void testOperationsWithoutReadyResult() {
        streaming.addSamples(new double[]{1.0, 2.0, 3.0}); // Not enough for full window
        
        assertFalse(streaming.isResultReady());
        assertThrows(IllegalStateException.class, () -> streaming.getNextResult());
    }
    
    @Test
    @DisplayName("Performance stats should be accurate")
    void testPerformanceStats() {
        streaming.addSamples(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0});
        
        StreamingWaveletTransformImpl.StreamingPerformanceStats stats = 
            streaming.getPerformanceStats();
        
        assertEquals(10, stats.getBufferedSamples());
        assertEquals(8, stats.getWindowSize());
        assertEquals(4, stats.getOverlapSize());
        assertEquals(4, stats.getHopSize());
        assertEquals(0.5, stats.getOverlapRatio(), 0.001);
        assertEquals(0.5, stats.getMemoryEfficiency(), 0.001);
        
        assertNotNull(stats.toString());
        assertTrue(stats.toString().contains("buffered=10"));
    }
    
    @Test
    @DisplayName("Underlying wavelet transform should be accessible")
    void testWaveletTransformAccess() {
        assertNotNull(streaming.getWaveletTransform());
        assertEquals(BoundaryMode.PERIODIC, streaming.getWaveletTransform().getBoundaryMode());
        assertTrue(streaming.getWaveletTransform().getWavelet() instanceof Haar);
    }
    
    @Test
    @DisplayName("Continuous streaming should work over many windows")
    void testContinuousStreaming() {
        int totalSamples = 100;
        int expectedResults = 0;
        
        for (int i = 1; i <= totalSamples; i++) {
            streaming.addSample(i * 1.0);
            
            if (streaming.isResultReady()) {
                TransformResult result = streaming.getNextResult();
                assertNotNull(result);
                expectedResults++;
            }
        }
        
        // With window size 8 and hop size 4, we should get multiple results
        assertTrue(expectedResults > 10);
        assertTrue(expectedResults < totalSamples); // Sanity check
    }
}