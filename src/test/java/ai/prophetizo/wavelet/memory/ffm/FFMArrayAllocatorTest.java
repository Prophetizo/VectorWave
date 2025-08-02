package ai.prophetizo.wavelet.memory.ffm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FFMArrayAllocator memory allocation functionality.
 */
@EnabledForJreRange(min = JRE.JAVA_21) // FFM API available from Java 21+, project targets Java 23
class FFMArrayAllocatorTest {

    @Test
    void testAllocateAligned() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 1024);
            assertNotNull(segment);
            assertEquals(1024 * Double.BYTES, segment.byteSize());
        }
    }

    @Test
    void testAllocateAlignedNullArena() {
        assertThrows(NullPointerException.class, () -> 
            FFMArrayAllocator.allocateAligned(null, 1024));
    }

    @Test
    void testAllocateAlignedInvalidSize() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IllegalArgumentException.class, () -> 
                FFMArrayAllocator.allocateAligned(arena, 0));
            assertThrows(IllegalArgumentException.class, () -> 
                FFMArrayAllocator.allocateAligned(arena, -1));
        }
    }

    @Test
    void testIsAligned() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 64);
            assertTrue(FFMArrayAllocator.isAligned(segment));
        }
    }

    @Test
    void testFromArray() {
        double[] sourceArray = {1.0, 2.5, 3.14, 4.0, 5.5};
        MemorySegment segment = FFMArrayAllocator.fromArray(sourceArray);
        
        assertNotNull(segment);
        assertEquals(sourceArray.length * Double.BYTES, segment.byteSize());
        
        // Verify data can be read correctly
        for (int i = 0; i < sourceArray.length; i++) {
            double value = FFMArrayAllocator.get(segment, i);
            assertEquals(sourceArray[i], value, 1e-15);
        }
    }

    @Test
    void testFromArrayNullInput() {
        assertThrows(NullPointerException.class, () -> 
            FFMArrayAllocator.fromArray(null));
    }

    @Test
    void testCopyToArray() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 5;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            // Fill segment with test data
            for (int i = 0; i < size; i++) {
                FFMArrayAllocator.set(segment, i, i * 1.5);
            }
            
            double[] array = new double[size];
            FFMArrayAllocator.copyToArray(segment, array, 0, size);
            
            for (int i = 0; i < size; i++) {
                assertEquals(i * 1.5, array[i], 1e-15);
            }
        }
    }

    @Test
    void testCopyFromArray() {
        try (Arena arena = Arena.ofConfined()) {
            double[] sourceArray = {1.1, 2.2, 3.3, 4.4, 5.5};
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, sourceArray.length);
            
            FFMArrayAllocator.copyFromArray(sourceArray, 0, segment, sourceArray.length);
            
            // Verify data was copied correctly
            for (int i = 0; i < sourceArray.length; i++) {
                double value = FFMArrayAllocator.get(segment, i);
                assertEquals(sourceArray[i], value, 1e-15);
            }
        }
    }

    @Test
    void testLargeAllocation() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 100_000;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            assertNotNull(segment);
            assertEquals(size * Double.BYTES, segment.byteSize());
            assertTrue(FFMArrayAllocator.isAligned(segment));
        }
    }

    @Test
    void testSmallAllocation() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 1);
            
            assertNotNull(segment);
            assertEquals(Double.BYTES, segment.byteSize());
            assertTrue(FFMArrayAllocator.isAligned(segment));
        }
    }

    @Test
    void testGetAndSet() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 10);
            
            // Test setting and getting values
            double testValue = 3.14159;
            FFMArrayAllocator.set(segment, 5, testValue);
            double retrieved = FFMArrayAllocator.get(segment, 5);
            
            assertEquals(testValue, retrieved, 1e-15);
        }
    }

    @Test
    void testFill() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 8;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            double fillValue = 2.718;
            FFMArrayAllocator.fill(segment, fillValue);
            
            // Verify all elements have the fill value
            for (int i = 0; i < size; i++) {
                assertEquals(fillValue, FFMArrayAllocator.get(segment, i), 1e-15);
            }
        }
    }

    @Test
    void testFillZero() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 10;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            // First set some non-zero values
            for (int i = 0; i < size; i++) {
                FFMArrayAllocator.set(segment, i, i + 1.0);
            }
            
            // Fill with zeros
            FFMArrayAllocator.fill(segment, 0.0);
            
            // Verify all elements are zero
            for (int i = 0; i < size; i++) {
                assertEquals(0.0, FFMArrayAllocator.get(segment, i), 1e-15);
            }
        }
    }

    @Test
    void testSlice() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 10;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            // Fill with test data
            for (int i = 0; i < size; i++) {
                FFMArrayAllocator.set(segment, i, i * 2.0);
            }
            
            // Create a slice
            MemorySegment slice = FFMArrayAllocator.slice(segment, 2, 5);
            assertEquals(5 * Double.BYTES, slice.byteSize());
            
            // Verify slice contains correct data (offset by 2)
            for (int i = 0; i < 5; i++) {
                double expected = (i + 2) * 2.0;
                assertEquals(expected, FFMArrayAllocator.get(slice, i), 1e-15);
            }
        }
    }

    @Test
    void testSliceOutOfBounds() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 5);
            
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.slice(segment, 0, 10)); // Too long
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.slice(segment, 3, 3)); // Offset + length exceeds bounds
        }
    }

    @Test
    void testElementCount() {
        try (Arena arena = Arena.ofConfined()) {
            int size = 17;
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
            
            assertEquals(size, FFMArrayAllocator.elementCount(segment));
        }
    }

    @Test
    void testCopyToArrayBoundsChecking() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 5);
            double[] array = new double[3];
            
            // Test various boundary conditions
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyToArray(segment, array, -1, 3));
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyToArray(segment, array, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyToArray(segment, array, 2, 3)); // offset + length > array.length
            // Test segment capacity check - segment has 5 elements, trying to copy 6
            double[] destinationArray = new double[10];
            assertThrows(IllegalArgumentException.class, () ->
                FFMArrayAllocator.copyToArray(segment, destinationArray, 0, 6)); // trying to copy more elements than segment contains
        }
    }

    @Test
    void testCopyFromArrayBoundsChecking() {
        try (Arena arena = Arena.ofConfined()) {
            double[] array = {1.0, 2.0, 3.0};
            MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, 2);
            
            // Test various boundary conditions
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyFromArray(array, -1, segment, 2));
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyFromArray(array, 0, segment, -1));
            assertThrows(IndexOutOfBoundsException.class, () ->
                FFMArrayAllocator.copyFromArray(array, 2, segment, 2)); // offset + length > array.length
            assertThrows(IllegalArgumentException.class, () ->
                FFMArrayAllocator.copyFromArray(array, 0, segment, 3)); // length > segment capacity
        }
    }
}