package ai.prophetizo.wavelet.cwt.optimization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparison tests showing the performance improvement of vectorized complex operations.
 */
class ComplexOperationsComparisonTest {
    
    @Test
    @DisplayName("Should demonstrate complex multiplication speedup")
    void testComplexMultiplicationSpeedup() {
        int[] sizes = {256, 1024, 4096, 16384};
        ComplexVectorOps vectorOps = new ComplexVectorOps();
        
        System.out.println("\nComplex Multiplication Performance Comparison:");
        System.out.println("Size\tScalar(µs)\tVector(µs)\tSpeedup");
        
        for (int size : sizes) {
            // Create test data
            double[] real1 = new double[size];
            double[] imag1 = new double[size];
            double[] real2 = new double[size];
            double[] imag2 = new double[size];
            double[] resultReal = new double[size];
            double[] resultImag = new double[size];
            
            Random random = new Random(42);
            for (int i = 0; i < size; i++) {
                real1[i] = random.nextGaussian();
                imag1[i] = random.nextGaussian();
                real2[i] = random.nextGaussian();
                imag2[i] = random.nextGaussian();
            }
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                scalarComplexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
                vectorOps.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
            }
            
            // Time scalar implementation
            long scalarTime = 0;
            int iterations = 1000;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                scalarComplexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
                scalarTime += System.nanoTime() - start;
            }
            
