package ai.prophetizo.wavelet.concurrent;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine.MultiLevelResult;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine.SignalProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
class ParallelWaveletEngineTest {
    
    private ParallelWaveletEngine engine;
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        engine = new ParallelWaveletEngine();
    }
    
    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }
    
    @Test
    void testConstructors() {
        // Default constructor
        assertDoesNotThrow(() -> {
            try (ParallelWaveletEngine e = new ParallelWaveletEngine()) {
                assertNotNull(e);
            }
        });
        
        // With parallelism
        assertDoesNotThrow(() -> {
            try (ParallelWaveletEngine e = new ParallelWaveletEngine(4)) {
                assertNotNull(e);
            }
        });
        
        // With existing executor
        ForkJoinPool pool = new ForkJoinPool(2);
        try {
            ParallelWaveletEngine e = new ParallelWaveletEngine(pool);
            assertNotNull(e);
            e.close(); // Should not shutdown the pool
            assertTrue(pool.isQuiescent());
            assertFalse(pool.isShutdown());
        } finally {
            pool.shutdown();
        }
    }
    
    @Test
    void testBatchTransform() {
        double[][] signals = createTestSignals(100, 64);
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertEquals(100, results.length);
        for (TransformResult result : results) {
            assertNotNull(result);
            assertNotNull(result.approximationCoeffs());
            assertNotNull(result.detailCoeffs());
            assertEquals(64, result.approximationCoeffs().length + result.detailCoeffs().length);
        }
    }
    
    @Test
    void testSmallBatchFallback() {
        // Small batch should use sequential processing
        double[][] signals = createTestSignals(2, 64);
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertEquals(2, results.length);
        for (TransformResult result : results) {
            assertNotNull(result);
        }
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testAsyncTransform() throws Exception {
        double[][] signals = createTestSignals(50, 128);
        
        CompletableFuture<TransformResult[]> future = 
            engine.transformBatchAsync(signals, Daubechies.DB4, BoundaryMode.ZERO_PADDING);
        
        assertNotNull(future);
        TransformResult[] results = future.get();
        
        assertEquals(50, results.length);
        for (TransformResult result : results) {
            assertNotNull(result);
            assertEquals(128, result.approximationCoeffs().length + result.detailCoeffs().length);
        }
    }
    
    @Test
    void testProcessBatch() {
        double[][] signals = createTestSignals(100, 64);
        
        // Custom processor that computes mean
        SignalProcessor<Double> meanProcessor = signal -> {
            double sum = 0;
            for (double v : signal) {
                sum += v;
            }
            return sum / signal.length;
        };
        
        List<Double> means = engine.processBatch(signals, meanProcessor);
        
        assertEquals(100, means.size());
        for (Double mean : means) {
            assertNotNull(mean);
            assertTrue(Math.abs(mean) < 1.0); // Random signals should have mean near 0
        }
    }
    
    @Test
    void testMultiLevelDecompose() {
        double[][] signals = createTestSignals(20, 128);
        int levels = 3;
        
        MultiLevelResult[] results = engine.multiLevelDecomposeBatch(
            signals, new Haar(), levels, BoundaryMode.PERIODIC);
        
        assertEquals(20, results.length);
        
        for (MultiLevelResult result : results) {
            assertNotNull(result);
            assertTrue(result.getLevels() <= levels);
            assertTrue(result.getLevels() > 0);
            
            // Check each level
            for (int i = 0; i < result.getLevels(); i++) {
                TransformResult levelResult = result.getLevel(i);
                assertNotNull(levelResult);
            }
            
            // Check final approximation
            assertNotNull(result.getFinalApproximation());
        }
    }
    
    @Test
    void testMultiLevelWithSmallSignals() {
        // Test with signals that become too small for all levels
        double[][] signals = createTestSignals(10, 16); // Can only do 2 levels max
        int requestedLevels = 5;
        
        MultiLevelResult[] results = engine.multiLevelDecomposeBatch(
            signals, new Haar(), requestedLevels, BoundaryMode.PERIODIC);
        
        for (MultiLevelResult result : results) {
            assertTrue(result.getLevels() < requestedLevels);
            assertTrue(result.getLevels() <= 3); // 16 -> 8 -> 4 -> 2 (but might stop at 4)
        }
    }
    
    @Test
    void testConcurrentAccess() throws Exception {
        int threadCount = 4;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        double[][] signals = createTestSignals(25, 64);
                        TransformResult[] results = engine.transformBatch(
                            signals, new Haar(), BoundaryMode.PERIODIC);
                        
                        if (results.length == 25) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }));
            }
            
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(threadCount, successCount.get());
            
            // Ensure all tasks completed successfully
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }
    }
    
    @Test
    void testExceptionHandling() {
        // Test with null signals
        assertThrows(RuntimeException.class, () -> {
            engine.transformBatch(null, new Haar(), BoundaryMode.PERIODIC);
        });
        
        // Test with invalid signal (will fail in WaveletTransform)
        assertThrows(RuntimeException.class, () -> {
            double[][] signals = new double[][]{ new double[]{1.0} }; // Too small
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        });
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8})
    void testDifferentParallelismLevels(int parallelism) {
        try (ParallelWaveletEngine customEngine = new ParallelWaveletEngine(parallelism)) {
            double[][] signals = createTestSignals(100, 64);
            TransformResult[] results = customEngine.transformBatch(
                signals, new Haar(), BoundaryMode.PERIODIC);
            
            assertEquals(100, results.length);
        }
    }
    
    @Test
    void testResultConsistency() {
        // Verify parallel results match sequential
        double[][] signals = createTestSignals(50, 64);
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.ZERO_PADDING;
        
        // Parallel results
        TransformResult[] parallelResults = engine.transformBatch(signals, wavelet, mode);
        
        // Sequential results
        TransformResult[] sequentialResults = new TransformResult[signals.length];
        WaveletTransform transform = new WaveletTransform(wavelet, mode);
        for (int i = 0; i < signals.length; i++) {
            sequentialResults[i] = transform.forward(signals[i]);
        }
        
        // Compare
        assertEquals(sequentialResults.length, parallelResults.length);
        for (int i = 0; i < signals.length; i++) {
            assertArrayEquals(
                sequentialResults[i].approximationCoeffs(),
                parallelResults[i].approximationCoeffs(),
                EPSILON
            );
            assertArrayEquals(
                sequentialResults[i].detailCoeffs(),
                parallelResults[i].detailCoeffs(),
                EPSILON
            );
        }
    }
    
    @Test
    void testCloseWithCustomExecutor() {
        ForkJoinPool customPool = new ForkJoinPool(2);
        ParallelWaveletEngine customEngine = new ParallelWaveletEngine(customPool);
        
        double[][] signals = createTestSignals(10, 32);
        customEngine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        customEngine.close();
        
        // Pool should still be active
        assertFalse(customPool.isShutdown());
        
        customPool.shutdown();
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testShutdownTimeout() throws Exception {
        // Test shutdown with pending tasks
        try (ParallelWaveletEngine timeoutEngine = new ParallelWaveletEngine(1)) {
            // Submit a long-running task
            CompletableFuture<TransformResult[]> future = timeoutEngine.transformBatchAsync(
                createTestSignals(1000, 1024), 
                Daubechies.DB4, 
                BoundaryMode.PERIODIC
            );
            
            // The engine will be closed immediately by try-with-resources
            // The future might complete or be cancelled
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (CancellationException | TimeoutException e) {
                // Expected if task was cancelled
            }
        }
    }
    
    // Helper method to create test signals
    private double[][] createTestSignals(int count, int length) {
        Random random = new Random(TestConstants.TEST_SEED); // Fixed seed for reproducibility
        double[][] signals = new double[count][length];
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < length; j++) {
                signals[i][j] = random.nextGaussian();
            }
        }
        
        return signals;
    }
}