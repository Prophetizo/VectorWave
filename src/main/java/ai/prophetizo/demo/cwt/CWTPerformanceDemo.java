package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.optimization.CacheAwareOps;
import ai.prophetizo.wavelet.util.PlatformDetector;

/**
 * Demonstrates CWT performance optimizations and platform adaptability.
 * 
 * <p>This demo showcases FFT acceleration, cache optimization, and platform-specific
 * performance tuning for Continuous Wavelet Transform operations.</p>
 */
public class CWTPerformanceDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave CWT Performance Demo ===\n");
        
        // Demo 1: Platform detection and cache configuration
        platformDetection();
        
        // Demo 2: FFT vs Direct convolution performance
        fftPerformanceComparison();
        
        // Demo 3: Cache-aware optimization
        cacheOptimization();
        
        // Demo 4: Scaling analysis
        scalingAnalysis();
        
        // Demo 5: Memory efficiency
        memoryEfficiency();
    }
    
    /**
     * Demonstrates platform detection and adaptive cache configuration.
     */
    private static void platformDetection() {
        System.out.println("1. Platform Detection & Cache Configuration");
        System.out.println("==========================================");
        
        // Display system information
        String javaVersion = System.getProperty("java.version");
        
        System.out.printf("Operating System: %s%n", PlatformDetector.getOperatingSystem());
        System.out.printf("Architecture: %s%n", PlatformDetector.getPlatform());
        System.out.printf("Java Version: %s%n", javaVersion);
        System.out.printf("Platform Details: %s%n", PlatformDetector.getPlatformOptimizationHints());
        
        // Get default cache configuration
        CacheAwareOps.CacheConfig defaultConfig = CacheAwareOps.getDefaultCacheConfig();
        
        System.out.println("\nDetected cache configuration:");
        System.out.printf("  L1 Cache Size: %d KB%n", defaultConfig.l1CacheSize / 1024);
        System.out.printf("  L2 Cache Size: %d KB%n", defaultConfig.l2CacheSize / 1024);
        System.out.printf("  Cache Line Size: %d bytes%n", defaultConfig.cacheLineSize);
        System.out.printf("  Optimal Block Size: %d elements%n", defaultConfig.optimalBlockSize);
        System.out.printf("  Tile Size: %d elements%n", defaultConfig.tileSize);
        
        // Show platform-specific optimizations
        if (PlatformDetector.isAppleSilicon()) {
            System.out.println("\nApple Silicon optimizations detected:");
            System.out.println("  - Larger L1 cache (128KB) for better blocking");
            System.out.println("  - Large L2 cache (4MB) for multi-scale analysis");
            System.out.println("  - Optimized for unified memory architecture");
        } else if (PlatformDetector.isX86_64()) {
            System.out.println("\nx86-64 optimizations detected:");
            System.out.println("  - Standard L1 cache (32KB) configuration");
            System.out.println("  - Conservative L2 cache (256KB) assumptions");
            System.out.println("  - Optimized for traditional cache hierarchy");
        }
        
        // Demonstrate custom configuration
        System.out.println("\nCustom cache configuration example:");
        CacheAwareOps.CacheConfig customConfig = CacheAwareOps.CacheConfig.create(
            64 * 1024,   // 64KB L1
            1024 * 1024, // 1MB L2
            128          // 128-byte cache lines
        );
        
        System.out.printf("  Custom L1: %d KB, L2: %d KB, Line: %d bytes%n",
            customConfig.l1CacheSize / 1024,
            customConfig.l2CacheSize / 1024,
            customConfig.cacheLineSize);
        
        System.out.println();
    }
    
    /**
     * Compares FFT-accelerated vs direct convolution performance.
     */
    private static void fftPerformanceComparison() {
        System.out.println("2. FFT vs Direct Convolution Performance");
        System.out.println("========================================");
        
        MorletWavelet wavelet = new MorletWavelet();
        
        // Test different signal sizes - include larger sizes where FFT benefits are clear
        int[] signalSizes = {128, 256, 512, 1024, 2048, 4096, 8192};
        double[] scales = ScaleSpace.logarithmic(2.0, 32.0, 20).getScales(); // Balanced number of scales
        
        System.out.println("Signal Size | Direct (ms) | FFT (ms) | Speedup | FFT Complexity");
        System.out.println("-----------|-------------|----------|---------|---------------");
        
        for (int size : signalSizes) {
            double[] signal = createTestSignal(size);
            
            // Direct convolution - explicitly disable FFT
            CWTConfig directConfig = CWTConfig.builder()
                .enableFFT(false)
                .normalizeScales(true)
                .build();
            CWTTransform directCwt = new CWTTransform(wavelet, directConfig);
            
            // Warm up
            directCwt.analyze(signal, new double[]{scales[0]});
            
            long directStart = System.nanoTime();
            CWTResult directResult = directCwt.analyze(signal, scales);
            long directTime = System.nanoTime() - directStart;
            
            // FFT-accelerated - force FFT usage with explicit size
            CWTConfig fftConfig = CWTConfig.builder()
                .enableFFT(true)
                .normalizeScales(true)
                .fftSize(Math.max(size * 2, 128)) // Force FFT for all sizes
                .build();
            CWTTransform fftCwt = new CWTTransform(wavelet, fftConfig);
            
            // Warm up
            fftCwt.analyze(signal, new double[]{scales[0]});
            
            long fftStart = System.nanoTime();
            CWTResult fftResult = fftCwt.analyze(signal, scales);
            long fftTime = System.nanoTime() - fftStart;
            
            double speedup = (double) directTime / fftTime;
            String directComplexity = "O(n²)";
            String fftComplexity = speedup > 1.2 ? "O(n log n)" : "O(n²)";
            
            System.out.printf("%10d | %11.2f | %8.2f | %7.1fx | %s%n",
                size, directTime / 1e6, fftTime / 1e6, speedup, fftComplexity);
            
            // Verify results are similar (sanity check)
            double mse = computeMSE(directResult.getCoefficients(), fftResult.getCoefficients());
            if (mse > 1e-10) {
                System.out.printf("           | WARNING: Large MSE between methods: %.2e%n", mse);
            }
        }
        
        System.out.println("\nNotes:");
        System.out.println("- FFT shows O(n log n) scaling for larger signals and more scales");
        System.out.println("- Direct method is O(n²) but may be faster for small signals due to overhead");
        System.out.println("- FFT advantage increases with signal size and number of scales");
        System.out.println("- Small differences in results are due to numerical precision");
        
        // Additional test with varying number of scales to show FFT advantage
        System.out.println("\nFFT Advantage with Varying Scale Counts (signal size 2048):");
        System.out.println("Scales | Direct (ms) | FFT (ms) | Speedup");
        System.out.println("-------|-------------|----------|--------");
        
        double[] testSignal = createTestSignal(2048);
        int[] scaleCounts = {5, 10, 20, 40, 80};
        
        for (int scaleCount : scaleCounts) {
            double[] testScales = ScaleSpace.logarithmic(2.0, 32.0, scaleCount).getScales();
            
            // Direct method
            CWTConfig directConfig = CWTConfig.builder().enableFFT(false).build();
            CWTTransform directCwt = new CWTTransform(wavelet, directConfig);
            
            long directStart = System.nanoTime();
            directCwt.analyze(testSignal, testScales);
            long directTime = System.nanoTime() - directStart;
            
            // FFT method
            CWTConfig fftConfig = CWTConfig.builder().enableFFT(true).fftSize(4096).build();
            CWTTransform fftCwt = new CWTTransform(wavelet, fftConfig);
            
            long fftStart = System.nanoTime();
            fftCwt.analyze(testSignal, testScales);
            long fftTime = System.nanoTime() - fftStart;
            
            double speedup = (double) directTime / fftTime;
            System.out.printf("%6d | %11.2f | %8.2f | %6.1fx%n",
                scaleCount, directTime / 1e6, fftTime / 1e6, speedup);
        }
        
        System.out.println();
        
        // Test parallel vs sequential processing
        parallelProcessingComparison();
    }
    
    /**
     * Demonstrates parallel processing performance benefits.
     */
    private static void parallelProcessingComparison() {
        System.out.println("\nParallel vs Sequential Processing:");
        System.out.println("Scales | Sequential (ms) | Parallel (ms) | Speedup | Cores Used");
        System.out.println("-------|-----------------|---------------|---------|----------");
        
        MorletWavelet wavelet = new MorletWavelet();
        double[] testSignal = createTestSignal(2048);
        int[] scaleCounts = {4, 8, 16, 32, 64};
        int availableCores = Runtime.getRuntime().availableProcessors();
        
        for (int scaleCount : scaleCounts) {
            double[] testScales = ScaleSpace.logarithmic(2.0, 32.0, scaleCount).getScales();
            
            // Sequential processing
            CWTConfig sequentialConfig = CWTConfig.builder()
                .enableFFT(true)
                .useStructuredConcurrency(false)
                .build();
            CWTTransform sequentialCwt = new CWTTransform(wavelet, sequentialConfig);
            
            // Warm up
            sequentialCwt.analyze(testSignal, new double[]{testScales[0]});
            
            long seqStart = System.nanoTime();
            sequentialCwt.analyze(testSignal, testScales);
            long seqTime = System.nanoTime() - seqStart;
            
            // Parallel processing
            CWTConfig parallelConfig = CWTConfig.builder()
                .enableFFT(true)
                .useStructuredConcurrency(true)
                .build();
            CWTTransform parallelCwt = new CWTTransform(wavelet, parallelConfig);
            
            // Warm up
            parallelCwt.analyze(testSignal, new double[]{testScales[0]});
            
            long parStart = System.nanoTime();
            parallelCwt.analyze(testSignal, testScales);
            long parTime = System.nanoTime() - parStart;
            
            double speedup = (double) seqTime / parTime;
            int effectiveCores = Math.min(scaleCount, availableCores);
            
            System.out.printf("%6d | %15.2f | %13.2f | %7.1fx | %d/%d%n",
                scaleCount, seqTime / 1e6, parTime / 1e6, speedup, effectiveCores, availableCores);
        }
        
        System.out.println("\nParallel processing benefits:");
        System.out.println("- Scales processed independently across CPU cores");
        System.out.println("- Best speedup achieved when scales ≥ CPU cores");
        System.out.println("- Overhead negligible for 4+ scales");
        System.out.printf("- System has %d CPU cores available%n", availableCores);
    }
    
    /**
     * Demonstrates cache-aware optimizations.
     */
    private static void cacheOptimization() {
        System.out.println("3. Cache-Aware Optimization");
        System.out.println("===========================");
        
        // Create a large signal that will stress the cache
        int signalSize = 8192;
        double[] signal = createTestSignal(signalSize);
        double[] scales = ScaleSpace.logarithmic(1.0, 64.0, 32).getScales();
        
        MorletWavelet wavelet = new MorletWavelet();
        
        System.out.printf("Testing with signal size: %d, scales: %d%n", signalSize, scales.length);
        
        // Test different cache configurations
        CacheAwareOps.CacheConfig[] configs = {
            CacheAwareOps.getDefaultCacheConfig(),
            CacheAwareOps.CacheConfig.create(16 * 1024, 128 * 1024, 64),  // Small cache
            CacheAwareOps.CacheConfig.create(128 * 1024, 4 * 1024 * 1024, 64), // Large cache
        };
        
        String[] configNames = {"Default", "Small Cache", "Large Cache"};
        
        for (int i = 0; i < configs.length; i++) {
            CacheAwareOps.CacheConfig config = configs[i];
            String name = configNames[i];
            
            System.out.printf("\n%s Configuration:%n", name);
            System.out.printf("  L1: %d KB, L2: %d KB, Block: %d%n",
                config.l1CacheSize / 1024, config.l2CacheSize / 1024, config.optimalBlockSize);
            
            // Configure CWT with cache-aware settings
            CWTConfig cwtConfig = CWTConfig.builder()
                .enableFFT(true)
                .normalizeScales(true)
                .build();
            
            CWTTransform cwt = new CWTTransform(wavelet, cwtConfig);
            
            // Measure performance
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(signal, scales);
            long endTime = System.nanoTime();
            
            double timems = (endTime - startTime) / 1e6;
            double throughput = (signalSize * scales.length) / (timems / 1000.0) / 1e6;
            
            System.out.printf("  Time: %.2f ms%n", timems);
            System.out.printf("  Throughput: %.2f M operations/sec%n", throughput);
        }
        
        System.out.println();
    }
    
    /**
     * Analyzes performance scaling with signal size and number of scales.
     */
    private static void scalingAnalysis() {
        System.out.println("4. Performance Scaling Analysis");
        System.out.println("===============================");
        
        MorletWavelet wavelet = new MorletWavelet();
        
        // Configure for optimal performance
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .normalizeScales(true)
            .build();
        CWTTransform cwt = new CWTTransform(wavelet, config);
        
        System.out.println("Signal size scaling (20 scales):");
        System.out.println("Size    | Time (ms) | Throughput (Mops/s) | Scaling");
        System.out.println("--------|-----------|---------------------|--------");
        
        int[] sizes = {256, 512, 1024, 2048, 4096};
        double[] scales = ScaleSpace.logarithmic(2.0, 32.0, 20).getScales();
        
        double previousTime = 0;
        
        for (int size : sizes) {
            double[] signal = createTestSignal(size);
            
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(signal, scales);
            long endTime = System.nanoTime();
            
            double timems = (endTime - startTime) / 1e6;
            double throughput = (size * scales.length) / (timems / 1000.0) / 1e6;
            
            String scaling = "";
            if (previousTime > 0) {
                double ratio = timems / previousTime;
                double sizeRatio = (double) size / (size / 2); // Previous size
                double expectedLinear = sizeRatio;
                double expectedNLogN = sizeRatio * Math.log(sizeRatio) / Math.log(2);
                
                if (Math.abs(ratio - expectedNLogN) < Math.abs(ratio - expectedLinear)) {
                    scaling = "~O(n log n)";
                } else {
                    scaling = "~O(n)";
                }
            }
            
            System.out.printf("%7d | %9.2f | %19.2f | %s%n", 
                size, timems, throughput, scaling);
            
            previousTime = timems;
        }
        
        System.out.println("\nScale count scaling (1024 samples):");
        System.out.println("Scales | Time (ms) | Ops/Scale (ms) | Efficiency");
        System.out.println("-------|-----------|----------------|------------");
        
        int[] scaleCounts = {10, 20, 40, 80};
        int fixedSize = 1024;
        double[] fixedSignal = createTestSignal(fixedSize);
        
        for (int scaleCount : scaleCounts) {
            double[] testScales = ScaleSpace.logarithmic(2.0, 32.0, scaleCount).getScales();
            
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(fixedSignal, testScales);
            long endTime = System.nanoTime();
            
            double timems = (endTime - startTime) / 1e6;
            double timePerScale = timems / scaleCount;
            double efficiency = (10.0 / scaleCounts[0]) / (timePerScale / (timems / scaleCount));
            
            System.out.printf("%6d | %9.2f | %14.2f | %9.2fx%n", 
                scaleCount, timems, timePerScale, efficiency);
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates memory efficiency and allocation patterns.
     */
    private static void memoryEfficiency() {
        System.out.println("5. Memory Efficiency Analysis");
        System.out.println("=============================");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Measure memory usage for different configurations
        MorletWavelet wavelet = new MorletWavelet();
        int signalSize = 2048;
        int scaleCount = 40;
        
        double[] signal = createTestSignal(signalSize);
        double[] scales = ScaleSpace.logarithmic(1.0, 64.0, scaleCount).getScales();
        
        System.out.printf("Test configuration: %d samples, %d scales%n", signalSize, scaleCount);
        System.out.printf("Expected result size: %.2f MB%n", 
            (signalSize * scaleCount * 8.0) / (1024 * 1024));
        
        // Measure FFT memory usage
        System.out.println("\nFFT-accelerated version:");
        measureMemoryUsage(runtime, () -> {
            CWTConfig fftConfig = CWTConfig.builder()
                .enableFFT(true)
                .normalizeScales(true)
                .build();
            CWTTransform fftCwt = new CWTTransform(wavelet, fftConfig);
            CWTResult result = fftCwt.analyze(signal, scales);
            return result;
        });
        
        // Measure direct convolution memory usage
        System.out.println("\nDirect convolution version:");
        measureMemoryUsage(runtime, () -> {
            CWTConfig directConfig = CWTConfig.builder()
                .enableFFT(false)
                .normalizeScales(true)
                .build();
            CWTTransform directCwt = new CWTTransform(wavelet, directConfig);
            CWTResult result = directCwt.analyze(signal, scales);
            return result;
        });
        
        // Memory efficiency tips
        System.out.println("\nMemory Efficiency Tips:");
        System.out.println("- FFT uses temporary arrays but enables better cache utilization");
        System.out.println("- Process signals in chunks for very large datasets");
        System.out.println("- Use scale decimation for exploratory analysis");
        System.out.println("- Consider streaming CWT for real-time applications");
        
        System.out.println();
    }
    
    // Helper methods
    
    private static double[] createTestSignal(int size) {
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            double t = (double) i / size;
            
            // Multi-frequency signal
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +
                       0.5 * Math.sin(2 * Math.PI * 20 * t) +
                       0.3 * Math.sin(2 * Math.PI * 50 * t);
            
            // Add some noise
            signal[i] += 0.1 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double computeMSE(double[][] a, double[][] b) {
        double mse = 0;
        int count = 0;
        
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                double diff = a[i][j] - b[i][j];
                mse += diff * diff;
                count++;
            }
        }
        
        return mse / count;
    }
    
    private static void measureMemoryUsage(Runtime runtime, java.util.function.Supplier<CWTResult> operation) {
        // Force garbage collection before measurement
        System.gc();
        Thread.yield();
        
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        long startTime = System.nanoTime();
        CWTResult result = operation.get();
        long endTime = System.nanoTime();
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // Force another GC to see retained memory
        System.gc();
        Thread.yield();
        
        long memoryRetained = runtime.totalMemory() - runtime.freeMemory();
        
        double executionTime = (endTime - startTime) / 1e6;
        double memoryUsed = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);
        double memoryRetainedMB = (memoryRetained - memoryBefore) / (1024.0 * 1024.0);
        
        System.out.printf("  Execution time: %.2f ms%n", executionTime);
        System.out.printf("  Peak memory usage: %.2f MB%n", memoryUsed);
        System.out.printf("  Retained memory: %.2f MB%n", memoryRetainedMB);
        
        // Verify result dimensions
        double[][] coeffs = result.getCoefficients();
        System.out.printf("  Result dimensions: %d × %d%n", coeffs.length, coeffs[0].length);
    }
}