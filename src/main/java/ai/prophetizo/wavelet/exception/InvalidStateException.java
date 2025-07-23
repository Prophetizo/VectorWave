package ai.prophetizo.wavelet.exception;

/**
 * Exception thrown when an operation is attempted on an object in an invalid state.
 * This is the custom equivalent of IllegalStateException within the wavelet transform hierarchy.
 */
public class InvalidStateException extends WaveletTransformException {

    /**
     * Constructs a new invalid state exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidStateException(String message) {
        super(message);
    }

    /**
     * Creates an exception for operations attempted on a closed resource.
     *
     * @param resourceName the name of the closed resource
     * @return a new InvalidStateException
     */
    public static InvalidStateException closed(String resourceName) {
        return new InvalidStateException(resourceName + " is closed");
    }

    /**
     * Creates an exception for operations that require initialization.
     *
     * @param objectName the name of the uninitialized object
     * @return a new InvalidStateException
     */
    public static InvalidStateException notInitialized(String objectName) {
        return new InvalidStateException(objectName + " has not been initialized");
    }
}
