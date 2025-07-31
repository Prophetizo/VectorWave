package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Performance tests for complex matrix operations.
 */
class ComplexMatrixPerformanceTest {
    
    @Test
    @DisplayName("Should demonstrate improved cache efficiency in matrix multiplication")
    void testMatrixMultiplicationCacheEfficiency() {
        int[] sizes = {16, 32, 64, 128};
        ComplexVectorOps ops = new ComplexVectorOps();
        
        System.out.println("\nComplex Matrix Multiplication Performance:");
        System.out.println("Size\tNaive(ms)\tBlocked(ms)\tTransposed(ms)\tSpeedup");
        
        for (int size : sizes) {
            // Create test matrices
            ComplexMatrix a = createRandomMatrix(size, size);
            ComplexMatrix b = createRandomMatrix(size, size);
            
            // Warm up
            for (int i = 0; i < 10; i++) {
                naiveMatrixMultiply(a, b);
                ops.matrixMultiply(a, b);
            }
            
            // Time naive implementation
            long naiveStart = System.nanoTime();
            ComplexMatrix naiveResult = naiveMatrixMultiply(a, b);
            long naiveTime = System.nanoTime() - naiveStart;
            
            // Time blocked implementation
            long blockedStart = System.nanoTime();
            ComplexMatrix blockedResult = ops.matrixMultiply(a, b);
            long blockedTime = System.nanoTime() - blockedStart;
            
            // Time transposed implementation
            ComplexMatrix bTransposed = transposeMatrix(b);
            long transposedStart = System.nanoTime();
            ComplexMatrix transposedResult = ops.matrixMultiplyTransposed(a, bTransposed);
            long transposedTime = System.nanoTime() - transposedStart;
            
            double naiveMs = naiveTime / 1_000_000.0;
            double blockedMs = blockedTime / 1_000_000.0;
            double transposedMs = transposedTime / 1_000_000.0;
            double speedup = naiveMs / blockedMs;
            double transposedSpeedup = naiveMs / transposedMs;
            
            System.out.printf("%d\t%.2f\t\t%.2f\t\t%.2f\t\t%.2fx / %.2fx%n",
                            size, naiveMs, blockedMs, transposedMs, speedup, transposedSpeedup);
            
            // Verify correctness
            verifyMatrixEquals(naiveResult, blockedResult, 1e-10);
            verifyMatrixEquals(naiveResult, transposedResult, 1e-10);
        }
    }
    
    @Test
    @DisplayName("Should show column unrolling benefits")
    void testColumnUnrollingBenefits() {
        // Test with a rectangular matrix to emphasize column access
        int rows = 64;
        int cols = 256;
        int inner = 32;
        
        ComplexMatrix a = createRandomMatrix(rows, inner);
        ComplexMatrix b = createRandomMatrix(inner, cols);
        
        ComplexVectorOps ops = new ComplexVectorOps();
        
        // Warm up
        for (int i = 0; i < 5; i++) {
            ops.matrixMultiply(a, b);
        }
        
        // Measure with column unrolling
        long start = System.nanoTime();
        ComplexMatrix result = ops.matrixMultiply(a, b);
        long time = System.nanoTime() - start;
        
        System.out.println("\nColumn Unrolling Performance:");
        System.out.printf("Matrix size: %dx%d * %dx%d%n", rows, inner, inner, cols);
        System.out.printf("Time: %.2f ms%n", time / 1_000_000.0);
        System.out.printf("GFLOPS: %.2f%n", (8.0 * rows * cols * inner) / time);
        
        assertNotNull(result);
        assertEquals(rows, result.getRows());
        assertEquals(cols, result.getCols());
    }
    
