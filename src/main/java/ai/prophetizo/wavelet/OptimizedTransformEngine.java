package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine;
import ai.prophetizo.wavelet.internal.*;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool.PooledArray;

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
 * @since 1.4.0
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
    public TransformResult transform(double[] signal, Wavelet wavelet, BoundaryMode mode) {
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
        WaveletTransform transform = new WaveletTransform(wavelet, mode);
        return transform.forward(signal);
    }
    
    /**
     * Batch transform with automatic optimization selection.
     */
    public TransformResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode) {
        if (signals.length == 0) {
            return new TransformResult[0];
        }
        
        // Use parallel engine for large batches
        if (parallelEngine != null && signals.length >= 16) {
            return parallelEngine.transformBatch(signals, wavelet, mode);
        }
        
        // Use SoA layout for medium batches
        if (useSoALayout && signals.length >= 4 && signals.length <= 16) {
            return transformBatchSoA(signals, wavelet, mode);
        }
        
        // Use pooled batch operations
        if (useMemoryPool) {
            return transformBatchPooled(signals, wavelet, mode);
        }
        
        // Sequential processing
        TransformResult[] results = new TransformResult[signals.length];
        WaveletTransform transform = new WaveletTransform(wavelet, mode);
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        return results;
    }
    
    /**
     * Asynchronous batch transform.
     */
    public CompletableFuture<TransformResult[]> transformBatchAsync(
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
    private TransformResult transformWithSpecializedKernel(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {
        
        int length = signal.length;
        int halfLength = length / 2;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        
        if (mode != BoundaryMode.PERIODIC) {
            // Fall back to regular transform for non-periodic boundaries
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            return transform.forward(signal);
        }
        
        // Use specialized kernels
        String waveletName = wavelet.name().toLowerCase();
        
        if (waveletName.equals("db4")) {
            SpecializedKernels.db4ForwardOptimized(signal, approx, detail, length);
        } else if (waveletName.equals("sym4")) {
            SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, length);
        } else if (waveletName.equals("haar")) {
            // Use vectorized Haar
            double[][] batch = {signal};
            double[][] approxBatch = {approx};
            double[][] detailBatch = {detail};
            SpecializedKernels.haarBatchOptimized(batch, approxBatch, detailBatch);
        } else {
            // No specialized kernel, fall back
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            return transform.forward(signal);
        }
        
        return TransformResult.create(approx, detail);
    }
    
    /**
     * Transform with cache-aware blocking.
     */
    private TransformResult transformCacheAware(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {
        
        if (!(wavelet instanceof DiscreteWavelet)) {
            // Fall back for continuous wavelets
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            return transform.forward(signal);
        }
        
        DiscreteWavelet dw = (DiscreteWavelet) wavelet;
        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();
        
        int halfLength = signal.length / 2;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        
        if (mode == BoundaryMode.PERIODIC) {
            CacheAwareOps.forwardTransformBlocked(
                signal, approx, detail, lowPass, highPass, 
                signal.length, lowPass.length
            );
        } else {
            // Fall back for non-periodic
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            return transform.forward(signal);
        }
        
        return TransformResult.create(approx, detail);
    }
    
    /**
     * Transform using pooled memory.
     */
    private TransformResult transformPooled(
            double[] signal, Wavelet wavelet, BoundaryMode mode) {
        
        if (!(wavelet instanceof DiscreteWavelet)) {
            // Fall back for continuous wavelets
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            return transform.forward(signal);
        }
        
        DiscreteWavelet dw = (DiscreteWavelet) wavelet;
        double[] filter = dw.lowPassDecomposition();
        
        if (mode == BoundaryMode.PERIODIC) {
            double[] approx = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
                signal, filter, signal.length, filter.length
            );
            
            // Compute detail coefficients
            double[] highPass = dw.highPassDecomposition();
            double[] detail = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
                signal, highPass, signal.length, highPass.length
            );
            
            return TransformResult.create(approx, detail);
        }
        
        // Fall back for non-periodic
        WaveletTransform transform = new WaveletTransform(wavelet, mode);
        return transform.forward(signal);
    }
    
    /**
     * Batch transform using Structure-of-Arrays layout.
     */
    private TransformResult[] transformBatchSoA(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {
        
        if (!(wavelet instanceof DiscreteWavelet) || mode != BoundaryMode.PERIODIC) {
            // Fall back to sequential processing to avoid infinite recursion
            TransformResult[] results = new TransformResult[signals.length];
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            for (int i = 0; i < signals.length; i++) {
                results[i] = transform.forward(signals[i]);
            }
            return results;
        }
        
        DiscreteWavelet dw = (DiscreteWavelet) wavelet;
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        int halfLength = signalLength / 2;
        
        // Convert to SoA layout
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * halfLength];
        double[] soaDetail = new double[numSignals * halfLength];
        
        // Transform in SoA layout
        if (wavelet.name().equals("haar")) {
            SoATransform.haarTransformSoA(
                soaInput, soaApprox, soaDetail, numSignals, signalLength
            );
        } else {
            double[] lowPass = dw.lowPassDecomposition();
            double[] highPass = dw.highPassDecomposition();
            SoATransform.transformSoA(
                soaInput, soaApprox, soaDetail, lowPass, highPass,
                numSignals, signalLength, lowPass.length
            );
        }
        
        // Convert back to AoS and create results
        TransformResult[] results = new TransformResult[numSignals];
        double[][] approxAoS = new double[numSignals][halfLength];
        double[][] detailAoS = new double[numSignals][halfLength];
        
        SoATransform.convertSoAToAoS(soaApprox, approxAoS);
        SoATransform.convertSoAToAoS(soaDetail, detailAoS);
        
        for (int i = 0; i < numSignals; i++) {
            results[i] = TransformResult.create(approxAoS[i], detailAoS[i]);
        }
        
        return results;
    }
    
    /**
     * Batch transform using pooled memory.
     */
    private TransformResult[] transformBatchPooled(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {
        
        if (!(wavelet instanceof DiscreteWavelet) || mode != BoundaryMode.PERIODIC) {
            // Fall back to regular batch processing
            TransformResult[] results = new TransformResult[signals.length];
            WaveletTransform transform = new WaveletTransform(wavelet, mode);
            for (int i = 0; i < signals.length; i++) {
                results[i] = transform.forward(signals[i]);
            }
            return results;
        }
        
        DiscreteWavelet dw = (DiscreteWavelet) wavelet;
        double[] lowPass = dw.lowPassDecomposition();
        double[] highPass = dw.highPassDecomposition();
        
        // Process batch with pooled memory
        double[][] approxResults = VectorOpsPooled.batchConvolveAndDownsample(
            signals, lowPass, signals[0].length, lowPass.length
        );
        double[][] detailResults = VectorOpsPooled.batchConvolveAndDownsample(
            signals, highPass, signals[0].length, highPass.length
        );
        
        // Create results
        TransformResult[] results = new TransformResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = TransformResult.create(
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