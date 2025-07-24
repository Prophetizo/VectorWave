package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import java.util.Arrays;

/**
 * Median Absolute Deviation (MAD) based noise estimator for streaming applications.
 * 
 * <p>This estimator uses the robust MAD statistic to estimate noise levels,
 * which is less sensitive to outliers than standard deviation. It maintains
 * a sliding window of coefficients for adaptive estimation.</p>
 *
 * @since 1.6.0
 */
public class MADNoiseEstimator implements NoiseEstimator {
    
    private static final double MAD_SCALE_FACTOR = 1.4826; // For Gaussian noise
    private static final double UNIVERSAL_THRESHOLD_FACTOR = Math.sqrt(2 * Math.log(2)); // For universal threshold
    
    private final int windowSize;
    private final double[] buffer;
    private final double smoothingFactor;
    private int bufferPosition;
    private int samplesInBuffer;
    private double currentNoiseEstimate;
    private long totalSamples;
    
    /**
     * Creates a MAD noise estimator with default settings.
     */
    public MADNoiseEstimator() {
        this(1024, 0.95);
    }
    
    /**
     * Creates a MAD noise estimator with specified parameters.
     * 
     * @param windowSize size of the sliding window for MAD calculation
     * @param smoothingFactor exponential smoothing factor (0-1, higher = more smoothing)
     */
    public MADNoiseEstimator(int windowSize, double smoothingFactor) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (smoothingFactor < 0 || smoothingFactor > 1) {
            throw new IllegalArgumentException("Smoothing factor must be between 0 and 1");
        }
        
        this.windowSize = windowSize;
        this.buffer = new double[windowSize];
        this.smoothingFactor = smoothingFactor;
        this.bufferPosition = 0;
        this.samplesInBuffer = 0;
        this.currentNoiseEstimate = 0.0;
        this.totalSamples = 0;
    }
    
    @Override
    public double estimateNoise(double[] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return currentNoiseEstimate;
        }
        
        // Add coefficients to buffer
        for (double coeff : coefficients) {
            buffer[bufferPosition] = Math.abs(coeff);
            bufferPosition = (bufferPosition + 1) % windowSize;
            if (samplesInBuffer < windowSize) {
                samplesInBuffer++;
            }
            totalSamples++;
        }
        
        // Calculate MAD from buffer
        if (samplesInBuffer > 0) {
            double mad = calculateMAD();
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
                int n = Math.max(samplesInBuffer, 64); // Minimum size for stability
                return currentNoiseEstimate * Math.sqrt(2 * Math.log(n));
                
            case SURE:
                // Simplified SURE threshold (typically requires full signal)
                // Using a conservative estimate for streaming
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
        Arrays.fill(buffer, 0.0);
        bufferPosition = 0;
        samplesInBuffer = 0;
        currentNoiseEstimate = 0.0;
        totalSamples = 0;
    }
    
    @Override
    public long getSampleCount() {
        return totalSamples;
    }
    
    /**
     * Calculates the Median Absolute Deviation from the buffer.
     */
    private double calculateMAD() {
        if (samplesInBuffer == 0) {
            return 0.0;
        }
        
        // Copy active buffer contents
        double[] activeData = new double[samplesInBuffer];
        if (samplesInBuffer == windowSize) {
            System.arraycopy(buffer, 0, activeData, 0, samplesInBuffer);
        } else {
            // Buffer not full yet
            System.arraycopy(buffer, 0, activeData, 0, samplesInBuffer);
        }
        
        // Find median
        Arrays.sort(activeData);
        double median = samplesInBuffer % 2 == 0 ?
            (activeData[samplesInBuffer / 2 - 1] + activeData[samplesInBuffer / 2]) / 2.0 :
            activeData[samplesInBuffer / 2];
        
        // Calculate absolute deviations from median
        for (int i = 0; i < samplesInBuffer; i++) {
            activeData[i] = Math.abs(activeData[i] - median);
        }
        
        // Find median of absolute deviations
        Arrays.sort(activeData);
        return samplesInBuffer % 2 == 0 ?
            (activeData[samplesInBuffer / 2 - 1] + activeData[samplesInBuffer / 2]) / 2.0 :
            activeData[samplesInBuffer / 2];
    }
}