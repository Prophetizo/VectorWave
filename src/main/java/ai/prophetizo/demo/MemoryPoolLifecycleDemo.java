package ai.prophetizo.demo;

import ai.prophetizo.wavelet.memory.MemoryPool;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.memory.ffm.FFMMemoryPool;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates proper memory pool lifecycle management patterns.
 * 
 * This demo shows:
 * - Different pool types and their use cases
 * - Proper resource management with try-finally
 * - Thread-safe pool usage patterns
 * - Performance monitoring and pool clearing strategies
 * - Common pitfalls and how to avoid them
 */
public class MemoryPoolLifecycleDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Memory Pool Lifecycle Demo ===\n");
        
        // 1. Basic pool usage with try-finally
        demonstrateBasicPoolUsage();
        
        // 2. Thread-safe shared pool
        demonstrateSharedPoolPattern();
        
        // 3. Thread-local pool pattern
        demonstrateThreadLocalPattern();
        
        // 4. FFM pool with automatic lifecycle
        demonstrateFFMPoolLifecycle();
        
        // 5. Pool clearing strategies
        demonstratePoolClearingStrategies();
        
        // 6. Performance monitoring
        demonstratePoolMonitoring();
        
        // 7. Common anti-patterns (what NOT to do)
        demonstrateAntiPatterns();
    }
    
    /**
     * 1. Basic pool usage with proper resource management
     */
    private static void demonstrateBasicPoolUsage() {
        System.out.println("1. Basic Pool Usage Pattern");
        System.out.println("---------------------------");
        
        MemoryPool pool = new MemoryPool();
        pool.setMaxArraysPerSize(10);
        
        // Correct pattern: always return arrays
        for (int i = 0; i < 5; i++) {
            double[] workspace = pool.borrowArray(1024);
            try {
                // Simulate processing
                processSignal(workspace);
                System.out.printf("Iteration %d: Borrowed and returned array\n", i);
            } finally {
                pool.returnArray(workspace); // Always return!
            }
        }
        
        pool.printStatistics();
        System.out.println();
    }
    
    /**
     * 2. Thread-safe shared pool pattern
     */
    private static void demonstrateSharedPoolPattern() throws InterruptedException {
        System.out.println("2. Shared Pool Pattern (Thread-Safe)");
        System.out.println("------------------------------------");
        
        MemoryPool sharedPool = new MemoryPool();
        sharedPool.setMaxArraysPerSize(20); // Size for concurrency
        
        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicLong totalOperations = new AtomicLong();
        
        // Multiple threads sharing the same pool
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < 100; i++) {
                    double[] array = sharedPool.borrowArray(512 + threadId * 100);
                    try {
                        processSignal(array);
                        totalOperations.incrementAndGet();
                    } finally {
                        sharedPool.returnArray(array);
                    }
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.printf("Completed %d operations across %d threads\n", 
            totalOperations.get(), numThreads);
        sharedPool.printStatistics();
        System.out.println();
    }
    
    /**
     * 3. Thread-local pool pattern for high contention scenarios
     */
    private static void demonstrateThreadLocalPattern() throws InterruptedException {
        System.out.println("3. Thread-Local Pool Pattern");
        System.out.println("----------------------------");
        
        ThreadLocal<MemoryPool> threadLocalPools = ThreadLocal.withInitial(() -> {
            MemoryPool pool = new MemoryPool();
            pool.setMaxArraysPerSize(5); // Smaller per-thread pools
            return pool;
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicLong totalAllocations = new AtomicLong();
        
        for (int t = 0; t < 4; t++) {
            executor.submit(() -> {
                MemoryPool myPool = threadLocalPools.get();
                for (int i = 0; i < 50; i++) {
                    double[] array = myPool.borrowArray(1024);
                    try {
                        processSignal(array);
                        totalAllocations.incrementAndGet();
                    } finally {
                        myPool.returnArray(array);
                    }
                }
                
                // Print per-thread statistics
                System.out.printf("Thread %s statistics:\n", Thread.currentThread().getName());
                myPool.printStatistics();
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // Important: clean up thread-local to prevent memory leaks
        threadLocalPools.remove();
        System.out.println();
    }
    
    /**
     * 4. FFM pool with automatic lifecycle management
     */
    private static void demonstrateFFMPoolLifecycle() {
        System.out.println("4. FFM Pool with Automatic Lifecycle");
        System.out.println("------------------------------------");
        
        // FFM pools are AutoCloseable - use try-with-resources
        try (FFMMemoryPool ffmPool = new FFMMemoryPool()) {
            System.out.println("Created FFM pool with automatic arena management");
            
            // Perform multiple operations
            for (int i = 0; i < 3; i++) {
                var segment = ffmPool.acquire(1024);
                try {
                    System.out.printf("Acquired segment of size %d bytes\n", segment.byteSize());
                    // Process with segment...
                } finally {
                    ffmPool.release(segment);
                }
            }
            
            // Print statistics
            var stats = ffmPool.getStatistics();
            System.out.printf("Pool efficiency: %.1f%%\n", stats.hitRate() * 100);
            
        } // Pool and arena automatically closed here
        
        System.out.println("FFM pool closed automatically\n");
    }
    
    /**
     * 5. Pool clearing strategies
     */
    private static void demonstratePoolClearingStrategies() {
        System.out.println("5. Pool Clearing Strategies");
        System.out.println("---------------------------");
        
        // Strategy 1: Clear between processing phases
        System.out.println("a) Phase-based clearing:");
        MemoryPool phasePool = new MemoryPool();
        
        // Phase 1: Load data
        System.out.println("   Phase 1: Loading data");
        for (int i = 0; i < 10; i++) {
            double[] array = phasePool.borrowArray(1024);
            phasePool.returnArray(array);
        }
        System.out.printf("   Arrays pooled: %d\n", phasePool.getTotalPooledCount());
        
        phasePool.clear();
        System.out.println("   Cleared pool between phases");
        
        // Phase 2: Process data
        System.out.println("   Phase 2: Processing data");
        for (int i = 0; i < 10; i++) {
            double[] array = phasePool.borrowArray(2048);
            phasePool.returnArray(array);
        }
        System.out.printf("   Arrays pooled: %d\n", phasePool.getTotalPooledCount());
        
        // Strategy 2: Memory-based clearing
        System.out.println("\nb) Memory-based clearing:");
        MemoryPool memoryPool = new MemoryPool();
        memoryPool.setMaxArraysPerSize(5);
        
        long maxMemoryBytes = 100_000; // 100KB threshold
        long estimatedUsage = 0;
        
        for (int i = 0; i < 20; i++) {
            int size = 1024 * (i % 4 + 1);
            double[] array = memoryPool.borrowArray(size);
            memoryPool.returnArray(array);
            
            estimatedUsage = memoryPool.getTotalPooledCount() * 8 * 1024; // Rough estimate
            if (estimatedUsage > maxMemoryBytes) {
                System.out.printf("   Memory usage ~%d KB, clearing pool\n", estimatedUsage / 1024);
                memoryPool.clear();
                estimatedUsage = 0;
            }
        }
        
        System.out.println();
    }
    
    /**
     * 6. Performance monitoring
     */
    private static void demonstratePoolMonitoring() {
        System.out.println("6. Pool Performance Monitoring");
        System.out.println("------------------------------");
        
        MemoryPool pool = new MemoryPool();
        pool.setMaxArraysPerSize(10);
        
        // Simulate workload with monitoring
        long startTime = System.nanoTime();
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            // Vary array sizes to simulate real workload
            int size = 512 + (i % 8) * 256;
            double[] array = pool.borrowArray(size);
            try {
                // Simulate deterministic work
                processSignal(array);
            } finally {
                pool.returnArray(array);
            }
            
            // Periodic monitoring
            if (i > 0 && i % 100 == 0) {
                double hitRate = pool.getHitRate();
                System.out.printf("After %d operations: Hit rate = %.1f%%, Pooled arrays = %d\n",
                    i, hitRate * 100, pool.getTotalPooledCount());
            }
        }
        
        long elapsed = System.nanoTime() - startTime;
        System.out.printf("\nCompleted %d operations in %.2f ms\n", 
            iterations, elapsed / 1_000_000.0);
        
        pool.printStatistics();
        System.out.println();
    }
    
    /**
     * 7. Common anti-patterns to avoid
     */
    private static void demonstrateAntiPatterns() {
        System.out.println("7. Anti-Patterns to Avoid");
        System.out.println("-------------------------");
        
        // Anti-pattern 1: Forgetting to return arrays
        System.out.println("❌ Anti-pattern 1: Memory leak from unreturned arrays");
        MemoryPool leakyPool = new MemoryPool();
        for (int i = 0; i < 3; i++) {
            double[] array = leakyPool.borrowArray(1024);
            // Forgot to return! This causes a memory leak
            processSignal(array);
        }
        System.out.printf("   Arrays borrowed: 3, Arrays in pool: %d (should be 3!)\n", 
            leakyPool.getTotalPooledCount());
        
        // Anti-pattern 2: Not clearing sensitive data
        System.out.println("\n❌ Anti-pattern 2: Sensitive data exposure");
        MemoryPool sensitivePool = new MemoryPool();
        double[] sensitiveData = sensitivePool.borrowArray(10);
        // Simulate sensitive data
        for (int i = 0; i < sensitiveData.length; i++) {
            sensitiveData[i] = 123.456 + i; // "Sensitive" values
        }
        // Returning without clearing - data might leak to next user!
        sensitivePool.returnArray(sensitiveData); // Pool clears it, but manual clearing is better practice
        
        // Anti-pattern 3: Unbounded pool growth
        System.out.println("\n❌ Anti-pattern 3: Unbounded pool growth");
        MemoryPool unboundedPool = new MemoryPool();
        unboundedPool.setMaxArraysPerSize(Integer.MAX_VALUE); // Bad idea!
        System.out.println("   Setting max arrays to Integer.MAX_VALUE allows unbounded growth");
        
        // Correct patterns summary
        System.out.println("\n✅ Correct Patterns:");
        System.out.println("   1. Always use try-finally to ensure arrays are returned");
        System.out.println("   2. Clear sensitive data before returning to pool");
        System.out.println("   3. Set reasonable pool size limits");
        System.out.println("   4. Monitor pool statistics in production");
        System.out.println("   5. Clear pools periodically or at phase boundaries");
    }
    
    /**
     * Simulate signal processing work
     */
    private static void processSignal(double[] workspace) {
        // Simulate some work on the array
        for (int i = 0; i < Math.min(10, workspace.length); i++) {
            workspace[i] = Math.sin(i * 0.1);
        }
    }
    
}