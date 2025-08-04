package ai.prophetizo.wavelet.modwt.streaming;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streaming denoiser based on MODWT for real-time signal denoising.
 * 
 * <p>This class provides real-time denoising capabilities using the MODWT transform,
 * which offers several advantages over DWT-based streaming denoisers:</p>
 * <ul>
 *   <li>Shift-invariance prevents artifacts at block boundaries</li>
 *   <li>Works with any buffer size (not restricted to powers of 2)</li>
 *   <li>Better preservation of signal features</li>
 *   <li>Improved noise estimation accuracy</li>
 * </ul>
 * 
 * <p>The denoiser processes data in blocks and can optionally publish results
 * to subscribers for further processing.</p>
 * 
 * @since 3.0.0
 */
public class MODWTStreamingDenoiser implements Flow.Publisher<double[]>, AutoCloseable {
    
    private final MODWTTransform transform;
    private final WaveletDenoiser denoiser;
    private final int bufferSize;
    private final ThresholdType thresholdType;
    private final ThresholdMethod thresholdMethod;
    private final double thresholdMultiplier;
    private final NoiseEstimation noiseEstimation;
    private final int noiseWindowSize;
    
    private final SubmissionPublisher<double[]> publisher;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong samplesProcessed = new AtomicLong(0);
    
    // Noise estimation
    private double[] noiseWindow;
    private int noiseWindowIndex = 0;
    private double estimatedNoiseLevel = 0.0;
    
    /**
     * Noise estimation method for streaming denoising.
     */
    public enum NoiseEstimation {
        /** Median Absolute Deviation estimation */
        MAD,
        /** Standard deviation estimation */
        STD,
        /** Fixed noise level */
        FIXED
    }
    
    private MODWTStreamingDenoiser(Builder builder) {
        this.transform = new MODWTTransform(builder.wavelet, builder.boundaryMode);
        this.denoiser = new WaveletDenoiser(builder.wavelet, builder.boundaryMode);
        
        this.bufferSize = builder.bufferSize;
        this.thresholdType = builder.thresholdType;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdMultiplier = builder.thresholdMultiplier;
        this.noiseEstimation = builder.noiseEstimation;
        this.noiseWindowSize = builder.noiseWindowSize;
        
        this.publisher = new SubmissionPublisher<>();
        
        if (noiseEstimation != NoiseEstimation.FIXED) {
            this.noiseWindow = new double[noiseWindowSize];
        }
    }
    
    /**
     * Process a block of samples and return the denoised result.
     * 
     * @param samples the input samples
     * @return the denoised samples
     * @throws IllegalStateException if the denoiser is closed
     * @throws InvalidArgumentException if samples is null or empty
     */
    public double[] denoise(double[] samples) {
        if (closed.get()) {
            throw new IllegalStateException("Denoiser is closed");
        }
        if (samples == null || samples.length == 0) {
            throw new InvalidArgumentException("Samples cannot be null or empty");
        }
        
        // Update noise estimation if needed
        if (noiseEstimation != NoiseEstimation.FIXED) {
            updateNoiseEstimation(samples);
        }
        
        // Denoise using WaveletDenoiser (which internally uses MODWT)
        double[] denoised = denoiser.denoise(samples, thresholdMethod);
        
        // Update statistics
        samplesProcessed.addAndGet(samples.length);
        
        // Publish to subscribers if any
        if (publisher.hasSubscribers()) {
            publisher.submit(denoised.clone());
        }
        
        return denoised;
    }
    
    /**
     * Update noise estimation based on new samples.
     */
    private void updateNoiseEstimation(double[] samples) {
        // Transform to get detail coefficients
        MODWTResult result = transform.forward(samples);
        double[] details = result.detailCoeffs();
        
        // Update noise window with detail coefficients
        for (double detail : details) {
            noiseWindow[noiseWindowIndex] = Math.abs(detail);
            noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
        }
        
        // Calculate noise level based on method
        switch (noiseEstimation) {
            case MAD:
                estimatedNoiseLevel = calculateMAD(noiseWindow) / 0.6745;
                break;
            case STD:
                estimatedNoiseLevel = calculateSTD(noiseWindow);
                break;
            default:
                // Fixed noise level, no update needed
                break;
        }
    }
    
    private double calculateMAD(double[] values) {
        // First, find median using quickselect (modifies array, so clone)
        double[] work = values.clone();
        double median = quickSelect(work, 0, work.length - 1, work.length / 2);
        
        // Calculate absolute deviations in-place
        for (int i = 0; i < work.length; i++) {
            work[i] = Math.abs(values[i] - median);
        }
        
        // Find median of deviations using quickselect
        return quickSelect(work, 0, work.length - 1, work.length / 2);
    }
    
    /**
     * Quickselect algorithm for finding kth smallest element.
     * More efficient than full sort for median calculation.
     * Average O(n) time complexity vs O(n log n) for sorting.
     */
    private double quickSelect(double[] arr, int left, int right, int k) {
        if (left == right) {
            return arr[left];
        }
        
        // Choose pivot and partition
        int pivotIndex = partition(arr, left, right);
        
        if (k == pivotIndex) {
            return arr[k];
        } else if (k < pivotIndex) {
            return quickSelect(arr, left, pivotIndex - 1, k);
        } else {
            return quickSelect(arr, pivotIndex + 1, right, k);
        }
    }
    
