package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedMemoryPoolManagerTest {
    
    @Test
    void testSingletonInstance() {
        SharedMemoryPoolManager manager1 = SharedMemoryPoolManager.getInstance();
        SharedMemoryPoolManager manager2 = SharedMemoryPoolManager.getInstance();
        
        assertSame(manager1, manager2, "Should return the same singleton instance");
    }
    
    @Test
    void testActiveUserTracking() {
        SharedMemoryPoolManager manager = SharedMemoryPoolManager.getInstance();
        int initialUsers = manager.getActiveUserCount();
        
        // Get the shared pool (simulates a user)
        manager.getSharedPool();
        assertEquals(initialUsers + 1, manager.getActiveUserCount());
        
        // Release the user
        manager.releaseUser();
        assertEquals(initialUsers, manager.getActiveUserCount());
    }
    
    @Test
    void testClearIfUnused() {
        SharedMemoryPoolManager manager = SharedMemoryPoolManager.getInstance();
        
        // Cannot clear if there are active users from other tests
        if (manager.getActiveUserCount() > 0) {
            assertFalse(manager.clearIfUnused(), "Should not clear when users are active");
        } else {
            assertTrue(manager.clearIfUnused(), "Should clear when no users are active");
        }
    }
    
    @Test
    void testSharedPoolAccess() {
        SharedMemoryPoolManager manager = SharedMemoryPoolManager.getInstance();
        
        var pool1 = manager.getSharedPool();
        var pool2 = manager.getSharedPool();
        
        assertSame(pool1, pool2, "Should return the same pool instance");
        
        // Clean up
        manager.releaseUser();
        manager.releaseUser();
    }
    
    @Test
    void testStatistics() {
        SharedMemoryPoolManager manager = SharedMemoryPoolManager.getInstance();
        String stats = manager.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.contains("Active users:"));
        assertTrue(stats.contains("Total pooled arrays:"));
    }
}