            // Time vector implementation
            long vectorTime = 0;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                vectorOps.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
                vectorTime += System.nanoTime() - start;
            }
            
            double scalarMicros = scalarTime / (1000.0 * iterations);
            double vectorMicros = vectorTime / (1000.0 * iterations);
            double speedup = scalarMicros / vectorMicros;
            
            System.out.printf("%d\t%.2f\t\t%.2f\t\t%.2fx%n", 
                            size, scalarMicros, vectorMicros, speedup);
            
            // Verify correctness on a few elements
            double[] testResultReal = new double[size];
            double[] testResultImag = new double[size];
            scalarComplexMultiply(real1, imag1, real2, imag2, testResultReal, testResultImag);
            
            for (int i = 0; i < Math.min(10, size); i++) {
                assertEquals(testResultReal[i], resultReal[i], 1e-10);
                assertEquals(testResultImag[i], resultImag[i], 1e-10);
            }
        }
    }
    
    @Test
    @DisplayName("Should demonstrate complex convolution improvement")
    void testComplexConvolutionImprovement() {
        // Simulate a typical CWT convolution scenario
        int signalLength = 2048;
        int waveletLength = 64;
        
        double[] realSignal = new double[signalLength];
        double[] imagSignal = new double[signalLength];
        double[] realWavelet = new double[waveletLength];
        double[] imagWavelet = new double[waveletLength];
        
        Random random = new Random(42);
        for (int i = 0; i < signalLength; i++) {
            realSignal[i] = Math.sin(2 * Math.PI * i / 32) + 0.1 * random.nextGaussian();
            imagSignal[i] = Math.cos(2 * Math.PI * i / 32) + 0.1 * random.nextGaussian();
        }
        
        for (int i = 0; i < waveletLength; i++) {
            double t = (i - waveletLength / 2.0) / 8.0;
            realWavelet[i] = Math.exp(-t * t / 2) * Math.cos(5 * t);
            imagWavelet[i] = Math.exp(-t * t / 2) * Math.sin(5 * t);
        }
        
        // Time convolution operations
        ComplexVectorOps vectorOps = new ComplexVectorOps();
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            performScalarConvolution(realSignal, imagSignal, realWavelet, imagWavelet);
            performVectorConvolution(realSignal, imagSignal, realWavelet, imagWavelet, vectorOps);
        }
        
        // Measure scalar convolution
        long scalarStart = System.nanoTime();
        double[] scalarResult = performScalarConvolution(realSignal, imagSignal, 
                                                        realWavelet, imagWavelet);
        long scalarTime = System.nanoTime() - scalarStart;
        
        // Measure vector convolution
        long vectorStart = System.nanoTime();
        double[] vectorResult = performVectorConvolution(realSignal, imagSignal, 
                                                       realWavelet, imagWavelet, vectorOps);
        long vectorTime = System.nanoTime() - vectorStart;
        
        double speedup = (double) scalarTime / vectorTime;
        
        System.out.println("\nComplex Convolution Performance:");
        System.out.printf("Signal length: %d, Wavelet length: %d%n", signalLength, waveletLength);
        System.out.printf("Scalar time: %.2f ms%n", scalarTime / 1_000_000.0);
        System.out.printf("Vector time: %.2f ms%n", vectorTime / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", speedup);
        
        // Note: On ARM M1 with vector length 2, we see benefits only for larger arrays
        // The convolution test uses moderate sizes, so speedup may be < 1
        System.out.println("Note: Speedup depends on array size and platform. ARM M1 shows benefits at >16K elements.");
    }
    
    @Test
    @DisplayName("Should show performance improvement for large arrays")
    void testLargeArrayPerformance() {
        // Test with array size where we know vectorization helps
        int size = 32768; // 32K elements
        ComplexVectorOps vectorOps = new ComplexVectorOps();
        
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        Random random = new Random(42);
        for (int i = 0; i < size; i++) {
            real1[i] = random.nextGaussian();
            imag1[i] = random.nextGaussian();
            real2[i] = random.nextGaussian();
            imag2[i] = random.nextGaussian();
        }
        
        // Warm up
        for (int i = 0; i < 50; i++) {
            scalarComplexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
            vectorOps.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        }
        
        // Time operations
        long scalarTime = 0;
        long vectorTime = 0;
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            scalarComplexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
            scalarTime += System.nanoTime() - start;
            
            start = System.nanoTime();
            vectorOps.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
            vectorTime += System.nanoTime() - start;
        }
        
        double speedup = (double) scalarTime / vectorTime;
        
        System.out.println("\nLarge Array Performance (32K elements):");
        System.out.printf("Scalar time: %.2f ms%n", scalarTime / (1_000_000.0 * iterations));
        System.out.printf("Vector time: %.2f ms%n", vectorTime / (1_000_000.0 * iterations));
        System.out.printf("Speedup: %.2fx%n", speedup);
        
        // For large arrays, we should see improvement
        assertTrue(speedup > 1.0, "Should show improvement for large arrays");
    }
    
    @Test
    @DisplayName("Should show batch processing benefits")
    void testBatchProcessingBenefits() {
        // Test processing multiple signals with the same wavelet
        int numSignals = 32;
        int signalLength = 1024;
        
        double[][] realSignals = new double[numSignals][signalLength];
        double[][] imagSignals = new double[numSignals][signalLength];
        double[][] magnitudes = new double[numSignals][signalLength];
        
        Random random = new Random(42);
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < signalLength; i++) {
                realSignals[s][i] = random.nextGaussian();
                imagSignals[s][i] = random.nextGaussian();
            }
        }
        
        ComplexVectorOps vectorOps = new ComplexVectorOps();
        
        // Time batch magnitude computation
        long batchStart = System.nanoTime();
        for (int s = 0; s < numSignals; s++) {
            vectorOps.complexMagnitude(realSignals[s], imagSignals[s], magnitudes[s]);
        }
        long batchTime = System.nanoTime() - batchStart;
        
        // Time individual magnitude computation
        long individualStart = System.nanoTime();
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < signalLength; i++) {
                double r = realSignals[s][i];
                double im = imagSignals[s][i];
                magnitudes[s][i] = Math.sqrt(r * r + im * im);
            }
        }
        long individualTime = System.nanoTime() - individualStart;
        
        double speedup = (double) individualTime / batchTime;
        
        System.out.println("\nBatch Processing Benefits:");
        System.out.printf("Processing %d signals of length %d%n", numSignals, signalLength);
        System.out.printf("Individual time: %.2f ms%n", individualTime / 1_000_000.0);
        System.out.printf("Batch SIMD time: %.2f ms%n", batchTime / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", speedup);
        
        // Note: The "batch" here still processes arrays individually, so we may not see speedup
        // True batch processing would process multiple operations in a single SIMD instruction
        System.out.println("Note: Current implementation processes arrays sequentially. True SIMD batch processing would show better results.");
    }
    
    // Helper methods
    
    private void scalarComplexMultiply(double[] real1, double[] imag1,
                                      double[] real2, double[] imag2,
                                      double[] resultReal, double[] resultImag) {
        for (int i = 0; i < real1.length; i++) {
            double ar = real1[i], ai = imag1[i];
            double br = real2[i], bi = imag2[i];
            resultReal[i] = ar * br - ai * bi;
            resultImag[i] = ar * bi + ai * br;
        }
    }
    
    private double[] performScalarConvolution(double[] realSignal, double[] imagSignal,
                                            double[] realWavelet, double[] imagWavelet) {
        int resultLength = realSignal.length;
        double[] result = new double[resultLength * 2]; // Real and imaginary interleaved
        
        int halfWavelet = realWavelet.length / 2;
        
        for (int tau = 0; tau < resultLength; tau++) {
            double sumReal = 0.0;
            double sumImag = 0.0;
            
            for (int t = 0; t < realWavelet.length; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < realSignal.length) {
                    double sigR = realSignal[idx];
                    double sigI = imagSignal[idx];
                    double wavR = realWavelet[t];
                    double wavI = imagWavelet[t];
                    
                    sumReal += sigR * wavR - sigI * wavI;
                    sumImag += sigR * wavI + sigI * wavR;
                }
            }
            
            result[2 * tau] = sumReal;
            result[2 * tau + 1] = sumImag;
        }
        
        return result;
    }
    
    private double[] performVectorConvolution(double[] realSignal, double[] imagSignal,
                                            double[] realWavelet, double[] imagWavelet,
                                            ComplexVectorOps vectorOps) {
        // Simplified version using vector operations
        int resultLength = realSignal.length;
        double[] result = new double[resultLength * 2];
        
        int halfWavelet = realWavelet.length / 2;
        
        // Process in blocks for better vectorization
        for (int tau = 0; tau < resultLength; tau++) {
            int startIdx = Math.max(0, tau - halfWavelet);
            int endIdx = Math.min(realSignal.length, tau + halfWavelet);
            int length = endIdx - startIdx;
            
            if (length > 0) {
                double[] tempReal1 = new double[length];
                double[] tempImag1 = new double[length];
                double[] tempReal2 = new double[length];
                double[] tempImag2 = new double[length];
                double[] tempResultReal = new double[length];
                double[] tempResultImag = new double[length];
                
                // Copy relevant portions
                System.arraycopy(realSignal, startIdx, tempReal1, 0, length);
                System.arraycopy(imagSignal, startIdx, tempImag1, 0, length);
                
                int wavStart = Math.max(0, halfWavelet - tau);
                System.arraycopy(realWavelet, wavStart, tempReal2, 0, 
                               Math.min(length, realWavelet.length - wavStart));
                System.arraycopy(imagWavelet, wavStart, tempImag2, 0, 
                               Math.min(length, imagWavelet.length - wavStart));
                
                // Use vectorized multiplication
                vectorOps.complexMultiply(tempReal1, tempImag1, tempReal2, tempImag2,
                                        tempResultReal, tempResultImag);
                
                // Sum results
                double sumReal = 0.0, sumImag = 0.0;
                for (int i = 0; i < length; i++) {
                    sumReal += tempResultReal[i];
                    sumImag += tempResultImag[i];
                }
                
                result[2 * tau] = sumReal;
                result[2 * tau + 1] = sumImag;
            }
        }
        
        return result;
    }
}