package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.api.DiscreteWavelet;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for WaveletValidationUtils.
 * Tests all public methods for proper validation behavior and error conditions.
 */
@DisplayName("WaveletValidationUtils Tests")
class WaveletValidationUtilsTest {

    // Mock implementations for testing
    private static class MockDiscreteWavelet implements DiscreteWavelet {
        private final double[] lowPass;
        
        public MockDiscreteWavelet(double[] lowPass) {
            this.lowPass = lowPass;
        }
        
        @Override
        public String name() { return "MockDiscrete"; }
        
        @Override
        public double[] lowPassDecomposition() { return lowPass; }
        
        @Override
        public double[] highPassDecomposition() { return new double[]{1.0, -1.0}; }
        
        @Override
        public double[] lowPassReconstruction() { return lowPass; }
        
        @Override
        public double[] highPassReconstruction() { return new double[]{-1.0, 1.0}; }
        
        @Override
        public int vanishingMoments() { return 1; }
    }
    
    private static class MockContinuousWavelet implements ContinuousWavelet {
        @Override
        public String name() { return "MockContinuous"; }
        
        @Override
        public double evaluate(double t) { return Math.exp(-t*t/2); }
        
        @Override
        public double centerFrequency() { return 1.0; }
        
        @Override
        public double bandwidth() { return 1.0; }
        
        @Override
        public boolean isComplex() { return false; }
    }

    // === Constructor Test ===
    
