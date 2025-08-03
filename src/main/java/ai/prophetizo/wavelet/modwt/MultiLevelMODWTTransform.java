package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Performs multi-level MODWT (Maximal Overlap Discrete Wavelet Transform) decomposition and reconstruction.
 * 
 * <p>Multi-level MODWT applies the wavelet transform recursively to the approximation coefficients,
 * creating a hierarchy of detail coefficients at different scales. Unlike standard DWT multi-level
 * decomposition, MODWT preserves the original signal length at each level.</p>
 * 
 * <p><strong>Key advantages over multi-level DWT:</strong></p>
 * <ul>
 *   <li><strong>Shift-invariant at all levels:</strong> Pattern detection is consistent regardless of signal position</li>
 *   <li><strong>No downsampling:</strong> All coefficients align with original time points</li>
 *   <li><strong>Arbitrary signal length:</strong> No power-of-2 restriction</li>
 *   <li><strong>Better for alignment:</strong> Easier to relate features to original signal</li>
 * </ul>
 * 
 * <p><strong>Mathematical foundation:</strong></p>
 * <ul>
 *   <li>At level j, filters are scaled by 2^(j/2)</li>
 *   <li>Circular convolution without downsampling</li>
 *   <li>Redundant representation provides more information</li>
 * </ul>
 * 
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // Create multi-level MODWT transform
 * MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
 *     new Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Decompose signal to maximum depth
 * double[] signal = getFinancialTimeSeries(); // Any length!
 * MultiLevelMODWTResult result = mwt.decompose(signal);
 * 
 * // Analyze energy at different scales
 * for (int level = 1; level <= result.getLevels(); level++) {
 *     double[] details = result.getDetailCoeffsAtLevel(level);
 *     double energy = computeEnergy(details);
 *     System.out.println("Level " + level + " energy: " + energy);
 * }
 * 
 * // Reconstruct from specific level (denoising)
 * double[] denoised = mwt.reconstructFromLevel(result, 2);
 * 
 * // Partial reconstruction (only specific levels)
 * double[] bandpass = mwt.reconstructLevels(result, 2, 4);
 * }</pre>
 * 
 * @see MODWTTransform
 * @see MultiLevelMODWTResult
 * @since 3.0.0
 */
public class MultiLevelMODWTTransform {
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final MODWTTransform singleLevelTransform;
    
    /**
     * Constructs a multi-level MODWT transformer.
     * 
     * @param wavelet The wavelet to use for transformations
     * @param boundaryMode The boundary handling mode (currently only PERIODIC)
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public MultiLevelMODWTTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        this.singleLevelTransform = new MODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Performs full multi-level MODWT decomposition.
     * Decomposes to the maximum possible level based on signal length and filter length.
     * 
     * @param signal The input signal of any length
     * @return Multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     */
    public MultiLevelMODWTResult decompose(double[] signal) {
        int maxLevels = calculateMaxLevels(signal.length);
        return decompose(signal, maxLevels);
    }
    
    /**
     * Performs multi-level MODWT decomposition to specified number of levels.
     * 
     * @param signal The input signal of any length
     * @param levels Number of decomposition levels
     * @return Multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     * @throws InvalidArgumentException if levels is invalid
     */
    public MultiLevelMODWTResult decompose(double[] signal, int levels) {
        // Validate inputs
        ValidationUtils.validateFiniteValues(signal, "signal");
        if (signal.length == 0) {
            throw new InvalidSignalException("Signal cannot be empty");
        }
        
        int maxLevels = calculateMaxLevels(signal.length);
        if (levels < 1 || levels > maxLevels) {
            throw new InvalidArgumentException(
                "Invalid number of levels: " + levels + 
                ". Must be between 1 and " + maxLevels);
        }
        
        // Perform multi-level decomposition
        MultiLevelMODWTResultImpl result = new MultiLevelMODWTResultImpl(signal.length, levels);
        
        double[] currentApprox = signal.clone(); // Start with original signal
        
        for (int level = 1; level <= levels; level++) {
            // Apply single-level MODWT with scaled filters
            MODWTResult levelResult = transformAtLevel(currentApprox, level);
            
            // Store detail coefficients
            result.setDetailCoeffsAtLevel(level, levelResult.detailCoeffs());
            
            // Update approximation for next level
            currentApprox = levelResult.approximationCoeffs();
            
            // Store final approximation if at last level
            if (level == levels) {
                result.setApproximationCoeffs(currentApprox);
            }
        }
        
        return result;
    }
    
    /**
     * Reconstructs the original signal from multi-level MODWT result.
     * 
     * @param result The multi-level MODWT result
     * @return Reconstructed signal
     * @throws NullPointerException if result is null
     */
    public double[] reconstruct(MultiLevelMODWTResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        
        // Start with approximation at coarsest level
        double[] reconstruction = result.getApproximationCoeffs().clone();
        
        // Reconstruct level by level (from coarsest to finest)
        for (int level = result.getLevels(); level >= 1; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            reconstruction = reconstructSingleLevel(reconstruction, details, level);
        }
        
        return reconstruction;
    }
    
