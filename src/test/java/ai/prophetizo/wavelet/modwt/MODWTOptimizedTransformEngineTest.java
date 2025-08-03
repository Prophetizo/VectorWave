package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MODWTOptimizedTransformEngine.
 * 
 * @since 3.0.0
 */
class MODWTOptimizedTransformEngineTest {
    
    private static final double EPSILON = 1e-10;
    private MODWTOptimizedTransformEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new MODWTOptimizedTransformEngine();
    }
    
    @Test
    void testSingleTransformHaar() {
        double[] signal = createTestSignal(250);  // Non-power-of-2
        Wavelet wavelet = new Haar();
        
        MODWTResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(250, result.approximationCoeffs().length);
        assertEquals(250, result.detailCoeffs().length);
        
        // Compare with standard MODWT transform
        MODWTTransform standard = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        MODWTResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), result.approximationCoeffs(), EPSILON);
        assertArrayEquals(expected.detailCoeffs(), result.detailCoeffs(), EPSILON);
    }
    
    @Test
    void testSingleTransformDB4() {
        double[] signal = createTestSignal(500);  // Non-power-of-2
        Wavelet wavelet = Daubechies.DB4;
        
        MODWTResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(500, result.approximationCoeffs().length);
        assertEquals(500, result.detailCoeffs().length);
        
        // Verify energy (MODWT has redundancy factor of 2)
        double inputEnergy = computeEnergy(signal);
        double outputEnergy = (computeEnergy(result.approximationCoeffs()) + 
                              computeEnergy(result.detailCoeffs())) / 2.0;
        
        assertEquals(inputEnergy, outputEnergy, inputEnergy * 0.01);
    }
    
    @Test
    void testMultiLevelTransform() {
        double[] signal = createTestSignal(300);
        Wavelet wavelet = new Haar();
        int levels = 3;
        
        MultiLevelMODWTResult result = engine.transformMultiLevel(
            signal, wavelet, BoundaryMode.PERIODIC, levels);
        
        assertNotNull(result);
        assertEquals(levels, result.getLevels());
        assertEquals(300, result.getSignalLength());
        
        // Check all levels have correct length
        for (int level = 1; level <= levels; level++) {
            assertEquals(300, result.getDetailCoeffsAtLevel(level).length);
        }
        assertEquals(300, result.getApproximationCoeffs().length);
    }
    
    @Test
    void testBatchTransformSmall() {
        int batchSize = 5;
        double[][] signals = createTestSignals(batchSize, 150);  // Non-power-of-2
        Wavelet wavelet = new Haar();
        
        MODWTResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Verify each result
        for (int i = 0; i < batchSize; i++) {
            assertEquals(150, results[i].approximationCoeffs().length);
            assertEquals(150, results[i].detailCoeffs().length);
            assertTrue(results[i].isValid());
        }
    }
    
    @Test
    void testBatchTransformMedium() {
        // This should trigger SoA optimization
        int batchSize = 8;
        double[][] signals = createTestSignals(batchSize, 256);
        Wavelet wavelet = new Haar();
        
        MODWTResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Compare with standard MODWT transform
        MODWTTransform standard = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            MODWTResult expected = standard.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), 
                            results[i].approximationCoeffs(), EPSILON);
            assertArrayEquals(expected.detailCoeffs(), 
                            results[i].detailCoeffs(), EPSILON);
        }
    }
    
    @Test
    void testBatchTransformLarge() {
        // This should trigger parallel processing
        int batchSize = 150;
        double[][] signals = createTestSignals(batchSize, 100);
        Wavelet wavelet = new Haar();
        
        MODWTResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Verify first and last results
        assertNotNull(results[0]);
        assertNotNull(results[batchSize - 1]);
        assertEquals(100, results[0].getSignalLength());
        assertEquals(100, results[batchSize - 1].getSignalLength());
    }
    
    @Test
    void testAsyncTransform() throws Exception {
        int batchSize = 32;
        double[][] signals = createTestSignals(batchSize, 128);
        Wavelet wavelet = Daubechies.DB2;
        
        CompletableFuture<MODWTResult[]> future = engine.transformBatchAsync(
            signals, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(future);
        MODWTResult[] results = future.get(); // Wait for completion
        
        assertEquals(batchSize, results.length);
        for (MODWTResult result : results) {
            assertNotNull(result);
            assertEquals(128, result.getSignalLength());
        }
    }
    
    @Test
    void testEmptyBatch() {
        double[][] emptySignals = new double[0][];
        Wavelet wavelet = new Haar();
        
        MODWTResult[] results = engine.transformBatch(
            emptySignals, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(results);
        assertEquals(0, results.length);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8})
    void testCustomEngineConfig(int parallelism) {
        var config = new MODWTOptimizedTransformEngine.EngineConfig()
            .withParallelism(parallelism)
            .withMemoryPool(true)
            .withSpecializedKernels(true)
            .withSoALayout(true)
            .withCacheBlocking(true);
        
        MODWTOptimizedTransformEngine customEngine = new MODWTOptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(100);
        MODWTResult result = customEngine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(100, result.getSignalLength());
    }
    
    @Test
    void testNoSpecializedKernels() {
        var config = new MODWTOptimizedTransformEngine.EngineConfig()
            .withSpecializedKernels(false);
        
        MODWTOptimizedTransformEngine customEngine = new MODWTOptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(64);
        MODWTResult result = customEngine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(64, result.getSignalLength());
    }
    
    @Test
    void testContinuousWavelet() {
        // Test with a continuous wavelet (should fall back to standard transform)
        double[] signal = createTestSignal(128);
        Wavelet wavelet = new MorletWavelet();
        
        MODWTResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.getSignalLength());
    }
    
    // Helper methods
    
    private double[] createTestSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        for (int i = 0; i < length; i++) {
            signal[i] = rand.nextGaussian();
        }
        return signal;
    }
    
    private double[][] createTestSignals(int count, int length) {
        double[][] signals = new double[count][];
        Random rand = new Random(42);
        for (int i = 0; i < count; i++) {
            signals[i] = new double[length];
            for (int j = 0; j < length; j++) {
                signals[i][j] = rand.nextGaussian();
            }
        }
        return signals;
    }
    
    private double computeEnergy(double[] signal) {
        double energy = 0.0;
        for (double s : signal) {
            energy += s * s;
        }
        return energy;
    }
}