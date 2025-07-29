package ai.prophetizo.financial;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Financial wavelet analyzer for volatility analysis and trading signal generation.
 * This version has memory allocation issues that need optimization.
 */
public class FinancialWaveletAnalyzer {
    
    private final WaveletTransformFactory factory;
    private final Wavelet defaultWavelet;
    
    // Configuration parameters
    private final int windowSize;
    private final int minPeriods;
    private final double confidenceThreshold;
    
    public FinancialWaveletAnalyzer() {
        this(256, 20, 0.7);
    }
    
    public FinancialWaveletAnalyzer(int windowSize, int minPeriods, double confidenceThreshold) {
        this.windowSize = windowSize;
        this.minPeriods = minPeriods;
        this.confidenceThreshold = confidenceThreshold;
        this.factory = new WaveletTransformFactory().withBoundaryMode(BoundaryMode.PERIODIC);
        this.defaultWavelet = Daubechies.DB4;
    }
    
    /**
     * Analyzes price data for various trading patterns and signals.
     */
    public Map<String, Object> analyzePriceData(double[] prices) {
        if (prices == null || prices.length < minPeriods) {
            throw new IllegalArgumentException("Insufficient price data");
        }
        
        Map<String, Object> results = new HashMap<>();
        
        // Basic statistics
        results.put("mean", calculateMean(prices));
        results.put("volatility", calculateVolatility(prices));
        results.put("trend", analyzeTrend(prices));
        
        // Wavelet-based analysis
        if (prices.length >= windowSize) {
            results.put("wavelet_decomposition", performWaveletDecomposition(prices));
            results.put("frequency_analysis", analyzeFrequencyComponents(prices));
        }
        
        return results;
    }
    
    /**
     * Calculates basic price statistics.
     */
    private double calculateMean(double[] prices) {
        double sum = 0.0;
        for (double price : prices) {
            sum += price;
        }
        return sum / prices.length;
    }
    
    /**
     * Basic volatility calculation using standard deviation.
     */
    private double calculateVolatility(double[] prices) {
        double mean = calculateMean(prices);
        double sumSquaredDiffs = 0.0;
        
        for (double price : prices) {
            double diff = price - mean;
            sumSquaredDiffs += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiffs / (prices.length - 1));
    }
    
