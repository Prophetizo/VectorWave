package ai.prophetizo.wavelet.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Foreign Function & Memory API-based implementation of ManagedArray.
 * 
 * <p>This implementation uses MemorySegments for direct memory access
 * and supports SIMD-aligned memory allocation for better performance.</p>
 */
public final class OffHeapArray implements ManagedArray {
    
    private static final ValueLayout.OfDouble DOUBLE_LAYOUT = ValueLayout.JAVA_DOUBLE;
    
    private final MemorySegment segment;
    private final int length;
    private final int alignment;
    private final Arena arena;
    private boolean closed = false;
    
    /**
     * Creates a new off-heap array with the specified segment and length.
     * 
     * @param segment the memory segment backing this array
     * @param length the logical length of the array
     * @param alignment the memory alignment in bytes
     * @param arena the arena managing the memory (optional, can be null if segment is global)
     */
    public OffHeapArray(MemorySegment segment, int length, int alignment, Arena arena) {
        this.segment = Objects.requireNonNull(segment, "segment cannot be null");
        this.length = length;
        this.alignment = alignment;
        this.arena = arena; // Can be null for global segments
        
        // Verify the segment is large enough
        long requiredSize = (long) length * DOUBLE_LAYOUT.byteSize();
        if (segment.byteSize() < requiredSize) {
            throw new IllegalArgumentException("Segment too small: " + segment.byteSize() + " < " + requiredSize);
        }
    }
    
    @Override
    public int length() {
        checkNotClosed();
        return length;
    }
    
    @Override
    public double get(int index) {
        checkNotClosed();
        checkBounds(index);
        return segment.getAtIndex(DOUBLE_LAYOUT, index);
    }
    
    @Override
    public void set(int index, double value) {
        checkNotClosed();
        checkBounds(index);
        segment.setAtIndex(DOUBLE_LAYOUT, index, value);
    }
    
    @Override
    public void copyTo(double[] dest, int destOffset, int srcOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(dest, "dest cannot be null");
        checkCopyBounds(srcOffset, length, destOffset, dest.length);
        
        // Copy from MemorySegment to heap array element by element for better compatibility
        for (int i = 0; i < length; i++) {
            dest[destOffset + i] = segment.getAtIndex(DOUBLE_LAYOUT, srcOffset + i);
        }
    }
    
    @Override
    public void copyFrom(double[] src, int srcOffset, int destOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(src, "src cannot be null");
        checkCopyBounds(destOffset, length, srcOffset, src.length);
        
        // Copy from heap array to MemorySegment element by element for better compatibility
        for (int i = 0; i < length; i++) {
            segment.setAtIndex(DOUBLE_LAYOUT, destOffset + i, src[srcOffset + i]);
        }
    }
    
    @Override
    public void copyTo(ManagedArray dest, int destOffset, int srcOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(dest, "dest cannot be null");
        
        // Optimize for off-heap to off-heap copy
        if (dest instanceof OffHeapArray offHeapDest) {
            checkCopyBounds(srcOffset, length, destOffset, dest.length());
            // Copy element by element for better compatibility
            for (int i = 0; i < length; i++) {
                double value = segment.getAtIndex(DOUBLE_LAYOUT, srcOffset + i);
                offHeapDest.segment.setAtIndex(DOUBLE_LAYOUT, destOffset + i, value);
            }
        } else {
            // General case - copy element by element
            checkCopyBounds(srcOffset, length, destOffset, dest.length());
            for (int i = 0; i < length; i++) {
                dest.set(destOffset + i, segment.getAtIndex(DOUBLE_LAYOUT, srcOffset + i));
            }
        }
    }
    
    @Override
    public double[] toArray() {
        checkNotClosed();
        double[] result = new double[length];
        // Copy element by element for better compatibility
        for (int i = 0; i < length; i++) {
            result[i] = segment.getAtIndex(DOUBLE_LAYOUT, i);
        }
        return result;
    }
    
    @Override
    public void fill(double value) {
        checkNotClosed();
        for (int i = 0; i < length; i++) {
            segment.setAtIndex(DOUBLE_LAYOUT, i, value);
        }
    }
    
    @Override
    public boolean isOffHeap() {
        return true;
    }
    
    @Override
    public int alignment() {
        return alignment;
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            // If we own the arena, close it
            if (arena != null) {
                arena.close();
            }
        }
    }
    
    /**
     * Gets direct access to the underlying MemorySegment for performance-critical operations.
     * 
     * <p><strong>Warning:</strong> This breaks encapsulation and should be used
     * carefully. The returned segment should not outlive this ManagedArray.</p>
     * 
     * @return the underlying memory segment
     * @throws IllegalStateException if the array is closed
     */
    public MemorySegment getSegment() {
        checkNotClosed();
        return segment;
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Array has been closed");
        }
    }
    
    private void checkBounds(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
    }
    
    private void checkCopyBounds(int srcOffset, int length, int destOffset, int destLength) {
        if (srcOffset < 0 || length < 0 || srcOffset + length > this.length) {
            throw new IndexOutOfBoundsException("Source bounds invalid: offset=" + srcOffset + ", length=" + length);
        }
        if (destOffset < 0 || destOffset + length > destLength) {
            throw new IndexOutOfBoundsException("Destination bounds invalid: offset=" + destOffset + ", length=" + length);
        }
    }
}