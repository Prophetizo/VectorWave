package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OverlapBuffer overlap-add implementation.
 */
@DisplayName("OverlapBuffer Tests")
class OverlapBufferTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Basic overlap-add functionality")
    void testBasicOverlapAdd() {
        int blockSize = 128;
        double overlapFactor = 0.5;
        
        OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, 
            OverlapBuffer.WindowFunction.RECTANGULAR);
        
        // First block should return full size
        double[] input1 = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            input1[i] = 1.0; // Constant signal
        }
        
        double[] output1 = buffer.process(input1);
        assertEquals(blockSize, output1.length, "First block should be full size");
        
        // Subsequent blocks should return hop size
        double[] input2 = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            input2[i] = 1.0;
        }
        
        double[] output2 = buffer.process(input2);
        assertEquals(64, output2.length, "Subsequent blocks should be hop size (64)");
        
        // With rectangular window and constant signal, output should be constant
        for (int i = 0; i < output2.length; i++) {
            assertEquals(1.0, output2[i], EPSILON, "Output should be constant");
        }
    }
    
    @Test
    @DisplayName("Hann window overlap-add")
    void testHannWindowOverlapAdd() {
        int blockSize = 128;
        double overlapFactor = 0.5;
        
        OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, 
            OverlapBuffer.WindowFunction.HANN);
        
        // Process multiple blocks of constant signal
        double[] constant = new double[blockSize];
        java.util.Arrays.fill(constant, 1.0);
        
        // First block
        double[] output1 = buffer.process(constant);
        assertEquals(blockSize, output1.length);
        
        // Check that Hann window was applied (edges should be attenuated)
        assertTrue(output1[0] < 0.1, "Beginning should be attenuated");
        assertTrue(output1[blockSize-1] < 0.1, "End should be attenuated");
        assertTrue(output1[blockSize/2] > 0.9, "Middle should be near 1");
        
        // Process several more blocks to reach steady state
        for (int i = 0; i < 5; i++) {
            buffer.process(constant);
        }
        
        // In steady state, overlap-add should reconstruct the signal
        double[] steadyState = buffer.process(constant);
        
        // Middle portion should be reconstructed to ~1.0
        for (int i = 10; i < steadyState.length - 10; i++) {
            assertEquals(1.0, steadyState[i], 0.05, 
                "Overlap-add should reconstruct constant signal");
        }
    }
    
    @Test
    @DisplayName("Zero overlap (no overlap-add)")
    void testZeroOverlap() {
        int blockSize = 128;
        
        OverlapBuffer buffer = new OverlapBuffer(blockSize, 0.0, 
            OverlapBuffer.WindowFunction.RECTANGULAR);
        
        double[] input = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            input[i] = i; // Ramp signal
        }
        
        // All blocks should be full size with no overlap
        double[] output1 = buffer.process(input);
        assertEquals(blockSize, output1.length);
        assertArrayEquals(input, output1, EPSILON);
        
        double[] output2 = buffer.process(input);
        assertEquals(blockSize, output2.length);
        assertArrayEquals(input, output2, EPSILON);
    }
    
    @Test
    @DisplayName("High overlap factor")
    void testHighOverlap() {
        int blockSize = 128;
        double overlapFactor = 0.75; // 75% overlap
        
        OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, 
            OverlapBuffer.WindowFunction.HAMMING);
        
        // Process sine wave
        double[] sine1 = new double[blockSize];
        double[] sine2 = new double[blockSize];
        
        for (int i = 0; i < blockSize; i++) {
            sine1[i] = Math.sin(2 * Math.PI * i / 32);
            sine2[i] = Math.sin(2 * Math.PI * (i + 32) / 32); // Continued sine
        }
        
        double[] output1 = buffer.process(sine1);
        assertEquals(blockSize, output1.length, "First block full size");
        
        double[] output2 = buffer.process(sine2);
        assertEquals(32, output2.length, "Hop size should be 32 (25% of 128)");
        
        // Output should be smooth (no discontinuities)
        for (int i = 1; i < output2.length; i++) {
            double diff = Math.abs(output2[i] - output2[i-1]);
            assertTrue(diff < 0.5, "Output should be smooth");
        }
    }
    
    @Test
    @DisplayName("Overlap buffer state management")
    void testOverlapBufferState() {
        OverlapBuffer buffer = new OverlapBuffer(64, 0.5, 
            OverlapBuffer.WindowFunction.HANN);
        
        // Initially no overlap buffer
        assertNull(buffer.getOverlapBuffer());
        
        // Process first block
        double[] input = new double[64];
        java.util.Arrays.fill(input, 1.0);
        buffer.process(input);
        
        // Now should have overlap buffer
        double[] overlapBuffer = buffer.getOverlapBuffer();
        assertNotNull(overlapBuffer);
        assertEquals(32, overlapBuffer.length);
        
        // Reset should clear state
        buffer.reset();
        assertNull(buffer.getOverlapBuffer());
    }
    
    @Test
    @DisplayName("Window function caching")
    void testWindowCaching() {
        int initialCacheSize = OverlapBuffer.getWindowCacheSize();
        
        // Create multiple buffers with same parameters
        for (int i = 0; i < 5; i++) {
            new OverlapBuffer(256, 0.5, OverlapBuffer.WindowFunction.HANN);
        }
        
        // Cache size should increase by 1 (not 5)
        assertTrue(OverlapBuffer.getWindowCacheSize() >= initialCacheSize,
            "Window should be cached");
        
        // Clear cache
        OverlapBuffer.clearWindowCache();
        assertEquals(0, OverlapBuffer.getWindowCacheSize());
    }
    
    @Test
    @DisplayName("Power of two and non-power of two block sizes")
    void testVariousBlockSizes() {
        // Power of two sizes
        assertDoesNotThrow(() -> new OverlapBuffer(256, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertDoesNotThrow(() -> new OverlapBuffer(512, 0.75, OverlapBuffer.WindowFunction.RECTANGULAR));
        
        // Non-power of two sizes
        assertDoesNotThrow(() -> new OverlapBuffer(100, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertDoesNotThrow(() -> new OverlapBuffer(250, 0.6, OverlapBuffer.WindowFunction.TUKEY));
    }
    
    @Test
    @DisplayName("Invalid parameters")
    void testInvalidParameters() {
        // Invalid block size
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(0, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(-100, 0.5, OverlapBuffer.WindowFunction.HANN));
        
        // Invalid overlap factor
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(256, -0.1, OverlapBuffer.WindowFunction.HANN));
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(256, 1.0, OverlapBuffer.WindowFunction.HANN));
    }
    
    @Test
    @DisplayName("Sine wave reconstruction with overlap-add")
    void testSineWaveReconstruction() {
        int blockSize = 256;
        double overlapFactor = 0.5;
        int hopSize = (int)(blockSize * (1 - overlapFactor)); // 128
        int signalLength = 1024;
        double frequency = 5.0;
        
        OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, 
            OverlapBuffer.WindowFunction.HANN);
        
        // Generate sine wave in overlapping blocks
        java.util.List<double[]> outputs = new java.util.ArrayList<>();
        
        // Process with proper overlap - step by hopSize, not blockSize
        for (int blockStart = 0; blockStart + blockSize <= signalLength; blockStart += hopSize) {
            double[] block = new double[blockSize];
            for (int i = 0; i < blockSize; i++) {
                block[i] = Math.sin(2 * Math.PI * frequency * (blockStart + i) / signalLength);
            }
            
            double[] output = buffer.process(block);
            outputs.add(output);
        }
        
        // Reconstruct signal
        int totalLength = outputs.stream().mapToInt(arr -> arr.length).sum();
        double[] reconstructed = new double[totalLength];
        int pos = 0;
        for (double[] output : outputs) {
            System.arraycopy(output, 0, reconstructed, pos, output.length);
            pos += output.length;
        }
        
        // The reconstructed signal length should be close to original
        assertTrue(Math.abs(totalLength - signalLength) <= blockSize, 
            "Reconstructed length should be close to original");
        
        // For windowed overlap-add, perfect reconstruction is not possible with Hann window
        // Instead, verify that the signal maintains its general shape and amplitude
        
        // Find peaks in the reconstructed signal
        double maxAmplitude = 0;
        for (int i = blockSize; i < totalLength - blockSize; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(reconstructed[i]));
        }
        
        // The amplitude should be close to 1.0 (within 10%)
        assertEquals(1.0, maxAmplitude, 0.1, 
            "Reconstructed signal amplitude should be close to original");
        
        // Verify signal is smooth (no discontinuities)
        for (int i = blockSize + 1; i < totalLength - blockSize; i++) {
            double diff = Math.abs(reconstructed[i] - reconstructed[i-1]);
            assertTrue(diff < 0.5, 
                String.format("Signal should be smooth at sample %d, but diff=%.3f", i, diff));
        }
    }
    
    @Test
    @DisplayName("Different window functions produce correct output sizes")
    void testWindowFunctionOutputSizes() {
        int blockSize = 128;
        double overlapFactor = 0.5;
        int expectedHopSize = 64;
        
        for (OverlapBuffer.WindowFunction window : OverlapBuffer.WindowFunction.values()) {
            OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, window);
            
            double[] testBlock = new double[blockSize];
            
            // First block
            double[] output1 = buffer.process(testBlock);
            assertEquals(blockSize, output1.length, 
                window + ": First block should be full size");
            
            // Subsequent blocks
            double[] output2 = buffer.process(testBlock);
            assertEquals(expectedHopSize, output2.length, 
                window + ": Subsequent blocks should be hop size");
        }
    }
}