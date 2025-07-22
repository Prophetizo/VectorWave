package ai.prophetizo.wavelet.config;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransformConfig and its builder.
 */
@DisplayName("TransformConfig Tests")
class TransformConfigTest {
    
    @Test
    @DisplayName("Default config should have expected values")
    void testDefaultConfig() {
        TransformConfig config = TransformConfig.builder().build();
        
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertEquals(20, config.getMaxDecompositionLevels());
    }
    
    @Test
    @DisplayName("Builder should set boundary mode correctly")
    void testBuilderBoundaryMode() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .build();
        
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
    }
    
    @Test
    @DisplayName("Builder should set max decomposition levels correctly")
    void testBuilderMaxDecompositionLevels() {
        TransformConfig config = TransformConfig.builder()
            .maxDecompositionLevels(5)
            .build();
        
        assertEquals(5, config.getMaxDecompositionLevels());
    }
    
    @Test
    @DisplayName("Builder should chain methods correctly")
    void testBuilderChaining() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .maxDecompositionLevels(3)
            .build();
        
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertEquals(3, config.getMaxDecompositionLevels());
    }
    
    @Test
    @DisplayName("Builder should reject null boundary mode")
    void testBuilderNullBoundaryMode() {
        TransformConfig.Builder builder = TransformConfig.builder();
        
        assertThrows(NullPointerException.class,
            () -> builder.boundaryMode(null),
            "Should throw exception for null boundary mode");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Builder should reject non-positive decomposition levels")
    void testBuilderInvalidDecompositionLevels(int invalidLevel) {
        TransformConfig.Builder builder = TransformConfig.builder();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> builder.maxDecompositionLevels(invalidLevel));
        
        assertTrue(exception.getMessage().contains("must be at least 1"),
            "Error message should mention minimum requirement");
    }
    
    @Test
    @DisplayName("Config should be immutable")
    void testConfigImmutability() {
        TransformConfig.Builder builder = TransformConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC)
            .maxDecompositionLevels(2);
        
        TransformConfig config1 = builder.build();
        
        // Modify builder after first build
        builder.boundaryMode(BoundaryMode.ZERO_PADDING)
               .maxDecompositionLevels(5);
        
        TransformConfig config2 = builder.build();
        
        // First config should be unchanged
        assertEquals(BoundaryMode.PERIODIC, config1.getBoundaryMode());
        assertEquals(2, config1.getMaxDecompositionLevels());
        
        // Second config should have new values
        assertEquals(BoundaryMode.ZERO_PADDING, config2.getBoundaryMode());
        assertEquals(5, config2.getMaxDecompositionLevels());
    }
    
    @Test
    @DisplayName("Config toString should contain all fields")
    void testConfigToString() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .maxDecompositionLevels(3)
            .build();
        
        String str = config.toString();
        
        assertTrue(str.contains("TransformConfig"), "Should contain class name");
        assertTrue(str.contains("ZERO_PADDING"), "Should contain boundary mode");
        assertTrue(str.contains("3"), "Should contain decomposition levels");
    }
    
    @Test
    @DisplayName("Multiple builds from same builder should work")
    void testMultipleBuilds() {
        TransformConfig.Builder builder = TransformConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC);
        
        TransformConfig config1 = builder.build();
        TransformConfig config2 = builder.build();
        
        assertNotSame(config1, config2, "Should create different instances");
        assertEquals(config1.getBoundaryMode(), config2.getBoundaryMode());
        assertEquals(config1.getMaxDecompositionLevels(), config2.getMaxDecompositionLevels());
    }
    
    @Test
    @DisplayName("Builder with all boundary modes")
    void testAllBoundaryModes() {
        for (BoundaryMode mode : BoundaryMode.values()) {
            TransformConfig config = TransformConfig.builder()
                .boundaryMode(mode)
                .build();
            
            assertEquals(mode, config.getBoundaryMode(),
                "Should correctly set " + mode + " boundary mode");
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10, 100})
    @DisplayName("Builder should accept various valid decomposition levels")
    void testValidDecompositionLevels(int level) {
        TransformConfig config = TransformConfig.builder()
            .maxDecompositionLevels(level)
            .build();
        
        assertEquals(level, config.getMaxDecompositionLevels());
    }
}