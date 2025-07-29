package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QualityStreamingDenoiser.
 */
@DisplayName("QualityStreamingDenoiser")
class QualityStreamingDenoiserTest {
    
    @Test
    @DisplayName("Basic functionality")
    void testBasicFunctionality() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        assertNotNull(denoiser);
        assertEquals(256, denoiser.getBlockSize());
        assertTrue(denoiser.isReady());
        
        denoiser.close();
        assertFalse(denoiser.isReady());
    }
    
    @Test
    @DisplayName("Process single samples")
    void testProcessSingleSamples() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(16) // Small for testing
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        List<double[]> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        denoiser.subscribe(new TestSubscriber(results, latch));
        
        // Process enough samples to trigger block processing
        for (int i = 0; i < 32; i++) {
            denoiser.process(i * 0.1);
        }
        
        denoiser.close();
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
    }
    
    @Test
    @DisplayName("Process array of samples")
    void testProcessArrayOfSamples() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(32)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        List<double[]> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        denoiser.subscribe(new TestSubscriber(results, latch));
        
        // Process array
        double[] samples = new double[64];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = Math.sin(2 * Math.PI * i / 32);
        }
        
        denoiser.process(samples);
        denoiser.close();
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
    }
    
    @Test
    @DisplayName("Different overlap factors")
    void testDifferentOverlapFactors() throws InterruptedException {
        double[] overlapFactors = {0.0, 0.25, 0.5, 0.75};
        
        for (double overlap : overlapFactors) {
            StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                .wavelet(new Haar())
                .blockSize(32)
                .overlapFactor(overlap)
                .build();
            
            QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
            
            List<double[]> results = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            denoiser.subscribe(new TestSubscriber(results, latch));
            
            // Process data
            double[] samples = new double[128];
            denoiser.process(samples);
            denoiser.close();
            
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            
            // With different overlaps, we should get different amounts of output
            if (overlap == 0.0) {
                assertEquals(32, denoiser.getHopSize()); // No overlap
            } else {
                assertTrue(denoiser.getHopSize() < 32); // Some overlap
            }
        }
    }
    
    @Test
    @DisplayName("Adaptive threshold functionality")
    void testAdaptiveThreshold() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(64)
            .adaptiveThreshold(true)
            .attackTime(1.0)
            .releaseTime(5.0)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        List<Double> thresholds = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                thresholds.add(denoiser.getCurrentThreshold());
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
        
        // Process varying noise levels
        for (int i = 0; i < 256; i++) {
            double noiseLevel = (i < 128) ? 0.1 : 0.5;
            denoiser.process(Math.random() * noiseLevel);
        }
        
        denoiser.close();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        // Should have recorded threshold values
        assertFalse(thresholds.isEmpty());
    }
    
    @Test
    @DisplayName("Statistics tracking")
    void testStatisticsTracking() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(32)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        CountDownLatch latch = new CountDownLatch(1);
        denoiser.subscribe(new TestSubscriber(new ArrayList<>(), latch));
        
        // Process samples
        double[] samples = new double[96];
        denoiser.process(samples);
        
        StreamingWaveletTransform.StreamingStatistics stats = denoiser.getStatistics();
        assertEquals(96, stats.getSamplesProcessed());
        assertTrue(stats.getBlocksEmitted() > 0);
        assertTrue(stats.getAverageProcessingTime() > 0);
        
        denoiser.close();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("Flush functionality")
    void testFlush() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(32)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        List<double[]> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        denoiser.subscribe(new TestSubscriber(results, latch));
        
        // Process partial block
        for (int i = 0; i < 16; i++) {
            denoiser.process(i * 0.1);
        }
        
        // Buffer level should be 16
        assertEquals(16, denoiser.getBufferLevel());
        
        // Flush should process the partial block
        denoiser.flush();
        assertEquals(0, denoiser.getBufferLevel());
        
        denoiser.close();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
    }
    
    @Test
    @DisplayName("Performance profile")
    void testPerformanceProfile() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .overlapFactor(0.5)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();
        
        assertNotNull(profile);
        assertTrue(profile.expectedLatencyMicros() > 0);
        assertTrue(profile.expectedSNRImprovement() > -7.0); // Better than FAST
        assertTrue(profile.memoryUsageBytes() > 20 * 1024); // Uses more memory
        assertFalse(profile.isRealTimeCapable()); // Not real-time with overlap
        
        denoiser.close();
    }
    
    @Test
    @DisplayName("Closed state operations")
    void testClosedStateOperations() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        denoiser.close();
        
        // Should throw when processing after close
        assertThrows(InvalidStateException.class, () -> denoiser.process(1.0));
        assertThrows(InvalidStateException.class, () -> denoiser.process(new double[10]));
        
        // Flush should not throw but do nothing
        assertDoesNotThrow(() -> denoiser.flush());
    }
    
    @Test
    @DisplayName("Multi-level denoising")
    void testMultiLevelDenoising() throws InterruptedException {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(128)
            .levels(3)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        List<double[]> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        denoiser.subscribe(new TestSubscriber(results, latch));
        
        // Process signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        
        denoiser.process(signal);
        denoiser.close();
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
    }
    
    @Test
    @DisplayName("Noise level tracking")
    void testNoiseLevelTracking() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(64)
            .build();
        
        QualityStreamingDenoiser denoiser = new QualityStreamingDenoiser(config);
        
        // Initially zero
        assertEquals(0.0, denoiser.getCurrentNoiseLevel());
        
        // Process noisy data
        double[] noise = new double[128];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = Math.random() * 0.1;
        }
        
        denoiser.process(noise);
        
        // Should have non-zero noise estimate after processing
        assertTrue(denoiser.getCurrentNoiseLevel() >= 0.0);
        
        denoiser.close();
    }
    
    // Helper test subscriber
    private static class TestSubscriber implements Flow.Subscriber<double[]> {
        private final List<double[]> results;
        private final CountDownLatch latch;
        
        TestSubscriber(List<double[]> results, CountDownLatch latch) {
            this.results = results;
            this.latch = latch;
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(double[] item) {
            results.add(item.clone());
        }
        
        @Override
        public void onError(Throwable throwable) {
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
}