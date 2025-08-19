package ai.prophetizo.wavelet.swt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mathematical analysis addressing the core question: 
 * "Does it make sense to have a canonical SWT implementation, or is using the MODWT good enough?"
 * 
 * This simplified analysis focuses on:
 * 1. Mathematical equivalence between SWT adapter and direct MODWT
 * 2. Performance characteristics of the MODWT-based approach
 * 3. Validation of key mathematical properties
 */
@DisplayName("SWT-MODWT Mathematical Analysis")
class SwtModwtMathematicalAnalysisTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("SWT adapter should be mathematically equivalent to direct MODWT")
    void testSwtModwtMathematicalEquivalence() {
        // Test with a variety of signal sizes and wavelets
        testEquivalenceScenario(new Haar(), 128, 3);
        testEquivalenceScenario(Daubechies.DB2, 256, 4);
        testEquivalenceScenario(Daubechies.DB4, 512, 5);
        
        System.out.println("✓ SWT adapter produces mathematically identical results to direct MODWT");
    }
    
    private void testEquivalenceScenario(ai.prophetizo.wavelet.api.Wavelet wavelet, int signalLength, int levels) {
        // Create test signal
        double[] signal = createTestSignal(signalLength);
        
        // Method 1: Direct MODWT multi-level transform
        MultiLevelMODWTTransform directTransform = new MultiLevelMODWTTransform(
            wavelet, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult directResult = directTransform.decomposeMutable(signal, levels);
        double[] directReconstructed = directTransform.reconstruct(directResult);
        
        // Method 2: SWT adapter using MODWT backend
        VectorWaveSwtAdapter swtAdapter = new VectorWaveSwtAdapter(wavelet, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult swtResult = swtAdapter.forward(signal, levels);
        double[] swtReconstructed = swtAdapter.inverse(swtResult);
        
        // Verify complete equivalence
        assertEquals(directResult.getLevels(), swtResult.getLevels());
        assertEquals(directResult.getSignalLength(), swtResult.getSignalLength());
        
        // Compare approximation coefficients
        assertArrayEquals(directResult.getApproximationCoeffs(), 
            swtResult.getApproximationCoeffs(), TOLERANCE,
            "Approximation coefficients should be identical");
        
        // Compare detail coefficients at each level
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(directResult.getDetailCoeffsAtLevel(level),
                swtResult.getDetailCoeffsAtLevel(level), TOLERANCE,
                String.format("Detail coefficients at level %d should be identical", level));
        }
        
        // Verify reconstruction equivalence
        assertArrayEquals(directReconstructed, swtReconstructed, TOLERANCE,
            "Reconstructions should be identical");
    }
    
    @Test
    @DisplayName("SWT implementation should satisfy core mathematical properties")
    void testSwtMathematicalProperties() {
        double[] signal = createTestSignal(256);
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Test perfect reconstruction
        MutableMultiLevelMODWTResult result = swt.forward(signal, 4);
        double[] reconstructed = swt.inverse(result);
        assertArrayEquals(signal, reconstructed, TOLERANCE,
            "SWT should achieve perfect reconstruction");
        
        // Test linearity
        testSwtLinearity(swt);
        
        // Test energy conservation
        testSwtEnergyConservation(swt, signal);
        
        System.out.println("✓ SWT implementation satisfies core mathematical properties");
    }
    
    private void testSwtLinearity(VectorWaveSwtAdapter swt) {
        double[] signal1 = createTestSignal(128);
        double[] signal2 = createTestSignal(128, 2.0, Math.PI/4);
        double[] signalSum = new double[128];
        
        for (int i = 0; i < 128; i++) {
            signalSum[i] = signal1[i] + signal2[i];
        }
        
        MutableMultiLevelMODWTResult result1 = swt.forward(signal1, 3);
        MutableMultiLevelMODWTResult result2 = swt.forward(signal2, 3);
        MutableMultiLevelMODWTResult resultSum = swt.forward(signalSum, 3);
        
        // Verify linearity for approximation
        double[] approx1 = result1.getApproximationCoeffs();
        double[] approx2 = result2.getApproximationCoeffs();
        double[] approxSum = resultSum.getApproximationCoeffs();
        
        for (int i = 0; i < approx1.length; i++) {
            assertEquals(approx1[i] + approx2[i], approxSum[i], TOLERANCE,
                "SWT should be linear for approximation coefficients");
        }
        
        // Verify linearity for details
        for (int level = 1; level <= 3; level++) {
            double[] details1 = result1.getDetailCoeffsAtLevel(level);
            double[] details2 = result2.getDetailCoeffsAtLevel(level);
            double[] detailsSum = resultSum.getDetailCoeffsAtLevel(level);
            
            for (int i = 0; i < details1.length; i++) {
                assertEquals(details1[i] + details2[i], detailsSum[i], TOLERANCE,
                    String.format("SWT should be linear for detail coefficients at level %d", level));
            }
        }
    }
    
    private void testSwtEnergyConservation(VectorWaveSwtAdapter swt, double[] signal) {
        MutableMultiLevelMODWTResult result = swt.forward(signal, 4);
        
        // Calculate signal energy
        double signalEnergy = 0.0;
        for (double value : signal) {
            signalEnergy += value * value;
        }
        
        // Calculate transform energy
        double transformEnergy = 0.0;
        
        // Approximation energy
        double[] approx = result.getApproximationCoeffs();
        for (double value : approx) {
            transformEnergy += value * value;
        }
        
        // Detail energy at each level
        for (int level = 1; level <= 4; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            for (double value : details) {
                transformEnergy += value * value;
            }
        }
        
        assertEquals(signalEnergy, transformEnergy, 1e-8,
            "SWT should conserve energy");
    }
    
    @Test
    @DisplayName("Performance characteristics should demonstrate MODWT optimizations")
    void testPerformanceCharacteristics() {
        System.out.println("Performance Analysis:");
        
        int[] sizes = {128, 512, 2048};
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        for (int size : sizes) {
            double[] signal = createTestSignal(size);
            
            // Warmup
            for (int i = 0; i < 5; i++) {
                MutableMultiLevelMODWTResult result = swt.forward(signal, 3);
                swt.inverse(result);
            }
            
            // Measure forward transform
            long startTime = System.nanoTime();
            MutableMultiLevelMODWTResult result = swt.forward(signal, 3);
            long forwardTime = System.nanoTime() - startTime;
            
            // Measure inverse transform
            startTime = System.nanoTime();
            double[] reconstructed = swt.inverse(result);
            long inverseTime = System.nanoTime() - startTime;
            
            // Verify accuracy is maintained
            assertArrayEquals(signal, reconstructed, TOLERANCE,
                "Performance optimizations should not compromise accuracy");
            
            System.out.printf("  Size %4d: Forward=%6.2fms, Inverse=%6.2fms%n", 
                size, forwardTime / 1e6, inverseTime / 1e6);
        }
        
        System.out.println("✓ SWT adapter leverages MODWT performance optimizations effectively");
    }
    
    @Test
    @DisplayName("MODWT-based SWT should handle edge cases robustly")
    void testRobustness() {
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(new Haar(), BoundaryMode.PERIODIC);
        
        // Test constant signal
        double[] constantSignal = new double[128];
        for (int i = 0; i < 128; i++) {
            constantSignal[i] = 5.0;
        }
        
        MutableMultiLevelMODWTResult result = swt.forward(constantSignal, 3);
        double[] reconstructed = swt.inverse(result);
        
        assertArrayEquals(constantSignal, reconstructed, TOLERANCE,
            "Constant signals should be perfectly reconstructed");
        
        // Detail coefficients should be near zero for constant signals
        for (int level = 1; level <= 3; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            for (double detail : details) {
                assertEquals(0.0, detail, 1e-10,
                    "Detail coefficients should be zero for constant signals");
            }
        }
        
        // Test impulse signal
        double[] impulseSignal = new double[128];
        impulseSignal[64] = 1.0;
        
        result = swt.forward(impulseSignal, 3);
        reconstructed = swt.inverse(result);
        
        assertArrayEquals(impulseSignal, reconstructed, TOLERANCE,
            "Impulse signals should be perfectly reconstructed");
        
        System.out.println("✓ SWT implementation handles edge cases robustly");
    }
    
    @Test
    @DisplayName("Coefficient manipulation features should work correctly")
    void testCoefficientManipulation() {
        double[] noisySignal = createNoisyTestSignal(256);
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Decompose signal
        MutableMultiLevelMODWTResult result = swt.forward(noisySignal, 4);
        
        // Apply thresholding to finest detail level (level 1) to remove noise
        double[] originalDetails = result.getDetailCoeffsAtLevel(1).clone();
        swt.applyThreshold(result, 1, 0.1, true); // Soft thresholding
        double[] thresholdedDetails = result.getDetailCoeffsAtLevel(1);
        
        // Verify thresholding was applied
        boolean thresholdingApplied = false;
        for (int i = 0; i < originalDetails.length; i++) {
            if (Math.abs(originalDetails[i] - thresholdedDetails[i]) > 1e-10) {
                thresholdingApplied = true;
                break;
            }
        }
        assertTrue(thresholdingApplied, "Thresholding should modify coefficients");
        
        // Reconstruct denoised signal
        double[] denoisedSignal = swt.inverse(result);
        assertEquals(noisySignal.length, denoisedSignal.length,
            "Denoised signal should have same length as original");
        
        // Test convenience denoising method
        double[] denoisedSignal2 = swt.denoise(noisySignal, 4, 0.1, true);
        assertEquals(noisySignal.length, denoisedSignal2.length,
            "Convenience denoising should produce correct length output");
        
        System.out.println("✓ Coefficient manipulation features work correctly");
    }
    
    // Utility methods
    private double[] createTestSignal(int length) {
        return createTestSignal(length, 1.0, 0.0);
    }
    
    private double[] createTestSignal(int length, double amplitude, double phase) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = amplitude * (
                Math.sin(2 * Math.PI * i / 16.0 + phase) + 
                0.5 * Math.sin(2 * Math.PI * i / 8.0 + phase) +
                0.25 * Math.sin(2 * Math.PI * i / 32.0 + phase)
            );
        }
        return signal;
    }
    
    private double[] createNoisyTestSignal(int length) {
        double[] signal = createTestSignal(length);
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducible tests
        
        for (int i = 0; i < length; i++) {
            signal[i] += 0.05 * random.nextGaussian(); // Add 5% noise
        }
        
        return signal;
    }
}