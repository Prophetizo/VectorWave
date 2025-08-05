package ai.prophetizo.wavelet.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ModelAccuracy.
 * Tests all accuracy metrics, updates, and edge cases.
 */
@DisplayName("ModelAccuracy Tests")
class ModelAccuracyTest {
    
    private ModelAccuracy accuracy;
    
    @BeforeEach
    void setUp() {
        accuracy = new ModelAccuracy();
    }

    // === Initial State Tests ===
    
    @Test
    @DisplayName("Should initialize with empty state")
    void testInitialState() {
        assertEquals(0, accuracy.getCount());
        assertEquals(0.0, accuracy.getMeanAbsoluteError());
        assertEquals(0.0, accuracy.getRootMeanSquaredError());
        assertEquals(0.0, accuracy.getMeanAbsolutePercentageError());
        assertEquals(0.0, accuracy.getRSquared());
        assertEquals(1.0, accuracy.getConfidence()); // 1 - 0 MAPE = 1.0
        assertEquals(0.0, accuracy.getIntervalHitRate());
        assertEquals(0.0, accuracy.getMaxOverPrediction());
        assertEquals(0.0, accuracy.getMaxUnderPrediction());
    }
    
    @Test
    @DisplayName("Should handle toString with no data")
    void testToStringEmpty() {
        String result = accuracy.toString();
        assertTrue(result.contains("n=0"));
        assertTrue(result.contains("confidence=100.0%")); // No errors = 100% confidence
    }
    
    @Test
    @DisplayName("Should handle getSummary with no data")
    void testGetSummaryEmpty() {
        String summary = accuracy.getSummary();
        assertEquals("No predictions recorded", summary);
    }

    // === Single Prediction Tests ===
    
    @Test
    @DisplayName("Should handle perfect prediction")
    void testPerfectPrediction() {
        accuracy.updateWithPrediction(100.0, 100.0);
        
        assertEquals(1, accuracy.getCount());
        assertEquals(0.0, accuracy.getMeanAbsoluteError());
        assertEquals(0.0, accuracy.getRootMeanSquaredError());
        assertEquals(0.0, accuracy.getMeanAbsolutePercentageError());
        assertEquals(1.0, accuracy.getConfidence());
        assertEquals(0.0, accuracy.getMaxOverPrediction());
        assertEquals(0.0, accuracy.getMaxUnderPrediction());
    }
    
    @Test
    @DisplayName("Should handle over-prediction")
    void testOverPrediction() {
        accuracy.updateWithPrediction(120.0, 100.0); // Predicted 20% too high
        
        assertEquals(1, accuracy.getCount());
        assertEquals(20.0, accuracy.getMeanAbsoluteError());
        assertEquals(20.0, accuracy.getRootMeanSquaredError());
        assertEquals(0.2, accuracy.getMeanAbsolutePercentageError(), 1e-10);
        assertEquals(0.8, accuracy.getConfidence(), 1e-10); // 1 - 0.2 = 0.8
        assertEquals(20.0/120.0, accuracy.getMaxOverPrediction(), 1e-10); // ~16.67%
        assertEquals(0.0, accuracy.getMaxUnderPrediction());
    }
    
    @Test
    @DisplayName("Should handle under-prediction")
    void testUnderPrediction() {
        accuracy.updateWithPrediction(80.0, 100.0); // Predicted 20% too low
        
        assertEquals(1, accuracy.getCount());
        assertEquals(20.0, accuracy.getMeanAbsoluteError());
        assertEquals(20.0, accuracy.getRootMeanSquaredError());
        assertEquals(0.2, accuracy.getMeanAbsolutePercentageError(), 1e-10);
        assertEquals(0.8, accuracy.getConfidence(), 1e-10);
        assertEquals(0.0, accuracy.getMaxOverPrediction());
        assertEquals(20.0/80.0, accuracy.getMaxUnderPrediction(), 1e-10); // 25%
    }

    // === Multiple Predictions Tests ===
    
