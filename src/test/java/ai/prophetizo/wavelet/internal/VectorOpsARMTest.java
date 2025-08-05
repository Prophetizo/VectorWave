package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.util.PlatformDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ARM-specific vector operations.
 */
class VectorOpsARMTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolvePeriodicARM_Haar() {
        // Haar wavelet filter
        double[] filter = new Haar().lowPassDecomposition();
        
        // Simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolvePeriodicARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolvePeriodic(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard implementation");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolvePeriodicARM_DB4() {
        // Daubechies-4 wavelet filter
        double[] filter = Daubechies.DB4.lowPassDecomposition();
        
        // Test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolvePeriodicARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolvePeriodic(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard implementation for DB4");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolveZeroPaddingARM_Haar() {
        // Haar wavelet filter
        double[] filter = new Haar().lowPassDecomposition();
        
        // Simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolveZeroPadding(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM zero-padding implementation should match standard implementation");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolveZeroPaddingARM_LargeSignal() {
        // Test with larger random signal
        Random random = new Random(42);
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        double[] filter = Daubechies.DB4.lowPassDecomposition();
        
        // Compare implementations
        double[] resultARM = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                signal, filter, signal.length, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolveZeroPadding(
                signal, filter, signal.length, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard for large signals");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testConvolveAndDownsampleARM_Consistency() {
        // Test that ARM downsampling is consistent with existing implementation
        double[] signal = new double[128];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        double[] filter = new Haar().lowPassDecomposition();
        
        double[] resultARM = VectorOpsARM.convolveAndDownsampleARM(
                signal, filter, signal.length, filter.length);
        double[] resultStandard = VectorOps.convolveAndDownsamplePeriodic(
                signal, filter, signal.length, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM downsampling should match standard implementation");
    }
    
    @Test
    void testPlatformDetection() {
        System.out.println("Platform Information:");
        System.out.println("  OS Arch: " + System.getProperty("os.arch"));
        System.out.println("  Is ARM: " + PlatformDetector.isARM());
        System.out.println("  Is Apple Silicon: " + PlatformDetector.isAppleSilicon());
        
        // This test always passes, just prints platform info
        assertTrue(true);
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testEdgeCases() {
        // Test with single element
        double[] singleElement = {5.0};
        double[] filter = new Haar().lowPassDecomposition();
        
        assertDoesNotThrow(() -> {
            VectorOpsARM.upsampleAndConvolvePeriodicARM(
                    singleElement, filter, 1, filter.length);
            VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                    singleElement, filter, 1, filter.length);
        });
        
        // Test with null inputs
        assertThrows(IllegalArgumentException.class, () ->
                VectorOpsARM.upsampleAndConvolvePeriodicARM(
                        null, filter, 1, filter.length));
        assertThrows(IllegalArgumentException.class, () ->
                VectorOpsARM.upsampleAndConvolvePeriodicARM(
                        singleElement, null, 1, filter.length));
    }
    
    // Helper method for conditional test execution
    static boolean isARMPlatform() {
        return PlatformDetector.isARM();
    }
}