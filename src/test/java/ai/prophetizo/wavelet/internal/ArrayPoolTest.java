package ai.prophetizo.wavelet.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ArrayPool.
 * Tests thread-safety, pooling behavior, and memory management.
 */
public class ArrayPoolTest {
    
    @BeforeEach
    void setup() {
        // Clear pool before each test
        clearPool();
    }
    
    @AfterEach
    void cleanup() {
        // Clear pool after each test
        clearPool();
    }
    
    @Test
    @DisplayName("Test borrowing arrays of standard sizes")
    void testBorrowStandardSizes() {
        int[] standardSizes = {32, 64, 128, 256, 512, 1024};
        
        for (int size : standardSizes) {
            double[] array = borrowArray(size);
            assertNotNull(array, "Array should not be null");
            assertEquals(size, array.length, "Array should have correct size");
            assertArrayEquals(new double[size], array, "Array should be zero-initialized");
        }
    }
    
    @Test
    @DisplayName("Test borrowing arrays of non-standard sizes")
    void testBorrowNonStandardSizes() {
        int[] nonStandardSizes = {17, 33, 100, 500, 1000, 2048};
        
        for (int size : nonStandardSizes) {
            double[] array = borrowArray(size);
            assertNotNull(array, "Array should not be null");
            assertEquals(size, array.length, "Array should have correct size");
            assertArrayEquals(new double[size], array, "Array should be zero-initialized");
        }
    }
    
    @Test
    @DisplayName("Test releasing and reusing arrays")
    void testReleaseAndReuse() {
        // Borrow an array
        double[] array1 = borrowArray(64);
        Arrays.fill(array1, 1.0); // Modify the array
        
        // Release it
        releaseArray(array1);
        
        // Borrow another array of the same size
        double[] array2 = borrowArray(64);
        
        // Should get a cleared array (might be the same instance)
        assertArrayEquals(new double[64], array2, "Reused array should be cleared");
    }
    
    @Test
    @DisplayName("Test pool limit enforcement")
    void testPoolLimitEnforcement() throws Exception {
        int size = 64;
        int maxArraysPerSize = 4; // From the implementation
        
        // Borrow and release more arrays than the limit
        double[][] arrays = new double[maxArraysPerSize + 2][];
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = borrowArray(size);
        }
        
        // Release all arrays
        for (double[] array : arrays) {
            releaseArray(array);
        }
        
