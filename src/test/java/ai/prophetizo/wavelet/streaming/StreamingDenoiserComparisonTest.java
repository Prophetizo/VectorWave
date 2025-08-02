package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparison tests between original and improved StreamingDenoiser implementations.
 * 
 * Measures:
 * - Quality improvements (SNR, MSE)
 * - Performance overhead
 * - Memory usage
 */
class StreamingDenoiserComparisonTest {
    
    private static final int SIGNAL_LENGTH = 4096;
    private static final double NOISE_LEVEL = 0.3;
    private static final int BLOCK_SIZE = 256;
    
    @Test
    void compareQualityMetrics() throws Exception {
        System.out.println("=== Quality Comparison: Fast vs Quality StreamingDenoiser ===\n");
        
        // Generate test signal
        double[] cleanSignal = generateTestSignal(SIGNAL_LENGTH);
        double[] noisySignal = addGaussianNoise(cleanSignal, NOISE_LEVEL, 42);
        
        // Calculate input SNR
        double inputSNR = calculateSNR(cleanSignal, noisySignal);
        double inputMSE = calculateMSE(cleanSignal, noisySignal);
        
        System.out.printf("Input Signal:\n");
        System.out.printf("  SNR: %.2f dB\n", inputSNR);
        System.out.printf("  MSE: %.6f\n\n", inputMSE);
        
        // Test different overlap factors
        double[] overlapFactors = {0.0, 0.5, 0.75};
        
        for (double overlap : overlapFactors) {
            System.out.printf("--- Overlap Factor: %.0f%% ---\n", overlap * 100);
            
            // Fast implementation
            double[] fastDenoised = processWithFast(noisySignal, overlap);
            double fastSNR = calculateSNR(cleanSignal, fastDenoised);
            double fastMSE = calculateMSE(cleanSignal, fastDenoised);
            
            // Quality implementation
            double[] qualityDenoised = processWithQuality(noisySignal, overlap);
            double qualitySNR = calculateSNR(cleanSignal, qualityDenoised);
            double qualityMSE = calculateMSE(cleanSignal, qualityDenoised);
            
            // Batch processing for reference
            WaveletDenoiser batchDenoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
            double[] batchDenoised = batchDenoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);
            double batchSNR = calculateSNR(cleanSignal, batchDenoised);
            double batchMSE = calculateMSE(cleanSignal, batchDenoised);
            
            // Print results
            System.out.printf("Fast StreamingDenoiser:\n");
            System.out.printf("  SNR: %.2f dB (change: %+.2f dB)\n", fastSNR, fastSNR - inputSNR);
            System.out.printf("  MSE: %.6f (change: %+.6f)\n", fastMSE, fastMSE - inputMSE);
            
            System.out.printf("Quality StreamingDenoiser:\n");
            System.out.printf("  SNR: %.2f dB (change: %+.2f dB)\n", qualitySNR, qualitySNR - inputSNR);
            System.out.printf("  MSE: %.6f (change: %+.6f)\n", qualityMSE, qualityMSE - inputMSE);
            
            System.out.printf("Batch WaveletDenoiser (reference):\n");
            System.out.printf("  SNR: %.2f dB (change: %+.2f dB)\n", batchSNR, batchSNR - inputSNR);
            System.out.printf("  MSE: %.6f (change: %+.6f)\n", batchMSE, batchMSE - inputMSE);
            
            System.out.printf("Quality improvement over Fast:\n");
            System.out.printf("  SNR gain: %+.2f dB\n", qualitySNR - fastSNR);
            System.out.printf("  MSE reduction: %.6f\n\n", fastMSE - qualityMSE);
        }
    }
    
    @Test
    // @Disabled("Performance comparison - run manually")
    void comparePerformance() throws Exception {
        System.out.println("=== Performance Comparison: Fast vs Quality StreamingDenoiser ===\n");
        
        int warmupSamples = 10_000;
        int benchmarkSamples = 100_000;
        
        // Test different configurations
        double[] overlapFactors = {0.0, 0.5, 0.75};
        
        for (double overlap : overlapFactors) {
            System.out.printf("--- Overlap Factor: %.0f%% ---\n", overlap * 100);
            
            // Benchmark fast implementation
            PerformanceMetrics fastMetrics = benchmarkImplementation(
                "Fast", StreamingDenoiserFactory.Implementation.FAST, overlap, warmupSamples, benchmarkSamples);
            
            // Benchmark quality implementation
            PerformanceMetrics qualityMetrics = benchmarkImplementation(
                "Quality", StreamingDenoiserFactory.Implementation.QUALITY, overlap, warmupSamples, benchmarkSamples);
            
            // Calculate overhead
            double latencyOverhead = (qualityMetrics.avgLatencyUs / fastMetrics.avgLatencyUs - 1) * 100;
            double throughputReduction = (1 - qualityMetrics.throughput / fastMetrics.throughput) * 100;
            
            System.out.printf("Performance Overhead:\n");
            System.out.printf("  Latency increase: %.1f%%\n", latencyOverhead);
            System.out.printf("  Throughput reduction: %.1f%%\n", throughputReduction);
            System.out.printf("  Memory increase: ~%.1f KB\n\n", 
                (qualityMetrics.memoryKB - fastMetrics.memoryKB));
        }
    }
    
    private PerformanceMetrics benchmarkImplementation(String name, StreamingDenoiserFactory.Implementation impl, 
                                                      double overlapFactor, int warmupSamples, 
                                                      int benchmarkSamples) throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(BLOCK_SIZE)
            .overlapFactor(overlapFactor)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .adaptiveThreshold(false)
            .build();
        
        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(impl, config);
        TestSubscriber subscriber = new TestSubscriber();
        denoiser.subscribe(subscriber);
        
        try {
            // Warmup
            for (int i = 0; i < warmupSamples; i++) {
                denoiser.process(Math.sin(2 * Math.PI * i / 64));
            }
            
            // Reset and benchmark
            subscriber.reset();
            long startTime = System.nanoTime();
            long[] sampleTimes = new long[benchmarkSamples];
            
            for (int i = 0; i < benchmarkSamples; i++) {
                long sampleStart = System.nanoTime();
                denoiser.process(Math.sin(2 * Math.PI * i / 64));
                sampleTimes[i] = System.nanoTime() - sampleStart;
            }
            
            long totalTime = System.nanoTime() - startTime;
            
            // Calculate metrics
            long totalLatency = 0;
            for (long time : sampleTimes) {
                totalLatency += time;
            }
            
            double avgLatencyUs = totalLatency / (double)benchmarkSamples / 1000.0;
            double throughput = benchmarkSamples * 1_000_000_000.0 / totalTime;
            
            // Memory estimate
            double memoryKB = denoiser.getPerformanceProfile().memoryUsageBytes() / 1024.0;
            
            System.out.printf("%s StreamingDenoiser:\n", name);
            System.out.printf("  Avg latency: %.2f µs/sample\n", avgLatencyUs);
            System.out.printf("  Throughput: %.0f samples/sec\n", throughput);
            System.out.printf("  Memory: ~%.1f KB\n", memoryKB);
            
            return new PerformanceMetrics(avgLatencyUs, throughput, memoryKB);
            
        } finally {
            denoiser.close();
            subscriber.latch.await(1, TimeUnit.SECONDS);
        }
    }
    
    private double[] processWithFast(double[] signal, double overlapFactor) throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(BLOCK_SIZE)
                .overlapFactor(overlapFactor)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .adaptiveThreshold(false)
                .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.FAST, config)) {
            
            List<double[]> results = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            denoiser.subscribe(new TestSubscriber(results, latch));
            denoiser.process(signal);
            denoiser.close();
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            return reconstructOutput(results, signal.length);
        }
    }
    
    private double[] processWithQuality(double[] signal, double overlapFactor) throws Exception {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(BLOCK_SIZE)
                .overlapFactor(overlapFactor)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .adaptiveThreshold(false)
                .build();
        
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.QUALITY, config)) {
            
            List<double[]> results = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            denoiser.subscribe(new TestSubscriber(results, latch));
            denoiser.process(signal);
            denoiser.close();
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            return reconstructOutput(results, signal.length);
        }
    }
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i * 5 / length) +
                       0.5 * Math.sin(2 * Math.PI * i * 20 / length);
        }
        return signal;
    }
    
    private double[] addGaussianNoise(double[] signal, double noiseLevel, long seed) {
        Random rng = new Random(seed);
        double[] noisy = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + rng.nextGaussian() * noiseLevel;
        }
        
        return noisy;
    }
    
    private double[] reconstructOutput(List<double[]> blocks, int targetLength) {
        double[] output = new double[targetLength];
        int position = 0;
        
        for (double[] block : blocks) {
            int copyLength = Math.min(block.length, targetLength - position);
            System.arraycopy(block, 0, output, position, copyLength);
            position += copyLength;
        }
        
        return output;
    }
    
    private double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }
        
        if (noisePower < 1e-10) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    private double calculateMSE(double[] reference, double[] estimate) {
        double sum = 0;
        for (int i = 0; i < reference.length; i++) {
            double diff = reference[i] - estimate[i];
            sum += diff * diff;
        }
        return sum / reference.length;
    }
    
    private static class PerformanceMetrics {
        final double avgLatencyUs;
        final double throughput;
        final double memoryKB;
        
        PerformanceMetrics(double avgLatencyUs, double throughput, double memoryKB) {
            this.avgLatencyUs = avgLatencyUs;
            this.throughput = throughput;
            this.memoryKB = memoryKB;
        }
    }
    
    private static class TestSubscriber implements Flow.Subscriber<double[]> {
        private final List<double[]> results;
        private final CountDownLatch latch;
        
        TestSubscriber() {
            this.results = null;
            this.latch = new CountDownLatch(1);
        }
        
        TestSubscriber(List<double[]> results, CountDownLatch latch) {
            this.results = results;
            this.latch = latch;
        }
        
        void reset() {
            // For performance testing
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(double[] item) {
            if (results != null) {
                results.add(item.clone());
            }
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
}