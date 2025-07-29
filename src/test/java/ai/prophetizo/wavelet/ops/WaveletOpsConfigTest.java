package ai.prophetizo.wavelet.ops;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.ops.WaveletOpsFactory.OptimizationLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the WaveletOpsConfig class.
 * 
 * Validates the configuration class behavior, including immutability,
 * equality, and accessor methods.
 */
class WaveletOpsConfigTest {
    
    @Test
    @DisplayName("Config should be created with all parameters")
    void shouldBeCreatedWithAllParameters() {
        WaveletOpsConfig config = new WaveletOpsConfig(
            BoundaryMode.ZERO_PADDING, 
            OptimizationLevel.AGGRESSIVE, 
            false
        );
        
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertEquals(OptimizationLevel.AGGRESSIVE, config.getOptimizationLevel());
        assertFalse(config.isVectorizationEnabled());
    }
    
    @Test
    @DisplayName("Config should validate null boundary mode")
    void shouldValidateNullBoundaryMode() {
        assertThrows(NullPointerException.class, () ->
            new WaveletOpsConfig(null, OptimizationLevel.STANDARD, true)
        );
    }
    
    @Test
    @DisplayName("Config should validate null optimization level")
    void shouldValidateNullOptimizationLevel() {
        assertThrows(NullPointerException.class, () ->
            new WaveletOpsConfig(BoundaryMode.PERIODIC, null, true)
        );
    }
    
    @Test
    @DisplayName("Config should provide convenience methods")
    void shouldProvideConvenienceMethods() {
        WaveletOpsConfig periodicConfig = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        WaveletOpsConfig zeroPadConfig = new WaveletOpsConfig(
            BoundaryMode.ZERO_PADDING, OptimizationLevel.BASIC, false
        );
        WaveletOpsConfig aggressiveConfig = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.AGGRESSIVE, true
        );
        
        assertTrue(periodicConfig.isPeriodicBoundary());
        assertFalse(zeroPadConfig.isPeriodicBoundary());
        
        assertFalse(periodicConfig.isAggressiveOptimization());
        assertTrue(aggressiveConfig.isAggressiveOptimization());
    }
    
    @Test
    @DisplayName("Config should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        WaveletOpsConfig config1 = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        WaveletOpsConfig config2 = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        WaveletOpsConfig config3 = new WaveletOpsConfig(
            BoundaryMode.ZERO_PADDING, OptimizationLevel.STANDARD, true
        );
        
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1, config1); // reflexive
        assertNotEquals(config1, null);
        assertNotEquals(config1, "not a config");
    }
    
    @Test
    @DisplayName("Config should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        WaveletOpsConfig config1 = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        WaveletOpsConfig config2 = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        
        assertEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    @DisplayName("Config should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
        WaveletOpsConfig config = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.AGGRESSIVE, false
        );
        
        String toString = config.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("WaveletOpsConfig"));
        assertTrue(toString.contains("PERIODIC"));
        assertTrue(toString.contains("AGGRESSIVE"));
        assertTrue(toString.contains("false"));
    }
    
    @Test
    @DisplayName("Config should be immutable")
    void shouldBeImmutable() {
        WaveletOpsConfig config = new WaveletOpsConfig(
            BoundaryMode.PERIODIC, OptimizationLevel.STANDARD, true
        );
        
        // Store original values
        BoundaryMode originalBoundary = config.getBoundaryMode();
        OptimizationLevel originalLevel = config.getOptimizationLevel();
        boolean originalVectorization = config.isVectorizationEnabled();
        
        // There should be no way to modify the config after creation
        // (This is verified by the class design - no setters)
        
        // Values should remain unchanged
        assertEquals(originalBoundary, config.getBoundaryMode());
        assertEquals(originalLevel, config.getOptimizationLevel());
        assertEquals(originalVectorization, config.isVectorizationEnabled());
    }
}