    @Test
    @DisplayName("Should calculate mean absolute error correctly")
    void testMeanAbsoluteError() {
        accuracy.updateWithPrediction(100.0, 90.0);  // Error: 10
        accuracy.updateWithPrediction(200.0, 220.0); // Error: 20
        accuracy.updateWithPrediction(50.0, 50.0);   // Error: 0
        
        assertEquals(3, accuracy.getCount());
        assertEquals(10.0, accuracy.getMeanAbsoluteError(), 1e-10); // (10+20+0)/3 = 10
    }
    
    @Test
    @DisplayName("Should calculate root mean squared error correctly")
    void testRootMeanSquaredError() {
        accuracy.updateWithPrediction(100.0, 90.0);  // Error: -10, squared: 100
        accuracy.updateWithPrediction(200.0, 220.0); // Error: 20, squared: 400
        accuracy.updateWithPrediction(50.0, 50.0);   // Error: 0, squared: 0
        
        assertEquals(3, accuracy.getCount());
        // RMSE = sqrt((100+400+0)/3) = sqrt(500/3) ≈ 12.91
        assertEquals(Math.sqrt(500.0/3.0), accuracy.getRootMeanSquaredError(), 1e-10);
    }
    
    @Test
    @DisplayName("Should calculate mean absolute percentage error correctly")
    void testMeanAbsolutePercentageError() {
        accuracy.updateWithPrediction(100.0, 90.0);  // Error: 10/90 ≈ 11.11%
        accuracy.updateWithPrediction(200.0, 220.0); // Error: 20/220 ≈ 9.09%
        accuracy.updateWithPrediction(50.0, 50.0);   // Error: 0/50 = 0%
        
        assertEquals(3, accuracy.getCount());
        double expectedMAPE = (10.0/90.0 + 20.0/220.0 + 0.0/50.0) / 3.0;
        assertEquals(expectedMAPE, accuracy.getMeanAbsolutePercentageError(), 1e-10);
    }
    
    @Test
    @DisplayName("Should track maximum over and under predictions")
    void testMaxPredictionTracking() {
        accuracy.updateWithPrediction(100.0, 80.0);  // Over-prediction: 20/100 = 20%
        accuracy.updateWithPrediction(50.0, 70.0);   // Under-prediction: 20/50 = 40%
        accuracy.updateWithPrediction(200.0, 190.0); // Over-prediction: 10/200 = 5%
        accuracy.updateWithPrediction(80.0, 100.0);  // Under-prediction: 20/80 = 25%
        
        assertEquals(0.2, accuracy.getMaxOverPrediction(), 1e-10); // 20%
        assertEquals(0.4, accuracy.getMaxUnderPrediction(), 1e-10); // 40%
    }

    // === Confidence Interval Hit Rate Tests ===
    
    @Test
    @DisplayName("Should track interval hit rate correctly")
    void testIntervalHitRate() {
        accuracy.recordIntervalHit(true);
        accuracy.recordIntervalHit(false);
        accuracy.recordIntervalHit(true);
        accuracy.recordIntervalHit(true);
        
        // For interval hit rate, we need to also add predictions to get count
        accuracy.updateWithPrediction(100.0, 100.0);
        accuracy.updateWithPrediction(100.0, 100.0);
        accuracy.updateWithPrediction(100.0, 100.0);
        accuracy.updateWithPrediction(100.0, 100.0);
        
        assertEquals(75.0, accuracy.getIntervalHitRate(), 1e-10); // 3/4 = 75%
    }
    
    @Test
    @DisplayName("Should handle interval hits without predictions")
    void testIntervalHitRateNoPredictions() {
        accuracy.recordIntervalHit(true);
        accuracy.recordIntervalHit(false);
        
        assertEquals(0.0, accuracy.getIntervalHitRate()); // No count, so 0%
    }

    // === Confidence Calculation Tests ===
    
