package ai.prophetizo.wavelet.cwt.finance;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.api.ContinuousWavelet;

import java.util.*;

import static ai.prophetizo.wavelet.cwt.finance.FinancialAnalysisConfig.*;

/**
 * Comprehensive financial analysis using specialized wavelets.
 * 
 * <p>This analyzer combines Paul, DOG, and Shannon wavelets to provide
 * advanced financial market analysis including:</p>
 * <ul>
 *   <li>Market crash detection and prediction</li>
 *   <li>Volatility clustering analysis</li>
 *   <li>Cyclical pattern identification</li>
 *   <li>Trading signal generation</li>
 *   <li>Risk assessment</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class FinancialWaveletAnalyzer {
    
    // Analysis result classes
    
    public record CrashDetectionResult(
        List<Integer> crashPoints,
        double[] severity,
        double maxSeverity,
        Map<Integer, Double> crashProbabilities
    ) {
        public List<Integer> getCrashPoints() { return crashPoints; }
        public double getMaxSeverity() { return maxSeverity; }
    }
    
    public enum VolatilityLevel {
        LOW, MEDIUM, HIGH, EXTREME
    }
    
    public record VolatilityCluster(
        int startIndex,
        int endIndex,
        VolatilityLevel level,
        double averageVolatility
    ) {}
    
    public record VolatilityAnalysisResult(
        List<VolatilityCluster> volatilityClusters,
        double[] instantaneousVolatility,
        double averageVolatility,
        double maxVolatility
    ) {
        public List<VolatilityCluster> getVolatilityClusters() { return volatilityClusters; }
    }
    
    public record MarketCycle(
        double period,
        double frequency,
        double strength,
        double phase
    ) {}
    
    public record CyclicalAnalysisResult(
        List<MarketCycle> dominantCycles,
        double[] spectralDensity,
        Map<Double, Double> periodogram
    ) {
        public List<MarketCycle> getDominantCycles() { return dominantCycles; }
    }
    
    public enum MarketRegime {
        TRENDING_UP, TRENDING_DOWN, RANGING, VOLATILE
    }
    
    public enum AnomalyType {
        PRICE_SPIKE, VOLUME_SPIKE, VOLUME_PRICE_DIVERGENCE, UNUSUAL_PATTERN
    }
    
    public record MarketAnomaly(
        int timeIndex,
        AnomalyType type,
        double severity,
        String description
    ) {}
    
    public record MarketAnalysisResult(
        List<Integer> regimeChanges,
        List<MarketAnomaly> anomalies,
        double currentRiskLevel,
        double maxDrawdown,
        Map<Integer, MarketRegime> regimeMap
    ) {
        public List<Integer> getRegimeChanges() { return regimeChanges; }
        public List<MarketAnomaly> getAnomalies() { return anomalies; }
        public double getCurrentRiskLevel() { return currentRiskLevel; }
        public double getMaxDrawdown() { return maxDrawdown; }
    }
    
    public enum SignalType {
        BUY, SELL, HOLD
    }
    
    public record TradingSignal(
        int timeIndex,
        SignalType type,
        double confidence,
        String rationale
    ) {}
    
    public record TradingSignalResult(
        List<TradingSignal> signals,
        double sharpeRatio,
        double winRate
    ) {
        public List<TradingSignal> getSignals() { return signals; }
    }
    
    public record WaveletIndicators(
        double[] trendStrength,
        double[] momentum,
        double[] volatilityIndex,
        double[] supportResistance
    ) {
        public double[] getTrendStrength() { return trendStrength; }
        public double[] getMomentum() { return momentum; }
        public double[] getVolatilityIndex() { return volatilityIndex; }
        public double[] getSupportResistance() { return supportResistance; }
    }
    
    public enum AnalysisObjective {
        CRASH_DETECTION, VOLATILITY_ANALYSIS, CYCLE_DETECTION, SIGNAL_GENERATION
    }
    
    public record OptimalParameters(
        int paulOrder,
        int dogOrder,
        double shannonBandwidth,
        double shannonCenterFreq,
        double[] scaleRange
    ) {
        public int getPaulOrder() { return paulOrder; }
        public int getDogOrder() { return dogOrder; }
        public double getShannonBandwidth() { return shannonBandwidth; }
        public double[] getScaleRange() { return scaleRange; }
    }
    
    // Configuration
    private final CWTConfig config;
    
    public FinancialWaveletAnalyzer() {
        this(CWTConfig.defaultConfig());
    }
    
    public FinancialWaveletAnalyzer(CWTConfig config) {
        this.config = config;
    }
    
    /**
     * Detects market crashes using Paul wavelet for asymmetric pattern recognition.
     */
    public CrashDetectionResult detectMarketCrashes(double[] priceData, double samplingRate) {
        PaulWavelet paul = new PaulWavelet(4); // Order 4 for crash detection
        CWTTransform transform = new CWTTransform(paul, config);
        
        // Use scales that capture sharp drops (1-10 day movements)
        ScaleSpace scales = ScaleSpace.logarithmic(CRASH_MIN_SCALE, CRASH_MAX_SCALE, CRASH_NUM_SCALES);
        CWTResult result = transform.analyze(priceData, scales);
        
        // Analyze coefficients for asymmetric patterns
        List<Integer> crashPoints = new ArrayList<>();
        double[] severity = new double[priceData.length];
        Map<Integer, Double> crashProbabilities = new HashMap<>();
        
        double[][] coeffs = result.getCoefficients();
        
        // Look for large negative coefficients indicating sharp drops
        for (int t = 1; t < priceData.length - 1; t++) {
            double asymmetryScore = 0;
            
            for (int s = 0; s < scales.getNumScales(); s++) {
                double coeff = coeffs[s][t];
                // Paul wavelet responds strongly to asymmetric patterns
                if (coeff < 0) {
                    asymmetryScore += Math.abs(coeff) * scales.getScale(s);
                }
            }
            
            severity[t] = asymmetryScore;
            
            // Detect crash if asymmetry score exceeds threshold
            if (asymmetryScore > CRASH_ASYMMETRY_THRESHOLD && isLocalMaximum(severity, t)) {
                crashPoints.add(t);
                crashProbabilities.put(t, Math.min(asymmetryScore / CRASH_PROBABILITY_NORMALIZATION, 1.0));
            }
        }
        
        double maxSeverity = Arrays.stream(severity).max().orElse(0.0);
        
        return new CrashDetectionResult(crashPoints, severity, maxSeverity, crashProbabilities);
    }
    
    /**
     * Analyzes volatility clusters using DOG wavelet.
     */
    public VolatilityAnalysisResult analyzeVolatility(double[] priceData, double samplingRate) {
        // Calculate absolute returns as volatility proxy
        double[] absReturns = new double[priceData.length - 1];
        for (int i = 0; i < absReturns.length; i++) {
            absReturns[i] = Math.abs((priceData[i + 1] - priceData[i]) / priceData[i]);
        }
        
        DOGWavelet dog = new DOGWavelet(2); // Mexican Hat for volatility
        CWTTransform transform = new CWTTransform(dog, config);
        
        // Scales for different volatility horizons
        ScaleSpace scales = ScaleSpace.logarithmic(1.0, 30.0, 15);
        CWTResult result = transform.analyze(absReturns, scales);
        
        // Calculate instantaneous volatility
        double[] instantaneousVolatility = new double[absReturns.length];
        double[][] magnitude = result.getMagnitude();
        
        for (int t = 0; t < absReturns.length; t++) {
            double vol = 0;
            for (int s = 0; s < scales.getNumScales(); s++) {
                vol += magnitude[s][t] * magnitude[s][t];
            }
            instantaneousVolatility[t] = Math.sqrt(vol);
        }
        
        // Identify volatility clusters
        List<VolatilityCluster> clusters = identifyVolatilityClusters(instantaneousVolatility);
        
        double avgVol = Arrays.stream(instantaneousVolatility).average().orElse(0.0);
        double maxVol = Arrays.stream(instantaneousVolatility).max().orElse(0.0);
        
        return new VolatilityAnalysisResult(clusters, instantaneousVolatility, avgVol, maxVol);
    }
    
    /**
     * Identifies cyclical patterns using Shannon wavelet.
     */
    public CyclicalAnalysisResult analyzeCyclicalPatterns(double[] priceData, double samplingRate) {
        // Detrend the data first
        double[] detrended = detrendData(priceData);
        
        List<MarketCycle> dominantCycles = new ArrayList<>();
        Map<Double, Double> periodogram = new HashMap<>();
        
        // Test multiple Shannon wavelets with different frequency bands
        double[] testFrequencies = CYCLE_TEST_FREQUENCIES; // 5, 10, 22, 50 day cycles
        
        for (double testFreq : testFrequencies) {
            double bandwidth = 0.2 * testFreq; // Narrow band around test frequency
            ShannonGaborWavelet shannon = new ShannonGaborWavelet(bandwidth, testFreq / bandwidth);
            
            CWTTransform transform = new CWTTransform(shannon, config);
            ScaleSpace scales = ScaleSpace.linear(0.8, 1.2, 5); // Fine scale resolution
            
            CWTResult result = transform.analyze(detrended, scales);
            double[][] magnitude = result.getMagnitude();
            
            // Calculate average power at this frequency
            double avgPower = 0;
            for (int t = 0; t < detrended.length; t++) {
                for (int s = 0; s < scales.getNumScales(); s++) {
                    avgPower += magnitude[s][t] * magnitude[s][t];
                }
            }
            avgPower /= (detrended.length * scales.getNumScales());
            
            double period = 1.0 / testFreq;
            periodogram.put(period, avgPower);
            
            if (avgPower > 0.01) { // Lower threshold for cycle detection
                // Estimate phase
                double phase = estimatePhase(result);
                dominantCycles.add(new MarketCycle(period, testFreq, avgPower, phase));
            }
        }
        
        // Sort by strength
        dominantCycles.sort((a, b) -> Double.compare(b.strength(), a.strength()));
        
        double[] spectralDensity = new double[priceData.length / 2];
        // Simplified spectral density calculation
        for (int i = 0; i < spectralDensity.length; i++) {
            spectralDensity[i] = 0.1 * Math.random(); // Placeholder
        }
        
        return new CyclicalAnalysisResult(dominantCycles, spectralDensity, periodogram);
    }
    
    /**
     * Performs comprehensive market analysis combining all wavelets.
     */
    public MarketAnalysisResult analyzeMarket(double[] priceData, double[] volumeData, 
                                            double samplingRate) {
        // Detect crashes
        CrashDetectionResult crashes = detectMarketCrashes(priceData, samplingRate);
        
        // Analyze volatility
        VolatilityAnalysisResult volatility = analyzeVolatility(priceData, samplingRate);
        
        // Detect regime changes
        List<Integer> regimeChanges = new ArrayList<>();
        Map<Integer, MarketRegime> regimeMap = new HashMap<>();
        MarketRegime currentRegime = MarketRegime.RANGING;
        
        // Note: volatility.instantaneousVolatility has length priceData.length - 1
        // Loop goes up to priceData.length - 1 (exclusive), so max i = priceData.length - 2
        // This correctly accesses the last element of instantaneousVolatility array
        for (int i = REGIME_DETECTION_LOOKBACK_PERIOD; i < priceData.length - 1; i++) {
            MarketRegime newRegime = detectRegime(priceData, i, volatility.instantaneousVolatility[i]);
            if (newRegime != currentRegime) {
                regimeChanges.add(i);
                currentRegime = newRegime;
            }
            regimeMap.put(i, currentRegime);
        }
        
        // Handle the last price point separately (no volatility data for it)
        if (priceData.length >= REGIME_DETECTION_LOOKBACK_PERIOD) {
            regimeMap.put(priceData.length - 1, currentRegime);
        }
        
        // Detect anomalies
        List<MarketAnomaly> anomalies = new ArrayList<>();
        
        // Volume-price divergence
        if (volumeData == null || volumeData.length < 2) {
            throw new IllegalArgumentException("volumeData must not be null and must have at least two elements.");
        }
        if (volumeData.length != priceData.length) {
            throw new IllegalArgumentException("volumeData length (" + volumeData.length + 
                ") must match priceData length (" + priceData.length + ")");
        }
        
        for (int i = 1; i < priceData.length - 1; i++) {
            double priceChange = Math.abs(priceData[i] - priceData[i-1]) / priceData[i-1];
            double volumeChange = Math.abs(volumeData[i] - volumeData[i-1]) / volumeData[i-1];
            
            if (volumeChange > VOLUME_DIVERGENCE_THRESHOLD && priceChange < PRICE_DIVERGENCE_THRESHOLD) {
                anomalies.add(new MarketAnomaly(i, AnomalyType.VOLUME_PRICE_DIVERGENCE, 
                    volumeChange, "High volume with minimal price movement"));
            }
        }
        
        // Add crash points as anomalies
        for (int crashPoint : crashes.crashPoints) {
            anomalies.add(new MarketAnomaly(crashPoint, AnomalyType.PRICE_SPIKE,
                crashes.severity[crashPoint], "Market crash detected"));
        }
        
        // Calculate risk metrics
        int riskIndex = Math.min(priceData.length - 1, volatility.instantaneousVolatility.length - 1);
        double currentRiskLevel = calculateRiskLevel(volatility, crashes, riskIndex);
        double maxDrawdown = calculateMaxDrawdown(priceData);
        
        return new MarketAnalysisResult(regimeChanges, anomalies, currentRiskLevel, 
                                      maxDrawdown, regimeMap);
    }
    
    /**
     * Generates trading signals based on wavelet analysis.
     */
    public TradingSignalResult generateTradingSignals(double[] priceData, double samplingRate) {
        List<TradingSignal> signals = new ArrayList<>();
        
        // Get all analyses
        CrashDetectionResult crashes = detectMarketCrashes(priceData, samplingRate);
        VolatilityAnalysisResult volatility = analyzeVolatility(priceData, samplingRate);
        CyclicalAnalysisResult cycles = analyzeCyclicalPatterns(priceData, samplingRate);
        
        // Generate signals based on combined analysis
        // Ensure we don't exceed volatility array bounds (length = priceData.length - 1)
        int maxIndex = Math.min(priceData.length - CRASH_PREDICTION_FORWARD_WINDOW, 
                               volatility.instantaneousVolatility.length);
        for (int i = SIGNAL_GENERATION_MIN_HISTORY; i < maxIndex; i++) {
            // Sell signal before potential crash
            if (crashes.crashProbabilities.containsKey(i + CRASH_PREDICTION_FORWARD_WINDOW)) {
                double prob = crashes.crashProbabilities.get(i + CRASH_PREDICTION_FORWARD_WINDOW);
                if (prob > 0.7) {
                    signals.add(new TradingSignal(i, SignalType.SELL, prob,
                        "High crash probability detected"));
                }
            }
            
            // Buy signal after crash in low volatility
            boolean recentCrash = false;
            int lookbackStart = Math.max(0, i - RECENT_CRASH_LOOKBACK_WINDOW);
            int lookbackEnd = Math.max(0, i - CRASH_PREDICTION_FORWARD_WINDOW);
            
            // Only check for recent crashes if we have a valid range
            if (lookbackStart < lookbackEnd) {
                for (int j = lookbackStart; j < lookbackEnd; j++) {
                    if (crashes.crashPoints.contains(j)) {
                        recentCrash = true;
                        break;
                    }
                }
            }
            
            if (recentCrash && volatility.instantaneousVolatility[i] < 
                volatility.averageVolatility * 0.8) {
                signals.add(new TradingSignal(i, SignalType.BUY, 0.75,
                    "Post-crash recovery opportunity"));
            }
        }
        
        // Calculate performance metrics
        double sharpeRatio = calculateSharpeRatio(signals, priceData);
        double winRate = calculateWinRate(signals, priceData);
        
        return new TradingSignalResult(signals, sharpeRatio, winRate);
    }
    
    /**
     * Calculates wavelet-based technical indicators.
     */
    public WaveletIndicators calculateWaveletIndicators(double[] priceData, double samplingRate) {
        int N = priceData.length;
        double[] trendStrength = new double[N];
        double[] momentum = new double[N];
        double[] volatilityIndex = new double[N];
        double[] supportResistance = new double[N];
        
        // Use Paul wavelet for trend and momentum
        PaulWavelet paul = new PaulWavelet(3);
        CWTTransform paulTransform = new CWTTransform(paul, config);
        ScaleSpace trendScales = ScaleSpace.logarithmic(TREND_MIN_SCALE, TREND_MAX_SCALE, TREND_NUM_SCALES);
        CWTResult paulResult = paulTransform.analyze(priceData, trendScales);
        
        // Use DOG wavelet for volatility - analyze absolute returns
        double[] absReturns = new double[N - 1];
        for (int i = 0; i < absReturns.length; i++) {
            absReturns[i] = Math.abs((priceData[i + 1] - priceData[i]) / priceData[i]);
        }
        
        DOGWavelet dog = new DOGWavelet(2);
        CWTTransform dogTransform = new CWTTransform(dog, config);
        ScaleSpace volScales = ScaleSpace.logarithmic(1.0, 20.0, 10);
        CWTResult dogResult = dogTransform.analyze(absReturns, volScales);
        
        double[][] paulCoeffs = paulResult.getCoefficients();
        double[][] dogMagnitude = dogResult.getMagnitude();
        
        for (int t = 0; t < N; t++) {
            // Trend strength from low-frequency Paul coefficients
            double trend = 0;
            for (int s = 5; s < 10 && s < paulCoeffs.length; s++) {
                trend += paulCoeffs[s][t];
            }
            trendStrength[t] = trend / 5.0;
            
            // Momentum from mid-frequency Paul coefficients
            double mom = 0;
            for (int s = 0; s < 5 && s < paulCoeffs.length; s++) {
                mom += paulCoeffs[s][t];
            }
            momentum[t] = mom / 5.0;
            
            // Volatility index from DOG magnitude
            volatilityIndex[t] = calculateVolatilityIndex(t, absReturns, dogMagnitude, volScales);
            
            // Support/Resistance from low-frequency Paul coefficients
            // Low-frequency components represent the underlying trend and key levels
            supportResistance[t] = calculateSupportResistance(paulCoeffs, priceData, t);
        }
        
        return new WaveletIndicators(trendStrength, momentum, volatilityIndex, supportResistance);
    }
    
    /**
     * Optimizes wavelet parameters for specific analysis objective.
     */
    public OptimalParameters optimizeParameters(double[] priceData, AnalysisObjective objective) {
        // Simplified parameter optimization
        OptimalParameters params = switch (objective) {
            case CRASH_DETECTION -> new OptimalParameters(
                OptimizationDefaults.CRASH_PAUL_ORDER, 
                OptimizationDefaults.CRASH_DOG_ORDER, 
                OptimizationDefaults.CRASH_THRESHOLD_FACTOR, 
                OptimizationDefaults.CRASH_SEVERITY_EXPONENT, 
                OptimizationDefaults.CRASH_SCALE_RANGE
            );
            case VOLATILITY_ANALYSIS -> new OptimalParameters(
                OptimizationDefaults.VOLATILITY_PAUL_ORDER, 
                OptimizationDefaults.VOLATILITY_DOG_ORDER, 
                OptimizationDefaults.VOLATILITY_THRESHOLD_FACTOR, 
                OptimizationDefaults.VOLATILITY_EXPONENT, 
                OptimizationDefaults.VOLATILITY_SCALE_RANGE
            );
            case CYCLE_DETECTION -> new OptimalParameters(
                OptimizationDefaults.CYCLE_SHANNON_FB, 
                OptimizationDefaults.CYCLE_SHANNON_FC, 
                OptimizationDefaults.CYCLE_THRESHOLD_FACTOR, 
                OptimizationDefaults.CYCLE_EXPONENT, 
                OptimizationDefaults.CYCLE_SCALE_RANGE
            );
            case SIGNAL_GENERATION -> new OptimalParameters(
                OptimizationDefaults.SIGNAL_PAUL_ORDER, 
                OptimizationDefaults.SIGNAL_DOG_ORDER, 
                OptimizationDefaults.SIGNAL_THRESHOLD_FACTOR, 
                OptimizationDefaults.SIGNAL_EXPONENT, 
                OptimizationDefaults.SIGNAL_SCALE_RANGE
            );
        };
        
        return params;
    }
    
    // Helper methods
    
    private double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }
    
    private boolean isLocalMaximum(double[] data, int index) {
        if (index <= 0 || index >= data.length - 1) return false;
        return data[index] > data[index - 1] && data[index] > data[index + 1];
    }
    
    private List<VolatilityCluster> identifyVolatilityClusters(double[] volatility) {
        List<VolatilityCluster> clusters = new ArrayList<>();
        double avgVol = Arrays.stream(volatility).average().orElse(0.0);
        
        int startIdx = -1;
        VolatilityLevel currentLevel = VolatilityLevel.LOW;
        
        for (int i = 0; i < volatility.length; i++) {
            VolatilityLevel level = classifyVolatility(volatility[i], avgVol);
            
            if (level != currentLevel) {
                if (startIdx >= 0 && currentLevel != VolatilityLevel.LOW) {
                    // End current cluster
                    double clusterAvg = 0;
                    for (int j = startIdx; j < i; j++) {
                        clusterAvg += volatility[j];
                    }
                    clusterAvg /= (i - startIdx);
                    
                    clusters.add(new VolatilityCluster(startIdx, i - 1, currentLevel, clusterAvg));
                }
                
                startIdx = i;
                currentLevel = level;
            }
        }
        
        // Handle last cluster
        if (startIdx >= 0 && currentLevel != VolatilityLevel.LOW) {
            double clusterAvg = 0;
            for (int j = startIdx; j < volatility.length; j++) {
                clusterAvg += volatility[j];
            }
            clusterAvg /= (volatility.length - startIdx);
            
            clusters.add(new VolatilityCluster(startIdx, volatility.length - 1, 
                                              currentLevel, clusterAvg));
        }
        
        return clusters;
    }
    
    private VolatilityLevel classifyVolatility(double vol, double avgVol) {
        if (vol < avgVol * VOLATILITY_LOW_THRESHOLD) return VolatilityLevel.LOW;
        if (vol < avgVol * VOLATILITY_MEDIUM_THRESHOLD) return VolatilityLevel.MEDIUM;
        if (vol < avgVol * VOLATILITY_HIGH_THRESHOLD) return VolatilityLevel.HIGH;
        return VolatilityLevel.EXTREME;
    }
    
    private double[] detrendData(double[] data) {
        double[] detrended = new double[data.length];
        
        // Simple linear detrending
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = data.length;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += data[i];
            sumXY += i * data[i];
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        for (int i = 0; i < n; i++) {
            detrended[i] = data[i] - (slope * i + intercept);
        }
        
        return detrended;
    }
    
    private double estimatePhase(CWTResult result) {
        // Simplified phase estimation
        if (result.isComplex()) {
            double[][] phase = result.getPhase();
            return phase[0][phase[0].length / 2]; // Middle point phase
        }
        return 0.0;
    }
    
    private MarketRegime detectRegime(double[] prices, int index, double volatility) {
        // Simple regime detection based on recent price movement and volatility
        if (index < REGIME_DETECTION_LOOKBACK_PERIOD) return MarketRegime.RANGING;
        
        double recentReturn = (prices[index] - prices[index - REGIME_DETECTION_LOOKBACK_PERIOD]) / 
                             prices[index - REGIME_DETECTION_LOOKBACK_PERIOD];
        double avgVolatility = DEFAULT_AVERAGE_VOLATILITY; // Assumed average
        
        if (volatility > avgVolatility * 2) {
            return MarketRegime.VOLATILE;
        } else if (recentReturn > REGIME_TREND_THRESHOLD) {
            return MarketRegime.TRENDING_UP;
        } else if (recentReturn < -REGIME_TREND_THRESHOLD) {
            return MarketRegime.TRENDING_DOWN;
        } else {
            return MarketRegime.RANGING;
        }
    }
    
    private double calculateRiskLevel(VolatilityAnalysisResult volatility, 
                                    CrashDetectionResult crashes, int currentIndex) {
        double riskLevel = BASE_RISK_LEVEL; // Base risk
        
        // Increase risk based on current volatility
        if (currentIndex < volatility.instantaneousVolatility.length) {
            double currentVol = volatility.instantaneousVolatility[currentIndex];
            riskLevel += 0.3 * (currentVol / volatility.maxVolatility);
        }
        
        // Increase risk if recent crash
        for (int crashPoint : crashes.crashPoints) {
            if (Math.abs(crashPoint - currentIndex) < RISK_ASSESSMENT_CRASH_WINDOW) {
                riskLevel += 0.2;
                break;
            }
        }
        
        return Math.min(riskLevel, 1.0);
    }
    
    private double calculateMaxDrawdown(double[] prices) {
        double maxDrawdown = 0;
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
     * Calculates the Sharpe ratio for the generated trading signals.
     * 
     * @param signals list of trading signals
     * @param prices price data
     * @return Sharpe ratio (annualized)
     */
    private double calculateSharpeRatio(List<TradingSignal> signals, double[] prices) {
        if (signals.isEmpty() || prices.length < 2) {
            return 0.0;
        }
        
        // Calculate returns from signals
        List<Double> returns = new ArrayList<>();
        double position = 0; // 0 = no position, 1 = long, -1 = short
        int entryIndex = -1;
        double entryPrice = 0;
        
        for (TradingSignal signal : signals) {
            if (signal.timeIndex() >= prices.length) continue;
            
            if (signal.type() == SignalType.BUY && position <= 0) {
                if (position < 0) {
                    // Close short position
                    double returnPct = (entryPrice - prices[signal.timeIndex()]) / entryPrice;
                    returns.add(returnPct);
                }
                // Open long position
                position = 1;
                entryIndex = signal.timeIndex();
                entryPrice = prices[entryIndex];
                
            } else if (signal.type() == SignalType.SELL && position >= 0) {
                if (position > 0) {
                    // Close long position
                    double returnPct = (prices[signal.timeIndex()] - entryPrice) / entryPrice;
                    returns.add(returnPct);
                }
                // Open short position
                position = -1;
                entryIndex = signal.timeIndex();
                entryPrice = prices[entryIndex];
            }
        }
        
        // Close any open position at the end
        if (position != 0 && entryIndex >= 0 && entryIndex < prices.length - 1) {
            double finalPrice = prices[prices.length - 1];
            double returnPct = position > 0 
                ? (finalPrice - entryPrice) / entryPrice
                : (entryPrice - finalPrice) / entryPrice;
            returns.add(returnPct);
        }
        
        if (returns.isEmpty()) {
            return 0.0;
        }
        
        // Calculate mean and standard deviation
        double meanReturn = returns.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
            
        double variance = returns.stream()
            .mapToDouble(r -> Math.pow(r - meanReturn, 2))
            .average()
            .orElse(0.0);
            
        double stdDev = Math.sqrt(variance);
        
        // Sharpe ratio with risk-free rate of 0 for simplicity
        // Annualize assuming 252 trading days
        double dailySharpe = stdDev > 0 ? meanReturn / stdDev : 0.0;
        return dailySharpe * Math.sqrt(252);
    }
    
    /**
     * Calculates volatility index at time t using absolute returns and wavelet enhancement.
     * 
     * <p>This method handles the complex indexing between price data (length N) and
     * absolute returns (length N-1), combining raw volatility with wavelet-based
     * enhancement for better detection of volatility patterns.</p>
     * 
     * @param t current time index in price data
     * @param absReturns absolute returns array (length N-1)
     * @param dogMagnitude DOG wavelet magnitude coefficients
     * @param volScales scale space used for volatility analysis
     * @return volatility index value at time t
     */
    private double calculateVolatilityIndex(int t, double[] absReturns, double[][] dogMagnitude, 
                                          ScaleSpace volScales) {
        // Note: absReturns is N-1 length, so we need to handle indexing carefully
        if (t == 0) {
            // First point: use the first return volatility
            return absReturns.length > 0 ? absReturns[0] * 100.0 : 0.0;
        } else if (t < absReturns.length) {
            // For t > 0 and t < absReturns.length, use raw absolute return plus wavelet enhancement
            double baseVol = absReturns[t-1] * 100.0; // Scale up for visibility
            
            // Add wavelet-based enhancement for better detection
            if (t-1 < dogMagnitude[0].length) {
                double waveletEnhancement = 0;
                int volScalesUsed = Math.min(3, volScales.getNumScales());
                for (int s = 0; s < volScalesUsed; s++) {
                    waveletEnhancement += dogMagnitude[s][t-1] * dogMagnitude[s][t-1];
                }
                return baseVol + Math.sqrt(waveletEnhancement);
            } else {
                return baseVol;
            }
        } else {
            // Last element: can't calculate new volatility, so use previous value
            // This handles the case where t == priceData.length - 1
            return t > 0 ? calculateVolatilityIndex(t-1, absReturns, dogMagnitude, volScales) : 0.0;
        }
    }
    
    /**
     * Calculates support/resistance levels using wavelet analysis.
     * 
     * <p>This method identifies key price levels by analyzing low-frequency wavelet
     * coefficients which represent the underlying market structure. Local extrema
     * in these coefficients often correspond to support and resistance levels.</p>
     * 
     * @param paulCoeffs wavelet coefficients from Paul transform
     * @param prices original price data
     * @param currentIndex current time index
     * @return estimated support/resistance level at current index
     */
    private double calculateSupportResistance(double[][] paulCoeffs, double[] prices, int currentIndex) {
        if (currentIndex == 0 || paulCoeffs.length == 0) {
            return prices[currentIndex];
        }
        
        // Use low-frequency scales (higher indices) for structure
        int structureScaleStart = Math.min(5, paulCoeffs.length - 1);
        int structureScaleEnd = Math.min(10, paulCoeffs.length);
        
        // Look for local extrema in a window around current point
        int windowSize = SUPPORT_RESISTANCE_WINDOW;
        int startIdx = Math.max(0, currentIndex - windowSize);
        int endIdx = Math.min(paulCoeffs[0].length - 1, currentIndex + windowSize);
        
        double closestSupport = prices[currentIndex];
        double closestResistance = prices[currentIndex];
        double minDistance = Double.MAX_VALUE;
        
        // Find local maxima (resistance) and minima (support) in wavelet space
        for (int scale = structureScaleStart; scale < structureScaleEnd; scale++) {
            if (scale >= paulCoeffs.length) break;
            
            double[] coeffs = paulCoeffs[scale];
            
            for (int i = startIdx + 1; i < endIdx - 1 && i < coeffs.length - 1; i++) {
                // Check for local maximum (potential resistance)
                if (coeffs[i] > coeffs[i-1] && coeffs[i] > coeffs[i+1]) {
                    double level = prices[i];
                    double distance = Math.abs(level - prices[currentIndex]);
                    
                    if (level > prices[currentIndex] && distance < minDistance) {
                        closestResistance = level;
                        minDistance = distance;
                    }
                }
                
                // Check for local minimum (potential support)
                if (coeffs[i] < coeffs[i-1] && coeffs[i] < coeffs[i+1]) {
                    double level = prices[i];
                    double distance = Math.abs(level - prices[currentIndex]);
                    
                    if (level < prices[currentIndex] && distance < minDistance) {
                        closestSupport = level;
                        minDistance = distance;
                    }
                }
            }
        }
        
        // Return the closest support or resistance level
        double supportDist = Math.abs(prices[currentIndex] - closestSupport);
        double resistDist = Math.abs(prices[currentIndex] - closestResistance);
        
        // If we're closer to support, return support; otherwise resistance
        if (supportDist < resistDist) {
            return closestSupport;
        } else {
            return closestResistance;
        }
    }
    
    /**
     * Calculates the win rate of trading signals.
     * 
     * @param signals list of trading signals
     * @param prices price data
     * @return win rate (0.0 to 1.0)
     */
    private double calculateWinRate(List<TradingSignal> signals, double[] prices) {
        if (signals.isEmpty() || prices.length < 2) {
            return 0.0;
        }
        
        int winningTrades = 0;
        int totalTrades = 0;
        double position = 0;
        double entryPrice = 0;
        
        for (TradingSignal signal : signals) {
            if (signal.timeIndex() >= prices.length) continue;
            
            if (signal.type() == SignalType.BUY && position <= 0) {
                if (position < 0) {
                    // Close short position
                    double profit = entryPrice - prices[signal.timeIndex()];
                    if (profit > 0) winningTrades++;
                    totalTrades++;
                }
                // Open long position
                position = 1;
                entryPrice = prices[signal.timeIndex()];
                
            } else if (signal.type() == SignalType.SELL && position >= 0) {
                if (position > 0) {
                    // Close long position
                    double profit = prices[signal.timeIndex()] - entryPrice;
                    if (profit > 0) winningTrades++;
                    totalTrades++;
                }
                // Open short position
                position = -1;
                entryPrice = prices[signal.timeIndex()];
            }
        }
        
        // Check final position
        if (position != 0 && prices.length > 0) {
            double finalPrice = prices[prices.length - 1];
            double profit = position > 0 
                ? finalPrice - entryPrice
                : entryPrice - finalPrice;
            if (profit > 0) winningTrades++;
            totalTrades++;
        }
        
        return totalTrades > 0 ? (double) winningTrades / totalTrades : 0.0;
    }
}