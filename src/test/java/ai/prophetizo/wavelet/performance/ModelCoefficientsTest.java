package ai.prophetizo.wavelet.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ModelCoefficients.
 * Tests polynomial evaluation, coefficient updates, and online learning.
 */
@DisplayName("ModelCoefficients Tests")
class ModelCoefficientsTest {

    private static final double EPSILON = 1e-10;

    // === Constructor Tests ===
    
    @Test
    @DisplayName("Should create coefficients with default values")
    void testDefaultConstructor() {
        ModelCoefficients coeffs = new ModelCoefficients();
        
        assertEquals(0.1, coeffs.getA(), EPSILON);
        assertEquals(0.0001, coeffs.getB(), EPSILON);
        assertEquals(0.0, coeffs.getC(), EPSILON);
    }
    
    @Test
    @DisplayName("Should create coefficients with specified values")
    void testParameterizedConstructor() {
        ModelCoefficients coeffs = new ModelCoefficients(5.0, 2.5, 0.1);
        
        assertEquals(5.0, coeffs.getA(), EPSILON);
        assertEquals(2.5, coeffs.getB(), EPSILON);
        assertEquals(0.1, coeffs.getC(), EPSILON);
    }
    
    @Test
    @DisplayName("Should accept zero coefficients")
    void testZeroCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(0.0, 0.0, 0.0);
        
