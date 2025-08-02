package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.WaveletOpsFactory;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.CWTFactory;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the common Factory interface implementations.
 */
class FactoryInterfaceTest {
    
    @Test
    void testWaveletOpsFactoryInstance() {
        Factory<WaveletOpsFactory.WaveletOps, TransformConfig> factory = 
            WaveletOpsFactory.getInstance();
        
        assertNotNull(factory);
        
        // Test create with default config
        WaveletOpsFactory.WaveletOps ops1 = factory.create();
        assertNotNull(ops1);
        
        // Test create with custom config
        TransformConfig config = TransformConfig.builder()
            .forceScalar(true)
            .build();
        WaveletOpsFactory.WaveletOps ops2 = factory.create(config);
        assertNotNull(ops2);
        assertEquals("Scalar", ops2.getImplementationType());
        
        // Test validation
        assertTrue(factory.isValidConfiguration(config));
        
        // Test description
        assertNotNull(factory.getDescription());
    }
    
    @Test
    void testWaveletTransformFactory() {
        WaveletTransformFactory factory = new WaveletTransformFactory();
        
        // Test that it implements Factory interface
        assertTrue(factory instanceof Factory);
        
        // Test create with default wavelet
        var transform1 = factory.create();
        assertNotNull(transform1);
        
        // Test create with specific wavelet
        var wavelet = new Haar();
        var transform2 = factory.create(wavelet);
        assertNotNull(transform2);
        
        // Test validation
        assertTrue(factory.isValidConfiguration(wavelet));
        assertFalse(factory.isValidConfiguration(null));
        
        // Test description
        assertNotNull(factory.getDescription());
        assertTrue(factory.getDescription().contains("PERIODIC"));
    }
    
    @Test
    void testCWTFactoryInstance() {
        var factory = CWTFactory.getInstance();
        
        assertNotNull(factory);
        
        // Test that create() without wavelet throws exception
        assertThrows(UnsupportedOperationException.class, () -> factory.create());
        
        // Test create with wavelet
        var wavelet = new MorletWavelet();
        var transform = factory.create(wavelet);
        assertNotNull(transform);
        
        // Test validation
        assertTrue(factory.isValidConfiguration(wavelet));
        assertFalse(factory.isValidConfiguration(null));
        
        // Test description
        assertNotNull(factory.getDescription());
    }
    
    @Test
    void testStreamingDenoiserFactoryInstance() {
        var factory = StreamingDenoiserFactory.getInstance();
        
        assertNotNull(factory);
        
        // Test that create() without config throws exception
        assertThrows(UnsupportedOperationException.class, () -> factory.create());
        
        // Test create with config
        var config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(128)
            .build();
        var denoiser = factory.create(config);
        assertNotNull(denoiser);
        
        // Test validation
        assertTrue(factory.isValidConfiguration(config));
        assertFalse(factory.isValidConfiguration(null));
        
        // Test description
        assertNotNull(factory.getDescription());
        
        // Clean up
        try {
            denoiser.close();
        } catch (Exception e) {
            // Ignore
        }
    }
    
    @Test
    void testFactoryRegistry() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Clear to start fresh
        registry.clear();
        
        // Test registration
        var factory = new WaveletTransformFactory();
        registry.register("testFactory", factory);
        
        assertTrue(registry.isRegistered("testFactory"));
        
        // Test retrieval
        var retrieved = registry.getFactory("testFactory");
        assertTrue(retrieved.isPresent());
        assertEquals(factory, retrieved.get());
        
        // Test typed retrieval
        var typedFactory = registry.getFactory("testFactory", 
            ai.prophetizo.wavelet.WaveletTransform.class, Wavelet.class);
        assertTrue(typedFactory.isPresent());
        
        // Test unregistration
        assertTrue(registry.unregister("testFactory"));
        assertFalse(registry.isRegistered("testFactory"));
        
        // Test default registration
        FactoryRegistry.registerDefaults();
        assertTrue(registry.isRegistered("waveletOps"));
        assertTrue(registry.isRegistered("waveletTransform"));
        assertTrue(registry.isRegistered("cwtTransform"));
        assertTrue(registry.isRegistered("streamingDenoiser"));
    }
}