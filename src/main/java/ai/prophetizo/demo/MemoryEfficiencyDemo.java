package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.modwt.streaming.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.memory.MemoryPool;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates memory-efficient usage patterns for MODWT in the VectorWave library.
 *
 * <p>This demo covers:
 * <ul>
 *   <li>Memory pooling strategies with MODWT</li>
 *   <li>Efficient coefficient handling</li>
 *   <li>Streaming with minimal footprint</li>
 *   <li>GC-friendly patterns</li>
 *   <li>Large dataset handling with arbitrary lengths</li>
 * </ul>
 * 
 */
public class MemoryEfficiencyDemo {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    public static void main(String[] args) throws Exception {
        System.out.println("=== VectorWave MODWT Memory Efficiency Demo ===\n");

        // Demo 1: Memory pooling benefits
        demonstrateMemoryPooling();

        // Demo 2: MODWT memory advantages
        demonstrateMODWTMemoryAdvantages();

        // Demo 3: Streaming with minimal memory
        demonstrateStreamingMemoryUsage();

        // Demo 4: GC-friendly patterns
        demonstrateGCFriendlyPatterns();

        // Demo 5: Large dataset strategies
        demonstrateLargeDatasetHandling();
    }

    private static void demonstrateMemoryPooling() {
        System.out.println("1. Memory Pooling Benefits with MODWT");
        System.out.println("--------------------------------------");

        int iterations = 10000;
        int signalSize = 1777; // Non-power-of-2 to show MODWT advantage

        // Without pooling
        System.gc();
        sleep(100);
        long startHeap = getUsedHeap();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            double[] signal = new double[signalSize];
            double[] workspace = new double[signalSize];
            // Simulate transform work
            System.arraycopy(signal, 0, workspace, 0, signalSize);
        }

        long timeNoPool = System.currentTimeMillis() - startTime;
        long heapNoPool = getUsedHeap() - startHeap;

        // With pooling
        System.gc();
        sleep(100);
        startHeap = getUsedHeap();
        startTime = System.currentTimeMillis();

        MemoryPool pool = new MemoryPool();
        pool.setMaxArraysPerSize(10); // Limit pool size

        for (int i = 0; i < iterations; i++) {
            double[] signal = pool.borrowArray(signalSize);
            double[] workspace = pool.borrowArray(signalSize);
            try {
                // Simulate transform work
                System.arraycopy(signal, 0, workspace, 0, signalSize);
            } finally {
                pool.returnArray(signal);
                pool.returnArray(workspace);
            }
        }

        long timeWithPool = System.currentTimeMillis() - startTime;
        long heapWithPool = getUsedHeap() - startHeap;

        System.out.printf("Without pooling: %d ms, %.2f MB heap growth\n",
                timeNoPool, heapNoPool / 1048576.0);
        System.out.printf("With pooling:    %d ms, %.2f MB heap growth\n",
                timeWithPool, heapWithPool / 1048576.0);
        System.out.printf("Memory saved: %.1f%%\n\n",
                100.0 * (heapNoPool - heapWithPool) / heapNoPool);

