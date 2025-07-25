package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Configuration for streaming denoiser implementations.
 * 
 * <p>This class encapsulates all configuration parameters that can be used
 * by different {@link StreamingDenoiserStrategy} implementations.</p>
 * 
 * @since 1.8.0
 */
public final class StreamingDenoiserConfig {
    
    private final Wavelet wavelet;
    private final int blockSize;
    private final double overlapFactor;
    private final int levels;
    private final ThresholdMethod thresholdMethod;
    private final ThresholdType thresholdType;
    private final boolean adaptiveThreshold;
    private final double attackTime;
    private final double releaseTime;
    private final boolean useSharedMemoryPool;
    private final int noiseBufferFactor;
    
    private StreamingDenoiserConfig(Builder builder) {
        this.wavelet = builder.wavelet;
        this.blockSize = builder.blockSize;
        this.overlapFactor = builder.overlapFactor;
        this.levels = builder.levels;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdType = builder.thresholdType;
        this.adaptiveThreshold = builder.adaptiveThreshold;
        this.attackTime = builder.attackTime;
        this.releaseTime = builder.releaseTime;
        this.useSharedMemoryPool = builder.useSharedMemoryPool;
        this.noiseBufferFactor = builder.noiseBufferFactor;
    }
    
    // Getters
    public Wavelet getWavelet() { return wavelet; }
    public int getBlockSize() { return blockSize; }
    public double getOverlapFactor() { return overlapFactor; }
    public int getLevels() { return levels; }
    public ThresholdMethod getThresholdMethod() { return thresholdMethod; }
    public ThresholdType getThresholdType() { return thresholdType; }
    public boolean isAdaptiveThreshold() { return adaptiveThreshold; }
    public double getAttackTime() { return attackTime; }
    public double getReleaseTime() { return releaseTime; }
    public boolean isUseSharedMemoryPool() { return useSharedMemoryPool; }
    public int getNoiseBufferFactor() { return noiseBufferFactor; }
    
    /**
     * Builder for creating StreamingDenoiserConfig instances.
     */
    public static class Builder {
        private Wavelet wavelet;
        private int blockSize = 512;
        private double overlapFactor = 0.5;
        private int levels = 1;
        private ThresholdMethod thresholdMethod = ThresholdMethod.UNIVERSAL;
        private ThresholdType thresholdType = ThresholdType.SOFT;
        private boolean adaptiveThreshold = true;
        private double attackTime = 10.0;
        private double releaseTime = 50.0;
        private boolean useSharedMemoryPool = true;
        private int noiseBufferFactor = 4;
        
        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        public Builder blockSize(int blockSize) {
            if (blockSize <= 0 || (blockSize & (blockSize - 1)) != 0) {
                throw new IllegalArgumentException(
                    "Block size must be a positive power of 2");
            }
            this.blockSize = blockSize;
            return this;
        }
        
        public Builder overlapFactor(double overlapFactor) {
            if (overlapFactor < 0.0 || overlapFactor >= 1.0) {
                throw new IllegalArgumentException(
                    "Overlap factor must be in [0, 1)");
            }
            this.overlapFactor = overlapFactor;
            return this;
        }
        
        public Builder levels(int levels) {
            if (levels <= 0) {
                throw new IllegalArgumentException(
                    "Levels must be positive");
            }
            this.levels = levels;
            return this;
        }
        
        public Builder thresholdMethod(ThresholdMethod method) {
            this.thresholdMethod = method;
            return this;
        }
        
        public Builder thresholdType(ThresholdType type) {
            this.thresholdType = type;
            return this;
        }
        
        public Builder adaptiveThreshold(boolean adaptive) {
            this.adaptiveThreshold = adaptive;
            return this;
        }
        
        public Builder attackTime(double attackTime) {
            if (attackTime <= 0) {
                throw new IllegalArgumentException(
                    "Attack time must be positive");
            }
            this.attackTime = attackTime;
            return this;
        }
        
        public Builder releaseTime(double releaseTime) {
            if (releaseTime <= 0) {
                throw new IllegalArgumentException(
                    "Release time must be positive");
            }
            this.releaseTime = releaseTime;
            return this;
        }
        
        public Builder useSharedMemoryPool(boolean useShared) {
            this.useSharedMemoryPool = useShared;
            return this;
        }
        
        public Builder noiseBufferFactor(int factor) {
            if (factor <= 0) {
                throw new IllegalArgumentException(
                    "Noise buffer factor must be positive");
            }
            this.noiseBufferFactor = factor;
            return this;
        }
        
        public StreamingDenoiserConfig build() {
            if (wavelet == null) {
                throw new InvalidArgumentException(
                    "Wavelet must be specified");
            }
            return new StreamingDenoiserConfig(this);
        }
    }
}