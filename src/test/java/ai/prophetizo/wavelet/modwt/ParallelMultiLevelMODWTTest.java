package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parallel multi-level MODWT implementation.
 */
public class ParallelMultiLevelMODWTTest {
    
    private static final double EPSILON = 1e-10;
    private ParallelMultiLevelMODWT parallelTransform;
    private MultiLevelMODWTTransform sequentialTransform;
    
    @BeforeEach
    void setUp() {
        parallelTransform = new ParallelMultiLevelMODWT();
    }
    
    @Test
    void testParallelMatchesSequential_Haar() {
        // Simple test signal
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        Haar wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 3;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match"
            );
        }
    }
    
    @Test
    void testParallelMatchesSequential_DB4() {
        // Larger test signal
        double[] signal = new double[128];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        var wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 4;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match for DB4"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match for DB4"
            );
        }
    }
    
    @Test
    void testParallelWithZeroPadding() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Haar wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.ZERO_PADDING;
        int levels = 2;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match with zero padding"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match with zero padding"
            );
        }
    }
    
    @Test
    void testParallelPerformance() {
        // Large signal for performance testing
        double[] signal = new double[4096];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        var wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 6;
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
            sequentialTransform.decompose(signal, levels);
            parallelTransform.decompose(signal, wavelet, mode, levels);
        }
        
        // Time sequential
        long sequentialStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
            sequentialTransform.decompose(signal, levels);
        }
        long sequentialTime = System.nanoTime() - sequentialStart;
        
        // Time parallel
        long parallelStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            parallelTransform.decompose(signal, wavelet, mode, levels);
        }
        long parallelTime = System.nanoTime() - parallelStart;
        
        double speedup = (double) sequentialTime / parallelTime;
        System.out.printf("Sequential: %.2f ms, Parallel: %.2f ms, Speedup: %.2fx%n",
            sequentialTime / 1e6, parallelTime / 1e6, speedup);
        
        // Parallel should be at least as fast (allowing some margin for small signals)
        assertTrue(speedup > 0.8, "Parallel should not be significantly slower than sequential");
    }
    
    @Test
    void testCustomExecutor() {
        // Test with custom executor
        ForkJoinPool customPool = new ForkJoinPool(2);
        try {
            ParallelMultiLevelMODWT customTransform = new ParallelMultiLevelMODWT(customPool);
            
            double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
            Haar wavelet = new Haar();
            
            MultiLevelMODWTResult result = customTransform.decompose(
                signal, wavelet, BoundaryMode.PERIODIC, 2);
            
            assertNotNull(result);
            assertEquals(2, result.getLevels());
            assertEquals(signal.length, result.getSignalLength());
            
        } finally {
            customPool.shutdown();
        }
    }
    
    @Test
    void testEdgeCases() {
        ParallelMultiLevelMODWT transform = new ParallelMultiLevelMODWT();
        
        // Test single level
        double[] signal = {1, 2, 3, 4};
        MultiLevelMODWTResult result = transform.decompose(
            signal, new Haar(), BoundaryMode.PERIODIC, 1);
        
        assertNotNull(result);
        assertEquals(1, result.getLevels());
        
        // Test invalid inputs
        assertThrows(InvalidSignalException.class, () ->
            transform.decompose(new double[0], new Haar(), BoundaryMode.PERIODIC, 1));
        
        assertThrows(InvalidArgumentException.class, () ->
            transform.decompose(signal, new Haar(), BoundaryMode.PERIODIC, 0));
        
        assertThrows(InvalidArgumentException.class, () ->
            transform.decompose(signal, new Haar(), BoundaryMode.PERIODIC, 10));
    }
}