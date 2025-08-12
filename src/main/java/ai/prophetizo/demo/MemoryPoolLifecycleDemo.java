package ai.prophetizo.demo;

import ai.prophetizo.wavelet.memory.MemoryPool;
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.api.*;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates proper memory pool lifecycle management patterns with MODWT.
 * 
 * <p>This demo shows:
 * <ul>
 *   <li>Different pool types and their use cases</li>
 *   <li>Proper resource management with try-finally</li>
 *   <li>Thread-safe pool usage patterns</li>
 *   <li>Performance monitoring and pool clearing strategies</li>
 *   <li>Common pitfalls and how to avoid them</li>
 * </ul>
 * 
 */
public class MemoryPoolLifecycleDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== MODWT Memory Pool Lifecycle Demo ===\n");
        
        // 1. Basic pool usage with try-finally
        demonstrateBasicPoolUsage();
        
        // 2. Thread-safe shared pool
        demonstrateSharedPoolPattern();
        
        // 3. Thread-local pool pattern
        demonstrateThreadLocalPattern();
        
        // 4. MODWT with memory pooling
        demonstrateMODWTWithPooling();
        
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
     * 4. MODWT with memory pooling for efficient processing
     */
    private static void demonstrateMODWTWithPooling() {
        System.out.println("4. MODWT with Memory Pooling");
        System.out.println("-----------------------------");
        
        MemoryPool pool = new MemoryPool();
        pool.setMaxArraysPerSize(10);
        
        // Create MODWT transform
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        System.out.println("Processing multiple signals with pooled memory:");
        
        // Process multiple signals of varying sizes
        int[] signalSizes = {256, 333, 512, 777, 1024}; // MODWT handles any size!
        
        for (int size : signalSizes) {
            // Borrow array from pool
            double[] signal = pool.borrowArray(size);
            
            try {
                // Generate test signal
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 32) + 0.1 * Math.random();
                }
                
                // Process with MODWT
                MODWTResult result = transform.forward(signal);
                
                // Use result (calculate energy)
                double energy = 0;
                for (double c : result.approximationCoeffs()) energy += c * c;
                for (double c : result.detailCoeffs()) energy += c * c;
                
                System.out.printf("  Size %4d: Energy = %.2f, Pool hit rate = %.1f%%\n", 
                    size, energy, pool.getHitRate() * 100);
                    
            } finally {
                pool.returnArray(signal);
            }
        }
        
        System.out.println("\nPool statistics after MODWT processing:");
        pool.printStatistics();
        
        // Clear pool to free memory
        pool.clear();
        System.out.println("Pool cleared\n");
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
        
        // Strategy 3: MODWT-specific clearing
        System.out.println("\nc) MODWT batch processing with clearing:");
        MemoryPool modwtPool = new MemoryPool();
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        for (int batch = 0; batch < 3; batch++) {
            System.out.printf("   Batch %d:\n", batch + 1);
            
            // Process batch
            for (int i = 0; i < 5; i++) {
                int size = 500 + i * 100; // Varying non-power-of-2 sizes
                double[] signal = modwtPool.borrowArray(size);
                
                try {
                    MODWTResult result = transform.forward(signal);
                    System.out.printf("     Processed signal of size %d\n", size);
                } finally {
                    modwtPool.returnArray(signal);
                }
            }
            
            // Clear between batches
            modwtPool.clear();
            System.out.println("   Cleared pool after batch");
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
        
        // Create MODWT transform for realistic workload
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Simulate workload with monitoring
        long startTime = System.nanoTime();
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            // Vary array sizes to simulate real workload
            int size = 512 + (i % 8) * 256;
            double[] array = pool.borrowArray(size);
            
            try {
                // Fill with test data
                for (int j = 0; j < size; j++) {
                    array[j] = Math.sin(j * 0.1);
                }
                
                // Process with MODWT
                MODWTResult result = transform.forward(array);
                
                // Simulate using the result
                double sum = result.approximationCoeffs()[0] + result.detailCoeffs()[0];
                
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
        
        // Anti-pattern 4: Wrong size assumptions with MODWT
        System.out.println("\n❌ Anti-pattern 4: Assuming power-of-2 sizes (not needed with MODWT!)");
        System.out.println("   DWT required padding to power-of-2, wasting memory");
        System.out.println("   MODWT works with ANY size - no padding needed!");
        
        // Correct patterns summary
        System.out.println("\n✅ Correct Patterns:");
        System.out.println("   1. Always use try-finally to ensure arrays are returned");
        System.out.println("   2. Clear sensitive data before returning to pool");
        System.out.println("   3. Set reasonable pool size limits");
        System.out.println("   4. Monitor pool statistics in production");
        System.out.println("   5. Clear pools periodically or at phase boundaries");
        System.out.println("   6. With MODWT, borrow exact sizes needed - no padding!");
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