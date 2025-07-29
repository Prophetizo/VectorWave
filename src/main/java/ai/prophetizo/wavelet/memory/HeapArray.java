package ai.prophetizo.wavelet.memory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Traditional heap-based implementation of ManagedArray.
 * 
 * <p>This implementation uses standard Java double arrays and provides
 * backward compatibility for existing code.</p>
 */
public final class HeapArray implements ManagedArray {
    
    private final double[] data;
    private boolean closed = false;
    
    /**
     * Creates a new heap array with the specified length.
     * 
     * @param length the array length
     * @throws IllegalArgumentException if length is negative
     */
    public HeapArray(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        this.data = new double[length];
    }
    
    /**
     * Creates a new heap array from existing data.
     * The data array is used directly (not copied).
     * 
     * @param data the backing array
     * @throws NullPointerException if data is null
     */
    public HeapArray(double[] data) {
        this.data = Objects.requireNonNull(data, "data cannot be null");
    }
    
    @Override
    public int length() {
        checkNotClosed();
        return data.length;
    }
    
    @Override
    public double get(int index) {
        checkNotClosed();
        return data[index]; // Let array bounds checking happen naturally
    }
    
    @Override
    public void set(int index, double value) {
        checkNotClosed();
        data[index] = value; // Let array bounds checking happen naturally
    }
    
    @Override
    public void copyTo(double[] dest, int destOffset, int srcOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(dest, "dest cannot be null");
        System.arraycopy(data, srcOffset, dest, destOffset, length);
    }
    
    @Override
    public void copyFrom(double[] src, int srcOffset, int destOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(src, "src cannot be null");
        System.arraycopy(src, srcOffset, data, destOffset, length);
    }
    
    @Override
    public void copyTo(ManagedArray dest, int destOffset, int srcOffset, int length) {
        checkNotClosed();
        Objects.requireNonNull(dest, "dest cannot be null");
        
        // Optimize for heap-to-heap copy
        if (dest instanceof HeapArray heapDest) {
            System.arraycopy(data, srcOffset, heapDest.data, destOffset, length);
        } else {
            // General case - copy element by element
            for (int i = 0; i < length; i++) {
                dest.set(destOffset + i, data[srcOffset + i]);
            }
        }
    }
    
    @Override
    public double[] toArray() {
        checkNotClosed();
        return Arrays.copyOf(data, data.length);
    }
    
    @Override
    public void fill(double value) {
        checkNotClosed();
        Arrays.fill(data, value);
    }
    
    @Override
    public boolean isOffHeap() {
        return false;
    }
    
    @Override
    public int alignment() {
        return 0; // Heap arrays are not guaranteed to be aligned
    }
    
    @Override
    public void close() {
        closed = true;
        // Heap arrays don't need explicit cleanup
    }
    
    /**
     * Gets direct access to the underlying array for performance-critical operations.
     * 
     * <p><strong>Warning:</strong> This breaks encapsulation and should be used
     * carefully. Modifying the returned array affects this ManagedArray.</p>
     * 
     * @return the underlying array
     * @throws IllegalStateException if the array is closed
     */
    public double[] getBackingArray() {
        checkNotClosed();
        return data;
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Array has been closed");
        }
    }
}