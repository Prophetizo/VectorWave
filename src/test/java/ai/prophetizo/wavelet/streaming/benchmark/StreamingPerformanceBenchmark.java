package ai.prophetizo.wavelet.streaming.benchmark;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransformImpl;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing traditional array-copying approach vs ring buffer streaming approach.
 * 
 * This benchmark validates the performance improvements claimed in the issue:
 * - 50% reduction in memory bandwidth
 * - Lower latency for streaming
 * - Better scalability
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class StreamingPerformanceBenchmark {
    
    @Param({"256", "512", "1024", "2048"})
    private int windowSize;
    
    @Param({"0.25", "0.5", "0.75"})
    private double overlapRatio;
    
    private StreamingWaveletTransformImpl streamingTransform;
    private WaveletTransform traditionalTransform;
    private double[] sampleBatch;
    private int overlapSize;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        overlapSize = (int) (windowSize * overlapRatio);
        streamingTransform = new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, windowSize, overlapSize);
        traditionalTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Prepare sample batch - use hop size for realistic streaming
        int hopSize = windowSize - overlapSize;
        sampleBatch = new double[hopSize];
        for (int i = 0; i < hopSize; i++) {
            sampleBatch[i] = Math.sin(2 * Math.PI * i / hopSize);
        }
    }
    
    @Setup(Level.Invocation)
    public void setupInvocation() {
        // Reset streaming transform state
        streamingTransform.reset();
        
        // Fill streaming transform with initial window
        double[] initialWindow = new double[windowSize];
        for (int i = 0; i < windowSize; i++) {
            initialWindow[i] = Math.sin(2 * Math.PI * i / windowSize);
        }
        streamingTransform.addSamples(initialWindow);
        
        // Get first result to establish steady state
        if (streamingTransform.isResultReady()) {
            streamingTransform.getNextResult();
        }
    }
    
    /**
     * Benchmark the new streaming approach using ring buffer.
     * This represents the optimized zero-copy approach.
     */
    @Benchmark
    public TransformResult streamingApproach() {
        // Add new samples (simulating streaming data)
        streamingTransform.addSamples(sampleBatch);
        
        // Get result if ready
        if (streamingTransform.isResultReady()) {
            return streamingTransform.getNextResult();
        }
        
        return null; // Shouldn't happen in steady state
    }
    
    /**
     * Benchmark the traditional approach that would require array copying.
     * This simulates the old approach with manual overlap management.
     */
    @Benchmark
    public TransformResult traditionalApproach() {
        // Simulate traditional overlap buffer with array copying
        return traditionalArrayCopyingApproach(sampleBatch);
    }
    
    /**
     * Simulates the traditional approach with array copying for overlap management.
     * This represents what the old implementation would do.
     */
    private TransformResult traditionalArrayCopyingApproach(double[] newSamples) {
        // This would be the previous approach: maintain overlap by copying arrays
        double[] window = new double[windowSize];
        
        // Simulate overlap by copying previous data (expensive operation)
        // In reality, this would come from a previous buffer
        for (int i = 0; i < overlapSize; i++) {
            window[i] = Math.sin(2 * Math.PI * (i + newSamples.length) / windowSize);
        }
        
        // Copy new samples
        System.arraycopy(newSamples, 0, window, overlapSize, newSamples.length);
        
        // Perform transform
        return traditionalTransform.forward(window);
    }
    
    /**
     * Benchmark memory allocation patterns.
     * Measures the overhead of creating new arrays vs reusing ring buffer.
     */
    @Benchmark
    public long memoryAllocationOverhead() {
        long startMemory = getUsedMemory();
        
        // Traditional approach: create new arrays
        double[] overlap = new double[overlapSize];
        double[] window = new double[windowSize];
        System.arraycopy(sampleBatch, 0, overlap, 0, Math.min(sampleBatch.length, overlapSize));
        System.arraycopy(overlap, 0, window, 0, overlapSize);
        
        long endMemory = getUsedMemory();
        return endMemory - startMemory;
    }
    
    /**
     * Benchmark ring buffer reuse patterns.
     * Measures the efficiency of ring buffer reuse vs array allocation.
     */
    @Benchmark
    public long ringBufferReuseEfficiency() {
        long startMemory = getUsedMemory();
        
        // Ring buffer approach: reuse existing buffer
        streamingTransform.addSamples(sampleBatch);
        if (streamingTransform.isResultReady()) {
            streamingTransform.getNextResult();
        }
        
        long endMemory = getUsedMemory();
        return endMemory - startMemory;
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}