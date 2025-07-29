package ai.prophetizo.wavelet.streaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StreamingThresholdAdapter.
 */
@DisplayName("StreamingThresholdAdapter")
class StreamingThresholdAdapterTest {
    
    private StreamingThresholdAdapter adapter;
    
    @BeforeEach
    void setUp() {
        adapter = new StreamingThresholdAdapter();
    }
    
    @Test
    @DisplayName("Constructor with default parameters")
    void testDefaultConstructor() {
        assertEquals(0.0, adapter.getCurrentThreshold());
        assertEquals(0.0, adapter.getTargetThreshold());
        assertEquals(10.0, adapter.getAttackTime());
        assertEquals(50.0, adapter.getReleaseTime());
    }
    
    @Test
    @DisplayName("Constructor with custom parameters")
    void testCustomConstructor() {
        StreamingThresholdAdapter customAdapter = new StreamingThresholdAdapter(
            5.0, 20.0, 0.1, 10.0);
        
        assertEquals(0.1, customAdapter.getCurrentThreshold());
        assertEquals(0.1, customAdapter.getTargetThreshold());
        assertEquals(5.0, customAdapter.getAttackTime());
        assertEquals(20.0, customAdapter.getReleaseTime());
    }
    
    @Test
    @DisplayName("Constructor validation")
    void testConstructorValidation() {
        // Test invalid attack time
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(0.0, 10.0, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(-1.0, 10.0, 0.0, 10.0));
        
        // Test invalid release time
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(10.0, 0.0, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(10.0, -1.0, 0.0, 10.0));
        
        // Test invalid min threshold
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(10.0, 10.0, -1.0, 10.0));
        
