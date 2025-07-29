package ai.prophetizo.wavelet.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Factory for creating off-heap managed arrays using the Foreign Function & Memory API.
 * 
 * <p>This factory uses Arena for scoped memory management and supports
 * SIMD-aligned memory allocation for optimal performance.</p>
 */
public final class OffHeapArrayFactory implements ArrayFactory {
    
    private static final ValueLayout.OfDouble DOUBLE_LAYOUT = ValueLayout.JAVA_DOUBLE;
    private static final int DEFAULT_ALIGNMENT = 32; // 256-bit alignment for AVX
    
    private final Arena arena;
    private final int defaultAlignment;
    private boolean closed = false;
    
    /**
     * Creates a new factory with a confined arena.
     * All arrays created by this factory will be tied to the arena's lifecycle.
     */
    public OffHeapArrayFactory() {
        this(Arena.ofConfined(), DEFAULT_ALIGNMENT);
    }
    
    /**
     * Creates a new factory with the specified default alignment.
     * 
     * @param defaultAlignment the default alignment in bytes (must be power of 2)
     * @throws IllegalArgumentException if alignment is not a power of 2
     */
    public OffHeapArrayFactory(int defaultAlignment) {
        this(Arena.ofConfined(), defaultAlignment);
    }
    
    /**
     * Creates a new factory with the specified arena and default alignment.
     * 
     * @param arena the arena to use for memory management
     * @param defaultAlignment the default alignment in bytes (must be power of 2)
     * @throws NullPointerException if arena is null
     * @throws IllegalArgumentException if alignment is not a power of 2
     */
    public OffHeapArrayFactory(Arena arena, int defaultAlignment) {
        this.arena = Objects.requireNonNull(arena, "arena cannot be null");
        this.defaultAlignment = validateAlignment(defaultAlignment);
    }
    
    @Override
    public ManagedArray create(int length) {
        checkNotClosed();
        return createAligned(length, defaultAlignment);
    }
    
    @Override
    public ManagedArray createAligned(int length, int alignment) {
        checkNotClosed();
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        
        alignment = validateAlignment(alignment);
        
        // Calculate required size in bytes
        long byteSize = (long) length * DOUBLE_LAYOUT.byteSize();
        
        // Allocate aligned memory
        MemorySegment segment = arena.allocate(byteSize, alignment);
        
        return new OffHeapArray(segment, length, alignment, null); // Arena manages lifecycle
    }
    
    @Override
    public ManagedArray from(double[] data) {
        checkNotClosed();
        Objects.requireNonNull(data, "data cannot be null");
        return fromAligned(data, defaultAlignment);
    }
    
    @Override
    public ManagedArray fromAligned(double[] data, int alignment) {
        checkNotClosed();
        Objects.requireNonNull(data, "data cannot be null");
        
        ManagedArray result = createAligned(data.length, alignment);
        result.copyFrom(data, 0, 0, data.length);
        return result;
    }
    
    @Override
    public boolean isOffHeap() {
        return true;
    }
    
    @Override
    public int defaultAlignment() {
        return defaultAlignment;
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            arena.close();
        }
    }
    
    /**
     * Creates a shared factory that uses the global arena.
     * Arrays created by this factory will not be automatically cleaned up.
     * 
     * @return a factory using the global arena
     */
    public static OffHeapArrayFactory shared() {
        return new OffHeapArrayFactory(Arena.global(), DEFAULT_ALIGNMENT);
    }
    
    /**
     * Creates a shared factory with the specified default alignment.
     * 
     * @param defaultAlignment the default alignment in bytes
     * @return a factory using the global arena with specified alignment
     */
    public static OffHeapArrayFactory shared(int defaultAlignment) {
        return new OffHeapArrayFactory(Arena.global(), defaultAlignment);
    }
    
    /**
     * Gets the arena used by this factory.
     * 
     * @return the arena
     */
    public Arena getArena() {
        checkNotClosed();
        return arena;
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Factory has been closed");
        }
    }
    
    private static int validateAlignment(int alignment) {
        if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
            throw new IllegalArgumentException("Alignment must be a positive power of 2: " + alignment);
        }
        return alignment;
    }
}