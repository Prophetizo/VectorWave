package ai.prophetizo;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;

/**
 * Quick demonstration of SIMD optimization performance.
 */
public class SIMDDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave SIMD Optimization Demo");
        System.out.println("=================================\n");
        
        // Create test signal (financial-like data)
        int signalSize = 1024;
        double[] signal = new double[signalSize];
        signal[0] = 100.0;
        for (int i = 1; i < signalSize; i++) {
            double randomWalk = (Math.random() - 0.5) * 0.02;
            signal[i] = signal[i-1] * (1 + randomWalk);
        }
        
        // Create transforms
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        
        TransformConfig simdConfig = TransformConfig.builder()
            .forceVector(true)
            .build();
        
        WaveletTransform scalarTransform = new WaveletTransform(wavelet, mode, scalarConfig);
        WaveletTransform simdTransform = new WaveletTransform(wavelet, mode, simdConfig);
        WaveletTransform autoTransform = new WaveletTransform(wavelet, mode);
        
        System.out.println("Transform Implementations:");
        System.out.println("- Scalar: " + scalarTransform.getImplementationType());
        System.out.println("- SIMD:   " + simdTransform.getImplementationType());
        System.out.println("- Auto:   " + autoTransform.getImplementationType());
        System.out.println();
        System.out.println("Note: Auto now uses optimized vector operations with:");
        System.out.println("  - Efficient memory gather operations");
        System.out.println("  - Combined transform for better cache usage");
        System.out.println("  - Specialized Haar implementation");
        System.out.println();
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            scalarTransform.forward(signal);
            simdTransform.forward(signal);
        }
        
        // Benchmark
        System.out.println("Performance Comparison (average of 10,000 runs):");
        System.out.println("Signal size: " + signalSize + " samples");
        System.out.println();
        
        // Scalar timing
        long startScalar = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            TransformResult result = scalarTransform.forward(signal);
        }
        long endScalar = System.nanoTime();
        double scalarTime = (endScalar - startScalar) / 10_000_000.0; // Convert to ms per operation
        
        // SIMD timing
        long startSIMD = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            TransformResult result = simdTransform.forward(signal);
        }
        long endSIMD = System.nanoTime();
        double simdTime = (endSIMD - startSIMD) / 10_000_000.0; // Convert to ms per operation
        
        // Auto timing
        long startAuto = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            TransformResult result = autoTransform.forward(signal);
        }
        long endAuto = System.nanoTime();
        double autoTime = (endAuto - startAuto) / 10_000_000.0; // Convert to ms per operation
        
        System.out.printf("Scalar:     %.3f ms per transform\n", scalarTime);
        System.out.printf("SIMD:       %.3f ms per transform\n", simdTime);
        System.out.printf("Auto:       %.3f ms per transform\n", autoTime);
        System.out.println();
        
        double speedup = scalarTime / simdTime;
        System.out.printf("SIMD Speedup: %.2fx faster than scalar\n", speedup);
        
        // Verify correctness
        TransformResult scalarResult = scalarTransform.forward(signal);
        TransformResult simdResult = simdTransform.forward(signal);
        
        double maxDiff = 0.0;
        double[] scalarApprox = scalarResult.approximationCoeffs();
        double[] simdApprox = simdResult.approximationCoeffs();
        
        for (int i = 0; i < scalarApprox.length; i++) {
            double diff = Math.abs(scalarApprox[i] - simdApprox[i]);
            maxDiff = Math.max(maxDiff, diff);
        }
        
        System.out.println();
        System.out.println("Correctness Verification:");
        System.out.printf("Maximum difference between scalar and SIMD: %.2e\n", maxDiff);
        System.out.println(maxDiff < 1e-10 ? "✓ Results are identical" : "✗ Results differ!");
    }
}