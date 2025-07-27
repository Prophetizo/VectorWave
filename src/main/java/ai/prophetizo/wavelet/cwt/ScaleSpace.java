package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;

import java.util.Arrays;

/**
 * Represents the scale space for Continuous Wavelet Transform.
 * 
 * <p>ScaleSpace defines the scales at which the wavelet transform is computed.
 * It provides various factory methods for creating commonly used scale distributions
 * (linear, logarithmic, dyadic) and supports conversion between scales and frequencies.</p>
 *
 * @since 1.0.0
 */
public final class ScaleSpace {
    
    /**
     * Types of scale spacing.
     */
    public enum ScaleType {
        LINEAR,      // Linear scale spacing
        LOGARITHMIC, // Log scale spacing (most common)
        DYADIC,      // Powers of 2
        CUSTOM       // User-defined scales
    }
    
    private final double[] scales;
    private final ScaleType type;
    private final double minScale;
    private final double maxScale;
    
    private ScaleSpace(double[] scales, ScaleType type) {
        if (scales == null || scales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Validate scales are positive and sorted
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] <= 0) {
                throw new IllegalArgumentException("All scales must be positive, found: " + scales[i]);
            }
            if (i > 0 && scales[i] <= scales[i - 1]) {
                throw new IllegalArgumentException("Scales must be in ascending order");
            }
        }
        
        this.scales = scales.clone();
        this.type = type;
        this.minScale = scales[0];
        this.maxScale = scales[scales.length - 1];
    }
    
    /**
     * Creates a linear scale space.
     * 
     * @param minScale minimum scale
     * @param maxScale maximum scale
     * @param numScales number of scales
     * @return linear scale space
     */
    public static ScaleSpace linear(double minScale, double maxScale, int numScales) {
        validateScaleRange(minScale, maxScale, numScales);
        
        double[] scales = new double[numScales];
        double step = (maxScale - minScale) / (numScales - 1);
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = minScale + i * step;
        }
        
        return new ScaleSpace(scales, ScaleType.LINEAR);
    }
    
    /**
     * Creates a logarithmic scale space.
     * 
     * @param minScale minimum scale
     * @param maxScale maximum scale
     * @param numScales number of scales
     * @return logarithmic scale space
     */
    public static ScaleSpace logarithmic(double minScale, double maxScale, int numScales) {
        validateScaleRange(minScale, maxScale, numScales);
        
        double[] scales = new double[numScales];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        double logStep = (logMax - logMin) / (numScales - 1);
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.exp(logMin + i * logStep);
        }
        
        return new ScaleSpace(scales, ScaleType.LOGARITHMIC);
    }
    
    /**
     * Creates a dyadic (powers of 2) scale space.
     * 
     * @param minLevel minimum level (2^minLevel)
     * @param maxLevel maximum level (2^maxLevel)
     * @return dyadic scale space
     */
    public static ScaleSpace dyadic(int minLevel, int maxLevel) {
        if (minLevel > maxLevel) {
            throw new IllegalArgumentException("minLevel must be <= maxLevel");
        }
        
        int numScales = maxLevel - minLevel + 1;
        double[] scales = new double[numScales];
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.pow(2, minLevel + i);
        }
        
        return new ScaleSpace(scales, ScaleType.DYADIC);
    }
    
    /**
     * Creates a scale space for a given frequency range.
     * 
     * @param minFreq minimum frequency in Hz
     * @param maxFreq maximum frequency in Hz
     * @param samplingRate sampling rate in Hz
     * @param wavelet continuous wavelet
     * @param numScales number of scales
     * @return scale space covering the frequency range
     */
    public static ScaleSpace forFrequencyRange(double minFreq, double maxFreq, 
                                              double samplingRate, ContinuousWavelet wavelet, 
                                              int numScales) {
        if (minFreq <= 0 || maxFreq <= 0) {
            throw new IllegalArgumentException("Frequencies must be positive");
        }
        if (minFreq >= maxFreq) {
            throw new IllegalArgumentException("minFreq must be < maxFreq");
        }
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        if (numScales <= 0) {
            throw new IllegalArgumentException("Number of scales must be positive");
        }
        
        // Convert frequencies to scales using:
        // scale = (centerFreq * samplingRate) / frequency
        double centerFreq = wavelet.centerFrequency();
        double maxScale = (centerFreq * samplingRate) / minFreq;
        double minScale = (centerFreq * samplingRate) / maxFreq;
        
        // Use logarithmic spacing for frequency range
        return logarithmic(minScale, maxScale, numScales);
    }
    
    /**
     * Creates a custom scale space from user-defined scales.
     * The scales will be automatically sorted in ascending order.
     * 
     * @param scales array of scales (must be positive, will be sorted)
     * @return custom scale space
     */
    public static ScaleSpace custom(double[] scales) {
        if (scales == null || scales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Make a sorted copy
        double[] sortedScales = scales.clone();
        Arrays.sort(sortedScales);
        
        return new ScaleSpace(sortedScales, ScaleType.CUSTOM);
    }
    
    /**
     * Creates a custom scale space from pre-sorted scales.
     * This method is more efficient than {@link #custom(double[])} when
     * the scales are already sorted in ascending order.
     * 
     * @param sortedScales array of scales in ascending order (must be positive)
     * @return custom scale space
     * @throws IllegalArgumentException if scales are not in ascending order
     */
    public static ScaleSpace customSorted(double[] sortedScales) {
        if (sortedScales == null || sortedScales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Verify scales are sorted and positive
        double previousScale = 0;
        for (int i = 0; i < sortedScales.length; i++) {
            if (sortedScales[i] <= 0) {
                throw new IllegalArgumentException("All scales must be positive");
            }
            if (sortedScales[i] <= previousScale) {
                throw new IllegalArgumentException("Scales must be in strictly ascending order");
            }
            previousScale = sortedScales[i];
        }
        
        // Use the array directly (clone for safety)
        return new ScaleSpace(sortedScales.clone(), ScaleType.CUSTOM);
    }
    
    /**
     * Converts scales to frequencies.
     * 
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return array of frequencies corresponding to each scale
     */
    public double[] toFrequencies(ContinuousWavelet wavelet, double samplingRate) {
        double centerFreq = wavelet.centerFrequency();
        double[] frequencies = new double[scales.length];
        
        for (int i = 0; i < scales.length; i++) {
            frequencies[i] = (centerFreq * samplingRate) / scales[i];
        }
        
        return frequencies;
    }
    
    /**
     * Converts a single scale to frequency.
     * 
     * @param scale the scale value
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return corresponding frequency in Hz
     */
    public double scaleToFrequency(double scale, ContinuousWavelet wavelet, double samplingRate) {
        return (wavelet.centerFrequency() * samplingRate) / scale;
    }
    
    /**
     * Finds the scale index closest to a target frequency.
     * 
     * @param targetFreq target frequency in Hz
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return index of the closest scale
     */
    public int findScaleIndexForFrequency(double targetFreq, ContinuousWavelet wavelet, 
                                         double samplingRate) {
        double[] frequencies = toFrequencies(wavelet, samplingRate);
        
        int closestIndex = 0;
        double minDiff = Math.abs(frequencies[0] - targetFreq);
        
        for (int i = 1; i < frequencies.length; i++) {
            double diff = Math.abs(frequencies[i] - targetFreq);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        
        return closestIndex;
    }
    
    // Getters
    
    public double[] getScales() {
        return scales.clone();
    }
    
    public int getNumScales() {
        return scales.length;
    }
    
    public double getMinScale() {
        return minScale;
    }
    
    public double getMaxScale() {
        return maxScale;
    }
    
    public ScaleType getType() {
        return type;
    }
    
    public double getScale(int index) {
        if (index < 0 || index >= scales.length) {
            throw new IndexOutOfBoundsException("Scale index out of bounds: " + index);
        }
        return scales[index];
    }
    
    // Private helper methods
    
    private static void validateScaleRange(double minScale, double maxScale, int numScales) {
        if (minScale <= 0 || maxScale <= 0) {
            throw new IllegalArgumentException("Scales must be positive");
        }
        if (minScale >= maxScale) {
            throw new IllegalArgumentException("minScale must be < maxScale");
        }
        if (numScales <= 0) {
            throw new IllegalArgumentException("Number of scales must be positive");
        }
    }
}