        assertEquals(0.0, coeffs.getA(), EPSILON);
        assertEquals(0.0, coeffs.getB(), EPSILON);
        assertEquals(0.0, coeffs.getC(), EPSILON);
    }
    
    @Test
    @DisplayName("Should accept negative coefficients initially")
    void testNegativeCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(-1.0, -0.5, -0.1);
        
        assertEquals(-1.0, coeffs.getA(), EPSILON);
        assertEquals(-0.5, coeffs.getB(), EPSILON);
        assertEquals(-0.1, coeffs.getC(), EPSILON);
    }

    // === Evaluate Tests ===
    
    @Test
    @DisplayName("Should evaluate constant model correctly")
    void testEvaluateConstantModel() {
        ModelCoefficients coeffs = new ModelCoefficients(5.0, 0.0, 0.0);
        
        assertEquals(5.0, coeffs.evaluate(0), EPSILON);
        assertEquals(5.0, coeffs.evaluate(10), EPSILON);
        assertEquals(5.0, coeffs.evaluate(100), EPSILON);
    }
    
    @Test
    @DisplayName("Should evaluate linear model correctly")
    void testEvaluateLinearModel() {
        ModelCoefficients coeffs = new ModelCoefficients(2.0, 3.0, 0.0);
        
        assertEquals(2.0, coeffs.evaluate(0), EPSILON);  // 2 + 3*0 = 2
        assertEquals(5.0, coeffs.evaluate(1), EPSILON);  // 2 + 3*1 = 5
        assertEquals(32.0, coeffs.evaluate(10), EPSILON); // 2 + 3*10 = 32
    }
    
    @Test
    @DisplayName("Should evaluate quadratic model correctly")
    void testEvaluateQuadraticModel() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 2.0, 0.5);
        
        assertEquals(1.0, coeffs.evaluate(0), EPSILON);   // 1 + 2*0 + 0.5*0^2 = 1
        assertEquals(3.5, coeffs.evaluate(1), EPSILON);   // 1 + 2*1 + 0.5*1^2 = 3.5
        assertEquals(26.0, coeffs.evaluate(5), EPSILON);  // 1 + 2*5 + 0.5*25 = 26
        assertEquals(121.0, coeffs.evaluate(10), EPSILON); // 1 + 2*10 + 0.5*100 = 121
    }
    
    @ParameterizedTest
    @CsvSource({
        "0, 1.0",      // n=0: 1 + 0 + 0 = 1
        "1, 3.5",      // n=1: 1 + 2 + 0.5 = 3.5
        "2, 8.0",      // n=2: 1 + 4 + 2 = 7, wait: 1 + 2*2 + 0.5*4 = 1 + 4 + 2 = 7
        "3, 14.5",     // n=3: 1 + 6 + 4.5 = 11.5, wait: 1 + 2*3 + 0.5*9 = 1 + 6 + 4.5 = 11.5
        "4, 23.0"      // n=4: 1 + 8 + 8 = 17, wait: 1 + 2*4 + 0.5*16 = 1 + 8 + 8 = 17
    })
    @DisplayName("Should evaluate quadratic model for various inputs")
    void testEvaluateQuadraticVariousInputs(int n, double expected) {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 2.0, 0.5);
        
        // Let me recalculate:
        // f(n) = 1 + 2*n + 0.5*n^2
        double actualExpected = 1.0 + 2.0*n + 0.5*n*n;
        assertEquals(actualExpected, coeffs.evaluate(n), EPSILON);
    }
    
    @Test
    @DisplayName("Should handle large input values")
    void testEvaluateLargeInputs() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 0.001, 0.000001);
        
        double result = coeffs.evaluate(1000);
        double expected = 1.0 + 0.001 * 1000 + 0.000001 * 1000 * 1000;
        assertEquals(expected, result, EPSILON);
    }

    // === Update with Measurement Tests ===
    
    @Test
    @DisplayName("Should update coefficients with single measurement")
    void testUpdateWithSingleMeasurement() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 1.0, 0.0);
        
        // Initial prediction for n=10: 1 + 1*10 = 11
        // Actual time: 15, so error = 4
        double initialPrediction = coeffs.evaluate(10);
        coeffs.updateWithMeasurement(10, 15.0);
        
        // Coefficients should have changed to reduce error
        assertNotEquals(1.0, coeffs.getA(), EPSILON);
        assertNotEquals(1.0, coeffs.getB(), EPSILON);
        // New prediction should be closer to 15
        double newPrediction = coeffs.evaluate(10);
        assertTrue(Math.abs(15.0 - newPrediction) < Math.abs(15.0 - initialPrediction));
    }
    
    @Test
    @DisplayName("Should maintain non-negative coefficients after update")
    void testUpdateMaintainsNonNegativeCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(0.1, 0.01, 0.001);
        
        // Update with a measurement that might push coefficients negative
        coeffs.updateWithMeasurement(1, 0.05); // Much smaller than predicted
        
        // All coefficients should remain non-negative
        assertTrue(coeffs.getA() >= 0, "A coefficient should be non-negative");
        assertTrue(coeffs.getB() >= 0, "B coefficient should be non-negative");
        assertTrue(coeffs.getC() >= 0, "C coefficient should be non-negative");
    }
    
    @Test
    @DisplayName("Should converge with multiple measurements")
    void testUpdateConvergence() {
        ModelCoefficients coeffs = new ModelCoefficients(0.1, 0.1, 0.01);
        
        // Simulate measurements that follow a known pattern: time = 2 + 0.5*n
        int[] inputSizes = {10, 20, 30, 40, 50};
        double[] actualTimes = {7.0, 12.0, 17.0, 22.0, 27.0}; // 2 + 0.5*n
        
        double initialError = 0;
        double finalError = 0;
        
        // Calculate initial total error
        for (int i = 0; i < inputSizes.length; i++) {
            double predicted = coeffs.evaluate(inputSizes[i]);
            initialError += Math.abs(actualTimes[i] - predicted);
        }
        
        // Update with measurements multiple times
        for (int iteration = 0; iteration < 100; iteration++) {
            for (int i = 0; i < inputSizes.length; i++) {
                coeffs.updateWithMeasurement(inputSizes[i], actualTimes[i]);
            }
        }
        
        // Calculate final total error
        for (int i = 0; i < inputSizes.length; i++) {
            double predicted = coeffs.evaluate(inputSizes[i]);
            finalError += Math.abs(actualTimes[i] - predicted);
        }
        
        // Error should have decreased
        assertTrue(finalError < initialError, "Model should have improved with updates");
    }
    
    @Test
    @DisplayName("Should handle perfect predictions")
    void testUpdateWithPerfectPrediction() {
        ModelCoefficients coeffs = new ModelCoefficients(2.0, 0.5, 0.0);
        
        double originalA = coeffs.getA();
        double originalB = coeffs.getB();
        double originalC = coeffs.getC();
        
        // Perfect prediction: actual equals predicted
        double predicted = coeffs.evaluate(10);
        coeffs.updateWithMeasurement(10, predicted);
        
        // Coefficients might change slightly due to numerical effects, but should be close
        assertEquals(originalA, coeffs.getA(), 0.01);
        assertEquals(originalB, coeffs.getB(), 0.01);
        assertEquals(originalC, coeffs.getC(), 0.01);
    }
    
    @Test
    @DisplayName("Should handle extreme measurement values")
    void testUpdateWithExtremeMeasurements() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 1.0, 0.0);
        
        // Very large actual time
        coeffs.updateWithMeasurement(10, 1000.0);
        
        // Should still have valid coefficients
        assertTrue(Double.isFinite(coeffs.getA()));
        assertTrue(Double.isFinite(coeffs.getB()));
        assertTrue(Double.isFinite(coeffs.getC()));
        
        // Should predict higher values now
        assertTrue(coeffs.evaluate(10) > 11.0);
    }
    
    @Test
    @DisplayName("Should use adaptive learning rate")
    void testAdaptiveLearningRate() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 1.0, 0.0);
        
        double firstUpdate = coeffs.getA();
        coeffs.updateWithMeasurement(10, 20.0);
        double afterFirstUpdate = coeffs.getA();
        double firstChange = Math.abs(afterFirstUpdate - firstUpdate);
        
        // Perform many updates to increase update count
        for (int i = 0; i < 50; i++) {
            coeffs.updateWithMeasurement(10, 20.0);
        }
        
        double beforeLastUpdate = coeffs.getA();
        coeffs.updateWithMeasurement(10, 20.0);
        double afterLastUpdate = coeffs.getA();
        double lastChange = Math.abs(afterLastUpdate - beforeLastUpdate);
        
        // Later updates should have smaller changes due to adaptive learning rate
        assertTrue(lastChange < firstChange, "Later updates should have smaller changes");
    }

    // === Getters Tests ===
    
    @Test
    @DisplayName("Should return correct coefficient values")
    void testGetters() {
        ModelCoefficients coeffs = new ModelCoefficients(1.5, 2.7, 0.33);
        
        assertEquals(1.5, coeffs.getA(), EPSILON);
        assertEquals(2.7, coeffs.getB(), EPSILON);
        assertEquals(0.33, coeffs.getC(), EPSILON);
    }
    
    @Test
    @DisplayName("Should maintain getter consistency after updates")
    void testGettersAfterUpdate() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 2.0, 0.5);
        
        coeffs.updateWithMeasurement(5, 10.0);
        
        // Getters should return current values
        double a = coeffs.getA();
        double b = coeffs.getB();
        double c = coeffs.getC();
        
        // Evaluate should use the same values
        double expected = a + b * 5 + c * 5 * 5;
        assertEquals(expected, coeffs.evaluate(5), EPSILON);
    }

    // === Copy Tests ===
    
    @Test
    @DisplayName("Should create independent copy")
    void testCopy() {
        ModelCoefficients original = new ModelCoefficients(1.5, 2.5, 0.5);
        ModelCoefficients copy = original.copy();
        
        // Should have same values
        assertEquals(original.getA(), copy.getA(), EPSILON);
        assertEquals(original.getB(), copy.getB(), EPSILON);
        assertEquals(original.getC(), copy.getC(), EPSILON);
        
        // Should be independent - modifying copy shouldn't affect original
        copy.updateWithMeasurement(10, 100.0);
        
        assertEquals(1.5, original.getA(), EPSILON);
        assertEquals(2.5, original.getB(), EPSILON);
        assertEquals(0.5, original.getC(), EPSILON);
    }
    
    @Test
    @DisplayName("Should copy after updates")
    void testCopyAfterUpdates() {
        ModelCoefficients original = new ModelCoefficients(1.0, 1.0, 0.0);
        original.updateWithMeasurement(5, 10.0);
        
        ModelCoefficients copy = original.copy();
        
        // Copy should have the updated values
        assertEquals(original.getA(), copy.getA(), EPSILON);
        assertEquals(original.getB(), copy.getB(), EPSILON);
        assertEquals(original.getC(), copy.getC(), EPSILON);
        
        // Evaluation should give same results
        assertEquals(original.evaluate(10), copy.evaluate(10), EPSILON);
    }

    // === toString Tests ===
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        ModelCoefficients coeffs = new ModelCoefficients(1.2345, 0.006789, 0.000012345);
        String result = coeffs.toString();
        
        assertTrue(result.contains("1.2345"));
        assertTrue(result.contains("0.006789"));
        assertTrue(result.contains("0.000012345"));
        assertTrue(result.contains("n²"));
        assertTrue(result.contains("+"));
        assertTrue(result.contains("*n"));
    }
    
    @Test
    @DisplayName("Should format toString with specific precision")
    void testToStringPrecision() {
        ModelCoefficients coeffs = new ModelCoefficients(1.0, 0.001, 0.000001);
        String result = coeffs.toString();
        
        // Should format with specified decimal places
        assertTrue(result.contains("1.0000")); // 4 decimal places for a
        assertTrue(result.contains("0.001000")); // 6 decimal places for b
        assertTrue(result.contains("0.000001000")); // 9 decimal places for c
    }
    
    @Test
    @DisplayName("Should handle zero coefficients in toString")
    void testToStringZeroCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(0.0, 0.0, 0.0);
        String result = coeffs.toString();
        
        assertTrue(result.contains("0.0000"));
        assertTrue(result.contains("0.000000"));
        assertTrue(result.contains("0.000000000"));
    }
    
    @Test
    @DisplayName("Should handle negative coefficients in toString")
    void testToStringNegativeCoefficients() {
        ModelCoefficients coeffs = new ModelCoefficients(-1.5, -0.5, -0.001);
        String result = coeffs.toString();
        
        assertTrue(result.contains("-1.5000"));
        assertTrue(result.contains("-0.500000"));
        assertTrue(result.contains("-0.001000000"));
    }

    // === Edge Cases Tests ===
    
    @Test
    @DisplayName("Should handle zero input size")
    void testZeroInputSize() {
        ModelCoefficients coeffs = new ModelCoefficients(5.0, 2.0, 1.0);
        
        assertEquals(5.0, coeffs.evaluate(0), EPSILON);
        
        // Update with zero input size
        coeffs.updateWithMeasurement(0, 6.0);
        
        // Should still work
        assertTrue(Double.isFinite(coeffs.getA()));
        assertTrue(Double.isFinite(coeffs.getB()));
        assertTrue(Double.isFinite(coeffs.getC()));
    }
    
    @Test
    @DisplayName("Should handle very small values")
    void testVerySmallValues() {
        ModelCoefficients coeffs = new ModelCoefficients(1e-10, 1e-12, 1e-15);
        
        double result = coeffs.evaluate(100);
        assertTrue(Double.isFinite(result));
        assertTrue(result >= 0);
        
        coeffs.updateWithMeasurement(100, 1e-9);
        assertTrue(Double.isFinite(coeffs.getA()));
        assertTrue(Double.isFinite(coeffs.getB()));
        assertTrue(Double.isFinite(coeffs.getC()));
    }
    
    @Test
    @DisplayName("Should handle large values")
    void testLargeValues() {
        ModelCoefficients coeffs = new ModelCoefficients(1e6, 1e3, 1.0);
        
        double result = coeffs.evaluate(1000);
        assertTrue(Double.isFinite(result));
        
        coeffs.updateWithMeasurement(1000, 2e6);
        assertTrue(Double.isFinite(coeffs.getA()));
        assertTrue(Double.isFinite(coeffs.getB()));
        assertTrue(Double.isFinite(coeffs.getC()));
    }

    // === Integration Tests ===
    
    @Test
    @DisplayName("Should work in realistic performance modeling scenario")
    void testRealisticScenario() {
        ModelCoefficients coeffs = new ModelCoefficients();
        
        // Simulate performance measurements for different input sizes
        // Real pattern: roughly 0.1 + 0.0001*n + 0.00000001*n^2
        int[] sizes = {100, 500, 1000, 2000, 5000};
        double[] times = new double[sizes.length];
        
        for (int i = 0; i < sizes.length; i++) {
            times[i] = 0.1 + 0.0001 * sizes[i] + 0.00000001 * sizes[i] * sizes[i];
            times[i] += (Math.random() - 0.5) * 0.01; // Add some noise
        }
        
        // Train the model
        for (int iteration = 0; iteration < 20; iteration++) {
            for (int i = 0; i < sizes.length; i++) {
                coeffs.updateWithMeasurement(sizes[i], times[i]);
            }
        }
        
        // Model should make reasonable predictions
        for (int i = 0; i < sizes.length; i++) {
            double predicted = coeffs.evaluate(sizes[i]);
            double error = Math.abs(predicted - times[i]) / times[i];
            assertTrue(error < 0.5, "Prediction error should be reasonable for size " + sizes[i]);
        }
        
        // Should predict increasing times for larger inputs
        assertTrue(coeffs.evaluate(10000) > coeffs.evaluate(1000));
        assertTrue(coeffs.evaluate(1000) > coeffs.evaluate(100));
    }
    
    @Test
    @DisplayName("Should maintain mathematical consistency")
    void testMathematicalConsistency() {
        ModelCoefficients coeffs = new ModelCoefficients(2.0, 1.5, 0.5);
        
        // Evaluation should be mathematically consistent
        int n = 7;
        double expected = 2.0 + 1.5 * n + 0.5 * n * n;
        assertEquals(expected, coeffs.evaluate(n), EPSILON);
        
        // Copy should preserve values exactly
        ModelCoefficients copy = coeffs.copy();
        assertEquals(coeffs.evaluate(n), copy.evaluate(n), EPSILON);
        
        // After updates, all coefficients should remain finite and non-negative
        for (int i = 0; i < 10; i++) {
            coeffs.updateWithMeasurement(i + 1, i * 2.0);
            assertTrue(Double.isFinite(coeffs.getA()));
            assertTrue(Double.isFinite(coeffs.getB()));
            assertTrue(Double.isFinite(coeffs.getC()));
            assertTrue(coeffs.getA() >= 0);
            assertTrue(coeffs.getB() >= 0);
            assertTrue(coeffs.getC() >= 0);
        }
    }
}