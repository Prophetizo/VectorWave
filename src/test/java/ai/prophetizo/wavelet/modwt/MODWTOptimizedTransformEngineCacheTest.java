package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.BiorthogonalSpline;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Symlet;
import ai.prophetizo.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LRU cache implementation in MODWTOptimizedTransformEngine.
 */
class MODWTOptimizedTransformEngineCacheTest {
    
    private MODWTOptimizedTransformEngine engine;
    
    @BeforeEach
    void setUp() {
        // Create engine with minimal features to ensure we hit the cache path
        MODWTOptimizedTransformEngine.EngineConfig config = new MODWTOptimizedTransformEngine.EngineConfig()
            .withParallelism(1)
            .withMemoryPool(false)
            .withSoALayout(false)
            .withCacheBlocking(false);
        engine = new MODWTOptimizedTransformEngine(config);
    }
    
    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }
    
    @Test
    void testCacheStartsEmpty() {
        assertEquals(0, engine.getCacheSize());
    }
    
    @Test
    void testCacheGrowsWithUse() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Use different wavelets to populate cache - all with PERIODIC mode
        // to ensure we hit the default cache path
        Wavelet haar = new Haar();
        engine.transform(signal, haar, BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
        
        engine.transform(signal, Daubechies.DB4, BoundaryMode.PERIODIC);
        assertEquals(2, engine.getCacheSize());
        
        engine.transform(signal, Daubechies.DB2, BoundaryMode.PERIODIC);
        assertEquals(3, engine.getCacheSize());
    }
    
    @Test
    void testCacheReusesExistingTransforms() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Use the same Haar instance to ensure caching
        Wavelet haar = new Haar();
        
        // First use creates cached instance
        engine.transform(signal, haar, BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
        
        // Second use should reuse cached instance
        engine.transform(signal, haar, BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
        
        // Different signal but same wavelet/mode should reuse
        double[] signal2 = {8, 7, 6, 5, 4, 3, 2, 1};
        engine.transform(signal2, haar, BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
    }
    
    @Test
    void testCacheHandlesDifferentWavelets() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Each different wavelet should create a new cache entry
        engine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        engine.transform(signal, Daubechies.DB2, BoundaryMode.PERIODIC);
        engine.transform(signal, Daubechies.DB4, BoundaryMode.PERIODIC);
        engine.transform(signal, Symlet.SYM2, BoundaryMode.PERIODIC);
        
        assertEquals(4, engine.getCacheSize());
    }
    
    @Test
    void testClearCache() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Populate cache
        engine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        engine.transform(signal, Daubechies.DB4, BoundaryMode.PERIODIC);
        assertTrue(engine.getCacheSize() > 0);
        
        // Clear cache
        engine.clearCache();
        assertEquals(0, engine.getCacheSize());
        
        // Cache should work normally after clearing
        engine.transform(signal, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
    }
    
    @Test
    void testCacheWithBatchOperations() {
        double[][] signals = {
            {1, 2, 3, 4, 5, 6, 7, 8},
            {8, 7, 6, 5, 4, 3, 2, 1},
            {1, 1, 1, 1, 1, 1, 1, 1}
        };
        
        // Batch operations should also use cache
        engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
        
        // Same wavelet/mode in another batch should reuse
        engine.transformBatch(signals, new Haar(), BoundaryMode.PERIODIC);
        assertEquals(1, engine.getCacheSize());
    }
    
    @Test
    void testCacheSizeLimit() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        int maxSize = MODWTOptimizedTransformEngine.getMaxCacheSize();
        
        // The cache should never exceed the maximum size
        // Add many different wavelets
        Wavelet[] wavelets = {
            new Haar(), Daubechies.DB2, Daubechies.DB4,
            Symlet.SYM2, Symlet.SYM3, Symlet.SYM4,
            BiorthogonalSpline.BIOR1_3
        };
        
        // Add entries up to and beyond the max size
        for (int i = 0; i < maxSize + 10; i++) {
            // Use modulo to cycle through available wavelets
            Wavelet w = wavelets[i % wavelets.length];
            // Use PERIODIC mode to ensure we hit the cache path
            engine.transform(signal, w, BoundaryMode.PERIODIC);
            
            // Cache size should never exceed max
            assertTrue(engine.getCacheSize() <= maxSize);
        }
        
        // Cache should be at or below max size
        assertTrue(engine.getCacheSize() > 0);
        assertTrue(engine.getCacheSize() <= maxSize);
    }
}