package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.util.ValidationUtils;
import ai.prophetizo.wavelet.util.BatchValidation;

/**
 * Performs multi-level wavelet decomposition and reconstruction.
 * 
 * <p>Multi-level decomposition applies the wavelet transform recursively to the
 * approximation coefficients, creating a hierarchy of detail coefficients at
 * different scales. This is essential for:</p>
 * <ul>
 *   <li>Multi-resolution analysis of financial time series</li>
 *   <li>Separating trends from cycles at different time horizons</li>
 *   <li>Denoising by removing high-frequency components</li>
 *   <li>Feature extraction at multiple scales</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * double[] prices = getStockPrices();
 * MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Full decomposition
 * MultiLevelTransformResult result = mwt.decompose(prices);
 * 
 * // Analyze volatility at different scales
 * for (int level = 1; level <= result.levels(); level++) {
 *     double energy = result.detailEnergyAtLevel(level);
 *     System.out.println("Scale " + level + " volatility: " + Math.sqrt(energy));
 * }
 * 
 * // Denoise by reconstructing without finest details
 * double[] smoothed = mwt.reconstructFromLevel(result, 2);
 * 
 * // For long-lived results, clear cache to free memory
 * if (result.getCacheMemoryUsage() > 1_000_000) { // 1MB threshold
 *     result.clearCache();
 * }
 * }</pre>
 * 
 * @since 1.1.0
 */
public class MultiLevelWaveletTransform {
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final TransformConfig config;
    private final WaveletTransform baseTransform;
    
    /**
     * Creates a multi-level wavelet transform with periodic boundary handling.
     * 
     * @param wavelet the wavelet to use for decomposition
     * @throws IllegalArgumentException if wavelet is null or not discrete
     */
    public MultiLevelWaveletTransform(Wavelet wavelet) {
        this(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Creates a multi-level wavelet transform with specified boundary handling.
     * 
     * @param wavelet the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @throws IllegalArgumentException if any parameter is null or wavelet is not discrete
     */
    public MultiLevelWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, TransformConfig.defaultConfig());
    }
    
