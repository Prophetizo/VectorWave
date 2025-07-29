package ai.prophetizo.financial;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optimized version of FinancialWaveletAnalyzer with reduced memory allocations.
 * 
 * Key optimizations:
 * 1. Object pooling for result objects
 * 2. Pre-allocated and reused arrays
 * 3. Primitive collections instead of ArrayList
 * 4. Streaming calculations where possible
 */
public class OptimizedFinancialWaveletAnalyzer {
    
    private final WaveletTransformFactory factory;
    private final Wavelet defaultWavelet;
    
    // Configuration parameters
    private final int windowSize;
    private final int minPeriods;
    private final double confidenceThreshold;
    
    // Object pool for trading signals
    private final TradingSignalPool signalPool;
    
    // Pre-allocated reusable arrays to reduce GC pressure
    private double[] reusableReturns;
    private double[] reusableSquaredReturns;
    private double[] reusableRollingVolatilities;
    private double[] reusableWindowBuffer;
    private double[] reusableDetailVolatilities;
    private double[] reusableTimeScaleVolatilities;
    private double[] reusablePriceWindow;
    private double[] reusableAnalysisData;
    private double[] reusableTrendRisk;
    private double[] reusableNoiseRisk;
    private double[] reusableWeights;
    private double[] reusableDownsampled;
    
    // Pre-allocated arrays for trading signals (avoiding ArrayList)
    private TradingSignal[] signalsBuffer;
    private int signalsCount;
    private static final int MAX_SIGNALS = 10000;
    
    public OptimizedFinancialWaveletAnalyzer() {
        this(256, 20, 0.7);
    }
    
    public OptimizedFinancialWaveletAnalyzer(int windowSize, int minPeriods, double confidenceThreshold) {
        this.windowSize = windowSize;
        this.minPeriods = minPeriods;
        this.confidenceThreshold = confidenceThreshold;
        this.factory = new WaveletTransformFactory().withBoundaryMode(BoundaryMode.PERIODIC);
        this.defaultWavelet = Daubechies.DB4;
        this.signalPool = new TradingSignalPool(100);
        
        // Pre-allocate reusable arrays
        initializeReusableArrays();
    }
    
    private void initializeReusableArrays() {
        // Size arrays for typical workloads, will resize if needed
        int maxExpectedSize = Math.max(windowSize * 2, 2048);
        
        reusableReturns = new double[maxExpectedSize];
        reusableSquaredReturns = new double[maxExpectedSize];
        reusableRollingVolatilities = new double[maxExpectedSize];
        reusableWindowBuffer = new double[windowSize];
        reusableDetailVolatilities = new double[maxExpectedSize];
        reusableTimeScaleVolatilities = new double[10]; // Fixed size for time scales
        reusablePriceWindow = new double[minPeriods];
        reusableAnalysisData = new double[maxExpectedSize];
        reusableTrendRisk = new double[maxExpectedSize];
        reusableNoiseRisk = new double[maxExpectedSize];
        reusableWeights = new double[maxExpectedSize];
        reusableDownsampled = new double[maxExpectedSize / 2];
        
        signalsBuffer = new TradingSignal[MAX_SIGNALS];
        signalsCount = 0;
    }
    
    /**
     * Ensures an array is large enough for the required size.
     * Resizes only if necessary to minimize allocations.
     */
    private double[] ensureCapacity(double[] array, int requiredSize) {
        if (array.length < requiredSize) {
            // Only resize when necessary, grow by 1.5x to reduce future resizing
            return new double[Math.max(requiredSize, (int)(array.length * 1.5))];
        }
        return array;
    }
    
