package ai.prophetizo.wavelet.config;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;
import ai.prophetizo.wavelet.util.NullChecks;

/**
 * Immutable configuration for wavelet transform operations.
 * 
 * <p>This class provides fine-grained control over wavelet transform behavior,
 * including performance optimizations, boundary handling, and decomposition limits.
 * It uses the builder pattern for flexible, readable construction.</p>
 * 
 * <p>Key configuration options:</p>
 * <ul>
 *   <li><b>Boundary Mode</b>: How to handle signal boundaries during convolution</li>
 *   <li><b>Optimization Path</b>: Force scalar or SIMD operations, or auto-detect</li>
 *   <li><b>Decomposition Levels</b>: Maximum allowed levels for multi-level transforms</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Auto-detect optimal path (recommended)
 * TransformConfig autoConfig = TransformConfig.defaultConfig();
 * 
 * // Force scalar operations for debugging
 * TransformConfig scalarConfig = TransformConfig.builder()
 *     .forceScalar(true)
 *     .boundaryMode(BoundaryMode.ZERO_PADDING)
 *     .build();
 * 
 * // Force SIMD for maximum performance
 * TransformConfig simdConfig = TransformConfig.builder()
 *     .forceSIMD(true)
 *     .maxDecompositionLevels(5)
 *     .build();
 * 
 * // Use with transform
 * WaveletTransform transform = new WaveletTransform(
 *     Daubechies.DB4, 
 *     BoundaryMode.PERIODIC, 
 *     simdConfig
 * );
 * }</pre>
 * 
 * <p>Performance notes:</p>
 * <ul>
 *   <li>Auto-detection usually provides optimal performance</li>
 *   <li>Force scalar for consistent behavior across platforms</li>
 *   <li>Force SIMD when you know your data size benefits from vectorization</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public final class TransformConfig {

    // Boundary handling mode for the transform.
    private final BoundaryMode boundaryMode;
    // Forces scalar engine if true, otherwise auto-detects.
    private final boolean forceScalar;
    // Forces use of SIMD operations when available.
    private final boolean forceSIMD;
    // Maximum allowed decomposition levels.
    private final int maxDecompositionLevels;

    /**
     * Private constructor used by Builder.
     *
     * @param builder Builder instance with configuration values.
     */
    private TransformConfig(Builder builder) {
        this.boundaryMode = builder.boundaryMode;
        this.forceScalar = builder.forceScalar;
        this.forceSIMD = builder.forceSIMD;
        this.maxDecompositionLevels = builder.maxDecompositionLevels;

        // Validate: can't force both scalar and SIMD
        if (forceScalar && forceSIMD) {
            throw InvalidConfigurationException.conflictingOptions("forceScalar", "forceSIMD");
        }
    }

    /**
     * Creates a new builder for TransformConfig.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration.
     *
     * @return default config with periodic boundaries and auto-detected engine
     */
    public static TransformConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * @return boundary mode for the transform
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }

    /**
     * @return true if scalar engine is forced
     */
    public boolean isForceScalar() {
        return forceScalar;
    }

    /**
     * @return true if scalar operations are forced (same as isForceScalar)
     */
    public boolean isForceScalarOperations() {
        return forceScalar;
    }

    /**
     * @return true if SIMD operations are forced when available
     */
    public boolean isForceSIMD() {
        return forceSIMD;
    }

    /**
     * @return maximum decomposition levels
     */
    public int getMaxDecompositionLevels() {
        return maxDecompositionLevels;
    }

    /**
     * Returns a string representation of the configuration.
     *
     * @return string with field values
     */
    @Override
    public String toString() {
        return "TransformConfig{" +
                "boundaryMode=" + boundaryMode +
                ", forceScalar=" + forceScalar +
                ", forceSIMD=" + forceSIMD +
                ", maxDecompositionLevels=" + maxDecompositionLevels +
                '}';
    }

    /**
     * Builder for TransformConfig.
     * Allows stepwise construction of configuration.
     */
    public static class Builder {
        // Default boundary mode is PERIODIC.
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        // Default: do not force scalar engine.
        private boolean forceScalar = false;
        // Default: auto-detect SIMD availability.
        private boolean forceSIMD = false;
        // Default: allow up to 20 decomposition levels (handles signals up to 2^20 = 1,048,576 samples).
        private int maxDecompositionLevels = 20;

        // Private constructor to enforce use via TransformConfig.
        private Builder() {
        }

        /**
         * Sets the boundary mode.
         *
         * @param boundaryMode boundary handling mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode boundaryMode) {
            this.boundaryMode = NullChecks.requireNonNull(boundaryMode, "boundaryMode");
            return this;
        }

        /**
         * Sets whether to force scalar engine.
         *
         * @param forceScalar true to force scalar engine
         * @return this builder
         */
        public Builder forceScalar(boolean forceScalar) {
            this.forceScalar = forceScalar;
            return this;
        }

        /**
         * Sets whether to force SIMD operations when available.
         * If SIMD is not available, this setting is ignored.
         *
         * @param forceSIMD true to force SIMD operations
         * @return this builder
         */
        public Builder forceSIMD(boolean forceSIMD) {
            this.forceSIMD = forceSIMD;
            return this;
        }

        /**
         * Sets the maximum decomposition levels.
         *
         * @param maxLevels maximum levels (must be >= 1)
         * @return this builder
         * @throws InvalidArgumentException if maxLevels < 1
         */
        public Builder maxDecompositionLevels(int maxLevels) {
            if (maxLevels < 1) {
                throw InvalidArgumentException.notPositive(maxLevels);
            }
            this.maxDecompositionLevels = maxLevels;
            return this;
        }

        /**
         * Builds the TransformConfig instance.
         *
         * @return new TransformConfig
         */
        public TransformConfig build() {
            return new TransformConfig(this);
        }
    }
}