package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import ai.prophetizo.wavelet.memory.BatchMemoryLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for batch SIMD wavelet transform implementation.
 */
@DisplayName("Batch SIMD Transform Tests")
public class BatchSIMDTransformTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final Random random = new Random(42);
    
    @Test
    @DisplayName("Test Haar batch transform correctness")
    public void testHaarBatchCorrectness() {
        int batchSize = 8;
        int signalLength = 64;
        
        // Generate test signals
        double[][] signals = generateTestSignals(batchSize, signalLength);
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        // Perform batch transform
        BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults);
        
        // Verify against sequential implementation
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approxResults[i], TOLERANCE,
                "Approximation coefficients mismatch for signal " + i);
            assertArrayEquals(expected.detailCoeffs(), detailResults[i], TOLERANCE,
                "Detail coefficients mismatch for signal " + i);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16, 32})
    @DisplayName("Test batch sizes")
    public void testDifferentBatchSizes(int batchSize) {
        int signalLength = 128;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        // Should not throw for any batch size
        assertDoesNotThrow(() -> 
            BatchSIMDTransform.haarBatchTransformSIMD(signals, approxResults, detailResults)
        );
        
        // Verify non-zero results
        for (int i = 0; i < batchSize; i++) {
            assertNotAllZero(approxResults[i], "Approximation coefficients are all zero");
            assertNotAllZero(detailResults[i], "Detail coefficients are all zero");
        }
    }
    
    @Test
    @DisplayName("Test general wavelet batch transform")
    public void testGeneralWaveletBatch() {
        int batchSize = 4;
        int signalLength = 64;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        // Test with DB4
        Wavelet db4 = Daubechies.DB4;
        double[] lowPass = db4.lowPassDecomposition();
        double[] highPass = db4.highPassDecomposition();
        
        BatchSIMDTransform.blockedBatchTransformSIMD(
            signals, approxResults, detailResults, lowPass, highPass
        );
        
        // Verify against sequential
        WaveletTransform transform = new WaveletTransform(db4, BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approxResults[i], TOLERANCE,
                "DB4 approximation mismatch for signal " + i);
            assertArrayEquals(expected.detailCoeffs(), detailResults[i], TOLERANCE,
                "DB4 detail mismatch for signal " + i);
        }
    }
    
    @Test
    @DisplayName("Test aligned batch transform")
    public void testAlignedBatchTransform() {
        // Use perfectly aligned batch size
        int batchSize = 8; // Multiple of typical vector length
        int signalLength = 128;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        Wavelet haar = new Haar();
        double[] lowPass = haar.lowPassDecomposition();
        double[] highPass = haar.highPassDecomposition();
        
        // Test aligned version
        BatchSIMDTransform.alignedBatchTransformSIMD(
            signals, approxResults, detailResults, lowPass, highPass
        );
        
        // Verify correctness
        WaveletTransform transform = new WaveletTransform(haar, BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approxResults[i], TOLERANCE);
            assertArrayEquals(expected.detailCoeffs(), detailResults[i], TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Test adaptive batch transform")
    public void testAdaptiveBatchTransform() {
        // Test different scenarios
        testAdaptiveScenario(4, 64, new Haar());      // Small batch, Haar
        testAdaptiveScenario(16, 512, Daubechies.DB2); // Medium batch, DB2
        testAdaptiveScenario(32, 1024, Daubechies.DB4); // Large batch, DB4
    }
    
    private void testAdaptiveScenario(int batchSize, int signalLength, Wavelet wavelet) {
        double[][] signals = generateTestSignals(batchSize, signalLength);
        double[][] approxResults = new double[batchSize][signalLength / 2];
        double[][] detailResults = new double[batchSize][signalLength / 2];
        
        double[] lowPass = wavelet.lowPassDecomposition();
        double[] highPass = wavelet.highPassDecomposition();
        
        BatchSIMDTransform.adaptiveBatchTransform(
            signals, approxResults, detailResults, lowPass, highPass
        );
        
        // Verify correctness
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        for (int i = 0; i < batchSize; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approxResults[i], TOLERANCE,
                String.format("Adaptive mismatch for %s, batch %d, signal %d", 
                    wavelet.name(), batchSize, i));
        }
    }
    
    @Test
    @DisplayName("Test batch memory layout")
    public void testBatchMemoryLayout() {
        int batchSize = 7; // Non-aligned size
        int signalLength = 64;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        
        try (BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength)) {
            // Test loading and extraction
            layout.loadSignals(signals);
            
            double[][] approxResults = new double[batchSize][signalLength / 2];
            double[][] detailResults = new double[batchSize][signalLength / 2];
            
            // Perform transform
            layout.haarTransformAligned();
            
            // Extract results
            layout.extractResults(approxResults, detailResults);
            
            // Verify some results are non-zero
            for (int i = 0; i < batchSize; i++) {
                assertNotAllZero(approxResults[i], "Layout approx all zero");
                assertNotAllZero(detailResults[i], "Layout detail all zero");
            }
        }
    }
    
    @Test
    @DisplayName("Test interleaved memory layout")
    public void testInterleavedLayout() {
        int batchSize = 4;
        int signalLength = 32;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        
        try (BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength)) {
            // Test interleaved loading
            layout.loadSignalsInterleaved(signals, true);
            
            double[][] approxResults = new double[batchSize][signalLength / 2];
            double[][] detailResults = new double[batchSize][signalLength / 2];
            
            layout.haarTransformAligned();
            layout.extractResultsInterleaved(approxResults, detailResults);
            
            // Results should be valid
            for (int i = 0; i < batchSize; i++) {
                assertNotAllZero(approxResults[i]);
                assertNotAllZero(detailResults[i]);
            }
        }
    }
    
    @Test
    @DisplayName("Test WaveletTransform batch API")
    public void testWaveletTransformBatchAPI() {
        int batchSize = 8;
        int signalLength = 64;
        
        double[][] signals = generateTestSignals(batchSize, signalLength);
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Test forward batch
        TransformResult[] results = transform.forwardBatch(signals);
        assertEquals(batchSize, results.length);
        
        // Test inverse batch
        double[][] reconstructed = transform.inverseBatch(results);
        assertEquals(batchSize, reconstructed.length);
        
        // Verify reconstruction
        for (int i = 0; i < batchSize; i++) {
            assertArrayEquals(signals[i], reconstructed[i], 1e-8,
                "Reconstruction mismatch for signal " + i);
        }
    }
    
    @Test
    @DisplayName("Test edge cases")
    public void testEdgeCases() {
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Empty batch
        TransformResult[] emptyResults = transform.forwardBatch(new double[0][]);
        assertEquals(0, emptyResults.length);
        
        // Null handling
        assertEquals(0, transform.forwardBatch(null).length);
        assertEquals(0, transform.inverseBatch(null).length);
        
        // Single signal batch
        double[][] singleSignal = {generateTestSignals(1, 32)[0]};
        TransformResult[] singleResult = transform.forwardBatch(singleSignal);
        assertEquals(1, singleResult.length);
    }
    
    private double[][] generateTestSignals(int batchSize, int signalLength) {
        double[][] signals = new double[batchSize][signalLength];
        
        for (int i = 0; i < batchSize; i++) {
            // Mix of different signal types
            if (i % 3 == 0) {
                // Random signal
                for (int j = 0; j < signalLength; j++) {
                    signals[i][j] = random.nextGaussian();
                }
            } else if (i % 3 == 1) {
                // Sinusoidal
                for (int j = 0; j < signalLength; j++) {
                    signals[i][j] = Math.sin(2 * Math.PI * j / signalLength);
                }
            } else {
                // Step function
                for (int j = 0; j < signalLength; j++) {
                    signals[i][j] = j < signalLength / 2 ? 1.0 : -1.0;
                }
            }
        }
        
        return signals;
    }
    
    private void assertNotAllZero(double[] array, String message) {
        boolean hasNonZero = false;
        for (double val : array) {
            if (Math.abs(val) > TOLERANCE) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero, message);
    }
    
    private void assertNotAllZero(double[] array) {
        assertNotAllZero(array, "Array contains only zeros");
    }
}