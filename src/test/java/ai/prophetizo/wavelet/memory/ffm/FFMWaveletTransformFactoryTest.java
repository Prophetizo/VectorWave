package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FFMWaveletTransformFactory.
 */
@EnabledForJreRange(min = JRE.JAVA_21) // FFM API available from Java 21+, project targets Java 23
class FFMWaveletTransformFactoryTest {

    @Test
    void testDefaultConstructor() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        assertNotNull(factory);
        assertEquals("FFMWaveletTransformFactory", factory.getDescription());
    }

    @Test
    void testConstructorWithBoundaryMode() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory(BoundaryMode.ZERO_PADDING);
        assertNotNull(factory);
    }

    @Test
    void testConstructorWithBoundaryModeAndPool() {
        FFMMemoryPool pool = new FFMMemoryPool();
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory(BoundaryMode.PERIODIC, pool);
        assertNotNull(factory);
        pool.close();
    }

    @Test
    void testConstructorWithFullConfiguration() {
        FFMMemoryPool pool = new FFMMemoryPool();
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory(
            BoundaryMode.ZERO_PADDING, pool, null);
        assertNotNull(factory);
        pool.close();
    }

    @Test
    void testCreateWithWavelet() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        Haar wavelet = new Haar();
        
        FFMWaveletTransform transform = factory.create(wavelet);
        assertNotNull(transform);
        transform.close();
    }

    @Test
    void testCreateWithNullWavelet() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        assertThrows(NullPointerException.class, () -> factory.create((Wavelet) null));
    }

    @Test
    void testCreate() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        assertThrows(UnsupportedOperationException.class, factory::create);
    }

    @Test
    void testIsValidConfiguration() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        
        assertTrue(factory.isValidConfiguration(new Haar()));
        // Create Daubechies properly with name, coefficients, and order
        Daubechies db4 = new Daubechies("db4", 
            new double[]{0.23037781, 0.71484657, 0.63088077, -0.02798377, -0.18703481, 0.03084138, 0.03288301, -0.01059740},
            4);
        assertTrue(factory.isValidConfiguration(db4));
        assertFalse(factory.isValidConfiguration(null));
    }

    @Test
    void testGetDescription() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        String description = factory.getDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty());
    }

    @Test
    void testWrapperCreation() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        Haar wavelet = new Haar();
        
        FFMWaveletTransform transform = factory.create(wavelet);
        
        // Test that the wrapper properly delegates
        assertNotNull(transform);
        
        // Clean up
        transform.close();
    }

    @Test
    void testMultipleCreations() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        Haar wavelet = new Haar();
        
        FFMWaveletTransform transform1 = factory.create(wavelet);
        FFMWaveletTransform transform2 = factory.create(wavelet);
        
        assertNotNull(transform1);
        assertNotNull(transform2);
        assertNotSame(transform1, transform2); // Should create different instances
        
        transform1.close();
        transform2.close();
    }

    @Test
    void testCreateWithDifferentWavelets() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        
        FFMWaveletTransform haarTransform = factory.create(new Haar());
        Daubechies db4 = new Daubechies("db4", 
            new double[]{0.23037781, 0.71484657, 0.63088077, -0.02798377, -0.18703481, 0.03084138, 0.03288301, -0.01059740},
            4);
        FFMWaveletTransform db4Transform = factory.create(db4);
        
        assertNotNull(haarTransform);
        assertNotNull(db4Transform);
        
        haarTransform.close();
        db4Transform.close();
    }

    @Test
    void testCreateWithBoundaryMode() {
        FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
        Haar wavelet = new Haar();
        
        FFMWaveletTransform transform = factory.create(wavelet, BoundaryMode.ZERO_PADDING);
        assertNotNull(transform);
        transform.close();
    }
}