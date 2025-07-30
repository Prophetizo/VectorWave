# Financial Analysis Parameters Guide

This document describes all configurable parameters for financial wavelet analysis in VectorWave.

## Overview

The `FinancialAnalysisParameters` class provides fine-grained control over all thresholds and parameters used in financial market analysis. This allows customization for different markets, instruments, and trading strategies.

## Quick Start

```java
// Use default parameters (equivalent to the old static constants)
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();

// Customize parameters for volatile markets
FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(15.0)     // More sensitive crash detection
    .volatilityThresholds(0.3, 1.2, 2.5)  // Tighter volatility bands
    .regimeTrendThreshold(0.03)        // 3% trend threshold
    .build();

FinancialWaveletAnalyzer customAnalyzer = new FinancialWaveletAnalyzer(params);
```

## Parameter Categories

### 1. Crash Detection Parameters

These parameters control how the analyzer detects market crashes and sharp price drops.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `crashAsymmetryThreshold` | 10.0 | Minimum asymmetry score to detect a crash | 5.0 - 20.0 |
| `crashMinScale` | 1.0 | Minimum wavelet scale for crash analysis | 0.5 - 2.0 |
| `crashMaxScale` | 10.0 | Maximum wavelet scale for crash analysis | 5.0 - 20.0 |
| `crashNumScales` | 20 | Number of scales to analyze | 10 - 50 |
| `crashProbabilityNormalization` | 20.0 | Factor to normalize crash probability to [0,1] | 10.0 - 50.0 |
| `crashPredictionForwardWindow` | 5 | Days to look ahead for crash prediction | 3 - 10 |

**Example: Aggressive Crash Detection**
```java
params.crashAsymmetryThreshold(5.0)  // Lower threshold = more sensitive
      .crashScaleRange(0.5, 15.0, 30); // Wider scale range
```

### 2. Volatility Analysis Parameters

Controls volatility clustering detection and classification.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `volatilityLowThreshold` | 0.5 | Multiplier for low volatility classification | 0.3 - 0.7 |
| `volatilityMediumThreshold` | 1.5 | Multiplier for medium volatility | 1.0 - 2.0 |
| `volatilityHighThreshold` | 3.0 | Multiplier for high volatility | 2.5 - 4.0 |
| `volumeDivergenceThreshold` | 2.0 | Volume change threshold for divergence | 1.5 - 3.0 |
| `priceDivergenceThreshold` | 0.01 | Price change threshold (1%) | 0.005 - 0.02 |

**Example: Crypto Market Volatility**
```java
params.volatilityThresholds(0.8, 2.5, 5.0); // Higher thresholds for crypto
```

### 3. Cyclical Analysis Parameters

For detecting periodic patterns in market data.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `cycleTestFrequencies` | [0.2, 0.1, 0.045, 0.02] | Frequencies to test (5, 10, 22, 50 day cycles) |

**Example: Custom Trading Cycles**
```java
params.cycleTestFrequencies(
    0.33,   // 3-day cycle
    0.143,  // 7-day cycle (weekly)
    0.0455  // 22-day cycle (monthly)
);
```

### 4. Trend Analysis Parameters

Controls trend detection at multiple scales.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `trendMinScale` | 10.0 | Minimum scale for trend analysis | 5.0 - 20.0 |
| `trendMaxScale` | 50.0 | Maximum scale for trend analysis | 30.0 - 100.0 |
| `trendNumScales` | 10 | Number of trend scales | 5 - 20 |

### 5. Market Regime Detection

Parameters for identifying market states (trending, ranging, volatile).

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `regimeDetectionLookbackPeriod` | 20 | Days to look back for regime detection | 10 - 50 |
| `regimeTrendThreshold` | 0.05 | Return threshold for trend detection (5%) | 0.02 - 0.10 |

**Example: Fast Market Regime Detection**
```java
params.regimeDetectionLookbackPeriod(10)  // Shorter lookback
      .regimeTrendThreshold(0.02);        // 2% threshold
```

### 6. Trading Signal Generation

Controls when buy/sell signals are generated.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `signalGenerationMinHistory` | 20 | Minimum history before generating signals | 10 - 50 |
| `recentCrashLookbackWindow` | 20 | Days to check for recent crashes | 10 - 30 |

### 7. Risk Assessment

Parameters for calculating market risk levels.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `riskAssessmentCrashWindow` | 20 | Window for crash impact on risk | 10 - 30 |
| `defaultAverageVolatility` | 0.02 | Assumed average market volatility (2%) | 0.01 - 0.05 |
| `baseRiskLevel` | 0.5 | Baseline risk level | 0.3 - 0.7 |