    /**
     * Analyzes price trend using linear regression.
     */
    private String analyzeTrend(double[] prices) {
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
     * Performs wavelet decomposition on price data.
     */
    private Map<String, double[]> performWaveletDecomposition(double[] prices) {
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
            
            // Extract subset of data
            double[] analysisData = new double[powerOf2Size];
            System.arraycopy(prices, prices.length - powerOf2Size, analysisData, 0, powerOf2Size);
            
            WaveletTransform transform = factory.create(defaultWavelet);
            TransformResult result = transform.forward(analysisData);
            
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
     * Analyzes frequency components in the price data.
     */
    private Map<String, Double> analyzeFrequencyComponents(double[] prices) {
        Map<String, Double> frequencies = new HashMap<>();
        
        // Simple frequency analysis using wavelet coefficients  
        Map<String, double[]> decomposition = performWaveletDecomposition(prices);
        double[] details = decomposition.get("detail");
        
        if (details.length > 0) {
            frequencies.put("high_freq_energy", calculateEnergy(details));
            frequencies.put("dominant_frequency", findDominantFrequency(details));
        } else {
            frequencies.put("high_freq_energy", 0.0);
            frequencies.put("dominant_frequency", 0.0);
        }
        
        return frequencies;
    }
    
    /**
     * Calculates energy in a signal.
     */
    private double calculateEnergy(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
    
    /**
     * Finds dominant frequency in signal (simplified).
     */
    private double findDominantFrequency(double[] signal) {
        if (signal.length == 0) return 0.0;
        
        // Find index of maximum absolute value
        int maxIndex = 0;
        double maxValue = Math.abs(signal[0]);
        
        for (int i = 1; i < signal.length; i++) {
            double absValue = Math.abs(signal[i]);
            if (absValue > maxValue) {
                maxValue = absValue;
                maxIndex = i;
            }
        }
        
        // Convert index to frequency (simplified)
        return (double) maxIndex / signal.length;
    }
    
    /**
     * MEMORY ALLOCATION HOT SPOT #1: analyzeVolatility() - lines 209-241
     * Creates many temporary arrays for each call, causing GC pressure.
     */
    public VolatilityResult analyzeVolatility(double[] prices, int lookbackWindow) {
        if (prices == null || prices.length < lookbackWindow) {
            throw new IllegalArgumentException("Insufficient data for volatility analysis");
        }
        
        long timestamp = System.currentTimeMillis();
        
        // MEMORY ISSUE: Creating new arrays on every call
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        
        // MEMORY ISSUE: More temporary arrays
        double[] squaredReturns = new double[returns.length];
        for (int i = 0; i < returns.length; i++) {
            squaredReturns[i] = returns[i] * returns[i];
        }
        
        // MEMORY ISSUE: Rolling window calculations create many temp arrays
        double[] rollingVolatilities = new double[Math.max(0, returns.length - lookbackWindow + 1)];
        for (int i = lookbackWindow - 1; i < returns.length; i++) {
            double[] windowReturns = new double[lookbackWindow]; // New array each iteration!
            System.arraycopy(returns, i - lookbackWindow + 1, windowReturns, 0, lookbackWindow);
            rollingVolatilities[i - lookbackWindow + 1] = calculateVolatility(windowReturns);
        }
        
        // MEMORY ISSUE: Wavelet analysis creates more temporary arrays
        double[] detailVolatilities = calculateDetailVolatilities(returns);
        double[] timeScaleVolatilities = calculateTimeScaleVolatilities(returns);
        
        // Calculate final volatilities
        double realizedVol = rollingVolatilities[rollingVolatilities.length - 1];
        double garchVol = estimateGarchVolatility(squaredReturns);
        double waveletVol = calculateWaveletVolatility(detailVolatilities);
        
        return new VolatilityResult(realizedVol, garchVol, waveletVol, 
                                   detailVolatilities, timeScaleVolatilities, timestamp);
    }
    
    /**
     * Helper method with memory allocation issues.
     */
    private double[] calculateDetailVolatilities(double[] returns) {
        try {
            // Find appropriate power-of-2 size
            int size = 1;
            while (size < returns.length) size *= 2;
            if (size > returns.length) size /= 2;
            
            // MEMORY ISSUE: Creating temporary arrays
            double[] paddedReturns = new double[size];
            System.arraycopy(returns, Math.max(0, returns.length - size), paddedReturns, 0, 
                           Math.min(size, returns.length));
            
            WaveletTransform transform = factory.create(defaultWavelet);
            TransformResult result = transform.forward(paddedReturns);
            
            double[] details = result.detailCoeffs();
            double[] volatilities = new double[details.length]; // Another temp array!
            
            for (int i = 0; i < details.length; i++) {
                volatilities[i] = Math.abs(details[i]);
            }
            
            return volatilities;
        } catch (Exception e) {
            return new double[]{0.0}; // Fallback allocation
        }
    }
    
    /**
     * More helper methods with allocations.
     */
    private double[] calculateTimeScaleVolatilities(double[] returns) {
        // MEMORY ISSUE: Multiple time scale analyses create many arrays
        int[] timeScales = {2, 4, 8, 16, 32};
        double[] volatilities = new double[timeScales.length];
        
        for (int i = 0; i < timeScales.length; i++) {
            int scale = timeScales[i];
            if (scale > returns.length) {
                volatilities[i] = 0.0;
                continue;
            }
            
            // MEMORY ISSUE: Creating downsampled array for each scale
            double[] downsampled = new double[returns.length / scale];
            for (int j = 0; j < downsampled.length; j++) {
                double sum = 0.0;
                for (int k = 0; k < scale && j * scale + k < returns.length; k++) {
                    sum += returns[j * scale + k];
                }
                downsampled[j] = sum / scale;
            }
            
            volatilities[i] = calculateVolatility(downsampled);
        }
        
        return volatilities;
    }
    
    /**
     * GARCH volatility estimation with memory issues.
     */
    private double estimateGarchVolatility(double[] squaredReturns) {
        if (squaredReturns.length < 10) return Math.sqrt(calculateMean(squaredReturns));
        
        // MEMORY ISSUE: Creating arrays for GARCH estimation
        double[] weights = new double[squaredReturns.length];
        double lambda = 0.94; // Decay factor
        
        weights[weights.length - 1] = 1.0;
        for (int i = weights.length - 2; i >= 0; i--) {
            weights[i] = weights[i + 1] * lambda;
        }
        
        // Normalize weights
        double sum = 0.0;
        for (double weight : weights) sum += weight;
        for (int i = 0; i < weights.length; i++) weights[i] /= sum;
        
        // Calculate weighted variance
        double weightedVar = 0.0;
        for (int i = 0; i < squaredReturns.length; i++) {
            weightedVar += weights[i] * squaredReturns[i];
        }
        
        return Math.sqrt(weightedVar);
    }
    
    /**
     * Calculate wavelet-based volatility.
     */
    private double calculateWaveletVolatility(double[] detailVolatilities) {
        if (detailVolatilities.length == 0) return 0.0;
        
        double sum = 0.0;
        for (double vol : detailVolatilities) {
            sum += vol * vol;
        }
        return Math.sqrt(sum / detailVolatilities.length);
    }
    
    /**
     * MEMORY ALLOCATION HOT SPOT #2: generateTradingSignals() - lines 415-449  
     * Creates many TradingSignal objects, causing GC pressure.
     */
    public List<TradingSignal> generateTradingSignals(double[] prices, double[] volumes) {
        if (prices == null || prices.length < minPeriods) {
            return new ArrayList<>(); // MEMORY ISSUE: Creating empty list each time
        }
        
        long currentTime = System.currentTimeMillis();
        List<TradingSignal> signals = new ArrayList<>(); // MEMORY ISSUE: Dynamic resizing
        
        // MEMORY ISSUE: Many TradingSignal object creations in loops
        for (int i = minPeriods; i < prices.length; i++) {
            // Extract window for analysis
            double[] priceWindow = new double[minPeriods]; // MEMORY ISSUE: New array each iteration
            System.arraycopy(prices, i - minPeriods, priceWindow, 0, minPeriods);
            
            // Calculate indicators
            double sma = calculateSimpleMovingAverage(priceWindow);
            double momentum = calculateMomentum(priceWindow);
            double waveletEnergy = calculateWaveletEnergy(priceWindow);
            
            // Generate signals based on conditions
            if (momentum > 0.02 && waveletEnergy > confidenceThreshold) {
                // MEMORY ISSUE: Creating new TradingSignal object
                TradingSignal signal = new TradingSignal(
                    TradingSignal.Type.BUY,
                    determineStrength(momentum, waveletEnergy),
                    Math.min(0.95, waveletEnergy),
                    prices[i],
                    currentTime + i * 1000,
                    String.format("Momentum=%.3f, Energy=%.3f", momentum, waveletEnergy)
                );
                signals.add(signal);
            } else if (momentum < -0.02 && waveletEnergy > confidenceThreshold) {
                // MEMORY ISSUE: Another TradingSignal object
                TradingSignal signal = new TradingSignal(
                    TradingSignal.Type.SELL,
                    determineStrength(Math.abs(momentum), waveletEnergy),
                    Math.min(0.95, waveletEnergy),
                    prices[i],
                    currentTime + i * 1000,
                    String.format("Momentum=%.3f, Energy=%.3f", momentum, waveletEnergy)
                );
                signals.add(signal);
            } else if (Math.abs(momentum) < 0.005) {
                // MEMORY ISSUE: Yet another TradingSignal object  
                TradingSignal signal = new TradingSignal(
                    TradingSignal.Type.HOLD,
                    TradingSignal.Strength.WEAK,
                    0.5,
                    prices[i],
                    currentTime + i * 1000,
                    "Low momentum detected"
                );
                signals.add(signal);
            }
        }
        
        return signals;
    }
    
    /**
     * Calculate simple moving average.
     */
    private double calculateSimpleMovingAverage(double[] prices) {
        double sum = 0.0;
        for (double price : prices) {
            sum += price;
        }
        return sum / prices.length;
    }
    
    /**
     * Calculate price momentum.
     */
    private double calculateMomentum(double[] prices) {
        if (prices.length < 2) return 0.0;
        return (prices[prices.length - 1] - prices[0]) / prices[0];
    }
    
    /**
     * Calculate wavelet energy for trading signals.
     */
    private double calculateWaveletEnergy(double[] prices) {
        try {
            // Find power-of-2 size
            int size = 1;
            while (size < prices.length) size *= 2;
            if (size > prices.length) size /= 2;
            
            // MEMORY ISSUE: Temporary array creation  
            double[] analysisData = new double[size];
            System.arraycopy(prices, Math.max(0, prices.length - size), analysisData, 0, 
                           Math.min(size, prices.length));
            
            WaveletTransform transform = factory.create(defaultWavelet);
            TransformResult result = transform.forward(analysisData);
            
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
    
    /**
     * Analyze market regime using wavelet decomposition.
     */
    public String analyzeMarketRegime(double[] prices) {
        if (prices.length < windowSize) {
            return "INSUFFICIENT_DATA";
        }
        
        Map<String, double[]> decomposition = performWaveletDecomposition(prices);
        double[] approximation = decomposition.get("approximation");
        double[] detail = decomposition.get("detail");
        
        if (approximation.length == 0 || detail.length == 0) {
            return "ANALYSIS_ERROR";
        }
        
        // Calculate trend from approximation coefficients
        double trendStrength = calculateTrendStrength(approximation);
        
        // Calculate volatility from detail coefficients  
        double volatilityLevel = calculateVolatilityLevel(detail);
        
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
    
    /**
     * Calculate trend strength from approximation coefficients.
     */
    private double calculateTrendStrength(double[] approximation) {
        if (approximation.length < 2) return 0.0;
        
        double totalChange = Math.abs(approximation[approximation.length - 1] - approximation[0]);
        double totalVariation = 0.0;
        
        for (int i = 1; i < approximation.length; i++) {
            totalVariation += Math.abs(approximation[i] - approximation[i - 1]);
        }
        
        return totalVariation > 0 ? totalChange / totalVariation : 0.0;
    }
    
    /**
     * Calculate volatility level from detail coefficients.
     */
    private double calculateVolatilityLevel(double[] detail) {
        if (detail.length == 0) return 0.0;
        
        double energy = 0.0;
        for (double coeff : detail) {
            energy += coeff * coeff;
        }
        
        return Math.sqrt(energy / detail.length);
    }
    
    /**
     * Multi-timeframe analysis with multiple wavelet decompositions.
     */
    public Map<String, Object> performMultiTimeframeAnalysis(double[] prices) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Analyze at different scales
        int[] timeframes = {64, 128, 256, 512};
        for (int timeframe : timeframes) {
            if (prices.length >= timeframe) {
                // MEMORY ISSUE: Creating subset arrays for each timeframe  
                double[] subset = new double[timeframe];
                System.arraycopy(prices, prices.length - timeframe, subset, 0, timeframe);
                
                Map<String, Object> timeframeAnalysis = new HashMap<>();
                timeframeAnalysis.put("regime", analyzeMarketRegime(subset));
                timeframeAnalysis.put("volatility", analyzeVolatility(subset, Math.min(20, timeframe / 4)));
                timeframeAnalysis.put("signals", generateTradingSignals(subset, generateMockVolumes(subset.length)));
                
                analysis.put("timeframe_" + timeframe, timeframeAnalysis);
            }
        }
        
        return analysis;
    }
    
    /**
     * Generate mock volume data for testing.
     */
    private double[] generateMockVolumes(int length) {
        double[] volumes = new double[length]; // MEMORY ISSUE: Always creating new array
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 0; i < length; i++) {
            volumes[i] = random.nextDouble(1000, 10000);
        }
        
        return volumes;
    }
    
    /**
     * Risk analysis using wavelet variance decomposition.
     */
    public Map<String, Double> analyzeRisk(double[] prices, double[] benchmarkPrices) {
        Map<String, Double> riskMetrics = new HashMap<>();
        
        if (prices.length != benchmarkPrices.length) {
            throw new IllegalArgumentException("Price arrays must have same length");
        }
        
        // Calculate returns
        double[] returns = new double[prices.length - 1];
        double[] benchmarkReturns = new double[benchmarkPrices.length - 1];
        
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
            benchmarkReturns[i - 1] = Math.log(benchmarkPrices[i] / benchmarkPrices[i - 1]);
        }
        
        // Basic risk metrics
        riskMetrics.put("volatility", calculateVolatility(returns));
        riskMetrics.put("beta", calculateBeta(returns, benchmarkReturns));
        riskMetrics.put("var_95", calculateVaR(returns, 0.95));
        riskMetrics.put("max_drawdown", calculateMaxDrawdown(prices));
        
        // Wavelet-based risk decomposition
        if (returns.length >= 64) {
            Map<String, Double> waveletRisk = calculateWaveletRiskDecomposition(returns);
            riskMetrics.putAll(waveletRisk);
        }
        
        return riskMetrics;
    }
    
    /**
     * Calculate beta coefficient.
     */
    private double calculateBeta(double[] returns, double[] benchmarkReturns) {
        double meanReturn = calculateMean(returns);
        double meanBenchmark = calculateMean(benchmarkReturns);
        
        double covariance = 0.0;
        double benchmarkVariance = 0.0;
        
        for (int i = 0; i < returns.length; i++) {
            double returnDiff = returns[i] - meanReturn;
            double benchmarkDiff = benchmarkReturns[i] - meanBenchmark;
            
            covariance += returnDiff * benchmarkDiff;
            benchmarkVariance += benchmarkDiff * benchmarkDiff;
        }
        
        return benchmarkVariance > 0 ? covariance / benchmarkVariance : 0.0;
    }
    
    /**
     * Calculate Value at Risk.
     */
    private double calculateVaR(double[] returns, double confidence) {
        // MEMORY ISSUE: Creating sorted copy of returns array
        double[] sortedReturns = returns.clone();
        Arrays.sort(sortedReturns);
        
        int index = (int) ((1.0 - confidence) * sortedReturns.length);
        return index < sortedReturns.length ? -sortedReturns[index] : 0.0;
    }
    
    /**
     * Calculate maximum drawdown.
     */
    private double calculateMaxDrawdown(double[] prices) {
        double maxDrawdown = 0.0;
        double peak = prices[0];
        
        for (int i = 1; i < prices.length; i++) {
            if (prices[i] > peak) {
                peak = prices[i];
            } else {
                double drawdown = (peak - prices[i]) / peak;
                maxDrawdown = Math.max(maxDrawdown, drawdown);
            }
        }
        
        return maxDrawdown;
    }
    
    /**
     * MEMORY ALLOCATION HOT SPOT #3: Helper methods - lines 783-823
     * Temporary array allocations in wavelet risk calculations.
     */
    private Map<String, Double> calculateWaveletRiskDecomposition(double[] returns) {
        Map<String, Double> riskDecomp = new HashMap<>();
        
        try {
            // Find power-of-2 size  
            int size = 1;
            while (size < returns.length) size *= 2;
            if (size > returns.length) size /= 2;
            
            // MEMORY ISSUE: Creating temporary array for analysis
            double[] analysisReturns = new double[size];
            System.arraycopy(returns, Math.max(0, returns.length - size), analysisReturns, 0, 
                           Math.min(size, returns.length));
            
            WaveletTransform transform = factory.create(defaultWavelet);
            TransformResult result = transform.forward(analysisReturns);
            
            double[] approximation = result.approximationCoeffs();
            double[] detail = result.detailCoeffs();
            
            // MEMORY ISSUE: Creating arrays for each risk component calculation
            double[] trendRisk = new double[approximation.length];
            for (int i = 0; i < approximation.length; i++) {
                trendRisk[i] = approximation[i] * approximation[i];
            }
            
            double[] noiseRisk = new double[detail.length]; 
            for (int i = 0; i < detail.length; i++) {
                noiseRisk[i] = detail[i] * detail[i];
            }
            
            // Calculate risk contributions
            double totalTrendRisk = 0.0;
            for (double risk : trendRisk) totalTrendRisk += risk;
            
            double totalNoiseRisk = 0.0;
            for (double risk : noiseRisk) totalNoiseRisk += risk;
            
            double totalRisk = totalTrendRisk + totalNoiseRisk;
            
            if (totalRisk > 0) {
                riskDecomp.put("trend_risk_contribution", totalTrendRisk / totalRisk);
                riskDecomp.put("noise_risk_contribution", totalNoiseRisk / totalRisk);
            } else {
                riskDecomp.put("trend_risk_contribution", 0.5);
                riskDecomp.put("noise_risk_contribution", 0.5);
            }
            
            riskDecomp.put("wavelet_total_risk", Math.sqrt(totalRisk / size));
            
        } catch (Exception e) {
            // Fallback values
            riskDecomp.put("trend_risk_contribution", 0.5);
            riskDecomp.put("noise_risk_contribution", 0.5);
            riskDecomp.put("wavelet_total_risk", 0.0);
        }
        
        return riskDecomp;
    }
    
    /**
     * Portfolio optimization using wavelet analysis.
     */
    public Map<String, Object> optimizePortfolio(double[][] assetPrices, double[] expectedReturns, double riskTolerance) {
        Map<String, Object> optimization = new HashMap<>();
        
        int numAssets = assetPrices.length;
        if (numAssets == 0 || expectedReturns.length != numAssets) {
            throw new IllegalArgumentException("Invalid asset data");
        }
        
        // MEMORY ISSUE: Creating covariance matrix and intermediate calculations
        double[][] covarianceMatrix = new double[numAssets][numAssets];
        
        // Calculate asset returns for each asset
        for (int i = 0; i < numAssets; i++) {
            for (int j = i; j < numAssets; j++) {
                double[] returnsI = calculateReturns(assetPrices[i]);
                double[] returnsJ = calculateReturns(assetPrices[j]);
                
                double covariance = calculateCovariance(returnsI, returnsJ);
                covarianceMatrix[i][j] = covariance;
                covarianceMatrix[j][i] = covariance; // Symmetric matrix
            }
        }
        
        // Simple equal-weight portfolio as baseline
        double[] weights = new double[numAssets];
        Arrays.fill(weights, 1.0 / numAssets);
        
        // Calculate portfolio metrics
        double portfolioReturn = 0.0;
        for (int i = 0; i < numAssets; i++) {
            portfolioReturn += weights[i] * expectedReturns[i];
        }
        
        double portfolioVolatility = calculatePortfolioVolatility(weights, covarianceMatrix);
        double sharpeRatio = portfolioReturn / portfolioVolatility;
        
        optimization.put("weights", weights);
        optimization.put("expected_return", portfolioReturn);
        optimization.put("volatility", portfolioVolatility);
        optimization.put("sharpe_ratio", sharpeRatio);
        
        return optimization;
    }
    
    /**
     * Calculate returns from prices.
     */
    private double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1]; // MEMORY ISSUE: New array allocation
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        return returns;
    }
    
    /**
     * Calculate covariance between two return series.
     */
    private double calculateCovariance(double[] returnsA, double[] returnsB) {
        if (returnsA.length != returnsB.length) {
            throw new IllegalArgumentException("Return series must have same length");
        }
        
        double meanA = calculateMean(returnsA);
        double meanB = calculateMean(returnsB);
        
        double covariance = 0.0;
        for (int i = 0; i < returnsA.length; i++) {
            covariance += (returnsA[i] - meanA) * (returnsB[i] - meanB);
        }
        
        return covariance / (returnsA.length - 1);
    }
    
    /**
     * Calculate portfolio volatility given weights and covariance matrix.
     */
    private double calculatePortfolioVolatility(double[] weights, double[][] covarianceMatrix) {
        double variance = 0.0;
        
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights.length; j++) {
                variance += weights[i] * weights[j] * covarianceMatrix[i][j];
            }
        }
        
