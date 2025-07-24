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
 * @since 1.6.0
 */
public final class SharedMemoryPoolManager {
    
    private static final SharedMemoryPoolManager INSTANCE = new SharedMemoryPoolManager();
    
    private final MemoryPool sharedPool;
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    
    private SharedMemoryPoolManager() {
        this.sharedPool = new MemoryPool();
        // Configure pool for typical streaming workloads
        this.sharedPool.setMaxArraysPerSize(20); // Allow more arrays per size for concurrent usage
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
     * <p>This method also increments the active user count for monitoring.</p>
     */
    public MemoryPool getSharedPool() {
        activeUsers.incrementAndGet();
        return sharedPool;
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
     * <p>This can be called during application shutdown or when
     * memory needs to be reclaimed.</p>
     * 
     * @return true if the pool was cleared, false if there are active users
     */
    public boolean clearIfUnused() {
        if (activeUsers.get() == 0) {
            sharedPool.clear();
            return true;
        }
        return false;
    }
    
    /**
     * Gets statistics about the shared pool usage.
     */
    public String getStatistics() {
        return String.format("Active users: %d, Total pooled arrays: %d",
            activeUsers.get(), sharedPool.getTotalPooledCount());
    }
}