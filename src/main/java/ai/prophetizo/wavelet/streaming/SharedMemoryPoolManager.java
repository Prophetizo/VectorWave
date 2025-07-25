package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.memory.MemoryPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a shared memory pool for streaming components.
 *
 * <p>This singleton class provides a shared memory pool that can be used
 * across multiple streaming instances to reduce memory fragmentation and
 * improve cache locality.</p>
 * 
 * <p>The maximum number of arrays per size can be configured via the system property:
 * {@code -Dai.prophetizo.wavelet.sharedPool.maxArraysPerSize=20} (default: 20)</p>
 *
 * @since 1.6.0
 */
public final class SharedMemoryPoolManager {

    private static final SharedMemoryPoolManager INSTANCE = new SharedMemoryPoolManager();
    
    // Default maximum arrays per size for the shared pool
    private static final int DEFAULT_MAX_ARRAYS_PER_SIZE = 20;
    
    // System property to configure max arrays per size
    private static final String MAX_ARRAYS_PER_SIZE_PROPERTY = "ai.prophetizo.wavelet.sharedPool.maxArraysPerSize";
    
    private final MemoryPool sharedPool;
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final Object poolLock = new Object();

    private SharedMemoryPoolManager() {
        this.sharedPool = new MemoryPool();
        
        // Configure pool for typical streaming workloads
        // Allow configuration via system property for different deployment scenarios
        int maxArraysPerSize = Integer.getInteger(MAX_ARRAYS_PER_SIZE_PROPERTY, DEFAULT_MAX_ARRAYS_PER_SIZE);
        this.sharedPool.setMaxArraysPerSize(maxArraysPerSize);
    }

    /**
     * Gets the singleton instance of the shared memory pool manager.
     */
    public static SharedMemoryPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the shared memory pool.
     *
     * <p>This method increments the active user count for monitoring. Users must
     * call {@link #releaseUser()} when they are done using the pool.</p>
     * 
     * <p><b>Important:</b> The pool should not be cleared while there are active users.
     * The {@link #clearIfUnused()} method checks for active users, but there is a small
     * window between getting the pool and using it where clearing could theoretically occur.
     * In practice, this is not an issue because:</p>
     * <ul>
     *   <li>clearIfUnused() is typically only called during shutdown</li>
     *   <li>The pool itself is thread-safe for concurrent operations</li>
     *   <li>Clearing the pool only affects future allocations, not existing arrays</li>
     * </ul>
     * 
     * @return the shared memory pool
     */
    public MemoryPool getSharedPool() {
        synchronized (poolLock) {
            // Increment user count atomically with pool access
            activeUsers.incrementAndGet();
            return sharedPool;
        }
    }

    /**
     * Notifies that a user has stopped using the shared pool.
     *
     * <p>This is used for monitoring and potential future cleanup strategies.</p>
     */
    public void releaseUser() {
        activeUsers.decrementAndGet();
    }

    /**
     * Gets the number of active users of the shared pool.
     */
    public int getActiveUserCount() {
        return activeUsers.get();
    }

    /**
     * Clears the shared pool if there are no active users.
     *
     * <p>This method is intended for use during application shutdown or when
     * memory needs to be reclaimed. It should not be called during normal
     * operation while components may be actively using the pool.</p>
     * 
     * <p>Note: Clearing the pool only affects future allocations. Arrays that
     * have already been borrowed from the pool remain valid and can still be
     * returned to the pool (though they will be discarded).</p>
     *
     * @return true if the pool was cleared, false if there are active users
     */
    public boolean clearIfUnused() {
        synchronized (poolLock) {
            // Check and clear atomically to prevent race conditions
            if (activeUsers.get() == 0) {
                sharedPool.clear();
                return true;
            }
            return false;
        }
    }

    /**
     * Gets statistics about the shared pool usage.
     */
    public String getStatistics() {
        return String.format("Active users: %d, Total pooled arrays: %d",
                activeUsers.get(), sharedPool.getTotalPooledCount());
    }
}