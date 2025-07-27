package ai.prophetizo.wavelet.cwt.finance;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class DOGWaveletTest {
    
    private static final double TOLERANCE = 1e-10;
    private DOGWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        wavelet = new DOGWavelet(); // Default order n=2
    }
    
    @Test
    @DisplayName("Should create DOG wavelet with default order")
    void testCreateDefaultDOGWavelet() {
        assertNotNull(wavelet);
        assertEquals("dog2", wavelet.name());
        assertEquals(2, wavelet.getDerivativeOrder());
        assertFalse(wavelet.isComplex()); // DOG is real-valued
    }
    
    @Test
    @DisplayName("Should create DOG wavelet with custom order")
    void testCreateCustomOrderDOGWavelet() {
        DOGWavelet dog4 = new DOGWavelet(4);
        assertEquals("dog4", dog4.name());
        assertEquals(4, dog4.getDerivativeOrder());
    }
    
    @Test
    @DisplayName("Should validate derivative order")
    void testValidateOrder() {
        assertThrows(IllegalArgumentException.class, () -> new DOGWavelet(0));
        assertThrows(IllegalArgumentException.class, () -> new DOGWavelet(-1));
        assertThrows(IllegalArgumentException.class, () -> new DOGWavelet(11)); // Too high
    }
    
    @Test
    @DisplayName("Should compute wavelet function values")
    void testWaveletFunction() {
        // DOG(2) (Mexican Hat) at t=0 is positive
        double psi0 = wavelet.psi(0.0);
        assertTrue(psi0 > 0); // Positive at center for Mexican Hat
        
        // Should be symmetric with zero at t=±1
        double psi1 = wavelet.psi(1.0);
        double psiMinus1 = wavelet.psi(-1.0);
        assertEquals(psi1, psiMinus1, TOLERANCE);
        assertEquals(0.0, psi1, TOLERANCE); // Zero at t=±1
        
        // Should have negative side lobes
        double psi2 = wavelet.psi(2.0);
        assertTrue(psi2 < 0);
        
        // Should decay to zero as |t| increases
        assertTrue(Math.abs(wavelet.psi(10.0)) < 1e-8);
        assertTrue(Math.abs(wavelet.psi(-10.0)) < 1e-8);
    }
    
    @Test
    @DisplayName("Should verify Mexican Hat is DOG(2)")
    void testMexicanHatEquivalence() {
        // DOG(2) is the Mexican Hat wavelet
        DOGWavelet mexicanHat = new DOGWavelet(2);
        
        // Check at several points
        for (double t = -5; t <= 5; t += 0.5) {
            double dog2Value = mexicanHat.psi(t);
            // Mexican Hat formula: (2/√3 * π^(-1/4)) * (1 - t²) * exp(-t²/2)
            double normFactor = 2.0 / (Math.sqrt(3) * Math.pow(Math.PI, 0.25));
            double mexicanHatValue = normFactor * (1 - t * t) * Math.exp(-t * t / 2);
            assertEquals(mexicanHatValue, dog2Value, Math.abs(mexicanHatValue) * 1e-10);
        }
    }
    
    @Test
    @DisplayName("Should compute correct center frequency")
    void testCenterFrequency() {
        // DOG wavelet center frequency depends on derivative order
        double centerFreq = wavelet.centerFrequency();
        assertTrue(centerFreq > 0);
        
        // Higher derivatives should have higher center frequencies
        DOGWavelet dog4 = new DOGWavelet(4);
        assertTrue(dog4.centerFrequency() > centerFreq);
    }
    
    @Test
    @DisplayName("Should have appropriate bandwidth for volatility detection")
    void testBandwidth() {
        double bandwidth = wavelet.bandwidth();
        assertTrue(bandwidth > 0);
        // DOG wavelets have moderate bandwidth, good for volatility
        assertTrue(bandwidth > 0.5 && bandwidth < 2.0);
    }
    
    @Test
    @DisplayName("Should properly discretize wavelet")
    void testDiscretization() {
        int numSamples = 128;
        double[] samples = wavelet.discretize(numSamples);
        
        assertNotNull(samples);
        assertEquals(numSamples, samples.length);
        
        // Should be centered and symmetric
        int center = numSamples / 2;
        
        // Check symmetry
        for (int i = 1; i < numSamples / 4; i++) {
            assertEquals(samples[center - i], samples[center + i], 
                Math.abs(samples[center + i]) * 1e-10);
        }
        
        // Center value should be positive for DOG(2) (Mexican Hat)
        assertTrue(samples[center] > 0);
    }
    
    @Test
    @DisplayName("Should detect volatility clusters")
    void testVolatilityDetection() {
        // Create a signal with volatility clusters
        int N = 256;
        double[] signal = new double[N];
        
        // Low volatility period
        for (int i = 0; i < 80; i++) {
            signal[i] = 100.0 + 0.5 * Math.sin(2 * Math.PI * i / 20);
        }
        
        // High volatility cluster
        for (int i = 80; i < 120; i++) {
            signal[i] = 100.0 + 5.0 * Math.sin(2 * Math.PI * i / 5) + 
                        3.0 * Math.cos(2 * Math.PI * i / 3);
        }
        
        // Low volatility again
        for (int i = 120; i < N; i++) {
            signal[i] = 100.0 + 0.3 * Math.sin(2 * Math.PI * i / 25);
        }
        
        // DOG wavelet should respond strongly to volatility changes
        // Compute simple convolution around volatility transition
        double response = 0;
        double scale = 4.0;
        for (int i = 70; i < 90; i++) {
            double t = (i - 80) / scale;
            response += Math.abs(signal[i] * wavelet.psi(t));
        }
        
        // Should have significant response to volatility change
        assertTrue(response > 50.0);
    }
    
    @Test
    @DisplayName("Should handle scaled wavelet function")
    void testScaledWaveletFunction() {
        double scale = 2.0;
        double translation = 1.0;
        
        // Test scaling property
        double t = 3.0;
        double scaledPsi = wavelet.psi(t, scale, translation);
        double expectedPsi = (1.0 / Math.sqrt(scale)) * wavelet.psi((t - translation) / scale);
        assertEquals(expectedPsi, scaledPsi, TOLERANCE);
    }
}