package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.streaming.OptimizedStreamingWaveletTransform;
import ai.prophetizo.wavelet.streaming.RingBuffer;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransform;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for optimized streaming wavelet transform performance.
 * 
 * Measures the impact of:
 * - Batch processing in RingBuffer
 * - SIMD integration for transforms
 * - Memory prefetching
 * - Adaptive buffer sizing
 * 
 * Run with: ./jmh-runner.sh OptimizedStreamingBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--add-modules=jdk.incubator.vector"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class OptimizedStreamingBenchmark {
    
    @Param({"256", "512", "1024"})
    private int blockSize;
    
    @Param({"Haar", "DB4"})
    private String waveletType;
    
    @Param({"true", "false"})
    private boolean useOptimizations;
    
    private static final int BATCH_SIZE = 16;
    private static final int NUM_SAMPLES = 100000;
    
    private double[] testData;
    private double[][] testBatches;
    private RingBuffer ringBuffer;
    private Wavelet wavelet;
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize wavelet
        wavelet = waveletType.equals("Haar") ? new Haar() : Daubechies.DB4;
        
        // Generate test data
        Random random = new Random(42);
        testData = new double[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            testData[i] = random.nextGaussian();
        }
        
        // Create batches for batch processing benchmark
        int numBatches = NUM_SAMPLES / BATCH_SIZE;
        testBatches = new double[numBatches][BATCH_SIZE];
        for (int i = 0; i < numBatches; i++) {
            System.arraycopy(testData, i * BATCH_SIZE, testBatches[i], 0, BATCH_SIZE);
        }
        
        // Create ring buffer
        ringBuffer = new RingBuffer(blockSize * 8);
    }
    
    @Benchmark
    public void batchWriteVsIndividual(Blackhole blackhole) {
        ringBuffer.clear();
        
        if (useOptimizations) {
            // Use batch write
            for (int i = 0; i < testBatches.length && !ringBuffer.isFull(); i++) {
                int written = ringBuffer.writeBatch(new double[][] { testBatches[i] });
                blackhole.consume(written);
            }
        } else {
            // Use individual writes
            for (double value : testData) {
                if (!ringBuffer.write(value)) {
                    break;
                }
            }
        }
        
        blackhole.consume(ringBuffer.available());
    }
    
    @Benchmark
    public void streamingTransformThroughput(Blackhole blackhole) throws Exception {
        StreamingWaveletTransform transform;
        
        if (useOptimizations) {
            // Use optimized version with SIMD and other improvements
            transform = new OptimizedStreamingWaveletTransform(
                wavelet, BoundaryMode.PERIODIC, blockSize, 0.5, 8
            );
        } else {
            // Use baseline version
            transform = StreamingWaveletTransform.create(
                wavelet, BoundaryMode.PERIODIC, blockSize
            );
        }
        
        // Subscribe to consume results
        transform.subscribe(new BenchmarkSubscriber(blackhole));
        
        // Process data in chunks
        int chunkSize = blockSize * 10;
        for (int i = 0; i < testData.length; i += chunkSize) {
            int length = Math.min(chunkSize, testData.length - i);
            double[] chunk = new double[length];
            System.arraycopy(testData, i, chunk, 0, length);
            transform.process(chunk);
        }
        
        transform.close();
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void singleBlockLatency(Blackhole blackhole) throws Exception {
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize
        );
        
        // Subscribe to consume results
        transform.subscribe(new BenchmarkSubscriber(blackhole));
        
        // Process exactly one block
        double[] block = new double[blockSize];
        System.arraycopy(testData, 0, block, 0, blockSize);
        transform.process(block);
        transform.flush();
        
        transform.close();
    }
    
    @State(Scope.Thread)
    public static class AdaptiveBufferState {
        OptimizedStreamingWaveletTransform adaptiveTransform;
        double[] streamData;
        
        @Setup(Level.Invocation)
        public void setup() {
            adaptiveTransform = new OptimizedStreamingWaveletTransform(
                new Haar(), BoundaryMode.PERIODIC, 256, 0.0, 4
            );
            
            // Generate streaming data
            Random random = new Random(42);
            streamData = new double[10000];
            for (int i = 0; i < streamData.length; i++) {
                streamData[i] = random.nextGaussian();
            }
        }
        
        @TearDown(Level.Invocation)
        public void tearDown() {
            if (adaptiveTransform != null) {
                adaptiveTransform.close();
            }
        }
    }
    
    @Benchmark
    public void adaptiveBufferOverhead(AdaptiveBufferState state, Blackhole blackhole) {
        state.adaptiveTransform.subscribe(new BenchmarkSubscriber(blackhole));
        
        // Simulate varying throughput
        for (int i = 0; i < 10; i++) {
            state.adaptiveTransform.process(state.streamData);
            
            // Simulate processing delay
            blackhole.consume(state.adaptiveTransform.getCurrentBufferMultiplier());
        }
    }
    
    /**
     * Subscriber that consumes results for benchmarking.
     */
    private static class BenchmarkSubscriber implements Flow.Subscriber<TransformResult> {
        private final Blackhole blackhole;
        private Flow.Subscription subscription;
        
        BenchmarkSubscriber(Blackhole blackhole) {
            this.blackhole = blackhole;
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(TransformResult item) {
            blackhole.consume(item);
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }
        
        @Override
        public void onComplete() {
            // Nothing to do
        }
    }
}