        // Borrow arrays again - pool should only return up to maxArraysPerSize
        // Additional arrays will be newly allocated
        for (int i = 0; i < maxArraysPerSize + 1; i++) {
            double[] borrowed = borrowArray(size);
            assertNotNull(borrowed);
            assertEquals(size, borrowed.length);
        }
    }
    
    @Test
    @DisplayName("Test clearing the pool")
    void testClearPool() {
        // Borrow and release some arrays
        double[] array1 = borrowArray(32);
        double[] array2 = borrowArray(64);
        releaseArray(array1);
        releaseArray(array2);
        
        // Clear the pool
        clearPool();
        
        // Borrow again - should get new arrays (not from pool)
        double[] array3 = borrowArray(32);
        double[] array4 = borrowArray(64);
        
        assertNotNull(array3);
        assertNotNull(array4);
        // Can't easily verify they're new instances, but they should be cleared
        assertArrayEquals(new double[32], array3);
        assertArrayEquals(new double[64], array4);
    }
    
    @Test
    @DisplayName("Test releasing null arrays")
    void testReleaseNull() {
        // Should not throw exception
        assertDoesNotThrow(() -> releaseArray(null));
    }
    
    @Test
    @DisplayName("Test thread safety with concurrent operations")
    void testThreadSafety() throws Exception {
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Randomly choose size
                        int[] sizes = {32, 64, 128, 256};
                        int size = sizes[j % sizes.length];
                        
                        // Borrow
                        double[] array = borrowArray(size);
                        assertNotNull(array);
                        assertEquals(size, array.length);
                        
                        // Use the array
                        Arrays.fill(array, j);
                        
                        // Release
                        releaseArray(array);
                        
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
        
        assertEquals(numThreads * operationsPerThread, successCount.get(),
            "All operations should succeed");
    }
    
    @Test
    @DisplayName("Test array clearing on release")
    void testArrayClearingOnRelease() {
        double[] array = borrowArray(64);
        
        // Fill with non-zero values
        Arrays.fill(array, 42.0);
        
        // Release the array
        releaseArray(array);
        
        // The array should be cleared (though we can't directly verify this
        // without accessing it after release, which would be incorrect usage)
        
        // Instead, borrow another array of the same size
        double[] newArray = borrowArray(64);
        
        // It should be cleared
        for (double value : newArray) {
            assertEquals(0.0, value, "Array should be cleared after release");
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {32, 64, 128, 256, 512, 1024})
    @DisplayName("Test pooling for all standard sizes")
    void testPoolingForStandardSizes(int size) {
        // Borrow an array
        double[] array1 = borrowArray(size);
        assertNotNull(array1);
        
        // Mark it somehow (we'll use the first element)
        array1[0] = size;
        
        // Release it
        releaseArray(array1);
        
        // Borrow again - should be cleared
        double[] array2 = borrowArray(size);
        assertEquals(0.0, array2[0], "Pooled array should be cleared");
        
        // Release for cleanup
        releaseArray(array2);
    }
    
    @Test
    @DisplayName("Test Pool inner class directly")
    void testPoolInnerClass() throws Exception {
        // Use reflection to access the inner Pool class
        Class<?> poolClass = Class.forName("ai.prophetizo.wavelet.internal.ArrayPool$Pool");
        Constructor<?> constructor = poolClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object pool = constructor.newInstance();
        
        // Get methods
        Method borrowMethod = poolClass.getDeclaredMethod("borrow", int.class);
        Method releaseMethod = poolClass.getDeclaredMethod("release", double[].class);
        Method clearMethod = poolClass.getDeclaredMethod("clear");
        borrowMethod.setAccessible(true);
        releaseMethod.setAccessible(true);
        clearMethod.setAccessible(true);
        
        // Test borrow
        double[] array = (double[]) borrowMethod.invoke(pool, 64);
        assertNotNull(array);
        assertEquals(64, array.length);
        
        // Test release
        releaseMethod.invoke(pool, array);
        
        // Test clear
        clearMethod.invoke(pool);
        
        // Test releasing null
        releaseMethod.invoke(pool, (Object) null); // Should not throw
        
        // Test non-standard size
        double[] nonStandardArray = (double[]) borrowMethod.invoke(pool, 77);
        assertNotNull(nonStandardArray);
        assertEquals(77, nonStandardArray.length);
        
        // Release non-standard size (should not be pooled)
        releaseMethod.invoke(pool, nonStandardArray);
    }
    
    @Test
    @DisplayName("Test getPoolIndex method")
    void testGetPoolIndex() throws Exception {
        // Use reflection to test the private getPoolIndex method
        Class<?> poolClass = Class.forName("ai.prophetizo.wavelet.internal.ArrayPool$Pool");
        Constructor<?> constructor = poolClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object pool = constructor.newInstance();
        
        Method getPoolIndexMethod = poolClass.getDeclaredMethod("getPoolIndex", int.class);
        getPoolIndexMethod.setAccessible(true);
        
        // Test standard sizes
        assertEquals(0, getPoolIndexMethod.invoke(pool, 32));
        assertEquals(1, getPoolIndexMethod.invoke(pool, 64));
        assertEquals(2, getPoolIndexMethod.invoke(pool, 128));
        assertEquals(3, getPoolIndexMethod.invoke(pool, 256));
        assertEquals(4, getPoolIndexMethod.invoke(pool, 512));
        assertEquals(5, getPoolIndexMethod.invoke(pool, 1024));
        
        // Test non-standard sizes
        assertEquals(-1, getPoolIndexMethod.invoke(pool, 31));
        assertEquals(-1, getPoolIndexMethod.invoke(pool, 33));
        assertEquals(-1, getPoolIndexMethod.invoke(pool, 100));
        assertEquals(-1, getPoolIndexMethod.invoke(pool, 2048));
    }
    
    // Helper methods to access ArrayPool methods directly
    private double[] borrowArray(int size) {
        return ArrayPool.borrow(size);
    }
    
    private void releaseArray(double[] array) {
        ArrayPool.release(array);
    }
    
    private void clearPool() {
        ArrayPool.clear();
    }
}