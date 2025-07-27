package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CWTVectorOpsTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double RELAXED_TOLERANCE = 1e-6; // Due to additional floating-point operations in complex arithmetic
    private CWTVectorOps vectorOps;
    private double[] testSignal;
    private double[] testWavelet;
    
    @BeforeEach
    void setUp() {
        vectorOps = new CWTVectorOps();
        
        // Create test signal
        testSignal = new double[256];
        for (int i = 0; i < testSignal.length; i++) {
            testSignal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Create test wavelet (simplified Morlet)
        testWavelet = new double[32];
        double sigma = 1.0;
        for (int i = 0; i < testWavelet.length; i++) {
            double t = (i - testWavelet.length / 2.0) / 4.0;
            testWavelet[i] = Math.exp(-0.5 * t * t / (sigma * sigma)) * Math.cos(6.0 * t);
        }
    }
    
    @Test
    @DisplayName("Should compute convolution using SIMD")
    void testConvolveSIMD() {
        // Given
        double scale = 2.0;
        
        // When
        double[] result = vectorOps.convolve(testSignal, testWavelet, scale);
        
        // Then
        assertNotNull(result);
        assertEquals(testSignal.length, result.length);
        
        // Verify result is non-trivial
        double sum = 0;
        for (double v : result) {
            sum += Math.abs(v);
        }
        assertTrue(sum > 0, "Convolution result should be non-zero");
    }
    
    @Test
    @DisplayName("Should match scalar convolution results")
    void testConvolveMatchesScalar() {
        // Given
        double scale = 1.0;
        double[] scalarResult = scalarConvolve(testSignal, testWavelet, scale);
        
        // When
        double[] vectorResult = vectorOps.convolve(testSignal, testWavelet, scale);
        
        // Then
        assertArrayEquals(scalarResult, vectorResult, RELAXED_TOLERANCE);
    }
    
    @Test
    @DisplayName("Should compute complex convolution")
    void testComplexConvolve() {
        // Given
        double[] realSignal = testSignal;
        double[] imagSignal = new double[testSignal.length]; // Zero imaginary part
        double[] realWavelet = testWavelet;
        double[] imagWavelet = new double[testWavelet.length];
        
        // Add imaginary component to wavelet
        for (int i = 0; i < testWavelet.length; i++) {
            double t = (i - testWavelet.length / 2.0) / 4.0;
            imagWavelet[i] = Math.exp(-0.5 * t * t) * Math.sin(6.0 * t);
        }
        
        double scale = 2.0;
        
        // When
        ComplexMatrix result = vectorOps.complexConvolve(
            realSignal, imagSignal, realWavelet, imagWavelet, scale);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getRows());
        assertEquals(testSignal.length, result.getCols());
        
        // Verify non-trivial result
        double magnitudeSum = 0;
        for (int i = 0; i < testSignal.length; i++) {
            magnitudeSum += result.getMagnitude(0, i);
        }
        assertTrue(magnitudeSum > 0, "Complex convolution should produce non-zero results");
    }
    
    @Test
    @DisplayName("Should compute multi-scale transform")
    void testMultiScaleTransform() {
        // Given
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        double[][] coefficients = vectorOps.computeMultiScale(
            testSignal, scales, wavelet);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(scales.length, coefficients.length);
        assertEquals(testSignal.length, coefficients[0].length);
        
        // Verify each scale produces different results
        for (int s = 1; s < scales.length; s++) {
            boolean different = false;
            for (int t = 0; t < testSignal.length; t++) {
                if (Math.abs(coefficients[s][t] - coefficients[0][t]) > TOLERANCE) {
                    different = true;
                    break;
                }
            }
            assertTrue(different, "Different scales should produce different coefficients");
        }
    }
    
    @Test
    @DisplayName("Should normalize coefficients by scale")
    void testScaleNormalization() {
        // Given
        double[] scales = {1.0, 2.0, 4.0};
        double[][] coefficients = new double[scales.length][testSignal.length];
        
        // Initialize with same values
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < testSignal.length; t++) {
                coefficients[s][t] = 1.0;
            }
        }
        
        // When
        vectorOps.normalizeByScale(coefficients, scales);
        
        // Then
        for (int s = 0; s < scales.length; s++) {
            double expectedValue = 1.0 / Math.sqrt(scales[s]);
            for (int t = 0; t < testSignal.length; t++) {
                assertEquals(expectedValue, coefficients[s][t], TOLERANCE);
            }
        }
    }
    
    @Test
    @DisplayName("Should compute magnitude from complex coefficients")
    void testMagnitudeComputation() {
        // Given
        ComplexMatrix complex = new ComplexMatrix(2, 4);
        complex.set(0, 0, 3.0, 4.0);  // |3+4i| = 5
        complex.set(0, 1, 5.0, 12.0); // |5+12i| = 13
        complex.set(1, 0, 8.0, 15.0); // |8+15i| = 17
        complex.set(1, 1, 7.0, 24.0); // |7+24i| = 25
        
        // When
        double[][] magnitude = vectorOps.computeMagnitude(complex);
        
        // Then
        assertEquals(5.0, magnitude[0][0], TOLERANCE);
        assertEquals(13.0, magnitude[0][1], TOLERANCE);
        assertEquals(17.0, magnitude[1][0], TOLERANCE);
        assertEquals(25.0, magnitude[1][1], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle boundary conditions with padding")
    void testBoundaryHandling() {
        // Given
        double[] shortSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 4.0, 3.0, 2.0};
        double[] wavelet = {0.25, 0.5, 0.25}; // Simple averaging kernel
        
        // When - test different padding modes
        double[] zeroPadded = vectorOps.convolveWithPadding(
            shortSignal, wavelet, 1.0, CWTVectorOps.PaddingMode.ZERO);
        double[] reflectPadded = vectorOps.convolveWithPadding(
            shortSignal, wavelet, 1.0, CWTVectorOps.PaddingMode.REFLECT);
        double[] periodicPadded = vectorOps.convolveWithPadding(
            shortSignal, wavelet, 1.0, CWTVectorOps.PaddingMode.PERIODIC);
        
        // Then
        assertNotNull(zeroPadded);
        assertNotNull(reflectPadded);
        assertNotNull(periodicPadded);
        assertEquals(shortSignal.length, zeroPadded.length);
        
        // Verify different padding produces different edge results
        assertNotEquals(zeroPadded[0], reflectPadded[0], TOLERANCE);
        assertNotEquals(zeroPadded[0], periodicPadded[0], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should optimize for different signal sizes")
    void testAdaptiveOptimization() {
        // Given
        double[] smallSignal = new double[64];
        double[] mediumSignal = new double[512];
        double[] largeSignal = new double[4096];
        
        Random rand = new Random(42);
        fillRandom(smallSignal, rand);
        fillRandom(mediumSignal, rand);
        fillRandom(largeSignal, rand);
        
        // When - should select appropriate strategy
        CWTVectorOps.OptimizationStrategy smallStrategy = 
            vectorOps.selectStrategy(smallSignal.length, testWavelet.length);
        CWTVectorOps.OptimizationStrategy mediumStrategy = 
            vectorOps.selectStrategy(mediumSignal.length, testWavelet.length);
        CWTVectorOps.OptimizationStrategy largeStrategy = 
            vectorOps.selectStrategy(largeSignal.length, testWavelet.length);
        
        // Then
        assertNotNull(smallStrategy);
        assertNotNull(mediumStrategy);
        assertNotNull(largeStrategy);
        
        // For small signals, might prefer direct computation
        // For large signals, might prefer FFT or blocked approach
        assertTrue(smallStrategy.useDirectComputation() || 
                  mediumStrategy.useBlockedComputation() || 
                  largeStrategy.useFFT());
    }
    
    @Test
    @DisplayName("Should compute power spectrum efficiently")
    void testPowerSpectrumComputation() {
        // Given
        double[][] coefficients = new double[3][128];
        for (int s = 0; s < 3; s++) {
            for (int t = 0; t < 128; t++) {
                coefficients[s][t] = Math.sin(2 * Math.PI * t / 16.0) * (s + 1);
            }
        }
        
        // When
        double[][] power = vectorOps.computePowerSpectrum(coefficients);
        
        // Then
        assertNotNull(power);
        assertEquals(coefficients.length, power.length);
        assertEquals(coefficients[0].length, power[0].length);
        
        // Verify power = magnitude²
        for (int s = 0; s < coefficients.length; s++) {
            for (int t = 0; t < coefficients[0].length; t++) {
                double expected = coefficients[s][t] * coefficients[s][t];
                assertEquals(expected, power[s][t], TOLERANCE);
            }
        }
    }
    
    @Test
    @DisplayName("Should support streaming computation")
    void testStreamingComputation() {
        // Given
        int windowSize = 64;
        int hopSize = 32;
        double[] scales = {1.0, 2.0, 4.0};
        
        CWTVectorOps.StreamingContext context = 
            vectorOps.createStreamingContext(windowSize, hopSize, scales);
        
        // When - process signal in chunks
        int numChunks = testSignal.length / hopSize;
        boolean hasProcessedAtLeastOneWindow = false;
        
        for (int i = 0; i < numChunks; i++) {
            int start = i * hopSize;
            int end = Math.min(start + hopSize, testSignal.length);
            double[] chunk = new double[end - start];
            System.arraycopy(testSignal, start, chunk, 0, chunk.length);
            
            // Check readiness before processing
            boolean wasReady = context.isReady();
            
            double[][] chunkResult = vectorOps.processStreamingChunk(
                context, chunk, testWavelet);
            
            // Then
            if (wasReady || (i >= 1 && chunk.length == hopSize)) {
                // Should be ready after first full window
                assertNotNull(chunkResult, "Should return result when enough data available");
                assertEquals(scales.length, chunkResult.length);
                hasProcessedAtLeastOneWindow = true;
            } else {
                assertNull(chunkResult, "Should return null when not enough data available (chunk " + i + ")");
            }
        }
        
        assertTrue(hasProcessedAtLeastOneWindow, "Should have processed at least one window");
    }
    
    @Test
    @DisplayName("Should leverage platform-specific optimizations")
    @EnabledIfSystemProperty(named = "jdk.incubator.vector.VECTOR_ACCESS", matches = "true")
    void testPlatformOptimizations() {
        // Given
        CWTVectorOps.PlatformInfo platform = vectorOps.getPlatformInfo();
        
        // Then
        assertNotNull(platform);
        assertTrue(platform.vectorLength() > 0);
        assertNotNull(platform.vectorSpecies());
        
        // Verify SIMD is available
        assertTrue(platform.supportsSIMD(), "SIMD should be available with Vector API");
        
        // Check for specific features
        if (platform.isAppleSilicon()) {
            assertTrue(platform.vectorLength() >= 128, "Apple Silicon should support at least 128-bit vectors");
        } else if (platform.hasAVX512()) {
            assertEquals(512, platform.vectorLength(), "AVX-512 should support 512-bit vectors");
        } else if (platform.hasAVX2()) {
            assertEquals(256, platform.vectorLength(), "AVX2 should support 256-bit vectors");
        }
    }
    
    // Helper methods
    
    private double[] scalarConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        for (int tau = 0; tau < signalLen; tau++) {
            double sum = 0.0;
            
            for (int t = 0; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    sum += signal[idx] * wavelet[t] / sqrtScale;
                }
            }
            
            result[tau] = sum;
        }
        
        return result;
    }
    
    private void fillRandom(double[] array, Random rand) {
        for (int i = 0; i < array.length; i++) {
            array[i] = rand.nextGaussian();
        }
    }
}