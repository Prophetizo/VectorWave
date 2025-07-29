package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for OverlapBuffer implementation.
 * Tests focus on overlapping window management and zero-copy optimization.
 */
class OverlapBufferTest {
    
    private OverlapBuffer buffer;
    
    @BeforeEach
    void setUp() {
        // 8-sample window with 4-sample overlap (50% overlap)
        buffer = new OverlapBuffer(8, 4);
    }
    
    @Test
    @DisplayName("Constructor should validate parameters")
    void testConstructorValidation() {
        // Valid parameters
        assertDoesNotThrow(() -> new OverlapBuffer(8, 0)); // No overlap
        assertDoesNotThrow(() -> new OverlapBuffer(8, 4)); // 50% overlap
        assertDoesNotThrow(() -> new OverlapBuffer(8, 7)); // 87.5% overlap
        
        // Invalid parameters
        assertThrows(IllegalArgumentException.class, () -> new OverlapBuffer(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new OverlapBuffer(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new OverlapBuffer(8, -1));
        assertThrows(IllegalArgumentException.class, () -> new OverlapBuffer(8, 8));
        assertThrows(IllegalArgumentException.class, () -> new OverlapBuffer(8, 9));
    }
    
    @Test
    @DisplayName("Empty buffer should behave correctly")
    void testEmptyBuffer() {
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertFalse(buffer.isWindowReady());
        
        assertEquals(8, buffer.getWindowSize());
        assertEquals(4, buffer.getOverlapSize());
        assertEquals(4, buffer.getHopSize());
    }
    
    @Test
    @DisplayName("Single sample addition should work correctly")
    void testSingleSampleAddition() {
        assertFalse(buffer.addSample(1.0));
        assertEquals(1, buffer.size());
        assertFalse(buffer.isWindowReady());
        
        // Add samples until window is ready
        for (int i = 2; i <= 7; i++) {
            assertFalse(buffer.addSample(i * 1.0));
        }
        
        assertTrue(buffer.addSample(8.0)); // Should make window ready
        assertTrue(buffer.isWindowReady());
        assertEquals(8, buffer.size());
    }
    
    @Test
    @DisplayName("Batch sample addition should work correctly")
    void testBatchSampleAddition() {
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        assertDoesNotThrow(() -> buffer.addSamples(samples));
        assertTrue(buffer.isWindowReady());
        assertEquals(8, buffer.size());
        
        assertThrows(IllegalArgumentException.class, () -> buffer.addSamples(null));
    }
    
    @Test
    @DisplayName("Window extraction should provide correct data")
    void testWindowExtraction() {
        double[] samples = {10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0};
        buffer.addSamples(samples);
        
        assertTrue(buffer.isWindowReady());
        
        double[] window = buffer.getCurrentWindow();
        assertArrayEquals(samples, window);
        
        // Window should be a defensive copy
        window[0] = 999.0;
        double[] window2 = buffer.getCurrentWindow();
        assertEquals(10.0, window2[0]); // Original value should be unchanged
    }
    
    @Test
    @DisplayName("Window segment should provide zero-copy access")
    void testWindowSegment() {
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        buffer.addSamples(samples);
        
        DoubleRingBuffer.DoubleBufferSegment segment = buffer.getCurrentWindowSegment();
        
        assertEquals(8, segment.length());
        assertTrue(segment.isValid());
        
        for (int i = 0; i < 8; i++) {
            assertEquals(samples[i], segment.get(i));
        }
    }
    
    @Test
    @DisplayName("Window advance should create proper overlap")
    void testWindowAdvance() {
        // First window: [1, 2, 3, 4, 5, 6, 7, 8]
        double[] firstWindow = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        buffer.addSamples(firstWindow);
        
        assertTrue(buffer.isWindowReady());
        assertArrayEquals(firstWindow, buffer.getCurrentWindow());
        
        // Advance (removes 4 samples, keeps 4 for overlap)
        buffer.advance();
        assertEquals(4, buffer.size()); // Should have 4 samples remaining
        assertFalse(buffer.isWindowReady()); // Need 4 more samples
        
        // Add 4 more samples for next window
        // Next window should be: [5, 6, 7, 8, 9, 10, 11, 12]
        double[] additionalSamples = {9.0, 10.0, 11.0, 12.0};
        buffer.addSamples(additionalSamples);
        
        assertTrue(buffer.isWindowReady());
        double[] secondWindow = buffer.getCurrentWindow();
        double[] expectedSecondWindow = {5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0};
        assertArrayEquals(expectedSecondWindow, secondWindow);
    }
    
    @Test
    @DisplayName("Multiple window advances should maintain correct overlap")
    void testMultipleAdvances() {
        // Test streaming behavior step by step
        // Add first window worth of samples
        double[] firstBatch = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        buffer.addSamples(firstBatch);
        
        // First window: [1, 2, 3, 4, 5, 6, 7, 8]
        double[] firstWindow = buffer.getCurrentWindow();
        assertArrayEquals(firstBatch, firstWindow);
        
        buffer.advance(); // Removes first 4 samples, keeps [5, 6, 7, 8]
        
        // Add 4 more samples for next window
        double[] secondBatch = {9.0, 10.0, 11.0, 12.0};
        buffer.addSamples(secondBatch);
        
        // Second window should be: [5, 6, 7, 8, 9, 10, 11, 12]
        double[] secondWindow = buffer.getCurrentWindow();
        double[] expectedSecond = {5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0};
        assertArrayEquals(expectedSecond, secondWindow);
        
        buffer.advance(); // Removes [5, 6, 7, 8], keeps [9, 10, 11, 12]
        
        // Add 4 more samples for third window
        double[] thirdBatch = {13.0, 14.0, 15.0, 16.0};
        buffer.addSamples(thirdBatch);
        
        // Third window should be: [9, 10, 11, 12, 13, 14, 15, 16]
        double[] thirdWindow = buffer.getCurrentWindow();
        double[] expectedThird = {9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
        assertArrayEquals(expectedThird, thirdWindow);
    }
    
    @Test
    @DisplayName("No overlap configuration should work correctly")
    void testNoOverlap() {
        OverlapBuffer noOverlapBuffer = new OverlapBuffer(4, 0);
        
        assertEquals(4, noOverlapBuffer.getWindowSize());
        assertEquals(0, noOverlapBuffer.getOverlapSize());
        assertEquals(4, noOverlapBuffer.getHopSize());
        
        // Add first window
        double[] firstSamples = {1.0, 2.0, 3.0, 4.0};
        noOverlapBuffer.addSamples(firstSamples);
        
        assertTrue(noOverlapBuffer.isWindowReady());
        assertArrayEquals(firstSamples, noOverlapBuffer.getCurrentWindow());
        
        noOverlapBuffer.advance();
        
        // After advance with no overlap, buffer should be empty
        assertEquals(0, noOverlapBuffer.size());
        assertFalse(noOverlapBuffer.isWindowReady());
        
        // Add second window
        double[] secondSamples = {5.0, 6.0, 7.0, 8.0};
        noOverlapBuffer.addSamples(secondSamples);
        
        assertTrue(noOverlapBuffer.isWindowReady());
        assertArrayEquals(secondSamples, noOverlapBuffer.getCurrentWindow());
    }
    
    @Test
    @DisplayName("High overlap configuration should work correctly")
    void testHighOverlap() {
        // 75% overlap (6 out of 8 samples)
        OverlapBuffer highOverlapBuffer = new OverlapBuffer(8, 6);
        
        assertEquals(8, highOverlapBuffer.getWindowSize());
        assertEquals(6, highOverlapBuffer.getOverlapSize());
        assertEquals(2, highOverlapBuffer.getHopSize());
        
        // Add first window
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        highOverlapBuffer.addSamples(samples);
        
        assertArrayEquals(samples, highOverlapBuffer.getCurrentWindow());
        
        highOverlapBuffer.advance();
        
        // Should have 6 samples remaining after advance
        assertEquals(6, highOverlapBuffer.size());
        
        // Add 2 more samples for next window
        highOverlapBuffer.addSamples(new double[]{9.0, 10.0});
        
        double[] secondWindow = highOverlapBuffer.getCurrentWindow();
        double[] expectedSecond = {3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        assertArrayEquals(expectedSecond, secondWindow);
    }
    
    @Test
    @DisplayName("Clear should reset buffer state")
    void testClear() {
        buffer.addSamples(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        assertTrue(buffer.isWindowReady());
        
        buffer.clear();
        
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertFalse(buffer.isWindowReady());
    }
    
    @Test
    @DisplayName("Operations on empty window should throw appropriate exceptions")
    void testEmptyWindowOperations() {
        assertThrows(IllegalStateException.class, () -> buffer.getCurrentWindow());
        assertThrows(IllegalStateException.class, () -> buffer.getCurrentWindowSegment());
        assertThrows(IllegalStateException.class, () -> buffer.advance());
    }
    
    @Test
    @DisplayName("Advance without ready window should throw exception")
    void testAdvanceWithoutReadyWindow() {
        buffer.addSamples(new double[]{1.0, 2.0, 3.0}); // Not enough for full window
        
        assertFalse(buffer.isWindowReady());
        assertThrows(IllegalStateException.class, () -> buffer.advance());
    }
    
    @Test
    @DisplayName("Buffer should handle edge case window sizes")
    void testEdgeCaseWindowSizes() {
        // Minimum window size
        OverlapBuffer minBuffer = new OverlapBuffer(1, 0);
        assertEquals(1, minBuffer.getWindowSize());
        assertEquals(0, minBuffer.getOverlapSize());
        assertEquals(1, minBuffer.getHopSize());
        
        minBuffer.addSample(42.0);
        assertTrue(minBuffer.isWindowReady());
        assertArrayEquals(new double[]{42.0}, minBuffer.getCurrentWindow());
        
        // Large window size (power of 2)
        assertDoesNotThrow(() -> new OverlapBuffer(1024, 512));
    }
}