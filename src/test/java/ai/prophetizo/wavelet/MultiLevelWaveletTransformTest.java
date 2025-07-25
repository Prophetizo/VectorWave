package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import ai.prophetizo.wavelet.exception.InvalidSignalException;

/**
 * Tests for multi-level wavelet decomposition and reconstruction.
 */
class MultiLevelWaveletTransformTest extends BaseWaveletTest {
    
    private static final double EPSILON = 1e-10;
    
    static Stream<Arguments> waveletProvider() {
        return Stream.of(
            Arguments.of(new Haar(), "Haar"),
            Arguments.of(Daubechies.DB2, "DB2"),
            Arguments.of(Daubechies.DB4, "DB4"),
            Arguments.of(Symlet.SYM3, "SYM3"),
            Arguments.of(Coiflet.COIF1, "COIF1")
        );
    }
    
    @ParameterizedTest(name = "{1} wavelet multi-level decomposition")
    @MethodSource("waveletProvider")
    @DisplayName("Perfect reconstruction for multi-level decomposition")
    void testPerfectReconstruction(Wavelet wavelet, String name) {
        double[] signal = generateTestSignal(256);
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(wavelet);
        
        // Test different decomposition levels
        for (int levels = 1; levels <= 4; levels++) {
            MultiLevelTransformResult result = mwt.decompose(signal, levels);
            double[] reconstructed = mwt.reconstruct(result);
            
            assertArrayEquals(signal, reconstructed, EPSILON,
                "Failed perfect reconstruction for " + name + " with " + levels + " levels");
        }
    }
    
