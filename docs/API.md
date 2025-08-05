# VectorWave API Reference

## Quick Reference

### MODWT Transform (Primary)
```java
// Basic transform - works with ANY signal length
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);

// Batch processing with automatic SIMD
double[][] signals = new double[32][1000];
MODWTResult[] results = transform.forwardBatch(signals);

// Multi-level decomposition
MultiLevelMODWTTransform mlTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
MultiLevelMODWTResult mlResult = mlTransform.decompose(signal, levels);
double[] reconstructed = mlTransform.reconstruct(mlResult);
```

### CWT Transform
```java
// Basic CWT analysis
CWTTransform cwt = new CWTTransform(new MorletWavelet());
double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
CWTResult result = cwt.analyze(signal, scales);

// Complex analysis with phase information
ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);
double[][] magnitude = complexResult.getMagnitude();
double[][] phase = complexResult.getPhase();
```

### Financial Analysis
```java
// Configure financial parameters explicitly
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

// Wavelet-based Sharpe ratio
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);

// Financial wavelet analysis
PaulWavelet paulWavelet = new PaulWavelet(4); // Asymmetric crash detection
CWTTransform financialCWT = new CWTTransform(paulWavelet);
CWTResult crashAnalysis = financialCWT.analyze(priceReturns, scales);
```

### Streaming Processing
```java
// Real-time streaming with arbitrary block sizes
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .bufferSize(480) // 10ms at 48kHz - no padding needed!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .build();

// Process continuous stream
for (double[] chunk : audioStream) {
    double[] denoised = denoiser.denoise(chunk);
}
```

### Wavelet Registry
```java
// Discover available wavelets
Set<String> available = WaveletRegistry.getAvailableWavelets();
List<String> orthogonal = WaveletRegistry.getOrthogonalWavelets();

// Get wavelets by name (case-insensitive)
Wavelet db4 = WaveletRegistry.getWavelet("db4");
Wavelet morlet = WaveletRegistry.getWavelet("morlet");
```

## Core Classes

### MODWT Package (`ai.prophetizo.wavelet.modwt`)

#### MODWTTransform
- `forward(double[] signal)` - Forward transform
- `inverse(MODWTResult result)` - Inverse transform  
- `forwardBatch(double[][] signals)` - Batch processing
- `inverseBatch(MODWTResult[] results)` - Batch reconstruction
- `getPerformanceInfo()` - Platform capabilities
- `estimateProcessingTime(int length)` - Performance estimation

#### MODWTResult
- `create(double[] approx, double[] detail)` - Factory method
- `approximationCoeffs()` - Get approximation coefficients
- `detailCoeffs()` - Get detail coefficients
- `getSignalLength()` - Original signal length
- `isValid()` - Check for finite values

#### MultiLevelMODWTTransform
- `decompose(double[] signal)` - Automatic max levels
- `decompose(double[] signal, int levels)` - Specify levels
- `reconstruct(MultiLevelMODWTResult result)` - Full reconstruction
- `reconstructFromLevel(result, int level)` - Partial reconstruction
- `getMaximumLevels(int signalLength)` - Calculate max levels

### CWT Package (`ai.prophetizo.wavelet.cwt`)

#### CWTTransform
- `analyze(double[] signal, double[] scales)` - Basic analysis
- `analyzeComplex(double[] signal, double[] scales)` - Complex analysis

#### Wavelets
- `MorletWavelet` - Time-frequency analysis
- `PaulWavelet(int order)` - Financial crash detection
- `GaussianDerivativeWavelet(int derivative)` - Edge detection

### Financial Package (`ai.prophetizo.financial`)

#### FinancialWaveletAnalyzer
- Constructor requires `FinancialConfig(double riskFreeRate)`
- `calculateSharpeRatio(double[] returns)` - Standard Sharpe ratio
- `calculateWaveletSharpeRatio(double[] returns)` - Wavelet-based Sharpe

## Wavelet Families

### Orthogonal Wavelets
```java
new Haar()                    // Simplest wavelet
Daubechies.DB2                // 2 vanishing moments
Daubechies.DB4                // 4 vanishing moments (popular)
Daubechies.DB8                // 8 vanishing moments
Symlet.SYM4                   // Nearly symmetric
Coiflet.COIF2                 // Both functions have vanishing moments
```

### Biorthogonal Wavelets
```java
BiorthogonalSpline.BIOR1_3    // CDF 1,3 wavelet
```

### Continuous Wavelets
```java
new MorletWavelet()           // Complex Morlet
new PaulWavelet(4)            // Paul order 4
new GaussianDerivativeWavelet(2)  // Mexican Hat (2nd derivative)
```

### Financial Wavelets
```java
new PaulWavelet(4)            // Crash detection
new ShannonGaborWavelet()     // Reduced artifacts
new DOGWavelet(2)             // Difference of Gaussians
```

## Performance Features

### Automatic Optimization
- **SIMD Detection**: Automatically uses Vector API when available
- **Platform Adaptive**: x86 (AVX2/AVX512), ARM (NEON)
- **Scalar Fallback**: Graceful degradation on unsupported platforms
- **Batch Optimization**: 2-4x speedup for multiple signals

### Performance Monitoring
```java
// Check platform capabilities
WaveletOperations.PerformanceInfo info = WaveletOperations.getPerformanceInfo();
System.out.println(info.description());
// Output: "Vectorized operations enabled on aarch64 with S_128_BIT"

// Estimate processing time
MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
System.out.println(estimate.description());
```

## Configuration

### Boundary Modes
- `BoundaryMode.PERIODIC` - Circular/periodic boundary (recommended for MODWT)
- `BoundaryMode.ZERO_PADDING` - Zero padding at boundaries

### Streaming Configuration
```java
MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(1024)  // Any size - no power-of-2 restriction
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .build();
```

## Error Handling

### Exception Types
- `InvalidSignalException` - Invalid input signals
- `InvalidArgumentException` - Invalid parameters
- `InvalidConfigurationException` - Invalid configuration
- `WaveletTransformException` - Transform-specific errors

### Common Patterns
```java
try {
    MODWTResult result = transform.forward(signal);
} catch (InvalidSignalException e) {
    // Handle invalid signal (NaN, Infinity, empty)
    ErrorContext context = e.getErrorContext();
    System.err.println("Signal error: " + context.getDescription());
}
```

## Key Differences from Standard Wavelet Libraries

1. **No Power-of-2 Restriction**: MODWT works with signals of any length
2. **Shift Invariance**: MODWT provides translation-invariant analysis
3. **Automatic SIMD**: Platform-specific optimizations without manual configuration
4. **Financial Focus**: Specialized wavelets and analysis tools for market data
5. **Explicit Configuration**: No default values for financial parameters
6. **Java 23 Features**: Modern Java with Vector API integration

## Migration from Other Libraries

### From PyWavelets
```python
# PyWavelets
coeffs = pywt.dwt(signal, 'db4')

# VectorWave (Java)
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
MODWTResult result = transform.forward(signal);
```

### From MATLAB Wavelet Toolbox
```matlab
% MATLAB
[cA, cD] = dwt(signal, 'db4');

% VectorWave (Java)  
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
MODWTResult result = transform.forward(signal);
double[] approx = result.approximationCoeffs();
double[] detail = result.detailCoeffs();
```

## Requirements

- **Java 23+** - Required for Vector API and modern features
- **Compilation**: `--add-modules jdk.incubator.vector`
- **Runtime**: Vector API optional (automatic fallback)
- **No Dependencies**: Pure Java implementation