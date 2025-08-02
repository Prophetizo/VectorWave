package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class WaveletValidationUtilsTest {

    @Test
    void testConstructor_ThrowsAssertionError() throws Exception {
        // Test that the utility class cannot be instantiated
        var constructor = WaveletValidationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        try {
            constructor.newInstance();
            fail("Expected AssertionError to be thrown");
        } catch (InvocationTargetException e) {
            // The real exception is wrapped in InvocationTargetException
            assertTrue(e.getCause() instanceof AssertionError);
        }
    }

    @Test
    void testValidateWaveletNotNull_WithValidWavelet_DoesNotThrow() {
        Wavelet wavelet = new Haar();
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateWaveletNotNull(wavelet, "wavelet")
        );
    }

    @Test
    void testValidateWaveletNotNull_WithNullWavelet_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateWaveletNotNull(null, "testWavelet")
        );
        assertTrue(exception.getMessage().contains("testWavelet"));
    }

    @Test
    void testValidateDiscreteWavelet_WithDiscreteWavelet_DoesNotThrow() {
        DiscreteWavelet wavelet = new Haar();
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDiscreteWavelet(wavelet)
        );
    }

    @Test
    void testValidateDiscreteWavelet_WithContinuousWavelet_ThrowsException() {
        ContinuousWavelet wavelet = new MorletWavelet();
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> WaveletValidationUtils.validateDiscreteWavelet(wavelet)
        );
        assertTrue(exception.getMessage().contains("MorletWavelet"));
        assertTrue(exception.getMessage().contains("discrete wavelet transform operations"));
    }

    @Test
    void testValidateDiscreteWavelet_WithNullWavelet_ThrowsException() {
        assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateDiscreteWavelet(null)
        );
    }

    @Test
    void testValidateDecompositionLevel_WithValidLevel_DoesNotThrow() {
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(3, 5, "test context")
        );
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(1, 1, "test context")
        );
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(5, 5, "test context")
        );
    }

    @Test
    void testValidateDecompositionLevel_WithZeroLevel_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateDecompositionLevel(0, 5, "test context")
        );
        assertTrue(exception.getMessage().contains("Decomposition level must be at least 1"));
        assertTrue(exception.getMessage().contains("got: 0"));
        assertTrue(exception.getMessage().contains("test context"));
    }

    @Test
    void testValidateDecompositionLevel_WithNegativeLevel_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateDecompositionLevel(-1, 5, "test context")
        );
        assertTrue(exception.getMessage().contains("Decomposition level must be at least 1"));
        assertTrue(exception.getMessage().contains("got: -1"));
    }

    @Test
    void testValidateDecompositionLevel_WithLevelExceedingMax_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateDecompositionLevel(6, 5, "test context")
        );
        assertTrue(exception.getMessage().contains("Decomposition level 6 exceeds maximum 5"));
        assertTrue(exception.getMessage().contains("test context"));
    }

    @Test
    void testValidateCoefficientLengths_WithMatchingLengths_DoesNotThrow() {
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(10, 10, "test context")
        );
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(0, 0, "test context")
        );
    }

    @Test
    void testValidateCoefficientLengths_WithMismatchedLengths_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.validateCoefficientLengths(10, 8, "test context")
        );
        assertTrue(exception.getMessage().contains("Coefficient arrays must have same length"));
        assertTrue(exception.getMessage().contains("Approximation: 10, Detail: 8"));
        assertTrue(exception.getMessage().contains("test context"));
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithHaarWavelet() {
        Wavelet haar = new Haar();
        
        // Haar has filter length 2
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(2, haar, 10));
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(4, haar, 10));
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(8, haar, 10));
        assertEquals(4, WaveletValidationUtils.calculateMaxDecompositionLevels(16, haar, 10));
        assertEquals(5, WaveletValidationUtils.calculateMaxDecompositionLevels(32, haar, 10));
        assertEquals(6, WaveletValidationUtils.calculateMaxDecompositionLevels(64, haar, 10));
        assertEquals(7, WaveletValidationUtils.calculateMaxDecompositionLevels(128, haar, 10));
        assertEquals(8, WaveletValidationUtils.calculateMaxDecompositionLevels(256, haar, 10));
        assertEquals(9, WaveletValidationUtils.calculateMaxDecompositionLevels(512, haar, 10));
        assertEquals(10, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 10)); // Limited by maxAllowed
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithDaubechiesWavelet() {
        Wavelet db4 = Daubechies.DB4;
        
        // DB4 has filter length 8
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(8, db4, 10));
        // For length 15: 15->8->4 (stop, 4 < 8), so 2 levels
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(15, db4, 10));
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(16, db4, 10));
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(32, db4, 10));
        assertEquals(4, WaveletValidationUtils.calculateMaxDecompositionLevels(64, db4, 10));
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithSmallMaxAllowed() {
        Wavelet haar = new Haar();
        
        // Should be limited by maxAllowed parameter
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 1));
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 2));
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 3));
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithSignalShorterThanFilter() {
        Wavelet db4 = Daubechies.DB4; // Filter length 8
        
        // When signal is shorter than filter, should return 1
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1, db4, 10));
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(4, db4, 10));
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(7, db4, 10));
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithContinuousWavelet_ThrowsException() {
        ContinuousWavelet morlet = new MorletWavelet();
        
        assertThrows(
                InvalidConfigurationException.class,
                () -> WaveletValidationUtils.calculateMaxDecompositionLevels(128, morlet, 10)
        );
    }

    @Test
    void testCalculateMaxDecompositionLevels_WithNullWavelet_ThrowsException() {
        assertThrows(
                InvalidArgumentException.class,
                () -> WaveletValidationUtils.calculateMaxDecompositionLevels(128, null, 10)
        );
    }

    @Test
    void testCalculateMaxDecompositionLevels_EdgeCases() {
        Wavelet haar = new Haar();
        
        // Test with zero maxAllowed - should return 1 (minimum)
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 0));
        
        // Test with very small signal
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1, haar, 10));
        
        // Test boundary conditions (Haar has filter length 2)
        // For length 3: 3->2->1, so 2 levels
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(3, haar, 10));
        // For length 5: 5->3->2->1, so 3 levels  
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(5, haar, 10));
    }
}