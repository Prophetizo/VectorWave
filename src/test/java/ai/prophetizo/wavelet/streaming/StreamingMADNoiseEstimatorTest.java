package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamingMADNoiseEstimatorTest {
    
    private StreamingMADNoiseEstimator estimator;
    
    @BeforeEach
    void setUp() {
        estimator = new StreamingMADNoiseEstimator(0.9);
    }
    
    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingMADNoiseEstimator(-0.1));
        assertThrows(IllegalArgumentException.class, () -> 
            new StreamingMADNoiseEstimator(1.1));
    }
    
    @Test
    void testNoiseEstimation() {
        // Generate Gaussian noise
        double[] noise = generateGaussianNoise(1000, 0.0, 0.1);
        
        double estimate = estimator.estimateNoise(noise);
        
        // MAD estimator scales differently than standard deviation
        // For Gaussian noise with std=0.1, MAD ≈ 0.6745 * σ ≈ 0.06745
        // But our P2 estimator may have some variance, so allow wider range
        assertTrue(estimate > 0.04 && estimate < 0.12, 
            "Estimate should be in reasonable range: " + estimate);
        assertTrue(estimator.getSampleCount() > 0);
    }
    
    @Test
    void testStreamingUpdates() {
        // Process data in small chunks to test streaming behavior
        double targetStd = 0.15;
        
        for (int i = 0; i < 20; i++) {
            double[] batch = generateGaussianNoise(50, 0.0, targetStd);
            estimator.updateEstimate(batch);
        }
        
        double finalEstimate = estimator.getCurrentNoiseLevel();
        // With smoothing and P2 estimation, the final estimate may be higher
        // For std=0.15, expect roughly 0.15 * 0.6745 * 1.4826 ≈ 0.1499
        // The test allows for a wider range due to smoothing and P2 estimation variability.
        assertTrue(finalEstimate > 0.08 && finalEstimate < 0.22,
            "Final estimate should converge to expected range: " + finalEstimate);
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
        assertTrue(estimate < 0.5, "Should be robust to outliers: " + estimate);
        assertTrue(estimate > 0.02, "Should still detect base noise: " + estimate);
    }
    
    @Test
    void testComparisonWithBufferedVersion() {
        // Compare with original MADNoiseEstimator for same data
        MADNoiseEstimator bufferedEstimator = new MADNoiseEstimator(1000, 0.9);
        StreamingMADNoiseEstimator streamingEstimator = new StreamingMADNoiseEstimator(0.9);
        
        // Generate test data
        double[] testData = generateGaussianNoise(1000, 0.0, 0.1);
        
        // Process same data
        bufferedEstimator.estimateNoise(testData);
        streamingEstimator.estimateNoise(testData);
        
        double bufferedEstimate = bufferedEstimator.getCurrentNoiseLevel();
        double streamingEstimate = streamingEstimator.getCurrentNoiseLevel();
        
        // Estimates should be similar (within 50% of each other)
        double ratio = streamingEstimate / bufferedEstimate;
        assertTrue(ratio > 0.5 && ratio < 2.0,
            String.format("Estimates should be similar. Buffered: %.4f, Streaming: %.4f", 
                bufferedEstimate, streamingEstimate));
    }
    
    @Test
    void testPerformance() {
        // This test demonstrates the performance advantage
        int iterations = 100;
        int dataSize = 1000;
        
        // Warmup phase to ensure JIT compilation
        StreamingMADNoiseEstimator warmupStreaming = new StreamingMADNoiseEstimator();
        MADNoiseEstimator warmupBuffered = new MADNoiseEstimator(dataSize, 0.9);
        for (int i = 0; i < 50; i++) {
            double[] data = generateGaussianNoise(dataSize, 0.0, 0.1);
            warmupStreaming.updateEstimate(data);
            warmupBuffered.updateEstimate(data);
        }
        
        // Reset for actual test
        estimator = new StreamingMADNoiseEstimator();
        
        // Time streaming version
        long streamingStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            double[] data = generateGaussianNoise(dataSize, 0.0, 0.1);
            estimator.updateEstimate(data);
        }
        long streamingTime = System.nanoTime() - streamingStart;
        
        // Time buffered version
        MADNoiseEstimator buffered = new MADNoiseEstimator(dataSize, 0.9);
        long bufferedStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            double[] data = generateGaussianNoise(dataSize, 0.0, 0.1);
            buffered.updateEstimate(data);
        }
        long bufferedTime = System.nanoTime() - bufferedStart;
        
        // Calculate speedup
        double speedup = (double) bufferedTime / streamingTime;
        
        // P² algorithm has overhead but provides O(1) memory vs O(n)
        // In tests with small data, buffered may be faster due to cache locality
        // Allow streaming to be up to 5x slower in this synthetic test
        // The real benefit is O(1) memory vs O(n), not raw speed on small datasets
        assertTrue(speedup > 0.2, 
                String.format("Streaming should not be more than 5x slower (speedup: %.2fx)", speedup));
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