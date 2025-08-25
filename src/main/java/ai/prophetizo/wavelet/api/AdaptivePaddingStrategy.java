package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import java.util.Set;
import java.util.HashSet;

/**
 * Adaptive padding strategy that automatically selects the best padding method.
 * 
 * <p>This intelligent strategy analyzes signal characteristics and automatically
 * selects the most appropriate padding method. It considers factors such as:</p>
 * <ul>
 *   <li>Signal smoothness and differentiability</li>
 *   <li>Presence of trends or periodicity</li>
 *   <li>Statistical properties (stationarity, variance)</li>
 *   <li>Discontinuities and edge behavior</li>
 *   <li>Noise characteristics</li>
 * </ul>
 * 
 * <p>The selection process is transparent, providing reasoning for the chosen
 * strategy through the {@link #getLastSelectionReason()} method.</p>
 * 
 * <p>Ideal for:</p>
 * <ul>
 *   <li>Unknown or varying signal types</li>
 *   <li>Automated processing pipelines</li>
 *   <li>Situations where optimal padding is critical</li>
 *   <li>Adaptive algorithms that process diverse signals</li>
 * </ul>
 * 
 * @since 1.5.0
 */
public final class AdaptivePaddingStrategy implements PaddingStrategy {
    
    private final Set<PaddingStrategy> candidateStrategies;
    private String lastSelectionReason = "No padding performed yet";
    private PaddingStrategy lastSelectedStrategy = null;
    
    /**
     * Signal characteristics used for strategy selection.
     */
    private record SignalCharacteristics(
        double smoothness,        // 0=rough, 1=very smooth
        double trendStrength,     // 0=no trend, 1=strong trend
        double periodicity,       // 0=aperiodic, 1=strongly periodic
        double noiseLevel,        // 0=clean, 1=very noisy
        double stationarity,      // 0=non-stationary, 1=stationary
        boolean hasDiscontinuity, // Edge discontinuities detected
        int signalLength         // Original signal length
    ) {}
    
    /**
     * Creates an adaptive padding strategy with default candidate strategies.
     */
    public AdaptivePaddingStrategy() {
        this.candidateStrategies = createDefaultCandidates();
    }
    
    /**
     * Creates an adaptive padding strategy with custom candidate strategies.
     * 
     * @param candidateStrategies set of strategies to choose from
     */
    public AdaptivePaddingStrategy(Set<PaddingStrategy> candidateStrategies) {
        if (candidateStrategies == null || candidateStrategies.isEmpty()) {
            throw new InvalidArgumentException("Candidate strategies cannot be null or empty");
        }
        this.candidateStrategies = new HashSet<>(candidateStrategies);
    }
    
    /**
     * Create default set of candidate strategies.
     */
    private Set<PaddingStrategy> createDefaultCandidates() {
        Set<PaddingStrategy> candidates = new HashSet<>();
        
        // Basic strategies
        candidates.add(new ZeroPaddingStrategy());
        candidates.add(new ConstantPaddingStrategy());
        candidates.add(new SymmetricPaddingStrategy());
        candidates.add(new PeriodicPaddingStrategy());
        
        // Advanced strategies
        candidates.add(new LinearExtrapolationStrategy(3));
        candidates.add(new PolynomialExtrapolationStrategy(3));
        candidates.add(new StatisticalPaddingStrategy(StatisticalPaddingStrategy.StatMethod.MEAN));
        candidates.add(new StatisticalPaddingStrategy(StatisticalPaddingStrategy.StatMethod.TREND));
        candidates.add(new AntisymmetricPaddingStrategy());
        
        return candidates;
    }
    
    @Override
    public double[] pad(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be empty");
        }
        if (targetLength < signal.length) {
            throw new InvalidArgumentException(
                    "Target length " + targetLength + " must be >= signal length " + signal.length);
        }
        
        if (targetLength == signal.length) {
            return signal.clone();
        }
        
        // Analyze signal characteristics
        SignalCharacteristics characteristics = analyzeSignal(signal);
        
        // Select optimal strategy
        lastSelectedStrategy = selectOptimalStrategy(characteristics);
        
