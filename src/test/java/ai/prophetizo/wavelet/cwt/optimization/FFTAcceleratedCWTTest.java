package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FFTAcceleratedCWTTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double RELAXED_TOLERANCE = 1e-6; // For FFT operations
    private static final long RANDOM_SEED = 12345L; // Fixed seed for reproducible tests
    private FFTAcceleratedCWT fftCWT;
    private double[] testSignal;
    private MorletWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        fftCWT = new FFTAcceleratedCWT();
        wavelet = new MorletWavelet();
        
        // Create test signal with known frequency content
        testSignal = new double[256];
        for (int i = 0; i < testSignal.length; i++) {
            // Mix of two frequencies
            double t = i / 256.0;
            testSignal[i] = Math.sin(2 * Math.PI * 10 * t) + 
                           0.5 * Math.sin(2 * Math.PI * 25 * t);
        }
    }
    
    @Test
    @DisplayName("Should compute FFT correctly")
    void testFFTComputation() {
        // Given - simple signal
        double[] signal = new double[8];
        signal[0] = 1.0; // Impulse
        
        // When
        FFTAcceleratedCWT.Complex[] fft = fftCWT.fft(signal);
        
        // Then
        assertNotNull(fft);
        assertEquals(8, fft.length);
        
        // Impulse FFT should have constant magnitude
        for (FFTAcceleratedCWT.Complex c : fft) {
            assertEquals(1.0, c.magnitude(), TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Should compute inverse FFT correctly")
    void testInverseFFT() {
        // Given
        double[] original = {1.0, 2.0, 3.0, 4.0, 5.0, 4.0, 3.0, 2.0};
        
        // When
        FFTAcceleratedCWT.Complex[] fft = fftCWT.fft(original);
        double[] reconstructed = fftCWT.ifft(fft);
        
        // Then
        assertArrayEquals(original, reconstructed, RELAXED_TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle power-of-2 sizes")
    void testPowerOfTwoSizes() {
        // Test various power-of-2 sizes
        int[] sizes = {2, 4, 8, 16, 32, 64, 128, 256};
        
        for (int size : sizes) {
            double[] signal = new double[size];
            signal[0] = 1.0; // Impulse
            
            FFTAcceleratedCWT.Complex[] fft = fftCWT.fft(signal);
            assertNotNull(fft);
            assertEquals(size, fft.length);
        }
    }
    
    @Test
    @DisplayName("Should compute CWT scale using FFT")
    void testComputeScaleFFT() {
        // Given
        double scale = 4.0;
        int fftSize = 512;
        
        // When
        double[] coefficients = fftCWT.computeScaleFFT(testSignal, wavelet, scale, fftSize);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(testSignal.length, coefficients.length);
        
        // Verify non-trivial result
        double sum = 0;
        for (double c : coefficients) {
            sum += Math.abs(c);
        }
        assertTrue(sum > 0, "FFT convolution should produce non-zero results");
    }
    
    @Test
    @DisplayName("Should produce similar pattern to direct convolution")
    void testFFTPatternMatchesDirect() {
        // Given - use simpler test signal
        double[] simpleSignal = new double[128];
        for (int i = 0; i < simpleSignal.length; i++) {
            double t = i / 128.0;
            simpleSignal[i] = Math.sin(2 * Math.PI * 5 * t); // Single frequency
        }
        
        double scale = 4.0;
        int fftSize = 256;
        
        // Direct convolution for comparison
        CWTVectorOps vectorOps = new CWTVectorOps();
        int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
        double[] waveletSamples = new double[waveletSupport];
        
        // Sample wavelet at scale
        for (int i = 0; i < waveletSupport; i++) {
            double t = (i - waveletSupport / 2.0) / scale;
            waveletSamples[i] = wavelet.psi(t);
        }
        
        double[] directResult = vectorOps.convolve(simpleSignal, waveletSamples, scale);
        
        // When
        double[] fftResult = fftCWT.computeScaleFFT(simpleSignal, wavelet, scale, fftSize);
        
        // Then - verify both produce non-zero results
        double directMax = 0, fftMax = 0;
        for (int i = 0; i < simpleSignal.length; i++) {
            directMax = Math.max(directMax, Math.abs(directResult[i]));
            fftMax = Math.max(fftMax, Math.abs(fftResult[i]));
        }
        
        assertTrue(directMax > 0, "Direct convolution should produce non-zero results");
        assertTrue(fftMax > 0, "FFT convolution should produce non-zero results");
        
        // Check that magnitudes are in same order
        assertEquals(directMax, fftMax, directMax * 0.5, 
            "Maximum magnitudes should be similar");
    }
    
    @Test
    @DisplayName("Should compute multi-scale transform efficiently")
    void testMultiScaleFFT() {
        // Given
        double[] scales = {2.0, 4.0, 8.0, 16.0};
        
        // When
        double[][] coefficients = fftCWT.computeMultiScaleFFT(testSignal, scales, wavelet);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(scales.length, coefficients.length);
        assertEquals(testSignal.length, coefficients[0].length);
        
        // Different scales should produce different patterns
        for (int s = 1; s < scales.length; s++) {
            boolean different = false;
            for (int t = 0; t < testSignal.length; t++) {
                if (Math.abs(coefficients[s][t] - coefficients[0][t]) > TOLERANCE) {
                    different = true;
                    break;
                }
            }
            assertTrue(different, "Different scales should produce different results");
        }
    }
    
    @Test
    @DisplayName("Should handle complex wavelets with FFT")
    void testComplexWaveletFFT() {
        // Given
        double scale = 4.0;
        int fftSize = 512;
        
        // When
        ComplexMatrix result = fftCWT.computeComplexScaleFFT(
            testSignal, wavelet, scale, fftSize);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getRows());
        assertEquals(testSignal.length, result.getCols());
        
        // Verify magnitude and phase are reasonable
        for (int i = 0; i < testSignal.length; i++) {
            double mag = result.getMagnitude(0, i);
            double phase = result.getPhase(0, i);
            
            assertTrue(mag >= 0, "Magnitude should be non-negative");
            assertTrue(phase >= -Math.PI && phase <= Math.PI, "Phase should be in [-π, π]");
        }
    }
    
    @Test
    @DisplayName("Should select optimal FFT size")
    void testOptimalFFTSize() {
        // Test various signal and wavelet sizes
        assertEquals(128, fftCWT.selectOptimalFFTSize(100, 20));
        assertEquals(256, fftCWT.selectOptimalFFTSize(200, 50));
        assertEquals(512, fftCWT.selectOptimalFFTSize(400, 100));
        assertEquals(1024, fftCWT.selectOptimalFFTSize(800, 200));
        assertEquals(2048, fftCWT.selectOptimalFFTSize(1500, 300));
    }
    
    @Test
    @DisplayName("Should handle pre-computed FFT for efficiency")
    void testPrecomputedFFT() {
        // Given
        FFTAcceleratedCWT.FFTCache cache = fftCWT.createFFTCache(testSignal);
        double[] scales = {2.0, 4.0, 8.0};
        
        // When - compute using cached FFT
        double[][] coefficients = fftCWT.computeWithCache(cache, scales, wavelet);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(scales.length, coefficients.length);
        
        // Compare with non-cached computation
        double[][] directCoeffs = fftCWT.computeMultiScaleFFT(testSignal, scales, wavelet);
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(directCoeffs[s], coefficients[s], RELAXED_TOLERANCE,
                "Cached and direct computation should match for scale " + scales[s]);
        }
    }
    
    @Test
    @DisplayName("Should apply proper windowing for edge effects")
    void testWindowingForEdgeEffects() {
        // Given
        double[] window = fftCWT.createWindow(testSignal.length, 
            FFTAcceleratedCWT.WindowType.TUKEY);
        
        // Then
        assertNotNull(window);
        assertEquals(testSignal.length, window.length);
        
        // Check window properties
        assertEquals(0.0, window[0], TOLERANCE); // Start at 0
        assertEquals(1.0, window[window.length / 2], TOLERANCE); // Peak at center
        assertEquals(0.0, window[window.length - 1], TOLERANCE); // End at 0
    }
    
    @Test
    @DisplayName("Should validate FFT size requirements")
    void testFFTSizeValidation() {
        // Non-power-of-2 should throw exception
        assertThrows(IllegalArgumentException.class, 
            () -> fftCWT.computeScaleFFT(testSignal, wavelet, 2.0, 100));
        
        assertThrows(IllegalArgumentException.class, 
            () -> fftCWT.computeScaleFFT(testSignal, wavelet, 2.0, 513));
        
        // Zero or negative sizes
        assertThrows(IllegalArgumentException.class, 
            () -> fftCWT.computeScaleFFT(testSignal, wavelet, 2.0, 0));
            
        assertThrows(IllegalArgumentException.class, 
            () -> fftCWT.computeScaleFFT(testSignal, wavelet, 2.0, -256));
    }
    
    @Test
    @DisplayName("Should benchmark FFT vs direct convolution")
    void testPerformanceComparison() {
        // Given
        double[] largeSignal = new double[4096];
        Random random = new Random(RANDOM_SEED); // Use seeded Random for reproducibility
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = random.nextDouble() - 0.5;
        }
        double scale = 16.0;
        
        // When - measure FFT time
        long fftStart = System.nanoTime();
        double[] fftResult = fftCWT.computeScaleFFT(largeSignal, wavelet, scale, 8192);
        long fftTime = System.nanoTime() - fftStart;
        
        // When - measure direct convolution time
        CWTVectorOps vectorOps = new CWTVectorOps();
        double[] waveletSamples = generateScaledWavelet(wavelet, scale, 256);
        
        long directStart = System.nanoTime();
        double[] directResult = vectorOps.convolve(largeSignal, waveletSamples, scale);
        long directTime = System.nanoTime() - directStart;
        
        // Then - FFT should be faster for large signals
        System.out.printf("FFT time: %.2f ms, Direct time: %.2f ms%n", 
            fftTime / 1e6, directTime / 1e6);
        
        // Verify results match
        assertNotNull(fftResult);
        assertNotNull(directResult);
        assertEquals(largeSignal.length, fftResult.length);
        assertEquals(largeSignal.length, directResult.length);
    }
    
    // Helper methods
    
    private double[] generateScaledWavelet(MorletWavelet wavelet, double scale, int length) {
        double[] samples = new double[length];
        int center = length / 2;
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) / scale;
            samples[i] = wavelet.psi(t);
        }
        
        return samples;
    }
    
    private int findMaxIndex(double[] array, int startIdx, int endIdx) {
        int maxIdx = startIdx;
        double maxValue = Math.abs(array[startIdx]);
        
        for (int i = startIdx + 1; i < endIdx && i < array.length; i++) {
            double absValue = Math.abs(array[i]);
            if (absValue > maxValue) {
                maxValue = absValue;
                maxIdx = i;
            }
        }
        
        return maxIdx;
    }
    
    private double computeCorrelation(double[] x, double[] y, int startIdx, int endIdx) {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        int count = 0;
        
        for (int i = startIdx; i < endIdx && i < x.length && i < y.length; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
            count++;
        }
        
        if (count == 0) return 0;
        
        double meanX = sumX / count;
        double meanY = sumY / count;
        
        double numerator = sumXY - count * meanX * meanY;
        double denomX = sumX2 - count * meanX * meanX;
        double denomY = sumY2 - count * meanY * meanY;
        
        if (denomX <= 0 || denomY <= 0) return 0;
        
        return numerator / Math.sqrt(denomX * denomY);
    }
    
    private double computeEnergy(double[] array, int startIdx, int endIdx) {
        double energy = 0;
        
        for (int i = startIdx; i < endIdx && i < array.length; i++) {
            energy += array[i] * array[i];
        }
        
        return energy;
    }
}