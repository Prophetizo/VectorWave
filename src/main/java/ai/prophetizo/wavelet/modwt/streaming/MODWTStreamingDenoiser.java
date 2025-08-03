package ai.prophetizo.wavelet.modwt.streaming;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;

import java.util.concurrent.Flow;

/**
 * Streaming denoiser using MODWT for real-time noise reduction.
 *
 * <p>This class provides real-time denoising of streaming signals using MODWT's
 * shift-invariant properties for better denoising quality compared to DWT-based
 * streaming denoisers.</p>
 *
 * <p>Key advantages:</p>
 * <ul>
 *   <li>No blocking artifacts due to shift-invariance</li>
 *   <li>Smooth transitions across buffer boundaries</li>
 *   <li>Better preservation of signal features</li>
 *   <li>Works with any buffer size</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
 *     .wavelet(Daubechies.DB4)
 *     .bufferSize(256)
 *     .thresholdType(ThresholdType.SOFT)
 *     .noiseEstimation(NoiseEstimation.MAD)
 *     .build();
 *
 * // Process noisy signal
 * double[] denoisedChunk = denoiser.denoise(noisyChunk);
 * }</pre>
 *
 * @since 3.0.0
 */
public class MODWTStreamingDenoiser implements AutoCloseable {

    private final MODWTTransform transform;
    private final MODWTTransform inverseTransform;
    private final Wavelet wavelet;
    private final int bufferSize;
    private final WaveletDenoiser.ThresholdType thresholdType;
    private final NoiseEstimation noiseEstimation;
    private final double thresholdMultiplier;
    
    // Circular buffer for continuity
    private final double[] overlapBuffer;
    private final int overlapSize;
    private int overlapPosition = 0;
    
    // Noise level estimation
    private double estimatedNoiseLevel = 0.0;
    private final double[] noiseWindow;
    private int noiseWindowPosition = 0;
    private boolean noiseEstimated = false;

    private MODWTStreamingDenoiser(Builder builder) {
        this.wavelet = builder.wavelet;
        this.bufferSize = builder.bufferSize;
        this.thresholdType = builder.thresholdType;
        this.noiseEstimation = builder.noiseEstimation;
        this.thresholdMultiplier = builder.thresholdMultiplier;
        
        // Create MODWT transforms
        this.transform = new MODWTTransform(wavelet, builder.boundaryMode);
        this.inverseTransform = new MODWTTransform(wavelet, builder.boundaryMode);
        
        // Initialize overlap buffer
        this.overlapSize = wavelet.lowPassDecomposition().length;
        this.overlapBuffer = new double[overlapSize];
        
        // Initialize noise estimation window
        this.noiseWindow = new double[builder.noiseWindowSize];
    }

    /**
     * Denoise a chunk of streaming data.
     *
     * @param noisyData the noisy input data
     * @return denoised output data
     * @throws InvalidArgumentException if data is null or empty
     */
    public synchronized double[] denoise(double[] noisyData) {
        if (noisyData == null || noisyData.length == 0) {
            throw new InvalidArgumentException("Data cannot be null or empty");
        }
        
        double[] denoised = new double[noisyData.length];
        int outputIndex = 0;
        
        // Process in buffer-sized chunks
        for (int i = 0; i < noisyData.length; i += bufferSize) {
            int chunkSize = Math.min(bufferSize, noisyData.length - i);
            double[] chunk = new double[bufferSize];
            
            // Copy input data to chunk
            System.arraycopy(noisyData, i, chunk, 0, chunkSize);
            
            // Apply overlap from previous chunk
            for (int j = 0; j < overlapSize && j < chunkSize; j++) {
                chunk[j] = 0.5 * (chunk[j] + overlapBuffer[(overlapPosition + j) % overlapSize]);
            }
            
            // Apply MODWT
            MODWTResult modwtResult = transform.forward(chunk);
            double[] details = modwtResult.detailCoeffs();
            
            // Estimate noise if not done yet
            if (!noiseEstimated) {
                updateNoiseEstimate(details);
            }
            
            // Apply thresholding
            double threshold = calculateThreshold(details);
            double[] thresholdedDetails = applyThreshold(details, threshold);
            
            // Reconstruct using MODWT inverse
            MODWTResult thresholdedResult = new MODWTResultImpl(
                modwtResult.approximationCoeffs(), thresholdedDetails);
            double[] reconstructed = inverseTransform.inverse(thresholdedResult);
            
            // Copy to output
            System.arraycopy(reconstructed, 0, denoised, outputIndex, chunkSize);
            outputIndex += chunkSize;
            
            // Update overlap buffer
            int overlapStart = Math.max(0, chunkSize - overlapSize);
            for (int j = 0; j < overlapSize; j++) {
                if (overlapStart + j < chunkSize) {
                    overlapBuffer[j] = reconstructed[overlapStart + j];
                }
            }
            overlapPosition = 0;
        }
        
        return denoised;
    }

    /**
     * Process a single sample and return denoised result.
     *
     * @param noisySample the noisy input sample
     * @return denoised sample (may have latency due to buffering)
     */
    public synchronized double denoiseSample(double noisySample) {
        // For single-sample processing, we need to buffer
        // This is a simplified implementation
        double[] result = denoise(new double[]{noisySample});
        return result[0];
    }

    /**
     * Reset the denoiser state.
     */
    public synchronized void reset() {
        overlapPosition = 0;
        noiseWindowPosition = 0;
        noiseEstimated = false;
        estimatedNoiseLevel = 0.0;
        java.util.Arrays.fill(overlapBuffer, 0.0);
        java.util.Arrays.fill(noiseWindow, 0.0);
    }