    @Test
    @DisplayName("Should prevent instantiation")
    void testConstructorThrows() {
        assertThrows(AssertionError.class, () -> {
            // Use reflection to access private constructor
            var constructor = WaveletValidationUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }

    // === validateWaveletNotNull Tests ===
    
    @Test
    @DisplayName("validateWaveletNotNull should accept valid wavelet")
    void testValidateWaveletNotNullValid() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0});
        
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateWaveletNotNull(wavelet, "testWavelet"));
    }
    
    @Test
    @DisplayName("validateWaveletNotNull should reject null wavelet")
    void testValidateWaveletNotNullRejectsNull() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateWaveletNotNull(null, "testParam"));
        
        assertTrue(exception.getMessage().contains("testParam"));
        assertTrue(exception.getMessage().contains("null"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"wavelet", "filter", "input", "parameter"})
    @DisplayName("validateWaveletNotNull should include parameter name in error")
    void testValidateWaveletNotNullParameterName(String paramName) {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateWaveletNotNull(null, paramName));
        
        assertTrue(exception.getMessage().contains(paramName));
    }

    // === validateDiscreteWavelet Tests ===
    
    @Test
    @DisplayName("validateDiscreteWavelet should accept discrete wavelet")
    void testValidateDiscreteWaveletValid() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0});
        
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDiscreteWavelet(wavelet));
    }
    
    @Test
    @DisplayName("validateDiscreteWavelet should reject null wavelet")
    void testValidateDiscreteWaveletRejectsNull() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateDiscreteWavelet(null));
        
        assertTrue(exception.getMessage().contains("wavelet"));
        assertTrue(exception.getMessage().contains("null"));
    }
    
    @Test
    @DisplayName("validateDiscreteWavelet should reject continuous wavelet")
    void testValidateDiscreteWaveletRejectsContinuous() {
        Wavelet wavelet = new MockContinuousWavelet();
        
        InvalidConfigurationException exception = assertThrows(InvalidConfigurationException.class, () ->
            WaveletValidationUtils.validateDiscreteWavelet(wavelet));
        
        assertTrue(exception.getMessage().contains("MockContinuous"));
        assertTrue(exception.getMessage().contains("discrete wavelet transform"));
    }

    // === validateDecompositionLevel Tests ===
    
    @ParameterizedTest
    @CsvSource({
        "1, 5, 'Valid level 1'",
        "3, 5, 'Valid level 3'", 
        "5, 5, 'Valid max level'"
    })
    @DisplayName("validateDecompositionLevel should accept valid levels")
    void testValidateDecompositionLevelValid(int level, int maxLevel, String context) {
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(level, maxLevel, context));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("validateDecompositionLevel should reject levels < 1")
    void testValidateDecompositionLevelTooLow(int level) {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateDecompositionLevel(level, 5, "test"));
        
        assertTrue(exception.getMessage().contains("at least 1"));
        assertTrue(exception.getMessage().contains(String.valueOf(level)));
        assertTrue(exception.getMessage().contains("test"));
    }
    
    @ParameterizedTest
    @CsvSource({
        "6, 5",
        "10, 3",
        "100, 1"
    })
    @DisplayName("validateDecompositionLevel should reject levels > max")
    void testValidateDecompositionLevelTooHigh(int level, int maxLevel) {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateDecompositionLevel(level, maxLevel, "test context"));
        
        assertTrue(exception.getMessage().contains("exceeds maximum"));
        assertTrue(exception.getMessage().contains(String.valueOf(level)));
        assertTrue(exception.getMessage().contains(String.valueOf(maxLevel)));
        assertTrue(exception.getMessage().contains("test context"));
    }

    // === validateCoefficientLengths Tests ===
    
    @ParameterizedTest
    @CsvSource({
        "1, 1, 'Single element'",
        "8, 8, 'Power of 2'",
        "100, 100, 'Large equal arrays'",
        "0, 0, 'Empty arrays'"
    })
    @DisplayName("validateCoefficientLengths should accept matching lengths")
    void testValidateCoefficientLengthsValid(int approxLength, int detailLength, String context) {
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(approxLength, detailLength, context));
    }
    
    @ParameterizedTest
    @CsvSource({
        "5, 3, 'Different small sizes'",
        "8, 4, 'Power of 2 mismatch'",
        "100, 50, 'Large mismatch'",
        "1, 0, 'One empty'"
    })
    @DisplayName("validateCoefficientLengths should reject mismatched lengths")
    void testValidateCoefficientLengthsMismatch(int approxLength, int detailLength, String context) {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.validateCoefficientLengths(approxLength, detailLength, context));
        
        assertTrue(exception.getMessage().contains("same length"));
        assertTrue(exception.getMessage().contains("Approximation: " + approxLength));
        assertTrue(exception.getMessage().contains("Detail: " + detailLength));
        assertTrue(exception.getMessage().contains(context));
    }

    // === calculateMaxDecompositionLevels Tests ===
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should accept discrete wavelet")
    void testCalculateMaxDecompositionLevelsValid() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0}); // filter length = 2
        
        // Signal length 8, filter length 2 -> max levels should be 3
        // Level 1: 8 -> 4, Level 2: 4 -> 2, Level 3: 2 -> 1, Level 4: would be < filter length
        int result = WaveletValidationUtils.calculateMaxDecompositionLevels(8, wavelet, 10);
        assertEquals(3, result);
    }
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should respect maxAllowed limit")
    void testCalculateMaxDecompositionLevelsLimited() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0}); // filter length = 2
        
        // Even though signal could support 3 levels, limit to 2
        int result = WaveletValidationUtils.calculateMaxDecompositionLevels(8, wavelet, 2);
        assertEquals(2, result);
    }
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should handle larger filter lengths")
    void testCalculateMaxDecompositionLevelsLargeFilter() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0, 1.0, 1.0}); // filter length = 4
        
        // Signal length 8, filter length 4 -> max levels should be 1
        // Level 1: 8 -> 4, Level 2: would be 2 < filter length 4
        int result = WaveletValidationUtils.calculateMaxDecompositionLevels(8, wavelet, 10);
        assertEquals(1, result);
    }
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should return at least 1")
    void testCalculateMaxDecompositionLevelsMinimum() {
        Wavelet wavelet = new MockDiscreteWavelet(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}); // filter length = 8
        
        // Signal length 4, filter length 8 -> should still return 1 (minimum)
        int result = WaveletValidationUtils.calculateMaxDecompositionLevels(4, wavelet, 10);
        assertEquals(1, result);
    }
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should reject null wavelet")
    void testCalculateMaxDecompositionLevelsNullWavelet() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () ->
            WaveletValidationUtils.calculateMaxDecompositionLevels(8, null, 5));
        
        assertTrue(exception.getMessage().contains("wavelet"));
    }
    
    @Test
    @DisplayName("calculateMaxDecompositionLevels should reject continuous wavelet")
    void testCalculateMaxDecompositionLevelsContinuousWavelet() {
        Wavelet wavelet = new MockContinuousWavelet();
        
        InvalidConfigurationException exception = assertThrows(InvalidConfigurationException.class, () ->
            WaveletValidationUtils.calculateMaxDecompositionLevels(8, wavelet, 5));
        
        assertTrue(exception.getMessage().contains("discrete wavelet transform"));
    }
    
    @ParameterizedTest
    @CsvSource({
        "16, 2, 10, 4",  // 16->8->4->2->1, 4 levels
        "32, 2, 10, 5",  // 32->16->8->4->2->1, 5 levels  
        "64, 4, 10, 3",  // 64->32->16->8, then 8/2=4 == filter length so stop, 3 levels
        "7, 2, 10, 2"    // 7->4->2->1, 3 would be possible but 7 is odd so (7+1)/2 = 4, 4->2->1
    })
    @DisplayName("calculateMaxDecompositionLevels edge cases")
    void testCalculateMaxDecompositionLevelsEdgeCases(int signalLength, int filterLength, int maxAllowed, int expected) {
        double[] filter = new double[filterLength];
        java.util.Arrays.fill(filter, 1.0);
        Wavelet wavelet = new MockDiscreteWavelet(filter);
        
        int result = WaveletValidationUtils.calculateMaxDecompositionLevels(signalLength, wavelet, maxAllowed);
        assertEquals(expected, result);
    }
}