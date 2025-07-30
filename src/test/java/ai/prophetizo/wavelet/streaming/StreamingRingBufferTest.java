package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StreamingRingBuffer implementation.
 */
class StreamingRingBufferTest {
    
    @Test
    @DisplayName("Should create streaming ring buffer with valid parameters")
    void testValidConstruction() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 8);
        assertEquals(64, buffer.getCapacity());
        assertEquals(16, buffer.getWindowSize());
        assertEquals(8, buffer.getHopSize());
        assertEquals(8, buffer.getOverlapSize());
        assertFalse(buffer.hasWindow());
    }
    
    @Test
    @DisplayName("Should reject invalid construction parameters")
    void testInvalidConstruction() {
        // Invalid window size
        assertThrows(InvalidArgumentException.class, () -> new StreamingRingBuffer(64, 0, 8));
        assertThrows(InvalidArgumentException.class, () -> new StreamingRingBuffer(64, -1, 8));
        
        // Invalid hop size
        assertThrows(InvalidArgumentException.class, () -> new StreamingRingBuffer(64, 16, 0));
        assertThrows(InvalidArgumentException.class, () -> new StreamingRingBuffer(64, 16, 17));
        
        // Capacity too small
        assertThrows(InvalidArgumentException.class, () -> new StreamingRingBuffer(16, 16, 8));
    }
    
    @Test
    @DisplayName("Should detect when window is available")
    void testHasWindow() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 8);
        
        assertFalse(buffer.hasWindow());
        
        // Add insufficient data
        double[] data = new double[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        assertFalse(buffer.hasWindow());
        
        // Add more data to have a window
        buffer.write(data, 0, 6);
        assertTrue(buffer.hasWindow());
    }
    
    @Test
    @DisplayName("Should extract window correctly")
    void testGetWindow() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 8, 4);
        double[] window = new double[8];
        
        // Fill buffer with sequential data
        double[] data = new double[16];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Get first window
        assertTrue(buffer.getWindow(window));
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1.0, window[i]);
        }
        
        // Window should not advance position
        assertTrue(buffer.getWindow(window));
        assertEquals(1.0, window[0]);
    }
    
    @Test
    @DisplayName("Should get window direct without allocation")
    void testGetWindowDirect() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 8, 4);
        
        // No window available
        assertNull(buffer.getWindowDirect());
        
        // Fill buffer
        double[] data = new double[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Get window direct
        double[] window = buffer.getWindowDirect();
        assertNotNull(window);
        assertEquals(8, window.length);
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1.0, window[i]);
        }
        
        // Should return same array instance
        assertSame(window, buffer.getWindowDirect());
    }
    
    @Test
    @DisplayName("Should advance window by hop size")
    void testAdvanceWindow() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 8, 4);
        double[] window = new double[8];
        
        // Fill buffer
        double[] data = new double[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Get first window
        buffer.getWindow(window);
        assertEquals(1.0, window[0]);
        
        // Advance window
        assertTrue(buffer.advanceWindow());
        
        // Get next window - should start at hop size
        buffer.getWindow(window);
        assertEquals(5.0, window[0]);
        assertEquals(12.0, window[7]);
        
        // Advance again
        assertTrue(buffer.advanceWindow());
        buffer.getWindow(window);
        assertEquals(9.0, window[0]);
    }
    
    @Test
    @DisplayName("Should process windows with callback")
    void testProcessWindow() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 8, 4);
        List<Double> firstValues = new ArrayList<>();
        
        // Fill buffer
        double[] data = new double[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Process windows
        StreamingRingBuffer.WindowProcessor processor = (windowData, offset, length) -> {
            assertEquals(8, length);
            assertEquals(0, offset);
            firstValues.add(windowData[offset]);
        };
        
        assertTrue(buffer.processWindow(processor));
        assertTrue(buffer.processWindow(processor));
        assertTrue(buffer.processWindow(processor));
        assertFalse(buffer.processWindow(processor)); // Not enough data
        
        assertEquals(List.of(1.0, 5.0, 9.0), firstValues);
    }
    
    @Test
    @DisplayName("Should handle overlap correctly")
    void testGetOverlap() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 10);
        double[] overlap = new double[6]; // overlap size = 16 - 10 = 6
        
        // Fill buffer
        double[] data = new double[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Get overlap from current position
        assertTrue(buffer.getOverlap(overlap));
        for (int i = 0; i < 6; i++) {
            assertEquals(i + 1.0, overlap[i]);
        }
        
        // Advance window
        buffer.advanceWindow();
        
        // Get new overlap
        assertTrue(buffer.getOverlap(overlap));
        for (int i = 0; i < 6; i++) {
            assertEquals(i + 11.0, overlap[i]);
        }
    }
    
    @Test
    @DisplayName("Should handle no overlap case")
    void testNoOverlap() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 16); // hop = window, no overlap
        assertEquals(0, buffer.getOverlapSize());
        
        double[] overlap = new double[1];
        assertFalse(buffer.getOverlap(overlap));
    }
    
    @Test
    @DisplayName("Should fill for streaming correctly")
    void testFillForStreaming() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 8);
        
        double[] data = new double[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        
        // First fill - not enough for window
        assertFalse(buffer.fillForStreaming(data, 0, data.length));
        assertFalse(buffer.hasWindow());
        
        // Second fill - now have enough
        assertTrue(buffer.fillForStreaming(data, 0, 6));
        assertTrue(buffer.hasWindow());
    }
    
    @Test
    @DisplayName("Should handle wrap-around during window operations")
    void testWrapAroundWindowing() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(32, 8, 4);
        double[] window = new double[8];
        
        // Fill buffer near capacity
        double[] data = new double[30];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1.0;
        }
        buffer.write(data, 0, data.length);
        
        // Process windows until wrap-around occurs
        List<Double> firstValues = new ArrayList<>();
        while (buffer.available() >= 12) { // Keep some data for wrap test
            buffer.getWindow(window);
            firstValues.add(window[0]);
            buffer.advanceWindow();
        }
        
        // Add more data that will wrap
        double[] moreData = new double[10];
        for (int i = 0; i < moreData.length; i++) {
            moreData[i] = 31.0 + i;
        }
        buffer.write(moreData, 0, moreData.length);
        
        // Process wrapped window
        assertTrue(buffer.hasWindow());
        buffer.getWindow(window);
        
        // Verify window spans wrap boundary correctly
        boolean foundWrap = false;
        for (int i = 1; i < window.length; i++) {
            if (window[i] != window[i-1] + 1.0) {
                foundWrap = true;
                break;
            }
        }
        assertFalse(foundWrap, "Window should be contiguous despite wrap");
    }
    
    @Test
    @DisplayName("Should validate window output array")
    void testWindowArrayValidation() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 8);
        
        // Null array
        assertThrows(InvalidArgumentException.class, () -> buffer.getWindow(null));
        
        // Too small array
        assertThrows(InvalidArgumentException.class, () -> buffer.getWindow(new double[15]));
    }
    
    @Test
    @DisplayName("Should validate overlap output array")
    void testOverlapArrayValidation() {
        StreamingRingBuffer buffer = new StreamingRingBuffer(64, 16, 8);
        
        // Null array
        assertThrows(InvalidArgumentException.class, () -> buffer.getOverlap(null));
        
        // Too small array
        assertThrows(InvalidArgumentException.class, () -> buffer.getOverlap(new double[7]));
    }
}