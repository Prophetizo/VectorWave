package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Centralized management for ThreadLocal cleanup across the wavelet library.
 * 
 * <p>This utility helps prevent memory leaks in long-running applications and
 * thread pool environments by providing coordinated cleanup of ThreadLocal
 * resources used throughout the library.</p>
 * 
 * <p><strong>Usage in applications:</strong></p>
 * <pre>{@code
 * try {
 *     // Perform wavelet operations
 *     transform.forward(signal);
 *     batchProcessor.processBatch(signals);
 * } finally {
 *     // Clean up thread-local resources
 *     ThreadLocalCleanup.cleanupCurrentThread();
 * }
 * }</pre>
 * 
 * <p><strong>Usage in application servers:</strong></p>
 * <pre>{@code
 * // Register cleanup hook for servlet context destruction
 * @Override
 * public void contextDestroyed(ServletContextEvent sce) {
 *     ThreadLocalCleanup.cleanupAllThreads();
 * }
 * }</pre>
 * 
 * @since 3.1.0
 */
public final class ThreadLocalCleanup {
    
    private static final Logger logger = Logger.getLogger(ThreadLocalCleanup.class.getName());
    
    // Registry of cleanup tasks for different components
    private static final List<Runnable> CLEANUP_TASKS = new ArrayList<>();
    
    // Track which threads have ThreadLocal data
    private static final ConcurrentMap<Long, String> ACTIVE_THREADS = new ConcurrentHashMap<>();
    
    static {
        // Register built-in cleanup tasks
        registerCleanupTask("BatchSIMDTransform", BatchSIMDTransform::cleanupThreadLocals);
        registerCleanupTask("AlignedMemoryPool", AlignedMemoryPool::cleanupThreadLocals);
        
        // Register shutdown hook for JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutdown detected, cleaning up all ThreadLocal resources");
            cleanupAllThreads();
        }, "WaveletThreadLocalCleanup"));
    }
    
    private ThreadLocalCleanup() {
        // Utility class
    }
    
    /**
     * Registers a cleanup task for a specific component.
     * 
     * @param componentName the name of the component (for logging)
     * @param cleanupTask the cleanup task to execute
     */
    public static synchronized void registerCleanupTask(String componentName, Runnable cleanupTask) {
        logger.fine("Registering ThreadLocal cleanup task for: " + componentName);
        CLEANUP_TASKS.add(() -> {
            try {
                cleanupTask.run();
            } catch (Exception e) {
                logger.warning("Failed to cleanup ThreadLocal for " + componentName + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Cleans up ThreadLocal resources for the current thread.
     * 
     * <p>This method should be called when a thread finishes processing
     * wavelet operations, especially in thread pool environments.</p>
     */
    public static void cleanupCurrentThread() {
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        logger.fine("Cleaning up ThreadLocal resources for thread: " + threadName + " (ID: " + threadId + ")");
        
        // Execute all registered cleanup tasks
        for (Runnable task : CLEANUP_TASKS) {
            task.run();
        }
        
        // Remove from active threads tracking
        ACTIVE_THREADS.remove(threadId);
        
        logger.fine("ThreadLocal cleanup completed for thread: " + threadName);
    }
    
    /**
     * Cleans up ThreadLocal resources for all known threads.
     * 
     * <p>This method is more aggressive and should be used during application
     * shutdown or when explicitly clearing all resources.</p>
     */
    public static void cleanupAllThreads() {
        logger.info("Starting cleanup of ThreadLocal resources for all threads");
        
        // Execute cleanup for all known components
        for (Runnable task : CLEANUP_TASKS) {
            task.run();
        }
        
        // Clear tracking
        int clearedThreads = ACTIVE_THREADS.size();
        ACTIVE_THREADS.clear();
        
        logger.info("Completed ThreadLocal cleanup for " + clearedThreads + " threads");
    }
    
    /**
     * Marks the current thread as having active ThreadLocal resources.
     * 
     * <p>This is called automatically by wavelet components that use ThreadLocal storage.</p>
     * 
     * @param componentName the name of the component creating ThreadLocal resources
     */
    public static void markThreadActive(String componentName) {
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        ACTIVE_THREADS.put(threadId, componentName);
        logger.fine("Thread " + threadName + " marked as active for component: " + componentName);
    }
    
    /**
     * Returns the number of threads with active ThreadLocal resources.
     * 
     * @return the count of active threads
     */
    public static int getActiveThreadCount() {
        return ACTIVE_THREADS.size();
    }
    
    /**
     * Returns detailed information about ThreadLocal resource usage.
     * 
     * @return a string describing current ThreadLocal usage
     */
    public static String getUsageInfo() {
        StringBuilder info = new StringBuilder();
        info.append("ThreadLocal Usage Report:\n");
        info.append("  Active threads: ").append(ACTIVE_THREADS.size()).append("\n");
        info.append("  Registered cleanup tasks: ").append(CLEANUP_TASKS.size()).append("\n");
        
        if (!ACTIVE_THREADS.isEmpty()) {
            info.append("  Active thread details:\n");
            ACTIVE_THREADS.forEach((threadId, component) -> {
                info.append("    Thread ID ").append(threadId)
                    .append(" (").append(component).append(")\n");
            });
        }
        
        return info.toString();
    }
    
    /**
     * Creates a try-with-resources wrapper for automatic cleanup.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * try (var cleanup = ThreadLocalCleanup.autoCleanup()) {
     *     // Perform wavelet operations
     *     transform.forward(signal);
     * } // Automatic cleanup on close
     * }</pre>
     * 
     * @return an AutoCloseable that performs cleanup when closed
     */
    public static AutoCloseable autoCleanup() {
        return ThreadLocalCleanup::cleanupCurrentThread;
    }
}