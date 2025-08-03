package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.memory.MemoryPool;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates memory-efficient usage patterns for the VectorWave library.
 *
 * <p>This demo covers:
 * <ul>
 *   <li>Memory pooling strategies</li>
 *   <li>In-place transformations</li>
 *   <li>Streaming with minimal footprint</li>
 *   <li>GC-friendly patterns</li>
 *   <li>Large dataset handling</li>
 * </ul>
 */
public class MemoryEfficiencyDemo {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /* TODO: This demo needs to be migrated to MODWT.
     * The demo uses DWT-specific features that need careful adaptation:
     * - Factory patterns (MODWT uses direct instantiation)
     * - FFM features (needs MODWT-specific FFM implementation)
     * - Streaming features (needs MODWT streaming implementation)
     * Temporarily disabled to allow compilation.
     */
    public static void main_disabled(String[] args) {
        System.out.println("This demo is temporarily disabled during DWT to MODWT migration.");
        System.out.println("Please check back later or contribute to the migration effort!");
    }
    
    public static void main_original(String[] args) throws Exception {
        System.out.println("=== VectorWave Memory Efficiency Demo ===\n");

        // Demo 1: Memory pooling benefits
        demonstrateMemoryPooling();

        // Demo 2: In-place transformations
        demonstrateInPlaceTransforms();

        // Demo 3: Streaming with minimal memory
        demonstrateStreamingMemoryUsage();

        // Demo 4: GC-friendly patterns
        demonstrateGCFriendlyPatterns();

        // Demo 5: Large dataset strategies
        demonstrateLargeDatasetHandling();
    }

    private static void demonstrateMemoryPooling() {
        System.out.println("1. Memory Pooling Benefits");
        System.out.println("--------------------------");

        int iterations = 10000;
        int signalSize = 1024;

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

    private static void demonstrateInPlaceTransforms() {
        System.out.println("2. In-Place Transformations");
        System.out.println("---------------------------");

        int signalSize = 4096;
        double[] signal = generateTestSignal(signalSize);

        // Traditional approach - creates new arrays
        System.gc();
        sleep(100);
        long startMem = getUsedHeap();

        WaveletTransform transform = new WaveletTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        List<TransformResult> results = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            double[] copy = signal.clone(); // Need copy to preserve original
            results.add(transform.forward(copy));
        }

        long traditionalMem = getUsedHeap() - startMem;

        // In-place approach with reused buffers
        System.gc();
        sleep(100);
        startMem = getUsedHeap();

        // Pre-allocate workspace
        double[] workspace = new double[signalSize];
        double[] approxBuffer = new double[signalSize / 2];
        double[] detailBuffer = new double[signalSize / 2];

        for (int i = 0; i < 100; i++) {
            // Copy to workspace for in-place operation
            System.arraycopy(signal, 0, workspace, 0, signalSize);

            // Perform transform using pre-allocated buffers
            performInPlaceTransform(workspace, approxBuffer, detailBuffer, transform);
        }

        long inPlaceMem = getUsedHeap() - startMem;

        System.out.printf("Traditional approach: %.2f MB allocated\n",
                traditionalMem / 1048576.0);
        System.out.printf("In-place approach:    %.2f MB allocated\n",
                inPlaceMem / 1048576.0);
        System.out.printf("Memory reduction: %.1f%%\n\n",
                100.0 * (traditionalMem - inPlaceMem) / traditionalMem);
    }

    private static void demonstrateStreamingMemoryUsage() throws Exception {
        System.out.println("3. Streaming with Minimal Memory");
        System.out.println("---------------------------------");

        // Simulate large data stream
        int totalSamples = 1_000_000;
        int blockSize = 1024;

        System.gc();
        sleep(100);
        long startMem = getUsedHeap();

        // Create streaming transform with memory constraints
        StreamingWaveletTransform streamTransform = StreamingWaveletTransform.create(
                new Haar(), BoundaryMode.PERIODIC, blockSize);

        // Memory-efficient subscriber
        AtomicLong processedBlocks = new AtomicLong();
        AtomicDouble totalEnergy = new AtomicDouble();

        streamTransform.subscribe(new Flow.Subscriber<TransformResult>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1); // Request one at a time
            }

            @Override
            public void onNext(TransformResult result) {
                // Process immediately and discard
                double energy = 0;
                for (double c : result.approximationCoeffs()) energy += c * c;
                for (double c : result.detailCoeffs()) energy += c * c;

                totalEnergy.addAndGet(energy);
                processedBlocks.incrementAndGet();

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
        for (int i = 0; i < totalSamples; i++) {
            streamTransform.process(rng.nextGaussian());
        }

        streamTransform.close();
        sleep(100); // Wait for processing

        long streamMem = getUsedHeap() - startMem;

        System.out.printf("Processed %d samples in %d blocks\n",
                totalSamples, processedBlocks.get());
        System.out.printf("Memory used: %.2f MB (%.2f bytes/sample)\n",
                streamMem / 1048576.0, (double) streamMem / totalSamples);
        System.out.printf("Average energy per block: %.4f\n\n",
                totalEnergy.get() / processedBlocks.get());
    }