        // Show pool statistics
        System.out.println("Pool statistics:");
        pool.printStatistics();
        System.out.println();
    }

    private static void demonstrateMODWTMemoryAdvantages() {
        System.out.println("2. MODWT Memory Advantages");
        System.out.println("--------------------------");

        // Show memory savings for non-power-of-2 signals
        int[] signalSizes = {777, 1234, 3333, 5678, 9999};
        
        System.out.println("Memory comparison: MODWT vs DWT padding requirements");
        System.out.println("Signal Size | DWT Padded | Memory Waste | MODWT Advantage");
        System.out.println("------------|------------|--------------|----------------");
        
        long totalDWTMemory = 0;
        long totalMODWTMemory = 0;
        
        for (int size : signalSizes) {
            int dwtSize = nextPowerOfTwo(size);
            int padding = dwtSize - size;
            double wastePercent = 100.0 * padding / dwtSize;
            
            // Memory for coefficients (assuming double precision)
            long modwtMem = (long)size * 2 * 8; // approx + detail, same length
            long dwtMem = (long)dwtSize * 2 * 8; // padded size
            
            totalMODWTMemory += modwtMem;
            totalDWTMemory += dwtMem;
            
            System.out.printf("%11d | %10d | %11.1f%% | %d bytes saved\n",
                    size, dwtSize, wastePercent, dwtMem - modwtMem);
        }
        
        System.out.printf("\nTotal memory: MODWT=%.2f KB, DWT=%.2f KB (%.1f%% savings)\n\n",
                totalMODWTMemory / 1024.0, totalDWTMemory / 1024.0,
                100.0 * (totalDWTMemory - totalMODWTMemory) / totalDWTMemory);

        // Demonstrate processing
        System.out.println("Processing demonstration:");
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        for (int size : new int[]{100, 777, 1234}) {
            double[] signal = generateTestSignal(size);
            
            System.gc();
            sleep(50);
            long startMem = getUsedHeap();
            
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            long usedMem = getUsedHeap() - startMem;
            
            double error = 0;
            for (int i = 0; i < size; i++) {
                error = Math.max(error, Math.abs(signal[i] - reconstructed[i]));
            }
            
            System.out.printf("  Size %4d: Memory=%.1f KB, Error=%.2e\n",
                    size, usedMem / 1024.0, error);
        }
        System.out.println();
    }

    private static void demonstrateStreamingMemoryUsage() throws Exception {
        System.out.println("3. Streaming with Minimal Memory");
        System.out.println("---------------------------------");

        // Use MODWT streaming denoiser for memory-efficient processing
        int totalSamples = 100_000;
        int bufferSize = 256; // Can be any size with MODWT!

        System.gc();
        sleep(100);
        long startMem = getUsedHeap();

        // Create MODWT streaming denoiser
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(bufferSize)
            .build();

        // Memory-efficient subscriber
        AtomicLong processedSamples = new AtomicLong();
        AtomicDouble totalEnergy = new AtomicDouble();

        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1); // Request one at a time
            }

            @Override
            public void onNext(double[] denoisedBlock) {
                // Process immediately and discard
                double energy = 0;
                for (double v : denoisedBlock) energy += v * v;

                totalEnergy.addAndGet(energy);
                processedSamples.addAndGet(denoisedBlock.length);

                // Request next block
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Stream error: " + throwable);
            }

            @Override
            public void onComplete() {
            }
        });

        // Stream data without keeping it in memory
        Random rng = new Random(42);
        double[] chunk = new double[bufferSize];
        
        for (int i = 0; i < totalSamples; i += bufferSize) {
            int size = Math.min(bufferSize, totalSamples - i);
            for (int j = 0; j < size; j++) {
                chunk[j] = rng.nextGaussian() * 0.1 + Math.sin(2 * Math.PI * (i + j) / 100);
            }
            
            if (size < bufferSize) {
                // Handle last chunk which might be smaller
                double[] lastChunk = Arrays.copyOf(chunk, size);
                denoiser.denoise(lastChunk);
            } else {
                denoiser.denoise(chunk);
            }
        }

        denoiser.close();
        sleep(100); // Wait for processing

        long streamMem = getUsedHeap() - startMem;

        System.out.printf("Processed %d samples\n", processedSamples.get());
        System.out.printf("Memory used: %.2f MB (%.2f bytes/sample)\n",
                streamMem / 1048576.0, (double) streamMem / totalSamples);
        System.out.printf("Total energy: %.4f\n", totalEnergy.get());
        System.out.printf("Estimated noise level: %.4f\n\n", denoiser.getEstimatedNoiseLevel());
    }

    private static void demonstrateGCFriendlyPatterns() {
        System.out.println("4. GC-Friendly Patterns with MODWT");
        System.out.println("-----------------------------------");

        int iterations = 1000;
        int signalSize = 2345; // Non-power-of-2

        // Bad pattern: Creating many short-lived objects
        System.out.println("Testing GC-unfriendly pattern...");
        List<Long> badGcTimes = measureGCPressure(() -> {
            for (int i = 0; i < iterations; i++) {
                // Creates multiple temporary arrays
                double[] signal = new double[signalSize];
                MODWTTransform transform = new MODWTTransform(
                        Daubechies.DB4, BoundaryMode.PERIODIC);
                MODWTResult result = transform.forward(signal);
                // Result goes out of scope immediately
            }
        });

        // Good pattern: Reusing objects
        System.out.println("Testing GC-friendly pattern...");
        List<Long> goodGcTimes = measureGCPressure(() -> {
            // Reuse transform and buffers
            MODWTTransform transform = new MODWTTransform(
                    Daubechies.DB4, BoundaryMode.PERIODIC);
            double[] signal = new double[signalSize];

            for (int i = 0; i < iterations; i++) {
                // Reuse existing array
                Arrays.fill(signal, i * 0.001);
                MODWTResult result = transform.forward(signal);
                // Process result immediately
                double sum = result.approximationCoeffs()[0] + result.detailCoeffs()[0];
            }
        });

        System.out.printf("GC-unfriendly: %d GC pauses, total %d ms\n",
                badGcTimes.size(), badGcTimes.stream().mapToLong(Long::longValue).sum());
        System.out.printf("GC-friendly:   %d GC pauses, total %d ms\n",
                goodGcTimes.size(), goodGcTimes.stream().mapToLong(Long::longValue).sum());

        System.out.println("\nBest practices with MODWT:");
        System.out.println("✓ Reuse MODWTTransform instances");
        System.out.println("✓ Pre-allocate arrays for repeated operations");
        System.out.println("✓ Process results immediately");
        System.out.println("✓ Use streaming denoiser for continuous data");
        System.out.println("✓ MODWT works with any size - no padding overhead!\n");
    }

    private static void demonstrateLargeDatasetHandling() throws Exception {
        System.out.println("5. Large Dataset Strategies with MODWT");
        System.out.println("---------------------------------------");

        // Simulate processing a very large dataset
        int numChannels = 50;
        int samplesPerChannel = 7777; // Non-power-of-2
        int blockSize = 333; // Also non-power-of-2!

        System.out.printf("Processing %d channels × %d samples = %d total samples\n",
                numChannels, samplesPerChannel, numChannels * samplesPerChannel);
        System.out.println("Using MODWT's ability to handle arbitrary block sizes...\n");

        // Strategy 1: Process one channel at a time
        System.gc();
        sleep(100);
        long startMem = getUsedHeap();
        long startTime = System.currentTimeMillis();

        MODWTTransform transform = new MODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);
        MemoryPool pool = new MemoryPool();

        // Process channels sequentially
        double totalEnergySeq = 0;
        for (int channel = 0; channel < numChannels; channel++) {
            totalEnergySeq += processLargeChannelMODWT(
                    samplesPerChannel, blockSize, transform, pool);
        }

        long seqTime = System.currentTimeMillis() - startTime;
        long seqMem = getUsedHeap() - startMem;

        // Strategy 2: Parallel processing with memory constraints
        System.gc();
        sleep(100);
        startMem = getUsedHeap();
        startTime = System.currentTimeMillis();

        // Use limited thread pool to control memory usage
        int numThreads = Math.min(4, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Semaphore memoryPermits = new Semaphore(numThreads * 2); // Limit concurrent blocks

        List<Future<Double>> futures = new ArrayList<>();

        for (int channel = 0; channel < numChannels; channel++) {
            final int ch = channel;
            futures.add(executor.submit(() -> {
                memoryPermits.acquire();
                try {
                    MODWTTransform localTransform = new MODWTTransform(
                            Daubechies.DB4, BoundaryMode.PERIODIC);
                    return processLargeChannelParallelMODWT(
                            samplesPerChannel, blockSize, localTransform);
                } finally {
                    memoryPermits.release();
                }
            }));
        }

        // Wait for completion
        double totalEnergyPar = 0;
        for (Future<Double> future : futures) {
            totalEnergyPar += future.get();
        }

        executor.shutdown();
        long parTime = System.currentTimeMillis() - startTime;
        long parMem = getUsedHeap() - startMem;

        System.out.println("Results:");
        System.out.printf("Sequential: %d ms, %.2f MB peak memory, energy=%.2f\n",
                seqTime, seqMem / 1048576.0, totalEnergySeq);
        System.out.printf("Parallel:   %d ms, %.2f MB peak memory, energy=%.2f\n",
                parTime, parMem / 1048576.0, totalEnergyPar);
        System.out.printf("Speedup: %.2fx with %.1f%% memory increase\n",
                (double) seqTime / parTime,
                100.0 * (parMem - seqMem) / seqMem);

        System.out.println("\nMODWT advantages for large datasets:");
        System.out.println("✓ Process blocks of ANY size - no padding waste");
        System.out.println("✓ Shift-invariance allows flexible block boundaries");
        System.out.println("✓ Memory usage scales linearly with signal size");
        System.out.println("✓ Perfect for streaming and real-time applications");
        System.out.println("✓ No need to buffer to power-of-2 sizes");
    }

    // Helper methods

    private static long getUsedHeap() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        return signal;
    }

    private static double processLargeChannelMODWT(int samples, int blockSize,
                                                   MODWTTransform transform,
                                                   MemoryPool pool) {
        double totalEnergy = 0;
        
        // Process channel in blocks
        for (int start = 0; start < samples; start += blockSize) {
            int size = Math.min(blockSize, samples - start);
            double[] block = pool.borrowArray(size);
            try {
                // Simulate data loading
                for (int i = 0; i < size; i++) {
                    block[i] = Math.sin(2 * Math.PI * (start + i) / 100) + 
                              0.1 * Math.random();
                }

                // Process any size block with MODWT
                MODWTResult result = transform.forward(block);
                
                // Calculate energy
                for (double c : result.approximationCoeffs()) totalEnergy += c * c;
                for (double c : result.detailCoeffs()) totalEnergy += c * c;
                
            } finally {
                pool.returnArray(block);
            }
        }
        
        return totalEnergy;
    }

    private static double processLargeChannelParallelMODWT(int samples, int blockSize,
                                                          MODWTTransform transform)
            throws InterruptedException {
        double totalEnergy = 0;

        for (int start = 0; start < samples; start += blockSize) {
            int size = Math.min(blockSize, samples - start);
            double[] block = new double[size];
            
            for (int i = 0; i < size; i++) {
                block[i] = Math.sin(2 * Math.PI * (start + i) / 100) + 
                          0.1 * Math.random();
            }

            MODWTResult result = transform.forward(block);

            // Calculate energy
            for (double c : result.approximationCoeffs()) totalEnergy += c * c;
            for (double c : result.detailCoeffs()) totalEnergy += c * c;
        }

        return totalEnergy;
    }

    private static List<Long> measureGCPressure(Runnable task) {
        List<Long> gcTimes = new ArrayList<>();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // Record initial GC counts
        Map<String, Long> initialCounts = new HashMap<>();
        Map<String, Long> initialTimes = new HashMap<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            initialCounts.put(gc.getName(), gc.getCollectionCount());
            initialTimes.put(gc.getName(), gc.getCollectionTime());
        }

        // Run task
        task.run();

        // Record GC activity
        for (GarbageCollectorMXBean gc : gcBeans) {
            long countDiff = gc.getCollectionCount() - initialCounts.get(gc.getName());
            long timeDiff = gc.getCollectionTime() - initialTimes.get(gc.getName());

            if (countDiff > 0) {
                gcTimes.add(timeDiff);
            }
        }

        return gcTimes;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }

    // Simple atomic double for demo
    static class AtomicDouble {
        private volatile double value;

        public void addAndGet(double delta) {
            synchronized (this) {
                value += delta;
            }
        }

        public double get() {
            return value;
        }
    }
}