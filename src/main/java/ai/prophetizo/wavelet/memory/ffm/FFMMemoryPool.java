package ai.prophetizo.wavelet.memory.ffm;

import java.lang.foreign.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Memory pool using Foreign Function & Memory API for efficient allocation.
 * Provides SIMD-aligned, reusable memory segments with Arena-based lifecycle.
 * 
 * <p>Key advantages over traditional array pooling:</p>
 * <ul>
 *   <li>Guaranteed SIMD alignment for optimal vectorization</li>
 *   <li>Deterministic memory management via Arena scoping</li>
 *   <li>Zero-copy slicing and views</li>
 *   <li>Direct interop with native libraries</li>
 * </ul>
 * 
 * @since 2.0.0
 */
public final class FFMMemoryPool implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(FFMMemoryPool.class.getName());
    
    // Pool configuration
    private static final int MAX_SEGMENTS_PER_SIZE = 16;
    private static final int[] POOL_SIZES = {32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};
    
    // Memory management
    private final Arena arena;
    private final boolean autoClose;
    private final ConcurrentHashMap<Integer, Queue<PooledSegment>> segmentPools;
    
    // Statistics
    private final AtomicLong allocations = new AtomicLong();
    private final AtomicLong poolHits = new AtomicLong();
    private final AtomicLong poolMisses = new AtomicLong();
    private final AtomicLong totalBytesAllocated = new AtomicLong();
    
    /**
     * Wrapper for pooled segments to track metadata.
     */
    private static final class PooledSegment {
        final MemorySegment segment;
        final long elementCount;
        final long lastUsed;
        
        PooledSegment(MemorySegment segment, long elementCount) {
            this.segment = segment;
            this.elementCount = elementCount;
            this.lastUsed = System.nanoTime();
        }
    }
    
    /**
     * Creates a memory pool with automatic arena management.
     */
    public FFMMemoryPool() {
        this(Arena.ofShared(), true);
    }
    
    /**
     * Creates a memory pool using the specified arena.
     * 
     * @param arena the arena to use for allocations
     */
    public FFMMemoryPool(Arena arena) {
        this(arena, false);
    }
    
    private FFMMemoryPool(Arena arena, boolean autoClose) {
        this.arena = Objects.requireNonNull(arena, "Arena cannot be null");
        this.autoClose = autoClose;
        this.segmentPools = new ConcurrentHashMap<>();
        
        // Pre-initialize pools
        for (int size : POOL_SIZES) {
            segmentPools.put(size, new ConcurrentLinkedQueue<>());
        }
    }
    
    /**
     * Acquires a memory segment of at least the specified size.
     * The returned segment is SIMD-aligned and zero-initialized.
     * 
     * @param elementCount minimum number of double elements needed
     * @return a pooled or newly allocated memory segment
     */
    public MemorySegment acquire(long elementCount) {
        if (elementCount <= 0) {
            throw new IllegalArgumentException("Element count must be positive: " + elementCount);
        }
        
        allocations.incrementAndGet();
        
        // Find appropriate pool size
        int poolSize = nextPoolSize(elementCount);
        Queue<PooledSegment> pool = segmentPools.get(poolSize);
        
        if (pool != null) {
            PooledSegment pooled = pool.poll();
            if (pooled != null && pooled.elementCount >= elementCount) {
                poolHits.incrementAndGet();
                // Clear the segment
                FFMArrayAllocator.fill(pooled.segment.asSlice(0, elementCount * Double.BYTES), 0.0);
                return pooled.segment;
            }
        }
        
        // Allocate new segment
        poolMisses.incrementAndGet();
        long size = Math.max(elementCount, poolSize);
        MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, size);
        totalBytesAllocated.addAndGet(segment.byteSize());
        
        // Zero-initialize
        segment.fill((byte) 0);
        
        return segment;
    }
    
    /**
     * Releases a memory segment back to the pool for reuse.
     * 
     * @param segment the segment to release
     */
    public void release(MemorySegment segment) {
        if (segment == null) {
            return;
        }
        
        if (!segment.scope().equals(arena.scope())) {
            LOGGER.log(Level.WARNING, "Attempted to release segment from different arena. " +
                    "This may indicate a programming error where segments are being mixed between pools.");
            return;
        }
        
        long elementCount = FFMArrayAllocator.elementCount(segment);
        int poolSize = findPoolSize(elementCount);
        
        if (poolSize > 0) {
            Queue<PooledSegment> pool = segmentPools.get(poolSize);
            if (pool != null && pool.size() < MAX_SEGMENTS_PER_SIZE) {
                pool.offer(new PooledSegment(segment, elementCount));
            }
        }
        // Otherwise, let Arena handle cleanup
    }
    
    /**
     * Creates a scoped memory pool for a specific operation.
     * All allocations within the scope are automatically released.
     * 
     * @param operation the operation to perform with the scoped pool
     * @param <T> the return type
     * @return the operation result
     */
    public static <T> T withScope(ScopedOperation<T> operation) {
        try (Arena scopedArena = Arena.ofConfined()) {
            FFMMemoryPool scopedPool = new FFMMemoryPool(scopedArena);
            return operation.execute(scopedPool);
        }
    }
    
    /**
     * Functional interface for scoped operations.
     */
    @FunctionalInterface
    public interface ScopedOperation<T> {
        T execute(FFMMemoryPool pool);
    }
    
    /**
     * Pre-warms the pool by allocating segments of common sizes.
     * Useful for reducing allocation latency in performance-critical paths.
     * 
     * @param sizesToWarm array of element counts to pre-allocate
     */
    public void prewarm(int... sizesToWarm) {
        for (int size : sizesToWarm) {
            int poolSize = nextPoolSize(size);
            Queue<PooledSegment> pool = segmentPools.get(poolSize);
            
            if (pool != null) {
                int toAllocate = Math.min(4, MAX_SEGMENTS_PER_SIZE - pool.size());
                for (int i = 0; i < toAllocate; i++) {
                    MemorySegment segment = FFMArrayAllocator.allocateAligned(arena, poolSize);
                    totalBytesAllocated.addAndGet(segment.byteSize());
                    pool.offer(new PooledSegment(segment, poolSize));
                }
            }
        }
    }
    
    /**
     * Gets current pool statistics.
     * 
     * @return statistics snapshot
     */
    public PoolStatistics getStatistics() {
        long totalAllocations = allocations.get();
        long hits = poolHits.get();
        long misses = poolMisses.get();
        long bytesAllocated = totalBytesAllocated.get();
        
        int pooledSegments = segmentPools.values().stream()
            .mapToInt(Queue::size)
            .sum();
        
        return new PoolStatistics(totalAllocations, hits, misses, 
                                 bytesAllocated, pooledSegments);
    }
    
    /**
     * Clears all pooled segments, releasing memory back to the arena.
     */
    public void clear() {
        segmentPools.values().forEach(Queue::clear);
    }
    
    @Override
    public void close() {
        clear();
        if (autoClose) {
            arena.close();
        }
    }
    
    // Helper methods
    
    private int nextPoolSize(long elementCount) {
        for (int size : POOL_SIZES) {
            if (size >= elementCount) {
                return size;
            }
        }
        // For very large sizes, round up to next power of 2
        return nextPowerOfTwo((int) elementCount);
    }
    
    private int findPoolSize(long elementCount) {
        for (int size : POOL_SIZES) {
            if (size == elementCount) {
                return size;
            }
        }
        return -1;
    }
    
    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    /**
     * Pool statistics record.
     */
    public record PoolStatistics(
        long totalAllocations,
        long poolHits,
        long poolMisses,
        long totalBytesAllocated,
        int pooledSegments
    ) {
        public double hitRate() {
            return totalAllocations > 0 ? 
                (double) poolHits / totalAllocations : 0.0;
        }
        
        public String toDetailedString() {
            return String.format(
                "FFMPool[allocations=%d, hits=%d (%.1f%%), misses=%d, " +
                "bytes=%d, pooled=%d]",
                totalAllocations, poolHits, hitRate() * 100, poolMisses,
                totalBytesAllocated, pooledSegments
            );
        }
    }
}