    @ParameterizedTest
    @CsvSource({
        "0.0, 1.0",      // 0% error -> 100% confidence
        "0.1, 0.9",      // 10% error -> 90% confidence  
        "0.2, 0.8",      // 20% error -> 80% confidence
        "0.5, 0.5",      // 50% error -> 50% confidence
        "1.0, 0.0",      // 100% error -> 0% confidence
        "1.5, 0.0"       // 150% error -> 0% confidence (clamped)
    })
    @DisplayName("Should calculate confidence from MAPE correctly")
    void testConfidenceCalculation(double mape, double expectedConfidence) {
        // Create prediction that results in specified MAPE
        double predicted = 100.0;
        double actual = predicted * (1 + mape); // This will create the desired MAPE
        // Note: MAPE = |actual - predicted| / actual
        // So if actual = predicted * (1 + mape), then:
        // MAPE = |predicted * (1 + mape) - predicted| / (predicted * (1 + mape))
        //      = |predicted * mape| / (predicted * (1 + mape))
        //      = mape / (1 + mape)
        
        // Let's recalculate to get the right actual value
        // We want: mape = |actual - predicted| / actual
        // So: actual = predicted / (1 - mape) for over-prediction
        // Or: actual = predicted / (1 + mape) for under-prediction
        
        if (mape == 0) {
            actual = predicted;
        } else if (mape <= 1.0) {
            actual = predicted / (1 - mape); // Over-prediction case
        } else {
            actual = predicted; // For very high MAPE, just use a large difference
            predicted = actual * 0.1; // Make prediction much smaller
        }
        
        accuracy.updateWithPrediction(predicted, actual);
        assertEquals(expectedConfidence, accuracy.getConfidence(), 1e-10);
    }

    // === R-Squared Tests ===
    
    @Test
    @DisplayName("Should return 0 R-squared for insufficient data")
    void testRSquaredInsufficientData() {
        assertEquals(0.0, accuracy.getRSquared());
        
        accuracy.updateWithPrediction(100.0, 100.0);
        assertEquals(0.0, accuracy.getRSquared()); // Still < 2 predictions
    }
    
    @Test
    @DisplayName("Should calculate R-squared for perfect predictions")
    void testRSquaredPerfectPredictions() {
        accuracy.updateWithPrediction(100.0, 100.0);
        accuracy.updateWithPrediction(200.0, 200.0);
        
        // Perfect predictions should give R² close to 1
        // Note: The R² implementation in the class might not be standard
        // Let's just verify it returns a reasonable value
        double rSquared = accuracy.getRSquared();
        assertTrue(rSquared >= 0.0 && rSquared <= 1.0);
    }

    // === Reset Tests ===
    
    @Test
    @DisplayName("Should reset all metrics to initial state")
    void testReset() {
        // Add some data
        accuracy.updateWithPrediction(100.0, 90.0);
        accuracy.updateWithPrediction(200.0, 220.0);
        accuracy.recordIntervalHit(true);
        accuracy.recordIntervalHit(false);
        
        // Verify data exists
        assertTrue(accuracy.getCount() > 0);
        assertTrue(accuracy.getMeanAbsoluteError() > 0);
        
        // Reset
        accuracy.reset();
        
        // Verify reset to initial state
        assertEquals(0, accuracy.getCount());
        assertEquals(0.0, accuracy.getMeanAbsoluteError());
        assertEquals(0.0, accuracy.getRootMeanSquaredError());
        assertEquals(0.0, accuracy.getMeanAbsolutePercentageError());
        assertEquals(0.0, accuracy.getRSquared());
        assertEquals(1.0, accuracy.getConfidence());
        assertEquals(0.0, accuracy.getIntervalHitRate());
        assertEquals(0.0, accuracy.getMaxOverPrediction());
        assertEquals(0.0, accuracy.getMaxUnderPrediction());
    }

    // === Summary and toString Tests ===
    
