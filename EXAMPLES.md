# VectorWave Examples

This document provides comprehensive examples of using VectorWave's MODWT (Maximal Overlap Discrete Wavelet Transform) and other features.

## Table of Contents

1. [Basic MODWT Usage](#basic-modwt-usage)
2. [Multi-Level MODWT](#multi-level-modwt)
3. [Real-time Streaming](#real-time-streaming)
4. [Signal Denoising](#signal-denoising)
5. [Financial Analysis](#financial-analysis)
6. [Batch Processing](#batch-processing)
7. [Pattern Detection](#pattern-detection)
8. [Memory Optimization](#memory-optimization)
9. [CWT Examples](#cwt-examples)

## Basic MODWT Usage

### Simple Forward and Inverse Transform

```java
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.api.*;

// Create MODWT transform with Haar wavelet
MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Works with ANY signal length - no power-of-2 restriction!
double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // length 7

// Forward transform
MODWTResult result = modwt.forward(signal);

// Access coefficients (same length as input)
double[] approx = result.approximationCoeffs(); // length 7
double[] detail = result.detailCoeffs();       // length 7

// Perfect reconstruction
double[] reconstructed = modwt.inverse(result);

// Verify reconstruction error
double maxError = 0;
for (int i = 0; i < signal.length; i++) {
    maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
}
System.out.println("Max reconstruction error: " + maxError); // ~1e-15
```

### Using Different Wavelets

```java
// Daubechies wavelets
MODWTTransform db4 = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
MODWTTransform db8 = new MODWTTransform(Daubechies.DB8, BoundaryMode.PERIODIC);

// Symlets
MODWTTransform sym4 = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);

// Coiflets
MODWTTransform coif2 = new MODWTTransform(Coiflet.COIF2, BoundaryMode.PERIODIC);

// Biorthogonal
MODWTTransform bior = new MODWTTransform(BiorthogonalSpline.BIOR1_3, BoundaryMode.PERIODIC);

// Process signal of arbitrary length
double[] ecgSignal = loadECGData(); // length 3333
MODWTResult result = db4.forward(ecgSignal);
```

## Multi-Level MODWT

### Decompose Signal into Multiple Levels

```java
// Create multi-level MODWT
MultiLevelMODWTTransform mlModwt = new MultiLevelMODWTTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC);

// Signal of arbitrary length
double[] signal = generateSignal(777); // Any length!

// Determine maximum decomposition levels
int maxLevels = mlModwt.getMaxDecompositionLevel(signal.length);
System.out.println("Max levels for signal of length " + signal.length + ": " + maxLevels);

// Perform 4-level decomposition
MultiLevelMODWTResult mlResult = mlModwt.forward(signal, 4);

// Access coefficients at each level
for (int level = 1; level <= 4; level++) {
    double[] details = mlResult.getDetailCoeffsAtLevel(level);
    System.out.println("Level " + level + " details length: " + details.length);
    // All have same length as original signal!
}

// Get final approximation
double[] finalApprox = mlResult.getApproximationCoeffs();

// Reconstruct
double[] reconstructed = mlModwt.inverse(mlResult);
```

### Time-Frequency Analysis

```java
// Analyze chirp signal (frequency increases with time)
int length = 1000;
double[] chirp = new double[length];
for (int i = 0; i < length; i++) {
    double t = (double) i / length;
    double frequency = 5 + 20 * t; // 5 Hz to 25 Hz
    chirp[i] = Math.sin(2 * Math.PI * frequency * t * length / 100);
}

MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC);

// Decompose into 5 levels
MultiLevelMODWTResult result = mwt.forward(chirp, 5);

// Analyze energy distribution across scales
for (int level = 1; level <= 5; level++) {
    double[] details = result.getDetailCoeffsAtLevel(level);
    double energy = 0;
    for (double d : details) energy += d * d;
    
    // Find temporal location of maximum energy
    int maxIndex = 0;
    double maxLocalEnergy = 0;
    int windowSize = 10;
    
    for (int i = 0; i <= details.length - windowSize; i++) {
        double localEnergy = 0;
        for (int j = 0; j < windowSize; j++) {
            localEnergy += details[i + j] * details[i + j];
        }
        if (localEnergy > maxLocalEnergy) {
            maxLocalEnergy = localEnergy;
            maxIndex = i;
        }
    }
    
    System.out.printf("Level %d: Energy=%.2f, Peak at t=%.2f%%\n",
        level, energy, 100.0 * maxIndex / details.length);
}
```

## Real-time Streaming

### Basic Streaming Transform

```java
// Create streaming MODWT transform
MODWTStreamingTransform stream = new MODWTStreamingTransformImpl(
    new Haar(),
    BoundaryMode.PERIODIC,
    480 // Exactly 10ms at 48kHz - no padding needed!
);

// Subscribe to results
stream.subscribe(new Flow.Subscriber<MODWTResult>() {
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onNext(MODWTResult result) {
        // Process each block's coefficients
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // Detect features, denoise, etc.
        processCoefficients(approx, detail);
    }
    
    @Override
    public void onError(Throwable throwable) {
        System.err.println("Stream error: " + throwable);
    }
    
    @Override
    public void onComplete() {
        System.out.println("Stream completed");
    }
});

// Feed data to stream
for (double[] chunk : audioStream) {
    stream.onNext(chunk);
}

// Clean up
stream.close();
```

### Streaming Denoiser

```java
// Configure streaming denoiser
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(333)  // Any size - perfect for 44.1kHz audio blocks
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
    .noiseWindowSize(1000)
    .build();

// Process noisy audio stream
double totalNoise = 0;
int blockCount = 0;

for (double[] noisyBlock : audioStream) {
    // Denoise block
    double[] cleanBlock = denoiser.denoise(noisyBlock);
    
    // Track noise level
    double noiseLevel = denoiser.getEstimatedNoiseLevel();
    totalNoise += noiseLevel;
    blockCount++;
    
    // Output clean audio
    audioOutput.write(cleanBlock);
}

System.out.printf("Average noise level: %.4f\n", totalNoise / blockCount);
System.out.printf("Samples processed: %d\n", denoiser.getSamplesProcessed());

denoiser.close();
```

## Signal Denoising

### Basic Denoising Example

```java
// Generate noisy signal
double[] clean = new double[500];
for (int i = 0; i < clean.length; i++) {
    clean[i] = Math.sin(2 * Math.PI * i / 32) + 
               0.5 * Math.sin(2 * Math.PI * i / 8);
}

// Add noise
Random rng = new Random(42);
double[] noisy = new double[clean.length];
double noiseLevel = 0.2;
for (int i = 0; i < clean.length; i++) {
    noisy[i] = clean[i] + noiseLevel * rng.nextGaussian();
}

// Denoise using MODWT
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(clean.length)
    .thresholdMethod(ThresholdMethod.SURE)
    .thresholdType(ThresholdType.SOFT)
    .build();

double[] denoised = denoiser.denoise(noisy);

// Calculate SNR improvement
double noisePower = 0, denoisedError = 0;
for (int i = 0; i < clean.length; i++) {
    noisePower += Math.pow(noisy[i] - clean[i], 2);
    denoisedError += Math.pow(denoised[i] - clean[i], 2);
}

double snrImprovement = 10 * Math.log10(noisePower / denoisedError);
System.out.printf("SNR improvement: %.1f dB\n", snrImprovement);
```

### Adaptive Denoising

```java
// Process signal with varying noise levels
MODWTStreamingDenoiser adaptiveDenoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(128)
    .thresholdMethod(ThresholdMethod.MINIMAX)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.ADAPTIVE)
    .noiseWindowSize(512)
    .build();

// Simulate varying noise conditions
for (int segment = 0; segment < 10; segment++) {
    double segmentNoise = 0.05 + 0.15 * Math.abs(Math.sin(segment * 0.5));
    
    double[] block = new double[128];
    for (int i = 0; i < block.length; i++) {
        block[i] = Math.sin(2 * Math.PI * i / 16) + 
                   segmentNoise * rng.nextGaussian();
    }
    
    double[] denoised = adaptiveDenoiser.denoise(block);
    System.out.printf("Segment %d: Estimated noise=%.3f (actual=%.3f)\n",
        segment, adaptiveDenoiser.getEstimatedNoiseLevel(), segmentNoise);
}
```

## Financial Analysis

### Stock Price Analysis

```java
// Load stock price data (arbitrary length)
double[] prices = loadStockPrices("AAPL", "2023-01-01", "2024-01-01");
System.out.println("Price data length: " + prices.length); // e.g., 252 trading days

// Convert to returns
double[] returns = new double[prices.length - 1];
for (int i = 0; i < returns.length; i++) {
    returns[i] = Math.log(prices[i + 1] / prices[i]);
}

// Multi-resolution analysis
MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC);

MultiLevelMODWTResult result = mwt.forward(returns, 4);

// Analyze volatility at different time scales
System.out.println("Volatility Analysis by Time Scale:");
for (int level = 1; level <= 4; level++) {
    double[] details = result.getDetailCoeffsAtLevel(level);
    
    // Calculate volatility (standard deviation)
    double sum = 0, sumSq = 0;
    for (double d : details) {
        sum += d;
        sumSq += d * d;
    }
    double mean = sum / details.length;
    double volatility = Math.sqrt(sumSq / details.length - mean * mean);
    
    int days = (int) Math.pow(2, level);
    System.out.printf("Scale %d (~%d days): Volatility = %.4f\n",
        level, days, volatility);
}

// Detect market regime changes
double[] level3Details = result.getDetailCoeffsAtLevel(3);
for (int i = 10; i < level3Details.length - 10; i++) {
    // Calculate local energy
    double localEnergy = 0;
    for (int j = -10; j <= 10; j++) {
        localEnergy += level3Details[i + j] * level3Details[i + j];
    }
    
    if (localEnergy > 0.01) { // Threshold for regime change
        System.out.printf("Potential regime change at day %d\n", i);
    }
}
```

### High-Frequency Trading Analysis

```java
// Analyze tick data with MODWT
double[] tickPrices = loadTickData("EUR/USD", date);
int ticksPerMinute = 120; // 2 ticks per second average

// Use MODWT for microstructure noise filtering
MODWTStreamingDenoiser noiseFilter = new MODWTStreamingDenoiser.Builder()
    .wavelet(new Haar()) // Fast for HFT
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(ticksPerMinute) // 1-minute windows
    .thresholdMethod(ThresholdMethod.MINIMAX)
    .thresholdType(ThresholdType.HARD)
    .build();

List<Double> filteredPrices = new ArrayList<>();
List<Double> microstructureNoise = new ArrayList<>();

// Process in 1-minute blocks
for (int i = 0; i < tickPrices.length; i += ticksPerMinute) {
    int blockSize = Math.min(ticksPerMinute, tickPrices.length - i);
    double[] block = Arrays.copyOfRange(tickPrices, i, i + blockSize);
    
    double[] filtered = noiseFilter.denoise(block);
    
    // Extract microstructure noise
    for (int j = 0; j < blockSize; j++) {
        filteredPrices.add(filtered[j]);
        microstructureNoise.add(block[j] - filtered[j]);
    }
}

// Analyze noise characteristics
double avgNoise = microstructureNoise.stream()
    .mapToDouble(Math::abs)
    .average()
    .orElse(0.0);

System.out.printf("Average microstructure noise: %.5f (%.2f bps)\n",
    avgNoise, avgNoise * 10000);
```

## Batch Processing

### SIMD-Optimized Batch Transform

```java
// Process multiple signals simultaneously
int numSignals = 64;
int signalLength = 333; // Any length with MODWT!

double[][] signals = new double[numSignals][signalLength];

// Generate test signals
for (int i = 0; i < numSignals; i++) {
    for (int j = 0; j < signalLength; j++) {
        signals[i][j] = Math.sin(2 * Math.PI * j / (10 + i)) + 
                        0.1 * Math.random();
    }
}

// Batch transform using MODWT
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

long startTime = System.nanoTime();
MODWTResult[] results = transform.forwardBatch(signals);
long batchTime = System.nanoTime() - startTime;

// Compare with sequential processing
startTime = System.nanoTime();
MODWTResult[] seqResults = new MODWTResult[numSignals];
for (int i = 0; i < numSignals; i++) {
    seqResults[i] = transform.forward(signals[i]);
}
long seqTime = System.nanoTime() - startTime;

System.out.printf("Batch processing: %.2f ms\n", batchTime / 1e6);
System.out.printf("Sequential: %.2f ms\n", seqTime / 1e6);
System.out.printf("Speedup: %.2fx\n", (double) seqTime / batchTime);

// Batch reconstruction
double[][] reconstructed = transform.inverseBatch(results);
```

### Multi-Channel Audio Processing

```java
// Process stereo audio with MODWT
int sampleRate = 48000;
int blockSize = 480; // Exactly 10ms blocks
int channels = 2;

// Create denoiser for each channel
MODWTStreamingDenoiser[] denoisers = new MODWTStreamingDenoiser[channels];
for (int ch = 0; ch < channels; ch++) {
    denoisers[ch] = new MODWTStreamingDenoiser.Builder()
        .wavelet(Daubechies.DB4)
        .boundaryMode(BoundaryMode.PERIODIC)
        .bufferSize(blockSize)
        .thresholdMethod(ThresholdMethod.UNIVERSAL)
        .build();
}

// Process audio blocks
double[][] audioBlock = new double[channels][blockSize];
while (readAudioBlock(audioBlock)) {
    // Process each channel
    for (int ch = 0; ch < channels; ch++) {
        audioBlock[ch] = denoisers[ch].denoise(audioBlock[ch]);
    }
    
    // Write processed audio
    writeAudioBlock(audioBlock);
    
    // Monitor noise levels
    if (frameCount % 100 == 0) {
        System.out.printf("Frame %d - L: %.4f, R: %.4f\n",
            frameCount,
            denoisers[0].getEstimatedNoiseLevel(),
            denoisers[1].getEstimatedNoiseLevel());
    }
}
```

## Pattern Detection

### Shift-Invariant Pattern Detection

```java
// MODWT's shift-invariance is perfect for pattern detection
double[] signal = loadSensorData(); // Length 1234

// Create pattern to search for
double[] pattern = {1.0, 2.0, 3.0, 2.0, 1.0, 0.0, -1.0, -2.0, -1.0, 0.0};

// Embed pattern at various locations
signal[100] = pattern[0]; // ... copy pattern
signal[567] = pattern[0]; // ... copy pattern at different location

// Use MODWT for detection
MODWTTransform modwt = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

// Transform signal and shifted versions
MODWTResult result1 = modwt.forward(signal);

// Shift signal by 5 samples
double[] shiftedSignal = new double[signal.length];
System.arraycopy(signal, 5, shiftedSignal, 0, signal.length - 5);
System.arraycopy(signal, 0, shiftedSignal, signal.length - 5, 5);

MODWTResult result2 = modwt.forward(shiftedSignal);

// Compare coefficients - they should be shifted versions of each other
double[] detail1 = result1.detailCoeffs();
double[] detail2 = result2.detailCoeffs();

// Detect pattern locations using detail coefficients
List<Integer> detectedLocations = new ArrayList<>();
double threshold = computeThreshold(detail1);

for (int i = pattern.length; i < detail1.length - pattern.length; i++) {
    double localEnergy = 0;
    for (int j = 0; j < pattern.length; j++) {
        localEnergy += detail1[i - pattern.length/2 + j] * 
                       detail1[i - pattern.length/2 + j];
    }
    
    if (localEnergy > threshold) {
        detectedLocations.add(i);
    }
}

System.out.println("Pattern detected at: " + detectedLocations);
```

### Anomaly Detection

```java
// Real-time anomaly detection with MODWT
MODWTTransform modwt = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

// Sliding window parameters
int windowSize = 64;
int stride = 16;
double[] signal = loadTimeSeriesData(); // Any length

// Baseline statistics from normal data
double[] normalData = Arrays.copyOfRange(signal, 0, 1000);
MODWTResult normalResult = modwt.forward(normalData);
double[] normalDetail = normalResult.detailCoeffs();

// Calculate threshold based on normal behavior
double mean = Arrays.stream(normalDetail).average().orElse(0);
double std = Math.sqrt(Arrays.stream(normalDetail)
    .map(x -> Math.pow(x - mean, 2))
    .average().orElse(0));
double anomalyThreshold = mean + 3 * std;

// Detect anomalies using sliding window
List<Integer> anomalies = new ArrayList<>();
for (int start = 1000; start <= signal.length - windowSize; start += stride) {
    double[] window = Arrays.copyOfRange(signal, start, start + windowSize);
    MODWTResult result = modwt.forward(window);
    
    // Check for anomalous patterns in detail coefficients
    double maxDetail = Arrays.stream(result.detailCoeffs())
        .map(Math::abs)
        .max()
        .orElse(0);
    
    if (maxDetail > anomalyThreshold) {
        anomalies.add(start + windowSize/2);
        System.out.printf("Anomaly detected at position %d (score: %.2f)\n",
            start + windowSize/2, maxDetail / anomalyThreshold);
    }
}
```

## Memory Optimization

### Using Memory Pools

```java
// Efficient memory management for repeated transforms
MemoryPool pool = new MemoryPool();
pool.setMaxArraysPerSize(20);

// Process large dataset efficiently
List<double[]> dataset = loadLargeDataset(); // 10,000 signals of varying lengths

MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
List<MODWTResult> results = new ArrayList<>();

long startTime = System.currentTimeMillis();

for (double[] signal : dataset) {
    // Borrow arrays from pool
    double[] workspace = pool.borrowArray(signal.length);
    
    try {
        // Copy signal to workspace
        System.arraycopy(signal, 0, workspace, 0, signal.length);
        
        // Transform
        MODWTResult result = transform.forward(workspace);
        results.add(result);
        
    } finally {
        // Always return arrays to pool
        pool.returnArray(workspace);
    }
}

long elapsed = System.currentTimeMillis() - startTime;

// Print statistics
System.out.printf("Processed %d signals in %d ms\n", dataset.size(), elapsed);
pool.printStatistics();
System.out.printf("Memory saved by pooling: %.1f%%\n", pool.getHitRate() * 100);
```

### Streaming Large Files

```java
// Process large files without loading into memory
Path largefile = Paths.get("sensor_data.bin");
long fileSize = Files.size(largefile);
int blockSize = 4096; // Process in 4KB blocks

MODWTStreamingTransform stream = new MODWTStreamingTransformImpl(
    Daubechies.DB4, BoundaryMode.PERIODIC, blockSize / 8); // doubles

// Statistics collectors
AtomicLong samplesProcessed = new AtomicLong();
AtomicDouble totalEnergy = new AtomicDouble();

stream.subscribe(new Flow.Subscriber<MODWTResult>() {
    @Override
    public void onNext(MODWTResult result) {
        double[] details = result.detailCoeffs();
        double energy = Arrays.stream(details)
            .map(d -> d * d)
            .sum();
        
        totalEnergy.addAndGet(energy);
        samplesProcessed.addAndGet(details.length);
    }
    // ... other methods
});

// Stream file data
try (RandomAccessFile file = new RandomAccessFile(largefile.toFile(), "r");
     FileChannel channel = file.getChannel()) {
    
    ByteBuffer buffer = ByteBuffer.allocateDirect(blockSize);
    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
    
    while (channel.read(buffer) != -1) {
        buffer.flip();
        
        double[] data = new double[doubleBuffer.remaining()];
        doubleBuffer.get(data);
        
        stream.onNext(data);
        
        buffer.clear();
        doubleBuffer = buffer.asDoubleBuffer();
    }
}

stream.close();

System.out.printf("Processed %.2f MB of data\n", fileSize / 1048576.0);
System.out.printf("Total samples: %d\n", samplesProcessed.get());
System.out.printf("Average energy: %.4f\n", 
    totalEnergy.get() / samplesProcessed.get());
```

## CWT Examples

### Basic CWT Analysis

```java
// Continuous Wavelet Transform for time-frequency analysis
MorletWavelet wavelet = new MorletWavelet();
CWTTransform cwt = new CWTTransform(wavelet);

// Generate test signal with changing frequency
double[] signal = new double[1000];
for (int i = 0; i < signal.length; i++) {
    double t = i / 1000.0;
    // Chirp: frequency increases from 10 to 50 Hz
    double freq = 10 + 40 * t;
    signal[i] = Math.sin(2 * Math.PI * freq * t * 1000);
}

// Define scales (corresponds to frequencies)
double[] scales = new double[50];
for (int i = 0; i < scales.length; i++) {
    scales[i] = 1 + i * 2; // Scales from 1 to 99
}

// Perform CWT
CWTResult result = cwt.analyze(signal, scales);

// Find ridge (maximum coefficient at each time)
int[] ridge = new int[signal.length];
for (int t = 0; t < signal.length; t++) {
    double maxCoeff = 0;
    int maxScale = 0;
    
    for (int s = 0; s < scales.length; s++) {
        double coeff = Math.abs(result.getCoefficient(s, t));
        if (coeff > maxCoeff) {
            maxCoeff = coeff;
            maxScale = s;
        }
    }
    ridge[t] = maxScale;
}

// Convert ridge to instantaneous frequency
double samplingRate = 1000; // Hz
for (int t = 0; t < signal.length; t += 100) {
    double scale = scales[ridge[t]];
    double frequency = wavelet.getCenterFrequency() * samplingRate / scale;
    System.out.printf("Time %.1fs: Frequency = %.1f Hz\n", 
        t / 1000.0, frequency);
}
```

### Complex CWT for Phase Analysis

```java
// Complex CWT for phase information
ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);

// Extract magnitude and phase
double[][] magnitude = complexResult.getMagnitude();
double[][] phase = complexResult.getPhase();

// Compute instantaneous frequency from phase
double[][] instFreq = complexResult.getInstantaneousFrequency();

// Find phase synchronization between two signals
double[] signal1 = loadSignal1();
double[] signal2 = loadSignal2();

ComplexCWTResult result1 = cwt.analyzeComplex(signal1, scales);
ComplexCWTResult result2 = cwt.analyzeComplex(signal2, scales);

// Calculate phase synchronization index
double[][] phase1 = result1.getPhase();
double[][] phase2 = result2.getPhase();

for (int s = 0; s < scales.length; s++) {
    double syncIndex = 0;
    for (int t = 0; t < signal1.length; t++) {
        double phaseDiff = phase1[s][t] - phase2[s][t];
        syncIndex += Math.cos(phaseDiff);
    }
    syncIndex /= signal1.length;
    
    System.out.printf("Scale %d: Synchronization = %.3f\n", 
        (int)scales[s], syncIndex);
}
```

## Performance Tips

### 1. Choose the Right Wavelet
```java
// For sharp transitions: Haar
MODWTTransform haar = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// For smooth signals: Higher-order Daubechies
MODWTTransform db8 = new MODWTTransform(Daubechies.DB8, BoundaryMode.PERIODIC);

// For symmetric signals: Symlets
MODWTTransform sym4 = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
```

### 2. Monitor Performance
```java
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

// Check if SIMD is being used
MODWTTransform.PerformanceInfo perfInfo = transform.getPerformanceInfo();
System.out.println("Performance: " + perfInfo.description());

// Estimate processing time
long estimatedNanos = transform.estimateProcessingTime(signalLength);
System.out.printf("Estimated time: %.2f ms\n", estimatedNanos / 1e6);
```

### 3. Use Appropriate Buffer Sizes
```java
// For real-time audio at 48kHz
int blockSize = 480; // Exactly 10ms blocks

// For financial tick data
int blockSize = 100; // Typical number of ticks per second

// For batch processing
int blockSize = 1024; // Good for cache efficiency
```

### 4. Clean Up Resources
```java
// Always close streaming transforms
try (MODWTStreamingTransform stream = createStream()) {
    // Use stream
} // Automatically closed

// Clean up thread-local resources in server environments
BatchSIMDTransform.cleanupThreadLocals();

// Clear memory pools periodically
pool.clear();
```

## Advanced Topics

### Custom Threshold Functions

```java
// Implement custom thresholding for denoising
public class CustomThreshold {
    public static double[] customDenoise(double[] signal, 
                                        MODWTTransform transform,
                                        double alpha) {
        // Forward transform
        MODWTResult result = transform.forward(signal);
        double[] details = result.detailCoeffs();
        
        // Custom threshold: alpha * sqrt(2 * log(n)) * sigma
        double sigma = medianAbsoluteDeviation(details) / 0.6745;
        double threshold = alpha * Math.sqrt(2 * Math.log(signal.length)) * sigma;
        
        // Soft thresholding with custom rule
        double[] thresholded = new double[details.length];
        for (int i = 0; i < details.length; i++) {
            double x = details[i];
            if (Math.abs(x) <= threshold) {
                thresholded[i] = 0;
            } else {
                // Custom soft threshold formula
                double sign = Math.signum(x);
                thresholded[i] = sign * (Math.abs(x) - threshold) * 
                                Math.exp(-Math.pow(x/threshold, 2));
            }
        }
        
        // Create new result with thresholded coefficients
        MODWTResult denoisedResult = MODWTResultImpl.create(
            result.approximationCoeffs(), thresholded);
        
        // Inverse transform
        return transform.inverse(denoisedResult);
    }
}
```

### Wavelet Packet Transform (Future)

```java
// Note: Wavelet Packet Transform planned for future release
// Will provide:
// - Adaptive decomposition of both approximation and detail
// - Best basis selection
// - Enhanced frequency resolution
```

## Conclusion

VectorWave's MODWT implementation provides a powerful, flexible foundation for signal processing applications. Key advantages:

1. **No length restrictions** - Process signals of any length
2. **Shift-invariance** - Robust pattern detection and analysis
3. **Perfect reconstruction** - Machine-precision accuracy
4. **Real-time capable** - Streaming APIs for continuous processing
5. **High performance** - SIMD optimization with Java 23

For more examples, see the demo files in `src/main/java/ai/prophetizo/demo/`.