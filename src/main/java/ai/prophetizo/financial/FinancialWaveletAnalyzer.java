package ai.prophetizo.financial;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.util.ValidationUtils;

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
        if (!ValidationUtils.isPowerOfTwo(returns.length)) {
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
}