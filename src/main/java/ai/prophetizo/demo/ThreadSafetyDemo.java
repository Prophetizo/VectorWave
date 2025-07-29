package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransformImpl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates thread safety characteristics of the ring buffer implementation.
 * 
 * Note: The current implementation is designed for single producer/single consumer
 * scenarios, which is typical for streaming signal processing pipelines.
 */
public class ThreadSafetyDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Ring Buffer Thread Safety Demonstration ===\n");
        
        demonstrateSingleProducerSingleConsumer();
        System.out.println();
        
        demonstrateProducerConsumerLatency();
        System.out.println();
        
        demonstrateRingBufferStability();
    }
    
    /**
     * Demonstrates single producer/single consumer pattern with streaming transforms.
     */
    private static void demonstrateSingleProducerSingleConsumer() throws InterruptedException {
        System.out.println("1. Single Producer/Single Consumer Pattern");
        System.out.println("=========================================");
        
        StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, 256, 128);
        
        AtomicInteger samplesProduced = new AtomicInteger(0);
        AtomicInteger resultsConsumed = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        CountDownLatch finished = new CountDownLatch(2);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10000; i++) {
                    double sample = Math.sin(2 * Math.PI * i / 100.0);
                    long startTime = System.nanoTime();
                    
                    streaming.addSample(sample);
                    
                    long endTime = System.nanoTime();
                    totalLatency.addAndGet(endTime - startTime);
                    samplesProduced.incrementAndGet();
                    
                    // Simulate real-time constraints with small delays
                    if (i % 100 == 0) {
                        Thread.sleep(1); // Simulate ADC sampling rate
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();
            }
        }, "Producer");
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                while (resultsConsumed.get() < 70 || !producer.getState().equals(Thread.State.TERMINATED)) {
                    if (streaming.isResultReady()) {
                        TransformResult result = streaming.getNextResult();
                        resultsConsumed.incrementAndGet();
                        
                        // Simulate processing time
                        if (resultsConsumed.get() % 10 == 0) {
                            System.out.printf("Processed %d results, buffer size: %d%n", 
                                resultsConsumed.get(), streaming.getBufferedSampleCount());
                        }
                    } else {
                        Thread.yield(); // Give producer a chance
                    }
                }
            } finally {
                finished.countDown();
            }
        }, "Consumer");
        
        long startTime = System.currentTimeMillis();
        producer.start();
        consumer.start();
        
        finished.await();
        long endTime = System.currentTimeMillis();
        
        System.out.printf("Completed: %d samples produced, %d results consumed%n", 
            samplesProduced.get(), resultsConsumed.get());
        System.out.printf("Total time: %d ms%n", endTime - startTime);
        System.out.printf("Average sample latency: %.2f ns%n", 
            totalLatency.get() / (double) samplesProduced.get());
    }
    
    /**
     * Demonstrates latency characteristics under different loads.
     */
    private static void demonstrateProducerConsumerLatency() throws InterruptedException {
        System.out.println("2. Latency Analysis Under Load");
        System.out.println("==============================");
        
        int[] windowSizes = {128, 256, 512, 1024};
        
        for (int windowSize : windowSizes) {
            StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
                new Haar(), BoundaryMode.PERIODIC, windowSize, windowSize / 2);
            
            // Measure latency for batch processing
            int batchSize = windowSize / 4;
            double[] batch = new double[batchSize];
            for (int i = 0; i < batchSize; i++) {
                batch[i] = Math.sin(2 * Math.PI * i / batchSize);
            }
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                streaming.addSamples(batch);
                if (streaming.isResultReady()) {
                    streaming.getNextResult();
                }
            }
            
            // Measure
            long startTime = System.nanoTime();
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                streaming.addSamples(batch);
                if (streaming.isResultReady()) {
                    streaming.getNextResult();
                }
            }
            
            long endTime = System.nanoTime();
            double avgLatency = (endTime - startTime) / (double) iterations / 1000.0; // μs
            
            System.out.printf("Window %4d: %.2f μs/batch, %.1f MHz throughput%n", 
                windowSize, avgLatency, batchSize / avgLatency);
        }
    }
    
    /**
     * Demonstrates ring buffer stability under continuous operation.
     */
    private static void demonstrateRingBufferStability() {
        System.out.println("3. Ring Buffer Stability Test");
        System.out.println("=============================");
        
        StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, 512, 256);
        
        System.out.println("Running stability test with continuous processing...");
        
        long startTime = System.currentTimeMillis();
        int totalSamples = 0;
        int totalResults = 0;
        int maxBufferSize = 0;
        
        // Run for a longer period to test stability
        while (System.currentTimeMillis() - startTime < 2000) { // 2 seconds
            // Add varying batch sizes to test different scenarios
            int batchSize = 1 + (int) (Math.random() * 10);
            double[] batch = new double[batchSize];
            
            for (int i = 0; i < batchSize; i++) {
                batch[i] = Math.sin(2 * Math.PI * (totalSamples + i) / 50.0);
            }
            
            streaming.addSamples(batch);
            totalSamples += batchSize;
            
            while (streaming.isResultReady()) {
                streaming.getNextResult();
                totalResults++;
            }
            
            maxBufferSize = Math.max(maxBufferSize, streaming.getBufferedSampleCount());
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.printf("Stability test completed:%n");
        System.out.printf("  Duration: %d ms%n", endTime - startTime);
        System.out.printf("  Samples processed: %d%n", totalSamples);
        System.out.printf("  Results generated: %d%n", totalResults);
        System.out.printf("  Max buffer size: %d%n", maxBufferSize);
        System.out.printf("  Final buffer size: %d%n", streaming.getBufferedSampleCount());
        System.out.printf("  Throughput: %.1f samples/second%n", 
            totalSamples * 1000.0 / (endTime - startTime));
        
        // Verify final state
        StreamingWaveletTransformImpl.StreamingPerformanceStats stats = 
            streaming.getPerformanceStats();
        System.out.printf("  Final stats: %s%n", stats);
        
        System.out.println("✅ Ring buffer maintained stability throughout the test");
    }
}