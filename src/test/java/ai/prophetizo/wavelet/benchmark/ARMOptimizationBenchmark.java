package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.WaveletOpsFactory;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsARM;
import ai.prophetizo.wavelet.util.PlatformDetector;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to measure performance improvements of ARM-specific optimizations.
 */
public class ARMOptimizationBenchmark {
    
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASURE_ITERATIONS = 10000;
    private static final int[] SIGNAL_SIZES = {128, 256, 512, 1024, 2048, 4096};
    
    public static void main(String[] args) {
        System.out.println("ARM Optimization Benchmark");
        System.out.println("==========================");
        System.out.println("Platform: " + System.getProperty("os.arch"));
        System.out.println("Is ARM: " + PlatformDetector.isARM());
        System.out.println("Is Apple Silicon: " + PlatformDetector.isAppleSilicon());
        System.out.println();
        
        // Test both Haar and DB4 wavelets
        benchmarkWavelet(new Haar(), "Haar");
        benchmarkWavelet(Daubechies.DB4, "Daubechies-4");
    }
    
    private static void benchmarkWavelet(ai.prophetizo.wavelet.api.Wavelet wavelet, String waveletName) {
        System.out.println("\n" + waveletName + " Wavelet Benchmarks");
        System.out.println("----------------------------------------");
        
        double[] filter = wavelet.lowPassDecomposition();
        
        for (int signalSize : SIGNAL_SIZES) {
            System.out.println("\nSignal Size: " + signalSize);
            
            // Generate test data
            double[] signal = generateRandomSignal(signalSize);
            double[] downsampled = new double[signalSize / 2];
            
            // Downsample for upsampling tests
            for (int i = 0; i < downsampled.length; i++) {
                downsampled[i] = signal[i * 2];
            }
            
            // Benchmark downsampling
            benchmarkDownsampling(signal, filter, signalSize, filter.length);
            
            // Benchmark upsampling
            benchmarkUpsampling(downsampled, filter, downsampled.length, filter.length);
        }
    }
    
    private static void benchmarkDownsampling(double[] signal, double[] filter, 
                                            int signalLength, int filterLength) {
        System.out.println("\n  Downsampling (Convolution + Decimation):");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            VectorOps.convolveAndDownsamplePeriodic(signal, filter, signalLength, filterLength);
            if (PlatformDetector.isARM()) {
                VectorOpsARM.convolveAndDownsampleARM(signal, filter, signalLength, filterLength);
            }
        }
        
        // Measure standard implementation
        long standardTime = measureDownsampling(
            () -> VectorOps.convolveAndDownsamplePeriodic(signal, filter, signalLength, filterLength)
        );
        
        // Measure ARM implementation if on ARM platform
        if (PlatformDetector.isARM()) {
            long armTime = measureDownsampling(
                () -> VectorOpsARM.convolveAndDownsampleARM(signal, filter, signalLength, filterLength)
            );
            
            double speedup = (double) standardTime / armTime;
            System.out.printf("    Standard: %,d ns%n", standardTime);
            System.out.printf("    ARM:      %,d ns (%.2fx speedup)%n", armTime, speedup);
        } else {
            System.out.printf("    Standard: %,d ns%n", standardTime);
            System.out.println("    ARM:      N/A (not on ARM platform)");
        }
    }
    
    private static void benchmarkUpsampling(double[] signal, double[] filter,
                                          int signalLength, int filterLength) {
        System.out.println("\n  Upsampling (Interpolation + Convolution):");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            VectorOps.upsampleAndConvolvePeriodic(signal, filter, signalLength, filterLength);
            if (PlatformDetector.isARM()) {
                VectorOpsARM.upsampleAndConvolvePeriodicARM(signal, filter, signalLength, filterLength);
            }
        }
        
        // Measure standard implementation
        long standardTime = measureUpsampling(
            () -> VectorOps.upsampleAndConvolvePeriodic(signal, filter, signalLength, filterLength)
        );
        
        // Measure ARM implementation if on ARM platform
        if (PlatformDetector.isARM()) {
            long armTime = measureUpsampling(
                () -> VectorOpsARM.upsampleAndConvolvePeriodicARM(signal, filter, signalLength, filterLength)
            );
            
            double speedup = (double) standardTime / armTime;
            System.out.printf("    Standard: %,d ns%n", standardTime);
            System.out.printf("    ARM:      %,d ns (%.2fx speedup)%n", armTime, speedup);
        } else {
            System.out.printf("    Standard: %,d ns%n", standardTime);
            System.out.println("    ARM:      N/A (not on ARM platform)");
        }
    }
    
    private static long measureDownsampling(Runnable operation) {
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            operation.run();
        }
        long endTime = System.nanoTime();
        return (endTime - startTime) / MEASURE_ITERATIONS;
    }
    
    private static long measureUpsampling(Runnable operation) {
        return measureDownsampling(operation); // Same measurement logic
    }
    
    private static double[] generateRandomSignal(int length) {
        Random random = new Random(42); // Fixed seed for reproducibility
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
}