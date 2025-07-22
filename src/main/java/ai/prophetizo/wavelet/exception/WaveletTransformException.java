package ai.prophetizo.wavelet.exception;

/**
 * Base exception for all wavelet transform related errors.
 * This is the parent class for more specific wavelet exceptions.
 */
public class WaveletTransformException extends RuntimeException {

    /**
     * Constructs a new wavelet transform exception with the specified detail message.
     *
     * @param message the detail message
     */
    public WaveletTransformException(String message) {
        super(message);
    }

    /**
     * Constructs a new wavelet transform exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public WaveletTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}