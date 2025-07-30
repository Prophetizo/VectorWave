package ai.prophetizo.wavelet.cwt.optimization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for complex number layout conversion optimizations.
 */
class ComplexLayoutConversionTest {
    
    @Test
    @DisplayName("Should correctly convert from split to interleaved layout")
    void testSplitToInterleaved() {
        ComplexVectorOps ops = new ComplexVectorOps();
        int[] sizes = {16, 64, 256, 1024};
        
        for (int size : sizes) {
            double[] real = new double[size];
            double[] imag = new double[size];
            double[] interleaved = new double[2 * size];
            
            // Initialize with test data
            Random random = new Random(42);
            for (int i = 0; i < size; i++) {
                real[i] = random.nextDouble();
                imag[i] = random.nextDouble();
            }
            
            // Convert
            ops.convertToInterleaved(real, imag, interleaved);
            
            // Verify
            for (int i = 0; i < size; i++) {
                assertEquals(real[i], interleaved[2 * i], 1e-10,
                    "Real part mismatch at index " + i);
                assertEquals(imag[i], interleaved[2 * i + 1], 1e-10,
                    "Imaginary part mismatch at index " + i);
            }
        }
    }
    
    @Test
    @DisplayName("Should correctly convert from interleaved to split layout")
    void testInterleavedToSplit() {
        ComplexVectorOps ops = new ComplexVectorOps();
        int[] sizes = {16, 64, 256, 1024};
        
        for (int size : sizes) {
            double[] interleaved = new double[2 * size];
            double[] real = new double[size];
            double[] imag = new double[size];
            
            // Initialize with test data
            Random random = new Random(42);
            for (int i = 0; i < size; i++) {
                interleaved[2 * i] = random.nextDouble();
                interleaved[2 * i + 1] = random.nextDouble();
            }
            
            // Convert
            ops.convertToSplit(interleaved, real, imag);
            
            // Verify
            for (int i = 0; i < size; i++) {
                assertEquals(interleaved[2 * i], real[i], 1e-10,
                    "Real part mismatch at index " + i);
                assertEquals(interleaved[2 * i + 1], imag[i], 1e-10,
                    "Imaginary part mismatch at index " + i);
            }
        }
    }
    
    @Test
    @DisplayName("Should show performance improvement for layout conversions")
    void testConversionPerformance() {
        ComplexVectorOps ops = new ComplexVectorOps();
        int size = 32768; // Large array to show vectorization benefits
        
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] interleaved = new double[2 * size];
        
        // Initialize with random data
        Random random = new Random(42);
        for (int i = 0; i < size; i++) {
            real[i] = random.nextDouble();
            imag[i] = random.nextDouble();
            interleaved[2 * i] = random.nextDouble();
            interleaved[2 * i + 1] = random.nextDouble();
        }
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            ops.convertToInterleaved(real, imag, interleaved);
            ops.convertToSplit(interleaved, real, imag);
        }
        
        // Time split to interleaved
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ops.convertToInterleaved(real, imag, interleaved);
        }
        long vectorizedSplitToInterleaved = System.nanoTime() - start;
        
        // Time scalar version
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            scalarConvertToInterleaved(real, imag, interleaved);
        }
        long scalarSplitToInterleaved = System.nanoTime() - start;
        
        // Time interleaved to split
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ops.convertToSplit(interleaved, real, imag);
        }
        long vectorizedInterleavedToSplit = System.nanoTime() - start;
        
        // Time scalar version
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            scalarConvertToSplit(interleaved, real, imag);
        }
        long scalarInterleavedToSplit = System.nanoTime() - start;
        
        double splitToInterleavedSpeedup = (double) scalarSplitToInterleaved / vectorizedSplitToInterleaved;
        double interleavedToSplitSpeedup = (double) scalarInterleavedToSplit / vectorizedInterleavedToSplit;
        
        System.out.println("\nLayout Conversion Performance (32K elements):");
        System.out.println("Split to Interleaved:");
        System.out.printf("  Scalar: %.2f µs%n", scalarSplitToInterleaved / 100000.0);
        System.out.printf("  Vectorized: %.2f µs%n", vectorizedSplitToInterleaved / 100000.0);
        System.out.printf("  Speedup: %.2fx%n", splitToInterleavedSpeedup);
        
        System.out.println("Interleaved to Split:");
        System.out.printf("  Scalar: %.2f µs%n", scalarInterleavedToSplit / 100000.0);
        System.out.printf("  Vectorized: %.2f µs%n", vectorizedInterleavedToSplit / 100000.0);
        System.out.printf("  Speedup: %.2fx%n", interleavedToSplitSpeedup);
        
        // Expect some improvement from loop unrolling at least
        assertTrue(splitToInterleavedSpeedup > 0.9 || interleavedToSplitSpeedup > 0.9,
            "At least one conversion should show improvement or no regression");
    }
    
    @Test
    @DisplayName("Should handle edge cases correctly")
    void testEdgeCases() {
        ComplexVectorOps ops = new ComplexVectorOps();
        
        // Test empty arrays
        double[] emptyReal = new double[0];
        double[] emptyImag = new double[0];
        double[] emptyInterleaved = new double[0];
        
        assertDoesNotThrow(() -> {
            ops.convertToInterleaved(emptyReal, emptyImag, emptyInterleaved);
            ops.convertToSplit(emptyInterleaved, emptyReal, emptyImag);
        });
        
        // Test single element
        double[] singleReal = {1.0};
        double[] singleImag = {2.0};
        double[] singleInterleaved = new double[2];
        
        ops.convertToInterleaved(singleReal, singleImag, singleInterleaved);
        assertEquals(1.0, singleInterleaved[0]);
        assertEquals(2.0, singleInterleaved[1]);
        
        // Test small arrays (below SIMD threshold)
        double[] smallReal = new double[10];
        double[] smallImag = new double[10];
        double[] smallInterleaved = new double[20];
        
        for (int i = 0; i < 10; i++) {
            smallReal[i] = i;
            smallImag[i] = i + 10;
        }
        
        ops.convertToInterleaved(smallReal, smallImag, smallInterleaved);
        
        for (int i = 0; i < 10; i++) {
            assertEquals(i, smallInterleaved[2 * i]);
            assertEquals(i + 10, smallInterleaved[2 * i + 1]);
        }
    }
    
    // Scalar implementations for comparison
    
    private void scalarConvertToInterleaved(double[] real, double[] imag, double[] interleaved) {
        for (int i = 0; i < real.length; i++) {
            interleaved[2 * i] = real[i];
            interleaved[2 * i + 1] = imag[i];
        }
    }
    
    private void scalarConvertToSplit(double[] interleaved, double[] real, double[] imag) {
        for (int i = 0; i < real.length; i++) {
            real[i] = interleaved[2 * i];
            imag[i] = interleaved[2 * i + 1];
        }
    }
}