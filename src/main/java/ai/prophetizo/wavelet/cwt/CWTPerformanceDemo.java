package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.optimization.CWTVectorOps;

/**
 * Demonstrates the performance improvements of CWT with SIMD optimizations.
 */
public class CWTPerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("CWT Performance Demonstration");
        System.out.println("============================\n");
        
        // Create test signal
        int signalLength = 4096;
        double[] signal = createChirpSignal(signalLength);
        
        // Create wavelet and scales
        MorletWavelet wavelet = new MorletWavelet();
        double[] scales = createLogScales(1.0, 128.0, 64);
        
        // Platform info
        CWTVectorOps vectorOps = new CWTVectorOps();
        CWTVectorOps.PlatformInfo platform = vectorOps.getPlatformInfo();
        System.out.println("Platform Information:");
        System.out.println("- Vector length: " + platform.vectorLength() + " bits");
        System.out.println("- Apple Silicon: " + platform.isAppleSilicon());
        System.out.println("- AVX-512: " + platform.hasAVX512());
        System.out.println("- AVX2: " + platform.hasAVX2());
        System.out.println("- SIMD Support: " + platform.supportsSIMD());
        System.out.println();
        
        // Test with SIMD-optimized transform
        System.out.println("Testing SIMD-optimized CWT...");
        CWTTransform optimizedTransform = CWTFactory.createOptimizedForJava23(wavelet);
        
        // Warm-up
        for (int i = 0; i < 5; i++) {
            optimizedTransform.analyze(signal, scales);
        }
        
        // Measure performance
        long startTime = System.nanoTime();
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            CWTResult result = optimizedTransform.analyze(signal, scales);
        }
        
        long endTime = System.nanoTime();
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        System.out.printf("Average time per transform: %.2f ms%n", avgTimeMs);
        System.out.printf("Samples per second: %.0f%n", signalLength / (avgTimeMs / 1000.0));
        System.out.printf("Total coefficients computed: %d%n", signalLength * scales.length);
        System.out.printf("Coefficients per second: %.0f%n", 
            (signalLength * scales.length) / (avgTimeMs / 1000.0));
        
        // Test without SIMD (scalar fallback)
        System.out.println("\nTesting scalar CWT (no SIMD)...");
        CWTConfig scalarConfig = CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(true)
            .build();
        CWTTransform scalarTransform = new CWTTransform(wavelet, scalarConfig);
        
        // Force scalar computation by using small signal chunks
        double[] smallSignal = new double[32]; // Below SIMD threshold
        System.arraycopy(signal, 0, smallSignal, 0, 32);
        
        startTime = System.nanoTime();
        for (int i = 0; i < iterations * 128; i++) { // More iterations for small signal
            scalarTransform.analyze(smallSignal, new double[]{2.0, 4.0, 8.0});
        }
        endTime = System.nanoTime();
        
        double scalarTimeMs = (endTime - startTime) / 1_000_000.0 / (iterations * 128);
        System.out.printf("Average time per small transform: %.4f ms%n", scalarTimeMs);
        
        // Demonstrate different optimization strategies
        System.out.println("\nOptimization Strategy Selection:");
        CWTVectorOps.OptimizationStrategy smallStrategy = 
            vectorOps.selectStrategy(64, 16);
        CWTVectorOps.OptimizationStrategy mediumStrategy = 
            vectorOps.selectStrategy(512, 32);
        CWTVectorOps.OptimizationStrategy largeStrategy = 
            vectorOps.selectStrategy(4096, 64);
        
        System.out.println("- Small signal (64 samples): " + 
            describeStrategy(smallStrategy));
        System.out.println("- Medium signal (512 samples): " + 
            describeStrategy(mediumStrategy));
        System.out.println("- Large signal (4096 samples): " + 
            describeStrategy(largeStrategy));
    }
    
    private static double[] createChirpSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 10 + 40 * t; // Frequency increases from 10 to 50
            signal[i] = Math.sin(2 * Math.PI * freq * t);
        }
        return signal;
    }
    
    private static double[] createLogScales(double min, double max, int count) {
        double[] scales = new double[count];
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        double step = (logMax - logMin) / (count - 1);
        
        for (int i = 0; i < count; i++) {
            scales[i] = Math.exp(logMin + i * step);
        }
        return scales;
    }
    
    private static String describeStrategy(CWTVectorOps.OptimizationStrategy strategy) {
        if (strategy.useDirectComputation()) {
            return "Direct computation";
        } else if (strategy.useBlockedComputation()) {
            return "Blocked computation (cache-optimized)";
        } else if (strategy.useFFT()) {
            return "FFT-based computation";
        }
        return "Unknown";
    }
}