package ai.prophetizo.wavelet.memory.ffm;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Memory allocator using Java 23's Foreign Function & Memory API.
 * Provides SIMD-aligned, zero-copy memory segments for wavelet operations.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>64-byte aligned allocations for optimal SIMD performance</li>
 *   <li>Zero-copy array views via MemorySegment</li>
 *   <li>Scoped lifecycle management via Arena</li>
 *   <li>Direct integration with Vector API</li>
 * </ul>
 * 
 * @since 2.0.0
 */
public final class FFMArrayAllocator {
    
    // SIMD alignment requirements
    private static final long SIMD_ALIGNMENT = 64L; // 64-byte alignment for AVX-512
    private static final long CACHE_LINE_SIZE = 64L;
    
    // Layout for double arrays with proper alignment
    private static final ValueLayout.OfDouble DOUBLE_LAYOUT = ValueLayout.JAVA_DOUBLE;
    
    private FFMArrayAllocator() {
        // Prevent instantiation
    }
    
    /**
     * Allocates a SIMD-aligned double array in the given arena.
     * 
     * @param arena the memory arena for lifecycle management
     * @param size the number of doubles to allocate
     * @return a memory segment representing the array
     */
    public static MemorySegment allocateAligned(Arena arena, long size) {
        Objects.requireNonNull(arena, "Arena cannot be null");
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive: " + size);
        }
        
        // Calculate total bytes needed including alignment padding
        long bytesNeeded = size * Double.BYTES;
        long totalBytes = bytesNeeded + SIMD_ALIGNMENT;
        
        // Allocate raw memory
        MemorySegment raw = arena.allocate(totalBytes);
        
        // Calculate aligned offset
        long address = raw.address();
        long alignedAddress = (address + SIMD_ALIGNMENT - 1) & ~(SIMD_ALIGNMENT - 1);
        long offset = alignedAddress - address;
        
        // Return aligned slice
        return raw.asSlice(offset, bytesNeeded);
    }
    
    /**
     * Creates a memory segment view of an existing double array.
     * Note: This creates a heap segment, not off-heap memory.
     * 
     * @param array the source array
     * @return a memory segment view of the array
     */
    public static MemorySegment fromArray(double[] array) {
        Objects.requireNonNull(array, "Array cannot be null");
        return MemorySegment.ofArray(array);
    }
    
    /**
     * Copies data from a memory segment to a double array.
     * 
     * @param segment the source memory segment
     * @param array the destination array
     * @param offset offset in the array
     * @param length number of elements to copy
     */
    public static void copyToArray(MemorySegment segment, double[] array, int offset, int length) {
        Objects.requireNonNull(segment, "Segment cannot be null");
        Objects.requireNonNull(array, "Array cannot be null");
        
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        
        if (length * Double.BYTES > segment.byteSize()) {
            throw new IllegalArgumentException("Segment too small for requested copy");
        }
        
        // Use bulk copy for efficiency
        MemorySegment.copy(segment, ValueLayout.JAVA_DOUBLE, 0,
                          array, offset, length);
    }
    
    /**
     * Copies data from a double array to a memory segment.
     * 
     * @param array the source array
     * @param offset offset in the array
     * @param segment the destination memory segment
     * @param length number of elements to copy
     */
    public static void copyFromArray(double[] array, int offset, MemorySegment segment, int length) {
        Objects.requireNonNull(array, "Array cannot be null");
        Objects.requireNonNull(segment, "Segment cannot be null");
        
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        
        if (length * Double.BYTES > segment.byteSize()) {
            throw new IllegalArgumentException("Segment too small for requested copy");
        }
        
        // Use bulk copy for efficiency
        MemorySegment.copy(array, offset, segment, ValueLayout.JAVA_DOUBLE, 0, length);
    }
    
    /**
     * Gets a double value from a memory segment.
     * 
     * @param segment the memory segment
     * @param index the element index
     * @return the double value
     */
    public static double get(MemorySegment segment, long index) {
        return segment.getAtIndex(ValueLayout.JAVA_DOUBLE, index);
    }
    
    /**
     * Sets a double value in a memory segment.
     * 
     * @param segment the memory segment
     * @param index the element index
     * @param value the value to set
     */
    public static void set(MemorySegment segment, long index, double value) {
        segment.setAtIndex(ValueLayout.JAVA_DOUBLE, index, value);
    }
    
    /**
     * Fills a memory segment with a constant value.
     * 
     * @param segment the memory segment
     * @param value the fill value
     */
    public static void fill(MemorySegment segment, double value) {
        long elementCount = segment.byteSize() / Double.BYTES;
        
        if (value == 0.0) {
            // Use bulk fill for zeros
            segment.fill((byte) 0);
        } else {
            // Fill element by element
            for (long i = 0; i < elementCount; i++) {
                segment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, value);
            }
        }
    }
    
    /**
     * Creates a zero-copy slice of a memory segment.
     * 
     * @param segment the source segment
     * @param offset offset in elements (not bytes)
     * @param length length in elements (not bytes)
     * @return a slice of the original segment
     */
    public static MemorySegment slice(MemorySegment segment, long offset, long length) {
        long byteOffset = offset * Double.BYTES;
        long byteLength = length * Double.BYTES;
        
        if (byteOffset + byteLength > segment.byteSize()) {
            throw new IndexOutOfBoundsException("Slice exceeds segment bounds");
        }
        
        return segment.asSlice(byteOffset, byteLength);
    }
    
    /**
     * Checks if a memory segment is properly aligned for SIMD operations.
     * 
     * @param segment the memory segment to check
     * @return true if the segment is SIMD-aligned
     */
    public static boolean isAligned(MemorySegment segment) {
        return (segment.address() & (SIMD_ALIGNMENT - 1)) == 0;
    }
    
    /**
     * Gets the number of double elements that can fit in the segment.
     * 
     * @param segment the memory segment
     * @return the number of double elements
     */
    public static long elementCount(MemorySegment segment) {
        return segment.byteSize() / Double.BYTES;
    }
}