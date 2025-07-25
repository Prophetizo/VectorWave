package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class StreamingDenoiserBenchmark {
    
    // Adapter class for backward compatibility
    private static class StreamingDenoiser {
        static class Builder {
            private StreamingDenoiserConfig.Builder configBuilder = new StreamingDenoiserConfig.Builder();
            
            Builder wavelet(Wavelet wavelet) {
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
        
        // Generate test signal with noise
        double[] signal = generateNoisySignal(signalSize);
        
        System.out.println("=== Streaming Denoiser Performance Benchmark ===");
        System.out.println("Signal size: " + signalSize + " samples");
        System.out.println("Warmup runs: " + warmupRuns);
        System.out.println("Measurement runs: " + measurementRuns);
        System.out.println();
        
        // Test with different wavelets
        Wavelet[] wavelets = {new Haar(), Daubechies.DB4};
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("--- Wavelet: " + wavelet.name() + " ---");
            
            // Benchmark streaming denoiser
            benchmarkStreamingDenoiser(wavelet, signal, warmupRuns, measurementRuns);
            
            // Benchmark traditional denoiser for comparison
            benchmarkTraditionalDenoiser(wavelet, signal, warmupRuns, measurementRuns);
            
            System.out.println();
        }
    }
    
    private static void benchmarkStreamingDenoiser(Wavelet wavelet, double[] signal, 
            int warmupRuns, int measurementRuns) throws Exception {
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            processWithStreamingDenoiser(wavelet, signal);
        }
        
        // Measure
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        
        for (int i = 0; i < measurementRuns; i++) {
            long startTime = System.nanoTime();
            double[] result = processWithStreamingDenoiser(wavelet, signal);
            long elapsed = System.nanoTime() - startTime;
            
            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }
        
        double avgTimeMs = totalTime / 1_000_000.0 / measurementRuns;
        double minTimeMs = minTime / 1_000_000.0;
        double maxTimeMs = maxTime / 1_000_000.0;
        double throughput = measurementRuns * signal.length / (totalTime / 1_000_000_000.0);
        
        System.out.printf("Streaming Denoiser:\n");
        System.out.printf("  Average time: %.3f ms\n", avgTimeMs);
        System.out.printf("  Min time: %.3f ms\n", minTimeMs);
        System.out.printf("  Max time: %.3f ms\n", maxTimeMs);
        System.out.printf("  Throughput: %.0f samples/sec\n", throughput);
    }
    
    private static void benchmarkTraditionalDenoiser(Wavelet wavelet, double[] signal,
            int warmupRuns, int measurementRuns) {
        
        WaveletDenoiser denoiser = new WaveletDenoiser(wavelet, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            denoiser.denoise(signal, ThresholdMethod.UNIVERSAL, ThresholdType.SOFT);
        }
        
        // Measure
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        
        for (int i = 0; i < measurementRuns; i++) {
            long startTime = System.nanoTime();
            double[] result = denoiser.denoise(signal, ThresholdMethod.UNIVERSAL, ThresholdType.SOFT);
            long elapsed = System.nanoTime() - startTime;
            
            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }
        
        double avgTimeMs = totalTime / 1_000_000.0 / measurementRuns;
        double minTimeMs = minTime / 1_000_000.0;
        double maxTimeMs = maxTime / 1_000_000.0;
        double throughput = measurementRuns * signal.length / (totalTime / 1_000_000_000.0);
        
        System.out.printf("Traditional Denoiser:\n");
        System.out.printf("  Average time: %.3f ms\n", avgTimeMs);
        System.out.printf("  Min time: %.3f ms\n", minTimeMs);
        System.out.printf("  Max time: %.3f ms\n", maxTimeMs);
        System.out.printf("  Throughput: %.0f samples/sec\n", throughput);
    }
    
    private static double[] processWithStreamingDenoiser(Wavelet wavelet, double[] signal) 
            throws Exception {
        
        StreamingDenoiserStrategy denoiser = new StreamingDenoiser.Builder()
            .wavelet(wavelet)
            .blockSize(128)  // Process entire signal as one block
            .overlapFactor(0.0)  // No overlap for this test
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .thresholdType(ThresholdType.SOFT)
            .useSharedMemoryPool(false)
            .build();
        
        // Collect results
        final double[] result = new double[signal.length];
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] received = {0};
        
        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                System.arraycopy(item, 0, result, received[0], item.length);
                received[0] += item.length;
            }
            
            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        // Process signal
        denoiser.process(signal);
        denoiser.close();
        
        latch.await();
        return result;
    }
    
    private static double[] generateNoisySignal(int length) {
        Random random = new Random(42);
        double[] signal = new double[length];
        
        // Generate signal: sine wave + noise
        for (int i = 0; i < length; i++) {
            double t = i / (double) length;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) + 0.1 * random.nextGaussian();
        }
        
        return signal;
    }
}