    /**
     * Reconstructs signal from a specific level, discarding finer details.
     * Useful for denoising by removing high-frequency components.
     * 
     * @param result The multi-level MODWT result
     * @param startLevel The level to start reconstruction from (1 = finest)
     * @return Reconstructed signal without details finer than startLevel
     */
    public double[] reconstructFromLevel(MultiLevelMODWTResult result, int startLevel) {
        Objects.requireNonNull(result, "result cannot be null");
        
        if (startLevel < 1 || startLevel > result.getLevels()) {
            throw new InvalidArgumentException(
                "Invalid start level: " + startLevel + 
                ". Must be between 1 and " + result.getLevels());
        }
        
        // Start with approximation
        double[] reconstruction = result.getApproximationCoeffs().clone();
        
        // Reconstruct from coarsest to startLevel (skip finer levels)
        for (int level = result.getLevels(); level >= startLevel; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            reconstruction = reconstructSingleLevel(reconstruction, details, level);
        }
        
        // Add zeros for skipped levels to maintain correct length
        for (int level = startLevel - 1; level >= 1; level--) {
            double[] zeroDetails = new double[reconstruction.length];
            reconstruction = reconstructSingleLevel(reconstruction, zeroDetails, level);
        }
        
        return reconstruction;
    }
    
    /**
     * Reconstructs signal using only specific levels (bandpass filtering).
     * 
     * @param result The multi-level MODWT result
     * @param minLevel Minimum level to include (inclusive)
     * @param maxLevel Maximum level to include (inclusive)
     * @return Reconstructed signal containing only specified frequency bands
     */
    public double[] reconstructLevels(MultiLevelMODWTResult result, int minLevel, int maxLevel) {
        Objects.requireNonNull(result, "result cannot be null");
        
        if (minLevel < 1 || maxLevel > result.getLevels() || minLevel > maxLevel) {
            throw new InvalidArgumentException(
                "Invalid level range [" + minLevel + ", " + maxLevel + "]. " +
                "Must be within [1, " + result.getLevels() + "] with minLevel <= maxLevel");
        }
        
        // Start with zeros (no approximation)
        double[] reconstruction = new double[result.getSignalLength()];
        
        // Add contributions from selected levels only
        for (int level = minLevel; level <= maxLevel; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            
            // For MODWT, we can directly add scaled contributions
            double scale = Math.pow(2, level / 2.0);
            for (int i = 0; i < details.length; i++) {
                reconstruction[i] += details[i] / scale;
            }
        }
        
        return reconstruction;
    }
    
    /**
     * Calculates the maximum number of decomposition levels.
     * For MODWT, this is based on filter length and signal length.
     */
    private int calculateMaxLevels(int signalLength) {
        int filterLength = wavelet.lowPassDecomposition().length;
        
        // For MODWT, we need to ensure the scaled filter doesn't exceed signal length
        // At level j, the filter is scaled by 2^(j-1), so effective length is (L-1)*2^(j-1)+1
        // We want (L-1)*2^(j-1)+1 <= N, which gives j <= log2(N-1/L-1) + 1
        
        int maxLevels = 1;
        while (maxLevels < 10) { // Practical limit
            int scaledFilterLength = (filterLength - 1) * (1 << (maxLevels - 1)) + 1;
            if (scaledFilterLength > signalLength) {
                break;
            }
            maxLevels++;
        }
        
        return maxLevels - 1;
    }
    
    /**
     * Performs single-level MODWT with appropriately scaled filters.
     */
    private MODWTResult transformAtLevel(double[] signal, int level) {
        // For level j, scale filters by 2^(j-1) through upsampling
        double[] scaledLowPass = scaleFilterForLevel(wavelet.lowPassDecomposition(), level);
        double[] scaledHighPass = scaleFilterForLevel(wavelet.highPassDecomposition(), level);
        
        // Apply MODWT with scaled filters directly
        return applyScaledMODWT(signal, scaledLowPass, scaledHighPass);
    }
    
    /**
     * Reconstructs single level by combining approximation and details.
     */
    private double[] reconstructSingleLevel(double[] approx, double[] details, int level) {
        // For MODWT reconstruction, we need upsampled filters but NOT scaled
        // The scaling was already applied during decomposition
        double[] upsampledLowPass = upsampleFilterForLevel(wavelet.lowPassReconstruction(), level);
        double[] upsampledHighPass = upsampleFilterForLevel(wavelet.highPassReconstruction(), level);
        
        // Apply inverse MODWT with upsampled (but not scaled) filters
        return applyScaledInverseMODWT(approx, details, upsampledLowPass, upsampledHighPass);
    }
    
