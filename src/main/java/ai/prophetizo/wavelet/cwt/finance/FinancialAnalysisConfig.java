package ai.prophetizo.wavelet.cwt.finance;

/**
 * Configuration constants for financial wavelet analysis.
 * 
 * <p>This class centralizes all magic numbers and thresholds used in financial
 * analysis to make them easily configurable and documented.</p>
 * 
 * @since 1.0.0
 */
public final class FinancialAnalysisConfig {
    
    // Crash Detection Parameters
    /** Threshold for asymmetry score to detect potential crashes */
    public static final double CRASH_ASYMMETRY_THRESHOLD = 10.0;
    
    /** Default scale range for crash detection analysis */
    public static final double CRASH_MIN_SCALE = 1.0;
    public static final double CRASH_MAX_SCALE = 10.0;
    public static final int CRASH_NUM_SCALES = 20;
    
    // Volatility Analysis Parameters
    /** Thresholds for volatility level classification relative to average */
    public static final double VOLATILITY_LOW_THRESHOLD = 0.5;
    public static final double VOLATILITY_MEDIUM_THRESHOLD = 1.5;
    public static final double VOLATILITY_HIGH_THRESHOLD = 3.0;
    
    /** Volume change threshold for divergence detection */
    public static final double VOLUME_DIVERGENCE_THRESHOLD = 2.0;
    
    /** Price change threshold for divergence detection */
    public static final double PRICE_DIVERGENCE_THRESHOLD = 0.01;
    
    // Cyclical Analysis Parameters
    /** Test frequencies for cycle detection (corresponding to trading days) */
    public static final double[] CYCLE_TEST_FREQUENCIES = {
        0.2,   // 5-day cycle
        0.1,   // 10-day cycle
        0.045, // 22-day cycle (monthly)
        0.02   // 50-day cycle
    };
    
    // Trend Analysis Parameters
    /** Scale range for trend analysis */
    public static final double TREND_MIN_SCALE = 10.0;
    public static final double TREND_MAX_SCALE = 50.0;
    public static final int TREND_NUM_SCALES = 10;
    
    // Market Regime Detection Parameters
    /** Lookback period (in days) for regime detection calculations */
    public static final int REGIME_DETECTION_LOOKBACK_PERIOD = 20;
    
    /** Threshold for trend detection (5% return over lookback period) */
    public static final double REGIME_TREND_THRESHOLD = 0.05;
    
    // Trading Signal Generation Parameters
    /** Minimum history required before generating trading signals */
    public static final int SIGNAL_GENERATION_MIN_HISTORY = 20;
    
    /** Lookback window for checking recent crashes */
    public static final int RECENT_CRASH_LOOKBACK_WINDOW = 20;
    
    /** Crash probability normalization factor */
    public static final double CRASH_PROBABILITY_NORMALIZATION = 20.0;
    
    /** Recent crash window for risk assessment */
    public static final int RISK_ASSESSMENT_CRASH_WINDOW = 20;
    
    /** Forward-looking window for crash prediction (days) */
    public static final int CRASH_PREDICTION_FORWARD_WINDOW = 5;
    
    // Risk Assessment Parameters
    /** Default assumed average volatility for risk calculations */
    public static final double DEFAULT_AVERAGE_VOLATILITY = 0.02;
    
    /** Base risk level for calculations */
    public static final double BASE_RISK_LEVEL = 0.5;
    
    // Parameter Optimization Defaults
    public static final class OptimizationDefaults {
        // Crash Detection
        public static final int CRASH_PAUL_ORDER = 4;
        public static final int CRASH_DOG_ORDER = 2;
        public static final double CRASH_THRESHOLD_FACTOR = 0.5;
        public static final double CRASH_SEVERITY_EXPONENT = 1.5;
        public static final double[] CRASH_SCALE_RANGE = {1.0, 10.0};
        
        // Volatility Analysis
        public static final int VOLATILITY_PAUL_ORDER = 3;
        public static final int VOLATILITY_DOG_ORDER = 2;
        public static final double VOLATILITY_THRESHOLD_FACTOR = 0.3;
        public static final double VOLATILITY_EXPONENT = 1.0;
        public static final double[] VOLATILITY_SCALE_RANGE = {1.0, 30.0};
        
        // Cycle Detection
        public static final int CYCLE_SHANNON_FB = 2;
        public static final int CYCLE_SHANNON_FC = 3;
        public static final double CYCLE_THRESHOLD_FACTOR = 0.2;
        public static final double CYCLE_EXPONENT = 2.0;
        public static final double[] CYCLE_SCALE_RANGE = {5.0, 50.0};
        
        // Signal Generation
        public static final int SIGNAL_PAUL_ORDER = 4;
        public static final int SIGNAL_DOG_ORDER = 2;
        public static final double SIGNAL_THRESHOLD_FACTOR = 0.4;
        public static final double SIGNAL_EXPONENT = 1.5;
        public static final double[] SIGNAL_SCALE_RANGE = {2.0, 20.0};
    }
    
    // Private constructor to prevent instantiation
    private FinancialAnalysisConfig() {
        throw new AssertionError("Configuration class should not be instantiated");
    }
}