package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.util.ThreadLocalManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for batch SIMD MODWT implementation.
 */
public class BatchSIMDMODWTTest {
    
    private static final double EPSILON = 1e-10;
    
    @AfterEach
    void cleanup() {
        // Use ThreadLocalManager directly for cleanup
        ThreadLocalManager.cleanupCurrentThread();
    }
    
    @Test
    void testSoAConversion() {
        // Test data
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0},
            {9.0, 10.0, 11.0, 12.0}
        };
        
        // Convert to SoA
        double[] soa = new double[12];
        BatchSIMDMODWT.convertToSoA(signals, soa);
        
        // Verify SoA layout: [1,5,9, 2,6,10, 3,7,11, 4,8,12]
        double[] expected = {1.0, 5.0, 9.0, 2.0, 6.0, 10.0, 3.0, 7.0, 11.0, 4.0, 8.0, 12.0};
        assertArrayEquals(expected, soa, EPSILON);
        
        // Convert back
        double[][] reconstructed = new double[3][4];
        BatchSIMDMODWT.convertFromSoA(soa, reconstructed);
        
        // Verify reconstruction
        for (int i = 0; i < signals.length; i++) {
            assertArrayEquals(signals[i], reconstructed[i], EPSILON);
        }
    }
    
    @Test
    void testHaarBatchMODWT() {
        // Create test batch
        int batchSize = 8;
        int signalLength = 16;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize with simple patterns
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = (b + 1) * Math.sin(2 * Math.PI * t / signalLength);
            }
        }
        
        // Sequential reference
        MODWTTransform sequentialTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        double[][] expectedApprox = new double[batchSize][signalLength];
        double[][] expectedDetail = new double[batchSize][signalLength];
        
        for (int b = 0; b < batchSize; b++) {
            MODWTResult result = sequentialTransform.forward(signals[b]);
            expectedApprox[b] = result.approximationCoeffs();
            expectedDetail[b] = result.detailCoeffs();
        }
        
        // SIMD batch processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    new Haar(), batchSize, signalLength);
        
        double[][] actualApprox = new double[batchSize][signalLength];
        double[][] actualDetail = new double[batchSize][signalLength];
        
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        BatchSIMDMODWT.convertFromSoA(soaDetail, actualDetail);
        
        // Compare results
        for (int b = 0; b < batchSize; b++) {
            assertArrayEquals(expectedApprox[b], actualApprox[b], EPSILON,
                "Approximation coefficients should match for signal " + b);
            assertArrayEquals(expectedDetail[b], actualDetail[b], EPSILON,
                "Detail coefficients should match for signal " + b);
        }
    }
    
    @Test
    void testDB4BatchMODWT() {
        // Create test batch with random signals
        int batchSize = 16;
        int signalLength = 64;
        double[][] signals = new double[batchSize][signalLength];
        
        Random random = new Random(42);
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = random.nextGaussian();
            }
        }
        
        // Sequential reference
        MODWTTransform sequentialTransform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        double[][] expectedApprox = new double[batchSize][signalLength];
        double[][] expectedDetail = new double[batchSize][signalLength];
        
        for (int b = 0; b < batchSize; b++) {
            MODWTResult result = sequentialTransform.forward(signals[b]);
            expectedApprox[b] = result.approximationCoeffs();
            expectedDetail[b] = result.detailCoeffs();
        }
        
        // SIMD batch processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    Daubechies.DB4, batchSize, signalLength);
        
        double[][] actualApprox = new double[batchSize][signalLength];
        double[][] actualDetail = new double[batchSize][signalLength];
        
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        BatchSIMDMODWT.convertFromSoA(soaDetail, actualDetail);
        
        // Compare results
        for (int b = 0; b < batchSize; b++) {
            assertArrayEquals(expectedApprox[b], actualApprox[b], EPSILON,
                "DB4 approximation coefficients should match for signal " + b);
            assertArrayEquals(expectedDetail[b], actualDetail[b], EPSILON,
                "DB4 detail coefficients should match for signal " + b);
        }
    }
    
    @Test
    void testNonAlignedBatchSize() {
        // Test with batch size that's not a multiple of vector length
        int batchSize = 7; // Not a power of 2
        int signalLength = 32;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize signals
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = b + t * 0.1;
            }
        }
        
        // SIMD processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    new Haar(), batchSize, signalLength);
        
        // Should not throw and produce valid results
        double[][] actualApprox = new double[batchSize][signalLength];
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        
        // Basic validation - check first few values are non-zero
        assertNotEquals(0.0, actualApprox[0][0]);
        assertNotEquals(0.0, actualApprox[batchSize-1][0]);
    }
    
    @Test
    void testBatchTransformIntegration() {
        // Test integration with MODWTTransform batch processing
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        int batchSize = 8;
        int signalLength = 128;
        double[][] signals = new double[batchSize][signalLength];
        
        Random random = new Random(42);
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = random.nextGaussian();
            }
        }
        
        // Process batch through transform
        MODWTResult[] results = transform.forwardBatch(signals);
        
        assertNotNull(results);
        assertEquals(batchSize, results.length);
        
        for (int b = 0; b < batchSize; b++) {
            assertEquals(signalLength, results[b].approximationCoeffs().length);
            assertEquals(signalLength, results[b].detailCoeffs().length);
        }
    }
}