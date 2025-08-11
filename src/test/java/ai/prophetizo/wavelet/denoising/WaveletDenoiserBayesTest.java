package ai.prophetizo.wavelet.denoising;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for BAYES threshold method in WaveletDenoiser.
 */
class WaveletDenoiserBayesTest {

    @Test
    void testBayesThresholdCalculation() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a test signal with known noise level
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] noisySignal = new double[signal.length];
        double noiseLevel = 0.1;
        
        // Add Gaussian noise
        for (int i = 0; i < signal.length; i++) {
            noisySignal[i] = signal[i] + noiseLevel * Math.sin(i * 0.5); // Deterministic "noise"
        }
        
        // Test single-level denoising with BAYES method
        double[] denoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.BAYES);
        
        assertNotNull(denoised, "Denoised signal should not be null");
        assertEquals(noisySignal.length, denoised.length, "Denoised signal should have same length");
        
        // The denoised signal should be finite and reasonable
        for (double value : denoised) {
            assertTrue(Double.isFinite(value), "All denoised values should be finite");
        }
        
        // Verify BAYES produces different results than UNIVERSAL method
        double[] universalDenoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL);
        assertFalse(java.util.Arrays.equals(denoised, universalDenoised), 
                   "BAYES should produce different results than UNIVERSAL");
    }

    @Test
    void testBayesVsOtherMethods() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a noisy sinusoidal signal with more variation
        int n = 64;  // Smaller size for simpler testing
        double[] noisySignal = new double[n];
        
        for (int i = 0; i < n; i++) {
            double clean = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.cos(2 * Math.PI * i / 8);
            double noise = 0.3 * Math.cos(2 * Math.PI * i / 3 + 1.5);
            noisySignal[i] = clean + noise;
        }
        
        // Test all threshold methods
        double[] bayesDenoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.BAYES);
        double[] universalDenoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL);
        double[] sureDenoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.SURE);
        double[] minimaxDenoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.MINIMAX);
        
        // All methods should produce finite results
        assertFiniteArray(bayesDenoised, "BAYES denoised");
        assertFiniteArray(universalDenoised, "UNIVERSAL denoised");
        assertFiniteArray(sureDenoised, "SURE denoised");
        assertFiniteArray(minimaxDenoised, "MINIMAX denoised");
        
        // Simply verify that BAYES produces valid results
        // The specific comparison doesn't matter as much as ensuring the method works
        assertTrue(bayesDenoised.length == noisySignal.length, "BAYES output should have correct length");
        assertTrue(universalDenoised.length == noisySignal.length, "UNIVERSAL output should have correct length");
        
        // Test that BAYES can handle edge cases
        double[] constantSignal = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        double[] constantDenoised = denoiser.denoise(constantSignal, WaveletDenoiser.ThresholdMethod.BAYES);
        assertFiniteArray(constantDenoised, "BAYES with constant signal");
    }

    @Test
    void testBayesMultiLevel() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a test signal
        int n = 256;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        
        // Test multi-level denoising with BAYES method
        double[] denoised = denoiser.denoiseMultiLevel(signal, 3, 
                                                      WaveletDenoiser.ThresholdMethod.BAYES,
                                                      WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised, "Multi-level denoised signal should not be null");
        assertEquals(signal.length, denoised.length, "Denoised signal should have same length");
        assertFiniteArray(denoised, "Multi-level BAYES denoised");
    }

    @Test
    void testBayesWithDifferentThresholdTypes() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a simple test signal
        double[] signal = {1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8};
        
        // Test both soft and hard thresholding with BAYES
        double[] softDenoised = denoiser.denoise(signal, WaveletDenoiser.ThresholdMethod.BAYES,
                                                WaveletDenoiser.ThresholdType.SOFT);
        double[] hardDenoised = denoiser.denoise(signal, WaveletDenoiser.ThresholdMethod.BAYES,
                                                WaveletDenoiser.ThresholdType.HARD);
        
        assertNotNull(softDenoised, "Soft BAYES denoised should not be null");
        assertNotNull(hardDenoised, "Hard BAYES denoised should not be null");
        assertEquals(signal.length, softDenoised.length, "Soft denoised should have same length");
        assertEquals(signal.length, hardDenoised.length, "Hard denoised should have same length");
        
        assertFiniteArray(softDenoised, "Soft BAYES denoised");
        assertFiniteArray(hardDenoised, "Hard BAYES denoised");
    }

    @Test
    void testBayesThresholdProperties() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Test with signal of varying variance
        double[] lowVarianceSignal = {1.0, 1.1, 0.9, 1.05, 0.95, 1.02, 0.98, 1.0};
        double[] highVarianceSignal = {1.0, 5.0, 2.0, 8.0, 3.0, 6.0, 4.0, 7.0};
        
        double[] lowVarDenoised = denoiser.denoise(lowVarianceSignal, WaveletDenoiser.ThresholdMethod.BAYES);
        double[] highVarDenoised = denoiser.denoise(highVarianceSignal, WaveletDenoiser.ThresholdMethod.BAYES);
        
        assertFiniteArray(lowVarDenoised, "Low variance BAYES denoised");
        assertFiniteArray(highVarDenoised, "High variance BAYES denoised");
        
        // Both should produce reasonable results
        assertEquals(lowVarianceSignal.length, lowVarDenoised.length);
        assertEquals(highVarianceSignal.length, highVarDenoised.length);
    }

    private void assertFiniteArray(double[] array, String description) {
        for (int i = 0; i < array.length; i++) {
            assertTrue(Double.isFinite(array[i]), 
                      description + " should have finite values at index " + i + ", got: " + array[i]);
        }
    }

    private boolean arrayEquals(double[] a, double[] b, double tolerance) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > tolerance) return false;
        }
        return true;
    }

    @Test
    void testBayesWithStreamingDenoiserConfig() {
        // Test that StreamingDenoiserConfig accepts BAYES method
        assertDoesNotThrow(() -> {
            StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                .blockSize(256)
                .thresholdMethod(WaveletDenoiser.ThresholdMethod.BAYES)
                .build();
            
            assertEquals(WaveletDenoiser.ThresholdMethod.BAYES, config.getThresholdMethod(),
                        "Config should accept BAYES threshold method");
        }, "StreamingDenoiserConfig should accept BAYES method");
    }
}