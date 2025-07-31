package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
class P2QuantileEstimatorTest {
    
    private static final double TOLERANCE = 0.1; // 10% tolerance for estimates
    
    @Test
    void testMedianEstimation() {
        P2QuantileEstimator estimator = P2QuantileEstimator.forMedian();
        
        // Test with known data
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (double x : data) {
            estimator.update(x);
        }
        
        // Exact median is 5.5
        double estimate = estimator.getQuantile();
        assertEquals(5.5, estimate, 0.5, "Median estimate should be close to 5.5");
    }
    
    @Test
    void testQuantileEstimation() {
        // Test various quantiles
        double[] quantiles = {0.1, 0.25, 0.5, 0.75, 0.9};
        
        for (double q : quantiles) {
            P2QuantileEstimator estimator = new P2QuantileEstimator(q);
            
            // Generate sorted data 1 to 100
            for (int i = 1; i <= 100; i++) {
                estimator.update(i);
            }
            
            double estimate = estimator.getQuantile();
            double expected = q * 100;
            
            assertEquals(expected, estimate, expected * TOLERANCE,
                String.format("Quantile %.2f estimate should be close to %.1f", q, expected));
        }
    }
    
    @Test
    void testStreamingAccuracy() {
        P2QuantileEstimator p2Estimator = P2QuantileEstimator.forMedian();
        
        // Generate random data
        Random random = new Random(TestConstants.TEST_SEED);
        double[] data = new double[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextGaussian();
            p2Estimator.update(data[i]);
        }
        
        // Calculate exact median
        Arrays.sort(data);
        double exactMedian = (data[499] + data[500]) / 2.0;
        
        // Compare with P² estimate
        double p2Estimate = p2Estimator.getQuantile();
        
        assertEquals(exactMedian, p2Estimate, Math.abs(exactMedian) * TOLERANCE,
            "P² estimate should be within tolerance of exact median");
    }
    
    @Test
    void testInitializationPhase() {
        P2QuantileEstimator estimator = P2QuantileEstimator.forMedian();
        
        // Test with fewer than 5 observations
        estimator.update(3);
        estimator.update(1);
        estimator.update(4);
        
        // Should return exact median
        double median = estimator.getQuantile();
        assertEquals(3.0, median, 0.001, "Should return exact median for small samples");
        
        // Add more observations
        estimator.update(2);
        estimator.update(5);
        
        // Now should switch to P² algorithm
        median = estimator.getQuantile();
        assertEquals(3.0, median, 0.001, "Should return correct median after initialization");
    }
    
    @Test
    void testExtremeValues() {
        P2QuantileEstimator estimator = P2QuantileEstimator.forMedian();
        
        // Add normal values
        for (int i = 0; i < 100; i++) {
            estimator.update(i);
        }
        
        // Add extreme values
        estimator.update(1000);
        estimator.update(-1000);
        
        double median = estimator.getQuantile();
        
        // Median should still be around 50 despite extremes
        assertTrue(median > 40 && median < 60,
            "Median should be robust to extreme values");
    }
    
    @Test
    void testMonotonicSequence() {
        P2QuantileEstimator estimator = new P2QuantileEstimator(0.9); // 90th percentile
        
        // Add monotonically increasing values
        for (int i = 1; i <= 100; i++) {
            estimator.update(i);
        }
        
        double p90 = estimator.getQuantile();
        assertEquals(90.0, p90, 5.0, "90th percentile should be around 90");
    }
    
    @Test
    void testReset() {
        P2QuantileEstimator estimator = P2QuantileEstimator.forMedian();
        
        // Add some data
        for (int i = 0; i < 10; i++) {
            estimator.update(i);
        }
        
        assertTrue(estimator.getCount() > 0);
        assertTrue(estimator.getQuantile() > 0);
        
        // Reset
        estimator.reset();
        
        assertEquals(0, estimator.getCount());
        assertEquals(0.0, estimator.getQuantile());
        
        // Should work normally after reset
        for (int i = 0; i < 10; i++) {
            estimator.update(i + 10);
        }
        
        assertTrue(estimator.getQuantile() > 10);
    }
    
    @Test
    void testInvalidQuantile() {
        assertThrows(IllegalArgumentException.class, () -> new P2QuantileEstimator(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new P2QuantileEstimator(1.1));
    }
    
    @RepeatedTest(5)
    void testConsistency() {
        // Test that multiple runs with same data produce similar results
        P2QuantileEstimator estimator1 = P2QuantileEstimator.forMedian();
        P2QuantileEstimator estimator2 = P2QuantileEstimator.forMedian();
        
        Random random = new Random(12345);
        
        for (int i = 0; i < 1000; i++) {
            double value = random.nextDouble() * 100;
            estimator1.update(value);
            estimator2.update(value);
        }
        
        double median1 = estimator1.getQuantile();
        double median2 = estimator2.getQuantile();
        
        assertEquals(median1, median2, 0.001,
            "Same data should produce same results");
    }
    
    @Test
    void testLargeDataset() {
        P2QuantileEstimator estimator = new P2QuantileEstimator(0.75); // 75th percentile
        
        // Test with large dataset
        Random random = new Random(TestConstants.TEST_SEED);
        int n = 100000;
        
        for (int i = 0; i < n; i++) {
            estimator.update(random.nextGaussian());
        }
        
        // For standard normal distribution, 75th percentile ≈ 0.674
        double p75 = estimator.getQuantile();
        assertEquals(0.674, p75, 0.1, "75th percentile of normal distribution");
        
        assertEquals(n, estimator.getCount());
    }
}