    @Test
    @DisplayName("Multi-level decomposition structure validation")
    void testDecompositionStructure() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 8.0);
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB4);
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Verify structure
        assertEquals(3, result.levels());
        
        // Check coefficient lengths at each level
        assertEquals(128, result.detailsAtLevel(1).length); // Level 1: 256/2
        assertEquals(64, result.detailsAtLevel(2).length);  // Level 2: 128/2
        assertEquals(32, result.detailsAtLevel(3).length);  // Level 3: 64/2
        assertEquals(32, result.finalApproximation().length);
        
        // Verify total coefficient count equals signal length
        int totalCoeffs = result.finalApproximation().length;
        for (int level = 1; level <= 3; level++) {
            totalCoeffs += result.detailsAtLevel(level).length;
        }
        assertEquals(signal.length, totalCoeffs);
    }
    
    @Test
    @DisplayName("Energy conservation in multi-level decomposition")
    void testEnergyConservation() {
        double[] signal = generateTestSignal(128);
        
        // Calculate signal energy
        double signalEnergy = 0.0;
        for (double s : signal) {
            signalEnergy += s * s;
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Calculate decomposition energy
        double decompositionEnergy = 0.0;
        
        // Add final approximation energy
        for (double a : result.finalApproximation()) {
            decompositionEnergy += a * a;
        }
        
        // Add detail energies
        decompositionEnergy += result.totalDetailEnergy();
        
        // Haar wavelet preserves energy exactly
        assertEquals(signalEnergy, decompositionEnergy, EPSILON,
            "Energy not conserved in decomposition");
    }
    
    @Test
    @DisplayName("Adaptive decomposition based on energy threshold")
    void testAdaptiveDecomposition() {
        // Create signal with decreasing high-frequency content
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 128.0) +    // Low frequency
                       0.1 * Math.sin(2 * Math.PI * i / 16.0) +  // Medium frequency
                       0.01 * Math.sin(2 * Math.PI * i / 4.0);   // High frequency
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB4);
        
        // High threshold - should stop early
        MultiLevelTransformResult highThreshold = mwt.decomposeAdaptive(signal, 0.05);
        
        // Low threshold - should go deeper
        MultiLevelTransformResult lowThreshold = mwt.decomposeAdaptive(signal, 0.001);
        
        assertTrue(highThreshold.levels() <= lowThreshold.levels(),
            "Adaptive decomposition should produce fewer or equal levels with higher threshold");
    }
    
    @Test
    @DisplayName("Level-wise reconstruction for denoising")
    void testLevelWiseReconstruction() {
        // Create noisy signal
        double[] clean = new double[256];
        double[] noisy = new double[256];
        
        for (int i = 0; i < clean.length; i++) {
            clean[i] = Math.sin(2 * Math.PI * i / 64.0);
            noisy[i] = clean[i] + 0.1 * Math.random(); // Add noise
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB4);
        MultiLevelTransformResult result = mwt.decompose(noisy, 4);
        
        // Reconstruct from different levels
        double[] fromLevel0 = mwt.reconstructFromLevel(result, 0); // Full reconstruction
        double[] fromLevel2 = mwt.reconstructFromLevel(result, 2); // Remove 2 finest levels
        double[] fromLevel4 = mwt.reconstructFromLevel(result, 4); // Keep only approximation
        
        // Verify reconstructions
        assertArrayEquals(noisy, fromLevel0, EPSILON, "Level 0 should give full reconstruction");
        
        // Calculate noise reduction (MSE)
        double mseLevel2 = 0.0;
        double mseOriginal = 0.0;
        
        for (int i = 0; i < clean.length; i++) {
            mseLevel2 += Math.pow(clean[i] - fromLevel2[i], 2);
            mseOriginal += Math.pow(clean[i] - noisy[i], 2);
        }
        
        assertTrue(mseLevel2 < mseOriginal,
            "Reconstruction from level 2 should reduce noise");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {32, 64, 128, 256, 512})
    @DisplayName("Maximum decomposition levels for different signal lengths")
    void testMaximumLevels(int signalLength) {
        double[] signal = new double[signalLength];
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        
        // Full decomposition
        MultiLevelTransformResult result = mwt.decompose(signal);
        
        // Verify we can't decompose further
        int maxLevels = result.levels();
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decompose(signal, maxLevels + 1),
            "Should not allow decomposition beyond maximum levels");
        
        // Verify final approximation length
        int expectedFinalLength = signalLength / (1 << maxLevels);
        assertTrue(expectedFinalLength >= 1,
            "Final approximation should have at least 1 coefficient");
    }
    
    @Test
    @DisplayName("Approximation at different levels")
    void testApproximationAtLevels() {
        double[] signal = generateTestSignal(128);
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB2);
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Test approximation at each level
        double[] approx0 = result.approximationAtLevel(0);
        double[] approx1 = result.approximationAtLevel(1);
        double[] approx2 = result.approximationAtLevel(2);
        double[] approx3 = result.approximationAtLevel(3);
        
        // Verify lengths
        assertEquals(128, approx0.length); // Original signal
        assertEquals(64, approx1.length);  // After 1 level
        assertEquals(32, approx2.length);  // After 2 levels
        assertEquals(16, approx3.length);  // After 3 levels (final)
        
        // Level 0 should equal original signal
        assertArrayEquals(signal, approx0, EPSILON);
        
        // Final level should equal finalApproximation
        assertArrayEquals(result.finalApproximation(), approx3, EPSILON);
    }
    
    @Test
    @DisplayName("Transform result at specific levels")
    void testTransformResultAtLevels() {
        double[] signal = generateTestSignal(256);
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(new Haar());
        MultiLevelTransformResult result = mwt.decompose(signal, 3);
        
        // Get transform results at each level
        for (int level = 1; level <= 3; level++) {
            TransformResult levelResult = result.getTransformResultAtLevel(level);
            
            // Verify consistency
            assertArrayEquals(result.detailsAtLevel(level), 
                            levelResult.detailCoeffs(), EPSILON);
            
            // The approximation coefficients in levelResult should match
            // the coefficients that produced these details
            // For verification, we can check that reconstruction works
            WaveletTransform wt = new WaveletTransform(mwt.getWavelet(), mwt.getBoundaryMode());
            double[] reconstructed = wt.inverse(levelResult);
            
            // The reconstructed signal should match what we'd get by
            // reconstructing from that level
            if (level == 1) {
                assertArrayEquals(signal, reconstructed, EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Invalid input handling")
    void testInvalidInputs() {
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB4);
        
        // Null signal
        assertThrows(InvalidSignalException.class,
            () -> mwt.decompose(null));
        
        // Empty signal
        assertThrows(InvalidSignalException.class,
            () -> mwt.decompose(new double[0]));
        
        // Non-power-of-2 length
        assertThrows(InvalidSignalException.class,
            () -> mwt.decompose(new double[100]));
        
        // Invalid number of levels
        double[] signal = new double[64];
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decompose(signal, 0));
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decompose(signal, -1));
        
        // Invalid adaptive threshold
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decomposeAdaptive(signal, 0.0));
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decomposeAdaptive(signal, 1.0));
        assertThrows(InvalidArgumentException.class,
            () -> mwt.decomposeAdaptive(signal, -0.1));
    }
    
    @Test
    @DisplayName("Financial time series multi-scale analysis")
    void testFinancialTimeSeriesAnalysis() {
        // Simulate stock returns with volatility clustering
        double[] returns = new double[512];
        for (int i = 0; i < returns.length; i++) {
            double volatility = (i < 256) ? 0.01 : 0.03; // Regime change
            returns[i] = volatility * (Math.random() - 0.5);
        }
        
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(Daubechies.DB4);
        MultiLevelTransformResult result = mwt.decompose(returns, 5);
        
        // Analyze volatility at different scales
        double[] scaleVolatility = new double[5];
        for (int level = 1; level <= 5; level++) {
            scaleVolatility[level - 1] = Math.sqrt(result.detailEnergyAtLevel(level));
        }
        
        // Finer scales should capture the volatility change
        double earlyVol = 0.0;
        double lateVol = 0.0;
        
        double[] level1Details = result.detailsAtLevel(1);
        int midPoint = level1Details.length / 2;
        
        for (int i = 0; i < midPoint; i++) {
            earlyVol += level1Details[i] * level1Details[i];
        }
        for (int i = midPoint; i < level1Details.length; i++) {
            lateVol += level1Details[i] * level1Details[i];
        }
        
        assertTrue(lateVol > earlyVol,
            "Multi-level decomposition should detect volatility regime change");
    }
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 16.0) +
                       0.25 * Math.sin(2 * Math.PI * i / 8.0);
        }
        return signal;
    }
}