### 8. Technical Analysis

Support and resistance detection parameters.

| Parameter | Default | Description | Typical Range |
|-----------|---------|-------------|---------------|
| `supportResistanceWindow` | 20 | Window size for S/R detection | 10 - 50 |

## Optimization Parameters

The `OptimizationParameters` class contains wavelet-specific settings for fine-tuning analysis algorithms.

### Crash Detection Optimization
```java
OptimizationParameters.builder()
    .crashPaulOrder(4)           // Paul wavelet order
    .crashDogOrder(2)            // DOG wavelet order
    .crashThresholdFactor(0.5)   // Detection sensitivity
    .crashSeverityExponent(1.5)  // Severity scaling
    .crashScaleRange(1.0, 10.0)  // Analysis scale range
```

### Volatility Analysis Optimization
```java
.volatilityPaulOrder(3)
.volatilityDogOrder(2)
.volatilityThresholdFactor(0.3)
.volatilityExponent(1.0)
.volatilityScaleRange(1.0, 30.0)
```

### Cycle Detection Optimization
```java
.cycleShannonParameters(2, 3)    // Shannon wavelet (fb, fc)
.cycleThresholdFactor(0.2)
.cycleExponent(2.0)
.cycleScaleRange(5.0, 50.0)
```

### Signal Generation Optimization
```java
.signalPaulOrder(4)
.signalDogOrder(2)
.signalThresholdFactor(0.4)
.signalExponent(1.5)
.signalScaleRange(2.0, 20.0)
```

## Market-Specific Configurations

### Stock Market (S&P 500)
```java
FinancialAnalysisParameters stockParams = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(10.0)
    .volatilityThresholds(0.5, 1.5, 3.0)
    .regimeTrendThreshold(0.05)
    .defaultAverageVolatility(0.015)  // 1.5% daily volatility
    .build();
```

### Forex Market
```java
FinancialAnalysisParameters forexParams = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(8.0)    // More sensitive for smaller moves
    .volatilityThresholds(0.3, 1.0, 2.0)
    .regimeTrendThreshold(0.02)      // 2% for currencies
    .defaultAverageVolatility(0.008)  // 0.8% daily volatility
    .cycleTestFrequencies(0.2, 0.1, 0.045)  // Shorter cycles
    .build();
```

### Cryptocurrency Market
```java
FinancialAnalysisParameters cryptoParams = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(20.0)   // Higher threshold for volatile markets
    .volatilityThresholds(1.0, 3.0, 6.0)  // Much higher volatility
    .regimeTrendThreshold(0.10)      // 10% moves are common
    .defaultAverageVolatility(0.05)  // 5% daily volatility
    .signalGenerationMinHistory(30)  // More history needed
    .build();
```

### Commodities (Gold, Oil)
```java
FinancialAnalysisParameters commodityParams = FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(12.0)
    .volatilityThresholds(0.6, 1.8, 3.5)
    .regimeTrendThreshold(0.04)
    .cycleTestFrequencies(0.045, 0.02, 0.01)  // Longer cycles
    .trendScaleRange(20.0, 100.0, 15)  // Longer trends
    .build();
```

## Migration from Static Configuration

If you're upgrading from the old static configuration:

```java
// Old way (using static constants)
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();

// New way (equivalent behavior)
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(
    FinancialAnalysisParameters.defaultParameters()
);

// The default parameters exactly match the old static constants
```

## Best Practices

1. **Start with defaults**: Use `defaultParameters()` and adjust only what's needed
2. **Test incrementally**: Change one parameter category at a time
3. **Market-specific tuning**: Different markets need different parameters
4. **Backtesting**: Always backtest parameter changes on historical data
5. **Document changes**: Keep notes on why parameters were adjusted

## Validation

All parameters are validated when set:
- Numeric parameters must be positive (except where noted)
- Thresholds must be in valid ranges
- Array parameters cannot be empty
- Scale ranges must have min < max

Example validation error:
```java
// This will throw IllegalArgumentException
params.crashAsymmetryThreshold(-5.0);  // Must be positive
```

## Performance Considerations

- More scales = more computation time
- Larger windows = more memory usage
- Shorter lookback periods = faster response but more false signals
- Consider using `CWTConfig` to control parallel processing

## Future Enhancements

The parameter system is designed for extensibility:
- Machine learning integration for parameter optimization
- Market condition adaptive parameters
- Real-time parameter adjustment based on volatility
- Parameter persistence and profiles