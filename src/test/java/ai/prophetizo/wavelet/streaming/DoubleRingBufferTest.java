package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DoubleRingBuffer implementation.
 * Tests focus on correctness, thread safety patterns, and performance characteristics.
 */
class DoubleRingBufferTest {
    
    private DoubleRingBuffer buffer;
    
    @BeforeEach
    void setUp() {
        buffer = new DoubleRingBuffer(8); // Small buffer for testing
    }
    
    @Test
    @DisplayName("Constructor should require power of 2 capacity")
    void testConstructorValidation() {
        // Valid power of 2 capacities
        assertDoesNotThrow(() -> new DoubleRingBuffer(1));
        assertDoesNotThrow(() -> new DoubleRingBuffer(2));
        assertDoesNotThrow(() -> new DoubleRingBuffer(4));
        assertDoesNotThrow(() -> new DoubleRingBuffer(1024));
        
        // Invalid capacities
        assertThrows(IllegalArgumentException.class, () -> new DoubleRingBuffer(0));
        assertThrows(IllegalArgumentException.class, () -> new DoubleRingBuffer(-1));
        assertThrows(IllegalArgumentException.class, () -> new DoubleRingBuffer(3));
        assertThrows(IllegalArgumentException.class, () -> new DoubleRingBuffer(5));
        assertThrows(IllegalArgumentException.class, () -> new DoubleRingBuffer(7));
    }
    
    @Test
    @DisplayName("Empty buffer should behave correctly")
    void testEmptyBuffer() {
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(0, buffer.size());
        assertEquals(8, buffer.capacity());
        
        assertTrue(Double.isNaN(buffer.poll()));
        assertTrue(Double.isNaN(buffer.peek()));
        assertTrue(Double.isNaN(buffer.peek(0)));
        assertTrue(Double.isNaN(buffer.peek(1)));
    }
    
    @Test
    @DisplayName("Single element operations should work correctly")
    void testSingleElement() {
        assertTrue(buffer.offer(42.0));
        
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(1, buffer.size());
        
        assertEquals(42.0, buffer.peek());
        assertEquals(42.0, buffer.peek(0));
        assertTrue(Double.isNaN(buffer.peek(1)));
        
        assertEquals(42.0, buffer.poll());
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
    }
    
    @Test
    @DisplayName("Buffer should handle fill and drain correctly")
    void testFillAndDrain() {
        // Fill buffer to capacity
        for (int i = 0; i < 8; i++) {
            assertTrue(buffer.offer(i * 1.0), "Should be able to add element " + i);
            assertEquals(i + 1, buffer.size());
        }
        
        assertTrue(buffer.isFull());
        assertFalse(buffer.isEmpty());
        
        // Buffer should reject additional elements when full
        assertFalse(buffer.offer(99.0));
        
        // Verify peek operations
        for (int i = 0; i < 8; i++) {
            assertEquals(i * 1.0, buffer.peek(i), "Peek at offset " + i);
        }
        
        // Drain buffer
        for (int i = 0; i < 8; i++) {
            assertEquals(i * 1.0, buffer.poll(), "Poll element " + i);
            assertEquals(7 - i, buffer.size());
        }
        
        assertTrue(buffer.isEmpty());
        assertTrue(Double.isNaN(buffer.poll()));
    }
    
    @Test
    @DisplayName("Buffer should handle wrap-around correctly")
    void testWrapAround() {
        // Fill buffer partially
        for (int i = 0; i < 5; i++) {
            buffer.offer(i * 1.0);
        }
        
        // Remove some elements
        for (int i = 0; i < 3; i++) {
            assertEquals(i * 1.0, buffer.poll());
        }
        
        // Add more elements (should wrap around)
        for (int i = 5; i < 8; i++) {
            buffer.offer(i * 1.0);
        }
        
        // Verify correct order
        assertEquals(3.0, buffer.poll());
        assertEquals(4.0, buffer.poll());
        assertEquals(5.0, buffer.poll());
        assertEquals(6.0, buffer.poll());
        assertEquals(7.0, buffer.poll());
        
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @DisplayName("Peek with offset should handle bounds correctly")
    void testPeekOffsetBounds() {
        // Add some elements
        buffer.offer(1.0);
        buffer.offer(2.0);
        buffer.offer(3.0);
        
        assertEquals(1.0, buffer.peek(0));
        assertEquals(2.0, buffer.peek(1));
        assertEquals(3.0, buffer.peek(2));
        assertTrue(Double.isNaN(buffer.peek(3)));
        
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.peek(-1));
    }
    
