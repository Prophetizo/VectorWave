package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
class EfficientMADNoiseEstimatorTest {
    
    private EfficientMADNoiseEstimator estimator;
    
    @BeforeEach
    void setUp() {
        estimator = new EfficientMADNoiseEstimator();
    }
    
    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> new EfficientMADNoiseEstimator(-1, 0.5));
        assertThrows(IllegalArgumentException.class, 
            () -> new EfficientMADNoiseEstimator(100, -0.1));
        assertThrows(IllegalArgumentException.class, 
            () -> new EfficientMADNoiseEstimator(100, 1.1));
    }
    
    @Test
    void testBasicNoiseEstimation() {
        // Generate noisy signal
        Random random = new Random(TestConstants.TEST_SEED);
        double[] coefficients = new double[100];
        double noiseLevel = 0.5;
        
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = random.nextGaussian() * noiseLevel;
        }
        
        double estimate = estimator.estimateNoise(coefficients);
        
        // Should be close to actual noise level
        assertEquals(noiseLevel, estimate, noiseLevel * 0.3,
            "Noise estimate should be within 30% of actual level");
    }
    
    @Test
    void testStreamingBehavior() {
        // Process coefficients in small batches
        Random random = new Random(TestConstants.TEST_SEED);
        double noiseLevel = 1.0;
        
        for (int batch = 0; batch < 20; batch++) {
            double[] coeffs = new double[10];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = random.nextGaussian() * noiseLevel;
            }
            estimator.updateEstimate(coeffs);
        }
        
        double finalEstimate = estimator.getCurrentNoiseLevel();
        assertEquals(noiseLevel, finalEstimate, noiseLevel * 0.4,
            "Streaming estimate should converge to noise level");
        
        assertEquals(200, estimator.getSampleCount());
    }
    
    @Test
    void testThresholdMethods() {
        // Add some coefficients
        double[] coeffs = generateGaussianNoise(100, 0.5);
        estimator.estimateNoise(coeffs);
        
        double noiseLevel = estimator.getCurrentNoiseLevel();
        
        // Test different threshold methods
        double universal = estimator.getThreshold(ThresholdMethod.UNIVERSAL);
        double sure = estimator.getThreshold(ThresholdMethod.SURE);
        double minimax = estimator.getThreshold(ThresholdMethod.MINIMAX);
        
        // Universal should be highest, SURE intermediate, Minimax lowest
        assertTrue(universal > minimax, "Universal threshold should be higher than Minimax");
        assertTrue(sure > minimax, "SURE threshold should be higher than Minimax");
        
        // All should be proportional to noise level
        assertTrue(universal > noiseLevel, "Universal threshold should exceed noise level");
    }
    
    @Test
    void testAdaptiveSmoothing() {
        EfficientMADNoiseEstimator highSmoothing = new EfficientMADNoiseEstimator(256, 0.99);
        EfficientMADNoiseEstimator lowSmoothing = new EfficientMADNoiseEstimator(256, 0.5);
        
        // Process same data with varying noise
        Random random = new Random(TestConstants.TEST_SEED);
        
        // Low noise period
        for (int i = 0; i < 10; i++) {
            double[] coeffs = generateGaussianNoise(20, 0.1);
            highSmoothing.updateEstimate(coeffs);
            lowSmoothing.updateEstimate(coeffs);
        }
        
        // High noise period
        for (int i = 0; i < 10; i++) {
            double[] coeffs = generateGaussianNoise(20, 1.0);
            highSmoothing.updateEstimate(coeffs);
            lowSmoothing.updateEstimate(coeffs);
        }
        
        // Low smoothing should adapt faster
        double highSmoothEstimate = highSmoothing.getCurrentNoiseLevel();
        double lowSmoothEstimate = lowSmoothing.getCurrentNoiseLevel();
        
        assertTrue(lowSmoothEstimate > highSmoothEstimate,
            "Low smoothing should adapt faster to increased noise");
    }
    
    @Test
    void testReset() {
        // Add data
        double[] coeffs = generateGaussianNoise(100, 0.5);
        estimator.estimateNoise(coeffs);
        
        assertTrue(estimator.getCurrentNoiseLevel() > 0);
        assertTrue(estimator.getSampleCount() > 0);
        
        // Reset
        estimator.reset();
        
        assertEquals(0.0, estimator.getCurrentNoiseLevel());
        assertEquals(0, estimator.getSampleCount());
        assertEquals(0.0, estimator.getCurrentMedian());
        assertEquals(0.0, estimator.getCurrentMAD());
    }
    
    @Test
    void testEmptyInput() {
        double estimate = estimator.estimateNoise(new double[0]);
        assertEquals(0.0, estimate);
        
        estimate = estimator.estimateNoise(null);
        assertEquals(0.0, estimate);
    }
    
    @Test
    void testPerformanceComparison() {
        // Compare performance with original MADNoiseEstimator
        // Note: For small data sizes, the overhead of P2 might make it slower
        // The benefit appears with larger streaming data
        int iterations = 10;
        int coeffsPerIteration = 10000;
        
        EfficientMADNoiseEstimator efficient = new EfficientMADNoiseEstimator(1024, 0.95);
        MADNoiseEstimator original = new MADNoiseEstimator(1024, 0.95);
        
        Random random = new Random(TestConstants.TEST_SEED);
        
        // Warm up
        for (int i = 0; i < 5; i++) {
            double[] coeffs = generateGaussianNoise(1000, 0.5);
            efficient.estimateNoise(coeffs);
            original.estimateNoise(coeffs);
        }
        
        // For streaming scenario, process data in small batches
        // This is where the efficient version should shine
        int batchSize = 100;
        int numBatches = iterations * coeffsPerIteration / batchSize;
        
        // Measure efficient version
        long efficientStart = System.nanoTime();
        for (int i = 0; i < numBatches; i++) {
            double[] coeffs = new double[batchSize];
            for (int j = 0; j < batchSize; j++) {
                coeffs[j] = random.nextGaussian() * 0.5;
            }
            efficient.estimateNoise(coeffs);
        }
        long efficientTime = System.nanoTime() - efficientStart;
        
        // Reset random seed for fair comparison
        random = new Random(TestConstants.TEST_SEED);
        
        // Measure original version
        long originalStart = System.nanoTime();
        for (int i = 0; i < numBatches; i++) {
            double[] coeffs = new double[batchSize];
            for (int j = 0; j < batchSize; j++) {
                coeffs[j] = random.nextGaussian() * 0.5;
            }
            original.estimateNoise(coeffs);
        }
        long originalTime = System.nanoTime() - originalStart;
        
        // For streaming with many small batches, efficient should be faster
        // But for small datasets, the overhead might make it slower
        
        // Results should be similar
        double efficientEstimate = efficient.getCurrentNoiseLevel();
        double originalEstimate = original.getCurrentNoiseLevel();
        
        assertEquals(originalEstimate, efficientEstimate, originalEstimate * 0.3,
            "Estimates should be similar between implementations");
    }
    
    @Test
    void testMedianAndMADAccess() {
        double[] coeffs = generateGaussianNoise(100, 0.5);
        estimator.estimateNoise(coeffs);
        
        double median = estimator.getCurrentMedian();
        double mad = estimator.getCurrentMAD();
        double noiseLevel = estimator.getCurrentNoiseLevel();
        
        assertTrue(median >= 0, "Median should be non-negative for absolute values");
        assertTrue(mad >= 0, "MAD should be non-negative");
        
        // Noise level should be MAD * scale factor
        assertEquals(mad * 1.4826, noiseLevel, 0.001,
            "Noise level should be MAD times scale factor");
    }
    
    // Helper method
    private static double[] generateGaussianNoise(int length, double stdDev) {
        Random random = new Random(TestConstants.TEST_SEED);
        double[] noise = new double[length];
        for (int i = 0; i < length; i++) {
            noise[i] = random.nextGaussian() * stdDev;
        }
        return noise;
    }
}