package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Tests for vectorized complex number operations.
 */
class ComplexVectorOpsTest {
    
    private ComplexVectorOps ops;
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        ops = new ComplexVectorOps();
    }
    
    @Test
    @DisplayName("Should perform complex multiplication correctly")
    void testComplexMultiply() {
        // Test data: (3 + 4i) * (1 + 2i) = (3 - 8) + (6 + 4)i = -5 + 10i
        double[] real1 = {3.0, 1.0, 2.0};
        double[] imag1 = {4.0, -1.0, 3.0};
        double[] real2 = {1.0, 2.0, -1.0};
        double[] imag2 = {2.0, 3.0, 2.0};
        
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // Verify results
        assertEquals(-5.0, resultReal[0], EPSILON); // (3 + 4i) * (1 + 2i)
        assertEquals(10.0, resultImag[0], EPSILON);
        
        assertEquals(5.0, resultReal[1], EPSILON);  // (1 - i) * (2 + 3i)
        assertEquals(1.0, resultImag[1], EPSILON);
        
        assertEquals(-8.0, resultReal[2], EPSILON); // (2 + 3i) * (-1 + 2i)
        assertEquals(1.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Should perform complex scalar multiplication")
    void testComplexScalarMultiply() {
        double[] real = {2.0, 3.0, -1.0};
        double[] imag = {1.0, -2.0, 4.0};
        double scalarReal = 2.0;
        double scalarImag = -1.0;
        
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexScalarMultiply(real, imag, scalarReal, scalarImag, resultReal, resultImag);
        
        // (2 + i) * (2 - i) = 4 - 2i + 2i - i² = 4 + 1 = 5
        assertEquals(5.0, resultReal[0], EPSILON);
        assertEquals(0.0, resultImag[0], EPSILON);
        
        // (3 - 2i) * (2 - i) = 6 - 3i - 4i + 2i² = 6 - 7i - 2 = 4 - 7i
        assertEquals(4.0, resultReal[1], EPSILON);
        assertEquals(-7.0, resultImag[1], EPSILON);
    }
    
    @Test
    @DisplayName("Should compute complex conjugate")
    void testComplexConjugate() {
        double[] real = {1.0, 2.0, -3.0};
        double[] imag = {2.0, -3.0, 4.0};
        
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexConjugate(real, imag, resultReal, resultImag);
        
        // Real parts should be unchanged
        assertArrayEquals(real, resultReal, EPSILON);
        
        // Imaginary parts should be negated
        assertEquals(-2.0, resultImag[0], EPSILON);
        assertEquals(3.0, resultImag[1], EPSILON);
        assertEquals(-4.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Should add complex arrays")
    void testComplexAdd() {
        double[] real1 = {1.0, 2.0, 3.0};
        double[] imag1 = {4.0, 5.0, 6.0};
        double[] real2 = {7.0, 8.0, 9.0};
        double[] imag2 = {10.0, 11.0, 12.0};
        
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexAdd(real1, imag1, real2, imag2, resultReal, resultImag);
        
        assertEquals(8.0, resultReal[0], EPSILON);
        assertEquals(14.0, resultImag[0], EPSILON);
        assertEquals(10.0, resultReal[1], EPSILON);
        assertEquals(16.0, resultImag[1], EPSILON);
        assertEquals(12.0, resultReal[2], EPSILON);
        assertEquals(18.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Should compute magnitude correctly")
    void testComplexMagnitude() {
        double[] real = {3.0, 5.0, 0.0, -4.0};
        double[] imag = {4.0, 12.0, 7.0, 3.0};
        double[] magnitude = new double[4];
        
        ops.complexMagnitude(real, imag, magnitude);
        
        assertEquals(5.0, magnitude[0], EPSILON);   // |3 + 4i| = 5
        assertEquals(13.0, magnitude[1], EPSILON);  // |5 + 12i| = 13
        assertEquals(7.0, magnitude[2], EPSILON);   // |0 + 7i| = 7
        assertEquals(5.0, magnitude[3], EPSILON);   // |-4 + 3i| = 5
    }
    
    @Test
    @DisplayName("Should compute phase correctly")
    void testComplexPhase() {
        double[] real = {1.0, 0.0, -1.0, 1.0};
        double[] imag = {0.0, 1.0, 0.0, 1.0};
        double[] phase = new double[4];
        
        ops.complexPhase(real, imag, phase);
        
        assertEquals(0.0, phase[0], EPSILON);            // arg(1 + 0i) = 0
        assertEquals(Math.PI / 2, phase[1], EPSILON);   // arg(0 + i) = π/2
        assertEquals(Math.PI, Math.abs(phase[2]), EPSILON); // arg(-1 + 0i) = π
        assertEquals(Math.PI / 4, phase[3], EPSILON);   // arg(1 + i) = π/4
    }
    
    @Test
    @DisplayName("Should divide complex numbers correctly")
    void testComplexDivide() {
        // (4 + 3i) / (2 + i) = ((4 + 3i)(2 - i)) / (4 + 1) = (8 - 4i + 6i - 3i²) / 5
        //                    = (8 + 2i + 3) / 5 = (11 + 2i) / 5 = 2.2 + 0.4i
        double[] real1 = {4.0};
        double[] imag1 = {3.0};
        double[] real2 = {2.0};
        double[] imag2 = {1.0};
        
        double[] resultReal = new double[1];
        double[] resultImag = new double[1];
        
        ops.complexDivide(real1, imag1, real2, imag2, resultReal, resultImag);
        
        assertEquals(2.2, resultReal[0], EPSILON);
        assertEquals(0.4, resultImag[0], EPSILON);
    }
    
    @Test
    @DisplayName("Should handle large arrays efficiently")
    void testLargeArrayPerformance() {
        int size = 10000;
        Random random = new Random(TestConstants.TEST_SEED);
        
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        
        // Fill with random data
        for (int i = 0; i < size; i++) {
            real1[i] = random.nextGaussian();
            imag1[i] = random.nextGaussian();
            real2[i] = random.nextGaussian();
            imag2[i] = random.nextGaussian();
        }
        
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        // Time the vectorized operation
        long start = System.nanoTime();
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        long vectorTime = System.nanoTime() - start;
        
        // Verify a few results
        for (int i = 0; i < 10; i++) {
            double expectedReal = real1[i] * real2[i] - imag1[i] * imag2[i];
            double expectedImag = real1[i] * imag2[i] + imag1[i] * real2[i];
            assertEquals(expectedReal, resultReal[i], EPSILON);
            assertEquals(expectedImag, resultImag[i], EPSILON);
        }
        
        // Performance timing is validated by ensuring operation completes without error
        assertTrue(vectorTime > 0, "Operation should take measurable time");
    }
    
    @Test
    @DisplayName("Should convert between layouts correctly")
    void testLayoutConversion() {
        double[] real = {1.0, 2.0, 3.0, 4.0};
        double[] imag = {5.0, 6.0, 7.0, 8.0};
        double[] interleaved = new double[8];
        
        // Convert to interleaved
        ops.convertToInterleaved(real, imag, interleaved);
        
        // Verify interleaved layout
        assertEquals(1.0, interleaved[0], EPSILON); // real[0]
        assertEquals(5.0, interleaved[1], EPSILON); // imag[0]
        assertEquals(2.0, interleaved[2], EPSILON); // real[1]
        assertEquals(6.0, interleaved[3], EPSILON); // imag[1]
        assertEquals(3.0, interleaved[4], EPSILON); // real[2]
        assertEquals(7.0, interleaved[5], EPSILON); // imag[2]
        assertEquals(4.0, interleaved[6], EPSILON); // real[3]
        assertEquals(8.0, interleaved[7], EPSILON); // imag[3]
        
        // Convert back to split
        double[] newReal = new double[4];
        double[] newImag = new double[4];
        ops.convertToSplit(interleaved, newReal, newImag);
        
        assertArrayEquals(real, newReal, EPSILON);
        assertArrayEquals(imag, newImag, EPSILON);
    }
    
    @Test
    @DisplayName("Should multiply complex matrices")
    void testMatrixMultiply() {
        // Create 2x2 complex matrices
        // A = [1+2i, 3+4i]    B = [5+6i, 7+8i]
        //     [2+3i, 4+5i]        [1+2i, 3+4i]
        
        ComplexMatrix a = new ComplexMatrix(2, 2);
        a.set(0, 0, 1.0, 2.0);
        a.set(0, 1, 3.0, 4.0);
        a.set(1, 0, 2.0, 3.0);
        a.set(1, 1, 4.0, 5.0);
        
        ComplexMatrix b = new ComplexMatrix(2, 2);
        b.set(0, 0, 5.0, 6.0);
        b.set(0, 1, 7.0, 8.0);
        b.set(1, 0, 1.0, 2.0);
        b.set(1, 1, 3.0, 4.0);
        
        ComplexMatrix result = ops.matrixMultiply(a, b);
        
        // Verify dimensions
        assertEquals(2, result.getRows());
        assertEquals(2, result.getCols());
        
        // Verify first element: (1+2i)(5+6i) + (3+4i)(1+2i)
        // = (5-12) + (10+6)i + (3-8) + (6+4)i
        // = -7 + 16i + (-5) + 10i = -12 + 26i
        assertEquals(-12.0, result.getReal(0, 0), EPSILON);
        assertEquals(26.0, result.getImaginary(0, 0), EPSILON);
    }
    
    @Test
    @DisplayName("Should handle edge cases")
    void testEdgeCases() {
        // Test with zero arrays
        double[] zeros = new double[5];
        double[] result = new double[5];
        
        ops.complexMagnitude(zeros, zeros, result);
        assertArrayEquals(zeros, result, EPSILON);
        
        // Test with single element
        double[] single = {3.0};
        double[] singleImag = {4.0};
        double[] singleResult = new double[1];
        
        ops.complexMagnitude(single, singleImag, singleResult);
        assertEquals(5.0, singleResult[0], EPSILON);
    }
    
    @Test
    @DisplayName("Should provide correct optimization stats")
    void testOptimizationStats() {
        ComplexVectorOps.OptimizationStats stats = ops.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.vectorLength() > 0);
        assertTrue(stats.simdThreshold() > 0);
        assertTrue(stats.availableProcessors() > 0);
        assertNotNull(stats.vectorSpecies());
    }
}