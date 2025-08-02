package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.util.PlatformDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance validation tests for ARM/Apple Silicon optimizations.
 */
public class ARMPerformanceTest {
    
    @Test
    @EnabledOnOs(OS.MAC)
    void testARMOptimizationDetection() {
        // Check that ARM optimization is detected on Apple Silicon
        String implementations = WaveletOpsFactory.getAvailableImplementations();
        System.out.println("Available implementations:\n" + implementations);
        
        if (PlatformDetector.isARM()) {
            assertTrue(implementations.contains("ARM Optimized"));
            
            // Verify we're using ARM ops
            WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
            assertEquals("ARM Optimized (Apple Silicon)", 
                WaveletOpsFactory.createOptimal().getImplementationType());
        }
    }
    
    @Test
    void testPerformanceImprovement() {
        // Test with various signal sizes
        int[] sizes = {64, 128, 256, 512, 1024};
        
        for (int size : sizes) {
            double[] signal = generateSignal(size);
            
            // Test with scalar operations
            long scalarTime = measureTransformTime(signal, true);
            
            // Test with optimized operations
            long optimizedTime = measureTransformTime(signal, false);
            
            System.out.printf("Size %d: Scalar=%d ns, Optimized=%d ns, Speedup=%.2fx%n",
                size, scalarTime, optimizedTime, 
                (double) scalarTime / optimizedTime);
            
            // On Apple Silicon, we should see some improvement even with 2-element vectors
            // Don't assert specific speedup as it varies by size and system load
        }
    }
    
    @Test
    void testSpecializedKernels() {
        // Test Haar transform (2 coefficients)
        testSpecializedKernel(new Haar(), 128);
        
        // Test DB2 transform (4 coefficients)
        testSpecializedKernel(Daubechies.DB2, 128);
    }
    
    private void testSpecializedKernel(ai.prophetizo.wavelet.api.Wavelet wavelet, int size) {
        double[] signal = generateSignal(size);
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            transform.forward(signal);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            transform.forward(signal);
        }
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("%s kernel performance: %.2f ns/op%n", 
            wavelet.name(), elapsed / 1000.0);
    }
    
    private double[] generateSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(8 * Math.PI * i / 32.0);
        }
        return signal;
    }
    
    private long measureTransformTime(double[] signal, boolean forceScalar) {
        TransformConfig config = TransformConfig.builder()
            .forceScalar(forceScalar)
            .build();
            
        WaveletTransform transform = new WaveletTransform(
            new Haar(), BoundaryMode.PERIODIC, config);
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            transform.forward(signal);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            transform.forward(signal);
        }
        
        return (System.nanoTime() - start) / 1000;
    }
}