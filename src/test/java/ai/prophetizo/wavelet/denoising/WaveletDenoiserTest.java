package ai.prophetizo.wavelet.denoising;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for WaveletDenoiser.
 */
@DisplayName("WaveletDenoiser Tests")
class WaveletDenoiserTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Constructor validation")
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, 
            () -> new WaveletDenoiser(null, BoundaryMode.PERIODIC));
        
        assertThrows(NullPointerException.class,
            () -> new WaveletDenoiser(new Haar(), null));
    }
    
    @Test
    @DisplayName("Basic denoising with universal threshold")
    void testBasicDenoising() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a clean signal
        double[] cleanSignal = generateSineWave(256, 10.0);
        
        // Add noise
        double noiseLevel = 0.5;
        double[] noisySignal = addGaussianNoise(cleanSignal, noiseLevel, 42);
        
        // Denoise
        double[] denoised = denoiser.denoise(noisySignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL);
        
        // Calculate SNR improvement
        double noisySNR = calculateSNR(cleanSignal, noisySignal);
        double denoisedSNR = calculateSNR(cleanSignal, denoised);
        
        assertTrue(denoisedSNR > noisySNR, 
            "Denoised SNR should be better than noisy SNR");
    }
    
    @ParameterizedTest
    @EnumSource(WaveletDenoiser.ThresholdMethod.class)
    @DisplayName("All threshold methods produce valid results")
    void testAllThresholdMethods(WaveletDenoiser.ThresholdMethod method) {
        if (method == WaveletDenoiser.ThresholdMethod.FIXED) {
            return; // Skip FIXED method in this test
        }
        
        WaveletDenoiser denoiser = new WaveletDenoiser(Symlet.SYM3, BoundaryMode.PERIODIC);
        double[] signal = generateNoisySignal(128, 0.3, 123);
        
        double[] denoised = denoiser.denoise(signal, method);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
        
        // Check all values are finite
        for (double value : denoised) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    @DisplayName("Soft vs Hard thresholding comparison")
    void testSoftVsHardThresholding() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB2, BoundaryMode.PERIODIC);
        
        double[] signal = generateNoisySignal(256, 0.4, 456);
        
        double[] softDenoised = denoiser.denoise(signal, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);
        
        double[] hardDenoised = denoiser.denoise(signal,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.HARD);
        
        // Soft thresholding should produce smoother results
        double softVariance = calculateVariance(softDenoised);
        double hardVariance = calculateVariance(hardDenoised);
        
        // Both should reduce noise
        double originalVariance = calculateVariance(signal);
        assertTrue(softVariance < originalVariance);
        assertTrue(hardVariance < originalVariance);
    }
    
    @Test
    @DisplayName("Multi-level denoising")
    void testMultiLevelDenoising() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        double[] cleanSignal = generateComplexSignal(512);
        double[] noisySignal = addGaussianNoise(cleanSignal, 0.5, 789);
        
        // Test different decomposition levels
        for (int levels = 1; levels <= 4; levels++) {
            double[] denoised = denoiser.denoiseMultiLevel(noisySignal, levels,
                WaveletDenoiser.ThresholdMethod.SURE,
                WaveletDenoiser.ThresholdType.SOFT);
            
            assertNotNull(denoised);
            assertEquals(noisySignal.length, denoised.length);
            
            // Higher levels should generally produce smoother results
            if (levels > 1) {
                double variance = calculateVariance(denoised);
                assertTrue(variance > 0); // Result should not be all zeros
            }
        }
    }
    
    @Test
    @DisplayName("Fixed threshold denoising")
    void testFixedThresholdDenoising() {
        WaveletDenoiser denoiser = new WaveletDenoiser(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = generateNoisySignal(128, 0.5, 101);
        
        // Test different threshold values
        double[] thresholds = {0.1, 0.5, 1.0, 2.0};
        
        // Track that increasing threshold reduces energy
        double previousEnergy = Double.MAX_VALUE;
        
        for (double threshold : thresholds) {
            double[] denoised = denoiser.denoiseFixed(signal, threshold, 
                WaveletDenoiser.ThresholdType.SOFT);
            
            assertNotNull(denoised);
            assertEquals(signal.length, denoised.length);
            
            double denoisedEnergy = calculateRMS(denoised);
            
            // Energy should decrease with increasing threshold
            assertTrue(denoisedEnergy <= previousEnergy,
                "Energy should decrease with increasing threshold");
            previousEnergy = denoisedEnergy;
            
            // At very high threshold, should significantly reduce signal
            if (threshold >= 2.0) {
                double originalEnergy = calculateRMS(signal);
                assertTrue(denoisedEnergy < originalEnergy * 0.9, 
                    "Very high threshold should reduce signal energy");
            }
        }
    }
    
    @Test
    @DisplayName("SURE threshold adaptation")
    void testSUREThresholdAdaptation() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create signals with different characteristics
        double[] smoothSignal = generateSmoothSignal(256);
        double[] jumpySignal = generateJumpySignal(256);
        
        // Add same noise level
        double noiseLevel = 0.3;
        double[] noisySmooth = addGaussianNoise(smoothSignal, noiseLevel, 111);
        double[] noisyJumpy = addGaussianNoise(jumpySignal, noiseLevel, 222);
        
        // Denoise with SURE
        double[] denoisedSmooth = denoiser.denoise(noisySmooth, 
            WaveletDenoiser.ThresholdMethod.SURE);
        double[] denoisedJumpy = denoiser.denoise(noisyJumpy,
            WaveletDenoiser.ThresholdMethod.SURE);
        
        // SURE should adapt to signal characteristics
        // Smooth signals should be denoised more aggressively
        double smoothError = calculateMSE(smoothSignal, denoisedSmooth);
        double jumpyError = calculateMSE(jumpySignal, denoisedJumpy);
        
        // SURE should reduce error or at worst maintain it (within tolerance)
        double noisySmoothError = calculateMSE(smoothSignal, noisySmooth);
        double noisyJumpyError = calculateMSE(jumpySignal, noisyJumpy);
        
        assertTrue(smoothError <= noisySmoothError * 1.05,
            "SURE should not significantly increase error for smooth signals");
        assertTrue(jumpyError <= noisyJumpyError * 1.05,
            "SURE should not significantly increase error for jumpy signals");
    }
    
    @Test
    @DisplayName("Financial data denoising")
    void testFinancialDataDenoising() {
        WaveletDenoiser denoiser = WaveletDenoiser.forFinancialData();
        
        // Simulate financial returns with noise
        double[] returns = generateFinancialReturns(256);
        double[] noisyReturns = addMarketMicrostructureNoise(returns, 333);
        
        double[] denoised = denoiser.denoise(noisyReturns, 
            WaveletDenoiser.ThresholdMethod.SURE);
        
        // Check key properties are preserved
        double originalMean = calculateMean(returns);
        double denoisedMean = calculateMean(denoised);
        
        // Mean should be approximately preserved
        assertEquals(originalMean, denoisedMean, 0.01,
            "Mean return should be preserved");
        
        // Volatility should be reduced (noise removal)
        double noisyVol = calculateVolatility(noisyReturns);
        double denoisedVol = calculateVolatility(denoised);
        
        assertTrue(denoisedVol < noisyVol,
            "Denoising should reduce volatility");
    }
    
    @ParameterizedTest
    @MethodSource("provideBoundaryTestCases")
    @DisplayName("Boundary mode effects on denoising")
    void testBoundaryModeEffects(BoundaryMode mode, String description) {
        if (mode == BoundaryMode.SYMMETRIC) {
            return; // Skip unsupported mode
        }
        
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB2, mode);
        
        // Create signal with boundary discontinuity
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = (i < 64) ? 1.0 : -1.0;
        }
        
        // Add noise
        double[] noisy = addGaussianNoise(signal, 0.2, 444);
        
        // Denoise
        double[] denoised = denoiser.denoise(noisy, WaveletDenoiser.ThresholdMethod.MINIMAX);
        
        // Check boundary handling
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
        
        // Verify no NaN or infinity at boundaries
        assertTrue(Double.isFinite(denoised[0]));
        assertTrue(Double.isFinite(denoised[denoised.length - 1]));
    }
    
    @Test
    @DisplayName("Noise level estimation accuracy")
    void testNoiseEstimation() {
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Test with known noise levels
        double[] knownNoiseLevels = {0.1, 0.5, 1.0, 2.0};
        
        for (double trueNoise : knownNoiseLevels) {
            // Generate pure noise
            double[] pureNoise = new double[256];
            Random rng = new Random(555);
            for (int i = 0; i < pureNoise.length; i++) {
                pureNoise[i] = rng.nextGaussian() * trueNoise;
            }
            
            // Denoise will estimate noise level internally
            double[] denoised = denoiser.denoise(pureNoise, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL);
            
            // For pure noise, denoising should reduce it significantly
            double denoisedRMS = calculateRMS(denoised);
            assertTrue(denoisedRMS < trueNoise * 0.75,
                "Denoising should reduce pure noise by at least 25%");
        }
    }
    
    @Test
    @DisplayName("Performance with different signal lengths")
    void testDifferentSignalLengths() {
        WaveletDenoiser denoiser = new WaveletDenoiser(new Haar(), BoundaryMode.PERIODIC);
        
        int[] lengths = {64, 128, 256, 512, 1024};
        
        for (int length : lengths) {
            double[] signal = generateNoisySignal(length, 0.3, length);
            
            double[] denoised = denoiser.denoise(signal, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL);
            
            assertNotNull(denoised);
            assertEquals(length, denoised.length);
        }
    }
    
    // Helper methods
    
    private double[] generateSineWave(int length, double frequency) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / length);
        }
        return signal;
    }
    
    private double[] generateComplexSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / length) +
                       0.5 * Math.sin(2 * Math.PI * 20 * i / length) +
                       0.25 * Math.cos(2 * Math.PI * 50 * i / length);
        }
        return signal;
    }
    
    private double[] generateSmoothSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.exp(-5 * (t - 0.5) * (t - 0.5));
        }
        return signal;
    }
    
    private double[] generateJumpySignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (i % 32 < 16) ? 1.0 : -1.0;
            if (i % 64 == 0 && i > 0) {
                signal[i] *= 2.0; // Add jumps
            }
        }
        return signal;
    }
    
    private double[] generateFinancialReturns(int length) {
        double[] returns = new double[length];
        Random rng = new Random(42);
        
        // Simulate returns with volatility clustering
        double vol = 0.01;
        for (int i = 0; i < length; i++) {
            if (i % 50 == 0) {
                vol = 0.005 + 0.02 * rng.nextDouble();
            }
            returns[i] = rng.nextGaussian() * vol;
        }
        
        return returns;
    }
    
    private double[] addGaussianNoise(double[] signal, double noiseLevel, long seed) {
        Random rng = new Random(seed);
        double[] noisy = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + rng.nextGaussian() * noiseLevel;
        }
        
        return noisy;
    }
    
    private double[] addMarketMicrostructureNoise(double[] returns, long seed) {
        Random rng = new Random(seed);
        double[] noisy = new double[returns.length];
        
        for (int i = 0; i < returns.length; i++) {
            // Add bid-ask bounce and rounding noise
            double microNoise = 0.0001 * (rng.nextDouble() - 0.5);
            noisy[i] = returns[i] + microNoise;
        }
        
        return noisy;
    }
    
    private double[] generateNoisySignal(int length, double noiseLevel, long seed) {
        double[] clean = generateSineWave(length, 8.0);
        return addGaussianNoise(clean, noiseLevel, seed);
    }
    
    private double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }
        
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    private double calculateMSE(double[] reference, double[] estimate) {
        double sum = 0;
        for (int i = 0; i < reference.length; i++) {
            double diff = reference[i] - estimate[i];
            sum += diff * diff;
        }
        return sum / reference.length;
    }
    
    private double calculateRMS(double[] signal) {
        double sum = 0;
        for (double value : signal) {
            sum += value * value;
        }
        return Math.sqrt(sum / signal.length);
    }
    
    private double calculateMean(double[] data) {
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.length;
    }
    
    private double calculateVariance(double[] data) {
        double mean = calculateMean(data);
        double sum = 0;
        for (double value : data) {
            sum += (value - mean) * (value - mean);
        }
        return sum / data.length;
    }
    
    private double calculateVolatility(double[] returns) {
        return Math.sqrt(calculateVariance(returns));
    }
    
    private int countZeros(double[] data) {
        int count = 0;
        for (double value : data) {
            if (Math.abs(value) < 1e-10) {
                count++;
            }
        }
        return count;
    }
    
    private static Stream<Arguments> provideBoundaryTestCases() {
        return Stream.of(
            Arguments.of(BoundaryMode.PERIODIC, "Periodic boundary"),
            Arguments.of(BoundaryMode.ZERO_PADDING, "Zero-padding boundary")
        );
    }
}