package ai.prophetizo.financial;

/**
 * Configuration class for financial analysis parameters.
 * 
 * <p>This class provides configurable thresholds and parameters for various
 * financial analysis operations including crash detection, volatility analysis,
 * and regime change detection using wavelet transforms.</p>
 * 
 * <p>All parameters have sensible defaults but can be customized for different
 * markets, instruments, or analysis requirements.</p>
 * 
 * @since 1.0.0
 */
public final class FinancialAnalysisConfig {
    
    // Default threshold values
    // Crash asymmetry threshold: The analyzeCrashAsymmetry() method returns a ratio
    // between 0 and 1.
    private static final double DEFAULT_CRASH_ASYMMETRY_THRESHOLD = 0.7;
    private static final double DEFAULT_VOLATILITY_LOW_THRESHOLD = 0.5;
    private static final double DEFAULT_REGIME_TREND_THRESHOLD = 0.02;
    private static final double DEFAULT_VOLATILITY_HIGH_THRESHOLD = 2.0;
    private static final double DEFAULT_ANOMALY_DETECTION_THRESHOLD = 3.0;
    private static final int DEFAULT_WINDOW_SIZE = 256;
    private static final double DEFAULT_CONFIDENCE_LEVEL = 0.95;
    
    private final double crashAsymmetryThreshold;
    private final double volatilityLowThreshold;
    private final double volatilityHighThreshold;
    private final double regimeTrendThreshold;
    private final double anomalyDetectionThreshold;
    private final int windowSize;
    private final double confidenceLevel;
    
    /**
     * Creates a new FinancialAnalysisConfig with custom parameters.
     */
    private FinancialAnalysisConfig(Builder builder) {
        this.crashAsymmetryThreshold = builder.crashAsymmetryThreshold;
        this.volatilityLowThreshold = builder.volatilityLowThreshold;
        this.volatilityHighThreshold = builder.volatilityHighThreshold;
        this.regimeTrendThreshold = builder.regimeTrendThreshold;
        this.anomalyDetectionThreshold = builder.anomalyDetectionThreshold;
        this.windowSize = builder.windowSize;
        this.confidenceLevel = builder.confidenceLevel;
    }
    
    /**
     * Creates a default configuration with standard financial analysis parameters.
     * 
     * @return a FinancialAnalysisConfig with default values
     */
    public static FinancialAnalysisConfig defaultConfig() {
        return new Builder().build();
    }
    
    /**
     * Creates a new builder for customizing configuration parameters.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the threshold for detecting market crash asymmetry.
     * Higher values indicate more severe asymmetric behavior in market movements.
     * 
     * @return the crash asymmetry threshold
     */
    public double getCrashAsymmetryThreshold() {
        return crashAsymmetryThreshold;
    }
    
    /**
     * Gets the threshold for low volatility detection.
     * Values below this threshold indicate low volatility periods.
     * 
     * @return the low volatility threshold
     */
    public double getVolatilityLowThreshold() {
        return volatilityLowThreshold;
    }
    
    /**
     * Gets the threshold for high volatility detection.
     * Values above this threshold indicate high volatility periods.
     * 
     * @return the high volatility threshold
     */
    public double getVolatilityHighThreshold() {
        return volatilityHighThreshold;
    }
    
    /**
     * Gets the threshold for regime trend detection.
     * Changes above this threshold indicate potential regime shifts.
     * 
     * @return the regime trend threshold
     */
    public double getRegimeTrendThreshold() {
        return regimeTrendThreshold;
    }
    
    /**
     * Gets the threshold for anomaly detection.
     * Values represent standard deviations from normal behavior.
     * 
     * @return the anomaly detection threshold
     */
    public double getAnomalyDetectionThreshold() {
        return anomalyDetectionThreshold;
    }
    
    /**
     * Gets the window size for analysis operations.
     * Must be a power of 2 for efficient wavelet transforms.
     * 
     * @return the analysis window size
     */
    public int getWindowSize() {
        return windowSize;
    }
    
    /**
     * Gets the confidence level for statistical tests.
     * 
     * @return the confidence level (0.0 to 1.0)
     */
    public double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    /**
     * Builder class for creating FinancialAnalysisConfig instances.
     */
    public static final class Builder {
        private double crashAsymmetryThreshold = DEFAULT_CRASH_ASYMMETRY_THRESHOLD;
        private double volatilityLowThreshold = DEFAULT_VOLATILITY_LOW_THRESHOLD;
        private double volatilityHighThreshold = DEFAULT_VOLATILITY_HIGH_THRESHOLD;
        private double regimeTrendThreshold = DEFAULT_REGIME_TREND_THRESHOLD;
        private double anomalyDetectionThreshold = DEFAULT_ANOMALY_DETECTION_THRESHOLD;
        private int windowSize = DEFAULT_WINDOW_SIZE;
        private double confidenceLevel = DEFAULT_CONFIDENCE_LEVEL;
        
        /**
         * Sets the crash asymmetry threshold.
         * 
         * @param threshold the crash asymmetry threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder crashAsymmetryThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Crash asymmetry threshold must be positive");
            }
            this.crashAsymmetryThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the low volatility threshold.
         * 
         * @param threshold the low volatility threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder volatilityLowThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volatility low threshold must be positive");
            }
            this.volatilityLowThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the high volatility threshold.
         * 
         * @param threshold the high volatility threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder volatilityHighThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volatility high threshold must be positive");
            }
            this.volatilityHighThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the regime trend threshold.
         * 
         * @param threshold the regime trend threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder regimeTrendThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Regime trend threshold must be positive");
            }
            this.regimeTrendThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the anomaly detection threshold.
         * 
         * @param threshold the anomaly detection threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder anomalyDetectionThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Anomaly detection threshold must be positive");
            }
            this.anomalyDetectionThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the window size for analysis.
         * 
         * @param size the window size (must be a power of 2 and at least 2)
         * @return this builder
         * @throws IllegalArgumentException if size is not a power of 2 or less than 2
         */
        public Builder windowSize(int size) {
            if (size < 2 || (size & (size - 1)) != 0) {
                throw new IllegalArgumentException("Window size must be a power of 2 and at least 2");
            }
            this.windowSize = size;
            return this;
        }
        
        /**
         * Sets the confidence level for statistical tests.
         * 
         * @param level the confidence level (must be between 0.0 and 1.0, exclusive)
         * @return this builder
         * @throws IllegalArgumentException if level is not between 0.0 and 1.0 (exclusive)
         */
        public Builder confidenceLevel(double level) {
            if (level <= 0.0 || level >= 1.0) {
                throw new IllegalArgumentException("Confidence level must be between 0.0 and 1.0");
            }
            this.confidenceLevel = level;
            return this;
        }
        
        /**
         * Builds the FinancialAnalysisConfig with the specified parameters.
         * 
         * @return a new FinancialAnalysisConfig instance
         */
        public FinancialAnalysisConfig build() {
            return new FinancialAnalysisConfig(this);
        }
    }
}