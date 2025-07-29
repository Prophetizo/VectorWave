package ai.prophetizo.wavelet.memory.benchmark;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.memory.*;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing heap vs off-heap memory management performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Disabled // Disabled by default to avoid long CI times
public class MemoryManagementBenchmark {

    private double[] smallSignal;
    private double[] largeSignal;
    private ArrayFactoryManager heapManager;
    private ArrayFactoryManager offHeapManager;
    private ManagedWaveletTransform heapTransform;
    private ManagedWaveletTransform offHeapTransform;

    @Setup
    public void setup() {
        // Create test signals
        smallSignal = createRandomSignal(256);
        largeSignal = createRandomSignal(4096);
        
        // Create factory managers
        heapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.HEAP_ONLY);
        offHeapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.OFF_HEAP_ONLY);
        
        // Create transforms
        heapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, heapManager);
        offHeapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, offHeapManager);
    }

    @TearDown
    public void tearDown() {
        if (heapTransform != null) heapTransform.close();
        if (offHeapTransform != null) offHeapTransform.close();
        if (heapManager != null) heapManager.close();
        if (offHeapManager != null) offHeapManager.close();
    }

    @Benchmark
    public double[] heapSmallArrayOperations() {
        try (ManagedTransformResult result = heapTransform.forwardManaged(smallSignal);
             ManagedArray reconstructed = heapTransform.inverseManaged(result)) {
            return reconstructed.toArray();
        }
    }

    @Benchmark
    public double[] offHeapSmallArrayOperations() {
        try (ManagedTransformResult result = offHeapTransform.forwardManaged(smallSignal);
             ManagedArray reconstructed = offHeapTransform.inverseManaged(result)) {
            return reconstructed.toArray();
        }
    }

    @Benchmark
    public double[] heapLargeArrayOperations() {
        try (ManagedTransformResult result = heapTransform.forwardManaged(largeSignal);
             ManagedArray reconstructed = heapTransform.inverseManaged(result)) {
            return reconstructed.toArray();
        }
    }

    @Benchmark
    public double[] offHeapLargeArrayOperations() {
        try (ManagedTransformResult result = offHeapTransform.forwardManaged(largeSignal);
             ManagedArray reconstructed = offHeapTransform.inverseManaged(result)) {
            return reconstructed.toArray();
        }
    }

    @Benchmark
    public void heapArrayCreation() {
        try (ManagedArray array = heapManager.createAligned(1024, 32)) {
            array.fill(42.0);
        }
    }

    @Benchmark
    public void offHeapArrayCreation() {
        try (ManagedArray array = offHeapManager.createAligned(1024, 32)) {
            array.fill(42.0);
        }
    }

    @Benchmark
    public void heapArrayCopy() {
        try (ManagedArray src = heapManager.from(smallSignal);
             ManagedArray dest = heapManager.create(smallSignal.length)) {
            src.copyTo(dest, 0, 0, smallSignal.length);
        }
    }

    @Benchmark
    public void offHeapArrayCopy() {
        try (ManagedArray src = offHeapManager.from(smallSignal);
             ManagedArray dest = offHeapManager.create(smallSignal.length)) {
            src.copyTo(dest, 0, 0, smallSignal.length);
        }
    }

    private static double[] createRandomSignal(int size) {
        double[] signal = new double[size];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MemoryManagementBenchmark.class.getSimpleName())
                .jvmArgs("--enable-preview")
                .build();

        new Runner(opt).run();
    }
}