package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.config.TransformConfig;

/**
 * Factory for creating FFM-based wavelet transforms.
 * Implements the common Factory interface for consistency.
 * 
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Direct factory usage
 * FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory();
 * FFMWaveletTransform transform = factory.create(new Haar());
 * 
 * // With custom configuration
 * FFMWaveletTransformFactory factory = new FFMWaveletTransformFactory(
 *     BoundaryMode.ZERO_PADDING, 
 *     new FFMMemoryPool()
 * );
 * 
 * // Factory registry integration
 * FactoryRegistry registry = FactoryRegistry.getInstance();
 * registry.register("ffm-transform", new FFMWaveletTransformFactory());
 * }</pre>
 * 
 * @since 2.0.0
 */
public class FFMWaveletTransformFactory implements Factory<FFMWaveletTransform, Wavelet> {
    
    private static final FFMWaveletTransformFactory DEFAULT_INSTANCE = new FFMWaveletTransformFactory();
    
    private final BoundaryMode defaultBoundaryMode;
    private final FFMMemoryPool sharedPool;
    private final TransformConfig defaultConfig;
    
    /**
     * Creates a factory with default settings.
     */
    public FFMWaveletTransformFactory() {
        this(BoundaryMode.PERIODIC, null, null);
    }
    
    /**
     * Creates a factory with specified boundary mode.
     * 
     * @param defaultBoundaryMode default boundary mode for transforms
     */
    public FFMWaveletTransformFactory(BoundaryMode defaultBoundaryMode) {
        this(defaultBoundaryMode, null, null);
    }
    
    /**
     * Creates a factory with a shared memory pool.
     * 
     * @param defaultBoundaryMode default boundary mode
     * @param sharedPool shared memory pool (null to create per-transform)
     */
    public FFMWaveletTransformFactory(BoundaryMode defaultBoundaryMode, FFMMemoryPool sharedPool) {
        this(defaultBoundaryMode, sharedPool, null);
    }
    
    /**
     * Creates a factory with full configuration.
     * 
     * @param defaultBoundaryMode default boundary mode
     * @param sharedPool shared memory pool (null to create per-transform)
     * @param defaultConfig default transform configuration
     */
    public FFMWaveletTransformFactory(BoundaryMode defaultBoundaryMode, 
                                     FFMMemoryPool sharedPool,
                                     TransformConfig defaultConfig) {
        this.defaultBoundaryMode = defaultBoundaryMode != null ? 
            defaultBoundaryMode : BoundaryMode.PERIODIC;
        this.sharedPool = sharedPool;
        this.defaultConfig = defaultConfig;
    }
    
    @Override
    public FFMWaveletTransform create() {
        throw new UnsupportedOperationException("Wavelet parameter is required. Use create(wavelet) instead.");
    }
    
    @Override
    public FFMWaveletTransform create(Wavelet wavelet) {
        return new FFMWaveletTransform(wavelet, defaultBoundaryMode, sharedPool, defaultConfig);
    }
    
    /**
     * Creates a transform with specific boundary mode.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary mode
     * @return new FFM wavelet transform
     */
    public FFMWaveletTransform create(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new FFMWaveletTransform(wavelet, boundaryMode, sharedPool, defaultConfig);
    }
    
    /**
     * Creates a transform with specific configuration.
     * 
     * @param wavelet the wavelet to use
     * @param config transform configuration
     * @return new FFM wavelet transform
     */
    public FFMWaveletTransform create(Wavelet wavelet, TransformConfig config) {
        return new FFMWaveletTransform(wavelet, defaultBoundaryMode, sharedPool, config);
    }
    
    /**
     * Gets the singleton instance with default settings.
     * 
     * @return default factory instance
     */
    public static FFMWaveletTransformFactory getInstance() {
        return DEFAULT_INSTANCE;
    }
    
    /**
     * Creates an adapter that returns FFMWaveletTransform as WaveletTransform.
     * Useful for backward compatibility.
     * 
     * @return adapter factory
     */
    public Factory<WaveletTransform, Wavelet> asWaveletTransformFactory() {
        return new WaveletTransformAdapter();
    }
    
    /**
     * Adapter to provide WaveletTransform interface compatibility.
     */
    private class WaveletTransformAdapter implements Factory<WaveletTransform, Wavelet> {
        @Override
        public WaveletTransform create() {
            throw new UnsupportedOperationException("Wavelet parameter is required");
        }
        
        @Override
        public WaveletTransform create(Wavelet wavelet) {
            // FFMWaveletTransform can be used as WaveletTransform through delegation
            return new FFMWaveletTransformWrapper(
                FFMWaveletTransformFactory.this.create(wavelet)
            );
        }
    }
    
    /**
     * Wrapper to adapt FFMWaveletTransform to WaveletTransform interface.
     */
    private static class FFMWaveletTransformWrapper extends WaveletTransform {
        private final FFMWaveletTransform delegate;
        
        FFMWaveletTransformWrapper(FFMWaveletTransform delegate) {
            super(delegate.getWavelet(), delegate.getBoundaryMode());
            this.delegate = delegate;
        }
        
        @Override
        public TransformResult forward(double[] signal) {
            return delegate.forward(signal);
        }
        
        @Override
        public TransformResult forward(double[] signal, int offset, int length) {
            return delegate.forward(signal, offset, length);
        }
        
        @Override
        public double[] inverse(TransformResult transformResult) {
            return delegate.inverse(transformResult);
        }
    }
}