    /**
     * Creates a multi-level wavelet transform with custom configuration.
     * 
     * @param wavelet the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @param config the transform configuration
     * @throws IllegalArgumentException if any parameter is null or wavelet is not discrete
     */
    public MultiLevelWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                     TransformConfig config) {
        if (wavelet == null) {
            throw new IllegalArgumentException("wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new IllegalArgumentException("boundaryMode cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        
        if (!(wavelet instanceof DiscreteWavelet)) {
            throw new IllegalArgumentException(
                "Only discrete wavelets are supported. Got: " + wavelet.getClass().getSimpleName());
        }
        
        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.config = config;
        this.baseTransform = new WaveletTransform(wavelet, boundaryMode);
    }
    
    /**
     * Performs full decomposition to the maximum possible number of levels.
     * 
     * <p>The maximum number of levels is determined by the signal length and
     * the wavelet filter length. Decomposition stops when further decomposition
     * would produce coefficients shorter than the filter.</p>
     * 
     * @param signal the input signal (must be power of 2 length)
     * @return multi-level transform result
     * @throws IllegalArgumentException if signal is invalid
     */
    public MultiLevelTransformResult decompose(double[] signal) {
        ValidationUtils.validateSignal(signal, "signal");
        
        // Calculate maximum possible levels
        int maxLevels = calculateMaxLevels(signal.length);
        return decompose(signal, maxLevels);
    }
    
    /**
     * Performs decomposition to the specified number of levels.
     * 
     * @param signal the input signal (must be power of 2 length)
     * @param requestedLevels the number of decomposition levels
     * @return multi-level transform result
     * @throws IllegalArgumentException if signal is invalid or levels out of range
     */
    public MultiLevelTransformResult decompose(double[] signal, int requestedLevels) {
        ValidationUtils.validateSignal(signal, "signal");
        
        if (requestedLevels < 1) {
            throw new IllegalArgumentException("Number of levels must be at least 1");
        }
        
        // Calculate maximum possible levels
        int maxLevels = calculateMaxLevels(signal.length);
        if (requestedLevels > maxLevels) {
            throw new IllegalArgumentException(
                "Requested levels (" + requestedLevels + ") exceeds maximum possible (" + 
                maxLevels + ") for signal length " + signal.length);
        }
        
        // Perform multi-level decomposition
        double[][] allDetails = new double[requestedLevels][];
        double[] currentApprox = signal.clone(); // Start with original signal
        
        for (int level = 0; level < requestedLevels; level++) {
            // Apply single-level transform
            TransformResult levelResult = baseTransform.forward(currentApprox);
            
            // Store detail coefficients
            allDetails[level] = levelResult.detailCoeffs();
            
            // Use approximation for next level
            currentApprox = levelResult.approximationCoeffs();
        }
        
        // Create and return result
        return new MultiLevelTransformResultImpl(
            currentApprox, allDetails, wavelet, boundaryMode);
    }
    
    /**
     * Performs adaptive decomposition based on detail energy threshold.
     * 
     * <p>Decomposition continues until the relative energy of detail coefficients
     * falls below the specified threshold. This is useful for automatic
     * determination of significant decomposition levels.</p>
     * 
     * @param signal the input signal
     * @param energyThreshold the relative energy threshold (0.0 to 1.0)
     * @return multi-level transform result
     * @throws IllegalArgumentException if inputs are invalid
     */
    public MultiLevelTransformResult decomposeAdaptive(double[] signal, 
                                                      double energyThreshold) {
        ValidationUtils.validateSignal(signal, "signal");
        
        if (energyThreshold <= 0.0 || energyThreshold >= 1.0) {
            throw new IllegalArgumentException(
                "Energy threshold must be between 0 and 1, got: " + energyThreshold);
        }
        
        // Calculate signal energy
        double signalEnergy = 0.0;
        for (double s : signal) {
            signalEnergy += s * s;
        }
        
        // Perform adaptive decomposition
        int maxLevels = calculateMaxLevels(signal.length);
        double[][] detailsList = new double[maxLevels][];
        double[] currentApprox = signal.clone();
        int actualLevels = 0;
        
        for (int level = 0; level < maxLevels; level++) {
            // Apply single-level transform
            TransformResult levelResult = baseTransform.forward(currentApprox);
            double[] details = levelResult.detailCoeffs();
            
            // Calculate detail energy
            double detailEnergy = 0.0;
            for (double d : details) {
                detailEnergy += d * d;
            }
            
            // Check threshold
            double relativeEnergy = detailEnergy / signalEnergy;
            if (relativeEnergy < energyThreshold && level > 0) {
                // Stop decomposition
                break;
            }
            
            // Store coefficients
            detailsList[level] = details;
            currentApprox = levelResult.approximationCoeffs();
            actualLevels++;
        }
        
        // Trim arrays to actual size
        double[][] allDetails = new double[actualLevels][];
        System.arraycopy(detailsList, 0, allDetails, 0, actualLevels);
        
        return new MultiLevelTransformResultImpl(
            currentApprox, allDetails, wavelet, boundaryMode);
    }
    
    /**
     * Reconstructs the original signal from multi-level decomposition.
     * 
     * @param result the multi-level transform result
     * @return the reconstructed signal
     * @throws IllegalArgumentException if result is null
     */
    public double[] reconstruct(MultiLevelTransformResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        
        // Start with final approximation
        double[] current = result.finalApproximation();
        
        // Reconstruct level by level
        for (int level = result.levels(); level >= 1; level--) {
            // Get details at this level
            double[] details = result.detailsAtLevel(level);
            
            // Create single-level result
            TransformResult levelResult = TransformResultImpl.createFast(current, details);
            
            // Apply inverse transform
            current = baseTransform.inverse(levelResult);
        }
        
        return current;
    }
    
    /**
     * Reconstructs signal from a specific level, zeroing out finer details.
     * 
     * <p>This is useful for denoising or extracting smooth trends by removing
     * high-frequency components. Level 0 returns the original signal,
     * higher levels produce progressively smoother reconstructions.</p>
     * 
     * @param result the multi-level transform result
     * @param fromLevel the level to reconstruct from (0 to levels)
     * @return the reconstructed signal with finer details removed
     * @throws IllegalArgumentException if inputs are invalid
     */
    public double[] reconstructFromLevel(MultiLevelTransformResult result, int fromLevel) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        
        if (fromLevel < 0 || fromLevel > result.levels()) {
            throw new IllegalArgumentException(
                "From level must be between 0 and " + result.levels() + ", got: " + fromLevel);
        }
        
        // Level 0 means full reconstruction
        if (fromLevel == 0) {
            return reconstruct(result);
        }
        
        // Start with approximation at fromLevel
        double[] current = result.approximationAtLevel(fromLevel);
        
        // Reconstruct from fromLevel down to level 1
        for (int level = fromLevel; level >= 1; level--) {
            // Create zero details for this level
            double[] zeroDetails = new double[result.detailsAtLevel(level).length];
            
            // Create single-level result with zero details
            TransformResult levelResult = TransformResultImpl.createFast(current, zeroDetails);
            
            // Apply inverse transform
            current = baseTransform.inverse(levelResult);
        }
        
        return current;
    }
    
    /**
     * Calculates the maximum number of decomposition levels for a given signal length.
     */
    private int calculateMaxLevels(int signalLength) {
        if (!(wavelet instanceof DiscreteWavelet discreteWavelet)) {
            throw new IllegalStateException("Wavelet must be discrete");
        }
        
        int filterLength = discreteWavelet.lowPassDecomposition().length;
        int maxLevels = 0;
        int currentLength = signalLength;
        
        // Each level halves the length; stop when length < filter length
        while (currentLength >= filterLength && maxLevels < config.getMaxDecompositionLevels()) {
            currentLength = (currentLength + 1) / 2;
            maxLevels++;
        }
        
        return Math.max(1, maxLevels);
    }
    
    /**
     * Gets the wavelet used by this transform.
     * 
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this transform.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Gets the transform configuration.
     * 
     * @return the configuration
     */
    public TransformConfig getConfig() {
        return config;
    }
}