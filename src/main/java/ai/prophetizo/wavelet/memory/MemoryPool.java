package ai.prophetizo.wavelet.memory;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple memory pool for reusing double arrays to reduce garbage collection pressure.
 *
 * <p>This pool maintains collections of arrays grouped by size, allowing efficient
 * reuse of memory for repeated operations.</p>
 */
public class MemoryPool {

    private final Map<Integer, Queue<double[]>> pools = new ConcurrentHashMap<>();
    private int maxArraysPerSize = 10;
    private long totalBorrowed = 0;
    private long totalReturned = 0;
    private long totalCreated = 0;

    /**
     * Borrows an array of the specified size from the pool.
     * If no array is available, a new one is created.
     *
     * @param size the size of the array needed
     * @return a double array of the requested size
     */
    public double[] borrowArray(int size) {
        Queue<double[]> pool = pools.computeIfAbsent(size, k -> new ConcurrentLinkedQueue<>());
        double[] array = pool.poll();

        totalBorrowed++;
        if (array == null) {
            array = new double[size];
            totalCreated++;
        }

        return array;
    }

    /**
     * Returns an array to the pool for reuse.
     * The array is cleared (filled with zeros) before being pooled.
     *
     * @param array the array to return
     */
    public void returnArray(double[] array) {
        if (array == null) return;

        int size = array.length;
        Queue<double[]> pool = pools.computeIfAbsent(size, k -> new ConcurrentLinkedQueue<>());

        // Only pool if we haven't reached the limit
        if (pool.size() < maxArraysPerSize) {
            // Clear the array
            Arrays.fill(array, 0.0);
            pool.offer(array);
            totalReturned++;
        }
    }

    /**
     * Sets the maximum number of arrays to keep per size.
     *
     * @param max the maximum number of arrays per size
     */
    public void setMaxArraysPerSize(int max) {
        this.maxArraysPerSize = max;
    }

    /**
     * Clears all pooled arrays.
     */
    public void clear() {
        pools.clear();
    }

    /**
     * Gets the number of arrays currently pooled for a specific size.
     *
     * @param size the array size
     * @return the number of pooled arrays
     */
    public int getPooledCount(int size) {
        Queue<double[]> pool = pools.get(size);
        return pool != null ? pool.size() : 0;
    }

    /**
     * Gets the total number of arrays currently in all pools.
     *
     * @return the total number of pooled arrays
     */
    public int getTotalPooledCount() {
        return pools.values().stream()
                .mapToInt(Queue::size)
                .sum();
    }

    /**
     * Prints pool statistics for monitoring.
     */
    public void printStatistics() {
        System.out.println("Memory Pool Statistics:");
        System.out.printf("  Total borrowed: %d\n", totalBorrowed);
        System.out.printf("  Total returned: %d\n", totalReturned);
        System.out.printf("  Total created: %d\n", totalCreated);
        System.out.printf("  Reuse rate: %.1f%%\n",
                totalBorrowed > 0 ? 100.0 * (totalBorrowed - totalCreated) / totalBorrowed : 0);
        System.out.printf("  Currently pooled: %d arrays\n", getTotalPooledCount());

        if (!pools.isEmpty()) {
            System.out.println("  Pool sizes:");
            pools.forEach((size, pool) -> {
                if (!pool.isEmpty()) {
                    System.out.printf("    Size %d: %d arrays\n", size, pool.size());
                }
            });
        }
    }
}