package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.config.TransformConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Vector API optimization functionality.
 */
class SIMDTest {

    @Test
    void testVectorAvailability() {
        // Test default (auto-detection)
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        System.out.println("Default implementation: " + transform.getImplementationType());
        System.out.println("Available implementations:\n" + WaveletOpsFactory.getAvailableImplementations());
        
        // Test forced scalar
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        WaveletTransform scalarTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, scalarConfig);
        assertFalse(scalarTransform.isUsingVector());
        assertEquals("Scalar", scalarTransform.getImplementationType());
        
        // Test forced Vector API (if available)
        TransformConfig vectorConfig = TransformConfig.builder()
            .forceVector(true)
            .build();
        WaveletTransform vectorTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, vectorConfig);
        System.out.println("Vector implementation: " + vectorTransform.getImplementationType());
    }
    
    @Test
    void testVectorCorrectness() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Test with scalar operations
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        WaveletTransform scalarTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, scalarConfig);
        TransformResult scalarResult = scalarTransform.forward(signal);
        
        // Test with auto-selected operations
        WaveletTransform autoTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult autoResult = autoTransform.forward(signal);
        
        // Results should be identical
        assertArrayEquals(scalarResult.approximationCoeffs(), autoResult.approximationCoeffs(), 1e-10);
        assertArrayEquals(scalarResult.detailCoeffs(), autoResult.detailCoeffs(), 1e-10);
        
        // Test inverse transform
        double[] scalarReconstructed = scalarTransform.inverse(scalarResult);
        double[] autoReconstructed = autoTransform.inverse(autoResult);
        
        assertArrayEquals(scalarReconstructed, autoReconstructed, 1e-10);
        assertArrayEquals(signal, scalarReconstructed, 1e-10);
    }
}