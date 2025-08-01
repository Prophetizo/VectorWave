package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FFM-based wavelet transform implementation.
 * Verifies correctness and compatibility with traditional implementation.
 * 
 * @since 2.0.0
 */
class FFMWaveletTransformTest {
    
    private static final double TOLERANCE = 1e-10;
    private FFMWaveletTransform ffmTransform;
    private WaveletTransform traditionalTransform;
    
    @AfterEach
    void tearDown() {
        if (ffmTransform != null) {
            ffmTransform.close();
        }
    }
    
    @Test
    void testBasicForwardTransform() {
        Haar haar = new Haar();
        ffmTransform = new FFMWaveletTransform(haar);
        traditionalTransform = new WaveletTransform(haar, BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        TransformResult ffmResult = ffmTransform.forward(signal);
        TransformResult tradResult = traditionalTransform.forward(signal);
        
        assertArrayEquals(tradResult.approximationCoeffs(), 
                         ffmResult.approximationCoeffs(), TOLERANCE);
        assertArrayEquals(tradResult.detailCoeffs(), 
                         ffmResult.detailCoeffs(), TOLERANCE);
    }
    
    @Test
    void testInverseTransform() {
        Haar haar = new Haar();
        ffmTransform = new FFMWaveletTransform(haar);
        traditionalTransform = new WaveletTransform(haar, BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        TransformResult forward = ffmTransform.forward(signal);
        double[] reconstructed = ffmTransform.inverse(forward);
        
        assertArrayEquals(signal, reconstructed, TOLERANCE);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {32, 64, 128, 256, 512, 1024})
    void testVariousSignalSizes(int size) {
        Daubechies db4 = new Daubechies(4);
        ffmTransform = new FFMWaveletTransform(db4);
        traditionalTransform = new WaveletTransform(db4, BoundaryMode.PERIODIC);
        
        double[] signal = generateRandomSignal(size);
        
        TransformResult ffmResult = ffmTransform.forward(signal);
        TransformResult tradResult = traditionalTransform.forward(signal);
        
        assertArrayEquals(tradResult.approximationCoeffs(), 
                         ffmResult.approximationCoeffs(), TOLERANCE);
        assertArrayEquals(tradResult.detailCoeffs(), 
                         ffmResult.detailCoeffs(), TOLERANCE);
    }
    
    @Test
    void testZeroCopySlice() {
        Haar haar = new Haar();
        ffmTransform = new FFMWaveletTransform(haar);
        
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i;
        }
        
        // Test slice processing
        int offset = 64;
        int length = 128;
        
        TransformResult sliceResult = ffmTransform.forward(signal, offset, length);
        
        // Compare with traditional processing of the slice
        double[] slice = new double[length];
        System.arraycopy(signal, offset, slice, 0, length);
        traditionalTransform = new WaveletTransform(haar, BoundaryMode.PERIODIC);
        TransformResult expectedResult = traditionalTransform.forward(slice);
        
        assertArrayEquals(expectedResult.approximationCoeffs(), 
                         sliceResult.approximationCoeffs(), TOLERANCE);
        assertArrayEquals(expectedResult.detailCoeffs(), 
                         sliceResult.detailCoeffs(), TOLERANCE);
    }
    
    @Test
    void testMemoryPoolReuse() {
        FFMMemoryPool pool = new FFMMemoryPool();
        Haar haar = new Haar();
        ffmTransform = new FFMWaveletTransform(haar, pool);
        
        double[] signal = generateRandomSignal(256);
        
        // Perform multiple transforms
        for (int i = 0; i < 100; i++) {
            TransformResult result = ffmTransform.forward(signal);
            assertNotNull(result);
        }
        
        // Check pool statistics
        var stats = ffmTransform.getPoolStatistics();
        assertTrue(stats.hitRate() > 0.8, "Pool hit rate should be high after warmup");
        
        pool.close();
    }
    
    @Test
    void testForwardInverseScoped() {
        Symlet sym8 = new Symlet(8);
        ffmTransform = new FFMWaveletTransform(sym8);
        
        double[] signal = generateRandomSignal(512);
        double[] reconstructed = ffmTransform.forwardInverse(signal);
        
        assertArrayEquals(signal, reconstructed, TOLERANCE);
    }
    
    @Test
    void testStreamingTransform() throws Exception {
        Haar haar = new Haar();
        int blockSize = 128;
        
        try (FFMStreamingTransform streaming = new FFMStreamingTransform(haar, blockSize)) {
            double[] data = generateRandomSignal(blockSize * 3);
            
            // Process in chunks
            int processed = 0;
            int resultsReceived = 0;
            
            for (int i = 0; i < data.length; i += blockSize / 2) {
                int chunkSize = Math.min(blockSize / 2, data.length - i);
                streaming.processChunk(data, i, chunkSize);
                processed += chunkSize;
                
                while (streaming.hasCompleteBlock()) {
                    TransformResult result = streaming.getNextResult();
                    assertNotNull(result);
                    assertEquals(blockSize / 2, result.approximationCoeffs().length);
                    assertEquals(blockSize / 2, result.detailCoeffs().length);
                    resultsReceived++;
                }
            }
            
            assertTrue(resultsReceived > 0, "Should have received transform results");
        }
    }
    
    @Test
    void testMemoryAlignment() {
        FFMMemoryPool pool = new FFMMemoryPool();
        
        // Test various sizes for alignment
        int[] sizes = {32, 64, 128, 256, 512, 1024};
        
        for (int size : sizes) {
            var segment = pool.acquire(size);
            assertTrue(FFMArrayAllocator.isAligned(segment), 
                      "Segment should be SIMD-aligned for size: " + size);
            pool.release(segment);
        }
        
        pool.close();
    }
    
    @Test
    void testBiorthogonalWavelets() {
        BiorthogonalSpline bior = new BiorthogonalSpline(3, 5);
        ffmTransform = new FFMWaveletTransform(bior);
        traditionalTransform = new WaveletTransform(bior, BoundaryMode.PERIODIC);
        
        double[] signal = generateRandomSignal(256);
        
        TransformResult ffmResult = ffmTransform.forward(signal);
        TransformResult tradResult = traditionalTransform.forward(signal);
        
        assertArrayEquals(tradResult.approximationCoeffs(), 
                         ffmResult.approximationCoeffs(), TOLERANCE);
        assertArrayEquals(tradResult.detailCoeffs(), 
                         ffmResult.detailCoeffs(), TOLERANCE);
        
        // Test reconstruction
        double[] ffmRecon = ffmTransform.inverse(ffmResult);
        double[] tradRecon = traditionalTransform.inverse(tradResult);
        
        assertArrayEquals(tradRecon, ffmRecon, TOLERANCE);
        assertArrayEquals(signal, ffmRecon, 1e-6); // Biorthogonal has some reconstruction error
    }
    
    private double[] generateRandomSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
}