package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for real-time application scenarios.
 * 
 * <p>This benchmark suite focuses on common real-time use cases:</p>
 * <ul>
 *   <li>Audio processing (small buffers, low latency requirements)</li>
 *   <li>Financial tick data (continuous stream processing)</li>
 *   <li>Sensor data filtering (embedded systems constraints)</li>
 *   <li>Real-time denoising (signal quality improvement)</li>
 * </ul>
 * 
 * <p>Run with: {@code ./jmh-runner.sh RealTimeBenchmark}</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 3, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UnlockDiagnosticVMOptions",
    "-Xms512M", "-Xmx512M",  // Smaller heap for embedded-like constraints
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=10"  // Low pause target for real-time
})
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class RealTimeBenchmark {

    // Audio processing parameters
    @Param({"64", "128", "256", "512"})  // Common audio buffer sizes
    private int audioBufferSize;
    
    // Financial data parameters
    private static final int TICK_BATCH_SIZE = 32;  // Typical tick batch
    
    // Sensor data parameters  
    private static final int SENSOR_WINDOW = 16;  // Small window for edge devices
    
    private double[] audioBuffer;
    private double[] tickBuffer;
    private double[] sensorBuffer;
    
    // Transforms for different scenarios
    private WaveletTransform audioTransform;
    private WaveletTransform tickTransform;
    private WaveletTransform sensorTransform;
    
    // Denoiser for real-time filtering
    private WaveletDenoiser denoiser;
    
    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(42);
        
        // Initialize audio buffer with synthetic audio signal
        audioBuffer = new double[audioBufferSize];
        for (int i = 0; i < audioBufferSize; i++) {
            // Simulate 440Hz tone with harmonics and noise
            audioBuffer[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / 44100) +
                           0.2 * Math.sin(2 * Math.PI * 880 * i / 44100) +
                           0.05 * (random.nextDouble() - 0.5);
        }
        
        // Initialize tick buffer with financial data pattern
        tickBuffer = new double[TICK_BATCH_SIZE];
        double price = 100.0;
        for (int i = 0; i < TICK_BATCH_SIZE; i++) {
            price *= (1 + 0.0001 * (random.nextGaussian()));
            tickBuffer[i] = Math.log(price / 100.0);  // Log returns
        }
        
        // Initialize sensor buffer with noisy sensor readings
        sensorBuffer = new double[SENSOR_WINDOW];
        for (int i = 0; i < SENSOR_WINDOW; i++) {
            sensorBuffer[i] = 20.0 + 0.1 * Math.sin(i * 0.5) + 
                            0.05 * random.nextGaussian();
        }
        
        // Configure transforms for different scenarios
        TransformConfig audioConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC)
            .build();
            
        TransformConfig tickConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)  // No lookahead
            .build();
            
        TransformConfig sensorConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC)
            .forceScalar(true)  // May be better for very small signals
            .build();
        
        // Use Haar for audio (fast), DB4 for finance (smoothness), DB2 for sensors (balance)
        audioTransform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC, audioConfig);
        tickTransform = new WaveletTransform(Daubechies.DB4, BoundaryMode.ZERO_PADDING, tickConfig);
        sensorTransform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC, sensorConfig);
        
        // Setup denoiser for real-time filtering
        denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
    }
    
    // ===== Audio Processing Benchmarks =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void audioProcessingLatency(Blackhole bh) {
        // Measure end-to-end latency for audio buffer processing
        TransformResult forward = audioTransform.forward(audioBuffer);
        
        // Simple high-frequency removal (low-pass filtering)
        double[] details = forward.detailCoeffs();
        for (int i = details.length / 2; i < details.length; i++) {
            details[i] *= 0.1;  // Attenuate high frequencies
        }
        
        double[] processed = audioTransform.inverse(forward);
        bh.consume(processed);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void audioProcessingThroughput(Blackhole bh) {
        // Measure samples per second throughput
        TransformResult result = audioTransform.forward(audioBuffer);
        double[] reconstructed = audioTransform.inverse(result);
        bh.consume(reconstructed);
    }
    
    // ===== Financial Tick Processing =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void tickDataProcessing(Blackhole bh) {
        // Process tick batch with minimal latency
        TransformResult result = tickTransform.forward(tickBuffer);
        
        // Extract trend (approximation) and noise (details)
        double[] trend = result.approximationCoeffs();
        double[] noise = result.detailCoeffs();
        
        // Simple volatility estimate from details
        double volatility = 0;
        for (double d : noise) {
            volatility += d * d;
        }
        volatility = Math.sqrt(volatility / noise.length);
        
        bh.consume(trend);
        bh.consume(volatility);
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 100)  // More iterations for percentile analysis
    public void tickDataLatencyPercentiles(Blackhole bh) {
        // Measure latency distribution for SLA compliance
        TransformResult result = tickTransform.forward(tickBuffer);
        bh.consume(result);
    }
    
    // ===== Sensor Data Filtering =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void sensorDataFiltering(Blackhole bh) {
        // Fast filtering for embedded systems
        TransformResult result = sensorTransform.forward(sensorBuffer);
        
        // Simple threshold denoising
        double[] details = result.detailCoeffs();
        double threshold = 0.01;
        for (int i = 0; i < details.length; i++) {
            if (Math.abs(details[i]) < threshold) {
                details[i] = 0;
            }
        }
        
        double[] filtered = sensorTransform.inverse(result);
        bh.consume(filtered);
    }
    
    // ===== Real-time Denoising =====
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void realTimeDenoising(Blackhole bh) {
        // Denoise audio buffer with automatic threshold
        double[] denoised = denoiser.denoise(audioBuffer, 
            ThresholdMethod.UNIVERSAL, ThresholdType.SOFT);
        bh.consume(denoised);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void adaptiveDenoising(Blackhole bh) {
        // Multi-level denoising with SURE threshold
        double[] denoised = denoiser.denoiseMultiLevel(
            tickBuffer, 3, ThresholdMethod.SURE, ThresholdType.SOFT);
        bh.consume(denoised);
    }
    
    // ===== Memory Allocation Benchmarks =====
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 1000)
    public void allocationOverhead(Blackhole bh) {
        // Measure allocation overhead for real-time constraints
        WaveletTransform transform = new WaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        double[] signal = new double[64];
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    // ===== Cache Effects Benchmarks =====
    
    private double[] coldBuffer;
    
    @Setup(Level.Invocation)
    public void setupCold() {
        // Create new buffer to simulate cold cache
        coldBuffer = new double[audioBufferSize];
        System.arraycopy(audioBuffer, 0, coldBuffer, 0, audioBufferSize);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void coldCacheLatency(Blackhole bh) {
        TransformResult result = audioTransform.forward(coldBuffer);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime) 
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 100)  // Ensure cache is warm
    public void warmCacheLatency(Blackhole bh) {
        TransformResult result = audioTransform.forward(audioBuffer);
        bh.consume(result);
    }
}