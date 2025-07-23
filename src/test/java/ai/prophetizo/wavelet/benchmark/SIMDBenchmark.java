package ai.prophetizo.wavelet.benchmark;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks comparing scalar vs SIMD performance for wavelet transforms.
 * 
 * <p>Run with: {@code ./jmh-runner.sh SIMDBenchmark}</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+PrintInlining",
    "-XX:+PrintCompilation",
    "-XX:+LogCompilation",
    "-XX:LogFile=hotspot.log"
})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class SIMDBenchmark {

    @Param({"64", "128", "256", "512", "1024", "2048", "4096"})
    private int signalSize;
    
    @Param({"PERIODIC", "ZERO_PADDING"})
    private String boundaryMode;
    
    private double[] signal;
    private double[] financialSignal;
    private WaveletTransform scalarTransform;
    private WaveletTransform simdTransform;
    private WaveletTransform autoTransform;
    
    // Different wavelets for testing
    private Haar haar;
    private Daubechies db2;
    private Daubechies db4;
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize wavelets
        haar = new Haar();
        db2 = Daubechies.DB2;
        db4 = Daubechies.DB4;
        
        // Create synthetic signal
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Create financial-like signal (random walk with trend)
        financialSignal = new double[signalSize];
        financialSignal[0] = 100.0;
        double trend = 0.0001;
        double volatility = 0.01;
        for (int i = 1; i < signalSize; i++) {
            double randomShock = (Math.random() - 0.5) * volatility;
            financialSignal[i] = financialSignal[i-1] * (1 + trend + randomShock);
        }
        
        // Create transforms
        BoundaryMode mode = BoundaryMode.valueOf(boundaryMode);
        
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .boundaryMode(mode)
            .build();
            
        TransformConfig simdConfig = TransformConfig.builder()
            .forceSIMD(true)
            .boundaryMode(mode)
            .build();
        
        scalarTransform = new WaveletTransform(haar, mode, scalarConfig);
        simdTransform = new WaveletTransform(haar, mode, simdConfig);
        autoTransform = new WaveletTransform(haar, mode);
    }
    
    // ===== Haar wavelet benchmarks =====
    
    @Benchmark
    public void haarScalarForward(Blackhole bh) {
        TransformResult result = scalarTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void haarSIMDForward(Blackhole bh) {
        TransformResult result = simdTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void haarAutoForward(Blackhole bh) {
        TransformResult result = autoTransform.forward(signal);
        bh.consume(result);
    }
    
    // ===== DB2 wavelet benchmarks =====
    
    @Benchmark
    public void db2ScalarForward(Blackhole bh) {
        WaveletTransform transform = new WaveletTransform(db2, BoundaryMode.valueOf(boundaryMode),
            TransformConfig.builder().forceScalar(true).build());
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    public void db2SIMDForward(Blackhole bh) {
        WaveletTransform transform = new WaveletTransform(db2, BoundaryMode.valueOf(boundaryMode),
            TransformConfig.builder().forceSIMD(true).build());
        TransformResult result = transform.forward(signal);
        bh.consume(result);
    }
    
    // ===== Financial signal benchmarks =====
    
    @Benchmark
    public void financialScalarForward(Blackhole bh) {
        TransformResult result = scalarTransform.forward(financialSignal);
        bh.consume(result);
    }
    
    @Benchmark
    public void financialSIMDForward(Blackhole bh) {
        TransformResult result = simdTransform.forward(financialSignal);
        bh.consume(result);
    }
    
    // ===== Round-trip (forward + inverse) benchmarks =====
    
    @Benchmark
    public void roundTripScalar(Blackhole bh) {
        TransformResult forward = scalarTransform.forward(signal);
        double[] reconstructed = scalarTransform.inverse(forward);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    public void roundTripSIMD(Blackhole bh) {
        TransformResult forward = simdTransform.forward(signal);
        double[] reconstructed = simdTransform.inverse(forward);
        bh.consume(reconstructed);
    }
    
    // ===== Throughput benchmark =====
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputScalar(Blackhole bh) {
        TransformResult result = scalarTransform.forward(signal);
        bh.consume(result);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputSIMD(Blackhole bh) {
        TransformResult result = simdTransform.forward(signal);
        bh.consume(result);
    }
}