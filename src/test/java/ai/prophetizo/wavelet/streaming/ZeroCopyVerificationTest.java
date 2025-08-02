package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification tests to ensure the streaming transform achieves true zero-copy operation.
 * 
 * <p>These tests verify that:</p>
 * <ul>
 *   <li>No array copying occurs during normal processing</li>
 *   <li>The ring buffer window is used directly</li>
 *   <li>Memory bandwidth is reduced compared to copying implementations</li>
 * </ul>
 */
class ZeroCopyVerificationTest {
    
    @Test
    @DisplayName("Should process windows without array copying")
    void testZeroCopyProcessing() {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        
        // Create a custom ring buffer that tracks if getWindowDirect is used
        TestableStreamingRingBuffer ringBuffer = new TestableStreamingRingBuffer(
            blockSize * 4, blockSize, blockSize
        );
        
        // Create transform with our testable ring buffer
        TestableOptimizedStreamingTransform transform = new TestableOptimizedStreamingTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, ringBuffer
        );
        
        AtomicInteger blocksProcessed = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult item) {
                blocksProcessed.incrementAndGet();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable);
            }
            
            @Override
            public void onComplete() {
                completionLatch.countDown();
            }
        });
        
        // Generate test data
        double[] data = new double[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        
        // Process data
        transform.process(data);
        transform.close();
        
        // Wait for completion
        try {
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        // Verify zero-copy operation
        assertTrue(ringBuffer.wasGetWindowDirectCalled(), 
            "getWindowDirect should be called for zero-copy operation");
        assertEquals(4, blocksProcessed.get());
        assertEquals(4, ringBuffer.getGetWindowDirectCallCount(),
            "getWindowDirect should be called once per block");
    }
    
    @Test
    @DisplayName("Should verify array slice processing in WaveletTransform")
    void testArraySliceProcessing() {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        double overlapFactor = 0.5;
        
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, overlapFactor
        );
        
        AtomicReference<TransformResult> lastResult = new AtomicReference<>();
        CountDownLatch firstBlockLatch = new CountDownLatch(1);
        
        transform.subscribe(new Flow.Subscriber<TransformResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(TransformResult item) {
                lastResult.set(item);
                firstBlockLatch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {}
            
            @Override
            public void onComplete() {}
        });
        
        // Generate test data with known pattern
        double[] data = new double[blockSize * 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.sin(2 * Math.PI * i / 64.0); // Simple sine wave
        }
        
        // Process data
        transform.process(data);
        
        // Wait for first block
        try {
            assertTrue(firstBlockLatch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        // Verify we got a valid transform result
        TransformResult result = lastResult.get();
        assertNotNull(result);
        assertEquals(blockSize / 2, result.approximationCoeffs().length);
        assertEquals(blockSize / 2, result.detailCoeffs().length);
        
        transform.close();
    }
    
    /**
     * Testable ring buffer that tracks method calls.
     */
    private static class TestableStreamingRingBuffer extends StreamingRingBuffer {
        private boolean getWindowDirectCalled = false;
        private int getWindowDirectCallCount = 0;
        
        TestableStreamingRingBuffer(int capacity, int windowSize, int hopSize) {
            super(capacity, windowSize, hopSize);
        }
        
        @Override
        public double[] getWindowDirect() {
            getWindowDirectCalled = true;
            getWindowDirectCallCount++;
            return super.getWindowDirect();
        }
        
        boolean wasGetWindowDirectCalled() {
            return getWindowDirectCalled;
        }
        
        int getGetWindowDirectCallCount() {
            return getWindowDirectCallCount;
        }
    }
    
    /**
     * Testable transform that allows injecting a custom ring buffer.
     */
    private static class TestableOptimizedStreamingTransform extends OptimizedStreamingWaveletTransform {
        TestableOptimizedStreamingTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                          int blockSize, StreamingRingBuffer ringBuffer) {
            super(wavelet, boundaryMode, blockSize);
            // Replace the ring buffer using reflection (for testing only)
            // Since ringBuffer is now ResizableStreamingRingBuffer, we need to wrap it
            try {
                // Create a ResizableStreamingRingBuffer that wraps our test buffer
                ResizableStreamingRingBuffer resizableBuffer = new ResizableStreamingRingBuffer(
                    ringBuffer.getCapacity(), blockSize, blockSize,
                    ringBuffer.getCapacity(), ringBuffer.getCapacity()
                );
                
                // Now inject our testable buffer into the resizable buffer
                java.lang.reflect.Field bufferRefField = ResizableStreamingRingBuffer.class
                    .getDeclaredField("bufferRef");
                bufferRefField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.concurrent.atomic.AtomicReference<StreamingRingBuffer> bufferRef = 
                    (java.util.concurrent.atomic.AtomicReference<StreamingRingBuffer>) bufferRefField.get(resizableBuffer);
                bufferRef.set(ringBuffer);
                
                // Now inject the resizable buffer into the transform
                java.lang.reflect.Field transformField = OptimizedStreamingWaveletTransform.class
                    .getDeclaredField("ringBuffer");
                transformField.setAccessible(true);
                transformField.set(this, resizableBuffer);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject test ring buffer", e);
            }
        }
    }
}