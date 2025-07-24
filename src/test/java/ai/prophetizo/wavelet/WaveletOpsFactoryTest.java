package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class WaveletOpsFactoryTest {

    @Test
    void testCreateOptimal_ReturnsNonNullImplementation() {
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.createOptimal();
        assertNotNull(ops);
        assertNotNull(ops.getImplementationType());
    }

    @Test
    void testCreate_WithNullConfig_ReturnsOptimalImplementation() {
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(null);
        assertNotNull(ops);
        assertNotNull(ops.getImplementationType());
    }

    @Test
    void testCreate_WithScalarConfig_ReturnsScalarImplementation() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        assertNotNull(ops);
        assertEquals("Scalar", ops.getImplementationType());
    }

    @Test
    void testCreate_WithNonScalarConfig_ReturnsOptimalImplementation() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(false)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        assertNotNull(ops);
        // Could be "Scalar" or "Optimized ..." depending on platform
        assertNotNull(ops.getImplementationType());
    }

    @Test
    void testGetAvailableImplementations() {
        String info = WaveletOpsFactory.getAvailableImplementations();
        
        assertNotNull(info);
        assertTrue(info.contains("Available implementations:"));
        assertTrue(info.contains("Scalar: Always available"));
        assertTrue(info.contains("Vector:"));
    }

    @Test
    void testScalarOps_ConvolveAndDownsample_Periodic() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = ops.convolveAndDownsample(signal, filter, 4, 2, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(2, result.length); // Downsampled by 2
    }

    @Test
    void testScalarOps_ConvolveAndDownsample_ZeroPadding() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = ops.convolveAndDownsample(signal, filter, 4, 2, BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(2, result.length); // Downsampled by 2
    }

    @Test
    void testScalarOps_ConvolveAndDownsample_UnsupportedMode() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, 0.5};
        
        assertThrows(UnsupportedOperationException.class, () ->
            ops.convolveAndDownsample(signal, filter, 4, 2, BoundaryMode.SYMMETRIC)
        );
    }

    @Test
    void testScalarOps_UpsampleAndConvolve_Periodic() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = ops.upsampleAndConvolve(signal, filter, 2, 2, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(4, result.length); // Upsampled by 2
    }

    @Test
    void testScalarOps_UpsampleAndConvolve_ZeroPadding() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = ops.upsampleAndConvolve(signal, filter, 2, 2, BoundaryMode.ZERO_PADDING);
        
        assertNotNull(result);
        assertEquals(4, result.length); // Upsampled by 2
    }

    @Test
    void testScalarOps_UpsampleAndConvolve_UnsupportedMode() {
        TransformConfig config = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.create(config);
        
        double[] signal = {1.0, 2.0};
        double[] filter = {0.5, 0.5};
        
        assertThrows(UnsupportedOperationException.class, () ->
            ops.upsampleAndConvolve(signal, filter, 2, 2, BoundaryMode.SYMMETRIC)
        );
    }

    @Test
    void testOptimalOps_BasicOperations() {
        // Test with optimal selection (may be scalar or vector)
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.createOptimal();
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.7071, 0.7071}; // Haar low-pass
        
        // Test convolve and downsample
        double[] downsampled = ops.convolveAndDownsample(signal, filter, 8, 2, BoundaryMode.PERIODIC);
        assertNotNull(downsampled);
        assertEquals(4, downsampled.length);
        
        // Test upsample and convolve
        double[] upsampled = ops.upsampleAndConvolve(downsampled, filter, 4, 2, BoundaryMode.PERIODIC);
        assertNotNull(upsampled);
        assertEquals(8, upsampled.length);
    }

    @Test
    void testSingletonBehavior() {
        // Multiple calls should return the same instance types
        WaveletOpsFactory.WaveletOps ops1 = WaveletOpsFactory.createOptimal();
        WaveletOpsFactory.WaveletOps ops2 = WaveletOpsFactory.createOptimal();
        
        // Should be the same implementation type
        assertEquals(ops1.getImplementationType(), ops2.getImplementationType());
        
        // Scalar config should always return scalar
        TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps scalar1 = WaveletOpsFactory.create(scalarConfig);
        WaveletOpsFactory.WaveletOps scalar2 = WaveletOpsFactory.create(scalarConfig);
        
        assertEquals("Scalar", scalar1.getImplementationType());
        assertEquals("Scalar", scalar2.getImplementationType());
    }

    @Test
    void testImplementationConsistency() {
        // Both scalar and optimal should produce same mathematical results
        TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
        
        WaveletOpsFactory.WaveletOps scalarOps = WaveletOpsFactory.create(scalarConfig);
        WaveletOpsFactory.WaveletOps optimalOps = WaveletOpsFactory.createOptimal();
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        // Test periodic mode
        double[] scalarResult = scalarOps.convolveAndDownsample(
                signal, filter, 8, 2, BoundaryMode.PERIODIC);
        double[] optimalResult = optimalOps.convolveAndDownsample(
                signal, filter, 8, 2, BoundaryMode.PERIODIC);
        
        assertArrayEquals(scalarResult, optimalResult, 1e-10);
        
        // Test zero padding mode
        scalarResult = scalarOps.convolveAndDownsample(
                signal, filter, 8, 2, BoundaryMode.ZERO_PADDING);
        optimalResult = optimalOps.convolveAndDownsample(
                signal, filter, 8, 2, BoundaryMode.ZERO_PADDING);
        
        assertArrayEquals(scalarResult, optimalResult, 1e-10);
    }

    @Test
    void testLargeSignalProcessing() {
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.createOptimal();
        
        int size = 1024;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0);
        }
        
        double[] filter = {0.7071, 0.7071};
        
        double[] result = ops.convolveAndDownsample(signal, filter, size, 2, BoundaryMode.PERIODIC);
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify some values are non-zero
        boolean hasNonZero = false;
        for (double v : result) {
            if (v != 0.0) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero);
    }

    @Test
    void testErrorPropagation() {
        WaveletOpsFactory.WaveletOps ops = WaveletOpsFactory.createOptimal();
        
        // Test with invalid inputs
        assertThrows(Exception.class, () ->
            ops.convolveAndDownsample(null, new double[]{1.0}, 1, 1, BoundaryMode.PERIODIC)
        );
        
        assertThrows(Exception.class, () ->
            ops.upsampleAndConvolve(new double[]{1.0}, null, 1, 1, BoundaryMode.PERIODIC)
        );
    }
    
    @Test
    void testVectorWaveletOps() throws Exception {
        // Use reflection to test VectorWaveletOps directly
        Class<?> vectorOpsClass = Class.forName(
            "ai.prophetizo.wavelet.WaveletOpsFactory$VectorWaveletOps");
        Constructor<?> constructor = vectorOpsClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        WaveletOpsFactory.WaveletOps vectorOps = 
            (WaveletOpsFactory.WaveletOps) constructor.newInstance();
        
        // Test getImplementationType
        String implType = vectorOps.getImplementationType();
        assertNotNull(implType);
        assertTrue(implType.startsWith("Vector"));
        assertTrue(implType.contains("Species"));
        
        // Test operations
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.5};
        
        // Test PERIODIC mode
        double[] result = vectorOps.convolveAndDownsample(
            signal, filter, 8, 2, BoundaryMode.PERIODIC);
        assertNotNull(result);
        assertEquals(4, result.length);
        
        result = vectorOps.upsampleAndConvolve(
            signal, filter, 8, 2, BoundaryMode.PERIODIC);
        assertNotNull(result);
        assertEquals(16, result.length);
        
        // Test ZERO_PADDING mode
        result = vectorOps.convolveAndDownsample(
            signal, filter, 8, 2, BoundaryMode.ZERO_PADDING);
        assertNotNull(result);
        assertEquals(4, result.length);
        
        result = vectorOps.upsampleAndConvolve(
            signal, filter, 8, 2, BoundaryMode.ZERO_PADDING);
        assertNotNull(result);
        assertEquals(16, result.length);
        
        // Test unsupported mode
        assertThrows(UnsupportedOperationException.class, () ->
            vectorOps.convolveAndDownsample(
                signal, filter, 8, 2, BoundaryMode.SYMMETRIC)
        );
        
        assertThrows(UnsupportedOperationException.class, () ->
            vectorOps.upsampleAndConvolve(
                signal, filter, 8, 2, BoundaryMode.SYMMETRIC)
        );
    }
    
    @Test
    void testOptimizedVectorWaveletOps() throws Exception {
        // Use reflection to test OptimizedVectorWaveletOps directly
        Class<?> optimizedOpsClass = Class.forName(
            "ai.prophetizo.wavelet.WaveletOpsFactory$OptimizedVectorWaveletOps");
        Constructor<?> constructor = optimizedOpsClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        WaveletOpsFactory.WaveletOps optimizedOps = 
            (WaveletOpsFactory.WaveletOps) constructor.newInstance();
        
        // Test getImplementationType
        String implType = optimizedOps.getImplementationType();
        assertNotNull(implType);
        assertTrue(implType.startsWith("Optimized"));
        
        // Test operations
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        double[] filter = {0.7071, 0.7071, 0, 0}; // Padded Haar filter
        
        // Test PERIODIC mode
        double[] result = optimizedOps.convolveAndDownsample(
            signal, filter, 128, 4, BoundaryMode.PERIODIC);
        assertNotNull(result);
        assertEquals(64, result.length);
        
        result = optimizedOps.upsampleAndConvolve(
            signal, filter, 128, 4, BoundaryMode.PERIODIC);
        assertNotNull(result);
        assertEquals(256, result.length);
        
        // Test ZERO_PADDING mode (falls back to regular VectorOps)
        result = optimizedOps.convolveAndDownsample(
            signal, filter, 128, 4, BoundaryMode.ZERO_PADDING);
        assertNotNull(result);
        assertEquals(64, result.length);
        
        result = optimizedOps.upsampleAndConvolve(
            signal, filter, 128, 4, BoundaryMode.ZERO_PADDING);
        assertNotNull(result);
        assertEquals(256, result.length);
        
        // Test unsupported modes
        assertThrows(UnsupportedOperationException.class, () ->
            optimizedOps.convolveAndDownsample(
                signal, filter, 128, 4, BoundaryMode.SYMMETRIC)
        );
        
        assertThrows(UnsupportedOperationException.class, () ->
            optimizedOps.upsampleAndConvolve(
                signal, filter, 128, 4, BoundaryMode.SYMMETRIC)
        );
    }
    
    @Test
    void testVectorOpsConsistency() throws Exception {
        // Create instances of all three implementations
        TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
        WaveletOpsFactory.WaveletOps scalarOps = WaveletOpsFactory.create(scalarConfig);
        
        Class<?> vectorOpsClass = Class.forName(
            "ai.prophetizo.wavelet.WaveletOpsFactory$VectorWaveletOps");
        Constructor<?> vectorConstructor = vectorOpsClass.getDeclaredConstructor();
        vectorConstructor.setAccessible(true);
        WaveletOpsFactory.WaveletOps vectorOps = 
            (WaveletOpsFactory.WaveletOps) vectorConstructor.newInstance();
        
        Class<?> optimizedOpsClass = Class.forName(
            "ai.prophetizo.wavelet.WaveletOpsFactory$OptimizedVectorWaveletOps");
        Constructor<?> optimizedConstructor = optimizedOpsClass.getDeclaredConstructor();
        optimizedConstructor.setAccessible(true);
        WaveletOpsFactory.WaveletOps optimizedOps = 
            (WaveletOpsFactory.WaveletOps) optimizedConstructor.newInstance();
        
        // Test data
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.1 + Math.cos(2 * Math.PI * i / 8.0);
        }
        double[] filter = {0.7071, 0.7071};
        
        // Compare results - all should be mathematically equivalent
        double[] scalarResult = scalarOps.convolveAndDownsample(
            signal, filter, 64, 2, BoundaryMode.PERIODIC);
        double[] vectorResult = vectorOps.convolveAndDownsample(
            signal, filter, 64, 2, BoundaryMode.PERIODIC);
        double[] optimizedResult = optimizedOps.convolveAndDownsample(
            signal, filter, 64, 2, BoundaryMode.PERIODIC);
        
        assertArrayEquals(scalarResult, vectorResult, 1e-10);
        assertArrayEquals(scalarResult, optimizedResult, 1e-10);
        
        // Test upsampling too
        scalarResult = scalarOps.upsampleAndConvolve(
            signal, filter, 64, 2, BoundaryMode.ZERO_PADDING);
        vectorResult = vectorOps.upsampleAndConvolve(
            signal, filter, 64, 2, BoundaryMode.ZERO_PADDING);
        optimizedResult = optimizedOps.upsampleAndConvolve(
            signal, filter, 64, 2, BoundaryMode.ZERO_PADDING);
        
        assertArrayEquals(scalarResult, vectorResult, 1e-10);
        assertArrayEquals(scalarResult, optimizedResult, 1e-10);
    }
    
    @Test
    void testFactorySelection() {
        // Test that factory makes appropriate selections
        String available = WaveletOpsFactory.getAvailableImplementations();
        
        // Check if vector operations are available
        boolean hasVector = available.contains("Vector:") && 
                           !available.contains("Vector: Not available");
        
        // If vector is available, optimal should not be scalar
        if (hasVector) {
            WaveletOpsFactory.WaveletOps optimal = WaveletOpsFactory.createOptimal();
            String optimalType = optimal.getImplementationType();
            assertNotEquals("Scalar", optimalType);
            assertTrue(optimalType.contains("Vector") || optimalType.contains("Optimized"));
        }
    }
}