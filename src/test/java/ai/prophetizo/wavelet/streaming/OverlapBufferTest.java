package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OverlapBufferTest {
    
    @Test
    void testPowerOfTwoBlockSize() {
        // Should work with power-of-two sizes
        assertDoesNotThrow(() -> new OverlapBuffer(256, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertDoesNotThrow(() -> new OverlapBuffer(512, 0.75, OverlapBuffer.WindowFunction.RECTANGULAR));
        assertDoesNotThrow(() -> new OverlapBuffer(1024, 0.25, OverlapBuffer.WindowFunction.HAMMING));
    }
    
    @Test
    void testNonPowerOfTwoBlockSize() {
        // After our change, non-power-of-two sizes should also work
        assertDoesNotThrow(() -> new OverlapBuffer(100, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertDoesNotThrow(() -> new OverlapBuffer(250, 0.6, OverlapBuffer.WindowFunction.TUKEY));
        assertDoesNotThrow(() -> new OverlapBuffer(1000, 0.8, OverlapBuffer.WindowFunction.HAMMING));
    }
    
    @Test
    void testInvalidBlockSize() {
        // Negative or zero block sizes should fail
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(0, 0.5, OverlapBuffer.WindowFunction.HANN));
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(-100, 0.5, OverlapBuffer.WindowFunction.HANN));
    }
    
    @Test
    void testInvalidOverlapFactor() {
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(256, -0.1, OverlapBuffer.WindowFunction.HANN));
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(256, 1.0, OverlapBuffer.WindowFunction.HANN));
        assertThrows(IllegalArgumentException.class, 
            () -> new OverlapBuffer(256, 1.5, OverlapBuffer.WindowFunction.HANN));
    }
    
    @Test
    void testBasicOperation() {
        int blockSize = 100; // Non-power-of-two
        OverlapBuffer buffer = new OverlapBuffer(blockSize, 0.5, OverlapBuffer.WindowFunction.HANN);
        
        // Create test signal
        double[] block1 = new double[blockSize];
        double[] block2 = new double[blockSize];
        
        for (int i = 0; i < blockSize; i++) {
            block1[i] = Math.sin(2 * Math.PI * i / blockSize);
            block2[i] = Math.cos(2 * Math.PI * i / blockSize);
        }
        
        // Process first block
        double[] output1 = buffer.process(block1);
        assertNotNull(output1);
        assertEquals(blockSize, output1.length);
        
        // First block should pass through unchanged (no history)
        assertArrayEquals(block1, output1, 1e-10);
        
        // Process second block
        double[] output2 = buffer.process(block2);
        assertNotNull(output2);
        assertEquals(blockSize, output2.length);
        
        // Second block should have cross-fade in overlap region
        // Check that overlap region is different from input
        boolean hasOverlapEffect = false;
        int overlapSize = (int)(blockSize * 0.5);
        for (int i = 0; i < overlapSize; i++) {
            if (Math.abs(output2[i] - block2[i]) > 1e-10) {
                hasOverlapEffect = true;
                break;
            }
        }
        assertTrue(hasOverlapEffect, "Overlap region should show cross-fade effect");
    }
    
    @Test
    void testDifferentWindowFunctions() {
        int blockSize = 128;
        double overlapFactor = 0.5;
        
        // Test all window functions work
        for (OverlapBuffer.WindowFunction window : OverlapBuffer.WindowFunction.values()) {
            OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, window);
            
            double[] testBlock = new double[blockSize];
            for (int i = 0; i < blockSize; i++) {
                testBlock[i] = 1.0; // Constant signal
            }
            
            // Process two blocks
            buffer.process(testBlock);
            double[] output = buffer.process(testBlock);
            
            assertNotNull(output);
            assertEquals(blockSize, output.length);
            
            // For constant input, output should still be close to 1.0
            // (window functions should sum to ~1 in overlap region)
            for (double val : output) {
                assertTrue(Math.abs(val - 1.0) < 0.1, 
                    "Window function " + window + " should preserve constant signals");
            }
        }
    }
    
    @Test
    void testReset() {
        OverlapBuffer buffer = new OverlapBuffer(64, 0.5, OverlapBuffer.WindowFunction.HANN);
        
        double[] block1 = new double[64];
        for (int i = 0; i < 64; i++) {
            block1[i] = i;
        }
        
        // Process first block
        buffer.process(block1);
        assertNotNull(buffer.getOverlapRegion());
        
        // Reset
        buffer.reset();
        assertNull(buffer.getOverlapRegion());
        
        // Next block should behave like first block again
        double[] output = buffer.process(block1);
        assertArrayEquals(block1, output, 1e-10);
    }
    
    @Test
    void testMemoryOptimization() {
        // Test that window size scales with overlap factor
        int blockSize = 1024;
        
        // Small overlap factor - should use less memory
        double smallOverlap = 0.1;
        OverlapBuffer smallBuffer = new OverlapBuffer(blockSize, smallOverlap, OverlapBuffer.WindowFunction.HANN);
        
        // Large overlap factor - uses more memory
        double largeOverlap = 0.9;
        OverlapBuffer largeBuffer = new OverlapBuffer(blockSize, largeOverlap, OverlapBuffer.WindowFunction.HANN);
        
        // The window arrays should be sized according to overlap, not full block size
        // This is an implementation detail test to verify memory optimization
        int smallOverlapSize = (int)(blockSize * smallOverlap);
        int largeOverlapSize = (int)(blockSize * largeOverlap);
        
        // Process some data to ensure functionality is preserved
        double[] testBlock = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            testBlock[i] = Math.sin(2 * Math.PI * i / blockSize);
        }
        
        // Both should work correctly despite different window sizes
        assertNotNull(smallBuffer.process(testBlock));
        assertNotNull(largeBuffer.process(testBlock));
        
        // Verify overlap regions are correctly sized
        // After first process, we have history so getOverlapRegion returns data
        double[] smallOverlapRegion = smallBuffer.getOverlapRegion();
        assertNotNull(smallOverlapRegion);
        assertEquals(smallOverlapSize, smallOverlapRegion.length);
        
        double[] largeOverlapRegion = largeBuffer.getOverlapRegion();
        assertNotNull(largeOverlapRegion);
        assertEquals(largeOverlapSize, largeOverlapRegion.length);
        
        // Process second block to ensure overlap processing works
        assertNotNull(smallBuffer.process(testBlock));
        assertNotNull(largeBuffer.process(testBlock));
    }
    
    @Test
    void testGetOverlapRegion() {
        int blockSize = 80;
        double overlapFactor = 0.25;
        OverlapBuffer buffer = new OverlapBuffer(blockSize, overlapFactor, OverlapBuffer.WindowFunction.RECTANGULAR);
        
        // No overlap region initially
        assertNull(buffer.getOverlapRegion());
        
        // Process first block
        double[] block = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            block[i] = i;
        }
        buffer.process(block);
        
        // Now should have overlap region
        double[] overlap = buffer.getOverlapRegion();
        assertNotNull(overlap);
        assertEquals((int)(blockSize * overlapFactor), overlap.length);
        
        // Check overlap contains end of first block
        int overlapSize = (int)(blockSize * overlapFactor);
        for (int i = 0; i < overlapSize; i++) {
            assertEquals(blockSize - overlapSize + i, overlap[i], 1e-10);
        }
    }
}