    /**
     * Get the current estimated noise level.
     *
     * @return estimated noise standard deviation
     */
    public double getEstimatedNoiseLevel() {
        return estimatedNoiseLevel;
    }

    private void updateNoiseEstimate(double[] details) {
        // Add detail coefficients to noise window
        for (double d : details) {
            noiseWindow[noiseWindowPosition] = Math.abs(d);
            noiseWindowPosition = (noiseWindowPosition + 1) % noiseWindow.length;
        }
        
        // Estimate noise level using selected method
        if (noiseWindowPosition == 0) { // Window is full
            noiseEstimated = true;
            estimatedNoiseLevel = noiseEstimation.estimate(noiseWindow);
        }
    }

    private double calculateThreshold(double[] details) {
        if (estimatedNoiseLevel == 0.0) {
            // Fallback: estimate from current details
            estimatedNoiseLevel = noiseEstimation.estimate(details);
        }
        
        // Universal threshold: sigma * sqrt(2 * log(N))
        double universalThreshold = estimatedNoiseLevel * 
            Math.sqrt(2 * Math.log(details.length));
        
        return universalThreshold * thresholdMultiplier;
    }

    private double[] applyThreshold(double[] coefficients, double threshold) {
        double[] thresholded = new double[coefficients.length];
        
        for (int i = 0; i < coefficients.length; i++) {
            if (thresholdType == WaveletDenoiser.ThresholdType.SOFT) {
                // Soft thresholding
                double absCoeff = Math.abs(coefficients[i]);
                if (absCoeff <= threshold) {
                    thresholded[i] = 0.0;
                } else {
                    thresholded[i] = Math.signum(coefficients[i]) * (absCoeff - threshold);
                }
            } else {
                // Hard thresholding
                thresholded[i] = Math.abs(coefficients[i]) <= threshold ? 0.0 : coefficients[i];
            }
        }
        
        return thresholded;
    }

    @Override
    public void close() {
        // Clean up resources if needed
        reset();
    }

    /**
     * Noise estimation methods.
     */
    public enum NoiseEstimation {
        /**
         * Median Absolute Deviation - robust to outliers.
         */
        MAD {
            @Override
            double estimate(double[] coefficients) {
                double[] sorted = coefficients.clone();
                java.util.Arrays.sort(sorted);
                double median = sorted[sorted.length / 2];
                
                double[] deviations = new double[sorted.length];
                for (int i = 0; i < sorted.length; i++) {
                    deviations[i] = Math.abs(sorted[i] - median);
                }
                
                java.util.Arrays.sort(deviations);
                double mad = deviations[deviations.length / 2];
                
                // Scale factor for Gaussian noise
                return mad / 0.6745;
            }
        },
        
        /**
         * Standard deviation - assumes Gaussian noise.
         */
        STD {
            @Override
            double estimate(double[] coefficients) {
                double sum = 0.0;
                double sumSq = 0.0;
                
                for (double c : coefficients) {
                    sum += c;
                    sumSq += c * c;
                }
                
                double mean = sum / coefficients.length;
                double variance = (sumSq / coefficients.length) - (mean * mean);
                
                return Math.sqrt(variance);
            }
        };
        
        abstract double estimate(double[] coefficients);
    }

    /**
     * Builder for creating streaming MODWT denoisers.
     */
    public static class Builder {
        private Wavelet wavelet = ai.prophetizo.wavelet.api.Daubechies.DB4;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private int bufferSize = 256;
        private WaveletDenoiser.ThresholdType thresholdType = WaveletDenoiser.ThresholdType.SOFT;
        private NoiseEstimation noiseEstimation = NoiseEstimation.MAD;
        private double thresholdMultiplier = 1.0;
        private int noiseWindowSize = 1024;

        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }

        public Builder boundaryMode(BoundaryMode mode) {
            this.boundaryMode = mode;
            return this;
        }

        public Builder bufferSize(int size) {
            if (size <= 0) {
                throw new InvalidArgumentException("Buffer size must be positive");
            }
            this.bufferSize = size;
            return this;
        }

        public Builder thresholdType(WaveletDenoiser.ThresholdType type) {
            this.thresholdType = type;
            return this;
        }

        // Removed thresholdFunction method as we're using ThresholdType enum

        public Builder noiseEstimation(NoiseEstimation method) {
            this.noiseEstimation = method;
            return this;
        }

        public Builder thresholdMultiplier(double multiplier) {
            if (multiplier <= 0) {
                throw new InvalidArgumentException("Threshold multiplier must be positive");
            }
            this.thresholdMultiplier = multiplier;
            return this;
        }

        public Builder noiseWindowSize(int size) {
            if (size <= 0) {
                throw new InvalidArgumentException("Noise window size must be positive");
            }
            this.noiseWindowSize = size;
            return this;
        }

        public MODWTStreamingDenoiser build() {
            return new MODWTStreamingDenoiser(this);
        }
    }

    /**
     * Simple implementation of MODWTResult.
     */
    private static class MODWTResultImpl implements MODWTResult {
        private final double[] approx;
        private final double[] detail;

        MODWTResultImpl(double[] approx, double[] detail) {
            this.approx = approx;
            this.detail = detail;
        }

        @Override
        public double[] approximationCoeffs() {
            return approx.clone();
        }

        @Override
        public double[] detailCoeffs() {
            return detail.clone();
        }

        @Override
        public int getSignalLength() {
            return approx.length;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}