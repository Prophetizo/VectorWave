package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QualityStreamingDenoiser.Builder.
 */
@DisplayName("QualityStreamingDenoiser.Builder")
class QualityStreamingDenoiserBuilderTest {
    
    @Test
    @DisplayName("Default builder values")
    void testDefaultBuilderValues() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder();
        builder.wavelet(new Haar()); // Required
        
        QualityStreamingDenoiser denoiser = builder.build();
        
        assertNotNull(denoiser);
        assertEquals(512, denoiser.getBlockSize()); // Default block size
        assertTrue(denoiser.isReady());
        
        denoiser.close();
    }
    
    @Test
    @DisplayName("Custom builder values")
    void testCustomBuilderValues() {
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(1024)
            .overlapFactor(0.5)
            .levels(3)
            .thresholdMethod(ThresholdMethod.SURE)
            .thresholdType(ThresholdType.HARD)
            .adaptiveThreshold(false)
            .attackTime(5.0)
            .releaseTime(25.0)
            .useSharedMemoryPool(false)
            .noiseBufferFactor(8)
            .build();
        
        assertNotNull(denoiser);
        assertEquals(1024, denoiser.getBlockSize());
        
        denoiser.close();
    }
    
    @Test
    @DisplayName("Builder validation - null wavelet")
    void testBuilderValidationNullWavelet() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder();
        
        assertThrows(InvalidArgumentException.class, builder::build);
    }
    
    @Test
    @DisplayName("Builder validation - invalid overlap factor")
    void testBuilderValidationInvalidOverlapFactor() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder()
            .wavelet(new Haar());
        
        assertThrows(IllegalArgumentException.class, 
            () -> builder.overlapFactor(-0.1));
        assertThrows(IllegalArgumentException.class, 
            () -> builder.overlapFactor(1.0));
        assertThrows(IllegalArgumentException.class, 
            () -> builder.overlapFactor(1.5));
        
        // Valid cases
        assertDoesNotThrow(() -> builder.overlapFactor(0.0));
        assertDoesNotThrow(() -> builder.overlapFactor(0.5));
        assertDoesNotThrow(() -> builder.overlapFactor(0.99));
    }
    
    @Test
    @DisplayName("Builder validation - invalid noise buffer factor")
    void testBuilderValidationInvalidNoiseBufferFactor() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder()
            .wavelet(new Haar());
        
        assertThrows(IllegalArgumentException.class, 
            () -> builder.noiseBufferFactor(0));
        assertThrows(IllegalArgumentException.class, 
            () -> builder.noiseBufferFactor(-1));
        
        // Valid cases
        assertDoesNotThrow(() -> builder.noiseBufferFactor(1));
        assertDoesNotThrow(() -> builder.noiseBufferFactor(10));
    }
    
    @Test
    @DisplayName("Builder method chaining")
    void testBuilderMethodChaining() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder();
        
        // Verify all methods return the builder for chaining
        assertSame(builder, builder.wavelet(new Haar()));
        assertSame(builder, builder.blockSize(256));
        assertSame(builder, builder.overlapFactor(0.5));
        assertSame(builder, builder.levels(2));
        assertSame(builder, builder.thresholdMethod(ThresholdMethod.MINIMAX));
        assertSame(builder, builder.thresholdType(ThresholdType.SOFT));
        assertSame(builder, builder.adaptiveThreshold(true));
        assertSame(builder, builder.attackTime(10.0));
        assertSame(builder, builder.releaseTime(50.0));
        assertSame(builder, builder.useSharedMemoryPool(true));
        assertSame(builder, builder.noiseBufferFactor(4));
    }
    
    @Test
    @DisplayName("Multiple builds from same builder")
    void testMultipleBuilds() {
        QualityStreamingDenoiser.Builder builder = new QualityStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .blockSize(256);
        
        QualityStreamingDenoiser denoiser1 = builder.build();
        QualityStreamingDenoiser denoiser2 = builder.build();
        
        assertNotNull(denoiser1);
        assertNotNull(denoiser2);
        assertNotSame(denoiser1, denoiser2); // Should create new instances
        
        assertEquals(256, denoiser1.getBlockSize());
        assertEquals(256, denoiser2.getBlockSize());
        
        denoiser1.close();
        denoiser2.close();
    }
    
    @Test
    @DisplayName("Builder with all threshold methods")
    void testAllThresholdMethods() {
        for (ThresholdMethod method : ThresholdMethod.values()) {
            QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .thresholdMethod(method)
                .build();
            
            assertNotNull(denoiser);
            denoiser.close();
        }
    }
    
    @Test
    @DisplayName("Builder with all threshold types")
    void testAllThresholdTypes() {
        for (ThresholdType type : ThresholdType.values()) {
            QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .thresholdType(type)
                .build();
            
            assertNotNull(denoiser);
            denoiser.close();
        }
    }
    
    
    @Test
    @DisplayName("Builder with extreme values")
    void testBuilderWithExtremeValues() {
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .blockSize(16) // Minimum valid size
            .overlapFactor(0.95) // High overlap
            .levels(4) // Many levels
            .attackTime(0.1) // Fast attack
            .releaseTime(100.0) // Slow release
            .noiseBufferFactor(100) // Large buffer
            .build();
        
        assertNotNull(denoiser);
        assertEquals(16, denoiser.getBlockSize());
        
        denoiser.close();
    }
}