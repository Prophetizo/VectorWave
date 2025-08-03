package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.concurrent.ParallelWaveletEngine;
import ai.prophetizo.wavelet.internal.*;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.Random;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Comprehensive benchmark for all optimization strategies.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {
    "--add-modules=jdk.incubator.vector",
    "-XX:+UseParallelGC",
    "-Xms2g",
    "-Xmx2g"
})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class OptimizationBenchmark {
    
    @Param({"128", "1024", "8192", "65536"})
    private int signalSize;
    
    @Param({"haar", "db4", "sym4"})
    private String waveletType;
    
    private double[] signal;
    private double[][] batchSignals;
    private Wavelet wavelet;
    private MODWTTransform baselineTransform;
    private OptimizedTransformEngine optimizedEngine;
    private ParallelWaveletEngine parallelEngine;
    
    @Setup
    public void setup() {
        // Initialize signal
        signal = generateSignal(signalSize);
        
        // Initialize batch (16 signals)
        batchSignals = new double[16][signalSize];
        for (int i = 0; i < 16; i++) {
            batchSignals[i] = generateSignal(signalSize);
        }
        
        // Initialize wavelet
        wavelet = switch (waveletType) {
            case "haar" -> new Haar();
            case "db4" -> Daubechies.DB4;
            case "sym4" -> Symlet.SYM4;
            default -> throw new IllegalArgumentException("Unknown wavelet: " + waveletType);
        };
        
        // Initialize transforms
        baselineTransform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        optimizedEngine = new OptimizedTransformEngine();
        parallelEngine = new ParallelWaveletEngine();
        
        // Clear memory pool statistics
        AlignedMemoryPool.clear();
    }
    
    @TearDown
    public void tearDown() {
        if (parallelEngine != null) {
            parallelEngine.close();
        }
    }
    
    /**
     * Baseline: Standard scalar implementation.
     */
    @Benchmark
    public void baseline(Blackhole bh) {
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        // Scale filters for MODWT
        double scale = 1.0 / Math.sqrt(2.0);
        double[] scaledLowPass = new double[lowPass.length];
        double[] scaledHighPass = new double[highPass.length];
        for (int i = 0; i < lowPass.length; i++) {
            scaledLowPass[i] = lowPass[i] * scale;
            scaledHighPass[i] = highPass[i] * scale;
        }
        
        // MODWT produces same-length coefficients
        double[] approx = new double[signalSize];
        double[] detail = new double[signalSize];
        
        ScalarOps.circularConvolveMODWT(signal, scaledLowPass, approx);
        ScalarOps.circularConvolveMODWT(signal, scaledHighPass, detail);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Basic Vector API implementation - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void vectorBasic(Blackhole bh) {
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        double[] approx = VectorOps.convolveAndDownsamplePeriodic(
            signal, lowPass, signalSize, lowPass.length);
        double[] detail = VectorOps.convolveAndDownsamplePeriodic(
            signal, highPass, signalSize, highPass.length);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * ARM-optimized implementation - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void vectorARM(Blackhole bh) {
        if (!VectorOpsARM.isAppleSilicon()) {
            // Fall back to basic vector
            vectorBasic(bh);
            return;
        }
        
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        double[] approx = VectorOpsARM.convolveAndDownsampleARM(
            signal, lowPass, signalSize, lowPass.length);
        double[] detail = VectorOpsARM.convolveAndDownsampleARM(
            signal, highPass, signalSize, highPass.length);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Memory-pooled operations - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void memoryPooled(Blackhole bh) {
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        double[] approx = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            signal, lowPass, signalSize, lowPass.length);
        double[] detail = VectorOpsPooled.convolveAndDownsamplePeriodicPooled(
            signal, highPass, signalSize, highPass.length);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Specialized kernel - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void specializedKernel(Blackhole bh) {
        if (!waveletType.equals("db4") && !waveletType.equals("sym4") && !waveletType.equals("haar")) {
            // No specialized kernel, fall back
            vectorBasic(bh);
            return;
        }
        
        int halfLength = signalSize / 2;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        
        switch (waveletType) {
            case "db4" -> SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signalSize);
            case "sym4" -> SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signalSize);
            case "haar" -> {
                double[][] batch = {signal};
                double[][] approxBatch = {approx};
                double[][] detailBatch = {detail};
                SpecializedKernels.haarBatchOptimized(batch, approxBatch, detailBatch);
            }
        }
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Cache-aware implementation - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void cacheAware(Blackhole bh) {
        if (signalSize < 8192) {
            // Too small for cache blocking
            memoryPooled(bh);
            return;
        }
        
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        int halfLength = signalSize / 2;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        
        CacheAwareOps.forwardTransformBlocked(
            signal, approx, detail, lowPass, highPass, signalSize, lowPass.length);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Gather/scatter operations - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void gatherScatter(Blackhole bh) {
        if (!GatherScatterOps.isGatherScatterAvailable()) {
            // Fall back
            vectorBasic(bh);
            return;
        }
        
        double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
        double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
        
        double[] approx = GatherScatterOps.gatherPeriodicDownsample(
            signal, lowPass, signalSize, lowPass.length);
        double[] detail = GatherScatterOps.gatherPeriodicDownsample(
            signal, highPass, signalSize, highPass.length);
        
        bh.consume(approx);
        bh.consume(detail);
    }
    
    /**
     * Fully optimized engine (all optimizations).
     */
    @Benchmark
    public void fullyOptimized(Blackhole bh) {
        MODWTResult result = optimizedEngine.transform(signal, wavelet, BoundaryMode.PERIODIC);
        bh.consume(result);
    }
    
    /**
     * Batch processing - baseline.
     */
    @Benchmark
    public void batchBaseline(Blackhole bh) {
        MODWTResult[] results = new MODWTResult[16];
        for (int i = 0; i < 16; i++) {
            results[i] = baselineTransform.forward(batchSignals[i]);
        }
        bh.consume(results);
    }
    
    /**
     * Batch processing - SoA layout - DISABLED for MODWT migration.
     */
    // @Benchmark  // Disabled - no direct MODWT equivalent
    public void batchSoA(Blackhole bh) {
        if (!waveletType.equals("haar")) {
            // Use general SoA transform
            double[] lowPass = ((DiscreteWavelet) wavelet).lowPassDecomposition();
            double[] highPass = ((DiscreteWavelet) wavelet).highPassDecomposition();
            
            double[] soa = SoATransform.convertAoSToSoA(batchSignals);
            double[] soaApprox = new double[16 * signalSize / 2];
            double[] soaDetail = new double[16 * signalSize / 2];
            
            SoATransform.transformSoA(soa, soaApprox, soaDetail, 
                lowPass, highPass, 16, signalSize, lowPass.length);
            
            bh.consume(soaApprox);
            bh.consume(soaDetail);
        } else {
            // Use Haar SoA
            double[] soa = SoATransform.convertAoSToSoA(batchSignals);
            double[] soaApprox = new double[16 * signalSize / 2];
            double[] soaDetail = new double[16 * signalSize / 2];
            
            SoATransform.haarTransformSoA(soa, soaApprox, soaDetail, 16, signalSize);
            
            bh.consume(soaApprox);
            bh.consume(soaDetail);
        }
    }
    
    /**
     * Batch processing - parallel.
     */
    @Benchmark
    public void batchParallel(Blackhole bh) {
        MODWTResult[] results = parallelEngine.transformBatch(
            batchSignals, wavelet, BoundaryMode.PERIODIC);
        bh.consume(results);
    }
    
    /**
     * Batch processing - fully optimized.
     */
    @Benchmark
    public void batchOptimized(Blackhole bh) {
        MODWTResult[] results = optimizedEngine.transformBatch(
            batchSignals, wavelet, BoundaryMode.PERIODIC);
        bh.consume(results);
    }
    
    private double[] generateSignal(int size) {
        Random rand = new Random(TestConstants.TEST_SEED);
        double[] signal = new double[size];
        
        // Generate a signal with multiple frequency components
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                       0.5 * Math.cos(8 * Math.PI * i / 32.0) +
                       0.25 * Math.sin(16 * Math.PI * i / 32.0) +
                       0.1 * rand.nextGaussian();
        }
        
        return signal;
    }
}