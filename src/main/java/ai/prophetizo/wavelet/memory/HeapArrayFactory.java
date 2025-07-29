package ai.prophetizo.wavelet.memory;

import java.util.Objects;

/**
 * Factory for creating heap-based managed arrays.
 * 
 * <p>This factory creates arrays backed by traditional Java double arrays.
 * Alignment parameters are ignored since heap arrays cannot guarantee alignment.</p>
 */
public final class HeapArrayFactory implements ArrayFactory {
    
    private boolean closed = false;
    
    @Override
    public ManagedArray create(int length) {
        checkNotClosed();
        return new HeapArray(length);
    }
    
    @Override
    public ManagedArray createAligned(int length, int alignment) {
        checkNotClosed();
        // Heap arrays cannot guarantee alignment, so ignore the alignment parameter
        return new HeapArray(length);
    }
    
    @Override
    public ManagedArray from(double[] data) {
        checkNotClosed();
        Objects.requireNonNull(data, "data cannot be null");
        // Create a copy to maintain isolation
        double[] copy = new double[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return new HeapArray(copy);
    }
    
    @Override
    public ManagedArray fromAligned(double[] data, int alignment) {
        checkNotClosed();
        // Heap arrays cannot guarantee alignment, so ignore the alignment parameter
        return from(data);
    }
    
    @Override
    public boolean isOffHeap() {
        return false;
    }
    
    @Override
    public int defaultAlignment() {
        return 0; // No alignment guarantee
    }
    
    @Override
    public void close() {
        closed = true;
        // Heap factory doesn't need explicit cleanup
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Factory has been closed");
        }
    }
}