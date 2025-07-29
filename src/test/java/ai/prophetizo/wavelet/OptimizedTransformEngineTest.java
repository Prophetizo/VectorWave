package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class OptimizedTransformEngineTest {
    
    private static final double EPSILON = 1e-10;
    private OptimizedTransformEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new OptimizedTransformEngine();
    }
    
    @Test
    void testSingleTransformHaar() {
        double[] signal = createTestSignal(256);
        Wavelet wavelet = new Haar();
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
        assertEquals(128, result.detailCoeffs().length);
        
        // Compare with standard transform
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), result.approximationCoeffs(), EPSILON);
        assertArrayEquals(expected.detailCoeffs(), result.detailCoeffs(), EPSILON);
    }
    
    @Test
    void testSingleTransformDB4() {
        double[] signal = createTestSignal(512);
        Wavelet wavelet = Daubechies.DB4;
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(256, result.approximationCoeffs().length);
        assertEquals(256, result.detailCoeffs().length);
        
        // Verify energy preservation
        double inputEnergy = computeEnergy(signal);
        double outputEnergy = computeEnergy(result.approximationCoeffs()) + 
                             computeEnergy(result.detailCoeffs());
        
        assertEquals(inputEnergy, outputEnergy, inputEnergy * 0.001);
    }
    
    @Test
    void testBatchTransformSmall() {
        int batchSize = 5;
        double[][] signals = createTestSignals(batchSize, 128);
        Wavelet wavelet = new Haar();
        
        TransformResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Verify each result
        for (int i = 0; i < batchSize; i++) {
            assertEquals(64, results[i].approximationCoeffs().length);
            assertEquals(64, results[i].detailCoeffs().length);
        }
    }
    
    @Test
    void testBatchTransformMedium() {
        // This should trigger SoA optimization
        int batchSize = 8;
        double[][] signals = createTestSignals(batchSize, 256);
        Wavelet wavelet = new Haar();
        
        TransformResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Compare with standard transform
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            TransformResult expected = standard.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), 
                            results[i].approximationCoeffs(), EPSILON);
            assertArrayEquals(expected.detailCoeffs(), 
                            results[i].detailCoeffs(), EPSILON);
        }
    }
    
    @Test
    void testBatchTransformLarge() {
        // This should trigger parallel processing
        int batchSize = 20;
        double[][] signals = createTestSignals(batchSize, 128);
        Wavelet wavelet = Daubechies.DB4;
        
        TransformResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
        
        assertEquals(batchSize, results.length);
        
        // Verify all results are non-null and correct size
        for (TransformResult result : results) {
            assertNotNull(result);
            assertEquals(64, result.approximationCoeffs().length);
            assertEquals(64, result.detailCoeffs().length);
        }
    }
    
    @Test
    void testAsyncBatchTransform() throws Exception {
        int batchSize = 10;
        double[][] signals = createTestSignals(batchSize, 256);
        Wavelet wavelet = new Haar();
        
        CompletableFuture<TransformResult[]> future = 
            engine.transformBatchAsync(signals, wavelet, BoundaryMode.PERIODIC);
        
        TransformResult[] results = future.get(); // Wait for completion
        
        assertEquals(batchSize, results.length);
        
        // Verify results
        for (TransformResult result : results) {
            assertNotNull(result);
            assertEquals(128, result.approximationCoeffs().length);
        }
    }
    
    @Test
    void testCacheAwareTransform() {
        // Large signal should trigger cache-aware processing
        double[] signal = createTestSignal(16384);
        Wavelet wavelet = new Haar();
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(8192, result.approximationCoeffs().length);
        assertEquals(8192, result.detailCoeffs().length);
        
        // Verify correctness with spot checks
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult expected = standard.forward(signal);
        
        // Check first and last few values
        for (int i = 0; i < 10; i++) {
            assertEquals(expected.approximationCoeffs()[i], 
                        result.approximationCoeffs()[i], EPSILON);
        }
    }
    
    @Test
    void testSpecializedKernelSym4() {
        double[] signal = createTestSignal(256);
        Wavelet wavelet = Symlet.SYM4;
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
        
        // Verify against standard transform
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), 
                         result.approximationCoeffs(), EPSILON);
    }
    
    @Test
    void testZeroPaddingMode() {
        double[] signal = createTestSignal(128);
        Wavelet wavelet = new Haar();
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(64, result.approximationCoeffs().length);
        
        // Verify it falls back to standard transform for non-periodic
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.ZERO_PADDING);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), 
                         result.approximationCoeffs(), EPSILON);
    }
    
    @Test
    void testContinuousWavelet() {
        double[] signal = createTestSignal(256);
        Wavelet wavelet = new MorletWavelet();
        
        TransformResult result = engine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
        
        // Should fall back to standard transform for continuous wavelets
        WaveletTransform standard = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), 
                         result.approximationCoeffs(), EPSILON);
    }
    
    @Test
    void testEmptyBatch() {
        double[][] signals = new double[0][];
        
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertEquals(0, results.length);
    }
    
    @Test
    void testCustomConfiguration() {
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1)
            .withMemoryPool(false)
            .withSpecializedKernels(false)
            .withSoALayout(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine customEngine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(256);
        TransformResult result = customEngine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
    }
    
    @Test
    void testOptimizationInfo() {
        String info = engine.getOptimizationInfo();
        
        assertNotNull(info);
        assertTrue(info.contains("Optimization Engine Configuration"));
        assertTrue(info.contains("Memory Pooling:"));
        assertTrue(info.contains("Specialized Kernels:"));
        assertTrue(info.contains("SoA Layout:"));
        assertTrue(info.contains("Cache Blocking:"));
        assertTrue(info.contains("Parallel Processing:"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {64, 128, 256, 512, 1024, 2048})
    void testVariousSignalSizes(int size) {
        double[] signal = createTestSignal(size);
        
        TransformResult result = engine.transform(signal, Daubechies.DB4, BoundaryMode.PERIODIC);
        
        assertEquals(size / 2, result.approximationCoeffs().length);
        assertEquals(size / 2, result.detailCoeffs().length);
        
        // Verify non-trivial results
        assertTrue(hasSignificantValues(result.approximationCoeffs()));
        assertTrue(hasSignificantValues(result.detailCoeffs()));
    }
    
    @Test
    void testBatchWithDifferentWavelets() {
        int batchSize = 10;
        double[][] signals = createTestSignals(batchSize, 256);
        
        // Test different wavelets
        Wavelet[] wavelets = {new Haar(), Daubechies.DB4, Symlet.SYM4};
        
        for (Wavelet wavelet : wavelets) {
            TransformResult[] results = engine.transformBatch(signals, wavelet, BoundaryMode.PERIODIC);
            
            assertEquals(batchSize, results.length);
            
            // Verify all results
            for (TransformResult result : results) {
                assertNotNull(result);
                assertEquals(128, result.approximationCoeffs().length);
            }
        }
    }
    
    @Test
    void testSingleElementBatch() {
        double[][] signals = createTestSignals(1, 128);
        
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertEquals(1, results.length);
        assertEquals(64, results[0].approximationCoeffs().length);
    }
    
    @Test
    void testMixedSizeOptimization() {
        // Test that appropriate optimizations are selected for different sizes
        
        // Small signal - should use specialized kernel
        double[] small = createTestSignal(64);
        TransformResult smallResult = engine.transform(small, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(32, smallResult.approximationCoeffs().length);
        
        // Large signal - should use cache blocking
        double[] large = createTestSignal(16384);
        TransformResult largeResult = engine.transform(large, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(8192, largeResult.approximationCoeffs().length);
        
        // Medium batch - should use SoA
        double[][] mediumBatch = createTestSignals(8, 256);
        TransformResult[] batchResults = engine.transformBatch(mediumBatch, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(8, batchResults.length);
    }
    
    @Test
    void testEngineConfigBuilder() {
        // Test the fluent API for configuration
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(4)
            .withMemoryPool(true)
            .withSpecializedKernels(true)
            .withSoALayout(true)
            .withCacheBlocking(true);
        
        OptimizedTransformEngine configuredEngine = new OptimizedTransformEngine(config);
        
        // Test that it still works correctly
        double[] signal = createTestSignal(256);
        TransformResult result = configuredEngine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
    }
    
    @Test
    void testTransformPooledWithNonPeriodic() {
        // Test that pooled transform falls back correctly for non-periodic mode
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withMemoryPool(true)
            .withSpecializedKernels(false)
            .withSoALayout(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(128);
        TransformResult result = engine.transform(signal, new Haar(), BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(64, result.approximationCoeffs().length);
        
        // Verify against standard transform
        WaveletTransform standard = new WaveletTransform(new Haar(), BoundaryMode.ZERO_PADDING);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), result.approximationCoeffs(), EPSILON);
    }
    
    @Test
    void testBatchTransformPooledWithNonPeriodic() {
        // Test batch pooled transform with non-periodic boundary
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withMemoryPool(true)
            .withParallelism(1)
            .withSoALayout(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[][] signals = createTestSignals(5, 128);
        TransformResult[] results = engine.transformBatch(signals, Daubechies.DB4, BoundaryMode.ZERO_PADDING);
        
        assertEquals(5, results.length);
        
        // Verify all results are correct
        WaveletTransform standard = new WaveletTransform(Daubechies.DB4, BoundaryMode.ZERO_PADDING);
        for (int i = 0; i < 5; i++) {
            TransformResult expected = standard.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), 
                            results[i].approximationCoeffs(), EPSILON);
        }
    }
    
    @Test
    void testTransformBatchSoAWithNonPeriodic() {
        // Test that SoA batch transform falls back correctly for non-periodic mode
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withSoALayout(true)
            .withParallelism(1);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[][] signals = createTestSignals(8, 128);
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.ZERO_PADDING);
        
        assertEquals(8, results.length);
        
        // Should fall back to regular batch processing
        for (TransformResult result : results) {
            assertNotNull(result);
            assertEquals(64, result.approximationCoeffs().length);
        }
    }
    
    @Test
    void testTransformBatchSoAWithContinuousWavelet() {
        // Test that SoA batch transform falls back correctly for continuous wavelets
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withSoALayout(true)
            .withParallelism(1);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[][] signals = createTestSignals(8, 128);
        TransformResult[] results = engine.transformBatch(signals, new MorletWavelet(), BoundaryMode.PERIODIC);
        
        assertEquals(8, results.length);
        
        // Should fall back to regular batch processing
        for (TransformResult result : results) {
            assertNotNull(result);
            assertEquals(64, result.approximationCoeffs().length);
        }
    }
    
    @Test
    void testHasSpecializedKernel() {
        // This tests the private hasSpecializedKernel method indirectly
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withSpecializedKernels(true)
            .withMemoryPool(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        // Test with specialized kernels - should use them
        double[] signal = createTestSignal(256);
        
        // Test Haar (has specialized kernel)
        TransformResult haarResult = engine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        assertNotNull(haarResult);
        
        // Test DB4 (has specialized kernel)
        TransformResult db4Result = engine.transform(signal, Daubechies.DB4, BoundaryMode.PERIODIC);
        assertNotNull(db4Result);
        
        // Test Sym4 (has specialized kernel)
        TransformResult sym4Result = engine.transform(signal, Symlet.SYM4, BoundaryMode.PERIODIC);
        assertNotNull(sym4Result);
        
        // Test DB2 (no specialized kernel - should fall back)
        TransformResult db2Result = engine.transform(signal, Daubechies.DB2, BoundaryMode.PERIODIC);
        assertNotNull(db2Result);
    }
    
    @Test
    void testTransformCacheAwareWithContinuousWavelet() {
        // Test cache-aware transform falls back for continuous wavelets
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withCacheBlocking(true)
            .withSpecializedKernels(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(16384); // Large signal
        TransformResult result = engine.transform(signal, new MorletWavelet(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(8192, result.approximationCoeffs().length);
    }
    
    @Test
    void testTransformCacheAwareWithNonPeriodic() {
        // Test cache-aware transform falls back for non-periodic boundary
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withCacheBlocking(true)
            .withSpecializedKernels(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(16384); // Large signal
        TransformResult result = engine.transform(signal, new Haar(), BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(8192, result.approximationCoeffs().length);
    }
    
    @Test
    void testTransformPooledWithContinuousWavelet() {
        // Test pooled transform falls back for continuous wavelets
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withMemoryPool(true)
            .withSpecializedKernels(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(256);
        TransformResult result = engine.transform(signal, new MorletWavelet(), BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
    }
    
    @Test
    void testSpecializedKernelWithNonPeriodicBoundary() {
        // Test specialized kernel falls back for non-periodic boundary
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withSpecializedKernels(true)
            .withMemoryPool(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[] signal = createTestSignal(256);
        
        // DB4 has specialized kernel but should fall back for non-periodic
        TransformResult result = engine.transform(signal, Daubechies.DB4, BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(128, result.approximationCoeffs().length);
        
        // Verify correctness
        WaveletTransform standard = new WaveletTransform(Daubechies.DB4, BoundaryMode.ZERO_PADDING);
        TransformResult expected = standard.forward(signal);
        
        assertArrayEquals(expected.approximationCoeffs(), result.approximationCoeffs(), EPSILON);
    }
    
    @Test
    void testGetOptimizationInfoWithAllDisabled() {
        // Test optimization info when all features are disabled
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1)
            .withMemoryPool(false)
            .withSpecializedKernels(false)
            .withSoALayout(false)
            .withCacheBlocking(false);
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        String info = engine.getOptimizationInfo();
        
        assertNotNull(info);
        assertTrue(info.contains("Memory Pooling: Disabled"));
        assertTrue(info.contains("Specialized Kernels: Disabled"));
        assertTrue(info.contains("SoA Layout: Disabled"));
        assertTrue(info.contains("Cache Blocking: Disabled"));
        assertTrue(info.contains("Parallel Processing: Disabled"));
        
        // Should not contain pool statistics when disabled
        assertFalse(info.contains("Pool Statistics"));
    }
    
    @Test
    void testBatchTransformWithSingleSignal() {
        // Edge case: batch with single signal
        OptimizedTransformEngine engine = new OptimizedTransformEngine();
        
        double[][] signals = createTestSignals(1, 128);
        TransformResult[] results = engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertEquals(1, results.length);
        assertNotNull(results[0]);
    }
    
    @Test
    void testAsyncBatchTransformWithNullParallelEngine() {
        // Test async when parallel engine is disabled
        OptimizedTransformEngine.EngineConfig config = new OptimizedTransformEngine.EngineConfig()
            .withParallelism(1); // This should result in null parallelEngine
        
        OptimizedTransformEngine engine = new OptimizedTransformEngine(config);
        
        double[][] signals = createTestSignals(5, 128);
        CompletableFuture<TransformResult[]> future = 
            engine.transformBatchAsync(signals, new Haar(), BoundaryMode.PERIODIC);
        
        assertNotNull(future);
        
        // Should still complete successfully
        TransformResult[] results = future.join();
        assertEquals(5, results.length);
    }
    
    // Helper methods
    
    private double[] createTestSignal(int length) {
        Random random = new Random(12345);
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 16.0) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private double[][] createTestSignals(int count, int length) {
        double[][] signals = new double[count][length];
        Random random = new Random(54321);
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < length; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 16.0 + i * Math.PI / 8) +
                               0.1 * random.nextGaussian();
            }
        }
        
        return signals;
    }
    
    private double computeEnergy(double[] signal) {
        double energy = 0.0;
        for (double val : signal) {
            energy += val * val;
        }
        return energy;
    }
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}