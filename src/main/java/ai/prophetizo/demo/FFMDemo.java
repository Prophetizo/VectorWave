package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.memory.ffm.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Random;

/**
 * Comprehensive demonstration of Foreign Function & Memory (FFM) API features.
 * Shows advanced memory management, zero-copy operations, and performance benefits.
 * 
 * Requires Java 23+ with --enable-native-access=ALL-UNNAMED flag.
 */
public class FFMDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave FFM (Foreign Function & Memory) API Demo");
        System.out.println("===================================================\n");
        
        // Demo 1: Basic FFM usage
        basicFFMUsage();
        
        // Demo 2: Memory pool management
        memoryPoolManagement();
        
        // Demo 3: Zero-copy processing
        zeroCopyProcessing();
        
        // Demo 4: Streaming with FFM
        streamingFFMDemo();
        
        // Demo 5: Performance comparison
        performanceComparison();
        
        // Demo 6: Advanced memory segment operations
        advancedMemorySegmentOps();
    }
    
    private static void basicFFMUsage() {
        System.out.println("1. Basic FFM Usage");
        System.out.println("------------------");
        
        double[] signal = generateSignal(1024);
        Wavelet wavelet = new Haar();
        
        // Drop-in replacement for WaveletTransform
        try (FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet)) {
            // Forward transform
            TransformResult result = ffm.forward(signal);
            System.out.println("Forward transform completed:");
            System.out.println("  Approximation coeffs: " + result.approximationCoeffs().length);
            System.out.println("  Detail coeffs: " + result.detailCoeffs().length);
            
            // Inverse transform
            double[] reconstructed = ffm.inverse(result);
            System.out.println("Inverse transform completed");
            
            // Verify reconstruction
            double error = calculateRMSE(signal, reconstructed);
            System.out.printf("Reconstruction RMSE: %.2e\n", error);
            
            // Combined forward-inverse (optimized path)
            double[] combined = ffm.forwardInverse(signal);
            double combinedError = calculateRMSE(signal, combined);
            System.out.printf("Combined transform RMSE: %.2e\n", combinedError);
        }
        
        System.out.println();
    }
    
    private static void memoryPoolManagement() {
        System.out.println("2. Memory Pool Management");
        System.out.println("-------------------------");
        
        // Create custom memory pool
        FFMMemoryPool pool = new FFMMemoryPool();
        
        try {
            // Create transform using the pool
            FFMWaveletTransform ffm = new FFMWaveletTransform(Daubechies.DB4, pool);
            
            // Pre-warm the pool
            pool.prewarm(64, 128, 256, 512, 1024);
            System.out.println("Pool pre-warmed with common sizes");
            
            // Perform multiple transforms
            Random random = new Random(42);
            for (int i = 0; i < 100; i++) {
                int size = 1 << (6 + random.nextInt(5)); // 64 to 1024
                double[] signal = generateSignal(size);
                
                TransformResult result = ffm.forward(signal);
                ffm.inverse(result);
            }
            
            // Show pool statistics
            FFMMemoryPool.PoolStatistics stats = pool.getStatistics();
            System.out.println("\nPool statistics after 100 operations:");
            System.out.println(stats.toDetailedString());
            
            // Show efficiency metrics
            System.out.printf("Memory efficiency: %.1f%%\n", 
                stats.hitRate() * 100);
            System.out.printf("Average allocation size: %.0f bytes\n", 
                (double)stats.totalBytesAllocated() / stats.totalAllocations());
            
            ffm.close();
        } finally {
            pool.close();
        }
        
        System.out.println();
    }
    
    private static void zeroCopyProcessing() {
        System.out.println("3. Zero-Copy Processing");
        System.out.println("-----------------------");
        
        // Create a large signal
        double[] largeSignal = generateSignal(8192);
        
        try (FFMWaveletTransform ffm = new FFMWaveletTransform(Symlet.SYM4)) {
            // Process slice without copying
            int offset = 2048;
            int length = 4096;
            
            System.out.printf("Processing slice [%d:%d] of %d-element array\n", 
                offset, offset + length, largeSignal.length);
            
            // Zero-copy transform on slice
            TransformResult sliceResult = ffm.forward(largeSignal, offset, length);
            System.out.println("Zero-copy transform completed");
            System.out.println("  No array copying required");
            System.out.println("  Direct memory segment access");
            
            // Process using memory segments directly
            MemorySegment signalSegment = MemorySegment.ofArray(largeSignal);
            TransformResult segmentResult = ffm.forwardSegment(signalSegment, largeSignal.length);
            System.out.println("\nDirect memory segment processing completed");
            
            // Compare results
            double[] slice1 = sliceResult.approximationCoeffs();
            double[] full1 = segmentResult.approximationCoeffs();
            System.out.printf("Results match: %s\n", 
                compareArrays(slice1, 0, full1, offset/2, length/2));
        }
        
        System.out.println();
    }
    
    private static void streamingFFMDemo() {
        System.out.println("4. Streaming with FFM");
        System.out.println("---------------------");
        
        int blockSize = 256;
        
        try (FFMStreamingTransform stream = new FFMStreamingTransform(new Haar(), blockSize)) {
            System.out.println("Streaming transform created:");
            System.out.println("  Block size: " + blockSize);
            System.out.println("  Zero-copy processing enabled");
            
            // Simulate real-time data arrival
            int totalSamples = 0;
            int blocksProcessed = 0;
            Random random = new Random(42);
            
            for (int batch = 0; batch < 10; batch++) {
                // Generate chunk of varying size
                int chunkSize = 50 + random.nextInt(100);
                double[] chunk = generateSignal(chunkSize);
                
                // Process chunk
                stream.processChunk(chunk, 0, chunkSize);
                totalSamples += chunkSize;
                
                // Check for complete blocks
                while (stream.hasCompleteBlock()) {
                    TransformResult result = stream.getNextResult();
                    blocksProcessed++;
                    
                    if (blocksProcessed == 1) {
                        System.out.println("\nFirst block result:");
                        System.out.println("  Approx energy: " + 
                            calculateEnergy(result.approximationCoeffs()));
                        System.out.println("  Detail energy: " + 
                            calculateEnergy(result.detailCoeffs()));
                    }
                }
            }
            
            System.out.printf("\nProcessed %d samples in %d blocks\n", 
                totalSamples, blocksProcessed);
            System.out.println("Remaining samples buffered: " + 
                (totalSamples - blocksProcessed * blockSize));
        }
        
        System.out.println();
    }
    
    private static void performanceComparison() {
        System.out.println("5. Performance Comparison");
        System.out.println("-------------------------");
        
        int[] sizes = {256, 512, 1024, 2048, 4096};
        Wavelet wavelet = Daubechies.DB4;
        int iterations = 100;
        
        System.out.println("Comparing traditional vs FFM implementation:");
        System.out.println("Wavelet: " + wavelet.name());
        System.out.println("Iterations: " + iterations);
        System.out.println();
        
        System.out.println("Size  | Traditional (ms) | FFM (ms) | Speedup | Pool Hit Rate");
        System.out.println("------|------------------|----------|---------|-------------");
        
        // Create shared memory pool
        FFMMemoryPool pool = new FFMMemoryPool();
        pool.prewarm(sizes);
        
        try {
            for (int size : sizes) {
                double[] signal = generateSignal(size);
                
                // Traditional timing
                WaveletTransform traditional = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
                long tradTime = timeTransform(traditional, signal, iterations);
                
                // FFM timing
                FFMWaveletTransform ffm = new FFMWaveletTransform(wavelet, pool);
                long ffmTime = timeFFMTransform(ffm, signal, iterations);
                
                // Get pool statistics
                FFMMemoryPool.PoolStatistics stats = pool.getStatistics();
                double hitRate = stats.hitRate();
                
                // Display results
                double speedup = (double)tradTime / ffmTime;
                System.out.printf("%5d | %16.2f | %8.2f | %7.2fx | %5.1f%%\n",
                    size, tradTime/1e6, ffmTime/1e6, speedup, hitRate * 100);
                
                ffm.close();
            }
        } finally {
            pool.close();
        }
        
        System.out.println("\nBenefits of FFM implementation:");
        System.out.println("  - Reduced GC pressure from off-heap memory");
        System.out.println("  - Better cache locality with SIMD alignment");
        System.out.println("  - Zero-copy operations on slices");
        System.out.println("  - Efficient memory pooling");
        
        System.out.println();
    }
    
    private static void advancedMemorySegmentOps() {
        System.out.println("6. Advanced Memory Segment Operations");
        System.out.println("-------------------------------------");
        
        try (FFMMemoryPool pool = new FFMMemoryPool()) {
            // Allocate SIMD-aligned memory
            int size = 1024;
            MemorySegment aligned = pool.acquire(size);
            
            System.out.println("Allocated SIMD-aligned memory:");
            System.out.println("  Size: " + size + " doubles");
            System.out.println("  Bytes: " + aligned.byteSize());
            System.out.println("  Address alignment: " + 
                (aligned.address() % 64));
            
            // Fill with test data
            for (int i = 0; i < size; i++) {
                aligned.setAtIndex(ValueLayout.JAVA_DOUBLE, i, 
                    Math.sin(2 * Math.PI * i / size));
            }
            
            // Create FFM transform with the pool
            FFMWaveletTransform ffm = new FFMWaveletTransform(new Haar(), pool);
            
            // Process the memory segment directly
            TransformResult result = ffm.forwardSegment(aligned, size);
            System.out.println("\nProcessed memory segment directly:");
            System.out.println("  No array allocation required");
            System.out.println("  Direct SIMD-aligned access");
            
            // Demonstrate scoped allocation
            FFMWaveletOps ops = new FFMWaveletOps(pool);
            double[] signal = generateSignal(512);
            double[] filters = {0.5, 0.5}; // Simple averaging filter
            
            double[] scopedResult = ops.forwardInverseScoped(
                signal, filters, filters, filters, filters);
            
            System.out.println("\nScoped allocation demo:");
            System.out.println("  All intermediate memory automatically freed");
            System.out.println("  No manual memory management required");
            
            @SuppressWarnings("resource")  // Explicit close for demonstration
            FFMWaveletOps ignoredOps = ops;
            ops.close();
            @SuppressWarnings("resource")  // Explicit close for demonstration
            FFMMemoryPool ignoredPool = pool;
            pool.close();
        }
        
        System.out.println("\nFFM API requirements:");
        System.out.println("  - Java 23 or later");
        System.out.println("  - JVM flag: --enable-native-access=ALL-UNNAMED");
        System.out.println("  - Automatic fallback to traditional implementation if unavailable");
    }
    
    // Helper methods
    
    private static double[] generateSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.sin(4 * Math.PI * i / 64) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private static double calculateRMSE(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double v : signal) {
            energy += v * v;
        }
        return energy;
    }
    
    private static boolean compareArrays(double[] a, int aOffset, double[] b, int bOffset, int length) {
        for (int i = 0; i < length; i++) {
            if (Math.abs(a[aOffset + i] - b[bOffset + i]) > 1e-10) {
                return false;
            }
        }
        return true;
    }
    
    private static long timeTransform(WaveletTransform transform, double[] signal, int iterations) {
        // Warmup
        for (int i = 0; i < 10; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        
        // Time
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        return System.nanoTime() - start;
    }
    
    private static long timeFFMTransform(FFMWaveletTransform transform, double[] signal, int iterations) {
        // Warmup
        for (int i = 0; i < 10; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        
        // Time
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            TransformResult result = transform.forward(signal);
            transform.inverse(result);
        }
        return System.nanoTime() - start;
    }
}