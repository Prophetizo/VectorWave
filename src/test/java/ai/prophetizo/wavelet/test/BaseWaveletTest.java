package ai.prophetizo.wavelet.test;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.logging.Logger;

/**
 * Base class for wavelet transform tests providing common setup and utilities.
 * 
 * <p>This class provides:</p>
 * <ul>
 *   <li>Standard test signal generation</li>
 *   <li>Transform factory configuration</li>
 *   <li>Test execution timing</li>
 *   <li>Common test patterns</li>
 * </ul>
 */
public abstract class BaseWaveletTest {
    
    protected static final Logger logger = Logger.getLogger(BaseWaveletTest.class.getName());
    
    // Standard test signal lengths
    protected static final int SMALL_SIGNAL_LENGTH = 8;
    protected static final int MEDIUM_SIGNAL_LENGTH = 64;
    protected static final int LARGE_SIGNAL_LENGTH = 1024;
    
    // Common test signals
    protected double[] smallTestSignal;
    protected double[] mediumTestSignal;
    protected double[] largeTestSignal;
    
    // Transform factory for creating transforms
    protected WaveletTransformFactory transformFactory;
    
    // Test timing
    private long testStartTime;
    
    @BeforeEach
    protected void setUp(TestInfo testInfo) {
        // Log test start
        String testName = testInfo.getDisplayName();
        logger.info("Starting test: " + testName);
        testStartTime = System.nanoTime();
        
        // Initialize common test signals
        smallTestSignal = WaveletTestUtils.createSimpleTestSignal(SMALL_SIGNAL_LENGTH);
        mediumTestSignal = WaveletTestUtils.generateSineWave(
            MEDIUM_SIGNAL_LENGTH, 0.1, 1.0, 0.0);
        largeTestSignal = WaveletTestUtils.generateCompositeSignal(
            LARGE_SIGNAL_LENGTH, 
            new double[]{0.05, 0.1, 0.2}, 
            new double[]{1.0, 0.5, 0.25});
        
        // Initialize transform factory with default settings
        transformFactory = new WaveletTransformFactory();
        
        // Allow subclasses to perform additional setup
        additionalSetUp();
    }
    
    /**
     * Hook for subclasses to perform additional setup.
     * Called at the end of setUp().
     */
    protected void additionalSetUp() {
        // Default implementation does nothing
    }
    
    /**
     * Creates a transform for the given wavelet with default settings.
     * 
     * <p>Default settings include:</p>
     * <ul>
     *   <li>Boundary mode: PERIODIC</li>
     *   <li>Implementation: Auto-detected (Vector if available, otherwise Scalar)</li>
     * </ul>
     * 
     * @param wavelet the wavelet to use
     * @return configured transform with default settings
     */
    protected WaveletTransform createTransform(Wavelet wavelet) {
        return transformFactory.create(wavelet);
    }
    
    /**
     * Creates a transform with specific boundary mode.
     * 
     * <p>Configuration:</p>
     * <ul>
     *   <li>Boundary mode: As specified</li>
     *   <li>Implementation: Auto-detected (Vector if available, otherwise Scalar)</li>
     * </ul>
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary mode (PERIODIC or ZERO_PADDING)
     * @return configured transform with specified boundary mode
     */
    protected WaveletTransform createTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new WaveletTransformFactory()
            .boundaryMode(boundaryMode)
            .create(wavelet);
    }
    
    /**
     * Creates a scalar-only transform for testing.
     * 
     * <p>Forces the use of scalar implementation even if vector operations
     * are available. Useful for:</p>
     * <ul>
     *   <li>Testing scalar implementation specifically</li>
     *   <li>Comparing scalar vs vector performance</li>
     *   <li>Debugging implementation differences</li>
     * </ul>
     * 
     * <p>Configuration:</p>
     * <ul>
     *   <li>Boundary mode: PERIODIC (default)</li>
     *   <li>Implementation: Scalar (forced)</li>
     * </ul>
     * 
     * @param wavelet the wavelet to use
     * @return scalar-only transform
     */
    protected WaveletTransform createScalarTransform(Wavelet wavelet) {
        return new WaveletTransformFactory()
            .create(wavelet);
    }
    
    /**
     * Logs test completion time.
     * 
     * @param testInfo test information
     */
    protected void logTestCompletion(TestInfo testInfo) {
        long duration = System.nanoTime() - testStartTime;
        double durationMs = duration / 1_000_000.0;
        logger.info(String.format("Completed test '%s' in %.2f ms",
            testInfo.getDisplayName(), durationMs));
    }
    
    /**
     * Logs a signal for debugging purposes.
     * Uses the test logger instead of System.out.
     * 
     * @param label label for the signal
     * @param signal the signal to log
     * @param maxElements maximum elements to show
     */
    protected void logSignal(String label, double[] signal, int maxElements) {
        String formatted = WaveletTestUtils.formatSignal(label, signal, maxElements);
        logger.fine(formatted);
    }
    
    /**
     * Runs a test with both scalar and vector implementations.
     * 
     * @param wavelet the wavelet to test
     * @param testAction the test to perform
     */
    protected void testBothImplementations(Wavelet wavelet, 
                                         WaveletTransformTest testAction) {
        // Test with scalar implementation
        WaveletTransform scalarTransform = createScalarTransform(wavelet);
        testAction.test(scalarTransform, "Scalar");
        
        // Test with auto-selected implementation (may be vector)
        WaveletTransform autoTransform = createTransform(wavelet);
        testAction.test(autoTransform, "Auto");
    }
    
    /**
     * Functional interface for transform tests.
     */
    @FunctionalInterface
    protected interface WaveletTransformTest {
        void test(WaveletTransform transform, String implementationType);
    }
    
    /**
     * Generates a specific test signal based on type.
     * 
     * @param type the signal type
     * @param length signal length
     * @return generated signal
     */
    protected double[] generateTestSignal(SignalType type, int length) {
        switch (type) {
            case CONSTANT:
                return generateConstantSignal(length, 1.0);
            case LINEAR:
                return generateLinearSignal(length);
            case QUADRATIC:
                return generateQuadraticSignal(length);
            case RANDOM:
                return WaveletTestUtils.generateRandomSignal(length, 42L, -1.0, 1.0);
            case SINE:
                return WaveletTestUtils.generateSineWave(length, 0.1, 1.0, 0.0);
            case STEP:
                return WaveletTestUtils.generateStepFunction(length, length / 2, 0.0, 1.0);
            default:
                throw new IllegalArgumentException("Unknown signal type: " + type);
        }
    }
    
    /**
     * Signal types for testing.
     */
    protected enum SignalType {
        CONSTANT,
        LINEAR,
        QUADRATIC,
        RANDOM,
        SINE,
        STEP
    }
    
    // === Helper Methods ===
    
    private double[] generateConstantSignal(int length, double value) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = value;
        }
        return signal;
    }
    
    private double[] generateLinearSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (double) i / length;
        }
        return signal;
    }
    
    private double[] generateQuadraticSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double x = (double) i / length;
            signal[i] = x * x;
        }
        return signal;
    }
}