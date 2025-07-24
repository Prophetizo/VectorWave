package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

/**
 * Efficient Median Absolute Deviation (MAD) based noise estimator for streaming applications.
 * 
 * <p>This implementation uses P² quantile estimation algorithm for efficient
 * online median calculation with O(1) update time instead of O(n log n) sorting.
 * It maintains two P² estimators - one for the median of absolute values and
 * one for the median of absolute deviations.</p>
 *
 * @since 1.6.0
 */
public class EfficientMADNoiseEstimator implements NoiseEstimator {
    
    private static final double MAD_SCALE_FACTOR = 1.4826; // For Gaussian noise
    private static final double UNIVERSAL_THRESHOLD_FACTOR = Math.sqrt(2 * Math.log(2));
    
    // P² estimator for median of absolute coefficients
    private final P2QuantileEstimator medianEstimator;
    
    // P² estimator for MAD (median of absolute deviations)
    private final P2QuantileEstimator madEstimator;
    
    // Circular buffer for recent coefficients (for MAD calculation)
    private final double[] recentCoeffs;
    private final int bufferSize;
    private int bufferPos;
    private boolean bufferFull;
    
    private final double smoothingFactor;
    private double currentNoiseEstimate;
    private long totalSamples;
    
    /**
     * Creates an efficient MAD noise estimator with default settings.
     */
    public EfficientMADNoiseEstimator() {
        this(256, 0.95);
    }
    
    /**
     * Creates an efficient MAD noise estimator with specified parameters.
     * 
     * @param bufferSize size of the buffer for recent coefficients
     * @param smoothingFactor exponential smoothing factor (0-1, higher = more smoothing)
     */
    public EfficientMADNoiseEstimator(int bufferSize, double smoothingFactor) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        if (smoothingFactor < 0 || smoothingFactor > 1) {
            throw new IllegalArgumentException("Smoothing factor must be between 0 and 1");
        }
        
        this.bufferSize = bufferSize;
        this.recentCoeffs = new double[bufferSize];
        this.bufferPos = 0;
        this.bufferFull = false;
        
        this.medianEstimator = P2QuantileEstimator.forMedian();
        this.madEstimator = P2QuantileEstimator.forMedian();
        
        this.smoothingFactor = smoothingFactor;
        this.currentNoiseEstimate = 0.0;
        this.totalSamples = 0;
    }
    
    @Override
    public double estimateNoise(double[] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return currentNoiseEstimate;
        }
        
        // Process each coefficient
        for (double coeff : coefficients) {
            double absCoeff = Math.abs(coeff);
            
            // Update median estimator with absolute coefficient
            medianEstimator.update(absCoeff);
            
            // Store in circular buffer
            recentCoeffs[bufferPos] = absCoeff;
            bufferPos = (bufferPos + 1) % bufferSize;
            if (!bufferFull && bufferPos == 0) {
                bufferFull = true;
            }
            
            // Update MAD estimator with deviation from current median
            if (medianEstimator.getCount() > 5) { // Need some samples for stable median
                double currentMedian = medianEstimator.getQuantile();
                double deviation = Math.abs(absCoeff - currentMedian);
                madEstimator.update(deviation);
            }
            
            totalSamples++;
        }
        
        // Calculate noise estimate from MAD
        if (madEstimator.getCount() > 5) {
            double mad = madEstimator.getQuantile();
            double newEstimate = mad * MAD_SCALE_FACTOR;
            
            // Apply exponential smoothing
            if (currentNoiseEstimate == 0.0) {
                currentNoiseEstimate = newEstimate;
            } else {
                currentNoiseEstimate = smoothingFactor * currentNoiseEstimate + 
                                      (1 - smoothingFactor) * newEstimate;
            }
        }
        
        return currentNoiseEstimate;
    }
    
    @Override
    public void updateEstimate(double[] coefficients) {
        estimateNoise(coefficients);
    }
    
    @Override
    public double getThreshold(ThresholdMethod method) {
        switch (method) {
            case UNIVERSAL:
                // Universal threshold: sigma * sqrt(2 * log(n))
                int n = Math.max((int)Math.min(totalSamples, bufferSize), 64);
                return currentNoiseEstimate * Math.sqrt(2 * Math.log(n));
                
            case SURE:
                // Simplified SURE threshold for streaming
                return currentNoiseEstimate * 2.5;
                
            case MINIMAX:
                // Minimax threshold approximation
                return currentNoiseEstimate * 1.67;
                
            default:
                return currentNoiseEstimate * UNIVERSAL_THRESHOLD_FACTOR;
        }
    }
    
    @Override
    public double getCurrentNoiseLevel() {
        return currentNoiseEstimate;
    }
    
    @Override
    public void reset() {
        medianEstimator.reset();
        madEstimator.reset();
        bufferPos = 0;
        bufferFull = false;
        currentNoiseEstimate = 0.0;
        totalSamples = 0;
        
        // Clear buffer
        for (int i = 0; i < bufferSize; i++) {
            recentCoeffs[i] = 0.0;
        }
    }
    
    @Override
    public long getSampleCount() {
        return totalSamples;
    }
    
    /**
     * Gets the current median estimate.
     * 
     * @return the estimated median of absolute coefficients
     */
    public double getCurrentMedian() {
        return medianEstimator.getQuantile();
    }
    
    /**
     * Gets the current MAD estimate.
     * 
     * @return the estimated MAD before scaling
     */
    public double getCurrentMAD() {
        return madEstimator.getQuantile();
    }
}