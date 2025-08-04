package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletType;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
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
    
    /**
     * Maximum practical limit for decomposition levels.
     * 
     * <p>This limit is based on several practical considerations:</p>
     * <ul>
     *   <li><strong>Numerical stability:</strong> At level j, filters are upsampled by 2^(j-1),
     *       leading to very long filters at high levels (e.g., level 10 = 512x original length)</li>
     *   <li><strong>Signal resolution:</strong> Most real-world signals lose meaningful information
     *       beyond 8-10 decomposition levels due to finite precision</li>
     *   <li><strong>Memory requirements:</strong> Each level stores full-length coefficient arrays,
     *       so 10 levels require 10x the original signal memory</li>
     *   <li><strong>Practical usage:</strong> Financial and scientific applications rarely need
     *       more than 6-8 levels of decomposition</li>
     * </ul>
     * 
     * <p>For a signal of length N with filter length L, the maximum meaningful level is
     * approximately log2(N/L). For example:</p>
     * <ul>
     *   <li>Signal length 1024, Haar filter (L=2): max ≈ 9 levels</li>
     *   <li>Signal length 4096, DB4 filter (L=8): max ≈ 9 levels</li>
     *   <li>Signal length 65536, DB8 filter (L=16): max ≈ 12 levels</li>
     * </ul>
     * 
     * <p>The limit of 10 provides a reasonable balance between flexibility and practicality.
     * If higher levels are needed for specific applications, this constant can be increased,
     * though performance and numerical stability should be carefully evaluated.</p>
     */
    private static final int MAX_DECOMPOSITION_LEVELS = 10;
    
    /**
     * Maximum safe bit shift amount to prevent integer overflow.
     * 
     * <p>In Java, left-shifting an integer by 31 or more bits results in undefined behavior
     * or integer overflow. Specifically, {@code 1 << 31} would overflow to a negative value
     * (Integer.MIN_VALUE). This constant is used to ensure bit shift operations remain safe
     * when calculating powers of 2 for filter upsampling at different decomposition levels.</p>
     * 
     * <p>For MODWT, at level j, filters are upsampled by 2^(j-1), so we need to ensure
     * that (j-1) never exceeds this limit to prevent overflow in scale factor calculations.</p>
     */
    private static final int MAX_SAFE_SHIFT_BITS = 31;
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final MODWTTransform singleLevelTransform;
    
    /**
     * Cache for truncated filters to avoid repeated allocations.
     * Uses a long key encoding both filter type and length to minimize allocations.
     * 
     * Key encoding: upper 32 bits = filter type ordinal, lower 32 bits = length
     * This avoids object allocation on every cache lookup.
     */
    private final Map<Long, double[]> truncatedFilterCache = new ConcurrentHashMap<>();
    
    /**
     * Record to hold a pair of scaled filters for efficient computation.
     * Avoids redundant calculations when scaling both low-pass and high-pass filters.
     */
    private record ScaledFilterPair(double[] lowPass, double[] highPass) {}
    
    /**
     * Validates that a decomposition level is safe for bit shift operations.
     * Prevents integer overflow in calculations like 1 << (level - 1).
     * 
     * @param level the decomposition level to validate (1-based)
     * @param operationName descriptive name of the operation for error message
     * @throws InvalidArgumentException if level would cause overflow
     */
    private static void validateLevelForBitShift(int level, String operationName) {
        if (level - 1 >= MAX_SAFE_SHIFT_BITS) {
            throw new InvalidArgumentException(
                "Level " + level + " would cause integer overflow in " + operationName);
        }
    }
    
    /**
     * Filter types for cache key encoding.
     */
    private enum FilterType {
        LOW(0), HIGH(1), LOW_RECON(2), HIGH_RECON(3);
        
        final int ordinal;
        
        FilterType(int ordinal) {
            this.ordinal = ordinal;
        }
    }
    
    /**
     * Constructs a multi-level MODWT transformer.
     * 
     * @param wavelet The wavelet to use for transformations
     * @param boundaryMode The boundary handling mode (PERIODIC or ZERO_PADDING)
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
        
        // Start with zeros for both approximation and details
        double[] reconstruction = new double[result.getSignalLength()];
        double[] zeroApprox = new double[result.getSignalLength()];
        double[] zeroDetails = new double[result.getSignalLength()];  // Reusable zero array
        
        // Reconstruct each level within the range properly
        for (int level = result.getLevels(); level >= 1; level--) {
            double[] details;
            
            if (level >= minLevel && level <= maxLevel) {
                // Use actual detail coefficients for levels in range
                details = result.getDetailCoeffsAtLevel(level);
            } else {
                // Use pre-allocated zeros for levels outside the range
                details = zeroDetails;
            }
            
            // Use proper convolution-based reconstruction
            // If this is the highest level and we're including it, start with approximation
            if (level == result.getLevels() && level <= maxLevel) {
                reconstruction = reconstructSingleLevel(result.getApproximationCoeffs(), details, level);
            } else if (level == result.getLevels()) {
                // Start with zero approximation if highest level is excluded
                reconstruction = reconstructSingleLevel(zeroApprox, details, level);
            } else {
                // Continue reconstruction with previous result
                reconstruction = reconstructSingleLevel(reconstruction, details, level);
            }
        }
        
        return reconstruction;
    }
    
    /**
     * Calculates the maximum number of decomposition levels.
     * For MODWT, this is based on filter length and signal length.
     * 
     * <p>Optimized implementation that avoids expensive operations in loops
     * by using direct calculation where possible.</p>
     */
    private int calculateMaxLevels(int signalLength) {
        int filterLength = wavelet.lowPassDecomposition().length;
        
        // Quick check for edge cases
        if (signalLength <= filterLength) {
            return 0;  // Can't even do one level
        }
        
        // For MODWT: at level j, effective filter length = (L-1)*2^(j-1)+1
        // We need (L-1)*2^(j-1)+1 <= N
        
        // Use the original algorithm's approach, but optimized
        // Start from level 1 and find the maximum valid level
        int maxLevel = 1;
        
        // Pre-compute values to avoid repeated calculations
        int filterLengthMinus1 = filterLength - 1;
        
        // Use bit shifting for powers of 2
        while (maxLevel < MAX_DECOMPOSITION_LEVELS) {
            // Check for potential overflow before shifting
            if (maxLevel - 1 >= MAX_SAFE_SHIFT_BITS) {
                break;  // Keep original logic for this case - it's breaking from loop, not throwing
            }
            
            // Calculate scaled filter length using bit shift
            // This is equivalent to: (filterLength - 1) * 2^(maxLevel - 1) + 1
            long scaledFilterLength = ((long)filterLengthMinus1 << (maxLevel - 1)) + 1;
            
            if (scaledFilterLength > signalLength) {
                break;
            }
            
            maxLevel++;
        }
        
        return maxLevel - 1;
    }
    
    /**
     * Performs single-level MODWT with appropriately scaled filters.
     */
    private MODWTResult transformAtLevel(double[] signal, int level) {
        // For level j, scale both filters together to avoid redundant calculations
        ScaledFilterPair scaledFilters = scaleFiltersForLevel(
            wavelet.lowPassDecomposition(), 
            wavelet.highPassDecomposition(), 
            level
        );
        
        // Apply MODWT with scaled filters directly
        return applyScaledMODWT(signal, scaledFilters.lowPass(), scaledFilters.highPass());
    }
    
    /**
     * Reconstructs single level by combining approximation and details.
     */
    private double[] reconstructSingleLevel(double[] approx, double[] details, int level) {
        // For MODWT reconstruction, we need upsampled filters - process both together
        ScaledFilterPair upsampledFilters = upsampleFiltersForLevel(
            wavelet.lowPassReconstruction(), 
            wavelet.highPassReconstruction(), 
            level
        );
        
        // Apply inverse MODWT with the upsampled and scaled filters
        return applyScaledInverseMODWT(approx, details, upsampledFilters.lowPass(), upsampledFilters.highPass());
    }
    
    /**
     * Upsamples both low-pass and high-pass filters for MODWT at given level with scaling.
     * This optimized method avoids redundant calculations by processing both filters together.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients and apply 1/sqrt(2) scaling.
     */
    private ScaledFilterPair upsampleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        if (level == 1) {
            // Level 1: no upsampling, just scale by 1/sqrt(2)
            double scale = 1.0 / Math.sqrt(2.0);
            
            double[] scaledLow = lowFilter.clone();
            double[] scaledHigh = highFilter.clone();
            
            for (int i = 0; i < scaledLow.length; i++) {
                scaledLow[i] *= scale;
            }
            for (int i = 0; i < scaledHigh.length; i++) {
                scaledHigh[i] *= scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
        }
        
        try {
            // Check for potential overflow in bit shift (once for both filters)
            validateLevelForBitShift(level, "filter upsampling");
            
            // Calculate shared values once
            int upFactor = 1 << (level - 1);
            double scale = 1.0 / Math.sqrt(2.0); // MODWT scaling
            
            // Process low-pass filter
            int upsampledLowLength = Math.addExact(
                Math.multiplyExact(lowFilter.length - 1, upFactor), 
                1
            );
            double[] upsampledLow = new double[upsampledLowLength];
            
            for (int i = 0; i < lowFilter.length; i++) {
                upsampledLow[i * upFactor] = lowFilter[i] * scale;
            }
            
            // Process high-pass filter
            int upsampledHighLength = Math.addExact(
                Math.multiplyExact(highFilter.length - 1, upFactor), 
                1
            );
            double[] upsampledHigh = new double[upsampledHighLength];
            
            for (int i = 0; i < highFilter.length; i++) {
                upsampledHigh[i * upFactor] = highFilter[i] * scale;
            }
            
            return new ScaledFilterPair(upsampledLow, upsampledHigh);
            
        } catch (ArithmeticException e) {
            throw new InvalidArgumentException(
                "Arithmetic overflow when upsampling filters for level " + level + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * Upsamples filter for MODWT at given level WITHOUT scaling.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     * 
     * @deprecated Use upsampleFiltersForLevel for better performance when processing both filters
     */
    private double[] upsampleFilterForLevel(double[] filter, int level) {
        if (level == 1) {
            return filter.clone();
        }
        
        try {
            // Check for potential overflow in bit shift
            validateLevelForBitShift(level, "filter upsampling");
            
            int upFactor = 1 << (level - 1); // Safer than Math.pow for integer powers of 2
            int scaledLength = Math.addExact(
                Math.multiplyExact(filter.length - 1, upFactor), 
                1
            );
            double[] upsampled = new double[scaledLength];
            
            // Insert zeros between filter coefficients WITHOUT scaling
            for (int i = 0; i < filter.length; i++) {
                upsampled[i * upFactor] = filter[i];
            }
            
            return upsampled;
        } catch (ArithmeticException e) {
            // Overflow in filter length calculation - this indicates the level is too high
            throw new InvalidArgumentException(
                "Level " + level + " would create filter length exceeding integer limits");
        }
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
            if (scaledLowPassRecon.length > signalLength) {
                scaledLowPassRecon = getTruncatedFilter(scaledLowPassRecon, signalLength, FilterType.LOW_RECON);
            }
            if (scaledHighPassRecon.length > signalLength) {
                scaledHighPassRecon = getTruncatedFilter(scaledHighPassRecon, signalLength, FilterType.HIGH_RECON);
            }
        }
        
        // MODWT reconstruction using same indexing as single-level
        for (int t = 0; t < signalLength; t++) {
            double sum = 0.0;
            
            // Use (t + l) indexing to match single-level MODWT reconstruction
            for (int l = 0; l < scaledLowPassRecon.length; l++) {
                int idx = (t + l) % signalLength;
                sum += scaledLowPassRecon[l] * approx[idx] + 
                       scaledHighPassRecon[l] * details[idx];
            }
            
            // No additional normalization needed with scaled filters
            reconstructed[t] = sum;
        }
        
        return reconstructed;
    }
    
    /**
     * Scales both low-pass and high-pass filters for MODWT at given level.
     * This optimized method avoids redundant calculations by processing both filters together.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     * MODWT only uses 1/sqrt(2) scaling, regardless of level.
     */
    private ScaledFilterPair scaleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        if (level == 1) {
            // Level 1: no upsampling, just scale by 1/sqrt(2)
            double scale = 1.0 / Math.sqrt(2.0);
            
            double[] scaledLow = lowFilter.clone();
            double[] scaledHigh = highFilter.clone();
            
            for (int i = 0; i < scaledLow.length; i++) {
                scaledLow[i] *= scale;
            }
            for (int i = 0; i < scaledHigh.length; i++) {
                scaledHigh[i] *= scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
        }
        
        try {
            // Check for potential overflow in bit shift (once for both filters)
            validateLevelForBitShift(level, "filter scaling");
            
            // Calculate shared values once
            int upFactor = 1 << (level - 1); // Safer than Math.pow for integer powers of 2
            double scale = 1.0 / Math.sqrt(2.0); // MODWT always uses 1/sqrt(2) scaling
            
            // Process low-pass filter
            int scaledLowLength = Math.addExact(
                Math.multiplyExact(lowFilter.length - 1, upFactor), 
                1
            );
            double[] scaledLow = new double[scaledLowLength];
            
            for (int i = 0; i < lowFilter.length; i++) {
                scaledLow[i * upFactor] = lowFilter[i] * scale;
            }
            
            // Process high-pass filter
            int scaledHighLength = Math.addExact(
                Math.multiplyExact(highFilter.length - 1, upFactor), 
                1
            );
            double[] scaledHigh = new double[scaledHighLength];
            
            for (int i = 0; i < highFilter.length; i++) {
                scaledHigh[i * upFactor] = highFilter[i] * scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
            
        } catch (ArithmeticException e) {
            throw new InvalidArgumentException(
                "Arithmetic overflow when scaling filters for level " + level + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * Scales filter for MODWT at given level by upsampling with zeros.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     * MODWT only uses 1/sqrt(2) scaling, regardless of level.
     * 
     * @deprecated Use scaleFiltersForLevel for better performance when scaling both filters
     */
    private double[] scaleFilterForLevel(double[] filter, int level) {
        if (level == 1) {
            // Level 1: no upsampling, just scale by 1/sqrt(2)
            double[] scaled = filter.clone();
            double scale = 1.0 / Math.sqrt(2.0);
            for (int i = 0; i < scaled.length; i++) {
                scaled[i] *= scale;
            }
            return scaled;
        }
        
        try {
            // Check for potential overflow in bit shift
            validateLevelForBitShift(level, "filter scaling");
            
            int upFactor = 1 << (level - 1); // Safer than Math.pow for integer powers of 2
            int scaledLength = Math.addExact(
                Math.multiplyExact(filter.length - 1, upFactor), 
                1
            );
            double[] scaled = new double[scaledLength];
            
            // MODWT always uses 1/sqrt(2) scaling, not 2^(-j/2)
            double scale = 1.0 / Math.sqrt(2.0);
            
            // Insert zeros between filter coefficients and apply scaling
            for (int i = 0; i < filter.length; i++) {
                scaled[i * upFactor] = filter[i] * scale;
            }
            
            return scaled;
        } catch (ArithmeticException e) {
            // Overflow in filter length calculation - this indicates the level is too high
            throw new InvalidArgumentException(
                "Level " + level + " would create filter length exceeding integer limits");
        }
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
     * Calculates the theoretical maximum number of decomposition levels for a given signal length.
     * This is based on the mathematical constraint that the scaled filter should not exceed the signal length.
     * 
     * @param signalLength the length of the signal
     * @return the maximum number of decomposition levels (capped at MAX_DECOMPOSITION_LEVELS)
     */
    public int getMaximumLevels(int signalLength) {
        return Math.min(calculateMaxLevels(signalLength), MAX_DECOMPOSITION_LEVELS);
    }
    
    /**
     * Gets the maximum decomposition level limit.
     * 
     * @return the maximum allowed decomposition levels (currently 10)
     */
    public static int getMaxDecompositionLevels() {
        return MAX_DECOMPOSITION_LEVELS;
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
            scaledLowPass = getTruncatedFilter(scaledLowPass, signalLength, FilterType.LOW);
        }
        if (scaledHighPass.length > signalLength) {
            scaledHighPass = getTruncatedFilter(scaledHighPass, signalLength, FilterType.HIGH);
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
        
        // Optimize for the common case where we don't need wrapping
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            // Process coefficients that don't need wrapping
            int maxK = Math.min(effectiveFilterLen, t + 1);
            for (int k = 0; k < maxK; k++) {
                sum += filter[k] * signal[t - k];
            }
            
            // Process coefficients that need wrapping (circular convolution)
            for (int k = maxK; k < effectiveFilterLen; k++) {
                sum += filter[k] * signal[t - k + signalLen];
            }
            
            output[t] = sum;
        }
    }
    
    /**
     * Gets a truncated version of a filter, using cache to avoid repeated allocations.
     * 
     * @param filter the original filter
     * @param targetLength the desired length
     * @param filterType the type of filter for caching
     * @return truncated filter of the specified length
     * @throws IllegalArgumentException if targetLength is invalid
     */
    private double[] getTruncatedFilter(double[] filter, int targetLength, FilterType filterType) {
        if (targetLength <= 0) {
            throw new IllegalArgumentException("Target length must be positive: " + targetLength);
        }
        if (targetLength > filter.length) {
            throw new IllegalArgumentException(
                "Target length (" + targetLength + ") exceeds filter length (" + filter.length + ")");
        }
        
        // Use primitive long key to avoid object allocation - inline for performance
        long cacheKey = ((long) filterType.ordinal << 32) | (targetLength & 0xFFFFFFFFL);
        
        return truncatedFilterCache.computeIfAbsent(cacheKey, key -> {
            double[] truncated = new double[targetLength];
            System.arraycopy(filter, 0, truncated, 0, targetLength);
            return truncated;
        });
    }
}