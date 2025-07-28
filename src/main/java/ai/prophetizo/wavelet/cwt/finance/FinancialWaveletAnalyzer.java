package ai.prophetizo.wavelet.cwt.finance;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.api.ContinuousWavelet;

import java.util.*;

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
        ScaleSpace scales = ScaleSpace.logarithmic(1.0, 10.0, 20);
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
            if (asymmetryScore > 10.0 && isLocalMaximum(severity, t)) {
                crashPoints.add(t);
                crashProbabilities.put(t, Math.min(asymmetryScore / 20.0, 1.0));
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
        double[] testFrequencies = {0.2, 0.1, 0.045, 0.02}; // 5, 10, 22, 50 day cycles
        
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
        
        for (int i = 20; i < priceData.length; i++) {
            // volatility.instantaneousVolatility is returns.length = priceData.length - 1
            int volIndex = Math.min(i, volatility.instantaneousVolatility.length - 1);
            MarketRegime newRegime = detectRegime(priceData, i, volatility.instantaneousVolatility[volIndex]);
            if (newRegime != currentRegime) {
                regimeChanges.add(i);
                currentRegime = newRegime;
            }
            regimeMap.put(i, currentRegime);
        }
        
        // Detect anomalies
        List<MarketAnomaly> anomalies = new ArrayList<>();
        
        // Volume-price divergence
        for (int i = 1; i < priceData.length - 1; i++) {
            double priceChange = Math.abs(priceData[i] - priceData[i-1]) / priceData[i-1];
            double volumeChange = Math.abs(volumeData[i] - volumeData[i-1]) / volumeData[i-1];
            
            if (volumeChange > 2.0 && priceChange < 0.01) {
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
        for (int i = 20; i < priceData.length - 5; i++) {
            // Sell signal before potential crash
            if (crashes.crashProbabilities.containsKey(i + 5)) {
                double prob = crashes.crashProbabilities.get(i + 5);
                if (prob > 0.7) {
                    signals.add(new TradingSignal(i, SignalType.SELL, prob,
                        "High crash probability detected"));
                }
            }
            
            // Buy signal after crash in low volatility
            boolean recentCrash = false;
            for (int j = Math.max(0, i - 20); j < i - 5; j++) {
                if (crashes.crashPoints.contains(j)) {
                    recentCrash = true;
                    break;
                }
            }
            
            if (recentCrash && volatility.instantaneousVolatility[i] < 
                volatility.averageVolatility * 0.8) {
                signals.add(new TradingSignal(i, SignalType.BUY, 0.75,
                    "Post-crash recovery opportunity"));
            }
        }
        
        // Calculate performance metrics (simplified)
        double sharpeRatio = 0.8; // Placeholder
        double winRate = 0.6; // Placeholder
        
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
        ScaleSpace trendScales = ScaleSpace.logarithmic(10.0, 50.0, 10);
        CWTResult paulResult = paulTransform.analyze(priceData, trendScales);
        
        // Use DOG wavelet for volatility - analyze returns not prices
        double[] returns = new double[N - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = Math.abs(priceData[i + 1] - priceData[i]) / priceData[i];
        }
        
        DOGWavelet dog = new DOGWavelet(2);
        CWTTransform dogTransform = new CWTTransform(dog, config);
        ScaleSpace volScales = ScaleSpace.logarithmic(1.0, 20.0, 10);
        CWTResult dogResult = dogTransform.analyze(returns, volScales);
        
        double[][] paulCoeffs = paulResult.getCoefficients();
        double[][] dogMagnitude = dogResult.getMagnitude();
        
        for (int t = 0; t < N; t++) {
            // Trend strength from low-frequency Paul coefficients
            double trend = 0;
            for (int s = 5; s < 10; s++) {
                trend += paulCoeffs[s][t];
            }
            trendStrength[t] = trend / 5.0;
            
            // Momentum from mid-frequency Paul coefficients
            double mom = 0;
            for (int s = 0; s < 5; s++) {
                mom += paulCoeffs[s][t];
            }
            momentum[t] = mom / 5.0;
            
            // Volatility index from DOG magnitude
            if (t < dogMagnitude[0].length) {
                double vol = 0;
                for (int s = 0; s < volScales.getNumScales(); s++) {
                    vol += dogMagnitude[s][t] * dogMagnitude[s][t];
                }
                volatilityIndex[t] = Math.sqrt(vol / volScales.getNumScales());
            } else {
                // Last element gets previous value
                volatilityIndex[t] = t > 0 ? volatilityIndex[t-1] : 0.0;
            }
            
            // TODO: Implement proper support/resistance detection using wavelet-based peak/trough analysis
            // For now, return the price itself as a placeholder
            supportResistance[t] = priceData[t];
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
                4, 2, 0.5, 1.5, new double[]{1.0, 10.0}
            );
            case VOLATILITY_ANALYSIS -> new OptimalParameters(
                3, 2, 0.3, 1.0, new double[]{1.0, 30.0}
            );
            case CYCLE_DETECTION -> new OptimalParameters(
                2, 3, 0.2, 2.0, new double[]{5.0, 50.0}
            );
            case SIGNAL_GENERATION -> new OptimalParameters(
                4, 2, 0.4, 1.5, new double[]{2.0, 20.0}
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
        if (vol < avgVol * 0.5) return VolatilityLevel.LOW;
        if (vol < avgVol * 1.5) return VolatilityLevel.MEDIUM;
        if (vol < avgVol * 3.0) return VolatilityLevel.HIGH;
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
        if (index < 20) return MarketRegime.RANGING;
        
        double recentReturn = (prices[index] - prices[index - 20]) / prices[index - 20];
        double avgVolatility = 0.02; // Assumed average
        
        if (volatility > avgVolatility * 2) {
            return MarketRegime.VOLATILE;
        } else if (recentReturn > 0.05) {
            return MarketRegime.TRENDING_UP;
        } else if (recentReturn < -0.05) {
            return MarketRegime.TRENDING_DOWN;
        } else {
            return MarketRegime.RANGING;
        }
    }
    
    private double calculateRiskLevel(VolatilityAnalysisResult volatility, 
                                    CrashDetectionResult crashes, int currentIndex) {
        double riskLevel = 0.5; // Base risk
        
        // Increase risk based on current volatility
        if (currentIndex < volatility.instantaneousVolatility.length) {
            double currentVol = volatility.instantaneousVolatility[currentIndex];
            riskLevel += 0.3 * (currentVol / volatility.maxVolatility);
        }
        
        // Increase risk if recent crash
        for (int crashPoint : crashes.crashPoints) {
            if (Math.abs(crashPoint - currentIndex) < 20) {
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
}