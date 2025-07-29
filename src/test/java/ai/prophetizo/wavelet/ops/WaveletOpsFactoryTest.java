package ai.prophetizo.wavelet.ops;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.ops.WaveletOpsFactory.OptimizationLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the WaveletOpsFactory class.
 * 
 * Validates the factory's ability to create and configure WaveletOpsConfig instances
 * with appropriate settings, validation, and error handling.
 */
class WaveletOpsFactoryTest {
    
    private WaveletOpsFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new WaveletOpsFactory();
    }
    
    @Test
    @DisplayName("Factory should create WaveletOpsConfig instances")
    void shouldCreateWaveletOpsConfigInstances() {
        WaveletOpsConfig config = factory.create();
        
        assertNotNull(config);
        assertInstanceOf(WaveletOpsConfig.class, config);
    }
    
    @Test
    @DisplayName("Factory should have sensible defaults")
    void shouldHaveSensibleDefaults() {
        WaveletOpsConfig config = factory.create();
        
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertEquals(OptimizationLevel.STANDARD, config.getOptimizationLevel());
        assertTrue(config.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Factory should support fluent configuration")
    void shouldSupportFluentConfiguration() {
        WaveletOpsFactory configured = factory
            .withBoundaryMode(BoundaryMode.ZERO_PADDING)
            .withOptimizationLevel(OptimizationLevel.AGGRESSIVE)
            .withVectorization(false);
        
        assertSame(factory, configured);
        
        WaveletOpsConfig config = factory.create();
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertEquals(OptimizationLevel.AGGRESSIVE, config.getOptimizationLevel());
        assertFalse(config.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Factory should validate boundary mode parameter")
    void shouldValidateBoundaryModeParameter() {
        assertThrows(NullPointerException.class, 
            () -> factory.withBoundaryMode(null));
    }
    
    @Test
    @DisplayName("Factory should validate optimization level parameter")
    void shouldValidateOptimizationLevelParameter() {
        assertThrows(NullPointerException.class,
            () -> factory.withOptimizationLevel(null));
    }
    
    @Test
    @DisplayName("Factory should support all boundary modes")
    void shouldSupportAllBoundaryModes() {
        for (BoundaryMode mode : BoundaryMode.values()) {
            factory.withBoundaryMode(mode);
            WaveletOpsConfig config = factory.create();
            assertEquals(mode, config.getBoundaryMode());
        }
    }
    
    @Test
    @DisplayName("Factory should support all optimization levels")
    void shouldSupportAllOptimizationLevels() {
        for (OptimizationLevel level : OptimizationLevel.values()) {
            factory.withOptimizationLevel(level);
            WaveletOpsConfig config = factory.create();
            assertEquals(level, config.getOptimizationLevel());
        }
    }
    
    @Test
    @DisplayName("Factory should support vectorization configuration")
    void shouldSupportVectorizationConfiguration() {
        factory.withVectorization(false);
        WaveletOpsConfig config1 = factory.create();
        assertFalse(config1.isVectorizationEnabled());
        
        factory.withVectorization(true);
        WaveletOpsConfig config2 = factory.create();
        assertTrue(config2.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Factory should provide access to current settings")
    void shouldProvideAccessToCurrentSettings() {
        factory.withBoundaryMode(BoundaryMode.ZERO_PADDING)
               .withOptimizationLevel(OptimizationLevel.BASIC)
               .withVectorization(false);
        
        assertEquals(BoundaryMode.ZERO_PADDING, factory.getBoundaryMode());
        assertEquals(OptimizationLevel.BASIC, factory.getOptimizationLevel());
        assertFalse(factory.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Factory should provide meaningful description")
    void shouldProvideMeaningfulDescription() {
        String description = factory.getDescription();
        
        assertNotNull(description);
        assertFalse(description.trim().isEmpty());
        assertTrue(description.contains("wavelet operation"));
    }
    
    @Test
    @DisplayName("Factory should return correct product type")
    void shouldReturnCorrectProductType() {
        Class<WaveletOpsConfig> productType = factory.getProductType();
        
        assertNotNull(productType);
        assertEquals(WaveletOpsConfig.class, productType);
    }
    
    @Test
    @DisplayName("Static createDefault should work correctly")
    void staticCreateDefaultShouldWorkCorrectly() {
        WaveletOpsConfig config = WaveletOpsFactory.createDefault();
        
        assertNotNull(config);
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertEquals(OptimizationLevel.STANDARD, config.getOptimizationLevel());
        assertTrue(config.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Factory should create new instances each time")
    void shouldCreateNewInstancesEachTime() {
        WaveletOpsConfig config1 = factory.create();
        WaveletOpsConfig config2 = factory.create();
        
        assertNotNull(config1);
        assertNotNull(config2);
        assertNotSame(config1, config2);
        assertEquals(config1, config2); // But they should be equal
    }
    
    @Test
    @DisplayName("Configuration changes should affect new instances")
    void configurationChangesShouldAffectNewInstances() {
        WaveletOpsConfig config1 = factory.create();
        
        factory.withBoundaryMode(BoundaryMode.ZERO_PADDING);
        WaveletOpsConfig config2 = factory.create();
        
        assertEquals(BoundaryMode.PERIODIC, config1.getBoundaryMode());
        assertEquals(BoundaryMode.ZERO_PADDING, config2.getBoundaryMode());
        assertNotEquals(config1, config2);
    }
}