    private int partition(double[] arr, int left, int right) {
        // Use middle element as pivot to avoid worst-case on sorted data
        int mid = left + (right - left) / 2;
        double pivot = arr[mid];
        
        // Move pivot to end
        swap(arr, mid, right);
        
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (arr[i] < pivot) {
                swap(arr, storeIndex, i);
                storeIndex++;
            }
        }
        
        // Move pivot to its final position
        swap(arr, storeIndex, right);
        return storeIndex;
    }
    
    private void swap(double[] arr, int i, int j) {
        double temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    
    private double calculateSTD(double[] values) {
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.length;
        
        double variance = 0;
        for (double v : values) {
            double diff = v - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.length);
    }
    
    /**
     * Get the current estimated noise level.
     * 
     * @return the estimated noise level
     */
    public double getEstimatedNoiseLevel() {
        return estimatedNoiseLevel;
    }
    
    /**
     * Get the total number of samples processed.
     * 
     * @return the number of samples processed
     */
    public long getSamplesProcessed() {
        return samplesProcessed.get();
    }
    
    @Override
    public void subscribe(Flow.Subscriber<? super double[]> subscriber) {
        publisher.subscribe(subscriber);
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            publisher.close();
        }
    }
    
    /**
     * Check if the denoiser is closed.
     * 
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    /**
     * Builder for creating MODWTStreamingDenoiser instances.
     */
    public static class Builder {
        private Wavelet wavelet = Daubechies.DB4;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private int bufferSize = 256;
        private ThresholdType thresholdType = ThresholdType.SOFT;
        private ThresholdMethod thresholdMethod = ThresholdMethod.UNIVERSAL;
        private double thresholdMultiplier = 1.0;
        private NoiseEstimation noiseEstimation = NoiseEstimation.MAD;
        private int noiseWindowSize = 1024;
        
        /**
         * Set the wavelet to use for denoising.
         * 
         * @param wavelet the wavelet
         * @return this builder
         */
        public Builder wavelet(Wavelet wavelet) {
            if (wavelet == null) {
                throw new InvalidArgumentException("Wavelet cannot be null");
            }
            this.wavelet = wavelet;
            return this;
        }
        
        /**
         * Set the boundary mode.
         * 
         * @param boundaryMode the boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode boundaryMode) {
            if (boundaryMode == null) {
                throw new InvalidArgumentException("Boundary mode cannot be null");
            }
            this.boundaryMode = boundaryMode;
            return this;
        }
        
        /**
         * Set the buffer size.
         * 
         * @param bufferSize the buffer size (must be positive)
         * @return this builder
         */
        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new InvalidArgumentException("Buffer size must be positive");
            }
            this.bufferSize = bufferSize;
            return this;
        }
        
        /**
         * Set the threshold type.
         * 
         * @param thresholdType the threshold type
         * @return this builder
         */
        public Builder thresholdType(ThresholdType thresholdType) {
            if (thresholdType == null) {
                throw new InvalidArgumentException("Threshold type cannot be null");
            }
            this.thresholdType = thresholdType;
            return this;
        }
        
        /**
         * Set the threshold method.
         * 
         * @param thresholdMethod the threshold method
         * @return this builder
         */
        public Builder thresholdMethod(ThresholdMethod thresholdMethod) {
            if (thresholdMethod == null) {
                throw new InvalidArgumentException("Threshold method cannot be null");
            }
            this.thresholdMethod = thresholdMethod;
            return this;
        }
        
        /**
         * Set the threshold multiplier.
         * 
         * @param thresholdMultiplier the threshold multiplier (must be positive)
         * @return this builder
         */
        public Builder thresholdMultiplier(double thresholdMultiplier) {
            if (thresholdMultiplier <= 0) {
                throw new InvalidArgumentException("Threshold multiplier must be positive");
            }
            this.thresholdMultiplier = thresholdMultiplier;
            return this;
        }
        
        /**
         * Set the noise estimation method.
         * 
         * @param noiseEstimation the noise estimation method
         * @return this builder
         */
        public Builder noiseEstimation(NoiseEstimation noiseEstimation) {
            if (noiseEstimation == null) {
                throw new InvalidArgumentException("Noise estimation cannot be null");
            }
            this.noiseEstimation = noiseEstimation;
            return this;
        }
        
        /**
         * Set the noise window size for estimation.
         * 
         * @param noiseWindowSize the window size (must be positive)
         * @return this builder
         */
        public Builder noiseWindowSize(int noiseWindowSize) {
            if (noiseWindowSize <= 0) {
                throw new InvalidArgumentException("Noise window size must be positive");
            }
            this.noiseWindowSize = noiseWindowSize;
            return this;
        }
        
        /**
         * Build the streaming denoiser.
         * 
         * @return a new MODWTStreamingDenoiser instance
         */
        public MODWTStreamingDenoiser build() {
            return new MODWTStreamingDenoiser(this);
        }
    }
}