package ai.prophetizo.wavelet.integration;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTTransformFactory;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.test.BaseMODWTTest;
import ai.prophetizo.wavelet.util.ToleranceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Stress tests for wavelet transforms.
 * Tests performance, concurrency, and large-scale operations.
 * 
 * These tests are disabled by default. Enable with:
 * -Dstress.tests.enabled=true
 */
@DisplayName("Wavelet Transform Stress Tests")
@EnabledIfSystemProperty(named = "stress.tests.enabled", matches = "true")
class StressTest extends BaseMODWTTest {
    
    private MODWTTransformFactory factory;
    private static final String STRESS_TESTS_ENABLED_PROPERTY = "stress.tests.enabled";
    private static final int LARGE_SIGNAL_SIZE = 65536; // MODWT works with any size
    private static final int VERY_LARGE_SIGNAL_SIZE = 1048576; // MODWT works with any size
    
    private static final double TOLERANCE = ToleranceConstants.DEFAULT_TOLERANCE;
    
    @BeforeEach
    protected void setUp(org.junit.jupiter.api.TestInfo testInfo) {
        super.setUp(testInfo);
        factory = new MODWTTransformFactory();
    }
    
    @Test
    @DisplayName("Should handle large signals efficiently")
    void testLargeSignalPerformance() {
        MODWTTransform transform = factory.create(new Haar());
        double[] signal = createRandomSignal(LARGE_SIGNAL_SIZE);
        
        long startTime = System.nanoTime();
        MODWTResult result = transform.forward(signal);
        long forwardTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        double[] reconstructed = transform.inverse(result);
        long inverseTime = System.nanoTime() - startTime;
        
        // Log performance
        logger.info(String.format("Large signal (%d samples) - Forward: %.2f ms, Inverse: %.2f ms",
            LARGE_SIGNAL_SIZE, forwardTime / 1e6, inverseTime / 1e6));
        
        // Verify correctness
        assertArrayEquals(signal, reconstructed, 1e-10);
        
        // Performance assertions - MODWT is slower than DWT due to non-decimated nature
        assertTrue(forwardTime < 200_000_000L, // 200ms for MODWT
            "Forward transform took too long: " + (forwardTime / 1e6) + " ms");
        assertTrue(inverseTime < 200_000_000L, // 200ms for MODWT
            "Inverse transform took too long: " + (inverseTime / 1e6) + " ms");
    }
    
