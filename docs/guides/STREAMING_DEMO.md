# VectorWave Streaming Financial Analysis Demos

This directory contains interactive demonstrations of VectorWave's streaming financial analysis capabilities, showcasing real-time market data processing with minimal memory usage.

## Running the Demos

Use the provided script to run different demo modes:

```bash
./run-streaming-demo.sh [mode]
```

Available modes:
- `simple` - Real-time streaming dashboard (default)
- `incremental` - Detailed incremental analysis with EMAs
- `compare` - Performance comparison vs batch processing
- `trading` - Interactive live trading simulation

## Demo Descriptions

### 1. Simple Streaming Analysis (`simple`)

A real-time dashboard that processes market data using sliding windows:

```bash
./run-streaming-demo.sh simple
```

Features:
- Live price chart visualization
- Real-time volatility calculation
- Market regime detection
- Risk level monitoring
- Trading signal generation

**What to watch for:**
- How volatility spikes during market events
- Regime changes from ranging to trending
- Risk level adjustments based on market conditions

### 2. Incremental Analysis (`incremental`)

Demonstrates advanced incremental calculations with minimal memory overhead:

```bash
./run-streaming-demo.sh incremental
```

Features:
- Exponential Moving Averages (12, 26, 50)
- Welford's algorithm for online volatility
- Crash detection using wavelet analysis
- Maximum drawdown tracking

**Key insights:**
- EMA crossovers indicating trend changes
- Volatility calculated without storing history
- Real-time crash warnings

### 3. Performance Comparison (`compare`)

Benchmarks streaming vs batch processing:

```bash
./run-streaming-demo.sh compare
```

Shows:
- Processing time comparison
- Memory usage differences
- Scalability demonstration (1M samples)
- Constant memory proof

**Results highlight:**
- Streaming uses constant memory regardless of data size
- Faster processing for real-time scenarios
- Ideal for continuous data feeds

### 4. Live Trading Simulation (`trading`)

An interactive trading bot simulation using wavelet analysis:

```bash
./run-streaming-demo.sh trading
```

Features:
- $10,000 starting portfolio
- Automated buy/sell decisions
- Real-time P&L tracking
- Market event simulation
- Win/loss statistics

**Market events simulated:**
- Earnings beats
- Fed rate changes
- Flash crashes
- Product launches
- High volatility periods

## Technical Implementation

### Memory Efficiency

The streaming analyzers use several techniques to minimize memory usage:

1. **Circular Buffers**: Fixed-size arrays with index wrapping
2. **Incremental Statistics**: Running calculations without full history
3. **Object Pooling**: Reuse of temporary arrays and objects
4. **Primitive Collections**: Avoid boxing overhead

### Streaming Algorithms

1. **Simple Streaming**:
   ```java
   // Fixed window size, no growing arrays
   double[] window = new double[50];
   int index = samplesProcessed % 50;
   window[index] = newPrice;
   ```

2. **Incremental Updates**:
   ```java
   // Welford's algorithm for variance
   delta = value - mean;
   mean += delta / count;
   M2 += delta * (value - mean);
   variance = M2 / (count - 1);
   ```

3. **EMA Calculation**:
   ```java
   // Exponential smoothing
   double alpha = 2.0 / (period + 1);
   ema = price * alpha + prevEMA * (1 - alpha);
   ```

## Performance Characteristics

| Metric | Streaming | Batch |
|--------|-----------|-------|
| Memory | O(window_size) | O(n) |
| Time/sample | O(1) | O(n) |
| Latency | Microseconds | Milliseconds |
| GC Pressure | Minimal | High |

## Use Cases

### Real-time Trading
- High-frequency trading systems
- Algorithmic trading bots
- Risk management systems

### Market Monitoring
- Live dashboards
- Alert systems
- Anomaly detection

### Resource-Constrained Environments
- Edge computing
- Mobile applications
- Embedded systems

## Customization

Modify parameters in the demo code:

```java
// Window size (affects memory usage)
new SimpleStreamingAnalyzer(100, 10);

// Update frequency (affects CPU usage)
new IncrementalFinancialAnalyzer(
    parameters,
    256,  // window size
    5     // update every 5 samples
);

// Trading thresholds
FinancialAnalysisParameters.builder()
    .crashAsymmetryThreshold(8.0)
    .regimeTrendThreshold(0.02)
    .build()
```

## Visual Output

The demos use ANSI color codes for terminal visualization:
- 🟢 Green: Positive trends, buy signals
- 🔴 Red: Negative trends, sell signals
- 🟡 Yellow: Warnings, medium risk
- 🔵 Blue: Neutral, ranging markets
- ⚡ Lightning: High volatility
- 📈 Charts: Buy opportunities
- 📉 Charts: Sell opportunities
- ⚠️ Warning: Crash detection

## Integration Example

To use streaming analysis in your application:

```java
// Create analyzer
SimpleStreamingAnalyzer analyzer = new SimpleStreamingAnalyzer(50, 10);

// Set up result handler
analyzer.onResult(result -> {
    if (result.signal().isPresent()) {
        executeTrade(result.signal().get());
    }
    updateDashboard(result);
});

// Feed live data
marketDataFeed.subscribe(price -> {
    analyzer.processSample(price);
});
```

## Conclusion

These demos showcase how VectorWave enables:
- Real-time financial analysis with minimal latency
- Constant memory usage for infinite streams
- Production-ready algorithms for trading systems
- Visual feedback for market understanding

Perfect for building responsive, efficient financial applications that can process continuous data streams without memory concerns.