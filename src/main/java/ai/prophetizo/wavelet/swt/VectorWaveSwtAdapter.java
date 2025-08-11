package ai.prophetizo.wavelet.swt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.modwt.MutableMultiLevelMODWTResult;

import java.util.Objects;

/**
 * Adapter providing Stationary Wavelet Transform (SWT) functionality using MODWT.
 * 
 * <p>The Stationary Wavelet Transform (SWT), also known as the Undecimated Wavelet Transform
 * or À Trous Algorithm, is a shift-invariant wavelet transform that maintains the same data
 * length at each decomposition level. This adapter leverages VectorWave's MODWT implementation,
 * which shares the same mathematical properties as SWT.</p>
 * 
 * <p><strong>Key Properties:</strong></p>
 * <ul>
 *   <li><strong>Shift-invariant:</strong> Pattern detection is consistent regardless of signal position</li>
 *   <li><strong>Redundant representation:</strong> All levels have the same length as the original signal</li>
 *   <li><strong>Perfect reconstruction:</strong> Signal can be exactly reconstructed from coefficients</li>
 *   <li><strong>Arbitrary signal length:</strong> No power-of-2 restriction unlike standard DWT</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create SWT adapter
 * VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Perform SWT decomposition
 * MutableMultiLevelMODWTResult swtResult = swt.forward(signal, 3);
 * 
 * // Modify coefficients (e.g., thresholding for denoising)
 * swt.applyThreshold(swtResult, 2, 0.5, true);  // Soft threshold level 2
 * 
 * // Reconstruct modified signal
 * double[] denoised = swt.inverse(swtResult);
 * 
 * // Or use convenience denoising method
 * double[] denoised2 = swt.denoise(signal, 3, 0.5);
 * }</pre>
 * 
 * <p><strong>Relationship to MODWT:</strong></p>
 * <p>SWT and MODWT are mathematically equivalent transforms with different historical origins.
 * Both provide shift-invariant, redundant wavelet decompositions. This adapter provides an
 * SWT-style interface for users familiar with that terminology while leveraging VectorWave's
 * optimized MODWT implementation.</p>
 * 
 * @see MultiLevelMODWTTransform
 * @see MutableMultiLevelMODWTResult
 * @since 1.0
 */
public class VectorWaveSwtAdapter {
    
