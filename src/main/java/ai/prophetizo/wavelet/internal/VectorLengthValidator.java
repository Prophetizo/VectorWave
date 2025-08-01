package ai.prophetizo.wavelet.internal;

import jdk.incubator.vector.VectorSpecies;

/**
 * Utility class for validating SIMD vector lengths across the library.
 * Ensures consistent validation logic and configuration.
 */
public final class VectorLengthValidator {
    
    // Minimum vector length for meaningful SIMD operations
    private static final int MIN_VECTOR_LENGTH = 1;
    
    // Maximum expected vector length (can be overridden via system property)
    private static final int MAX_VECTOR_LENGTH = Integer.getInteger(
        "ai.prophetizo.wavelet.max.vector.length", 16
    );
    
    private VectorLengthValidator() {
        // Utility class
    }
    
    /**
     * Validates that the vector length is within expected bounds.
     * 
     * @param vectorLength the vector length to validate
     * @param context additional context for error messages (e.g., class name)
     * @throws IllegalStateException if vector length is outside expected bounds
     */
    public static void validateVectorLength(int vectorLength, String context) {
        if (vectorLength < MIN_VECTOR_LENGTH || vectorLength > MAX_VECTOR_LENGTH) {
            throw new IllegalStateException(String.format(
                "%s: Unexpected vector length: %d (expected %d-%d doubles). Platform: %s. " +
                "Override with -Dvectorwave.max.vector.length=N",
                context, vectorLength, MIN_VECTOR_LENGTH, MAX_VECTOR_LENGTH, 
                System.getProperty("os.arch")
            ));
        }
    }
    
    /**
     * Gets the minimum expected vector length.
     */
    public static int getMinVectorLength() {
        return MIN_VECTOR_LENGTH;
    }
    
    /**
     * Gets the maximum expected vector length.
     */
    public static int getMaxVectorLength() {
        return MAX_VECTOR_LENGTH;
    }
}