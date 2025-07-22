package ai.prophetizo.wavelet.config;

import ai.prophetizo.wavelet.api.BoundaryMode;

/**
 * Immutable configuration for wavelet transform.
 * Uses builder pattern for flexible construction.
 */
public final class TransformConfig {

    // Boundary handling mode for the transform.
    private final BoundaryMode boundaryMode;
    // Forces scalar engine if true, otherwise auto-detects.
    private final boolean forceScalar;
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
        this.maxDecompositionLevels = builder.maxDecompositionLevels;
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
            this.boundaryMode = java.util.Objects.requireNonNull(boundaryMode,
                    "boundaryMode cannot be null.");
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
         * Sets the maximum decomposition levels.
         *
         * @param maxLevels maximum levels (must be >= 1)
         * @return this builder
         * @throws IllegalArgumentException if maxLevels < 1
         */
        public Builder maxDecompositionLevels(int maxLevels) {
            if (maxLevels < 1) {
                throw new IllegalArgumentException(
                        "maxDecompositionLevels must be at least 1.");
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