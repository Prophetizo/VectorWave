package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.streaming.OptimizedStreamingWaveletTransform;
import ai.prophetizo.wavelet.streaming.ResizableStreamingRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for adaptive buffer resizing performance.
 * 
 * Measures the overhead and benefits of dynamic buffer sizing.
 * 
 * Run with: ./jmh-runner.sh AdaptiveBufferBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AdaptiveBufferBenchmark {
    
    @Param({"256", "512"})
    private int blockSize;
    
    @Param({"fixed", "adaptive"})
    private String bufferMode;
    
    @Param({"steady", "bursty"})
    private String workloadPattern;
    
    private static final int TOTAL_SAMPLES = 100_000;
    private double[] steadyData;
    private double[][] burstyData;
    
    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(42);
        
        // Generate steady workload data
        steadyData = new double[TOTAL_SAMPLES];
        for (int i = 0; i < TOTAL_SAMPLES; i++) {
            steadyData[i] = random.nextGaussian();
        }
        
        // Generate bursty workload data
        burstyData = new double[100][];
        for (int i = 0; i < burstyData.length; i++) {
            // Alternate between small and large bursts
            int burstSize = (i % 2 == 0) ? 100 : 2000;
            burstyData[i] = new double[burstSize];
            for (int j = 0; j < burstSize; j++) {
                burstyData[i][j] = random.nextGaussian();
            }
        }
    }
    
    @Benchmark
    public void streamingTransformWithAdaptiveBuffer(Blackhole blackhole) throws Exception {
        Wavelet wavelet = Daubechies.DB4;
        boolean enableAdaptive = bufferMode.equals("adaptive");
        
        OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, blockSize, 0.0, 4, enableAdaptive
        );
        
        transform.subscribe(new BenchmarkSubscriber(blackhole));
        
        if (workloadPattern.equals("steady")) {
            // Process steady workload
            int chunkSize = 1000;
            for (int i = 0; i < steadyData.length; i += chunkSize) {
                int length = Math.min(chunkSize, steadyData.length - i);
                double[] chunk = new double[length];
                System.arraycopy(steadyData, i, chunk, 0, length);
                transform.process(chunk);
            }
        } else {
            // Process bursty workload
            for (double[] burst : burstyData) {
                transform.process(burst);
                // Simulate processing delay between bursts
                blackhole.consume(burst.length);
            }
        }
        
        transform.close();
        
        // Record final buffer multiplier for adaptive mode
        if (enableAdaptive) {
            blackhole.consume(transform.getCurrentBufferMultiplier());
        }
    }
    
    @Benchmark
    public void resizableBufferOverhead(Blackhole blackhole) {
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            blockSize * 4, blockSize / 2, blockSize / 4, 
            blockSize * 2, blockSize * 16
        );
        
        Random random = new Random(42);
        int operations = 10000;
        
        for (int i = 0; i < operations; i++) {
            // Write some data
            double value = random.nextDouble();
            buffer.write(value);
            
            // Periodically check utilization and resize
            if (i % 100 == 0) {
                double utilization = (double) buffer.available() / buffer.getCapacity();
                boolean resized = buffer.resizeBasedOnUtilization(utilization);
                blackhole.consume(resized);
            }
            
            // Read some data
            if (buffer.available() > blockSize / 2) {
                double[] temp = new double[blockSize / 4];
                int read = buffer.read(temp, 0, temp.length);
                blackhole.consume(read);
            }
        }
        
        blackhole.consume(buffer.getCapacity());
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void resizeOperationLatency(Blackhole blackhole) {
        ResizableStreamingRingBuffer buffer = new ResizableStreamingRingBuffer(
            1024, 128, 64, 512, 4096
        );
        
        // Fill buffer with some data
        double[] data = new double[800];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        buffer.write(data, 0, data.length);
        
        // Measure resize operation
        boolean result = buffer.forceResize(2048);
        blackhole.consume(result);
        blackhole.consume(buffer.available());
    }
    
    /**
     * Subscriber for consuming transform results.
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