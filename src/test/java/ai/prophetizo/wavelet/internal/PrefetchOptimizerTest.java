package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Tests for the PrefetchOptimizer class.
 */
class PrefetchOptimizerTest {
    
    @Test
    void testPrefetchInfoNotNull() {
        String info = PrefetchOptimizer.getPrefetchInfo();
        assertNotNull(info);
        assertTrue(info.contains("Prefetch Support"));
        assertTrue(info.contains("Architecture"));
        assertTrue(info.contains("Cache Line Size"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {128, 255, 256, 512, 1024, 2048})
    void testPrefetchBeneficial(int signalLength) {
        boolean result = PrefetchOptimizer.isPrefetchBeneficial(signalLength);
        
        // Should return false for signals < 256, true for >= 256
        if (signalLength < 256) {
            assertFalse(result, "Small signals should not benefit from prefetch");
        } else {
            // For larger signals (>= 256), prefetch benefit depends on platform support
            // The method should return a consistent value for the same platform
            boolean firstCall = PrefetchOptimizer.isPrefetchBeneficial(signalLength);
            boolean secondCall = PrefetchOptimizer.isPrefetchBeneficial(signalLength);
            assertEquals(firstCall, secondCall, 
                "isPrefetchBeneficial should return consistent results for the same input");
        }
    }
    
    @Test
    void testConvolveAndDownsampleWithPrefetch() {
        // Test with various signal sizes
        int[] sizes = {256, 512, 1024, 2048};
        Wavelet wavelet = WaveletRegistry.getWavelet("db4");
        double[] filter = wavelet.lowPassDecomposition();
        
        Random random = new Random(TestConstants.TEST_SEED);
        
        for (int size : sizes) {
            // Create test signal
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = random.nextDouble();
            }
            
            // Compute with standard method
            double[] expectedOutput = new double[size / 2];
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, expectedOutput);
            
            // Compute with prefetch method
            double[] actualOutput = new double[size / 2];
            PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(signal, filter, actualOutput);
            
            // Verify results match
            assertArrayEquals(expectedOutput, actualOutput, 1e-10, 
                "Prefetch optimization should produce identical results for size " + size);
        }
    }
    
    @Test
    void testPrefetchWithSmallSignal() {
        // Test that small signals fall back to standard implementation
        double[] signal = new double[128]; // Below threshold
        double[] filter = {0.5, 0.5}; // Simple filter
        double[] output = new double[64];
        
        Arrays.fill(signal, 1.0);
        
        // This should use the standard ScalarOps implementation
        PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(signal, filter, output);
        
        // Verify output
        for (double val : output) {
            assertEquals(1.0, val, 1e-10, "Simple convolution should produce 1.0");
        }
    }
    
    @Test
    void testPrefetchForConvolution() {
        // Test that prefetch methods don't crash
        double[] signal = new double[1024];
        
        // These should complete without error
        for (int i = 0; i < 100; i += 10) {
            PrefetchOptimizer.prefetchForConvolution(signal, i, 4);
            PrefetchOptimizer.prefetchForConvolution(signal, i, 8);
        }
        
        // Test edge cases
        PrefetchOptimizer.prefetchForConvolution(signal, 1020, 4);
        PrefetchOptimizer.prefetchForConvolution(signal, 0, 16);
    }
    
    @Test
    void testPrefetchForUpsampling() {
        double[] coeffs = new double[512];
        
        // Test various positions
        for (int i = 0; i < coeffs.length; i += 50) {
            PrefetchOptimizer.prefetchForUpsampling(coeffs, i);
        }
        
        // Test boundary conditions
        PrefetchOptimizer.prefetchForUpsampling(coeffs, 0);
        PrefetchOptimizer.prefetchForUpsampling(coeffs, coeffs.length - 1);
        PrefetchOptimizer.prefetchForUpsampling(coeffs, coeffs.length - 10);
    }
    
    @Test
    void testPrefetchForCombinedTransform() {
        double[] signal = new double[2048];
        int[] filterLengths = {2, 4, 8, 16, 20};
        
        for (int filterLen : filterLengths) {
            for (int outputIdx = 0; outputIdx < signal.length / 2; outputIdx += 100) {
                PrefetchOptimizer.prefetchForCombinedTransform(signal, outputIdx, filterLen);
            }
        }
    }
    
    @Test
    void testPrefetchCorrectness() {
        // Comprehensive test comparing results with and without prefetch
        Wavelet[] wavelets = {
            WaveletRegistry.getWavelet("haar"),
            WaveletRegistry.getWavelet("db2"),
            WaveletRegistry.getWavelet("db4"),
            WaveletRegistry.getWavelet("sym4")
        };
        
        Random random = new Random(123);
        
        for (Wavelet wavelet : wavelets) {
            double[] lowFilter = wavelet.lowPassDecomposition();
            double[] highFilter = wavelet.highPassDecomposition();
            
            // Test multiple sizes
            for (int size = 256; size <= 2048; size *= 2) {
                // Create random signal
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = random.nextGaussian();
                }
                
                // Standard convolution
                double[] standardLow = new double[size / 2];
                double[] standardHigh = new double[size / 2];
                
                // Create a copy of ScalarOps behavior without prefetch
                for (int i = 0; i < size / 2; i++) {
                    double sumLow = 0.0;
                    double sumHigh = 0.0;
                    int kStart = 2 * i;
                    
                    for (int j = 0; j < lowFilter.length; j++) {
                        int idx = (kStart + j) % size;
                        sumLow += signal[idx] * lowFilter[j];
                        sumHigh += signal[idx] * highFilter[j];
                    }
                    
                    standardLow[i] = sumLow;
                    standardHigh[i] = sumHigh;
                }
                
                // Prefetch convolution
                double[] prefetchLow = new double[size / 2];
                double[] prefetchHigh = new double[size / 2];
                PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(signal, lowFilter, prefetchLow);
                PrefetchOptimizer.convolveAndDownsamplePeriodicWithPrefetch(signal, highFilter, prefetchHigh);
                
                // Compare results
                assertArrayEquals(standardLow, prefetchLow, 1e-10,
                    String.format("Low-pass mismatch for %s at size %d", wavelet.name(), size));
                assertArrayEquals(standardHigh, prefetchHigh, 1e-10,
                    String.format("High-pass mismatch for %s at size %d", wavelet.name(), size));
            }
        }
    }
}