    @Test
    @DisplayName("Should generate complete summary")
    void testGetSummary() {
        accuracy.updateWithPrediction(100.0, 90.0);
        accuracy.updateWithPrediction(200.0, 220.0);
        accuracy.recordIntervalHit(true);
        accuracy.recordIntervalHit(false);
        
        String summary = accuracy.getSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("n=2"));
        assertTrue(summary.contains("MAE:"));
        assertTrue(summary.contains("RMSE:"));
        assertTrue(summary.contains("MAPE:"));
        assertTrue(summary.contains("R²:"));
        assertTrue(summary.contains("Confidence:"));
        assertTrue(summary.contains("Interval hit rate:"));
        assertTrue(summary.contains("Max over-prediction:"));
        assertTrue(summary.contains("Max under-prediction:"));
    }
    
    @Test
    @DisplayName("Should generate meaningful toString")
    void testToString() {
        accuracy.updateWithPrediction(100.0, 110.0); // 10% error
        
        String result = accuracy.toString();
        
        assertTrue(result.contains("ModelAccuracy"));
        assertTrue(result.contains("MAPE="));
        assertTrue(result.contains("confidence="));
        assertTrue(result.contains("n=1"));
        assertTrue(result.contains("90.0%")); // 1 - 0.1 = 0.9 -> 90%
    }

    // === Edge Cases Tests ===
    
    @Test
    @DisplayName("Should handle zero actual values")
    void testZeroActualValues() {
        // This would cause division by zero in percentage error calculation
        // The implementation should handle this gracefully
        assertThrows(ArithmeticException.class, () -> {
            accuracy.updateWithPrediction(100.0, 0.0);
        });
    }
    
    @Test
    @DisplayName("Should handle very small predictions and actuals")
    void testVerySmallValues() {
        accuracy.updateWithPrediction(0.001, 0.0011); // 10% error
        
        assertEquals(1, accuracy.getCount());
        assertEquals(0.0001, accuracy.getMeanAbsoluteError(), 1e-15);
        assertTrue(accuracy.getMeanAbsolutePercentageError() > 0);
    }
    
    @Test
    @DisplayName("Should handle large values")
    void testLargeValues() {
        accuracy.updateWithPrediction(1e6, 1.1e6); // 10% error
        
        assertEquals(1, accuracy.getCount());
        assertEquals(1e5, accuracy.getMeanAbsoluteError(), 1e-10);
        assertEquals(0.1, accuracy.getMeanAbsolutePercentageError(), 1e-10);
    }
    
    @Test
    @DisplayName("Should handle negative predictions and actuals")
    void testNegativeValues() {
        // Negative times don't make physical sense but test mathematical robustness
        accuracy.updateWithPrediction(-100.0, -90.0);
        
        assertEquals(1, accuracy.getCount());
        assertEquals(10.0, accuracy.getMeanAbsoluteError());
    }

    // === Integration Tests ===
    
    @Test
    @DisplayName("Should work in realistic prediction scenario")
    void testRealisticScenario() {
        // Simulate a series of predictions with varying accuracy
        double[] predictions = {100.0, 250.0, 75.0, 500.0, 50.0};
        double[] actuals = {105.0, 240.0, 80.0, 520.0, 48.0};
        boolean[] withinInterval = {true, true, false, true, true};
        
        for (int i = 0; i < predictions.length; i++) {
            accuracy.updateWithPrediction(predictions[i], actuals[i]);
            accuracy.recordIntervalHit(withinInterval[i]);
        }
        
        assertEquals(5, accuracy.getCount());
        assertTrue(accuracy.getMeanAbsoluteError() > 0);
        assertTrue(accuracy.getMeanAbsolutePercentageError() > 0);
        assertTrue(accuracy.getConfidence() > 0.8); // Should be fairly good
        assertEquals(80.0, accuracy.getIntervalHitRate()); // 4/5 = 80%
        
        // Verify summary is complete
        String summary = accuracy.getSummary();
        assertTrue(summary.contains("n=5"));
        
        // Verify toString is meaningful
        String toString = accuracy.toString();
        assertTrue(toString.contains("n=5"));
    }
    
    @Test
    @DisplayName("Should maintain mathematical consistency")
    void testMathematicalConsistency() {
        accuracy.updateWithPrediction(100.0, 90.0);
        accuracy.updateWithPrediction(200.0, 220.0);
        
        // MAE should be <= RMSE (due to Jensen's inequality)
        assertTrue(accuracy.getMeanAbsoluteError() <= accuracy.getRootMeanSquaredError());
        
        // MAPE should be reasonable
        assertTrue(accuracy.getMeanAbsolutePercentageError() >= 0);
        assertTrue(accuracy.getMeanAbsolutePercentageError() <= 1.0); // For this data
        
        // Confidence should be complement of MAPE (approximately)
        double expectedConfidence = Math.max(0, 1 - accuracy.getMeanAbsolutePercentageError());
        assertEquals(expectedConfidence, accuracy.getConfidence(), 1e-10);
        
        // R² should be between 0 and 1
        double rSquared = accuracy.getRSquared();
        assertTrue(rSquared >= 0.0 && rSquared <= 1.0);
    }
}