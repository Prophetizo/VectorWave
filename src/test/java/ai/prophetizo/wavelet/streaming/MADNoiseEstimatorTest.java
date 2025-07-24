package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MADNoiseEstimatorTest {
    
    private MADNoiseEstimator estimator;
    
    @BeforeEach
    void setUp() {
        estimator = new MADNoiseEstimator(100, 0.9);
    }
    
    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            new MADNoiseEstimator(-1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> 
            new MADNoiseEstimator(100, -0.1));
        assertThrows(IllegalArgumentException.class, () -> 
            new MADNoiseEstimator(100, 1.1));
    }
    
    @Test
    void testNoiseEstimation() {
        // Generate Gaussian noise
        double[] noise = generateGaussianNoise(1000, 0.0, 0.1);
        
        double estimate = estimator.estimateNoise(noise);
        
        // MAD estimator should be close to true std dev for Gaussian noise
        assertEquals(0.1, estimate, 0.02);
        assertTrue(estimator.getSampleCount() > 0);
    }
    
    @Test
    void testIncrementalEstimation() {
        // Test that estimate improves with more data
        double targetStd = 0.15;
        
        // First batch
        double[] batch1 = generateGaussianNoise(50, 0.0, targetStd);
        double estimate1 = estimator.estimateNoise(batch1);
        
        // Second batch
        double[] batch2 = generateGaussianNoise(50, 0.0, targetStd);
        estimator.updateEstimate(batch2);
        double estimate2 = estimator.getCurrentNoiseLevel();
        
        // More data should stabilize estimate
        assertTrue(Math.abs(estimate2 - targetStd) <= Math.abs(estimate1 - targetStd) * 1.5);
    }
    
    @Test
    void testSlidingWindow() {
        // Fill buffer
        double[] initialNoise = generateGaussianNoise(100, 0.0, 0.1);
        estimator.estimateNoise(initialNoise);
        
        double initialEstimate = estimator.getCurrentNoiseLevel();
        
        // Add high noise data to push out old data
        double[] highNoise = generateGaussianNoise(100, 0.0, 0.5);
        estimator.updateEstimate(highNoise);
        
        double newEstimate = estimator.getCurrentNoiseLevel();
        
        // Estimate should increase but be smoothed
        assertTrue(newEstimate > initialEstimate);
        assertTrue(newEstimate < 0.5); // Due to smoothing
    }
    
    @Test
    void testThresholdCalculation() {
        // Set known noise level
        double[] noise = generateGaussianNoise(100, 0.0, 0.1);
        estimator.estimateNoise(noise);
        
        // Test different threshold methods
        double universal = estimator.getThreshold(ThresholdMethod.UNIVERSAL);
        double sure = estimator.getThreshold(ThresholdMethod.SURE);
        double minimax = estimator.getThreshold(ThresholdMethod.MINIMAX);
        
        // All should be positive and scaled from noise estimate
        assertTrue(universal > 0);
        assertTrue(sure > 0);
        assertTrue(minimax > 0);
        
        // Universal threshold should scale with log(n)
        assertTrue(universal > estimator.getCurrentNoiseLevel());
        
        // SURE is conservative in streaming context
        assertTrue(sure > universal);
        
        // Minimax is between
        assertTrue(minimax > estimator.getCurrentNoiseLevel());
        assertTrue(minimax < sure);
    }
    
    @Test
    void testReset() {
        // Add data
        double[] noise = generateGaussianNoise(100, 0.0, 0.1);
        estimator.estimateNoise(noise);
        
        assertTrue(estimator.getCurrentNoiseLevel() > 0);
        assertTrue(estimator.getSampleCount() > 0);
        
        // Reset
        estimator.reset();
        
        assertEquals(0.0, estimator.getCurrentNoiseLevel());
        assertEquals(0, estimator.getSampleCount());
    }
    
    @Test
    void testEmptyInput() {
        double estimate = estimator.estimateNoise(new double[0]);
        assertEquals(0.0, estimate);
        
        estimate = estimator.estimateNoise(null);
        assertEquals(0.0, estimate);
    }
    
    @Test
    void testRobustnessToOutliers() {
        // Generate noise with outliers
        double[] noise = generateGaussianNoise(90, 0.0, 0.1);
        
        // Add outliers
        double[] withOutliers = new double[100];
        System.arraycopy(noise, 0, withOutliers, 0, 90);
        for (int i = 90; i < 100; i++) {
            withOutliers[i] = (i % 2 == 0) ? 5.0 : -5.0; // Large outliers
        }
        
        double estimate = estimator.estimateNoise(withOutliers);
        
        // MAD should be robust to outliers
        assertTrue(estimate < 0.5); // Much less than outlier magnitude
        assertTrue(estimate > 0.05); // But still detects base noise
    }
    
    @Test
    void testSmoothing() {
        MADNoiseEstimator highSmoothing = new MADNoiseEstimator(50, 0.99);
        MADNoiseEstimator lowSmoothing = new MADNoiseEstimator(50, 0.5);
        
        // Initial estimate
        double[] batch1 = generateGaussianNoise(50, 0.0, 0.1);
        highSmoothing.estimateNoise(batch1);
        lowSmoothing.estimateNoise(batch1);
        
        // Sudden change
        double[] batch2 = generateGaussianNoise(50, 0.0, 0.5);
        highSmoothing.updateEstimate(batch2);
        lowSmoothing.updateEstimate(batch2);
        
        // Low smoothing should adapt faster
        double highSmoothEstimate = highSmoothing.getCurrentNoiseLevel();
        double lowSmoothEstimate = lowSmoothing.getCurrentNoiseLevel();
        
        assertTrue(lowSmoothEstimate > highSmoothEstimate);
    }
    
    private static double[] generateGaussianNoise(int length, double mean, double std) {
        double[] noise = new double[length];
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < length; i++) {
            noise[i] = mean + std * random.nextGaussian();
        }
        
        return noise;
    }
}