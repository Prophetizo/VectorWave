package ai.prophetizo.wavelet.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for OffHeapArray functionality.
 */
class OffHeapArrayTest {

    private OffHeapArrayFactory factory;
    private ManagedArray array;

    @BeforeEach
    void setUp() {
        factory = new OffHeapArrayFactory();
        // Create a test array with some initial data
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        array = factory.from(data);
    }

    @AfterEach
    void tearDown() {
        if (array != null) {
            array.close();
        }
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("OffHeapArray should create with correct length")
    void testLength() {
        assertEquals(5, array.length());
    }

    @Test
    @DisplayName("OffHeapArray should get and set values correctly")
    void testGetAndSet() {
        assertEquals(1.0, array.get(0));
        assertEquals(3.0, array.get(2));
        
        array.set(2, 10.0);
        assertEquals(10.0, array.get(2));
    }

    @Test
    @DisplayName("OffHeapArray should handle bounds checking")
    void testBoundsChecking() {
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(-1, 1.0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(5, 1.0));
    }

    @Test
    @DisplayName("OffHeapArray should copy data correctly")
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
    @DisplayName("OffHeapArray should return defensive copy in toArray")
    void testToArray() {
        double[] copy = array.toArray();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, copy);
        
        // Modifying the copy should not affect the original
        copy[0] = 999.0;
        assertEquals(1.0, array.get(0));
    }

    @Test
    @DisplayName("OffHeapArray should fill with value correctly")
    void testFill() {
        array.fill(7.0);
        for (int i = 0; i < array.length(); i++) {
            assertEquals(7.0, array.get(i));
        }
    }

    @Test
    @DisplayName("OffHeapArray should report correct memory characteristics")
    void testMemoryCharacteristics() {
        assertTrue(array.isOffHeap());
        assertEquals(32, array.alignment()); // Default alignment
    }

    @Test
    @DisplayName("OffHeapArray should handle closure correctly")
    void testClosure() {
        array.close();
        assertThrows(IllegalStateException.class, () -> array.length());
        assertThrows(IllegalStateException.class, () -> array.get(0));
    }

    @Test
    @DisplayName("OffHeapArray should copy between ManagedArrays")
    void testManagedArrayCopy() {
        try (ManagedArray dest = factory.create(3)) {
            array.copyTo(dest, 0, 1, 3);
            
            assertEquals(2.0, dest.get(0));
            assertEquals(3.0, dest.get(1));
            assertEquals(4.0, dest.get(2));
        }
    }

    @Test
    @DisplayName("OffHeapArrayFactory should create aligned arrays")
    void testAlignedCreation() {
        try (ManagedArray aligned = factory.createAligned(10, 64)) {
            assertTrue(aligned.isOffHeap());
            assertEquals(64, aligned.alignment());
        }
    }

    @Test
    @DisplayName("OffHeapArrayFactory should validate alignment")
    void testAlignmentValidation() {
        assertThrows(IllegalArgumentException.class, () -> factory.createAligned(10, 7)); // Not power of 2
        assertThrows(IllegalArgumentException.class, () -> factory.createAligned(10, 0));  // Zero
        assertThrows(IllegalArgumentException.class, () -> factory.createAligned(10, -1)); // Negative
    }
}