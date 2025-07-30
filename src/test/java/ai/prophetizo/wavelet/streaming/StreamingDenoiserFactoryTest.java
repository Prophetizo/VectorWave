package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Symlet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StreamingDenoiserFactory.
 */
@DisplayName("StreamingDenoiserFactory")
class StreamingDenoiserFactoryTest {
    
    @Test
    @DisplayName("Create with FAST implementation")
    void testCreateFastImplementation() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.FAST, config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof FastStreamingDenoiser);
            assertEquals(256, denoiser.getBlockSize());
        }
    }
    
    @Test
    @DisplayName("Create with QUALITY implementation")
    void testCreateQualityImplementation() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(512)
            .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.QUALITY, config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof QualityStreamingDenoiser);
            assertEquals(512, denoiser.getBlockSize());
        }
    }
    
    @Test
    @DisplayName("Create with AUTO implementation - selects FAST for small blocks")
    void testCreateAutoImplementationSmallBlock() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(128) // Small block should select FAST
            .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.AUTO, config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof FastStreamingDenoiser);
        }
    }
    
    @Test
    @DisplayName("Create with AUTO implementation - selects FAST for overlap + adaptive")
    void testCreateAutoImplementationOverlapAdaptive() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(512)
            .overlapFactor(0.5)
            .adaptiveThreshold(true)
            .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.AUTO, config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof FastStreamingDenoiser);
        }
    }
    
    @Test
    @DisplayName("Create with AUTO implementation - selects QUALITY for large blocks no overlap")
    void testCreateAutoImplementationLargeBlockNoOverlap() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(1024)
            .overlapFactor(0.0)
            .adaptiveThreshold(false)
            .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.AUTO, config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof QualityStreamingDenoiser);
        }
    }
    
    @Test
    @DisplayName("Create with default AUTO implementation")
    void testCreateDefaultAuto() throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(512)
            .build();
        
        // Should use AUTO by default
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(config)) {
            
            assertNotNull(denoiser);
            assertTrue(denoiser instanceof FastStreamingDenoiser || 
                      denoiser instanceof QualityStreamingDenoiser);
        }
    }
    
    @Test
    @DisplayName("Null validation")
    void testNullValidation() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .build();
        
        assertThrows(InvalidArgumentException.class, 
            () -> StreamingDenoiserFactory.create(null, config));
        
        assertThrows(InvalidArgumentException.class, 
            () -> StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.FAST, null));
    }
    
    @Test
    @DisplayName("Get expected performance for FAST")
    void testGetExpectedPerformanceFast() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .build();
        
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserFactory.getExpectedPerformance(
                StreamingDenoiserFactory.Implementation.FAST, config);
        
        assertNotNull(profile);
        assertEquals(0.70, profile.expectedLatencyMicros());
        assertEquals(-7.0, profile.expectedSNRImprovement());
        assertEquals(22 * 1024, profile.memoryUsageBytes());
        assertTrue(profile.isRealTimeCapable());
    }
    
    @Test
    @DisplayName("Get expected performance for QUALITY")
    void testGetExpectedPerformanceQuality() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .build();
        
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserFactory.getExpectedPerformance(
                StreamingDenoiserFactory.Implementation.QUALITY, config);
        
        assertNotNull(profile);
        // Quality implementation should have different characteristics
        assertTrue(profile.expectedLatencyMicros() >= 0.2);
        assertTrue(profile.expectedSNRImprovement() > -7.0);
        assertTrue(profile.memoryUsageBytes() > 0);
    }
    
    @Test
    @DisplayName("Get expected performance for AUTO")
    void testGetExpectedPerformanceAuto() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(128) // Small block should select FAST
            .build();
        
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserFactory.getExpectedPerformance(
                StreamingDenoiserFactory.Implementation.AUTO, config);
        
        assertNotNull(profile);
        // Should match FAST implementation profile
        assertEquals(0.70, profile.expectedLatencyMicros());
        assertEquals(-7.0, profile.expectedSNRImprovement());
    }
    
    @Test
    @DisplayName("Factory instantiation prevention")
    void testFactoryInstantiationPrevention() {
        // Use reflection to try to instantiate the factory
        try {
            var constructor = StreamingDenoiserFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            // InvocationTargetException wraps the actual exception
            assertThrows(InvocationTargetException.class, constructor::newInstance);
        } catch (NoSuchMethodException e) {
            // If we can't access the constructor, that's also fine
            assertTrue(true);
        }
    }
    
    @Test
    @DisplayName("Complex configuration selection")
    void testComplexConfigurationSelection() throws Exception {
        // Test various complex configurations
        
        // Config 1: Multi-level with overlap
        StreamingDenoiserConfig config1 = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(1024)
            .overlapFactor(0.75)
            .levels(4)
            .thresholdMethod(ThresholdMethod.SURE)
            .build();
        
        try (StreamingDenoiserStrategy denoiser1 = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.AUTO, config1)) {
            assertTrue(denoiser1 instanceof FastStreamingDenoiser); // overlap > 0
        }
        
        // Config 2: No overlap, large block
        StreamingDenoiserConfig config2 = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(2048)
            .overlapFactor(0.0)
            .adaptiveThreshold(false)
            .build();
        
        try (StreamingDenoiserStrategy denoiser2 = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.AUTO, config2)) {
            assertTrue(denoiser2 instanceof QualityStreamingDenoiser);
        }
    }
}