    @Test
    @DisplayName("Should demonstrate cache miss reduction")
    void testCacheMissReduction() {
        // Test different block sizes to show cache effects
        int matrixSize = 128;
        int[] blockSizes = {8, 16, 32, 64, 128};
        
        ComplexMatrix a = createRandomMatrix(matrixSize, matrixSize);
        ComplexMatrix b = createRandomMatrix(matrixSize, matrixSize);
        
        System.out.println("\nCache Effects with Different Block Sizes:");
        System.out.println("Block Size\tTime(ms)\tRelative");
        
        long baseTime = 0;
        for (int blockSize : blockSizes) {
            // Simulate blocked multiplication with specific block size
            long start = System.nanoTime();
            blockedMultiplyWithSize(a, b, blockSize);
            long time = System.nanoTime() - start;
            
            if (baseTime == 0) baseTime = time;
            
            System.out.printf("%d\t\t%.2f\t\t%.2f%n",
                            blockSize, time / 1_000_000.0, (double)time / baseTime);
        }
    }
    
    // Helper methods
    
    private ComplexMatrix createRandomMatrix(int rows, int cols) {
        ComplexMatrix matrix = new ComplexMatrix(rows, cols);
        Random random = new Random(TestConstants.TEST_SEED);
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix.set(i, j, random.nextGaussian(), random.nextGaussian());
            }
        }
        
        return matrix;
    }
    
    private ComplexMatrix naiveMatrixMultiply(ComplexMatrix a, ComplexMatrix b) {
        int m = a.getRows();
        int n = b.getCols();
        int k = a.getCols();
        
        ComplexMatrix result = new ComplexMatrix(m, n);
        
        // Naive triple loop with poor cache locality
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int kk = 0; kk < k; kk++) {
                    double ar = a.getReal(i, kk);
                    double ai = a.getImaginary(i, kk);
                    double br = b.getReal(kk, j);
                    double bi = b.getImaginary(kk, j);
                    
                    sumReal += ar * br - ai * bi;
                    sumImag += ar * bi + ai * br;
                }
                
                result.set(i, j, sumReal, sumImag);
            }
        }
        
        return result;
    }
    
    private ComplexMatrix transposeMatrix(ComplexMatrix matrix) {
        int rows = matrix.getRows();
        int cols = matrix.getCols();
        ComplexMatrix transposed = new ComplexMatrix(cols, rows);
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed.set(j, i, matrix.getReal(i, j), matrix.getImaginary(i, j));
            }
        }
        
        return transposed;
    }
    
    private void verifyMatrixEquals(ComplexMatrix expected, ComplexMatrix actual, double tolerance) {
        assertEquals(expected.getRows(), actual.getRows());
        assertEquals(expected.getCols(), actual.getCols());
        
        for (int i = 0; i < expected.getRows(); i++) {
            for (int j = 0; j < expected.getCols(); j++) {
                assertEquals(expected.getReal(i, j), actual.getReal(i, j), tolerance,
                    String.format("Real part mismatch at [%d,%d]", i, j));
                assertEquals(expected.getImaginary(i, j), actual.getImaginary(i, j), tolerance,
                    String.format("Imaginary part mismatch at [%d,%d]", i, j));
            }
        }
    }
    
    private ComplexMatrix blockedMultiplyWithSize(ComplexMatrix a, ComplexMatrix b, int blockSize) {
        int m = a.getRows();
        int n = b.getCols();
        int k = a.getCols();
        
        ComplexMatrix result = new ComplexMatrix(m, n);
        
        // Blocked multiplication with specified block size
        for (int i0 = 0; i0 < m; i0 += blockSize) {
            for (int j0 = 0; j0 < n; j0 += blockSize) {
                for (int k0 = 0; k0 < k; k0 += blockSize) {
                    int iMax = Math.min(i0 + blockSize, m);
                    int jMax = Math.min(j0 + blockSize, n);
                    int kMax = Math.min(k0 + blockSize, k);
                    
                    for (int i = i0; i < iMax; i++) {
                        for (int j = j0; j < jMax; j++) {
                            double sumReal = result.getReal(i, j);
                            double sumImag = result.getImaginary(i, j);
                            
                            for (int kk = k0; kk < kMax; kk++) {
                                double ar = a.getReal(i, kk);
                                double ai = a.getImaginary(i, kk);
                                double br = b.getReal(kk, j);
                                double bi = b.getImaginary(kk, j);
                                
                                sumReal += ar * br - ai * bi;
                                sumImag += ar * bi + ai * br;
                            }
                            
                            result.set(i, j, sumReal, sumImag);
                        }
                    }
                }
            }
        }
        
        return result;
    }
}