    /**
     * Analyzes price data for various trading patterns and signals.
     * Same interface as original, but optimized internally.
     */
    public Map<String, Object> analyzePriceData(double[] prices) {
        if (prices == null || prices.length < minPeriods) {
            throw new IllegalArgumentException("Insufficient price data");
        }
        
        Map<String, Object> results = new HashMap<>();
        
        // Basic statistics - using streaming calculations
        double mean = calculateMeanStreaming(prices);
        double volatility = calculateVolatilityStreaming(prices, mean);
        
        results.put("mean", mean);
        results.put("volatility", volatility);
        results.put("trend", analyzeTrendOptimized(prices));
        
        // Wavelet-based analysis
        if (prices.length >= windowSize) {
            results.put("wavelet_decomposition", performWaveletDecompositionOptimized(prices));
            results.put("frequency_analysis", analyzeFrequencyComponentsOptimized(prices));
        }
        
        return results;
    }
    
    /**
     * Streaming calculation of mean to avoid temporary arrays.
     */
    private double calculateMeanStreaming(double[] prices) {
        double sum = 0.0;
        for (double price : prices) {
            sum += price;
        }
        return sum / prices.length;
    }
    
    /**
     * Streaming calculation of volatility.
     */
    private double calculateVolatilityStreaming(double[] prices, double mean) {
        double sumSquaredDiffs = 0.0;
        
        for (double price : prices) {
            double diff = price - mean;
            sumSquaredDiffs += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiffs / (prices.length - 1));
    }
    
    /**
     * Optimized trend analysis - same logic but using streaming calculations.
     */
    private String analyzeTrendOptimized(double[] prices) {
        double n = prices.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < prices.length; i++) {
            sumX += i;
            sumY += prices[i];
            sumXY += i * prices[i];
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        
        if (slope > 0.1) return "BULLISH";
        if (slope < -0.1) return "BEARISH";
        return "SIDEWAYS";
    }
    
    /**
     * Optimized wavelet decomposition - reuses pre-allocated arrays.
     */
    private Map<String, double[]> performWaveletDecompositionOptimized(double[] prices) {
        Map<String, double[]> decomposition = new HashMap<>();
        
        try {
            // Ensure we have power-of-2 length
            int powerOf2Size = 1;
            while (powerOf2Size < prices.length) {
                powerOf2Size *= 2;
            }
            
            if (powerOf2Size > prices.length) {
                powerOf2Size /= 2;
            }
            
            // Reuse pre-allocated analysis data array
            reusableAnalysisData = ensureCapacity(reusableAnalysisData, powerOf2Size);
            
            // Copy data without creating new array
            int startIndex = prices.length - powerOf2Size;
            for (int i = 0; i < powerOf2Size; i++) {
                reusableAnalysisData[i] = prices[startIndex + i];
            }
            
            WaveletTransform transform = factory.create(defaultWavelet);
            
            // If we need only a portion, create a temporary array (still better than the original)
            double[] dataForTransform;
            if (powerOf2Size == reusableAnalysisData.length) {
                dataForTransform = reusableAnalysisData;
            } else {
                dataForTransform = new double[powerOf2Size];
                System.arraycopy(reusableAnalysisData, 0, dataForTransform, 0, powerOf2Size);
            }
            
            TransformResult result = transform.forward(dataForTransform);
            
            decomposition.put("approximation", result.approximationCoeffs());
            decomposition.put("detail", result.detailCoeffs());
            
        } catch (Exception e) {
            // Return empty decomposition on error
            decomposition.put("approximation", new double[0]);
            decomposition.put("detail", new double[0]);
        }
        
        return decomposition;
    }
    
    /**
     * Optimized frequency analysis.
     */
    private Map<String, Double> analyzeFrequencyComponentsOptimized(double[] prices) {
        Map<String, Double> frequencies = new HashMap<>();
        
        // Reuse the decomposition from optimized method
        Map<String, double[]> decomposition = performWaveletDecompositionOptimized(prices);
        double[] details = decomposition.get("detail");
        
        if (details.length > 0) {
            frequencies.put("high_freq_energy", calculateEnergyStreaming(details));
            frequencies.put("dominant_frequency", findDominantFrequencyOptimized(details));
        } else {
            frequencies.put("high_freq_energy", 0.0);
            frequencies.put("dominant_frequency", 0.0);
        }
        
        return frequencies;
    }
    
