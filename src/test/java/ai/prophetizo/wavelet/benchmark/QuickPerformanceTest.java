package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;

import java.util.Random;

/**
 * Quick performance test to measure the impact of Phase 4 optimizations.
 * This is not a comprehensive benchmark but gives a quick indication of performance.
 */
public class QuickPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASURE_ITERATIONS = 5000;
    
    public static void main(String[] args) {
        System.out.println("Quick Performance Test - Phase 4 & 5.1 Optimizations");
        System.out.println("===================================================");
        System.out.println("Phase 4: DB2/DB4 specialization, signal size specialization");
        System.out.println("Phase 5.1: SIMD instruction selection");
        System.out.println();
        
        // Test configurations
        int[] signalSizes = {256, 1024, 16384};
        String[] waveletTypes = {"HAAR", "DB2", "DB4"};
        
        for (String waveletType : waveletTypes) {
            System.out.println("Testing " + waveletType + " wavelet:");
            System.out.println("----------------------------");
            
            for (int signalSize : signalSizes) {
                testConfiguration(waveletType, signalSize);
            }
            System.out.println();
        }
    }
    
    private static void testConfiguration(String waveletType, int signalSize) {
        // Create test signal
        double[] signal = new double[signalSize];
        Random random = new Random(42);
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 0.1 * random.nextGaussian();
        }
        
        // Create wavelet
        Wavelet wavelet = switch (waveletType) {
            case "HAAR" -> new Haar();
            case "DB2" -> Daubechies.DB2;
            case "DB4" -> Daubechies.DB4;
            default -> throw new IllegalArgumentException("Unknown wavelet: " + waveletType);
        };
        
        // Create transform with default settings (automatically selects best implementation)
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC);
        
        WaveletTransform transform = factory.create(wavelet);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        
        // Measure performance
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        long totalTime = System.nanoTime() - start;
        
        // Calculate results
        double msPerOp = (totalTime / 1_000_000.0) / MEASURE_ITERATIONS;
        double throughput = (signalSize * MEASURE_ITERATIONS) / (totalTime / 1_000_000_000.0);
        
        // Print results
        System.out.printf("  Signal size %5d: %.3fms per transform, %.2f Msamples/sec%n",
                signalSize, msPerOp, throughput / 1_000_000);
    }
}