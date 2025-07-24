package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.internal.VectorOps;
import ai.prophetizo.wavelet.internal.VectorOpsOptimized;
import ai.prophetizo.wavelet.internal.VectorOpsARM;

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
 * @since 1.3.0
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
     * Operations interface for wavelet transforms.
     */
    public interface WaveletOps {
        double[] convolveAndDownsample(double[] signal, double[] filter,
                                       int signalLength, int filterLength,
                                       BoundaryMode mode);

        double[] upsampleAndConvolve(double[] signal, double[] filter,
                                     int signalLength, int filterLength,
                                     BoundaryMode mode);

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
            // For now, fall back to regular vector ops for upsampling
            // TODO: Implement ARM-specific upsampling
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
            return "ARM Optimized (Apple Silicon)";
        }
    }
}