        // Test invalid max threshold
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(10.0, 10.0, 5.0, 5.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new StreamingThresholdAdapter(10.0, 10.0, 5.0, 4.0));
    }
    
    @Test
    @DisplayName("Adapt threshold - attack phase")
    void testAdaptThresholdAttack() {
        adapter.setTargetThreshold(1.0);
        
        // Attack phase - threshold should increase
        double threshold1 = adapter.adaptThreshold();
        assertTrue(threshold1 > 0.0);
        assertTrue(threshold1 < 1.0);
        
        double threshold2 = adapter.adaptThreshold();
        assertTrue(threshold2 > threshold1);
        assertTrue(threshold2 < 1.0);
        
        // Continue adapting - should approach target
        for (int i = 0; i < 50; i++) {
            adapter.adaptThreshold();
        }
        
        double finalThreshold = adapter.getCurrentThreshold();
        assertTrue(Math.abs(finalThreshold - 1.0) < 0.01);
    }
    
    @Test
    @DisplayName("Adapt threshold - release phase")
    void testAdaptThresholdRelease() {
        // Set initial high threshold
        adapter.setCurrentThreshold(1.0);
        adapter.setTargetThreshold(0.0);
        
        // Release phase - threshold should decrease
        double threshold1 = adapter.adaptThreshold();
        assertTrue(threshold1 < 1.0);
        assertTrue(threshold1 > 0.0);
        
        double threshold2 = adapter.adaptThreshold();
        assertTrue(threshold2 < threshold1);
        assertTrue(threshold2 >= 0.0);
        
        // Continue adapting - should approach target
        for (int i = 0; i < 100; i++) {
            adapter.adaptThreshold();
        }
        
        double finalThreshold = adapter.getCurrentThreshold();
        assertTrue(finalThreshold < 0.2, "Final threshold should be close to 0.0, but was: " + finalThreshold);
    }
    
    @Test
    @DisplayName("Adapt threshold - no change when at target")
    void testAdaptThresholdNoChange() {
        adapter.setCurrentThreshold(0.5);
        adapter.setTargetThreshold(0.5);
        
        double threshold1 = adapter.adaptThreshold();
        assertEquals(0.5, threshold1);
        
        double threshold2 = adapter.adaptThreshold();
        assertEquals(0.5, threshold2);
    }
    
    @Test
    @DisplayName("Threshold bounds enforcement")
    void testThresholdBounds() {
        StreamingThresholdAdapter boundedAdapter = new StreamingThresholdAdapter(
            1.0, 1.0, 0.1, 0.9);
        
        // Test upper bound
        boundedAdapter.setTargetThreshold(2.0);
        for (int i = 0; i < 100; i++) {
            boundedAdapter.adaptThreshold();
        }
        assertEquals(0.9, boundedAdapter.getCurrentThreshold());
        
        // Test lower bound
        boundedAdapter.setTargetThreshold(-1.0);
        for (int i = 0; i < 100; i++) {
            boundedAdapter.adaptThreshold();
        }
        assertEquals(0.1, boundedAdapter.getCurrentThreshold());
    }
    
    @Test
    @DisplayName("Set current threshold with bounds")
    void testSetCurrentThresholdBounds() {
        StreamingThresholdAdapter boundedAdapter = new StreamingThresholdAdapter(
            10.0, 10.0, 0.5, 1.5);
        
        // Test setting within bounds
        boundedAdapter.setCurrentThreshold(1.0);
        assertEquals(1.0, boundedAdapter.getCurrentThreshold());
        assertEquals(1.0, boundedAdapter.getTargetThreshold());
        
        // Test setting above max
        boundedAdapter.setCurrentThreshold(2.0);
        assertEquals(1.5, boundedAdapter.getCurrentThreshold());
        assertEquals(1.5, boundedAdapter.getTargetThreshold());
        
        // Test setting below min
        boundedAdapter.setCurrentThreshold(0.0);
        assertEquals(0.5, boundedAdapter.getCurrentThreshold());
        assertEquals(0.5, boundedAdapter.getTargetThreshold());
    }
    
    @Test
    @DisplayName("Set target threshold with bounds")
    void testSetTargetThresholdBounds() {
        StreamingThresholdAdapter boundedAdapter = new StreamingThresholdAdapter(
            10.0, 10.0, 0.5, 1.5);
        
        // Test setting within bounds
        boundedAdapter.setTargetThreshold(1.0);
        assertEquals(1.0, boundedAdapter.getTargetThreshold());
        
        // Test setting above max
        boundedAdapter.setTargetThreshold(2.0);
        assertEquals(1.5, boundedAdapter.getTargetThreshold());
        
        // Test setting below min
        boundedAdapter.setTargetThreshold(0.0);
        assertEquals(0.5, boundedAdapter.getTargetThreshold());
    }
    
    @Test
    @DisplayName("Reset functionality")
    void testReset() {
        StreamingThresholdAdapter customAdapter = new StreamingThresholdAdapter(
            10.0, 10.0, 0.5, 1.5);
        
        // Set some values
        customAdapter.setCurrentThreshold(1.2);
        customAdapter.setTargetThreshold(1.4);
        
        // Reset
        customAdapter.reset();
        
        assertEquals(0.5, customAdapter.getCurrentThreshold());
        assertEquals(0.5, customAdapter.getTargetThreshold());
    }
    
    @Test
    @DisplayName("Has reached target")
    void testHasReachedTarget() {
        adapter.setCurrentThreshold(1.0);
        adapter.setTargetThreshold(1.0);
        
        assertTrue(adapter.hasReachedTarget(0.1));
        assertTrue(adapter.hasReachedTarget(0.01));
        assertTrue(adapter.hasReachedTarget(0.001));
        
        adapter.setTargetThreshold(1.05);
        assertFalse(adapter.hasReachedTarget(0.01));
        assertTrue(adapter.hasReachedTarget(0.1));
        
        adapter.setTargetThreshold(0.95);
        assertFalse(adapter.hasReachedTarget(0.01));
        assertTrue(adapter.hasReachedTarget(0.1));
    }
    
    @Test
    @DisplayName("Different attack and release rates")
    void testDifferentRates() {
        // Fast attack, slow release
        StreamingThresholdAdapter fastAttack = new StreamingThresholdAdapter(
            1.0, 10.0, 0.0, 10.0);
        
        fastAttack.setTargetThreshold(1.0);
        
        // Attack should be fast
        double attackChange1 = fastAttack.adaptThreshold();
        assertTrue(attackChange1 > 0.5); // Should make significant progress
        
        // Switch to release
        fastAttack.setCurrentThreshold(1.0);
        fastAttack.setTargetThreshold(0.0);
        
        double releaseChange1 = fastAttack.adaptThreshold();
        assertTrue(releaseChange1 < 1.0);
        assertTrue(releaseChange1 > 0.9); // Should be slower
    }
}