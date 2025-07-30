package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.streaming.OptimizedStreamingWaveletTransform;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing the performance of streaming wavelet transform implementations.
 * 
 * <p>This benchmark measures:</p>
 * <ul>
 *   <li>Throughput of zero-copy vs traditional implementation</li>
 *   <li>Memory bandwidth utilization</li>
 *   <li>Effect of different overlap factors</li>
 *   <li>Impact of buffer size configurations</li>
 * </ul>
 * 
 * <p>Run with: {@code ./jmh-runner.sh StreamingTransformBenchmark}</p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--add-modules=jdk.incubator.vector"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class StreamingTransformBenchmark {
    
    @Param({"256", "512", "1024"})
    private int blockSize;
    
    @Param({"0.0", "0.5", "0.75"})
    private double overlapFactor;
    
    @Param({"4", "8"})
    private int bufferMultiplier;
    
    private static final int DATA_SIZE = 1024 * 1024; // 1M samples
    private double[] testData;
    
    private OptimizedStreamingWaveletTransform zeroCopyTransform;
    
    @Setup(Level.Trial)
    public void setup() {
        // Generate test data
        Random random = new Random(42);
        testData = new double[DATA_SIZE];
        for (int i = 0; i < DATA_SIZE; i++) {
            testData[i] = random.nextGaussian();
        }
        
        Wavelet wavelet = new Haar();
        
        // Create zero-copy transform
        zeroCopyTransform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, overlapFactor, bufferMultiplier
        );
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        if (zeroCopyTransform != null) {
            zeroCopyTransform.close();
        }
    }
    
    @Benchmark
    public void zeroCopyTransform(Blackhole blackhole) {
        // Subscribe to consume results
        zeroCopyTransform.subscribe(new BenchmarkSubscriber(blackhole));
        
        // Process all data
        zeroCopyTransform.process(testData);
        zeroCopyTransform.flush();
    }
    
    
    @Benchmark
    public void zeroCopyStreamProcessing(Blackhole blackhole) {
        // Subscribe to consume results
        zeroCopyTransform.subscribe(new BenchmarkSubscriber(blackhole));
        
        // Process data in streaming fashion (simulating real-time input)
        int chunkSize = 4096;
        for (int i = 0; i < testData.length; i += chunkSize) {
            int length = Math.min(chunkSize, testData.length - i);
            double[] chunk = new double[length];
            System.arraycopy(testData, i, chunk, 0, length);
            zeroCopyTransform.process(chunk);
        }
        zeroCopyTransform.flush();
    }
    
    /**
     * Benchmark subscriber that consumes results to prevent dead code elimination.
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