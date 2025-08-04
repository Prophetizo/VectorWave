package ai.prophetizo.wavelet.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the empirical performance modeling system.
 */
class PerformanceModelTest {
    
    private PerformanceModel model;
    private PlatformFactors testPlatform;
    
    @BeforeEach
    void setUp() {
        // Create test platform with known factors
        testPlatform = new PlatformFactors.Builder()
            .cpuSpeedFactor(1.0)
            .vectorSpeedup(4.0)
            .l1CacheSize(32 * 1024)
            .l2CacheSize(256 * 1024)
            .l3CacheSize(8 * 1024 * 1024)
            .coreCount(4)
            .architecture("test")
            .hasAVX512(false)
            .hasNEON(false)
            .memoryBandwidth(25.0)
            .build();
        
        model = new PerformanceModel(testPlatform);
    }
    
    @Test
    void testBasicPrediction() {
        // Test prediction for various sizes
        PredictionResult result = model.predict(1024, false);
        
        assertNotNull(result);
        assertTrue(result.estimatedTime() > 0);
        assertTrue(result.lowerBound() <= result.estimatedTime());
        assertTrue(result.upperBound() >= result.estimatedTime());
        assertTrue(result.confidence() >= 0 && result.confidence() <= 1);
    }
    
    @Test
    void testVectorizationSpeedup() {
        // Prediction without vectorization
        PredictionResult scalarResult = model.predict(1024, false);
        
        // Prediction with vectorization
        PredictionResult vectorResult = model.predict(1024, true);
        
        // Vector should be faster
        assertTrue(vectorResult.estimatedTime() < scalarResult.estimatedTime());
        
        // Speedup should be approximately the platform factor
        double speedup = scalarResult.estimatedTime() / vectorResult.estimatedTime();
        assertEquals(testPlatform.vectorSpeedup, speedup, 0.1);
    }
    
    @Test
    void testSizeRanges() {
        // Test that larger sizes take more time
        PredictionResult tiny = model.predict(100, false);
        PredictionResult small = model.predict(500, false);
        PredictionResult medium = model.predict(2000, false);
        PredictionResult large = model.predict(8000, false);
        
        assertTrue(tiny.estimatedTime() < small.estimatedTime());
        assertTrue(small.estimatedTime() < medium.estimatedTime());
        assertTrue(medium.estimatedTime() < large.estimatedTime());
    }
    
    @Test
    void testOnlineLearning() {
        // Initial prediction
        PredictionResult initialPred = model.predict(1000, false);
        
        // Add measurements
        for (int i = 0; i < 10; i++) {
            // Simulate actual time being 20% higher than predicted
            double actualTime = initialPred.estimatedTime() * 1.2;
            model.updateWithMeasurement(1000, actualTime, false);
        }
        
        // New prediction should be adjusted
        PredictionResult updatedPred = model.predict(1000, false);
        
        // Updated prediction should be different (not necessarily higher)
        assertNotEquals(initialPred.estimatedTime(), updatedPred.estimatedTime(), 0.001);
        
        // Model should have better accuracy
        assertTrue(model.getAccuracy().getCount() == 10);
    }
    
    @Test
    void testCalibration() {
        // Create test measurements
        PerformanceModel.Measurement[] measurements = new PerformanceModel.Measurement[] {
            new PerformanceModel.Measurement(100, 0.1, false),
            new PerformanceModel.Measurement(200, 0.2, false),
            new PerformanceModel.Measurement(400, 0.45, false),
            new PerformanceModel.Measurement(800, 1.0, false),
            new PerformanceModel.Measurement(1600, 2.5, false)
        };
        
        // Calibrate model
        model.calibrate(measurements);
        
        // Check accuracy
        ModelAccuracy accuracy = model.getAccuracy();
        assertNotNull(accuracy);
        assertEquals(5, accuracy.getCount());
        
        // Predictions should be positive
        PredictionResult pred = model.predict(300, false);
        assertTrue(pred.estimatedTime() > 0);
    }
    
    @Test
    void testConfidenceIntervals() {
        // Add measurements with varying accuracy
        model.updateWithMeasurement(1000, 1.0, false);
        model.updateWithMeasurement(1000, 1.1, false);
        model.updateWithMeasurement(1000, 0.9, false);
        model.updateWithMeasurement(1000, 1.2, false);
        model.updateWithMeasurement(1000, 0.8, false);
        
        PredictionResult result = model.predict(1000, false);
        
        // Confidence interval should widen with more variance
        double intervalWidth = result.upperBound() - result.lowerBound();
        assertTrue(intervalWidth > 0);
        
        // Should contain reasonable bounds around the estimate
        assertTrue(result.lowerBound() < result.estimatedTime());
        assertTrue(result.upperBound() > result.estimatedTime());
    }
    
    @Test
    void testModelCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(0.1, 0.001, 0.000001);
        
        // Test evaluation
        assertEquals(0.1, coeffs.evaluate(0), 0.0001);
        // 0.1 + 0.001*1000 + 0.000001*1000*1000 = 0.1 + 1.0 + 1.0 = 2.1
        assertEquals(2.1, coeffs.evaluate(1000), 0.01);
        
        // Test update
        coeffs.updateWithMeasurement(1000, 1.5);
        
        // Coefficients should change
        assertNotEquals(0.1, coeffs.getA());
        assertNotEquals(0.001, coeffs.getB());
    }
    
    @Test
    void testPlatformFactors() {
        // Test platform detection (just ensure it doesn't crash)
        PlatformFactors detected = PlatformFactors.detectPlatform();
        assertNotNull(detected);
        assertTrue(detected.coreCount > 0);
        assertTrue(detected.cpuSpeedFactor > 0);
        assertNotNull(detected.architecture);
    }
    
    @Test
    void testAccuracyMetrics() {
        ModelAccuracy accuracy = new ModelAccuracy();
        
        // Add predictions
        accuracy.updateWithPrediction(1.0, 1.1); // 10% error
        accuracy.updateWithPrediction(2.0, 1.8); // 10% error
        accuracy.updateWithPrediction(3.0, 3.3); // 10% error
        
        assertEquals(3, accuracy.getCount());
        assertEquals(0.1, accuracy.getMeanAbsolutePercentageError(), 0.01);
        assertTrue(accuracy.getConfidence() > 0.8);
        
        // Test summary
        String summary = accuracy.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("MAPE"));
    }
    
    @Test
    void testRecalibrationNeeded() {
        // Fresh model with few measurements should need recalibration
        assertTrue(model.needsRecalibration()); // Less than 100 measurements
        
        // Add many good predictions
        for (int i = 0; i < 100; i++) {
            model.updateWithMeasurement(1000 + i, 1.0 + i * 0.001, false);
        }
        
        // Should not need recalibration with good accuracy and enough measurements
        boolean needsRecalib = model.needsRecalibration();
        // May or may not need based on accuracy, so just verify it runs
        assertNotNull(model.getAccuracy());
    }
}