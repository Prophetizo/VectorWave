package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RingBuffer implementation.
 */
class RingBufferTest {
    
    @Test
    @DisplayName("Should create ring buffer with valid power-of-2 capacity")
    void testValidConstruction() {
        RingBuffer buffer = new RingBuffer(16);
        assertEquals(16, buffer.getCapacity());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(0, buffer.available());
        assertEquals(15, buffer.remainingCapacity());
    }
    
    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 15, 17, 31, 33})
    @DisplayName("Should reject non-power-of-2 capacity")
    void testInvalidCapacity(int capacity) {
        assertThrows(InvalidArgumentException.class, () -> new RingBuffer(capacity));
    }
    
    @Test
    @DisplayName("Should reject capacity less than 2")
    void testTooSmallCapacity() {
        assertThrows(InvalidArgumentException.class, () -> new RingBuffer(1));
        assertThrows(InvalidArgumentException.class, () -> new RingBuffer(0));
        assertThrows(InvalidArgumentException.class, () -> new RingBuffer(-1));
    }
    
    @Test
    @DisplayName("Should write and read single values correctly")
    void testSingleValueOperations() {
        RingBuffer buffer = new RingBuffer(8);
        
        // Write values
        assertTrue(buffer.write(1.0));
        assertTrue(buffer.write(2.0));
        assertTrue(buffer.write(3.0));
        
        assertEquals(3, buffer.available());
        assertEquals(4, buffer.remainingCapacity());
        
        // Read values
        assertEquals(1.0, buffer.read());
        assertEquals(2.0, buffer.read());
        assertEquals(3.0, buffer.read());
        
        assertTrue(buffer.isEmpty());
        assertEquals(Double.NaN, buffer.read());
    }
    
    @Test
    @DisplayName("Should handle buffer wrap-around correctly")
    void testWrapAround() {
        RingBuffer buffer = new RingBuffer(4);
        
        // Fill buffer
        assertTrue(buffer.write(1.0));
        assertTrue(buffer.write(2.0));
        assertTrue(buffer.write(3.0));
        
        // Buffer is full (capacity - 1)
        assertFalse(buffer.write(4.0));
        assertTrue(buffer.isFull());
        
        // Read one value to make space
        assertEquals(1.0, buffer.read());
        
        // Now can write again
        assertTrue(buffer.write(4.0));
        
        // Read remaining values
        assertEquals(2.0, buffer.read());
        assertEquals(3.0, buffer.read());
        assertEquals(4.0, buffer.read());
        
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @DisplayName("Should write and read arrays correctly")
    void testArrayOperations() {
        RingBuffer buffer = new RingBuffer(16);
        double[] writeData = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] readData = new double[10];
        
        // Write array
        int written = buffer.write(writeData, 0, writeData.length);
        assertEquals(5, written);
        assertEquals(5, buffer.available());
        
        // Read partial array
        int read = buffer.read(readData, 0, 3);
        assertEquals(3, read);
        assertEquals(1.0, readData[0]);
        assertEquals(2.0, readData[1]);
        assertEquals(3.0, readData[2]);
        
        // Read remaining
        read = buffer.read(readData, 3, 2);
        assertEquals(2, read);
        assertEquals(4.0, readData[3]);
        assertEquals(5.0, readData[4]);
        
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @DisplayName("Should handle array wrap-around correctly")
    void testArrayWrapAround() {
        RingBuffer buffer = new RingBuffer(8);
        double[] data = new double[10];
        
        // Fill buffer near capacity
        for (int i = 0; i < 6; i++) {
            buffer.write(i + 1.0);
        }
        
        // Read 4 values to create wrap-around scenario
        for (int i = 0; i < 4; i++) {
            buffer.read();
        }
        
        // Write array that will wrap around
        double[] writeData = {7.0, 8.0, 9.0, 10.0, 11.0};
        int written = buffer.write(writeData, 0, writeData.length);
        assertEquals(5, written);
        
        // Read all values
        int read = buffer.read(data, 0, 7);
        assertEquals(7, read);
        assertArrayEquals(new double[]{5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 0.0, 0.0, 0.0}, data);
    }
    
    @Test
    @DisplayName("Should peek without advancing read position")
    void testPeek() {
        RingBuffer buffer = new RingBuffer(8);
        double[] data = new double[5];
        
        // Write values
        for (int i = 0; i < 5; i++) {
            buffer.write(i + 1.0);
        }
        
        // Peek at data
        int peeked = buffer.peek(data, 0, 3);
        assertEquals(3, peeked);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 0.0, 0.0}, data);
        
        // Available should not change
        assertEquals(5, buffer.available());
        
        // Read should still return first value
        assertEquals(1.0, buffer.read());
    }
    
    @Test
    @DisplayName("Should skip values correctly")
    void testSkip() {
        RingBuffer buffer = new RingBuffer(8);
        
        // Write values
        for (int i = 0; i < 5; i++) {
            buffer.write(i + 1.0);
        }
        
        // Skip 2 values
        int skipped = buffer.skip(2);
        assertEquals(2, skipped);
        assertEquals(3, buffer.available());
        
        // Next read should be third value
        assertEquals(3.0, buffer.read());
        
        // Try to skip more than available
        skipped = buffer.skip(5);
        assertEquals(2, skipped);
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @DisplayName("Should clear buffer correctly")
    void testClear() {
        RingBuffer buffer = new RingBuffer(8);
        
        // Fill with data
        for (int i = 0; i < 5; i++) {
            buffer.write(i + 1.0);
        }
        
        assertEquals(5, buffer.available());
        
        // Clear buffer
        buffer.clear();
        
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.available());
        assertEquals(7, buffer.remainingCapacity());
    }
    
    @Test
    @DisplayName("Should validate array parameters")
    void testArrayParameterValidation() {
        RingBuffer buffer = new RingBuffer(8);
        double[] data = new double[5];
        
        // Null array
        assertThrows(InvalidArgumentException.class, () -> buffer.write(null, 0, 1));
        assertThrows(InvalidArgumentException.class, () -> buffer.read(null, 0, 1));
        assertThrows(InvalidArgumentException.class, () -> buffer.peek(null, 0, 1));
        
        // Invalid offset
        assertThrows(InvalidArgumentException.class, () -> buffer.write(data, -1, 1));
        assertThrows(InvalidArgumentException.class, () -> buffer.write(data, 6, 1));
        
        // Invalid length
        assertThrows(InvalidArgumentException.class, () -> buffer.write(data, 0, -1));
        assertThrows(InvalidArgumentException.class, () -> buffer.write(data, 0, 6));
        assertThrows(InvalidArgumentException.class, () -> buffer.write(data, 2, 4));
    }
    
    @Test
    @DisplayName("Should handle concurrent single producer single consumer")
    void testConcurrentSPSC() throws InterruptedException {
        RingBuffer buffer = new RingBuffer(1024);
        int itemCount = 10000;
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean producerDone = new AtomicBoolean(false);
        AtomicInteger consumed = new AtomicInteger(0);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            for (int i = 0; i < itemCount; i++) {
                while (!buffer.write(i + 1.0)) {
                    Thread.yield();
                }
            }
            producerDone.set(true);
            latch.countDown();
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            int expected = 1;
            while (!producerDone.get() || !buffer.isEmpty()) {
                double value = buffer.read();
                if (!Double.isNaN(value)) {
                    assertEquals(expected++, value);
                    consumed.incrementAndGet();
                }
            }
            latch.countDown();
        });
        
        producer.start();
        consumer.start();
        
        latch.await();
        
        assertEquals(itemCount, consumed.get());
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @DisplayName("Should handle edge case of single-element operations at capacity")
    void testCapacityEdgeCases() {
        RingBuffer buffer = new RingBuffer(4);
        
        // Fill to capacity - 1
        assertTrue(buffer.write(1.0));
        assertTrue(buffer.write(2.0));
        assertTrue(buffer.write(3.0));
        
        // Should not be able to write more
        assertFalse(buffer.write(4.0));
        
        // Read and write in alternation
        assertEquals(1.0, buffer.read());
        assertTrue(buffer.write(4.0));
        assertEquals(2.0, buffer.read());
        assertTrue(buffer.write(5.0));
        assertEquals(3.0, buffer.read());
        assertTrue(buffer.write(6.0));
        
        // Should have 3 elements: 4.0, 5.0, 6.0
        assertEquals(3, buffer.available());
    }
}