package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing FFM-based implementation with traditional array-based.
 * 
 * Run with: ./jmh-runner.sh FFMBenchmark
 * 
 * @since 2.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {
    "-Xms2G", "-Xmx2G",
    "--add-modules=jdk.incubator.vector",
    "--enable-native-access=ALL-UNNAMED",
    "-XX:+UseG1GC"
})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class FFMBenchmark {
    
    @Param({"64", "256", "1024", "4096", "16384"})
    private int signalSize;
    
    @Param({"Haar", "Daubechies4"})
    private String waveletType;
    
    private double[] signal;
    private Wavelet wavelet;
    
    // Traditional implementations
    private WaveletTransform traditionalTransform;
    private WaveletTransformPool pooledTransform;
    
    // FFM implementations
    private FFMWaveletTransform ffmTransform;
    private FFMMemoryPool ffmPool;
    
    @Setup(Level.Trial)
    public void setup() {
        // Generate test signal
        Random random = new Random(42);
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = random.nextGaussian();
        }
        
        // Setup wavelet
        wavelet = switch (waveletType) {
            case "Haar" -> new Haar();
            case "Daubechies4" -> Daubechies.DB4;
            default -> throw new IllegalArgumentException("Unknown wavelet: " + waveletType);
        };
        
        // Setup transforms
        traditionalTransform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        pooledTransform = new WaveletTransformPool(wavelet, BoundaryMode.PERIODIC);
        
        ffmPool = new FFMMemoryPool();
        ffmTransform = new FFMWaveletTransform(wavelet, BoundaryMode.PERIODIC, ffmPool, null);
        
        // Pre-warm FFM pool
        ffmPool.prewarm(signalSize / 2, signalSize);
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        pooledTransform.clearPool();
        ffmTransform.close();
    }
    
    @Benchmark
    public void traditionalForward(Blackhole bh) {
        TransformResult result = traditionalTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void pooledForward(Blackhole bh) {
        TransformResult result = pooledTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void ffmForward(Blackhole bh) {
        TransformResult result = ffmTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void traditionalForwardInverse(Blackhole bh) {
        TransformResult forward = traditionalTransform.forward(signal);
        double[] inverse = traditionalTransform.inverse(forward);
        bh.consume(inverse);
    }
    
    @Benchmark
    public void pooledForwardInverse(Blackhole bh) {
        double[] result = pooledTransform.forwardInverse(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void ffmForwardInverse(Blackhole bh) {
        double[] result = ffmTransform.forwardInverse(signal);
        bh.consume(result);
    }
    
    /**
     * Benchmark memory allocation patterns
     */
    @State(Scope.Thread)
    public static class AllocationBenchmark {
        @Param({"1024", "4096", "16384"})
        private int arraySize;
        
        private FFMMemoryPool ffmPool;
        
        @Setup
        public void setup() {
            ffmPool = new FFMMemoryPool();
        }
        
        @TearDown
        public void tearDown() {
            ffmPool.close();
        }
        
        @Benchmark
        public void traditionalArrayAllocation(Blackhole bh) {
            double[] array = new double[arraySize];
            bh.consume(array);
        }
        
        @Benchmark
        public void ffmSegmentAllocation(Blackhole bh) {
            var segment = ffmPool.acquire(arraySize);
            bh.consume(segment);
            ffmPool.release(segment);
        }
    }
    
    /**
     * Benchmark streaming performance
     */
    @State(Scope.Thread)
    public static class StreamingBenchmark {
        @Param({"256", "1024", "4096"})
        private int blockSize;
        
        private double[] streamData;
        private FFMStreamingTransform ffmStreaming;
        private Wavelet wavelet;
        
        @Setup
        public void setup() {
            wavelet = new Haar();
            streamData = new double[blockSize * 10];
            Random random = new Random(42);
            for (int i = 0; i < streamData.length; i++) {
                streamData[i] = random.nextGaussian();
            }
            
            ffmStreaming = new FFMStreamingTransform(wavelet, blockSize);
        }
        
        @TearDown
        public void tearDown() {
            ffmStreaming.close();
        }
        
        @Benchmark
        public void ffmStreamingProcess(Blackhole bh) {
            // Process chunks
            for (int i = 0; i < streamData.length; i += blockSize) {
                ffmStreaming.processChunk(streamData, i, blockSize);
                if (ffmStreaming.hasCompleteBlock()) {
                    TransformResult result = ffmStreaming.getNextResult();
                    bh.consume(result);
                }
            }
        }
    }
}