    private final MultiLevelMODWTTransform modwtTransform;
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    
    /**
     * Creates a new SWT adapter with specified wavelet and boundary handling.
     * 
     * @param wavelet the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @throws NullPointerException if wavelet or boundaryMode is null
     */
    public VectorWaveSwtAdapter(Wavelet wavelet, BoundaryMode boundaryMode) {
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "Boundary mode cannot be null");
        this.modwtTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Creates a new SWT adapter with periodic boundary handling.
     * 
     * @param wavelet the wavelet to use for decomposition
     * @throws NullPointerException if wavelet is null
     */
    public VectorWaveSwtAdapter(Wavelet wavelet) {
        this(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Performs forward SWT decomposition to the maximum number of levels.
     * 
     * @param signal the input signal
     * @return mutable SWT decomposition result
     * @throws IllegalArgumentException if signal is invalid
     */
    public MutableMultiLevelMODWTResult forward(double[] signal) {
        return modwtTransform.decomposeMutable(signal);
    }
    
    /**
     * Performs forward SWT decomposition to specified number of levels.
     * 
     * @param signal the input signal
     * @param levels number of decomposition levels
     * @return mutable SWT decomposition result
     * @throws IllegalArgumentException if signal or levels is invalid
     */
    public MutableMultiLevelMODWTResult forward(double[] signal, int levels) {
        return modwtTransform.decomposeMutable(signal, levels);
    }
    
    /**
     * Performs inverse SWT reconstruction from decomposition result.
     * 
     * <p>This method reconstructs the signal from potentially modified coefficients,
     * making it suitable for applications like denoising or feature extraction.</p>
     * 
     * @param result the SWT decomposition result (possibly modified)
     * @return reconstructed signal
     * @throws NullPointerException if result is null
     */
    public double[] inverse(MutableMultiLevelMODWTResult result) {
        return modwtTransform.reconstruct(result);
    }
    
    /**
     * Applies hard or soft thresholding to coefficients at a specific level.
     * 
     * <p>This is a convenience method for coefficient thresholding, commonly used
     * in wavelet denoising applications.</p>
     * 
     * @param result the SWT result to modify
     * @param level the decomposition level (0 for approximation, 1+ for details)
     * @param threshold the threshold value
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @throws NullPointerException if result is null
     * @throws IllegalArgumentException if level is out of range
     */
    public void applyThreshold(MutableMultiLevelMODWTResult result, int level, 
                               double threshold, boolean soft) {
        Objects.requireNonNull(result, "Result cannot be null");
        result.applyThreshold(level, threshold, soft);
    }
    
    /**
     * Applies universal threshold to all detail levels for denoising.
     * 
     * <p>The universal threshold is calculated as σ√(2log(N)) where σ is the
     * noise standard deviation estimated from the finest detail level.</p>
     * 
     * @param result the SWT result to denoise
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @throws NullPointerException if result is null
     */
    public void applyUniversalThreshold(MutableMultiLevelMODWTResult result, boolean soft) {
        Objects.requireNonNull(result, "Result cannot be null");
        
        // Estimate noise from finest detail level
        double[] finestDetails = result.getMutableDetailCoeffs(1);
        double sigma = estimateNoiseSigma(finestDetails);
        
        // Calculate universal threshold
        int n = result.getSignalLength();
        double threshold = sigma * Math.sqrt(2 * Math.log(n));
        
        // Apply to all detail levels
        for (int level = 1; level <= result.getLevels(); level++) {
            applyThreshold(result, level, threshold, soft);
        }
    }
    
    /**
     * Convenience method for signal denoising using SWT.
     * 
     * <p>Performs decomposition, applies universal soft thresholding, and reconstructs.</p>
     * 
     * @param signal the noisy signal
     * @param levels number of decomposition levels
     * @return denoised signal
     * @throws IllegalArgumentException if signal or levels is invalid
     */
    public double[] denoise(double[] signal, int levels) {
        return denoise(signal, levels, -1, true);
    }
    
    /**
     * Convenience method for signal denoising with custom threshold.
     * 
     * @param signal the noisy signal
     * @param levels number of decomposition levels
     * @param threshold custom threshold value (use -1 for universal threshold)
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @return denoised signal
     * @throws IllegalArgumentException if signal or levels is invalid
     */
    public double[] denoise(double[] signal, int levels, double threshold, boolean soft) {
        // Decompose signal
        MutableMultiLevelMODWTResult result = forward(signal, levels);
        
        if (threshold < 0) {
            // Use universal threshold
            applyUniversalThreshold(result, soft);
        } else {
            // Apply custom threshold to all detail levels
            for (int level = 1; level <= levels; level++) {
                applyThreshold(result, level, threshold, soft);
            }
        }
        
        // Reconstruct denoised signal
        return inverse(result);
    }
    
    /**
     * Extracts features at a specific decomposition level.
     * 
     * <p>This method zeros out all other levels and reconstructs, effectively
     * extracting features present only at the specified scale.</p>
     * 
     * @param signal the input signal
     * @param levels total number of decomposition levels
     * @param targetLevel the level to extract (0 for approximation, 1+ for details)
     * @return signal containing only features at the target level
     * @throws IllegalArgumentException if parameters are invalid
     */
    public double[] extractLevel(double[] signal, int levels, int targetLevel) {
        // Decompose signal
        MutableMultiLevelMODWTResult result = forward(signal, levels);
        
        // Zero out all levels except target
        for (int level = 1; level <= levels; level++) {
            if (level != targetLevel) {
                double[] coeffs = result.getMutableDetailCoeffs(level);
                java.util.Arrays.fill(coeffs, 0.0);
            }
        }
        
        // Zero approximation if not target
        if (targetLevel != 0) {
            double[] approx = result.getMutableApproximationCoeffs();
            java.util.Arrays.fill(approx, 0.0);
        }
        
        result.clearCaches();
        
        // Reconstruct with only target level
        return inverse(result);
    }
    
    /**
     * Gets the wavelet used by this adapter.
     * 
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this adapter.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Estimates noise standard deviation using median absolute deviation (MAD).
     * 
     * <p>This robust estimator is commonly used in wavelet denoising:
     * σ = median(|coeffs|) / 0.6745</p>
     * 
     * @param coeffs wavelet coefficients (typically finest detail level)
     * @return estimated noise standard deviation
     */
    private static double estimateNoiseSigma(double[] coeffs) {
        // Calculate absolute values
        double[] absCoeffs = new double[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            absCoeffs[i] = Math.abs(coeffs[i]);
        }
        
        // Find median
        java.util.Arrays.sort(absCoeffs);
        double median;
        if (absCoeffs.length % 2 == 0) {
            median = (absCoeffs[absCoeffs.length / 2 - 1] + absCoeffs[absCoeffs.length / 2]) / 2.0;
        } else {
            median = absCoeffs[absCoeffs.length / 2];
        }
        
        // MAD estimator for Gaussian noise
        return median / 0.6745;
    }
}