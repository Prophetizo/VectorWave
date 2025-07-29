package ai.prophetizo.wavelet.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for HeapArray functionality.
 */
class HeapArrayTest {

    private HeapArray array;

    @BeforeEach
    void setUp() {
        // Create a test array with some initial data
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        array = new HeapArray(data);
    }

    @AfterEach
    void tearDown() {
        if (array != null) {
            array.close();
        }
    }

    @Test
    @DisplayName("HeapArray should create with correct length")
    void testLength() {
        assertEquals(5, array.length());
    }

    @Test
    @DisplayName("HeapArray should get and set values correctly")
    void testGetAndSet() {
        assertEquals(1.0, array.get(0));
        assertEquals(3.0, array.get(2));
        
        array.set(2, 10.0);
        assertEquals(10.0, array.get(2));
    }

    @Test
    @DisplayName("HeapArray should handle bounds checking")
    void testBoundsChecking() {
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(-1, 1.0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(5, 1.0));
    }

    @Test
    @DisplayName("HeapArray should copy data correctly")
    void testCopyOperations() {
        double[] dest = new double[3];
        array.copyTo(dest, 0, 1, 3);
        assertArrayEquals(new double[]{2.0, 3.0, 4.0}, dest);

        double[] src = {10.0, 20.0};
        array.copyFrom(src, 0, 1, 2);
        assertEquals(10.0, array.get(1));
        assertEquals(20.0, array.get(2));
    }

    @Test
    @DisplayName("HeapArray should return defensive copy in toArray")
    void testToArray() {
        double[] copy = array.toArray();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, copy);
        
        // Modifying the copy should not affect the original
        copy[0] = 999.0;
        assertEquals(1.0, array.get(0));
    }

    @Test
    @DisplayName("HeapArray should fill with value correctly")
    void testFill() {
        array.fill(7.0);
        for (int i = 0; i < array.length(); i++) {
            assertEquals(7.0, array.get(i));
        }
    }

    @Test
    @DisplayName("HeapArray should report correct memory characteristics")
    void testMemoryCharacteristics() {
        assertFalse(array.isOffHeap());
        assertEquals(0, array.alignment());
    }

    @Test
    @DisplayName("HeapArray should handle closure correctly")
    void testClosure() {
        array.close();
        assertThrows(IllegalStateException.class, () -> array.length());
        assertThrows(IllegalStateException.class, () -> array.get(0));
    }

    @Test
    @DisplayName("HeapArray should copy between ManagedArrays")
    void testManagedArrayCopy() {
        HeapArray dest = new HeapArray(3);
        array.copyTo(dest, 0, 1, 3);
        
        assertEquals(2.0, dest.get(0));
        assertEquals(3.0, dest.get(1));
        assertEquals(4.0, dest.get(2));
        
        dest.close();
    }
}