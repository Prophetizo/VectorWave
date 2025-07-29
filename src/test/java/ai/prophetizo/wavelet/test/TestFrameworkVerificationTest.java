package ai.prophetizo.wavelet.test;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.util.ToleranceConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test to ensure the testing framework is properly configured.
 * 
 * <p>This test class verifies that:</p>
 * <ul>
 *   <li>JUnit 5 is working correctly</li>
 *   <li>Test utilities are accessible</li>
 *   <li>Assertions work as expected</li>
 *   <li>Base test class functionality works</li>
 * </ul>
 */
@DisplayName("Testing Framework Verification")
class TestFrameworkVerificationTest extends BaseWaveletTest {
    
    @Test
    @DisplayName("JUnit 5 basic assertion test")
    void testJUnit5Assertions() {
        assertTrue(true, "Basic assertTrue");
        assertEquals(4, 2 + 2, "Basic assertEquals");
        assertNotNull(new Object(), "Basic assertNotNull");
    }
    
    @Test
    @DisplayName("WaveletTestUtils functionality")
    void testWaveletTestUtils() {
        // Test signal generation
        double[] sineWave = WaveletTestUtils.generateSineWave(64, 0.1, 1.0, 0.0);
        assertNotNull(sineWave);
        assertEquals(64, sineWave.length);
        
        // Test energy computation
        double energy = WaveletTestUtils.computeEnergy(new double[]{3.0, 4.0});
        assertEquals(25.0, energy, 1e-10); // 3² + 4² = 9 + 16 = 25
        
        // Test array comparison
        double[] arr1 = {1.0, 2.0, 3.0};
        double[] arr2 = {1.0, 2.0000001, 3.0};
        assertTrue(WaveletTestUtils.arraysEqualWithTolerance(arr1, arr2, 1e-6));
        assertFalse(WaveletTestUtils.arraysEqualWithTolerance(arr1, arr2, 1e-8));
    }
    
    @Test
    @DisplayName("WaveletAssertions functionality")
    void testWaveletAssertions() {
        double[] signal1 = {1.0, 2.0, 3.0, 4.0};
        double[] signal2 = {1.0, 2.0, 3.0, 4.0};
        
        // Test array assertions
        WaveletAssertions.assertArraysEqualWithTolerance(
            signal1, signal2, 1e-10, "Arrays should be equal");
        
        // Test finite values assertion
        WaveletAssertions.assertAllFinite(signal1, "Test signal");
        
        // Test signal length assertion
        double[] powerOfTwoSignal = new double[8];
        WaveletAssertions.assertSignalLength(powerOfTwoSignal, 8);
    }
    
    @Test
    @DisplayName("BaseWaveletTest setup and utilities")
    void testBaseTestFunctionality() {
        // Verify test signals are initialized
        assertNotNull(smallTestSignal);
        assertNotNull(mediumTestSignal);
        assertNotNull(largeTestSignal);
        
        assertEquals(SMALL_SIGNAL_LENGTH, smallTestSignal.length);
        assertEquals(MEDIUM_SIGNAL_LENGTH, mediumTestSignal.length);
        assertEquals(LARGE_SIGNAL_LENGTH, largeTestSignal.length);
        
        // Test transform creation
        WaveletTransform transform = createTransform(new Haar());
        assertNotNull(transform);
        
        // Test signal generation
        double[] constantSignal = generateTestSignal(SignalType.CONSTANT, 16);
        assertEquals(16, constantSignal.length);
        for (double value : constantSignal) {
            assertEquals(1.0, value, 1e-10);
        }
    }
    
    @Test
    @DisplayName("End-to-end transform test with assertions")
    void testEndToEndWithAssertions() {
        // Create a simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 4.0, 3.0, 2.0};
        
        // Create transform
        WaveletTransform transform = createTransform(new Haar());
        
        // Perform forward transform
        TransformResult result = transform.forward(signal);
        WaveletAssertions.assertValidTransformResult(result);
        
        // Check coefficient lengths
        WaveletAssertions.assertCoefficientsLength(result, signal.length / 2);
        
        // Test energy preservation
        WaveletAssertions.assertEnergyPreserved(
            signal, result, ToleranceConstants.ENERGY_TOLERANCE);
        
        // Test reconstruction
        double[] reconstructed = transform.inverse(result);
        WaveletAssertions.assertPerfectReconstruction(
            signal, reconstructed, ToleranceConstants.DEFAULT_TOLERANCE);
    }
    
    @Test
    @DisplayName("Test both implementations pattern")
    void testBothImplementationsPattern() {
        final int[] callCount = {0};
        
        testBothImplementations(new Haar(), (transform, implType) -> {
            callCount[0]++;
            assertNotNull(transform);
            assertNotNull(implType);
            
            // Perform a simple transform
            double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
            TransformResult result = transform.forward(signal);
            assertNotNull(result);
        });
        
        // Should be called twice (scalar and auto)
        assertEquals(2, callCount[0]);
    }
}