        // Apply selected strategy
        return lastSelectedStrategy.pad(signal, targetLength);
    }
    
    /**
     * Analyze signal to determine its characteristics.
     */
    private SignalCharacteristics analyzeSignal(double[] signal) {
        int n = signal.length;
        
        // Calculate smoothness (based on second differences)
        double smoothness = calculateSmoothness(signal);
        
        // Detect trend strength
        double trendStrength = calculateTrendStrength(signal);
        
        // Check for periodicity
        double periodicity = calculatePeriodicity(signal);
        
        // Estimate noise level
        double noiseLevel = calculateNoiseLevel(signal);
        
        // Check stationarity
        double stationarity = calculateStationarity(signal);
        
        // Check for discontinuities at edges
        boolean hasDiscontinuity = checkEdgeDiscontinuity(signal);
        
        return new SignalCharacteristics(
            smoothness, trendStrength, periodicity,
            noiseLevel, stationarity, hasDiscontinuity, n
        );
    }
    
    /**
     * Calculate signal smoothness based on second differences.
     */
    private double calculateSmoothness(double[] signal) {
        if (signal.length < 3) {
            return 0.5; // Default for very short signals
        }
        
        double sumSecondDiff = 0;
        double sumFirstDiff = 0;
        
        for (int i = 1; i < signal.length - 1; i++) {
            double firstDiff = Math.abs(signal[i] - signal[i-1]);
            double secondDiff = Math.abs((signal[i+1] - signal[i]) - (signal[i] - signal[i-1]));
            sumFirstDiff += firstDiff;
            sumSecondDiff += secondDiff;
        }
        
        if (sumFirstDiff < 1e-10) {
            return 1.0; // Constant signal is maximally smooth
        }
        
        // Smoothness inversely related to ratio of second to first differences
        double ratio = sumSecondDiff / sumFirstDiff;
        return Math.exp(-2 * ratio); // Maps to [0,1]
    }
    
    /**
     * Calculate trend strength using linear regression R².
     */
    private double calculateTrendStrength(double[] signal) {
        int n = signal.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = signal[i];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }
        
        double meanY = sumY / n;
        double ssTotal = sumY2 - n * meanY * meanY;
        
        if (ssTotal < 1e-10) {
            return 0; // No variation
        }
        
        // Calculate R² for linear fit
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return 0;
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        // Calculate residual sum of squares
        double ssResidual = 0;
        for (int i = 0; i < n; i++) {
            double predicted = intercept + slope * i;
            double residual = signal[i] - predicted;
            ssResidual += residual * residual;
        }
        
        double r2 = 1 - (ssResidual / ssTotal);
        return Math.max(0, Math.min(1, r2)); // Clamp to [0,1]
    }
    
    /**
     * Calculate periodicity using autocorrelation.
     */
    private double calculatePeriodicity(double[] signal) {
        if (signal.length < 10) {
            return 0; // Too short for periodicity detection
        }
        
        int maxLag = Math.min(signal.length / 2, 50);
        double maxCorr = 0;
        
        // Calculate mean
        double mean = 0;
        for (double val : signal) {
            mean += val;
        }
        mean /= signal.length;
        
        // Calculate variance
        double variance = 0;
        for (double val : signal) {
            double diff = val - mean;
            variance += diff * diff;
        }
        
        if (variance < 1e-10) {
            return 0; // Constant signal
        }
        
        // Find maximum autocorrelation for lags 2 to maxLag
        for (int lag = 2; lag < maxLag; lag++) {
            double correlation = 0;
            for (int i = 0; i < signal.length - lag; i++) {
                correlation += (signal[i] - mean) * (signal[i + lag] - mean);
            }
            correlation /= variance * (signal.length - lag);
            maxCorr = Math.max(maxCorr, Math.abs(correlation));
        }
        
        return maxCorr; // Already in [0,1]
    }
    
    /**
     * Estimate noise level using median absolute deviation.
     */
    private double calculateNoiseLevel(double[] signal) {
        if (signal.length < 5) {
            return 0.1; // Default for very short signals
        }
        
        // Calculate first differences
        double[] diffs = new double[signal.length - 1];
        for (int i = 0; i < diffs.length; i++) {
            diffs[i] = signal[i + 1] - signal[i];
        }
        
        // Calculate median of absolute differences
        double[] absDiffs = new double[diffs.length];
        for (int i = 0; i < diffs.length; i++) {
            absDiffs[i] = Math.abs(diffs[i]);
        }
        
        java.util.Arrays.sort(absDiffs);
        double mad = absDiffs[absDiffs.length / 2];
        
        // Normalize by signal range
        double min = signal[0], max = signal[0];
        for (double val : signal) {
            min = Math.min(min, val);
            max = Math.max(max, val);
        }
        
        double range = max - min;
        if (range < 1e-10) {
            return 0; // No noise in constant signal
        }
        
        double noiseRatio = mad / range;
        return Math.min(1, noiseRatio * 5); // Scale and clamp to [0,1]
    }
    
    /**
     * Calculate stationarity by comparing local statistics.
     */
    private double calculateStationarity(double[] signal) {
        if (signal.length < 20) {
            return 0.5; // Default for short signals
        }
        
        // Divide signal into segments
        int numSegments = 4;
        int segmentSize = signal.length / numSegments;
        
        double[] segmentMeans = new double[numSegments];
        double[] segmentVars = new double[numSegments];
        
        for (int seg = 0; seg < numSegments; seg++) {
            int start = seg * segmentSize;
            int end = (seg == numSegments - 1) ? signal.length : start + segmentSize;
            
            // Calculate segment statistics
            double sum = 0, sumSq = 0;
            for (int i = start; i < end; i++) {
                sum += signal[i];
                sumSq += signal[i] * signal[i];
            }
            
            int n = end - start;
            segmentMeans[seg] = sum / n;
            segmentVars[seg] = (sumSq / n) - (segmentMeans[seg] * segmentMeans[seg]);
        }
        
        // Calculate coefficient of variation for means and variances
        double meanOfMeans = 0, meanOfVars = 0;
        for (int i = 0; i < numSegments; i++) {
            meanOfMeans += segmentMeans[i];
            meanOfVars += segmentVars[i];
        }
        meanOfMeans /= numSegments;
        meanOfVars /= numSegments;
        
        double cvMeans = 0, cvVars = 0;
        for (int i = 0; i < numSegments; i++) {
            cvMeans += Math.pow(segmentMeans[i] - meanOfMeans, 2);
            cvVars += Math.pow(segmentVars[i] - meanOfVars, 2);
        }
        
        if (Math.abs(meanOfMeans) > 1e-10) {
            cvMeans = Math.sqrt(cvMeans / numSegments) / Math.abs(meanOfMeans);
        }
        if (meanOfVars > 1e-10) {
            cvVars = Math.sqrt(cvVars / numSegments) / meanOfVars;
        }
        
        // Stationarity inversely related to variation in statistics
        double nonStationarity = (cvMeans + cvVars) / 2;
        return Math.exp(-2 * nonStationarity); // Maps to [0,1]
    }
    
    /**
     * Check for discontinuities at signal edges.
     */
    private boolean checkEdgeDiscontinuity(double[] signal) {
        if (signal.length < 3) {
            return false;
        }
        
        // Check if edge values are significantly different from neighbors
        double firstDiff = Math.abs(signal[1] - signal[0]);
        double lastDiff = Math.abs(signal[signal.length - 1] - signal[signal.length - 2]);
        
        // Calculate typical difference in middle of signal
        double sumDiff = 0;
        int count = 0;
        for (int i = 2; i < signal.length - 2; i++) {
            sumDiff += Math.abs(signal[i] - signal[i - 1]);
            count++;
        }
        
        if (count == 0) {
            return false;
        }
        
        double typicalDiff = sumDiff / count;
        
        // Discontinuity if edge differences are much larger than typical
        return firstDiff > 3 * typicalDiff || lastDiff > 3 * typicalDiff;
    }
    
    /**
     * Select optimal padding strategy based on signal characteristics.
     */
    private PaddingStrategy selectOptimalStrategy(SignalCharacteristics chars) {
        // Decision tree for strategy selection
        
        // Check for special cases first
        if (chars.signalLength < 5) {
            lastSelectionReason = "Very short signal - using constant padding for stability";
            return findStrategy(ConstantPaddingStrategy.class);
        }
        
        if (chars.periodicity > 0.7) {
            lastSelectionReason = String.format(
                "Strong periodicity detected (%.2f) - using periodic padding", 
                chars.periodicity);
            return findStrategy(PeriodicPaddingStrategy.class);
        }
        
        if (chars.hasDiscontinuity && chars.smoothness < 0.3) {
            lastSelectionReason = "Edge discontinuities with rough signal - using zero padding";
            return findStrategy(ZeroPaddingStrategy.class);
        }
        
        if (chars.trendStrength > 0.8) {
            if (chars.noiseLevel < 0.2) {
                lastSelectionReason = String.format(
                    "Strong trend (R²=%.2f) with low noise - using polynomial extrapolation",
                    chars.trendStrength);
                return findStrategy(PolynomialExtrapolationStrategy.class);
            } else {
                lastSelectionReason = String.format(
                    "Strong trend (R²=%.2f) with noise - using statistical trend padding",
                    chars.trendStrength);
                return findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod.TREND);
            }
        }
        
        if (chars.smoothness > 0.7 && chars.noiseLevel < 0.3) {
            if (chars.trendStrength > 0.5) {
                lastSelectionReason = String.format(
                    "Smooth signal (%.2f) with moderate trend - using linear extrapolation",
                    chars.smoothness);
                return findStrategy(LinearExtrapolationStrategy.class);
            } else {
                lastSelectionReason = String.format(
                    "Very smooth signal (%.2f) - using polynomial extrapolation",
                    chars.smoothness);
                return findStrategy(PolynomialExtrapolationStrategy.class);
            }
        }
        
        if (chars.stationarity > 0.7) {
            if (chars.noiseLevel > 0.5) {
                lastSelectionReason = String.format(
                    "Stationary signal (%.2f) with noise - using statistical mean padding",
                    chars.stationarity);
                return findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod.MEAN);
            } else {
                lastSelectionReason = String.format(
                    "Stationary signal (%.2f) - using constant padding",
                    chars.stationarity);
                return findStrategy(ConstantPaddingStrategy.class);
            }
        }
        
        if (chars.noiseLevel > 0.6) {
            lastSelectionReason = String.format(
                "High noise level (%.2f) - using symmetric padding for robustness",
                chars.noiseLevel);
            return findStrategy(SymmetricPaddingStrategy.class);
        }
        
        // Default fallback
        lastSelectionReason = "Mixed characteristics - using symmetric padding as safe default";
        return findStrategy(SymmetricPaddingStrategy.class);
    }
    
    /**
     * Find a strategy of the given type in candidates.
     */
    private PaddingStrategy findStrategy(Class<? extends PaddingStrategy> strategyClass) {
        for (PaddingStrategy strategy : candidateStrategies) {
            if (strategyClass.isInstance(strategy)) {
                return strategy;
            }
        }
        // Fallback to first available strategy
        return candidateStrategies.iterator().next();
    }
    
    /**
     * Find a statistical strategy with specific method.
     */
    private PaddingStrategy findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod method) {
        for (PaddingStrategy strategy : candidateStrategies) {
            if (strategy instanceof StatisticalPaddingStrategy stat) {
                if (stat.method() == method) {
                    return strategy;
                }
            }
        }
        // Fallback to any statistical strategy
        return findStrategy(StatisticalPaddingStrategy.class);
    }
    
    /**
     * Get the reasoning for the last strategy selection.
     * 
     * @return explanation of why the strategy was chosen
     */
    public String getLastSelectionReason() {
        return lastSelectionReason;
    }
    
    /**
     * Get the last selected strategy.
     * 
     * @return the strategy that was last selected, or null if no padding performed yet
     */
    public PaddingStrategy getLastSelectedStrategy() {
        return lastSelectedStrategy;
    }
    
    @Override
    public String name() {
        if (lastSelectedStrategy != null) {
            return "adaptive-" + lastSelectedStrategy.name();
        }
        return "adaptive";
    }
    
    @Override
    public String description() {
        if (lastSelectedStrategy != null) {
            return String.format("Adaptive padding (selected: %s) - %s", 
                    lastSelectedStrategy.name(), lastSelectionReason);
        }
        return "Adaptive padding - automatically selects optimal strategy";
    }
}