    /**
     * Streaming energy calculation.
     */
    private double calculateEnergyStreaming(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
    
    /**
     * Optimized dominant frequency finding.
     */
    private double findDominantFrequencyOptimized(double[] signal) {
        if (signal.length == 0) return 0.0;
        
        // Find index of maximum absolute value in single pass
        int maxIndex = 0;
        double maxValue = Math.abs(signal[0]);
        
        for (int i = 1; i < signal.length; i++) {
            double absValue = Math.abs(signal[i]);
            if (absValue > maxValue) {
                maxValue = absValue;
                maxIndex = i;
            }
        }
        
        return (double) maxIndex / signal.length;
    }
    
    /**
     * OPTIMIZED VERSION OF HOT SPOT #1: analyzeVolatility()
     * Uses pre-allocated arrays and avoids creating temporary arrays in loops.
     */
    public VolatilityResult analyzeVolatility(double[] prices, int lookbackWindow) {
        if (prices == null || prices.length < lookbackWindow) {
            throw new IllegalArgumentException("Insufficient data for volatility analysis");
        }
        
        long timestamp = System.currentTimeMillis();
        int returnsLength = prices.length - 1;
        
        // Ensure reusable arrays are large enough
        reusableReturns = ensureCapacity(reusableReturns, returnsLength);
        reusableSquaredReturns = ensureCapacity(reusableSquaredReturns, returnsLength);
        
        // Calculate returns in single pass - no new array allocation
        for (int i = 1; i < prices.length; i++) {
            reusableReturns[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        
        // Calculate squared returns in single pass - reuse array
        for (int i = 0; i < returnsLength; i++) {
            reusableSquaredReturns[i] = reusableReturns[i] * reusableReturns[i];
        }
        
        // Rolling window calculations - avoid creating new arrays in loop
        int rollingLength = Math.max(0, returnsLength - lookbackWindow + 1);
        reusableRollingVolatilities = ensureCapacity(reusableRollingVolatilities, rollingLength);
        
        for (int i = lookbackWindow - 1; i < returnsLength; i++) {
            // Calculate volatility for window without creating new array
            // Use start and end indices instead of copying data
            int windowStart = i - lookbackWindow + 1;
            double volatility = calculateVolatilityFromRange(reusableReturns, windowStart, lookbackWindow);
            reusableRollingVolatilities[i - lookbackWindow + 1] = volatility;
        }
        
        // Optimized wavelet analysis
        double[] detailVolatilities = calculateDetailVolatilitiesOptimized(reusableReturns, returnsLength);
        double[] timeScaleVolatilities = calculateTimeScaleVolatilitiesOptimized(reusableReturns, returnsLength);
        
        // Calculate final volatilities
        double realizedVol = reusableRollingVolatilities[rollingLength - 1];
        double garchVol = estimateGarchVolatilityOptimized(reusableSquaredReturns, returnsLength);
        double waveletVol = calculateWaveletVolatilityOptimized(detailVolatilities);
        
        return new VolatilityResult(realizedVol, garchVol, waveletVol, 
                                   detailVolatilities, timeScaleVolatilities, timestamp);
    }
    
    /**
     * Calculate volatility from array range without copying data.
     */
    private double calculateVolatilityFromRange(double[] returns, int start, int length) {
        double sum = 0.0;
        for (int i = start; i < start + length; i++) {
            sum += returns[i];
        }
        double mean = sum / length;
        
        double sumSquaredDiffs = 0.0;
        for (int i = start; i < start + length; i++) {
            double diff = returns[i] - mean;
            sumSquaredDiffs += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiffs / (length - 1));
    }
    
    /**
     * Optimized detail volatilities calculation.
     */
    private double[] calculateDetailVolatilitiesOptimized(double[] returns, int length) {
        try {
            // Find appropriate power-of-2 size
            int size = 1;
            while (size < length) size *= 2;
            if (size > length) size /= 2;
            
            // Reuse pre-allocated array instead of creating new one
            reusableAnalysisData = ensureCapacity(reusableAnalysisData, size);
            
            // Copy data without creating temporary array
            int startIndex = Math.max(0, length - size);
            for (int i = 0; i < Math.min(size, length); i++) {
                reusableAnalysisData[i] = returns[startIndex + i];
            }
            
            WaveletTransform transform = factory.create(defaultWavelet);
            
            // Create array for transform if needed
            double[] dataForTransform;
            if (size == reusableAnalysisData.length) {
                dataForTransform = reusableAnalysisData;
            } else {
                dataForTransform = new double[size];
                System.arraycopy(reusableAnalysisData, 0, dataForTransform, 0, size);
            }
            
            TransformResult result = transform.forward(dataForTransform);
            
            double[] details = result.detailCoeffs();
            
            // Reuse volatilities array instead of creating new one
            reusableDetailVolatilities = ensureCapacity(reusableDetailVolatilities, details.length);
            
            for (int i = 0; i < details.length; i++) {
                reusableDetailVolatilities[i] = Math.abs(details[i]);
            }
            
            // Return copy of the needed portion
            double[] result_copy = new double[details.length];
            System.arraycopy(reusableDetailVolatilities, 0, result_copy, 0, details.length);
            return result_copy;
            
        } catch (Exception e) {
            return new double[]{0.0};
        }
    }
    
    /**
     * Optimized time scale volatilities calculation.
     */
    private double[] calculateTimeScaleVolatilitiesOptimized(double[] returns, int length) {
        int[] timeScales = {2, 4, 8, 16, 32};
        double[] volatilities = new double[timeScales.length];
        
        for (int i = 0; i < timeScales.length; i++) {
            int scale = timeScales[i];
            if (scale > length) {
                volatilities[i] = 0.0;
                continue;
            }
            
            // Calculate downsampled length
            int downsampledLength = length / scale;
            
            // Reuse downsampled array instead of creating new one
            reusableDownsampled = ensureCapacity(reusableDownsampled, downsampledLength);
            
            // Downsample in-place
            for (int j = 0; j < downsampledLength; j++) {
                double sum = 0.0;
                for (int k = 0; k < scale && j * scale + k < length; k++) {
                    sum += returns[j * scale + k];
                }
                reusableDownsampled[j] = sum / scale;
            }
            
            volatilities[i] = calculateVolatilityFromRange(reusableDownsampled, 0, downsampledLength);
        }
        
        return volatilities;
    }
    
    /**
     * Optimized GARCH volatility estimation.
     */
    private double estimateGarchVolatilityOptimized(double[] squaredReturns, int length) {
        if (length < 10) {
            // Calculate mean directly without creating array
            double sum = 0.0;
            for (int i = 0; i < length; i++) {
                sum += squaredReturns[i];
            }
            return Math.sqrt(sum / length);
        }
        
        // Reuse weights array
        reusableWeights = ensureCapacity(reusableWeights, length);
        double lambda = 0.94; // Decay factor
        
        reusableWeights[length - 1] = 1.0;
        for (int i = length - 2; i >= 0; i--) {
            reusableWeights[i] = reusableWeights[i + 1] * lambda;
        }
        
        // Normalize weights in single pass
        double sum = 0.0;
        for (int i = 0; i < length; i++) sum += reusableWeights[i];
        for (int i = 0; i < length; i++) reusableWeights[i] /= sum;
        
        // Calculate weighted variance
        double weightedVar = 0.0;
        for (int i = 0; i < length; i++) {
            weightedVar += reusableWeights[i] * squaredReturns[i];
        }
        
        return Math.sqrt(weightedVar);
    }
    
    /**
     * Optimized wavelet volatility calculation.
     */
    private double calculateWaveletVolatilityOptimized(double[] detailVolatilities) {
        if (detailVolatilities.length == 0) return 0.0;
        
        // Single pass calculation
        double sum = 0.0;
        for (double vol : detailVolatilities) {
            sum += vol * vol;
        }
        return Math.sqrt(sum / detailVolatilities.length);
    }
    
    /**
     * OPTIMIZED VERSION OF HOT SPOT #2: generateTradingSignals()
     * Uses object pooling and pre-allocated arrays instead of ArrayList.
     */
    public List<TradingSignal> generateTradingSignals(double[] prices, double[] volumes) {
        if (prices == null || prices.length < minPeriods) {
            return new ArrayList<>();
        }
        
        long currentTime = System.currentTimeMillis();
        signalsCount = 0; // Reset signal count
        
        // Reuse price window array instead of creating new one each iteration
        reusablePriceWindow = ensureCapacity(reusablePriceWindow, minPeriods);
        
        for (int i = minPeriods; i < prices.length && signalsCount < MAX_SIGNALS; i++) {
            // Copy window data without creating new array
            System.arraycopy(prices, i - minPeriods, reusablePriceWindow, 0, minPeriods);
            
            // Calculate indicators using reusable array
            double sma = calculateSimpleMovingAverageOptimized(reusablePriceWindow, minPeriods);
            double momentum = calculateMomentumOptimized(reusablePriceWindow, minPeriods);
            double waveletEnergy = calculateWaveletEnergyOptimized(reusablePriceWindow, minPeriods);
            
            // Generate signals based on conditions and use object pooling
            if (momentum > 0.02 && waveletEnergy > confidenceThreshold) {
                TradingSignal signal = signalPool.acquire(
                    TradingSignal.Type.BUY,
                    determineStrength(momentum, waveletEnergy),
                    Math.min(0.95, waveletEnergy),
                    prices[i],
                    currentTime + i * 1000,
                    String.format("Momentum=%.3f, Energy=%.3f", momentum, waveletEnergy)
                );
                signalsBuffer[signalsCount++] = signal;
                
            } else if (momentum < -0.02 && waveletEnergy > confidenceThreshold) {
                TradingSignal signal = signalPool.acquire(
                    TradingSignal.Type.SELL,
                    determineStrength(Math.abs(momentum), waveletEnergy),
                    Math.min(0.95, waveletEnergy),
                    prices[i],
                    currentTime + i * 1000,
                    String.format("Momentum=%.3f, Energy=%.3f", momentum, waveletEnergy)
                );
                signalsBuffer[signalsCount++] = signal;
                
            } else if (Math.abs(momentum) < 0.005) {
                TradingSignal signal = signalPool.acquire(
                    TradingSignal.Type.HOLD,
                    TradingSignal.Strength.WEAK,
                    0.5,
                    prices[i],
                    currentTime + i * 1000,
                    "Low momentum detected"
                );
                signalsBuffer[signalsCount++] = signal;
            }
        }
        
        // Convert buffer to list - only create ArrayList once with known size
        List<TradingSignal> signals = new ArrayList<>(signalsCount);
        for (int i = 0; i < signalsCount; i++) {
            signals.add(signalsBuffer[i]);
        }
        
        return signals;
    }
    
    /**
     * Optimized SMA calculation using pre-allocated array.
     */
    private double calculateSimpleMovingAverageOptimized(double[] prices, int length) {
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += prices[i];
        }
        return sum / length;
    }
    
    /**
     * Optimized momentum calculation.
     */
    private double calculateMomentumOptimized(double[] prices, int length) {
        if (length < 2) return 0.0;
        return (prices[length - 1] - prices[0]) / prices[0];
    }
    
    /**
     * Optimized wavelet energy calculation.
     */
    private double calculateWaveletEnergyOptimized(double[] prices, int length) {
        try {
            // Find power-of-2 size
            int size = 1;
            while (size < length) size *= 2;
            if (size > length) size /= 2;
            
            // Reuse analysis data array
            reusableAnalysisData = ensureCapacity(reusableAnalysisData, size);
            
            // Copy data using start index instead of creating new array
            int startIndex = Math.max(0, length - size);
            for (int i = 0; i < Math.min(size, length); i++) {
                reusableAnalysisData[i] = prices[startIndex + i];
            }
            
            WaveletTransform transform = factory.create(defaultWavelet);
            
            // Create array for transform if needed
            double[] dataForTransform;
            if (size == reusableAnalysisData.length) {
                dataForTransform = reusableAnalysisData;
            } else {
                dataForTransform = new double[size];
                System.arraycopy(reusableAnalysisData, 0, dataForTransform, 0, size);
            }
            
            TransformResult result = transform.forward(dataForTransform);
            
            // Calculate energy in detail coefficients
            double[] details = result.detailCoeffs();
            double energy = 0.0;
            for (double coeff : details) {
                energy += coeff * coeff;
            }
            
            return Math.sqrt(energy / details.length);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Determine signal strength based on momentum and energy.
     */
    private TradingSignal.Strength determineStrength(double momentum, double energy) {
        double combined = momentum + energy;
        if (combined > 1.5) return TradingSignal.Strength.STRONG;
        if (combined > 0.8) return TradingSignal.Strength.MODERATE;
        return TradingSignal.Strength.WEAK;
    }
    
    // Additional optimized methods following the same pattern...
    
    /**
     * Analyze market regime using optimized wavelet decomposition.
     */
    public String analyzeMarketRegime(double[] prices) {
        if (prices.length < windowSize) {
            return "INSUFFICIENT_DATA";
        }
        
        Map<String, double[]> decomposition = performWaveletDecompositionOptimized(prices);
        double[] approximation = decomposition.get("approximation");
        double[] detail = decomposition.get("detail");
        
        if (approximation.length == 0 || detail.length == 0) {
            return "ANALYSIS_ERROR";
        }
        
        // Calculate trend from approximation coefficients
        double trendStrength = calculateTrendStrengthOptimized(approximation);
        
        // Calculate volatility from detail coefficients  
        double volatilityLevel = calculateVolatilityLevelOptimized(detail);
        
        // Determine regime
        if (trendStrength > 0.7 && volatilityLevel < 0.3) {
            return "TRENDING_LOW_VOL";
        } else if (trendStrength > 0.7 && volatilityLevel > 0.7) {
            return "TRENDING_HIGH_VOL";
        } else if (trendStrength < 0.3 && volatilityLevel > 0.7) {
            return "RANGING_HIGH_VOL";
        } else {
            return "RANGING_LOW_VOL";
        }
    }
    
    private double calculateTrendStrengthOptimized(double[] approximation) {
        if (approximation.length < 2) return 0.0;
        
        double totalChange = Math.abs(approximation[approximation.length - 1] - approximation[0]);
        double totalVariation = 0.0;
        
        for (int i = 1; i < approximation.length; i++) {
            totalVariation += Math.abs(approximation[i] - approximation[i - 1]);
        }
        
        return totalVariation > 0 ? totalChange / totalVariation : 0.0;
    }
    
    private double calculateVolatilityLevelOptimized(double[] detail) {
        if (detail.length == 0) return 0.0;
        
        double energy = 0.0;
        for (double coeff : detail) {
            energy += coeff * coeff;
        }
        
        return Math.sqrt(energy / detail.length);
    }
    
    /**
     * Get pool statistics for monitoring.
     */
    public Map<String, Integer> getPoolStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("signal_pool_size", signalPool.getPoolSize());
        stats.put("reusable_arrays_count", 12); // Number of pre-allocated arrays
        return stats;
    }
}