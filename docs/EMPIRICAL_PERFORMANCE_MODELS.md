# Empirical Performance Models

## Overview

This document describes the empirical performance modeling system implemented for VectorWave, addressing issue #157's requirement for platform-specific performance calibration.

## Architecture

### 1. **Core Components**

#### PerformanceModel
- Piecewise polynomial model (quadratic: time = a + b*n + c*n²)
- Adaptive learning through online gradient descent
- Confidence intervals based on prediction errors
- Platform-specific scaling factors

#### PlatformFactors
- Detects hardware characteristics:
  - CPU architecture (x86, ARM, etc.)
  - Vector instruction support (AVX-512, NEON)
  - Cache sizes (L1, L2, L3)
  - Core count and memory bandwidth
- Provides scaling factors for performance adjustment

#### AdaptivePerformanceEstimator
- Singleton that manages all performance models
- Learns from actual execution measurements
- Automatically recalibrates when accuracy degrades
- Persists models to disk for reuse

### 2. **Model Types**

#### MODWT Model
- Predicts execution time for MODWT operations
- Accounts for wavelet complexity
- Separate calibration for different wavelet types

#### Convolution Model
- Models circular and zero-padding convolution
- Scales with filter length
- Captures cache effects

#### Batch Model
- Predicts batch operation performance
- Models efficiency gains from SIMD processing
- Accounts for diminishing returns with larger batches

### 3. **Key Features**

#### Adaptive Learning
```java
// Automatic measurement recording
MODWTTransform transform = new MODWTTransform(wavelet, mode);
MODWTResult result = transform.forward(signal);
// Execution time is automatically recorded for learning
```

#### Confidence Intervals
```java
ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
// Returns: 1.23ms [0.98-1.48ms] with 95% confidence
```

#### Platform Detection
```java
PlatformFactors platform = PlatformFactors.detectPlatform();
// Detects: ARM/x86, NEON/AVX-512, cache sizes, core count
```

## Implementation Details

### 1. **Polynomial Model**

The base model uses a quadratic polynomial:
```
time(n) = a + b*n + c*n²
```

Where:
- `a`: Fixed overhead (constant term)
- `b`: Linear scaling factor
- `c`: Quadratic term (for cache effects)

### 2. **Size Ranges**

Different coefficient sets for different input sizes:
- TINY: 0-256 samples
- SMALL: 257-1024 samples
- MEDIUM: 1025-4096 samples
- LARGE: 4097-16384 samples
- HUGE: 16385+ samples

### 3. **Online Learning**

Coefficients update using gradient descent:
```java
error = actual - predicted
a -= learningRate * (-2 * error)
b -= learningRate * (-2 * error * inputSize)
c -= learningRate * (-2 * error * inputSize²) * 0.1
```

Learning rate decreases with more measurements for stability.

### 4. **Cache Effects**

Cache multipliers based on data size:
- L1 cache: 1.0x (no penalty)
- L2 cache: 1.2x penalty
- L3 cache: 1.5x penalty
- Main memory: 2.0x penalty

### 5. **Calibration Process**

Full calibration tests:
1. Multiple signal sizes (64 to 65536)
2. Different wavelets (Haar, Daubechies)
3. Various filter lengths
4. Batch operations
5. Warm-up iterations to stabilize
6. Multiple measurements for accuracy

## Usage

### Basic Estimation
```java
MODWTTransform transform = new MODWTTransform(wavelet, mode);
ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
System.out.println(estimate.description());
// Output: "Signal length 1024: 0.45ms [0.36-0.54ms] (4.2x speedup with vectors) - 92% confidence"
```

### Manual Calibration
```java
PerformanceCalibrator calibrator = new PerformanceCalibrator();
CalibratedModels models = calibrator.calibrate();
models.save("performance_models.dat");
```

### Accuracy Monitoring
```java
AdaptivePerformanceEstimator estimator = AdaptivePerformanceEstimator.getInstance();
System.out.println(estimator.getAccuracyReport());
```

## Platform-Specific Optimizations

### Apple Silicon (M1/M2)
- Detected as ARM architecture with NEON
- Higher L1 cache (128KB vs 32KB)
- Unified memory architecture benefits
- Conservative 2x speedup for NEON

### x86-64 with AVX-512
- 8x theoretical speedup (512-bit vectors)
- Actual speedup varies with operation
- Larger cache requirements

### x86-64 with AVX2
- 4x theoretical speedup (256-bit vectors)
- Most common configuration
- Well-balanced performance

## Automatic Recalibration

The system automatically recalibrates when:
1. Mean absolute percentage error > 15%
2. More than 30 days since last calibration
3. Fewer than 100 measurements collected
4. Model confidence drops below 85%

## Performance Results

Example accuracy metrics after calibration:
```
MODWT Model:
  MAE: 0.12 ms
  RMSE: 0.18 ms
  MAPE: 8.3%
  R²: 0.976
  Confidence: 91.7%
  Interval hit rate: 94.2%
```

## Future Enhancements

1. **GPU Performance Models**: Add CUDA/OpenCL predictions
2. **Network Latency Models**: For distributed processing
3. **Energy Consumption Models**: Power-aware computing
4. **Multi-threaded Models**: Predict parallel speedup
5. **Memory Bandwidth Models**: For very large signals

## Configuration

Models are stored in: `~/.vectorwave/performance/performance_models.dat`

Environment variables:
- `VECTORWAVE_CALIBRATE`: Force recalibration on startup
- `VECTORWAVE_PERF_LOG`: Enable detailed performance logging
- `VECTORWAVE_MODEL_DIR`: Custom model storage directory