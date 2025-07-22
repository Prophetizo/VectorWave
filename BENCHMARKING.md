# VectorWave Performance Benchmarking Guide

This guide explains how to run and interpret performance benchmarks for the VectorWave library using JMH (Java Microbenchmark Harness).

## Quick Start

Run all benchmarks:
```bash
./jmh-runner.sh
```

## Prerequisites

- Java 21+
- Maven 3.6+
- Sufficient heap memory (4GB+ recommended)

## Available Benchmarks

### 1. Signal Size Scaling

Measures performance across different signal sizes to understand scaling characteristics.

```bash
./jmh-runner.sh SignalSizeBenchmark
```

**Parameters:**
- Signal sizes: 256, 512, 1024, 2048, 4096, 8192, 16384
- Measures: Throughput and average time
- Includes cold-start and batch processing tests

### 2. Wavelet Type Comparison

Compares performance across different wavelet families.

```bash
./jmh-runner.sh WaveletTypeBenchmark
```

**Parameters:**
- Wavelet types: Haar, Daubechies-2 (DB2), Daubechies-4 (DB4)
- Measures: Transform time for each wavelet type
- Fixed signal size: 4096 samples

### 3. Validation Performance

Measures the overhead of input validation.

```bash
./jmh-runner.sh ValidationBenchmark
```

**Parameters:**
- Various validation scenarios
- Measures validation overhead in nanoseconds

### 4. Quick Performance Test

A lightweight benchmark for quick performance checks.

```bash
./jmh-runner.sh QuickPerformanceTest
```

**Parameters:**
- Limited iterations for fast results
- Good for regression testing

## JMH Parameters

Customize benchmark execution with these parameters:

```bash
# Example: Run with custom warmup and measurement iterations
./jmh-runner.sh -wi 10 -i 20

# Example: Run specific benchmark with fork count
./jmh-runner.sh SignalSizeBenchmark -f 3

# Example: Override parameter values
./jmh-runner.sh SignalSizeBenchmark -p signalSize=2048,8192
```

### Common Parameters:
- `-wi N`: Warmup iterations (default: 5)
- `-i N`: Measurement iterations (default: 10)
- `-f N`: Fork count (default: 2)
- `-t N`: Thread count (default: 1)
- `-p param=value`: Override @Param values
- `-rf format`: Result format (json, csv, text)
- `-rff file`: Result file

## Interpreting Results

### Throughput Metrics
- **ops/s**: Operations per second (higher is better)
- **ms/op**: Milliseconds per operation (lower is better)
- **samples/sec**: Signal samples processed per second

### Understanding Scores
```
Benchmark                       Mode  Cnt     Score    Error  Units
SignalSizeBenchmark.forward    thrpt   10  1234.567 ± 12.345  ops/s
                                           ^^^^^^^^^   ^^^^^^
                                           throughput  std dev
```

### Performance Guidelines

Expected performance characteristics:
- **Linear scaling**: Transform time should scale linearly with signal size
- **Wavelet complexity**: Haar < DB2 < DB4 in terms of computation time
- **Memory efficiency**: No significant GC pressure during normal operation

## Advanced Usage

### Running from Source

```bash
# Using the JMH runner script (recommended)
./jmh-runner.sh SignalSizeBenchmark

# Or manually compile and run
mvn test-compile
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
    org.openjdk.jmh.Main SignalSizeBenchmark
```

### Profiling Integration

```bash
# Run with async profiler
./jmh-runner.sh SignalSizeBenchmark -prof async

# Run with GC profiler
./jmh-runner.sh ValidationBenchmark -prof gc
```

### Custom Benchmark Development

Create new benchmarks by extending the base benchmark class:

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MyBenchmark {
    @Benchmark
    public void myTest() {
        // benchmark code
    }
}
```

## Troubleshooting

### Out of Memory
The JMH runner script already sets heap size, but you can modify it in the benchmark's @Fork annotation or run manually:
```bash
java -Xmx8G -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
    org.openjdk.jmh.Main SignalSizeBenchmark
```

### Inconsistent Results
- Ensure no other CPU-intensive processes are running
- Use more warmup iterations: `-wi 10`
- Increase fork count: `-f 3`

### Slow Execution
- Reduce iteration count for quick tests: `-wi 1 -i 1`
- Run specific benchmarks instead of all

## Best Practices

1. **Isolation**: Run benchmarks on a quiet system
2. **Warmup**: Allow sufficient warmup for JIT compilation
3. **Multiple runs**: Use forks to ensure consistency
4. **Baseline**: Always compare against a known baseline
5. **Documentation**: Record environment details with results