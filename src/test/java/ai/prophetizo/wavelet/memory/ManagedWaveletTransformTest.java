package ai.prophetizo.wavelet.memory;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ManagedWaveletTransform functionality.
 */
class ManagedWaveletTransformTest {

    private ManagedWaveletTransform transform;
    private double[] testSignal = {10, 12, 15, 18, 20, 17, 14, 11};

    @BeforeEach
    void setUp() {
        transform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC);
    }

    @AfterEach
    void tearDown() {
        if (transform != null) {
            transform.close();
        }
    }

    @Test
    @DisplayName("ManagedWaveletTransform should perform forward transform")
    void testForwardTransform() {
        try (ManagedTransformResult result = transform.forwardManaged(testSignal)) {
            assertNotNull(result);
            assertEquals(4, result.getManagedApproximationCoeffs().length());
            assertEquals(4, result.getManagedDetailCoeffs().length());
            
            // Verify some expected values for Haar wavelet
            double[] approx = result.approximationCoeffs();
            double[] detail = result.detailCoeffs();
            
            assertNotNull(approx);
            assertNotNull(detail);
            assertEquals(4, approx.length);
            assertEquals(4, detail.length);
        }
    }

    @Test
    @DisplayName("ManagedWaveletTransform should perform inverse transform")
    void testInverseTransform() {
        try (ManagedTransformResult result = transform.forwardManaged(testSignal)) {
            try (ManagedArray reconstructed = transform.inverseManaged(result)) {
                assertEquals(testSignal.length, reconstructed.length());
                
                // Verify reconstruction accuracy
                double[] reconstructedArray = reconstructed.toArray();
                for (int i = 0; i < testSignal.length; i++) {
                    assertEquals(testSignal[i], reconstructedArray[i], 1e-10);
                }
            }
        }
    }

    @Test
    @DisplayName("ManagedWaveletTransform should maintain backward compatibility")
    void testBackwardCompatibility() {
        // Test standard TransformResult interface
        var result = transform.forward(testSignal);
        assertNotNull(result);
        assertEquals(4, result.approximationCoeffs().length);
        assertEquals(4, result.detailCoeffs().length);
        
        // Test inverse with standard TransformResult
        double[] reconstructed = transform.inverse(result);
        assertEquals(testSignal.length, reconstructed.length);
        
        for (int i = 0; i < testSignal.length; i++) {
            assertEquals(testSignal[i], reconstructed[i], 1e-10);
        }
    }

    @Test
    @DisplayName("ManagedWaveletTransform should handle different array factory strategies")
    void testDifferentStrategies() {
        // Test with heap-only strategy
        ArrayFactoryManager heapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.HEAP_ONLY);
        try (ManagedWaveletTransform heapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, heapManager)) {
            try (ManagedTransformResult result = heapTransform.forwardManaged(testSignal)) {
                assertFalse(result.isOffHeap());
            }
        }
        heapManager.close();
        
        // Test with off-heap strategy
        ArrayFactoryManager offHeapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.OFF_HEAP_ONLY);
        try (ManagedWaveletTransform offHeapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, offHeapManager)) {
            try (ManagedTransformResult result = offHeapTransform.forwardManaged(testSignal)) {
                assertTrue(result.isOffHeap());
                assertEquals(32, result.alignment()); // Default alignment
            }
        }
        offHeapManager.close();
    }

    @Test
    @DisplayName("ManagedWaveletTransform should handle large arrays with size-based strategy")
    void testSizeBasedStrategy() {
        // Create a large signal that should trigger off-heap allocation
        double[] largeSignal = new double[2048];
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = Math.sin(2 * Math.PI * i / 64.0);
        }
        
        ArrayFactoryManager sizeBasedManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.SIZE_BASED, 1024);
        try (ManagedWaveletTransform sizeBasedTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, sizeBasedManager)) {
            try (ManagedTransformResult result = sizeBasedTransform.forwardManaged(largeSignal)) {
                // Large arrays should use off-heap storage
                assertTrue(result.isOffHeap());
                
                // Verify transform correctness
                try (ManagedArray reconstructed = sizeBasedTransform.inverseManaged(result)) {
                    double[] reconstructedArray = reconstructed.toArray();
                    for (int i = 0; i < largeSignal.length; i++) {
                        assertEquals(largeSignal[i], reconstructedArray[i], 1e-10);
                    }
                }
            }
        }
        sizeBasedManager.close();
    }

    @Test
    @DisplayName("ManagedWaveletTransform should validate input signals")
    void testInputValidation() {
        assertThrows(Exception.class, () -> transform.forwardManaged(null));
        assertThrows(Exception.class, () -> transform.forwardManaged(new double[0]));
        assertThrows(Exception.class, () -> transform.forwardManaged(new double[3])); // Not power of 2
    }
}