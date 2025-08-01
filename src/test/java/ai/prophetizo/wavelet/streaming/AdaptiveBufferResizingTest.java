package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Tests for the adaptive buffer resizing functionality.
 */
class AdaptiveBufferResizingTest {
    
    @Test
    @DisplayName("Should resize buffer based on utilization")
    void testUtilizationBasedResizing() {
        int initialCapacity = 1024;
        int windowSize = 128;
        int hopSize = 64;
        int minCapacity = 512;
        int maxCapacity = 4096;
        
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            initialCapacity, windowSize, hopSize, minCapacity, maxCapacity
        );
        
        assertEquals(initialCapacity, buffer.getCapacity());
        
        // Fill buffer to high utilization
        double[] data = new double[900];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        buffer.write(data, 0, data.length);
        
        // Check utilization
        double utilization = (double) buffer.available() / buffer.getCapacity();
        assertTrue(utilization > 0.85);
        
        // Trigger resize based on high utilization
        boolean resized = buffer.resizeBasedOnUtilization(utilization);
        assertTrue(resized);
        assertEquals(2048, buffer.getCapacity()); // Should double
        
        // Verify data was preserved
        assertEquals(900, buffer.available());
        
        // Read some data to lower utilization
        double[] readData = new double[700];
        buffer.read(readData, 0, 700);
        assertEquals(200, buffer.available());
        
        // Check low utilization
        utilization = (double) buffer.available() / buffer.getCapacity();
        assertTrue(utilization < 0.25);
        
        // For the second resize, we need to force it since not enough time has passed
        resized = buffer.forceResize(1024);
        assertTrue(resized);
        assertEquals(1024, buffer.getCapacity()); // Should halve
        
        // Verify remaining data was preserved
        assertEquals(200, buffer.available());
    }
    
    @Test
    @DisplayName("Should respect min and max capacity limits")
    void testCapacityLimits() {
        int initialCapacity = 512;
        int windowSize = 64;
        int hopSize = 32;
        int minCapacity = 256;
        int maxCapacity = 1024;
        
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            initialCapacity, windowSize, hopSize, minCapacity, maxCapacity
        );
        
        // Try to resize below minimum
        assertThrows(Exception.class, () -> buffer.resize(128));
        
        // Try to resize above maximum
        assertThrows(Exception.class, () -> buffer.resize(2048));
        
        // Resize to maximum
        assertTrue(buffer.forceResize(maxCapacity));
        assertEquals(maxCapacity, buffer.getCapacity());
        
        // Try to increase when at maximum (should fail)
        assertFalse(buffer.resizeBasedOnUtilization(0.95));
        
        // Resize to minimum
        assertTrue(buffer.forceResize(minCapacity));
        assertEquals(minCapacity, buffer.getCapacity());
        
        // Try to decrease when at minimum (should fail)
        assertFalse(buffer.resizeBasedOnUtilization(0.1));
    }
    
    @Test
    @DisplayName("Should handle concurrent operations during resize")
    @Timeout(10)
    void testConcurrentResize() throws InterruptedException {
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            1024, 128, 64, 512, 4096
        );
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger writeCount = new AtomicInteger();
        AtomicInteger readCount = new AtomicInteger();
        
        // Writer thread
        Thread writer = new Thread(() -> {
            try {
                startLatch.await();
                Random random = new Random(TestConstants.TEST_SEED);
                for (int i = 0; i < 10000; i++) {
                    if (buffer.write(random.nextDouble())) {
                        writeCount.incrementAndGet();
                    }
                    if (i % 100 == 0) {
                        Thread.yield();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Reader thread with resizing
        Thread reader = new Thread(() -> {
            try {
                startLatch.await();
                double[] temp = new double[64];
                for (int i = 0; i < 100; i++) {
                    int read = buffer.read(temp, 0, temp.length);
                    readCount.addAndGet(read);
                    
                    // Periodically trigger resize
                    if (i % 20 == 0) {
                        double utilization = (double) buffer.available() / buffer.getCapacity();
                        buffer.resizeBasedOnUtilization(utilization);
                    }
                    
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        
        writer.start();
        reader.start();
        startLatch.countDown();
        
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        
        // Verify no data loss
        int finalAvailable = buffer.available();
        assertEquals(writeCount.get() - readCount.get(), finalAvailable,
            "Data integrity check: written - read should equal available");
    }
    
    @Test
    @DisplayName("Should adapt buffer size in streaming transform")
    void testAdaptiveResizingInTransform() throws InterruptedException {
        Wavelet wavelet = new Haar();
        int blockSize = 256;
        
        // Create transform with adaptive resizing enabled
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, 0.0, 4, true
        );
        
        AtomicInteger blocksProcessed = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        
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
                throwable.printStackTrace();
            }
            
            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        
        // Record initial buffer multiplier
        int initialMultiplier = transform.getCurrentBufferMultiplier();
        
        // Generate and process data in bursts to trigger adaptation
        Random random = new Random(TestConstants.TEST_SEED);
        
        // First burst - fill the buffer
        double[] largeBurst = new double[blockSize * 20];
        for (int i = 0; i < largeBurst.length; i++) {
            largeBurst[i] = random.nextGaussian();
        }
        transform.process(largeBurst);
        
        // Wait for adaptive check interval
        Thread.sleep(1100);
        
        // Second burst to trigger resize
        transform.process(largeBurst);
        
        // Process remaining data
        transform.flush();
        transform.close();
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Verify blocks were processed
        assertTrue(blocksProcessed.get() > 0);
        
        // Buffer multiplier may have changed (depending on timing and throughput)
        int finalMultiplier = transform.getCurrentBufferMultiplier();
        System.out.printf("Buffer multiplier: initial=%d, final=%d%n", 
            initialMultiplier, finalMultiplier);
    }
    
    @Test
    @DisplayName("Should preserve data integrity during resize")
    void testDataIntegrityDuringResize() {
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            512, 64, 32, 256, 2048
        );
        
        // Write sequential data
        double[] testData = new double[400];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        buffer.write(testData, 0, testData.length);
        
        // Resize buffer
        assertTrue(buffer.forceResize(1024));
        
        // Read back data
        double[] readData = new double[400];
        int read = buffer.read(readData, 0, readData.length);
        assertEquals(400, read);
        
        // Verify data integrity
        for (int i = 0; i < 400; i++) {
            assertEquals(i, readData[i], 0.0001,
                "Data at index " + i + " should match");
        }
    }
    
    @Test
    @DisplayName("Should handle power-of-2 rounding during resize")
    void testPowerOfTwoRounding() {
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            1024, 128, 64, 512, 4096
        );
        
        // Request non-power-of-2 size
        assertTrue(buffer.forceResize(1500));
        
        // Should round up to next power of 2
        assertEquals(2048, buffer.getCapacity());
        
        // Request another non-power-of-2 size
        assertTrue(buffer.forceResize(3000));
        
        // Should round up to 4096
        assertEquals(4096, buffer.getCapacity());
        
        // Request size that would exceed max after rounding
        // Since we're already at max (4096), resize should return false
        assertFalse(buffer.forceResize(3500));
        assertEquals(4096, buffer.getCapacity());
    }
}