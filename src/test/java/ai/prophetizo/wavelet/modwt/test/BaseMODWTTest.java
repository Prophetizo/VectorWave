package ai.prophetizo.wavelet.modwt.test;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTTransformFactory;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.test.WaveletTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.logging.Logger;

/**
 * Base class for MODWT transform tests providing common setup and utilities.
 * 
 * <p>This class provides:</p>
 * <ul>
 *   <li>Standard test signal generation (any length, not just power-of-2)</li>
 *   <li>MODWT transform factory configuration</li>
 *   <li>Test execution timing</li>
 *   <li>Common test patterns for MODWT</li>
 * </ul>
 * 
 * <p>Key differences from DWT base test:</p>
 * <ul>
 *   <li>Supports arbitrary signal lengths</li>
 *   <li>Tests shift-invariant properties</li>
 *   <li>Includes multi-level MODWT testing</li>
 * </ul>
 * 
 * @since 3.0.0
 */
public abstract class BaseMODWTTest {
    
    protected static final Logger logger = Logger.getLogger(BaseMODWTTest.class.getName());
    
    // Standard test signal lengths (including non-power-of-2)
    protected static final int SMALL_SIGNAL_LENGTH = 10;  // Non-power-of-2
    protected static final int MEDIUM_SIGNAL_LENGTH = 100;  // Non-power-of-2
    protected static final int LARGE_SIGNAL_LENGTH = 1000;  // Non-power-of-2
    protected static final int POWER_OF_TWO_LENGTH = 1024;  // For comparison
    
    // Common test signals
    protected double[] smallTestSignal;
    protected double[] mediumTestSignal;
    protected double[] largeTestSignal;
    protected double[] powerOfTwoSignal;
    
    // Transform factories for creating MODWT transforms
    protected MODWTTransformFactory transformFactory;
    
    // Test timing
    private long testStartTime;
    
    @BeforeEach
    protected void setUp(TestInfo testInfo) {
        // Log test start
        String testName = testInfo.getDisplayName();
        logger.info("Starting MODWT test: " + testName);
        testStartTime = System.nanoTime();
        
        // Initialize common test signals (including non-power-of-2 lengths)
        smallTestSignal = createTestSignal(SMALL_SIGNAL_LENGTH);
        mediumTestSignal = WaveletTestUtils.generateSineWave(
            MEDIUM_SIGNAL_LENGTH, 0.1, 1.0, 0.0);
        largeTestSignal = WaveletTestUtils.generateCompositeSignal(
            LARGE_SIGNAL_LENGTH, 
            new double[]{0.05, 0.1, 0.2}, 
            new double[]{1.0, 0.5, 0.25});
        powerOfTwoSignal = WaveletTestUtils.generateCompositeSignal(
            POWER_OF_TWO_LENGTH,
            new double[]{0.05, 0.1, 0.2},
            new double[]{1.0, 0.5, 0.25});
        
        // Initialize MODWT transform factory with default settings
        transformFactory = new MODWTTransformFactory();
        
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
     * Creates a MODWT transform for the given wavelet with default settings.
     * 
     * <p>Default settings include:</p>
     * <ul>
     *   <li>Boundary mode: PERIODIC</li>
     *   <li>Implementation: Auto-detected (Vector if available, otherwise Scalar)</li>
     * </ul>
     * 
     * @param wavelet the wavelet to use
     * @return configured MODWT transform with default settings
     */
    protected MODWTTransform createTransform(Wavelet wavelet) {
        return transformFactory.create(wavelet);
    }
    
    /**
     * Creates a MODWT transform with specific boundary mode.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary mode
     * @return configured MODWT transform
     */
    protected MODWTTransform createTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new MODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Creates a multi-level MODWT transform.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary mode
     * @return multi-level MODWT transform
     */
    protected MultiLevelMODWTTransform createMultiLevelTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new MultiLevelMODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Creates a simple test signal of given length.
     * Unlike DWT tests, this works with any length.
     * 
     * @param length the signal length (can be any positive value)
     * @return test signal
     */
    protected double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1;  // Simple ascending sequence
        }
        return signal;
    }
    
    /**
     * Tests shift-invariant property of MODWT.
     * This is a key differentiator from DWT.
     * 
     * @param transform the MODWT transform to test
     * @param signal the test signal
     * @param shift the circular shift amount
     * @param tolerance the acceptable difference tolerance
     * @return true if shift-invariant within tolerance
     */
    protected boolean testShiftInvariance(MODWTTransform transform, double[] signal, 
                                        int shift, double tolerance) {
        // Transform original signal
        var result1 = transform.forward(signal);
        
        // Create shifted signal
        double[] shiftedSignal = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            shiftedSignal[i] = signal[(i - shift + signal.length) % signal.length];
        }
        
        // Transform shifted signal
        var result2 = transform.forward(shiftedSignal);
        
        // Check if coefficients are circularly shifted versions
        double[] approx1 = result1.approximationCoeffs();
        double[] approx2 = result2.approximationCoeffs();
        double[] detail1 = result1.detailCoeffs();
        double[] detail2 = result2.detailCoeffs();
        
        // Compare shifted coefficients
        double maxDiffApprox = 0.0;
        double maxDiffDetail = 0.0;
        
        for (int i = 0; i < signal.length; i++) {
            int shiftedIdx = (i - shift + signal.length) % signal.length;
            maxDiffApprox = Math.max(maxDiffApprox, 
                Math.abs(approx1[i] - approx2[shiftedIdx]));
            maxDiffDetail = Math.max(maxDiffDetail, 
                Math.abs(detail1[i] - detail2[shiftedIdx]));
        }
        
        return maxDiffApprox < tolerance && maxDiffDetail < tolerance;
    }
    
    /**
     * Logs test completion time.
     * 
     * @param testInfo test information
     */
    protected void logTestCompletion(TestInfo testInfo) {
        long duration = System.nanoTime() - testStartTime;
        double durationMs = duration / 1_000_000.0;
        logger.info(String.format("Completed test %s in %.2f ms", 
            testInfo.getDisplayName(), durationMs));
    }
    
    /**
     * Gets the MODWT transform factory.
     * 
     * @return the transform factory
     */
    protected MODWTTransformFactory getTransformFactory() {
        return transformFactory;
    }
}