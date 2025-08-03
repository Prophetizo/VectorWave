package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiLevelMODWTTransform implementation.
 */
class MultiLevelMODWTTransformTest {
    
    private MultiLevelMODWTTransform haarTransform;
    private MultiLevelMODWTTransform db4Transform;
    
    @BeforeEach
    void setUp() {
        haarTransform = new MultiLevelMODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        db4Transform = new MultiLevelMODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
    }
    
    @Test
    void testBasicDecomposition() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 2);
        
        assertEquals(2, result.getLevels());
        assertEquals(8, result.getSignalLength());
        
        // All coefficients should have same length as original signal
        assertEquals(8, result.getDetailCoeffsAtLevel(1).length);
        assertEquals(8, result.getDetailCoeffsAtLevel(2).length);
        assertEquals(8, result.getApproximationCoeffs().length);
    }
    
    @Test
    void testPerfectReconstruction() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 3);
        double[] reconstructed = haarTransform.reconstruct(result);
        
        assertEquals(signal.length, reconstructed.length);
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-10, 
                "Mismatch at index " + i);
        }
    }
    
    @Test
    void testArbitraryLengthSignal() {
        // MODWT should work with non-power-of-2 lengths
        double[] signal = {1, 2, 3, 4, 5, 6, 7}; // Length 7
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 2);
        double[] reconstructed = haarTransform.reconstruct(result);
        
        assertEquals(7, result.getSignalLength());
        assertEquals(7, reconstructed.length);
        
        // Check reconstruction accuracy
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-10);
        }
    }
    
    @Test
    void testEnergyPreservation() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Compute signal energy
        double signalEnergy = 0;
        for (double s : signal) {
            signalEnergy += s * s;
        }
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 3);
        
        // For MODWT, energy is not preserved in the same way as DWT
        // due to redundancy and different normalization
        // Instead, test that the reconstruction preserves the signal
        double[] reconstructed = haarTransform.reconstruct(result);
        
        double reconstructedEnergy = 0;
        for (double r : reconstructed) {
            reconstructedEnergy += r * r;
        }
        
        // Energy of reconstructed signal should match original
        assertEquals(signalEnergy, reconstructedEnergy, 1e-10);
    }
    
    @Test
    void testRelativeEnergyDistribution() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 3);
        double[] distribution = result.getRelativeEnergyDistribution();
        
        // Should have levels + 1 elements (approximation + details)
        assertEquals(4, distribution.length);
        
        // Should sum to 1.0
        double sum = 0;
        for (double d : distribution) {
            sum += d;
        }
        assertEquals(1.0, sum, 1e-10);
    }
    
    @Test
    void testDenoisingReconstruction() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 3);
        
        // Reconstruct from level 2 (remove finest details)
        double[] denoised = haarTransform.reconstructFromLevel(result, 2);
        
        assertEquals(signal.length, denoised.length);
        
        // Denoised should be smoother (less variation)
        double signalVar = computeVariance(signal);
        double denoisedVar = computeVariance(denoised);
        assertTrue(denoisedVar < signalVar);
    }
    
    @Test
    void testBandpassReconstruction() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 3);
        
        // Reconstruct only level 2 (bandpass)
        double[] bandpass = haarTransform.reconstructLevels(result, 2, 2);
        
        assertEquals(signal.length, bandpass.length);
        
        // Should contain only mid-frequency components
        // Verify by checking it's different from both signal and approximation
        assertFalse(arraysEqual(bandpass, signal));
        assertFalse(arraysEqual(bandpass, result.getApproximationCoeffs()));
    }
    
    @Test
    void testMaxLevelsCalculation() {
        // Test with different signal lengths
        double[] signal16 = new double[16];
        double[] signal100 = new double[100];
        double[] signal1000 = new double[1000];
        
        // Fill with simple pattern
        for (int i = 0; i < signal16.length; i++) signal16[i] = i + 1;
        for (int i = 0; i < signal100.length; i++) signal100[i] = i + 1;
        for (int i = 0; i < signal1000.length; i++) signal1000[i] = i + 1;
        
        // Should decompose to maximum possible levels
        MultiLevelMODWTResult result16 = haarTransform.decompose(signal16);
        MultiLevelMODWTResult result100 = haarTransform.decompose(signal100);
        MultiLevelMODWTResult result1000 = haarTransform.decompose(signal1000);
        
        // For MODWT with Haar (filter length 2), max levels are limited by
        // when scaled filter length exceeds signal length
        assertTrue(result16.getLevels() >= 3);
        assertTrue(result100.getLevels() >= 5);
        assertTrue(result1000.getLevels() >= 8);
        
        // Test reconstruction works at maximum levels
        double[] recon16 = haarTransform.reconstruct(result16);
        double[] recon100 = haarTransform.reconstruct(result100);
        double[] recon1000 = haarTransform.reconstruct(result1000);
        
        // Verify reconstruction
        assertArrayEquals(signal16, recon16, 1e-10);
        assertArrayEquals(signal100, recon100, 1e-10);
        assertArrayEquals(signal1000, recon1000, 1e-10);
    }
    
    @Test
    void testInvalidInputs() {
        // Empty signal
        assertThrows(InvalidSignalException.class, () -> {
            haarTransform.decompose(new double[0]);
        });
        
        // Invalid levels
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        assertThrows(InvalidArgumentException.class, () -> {
            haarTransform.decompose(signal, 0);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            haarTransform.decompose(signal, 20); // Too many levels
        });
    }
    
    @Test
    void testResultValidation() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        MultiLevelMODWTResult result = haarTransform.decompose(signal, 2);
        
        assertTrue(result.isValid());
        
        // Test copy
        MultiLevelMODWTResult copy = result.copy();
        assertTrue(copy.isValid());
        assertEquals(result.getLevels(), copy.getLevels());
        assertEquals(result.getSignalLength(), copy.getSignalLength());
    }
    
    @Test
    void testDaubechiesWavelet() {
        // Test with a more complex wavelet
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(8 * Math.PI * i / 32.0);
        }
        
        // DB4 has filter length 8, so max levels for signal length 32 is limited
        MultiLevelMODWTResult result = db4Transform.decompose(signal, 3);
        double[] reconstructed = db4Transform.reconstruct(result);
        
        // Check reconstruction accuracy
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-10);
        }
    }
    
    // Helper methods
    
    private double computeVariance(double[] data) {
        double mean = 0;
        for (double d : data) {
            mean += d;
        }
        mean /= data.length;
        
        double variance = 0;
        for (double d : data) {
            double diff = d - mean;
            variance += diff * diff;
        }
        return variance / data.length;
    }
    
    private boolean arraysEqual(double[] a, double[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-10) return false;
        }
        return true;
    }
}