package ai.prophetizo.wavelet.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidStateExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Test invalid state message";
        InvalidStateException exception = new InvalidStateException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
    }

    @Test
    void testConstructorWithErrorCodeAndMessage() {
        ErrorCode errorCode = ErrorCode.STATE_INVALID;
        String message = "Test message with error code";
        InvalidStateException exception = new InvalidStateException(errorCode, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    void testClosedFactory() {
        String resourceName = "TestResource";
        InvalidStateException exception = InvalidStateException.closed(resourceName);
        
        assertEquals("TestResource is closed", exception.getMessage());
        assertEquals(ErrorCode.STATE_CLOSED, exception.getErrorCode());
    }

    @Test
    void testClosedFactoryWithNullResource() {
        InvalidStateException exception = InvalidStateException.closed(null);
        
        assertEquals("null is closed", exception.getMessage());
        assertEquals(ErrorCode.STATE_CLOSED, exception.getErrorCode());
    }

    @Test
    void testNotInitializedFactory() {
        String objectName = "TestObject";
        InvalidStateException exception = InvalidStateException.notInitialized(objectName);
        
        assertEquals("TestObject has not been initialized", exception.getMessage());
        assertEquals(ErrorCode.STATE_INVALID, exception.getErrorCode());
    }

    @Test
    void testNotInitializedFactoryWithNullObject() {
        InvalidStateException exception = InvalidStateException.notInitialized(null);
        
        assertEquals("null has not been initialized", exception.getMessage());
        assertEquals(ErrorCode.STATE_INVALID, exception.getErrorCode());
    }

    @Test
    void testInheritance() {
        InvalidStateException exception = new InvalidStateException("test");
        
        // Verify inheritance hierarchy
        assertTrue(exception instanceof WaveletTransformException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    void testDifferentErrorCodes() {
        // Test with different error codes to ensure they're properly set
        InvalidStateException exception1 = new InvalidStateException(ErrorCode.STATE_CLOSED, "closed");
        InvalidStateException exception2 = new InvalidStateException(ErrorCode.STATE_INVALID, "invalid");
        
        assertEquals(ErrorCode.STATE_CLOSED, exception1.getErrorCode());
        assertEquals(ErrorCode.STATE_INVALID, exception2.getErrorCode());
        assertNotEquals(exception1.getErrorCode(), exception2.getErrorCode());
    }

    @Test
    void testMessageFormatting() {
        // Test various resource/object names to ensure proper formatting
        assertEquals("myResource is closed", InvalidStateException.closed("myResource").getMessage());
        assertEquals("StreamProcessor is closed", InvalidStateException.closed("StreamProcessor").getMessage());
        assertEquals("connection-pool is closed", InvalidStateException.closed("connection-pool").getMessage());
        
        assertEquals("myObject has not been initialized", InvalidStateException.notInitialized("myObject").getMessage());
        assertEquals("WaveletTransform has not been initialized", InvalidStateException.notInitialized("WaveletTransform").getMessage());
        assertEquals("buffer_123 has not been initialized", InvalidStateException.notInitialized("buffer_123").getMessage());
    }

    @Test
    void testEmptyStringHandling() {
        // Test with empty strings
        InvalidStateException closedException = InvalidStateException.closed("");
        InvalidStateException notInitException = InvalidStateException.notInitialized("");
        
        assertEquals(" is closed", closedException.getMessage());
        assertEquals(" has not been initialized", notInitException.getMessage());
    }
}