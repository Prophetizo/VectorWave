package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class DetailedStreamingBenchmark {
    
    // Adapter class for backward compatibility
    private static class StreamingDenoiser {
        static class Builder {
            private StreamingDenoiserConfig.Builder configBuilder = new StreamingDenoiserConfig.Builder();
            
            Builder wavelet(ai.prophetizo.wavelet.api.Wavelet wavelet) {
                configBuilder.wavelet(wavelet);
                return this;
            }
            
            Builder blockSize(int blockSize) {
                configBuilder.blockSize(blockSize);
                return this;
            }
            
            Builder overlapFactor(double overlapFactor) {
                configBuilder.overlapFactor(overlapFactor);
                return this;
            }
            
            Builder thresholdMethod(ThresholdMethod method) {
                configBuilder.thresholdMethod(method);
                return this;
            }
            
            Builder thresholdType(ThresholdType type) {
                configBuilder.thresholdType(type);
                return this;
            }
            
            Builder useSharedMemoryPool(boolean useShared) {
                configBuilder.useSharedMemoryPool(useShared);
                return this;
            }
            
            StreamingDenoiserStrategy build() {
                return StreamingDenoiserFactory.create(
                    StreamingDenoiserFactory.Implementation.FAST, configBuilder.build());
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        int signalSize = 128;
        int warmupRuns = 1000;
        int measurementRuns = 10000;
        
        System.out.println("=== Detailed Streaming Denoiser Performance Analysis ===");
        System.out.println("Signal size: " + signalSize + " samples");
        System.out.println();
        
        // Test individual components
        benchmarkNoiseEstimation(signalSize, warmupRuns, measurementRuns);
        benchmarkOverlapProcessing(signalSize, warmupRuns, measurementRuns);
        benchmarkMemoryAllocation(signalSize, warmupRuns, measurementRuns);
        benchmarkFlowOverhead(signalSize, warmupRuns, measurementRuns);
    }
    
    private static void benchmarkNoiseEstimation(int signalSize, int warmupRuns, int measurementRuns) {
        System.out.println("--- Noise Estimation Component ---");
        
        double[] signal = generateNoisySignal(signalSize);
        
        // Test MAD with buffering
        MADNoiseEstimator bufferedMAD = new MADNoiseEstimator(signalSize, 0.9);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            bufferedMAD.updateEstimate(signal);
        }
        
        // Measure buffered MAD
        long startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            bufferedMAD.updateEstimate(signal);
        }
        long bufferedTime = System.nanoTime() - startTime;
        
        // Test streaming MAD with P²
        StreamingMADNoiseEstimator streamingMAD = new StreamingMADNoiseEstimator();
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            streamingMAD.updateEstimate(signal);
        }
        
        // Measure streaming MAD
        startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            streamingMAD.updateEstimate(signal);
        }
        long streamingTime = System.nanoTime() - startTime;
        
        System.out.printf("  Buffered MAD: %.3f ms/update\n", bufferedTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  Streaming MAD (P²): %.3f ms/update\n", streamingTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  P² Overhead: %.1fx slower\n", (double) streamingTime / bufferedTime);
        System.out.println();
    }
    
    private static void benchmarkOverlapProcessing(int signalSize, int warmupRuns, int measurementRuns) {
        System.out.println("--- Overlap Processing Component ---");
        
        double[] signal = generateNoisySignal(signalSize);
        
        // Test with no overlap
        OverlapBuffer noOverlap = new OverlapBuffer(signalSize, 0.0, OverlapBuffer.WindowFunction.RECTANGULAR);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            noOverlap.process(signal);
        }
        
        // Measure no overlap
        long startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            noOverlap.process(signal);
        }
        long noOverlapTime = System.nanoTime() - startTime;
        
        // Test with 50% overlap
        OverlapBuffer withOverlap = new OverlapBuffer(signalSize, 0.5, OverlapBuffer.WindowFunction.HANN);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            withOverlap.process(signal);
        }
        
        // Measure with overlap
        startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            withOverlap.process(signal);
        }
        long withOverlapTime = System.nanoTime() - startTime;
        
        System.out.printf("  No overlap: %.3f ms/block\n", noOverlapTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  50%% overlap (Hann): %.3f ms/block\n", withOverlapTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  Overlap overhead: %.1fx\n", (double) withOverlapTime / noOverlapTime);
        System.out.println();
    }
    
    private static void benchmarkMemoryAllocation(int signalSize, int warmupRuns, int measurementRuns) 
            throws Exception {
        System.out.println("--- Memory Allocation Component ---");
        
        double[] signal = generateNoisySignal(signalSize);
        
        // Test with shared memory pool
        long startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
                .wavelet(new Haar())
                .blockSize(signalSize)
                .useSharedMemoryPool(true)
                .build();
            denoiser.close();
        }
        long sharedPoolTime = System.nanoTime() - startTime;
        
        // Test without shared memory pool
        startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
                .wavelet(new Haar())
                .blockSize(signalSize)
                .useSharedMemoryPool(false)
                .build();
            denoiser.close();
        }
        long noPoolTime = System.nanoTime() - startTime;
        
        System.out.printf("  Shared pool creation: %.3f ms/instance\n", 
            sharedPoolTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  No pool creation: %.3f ms/instance\n", 
            noPoolTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  Pool speedup: %.1fx\n", (double) noPoolTime / sharedPoolTime);
        System.out.println();
    }
    
    private static void benchmarkFlowOverhead(int signalSize, int warmupRuns, int measurementRuns) 
            throws Exception {
        System.out.println("--- Flow API Overhead ---");
        
        double[] signal = generateNoisySignal(signalSize);
        
        // Create a simple flow subscriber
        class SimpleSubscriber implements Flow.Subscriber<double[]> {
            int count = 0;
            Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                count++;
            }
            
            @Override
            public void onError(Throwable throwable) {}
            
            @Override
            public void onComplete() {}
        }
        
        // Test with Flow API
        StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
            .wavelet(new Haar())
            .blockSize(signalSize)
            .overlapFactor(0.0)
            .build();
        
        SimpleSubscriber subscriber = new SimpleSubscriber();
        denoiser.subscribe(subscriber);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            denoiser.process(signal);
        }
        
        // Reset
        subscriber.count = 0;
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < measurementRuns; i++) {
            denoiser.process(signal);
        }
        long flowTime = System.nanoTime() - startTime;
        
        denoiser.close();
        
        System.out.printf("  Flow API processing: %.3f ms/block\n", 
            flowTime / 1_000_000.0 / measurementRuns);
        System.out.printf("  Blocks processed: %d\n", subscriber.count);
        System.out.println();
    }
    
    private static double[] generateNoisySignal(int length) {
        Random random = new Random(42);
        double[] signal = new double[length];
        
        for (int i = 0; i < length; i++) {
            double t = i / (double) length;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) + 0.1 * random.nextGaussian();
        }
        
        return signal;
    }
}