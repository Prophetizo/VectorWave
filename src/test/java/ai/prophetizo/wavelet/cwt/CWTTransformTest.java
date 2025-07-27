package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class CWTTransformTest {
    
    private static final double TOLERANCE = 1e-10;
    
    /**
     * Relative tolerance for normalization factor tests (10%).
     * This tolerance accounts for:
     * 1. Numerical integration errors that accumulate differently at different scales
     * 2. Boundary effects that can introduce small variations
     * 3. Signal frequency content that may not perfectly align with wavelet center frequency
     */
    private static final double NORMALIZATION_TOLERANCE = 0.1;
    private CWTTransform transform;
    private MorletWavelet wavelet;
    private double[] testSignal;
    
    @BeforeEach
    void setUp() {
        wavelet = new MorletWavelet();
        transform = new CWTTransform(wavelet);
        
        // Create a test signal with known frequency content
        // Signal = sin(2π * 10 * t) + sin(2π * 25 * t)
        int N = 256;
        testSignal = new double[N];
        double samplingRate = 100.0; // Hz
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            testSignal[i] = Math.sin(2 * Math.PI * 10 * t) + 
                           0.5 * Math.sin(2 * Math.PI * 25 * t);
        }
    }
    
    @Test
    @DisplayName("Should create CWT transform with default config")
    void testCreateWithDefaultConfig() {
        // When
        CWTTransform cwt = new CWTTransform(wavelet);
        
        // Then
        assertNotNull(cwt);
        assertNotNull(cwt.getConfig());
        assertEquals(wavelet, cwt.getWavelet());
    }
    
    @Test
    @DisplayName("Should create CWT transform with custom config")
    void testCreateWithCustomConfig() {
        // Given
        CWTConfig config = CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(false)
            .build();
        
        // When
        CWTTransform cwt = new CWTTransform(wavelet, config);
        
        // Then
        assertNotNull(cwt);
        assertEquals(config, cwt.getConfig());
        assertFalse(cwt.getConfig().isFFTEnabled());
    }
    
    @Test
    @DisplayName("Should analyze signal with scale array")
    void testAnalyzeWithScaleArray() {
        // Given
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        
        // When
        CWTResult result = transform.analyze(testSignal, scales);
        
        // Then
        assertNotNull(result);
        assertEquals(4, result.getNumScales());
        assertEquals(testSignal.length, result.getNumSamples());
        assertArrayEquals(scales, result.getScales(), TOLERANCE);
        assertEquals(wavelet, result.getWavelet());
    }
    
    @Test
    @DisplayName("Should analyze signal with ScaleSpace")
    void testAnalyzeWithScaleSpace() {
        // Given
        ScaleSpace scaleSpace = ScaleSpace.logarithmic(1.0, 32.0, 10);
        
        // When
        CWTResult result = transform.analyze(testSignal, scaleSpace);
        
        // Then
        assertNotNull(result);
        assertEquals(10, result.getNumScales());
        assertEquals(testSignal.length, result.getNumSamples());
    }
    
    @Test
    @DisplayName("Should detect frequency components")
    void testFrequencyDetection() {
        // Given
        double samplingRate = 100.0;
        // Create scales for frequencies around 10Hz and 25Hz
        ScaleSpace scaleSpace = ScaleSpace.forFrequencyRange(
            5.0, 50.0, samplingRate, wavelet, 20
        );
        
        // When
        CWTResult result = transform.analyze(testSignal, scaleSpace);
        double[] frequencies = result.getFrequencies(samplingRate);
        
        // Then
        // Find peaks in the time-averaged spectrum
        double[] avgSpectrum = result.getTimeAveragedSpectrum();
        
        // Should have peaks around 10Hz and 25Hz
        int peak1Idx = findClosestIndex(frequencies, 10.0);
        int peak2Idx = findClosestIndex(frequencies, 25.0);
        
        // Verify peaks exist at expected frequencies
        assertTrue(avgSpectrum[peak1Idx] > avgSpectrum[peak1Idx - 1]);
        assertTrue(avgSpectrum[peak1Idx] > avgSpectrum[peak1Idx + 1]);
        assertTrue(avgSpectrum[peak2Idx] > avgSpectrum[peak2Idx - 1]);
        assertTrue(avgSpectrum[peak2Idx] > avgSpectrum[peak2Idx + 1]);
    }
    
    @Test
    @DisplayName("Should handle edge effects correctly")
    void testEdgeEffects() {
        // Given
        double[] shortSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 4.0, 3.0, 2.0};
        double[] scales = {1.0, 2.0};
        
        // When
        CWTResult result = transform.analyze(shortSignal, scales);
        
        // Then
        double[][] coeffs = result.getCoefficients();
        assertNotNull(coeffs);
        assertEquals(2, coeffs.length);
        assertEquals(8, coeffs[0].length);
        
        // Check that edge values are reasonable (no NaN or Inf)
        for (double[] row : coeffs) {
            for (double val : row) {
                assertTrue(Double.isFinite(val));
            }
        }
    }
    
    @Test
    @DisplayName("Should normalize across scales when enabled")
    void testScaleNormalization() {
        // Given
        CWTConfig normalizedConfig = CWTConfig.builder()
            .normalizeScales(true)
            .build();
        CWTTransform normalizedTransform = new CWTTransform(wavelet, normalizedConfig);
        
        CWTConfig unnormalizedConfig = CWTConfig.builder()
            .normalizeScales(false)
            .build();
        CWTTransform unnormalizedTransform = new CWTTransform(wavelet, unnormalizedConfig);
        
        // Use a chirp signal instead of constant
        // A chirp signal has linearly increasing frequency, which helps test
        // the normalization across different scales. The instantaneous frequency
        // at time t is: f(t) = f0 + k*t, where k is the chirp rate
        double[] chirpSignal = new double[256];
        for (int i = 0; i < chirpSignal.length; i++) {
            double t = i / 256.0;
            double freq = 5 + 20 * t; // Frequency increases from 5 to 25 Hz
            chirpSignal[i] = Math.sin(2 * Math.PI * freq * t);
        }
        double[] scales = {2.0, 4.0, 8.0};
        
        // When
        CWTResult normalizedResult = normalizedTransform.analyze(chirpSignal, scales);
        CWTResult unnormalizedResult = unnormalizedTransform.analyze(chirpSignal, scales);
        
        // Then
        double[][] normCoeffs = normalizedResult.getMagnitude();
        double[][] unnormCoeffs = unnormalizedResult.getMagnitude();
        
        // Mathematical relationship between normalized and unnormalized coefficients:
        // 
        // The continuous wavelet transform is defined as:
        // CWT(a,b) = (1/√a) ∫ f(t) ψ*((t-b)/a) dt
        // 
        // Where:
        // - a is the scale parameter
        // - b is the translation parameter  
        // - ψ is the mother wavelet
        // - ψ* is the complex conjugate of ψ
        //
        // When normalization is enabled, the 1/√a factor is included,
        // which preserves the L2 norm of the wavelet across scales.
        // When disabled, this factor is omitted.
        //
        // Therefore: unnormalized_coeff = normalized_coeff * √a
        //
        // Check that normalized coefficients differ from unnormalized by sqrt(scale) factor
        for (int s = 0; s < scales.length; s++) {
            double scaleFactor = Math.sqrt(scales[s]);
            
            // Check a few points in the middle of the signal
            // We avoid edges to prevent boundary effects from affecting the test
            for (int t = 100; t < 150; t++) {
                double expectedRatio = scaleFactor;
                double actualRatio = unnormCoeffs[s][t] / normCoeffs[s][t];
                
                // Check that the normalized coefficient is not too small to avoid
                // numerical instability when computing the ratio
                if (normCoeffs[s][t] > TOLERANCE) { // Avoid division by very small numbers
                    assertEquals(expectedRatio, actualRatio, expectedRatio * NORMALIZATION_TOLERANCE,
                        "Normalization factor incorrect at scale " + scales[s] + ", time " + t);
                }
            }
        }
    }
    
    @Test
    @DisplayName("Should use FFT acceleration when beneficial")
    void testFFTAcceleration() {
        // Given
        CWTConfig fftConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftSize(512)
            .build();
        CWTTransform fftTransform = new CWTTransform(wavelet, fftConfig);
        
        CWTConfig directConfig = CWTConfig.builder()
            .enableFFT(false)
            .build();
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        
        double[] scales = {4.0, 8.0, 16.0};
        
        // When
        CWTResult fftResult = fftTransform.analyze(testSignal, scales);
        CWTResult directResult = directTransform.analyze(testSignal, scales);
        
        // Then - results should be similar
        double[][] fftCoeffs = fftResult.getMagnitude();
        double[][] directCoeffs = directResult.getMagnitude();
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 10; j < testSignal.length - 10; j++) { // Skip edges
                assertEquals(directCoeffs[i][j], fftCoeffs[i][j], 
                    Math.abs(directCoeffs[i][j]) * 0.01, // 1% tolerance
                    "Mismatch at scale " + i + ", time " + j);
            }
        }
    }
    
    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Null signal
        assertThrows(IllegalArgumentException.class, 
            () -> transform.analyze((double[])null, new double[]{1.0}));
        
        // Empty signal
        assertThrows(IllegalArgumentException.class, 
            () -> transform.analyze(new double[0], new double[]{1.0}));
        
        // Null scales
        assertThrows(IllegalArgumentException.class, 
            () -> transform.analyze(testSignal, (double[])null));
        
        // Empty scales
        assertThrows(IllegalArgumentException.class, 
            () -> transform.analyze(testSignal, new double[0]));
        
        // Invalid scales (negative)
        assertThrows(IllegalArgumentException.class, 
            () -> transform.analyze(testSignal, new double[]{1.0, -2.0}));
    }
    
    @Test
    @DisplayName("Should handle complex wavelet correctly")
    void testComplexWavelet() {
        // Given
        ComplexMorletWavelet complexWavelet = new ComplexMorletWavelet();
        CWTTransform complexTransform = new CWTTransform(complexWavelet);
        double[] scales = {2.0, 4.0, 8.0};
        
        // When
        CWTResult result = complexTransform.analyze(testSignal, scales);
        
        // Then
        assertTrue(result.isComplex());
        assertNotNull(result.getPhase());
        
        // Phase should be well-defined
        double[][] phase = result.getPhase();
        for (double[] row : phase) {
            for (double val : row) {
                assertTrue(val >= -Math.PI && val <= Math.PI);
            }
        }
    }
    
    @Test
    @DisplayName("Should analyze impulse signal correctly")
    void testImpulseResponse() {
        // Given
        double[] impulse = new double[128];
        impulse[64] = 1.0; // Delta function at center
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        
        // When
        CWTResult result = transform.analyze(impulse, scales);
        
        // Then
        // CWT of impulse should peak at impulse location
        for (int scaleIdx = 0; scaleIdx < scales.length; scaleIdx++) {
            double[] timeSlice = result.getTimeSlice(scaleIdx);
            
            // Find maximum
            int maxIdx = 0;
            double maxVal = Math.abs(timeSlice[0]);
            for (int i = 1; i < timeSlice.length; i++) {
                if (Math.abs(timeSlice[i]) > maxVal) {
                    maxVal = Math.abs(timeSlice[i]);
                    maxIdx = i;
                }
            }
            
            // Maximum should be near the impulse location
            assertEquals(64, maxIdx, (int)(scales[scaleIdx] * 2), 
                "Impulse response peak not at expected location for scale " + scales[scaleIdx]);
        }
    }
    
    // Helper methods
    
    private int findClosestIndex(double[] array, double target) {
        int closest = 0;
        double minDiff = Math.abs(array[0] - target);
        
        for (int i = 1; i < array.length; i++) {
            double diff = Math.abs(array[i] - target);
            if (diff < minDiff) {
                minDiff = diff;
                closest = i;
            }
        }
        
        return closest;
    }
    
    /**
     * Complex Morlet wavelet for testing complex CWT.
     */
    static class ComplexMorletWavelet implements ContinuousWavelet {
        private final double omega0 = 6.0;
        private final double sigma = 1.0;
        
        @Override
        public String name() {
            return "cmor";
        }
        
        @Override
        public double psi(double t) {
            // Real part only for interface compatibility
            double gaussian = Math.exp(-0.5 * t * t / (sigma * sigma));
            return gaussian * Math.cos(omega0 * t);
        }
        
        @Override
        public double centerFrequency() {
            return omega0 / (2 * Math.PI);
        }
        
        @Override
        public double bandwidth() {
            return sigma;
        }
        
        @Override
        public boolean isComplex() {
            return true;
        }
        
        @Override
        public double[] discretize(int numCoeffs) {
            // Simple discretization for testing
            double[] coeffs = new double[numCoeffs];
            double t0 = -4.0 * sigma;
            double dt = 8.0 * sigma / (numCoeffs - 1);
            
            for (int i = 0; i < numCoeffs; i++) {
                double t = t0 + i * dt;
                coeffs[i] = psi(t);
            }
            return coeffs;
        }
        
        public ComplexValue psiComplex(double t) {
            double gaussian = Math.exp(-0.5 * t * t / (sigma * sigma));
            double real = gaussian * Math.cos(omega0 * t);
            double imag = gaussian * Math.sin(omega0 * t);
            return new ComplexValue(real, imag);
        }
    }
    
    record ComplexValue(double real, double imag) {}
}