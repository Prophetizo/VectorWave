package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaveletTransformFactory edge cases and configuration scenarios.
 */
@DisplayName("WaveletTransformFactory Tests")
class WaveletTransformFactoryTest {
    
    private WaveletTransformFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new WaveletTransformFactory();
    }
    
    // === Null Parameter Tests ===
    
    @Test
    @DisplayName("Create with null wavelet should throw exception")
    void testCreateNullWavelet() {
        assertThrows(InvalidArgumentException.class,
            () -> factory.create(null));
    }
    
    @Test
    @DisplayName("WithBoundaryMode with null should throw exception")
    void testWithBoundaryModeNull() {
        assertThrows(InvalidArgumentException.class,
            () -> factory.boundaryMode(null));
    }
    
    // === Factory Method Tests ===
    
    @Test
    @DisplayName("Create with just wavelet should use default config")
    void testCreateWithJustWavelet() {
        WaveletTransform transform = factory.create(new Haar());
        
        assertNotNull(transform);
        // Should work with default config (PERIODIC boundary mode)
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        TransformResult result = transform.forward(signal);
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Create with wavelet after setting boundary mode")
    void testCreateWithBoundaryMode() {
        WaveletTransform transform = factory
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .create(new Haar());
        
        assertNotNull(transform);
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        TransformResult result = transform.forward(signal);
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Create with default settings")
    void testCreateWithDefaultSettings() {
        // Test with default settings (automatically selects best implementation)
        WaveletTransform transform = factory.create(new Haar());
        
        assertNotNull(transform);
        verifyTransformWorks(transform);
        
        // Test with new factory instance
        WaveletTransform transform2 = new WaveletTransformFactory()
            .create(new Haar());
        
        assertNotNull(transform2);
        verifyTransformWorks(transform2);
    }
    
    @Test
    @DisplayName("Static createDefault method")
    void testStaticCreateDefault() {
        WaveletTransform transform = WaveletTransformFactory.createDefault(new Haar());
        
        assertNotNull(transform);
        verifyTransformWorks(transform);
    }
    
    // === All Wavelet Types ===
    
    @Test
    @DisplayName("Factory should support all wavelet types")
    void testAllWaveletTypes() {
        // Test Haar
        assertNotNull(factory.create(new Haar()));
        
        // Test Daubechies wavelets
        assertNotNull(factory.create(Daubechies.DB2));
        assertNotNull(factory.create(Daubechies.DB4));
    }
    
    // === All Boundary Modes ===
    
    @Test
    @DisplayName("Factory should support PERIODIC boundary mode")
    void testPeriodicBoundaryMode() {
        WaveletTransform transform = factory
            .boundaryMode(BoundaryMode.PERIODIC)
            .create(new Haar());
        
        assertNotNull(transform);
        verifyTransformWorks(transform);
    }
    
    @Test
    @DisplayName("Factory should support ZERO_PADDING boundary mode")
    void testZeroPaddingBoundaryMode() {
        WaveletTransform transform = factory
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .create(new Haar());
        
        assertNotNull(transform);
        verifyTransformWorks(transform);
    }
    
    @Test
    @DisplayName("Factory should reject SYMMETRIC boundary mode")
    void testSymmetricBoundaryMode() {
        assertThrows(InvalidConfigurationException.class,
            () -> factory.boundaryMode(BoundaryMode.SYMMETRIC).create(new Haar()));
    }
    
    @Test
    @DisplayName("Factory should reject CONSTANT boundary mode")
    void testConstantBoundaryMode() {
        assertThrows(InvalidConfigurationException.class,
            () -> factory.boundaryMode(BoundaryMode.CONSTANT).create(new Haar()));
    }
    
    // === Fluent API Tests ===
    
    @Test
    @DisplayName("Factory should support method chaining")
    void testMethodChaining() {
        WaveletTransform transform = factory
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .create(new Haar());
        
        assertNotNull(transform);
        verifyTransformWorks(transform);
    }
    
    @Test
    @DisplayName("Factory settings should persist across multiple creates")
    void testFactorySettingsPersistence() {
        factory.boundaryMode(BoundaryMode.ZERO_PADDING);
        
        // Create multiple transforms with same settings
        WaveletTransform transform1 = factory.create(new Haar());
        WaveletTransform transform2 = factory.create(Daubechies.DB2);
        
        assertNotNull(transform1);
        assertNotNull(transform2);
        verifyTransformWorks(transform1);
        verifyTransformWorks(transform2);
    }
    
    // === Complex Configurations ===
    
    @Test
    @DisplayName("Factory should handle all combinations of parameters")
    void testAllCombinations() {
        Wavelet[] wavelets = {new Haar(), Daubechies.DB2, Daubechies.DB4};
        BoundaryMode[] supportedModes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING};
        
        for (Wavelet wavelet : wavelets) {
            for (BoundaryMode mode : supportedModes) {
                WaveletTransformFactory testFactory = new WaveletTransformFactory()
                    .boundaryMode(mode);
                
                WaveletTransform transform = testFactory.create(wavelet);
                assertNotNull(transform, 
                    String.format("Failed to create transform for %s, %s", 
                        wavelet.name(), mode));
                verifyTransformWorks(transform);
            }
        }
    }
    
    @Test
    @DisplayName("Factory should reset properly for new instances")
    void testFactoryReset() {
        // First factory with specific settings
        WaveletTransformFactory factory1 = new WaveletTransformFactory()
            .boundaryMode(BoundaryMode.ZERO_PADDING);
        
        // Second factory should have default settings
        WaveletTransformFactory factory2 = new WaveletTransformFactory();
        
        // Both should work independently
        WaveletTransform transform1 = factory1.create(new Haar());
        WaveletTransform transform2 = factory2.create(new Haar());
        
        assertNotNull(transform1);
        assertNotNull(transform2);
        verifyTransformWorks(transform1);
        verifyTransformWorks(transform2);
    }
    
    // === Helper Methods ===
    
    private void verifyTransformWorks(WaveletTransform transform) {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        TransformResult result = transform.forward(signal);
        assertNotNull(result);
        assertNotNull(result.approximationCoeffs());
        assertNotNull(result.detailCoeffs());
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
        
        // Verify inverse works
        double[] reconstructed = transform.inverse(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
}