    private static void demonstrateGCFriendlyPatterns() {
        System.out.println("4. GC-Friendly Patterns");
        System.out.println("-----------------------");

        int iterations = 1000;
        int signalSize = 2048;

        // Bad pattern: Creating many short-lived objects
        System.out.println("Testing GC-unfriendly pattern...");
        List<Long> badGcTimes = measureGCPressure(() -> {
            for (int i = 0; i < iterations; i++) {
                // Creates multiple temporary arrays
                double[] signal = new double[signalSize];
                WaveletTransform transform = new WaveletTransform(
                        Daubechies.DB4, BoundaryMode.PERIODIC);
                TransformResult result = transform.forward(signal);
                // Result goes out of scope immediately
            }
        });

        // Good pattern: Reusing objects
        System.out.println("Testing GC-friendly pattern...");
        List<Long> goodGcTimes = measureGCPressure(() -> {
            // Reuse transform and buffers
            WaveletTransform transform = new WaveletTransform(
                    Daubechies.DB4, BoundaryMode.PERIODIC);
            double[] signal = new double[signalSize];

            for (int i = 0; i < iterations; i++) {
                // Reuse existing array
                Arrays.fill(signal, i * 0.001);
                TransformResult result = transform.forward(signal);
                // Process result immediately
                double sum = result.approximationCoeffs()[0] + result.detailCoeffs()[0];
            }
        });

        System.out.printf("GC-unfriendly: %d GC pauses, total %d ms\n",
                badGcTimes.size(), badGcTimes.stream().mapToLong(Long::longValue).sum());
        System.out.printf("GC-friendly:   %d GC pauses, total %d ms\n",
                goodGcTimes.size(), goodGcTimes.stream().mapToLong(Long::longValue).sum());

        System.out.println("\nBest practices:");
        System.out.println("✓ Reuse WaveletTransform instances");
        System.out.println("✓ Pre-allocate arrays for repeated operations");
        System.out.println("✓ Process results immediately");
        System.out.println("✓ Use memory pools for temporary buffers\n");
    }

    private static void demonstrateLargeDatasetHandling() throws Exception {
        System.out.println("5. Large Dataset Strategies");
        System.out.println("---------------------------");

        // Simulate processing a very large dataset
        int numChannels = 100;
        int samplesPerChannel = 10_000;
        int blockSize = 1024;

        System.out.println("Processing 100 channels × 10,000 samples = 1M total samples");
        System.out.println("Using block processing to limit memory usage...\n");

        // Strategy 1: Process one channel at a time
        System.gc();
        sleep(100);
        long startMem = getUsedHeap();
        long startTime = System.currentTimeMillis();

        WaveletTransform transform = new WaveletTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);
        MemoryPool pool = new MemoryPool();

        // Process channels sequentially
        for (int channel = 0; channel < numChannels; channel++) {
            processLargeChannel(samplesPerChannel, blockSize, transform, pool);
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
                    WaveletTransform localTransform = new WaveletTransform(
                            Daubechies.DB4, BoundaryMode.PERIODIC);
                    return processLargeChannelParallel(samplesPerChannel, blockSize,
                            localTransform);
                } finally {
                    memoryPermits.release();
                }
            }));
        }

        // Wait for completion
        double totalEnergy = 0;
        for (Future<Double> future : futures) {
            totalEnergy += future.get();
        }

        executor.shutdown();
        long parTime = System.currentTimeMillis() - startTime;
        long parMem = getUsedHeap() - startMem;

        System.out.println("Results:");
        System.out.printf("Sequential: %d ms, %.2f MB peak memory\n",
                seqTime, seqMem / 1048576.0);
        System.out.printf("Parallel:   %d ms, %.2f MB peak memory\n",
                parTime, parMem / 1048576.0);
        System.out.printf("Speedup: %.2fx with %.1f%% memory increase\n",
                (double) seqTime / parTime,
                100.0 * (parMem - seqMem) / seqMem);

        System.out.println("\nMemory-efficient strategies for large datasets:");
        System.out.println("✓ Process in blocks rather than loading entire dataset");
        System.out.println("✓ Use streaming transforms for continuous data");
        System.out.println("✓ Limit parallelism to control memory usage");
        System.out.println("✓ Release resources as soon as possible");
        System.out.println("✓ Consider memory-mapped files for huge datasets");
    }

    // Helper methods

    private static long getUsedHeap() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32);
        }
        return signal;
    }

    private static void performInPlaceTransform(double[] workspace,
                                                double[] approxBuffer,
                                                double[] detailBuffer,
                                                WaveletTransform transform) {
        // Simulate in-place transform operation
        TransformResult result = transform.forward(workspace);
        System.arraycopy(result.approximationCoeffs(), 0, approxBuffer, 0,
                result.approximationCoeffs().length);
        System.arraycopy(result.detailCoeffs(), 0, detailBuffer, 0,
                result.detailCoeffs().length);
    }

    private static void processLargeChannel(int samples, int blockSize,
                                            WaveletTransform transform,
                                            MemoryPool pool) {
        // Process channel in blocks
        for (int start = 0; start < samples; start += blockSize) {
            int size = Math.min(blockSize, samples - start);
            double[] block = pool.borrowArray(size);
            try {
                // Simulate data loading
                for (int i = 0; i < size; i++) {
                    block[i] = Math.random();
                }

                // Process block
                if (size == blockSize) { // Only process full blocks
                    TransformResult result = transform.forward(block);
                    // Immediately discard result to save memory
                }
            } finally {
                pool.returnArray(block);
            }
        }
    }

    private static double processLargeChannelParallel(int samples, int blockSize,
                                                      WaveletTransform transform)
            throws InterruptedException {
        double totalEnergy = 0;

        for (int start = 0; start < samples; start += blockSize) {
            int size = Math.min(blockSize, samples - start);
            if (size == blockSize) {
                double[] block = new double[size];
                for (int i = 0; i < size; i++) {
                    block[i] = Math.random();
                }

                TransformResult result = transform.forward(block);

                // Calculate energy
                for (double c : result.approximationCoeffs()) totalEnergy += c * c;
                for (double c : result.detailCoeffs()) totalEnergy += c * c;
            }
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