    /**
     * Upsamples filter for MODWT at given level WITHOUT scaling.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     */
    private double[] upsampleFilterForLevel(double[] filter, int level) {
        if (level == 1) {
            return filter.clone();
        }
        
        int upFactor = (int) Math.pow(2, level - 1);
        int scaledLength = (filter.length - 1) * upFactor + 1;
        double[] upsampled = new double[scaledLength];
        
        // Insert zeros between filter coefficients WITHOUT scaling
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * upFactor] = filter[i];
        }
        
        return upsampled;
    }
    
    /**
     * Applies inverse MODWT with scaled filters directly.
     */
    private double[] applyScaledInverseMODWT(double[] approx, double[] details,
                                            double[] scaledLowPassRecon, double[] scaledHighPassRecon) {
        int signalLength = approx.length;
        double[] reconstructed = new double[signalLength];
        
        // For MODWT reconstruction, we need to be careful with long filters
        int maxFilterLength = Math.max(scaledLowPassRecon.length, scaledHighPassRecon.length);
        if (maxFilterLength > signalLength) {
            // Truncate filters if they exceed signal length
            int truncLength = signalLength;
            if (scaledLowPassRecon.length > truncLength) {
                double[] truncated = new double[truncLength];
                System.arraycopy(scaledLowPassRecon, 0, truncated, 0, truncLength);
                scaledLowPassRecon = truncated;
            }
            if (scaledHighPassRecon.length > truncLength) {
                double[] truncated = new double[truncLength];
                System.arraycopy(scaledHighPassRecon, 0, truncated, 0, truncLength);
                scaledHighPassRecon = truncated;
            }
        }
        
        // MODWT reconstruction using same indexing as single-level
        for (int t = 0; t < signalLength; t++) {
            double sum = 0.0;
            
            // Use backward indexing like the single-level MODWT
            for (int l = 0; l < scaledLowPassRecon.length; l++) {
                int idx = (t - l + signalLength * scaledLowPassRecon.length) % signalLength;
                sum += scaledLowPassRecon[l] * approx[idx] + 
                       scaledHighPassRecon[l] * details[idx];
            }
            
            // No additional normalization needed for multi-level
            reconstructed[t] = sum;
        }
        
        return reconstructed;
    }
    
    /**
     * Scales filter for MODWT at given level by upsampling with zeros.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     * MODWT also requires scaling by 2^(-j/2) for the filter values.
     */
    private double[] scaleFilterForLevel(double[] filter, int level) {
        if (level == 1) {
            // Even at level 1, MODWT scales by 2^(-1/2)
            double[] scaled = filter.clone();
            double scale = 1.0 / Math.sqrt(2.0);
            for (int i = 0; i < scaled.length; i++) {
                scaled[i] *= scale;
            }
            return scaled;
        }
        
        int upFactor = (int) Math.pow(2, level - 1);
        int scaledLength = (filter.length - 1) * upFactor + 1;
        double[] scaled = new double[scaledLength];
        
        // MODWT scaling factor
        double scale = Math.pow(2, -level / 2.0);
        
        // Insert zeros between filter coefficients and apply scaling
        for (int i = 0; i < filter.length; i++) {
            scaled[i * upFactor] = filter[i] * scale;
        }
        
        return scaled;
    }
    
    /**
     * Gets the wavelet used by this transform.
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this transform.
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Applies single-level MODWT with scaled filters directly.
     * This avoids the need to create a wavelet wrapper.
     */
    private MODWTResult applyScaledMODWT(double[] signal, double[] scaledLowPass, 
                                         double[] scaledHighPass) {
        int signalLength = signal.length;
        double[] approximationCoeffs = new double[signalLength];
        double[] detailCoeffs = new double[signalLength];
        
        // For very long filters (at high decomposition levels), we may need to truncate
        // to avoid issues with circular convolution
        if (scaledLowPass.length > signalLength) {
            double[] truncated = new double[signalLength];
            System.arraycopy(scaledLowPass, 0, truncated, 0, signalLength);
            scaledLowPass = truncated;
        }
        if (scaledHighPass.length > signalLength) {
            double[] truncated = new double[signalLength];
            System.arraycopy(scaledHighPass, 0, truncated, 0, signalLength);
            scaledHighPass = truncated;
        }
        
        // Use ScalarOps circular convolution for MODWT
        // For very short signals or long filters, use scalar implementation directly
        if (signal.length < 64 || scaledLowPass.length > signal.length / 2) {
            // Use scalar implementation directly
            circularConvolveMODWTDirect(signal, scaledLowPass, approximationCoeffs);
            circularConvolveMODWTDirect(signal, scaledHighPass, detailCoeffs);
        } else {
            ai.prophetizo.wavelet.internal.ScalarOps.circularConvolveMODWT(
                signal, scaledLowPass, approximationCoeffs);
            ai.prophetizo.wavelet.internal.ScalarOps.circularConvolveMODWT(
                signal, scaledHighPass, detailCoeffs);
        }
        
        return new MODWTResultImpl(approximationCoeffs, detailCoeffs);
    }
    
    /**
     * Direct circular convolution implementation for MODWT.
     * Used for small signals or when filters are very long.
     */
    private void circularConvolveMODWTDirect(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        // Ensure we don't go out of bounds with very long filters
        int effectiveFilterLen = Math.min(filterLen, signalLen);
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            for (int k = 0; k < effectiveFilterLen; k++) {
                int idx = (t - k + signalLen) % signalLen;
                sum += filter[k] * signal[idx];
            }
            output[t] = sum;
        }
    }
}