package ai.prophetizo.wavelet.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ErrorCode enum.
 * Tests both the enum itself and its integration with exception classes.
 */
@DisplayName("Error Code Tests")
class ErrorCodeTest {
    
    // === ErrorCode Enum Tests ===
    
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("All error codes should have non-null code and description")
    void testErrorCodeProperties(ErrorCode errorCode) {
        assertNotNull(errorCode.getCode(), "Code should not be null for " + errorCode);
        assertNotNull(errorCode.getDescription(), "Description should not be null for " + errorCode);
        assertFalse(errorCode.getCode().isEmpty(), "Code should not be empty for " + errorCode);
        assertFalse(errorCode.getDescription().isEmpty(), "Description should not be empty for " + errorCode);
    }
    
    @Test
    @DisplayName("All error codes should have unique codes")
    void testErrorCodeUniqueness() {
        ErrorCode[] allCodes = ErrorCode.values();
        Set<String> uniqueCodes = new HashSet<>();
        
        for (ErrorCode errorCode : allCodes) {
            String code = errorCode.getCode();
            assertFalse(uniqueCodes.contains(code), 
                "Duplicate error code found: " + code + " for " + errorCode);
            uniqueCodes.add(code);
        }
        
        assertEquals(allCodes.length, uniqueCodes.size(), "All codes should be unique");
    }
    
    @ParameterizedTest
    @CsvSource({
        "VAL_NULL_ARGUMENT, VAL_001, Null argument",
        "VAL_NOT_POWER_OF_TWO, VAL_002, Signal length not power of two",
        "VAL_NON_FINITE_VALUES, VAL_003, Signal contains non-finite values",
        "VAL_NOT_POSITIVE, VAL_004, Value must be positive",
        "VAL_TOO_LARGE, VAL_005, Value too large",
        "VAL_EMPTY, VAL_006, Empty array or collection",
        "VAL_LENGTH_MISMATCH, VAL_007, Array length mismatch",
        "CFG_UNSUPPORTED_OPERATION, CFG_001, Unsupported operation",
        "CFG_CONFLICTING_OPTIONS, CFG_002, Conflicting options",
        "CFG_UNSUPPORTED_BOUNDARY_MODE, CFG_003, Unsupported boundary mode",
        "CFG_INVALID_DECOMPOSITION_LEVEL, CFG_004, Invalid decomposition level",
        "SIG_TOO_SHORT, SIG_001, Signal too short",
        "SIG_MAX_LEVEL_EXCEEDED, SIG_002, Maximum decomposition level exceeded",
        "SIG_INVALID_THRESHOLD, SIG_003, Invalid threshold value",
        "STATE_CLOSED, STATE_001, Resource closed",
        "STATE_INVALID, STATE_002, Invalid state",
        "STATE_IN_PROGRESS, STATE_003, Operation in progress",
        "POOL_FULL, POOL_001, Pool full",
        "POOL_EXHAUSTED, POOL_002, Pool exhausted"
    })
    @DisplayName("Error codes should have correct code and description values")
    void testSpecificErrorCodeValues(String enumName, String expectedCode, String expectedDescription) {
        ErrorCode errorCode = ErrorCode.valueOf(enumName);
        assertEquals(expectedCode, errorCode.getCode());
        assertEquals(expectedDescription, errorCode.getDescription());
    }
    
