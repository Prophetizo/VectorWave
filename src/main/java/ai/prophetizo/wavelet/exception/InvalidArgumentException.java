package ai.prophetizo.wavelet.exception;

/**
 * Exception thrown when an invalid argument is provided to utility methods.
 * This provides consistency with the wavelet transform exception hierarchy
 * while maintaining semantic clarity for argument validation errors.
 */
public class InvalidArgumentException extends WaveletTransformException {
    private static final long serialVersionUID = 202501150003L; // Invalid argument exception v1.0

    /**
     * Constructs a new invalid argument exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidArgumentException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid argument exception with the specified error code and detail message.
     *
     * @param errorCode the error code
     * @param message   the detail message
     */
    public InvalidArgumentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an exception for null arguments.
     *
     * @param parameterName the name of the null parameter
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException nullArgument(String parameterName) {
        return new InvalidArgumentException(ErrorCode.VAL_NULL_ARGUMENT,
                parameterName + " cannot be null");
    }

    /**
     * Creates an exception for negative or zero input where positive is required.
     *
     * @param value the invalid value
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException notPositive(int value) {
        return new InvalidArgumentException(ErrorCode.VAL_NOT_POSITIVE,
                String.format("Input must be positive, but was: %d", value));
    }

    /**
     * Creates an exception for input that is too large.
     *
     * @param value    the value that is too large
     * @param maxValue the maximum allowed value
     * @param context  additional context about why the limit exists
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException tooLarge(int value, int maxValue, String context) {
        return new InvalidArgumentException(ErrorCode.VAL_TOO_LARGE,
                String.format("Input too large: %d. Maximum supported value is %d (2^30). %s",
                        value, maxValue, context));
    }

    /**
     * Creates an exception for wavelet configuration issues with detailed context.
     *
     * @param waveletName the name of the wavelet
     * @param boundaryMode the boundary mode being used
     * @param signalLength the signal length
     * @param reason the specific reason for the failure
     * @return a new InvalidArgumentException with remediation suggestions
     */
    public static InvalidArgumentException waveletConfigurationError(
            String waveletName, String boundaryMode, int signalLength, String reason) {
        String message = String.format(
            "Invalid wavelet configuration: %s wavelet with %s boundary mode on signal of length %d. " +
            "Reason: %s. " +
            "Suggestions: " +
            "1) For signals < 32 samples, consider using Haar wavelet; " +
            "2) For MODWT, any signal length is supported; " +
            "3) For DWT, signal length must be power of 2; " +
            "4) PERIODIC boundary mode works best for cyclic signals; " +
            "5) ZERO_PADDING is safer for general signals.",
            waveletName, boundaryMode, signalLength, reason);
        
        return new InvalidArgumentException(ErrorCode.VAL_INVALID_COMBINATION, message);
    }

    /**
     * Creates an exception for memory allocation issues with platform-specific guidance.
     *
     * @param requestedSize the requested allocation size
     * @param availableMemory the available memory
     * @param operation the operation being performed
     * @return a new InvalidArgumentException with memory optimization suggestions
     */
    public static InvalidArgumentException memoryAllocationError(
            long requestedSize, long availableMemory, String operation) {
        String message = String.format(
            "Insufficient memory for %s: requested %d bytes, available %d bytes. " +
            "Suggestions: " +
            "1) Reduce signal length or batch size; " +
            "2) Use streaming processing for large signals; " +
            "3) Enable memory pooling with AlignedMemoryPool; " +
            "4) Consider using MODWT instead of DWT for memory efficiency; " +
            "5) Increase JVM heap size with -Xmx flag.",
            operation, requestedSize, availableMemory);
        
        return new InvalidArgumentException(ErrorCode.POOL_ALLOCATION_FAILED, message);
    }

    /**
     * Creates an exception for SIMD/Vector API issues with platform-specific guidance.
     *
     * @param platform the detected platform
     * @param vectorLength the attempted vector length
     * @param operation the operation being performed
     * @return a new InvalidArgumentException with SIMD optimization suggestions
     */
    public static InvalidArgumentException simdConfigurationError(
            String platform, int vectorLength, String operation) {
        String message = String.format(
            "SIMD configuration error on %s platform: vector length %d not supported for %s. " +
            "Platform-specific suggestions: " +
            "1) For ARM/Apple Silicon: Use 2-element vectors (128-bit NEON); " +
            "2) For Intel x86_64: Use 4-element (AVX2) or 8-element (AVX512) vectors; " +
            "3) Enable Vector API with --add-modules=jdk.incubator.vector; " +
            "4) For Java < 23: SIMD optimizations may be limited; " +
            "5) Consider scalar fallback for better portability.",
            platform, vectorLength, operation);
        
        return new InvalidArgumentException(ErrorCode.CFG_PLATFORM_UNSUPPORTED, message);
    }

    /**
     * Creates an exception for parallel processing issues with concurrency guidance.
     *
     * @param threadCount the number of threads attempted
     * @param availableCores the number of available CPU cores
     * @param operation the operation being performed
     * @return a new InvalidArgumentException with parallelization suggestions
     */
    public static InvalidArgumentException parallelProcessingError(
            int threadCount, int availableCores, String operation) {
        String message = String.format(
            "Parallel processing configuration error for %s: %d threads requested on %d-core system. " +
            "Optimization suggestions: " +
            "1) Use thread count ≤ CPU cores (%d) for CPU-bound tasks; " +
            "2) For batch processing: optimal batch size = cores × 2-4; " +
            "3) Enable ForkJoinPool for recursive decomposition; " +
            "4) Consider signal size: parallel overhead not worth it for small signals; " +
            "5) Use sequential processing as fallback for reliability.",
            operation, threadCount, availableCores, availableCores);
        
        return new InvalidArgumentException(ErrorCode.CFG_THREAD_COUNT_INVALID, message);
    }
}