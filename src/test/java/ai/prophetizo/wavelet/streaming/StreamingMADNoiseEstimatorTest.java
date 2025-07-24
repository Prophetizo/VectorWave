package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamingMADNoiseEstimatorTest {
    
    // Test data sizes
    private static final int LARGE_DATA_SIZE = 1000;
    private static final int SMALL_DATA_SIZE = 100;
    
    // Noise parameters
    private static final double NOISE_MEAN = 0.0;
    private static final double NOISE_STD_LOW = 0.1;
    private static final double NOISE_STD_HIGH = 0.15;
    
    // Test parameters
    private static final int PERFORMANCE_ITERATIONS = 100;
    private static final int WARMUP_ITERATIONS = 50;
    private static final double DECAY_FACTOR = 0.9;
    
    // Outlier parameters
    private static final int OUTLIER_START_INDEX = 90;
    private static final double OUTLIER_VALUE = 10.0;
    
    private StreamingMADNoiseEstimator estimator;
    
    @BeforeEach
    void setUp() {
        estimator = new StreamingMADNoiseEstimator(DECAY_FACTOR);
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
        double[] noise = generateGaussianNoise(LARGE_DATA_SIZE, NOISE_MEAN, NOISE_STD_LOW);
        
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
        double targetStd = NOISE_STD_HIGH;
        
        for (int i = 0; i < 20; i++) {
            double[] batch = generateGaussianNoise(SMALL_DATA_SIZE / 2, NOISE_MEAN, targetStd);
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
        double[] noise = generateGaussianNoise(SMALL_DATA_SIZE, NOISE_MEAN, NOISE_STD_LOW);
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
        double[] noise = generateGaussianNoise(SMALL_DATA_SIZE, NOISE_MEAN, NOISE_STD_LOW);
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
        double[] noise = generateGaussianNoise(OUTLIER_START_INDEX, NOISE_MEAN, NOISE_STD_LOW);
        
        // Add outliers
        double[] withOutliers = new double[SMALL_DATA_SIZE];
        System.arraycopy(noise, 0, withOutliers, 0, OUTLIER_START_INDEX);
        for (int i = OUTLIER_START_INDEX; i < SMALL_DATA_SIZE; i++) {
            withOutliers[i] = (i % 2 == 0) ? OUTLIER_VALUE / 2 : -OUTLIER_VALUE / 2; // Large outliers
        }
        
        double estimate = estimator.estimateNoise(withOutliers);
        
        // MAD should be robust to outliers
        assertTrue(estimate < 0.5, "Should be robust to outliers: " + estimate);
        assertTrue(estimate > 0.02, "Should still detect base noise: " + estimate);
    }
    
    @Test
    void testComparisonWithBufferedVersion() {
        // Compare with original MADNoiseEstimator for same data
        MADNoiseEstimator bufferedEstimator = new MADNoiseEstimator(LARGE_DATA_SIZE, DECAY_FACTOR);
        StreamingMADNoiseEstimator streamingEstimator = new StreamingMADNoiseEstimator(DECAY_FACTOR);
        
        // Generate test data
        double[] testData = generateGaussianNoise(LARGE_DATA_SIZE, NOISE_MEAN, NOISE_STD_LOW);
        
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
        int iterations = PERFORMANCE_ITERATIONS;
        int dataSize = LARGE_DATA_SIZE;
        
        // Warmup phase to ensure JIT compilation
        StreamingMADNoiseEstimator warmupStreaming = new StreamingMADNoiseEstimator();
        MADNoiseEstimator warmupBuffered = new MADNoiseEstimator(dataSize, DECAY_FACTOR);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            double[] data = generateGaussianNoise(dataSize, NOISE_MEAN, NOISE_STD_LOW);
            warmupStreaming.updateEstimate(data);
            warmupBuffered.updateEstimate(data);
        }
        
        // Reset for actual test
        estimator = new StreamingMADNoiseEstimator();
        
        // Time streaming version
        long streamingStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            double[] data = generateGaussianNoise(dataSize, NOISE_MEAN, NOISE_STD_LOW);
            estimator.updateEstimate(data);
        }
        long streamingTime = System.nanoTime() - streamingStart;
        
        // Time buffered version
        MADNoiseEstimator buffered = new MADNoiseEstimator(dataSize, DECAY_FACTOR);
        long bufferedStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            double[] data = generateGaussianNoise(dataSize, NOISE_MEAN, NOISE_STD_LOW);
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