        return Math.sqrt(variance);
=======
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.BoundaryMode;

/**
 * Financial analysis using wavelet transforms.
 * Provides methods for calculating risk-adjusted returns and other financial metrics
 * using wavelet-based signal processing techniques.
 */
public class FinancialWaveletAnalyzer {
    
    private final FinancialConfig config;
    private final WaveletTransform transform;
    
    /**
     * Creates a financial analyzer with default configuration and Haar wavelet.
     */
    public FinancialWaveletAnalyzer() {
        this(new FinancialConfig());
    }
    
    /**
     * Creates a financial analyzer with specified configuration and default Haar wavelet.
     * 
     * @param config the financial configuration containing risk-free rate and other parameters
     * @throws IllegalArgumentException if config is null
     */
    public FinancialWaveletAnalyzer(FinancialConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Financial configuration cannot be null");
        }
        this.config = config;
        this.transform = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC)
                .create(new Haar());
    }
    
    /**
     * Creates a financial analyzer with specified configuration and wavelet transform.
     * 
     * @param config the financial configuration
     * @param transform the wavelet transform to use for signal processing
     * @throws IllegalArgumentException if config or transform is null
     */
    public FinancialWaveletAnalyzer(FinancialConfig config, WaveletTransform transform) {
        if (config == null) {
            throw new IllegalArgumentException("Financial configuration cannot be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("Wavelet transform cannot be null");
        }
        this.config = config;
        this.transform = transform;
    }
    
    /**
     * Calculates the Sharpe ratio for the given returns using the configured risk-free rate.
     * The Sharpe ratio is calculated as (mean_return - risk_free_rate) / std_deviation.
     * 
     * @param returns the array of returns (as decimals, e.g., 0.05 for 5% return)
     * @return the Sharpe ratio
     * @throws IllegalArgumentException if returns is null, empty, or contains insufficient data
     */
    public double calculateSharpeRatio(double[] returns) {
        return calculateSharpeRatio(returns, config.getRiskFreeRate());
    }
    
    /**
     * Calculates the Sharpe ratio for the given returns using a specified risk-free rate.
     * The Sharpe ratio is calculated as (mean_return - risk_free_rate) / std_deviation.
     * 
     * @param returns the array of returns (as decimals, e.g., 0.05 for 5% return)
     * @param riskFreeRate the risk-free rate to use (as annual decimal)
     * @return the Sharpe ratio
     * @throws IllegalArgumentException if returns is null, empty, or contains insufficient data
     */
    public double calculateSharpeRatio(double[] returns, double riskFreeRate) {
        if (returns == null) {
            throw new IllegalArgumentException("Returns array cannot be null");
        }
        if (returns.length == 0) {
            throw new IllegalArgumentException("Returns array cannot be empty");
        }
        if (returns.length == 1) {
            throw new IllegalArgumentException("At least 2 returns are required to calculate standard deviation");
        }
        
        // Calculate mean return
        double mean = calculateMean(returns);
        
        // Calculate standard deviation
        double stdDev = calculateStandardDeviation(returns, mean);
        
        // Handle zero standard deviation case
        if (stdDev == 0.0) {
            if (mean == riskFreeRate) {
                return 0.0; // No excess return, no risk
            }
            return mean > riskFreeRate ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        
        // Calculate Sharpe ratio
        return (mean - riskFreeRate) / stdDev;
    }
    
    /**
     * Calculates wavelet-denoised Sharpe ratio by first applying wavelet transform
     * to filter out noise from the returns series.
     * 
     * @param returns the array of returns (must be power of 2 length for wavelet transform)
     * @return the Sharpe ratio calculated from denoised returns
     * @throws IllegalArgumentException if returns is null, empty, or not power of 2 length
     */
    public double calculateWaveletSharpeRatio(double[] returns) {
        return calculateWaveletSharpeRatio(returns, config.getRiskFreeRate());
    }
    
    /**
     * Calculates wavelet-denoised Sharpe ratio using specified risk-free rate.
     * 
     * @param returns the array of returns (must be power of 2 length for wavelet transform)
     * @param riskFreeRate the risk-free rate to use
     * @return the Sharpe ratio calculated from denoised returns
     * @throws IllegalArgumentException if returns is null, empty, or not power of 2 length
     */
    public double calculateWaveletSharpeRatio(double[] returns, double riskFreeRate) {
        if (returns == null) {
            throw new IllegalArgumentException("Returns array cannot be null");
        }
        if (returns.length == 0) {
            throw new IllegalArgumentException("Returns array cannot be empty");
        }
        if (!isPowerOfTwo(returns.length)) {
            throw new IllegalArgumentException("Returns array length must be a power of 2 for wavelet transform: " + returns.length);
        }
        
        // Apply wavelet transform for denoising
        TransformResult result = transform.forward(returns);
        
        // Simple denoising strategy: remove high-frequency noise by zeroing detail coefficients
        // This preserves the main trend (approximation) while eliminating short-term fluctuations
        // Note: This is a basic denoising approach. For more sophisticated denoising,
        // consider using soft/hard thresholding on detail coefficients instead of zeroing them.
        double[] approxCoeffs = result.approximationCoeffs();
        double[] detailCoeffs = result.detailCoeffs();
        
        // Create zero-filled detail coefficients array of the same length
        // The TransformResult.create method requires matching array lengths
        double[] zeroDetails = new double[detailCoeffs.length];
        
        // Create a new TransformResult with original approximation and zeroed details
        // Note: approxCoeffs and detailCoeffs have the same length by design from the forward transform
        TransformResult denoisedResult = TransformResult.create(approxCoeffs, zeroDetails);
        
        // Perform inverse transform to get denoised signal
        double[] denoisedReturns = transform.inverse(denoisedResult);
        
        return calculateSharpeRatio(denoisedReturns, riskFreeRate);
    }
    
    /**
     * Returns the current financial configuration.
     * 
     * @return the financial configuration
     */
    public FinancialConfig getConfig() {
        return config;
    }
    
    /**
     * Returns the wavelet transform being used.
     * 
     * @return the wavelet transform
     */
    public WaveletTransform getTransform() {
        return transform;
    }
    
    // Private helper methods
    
    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    private double calculateStandardDeviation(double[] values, double mean) {
        double sumSquaredDiffs = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / (values.length - 1)); // Sample standard deviation
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}