    @Test
    @DisplayName("Buffer segments should provide zero-copy access")
    void testBufferSegments() {
        // Add test data
        for (int i = 0; i < 5; i++) {
            buffer.offer(i * 10.0);
        }
        
        // Get a segment
        DoubleRingBuffer.DoubleBufferSegment segment = buffer.getSegment(1, 3);
        
        assertEquals(3, segment.length());
        assertTrue(segment.isValid());
        
        assertEquals(10.0, segment.get(0));
        assertEquals(20.0, segment.get(1));
        assertEquals(30.0, segment.get(2));
        
        assertThrows(IndexOutOfBoundsException.class, () -> segment.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> segment.get(3));
    }
    
    @Test
    @DisplayName("Buffer segments should handle bounds correctly")
    void testBufferSegmentBounds() {
        buffer.offer(1.0);
        buffer.offer(2.0);
        
        // Valid segments
        assertDoesNotThrow(() -> buffer.getSegment(0, 1));
        assertDoesNotThrow(() -> buffer.getSegment(0, 2));
        assertDoesNotThrow(() -> buffer.getSegment(1, 1));
        
        // Invalid segments
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getSegment(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> buffer.getSegment(0, 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.getSegment(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getSegment(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getSegment(2, 1));
    }
    
    @Test
    @DisplayName("Buffer segment copyTo should work correctly")
    void testBufferSegmentCopyTo() {
        // Add test data
        for (int i = 0; i < 4; i++) {
            buffer.offer(i * 5.0);
        }
        
        DoubleRingBuffer.DoubleBufferSegment segment = buffer.getSegment(1, 2);
        
        double[] destination = new double[5];
        segment.copyTo(destination, 1);
        
        assertEquals(0.0, destination[0]); // Unchanged
        assertEquals(5.0, destination[1]); // Copied
        assertEquals(10.0, destination[2]); // Copied
        assertEquals(0.0, destination[3]); // Unchanged
        assertEquals(0.0, destination[4]); // Unchanged
        
        // Test bounds checking
        assertThrows(IllegalArgumentException.class, () -> segment.copyTo(null, 0));
        assertThrows(IllegalArgumentException.class, () -> segment.copyTo(new double[1], 0));
        assertThrows(IllegalArgumentException.class, () -> segment.copyTo(destination, -1));
        assertThrows(IllegalArgumentException.class, () -> segment.copyTo(destination, 4));
    }
    
    @Test
    @DisplayName("Clear should reset buffer state")
    void testClear() {
        // Add some data
        for (int i = 0; i < 5; i++) {
            buffer.offer(i * 1.0);
        }
        
        assertFalse(buffer.isEmpty());
        
        buffer.clear();
        
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertTrue(Double.isNaN(buffer.poll()));
        assertTrue(Double.isNaN(buffer.peek()));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("Buffer should work correctly with various power-of-2 sizes")
    void testVariousSizes(int capacity) {
        DoubleRingBuffer testBuffer = new DoubleRingBuffer(capacity);
        
        assertEquals(capacity, testBuffer.capacity());
        assertTrue(testBuffer.isEmpty());
        assertFalse(testBuffer.isFull());
        
        // Fill to capacity
        for (int i = 0; i < capacity; i++) {
            assertTrue(testBuffer.offer(i * 1.0));
        }
        
        assertTrue(testBuffer.isFull());
        assertEquals(capacity, testBuffer.size());
        
        // Verify all elements
        for (int i = 0; i < capacity; i++) {
            assertEquals(i * 1.0, testBuffer.peek(i));
        }
        
        // Drain all elements
        for (int i = 0; i < capacity; i++) {
            assertEquals(i * 1.0, testBuffer.poll());
        }
        
        assertTrue(testBuffer.isEmpty());
    }
    
    @Test
    @DisplayName("Buffer should handle concurrent-like access patterns")
    void testConcurrentLikePatterns() {
        // Simulate alternating produce/consume pattern
        for (int round = 0; round < 5; round++) {
            // Produce some elements
            for (int i = 0; i < 2; i++) {
                assertTrue(buffer.offer(round * 10 + i));
            }
            
            // Consume one element (net gain of 1 per round)
            double value = buffer.poll();
            assertFalse(Double.isNaN(value));
        }
        
        // Buffer should have some remaining elements
        assertFalse(buffer.isEmpty());
        assertEquals(5, buffer.size()); // (2-1) * 5 rounds
    }
}