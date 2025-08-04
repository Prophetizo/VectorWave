package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.AbstractStaticFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsARM;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;

/**
 * Factory for creating optimal wavelet operation implementations.
 *
 * <p>This factory automatically selects between scalar and SIMD-vectorized
 * implementations based on:</p>
 * <ul>
 *   <li>Signal size</li>
 *   <li>CPU capabilities</li>
 *   <li>Configuration preferences</li>
 * </ul>
 * 
 * <p>This factory supports two usage patterns:</p>
 * <ul>
 *   <li>Static methods for direct creation: {@code WaveletOpsFactory.create(config)}</li>
 *   <li>Factory interface pattern: {@code WaveletOpsFactory.getInstance().create(config)}</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Static method pattern
 * WaveletOps ops1 = WaveletOpsFactory.create(TransformConfig.defaultConfig());
 * 
 * // Factory interface pattern (for dependency injection)
 * Factory<WaveletOps, TransformConfig> factory = WaveletOpsFactory.getInstance();
 * WaveletOps ops2 = factory.create(config);
 * 
 * // Force specific implementation
 * TransformConfig scalarConfig = TransformConfig.builder()
 *     .forceScalar(true)
 *     .build();
 * WaveletOps scalarOps = factory.create(scalarConfig);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WaveletOpsFactory {

    // Singleton instances
    private static final WaveletOps SCALAR_OPS = new ScalarWaveletOps();
    private static final WaveletOps VECTOR_OPS = new VectorWaveletOps();
    private static final WaveletOps OPTIMIZED_VECTOR_OPS = new OptimizedVectorWaveletOps();
    private static final WaveletOps ARM_OPS = new ARMWaveletOps();
    // Platform detection
    private static final boolean VECTOR_API_AVAILABLE = checkVectorApiAvailable();
    private static final boolean IS_ARM_PLATFORM = VectorOpsARM.isARMPlatform();
    private static final boolean IS_APPLE_SILICON = VectorOpsARM.isAppleSilicon();

    private WaveletOpsFactory() {
        // Factory class
    }

    /**
     * Creates the optimal WaveletOps implementation based on configuration.
     *
     * @param config transform configuration
     * @return optimal implementation
     */
    public static WaveletOps create(TransformConfig config) {
        if (config != null && config.isForceScalarOperations()) {
            return SCALAR_OPS;
        }

        // Use ARM-optimized operations on Apple Silicon
        if (IS_APPLE_SILICON && VECTOR_API_AVAILABLE) {
            return ARM_OPS;
        }

        return VECTOR_API_AVAILABLE ? OPTIMIZED_VECTOR_OPS : SCALAR_OPS;
    }

    /**
     * Creates the optimal WaveletOps implementation with automatic selection.
     *
     * @return optimal implementation
     */
    public static WaveletOps createOptimal() {
        // Use ARM-optimized operations on Apple Silicon
        if (IS_APPLE_SILICON && VECTOR_API_AVAILABLE) {
            return ARM_OPS;
        }

        return VECTOR_API_AVAILABLE ? OPTIMIZED_VECTOR_OPS : SCALAR_OPS;
    }

    /**
     * Check if Vector API is available at runtime.
     */
    private static boolean checkVectorApiAvailable() {
        try {
            // Try to load Vector API classes
            Class.forName("jdk.incubator.vector.DoubleVector");

            // Check if we can actually use it (some platforms may not support it)
            return VectorOps.isVectorizedOperationBeneficial(128);
        } catch (ClassNotFoundException | NoClassDefFoundError | UnsupportedOperationException e) {
            // Vector API not available
            return false;
        }
    }

    /**
     * Gets information about available implementations.
     */
    public static String getAvailableImplementations() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available implementations:\n");
        sb.append("  - Scalar: Always available\n");
        if (VECTOR_API_AVAILABLE) {
            sb.append("  - Vector: ").append(VectorOps.getVectorInfo()).append("\n");
            if (IS_APPLE_SILICON) {
                sb.append("  - ARM Optimized: Available for Apple Silicon\n");
            }
        } else {
            sb.append("  - Vector: Not available (requires --add-modules jdk.incubator.vector)\n");
        }
        return sb.toString();
    }

    /**
     * Gets the factory instance that implements the common Factory interface.
     *
     * @return the factory instance
     */
    public static Instance getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Factory instance that implements the common Factory interface.
     * This provides an alternative way to use the factory that follows
     * the standardized factory pattern.
     */
    public static final class Instance extends AbstractStaticFactory<WaveletOps, TransformConfig> {
        private static final Instance INSTANCE = new Instance();

        private Instance() {
            // Singleton
        }

        @Override
        protected WaveletOps doCreate() {
            return WaveletOpsFactory.createOptimal();
        }

        @Override
        protected WaveletOps doCreate(TransformConfig config) {
            return WaveletOpsFactory.create(config);
        }

        @Override
        protected TransformConfig getDefaultConfiguration() {
            return TransformConfig.defaultConfig();
        }

        @Override
        public boolean isValidConfiguration(TransformConfig config) {
            // All non-null configs are valid
            return config != null;
        }

        @Override
        public String getDescription() {
            return "Factory for creating optimal wavelet operation implementations";
        }
    }

    /**
     * Operations interface for wavelet transforms.
     * 
     * <p>This interface defines the core mathematical operations required for
     * wavelet decomposition and reconstruction. Implementations may use
     * scalar operations, SIMD vectorization, or platform-specific optimizations.</p>
     * 
     * <p>The factory automatically selects the optimal implementation based on:</p>
     * <ul>
     *   <li>Hardware capabilities (AVX2, AVX512, ARM NEON)</li>
     *   <li>Signal size (SIMD benefits vary by data size)</li>
     *   <li>Platform (special optimizations for Apple Silicon)</li>
     * </ul>
     */
    public interface WaveletOps {
        /**
         * Performs convolution followed by downsampling (decimation by 2).
         * 
         * <p>This operation is used in the forward wavelet transform to compute
         * approximation and detail coefficients. The convolution handles boundary
         * conditions according to the specified mode.</p>
         * 
         * @param signal the input signal
         * @param filter the wavelet filter coefficients
         * @param signalLength the length of the input signal
         * @param filterLength the length of the filter
         * @param mode the boundary handling mode
         * @return the convolved and downsampled result
         */
        double[] convolveAndDownsample(double[] signal, double[] filter,
                                       int signalLength, int filterLength,
                                       BoundaryMode mode);

        /**
         * Performs upsampling (zero insertion) followed by convolution.
         * 
         * <p>This operation is used in the inverse wavelet transform to reconstruct
         * the signal from approximation and detail coefficients. Zeros are inserted
         * between samples before convolution.</p>
         * 
         * @param signal the input coefficients
         * @param filter the wavelet filter coefficients
         * @param signalLength the length of the input signal
         * @param filterLength the length of the filter
         * @param mode the boundary handling mode
         * @return the upsampled and convolved result
         */
        double[] upsampleAndConvolve(double[] signal, double[] filter,
                                     int signalLength, int filterLength,
                                     BoundaryMode mode);

        /**
         * Returns a description of the implementation type.
         * 
         * @return implementation type (e.g., "Scalar", "SIMD-AVX2", "ARM-NEON")
         */
        String getImplementationType();
    }

    /**
     * Scalar implementation of wavelet operations.
     */
    private static class ScalarWaveletOps implements WaveletOps {
        @Override
        public double[] convolveAndDownsample(double[] signal, double[] filter,
                                              int signalLength, int filterLength,
                                              BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> ScalarOps.convolveAndDownsamplePeriodic(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> ScalarOps.convolveAndDownsampleZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public double[] upsampleAndConvolve(double[] signal, double[] filter,
                                            int signalLength, int filterLength,
                                            BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> ScalarOps.upsampleAndConvolvePeriodic(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> ScalarOps.upsampleAndConvolveZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public String getImplementationType() {
            return "Scalar";
        }
    }

    /**
     * Vector API implementation of wavelet operations.
     */
    private static class VectorWaveletOps implements WaveletOps {
        @Override
        public double[] convolveAndDownsample(double[] signal, double[] filter,
                                              int signalLength, int filterLength,
                                              BoundaryMode mode) {
            // VectorOps already has internal fallback to scalar for small signals
            return switch (mode) {
                case PERIODIC -> VectorOps.convolveAndDownsamplePeriodic(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOps.convolveAndDownsampleZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public double[] upsampleAndConvolve(double[] signal, double[] filter,
                                            int signalLength, int filterLength,
                                            BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> VectorOps.upsampleAndConvolvePeriodic(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOps.upsampleAndConvolveZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public String getImplementationType() {
            return "Vector " + VectorOps.getVectorInfo();
        }
    }

    /**
     * Optimized Vector API implementation of wavelet operations.
     */
    private static class OptimizedVectorWaveletOps implements WaveletOps {
        @Override
        public double[] convolveAndDownsample(double[] signal, double[] filter,
                                              int signalLength, int filterLength,
                                              BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> VectorOpsOptimized.convolveAndDownsamplePeriodicOptimized(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOps.convolveAndDownsampleZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public double[] upsampleAndConvolve(double[] signal, double[] filter,
                                            int signalLength, int filterLength,
                                            BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> VectorOpsOptimized.upsampleAndConvolvePeriodicOptimized(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOps.upsampleAndConvolveZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public String getImplementationType() {
            return "Optimized " + VectorOpsOptimized.getVectorInfo();
        }
    }

    /**
     * ARM-optimized implementation of wavelet operations.
     */
    private static class ARMWaveletOps implements WaveletOps {
        @Override
        public double[] convolveAndDownsample(double[] signal, double[] filter,
                                              int signalLength, int filterLength,
                                              BoundaryMode mode) {
            return switch (mode) {
                case PERIODIC -> VectorOpsARM.convolveAndDownsampleARM(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOps.convolveAndDownsampleZeroPadding(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public double[] upsampleAndConvolve(double[] signal, double[] filter,
                                            int signalLength, int filterLength,
                                            BoundaryMode mode) {
            // Use ARM-optimized upsampling implementations
            return switch (mode) {
                case PERIODIC -> VectorOpsARM.upsampleAndConvolvePeriodicARM(
                        signal, filter, signalLength, filterLength);
                case ZERO_PADDING -> VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                        signal, filter, signalLength, filterLength);
                default -> throw new UnsupportedOperationException(
                        "Boundary mode " + mode + " is not yet implemented");
            };
        }

        @Override
        public String getImplementationType() {
            return "ARM Optimized (Apple Silicon)";
        }
    }
}