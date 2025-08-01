# Financial Analysis with VectorWave

VectorWave now includes financial analysis capabilities that leverage wavelet transforms for advanced signal processing in financial data analysis.

## Features

### Sharpe Ratio Calculation
- **Configurable Risk-Free Rate**: Set custom risk-free rates or use the default treasury rate
- **Multiple Calculation Methods**: Standard and wavelet-denoised Sharpe ratio calculations
- **Robust Input Validation**: Comprehensive error handling and edge case management

### Wavelet-Based Denoising
- **Signal Noise Reduction**: Remove high-frequency noise from financial time series
- **Improved Risk Metrics**: Calculate more stable risk-adjusted returns using denoised data

## Quick Start

### Basic Usage

```java
import ai.prophetizo.financial.*;

// Create analyzer with default configuration (4.5% risk-free rate)
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();

// Calculate Sharpe ratio for monthly returns
double[] monthlyReturns = {0.05, 0.02, -0.01, 0.08, 0.04, 0.06, -0.02, 0.09};
double sharpeRatio = analyzer.calculateSharpeRatio(monthlyReturns);
```

### Custom Risk-Free Rate

```java
// Use custom risk-free rate (3.0%)
FinancialConfig config = new FinancialConfig(0.03);
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

double sharpeRatio = analyzer.calculateSharpeRatio(monthlyReturns);

// Or specify risk-free rate directly
double sharpeRatio = analyzer.calculateSharpeRatio(monthlyReturns, 0.025);
```

### Wavelet-Based Analysis

```java
// For power-of-2 length data, use wavelet denoising
double[] returns = {0.05, 0.02, -0.01, 0.08, 0.04, 0.06, -0.02, 0.09}; // Length: 8
double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);
```

### Advanced Configuration

```java
// Use custom wavelet for analysis
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(Daubechies.DB4);

FinancialConfig config = new FinancialConfig(0.025);
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config, transform);
```

## API Reference

### FinancialConfig

**Constructor**
- `FinancialConfig()` - Uses default risk-free rate (4.5%)
- `FinancialConfig(double riskFreeRate)` - Custom risk-free rate (annual decimal)

**Methods**
- `getRiskFreeRate()` - Returns the configured risk-free rate

### FinancialWaveletAnalyzer

**Constructors**
- `FinancialWaveletAnalyzer()` - Default configuration with Haar wavelet
- `FinancialWaveletAnalyzer(FinancialConfig config)` - Custom config with Haar wavelet
- `FinancialWaveletAnalyzer(FinancialConfig config, WaveletTransform transform)` - Full customization

**Sharpe Ratio Methods**
- `calculateSharpeRatio(double[] returns)` - Uses configured risk-free rate
- `calculateSharpeRatio(double[] returns, double riskFreeRate)` - Custom risk-free rate
- `calculateWaveletSharpeRatio(double[] returns)` - Wavelet-denoised, configured rate
- `calculateWaveletSharpeRatio(double[] returns, double riskFreeRate)` - Wavelet-denoised, custom rate

## Default Values

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| Risk-Free Rate | 4.5% (0.045) | Approximates current US Treasury 10-year rate |
| Wavelet | Haar | Simple, fast wavelet suitable for financial analysis |
| Boundary Mode | Periodic | Handles signal boundaries appropriately |

## Input Requirements

### Return Data Format
- **Data Type**: `double[]` array
- **Values**: Decimal format (e.g., 0.05 for 5% return)
- **Minimum Length**: 2 data points (for standard deviation calculation)
- **Wavelet Analysis**: Power-of-2 length required (2, 4, 8, 16, 32, ...)

### Risk-Free Rate Format
- **Data Type**: `double`
- **Range**: Non-negative values (≥ 0.0)
- **Format**: Annual decimal (e.g., 0.045 for 4.5%)

## Error Handling

The library provides comprehensive error handling:

```java
// Invalid inputs throw IllegalArgumentException
analyzer.calculateSharpeRatio(null);           // → IllegalArgumentException
analyzer.calculateSharpeRatio(new double[0]);  // → IllegalArgumentException
analyzer.calculateSharpeRatio(new double[]{0.05}); // → IllegalArgumentException

// Wavelet analysis requires power-of-2 length
double[] invalidLength = {0.05, 0.02, 0.08}; // Length 3
analyzer.calculateWaveletSharpeRatio(invalidLength); // → IllegalArgumentException
```

## Mathematical Notes

### Sharpe Ratio Formula
```
Sharpe Ratio = (Mean Return - Risk-Free Rate) / Standard Deviation
```

### Special Cases
- **Zero Standard Deviation**: Returns ±∞ based on excess return sign
- **Zero Excess Return with Zero Std Dev**: Returns 0.0
- **Negative Excess Return**: Returns negative Sharpe ratio

### Wavelet Denoising
The wavelet denoising process:
1. Applies forward wavelet transform to decompose the signal
2. Removes detail (high-frequency) coefficients
3. Reconstructs using only approximation (low-frequency) coefficients
4. Calculates Sharpe ratio on the denoised signal

## Running the Demo

```bash
# Compile the project
mvn compile

# Run the financial analysis demo
java -cp target/classes ai.prophetizo.demo.FinancialDemo
```

The demo showcases:
- Default configuration usage
- Multiple risk-free rate scenarios
- Comparison between regular and wavelet-based Sharpe ratios