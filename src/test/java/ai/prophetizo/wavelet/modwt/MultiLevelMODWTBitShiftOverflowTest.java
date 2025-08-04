package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify bit shift overflow protection in MultiLevelMODWTTransform.
 */
class MultiLevelMODWTBitShiftOverflowTest {
    
    @Test
    void testCalculateMaxLevelsDoesNotOverflow() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private calculateMaxLevels method via reflection
        Method calculateMaxLevels = MultiLevelMODWTTransform.class
            .getDeclaredMethod("calculateMaxLevels", int.class);
        calculateMaxLevels.setAccessible(true);
        
        // Test with various signal lengths
        // Should not overflow even with very large signal
        int maxLevels = (int) calculateMaxLevels.invoke(transform, Integer.MAX_VALUE);
        assertTrue(maxLevels > 0 && maxLevels <= 10, 
            "Max levels should be reasonable even for MAX_VALUE signal length");
        
        // Test edge case where we might approach 31 levels
        // This should be safely capped without overflow
        maxLevels = (int) calculateMaxLevels.invoke(transform, 1_000_000_000);
        assertTrue(maxLevels > 0 && maxLevels <= 10, 
            "Max levels should be capped at MAX_DECOMPOSITION_LEVELS");
    }
    
    @Test
    void testUpsampleFilterForLevelWithHighLevel() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private upsampleFilterForLevel method via reflection
        Method upsampleFilterForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("upsampleFilterForLevel", double[].class, int.class);
        upsampleFilterForLevel.setAccessible(true);
        
        double[] filter = {0.7071, 0.7071};
        
        // Test with level that would cause overflow (32 or higher)
        assertThrows(Exception.class, () -> {
            upsampleFilterForLevel.invoke(transform, filter, 32);
        }, "Should throw exception for level >= 32");
        
        // Test with maximum safe level (31)
        // This should also throw because it would create massive arrays
        assertThrows(Exception.class, () -> {
            upsampleFilterForLevel.invoke(transform, filter, 31);
        }, "Should throw exception for level 31");
    }
    
    @Test
    void testScaleFilterForLevelWithHighLevel() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private scaleFilterForLevel method via reflection
        Method scaleFilterForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("scaleFilterForLevel", double[].class, int.class);
        scaleFilterForLevel.setAccessible(true);
        
        double[] filter = {0.7071, 0.7071};
        
        // Test with level that would cause overflow
        assertThrows(Exception.class, () -> {
            scaleFilterForLevel.invoke(transform, filter, 32);
        }, "Should throw exception for level >= 32");
    }
    
    @Test
    void testDecomposeWithReasonableLevels() {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Create a signal
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.1);
        }
        
        // Should work fine with reasonable levels
        MultiLevelMODWTResult result = transform.decompose(signal, 5);
        assertNotNull(result);
        assertEquals(5, result.getLevels());
        
        // Should reject levels that are too high
        int maxLevels = transform.getMaximumLevels(signal.length);
        assertThrows(InvalidArgumentException.class, () -> {
            transform.decompose(signal, maxLevels + 1);
        }, "Should reject levels exceeding maximum");
    }
}