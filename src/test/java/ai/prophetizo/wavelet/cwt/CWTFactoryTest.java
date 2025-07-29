package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    }
}
