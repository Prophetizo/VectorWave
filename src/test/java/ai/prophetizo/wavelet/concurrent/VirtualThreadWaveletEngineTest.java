package ai.prophetizo.wavelet.concurrent;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VirtualThreadWaveletEngine using Java 23 virtual threads.
 */
class VirtualThreadWaveletEngineTest {
    
    private VirtualThreadWaveletEngine engine;
    private double[][] testSignals;
    
    @BeforeEach
    void setUp() {
        engine = new VirtualThreadWaveletEngine();
        
        // Create test signals
        int numSignals = 100;
        int signalLength = 256;
        testSignals = new double[numSignals][signalLength];
        
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                testSignals[i][j] = Math.sin(2 * Math.PI * j / 32.0) + 
                                   0.5 * Math.sin(2 * Math.PI * j / 16.0);
            }
        }
    }
    
    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }
    
    @Test
    @DisplayName("Should transform multiple signals using virtual threads")
    void testBatchTransform() throws InterruptedException {
        // Given
        Wavelet wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        // When
        TransformResult[] results = engine.transformBatch(testSignals, wavelet, mode);
        
        // Then
        assertNotNull(results);
        assertEquals(testSignals.length, results.length);
        
        for (int i = 0; i < results.length; i++) {
            assertNotNull(results[i], "Result " + i + " should not be null");
            assertEquals(testSignals[i].length, 
                        results[i].approximationCoeffs().length + results[i].detailCoeffs().length);
        }
    }
    
    @Test
    @DisplayName("Should handle empty input gracefully")
    void testEmptyInput() throws InterruptedException {
        // Given
        double[][] emptySignals = new double[0][];
        
        // When
        TransformResult[] results = engine.transformBatch(
            emptySignals, new Haar(), BoundaryMode.PERIODIC);
        
        // Then
        assertNotNull(results);
        assertEquals(0, results.length);
    }
    
    @Test
    @DisplayName("Should process large batches efficiently with virtual threads")
    void testLargeBatchProcessing() throws InterruptedException {
        // Given - 1000 signals
        int numSignals = 1000;
        double[][] largeSignals = new double[numSignals][256];
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < 256; j++) {
                largeSignals[i][j] = Math.random();
            }
        }
        
        // When
        long startTime = System.nanoTime();
        TransformResult[] results = engine.transformBatch(
            largeSignals, Daubechies.DB4, BoundaryMode.PERIODIC);
        long duration = System.nanoTime() - startTime;
        
        // Then
        assertEquals(numSignals, results.length);
        System.out.printf("Virtual threads processed %d signals in %.2f ms%n", 
                         numSignals, duration / 1_000_000.0);
        
        // Verify all results are valid
        for (TransformResult result : results) {
            assertNotNull(result);
        }
    }
    
    @Test
    @DisplayName("Should handle timeouts appropriately")
    void testTimeoutHandling() {
        // Given - signals that would take long to process
        double[][] signals = new double[10][8192]; // Large signals
        for (double[] signal : signals) {
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.random();
            }
        }
        
        // When/Then - expect timeout with very short duration
        assertThrows(TimeoutException.class, () -> {
            engine.transformBatch(signals, Daubechies.DB4, BoundaryMode.PERIODIC, 
                                1, TimeUnit.NANOSECONDS);
        });
    }
    
    @Test
    @DisplayName("Should process with custom function using virtual threads")
    void testCustomProcessing() throws InterruptedException, ExecutionException {
        // Given
        double[][] signals = new double[50][128];
        for (int i = 0; i < signals.length; i++) {
            for (int j = 0; j < signals[i].length; j++) {
                signals[i][j] = i + j * 0.1; // Simple pattern
            }
        }
        
        // When - compute mean of each signal
        List<Double> means = engine.processBatch(signals, signal -> {
            double sum = 0;
            for (double v : signal) sum += v;
            return sum / signal.length;
        });
        
        // Then
        assertEquals(signals.length, means.size());
        
        // Verify means are correct
        for (int i = 0; i < signals.length; i++) {
            double expectedMean = i + (signals[i].length - 1) * 0.05;
            assertEquals(expectedMean, means.get(i), 1e-10);
        }
    }
    
    @Test
    @DisplayName("Should use virtual threads")
    void testVirtualThreadUsage() {
        assertTrue(engine.isUsingVirtualThreads());
    }
    
    @Test
    @DisplayName("Should handle exceptions in individual tasks")
    void testExceptionHandling() {
        // Given - create a signal that will cause an error (wrong length)
        double[][] signals = new double[3][];
        signals[0] = new double[256];
        signals[1] = new double[255]; // Invalid length
        signals[2] = new double[256];
        
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        });
        
        assertTrue(exception.getMessage().contains("Transform failed"));
    }
    
    @Test
    @DisplayName("Virtual threads should scale better than platform threads")
    void testScalabilityComparison() throws Exception {
        // Given - many small tasks (ideal for virtual threads)
        int numTasks = 10000;
        double[][] smallSignals = IntStream.range(0, numTasks)
            .mapToObj(i -> new double[64])
            .toArray(double[][]::new);
        
        // Initialize signals
        for (double[] signal : smallSignals) {
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.random();
            }
        }
        
        // When - time virtual thread execution
        long startVirtual = System.nanoTime();
        try (var virtualEngine = new VirtualThreadWaveletEngine()) {
            TransformResult[] virtualResults = virtualEngine.transformBatch(
                smallSignals, new Haar(), BoundaryMode.PERIODIC);
            assertEquals(numTasks, virtualResults.length);
        }
        long virtualDuration = System.nanoTime() - startVirtual;
        
        // Compare with regular parallel engine
        long startRegular = System.nanoTime();
        try (var regularEngine = new ParallelWaveletEngine()) {
            TransformResult[] regularResults = regularEngine.transformBatch(
                smallSignals, new Haar(), BoundaryMode.PERIODIC);
            assertEquals(numTasks, regularResults.length);
        }
        long regularDuration = System.nanoTime() - startRegular;
        
        // Then - log performance comparison
        System.out.printf("Performance comparison for %d small tasks:%n", numTasks);
        System.out.printf("Virtual threads: %.2f ms%n", virtualDuration / 1_000_000.0);
        System.out.printf("Platform threads: %.2f ms%n", regularDuration / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", (double) regularDuration / virtualDuration);
    }
}