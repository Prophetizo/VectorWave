package ai.prophetizo.wavelet.test;

/**
 * Common constants used across test files to ensure consistency
 * and make test intentions clearer.
 */
public final class TestConstants {
    
    /**
     * Default random seed for tests requiring deterministic behavior.
     * Using a fixed seed ensures reproducible test results across runs.
     * The value 42 is a cultural reference to "The Hitchhiker's Guide to the Galaxy"
     * where it's the "Answer to the Ultimate Question of Life, the Universe, and Everything".
     */
    public static final long TEST_SEED = 42L;
    
    /**
     * Default tolerance for floating-point comparisons in tests.
     * This value is suitable for most double-precision calculations.
     */
    public static final double DEFAULT_TOLERANCE = 1e-10;
    
    /**
     * Tolerance for tests involving more complex calculations or
     * accumulated floating-point errors.
     */
    public static final double RELAXED_TOLERANCE = 1e-8;
    
    /**
     * Timeout duration in seconds for tests that wait for asynchronous operations.
     */
    public static final int ASYNC_TIMEOUT_SECONDS = 5;
    
    /**
     * Common test signal sizes for FFT and wavelet transform tests.
     */
    public static final int[] POWER_OF_TWO_SIZES = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};
    
    /**
     * Common non-power-of-two sizes for testing arbitrary-size algorithms.
     */
    public static final int[] NON_POWER_OF_TWO_SIZES = {3, 5, 7, 11, 13, 17, 31, 100, 127};
    
    private TestConstants() {
        // Prevent instantiation
    }
}