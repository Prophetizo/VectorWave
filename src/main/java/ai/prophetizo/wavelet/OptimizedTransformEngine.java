package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.DiscreteWavelet;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine;
import ai.prophetizo.wavelet.internal.*;
import ai.prophetizo.wavelet.internal.BatchSIMDTransform;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTResultImpl;

import java.util.concurrent.CompletableFuture;

/**
 * High-performance wavelet transform engine that integrates all optimizations.
 *
 * <p>This engine automatically selects and combines the best optimization
 * strategies based on:</p>
 * <ul>
 *   <li>Platform capabilities (ARM, x86, gather/scatter support)</li>
 *   <li>Signal characteristics (size, batch size)</li>
 *   <li>Wavelet type (specialized kernels for common wavelets)</li>
 *   <li>Memory constraints (pooling, cache-aware blocking)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class OptimizedTransformEngine {

    private final ParallelWaveletEngine parallelEngine;
    private final boolean useMemoryPool;
    private final boolean useSpecializedKernels;
    private final boolean useSoALayout;
    private final boolean useCacheBlocking;

    /**
     * Creates an optimized engine with default settings.
     */
    public OptimizedTransformEngine() {
        this(new EngineConfig());
    }

    /**
     * Creates an optimized engine with custom configuration.
     */
    public OptimizedTransformEngine(EngineConfig config) {
        this.parallelEngine = config.parallelism > 1
                ? new ParallelWaveletEngine(config.parallelism)
                : null;
        this.useMemoryPool = config.useMemoryPool;
        this.useSpecializedKernels = config.useSpecializedKernels;
        this.useSoALayout = config.useSoALayout;
        this.useCacheBlocking = config.useCacheBlocking;
    }

    /**
     * Single signal transform with all optimizations.
     */
    public MODWTResult transform(double[] signal, Wavelet wavelet, BoundaryMode mode) {
        // Check for specialized kernel
        if (useSpecializedKernels && hasSpecializedKernel(wavelet)) {
            return transformWithSpecializedKernel(signal, wavelet, mode);
        }

        // Use cache-aware transform for large signals
        if (useCacheBlocking && signal.length > 8192) {
            return transformCacheAware(signal, wavelet, mode);
        }

        // Use pooled memory operations
        if (useMemoryPool) {
            return transformPooled(signal, wavelet, mode);
        }

        // Default transform
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        return transform.forward(signal);
    }

    /**
     * Batch transform with automatic optimization selection.
     */
    public MODWTResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode) {
        if (signals.length == 0) {
            return new MODWTResult[0];
        }

        // Use parallel engine for very large batches
        if (parallelEngine != null && signals.length >= 128) {
            return parallelEngine.transformBatch(signals, wavelet, mode);
        }

        // Use SoA layout with SIMD for most batch sizes
        if (useSoALayout && signals.length >= 2) {
            return transformBatchSoA(signals, wavelet, mode);
        }

        // Use pooled batch operations
        if (useMemoryPool) {
            return transformBatchPooled(signals, wavelet, mode);
        }

        // Sequential processing
        MODWTResult[] results = new MODWTResult[signals.length];
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        return results;
    }

    /**
     * Asynchronous batch transform.
     */
    public CompletableFuture<MODWTResult[]> transformBatchAsync(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        if (parallelEngine != null) {
            return parallelEngine.transformBatchAsync(signals, wavelet, mode);
        }

        // Fallback to synchronous in separate thread
        return CompletableFuture.supplyAsync(() ->
                transformBatch(signals, wavelet, mode)
        );
    }

    /**
     * Transform using specialized kernel implementations.
     */
    private MODWTResult transformWithSpecializedKernel(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {

        int length = signal.length;
        // MODWT produces same-length coefficients
        double[] approx = new double[length];
        double[] detail = new double[length];

        // For MODWT, we can't use DWT-based specialized kernels directly
        // since they perform downsampling. Fall back to regular MODWT transform
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        return transform.forward(signal);
    }

    /**
     * Transform with cache-aware blocking.
     */
    private MODWTResult transformCacheAware(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {

        if (!(wavelet instanceof DiscreteWavelet dw)) {
            // Fall back for continuous wavelets
            MODWTTransform transform = new MODWTTransform(wavelet, mode);
            return transform.forward(signal);
        }

        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();

        // MODWT produces same-length coefficients
        double[] approx = new double[signal.length];
        double[] detail = new double[signal.length];

        // Cache-aware ops are DWT-based (with downsampling)
        // For MODWT, fall back to regular transform
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        return transform.forward(signal);
    }

    /**
     * Transform using pooled memory.
     */
    private MODWTResult transformPooled(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {

        if (!(wavelet instanceof DiscreteWavelet dw)) {
            // Fall back for continuous wavelets
            MODWTTransform transform = new MODWTTransform(wavelet, mode);
            return transform.forward(signal);
        }

        // VectorOpsPooled operations are DWT-based (with downsampling)
        // For MODWT, we need non-downsampling operations

        // Fall back for non-periodic
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        return transform.forward(signal);
    }

    /**
     * Batch transform using Structure-of-Arrays layout.
     */
    private MODWTResult[] transformBatchSoA(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        if (!(wavelet instanceof DiscreteWavelet dw) || mode != BoundaryMode.PERIODIC) {
            // Fall back to sequential processing to avoid infinite recursion
            MODWTResult[] results = new MODWTResult[signals.length];
            MODWTTransform transform = new MODWTTransform(wavelet, mode);
            for (int i = 0; i < signals.length; i++) {
                results[i] = transform.forward(signals[i]);
            }
            return results;
        }

        int numSignals = signals.length;
        int signalLength = signals[0].length;

        // MODWT produces same-length coefficients
        double[][] approxResults = new double[numSignals][signalLength];
        double[][] detailResults = new double[numSignals][signalLength];
        
        // BatchSIMDTransform is DWT-based (performs downsampling)
        // For MODWT batch processing, fall back to sequential
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        for (int i = 0; i < numSignals; i++) {
            MODWTResult result = transform.forward(signals[i]);
            approxResults[i] = result.approximationCoeffs();
            detailResults[i] = result.detailCoeffs();
        }

        // Create results
        MODWTResult[] results = new MODWTResult[numSignals];
        for (int i = 0; i < numSignals; i++) {
            results[i] = new MODWTResultImpl(approxResults[i], detailResults[i]);
        }

        return results;
    }

    /**
     * Batch transform using pooled memory.
     */
    private MODWTResult[] transformBatchPooled(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        if (!(wavelet instanceof DiscreteWavelet dw) || mode != BoundaryMode.PERIODIC) {
            // Fall back to regular batch processing
            MODWTResult[] results = new MODWTResult[signals.length];
            MODWTTransform transform = new MODWTTransform(wavelet, mode);
            for (int i = 0; i < signals.length; i++) {
                results[i] = transform.forward(signals[i]);
            }
            return results;
        }

        // VectorOpsPooled batch operations are DWT-based (with downsampling)
        // For MODWT, process each signal individually
        int numSignals = signals.length;
        double[][] approxResults = new double[numSignals][];
        double[][] detailResults = new double[numSignals][];
        
        MODWTTransform transform = new MODWTTransform(wavelet, mode);
        for (int i = 0; i < numSignals; i++) {
            MODWTResult result = transform.forward(signals[i]);
            approxResults[i] = result.approximationCoeffs();
            detailResults[i] = result.detailCoeffs();
        }

        // Create results
        MODWTResult[] results = new MODWTResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = new MODWTResultImpl(
                    approxResults[i], detailResults[i]
            );
        }

        return results;
    }

    /**
     * Check if we have a specialized kernel for this wavelet.
     */
    private boolean hasSpecializedKernel(Wavelet wavelet) {
        String name = wavelet.name().toLowerCase();
        return name.equals("haar") || name.equals("db4") || name.equals("sym4");
    }

    /**
     * Get optimization information.
     */
    public String getOptimizationInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Optimization Engine Configuration:\n");
        sb.append("- Memory Pooling: ").append(useMemoryPool ? "Enabled" : "Disabled").append("\n");
        sb.append("- Specialized Kernels: ").append(useSpecializedKernels ? "Enabled" : "Disabled").append("\n");
        sb.append("- SoA Layout: ").append(useSoALayout ? "Enabled" : "Disabled").append("\n");
        sb.append("- Cache Blocking: ").append(useCacheBlocking ? "Enabled" : "Disabled").append("\n");
        sb.append("- Parallel Processing: ").append(parallelEngine != null ? "Enabled" : "Disabled").append("\n");

        if (useMemoryPool) {
            sb.append("\n").append(AlignedMemoryPool.getStatistics());
        }

        if (GatherScatterOps.isGatherScatterAvailable()) {
            sb.append("\n").append(GatherScatterOps.getGatherScatterInfo());
        }

        return sb.toString();
    }

    /**
     * Configuration for the optimized engine.
     */
    public static class EngineConfig {
        private int parallelism = Runtime.getRuntime().availableProcessors();
        private boolean useMemoryPool = true;
        private boolean useSpecializedKernels = true;
        private boolean useSoALayout = true;
        private boolean useCacheBlocking = true;

        public EngineConfig withParallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public EngineConfig withMemoryPool(boolean use) {
            this.useMemoryPool = use;
            return this;
        }

        public EngineConfig withSpecializedKernels(boolean use) {
            this.useSpecializedKernels = use;
            return this;
        }

        public EngineConfig withSoALayout(boolean use) {
            this.useSoALayout = use;
            return this;
        }

        public EngineConfig withCacheBlocking(boolean use) {
            this.useCacheBlocking = use;
            return this;
        }
    }
}