    @Test
    @DisplayName("Error code categories should follow naming convention")
    void testErrorCodeCategories() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            String code = errorCode.getCode();
            assertTrue(code.matches("^[A-Z]+_[0-9]{3}$"), 
                "Error code should follow pattern PREFIX_NNN: " + code);
        }
        
        // Test specific category prefixes exist
        assertTrue(hasCodeWithPrefix("VAL_"), "Should have validation error codes");
        assertTrue(hasCodeWithPrefix("CFG_"), "Should have configuration error codes");
        assertTrue(hasCodeWithPrefix("SIG_"), "Should have signal processing error codes");
        assertTrue(hasCodeWithPrefix("STATE_"), "Should have state error codes");
        assertTrue(hasCodeWithPrefix("POOL_"), "Should have pool error codes");
    }
    
    private boolean hasCodeWithPrefix(String prefix) {
        return Arrays.stream(ErrorCode.values())
                .anyMatch(ec -> ec.getCode().startsWith(prefix));
    }
    
    @Test
    @DisplayName("Error code toString should include code and description")
    void testErrorCodeToString() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            String toString = errorCode.toString();
            assertTrue(toString.contains(errorCode.getCode()), 
                "toString should contain code for " + errorCode);
            assertTrue(toString.contains(errorCode.getDescription()), 
                "toString should contain description for " + errorCode);
            assertTrue(toString.contains(": "), 
                "toString should contain separator for " + errorCode);
        }
    }
    
    @Test
    @DisplayName("Error code toString format should be consistent")
    void testErrorCodeToStringFormat() {
        ErrorCode testCode = ErrorCode.VAL_NULL_ARGUMENT;
        String result = testCode.toString();
        assertEquals("VAL_001: Null argument", result);
    }
    
    @Test
    @DisplayName("All validation error codes should start with VAL_")
    void testValidationErrorCodes() {
        ErrorCode[] validationCodes = {
            ErrorCode.VAL_NULL_ARGUMENT,
            ErrorCode.VAL_NOT_POWER_OF_TWO,
            ErrorCode.VAL_NON_FINITE_VALUES,
            ErrorCode.VAL_NOT_POSITIVE,
            ErrorCode.VAL_TOO_LARGE,
            ErrorCode.VAL_EMPTY,
            ErrorCode.VAL_LENGTH_MISMATCH
        };
        
        for (ErrorCode code : validationCodes) {
            assertTrue(code.getCode().startsWith("VAL_"), 
                "Validation code should start with VAL_: " + code);
        }
    }
    
    @Test
    @DisplayName("All configuration error codes should start with CFG_")
    void testConfigurationErrorCodes() {
        ErrorCode[] configCodes = {
            ErrorCode.CFG_UNSUPPORTED_OPERATION,
            ErrorCode.CFG_CONFLICTING_OPTIONS,
            ErrorCode.CFG_UNSUPPORTED_BOUNDARY_MODE,
            ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL
        };
        
        for (ErrorCode code : configCodes) {
            assertTrue(code.getCode().startsWith("CFG_"), 
                "Configuration code should start with CFG_: " + code);
        }
    }
    
    @Test
    @DisplayName("All signal processing error codes should start with SIG_")
    void testSignalErrorCodes() {
        ErrorCode[] signalCodes = {
            ErrorCode.SIG_TOO_SHORT,
            ErrorCode.SIG_MAX_LEVEL_EXCEEDED,
            ErrorCode.SIG_INVALID_THRESHOLD
        };
        
        for (ErrorCode code : signalCodes) {
            assertTrue(code.getCode().startsWith("SIG_"), 
                "Signal code should start with SIG_: " + code);
        }
    }
    
    @Test
    @DisplayName("All state error codes should start with STATE_")
    void testStateErrorCodes() {
        ErrorCode[] stateCodes = {
            ErrorCode.STATE_CLOSED,
            ErrorCode.STATE_INVALID,
            ErrorCode.STATE_IN_PROGRESS
        };
        
        for (ErrorCode code : stateCodes) {
            assertTrue(code.getCode().startsWith("STATE_"), 
                "State code should start with STATE_: " + code);
        }
    }
    
    @Test
    @DisplayName("All pool error codes should start with POOL_")
    void testPoolErrorCodes() {
        ErrorCode[] poolCodes = {
            ErrorCode.POOL_FULL,
            ErrorCode.POOL_EXHAUSTED
        };
        
        for (ErrorCode code : poolCodes) {
            assertTrue(code.getCode().startsWith("POOL_"), 
                "Pool code should start with POOL_: " + code);
        }
    }
    
    @Test
    @DisplayName("Error code values method should return all defined codes")
    void testErrorCodeValues() {
        ErrorCode[] allCodes = ErrorCode.values();
        assertTrue(allCodes.length >= 19, "Should have at least 19 error codes defined");
        
        // Verify specific codes are present
        assertTrue(Arrays.asList(allCodes).contains(ErrorCode.VAL_NULL_ARGUMENT));
        assertTrue(Arrays.asList(allCodes).contains(ErrorCode.CFG_UNSUPPORTED_OPERATION));
        assertTrue(Arrays.asList(allCodes).contains(ErrorCode.SIG_TOO_SHORT));
        assertTrue(Arrays.asList(allCodes).contains(ErrorCode.STATE_CLOSED));
        assertTrue(Arrays.asList(allCodes).contains(ErrorCode.POOL_FULL));
    }
    
    @Test
    @DisplayName("Error code valueOf should work for all enum names")
    void testErrorCodeValueOf() {
        // Test a few specific ones
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, ErrorCode.valueOf("VAL_NULL_ARGUMENT"));
        assertEquals(ErrorCode.CFG_CONFLICTING_OPTIONS, ErrorCode.valueOf("CFG_CONFLICTING_OPTIONS"));
        assertEquals(ErrorCode.STATE_CLOSED, ErrorCode.valueOf("STATE_CLOSED"));
        
        // Test that invalid names throw exception
        assertThrows(IllegalArgumentException.class, () -> 
            ErrorCode.valueOf("INVALID_CODE"));
    }

    // === Integration Tests with Exceptions ===
    
    @Test
    @DisplayName("Exception should contain error code when created with code")
    void testExceptionWithErrorCode() {
        InvalidArgumentException ex = InvalidArgumentException.nullArgument("signal");
        
        assertTrue(ex.hasErrorCode());
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, ex.getErrorCode());
        assertEquals("VAL_001", ex.getErrorCode().getCode());
        assertEquals("signal cannot be null", ex.getMessage());
    }
    
    @Test
    @DisplayName("Exception without error code should return null")
    void testExceptionWithoutErrorCode() {
        InvalidArgumentException ex = new InvalidArgumentException("Custom message");
        
        assertFalse(ex.hasErrorCode());
        assertNull(ex.getErrorCode());
        assertEquals("Custom message", ex.getMessage());
    }
    
    @Test
    @DisplayName("InvalidSignalException should have correct error codes")
    void testInvalidSignalExceptionCodes() {
        InvalidSignalException nullEx = InvalidSignalException.nullSignal("data");
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, nullEx.getErrorCode());
        
        InvalidSignalException emptyEx = InvalidSignalException.emptySignal("data");
        assertEquals(ErrorCode.VAL_EMPTY, emptyEx.getErrorCode());
        
        InvalidSignalException powerEx = InvalidSignalException.notPowerOfTwo(100);
        assertEquals(ErrorCode.VAL_NOT_POWER_OF_TWO, powerEx.getErrorCode());
        
        InvalidSignalException nanEx = InvalidSignalException.nanValue("signal", 5);
        assertEquals(ErrorCode.VAL_NON_FINITE_VALUES, nanEx.getErrorCode());
        
        InvalidSignalException infEx = InvalidSignalException.infinityValue("signal", 10, Double.POSITIVE_INFINITY);
        assertEquals(ErrorCode.VAL_NON_FINITE_VALUES, infEx.getErrorCode());
        
        InvalidSignalException mismatchEx = InvalidSignalException.mismatchedCoefficients(10, 5);
        assertEquals(ErrorCode.VAL_LENGTH_MISMATCH, mismatchEx.getErrorCode());
    }
    
    @Test
    @DisplayName("InvalidStateException should have correct error codes")
    void testInvalidStateExceptionCodes() {
        InvalidStateException closedEx = InvalidStateException.closed("Transform");
        assertEquals(ErrorCode.STATE_CLOSED, closedEx.getErrorCode());
        
        InvalidStateException notInitEx = InvalidStateException.notInitialized("Pool");
        assertEquals(ErrorCode.STATE_INVALID, notInitEx.getErrorCode());
    }
    
    @Test
    @DisplayName("InvalidConfigurationException should have correct error codes")
    void testInvalidConfigurationExceptionCodes() {
        InvalidConfigurationException conflictEx = InvalidConfigurationException.conflictingOptions("forceScalar", "forceVector");
        assertEquals(ErrorCode.CFG_CONFLICTING_OPTIONS, conflictEx.getErrorCode());
        
        InvalidConfigurationException unsupportedEx = InvalidConfigurationException.unsupportedOperation("ContinuousWavelet", "discrete transform");
        assertEquals(ErrorCode.CFG_UNSUPPORTED_OPERATION, unsupportedEx.getErrorCode());
        
        InvalidConfigurationException boundaryEx = InvalidConfigurationException.unsupportedBoundaryMode("REFLECT");
        assertEquals(ErrorCode.CFG_UNSUPPORTED_BOUNDARY_MODE, boundaryEx.getErrorCode());
    }
    
    @Test
    @DisplayName("Error codes should have unique identifiers")
    void testErrorCodeUniquenessExamples() {
        // Check a few error codes
        assertNotEquals(ErrorCode.VAL_NULL_ARGUMENT.getCode(), ErrorCode.VAL_NOT_POWER_OF_TWO.getCode());
        assertNotEquals(ErrorCode.CFG_CONFLICTING_OPTIONS.getCode(), ErrorCode.STATE_CLOSED.getCode());
        assertNotEquals(ErrorCode.SIG_TOO_SHORT.getCode(), ErrorCode.POOL_FULL.getCode());
    }
    
    @Test
    @DisplayName("Programmatic error handling should work with error codes")
    void testProgrammaticErrorHandling() {
        try {
            throw InvalidArgumentException.nullArgument("test");
        } catch (InvalidArgumentException e) {
            // Can check error code programmatically
            if (e.hasErrorCode() && e.getErrorCode() == ErrorCode.VAL_NULL_ARGUMENT) {
                // Handle null argument case specifically
                assertEquals(ErrorCode.VAL_NULL_ARGUMENT, e.getErrorCode());
            } else {
                fail("Should have caught null argument error");
            }
        }
    }
    
    @Test
    @DisplayName("Error code should maintain enum contract")
    void testEnumContract() {
        ErrorCode code1 = ErrorCode.VAL_NULL_ARGUMENT;
        ErrorCode code2 = ErrorCode.VAL_NULL_ARGUMENT;
        ErrorCode code3 = ErrorCode.VAL_NOT_POWER_OF_TWO;
        
        // Test equality
        assertEquals(code1, code2);
        assertNotEquals(code1, code3);
        
        // Test hashCode
        assertEquals(code1.hashCode(), code2.hashCode());
        assertNotEquals(code1.hashCode(), code3.hashCode());
        
        // Test enum comparison
        assertTrue(code1.compareTo(code3) != 0);
        assertEquals(0, code1.compareTo(code2));
    }
}