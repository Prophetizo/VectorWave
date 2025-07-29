package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CWTFactory class.
 * 
 * Validates the factory's ability to create and configure FFTAcceleratedCWT instances
 * with appropriate settings and error handling.
 */
class CWTFactoryTest {
    
    private CWTFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new CWTFactory();
    }
    
    @Test
    @DisplayName("Factory should create FFTAcceleratedCWT instances")
    void shouldCreateFFTAcceleratedCWTInstances() {
        FFTAcceleratedCWT cwt = factory.create();
        
        assertNotNull(cwt);
        assertInstanceOf(FFTAcceleratedCWT.class, cwt);
    }
    
    @Test
    @DisplayName("Factory should support fluent configuration")
    void shouldSupportFluentConfiguration() {
        CWTFactory configured = factory
            .withOptimizations(false)
            .withOptimizations(true);
        
        assertSame(factory, configured);
        assertTrue(factory.isOptimizationsEnabled());
    }
    
    @Test
    @DisplayName("Factory should have default optimization settings")
    void shouldHaveDefaultOptimizationSettings() {
        assertTrue(factory.isOptimizationsEnabled());
    }
    
    @Test
    @DisplayName("Factory should allow disabling optimizations")
    void shouldAllowDisablingOptimizations() {
        factory.withOptimizations(false);
        assertFalse(factory.isOptimizationsEnabled());
    }
    
    @Test
    @DisplayName("Factory should provide meaningful description")
    void shouldProvideMeaningfulDescription() {
        String description = factory.getDescription();
        
        assertNotNull(description);
        assertFalse(description.trim().isEmpty());
        assertTrue(description.contains("FFT"));
        assertTrue(description.contains("Continuous Wavelet Transform"));
    }
    
    @Test
    @DisplayName("Factory should return correct product type")
    void shouldReturnCorrectProductType() {
        Class<FFTAcceleratedCWT> productType = factory.getProductType();
        
        assertNotNull(productType);
        assertEquals(FFTAcceleratedCWT.class, productType);
    }
    
    @Test
    @DisplayName("Static createDefault should work correctly")
    void staticCreateDefaultShouldWorkCorrectly() {
        FFTAcceleratedCWT cwt = CWTFactory.createDefault();
        
        assertNotNull(cwt);
        assertInstanceOf(FFTAcceleratedCWT.class, cwt);
    }
    
    @Test
    @DisplayName("Factory should create new instances each time")
    void shouldCreateNewInstancesEachTime() {
        FFTAcceleratedCWT cwt1 = factory.create();
        FFTAcceleratedCWT cwt2 = factory.create();
        
        assertNotNull(cwt1);
        assertNotNull(cwt2);
        assertNotSame(cwt1, cwt2);
    }
    
    @Test
    @DisplayName("Configuration should not affect previously created instances")
    void configurationShouldNotAffectPreviouslyCreatedInstances() {
        factory.withOptimizations(true);
        FFTAcceleratedCWT cwt1 = factory.create();
        
        factory.withOptimizations(false);
        FFTAcceleratedCWT cwt2 = factory.create();
        
        // Both instances should be created successfully regardless of configuration
        assertNotNull(cwt1);
        assertNotNull(cwt2);
        assertNotSame(cwt1, cwt2);
=======
class CWTFactoryTest {
    
    @Test
    @DisplayName("Should create transform with default config")
    void testCreateWithDefaultConfig() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.create(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertNotNull(transform.getConfig());
        assertTrue(transform.getConfig().isFFTEnabled());
        assertTrue(transform.getConfig().isNormalizeAcrossScales());
    }
    
    @Test
    @DisplayName("Should create transform with custom config")
    void testCreateWithCustomConfig() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        CWTConfig config = CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(false)
            .build();
        
        // When
        CWTTransform transform = CWTFactory.create(wavelet, config);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertEquals(config, transform.getConfig());
        assertFalse(transform.getConfig().isFFTEnabled());
        assertFalse(transform.getConfig().isNormalizeAcrossScales());
    }
    
    @Test
    @DisplayName("Should create transform for real-time processing")
    void testCreateForRealTime() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createForRealTime(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertFalse(transform.getConfig().isFFTEnabled()); // Direct convolution for low latency
        assertTrue(transform.getConfig().isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create transform for batch processing")
    void testCreateForBatchProcessing() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createForBatchProcessing(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertTrue(transform.getConfig().isFFTEnabled()); // FFT for efficiency
        assertTrue(transform.getConfig().isUseStructuredConcurrency());
        assertTrue(transform.getConfig().isUseScopedValues());
    }
    
    @Test
    @DisplayName("Should create transform optimized for Java 23")
    void testCreateOptimizedForJava23() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createOptimizedForJava23(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertTrue(transform.getConfig().isFFTEnabled());
        assertTrue(transform.getConfig().isUseScopedValues());
        assertTrue(transform.getConfig().isUseStructuredConcurrency());
        assertTrue(transform.getConfig().isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create builder")
    void testCreateBuilder() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTFactory.Builder builder = CWTFactory.builder()
            .wavelet(wavelet)
            .enableFFT(false)
            .normalizeScales(false)
            .fftSize(1024);
        
        CWTTransform transform = builder.build();
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertFalse(transform.getConfig().isFFTEnabled());
        assertFalse(transform.getConfig().isNormalizeAcrossScales());
        assertEquals(1024, transform.getConfig().getFFTSize());
    }
    
    @Test
    @DisplayName("Should validate null wavelet")
    void testValidateNullWavelet() {
        // Then
        assertThrows(IllegalArgumentException.class, 
            () -> CWTFactory.create(null));
        
        assertThrows(IllegalArgumentException.class, 
            () -> CWTFactory.create(null, CWTConfig.defaultConfig()));
        
        assertThrows(IllegalStateException.class,
            () -> CWTFactory.builder().build());
    }
    
    @Test
    @DisplayName("Should create multiple independent transforms")
    void testCreateMultipleIndependentTransforms() {
        // Given
        MorletWavelet wavelet1 = new MorletWavelet(5.0, 1.0);
        MorletWavelet wavelet2 = new MorletWavelet(6.0, 1.5);
        
        // When
        CWTTransform transform1 = CWTFactory.create(wavelet1);
        CWTTransform transform2 = CWTFactory.create(wavelet2);
        
        // Then
        assertNotNull(transform1);
        assertNotNull(transform2);
        assertNotSame(transform1, transform2);
        assertNotSame(transform1.getConfig(), transform2.getConfig());
        assertEquals(wavelet1, transform1.getWavelet());
        assertEquals(wavelet2, transform2.getWavelet());
    }
    
    @Test
    @DisplayName("Should build transform with all options")
    void testBuilderWithAllOptions() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.builder()
            .wavelet(wavelet)
            .boundaryMode(ai.prophetizo.wavelet.api.BoundaryMode.ZERO_PADDING)
            .enableFFT(true)
            .normalizeScales(true)
            .paddingStrategy(CWTConfig.PaddingStrategy.SYMMETRIC)
            .fftSize(2048)
            .useScopedValues(true)
            .useStructuredConcurrency(false)
            .useStreamGatherers(true)
            .build();
        
        // Then
        CWTConfig config = transform.getConfig();
        assertEquals(ai.prophetizo.wavelet.api.BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertTrue(config.isFFTEnabled());
        assertTrue(config.isNormalizeAcrossScales());
        assertEquals(CWTConfig.PaddingStrategy.SYMMETRIC, config.getPaddingStrategy());
        assertEquals(2048, config.getFFTSize());
        assertTrue(config.isUseScopedValues());
        assertFalse(config.isUseStructuredConcurrency());
        assertTrue(config.isUseStreamGatherers());
    }
}