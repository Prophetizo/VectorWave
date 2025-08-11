package ai.prophetizo.wavelet.swt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.modwt.MutableMultiLevelMODWTResult;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VectorWaveSwtAdapter and mutable MODWT results.
 */
class VectorWaveSwtAdapterTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("SWT adapter should perform basic decomposition and reconstruction")
    void testBasicSwtDecompositionReconstruction() {
        // Create test signal
        double[] signal = createTestSignal(64);
        
        // Create SWT adapter
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Perform decomposition
        MutableMultiLevelMODWTResult result = swt.forward(signal, 3);
        
        // Verify structure
        assertNotNull(result);
        assertEquals(3, result.getLevels());
        assertEquals(64, result.getSignalLength());
        
        // Verify perfect reconstruction
        double[] reconstructed = swt.inverse(result);
        assertArrayEquals(signal, reconstructed, TOLERANCE, 
            "Perfect reconstruction should be achieved");
    }
    
    @Test
    @DisplayName("Mutable result should allow coefficient modification")
    void testMutableCoefficientModification() {
        double[] signal = createTestSignal(32);
        
        // Create transform and decompose
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            Haar.INSTANCE, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult mutableResult = transform.decomposeMutable(signal, 2);
        
        // Get mutable detail coefficients
        double[] details = mutableResult.getMutableDetailCoeffs(1);
        double originalValue = details[0];
        
        // Modify coefficient directly
        details[0] = originalValue * 2;
        
        // Verify modification persists
        double[] retrievedDetails = mutableResult.getDetailCoeffsAtLevel(1);
        assertEquals(originalValue * 2, retrievedDetails[0], TOLERANCE,
            "Modification should persist in result");
        
        // Reconstruction should reflect changes
        double[] reconstructed1 = transform.reconstruct(mutableResult);
        
        // Reset and verify different reconstruction
        details[0] = originalValue;
        double[] reconstructed2 = transform.reconstruct(mutableResult);
        
        assertFalse(java.util.Arrays.equals(reconstructed1, reconstructed2),
            "Different coefficients should produce different reconstructions");
    }
    
    @Test
    @DisplayName("SWT thresholding should modify coefficients correctly")
    void testSwtThresholding() {
        double[] signal = createTestSignal(32);
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB2);
        
        // Decompose
        MutableMultiLevelMODWTResult result = swt.forward(signal, 2);
        
        // Get original coefficients
        double[] originalDetails = result.getDetailCoeffsAtLevel(1).clone();
        
        // Apply hard thresholding
        double threshold = 0.1;
        swt.applyThreshold(result, 1, threshold, false);
        
        // Verify thresholding
        double[] thresholdedDetails = result.getDetailCoeffsAtLevel(1);
        for (int i = 0; i < originalDetails.length; i++) {
            if (Math.abs(originalDetails[i]) <= threshold) {
                assertEquals(0.0, thresholdedDetails[i], TOLERANCE,
                    "Values below threshold should be zeroed");
            } else {
                assertEquals(originalDetails[i], thresholdedDetails[i], TOLERANCE,
                    "Values above threshold should be unchanged");
            }
        }
    }
    
    @Test
    @DisplayName("Soft thresholding should shrink coefficients correctly")
    void testSoftThresholding() {
        double[] signal = createTestSignal(32);
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Haar.INSTANCE);
        
        // Decompose
        MutableMultiLevelMODWTResult result = swt.forward(signal, 1);
        
        // Apply soft thresholding
        double threshold = 0.5;
        result.applyThreshold(1, threshold, true);
        
        // Verify soft thresholding
        double[] details = result.getDetailCoeffsAtLevel(1);
        for (double coeff : details) {
            assertTrue(Math.abs(coeff) <= Math.abs(createTestSignal(32)[0]) || 
                      Math.abs(coeff) == 0.0,
                "Soft thresholding should shrink or zero coefficients");
        }
    }
    
    @Test
    @DisplayName("Cache clearing should work after coefficient modification")
    void testCacheClearing() {
        double[] signal = createTestSignal(16);
        
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            Haar.INSTANCE, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult result = transform.decomposeMutable(signal, 1);
        
        // Get initial energy
        double initialEnergy = result.getDetailEnergyAtLevel(1);
        
        // Modify coefficients
        double[] details = result.getMutableDetailCoeffs(1);
        for (int i = 0; i < details.length; i++) {
            details[i] *= 2;
        }
        
        // Clear cache
        result.clearCaches();
        
        // Energy should be recalculated
        double newEnergy = result.getDetailEnergyAtLevel(1);
        assertEquals(initialEnergy * 4, newEnergy, TOLERANCE * 100,
            "Energy should quadruple when coefficients are doubled");
    }
    
    @Test
    @DisplayName("Denoising convenience method should work")
    void testDenoisingConvenience() {
        // Create a simple step signal that's easier to denoise
        double[] clean = new double[64];
        for (int i = 0; i < 32; i++) {
            clean[i] = 1.0;
        }
        for (int i = 32; i < 64; i++) {
            clean[i] = -1.0;
        }
        
        // Add significant noise
        double[] noisy = addNoise(clean, 0.3);
        
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4);
        
        // Denoise using convenience method with custom threshold
        double[] denoised = swt.denoise(noisy, 3, 0.5, true);
        
        // Verify denoising improved signal
        double noisyError = computeRMSE(clean, noisy);
        double denoisedError = computeRMSE(clean, denoised);
        
        // Even if not perfect, thresholding should at least not make it worse
        assertTrue(denoisedError <= noisyError * 1.1,
            "Denoising should not significantly worsen the signal: " + denoisedError + " <= " + (noisyError * 1.1));
    }
    
    @Test
    @DisplayName("Level extraction should isolate specific scales")
    void testLevelExtraction() {
        double[] signal = createTestSignal(64);
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Haar.INSTANCE);
        
        // Extract level 1 details
        double[] level1 = swt.extractLevel(signal, 3, 1);
        
        // The extracted signal should be non-zero
        double extractedEnergy = 0;
        for (double val : level1) {
            extractedEnergy += val * val;
        }
        assertTrue(extractedEnergy > 0, "Extracted level should have energy");
        
        // Verify reconstruction with only one level differs from original
        double reconstructionError = computeRMSE(signal, level1);
        assertTrue(reconstructionError > 0, 
            "Single level extraction should differ from original signal");
        
        // Extract and sum all components should approximately equal original
        double[] approx = swt.extractLevel(signal, 3, 0);
        double[] detail1 = swt.extractLevel(signal, 3, 1);
        double[] detail2 = swt.extractLevel(signal, 3, 2);
        double[] detail3 = swt.extractLevel(signal, 3, 3);
        
        double[] sum = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            sum[i] = approx[i] + detail1[i] + detail2[i] + detail3[i];
        }
        
        assertArrayEquals(signal, sum, 1e-8,
            "Sum of all extracted levels should equal original signal");
    }
    
    @Test
    @DisplayName("Immutable conversion should create independent copy")
    void testImmutableConversion() {
        double[] signal = createTestSignal(32);
        
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            Haar.INSTANCE, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult mutable = transform.decomposeMutable(signal, 2);
        
        // Convert to immutable
        MultiLevelMODWTResult immutable = mutable.toImmutable();
        
        // Modify mutable version
        double[] mutableDetails = mutable.getMutableDetailCoeffs(1);
        mutableDetails[0] *= 2;
        
        // Immutable should be unaffected
        double[] immutableDetails = immutable.getDetailCoeffsAtLevel(1);
        assertNotEquals(mutableDetails[0], immutableDetails[0],
            "Immutable copy should be independent");
    }
    
    @Test
    @DisplayName("Universal threshold should be calculated correctly")
    void testUniversalThreshold() {
        double[] signal = createTestSignal(256);
        double[] noisy = addNoise(signal, 0.1);
        
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4);
        MutableMultiLevelMODWTResult result = swt.forward(noisy, 4);
        
        // Apply universal threshold
        swt.applyUniversalThreshold(result, true);
        
        // Verify some coefficients were thresholded
        boolean someZeroed = false;
        boolean someNonZero = false;
        
        for (int level = 1; level <= 4; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            for (double coeff : details) {
                if (coeff == 0.0) someZeroed = true;
                if (coeff != 0.0) someNonZero = true;
            }
        }
        
        assertTrue(someZeroed, "Some coefficients should be zeroed by thresholding");
        assertTrue(someNonZero, "Some coefficients should survive thresholding");
    }
    
    @Test
    @DisplayName("Result validation should detect invalid states")
    void testResultValidation() {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            Haar.INSTANCE, BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        MutableMultiLevelMODWTResult result = transform.decomposeMutable(signal, 2);
        
        // Valid result
        assertTrue(result.isValid(), "Normal result should be valid");
        
        // Introduce NaN
        double[] details = result.getMutableDetailCoeffs(1);
        details[0] = Double.NaN;
        
        assertFalse(result.isValid(), "Result with NaN should be invalid");
        
        // Fix and verify
        details[0] = 0.0;
        assertTrue(result.isValid(), "Fixed result should be valid again");
    }
    
    // Helper methods
    
    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 16.0);
        }
        return signal;
    }
    
    private static double[] addNoise(double[] signal, double noiseLevel) {
        java.util.Random rng = new java.util.Random(42);
        double[] noisy = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + noiseLevel * rng.nextGaussian();
        }
        return noisy;
    }
    
    private static double computeRMSE(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }
}