    @Test
    @DisplayName("Should handle very large signals")
    void testVeryLargeSignal() {
        MODWTTransform transform = factory.create(new Haar());
        double[] signal = createRandomSignal(VERY_LARGE_SIGNAL_SIZE);
        
        assertDoesNotThrow(() -> {
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Spot check some values
            Random random = new Random(TestConstants.TEST_SEED);
            for (int i = 0; i < 100; i++) {
                int idx = random.nextInt(signal.length);
                assertEquals(signal[idx], reconstructed[idx], 1e-10,
                    "Mismatch at index " + idx);
            }
        });
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16})
    @DisplayName("Should handle concurrent transforms")
    void testConcurrentTransforms(int threadCount) throws Exception {
        MODWTTransform transform = factory.create(new Haar());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        // Each thread processes multiple signals
                        for (int i = 0; i < 100; i++) {
                            double[] signal = createRandomSignal(256, threadId * 1000L + i);
                            MODWTResult result = transform.forward(signal);
                            double[] reconstructed = transform.inverse(result);
                            
                            // Verify reconstruction
                            for (int j = 0; j < signal.length; j++) {
                                if (Math.abs(signal[j] - reconstructed[j]) > 1e-10) {
                                    throw new AssertionError(
                                        String.format("Thread %d, iteration %d: mismatch at index %d",
                                            threadId, i, j));
                                }
                            }
                        }
                    } catch (Throwable e) {
                        errors.offer(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Timeout waiting for threads");
            
            if (!errors.isEmpty()) {
                Throwable firstError = errors.poll();
                fail("Concurrent execution failed: " + firstError.getMessage());
            }
        } finally {
            executor.shutdown();
        }
    }
    
    @Test
    @DisplayName("Should maintain accuracy over many iterations")
    void testIterativeStability() {
        MODWTTransform transform = factory.create(new Haar());
        double[] original = createRandomSignal(1024);
        double[] signal = original.clone();
        
        // Apply forward and inverse transform multiple times
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            MODWTResult result = transform.forward(signal);
            signal = transform.inverse(result);
        }
        
        // Check accumulated error
        double maxError = 0;
        double totalError = 0;
        for (int i = 0; i < original.length; i++) {
            double error = Math.abs(original[i] - signal[i]);
            maxError = Math.max(maxError, error);
            totalError += error * error;
        }
        
        double rmsError = Math.sqrt(totalError / original.length);
        
        logger.info(String.format("After %d iterations - Max error: %e, RMS error: %e",
            iterations, maxError, rmsError));
        
        assertTrue(maxError < 1e-10, "Maximum error too large: " + maxError);
        assertTrue(rmsError < 1e-12, "RMS error too large: " + rmsError);
    }
    
    @Test
    @DisplayName("Memory stress test with many transforms")
    void testMemoryStress() {
        MODWTTransform transform = factory.create(new Haar());
        
        // Process many signals without keeping references
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 1000; i++) {
                double[] signal = createRandomSignal(4096);
                MODWTResult result = transform.forward(signal);
                double[] reconstructed = transform.inverse(result);
                
                // Just verify first and last values
                assertEquals(signal[0], reconstructed[0], 1e-10);
                assertEquals(signal[signal.length - 1], 
                           reconstructed[reconstructed.length - 1], 1e-10);
                
                if (i % 100 == 0) {
                    logger.info("Processed " + i + " signals");
                }
            }
        });
    }
    
    @Test
    @DisplayName("Performance benchmark for large signals")
    void testLargeSignalPerformanceBenchmark() {
        double[] signal = createRandomSignal(LARGE_SIGNAL_SIZE);
        
        // Create transform with default settings (automatically selects best implementation)
        MODWTTransform transform = factory.create(new Haar());
            
        // Warmup
        for (int i = 0; i < 10; i++) {
            MODWTResult result = transform.forward(signal);
            transform.inverse(result);
        }
        
        // Measure performance
        long startTime = System.nanoTime();
        MODWTResult result = transform.forward(signal);
        long forwardTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        double[] reconstructed = transform.inverse(result);
        long inverseTime = System.nanoTime() - startTime;
        
        logger.info(String.format("Large signal performance (%d samples) - Forward: %.2f ms, Inverse: %.2f ms",
            LARGE_SIGNAL_SIZE, forwardTime / 1e6, inverseTime / 1e6));
        
        // Verify correctness
        assertArrayEquals(signal, reconstructed, 1e-10);
    }
    
    @Test
    @DisplayName("Should handle rapid allocation/deallocation cycles")
    void testRapidAllocationCycles() {
        MODWTTransform transform = factory.create(new Haar());
        
        // Rapidly create and process signals of varying sizes
        Random random = new Random(12345);
        
        for (int i = 0; i < 1000; i++) {
            int size = 16 + random.nextInt(1009); // 16 to 1024, any size works with MODWT
            double[] signal = createRandomSignal(size, i);
            
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Spot check
            int checkIdx = random.nextInt(size);
            assertEquals(signal[checkIdx], reconstructed[checkIdx], 1e-10,
                "Failed at iteration " + i);
        }
    }
    
    // === Helper Methods ===
    
    private double[] createRandomSignal(int length) {
        return createRandomSignal(length, System.nanoTime());
    }
    
    private double[] createRandomSignal(int length, long seed) {
        Random random = new Random(seed);
        return IntStream.range(0, length)
            .mapToDouble(i -> random.nextGaussian())
            .toArray();
    }
}